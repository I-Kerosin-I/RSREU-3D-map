package org.rsreu.map.domain.campus;

import org.rsreu.map.domain.model.FloorId;
import org.rsreu.map.domain.model.RoomId;

import java.util.List;
import java.util.Map;

/**
 * Represents any navigable space: room, corridor, stairwell, elevator hall, etc.
 * <p>
 * Besides the display fields, a space carries optional structured attributes
 * (seat count, equipment) and free-form key/value attributes for the room
 * passport; all of them may be absent and are normalized to empty values.
 */
public record Space(
    RoomId id,
    String displayName,
    FloorId floorId,
    SpaceType type,
    String modelNodeName,
    String description,
    String category,
    String subcategory,
    Integer seats,
    List<String> equipment,
    Map<String, String> attributes
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
        equipment = equipment == null ? List.of() : List.copyOf(equipment);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
