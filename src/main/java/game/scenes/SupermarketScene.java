package game.scenes;

import engine.components.ColliderComponent;
import engine.components.InputComponent;
import engine.components.InventoryComponent;
import engine.components.ProductComponent;
import engine.components.RenderComponent;
import engine.components.TransformComponent;
import engine.components.VelocityComponent;
import engine.core.Scene;
import engine.ecs.EcsWorld;
import engine.input.InputSystem;
import engine.math.Vector3;
import engine.render.RenderSystem;
import engine.render.Window;
import engine.systems.InteractionDetectionSystem;
import engine.systems.InteractionExecutionSystem;
import engine.systems.MovementSystem;

public final class SupermarketScene implements Scene {
    @Override
    public void load(EcsWorld world, Window window) {
        world.registerSystem(new InputSystem(window));
        world.registerSystem(new MovementSystem());
        world.registerSystem(new InteractionDetectionSystem());
        world.registerSystem(new InteractionExecutionSystem());
        world.registerSystem(new RenderSystem(window));

        spawnRoom(world);
        spawnPlayer(world);
        spawnInteractableCube(world);
    }

    private void spawnRoom(EcsWorld world) {
        int roomId = world.createEntity();
        TransformComponent transform = world.addComponent(roomId, new TransformComponent());
        transform.position.set(0.0f, 0.0f, 0.0f);
        transform.scale.set(1.0f, 1.0f, 1.0f);
        world.addComponent(roomId, new RenderComponent("room", "placeholder-room-material"));
    }

    private void spawnPlayer(EcsWorld world) {
        int playerId = world.createEntity();
        TransformComponent transform = world.addComponent(playerId, new TransformComponent());
        transform.position.set(0.0f, 0.0f, 0.0f);
        transform.scale.set(0.6f, 0.6f, 0.6f);

        world.addComponent(playerId, new RenderComponent("placeholder-player", "placeholder-player-material"));
        world.addComponent(playerId, new InputComponent());
        world.addComponent(playerId, new InventoryComponent());
        world.addComponent(playerId, new ColliderComponent());

        VelocityComponent velocity = world.addComponent(playerId, new VelocityComponent());
        velocity.speed = 3.0f;
    }

    private void spawnInteractableCube(EcsWorld world) {
        int interactableId = world.createEntity();
        TransformComponent transform = world.addComponent(interactableId, new TransformComponent());
        transform.position.set(new Vector3(1.4f, 0.35f, 0.4f));
        transform.scale.set(0.35f, 0.35f, 0.35f);

        world.addComponent(interactableId, new RenderComponent("placeholder-product", "placeholder-product-material"));
        world.addComponent(interactableId, new ProductComponent("demo"));

        ColliderComponent collider = world.addComponent(interactableId, new ColliderComponent());
        collider.interactionRadius = 1.3f;
    }
}