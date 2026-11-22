package org.rsreu.map.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.Vector3;
import org.rsreu.map.input.CameraInputController;
import org.rsreu.map.input.Picking;
import org.rsreu.map.input.RoomPicker;
import org.rsreu.map.input.WorkArea;
import org.rsreu.map.render.RoomHighlighter;
import org.rsreu.map.render.RoomMaterialManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Work-area picking, pointer ownership and hover/selection boundary regression. */
public final class WorkAreaInputTest {

    public static void main(String[] args) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Workspace input regression");
        config.setWindowedMode(1280, 720);
        config.setInitialVisible(false);
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    checkInput();
                    System.out.println("WorkAreaInputTest: all checks passed");
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, config);
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    private static void checkInput() {
        WorkArea area = new WorkArea();
        require(!area.containsScreen(0, 0, 720), "uninitialized workspace rejects input");
        area.set(304, 30, 620, 650);
        require(area.containsScreen(304, 40, 720), "top-left inclusive edge");
        require(!area.containsScreen(924, 200, 720), "right edge excluded");
        require(!area.containsScreen(400, 690, 720), "bottom edge excluded");
        require(!area.containsScreen(303, 200, 720), "left dock excluded");
        require(!area.containsScreen(400, 39, 720), "top chrome excluded");

        PerspectiveCamera camera = new PerspectiveCamera(60, area.width, area.height);
        camera.position.set(0, 0, 10);
        camera.lookAt(Vector3.Zero);
        camera.near = 0.1f;
        camera.far = 100f;
        camera.update();
        require(Picking.getPickRay(camera, area, 100, 200) == null, "outside coordinates produce no pick ray");
        Vector3 rayDirection = new Vector3(Picking.getPickRay(camera, area, 614, 365).direction);
        require(rayDirection.dot(camera.direction) > 0.999f, "offset viewport center ray follows camera direction");

        ModelInstance instance = new ModelInstance(new Model());
        Node floor = new Node();
        floor.id = "floor3";
        Node room = new Node();
        room.id = "main_room_346_hitbox";
        MeshPart meshPart = new MeshPart();
        meshPart.primitiveType = GL20.GL_TRIANGLES;
        NodePart part = new NodePart(meshPart, new Material(ColorAttribute.createDiffuse(Color.WHITE)));
        room.parts.add(part);
        floor.addChild(room);
        instance.nodes.add(floor);
        instance.calculateTransforms();
        RoomPicker picker = new RoomPicker(instance.nodes);
        picker.boundsOf(room.id).set(new Vector3(-1, -1, -1), new Vector3(1, 1, 1));
        RoomMaterialManager materials = new RoomMaterialManager(instance, null);
        RoomHighlighter highlighter = new RoomHighlighter(camera, area, picker, materials);
        int[] selections = {0};
        highlighter.setRoomClickedHandler(hitbox -> selections[0]++);
        highlighter.onMouseMoved(614, 365);
        require(room.id.equals(highlighter.hoveredHitbox()), "visible room is hovered in viewport");
        highlighter.onMouseMoved(100, 200);
        require(highlighter.hoveredHitbox() == null, "dock pointer clears hover");
        highlighter.selectRoom(room.id);
        highlighter.onMouseMoved(614, 365);
        highlighter.clearHover();
        materials.update(100f);
        require("floor3".equals(highlighter.lockedFloor()) && part.enabled, "hover exit preserves selected room and floor");
        highlighter.onClick(100, 200);
        require("floor3".equals(highlighter.lockedFloor()), "outside click cannot clear selection");
        highlighter.showCategory(List.of(room.id));
        highlighter.onMouseMoved(614, 365);
        highlighter.clearHover();
        materials.update(100f);
        require(part.enabled, "category highlight survives pointer exit");

        CameraInputController controller = new CameraInputController(area);
        controller.setCamera(camera);
        controller.setRoomHighlighter(highlighter);
        require(!controller.touchDown(100, 200, 0, Input.Buttons.LEFT), "UI press cannot start pan");
        require(!controller.touchUp(614, 365, 0, Input.Buttons.LEFT) && selections[0] == 0,
            "release after UI press cannot select a room");
        require(controller.touchDown(614, 365, 0, Input.Buttons.LEFT), "viewport press accepted");
        controller.touchUp(100, 200, 0, Input.Buttons.LEFT);
        require(selections[0] == 0, "release outside workspace cannot select");
        controller.touchDown(614, 365, 0, Input.Buttons.LEFT);
        controller.touchDragged(100, 200, 0);
        require(!controller.touchDragged(614, 365, 0), "drag cannot resume after crossing dock boundary");
        controller.touchUp(614, 365, 0, Input.Buttons.LEFT);
        require(selections[0] == 0, "cancelled drag cannot become click");
        controller.setPointerBlockedPredicate((x, y) -> true);
        require(!controller.touchDown(614, 365, 0, Input.Buttons.RIGHT), "overlay blocks orbit inside viewport");
        require(!controller.scrolled(0, 1), "UI overlay blocks wheel zoom");
        controller.setPointerBlockedPredicate(null);
        controller.touchDown(614, 365, 0, Input.Buttons.LEFT);
        controller.touchUp(614, 365, 0, Input.Buttons.LEFT);
        require(selections[0] == 1, "matching in-viewport click still selects room");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
