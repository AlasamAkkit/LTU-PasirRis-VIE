package engine.components;

import engine.math.Vector3;

public class TransformComponent {
    public final Vector3 position = new Vector3();
    public final Vector3 rotation = new Vector3();
    public final Vector3 scale = new Vector3(1.0f, 1.0f, 1.0f);
}