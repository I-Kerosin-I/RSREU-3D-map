package org.rsreu.map.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import org.rsreu.map.controller.MapController;
import org.rsreu.map.domain.campus.CategoryDef;
import org.rsreu.map.domain.campus.CategoryTaxonomy;
import org.rsreu.map.domain.campus.Space;
import org.rsreu.map.domain.campus.SubcategoryDef;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The category tree of the navigation panel: category → subcategory → objects,
 * built from the manifest taxonomy, so new categories appear without a code
 * change. Rows are quiet text lines: categories in bold, rooms in the
 * secondary color; a hovered row lightens, the selected row carries the muted
 * selection wash. Clicking an object selects it on the map (every click
 * re-runs the camera flight, even for the already selected room). Clicking a
 * category or a subcategory highlights all its rooms over the dimmed
 * building, across floors. The visual selection follows the application
 * state, so a selection cleared in the 3D scene (Escape, click on empty
 * space) clears the row highlight too.
 */
public final class ObjectBrowserPanel {

    /** Notified when a room row is right-clicked: (room, stage coordinates). */
    public interface RoomContextMenuHandler {

        void onRoomContextMenu(Space space, float stageX, float stageY);
    }

    private final UiTheme theme;
    private final MapController controller;
    private final ChipTree tree;
    private RoomContextMenuHandler roomContextMenuHandler;

    /** Leaves by room id, for syncing the visual selection with the app state. */
    private final Map<String, BrowserNode> nodesByRoomId = new HashMap<>();
    /** Category / subcategory nodes by {@code category[/subcategory]} key. */
    private final Map<String, BrowserNode> nodesByCategoryKey = new HashMap<>();
    /** The node whose row is lit with the selection color. */
    private BrowserNode styledNode;
    /** The node whose row carries the hover highlight. */
    private BrowserNode hoveredNode;

    public ObjectBrowserPanel(UiTheme theme, MapController controller) {
        this.theme = theme;
        this.controller = controller;

        tree = new ChipTree(theme.skin());
        // Keeps the rows away from the left scrollbar of the pane.
        tree.setPadding(5f, 5f);
        // -1: the listener accepts every mouse button — the left one selects,
        // the right one opens the room context menu (the default constructor
        // silently drops everything but the left click).
        tree.addListener(new ClickListener(-1) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (event.getButton() == Input.Buttons.RIGHT) {
                    notifyRoomContextMenu(event, y);
                    return;
                }
                onNodeClicked(x, y);
            }
        });
    }

    /** The browser wrapped in a vertically scrolling pane. */
    public ScrollPane getActor() {
        ScrollPane pane = new ContentScrollPane(tree, theme.skin(), false);
        pane.setScrollingDisabled(true, false);
        pane.setFadeScrollBars(false);
        return pane;
    }

    /** Wires the receiver of a right-click on a room row of the hierarchy. */
    public void setRoomContextMenuHandler(RoomContextMenuHandler handler) {
        roomContextMenuHandler = handler;
    }

    /**
     * Rebuilds the tree from the manifest taxonomy and the room data. Only
     * editor-registered rooms are listed: rooms that sit in the manifest
     * without ever being saved through the room editor stay out of the tree
     * (search and tooltips still know them).
     */
    public void rebuild(CategoryTaxonomy taxonomy) {
        tree.clearChildren();
        nodesByRoomId.clear();
        nodesByCategoryKey.clear();
        styledNode = null;
        hoveredNode = null;

        for (CategoryDef category : taxonomy.categories()) {
            List<Space> spaces = controller.findByCategory(category.id(), null).stream()
                .filter(Space::edited)
                .toList();
            if (spaces.isEmpty()) {
                continue;
            }

            BrowserNode categoryNode = new BrowserNode(rowLabel(category.name(), "heading"),
                "heading", category.id(), null, null);
            nodesByCategoryKey.put(category.id(), categoryNode);
            trackHover(categoryNode);
            Set<String> placed = new HashSet<>();

            for (SubcategoryDef subcategory : category.subcategories()) {
                List<Space> subSpaces = spaces.stream()
                    .filter(space -> subcategory.id().equals(space.subcategory()))
                    .toList();
                if (subSpaces.isEmpty()) {
                    continue;
                }
                BrowserNode subNode = new BrowserNode(rowLabel(subcategory.name(), "tree-sub"),
                    "tree-sub", category.id(), subcategory.id(), null);
                nodesByCategoryKey.put(categoryKey(category.id(), subcategory.id()), subNode);
                trackHover(subNode);
                for (Space space : subSpaces) {
                    subNode.add(leaf(space));
                    placed.add(space.id().value());
                }
                categoryNode.add(subNode);
            }

            // Rooms whose subcategory is not declared in the taxonomy.
            for (Space space : spaces) {
                if (!placed.contains(space.id().value())) {
                    categoryNode.add(leaf(space));
                }
            }

            tree.add(categoryNode);
            categoryNode.setExpanded(true);
        }
    }

    /**
     * Aligns the visual selection with the application state: the selected room,
     * the selected category, or nothing. The selected row carries the selection
     * wash and a brighter text.
     */
    public void syncSelection(String selectedRoomId, String selectedCategoryId,
                              String selectedSubcategoryId) {
        BrowserNode node = null;
        if (selectedRoomId != null) {
            node = nodesByRoomId.get(selectedRoomId);
        } else if (selectedCategoryId != null) {
            node = nodesByCategoryKey.get(categoryKey(selectedCategoryId, selectedSubcategoryId));
        }
        if (node != styledNode) {
            BrowserNode previous = styledNode;
            styledNode = node;
            if (previous != null) {
                applyRowStyle(previous);
            }
            if (node != null) {
                applyRowStyle(node);
            }
        }
        if (node != null) {
            tree.getSelection().set(node);
        } else {
            tree.getSelection().clear();
        }
    }

    private void onNodeClicked(float x, float y) {
        BrowserNode node = tree.getNodeAt(y);
        if (node == null) {
            return;
        }
        if (node.hasChildren() && x < node.getActor().getX()) {
            // Click on the triangular expansion arrow: the tree toggles it, no
            // selection.
            return;
        }
        if (node.space != null) {
            controller.selectObject(node.space);
        } else if (node.categoryId != null) {
            controller.selectCategory(node.categoryId, node.subcategoryId);
        }
    }

    /** Right-click on a room row: the game layer opens the room context menu. */
    private void notifyRoomContextMenu(InputEvent event, float y) {
        if (roomContextMenuHandler == null) {
            return;
        }
        BrowserNode node = tree.getNodeAt(y);
        if (node == null || node.space == null) {
            return;
        }
        roomContextMenuHandler.onRoomContextMenu(node.space, event.getStageX(), event.getStageY());
    }

    private BrowserNode leaf(Space space) {
        BrowserNode node = new BrowserNode(rowLabel(space.displayName(), "secondary"),
            "secondary", null, null, space);
        trackHover(node);
        nodesByRoomId.put(space.id().value(), node);
        return node;
    }

    /** A text label on its own row container: the hover / selection carrier. */
    private Container<Label> rowLabel(String text, String style) {
        Label label = new Label(text, theme.skin(), style);
        Container<Label> container = new Container<>(label);
        // Containers are Touchable.childrenOnly by default, which would make
        // everything but the text itself dead to clicks and hover.
        container.setTouchable(Touchable.enabled);
        container.setBackground(theme.rowIdle());
        return container;
    }

    /**
     * Adds the hover highlight of a row: lightens the row under the cursor,
     * leaving the selected row to the selection wash.
     */
    private void trackHover(BrowserNode node) {
        node.chip.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                hoveredNode = node;
                applyRowBackground(node);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (hoveredNode == node) {
                    hoveredNode = null;
                }
                applyRowBackground(node);
            }
        });
    }

    /** Hover + selection background of one row. */
    private void applyRowBackground(BrowserNode node) {
        node.chip.setBackground(node == styledNode ? theme.rowSelected()
            : node == hoveredNode ? theme.rowHover() : theme.rowIdle());
    }

    /**
     * Restyles one row for its current selection state: the selected room
     * brightens from the secondary color to the full text color.
     */
    private void applyRowStyle(BrowserNode node) {
        node.label.setStyle(node == styledNode ? node.selectedStyle
            : theme.skin().get(node.style, Label.LabelStyle.class));
        applyRowBackground(node);
    }

    private static String categoryKey(String categoryId, String subcategoryId) {
        return subcategoryId == null ? categoryId : categoryId + "/" + subcategoryId;
    }

    /**
     * Tree node type used for every row: category and subcategory rows carry
     * their taxonomy ids, leaves carry the room metadata.
     */
    private static final class BrowserNode extends Tree.Node<BrowserNode, String, Container<Label>> {

        final Container<Label> chip;
        final Label label;
        final String style;
        final Label.LabelStyle selectedStyle;
        final String categoryId;
        final String subcategoryId;
        final Space space;

        BrowserNode(Container<Label> chip, String style, String categoryId, String subcategoryId, Space space) {
            super(chip);
            this.chip = chip;
            this.label = chip.getActor();
            this.style = style;
            this.selectedStyle = new Label.LabelStyle(label.getStyle());
            this.selectedStyle.fontColor = UiTheme.ACCENT;
            this.categoryId = categoryId;
            this.subcategoryId = subcategoryId;
            this.space = space;
        }
    }

    /**
     * Tree whose rows are hittable only where they are drawn: the row labels
     * and the expansion-arrow strips. The plain Tree claims its whole bounds,
     * so clicks and the wheel in the empty space right of the rows would never
     * reach the 3D scene.
     */
    private static final class ChipTree extends Tree<BrowserNode, String> {

        ChipTree(Skin skin) {
            super(skin, "tree");
        }

        @Override
        public Actor hit(float x, float y, boolean touchable) {
            Actor hit = super.hit(x, y, touchable);
            if (hit != this) {
                return hit; // a row label (or its container) was hit
            }
            // The tree itself was hit: only the expansion-arrow strip of a node
            // with children counts, the rest of the row passes through.
            BrowserNode node = getNodeAt(y);
            if (node != null && node.hasChildren() && x < node.getActor().getX()) {
                return this;
            }
            return null;
        }
    }
}
