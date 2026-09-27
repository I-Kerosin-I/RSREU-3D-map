package org.rsreu.map.application.usecase;

import org.rsreu.map.application.port.CampusRepository;
import org.rsreu.map.domain.campus.Space;
import org.rsreu.map.domain.model.RoomId;

import java.util.Optional;

/**
 * Use case for selecting a specific room.
 */
public class SelectRoomUseCase {

    private final CampusRepository campusRepository;

    public SelectRoomUseCase(CampusRepository campusRepository) {
        this.campusRepository = campusRepository;
    }

    /**
     * Selects a room by its ID.
     *
     * @param roomId the room ID to select
     * @return the selected space if found
     */
    public Optional<Space> execute(RoomId roomId) {
        if (roomId == null) {
            return Optional.empty();
        }
        return campusRepository.findSpace(roomId);
    }
}