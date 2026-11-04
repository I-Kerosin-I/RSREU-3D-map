package org.rsreu.map.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;

/** CPU-only regression checks; run main after Maven test-compile. */
public final class BuildingRenderPassesTest {

    private final Pool<Renderable> pool = new Pool<>() {
        @Override
        protected Renderable newObject() {
            return new Renderable();
        }
    };

    public static void main(String[] args) {
        new BuildingRenderPassesTest().checkPasses();
        System.out.println("BuildingRenderPassesTest: all checks passed");
    }

    private void checkPasses() {
        ModelInstance instance = new ModelInstance(new Model());
        Node root = node("Main", null);
        Node shell = node("main", GL20.GL_TRIANGLES);
        Node shellEdges = node("main_Edges", GL20.GL_LINES);
        Node floor = node("floor3", null);
        Node floorMesh = node("floor3", GL20.GL_TRIANGLES);
        Node floorEdges = node("floor3_Edges", GL20.GL_LINES);
        Node hitbox = node("main_room_301_hitbox", GL20.GL_TRIANGLES);
        root.addChild(shell);
        root.addChild(shellEdges);
        root.addChild(floor);
        floor.addChild(floorMesh);
        floor.addChild(floorEdges);
        floor.addChild(hitbox);
        instance.nodes.add(root);
        instance.calculateTransforms();

        RoomMaterialManager manager = new RoomMaterialManager(instance, new BuildingRenderableSorter());
        BuildingRenderPasses passes = manager.renderPasses();
        NodePart shellPart = shell.parts.first();
        Material original = shellPart.material;
        require(count(passes.body()) == 2, "body contains only floor fill and edges");
        require(count(passes.shellDepth()) == 1, "shell prepass contains only shell fill");
        require(count(passes.shellColor()) == 1, "shell color contains only shell fill");
        require(count(passes.shellLines()) == 1, "shell lines are separate");
        require(count(passes.overlays()) == 0, "normal hitboxes are not submitted");
        require(!depth(floorEdges.parts.first().material).depthMask, "floor edges do not write depth");
        checkShellMaterials(passes, shellPart, 1f);

        manager.applyHover("floor3");
        manager.setHitboxHighlighted(hitbox.id, true);
        manager.update(0.1f);
        float opacity = blend(shellPart.material).opacity;
        require(opacity > 0.15f && opacity < 1f, "first transition frame is not snapped");
        checkShellMaterials(passes, shellPart, opacity);
        Renderable marker = first(passes.overlays());
        require(depth(marker.material).depthFunc == GL20.GL_ALWAYS, "hitbox keeps x-ray depth test");
        require(!depth(marker.material).depthMask, "hitbox cannot occlude shell");
        require(count(passes.shellDepth()) == 1, "highlight cannot enter shell depth pass");

        Material cachedColor = first(passes.shellColor()).material;
        Material cachedDepth = first(passes.shellDepth()).material;
        manager.update(0.1f);
        require(first(passes.shellColor()).material == cachedColor, "color copy reused during fade");
        require(first(passes.shellDepth()).material == cachedDepth, "depth copy reused during fade");
        checkShellMaterials(passes, shellPart, blend(shellPart.material).opacity);

        manager.resetAll();
        manager.update(100f);
        require(shellPart.material != original, "restoration uses a private original snapshot");
        require(!shellPart.material.has(BlendingAttribute.Type), "normal source is restored immediately");
        checkShellMaterials(passes, shellPart, 1f);
        require(count(passes.shellDepth()) == 1 && count(passes.body()) == 2,
            "material restoration cannot move shell to body");
        require(count(passes.overlays()) == 0, "faded hitbox is disabled");

        manager.selectFloor("floor3");
        manager.update(100f);
        require(count(passes.shellDepth()) == 0, "hidden shell writes no depth");
        require(count(passes.shellColor()) == 0 && count(passes.shellLines()) == 0,
            "hidden shell has no color or wireframe");
        require(count(passes.body()) == 2, "selected floor remains rendered");
        manager.resetAll();
        manager.update(0.1f);
        checkShellMaterials(passes, shellPart, blend(shellPart.material).opacity);
        require(count(passes.shellLines()) == 1, "shell edges reappear on reverse fade");

        // A source may gain an attribute without changing its object identity.
        shellPart.material.set(ColorAttribute.createEmissive(Color.RED));
        checkShellMaterials(passes, shellPart, blend(shellPart.material).opacity);
        require(first(passes.shellColor()).material.has(ColorAttribute.Emissive),
            "cached materials refresh when source attribute mask changes");
    }

    private void checkShellMaterials(BuildingRenderPasses passes, NodePart shellPart, float opacity) {
        Material source = shellPart.material;
        Material prepass = first(passes.shellDepth()).material;
        Material color = first(passes.shellColor()).material;
        require(prepass != source && color != source && prepass != color, "pass materials are private");
        require(prepass.getMask() == color.getMask(), "depth and color use matching shader features");
        require(depth(prepass).depthFunc == GL20.GL_LEQUAL && depth(prepass).depthMask,
            "shell prepass writes nearest depth");
        require(depth(color).depthFunc == GL20.GL_EQUAL && !depth(color).depthMask,
            "shell color uses prepass equality without depth writes");
        require(blend(prepass).opacity == 1f, "shell depth never depends on fade opacity");
        require(Math.abs(blend(color).opacity - opacity) < 1e-6f, "shell color preserves animated opacity");
        Color base = ((ColorAttribute) source.get(PBRColorAttribute.BaseColorFactor)).color;
        require(((ColorAttribute) color.get(PBRColorAttribute.BaseColorFactor)).color.equals(base),
            "color copy follows in-place dim animation");
    }

    private int count(RenderableProvider provider) {
        Array<Renderable> renderables = new Array<>();
        provider.getRenderables(renderables, pool);
        int count = renderables.size;
        pool.freeAll(renderables);
        return count;
    }

    private Renderable first(RenderableProvider provider) {
        Array<Renderable> renderables = new Array<>();
        provider.getRenderables(renderables, pool);
        require(renderables.size == 1, "expected exactly one renderable");
        Renderable result = new Renderable().set(renderables.first());
        pool.freeAll(renderables);
        return result;
    }

    private static Node node(String id, Integer primitiveType) {
        Node node = new Node();
        node.id = id;
        if (primitiveType != null) {
            MeshPart mesh = new MeshPart();
            mesh.id = id;
            mesh.primitiveType = primitiveType;
            node.parts.add(new NodePart(mesh,
                new Material(PBRColorAttribute.createBaseColorFactor(Color.GRAY))));
        }
        return node;
    }

    private static DepthTestAttribute depth(Material material) {
        return (DepthTestAttribute) material.get(DepthTestAttribute.Type);
    }

    private static BlendingAttribute blend(Material material) {
        return (BlendingAttribute) material.get(BlendingAttribute.Type);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
