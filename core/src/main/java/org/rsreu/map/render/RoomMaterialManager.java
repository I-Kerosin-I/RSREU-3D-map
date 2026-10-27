package org.rsreu.map.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;

import java.util.regex.Pattern;

/**
 * Applies opacity / color states to the nodes of a building model.
 * <p>
 * The building is organised as floor containers (nodes named {@code floorN})
 * that own the walls, floor slab and edge wireframe of that floor, plus a
 * {@code main_campus} shell node and invisible {@code *_hitbox} cubes used for
 * ray picking. This manager changes the material of every controlled part so a
 * floor can be shown opaque, dimmed, hidden, or highlighted.
 * <p>
 * Materials of the {@link ModelInstance} are made private copies, so the
 * underlying model and other instances are never affected.
 */
public final class RoomMaterialManager {

    private static final String TAG = "RoomMaterialManager";

    public static final String HITBOX_SUFFIX = "_hitbox";
    private static final Pattern FLOOR_PATTERN = Pattern.compile("floor\\d+");

    /** Alpha for a floor that is dimmed because another floor is hovered. */
    private static final float DIM_ALPHA = 0.15f;
    /** Color multiplier applied to a dimmed floor. */
    private static final float DIM_FACTOR = 0.3f;
    /** A hidden floor is fully transparent. */
    private static final float HIDDEN_ALPHA = 0f;
    /** Alpha of a highlighted (selected / hovered) hitbox. */
    private static final float HIGHLIGHT_ALPHA = 0.6f;
    private static final Color HIGHLIGHT_COLOR = new Color(0x2f6db2);

    private enum State {
        NORMAL, DIMMED, HIDDEN, HIGHLIGHTED
    }

    /** A set of parts that share one visual state. */
    private static final class Unit {
        final String name;
        final boolean hitbox;
        final Array<NodePart> parts = new Array<>();
        final Array<Material> originals = new Array<>();
        final Array<Color> baseColors = new Array<>();

        Unit(String name, boolean hitbox) {
            this.name = name;
            this.hitbox = hitbox;
        }
    }

    private final Array<Unit> units = new Array<>();

    /**
     * Builds the manager from the root nodes of a model instance. The nodes are
     * classified into floor units, a campus (shell) unit and per-hitbox units.
     *
     * @param instance the building model, not null
     */
    public RoomMaterialManager(ModelInstance instance) {
        collectUnits(instance.nodes);
    }

    /**
     * Restores every managed part to its original material.
     */
    public void resetAll() {
        for (Unit unit : units) {
            applyState(unit, State.NORMAL);
        }
    }

    /**
     * Floor isolation on hover: the hovered floor stays opaque, every other floor
     * and the outer shell become dimmed, hitboxes stay invisible.
     *
     * @param floorNodeName node name of the hovered floor, e.g. {@code "floor2"}
     */
    public void applyHover(String floorNodeName) {
        for (Unit unit : units) {
            State state;
            if (unit.hitbox) {
                state = State.NORMAL;
            } else if (isFloor(unit.name)) {
                state = unit.name.equals(floorNodeName) ? State.NORMAL : State.DIMMED;
            } else {
                state = State.DIMMED; // shell / edges
            }
            applyState(unit, state);
        }
    }

    /**
     * Locks a floor selection: the selected floor stays opaque, all other floors
     * and the shell become fully hidden, hitboxes stay invisible.
     *
     * @param floorNodeName node name of the selected floor
     */
    public void selectFloor(String floorNodeName) {
        for (Unit unit : units) {
            State state;
            if (unit.hitbox) {
                state = State.NORMAL;
            } else if (isFloor(unit.name)) {
                state = unit.name.equals(floorNodeName) ? State.NORMAL : State.HIDDEN;
            } else {
                state = State.HIDDEN; // shell / edges
            }
            applyState(unit, state);
        }
    }

    /**
     * Shows or hides a hitbox highlight. Serves as the visual indicator of the
     * hovered / selected room. The floor the hitbox belongs to is unaffected.
     *
     * @param hitboxNodeName node name of the hitbox
     * @param highlighted     true to show the accent highlight
     */
    public void setHitboxHighlighted(String hitboxNodeName, boolean highlighted) {
        for (Unit unit : units) {
            if (unit.hitbox && unit.name.equals(hitboxNodeName)) {
                applyState(unit, highlighted ? State.HIGHLIGHTED : State.NORMAL);
            }
        }
    }

    // --- internal ------------------------------------------------------------

    private void collectUnits(Array<Node> roots) {
        for (Node root : roots) {
            if (isFloor(root.id)) {
                Unit unit = new Unit(root.id, false);
                collectNodeParts(root, unit);
                units.add(unit);
            } else {
                // Shell (main_campus) and any other root-level geometry / edges.
                Unit unit = new Unit(root.id, false);
                collectNodeParts(root, unit);
                units.add(unit);
            }
        }
        collectHitboxUnits(roots);
    }

    private void collectHitboxUnits(Iterable<Node> nodes) {
        for (Node node : nodes) {
            if (isHitbox(node.id)) {
                Unit unit = new Unit(node.id, true);
                collectNodeParts(node, unit);
                units.add(unit);
            }
            if (node.hasChildren()) {
                collectHitboxUnits(node.getChildren());
            }
        }
    }

    /**
     * Adds every part of a node subtree to a unit, skipping hitbox subtrees
     * (which are owned by their own units) and giving each part a private material
     * copy so state changes never leak.
     */
    private void collectNodeParts(Node node, Unit unit) {
        for (NodePart part : node.parts) {
            if (part.material == null) {
                continue;
            }
            part.material = new Material(part.material);
            unit.parts.add(part);
            unit.originals.add(new Material(part.material));
            ColorAttribute colorAttribute = (ColorAttribute) part.material.get(PBRColorAttribute.BaseColorFactor);
            Color base = colorAttribute != null ? colorAttribute.color : null;
            unit.baseColors.add(base != null ? new Color(base) : new Color(1f, 1f, 1f, 1f));
        }
        for (Node child : node.getChildren()) {
            if (isHitbox(child.id)) {
                continue;
            }
            collectNodeParts(child, unit);
        }
    }

    private void applyState(Unit unit, State state) {
        for (int i = 0; i < unit.parts.size; i++) {
            Material material = new Material(unit.originals.get(i));
            switch (state) {
                case DIMMED -> {
                    Color base = unit.baseColors.get(i);
                    material.set(PBRColorAttribute.createBaseColorFactor(
                        new Color(base.r * DIM_FACTOR, base.g * DIM_FACTOR, base.b * DIM_FACTOR, 1f)));
                    material.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, DIM_ALPHA));
                }
                case HIDDEN ->
                    material.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, HIDDEN_ALPHA));
                case HIGHLIGHTED -> {
                    material.set(PBRColorAttribute.createBaseColorFactor(HIGHLIGHT_COLOR));
                    material.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, HIGHLIGHT_ALPHA));
                }
                case NORMAL -> { /* material is already the original snapshot */ }
            }
            unit.parts.get(i).material = material;
        }
    }

    private static boolean isFloor(String name) {
        return name != null && FLOOR_PATTERN.matcher(name).matches();
    }

    private static boolean isHitbox(String name) {
        return name != null && name.endsWith(HITBOX_SUFFIX);
    }
}
