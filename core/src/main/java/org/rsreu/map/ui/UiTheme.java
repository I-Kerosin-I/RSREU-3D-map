package org.rsreu.map.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
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
    public static final Color FIELD_BG = new Color(0x1A223099);
    public static final Color FIELD_BG_FOCUSED = new Color(0x212B3DB4);
    public static final Color SCROLL_TRACK = new Color(0xFFFFFF28);
    public static final Color SCROLL_KNOB = new Color(0xFFFFFF66);

    /** Default ASCII charset of FreeTypeFontGenerator plus Russian glyphs and marks. */
    private static final String CHARACTERS = FreeTypeFontGenerator.DEFAULT_CHARS
        + "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
        + "абвгдеёжзийклмнопрстуфхцчшщъыьэюя«»—–…№";

    private final Skin skin;
    private final BitmapFont regularFont;
    private final BitmapFont boldFont;
    private final BitmapFont smallFont;
    private final Drawable tooltipBackground;
    private final Drawable chipBackground;
    private final Drawable chipHover;
    private final Drawable chipSelected;

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

        addStyles();
    }

    public Skin skin() {
        return skin;
    }

    /** Rounded background used by the hover tooltip. */
    public Drawable tooltipBackground() {
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

    private void addStyles() {
        Label.LabelStyle labelDefault = new Label.LabelStyle(regularFont, TEXT);
        skin.add("default", labelDefault);
        skin.add("title", new Label.LabelStyle(boldFont, TEXT));
        skin.add("hint", new Label.LabelStyle(smallFont, TEXT_DIM));
        skin.add("accent", new Label.LabelStyle(regularFont, ACCENT));

        TextField.TextFieldStyle field = new TextField.TextFieldStyle();
        field.font = regularFont;
        field.fontColor = TEXT;
        field.messageFont = smallFont;
        field.messageFontColor = TEXT_DIM;
        // The rounded background carries the horizontal padding, so the typed
        // text and the placeholder sit away from the field's left edge.
        field.background = roundedPadded(FIELD_BG, 10, 10, 7);
        field.focusedBackground = roundedPadded(FIELD_BG_FOCUSED, 10, 10, 7);
        field.cursor = flat(TEXT);
        field.selection = flat(new Color(ACCENT.r, ACCENT.g, ACCENT.b, 0.35f));
        skin.add("field", field);

        ScrollPane.ScrollPaneStyle scroll = new ScrollPane.ScrollPaneStyle();
        // A 1px-wide drawable would render the bar invisible; give the track
        // and the knob a sensible minimum width.
        Drawable track = flat(SCROLL_TRACK);
        track.setMinWidth(3f);
        Drawable knob = flat(SCROLL_KNOB);
        knob.setMinWidth(3f);
        scroll.vScroll = track;
        scroll.vScrollKnob = knob;
        scroll.hScroll = flat(SCROLL_TRACK);
        scroll.hScrollKnob = flat(SCROLL_KNOB);
        skin.add("default", scroll);

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

    @Override
    public void dispose() {
        // Disposes the skin and every font / texture registered in it.
        skin.dispose();
    }
}
