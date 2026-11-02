package org.rsreu.map.content;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Loads manifest.json from assets folder using libGDX JsonReader.
 */
public class ManifestLoader {

    private static final String MANIFEST_PATH = "assets/manifest.json";

    private final JsonReader jsonReader;

    public ManifestLoader() {
        this.jsonReader = new JsonReader();
    }

    /**
     * Loads and returns the raw JsonValue for manifest.json from assets.
     *
     * @return JsonValue representing the parsed manifest
     * @throws IllegalStateException if the manifest cannot be loaded or parsed
     */
    public JsonValue load() {
        try {
            JsonValue root = jsonReader.parse(Gdx.files.internal(MANIFEST_PATH));
            Gdx.app.log("ManifestLoader", "Successfully loaded manifest.json");
            return root;
        } catch (Exception e) {
            Gdx.app.error("ManifestLoader", "Failed to load manifest.json: " + e.getMessage());
            throw new IllegalStateException("Failed to load manifest.json", e);
        }
    }

    /**
     * Checks if the manifest file exists in assets.
     *
     * @return true if manifest.json exists
     */
    public boolean manifestExists() {
        return Gdx.files.internal(MANIFEST_PATH).exists();
    }
}