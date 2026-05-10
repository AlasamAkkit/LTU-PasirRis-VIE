package game.scenes;

import engine.components.OrderComponent;
import engine.core.Scene;
import engine.ecs.EcsWorld;
import engine.input.InputSystem;
import engine.math.Vector3;
import engine.render.RenderSystem;
import engine.render.Window;
import engine.systems.InteractionDetectionSystem;
import engine.systems.InteractionExecutionSystem;
import engine.systems.MovementSystem;
import engine.systems.OrderCompletionSystem;
import engine.systems.OrderGenerationSystem;
import engine.systems.OrderTrackingSystem;
import engine.systems.OrderUISystem;
import engine.systems.SpawnSystem;
import engine.systems.TaskSystem;
import game.content.DemoWorldFactory;

/**
 * SupermarketScene orchestrates the game scene initialization.
 * 
 * Responsibilities:
 * - Register all ECS systems in the correct order
 * - Create initial game entities
 * - Initialize world configuration
 * 
 * System Execution Order:
 * 1. InputSystem - reads keyboard input
 * 2. MovementSystem - updates player position
 * 3. InteractionDetectionSystem - finds interactable objects
 * 4. InteractionExecutionSystem - handles pickup/drop/delivery
 * 5. TaskSystem - updates task progress
 * 6. OrderGenerationSystem - generates new orders at startup
 * 7. OrderTrackingSystem - monitors item delivery
 * 8. OrderCompletionSystem - checks if order is complete
 * 9. OrderUISystem - updates HUD display
 * 10. RenderSystem - renders all graphics
 */
public final class SupermarketScene implements Scene {
    @Override
    public void load(EcsWorld world, Window window) {
        SpawnSystem spawnSystem = new SpawnSystem();

        // Register game systems in update order
        world.registerSystem(new InputSystem(window));
        world.registerSystem(new MovementSystem());
        world.registerSystem(new InteractionDetectionSystem());
        world.registerSystem(new InteractionExecutionSystem());
        world.registerSystem(new TaskSystem());
        
        // Order management systems (game-specific supermarket logic)
        OrderGenerationSystem orderGenSystem = new OrderGenerationSystem();
        world.registerSystem(orderGenSystem);
        world.registerSystem(new OrderTrackingSystem());
        world.registerSystem(new OrderCompletionSystem(orderGenSystem));
        world.registerSystem(new OrderUISystem(window));
        
        // Render system (must be last for rendering)
        world.registerSystem(new RenderSystem(window));

        // Spawn initial world using configuration
        spawnSystem.spawnRoom(world, new Vector3(0.0f, 0.0f, 0.0f), new Vector3(1.0f, 1.0f, 1.0f));
        spawnSystem.spawnWorld(world, DemoWorldFactory.createDefaultConfig());
        
        // Create the OrderComponent entity (singleton-like)
        createOrderEntity(world);
    }

    /**
     * Create the main order tracking entity.
     * This entity holds the OrderComponent which tracks game state.
     * 
     * ECS Design: This is a game-specific data entity that holds order state.
     * Pure data component - updated by order management systems.
     */
    private void createOrderEntity(EcsWorld world) {
        int orderEntityId = world.createEntity();
        world.addComponent(orderEntityId, new OrderComponent());
    }
}
