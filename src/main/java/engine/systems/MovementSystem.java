package engine.systems;

import engine.components.InputComponent;
import engine.components.TransformComponent;
import engine.components.VelocityComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;

public final class MovementSystem implements GameSystem {
    private static final float DEFAULT_MIN_X = -4.6f;
    private static final float DEFAULT_MAX_X = 4.6f;
    private static final float DEFAULT_MIN_Z = -4.4f;
    private static final float DEFAULT_MAX_Z = 4.4f;
    private final float minX;
    private final float maxX;
    private final float minZ;
    private final float maxZ;

    public MovementSystem() {
        this(DEFAULT_MIN_X, DEFAULT_MAX_X, DEFAULT_MIN_Z, DEFAULT_MAX_Z);
    }

    public MovementSystem(float minX, float maxX, float minZ, float maxZ) {
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

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
                float effectiveSpeed = velocity.speed * MessSystem.speedMultiplierAt(world, transform.position);
                velocity.velocity.set(directionX * effectiveSpeed, 0.0f, directionZ * effectiveSpeed);
                transform.position.add(directionX * effectiveSpeed * deltaSeconds, 0.0f,
                        directionZ * effectiveSpeed * deltaSeconds);
                transform.position.x = clamp(transform.position.x, minX, maxX);
                transform.position.y = 0.0f;
                transform.position.z = clamp(transform.position.z, minZ, maxZ);
            } else {
                velocity.velocity.set(0.0f, 0.0f, 0.0f);
            }
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
