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
            for (String style : List.of("menu-item", "top-bar")) {
                TextButton.TextButtonStyle buttonStyle = theme.skin().get(style, TextButton.TextButtonStyle.class);
                checkPadding(buttonStyle.up);
                checkPadding(buttonStyle.over);
                checkPadding(buttonStyle.down);
            }
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
            Vector2 closedRight = position(right);
            checkSize(left);
            checkSize(right);
            require(close(openLeft.y, closedRight.y), "panel controls align with the search row");
            require(close(closedRight.x, stage.getWidth() - 12 - PanelToggleButton.WIDTH), "right control at edge");
            Drawable leftArrow = ((com.badlogic.gdx.scenes.scene2d.ui.Image) left.getChildren().first()).getDrawable();
            Drawable rightArrow = ((com.badlogic.gdx.scenes.scene2d.ui.Image) right.getChildren().first()).getDrawable();
            for (int i = 0; i < 5; i++) {
                left.fire(new ChangeEvent());
                layout(stage);
                checkSize(left);
                require(close(position(left).x, 12), "collapsed left control at edge");
                require(close(position(left).y, openLeft.y), "left control keeps vertical position");
                Vector2 center = position(left).add(PanelToggleButton.WIDTH / 2, PanelToggleButton.HEIGHT / 2);
                Actor hit = stage.hit(center.x, center.y, true);
                require(hit != null && hit.isDescendantOf(left), "collapsed control hit area follows the button");
                require(((com.badlogic.gdx.scenes.scene2d.ui.Image) left.getChildren().first()).getDrawable() != leftArrow,
                    "left arrow reverses when collapsed");
                right.fire(new ChangeEvent());
                layout(stage);
                checkSize(right);
                require(((com.badlogic.gdx.scenes.scene2d.ui.Image) right.getChildren().first()).getDrawable() != rightArrow,
                    "right arrow reverses when expanded");
                left.fire(new ChangeEvent());
                right.fire(new ChangeEvent());
                layout(stage);
                checkSize(left);
                checkSize(right);
                require(position(left).epsilonEquals(openLeft, 0.1f), "left control restores its search-row position");
                require(position(right).epsilonEquals(closedRight, 0.1f), "right control restores its edge position");
                require(left.getStage() == stage && right.getStage() == stage, "buttons remain interactive");
            }
            stage.getViewport().update(960, 600, true);
            layout(stage);
            checkSize(left);
            checkSize(right);
            require(close(position(right).x, 960 - 12 - PanelToggleButton.WIDTH), "right control follows resize");
        } finally {
            hud.dispose();
            theme.dispose();
        }
    }

    private static void checkMenuLabels(Group group) {
        for (Actor actor : group.getChildren()) {
            if (actor instanceof TextButton button
                && List.of("Файл", "Режим", "Вид", "Сохранить").contains(button.getText().toString())) {
                Actor label = button.getLabel();
                require(close(label.getX(), button.getWidth() - label.getX() - label.getWidth()),
                    "menu label centered horizontally: " + button.getText());
                require(close(label.getY(), button.getHeight() - label.getY() - label.getHeight()),
                    "menu label centered vertically: " + button.getText() + " buttonHeight=" + button.getHeight()
                        + " labelY=" + label.getY() + " labelHeight=" + label.getHeight());
            } else if (actor instanceof Group child) {
                checkMenuLabels(child);
            }
        }
    }

    private static void checkPadding(Drawable drawable) {
        require(close(drawable.getLeftWidth(), 10) && close(drawable.getRightWidth(), 10), "symmetric horizontal menu padding");
        require(close(drawable.getTopHeight(), 4) && close(drawable.getBottomHeight(), 4), "symmetric vertical menu padding");
    }

    private static void checkSize(PanelToggleButton button) {
        require(close(button.getWidth(), PanelToggleButton.WIDTH), "panel button width stays 28");
        require(close(button.getHeight(), PanelToggleButton.HEIGHT), "panel button height stays 36");
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
        return Math.abs(actual - expected) < 0.1f;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
