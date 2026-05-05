package engine.graphics;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL11;

/**
 * ShaderProgram wraps a compiled and linked OpenGL shader program.
 * 
 * Handles:
 * - Compilation of vertex/fragment shaders
 * - Linking into a program
 * - Uniform setting (matrices, colors, lights, etc.)
 * - Program use/binding
 * 
 * Students: Load shader source from strings or files.
 * This minimal version assumes source strings.
 */
public class ShaderProgram {
    private int programId;
    private int vertexShaderId;
    private int fragmentShaderId;

    /**
     * Create and compile a shader program from vertex and fragment source.
     */
    public ShaderProgram(String vertexSource, String fragmentSource) {
        this.vertexShaderId = compileShader(vertexSource, GL20.GL_VERTEX_SHADER);
        this.fragmentShaderId = compileShader(fragmentSource, GL20.GL_FRAGMENT_SHADER);
        this.programId = linkProgram(vertexShaderId, fragmentShaderId);
    }

    private int compileShader(String source, int shaderType) {
        int shaderId = GL20.glCreateShader(shaderType);
        GL20.glShaderSource(shaderId, source);
        GL20.glCompileShader(shaderId);

        // Check for compilation errors
        int status = GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS);
        if (status != GL11.GL_TRUE) {
            String log = GL20.glGetShaderInfoLog(shaderId, 1024);
            String shaderName = (shaderType == GL20.GL_VERTEX_SHADER) ? "Vertex" : "Fragment";
            throw new RuntimeException(shaderName + " Shader compilation failed:\n" + log);
        }

        return shaderId;
    }

    private int linkProgram(int vertexId, int fragmentId) {
        int progId = GL20.glCreateProgram();
        GL20.glAttachShader(progId, vertexId);
        GL20.glAttachShader(progId, fragmentId);
        GL20.glLinkProgram(progId);

        // Check for linking errors
        int status = GL20.glGetProgrami(progId, GL20.GL_LINK_STATUS);
        if (status != GL11.GL_TRUE) {
            String log = GL20.glGetProgramInfoLog(progId, 1024);
            throw new RuntimeException("Shader Program linking failed:\n" + log);
        }

        return progId;
    }

    public void use() {
        GL20.glUseProgram(programId);
    }

    public void unuse() {
        GL20.glUseProgram(0);
    }

    // Uniform setters
    public void setUniform1i(String name, int value) {
        int loc = GL20.glGetUniformLocation(programId, name);
        GL20.glUniform1i(loc, value);
    }

    public void setUniform1f(String name, float value) {
        int loc = GL20.glGetUniformLocation(programId, name);
        GL20.glUniform1f(loc, value);
    }

    public void setUniform3f(String name, float x, float y, float z) {
        int loc = GL20.glGetUniformLocation(programId, name);
        GL20.glUniform3f(loc, x, y, z);
    }

    public void setUniform4f(String name, float x, float y, float z, float w) {
        int loc = GL20.glGetUniformLocation(programId, name);
        GL20.glUniform4f(loc, x, y, z, w);
    }

    public void setUniformMatrix4f(String name, float[] matrix) {
        int loc = GL20.glGetUniformLocation(programId, name);
        GL20.glUniformMatrix4fv(loc, false, matrix);
    }

    public void destroy() {
        if (vertexShaderId != 0) {
            GL20.glDeleteShader(vertexShaderId);
        }
        if (fragmentShaderId != 0) {
            GL20.glDeleteShader(fragmentShaderId);
        }
        if (programId != 0) {
            GL20.glDeleteProgram(programId);
        }
    }

    public int getId() {
        return programId;
    }
}
