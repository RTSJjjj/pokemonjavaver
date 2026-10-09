package pokemon.runtime.input.touch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TouchPadTest {
    private static final int W = 2400, H = 1080;
    private final List<TouchButton> buttons = TouchLayout.create(W, H, 1f);
    private final TouchPad pad = new TouchPad(buttons, 8f);

    private TouchButton key(String id) {
        return buttons.stream().filter(b -> b.id.equals(id)).findFirst().orElseThrow();
    }

    private void fingers(float... xy) {
        int n = xy.length / 2;
        boolean[] active = new boolean[n];
        float[] xs = new float[n], ys = new float[n];
        for (int i = 0; i < n; i++) {
            active[i] = true;
            xs[i] = xy[2 * i];
            ys[i] = xy[2 * i + 1];
        }
        pad.update(active, xs, ys);
    }

    @Test
    @DisplayName("three fingers press direction + run + confirm at once")
    void multiTouch() {
        TouchButton dpad = key("dpad");
        fingers(dpad.centerX() + dpad.w * 0.4f, dpad.centerY(),
                key("run").centerX(), key("run").centerY(),
                key("confirm").centerX(), key("confirm").centerY());
        assertTrue(pad.isDown("right"));
        assertTrue(pad.isDown("run"));
        assertTrue(pad.isDown("confirm"));
        assertFalse(pad.isDown("left"));
    }

    @Test
    @DisplayName("the plus pad: arms are keys, the corners and the dead centre are not")
    void plusPad() {
        TouchButton dpad = key("dpad");
        fingers(dpad.centerX(), dpad.centerY() - dpad.h * 0.4f);
        assertTrue(pad.isDown("up") && !pad.isDown("down"));
        fingers(dpad.centerX() - dpad.w * 0.4f, dpad.centerY());
        assertTrue(pad.isDown("left") && !pad.isDown("up"));
        fingers(dpad.x + 2, dpad.y + 2);                       // corner of the bounding box
        assertTrue(pad.pressed().isEmpty());
        fingers(dpad.centerX(), dpad.centerY());               // dead centre
        assertTrue(pad.pressed().isEmpty());
    }

    @Test
    @DisplayName("a finger sliding to the neighbouring key switches keys; lifting releases")
    void slideAndLift() {
        fingers(key("cancel").centerX(), key("cancel").centerY());
        assertTrue(pad.isDown("cancel"));
        fingers(key("confirm").centerX(), key("confirm").centerY());
        assertTrue(pad.isDown("confirm"));
        assertFalse(pad.isDown("cancel"));
        pad.update(new boolean[] {false}, new float[1], new float[1]);
        assertTrue(pad.pressed().isEmpty());
    }

    @Test
    @DisplayName("the middle of the screen is not a key: taps on the game do nothing")
    void gameAreaIsInert() {
        fingers(W / 2f, H / 2f);
        assertTrue(pad.pressed().isEmpty());
    }

    @Test
    @DisplayName("the layout stays inside the screen and keys do not overlap")
    void layoutBounds() {
        for (TouchButton a : buttons) {
            assertTrue(a.x >= 0 && a.y >= 0 && a.x + a.w <= W && a.y + a.h <= H, a.id + " outside the screen");
            for (TouchButton b : buttons) {
                if (a == b) continue;
                boolean overlap = a.x < b.x + b.w && b.x < a.x + a.w && a.y < b.y + b.h && b.y < a.y + a.h;
                assertFalse(overlap, a.id + " overlaps " + b.id);
            }
        }
    }
}
