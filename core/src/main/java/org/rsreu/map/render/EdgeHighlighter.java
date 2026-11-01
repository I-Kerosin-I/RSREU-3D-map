package org.rsreu.map.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;

/**
 * Paints the line primitives of a scene with a single accent color.
 * <p>
 * A glTF file may carry the building surface and its edge wireframe as two
 * separate nodes: one mesh with triangles and one mesh with lines. gdx-gltf keeps
 * the primitive type of each mesh part, and shades line primitives unlit because
 * they carry no normals, so the accent color is written to the material as is.
 */
public final class EdgeHighlighter {

    private static final String TAG = "EdgeHighlighter";

    private EdgeHighlighter() {
    }

    /**
     * Recolors every line primitive of the given instance.
     * <p>
     * The materials of a {@link ModelInstance} are private copies, so this does not
     * leak into the underlying model or into other instances of it. Must be called
     * before the scene is rendered, because shaders are compiled on first use.
     *
     * @param instance scene to restyle, may be null
     * @param accent   color for the lines, may be null
     * @return number of node parts that were recolored
     */
    public static int applyAccentColor(ModelInstance instance, Color accent) {
        if (instance == null || accent == null) {
            return 0;
        }

        Array<NodePart> lineParts = new Array<>();
        // Materials of solid parts have to keep their look, so a line part may only
        // be recolored when no solid part uses the same material.
        ObjectSet<Material> solidMaterials = new ObjectSet<>();

        collectParts(instance.nodes, lineParts, solidMaterials);

        int recolored = 0;
        for (NodePart part : lineParts) {
            if (part.material == null) {
                continue;
            }
            if (solidMaterials.contains(part.material)) {
                Gdx.app.error(TAG, "line part '" + part.meshPart.id
                    + "' shares its material with a solid part, skipping");
                continue;
            }
            part.material.set(PBRColorAttribute.createBaseColorFactor(accent));
            recolored++;
        }
        return recolored;
    }

    /**
     * Counts the line primitives of the given instance.
     */
    public static int countLineParts(ModelInstance instance) {
        if (instance == null) {
            return 0;
        }
        int count = 0;
        for (Node node : instance.nodes) {
            count += countLineParts(node);
        }
        return count;
    }

    /**
     * Splits every part of the node subtree into line parts and materials of
     * solid parts. The hierarchy nests meshes below wrapper nodes, so the whole
     * tree has to be walked.
     */
    private static void collectParts(Iterable<Node> nodes, Array<NodePart> lineParts,
                                     ObjectSet<Material> solidMaterials) {
        for (Node node : nodes) {
            for (NodePart part : node.parts) {
                if (part.meshPart == null) {
                    continue;
                }
                if (isLine(part.meshPart.primitiveType)) {
                    lineParts.add(part);
                } else {
                    solidMaterials.add(part.material);
                }
            }
            if (node.hasChildren()) {
                collectParts(node.getChildren(), lineParts, solidMaterials);
            }
        }
    }

    private static int countLineParts(Node node) {
        int count = 0;
        for (NodePart part : node.parts) {
            if (part.meshPart != null && isLine(part.meshPart.primitiveType)) {
                count++;
            }
        }
        for (Node child : node.getChildren()) {
            count += countLineParts(child);
        }
        return count;
    }

    private static boolean isLine(int primitiveType) {
        return primitiveType == GL20.GL_LINES
            || primitiveType == GL20.GL_LINE_LOOP
            || primitiveType == GL20.GL_LINE_STRIP;
    }
}