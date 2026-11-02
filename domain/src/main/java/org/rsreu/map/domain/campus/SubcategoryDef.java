package org.rsreu.map.domain.campus;

/**
 * Displayable subcategory of map objects, e.g. a single department inside
 * "Кафедры" or М/Ж inside "WC". Pure metadata, driven by the manifest.
 */
public record SubcategoryDef(String id, String name) {

    public SubcategoryDef {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("SubcategoryDef id cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("SubcategoryDef name cannot be null or blank");
        }
    }
}
