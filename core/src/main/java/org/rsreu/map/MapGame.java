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
    private ModelBuilder modelBuilder;

    private SceneLoader sceneLoader;

    private Array<ModelInstance> modelInstances;
    private ModelInstance axesInstance;

    @Override
    public void create() {
        Gdx.app.log("MapGame", "Creating RSREU 3D Map application");

        // Set up camera
        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 5f, 10f);
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
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new com.badlogic.gdx.graphics.g3d.environment.DirectionalLight()
            .set(Color.WHITE, -1f, -1f, -1f));

        modelBuilder = new ModelBuilder();
        modelInstances = new Array<>();

        // Create coordinate axes for reference
        createCoordinateAxes();

        // Initialize scene loader
        sceneLoader = new SceneLoader();

        // Try to load the test GLB model
        loadTestModel();

        Gdx.app.log("MapGame", "Application created successfully");
    }

    private void createCoordinateAxes() {
        // Create coordinate axes manually using Box model scaled to show axes
        // For simplicity, skip axes if API doesn't match
        Gdx.app.log("MapGame", "Coordinate axes skipped - using simple scene");
    }

    private void loadTestModel() {
        try {
            // Try to load test.glb from assets
            String modelPath = "models/test.glb";
            ModelInstance instance = sceneLoader.loadScene(modelPath);

            if (instance != null) {
                modelInstances.add(instance);
                Gdx.app.log("MapGame", "Successfully loaded test model: " + modelPath);

                // Center the model at origin
                centerModel(instance);
            } else {
                Gdx.app.log("MapGame", "Warning: Could not load test model, instance is null");
            }
        } catch (Exception e) {
            Gdx.app.log("MapGame", "Could not load test.glb: " + e.getMessage());
        }
    }

    private void centerModel(ModelInstance instance) {
        if (instance != null && instance.transform != null) {
            instance.transform.idt();
        }
    }

    @Override
    public void render() {
        // Update camera based on input
        cameraController.update();

        // Clear screen
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.3f, 1f);
        Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT | com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT);

        // Update camera
        camera.update();

        // Render coordinate axes
        if (axesInstance != null) {
            modelBatch.render(axesInstance, environment);
        }

        // Render all loaded model instances
        for (ModelInstance instance : modelInstances) {
            try {
                modelBatch.render(instance, environment);
            } catch (Exception e) {
                Gdx.app.log("MapGame", "Error rendering model: " + e.getMessage());
            }
        }
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