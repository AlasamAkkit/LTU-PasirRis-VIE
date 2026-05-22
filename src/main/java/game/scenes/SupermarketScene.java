package game.scenes;

import engine.components.InputComponent;
import engine.components.ThemeSelectionComponent;
import engine.core.Scene;
import engine.ecs.EcsWorld;
import engine.input.InputSystem;
import engine.physics.PhysicsSystem;
import engine.render.RenderSystem;
import engine.render.Window;
import engine.systems.CarriedItemSystem;
import engine.systems.DialogueInteractionSystem;
import engine.systems.InteractionDetectionSystem;
import engine.systems.InteractionExecutionSystem;
import engine.systems.MovementSystem;
import engine.systems.MessSystem;
import engine.systems.NavigationMovementSystem;
import engine.systems.NavigationPathSystem;
import engine.systems.OrderCompletionSystem;
import engine.systems.OrderGenerationSystem;
import engine.systems.OrderTimerSystem;
import engine.systems.OrderTrackingSystem;
import engine.systems.OrderUISystem;
import engine.systems.SpawnSystem;
import engine.systems.TaskSystem;
import game.config.WorldConfig;
import game.systems.InteractionPromptSystem;
import game.systems.ThemeSelectionSystem;
import game.systems.AssistantAgentSystem;

import java.util.Objects;

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
    private final WorldConfig worldConfig;

    public SupermarketScene(WorldConfig worldConfig) {
        this.worldConfig = Objects.requireNonNull(worldConfig, "worldConfig");
    }

    @Override
    public void load(EcsWorld world, Window window) {
        SpawnSystem spawnSystem = new SpawnSystem();

        createThemeSelectionEntity(world);

        // Register game systems in update order
        world.registerSystem(new InputSystem(window));
        world.registerSystem(new ThemeSelectionSystem(spawnSystem, worldConfig));
        world.registerSystem(new MovementSystem(
                worldConfig.map.minX(),
                worldConfig.map.maxX(),
                worldConfig.map.minZ(),
                worldConfig.map.maxZ()));
        world.registerSystem(new PhysicsSystem());
        world.registerSystem(new InteractionDetectionSystem());
        world.registerSystem(new DialogueInteractionSystem());
        world.registerSystem(new InteractionExecutionSystem(worldConfig.messRules));
        world.registerSystem(new AssistantAgentSystem(worldConfig.messRules));
        world.registerSystem(new NavigationPathSystem());
        world.registerSystem(new NavigationMovementSystem());
        world.registerSystem(new PhysicsSystem());
        world.registerSystem(new CarriedItemSystem());
        world.registerSystem(new MessSystem());
        world.registerSystem(new InteractionPromptSystem());

        // Order management systems (game-specific supermarket logic)
        OrderGenerationSystem orderGenSystem = new OrderGenerationSystem(worldConfig.orderRules);
        world.registerSystem(orderGenSystem);
        world.registerSystem(new OrderTrackingSystem());
        world.registerSystem(new OrderTimerSystem(worldConfig.orderRules, orderGenSystem));
        world.registerSystem(new TaskSystem());
        world.registerSystem(new OrderCompletionSystem(orderGenSystem));
        world.registerSystem(new OrderUISystem(window));

        // Render system (must be last for rendering)
        world.registerSystem(new RenderSystem(
                window,
                worldConfig.map.width,
                worldConfig.map.depth,
                worldConfig.map.wallHeight));

        // Gameplay world is spawned after the player chooses a theme.
    }

    /**
     * Create the main order tracking entity.
     * This entity holds the OrderComponent which tracks game state.
     * 
     * ECS Design: This is a game-specific data entity that holds order state.
     * Pure data component - updated by order management systems.
     */
    private void createThemeSelectionEntity(EcsWorld world) {
        int entityId = world.createEntity();
        world.addComponent(entityId, new InputComponent());
        world.addComponent(entityId, new ThemeSelectionComponent());
    }
}
