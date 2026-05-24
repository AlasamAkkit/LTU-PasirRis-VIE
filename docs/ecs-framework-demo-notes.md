# ECS Framework Demo Notes

This file is for a short framework explanation, not a full report. The goal is to help explain the engine in about 8 minutes by focusing on the parts that matter most: the ECS model, the engine layer, the game layer, and the main systems and components that make the supermarket game work.

## 1. What the framework is

This project is built as an engine plus a game on top of it.

- The engine layer contains reusable runtime features such as the window, rendering, input handling, ECS storage, physics, and the main update loop.
- The game layer contains supermarket-specific content such as the scene, products, orders, assistant behavior, and scenario setup.

The important idea is that the engine does not hardcode supermarket rules. Instead, the game scene decides what entities exist and which systems are active.

## 2. Core ECS idea

The game uses an ECS-style architecture:

- Entity: just an integer ID managed by `EcsWorld`
- Component: a data holder attached to an entity
- System: code that reads and writes components to produce behavior

This is the main framework concept to explain.

### Why this is useful

- It keeps data separate from behavior
- It avoids deep inheritance trees
- It makes it easier to add new gameplay objects by combining components
- It lets different systems cooperate through shared state instead of direct calls

## 3. Important engine classes

### `EcsWorld`

`EcsWorld` is the heart of the ECS runtime.

What to say:
- It creates and stores entities
- It stores components by type
- It registers systems
- It updates systems each frame
- It handles entity destruction in a controlled way

Why it matters:
- It is the shared container for all gameplay state
- Most systems depend on it
- It defines how data flows through the game

### `Engine`

The engine is the main runtime loop.

What to say:
- It opens the window
- It loads the scene
- It updates systems every frame
- It calls render and swap-buffer style operations

Why it matters:
- It is the generic loop that keeps the game running
- It does not know supermarket rules by itself

### `Main`

`Main` is only the bootstrap entry point.

What to say:
- It starts the application
- It launches the game class

Why it matters:
- It shows where execution begins
- It is not where gameplay logic lives

### `SupermarketScene`

This is the main game setup file.

What to say:
- It decides which systems are registered
- It controls the execution order of systems
- It spawns the initial world from the scenario config

Why it matters:
- It is the bridge between the engine and the game
- It is where the supermarket-specific experience is assembled

## 4. Important entity types

These are the entities worth mentioning during the demo.

### Player entity

The player is an entity with movement, input, inventory, and collision state.

Typical components:
- `TransformComponent`
- `VelocityComponent`
- `InputComponent`
- `InventoryComponent`
- `ColliderComponent`
- `RenderComponent`

What to say:
- The player is not a special class
- It is just an entity with the right components
- Systems interpret those components to move the player and handle interaction

### Product entity

Products are pickup objects the player and assistant can interact with.

Typical components:
- `TransformComponent`
- `RenderComponent`
- `ColliderComponent`
- `ProductComponent`
- optional metadata such as respawn and color

What to say:
- Products are world entities, not inventory objects by default
- They can be picked up, carried, delivered, or respawned
- Their behavior comes from the product component and interaction systems

### Order box entity

The order box is the delivery target.

Typical components:
- `TransformComponent`
- `RenderComponent`
- `ColliderComponent`
- `OrderBoxComponent`
- `TaskComponent`

What to say:
- It is where delivered products are turned into order progress
- It is a gameplay anchor point for the delivery loop

### Mess entity

Mess is a temporary interactable object generated during product pickup.

Typical components:
- `TransformComponent`
- `RenderComponent`
- `ColliderComponent`
- `InteractableComponent`
- `MessComponent`

What to say:
- Mess is spawned dynamically
- It slows movement and can be cleaned
- It is a good example of a system-generated entity

### Assistant entity

The assistant is a behavior-driven NPC.

Typical components:
- `TransformComponent`
- `VelocityComponent`
- `RenderComponent`
- `InventoryComponent`
- dialogue and navigation related components

What to say:
- The assistant is driven by the same ECS pattern as the player
- The assistant receives orders through dialogue and then navigates to products
- It demonstrates how ECS can support AI-style behavior

### Shelf and environment entities

Shelves, room pieces, and decorations are static or semi-static world entities.

Typical components:
- `TransformComponent`
- `RenderComponent`
- `ColliderComponent` for blocking or interaction
- `ShelfComponent` or similar marker components

What to say:
- These entities define the playable environment
- They are mostly data-driven scene content
- They show that ECS is not only for characters

## 5. Important components

### `TransformComponent`

This is one of the most important components in the whole engine.

What to say:
- It stores position, rotation, and scale
- Almost every visible entity needs it
- Rendering, collision, movement, and interaction all depend on it

### `RenderComponent`

This tells the renderer how an entity should appear.

What to say:
- It stores mesh and material handles
- It can also store visibility state
- The renderer uses it to decide what to draw

### `ColliderComponent`

This defines collision and interaction size.

What to say:
- It supports physical blocking or trigger-style detection
- It helps with interaction radius and obstacle behavior
- It is essential for movement and object pickup logic

### `InputComponent`

This stores the player’s current control state.

What to say:
- It contains input flags such as interact and drop
- It stores which entity is currently targeted
- It is a bridge between raw input and gameplay systems

### `InventoryComponent`

This stores what the player or assistant is carrying.

What to say:
- It tracks held entity IDs
- It limits capacity
- It is the key state for pickup and delivery gameplay

### `ProductComponent`

This describes a product’s gameplay state.

What to say:
- It stores product type and availability
- It can record holder information and respawn data
- It may also carry visual customization such as color

### `InteractableComponent`

This marks an entity as something the player can act on.

What to say:
- It stores prompt text and interaction mode
- It is used for things like talking, cleaning, or using objects
- It is a simple marker that helps the prompt and execution systems

### `MessComponent`

This stores mess-specific state.

What to say:
- It tracks cleaning progress and duration
- It can reference the cleaner entity
- It also contributes to movement slowdown behavior

### `OrderBoxComponent`

This stores order box state.

What to say:
- It tracks whether the order box is complete
- It is used when products are delivered into the target area

### `TaskComponent`

This stores task progress.

What to say:
- It tracks the current task and progress percentage
- It supports objective display and completion logic

### `DialogueChoiceComponent`

This stores dialogue UI and selection data.

What to say:
- It is used when interacting with the assistant
- It holds dialogue options and the chosen response
- It drives the assistant assignment flow

### `NavigationAgentComponent` and related navigation data

These support assistant pathing.

What to say:
- They store destination, target entity, path state, and waypoints
- Pathfinding and movement systems read and update them
- This is how the assistant moves intelligently

## 6. Important systems

### `InputSystem`

What to say:
- Reads keyboard input from GLFW
- Writes the result into `InputComponent`
- Converts raw input into ECS state

### `InteractionDetectionSystem`

What to say:
- Looks for nearby targets
- Decides what the player can interact with
- Writes the selected target and interaction mode into `InputComponent`

### `InteractionExecutionSystem`

What to say:
- Reads the selected target
- Executes pickup, delivery, cleaning, or other actions
- Starts the important gameplay chain after the player presses interact

### `MovementSystem`

What to say:
- Moves the player based on velocity and input state
- Uses world state and slowdown effects if needed
- Shows how ECS controls motion through component data

### `PhysicsSystem`

What to say:
- Handles collision and blocking logic
- Prevents entities from passing through solid obstacles
- Is one of the core performance-sensitive systems

### `MessSystem`

What to say:
- Updates mess lifetime and cleaning progress
- Applies slowdown behavior
- Removes mess when cleaning is complete or when it expires

### `NavigationPathSystem`

What to say:
- Builds paths for moving agents
- Turns map state into navigable routes
- Is a key AI support system

### `NavigationMovementSystem`

What to say:
- Moves the assistant along the computed path
- Completes the movement side of the AI loop

### `AssistantAgentSystem`

What to say:
- Reads dialogue choice data
- Assigns product targets to the assistant
- Coordinates the assistant’s behavior loop

### `OrderTrackingSystem`

What to say:
- Tracks which products were delivered
- Updates order progress state
- Connects delivery actions to objective completion

### `OrderCompletionSystem`

What to say:
- Checks whether the current order has been satisfied
- Advances or resets the level flow
- Closes the delivery loop

### `OrderGenerationSystem` and `OrderTimerSystem`

What to say:
- Create and manage the timed order structure
- Drive the level pressure and progression
- Keep the game loop objective-driven

### `RenderSystem`

What to say:
- Draws the world entities
- Builds HUD text
- Displays prompts, inventory, objectives, and status
- Is both a visual system and a UI system in this project

### `ThemeSelectionSystem`

What to say:
- Handles the optional customization flow
- Demonstrates how ECS can support setup screens and player choices
- Shows that the engine is not just for gameplay, but also for menus and configuration

## 7. Suggested explanation flow for an 8-minute demo

If you only have a few minutes, use this order:

1. Start with the big picture: engine versus game layer.
2. Explain ECS in one sentence: entities are IDs, components are data, systems are behavior.
3. Show the player loop: input, detection, execution, movement.
4. Show one interaction example: pick up a product, maybe spawn mess, update inventory, deliver to order box.
5. Show the assistant example: dialogue choice, pathfinding, movement, product delivery.
6. Explain the renderer and HUD as a system that reads ECS state.
7. End with performance: repeated world scans, pathfinding, collision, and rendering are the main hotspots.

## 8. Key takeaways to emphasize

- The engine is generic; the supermarket game is built on top of it.
- Entities are lightweight IDs, not full objects.
- Components store the state.
- Systems create the behavior.
- The scene controls the setup and ordering of systems.
- The major performance cost comes from repeated scans over the active entities.
- The architecture is simple enough to explain clearly, but flexible enough to support more features later.

## 9. Short closing statement

A good one-sentence conclusion is:

"This project uses an ECS framework where the engine provides the reusable runtime and the supermarket scene supplies the game-specific content, while systems communicate through shared component data to handle input, interaction, AI, rendering, orders, and performance-sensitive updates."
