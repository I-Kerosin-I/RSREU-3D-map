package org.rsreu.map.render;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.collision.Ray;
import org.rsreu.map.input.RoomPicker;

/**
 * Coordinates the interactive floor isolation and room highlighting.
 * <p>
 * Before a floor is selected (click) the hovered floor is shown opaque while all
 * other floors and the shell are dimmed, following the cursor. After a click the
 * selection is locked: only the selected floor stays visible and the cursor only
 * controls the accent highlight of the hitbox below it. {@link #clearSelection()}
 * (Escape or a click on empty space) returns to the normal fully opaque state.
 */
public final class RoomHighlighter {

    private final PerspectiveCamera camera;
    private final RoomPicker picker;
    private final RoomMaterialManager materials;

    private String lockedFloor;
    private String hoveredFloor;
    private String hoveredHitbox;

    public RoomHighlighter(PerspectiveCamera camera, RoomPicker picker, RoomMaterialManager materials) {
        this.camera = camera;
        this.picker = picker;
        this.materials = materials;
    }

    /**
     * Called on mouse movement. Updates the floor isolation and the accent
     * highlight of the room under the cursor; after a floor is selected the
     * cursor only controls the highlight.
     */
    public void onMouseMoved(int screenX, int screenY) {
        if (camera == null) {
            return;
        }
        camera.update();
        String hitbox = pickVisibleHitbox(camera.getPickRay(screenX, screenY));

        if (lockedFloor == null) {
            String floor = hitbox == null ? null : picker.floorOf(hitbox);
            if (!java.util.Objects.equals(floor, hoveredFloor)) {
                hoveredFloor = floor;
                materials.resetAll();
                if (floor != null) {
                    materials.applyHover(floor);
                }
            }
            // The accent highlight follows the cursor in the normal state too:
            // resetAll above cleared it whenever the floor changed.
            if (!java.util.Objects.equals(hitbox, hoveredHitbox)) {
                if (hoveredHitbox != null) {
                    materials.setHitboxHighlighted(hoveredHitbox, false);
                }
                hoveredHitbox = hitbox;
                if (hitbox != null) {
                    materials.setHitboxHighlighted(hitbox, true);
                }
            }
        } else {
            if (!java.util.Objects.equals(hitbox, hoveredHitbox)) {
                if (hoveredHitbox != null) {
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
     * on empty space clears the selection.
     */
    public void onClick(int screenX, int screenY) {
        if (camera == null) {
            return;
        }
        camera.update();
        String hitbox = pickVisibleHitbox(camera.getPickRay(screenX, screenY));

        if (hitbox != null) {
            String floor = picker.floorOf(hitbox);
            if (floor != null) {
                materials.resetAll();
                materials.selectFloor(floor);
                lockedFloor = floor;
                hoveredFloor = null;
                hoveredHitbox = null;
            }
        } else {
            clearSelection();
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
            && !java.util.Objects.equals(picker.floorOf(hitbox), lockedFloor)) {
            return null;
        }
        return hitbox;
    }

    /**
     * Returns to the normal state: every floor fully opaque, no highlights.
     */
    public void clearSelection() {
        lockedFloor = null;
        hoveredFloor = null;
        hoveredHitbox = null;
        materials.resetAll();
    }
}
