package org.rsreu.map.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import org.rsreu.map.controller.MapController;
import org.rsreu.map.content.ManifestStore;
import org.rsreu.map.domain.campus.CategoryTaxonomy;
import org.rsreu.map.domain.campus.Space;
import org.rsreu.map.schedule.RsreuScheduleService;

import java.util.function.Function;

/**
 * The root UI container: owns the Scene2D {@link Stage}, composes the classic
 * chrome (top menu bar, bottom status bar) and the docked side panels (search
 * + category tree on the left, the room inspector with the schedule on the
 * right), the hover tooltip and the room editor mode pieces, and forwards
 * application state to the components. Widgets communicate only with the
 * {@link MapController}, never with the 3D scene.
 * <p>
 * The chrome occupies dedicated top and bottom bands outside the 3D work area:
 * the game's render loop reads {@link #topChromeHeight()} and
 * {@link #bottomChromeHeight()} to keep the GL viewport and the camera inside
 * the rectangle between the bands. The side panels dock against the edges of
 * that work area and are separated from the 3D scene by a hairline border.
 */
public final class MapHud implements Disposable {

    private static final float PANEL_WIDTH = 304f;
    private static final float SCHEDULE_WIDTH = 356f;

    private final Stage stage;
    private final MapController controller;
    private final ManifestStore manifestStore;
    private final Function<String, String> floorIdResolver;

    private final SearchBar searchBar;
    private final SearchResultsPanel resultsPanel;
    private final ObjectBrowserPanel browserPanel;
    private final SchedulePanel schedulePanel;
    private final RoomTooltip tooltip;
    private final TopBar topBar;
    private final StatusBar statusBar;
    private final ContextMenu contextMenu;
    private final RoomEditorDialog editorDialog;

    private final Table objectsPanel;
    private final Table searchRow;
    private final PanelToggleButton objectsToggle;
    private final PanelToggleButton scheduleToggle;
    private final Cell<Actor> objectsCell;
    private final Cell<Actor> scheduleCell;
    private final Cell<Actor> searchToggleCell;
    private final ScrollPane resultsScroll;
    private final ScrollPane browserScroll;

    private String lastHint = "";

    public MapHud(UiTheme theme, MapController controller, CategoryTaxonomy taxonomy,
                  ManifestStore manifestStore, Function<String, String> floorIdResolver) {
        this.controller = controller;
        this.manifestStore = manifestStore;
        this.floorIdResolver = floorIdResolver;

        stage = new Stage(new ScreenViewport());

        searchBar = new SearchBar(theme, controller);
        resultsPanel = new SearchResultsPanel(theme, controller);
        browserPanel = new ObjectBrowserPanel(theme, controller);
        browserPanel.rebuild(taxonomy);
        schedulePanel = new SchedulePanel(theme, new RsreuScheduleService());
        tooltip = new RoomTooltip(theme, stage);
        contextMenu = new ContextMenu(theme);
        editorDialog = new RoomEditorDialog(theme, taxonomy, this::onRoomSaved);
        // Right-click on a room row of the hierarchy: the same context menu as
        // on a 3D room, but only in the editor mode.
        browserPanel.setRoomContextMenuHandler((space, stageX, stageY) -> {
            if (isEditMode()) {
                showRoomContextMenu(space, stageX, stageY);
            }
        });

        topBar = new TopBar(theme, contextMenu);
        topBar.setListener(new TopBar.Listener() {
            @Override
            public void onEditModeChanged(boolean editMode) {
                controller.setEditMode(editMode);
                statusBar.setTransient(editMode
                    ? "Режим редактора: правый клик по аудитории открывает меню"
                    : "Обычный режим");
            }

            @Override
            public void onObjectsPanelToggled(boolean visible) {
                setObjectsPanelVisible(visible);
            }

            @Override
            public void onSchedulePanelToggled(boolean visible) {
                setSchedulePanelVisible(visible);
            }

            @Override
            public void onSaveManifest() {
                saveManifest();
            }

            @Override
            public void onExit() {
                Gdx.app.exit();
            }
        });
        statusBar = new StatusBar(theme);
        // Initialize the mode menu before the first state change.
        topBar.setEditMode(controller.getState().editMode());

        Table root = new Table();
        root.setFillParent(true);
        // The root only hosts the chrome and the floating panels; it must never
        // be hit by stage.hit itself, otherwise every point of the screen would
        // count as "over the UI". childrenOnly keeps the children interactive.
        root.setTouchable(Touchable.childrenOnly);
        stage.addActor(root);

        Table contentRegion = new Table();
        contentRegion.setTouchable(Touchable.childrenOnly);

        objectsPanel = new Table();
        objectsPanel.setBackground(theme.leftPanelBackground());
        objectsPanel.pad(UiTheme.SPACE_M);
        objectsToggle = new PanelToggleButton(theme, false,
            () -> setObjectsPanelVisible(!objectsPanel.isVisible()));
        scheduleToggle = new PanelToggleButton(theme, false,
            () -> setSchedulePanelVisible(!schedulePanel.isVisible()));
        // The collapse toggle lives inside the inspector header, next to the
        // refresh action; collapsing swaps the whole panel for the bare button.
        schedulePanel.setCollapseButton(scheduleToggle);

        searchRow = new Table();
        searchRow.add(searchBar.getActor()).growX().height(PanelToggleButton.HEIGHT);
        searchToggleCell = searchRow.add((Actor) objectsToggle)
            .size(PanelToggleButton.WIDTH, PanelToggleButton.HEIGHT).fill().padLeft(UiTheme.SPACE_S);

        objectsPanel.add(searchRow).growX().padBottom(UiTheme.SPACE_S);
        objectsPanel.row();

        // While a query is typed the results replace the category tree; the
        // Stack keeps the panel layout stable in both modes. The scroll panes
        // react to input only over their rows: the empty area lets clicks and
        // the mouse wheel through to the 3D scene.
        Stack stack = new Stack();
        browserScroll = browserPanel.getActor();
        resultsScroll = new ContentScrollPane(resultsPanel.getActor(), theme.skin());
        resultsScroll.setFadeScrollBars(false);
        stack.add(browserScroll);
        stack.add(resultsScroll);
        resultsScroll.setVisible(false);
        objectsPanel.add(stack).grow();

        objectsCell = contentRegion.add((Actor) objectsPanel).width(PANEL_WIDTH).growY()
            .align(Align.topLeft);
        contentRegion.add().expandX();
        scheduleCell = contentRegion.add((Actor) schedulePanel.getActor()).width(SCHEDULE_WIDTH)
            .growY().align(Align.topRight);
        setSchedulePanelVisible(false);

        root.add(topBar.getActor()).growX();
        root.row();
        root.add(contentRegion).grow();
        root.row();
        root.add(statusBar.getActor()).growX();

        stage.addActor(tooltip.getActor());

        controller.addStateListener(state -> {
            resultsPanel.update(state.searchResults());
            refreshSearchMode();
            topBar.setEditMode(state.editMode());
            statusBar.setMode(state.editMode() ? "Режим: редактор" : "Режим: обычный");
            browserPanel.syncSelection(state.selectedRoomId(),
                state.selectedCategoryId(), state.selectedSubcategoryId());
            if (schedulePanel.isVisible()) {
                schedulePanel.showForRoom(state.selectedRoomId() == null ? null
                    : controller.findSpaceById(state.selectedRoomId()).orElse(null));
            }
        });

        // Escape unfocuses the search field and closes the editor dialog; the
        // camera controller still receives the unconsumed event afterwards and
        // clears the selection.
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    contextMenu.hide();
                    editorDialog.hide();
                    stage.setKeyboardFocus(null);
                }
                return false;
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                // Clicking anywhere but the search field leaves it, so WASD
                // returns to the camera. The event is not consumed: the click
                // keeps working wherever it landed.
                if (stage.getKeyboardFocus() instanceof TextField) {
                    Actor hit = stage.hit(x, y, true);
                    if (!(hit instanceof TextField)) {
                        stage.setKeyboardFocus(null);
                    }
                }
                return contextMenu.touchDown(x, y, button);
            }

        });
    }

    /** Whether the room editor mode is active (right-click menus enabled). */
    public boolean isEditMode() {
        return controller.getState().editMode();
    }

    /**
     * Swaps the objects panel with its collapse toggle. The toggle lives in the
     * search row while the panel is open and takes the hidden panel's spot
     * while collapsed, inset by the same panel padding: it never presses
     * against the window edge and keeps its height in both states.
     */
    private void setObjectsPanelVisible(boolean visible) {
        if (visible) {
            objectsCell.setActor(objectsPanel).width(PANEL_WIDTH).height(0f).pad(0f);
            searchToggleCell.setActor(objectsToggle);
        } else {
            searchToggleCell.clearActor();
            objectsCell.setActor(objectsToggle).width(28f).height(36f).pad(UiTheme.SPACE_M);
        }
        objectsPanel.setVisible(visible);
        objectsPanel.setTouchable(visible ? Touchable.enabled : Touchable.disabled);
        objectsToggle.setPointsRight(!visible);
        topBar.setObjectsVisible(visible);
    }

    /**
     * Swaps the schedule panel with its collapse toggle. The toggle lives in
     * the panel's header while open and takes the hidden panel's spot while
     * collapsed, inset by the same panel padding: same distance from the
     * window edge, same height in both states.
     */
    private void setSchedulePanelVisible(boolean visible) {
        if (visible) {
            scheduleCell.setActor(schedulePanel.getActor()).width(SCHEDULE_WIDTH).height(0f).pad(0f);
            schedulePanel.setCollapseButton(scheduleToggle);
        } else {
            scheduleCell.setActor(scheduleToggle).width(28f).height(36f).pad(UiTheme.SPACE_M);
        }
        schedulePanel.setVisible(visible);
        scheduleToggle.setPointsRight(visible);
        topBar.setScheduleVisible(visible);
        if (visible) {
            schedulePanel.showForRoom(controller.findSpaceById(
                controller.getState().selectedRoomId()).orElse(null));
        }
    }

    /** Opens the room context menu at the given screen position. */
    public void showRoomContextMenu(int screenX, int screenY) {
        String hitbox = controller.hoveredHitbox();
        if (hitbox == null) {
            Gdx.app.log("CM", "no hovered hitbox");
            return;
        }
        Space space = controller.findSpaceByHitbox(hitbox).orElse(null);
        Gdx.app.log("CM", "hitbox=" + hitbox + " known=" + (space != null));
        java.util.List<ContextMenu.Item> items = new java.util.ArrayList<>();
        if (space != null) {
            items.addAll(roomMenuItems(space, hitbox));
        } else {
            items.add(new ContextMenu.Item("Добавить в справочник…",
                () -> showRoomEditor(null, hitbox)));
            items.add(new ContextMenu.Item("Копировать ID хитбокса", () ->
                Gdx.app.getClipboard().setContents(hitbox)));
        }
        contextMenu.show(stage, screenX, screenY, items);
    }

    /** Opens the room context menu for a row of the object hierarchy. */
    private void showRoomContextMenu(Space space, float stageX, float stageY) {
        contextMenu.show(stage, stageX, stageY, roomMenuItems(space, space.modelNodeName()));
    }

    /** The menu items of a registered room: open the editor, copy the room id. */
    private java.util.List<ContextMenu.Item> roomMenuItems(Space space, String hitboxNodeName) {
        return java.util.List.of(
            new ContextMenu.Item("Редактировать…", () -> showRoomEditor(space, hitboxNodeName)),
            new ContextMenu.Item("Копировать ID", () ->
                Gdx.app.getClipboard().setContents(space.id().value())));
    }

    private void showRoomEditor(Space space, String hitboxNodeName) {
        String floorId = floorIdResolver.apply(hitboxNodeName);
        editorDialog.show(topBar.getActor(), space, manifestStore.taxonomy(), hitboxNodeName, floorId);
    }

    private void onRoomSaved(Space space, String floorId, String newSubcategoryName) throws Exception {
        if (newSubcategoryName != null) {
            // A department typed by hand: register it in the manifest taxonomy
            // first, so the saved space references a known subcategory.
            manifestStore.addSubcategory(space.category(), newSubcategoryName);
        }
        manifestStore.upsertSpace(space, floorId);
        String path = manifestStore.save();
        manifestStore.reloadFromRoot();
        controller.replaceCampus(manifestStore.campus());
        if (newSubcategoryName != null) {
            // The tree is built from the taxonomy; the new department must be
            // in it as well, not only in the editor's lists.
            browserPanel.rebuild(manifestStore.taxonomy());
        }
        statusBar.setTransient("Сохранено: " + path);
    }

    private void saveManifest() {
        try {
            String path = manifestStore.save();
            statusBar.setTransient("Сохранено: " + path);
        } catch (Exception e) {
            Gdx.app.error("MapHud", "Failed to save manifest: " + e.getMessage(), e);
            statusBar.setTransient("Ошибка сохранения: " + e.getMessage());
        }
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
        statusBar.act(delta);
        updateHoverInfo();
        // The 3D pass leaves the GL viewport at the work-area rectangle. The
        // stage must draw over the whole window: it does not re-apply its own
        // viewport inside draw(), so restore the full-window rectangle here.
        stage.getViewport().apply();
        stage.draw();
    }

    /**
     * Polls the hovered room once per frame: the floating tooltip next to the
     * cursor is the primary readout, the status bar repeats the same text as a
     * static hint at the bottom of the window.
     */
    private void updateHoverInfo() {
        String hitbox = controller.hoveredHitbox();
        Space space = controller.findSpaceByHitbox(hitbox).orElse(null);
        tooltip.update(hitbox, space);
        String hint = "";
        if (space != null) {
            String description = space.description();
            hint = space.displayName() + (description != null && !description.isBlank()
                ? " — " + description : "");
        }
        if (!hint.equals(lastHint)) {
            lastHint = hint;
            statusBar.setHint(hint);
        }
    }

    /** Height of the menu bar + toolbar band at the top of the window; the 3D
     *  work area starts right under it (read by the game's render loop). */
    public float topChromeHeight() {
        return topBar.getActor().getHeight();
    }

    /** Height of the status bar band at the bottom of the window. */
    public float bottomChromeHeight() {
        return statusBar.getActor().getHeight();
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
