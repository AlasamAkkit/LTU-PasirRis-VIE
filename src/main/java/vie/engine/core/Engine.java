package vie.engine.core;

import vie.engine.profiler.Profiler;
import vie.util.Config;

public class Engine {
    private final String title;
    private final IAppLogic appLogic;
    private final Time time;
    private final Profiler profiler;
    private boolean running;

    public Engine(String title, IAppLogic appLogic) {
        this.title = title;
        this.appLogic = appLogic;
        this.time = new Time();
        this.profiler = new Profiler();
        this.running = false;
    }

    public void start() {
        init();
        run();
        cleanup();
    }

    private void init() {
        appLogic.init();
        running = true;
        System.out.println("Engine started: " + title);
    }

    private void run() {
        int frameCount = 0;

        while (running) {
            time.update();
            float deltaTime = time.getDeltaTime();

            appLogic.input();

            profiler.start();
            appLogic.update(deltaTime);
            double updateMs = profiler.stopMillis();

            if (frameCount % 60 == 0) {
                if (updateMs > Config.TARGET_UPDATE_MS) {
                    System.out.println("WARNING: Simulation update exceeded target: " + updateMs + " ms");
                } else {
                    // System.out.println("Simulation update: " + updateMs + " ms");
                }
            }

            appLogic.render();
            frameCount++;
        }
    }

    private void cleanup() {
        appLogic.cleanup();
        System.out.println("Engine stopped.");
    }

    public void stop() {
        running = false;
    }

    public String getTitle() {
        return title;
    }
}