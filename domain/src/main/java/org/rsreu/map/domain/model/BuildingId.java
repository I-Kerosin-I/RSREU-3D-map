package org.rsreu.map.domain.model;

/**
 * Unique identifier for a building.
 */
public record BuildingId(String value) {

    public BuildingId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("BuildingId value cannot be null or blank");
        }
    }
}
