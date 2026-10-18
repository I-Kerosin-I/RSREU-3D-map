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

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

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

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("RSREU 3D Map");
        config.setWindowedMode(WIDTH, HEIGHT);
        // red, green, blue, alpha, depth, stencil, samples
        config.setBackBufferConfig(8, 8, 8, 8, DEPTH_BITS, 0, SAMPLES);
        config.useVsync(true);
        config.setForegroundFPS(60);

        new Lwjgl3Application(new MapGame(), config);
    }
}