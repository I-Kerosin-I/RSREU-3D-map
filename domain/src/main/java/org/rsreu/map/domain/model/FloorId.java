package org.rsreu.map.domain.model;

/**
 * Unique identifier for a floor within a building.
 */
public record FloorId(String value) {

    public FloorId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("FloorId value cannot be null or blank");
        }
    }
}
