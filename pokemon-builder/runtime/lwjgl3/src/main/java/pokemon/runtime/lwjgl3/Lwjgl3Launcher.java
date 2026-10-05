package pokemon.runtime.lwjgl3;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import pokemon.runtime.app.DisplaySettings;
import pokemon.runtime.app.PokemonGame;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.data.ProjectInfo;

import java.io.File;
import java.util.List;

/**
 * Desktop launcher (project3 section 38): window mode, fullscreen, resolution
 * and VSync start here.
 *
 * <p>Usage: {@code PokemonGame [runtime-data-root] [mapId] [flags]}. Without a
 * data root the runtime resolves generated/ (POKEMON_RUNTIME_DATA, the
 * pokemon.runtime.data property or the default layout). Flags (R11):
 * {@code --fullscreen}, {@code --size 1280x720}, {@code --logical 672x448},
 * {@code --no-vsync}, {@code --resizable}.</p>
 */
public final class Lwjgl3Launcher {

    private Lwjgl3Launcher() {
    }

    public static void main(String[] args) {
        installCrashLog();
        List<String> positional = DisplaySettings.positionals(args);
        DisplaySettings display = DisplaySettings.parse(args);
        // R12: a packaged app-image keeps its runtime data next to the exe; the
        // CWD of a double-clicked exe is not guaranteed to be the app folder,
        // so fall back to jpackage's app directory before the CWD default.
        String dataRoot = positional.isEmpty() ? packagedDataRoot() : positional.get(0);
        // R6.21: the render resolution defaults to the source project's own
        // screen size (project.json runtime profile); --logical still wins and
        // the window keeps its 672x488 integer scale unit.
        if (dataRoot != null) {
            int[] project = ProjectInfo.screenSize(new File(dataRoot));
            if (project != null) {
                display = display.withProjectLogical(project[0], project[1]);
            }
        }
        ScreenMetrics.configure(display.logicalWidth, display.logicalHeight);
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Pokemon Game");
        config.useVsync(display.vsync);
        config.setResizable(display.resizable);
        if (display.fullscreen) {
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        } else {
            // R11: default to the largest integer scale that fits the monitor, so
            // the pixel art is crisp and the window is comfortable to play in.
            Graphics.DisplayMode mode = Lwjgl3ApplicationConfiguration.getDisplayMode();
            int[] window = display.resolveWindow(mode.width, mode.height);
            config.setWindowedMode(window[0], window[1]);
            System.out.println("[PokemonGame] window " + window[0] + "x" + window[1]
                    + " on a " + mode.width + "x" + mode.height + " display");
        }
        config.setForegroundFPS(60);
        System.out.println("[PokemonGame] display: " + display.describe());
        int startMap = -1;
        if (positional.size() > 1) {
            try {
                startMap = Integer.parseInt(positional.get(1));
            } catch (NumberFormatException error) {
                System.err.println("ignoring invalid map id argument: " + positional.get(1));
            }
        }
        new Lwjgl3Application(new CrashGuardedListener(
                new PokemonGame(dataRoot, startMap, new Lwjgl3KeyStateSource())), config);
    }

    /**
     * Writes any uncaught exception to {@code ~/pokemon-runtime-crash.log} and
     * still prints it, so a crash is diagnosable even when the launcher hides
     * stdout (jpackage). A render-loop failure is caught and rethrown so libGDX
     * still stops, but the file keeps the stack.
     */
    private static void installCrashLog() {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> writeCrash(error));
    }

    static void writeCrash(Throwable error) {
        error.printStackTrace();
        try {
            File file = new File(System.getProperty("user.home", "."), "pokemon-runtime-crash.log");
            try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter(file, true))) {
                out.println("=== " + new java.util.Date() + " thread="
                        + Thread.currentThread().getName() + " ===");
                error.printStackTrace(out);
            }
            System.err.println("[PokemonGame] crash log: " + file.getAbsolutePath());
        } catch (Exception ignored) {
            // never let crash logging hide the original error
        }
    }

    /** Forwards to the game, capturing a render-loop throwable first. */
    private static final class CrashGuardedListener implements ApplicationListener {
        private final ApplicationListener game;

        CrashGuardedListener(ApplicationListener game) {
            this.game = game;
        }

        @Override
        public void create() {
            game.create();
        }

        @Override
        public void resize(int width, int height) {
            game.resize(width, height);
        }

        @Override
        public void render() {
            try {
                game.render();
            } catch (Throwable error) {
                writeCrash(error);
                throw error;
            }
        }

        @Override
        public void pause() {
            game.pause();
        }

        @Override
        public void resume() {
            game.resume();
        }

        @Override
        public void dispose() {
            game.dispose();
        }
    }

    /**
     * R12: runtime data of a packaged app-image. The normal layout searches the
     * working directory ({@code generated/} or {@code runtime-data/}); when the
     * launcher was started from {@code PokemonGame.exe}, the exe's directory is
     * the authoritative place (jpackage exposes it as {@code jpackage.app-path}).
     *
     * @return an explicit data root, or null so RuntimeDataLocator keeps its
     *         CWD / environment / property resolution
     */
    private static String packagedDataRoot() {
        if (hasRuntimeData(new File(""))) {
            return null;
        }
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && !appPath.isEmpty()) {
            File directory = new File(appPath).getParentFile();
            if (directory != null) {
                File runtimeData = new File(directory, "runtime-data");
                if (runtimeData.isDirectory()) {
                    return runtimeData.getAbsolutePath();
                }
                File generated = new File(directory, "generated");
                if (generated.isDirectory()) {
                    return generated.getAbsolutePath();
                }
            }
        }
        return null;
    }

    private static boolean hasRuntimeData(File base) {
        return new File(base, "runtime-data").isDirectory()
                || new File(base, "generated").isDirectory();
    }
}
