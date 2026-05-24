package game.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import engine.math.Vector3;

public final class ScenarioConfigLoader {
    private static final Path DEFAULT_SCENARIO_PATH = Path.of("assets", "config", "scenario.json");
    private static final Gson GSON = new GsonBuilder().create();
    private static final float SHELF_ANCHOR_Y = 0.9f;

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
        if (config.map == null) {
            config.map = new WorldConfig.MapSettings();
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
        if (config.orderRules.productTypes == null) {
            config.orderRules.productTypes = new ArrayList<>();
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

        validateMap(config.map, path);

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
        Set<String> productTypes = validateProductTypes(config.orderRules.productTypes, path);

        Map<String, WorldConfig.ShelfSpawn> shelvesById = new HashMap<>();
        Set<String> shelfIds = new HashSet<>();
        for (int index = 0; index < config.shelves.size(); index++) {
            WorldConfig.ShelfSpawn shelf = config.shelves.get(index);
            if (shelf == null) {
                throw invalid(path, "shelves[" + index + "] must not be null.");
            }
            requireText(shelf.id, "shelves[" + index + "].id", path);
            requirePosition(shelf.position, "shelves[" + index + "].position", path);
            if (shelf.spawnOnStart) {
                requireShelfHeight(shelf.position.y, "shelves[" + index + "].position.y", path);
            }
            requireFinite(shelf.rotationYDegrees, "shelves[" + index + "].rotationYDegrees", path);
            if (!shelfIds.add(shelf.id)) {
                throw invalid(path, "shelves[" + index + "].id duplicates shelf id '" + shelf.id + "'.");
            }
            shelvesById.put(shelf.id, shelf);
        }

        Set<String> spawnedProductTypes = new HashSet<>();
        for (int index = 0; index < config.products.size(); index++) {
            WorldConfig.ProductSpawn product = config.products.get(index);
            if (product == null) {
                throw invalid(path, "products[" + index + "] must not be null.");
            }
            if (!product.spawnOnStart) {
                continue;
            }
            requireText(product.productType, "products[" + index + "].productType", path);
            if (!productTypes.contains(product.productType)) {
                throw invalid(path, "products[" + index + "].productType '" + product.productType
                        + "' is not listed in orderRules.productTypes.");
            }
            validateColor(product.color, "products[" + index + "].color", path);
            spawnedProductTypes.add(product.productType);
            validateProductPlacement(product, index, shelvesById, path);
        }

        for (String productType : productTypes) {
            if (!spawnedProductTypes.contains(productType)) {
                throw invalid(path, "orderRules.productTypes includes '" + productType
                        + "', but no spawnOnStart product uses that productType.");
            }
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

        validateGameplayPlacement(config, shelvesById, path);
    }

    private static void validateMap(WorldConfig.MapSettings map, Path path) {
        requirePositive(map.width, "map.width", path);
        requirePositive(map.depth, "map.depth", path);
        requirePositive(map.wallHeight, "map.wallHeight", path);
        requirePositive(map.navigationCellSize, "map.navigationCellSize", path);
        requireNonNegative(map.playableMarginX, "map.playableMarginX", path);
        requireNonNegative(map.playableMarginZ, "map.playableMarginZ", path);
        if (map.minX() >= map.maxX()) {
            throw invalid(path, "map.width must be larger than twice map.playableMarginX.");
        }
        if (map.minZ() >= map.maxZ()) {
            throw invalid(path, "map.depth must be larger than twice map.playableMarginZ.");
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

    private static Set<String> validateProductTypes(List<String> productTypes, Path path) {
        Set<String> uniqueTypes = new HashSet<>();
        if (productTypes.isEmpty()) {
            throw invalid(path, "orderRules.productTypes must contain at least one product type.");
        }
        if (productTypes.size() > 9) {
            throw invalid(path, "orderRules.productTypes supports at most 9 product types.");
        }
        for (int index = 0; index < productTypes.size(); index++) {
            String productType = productTypes.get(index);
            requireText(productType, "orderRules.productTypes[" + index + "]", path);
            if (!uniqueTypes.add(productType)) {
                throw invalid(path, "orderRules.productTypes[" + index + "] duplicates product type '"
                        + productType + "'.");
            }
        }
        return uniqueTypes;
    }

    private static void validateGameplayPlacement(WorldConfig config,
            Map<String, WorldConfig.ShelfSpawn> shelvesById, Path path) {
        ArrayList<String> errors = new ArrayList<>();

        if (config.player.spawnOnStart) {
            requireInsideGameplayArea(errors, config.map, "player.position", config.player.position, 0.275f, 0.275f);
        }

        if (config.assistant.spawnOnStart) {
            requireInsideGameplayArea(errors, config.map, "assistant.position",
                    config.assistant.position, 0.275f, 0.275f);
        }

        for (int index = 0; index < config.shelves.size(); index++) {
            WorldConfig.ShelfSpawn shelf = config.shelves.get(index);
            if (shelf.spawnOnStart) {
                requireInsideGameplayArea(errors, config.map, "shelves[" + index + "].position",
                        shelf.position, rotatedHalfX(0.95f, 0.62f, shelf.rotationYDegrees),
                        rotatedHalfZ(0.95f, 0.62f, shelf.rotationYDegrees));
            }
        }

        for (int index = 0; index < config.products.size(); index++) {
            WorldConfig.ProductSpawn product = config.products.get(index);
            if (product.spawnOnStart) {
                Vector3 position = resolveProductPositionForValidation(product, shelvesById);
                requireInsideGameplayArea(errors, config.map, "products[" + index + "]", position,
                        productHalfX(product.productType), productHalfZ(product.productType));
            }
        }

        for (int index = 0; index < config.orderBoxes.size(); index++) {
            WorldConfig.OrderBoxSpawn orderBox = config.orderBoxes.get(index);
            if (orderBox.spawnOnStart) {
                requireInsideGameplayArea(errors, config.map, "orderBoxes[" + index + "].position",
                        orderBox.position, 0.45f, 0.45f);
            }
        }

        if (!errors.isEmpty()) {
            throw invalid(path, "Entities are outside the game locations:\n - " + String.join("\n - ", errors));
        }
    }

    private static Vector3 resolveProductPositionForValidation(WorldConfig.ProductSpawn product,
            Map<String, WorldConfig.ShelfSpawn> shelvesById) {
        if (hasText(product.shelfId)) {
            WorldConfig.ShelfSpawn shelf = shelvesById.get(product.shelfId);
            Vector3 offset = product.offsetFromShelf;
            Vector3 rotatedOffset = rotateShelfOffset(offset, shelf.rotationYDegrees);
            return new Vector3(
                    shelf.position.x + rotatedOffset.x,
                    SHELF_ANCHOR_Y + offset.y,
                    shelf.position.z + rotatedOffset.z);
        }
        return product.position;
    }

    private static void requireInsideGameplayArea(ArrayList<String> errors, WorldConfig.MapSettings map,
            String fieldName, Vector3 position, float halfX, float halfZ) {
        float minX = position.x - halfX;
        float maxX = position.x + halfX;
        float minZ = position.z - halfZ;
        float maxZ = position.z + halfZ;

        if (minX < map.minX() || maxX > map.maxX() || minZ < map.minZ() || maxZ > map.maxZ()) {
            errors.add(fieldName + " at x=" + position.x + ", z=" + position.z
                    + " has a footprint outside x[" + map.minX() + ", " + map.maxX() + "] and z["
                    + map.minZ() + ", " + map.maxZ() + "].");
        }
    }

    private static Vector3 rotateShelfOffset(Vector3 offset, float rotationYDegrees) {
        double radians = Math.toRadians(rotationYDegrees);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        return new Vector3(offset.x * cos + offset.z * sin, offset.y, -offset.x * sin + offset.z * cos);
    }

    private static float rotatedHalfX(float halfX, float halfZ, float rotationYDegrees) {
        double radians = Math.toRadians(rotationYDegrees);
        float cos = Math.abs((float) Math.cos(radians));
        float sin = Math.abs((float) Math.sin(radians));
        return halfX * cos + halfZ * sin;
    }

    private static float rotatedHalfZ(float halfX, float halfZ, float rotationYDegrees) {
        double radians = Math.toRadians(rotationYDegrees);
        float cos = Math.abs((float) Math.cos(radians));
        float sin = Math.abs((float) Math.sin(radians));
        return halfX * sin + halfZ * cos;
    }

    private static float productHalfX(String productType) {
        if ("milk".equals(productType)) {
            return 0.15f;
        }
        if ("bread".equals(productType)) {
            return 0.275f;
        }
        if ("apples".equals(productType)) {
            return 0.22f;
        }
        return 0.25f;
    }

    private static float productHalfZ(String productType) {
        if ("milk".equals(productType)) {
            return 0.15f;
        }
        if ("bread".equals(productType)) {
            return 0.175f;
        }
        if ("apples".equals(productType)) {
            return 0.22f;
        }
        return 0.25f;
    }

    private static void requirePositive(float value, String fieldName, Path path) {
        if (!Float.isFinite(value) || value <= 0.0f) {
            throw invalid(path, fieldName + " must be greater than 0.");
        }
    }

    private static void requireNonNegative(float value, String fieldName, Path path) {
        if (!Float.isFinite(value) || value < 0.0f) {
            throw invalid(path, fieldName + " must be 0 or greater.");
        }
    }

    private static void requireFinite(float value, String fieldName, Path path) {
        if (!Float.isFinite(value)) {
            throw invalid(path, fieldName + " must be a finite number.");
        }
    }

    private static void validateColor(float[] color, String fieldName, Path path) {
        if (color == null) {
            return;
        }
        if (color.length != 4) {
            throw invalid(path, fieldName + " must contain exactly 4 values.");
        }
        for (int index = 0; index < color.length; index++) {
            float value = color[index];
            if (!Float.isFinite(value) || value < 0.0f || value > 1.0f) {
                throw invalid(path, fieldName + "[" + index + "] must be between 0 and 1.");
            }
        }
    }

    private static void requirePosition(Object position, String fieldName, Path path) {
        if (position == null) {
            throw invalid(path, fieldName + " is required.");
        }
    }

    private static void requireShelfHeight(float value, String fieldName, Path path) {
        if (Math.abs(value - SHELF_ANCHOR_Y) > 0.001f) {
            throw invalid(path, fieldName + " must stay at " + SHELF_ANCHOR_Y
                    + ". Use position.x and position.z to move shelves on the floor; position.y is vertical height.");
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
