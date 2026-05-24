package game.config;

import java.util.ArrayList;
import java.util.List;

import engine.math.Vector3;

public final class WorldConfig {
    public String name = "Unnamed Scenario";
    public MapSettings map = new MapSettings();
    public PlayerSpawn player = new PlayerSpawn();
    public AssistantSpawn assistant = new AssistantSpawn();
    public OrderRules orderRules = new OrderRules();
    public MessRules messRules = new MessRules();
    public List<ShelfSpawn> shelves = new ArrayList<>();
    public List<ProductSpawn> products = new ArrayList<>();
    public List<OrderBoxSpawn> orderBoxes = new ArrayList<>();

    public static final class MapSettings {
        public float width = 10.0f;
        public float depth = 10.0f;
        public float wallHeight = 3.0f;
        public float playableMarginX = 0.4f;
        public float playableMarginZ = 0.6f;
        public float navigationCellSize = 0.4f;

        public MapSettings() {
        }

        public float minX() {
            return -width * 0.5f + playableMarginX;
        }

        public float maxX() {
            return width * 0.5f - playableMarginX;
        }

        public float minZ() {
            return -depth * 0.5f + playableMarginZ;
        }

        public float maxZ() {
            return depth * 0.5f - playableMarginZ;
        }
    }

    public static final class PlayerSpawn {
        public boolean spawnOnStart = true;
        public Vector3 position = new Vector3(0.0f, 0.0f, 4.0f);
        public float moveSpeed = 3.0f;

        public PlayerSpawn() {
        }
    }

    public static final class ShelfSpawn {
        public boolean spawnOnStart = true;
        public String id = "";
        public Vector3 position = new Vector3();
        public float rotationYDegrees = 0.0f;

        public ShelfSpawn() {
        }
    }

    public static final class ProductSpawn {
        public boolean spawnOnStart = true;
        public String productType = "";
        public float[] color;
        public Vector3 position;
        public String shelfId = "";
        public Vector3 offsetFromShelf;
        public boolean respawnOnPickup = true;

        public ProductSpawn() {
        }
    }

    public static final class OrderBoxSpawn {
        public boolean spawnOnStart = true;
        public Vector3 position = new Vector3();

        public OrderBoxSpawn() {
        }
    }

    public static final class AssistantSpawn {
        public boolean spawnOnStart = true;
        public Vector3 position = new Vector3(-4.1f, 0.0f, 3.8f);
        public float moveSpeed = 2.25f;

        public AssistantSpawn() {
        }
    }

    public static final class OrderRules {
        public static final String RESTART_CURRENT_LEVEL = "restart-current-level";
        public static final String RESTART_FROM_LEVEL_1 = "restart-from-level-1";

        public List<Float> levelTimeLimitsSeconds = new ArrayList<>(List.of(75.0f, 60.0f, 45.0f));
        public String failureBehavior = RESTART_CURRENT_LEVEL;
        public List<String> productTypes = new ArrayList<>(List.of("bread", "milk", "apples"));

        public OrderRules() {
        }

        public float timeLimitForLevel(int level) {
            int levelIndex = Math.max(0, level - 1);
            int configuredIndex = Math.min(levelIndex, levelTimeLimitsSeconds.size() - 1);
            return levelTimeLimitsSeconds.get(configuredIndex);
        }
    }

    public static final class MessRules {
        public boolean enabled = true;
        public float spawnChanceOnProductPickup = 0.20f;
        public float radius = 0.75f;
        public float speedMultiplier = 0.55f;
        public float durationSeconds = 0.0f;
        public float cleaningDurationSeconds = 2.5f;

        public MessRules() {
        }
    }
}
