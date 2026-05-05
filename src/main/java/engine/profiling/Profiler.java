package engine.profiling;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Profiler {
    private final Map<String, Long> sectionStarts = new LinkedHashMap<>();
    private final Map<String, Double> lastSectionMillis = new LinkedHashMap<>();
    private long frameStartNanos;
    private double lastFrameMillis;

    public void beginFrame() {
        frameStartNanos = System.nanoTime();
    }

    public void endFrame(float deltaSeconds) {
        lastFrameMillis = (System.nanoTime() - frameStartNanos) / 1_000_000.0;
    }

    public void beginSection(String name) {
        sectionStarts.put(name, System.nanoTime());
    }

    public void endSection(String name) {
        Long startedAt = sectionStarts.get(name);
        if (startedAt == null) {
            return;
        }
        lastSectionMillis.put(name, (System.nanoTime() - startedAt) / 1_000_000.0);
    }

    public double getLastFrameMillis() {
        return lastFrameMillis;
    }

    public double getLastSectionMillis(String name) {
        return lastSectionMillis.getOrDefault(name, 0.0);
    }

    public Map<String, Double> snapshotSections() {
        return new LinkedHashMap<>(lastSectionMillis);
    }
}