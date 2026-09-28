package org.rsreu.map.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.utils.Disposable;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

/**
 * Loads GLB/GLTF models using gdx-gltf library (net.mgsx.gltf).
 * Supports both GLB binary and GLTF text formats.
 * <p>
 * The loaded Scene can be used directly as a RenderableProvider with ModelBatch,
 * or the underlying Model can be extracted and used as a ModelInstance.
 */
public class SceneLoader implements Disposable {

    private GLBLoader glbLoader;

    public SceneLoader() {
        this.glbLoader = new GLBLoader();
    }

    /**
     * Loads a GLB/GLTF scene from the given path.
     * Returns a Scene which implements RenderableProvider.
     *
     * @param path path to the model file (relative to assets, e.g. "models/test.glb")
     * @return Scene for rendering, or null if loading fails
     */
    public Scene loadScene(String path) {
        try {
            if (path == null || path.isBlank()) {
                Gdx.app.log("SceneLoader", "Path is null or blank");
                return null;
            }

            String fullPath = "assets/" + path;
            Gdx.app.log("SceneLoader", "Loading scene from: " + fullPath);

            SceneAsset asset = glbLoader.load(Gdx.files.internal(fullPath));
            if (asset == null) {
                Gdx.app.log("SceneLoader", "Failed to load scene (asset is null): " + path);
                return null;
            }

            if (asset.scene == null) {
                Gdx.app.log("SceneLoader", "Failed to load scene (scene is null): " + path);
                return null;
            }

            // SceneModel is not Scene - construct Scene from SceneModel
            Scene scene = new Scene(asset.scene);

            Gdx.app.log("SceneLoader", "Successfully loaded scene: " + path);
            return scene;
        } catch (Exception e) {
            Gdx.app.error("SceneLoader", "Failed to load scene from " + path + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads a scene and returns it as a ModelInstance for use with ModelBatch.
     *
     * @param path path to the model file (relative to assets)
     * @return ModelInstance for rendering, or null if loading fails
     */
    public ModelInstance loadSceneAsModelInstance(String path) {
        Scene scene = loadScene(path);
        if (scene != null && scene.modelInstance != null) {
            return scene.modelInstance;
        }
        return null;
    }

    /**
     * Returns the raw libGDX Model from a loaded scene.
     *
     * @param path path to the model file (relative to assets)
     * @return Model, or null if loading fails
     */
    public Model loadModel(String path) {
        Scene scene = loadScene(path);
        if (scene != null && scene.modelInstance != null) {
            return scene.modelInstance.model;
        }
        return null;
    }

    @Override
    public void dispose() {
        if (glbLoader != null) {
            glbLoader = null;
        }
    }
}