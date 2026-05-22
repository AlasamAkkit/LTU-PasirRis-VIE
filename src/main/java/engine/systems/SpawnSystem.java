package engine.systems;

import engine.components.ColliderComponent;
import engine.components.InputComponent;
import engine.components.InventoryComponent;
import engine.components.InteractableComponent;
import engine.components.MessComponent;
import engine.components.NavigationObstacleComponent;
import engine.components.OrderBoxComponent;
import engine.components.ProductComponent;
import engine.components.RenderComponent;
import engine.components.ShelfComponent;
import engine.components.TaskComponent;
import engine.components.TransformComponent;
import engine.components.VelocityComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;
import engine.math.Vector3;
import game.config.WorldConfig;

import java.util.HashMap;
import java.util.Map;

public final class SpawnSystem implements GameSystem {
    private static final float SHELF_ANCHOR_Y = 0.9f;

    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        // Intentionally empty for now. This system exists as the home for config-driven
        // spawn passes.
    }

    public int spawnPlayer(EcsWorld world, Vector3 position, float moveSpeed) {
        int entityId = world.createEntity();
        TransformComponent transform = world.addComponent(entityId, new TransformComponent());
        transform.position.set(position);
        transform.scale.set(0.55f, 1.0f, 0.55f);
        world.addComponent(entityId, new RenderComponent("placeholder-player", "placeholder-player-material"));
        ColliderComponent collider = world.addComponent(entityId, new ColliderComponent());
        collider.halfExtents.set(0.275f, 0.5f, 0.275f);
        collider.trigger = false;
        VelocityComponent velocity = world.addComponent(entityId, new VelocityComponent());
        velocity.speed = moveSpeed;
        world.addComponent(entityId, new InputComponent());
        world.addComponent(entityId, new InventoryComponent());
        return entityId;
    }

    public int spawnRoom(EcsWorld world, Vector3 position, Vector3 scale) {
        int entityId = world.createEntity();
        TransformComponent transform = world.addComponent(entityId, new TransformComponent());
        transform.position.set(position);
        transform.scale.set(scale);
        world.addComponent(entityId, new RenderComponent("room", "room-material"));
        return entityId;
    }

    public int spawnShelf(EcsWorld world, String shelfId, Vector3 position) {
        return spawnShelf(world, shelfId, position, 0.0f);
    }

    public int spawnShelf(EcsWorld world, String shelfId, Vector3 position, float rotationYDegrees) {
        Vector3 shelfPosition = new Vector3(position.x, SHELF_ANCHOR_Y, position.z);
        int entityId = spawnProp(world, shelfPosition, new Vector3(1.35f, 1.7f, 0.12f),
                "shelf-back-material", rotationYDegrees);
        ShelfComponent shelf = world.addComponent(entityId, new ShelfComponent());
        shelf.shelfId = shelfId;
        ColliderComponent collider = world.addComponent(entityId, new ColliderComponent());
        collider.trigger = false;
        Vector3 colliderHalfExtents = rotatedHalfExtents(0.8f, 0.4f, rotationYDegrees);
        Vector3 colliderOffset = rotateShelfOffset(0.0f, 0.09f, rotationYDegrees);
        collider.halfExtents.set(colliderHalfExtents.x, 0.85f, colliderHalfExtents.z);
        collider.offset.set(colliderOffset.x, 0.0f, colliderOffset.z);
        NavigationObstacleComponent obstacle = world.addComponent(entityId, new NavigationObstacleComponent());
        Vector3 obstacleHalfExtents = rotatedHalfExtents(0.95f, 0.62f, rotationYDegrees);
        obstacle.halfExtents.set(obstacleHalfExtents.x, 0.0f, obstacleHalfExtents.z);

        spawnShelfPart(world, shelfPosition, 0.0f, 0.25f, 0.18f,
                new Vector3(1.45f, 0.12f, 0.75f),
                "shelf-plank-material", rotationYDegrees);
        spawnShelfPart(world, shelfPosition, 0.0f, 0.82f, 0.18f,
                new Vector3(1.45f, 0.10f, 0.75f),
                "shelf-plank-material", rotationYDegrees);
        spawnShelfPart(world, shelfPosition, 0.0f, 1.38f, 0.18f,
                new Vector3(1.45f, 0.10f, 0.75f),
                "shelf-plank-material", rotationYDegrees);
        spawnShelfPart(world, shelfPosition, -0.75f, 0.85f, 0.18f,
                new Vector3(0.12f, 1.75f, 0.75f),
                "shelf-frame-material", rotationYDegrees);
        spawnShelfPart(world, shelfPosition, 0.75f, 0.85f, 0.18f,
                new Vector3(0.12f, 1.75f, 0.75f),
                "shelf-frame-material", rotationYDegrees);
        spawnShelfPart(world, shelfPosition, 0.0f, 1.98f, 0.35f,
                new Vector3(1.55f, 0.28f, 0.10f),
                signMaterialForShelf(shelfId), rotationYDegrees);
        return entityId;
    }

    public int spawnProduct(EcsWorld world, String productType, Vector3 position) {
        int entityId = world.createEntity();
        TransformComponent transform = world.addComponent(entityId, new TransformComponent());
        transform.position.set(position);
        transform.scale.set(productScale(productType));
        world.addComponent(entityId, new RenderComponent("placeholder-product", "placeholder-product-material"));
        ColliderComponent collider = world.addComponent(entityId, new ColliderComponent());
        collider.interactionRadius = 1.35f;
        world.addComponent(entityId, new ProductComponent(productType));
        return entityId;
    }

    public int spawnShelfProduct(EcsWorld world, String productType, Vector3 position) {
        return spawnShelfProduct(world, productType, position, true);
    }

    public int spawnShelfProduct(EcsWorld world, String productType, Vector3 position, boolean respawnOnPickup) {
        int entityId = spawnProduct(world, productType, position);
        ProductComponent product = world.getComponent(entityId, ProductComponent.class);
        if (product != null) {
            product.respawnOnPickup = respawnOnPickup;
            product.respawnX = position.x;
            product.respawnY = position.y;
            product.respawnZ = position.z;
        }
        return entityId;
    }

    public int spawnOrderBox(EcsWorld world, Vector3 position) {
        int entityId = world.createEntity();
        TransformComponent transform = world.addComponent(entityId, new TransformComponent());
        transform.position.set(position);
        transform.scale.set(0.9f, 0.6f, 0.9f);
        world.addComponent(entityId, new RenderComponent("placeholder-order-box", "placeholder-order-box-material"));
        ColliderComponent collider = world.addComponent(entityId, new ColliderComponent());
        collider.interactionRadius = 1.4f;
        world.addComponent(entityId, new OrderBoxComponent());
        TaskComponent task = world.addComponent(entityId, new TaskComponent());
        task.currentTaskId = "fulfil-order";
        task.status = "in-progress";
        return entityId;
    }

    public int spawnMess(EcsWorld world, Vector3 position, float radius, float speedMultiplier, float durationSeconds,
            float cleaningDurationSeconds) {
        int entityId = world.createEntity();
        TransformComponent transform = world.addComponent(entityId, new TransformComponent());
        transform.position.set(position.x, 0.03f, position.z);
        transform.scale.set(radius * 2.0f, 0.04f, radius * 2.0f);
        world.addComponent(entityId, new RenderComponent("cube", "mess-material"));
        ColliderComponent collider = world.addComponent(entityId, new ColliderComponent());
        collider.interactionRadius = radius;

        InteractableComponent interactable = world.addComponent(entityId, new InteractableComponent());
        interactable.prompt = "Clean mess";
        interactable.interactionMode = "clean";

        MessComponent mess = world.addComponent(entityId, new MessComponent());
        mess.radius = radius;
        mess.speedMultiplier = speedMultiplier;
        mess.remainingSeconds = durationSeconds;
        mess.expires = durationSeconds > 0.0f;
        mess.cleaningDurationSeconds = cleaningDurationSeconds;
        return entityId;
    }

    public void spawnWorld(EcsWorld world, WorldConfig config) {
        Map<String, WorldConfig.ShelfSpawn> shelvesById = indexShelves(config);

        if (config.player.spawnOnStart) {
            spawnPlayer(world, config.player.position, config.player.moveSpeed);
        }

        for (WorldConfig.ShelfSpawn shelfSpawn : config.shelves) {
            if (shelfSpawn.spawnOnStart) {
                spawnShelf(world, shelfSpawn.id, shelfSpawn.position, shelfSpawn.rotationYDegrees);
            }
        }

        for (WorldConfig.ProductSpawn productSpawn : config.products) {
            if (productSpawn.spawnOnStart) {
                Vector3 position = resolveProductPosition(productSpawn, shelvesById);
                spawnShelfProduct(world, productSpawn.productType, position,
                        productSpawn.respawnOnPickup);
            }
        }

        for (WorldConfig.OrderBoxSpawn orderBoxSpawn : config.orderBoxes) {
            if (orderBoxSpawn.spawnOnStart) {
                spawnOrderBox(world, orderBoxSpawn.position);
            }
        }

        spawnStoreDecoration(world);
    }

    private Map<String, WorldConfig.ShelfSpawn> indexShelves(WorldConfig config) {
        Map<String, WorldConfig.ShelfSpawn> shelvesById = new HashMap<>();
        for (WorldConfig.ShelfSpawn shelf : config.shelves) {
            shelvesById.put(shelf.id, shelf);
        }
        return shelvesById;
    }

    private Vector3 resolveProductPosition(WorldConfig.ProductSpawn productSpawn,
            Map<String, WorldConfig.ShelfSpawn> shelvesById) {
        if (productSpawn.shelfId != null && !productSpawn.shelfId.isBlank()) {
            WorldConfig.ShelfSpawn shelf = shelvesById.get(productSpawn.shelfId);
            Vector3 offset = productSpawn.offsetFromShelf;
            Vector3 rotatedOffset = rotateShelfOffset(offset.x, offset.z, shelf.rotationYDegrees);
            return new Vector3(
                    shelf.position.x + rotatedOffset.x,
                    SHELF_ANCHOR_Y + offset.y,
                    shelf.position.z + rotatedOffset.z);
        }
        return productSpawn.position.copy();
    }

    public int spawnProp(EcsWorld world, Vector3 position, Vector3 scale, String materialHandle) {
        return spawnProp(world, position, scale, materialHandle, 0.0f);
    }

    public int spawnProp(EcsWorld world, Vector3 position, Vector3 scale, String materialHandle,
            float rotationYDegrees) {
        int entityId = world.createEntity();
        TransformComponent transform = world.addComponent(entityId, new TransformComponent());
        transform.position.set(position);
        transform.rotation.y = rotationYDegrees;
        transform.scale.set(scale);
        world.addComponent(entityId, new RenderComponent("cube", materialHandle));
        return entityId;
    }

    private int spawnShelfPart(EcsWorld world, Vector3 shelfPosition, float localX, float y, float localZ,
            Vector3 scale, String materialHandle, float rotationYDegrees) {
        Vector3 rotatedOffset = rotateShelfOffset(localX, localZ, rotationYDegrees);
        Vector3 position = new Vector3(
                shelfPosition.x + rotatedOffset.x,
                y,
                shelfPosition.z + rotatedOffset.z);
        return spawnProp(world, position, scale, materialHandle, rotationYDegrees);
    }

    private Vector3 rotateShelfOffset(float x, float z, float rotationYDegrees) {
        double radians = Math.toRadians(rotationYDegrees);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        return new Vector3(x * cos + z * sin, 0.0f, -x * sin + z * cos);
    }

    private Vector3 rotatedHalfExtents(float halfX, float halfZ, float rotationYDegrees) {
        double radians = Math.toRadians(rotationYDegrees);
        float cos = Math.abs((float) Math.cos(radians));
        float sin = Math.abs((float) Math.sin(radians));
        return new Vector3(halfX * cos + halfZ * sin, 0.0f, halfX * sin + halfZ * cos);
    }

    private void spawnStoreDecoration(EcsWorld world) {
        spawnProp(world, new Vector3(0.0f, 0.02f, 0.35f), new Vector3(2.1f, 0.04f, 6.6f), "aisle-floor-material");
        spawnProp(world, new Vector3(-2.65f, 0.025f, 0.35f), new Vector3(0.16f, 0.05f, 6.2f), "aisle-line-material");
        spawnProp(world, new Vector3(2.65f, 0.025f, 0.35f), new Vector3(0.16f, 0.05f, 6.2f), "aisle-line-material");
        int checkoutCounter = spawnProp(world, new Vector3(0.0f, 0.48f, 3.55f), new Vector3(2.8f, 0.95f, 0.35f),
                "checkout-counter-material");
        NavigationObstacleComponent obstacle = world.addComponent(checkoutCounter, new NavigationObstacleComponent());
        obstacle.halfExtents.set(1.55f, 0.0f, 0.35f);
        spawnProp(world, new Vector3(0.0f, 0.04f, 2.65f), new Vector3(1.55f, 0.08f, 1.25f), "order-zone-material");
    }

    private Vector3 productScale(String productType) {
        if ("milk".equals(productType)) {
            return new Vector3(0.30f, 0.58f, 0.30f);
        }
        if ("bread".equals(productType)) {
            return new Vector3(0.55f, 0.28f, 0.35f);
        }
        if ("apples".equals(productType)) {
            return new Vector3(0.36f, 0.36f, 0.36f);
        }
        return new Vector3(0.35f, 0.35f, 0.35f);
    }

    private String signMaterialForShelf(String shelfId) {
        if (shelfId.contains("dairy")) {
            return "dairy-sign-material";
        }
        if (shelfId.contains("bakery")) {
            return "bakery-sign-material";
        }
        if (shelfId.contains("produce")) {
            return "produce-sign-material";
        }
        return "generic-sign-material";
    }
}
