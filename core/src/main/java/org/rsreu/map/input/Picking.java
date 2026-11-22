package org.rsreu.map.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.collision.Ray;

/**
 * Screen-space picking rays. The two-argument {@link Camera#getPickRay(float,
 * float)} assumes the viewport spans the whole window; the classic chrome
 * reserves window space around the 3D work area, so rays must be built against
 * the {@link WorkArea} rectangle instead.
 */
public final class Picking {

    private Picking() {
    }

    /** A ray through the given window coordinates, or null outside the 3D work area. */
    public static Ray getPickRay(Camera camera, WorkArea workArea, float screenX, float screenY) {
        if (!workArea.containsScreen(screenX, screenY, Gdx.graphics.getHeight())) return null;
        return camera.getPickRay(screenX, screenY, workArea.x, workArea.y,
            workArea.width, workArea.height);
    }
}
