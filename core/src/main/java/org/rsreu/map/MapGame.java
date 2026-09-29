package org.rsreu.map;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.utils.Array;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import org.rsreu.map.input.CameraInputController;
import org.rsreu.map.render.SceneLoader;

/**
 * Main libGDX application for RSREU 3D Map.
 * <p>
 * Uses SceneManager from gdx-gltf for native PBR rendering.
 */
public class MapGame extends ApplicationAdapter {

    private PerspectiveCamera camera;
    private CameraInputController cameraController;
    private SceneManager sceneManager;

    private SceneLoader sceneLoader;
    private Array<Scene> loadedScenes;

    @Override
    public void create() {
        Gdx.app.log("MapGame", "Creating RSREU 3D Map application");

        // Set up camera
        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(5f, 0f, 0f);
        camera.lookAt(0f, 0f, 0f);
        camera.near = 0.1f;
        camera.far = 1000f;

        // Set up camera controller for mouse drag and WASD
        cameraController = new CameraInputController();
        cameraController.setCamera(camera);
        Gdx.input.setInputProcessor(cameraController);

        // Set up SceneManager with PBR rendering
        sceneManager = new SceneManager();
        sceneManager.setCamera(camera);
        
        // Set ambient light
        sceneManager.setAmbientLight(0.8f);
        
        // Add directional light
        sceneManager.environment.add(new com.badlogic.gdx.graphics.g3d.environment.DirectionalLight()
            .set(Color.WHITE, -1f, -1f, -1f));

        loadedScenes = new Array<>();

        // Initialize scene loader
        sceneLoader = new SceneLoader();

        // Load the test GLB model
        loadTestModel();

        Gdx.app.log("MapGame", "Application created successfully");
    }

    private void loadTestModel() {
        try {
            String modelPath = "models/test.glb";
            Scene scene = sceneLoader.loadScene(modelPath);

            if (scene != null) {
                // Add scene to SceneManager (appends scene's lights to environment automatically)
                sceneManager.addScene(scene, true);
                loadedScenes.add(scene);
                Gdx.app.log("MapGame", "Successfully loaded and added scene: " + modelPath);

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
        
        // Update SceneManager (animations, lights, etc.)
        sceneManager.update(Gdx.graphics.getDeltaTime());
        
        // Render everything (shadows, reflections, colors)
        sceneManager.render();
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

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("RSREU 3D Map");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        config.setForegroundFPS(60);

        new Lwjgl3Application(new MapGame(), config);
    }
}
