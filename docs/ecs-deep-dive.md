# ECS Design and Implementation Deep Dive

This document is a focused deep dive into the ECS architecture used in LTU-PasirRis-VIE. It explains how the engine is structured, how the game layer builds on top of it, where the performance-critical paths are, and what kinds of optimizations are most meaningful for the current codebase. The goal is not to describe ECS in the abstract, but to describe how this repository actually uses ECS in practice.

## 1. ECS Design

The project uses an ECS-style architecture because the gameplay is driven by many small pieces of state that need to be recombined in different ways. A supermarket scene naturally fits this model: a player, products, shelves, messes, order boxes, dialogue choices, and navigation agents all share some world rules, but they do not share one common object hierarchy. ECS lets the project describe those objects as data rather than as tightly coupled classes.

In this codebase, an entity is not a rich game object with methods. An entity is an integer ID created by `EcsWorld`. That ID is only meaningful once components are attached to it. This is an important design choice because it makes the entity itself almost free of behavior. The behavior is instead expressed through component combinations and the systems that interpret those combinations.

Components are the data layer. They hold values like position, scale, collision bounds, interaction mode, inventory contents, order state, task progress, navigation paths, and render metadata. A component does not orchestrate gameplay. It stores the data that other systems will consume later in the frame. That keeps each component simple, predictable, and easy to serialize or reason about.

Systems are the behavior layer. A system scans the world’s active entity IDs, checks whether the components it cares about are present, and then reads or mutates those components. This means a system does not need to know which concrete class owns a piece of logic, because the logic is distributed through data access patterns. The system only needs to know the component schema.

The scene layer is the orchestration boundary. `SupermarketScene` decides which systems are registered, in what order they run, and which initial entities are spawned. That is the point where the generic engine becomes a specific game. The engine provides the runtime and the rules for data flow; the scene provides the supermarket-specific arrangement of content and behavior.

This separation matters. It means the engine can remain reusable while the game content changes. If another game were built on top of the same engine, the core loop, ECS storage, rendering pipeline, and input handling could remain largely unchanged. The new game would mostly replace the scene, content, and gameplay systems.

## 2. Implementation Structure

The runtime center of the ECS implementation is `EcsWorld`. It owns entity creation, component storage, system registration, and entity destruction. That makes it the shared container for the simulation state. In practical terms, this is where the current scene’s active gameplay world lives.

The implementation is intentionally straightforward. Components are stored in maps keyed by component class, and each component class maps entity IDs to component instances. This is not the most advanced ECS design possible, but it is a practical and understandable one for a coursework-sized project. It favors clarity over extreme cache optimization, which is appropriate because the project’s scope is moderate.

The engine bootstrap is also deliberately lightweight. `Main` starts the application, `SupermarketGame` creates the shared engine and world objects, `Engine` opens the window and loads the scene, and `SupermarketScene` performs the actual game setup. After that, the engine loop updates systems, flushes destroyed entities, renders the frame, and swaps buffers. In other words, bootstrap code only sets up the stage; the actual gameplay structure comes from the ECS world and the registered systems.

A key implementation pattern in the repository is deferred behavior through shared state rather than direct calls. One system writes a component field, another system later reads that field, and the frame proceeds in order. For example, input is captured and stored in `InputComponent`, interaction detection writes the current target and mode into the same component, and interaction execution then consumes that state. This is a canonical ECS flow because it avoids one system directly invoking another.

The same pattern appears in the assistant and order flows. Dialogue and order progress are not managed through a single central controller. Instead, multiple systems cooperate by reading and writing shared component data. That keeps the control flow distributed but traceable.

## 3. Gameplay ECS Connections

The most useful way to understand the ECS design is to follow specific gameplay paths. These examples show how the project uses component state to move information between systems.

### 3.1 Input, interaction detection, and execution

The player interaction loop starts with `InputSystem`. It reads GLFW input and stores the results in `InputComponent`, including flags such as interact, drop, and dialogue choice selection. That component becomes the current input snapshot for the frame.

`InteractionDetectionSystem` reads the player’s state and nearby world entities, then determines what the player can interact with. It sets `selectedInteractableEntityId`, `canInteract`, and `interactionMode` in `InputComponent`. This is important because the selected target is not communicated through events or callbacks. It is written into shared state.

`InteractionExecutionSystem` then consumes that input state. If the selected entity is a product, the player picks it up. If it is an order box, the held item is delivered. If it is a mess, cleaning starts. The three systems are connected only through components, which is exactly the sort of loose coupling ECS is meant to provide.

### 3.2 Product pickup, inventory, and respawn

When a product is picked up, the system updates `ProductComponent` and `InventoryComponent`. The product is marked unavailable in the world, its holder entity is recorded, and the player’s inventory stores the entity ID.

If the product is configured to respawn on pickup, the spawn system creates a replacement product back at the original shelf or world position. That keeps the gameplay loop stable while still allowing the picked-up item to remain tracked in inventory and order logic. The respawn behavior shows one of the practical strengths of ECS: the same product entity can be reinterpreted in different states without changing the concept of the entity itself.

### 3.3 Mess generation and cleanup

Mess is generated when a product is picked up and the configured spawn chance succeeds. That is not a standalone ambient effect; it is a gameplay consequence of interaction. `MessFactory` decides whether a mess should be spawned based on the scenario rules, and `SpawnSystem` creates the mess entity with the appropriate components.

The mess entity includes `TransformComponent`, `RenderComponent`, `ColliderComponent`, `InteractableComponent`, and `MessComponent`. That combination means the mess is visible, interactable, and persistent enough for the ECS systems to treat it like a normal world entity. `MessSystem` then controls cleanup progress, expiration, and the movement slowdown effect when an entity is standing in a mess region.

This is a strong ECS example because a mess is not handled as a special-case effect in the player controller. It is an entity with state and behavior, and the behavior is distributed across systems.

### 3.4 Assistant AI and navigation

The assistant flow uses the same ECS style, but with different state. `AssistantAgentSystem` reads the dialogue choice selected by the player and uses that to assign the assistant a product target. It writes the result into `AssistantAgentComponent` and `NavigationAgentComponent`.

`NavigationPathSystem` then checks when the navigation state is dirty and computes a path. `NavigationMovementSystem` reads the path and advances the assistant entity over time. The important architectural point is that pathfinding and movement are separated. The assistant chooses what it wants to do, navigation computes how to do it, and movement carries it out.

This division makes the AI easier to extend. If a new kind of agent is added later, it can reuse the same navigation and movement systems while changing only the component data and selection logic.

### 3.5 Rendering and feedback

Rendering is also ECS-driven. `RenderSystem` reads `TransformComponent` and `RenderComponent` to draw visible world entities. It also reads `InputComponent`, `InventoryComponent`, `TaskComponent`, `OrderComponent`, and `DialogueChoiceComponent` to construct HUD text and interaction feedback.

This means the player’s UI is derived from live ECS state rather than from a separate hardcoded interface. The prompt text, the inventory display, the current order progress, and the dialogue choices are all views of the same underlying simulation data. That keeps the UI consistent with gameplay and makes the state easier to debug.

## 4. Performance Critical Paths

The current project scale is small enough that the ECS implementation is practical, but several parts of the architecture define the likely performance hotspots. These are the paths most worth discussing in a report because they shape how the engine scales.

### 4.1 Full-world scans

Many systems iterate over `world.getActiveEntityIds()` and then inspect the components attached to each entity. This is easy to read and easy to implement, but it creates repeated linear scans through the world. With a small number of entities, that is a good trade-off. With a larger number of entities, the cost becomes noticeable.

This pattern appears in multiple systems: interaction detection, rendering, mess updates, navigation checks, and HUD assembly. The same world list is scanned more than once per frame for different purposes. That is acceptable for the current coursework scope, but it is one of the first scaling bottlenecks.

### 4.2 Interaction detection

Interaction detection is one of the most gameplay-visible systems and one of the first places where the cost of scanning grows. It checks nearby interactable entities, evaluates their distances, and determines which object the player is closest to. If the scene contains many entities but only a few are actually interactable, the system still has to scan the whole candidate set.

That is fine for the supermarket map in this repository, but it would become more expensive in a larger scene or one with many more pickups and interactive objects.

### 4.3 Collision handling

Physics is another likely hot path. The collision system uses axis-aligned checks over entities that have colliders. In the worst case, this can approach an $O(n^2)$ pattern when many collidable entities are present. The current map is small enough that this is manageable, but it is an obvious future optimization target.

The reason physics is important in a report is that collision cost grows not only with the number of entities, but also with how densely the entities are packed. A supermarket with many shelves, products, agents, and obstacles is exactly the kind of setting where broad-phase filtering would pay off.

### 4.4 Navigation pathfinding

Navigation pathfinding is a bursty cost. `NavigationPathSystem` constructs blocked-cell information and computes paths when an agent’s navigation state becomes dirty. That is efficient enough for a modest number of agents, but if several agents request new paths in the same frame, the cost can spike.

This makes pathfinding a good benchmark candidate. It is not necessarily a constant cost every frame, but when it does run, it can be a meaningful source of frame-time variance.

### 4.5 Rendering and HUD construction

Rendering is not only a GPU cost in this codebase. It also includes repeated ECS queries for visible entities, HUD state, dialogue text, order progress, and interaction prompts. That means the render pass has CPU work as well as draw calls.

The visual side of the project is lightweight, but the text and HUD work can be more expensive than the simple mesh drawing itself. Rebuilding strings, searching the world for singleton-like state, and updating UI text every frame all contribute to the overall frame cost.

### 4.6 Text rendering

Text rendering is more expensive than static mesh drawing because it must generate glyph geometry and update GPU-facing data for dynamic content. In this project, that cost is still reasonable, but it becomes important if the HUD grows, if more prompts are added, or if a more detailed UI layer is introduced.

## 5. Benchmarks And Optimization

There are no formal benchmark numbers recorded in the repository, so a good report should discuss benchmark targets and expected hotspots rather than inventing measurements. The most defensible approach is to describe what should be measured, why it matters, and what optimization strategies fit the current architecture.

### 5.1 Useful benchmark targets

A strong benchmark section should include the following cases:

- frame time in the normal supermarket scene
- frame time during continuous movement and interaction
- pathfinding cost when several agents are active
- collision cost with many collidable entities on screen
- HUD and text rendering cost while prompts and order status are visible

These benchmarks are useful because they correspond directly to the parts of the ECS world that are most likely to scale poorly.

### 5.2 Practical optimization ideas

The most valuable optimizations for this codebase are the ones that preserve the current architecture while reducing repeated work.

1. Cache singleton-like entity lookups.
   Some state, such as order information or theme selection, behaves like singleton data even though it is stored as an entity component. Caching the relevant entity ID after the first lookup would avoid repeatedly scanning the world.

2. Reduce full-world scans.
   Multiple systems currently walk the same active entity list. A more advanced ECS query layer, or at least a few component-specific caches, would reduce repeated linear work.

3. Add spatial filtering.
   Interaction and collision both benefit from spatial locality. A broad-phase grid, bucket system, or spatial partition would reduce the number of pairwise comparisons.

4. Cache HUD text state.
   Some UI values are rebuilt every frame from world state. A dedicated UI cache or a small HUD state component would avoid repeated string construction and repeated world searches.

5. Keep render asset resolution stable.
   The renderer currently uses mesh and material handles in a simple, readable way. As the project grows, more aggressive caching of material and mesh lookups would reduce overhead.

6. Profile pathfinding separately.
   Pathfinding should be measured independently because its cost is bursty rather than constant. That gives a clearer picture of worst-case frame spikes.

### 5.3 Optimization priorities

If implementation time is limited, the most meaningful optimizations are likely to be:

- caching repeated singleton-style lookups
- reducing world scans in interaction detection and rendering
- adding spatial filtering for physics and interaction
- profiling navigation under multiple agents

These changes would improve performance without forcing a major redesign of the ECS architecture.

## 6. Why This ECS Design Works Here

This project does not need the most elaborate ECS possible. It needs an ECS that is simple enough to explain, stable enough to extend, and flexible enough to support gameplay, AI, rendering, and UI. The current design meets those goals.

The main strengths are separation of concerns, low coupling, and a clear data flow. The main trade-off is that repeated scans are not the most efficient possible approach. For this scale, that trade-off is acceptable. It makes the system easier to understand, which is valuable in a coursework project and in a report that has to be explained clearly.

The architecture also supports future work. A more advanced ECS query system, spatial partitioning, better UI layering, and asset-driven content loading can all be added without discarding the current structure. That is a good sign for the design because it means the current implementation is already pointing toward a scalable future instead of painting the project into a corner.

## 7. Concise Summary

The ECS implementation in LTU-PasirRis-VIE is built around a simple and readable model: entities are integer IDs, components store data, systems operate on shared state, and the scene decides how those systems are assembled. That design keeps engine code reusable and game code specific.

The main performance costs come from repeated world scans, interaction checks, physics, pathfinding, and HUD assembly. Those are the best candidates for benchmark work and optimization because they are the clearest sources of scaling cost. The best improvements are not architectural rewrites; they are targeted reductions in repeated work, better spatial filtering, and smarter caching of data that is effectively singleton-like.

Overall, the ECS design is a practical fit for the project. It is easy to reason about, it cleanly separates engine and game responsibilities, and it gives enough structure to support future expansion without sacrificing clarity.
