package org.rsreu.map.render;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;

/**
 * Renders building models in the 3D scene.
 */
public class BuildingRenderer {

    public BuildingRenderer() {
    }

    /**
     * Renders a model instance in the scene.
     *
     * @param instance the model instance to render
     * @param batch the model batch to use for rendering
     * @param environment the rendering environment (lighting, etc.)
     */
    public void renderScene(ModelInstance instance, ModelBatch batch, Environment environment) {
        if (instance == null || batch == null) {
            return;
        }

        try {
            batch.render(instance, environment);
        } catch (Exception e) {
            System.err.println("Error rendering scene: " + e.getMessage());
        }
    }
}