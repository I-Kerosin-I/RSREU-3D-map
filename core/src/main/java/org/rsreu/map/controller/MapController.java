package org.rsreu.map.controller;

/**
 * Placeholder for future state management.
 * Will orchestrate input, use cases, rendering, and UI state.
 */
public class MapController {

    private MapUiState state;

    public MapController() {
        this.state = MapUiState.empty();
    }

    public MapUiState getState() {
        return state;
    }

    public void updateState(MapUiState newState) {
        this.state = newState;
    }

    public void updateCurrentFloor(String floorId) {
        this.state = new MapUiState(floorId, state.selectedRoom(), state.searchResults(), state.route());
    }

    public void updateSelectedRoom(String roomId) {
        this.state = new MapUiState(state.currentFloor(), roomId, state.searchResults(), state.route());
    }

    public void updateSearchResults(String[] results) {
        this.state = new MapUiState(state.currentFloor(), state.selectedRoom(), results, state.route());
    }

    public void updateRoute(String route) {
        this.state = new MapUiState(state.currentFloor(), state.selectedRoom(), state.searchResults(), route);
    }

    public void clearSelection() {
        this.state = new MapUiState(state.currentFloor(), null, state.searchResults(), state.route());
    }

    public void clearRoute() {
        this.state = new MapUiState(state.currentFloor(), state.selectedRoom(), state.searchResults(), null);
    }
}