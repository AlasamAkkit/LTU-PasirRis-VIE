package game.systems;

import engine.components.DialogueChoiceComponent;
import engine.components.InputComponent;
import engine.components.InventoryComponent;
import engine.components.NavigationAgentComponent;
import engine.components.OrderBoxComponent;
import engine.components.OrderComponent;
import engine.components.ProductComponent;
import engine.components.RenderComponent;
import engine.components.TransformComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;
import engine.math.Vector3;
import engine.systems.SpawnSystem;
import game.components.AssistantAgentComponent;
import game.config.WorldConfig;
import game.content.MessFactory;

import java.util.Random;

public final class AssistantAgentSystem implements GameSystem {
    private static final String IDLE = "idle";
    private static final String FIND_PRODUCT = "find-product";
    private static final String MOVE_TO_PRODUCT = "move-to-product";
    private static final String MOVE_TO_ORDER_BOX = "move-to-order-box";
    private static final String RETURN_HOME = "return-home";

    private final SpawnSystem spawnSystem = new SpawnSystem();
    private final WorldConfig.MessRules messRules;
    private final Random random = new Random();

    public AssistantAgentSystem(WorldConfig.MessRules messRules) {
        this.messRules = messRules;
    }

    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        for (int entityId : world.getActiveEntityIds()) {
            AssistantAgentComponent assistant = world.getComponent(entityId, AssistantAgentComponent.class);
            NavigationAgentComponent navigation = world.getComponent(entityId, NavigationAgentComponent.class);
            InventoryComponent inventory = world.getComponent(entityId, InventoryComponent.class);
            TransformComponent transform = world.getComponent(entityId, TransformComponent.class);
            if (assistant == null || navigation == null || inventory == null || transform == null) {
                continue;
            }

            consumeDialogueSelection(world, entityId, assistant, navigation, inventory, transform);

            if (assistant.assignmentActive
                    && isRequestedProductQuotaFulfilled(world, assistant.requestedProductType)
                    && inventory.heldEntityIds.isEmpty()
                    && !RETURN_HOME.equals(assistant.phase)) {
                setDestination(navigation, assistant.homePosition);
                assistant.phase = RETURN_HOME;
                setFeedback(world, assistant.lastInteractorEntityId, "Assistant returning home");
                continue;
            }

            if (FIND_PRODUCT.equals(assistant.phase)) {
                int productEntityId = findNearestAvailableProduct(world, transform.position, assistant.requestedProductType);
                if (productEntityId == -1) {
                    setFeedback(world, assistant.lastInteractorEntityId,
                            "Assistant cannot find " + assistant.requestedProductType);
                    clearAssignment(assistant, navigation);
                    continue;
                }

                assistant.targetProductEntityId = productEntityId;
                setTargetEntity(world, navigation, productEntityId);
                assistant.phase = MOVE_TO_PRODUCT;
                continue;
            }

            if (MOVE_TO_PRODUCT.equals(assistant.phase)) {
                if (!isProductAvailable(world, assistant.targetProductEntityId, assistant.requestedProductType)) {
                    assistant.phase = FIND_PRODUCT;
                    continue;
                }

                if (navigation.reachedDestination) {
                    pickUpProduct(world, entityId, assistant.targetProductEntityId, inventory);
                    assistant.targetOrderBoxEntityId = findOrderBox(world);
                    if (assistant.targetOrderBoxEntityId == -1) {
                        setFeedback(world, assistant.lastInteractorEntityId, "Assistant cannot find the order box");
                        setDestination(navigation, assistant.homePosition);
                        assistant.phase = RETURN_HOME;
                        continue;
                    }

                    setTargetEntity(world, navigation, assistant.targetOrderBoxEntityId);
                    assistant.phase = MOVE_TO_ORDER_BOX;
                }
                continue;
            }

            if (MOVE_TO_ORDER_BOX.equals(assistant.phase)) {
                if (navigation.reachedDestination) {
                    deliverHeldProduct(world, assistant.targetOrderBoxEntityId, inventory);
                    setFeedback(world, assistant.lastInteractorEntityId,
                            "Assistant delivered " + assistant.requestedProductType);

                    if (isRequestedProductQuotaFulfilled(world, assistant.requestedProductType)) {
                        setDestination(navigation, assistant.homePosition);
                        assistant.phase = RETURN_HOME;
                    } else {
                        assistant.phase = FIND_PRODUCT;
                    }
                }
                continue;
            }

            if (RETURN_HOME.equals(assistant.phase) && navigation.reachedDestination) {
                if (assistant.assignmentActive
                        && !isRequestedProductQuotaFulfilled(world, assistant.requestedProductType)) {
                    assistant.phase = FIND_PRODUCT;
                } else {
                    clearAssignment(assistant, navigation);
                }
            }
        }
    }

    private void consumeDialogueSelection(EcsWorld world, int assistantEntityId, AssistantAgentComponent assistant,
            NavigationAgentComponent navigation, InventoryComponent inventory, TransformComponent assistantTransform) {
        DialogueChoiceComponent dialogue = world.getComponent(assistantEntityId, DialogueChoiceComponent.class);
        if (dialogue == null || dialogue.selectedChoiceValue == null || dialogue.selectedChoiceValue.isEmpty()) {
            return;
        }

        assistant.lastInteractorEntityId = dialogue.interactingEntityId;
        String requestedProductType = dialogue.selectedChoiceValue;
        dialogue.selectedChoiceIndex = -1;
        dialogue.selectedChoiceValue = "";

        if (!inventory.heldEntityIds.isEmpty()) {
            dropHeldItem(world, inventory, assistantTransform);
        }

        assistant.requestedProductType = requestedProductType;
        assistant.assignmentActive = true;
        assistant.targetProductEntityId = -1;
        assistant.targetOrderBoxEntityId = -1;
        clearNavigation(navigation);
        assistant.phase = FIND_PRODUCT;
        setFeedback(world, assistant.lastInteractorEntityId, "Assistant assigned to " + requestedProductType);
    }

    private int findNearestAvailableProduct(EcsWorld world, Vector3 position, String productType) {
        int selectedEntityId = -1;
        float bestDistance = Float.MAX_VALUE;

        for (int entityId : world.getActiveEntityIds()) {
            ProductComponent product = world.getComponent(entityId, ProductComponent.class);
            TransformComponent transform = world.getComponent(entityId, TransformComponent.class);
            if (product == null || transform == null) {
                continue;
            }
            if (!product.availableInWorld || product.holderEntityId != -1 || !productType.equals(product.productType)) {
                continue;
            }

            float distanceSquared = position.distanceSquared(transform.position);
            if (distanceSquared < bestDistance) {
                bestDistance = distanceSquared;
                selectedEntityId = entityId;
            }
        }

        return selectedEntityId;
    }

    private boolean isProductAvailable(EcsWorld world, int productEntityId, String productType) {
        if (productEntityId == -1 || !world.isAlive(productEntityId)) {
            return false;
        }
        ProductComponent product = world.getComponent(productEntityId, ProductComponent.class);
        return product != null && product.availableInWorld && product.holderEntityId == -1
                && productType.equals(product.productType);
    }

    private boolean isRequestedProductQuotaFulfilled(EcsWorld world, String productType) {
        if (productType == null || productType.isEmpty()) {
            return true;
        }
        OrderComponent order = findOrder(world);
        if (order == null) {
            return false;
        }
        int required = order.currentOrder.getOrDefault(productType, 0);
        int delivered = order.deliveredItems.getOrDefault(productType, 0);
        return delivered >= required;
    }

    private OrderComponent findOrder(EcsWorld world) {
        for (int entityId : world.getActiveEntityIds()) {
            OrderComponent order = world.getComponent(entityId, OrderComponent.class);
            if (order != null) {
                return order;
            }
        }
        return null;
    }

    private int findOrderBox(EcsWorld world) {
        for (int entityId : world.getActiveEntityIds()) {
            if (world.hasComponent(entityId, OrderBoxComponent.class)) {
                return entityId;
            }
        }
        return -1;
    }

    private void pickUpProduct(EcsWorld world, int assistantEntityId, int productEntityId, InventoryComponent inventory) {
        if (inventory.heldEntityIds.size() >= inventory.maxCapacity || inventory.heldEntityIds.contains(productEntityId)) {
            return;
        }

        ProductComponent product = world.getComponent(productEntityId, ProductComponent.class);
        if (product == null || !product.availableInWorld) {
            return;
        }

        TransformComponent productTransform = world.getComponent(productEntityId, TransformComponent.class);
        if (productTransform != null) {
            MessFactory.maybeSpawnOnProductPickup(world, spawnSystem, messRules, random, productTransform.position);
        }

        if (product.respawnOnPickup) {
            spawnSystem.spawnShelfProduct(world, product.productType,
                    new Vector3(product.respawnX, product.respawnY, product.respawnZ));
            product.respawnOnPickup = false;
        }

        inventory.heldEntityIds.add(productEntityId);
        product.holderEntityId = assistantEntityId;
        product.availableInWorld = false;

        RenderComponent render = world.getComponent(productEntityId, RenderComponent.class);
        if (render != null) {
            render.visible = true;
        }
    }

    private void deliverHeldProduct(EcsWorld world, int orderBoxEntityId, InventoryComponent inventory) {
        if (inventory.heldEntityIds.isEmpty()) {
            return;
        }

        OrderBoxComponent orderBox = world.getComponent(orderBoxEntityId, OrderBoxComponent.class);
        if (orderBox == null) {
            return;
        }

        int heldEntityId = inventory.heldEntityIds.remove(0);
        ProductComponent product = world.getComponent(heldEntityId, ProductComponent.class);
        if (product != null) {
            product.holderEntityId = -1;
            product.availableInWorld = false;
        }

        RenderComponent render = world.getComponent(heldEntityId, RenderComponent.class);
        if (render != null) {
            render.visible = false;
        }

        orderBox.receivedItemEntityIds.add(heldEntityId);
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
            productTransform.position.set(holderTransform.position.x, 0.35f, holderTransform.position.z - 0.75f);
            productTransform.scale.set(0.35f, 0.35f, 0.35f);
        }

        RenderComponent render = world.getComponent(heldEntityId, RenderComponent.class);
        if (render != null) {
            render.visible = true;
        }
    }

    private void setTargetEntity(EcsWorld world, NavigationAgentComponent navigation, int targetEntityId) {
        navigation.targetEntityId = targetEntityId;
        TransformComponent targetTransform = world.getComponent(targetEntityId, TransformComponent.class);
        if (targetTransform != null) {
            navigation.destination.set(targetTransform.position);
        }
        navigation.hasDestination = true;
        navigation.pathDirty = true;
        navigation.pathAvailable = false;
        navigation.reachedDestination = false;
        navigation.path.clear();
        navigation.currentWaypointIndex = 0;
    }

    private void setDestination(NavigationAgentComponent navigation, Vector3 destination) {
        navigation.targetEntityId = -1;
        navigation.destination.set(destination);
        navigation.hasDestination = true;
        navigation.pathDirty = true;
        navigation.pathAvailable = false;
        navigation.reachedDestination = false;
        navigation.path.clear();
        navigation.currentWaypointIndex = 0;
    }

    private void clearAssignment(AssistantAgentComponent assistant, NavigationAgentComponent navigation) {
        assistant.phase = IDLE;
        assistant.assignmentActive = false;
        assistant.requestedProductType = "";
        assistant.targetProductEntityId = -1;
        assistant.targetOrderBoxEntityId = -1;
        clearNavigation(navigation);
    }

    private void clearNavigation(NavigationAgentComponent navigation) {
        navigation.targetEntityId = -1;
        navigation.hasDestination = false;
        navigation.pathDirty = false;
        navigation.pathAvailable = false;
        navigation.reachedDestination = true;
        navigation.path.clear();
        navigation.currentWaypointIndex = 0;
    }

    private void setFeedback(EcsWorld world, int entityId, String message) {
        if (entityId == -1) {
            return;
        }

        InputComponent input = world.getComponent(entityId, InputComponent.class);
        if (input == null) {
            return;
        }

        input.feedbackMessage = message;
        input.feedbackSecondsRemaining = 2.5f;
    }
}
