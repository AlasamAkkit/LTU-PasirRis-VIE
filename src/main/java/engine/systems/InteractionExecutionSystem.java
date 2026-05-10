package engine.systems;

import engine.components.InputComponent;
import engine.components.InventoryComponent;
import engine.components.OrderBoxComponent;
import engine.components.ProductComponent;
import engine.components.RenderComponent;
import engine.components.TransformComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;
import engine.math.Vector3;

public final class InteractionExecutionSystem implements GameSystem {
    private final SpawnSystem spawnSystem = new SpawnSystem();
    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        for (int entityId : world.getActiveEntityIds()) {
            InputComponent input = world.getComponent(entityId, InputComponent.class);
            InventoryComponent inventory = world.getComponent(entityId, InventoryComponent.class);
            TransformComponent playerTransform = world.getComponent(entityId, TransformComponent.class);

            if (input == null || inventory == null || playerTransform == null) {
                continue;
            }

            updateFeedbackTimer(input, deltaSeconds);
            updateHeldItemVisual(world, inventory, playerTransform);

            if (input.dropPressed) {
                System.out.println("[InteractionExecutionSystem] Drop pressed");
                System.out.flush();
                dropHeldItem(world, input, inventory, playerTransform);
            }

            if (input.interactPressed && input.selectedInteractableEntityId != -1) {
                System.out.println("[InteractionExecutionSystem] Interact pressed - targetId: " + input.selectedInteractableEntityId);
                System.out.flush();
                int targetEntityId = input.selectedInteractableEntityId;
                ProductComponent product = world.getComponent(targetEntityId, ProductComponent.class);
                OrderBoxComponent orderBox = world.getComponent(targetEntityId, OrderBoxComponent.class);

                if (product != null) {
                    pickUpProduct(world, input, entityId, targetEntityId, inventory, product);
                } else if (orderBox != null) {
                    placeHeldItemIntoOrderBox(world, input, inventory, orderBox);
                }
            }
        }
    }

    private void pickUpProduct(EcsWorld world, InputComponent input, int holderEntityId, int productEntityId, InventoryComponent inventory, ProductComponent product) {
        if (inventory.isFull() || inventory.contains(productEntityId) || !product.availableInWorld) {
            setFeedback(input, "Already carrying an item");
            return;
        }

        if (product.respawnOnPickup) {
            Vector3 respawnPosition = new Vector3(product.respawnX, product.respawnY, product.respawnZ);
            spawnSystem.spawnShelfProduct(world, product.productType, respawnPosition);
            product.respawnOnPickup = false;
        }

        inventory.heldEntityIds.add(productEntityId);
        product.holderEntityId = holderEntityId;
        product.availableInWorld = false;

        RenderComponent render = world.getComponent(productEntityId, RenderComponent.class);
        if (render != null) {
            render.visible = true;
        }
        setFeedback(input, "Picked up " + product.productType);
        System.out.println("[Interaction] Picked up product: " + product.productType);
        System.out.flush();
    }

    private void placeHeldItemIntoOrderBox(EcsWorld world, InputComponent input, InventoryComponent inventory, OrderBoxComponent orderBox) {
        if (inventory.heldEntityIds.isEmpty()) {
            setFeedback(input, "Pick up an item first");
            return;
        }

        int heldEntityId = inventory.heldEntityIds.get(0);
        ProductComponent product = world.getComponent(heldEntityId, ProductComponent.class);
        if (product == null) {
            return;
        }

        inventory.heldEntityIds.remove(0);
        orderBox.receivedItemEntityIds.add(heldEntityId);
        product.holderEntityId = -1;
        product.availableInWorld = false;

        RenderComponent render = world.getComponent(heldEntityId, RenderComponent.class);
        if (render != null) {
            render.visible = false;
        }
        setFeedback(input, "Delivered " + product.productType + " - order complete");
        System.out.println("[Interaction] Placed product into order box: " + product.productType);
        System.out.flush();
    }

    private void dropHeldItem(EcsWorld world, InputComponent input, InventoryComponent inventory, TransformComponent holderTransform) {
        if (inventory.heldEntityIds.isEmpty()) {
            setFeedback(input, "Nothing to drop");
            return;
        }

        int heldEntityId = inventory.heldEntityIds.remove(inventory.heldEntityIds.size() - 1);
        ProductComponent product = world.getComponent(heldEntityId, ProductComponent.class);
        if (product != null) {
            product.holderEntityId = -1;
            product.availableInWorld = true;
        }

        TransformComponent productTransform = world.getComponent(heldEntityId, TransformComponent.class);
        if (productTransform != null) {
            productTransform.position.set(holderTransform.position.x, 0.35f, holderTransform.position.z - 0.75f);
            productTransform.scale.set(0.35f, 0.35f, 0.35f);
        }

        RenderComponent render = world.getComponent(heldEntityId, RenderComponent.class);
        if (render != null) {
            render.visible = true;
        }
        if (product != null) {
            setFeedback(input, "Dropped " + product.productType);
            System.out.println("[Interaction] Dropped product: " + product.productType);
            System.out.flush();
        }
    }

    private void updateHeldItemVisual(EcsWorld world, InventoryComponent inventory, TransformComponent holderTransform) {
        for (int heldEntityId : inventory.heldEntityIds) {
            TransformComponent productTransform = world.getComponent(heldEntityId, TransformComponent.class);
            if (productTransform == null) {
                continue;
            }
            productTransform.position.set(holderTransform.position.x, holderTransform.position.y + 1.15f, holderTransform.position.z);
            productTransform.scale.set(0.28f, 0.28f, 0.28f);
        }
    }

    private void updateFeedbackTimer(InputComponent input, float deltaSeconds) {
        if (input.feedbackSecondsRemaining <= 0.0f) {
            input.feedbackMessage = "";
            return;
        }
        input.feedbackSecondsRemaining -= deltaSeconds;
        if (input.feedbackSecondsRemaining <= 0.0f) {
            input.feedbackMessage = "";
        }
    }

    private void setFeedback(InputComponent input, String message) {
        input.feedbackMessage = message;
        input.feedbackSecondsRemaining = 2.5f;
    }
}
