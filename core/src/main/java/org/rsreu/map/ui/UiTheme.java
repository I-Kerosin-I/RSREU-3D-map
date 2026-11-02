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
 */
public final class UiTheme implements Disposable {

    // Centralized palette; the accent matches the edge wireframe color.
    public static final Color TEXT = new Color(0xE6EBF4FF);
    public static final Color TEXT_DIM = new Color(0x94A0B8FF);
    public static final Color ACCENT = new Color(0x2F6DB2FF);
    public static final Color PANEL_BG = new Color(0x10151FF0);
    public static final Color TOOLTIP_BG = new Color(0x0C111AE6);
    public static final Color FIELD_BG = new Color(0x1A2230FF);
    public static final Color FIELD_BG_FOCUSED = new Color(0x212B3DFF);
    public static final Color ROW_HOVER = new Color(0x2F6DB250);
    public static final Color TREE_SELECTION = new Color(0x2F6DB260);
    public static final Color SCROLL_TRACK = new Color(0xFFFFFF18);
    public static final Color SCROLL_KNOB = new Color(0xFFFFFF40);

    /** Default ASCII charset of FreeTypeFontGenerator plus Russian glyphs and marks. */
    private static final String CHARACTERS = FreeTypeFontGenerator.DEFAULT_CHARS
        + "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
        + "абвгдеёжзийклмнопрстуфхцчшщъыьэюя«»—–…№";

    private final Skin skin;
    private final BitmapFont regularFont;
    private final BitmapFont boldFont;
    private final BitmapFont smallFont;
    private final Drawable panelBackground;
    private final Drawable tooltipBackground;

    public UiTheme() {
        regularFont = createFont("assets/fonts/PTSans-Regular.ttf", 15);
        boldFont = createFont("assets/fonts/PTSans-Bold.ttf", 16);
        smallFont = createFont("assets/fonts/PTSans-Regular.ttf", 12);

        skin = new Skin();
        skin.add("regular", regularFont);
        skin.add("bold", boldFont);
        skin.add("small", smallFont);

        skin.add("white", new Texture(makeFlatPixmap(Color.WHITE)));

        panelBackground = rounded(PANEL_BG, 10);
        tooltipBackground = rounded(TOOLTIP_BG, 8);
        skin.add("panel_bg", panelBackground);
        skin.add("tooltip_bg", tooltipBackground);

        addStyles();
    }

    public Skin skin() {
        return skin;
    }

    /** Rounded panel background used by the side panel. */
    public Drawable panelBackground() {
        return panelBackground;
    }

    /** Rounded background used by the hover tooltip. */
    public Drawable tooltipBackground() {
        return tooltipBackground;
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
        field.background = flat(FIELD_BG);
        field.focusedBackground = flat(FIELD_BG_FOCUSED);
        field.cursor = flat(TEXT);
        field.selection = flat(new Color(ACCENT.r, ACCENT.g, ACCENT.b, 0.35f));
        skin.add("field", field);

        ScrollPane.ScrollPaneStyle scroll = new ScrollPane.ScrollPaneStyle();
        scroll.vScroll = flat(SCROLL_TRACK);
        scroll.vScrollKnob = flat(SCROLL_KNOB);
        scroll.hScroll = flat(SCROLL_TRACK);
        scroll.hScrollKnob = flat(SCROLL_KNOB);
        skin.add("default", scroll);

        Tree.TreeStyle tree = new Tree.TreeStyle();
        tree.plus = treeIcon(true);
        tree.minus = treeIcon(false);
        tree.selection = flat(TREE_SELECTION);
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
     * their radius at any widget size.
     */
    private Drawable rounded(Color color, int radius) {
        int size = radius * 2 + 2;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fillRectangle(radius, 0, size - 2 * radius, size);
        pixmap.fillRectangle(0, radius, size, size - 2 * radius);
        pixmap.fillCircle(radius, radius, radius);
        pixmap.fillCircle(size - radius - 1, radius, radius);
        pixmap.fillCircle(radius, size - radius - 1, radius);
        pixmap.fillCircle(size - radius - 1, size - radius - 1, radius);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        skin.add("rounded_" + Integer.toHexString(color.toIntBits()) + "_" + radius, texture);
        NinePatchDrawable drawable = new NinePatchDrawable(
            new NinePatch(texture, radius, radius, radius, radius));
        return drawable;
    }

    /** Small +/- expansion marker of the category tree. */
    private Drawable treeIcon(boolean plus) {
        Pixmap pixmap = new Pixmap(12, 12, Pixmap.Format.RGBA8888);
        pixmap.setColor(TEXT_DIM);
        pixmap.fillRectangle(2, 5, 8, 2);
        if (plus) {
            pixmap.fillRectangle(5, 2, 2, 8);
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        skin.add("tree_icon_" + (plus ? "plus" : "minus"), texture);
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    @Override
    public void dispose() {
        // Disposes the skin and every font / texture registered in it.
        skin.dispose();
    }
}
