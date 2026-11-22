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
 * Centralized UI design system: the palette, the typography scale, the spacing
 * and radius tokens, the generated Cyrillic-capable fonts (bundled PT Sans),
 * the shared drawables and the widget styles. Every widget takes its look from
 * here, so the visual design can change without touching UI logic.
 * <p>
 * The look follows a "technical minimalism" direction: a calm dark neutral
 * base, one cold blue accent reserved for interactive and selected states,
 * flat surfaces separated by hairline borders instead of shadows.
 */
public final class UiTheme implements Disposable {

    // === Palette. ===
    /** The 3D viewport backdrop; also the base of tooltips. */
    public static final Color APP_BG = new Color(0x171B24FF);
    /** Chrome strips, docked panels and popups. */
    public static final Color PANEL_BG = new Color(0x202633FF);
    /** Nested surfaces: fields, buttons, cards. */
    public static final Color SURFACE = new Color(0x282F3DFF);
    /** Hover highlight of interactive elements. */
    public static final Color SURFACE_HOVER = new Color(0x303949FF);
    /** Hairline borders and separators. */
    public static final Color BORDER = new Color(0x343D4DFF);
    public static final Color TEXT = new Color(0xE2E8F0FF);
    public static final Color TEXT_SECONDARY = new Color(0x929DB0FF);
    public static final Color TEXT_DISABLED = new Color(0x667286FF);
    /** The single accent: interactivity, selection, focus. Not decoration. */
    public static final Color ACCENT = new Color(0x4EA1FFFF);
    public static final Color ACCENT_HOVER = new Color(0x69B0FFFF);
    /** Background of selected rows: a muted accent wash, not a bright fill. */
    public static final Color SELECTED_BG = new Color(0x2D435EFF);
    public static final Color SUCCESS = new Color(0x65C59BFF);
    public static final Color WARNING = new Color(0xE5B567FF);
    public static final Color ERROR = new Color(0xE57878FF);
    private static final Color SCROLL_KNOB = new Color(0x46536BFF);
    private static final Color TOOLTIP_BG = new Color(0x1A2029F2);

    // === Spacing tokens (px): 4 minimal, 8 tightly related, 12 compact inner,
    // 16 main inner, 24 between sections. ===
    public static final float SPACE_XS = 4f;
    public static final float SPACE_S = 8f;
    public static final float SPACE_M = 12f;
    public static final float SPACE_L = 16f;
    public static final float SPACE_XL = 24f;

    // === Corner radius tokens: 4 small controls, 6 buttons and fields,
    // 8 cards and panels. ===
    public static final int RADIUS_S = 4;
    public static final int RADIUS_M = 6;
    public static final int RADIUS_L = 8;

    /** Common height of single-row controls: fields and buttons. */
    public static final float CONTROL_H = 32f;

    /** Preset equipment entries offered by the room editor as checkboxes. */
    public static final java.util.List<String> EQUIPMENT_PRESETS =
        java.util.List.of("Компьютеры", "Проектор", "Интерактивная доска", "Лабораторные стенды");

    /** Default ASCII charset of FreeTypeFontGenerator plus Russian glyphs and marks. */
    private static final String CHARACTERS = FreeTypeFontGenerator.DEFAULT_CHARS
        + "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
        + "абвгдеёжзийклмнопрстуфхцчшщъыьэюя«»—–…№✓‹›·×";

    private final Skin skin;
    private final BitmapFont titleFont;      // 16 bold: window / panel headers
    private final BitmapFont headingFont;    // 14 bold: section captions, tree categories
    private final BitmapFont defaultFont;    // 14 regular: primary text
    private final BitmapFont secondaryFont;  // 13 regular: secondary information
    private final BitmapFont smallFont;      // 12 regular: captions, status, hints

    private final Drawable tooltipBackground;
    private final Drawable popupBackground;
    private final Drawable cardBackground;
    private final Drawable rowIdle;
    private final Drawable rowHover;
    private final Drawable rowSelected;
    private final Drawable fieldBackground;
    private final Drawable fieldBackgroundFocused;
    private final Drawable panelBackground;
    private final Drawable menuBarBackground;
    private final Drawable statusBarBackground;
    private final Drawable leftPanelBackground;
    private final Drawable rightPanelBackground;

    public UiTheme() {
        titleFont = createFont("assets/fonts/PTSans-Bold.ttf", 16);
        headingFont = createFont("assets/fonts/PTSans-Bold.ttf", 14);
        defaultFont = createFont("assets/fonts/PTSans-Regular.ttf", 14);
        secondaryFont = createFont("assets/fonts/PTSans-Regular.ttf", 13);
        smallFont = createFont("assets/fonts/PTSans-Regular.ttf", 12);

        skin = new Skin();
        skin.add("title", titleFont);
        skin.add("heading", headingFont);
        skin.add("regular", defaultFont);
        skin.add("secondary", secondaryFont);
        skin.add("small", smallFont);

        skin.add("white", new Texture(makeFlatPixmap(Color.WHITE)));

        tooltipBackground = roundedBordered(TOOLTIP_BG, BORDER, RADIUS_M);
        skin.add("tooltip_bg", tooltipBackground);
        popupBackground = roundedBordered(PANEL_BG, BORDER, RADIUS_M);
        skin.add("popup_bg", popupBackground);
        cardBackground = rounded(SURFACE, RADIUS_L);
        skin.add("card_bg", cardBackground);
        rowIdle = roundedPadded(new Color(0, 0, 0, 0), RADIUS_S, SPACE_S, SPACE_XS);
        skin.add("row_idle", rowIdle);
        rowHover = roundedPadded(SURFACE_HOVER, RADIUS_S, SPACE_S, SPACE_XS);
        skin.add("row_hover", rowHover);
        rowSelected = roundedBorderedPadded(SELECTED_BG, ACCENT, RADIUS_S, SPACE_S, SPACE_XS);
        skin.add("row_selected", rowSelected);
        fieldBackground = roundedBordered(SURFACE, BORDER, RADIUS_M);
        skin.add("field_bg", fieldBackground);
        fieldBackgroundFocused = roundedBordered(SURFACE_HOVER, ACCENT, RADIUS_M);
        skin.add("field_bg_focused", fieldBackgroundFocused);

        // The chrome strips: opaque dark, square, with a hairline on the side
        // facing the 3D work area.
        panelBackground = flat(PANEL_BG);
        menuBarBackground = edgePanel(PANEL_BG, false, false, false, true);
        statusBarBackground = edgePanel(PANEL_BG, false, false, true, false);
        leftPanelBackground = edgePanel(PANEL_BG, false, true, false, false);
        rightPanelBackground = edgePanel(PANEL_BG, true, false, false, false);

        addStyles();
    }

    public Skin skin() {
        return skin;
    }

    /** Plain opaque panel fill, no border. */
    public Drawable panelBackground() {
        return panelBackground;
    }

    /** Menu bar strip: panel fill with a hairline at its bottom edge. */
    public Drawable menuBarBackground() {
        return menuBarBackground;
    }

    /** Status bar strip: panel fill with a hairline at its top edge. */
    public Drawable statusBarBackground() {
        return statusBarBackground;
    }

    /** Left docked panel: panel fill with a hairline at its right edge. */
    public Drawable leftPanelBackground() {
        return leftPanelBackground;
    }

    /** Right docked panel: panel fill with a hairline at its left edge. */
    public Drawable rightPanelBackground() {
        return rightPanelBackground;
    }

    /** Background of the context menu and the dropdown lists. */
    public Drawable popupBackground() {
        return popupBackground;
    }

    /** Rounded bordered background of the hover tooltip. */
    public Drawable tooltipBackground() {
        return tooltipBackground;
    }

    /** Rounded surface of a compact information card (schedule lessons etc.). */
    public Drawable cardBackground() {
        return cardBackground;
    }

    /** Transparent idle state with the same metrics as hover and selection. */
    public Drawable rowIdle() {
        return rowIdle;
    }

    /** Rounded hover highlight of a tree / list row. */
    public Drawable rowHover() {
        return rowHover;
    }

    /** Rounded highlight of the selected row. */
    public Drawable rowSelected() {
        return rowSelected;
    }

    /** Bordered rounded field background; the focused variant carries the accent outline. */
    public Drawable fieldBackground() {
        return fieldBackground;
    }

    public Drawable fieldBackgroundFocused() {
        return fieldBackgroundFocused;
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
    private Drawable flatPadded(Color color, float padX, float padY) {
        Pixmap pixmap = makeFlatPixmap(color);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        skin.add("flat_" + Integer.toHexString(color.toIntBits()) + "_" + Math.round(padX) + "_" + Math.round(padY), texture);
        NinePatch patch = new NinePatch(texture, 0, 0, 0, 0);
        patch.setPadding(Math.round(padX), Math.round(padX), Math.round(padY), Math.round(padY));
        return new NinePatchDrawable(patch);
    }

    private void addStyles() {
        // Label levels: title 16 bold → heading 14 bold → default 14 →
        // secondary 13 → small 12.
        skin.add("default", new Label.LabelStyle(defaultFont, TEXT));
        skin.add("title", new Label.LabelStyle(titleFont, TEXT));
        skin.add("heading", new Label.LabelStyle(headingFont, TEXT));
        skin.add("secondary", new Label.LabelStyle(secondaryFont, TEXT_SECONDARY));
        // Tree subcategories: the secondary size, but the full text color.
        skin.add("tree-sub", new Label.LabelStyle(secondaryFont, TEXT));
        skin.add("hint", new Label.LabelStyle(smallFont, TEXT_SECONDARY));
        skin.add("caption", new Label.LabelStyle(smallFont, TEXT_DISABLED));
        skin.add("accent", new Label.LabelStyle(defaultFont, ACCENT));
        skin.add("error", new Label.LabelStyle(smallFont, ERROR));

        // Text fields: bordered rounded surface; the focus state swaps the
        // outline to the accent color. The transparent padded variant backs
        // the search field, whose outline lives on the surrounding composite.
        TextField.TextFieldStyle field = new TextField.TextFieldStyle();
        field.font = defaultFont;
        field.fontColor = TEXT;
        field.messageFont = smallFont;
        field.messageFontColor = TEXT_DISABLED;
        field.background = roundedBorderedPadded(SURFACE, BORDER, RADIUS_M, SPACE_M + 2, SPACE_S);
        field.focusedBackground = roundedBorderedPadded(SURFACE_HOVER, ACCENT, RADIUS_M, SPACE_M + 2, SPACE_S);
        field.cursor = flat(TEXT);
        field.selection = flat(new Color(ACCENT.r, ACCENT.g, ACCENT.b, 0.35f));
        skin.add("default", field);
        skin.add("field", field);

        TextField.TextFieldStyle bare = new TextField.TextFieldStyle(field);
        bare.background = flatPadded(new Color(0, 0, 0, 0), SPACE_M + 2, SPACE_S);
        bare.focusedBackground = bare.background;
        skin.add("bare", bare);

        // Command buttons: bordered surface, hover lightens, press darkens into
        // the selected wash. checked stays null: Button toggles its checked
        // flag on every click, and a checked drawable would make command
        // buttons stick in the highlight until clicked twice.
        TextButton.TextButtonStyle button = new TextButton.TextButtonStyle();
        button.font = defaultFont;
        button.fontColor = TEXT;
        button.up = roundedBorderedPadded(SURFACE, BORDER, RADIUS_M, SPACE_M, SPACE_S);
        button.over = roundedBorderedPadded(SURFACE_HOVER, BORDER, RADIUS_M, SPACE_M, SPACE_S);
        button.down = roundedPadded(SELECTED_BG, RADIUS_M, SPACE_M, SPACE_S);
        skin.add("default", button);

        // Primary action of a dialog (e.g. «Сохранить»): the accent fill.
        TextButton.TextButtonStyle primary = new TextButton.TextButtonStyle();
        primary.font = defaultFont;
        primary.fontColor = TEXT;
        primary.up = roundedPadded(ACCENT, RADIUS_M, SPACE_M, SPACE_S);
        primary.over = roundedPadded(ACCENT_HOVER, RADIUS_M, SPACE_M, SPACE_S);
        primary.down = roundedPadded(SELECTED_BG, RADIUS_M, SPACE_M, SPACE_S);
        skin.add("primary", primary);

        // Compact in-panel action («Обновить», «+ атрибут»): small text, low key.
        TextButton.TextButtonStyle compact = new TextButton.TextButtonStyle();
        compact.font = smallFont;
        compact.fontColor = TEXT_SECONDARY;
        compact.up = roundedPadded(SURFACE, RADIUS_S, SPACE_S, SPACE_XS);
        compact.over = roundedPadded(SURFACE_HOVER, RADIUS_S, SPACE_S, SPACE_XS);
        compact.down = roundedPadded(SELECTED_BG, RADIUS_S, SPACE_S, SPACE_XS);
        skin.add("compact", compact);

        // Quiet footer link («В браузере»): transparent until hovered.
        TextButton.TextButtonStyle link = new TextButton.TextButtonStyle();
        link.font = smallFont;
        link.fontColor = TEXT_SECONDARY;
        link.up = flatPadded(new Color(0, 0, 0, 0), SPACE_S, SPACE_XS);
        link.over = flatPadded(SURFACE_HOVER, SPACE_S, SPACE_XS);
        link.down = flatPadded(SELECTED_BG, SPACE_S, SPACE_XS);
        skin.add("link", link);

        // Top-bar controls and popup rows: plain text with a flat neutral
        // highlight that extends past the text by a fixed padding. Every state
        // carries the same padding, so the metrics never change on hover, and
        // adjacent highlights sit flush against each other — the classic menu
        // bar look.
        int menuPadX = 10;
        int menuPadY = 4;
        Drawable menuUp = flatPadded(new Color(0, 0, 0, 0), menuPadX, menuPadY);
        Drawable menuHover = flatPadded(SURFACE_HOVER, menuPadX, menuPadY);

        TextButton.TextButtonStyle menuItem = new TextButton.TextButtonStyle();
        menuItem.font = defaultFont;
        menuItem.fontColor = TEXT;
        menuItem.up = menuUp;
        menuItem.over = menuHover;
        menuItem.down = menuHover;
        skin.add("menu-item", menuItem);

        Button.ButtonStyle panelToggle = new Button.ButtonStyle();
        panelToggle.up = rounded(SURFACE, RADIUS_M);
        panelToggle.over = rounded(SURFACE_HOVER, RADIUS_M);
        panelToggle.down = rounded(SELECTED_BG, RADIUS_M);
        skin.add("panel-toggle", panelToggle);
        skin.add("panel-chevron-left", panelChevron(false), Drawable.class);
        skin.add("panel-chevron-right", panelChevron(true), Drawable.class);
        skin.add("icon-search", searchIcon(), Drawable.class);

        CheckBox.CheckBoxStyle checkBox = new CheckBox.CheckBoxStyle();
        checkBox.font = defaultFont;
        checkBox.fontColor = TEXT;
        checkBox.checkboxOff = checkBoxPixmap(false);
        checkBox.checkboxOn = checkBoxPixmap(true);
        skin.add("default", checkBox);

        // The scroll style is shared: panes and the select box dropdown use it.
        // Only a quiet knob is drawn; no track, no arrows.
        ScrollPane.ScrollPaneStyle scroll = new ScrollPane.ScrollPaneStyle();
        scroll.vScrollKnob = flatMinSize(SCROLL_KNOB, 6);
        scroll.hScrollKnob = flatMinSize(SCROLL_KNOB, 6);
        skin.add("default", scroll);

        List.ListStyle list = new List.ListStyle(defaultFont, TEXT, TEXT, null);
        list.selection = roundedPadded(SELECTED_BG, RADIUS_S, SPACE_S, SPACE_XS);
        list.background = popupBackground;
        skin.add("default", list);

        SelectBox.SelectBoxStyle selectBox = new SelectBox.SelectBoxStyle();
        selectBox.font = defaultFont;
        selectBox.fontColor = TEXT;
        selectBox.background = fieldBackground;
        selectBox.backgroundOver = fieldBackgroundFocused;
        selectBox.backgroundOpen = fieldBackgroundFocused;
        selectBox.scrollStyle = scroll;
        selectBox.listStyle = list;
        skin.add("default", selectBox);

        Window.WindowStyle window = new Window.WindowStyle();
        window.titleFont = titleFont;
        window.titleFontColor = TEXT;
        NinePatch windowPatch = roundedBordered(PANEL_BG, BORDER, RADIUS_L).getPatch();
        // The title sits in the top padding band of the window background.
        windowPatch.setPadding(SPACE_L, SPACE_L, 32, SPACE_L);
        window.background = new NinePatchDrawable(windowPatch);
        skin.add("default", window);

        Tree.TreeStyle tree = new Tree.TreeStyle();
        // Collapsed / expanded subtrees show a small chevron instead of the
        // plus / minus squares.
        tree.plus = arrowChevron(false);
        tree.minus = arrowChevron(true);
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
     * Rounded surfaces share one coverage rasterizer. Each texel averages an
     * 8×8 sample grid over the exact circular corner, including its alpha;
     * linear filtering then preserves the smooth edge when the patch is drawn.
     * The nine patch stretches only the straight middle, never the corners.
     */
    private NinePatchDrawable rounded(Color color, int radius) {
        return roundedSurface(color, null, radius);
    }

    /** A rounded surface with a 1px outline: fields, popups, the window. */
    private NinePatchDrawable roundedBordered(Color fill, Color border, int radius) {
        return roundedSurface(fill, border, radius);
    }

    private NinePatchDrawable roundedSurface(Color fill, Color border, int radius) {
        int size = radius * 2 + 2;
        Pixmap pixmap = roundedPixmap(fill, border, size, radius);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        skin.add("rounded_" + Integer.toHexString(fill.toIntBits()) + "_"
            + (border == null ? "plain" : Integer.toHexString(border.toIntBits()))
            + "_" + radius, texture);
        return new NinePatchDrawable(new NinePatch(texture, radius, radius, radius, radius));
    }

    private static Pixmap roundedPixmap(Color fill, Color border, int size, int radius) {
        return roundedPixmap(fill, border, size, radius, 1f);
    }

    private static Pixmap roundedPixmap(Color fill, Color border, int size, int radius, float borderWidth) {
        int samples = 8;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int fillSamples = 0;
                int borderSamples = 0;
                for (int sy = 0; sy < samples; sy++) {
                    for (int sx = 0; sx < samples; sx++) {
                        float px = x + (sx + 0.5f) / samples;
                        float py = y + (sy + 0.5f) / samples;
                        if (roundedEdgeDistance(px, py, size, size, radius) > 0f) {
                            continue;
                        }
                        // The inner curve follows the border inset, including
                        // the matching smaller radius at each corner.
                        if (border != null && roundedEdgeDistance(px - borderWidth, py - borderWidth,
                            size - 2f * borderWidth, size - 2f * borderWidth,
                            Math.max(radius - borderWidth, 0f)) > 0f) {
                            borderSamples++;
                        } else {
                            fillSamples++;
                        }
                    }
                }
                float fillAlpha = fillSamples * fill.a;
                float borderAlpha = border == null ? 0f : borderSamples * border.a;
                float alpha = fillAlpha + borderAlpha;
                if (alpha == 0f) {
                    continue;
                }
                // Coverage is averaged in premultiplied space, then stored as
                // straight alpha for Scene2D's standard blending.
                float r = fill.r * fillAlpha + (border == null ? 0f : border.r * borderAlpha);
                float g = fill.g * fillAlpha + (border == null ? 0f : border.g * borderAlpha);
                float b = fill.b * fillAlpha + (border == null ? 0f : border.b * borderAlpha);
                pixmap.drawPixel(x, y, Color.rgba8888(r / alpha, g / alpha, b / alpha,
                    alpha / (samples * samples)));
            }
        }
        return pixmap;
    }

    /** A rounded background whose nine patch padding insets the content. */
    private NinePatchDrawable roundedPadded(Color color, int radius, float padX, float padY) {
        NinePatch patch = rounded(color, radius).getPatch();
        patch.setPadding(padX, padX, padY, padY);
        return new NinePatchDrawable(patch);
    }

    private NinePatchDrawable roundedBorderedPadded(Color fill, Color border, int radius, float padX, float padY) {
        NinePatch patch = roundedBordered(fill, border, radius).getPatch();
        patch.setPadding(padX, padX, padY, padY);
        return new NinePatchDrawable(patch);
    }

    /**
     * Opaque panel fill with a hairline drawn along one side. The line lives
     * in the outermost pixel column / row, so the nine patch keeps it exactly
     * one pixel wide at any widget size.
     */
    private NinePatchDrawable edgePanel(Color fill, boolean left, boolean right, boolean top, boolean bottom) {
        int size = 8;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(fill);
        pixmap.fill();
        pixmap.setColor(BORDER);
        if (left) pixmap.fillRectangle(0, 0, 1, size);
        if (right) pixmap.fillRectangle(size - 1, 0, 1, size);
        if (top) pixmap.fillRectangle(0, 0, size, 1);
        if (bottom) pixmap.fillRectangle(0, size - 1, size, 1);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        skin.add("edge_" + Integer.toHexString(fill.toIntBits())
            + (left ? "L" : "") + (right ? "R" : "") + (top ? "T" : "") + (bottom ? "B" : ""), texture);
        return new NinePatchDrawable(new NinePatch(texture, 2, 2, 2, 2));
    }

    /** A flat tint with an explicit minimum size: the scroll knob width. */
    private Drawable flatMinSize(Color color, float min) {
        Pixmap pixmap = new Pixmap(Math.round(min), Math.round(min), Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        skin.add("flat_min_" + Integer.toHexString(color.toIntBits()) + "_" + Math.round(min), texture);
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
        drawable.setMinWidth(min);
        drawable.setMinHeight(min);
        return drawable;
    }

    /**
     * Small triangular expansion marker of the category tree: pointing right
     * while the subtree is collapsed and down once it is expanded. Drawn
     * supersampled for a smooth stroke.
     */
    private Drawable arrowChevron(boolean expanded) {
        int scale = 4;
        int size = 10;
        Pixmap pixmap = new Pixmap(size * scale, size * scale, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(TEXT_SECONDARY);
        // Two straight strokes meeting at the middle: ">" collapsed, "v" expanded.
        float x1 = expanded ? 2.5f : 3.5f, y1 = 2.5f;
        float x2 = expanded ? 5f : 7.5f, y2 = 5f;
        float x3 = expanded ? 7.5f : 3.5f, y3 = expanded ? 2.5f : 7.5f;
        stroke(pixmap, scale, x1, y1, x2, y2, 0.9f);
        stroke(pixmap, scale, x2, y2, x3, y3, 0.9f);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        skin.add("tree_chevron_" + (expanded ? "down" : "right"), texture);
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
        drawable.setMinWidth(size);
        drawable.setMinHeight(size);
        return drawable;
    }

    /** Antialiased chevron of the panel collapse buttons, drawn independently
     *  of the UI font. */
    private Drawable panelChevron(boolean pointsRight) {
        int scale = 4;
        Pixmap pixmap = new Pixmap(12 * scale, 12 * scale, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(TEXT_SECONDARY);
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

    /** Small magnifier of the search field: a ring plus a diagonal handle. */
    private Drawable searchIcon() {
        int scale = 4;
        int size = 14;
        Pixmap pixmap = new Pixmap(size * scale, size * scale, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(TEXT_SECONDARY);
        float cx = 6f, cy = 6f, r = 3.6f;
        for (int y = 0; y < size * scale; y++) {
            for (int x = 0; x < size * scale; x++) {
                float px = (x + 0.5f) / scale;
                float py = (y + 0.5f) / scale;
                float dist = (float) Math.sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy));
                if (Math.abs(dist - r) <= 0.75f) {
                    pixmap.drawPixel(x, y);
                    continue;
                }
                if (strokeDistance(px, py, 8.8f, 8.8f, 12.4f, 12.4f) <= 0.75f) {
                    pixmap.drawPixel(x, y);
                }
            }
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        skin.add("icon_search", texture);
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
        drawable.setMinWidth(size);
        drawable.setMinHeight(size);
        return drawable;
    }

    /** Draws a straight stroke by walking the pixels near the segment. */
    private static void stroke(Pixmap pixmap, int scale, float x1, float y1, float x2, float y2, float width) {
        for (int y = 0; y < pixmap.getHeight(); y++) {
            for (int x = 0; x < pixmap.getWidth(); x++) {
                float px = (x + 0.5f) / scale;
                float py = (y + 0.5f) / scale;
                if (strokeDistance(px, py, x1, y1, x2, y2) <= width) {
                    pixmap.drawPixel(x, y);
                }
            }
        }
    }

    /** Distance from a point to the segment (x1,y1)-(x2,y2). */
    private static float strokeDistance(float px, float py, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        float lengthSq = dx * dx + dy * dy;
        float t = lengthSq == 0 ? 0
            : ((px - x1) * dx + (py - y1) * dy) / lengthSq;
        t = Math.max(0f, Math.min(1f, t));
        float nearX = x1 + t * dx, nearY = y1 + t * dy;
        return (float) Math.sqrt((px - nearX) * (px - nearX) + (py - nearY) * (py - nearY));
    }

    /** Square checkbox drawable; the checked variant carries a white tick. */
    private Drawable checkBoxPixmap(boolean checked) {
        int scale = 4;
        int size = 16;
        Pixmap pixmap = roundedPixmap(checked ? ACCENT : SURFACE,
            checked ? null : BORDER, size * scale, RADIUS_S * scale, scale);
        if (checked) {
            pixmap.setColor(TEXT);
            stroke(pixmap, scale, 4, 8.2f, 6.8f, 11f, 0.9f);
            stroke(pixmap, scale, 6.8f, 11f, 11.6f, 5f, 0.9f);
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        skin.add("checkbox_" + (checked ? "on" : "off"), texture);
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
        drawable.setMinWidth(size);
        drawable.setMinHeight(size);
        return drawable;
    }

    /** Signed distance to an exact rounded rectangle: negative inside. */
    private static float roundedEdgeDistance(float px, float py, float width, float height, float radius) {
        float qx = Math.abs(px - width / 2f) - (width / 2f - radius);
        float qy = Math.abs(py - height / 2f) - (height / 2f - radius);
        float dx = Math.max(qx, 0f);
        float dy = Math.max(qy, 0f);
        return (float) Math.sqrt(dx * dx + dy * dy) + Math.min(Math.max(qx, qy), 0f) - radius;
    }

    @Override
    public void dispose() {
        // Disposes the skin and every font / texture registered in it.
        skin.dispose();
    }
}
