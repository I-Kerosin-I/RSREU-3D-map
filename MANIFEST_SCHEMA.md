# Manifest Schema

## Purpose

Room metadata (names, departments, types) lives in `assets/manifest.json`, not in Java code. Parsed once at startup by `core/content/ManifestLoader` and converted to domain objects.

**Hitbox geometry is NOT in manifest.json** — it lives in the GLB as invisible `*_hitbox` box meshes. The manifest provides domain metadata only.

## Schema

```json
{
  "schemaVersion": 1,
  "coordinateSystem": { "units": "meters", "upAxis": "Y" },
  "buildings": [
    {
      "id": "main",
      "name": "Главный корпус",
      "model": "models/MainCampus.glb",
      "floors": [
        { "id": "2", "level": 2, "heightMeters": 0.0 }
      ],
      "spaces": [
        {
          "id": "main_room_210",
          "name": "210 кафедра ЭВМ",
          "type": "ROOM",
          "modelNode": "main_room_210"
        }
      ],
      "navigation": {
        "nodes": [
          { "id": "nav_floor_2_01", "floor": "2", "position": [10.0, 0.0, 4.0] }
        ],
        "edges": [
          { "from": "nav_floor_2_01", "to": "nav_floor_2_02", "type": "CORRIDOR", "bidirectional": true }
        ]
      }
    }
  ]
}
```

## Field Reference

### Top level fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| schemaVersion | integer | yes | Currently `1`. Increment on breaking changes. |
| coordinateSystem | object | yes | Units and axis |
| buildings | array | yes | List of buildings |

### coordinateSystem

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| units | string | yes | Always `"meters"` |
| upAxis | string | yes | Always `"Y"` |

### building

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string | yes | Unique ASCII identifier (`"main"`, `"sport"`) |
| name | string | yes | Display name, any language |
| model | string | yes | Path relative to `assets/` (e.g. `"models/MainCampus.glb"`) |
| floors | array | yes | At least one floor |
| spaces | array | yes | Rooms and other spaces |
| navigation | object | yes | Navigation graph |

### floor

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string | yes | Unique within building (`"1"`, `"2"`, `"B1"`) |
| level | integer | yes | Ordinal for ordering (1 = ground floor) |
| heightMeters | float | yes | Y coordinate of the floor plane |

### space

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string | yes | Stable identifier, ASCII only (e.g. `"main_room_210"`) |
| name | string | yes | Display name (any language) |
| floor | string | yes | References floor `id` |
| type | string | yes | One of: ROOM, CORRIDOR, STAIRS, ELEVATOR, ENTRANCE, RESTROOM, OTHER |
| modelNode | string | yes | Hitbox node name in GLB — must match the `*_hitbox` node parent for ray picking |

### navigation

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| nodes | array | yes | Navigation graph vertices |
| edges | array | yes | Navigation graph edges |

### navigation.nodes

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string | yes | Unique node identifier |
| floor | string | yes | References floor `id` |
| position | array[3] | yes | [x, y, z] in meters |

### navigation.edges

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| from | string | yes | Source node `id` |
| to | string | yes | Target node `id` |
| type | string | yes | One of: DOOR, CORRIDOR, STAIRS, ELEVATOR |
| bidirectional | boolean | yes | Whether traversal works both ways |

## Node Name Conventions

### glTF node hierarchy (in GLB)

```
Scene
 └── Main
      ├── floor1, floor2, ...        ← floor containers
      │    ├── Building_Edges.*       ← wireframe lines (NOT for manifest)
      │    ├── floorN               ← floor mesh
      │    ├── floorN_walls          ← walls mesh
      │    └── dept_roomNumber_hitbox ← INVISIBLE ray-pick cube
      ├── Building_Edges.main_campus ← outer shell edges (NOT for manifest)
      └── main_campus                 ← outer shell mesh
```

- Floor node: `floor1`, `floor2`, ... — contains all geometry for that floor
- Hitbox node: `*_hitbox` (e.g., `main_room_210_hitbox`) — invisible cube, child of floor node. Ray picking tests against this.
- Edge nodes: `Building_Edges.*` — line primitives, colored by `EdgeHighlighter`

### Space.modelNode

References the **hitbox node name** (not the floor node). E.g., `modelNode: "main_room_210"` — this is the `*_hitbox` node inside `floor2`.

Room ID format (from hitbox name, strip `_hitbox`): `department_roomNumber`
- department: `"main"`, `"evm"`, etc.
- roomNumber: `"210"`, `"201a"`, etc.
- Separator: `_`

## Content Validation Rules

On startup, `ContentValidator` must reject manifest if:

- referenced GLB file does not exist in `assets/models/`
- `space.modelNode` does not have a corresponding `*_hitbox` node in the GLB
- `navigation.nodes[].floor` references a non-existent floor
- navigation edge references a non-existent node id
- duplicate `space.id` or `navigation.nodes[].id`
- unknown `space.type` or navigation `edge.type`

## Adding a New Building

1. Export GLB from Blender, place in `assets/models/<id>.glb`
2. Add entry to `manifest.json` `buildings` array
3. Ensure all `space.modelNode` values match `*_hitbox` node names in the GLB exactly
4. Rebuild and run — no Java code changes needed
