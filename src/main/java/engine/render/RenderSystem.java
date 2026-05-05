package engine.render;

import engine.camera.Camera;
import engine.components.InputComponent;
import engine.components.InventoryComponent;
import engine.components.OrderBoxComponent;
import engine.components.ProductComponent;
import engine.components.RenderComponent;
import engine.components.TaskComponent;
import engine.components.TransformComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;
import engine.graphics.PrimitiveFactory;
import engine.graphics.SimpleShaders;
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
    private static final float CAMERA_YAW = 45.0f;
    private static final float CAMERA_PITCH = 40.0f;
    private static final float CAMERA_OFFSET_X = 5.0f;
    private static final float CAMERA_OFFSET_Y = 6.0f;
    private static final float CAMERA_OFFSET_Z = 5.0f;
    private static final float CAMERA_FOLLOW_SPEED = 8.0f;

    private final Window window;
    private Renderer renderer;
    private Camera camera;
    private boolean initialized = false;
    private float lastDeltaSeconds = 1.0f / 60.0f;
    private float smoothCameraX;
    private float smoothCameraY;
    private float smoothCameraZ;

    public RenderSystem(Window window) {
        this.window = window;
    }

    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        lastDeltaSeconds = deltaSeconds;
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
            updateCamera(world);
            updateHud(world);

            // Begin frame and clear
            renderer.beginFrame();
            renderer.setCamera(camera);

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
                            float[] color = getEntityColor(world, entityId, render);
                            // Draw the mesh using its transform and render data
                            renderer.drawMeshByHandle(
                                render.meshHandle,
                                transform.position.x, transform.position.y, transform.position.z,
                                transform.scale.x, transform.scale.y, transform.scale.z,
                                color[0], color[1], color[2], color[3]
                            );
                        }
                }
            }

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
        camera.setPosition(CAMERA_OFFSET_X, CAMERA_OFFSET_Y, CAMERA_OFFSET_Z);
        smoothCameraX = CAMERA_OFFSET_X;
        smoothCameraY = CAMERA_OFFSET_Y;
        smoothCameraZ = CAMERA_OFFSET_Z;
        System.out.println("[RenderSystem] Camera created: " + window.getWidth() + "x" + window.getHeight());

        // Create renderer and initialize
        System.out.println("[RenderSystem] Creating renderer...");
        this.renderer = new Renderer();
        System.out.println("[RenderSystem] Setting viewport size...");
        renderer.setViewportSize(window.getWidth(), window.getHeight());
        System.out.println("[RenderSystem] Initializing renderer with shaders...");
        renderer.initialize(SimpleShaders.BASIC_VERTEX, SimpleShaders.BASIC_FRAGMENT);
        System.out.println("[RenderSystem] Renderer initialized with shaders!");

        // Keep cursor free for this simple preview camera mode.
        window.setMouseCaptured(false);

        // Pre-create placeholder meshes
        System.out.println("[RenderSystem] Registering meshes...");
        renderer.registerMesh("cube", PrimitiveFactory.createCube());
        System.out.println("[RenderSystem] Registered cube mesh");
        renderer.registerMesh("plane", PrimitiveFactory.createPlane());
        System.out.println("[RenderSystem] Registered plane mesh");
        renderer.registerMesh("room", PrimitiveFactory.createRoom(10, 3, 10));
        System.out.println("[RenderSystem] Registered room mesh");

        // Register aliases for placeholder handles used by SpawnSystem
        renderer.registerMesh("placeholder-player", renderer.getMesh("cube"));
        renderer.registerMesh("placeholder-shelf", renderer.getMesh("cube"));
        renderer.registerMesh("placeholder-product", renderer.getMesh("cube"));
        renderer.registerMesh("placeholder-order-box", renderer.getMesh("cube"));
        System.out.println("[RenderSystem] Registered placeholder mesh aliases");
    }

    private void updateCamera(EcsWorld world) {
        TransformComponent playerTransform = null;

        for (int entityId : world.getActiveEntityIds()) {
            InputComponent input = world.getComponent(entityId, InputComponent.class);
            TransformComponent transform = world.getComponent(entityId, TransformComponent.class);
            if (input != null) {
                playerTransform = transform;
                break; // Use first entity with InputComponent
            }
        }

        if (playerTransform == null) {
            return;
        }

        float desiredX = playerTransform.position.x + CAMERA_OFFSET_X;
        float desiredY = playerTransform.position.y + CAMERA_OFFSET_Y;
        float desiredZ = playerTransform.position.z + CAMERA_OFFSET_Z;

        float alpha = Math.min(1.0f, CAMERA_FOLLOW_SPEED * Math.max(0.0f, lastDeltaSeconds));
        smoothCameraX += (desiredX - smoothCameraX) * alpha;
        smoothCameraY += (desiredY - smoothCameraY) * alpha;
        smoothCameraZ += (desiredZ - smoothCameraZ) * alpha;

        camera.setPosition(smoothCameraX, smoothCameraY, smoothCameraZ);
        camera.setYaw(CAMERA_YAW);
        camera.setPitch(CAMERA_PITCH);

        // Keep movement orientation consistent with the camera's ground-plane direction.
        playerTransform.rotation.y = CAMERA_YAW;
    }

    private void updateHud(EcsWorld world) {
        String objectiveText = "Objective: deliver milk to the order box";
        String inventoryText = "Held: none";
        String interactionText = "Target: none";
        String taskText = "Order: pending";
        String feedbackText = "";

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

        window.setTitle("LTU Pasir Ris VIE | WASD move | E interact | G drop | " + objectiveText + " | " + inventoryText + " | " + interactionText + " | " + taskText + feedbackText);
    }

    private float[] getEntityColor(EcsWorld world, int entityId, RenderComponent render) {
        float[] materialColor = getMaterialColor(render.materialHandle);
        if (materialColor != null) {
            return materialColor;
        }

        if ("room".equals(render.meshHandle)) {
            return new float[]{0.72f, 0.76f, 0.78f, 1.0f};
        }
        if ("placeholder-shelf".equals(render.meshHandle)) {
            return new float[]{0.55f, 0.35f, 0.22f, 1.0f};
        }
        if ("placeholder-player".equals(render.meshHandle)) {
            return new float[]{0.95f, 0.95f, 0.95f, 1.0f};
        }
        if ("placeholder-product".equals(render.meshHandle)) {
            ProductComponent product = world.getComponent(entityId, ProductComponent.class);
            if (product != null) {
                if ("milk".equals(product.productType)) {
                    return new float[]{0.92f, 0.96f, 1.0f, 1.0f};
                }
                if ("bread".equals(product.productType)) {
                    return new float[]{0.95f, 0.68f, 0.30f, 1.0f};
                }
                if ("apples".equals(product.productType)) {
                    return new float[]{0.90f, 0.18f, 0.18f, 1.0f};
                }
            }
            return new float[]{0.30f, 0.85f, 0.40f, 1.0f};
        }
        if ("placeholder-order-box".equals(render.meshHandle)) {
            OrderBoxComponent orderBox = world.getComponent(entityId, OrderBoxComponent.class);
            if (orderBox != null && orderBox.complete) {
                return new float[]{0.20f, 0.78f, 0.38f, 1.0f};
            }
            return new float[]{0.25f, 0.50f, 1.0f, 1.0f};
        }
        return new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    }

    private float[] getMaterialColor(String materialHandle) {
        if ("shelf-back-material".equals(materialHandle)) {
            return new float[]{0.34f, 0.24f, 0.18f, 1.0f};
        }
        if ("shelf-plank-material".equals(materialHandle)) {
            return new float[]{0.58f, 0.40f, 0.26f, 1.0f};
        }
        if ("shelf-frame-material".equals(materialHandle)) {
            return new float[]{0.24f, 0.17f, 0.13f, 1.0f};
        }
        if ("dairy-sign-material".equals(materialHandle)) {
            return new float[]{0.46f, 0.78f, 1.0f, 1.0f};
        }
        if ("bakery-sign-material".equals(materialHandle)) {
            return new float[]{1.0f, 0.72f, 0.32f, 1.0f};
        }
        if ("produce-sign-material".equals(materialHandle)) {
            return new float[]{0.40f, 0.82f, 0.42f, 1.0f};
        }
        if ("generic-sign-material".equals(materialHandle)) {
            return new float[]{0.82f, 0.82f, 0.82f, 1.0f};
        }
        if ("aisle-floor-material".equals(materialHandle)) {
            return new float[]{0.63f, 0.66f, 0.68f, 1.0f};
        }
        if ("aisle-line-material".equals(materialHandle)) {
            return new float[]{0.95f, 0.88f, 0.38f, 1.0f};
        }
        if ("checkout-counter-material".equals(materialHandle)) {
            return new float[]{0.12f, 0.32f, 0.42f, 1.0f};
        }
        if ("order-zone-material".equals(materialHandle)) {
            return new float[]{0.16f, 0.38f, 0.78f, 1.0f};
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
}
