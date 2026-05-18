package engine.systems;

import engine.components.NavigationAgentComponent;
import engine.components.TransformComponent;
import engine.components.VelocityComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;
import engine.math.Vector3;

public final class NavigationMovementSystem implements GameSystem {
    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        for (int entityId : world.getActiveEntityIds()) {
            TransformComponent transform = world.getComponent(entityId, TransformComponent.class);
            VelocityComponent velocity = world.getComponent(entityId, VelocityComponent.class);
            NavigationAgentComponent navigation = world.getComponent(entityId, NavigationAgentComponent.class);

            if (transform == null || velocity == null || navigation == null) {
                continue;
            }
            if (!navigation.hasDestination || !navigation.pathAvailable) {
                velocity.velocity.set(0.0f, 0.0f, 0.0f);
                continue;
            }
            if (navigation.currentWaypointIndex >= navigation.path.size()) {
                navigation.reachedDestination = true;
                navigation.hasDestination = false;
                velocity.velocity.set(0.0f, 0.0f, 0.0f);
                continue;
            }

            Vector3 waypoint = navigation.path.get(navigation.currentWaypointIndex);
            float deltaX = waypoint.x - transform.position.x;
            float deltaZ = waypoint.z - transform.position.z;
            float distanceSquared = deltaX * deltaX + deltaZ * deltaZ;
            float arriveDistanceSquared = navigation.arriveDistance * navigation.arriveDistance;

            if (distanceSquared <= arriveDistanceSquared) {
                navigation.currentWaypointIndex++;
                if (navigation.currentWaypointIndex >= navigation.path.size()) {
                    transform.position.x = waypoint.x;
                    transform.position.z = waypoint.z;
                    navigation.reachedDestination = true;
                    navigation.hasDestination = false;
                    velocity.velocity.set(0.0f, 0.0f, 0.0f);
                }
                continue;
            }

            float distance = (float) Math.sqrt(distanceSquared);
            float effectiveSpeed = velocity.speed * MessSystem.speedMultiplierAt(world, transform.position);
            float step = Math.min(distance, effectiveSpeed * deltaSeconds);
            float moveX = (deltaX / distance) * step;
            float moveZ = (deltaZ / distance) * step;
            transform.position.x += moveX;
            transform.position.y = 0.0f;
            transform.position.z += moveZ;
            velocity.velocity.set((deltaX / distance) * effectiveSpeed, 0.0f, (deltaZ / distance) * effectiveSpeed);
            navigation.reachedDestination = false;
        }
    }
}
