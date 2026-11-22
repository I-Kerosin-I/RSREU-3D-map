package org.rsreu.map.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import org.rsreu.map.controller.MapController;
import org.rsreu.map.domain.campus.Space;

import java.util.List;

/**
 * The temporary result list shown under the search bar while a query is typed.
 * Shows every match; the panel scrolls. Clicking a row selects the object on
 * the map. Rows are rebuilt per search, not per frame. Rows rest quietly on
 * the panel background and highlight only under the cursor.
 */
public final class SearchResultsPanel {

    private final UiTheme theme;
    private final MapController controller;
    private final Table container;

    public SearchResultsPanel(UiTheme theme, MapController controller) {
        this.theme = theme;
        this.controller = controller;
        // padLeft keeps the rows away from the left scrollbar of the pane.
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
            container.add(label).left()
                .pad(UiTheme.SPACE_S, UiTheme.SPACE_M, UiTheme.SPACE_S, UiTheme.SPACE_M);
            container.row();
            return;
        }

        for (Space space : results) {
            container.add(resultRow(space)).growX().padBottom(UiTheme.SPACE_XS);
            container.row();
        }
    }

    /**
     * A result row: the row highlights as a whole (hover and clicks work
     * between the labels too), while the invisible row space right of the
     * labels still passes clicks through to the 3D scene.
     */
    private Table resultRow(Space space) {
        Table row = new Table();
        row.setTouchable(Touchable.enabled);
        row.left();
        row.add(new Label(space.displayName(), theme.skin(), "default")).left();
        row.add(new Label("эт. " + space.floorId().value(), theme.skin(), "hint"))
            .left().padLeft(UiTheme.SPACE_S).padRight(UiTheme.SPACE_S);

        row.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                controller.selectObject(space);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                row.setBackground(theme.rowHover());
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                row.setBackground((com.badlogic.gdx.scenes.scene2d.utils.Drawable) null);
            }
        });
        return row;
    }
}
