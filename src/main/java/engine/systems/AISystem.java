package engine.systems;

import engine.components.AIComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;

public final class AISystem implements GameSystem {
    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        for (int entityId : world.getActiveEntityIds()) {
            AIComponent ai = world.getComponent(entityId, AIComponent.class);
            if (ai == null) {
                continue;
            }

            // Future autonomous worker behavior will live here.
            if (ai.state == null) {
                ai.state = "idle";
            }
        }
    }
}