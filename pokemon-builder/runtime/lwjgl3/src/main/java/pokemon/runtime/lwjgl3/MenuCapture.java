package pokemon.runtime.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.map.GraphicsLocator;
import pokemon.runtime.map.MapScreen;
import pokemon.runtime.state.GameState;
import pokemon.runtime.ui.menu.PauseMenuOverlay;
import pokemon.runtime.ui.menu.TitleScreen;

import java.io.File;
import java.nio.file.Files;

/**
 * L1 evidence: renders the production title screen and pause menu in a hidden
 * window and writes PNGs (one per state). Arguments: dataRoot outputDir.
 *
 * <p>The music is disabled and user.home points at a scratch directory, so the
 * capture never touches the player's real saves.</p>
 */
public final class MenuCapture extends ApplicationAdapter {

    private final String[] args;
    private Throwable failure;
    private FrameBuffer buffer;
    private SpriteBatch batch;
    private OrthographicCamera projection;
    private RuntimeContext context;
    private TitleScreen title;
    private PauseMenuOverlay overlay;
    private MapScreen mapScreen;
    private int width = ScreenMetrics.LOGICAL_WIDTH;
    private int height = ScreenMetrics.LOGICAL_HEIGHT;

    private MenuCapture(String[] args) {
        this.args = args;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("dataRoot outputDir");
        }
        // Keep the capture away from the real save directory.
        File homes = Files.createTempDirectory("pb-menu-capture-").toFile();
        System.setProperty("user.home", homes.getAbsolutePath());

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setInitialVisible(false);
        config.setWindowedMode(672, 448);
        config.disableAudio(true);
        MenuCapture capture = new MenuCapture(args);
        new Lwjgl3Application(capture, config);
        if (capture.failure != null) {
            throw new RuntimeException("menu capture failed", capture.failure);
        }
    }

    @Override
    public void create() {
        try {
            pokemon.runtime.app.PokemonGame game =
                    new pokemon.runtime.app.PokemonGame(args[0], -1, null);
            context = new RuntimeContext("capture", game, args[0]);
            context.loadRuntimeConfig();
            if (context.database() == null) {
                throw new IllegalStateException("runtime data unavailable: " + args[0]);
            }
            int[] screen = pokemon.runtime.data.ProjectInfo.screenSize(new File(args[0]));
            if (screen != null) {
                ScreenMetrics.configure(screen[0], screen[1]);
            }
            width = ScreenMetrics.logicalWidth();
            height = ScreenMetrics.logicalHeight();

            // A save slot so the load screen shows real content.
            GameState state = context.gameState();
            state.enterMap(2, 9, 22);
            state.playerName("小测");
            state.switches().set(1, true);
            context.saveManager().save(context.storage(), "1", state);

            GraphicsLocator locator = new GraphicsLocator(context.database());
            batch = new SpriteBatch();
            projection = new OrthographicCamera(width, height);
            projection.position.set(width / 2f, height / 2f, 0f);
            projection.update();
            buffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);

            title = new TitleScreen(context);
            title.resize(width, height); // not driven by Game#setScreen here
            // Splash (no show(): the capture stays silent).
            GraphicsLocator probe = locator;
            System.out.println("title probe: splash="
                    + context.database().title().splashImages.size
                    + " seconds=" + context.database().title().secondsPerSplash
                    + " intro1=" + probe.find("Titles", "intro1.png")
                    + " start=" + probe.find("Titles", "start.png")
                    + " mpmbg=" + probe.find("Pictures/MPM", "bg.png"));
            title.render(0.5f); // let the first slide fade in fully
            shot("l14-title-splash");

            tapTitle(GameAction.CONFIRM); // skip the splash -> the white intro fade
            title.render(0.1f);           // a few frames into the fade
            shot("l14-title-intro");

            tapTitle(GameAction.CONFIRM); // skip the intro -> the full title
            shot("l14-title-main");

            tapTitle(GameAction.CONFIRM); // command window (继续/新的故事/设置/退出)
            shot("l14-title-commands");

            // Continue -> load view (the seeded slot).
            tapTitle(GameAction.UP);      // 继续之前的故事
            tapTitle(GameAction.CONFIRM);
            shot("l14-title-load");

            // Pause menu overlay on its own (no map screen needed for visuals).
            overlay = new PauseMenuOverlay(context, locator);
            overlay.open();
            renderOverlay();
            shot("l1-menu-main");

            tapAndRender(GameAction.DOWN, overlay); // 保存
            tapAndRender(GameAction.CONFIRM, overlay);
            renderOverlay();
            shot("l1-menu-save");
            tapAndRender(GameAction.CANCEL, overlay);

            tapAndRender(GameAction.DOWN, overlay); // 读档
            tapAndRender(GameAction.CONFIRM, overlay);
            renderOverlay();
            shot("l1-menu-load");
            tapAndRender(GameAction.CANCEL, overlay);

            tapAndRender(GameAction.DOWN, overlay); // 设置
            tapAndRender(GameAction.CONFIRM, overlay);
            renderOverlay();
            shot("l1-menu-options");
            tapAndRender(GameAction.CANCEL, overlay);

            // Trainer is the first entry; walk back up to it.
            tapAndRender(GameAction.UP, overlay);
            tapAndRender(GameAction.CONFIRM, overlay);
            renderOverlay();
            shot("l1-menu-trainer");

            // Integration: a real MapScreen - X opens the pause menu; walking to
            // 读档 and confirming swaps screens (the host disposes the old
            // screen from inside the overlay update - the crash-prone path).
            mapScreen = new MapScreen(context, 2);
            context.game().setScreen(mapScreen);
            stepMap(GameAction.MENU);        // open the menu
            shotMap("l1-map-menu");
            stepMap(GameAction.DOWN);        // 保存
            stepMap(GameAction.DOWN);        // 读档
            stepMap(GameAction.CONFIRM);     // open the load view
            shotMap("l1-map-menu-load");
            stepMap(GameAction.CONFIRM);     // load slot 1 -> screen swap
            shotMap("l1-map-after-load");

            System.out.println("MENU CAPTURE OK outputDir=" + args[1]);
        } catch (Throwable error) {
            failure = error;
            error.printStackTrace();
        } finally {
            if (overlay != null) {
                overlay.dispose();
            }
            if (title != null) {
                title.dispose();
            }
            if (context != null && context.game() != null && context.game().getScreen() != null) {
                context.game().getScreen().dispose();
            }
            if (buffer != null) {
                buffer.dispose();
            }
            if (batch != null) {
                batch.dispose();
            }
            Gdx.app.exit();
        }
    }

    private void renderOverlay() {
        projection.update();
        batch.setProjectionMatrix(projection.combined);
        batch.begin();
        overlay.render(batch);
        batch.end();
    }

    /** A tap the title screen itself processes during its next render. */
    private void tapTitle(GameAction action) {
        InputManager input = context.inputManager();
        input.beginFrame();
        input.press(action);
        title.render(1f / 60f);
        input.endFrame();
        input.release(action);
    }

    /** One tap: press, let the target update, clear the edge, release. */
    private void tapAndRender(GameAction action, PauseMenuOverlay target) {
        InputManager input = context.inputManager();
        input.beginFrame();
        input.press(action);
        if (target != null) {
            target.update(1f / 60f);
        }
        input.endFrame();
        input.release(action);
    }

    private void shot(String name) {
        buffer.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (name.startsWith("l1-title") || name.startsWith("l14-title")) {
            title.render(1f / 60f);
            // The title screen applies its own FitViewport; the capture renders
            // several screens into the same framebuffer, so restore the
            // full-size viewport for the read (and any later draw).
            Gdx.gl.glViewport(0, 0, width, height);
        } else {
            renderOverlay();
        }
        writeShot(name);
    }

    /** One tap on the live MapScreen (press, render, clear edge, release). */
    private void stepMap(GameAction action) {
        InputManager input = context.inputManager();
        input.beginFrame();
        input.press(action);
        renderMap();
        input.endFrame();
        input.release(action);
    }

    private void renderMap() {
        Screen current = context.game().getScreen();
        if (current != null) {
            current.render(1f / 60f);
            Gdx.gl.glViewport(0, 0, width, height);
        }
    }

    private void shotMap(String name) {
        buffer.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        renderMap();
        Screen current = context.game().getScreen();
        System.out.println("map shot " + name + " screen="
                + (current == null ? "null" : current.getClass().getSimpleName())
                + " menuOpen=" + (current instanceof MapScreen && ((MapScreen) current).pauseMenuOpen()));
        writeShot(name);
    }

    private void writeShot(String name) {
        buffer.begin(); // re-bind: some screens switch the viewport/framebuffer
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, width, height);
        try {
            File output = new File(args[1], name + ".png");
            if (output.getParentFile() != null) {
                output.getParentFile().mkdirs();
            }
            PixmapIO.writePNG(Gdx.files.absolute(output.getAbsolutePath()), pixels, -1, true);
            System.out.println("wrote " + output.getAbsolutePath());
        } finally {
            pixels.dispose();
        }
        buffer.end();
    }
}
