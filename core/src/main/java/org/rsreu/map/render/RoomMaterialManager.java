package org.rsreu.map.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.utils.Array;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;

import java.util.regex.Pattern;

/**
 * Applies opacity / color states to the nodes of a building model.
 * <p>
 * The model nests everything below wrapper nodes (e.g. {@code Scene Collection
 * -> Main}), so floor containers (nodes named {@code floorN}, wherever they sit
 * in the tree) are discovered recursively; each floor unit owns the whole
 * subtree of its container except hitboxes. All remaining geometry — the outer
 * shell, its wireframe and stray root parts — forms a single shell unit.
 * Invisible {@code *_hitbox} cubes used for ray picking become units of their
 * own.
 * <p>
 * States do not snap into place: every unit keeps its current opacity and color
 * and approaches the target of the last applied state exponentially, advanced by
 * {@link #update(float)} from the render loop, so dimming, hiding and highlights
 * all fade smoothly.
 * <p>
 * Visibility is only partly a material question. A translucent surface must not
 * write depth (it would occlude the geometry behind it), so animated materials
 * carry a {@link DepthTestAttribute} whose depth mask follows the opacity. And
 * geometry that is fully invisible — hitboxes that are not highlighted, floors
 * hidden by a selection — is taken out of rendering entirely by clearing
 * {@link NodePart#enabled}, because the hitbox materials of the GLB are opaque.
 * <p>
 * Materials of the {@link ModelInstance} are made private copies, so the
 * underlying model and other instances are never affected.
 */
public final class RoomMaterialManager {

    public static final String HITBOX_SUFFIX = "_hitbox";
    private static final Pattern FLOOR_PATTERN = Pattern.compile("floor\\d+");

    /** Name of the unit that groups everything that is not a floor or a hitbox. */
    private static final String SHELL_UNIT_NAME = "shell";

    /** Alpha for a floor that is dimmed because another floor is hovered. */
    private static final float DIM_ALPHA = 0.15f;
    /** Color multiplier applied to a dimmed floor. */
    private static final float DIM_FACTOR = 0.3f;
    /** A hidden floor is fully transparent. */
    private static final float HIDDEN_ALPHA = 0f;
    /** Alpha of a highlighted (selected / hovered) hitbox. */
    private static final float HIGHLIGHT_ALPHA = 0.6f;
    private static final Color HIGHLIGHT_COLOR = new Color(0x2f6db2);

    /** Exponential approach rate of the animated opacity and color, 1/s. */
    private static final float ANIMATION_RATE = 1f;
    /** Remaining difference below which an animated value snaps to its target. */
    private static final float ANIMATION_EPSILON = 0.004f;

    private enum State {
        NORMAL, DIMMED, HIDDEN, HIGHLIGHTED
    }

    /**
     * A set of parts that share one visual state. The state is described by
     * three animated scalars: overall alpha, color dimming factor and the amount
     * of the highlight color blended over the base color.
     */
    private static final class Unit {
        final String name;
        final boolean hitbox;
        final Array<NodePart> parts = new Array<>();
        final Array<Material> originals = new Array<>();
        final Array<Color> baseColors = new Array<>();
        /** Working materials with animatable attributes, created on first use. */
        final Array<Material> animated = new Array<>();

        float alpha;
        float dim = 1f;
        float highlight = 0f;
        float targetAlpha;
        float targetDim = 1f;
        float targetHighlight = 0f;
        boolean animating = false;

        Unit(String name, boolean hitbox) {
            this.name = name;
            this.hitbox = hitbox;
            // A resting hitbox renders nothing at all, so its neutral alpha is 0.
            alpha = hitbox ? HIDDEN_ALPHA : 1f;
            targetAlpha = alpha;
        }
    }

    private final Array<Unit> units = new Array<>();

    /**
     * Builds the manager from the root nodes of a model instance. The nodes are
     * classified into floor units, a shell unit and per-hitbox units; hitbox
     * parts start disabled so they never render unless highlighted.
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
            setTarget(unit, State.NORMAL);
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
            setTarget(unit, state);
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
            setTarget(unit, state);
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
                setTarget(unit, highlighted ? State.HIGHLIGHTED : State.NORMAL);
            }
        }
    }

    /**
     * Advances the opacity / color animation of every unit that has not reached
     * its target state yet. Call once per frame before rendering.
     *
     * @param deltaTime seconds elapsed since the last call
     */
    public void update(float deltaTime) {
        if (deltaTime <= 0f) {
            return;
        }
        float approach = 1f - (float) Math.exp(-ANIMATION_RATE * deltaTime);
        for (Unit unit : units) {
            if (!unit.animating) {
                continue;
            }
            unit.alpha += (unit.targetAlpha - unit.alpha) * approach;
            unit.dim += (unit.targetDim - unit.dim) * approach;
            unit.highlight += (unit.targetHighlight - unit.highlight) * approach;

            boolean settled = Math.abs(unit.targetAlpha - unit.alpha) < ANIMATION_EPSILON
                && Math.abs(unit.targetDim - unit.dim) < ANIMATION_EPSILON
                && Math.abs(unit.targetHighlight - unit.highlight) < ANIMATION_EPSILON;
            if (settled) {
                unit.alpha = unit.targetAlpha;
                unit.dim = unit.targetDim;
                unit.highlight = unit.targetHighlight;
                unit.animating = false;
            }
            if (!unit.animating && !unit.hitbox && isRestNormal(unit)) {
                restoreOriginals(unit);
            } else {
                writeAnimatedState(unit);
                if (!unit.animating && unit.targetAlpha <= 0f && unit.alpha <= 0f) {
                    // Invisible and staying that way: take it out of rendering.
                    setPartsEnabled(unit, false);
                }
            }
        }
    }

    // --- unit collection -----------------------------------------------------

    private void collectUnits(Array<Node> roots) {
        collectFloorUnits(roots);
        collectShellUnit(roots);
        collectHitboxUnits(roots);
        // Hitboxes pick rooms but must never render on their own: their GLB
        // materials can be fully opaque, so visibility is the enabled flag.
        for (Unit unit : units) {
            if (unit.hitbox) {
                setPartsEnabled(unit, false);
            }
        }
    }

    /** Creates a unit per floor container node, wherever it sits in the tree. */
    private void collectFloorUnits(Iterable<Node> nodes) {
        for (Node node : nodes) {
            if (isFloor(node.id)) {
                Unit unit = new Unit(node.id, false);
                collectNodeParts(node, unit);
                units.add(unit);
                // The floor subtree is owned by this unit; do not descend further.
            } else if (node.hasChildren()) {
                collectFloorUnits(node.getChildren());
            }
        }
    }

    /** Groups every part that belongs neither to a floor nor to a hitbox. */
    private void collectShellUnit(Iterable<Node> nodes) {
        Unit unit = new Unit(SHELL_UNIT_NAME, false);
        collectShellParts(nodes, unit);
        if (unit.parts.size > 0) {
            units.add(unit);
        }
    }

    private void collectShellParts(Iterable<Node> nodes, Unit unit) {
        for (Node node : nodes) {
            if (isFloor(node.id)) {
                continue; // owned by the floor unit
            }
            if (!isHitbox(node.id)) {
                for (NodePart part : node.parts) {
                    takePart(part, unit);
                }
            }
            if (node.hasChildren()) {
                collectShellParts(node.getChildren(), unit);
            }
        }
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
     * (which are owned by their own units).
     */
    private void collectNodeParts(Node node, Unit unit) {
        for (NodePart part : node.parts) {
            takePart(part, unit);
        }
        for (Node child : node.getChildren()) {
            if (isHitbox(child.id)) {
                continue;
            }
            collectNodeParts(child, unit);
        }
    }

    /**
     * Adds a part to a unit and gives it a private material copy so state
     * changes never leak into the model or other instances.
     */
    private void takePart(NodePart part, Unit unit) {
        if (part.material == null) {
            return;
        }
        part.material = new Material(part.material);
        unit.parts.add(part);
        unit.originals.add(new Material(part.material));
        ColorAttribute colorAttribute = (ColorAttribute) part.material.get(PBRColorAttribute.BaseColorFactor);
        Color base = colorAttribute != null ? colorAttribute.color : null;
        unit.baseColors.add(base != null ? new Color(base) : new Color(1f, 1f, 1f, 1f));
    }

    // --- state animation -----------------------------------------------------

    /**
     * Sets the target values of the animated parameters for a state and starts
     * the animation if the target actually changed.
     */
    private void setTarget(Unit unit, State state) {
        float alpha;
        float dim = 1f;
        float highlight = 0f;
        switch (state) {
            case NORMAL -> alpha = unit.hitbox ? HIDDEN_ALPHA : 1f;
            case DIMMED -> {
                alpha = DIM_ALPHA;
                dim = DIM_FACTOR;
            }
            case HIDDEN -> alpha = HIDDEN_ALPHA;
            case HIGHLIGHTED -> {
                alpha = HIGHLIGHT_ALPHA;
                highlight = 1f;
            }
            default -> alpha = 1f;
        }
        if (unit.targetAlpha == alpha && unit.targetDim == dim && unit.targetHighlight == highlight) {
            return;
        }
        unit.targetAlpha = alpha;
        unit.targetDim = dim;
        unit.targetHighlight = highlight;
        if (alpha > 0f || highlight > 0f) {
            // A visible target must render while the animation approaches it.
            setPartsEnabled(unit, true);
        }
        unit.animating = true;
    }

    /** True when the unit rests in the NORMAL state and can drop its working material. */
    private boolean isRestNormal(Unit unit) {
        return unit.targetAlpha == (unit.hitbox ? HIDDEN_ALPHA : 1f)
            && unit.targetDim == 1f
            && unit.targetHighlight == 0f;
    }

    /** Swaps every part back to a pristine copy of its original material. */
    private void restoreOriginals(Unit unit) {
        for (int i = 0; i < unit.parts.size; i++) {
            NodePart part = unit.parts.get(i);
            part.material = new Material(unit.originals.get(i));
            part.enabled = true;
        }
    }

    /**
     * Writes the current animated values into the working materials, creating
     * them on first use.
     */
    private void writeAnimatedState(Unit unit) {
        for (int i = 0; i < unit.parts.size; i++) {
            Material material = animatedMaterial(unit, i);
            Color base = unit.baseColors.get(i);
            ColorAttribute color = (ColorAttribute) material.get(PBRColorAttribute.BaseColorFactor);
            color.color.set(
                lerp(base.r * unit.dim, HIGHLIGHT_COLOR.r, unit.highlight),
                lerp(base.g * unit.dim, HIGHLIGHT_COLOR.g, unit.highlight),
                lerp(base.b * unit.dim, HIGHLIGHT_COLOR.b, unit.highlight),
                1f);
            ((BlendingAttribute) material.get(BlendingAttribute.Type)).opacity = unit.alpha;
            // A translucent surface must not write depth, or it occludes the
            // geometry behind it; a fully opaque one keeps the depth writes.
            ((DepthTestAttribute) material.get(DepthTestAttribute.Type)).depthMask = unit.alpha >= 1f;
        }
    }

    /**
     * Returns the working material of a part and makes it the part's current
     * material, creating the working copy from the original snapshot on first
     * use. Fresh attribute instances replace the shared ones, because the
     * working material is mutated in place every frame.
     */
    private Material animatedMaterial(Unit unit, int index) {
        Material material;
        if (index < unit.animated.size) {
            material = unit.animated.get(index);
        } else {
            material = new Material(unit.originals.get(index));
            material.set(PBRColorAttribute.createBaseColorFactor(new Color(unit.baseColors.get(index))));
            material.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, unit.alpha));
            material.set(new DepthTestAttribute(GL20.GL_LEQUAL, true));
            unit.animated.add(material);
        }
        unit.parts.get(index).material = material;
        return material;
    }

    private void setPartsEnabled(Unit unit, boolean enabled) {
        for (int i = 0; i < unit.parts.size; i++) {
            unit.parts.get(i).enabled = enabled;
        }
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private static boolean isFloor(String name) {
        return name != null && FLOOR_PATTERN.matcher(name).matches();
    }

    private static boolean isHitbox(String name) {
        return name != null && name.endsWith(HITBOX_SUFFIX);
    }
}
