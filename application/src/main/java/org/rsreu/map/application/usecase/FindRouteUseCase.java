package org.rsreu.map.application.usecase;

import org.rsreu.map.application.port.CampusRepository;
import org.rsreu.map.domain.campus.Campus;
import org.rsreu.map.domain.campus.Space;
import org.rsreu.map.domain.model.RoomId;
import org.rsreu.map.domain.navigation.NavigationEdge;
import org.rsreu.map.domain.navigation.NavigationGraph;
import org.rsreu.map.domain.navigation.NavigationNode;
import org.rsreu.map.domain.navigation.Route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Use case for finding a route between two rooms.
 */
public class FindRouteUseCase {

    private final CampusRepository campusRepository;

    public FindRouteUseCase(CampusRepository campusRepository) {
        this.campusRepository = campusRepository;
    }

    /**
     * Finds a route between two rooms using A* pathfinding.
     *
     * @param from start room ID
     * @param to   destination room ID
     * @return the route if found
     */
    public Optional<Route> execute(RoomId from, RoomId to) {
        if (from == null || to == null) {
            return Optional.empty();
        }

        Optional<Space> fromSpace = campusRepository.findSpace(from);
        Optional<Space> toSpace = campusRepository.findSpace(to);

        if (fromSpace.isEmpty() || toSpace.isEmpty()) {
            return Optional.empty();
        }

        Campus campus = campusRepository.getCampus();
        NavigationGraph graph = findGraphForBuilding(campus, fromSpace.get());

        if (graph == null || graph.nodes().isEmpty()) {
            return Optional.empty();
        }

        return findPath(graph, fromSpace.get(), toSpace.get());
    }

    private NavigationGraph findGraphForBuilding(Campus campus, Space space) {
        // For now, return the first building's navigation graph
        // In the full implementation, this would find the building containing the space
        if (campus.buildings().isEmpty()) {
            return null;
        }
        return null; // Navigation graph is not yet part of Building - placeholder
    }

    private Optional<Route> findPath(NavigationGraph graph, Space fromSpace, Space toSpace) {
        // Simplified A* implementation
        // In a full implementation, we would:
        // 1. Find or create navigation nodes associated with from/to spaces
        // 2. Run A* on the navigation graph
        // 3. Return the resulting route

        // For now, return empty as navigation graph needs to be connected to Building
        return Optional.empty();
    }
}