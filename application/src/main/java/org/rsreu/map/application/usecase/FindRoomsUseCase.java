package org.rsreu.map.application.usecase;

import org.rsreu.map.application.port.CampusRepository;
import org.rsreu.map.domain.campus.Campus;
import org.rsreu.map.domain.campus.Space;

import java.util.List;

/**
 * Use case for searching rooms/spaces by query.
 */
public class FindRoomsUseCase {

    private final CampusRepository campusRepository;

    public FindRoomsUseCase(CampusRepository campusRepository) {
        this.campusRepository = campusRepository;
    }

    /**
     * Searches for spaces matching the given query.
     * Currently searches by display name containing the query (case-insensitive).
     *
     * @param query search query string
     * @return list of matching spaces
     */
    public List<Space> findByQuery(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        Campus campus = campusRepository.getCampus();
        String lowerQuery = query.toLowerCase();

        return campus.getAllSpaces().stream()
            .filter(space -> space.displayName().toLowerCase().contains(lowerQuery))
            .toList();
    }
}