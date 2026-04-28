package engine.core;

import engine.ecs.EcsWorld;
import engine.profiling.Profiler;
import engine.render.Window;

public final class Engine {
    private final EcsWorld world;
    private final Window window;
    private final Scene scene;
    private final Profiler profiler;
    private boolean running;

    public Engine(EcsWorld world, Window window, Scene scene, Profiler profiler) {
        this.world = world;
        this.window = window;
        this.scene = scene;
        this.profiler = profiler;
    }

    public void run() {
        window.create();
        scene.load(world, window);
        running = true;

        try {
            while (running && !window.shouldClose()) {
                float deltaSeconds = window.beginFrame();

                window.pollEvents();

                profiler.beginSection("simulation");
                world.updateSystems(deltaSeconds);
                profiler.endSection("simulation");

                world.flushDestroyedEntities();

                window.clear();

                profiler.beginSection("render");
                world.renderSystems();
                profiler.endSection("render");

                window.swapBuffers();
                profiler.endFrame(deltaSeconds);
            }
        } finally {
            scene.unload(world, window);
            window.destroy();
        }
    }

    public void stop() {
        running = false;
    }
}