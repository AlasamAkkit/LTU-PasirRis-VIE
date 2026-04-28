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
            if (input.moveForward) {
                direction.z -= 1.0f;
            }
            if (input.moveBackward) {
                direction.z += 1.0f;
            }
            if (input.moveLeft) {
                direction.x -= 1.0f;
            }
            if (input.moveRight) {
                direction.x += 1.0f;
            }
            if (input.moveUp) {
                direction.y += 1.0f;
            }
            if (input.moveDown) {
                direction.y -= 1.0f;
            }

            float lengthSquared = direction.lengthSquared();
            if (lengthSquared > 0.0f) {
                float inverseLength = 1.0f / (float) Math.sqrt(lengthSquared);
                direction.scale(inverseLength);
                transform.position.add(direction.x * velocity.speed * deltaSeconds, direction.y * velocity.speed * deltaSeconds, direction.z * velocity.speed * deltaSeconds);
            }
        }
    }
}