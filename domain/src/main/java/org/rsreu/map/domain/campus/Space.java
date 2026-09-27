package org.rsreu.map.domain.campus;

import org.rsreu.map.domain.geometry.Bounds3;
import org.rsreu.map.domain.model.FloorId;
import org.rsreu.map.domain.model.RoomId;

/**
 * Represents any navigable space: room, corridor, stairwell, elevator hall, etc.
 */
public record Space(
    RoomId id,
    String displayName,
    FloorId floorId,
    SpaceType type,
    String modelNodeName,
    Bounds3 bounds
) {

    public Space {
        if (id == null) throw new IllegalArgumentException("Space id cannot be null");
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Space displayName cannot be null or blank");
        }
        if (floorId == null) throw new IllegalArgumentException("Space floorId cannot be null");
        if (type == null) throw new IllegalArgumentException("Space type cannot be null");
        if (modelNodeName == null || modelNodeName.isBlank()) {
            throw new IllegalArgumentException("Space modelNodeName cannot be null or blank");
        }
        if (bounds == null) throw new IllegalArgumentException("Space bounds cannot be null");
    }
}
