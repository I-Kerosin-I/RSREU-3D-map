package org.rsreu.map.domain.navigation;

import org.rsreu.map.domain.geometry.Point3;
import org.rsreu.map.domain.model.FloorId;

/**
 * A point in the navigation graph.
 * Usually placed in corridors and transitions, not necessarily inside rooms.
 */
public record NavigationNode(
    String id,
    FloorId floorId,
    Point3 position
) {

    public NavigationNode {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("NavigationNode id cannot be null or blank");
        }
        if (floorId == null) throw new IllegalArgumentException("NavigationNode floorId cannot be null");
        if (position == null) throw new IllegalArgumentException("NavigationNode position cannot be null");
    }
}
