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
 * Every size is a fraction of the screen height ({@code unit}), so the layout
 * is the same on every phone; the keys sit in the screen corners, i.e. on the
 * letterbox bars when the 672x488 game does not fill a wide screen.
 */
public final class TouchLayout {

    public static final String DPAD = "dpad";
    public static final String UP = "up", DOWN = "down", LEFT = "left", RIGHT = "right";
    public static final String CONFIRM = "confirm", CANCEL = "cancel", RUN = "run", SPEED = "speed";
    public static final String SHOULDER_L = "l", SHOULDER_R = "r";

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
        return keys;
    }
}
