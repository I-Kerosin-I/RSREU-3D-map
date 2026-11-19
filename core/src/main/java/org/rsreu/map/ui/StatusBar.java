package org.rsreu.map.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

/**
 * The classic bottom status strip: current mode on the left, contextual hint
 * (the room under the cursor) on the right, transient messages on top of the
 * mode text for a few seconds.
 */
public final class StatusBar {

    private static final float TRANSIENT_SECONDS = 5f;

    private final Table table;
    private final Label leftLabel;
    private final Label rightLabel;

    private String mode = "Режим: обычный";
    private String transientMessage;
    private float transientLeft;
    private String hint = "";

    public StatusBar(UiTheme theme) {
        table = new Table();
        table.setBackground(theme.panelBackground());
        table.setTouchable(Touchable.enabled);
        table.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                     float x, float y, int pointer, int button) {
                // Swallow presses on the strip: no accidental camera drags.
                return true;
            }
        });

        leftLabel = new Label(mode, theme.skin(), "hint");
        rightLabel = new Label("", theme.skin(), "hint");
        rightLabel.setEllipsis(true);

        table.add(leftLabel).left().pad(4, 8, 4, 8);
        Table spacer = new Table();
        table.add(spacer).growX();
        table.add(rightLabel).right().pad(4, 8, 4, 8).maxWidth(600);
    }

    public Actor getActor() {
        return table;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    /** Shows a short-lived message (save results, errors) on the left side. */
    public void setTransient(String message) {
        transientMessage = message;
        transientLeft = TRANSIENT_SECONDS;
        refresh();
    }

    /** Sets the contextual hint on the right side (truncated when too long). */
    public void setHint(String hint) {
        if (!hint.equals(this.hint)) {
            this.hint = hint;
            refresh();
        }
    }

    /** Advances the transient message timer; called from the render loop. */
    public void act(float delta) {
        if (transientLeft > 0f) {
            transientLeft -= delta;
            if (transientLeft <= 0f) {
                transientMessage = null;
                refresh();
            }
        }
    }

    private void refresh() {
        leftLabel.setText(transientMessage != null ? transientMessage : mode);
        rightLabel.setText(hint);
    }
}
