# Domain Model

## Coordinate System

- Units: meters
- Up axis: Y
- All 3D positions stored as `Point3` (plain Java record)

All conversions to libGDX `Vector3` happen at the boundary of the `core` layer.

## Entities

### Campus

Top-level aggregate. Contains all buildings.

```java
public record Campus(List<Building> buildings) {}
```

### Building

Represents a single physical building.

```java
public record Building(
    BuildingId id,
    String name,
    List<Floor> floors,
    NavigationGraph navigation
) {}
```

### Floor

Represents a single floor within a building.

```java
public record Floor(
    FloorId id,
    BuildingId buildingId,
    int level,
    List<Space> spaces,
    float heightMeters
) {}
```

### Space

Any navigable area: room, corridor, stairwell, elevator hall.

```java
public record Space(
    RoomId id,
    String displayName,   // "Аудитория 101"
    FloorId floorId,
    SpaceType type,
    String modelNodeName, // glTF node name, stable identifier
    Bounds3 bounds
) {}
```

### SpaceType

```java
public enum SpaceType {
    ROOM,
    CORRIDOR,
    STAIRS,
    ELEVATOR,
    ENTRANCE,
    RESTROOM,
    OTHER
}
```

## Value Objects

### BuildingId, FloorId, RoomId

```java
public record BuildingId(String value) {}
public record FloorId(String value) {}     // e.g. "1", "2", "B1"
public record RoomId(String value) {}      // e.g. "room_101"
```

Rules:
- IDs are stable — they never change when the displayed name changes
- IDs use ASCII characters only
- Format convention: `room_<number>`, `nav_floor_<n>_hall`, `nav_stairs_<letter>`
- Node names in glTF must match `modelNodeName` in manifest exactly

### Point3, Bounds3

```java
public record Point3(float x, float y, float z) {}

public record Bounds3(Point3 min, Point3 max) {
    Point3 center() { ... }
    boolean contains(Point3 p) { ... }
}
```

## Navigation

### NavigationNode

A point in the navigation graph, usually placed in corridors and transitions, not necessarily inside rooms.

```java
public record NavigationNode(
    String id,
    FloorId floorId,
    Point3 position
) {}
```

### NavigationEdge

Connection between two nodes.

```java
public record NavigationEdge(
    String id,
    NavigationNode from,
    NavigationNode to,
    EdgeType type,
    boolean bidirectional
) {}

public enum EdgeType {
    DOOR,        // through a door
    CORRIDOR,    // walking along a corridor
    STAIRS,      // stairs between floors
    ELEVATOR     // elevator between floors
}
```

### NavigationGraph

```java
public record NavigationGraph(
    List<NavigationNode> nodes,
    List<NavigationEdge> edges
) {
    List<NavigationNode> adjacent(NavigationNode node) { ... }
}
```

### Route

Result of pathfinding. Contains ordered list of nodes and total distance.

```java
public record Route(
    List<NavigationNode> nodes,
    float totalDistanceMeters
) {}
```

## Repository Interface

Defined in `application` layer, implemented in `core`:

```java
public interface CampusRepository {
    Campus getCampus();

    Optional<Space> findSpaceByNodeName(String modelNodeName);

    Optional<Building> findBuilding(BuildingId id);

    Optional<Floor> findFloor(FloorId id);

    Optional<Space> findSpace(RoomId id);
}
```

## Restrictions

Domain layer must not contain:
- libGDX imports (`com.badlogic.gdx.*`)
- Android imports (`android.*`)
- LWJGL imports (`org.lwjgl.*`)
- Swing/AWT imports (`java.awt.*`, `javax.swing.*`)
- File I/O
- Logging frameworks

Use cases in `application` follow the same restrictions.