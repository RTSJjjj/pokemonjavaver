package pokemon.runtime.input.touch;

import java.util.ArrayList;
import java.util.List;

import pokemon.runtime.input.touch.TouchButton.Shape;

/**
 * Default on-screen layout (release plan P1), landscape only.
 *
 * <pre>
 *   [L]                                        [R]
 *
 *      ^                               [Z 奔跑] [加速]
 *    < + >   (one plus-shaped pad)     [X 取消] [C 确认]
 *      v
 * </pre>
 *
 * The gear (layout editor) sits beside L and the cheat key beside R; the player's own arrangement, if any, is applied on
 * top by {@link #apply}.
 *
 * Every size is a fraction of the screen height ({@code unit}), so the layout
 * is the same on every phone; the keys sit in the screen corners, i.e. on the
 * letterbox bars when the 672x488 game does not fill a wide screen.
 */
public final class TouchLayout {

    public static final String DPAD = "dpad";
    public static final String UP = "up", DOWN = "down", LEFT = "left", RIGHT = "right";
    public static final String CONFIRM = "confirm", CANCEL = "cancel", RUN = "run", SPEED = "speed";
    public static final String SHOULDER_L = "l", SHOULDER_R = "r";
    /** The cheat key: the plugin's F8 {@code goldFinger} menu (049_Scene_Map:200-201). */
    public static final String CHEAT = "cheat";
    /** The gear: opens the layout editor (not a game key). */
    public static final String GEAR = "gear";
    /** The layout editor's own bar. */
    public static final String EDIT_SMALLER = "edit_smaller", EDIT_BIGGER = "edit_bigger",
            EDIT_FADE = "edit_fade", EDIT_SOLID = "edit_solid", EDIT_RESET = "edit_reset", EDIT_DONE = "edit_done";

    private TouchLayout() { }

    /**
     * @param width  screen width in pixels
     * @param height screen height in pixels
     * @param scale  user size factor (1 = default, settings page)
     */
    public static List<TouchButton> create(int width, int height, float scale) {
        float d = height * 0.13f * scale;      // face key diameter
        float g = d * 0.1f;                    // gap
        float m = height * 0.04f;              // screen margin
        List<TouchButton> keys = new ArrayList<>();

        // Direction keys: one plus-shaped pad; the four arms are the keys.
        float cross = height * 0.36f * scale;
        keys.add(new TouchButton(DPAD, Shape.CROSS, 0, "", "")
                .place(m, height - m - cross, cross, cross));

        // 2x2 face keys, C at the bottom right where the thumb rests.
        float fx = width - m - d - g / 2;      // centre of the 2x2 block, x
        float fy = height - m - d - g / 2;     // centre of the 2x2 block, y
        float off = (d + g) / 2;
        keys.add(new TouchButton(RUN, Shape.ROUND, 0, "Z", "奔跑")
                .place(fx - off - d / 2, fy - off - d / 2, d, d));
        keys.add(new TouchButton(SPEED, Shape.ROUND, 0, "1×", "加速")
                .place(fx + off - d / 2, fy - off - d / 2, d, d));
        keys.add(new TouchButton(CANCEL, Shape.ROUND, 0, "X", "取消")
                .place(fx - off - d / 2, fy + off - d / 2, d, d));
        keys.add(new TouchButton(CONFIRM, Shape.ROUND, 0, "C", "确认")
                .place(fx + off - d / 2, fy + off - d / 2, d, d));

        // Shoulder keys in the top corners.
        float sw = d * 1.9f, sh = d * 0.6f;
        keys.add(new TouchButton(SHOULDER_L, Shape.PILL, 0, "L", "")
                .place(m, m, sw, sh));
        keys.add(new TouchButton(SHOULDER_R, Shape.PILL, 0, "R", "")
                .place(width - m - sw, m, sw, sh));

        // The layout editor's gear next to L, the cheat key next to R.
        keys.add(new TouchButton(GEAR, Shape.ROUND, 0, "", "")
                .place(m + sw + g * 2f, m + (sh - sh * 1.1f) / 2f, sh * 1.1f, sh * 1.1f));
        keys.add(new TouchButton(CHEAT, Shape.PILL, 0, "作弊", "")
                .place(width - m - sw - g * 2f - sw, m, sw, sh));
        return keys;
    }

    /** The default layout with the player's saved positions and sizes applied. */
    public static List<TouchButton> create(int width, int height, float scale, TouchLayoutStore store) {
        List<TouchButton> keys = create(width, height, scale);
        apply(keys, width, height, store);
        return keys;
    }

    /** Moves / scales each key to its saved entry (the gear itself never moves, so the editor can always be reached). */
    public static void apply(List<TouchButton> keys, int width, int height, TouchLayoutStore store) {
        if (store == null) {
            return;
        }
        for (TouchButton key : keys) {
            float[] entry = store.keys.get(key.id);
            if (entry == null || GEAR.equals(key.id)) {
                continue;
            }
            float w = key.w * entry[2], h = key.h * entry[2];
            float x = Math.max(0f, Math.min(width - w, entry[0] * width - w / 2f));
            float y = Math.max(0f, Math.min(height - h, entry[1] * height - h / 2f));
            key.place(x, y, w, h);
        }
    }

    /** The editor's bar of six pills, centred on the screen: smaller, bigger, fainter, more solid, reset, done. */
    public static List<TouchButton> editorBar(int width, int height) {
        float d = height * 0.13f;
        float w = d * 1.45f, h = d * 0.62f, g = d * 0.12f;
        String[][] items = {
                {EDIT_SMALLER, "缩小"}, {EDIT_BIGGER, "放大"}, {EDIT_FADE, "更淡"},
                {EDIT_SOLID, "更实"}, {EDIT_RESET, "重置"}, {EDIT_DONE, "完成"}};
        float total = items.length * w + (items.length - 1) * g;
        float x = (width - total) / 2f;
        float y = height * 0.5f - h / 2f;
        List<TouchButton> bar = new ArrayList<>();
        for (String[] item : items) {
            bar.add(new TouchButton(item[0], Shape.PILL, 0, item[1], "").place(x, y, w, h));
            x += w + g;
        }
        return bar;
    }
}
