package vie.engine.core;

public interface IAppLogic {

    void init();

    void input();

    void update(float deltaTime);

    void render();

    void cleanup();
}