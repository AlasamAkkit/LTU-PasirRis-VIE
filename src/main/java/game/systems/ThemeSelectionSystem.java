package game.systems;

import engine.components.InputComponent;
import engine.components.NavigationGridComponent;
import engine.components.OrderComponent;
import engine.components.ThemeSelectionComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;
import engine.systems.SpawnSystem;
import engine.math.Vector3;
import game.config.WorldConfig;
import game.content.AssistantAgentFactory;

public final class ThemeSelectionSystem implements GameSystem {
    private static final float[][] FLOOR_PALETTE = {
            { 0.30f, 0.30f, 0.30f, 1.0f },
            { 0.67f, 0.60f, 0.45f, 1.0f },
            { 0.22f, 0.48f, 0.52f, 1.0f }
    };

    private static final float[][] SHIRT_PALETTE = {
            { 0.12f, 0.36f, 0.80f, 1.0f },
            { 0.20f, 0.65f, 0.35f, 1.0f },
            { 0.82f, 0.24f, 0.18f, 1.0f }
    };

    private final SpawnSystem spawnSystem;
    private final WorldConfig worldConfig;
    private boolean gameStarted;
    private Integer cachedSelectionEntityId;

    public ThemeSelectionSystem(SpawnSystem spawnSystem, WorldConfig worldConfig) {
        this.spawnSystem = spawnSystem;
        this.worldConfig = worldConfig;
    }

    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        if (gameStarted) {
            return;
        }

        ThemeSelectionComponent selection = getSelection(world);
        if (selection == null) {
            return;
        }

        InputComponent input = world.getComponent(cachedSelectionEntityId, InputComponent.class);
        if (input == null) {
            return;
        }

        updatePrompt(selection);

        if (input.dialogueChoicePressedIndex == -1) {
            return;
        }

        int choiceIndex = input.dialogueChoicePressedIndex;
        if (selection.stage == ThemeSelectionComponent.STAGE_DEFAULT_OR_CUSTOM) {
            if (choiceIndex == 0) {
                applyDefaultTheme(selection);
                selection.complete = true;
                startGameplay(world, selection);
            } else if (choiceIndex == 1) {
                selection.customTheme = true;
                selection.stage = ThemeSelectionComponent.STAGE_FLOOR_COLOR;
            }
            return;
        }

        if (selection.stage == ThemeSelectionComponent.STAGE_FLOOR_COLOR) {
            if (choiceIndex >= 0 && choiceIndex < FLOOR_PALETTE.length) {
                setColor(selection.floorColor, FLOOR_PALETTE[choiceIndex]);
                selection.stage = ThemeSelectionComponent.STAGE_SHIRT_COLOR;
            }
            return;
        }

        if (selection.stage == ThemeSelectionComponent.STAGE_SHIRT_COLOR) {
            if (choiceIndex >= 0 && choiceIndex < SHIRT_PALETTE.length) {
                setColor(selection.shirtColor, SHIRT_PALETTE[choiceIndex]);
                selection.complete = true;
                startGameplay(world, selection);
            }
        }
    }

    private void startGameplay(EcsWorld world, ThemeSelectionComponent selection) {
        if (gameStarted) {
            return;
        }

        spawnSystem.spawnRoom(world, new Vector3(0.0f, 0.0f, 0.0f), new Vector3(1.0f, 1.0f, 1.0f));
        createNavigationGrid(world);
        spawnSystem.spawnWorld(world, worldConfig);
        if (worldConfig.assistant.spawnOnStart) {
            AssistantAgentFactory.spawnAssistant(world, worldConfig.assistant);
        }
        createOrderEntity(world);
        clearSelectionInput(world);
        gameStarted = true;
        selection.body = "Theme selected. Loading game...";
    }

    private ThemeSelectionComponent getSelection(EcsWorld world) {
        if (cachedSelectionEntityId != null) {
            ThemeSelectionComponent cached = world.getComponent(cachedSelectionEntityId, ThemeSelectionComponent.class);
            if (cached != null) {
                return cached;
            }
        }

        for (int entityId : world.getActiveEntityIds()) {
            ThemeSelectionComponent selection = world.getComponent(entityId, ThemeSelectionComponent.class);
            if (selection != null) {
                cachedSelectionEntityId = entityId;
                return selection;
            }
        }

        return null;
    }

    private void updatePrompt(ThemeSelectionComponent selection) {
        if (selection.stage == ThemeSelectionComponent.STAGE_DEFAULT_OR_CUSTOM) {
            selection.title = "Choose a theme";
            selection.body = "1 Default theme\n2 Custom theme";
            return;
        }

        if (selection.stage == ThemeSelectionComponent.STAGE_FLOOR_COLOR) {
            selection.title = "Custom theme: floor color";
            selection.body = "1 Slate\n2 Sand\n3 Teal";
            return;
        }

        if (selection.stage == ThemeSelectionComponent.STAGE_SHIRT_COLOR) {
            selection.title = "Custom theme: shirt color";
            selection.body = "1 Blue\n2 Green\n3 Red";
        }
    }

    private void applyDefaultTheme(ThemeSelectionComponent selection) {
        setColor(selection.floorColor, FLOOR_PALETTE[0]);
        setColor(selection.shirtColor, SHIRT_PALETTE[0]);
    }

    private void setColor(float[] destination, float[] source) {
        destination[0] = source[0];
        destination[1] = source[1];
        destination[2] = source[2];
        destination[3] = source[3];
    }

    private void clearSelectionInput(EcsWorld world) {
        if (cachedSelectionEntityId == null) {
            return;
        }
        world.removeComponent(cachedSelectionEntityId, InputComponent.class);
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

    private void createOrderEntity(EcsWorld world) {
        int orderEntityId = world.createEntity();
        world.addComponent(orderEntityId, new OrderComponent());
    }
}