package org.rsreu.map.input;

/**
 * The 3D work area inside the window, in GL coordinates (origin bottom-left):
 * the window minus the menu/status strips and both docked sidebars. The game
 * re-derives it from the laid-out UI every frame;
 * picking builds its rays against this rectangle, not against the whole window.
 */
public final class WorkArea {

    public int x;
    public int y;
    public int width;
    public int height;

    /** Pointer coordinates use a top-left origin; the rectangle uses bottom-left. */
    public boolean containsScreen(float screenX, float screenY, int windowHeight) {
        float top = windowHeight - y - height;
        return width > 0 && height > 0 && screenX >= x && screenX < x + width
            && screenY >= top && screenY < top + height;
    }

    public void set(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}
