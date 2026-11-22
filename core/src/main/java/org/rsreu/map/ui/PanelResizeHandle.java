package org.rsreu.map.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/** Inner dock edge: dragging resizes the panel without passing input to the map. */
public final class PanelResizeHandle extends Widget {

    public static final float WIDTH = 6f;

    private final Drawable normal;
    private final Drawable active;
    private boolean hovered;
    private boolean dragging;

    public PanelResizeHandle(UiTheme theme, boolean leftPanel, DoubleSupplier width,
                             DoubleConsumer resize) {
        normal = theme.flat(UiTheme.BORDER);
        active = theme.flat(UiTheme.ACCENT);
        setTouchable(Touchable.enabled);
        addListener(new InputListener() {
            private float startX;
            private double startWidth;
            private int dragPointer = -1;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (button != Input.Buttons.LEFT || dragPointer != -1) return false;
                dragPointer = pointer;
                startX = event.getStageX();
                startWidth = width.getAsDouble();
                dragging = true;
                event.stop();
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (pointer != dragPointer) return;
                float delta = event.getStageX() - startX;
                resize.accept(startWidth + (leftPanel ? delta : -delta));
                event.stop();
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (pointer != dragPointer) return;
                dragPointer = -1;
                dragging = false;
                event.stop();
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer,
                              com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                if (pointer == -1) hovered = true;
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer,
                             com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                if (pointer == -1) hovered = false;
            }
        });
    }

    @Override
    public float getPrefWidth() {
        return WIDTH;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.setColor(getColor().r, getColor().g, getColor().b, getColor().a * parentAlpha);
        boolean highlighted = hovered || dragging;
        float lineWidth = highlighted ? 2f : 1f;
        (highlighted ? active : normal).draw(batch,
            getX() + (getWidth() - lineWidth) / 2f, getY(), lineWidth, getHeight());
    }
}
