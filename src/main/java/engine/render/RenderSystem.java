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
    private static final float CAMERA_HEIGHT = 1.6f;
    private static final float CAMERA_MOUSE_SENSITIVITY = 0.1f;

    private final Window window;
    private Renderer renderer;
    private Camera camera;
    private boolean initialized = false;

    public RenderSystem(Window window) {
        this.window = window;
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
            // Update mouse position each frame
            window.updateMousePosition();

            // Update camera from input
            updateCamera(world);

            // Lightweight HUD/status text in title bar
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
        camera.setPosition(0, 1.6f, 0);
        // Reset orientation to a known default for debugging
        camera.setYaw(0.0f);
        camera.setPitch(0.0f);
        System.out.println("[RenderSystem] Camera created: " + window.getWidth() + "x" + window.getHeight());

        // Create renderer and initialize
        System.out.println("[RenderSystem] Creating renderer...");
        this.renderer = new Renderer();
        System.out.println("[RenderSystem] Setting viewport size...");
        renderer.setViewportSize(window.getWidth(), window.getHeight());
        System.out.println("[RenderSystem] Initializing renderer with shaders...");
        renderer.initialize(SimpleShaders.BASIC_VERTEX, SimpleShaders.BASIC_FRAGMENT);
        System.out.println("[RenderSystem] Renderer initialized with shaders!");

        // Enable mouse capture for camera look
        System.out.println("[RenderSystem] Enabling mouse capture...");
        window.setMouseCaptured(true);

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

        float mouseDeltaX = window.getMouseDeltaX();
        float mouseDeltaY = window.getMouseDeltaY();
        float cameraYaw = camera.getYaw() + mouseDeltaX * CAMERA_MOUSE_SENSITIVITY;
        float cameraPitch = camera.getPitch() + mouseDeltaY * CAMERA_MOUSE_SENSITIVITY;
        float cameraX = playerTransform.position.x;
        float cameraY = playerTransform.position.y + CAMERA_HEIGHT;
        float cameraZ = playerTransform.position.z;

        camera.setPosition(cameraX, cameraY, cameraZ);
        camera.setYaw(cameraYaw);
        camera.setPitch(cameraPitch);

        // Keep movement orientation in sync with camera look direction.
        playerTransform.rotation.y = cameraYaw;
    }

    private void updateHud(EcsWorld world) {
        String objectiveText = "Objective: collect required products and deliver with F";
        String inventoryText = "Held: none";
        String interactionText = "Target: none";

        for (int entityId : world.getActiveEntityIds()) {
            TaskComponent task = world.getComponent(entityId, TaskComponent.class);
            if (task != null) {
                int percent = Math.round(task.progress * 100.0f);
                objectiveText = task.complete ? "Objective complete" : ("Objective: " + percent + "% complete");
                break;
            }
        }

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
                interactionText = "Target: " + input.interactionMode + " (F)";
            }
            break;
        }

        window.setTitle("LTU Pasir Ris VIE | " + objectiveText + " | " + inventoryText + " | " + interactionText + " | WASD + mouse");
    }

    private float[] getEntityColor(EcsWorld world, int entityId, RenderComponent render) {
        if ("room".equals(render.meshHandle)) {
            return new float[]{0.82f, 0.78f, 0.70f, 1.0f};
        }
        if ("placeholder-shelf".equals(render.meshHandle)) {
            return new float[]{0.55f, 0.35f, 0.22f, 1.0f};
        }
        if ("placeholder-order-box".equals(render.meshHandle)) {
            OrderBoxComponent orderBox = world.getComponent(entityId, OrderBoxComponent.class);
            if (orderBox != null && orderBox.complete) {
                return new float[]{0.2f, 0.85f, 0.3f, 1.0f};
            }
            return new float[]{0.2f, 0.4f, 0.9f, 1.0f};
        }
        if ("placeholder-product".equals(render.meshHandle)) {
            ProductComponent product = world.getComponent(entityId, ProductComponent.class);
            if (product != null && "milk".equals(product.productType)) {
                return new float[]{0.95f, 0.95f, 1.0f, 1.0f};
            }
            if (product != null && "bread".equals(product.productType)) {
                return new float[]{0.92f, 0.72f, 0.35f, 1.0f};
            }
            if (product != null && "juice".equals(product.productType)) {
                return new float[]{0.95f, 0.52f, 0.12f, 1.0f};
            }
            return new float[]{0.30f, 0.85f, 0.40f, 1.0f};
        }
        return new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    }

    private void renderDemoScene() {
        renderer.drawMeshByHandle("room", 0, 0, 0, 1, 1, 1, 0.8f, 0.75f, 0.7f, 1.0f);
        renderer.drawMeshByHandle("cube", 0, 0.5f, -3, 0.6f, 0.6f, 0.6f, 0.25f, 0.75f, 0.35f, 1.0f);
    }

    public void cleanup() {
        if (renderer != null) {
            renderer.destroy();
        }
    }
}