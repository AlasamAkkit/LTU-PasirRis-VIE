package game.content;

import game.config.WorldConfig;

public final class DemoWorldFactory {
    private DemoWorldFactory() {
    }

    public static WorldConfig createDefaultConfig() {
        return WorldConfig.demo();
    }
}