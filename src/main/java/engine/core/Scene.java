package engine.core;

import engine.ecs.EcsWorld;
import engine.render.Window;

public interface Scene {
    void load(EcsWorld world, Window window);

    default void unload(EcsWorld world, Window window) {
    }
}