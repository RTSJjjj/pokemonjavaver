package pokemon.runtime.app;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.input.KeyStateSource;
import pokemon.runtime.map.MapScreen;
import pokemon.runtime.ui.menu.TitleScreen;
import pokemon.runtime.save.SaveManager;
import pokemon.runtime.ui.ScreenManager;

/**
 * Runtime main entry (project3 section 8).
 *
 * <p>Start up flow: initialize the context, load the runtime config, load the
 * game database, load the assets and enter the initial map screen. Stage 2
 * fills these in phase by phase; this class only owns the order.</p>
 */
public final class PokemonGame extends Game {

    public static final String RUNTIME_VERSION = "1.0.0";
    /** Quick save slot of the R11 desktop entry (F6 / F9). */
    public static final String QUICK_SLOT = "quick";

    private final String dataRoot;
    private final int startMapOverride;
    private final KeyStateSource keySource;
    /** On-screen keys of a touch build (Android); null on the desktop. */
    private final pokemon.runtime.input.touch.TouchControls touch;
    private com.badlogic.gdx.graphics.g2d.SpriteBatch touchBatch;
    private RuntimeContext context;
    /** Window size to restore when fullscreen is left (R11). */
    private int windowedWidth = ScreenMetrics.LOGICAL_WIDTH;
    private int windowedHeight = ScreenMetrics.LOGICAL_HEIGHT;

    public PokemonGame() {
        this(null, -1, null);
    }

    /** @param dataRoot optional runtime data root; null uses POKEMON_RUNTIME_DATA or generated/. */
    public PokemonGame(String dataRoot) {
        this(dataRoot, -1, null);
    }

    /**
     * @param dataRoot        optional runtime data root
     * @param startMapOverride map id to open instead of the System start map
     *                        (debugging and demos; -1 keeps the project start)
     * @param keySource       desktop key state; null keeps the game headless
     */
    public PokemonGame(String dataRoot, int startMapOverride, KeyStateSource keySource) {
        this(dataRoot, startMapOverride, keySource, null);
    }

    /**
     * @param touch on-screen keys (a touch build); the caller wraps its key
     *              source with {@code touch.wrap(...)} so the keys reach the
     *              game as ordinary keys
     */
    public PokemonGame(String dataRoot, int startMapOverride, KeyStateSource keySource,
                       pokemon.runtime.input.touch.TouchControls touch) {
        this.touch = touch;
        this.dataRoot = dataRoot;
        this.startMapOverride = startMapOverride;
        this.keySource = keySource;
    }

    @Override
    public void create() {
        context = new RuntimeContext(RUNTIME_VERSION, this, dataRoot);
        if (keySource != null) {
            context.bindKeySource(keySource);
        }

        Gdx.app.setLogLevel(com.badlogic.gdx.Application.LOG_DEBUG);
        Gdx.app.log("PokemonGame", "runtime " + RUNTIME_VERSION + " starting");

        // TODO(R2): the runtime config is inline for now; loadRuntimeConfig
        // loads the game database and reports a clear error when the Builder
        // output is missing.
        context.loadRuntimeConfig();

        // L1: the options screen remembers fullscreen; reapply it at boot.
        if (context.settings().fullscreen() && Gdx.graphics != null
                && !Gdx.graphics.isFullscreen()) {
            try {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            } catch (RuntimeException error) {
                log("cannot restore fullscreen: " + error.getMessage());
            }
        }

        context.touchBuild(touch != null);
        if (touch != null && context.database() != null) {
            createTouchControls();
        }

        if (context.database() != null) {
            // R3/R4: the walking demo starts on the System start map unless an
            // explicit map was requested (e.g. the demo runs Map002). L1: the
            // title screen runs first - probes and tests pass a map id to skip
            // it, exactly like RMXP's playtest shortcut.
            int startMap = startMapOverride > 0 ? startMapOverride : context.database().startMapId();
            if (startMapOverride > 0) {
                setScreen(new MapScreen(context, startMap));
            } else {
                // P4: the icons and menu pictures are decoded once, behind a loading bar, and kept until the game closes.
                setScreen(new pokemon.runtime.ui.menu.PreloadScreen(context, () -> new TitleScreen(context)));
            }
        } else {
            // No Builder output yet: keep the window responsive instead of
            // pretending the game is running.
            setScreen(new ScreenManager(context));
        }
    }

    /** The key faces use the project's message font, sized from the screen height. */
    private void createTouchControls() {
        String name = context.database().project().runtime.messageFont;
        java.io.File file = name == null ? null
                : new pokemon.runtime.map.GraphicsLocator(context.database()).font(name);
        int height = Gdx.graphics.getHeight();
        touch.create(new pokemon.runtime.ui.menu.MenuFont(file, Math.round(height * 0.06f)),
                new pokemon.runtime.ui.menu.MenuFont(file, Math.round(height * 0.028f)));
        touchBatch = new com.badlogic.gdx.graphics.g2d.SpriteBatch();
    }

    /** Logger wrapper so the context can log without holding a Gdx reference. */
    public void log(String message) {
        if (Gdx.app != null) {
            Gdx.app.log("PokemonGame", message);
        } else {
            System.out.println("[PokemonGame] " + message);
        }
    }

    @Override
    public void render() {
        // The input manager is polled once per frame so that every system sees
        // the same frame snapshot (project3 sections 13, 14).
        if (touch != null) {
            touch.update();
        }
        context.inputManager().beginFrame();
        if (getScreen() instanceof MapScreen) {
            context.gameState().trainer().playSeconds += Math.max(0, Math.min(1, Gdx.graphics.getDeltaTime()));
        }
        context.audioManager().update(Gdx.graphics.getDeltaTime()); // R6.24: BGM cue
        handleSystemKeys();
        // Game#render, with the frame time scaled by the speed-up (346_Speed_Up).
        if (getScreen() != null) {
            getScreen().render(GameSpeed.scale(Gdx.graphics.getDeltaTime()));
        }
        if (touch != null && touchBatch != null) {
            touch.render(touchBatch);
        } else {
            renderSpeedLabel();
        }
        context.inputManager().endFrame();
    }

    /**
     * R11 desktop shortcuts, handled outside the game input map because they are
     * window/system features: F11 toggles fullscreen (FitViewport keeps the
     * logical resolution), F5 quick-saves and F9 loads the quick slot.
     */
    private void handleSystemKeys() {
        if (Gdx.input == null) {
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            toggleFullscreen();
        }
        handleSpeedKeys();
        if ((context.battlePort() != null && context.battlePort().pending()) || context.menuService().pending() != null) return;
        // F5 is Input::F5 (the Ready Menu on the map, the party screen's 寄存系统, the battle info): the desktop quick save is on F6.
        boolean pauseMenuOpen = getScreen() instanceof pokemon.runtime.map.MapScreen
                && ((pokemon.runtime.map.MapScreen) getScreen()).pauseMenuOpen();
        if (!pauseMenuOpen && Gdx.input.isKeyJustPressed(Input.Keys.F6)) {
            saveQuick();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) {
            loadQuick();
        }
    }

    /**
     * 346_Speed_Up:16-34 (Windows only, like the plugin): Alt = next speed,
     * Ctrl+Alt = back to 1x. The touch build has its own speed key.
     */
    private void handleSpeedKeys() {
        if (touch != null || Gdx.app.getType() != com.badlogic.gdx.Application.ApplicationType.Desktop) {
            return;
        }
        boolean alt = Gdx.input.isKeyJustPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.ALT_RIGHT);
        if (!alt) {
            return;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) {
            GameSpeed.reset();
        } else {
            GameSpeed.cycle();
        }
    }

    /** 346_Speed_Up:30-37: "N×" at the top right of the game picture (small font, white on black, opacity 224). */
    private void renderSpeedLabel() {
        String label = GameSpeed.label();
        if (label == null || context.database() == null) {
            return;
        }
        int width = Gdx.graphics.getWidth(), height = Gdx.graphics.getHeight();
        float scale = Math.min(width / (float) ScreenMetrics.logicalWidth(), height / (float) ScreenMetrics.logicalHeight());
        int size = Math.max(8, Math.round(25f * scale));          // pbSetSmallFont
        if (speedFont == null || speedFontSize != size) {
            String name = context.database().project().runtime.messageFont;
            java.io.File file = name == null ? null
                    : new pokemon.runtime.map.GraphicsLocator(context.database()).font(name);
            if (speedFont != null) {
                speedFont.dispose();
            }
            speedFont = new pokemon.runtime.ui.menu.MenuFont(file, size);
            speedFontSize = size;
        }
        if (speedBatch == null) {
            speedBatch = new com.badlogic.gdx.graphics.g2d.SpriteBatch();
        }
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        com.badlogic.gdx.graphics.OrthographicCamera camera = new com.badlogic.gdx.graphics.OrthographicCamera();
        camera.setToOrtho(false, width, height);
        speedBatch.setProjectionMatrix(camera.combined);
        float right = (width + ScreenMetrics.logicalWidth() * scale) / 2f;       // right edge of the game picture
        speedBatch.begin();
        speedFont.drawRight(speedBatch, label, right, height, new com.badlogic.gdx.graphics.Color(1f, 1f, 1f, 224f / 255f),
                new com.badlogic.gdx.graphics.Color(0f, 0f, 0f, 224f / 255f));
        speedBatch.end();
    }

    private pokemon.runtime.ui.menu.MenuFont speedFont;
    private int speedFontSize;
    private com.badlogic.gdx.graphics.g2d.SpriteBatch speedBatch;

    private void toggleFullscreen() {
        if (Gdx.graphics == null) {
            return;
        }
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode(windowedWidth, windowedHeight);
            log("windowed " + windowedWidth + "x" + windowedHeight);
        } else {
            windowedWidth = Gdx.graphics.getWidth();
            windowedHeight = Gdx.graphics.getHeight();
            Graphics.DisplayMode mode = Gdx.graphics.getDisplayMode();
            Gdx.graphics.setFullscreenMode(mode);
            log("fullscreen " + mode.width + "x" + mode.height);
        }
    }

    private void saveQuick() {
        try {
            context.saveManager().save(context.storage(), QUICK_SLOT, context.gameState());
            log("saved to slot \"" + QUICK_SLOT + "\"");
        } catch (RuntimeException error) {
            log("save failed: " + error.getMessage());
        }
    }

    private void loadQuick() {
        try {
            if (!context.saveManager().load(context.storage(), QUICK_SLOT, context.gameState())) {
                log("no save in slot \"" + QUICK_SLOT + "\"");
                return;
            }
            log("loaded slot \"" + QUICK_SLOT + "\"");
            // The save may be on another map / tile: rebuild the map screen.
            setScreen(new MapScreen(context, context.gameState().currentMapId(),
                    context.gameState().playerX(), context.gameState().playerY(),
                    context.gameState().playerDirection()));
        } catch (RuntimeException error) {
            log("load failed: " + error.getMessage());
        }
    }

    @Override
    public void dispose() {
        GameDatabase database = context.database();
        if (database != null) {
            database.dispose();
        }
        SaveManager saves = context.saveManager();
        if (saves != null) {
            saves.dispose();
        }
        if (getScreen() != null) {
            getScreen().dispose();
        }
        context.disposeShared();
        if (touch != null) {
            touch.dispose();
        }
        if (touchBatch != null) {
            touchBatch.dispose();
        }
        if (speedBatch != null) {
            speedBatch.dispose();
        }
        if (speedFont != null) {
            speedFont.dispose();
        }
        super.dispose();
    }
}
