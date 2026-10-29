package org.rsreu.map.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.TimeUtils;
import org.rsreu.map.render.RoomHighlighter;

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
 *
 * <p>All motion carries a bit of inertia: a released drag keeps orbiting /
 * panning with exponentially decaying velocity, the wheel sets a zoom target the
 * orbit distance approaches smoothly, and WASD movement accelerates and brakes
 * softly. Grabbing the camera with any button stops the glide immediately.
 */
public class CameraInputController extends InputAdapter {

    private static final float ROTATION_SPEED = 0.3f;  // degrees per pixel
    private static final float PAN_SPEED = 0.001f;      // fraction of orbit distance per pixel
    private static final float KEY_MOVE_SPEED = 5f;    // units per second
    private static final float ZOOM_STEP = 1.1f;       // orbit distance multiplier per scroll notch
    private static final float MIN_PITCH = -85f;
    private static final float MAX_PITCH = 85f;

    private static final float MIN_ORBIT_DISTANCE = 1f;
    private static final float MAX_ORBIT_DISTANCE = 30f;

    /** Upper bound of the frame time used for camera motion, guards against hitches. */
    private static final float MAX_FRAME_DELTA = 0.05f;
    /** Longest drag event interval used for the velocity estimate, in seconds. */
    private static final float MAX_EVENT_DELTA = 0.1f;
    /** Smoothing of the drag velocity estimate, 1/s. */
    private static final float DRAG_VELOCITY_SMOOTHING = 20f;
    /** Exponential decay of the orbit and pan inertia after release, 1/s. */
    private static final float ORBIT_DECAY = 4.5f;
    private static final float PAN_DECAY = 4.5f;
    /** Rate at which the orbit distance approaches the zoom target, 1/s. */
    private static final float ZOOM_RATE = 9f;
    /** Rate at which the WASD velocity approaches the pressed-key target, 1/s. */
    private static final float KEY_RATE = 9f;
    /** A release after this much pointer idle does not glide, in seconds. */
    private static final float GLIDE_IDLE_MAX = 0.1f;
    /** Velocities below these values snap to zero. */
    private static final float ORBIT_VELOCITY_EPSILON = 1f;   // degrees per second
    private static final float PAN_VELOCITY_EPSILON = 0.05f;  // world units per second
    private static final float ZOOM_EPSILON = 0.005f;         // world units

    private static final float NANOS_TO_SECONDS = 1e-9f;

    /** Squared drag distance below which a left click is treated as a selection click. */
    private static final float CLICK_DIST_SQ = 9f;

    /** Distance from the camera to the orbit pivot, in world units. */
    private float orbitDistance = 5f;

    /** Zoom target the orbit distance smoothly approaches. */
    private float targetOrbitDistance = 5f;

    private static final Vector3 LOCAL_FORWARD = new Vector3(0f, 0f, -1f);
    private static final Vector3 LOCAL_UP = new Vector3(0f, 1f, 0f);
    private static final Vector3 LOCAL_RIGHT = new Vector3(1f, 0f, 0f);

    private PerspectiveCamera camera;

    /** Optional room interaction controller, wired by the game. */
    private RoomHighlighter roomHighlighter;

    private float dragMovedSq;

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

    /** Orbit inertia: angular velocity in degrees per second. */
    private float yawVelocity;
    private float pitchVelocity;
    /** Pan inertia in the horizontal plane, world units per second. */
    private final Vector3 panVelocity = new Vector3();
    /** Current WASD velocity in the horizontal plane, world units per second. */
    private final Vector3 keyVelocity = new Vector3();
    /** Timestamp of the last drag event, used to estimate the drag velocity. */
    private long lastDragNanos;

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
     * Wires the room interaction controller. The camera controller forwards mouse
     * movement, selection clicks and Escape to it.
     */
    public void setRoomHighlighter(RoomHighlighter roomHighlighter) {
        this.roomHighlighter = roomHighlighter;
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
        dragMovedSq = 0f;
        lastDragNanos = TimeUtils.nanoTime();
        // Grabbing the camera stops any glide that is still running.
        yawVelocity = 0f;
        pitchVelocity = 0f;
        panVelocity.setZero();

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
        // A pause before the release means the pointer was already still, so the
        // drag should not glide on.
        float idleSeconds = (TimeUtils.nanoTime() - lastDragNanos) * NANOS_TO_SECONDS;

        if (button == Input.Buttons.LEFT) {
            boolean click = dragMovedSq <= CLICK_DIST_SQ;
            panning = false;
            if (idleSeconds > GLIDE_IDLE_MAX) {
                panVelocity.setZero();
            }
            if (click && roomHighlighter != null) {
                roomHighlighter.onClick(screenX, screenY);
            }
        } else if (button == Input.Buttons.RIGHT) {
            orbiting = false;
            if (idleSeconds > GLIDE_IDLE_MAX) {
                yawVelocity = 0f;
                pitchVelocity = 0f;
            }
        }
        return panning || orbiting;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (camera == null) {
            return false;
        }

        long now = TimeUtils.nanoTime();
        float eventDelta = clamp((now - lastDragNanos) * NANOS_TO_SECONDS, 0f, MAX_EVENT_DELTA);
        lastDragNanos = now;

        float deltaX = screenX - lastX;
        float deltaY = screenY - lastY;
        lastX = screenX;
        lastY = screenY;
        dragMovedSq += deltaX * deltaX + deltaY * deltaY;

        if (orbiting) {
            float yaw = -deltaX * ROTATION_SPEED;
            float pitch = -deltaY * ROTATION_SPEED;
            orbit(yaw, pitch);
            if (eventDelta > 0f) {
                float s = 1f - (float) Math.exp(-DRAG_VELOCITY_SMOOTHING * eventDelta);
                yawVelocity += (yaw / eventDelta - yawVelocity) * s;
                pitchVelocity += (pitch / eventDelta - pitchVelocity) * s;
            }
            return true;
        }
        if (panning) {
            pan(deltaX, deltaY, eventDelta);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (roomHighlighter != null) {
            roomHighlighter.onMouseMoved(screenX, screenY);
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
     * Sets the zoom target while keeping the pivot in front of the camera. The
     * orbit distance itself approaches the target smoothly in {@link #update()}.
     *
     * @param amount positive values zoom in, negative values zoom out
     */
    private void zoom(float amount) {
        // The pivot does not move, so it is recomputed from the current orbit
        // distance before the distance starts changing.
        updatePivot();

        targetOrbitDistance = clamp(targetOrbitDistance * (float) Math.pow(ZOOM_STEP, amount),
            MIN_ORBIT_DISTANCE, MAX_ORBIT_DISTANCE);
    }

    /**
     * Orbits the camera around the pivot point. The camera position is rotated
     * around the pivot, and the orientation is rotated by the very same amount.
     *
     * @param yaw   rotation around the world up vector, in degrees
     * @param pitch rotation around the camera's right axis, in degrees
     */
    private void orbit(float yaw, float pitch) {

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
     * direction and the camera height unchanged. Also tracks the pan velocity for
     * the inertia after release.
     */
    private void pan(float deltaX, float deltaY, float eventDelta) {
        horizontalForward(tmpDirection);
        tmpRight.set(tmpDirection).crs(Vector3.Y).nor();

        float scale = orbitDistance * PAN_SPEED;
        tmpMove.setZero()
            .mulAdd(tmpRight, -deltaX * scale)
            .mulAdd(tmpDirection, deltaY * scale);
        tmpMove.y = 0f;

        moveCameraAndPivot(tmpMove);

        if (eventDelta > 0f) {
            float s = 1f - (float) Math.exp(-DRAG_VELOCITY_SMOOTHING * eventDelta);
            panVelocity.x += (tmpMove.x / eventDelta - panVelocity.x) * s;
            panVelocity.z += (tmpMove.z / eventDelta - panVelocity.z) * s;
        }
    }

    /** Shifts the camera and the pivot by the same offset. */
    private void moveCameraAndPivot(Vector3 offset) {
        camera.position.add(offset);
        pivot.add(offset);
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
            case Input.Keys.ESCAPE -> {
                if (roomHighlighter != null) {
                    roomHighlighter.clearSelection();
                }
                yield true;
            }
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
     * Per-frame camera motion: WASD movement with soft acceleration, drag inertia
     * and smooth zooming. Called from the render loop.
     */
    public void update() {
        if (camera == null) {
            return;
        }
        float delta = clamp(Gdx.graphics.getDeltaTime(), 0f, MAX_FRAME_DELTA);
        if (delta <= 0f) {
            return;
        }
        updateKeyboardMove(delta);
        updateInertia(delta);
        updateZoom(delta);
    }

    /**
     * Applies WASD movement in the horizontal plane. The velocity approaches the
     * pressed-key target smoothly, so the camera softly starts and stops.
     */
    private void updateKeyboardMove(float delta) {
        horizontalForward(tmpDirection);
        tmpRight.set(tmpDirection).crs(Vector3.Y).nor();
        tmpMove.setZero();

        if (keyW) tmpMove.add(tmpDirection);
        if (keyS) tmpMove.sub(tmpDirection);
        if (keyD) tmpMove.add(tmpRight);
        if (keyA) tmpMove.sub(tmpRight);

        // Only the horizontal part of the movement is applied.
        tmpMove.y = 0f;
        if (!tmpMove.isZero()) {
            tmpMove.nor().scl(KEY_MOVE_SPEED);
        }

        float s = 1f - (float) Math.exp(-KEY_RATE * delta);
        keyVelocity.x += (tmpMove.x - keyVelocity.x) * s;
        keyVelocity.z += (tmpMove.z - keyVelocity.z) * s;

        if (keyVelocity.len2() > 1e-8f) {
            moveCameraAndPivot(tmpMove.set(keyVelocity).scl(delta));
        }
    }

    /**
     * Continues orbit and pan motion after the drag is released, with the velocity
     * decaying exponentially until it becomes negligible.
     */
    private void updateInertia(float delta) {
        if (!orbiting) {
            boolean moving = Math.abs(yawVelocity) > ORBIT_VELOCITY_EPSILON
                || Math.abs(pitchVelocity) > ORBIT_VELOCITY_EPSILON;
            if (moving) {
                float pitch = pitchVelocity;
                float currentPitch = pitchDegrees();
                if ((currentPitch >= MAX_PITCH && pitch > 0f)
                    || (currentPitch <= MIN_PITCH && pitch < 0f)) {
                    // Pressed against the pitch limit: stop pitching, keep yawing.
                    pitch = 0f;
                    pitchVelocity = 0f;
                }
                orbit(yawVelocity * delta, pitch * delta);
                float decay = (float) Math.exp(-ORBIT_DECAY * delta);
                yawVelocity *= decay;
                pitchVelocity *= decay;
            } else {
                yawVelocity = 0f;
                pitchVelocity = 0f;
            }
        }
        if (!panning) {
            if (panVelocity.len2() > PAN_VELOCITY_EPSILON * PAN_VELOCITY_EPSILON) {
                moveCameraAndPivot(tmpMove.set(panVelocity).scl(delta));
                panVelocity.scl((float) Math.exp(-PAN_DECAY * delta));
            } else {
                panVelocity.setZero();
            }
        }
    }

    /**
     * Moves the orbit distance exponentially towards the zoom target, keeping the
     * pivot in place.
     */
    private void updateZoom(float delta) {
        if (orbitDistance == targetOrbitDistance) {
            return;
        }
        orbitDistance += (targetOrbitDistance - orbitDistance)
            * (1f - (float) Math.exp(-ZOOM_RATE * delta));
        if (Math.abs(targetOrbitDistance - orbitDistance) < ZOOM_EPSILON) {
            orbitDistance = targetOrbitDistance;
        }
        tmpDirection.set(camera.direction).nor();
        camera.position.set(pivot).mulAdd(tmpDirection, -orbitDistance);
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
