package org.rsreu.map.domain.navigation;

/**
 * Connection between two navigation nodes.
 */
public record NavigationEdge(
    String id,
    NavigationNode from,
    NavigationNode to,
    EdgeType type,
    boolean bidirectional
) {

    public NavigationEdge {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("NavigationEdge id cannot be null or blank");
        }
        if (from == null) throw new IllegalArgumentException("NavigationEdge from cannot be null");
        if (to == null) throw new IllegalArgumentException("NavigationEdge to cannot be null");
        if (type == null) throw new IllegalArgumentException("NavigationEdge type cannot be null");
    }
}