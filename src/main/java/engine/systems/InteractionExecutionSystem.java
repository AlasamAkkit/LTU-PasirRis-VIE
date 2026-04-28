package engine.systems;

import engine.components.InputComponent;
import engine.components.InventoryComponent;
import engine.components.OrderBoxComponent;
import engine.components.ProductComponent;
import engine.components.RenderComponent;
import engine.components.TransformComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;

public final class InteractionExecutionSystem implements GameSystem {
    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        for (int entityId : world.getActiveEntityIds()) {
            InputComponent input = world.getComponent(entityId, InputComponent.class);
            InventoryComponent inventory = world.getComponent(entityId, InventoryComponent.class);
            TransformComponent playerTransform = world.getComponent(entityId, TransformComponent.class);

            if (input == null || inventory == null || playerTransform == null) {
                continue;
            }

            if (input.dropPressed) {
                dropHeldItem(world, inventory, playerTransform);
            }

            if (input.interactPressed && input.selectedInteractableEntityId != -1) {
                int targetEntityId = input.selectedInteractableEntityId;
                ProductComponent product = world.getComponent(targetEntityId, ProductComponent.class);
                OrderBoxComponent orderBox = world.getComponent(targetEntityId, OrderBoxComponent.class);

                if (product != null) {
                    pickUpProduct(world, entityId, targetEntityId, inventory, product);
                } else if (orderBox != null) {
                    placeHeldItemIntoOrderBox(world, entityId, inventory, orderBox);
                }
            }
        }
    }

    private void pickUpProduct(EcsWorld world, int holderEntityId, int productEntityId, InventoryComponent inventory, ProductComponent product) {
        if (inventory.isFull() || inventory.contains(productEntityId) || !product.availableInWorld) {
            return;
        }

        inventory.heldEntityIds.add(productEntityId);
        product.holderEntityId = holderEntityId;
        product.availableInWorld = false;

        RenderComponent render = world.getComponent(productEntityId, RenderComponent.class);
        if (render != null) {
            render.visible = false;
        }
    }

    private void placeHeldItemIntoOrderBox(EcsWorld world, int holderEntityId, InventoryComponent inventory, OrderBoxComponent orderBox) {
        if (inventory.heldEntityIds.isEmpty()) {
            return;
        }

        int heldEntityId = inventory.heldEntityIds.get(0);
        ProductComponent product = world.getComponent(heldEntityId, ProductComponent.class);
        if (product == null) {
            return;
        }

        if (!orderBox.requiredProductTypes.contains(product.productType)) {
            return;
        }

        inventory.heldEntityIds.remove(0);
        orderBox.receivedItemEntityIds.add(heldEntityId);
        product.holderEntityId = holderEntityId;
        product.availableInWorld = false;
    }

    private void dropHeldItem(EcsWorld world, InventoryComponent inventory, TransformComponent holderTransform) {
        if (inventory.heldEntityIds.isEmpty()) {
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
            productTransform.position.set(holderTransform.position);
        }

        RenderComponent render = world.getComponent(heldEntityId, RenderComponent.class);
        if (render != null) {
            render.visible = true;
        }
    }
}