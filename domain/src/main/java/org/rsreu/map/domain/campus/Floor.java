package org.rsreu.map.domain.campus;

import org.rsreu.map.domain.model.BuildingId;
import org.rsreu.map.domain.model.FloorId;
import org.rsreu.map.domain.model.RoomId;

import java.util.List;
import java.util.Optional;

/**
 * Represents a single floor within a building.
 */
public record Floor(
    FloorId id,
    BuildingId buildingId,
    int level,
    float heightMeters,
    List<Space> spaces
) {

    public Floor {
        if (id == null) throw new IllegalArgumentException("Floor id cannot be null");
        if (buildingId == null) throw new IllegalArgumentException("Floor buildingId cannot be null");
        if (spaces == null) throw new IllegalArgumentException("Floor spaces cannot be null");
    }

    /**
     * Finds a space by its model node name.
     */
    public Optional<Space> findSpaceByNodeName(String modelNodeName) {
        return spaces.stream()
            .filter(space -> space.modelNodeName().equals(modelNodeName))
            .findFirst();
    }

    /**
     * Finds a space by its ID.
     */
    public Optional<Space> findSpace(RoomId roomId) {
        return spaces.stream()
            .filter(space -> space.id().equals(roomId))
            .findFirst();
    }
}
