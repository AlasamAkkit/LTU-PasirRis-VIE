package engine.systems;

import java.util.Random;

import engine.components.OrderBoxComponent;
import engine.components.OrderComponent;
import engine.components.TaskComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;
import game.config.WorldConfig;

/**
 * OrderGenerationSystem generates new orders based on the current level.
 * 
 * Responsibilities:
 * - Generate random quantities for bread, milk, and apples
 * - Limit quantities based on the current level (Level N allows max quantity N)
 * - Update the OrderComponent with new requirements
 * - Log order generation events for debugging
 * 
 * This system runs once at startup and whenever a new order is needed.
 * Integration point: called from OrderCompletionSystem when level advances.
 */
public final class OrderGenerationSystem implements GameSystem {
    private final Random random = new Random();
    private final WorldConfig.OrderRules orderRules;
    private boolean hasGeneratedInitialOrder = false;
    private int cachedOrderEntityId = -1;
    private int cachedOrderBoxEntityId = -1;

    public OrderGenerationSystem(WorldConfig.OrderRules orderRules) {
        this.orderRules = orderRules;
    }

    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        OrderComponent order = getCachedOrder(world);
        if (order == null) {
            return;
        }

        if (!hasGeneratedInitialOrder) {
            hasGeneratedInitialOrder = true;
            generateNewOrder(world, order);
            logOrderGeneration(order);
        }
    }

    @Override
    public void render(EcsWorld world) {
        // No rendering for order generation
    }

    /**
     * Generate a new order with random quantities based on current level.
     * Level N allows quantities from 1 to N (inclusive).
     */
    private void generateNewOrder(EcsWorld world, OrderComponent order) {
        int maxQuantity = order.currentLevel;
        
        // Random quantity: 1 to maxQuantity inclusive
        order.currentOrder.put("bread", 1 + random.nextInt(maxQuantity));
        order.currentOrder.put("milk", 1 + random.nextInt(maxQuantity));
        order.currentOrder.put("apples", 1 + random.nextInt(maxQuantity));
        
        // Reset delivered items for new order
        order.resetDelivered();
        order.orderComplete = false;
        order.timeLimitSeconds = orderRules.timeLimitForLevel(order.currentLevel);
        order.timeRemainingSeconds = order.timeLimitSeconds;

        OrderBoxComponent orderBox = getCachedOrderBox(world);
        if (orderBox != null) {
            orderBox.receivedItemEntityIds.clear();
            orderBox.complete = false;
        }

        TaskComponent task = getCachedTask(world);
        if (task != null) {
            task.progress = 0.0f;
            task.complete = false;
            task.status = "in-progress";
        }
    }

    /**
     * Trigger generation of a new order.
     * Called by OrderCompletionSystem when level advances.
     */
    public void generateNextOrder(EcsWorld world) {
        OrderComponent order = getCachedOrder(world);
        if (order != null) {
            generateNewOrder(world, order);
            logOrderGeneration(order);
        }
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

    private TaskComponent getCachedTask(EcsWorld world) {
        for (int entityId : world.getActiveEntityIds()) {
            TaskComponent task = world.getComponent(entityId, TaskComponent.class);
            if (task != null) {
                return task;
            }
        }
        return null;
    }

    private void logOrderGeneration(OrderComponent order) {
        String bread = String.valueOf(order.currentOrder.get("bread"));
        String milk = String.valueOf(order.currentOrder.get("milk"));
        String apples = String.valueOf(order.currentOrder.get("apples"));
        System.out.println("\n========================================");
        System.out.println("[OrderGenerationSystem] Generated Level " + order.currentLevel 
                         + " order: Bread=" + bread + ", Milk=" + milk + ", Apple=" + apples);
        System.out.println("========================================\n");
    }
}
