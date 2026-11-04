package org.rsreu.map.render;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.utils.RenderableSorter;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import java.util.Comparator;

/**
 * Orders body fills, wireframes and legacy shell/overlay materials by render
 * phase. Whole-mesh sorting cannot provide per-triangle shell self-occlusion;
 * {@link BuildingSceneManager} handles that with separate depth/color passes.
 * The material maps remain useful for grouping renderables without relying on
 * hashes of mutable color and opacity attributes.
 * <p>
 * The phase order:
 * <ol>
 *   <li>opaque fills (front to back, depth written);</li>
 *   <li>opaque wireframes (after their coplanar fills);</li>
 *   <li>blended body fills, then their wireframes (both back to front);</li>
 *   <li>blended outer-shell fills, then their wireframes (both back to front);</li>
 *   <li>the highlighted room hitbox — a depth-test-free x-ray marker.</li>
 * </ol>
 * <p>
 * A transparent interior wireframe is therefore composited before the shell
 * fill and is attenuated by it, while the shell's own accent lines remain on
 * top of its surface. This removes reliance on tied mesh centers or a large
 * polygon offset for the line-overlay order.
 * Membership in the shell and overlay layers is decided by material identity:
 * {@link RoomMaterialManager} registers every material it hands out for those
 * units in the maps returned by {@link #shellMaterials()} and
 * {@link #overlayMaterials()}.
 */
public final class BuildingRenderableSorter implements RenderableSorter, Comparator<Renderable> {

    private static final int OPAQUE_FILL = 0;
    private static final int OPAQUE_WIREFRAME = 1;
    private static final int BLENDED_BODY_FILL = 2;
    private static final int BLENDED_BODY_WIREFRAME = 3;
    private static final int BLENDED_SHELL_FILL = 4;
    private static final int BLENDED_SHELL_WIREFRAME = 5;
    private static final int HITBOX_OVERLAY = 6;

    /**
     * Layer per working material, keyed by object identity. A {@code Material}'s
     * own {@code equals} compares attribute values, which the material manager
     * mutates every frame, so any hash-based set would be unreliable here.
     */
    private final java.util.IdentityHashMap<Material, Integer> shellMaterials = new java.util.IdentityHashMap<>();
    private final java.util.IdentityHashMap<Material, Integer> overlayMaterials = new java.util.IdentityHashMap<>();

    private Camera camera;
    private final Vector3 tmpPosition = new Vector3();

    /** Materials whose renderables belong to the outer shell (drawn last over the interior). */
    public java.util.IdentityHashMap<Material, Integer> shellMaterials() {
        return shellMaterials;
    }

    /** Materials whose renderables are x-ray overlays (drawn after everything else). */
    public java.util.IdentityHashMap<Material, Integer> overlayMaterials() {
        return overlayMaterials;
    }

    @Override
    public void sort(Camera camera, Array<Renderable> renderables) {
        this.camera = camera;
        renderables.sort(this);
    }

    @Override
    public int compare(Renderable a, Renderable b) {
        int phaseA = phase(a);
        int phaseB = phase(b);
        if (phaseA != phaseB) {
            return Integer.compare(phaseA, phaseB);
        }
        if (phaseA == OPAQUE_FILL) {
            // Opaque fills write depth, so front-to-back rejects hidden fragments.
            return Float.compare(distance2(a), distance2(b));
        }
        // Lines do not write depth, and transparent primitives must composite
        // from far to near within one layer.
        return Float.compare(distance2(b), distance2(a));
    }

    private int phase(Renderable renderable) {
        if (isOverlay(renderable)) {
            return HITBOX_OVERLAY;
        }
        boolean shell = isShell(renderable);
        boolean line = isLine(renderable);
        if (!isBlended(renderable)) {
            return line ? OPAQUE_WIREFRAME : OPAQUE_FILL;
        }
        if (shell) {
            return line ? BLENDED_SHELL_WIREFRAME : BLENDED_SHELL_FILL;
        }
        return line ? BLENDED_BODY_WIREFRAME : BLENDED_BODY_FILL;
    }

    private boolean isShell(Renderable renderable) {
        return renderable.material != null && shellMaterials.containsKey(renderable.material);
    }

    private boolean isOverlay(Renderable renderable) {
        return renderable.material != null && overlayMaterials.containsKey(renderable.material);
    }

    private static boolean isLine(Renderable renderable) {
        int primitiveType = renderable.meshPart.primitiveType;
        return primitiveType == GL20.GL_LINES
            || primitiveType == GL20.GL_LINE_LOOP
            || primitiveType == GL20.GL_LINE_STRIP;
    }

    private static boolean isBlended(Renderable renderable) {
        Material material = renderable.material;
        if (material == null || !material.has(BlendingAttribute.Type)) {
            return false;
        }
        return ((BlendingAttribute) material.get(BlendingAttribute.Type)).blended;
    }

    /** Squared distance from the camera to the world position of the mesh part center. */
    private float distance2(Renderable renderable) {
        Vector3 center = renderable.meshPart.center;
        if (center.isZero()) {
            renderable.worldTransform.getTranslation(tmpPosition);
        } else {
            tmpPosition.set(center).mul(renderable.worldTransform);
        }
        return camera.position.dst2(tmpPosition);
    }
}
