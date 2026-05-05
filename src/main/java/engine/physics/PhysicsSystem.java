package engine.physics;

import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;

public final class PhysicsSystem implements GameSystem {
    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        // Placeholder for the jBullet integration boundary.
        // Keep collision and rigid-body management isolated from gameplay systems.
    }
}