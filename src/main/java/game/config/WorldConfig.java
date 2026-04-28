package game.config;

import engine.math.Vector3;

import java.util.ArrayList;
import java.util.List;

public final class WorldConfig {
    public final PlayerSpawn player = new PlayerSpawn(new Vector3(0.0f, 1.0f, 4.0f));
    public final List<ShelfSpawn> shelves = new ArrayList<>();
    public final List<ProductSpawn> products = new ArrayList<>();
    public final List<OrderBoxSpawn> orderBoxes = new ArrayList<>();

    public static WorldConfig demo() {
        WorldConfig config = new WorldConfig();
        config.shelves.add(new ShelfSpawn("shelf-a", new Vector3(-2.0f, 0.0f, -2.0f)));
        config.shelves.add(new ShelfSpawn("shelf-b", new Vector3(2.0f, 0.0f, -2.0f)));
        config.products.add(new ProductSpawn("milk", new Vector3(-2.0f, 1.0f, -1.5f)));
        config.products.add(new ProductSpawn("bread", new Vector3(2.0f, 1.0f, -1.5f)));
        config.orderBoxes.add(new OrderBoxSpawn(new Vector3(0.0f, 0.5f, -4.0f), List.of("milk", "bread")));
        return config;
    }

    public static final class PlayerSpawn {
        public final Vector3 position;

        public PlayerSpawn(Vector3 position) {
            this.position = position;
        }
    }

    public static final class ShelfSpawn {
        public final String id;
        public final Vector3 position;

        public ShelfSpawn(String id, Vector3 position) {
            this.id = id;
            this.position = position;
        }
    }

    public static final class ProductSpawn {
        public final String productType;
        public final Vector3 position;

        public ProductSpawn(String productType, Vector3 position) {
            this.productType = productType;
            this.position = position;
        }
    }

    public static final class OrderBoxSpawn {
        public final Vector3 position;
        public final List<String> requiredProductTypes;

        public OrderBoxSpawn(Vector3 position, List<String> requiredProductTypes) {
            this.position = position;
            this.requiredProductTypes = requiredProductTypes;
        }
    }
}