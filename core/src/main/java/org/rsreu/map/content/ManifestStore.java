package org.rsreu.map.content;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

import org.rsreu.map.domain.campus.Campus;
import org.rsreu.map.domain.campus.CategoryTaxonomy;
import org.rsreu.map.domain.campus.Space;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Owns the raw manifest.json tree: serves the parsed domain data, applies
 * editor changes in place and writes the file back to disk.
 * <p>
 * Editing mutates the raw {@link JsonValue} (unknown fields of untouched
 * entries survive), then the domain objects are re-parsed from the same tree,
 * so the in-memory campus and the file always stay in sync.
 */
public final class ManifestStore {

    /** Path the manifest is saved to, relative to the application working dir. */
    private static final String MANIFEST_PATH = "assets/manifest.json";

    private final ManifestParser parser = new ManifestParser();
    private JsonValue root;
    private Campus campus;
    private CategoryTaxonomy taxonomy;

    /**
     * Loads and parses the manifest. Throws when the manifest is missing or
     * broken: the editor cannot work without it, while the map itself can.
     */
    public void load() {
        root = new ManifestLoader().load();
        parseRoot();
    }

    private void parseRoot() {
        campus = parser.parse(root);
        taxonomy = parser.parseCategories(root);
    }

    public Campus campus() {
        return campus;
    }

    public CategoryTaxonomy taxonomy() {
        return taxonomy;
    }

    /** Finds the raw JSON entry of a space by its stable id. */
    public Optional<JsonValue> findSpaceEntry(String spaceId) {
        JsonValue buildings = root.get("buildings");
        for (JsonValue building = buildings.child(); building != null; building = building.next()) {
            JsonValue floors = building.get("floors");
            for (JsonValue floor = floors.child(); floor != null; floor = floor.next()) {
                JsonValue spaces = floor.get("spaces");
                for (JsonValue space = spaces == null ? null : spaces.child();
                     space != null; space = space.next()) {
                    if (spaceId.equals(space.getString("id", null))) {
                        return Optional.of(space);
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Creates or updates the JSON entry of a space and re-parses the domain
     * objects, keeping the campus in sync with the file tree.
     *
     * @param space   the space metadata to persist
     * @param floorId the floor id used when the space entry does not exist yet
     */
    public void upsertSpace(Space space, String floorId) {
        Optional<JsonValue> existing = findSpaceEntry(space.id().value());
        if (existing.isPresent()) {
            updateSpaceEntry(existing.get(), space);
        } else {
            createSpaceEntry(space, floorId);
        }
        parseRoot();
    }

    /**
     * Writes the manifest to disk and returns the absolute file path for status
     * messages.
     */
    public String save() {
        FileHandle file = Gdx.files.local(MANIFEST_PATH);
        file.writeString(root.prettyPrint(JsonWriter.OutputType.json, 2), false, "UTF-8");
        return file.file().getAbsolutePath();
    }

    /**
     * Registers a subcategory typed by the user in the room editor (for example
     * a department missing from the list). Its id equals its name, which keeps
     * matching simple: a repeated registration is a no-op. Re-parses the
     * taxonomy afterwards, so the new item reaches every list.
     *
     * @throws IllegalArgumentException when the category is unknown
     */
    public void addSubcategory(String categoryId, String subcategoryName) {
        JsonValue categories = root.get("categories");
        for (JsonValue category = categories.child(); category != null; category = category.next()) {
            if (categoryId.equals(category.getString("id", null))) {
                JsonValue subcategories = category.get("subcategories");
                if (subcategories == null || !subcategories.isArray()) {
                    subcategories = new JsonValue(JsonValue.ValueType.array);
                    category.addChild("subcategories", subcategories);
                }
                if (findSubcategory(subcategories, subcategoryName) != null) {
                    return;
                }
                JsonValue entry = new JsonValue(JsonValue.ValueType.object);
                entry.addChild("id", new JsonValue(subcategoryName));
                entry.addChild("name", new JsonValue(subcategoryName));
                subcategories.addChild(entry);
                parseRoot();
                return;
            }
        }
        throw new IllegalArgumentException("Категория не найдена в манифесте: " + categoryId);
    }

    private static JsonValue findSubcategory(JsonValue subcategories, String subcategoryId) {
        for (JsonValue subcategory = subcategories.child(); subcategory != null;
             subcategory = subcategory.next()) {
            if (subcategoryId.equals(subcategory.getString("id", null))) {
                return subcategory;
            }
        }
        return null;
    }

    private void updateSpaceEntry(JsonValue entry, Space space) {
        setString(entry, "name", space.displayName());
        setString(entry, "type", space.type().name());
        setString(entry, "description", space.description());
        setString(entry, "category", space.category());
        setString(entry, "subcategory", space.subcategory());
        setNumber(entry, "seats", space.seats());
        setStringList(entry, "equipment", space.equipment());
        setStringObject(entry, "attributes", space.attributes());
    }

    private void createSpaceEntry(Space space, String floorId) {
        JsonValue floorEntry = findFloorEntry(floorId);
        if (floorEntry == null) {
            throw new IllegalStateException("Floor not found in manifest: " + floorId);
        }
        JsonValue spaces = floorEntry.get("spaces");
        if (spaces == null || !spaces.isArray()) {
            spaces = new JsonValue(JsonValue.ValueType.array);
            floorEntry.addChild("spaces", spaces);
        }

        JsonValue entry = new JsonValue(JsonValue.ValueType.object);
        entry.addChild("id", new JsonValue(space.id().value()));
        entry.addChild("name", new JsonValue(space.displayName()));
        entry.addChild("type", new JsonValue(space.type().name()));
        entry.addChild("modelNode", new JsonValue(space.modelNodeName()));
        setString(entry, "description", space.description());
        setString(entry, "category", space.category());
        setString(entry, "subcategory", space.subcategory());
        setNumber(entry, "seats", space.seats());
        setStringList(entry, "equipment", space.equipment());
        setStringObject(entry, "attributes", space.attributes());
        spaces.addChild(entry);
    }

    private JsonValue findFloorEntry(String floorId) {
        JsonValue buildings = root.get("buildings");
        for (JsonValue building = buildings.child(); building != null; building = building.next()) {
            JsonValue floors = building.get("floors");
            for (JsonValue floor = floors.child(); floor != null; floor = floor.next()) {
                if (floorId.equals(floor.getString("id", null))) {
                    return floor;
                }
            }
        }
        return null;
    }

    private static void setString(JsonValue entry, String name, String value) {
        removeField(entry, name);
        if (value != null && !value.isBlank()) {
            entry.addChild(name, new JsonValue(value));
        }
    }

    private static void setNumber(JsonValue entry, String name, Integer value) {
        removeField(entry, name);
        if (value != null) {
            entry.addChild(name, new JsonValue(value.longValue()));
        }
    }

    private static void setStringList(JsonValue entry, String name, List<String> values) {
        removeField(entry, name);
        if (values == null || values.isEmpty()) {
            return;
        }
        JsonValue array = new JsonValue(JsonValue.ValueType.array);
        for (String item : values) {
            array.addChild(new JsonValue(item));
        }
        entry.addChild(name, array);
    }

    private static void setStringObject(JsonValue entry, String name, Map<String, String> values) {
        removeField(entry, name);
        if (values == null || values.isEmpty()) {
            return;
        }
        JsonValue object = new JsonValue(JsonValue.ValueType.object);
        for (Map.Entry<String, String> item : values.entrySet()) {
            object.addChild(item.getKey(), new JsonValue(item.getValue()));
        }
        entry.addChild(name, object);
    }

    public void reloadFromRoot() {
        parseRoot();
    }

    private static void removeField(JsonValue entry, String name) {
        // JsonValue.hasChild(name) is a trap in 1.12.1: it reports whether the
        // found child has children of its own, so it is false for every scalar
        // field and the field would never be replaced. remove(String) is a
        // no-op when the field is absent.
        entry.remove(name);
    }
}
