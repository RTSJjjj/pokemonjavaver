package pokemon.runtime.input.touch;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.GameSpeed;
import pokemon.runtime.app.StoragePort;
import pokemon.runtime.input.KeyStateSource;
import pokemon.runtime.ui.menu.MenuFont;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The on-screen keys of a touch build (release plan P1), wired into the game:
 * fingers are polled from libGDX every frame, resolved to keys by
 * {@link TouchPad}, and answered through {@link #wrap(KeyStateSource)} as the
 * physical keys the keys stand in for (C, X, Z, A, S, F8 and the arrows). The
 * game therefore sees the same {@code GameAction}s as from a keyboard; touching
 * the game picture itself does nothing.
 *
 * <p>The gear key opens the layout editor: drag a key to move it, pick it and use the bar to make it bigger or smaller,
 * make all keys fainter or more solid, reset or finish. The arrangement is kept in {@code touch-layout.json}
 * ({@link TouchLayoutStore}). While the editor is open no key reaches the game.</p>
 */
public final class TouchControls {

    /** Button id -> the physical key it stands in for (DefaultKeyBindings). */
    private static final String[] IDS = {
            TouchLayout.UP, TouchLayout.DOWN, TouchLayout.LEFT, TouchLayout.RIGHT,
            TouchLayout.CONFIRM, TouchLayout.CANCEL, TouchLayout.RUN,
            TouchLayout.SHOULDER_L, TouchLayout.SHOULDER_R, TouchLayout.CHEAT};
    private static final int[] KEYS = {
            Input.Keys.UP, Input.Keys.DOWN, Input.Keys.LEFT, Input.Keys.RIGHT,
            Input.Keys.C, Input.Keys.X, Input.Keys.Z,
            Input.Keys.A, Input.Keys.S,      // Input::L = A, Input::R = S
            Input.Keys.F8};                  // Input::F8: the goldFinger menu (049_Scene_Map:200)

    private static final int MAX_FINGERS = 10;

    private TouchPad pad;
    private List<TouchButton> buttons;
    private TouchOverlay overlay;
    private int width, height;
    private final boolean[] active = new boolean[MAX_FINGERS];
    private final float[] xs = new float[MAX_FINGERS];
    private final float[] ys = new float[MAX_FINGERS];
    /** Size factor (1 = default). */
    private float scale = 1f;

    /** The player's own arrangement (layout editor), persisted through {@link #attachStorage}. */
    private TouchLayoutStore store = new TouchLayoutStore();
    private StoragePort storage;

    // Layout editor state.
    private boolean editing;
    private List<TouchButton> bar;
    private TouchPad barPad;
    private String selectedId;
    private int dragFinger = -1;
    private float lastX, lastY;
    private final boolean[] wasActive = new boolean[MAX_FINGERS];
    private final Set<String> barWasDown = new HashSet<>();
    private boolean gearWasDown;

    /** Builds the face textures; needs the GL context and the message font. */
    public void create(MenuFont bigFont, MenuFont smallFont) {
        overlay = new TouchOverlay(bigFont, smallFont);
    }

    /** Where the arrangement is kept; loads the saved one. */
    public void attachStorage(StoragePort storage) {
        this.storage = storage;
        this.store = TouchLayoutStore.load(storage);
        this.pad = null;                                    // lay out again with the saved arrangement
    }

    /** Re-lays the keys out when the screen size changes. */
    private void ensureLayout() {
        int w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        if (pad != null && w == width && h == height) {
            return;
        }
        width = w;
        height = h;
        relayout();
        bar = TouchLayout.editorBar(w, h);
        barPad = new TouchPad(bar, 0f);
    }

    private void relayout() {
        buttons = TouchLayout.create(width, height, scale, store);
        pad = new TouchPad(buttons, height * 0.012f);
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
        if (editing) {
            updateEditor();
            return;
        }
        pad.update(active, xs, ys);
        boolean gearDown = pad.isDown(TouchLayout.GEAR);
        if (gearDown && !gearWasDown) {
            startEditing();                                 // the gear: the layout editor
        }
        gearWasDown = gearDown;
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

    private void startEditing() {
        editing = true;
        selectedId = null;
        dragFinger = -1;
        barWasDown.clear();
        for (int i = 0; i < MAX_FINGERS; i++) {
            wasActive[i] = true;                            // the finger that tapped the gear starts nothing
        }
        pad.update(new boolean[MAX_FINGERS], xs, ys);       // no game key stays held while editing
    }

    /** One frame of the layout editor: the bar's keys act on press, one finger drags the selected key. */
    private void updateEditor() {
        barPad.update(active, xs, ys);
        for (TouchButton item : bar) {
            boolean down = barPad.isDown(item.id);
            boolean was = barWasDown.contains(item.id);
            if (down && !was) {
                barKey(item.id);
            }
            if (down) {
                barWasDown.add(item.id);
            } else {
                barWasDown.remove(item.id);
            }
        }
        if (!editing) {
            for (int i = 0; i < MAX_FINGERS; i++) {
                wasActive[i] = active[i];
            }
            return;
        }
        if (dragFinger >= 0 && !active[dragFinger]) {
            dragFinger = -1;
            store.save(storage);                            // the key was let go: keep the arrangement
        }
        for (int i = 0; i < MAX_FINGERS && dragFinger < 0; i++) {
            if (!active[i] || wasActive[i] || onBar(xs[i], ys[i])) {
                continue;
            }
            TouchButton hit = null;
            float best = Float.MAX_VALUE;
            for (TouchButton button : buttons) {
                if (TouchLayout.GEAR.equals(button.id) || !button.contains(xs[i], ys[i], 0f)) {
                    continue;
                }
                float distance = button.distanceSquared(xs[i], ys[i]);
                if (distance < best) {
                    best = distance;
                    hit = button;
                }
            }
            if (hit != null) {
                selectedId = hit.id;
                dragFinger = i;
                lastX = xs[i];
                lastY = ys[i];
            }
        }
        if (dragFinger >= 0 && selectedId != null) {
            float dx = xs[dragFinger] - lastX, dy = ys[dragFinger] - lastY;
            lastX = xs[dragFinger];
            lastY = ys[dragFinger];
            TouchButton key = find(selectedId);
            if (key != null && (dx != 0f || dy != 0f)) {
                float[] entry = store.entry(key, width, height);
                entry[0] = TouchLayoutStore.clamp(entry[0] + dx / width, 0f, 1f);
                entry[1] = TouchLayoutStore.clamp(entry[1] + dy / height, 0f, 1f);
                relayout();
            }
        }
        for (int i = 0; i < MAX_FINGERS; i++) {
            wasActive[i] = active[i];
        }
    }

    private boolean onBar(float x, float y) {
        for (TouchButton item : bar) {
            if (item.contains(x, y, 0f)) {
                return true;
            }
        }
        return false;
    }

    private TouchButton find(String id) {
        for (TouchButton button : buttons) {
            if (button.id.equals(id)) {
                return button;
            }
        }
        return null;
    }

    private void barKey(String id) {
        TouchButton key = selectedId == null ? null : find(selectedId);
        switch (id) {
            case TouchLayout.EDIT_BIGGER:
            case TouchLayout.EDIT_SMALLER:
                if (key != null) {
                    float[] entry = store.entry(key, width, height);
                    float factor = TouchLayout.EDIT_BIGGER.equals(id) ? 1.1f : 1f / 1.1f;
                    entry[2] = TouchLayoutStore.clamp(entry[2] * factor, TouchLayoutStore.MIN_SCALE, TouchLayoutStore.MAX_SCALE);
                    relayout();
                    store.save(storage);
                }
                break;
            case TouchLayout.EDIT_FADE:
                store.opacity = TouchLayoutStore.clamp(store.opacity - 0.1f, TouchLayoutStore.MIN_OPACITY, 1f);
                store.save(storage);
                break;
            case TouchLayout.EDIT_SOLID:
                store.opacity = TouchLayoutStore.clamp(store.opacity + 0.1f, TouchLayoutStore.MIN_OPACITY, 1f);
                store.save(storage);
                break;
            case TouchLayout.EDIT_RESET:
                store.reset();
                selectedId = null;
                relayout();
                store.save(storage);
                break;
            case TouchLayout.EDIT_DONE:
                editing = false;
                selectedId = null;
                dragFinger = -1;
                gearWasDown = true;                         // the finger is still down: not a second tap on the gear
                store.save(storage);
                break;
            default:
                break;
        }
    }

    /** Draws the keys over the whole window (after the game has drawn). */
    public void render(SpriteBatch batch) {
        if (overlay == null) {
            return;
        }
        ensureLayout();
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        overlay.opacity = store.opacity;
        if (editing) {
            overlay.renderEditor(batch, width, height, buttons, selectedId, bar, barPad,
                    selectedId == null ? "拖动按键来移动位置" : "拖动按键移动，缩小/放大调整大小");
            return;
        }
        overlay.render(batch, width, height, buttons, pad);
    }

    /** The physical-key view of the screen keys, added on top of {@code base}. */
    public KeyStateSource wrap(KeyStateSource base) {
        return keyCode -> {
            if (base != null && base.isDown(keyCode)) {
                return true;
            }
            if (pad == null || editing) {
                return false;                               // the editor is modal: no key reaches the game
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
