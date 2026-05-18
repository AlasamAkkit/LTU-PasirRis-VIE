# Path A: Scenario Authoring with `scenario.json`

The game now supports a data-driven authoring path through:

```text
assets/config/scenario.json
```

A framework user can edit the JSON file and run the game again without changing engine code.

## Configurable in the scenario file

- player spawn position and movement speed
- assistant spawn position and movement speed
- per-level order timers and timeout behavior
- shelf placement, IDs, and whether each shelf spawns on start
- product placement, shelf-relative offsets, type, respawn-on-pickup behavior, and whether each product spawns on start
- order-box placement and whether it spawns on start
- mess chance, mess size, slow amount, optional mess lifetime, and cleaning duration

## Still kept inside engine code

- render loop
- physics and collision resolution
- input handling
- ECS system update order
- order generation rules
- low-level asset and rendering setup

## Example edits

Move the player:

```json
"player": {
  "spawnOnStart": true,
  "position": { "x": 1.5, "y": 0.0, "z": 3.5 },
  "moveSpeed": 3.0
}
```

Disable an object without deleting it:

```json
{
  "spawnOnStart": false,
  "productType": "bread",
  "position": { "x": 0.0, "y": 0.52, "z": -2.25 },
  "respawnOnPickup": true
}
```

Make a product single-use:

```json
{
  "spawnOnStart": true,
  "productType": "milk",
  "position": { "x": -3.2, "y": 0.55, "z": -1.35 },
  "respawnOnPickup": false
}
```

Attach products to shelves so they follow shelf moves:

```json
{
  "spawnOnStart": true,
  "productType": "milk",
  "shelfId": "dairy-shelf",
  "offsetFromShelf": { "x": 0.0, "y": -0.35, "z": 0.65 },
  "respawnOnPickup": true
}
```

Configure order timers and what happens when time expires:

```json
"orderRules": {
  "levelTimeLimitsSeconds": [75.0, 60.0, 45.0],
  "failureBehavior": "restart-current-level"
}
```

Supported `failureBehavior` values:

- `restart-current-level`
- `restart-from-level-1`

If the player reaches a level beyond the configured timer list, the final timer value is reused.

Configure pickup messes:

```json
"messRules": {
  "enabled": true,
  "spawnChanceOnProductPickup": 0.2,
  "radius": 0.75,
  "speedMultiplier": 0.55,
  "durationSeconds": 0.0,
  "cleaningDurationSeconds": 2.5
}
```

`durationSeconds: 0.0` means the mess stays for the rest of the session. A positive value makes it disappear after that many seconds.
The player can clean a mess by standing over it, pressing `E`, and remaining nearby until `cleaningDurationSeconds` has elapsed.

## Current limitation

This Path A pass covers entities, properties, spawn rules, shelf-relative placement, order timers, and one simple gameplay rule. It does **not** yet support general trigger-event links such as:

```text
when player enters zone -> spawn entity
```

That would be the next extension if you want to cover the PDF's "when X happens, do Y" requirement too.
