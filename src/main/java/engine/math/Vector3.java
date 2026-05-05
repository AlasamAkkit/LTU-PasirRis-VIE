package engine.math;

public class Vector3 {
    public float x;
    public float y;
    public float z;

    public Vector3() {
        this(0.0f, 0.0f, 0.0f);
    }

    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3 set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3 set(Vector3 other) {
        return set(other.x, other.y, other.z);
    }

    public Vector3 add(float x, float y, float z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public Vector3 add(Vector3 other) {
        return add(other.x, other.y, other.z);
    }

    public Vector3 scale(float scalar) {
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        return this;
    }

    public float lengthSquared() {
        return x * x + y * y + z * z;
    }

    public float distanceSquared(Vector3 other) {
        float deltaX = x - other.x;
        float deltaY = y - other.y;
        float deltaZ = z - other.z;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }

    public Vector3 copy() {
        return new Vector3(x, y, z);
    }

    @Override
    public String toString() {
        return "Vector3(" + x + ", " + y + ", " + z + ")";
    }
}