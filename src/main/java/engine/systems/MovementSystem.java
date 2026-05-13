package engine.systems;

import engine.components.InputComponent;
import engine.components.TransformComponent;
import engine.components.VelocityComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;

public final class MovementSystem implements GameSystem {
    private static final float MIN_X = -4.6f;
    private static final float MAX_X = 4.6f;
    private static final float MIN_Z = -4.4f;
    private static final float MAX_Z = 4.4f;

    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        for (int entityId : world.getActiveEntityIds()) {
            TransformComponent transform = world.getComponent(entityId, TransformComponent.class);
            VelocityComponent velocity = world.getComponent(entityId, VelocityComponent.class);
            InputComponent input = world.getComponent(entityId, InputComponent.class);

            if (transform == null || velocity == null || input == null) {
                continue;
            }

            float directionX = 0.0f;
            float directionZ = 0.0f;

            if (input.moveForward) {
                directionZ -= 1.0f;
            }
            if (input.moveBackward) {
                directionZ += 1.0f;
            }
            if (input.moveLeft) {
                directionX -= 1.0f;
            }
            if (input.moveRight) {
                directionX += 1.0f;
            }

            float lengthSquared = directionX * directionX + directionZ * directionZ;
            if (lengthSquared > 0.0f) {
                float inverseLength = 1.0f / (float) Math.sqrt(lengthSquared);
                directionX *= inverseLength;
                directionZ *= inverseLength;
                velocity.velocity.set(directionX * velocity.speed, 0.0f, directionZ * velocity.speed);
                transform.position.add(directionX * velocity.speed * deltaSeconds, 0.0f,
                        directionZ * velocity.speed * deltaSeconds);
                transform.position.x = clamp(transform.position.x, MIN_X, MAX_X);
                transform.position.y = 0.0f;
                transform.position.z = clamp(transform.position.z, MIN_Z, MAX_Z);
            } else {
                velocity.velocity.set(0.0f, 0.0f, 0.0f);
            }
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
