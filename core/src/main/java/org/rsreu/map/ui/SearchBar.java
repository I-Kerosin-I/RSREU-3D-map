package org.rsreu.map.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import org.rsreu.map.controller.MapController;

/**
 * The search input of the side panel. Only enters and forwards the query; the
 * search itself runs in the application layer ({@link MapController#search}).
 */
public final class SearchBar {

    private final TextField field;

    public SearchBar(UiTheme theme, MapController controller) {
        field = new TextField("", theme.skin(), "field");
        field.setMessageText("Поиск объектов…");
        // Searches on every keystroke: the dataset is one building, this is cheap.
        field.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                controller.search(field.getText());
            }
        });
    }

    public Actor getActor() {
        return field;
    }

    public String text() {
        return field.getText();
    }
}
