package engine.render;

import engine.components.RenderComponent;
import engine.components.TransformComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;

public final class RenderSystem implements GameSystem {
    @Override
    public void render(EcsWorld world) {
        // Starter renderer: validates renderable entities and leaves the actual mesh pipeline for the next iteration.
        // The loop is intentionally here so a future mesh/material submission step has a stable home.
        int renderableCount = 0;
        for (int entityId : world.getActiveEntityIds()) {
            TransformComponent transform = world.getComponent(entityId, TransformComponent.class);
            RenderComponent render = world.getComponent(entityId, RenderComponent.class);
            if (transform != null && render != null && render.visible) {
                renderableCount++;
            }
        }

        if (renderableCount < 0) {
            throw new IllegalStateException("Impossible render count");
        }
    }
}