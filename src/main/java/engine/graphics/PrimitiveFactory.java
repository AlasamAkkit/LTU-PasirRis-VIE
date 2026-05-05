package engine.graphics;

/**
 * PrimitiveFactory generates common placeholder geometries.
 * 
 * Provides:
 * - Cube (6 faces, 24 vertices unique per face)
 * - Plane (flat quad)
 * - Simple room (4 walls, floor, optional ceiling)
 * 
 * Used to bootstrap visual testing without complex model loading.
 * Can be extended with more shapes as needed.
 */
public class PrimitiveFactory {

    /**
     * Create a unit cube centered at origin.
     * Size: 1x1x1 (from -0.5 to 0.5 in all axes)
     * Returns a Mesh ready to render.
     */
    public static Mesh createCube() {
        // 24 vertices: 4 per face (6 faces)
        // Organized by face: front, back, left, right, top, bottom
        float[] positions = {
            // Front face (z = 0.5)
            -0.5f, -0.5f, 0.5f,
            0.5f, -0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            -0.5f, 0.5f, 0.5f,

            // Back face (z = -0.5)
            0.5f, -0.5f, -0.5f,
            -0.5f, -0.5f, -0.5f,
            -0.5f, 0.5f, -0.5f,
            0.5f, 0.5f, -0.5f,

            // Left face (x = -0.5)
            -0.5f, -0.5f, -0.5f,
            -0.5f, -0.5f, 0.5f,
            -0.5f, 0.5f, 0.5f,
            -0.5f, 0.5f, -0.5f,

            // Right face (x = 0.5)
            0.5f, -0.5f, 0.5f,
            0.5f, -0.5f, -0.5f,
            0.5f, 0.5f, -0.5f,
            0.5f, 0.5f, 0.5f,

            // Top face (y = 0.5)
            -0.5f, 0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            0.5f, 0.5f, -0.5f,
            -0.5f, 0.5f, -0.5f,

            // Bottom face (y = -0.5)
            -0.5f, -0.5f, -0.5f,
            0.5f, -0.5f, -0.5f,
            0.5f, -0.5f, 0.5f,
            -0.5f, -0.5f, 0.5f
        };

        int[] indices = {
            // Front face (z = 0.5)
            0, 1, 2,
            2, 3, 0,

            // Back face (z = -0.5)
            4, 5, 6,
            6, 7, 4,

            // Left face (x = -0.5)
            8, 9, 10,
            10, 11, 8,

            // Right face (x = 0.5)
            12, 13, 14,
            14, 15, 12,

            // Top face (y = 0.5)
            16, 17, 18,
            18, 19, 16,

            // Bottom face (y = -0.5)
            20, 21, 22,
            22, 23, 20
        };

        return new Mesh(positions, indices);
    }

    /**
     * Create a unit plane (flat quad) on the XZ plane at y = 0.
     * Size: 1x1 (from -0.5 to 0.5 in X and Z)
     */
    public static Mesh createPlane() {
        float[] positions = {
            -0.5f, 0, -0.5f,
            0.5f, 0, -0.5f,
            0.5f, 0, 0.5f,
            -0.5f, 0, 0.5f
        };

        int[] indices = {
            0, 1, 2,
            2, 3, 0
        };

        return new Mesh(positions, indices);
    }

    /**
     * Create a simple room: 4 walls and a floor.
     * No ceiling (so you can see inside).
     * 
     * Dimensions: width x height x depth (centered at origin, on ground at y=0).
     * Walls are thin (no thickness, single-sided).
     */
    public static Mesh createRoom(float width, float height, float depth) {
        // Room layout:
        // - Floor: XZ plane at y=0, spans [-width/2, width/2] x [-depth/2, depth/2]
        // - Walls: 4 walls at the edges
        //   - Front wall (z = depth/2): from [-width/2, 0] to [width/2, height]
        //   - Back wall (z = -depth/2): from [-width/2, 0] to [width/2, height]
        //   - Left wall (x = -width/2): from [-depth/2, 0] to [depth/2, height]
        //   - Right wall (x = width/2): from [-depth/2, 0] to [depth/2, height]

        float w = width / 2.0f;
        float h = height;
        float d = depth / 2.0f;

        float[] positions = {
            // Floor (0-3)
            -w, 0, -d,
            w, 0, -d,
            w, 0, d,
            -w, 0, d,

            // Front wall (4-7): z = d
            -w, 0, d,
            w, 0, d,
            w, h, d,
            -w, h, d,

            // Back wall (8-11): z = -d
            w, 0, -d,
            -w, 0, -d,
            -w, h, -d,
            w, h, -d,

            // Left wall (12-15): x = -w
            -w, 0, -d,
            -w, 0, d,
            -w, h, d,
            -w, h, -d,

            // Right wall (16-19): x = w
            w, 0, d,
            w, 0, -d,
            w, h, -d,
            w, h, d
        };

        int[] indices = {
            // Floor (CCW from top view)
            0, 2, 1,
            2, 0, 3,

            // Front wall (CCW from inside)
            4, 6, 5,
            6, 4, 7,

            // Back wall (CCW from inside)
            8, 10, 9,
            10, 8, 11,

            // Left wall (CCW from inside)
            12, 14, 13,
            14, 12, 15,

            // Right wall (CCW from inside)
            16, 18, 17,
            18, 16, 19
        };

        return new Mesh(positions, indices);
    }

    /**
     * Create a scaled cube for use as an object (shelf, product, box, etc.)
     * 
     * @param scaleX Width
     * @param scaleY Height
     * @param scaleZ Depth
     */
    public static Mesh createScaledCube(float scaleX, float scaleY, float scaleZ) {
        float hx = scaleX / 2.0f;
        float hy = scaleY / 2.0f;
        float hz = scaleZ / 2.0f;

        float[] positions = {
            // Front face
            -hx, -hy, hz,
            hx, -hy, hz,
            hx, hy, hz,
            -hx, hy, hz,

            // Back face
            hx, -hy, -hz,
            -hx, -hy, -hz,
            -hx, hy, -hz,
            hx, hy, -hz,

            // Left face
            -hx, -hy, -hz,
            -hx, -hy, hz,
            -hx, hy, hz,
            -hx, hy, -hz,

            // Right face
            hx, -hy, hz,
            hx, -hy, -hz,
            hx, hy, -hz,
            hx, hy, hz,

            // Top face
            -hx, hy, hz,
            hx, hy, hz,
            hx, hy, -hz,
            -hx, hy, -hz,

            // Bottom face
            -hx, -hy, -hz,
            hx, -hy, -hz,
            hx, -hy, hz,
            -hx, -hy, hz
        };

        int[] indices = {
            0, 1, 2, 2, 3, 0,       // Front
            4, 5, 6, 6, 7, 4,       // Back
            8, 9, 10, 10, 11, 8,    // Left
            12, 13, 14, 14, 15, 12, // Right
            16, 17, 18, 18, 19, 16, // Top
            20, 21, 22, 22, 23, 20  // Bottom
        };

        return new Mesh(positions, indices);
    }
}
