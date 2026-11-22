package org.rsreu.map.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import java.util.List;

/**
 * The classic top chrome of the window: a menu bar (Файл / Режим / Вид) with
 * the application caption on the right. The manifest is saved through
 * Файл → «Сохранить манифест», the editor mode is switched only through the
 * Режим menu. The dropdowns are the shared {@link ContextMenu} popup.
 */
public final class TopBar {

    /** Commands the top bar issues; implemented by the HUD/game wiring. */
    public interface Listener {
        void onEditModeChanged(boolean editMode);

        void onObjectsPanelToggled(boolean visible);

        void onSchedulePanelToggled(boolean visible);

        void onSaveManifest();

        void onExit();
    }

    private final Table table;
    private final ContextMenu menu;
    private final UiTheme theme;
    private Listener listener;

    private TextButton fileButton;
    private TextButton modeButton;
    private TextButton viewButton;

    private boolean editMode;
    private boolean objectsVisible = true;
    private boolean scheduleVisible;

    public TopBar(UiTheme theme, ContextMenu menu) {
        this.theme = theme;
        this.menu = menu;

        table = new Table();
        table.setBackground(theme.menuBarBackground());
        table.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        // Swallow presses on the strips: the classic chrome must not drag the
        // camera behind it.
        table.addListener(new ClickListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                     float x, float y, int pointer, int button) {
                return true;
            }
        });

        buildMenuRow();
    }

    private void buildMenuRow() {
        fileButton = menuButton("Файл", this::showFileMenu);
        modeButton = menuButton("Режим", this::showModeMenu);
        viewButton = menuButton("Вид", this::showViewMenu);

        Table menuRow = new Table();
        menuRow.add(fileButton).left().height(28f);
        menuRow.add(modeButton).left().height(28f);
        menuRow.add(viewButton).left().height(28f);
        menuRow.add().growX();
        menuRow.add(new com.badlogic.gdx.scenes.scene2d.ui.Label("RSREU 3D Map", theme.skin(), "caption"))
            .right().padRight(UiTheme.SPACE_L);
        menuRow.left();

        table.add(menuRow).growX().pad(4, 4, 4, 4);
        table.row();
    }

    private TextButton menuButton(String text, Runnable onClick) {
        TextButton button = new TextButton(text, theme.skin(), "menu-item");
        button.setRound(false);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                onClick.run();
            }
        });
        return button;
    }

    private void showFileMenu() {
        showUnder(fileButton, List.of(
            new ContextMenu.Item("Сохранить манифест", () -> {
                if (listener != null) {
                    listener.onSaveManifest();
                }
            }),
            new ContextMenu.Item("Выход", () -> {
                if (listener != null) {
                    listener.onExit();
                }
            })
        ));
    }

    private void showModeMenu() {
        showUnder(modeButton, List.of(
            new ContextMenu.Item(editMode ? "  Обычный режим" : "✓ Обычный режим",
                () -> notifyEditMode(false)),
            new ContextMenu.Item(editMode ? "✓ Режим редактора" : "  Режим редактора",
                () -> notifyEditMode(true))
        ));
    }

    private void showViewMenu() {
        showUnder(viewButton, List.of(
            new ContextMenu.Item((objectsVisible ? "✓ " : "  ") + "Панель объектов",
                this::notifyObjectsToggled),
            new ContextMenu.Item((scheduleVisible ? "✓ " : "  ") + "Панель расписания",
                this::notifyScheduleToggled)
        ));
    }

    /** Shows a dropdown with its top edge just under the given button. */
    private void showUnder(Actor button, List<ContextMenu.Item> items) {
        com.badlogic.gdx.scenes.scene2d.Stage stage = button.getStage();
        if (stage == null) {
            return;
        }
        com.badlogic.gdx.math.Vector2 top = button.localToStageCoordinates(
            new com.badlogic.gdx.math.Vector2(0, button.getHeight()));
        menu.show(stage, top.x, top.y, items);
    }

    private void notifyEditMode(boolean value) {
        if (listener != null) {
            listener.onEditModeChanged(value);
        }
    }

    private void notifyObjectsToggled() {
        if (listener != null) {
            listener.onObjectsPanelToggled(!objectsVisible);
        }
    }

    private void notifyScheduleToggled() {
        if (listener != null) {
            listener.onSchedulePanelToggled(!scheduleVisible);
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /** Syncs the Режим menu check marks with the application state. */
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public void setObjectsVisible(boolean visible) {
        objectsVisible = visible;
    }

    public void setScheduleVisible(boolean visible) {
        scheduleVisible = visible;
    }

    public Actor getActor() {
        return table;
    }
}
