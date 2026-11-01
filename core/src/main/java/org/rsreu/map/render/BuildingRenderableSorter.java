package org.rsreu.map.render;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.utils.RenderableSorter;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import java.util.Comparator;

/**
 * Sorts renderables into a fixed layer order instead of trusting pure distance
 * sorting, which breaks for nested geometry: the distance sorter measures the
 * center of each mesh, and the center of the outer shell mesh is the center of
 * the whole building, so interior pieces on the camera side sort as "closer"
 * and end up composited on top of the shell. While the shell is translucent
 * they then shine through at full strength no matter how opaque it gets, and
 * when its material is restored the depth test hides them at once — the visible
 * opacity jump.
 * <p>
 * The layer order that fixes it:
 * <ol>
 *   <li>opaque geometry (front to back, depth written)</li>
 *   <li>building body layers — floors, dimmed shells — back to front</li>
 *   <li>the outer shell — its camera-facing surface is always the closest one,
 *   so drawing it last composites it correctly over everything inside</li>
 *   <li>the highlighted room hitbox — a depth-test-free x-ray marker</li>
 * </ol>
 * Membership in the shell and overlay layers is decided by material identity:
 * {@link RoomMaterialManager} registers every material it hands out for those
 * units in the maps returned by {@link #shellMaterials()} and
 * {@link #overlayMaterials()}.
 */
public final class BuildingRenderableSorter implements RenderableSorter, Comparator<Renderable> {

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
        boolean blendedA = isBlended(a);
        boolean blendedB = isBlended(b);
        if (blendedA != blendedB) {
            // Opaque geometry first: it writes depth for everything after it.
            return blendedA ? 1 : -1;
        }
        if (!blendedA) {
            // Opaque: front to back, so the depth test rejects hidden fragments.
            return Float.compare(distance2(a), distance2(b));
        }
        int layerA = layer(a);
        int layerB = layer(b);
        if (layerA != layerB) {
            return Integer.compare(layerA, layerB);
        }
        // Within a layer: back to front, so blending composes correctly.
        return Float.compare(distance2(b), distance2(a));
    }

    private int layer(Renderable renderable) {
        Material material = renderable.material;
        if (material != null) {
            if (overlayMaterials.containsKey(material)) {
                return 2;
            }
            if (shellMaterials.containsKey(material)) {
                return 1;
            }
        }
        return 0;
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
