package org.rsreu.map.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import org.rsreu.map.application.usecase.FindRoomsUseCase;
import org.rsreu.map.content.InMemoryCampusRepository;
import org.rsreu.map.controller.MapController;
import org.rsreu.map.domain.campus.Building;
import org.rsreu.map.domain.campus.Campus;
import org.rsreu.map.domain.campus.CategoryDef;
import org.rsreu.map.domain.campus.CategoryTaxonomy;
import org.rsreu.map.domain.campus.Floor;
import org.rsreu.map.domain.campus.Space;
import org.rsreu.map.domain.campus.SpaceType;
import org.rsreu.map.domain.model.BuildingId;
import org.rsreu.map.domain.model.FloorId;
import org.rsreu.map.domain.model.RoomId;
import org.rsreu.map.ui.ObjectBrowserPanel;
import org.rsreu.map.ui.SearchBar;
import org.rsreu.map.ui.UiTheme;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Stable tree-row metrics and compact, symmetric search-icon layout. */
public final class UiNavigationTest {

    public static void main(String[] args) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Navigation regression");
        config.setWindowedMode(640, 480);
        config.setInitialVisible(false);
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    checkNavigation();
                    System.out.println("UiNavigationTest: all checks passed");
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, config);
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    private static void checkNavigation() {
        UiTheme theme = new UiTheme();
        Stage stage = new Stage();
        try {
            Space first = room("346");
            Space second = room("344");
            BuildingId buildingId = new BuildingId("main");
            Floor floor = new Floor(first.floorId(), buildingId, 3, 3f, List.of(first, second));
            Campus campus = new Campus(List.of(new Building(buildingId, "Главный корпус", List.of(floor))));
            InMemoryCampusRepository repository = new InMemoryCampusRepository(campus);
            MapController controller = new MapController(new FindRoomsUseCase(repository), repository);
            ObjectBrowserPanel browser = new ObjectBrowserPanel(theme, controller);
            browser.rebuild(new CategoryTaxonomy(List.of(new CategoryDef("rooms", "Аудитории", List.of()))));
            Table root = new Table();
            root.setFillParent(true);
            root.add(browser.getActor()).width(300).growY();
            stage.addActor(root);
            stage.draw();
            Container<?> firstRow = row(stage.getRoot(), "а.346");
            Container<?> secondRow = row(stage.getRoot(), "а.344");
            float x = firstRow.getX(), y = firstRow.getY();
            float width = firstRow.getWidth(), height = firstRow.getHeight(), nextY = secondRow.getY();
            Label label = (Label) firstRow.getActor();
            float labelX = label.getX();
            Object font = label.getStyle().font;
            require(labelX >= UiTheme.SPACE_S, "row text has horizontal padding");
            for (int i = 0; i < 5; i++) {
                hover(firstRow, true);
                stage.draw();
                checkRow(firstRow, secondRow, x, y, width, height, nextY);
                browser.syncSelection(first.id().value(), null, null);
                stage.draw();
                checkRow(firstRow, secondRow, x, y, width, height, nextY);
                require(label.getStyle().font == font && label.getStyle().fontColor.equals(UiTheme.ACCENT),
                    "selected row changes color, not typography");
                require(label.getX() == labelX, "selection never shifts text padding");
                browser.syncSelection(null, null, null);
                hover(firstRow, false);
                stage.draw();
                checkRow(firstRow, secondRow, x, y, width, height, nextY);
            }
            require(equalMetrics(theme.rowIdle(), theme.rowHover()) && equalMetrics(theme.rowHover(), theme.rowSelected()),
                "idle, hover and selection drawables have identical metrics");

            SearchBar search = new SearchBar(theme, controller);
            Table wrapper = (Table) search.getActor();
            wrapper.setSize(200, 36);
            wrapper.validate();
            Actor icon = wrapper.getChildren().get(0);
            require(Math.abs(icon.getX() - icon.getY()) < 0.5f, "magnifier left and vertical insets match");
            TextField field = (TextField) wrapper.getChildren().get(1);
            require(wrapper.getCell(field).getMinWidth() <= field.getPrefWidth() * 0.75f,
                "search text minimum width reduced by 25 percent");
            float iconX = icon.getX();
            wrapper.setSize(wrapper.getMinWidth(), 36);
            wrapper.validate();
            require(Math.abs(icon.getX() - iconX) < 0.5f, "search icon inset survives compression");
        } finally {
            stage.dispose();
            theme.dispose();
        }
    }

    private static Space room(String number) {
        return new Space(new RoomId(number), "а." + number, new FloorId("3"), SpaceType.ROOM,
            "main_room_" + number + "_hitbox", null, "rooms", null, null, null, null, true);
    }

    private static void checkRow(Container<?> first, Container<?> second, float x, float y,
                                 float width, float height, float nextY) {
        require(first.getX() == x && first.getY() == y && first.getWidth() == width && first.getHeight() == height,
            "row bounds stay fixed during hover and selection");
        require(second.getY() == nextY, "neighboring tree rows never move");
    }

    private static Container<?> row(Group group, String text) {
        for (Actor actor : group.getChildren()) {
            if (actor instanceof Container<?> container && container.getActor() instanceof Label label
                && label.getText().toString().equals(text)) return container;
            if (actor instanceof Group child) {
                Container<?> result = row(child, text);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static void hover(Actor actor, boolean entering) {
        InputEvent event = new InputEvent();
        event.setType(entering ? InputEvent.Type.enter : InputEvent.Type.exit);
        event.setStage(actor.getStage());
        event.setPointer(-1);
        actor.fire(event);
    }

    private static boolean equalMetrics(Drawable a, Drawable b) {
        return a.getMinWidth() == b.getMinWidth() && a.getMinHeight() == b.getMinHeight()
            && a.getLeftWidth() == b.getLeftWidth() && a.getRightWidth() == b.getRightWidth()
            && a.getTopHeight() == b.getTopHeight() && a.getBottomHeight() == b.getBottomHeight();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
