package vie;

import vie.engine.core.Engine;
import vie.game.SupermarketGame;
import vie.util.Config;

public class Main {
    public static void main(String[] args) {
        Engine engine = new Engine(Config.WINDOW_TITLE, new SupermarketGame());
        engine.start();
    }
}