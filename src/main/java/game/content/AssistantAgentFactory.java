package game.content;

import engine.components.ColliderComponent;
import engine.components.DialogueChoiceComponent;
import engine.components.InteractableComponent;
import engine.components.InventoryComponent;
import engine.components.NavigationAgentComponent;
import engine.components.RenderComponent;
import engine.components.TransformComponent;
import engine.components.VelocityComponent;
import engine.ecs.EcsWorld;
import engine.math.Vector3;
import game.components.AssistantAgentComponent;
import game.config.WorldConfig;

import java.util.List;

public final class AssistantAgentFactory {
    private AssistantAgentFactory() {
    }

    public static int spawnAssistant(EcsWorld world, WorldConfig.AssistantSpawn spawn) {
        return spawnAssistant(world, spawn.position, spawn.moveSpeed, List.of("bread", "milk", "apples"));
    }

    public static int spawnAssistant(EcsWorld world, WorldConfig.AssistantSpawn spawn, List<String> productTypes) {
        return spawnAssistant(world, spawn.position, spawn.moveSpeed, productTypes);
    }

    public static int spawnAssistant(EcsWorld world, Vector3 position, float moveSpeed) {
        return spawnAssistant(world, position, moveSpeed, List.of("bread", "milk", "apples"));
    }

    public static int spawnAssistant(EcsWorld world, Vector3 position, float moveSpeed, List<String> productTypes) {
        int entityId = world.createEntity();

        TransformComponent transform = world.addComponent(entityId, new TransformComponent());
        transform.position.set(position);
        transform.scale.set(0.55f, 1.0f, 0.55f);

        world.addComponent(entityId, new RenderComponent("placeholder-agent", "placeholder-agent-material"));

        ColliderComponent collider = world.addComponent(entityId, new ColliderComponent());
        collider.interactionRadius = 1.5f;

        InteractableComponent interactable = world.addComponent(entityId, new InteractableComponent());
        interactable.prompt = "Ask assistant";
        interactable.interactionMode = "talk";

        DialogueChoiceComponent dialogue = world.addComponent(entityId, new DialogueChoiceComponent());
        dialogue.title = "Assistant: what should I deliver?";
        dialogue.choiceLabels = buildChoiceLabels(productTypes);
        dialogue.choiceValues = productTypes.toArray(new String[0]);

        VelocityComponent velocity = world.addComponent(entityId, new VelocityComponent());
        velocity.speed = moveSpeed;

        world.addComponent(entityId, new InventoryComponent());
        world.addComponent(entityId, new NavigationAgentComponent());

        AssistantAgentComponent assistant = world.addComponent(entityId, new AssistantAgentComponent());
        assistant.homePosition.set(position);
        assistant.productChoices = productTypes.toArray(new String[0]);
        return entityId;
    }

    private static String[] buildChoiceLabels(List<String> productTypes) {
        String[] labels = new String[productTypes.size()];
        for (int index = 0; index < productTypes.size(); index++) {
            labels[index] = (index + 1) + " " + capitalize(productTypes.get(index));
        }
        return labels;
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}
