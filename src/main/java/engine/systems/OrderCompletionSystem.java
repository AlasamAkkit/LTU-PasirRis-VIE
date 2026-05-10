package engine.systems;

import engine.components.OrderBoxComponent;
import engine.components.OrderComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;

/**
 * OrderCompletionSystem checks if an order is completed and advances to the next level.
 * 
 * Responsibilities:
 * - Monitor if the OrderComponent has fulfilled all required items
 * - When order is complete: increment level and trigger new order generation
 * - Update the order complete flag for UI display
 * - Log order completion and level advance events
 * 
 * Integration:
 * - Reads from OrderComponent (current order and delivered items)
 * - Calls OrderGenerationSystem to generate next order
 * - Updates OrderBoxComponent.complete flag for visual feedback
 * 
 * This system is event-driven: it only takes action when the order state changes.
 */
public final class OrderCompletionSystem implements GameSystem {
    private final OrderGenerationSystem orderGenerationSystem;
    private boolean lastOrderState = false;
    private int cachedOrderEntityId = -1;
    private int cachedOrderBoxEntityId = -1;

    public OrderCompletionSystem(OrderGenerationSystem orderGenerationSystem) {
        this.orderGenerationSystem = orderGenerationSystem;
    }

    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        OrderComponent order = getCachedOrder(world);
        OrderBoxComponent orderBox = getCachedOrderBox(world);

        if (order == null) {
            return;
        }

        // Check if order was previously incomplete but is now complete
        boolean currentOrderState = order.isOrderFulfilled();
        if (currentOrderState && !lastOrderState) {
            // Order just completed!
            order.orderComplete = true;
            if (orderBox != null) {
                orderBox.complete = true;
            }
            
            logOrderCompletion();
            
            // Advance level and generate new order immediately
            order.currentLevel++;
            logLevelAdvance(order);
            
            orderGenerationSystem.generateNextOrder(world);
        }

        lastOrderState = currentOrderState;
    }

    @Override
    public void render(EcsWorld world) {
        // No rendering for order completion
    }

    private void logOrderCompletion() {
        System.out.println("\n========================================");
        System.out.println("[OrderCompletionSystem] Order completed!");
        System.out.println("========================================\n");
    }

    private void logLevelAdvance(OrderComponent order) {
        System.out.println("[OrderCompletionSystem] Advancing to Level " + order.currentLevel);
    }

    private OrderComponent getCachedOrder(EcsWorld world) {
        if (cachedOrderEntityId != -1) {
            OrderComponent cached = world.getComponent(cachedOrderEntityId, OrderComponent.class);
            if (cached != null) {
                return cached;
            }
        }

        for (int entityId : world.getActiveEntityIds()) {
            OrderComponent order = world.getComponent(entityId, OrderComponent.class);
            if (order != null) {
                cachedOrderEntityId = entityId;
                return order;
            }
        }

        return null;
    }

    private OrderBoxComponent getCachedOrderBox(EcsWorld world) {
        if (cachedOrderBoxEntityId != -1) {
            OrderBoxComponent cached = world.getComponent(cachedOrderBoxEntityId, OrderBoxComponent.class);
            if (cached != null) {
                return cached;
            }
        }

        for (int entityId : world.getActiveEntityIds()) {
            OrderBoxComponent orderBox = world.getComponent(entityId, OrderBoxComponent.class);
            if (orderBox != null) {
                cachedOrderBoxEntityId = entityId;
                return orderBox;
            }
        }

        return null;
    }
}
