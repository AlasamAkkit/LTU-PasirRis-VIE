package engine.components;

public class InputComponent {
    public boolean moveForward;
    public boolean moveBackward;
    public boolean moveLeft;
    public boolean moveRight;
    public boolean moveUp;
    public boolean moveDown;
    public boolean interactPressed;
    public boolean dropPressed;
    public boolean canInteract;
    public int selectedInteractableEntityId = -1;
    public String interactionMode = "none";
}