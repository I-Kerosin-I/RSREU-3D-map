package org.rsreu.map.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;

/**
 * Renders the interior before recording the nearest shell surface in depth.
 * Shell colors and lines then test against that depth, preserving shell
 * self-occlusion throughout a fade without blocking the interior color behind
 * it. All passes use the existing framebuffer, including its MSAA samples.
 */
public final class BuildingSceneManager extends SceneManager {

    private BuildingRenderPasses renderPasses;

    public BuildingSceneManager(BuildingRenderableSorter sorter) {
        super(PBRShaderProvider.createDefault(24), PBRShaderProvider.createDefaultDepth(24), sorter);
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
            ModelBatch batch = getBatch();
            batch.begin(camera);
            try {
                batch.render(renderPasses.body(), computedEnvironement);
                if (getSkyBox() != null) {
                    batch.render(getSkyBox());
                }
            } finally {
                batch.end();
            }

            // Interior color and opaque foreground depth are already present.
            // Do not clear depth: that would let shell faces draw over a nearer
            // interior wall when the camera moves inside the building.
            Gdx.gl.glColorMask(false, false, false, false);
            try {
                renderPass(renderPasses.shellDepth());
            } finally {
                Gdx.gl.glColorMask(true, true, true, true);
            }
            renderPass(renderPasses.shellColor());
            renderPass(renderPasses.shellLines());
            renderPass(renderPasses.overlays());
        } finally {
            // Each pass ends its cached RenderContext before raw state changes.
            // Leave both clearing the next frame and drawing the HUD enabled.
            Gdx.gl.glColorMask(true, true, true, true);
            Gdx.gl.glDepthMask(true);
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
