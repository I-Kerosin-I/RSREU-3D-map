package org.rsreu.map.application.port;

import org.rsreu.map.domain.campus.Building;
import org.rsreu.map.domain.campus.Campus;
import org.rsreu.map.domain.campus.Floor;
import org.rsreu.map.domain.campus.Space;
import org.rsreu.map.domain.model.BuildingId;
import org.rsreu.map.domain.model.FloorId;
import org.rsreu.map.domain.model.RoomId;

import java.util.Optional;

/**
 * Repository interface for accessing campus data.
 * Implemented in the core layer.
 */
public interface CampusRepository {

    /**
     * Returns the entire campus data.
     */
    Campus getCampus();

    /**
     * Finds a space by its glTF model node name.
     */
    Optional<Space> findSpaceByNodeName(String modelNodeName);

    /**
     * Finds a building by its ID.
     */
    Optional<Building> findBuilding(BuildingId id);

    /**
     * Finds a floor by its ID.
     */
    Optional<Floor> findFloor(FloorId id);

    /**
     * Finds a space (room) by its ID.
     */
    Optional<Space> findSpace(RoomId id);
}