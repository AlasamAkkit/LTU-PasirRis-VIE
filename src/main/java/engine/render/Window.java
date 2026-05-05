package engine.render;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Window {
    private final String title;
    private final int width;
    private final int height;
    private long handle;
    private float clearRed = 0.09f;
    private float clearGreen = 0.11f;
    private float clearBlue = 0.14f;
    private long lastFrameTimeNanos;
    
    // Mouse tracking for camera control
    private double lastMouseX;
    private double lastMouseY;
    private double mouseX;
    private double mouseY;
    public Window(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
    }

    public void create() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        handle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (handle == NULL) {
            throw new IllegalStateException("Unable to create GLFW window");
        }

        glfwMakeContextCurrent(handle);
        glfwSwapInterval(1);
        GL.createCapabilities();
        // Debug info: print GL vendor/renderer/version and native library path
        try {
            String vendor = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VENDOR);
            String renderer = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER);
            String version = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION);
            System.out.println("[Window] GL_VENDOR=" + vendor + " RENDERER=" + renderer + " VERSION=" + version);
            System.out.println("[Window] java.library.path=" + System.getProperty("java.library.path"));
            System.out.flush();
        } catch (Throwable t) {
            System.err.println("[Window] Failed to query GL strings: " + t.getMessage());
        }
        glEnable(GL_DEPTH_TEST);
        glfwShowWindow(handle);

        // Initialize mouse positions to current cursor to avoid large deltas on first frame
        double[] x = new double[1];
        double[] y = new double[1];
        glfwGetCursorPos(handle, x, y);
        mouseX = x[0];
        mouseY = y[0];
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        lastFrameTimeNanos = System.nanoTime();
    }

    public float beginFrame() {
        long now = System.nanoTime();
        float deltaSeconds = (now - lastFrameTimeNanos) / 1_000_000_000.0f;
        lastFrameTimeNanos = now;
        return deltaSeconds;
    }

    public void pollEvents() {
        glfwPollEvents();
    }

    public void clear() {
        glClearColor(clearRed, clearGreen, clearBlue, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void swapBuffers() {
        glfwSwapBuffers(handle);
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    public boolean isKeyPressed(int keyCode) {
        return glfwGetKey(handle, keyCode) == GLFW_PRESS;
    }

    public long getHandle() {
        return handle;
    }

    public void setClearColor(float red, float green, float blue) {
        this.clearRed = red;
        this.clearGreen = green;
        this.clearBlue = blue;
    }

    public void setTitle(String newTitle) {
        glfwSetWindowTitle(handle, newTitle);
    }

    public void requestClose() {
        glfwSetWindowShouldClose(handle, true);
    }

    /**
     * Set mouse capture mode for camera look-around.
     * When enabled, cursor is hidden and mouse position is locked to center.
     */
    public void setMouseCaptured(boolean captured) {
        if (captured) {
            glfwSetInputMode(handle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            // sync mouse tracking to current cursor to avoid a large initial delta
            double[] x = new double[1];
            double[] y = new double[1];
            glfwGetCursorPos(handle, x, y);
            mouseX = x[0];
            mouseY = y[0];
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        } else {
            glfwSetInputMode(handle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }
    }

    /**
     * Get mouse position delta since last frame (in pixels).
     * Only meaningful if mouse is captured.
     */
    public float getMouseDeltaX() {
        return (float)(mouseX - lastMouseX);
    }

    public float getMouseDeltaY() {
        return (float)(mouseY - lastMouseY);
    }

    /**
     * Update mouse position tracking (call each frame before using deltas).
     */
    public void updateMousePosition() {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        
        double[] x = new double[1];
        double[] y = new double[1];
        glfwGetCursorPos(handle, x, y);
        mouseX = x[0];
        mouseY = y[0];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void destroy() {
        if (handle != NULL) {
            glfwDestroyWindow(handle);
            handle = NULL;
        }
        glfwTerminate();
        GLFWErrorCallback currentErrorCallback = glfwSetErrorCallback(null);
        if (currentErrorCallback != null) {
            currentErrorCallback.free();
        }
    }
}