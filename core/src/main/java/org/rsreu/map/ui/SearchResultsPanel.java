package org.rsreu.map.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import org.rsreu.map.controller.MapController;
import org.rsreu.map.domain.campus.Space;

import java.util.List;

/**
 * The temporary result list shown under the search bar while a query is typed.
 * Shows every match; the panel scrolls. Clicking a row selects the object on
 * the map. Rows are rebuilt per search, not per frame.
 */
public final class SearchResultsPanel {

    private final UiTheme theme;
    private final MapController controller;
    private final Table container;

    public SearchResultsPanel(UiTheme theme, MapController controller) {
        this.theme = theme;
        this.controller = controller;
        // padLeft keeps the chips away from the left scrollbar of the pane.
        container = new Table().top().left().padLeft(5f);
    }

    public Actor getActor() {
        return container;
    }

    /** Replaces the rows with the given search results. */
    public void update(List<Space> results) {
        container.clearChildren();

        if (results.isEmpty()) {
            Label label = new Label("Ничего не найдено", theme.skin(), "hint");
            Container<Label> chip = new Container<>(label);
            chip.setBackground(theme.chipBackground());
            container.add(chip).left().padBottom(4f);
            container.row();
            return;
        }

        for (Space space : results) {
            container.add(resultRow(space)).growX().padBottom(4f);
            container.row();
        }
    }

    /**
     * A result row: the whole row width is the hit area (hover and clicks work
     * between the labels too), while the floating chip hugging the text is its
     * visual part, Fusion browser style.
     */
    private Table resultRow(Space space) {
        Table row = new Table();
        // The row stretches to the full list width, but only the chip is the
        // hit area: Tables are Touchable.childrenOnly by default, so the
        // invisible row space right of the chip passes clicks and hover
        // through to the 3D scene. The chip itself is hittable as a whole —
        // including the gap between the labels.
        row.left();
        Table chip = new Table();
        chip.setTouchable(Touchable.enabled);
        chip.setBackground(theme.chipBackground());
        chip.add(new Label(space.displayName(), theme.skin(), "default")).left();
        chip.add(new Label("эт. " + space.floorId().value(), theme.skin(), "hint")).left().padLeft(8f);
        row.add(chip).left();

        row.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                controller.selectObject(space);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                chip.setBackground(theme.chipHover());
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                chip.setBackground(theme.chipBackground());
            }
        });
        return row;
    }
}
