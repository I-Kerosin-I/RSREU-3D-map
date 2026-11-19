package org.rsreu.map.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

/** Compact side-panel control with the same geometry in both panel states. */
public final class PanelToggleButton extends Button {

    public static final float WIDTH = 28f;
    public static final float HEIGHT = 36f;

    private final UiTheme theme;
    private final Image arrow;

    public PanelToggleButton(UiTheme theme, boolean pointsRight, Runnable onClick) {
        super(theme.skin(), "panel-toggle");
        this.theme = theme;
        arrow = new Image();
        add(arrow).size(12f);
        setPointsRight(pointsRight);
        setSize(WIDTH, HEIGHT);
        addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                onClick.run();
            }
        });
    }

    public void setPointsRight(boolean pointsRight) {
        arrow.setDrawable(theme.skin().getDrawable(pointsRight
            ? "panel-chevron-right" : "panel-chevron-left"));
    }
}
