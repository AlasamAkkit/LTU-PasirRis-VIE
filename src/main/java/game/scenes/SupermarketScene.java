package game.scenes;

import engine.core.Scene;
import engine.ecs.EcsWorld;
import engine.input.InputSystem;
import engine.physics.PhysicsSystem;
import engine.render.RenderSystem;
import engine.render.Window;
import engine.systems.AISystem;
import engine.systems.InteractionDetectionSystem;
import engine.systems.InteractionExecutionSystem;
import engine.systems.MovementSystem;
import engine.systems.SpawnSystem;
import engine.systems.TaskSystem;
import game.content.DemoWorldFactory;
import game.config.WorldConfig;

public final class SupermarketScene implements Scene {
    @Override
    public void load(EcsWorld world, Window window) {
        SpawnSystem spawnSystem = new SpawnSystem();

        world.registerSystem(new InputSystem(window));
        world.registerSystem(new MovementSystem());
        world.registerSystem(new InteractionDetectionSystem());
        world.registerSystem(new InteractionExecutionSystem());
        world.registerSystem(new TaskSystem());
        world.registerSystem(new AISystem());
        world.registerSystem(new PhysicsSystem());
        world.registerSystem(spawnSystem);
        world.registerSystem(new RenderSystem());

        WorldConfig config = DemoWorldFactory.createDefaultConfig();
        spawnSystem.spawnWorld(world, config);
    }
}