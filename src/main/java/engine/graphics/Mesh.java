package engine.graphics;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Mesh represents a piece of 3D geometry.
 * 
 * Stores:
 * - Vertex Array Object (VAO)
 * - Vertex Buffer Object (VBO) for positions
 * - Index Buffer Object (IBO) for faces
 * - Vertex count and index count
 * 
 * Handles:
 * - Creation and cleanup of GPU buffers
 * - Drawing (bind and render)
 * 
 * Vertex format (simple): position only (3 floats per vertex)
 * Can be extended to include normals, UVs, colors, etc.
 */
public class Mesh {
    private int vaoId;
    private int vboId;
    private int iboId;
    private int indexCount;

    /**
     * Create a mesh from vertex positions and indices.
     * 
     * @param positions Flat array of vertex positions [x0, y0, z0, x1, y1, z1, ...]
     * @param indices   Index array defining triangles [i0, i1, i2, i3, i4, i5, ...]
     */
    public Mesh(float[] positions, int[] indices) {
        this.indexCount = indices.length;
        System.out.println("[Mesh] Creating mesh with " + positions.length + " position floats and " + indices.length + " indices");
        createBuffers(positions, indices);
    }

    private void createBuffers(float[] positions, int[] indices) {
        // Create VAO
        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);
        System.out.println("[Mesh] Created VAO: " + vaoId);

        // Create and fill VBO for positions
        vboId = GL15.glGenBuffers();
        FloatBuffer posBuffer = MemoryUtil.memAllocFloat(positions.length);
        posBuffer.put(positions).flip();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, posBuffer, GL15.GL_STATIC_DRAW);
        MemoryUtil.memFree(posBuffer);
        System.out.println("[Mesh] Created VBO: " + vboId);

        // Set up vertex attribute pointer for positions (location 0, 3 floats per vertex)
        GL20.glVertexAttribPointer(0, 3, GL20.GL_FLOAT, false, 12, 0);
        GL20.glEnableVertexAttribArray(0);

        // Create and fill IBO for indices
        iboId = GL15.glGenBuffers();
        IntBuffer idxBuffer = MemoryUtil.memAllocInt(indices.length);
        idxBuffer.put(indices).flip();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, iboId);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, idxBuffer, GL15.GL_STATIC_DRAW);
        MemoryUtil.memFree(idxBuffer);
        System.out.println("[Mesh] Created IBO: " + iboId);

        // Unbind VAO (state is saved)
        GL30.glBindVertexArray(0);
        System.out.println("[Mesh] Mesh buffers created successfully");
    }

    /**
     * Render this mesh.
     * Assumes a shader program is already bound.
     */
    public void render() {
        GL30.glBindVertexArray(vaoId);
        GL15.glDrawElements(GL15.GL_TRIANGLES, indexCount, GL15.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
    }

    public void destroy() {
        if (vaoId != 0) {
            GL30.glDeleteVertexArrays(vaoId);
            vaoId = 0;
        }
        if (vboId != 0) {
            GL15.glDeleteBuffers(vboId);
            vboId = 0;
        }
        if (iboId != 0) {
            GL15.glDeleteBuffers(iboId);
            iboId = 0;
        }
    }

    public int getVaoId() {
        return vaoId;
    }

    public int getIndexCount() {
        return indexCount;
    }
}
