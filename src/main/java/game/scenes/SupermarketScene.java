package game.scenes;

import engine.core.Scene;
import engine.ecs.EcsWorld;
import engine.input.InputSystem;
import engine.math.Vector3;
import engine.render.RenderSystem;
import engine.render.Window;
import engine.systems.InteractionDetectionSystem;
import engine.systems.InteractionExecutionSystem;
import engine.systems.MovementSystem;
import engine.systems.SpawnSystem;
import engine.systems.TaskSystem;
import game.content.DemoWorldFactory;

public final class SupermarketScene implements Scene {
    @Override
    public void load(EcsWorld world, Window window) {
        SpawnSystem spawnSystem = new SpawnSystem();

        world.registerSystem(new InputSystem(window));
        world.registerSystem(new MovementSystem());
        world.registerSystem(new InteractionDetectionSystem());
        world.registerSystem(new InteractionExecutionSystem());
        world.registerSystem(new TaskSystem());
        world.registerSystem(new RenderSystem(window));

        spawnSystem.spawnRoom(world, new Vector3(0.0f, 0.0f, 0.0f), new Vector3(1.0f, 1.0f, 1.0f));
        spawnSystem.spawnWorld(world, DemoWorldFactory.createDefaultConfig());
    }
}
