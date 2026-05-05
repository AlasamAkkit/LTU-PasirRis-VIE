package engine.systems;

import engine.components.OrderBoxComponent;
import engine.components.ProductComponent;
import engine.components.TaskComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;

import java.util.HashMap;
import java.util.Map;

public final class TaskSystem implements GameSystem {
    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        for (int entityId : world.getActiveEntityIds()) {
            OrderBoxComponent orderBox = world.getComponent(entityId, OrderBoxComponent.class);
            TaskComponent task = world.getComponent(entityId, TaskComponent.class);

            if (orderBox == null || task == null) {
                continue;
            }

            if (orderBox.requiredProductTypes.isEmpty()) {
                task.progress = 1.0f;
                task.complete = true;
                task.status = "complete";
                orderBox.complete = true;
                continue;
            }

            Map<String, Integer> receivedCounts = new HashMap<>();
            for (int receivedEntityId : orderBox.receivedItemEntityIds) {
                ProductComponent product = world.getComponent(receivedEntityId, ProductComponent.class);
                if (product == null) {
                    continue;
                }
                receivedCounts.merge(product.productType, 1, Integer::sum);
            }

            int satisfiedItems = 0;
            for (String requiredType : orderBox.requiredProductTypes) {
                int remaining = receivedCounts.getOrDefault(requiredType, 0);
                if (remaining > 0) {
                    receivedCounts.put(requiredType, remaining - 1);
                    satisfiedItems++;
                }
            }

            task.progress = (float) satisfiedItems / orderBox.requiredProductTypes.size();
            task.complete = satisfiedItems == orderBox.requiredProductTypes.size();
            task.status = task.complete ? "complete" : "in-progress";
            orderBox.complete = task.complete;
        }
    }
}