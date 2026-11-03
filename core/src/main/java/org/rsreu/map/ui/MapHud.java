package org.rsreu.map.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import org.rsreu.map.controller.MapController;
import org.rsreu.map.domain.campus.CategoryTaxonomy;

/**
 * The root UI container: owns the Scene2D {@link Stage}, composes the side
 * panel (search bar, search results, category tree) and the hover tooltip, and
 * forwards application state to the components. Widgets communicate only with
 * the {@link MapController}, never with the 3D scene.
 */
public final class MapHud implements Disposable {

    private static final float PANEL_WIDTH = 310f;
    private static final float PANEL_MARGIN = 12f;

    private final Stage stage;
    private final MapController controller;

    private final SearchBar searchBar;
    private final SearchResultsPanel resultsPanel;
    private final ObjectBrowserPanel browserPanel;
    private final RoomTooltip tooltip;

    private final ScrollPane resultsScroll;
    private final ScrollPane browserScroll;

    public MapHud(UiTheme theme, MapController controller, CategoryTaxonomy taxonomy) {
        this.controller = controller;

        stage = new Stage(new ScreenViewport());

        searchBar = new SearchBar(theme, controller);
        resultsPanel = new SearchResultsPanel(theme, controller);
        browserPanel = new ObjectBrowserPanel(theme, controller);
        browserPanel.rebuild(taxonomy);
        tooltip = new RoomTooltip(theme, stage);

        Table root = new Table();
        root.setFillParent(true);
        // Rows are aligned as a block; without this the side panel sits centered.
        root.align(Align.right);
        // The root only hosts the panels; it must never be hit by stage.hit,
        // otherwise every point of the screen would count as "over the UI".
        // childrenOnly keeps the panels (its children) fully interactive.
        root.setTouchable(Touchable.childrenOnly);
        stage.addActor(root);

        Table panel = new Table();
        panel.setBackground(theme.panelBackground());
        panel.pad(12f);
        root.add(panel).width(PANEL_WIDTH).growY().pad(PANEL_MARGIN).align(Align.right);

        panel.add(searchBar.getActor()).growX().height(36f).padBottom(10f);
        panel.row();

        // While a query is typed the results replace the category tree; the
        // Stack keeps the panel layout stable in both modes.
        Stack content = new Stack();
        browserScroll = browserPanel.getActor();
        resultsScroll = new ScrollPane(resultsPanel.getActor(), theme.skin());
        resultsScroll.setFadeScrollBars(false);
        content.add(browserScroll);
        content.add(resultsScroll);
        resultsScroll.setVisible(false);
        panel.add(content).grow();

        stage.addActor(tooltip.getActor());

        controller.addStateListener(state -> {
            resultsPanel.update(state.searchResults());
            refreshSearchMode();
            browserPanel.syncSelection(state.selectedRoomId(),
                state.selectedCategoryId(), state.selectedSubcategoryId());
        });

        // Escape in the search field unfocuses it; the camera controller still
        // receives the unconsumed event afterwards and clears the selection.
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    stage.setKeyboardFocus(null);
                }
                return false;
            }
        });
    }

    public Stage getStage() {
        return stage;
    }

    /** Whether a text field currently owns the keyboard (blocks camera WASD). */
    public boolean isTextFieldFocused() {
        return stage.getKeyboardFocus() instanceof TextField;
    }

    /** Advances and draws the UI. Called from the render loop after the 3D scene. */
    public void actAndDraw(float delta) {
        stage.act(delta);
        String hitbox = controller.hoveredHitbox();
        tooltip.update(hitbox, controller.findSpaceByHitbox(hitbox).orElse(null));
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    private void refreshSearchMode() {
        boolean searching = !searchBar.text().isBlank();
        resultsScroll.setVisible(searching);
        browserScroll.setVisible(!searching);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
