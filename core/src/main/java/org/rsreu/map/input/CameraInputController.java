package org.rsreu.map.input;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

/**
 * Camera input controller that handles mouse drag for orbiting
 * and WASD keys for movement.
 */
public class CameraInputController extends InputAdapter {

    private PerspectiveCamera camera;

    private boolean isDragging = false;
    private int lastX = 0;
    private int lastY = 0;

    private static final float ROTATION_SPEED = 0.5f;
    private static final float MOVE_SPEED = 5f;

    private boolean keyW = false;
    private boolean keyA = false;
    private boolean keyS = false;
    private boolean keyD = false;

    public CameraInputController() {
        super();
    }

    public void setCamera(PerspectiveCamera camera) {
        this.camera = camera;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        isDragging = true;
        lastX = screenX;
        lastY = screenY;
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        isDragging = false;
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (isDragging && camera != null) {
            float deltaX = screenX - lastX;
            float deltaY = screenY - lastY;

            // Rotate camera around origin
            float angleX = deltaX * ROTATION_SPEED;
            float angleY = deltaY * ROTATION_SPEED;

            // Get camera position
            Vector3 position = camera.position.cpy();

            // Simple orbit: rotate around Y axis
            float distance = position.len();
            float theta = (float) Math.atan2(position.x, position.z);
            float phi = (float) Math.acos(position.y / distance);

            theta -= Math.toRadians(angleX * 0.5f);
            phi -= Math.toRadians(angleY * 0.5f);

            // Clamp phi to avoid gimbal lock
            phi = Math.max(0.1f, Math.min((float) Math.PI - 0.1f, phi));

            position.x = distance * (float) Math.sin(phi) * (float) Math.sin(theta);
            position.y = distance * (float) Math.cos(phi);
            position.z = distance * (float) Math.sin(phi) * (float) Math.cos(theta);

            camera.position.set(position);
            camera.lookAt(0, 0, 0);
            camera.update();

            lastX = screenX;
            lastY = screenY;
        }
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case 19: keyW = true; break; // W key
            case 29: keyA = true; break; // A key
            case 47: keyS = true; break; // S key
            case 32: keyD = true; break; // D key
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        switch (keycode) {
            case 19: keyW = false; break;
            case 29: keyA = false; break;
            case 47: keyS = false; break;
            case 32: keyD = false; break;
        }
        return true;
    }

    /**
     * Updates camera position based on WASD keys.
     * Should be called in the render loop.
     */
    public void update() {
        if (camera == null) {
            return;
        }

        float deltaTime = 1f / 60f; // Assume 60 FPS for simplicity
        float moveDistance = MOVE_SPEED * deltaTime;

        Vector3 forward = new Vector3();
        Vector3 right = new Vector3();

        // Get camera direction
        forward.set(camera.direction).nor();
        forward.y = 0;
        forward.nor();

        // Get right vector
        right.set(forward).crs(Vector3.Y);

        Vector3 movement = new Vector3();

        if (keyW) movement.add(forward);
        if (keyS) movement.sub(forward);
        if (keyD) movement.add(right);
        if (keyA) movement.sub(right);

        if (movement.len() > 0) {
            movement.nor().scl(moveDistance);
            camera.position.add(movement);
            camera.update();
        }
    }

    /**
     * Gets the ray from camera through the given screen coordinates.
     */
    public Ray getRay(int screenX, int screenY) {
        if (camera == null) {
            return null;
        }

        return camera.getPickRay(screenX, screenY);
    }
}