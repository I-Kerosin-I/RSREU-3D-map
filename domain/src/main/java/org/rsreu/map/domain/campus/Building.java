package org.rsreu.map.domain.campus;

import org.rsreu.map.domain.model.BuildingId;
import org.rsreu.map.domain.model.FloorId;

import java.util.List;
import java.util.Optional;

/**
 * Represents a single physical building.
 */
public record Building(
    BuildingId id,
    String name,
    List<Floor> floors
) {

    public Building {
        if (id == null) throw new IllegalArgumentException("Building id cannot be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Building name cannot be null or blank");
        }
        if (floors == null) throw new IllegalArgumentException("Building floors cannot be null");
    }

    /**
     * Finds a floor by its ID.
     */
    public Optional<Floor> findFloor(FloorId floorId) {
        return floors.stream()
            .filter(floor -> floor.id().equals(floorId))
            .findFirst();
    }
}
