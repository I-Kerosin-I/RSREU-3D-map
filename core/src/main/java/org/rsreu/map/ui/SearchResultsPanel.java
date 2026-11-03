package org.rsreu.map.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

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
        container = new Table().top().left();
    }

    public Actor getActor() {
        return container;
    }

    /** Replaces the rows with the given search results. */
    public void update(List<Space> results) {
        container.clearChildren();

        if (results.isEmpty()) {
            Label empty = new Label("Ничего не найдено", theme.skin(), "hint");
            container.add(empty).left().pad(8f);
            container.row();
            return;
        }

        for (Space space : results) {
            container.add(resultRow(space)).growX();
            container.row();
        }
    }

    private Table resultRow(Space space) {
        Table row = new Table();
        row.pad(6f, 8f, 6f, 8f);
        row.add(new Label(space.displayName(), theme.skin(), "default")).left().growX();
        row.add(new Label("эт. " + space.floorId().value(), theme.skin(), "hint")).right().padLeft(8f);

        row.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                controller.selectObject(space);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                row.setBackground(theme.flat(UiTheme.ROW_HOVER));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                row.setBackground((Drawable) null);
            }
        });
        return row;
    }
}
