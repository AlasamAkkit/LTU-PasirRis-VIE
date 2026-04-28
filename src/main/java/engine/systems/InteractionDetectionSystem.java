package engine.systems;

import engine.components.ColliderComponent;
import engine.components.InputComponent;
import engine.components.InventoryComponent;
import engine.components.OrderBoxComponent;
import engine.components.ProductComponent;
import engine.components.TransformComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;

public final class InteractionDetectionSystem implements GameSystem {
    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        for (int entityId : world.getActiveEntityIds()) {
            InputComponent input = world.getComponent(entityId, InputComponent.class);
            TransformComponent transform = world.getComponent(entityId, TransformComponent.class);
            InventoryComponent inventory = world.getComponent(entityId, InventoryComponent.class);

            if (input == null || transform == null || inventory == null) {
                continue;
            }

            int selectedEntityId = -1;
            String interactionMode = "none";
            float bestDistance = Float.MAX_VALUE;

            for (int candidateId : world.getActiveEntityIds()) {
                if (candidateId == entityId) {
                    continue;
                }

                TransformComponent candidateTransform = world.getComponent(candidateId, TransformComponent.class);
                if (candidateTransform == null) {
                    continue;
                }

                ColliderComponent collider = world.getComponent(candidateId, ColliderComponent.class);
                ProductComponent product = world.getComponent(candidateId, ProductComponent.class);
                OrderBoxComponent orderBox = world.getComponent(candidateId, OrderBoxComponent.class);

                boolean interactable = product != null || orderBox != null;
                if (!interactable) {
                    continue;
                }

                float interactionRadius = collider != null ? collider.interactionRadius : 1.5f;
                float distanceSquared = transform.position.distanceSquared(candidateTransform.position);
                if (distanceSquared <= interactionRadius * interactionRadius && distanceSquared < bestDistance) {
                    bestDistance = distanceSquared;
                    selectedEntityId = candidateId;
                    interactionMode = product != null ? "pickup" : "place";
                }
            }

            input.selectedInteractableEntityId = selectedEntityId;
            input.canInteract = selectedEntityId != -1;
            input.interactionMode = interactionMode;
        }
    }
}