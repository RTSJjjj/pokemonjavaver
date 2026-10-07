package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.data.ProjectInfo;
import pokemon.runtime.event.MenuService;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.map.ToneShader;
import pokemon.runtime.ui.WindowSkin;

import java.util.Arrays;
import java.util.Random;

/**
 * {@code pbGenderSelector}: a line-by-line port of the project's {@code BWGenderSelector}
 * ({@code GenderSelectorScene}, Scripts #294; constants from {@code BWGenderSettings}).
 *
 * <p>The scene is a script written for RGSS's 40 frames per second: every
 * {@code Graphics.update} inside {@code selectBoy} / {@code selectGirl} /
 * {@code selection} / {@code pbShow} / {@code pbHide} is one {@link #tick()} here, with the
 * script's own per-frame increments (x, zoom, tone) on the same doubles, so the bars slide,
 * shrink and darken exactly like the original. Message windows are {@link PbMessage}
 * ({@code pbMessageDisplay} / {@code pbMessage} / {@code pbConfirmMessage}); the name box
 * is {@link TextEntryView} ({@code pbTrainerName} -&gt; {@code pbEnterPlayerName}).</p>
 *
 * <p>Coordinates are the script's (RMXP, top-left origin, 672x448); drawing converts to
 * libGDX with {@code y_gdx = H - y - h}. A sprite's {@code ox/oy} origin and
 * {@code zoom_x/zoom_y} map to the batch's origin + scale.</p>
 */
public final class GenderSelectorView {

    private enum Phase {
        SHOW,           // pbShow: 10 frames of opacity += 25.5
        ASK,            // pbMessageDisplay(msgwindow, "你是男生还是女生？")
        SELECT,         // selectBoy / selectGirl (6 frames)
        LOOP,           // loop do ... Input.trigger?(LEFT/RIGHT/C)
        SLIDE_IN,       // selection: 22 frames, the chosen side moves to the centre
        CONFIRM,        // pbConfirmMessage(是男生吗？ / 是女生吗？)
        SLIDE_OUT,      // 22 frames back (answer "否")
        INTRO,          // pbMessage("还不知道你的名字呢，...")
        HIDE,           // pbHide: 10 frames of opacity -= 25.5
        FADE_OUT,       // pbFadeOutIn: 17 frames to black
        ENTRY,          // pbEnterPlayerName
        FADE_BACK,      // pbFadeOutIn: 17 frames back from black
        NAME_CONFIRM,   // pbConfirmMessage(你的名字是{1}？...)
        DONE
    }

    /** One of the script's sprites: position, zoom, tone and the bitmap it shows. */
    private static final class Spr {
        double x;
        double y;
        double zoomX = 1.0;
        double zoomY = 1.0;
        float toneRgb;
        float toneGray;
        int ox;
        int oy;

        void tone(int rgb, int gray) {
            toneRgb = rgb;
            toneGray = gray;
        }
    }

    private static final int FADE_FRAMES = 16;                   // (40*0.4).floor
    private static final int FADE_STEP = 16;                     // (255.0/16).ceil
    private static final String[] MALE_NAMES = {"沐桐", "由贵", "岚"};     // getRandomNameEx type 0
    private static final String[] FEMALE_NAMES = {"沐夕", "步梦", "佑"};   // getRandomNameEx type 1
    /** MAX_PLAYER_NAME_SIZE (Settings #88009956). */
    private static final int MAX_PLAYER_NAME_SIZE = 10;

    private final RuntimeContext context;
    private final MenuService.Request request;
    private final MenuClock clock = new MenuClock();
    private final PbMessage message;
    private final Random random = new Random();

    private Phase phase = Phase.SHOW;
    private int animLeft;                                        // frames left in the current animation
    private int animFrame;                                       // `frame` inside selectBoy/selectGirl
    private boolean selecting;                                   // true while selectBoy/selectGirl runs
    private boolean boyAnim;
    private int select = -1;
    private int chosen;
    private double opacity;                                      // 0..255, the shared sprite opacity
    private boolean msgwindowVisible = true;
    private String msgwindowText = "";
    private double actualX;                                      // actualboyx / actualgirlx
    private String name = "";
    private TextEntryView entry;
    private boolean started;

    private final Spr genderGirl = new Spr();
    private final Spr genderBoy = new Spr();
    private final Spr girl = new Spr();
    private final Spr boy = new Spr();

    private Texture bg;
    private Texture bar;
    private Texture boySprite;
    private Texture girlSprite;
    private boolean loaded;
    private int barWidth;
    private int barHeight;
    private int boyFrames = 1;
    private int girlFrames = 1;
    private ToneShader tone;

    public GenderSelectorView(RuntimeContext context, MenuService.Request request) {
        this.context = context;
        this.request = request;
        this.message = new PbMessage(context);
    }

    // =====================================================================
    // pbStartScene
    // =====================================================================

    private void start(MenuAssets assets) {
        started = true;
        bg = assets.graphic("Pictures", "introbg");
        bar = assets.graphic("Pictures", "genderselect");
        boySprite = assets.graphic("Pictures", "introBoy");      // TRAINERMALE
        girlSprite = assets.graphic("Pictures", "introGirl");    // TRAINERFEMALE
        int width = ScreenMetrics.logicalWidth();
        int height = ScreenMetrics.logicalHeight();
        barWidth = bar == null ? 0 : bar.getWidth();
        barHeight = bar == null ? 0 : bar.getHeight() / 2;
        // gendergirl.x = Graphics.width; genderboy.x is never set (0); y = Graphics.height/2.
        genderGirl.x = width;
        genderGirl.y = height / 2.0;
        genderBoy.x = 0;
        genderBoy.y = height / 2.0;
        genderGirl.oy = barHeight / 2;
        genderBoy.oy = barHeight / 2;
        genderGirl.ox = barWidth / 2 - barWidth / 6;
        genderBoy.ox = barWidth / 2 + barWidth / 6;
        girl.x = width - 130;
        girl.y = height / 2.0;
        boy.x = 100;
        boy.y = height / 2.0;
        if (girlSprite != null && girlSprite.getHeight() > 0) {
            girlFrames = Math.max(1, girlSprite.getWidth() / girlSprite.getHeight());
            girl.ox = girlSprite.getWidth() / girlFrames / 2;
            girl.oy = girlSprite.getHeight() / 2;
        }
        if (boySprite != null && boySprite.getHeight() > 0) {
            boyFrames = Math.max(1, boySprite.getWidth() / boySprite.getHeight());
            boy.ox = boySprite.getWidth() / boyFrames / 2;
            boy.oy = boySprite.getHeight() / 2;
        }
        opacity = 0;
        animLeft = 10;                                           // pbUpdate: pbShow
    }

    // =====================================================================
    // Per rendered frame
    // =====================================================================

    public boolean update(InputManager input) {
        if (request.done) {
            if (entry != null) {
                entry.dispose();
                entry = null;
            }
            return true;
        }
        if (!started) {
            return false;                                       // waits for the first render (assets)
        }
        if (phase == Phase.ENTRY) {
            if (entry != null && entry.update(input)) {
                String typed = entry.result();
                entry.dispose();
                entry = null;
                nameEntered(typed);
            }
            return phase == Phase.DONE && request.done;
        }
        int ticks = clock.advance();
        if (message.active()) {
            message.update(input, ticks);
        }
        for (int t = 0; t < ticks; t++) {
            tick();
        }
        if (phase == Phase.LOOP) {
            updateLoop(input);
        }
        return request.done;
    }

    /** One 40fps frame of whichever script loop is running. */
    private void tick() {
        switch (phase) {
            case SHOW:
                opacity += 25.5;                                 // @sprites[...].opacity+=25.5
                if (--animLeft <= 0) {
                    opacity = Math.min(255, opacity);
                    askQuestion();
                }
                break;
            case SELECT:
                selectStep();
                break;
            case SLIDE_IN:
                slideInStep();
                break;
            case SLIDE_OUT:
                slideOutStep();
                break;
            case HIDE:
                opacity = Math.max(0, opacity - 25.5);
                if (--animLeft <= 0) {
                    opacity = 0;
                    next(Phase.FADE_OUT, FADE_FRAMES + 1);
                }
                break;
            case FADE_OUT:
                if (--animLeft <= 0) {
                    openEntry();
                }
                break;
            case FADE_BACK:
                if (--animLeft <= 0) {
                    confirmName();
                }
                break;
            default:
                break;
        }
    }

    private void next(Phase value, int frames) {
        phase = value;
        animLeft = frames;
    }

    // ---- pbUpdate -------------------------------------------------------

    private void askQuestion() {
        next(Phase.ASK, 0);
        String text = "你是男生还是女生？";
        message.start(text, null, 0, 0, result -> {
            msgwindowText = text;                                // pbMessageDisplay leaves the text showing
            beginSelect(true);                                   // selectBoy
        });
    }

    private void updateLoop(InputManager input) {
        if (input.wasPressed(GameAction.LEFT)) {
            if (select != 0) {
                beginSelect(true);
                return;
            }
        } else if (input.wasPressed(GameAction.RIGHT)) {
            if (select != 1) {
                beginSelect(false);
                return;
            }
        }
        if (input.wasPressed(GameAction.CONFIRM)) {
            beginSelection();
        }
    }

    // ---- selectBoy / selectGirl ------------------------------------------

    private void beginSelect(boolean boySide) {
        boyAnim = boySide;
        animFrame = 0;
        next(Phase.SELECT, 6);
    }

    private void selectStep() {
        animFrame++;
        if (boyAnim) {
            genderBoy.tone(0, 0);
            boy.tone(0, 0);
            genderBoy.x += 12;
            boy.x += 12;
            if (select == -1) {
                genderGirl.x += 6;
                girl.x += 6;
            } else {
                genderGirl.x += 12;
                girl.x += 12;
            }
            if (boy.zoomX != 1.0) boy.zoomX += 0.015 * 2;
            if (boy.zoomY != 1.0) boy.zoomY += 0.015 * 2;
            if (genderBoy.zoomX != 1.0) genderBoy.zoomX += 0.015 * 2;
            if (genderBoy.zoomY != 1.0) genderBoy.zoomY += 0.015 * 2;
            genderGirl.zoomX -= 0.015;
            genderGirl.zoomY -= 0.015;
            girl.zoomX -= 0.015 * 2;
            girl.zoomY -= 0.015 * 2;
            if (animFrame == 4) {
                genderGirl.tone(-20, 100);
                girl.tone(-60, 100);
            }
        } else {
            genderGirl.tone(0, 0);
            girl.tone(0, 0);
            genderGirl.x -= 12;
            girl.x -= 12;
            if (select == -1) {
                genderBoy.x -= 6;
                boy.x -= 6;
            } else {
                genderBoy.x -= 12;
                boy.x -= 12;
            }
            if (girl.zoomX != 1.0) girl.zoomX += 0.015 * 2;
            if (girl.zoomY != 1.0) girl.zoomY += 0.015 * 2;
            if (genderGirl.zoomX != 1.0) genderGirl.zoomX += 0.015 * 2;
            if (genderGirl.zoomY != 1.0) genderGirl.zoomY += 0.015 * 2;
            genderBoy.zoomX -= 0.015;
            genderBoy.zoomY -= 0.015;
            boy.zoomX -= 0.015 * 2;
            boy.zoomY -= 0.015 * 2;
            if (animFrame == 4) {
                genderBoy.tone(-20, 100);
                boy.tone(-60, 100);
            }
        }
        if (--animLeft <= 0) {
            select = boyAnim ? 0 : 1;
            next(Phase.LOOP, 0);
        }
    }

    // ---- selection ---------------------------------------------------------

    private void beginSelection() {
        actualX = select == 0 ? boy.x : girl.x;
        next(Phase.SLIDE_IN, 22);
    }

    private void slideInStep() {
        double centre = ScreenMetrics.logicalWidth() / 2;       // Graphics.width/2 (integer division)
        if (select == 0) {
            genderGirl.x += 16;
            girl.x += 12;
            genderBoy.x += 16;
            genderBoy.zoomY -= 0.008;
            if (boy.x != centre) boy.x += 12;
            if (boy.x > centre) boy.x = centre;
        } else {
            genderBoy.x -= 16;
            boy.x -= 12;
            genderGirl.x -= 16;
            genderGirl.zoomY -= 0.008;
            if (girl.x != centre) girl.x -= 12;
            if (girl.x < centre) girl.x = centre;
        }
        if (--animLeft <= 0) {
            confirmGender();
        }
    }

    private void confirmGender() {
        next(Phase.CONFIRM, 0);
        message.start(select == 0 ? "是男生吗？" : "是女生吗？", Arrays.asList("是", "否"), 2, 0, result -> {
            if (result == 0) {
                // pbChangePlayer(id) + $game_variables[52] = id ("性别分歧")
                applyPlayer(select);
                msgwindowVisible = false;
                next(Phase.INTRO, 0);
                message.start("还不知道你的名字呢，\n请把你的名字告诉我。", null, 0, 0,
                        done -> selectName());
            } else {
                next(Phase.SLIDE_OUT, 22);
            }
        });
    }

    private void slideOutStep() {
        if (select == 0) {
            genderGirl.x -= 16;
            girl.x -= 12;
            genderBoy.x -= 16;
            genderBoy.zoomY += 0.008;
            if (boy.x != actualX) boy.x -= 12;
            if (boy.x < actualX) boy.x = actualX;
        } else {
            genderBoy.x += 16;
            boy.x += 12;
            genderGirl.x += 16;
            genderGirl.zoomY += 0.008;
            if (girl.x != actualX) girl.x += 12;
            if (girl.x > actualX) girl.x = actualX;
        }
        msgwindowVisible = true;
        if (--animLeft <= 0) {
            next(Phase.LOOP, 0);
        }
    }

    /** The script's pbChangePlayer(id) + $game_variables[52] = id. */
    private void applyPlayer(int id) {
        chosen = id;
        context.gameState().playerId(chosen);
        context.gameState().trainer().gender = chosen;
        context.gameState().variables().set(52, chosen);
    }

    // ---- selectName ----------------------------------------------------------

    /** selectName: pbHide, pbTrainerName, pbConfirmMessage; again when answered "否". */
    private void selectName() {
        next(Phase.HIDE, 10);
    }

    private void openEntry() {
        phase = Phase.ENTRY;
        entry = new TextEntryView(context, "那么，你的名字是？", 0, MAX_PLAYER_NAME_SIZE, "",
                playerCharset(chosen));
    }

    /** pbTrainerName: an empty name falls back to pbSuggestTrainerName(gender). */
    private void nameEntered(String typed) {
        name = typed == null ? "" : typed;
        if (name.isEmpty()) {
            String[] names = chosen == 1 ? FEMALE_NAMES : MALE_NAMES;
            name = names[random.nextInt(names.length)];
            if (name.length() > MAX_PLAYER_NAME_SIZE) {
                name = name.substring(0, MAX_PLAYER_NAME_SIZE);
            }
        }
        context.gameState().playerName(name);
        context.gameState().trainer().name = name;
        next(Phase.FADE_BACK, FADE_FRAMES + 1);
    }

    private void confirmName() {
        next(Phase.NAME_CONFIRM, 0);
        message.start("你的名字是" + name + "？\n真是个不错的名字呢。", Arrays.asList("是", "否"), 2, 0, result -> {
            if (result == 0) {
                phase = Phase.DONE;                              // @finished = true
                request.complete(chosen, name);
            } else {
                selectName();
            }
        });
    }

    /** The trainer's charset name from the project's metaID table (PlayerA..H). */
    private String playerCharset(int playerId) {
        ProjectInfo.RuntimeProfile profile = context.database() == null || context.database().project() == null
                ? null : context.database().project().runtime;
        if (profile == null) {
            return null;
        }
        ProjectInfo.PlayerGraphic graphic = profile.player(playerId);
        if (graphic != null && graphic.charset != null && !graphic.charset.isEmpty()) {
            return graphic.charset;
        }
        return profile.playerCharset == null || profile.playerCharset.isEmpty() ? null : profile.playerCharset;
    }

    // =====================================================================
    // Drawing
    // =====================================================================

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, WindowSkin speech) {
        if (!loaded) {
            loaded = true;
            start(a);
        }
        int width = ScreenMetrics.logicalWidth();
        int height = ScreenMetrics.logicalHeight();
        if (phase == Phase.ENTRY && entry != null) {
            entry.render(b, a, f, skin);
            return;
        }
        float alpha = (float) (Math.max(0, Math.min(255, opacity)) / 255.0);
        if (bg != null && alpha > 0f) {
            b.setColor(1f, 1f, 1f, alpha);
            b.draw(bg, 0, 0, width, height);
            b.setColor(Color.WHITE);
        }
        if (alpha > 0f) {
            if (tone == null) {
                tone = new ToneShader();
            }
            if (tone.isCompiled()) {
                b.setShader(tone.program());
            }
            drawBar(b, genderGirl, 0, alpha, height);
            drawBar(b, genderBoy, barHeight, alpha, height);
            drawTrainer(b, girlSprite, girlFrames, girl, alpha, height);
            drawTrainer(b, boySprite, boyFrames, boy, alpha, height);
            b.setShader(null);
            b.setColor(Color.WHITE);
        }
        if (msgwindowVisible) {
            PbMessage.renderResting(b, a, f, speech, msgwindowText, width, height);
        }
        message.render(b, a, f, skin, speech, width, height);
        drawFade(b, a, width, height);
    }

    /** pbFadeOutIn's black viewport colour: alpha j*16 going out, (16-j)*16 coming back. */
    private void drawFade(SpriteBatch b, MenuAssets a, int width, int height) {
        float alpha;
        if (phase == Phase.FADE_OUT) {
            alpha = Math.min(255, (FADE_FRAMES + 1 - animLeft) * FADE_STEP);
        } else if (phase == Phase.FADE_BACK) {
            alpha = Math.max(0, (animLeft - 1) * FADE_STEP);
        } else {
            return;
        }
        MenuPanel.fill(b, a, 0f, 0f, width, height, 0f, 0f, 0f, Math.min(1f, alpha / 255f));
    }

    private void applyTone(SpriteBatch b, Spr s) {
        b.flush();
        if (tone != null && tone.isCompiled()) {
            tone.setTone(s.toneRgb, s.toneRgb, s.toneRgb, s.toneGray);
        }
    }

    /** genderselect.png: the girl bar is the top half, the boy bar the bottom half. */
    private void drawBar(SpriteBatch b, Spr s, int srcY, float alpha, int height) {
        if (bar == null || barWidth <= 0 || barHeight <= 0) {
            return;
        }
        applyTone(b, s);
        b.setColor(1f, 1f, 1f, alpha);
        // Sprite top-left = (x - ox, y - oy); the zoom pivots around (ox, oy).
        float left = (float) (s.x - s.ox);
        float bottom = (float) (height - (s.y - s.oy) - barHeight);
        b.draw(bar, left, bottom, s.ox, barHeight - s.oy, barWidth, barHeight,
                (float) s.zoomX, (float) s.zoomY, 0f, 0, srcY, barWidth, barHeight, false, false);
    }

    /** The last animation frame of introBoy / introGirl (src_rect.x = (frames-1)*width). */
    private void drawTrainer(SpriteBatch b, Texture texture, int frames, Spr s, float alpha, int height) {
        if (texture == null || texture.getHeight() <= 0) {
            return;
        }
        int frameH = texture.getHeight();
        int frameW = texture.getWidth() / frames;
        applyTone(b, s);
        b.setColor(1f, 1f, 1f, alpha);
        float left = (float) (s.x - s.ox);
        float bottom = (float) (height - (s.y - s.oy) - frameH);
        b.draw(texture, left, bottom, s.ox, frameH - s.oy, frameW, frameH,
                (float) s.zoomX, (float) s.zoomY, 0f, (frames - 1) * frameW, 0, frameW, frameH, false, false);
    }

    public void dispose() {
        if (entry != null) {
            entry.dispose();
            entry = null;
        }
        if (tone != null) {
            tone.dispose();
            tone = null;
        }
    }
}
