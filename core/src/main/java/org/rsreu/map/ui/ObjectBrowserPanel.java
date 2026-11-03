package org.rsreu.map.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
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
 * The category tree of the side panel: category → subcategory → objects. The
 * tree is built from the manifest taxonomy, so new categories appear without a
 * code change.
 * <p>
 * Clicking an object selects it on the map (every click re-runs the camera
 * flight, even for the already selected room). Clicking a category or a
 * subcategory highlights all its rooms over the dimmed building, across floors.
 * The visual selection follows the application state, so a selection cleared in
 * the 3D scene (Escape, click on empty space) clears the tree highlight too.
 */
public final class ObjectBrowserPanel {

    private final UiTheme theme;
    private final MapController controller;
    private final Tree<BrowserNode, String> tree;

    /** Leaves by room id, for syncing the visual selection with the app state. */
    private final Map<String, BrowserNode> nodesByRoomId = new HashMap<>();
    /** Category / subcategory nodes by {@code category[/subcategory]} key. */
    private final Map<String, BrowserNode> nodesByCategoryKey = new HashMap<>();

    public ObjectBrowserPanel(UiTheme theme, MapController controller) {
        this.theme = theme;
        this.controller = controller;

        tree = new Tree<>(theme.skin(), "tree");
        tree.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onNodeClicked(x, y);
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
        nodesByRoomId.clear();
        nodesByCategoryKey.clear();

        for (CategoryDef category : taxonomy.categories()) {
            List<Space> spaces = controller.findByCategory(category.id(), null);
            if (spaces.isEmpty()) {
                continue;
            }

            BrowserNode categoryNode = new BrowserNode(nodeLabel(category.name(), true),
                category.id(), null, null);
            nodesByCategoryKey.put(category.id(), categoryNode);
            Set<String> placed = new HashSet<>();

            for (SubcategoryDef subcategory : category.subcategories()) {
                List<Space> subSpaces = spaces.stream()
                    .filter(space -> subcategory.id().equals(space.subcategory()))
                    .toList();
                if (subSpaces.isEmpty()) {
                    continue;
                }
                BrowserNode subNode = new BrowserNode(nodeLabel(subcategory.name(), false),
                    category.id(), subcategory.id(), null);
                nodesByCategoryKey.put(categoryKey(category.id(), subcategory.id()), subNode);
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
     * the selected category, or nothing.
     */
    public void syncSelection(String selectedRoomId, String selectedCategoryId,
                              String selectedSubcategoryId) {
        BrowserNode node = null;
        if (selectedRoomId != null) {
            node = nodesByRoomId.get(selectedRoomId);
        } else if (selectedCategoryId != null) {
            node = nodesByCategoryKey.get(categoryKey(selectedCategoryId, selectedSubcategoryId));
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
            // Click on the +/- expansion icon: the tree toggles it, no selection.
            return;
        }
        if (node.space != null) {
            controller.selectObject(node.space);
        } else if (node.categoryId != null) {
            controller.selectCategory(node.categoryId, node.subcategoryId);
        }
    }

    private BrowserNode leaf(Space space) {
        BrowserNode node = new BrowserNode(nodeLabel(space.displayName(), false), null, null, space);
        nodesByRoomId.put(space.id().value(), node);
        return node;
    }

    private Label nodeLabel(String text, boolean category) {
        return new Label(text, theme.skin(), category ? "accent" : "default");
    }

    private static String categoryKey(String categoryId, String subcategoryId) {
        return subcategoryId == null ? categoryId : categoryId + "/" + subcategoryId;
    }

    /**
     * Tree node type used for every row: category and subcategory rows carry
     * their taxonomy ids, leaves carry the room metadata.
     */
    private static final class BrowserNode extends Tree.Node<BrowserNode, String, Label> {

        final String categoryId;
        final String subcategoryId;
        final Space space;

        BrowserNode(Label label, String categoryId, String subcategoryId, Space space) {
            super(label);
            this.categoryId = categoryId;
            this.subcategoryId = subcategoryId;
            this.space = space;
        }
    }
}
