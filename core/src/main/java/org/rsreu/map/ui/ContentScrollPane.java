package org.rsreu.map.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;

/**
 * Scroll pane that separates the wheel zone from the click zone:
 * <ul>
 * <li>the wheel scrolls the pane while the cursor is anywhere over the visible
 * part of its content — including the gaps between the rows, so a fast scroll
 * never suddenly zooms the camera;</li>
 * <li>clicks and hover reach only the rows themselves ({@link #hit} delegates
 * to the content widget, which in our lists means the chips): the empty part of
 * the pane and the space right of the chips pass clicks through to the 3D
 * scene;</li>
 * <li>the hidden pane never answers hits at all, so the two stacked panes
 * (search results behind the category tree) do not shadow each other.</li>
 * </ul>
 * The wheel part works through the stage scroll focus, which is refreshed every
 * frame in {@link #act(float)} from the pointer position.
 */
public final class ContentScrollPane extends ScrollPane {

    private final Vector2 pointerStage = new Vector2();

    public ContentScrollPane(Actor widget, Skin skin) {
        super(widget, skin);
        // The scrollbar sits to the left of the content. The field has no
        // public setter in libGDX 1.12.1, hence the reflection.
        try {
            java.lang.reflect.Field field = ScrollPane.class.getDeclaredField("vScrollOnRight");
            field.setAccessible(true);
            field.set(this, false);
            invalidate();
        } catch (ReflectiveOperationException e) {
            // The right-side scrollbar is an acceptable fallback.
        }
    }

    /**
     * Keeps the stage scroll focus in sync with the pointer: claimed while the
     * cursor is over the visible content, released otherwise, so the wheel
     * returns to the 3D scene as soon as the cursor leaves the rows.
     */
    @Override
    public void act(float delta) {
        super.act(delta);
        Stage stage = getStage();
        if (stage == null) {
            return;
        }
        if (isPointerOverVisibleContent()) {
            if (stage.getScrollFocus() != this) {
                stage.setScrollFocus(this);
            }
        } else if (stage.getScrollFocus() == this) {
            stage.setScrollFocus(null);
        }
    }

    /**
     * Whether the cursor is over the visible part of this pane's content: the
     * rows together with the gaps between them, across the full column width.
     */
    private boolean isPointerOverVisibleContent() {
        Actor widget = getWidget();
        if (widget == null) {
            return false;
        }
        Stage stage = getStage();
        if (stage == null) {
            return false;
        }
        pointerStage.set(Gdx.input.getX(), Gdx.input.getY());
        stage.screenToStageCoordinates(pointerStage);
        stageToLocalCoordinates(pointerStage);

        if (pointerStage.x < 0 || pointerStage.x >= getWidth()
            || pointerStage.y < 0 || pointerStage.y >= getHeight()) {
            return false;
        }
        // The content is laid out from the top of the pane; below its last row
        // there is nothing to scroll and the wheel belongs to the camera.
        float visibleContentHeight = widget instanceof Layout
            ? Math.min(((Layout) widget).getPrefHeight(), getHeight())
            : getHeight();
        return pointerStage.y >= getHeight() - visibleContentHeight;
    }

    @Override
    public Actor hit(float x, float y, boolean touchable) {
        // A hidden pane (the search results hidden behind the tree in the Stack)
        // must not answer hits: Group.hit does not filter invisible children.
        if (!isVisible()) {
            return null;
        }
        if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) {
            return null;
        }
        Actor widget = getWidget();
        if (widget == null) {
            return null;
        }
        return widget.hit(x - widget.getX(), y - widget.getY(), touchable);
    }
}
