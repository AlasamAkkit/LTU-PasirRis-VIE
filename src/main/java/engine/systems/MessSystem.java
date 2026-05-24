package engine.systems;

import engine.components.MessComponent;
import engine.components.InputComponent;
import engine.components.TransformComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;
import engine.math.Vector3;

public final class MessSystem implements GameSystem {
    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        for (int entityId : world.getActiveEntityIds()) {
            MessComponent mess = world.getComponent(entityId, MessComponent.class);
            if (mess == null) {
                continue;
            }

            updateCleaning(world, entityId, mess, deltaSeconds);
            if (!world.isAlive(entityId)) {
                continue;
            }

            if (!mess.expires) {
                continue;
            }

            mess.remainingSeconds -= deltaSeconds;
            if (mess.remainingSeconds <= 0.0f) {
                world.destroyEntity(entityId);
            }
        }
    }

    public static float speedMultiplierAt(EcsWorld world, Vector3 position) {
        float multiplier = 1.0f;
        for (int entityId : world.getActiveEntityIds()) {
            MessComponent mess = world.getComponent(entityId, MessComponent.class);
            TransformComponent messTransform = world.getComponent(entityId, TransformComponent.class);
            if (mess == null || messTransform == null) {
                continue;
            }

            float deltaX = position.x - messTransform.position.x;
            float deltaZ = position.z - messTransform.position.z;
            if (deltaX * deltaX + deltaZ * deltaZ <= mess.radius * mess.radius) {
                multiplier = Math.min(multiplier, mess.speedMultiplier);
            }
        }
        return multiplier;
    }

    private void updateCleaning(EcsWorld world, int messEntityId, MessComponent mess, float deltaSeconds) {
        if (mess.cleanerEntityId == -1 || !world.isAlive(mess.cleanerEntityId)) {
            resetCleaning(mess);
            return;
        }

        TransformComponent messTransform = world.getComponent(messEntityId, TransformComponent.class);
        TransformComponent cleanerTransform = world.getComponent(mess.cleanerEntityId, TransformComponent.class);
        if (messTransform == null || cleanerTransform == null) {
            resetCleaning(mess);
            return;
        }

        float deltaX = cleanerTransform.position.x - messTransform.position.x;
        float deltaZ = cleanerTransform.position.z - messTransform.position.z;
        if (deltaX * deltaX + deltaZ * deltaZ > mess.radius * mess.radius) {
            InputComponent input = world.getComponent(mess.cleanerEntityId, InputComponent.class);
            if (input != null) {
                input.feedbackMessage = "Cleaning cancelled";
                input.feedbackSecondsRemaining = 1.5f;
            }
            resetCleaning(mess);
            return;
        }

        mess.cleaningProgressSeconds += deltaSeconds;
        if (mess.cleaningProgressSeconds < mess.cleaningDurationSeconds) {
            return;
        }

        InputComponent input = world.getComponent(mess.cleanerEntityId, InputComponent.class);
        if (input != null) {
            input.feedbackMessage = "Mess cleaned";
            input.feedbackSecondsRemaining = 2.0f;
        }
        world.destroyEntity(messEntityId);
    }

    private void resetCleaning(MessComponent mess) {
        mess.cleanerEntityId = -1;
        mess.cleaningProgressSeconds = 0.0f;
    }
}
