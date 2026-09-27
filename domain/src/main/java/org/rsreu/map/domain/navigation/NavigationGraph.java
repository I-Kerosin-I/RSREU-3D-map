package org.rsreu.map.domain.navigation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Navigation graph containing nodes and edges for pathfinding.
 */
public record NavigationGraph(
    List<NavigationNode> nodes,
    List<NavigationEdge> edges
) {

    public NavigationGraph {
        if (nodes == null) throw new IllegalArgumentException("NavigationGraph nodes cannot be null");
        if (edges == null) throw new IllegalArgumentException("NavigationGraph edges cannot be null");
    }

    /**
     * Returns all nodes adjacent to the given node.
     */
    public List<NavigationNode> adjacent(NavigationNode node) {
        List<NavigationNode> result = new ArrayList<>();
        for (NavigationEdge edge : edges) {
            if (edge.from().equals(node)) {
                result.add(edge.to());
            } else if (edge.bidirectional() && edge.to().equals(node)) {
                result.add(edge.from());
            }
        }
        return result;
    }

    /**
     * Finds a node by its ID.
     */
    public Optional<NavigationNode> findNodeById(String nodeId) {
        return nodes.stream()
            .filter(node -> node.id().equals(nodeId))
            .findFirst();
    }

    /**
     * Returns a map of node ID to adjacent node IDs for efficient lookup.
     */
    public Map<String, List<NavigationNode>> buildAdjacencyMap() {
        Map<String, List<NavigationNode>> map = new HashMap<>();
        for (NavigationNode node : nodes) {
            map.put(node.id(), new ArrayList<>());
        }
        for (NavigationEdge edge : edges) {
            map.get(edge.from().id()).add(edge.to());
            if (edge.bidirectional()) {
                map.get(edge.to().id()).add(edge.from());
            }
        }
        return map;
    }
}