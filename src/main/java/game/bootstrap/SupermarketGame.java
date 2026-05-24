package game.bootstrap;

import engine.core.Engine;
import engine.ecs.EcsWorld;
import engine.profiling.Profiler;
import engine.render.Window;
import game.config.WorldConfig;
import game.content.DemoWorldFactory;
import game.scenes.SupermarketScene;

public final class SupermarketGame {
    public void run() {
        WorldConfig worldConfig;
        try {
            worldConfig = DemoWorldFactory.createDefaultConfig();
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            System.err.flush();
            return;
        }

        EcsWorld world = new EcsWorld();
        Window window = new Window("LTU Pasir Ris VIE", 1280, 720);
        Profiler profiler = new Profiler();
        Engine engine = new Engine(world, window, new SupermarketScene(worldConfig), profiler);
        engine.run();
    }
}
