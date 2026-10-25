# University 3D Map

## Project

Interactive 3D navigation map of RSREU buildings.

## Stack

- Java
- libGDX
- Maven
- gdx-gltf

## Architecture

- `core/` contains platform-independent code.
- `desktop/` contains desktop launcher code.
- Rendering must not contain university domain logic.
- University data must not depend on libGDX classes.

## Build

- Use Maven to build the project.
- Use `cd "D:\Projects\RSREU 3D Map"; mvn compile -q 2>&1 | Select-String -Pattern "ERROR|BUILD|SUCCESS"` to compile the project by default.

## 3D Assets

- Final runtime format: glTF 2.0 / GLB.
- Models are authored in Fusion 360 and prepared in Blender.
- Do not embed room metadata directly into Java rendering code.
- Models don't have textures, only a mesh.

### glTF Node Naming Convention

The model has a **floor-based hierarchy**:

```
Scene
 └── Main
      ├── Building_Edges.main_campus   ← wireframe overlay
      ├── floor1
      ├── floor2
      │    ├── Building_Edges.floor2_walls
      │    ├── floor2                   ← floor mesh
      │    ├── main_room_210_hitbox     ← INVISIBLE hitbox
      │    └── floor2_walls             ← solid walls for the floor
      ├── floor3
      ├── floor4
      └── main_campus                   ← outer building shell
```

- Floor node names: `floor1`, `floor2`, ... — contain all geometry for that floor
- Hitbox node names: `*_hitbox` suffix (e.g., `main_room_210_hitbox`) — invisible cubes for ray picking, child of floor node
- Edge node names: `Building_Edges.*` — line meshes for accent wireframe overlay
- Room ID format: `department_roomNumber` (e.g., `main_room_210`, `evm_room_201a`)

## Floor Isolation and Room Interaction

### States

**Normal (no hover):** All floors fully opaque.

**Hover:**
- `hovered floor` = opaque
- `all other floors` = semi-transparent (alpha ≈ 0.15, darkened)
- `main_campus` outer shell = semi-transparent
- Hitboxes invisible

**Click (floor selected):**
- `selected floor` = fully opaque
- `all other floors` = 100% transparent (removed from render)
- `main_campus` = 100% transparent
- Hitbox of hovered room = visible with accent color (semi-transparent)
- Cursor position no longer controls floor isolation
- `Escape` or click on empty space → returns to Normal state

**Hover after selection:**
- Hitbox of room under cursor = accent color + semi-transparent
- Other rooms on selected floor = unchanged (still visible)
- Other floors stay hidden

### Ray Picking

1. `camera.getPickRay(screenX, screenY)` → libGDX `Ray`
2. Iterate all nodes in `ModelInstance`
3. Filter: `node.name.endsWith("_hitbox")`
4. `node.calculateWorldTransform(); node.calculateBoundingBox(box, true)`
5. `Intersector.intersectRayBounds(ray, box, hit)` → closest hit
6. Hitbox parent = floor node → floor ID
7. Strip `_hitbox` suffix from hitbox name → room ID

### Material Management

Each floor node (walls, floor, hitboxes inside) gets material modifications:

| State | Blending | Color | Depth Write |
|-------|----------|-------|-------------|
| Normal | none | original | yes |
| Dimmed (hover, other floor) | SRC_ALPHA / ONE_MINUS_SRC_ALPHA, α=0.15 | darkened ×0.3 | no |
| Hidden (click, other floor) | α=0 → effectively invisible | — | no |
| Hitbox highlighted | SRC_ALPHA / ONE_MINUS_SRC_ALPHA, α=0.6 | accent color | no |

`BlendingAttribute` + `PBRColorAttribute.createBaseColorFactor` are set per-node via mutable `NodePart` materials. Materials are private copies per `ModelInstance`, so changes don't leak.

## Development

Before modifying architecture:
1. Inspect existing implementation.
2. Explain the proposed change.
3. Implement the smallest coherent change.
4. Run the relevant build/tests.
5. Report what changed and what was verified.

## Important

Do not introduce dependencies without checking whether
the existing stack already provides the required functionality.
