package engine.components;

public class ThemeSelectionComponent {
    public static final int STAGE_DEFAULT_OR_CUSTOM = 0;
    public static final int STAGE_FLOOR_COLOR = 1;
    public static final int STAGE_SHIRT_COLOR = 2;

    public boolean complete;
    public boolean customTheme;
    public int stage = STAGE_DEFAULT_OR_CUSTOM;

    public final float[] floorColor = new float[] { 0.30f, 0.30f, 0.30f, 1.0f };
    public final float[] shirtColor = new float[] { 0.12f, 0.36f, 0.80f, 1.0f };

    public String title = "Choose a theme";
    public String body = "1 Default theme\n2 Custom theme";
}