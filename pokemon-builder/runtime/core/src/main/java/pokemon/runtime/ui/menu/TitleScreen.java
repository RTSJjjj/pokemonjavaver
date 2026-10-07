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
import pokemon.runtime.data.ProjectInfo;
import pokemon.runtime.data.TitleData;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.map.GraphicsLocator;
import pokemon.runtime.map.MapScreen;
import pokemon.runtime.pokemon.PbsData;
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

    private static final String[] COMMANDS = { "继续之前的故事", "新的冒险", "选项", "退出游戏" };
    private static final float COMMAND_ROW = 44f;
    // PScreen_Load TEXTCOLOR / TEXTSHADOWCOLOR (the continue panel labels).
    private static final Color FOOTER_MAIN = new Color(232f / 255f, 232f / 255f, 232f / 255f, 1f);
    private static final Color FOOTER_SHADOW = new Color(136f / 255f, 136f / 255f, 136f / 255f, 1f);
    // PScreen_Load MALETEXTCOLOR / FEMALETEXTCOLOR (the trainer name).
    private static final Color MALE_MAIN = new Color(56f / 255f, 160f / 255f, 248f / 255f, 1f);
    private static final Color MALE_SHADOW = new Color(56f / 255f, 104f / 255f, 168f / 255f, 1f);
    private static final Color FEMALE_MAIN = new Color(240f / 255f, 72f / 255f, 88f / 255f, 1f);
    private static final Color FEMALE_SHADOW = new Color(160f / 255f, 64f / 255f, 64f / 255f, 1f);
    // 004_MSF_UI_Load's 20px tips bitmap: (248,248,248,128) / (88,88,88,128).
    private static final Color TIPS_MAIN = new Color(248f / 255f, 248f / 255f, 248f / 255f, 128f / 255f);
    private static final Color TIPS_SHADOW = new Color(88f / 255f, 88f / 255f, 88f / 255f, 128f / 255f);
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
    /** The four save slots and the one the continue panel is showing. */
    private Array<SaveSlots.Slot> slots = new Array<>();
    private int slotIndex;
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
        // Each logo part is centred on logoX by its own width (the plugin sets
        // ox = bitmap.width/2 on both); logo1 is the upper part (bottom at
        // logoY), logo2 the lower part (top at logoY), so they meet at logoY.
        float logo1Left = logoX - (logo1 != null ? logo1.getWidth() / 2f : 0f);
        float logo2Left = logoX - (logo2 != null ? logo2.getWidth() / 2f : 0f);
        if (logo1 != null) {
            batch.draw(logo1, logo1Left, logoBottomY);
        }
        if (logo2 != null) {
            batch.draw(logo2, logo2Left, logoBottomY - logo2.getHeight());
        }
        if (logoShine && logo1 != null) {
            Texture mask = assets.modular("", "logo3");
            if (mask != null) {
                float shine = anim.shineX();
                if (shine >= 0f && shine + 16f <= mask.getWidth()) {
                    int sx = Math.round(shine);
                    batch.draw(mask, logo1Left + sx, logoBottomY, sx, 0, 16, mask.getHeight());
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
            smallFont.draw(batch, data.footerLeft, 2f, smallFont.lineHeight() + 4f, FOOTER_MAIN, FOOTER_SHADOW);
        }
        if (!data.footerRight.isEmpty()) {
            smallFont.drawRight(batch, data.footerRight, width - 2f, smallFont.lineHeight() + 4f,
                    FOOTER_MAIN, FOOTER_SHADOW);
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
        boolean continueSelected = hasSaves && command == 0;
        if (continueSelected && slots.size > 1) {
            if (input.wasPressed(GameAction.LEFT)) {
                slotIndex = Math.floorMod(slotIndex - 1, slots.size);
                MenuSe.cursor(context.audioManager());
            }
            if (input.wasPressed(GameAction.RIGHT)) {
                slotIndex = Math.floorMod(slotIndex + 1, slots.size);
                MenuSe.cursor(context.audioManager());
            }
        }
        if (input.wasPressed(GameAction.UP)) {
            command = Math.floorMod(command - 1, COMMANDS.length);
            MenuSe.cursor(context.audioManager());
        }
        if (input.wasPressed(GameAction.DOWN)) {
            command = Math.floorMod(command + 1, COMMANDS.length);
            MenuSe.cursor(context.audioManager());
        }
        if (input.wasPressed(GameAction.CONFIRM)) {
            if (command == 0) {
                SaveSlots.Slot slot = currentSlot();
                if (slot == null || !slot.exists) {
                    MenuSe.buzzer(context.audioManager());
                } else {
                    MenuSe.decision(context.audioManager());
                    loadSlot(slot);
                    return;
                }
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

        // Visuals of the project's 004_MSF_UI_Load: the loadbg background plus
        // the loadPanels sheet (continue slot 408x222, normal rows 408x46, each
        // with a selected variant at +222 / +46). Panel x=144, y from 32,
        // stepping 224 (continue) / 48 (normal).
        Texture loadbg = assets.graphic("Pictures", "loadbg");
        if (loadbg != null) {
            batch.draw(loadbg, 0f, 0f, width, height);
        }
        Texture panels = assets.graphic("Pictures", "loadPanels");
        SaveSlots.Slot slot = currentSlot();
        float panelX = 144f;
        float panelTop = 32f;
        for (int i = 0; i < COMMANDS.length; i++) {
            boolean isContinue = i == 0 && hasSaves && slot != null;
            float panelH = isContinue ? 222f : 46f;
            float y = height - panelTop - panelH;
            if (panels != null) {
                int srcY = isContinue ? (i == command ? 222 : 0) : (i == command ? 490 : 444);
                batch.draw(panels, panelX, y, 0f, 0f, 408f, panelH, 1f, 1f, 0f,
                        0, srcY, 408, (int) panelH, false, false);
            }
            float top = height - panelTop;
            if (isContinue) {
                drawContinuePanel(slot, panelX, top);
            } else {
                // 004_MSF_UI_Load: normal rows draw the title at (16*2, line_y[0]*2).
                boolean disabled = i == 0 && !hasSaves;
                if (disabled) {
                    font.draw(batch, COMMANDS[i], panelX + 32f, top - 10f, DISABLED_MAIN, FOOTER_SHADOW);
                } else {
                    font.draw(batch, COMMANDS[i], panelX + 32f, top - 10f, FOOTER_MAIN, FOOTER_SHADOW);
                }
            }
            panelTop += isContinue ? 224f : 48f;
        }
        if (hasSaves && slot != null) {
            drawPartySprites(slot, height);
        }
        // The tips bitmap is a 20px strip at the top, shown only while the
        // continue row is selected (004_MSF_UI_Load: pbChoose).
        if (continueSelected && slot != null && slot.exists) {
            smallFont.draw(batch, "[←]/[→]:切换存档插槽", 0f, height - 2f, TIPS_MAIN, TIPS_SHADOW);
        }
    }

    /**
     * The continue panel is a bitmap drawn on by its own sprite, so the textpos
     * are panel relative: line_y = [10, 64, 96, 128] (top origin), labels at
     * x=28 / 268 and their values right aligned at 248 / 388, the name at
     * (112, 56) and the map name right aligned at 388/10 (004_MSF_UI_Load).
     */
    private void drawContinuePanel(SaveSlots.Slot slot, float panelX, float top) {
        font.draw(batch, continueTitle(slot), panelX + 28f, top - 10f, FOOTER_MAIN, FOOTER_SHADOW);
        String name = slot.playerName == null || slot.playerName.isEmpty() ? "训练家" : slot.playerName;
        Color nameMain = FOOTER_MAIN;
        Color nameShadow = FOOTER_SHADOW;
        if (slot.gender == 1) {
            nameMain = FEMALE_MAIN;
            nameShadow = FEMALE_SHADOW;
        } else if (slot.gender == 0) {
            nameMain = MALE_MAIN;
            nameShadow = MALE_SHADOW;
        }
        font.draw(batch, name, panelX + 112f, top - 56f, nameMain, nameShadow);
        if (slot.mapName != null && !slot.mapName.isEmpty()) {
            font.drawRight(batch, slot.mapName, panelX + 388f, top - 10f, FOOTER_MAIN, FOOTER_SHADOW);
        }
        font.draw(batch, "徽章：", panelX + 268f, top - 64f, FOOTER_MAIN, FOOTER_SHADOW);
        font.drawRight(batch, String.valueOf(slot.badges), panelX + 388f, top - 64f, FOOTER_MAIN, FOOTER_SHADOW);
        font.draw(batch, "图鉴：", panelX + 268f, top - 96f, FOOTER_MAIN, FOOTER_SHADOW);
        font.drawRight(batch, String.valueOf(slot.seen), panelX + 388f, top - 96f, FOOTER_MAIN, FOOTER_SHADOW);
        font.draw(batch, "时长：", panelX + 28f, top - 96f, FOOTER_MAIN, FOOTER_SHADOW);
        font.drawRight(batch, duration(slot.playSeconds), panelX + 248f, top - 96f, FOOTER_MAIN, FOOTER_SHADOW);
        font.draw(batch, "保存时间：", panelX + 28f, top - 128f, FOOTER_MAIN, FOOTER_SHADOW);
        font.drawRight(batch, relativeTime(slot.savedAtMillis), panelX + 248f, top - 128f,
                FOOTER_MAIN, FOOTER_SHADOW);
    }

    /** The continue row's title: "←自动保存→" / "←存档N→" (004_MSF_UI_Load). */
    private static String continueTitle(SaveSlots.Slot slot) {
        if (slot == null || !slot.exists) {
            return "空档";
        }
        if ("quick".equals(slot.id)) {
            return "←自动保存→";
        }
        return "←存档" + slot.id + "→";
    }

    private SaveSlots.Slot currentSlot() {
        return slots.size == 0 ? null : slots.get(slotIndex);
    }

    /**
     * 004_MSF_UI_Load's {@code pbSetParty}: the trainer's walking charset (first
     * frame) and the party's Pokemon icons, all in absolute screen coordinates.
     */
    private void drawPartySprites(SaveSlots.Slot slot, float height) {
        if (slot == null || !slot.exists) {
            return;
        }
        // pbSetParty: x = 56*2 - cw/8 + 32 + 64, y = 32*2 - ch/8, src_rect the
        // first frame (cw/4 x ch/4).
        String charset = playerCharset(slot.playerId);
        Texture sheet = charset == null ? null : assets.character(charset);
        if (sheet != null && sheet.getWidth() >= 4 && sheet.getHeight() >= 4) {
            int frameW = sheet.getWidth() / 4;
            int frameH = sheet.getHeight() / 4;
            batch.draw(sheet, 208f - frameW / 2f, height - 64f - frameH / 2f, frameW, frameH,
                    0, 0, frameW, frameH, false, false);
        }
        // PokemonIconSprite at x = (46+32*i)*2 + 32 + 64, y = 110*2, origin
        // Center (ox = w/2, oy = h*5/8), first frame only.
        for (int i = 0; i < slot.party.size; i++) {
            Texture icon = partyIcon(slot.party.get(i));
            if (icon == null || icon.getHeight() <= 0) {
                continue;
            }
            int size = icon.getHeight();
            float originX = 188f + 64f * i;
            batch.draw(icon, originX - size / 2f, height - 220f + size * 5f / 8f - size, size, size,
                    0, 0, size, size, false, false);
        }
    }

    /** The trainer's charset name from the project's metaID table (PlayerA..H). */
    private String playerCharset(int playerId) {
        ProjectInfo.RuntimeProfile profile =
                database == null || database.project() == null ? null : database.project().runtime;
        if (profile == null) {
            return null;
        }
        if (playerId >= 0) {
            ProjectInfo.PlayerGraphic graphic = profile.player(playerId);
            if (graphic != null && graphic.charset != null && !graphic.charset.isEmpty()) {
                return graphic.charset;
            }
        }
        return profile.playerCharset == null || profile.playerCharset.isEmpty() ? null : profile.playerCharset;
    }

    /**
     * Graphics/Icons/icon&lt;species&gt;&lt;f?&gt;&lt;s?&gt; (&lt;egg&gt;): the
     * project's numeric id first, then the internal name (pbCheckPokemonIconFiles).
     */
    private Texture partyIcon(SaveSlots.Slot.PartyEntry entry) {
        if (entry == null || entry.species == null || entry.species.isEmpty()) {
            return null;
        }
        String suffix = (entry.gender == 1 ? "f" : "") + (entry.shiny ? "s" : "");
        String extra = entry.egg ? "egg" : "";
        Texture icon = null;
        PbsData data = context.pbsData();
        PbsData.Species species = data == null ? null : data.species(entry.species);
        if (species != null) {
            icon = assets.icon(String.format("icon%03d%s%s", species.id, suffix, extra));
        }
        if (icon == null) {
            icon = assets.icon("icon" + entry.species + suffix + extra);
        }
        if (icon == null && entry.egg) {
            icon = assets.icon("iconEgg");
        }
        return icon;
    }

    /** "N小时M分钟" / "M分钟" (PScreen_Load's play-time line). */
    private static String duration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds / 60) % 60;
        return hours > 0 ? hours + "小时" + minutes + "分钟" : minutes + "分钟";
    }

    /** "刚刚 / N分钟前 / N小时前 / N天前 / N个月前 / N年前". */
    private static String relativeTime(long millis) {
        if (millis <= 0) {
            return "未知";
        }
        long delta = Math.max(0, System.currentTimeMillis() - millis) / 1000L;
        if (delta < 60) {
            return "刚刚";
        }
        if (delta < 3600) {
            return (delta / 60) + "分钟前";
        }
        if (delta < 86400) {
            return (delta / 3600) + "小时前";
        }
        if (delta < 86400L * 30) {
            return (delta / 86400) + "天前";
        }
        if (delta < 86400L * 365) {
            return (delta / (86400L * 30)) + "个月前";
        }
        if (delta < 86400L * 365 * 2) {
            return (delta / (86400L * 365)) + "年前";
        }
        return "很久之前";
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
        optionsView.render(batch, assets, font, skin, skin);
        if (result == OptionsView.Result.BACK) {
            state = State.COMMANDS;
        }
    }

    private void refreshSaves() {
        slots = SaveSlots.list(context.storage(), database);
        hasSaves = false;
        int newest = -1;
        for (int i = 0; i < slots.size; i++) {
            SaveSlots.Slot slot = slots.get(i);
            if (!slot.exists) {
                continue;
            }
            hasSaves = true;
            if (newest < 0 || slot.savedAtMillis > slots.get(newest).savedAtMillis) {
                newest = i;
            }
        }
        // SaveData.get_newest_slot: show the most recent save first.
        slotIndex = newest < 0 ? 0 : newest;
        if (!hasSaves) {
            command = 1; // 新的冒险 is the only usable entry without saves
        }
    }

    private void startNewGame() {
        context.gameState().reset();
        context.audioManager().stopBgm();
        switchTo(new MapScreen(context, database.startMapId()));
    }

    private void loadSelected() {
        loadSlot(loadView.selected());
    }

    private void loadSlot(SaveSlots.Slot slot) {
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
