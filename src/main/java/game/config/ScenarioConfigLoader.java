package game.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ScenarioConfigLoader {
    private static final Path DEFAULT_SCENARIO_PATH = Path.of("assets", "config", "scenario.json");
    private static final Gson GSON = new GsonBuilder().create();

    private ScenarioConfigLoader() {
    }

    public static WorldConfig loadDefault() {
        return load(DEFAULT_SCENARIO_PATH);
    }

    public static WorldConfig load(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            WorldConfig config = GSON.fromJson(reader, WorldConfig.class);
            normalize(config);
            validate(config, path);
            return config;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read scenario config at " + path.toAbsolutePath(), e);
        } catch (JsonParseException e) {
            throw new IllegalStateException("Invalid JSON in scenario config at " + path.toAbsolutePath(), e);
        }
    }

    private static void normalize(WorldConfig config) {
        if (config == null) {
            return;
        }
        if (config.player == null) {
            config.player = new WorldConfig.PlayerSpawn();
        }
        if (config.assistant == null) {
            config.assistant = new WorldConfig.AssistantSpawn();
        }
        if (config.orderRules == null) {
            config.orderRules = new WorldConfig.OrderRules();
        }
        if (config.messRules == null) {
            config.messRules = new WorldConfig.MessRules();
        }
        if (config.orderRules.levelTimeLimitsSeconds == null) {
            config.orderRules.levelTimeLimitsSeconds = new ArrayList<>();
        }
        if (config.shelves == null) {
            config.shelves = new ArrayList<>();
        }
        if (config.products == null) {
            config.products = new ArrayList<>();
        }
        if (config.orderBoxes == null) {
            config.orderBoxes = new ArrayList<>();
        }
    }

    private static void validate(WorldConfig config, Path path) {
        if (config == null) {
            throw new IllegalStateException("Scenario config at " + path.toAbsolutePath() + " is empty.");
        }

        if (config.player.spawnOnStart) {
            requirePosition(config.player.position, "player.position", path);
            requirePositive(config.player.moveSpeed, "player.moveSpeed", path);
        }

        if (config.assistant.spawnOnStart) {
            requirePosition(config.assistant.position, "assistant.position", path);
            requirePositive(config.assistant.moveSpeed, "assistant.moveSpeed", path);
        }

        validateOrderRules(config.orderRules, path);
        validateMessRules(config.messRules, path);

        Map<String, WorldConfig.ShelfSpawn> shelvesById = new HashMap<>();
        Set<String> shelfIds = new HashSet<>();
        for (int index = 0; index < config.shelves.size(); index++) {
            WorldConfig.ShelfSpawn shelf = config.shelves.get(index);
            if (shelf == null) {
                throw invalid(path, "shelves[" + index + "] must not be null.");
            }
            requireText(shelf.id, "shelves[" + index + "].id", path);
            requirePosition(shelf.position, "shelves[" + index + "].position", path);
            if (!shelfIds.add(shelf.id)) {
                throw invalid(path, "shelves[" + index + "].id duplicates shelf id '" + shelf.id + "'.");
            }
            shelvesById.put(shelf.id, shelf);
        }

        for (int index = 0; index < config.products.size(); index++) {
            WorldConfig.ProductSpawn product = config.products.get(index);
            if (product == null) {
                throw invalid(path, "products[" + index + "] must not be null.");
            }
            if (!product.spawnOnStart) {
                continue;
            }
            requireText(product.productType, "products[" + index + "].productType", path);
            validateProductPlacement(product, index, shelvesById, path);
        }

        for (int index = 0; index < config.orderBoxes.size(); index++) {
            WorldConfig.OrderBoxSpawn orderBox = config.orderBoxes.get(index);
            if (orderBox == null) {
                throw invalid(path, "orderBoxes[" + index + "] must not be null.");
            }
            if (!orderBox.spawnOnStart) {
                continue;
            }
            requirePosition(orderBox.position, "orderBoxes[" + index + "].position", path);
        }
    }

    private static void validateOrderRules(WorldConfig.OrderRules orderRules, Path path) {
        if (orderRules.levelTimeLimitsSeconds.isEmpty()) {
            throw invalid(path, "orderRules.levelTimeLimitsSeconds must contain at least one value.");
        }
        for (int index = 0; index < orderRules.levelTimeLimitsSeconds.size(); index++) {
            Float timeLimit = orderRules.levelTimeLimitsSeconds.get(index);
            if (timeLimit == null || timeLimit <= 0.0f) {
                throw invalid(path, "orderRules.levelTimeLimitsSeconds[" + index + "] must be greater than 0.");
            }
        }

        if (!WorldConfig.OrderRules.RESTART_CURRENT_LEVEL.equals(orderRules.failureBehavior)
                && !WorldConfig.OrderRules.RESTART_FROM_LEVEL_1.equals(orderRules.failureBehavior)) {
            throw invalid(path, "orderRules.failureBehavior must be '"
                    + WorldConfig.OrderRules.RESTART_CURRENT_LEVEL + "' or '"
                    + WorldConfig.OrderRules.RESTART_FROM_LEVEL_1 + "'.");
        }
    }

    private static void validateMessRules(WorldConfig.MessRules messRules, Path path) {
        if (messRules.spawnChanceOnProductPickup < 0.0f || messRules.spawnChanceOnProductPickup > 1.0f) {
            throw invalid(path, "messRules.spawnChanceOnProductPickup must be between 0 and 1.");
        }
        requirePositive(messRules.radius, "messRules.radius", path);
        if (messRules.speedMultiplier <= 0.0f || messRules.speedMultiplier > 1.0f) {
            throw invalid(path, "messRules.speedMultiplier must be greater than 0 and at most 1.");
        }
        if (messRules.durationSeconds < 0.0f) {
            throw invalid(path, "messRules.durationSeconds must be 0 or greater.");
        }
        requirePositive(messRules.cleaningDurationSeconds, "messRules.cleaningDurationSeconds", path);
    }

    private static void validateProductPlacement(WorldConfig.ProductSpawn product, int index,
            Map<String, WorldConfig.ShelfSpawn> shelvesById, Path path) {
        boolean usesShelfPlacement = hasText(product.shelfId);
        boolean usesAbsolutePlacement = product.position != null;

        if (usesShelfPlacement && usesAbsolutePlacement) {
            throw invalid(path, "products[" + index
                    + "] must use either shelfId + offsetFromShelf or position, not both.");
        }
        if (!usesShelfPlacement && !usesAbsolutePlacement) {
            throw invalid(path, "products[" + index
                    + "] must define either position or shelfId + offsetFromShelf.");
        }
        if (!usesShelfPlacement) {
            return;
        }

        requirePosition(product.offsetFromShelf, "products[" + index + "].offsetFromShelf", path);
        WorldConfig.ShelfSpawn shelf = shelvesById.get(product.shelfId);
        if (shelf == null) {
            throw invalid(path, "products[" + index + "].shelfId references unknown shelf '" + product.shelfId + "'.");
        }
        if (!shelf.spawnOnStart) {
            throw invalid(path, "products[" + index + "].shelfId references shelf '" + product.shelfId
                    + "', but that shelf does not spawn on start.");
        }
    }

    private static void requirePositive(float value, String fieldName, Path path) {
        if (value <= 0.0f) {
            throw invalid(path, fieldName + " must be greater than 0.");
        }
    }

    private static void requirePosition(Object position, String fieldName, Path path) {
        if (position == null) {
            throw invalid(path, fieldName + " is required.");
        }
    }

    private static void requireText(String value, String fieldName, Path path) {
        if (!hasText(value)) {
            throw invalid(path, fieldName + " is required.");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static IllegalStateException invalid(Path path, String message) {
        return new IllegalStateException("Invalid scenario config at " + path.toAbsolutePath() + ": " + message);
    }
}
