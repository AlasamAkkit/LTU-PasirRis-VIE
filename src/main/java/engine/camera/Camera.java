package engine.camera;

import engine.math.Vector3;

/**
 * Camera represents a view into 3D space.
 * 
 * Provides:
 * - Position (eye point)
 * - Yaw/Pitch orientation (Euler angles)
 * - View matrix (transforms world space to camera space)
 * - Projection matrix (transforms camera space to clip space)
 * 
 * Movement is controlled externally via update methods.
 * Camera is independent from gameplay logic—purely visual.
 */
public class Camera {
    private static final float FOV_DEGREES = 45.0f;
    private static final float NEAR_PLANE = 0.1f;
    private static final float FAR_PLANE = 1000.0f;

    private final Vector3 position;
    private float yaw;   // rotation around Y axis (left/right)
    private float pitch; // rotation around X axis (up/down)
    private int viewportWidth;
    private int viewportHeight;

    // Cache for view/projection matrices
    private final float[] viewMatrix;
    private final float[] projectionMatrix;
    private boolean viewDirty;
    private boolean projDirty;

    public Camera(int viewportWidth, int viewportHeight) {
        this.position = new Vector3(0, 1.6f, 3); // Eye height ~1.6m, looking at origin
        this.yaw = 0.0f;
        this.pitch = 0.0f;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.viewMatrix = new float[16];
        this.projectionMatrix = new float[16];
        this.viewDirty = true;
        this.projDirty = true;
    }

    /**
     * Update camera position based on input and delta time.
     * Assumes input uses world-space directions (WASD for forward/back/left/right, E/Q for up/down).
     */
    public void updatePosition(boolean moveForward, boolean moveBackward,
                               boolean moveLeft, boolean moveRight,
                               boolean moveUp, boolean moveDown,
                               float deltaSeconds) {
        float speed = 5.0f; // units per second
        float distance = speed * deltaSeconds;

        // Calculate forward/right vectors in world space from yaw
        float yawRad = (float) Math.toRadians(yaw);
        float cosYaw = (float) Math.cos(yawRad);
        float sinYaw = (float) Math.sin(yawRad);

        // Forward is -Z direction, rotated by yaw around Y axis
        float forwardX = -sinYaw;
        float forwardZ = -cosYaw;

        // Right is perpendicular to forward in XZ plane
        float rightX = cosYaw;
        float rightZ = sinYaw;

        if (moveForward) {
            position.x += forwardX * distance;
            position.z += forwardZ * distance;
        }
        if (moveBackward) {
            position.x -= forwardX * distance;
            position.z -= forwardZ * distance;
        }
        if (moveLeft) {
            position.x -= rightX * distance;
            position.z -= rightZ * distance;
        }
        if (moveRight) {
            position.x += rightX * distance;
            position.z += rightZ * distance;
        }

        viewDirty = true;
    }

    /**
     * Update camera rotation based on mouse delta.
     * Assumes deltaX/deltaY are in screen pixels.
     */
    public void updateRotation(float deltaX, float deltaY, float sensitivity) {
        yaw += deltaX * sensitivity;
        pitch -= deltaY * sensitivity;

        if (pitch > 89.0f) {
            pitch = 89.0f;
        }
        if (pitch < -89.0f) {
            pitch = -89.0f;
        }

        viewDirty = true;
    }

    /**
     * Get the view matrix (column-major, for OpenGL).
     * Lazily computed when dirty.
     */
    public float[] getViewMatrix() {
        if (viewDirty) {
            computeViewMatrix();
            viewDirty = false;
        }
        return viewMatrix;
    }

    /**
     * Get the projection matrix (column-major, for OpenGL).
     * Lazily computed when dirty.
     */
    public float[] getProjectionMatrix() {
        if (projDirty) {
            computeProjectionMatrix();
            projDirty = false;
        }
        return projectionMatrix;
    }

    private void computeViewMatrix() {
        // Convert yaw/pitch to forward/right/up vectors
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        float cosYaw = (float) Math.cos(yawRad);
        float sinYaw = (float) Math.sin(yawRad);
        float cosPitch = (float) Math.cos(pitchRad);
        float sinPitch = (float) Math.sin(pitchRad);

        // Forward vector (looking direction) - normalize
        float fx = -sinYaw * cosPitch;
        float fy = -sinPitch;
        float fz = -cosYaw * cosPitch;
        float fLen = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        if (fLen == 0) fLen = 1;
        float forwardX = fx / fLen;
        float forwardY = fy / fLen;
        float forwardZ = fz / fLen;

        // Compute right = normalize(cross(worldUp, forward))
        float upWorldX = 0f, upWorldY = 1f, upWorldZ = 0f;
        float rx = upWorldY * forwardZ - upWorldZ * forwardY;
        float ry = upWorldZ * forwardX - upWorldX * forwardZ;
        float rz = upWorldX * forwardY - upWorldY * forwardX;
        float rLen = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (rLen == 0) rLen = 1;
        float rightX = rx / rLen;
        float rightY = ry / rLen;
        float rightZ = rz / rLen;

        // Recompute true up = cross(forward, right) (already normalized)
        float upX = forwardY * rightZ - forwardZ * rightY;
        float upY = forwardZ * rightX - forwardX * rightZ;
        float upZ = forwardX * rightY - forwardY * rightX;

        // Construct orthonormal look-at view matrix in column-major order
        viewMatrix[0] = rightX;
        viewMatrix[1] = rightY;
        viewMatrix[2] = rightZ;
        viewMatrix[3] = 0;

        viewMatrix[4] = upX;
        viewMatrix[5] = upY;
        viewMatrix[6] = upZ;
        viewMatrix[7] = 0;

        viewMatrix[8] = -forwardX;
        viewMatrix[9] = -forwardY;
        viewMatrix[10] = -forwardZ;
        viewMatrix[11] = 0;

        // Translation part: -R * eye
        viewMatrix[12] = -(rightX * position.x + rightY * position.y + rightZ * position.z);
        viewMatrix[13] = -(upX * position.x + upY * position.y + upZ * position.z);
        viewMatrix[14] = (forwardX * position.x + forwardY * position.y + forwardZ * position.z);
        viewMatrix[15] = 1;
    }

    private void computeProjectionMatrix() {
        float aspect = (float) viewportWidth / viewportHeight;
        float fovRad = (float) Math.toRadians(FOV_DEGREES);
        float focalLength = 1.0f / (float) Math.tan(fovRad * 0.5f);

        // Perspective matrix, column-major
        projectionMatrix[0] = focalLength / aspect;
        projectionMatrix[1] = 0;
        projectionMatrix[2] = 0;
        projectionMatrix[3] = 0;

        projectionMatrix[4] = 0;
        projectionMatrix[5] = focalLength;
        projectionMatrix[6] = 0;
        projectionMatrix[7] = 0;

        projectionMatrix[8] = 0;
        projectionMatrix[9] = 0;
        projectionMatrix[10] = (FAR_PLANE + NEAR_PLANE) / (NEAR_PLANE - FAR_PLANE);
        projectionMatrix[11] = -1;

        projectionMatrix[12] = 0;
        projectionMatrix[13] = 0;
        projectionMatrix[14] = (2 * FAR_PLANE * NEAR_PLANE) / (NEAR_PLANE - FAR_PLANE);
        projectionMatrix[15] = 0;
    }

    public void setViewportSize(int width, int height) {
        if (width != viewportWidth || height != viewportHeight) {
            this.viewportWidth = width;
            this.viewportHeight = height;
            projDirty = true;
        }
    }

    public Vector3 getPosition() {
        return position;
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        viewDirty = true;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
        viewDirty = true;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        viewDirty = true;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }
}
