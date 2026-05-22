package engine.systems;

import engine.components.OrderBoxComponent;
import engine.components.OrderComponent;
import engine.components.ProductComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;

/**
 * OrderTrackingSystem tracks delivered items and updates the OrderComponent.
 * 
 * Responsibilities:
 * - Monitor when products are placed into the order box
 * - Count delivered items by product type
 * - Prevent duplicate counting of the same item
 * - Update the OrderComponent delivered items count
 * 
 * Integration:
 * - Reads from OrderBoxComponent.receivedItemIds (populated by InteractionExecutionSystem)
 * - Reads from ProductComponent.productType to identify item types
 * - Updates OrderComponent.deliveredItems count
 * 
 * This system is lightweight and only processes items that have been placed in the box.
 */
public final class OrderTrackingSystem implements GameSystem {
    // Track which items we've already counted to prevent duplicates
    private int lastProcessedItemCount = 0;

    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        // Find OrderComponent and OrderBoxComponent
        OrderComponent order = null;
        OrderBoxComponent orderBox = null;

        for (int entityId : world.getActiveEntityIds()) {
            OrderComponent o = world.getComponent(entityId, OrderComponent.class);
            if (o != null) {
                order = o;
            }

            OrderBoxComponent ob = world.getComponent(entityId, OrderBoxComponent.class);
            if (ob != null) {
                orderBox = ob;
            }

            if (order != null && orderBox != null) {
                break;
            }
        }

        if (order == null || orderBox == null) {
            return;
        }

        // Check if new items have been added to the order box
        int currentItemCount = orderBox.receivedItemEntityIds.size();
        if (currentItemCount < lastProcessedItemCount) {
            lastProcessedItemCount = currentItemCount;
        }
        if (currentItemCount > lastProcessedItemCount) {
            // New items added - process them
            for (int i = lastProcessedItemCount; i < currentItemCount; i++) {
                int itemEntityId = orderBox.receivedItemEntityIds.get(i);
                ProductComponent product = world.getComponent(itemEntityId, ProductComponent.class);
                
                if (product != null && product.productType != null) {
                    // Increment delivered count for this product type
                    String type = product.productType;
                    int currentCount = order.deliveredItems.getOrDefault(type, 0);
                    int nextCount = currentCount + 1;
                    order.deliveredItems.put(type, nextCount);

                    int requiredCount = order.currentOrder.getOrDefault(type, 0);
                    if (nextCount > requiredCount) {
                        resetOrderProgress(order, orderBox);
                        lastProcessedItemCount = 0;
                        return;
                    }

                    logItemDelivered(order, product.productType);
                }
            }
            lastProcessedItemCount = currentItemCount;
        }
    }

    @Override
    public void render(EcsWorld world) {
        // No rendering for order tracking
    }

    private void logItemDelivered(OrderComponent order, String productType) {
        int delivered = order.deliveredItems.get(productType);
        int required = order.currentOrder.get(productType);
        System.out.println("[OrderTrackingSystem] Delivered " + productType.toUpperCase() 
                         + ": " + delivered + "/" + required);
        System.out.flush();
        printOrderStatus(order);
    }

    private void resetOrderProgress(OrderComponent order, OrderBoxComponent orderBox) {
        order.resetDelivered();
        order.orderComplete = false;
        if (orderBox != null) {
            orderBox.complete = false;
            orderBox.receivedItemEntityIds.clear();
        }
        System.out.println("[OrderTrackingSystem] Too many items delivered. Order reset.");
        System.out.flush();
        printOrderStatus(order);
    }

    private void printOrderStatus(OrderComponent order) {
        StringBuilder builder = new StringBuilder();
        for (String productType : order.currentOrder.keySet()) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(capitalize(productType))
                    .append(": ")
                    .append(order.deliveredItems.getOrDefault(productType, 0))
                    .append("/")
                    .append(order.currentOrder.getOrDefault(productType, 0));
        }

        System.out.println("[ORDER] Level " + order.currentLevel + " - " + builder);
        System.out.flush();
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}
