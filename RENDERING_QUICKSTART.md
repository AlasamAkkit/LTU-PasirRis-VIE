# Rendering System Quick-Start Guide

## What Was Built

You now have a complete rendering pipeline with 3 layers:

### Created Files

**Camera System** (`engine/camera/`)
- `Camera.java` - 3D camera with position, yaw/pitch, and matrix generation

**Graphics Abstractions** (`engine/graphics/`)
- `ShaderProgram.java` - OpenGL shader compilation and uniform management
- `Mesh.java` - Geometry storage (VAO/VBO/IBO abstractions)
- `PrimitiveFactory.java` - Generate placeholder cubes, planes, and rooms
- `SimpleShaders.java` - Built-in basic shader source code

**Rendering Coordinator** (`engine/rendering/`)
- `Renderer.java` - High-level draw commands and mesh caching

**Integration** (Updated existing files)
- `engine/render/Window.java` - Added mouse input tracking
- `engine/render/RenderSystem.java` - Integrated Renderer, Camera, and demo scene
- `game/scenes/SupermarketScene.java` - Updated to pass Window to RenderSystem

## How to Use It

### Test the Rendering System

```bash
cd c:\Users\shark\OneDrive\Documents\NTU\Y2S2\group\LTU-PasirRis-VIE
gradlew.bat run
```

**Expected Output:**
- Window opens (1280x720)
- You see an indoor room with walls, floor, shelves, products, and an order box
- Room is lit with flat colors (no dynamic lighting yet)

### Camera Controls

| Key | Action |
|-----|--------|
| **W** | Move forward |
| **S** | Move backward |
| **A** | Strafe left |
| **D** | Strafe right |
| **E** | Move up |
| **Q** | Move down |
| **Mouse** | Look around (yaw/pitch) |
| **ESC** | Close window (you can also click the window close button) |

- Camera starts at position (0, 2, 5) looking at the origin
- Movement speed: 5 units/second
- Mouse sensitivity: 0.1f (tunable in RenderSystem.java, line ~107)

## Architecture at a Glance

```
INPUT LAYER
│
├─► KeyboardInput (W/A/S/D/E/Q)
├─► MouseInput (delta X/Y for look-around)
│
CAMERA LAYER
│
├─► Camera.updatePosition(movementVector, deltaTime)
├─► Camera.updateRotation(mouseDelta, sensitivity)
├─► Generates View Matrix + Projection Matrix
│
RENDERING LAYER
│
├─► Renderer.setCamera(camera)
├─► Renderer.drawMeshByHandle(meshName, pos, scale, color)
├─► Shader Binding + Uniform Setting (MVP matrices, color)
├─► GPU Draw Call
│
GRAPHICS LAYER
│
├─► Mesh rendering (VAO/VBO/IBO)
├─► OpenGL API calls
└─► Framebuffer output
```

## Next Steps

### Stage 1: Full ECS-Based Rendering ✅ (Ready)

Currently, `RenderSystem.java` renders a hardcoded demo scene. To use real ECS entities:

1. In `RenderSystem.render()`, replace `renderDemoScene()` with actual entity iteration:

```java
for (int entityId : world.getActiveEntityIds()) {
    RenderComponent render = world.getComponent(entityId, RenderComponent.class);
    TransformComponent transform = world.getComponent(entityId, TransformComponent.class);
    
    if (render != null && transform != null && render.visible) {
        renderer.drawMeshByHandle(
            render.meshHandle,
            transform.position.x, transform.position.y, transform.position.z,
            transform.scale.x, transform.scale.y, transform.scale.z,
            1.0f, 1.0f, 1.0f, 1.0f  // White color (customize as needed)
        );
    }
}
```

2. Update `SpawnSystem.java` to set `meshHandle` on `RenderComponent`:
   - Products: `"cube"`
   - Shelves: `"cube"` (scaled larger)
   - OrderBox: `"cube"` (scaled smaller)

### Stage 2: Model Loading (Assimp) 🚧 (Future)

Once placeholder geometry works:

1. Create `engine/graphics/ModelLoader.java` using LWJGL Assimp
2. Load `.obj` or `.fbx` files from `assets/models/`
3. Cache loaded models in Renderer
4. Update `RenderComponent.meshHandle` to point to real models

### Stage 3: Lighting 🚧 (Future)

1. Add normals to Mesh vertex format
2. Create `LightComponent` (PointLight, DirectionalLight)
3. Update shaders to use lighting calculations
4. RenderSystem passes light uniforms to shader

### Stage 4: Textures 🚧 (Future)

1. Add UV coordinates to Mesh
2. Create `TextureProgram` and `Texture` classes
3. Load `.png` files from `assets/textures/`
4. Update `RenderComponent` with `materialHandle`

## Common Issues & Fixes

### Issue: Black screen or no geometry visible
- **Check**: Is the window opening? (Should see default clear color)
- **Fix**: Verify shader compilation succeeded (check console for GLSL errors)
- **Fix**: Ensure `renderer.registerMesh()` is called in `RenderSystem.initialize()`

### Issue: Camera doesn't move with WASD
- **Check**: Is `updateMousePosition()` being called each frame?
- **Check**: Is `InputComponent` being created for the player entity?
- **Fix**: Verify `InputSystem` is registered before `RenderSystem`

### Issue: Mouse look doesn't work
- **Check**: Is `window.setMouseCaptured(true)` called?
- **Check**: Is cursor visible? (Should be invisible when captured)
- **Fix**: Press ESC and re-run (may need to re-enable mouse capture)

### Issue: Geometry is inverted or culled
- **Check**: Is vertex winding counter-clockwise from outside?
- **Fix**: Swap indices or flip vertex order if backface culling hides geometry

### Issue: Crash with "GL error"
- **Check**: Are you calling `renderer.destroy()`?
- **Fix**: Ensure all GPU resources are freed at shutdown

## File Organization

```
src/main/java/
├── engine/
│   ├── camera/
│   │   └── Camera.java                 ← Camera system
│   ├── graphics/
│   │   ├── ShaderProgram.java          ← Shader abstraction
│   │   ├── Mesh.java                   ← Geometry abstraction
│   │   ├── PrimitiveFactory.java       ← Generate cubes, planes, rooms
│   │   └── SimpleShaders.java          ← Built-in shaders
│   ├── rendering/
│   │   └── Renderer.java               ← High-level renderer
│   ├── render/
│   │   ├── Window.java                 ← (Updated: mouse input)
│   │   └── RenderSystem.java           ← (Updated: integration layer)
│   └── (other systems)
│
└── game/
    ├── scenes/
    │   └── SupermarketScene.java       ← (Updated: passes Window to RenderSystem)
    └── (other game code)

docs/
└── rendering-architecture.md           ← Detailed documentation
```

## Performance Tips

1. **Use mesh caching** - Call `renderer.registerMesh()` once, then `drawMeshByHandle()` for repeated objects
2. **Minimize draw calls** - Group similar objects and render in batches
3. **Enable face culling** - Already done in Renderer.initialize()
4. **Lazy matrix computation** - Camera only recomputes when dirty
5. **Profile** - Use the Profiler system to identify bottlenecks

## Extending the Renderer

### Add a New Primitive
```java
// In PrimitiveFactory.java
public static Mesh createSphere(float radius, int stacks, int slices) {
    // Generate sphere vertices and indices
    // ... (implement geometry math)
    return new Mesh(positions, indices);
}

// In RenderSystem.initialize()
renderer.registerMesh("sphere", PrimitiveFactory.createSphere(1.0f, 16, 32));

// Draw it
renderer.drawMeshByHandle("sphere", x, y, z, scale, scale, scale, r, g, b, 1.0f);
```

### Add a New Shader
```java
// In SimpleShaders.java (or new file)
public static final String LIGHTING_VERTEX = """
    #version 330 core
    layout (location = 0) in vec3 aPosition;
    layout (location = 1) in vec3 aNormal;
    
    uniform mat4 uModel;
    uniform mat4 uView;
    uniform mat4 uProjection;
    
    out vec3 vNormal;
    out vec3 vFragPos;
    
    void main() {
        gl_Position = uProjection * uView * uModel * vec4(aPosition, 1.0);
        vFragPos = vec3(uModel * vec4(aPosition, 1.0));
        vNormal = mat3(transpose(inverse(uModel))) * aNormal;
    }
    """;

// Use in Renderer or RenderSystem
Renderer litRenderer = new Renderer();
litRenderer.initialize(SimpleShaders.LIGHTING_VERTEX, SimpleShaders.LIGHTING_FRAGMENT);
```

## Testing Checklist

- [ ] Window opens and displays room
- [ ] WASD keys move camera forward/backward/left/right
- [ ] E/Q keys move camera up/down
- [ ] Mouse look rotates camera (yaw/pitch)
- [ ] No crashes or GL errors in console
- [ ] Colored cubes (shelves, products, order box) are visible
- [ ] Pitch is clamped (can't flip upside down)
- [ ] Close button works

---

**Ready to test!** Run `gradlew.bat run` and inspect the rendered scene. 

If you have issues, check `docs/rendering-architecture.md` for detailed explanations or open an issue in the project repo.
