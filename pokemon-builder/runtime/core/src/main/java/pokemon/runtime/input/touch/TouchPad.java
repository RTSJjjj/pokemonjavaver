package pokemon.runtime.input.touch;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Multi-touch tracker (release plan P1): every finger is resolved to the key
 * under it each frame, so keys never steal each other's fingers, a finger that
 * slides onto a neighbouring key switches to it, and lifting releases it. Pure
 * Java; the platform feeds finger positions with {@link #update}.
 */
public final class TouchPad {

    private final List<TouchButton> buttons;
    private final Set<String> down = new HashSet<>();
    private final float slop;

    public TouchPad(List<TouchButton> buttons, float slop) {
        this.buttons = buttons;
        this.slop = slop;
    }

    /**
     * @param active  whether finger i is on the screen
     * @param xs      finger x positions, screen pixels
     * @param ys      finger y positions, screen pixels (top = 0)
     */
    public void update(boolean[] active, float[] xs, float[] ys) {
        down.clear();
        for (int i = 0; i < active.length; i++) {
            if (!active[i]) {
                continue;
            }
            TouchButton hit = null;
            float best = Float.MAX_VALUE;
            for (TouchButton button : buttons) {
                if (!button.contains(xs[i], ys[i], slop)) {
                    continue;
                }
                float distance = button.distanceSquared(xs[i], ys[i]);
                if (distance < best) {
                    best = distance;
                    hit = button;
                }
            }
            if (hit != null && hit.shape == TouchButton.Shape.CROSS) {
                addArm(hit, xs[i], ys[i]);
            } else if (hit != null) {
                down.add(hit.id);
            }
        }
    }

    /** The plus is four keys: the arm is the dominant axis; the dead centre presses nothing. */
    private void addArm(TouchButton pad, float px, float py) {
        float dx = px - pad.centerX(), dy = py - pad.centerY();
        float dead = pad.w / 6f * 0.6f;
        if (Math.abs(dx) < dead && Math.abs(dy) < dead) {
            return;
        }
        if (Math.abs(dx) >= Math.abs(dy)) {
            down.add(dx < 0 ? TouchLayout.LEFT : TouchLayout.RIGHT);
        } else {
            down.add(dy < 0 ? TouchLayout.UP : TouchLayout.DOWN);
        }
    }

    public boolean isDown(String buttonId) {
        return down.contains(buttonId);
    }

    public Set<String> pressed() {
        return down;
    }

    public List<TouchButton> buttons() {
        return buttons;
    }
}
