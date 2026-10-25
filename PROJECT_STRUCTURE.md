# Project Structure

## Directory Layout

```
RSREU_3D_Map/
├── ARCHITECTURE.md
├── DOMAIN_MODEL.md
├── PROJECT_STRUCTURE.md
├── MANIFEST_SCHEMA.md
├── DEVELOPMENT.md
├── pom.xml                          # Parent POM
│
├── domain/                          # Maven module
│   ├── pom.xml
│   └── src/main/java/org/rsreu/map/domain/
│       ├── campus/
│       │   ├── Campus.java
│       │   ├── Building.java
│       │   ├── Floor.java
│       │   ├── Space.java
│       │   └── SpaceType.java
│       ├── navigation/
│       │   ├── NavigationNode.java
│       │   ├── NavigationEdge.java
│       │   ├── NavigationGraph.java
│       │   └── Route.java
│       ├── geometry/
│       │   ├── Point3.java
│       │   └── Bounds3.java
│       └── model/
│           ├── BuildingId.java
│           ├── FloorId.java
│           └── RoomId.java
│
├── application/                    # Maven module
│   ├── pom.xml
│   └── src/main/java/org/rsreu/map/application/
│       ├── port/
│       │   └── CampusRepository.java
│       ├── usecase/
│       │   ├── FindRoomsUseCase.java
│       │   ├── SelectRoomUseCase.java
│       │   └── FindRouteUseCase.java
│       └── model/
│           ├── SearchQuery.java
│           └── RouteRequest.java
│
├── core/                            # Maven module
│   ├── pom.xml
│   └── src/main/java/org/rsreu/map/
│       ├── MapGame.java
│       ├── controller/
│       │   ├── MapController.java
│       │   └── MapUiState.java
│       ├── content/
│       │   ├── ManifestLoader.java
│       │   ├── ManifestParser.java
│       │   └── ContentValidator.java
│       ├── render/
│       │   ├── EdgeHighlighter.java      # colors line primitives with accent
│       │   ├── RoomMaterialManager.java  # floor dim/highlight/hide materials
│       │   ├── RoomHighlighter.java      # coordinates: picker + materials + state
│       │   ├── RouteRenderer.java
│       │   ├── SceneLoader.java
│       │   └── BuildingRenderer.java
│       ├── input/
│       │   ├── CameraInputController.java
│       │   └── RoomPicker.java           # ray → hitbox → Space
│       └── ui/
│           ├── MapScreen.java
│           ├── RoomSearchUi.java
│           └── FloorSelector.java
│
├── desktop/                         # Maven module
│   ├── pom.xml
│   └── src/main/java/org/rsreu/map/desktop/
│       └── DesktopLauncher.java
│
└── assets/                          # Not a Maven module
    ├── manifest.json                # Building metadata (names, departments, navigation)
    └── models/
        └── MainCampus.glb           # Floor-based hierarchy with *_hitbox nodes
```

## Maven Modules

Each module is a standard Maven project with `pom.xml`.

### Parent pom.xml

```xml
<modules>
    <module>domain</module>
    <module>application</module>
    <module>core</module>
    <module>desktop</module>
</modules>
```

Use `dependencyManagement` to pin versions of libGDX and gdx-gltf.

### module pom.xml inheritance

Each child module inherits from parent:

```xml
<parent>
    <groupId>org.rsreu</groupId>
    <artifactId>RSREU_3D_Map</artifactId>
    <version>1.0-SNAPSHOT</version>
</parent>
```

## assets/

Contains runtime data — glTF models and manifest.

- `manifest.json` — room metadata (names, departments, types). Hitbox geometry lives in the GLB, not here.
- `models/*.glb` — one GLB per building. Floor-based node hierarchy with `*_hitbox` nodes for ray picking.

Hitboxes are **not** in `manifest.json`. All ray-picking geometry is embedded in the GLB as invisible box meshes. The manifest provides domain metadata (display names, department assignments) only.

When porting to Android, files under `assets/` are placed in `android/app/src/main/assets/`.

## core vs domain

`core` depends on libGDX. `domain` does not.

```
core/          →  depends on  →  libGDX, gdx-gltf
domain/        →  depends on  →  (nothing external)
application/   →  depends on  →  domain
desktop/       →  depends on  →  core, LWJGL backend
```

## Key Classes

### RoomMaterialManager

Manages per-floor and per-hitbox material state:

```java
public class RoomMaterialManager {
    // Floor opacity states
    void setFloorDimmed(String floorNodeName, boolean dimmed);   // hover
    void setFloorHidden(String floorNodeName, boolean hidden);   // click
    void setFloorSelected(String floorNodeName, boolean selected);
    void setMainCampusDimmed(boolean dimmed);

    // Hitbox visibility
    void setHitboxHighlighted(String hitboxNodeName, boolean highlighted);

    // Reset
    void resetAll();
}
```

Uses `PBRColorAttribute.createBaseColorFactor` and `BlendingAttribute` on `NodePart.material` of floor nodes and hitbox nodes. Materials are private copies per `ModelInstance`.

### RoomPicker

Ray picking against `*_hitbox` nodes:

```java
public class RoomPicker {
    Space pick(float screenX, float screenY, PerspectiveCamera camera, ModelInstance instance);
    // Returns: null (miss) or Space whose modelNodeName = hitbox parent (floor node)
}
```

Uses `camera.getPickRay()`, `Intersector.intersectRayBounds()`, and `Node.calculateWorldTransform()` + `Node.calculateBoundingBox(box, true)`.

### RoomHighlighter

Coordinates the interaction loop:

```java
public class RoomHighlighter {
    void onMouseMoved(int screenX, int screenY);   // updates floor isolation
    void onClick(int screenX, int screenY);       // locks floor selection
    void clearSelection();                         // Escape / click empty space
}
```

Maintains `selectionLocked` flag. When locked, `onMouseMoved` only updates hitbox highlight, not floor isolation.

## Android Structure (future)

```
android/
├── settings.gradle
├── build.gradle
├── gradle/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── java/org/rsreu/map/android/
│       │   └── AndroidLauncher.java
│       ├── assets/          ← copy of assets/ from root
│       └── AndroidManifest.xml
```

The `MapGame` class from `core` is instantiated by Android launcher, just like desktop launcher.
All game logic (controllers, render, content loading, navigation) is shared.
Only platform-specific code differs: input handling, lifecycle, and launcher.
