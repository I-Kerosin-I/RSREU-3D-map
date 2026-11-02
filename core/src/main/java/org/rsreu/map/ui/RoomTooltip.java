package org.rsreu.map.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import org.rsreu.map.domain.campus.Space;

/**
 * The hover tooltip: shows the name and description of the room under the
 * cursor next to the pointer. Pure screen-space Scene2D; it only reads the
 * hover state through the controller and never touches the 3D scene. Hidden
 * while the cursor is over any interactive UI, and it is not touchable itself,
 * so it never blocks the map picking.
 */
public final class RoomTooltip {

    private static final float WIDTH = 260f;
    private static final float OFFSET = 18f;
    private static final float MARGIN = 6f;

    private final Stage stage;
    private final Table container;
    private final Label titleLabel;
    private final Label descriptionLabel;
    private final Vector2 cursor = new Vector2();

    public RoomTooltip(UiTheme theme, Stage stage) {
        this.stage = stage;

        container = new Table();
        container.setBackground(theme.tooltipBackground());
        container.pad(10f, 12f, 10f, 12f);

        titleLabel = new Label("", theme.skin(), "title");
        descriptionLabel = new Label("", theme.skin(), "hint");
        descriptionLabel.setWrap(true);

        container.add(titleLabel).left().row();
        container.add(descriptionLabel).left().width(WIDTH).row();

        container.setVisible(false);
        // Not touchable: the tooltip is never hit by stage.hit, so it neither
        // blocks the 3D picking under it nor hides/reopens in a flicker loop.
        container.setTouchable(Touchable.disabled);
    }

    public Actor getActor() {
        return container;
    }

    /**
     * Advances the tooltip for this frame: shows it near the cursor for the
     * hovered room, hides it otherwise.
     *
     * @param hitbox the picked hitbox node name, or {@code null}
     * @param space  the manifest metadata of the hitbox, or {@code null}
     */
    public void update(String hitbox, Space space) {
        if (hitbox == null || space == null || cursorOverUi()) {
            container.setVisible(false);
            return;
        }

        titleLabel.setText(space.displayName());
        String description = space.description();
        descriptionLabel.setText(description != null ? description : "");
        descriptionLabel.setVisible(description != null);
        container.pack();

        stage.screenToStageCoordinates(cursor.set(Gdx.input.getX(), Gdx.input.getY()));
        float x = cursor.x + OFFSET;
        float y = cursor.y - container.getHeight() - OFFSET;
        x = clamp(x, MARGIN, stage.getWidth() - container.getWidth() - MARGIN);
        y = clamp(y, MARGIN, stage.getHeight() - container.getHeight() - MARGIN);
        if (cursor.y - container.getHeight() - OFFSET < MARGIN) {
            // Does not fit below the cursor: show above it.
            y = clamp(cursor.y + OFFSET, MARGIN, stage.getHeight() - container.getHeight() - MARGIN);
        }
        container.setPosition(x, y);
        container.setVisible(true);
    }

    private boolean cursorOverUi() {
        stage.screenToStageCoordinates(cursor.set(Gdx.input.getX(), Gdx.input.getY()));
        return stage.hit(cursor.x, cursor.y, true) != null;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
