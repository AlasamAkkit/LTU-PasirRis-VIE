package engine.systems;

import engine.components.DialogueChoiceComponent;
import engine.components.InputComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;

public final class DialogueInteractionSystem implements GameSystem {
    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        for (int entityId : world.getActiveEntityIds()) {
            InputComponent input = world.getComponent(entityId, InputComponent.class);
            if (input == null) {
                continue;
            }

            if (input.interactPressed && input.selectedInteractableEntityId != -1) {
                DialogueChoiceComponent dialogue = world.getComponent(input.selectedInteractableEntityId,
                        DialogueChoiceComponent.class);
                if (dialogue != null) {
                    dialogue.visible = true;
                    dialogue.interactingEntityId = entityId;
                    dialogue.selectedChoiceIndex = -1;
                    dialogue.selectedChoiceValue = "";
                }
            }

            if (input.dialogueChoicePressedIndex == -1) {
                continue;
            }

            for (int candidateId : world.getActiveEntityIds()) {
                DialogueChoiceComponent dialogue = world.getComponent(candidateId, DialogueChoiceComponent.class);
                if (dialogue == null || !dialogue.visible || dialogue.interactingEntityId != entityId) {
                    continue;
                }

                int choiceIndex = input.dialogueChoicePressedIndex;
                if (choiceIndex >= 0 && choiceIndex < dialogue.choiceValues.length) {
                    dialogue.selectedChoiceIndex = choiceIndex;
                    dialogue.selectedChoiceValue = dialogue.choiceValues[choiceIndex];
                    dialogue.visible = false;
                }
            }
        }
    }
}
