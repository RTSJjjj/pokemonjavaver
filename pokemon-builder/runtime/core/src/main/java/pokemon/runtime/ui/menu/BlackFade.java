package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * {@code pbFadeOutIn} (062_MessageConfig:542-569): the screen fades to black over {@code numFrames + 1 = 17} frames
 * (alpha {@code j * 16}), the block runs behind the black, and the new screen fades back in the same way. A screen that
 * hosts another one (the battle's party screen, the Pokedex page of a caught Pokemon) starts it with
 * {@link #start(Runnable)}: while {@link #active()} the host freezes its input and draws {@link #render} on top.
 */
public final class BlackFade {

    private static final int FRAMES = 16;
    private static final int STEP = 16;

    private enum Phase { NONE, OUT, IN }

    private Phase phase = Phase.NONE;
    private int frame;
    private Runnable atBlack;
    private final MenuClock clock = new MenuClock();

    /** Fades out, runs {@code atBlack} when the screen is black, then fades back in. */
    public void start(Runnable atBlack) {
        this.atBlack = atBlack;
        phase = Phase.OUT;
        frame = 0;
        clock.advance();
    }

    public boolean active() {
        return phase != Phase.NONE;
    }

    /** One rendered frame: advances the 40 fps fade counter. */
    public void update() {
        if (phase == Phase.NONE) {
            return;
        }
        frame += clock.advance();
        if (frame > FRAMES) {                                  // for j in 0..numFrames
            if (phase == Phase.OUT) {
                Runnable run = atBlack;
                atBlack = null;
                phase = Phase.IN;
                frame = 0;
                if (run != null) {
                    run.run();
                }
            } else {
                phase = Phase.NONE;
            }
        }
    }

    /** The black overlay's opacity, 0..1. */
    public float alpha() {
        switch (phase) {
            case OUT:
                return Math.min(255, frame * STEP) / 255f;
            case IN:
                return Math.max(0, (FRAMES - frame) * STEP) / 255f;
            default:
                return 0f;
        }
    }

    public void render(SpriteBatch b, MenuAssets a, float width, float height) {
        float alpha = alpha();
        if (alpha > 0f) {
            MenuPanel.fill(b, a, 0f, 0f, width, height, 0f, 0f, 0f, alpha);
        }
    }
}
