package engine.systems;

import engine.components.OrderComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;
import engine.render.Window;

/**
 * OrderUISystem displays the current order state in the HUD.
 * 
 * Responsibilities:
 * - Read from OrderComponent to get current order and delivered items
 * - Generate order display text (Level, products, counts)
 * - Update window title or HUD with order information
 * - Only update text when order state changes (avoid per-frame string creation)
 * 
 * This system is purely for presentation and does not modify game state.
 * It caches the last displayed text to avoid unnecessary string allocations.
 */
public final class OrderUISystem implements GameSystem {
    private final Window window;
    private String cachedOrderText = "";
    private String lastDisplayedTitle = "";
    private int cachedOrderEntityId = -1;
    private int lastLevel = -1;
    private int lastBreadRequired = -1;
    private int lastMilkRequired = -1;
    private int lastApplesRequired = -1;
    private int lastBreadDelivered = -1;
    private int lastMilkDelivered = -1;
    private int lastApplesDelivered = -1;
    private int lastDisplayedTimeRemaining = -1;

    public OrderUISystem(Window window) {
        this.window = window;
    }

    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        OrderComponent order = getCachedOrder(world);

        if (order == null) {
            return;
        }

        int timeRemaining = (int) Math.ceil(order.timeRemainingSeconds);
        String nextOrderText = buildOrderText(order, timeRemaining);

        if (!nextOrderText.equals(cachedOrderText)) {
            cachedOrderText = nextOrderText;
            lastLevel = order.currentLevel;
            lastDisplayedTimeRemaining = timeRemaining;
            
            // Print order state to console for debugging
            logOrderState(order);
        }

        // Update window title with order information
        updateWindowTitle();
    }

    private void logOrderState(OrderComponent order) {
        System.out.println("\n[ORDER STATUS]");
        System.out.println("  Level: " + order.currentLevel);
        for (String productType : order.currentOrder.keySet()) {
            System.out.println("  " + capitalize(productType) + ": "
                    + order.deliveredItems.getOrDefault(productType, 0)
                    + "/"
                    + order.currentOrder.getOrDefault(productType, 0));
        }
        System.out.println();
    }

    @Override
    public void render(EcsWorld world) {
        // HUD text rendering is handled via window title update
        // In a future implementation with proper text rendering, this would
        // render text to top-right corner using a text shader
    }

    /**
     * Update the window title to include order information.
     * Only updates if the text has changed to avoid excessive title updates.
     */
    private void updateWindowTitle() {
        String newTitle = "LTU Pasir Ris VIE | " + cachedOrderText;
        
        if (!newTitle.equals(lastDisplayedTitle)) {
            window.setTitle(newTitle);
            lastDisplayedTitle = newTitle;
        }
    }

    /**
     * Get the current order display text.
     * Useful for debugging or external HUD rendering.
     */
    public String getOrderDisplayText() {
        return cachedOrderText;
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

    private String buildOrderText(OrderComponent order, int timeRemaining) {
        StringBuilder builder = new StringBuilder(96)
                .append("Level ").append(order.currentLevel).append(" Order:\n")
                .append("Time: ").append(timeRemaining).append("s");

        for (String productType : order.currentOrder.keySet()) {
            builder.append('\n')
                    .append(capitalize(productType))
                    .append(": ")
                    .append(order.deliveredItems.getOrDefault(productType, 0))
                    .append('/')
                    .append(order.currentOrder.getOrDefault(productType, 0));
        }

        return builder.toString();
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}
