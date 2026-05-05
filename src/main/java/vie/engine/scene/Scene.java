package vie.engine.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Scene {
    private final List<Entity> entities;

    public Scene() {
        this.entities = new ArrayList<>();
    }

    public void addEntity(Entity entity) {
        entities.add(entity);
    }

    public void removeEntity(Entity entity) {
        entities.remove(entity);
    }

    public List<Entity> getEntities() {
        return Collections.unmodifiableList(entities);
    }
}