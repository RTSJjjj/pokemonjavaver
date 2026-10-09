package pokemon.runtime.ui.menu;

import pokemon.runtime.audio.UiSounds;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.ui.WindowSkin;

import java.util.function.IntConsumer;

/**
 * {@code UIHelper.pbChooseNumber(helpwindow, helptext, maximum, initnum)} (223_PScreen_ItemStorage:275-328): the help
 * text bottom left and an {@code x001} window bottom right; UP/DOWN change by 1 (wrapping), LEFT/RIGHT by 10 (clamped),
 * C answers the number, B answers 0.
 */
final class NumberPrompt {
    private final RuntimeContext context;
    private boolean active;
    private String help = "";
    private int max;
    private int current;
    private IntConsumer done;

    NumberPrompt(RuntimeContext context) {
        this.context = context;
    }

    boolean active() {
        return active;
    }

    void start(String helptext, int maximum, int initnum, IntConsumer callback) {
        this.help = helptext;
        this.max = maximum;
        this.current = initnum;
        this.done = callback;
        this.active = true;
    }

    private String text() {
        return String.format("x%03d", current);                                // _ISPRINTF("x{1:03d}")
    }

    void update(InputManager input) {
        if (!active) return;
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {   // :295
            UiSounds.cancel(context.audioManager());
            finish(0);
        } else if (input.wasPressed(GameAction.CONFIRM)) {                     // :299
            UiSounds.decision(context.audioManager());
            finish(current);
        } else if (input.wasRepeated(GameAction.UP)) {                         // :303
            current = current + 1 > max ? 1 : current + 1;
            UiSounds.cursor(context.audioManager());
        } else if (input.wasRepeated(GameAction.DOWN)) {                       // :308
            current = current - 1 < 1 ? max : current - 1;
            UiSounds.cursor(context.audioManager());
        } else if (input.wasRepeated(GameAction.LEFT)) {                       // :313
            current = Math.max(1, current - 10);
            UiSounds.cursor(context.audioManager());
        } else if (input.wasRepeated(GameAction.RIGHT)) {                      // :318
            current = Math.min(max, current + 10);
            UiSounds.cursor(context.audioManager());
        }
    }

    private void finish(int value) {
        active = false;
        IntConsumer callback = done;
        done = null;
        if (callback != null) callback.accept(value);
    }

    void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, WindowSkin speech, float w, float h) {
        if (!active) return;
        String number = text();
        float numWidth = f.width("x000") + 32f;                                // resizeToFit
        float height = 64f;
        MenuPanel.window(b, a, skin, w - numWidth, 0f, numWidth, height);       // pbBottomRight
        Color[] c = MenuPanel.textColors(skin);
        f.draw(b, number, w - numWidth + 16f, height - 16f - (32f - f.lineHeight()) / 2f, c[0], c[1]);
        float helpWidth = w - numWidth;
        MenuPanel.window(b, a, speech, 0f, 0f, helpWidth, height);              // pbBottomLeft
        Color[] t = MenuPanel.textColors(speech);
        f.draw(b, help, 16f, height - 16f - (32f - f.lineHeight()) / 2f, t[0], t[1]);
    }
}
