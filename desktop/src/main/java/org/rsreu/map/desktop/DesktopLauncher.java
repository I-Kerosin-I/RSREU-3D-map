package org.rsreu.map.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import org.rsreu.map.MapGame;

/**
 * Desktop launcher for RSREU 3D Map application.
 * <p>
 * This is the only place where the window and framebuffer are configured. An
 * earlier version had a second copy of this setup in {@link MapGame#main(String[])},
 * which was silently ignored because the run configuration starts
 * {@code DesktopLauncher}.
 */
public class DesktopLauncher {

    private static final int WIDTH = 1440;
    private static final int HEIGHT = 900;

    /**
     * Multisample count of the back buffer. libGDX defaults to 0, which means no
     * anti-aliasing at all: straight lines and jagged silhouettes.
     */
    private static final int SAMPLES = 4;

    /**
     * libGDX defaults to a 16 bit depth buffer, which is far too coarse for the
     * near and far planes of a 3D scene and makes surfaces shimmer while the
     * camera moves.
     */
    private static final int DEPTH_BITS = 24;

    /**
     * Window / taskbar icon. The LWJGL3 backend reads these through Pixmap, which
     * has no .ico support, so the frames of {@code assets/RSREU.ico} are shipped
     * as pre-rendered PNG sizes and GLFW picks the closest match.
     */
    private static final String[] WINDOW_ICONS = {
        "assets/icons/RSREU_16.png",
        "assets/icons/RSREU_32.png",
        "assets/icons/RSREU_48.png",
        "assets/icons/RSREU_64.png",
        "assets/icons/RSREU_128.png",
        "assets/icons/RSREU_256.png",
    };

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("RSREU 3D Map");
        config.setWindowIcon(WINDOW_ICONS);
        com.badlogic.gdx.Graphics.DisplayMode display = Lwjgl3ApplicationConfiguration.getDisplayMode();
        int width = Math.min(WIDTH, display.width - Math.min(80, display.width / 10));
        int height = Math.min(HEIGHT, display.height - Math.min(120, display.height / 10));
        config.setWindowedMode(Math.max(1, width), Math.max(1, height));
        // red, green, blue, alpha, depth, stencil, samples
        config.setBackBufferConfig(8, 8, 8, 8, DEPTH_BITS, 0, SAMPLES);
        config.useVsync(true);
        config.setForegroundFPS(60);

        new Lwjgl3Application(new MapGame(), config);
    }
}