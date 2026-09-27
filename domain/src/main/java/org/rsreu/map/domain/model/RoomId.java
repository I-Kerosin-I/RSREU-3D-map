package org.rsreu.map.domain.model;

/**
 * Unique identifier for a space (room, corridor, etc.).
 */
public record RoomId(String value) {

    public RoomId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("RoomId value cannot be null or blank");
        }
    }
}
