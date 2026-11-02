package org.rsreu.map.domain.campus;

import java.util.List;
import java.util.Optional;

/**
 * The category tree used by the object browser panel: categories, their
 * subcategories and display names. Loaded from the manifest, so adding a
 * category is a data change, not a code change.
 */
public record CategoryTaxonomy(List<CategoryDef> categories) {

    public static final CategoryTaxonomy EMPTY = new CategoryTaxonomy(List.of());

    public CategoryTaxonomy {
        categories = categories == null ? List.of() : List.copyOf(categories);
    }

    /** Finds a category definition by its stable id. */
    public Optional<CategoryDef> find(String categoryId) {
        return categories.stream()
            .filter(category -> category.id().equals(categoryId))
            .findFirst();
    }
}
