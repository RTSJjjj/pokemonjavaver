package pokemon.runtime.ui.menu;

import com.badlogic.gdx.Gdx;

/**
 * RGSS runs every scene at 40 frames per second ({@code Graphics.frame_rate}); the
 * plugin scripts count their waits, tweens and animations in those frames
 * ({@code pbWait(16)}, {@code 4*40/20}, {@code 40/4} ...). The runtime renders at the
 * display's rate, so a menu view turns each rendered frame into the number of
 * 40fps ticks that elapsed. Input is still handled once per rendered frame.
 */
final class MenuClock {
    static final float TICK = 1f / 40f;

    private float accumulated;

    /** How many 40fps ticks passed since the last call (at least 0). */
    int advance() {
        if (Gdx.graphics == null) {
            return 1;
        }
        accumulated += Math.min(Gdx.graphics.getDeltaTime(), 0.25f);
        int ticks = (int) (accumulated / TICK);
        accumulated -= ticks * TICK;
        return ticks;
    }
}
