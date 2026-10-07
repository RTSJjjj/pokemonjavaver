package pokemon.runtime.ui.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.input.TextTyping;

import java.util.List;

/**
 * {@code pbEnterPlayerName} / {@code pbEnterPokemonName} (TextEntry #70) with the player
 * typing on a real keyboard. The original offers two scenes: the cursor grid
 * ({@code PokemonEntryScene2}, patched by the "字库" plugin into a pinyin picker) and
 * the keyboard scene ({@code PokemonEntryScene}, {@code pbEntry1}). This view keeps the
 * look of the grid scene the project actually shows - {@code Naming/bg}, the subject on
 * the green banner with {@code icon_shadow}, the help text on the dark band, one blank
 * per allowed character with the typed characters above them - and takes its input from
 * the keyboard scene's rules: Enter confirms once {@code minlength} characters are in,
 * Esc returns "" when {@code minlength == 0}, Backspace deletes before the cursor, and
 * the box never grows past {@code maxlength}.
 *
 * <p>Characters arrive through {@link TextTyping}, so the player's own input method
 * (e.g. a Chinese IME) works. Android has no hardware keyboard: there the system text
 * dialog ({@code Gdx.input.getTextInput}) supplies the text instead.</p>
 *
 * <p>Timing is in RGSS 40fps ticks ({@link MenuClock}); the fade in/out is
 * {@code pbFadeInAndShow} / {@code pbFadeOutAndHide} (16 frames).</p>
 */
final class TextEntryView {

    private enum Phase { FADE_IN, ENTRY, FADE_OUT, DONE }

    private static final int FADE_FRAMES = 16;                   // (40*0.4).floor
    private static final int FADE_STEP = 16;                     // (255.0/16).ceil
    /** TrainerWalkingCharSprite#animspeed (frames per pattern). */
    private static final int ANIM_SPEED = 5;
    private static final Color BASE = new Color(248f / 255f, 248f / 255f, 248f / 255f, 1f);
    private static final Color SHADOW = new Color(120f / 255f, 120f / 255f, 120f / 255f, 1f);
    private static final Color BLANK_LIGHT = new Color(0f, 187f / 255f, 1f, 1f);
    private static final Color BLANK_DARK = new Color(0f, 102f / 255f, 170f / 255f, 1f);

    private final RuntimeContext context;
    private final String helptext;
    private final int minLength;
    private final String subjectCharset;
    private final TextEntryModel model;
    private final TextTyping typing = new TextTyping();
    private final MenuClock clock = new MenuClock();

    private Phase phase = Phase.FADE_IN;
    private int phaseTick;
    private int frame;                                           // Graphics.frame_count
    private int animFrame;
    private int animCounter;
    private int blankCursor;
    private boolean blankInit = true;
    private String result = "";

    // Android: the system dialog answers on the UI thread.
    private volatile String dialogText;
    private volatile boolean dialogCancelled;
    private boolean dialogShown;

    // Hold-to-repeat of Backspace / arrows (Input.repeat?: after 20 ticks, every 2).
    private int heldKey = -1;
    private int heldTicks;

    private Texture bg;
    private Texture shadow;
    private boolean loaded;

    TextEntryView(RuntimeContext context, String helptext, int minLength, int maxLength,
                  String initialText, String subjectCharset) {
        this.context = context;
        this.helptext = helptext == null ? "" : helptext;
        this.minLength = Math.max(0, minLength);
        this.subjectCharset = subjectCharset;
        this.model = new TextEntryModel(initialText, maxLength);
    }

    /** What the player entered ("" = cancelled with Esc / nothing typed). */
    String result() {
        return result;
    }

    boolean done() {
        return phase == Phase.DONE;
    }

    void dispose() {
        typing.end();
    }

    /** One rendered frame; returns true once the scene has faded out. */
    boolean update(pokemon.runtime.input.InputManager input) {
        int ticks = clock.advance();
        for (int t = 0; t < ticks; t++) {
            tick();
        }
        switch (phase) {
            case ENTRY:
                updateEntry(ticks);
                break;
            default:
                break;
        }
        return phase == Phase.DONE;
    }

    private void tick() {
        frame++;
        animCounter++;
        if (animCounter >= ANIM_SPEED) {                         // TrainerWalkingCharSprite#update
            animFrame = (animFrame + 1) % 4;
            animCounter -= ANIM_SPEED;
        }
        if (frame % 5 == 0 || blankInit) {                       // pbUpdate: the blank under the cursor
            blankInit = false;
            int cursor = model.cursor();
            if (cursor >= model.maxLength()) {
                cursor = model.maxLength() - 1;
            }
            blankCursor = Math.max(0, cursor);
        }
        switch (phase) {
            case FADE_IN:
                if (++phaseTick > FADE_FRAMES) {                 // j in 0..numFrames
                    next(Phase.ENTRY);
                    startEntry();
                }
                break;
            case FADE_OUT:
                if (++phaseTick > FADE_FRAMES) {
                    next(Phase.DONE);
                }
                break;
            default:
                break;
        }
    }

    private void next(Phase value) {
        phase = value;
        phaseTick = 0;
    }

    private void startEntry() {
        if (TextTyping.useDialog()) {
            showDialog();
        } else {
            typing.begin();
        }
    }

    private void showDialog() {
        dialogShown = true;
        if (Gdx.input == null) {
            dialogCancelled = true;
            return;
        }
        Gdx.input.getTextInput(new Input.TextInputListener() {
            @Override
            public void input(String text) {
                dialogText = text == null ? "" : text;
            }

            @Override
            public void canceled() {
                dialogCancelled = true;
            }
        }, helptext, model.text(), "");
    }

    /** pbEntry1: Esc with minlength 0 returns "", Enter needs minlength characters. */
    private void updateEntry(int ticks) {
        if (dialogShown) {
            String text = dialogText;
            if (text != null) {
                TextEntryModel typed = new TextEntryModel(text, model.maxLength());
                finish(typed.text());
            } else if (dialogCancelled) {
                finish("");
            }
            return;
        }
        List<TextTyping.Event> events = typing.drain();
        for (TextTyping.Event event : events) {
            switch (event.kind) {
                case CHAR:
                    model.insert(event.codePoint);
                    break;
                case BACKSPACE:
                    model.delete();
                    break;
                case DELETE:
                    model.deleteForward();
                    break;
                case LEFT:
                    model.cursorLeft();
                    break;
                case RIGHT:
                    model.cursorRight();
                    break;
                case HOME:
                    model.cursorHome();
                    break;
                case END:
                    model.cursorEnd();
                    break;
                case ESCAPE:
                    if (minLength == 0) {
                        finish("");
                        return;
                    }
                    break;
                case ENTER:
                    if (model.length() >= minLength) {
                        finish(model.text());
                        return;
                    }
                    break;
                default:
                    break;
            }
        }
        repeatHeldKeys(ticks);
    }

    private void repeatHeldKeys(int ticks) {
        if (Gdx.input == null) {
            return;
        }
        int key = -1;
        if (Gdx.input.isKeyPressed(Input.Keys.BACKSPACE)) {
            key = Input.Keys.BACKSPACE;
        } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            key = Input.Keys.LEFT;
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            key = Input.Keys.RIGHT;
        }
        if (key != heldKey) {
            heldKey = key;
            heldTicks = 0;                                       // the first press came from keyDown
            return;
        }
        if (key < 0) {
            return;
        }
        for (int t = 0; t < ticks; t++) {
            heldTicks++;
            if (heldTicks > 20 && heldTicks % 2 == 0) {
                if (key == Input.Keys.BACKSPACE) {
                    model.delete();
                } else if (key == Input.Keys.LEFT) {
                    model.cursorLeft();
                } else {
                    model.cursorRight();
                }
            }
        }
    }

    private void finish(String value) {
        result = value == null ? "" : value;
        typing.end();
        next(Phase.FADE_OUT);
    }

    private void ensureLoaded(MenuAssets assets) {
        if (loaded) {
            return;
        }
        loaded = true;
        bg = assets.graphic("Pictures/Naming", "bg");
        shadow = assets.graphic("Pictures/Naming", "icon_shadow");
    }

    void render(SpriteBatch b, MenuAssets a, MenuFont f, pokemon.runtime.ui.WindowSkin skin) {
        ensureLoaded(a);
        float width = ScreenMetrics.logicalWidth();
        float height = ScreenMetrics.logicalHeight();
        if (bg != null) {
            b.draw(bg, 0f, 0f, width, height);
        } else {
            MenuPanel.fill(b, a, 0f, 0f, width, height, .03f, .08f, .13f, 1f);
        }
        drawSubject(b, a, height);

        // pbDoUpdateOverlay: the help line on the dark band and the typed characters.
        f.draw(b, helptext, 160f + 32f + 48f, height - 12f, BASE, SHADOW);
        float x = 172f + 32f + 48f;
        for (String ch : model.textChars()) {
            drawChar(b, a, f, ch, x, height - 42f);
            x += 24f;
        }
        // One 24x6 blank per allowed character; the one under the cursor sits 4px lower.
        for (int i = 0; i < model.maxLength(); i++) {
            float top = i == blankCursor ? 82f : 78f;
            float bx = 160f + 96f + 24f * i;
            Texture pixel = a.pixel();
            b.setColor(BLANK_DARK);
            b.draw(pixel, bx + 2f, height - top - 6f, 22f, 4f);
            b.setColor(BLANK_LIGHT);
            b.draw(pixel, bx, height - top - 4f, 22f, 4f);
            b.setColor(Color.WHITE);
        }
        // PokemonEntryScene (keyboard) helpwindow text.
        String hint = minLength == 0
                ? "使用键盘输入文本。\n按Enter键确认，按Esc键取消。"
                : "使用键盘输入文本。\n按Enter键确认。";
        String[] lines = hint.split("\n");
        // PokemonEntryScene's helpwindow: the game's window frame (Window_UnformattedTextPokemon).
        float boxW = 448f;
        float boxH = 32f + lines.length * 32f;
        float top = height / 2f + 20f;
        MenuPanel.window(b, a, skin, (width - boxW) / 2f, height - top - boxH, boxW, boxH);
        Color[] tc = MenuPanel.textColors(skin);
        for (int i = 0; i < lines.length; i++) {
            f.drawCentered(b, lines[i], width / 2f, height - (top + 16f + i * 32f), tc[0], tc[1]);
        }

        float alpha = 0f;
        if (phase == Phase.FADE_IN) {
            alpha = Math.max(0, FADE_FRAMES - phaseTick) * FADE_STEP / 255f;
        } else if (phase == Phase.FADE_OUT || phase == Phase.DONE) {
            alpha = Math.min(255, phaseTick * FADE_STEP) / 255f;
            if (phase == Phase.DONE) {
                alpha = 1f;
            }
        }
        if (alpha > 0f) {
            MenuPanel.fill(b, a, 0f, 0f, width, height, 0f, 0f, 0f, Math.min(1f, alpha));
        }
    }

    /** A typed character; one the font lacks is drawn as an empty box instead. */
    private void drawChar(SpriteBatch b, MenuAssets a, MenuFont f, String ch, float x, float y) {
        if (f.hasGlyph(ch.codePointAt(0))) {
            try {
                f.draw(b, ch, x, y, BASE, SHADOW);
                return;
            } catch (RuntimeException error) {
                // fall through to the box
            }
        }
        Texture pixel = a.pixel();
        float size = 18f;
        float left = x + 1f;
        float bottom = y - size - 3f;
        b.setColor(SHADOW);
        b.draw(pixel, left + 2f, bottom - 2f, size, 2f);
        b.draw(pixel, left + 2f, bottom + size - 4f, size, 2f);
        b.draw(pixel, left + 2f, bottom - 2f, 2f, size);
        b.draw(pixel, left + size, bottom - 2f, 2f, size);
        b.setColor(BASE);
        b.draw(pixel, left, bottom, size, 2f);
        b.draw(pixel, left, bottom + size - 2f, size, 2f);
        b.draw(pixel, left, bottom, 2f, size);
        b.draw(pixel, left + size - 2f, bottom, 2f, size);
        b.setColor(Color.WHITE);
    }

    /** The player's walking sprite (TrainerWalkingCharSprite) on the banner. */
    private void drawSubject(SpriteBatch b, MenuAssets a, float height) {
        if (shadow != null) {
            b.draw(shadow, 66f, height - 64f - shadow.getHeight());
        }
        Texture sheet = subjectCharset == null || subjectCharset.isEmpty() ? null : a.character(subjectCharset);
        if (sheet == null || sheet.getWidth() < 4 || sheet.getHeight() < 4) {
            return;
        }
        int frameW = sheet.getWidth() / 4;
        int frameH = sheet.getHeight() / 4;
        float sx = 88f - sheet.getWidth() / 8f;                  // 88 - charwidth/8
        float sy = 76f - sheet.getHeight() / 4f;                 // 76 - charheight/4
        b.draw(sheet, sx, height - sy - frameH, frameW, frameH, animFrame * frameW, 0, frameW, frameH,
                false, false);
    }
}
