package vie.engine.core;

public class Time {
    private long lastTime;
    private float deltaTime;

    public Time() {
        lastTime = System.nanoTime();
        deltaTime = 0.0f;
    }

    public void update() {
        long currentTime = System.nanoTime();
        deltaTime = (currentTime - lastTime) / 1_000_000_000.0f;
        lastTime = currentTime;
    }

    public float getDeltaTime() {
        return deltaTime;
    }
}