package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.ui.WindowSkin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 080_Scene_Credits {@code Scene_Credits}: the credits scroll over the five slides of {@code Graphics/Titles}. The credit
 * text is the plugin's {@code CREDIT} heredoc (the resource {@code credits.txt}); a line with {@code <s>} is split into two
 * halves aligned towards the centre. Everything counts in 40 fps ticks.
 *
 * <p>登记: {@code {INSERTS_PLUGIN_CREDITS_DO_NOT_REMOVE}} (the credits of the registered Essentials plugins,
 * {@code PluginManager}) has no source here and is left out; the 20-frame {@code Graphics.transition} is a black fade.</p>
 */
public final class CreditsView {
    // ---- the constants of the scene (:1-8)
    private static final String[] BACKGROUNDS = {"credits1", "credits2", "credits3", "credits4", "credits5"};
    private static final String MUSIC = "Credits";
    private static final float SCROLL_SPEED = 4f;
    private static final int FREQUENCY = 9;                                    // seconds per slide
    private static final Color OUTLINE = new Color(0f, 0f, 128f / 255f, 1f);
    private static final Color SHADOW = new Color(0f, 0f, 0f, 100f / 255f);
    private static final Color FILL = new Color(1f, 1f, 1f, 1f);

    private final RuntimeContext context;
    private final MenuClock clock = new MenuClock();
    private final List<String> lines = new ArrayList<>();
    private final int framesPerBackground = FREQUENCY * 40;
    private final float oyChangePerFrame = SCROLL_SPEED * 20.0f / 40f;       // :270
    private final float trim;
    private float realOY;
    private int frameCounter;
    private int bgIndex;
    private int fadeTick;
    private boolean ending;
    private boolean finished;
    private float blackAlpha = 255f;

    public CreditsView(RuntimeContext context) {
        this.context = context;
        loadText();
        float height = ScreenMetrics.logicalHeight();
        trim = (float) Math.floor(height / 10f);                               // :268 @trim = Graphics.height/10
        realOY = -(height - trim);                                             // :269
        AudioManager audio = audio();
        if (audio != null) {
            audio.memorizeBgmAndBgs();                                         // :291 previousBGM
            audio.stopBgs();                                                   // :293 pbBGSStop (pbMEStop has no counterpart here)
            audio.stopSe();                                                    // :294 pbSEStop
            audio.fadeBgm(2.0f);                                               // :295 pbBGMFade(2.0)
            audio.playBgm(MUSIC);                                              // :296 pbBGMPlay(CreditsMusic)
        }
    }

    private AudioManager audio() {
        return context.audioManager();
    }

    private void loadText() {
        try (InputStream in = CreditsView.class.getResourceAsStream("credits.txt")) {
            if (in != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) lines.add(line);
            }
        } catch (java.io.IOException ignored) {
            // no credits text: the scroll is empty
        }
        lines.removeIf(l -> l.contains("{INSERTS_PLUGIN_CREDITS_DO_NOT_REMOVE}"));   // 登记: the registered plugins' credits
    }

    public boolean finished() {
        return finished;
    }

    /** @return true when the credits are over */
    public boolean update(InputManager input) {
        int ticks = clock.advance();
        boolean confirm = input.wasPressed(GameAction.CONFIRM);
        for (int i = 0; i < ticks && !finished; i++) {
            step(confirm && i == 0);
        }
        return finished;
    }

    private void step(boolean confirm) {
        if (ending) {                                                          // pbBGMFade, then the scene is over
            if (++fadeTick > 20) finish();
            blackAlpha = Math.min(255f, fadeTick * 255f / 20f);
            return;
        }
        if (blackAlpha > 0f && fadeTick < 20 && !started) {                    // :298 Graphics.transition(20)
            fadeTick++;
            blackAlpha = Math.max(0f, 255f * (1f - fadeTick / 20f));
            if (fadeTick >= 20) {
                started = true;
                fadeTick = 0;
                blackAlpha = 0f;
            }
            return;
        }
        frameCounter += 1;                                                     // :328
        if (frameCounter >= framesPerBackground) {                             // :330 go to next slide
            frameCounter -= framesPerBackground;
            bgIndex = (bgIndex + 1) % BACKGROUNDS.length;
        }
        if (confirm && context.gameState().fieldGlobals().creditsPlayed) {     // :312-318 cancel?
            beginEnd(1.0f);
            return;
        }
        float bitmapHeight = 32f * lines.size();
        if (realOY > bitmapHeight + trim) {                                    // :322-326 last?
            beginEnd(2.0f);
            return;
        }
        realOY += oyChangePerFrame;                                            // :340
    }

    private boolean started;

    private void beginEnd(float fadeSeconds) {
        if (audio() != null) audio().fadeBgm(fadeSeconds);                      // pbBGMFade
        ending = true;
        fadeTick = 0;
    }

    private void finish() {
        context.gameState().fieldGlobals().creditsPlayed = true;               // :307
        if (audio() != null) audio().restoreBgmAndBgs();                       // :308 pbBGMPlay(previousBGM)
        finished = true;
    }

    // =====================================================================
    // render
    // =====================================================================

    /** One draw of a half-line: {@code align} 0 left of {@code x}, 1 centred in {@code width}, 2 right-aligned to {@code x + width}. */
    private void put(SpriteBatch b, MenuFont f, String text, float x, float y, int align, float width, Color color) {
        Color none = new Color(0f, 0f, 0f, 0f);
        if (align == 1) f.drawCentered(b, text, x + width / 2f, y, color, none);
        else if (align == 2) f.drawRight(b, text, x + width, y, color, none);
        else f.draw(b, text, x, y, color, none);
    }

    /** :241-263 the shadow, the eight outline offsets and the fill. */
    private void drawOutlined(SpriteBatch b, MenuFont f, String text, float x, float baseline, int align, float width) {
        put(b, f, text, x, baseline - 8f, align, width, SHADOW);                  // :250 shadow at y + 8
        int[][] around = {{2, -2}, {0, -2}, {-2, -2}, {2, 0}, {-2, 0}, {2, 2}, {0, 2}, {-2, 2}};
        for (int[] d : around) {
            put(b, f, text, x + d[0], baseline + d[1], align, width, OUTLINE);
        }
        put(b, f, text, x, baseline, align, width, FILL);
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin) {
        float w = ScreenMetrics.logicalWidth();
        float h = ScreenMetrics.logicalHeight();
        b.setColor(0f, 0f, 0f, 1f);
        b.draw(a.pixel(), 0f, 0f, w, h);
        b.setColor(Color.WHITE);
        Texture bg = a.graphic("Titles", BACKGROUNDS[bgIndex]);                // :299 / :331 the slide
        if (bg != null) b.draw(bg, 0f, h - bg.getHeight());
        // the credits sprite sits in a viewport of (0, trim, W, H - 2*trim): the slide is redrawn over the two bands
        for (int i = 0; i < lines.size(); i++) {
            float top = trim + i * 32f - realOY;                                // line i of the bitmap, scrolled by oy
            if (top > h || top < -64f) continue;
            String[] parts = lines.get(i).split("<s>", -1);
            float baseline = h - (top + (32f - f.lineHeight()) / 2f);
            if (parts.length > 1) {
                for (int j = 0; j < parts.length; j++) {
                    float xpos = (j == 0) ? 0f : 20f + w / 2f;                  // :246
                    int align = (j == 0) ? 2 : 0;                               // right-aligned / left-aligned
                    drawOutlined(b, f, parts[j], xpos, baseline, align, w / 2f - 20f);
                }
            } else {
                drawOutlined(b, f, parts[0], 0f, baseline, 1, w);
            }
        }
        if (bg != null) {
            int bandTop = (int) trim;
            b.draw(bg, 0f, h - bandTop, bg.getWidth(), bandTop, 0, 0, bg.getWidth(), bandTop, false, false);
            b.draw(bg, 0f, 0f, bg.getWidth(), bandTop, 0, bg.getHeight() - bandTop, bg.getWidth(), bandTop, false, false);
        }
        if (blackAlpha > 0f) {
            b.setColor(0f, 0f, 0f, Math.min(1f, blackAlpha / 255f));
            b.draw(a.pixel(), 0f, 0f, w, h);
            b.setColor(Color.WHITE);
        }
    }
}
