package engine.graphics;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryUtil;

/**
 * Renders 2D text overlays using STB TrueType and a baked font atlas.
 */
public final class TextRenderer {
    private static final int FIRST_CHAR = 32;
    private static final int CHAR_COUNT = 96;

    private final int viewportWidth;
    private final int viewportHeight;
    private final int fontSize;

    private final ShaderProgram shader;
    private final STBTTBakedChar.Buffer charData;
    private final int textureId;
    private final int vaoId;
    private final int vboId;
    private final float[] projection = new float[16];

    private FloatBuffer vertexBuffer;
    private int vertexBufferCapacity = 0;

    public TextRenderer(int viewportWidth, int viewportHeight, String fontPath, int fontSize) throws IOException {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.fontSize = fontSize;

        this.shader = new ShaderProgram(SimpleShaders.TEXT_VERTEX, SimpleShaders.TEXT_FRAGMENT);
        this.charData = STBTTBakedChar.malloc(CHAR_COUNT);
        this.textureId = createFontTexture(fontPath, fontSize, charData);

        this.vaoId = GL30.glGenVertexArrays();
        this.vboId = GL15.glGenBuffers();
        setupBuffers();
        updateProjection();
    }

    public void drawText(String text, float x, float y, float scale, float[] color) {
        if (text == null || text.isEmpty()) {
            return;
        }

        int vertexCount = buildVertices(text, x, y, scale);
        if (vertexCount == 0) {
            return;
        }

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        shader.use();
        shader.setUniformMatrix4f("uProjection", projection);
        shader.setUniform4f("uColor", color[0], color[1], color[2], color[3]);
        shader.setUniform1i("uFontTexture", 0);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertexBuffer);

        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexCount);

        GL30.glBindVertexArray(0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        shader.unuse();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    public float getTextWidth(String text, float scale) {
        if (text == null || text.isEmpty()) {
            return 0.0f;
        }

        float width = 0.0f;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < FIRST_CHAR || c >= FIRST_CHAR + CHAR_COUNT) {
                continue;
            }
            STBTTBakedChar ch = charData.get(c - FIRST_CHAR);
            width += ch.xadvance() * scale;
        }

        return width;
    }

    public float getLineHeight(float scale) {
        return fontSize * 1.4f * scale;
    }

    private void updateProjection() {
        ortho(projection, 0.0f, viewportWidth, viewportHeight, 0.0f, -1.0f, 1.0f);
    }

    private int buildVertices(String text, float x, float y, float scale) {
        int maxVerts = text.length() * 6;
        ensureVertexCapacity(maxVerts);
        vertexBuffer.clear();

        float baseX = x;
        float baseY = y;
        float[] xpos = new float[] { x };
        float[] ypos = new float[] { y };
        STBTTAlignedQuad quad = STBTTAlignedQuad.malloc();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                xpos[0] = baseX;
                ypos[0] += getLineHeight(scale);
                continue;
            }
            if (c < FIRST_CHAR || c >= FIRST_CHAR + CHAR_COUNT) {
                continue;
            }

            STBTruetype.stbtt_GetBakedQuad(charData, 512, 512, c - FIRST_CHAR, xpos, ypos, quad, true);

            float x0 = baseX + (quad.x0() - baseX) * scale;
            float x1 = baseX + (quad.x1() - baseX) * scale;
            float y0 = baseY + (quad.y0() - baseY) * scale;
            float y1 = baseY + (quad.y1() - baseY) * scale;

            addVertex(x0, y0, quad.s0(), quad.t0());
            addVertex(x1, y0, quad.s1(), quad.t0());
            addVertex(x1, y1, quad.s1(), quad.t1());

            addVertex(x0, y0, quad.s0(), quad.t0());
            addVertex(x1, y1, quad.s1(), quad.t1());
            addVertex(x0, y1, quad.s0(), quad.t1());
        }

        quad.free();
        vertexBuffer.flip();
        return vertexBuffer.limit() / 4;
    }

    private void addVertex(float x, float y, float u, float v) {
        vertexBuffer.put(x).put(y).put(u).put(v);
    }

    private void ensureVertexCapacity(int vertexCount) {
        int requiredFloats = vertexCount * 4;
        if (requiredFloats <= vertexBufferCapacity) {
            return;
        }

        int newCapacity = Math.max(requiredFloats, vertexBufferCapacity * 2);
        if (vertexBuffer != null) {
            MemoryUtil.memFree(vertexBuffer);
        }
        vertexBuffer = MemoryUtil.memAllocFloat(newCapacity);
        vertexBufferCapacity = newCapacity;

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) newCapacity * 4, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    private void setupBuffers() {
        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);

        int initialCapacity = 1024 * 4;
        vertexBuffer = MemoryUtil.memAllocFloat(initialCapacity);
        vertexBufferCapacity = initialCapacity;
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) initialCapacity * 4, GL15.GL_DYNAMIC_DRAW);

        int stride = 4 * Float.BYTES;
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, stride, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 2L * Float.BYTES);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    private int createFontTexture(String fontPath, int fontSize, STBTTBakedChar.Buffer bakedChars) throws IOException {
        byte[] fontBytes = Files.readAllBytes(Path.of(fontPath));
        ByteBuffer fontData = MemoryUtil.memAlloc(fontBytes.length);
        fontData.put(fontBytes).flip();

        int bitmapW = 512;
        int bitmapH = 512;
        ByteBuffer bitmap = MemoryUtil.memAlloc(bitmapW * bitmapH);

        try {
            int result = STBTruetype.stbtt_BakeFontBitmap(fontData, fontSize, bitmap, bitmapW, bitmapH, FIRST_CHAR,
                    bakedChars);
            if (result <= 0) {
                throw new IOException("Failed to bake font bitmap: " + fontPath);
            }

            int texId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RED, bitmapW, bitmapH, 0, GL11.GL_RED,
                    GL11.GL_UNSIGNED_BYTE, bitmap);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

            return texId;
        } finally {
            MemoryUtil.memFree(bitmap);
            MemoryUtil.memFree(fontData);
        }
    }

    private void ortho(float[] out, float left, float right, float bottom, float top, float near, float far) {
        float rl = right - left;
        float tb = top - bottom;
        float fn = far - near;

        out[0] = 2.0f / rl;
        out[1] = 0.0f;
        out[2] = 0.0f;
        out[3] = 0.0f;

        out[4] = 0.0f;
        out[5] = 2.0f / tb;
        out[6] = 0.0f;
        out[7] = 0.0f;

        out[8] = 0.0f;
        out[9] = 0.0f;
        out[10] = -2.0f / fn;
        out[11] = 0.0f;

        out[12] = -(right + left) / rl;
        out[13] = -(top + bottom) / tb;
        out[14] = -(far + near) / fn;
        out[15] = 1.0f;
    }
}
