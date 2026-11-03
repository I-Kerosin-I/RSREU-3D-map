package org.rsreu.map.controller;

import org.rsreu.map.domain.campus.Space;

import java.util.List;

/**
 * Snapshot of the application state the UI renders. Produced by
 * {@link MapController}, consumed by UI components; never modified by widgets.
 * <p>
 * Exactly one of {@code selectedRoomId} and {@code selectedCategoryId} is set
 * at a time: a room selection (search / tree leaf) or a category visualization
 * (tree category node).
 */
public record MapUiState(
    List<Space> searchResults,
    String selectedRoomId,
    String selectedCategoryId,
    String selectedSubcategoryId
) {

    public static final MapUiState EMPTY = new MapUiState(List.of(), null, null, null);

    public MapUiState {
        searchResults = searchResults == null ? List.of() : List.copyOf(searchResults);
    }
}
