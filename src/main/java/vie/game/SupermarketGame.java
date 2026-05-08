package vie.game;

import vie.engine.rendering.Renderer;
import vie.engine.core.IAppLogic;
import vie.engine.scene.Entity;
import vie.engine.scene.Scene;
import vie.game.agents.WorkerAgent;
import vie.game.simulation.WarehouseSimulation;

public class SupermarketGame implements IAppLogic {
    private Scene scene;
    private WorkerAgent worker;
    // private OrderSystem orderSystem;
    private WarehouseSimulation simulation;
    private Renderer renderer;

    @Override
    public void init() {
        scene = new Scene();
        renderer = new Renderer();

        scene.addEntity(new Entity("Shelf_1", 0.0f, 0.0f, 0.0f));
        scene.addEntity(new Entity("Product_1", 1.0f, 0.5f, 0.0f));
        scene.addEntity(new Entity("OrderBox_1", 3.0f, 0.0f, 1.0f));
        scene.addEntity(new Entity("Wall_Left", -5.0f, 0.0f, 0.0f));
        scene.addEntity(new Entity("Wall_Right", 5.0f, 0.0f, 0.0f));
        scene.addEntity(new Entity("Wall_Top", 0.0f, 0.0f, -5.0f));
        scene.addEntity(new Entity("Wall_Bottom", 0.0f, 0.0f, -5.0f));

        // For Shaun
        simulation = new WarehouseSimulation();
        worker = new WorkerAgent(simulation.getOrderSystem());

        System.out.println("SupermarketGame initialized.");
        System.out.println("Entities in scene: " + scene.getEntities().size());
    }

    @Override
    public void input() {
    }

    @Override
    public void update(float deltaTime) {
        simulation.update(deltaTime);
        worker.update(deltaTime);
    }

    @Override
    public void render() {
        renderer.render(scene);
    }

    @Override
    public void cleanup() {
        System.out.println("SupermarketGame cleaned up.");
    }

    public Scene getScene() {
        return scene;
    }
}