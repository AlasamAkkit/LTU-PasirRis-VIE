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

        int breadRequired = order.currentOrder.getOrDefault("bread", 0);
        int milkRequired = order.currentOrder.getOrDefault("milk", 0);
        int applesRequired = order.currentOrder.getOrDefault("apples", 0);
        int breadDelivered = order.deliveredItems.getOrDefault("bread", 0);
        int milkDelivered = order.deliveredItems.getOrDefault("milk", 0);
        int applesDelivered = order.deliveredItems.getOrDefault("apples", 0);
        int timeRemaining = (int) Math.ceil(order.timeRemainingSeconds);

        if (order.currentLevel != lastLevel
                || breadRequired != lastBreadRequired
                || milkRequired != lastMilkRequired
                || applesRequired != lastApplesRequired
                || breadDelivered != lastBreadDelivered
                || milkDelivered != lastMilkDelivered
                || applesDelivered != lastApplesDelivered
                || timeRemaining != lastDisplayedTimeRemaining) {
            cachedOrderText = buildOrderText(order.currentLevel, breadDelivered, breadRequired, milkDelivered,
                    milkRequired, applesDelivered, applesRequired, timeRemaining);
            lastLevel = order.currentLevel;
            lastBreadRequired = breadRequired;
            lastMilkRequired = milkRequired;
            lastApplesRequired = applesRequired;
            lastBreadDelivered = breadDelivered;
            lastMilkDelivered = milkDelivered;
            lastApplesDelivered = applesDelivered;
            lastDisplayedTimeRemaining = timeRemaining;
            
            // Print order state to console for debugging
            logOrderState(order.currentLevel, breadDelivered, breadRequired, milkDelivered,
                    milkRequired, applesDelivered, applesRequired);
        }

        // Update window title with order information
        updateWindowTitle();
    }

    private void logOrderState(int level, int breadDelivered, int breadRequired, int milkDelivered,
            int milkRequired, int applesDelivered, int applesRequired) {
        System.out.println("\n[ORDER STATUS]");
        System.out.println("  Level: " + level);
        System.out.println("  Bread: " + breadDelivered + "/" + breadRequired);
        System.out.println("  Milk: " + milkDelivered + "/" + milkRequired);
        System.out.println("  Apples: " + applesDelivered + "/" + applesRequired);
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

    private String buildOrderText(int level, int breadDelivered, int breadRequired, int milkDelivered,
            int milkRequired, int applesDelivered, int applesRequired, int timeRemaining) {
        return new StringBuilder(96)
                .append("Level ").append(level).append(" Order:\n")
                .append("Time: ").append(timeRemaining).append("s\n")
                .append("Bread: ").append(breadDelivered).append('/').append(breadRequired).append('\n')
                .append("Milk: ").append(milkDelivered).append('/').append(milkRequired).append('\n')
                .append("Apples: ").append(applesDelivered).append('/').append(applesRequired)
                .toString();
    }
}
