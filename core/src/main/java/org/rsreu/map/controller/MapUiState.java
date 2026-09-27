package org.rsreu.map.controller;

/**
 * UI state for the map application.
 */
public record MapUiState(
    String currentFloor,
    String selectedRoom,
    String[] searchResults,
    String route
) {
    public MapUiState {
        // Default values
    }

    public static MapUiState empty() {
        return new MapUiState(null, null, null, null);
    }
}