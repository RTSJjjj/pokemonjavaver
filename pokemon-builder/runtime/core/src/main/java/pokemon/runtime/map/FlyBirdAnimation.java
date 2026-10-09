package pokemon.runtime.map;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * 340_Fly_Animation {@code pbFlyAnimation}: the bird that flies in from the upper right to the middle of the screen
 * and out to the left, {@code BIRD_ANIMATION_TIME = 0.15} seconds (6 frames at 40 fps) each way. One {@link #tick()} is
 * one frame.
 */
public final class FlyBirdAnimation {
    /** 340_Fly_Animation:5 {@code (BIRD_ANIMATION_TIME * Graphics.frame_rate).to_i}. */
    public static final int PHASE_FRAMES = (int) (0.15 * 40);

    private final Texture bird;
    private final int width;
    private final int height;
    private int frames;

    public FlyBirdAnimation(Texture bird, int width, int height) {
        this.bird = bird;
        this.width = width;
        this.height = height;
    }

    /** The seconds the whole flight takes. */
    public static float durationSeconds() {
        return 2 * Math.max(1, PHASE_FRAMES) / 40f;
    }

    public void tick() {
        frames++;
    }

    public int frames() {
        return frames;
    }

    public boolean finished() {
        return frames >= 2 * Math.max(1, PHASE_FRAMES);
    }

    /** True from the frame the bird is over the middle of the screen. */
    public boolean pastCenter() {
        return frames >= Math.max(1, PHASE_FRAMES);
    }

    /** Draws the bird; {@code left}/{@code bottom}/{@code screenHeight} are the screen's lower-left corner and height in world pixels. */
    public void render(SpriteBatch batch, float left, float bottom, float screenHeight) {
        if (bird == null || finished() || frames == 0) {
            return;
        }
        int total = Math.max(1, PHASE_FRAMES);
        float startX = width + bird.getWidth();                    // :63
        float startY = height / 4f;                                // :64
        float centerX = width / 2f + 10f;                          // :69
        float centerY = height / 2f;                               // :70
        float exitX = -bird.getWidth();                            // :73
        float x;
        float y;
        if (frames <= total) {                                     // :88-99 in
            float progress = frames / (float) total;
            x = startX + (centerX - startX) * progress;
            y = startY + (centerY - startY) * progress;
        } else {                                                   // :115-128 out
            float progress = (frames - total) / (float) total;
            x = centerX + (exitX - centerX) * progress;
            y = centerY + (startY - centerY) * progress;
        }
        // The sprite's origin is its centre (:59-60).
        batch.draw(bird, left + x - bird.getWidth() / 2f, bottom + screenHeight - y - bird.getHeight() / 2f);
    }
}
