package org.rsreu.map.render;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.collision.Ray;
import org.rsreu.map.input.RoomPicker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Coordinates the interactive floor isolation and room highlighting.
 * <p>
 * Before a floor is selected (click) the hovered floor is shown opaque while all
 * other floors and the shell are dimmed, following the cursor. After a click the
 * selection is locked: only the selected floor stays visible and the cursor only
 * controls the accent highlight of the hitbox below it. A room selected through
 * the UI ({@link #selectRoom}) stays highlighted for as long as the selection
 * lives, independent of where the cursor points. {@link #showCategory} highlights
 * every room of a category over the equally dimmed building, across all floors.
 * {@link #clearSelection()} (Escape or a click on empty space) returns to the
 * normal fully opaque state.
 */
public final class RoomHighlighter {

    private final PerspectiveCamera camera;
    private final RoomPicker picker;
    private final RoomMaterialManager materials;

    private String lockedFloor;
    private String hoveredFloor;
    private String hoveredHitbox;
    /** The room selected through the UI; highlighted independently of the hover. */
    private String selectedHitbox;
    /** Non-null while category mode is on: every hitbox to keep highlighted. */
    private List<String> categoryHitboxes;

    /** Notified when the 3D side discards a UI-made selection (Escape, click). */
    private Runnable selectionClearedCallback;

    public RoomHighlighter(PerspectiveCamera camera, RoomPicker picker, RoomMaterialManager materials) {
        this.camera = camera;
        this.picker = picker;
        this.materials = materials;
    }

    /**
     * Wires the callback fired when the scene itself discards the selection made
     * through the UI, so the menu can drop its visual selection as well.
     */
    public void setSelectionClearedCallback(Runnable callback) {
        selectionClearedCallback = callback;
    }

    /**
     * The hitbox node name currently accent-highlighted by the hover, or
     * {@code null}. Read by the UI layer to show the hover tooltip.
     */
    public String hoveredHitbox() {
        return hoveredHitbox;
    }

    /**
     * The locked (selected) floor node name, or {@code null} when no floor is
     * selected.
     */
    public String lockedFloor() {
        return lockedFloor;
    }

    /**
     * Called on mouse movement. Updates the floor isolation and the accent
     * highlight of the room under the cursor; after a floor is selected the
     * cursor only controls the hover highlight, the UI-selected room stays lit.
     */
    public void onMouseMoved(int screenX, int screenY) {
        if (camera == null) {
            return;
        }
        camera.update();
        String hitbox = pickVisibleHitbox(camera.getPickRay(screenX, screenY));

        if (categoryHitboxes != null) {
            // Category mode: no floor isolation, the hover state only feeds the
            // tooltip; the category highlights are cursor-independent.
            hoveredHitbox = hitbox;
            return;
        }

        if (lockedFloor == null) {
            String floor = hitbox == null ? null : picker.floorOf(hitbox);
            if (!Objects.equals(floor, hoveredFloor)) {
                hoveredFloor = floor;
                materials.resetAll();
                if (floor != null) {
                    materials.applyHover(floor);
                }
            }
            // The accent highlight follows the cursor in the normal state too:
            // resetAll above cleared it whenever the floor changed.
            if (!Objects.equals(hitbox, hoveredHitbox)) {
                if (hoveredHitbox != null) {
                    materials.setHitboxHighlighted(hoveredHitbox, false);
                }
                hoveredHitbox = hitbox;
                if (hitbox != null) {
                    materials.setHitboxHighlighted(hitbox, true);
                }
            }
        } else {
            if (!Objects.equals(hitbox, hoveredHitbox)) {
                // The UI-selected room keeps its highlight wherever the cursor goes.
                if (hoveredHitbox != null && !Objects.equals(hoveredHitbox, selectedHitbox)) {
                    materials.setHitboxHighlighted(hoveredHitbox, false);
                }
                hoveredHitbox = hitbox;
                if (hitbox != null) {
                    materials.setHitboxHighlighted(hitbox, true);
                }
            }
        }
    }

    /**
     * Called on click. A click on a room locks its floor as the selection; a click
     * on empty space clears the selection. Any scene click also discards a
     * selection made through the UI: the scene selects floors, the UI selects
     * rooms, and they do not mix.
     */
    public void onClick(int screenX, int screenY) {
        if (camera == null) {
            return;
        }
        camera.update();
        String hitbox = pickVisibleHitbox(camera.getPickRay(screenX, screenY));

        if (hitbox != null) {
            boolean hadUiSelection = selectedHitbox != null || categoryHitboxes != null;
            selectedHitbox = null;
            categoryHitboxes = null;
            String floor = picker.floorOf(hitbox);
            if (floor != null) {
                materials.resetAll();
                materials.selectFloor(floor);
                lockedFloor = floor;
                hoveredFloor = null;
                hoveredHitbox = null;
            }
            if (hadUiSelection) {
                notifySelectionCleared();
            }
        } else {
            clearSelection();
        }
    }

    /**
     * Selects a room programmatically (from search results or the category tree):
     * locks its floor and keeps the room accent-highlighted until the selection
     * is cleared, wherever the cursor moves.
     *
     * @param hitboxNodeName node name of the room's hitbox
     */
    public void selectRoom(String hitboxNodeName) {
        if (hitboxNodeName == null) {
            return;
        }
        String floor = picker.floorOf(hitboxNodeName);
        if (floor == null) {
            return;
        }
        categoryHitboxes = null;
        selectedHitbox = hitboxNodeName;
        materials.resetAll();
        materials.selectFloor(floor);
        lockedFloor = floor;
        hoveredFloor = null;
        hoveredHitbox = null;
        materials.setHitboxHighlighted(hitboxNodeName, true);
    }

    /**
     * Enters category mode: every listed room is accent-highlighted as an x-ray
     * marker while all floors and the shell are dimmed equally, so rooms of
     * different floors show through the whole building. The mode lasts until a
     * scene click (which locks a floor) or Escape returns to the normal state.
     *
     * @param hitboxNodeNames node names of the category's hitboxes, non-empty
     */
    public void showCategory(List<String> hitboxNodeNames) {
        if (hitboxNodeNames == null || hitboxNodeNames.isEmpty()) {
            return;
        }
        selectedHitbox = null;
        categoryHitboxes = new ArrayList<>(hitboxNodeNames);
        lockedFloor = null;
        hoveredFloor = null;
        hoveredHitbox = null;
        materials.resetAll();
        materials.dimAll();
        for (String hitboxNodeName : categoryHitboxes) {
            materials.setHitboxHighlighted(hitboxNodeName, true);
        }
    }

    /**
     * Picks the hitbox under the ray, ignoring rooms of floors the user can not
     * currently see: while a floor is locked, hitboxes of the hidden floors must
     * neither highlight nor react to clicks.
     */
    private String pickVisibleHitbox(Ray ray) {
        String hitbox = picker.pick(ray);
        if (hitbox != null && lockedFloor != null
            && !Objects.equals(picker.floorOf(hitbox), lockedFloor)) {
            return null;
        }
        return hitbox;
    }

    /**
     * Returns to the normal state: every floor fully opaque, no highlights.
     */
    public void clearSelection() {
        boolean hadUiSelection = selectedHitbox != null || categoryHitboxes != null;
        lockedFloor = null;
        hoveredFloor = null;
        hoveredHitbox = null;
        selectedHitbox = null;
        categoryHitboxes = null;
        materials.resetAll();
        if (hadUiSelection) {
            notifySelectionCleared();
        }
    }

    private void notifySelectionCleared() {
        if (selectionClearedCallback != null) {
            selectionClearedCallback.run();
        }
    }
}
