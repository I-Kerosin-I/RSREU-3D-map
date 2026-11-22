package org.rsreu.map.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import org.rsreu.map.application.usecase.FindRoomsUseCase;
import org.rsreu.map.content.InMemoryCampusRepository;
import org.rsreu.map.content.ManifestStore;
import org.rsreu.map.controller.MapController;
import org.rsreu.map.domain.campus.Campus;
import org.rsreu.map.domain.campus.CategoryTaxonomy;
import org.rsreu.map.ui.MapHud;
import org.rsreu.map.ui.PanelToggleButton;
import org.rsreu.map.ui.UiTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Standalone layout regression checks using the real desktop fonts and widgets. */
public final class UiLayoutTest {

    public static void main(String[] args) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("UI layout regression");
        config.setWindowedMode(1280, 720);
        config.setInitialVisible(false);
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    checkLayout();
                    System.out.println("UiLayoutTest: all checks passed");
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, config);
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    private static void checkLayout() {
        UiTheme theme = new UiTheme();
        InMemoryCampusRepository repository = new InMemoryCampusRepository(new Campus(List.of()));
        MapController controller = new MapController(new FindRoomsUseCase(repository), repository);
        MapHud hud = new MapHud(theme, controller, CategoryTaxonomy.EMPTY, new ManifestStore(), name -> "1");
        try {
            Stage stage = hud.getStage();
            layout(stage);
            checkMenuLabels(stage.getRoot());

            List<PanelToggleButton> buttons = new ArrayList<>();
            collect(stage.getRoot(), buttons);
            require(buttons.size() == 2, "exactly two panel controls");
            PanelToggleButton left = buttons.get(0);
            PanelToggleButton right = buttons.get(1);
            if (position(left).x > position(right).x) {
                PanelToggleButton swap = left;
                left = right;
                right = swap;
            }
            Vector2 openLeft = position(left);
            Vector2 rightHeader = null;
            checkSize(left);
            checkSize(right);
            Drawable leftArrow = arrowOf(left);
            Drawable rightArrow = arrowOf(right);

            for (int i = 0; i < 5; i++) {
                // Collapse the objects panel: the same button presses against
                // the left edge, reverses its arrow and stays interactive.
                left.fire(new ChangeEvent());
                layout(stage);
                checkSize(left);
                require(left.isVisible(), "collapsed left control is visible");
                require(close(position(left).x, 12), "collapsed left control inset from the window edge");
                require(close(position(left).y, openLeft.y), "collapsed left control keeps its expanded height");
                require(hitTest(stage, left), "collapsed left control is hitable");
                require(arrowOf(left) != leftArrow, "left arrow reverses when collapsed");
                // Collapse the schedule panel: the button takes its header spot
                // (12px panel inset), the arrow reverses, it stays hitable.
                right.fire(new ChangeEvent());
                layout(stage);
                checkSize(right);
                require(right.isVisible(), "toggled right control is visible");
                require(close(position(right).x, stage.getWidth() - 12 - PanelToggleButton.WIDTH),
                    "right control in its header spot");
                rightHeader = position(right);
                require(hitTest(stage, right), "toggled right control is hitable");
                System.out.println("right collapsed: pointsRight=" + right.isPointsRight() + " sameArrow=" + (arrowOf(right) == rightArrow));
                require(arrowOf(right) != rightArrow, "right arrow reverses when collapsed");
                // Expand both back: the buttons return to their home spots.
                left.fire(new ChangeEvent());
                right.fire(new ChangeEvent());
                layout(stage);
                checkSize(left);
                checkSize(right);
                require(left.isVisible() && right.isVisible(), "expanded controls are visible");
                Vector2 restored = position(left);
                require(close(restored.x, openLeft.x), "left control restores its search-row spot horizontally");
                require(close(restored.y, openLeft.y, 8), "left control restores its search-row spot vertically");
                require(hitTest(stage, left) && hitTest(stage, right), "expanded controls are hitable");
                require(arrowOf(left) == leftArrow && arrowOf(right) == rightArrow, "arrows restore");
                require(close(position(right).y, rightHeader.y),
                    "collapsed right control keeps its header height");
            }
            stage.getViewport().update(960, 600, true);
            layout(stage);
            checkSize(left);
            checkSize(right);
            require(close(position(right).x, 960 - 12 - PanelToggleButton.WIDTH),
                "right control follows resize");
        } finally {
            hud.dispose();
            theme.dispose();
        }
    }

    /** The menu labels must sit in the center of their highlight boxes. */
    private static void checkMenuLabels(Group group) {
        for (Actor actor : group.getChildren()) {
            if (actor instanceof TextButton button
                && List.of("Файл", "Режим", "Вид", "Сохранить").contains(button.getText().toString())) {
                Actor label = button.getLabel();
                require(close(label.getX(), button.getWidth() - label.getX() - label.getWidth()),
                    "menu label centered horizontally: " + button.getText());
                require(close(label.getY(), button.getHeight() - label.getY() - label.getHeight()),
                    "menu label centered vertically: " + button.getText());
            } else if (actor instanceof Group child) {
                checkMenuLabels(child);
            }
        }
    }

    private static void checkSize(PanelToggleButton button) {
        require(close(button.getWidth(), PanelToggleButton.WIDTH), "panel button width stays 28");
        require(close(button.getHeight(), PanelToggleButton.HEIGHT), "panel button height stays 36");
    }

    private static Drawable arrowOf(PanelToggleButton button) {
        return ((com.badlogic.gdx.scenes.scene2d.ui.Image) button.getChildren().first()).getDrawable();
    }

    private static boolean hitTest(Stage stage, Actor target) {
        Vector2 center = position(target).add(target.getWidth() / 2, target.getHeight() / 2);
        Actor hit = stage.hit(center.x, center.y, true);
        return hit != null && hit.isDescendantOf(target);
    }

    private static Vector2 position(Actor actor) {
        return actor.localToStageCoordinates(new Vector2());
    }

    private static void layout(Stage stage) {
        stage.act(0);
        for (Actor actor : stage.getActors()) {
            if (actor instanceof Table table) table.validate();
        }
        stage.getViewport().apply();
        stage.draw();
    }

    private static void collect(Group group, List<PanelToggleButton> result) {
        for (Actor actor : group.getChildren()) {
            if (actor instanceof PanelToggleButton button) result.add(button);
            else if (actor instanceof Group child) collect(child, result);
        }
    }

    private static boolean close(float actual, float expected) {
        return close(actual, expected, 0.5f);
    }

    private static boolean close(float actual, float expected, float epsilon) {
        return Math.abs(actual - expected) < epsilon;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
