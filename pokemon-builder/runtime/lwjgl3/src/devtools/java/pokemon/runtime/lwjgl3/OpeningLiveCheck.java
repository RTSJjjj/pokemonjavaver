package pokemon.runtime.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.map.GraphicsLocator;
import pokemon.runtime.ui.menu.OpeningScreen;

import java.io.File;
import java.nio.file.Files;

/**
 * Plays the real opening (audio on, volume 0) in a hidden window for a few seconds through the production
 * {@code show()} / {@code render()} path, checks that the music runs and drives the clock, then skips and checks
 * that the next screen was shown. Arguments: {@code <graphics root holding Opening/> <dataRoot> [seconds]}.
 */
public final class OpeningLiveCheck extends ApplicationAdapter {

    private final String[] args;
    private RuntimeContext context;
    private OpeningScreen screen;
    private boolean nextShown;
    private long started;
    private float seconds;
    private Throwable failure;

    private OpeningLiveCheck(String[] args) {
        this.args = args;
    }

    public static void main(String[] args) throws Exception {
        File homes = Files.createTempDirectory("pb-opening-live-").toFile();
        System.setProperty("user.home", homes.getAbsolutePath());
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setInitialVisible(false);
        config.setWindowedMode(672, 448);
        OpeningLiveCheck check = new OpeningLiveCheck(args);
        new Lwjgl3Application(check, config);
        if (check.failure != null) {
            throw new RuntimeException("opening live check failed", check.failure);
        }
        System.out.println("LIVE_CHECK_OK");
    }

    @Override
    public void create() {
        try {
            seconds = args.length > 2 ? Float.parseFloat(args[2]) : 4f;
            ScreenMetrics.configure(672, 448);
            pokemon.runtime.app.PokemonGame game = new pokemon.runtime.app.PokemonGame(args[1], -1, null);
            context = new RuntimeContext("live-check", game, args[1]);
            context.loadRuntimeConfig();
            context.audioManager().setVolumeFactors(0f, 0f, 0f);          // silent run
            screen = OpeningScreen.create(context, () -> {
                nextShown = true;
                return new ScreenAdapter();
            }, new GraphicsLocator(new File(args[0]), null));
            if (screen == null) {
                throw new IllegalStateException("no opening pack under " + args[0]);
            }
            game.setScreen(screen);                                         // show(): music opens and starts
            started = System.nanoTime();
        } catch (Throwable error) {
            failure = error;
            Gdx.app.exit();
        }
    }

    @Override
    public void render() {
        if (failure != null || screen == null) {
            return;
        }
        try {
            float wall = (System.nanoTime() - started) / 1e9f;
            screen.render(Gdx.graphics.getDeltaTime());
            if (wall >= seconds) {
                float clock = screen.clock();
                System.out.printf("wall %.2f s, opening clock %.2f s, music playing %b%n", wall, clock, screen.musicPlaying());
                if (!screen.musicPlaying()) {
                    throw new AssertionError("the opening track is not playing");
                }
                if (clock < seconds * 0.8f || clock > seconds + 0.5f) {
                    throw new AssertionError("clock " + clock + " does not follow " + wall + " s of play");
                }
                screen.skip();
                if (!nextShown) {
                    throw new AssertionError("skip did not show the next screen");
                }
                screen = null;
                Gdx.app.exit();
            }
        } catch (Throwable error) {
            failure = error;
            Gdx.app.exit();
        }
    }
}
