package engine.physics;

import engine.components.ColliderComponent;
import engine.components.TransformComponent;
import engine.components.VelocityComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;
import engine.math.Vector3;

public final class PhysicsSystem implements GameSystem {
    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        for (int entityId : world.getActiveEntityIds()) {
            TransformComponent transform = world.getComponent(entityId, TransformComponent.class);
            VelocityComponent velocity = world.getComponent(entityId, VelocityComponent.class);
            ColliderComponent collider = world.getComponent(entityId, ColliderComponent.class);

            if (transform == null || velocity == null || collider == null || collider.trigger) {
                continue;
            }
            if (velocity.velocity.lengthSquared() == 0.0f) {
                continue;
            }

            for (int otherId : world.getActiveEntityIds()) {
                if (otherId == entityId) {
                    continue;
                }

                TransformComponent otherTransform = world.getComponent(otherId, TransformComponent.class);
                ColliderComponent otherCollider = world.getComponent(otherId, ColliderComponent.class);

                if (otherTransform == null || otherCollider == null || otherCollider.trigger) {
                    continue;
                }

                if (checkAABBCollision(transform, collider, otherTransform, otherCollider)) {
                    transform.position.x -= velocity.velocity.x * deltaSeconds;
                    transform.position.y -= velocity.velocity.y * deltaSeconds;
                    transform.position.z -= velocity.velocity.z * deltaSeconds;
                    velocity.velocity.set(0.0f, 0.0f, 0.0f);
                    break;
                }
            }
        }
    }

    private boolean checkAABBCollision(TransformComponent transform1, ColliderComponent collider1,
            TransformComponent transform2, ColliderComponent collider2) {
        Vector3 min1 = new Vector3(
                transform1.position.x + collider1.offset.x - collider1.halfExtents.x,
                transform1.position.y + collider1.offset.y - collider1.halfExtents.y,
                transform1.position.z + collider1.offset.z - collider1.halfExtents.z);
        Vector3 max1 = new Vector3(
                transform1.position.x + collider1.offset.x + collider1.halfExtents.x,
                transform1.position.y + collider1.offset.y + collider1.halfExtents.y,
                transform1.position.z + collider1.offset.z + collider1.halfExtents.z);

        Vector3 min2 = new Vector3(
                transform2.position.x + collider2.offset.x - collider2.halfExtents.x,
                transform2.position.y + collider2.offset.y - collider2.halfExtents.y,
                transform2.position.z + collider2.offset.z - collider2.halfExtents.z);
        Vector3 max2 = new Vector3(
                transform2.position.x + collider2.offset.x + collider2.halfExtents.x,
                transform2.position.y + collider2.offset.y + collider2.halfExtents.y,
                transform2.position.z + collider2.offset.z + collider2.halfExtents.z);

        return min1.x < max2.x && max1.x > min2.x &&
                min1.y < max2.y && max1.y > min2.y &&
                min1.z < max2.z && max1.z > min2.z;
    }
}