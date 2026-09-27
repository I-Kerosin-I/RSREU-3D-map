package org.rsreu.map.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;

/**
 * Loads OBJ models for the scene.
 * GLB/GLTF loading requires additional dependencies.
 * For production, consider using gdx-gltf library or converting models to OBJ.
 */
public class SceneLoader {

    private ObjLoader objLoader;

    public SceneLoader() {
        this.objLoader = new ObjLoader();
    }

    /**
     * Loads a scene from the given path.
     * Currently supports OBJ format. GLB requires additional setup.
     *
     * @param path path to the model file (relative to assets)
     * @return ModelInstance for rendering, or null if loading fails
     */
    public ModelInstance loadScene(String path) {
        try {
            if (path == null || path.isBlank()) {
                Gdx.app.log("SceneLoader", "Path is null or blank");
                return null;
            }

            // Try to load as OBJ first
            String fullPath = "assets/" + path;
            Gdx.app.log("SceneLoader", "Loading scene from: " + fullPath);

            // Remove .glb/.gltf extension and try .obj
            String objPath = fullPath.replaceAll("\\.glb$", ".obj").replaceAll("\\.gltf$", ".obj");
            Gdx.app.log("SceneLoader", "Trying to load: " + objPath);

            Model model = objLoader.loadModel(Gdx.files.internal(objPath), true);
            if (model != null) {
                ModelInstance instance = new ModelInstance(model);
                Gdx.app.log("SceneLoader", "Successfully loaded scene: " + objPath);
                return instance;
            } else {
                Gdx.app.log("SceneLoader", "Model is null for: " + objPath);
                return null;
            }
        } catch (Exception e) {
            Gdx.app.log("SceneLoader", "Could not load model: " + e.getMessage());
            return null;
        }
    }

    /**
     * Disposes of resources.
     */
    public void dispose() {
        // No dispose method on ObjLoader
    }
}