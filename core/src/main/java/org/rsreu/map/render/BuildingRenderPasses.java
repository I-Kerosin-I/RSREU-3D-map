package org.rsreu.map.render;

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
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

/**
 * Stable part groups for body, shell and room-overlay rendering. Membership is
 * tied to the node part, not its material: a fade may replace or restore the
 * material without moving the geometry to a different render pass.
 */
public final class BuildingRenderPasses {

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

        /**
         * Both shell passes keep the same PBR shader features and vertex path.
         * Only the depth state and opacity differ; a packed-depth shader could
         * produce slightly different positions and fail the GL_EQUAL test.
         */
        Material shellMaterial(boolean depthOnly) {
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
            // The animator mutates colors in place without replacing the source.
            // Update the cached copies without allocating attributes every frame.
            for (Attribute attribute : source) {
                if (attribute instanceof ColorAttribute color) {
                    ((ColorAttribute) material.get(attribute.type)).color.set(color.color);
                }
            }
            BlendingAttribute blending = (BlendingAttribute) source.get(BlendingAttribute.Type);
            ((BlendingAttribute) material.get(BlendingAttribute.Type)).opacity =
                depthOnly || blending == null || !blending.blended ? 1f : blending.opacity;
            return material;
        }
    }

    private final ModelInstance instance;
    private final Array<Part> bodyParts = new Array<>();
    private final Array<Part> shellFillParts = new Array<>();
    private final Array<Part> shellLineParts = new Array<>();
    private final Array<Part> overlayParts = new Array<>();

    private final RenderableProvider body = new PartProvider(bodyParts, false, false);
    private final RenderableProvider shellDepth = new PartProvider(shellFillParts, true, true);
    private final RenderableProvider shellColor = new PartProvider(shellFillParts, true, false);
    private final RenderableProvider shellLines = new PartProvider(shellLineParts, false, false);
    private final RenderableProvider overlays = new PartProvider(overlayParts, false, false);

    public BuildingRenderPasses(ModelInstance instance) {
        this.instance = instance;
    }

    /** Called once by the material manager while it classifies the node tree. */
    void add(Node node, NodePart part, boolean shell, boolean hitbox) {
        Part binding = new Part(node, part);
        if (hitbox) {
            overlayParts.add(binding);
        } else if (shell) {
            (isLine(part) ? shellLineParts : shellFillParts).add(binding);
        } else {
            bodyParts.add(binding);
        }
    }

    public RenderableProvider body() {
        return body;
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
        private final boolean shellFill;
        private final boolean depthOnly;

        PartProvider(Array<Part> parts, boolean shellFill, boolean depthOnly) {
            this.parts = parts;
            this.shellFill = shellFill;
            this.depthOnly = depthOnly;
        }

        @Override
        public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
            for (Part binding : parts) {
                NodePart part = binding.part;
                if (!part.enabled || part.material == null) {
                    continue;
                }
                BlendingAttribute blending = (BlendingAttribute) part.material.get(BlendingAttribute.Type);
                if (blending != null && blending.blended && blending.opacity <= 0f) {
                    continue;
                }
                Renderable renderable = instance.getRenderable(pool.obtain(), binding.node, part);
                if (shellFill) {
                    renderable.material = binding.shellMaterial(depthOnly);
                }
                renderables.add(renderable);
            }
        }
    }

    private static boolean isLine(NodePart part) {
        int primitiveType = part.meshPart.primitiveType;
        return primitiveType == GL20.GL_LINES
            || primitiveType == GL20.GL_LINE_LOOP
            || primitiveType == GL20.GL_LINE_STRIP;
    }
}
