# Rendering Architecture Documentation

This document explains the rendering subsystem architecture for the LTU Pasir Ris VIE engine.

## Overview

The rendering system is split into **3 independent layers**:

1. **Camera Layer** (`engine.camera.Camera`)
   - Handles 3D view frustum, position, rotation
   - Generates view/projection matrices
   - Independent from gameplay logic

2. **Graphics Layer** (`engine.graphics.*`)
   - Low-level GPU abstractions
   - `ShaderProgram`: Shader compilation and uniform management
   - `Mesh`: Geometry storage (VAO/VBO/IBO)
   - `PrimitiveFactory`: Generate placeholder geometry (cubes, planes, rooms)
   - `SimpleShaders`: Built-in shader source code

3. **Rendering Layer** (`engine.rendering.Renderer`)
   - High-level rendering coordination
   - Mesh caching and lookup
   - Draw command batching
   - Matrix computations and state management

## File Responsibility

### engine/camera/Camera.java
- **Purpose**: Represent a camera viewing the 3D world
- **Key Methods**:
  - `updatePosition(moveX, moveY, moveZ, ..., deltaTime)` - Keyboard-based movement (WASD + E/Q)
  - `updateRotation(deltaX, deltaY, sensitivity)` - Mouse-look rotation (yaw/pitch)
  - `getViewMatrix()` / `getProjectionMatrix()` - Column-major matrices for OpenGL
- **Internals**:
  - Position stored as `Vector3`
  - Rotation stored as yaw (Y-axis rotation) and pitch (X-axis rotation)
  - Matrices lazily computed when parameters change (dirty-flag optimization)
  - Pitch clamped to ±89° to prevent gimbal lock
- **Notes**: 
  - Self-contained; doesn't depend on ECS or gameplay
  - Can be driven by keyboard/mouse input externally
  - Speed = 5 units/second, FOV = 45°, near = 0.1, far = 1000

### engine/graphics/ShaderProgram.java
- **Purpose**: Wraps an OpenGL shader program (vertex + fragment)
- **Key Methods**:
  - `new ShaderProgram(vertexSource, fragmentSource)` - Compile and link
  - `use()` / `unuse()` - Bind/unbind for rendering
  - `setUniform1i/1f/3f/4f()` - Set scalar and vector uniforms
  - `setUniformMatrix4f(name, float[16])` - Set 4x4 matrices (for MVP)
  - `destroy()` - Clean up GPU resources
- **Error Handling**:
  - Throws `RuntimeException` if shader compilation fails (includes GLSL error log)
  - Throws `RuntimeException` if program linking fails
- **Notes**:
  - Supports column-major matrix layout (OpenGL standard)
  - Uniform setters use location caching for efficiency

### engine/graphics/Mesh.java
- **Purpose**: Store and render 3D geometry
- **Key Methods**:
  - `new Mesh(float[] positions, int[] indices)` - Create from vertex/index arrays
  - `render()` - Draw the mesh (assumes shader is bound)
  - `destroy()` - Clean up GPU resources
- **Internals**:
  - **Vertex format**: Position only (3 floats per vertex, no normals/UVs yet)
  - **Storage**: VAO + VBO (positions) + IBO (indices)
  - **Attribute layout**: Location 0 = position
- **Extension Points**:
  - Can add `layout (location = 1) in vec3 aNormal` for lighting
  - Can add `layout (location = 2) in vec2 aTexCoord` for textures
  - Shader code must match vertex format

### engine/graphics/PrimitiveFactory.java
- **Purpose**: Generate common placeholder geometries
- **Methods**:
  - `createCube()` - Unit cube (1x1x1, centered at origin)
  - `createPlane()` - Flat quad on XZ plane (1x1)
  - `createRoom(width, height, depth)` - Room with 4 walls + floor (no ceiling)
  - `createScaledCube(scaleX, scaleY, scaleZ)` - Cube with custom dimensions
- **Notes**:
  - All geometries use **counter-clockwise (CCW) winding** from outside/inside
  - Room is single-sided (no thickness); walls visible from inside only
  - No texturing or complex materials yet
  - Fast static factory (no caching—caller must cache if needed)

### engine/graphics/SimpleShaders.java
- **Purpose**: Built-in shader source code
- **Shaders**:
  - `BASIC_VERTEX` / `BASIC_FRAGMENT` - Simple flat color (MVP transform + uniform color)
  - `COLORED_VERTEX` / `COLORED_FRAGMENT` - Per-vertex colors (for future use)
- **Uniforms** (basic shader):
  - `uModel` (mat4) - Model transformation
  - `uView` (mat4) - View transformation (from camera)
  - `uProjection` (mat4) - Projection transformation
  - `uColor` (vec4) - RGBA flat color for entire mesh
- **Notes**:
  - GLSL 330 (OpenGL 3.3+)
  - Can be replaced with external shader files later
  - Colored shader is stubbed for future per-vertex color rendering

### engine/rendering/Renderer.java
- **Purpose**: High-level rendering coordinator
- **Key Methods**:
  - `initialize(vertexShader, fragmentShader)` - Compile shaders and set up state
  - `beginFrame()` - Clear screen and prepare for drawing
  - `setCamera(camera)` - Set active camera for MVP matrices
  - `registerMesh(handle, mesh)` - Cache a mesh by string identifier
  - `getMesh(handle)` - Retrieve cached mesh
  - `drawMesh(mesh, x, y, z, sx, sy, sz, r, g, b, a)` - Draw at position/scale with color
  - `drawMeshByHandle(handle, ...)` - Draw using cached mesh handle
  - `endFrame()` - Finalize frame (unbind shaders)
  - `destroy()` - Clean up all GPU resources
- **Internals**:
  - Mesh cache: `Map<String, Mesh>` for fast lookup
  - Matrix utilities: identity, translate, scale operations (column-major)
  - Lazy shader binding optimizations possible (future enhancement)
- **Notes**:
  - Single shader program (can extend to multiple later)
  - Face culling enabled (backface culling)
  - No depth writing/reading yet (basic drawing only)

### engine/render/RenderSystem.java
- **Purpose**: ECS system that orchestrates all rendering
- **Responsibilities**:
  - Initialize Renderer, Camera, and Meshes (lazy on first render)
  - Poll mouse input via Window
  - Update camera from InputComponent + mouse
  - Call Renderer to draw scene
  - (Future) Draw entities based on RenderComponent + TransformComponent
- **Current Behavior**:
  - Renders demo scene (room, shelves, products, order box)
  - Placeholder until full ECS-based rendering is ready
- **Integration Points**:
  - Takes `Window` in constructor (for mouse/keyboard input)
  - Queries `InputComponent` for WASD/E/Q keys
  - Will query `RenderComponent` + `TransformComponent` for entity drawing
  - Calls `renderer.drawMeshByHandle()` for each object
- **Notes**:
  - Mouse sensitivity = 0.1f (tunable)
  - Fixed 16ms delta time (60 FPS estimate, should use actual delta from Engine)
  - Mouse capture enabled (invisible cursor, locked to window)

### engine/render/Window.java (Enhanced)
- **New Methods**:
  - `setMouseCaptured(bool)` - Enable/disable mouse capture (GLFW_CURSOR_DISABLED)
  - `updateMousePosition()` - Poll and track cursor position each frame
  - `getMouseDeltaX()` / `getMouseDeltaY()` - Get pixel delta since last frame
  - `getWidth()` / `getHeight()` - Get viewport dimensions (for camera aspect ratio)
- **Usage Pattern**:
  ```java
  window.setMouseCaptured(true);  // Once during initialization
  
  // Each frame:
  window.updateMousePosition();
  float dx = window.getMouseDeltaX();
  float dy = window.getMouseDeltaY();
  camera.updateRotation(dx, dy, sensitivity);
  ```

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│               RenderSystem (ECS System)                 │
│  - Updates camera from input                            │
│  - Calls renderer to draw                               │
└────────────────┬────────────────────────────────────────┘
                 │
         ┌───────┴─────────┐
         │                 │
    ┌────▼────┐      ┌─────▼──────────┐
    │ Camera  │      │  Renderer      │
    │ ────    │      │  ────────      │
    │ pos     │      │  shaders       │
    │ yaw/pit │      │  mesh cache    │
    │ matrices│      │  draw commands │
    └────▲────┘      └─────┬──────────┘
         │                 │
    ┌────┴────────┐   ┌────▼─────────┐
    │   Input     │   │   Graphics   │
    │ (keyboard   │   │   ────────   │
    │  + mouse)   │   │ ShaderProgram│
    │             │   │ Mesh         │
    │             │   │ PrimitiveFact│
    └─────────────┘   └──────────────┘
```

## Data Flow (Per Frame)

1. **Poll Input**
   - InputSystem reads keyboard (GLFW)
   - InputComponent stores state in ECS
   - Window tracks mouse position

2. **Update Camera**
   - RenderSystem reads InputComponent
   - Camera moves based on WASD/E/Q
   - Camera rotates based on mouse delta
   - View/projection matrices recalculated

3. **Render**
   - Renderer clears screen
   - Renderer sets camera uniforms
   - For each entity with RenderComponent:
     - Look up mesh by handle
     - Get position/scale from TransformComponent
     - Call `renderer.drawMesh(...)`
   - Swap buffers

## Coordinate System

- **Right-handed**: X = right, Y = up, Z = backward (toward viewer initially)
- **Camera**: Looks down -Z axis by default
- **Rotations**: Yaw rotates around Y (left/right), Pitch rotates around X (up/down)
- **OpenGL**: Column-major matrix layout

## Future Extensions

### 1. Full ECS-Based Rendering
Replace demo scene drawing with proper entity iteration:
```java
for (int entityId : world.getActiveEntityIds()) {
    RenderComponent render = world.getComponent(entityId, RenderComponent.class);
    TransformComponent transform = world.getComponent(entityId, TransformComponent.class);
    if (render != null && transform != null) {
        renderer.drawMeshByHandle(
            render.meshHandle,
            transform.position.x, transform.position.y, transform.position.z,
            transform.scale.x, transform.scale.y, transform.scale.z,
            colorR, colorG, colorB, 1.0f
        );
    }
}
```

### 2. Model Loading (Assimp)
- Extend `PrimitiveFactory` or create new `ModelLoader`
- Use LWJGL Assimp bindings to load .obj, .fbx, .gltf, etc.
- Cache loaded models by path in Renderer
- Update `RenderComponent.meshHandle` to point to real models

### 3. Lighting
- Add normals to Mesh vertex format
- Extend SimpleShaders with Phong/PBR lighting
- Add light sources to ECS (PointLight, DirectionalLight components)
- Pass light uniforms to shader

### 4. Texturing
- Add UV coordinates to Mesh vertex format
- Create TextureProgram and Texture classes
- Update Renderer to bind textures before drawing
- Update RenderComponent to include materialHandle

### 5. Advanced Rendering
- Deferred rendering for many lights
- Shadow mapping
- Post-processing (bloom, FXAA, etc.)
- Particle systems

## Performance Considerations

- **Matrix Caching**: Camera lazily recomputes matrices only when dirty
- **Mesh Caching**: Renderer caches meshes to avoid redundant GPU uploads
- **Face Culling**: Backface culling enabled for 2x performance
- **Draw Call Batching**: Future enhancement—batch similar meshes together

## Common Pitfalls

1. **Forgetting to call `updateMousePosition()`** → Mouse delta always 0
2. **Not calling `renderer.destroy()`** → GPU memory leak
3. **Shader uniform names don't match source** → Black/missing geometry
4. **Index count mismatch in Mesh** → Garbage rendering or crashes
5. **Column-major vs. row-major confusion** → Inverted transformations

## Testing the Rendering System

1. **Compile**: `gradlew.bat compileJava`
2. **Run**: `gradlew.bat run`
3. **Expected**: 
   - Window opens with indoor room
   - WASD moves camera forward/back/left/right
   - E/Q moves up/down
   - Mouse look rotates view
   - Colored cubes (shelves, products, box) visible in room

---

**Status**: Stage 1-2 complete (Camera + ShaderProgram + Mesh + Renderer). Stage 3-4 ready for testing. Stage 5-6 in development.
