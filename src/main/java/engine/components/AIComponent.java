package engine.components;

import engine.math.Vector3;

public class AIComponent {
    public String state = "idle";
    public int targetEntityId = -1;
    public final Vector3 targetPosition = new Vector3();
}