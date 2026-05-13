package engine.systems;

import engine.components.ColliderComponent;
import engine.components.InputComponent;
import engine.components.InventoryComponent;
import engine.components.InteractableComponent;
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
            boolean isHoldingItem = !inventory.heldEntityIds.isEmpty();

            for (int candidateId : world.getActiveEntityIds()) {
                if (candidateId == entityId) {
                    continue;
                }

                TransformComponent candidateTransform = world.getComponent(candidateId, TransformComponent.class);
                if (candidateTransform == null) {
                    continue;
                }

                ColliderComponent collider = world.getComponent(candidateId, ColliderComponent.class);
                InteractableComponent interactableComponent = world.getComponent(candidateId, InteractableComponent.class);
                ProductComponent product = world.getComponent(candidateId, ProductComponent.class);
                OrderBoxComponent orderBox = world.getComponent(candidateId, OrderBoxComponent.class);

                if (interactableComponent != null && !interactableComponent.enabled) {
                    continue;
                }
                if (product != null && !product.availableInWorld) {
                    continue;
                }
                if (orderBox != null && orderBox.complete) {
                    continue;
                }

                boolean interactable = isHoldingItem ? orderBox != null : product != null || interactableComponent != null;
                if (!interactable) {
                    continue;
                }

                float interactionRadius = collider != null ? collider.interactionRadius : 1.5f;
                float distanceSquared = transform.position.distanceSquared(candidateTransform.position);
                if (distanceSquared <= interactionRadius * interactionRadius && distanceSquared < bestDistance) {
                    bestDistance = distanceSquared;
                    selectedEntityId = candidateId;
                    if (product != null) {
                        interactionMode = "pickup";
                    } else if (orderBox != null) {
                        interactionMode = "place";
                    } else {
                        interactionMode = interactableComponent.interactionMode;
                    }
                }
            }

            input.selectedInteractableEntityId = selectedEntityId;
            input.canInteract = selectedEntityId != -1;
            input.interactionMode = interactionMode;
        }
    }
}
