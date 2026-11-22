package org.rsreu.map.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;

import org.rsreu.map.controller.MapController;

/**
 * The search input of the side panel: a bordered field with the magnifier
 * icon, rendered as one composite control. The field itself carries no
 * background — the outline belongs to the wrapper, which switches it on focus.
 * Only enters and forwards the query; the search itself runs in the
 * application layer ({@link MapController#search}).
 */
public final class SearchBar {

    private final Table wrapper;
    private final TextField field;

    public SearchBar(UiTheme theme, MapController controller) {
        field = new TextField("", theme.skin(), "bare");
        field.setMessageText("Поиск объектов…");

        wrapper = new Table();
        wrapper.setBackground(theme.fieldBackground());

        Image icon = new Image(theme.skin(), "icon-search");
        wrapper.add(icon).size(14f).padLeft(UiTheme.SPACE_M + 2).padRight(UiTheme.SPACE_S);
        wrapper.add(field).growX().height(UiTheme.CONTROL_H);

        field.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                controller.search(field.getText());
            }
        });
        // The focus outline lives on the composite: swap the wrapper's
        // background when the field gains or loses the keyboard.
        field.addListener(new FocusListener() {
            @Override
            public void keyboardFocusChanged(FocusEvent event, Actor actor, boolean focused) {
                wrapper.setBackground(focused
                    ? theme.fieldBackgroundFocused()
                    : theme.fieldBackground());
            }
        });
    }

    public Actor getActor() {
        return wrapper;
    }

    public String text() {
        return field.getText();
    }
}
