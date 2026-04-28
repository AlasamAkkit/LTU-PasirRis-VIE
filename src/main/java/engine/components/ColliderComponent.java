package engine.components;

import engine.math.Vector3;

public class ColliderComponent {
    public final Vector3 offset = new Vector3();
    public final Vector3 halfExtents = new Vector3(0.5f, 0.5f, 0.5f);
    public float interactionRadius = 1.0f;
    public boolean trigger = true;
}