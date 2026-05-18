package engine.components;

import java.util.HashMap;
import java.util.Map;

/**
 * OrderComponent tracks the current order state and game level.
 * 
 * Stores:
 * - Current level (increments as orders are completed)
 * - Current order requirements (quantities of each product type)
 * - Delivered items count (how many of each product type are in the box)
 * 
 * This is a game-specific component that represents the supermarket gameplay state.
 * There is typically one OrderComponent per game session.
 */
public final class OrderComponent {
    public int currentLevel = 1;
    
    // Current order requirements: product type -> required quantity
    public final Map<String, Integer> currentOrder = new HashMap<>();
    
    // Delivered items: product type -> delivered quantity
    public final Map<String, Integer> deliveredItems = new HashMap<>();
    
    // Flag to indicate order is complete (used by UI system)
    public boolean orderComplete = false;
    public float timeLimitSeconds;
    public float timeRemainingSeconds;

    public OrderComponent() {
        // Initialize with empty order - will be populated by OrderGenerationSystem
        currentOrder.put("bread", 0);
        currentOrder.put("milk", 0);
        currentOrder.put("apples", 0);
        
        resetDelivered();
    }

    /**
     * Reset delivered item counts to 0.
     * Called when a new order is generated.
     */
    public void resetDelivered() {
        deliveredItems.put("bread", 0);
        deliveredItems.put("milk", 0);
        deliveredItems.put("apples", 0);
    }

    /**
     * Get total required items across all product types.
     */
    public int getTotalRequired() {
        return currentOrder.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Get total delivered items across all product types.
     */
    public int getTotalDelivered() {
        return deliveredItems.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Check if order is completely fulfilled.
     */
    public boolean isOrderFulfilled() {
        for (String productType : currentOrder.keySet()) {
            int required = currentOrder.getOrDefault(productType, 0);
            int delivered = deliveredItems.getOrDefault(productType, 0);
            if (delivered < required) {
                return false;
            }
        }
        return true;
    }

    /**
     * Generate order display string for UI.
     * Format: "Level X Order:\nBread: a/b\nMilk: c/d\nApples: e/f"
     */
    public String getOrderDisplayText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Level ").append(currentLevel).append(" Order:\n");
        
        for (String productType : new String[]{"bread", "milk", "apples"}) {
            int delivered = deliveredItems.getOrDefault(productType, 0);
            int required = currentOrder.getOrDefault(productType, 0);
            sb.append(capitalize(productType)).append(": ").append(delivered).append("/").append(required).append("\n");
        }
        
        return sb.toString().trim();
    }

    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
