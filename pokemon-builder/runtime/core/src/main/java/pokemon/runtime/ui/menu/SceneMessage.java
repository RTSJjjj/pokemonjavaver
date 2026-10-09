package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.ui.WindowSkin;

import java.util.ArrayList;
import java.util.List;

/**
 * The bottom message window of a scene that owns its own screen (the evolution and trade scenes'
 * {@code pbCreateMessageWindow} + {@code pbMessageDisplay}, 071_Messages:1007-): the text is revealed letter by letter at the
 * game's text speed, waits for C / B when it is complete, and understands the control codes those scenes use:
 * {@code \se[name]} (play an SE; empty = stop), {@code \wt[n]} (wait n/20 s), {@code \wtnp[n]} (wait n/20 s and go on without
 * input) and the {@code \1} page pause.
 *
 * <p>Everything counts in 40 fps ticks: call {@link #tick()} once per tick and {@link #input} once per rendered frame.</p>
 */
final class SceneMessage {
    static final char PAUSE = '\u0001';
    private static final int ROW = 32;
    private static final int BORDER = 32;

    private final RuntimeContext context;
    private final List<Tok> tokens = new ArrayList<>();
    private int shown;
    private int wait;
    private boolean pausing;
    private boolean autoFinish;
    private int speedStep;
    private Runnable then;
    /** The text is up and awaits dismissal. */
    private boolean active;
    /** The window keeps showing the last text until {@link #clear()} ({@code msgwindow.text = ""}). */
    private boolean visible;

    /** One revealed unit: a character or a control code. */
    private static final class Tok {
        final char ch;
        final String se;
        final int wait;
        final boolean pause;
        final boolean noPause;

        Tok(char ch, String se, int wait, boolean pause, boolean noPause) {
            this.ch = ch;
            this.se = se;
            this.wait = wait;
            this.pause = pause;
            this.noPause = noPause;
        }
    }

    SceneMessage(RuntimeContext context) {
        this.context = context;
    }

    boolean active() {
        return active;
    }

    boolean visible() {
        return visible;
    }

    void clear() {
        visible = false;
        active = false;
    }

    /** {@code pbMessageDisplay(msgwindow, text)}: runs {@code done} once the player has dismissed the text. */
    void show(String text, Runnable done) {
        tokens.clear();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                int open = text.indexOf('[', i);
                int close = text.indexOf(']', i);
                if (text.startsWith("\\se[", i) && close > open) {
                    tokens.add(new Tok('\0', text.substring(open + 1, close), 0, false, false));
                    i = close + 1;
                    continue;
                }
                boolean noPause = text.startsWith("\\wtnp[", i);
                if ((noPause || text.startsWith("\\wt[", i)) && close > open) {
                    int n = 0;
                    try {
                        n = Integer.parseInt(text.substring(open + 1, close).trim());
                    } catch (NumberFormatException ignored) {
                        // no wait
                    }
                    tokens.add(new Tok('\0', null, n * 40 / 20, false, noPause));
                    i = close + 1;
                    continue;
                }
            }
            if (c == PAUSE) {
                tokens.add(new Tok('\0', null, 0, true, false));
            } else if (c == '\r') {
                // CR of a CR LF pair
            } else {
                tokens.add(new Tok(c, null, 0, false, false));
            }
            i++;
        }
        shown = 0;
        wait = 0;
        pausing = false;
        autoFinish = false;
        then = done;
        active = true;
        visible = true;
    }

    private boolean busy() {
        return shown < tokens.size();
    }

    /** One 40 fps tick of the letter-by-letter reveal. */
    void tick() {
        if (!active || pausing) {
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        if (autoFinish && !busy()) {
            finish();                                                           // \wtnp: no input needed
            return;
        }
        int speed = context.settings().textspeed;
        int step = speed == 0 ? ((speedStep++ % 3 == 0) ? 1 : 0) : speed == 2 ? 3 : 1;
        for (int k = 0; k < step && busy(); k++) {
            Tok t = tokens.get(shown);
            if (t.pause) {
                pausing = true;
                break;
            }
            shown++;
            if (t.se != null) {
                if (!t.se.isEmpty()) context.audioManager().playSe(t.se, 100, 100);
            } else if (t.wait > 0 || t.noPause) {
                wait = t.wait;
                if (t.noPause) autoFinish = true;
                break;
            }
        }
        if (autoFinish && !busy() && wait == 0) {
            finish();
        }
    }

    /** The C / B handling of one rendered frame. */
    void input(boolean confirm, boolean cancel) {
        if (!active || !(confirm || cancel)) {
            return;
        }
        if (busy()) {
            if (pausing && confirm) {
                MenuSe.decision(context.audioManager());
                pausing = false;
                shown++;
            }
            return;
        }
        MenuSe.decision(context.audioManager());
        finish();
    }

    private void finish() {
        active = false;
        Runnable done = then;
        then = null;
        if (done != null) done.run();
    }

    /** {@code pbBottomLeftLines(msgwindow, 2)}: the window along the bottom with the revealed text. */
    void draw(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, float w, float h) {
        if (!visible) {
            return;
        }
        float height = BORDER + 2 * ROW;
        float top = h - height;
        MenuPanel.window(b, a, skin, 0f, h - top - height, w, height);
        Color[] tc = MenuPanel.textColors(skin);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < shown && i < tokens.size(); i++) {
            Tok t = tokens.get(i);
            if (t.ch != '\0') text.append(t.ch);
        }
        String[] lines = text.toString().split("\n", -1);
        for (int i = 0; i < lines.length && i < 2; i++) {
            f.draw(b, lines[i], 16f, h - (top + 16f + i * ROW + (ROW - f.lineHeight()) / 2f), tc[0], tc[1]);
        }
    }
}
