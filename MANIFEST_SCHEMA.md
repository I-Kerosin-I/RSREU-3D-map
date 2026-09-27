# Manifest Schema

## Purpose

All building, floor, room, and navigation data lives in `assets/manifest.json`, not in Java code. Parsed once at startup by `core/content/ManifestLoader` and converted to domain objects.

## Schema

```json
{
  "schemaVersion": 1,
  "coordinateSystem": { "units": "meters", "upAxis": "Y" },
  "buildings": [
    {
      "id": "main",
      "name": "Главный корпус",
      "model": "models/main-building.glb",
      "floors": [
        { "id": "1", "level": 1, "heightMeters": 0.0 }
      ],
      "spaces": [
        {
          "id": "room_101",
          "name": "Аудитория 101",
          "floor": "1",
          "type": "ROOM",
          "modelNode": "room_101",
          "bounds": { "min": [8.0, 0.0, 2.0], "max": [12.0, 3.0, 6.0] }
        }
      ],
      "navigation": {
        "nodes": [
          { "id": "nav_floor_1_01", "floor": "1", "position": [10.0, 0.0, 4.0] }
        ],
        "edges": [
          { "from": "nav_floor_1_01", "to": "nav_floor_1_02", "type": "CORRIDOR", "bidirectional": true }
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
| model | string | yes | Path relative to `assets/` (e.g. `"models/main-building.glb"`) |
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
| id | string | yes | Stable identifier, ASCII only (e.g. `"room_101"`) |
| name | string | yes | Display name (any language) |
| floor | string | yes | References floor `id` |
| type | string | yes | One of: ROOM, CORRIDOR, STAIRS, ELEVATOR, ENTRANCE, RESTROOM, OTHER |
| modelNode | string | yes | glTF node name — must match exactly |
| bounds | object | no | Axis-aligned bounding box, 3 floats per corner |

### space.bounds

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| min | array[3] | yes | [x, y, z] minimum corner |
| max | array[3] | yes | [x, y, z] maximum corner |

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

## Node Name Convention

glTF node names must be stable and ASCII-only:

```
room_<number>          → room_101, room_102
room_<type>_<number>   → room_lab_1, room_lecture_3
nav_<location>         → nav_floor_1_hall, nav_stairs_a
```

Displayed names (e.g. "Аудитория 101") are stored in `name` and can change. `id` and `modelNode` never change.

## Content Validation Rules

On startup, `ContentValidator` must reject manifest if:

- referenced GLB file does not exist in `assets/models/`
- `modelNode` does not exist as a node in the GLB
- `navigation.nodes[].floor` references a non-existent floor
- navigation edge references a non-existent node id
- duplicate `space.id` or `navigation.nodes[].id`
- unknown `space.type` or navigation `edge.type`

## Adding a New Building

1. Export GLB from Blender, place in `assets/models/<id>.glb`
2. Add entry to `manifest.json` `buildings` array
3. Ensure all `modelNode` values match glTF node names exactly
4. Rebuild and run — no Java code changes needed