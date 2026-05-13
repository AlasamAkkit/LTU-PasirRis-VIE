package game.scenes;

import engine.components.OrderComponent;
import engine.components.NavigationGridComponent;
import engine.core.Scene;
import engine.ecs.EcsWorld;
import engine.input.InputSystem;
import engine.math.Vector3;
import engine.render.RenderSystem;
import engine.render.Window;
import engine.systems.CarriedItemSystem;
import engine.systems.DialogueInteractionSystem;
import engine.systems.InteractionDetectionSystem;
import engine.systems.InteractionExecutionSystem;
import engine.systems.MovementSystem;
import engine.systems.NavigationMovementSystem;
import engine.systems.NavigationPathSystem;
import engine.systems.OrderCompletionSystem;
import engine.systems.OrderGenerationSystem;
import engine.systems.OrderTrackingSystem;
import engine.systems.OrderUISystem;
import engine.systems.SpawnSystem;
import engine.systems.TaskSystem;
import game.content.AssistantAgentFactory;
import game.content.DemoWorldFactory;
import game.systems.AssistantAgentSystem;

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
 * 4. DialogueInteractionSystem - opens and resolves generic choice bubbles
 * 5. InteractionExecutionSystem - handles player pickup/drop/delivery
 * 6. AssistantAgentSystem - converts supermarket choices into agent tasks
 * 7. NavigationPathSystem - computes dirty navigation paths
 * 8. NavigationMovementSystem - moves autonomous agents along paths
 * 9. CarriedItemSystem - keeps held items attached to moving entities
 * 10. OrderGenerationSystem - generates new orders at startup
 * 11. OrderTrackingSystem - monitors item delivery
 * 12. TaskSystem - updates task progress
 * 13. OrderCompletionSystem - checks if order is complete
 * 14. OrderUISystem - updates HUD display
 * 15. RenderSystem - renders all graphics
 */
public final class SupermarketScene implements Scene {
    @Override
    public void load(EcsWorld world, Window window) {
        SpawnSystem spawnSystem = new SpawnSystem();

        // Register game systems in update order
        world.registerSystem(new InputSystem(window));
        world.registerSystem(new MovementSystem());
        world.registerSystem(new InteractionDetectionSystem());
        world.registerSystem(new DialogueInteractionSystem());
        world.registerSystem(new InteractionExecutionSystem());
        world.registerSystem(new AssistantAgentSystem());
        world.registerSystem(new NavigationPathSystem());
        world.registerSystem(new NavigationMovementSystem());
        world.registerSystem(new CarriedItemSystem());
        
        // Order management systems (game-specific supermarket logic)
        OrderGenerationSystem orderGenSystem = new OrderGenerationSystem();
        world.registerSystem(orderGenSystem);
        world.registerSystem(new OrderTrackingSystem());
        world.registerSystem(new TaskSystem());
        world.registerSystem(new OrderCompletionSystem(orderGenSystem));
        world.registerSystem(new OrderUISystem(window));
        
        // Render system (must be last for rendering)
        world.registerSystem(new RenderSystem(window));

        // Spawn initial world using configuration
        spawnSystem.spawnRoom(world, new Vector3(0.0f, 0.0f, 0.0f), new Vector3(1.0f, 1.0f, 1.0f));
        createNavigationGrid(world);
        spawnSystem.spawnWorld(world, DemoWorldFactory.createDefaultConfig());
        AssistantAgentFactory.spawnAssistant(world, new Vector3(-4.1f, 0.0f, 3.8f));
        
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

    private void createNavigationGrid(EcsWorld world) {
        int navigationEntityId = world.createEntity();
        NavigationGridComponent grid = world.addComponent(navigationEntityId, new NavigationGridComponent());
        grid.minX = -4.6f;
        grid.maxX = 4.6f;
        grid.minZ = -4.4f;
        grid.maxZ = 4.4f;
        grid.cellSize = 0.4f;
    }
}
