package game.content;

import engine.ecs.EcsWorld;
import engine.math.Vector3;
import engine.systems.SpawnSystem;
import game.config.WorldConfig;

import java.util.Random;

public final class MessFactory {
    private MessFactory() {
    }

    public static boolean maybeSpawnOnProductPickup(EcsWorld world, SpawnSystem spawnSystem,
            WorldConfig.MessRules rules, Random random, Vector3 position) {
        if (!rules.enabled || rules.spawnChanceOnProductPickup <= 0.0f) {
            return false;
        }
        if (random.nextFloat() > rules.spawnChanceOnProductPickup) {
            return false;
        }

        spawnSystem.spawnMess(world, position, rules.radius, rules.speedMultiplier, rules.durationSeconds,
                rules.cleaningDurationSeconds);
        return true;
    }
}
