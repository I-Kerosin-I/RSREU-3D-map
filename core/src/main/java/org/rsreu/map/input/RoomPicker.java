package org.rsreu.map.input;

import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import java.util.regex.Pattern;

/**
 * Ray picks rooms of a building by testing the pick ray against the world-space
 * bounding boxes of the model's invisible {@code *_hitbox} nodes.
 * <p>
 * The hitbox nodes are children of the floor container nodes, so a hit yields
 * both the room (from the hitbox name) and the floor (from the parent node).
 * <p>
 * The world bounds of every hitbox are computed once at construction time from
 * the mesh data and the node transform. {@link Node#calculateBoundingBox} can
 * not be used here: it skips node parts whose {@code enabled} flag is cleared,
 * and the material manager disables hitbox parts to keep them out of rendering.
 * The model is static, so the cached bounds stay valid for its lifetime.
 */
public final class RoomPicker {

    private static final String HITBOX_SUFFIX = "_hitbox";
    private static final Pattern FLOOR_PATTERN = Pattern.compile("floor\\d+");

    private final Array<Node> hitboxes = new Array<>();
    private final ObjectMap<String, String> parentFloor = new ObjectMap<>();
    private final ObjectMap<String, BoundingBox> worldBounds = new ObjectMap<>();

    private final BoundingBox meshBounds = new BoundingBox();
    private final Vector3 intersection = new Vector3();

    /**
     * @param rootNodes the model instance's root nodes
     */
    public RoomPicker(Array<Node> rootNodes) {
        collect(rootNodes, null);
    }

    /**
     * Returns the name of the hitbox node closest along the ray, or {@code null}
     * when the ray misses every hitbox.
     *
     * @param ray pick ray in world space, may be null
     */
    public String pick(Ray ray) {
        if (ray == null) {
            return null;
        }
        String best = null;
        float bestDistance = Float.MAX_VALUE;
        for (Node hitbox : hitboxes) {
            BoundingBox bounds = worldBounds.get(hitbox.id);
            if (bounds == null) {
                continue;
            }
            if (Intersector.intersectRayBounds(ray, bounds, intersection)) {
                float distance = ray.origin.dst2(intersection);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = hitbox.id;
                }
            }
        }
        return best;
    }

    /**
     * The floor container node that owns a hitbox, e.g. {@code "floor2"} for
     * {@code "main_room_210_hitbox"}.
     *
     * @param hitboxNodeName node name of the hitbox
     */
    public String floorOf(String hitboxNodeName) {
        return hitboxNodeName == null ? null : parentFloor.get(hitboxNodeName);
    }

    /**
     * Strips the {@code _hitbox} suffix to recover the stable room identifier,
     * e.g. {@code "main_room_210"} from {@code "main_room_210_hitbox"}.
     *
     * @param hitboxNodeName node name of the hitbox
     */
    public static String toRoomId(String hitboxNodeName) {
        if (hitboxNodeName == null || !hitboxNodeName.endsWith(HITBOX_SUFFIX)) {
            return null;
        }
        return hitboxNodeName.substring(0, hitboxNodeName.length() - HITBOX_SUFFIX.length());
    }

    private void collect(Iterable<Node> nodes, String currentFloor) {
        for (Node node : nodes) {
            String nodeFloor = isFloor(node.id) ? node.id : currentFloor;
            if (isHitbox(node.id)) {
                hitboxes.add(node);
                parentFloor.put(node.id, nodeFloor);
                worldBounds.put(node.id, computeWorldBounds(node));
            }
            if (node.hasChildren()) {
                collect(node.getChildren(), nodeFloor);
            }
        }
    }

    /**
     * Computes the world-space bounds of a node's mesh parts directly from the
     * mesh data, bypassing {@link Node#extendBoundingBox}, which ignores parts
     * with a cleared {@code enabled} flag.
     */
    private BoundingBox computeWorldBounds(Node node) {
        node.calculateWorldTransform();
        BoundingBox out = new BoundingBox().inf();
        for (NodePart part : node.parts) {
            MeshPart meshPart = part.meshPart;
            if (meshPart != null && meshPart.mesh != null) {
                meshPart.mesh.extendBoundingBox(out, meshPart.offset, meshPart.size,
                    node.globalTransform);
            }
        }
        return out;
    }

    private static boolean isFloor(String name) {
        return name != null && FLOOR_PATTERN.matcher(name).matches();
    }

    private static boolean isHitbox(String name) {
        return name != null && name.endsWith(HITBOX_SUFFIX);
    }
}
