package org.rsreu.map.controller;

import org.rsreu.map.domain.campus.Space;

import java.util.List;

/**
 * Snapshot of the application state the UI renders. Produced by
 * {@link MapController}, consumed by UI components; never modified by widgets.
 */
public record MapUiState(
    List<Space> searchResults,
    String selectedRoomId
) {

    public static final MapUiState EMPTY = new MapUiState(List.of(), null);

    public MapUiState {
        searchResults = searchResults == null ? List.of() : List.copyOf(searchResults);
    }
}
