package engine.systems;

import engine.components.AIComponent;
import engine.components.ColliderComponent;
import engine.components.InputComponent;
import engine.components.InventoryComponent;
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
        // Intentionally empty for now. This system exists as the home for config-driven spawn passes.
    }

    public int spawnPlayer(EcsWorld world, Vector3 position) {
        int entityId = world.createEntity();
        TransformComponent transform = world.addComponent(entityId, new TransformComponent());
        transform.position.set(position);
        world.addComponent(entityId, new RenderComponent("placeholder-player", "placeholder-player-material"));
        world.addComponent(entityId, new ColliderComponent());
        world.addComponent(entityId, new VelocityComponent());
        world.addComponent(entityId, new InputComponent());
        world.addComponent(entityId, new InventoryComponent());
        world.addComponent(entityId, new AIComponent());
        return entityId;
    }

    public int spawnShelf(EcsWorld world, String shelfId, Vector3 position) {
        int entityId = world.createEntity();
        TransformComponent transform = world.addComponent(entityId, new TransformComponent());
        transform.position.set(position);
        world.addComponent(entityId, new RenderComponent("placeholder-shelf", "placeholder-shelf-material"));
        ShelfComponent shelf = world.addComponent(entityId, new ShelfComponent());
        shelf.shelfId = shelfId;
        world.addComponent(entityId, new ColliderComponent());
        return entityId;
    }

    public int spawnProduct(EcsWorld world, String productType, Vector3 position) {
        int entityId = world.createEntity();
        TransformComponent transform = world.addComponent(entityId, new TransformComponent());
        transform.position.set(position);
        world.addComponent(entityId, new RenderComponent("placeholder-product", "placeholder-product-material"));
        world.addComponent(entityId, new ColliderComponent());
        world.addComponent(entityId, new ProductComponent(productType));
        return entityId;
    }

    public int spawnOrderBox(EcsWorld world, Vector3 position, java.util.List<String> requiredProductTypes) {
        int entityId = world.createEntity();
        TransformComponent transform = world.addComponent(entityId, new TransformComponent());
        transform.position.set(position);
        world.addComponent(entityId, new RenderComponent("placeholder-order-box", "placeholder-order-box-material"));
        world.addComponent(entityId, new ColliderComponent());
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
            spawnProduct(world, productSpawn.productType, productSpawn.position);
        }

        for (WorldConfig.OrderBoxSpawn orderBoxSpawn : config.orderBoxes) {
            spawnOrderBox(world, orderBoxSpawn.position, orderBoxSpawn.requiredProductTypes);
        }
    }
}