package org.rsreu.map.domain.navigation;

/**
 * Type of navigation edge connecting two nodes.
 */
public enum EdgeType {
    DOOR,        // through a door
    CORRIDOR,    // walking along a corridor
    STAIRS,      // stairs between floors
    ELEVATOR     // elevator between floors
}
