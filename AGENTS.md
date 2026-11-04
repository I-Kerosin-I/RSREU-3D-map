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

The model has a **floor-based hierarchy** nested below wrapper nodes (`Scene Collection` → `Main`), so floor containers must be discovered recursively:

```
Scene Collection
 └── Main
      ├── main                          ← outer building shell (mesh)
      ├── main_Edges                    ← shell wireframe overlay (lines)
      ├── floor1
      │    └── floor1                   ← walls + floor slab, one mesh
      ├── floor2
      │    ├── Building_Edges.floor2_walls
      │    ├── floor2                   ← floor slab
      │    ├── floor2_walls             ← solid walls for the floor
      │    └── main_room_210_hitbox     ← hitbox
      ├── floor3
      │    ├── floor3                   ← walls + floor slab, one mesh
      │    └── main_room_3XX_hitbox     ← ~70 hitboxes
      └── floor4
           └── floor4
```

- Floor node names: `floor1`, `floor2`, ... — container nodes own all geometry of that floor (walls + slab may be a single mesh named like the container)
- Hitbox node names: `*_hitbox` suffix (e.g., `main_room_210_hitbox`) — cubes for ray picking, children of the floor node. Their GLB materials are **opaque**; the code keeps them out of rendering via `NodePart.enabled = false` except the highlighted one
- Edge node names: `Building_Edges.*` / `*_Edges` — line meshes for accent wireframe overlay
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
2. `RoomPicker` collected all `*_hitbox` nodes recursively at construction; each one's world-space AABB was computed once from the mesh data and `node.globalTransform` (`Mesh.extendBoundingBox` with the mesh part range — **not** `Node.calculateBoundingBox`, which skips parts with `enabled = false`, and the hitbox parts are disabled for rendering)
3. `Intersector.intersectRayBounds(ray, cachedBox, hit)` → closest hit
4. Hitbox parent = floor node → floor ID
5. Strip `_hitbox` suffix from hitbox name → room ID
6. While a floor is locked, hitboxes of other floors are ignored (no highlight, no clicks)

### Material Management

Units are classified recursively: each `floorN` container node (wherever it sits in the tree) becomes a floor unit owning its subtree except hitboxes; all remaining geometry (shell, its wireframe) forms one shell unit; every hitbox is its own unit.

| State | Blending | Color | Depth Write |
|-------|----------|-------|-------------|
| Normal | none | original | yes |
| Dimmed (hover, other floor) | SRC_ALPHA / ONE_MINUS_SRC_ALPHA, α=0.15 | darkened ×0.3 | no (`DepthTestAttribute` mask follows opacity) |
| Hidden (click, other floor) | α=0, parts removed from render after the fade | — | no |
| Hitbox highlighted | SRC_ALPHA / ONE_MINUS_SRC_ALPHA, α=0.6 | accent color | no |
| Hitbox normal | not rendered (`NodePart.enabled = false`) | — | — |

Body fills write depth only when fully opaque. Wireframes depth-test but never write depth. Shell color never writes depth: its nearest surface is recorded in a separate depth-only pass **after** the body color pass. Fully invisible units (hidden floors, non-highlighted hitboxes) are removed from rendering via `NodePart.enabled = false` once their fade-out settles.

The global filled-polygon offset uses `factor = 0`, `units = 2`: the constant unit bias breaks the coplanar fill/line tie without the slope-dependent bias that can push large near-edge-on walls behind interior geometry.

### Transparency and shell self-occlusion

Whole-mesh sorting cannot reject hidden triangles and edges inside the double-sided shell mesh. Turning off shell depth writes during a fade loses that self-occlusion. `BuildingSceneManager.renderColors()` therefore always uses these passes on the existing MSAA framebuffer:

1. body fills and wireframes, sorted by `BuildingRenderableSorter`;
2. shell filled geometry only, color writes disabled, `GL_LEQUAL`, depth writes enabled regardless of opacity;
3. shell filled geometry with animated color/alpha, `GL_EQUAL`, no depth writes;
4. shell wireframe, `GL_LEQUAL`, no depth writes;
5. highlighted hitboxes, `GL_ALWAYS`, no depth writes.

The shell path is the same at alpha 1 and during a fade. The body is rendered **before** shell depth, so its interior color remains available for blending. Depth is not cleared between passes: opaque body geometry nearer than the shell must still occlude it, including from inside the building. Hidden/zero-alpha shell parts do not write depth.

`BuildingRenderPasses` stores stable `ModelInstance` / `Node` / `NodePart` bindings during the material manager's classification. It submits each enabled part only to its group; source-material replacement does not change membership. Cached pass-local shell materials preserve the source PBR features, update animated color/opacity, and differ only in their depth state and depth-pass opacity. Both passes use the same PBR vertex path and polygon offset for reliable `GL_EQUAL`; the standard packed-depth shader has a different vertex computation and is not used here.

The sorter's legacy material maps remain `IdentityHashMap`s: `Material.equals` and `hashCode` depend on mutable attributes. They order renderables but are **not** a substitute for per-fragment shell self-occlusion.

Animations retain exponential alpha/dim/highlight interpolation and the small settling epsilon. Normal source materials are restored immediately when settled; there is no extra `restorePending` frame or early-opacity threshold. Pass membership and shell depth/color policy remain unchanged at restoration. The renderer treats the shell as its nearest visible surface, not physically layered glass; exact coplanar duplicate shell triangles and translucent body geometry in front of the shell require separate treatment if encountered. Visual acceptance is required in addition to compilation and CPU regression checks.

`BlendingAttribute` + `PBRColorAttribute.createBaseColorFactor` are set per-node via mutable `NodePart` materials. Materials are private copies per `ModelInstance`, so changes don't leak.

## Camera Control

`CameraInputController` is an orbit camera (LMB pan, RMB orbit, wheel zoom, WASD move). All motion is slightly smoothed:

- Released drags keep orbiting / panning with an exponentially decaying velocity (release after ~0.1 s of pointer idle does not glide; grabbing stops the glide).
- The wheel sets a zoom target; the orbit distance approaches it smoothly.
- WASD velocity accelerates and brakes softly.
- Frame time used for camera motion is clamped (hitch guard).

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
