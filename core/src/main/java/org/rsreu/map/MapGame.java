package org.rsreu.map;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;
import org.rsreu.map.input.CameraInputController;
import org.rsreu.map.input.RoomPicker;
import org.rsreu.map.render.BuildingRenderableSorter;
import org.rsreu.map.render.EdgeHighlighter;
import org.rsreu.map.render.RoomHighlighter;
import org.rsreu.map.render.RoomMaterialManager;
import org.rsreu.map.render.SceneLoader;

import java.nio.IntBuffer;

/**
 * Main libGDX application for RSREU 3D Map.
 * <p>
 * Uses SceneManager from gdx-gltf for native PBR rendering.
 */
public class MapGame extends ApplicationAdapter {

    private static final String MODEL_PATH = "models/MainCampus.glb";

    /** Color of the edge wireframe that overlays the building surface. */
    private static final Color EDGE_ACCENT_COLOR = new Color(0x2f6db2);

    /**
     * Depth bias for the filled geometry. The edge wireframe is exactly coplanar
     * with the building surface, so both produce identical depth values and fight
     * over the depth buffer. Polygon offset only affects filled triangles, never
     * line primitives, so a positive bias pushes the walls away and lets the edges
     * win.
     * <p>
     * The factor is deliberately zero: it scales with the depth slope of a
     * triangle, which for the large, nearly edge-on triangles of a building would
     * push whole walls far too far back and let the wireframe shine through them.
     * A pure unit bias is enough to break an exact tie and is predictable.
     */
    private static final float POLYGON_OFFSET_FACTOR = 1f;
    private static final float POLYGON_OFFSET_UNITS = 2f;

    private PerspectiveCamera camera;
    private CameraInputController cameraController;
    private SceneManager sceneManager;
    /** Layered transparency order for the building units. */
    private BuildingRenderableSorter renderSorter;

    private SceneLoader sceneLoader;
    private Array<Scene> loadedScenes;

    /** Animates floor / room opacity transitions of the loaded scene. */
    private RoomMaterialManager roomMaterials;

    @Override
    public void create() {
        Gdx.app.log("MapGame", "Creating RSREU 3D Map application");

        // Set up camera
        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 0.2f, 5f);
        camera.lookAt(0f, 0f, 0f);
        camera.near = 0.1f;
        camera.far = 1000f;

        // Set up camera controller for mouse drag and WASD
        cameraController = new CameraInputController();
        cameraController.setCamera(camera);
        Gdx.input.setInputProcessor(cameraController);

        // Set up SceneManager with PBR rendering and the layered transparency
        // sorter (the shell must composite over the interior it encloses).
        renderSorter = new BuildingRenderableSorter();
        sceneManager = new SceneManager(
            PBRShaderProvider.createDefault(24), PBRShaderProvider.createDefaultDepth(24), renderSorter);
        sceneManager.setCamera(camera);

        // Set ambient light
        sceneManager.setAmbientLight(0.8f);

        // Add directional light
        sceneManager.environment.add(new com.badlogic.gdx.graphics.g3d.environment.DirectionalLight()
            .set(Color.WHITE, -1f, -1f, -1f));

        loadedScenes = new Array<>();

        logFramebuffer();

        // Initialize scene loader
        sceneLoader = new SceneLoader();

        // Load the campus model
        loadScene(MODEL_PATH);

        Gdx.app.log("MapGame", "Application created successfully");
    }

    /**
     * Reports what the driver actually granted. A framebuffer that silently comes
     * back without multisampling looks exactly like one with it, so asking is
     * cheaper than staring at jagged edges.
     */
private void logFramebuffer() {
        IntBuffer params = BufferUtils.newIntBuffer(1);

        Gdx.gl.glGetIntegerv(GL20.GL_SAMPLES, params);
        int samples = params.get(0);

        params.position(0);
        Gdx.gl.glGetIntegerv(GL20.GL_DEPTH_BITS, params);
        int depthBits = params.get(0);

        Gdx.app.log("MapGame", "Framebuffer: samples=" + samples + " depthBits=" + depthBits);
        if (samples < 2) {
            Gdx.app.error("MapGame", "No multisampling active, edges will be aliased");
        }
    }

    private void loadScene(String modelPath) {
        try {
            Scene scene = sceneLoader.loadScene(modelPath);

            if (scene == null) {
                Gdx.app.error("MapGame", "Could not load model: " + modelPath);
                return;
            }

            // Add scene to SceneManager (appends scene's lights to environment automatically)
            sceneManager.addScene(scene, true);
            loadedScenes.add(scene);
            Gdx.app.log("MapGame", "Successfully loaded and added scene: " + modelPath);

            styleEdges(scene);

            // Interactive floor isolation / room highlighting.
            roomMaterials = new RoomMaterialManager(scene.modelInstance, renderSorter);
            RoomPicker roomPicker = new RoomPicker(scene.modelInstance.nodes);
            RoomHighlighter roomHighlighter = new RoomHighlighter(camera, roomPicker, roomMaterials);
            cameraController.setRoomHighlighter(roomHighlighter);
        } catch (Exception e) {
            Gdx.app.error("MapGame", "Could not load " + modelPath + ": " + e.getMessage(), e);
        }
    }

    /**
     * Colors the line primitives of the scene, which carry the edge wireframe.
     */
    private void styleEdges(Scene scene) {
        if (scene.modelInstance == null) {
            Gdx.app.error("MapGame", "modelInstance is null, edges left unstyled");
            return;
        }

        int lineParts = EdgeHighlighter.countLineParts(scene.modelInstance);
        int recolored = EdgeHighlighter.applyAccentColor(scene.modelInstance, EDGE_ACCENT_COLOR);

        Gdx.app.log("MapGame", "Edge wireframe: " + lineParts + " line part(s), "
            + recolored + " recolored with " + EDGE_ACCENT_COLOR);
    }

    @Override
    public void render() {
        // Update camera based on input
        cameraController.update();

        // Advance the floor / room opacity and highlight animation
        if (roomMaterials != null) {
            roomMaterials.update(Gdx.graphics.getDeltaTime());
        }

        // Clear screen
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.25f, 0.25f, 0.35f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        // Update camera
        camera.update();

        // Update SceneManager (animations, lights, etc.)
        sceneManager.update(Gdx.graphics.getDeltaTime());

        // Render everything (shadows, reflections, colors)
        Gdx.gl.glEnable(GL20.GL_POLYGON_OFFSET_FILL);
        Gdx.gl.glPolygonOffset(POLYGON_OFFSET_FACTOR, POLYGON_OFFSET_UNITS);

        sceneManager.render();

        Gdx.gl.glDisable(GL20.GL_POLYGON_OFFSET_FILL);
    }

    @Override
    public void resize(int width, int height) {
        if (camera != null) {
            camera.viewportWidth = width;
            camera.viewportHeight = height;
            camera.update();
        }
        if (sceneManager != null) {
            sceneManager.updateViewport(width, height);
        }
    }

    @Override
    public void dispose() {
        if (sceneManager != null) {
            sceneManager.dispose();
        }
        if (sceneLoader != null) {
            sceneLoader.dispose();
        }
    }
}