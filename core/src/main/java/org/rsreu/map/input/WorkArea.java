package org.rsreu.map.input;

/**
 * The 3D work area inside the window, in GL coordinates (origin bottom-left):
 * the window minus the classic chrome bands (menu bar + toolbar on top, status
 * bar at the bottom). The game re-derives it from the laid-out UI every frame;
 * picking builds its rays against this rectangle, not against the whole window.
 */
public final class WorkArea {

    public int x;
    public int y;
    public int width;
    public int height;

    public void set(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}
