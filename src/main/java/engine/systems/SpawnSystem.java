package engine.systems;

import engine.components.ColliderComponent;
import engine.components.InputComponent;
import engine.components.InventoryComponent;
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

public final class SpawnSystem implements GameSystem {
    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        // Intentionally empty for now. This system exists as the home for config-driven
        // spawn passes.
    }

    public int spawnPlayer(EcsWorld world, Vector3 position) {
        int entityId = world.createEntity();
        TransformComponent transform = world.addComponent(entityId, new TransformComponent());
        transform.position.set(position);
        transform.scale.set(0.55f, 1.0f, 0.55f);
        world.addComponent(entityId, new RenderComponent("placeholder-player", "placeholder-player-material"));
        ColliderComponent collider = world.addComponent(entityId, new ColliderComponent());
        collider.halfExtents.set(0.275f, 0.5f, 0.275f);
        collider.trigger = false;
        VelocityComponent velocity = world.addComponent(entityId, new VelocityComponent());
        velocity.speed = 3.0f;
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
        int entityId = spawnProp(world, position, new Vector3(1.35f, 1.7f, 0.12f), "shelf-back-material");
        ShelfComponent shelf = world.addComponent(entityId, new ShelfComponent());
        shelf.shelfId = shelfId;
        ColliderComponent collider = world.addComponent(entityId, new ColliderComponent());
        collider.trigger = false;
        collider.halfExtents.set(0.8f, 0.85f, 0.4f);
        collider.offset.set(0.0f, 0.0f, 0.09f);
        NavigationObstacleComponent obstacle = world.addComponent(entityId, new NavigationObstacleComponent());
        obstacle.halfExtents.set(0.95f, 0.0f, 0.62f);

        spawnProp(world, new Vector3(position.x, 0.25f, position.z + 0.18f), new Vector3(1.45f, 0.12f, 0.75f),
                "shelf-plank-material");
        spawnProp(world, new Vector3(position.x, 0.82f, position.z + 0.18f), new Vector3(1.45f, 0.10f, 0.75f),
                "shelf-plank-material");
        spawnProp(world, new Vector3(position.x, 1.38f, position.z + 0.18f), new Vector3(1.45f, 0.10f, 0.75f),
                "shelf-plank-material");
        spawnProp(world, new Vector3(position.x - 0.75f, 0.85f, position.z + 0.18f), new Vector3(0.12f, 1.75f, 0.75f),
                "shelf-frame-material");
        spawnProp(world, new Vector3(position.x + 0.75f, 0.85f, position.z + 0.18f), new Vector3(0.12f, 1.75f, 0.75f),
                "shelf-frame-material");
        spawnProp(world, new Vector3(position.x, 1.98f, position.z + 0.35f), new Vector3(1.55f, 0.28f, 0.10f),
                signMaterialForShelf(shelfId));
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
        int entityId = spawnProduct(world, productType, position);
        ProductComponent product = world.getComponent(entityId, ProductComponent.class);
        if (product != null) {
            product.respawnOnPickup = true;
            product.respawnX = position.x;
            product.respawnY = position.y;
            product.respawnZ = position.z;
        }
        return entityId;
    }

    public int spawnOrderBox(EcsWorld world, Vector3 position, java.util.List<String> requiredProductTypes) {
        int entityId = world.createEntity();
        TransformComponent transform = world.addComponent(entityId, new TransformComponent());
        transform.position.set(position);
        transform.scale.set(0.9f, 0.6f, 0.9f);
        world.addComponent(entityId, new RenderComponent("placeholder-order-box", "placeholder-order-box-material"));
        ColliderComponent collider = world.addComponent(entityId, new ColliderComponent());
        collider.interactionRadius = 1.4f;
        OrderBoxComponent orderBox = world.addComponent(entityId, new OrderBoxComponent());
        for (String requiredProductType : requiredProductTypes) {
            orderBox.requiredProductTypes.add(requiredProductType);
        }
        TaskComponent task = world.addComponent(entityId, new TaskComponent());
        task.currentTaskId = "fulfil-order";
        task.status = "in-progress";
        return entityId;
    }

    public void spawnWorld(EcsWorld world, WorldConfig config) {
        spawnPlayer(world, config.player.position);

        for (WorldConfig.ShelfSpawn shelfSpawn : config.shelves) {
            spawnShelf(world, shelfSpawn.id, shelfSpawn.position);
        }

        for (WorldConfig.ProductSpawn productSpawn : config.products) {
            spawnShelfProduct(world, productSpawn.productType, productSpawn.position);
        }

        for (WorldConfig.OrderBoxSpawn orderBoxSpawn : config.orderBoxes) {
            spawnOrderBox(world, orderBoxSpawn.position, orderBoxSpawn.requiredProductTypes);
        }

        spawnStoreDecoration(world);
    }

    public int spawnProp(EcsWorld world, Vector3 position, Vector3 scale, String materialHandle) {
        int entityId = world.createEntity();
        TransformComponent transform = world.addComponent(entityId, new TransformComponent());
        transform.position.set(position);
        transform.scale.set(scale);
        world.addComponent(entityId, new RenderComponent("cube", materialHandle));
        return entityId;
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
