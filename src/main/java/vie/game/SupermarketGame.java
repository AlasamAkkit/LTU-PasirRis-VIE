package vie.game;

import vie.engine.core.IAppLogic;
import vie.engine.scene.Entity;
import vie.engine.scene.Scene;

public class SupermarketGame implements IAppLogic {
    private Scene scene;

    @Override
    public void init() {
        scene = new Scene();

        scene.addEntity(new Entity("Shelf_1", 0.0f, 0.0f, 0.0f));
        scene.addEntity(new Entity("Product_1", 1.0f, 0.5f, 0.0f));
        scene.addEntity(new Entity("OrderBox_1", 3.0f, 0.0f, 1.0f));

        System.out.println("SupermarketGame initialized.");
    }

    @Override
    public void input() {
    }

    @Override
    public void update(float deltaTime) {
    }

    @Override
    public void render() {
    }

    @Override
    public void cleanup() {
        System.out.println("SupermarketGame cleaned up.");
    }

    public Scene getScene() {
        return scene;
    }
}