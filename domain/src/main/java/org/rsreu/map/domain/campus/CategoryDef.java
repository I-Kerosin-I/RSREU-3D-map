package org.rsreu.map.domain.campus;

import java.util.List;

/**
 * Displayable category of map objects, e.g. "Кафедры", "WC", "Столовые".
 * Subcategories are optional. Pure metadata, driven by the manifest.
 */
public record CategoryDef(String id, String name, List<SubcategoryDef> subcategories) {

    public CategoryDef {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("CategoryDef id cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("CategoryDef name cannot be null or blank");
        }
        subcategories = subcategories == null ? List.of() : List.copyOf(subcategories);
    }
}
