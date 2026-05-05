package engine.rendering;

import org.lwjgl.opengl.GL11;
import java.util.HashMap;
import java.util.Map;

import engine.camera.Camera;
import engine.graphics.Mesh;
import engine.graphics.ShaderProgram;

/**
 * Renderer orchestrates all drawing operations.
 * 
 * Responsibilities:
 * - Manage shader programs
 * - Manage meshes (cache, lookup)
 * - Coordinate draw calls
 * - Handle matrices and uniforms
 * - Provide high-level drawing commands (drawMesh, drawObject, etc.)
 * 
 * Usage pattern:
 * 1. renderer.beginFrame()
 * 2. renderer.setCamera(camera)
 * 3. renderer.drawMesh(mesh, position, scale, color)
 * 4. renderer.endFrame()
 * 
 * Mesh handles are string identifiers ("cube", "plane", "room", etc.)
 * The renderer caches meshes to avoid redundant GPU uploads.
 */
public class Renderer {
    private ShaderProgram basicShader;
    private final Map<String, Mesh> meshCache;
    private Camera activeCamera;
    private int viewportWidth = 1280;
    private int viewportHeight = 720;
    private int drawCallCount = 0;

    // Temporary matrices for draw operations
    private final float[] modelMatrix;
    private int debugMatrixLogs = 0;

    public Renderer() {
        this.meshCache = new HashMap<>();
        this.modelMatrix = new float[16];
    }

    /**
     * Check for GL errors and log them.
     */
    private void checkGLError(String context) {
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            System.err.println("[Renderer] GL ERROR in " + context + ": 0x" + Integer.toHexString(error));
        }
    }

    /**
     * Initialize the renderer.
     * Compiles shaders and sets up basic rendering state.
     */
    public void initialize(String vertexShaderSource, String fragmentShaderSource) {
        // Compile shader
        this.basicShader = new ShaderProgram(vertexShaderSource, fragmentShaderSource);
        checkGLError("after shader creation");

        // Set viewport
        GL11.glViewport(0, 0, viewportWidth, viewportHeight);
        checkGLError("after glViewport");
        System.out.println("[Renderer] Viewport set to " + viewportWidth + "x" + viewportHeight);

        // Enable face culling for better performance (optional)
        // NOTE: disable face culling during early debugging to avoid hidden geometry
        GL11.glDisable(GL11.GL_CULL_FACE);
        System.out.println("[Renderer] Face culling disabled for debug");
        checkGLError("after face culling setup");
    }

    /**
     * Called at the start of a frame.
     * Clears the screen and prepares for drawing.
     */
    public void beginFrame() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        checkGLError("after glClear");
        drawCallCount = 0;
    }

    /**
     * Called at the end of a frame.
     * (Currently a no-op; can be used for cleanup or post-processing hooks.)
     */
    public void endFrame() {
        // Unbind any shader
        basicShader.unuse();
    }

    /**
     * Set the camera for subsequent draw calls.
     * The renderer will use this camera's view and projection matrices.
     */
    public void setCamera(Camera camera) {
        this.activeCamera = camera;
    }

    /**
     * Register a mesh in the cache so it can be referenced by handle.
     */
    public void registerMesh(String handle, Mesh mesh) {
        meshCache.put(handle, mesh);
    }

    /**
     * Get a mesh by handle.
     */
    public Mesh getMesh(String handle) {
        return meshCache.get(handle);
    }

    /**
     * Set the viewport size (call after renderer initialization).
     */
    public void setViewportSize(int width, int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
        GL11.glViewport(0, 0, width, height);
    }

    /**
     * Draw a mesh at a given position and scale with a color.
     * 
     * @param mesh Mesh to draw
     * @param posX X position
     * @param posY Y position
     * @param posZ Z position
     * @param scaleX X scale
     * @param scaleY Y scale
     * @param scaleZ Z scale
     * @param r Red (0-1)
     * @param g Green (0-1)
     * @param b Blue (0-1)
     * @param a Alpha (0-1)
     */
    public void drawMesh(Mesh mesh, float posX, float posY, float posZ,
                         float scaleX, float scaleY, float scaleZ,
                         float r, float g, float b, float a) {
        if (mesh == null || activeCamera == null) {
            System.err.println("[Renderer] Cannot draw: mesh=" + (mesh != null) + " camera=" + (activeCamera != null));
            System.err.flush();
            return;
        }

        drawCallCount++;

        // Build model matrix: translate and scale
        identityMatrix(modelMatrix);
        translateMatrix(modelMatrix, posX, posY, posZ);
        scaleMatrixInPlace(modelMatrix, scaleX, scaleY, scaleZ);

        // Use shader and set uniforms
        basicShader.use();
        checkGLError("after shader use");

        int progId = basicShader.getId();
        int locModel = org.lwjgl.opengl.GL20.glGetUniformLocation(progId, "uModel");
        int locView = org.lwjgl.opengl.GL20.glGetUniformLocation(progId, "uView");
        int locProj = org.lwjgl.opengl.GL20.glGetUniformLocation(progId, "uProjection");
        int locColor = org.lwjgl.opengl.GL20.glGetUniformLocation(progId, "uColor");
        System.out.flush();

        basicShader.setUniformMatrix4f("uModel", modelMatrix);
        checkGLError("after uModel uniform");

        basicShader.setUniformMatrix4f("uView", activeCamera.getViewMatrix());
        checkGLError("after uView uniform");

        basicShader.setUniformMatrix4f("uProjection", activeCamera.getProjectionMatrix());
        checkGLError("after uProjection uniform");

        // Debug: print a few view/projection matrix entries on the first frames
        if (debugMatrixLogs < 3) {
            float[] v = activeCamera.getViewMatrix();
            float[] p = activeCamera.getProjectionMatrix();
            System.out.println("[Renderer] VIEW t=(" + v[12] + "," + v[13] + "," + v[14] + ") rot0..2=(" + v[0] + "," + v[1] + "," + v[2] + ")");
            System.out.println("[Renderer] PROJ diag=(" + p[0] + "," + p[5] + "," + p[10] + ") p14=" + p[14]);
            System.out.flush();
            debugMatrixLogs++;
        }

        basicShader.setUniform4f("uColor", r, g, b, a);
        checkGLError("after uColor uniform");

        // Draw with normal fill mode and depth testing enabled.
        mesh.render();
        checkGLError("after mesh render");

    }

    /**
     * Convenience: draw a mesh by handle (lookup from cache).
     */
    public void drawMeshByHandle(String meshHandle, float posX, float posY, float posZ,
                                  float scaleX, float scaleY, float scaleZ,
                                  float r, float g, float b, float a) {
        Mesh mesh = getMesh(meshHandle);
        if (mesh != null) {
            System.out.flush();
            drawMesh(mesh, posX, posY, posZ, scaleX, scaleY, scaleZ, r, g, b, a);
        } else {
            System.err.println("[Renderer] Mesh not found: " + meshHandle);
            System.err.flush();
        }
    }

    /**
     * Draw a mesh with identity scale and a given color.
     */
    public void drawMesh(Mesh mesh, float posX, float posY, float posZ,
                         float r, float g, float b, float a) {
        drawMesh(mesh, posX, posY, posZ, 1, 1, 1, r, g, b, a);
    }

    // ========== Matrix utilities ==========

    private void identityMatrix(float[] m) {
        for (int i = 0; i < 16; i++) {
            m[i] = 0;
        }
        m[0] = m[5] = m[10] = m[15] = 1;
    }

    private void translateMatrix(float[] m, float x, float y, float z) {
        m[12] = x;
        m[13] = y;
        m[14] = z;
    }

    private void scaleMatrixInPlace(float[] m, float sx, float sy, float sz) {
        m[0] *= sx;
        m[1] *= sx;
        m[2] *= sx;
        m[3] *= sx;

        m[4] *= sy;
        m[5] *= sy;
        m[6] *= sy;
        m[7] *= sy;

        m[8] *= sz;
        m[9] *= sz;
        m[10] *= sz;
        m[11] *= sz;
    }

    public void destroy() {
        for (Mesh mesh : meshCache.values()) {
            mesh.destroy();
        }
        meshCache.clear();
        if (basicShader != null) {
            basicShader.destroy();
        }
    }
}
