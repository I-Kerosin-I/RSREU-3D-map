package org.rsreu.map;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Array;
import net.mgsx.gltf.scene3d.scene.Scene;
import org.rsreu.map.input.CameraInputController;
import org.rsreu.map.render.SceneLoader;


/**
 * Main libGDX application for RSREU 3D Map.
 */
public class MapGame extends ApplicationAdapter {

    private PerspectiveCamera camera;
    private CameraInputController cameraController;
    private ModelBatch modelBatch;
    private Environment environment;

    private SceneLoader sceneLoader;
    private Array<Scene> loadedScenes;
    private Array<ModelInstance> modelInstances; // For test boxes

    @Override
    public void create() {
        Gdx.app.log("MapGame", "Creating RSREU 3D Map application");

        // Set up camera
        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(5f, 5f, 5f);
        camera.lookAt(0f, 0f, 0f);
        camera.near = 0.1f;
        camera.far = 1000f;

        // Set up camera controller for mouse drag and WASD
        cameraController = new CameraInputController();
        cameraController.setCamera(camera);
        Gdx.input.setInputProcessor(cameraController);

        // Set up model batch and environment
        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f));
        environment.add(new com.badlogic.gdx.graphics.g3d.environment.DirectionalLight()
            .set(Color.WHITE, -1f, -1f, -1f));

        loadedScenes = new Array<>();
        modelInstances = new Array<>();

        // Create a simple test box using ModelBuilder
        createTestBox();

        // Initialize scene loader
        sceneLoader = new SceneLoader();

        // Load the test GLB model
        loadTestModel();

        Gdx.app.log("MapGame", "Application created successfully");
    }

    private void createTestBox() {
        try {
            ModelBuilder modelBuilder = new ModelBuilder();
            Material mat = new Material(ColorAttribute.createDiffuse(Color.RED));
            Model boxModel = modelBuilder.createBox(2f, 2f, 2f, mat,
                com.badlogic.gdx.graphics.VertexAttributes.Usage.Normal |
                com.badlogic.gdx.graphics.VertexAttributes.Usage.Position);
            ModelInstance boxInstance = new ModelInstance(boxModel);
            boxInstance.transform.setToTranslation(-4, 0, 0); // Offset to the left
            modelInstances.add(boxInstance);
            Gdx.app.log("MapGame", "Created test box at offset (-4, 0, 0)");
        } catch (Exception e) {
            Gdx.app.log("MapGame", "Failed to create test box: " + e.getMessage());
        }
    }

    private void loadTestModel() {
        try {
            String modelPath = "models/test.glb";
            Scene scene = sceneLoader.loadScene(modelPath);

            if (scene != null) {
                loadedScenes.add(scene);
                Gdx.app.log("MapGame", "Successfully loaded test model: " + modelPath);

                // Debug: print model info
                if (scene.modelInstance != null) {
                    Gdx.app.log("MapGame", "  Transform: " + scene.modelInstance.transform);
                    if (scene.modelInstance.model != null) {
                        Gdx.app.log("MapGame", "  Meshes: " + scene.modelInstance.model.meshParts.size);
                        Gdx.app.log("MapGame", "  Materials: " + scene.modelInstance.model.materials.size);
                    }
                } else {
                    Gdx.app.log("MapGame", "  Warning: modelInstance is null!");
                }
            } else {
                Gdx.app.log("MapGame", "Warning: Could not load test model, scene is null");
            }
        } catch (Exception e) {
            Gdx.app.log("MapGame", "Could not load test.glb: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void render() {
        // Update camera based on input
        cameraController.update();

        // Clear screen
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.25f, 0.25f, 0.35f, 1f);
        Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT | com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_DEPTH_TEST);

        // Update camera
        camera.update();

        modelBatch.begin(camera);

        // Render test box (should appear red, offset to the left)
        for (ModelInstance instance : modelInstances) {
            try {
                modelBatch.render(instance, environment);
            } catch (Exception e) {
                Gdx.app.log("MapGame", "Error rendering test box: " + e.getMessage());
            }
        }

        // Render all loaded scenes
        for (Scene scene : loadedScenes) {
            try {
                modelBatch.render(scene, environment);
            } catch (Exception e) {
                Gdx.app.log("MapGame", "Error rendering scene: " + e.getMessage());
            }
        }

        modelBatch.end();
    }

    @Override
    public void resize(int width, int height) {
        if (camera != null) {
            camera.viewportWidth = width;
            camera.viewportHeight = height;
            camera.update();
        }
    }

    @Override
    public void dispose() {
        if (modelBatch != null) {
            modelBatch.dispose();
        }
        if (sceneLoader != null) {
            sceneLoader.dispose();
        }
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("RSREU 3D Map");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        config.setForegroundFPS(60);

        new Lwjgl3Application(new MapGame(), config);
    }
}
