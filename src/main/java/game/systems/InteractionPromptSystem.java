package game.systems;

import engine.components.DialogueChoiceComponent;
import engine.components.InputComponent;
import engine.components.InventoryComponent;
import engine.components.InteractableComponent;
import engine.components.InteractionPromptComponent;
import engine.components.MessComponent;
import engine.components.OrderBoxComponent;
import engine.components.ProductComponent;
import engine.components.TransformComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;

public final class InteractionPromptSystem implements GameSystem {
    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        for (int entityId : world.getActiveEntityIds()) {
            InputComponent input = world.getComponent(entityId, InputComponent.class);
            InventoryComponent inventory = world.getComponent(entityId, InventoryComponent.class);
            TransformComponent transform = world.getComponent(entityId, TransformComponent.class);
            if (input == null || inventory == null || transform == null) {
                continue;
            }

            InteractionPromptComponent prompt = world.getComponent(entityId, InteractionPromptComponent.class);
            if (prompt == null) {
                prompt = world.addComponent(entityId, new InteractionPromptComponent());
            }

            prompt.visible = true;
            prompt.title = "INTERACTABLE";
            prompt.body = buildPromptBody(world, input, inventory);
            return;
        }
    }

    private String buildPromptBody(EcsWorld world, InputComponent input, InventoryComponent inventory) {
        int targetEntityId = input.selectedInteractableEntityId;
        if (targetEntityId == -1) {
            return "Nothing nearby";
        }

        DialogueChoiceComponent dialogue = world.getComponent(targetEntityId, DialogueChoiceComponent.class);
        if (dialogue != null && dialogue.visible) {
            return buildDialoguePrompt(dialogue);
        }

        ProductComponent product = world.getComponent(targetEntityId, ProductComponent.class);
        if (product != null && product.availableInWorld) {
            String productType = product.productType != null ? product.productType : "item";
            return "Press E to pick up " + productType;
        }

        OrderBoxComponent orderBox = world.getComponent(targetEntityId, OrderBoxComponent.class);
        if (orderBox != null) {
            if (inventory.heldEntityIds.isEmpty()) {
                return "Hold an item to deliver";
            }
            return "Press E to place held item";
        }

        MessComponent mess = world.getComponent(targetEntityId, MessComponent.class);
        if (mess != null) {
            return "Press E to clean mess";
        }

        InteractableComponent interactable = world.getComponent(targetEntityId, InteractableComponent.class);
        if (interactable != null && interactable.prompt != null && !interactable.prompt.isEmpty()) {
            if ("talk".equals(interactable.interactionMode)) {
                return "Press E to ask assistant";
            }
            return "Press E to " + interactable.prompt.toLowerCase();
        }

        if (input.canInteract) {
            return "Press E to interact";
        }

        return "Nothing nearby";
    }

    private String buildDialoguePrompt(DialogueChoiceComponent dialogue) {
        StringBuilder text = new StringBuilder();
        String title = dialogue.title != null && !dialogue.title.isEmpty() ? dialogue.title : "Choose one";
        text.append(title);

        if (dialogue.choiceLabels.length == 0) {
            text.append("\nUse 1-3 to choose");
            return text.toString();
        }

        text.append("\nUse 1-3 to choose");
        for (String choiceLabel : dialogue.choiceLabels) {
            if (choiceLabel == null || choiceLabel.isEmpty()) {
                continue;
            }
            text.append("\n").append(choiceLabel);
        }
        return text.toString();
    }
}