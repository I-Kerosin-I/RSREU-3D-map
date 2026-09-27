# Architecture

## Overview

Application for interactive 3D navigation map of RSREU buildings.

Technology stack: Java 17+, libGDX 1.12+, gdx-gltf, Maven.

## Principles

1. **Rendering must not contain university domain logic.**
   The renderer receives pre-built `RenderBuilding`, `RenderRoom`, and `RenderRoute` objects. It does not decide what a room is or what route is valid.

2. **University data must not depend on libGDX classes.**
   Domain model uses only plain Java. No `com.badlogic.gdx.*` imports in domain or application layers.

3. **Layers flow in one direction:**
   ```
   desktop / android
         ↓
        core
         ↓
    application
         ↓
       domain
   ```

4. **Architecture changes must follow the development process in DEVELOPMENT.md.**

## Layers

### domain

Plain Java. Contains:

- Building, Floor, Space, SpaceType
- NavigationNode, NavigationEdge, NavigationGraph, Route
- Point3, Bounds3, BuildingId, RoomId, FloorId

Rules:
- No imports from `com.badlogic.gdx.*`, `org.lwjgl.*`, `android.*`, `java.awt.*`
- Coordinates in meters, Y up axis
- Stable IDs independent of displayed names

### application

Plain Java. Contains:

- Use cases: FindRoomsUseCase, SelectRoomUseCase, FindRouteUseCase
- Ports: CampusRepository (interface)
- Request/response models

Rules:
- Depends only on domain
- No rendering, no platform input, no file I/O
- Implements routing algorithm (A* on local graph; no external graph library)

### core

libGDX application code. Contains:

- `MapGame` — main application listener
- `MapController` — orchestrates input, use cases, rendering, UI state
- `content/` — manifest loading, parsing, validation
- `render/` — building/room/route rendering, scene loading
- `input/` — camera, selection input controllers
- `ui/` — search UI, floor selector, screen layout

Rules:
- Receives domain objects, builds render objects, passes to renderer
- Renderer does not know about room names, building semantics, or route validity
- Manifest parsed once at startup; domain objects created before rendering starts

### desktop

LWJGL3 launcher only. Creates `Lwjgl3Application` with `MapGame`.

### android (future)

Standard Android application. Creates `AndroidApplication` with the same `MapGame`.

- Shares all code from domain, application, core
- Different input handling (touch vs mouse)
- Android lifecycle management
- Assets packaged in APK instead of filesystem

## Control Flow

```
User Input (click / touch)
    ↓
InputController
    ↓
MapController.resolveSelection(modelNodeName)
    ↓
CampusRepository.findRoomByNodeName(modelNodeName)  → domain.RoomId
    ↓
SelectRoomUseCase.execute(roomId)
    ↓
MapController.updateSelectedRoom(roomId)
    ↓
RoomHighlighter.buildHighlight(room)  → RenderRoom
    ↓
Renderer draws highlight
```

```
User Input (search query)
    ↓
InputController
    ↓
MapController.search(query)
    ↓
FindRoomsUseCase.execute(query)
    ↓
MapController.updateSearchResults(results)
    ↓
SearchResultsUi displays
```

```
User Input (click on nav target)
    ↓
MapController.requestRoute(fromRoomId, toRoomId)
    ↓
FindRouteUseCase.execute(from, to)
    ↓
NavigationGraph.findPath(from, to)  → Route
    ↓
MapController.updateRoute(route)
    ↓
RouteRenderer.build(route)  → RenderRoute
    ↓
Renderer draws path
```

## Modules and Dependencies

| Module | Depends On | Contains |
|--------|------------|----------|
| domain | JDK only | Entities, value objects, IDs |
| application | domain | Use cases, ports (interfaces) |
| core | application, domain, libGDX, gdx-gltf | MapGame, controllers, render, content |
| desktop | core, lwjgl3 | DesktopLauncher |
| android | core, android backend | AndroidLauncher, MainActivity |

## Java Version

Java 17. Not Java 24. Java 17 is required for:
- Compatibility with Android toolchain (AGP, D8)
- Stable records, sealed classes, Pattern Matching
- Availability across all target platforms

Update root pom.xml if Java version differs:
```xml
<maven.compiler.release>17</maven.compiler.release>
```