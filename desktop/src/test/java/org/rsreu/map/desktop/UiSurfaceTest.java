package org.rsreu.map.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import org.rsreu.map.ui.UiTheme;

import java.util.concurrent.atomic.AtomicReference;

/** Pixel regression checks for the theme's rounded surfaces, hairline borders and tree chevrons. */
public final class UiSurfaceTest {

    public static void main(String[] args) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("UI surface regression");
        config.setWindowedMode(320, 240);
        config.setInitialVisible(false);
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    checkSurfaces();
                    System.out.println("UiSurfaceTest: all checks passed");
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, config);
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    private static void checkSurfaces() {
        UiTheme theme = new UiTheme();
        try {
            checkSurface(theme.fieldBackground(), UiTheme.SURFACE, UiTheme.BORDER, UiTheme.RADIUS_M);
            checkSurface(theme.fieldBackgroundFocused(), UiTheme.SURFACE_HOVER, UiTheme.ACCENT, UiTheme.RADIUS_M);
            checkSurface(theme.popupBackground(), UiTheme.PANEL_BG, UiTheme.BORDER, UiTheme.RADIUS_M);
            checkSurface(theme.cardBackground(), UiTheme.SURFACE, null, UiTheme.RADIUS_L);
            checkTooltip(theme.tooltipBackground());
            TextButton.TextButtonStyle button = theme.skin().get(TextButton.TextButtonStyle.class);
            checkSurface(button.up, UiTheme.SURFACE, UiTheme.BORDER, UiTheme.RADIUS_M);
            Window.WindowStyle window = theme.skin().get(Window.WindowStyle.class);
            checkSurface(window.background, UiTheme.PANEL_BG, UiTheme.BORDER, UiTheme.RADIUS_L);
            Tree.TreeStyle tree = theme.skin().get("tree", Tree.TreeStyle.class);
            checkTreeChevron(tree.minus, true, "expanded tree minus");
            checkTreeChevron(tree.plus, false, "collapsed tree plus");

            TextField.TextFieldStyle field = theme.skin().get(TextField.TextFieldStyle.class);
            require(field.background.getLeftWidth() == UiTheme.SPACE_M + 2, "field left padding applied");
            require(field.background.getRightWidth() == field.background.getLeftWidth(), "field padding symmetric");
            require(field.background.getTopHeight() == UiTheme.SPACE_S, "field top padding applied");
            require(button.up.getLeftWidth() == UiTheme.SPACE_M, "button padding applied");
            require(window.background.getTopHeight() == 32, "window title padding applied");
            require(window.background.getLeftWidth() == UiTheme.SPACE_L, "window side padding applied");
        } finally {
            theme.dispose();
        }
    }

    private static void checkSurface(Drawable drawable, Color fill, Color border, int radius) {
        Pixmap small = render(drawable, 180, 48);
        Pixmap large = render(drawable, 360, 96);
        try {
            require((small.getPixel(0, 0) & 255) == 0, "corner outside arc is transparent");
            require((small.getPixel(2, 3) & 255) > 240, "corner follows a circular arc, not a diagonal bevel");
            int partialPixels = 0;
            for (int y = 0; y < radius; y++) {
                for (int x = 0; x < radius; x++) {
                    int pixel = small.getPixel(x, y);
                    int alpha = pixel & 255;
                    if (alpha > 0 && alpha < 255) partialPixels++;
                    require(pixel == small.getPixel(small.getWidth() - 1 - x, y), "horizontal corner symmetry");
                    require(pixel == small.getPixel(x, small.getHeight() - 1 - y), "vertical corner symmetry");
                    require(pixel == large.getPixel(x, y), "corner radius unchanged by stretching");
                }
            }
            require(partialPixels >= 3, "curved edge has antialiased coverage");
            int mid = small.getWidth() / 2;
            require(closeColor(small.getPixel(mid, 0), border == null ? fill : border), "first row has border color");
            require(closeColor(small.getPixel(mid, 1), fill), "border is exactly one pixel thick");
            require(closeColor(small.getPixel(0, small.getHeight() / 2), border == null ? fill : border),
                "left edge has border color");
            require(closeColor(small.getPixel(1, small.getHeight() / 2), fill), "vertical border is one pixel thick");
        } finally {
            small.dispose();
            large.dispose();
        }
    }

    private static void checkTooltip(Drawable drawable) {
        Pixmap pixels = render(drawable, 280, 80);
        try {
            int mid = pixels.getWidth() / 2;
            require(closeColor(pixels.getPixel(mid, 0), UiTheme.BORDER), "tooltip outline is opaque");
            require((pixels.getPixel(mid, 1) & 255) == 242, "tooltip fill starts after one pixel");
            require((pixels.getPixel(0, 0) & 255) == 0, "tooltip corner outside arc is transparent");
            int alpha = pixels.getPixel(1, 2) & 255;
            require(alpha > 0 && alpha < 242, "tooltip curved edge has partial coverage");
        } finally {
            pixels.dispose();
        }
    }

    private static void checkTreeChevron(Drawable drawable, boolean expanded, String label) {
        // Render above the ten-pixel display size so both strokes have measurable coverage.
        int size = 40;
        int alphaTolerance = 4;
        Pixmap pixels = render(drawable, size, size);
        try {
            int coveredPixels = 0;
            double[] coverage = new double[3];
            double[] position = new double[3];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int alpha = topDownAlpha(pixels, x, y);
                    int reflected = expanded
                        ? topDownAlpha(pixels, size - 1 - x, y)
                        : topDownAlpha(pixels, x, size - 1 - y);
                    require(Math.abs(alpha - reflected) <= alphaTolerance,
                        label + " has symmetric strokes within raster alpha tolerance");
                    if (alpha <= alphaTolerance) continue;
                    coveredPixels++;

                    // Compare the tip's alpha-weighted position with both outer arms.
                    int across = expanded ? x : y;
                    if (across < size / 5 || across >= 4 * size / 5) continue;
                    int band = across < 2 * size / 5 ? 0 : across < 3 * size / 5 ? 1 : 2;
                    coverage[band] += alpha;
                    position[band] += alpha * (expanded ? y : x);
                }
            }
            require(coveredPixels >= size, label + " renders a nonempty chevron");
            for (int band = 0; band < coverage.length; band++) {
                require(coverage[band] > 0, label + " renders both arms and the central tip");
                position[band] /= coverage[band];
            }
            require(Math.abs(position[0] - position[2]) <= 0.5,
                label + " arms have matching positions, not a slash");
            require(position[1] > position[0] + size * 0.05
                    && position[1] > position[2] + size * 0.05,
                label + (expanded ? " points down as a V" : " points right as a chevron"));
        } finally {
            pixels.dispose();
        }
    }

    private static int topDownAlpha(Pixmap pixels, int x, int y) {
        // Framebuffer readback starts at the bottom; texture artwork uses top-down rows.
        return pixels.getPixel(x, pixels.getHeight() - 1 - y) & 255;
    }

    private static Pixmap render(Drawable drawable, int width, int height) {
        FrameBuffer buffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
        SpriteBatch batch = new SpriteBatch();
        buffer.begin();
        try {
            Gdx.gl.glClearColor(0, 0, 0, 0);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, width, height));
            batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA,
                GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
            batch.begin();
            drawable.draw(batch, 0, 0, width, height);
            batch.end();
            return Pixmap.createFromFrameBuffer(0, 0, width, height);
        } finally {
            buffer.end();
            batch.dispose();
            buffer.dispose();
        }
    }

    private static boolean closeColor(int pixel, Color expected) {
        int color = Color.rgba8888(expected);
        for (int shift = 0; shift < 32; shift += 8) {
            if (Math.abs(((pixel >>> shift) & 255) - ((color >>> shift) & 255)) > 1) return false;
        }
        return true;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
