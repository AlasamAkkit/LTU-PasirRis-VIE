package engine.input;

import org.lwjgl.glfw.GLFW;

import engine.components.InputComponent;
import engine.ecs.EcsWorld;
import engine.ecs.GameSystem;
import engine.render.Window;

public final class InputSystem implements GameSystem {
    private final Window window;
    private boolean previousInteractKeyDown;
    private boolean previousDropKeyDown;

    public InputSystem(Window window) {
        this.window = window;
    }

    @Override
    public void update(EcsWorld world, float deltaSeconds) {
        for (int entityId : world.getActiveEntityIds()) {
            InputComponent input = world.getComponent(entityId, InputComponent.class);
            if (input == null) {
                continue;
            }

            input.moveForward = window.isKeyPressed(GLFW.GLFW_KEY_W);
            input.moveBackward = window.isKeyPressed(GLFW.GLFW_KEY_S);
            input.moveLeft = window.isKeyPressed(GLFW.GLFW_KEY_A);
            input.moveRight = window.isKeyPressed(GLFW.GLFW_KEY_D);
            input.moveUp = window.isKeyPressed(GLFW.GLFW_KEY_E);
            input.moveDown = window.isKeyPressed(GLFW.GLFW_KEY_Q);

            boolean interactKeyDown = window.isKeyPressed(GLFW.GLFW_KEY_F);
            input.interactPressed = interactKeyDown && !previousInteractKeyDown;
            previousInteractKeyDown = interactKeyDown;

            boolean dropKeyDown = window.isKeyPressed(GLFW.GLFW_KEY_G);
            input.dropPressed = dropKeyDown && !previousDropKeyDown;
            previousDropKeyDown = dropKeyDown;
        }
    }
}