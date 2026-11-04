package org.rsreu.map.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import net.mgsx.gltf.scene3d.shaders.PBRShader;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;

/** Keeps surface lines just ahead of their fills without disabling occlusion. */
public final class BuildingShaderProvider extends PBRShaderProvider {

    private static final float EDGE_BIAS_PIXELS = 1.5f;
    private static final float MAX_RELATIVE_DEPTH_BIAS = 0.003f;

    public BuildingShaderProvider(int maxBones) {
        super(createConfig(maxBones));
    }

    private static PBRShaderConfig createConfig(int maxBones) {
        PBRShaderConfig config = createDefaultConfig();
        config.numBones = maxBones;
        config.vertexShader = edgeVertexShader(config.vertexShader);
        return config;
    }

    @Override
    public String createPrefixBase(Renderable renderable, PBRShaderConfig config) {
        String prefix = super.createPrefixBase(renderable, config);
        return isLine(renderable.meshPart.primitiveType)
            ? prefix + "#define BUILDING_EDGE\n" : prefix;
    }

    @Override
    protected PBRShader createShader(Renderable renderable, PBRShaderConfig config, String prefix) {
        return new BuildingShader(renderable, config, prefix);
    }

    /** Wrap the library entry point; fills retain the unmodified PBR vertex path. */
    static String edgeVertexShader(String source) {
        return """
            #ifdef BUILDING_EDGE
            #define main buildingPbrMain
            #endif
            """ + source + """

            #ifdef BUILDING_EDGE
            #undef main
            uniform vec2 u_buildingEdgeDepthOffset;
            void main() {
                buildingPbrMain();
                if (gl_Position.w > 0.0) {
                    float offset = max(0.0, u_buildingEdgeDepthOffset.x
                        + u_buildingEdgeDepthOffset.y * gl_Position.w);
                    // Do not pull clipped vertices across the near plane.
                    float nearRoom = max(0.0, gl_Position.z + gl_Position.w);
                    gl_Position.z -= min(offset, 0.5 * nearRoom);
                }
            }
            #endif
            """;
    }

    /** Offset in clip Z: x is constant, y scales with clip W. */
    static void edgeDepthOffset(Camera camera, int framebufferHeight, Vector2 out) {
        float[] projection = camera.projection.val;
        float scaleY = Math.abs(projection[Matrix4.M11]);
        out.setZero();
        if (framebufferHeight <= 0 || scaleY <= 0f) {
            return;
        }
        float pixelSize = 2f * EDGE_BIAS_PIXELS / (framebufferHeight * scaleY);
        if (projection[Matrix4.M32] != 0f) {
            // Perspective: moving a point toward the camera by a relative
            // distance changes only clip Z when its original X/Y/W are retained.
            float relative = Math.min(pixelSize, MAX_RELATIVE_DEPTH_BIAS);
            out.x = Math.abs(projection[Matrix4.M23]) * relative / (1f - relative);
        } else {
            float distance = Math.min(pixelSize,
                MAX_RELATIVE_DEPTH_BIAS * Math.abs(camera.far - camera.near));
            out.y = Math.abs(projection[Matrix4.M22]) * distance;
        }
    }

    static boolean isLine(int primitiveType) {
        return primitiveType == GL20.GL_LINES
            || primitiveType == GL20.GL_LINE_LOOP
            || primitiveType == GL20.GL_LINE_STRIP;
    }

    static boolean samePrimitiveClass(int first, int second) {
        return isLine(first) == isLine(second);
    }

    private static final class BuildingShader extends PBRShader {
        private final int primitiveType;
        private final int edgeDepthUniform;
        private final Vector2 edgeOffset = new Vector2();

        BuildingShader(Renderable renderable, PBRShaderConfig config, String prefix) {
            super(renderable, config, prefix);
            primitiveType = renderable.meshPart.primitiveType;
            edgeDepthUniform = isLine(primitiveType)
                ? register(new Uniform("u_buildingEdgeDepthOffset")) : -1;
        }

        @Override
        public boolean canRender(Renderable renderable) {
            // PBR's material/vertex masks alone can match a line and an unlit fill.
            return samePrimitiveClass(primitiveType, renderable.meshPart.primitiveType)
                && super.canRender(renderable);
        }

        @Override
        public void begin(Camera camera, RenderContext context) {
            super.begin(camera, context);
            if (edgeDepthUniform >= 0) {
                edgeDepthOffset(camera, Gdx.graphics.getBackBufferHeight(), edgeOffset);
                set(edgeDepthUniform, edgeOffset.x, edgeOffset.y);
            }
        }
    }
}
