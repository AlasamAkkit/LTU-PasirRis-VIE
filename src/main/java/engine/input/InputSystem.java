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
    private final boolean[] previousChoiceKeyDown = new boolean[3];

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
            input.moveUp = window.isKeyPressed(GLFW.GLFW_KEY_SPACE);
            input.moveDown = window.isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT);

            boolean interactKeyDown = window.isKeyPressed(GLFW.GLFW_KEY_E);
            input.interactPressed = interactKeyDown && !previousInteractKeyDown;
            if (input.interactPressed) {
                System.out.println("[InputSystem] E key pressed - interactPressed set to true");
                System.out.flush();
            }
            previousInteractKeyDown = interactKeyDown;

            boolean dropKeyDown = window.isKeyPressed(GLFW.GLFW_KEY_G);
            input.dropPressed = dropKeyDown && !previousDropKeyDown;
            previousDropKeyDown = dropKeyDown;

            input.dialogueChoicePressedIndex = -1;
            for (int i = 0; i < previousChoiceKeyDown.length; i++) {
                boolean choiceKeyDown = window.isKeyPressed(GLFW.GLFW_KEY_1 + i);
                if (choiceKeyDown && !previousChoiceKeyDown[i]) {
                    input.dialogueChoicePressedIndex = i;
                }
                previousChoiceKeyDown[i] = choiceKeyDown;
            }
        }
    }
}
