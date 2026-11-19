package org.rsreu.map;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.JsonValue;
import net.mgsx.gltf.scene3d.scene.Scene;
import org.rsreu.map.application.port.CampusRepository;
import org.rsreu.map.application.usecase.FindRoomsUseCase;
import org.rsreu.map.controller.MapController;
import org.rsreu.map.content.InMemoryCampusRepository;
import org.rsreu.map.content.ManifestLoader;
import org.rsreu.map.content.ManifestParser;
import org.rsreu.map.content.ManifestStore;
import org.rsreu.map.domain.campus.Campus;
import org.rsreu.map.domain.campus.CategoryTaxonomy;
import org.rsreu.map.input.CameraInputController;
import org.rsreu.map.input.RoomPicker;
import org.rsreu.map.input.WorkArea;
import org.rsreu.map.render.BuildingRenderableSorter;
import org.rsreu.map.render.BuildingSceneManager;
import org.rsreu.map.render.EdgeHighlighter;
import org.rsreu.map.render.RoomHighlighter;
import org.rsreu.map.render.RoomMaterialManager;
import org.rsreu.map.render.SceneLoader;
import org.rsreu.map.ui.MapHud;
import org.rsreu.map.ui.UiTheme;

import java.nio.IntBuffer;
import java.util.List;

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
    private static final float POLYGON_OFFSET_FACTOR = 0f;
    private static final float POLYGON_OFFSET_UNITS = 2f;

    private PerspectiveCamera camera;
    private CameraInputController cameraController;
    private BuildingSceneManager sceneManager;

    /**
     * The 3D work area inside the window (GL coordinates): the window minus the
     * classic chrome bands. Shared with the picking code, re-derived each frame.
     */
    private final WorkArea workArea = new WorkArea();
    /** Layered transparency order for the building units. */
    private BuildingRenderableSorter renderSorter;

    private SceneLoader sceneLoader;
    private Array<Scene> loadedScenes;

    /** Animates floor / room opacity transitions of the loaded scene. */
    private RoomMaterialManager roomMaterials;

    /** Ray picking of the loaded scene's hitboxes; feeds the highlighter. */
    private RoomPicker roomPicker;
    /** Tracks the hovered room and the locked (selected) floor. */
    private RoomHighlighter roomHighlighter;

    /** Application state mediator shared by the UI and the 3D scene commands. */
    private MapController mapController;

    /** Owns the raw manifest tree; the room editor persists changes through it. */
    private ManifestStore manifestStore;

    /** Category tree of the object browser, parsed together with the manifest. */
    private CategoryTaxonomy taxonomy;

    /** Scene2D overlay: side panel, search, category tree, hover tooltip. */
    private UiTheme uiTheme;
    private MapHud hud;

    /** Orbit distance the camera settles at when flying to a selected room. */
    private static final float FLY_ORBIT_DISTANCE = 4f;

    @Override
    public void create() {
        Gdx.app.log("MapGame", "Creating RSREU 3D Map application");

        // Set up camera
        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 0.6f, 5f);
        camera.lookAt(0f, 0f, 0f);
        camera.near = 0.1f;
        camera.far = 1000f;

        // Set up camera controller for mouse drag and WASD
        cameraController = new CameraInputController(workArea);
        cameraController.setCamera(camera);

        // PBR rendering with a separate nearest-surface depth pass for the shell.
        renderSorter = new BuildingRenderableSorter();
        sceneManager = new BuildingSceneManager(renderSorter);
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

        // Application data and the UI ↔ 3D mediator
        InMemoryCampusRepository repository = new InMemoryCampusRepository(loadCampus());
        mapController = new MapController(new FindRoomsUseCase(repository), repository);
        mapController.setMapActions(new MapController.MapActions() {
            @Override
            public void focusRoom(String hitboxNodeName) {
                if (roomHighlighter != null) {
                    roomHighlighter.selectRoom(hitboxNodeName);
                }
                flyCameraTo(hitboxNodeName);
            }

            @Override
            public void focusCategory(List<String> hitboxNodeNames) {
                if (roomHighlighter != null) {
                    roomHighlighter.showCategory(hitboxNodeNames);
                }
            }

            @Override
            public void clearFocus() {
                if (roomHighlighter != null) {
                    roomHighlighter.clearSelection();
                }
            }
        });
        mapController.setHoveredHitboxSupplier(
            () -> roomHighlighter == null ? null : roomHighlighter.hoveredHitbox());
        // A selection cleared in the scene (Escape, click) must drop the UI selection.
        if (roomHighlighter != null) {
            roomHighlighter.setSelectionClearedCallback(() -> mapController.sceneSelectionCleared());
        }

        // UI overlay; the stage consumes input before the camera controller.
        // The floor id resolver translates a hitbox node name ("floor2") into
        // the manifest floor id ("2") for the room editor prefill.
        uiTheme = new UiTheme();
        hud = new MapHud(uiTheme, mapController, taxonomy, manifestStore, hitboxNodeName -> {
            String floorNodeName = roomPicker == null ? null : roomPicker.floorOf(hitboxNodeName);
            return floorNodeName == null ? null : floorNodeName.replaceFirst("floor", "");
        });
        cameraController.setKeyboardBlockedSupplier(hud::isTextFieldFocused);
        cameraController.setEditModeSupplier(hud::isEditMode);
        cameraController.setRoomContextMenuHandler((screenX, screenY) ->
            hud.showRoomContextMenu(screenX, screenY));
        Gdx.input.setInputProcessor(new InputMultiplexer(hud.getStage(), cameraController));

        Gdx.app.log("MapGame", "Application created successfully");
    }

    /**
     * Loads the manifest into the {@link ManifestStore}: the raw tree stays
     * available for the room editor, the parsed campus feeds the UI. A missing
     * or broken manifest degrades to an empty campus: the map still renders,
     * the UI just has no metadata to show.
     */
    private Campus loadCampus() {
        try {
            manifestStore = new ManifestStore();
            manifestStore.load();
            taxonomy = manifestStore.taxonomy();
            return manifestStore.campus();
        } catch (Exception e) {
            Gdx.app.error("MapGame", "Manifest unavailable, continuing without room metadata: " + e.getMessage(), e);
            manifestStore = new ManifestStore();
            taxonomy = CategoryTaxonomy.EMPTY;
            return new Campus(List.of());
        }
    }

    /** Aims the orbit camera at the center of the room's hitbox, if it is known. */
    private void flyCameraTo(String hitboxNodeName) {
        if (roomPicker == null || camera == null) {
            return;
        }
        BoundingBox bounds = roomPicker.boundsOf(hitboxNodeName);
        if (bounds == null) {
            return;
        }
        cameraController.flyTo(bounds.getCenter(new Vector3()), FLY_ORBIT_DISTANCE);
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

    /**
     * Keeps the camera viewport and the GL viewport inside the 3D work area:
     * the window minus the top chrome (menu bar, toolbar) and the status bar.
     * The camera viewport fields are also what screen-space picking uses, so
     * the rays stay aligned with what is on screen. Re-derived every frame:
     * the chrome heights come from the laid-out UI actors.
     */
    private void updateWorkAreaViewport() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        int chromeTop = Math.round(hud.topChromeHeight());
        int chromeBottom = Math.round(hud.bottomChromeHeight());
        int viewportHeight = Math.max(1, height - chromeTop - chromeBottom);

        camera.viewportWidth = width;
        camera.viewportHeight = viewportHeight;
        workArea.set(0, chromeBottom, width, viewportHeight);
        Gdx.gl.glViewport(0, chromeBottom, width, viewportHeight);
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
            sceneManager.setRenderPasses(roomMaterials.renderPasses());
            roomPicker = new RoomPicker(scene.modelInstance.nodes);
            roomHighlighter = new RoomHighlighter(camera, workArea, roomPicker, roomMaterials);
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
        // The classic chrome owns the top and bottom bands of the window; the
        // 3D camera and the GL viewport must stay inside the work area between
        // them, otherwise projection, picking and rendering would misalign.
        updateWorkAreaViewport();

        // Update camera based on input
        cameraController.update();

        // Advance the floor / room opacity and highlight animation
        if (roomMaterials != null) {
            roomMaterials.update(Gdx.graphics.getDeltaTime());
        }

        // Clear screen (the chrome bands are covered by opaque UI afterwards)
        Gdx.gl.glClearColor(0.25f, 0.25f, 0.35f, 1f);
        Gdx.gl.glColorMask(true, true, true, true);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        // Update camera
        camera.update();

        // Update SceneManager (animations, lights, etc.)
        sceneManager.update(Gdx.graphics.getDeltaTime());

        // Render everything (shadows, reflections, colors)
        Gdx.gl.glEnable(GL20.GL_POLYGON_OFFSET_FILL);
        Gdx.gl.glPolygonOffset(POLYGON_OFFSET_FACTOR, POLYGON_OFFSET_UNITS);

        try {
            sceneManager.render();
        } finally {
            Gdx.gl.glDisable(GL20.GL_POLYGON_OFFSET_FILL);
        }

        // UI overlay on top of the 3D scene
        if (hud != null) {
            hud.actAndDraw(Gdx.graphics.getDeltaTime());
        }
    }

    @Override
    public void resize(int width, int height) {
        // The camera and GL viewport geometry is re-derived from the chrome
        // band heights on every frame in render(); here only the UI re-layout.
        if (hud != null) {
            hud.resize(width, height);
        }
    }

    @Override
    public void dispose() {
        if (hud != null) {
            hud.dispose();
        }
        if (uiTheme != null) {
            uiTheme.dispose();
        }
        if (sceneManager != null) {
            sceneManager.dispose();
        }
        if (sceneLoader != null) {
            sceneLoader.dispose();
        }
    }
}