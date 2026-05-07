package vie.engine.rendering;

import vie.engine.scene.Entity;
import vie.engine.scene.Scene;

public class Renderer {

    public void render(Scene scene) {

        for (Entity entity : scene.getEntities()) {

            if (entity.getName().contains("Wall")) {
                System.out.println("Rendering wall: " + entity.getName());
            } else {
                System.out.println("Rendering entity: " + entity.getName());
            }
        }
    }
}