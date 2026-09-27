package org.rsreu.map.domain.campus;

import org.rsreu.map.domain.model.BuildingId;

import java.util.List;
import java.util.Optional;

/**
 * Top-level aggregate containing all buildings.
 */
public record Campus(List<Building> buildings) {

    public Campus {
        if (buildings == null) throw new IllegalArgumentException("Campus buildings cannot be null");
    }

    /**
     * Finds a building by its ID.
     */
    public Optional<Building> findBuilding(BuildingId buildingId) {
        return buildings.stream()
            .filter(building -> building.id().equals(buildingId))
            .findFirst();
    }

    /**
     * Finds all spaces across all buildings.
     */
    public List<Space> getAllSpaces() {
        return buildings.stream()
            .flatMap(building -> building.floors().stream())
            .flatMap(floor -> floor.spaces().stream())
            .toList();
    }
}
