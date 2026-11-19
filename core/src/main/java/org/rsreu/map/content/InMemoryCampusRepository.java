package org.rsreu.map.content;

import org.rsreu.map.application.port.CampusRepository;
import org.rsreu.map.domain.campus.Building;
import org.rsreu.map.domain.campus.Campus;
import org.rsreu.map.domain.campus.Floor;
import org.rsreu.map.domain.campus.Space;
import org.rsreu.map.domain.model.BuildingId;
import org.rsreu.map.domain.model.FloorId;
import org.rsreu.map.domain.model.RoomId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only {@link CampusRepository} over the Campus parsed from the manifest.
 * <p>
 * Spaces are indexed by their glTF hitbox node name, so the hover tooltip and
 * the selection commands resolve a picked room in O(1).
 */
public final class InMemoryCampusRepository implements CampusRepository {

    private Campus campus;
    private final Map<String, Space> spacesByNodeName = new HashMap<>();

    public InMemoryCampusRepository(Campus campus) {
        this.campus = campus;
        reindex();
    }

    /**
     * Swaps the campus data and rebuilds the node name index. Used after the
     * room editor persists changes, so the UI serves the fresh data without an
     * application restart.
     */
    public void replaceCampus(Campus campus) {
        if (campus == null) {
            throw new IllegalArgumentException("Campus cannot be null");
        }
        this.campus = campus;
        reindex();
    }

    private void reindex() {
        spacesByNodeName.clear();
        for (Space space : campus.getAllSpaces()) {
            spacesByNodeName.put(space.modelNodeName(), space);
        }
    }

    @Override
    public Campus getCampus() {
        return campus;
    }

    @Override
    public Optional<Space> findSpaceByNodeName(String modelNodeName) {
        return Optional.ofNullable(spacesByNodeName.get(modelNodeName));
    }

    /** Finds a space by its stable room id. */
    public Optional<Space> findSpaceById(String roomId) {
        if (roomId == null) {
            return Optional.empty();
        }
        return campus.getAllSpaces().stream()
            .filter(space -> space.id().value().equals(roomId))
            .findFirst();
    }

    @Override
    public Optional<Building> findBuilding(BuildingId id) {
        return campus.findBuilding(id);
    }

    @Override
    public Optional<Floor> findFloor(FloorId id) {
        return campus.buildings().stream()
            .flatMap(building -> building.floors().stream())
            .filter(floor -> floor.id().equals(id))
            .findFirst();
    }

    @Override
    public Optional<Space> findSpace(RoomId id) {
        return campus.getAllSpaces().stream()
            .filter(space -> space.id().equals(id))
            .findFirst();
    }
}
