package engine.systems;

import engine.components.OrderComponent;
import engine.components.OrderBoxComponent;
import engine.components.TaskComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;

public final class TaskSystem implements GameSystem {
    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        OrderComponent order = null;
        OrderBoxComponent orderBox = null;
        TaskComponent task = null;

        for (int entityId : world.getActiveEntityIds()) {
            if (order == null) {
                order = world.getComponent(entityId, OrderComponent.class);
            }
            if (orderBox == null) {
                orderBox = world.getComponent(entityId, OrderBoxComponent.class);
            }
            if (task == null) {
                task = world.getComponent(entityId, TaskComponent.class);
            }
            if (order != null && orderBox != null && task != null) {
                break;
            }
        }

        if (order == null || orderBox == null || task == null) {
            return;
        }

        int requiredTotal = order.getTotalRequired();
        if (requiredTotal <= 0) {
            task.progress = 0.0f;
            task.complete = false;
            task.status = "in-progress";
            orderBox.complete = false;
            return;
        }

        int deliveredTotal = order.getTotalDelivered();
        task.progress = Math.min(1.0f, (float) deliveredTotal / requiredTotal);
        task.complete = order.isOrderFulfilled();
        task.status = task.complete ? "complete" : "in-progress";
        orderBox.complete = task.complete;
    }
}