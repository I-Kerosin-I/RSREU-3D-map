package org.rsreu.map.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Array;

/**
 * A floating list of actions shown next to the cursor or under a menu button:
 * the shared popup of the menu bar and the room context menu. One instance is
 * a singleton per screen — showing a menu hides the previously open one.
 */
public final class ContextMenu {

    /** One selectable row of the menu. */
    public record Item(String text, Runnable action) {
    }

    private final UiTheme theme;
    private final Table table;
    private final Array<TextButton> buttons = new Array<>();
    /** Set while the pointer is inside the menu, so an outside click can close it. */
    private boolean pointerInside;

    public ContextMenu(UiTheme theme) {
        this.theme = theme;
        table = new Table();
        table.setBackground(theme.popupBackground());
        table.pad(UiTheme.SPACE_XS);
        table.setVisible(false);
    }

    public Actor getActor() {
        return table;
    }

    public boolean isVisible() {
        return table.isVisible();
    }

    /**
     * Replaces the menu content and shows it with its top-left corner at the
     * given stage coordinates. Shows nothing when the item list is empty.
     */
    public void show(Stage stage, float x, float y, Iterable<Item> items) {
        table.clearChildren();
        buttons.clear();

        boolean any = false;
        for (Item item : items) {
            TextButton button = new TextButton(item.text(), theme.skin(), "menu-item");
            button.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
                @Override
                public void clicked(InputEvent event, float cx, float cy) {
                    hide();
                    if (item.action() != null) {
                        item.action().run();
                    }
                }

                @Override
                public boolean touchDown(InputEvent event, float cx, float cy, int pointer, int button) {
                    pointerInside = true;
                    return super.touchDown(event, cx, cy, pointer, button);
                }

                @Override
                public boolean mouseMoved(InputEvent event, float cx, float cy) {
                    pointerInside = true;
                    return super.mouseMoved(event, cx, cy);
                }
            });
            // The row highlight comes from the menu-item drawable padding:
            // fillX stretches it across the row, adjacent rows stay flush.
            table.add(button).left().growX().fillX();
            table.row();
            buttons.add(button);
            any = true;
        }

        if (!any) {
            return;
        }
        table.pack();
        table.setPosition(x, y - table.getHeight());
        stage.addActor(table);
        table.toFront();
        table.setVisible(true);
        pointerInside = false;
    }

    public void hide() {
        table.setVisible(false);
    }

    /**
     * Called from a stage-wide input hook: closes the menu when a press lands
     * outside of it. Returns true when the menu handled (and consumed) the
     * press because the pointer was over a menu row.
     */
    public boolean touchDown(float screenX, float screenY, int button) {
        if (!isVisible()) {
            return false;
        }
        Stage stage = table.getStage();
        if (stage == null) {
            return false;
        }
        Actor hit = stage.hit(screenX, screenY, true);
        boolean overMenu = hit != null && hit.isDescendantOf(table);
        if (!overMenu) {
            hide();
            return false;
        }
        return pointerInside;
    }

    /** Marks that the pointer currently rests inside the menu rows. */
    public void setPointerInside(boolean inside) {
        pointerInside = inside;
    }
}
