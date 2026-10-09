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

    /**
     * {@code -Dpokemon.menu.clockDelta=0.025}: a fixed frame time for screen captures, which run
     * inside {@code create()} where libGDX's own delta never advances. Unset = the real delta.
     */
    private static final float FIXED_DELTA = fixedDelta();

    private static float fixedDelta() {
        try {
            return Float.parseFloat(System.getProperty("pokemon.menu.clockDelta", "-1"));
        } catch (NumberFormatException ignored) {
            return -1f;
        }
    }

    private float accumulated;

    /** How many 40fps ticks passed since the last call (at least 0). */
    int advance() {
        if (Gdx.graphics == null) {
            return 1;
        }
        accumulated += FIXED_DELTA >= 0f ? FIXED_DELTA
                : Math.min(pokemon.runtime.app.GameSpeed.scale(Gdx.graphics.getDeltaTime()), 0.25f * pokemon.runtime.app.GameSpeed.multiplier());
        int ticks = (int) (accumulated / TICK);
        accumulated -= ticks * TICK;
        return ticks;
    }
}
