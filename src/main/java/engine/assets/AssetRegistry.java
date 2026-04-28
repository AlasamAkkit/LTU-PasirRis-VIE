package engine.assets;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AssetRegistry {
    private final Map<String, String> modelPaths = new LinkedHashMap<>();
    private final Map<String, String> texturePaths = new LinkedHashMap<>();

    public void registerModel(String handle, String path) {
        modelPaths.put(handle, path);
    }

    public void registerTexture(String handle, String path) {
        texturePaths.put(handle, path);
    }

    public String getModelPath(String handle) {
        return modelPaths.get(handle);
    }

    public String getTexturePath(String handle) {
        return texturePaths.get(handle);
    }
}