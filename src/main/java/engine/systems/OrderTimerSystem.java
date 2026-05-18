package engine.systems;

import engine.components.OrderComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;
import game.config.WorldConfig;

public final class OrderTimerSystem implements GameSystem {
    private final WorldConfig.OrderRules orderRules;
    private final OrderGenerationSystem orderGenerationSystem;
    private int cachedOrderEntityId = -1;

    public OrderTimerSystem(WorldConfig.OrderRules orderRules, OrderGenerationSystem orderGenerationSystem) {
        this.orderRules = orderRules;
        this.orderGenerationSystem = orderGenerationSystem;
    }

    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        OrderComponent order = getCachedOrder(world);
        if (order == null || order.timeRemainingSeconds <= 0.0f || order.isOrderFulfilled()) {
            return;
        }

        order.timeRemainingSeconds = Math.max(0.0f, order.timeRemainingSeconds - deltaSeconds);
        if (order.timeRemainingSeconds > 0.0f) {
            return;
        }

        if (WorldConfig.OrderRules.RESTART_FROM_LEVEL_1.equals(orderRules.failureBehavior)) {
            order.currentLevel = 1;
        }

        System.out.println("[OrderTimerSystem] Time expired. Failure behavior: " + orderRules.failureBehavior);
        System.out.flush();
        orderGenerationSystem.generateNextOrder(world);
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
}
