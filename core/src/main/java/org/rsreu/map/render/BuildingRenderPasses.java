package org.rsreu.map.render;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pool;

/**
 * Stable part groups for floors, shell and room-overlay rendering. Membership
 * follows node parts rather than their changing animated/original materials.
 * Each floor owns its nearest-surface depth pass so one translucent floor does
 * not become an opaque depth occluder for the other translucent floors.
 */
public final class BuildingRenderPasses {

    private enum Pass {
        SOURCE, DEPTH, COLOR
    }

    private static final class Part {
        final Node node;
        final NodePart part;
        Material sourceMaterial;
        long sourceMask;
        Material depthMaterial;
        Material colorMaterial;

        Part(Node node, NodePart part) {
            this.node = node;
            this.part = part;
        }

        /** Matching PBR vertex paths avoid depth differences at GL_EQUAL. */
        Material surfaceMaterial(boolean depthOnly) {
            Material source = part.material;
            if (source != sourceMaterial || source.getMask() != sourceMask) {
                sourceMaterial = source;
                sourceMask = source.getMask();
                depthMaterial = new Material(source);
                colorMaterial = new Material(source);
                depthMaterial.set(new BlendingAttribute(GL20.GL_SRC_ALPHA,
                    GL20.GL_ONE_MINUS_SRC_ALPHA, 1f));
                colorMaterial.set(new BlendingAttribute(GL20.GL_SRC_ALPHA,
                    GL20.GL_ONE_MINUS_SRC_ALPHA, 1f));
                depthMaterial.set(new DepthTestAttribute(GL20.GL_LEQUAL, true));
                colorMaterial.set(new DepthTestAttribute(GL20.GL_EQUAL, false));
            }
            Material material = depthOnly ? depthMaterial : colorMaterial;
            // Colors change in place; keep cached pass copies in sync.
            for (Attribute attribute : source) {
                if (attribute instanceof ColorAttribute color) {
                    ((ColorAttribute) material.get(attribute.type)).color.set(color.color);
                }
            }
            ((BlendingAttribute) material.get(BlendingAttribute.Type)).opacity =
                depthOnly ? 1f : opacity(part);
            return material;
        }
    }

    /** One floor container, including any separately modelled walls and slab. */
    public final class FloorPasses {
        private final String name;
        private final int index;
        private final Array<Part> fills = new Array<>();
        private final Array<Part> lines = new Array<>();
        private final RenderableProvider depth = new PartProvider(fills, Pass.DEPTH, false);
        private final RenderableProvider color = new PartProvider(fills, Pass.COLOR, false);
        private final RenderableProvider wireframe = new PartProvider(lines, Pass.SOURCE, false);
        private final BoundingBox bounds = new BoundingBox();
        private final Matrix4 transform = new Matrix4();
        private final Vector3 point = new Vector3();
        private float viewDepth;

        FloorPasses(String name, int index) {
            this.name = name;
            this.index = index;
        }

        public String name() {
            return name;
        }

        public RenderableProvider depth() {
            return depth;
        }

        public RenderableProvider color() {
            return color;
        }

        public RenderableProvider lines() {
            return wireframe;
        }

        private boolean isTranslucent() {
            for (Part binding : fills) {
                float alpha = opacity(binding.part);
                if (alpha > 0f && alpha < 1f) {
                    return true;
                }
            }
            return false;
        }

        private void updateViewDepth(Camera camera) {
            bounds.inf();
            for (Part binding : fills) {
                if (opacity(binding.part) <= 0f) {
                    continue;
                }
                transform.set(instance.transform).mul(binding.node.globalTransform);
                Vector3 center = binding.part.meshPart.center;
                Vector3 half = binding.part.meshPart.halfExtents;
                for (int corner = 0; corner < 8; corner++) {
                    point.set(center.x + ((corner & 1) == 0 ? -half.x : half.x),
                        center.y + ((corner & 2) == 0 ? -half.y : half.y),
                        center.z + ((corner & 4) == 0 ? -half.z : half.z));
                    bounds.ext(point.mul(transform));
                }
            }
            bounds.getCenter(point);
            viewDepth = point.sub(camera.position).dot(camera.direction);
        }
    }

    private final ModelInstance instance;
    private final ObjectMap<String, FloorPasses> floorsByName = new ObjectMap<>();
    private final Array<FloorPasses> floors = new Array<>();
    private final Array<FloorPasses> translucentFloors = new Array<>();
    private final Array<Part> floorFills = new Array<>();
    private final Array<Part> floorLines = new Array<>();
    private final Array<Part> shellFillParts = new Array<>();
    private final Array<Part> shellLineParts = new Array<>();
    private final Array<Part> overlayParts = new Array<>();

    private final RenderableProvider opaqueDepth = new PartProvider(floorFills, Pass.DEPTH, true);
    private final RenderableProvider opaqueColor = new PartProvider(floorFills, Pass.COLOR, true);
    private final RenderableProvider opaqueLines = new PartProvider(floorLines, Pass.SOURCE, true);
    private final RenderableProvider shellDepth = new PartProvider(shellFillParts, Pass.DEPTH, false);
    private final RenderableProvider shellColor = new PartProvider(shellFillParts, Pass.COLOR, false);
    private final RenderableProvider shellLines = new PartProvider(shellLineParts, Pass.SOURCE, false);
    private final RenderableProvider overlays = new PartProvider(overlayParts, Pass.SOURCE, false);

    public BuildingRenderPasses(ModelInstance instance) {
        this.instance = instance;
    }

    /** Ownership comes from the floor unit, not from a descendant mesh's name. */
    void add(Node node, NodePart part, String unitName, boolean shell, boolean hitbox) {
        Part binding = new Part(node, part);
        if (hitbox) {
            overlayParts.add(binding);
        } else if (shell) {
            (isLine(part) ? shellLineParts : shellFillParts).add(binding);
        } else {
            FloorPasses floor = floorsByName.get(unitName);
            if (floor == null) {
                floor = new FloorPasses(unitName, floors.size);
                floorsByName.put(unitName, floor);
                floors.add(floor);
            }
            (isLine(part) ? floor.lines : floor.fills).add(binding);
            (isLine(part) ? floorLines : floorFills).add(binding);
        }
    }

    public FloorPasses floor(String name) {
        return floorsByName.get(name);
    }

    /** Current translucent groups, sorted along the camera view, far to near. */
    public Array<FloorPasses> translucentFloors(Camera camera) {
        translucentFloors.clear();
        for (FloorPasses floor : floors) {
            if (floor.isTranslucent()) {
                floor.updateViewDepth(camera);
                translucentFloors.add(floor);
            }
        }
        translucentFloors.sort((a, b) -> {
            int depthOrder = Float.compare(b.viewDepth, a.viewDepth);
            return depthOrder != 0 ? depthOrder : Integer.compare(a.index, b.index);
        });
        return translucentFloors;
    }

    public RenderableProvider opaqueDepth() {
        return opaqueDepth;
    }

    public RenderableProvider opaqueColor() {
        return opaqueColor;
    }

    public RenderableProvider opaqueLines() {
        return opaqueLines;
    }

    public RenderableProvider shellDepth() {
        return shellDepth;
    }

    public RenderableProvider shellColor() {
        return shellColor;
    }

    public RenderableProvider shellLines() {
        return shellLines;
    }

    public RenderableProvider overlays() {
        return overlays;
    }

    private final class PartProvider implements RenderableProvider {
        private final Array<Part> parts;
        private final Pass pass;
        private final boolean opaqueOnly;

        PartProvider(Array<Part> parts, Pass pass, boolean opaqueOnly) {
            this.parts = parts;
            this.pass = pass;
            this.opaqueOnly = opaqueOnly;
        }

        @Override
        public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
            for (Part binding : parts) {
                float alpha = opacity(binding.part);
                if (alpha <= 0f || (opaqueOnly && alpha < 1f)) {
                    continue;
                }
                Renderable renderable = instance.getRenderable(pool.obtain(), binding.node, binding.part);
                if (pass != Pass.SOURCE) {
                    renderable.material = binding.surfaceMaterial(pass == Pass.DEPTH);
                }
                renderables.add(renderable);
            }
        }
    }

    private static float opacity(NodePart part) {
        if (!part.enabled || part.material == null) {
            return 0f;
        }
        BlendingAttribute blending = (BlendingAttribute) part.material.get(BlendingAttribute.Type);
        return blending == null || !blending.blended ? 1f : blending.opacity;
    }

    private static boolean isLine(NodePart part) {
        int primitiveType = part.meshPart.primitiveType;
        return primitiveType == GL20.GL_LINES
            || primitiveType == GL20.GL_LINE_LOOP
            || primitiveType == GL20.GL_LINE_STRIP;
    }
}
