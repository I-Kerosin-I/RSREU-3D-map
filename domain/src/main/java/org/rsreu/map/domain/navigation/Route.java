package org.rsreu.map.domain.navigation;

import java.util.List;

/**
 * Result of pathfinding. Contains ordered list of nodes and total distance.
 */
public record Route(
    List<NavigationNode> nodes,
    float totalDistanceMeters
) {

    public Route {
        if (nodes == null) throw new IllegalArgumentException("Route nodes cannot be null");
    }

    /**
     * Returns the number of nodes in the route.
     */
    public int nodeCount() {
        return nodes.size();
    }

    /**
     * Checks if the route is empty.
     */
    public boolean isEmpty() {
        return nodes.isEmpty();
    }
}