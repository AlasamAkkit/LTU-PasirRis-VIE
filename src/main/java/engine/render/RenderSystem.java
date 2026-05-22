package engine.render;

import java.io.IOException;

import engine.camera.Camera;
import engine.components.DialogueChoiceComponent;
import engine.components.InputComponent;
import engine.components.InventoryComponent;
import engine.components.InteractionPromptComponent;
import engine.components.OrderBoxComponent;
import engine.components.OrderComponent;
import engine.components.ProductComponent;
import engine.components.RenderComponent;
import engine.components.ThemeSelectionComponent;
import engine.components.TaskComponent;
import engine.components.TransformComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;
import engine.graphics.PrimitiveFactory;
import engine.graphics.SimpleShaders;
import engine.graphics.TextRenderer;
import engine.rendering.Renderer;

/**
 * RenderSystem now handles drawing all entities.
 * 
 * Integrates:
 * - Camera with input-based movement
 * - Renderer for GPU commands
 * - ECS components (Transform, Input, Render)
 * 
 * Workflow:
 * 1. Initialize camera and renderer on first render call
 * 2. Each frame: update camera from input, then render entities
 */
public final class RenderSystem implements GameSystem {
    private static final float CAMERA_YAW = 0.0f;
    private static final float CAMERA_PITCH = 90.0f;
    private static final float CAMERA_X = 0.0f;
    private static final float DEFAULT_CAMERA_Y = 14.0f;
    private static final float CAMERA_Z = 0.0f;
    private static final String HUD_FONT_PATH = "assets/fonts/OrderHUD.ttf";
    private static final int HUD_FONT_SIZE = 24;

    private final Window window;
    private final float mapWidth;
    private final float mapDepth;
    private final float wallHeight;
    private final float cameraY;
    private Renderer renderer;
    private TextRenderer textRenderer;
    private Camera camera;
    private boolean initialized = false;

    public RenderSystem(Window window) {
        this(window, 10.0f, 10.0f, 3.0f);
    }

    public RenderSystem(Window window, float mapWidth, float mapDepth, float wallHeight) {
        this.window = window;
        this.mapWidth = mapWidth;
        this.mapDepth = mapDepth;
        this.wallHeight = wallHeight;
        this.cameraY = Math.max(DEFAULT_CAMERA_Y, Math.max(mapWidth, mapDepth) * 1.4f);
    }

    @Override
    public void update(EcsWorld world, float deltaSeconds) {
    }

    @Override
    public void render(EcsWorld world) {
        // Lazy initialization
        if (!initialized) {
            System.out.println("[RenderSystem] Initializing renderer...");
            System.out.flush();
            try {
                initialize();
                System.out.println("[RenderSystem] Renderer initialized successfully!");
                System.out.flush();
                initialized = true;
            } catch (Exception e) {
                System.err.println("[RenderSystem] INITIALIZATION FAILED:");
                System.err.println(e.getMessage());
                System.err.flush();
                return;
            }
        }

        try {
            updateCamera();
            updateHud(world);
            boolean themeSelectionActive = isThemeSelectionActive(world);

            // Begin frame and clear
            renderer.beginFrame();
            renderer.setCamera(camera);

            drawRoomWalls(world);

            if (themeSelectionActive) {
                drawThemeSelectionPrompt(world);
                renderer.endFrame();
                return;
            }

            // Render all entities with RenderComponent
            int renderableCount = 0;
            for (int entityId : world.getActiveEntityIds()) {
                if (world.hasComponent(entityId, TransformComponent.class)
                        && world.hasComponent(entityId, RenderComponent.class)) {
                    renderableCount++;

                    // Extract components
                    TransformComponent transform = world.getComponent(entityId, TransformComponent.class);
                    RenderComponent render = world.getComponent(entityId, RenderComponent.class);

                    if (render.visible && render.meshHandle != null) {
                        drawEntity(world, entityId, transform, render);
                    }
                }
            }

            drawInteractionMarker(world);
            drawInteractionPrompt(world);
            drawOrderProgress(world);
            drawOrderStatusText(world);
            drawDialogueChoices(world);

            // Fallback scene if ECS world has no renderables.
            if (renderableCount == 0) {
                renderDemoScene();
            }

            renderer.endFrame();
        } catch (Exception e) {
            System.err.println("[RenderSystem] RENDER FAILED:");
            System.err.println(e.getMessage());
            System.err.flush();
        }
    }

    private void initialize() {
        // Create camera
        System.out.println("[RenderSystem] Creating camera...");
        this.camera = new Camera(window.getWidth(), window.getHeight());
        camera.setYaw(CAMERA_YAW);
        camera.setPitch(CAMERA_PITCH);
        camera.setPosition(CAMERA_X, cameraY, CAMERA_Z);
        System.out.println("[RenderSystem] Camera created: " + window.getWidth() + "x" + window.getHeight());

        // Create renderer and initialize
        System.out.println("[RenderSystem] Creating renderer...");
        this.renderer = new Renderer();
        System.out.println("[RenderSystem] Setting viewport size...");
        renderer.setViewportSize(window.getWidth(), window.getHeight());
        System.out.println("[RenderSystem] Initializing renderer with shaders...");
        renderer.initialize(SimpleShaders.BASIC_VERTEX, SimpleShaders.BASIC_FRAGMENT);
        System.out.println("[RenderSystem] Renderer initialized with shaders!");

        try {
            System.out.println("[RenderSystem] Creating text renderer...");
            this.textRenderer = new TextRenderer(window.getWidth(), window.getHeight(), HUD_FONT_PATH, HUD_FONT_SIZE);
            System.out.println("[RenderSystem] Text renderer created!");
        } catch (IOException e) {
            System.err.println("[RenderSystem] Text renderer init failed: " + e.getMessage());
            System.err.flush();
            this.textRenderer = null;
        }

        // Keep cursor free for this simple preview camera mode.
        window.setMouseCaptured(false);

        // Pre-create placeholder meshes
        System.out.println("[RenderSystem] Registering meshes...");
        renderer.registerMesh("cube", PrimitiveFactory.createCube());
        System.out.println("[RenderSystem] Registered cube mesh");
        renderer.registerMesh("plane", PrimitiveFactory.createPlane());
        System.out.println("[RenderSystem] Registered plane mesh");
        renderer.registerMesh("room", PrimitiveFactory.createRoom(mapWidth, wallHeight, mapDepth));
        System.out.println("[RenderSystem] Registered room mesh");

        // Register aliases for placeholder handles used by SpawnSystem
        renderer.registerMesh("placeholder-player", renderer.getMesh("cube"));
        renderer.registerMesh("placeholder-shelf", renderer.getMesh("cube"));
        renderer.registerMesh("placeholder-product", renderer.getMesh("cube"));
        renderer.registerMesh("placeholder-order-box", renderer.getMesh("cube"));
        renderer.registerMesh("placeholder-agent", renderer.getMesh("cube"));
        System.out.println("[RenderSystem] Registered placeholder mesh aliases");
    }

    private void updateCamera() {
        camera.setPosition(CAMERA_X, cameraY, CAMERA_Z);
        camera.setYaw(CAMERA_YAW);
        camera.setPitch(CAMERA_PITCH);
    }

    private void updateHud(EcsWorld world) {
        String objectiveText = "Objective: deliver milk to the order box";
        String inventoryText = "Held: none";
        String interactionText = "Target: none";
        String taskText = "Order: pending";
        String feedbackText = "";
        String orderStatusText = "";

        for (int entityId : world.getActiveEntityIds()) {
            InventoryComponent inventory = world.getComponent(entityId, InventoryComponent.class);
            InputComponent input = world.getComponent(entityId, InputComponent.class);
            if (inventory == null || input == null) {
                continue;
            }

            if (inventory.heldEntityIds.isEmpty()) {
                inventoryText = "Held: none";
            } else {
                int heldEntityId = inventory.heldEntityIds.get(0);
                ProductComponent heldProduct = world.getComponent(heldEntityId, ProductComponent.class);
                String heldType = heldProduct != null ? heldProduct.productType : "unknown";
                inventoryText = "Held: " + heldType;
            }

            if (input.canInteract) {
                interactionText = "Target: " + input.interactionMode + " (E)";
            }
            if (input.feedbackMessage != null && !input.feedbackMessage.isEmpty()) {
                feedbackText = " | " + input.feedbackMessage;
            }
            break;
        }

        for (int entityId : world.getActiveEntityIds()) {
            TaskComponent task = world.getComponent(entityId, TaskComponent.class);
            if (task == null) {
                continue;
            }
            taskText = task.complete ? "Order: complete" : "Order: " + Math.round(task.progress * 100.0f) + "%";
            break;
        }

        // Get order status for display
        for (int entityId : world.getActiveEntityIds()) {
            OrderComponent order = world.getComponent(entityId, OrderComponent.class);
            if (order != null) {
                int timeRemaining = (int) Math.ceil(order.timeRemainingSeconds);
                orderStatusText = " | LEVEL " + order.currentLevel + " | "
                        + buildInlineOrderStatus(order) + " Time:" + timeRemaining + "s";
                break;
            }
        }

        window.setTitle("LTU Pasir Ris VIE | " + objectiveText + " | " + inventoryText
                + " | " + interactionText + " | " + taskText + orderStatusText + feedbackText);
    }

    private float[] getEntityColor(EcsWorld world, int entityId, RenderComponent render) {
        float[] materialColor = getMaterialColor(render.materialHandle);
        if (materialColor != null) {
            return materialColor;
        }

        if ("room".equals(render.meshHandle)) {
            return new float[] { 0.72f, 0.76f, 0.78f, 1.0f };
        }
        if ("placeholder-shelf".equals(render.meshHandle)) {
            return new float[] { 0.55f, 0.35f, 0.22f, 1.0f };
        }
        if ("placeholder-player".equals(render.meshHandle)) {
            return new float[] { 0.95f, 0.95f, 0.95f, 1.0f };
        }
        if ("placeholder-agent".equals(render.meshHandle)) {
            return new float[] { 0.18f, 0.74f, 0.66f, 1.0f };
        }
        if ("placeholder-product".equals(render.meshHandle)) {
            ProductComponent product = world.getComponent(entityId, ProductComponent.class);
            if (product != null) {
                if ("milk".equals(product.productType)) {
                    return new float[] { 0.92f, 0.96f, 1.0f, 1.0f };
                }
                if ("bread".equals(product.productType)) {
                    return new float[] { 0.95f, 0.68f, 0.30f, 1.0f };
                }
                if ("apples".equals(product.productType)) {
                    return new float[] { 0.90f, 0.18f, 0.18f, 1.0f };
                }
            }
            return new float[] { 0.30f, 0.85f, 0.40f, 1.0f };
        }
        if ("placeholder-order-box".equals(render.meshHandle)) {
            OrderBoxComponent orderBox = world.getComponent(entityId, OrderBoxComponent.class);
            if (orderBox != null && orderBox.complete) {
                return new float[] { 0.20f, 0.78f, 0.38f, 1.0f };
            }
            return new float[] { 0.25f, 0.50f, 1.0f, 1.0f };
        }
        return new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
    }

    private void drawEntity(EcsWorld world, int entityId, TransformComponent transform, RenderComponent render) {
        if ("placeholder-player".equals(render.meshHandle)) {
            drawPlayer(world, transform);
            return;
        }

        if ("placeholder-agent".equals(render.meshHandle)) {
            drawAssistant(transform);
            return;
        }

        if ("placeholder-product".equals(render.meshHandle)) {
            ProductComponent product = world.getComponent(entityId, ProductComponent.class);
            drawProduct(transform, product);
            return;
        }

        if ("placeholder-order-box".equals(render.meshHandle)) {
            OrderBoxComponent orderBox = world.getComponent(entityId, OrderBoxComponent.class);
            drawOrderBox(transform, orderBox);
            return;
        }

        float[] color = getEntityColor(world, entityId, render);
        drawCube(
                transform.position.x, transform.position.y, transform.position.z,
                transform.scale.x, transform.scale.y, transform.scale.z,
                transform.rotation.y, color);
    }

    private void drawPlayer(EcsWorld world, TransformComponent transform) {
        float[] shirtColor = getPlayerShirtColor(world);
        float x = transform.position.x;
        float z = transform.position.z;
        drawCube(x - 0.16f, 0.28f, z, 0.18f, 0.56f, 0.18f, new float[] { 0.12f, 0.13f, 0.16f, 1.0f });
        drawCube(x + 0.16f, 0.28f, z, 0.18f, 0.56f, 0.18f, new float[] { 0.12f, 0.13f, 0.16f, 1.0f });
        drawCube(x, 0.86f, z, 0.48f, 0.72f, 0.36f, shirtColor);
        drawCube(x, 0.86f, z - 0.20f, 0.34f, 0.46f, 0.06f, new float[] { 0.95f, 0.95f, 0.88f, 1.0f });
        drawCube(x - 0.36f, 0.84f, z, 0.14f, 0.55f, 0.14f, new float[] { 0.95f, 0.78f, 0.58f, 1.0f });
        drawCube(x + 0.36f, 0.84f, z, 0.14f, 0.55f, 0.14f, new float[] { 0.95f, 0.78f, 0.58f, 1.0f });
        drawCube(x, 1.34f, z, 0.36f, 0.36f, 0.36f, new float[] { 0.95f, 0.78f, 0.58f, 1.0f });
        drawCube(x, 1.58f, z, 0.42f, 0.12f, 0.42f, new float[] { 0.90f, 0.16f, 0.14f, 1.0f });
    }

    private void drawAssistant(TransformComponent transform) {
        float x = transform.position.x;
        float z = transform.position.z;
        drawCube(x - 0.16f, 0.28f, z, 0.18f, 0.56f, 0.18f, new float[] { 0.10f, 0.16f, 0.18f, 1.0f });
        drawCube(x + 0.16f, 0.28f, z, 0.18f, 0.56f, 0.18f, new float[] { 0.10f, 0.16f, 0.18f, 1.0f });
        drawCube(x, 0.86f, z, 0.48f, 0.72f, 0.36f, new float[] { 0.18f, 0.74f, 0.66f, 1.0f });
        drawCube(x, 0.86f, z - 0.20f, 0.34f, 0.46f, 0.06f, new float[] { 0.95f, 0.98f, 0.94f, 1.0f });
        drawCube(x - 0.36f, 0.84f, z, 0.14f, 0.55f, 0.14f, new float[] { 0.82f, 0.66f, 0.48f, 1.0f });
        drawCube(x + 0.36f, 0.84f, z, 0.14f, 0.55f, 0.14f, new float[] { 0.82f, 0.66f, 0.48f, 1.0f });
        drawCube(x, 1.34f, z, 0.36f, 0.36f, 0.36f, new float[] { 0.82f, 0.66f, 0.48f, 1.0f });
        drawCube(x, 1.57f, z, 0.44f, 0.12f, 0.44f, new float[] { 0.04f, 0.20f, 0.18f, 1.0f });
    }

    private void drawProduct(TransformComponent transform, ProductComponent product) {
        if (product == null || product.productType == null) {
            float[] color = new float[] { 0.30f, 0.85f, 0.40f, 1.0f };
            drawCube(transform.position.x, transform.position.y, transform.position.z, transform.scale.x,
                    transform.scale.y, transform.scale.z, color);
            return;
        }

        float x = transform.position.x;
        float y = transform.position.y;
        float z = transform.position.z;

        if ("milk".equals(product.productType)) {
            drawCube(x, y, z, transform.scale.x, transform.scale.y, transform.scale.z,
                    new float[] { 0.92f, 0.96f, 1.0f, 1.0f });
            drawCube(x, y + transform.scale.y * 0.48f, z, transform.scale.x * 0.72f, transform.scale.y * 0.20f,
                    transform.scale.z * 0.72f, new float[] { 0.45f, 0.76f, 1.0f, 1.0f });
            drawCube(x, y, z - transform.scale.z * 0.52f, transform.scale.x * 0.70f, transform.scale.y * 0.38f, 0.035f,
                    new float[] { 0.12f, 0.38f, 0.85f, 1.0f });
            return;
        }

        if ("bread".equals(product.productType)) {
            drawCube(x, y, z, transform.scale.x, transform.scale.y, transform.scale.z,
                    new float[] { 0.95f, 0.68f, 0.30f, 1.0f });
            drawCube(x, y + transform.scale.y * 0.42f, z, transform.scale.x * 0.82f, transform.scale.y * 0.18f,
                    transform.scale.z * 0.82f, new float[] { 0.70f, 0.42f, 0.18f, 1.0f });
            drawCube(x - transform.scale.x * 0.22f, y + transform.scale.y * 0.55f, z, 0.035f, transform.scale.y * 0.20f,
                    transform.scale.z * 0.90f, new float[] { 0.55f, 0.32f, 0.12f, 1.0f });
            drawCube(x + transform.scale.x * 0.22f, y + transform.scale.y * 0.55f, z, 0.035f, transform.scale.y * 0.20f,
                    transform.scale.z * 0.90f, new float[] { 0.55f, 0.32f, 0.12f, 1.0f });
            return;
        }

        if ("apples".equals(product.productType)) {
            float appleSize = Math.max(0.16f, transform.scale.x * 0.55f);
            drawCube(x - appleSize * 0.55f, y, z, appleSize, appleSize, appleSize,
                    new float[] { 0.90f, 0.18f, 0.18f, 1.0f });
            drawCube(x + appleSize * 0.55f, y, z, appleSize, appleSize, appleSize,
                    new float[] { 0.82f, 0.12f, 0.12f, 1.0f });
            drawCube(x, y + appleSize * 0.42f, z + appleSize * 0.35f, appleSize, appleSize, appleSize,
                    new float[] { 0.95f, 0.22f, 0.18f, 1.0f });
            drawCube(x, y + appleSize * 1.05f, z, appleSize * 0.70f, appleSize * 0.20f, appleSize * 0.45f,
                    new float[] { 0.22f, 0.58f, 0.22f, 1.0f });
            return;
        }

        drawCube(x, y, z, transform.scale.x, transform.scale.y, transform.scale.z,
                new float[] { 0.30f, 0.85f, 0.40f, 1.0f });
    }

    private void drawOrderBox(TransformComponent transform, OrderBoxComponent orderBox) {
        float x = transform.position.x;
        float y = transform.position.y;
        float z = transform.position.z;
        boolean complete = orderBox != null && orderBox.complete;

        float[] base = complete
                ? new float[] { 0.20f, 0.78f, 0.38f, 1.0f }
                : new float[] { 0.20f, 0.48f, 0.95f, 1.0f };
        drawCube(x, y, z, transform.scale.x, transform.scale.y * 0.45f, transform.scale.z, base);
        drawCube(x, y + 0.32f, z - 0.38f, transform.scale.x, 0.12f, 0.12f, new float[] { 0.08f, 0.16f, 0.32f, 1.0f });
        drawCube(x - 0.45f, y + 0.24f, z, 0.10f, 0.48f, transform.scale.z, new float[] { 0.08f, 0.16f, 0.32f, 1.0f });
        drawCube(x + 0.45f, y + 0.24f, z, 0.10f, 0.48f, transform.scale.z, new float[] { 0.08f, 0.16f, 0.32f, 1.0f });
        if (!complete) {
            drawCube(x, y + 0.62f, z, 0.52f, 0.08f, 0.52f, new float[] { 0.92f, 0.96f, 1.0f, 1.0f });
        }
    }

    private void drawInteractionMarker(EcsWorld world) {
        for (int entityId : world.getActiveEntityIds()) {
            InputComponent input = world.getComponent(entityId, InputComponent.class);
            if (input == null || input.selectedInteractableEntityId == -1) {
                continue;
            }

            TransformComponent target = world.getComponent(input.selectedInteractableEntityId,
                    TransformComponent.class);
            if (target == null) {
                return;
            }

            float[] markerColor = interactionMarkerColor(input.interactionMode);
            drawCube(target.position.x, target.position.y + 1.05f, target.position.z, 0.34f, 0.10f, 0.34f,
                    markerColor);
            drawCube(target.position.x, target.position.y + 1.25f, target.position.z, 0.14f, 0.26f, 0.14f,
                    markerColor);
            return;
        }
    }

    private float[] interactionMarkerColor(String interactionMode) {
        if ("clean".equals(interactionMode)) {
            return new float[] { 0.10f, 0.95f, 1.0f, 1.0f };
        }
        if ("place".equals(interactionMode)) {
            return new float[] { 0.25f, 0.55f, 1.0f, 1.0f };
        }
        if ("talk".equals(interactionMode)) {
            return new float[] { 0.34f, 1.0f, 0.66f, 1.0f };
        }
        return new float[] { 1.0f, 0.95f, 0.10f, 1.0f };
    }

    private void drawOrderProgress(EcsWorld world) {
        for (int entityId : world.getActiveEntityIds()) {
            OrderBoxComponent orderBox = world.getComponent(entityId, OrderBoxComponent.class);
            TaskComponent task = world.getComponent(entityId, TaskComponent.class);
            TransformComponent transform = world.getComponent(entityId, TransformComponent.class);
            if (orderBox == null || task == null || transform == null) {
                continue;
            }

            drawCube(transform.position.x, transform.position.y + 1.05f, transform.position.z, 1.05f, 0.10f, 0.10f,
                    new float[] { 0.08f, 0.08f, 0.08f, 1.0f });
            float progressWidth = Math.max(0.06f, 1.0f * task.progress);
            float progressX = transform.position.x - 0.5f + progressWidth * 0.5f;
            float[] progressColor = task.complete
                    ? new float[] { 0.20f, 0.86f, 0.36f, 1.0f }
                    : new float[] { 0.95f, 0.82f, 0.18f, 1.0f };
            drawCube(progressX, transform.position.y + 1.06f, transform.position.z - 0.01f, progressWidth, 0.12f, 0.12f,
                    progressColor);
            return;
        }
    }

    private void drawOrderStatusText(EcsWorld world) {
        if (textRenderer == null) {
            return;
        }

        OrderComponent order = null;
        for (int entityId : world.getActiveEntityIds()) {
            order = world.getComponent(entityId, OrderComponent.class);
            if (order != null) {
                break;
            }
        }

        if (order == null) {
            return;
        }

        int timeRemaining = (int) Math.ceil(order.timeRemainingSeconds);

        String[] lines = buildOrderStatusLines(order, timeRemaining);

        float scale = 1.0f;
        float padding = 16.0f;
        float lineHeight = textRenderer.getLineHeight(scale);

        float maxWidth = 0.0f;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, textRenderer.getTextWidth(line, scale));
        }

        float x = Math.max(padding, window.getWidth() - padding - maxWidth);
        float y = padding;

        for (int index = 0; index < lines.length; index++) {
            float[] color = index == 1
                    ? new float[] { 1.0f, 0.92f, 0.40f, 1.0f }
                    : new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
            textRenderer.drawText(lines[index], x, y, scale, color);
            y += lineHeight;
        }
    }

    private void drawInteractionPrompt(EcsWorld world) {
        if (textRenderer == null) {
            return;
        }

        InteractionPromptComponent prompt = null;
        for (int entityId : world.getActiveEntityIds()) {
            InteractionPromptComponent candidate = world.getComponent(entityId, InteractionPromptComponent.class);
            if (candidate != null && candidate.visible) {
                prompt = candidate;
                break;
            }
        }

        if (prompt == null) {
            return;
        }

        float x = 16.0f;
        float y = 140.0f;
        float scale = 1.0f;
        float[] titleColor = new float[] { 0.98f, 0.86f, 0.32f, 1.0f };
        float[] bodyColor = new float[] { 0.96f, 0.96f, 0.96f, 1.0f };

        textRenderer.drawText(prompt.title, x, y, scale, titleColor);
        y += textRenderer.getLineHeight(scale) * 1.15f;
        textRenderer.drawText(prompt.body, x, y, scale, bodyColor);
    }

    private void drawDialogueChoices(EcsWorld world) {
        if (textRenderer == null) {
            return;
        }

        DialogueChoiceComponent dialogue = null;
        for (int entityId : world.getActiveEntityIds()) {
            DialogueChoiceComponent candidate = world.getComponent(entityId, DialogueChoiceComponent.class);
            if (candidate != null && candidate.visible) {
                dialogue = candidate;
                break;
            }
        }

        if (dialogue == null) {
            return;
        }

        float scale = 1.0f;
        float lineHeight = textRenderer.getLineHeight(scale);
        float maxWidth = textRenderer.getTextWidth(dialogue.title, scale);
        for (String choiceLabel : dialogue.choiceLabels) {
            maxWidth = Math.max(maxWidth, textRenderer.getTextWidth(choiceLabel, scale));
        }

        float x = Math.max(16.0f, (window.getWidth() - maxWidth) * 0.5f);
        float y = Math.max(16.0f, window.getHeight() - lineHeight * (dialogue.choiceLabels.length + 2) - 28.0f);

        textRenderer.drawText(dialogue.title, x, y, scale, new float[] { 1.0f, 1.0f, 1.0f, 1.0f });
        y += lineHeight;
        for (String choiceLabel : dialogue.choiceLabels) {
            textRenderer.drawText(choiceLabel, x, y, scale, new float[] { 0.84f, 1.0f, 0.92f, 1.0f });
            y += lineHeight;
        }
    }

    private void drawCube(float x, float y, float z, float scaleX, float scaleY, float scaleZ, float[] color) {
        drawCube(x, y, z, scaleX, scaleY, scaleZ, 0.0f, color);
    }

    private void drawCube(float x, float y, float z, float scaleX, float scaleY, float scaleZ,
            float rotationYDegrees, float[] color) {
        renderer.drawMeshByHandle("cube", x, y, z, scaleX, scaleY, scaleZ, rotationYDegrees,
                color[0], color[1], color[2], color[3]);
    }

    private String buildInlineOrderStatus(OrderComponent order) {
        StringBuilder builder = new StringBuilder();
        for (String productType : order.currentOrder.keySet()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(capitalize(productType)).append(':')
                    .append(order.deliveredItems.getOrDefault(productType, 0))
                    .append('/')
                    .append(order.currentOrder.getOrDefault(productType, 0));
        }
        return builder.toString();
    }

    private String[] buildOrderStatusLines(OrderComponent order, int timeRemaining) {
        String[] lines = new String[order.currentOrder.size() + 2];
        lines[0] = "LEVEL " + order.currentLevel;
        lines[1] = "Time: " + timeRemaining + "s";
        int index = 2;
        for (String productType : order.currentOrder.keySet()) {
            lines[index] = capitalize(productType) + ": "
                    + order.deliveredItems.getOrDefault(productType, 0)
                    + "/"
                    + order.currentOrder.getOrDefault(productType, 0);
            index++;
        }
        return lines;
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private float[] getMaterialColor(String materialHandle) {
        if ("shelf-back-material".equals(materialHandle)) {
            return new float[] { 0.34f, 0.24f, 0.18f, 1.0f };
        }
        if ("shelf-plank-material".equals(materialHandle)) {
            return new float[] { 0.58f, 0.40f, 0.26f, 1.0f };
        }
        if ("shelf-frame-material".equals(materialHandle)) {
            return new float[] { 0.24f, 0.17f, 0.13f, 1.0f };
        }
        if ("dairy-sign-material".equals(materialHandle)) {
            return new float[] { 0.46f, 0.78f, 1.0f, 1.0f };
        }
        if ("bakery-sign-material".equals(materialHandle)) {
            return new float[] { 1.0f, 0.72f, 0.32f, 1.0f };
        }
        if ("produce-sign-material".equals(materialHandle)) {
            return new float[] { 0.40f, 0.82f, 0.42f, 1.0f };
        }
        if ("generic-sign-material".equals(materialHandle)) {
            return new float[] { 0.82f, 0.82f, 0.82f, 1.0f };
        }
        if ("aisle-floor-material".equals(materialHandle)) {
            return new float[] { 0.63f, 0.66f, 0.68f, 1.0f };
        }
        if ("aisle-line-material".equals(materialHandle)) {
            return new float[] { 0.95f, 0.88f, 0.38f, 1.0f };
        }
        if ("checkout-counter-material".equals(materialHandle)) {
            return new float[] { 0.12f, 0.32f, 0.42f, 1.0f };
        }
        if ("order-zone-material".equals(materialHandle)) {
            return new float[] { 0.16f, 0.38f, 0.78f, 1.0f };
        }
        if ("mess-material".equals(materialHandle)) {
            return new float[] { 0.44f, 0.25f, 0.10f, 1.0f };
        }
        return null;
    }

    private void renderDemoScene() {
        renderer.drawMeshByHandle("room", 0, 0, 0, 1, 1, 1, 0.8f, 0.75f, 0.7f, 1.0f);
        renderer.drawMeshByHandle("cube", 0, 0.5f, -3, 0.6f, 0.6f, 0.6f, 0.25f, 0.75f, 0.35f, 1.0f);
    }

    @Override
    public void cleanup(EcsWorld world) {
        if (renderer != null) {
            renderer.destroy();
        }
    }

    private void drawRoomWalls(EcsWorld world) {
        float[] floorColor = getThemeFloorColor(world);
        float halfWidth = mapWidth * 0.5f;
        float halfDepth = mapDepth * 0.5f;
        float wallThickness = 0.2f;
        float wallY = wallHeight * 0.5f;

        // Left wall
        drawCube(
                -halfWidth, wallY, 0.0f,
                wallThickness, wallHeight, mapDepth,
                new float[] { 0.45f, 0.45f, 0.45f, 1.0f });

        // Right wall
        drawCube(
                halfWidth, wallY, 0.0f,
                wallThickness, wallHeight, mapDepth,
                new float[] { 0.45f, 0.45f, 0.45f, 1.0f });

        // Top wall
        drawCube(
                0.0f, wallY, -halfDepth,
                mapWidth, wallHeight, wallThickness,
                new float[] { 0.45f, 0.45f, 0.45f, 1.0f });

        // Bottom wall
        drawCube(
                0.0f, wallY, halfDepth,
                mapWidth, wallHeight, wallThickness,
                new float[] { 0.45f, 0.45f, 0.45f, 1.0f });

        // Floor
        drawCube(
                0.0f, -0.1f, 0.0f,
                mapWidth, 0.1f, mapDepth,
                floorColor);
    }

    private boolean isThemeSelectionActive(EcsWorld world) {
        for (int entityId : world.getActiveEntityIds()) {
            ThemeSelectionComponent theme = world.getComponent(entityId, ThemeSelectionComponent.class);
            if (theme != null && !theme.complete) {
                return true;
            }
        }
        return false;
    }

    private ThemeSelectionComponent findThemeSelection(EcsWorld world) {
        for (int entityId : world.getActiveEntityIds()) {
            ThemeSelectionComponent theme = world.getComponent(entityId, ThemeSelectionComponent.class);
            if (theme != null) {
                return theme;
            }
        }
        return null;
    }

    private float[] getThemeFloorColor(EcsWorld world) {
        ThemeSelectionComponent theme = findThemeSelection(world);
        if (theme != null) {
            return theme.floorColor;
        }
        return new float[] { 0.30f, 0.30f, 0.30f, 1.0f };
    }

    private float[] getPlayerShirtColor(EcsWorld world) {
        ThemeSelectionComponent theme = world != null ? findThemeSelection(world) : null;
        if (theme != null) {
            return theme.shirtColor;
        }
        return new float[] { 0.12f, 0.36f, 0.80f, 1.0f };
    }

    private void drawThemeSelectionPrompt(EcsWorld world) {
        if (textRenderer == null) {
            return;
        }

        ThemeSelectionComponent theme = findThemeSelection(world);
        if (theme == null) {
            return;
        }

        float x = 16.0f;
        float y = 140.0f;
        float scale = 1.0f;

        textRenderer.drawText(theme.title, x, y, scale, new float[] { 0.98f, 0.86f, 0.32f, 1.0f });
        y += textRenderer.getLineHeight(scale) * 1.15f;
        textRenderer.drawText(theme.body, x, y, scale, new float[] { 0.96f, 0.96f, 0.96f, 1.0f });
    }
}
