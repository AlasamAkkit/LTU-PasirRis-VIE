package vie.engine.profiler;

public class Profiler {
    private long startTime;

    public void start() {
        startTime = System.nanoTime();
    }

    public double stopMillis() {
        long endTime = System.nanoTime();
        return (endTime - startTime) / 1_000_000.0;
    }
}