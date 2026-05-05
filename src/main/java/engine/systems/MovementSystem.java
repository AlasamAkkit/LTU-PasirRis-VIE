package engine.systems;

import engine.components.InputComponent;
import engine.components.TransformComponent;
import engine.components.VelocityComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;
import engine.math.Vector3;

public final class MovementSystem implements GameSystem {
    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        for (int entityId : world.getActiveEntityIds()) {
            TransformComponent transform = world.getComponent(entityId, TransformComponent.class);
            VelocityComponent velocity = world.getComponent(entityId, VelocityComponent.class);
            InputComponent input = world.getComponent(entityId, InputComponent.class);

            if (transform == null || velocity == null || input == null) {
                continue;
            }

            Vector3 direction = new Vector3();
            float yawRad = (float) Math.toRadians(transform.rotation.y);
            float cosYaw = (float) Math.cos(yawRad);
            float sinYaw = (float) Math.sin(yawRad);

            float forwardX = -sinYaw;
            float forwardZ = -cosYaw;
            float rightX = cosYaw;
            float rightZ = sinYaw;

            if (input.moveForward) {
                direction.x += forwardX;
                direction.z += forwardZ;
            }
            if (input.moveBackward) {
                direction.x -= forwardX;
                direction.z -= forwardZ;
            }
            if (input.moveLeft) {
                direction.x -= rightX;
                direction.z -= rightZ;
            }
            if (input.moveRight) {
                direction.x += rightX;
                direction.z += rightZ;
            }

            float lengthSquared = direction.x * direction.x + direction.z * direction.z;
            if (lengthSquared > 0.0f) {
                float inverseLength = 1.0f / (float) Math.sqrt(lengthSquared);
                direction.x *= inverseLength;
                direction.z *= inverseLength;
                transform.position.add(direction.x * velocity.speed * deltaSeconds, 0.0f, direction.z * velocity.speed * deltaSeconds);
                // Ensure player Y position stays at ground level (0.0f)
                transform.position.y = 0.0f;
            }
        }
    }
}