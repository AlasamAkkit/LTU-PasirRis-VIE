package engine.graphics;

/**
 * SimpleShaders provides basic vertex and fragment shader source code.
 * 
 * Contains two shaders:
 * - Basic shader: position + flat color (suitable for placeholder geometry)
 * - Color per vertex shader: vertex colors for basic visual variety
 */
public class SimpleShaders {

    /**
     * Basic vertex shader.
     * Transforms vertex position by model/view/projection matrices.
     */
    public static final String BASIC_VERTEX = """
        #version 330 core
        
        layout (location = 0) in vec3 aPosition;
        
        uniform mat4 uModel;
        uniform mat4 uView;
        uniform mat4 uProjection;
        
        void main() {
            gl_Position = uProjection * uView * uModel * vec4(aPosition, 1.0);
        }
        """;

    /**
     * Basic fragment shader.
     * Simple flat color.
     */
    public static final String BASIC_FRAGMENT = """
        #version 330 core
        
        uniform vec4 uColor;
        
        out vec4 FragColor;
        
        void main() {
            FragColor = uColor;
        }
        """;

    /**
     * Vertex shader with per-vertex colors (extended version).
     * Can be used if vertex colors are added to Mesh in future.
     */
    public static final String COLORED_VERTEX = """
        #version 330 core
        
        layout (location = 0) in vec3 aPosition;
        layout (location = 1) in vec3 aColor;
        
        uniform mat4 uModel;
        uniform mat4 uView;
        uniform mat4 uProjection;
        
        out vec3 vColor;
        
        void main() {
            gl_Position = uProjection * uView * uModel * vec4(aPosition, 1.0);
            vColor = aColor;
        }
        """;

    /**
     * Fragment shader with per-vertex interpolated colors.
     */
    public static final String COLORED_FRAGMENT = """
        #version 330 core
        
        in vec3 vColor;
        
        out vec4 FragColor;
        
        void main() {
            FragColor = vec4(vColor, 1.0);
        }
        """;

    /**
     * Vertex shader for 2D text rendering.
     */
    public static final String TEXT_VERTEX = """
        #version 330 core

        layout (location = 0) in vec2 aPosition;
        layout (location = 1) in vec2 aTexCoord;

        uniform mat4 uProjection;

        out vec2 vTexCoord;

        void main() {
            vTexCoord = aTexCoord;
            gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
        }
        """;

    /**
     * Fragment shader for 2D text rendering.
     */
    public static final String TEXT_FRAGMENT = """
        #version 330 core

        in vec2 vTexCoord;

        uniform sampler2D uFontTexture;
        uniform vec4 uColor;

        out vec4 FragColor;

        void main() {
            float alpha = texture(uFontTexture, vTexCoord).r;
            FragColor = vec4(uColor.rgb, uColor.a * alpha);
        }
        """;
}
