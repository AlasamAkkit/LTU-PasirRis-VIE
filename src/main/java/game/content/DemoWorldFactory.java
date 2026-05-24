package game.content;

import game.config.WorldConfig;
import game.config.ScenarioConfigLoader;

public final class DemoWorldFactory {
    private DemoWorldFactory() {
    }

    public static WorldConfig createDefaultConfig() {
        return ScenarioConfigLoader.loadDefault();
    }
}
