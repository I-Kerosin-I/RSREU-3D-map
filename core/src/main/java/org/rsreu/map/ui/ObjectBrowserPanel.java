package org.rsreu.map.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import org.rsreu.map.controller.MapController;
import org.rsreu.map.domain.campus.CategoryDef;
import org.rsreu.map.domain.campus.CategoryTaxonomy;
import org.rsreu.map.domain.campus.Space;
import org.rsreu.map.domain.campus.SubcategoryDef;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The category tree of the side panel: category → subcategory → objects. The
 * tree is built from the manifest taxonomy, so new categories appear without a
 * code change. Clicking an object selects it on the map.
 */
public final class ObjectBrowserPanel {

    private final UiTheme theme;
    private final MapController controller;
    private final Tree<SpaceNode, String> tree;

    /** The room selected by the previous change event; expansion events re-fire it. */
    private String lastSelectedRoomId;

    public ObjectBrowserPanel(UiTheme theme, MapController controller) {
        this.theme = theme;
        this.controller = controller;

        tree = new Tree<>(theme.skin(), "tree");
        tree.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                onSelectionChanged();
            }
        });
    }

    /** The browser wrapped in a vertically scrolling pane. */
    public ScrollPane getActor() {
        ScrollPane pane = new ScrollPane(tree, theme.skin());
        pane.setScrollingDisabled(true, false);
        pane.setFadeScrollBars(false);
        return pane;
    }

    /** Rebuilds the tree from the manifest taxonomy and the room data. */
    public void rebuild(CategoryTaxonomy taxonomy) {
        tree.clearChildren();
        lastSelectedRoomId = null;

        for (CategoryDef category : taxonomy.categories()) {
            List<Space> spaces = controller.findByCategory(category.id(), null);
            if (spaces.isEmpty()) {
                continue;
            }

            SpaceNode categoryNode = new SpaceNode(nodeLabel(category.name(), true), null);
            Set<String> placed = new HashSet<>();

            for (SubcategoryDef subcategory : category.subcategories()) {
                List<Space> subSpaces = spaces.stream()
                    .filter(space -> subcategory.id().equals(space.subcategory()))
                    .toList();
                if (subSpaces.isEmpty()) {
                    continue;
                }
                SpaceNode subNode = new SpaceNode(nodeLabel(subcategory.name(), false), null);
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

    private SpaceNode leaf(Space space) {
        return new SpaceNode(nodeLabel(space.displayName(), false), space);
    }

    private Label nodeLabel(String text, boolean category) {
        return new Label(text, theme.skin(), category ? "accent" : "default");
    }

    private void onSelectionChanged() {
        SpaceNode selected = tree.getSelectedNode();
        if (selected == null || selected.space == null) {
            return;
        }
        String roomId = selected.space.id().value();
        if (roomId.equals(lastSelectedRoomId)) {
            return;
        }
        lastSelectedRoomId = roomId;
        controller.selectObject(selected.space);
    }

    /**
     * Tree node type used for every row: category and subcategory rows carry a
     * {@code null} space, leaves carry the room metadata.
     */
    private static final class SpaceNode extends Tree.Node<SpaceNode, String, Label> {

        final Space space;

        SpaceNode(Label label, Space space) {
            super(label);
            this.space = space;
        }
    }
}
