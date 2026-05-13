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

public final class AssistantAgentFactory {
    private AssistantAgentFactory() {
    }

    public static int spawnAssistant(EcsWorld world, Vector3 position) {
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
        dialogue.choiceLabels = new String[] { "1 Bread", "2 Milk", "3 Apples" };
        dialogue.choiceValues = new String[] { "bread", "milk", "apples" };

        VelocityComponent velocity = world.addComponent(entityId, new VelocityComponent());
        velocity.speed = 2.25f;

        world.addComponent(entityId, new InventoryComponent());
        world.addComponent(entityId, new NavigationAgentComponent());

        AssistantAgentComponent assistant = world.addComponent(entityId, new AssistantAgentComponent());
        assistant.homePosition.set(position);
        return entityId;
    }
}
