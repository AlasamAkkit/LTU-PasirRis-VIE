package engine.ecs;

public interface GameSystem {
    default void update(EcsWorld world, float deltaSeconds) {
    }

    default void render(EcsWorld world) {
    }

    default void cleanup(EcsWorld world) {
    }
}
