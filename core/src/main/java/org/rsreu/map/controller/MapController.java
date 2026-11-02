package org.rsreu.map.controller;

import org.rsreu.map.application.port.CampusRepository;
import org.rsreu.map.application.usecase.FindRoomsUseCase;
import org.rsreu.map.domain.campus.Space;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Application-level mediator between the UI and the map data / 3D scene.
 * <p>
 * UI components issue commands ({@link #search}, {@link #selectObject},
 * {@link #clearSelection}) and listen for state changes; they never touch 3D
 * nodes directly. Commands that must affect the 3D scene are forwarded to
 * {@link MapActions}, implemented by the game layer (RoomHighlighter + camera).
 */
public class MapController {

    /**
     * 3D-side effects of application commands, implemented by the game.
     */
    public interface MapActions {

        /** Locks the room's floor, highlights the room and flies the camera to it. */
        void focusRoom(String hitboxNodeName);

        /** Returns the scene to the normal state (no selection, no highlight). */
        void clearFocus();
    }

    /**
     * Receives every new application state snapshot.
     */
    public interface StateListener {

        void onStateChanged(MapUiState state);
    }

    private final FindRoomsUseCase findRooms;
    private final CampusRepository campusRepository;

    private final List<StateListener> listeners = new ArrayList<>();

    private MapActions actions;
    private Supplier<String> hoveredHitboxSupplier = () -> null;
    private MapUiState state = MapUiState.EMPTY;

    public MapController(FindRoomsUseCase findRooms, CampusRepository campusRepository) {
        if (findRooms == null) throw new IllegalArgumentException("FindRoomsUseCase cannot be null");
        if (campusRepository == null) throw new IllegalArgumentException("CampusRepository cannot be null");
        this.findRooms = findRooms;
        this.campusRepository = campusRepository;
    }

    public void setMapActions(MapActions actions) {
        this.actions = actions;
    }

    /** Wires the source of the currently picked hitbox (RoomHighlighter getter). */
    public void setHoveredHitboxSupplier(Supplier<String> supplier) {
        this.hoveredHitboxSupplier = supplier == null ? () -> null : supplier;
    }

    public void addStateListener(StateListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public MapUiState getState() {
        return state;
    }

    /** The hitbox node name under the cursor, or {@code null}. */
    public String hoveredHitbox() {
        return hoveredHitboxSupplier.get();
    }

    /** The space metadata of the hitbox under the cursor, if the manifest knows it. */
    public Optional<Space> findSpaceByHitbox(String hitboxNodeName) {
        return campusRepository.findSpaceByNodeName(hitboxNodeName);
    }

    /**
     * All spaces of a category (optionally of one subcategory), for the category
     * tree of the object browser panel.
     */
    public List<Space> findByCategory(String category, String subcategory) {
        return findRooms.findByCategory(category, subcategory);
    }

    /**
     * Runs a search by display name and description and publishes the results.
     *
     * @param query search query, blank queries yield no results
     */
    public void search(String query) {
        state = new MapUiState(findRooms.findByQuery(query), state.selectedRoomId());
        notifyListeners();
    }

    /**
     * Selects an object from the UI: publishes the selection and asks the 3D
     * layer to lock its floor, highlight it and fly the camera to it.
     */
    public void selectObject(Space space) {
        if (space == null) {
            return;
        }
        state = new MapUiState(state.searchResults(), space.id().value());
        if (actions != null) {
            actions.focusRoom(space.modelNodeName());
        }
        notifyListeners();
    }

    /**
     * Clears the selection made through the UI. The 3D scene returns to the
     * normal state; search results stay until the query changes.
     */
    public void clearSelection() {
        if (state.selectedRoomId() == null) {
            return;
        }
        state = new MapUiState(state.searchResults(), null);
        if (actions != null) {
            actions.clearFocus();
        }
        notifyListeners();
    }

    private void notifyListeners() {
        for (StateListener listener : listeners) {
            listener.onStateChanged(state);
        }
    }
}
