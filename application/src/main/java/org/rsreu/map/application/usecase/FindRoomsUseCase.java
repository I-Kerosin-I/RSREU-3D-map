package org.rsreu.map.application.usecase;

import org.rsreu.map.application.port.CampusRepository;
import org.rsreu.map.domain.campus.Campus;
import org.rsreu.map.domain.campus.Space;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Use case for searching rooms/spaces by query or category.
 */
public class FindRoomsUseCase {

    private final CampusRepository campusRepository;

    public FindRoomsUseCase(CampusRepository campusRepository) {
        this.campusRepository = campusRepository;
    }

    /**
     * Searches for spaces matching the given query. Matches the display name and
     * the optional description, case-insensitively.
     *
     * @param query search query string
     * @return list of matching spaces ordered by display name
     */
    public List<Space> findByQuery(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        Campus campus = campusRepository.getCampus();
        String lowerQuery = query.toLowerCase().trim();

        return campus.getAllSpaces().stream()
            .filter(space -> matchesQuery(space, lowerQuery))
            .sorted(Comparator.comparing(Space::displayName))
            .toList();
    }

    /**
     * Returns all spaces of a category, optionally restricted to one of its
     * subcategories, ordered by display name.
     *
     * @param category    category id from the manifest taxonomy
     * @param subcategory subcategory id, or {@code null} for the whole category
     */
    public List<Space> findByCategory(String category, String subcategory) {
        if (category == null) {
            return List.of();
        }

        Campus campus = campusRepository.getCampus();
        return campus.getAllSpaces().stream()
            .filter(space -> Objects.equals(space.category(), category))
            .filter(space -> subcategory == null || Objects.equals(space.subcategory(), subcategory))
            .sorted(Comparator.comparing(Space::displayName))
            .toList();
    }

    private boolean matchesQuery(Space space, String lowerQuery) {
        if (space.displayName().toLowerCase().contains(lowerQuery)) {
            return true;
        }
        String description = space.description();
        return description != null && description.toLowerCase().contains(lowerQuery);
    }
}
