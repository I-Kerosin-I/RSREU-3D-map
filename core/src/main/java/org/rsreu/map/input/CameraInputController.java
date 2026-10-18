package org.rsreu.map.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

/**
 * Orbit camera controller.
 *
 * <p>Left mouse button pans the camera and the orbit pivot inside the horizontal
 * (XZ) plane. Right mouse button orbits the camera around the pivot point that
 * sits {@link #orbitDistance} units in front of the camera. The mouse wheel moves
 * the camera closer to or further from that pivot.
 *
 * <p>The camera orientation is kept as a single quaternion. Every orbit step
 * composes exactly two rotations: a yaw around the world up vector and a pitch
 * around the camera's own right axis, which is always horizontal. A rotation
 * around the view axis is never applied, so the camera can not accumulate roll
 * and always stays level with the world up vector.
 */
public class CameraInputController extends InputAdapter {

    private static final float ROTATION_SPEED = 0.3f;  // degrees per pixel
    private static final float PAN_SPEED = 0.001f;      // fraction of orbit distance per pixel
    private static final float KEY_MOVE_SPEED = 5f;    // units per second
    private static final float ZOOM_STEP = 1.1f;       // orbit distance multiplier per scroll notch
    private static final float MIN_PITCH = -85f;
    private static final float MAX_PITCH = 85f;

    private static final float MIN_ORBIT_DISTANCE = 1f;
    private static final float MAX_ORBIT_DISTANCE = 500f;

    /** Distance from the camera to the orbit pivot, in world units. */
    private float orbitDistance = 10f;

    private static final Vector3 LOCAL_FORWARD = new Vector3(0f, 0f, -1f);
    private static final Vector3 LOCAL_UP = new Vector3(0f, 1f, 0f);
    private static final Vector3 LOCAL_RIGHT = new Vector3(1f, 0f, 0f);

    private PerspectiveCamera camera;

    /** Camera orientation, roll free by construction. */
    private final Quaternion orientation = new Quaternion();
    private final Quaternion yawStep = new Quaternion();
    private final Quaternion pitchStep = new Quaternion();
    private final Quaternion worldPitchStep = new Quaternion();
    private final Quaternion previous = new Quaternion();

    /** Point the camera orbits around, kept in world space. */
    private final Vector3 pivot = new Vector3();

    private final Vector3 tmpDirection = new Vector3();
    private final Vector3 tmpRight = new Vector3();
    private final Vector3 tmpMove = new Vector3();

    private boolean panning;
    private boolean orbiting;
    private int lastX;
    private int lastY;

    private boolean keyW;
    private boolean keyA;
    private boolean keyS;
    private boolean keyD;

    public CameraInputController() {
        super();
    }

    public void setCamera(PerspectiveCamera camera) {
        this.camera = camera;
        readOrientationFromCamera();
    }

    /**
     * Rebuilds the orientation quaternion from the current camera direction so the
     * controller always continues from the actual camera state.
     */
    private void readOrientationFromCamera() {
        if (camera == null) {
            return;
        }
        tmpDirection.set(camera.direction).nor();
        float yaw = (float) Math.toDegrees(Math.atan2(-tmpDirection.x, -tmpDirection.z));
        float pitch = (float) Math.toDegrees(Math.asin(clamp(tmpDirection.y, -1f, 1f)));
        orientation.set(Vector3.Y, yaw).mul(pitchStep.set(LOCAL_RIGHT, pitch)).nor();
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        lastX = screenX;
        lastY = screenY;

        if (camera == null) {
            return false;
        }
        if (button == Input.Buttons.LEFT) {
            panning = true;
            return true;
        }
        if (button == Input.Buttons.RIGHT) {
            orbiting = true;
            readOrientationFromCamera();
            updatePivot();
            return true;
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            panning = false;
        } else if (button == Input.Buttons.RIGHT) {
            orbiting = false;
        }
        return panning || orbiting;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (camera == null) {
            return false;
        }

        float deltaX = screenX - lastX;
        float deltaY = screenY - lastY;
        lastX = screenX;
        lastY = screenY;

        if (orbiting) {
            orbit(deltaX, deltaY);
            return true;
        }
        if (panning) {
            pan(deltaX, deltaY);
            return true;
        }
        return false;
    }

    /**
     * Zooms with the mouse wheel. The pivot stays where it is, the camera moves
     * towards or away from it along the view ray.
     */
    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (camera == null || amountY == 0f) {
            return false;
        }
        zoom(amountY);
        return true;
    }

    /**
     * Changes the orbit distance while keeping the pivot in front of the camera.
     *
     * @param amount positive values zoom in, negative values zoom out
     */
    private void zoom(float amount) {
        // The pivot does not move, so it is recomputed from the current orbit
        // distance before that distance is changed.
        updatePivot();

        orbitDistance = clamp(orbitDistance * (float) Math.pow(ZOOM_STEP, amount),
            MIN_ORBIT_DISTANCE, MAX_ORBIT_DISTANCE);

        tmpDirection.set(camera.direction).nor();
        camera.position.set(pivot).mulAdd(tmpDirection, -orbitDistance);
        camera.update();
    }

    /**
     * Orbits the camera around the pivot point. The camera position is rotated
     * around the pivot, and the orientation is rotated by the very same amount.
     */
    private void orbit(float deltaX, float deltaY) {
        float yaw = -deltaX * ROTATION_SPEED;
        float pitch = -deltaY * ROTATION_SPEED;

        // Keep the camera from tipping over the poles.
        float currentPitch = pitchDegrees();
        pitch = clamp(currentPitch + pitch, MIN_PITCH, MAX_PITCH) - currentPitch;

        // Move the camera around the pivot: first pitch around the camera's own
        // right axis, then yaw around the world up vector. The order matches the
        // orientation update below, so the pivot stays exactly in front of the
        // camera.
        tmpRight.set(LOCAL_RIGHT).mul(orientation).nor();
        tmpMove.set(camera.position).sub(pivot);
        tmpMove.mul(worldPitchStep.set(tmpRight, pitch));
        tmpMove.mul(yawStep.set(Vector3.Y, yaw));
        camera.position.set(pivot).add(tmpMove);

        // Yaw is applied in world space, pitch around the camera's right axis.
        previous.set(orientation);
        orientation.set(yawStep).mul(previous).mul(pitchStep.set(LOCAL_RIGHT, pitch)).nor();

        applyOrientation();
        camera.update();
    }

    /**
     * Slides the camera and the pivot in the horizontal plane, keeping the viewing
     * direction and the camera height unchanged.
     */
    private void pan(float deltaX, float deltaY) {
        horizontalForward(tmpDirection);
        tmpRight.set(tmpDirection).crs(Vector3.Y).nor();

        float scale = orbitDistance * PAN_SPEED;
        tmpMove.setZero()
            .mulAdd(tmpRight, -deltaX * scale)
            .mulAdd(tmpDirection, deltaY * scale);
        tmpMove.y = 0f;

        camera.position.add(tmpMove);
        pivot.add(tmpMove);
        camera.update();
    }

    /**
     * Places the pivot a fixed distance in front of the camera.
     */
    private void updatePivot() {
        tmpDirection.set(camera.direction).nor();
        pivot.set(camera.position).mulAdd(tmpDirection, orbitDistance);
    }

    /** Writes the orientation quaternion into the camera direction and up vectors. */
    private void applyOrientation() {
        camera.direction.set(LOCAL_FORWARD).mul(orientation).nor();
        camera.up.set(LOCAL_UP).mul(orientation).nor();
    }

    /** Current pitch of the camera in degrees. */
    private float pitchDegrees() {
        tmpDirection.set(LOCAL_FORWARD).mul(orientation);
        return (float) Math.toDegrees(Math.asin(clamp(tmpDirection.y, -1f, 1f)));
    }

    /** Camera direction projected onto the horizontal plane. */
    private void horizontalForward(Vector3 out) {
        out.set(camera.direction);
        out.y = 0f;
//        if (out.sqrLen() < 1e-8f) {
//            // Looking straight up or down: fall back to the current yaw.
//            out.set(LOCAL_FORWARD).mul(orientation);
//            out.y = 0f;
//        }
        out.nor();
    }

    @Override
    public boolean keyDown(int keycode) {
        return switch (keycode) {
            case Input.Keys.W -> {
                keyW = true;
                yield true;
            }
            case Input.Keys.A -> {
                keyA = true;
                yield true;
            }
            case Input.Keys.S -> {
                keyS = true;
                yield true;
            }
            case Input.Keys.D -> {
                keyD = true;
                yield true;
            }
            default -> false;
        };
    }

    @Override
    public boolean keyUp(int keycode) {
        switch (keycode) {
            case Input.Keys.W:
                keyW = false;
                return true;
            case Input.Keys.A:
                keyA = false;
                return true;
            case Input.Keys.S:
                keyS = false;
                return true;
            case Input.Keys.D:
                keyD = false;
                return true;
            default:
                return false;
        }
    }

    /**
     * Applies WASD movement in the horizontal plane. Called from the render loop.
     */
    public void update() {
        if (camera == null) {
            return;
        }

        horizontalForward(tmpDirection);
        tmpRight.set(tmpDirection).crs(Vector3.Y).nor();
        tmpMove.setZero();

        if (keyW) tmpMove.add(tmpDirection);
        if (keyS) tmpMove.sub(tmpDirection);
        if (keyD) tmpMove.add(tmpRight);
        if (keyA) tmpMove.sub(tmpRight);

        if (tmpMove.isZero()) {
            return;
        }

        // Only the horizontal part of the movement is applied.
        tmpMove.y = 0f;
        tmpMove.nor().scl(Gdx.graphics.getDeltaTime() * KEY_MOVE_SPEED);
        camera.position.add(tmpMove);
        pivot.add(tmpMove);
        camera.update();
    }

    /**
     * Gets the ray from the camera through the given screen coordinates.
     */
    public Ray getRay(int screenX, int screenY) {
        if (camera == null) {
            return null;
        }
        return camera.getPickRay(screenX, screenY);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
