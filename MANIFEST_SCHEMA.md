# Manifest Schema

## Purpose

Room metadata (names, descriptions, categories, types) lives in `assets/manifest.json`, not in Java code. Parsed once at startup by `core/content/ManifestLoader` and `ManifestParser`, then served to the UI through `InMemoryCampusRepository` / `MapController`.

**Hitbox geometry is NOT in manifest.json** — it lives in the GLB as invisible `*_hitbox` box meshes. The manifest provides domain metadata only.

## Schema

```json
{
  "schemaVersion": 1,
  "coordinateSystem": { "units": "meters", "upAxis": "Y" },
  "categories": [
    {
      "id": "departments",
      "name": "Кафедры",
      "subcategories": [
        { "id": "evm", "name": "ЭВМ" }
      ]
    },
    { "id": "wc", "name": "WC", "subcategories": [
        { "id": "m", "name": "М" },
        { "id": "f", "name": "Ж" }
      ]
    },
    { "id": "canteen", "name": "Столовые" }
  ],
  "buildings": [
    {
      "id": "main",
      "name": "Главный корпус",
      "model": "models/MainCampus.glb",
      "floors": [
        { "id": "2", "level": 2, "heightMeters": 0.0 },
        {
          "id": "3",
          "level": 3,
          "heightMeters": 0.0,
          "spaces": [
            {
              "id": "main_room_313",
              "name": "а.313",
              "description": "Поточная лекционная аудитория-амфитеатр",
              "type": "ROOM",
              "category": "amphitheater",
              "modelNode": "main_room_313_hitbox"
            }
          ]
        }
      ],
      "navigation": {
        "nodes": [],
        "edges": []
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
| categories | array | no | Display taxonomy for the object browser panel |
| buildings | array | yes | List of buildings |

### coordinateSystem

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| units | string | yes | Always `"meters"` |
| upAxis | string | yes | Always `"Y"` |

### categories (taxonomy)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string | yes | Stable ASCII identifier (`"departments"`, `"wc"`, `"coffee"`) |
| name | string | yes | Display name shown in the tree |
| subcategories | array | no | Optional list of `{ "id", "name" }` |

Spaces reference a category by `id` (and a subcategory by `id`). The category tree, including which categories exist, is fully data-driven: adding one is a JSON edit, not a code change.

### building

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string | yes | Unique ASCII identifier (`"main"`, `"sport"`) |
| name | string | yes | Display name, any language |
| model | string | yes | Path relative to `assets/` (e.g. `"models/MainCampus.glb"`) |
| floors | array | yes | At least one floor |
| navigation | object | yes | Navigation graph (may be empty for now) |

### floor

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string | yes | Unique within building (`"1"`, `"2"`, `"B1"`); GLB floor container is `"floor" + id` |
| level | integer | yes | Ordinal for ordering (1 = ground floor) |
| heightMeters | float | yes | Y coordinate of the floor plane |
| spaces | array | no | Rooms and other spaces located on this floor |

### space

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string | yes | Stable identifier, ASCII only (e.g. `"main_room_210"`) — hitbox name minus `_hitbox` |
| name | string | yes | Display name (e.g. `"а.210"`, `"WC (Ж, лев.)"`, `"Буфет"`) |
| description | string | no | Shown in the hover tooltip and searched by the search bar |
| type | string | yes | One of: ROOM, CORRIDOR, STAIRS, ELEVATOR, ENTRANCE, RESTROOM, OTHER |
| category | string | no | Category id from `categories` — puts the space into the browser tree |
| subcategory | string | no | Subcategory id within the category |
| modelNode | string | yes | **Full hitbox node name in the GLB** including the `_hitbox` suffix (e.g. `"main_room_210_hitbox"`) — the exact join key for ray picking |

Floor membership is expressed by nesting the space in the floor's `spaces` array; there is no separate `floor` field.

## Node Name Conventions

### glTF node hierarchy (in GLB)

```
Scene Collection
 └── Main
      ├── main, main_Edges          ← outer shell mesh + wireframe (NOT for manifest)
      ├── floor1, floor2, ...       ← floor containers
      │    ├── floorN, floorN_walls ← floor mesh + walls
      │    └── main_room_XXX_hitbox ← INVISIBLE ray-pick cube
      └── ...
```

- Floor node: `floor1`, `floor2`, ... — contains all geometry for that floor; its name is `"floor" + <floor id>`
- Hitbox node: `*_hitbox` (e.g. `main_room_210_hitbox`) — invisible cube, child of the floor node. Ray picking tests against this; `space.modelNode` must name it exactly.

### Room ID format

`space.id` = hitbox node name minus `_hitbox`: `department_roomNumber`
- department: `"main"`, `"evm"`, etc.
- roomNumber: `"210"`, `"201a"`, etc.
- Separator: `_`

## Content Validation Rules

`ContentValidator` (to be wired) should reject the manifest if:

- referenced GLB file does not exist in `assets/models/`
- `space.modelNode` does not exactly match a `*_hitbox` node in the GLB
- `space.category` / `subcategory` do not reference ids from `categories`
- duplicate `space.id` or navigation node id

## Adding New Objects

1. Ensure the GLB has a `*_hitbox` cube for the object, child of its floor node.
2. Add a space entry to the floor's `spaces` array with `modelNode` = the full hitbox node name.
3. To expose it in the browser tree, reference a `categories` id (declare a new category in the same edit if needed).
4. Rebuild and run — no Java code changes needed.
