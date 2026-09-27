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
│       ├── model/
│       │   ├── BuildingId.java
│       │   ├── FloorId.java
│       │   └── RoomId.java
│       └── rsreu/
│           └── map/
│               └── domain/
│                   └── Manifest.java
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
│       │   ├── BuildingRenderer.java
│       │   ├── RoomHighlighter.java
│       │   ├── RouteRenderer.java
│       │   ├── SceneLoader.java
│       │   └── model/
│       │       ├── RenderBuilding.java
│       │       ├── RenderRoom.java
│       │       └── RenderRoute.java
│       ├── input/
│       │   ├── InputController.java
│       │   ├── CameraInputController.java
│       │   └── SelectionInputController.java
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
    ├── manifest.json
    └── models/
        ├── main-building.glb
        └── ... .glb
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

Contains runtime data — glTF models and manifest. Not a Java module.

- `manifest.json` — building catalog (floors, rooms, navigation graph)
- `models/*.glb` — one GLB per building, no textures, mesh only

Loaded at startup by `core/content/ManifestLoader`.

When porting to Android, files under `assets/` are placed in `android/app/src/main/assets/`.

## core vs domain

`core` depends on libGDX. `domain` does not.

```
core/          →  depends on  →  libGDX, gdx-gltf
domain/        →  depends on  →  (nothing external)
application/   →  depends on  →  domain
desktop/       →  depends on  →  core, LWJGL backend
```

## Render Model (plain objects for renderer)

These exist in `core/render/model/`. They are not domain entities.

```java
// RenderRoom: data the renderer needs, not domain logic
public record RenderRoom(
    RoomId id,
    String displayName,
    Color highlightColor,
    boolean pulsate,
    List<Vector3> outlineVertices
) {}
```

Conversion happens in `RoomHighlighter` (or similar):
```
domain.Space  →  RenderRoom  →  BuildingRenderer.drawRoom(renderRoom)
```

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