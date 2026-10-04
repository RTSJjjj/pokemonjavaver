package pokemon.runtime.ui.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.data.TitleData;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.map.GraphicsLocator;
import pokemon.runtime.map.MapScreen;
import pokemon.runtime.state.GameState;
import pokemon.runtime.ui.WindowSkin;

import java.io.File;
import java.util.Locale;

/**
 * L1/L14: the title screen. L14 mirrors the project's Modular Title Screen
 * visuals 1:1 with the plugin's own recipe ({@code title.json.modifiers}):
 * background ({@code background:bw}), scrolling overlay ({@code overlay2} =
 * scrolling002), FX6 shine ({@code effect6_y312}), the two-part logo with the
 * shine sweep, the blinking PRESS ENTER prompt, the footer bar (version name +
 * copyright) and the rotating splash message. The original flow is kept:
 * splash slides (fade in / hold / fade out) -> white intro fade -> title;
 * confirm opens the load/command screen (继续之前的故事 / 新的故事 / 设置 /
 * 退出游戏). A start map override skips the title entirely (PokemonGame).
 */
public final class TitleScreen extends ScreenAdapter {

    private enum State {
        SPLASH,
        TITLE,
        COMMANDS,
        LOAD,
        OPTIONS
    }

    private static final String[] COMMANDS = { "继续之前的故事", "新的故事", "设置", "退出游戏" };
    private static final float COMMAND_ROW = 44f;
    private static final Color FOOTER_MAIN = new Color(248f / 255f, 248f / 255f, 248f / 255f, 1f);
    private static final Color FOOTER_SHADOW = new Color(64f / 255f, 64f / 255f, 64f / 255f, 1f);
    private static final Color MESSAGE_MAIN = new Color(1f, 216f / 255f, 0f, 1f);
    private static final Color MESSAGE_SHADOW = new Color(216f / 255f, 128f / 255f, 0f, 1f);
    private static final Color DISABLED_MAIN = new Color(0.55f, 0.55f, 0.55f, 1f);

    private final RuntimeContext context;
    private final GameDatabase database;
    private final TitleData data;
    private final MenuAssets assets;
    private final MenuFont font;
    private final MenuFont smallFont;
    private final WindowSkin skin;
    private final SplashSequence splash;
    private final TitleAnimations anim = new TitleAnimations();
    private final SaveLoadView loadView = new SaveLoadView(false);
    private final OptionsView optionsView;
    private final SpriteBatch batch = new SpriteBatch();
    private final FitViewport viewport =
            new FitViewport(ScreenMetrics.logicalWidth(), ScreenMetrics.logicalHeight());
    private final Matrix4 savedTransform = new Matrix4();
    private final Matrix4 messageTransform = new Matrix4();

    // L14 visual recipe (parsed from title.json.modifiers)
    private String bgFile = "background";
    private String overlayFile;
    private String fxFile;
    private float fxX;
    private float fxY;
    private float logoX;
    private float logoY;
    private boolean logoShine;

    private State state = State.SPLASH;
    private int command;
    private boolean hasSaves;
    private String message;
    /** Screen requested by new game / continue; applied after batch.end(). */
    private Screen pendingSwitch;

    public TitleScreen(RuntimeContext context) {
        this.context = context;
        this.database = context.database();
        GraphicsLocator locator = database == null ? null : new GraphicsLocator(database);
        this.assets = new MenuAssets(locator);
        this.font = new MenuFont(messageFontFile(locator));
        this.smallFont = new MenuFont(messageFontFile(locator), 18);
        this.skin = assets.skin(database == null || database.system() == null
                ? "" : database.system().windowskinName);
        this.data = database == null ? TitleData.empty() : database.title();
        this.splash = new SplashSequence(data.splashImages,
                data.secondsPerSplash + 2f * data.fadeSeconds());
        this.optionsView = new OptionsView(context);
        parseRecipe();
        Array<String> messages = data.splashMessages;
        this.message = messages.size == 0 ? null : messages.random();
    }

    private File messageFontFile(GraphicsLocator locator) {
        String name = null;
        if (database != null && database.project() != null && database.project().runtime != null
                && database.project().runtime.hasMessageFont()) {
            name = database.project().runtime.messageFont;
        }
        return name == null || locator == null ? null : locator.font(name);
    }

    /** Parses the plugin's MODIFIERS tokens into the L14 recipe. */
    private void parseRecipe() {
        float width = ScreenMetrics.logicalWidth();
        float height = ScreenMetrics.logicalHeight();
        logoX = width / 2f - 10f;
        logoY = height / 2f + 42f;
        fxX = width / 2f;
        fxY = height / 2f;
        for (String raw : data.modifiers) {
            String token = raw == null ? "" : raw.trim();
            String lower = token.toLowerCase(Locale.ROOT);
            if (lower.startsWith("background:")) {
                bgFile = token.substring("background:".length());
            } else if (lower.equals("overlay2")) {
                overlayFile = "scrolling002";
            } else if (lower.startsWith("overlay:")) {
                overlayFile = token.substring("overlay:".length());
            } else if (lower.startsWith("logox:")) {
                logoX = number(token.substring("logox:".length()), logoX);
            } else if (lower.startsWith("logoy:")) {
                logoY = number(token.substring("logoy:".length()), logoY);
            } else if (lower.startsWith("logo:")) {
                logoShine |= lower.contains("shine");
            } else if (lower.startsWith("effect")) {
                parseEffect(lower);
            } else if (lower.startsWith("misc")) {
                // MX4 is a PokemonSprite gated by ModularTitle::SPECIES (nil in
                // this project): nothing to draw before stage 3.
            }
        }
    }

    private void parseEffect(String token) {
        String body = token.substring("effect".length());
        int index = 0;
        int at = 0;
        while (at < body.length() && Character.isDigit(body.charAt(at))) {
            index = index * 10 + (body.charAt(at) - '0');
            at++;
        }
        if (index != 6) {
            return; // only FX6 (shine002) is used by this project
        }
        fxFile = "shine002";
        for (String part : body.substring(at).split("_")) {
            if (part.startsWith("x")) {
                fxX = number(part.substring(1), fxX);
            } else if (part.startsWith("y")) {
                fxY = number(part.substring(1), fxY);
            }
        }
    }

    private static float number(String text, float fallback) {
        try {
            return Float.parseFloat(text.trim());
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    @Override
    public void show() {
        // Game#setScreen calls resize right after show; doing it here too keeps
        // the viewport correct when the screen is rendered outside a Game loop
        // (capture tools) and when the window size never changes.
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if (data.bgm != null && !data.bgm.isEmpty()) {
            context.audioManager().playBgm(data.bgm, data.bgmVolume, 100);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        context.sampleInput();
        InputManager input = context.inputManager();
        float width = ScreenMetrics.logicalWidth();
        float height = ScreenMetrics.logicalHeight();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        if (state == State.SPLASH) {
            renderSplash(delta, input, width, height);
        } else {
            renderTitle(delta, input, width, height);
        }
        batch.end();
        // New game / continue run inside the batch; swap screens only after it
        // is closed, otherwise the old screen would dispose its batch mid-frame.
        if (pendingSwitch != null) {
            Screen next = pendingSwitch;
            pendingSwitch = null;
            context.game().setScreen(next);
            dispose();
        }
    }

    // ------------------------------------------------------------------
    // Splash slides (fade in / hold / fade out, skippable like the original)
    // ------------------------------------------------------------------

    private void renderSplash(float delta, InputManager input, float width, float height) {
        splash.update(delta);
        if (input.wasPressed(GameAction.CONFIRM) || input.wasPressed(GameAction.CANCEL)
                || input.wasPressed(GameAction.MENU)) {
            splash.skip();
        }
        String image = splash.currentImage();
        if (image != null) {
            Texture texture = assets.title(image);
            if (texture != null) {
                float alpha = slideAlpha();
                batch.setColor(1f, 1f, 1f, alpha);
                batch.draw(texture, 0f, 0f, width, height);
                batch.setColor(1f, 1f, 1f, 1f);
            }
        }
        if (splash.finished()) {
            state = State.TITLE;
        }
    }

    /** The plugin fades each slide in and out over `fadeTicks/20` seconds. */
    private float slideAlpha() {
        float fade = data.fadeSeconds();
        float hold = data.secondsPerSplash;
        if (fade <= 0f || hold <= 0f) {
            return 1f;
        }
        float elapsed = splash.elapsed();
        if (elapsed < fade) {
            return Math.min(1f, elapsed / fade);
        }
        float fadeStart = fade + hold + fade;
        if (elapsed > fadeStart - fade) {
            return Math.max(0f, (fadeStart - elapsed) / fade);
        }
        return 1f;
    }

    // ------------------------------------------------------------------
    // Title scene
    // ------------------------------------------------------------------

    private void renderTitle(float delta, InputManager input, float width, float height) {
        anim.update(delta);
        renderScene(width, height);
        switch (state) {
            case TITLE:
                updateTitleInput(input);
                break;
            case COMMANDS:
                renderCommands(input, width, height);
                break;
            case LOAD:
                renderLoad(input, width, height);
                break;
            case OPTIONS:
                renderOptions(input, width, height);
                break;
            default:
                break;
        }
    }

    private void updateTitleInput(InputManager input) {
        if (input.wasPressed(GameAction.CONFIRM) || input.wasPressed(GameAction.CANCEL)
                || input.wasPressed(GameAction.MENU)) {
            if (!anim.introFinished()) {
                anim.skipIntro();
                return;
            }
            MenuSe.decision(context.audioManager());
            refreshSaves();
            state = State.COMMANDS;
        }
    }

    private void renderScene(float width, float height) {
        Texture background = assets.modular("Backgrounds", bgFile);
        if (background != null) {
            batch.draw(background, 0f, 0f, width, height);
        }
        if (fxFile != null) {
            Texture fx = assets.modular("Particles", fxFile);
            if (fx != null) {
                // RGSS y is top-down; libGDX draws bottom-up.
                float centerY = height - fxY;
                float zoom = anim.fxZoom();
                float w = fx.getWidth() * zoom;
                float h = fx.getHeight() * zoom;
                batch.draw(fx, fxX - w / 2f, centerY - h / 2f, w, h);
            }
        }
        if (overlayFile != null && !overlayFile.isEmpty()) {
            Texture overlay = assets.modular("Overlays", overlayFile);
            if (overlay != null) {
                float offset = anim.scrollOffset() % overlay.getWidth();
                batch.draw(overlay, -offset, 0f);
                batch.draw(overlay, overlay.getWidth() - offset, 0f);
            }
        }
        Texture logo1 = assets.modular("", "logo1");
        Texture logo2 = assets.modular("", "logo2");
        float logoBottomY = height - logoY; // RGSS bottom anchor -> libGDX y
        float logoLeft = logoX - (logo1 != null ? logo1.getWidth() / 2f
                : logo2 != null ? logo2.getWidth() / 2f : 0f);
        if (logo1 != null) {
            batch.draw(logo1, logoLeft, logoBottomY);
        }
        if (logo2 != null) {
            batch.draw(logo2, logoLeft, logoBottomY - logo2.getHeight());
        }
        if (logoShine && logo1 != null) {
            Texture mask = assets.modular("", "logo3");
            if (mask != null) {
                float shine = anim.shineX();
                if (shine >= 0f && shine + 16f <= mask.getWidth()) {
                    int sx = Math.round(shine);
                    batch.draw(mask, logoLeft + sx, logoBottomY, sx, 0, 16, mask.getHeight());
                }
            }
        }
        if (anim.introFinished()) {
            Texture start = assets.modular("", "start");
            if (start == null) {
                start = assets.title("start"); // projects without the plugin
            }
            if (start != null) {
                float alpha = Math.max(0f, Math.min(1f, anim.startAlpha() / 255f));
                batch.setColor(1f, 1f, 1f, alpha);
                batch.draw(start, width / 2f - start.getWidth() / 2f,
                        height * 0.15f - start.getHeight() / 2f);
                batch.setColor(1f, 1f, 1f, 1f);
            }
        }
        if (!data.footerLeft.isEmpty()) {
            smallFont.draw(batch, data.footerLeft, 2f, 8f, FOOTER_MAIN, FOOTER_SHADOW);
        }
        if (!data.footerRight.isEmpty()) {
            smallFont.drawRight(batch, data.footerRight, width - 2f, 8f, FOOTER_MAIN, FOOTER_SHADOW);
        }
        renderSplashMessage(width, height);
        if (!anim.introFinished()) {
            batch.setColor(1f, 1f, 1f, anim.introAlpha() / 255f);
            batch.draw(assets.pixel(), 0f, 0f, width, height);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    /** The rotating yellow quote (240x22 bitmap, angle 15, pulsing zoom). */
    private void renderSplashMessage(float width, float height) {
        if (message == null || !smallFont.ready()) {
            return;
        }
        float mx = width * 4f / 5f - 10f;
        float my = height / 2f + 44f; // RGSS height/2-44 -> libGDX
        smallFont.font().getData().setScale(anim.messageZoom());
        savedTransform.set(batch.getTransformMatrix());
        messageTransform.set(savedTransform).idt()
                .translate(mx, my, 0f)
                .rotate(0f, 0f, 1f, -15f);
        batch.setTransformMatrix(messageTransform);
        smallFont.drawCentered(batch, message, 0f, -7f, MESSAGE_MAIN, MESSAGE_SHADOW);
        batch.setTransformMatrix(savedTransform);
        smallFont.font().getData().setScale(1f);
    }

    private int messageDebug;

    private void renderCommands(InputManager input, float width, float height) {
        if (input.wasPressed(GameAction.UP)) {
            command = Math.floorMod(command - 1, COMMANDS.length);
            MenuSe.cursor(context.audioManager());
        }
        if (input.wasPressed(GameAction.DOWN)) {
            command = Math.floorMod(command + 1, COMMANDS.length);
            MenuSe.cursor(context.audioManager());
        }
        if (input.wasPressed(GameAction.CONFIRM)) {
            if (command == 0 && !hasSaves) {
                MenuSe.buzzer(context.audioManager());
            } else if (command == 0) {
                MenuSe.decision(context.audioManager());
                loadView.refresh(context.storage(), database);
                state = State.LOAD;
            } else if (command == 1) {
                MenuSe.decision(context.audioManager());
                startNewGame();
                return;
            } else if (command == 2) {
                MenuSe.decision(context.audioManager());
                state = State.OPTIONS;
            } else {
                MenuSe.decision(context.audioManager());
                Gdx.app.exit();
            }
        }
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            MenuSe.buzzer(context.audioManager());
            state = State.TITLE;
        }

        float windowWidth = 320f;
        float windowHeight = COMMANDS.length * COMMAND_ROW + 48f;
        float x = (width - windowWidth) / 2f;
        float y = 56f;
        if (skin != null) {
            skin.draw(batch, x, y, windowWidth, windowHeight);
        }
        for (int i = 0; i < COMMANDS.length; i++) {
            float rowY = y + windowHeight - 40f - i * COMMAND_ROW;
            boolean current = i == command;
            boolean disabled = i == 0 && !hasSaves;
            font.draw(batch, current ? "▶" : "  ", x + 24f, rowY);
            if (disabled) {
                font.draw(batch, COMMANDS[i], x + 64f, rowY, DISABLED_MAIN, FOOTER_SHADOW);
            } else {
                font.draw(batch, COMMANDS[i], x + 64f, rowY);
            }
        }
    }

    private void renderLoad(InputManager input, float width, float height) {
        SaveLoadView.Result result = loadView.update(input, context.audioManager());
        loadView.render(batch, assets, font, skin);
        if (result == SaveLoadView.Result.PICKED) {
            loadSelected();
        } else if (result == SaveLoadView.Result.CANCELLED) {
            state = State.COMMANDS;
        }
    }

    private void renderOptions(InputManager input, float width, float height) {
        OptionsView.Result result = optionsView.update(input, context.audioManager());
        optionsView.render(batch, assets, font, skin);
        if (result == OptionsView.Result.BACK) {
            state = State.COMMANDS;
        }
    }

    private void refreshSaves() {
        hasSaves = false;
        for (SaveSlots.Slot slot : SaveSlots.list(context.storage(), database)) {
            if (slot.exists) {
                hasSaves = true;
                break;
            }
        }
        if (!hasSaves) {
            command = 1; // 新的故事 is the only usable entry without saves
        }
    }

    private void startNewGame() {
        context.gameState().reset();
        context.audioManager().stopBgm();
        switchTo(new MapScreen(context, database.startMapId()));
    }

    private void loadSelected() {
        SaveSlots.Slot slot = loadView.selected();
        if (slot == null) {
            return;
        }
        GameState stateGame = context.gameState();
        try {
            if (!context.saveManager().load(context.storage(), slot.id, stateGame)) {
                MenuSe.buzzer(context.audioManager());
                state = State.COMMANDS;
                return;
            }
        } catch (RuntimeException error) {
            Gdx.app.error("TitleScreen", "load failed: " + error.getMessage());
            MenuSe.buzzer(context.audioManager());
            state = State.COMMANDS;
            return;
        }
        context.audioManager().stopBgm();
        switchTo(new MapScreen(context, stateGame.currentMapId(), stateGame.playerX(),
                stateGame.playerY(), stateGame.playerDirection()));
    }

    private void switchTo(Screen screen) {
        pendingSwitch = screen;
    }

    @Override
    public void dispose() {
        batch.dispose();
        assets.dispose();
        font.dispose();
        smallFont.dispose();
    }
}
