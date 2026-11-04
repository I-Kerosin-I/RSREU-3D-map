package org.rsreu.map.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;

/**
 * Floors and shell each use nearest-surface depth to reject their own hidden
 * surfaces and wireframes. A fading floor's temporary depth is removed before
 * the next floor, preserving the colors of all translucent layers. Opaque
 * foreground depth is replayed after each clear. All passes retain backbuffer
 * MSAA; the shell sequence and x-ray room overlays follow the floor colors.
 */
public final class BuildingSceneManager extends SceneManager {

    private BuildingRenderPasses renderPasses;

    public BuildingSceneManager(BuildingRenderableSorter sorter) {
        super(new BuildingShaderProvider(24), PBRShaderProvider.createDefaultDepth(24), sorter);
    }

    public void setRenderPasses(BuildingRenderPasses renderPasses) {
        this.renderPasses = renderPasses;
    }

    @Override
    public void renderColors() {
        if (renderPasses == null) {
            super.renderColors();
            return;
        }
        try {
            renderDepthPass(renderPasses.opaqueDepth());
            // Alpha-one color copies still carry BlendingAttribute. Keep lines
            // in a separate batch so sorting cannot move them before the fills.
            renderPass(renderPasses.opaqueColor());
            renderPass(renderPasses.opaqueLines());
            if (getSkyBox() != null) {
                renderPass(getSkyBox());
            }

            boolean temporaryFloorDepth = false;
            for (BuildingRenderPasses.FloorPasses floor : renderPasses.translucentFloors(camera)) {
                restoreOpaqueDepth();
                renderDepthPass(floor.depth());
                renderPass(floor.color());
                renderPass(floor.lines());
                temporaryFloorDepth = true;
            }
            if (temporaryFloorDepth) {
                // The last fading floor is not an opaque shell occluder. Keep
                // all accumulated color, but rebuild only opaque foreground depth.
                restoreOpaqueDepth();
            }

            // Opaque foreground remains a depth occluder, including from inside.
            // Interior colors are already available for the shell's blending.
            renderDepthPass(renderPasses.shellDepth());
            renderPass(renderPasses.shellColor());
            renderPass(renderPasses.shellLines());
            renderPass(renderPasses.overlays());
        } finally {
            Gdx.gl.glColorMask(true, true, true, true);
            Gdx.gl.glDepthMask(true);
        }
    }

    private void restoreOpaqueDepth() {
        // Clears run outside a batch and affect depth only, never accumulated color.
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
        renderDepthPass(renderPasses.opaqueDepth());
    }

    private void renderDepthPass(RenderableProvider provider) {
        Gdx.gl.glColorMask(false, false, false, false);
        try {
            renderPass(provider);
        } finally {
            Gdx.gl.glColorMask(true, true, true, true);
        }
    }

    private void renderPass(RenderableProvider provider) {
        ModelBatch batch = getBatch();
        batch.begin(camera);
        try {
            batch.render(provider, computedEnvironement);
        } finally {
            batch.end();
        }
    }
}
