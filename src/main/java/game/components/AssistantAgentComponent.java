package game.components;

import engine.math.Vector3;

public class AssistantAgentComponent {
    public final Vector3 homePosition = new Vector3();
    public String[] productChoices = new String[] { "bread", "milk", "apples" };
    public String phase = "idle";
    public String requestedProductType = "";
    public int targetProductEntityId = -1;
    public int targetOrderBoxEntityId = -1;
    public int lastInteractorEntityId = -1;
}
