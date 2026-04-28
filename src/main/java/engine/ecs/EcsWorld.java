package engine.ecs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EcsWorld {
    private final Map<Class<?>, Map<Integer, Object>> componentStores = new HashMap<>();
    private final List<GameSystem> systems = new ArrayList<>();
    private final Set<Integer> activeEntities = new LinkedHashSet<>();
    private final Set<Integer> destroyedEntities = new LinkedHashSet<>();
    private int nextEntityId = 1;

    public int createEntity() {
        int entityId = nextEntityId++;
        activeEntities.add(entityId);
        return entityId;
    }

    public void destroyEntity(int entityId) {
        if (activeEntities.contains(entityId)) {
            destroyedEntities.add(entityId);
        }
    }

    public boolean isAlive(int entityId) {
        return activeEntities.contains(entityId) && !destroyedEntities.contains(entityId);
    }

    public <T> T addComponent(int entityId, T component) {
        if (component == null) {
            throw new IllegalArgumentException("component cannot be null");
        }
        if (!isAlive(entityId)) {
            throw new IllegalStateException("Entity " + entityId + " is not active");
        }

        componentStores.computeIfAbsent(component.getClass(), ignored -> new HashMap<>()).put(entityId, component);
        return component;
    }

    @SuppressWarnings("unchecked")
    public <T> T getComponent(int entityId, Class<T> componentType) {
        Map<Integer, Object> store = componentStores.get(componentType);
        if (store == null) {
            return null;
        }
        return (T) store.get(entityId);
    }

    public <T> T requireComponent(int entityId, Class<T> componentType) {
        T component = getComponent(entityId, componentType);
        if (component == null) {
            throw new IllegalStateException("Entity " + entityId + " is missing component " + componentType.getSimpleName());
        }
        return component;
    }

    public <T> boolean hasComponent(int entityId, Class<T> componentType) {
        Map<Integer, Object> store = componentStores.get(componentType);
        return store != null && store.containsKey(entityId);
    }

    public <T> void removeComponent(int entityId, Class<T> componentType) {
        Map<Integer, Object> store = componentStores.get(componentType);
        if (store != null) {
            store.remove(entityId);
        }
    }

    public void registerSystem(GameSystem system) {
        systems.add(system);
    }

    public List<Integer> getActiveEntityIds() {
        return new ArrayList<>(activeEntities);
    }

    public void updateSystems(float deltaSeconds) {
        for (GameSystem system : systems) {
            system.update(this, deltaSeconds);
        }
    }

    public void renderSystems() {
        for (GameSystem system : systems) {
            system.render(this);
        }
    }

    public void flushDestroyedEntities() {
        if (destroyedEntities.isEmpty()) {
            return;
        }

        for (int entityId : destroyedEntities) {
            for (Map<Integer, Object> store : componentStores.values()) {
                store.remove(entityId);
            }
            activeEntities.remove(entityId);
        }

        destroyedEntities.clear();
    }
}