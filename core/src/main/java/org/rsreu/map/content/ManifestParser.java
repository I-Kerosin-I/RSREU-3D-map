package org.rsreu.map.content;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonValue;

import org.rsreu.map.domain.campus.Building;
import org.rsreu.map.domain.campus.Campus;
import org.rsreu.map.domain.campus.CategoryDef;
import org.rsreu.map.domain.campus.CategoryTaxonomy;
import org.rsreu.map.domain.campus.Floor;
import org.rsreu.map.domain.campus.Space;
import org.rsreu.map.domain.campus.SpaceType;
import org.rsreu.map.domain.campus.SubcategoryDef;
import org.rsreu.map.domain.geometry.Bounds3;
import org.rsreu.map.domain.geometry.Point3;
import org.rsreu.map.domain.model.BuildingId;
import org.rsreu.map.domain.model.FloorId;
import org.rsreu.map.domain.model.RoomId;
import org.rsreu.map.domain.navigation.EdgeType;
import org.rsreu.map.domain.navigation.NavigationEdge;
import org.rsreu.map.domain.navigation.NavigationGraph;
import org.rsreu.map.domain.navigation.NavigationNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts JsonValue manifest data to domain Campus object.
 * Currently creates a stub campus with hardcoded data.
 */
public class ManifestParser {

    public ManifestParser() {
    }

    /**
     * Parses the JsonValue manifest into a domain Campus object.
     *
     * @param root the parsed JSON manifest
     * @return Campus domain object
     */
    public Campus parse(JsonValue root) {
        if (root == null) {
            return createStubCampus();
        }

        try {
            return parseFullCampus(root);
        } catch (Exception e) {
            Gdx.app.error("ManifestParser", "Failed to parse full manifest, using stub data: " + e.getMessage(), e);
            return createStubCampus();
        }
    }

    /**
     * Parses the top-level {@code categories} section of the manifest into the
     * display taxonomy for the object browser panel.
     *
     * @param root the parsed JSON manifest
     * @return the category taxonomy, empty when the manifest defines none
     */
    public CategoryTaxonomy parseCategories(JsonValue root) {
        JsonValue categoriesArray = root == null ? null : root.get("categories");
        if (categoriesArray == null) {
            return CategoryTaxonomy.EMPTY;
        }

        List<CategoryDef> categories = new ArrayList<>();
        for (JsonValue categoryValue : categoriesArray) {
            String id = categoryValue.getString("id", null);
            if (id == null) {
                continue;
            }
            String name = categoryValue.getString("name", id);

            List<SubcategoryDef> subcategories = new ArrayList<>();
            JsonValue subcategoriesArray = categoryValue.get("subcategories");
            if (subcategoriesArray != null) {
                for (JsonValue subcategoryValue : subcategoriesArray) {
                    String subId = subcategoryValue.getString("id", null);
                    if (subId == null) {
                        continue;
                    }
                    subcategories.add(new SubcategoryDef(subId, subcategoryValue.getString("name", subId)));
                }
            }

            categories.add(new CategoryDef(id, name, subcategories));
        }
        return new CategoryTaxonomy(categories);
    }

    private Campus parseFullCampus(JsonValue root) {
        List<Building> buildings = new ArrayList<>();

        JsonValue buildingsArray = root.get("buildings");
        if (buildingsArray != null) {
            for (JsonValue buildingValue : buildingsArray) {
                Building building = parseBuilding(buildingValue);
                if (building != null) {
                    buildings.add(building);
                }
            }
        }

        return new Campus(buildings);
    }

    private Building parseBuilding(JsonValue buildingValue) {
        String id = buildingValue.getString("id", null);
        String name = buildingValue.getString("name", "Unknown Building");

        if (id == null) return null;

        List<Floor> floors = new ArrayList<>();
        JsonValue floorsArray = buildingValue.get("floors");
        if (floorsArray != null) {
            for (JsonValue floorValue : floorsArray) {
                Floor floor = parseFloor(floorValue, id);
                if (floor != null) {
                    floors.add(floor);
                }
            }
        }

        return new Building(new BuildingId(id), name, floors);
    }

    private Floor parseFloor(JsonValue floorValue, String buildingId) {
        String id = floorValue.getString("id", null);
        int level = floorValue.getInt("level", 1);
        float heightMeters = (float) floorValue.getFloat("heightMeters", 0f);

        if (id == null) return null;

        // Parse spaces
        List<Space> spaces = new ArrayList<>();
        JsonValue spacesArray = floorValue.get("spaces");
        if (spacesArray != null) {
            for (JsonValue spaceValue : spacesArray) {
                Space space = parseSpace(spaceValue, id);
                if (space != null) {
                    spaces.add(space);
                }
            }
        }

        return new Floor(new FloorId(id), new BuildingId(buildingId), level, heightMeters, spaces);
    }

    private Space parseSpace(JsonValue spaceValue, String floorId) {
        String id = spaceValue.getString("id", null);
        String name = spaceValue.getString("name", "Unknown Space");
        String typeStr = spaceValue.getString("type", "OTHER");
        String modelNode = spaceValue.getString("modelNode", "");
        String description = spaceValue.getString("description", null);
        String category = spaceValue.getString("category", null);
        String subcategory = spaceValue.getString("subcategory", null);

        if (id == null) return null;

        SpaceType type = parseSpaceType(typeStr);

        // Parse bounds if present
        Bounds3 bounds = parseBounds(spaceValue.get("bounds"));

        return new Space(new RoomId(id), name, new FloorId(floorId), type, modelNode, bounds,
            description, category, subcategory);
    }

    private SpaceType parseSpaceType(String typeStr) {
        try {
            return SpaceType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SpaceType.OTHER;
        }
    }

    private Bounds3 parseBounds(JsonValue boundsValue) {
        if (boundsValue == null) {
            return new Bounds3(new Point3(0, 0, 0), new Point3(1, 1, 1));
        }

        Point3 min = parsePoint3(boundsValue.get("min"));
        Point3 max = parsePoint3(boundsValue.get("max"));

        if (min == null || max == null) {
            return new Bounds3(new Point3(0, 0, 0), new Point3(1, 1, 1));
        }

        return new Bounds3(min, max);
    }

    private Point3 parsePoint3(JsonValue arrayValue) {
        if (arrayValue == null || arrayValue.size < 3) {
            return null;
        }

        float x = arrayValue.get(0).asFloat();
        float y = arrayValue.get(1).asFloat();
        float z = arrayValue.get(2).asFloat();

        return new Point3(x, y, z);
    }

    private Campus createStubCampus() {
        // Create a minimal stub campus for testing
        Space stubSpace = new Space(
            new RoomId("room_test"),
            "Test Room",
            new FloorId("1"),
            SpaceType.ROOM,
            "test_node",
            new Bounds3(new Point3(0, 0, 0), new Point3(1, 1, 1))
        );

        Floor stubFloor = new Floor(
            new FloorId("1"),
            new BuildingId("main"),
            1,
            0f,
            List.of(stubSpace)
        );

        Building stubBuilding = new Building(
            new BuildingId("main"),
            "Main Building",
            List.of(stubFloor)
        );

        return new Campus(List.of(stubBuilding));
    }
}