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
                continue;
            }
            if (navigation.currentWaypointIndex >= navigation.path.size()) {
                navigation.reachedDestination = true;
                navigation.hasDestination = false;
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
                }
                continue;
            }

            float distance = (float) Math.sqrt(distanceSquared);
            float step = Math.min(distance, velocity.speed * deltaSeconds);
            transform.position.x += (deltaX / distance) * step;
            transform.position.y = 0.0f;
            transform.position.z += (deltaZ / distance) * step;
            navigation.reachedDestination = false;
        }
    }
}
