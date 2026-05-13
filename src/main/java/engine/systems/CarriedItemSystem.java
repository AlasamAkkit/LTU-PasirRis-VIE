package engine.systems;

import engine.components.InventoryComponent;
import engine.components.TransformComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;

public final class CarriedItemSystem implements GameSystem {
    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        for (int holderEntityId : world.getActiveEntityIds()) {
            InventoryComponent inventory = world.getComponent(holderEntityId, InventoryComponent.class);
            TransformComponent holderTransform = world.getComponent(holderEntityId, TransformComponent.class);
            if (inventory == null || holderTransform == null) {
                continue;
            }

            for (int heldEntityId : inventory.heldEntityIds) {
                TransformComponent itemTransform = world.getComponent(heldEntityId, TransformComponent.class);
                if (itemTransform == null) {
                    continue;
                }

                itemTransform.position.set(holderTransform.position.x, holderTransform.position.y + 1.15f,
                        holderTransform.position.z);
                itemTransform.scale.set(0.28f, 0.28f, 0.28f);
            }
        }
    }
}
