package pokemon.runtime.input.touch;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.GameSpeed;
import pokemon.runtime.input.KeyStateSource;
import pokemon.runtime.ui.menu.MenuFont;

import java.util.ArrayList;
import java.util.List;

/**
 * The on-screen keys of a touch build (release plan P1), wired into the game:
 * fingers are polled from libGDX every frame, resolved to keys by
 * {@link TouchPad}, and answered through {@link #wrap(KeyStateSource)} as the
 * physical keys the keys stand in for (C, X, Z, A, S and the arrows). The
 * game therefore sees the same {@code GameAction}s as from a keyboard; touching
 * the game picture itself does nothing.
 */
public final class TouchControls {

    /** Button id -> the physical key it stands in for (DefaultKeyBindings). */
    private static final String[] IDS = {
            TouchLayout.UP, TouchLayout.DOWN, TouchLayout.LEFT, TouchLayout.RIGHT,
            TouchLayout.CONFIRM, TouchLayout.CANCEL, TouchLayout.RUN,
            TouchLayout.SHOULDER_L, TouchLayout.SHOULDER_R};
    private static final int[] KEYS = {
            Input.Keys.UP, Input.Keys.DOWN, Input.Keys.LEFT, Input.Keys.RIGHT,
            Input.Keys.C, Input.Keys.X, Input.Keys.Z,
            Input.Keys.A, Input.Keys.S};     // Input::L = A, Input::R = S

    private static final int MAX_FINGERS = 10;

    private TouchPad pad;
    private List<TouchButton> buttons;
    private TouchOverlay overlay;
    private int width, height;
    private final boolean[] active = new boolean[MAX_FINGERS];
    private final float[] xs = new float[MAX_FINGERS];
    private final float[] ys = new float[MAX_FINGERS];
    /** Size factor and opacity (the settings page, later). */
    private float scale = 1f;

    /** Builds the face textures; needs the GL context and the message font. */
    public void create(MenuFont bigFont, MenuFont smallFont) {
        overlay = new TouchOverlay(bigFont, smallFont);
    }

    /** Re-lays the keys out when the screen size changes. */
    private void ensureLayout() {
        int w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        if (pad != null && w == width && h == height) {
            return;
        }
        width = w;
        height = h;
        buttons = TouchLayout.create(w, h, scale);
        pad = new TouchPad(buttons, h * 0.012f);
    }

    /** Polls the fingers; call once per frame before the input is sampled. */
    public void update() {
        ensureLayout();
        for (int i = 0; i < MAX_FINGERS; i++) {
            active[i] = Gdx.input.isTouched(i);
            if (active[i]) {
                xs[i] = Gdx.input.getX(i);
                ys[i] = Gdx.input.getY(i);
            }
        }
        pad.update(active, xs, ys);
        // The speed key (346_Speed_Up) is not a game key: a press steps to the next speed.
        boolean speedDown = pad.isDown(TouchLayout.SPEED);
        if (speedDown && !speedWasDown) {
            GameSpeed.cycle();
        }
        speedWasDown = speedDown;
        for (TouchButton button : buttons) {
            if (TouchLayout.SPEED.equals(button.id)) {
                button.label = GameSpeed.multiplier() + "×";
            }
        }
    }

    private boolean speedWasDown;

    /** Draws the keys over the whole window (after the game has drawn). */
    public void render(SpriteBatch batch) {
        if (overlay == null) {
            return;
        }
        ensureLayout();
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        overlay.render(batch, width, height, buttons, pad);
    }

    /** The physical-key view of the screen keys, added on top of {@code base}. */
    public KeyStateSource wrap(KeyStateSource base) {
        return keyCode -> {
            if (base != null && base.isDown(keyCode)) {
                return true;
            }
            if (pad == null) {
                return false;
            }
            for (int i = 0; i < IDS.length; i++) {
                if (KEYS[i] == keyCode && pad.isDown(IDS[i])) {
                    return true;
                }
            }
            return false;
        };
    }

    public void dispose() {
        if (overlay != null) {
            overlay.dispose();
        }
    }
}
