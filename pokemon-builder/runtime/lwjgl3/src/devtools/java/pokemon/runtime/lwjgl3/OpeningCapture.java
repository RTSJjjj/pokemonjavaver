package pokemon.runtime.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.map.GraphicsLocator;
import pokemon.runtime.ui.menu.OpeningScreen;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Renders the opening movie at given times into PNGs, for a frame-by-frame comparison with the offline compositor.
 * Arguments: {@code <assets/opening dir> <output dir> <t1,t2,...>} (seconds). Runs with
 * {@code POKEMON_CAPTURE_MAIN=pokemon.runtime.lwjgl3.OpeningCapture scripts\jdk-capture.ps1 ...}.
 */
public final class OpeningCapture extends ApplicationAdapter {

    private final String[] args;
    private Throwable failure;

    private OpeningCapture(String[] args) {
        this.args = args;
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setInitialVisible(false);
        config.setWindowedMode(ScreenMetrics.LOGICAL_WIDTH, 448);
        config.disableAudio(true);
        OpeningCapture capture = new OpeningCapture(args);
        new Lwjgl3Application(capture, config);
        if (capture.failure != null) {
            throw new RuntimeException("opening capture failed", capture.failure);
        }
    }

    @Override
    public void create() {
        try {
            int width = 672;
            int height = 448;
            ScreenMetrics.configure(width, height);
            // the locator wants <root>/Graphics/Opening: stage the pack there
            Path root = Files.createTempDirectory("pb-opening-");
            Path target = root.resolve("Graphics").resolve("Opening");
            Files.createDirectories(target);
            try (Stream<Path> files = Files.list(Path.of(args[0]))) {
                for (Path file : (Iterable<Path>) files::iterator) {
                    Files.copy(file, target.resolve(file.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            OpeningScreen screen = OpeningScreen.create(null, () -> null, new GraphicsLocator(root.toFile(), null));
            if (screen == null) {
                throw new IllegalStateException("no opening pack in " + args[0]);
            }
            FrameBuffer buffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
            screen.resize(width, height);
            for (String value : args[2].split(",")) {
                float t = Float.parseFloat(value);
                buffer.begin();
                screen.renderAt(t);
                Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, width, height);
                buffer.end();
                File output = new File(args[1], String.format(Locale.ROOT, "java_%06.2f.png", t));
                output.getParentFile().mkdirs();
                PixmapIO.writePNG(Gdx.files.absolute(output.getAbsolutePath()), pixels, -1, true);
                pixels.dispose();
                System.out.println("wrote " + output);
            }
            buffer.dispose();
        } catch (Throwable error) {
            failure = error;
        } finally {
            Gdx.app.exit();
        }
    }
}
