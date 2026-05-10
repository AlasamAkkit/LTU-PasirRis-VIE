package engine.graphics;

/**
 * Simple text renderer for displaying order status in console.
 * Provides formatted output for game UI information.
 */
public class TextRenderer {
    private final int viewportWidth;
    private final int viewportHeight;

    public TextRenderer(int viewportWidth, int viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }

    /**
     * Draw text at the given screen coordinates.
     * Currently prints to console for debugging/visibility.
     */
    public void drawText(String text, float screenX, float screenY, float scale, float[] color) {
        // Placeholder - text rendering requires proper font system
        // For now, information is shown in window title and console
    }

    /**
     * Draw a simple progress indicator at screen coordinates.
     * Shows a bar with text label.
     */
    public void drawProgressBar(String label, int filled, int total, float screenX, float screenY, 
                                 float[] labelColor, float[] filledColor, float[] emptyColor) {
        // Placeholder - progress bars are shown in window title and console
    }
}
