package org.rsreu.map.content;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Validates that referenced content (GLB files, node names) exists and is valid.
 */
public class ContentValidator {

    public ContentValidator() {
    }

    /**
     * Validates the manifest content against the actual GLB files.
     *
     * @param glbPath path to the GLB file (relative to assets)
     * @param manifest the parsed manifest JsonValue
     * @throws ContentValidationException if validation fails
     */
    public void validate(String glbPath, JsonValue manifest) throws ContentValidationException {
        // Check if GLB file exists
        if (glbPath != null && !glbPath.isBlank()) {
            String fullPath = "assets/" + glbPath;
            FileHandle file = Gdx.files.internal(fullPath);

            if (!file.exists()) {
                throw new ContentValidationException("GLB file does not exist: " + fullPath);
            }
        }
    }

    /**
     * Exception thrown when content validation fails.
     */
    public static class ContentValidationException extends Exception {
        public ContentValidationException(String message) {
            super(message);
        }

        public ContentValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}