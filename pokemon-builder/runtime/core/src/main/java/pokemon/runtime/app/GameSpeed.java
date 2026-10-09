package pokemon.runtime.app;

/**
 * Game speed-up (release plan P5). The plugin {@code 346_Speed_Up} raises
 * {@code Graphics.frame_rate} (40 x mult) on Alt, Ctrl+Alt returns to 1x, and
 * shows "N×" at the top right. Its stages are {@code [1, 2, 3, 6, 12]}; this
 * port keeps the first three and stops at 4x ({@link #STAGES}).
 *
 * <p>Every screen already advances by the frame time it is handed (walking,
 * messages, animations, the 40 fps menu clock), so speeding the game up means
 * handing each screen {@code delta * multiplier}. Music stays at normal speed
 * and play time keeps counting real seconds.</p>
 */
public final class GameSpeed {

    /** 346_Speed_Up:1 SPEEDUP_STAGES, limited to 4x for this port. */
    public static final int[] STAGES = {1, 2, 3, 4};

    private static int stage;

    private GameSpeed() { }

    public static int multiplier() {
        return STAGES[stage];
    }

    /** Alt: 346_Speed_Up:23-26, the next stage and back to 1x after the last. */
    public static void cycle() {
        stage = (stage + 1) % STAGES.length;
    }

    /** Ctrl+Alt: 346_Speed_Up:20-22. */
    public static void reset() {
        stage = 0;
    }

    /** The frame time a screen should advance by. */
    public static float scale(float delta) {
        return delta * STAGES[stage];
    }

    /** 346_Speed_Up:30-33: the "N×" text is only shown above 1x. */
    public static String label() {
        return stage > 0 ? multiplier() + "×" : null;
    }
}
