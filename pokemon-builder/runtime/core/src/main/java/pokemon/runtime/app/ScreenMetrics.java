package pokemon.runtime.app;

/**
 * Logical game resolution (project3 section 39). The map screen renders at
 * this size and a FitViewport scales it onto any window (16:9 / 16:10 / 4:3 /
 * ultrawide) without stretching, so the pixel art and the UI keep their aspect.
 *
 * <p>The launcher may override it ({@code --logical 672x448}); otherwise the
 * launcher applies the source project's own size from project.json
 * ({@code runtime.screenWidth/Height}, R6.12), falling back to the constants
 * below. This project exports 672x448 - the original RMXP framing - while
 * {@link #WINDOW_WIDTH}/{@link #WINDOW_HEIGHT} keep the window at the size it
 * has always opened with (letterbox bars fill the difference).</p>
 */
public final class ScreenMetrics {

    public static final int LOGICAL_WIDTH = 672;
    public static final int LOGICAL_HEIGHT = 488;

    /**
     * R6.21: the integer scale unit of the window itself. The window opened at
     * a multiple of 672x488 before the render resolution went back to the
     * project default, and the user asked to keep that window size.
     */
    public static final int WINDOW_WIDTH = 672;
    public static final int WINDOW_HEIGHT = 488;

    private static int logicalWidth = LOGICAL_WIDTH;
    private static int logicalHeight = LOGICAL_HEIGHT;
    private static boolean configured;

    /** True once a launcher (or the project's own size, see {@link PokemonGame}) has set the resolution. */
    public static boolean configured() {
        return configured;
    }

    /** Called once by the launcher before the game window is created. */
    public static void configure(int width, int height) {
        if (width > 0 && height > 0) {
            logicalWidth = width;
            logicalHeight = height;
            configured = true;
        }
    }

    public static int logicalWidth() {
        return logicalWidth;
    }

    public static int logicalHeight() {
        return logicalHeight;
    }

    private ScreenMetrics() { }
}
