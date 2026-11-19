package org.rsreu.map.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;

/**
 * Centralized UI theme: Cyrillic-capable fonts generated at startup from the
 * bundled PT Sans files, the color palette, flat / rounded drawables and the
 * widget styles. All widgets take their styles from here, so the visual design
 * can change without touching UI logic.
 * <p>
 * The controls float over the 3D scene in the manner of the Fusion 360 browser:
 * there is no opaque panel behind them, every text row carries its own small
 * semi-transparent rounded "chip" instead.
 */
public final class UiTheme implements Disposable {

    // Centralized palette; the accent matches the edge wireframe color.
    public static final Color TEXT = new Color(0xE6EBF4FF);
    public static final Color TEXT_DIM = new Color(0x94A0B8FF);
    public static final Color ACCENT = new Color(0x2F6DB2FF);
    /** Floating chip under a single text row. */
    public static final Color CHIP_BG = new Color(0x10151F82);
    public static final Color CHIP_HOVER = new Color(0x2F6DB280);
    public static final Color CHIP_SELECTED = new Color(0x2F6DB2B4);
    public static final Color TOOLTIP_BG = new Color(0x0C111AE6);
    /** Opaque chrome of the classic layout: menu bar, toolbar, status bar,
     *  side panels, popups. They are separate window areas, not overlays. */
    public static final Color PANEL_BG = new Color(0x10151FFF);
    public static final Color FIELD_BG = new Color(0x1A223099);
    public static final Color FIELD_BG_FOCUSED = new Color(0x212B3DB4);
    public static final Color SCROLL_TRACK = new Color(0xFFFFFF28);
    public static final Color SCROLL_KNOB = new Color(0xFFFFFF66);
    public static final Color TREE_SELECTION = new Color(0x2F6DB260);
    public static final Color ROW_HOVER = new Color(0x2F6DB250);

    /** Preset equipment entries offered by the room editor as checkboxes. */
    public static final java.util.List<String> EQUIPMENT_PRESETS =
        java.util.List.of("Компьютеры", "Проектор", "Интерактивная доска", "Лабораторные стенды");

    /** Default ASCII charset of FreeTypeFontGenerator plus Russian glyphs and marks. */
    private static final String CHARACTERS = FreeTypeFontGenerator.DEFAULT_CHARS
        + "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
        + "абвгдеёжзийклмнопрстуфхцчшщъыьэюя«»—–…№✓‹›";

    private final Skin skin;
    private final BitmapFont regularFont;
    private final BitmapFont boldFont;
    private final BitmapFont smallFont;
    private final Drawable tooltipBackground;
    private final Drawable chipBackground;
    private final Drawable chipHover;
    private final Drawable chipSelected;
    private final Drawable panelBackground;

    public UiTheme() {
        regularFont = createFont("assets/fonts/PTSans-Regular.ttf", 15);
        boldFont = createFont("assets/fonts/PTSans-Bold.ttf", 16);
        smallFont = createFont("assets/fonts/PTSans-Regular.ttf", 12);

        skin = new Skin();
        skin.add("regular", regularFont);
        skin.add("bold", boldFont);
        skin.add("small", smallFont);

        skin.add("white", new Texture(makeFlatPixmap(Color.WHITE)));

        tooltipBackground = rounded(TOOLTIP_BG, 8);
        skin.add("tooltip_bg", tooltipBackground);
        chipBackground = roundedPadded(CHIP_BG, 9, 8, 3);
        chipHover = roundedPadded(CHIP_HOVER, 9, 8, 3);
        chipSelected = roundedPadded(CHIP_SELECTED, 9, 8, 3);
        // The chrome strips: square, opaque dark, full-bleed.
        panelBackground = roundedPadded(PANEL_BG, 0, 0, 0);

        addStyles();
    }

    public Skin skin() {
        return skin;
    }

    /** Dark translucent strip for the menu bar, toolbar and status bar. */
    public Drawable panelBackground() {
        return panelBackground;
    }

    /** Rounded background used by the hover tooltip. */    public Drawable tooltipBackground() {
        return tooltipBackground;
    }


    /** Floating chip under a regular text row. */
    public Drawable chipBackground() {
        return chipBackground;
    }

    /** Floating chip under a hovered row (search results). */
    public Drawable chipHover() {
        return chipHover;
    }

    /** Floating chip under the selected row. */
    public Drawable chipSelected() {
        return chipSelected;
    }

    /** A plain 1x1 drawable tinted with the given color. */
    public Drawable flat(Color color) {
        Texture white = skin.get("white", Texture.class);
        return new TextureRegionDrawable(new TextureRegion(white)).tint(color);
    }

    /** A flat square tint whose padding inflates the highlight box past the
     *  text: the classic look of a menu bar item. The padding lives inside the
     *  drawable, so adjacent items' highlights sit flush against each other.
     *  The patch must carry the padding before the drawable copies it: the
     *  drawable's own padding fields are never re-read from the patch. */
    private Drawable flatPadded(Color color, int padX, int padY) {
        Pixmap pixmap = makeFlatPixmap(color);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        skin.add("flat_" + Integer.toHexString(color.toIntBits()) + "_" + padX + "_" + padY, texture);
        NinePatch patch = new NinePatch(texture, 0, 0, 0, 0);
        patch.setPadding(padX, padX, padY, padY);
        return new NinePatchDrawable(patch);
    }

    private void addStyles() {
        Label.LabelStyle labelDefault = new Label.LabelStyle(regularFont, TEXT);
        skin.add("default", labelDefault);
        skin.add("title", new Label.LabelStyle(boldFont, TEXT));
        skin.add("hint", new Label.LabelStyle(smallFont, TEXT_DIM));
        skin.add("accent", new Label.LabelStyle(regularFont, ACCENT));
        skin.add("error", new Label.LabelStyle(smallFont, new Color(0xE07A7AFF)));

        TextField.TextFieldStyle field = new TextField.TextFieldStyle();
        field.font = regularFont;
        field.fontColor = TEXT;
        field.messageFont = smallFont;
        field.messageFontColor = TEXT_DIM;
        // The rounded background carries the horizontal padding, so the typed
        // text and the placeholder sit away from the field's left edge.
        field.background = roundedPadded(FIELD_BG, 8, 10, 8);
        field.focusedBackground = roundedPadded(FIELD_BG_FOCUSED, 8, 10, 8);
        field.cursor = flat(TEXT);
        field.selection = flat(new Color(ACCENT.r, ACCENT.g, ACCENT.b, 0.35f));
        skin.add("default", field);
        skin.add("field", field);

        // Buttons: rounded field background, accent when pressed/checked.
        TextButton.TextButtonStyle button = new TextButton.TextButtonStyle();
        button.font = regularFont;
        button.fontColor = TEXT;
        button.up = roundedPadded(FIELD_BG, 6, 10, 6);
        button.over = roundedPadded(FIELD_BG_FOCUSED, 6, 10, 6);
        button.down = roundedPadded(CHIP_SELECTED, 6, 10, 6);
        button.checked = roundedPadded(CHIP_SELECTED, 6, 10, 6);
        skin.add("default", button);

        // Top-bar controls and popup rows: plain text with a flat highlight
        // that extends past the text by a fixed padding. Every state carries
        // the same padding, so the metrics never change on hover, and adjacent
        // highlights sit flush against each other — the classic menu bar look.
        // checked stays null: Button toggles its checked flag on every click,
        // and a checked drawable would make command buttons stick in the
        // highlight until clicked twice.
        int menuPadX = 10;
        int menuPadY = 4;
        Drawable menuUp = flatPadded(new Color(0, 0, 0, 0), menuPadX, menuPadY);
        Drawable menuHover = flatPadded(ROW_HOVER, menuPadX, menuPadY);

        TextButton.TextButtonStyle menuItem = new TextButton.TextButtonStyle();
        menuItem.font = regularFont;
        menuItem.fontColor = TEXT;
        menuItem.up = menuUp;
        menuItem.over = menuHover;
        menuItem.down = menuHover;
        skin.add("menu-item", menuItem);

        TextButton.TextButtonStyle topBar = new TextButton.TextButtonStyle();
        topBar.font = regularFont;
        topBar.fontColor = TEXT;
        topBar.up = menuUp;
        topBar.over = menuHover;
        topBar.down = menuHover;
        skin.add("top-bar", topBar);

        Button.ButtonStyle panelToggle = new Button.ButtonStyle();
        panelToggle.up = rounded(FIELD_BG, 8);
        panelToggle.over = rounded(FIELD_BG_FOCUSED, 8);
        panelToggle.down = rounded(CHIP_SELECTED, 8);
        skin.add("panel-toggle", panelToggle);
        skin.add("panel-chevron-left", panelChevron(false), Drawable.class);
        skin.add("panel-chevron-right", panelChevron(true), Drawable.class);

        CheckBox.CheckBoxStyle checkBox = new CheckBox.CheckBoxStyle();
        checkBox.font = regularFont;
        checkBox.fontColor = TEXT;
        checkBox.checkboxOff = checkBoxPixmap(false);
        checkBox.checkboxOn = checkBoxPixmap(true);
        skin.add("default", checkBox);

        // The scroll style is shared: panes and the select box dropdown use it.
        ScrollPane.ScrollPaneStyle scroll = new ScrollPane.ScrollPaneStyle();
        scroll.vScroll = flat(SCROLL_TRACK);
        scroll.vScrollKnob = flat(SCROLL_KNOB);
        scroll.hScroll = flat(SCROLL_TRACK);
        scroll.hScrollKnob = flat(SCROLL_KNOB);
        skin.add("default", scroll);

        List.ListStyle list = new List.ListStyle(regularFont, TEXT, TEXT, null);
        list.selection = roundedPadded(CHIP_SELECTED, 4, 6, 2);
        list.background = rounded(FIELD_BG, 8);
        skin.add("default", list);

        SelectBox.SelectBoxStyle selectBox = new SelectBox.SelectBoxStyle();
        selectBox.font = regularFont;
        selectBox.fontColor = TEXT;
        selectBox.background = roundedPadded(FIELD_BG, 8, 10, 8);
        selectBox.scrollStyle = scroll;
        selectBox.listStyle = list;
        skin.add("default", selectBox);

        Window.WindowStyle window = new Window.WindowStyle();
        window.titleFont = boldFont;
        window.titleFontColor = TEXT;
        NinePatchDrawable windowBackground = rounded(PANEL_BG, 10);
        windowBackground.getPatch().setPadding(6, 26, 6, 6);
        window.background = windowBackground;
        skin.add("default", window);

        Tree.TreeStyle tree = new Tree.TreeStyle();
        // Collapsed / expanded subtrees show a small triangular arrow instead of
        // the plus / minus squares.
        tree.plus = arrowIcon(false);
        tree.minus = arrowIcon(true);
        // Row highlighting is done by the per-row chips; the full-width
        // selection bar stays off.
        tree.selection = null;
        skin.add("tree", tree);
    }

    /**
     * Generates a bitmap font from one of the bundled TTF files. The character
     * set must be given explicitly: FreeTypeFontGenerator rasterizes ASCII only
     * by default, which would silently drop every Cyrillic letter.
     */
    private static BitmapFont createFont(String path, int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(path));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter =
            new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = size;
        parameter.characters = CHARACTERS;
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        BitmapFont font = generator.generateFont(parameter);
        generator.dispose();
        return font;
    }

    private static Pixmap makeFlatPixmap(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        return pixmap;
    }

    /**
     * Rounded square texture stretched as a nine patch, so the corners keep
     * their radius at any widget size. Linear filtering keeps the rounded
     * corners smooth when the patch is stretched or minified. Blending is off
     * while drawing: the overlapping primitives must write the color and alpha
     * as given, not blend over each other and accumulate the opacity.
     */
    private NinePatchDrawable rounded(Color color, int radius) {
        int size = radius * 2 + 2;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(color);
        pixmap.fillRectangle(radius, 0, size - 2 * radius, size);
        pixmap.fillRectangle(0, radius, size, size - 2 * radius);
        pixmap.fillCircle(radius, radius, radius);
        pixmap.fillCircle(size - radius - 1, radius, radius);
        pixmap.fillCircle(radius, size - radius - 1, radius);
        pixmap.fillCircle(size - radius - 1, size - radius - 1, radius);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        skin.add("rounded_" + Integer.toHexString(color.toIntBits()) + "_" + radius, texture);
        return new NinePatchDrawable(new NinePatch(texture, radius, radius, radius, radius));
    }

    /** A rounded background whose nine patch padding insets the content. */
    private NinePatchDrawable roundedPadded(Color color, int radius, int padX, int padY) {
        NinePatchDrawable drawable = rounded(color, radius);
        drawable.getPatch().setPadding(padX, padY, padY, padX);
        return drawable;
    }

    /**
     * Small triangular expansion marker of the category tree: pointing right
     * while the subtree is collapsed and down once it is expanded.
     */
    private Drawable arrowIcon(boolean expanded) {
        Pixmap pixmap = new Pixmap(10, 10, Pixmap.Format.RGBA8888);
        pixmap.setColor(TEXT_DIM);
        if (expanded) {
            pixmap.fillTriangle(2, 3, 8, 3, 5, 8); // down
        } else {
            pixmap.fillTriangle(3, 2, 3, 8, 8, 5); // right
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        skin.add("tree_arrow_" + (expanded ? "down" : "right"), texture);
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    /** Antialiased chevron drawn independently of the UI font. */
    private Drawable panelChevron(boolean pointsRight) {
        int scale = 4;
        Pixmap pixmap = new Pixmap(12 * scale, 12 * scale, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(TEXT);
        for (int y = 0; y < 12 * scale; y++) {
            for (int x = 0; x < 12 * scale; x++) {
                float px = (x + 0.5f) / scale;
                float py = (y + 0.5f) / scale;
                if (!pointsRight) px = 12f - px;
                float edge = 8f - Math.abs(py - 6f);
                if (py >= 2f && py <= 10f && Math.abs(px - edge) <= 0.8f) {
                    pixmap.drawPixel(x, y);
                }
            }
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        skin.add("panel_chevron_" + pointsRight, texture);
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
        drawable.setMinWidth(12f);
        drawable.setMinHeight(12f);
        return drawable;
    }

    /** Square checkbox drawable; the checked variant carries a white tick. */
    private Drawable checkBoxPixmap(boolean checked) {
        Pixmap pixmap = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(checked ? ACCENT : FIELD_BG);
        pixmap.fillRectangle(2, 2, 12, 12);
        pixmap.setColor(TEXT_DIM);
        if (!checked) {
            // A thin border around the empty box.
            pixmap.fillRectangle(2, 2, 12, 1);
            pixmap.fillRectangle(2, 13, 12, 1);
            pixmap.fillRectangle(2, 2, 1, 12);
            pixmap.fillRectangle(13, 2, 1, 12);
        } else {
            pixmap.setColor(TEXT);
            pixmap.fillRectangle(4, 7, 2, 3);
            pixmap.fillRectangle(6, 8, 2, 3);
            pixmap.fillRectangle(8, 6, 2, 2);
            pixmap.fillRectangle(10, 4, 2, 3);
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        skin.add("checkbox_" + (checked ? "on" : "off"), texture);
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    @Override
    public void dispose() {
        // Disposes the skin and every font / texture registered in it.
        skin.dispose();
    }
}
