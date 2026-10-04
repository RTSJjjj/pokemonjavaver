package pokemon.runtime.map;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * R6.26: {@code pbCaveEntrance} / {@code pbCaveExit}, straight from the
 * project's own 0169.rb ({@code pbCaveEntranceEx}). Fifteen nested bands grow
 * from white to black while the player enters a cave (black to white on the way
 * out), then the screen tone takes over and the running event continues.
 *
 * <p>The numbers are the script's: {@code totalFrames = (40*0.4).floor = 16},
 * {@code increment = (255.0/16).ceil = 16}, {@code totalBands = 15}, band width
 * {@code ((screen/2)-12)/15} and band height {@code ((screen/2)-10)/15}. After
 * the band phase the sprite fades (0.4 s), the tone resets over 8 frames and
 * the script pauses 4 frames - which is what the event waits for.</p>
 */
public final class CaveTransition {

    /** One animation phase at the project's 40 fps: 16 frames = 0.4 s. */
    public static final int PHASE_FRAMES = 16;
    private static final int INCREMENT = 16;   // (255.0/16).ceil
    private static final int BANDS = 15;
    private static final float FRAME_SECONDS = 1f / 40f;
    private static final int TONE_FRAMES = 8;  // pbToneChangeAll(Tone.new(0,0,0),8)
    private static final int PAUSE_FRAMES = 4; // (40/10).times

    private boolean active;
    private boolean exiting;
    private float elapsed;
    private int bandFrame;
    private int fadeFrame;
    private final int[] grays = new int[BANDS];

    // L6d: when the animation finishes, its final black/white cover stays on
    // screen until the transfer that follows the script switches the map. The
    // original freezes that covered frame into the transition, so the map must
    // not flash through in between. A script that never transfers releases the
    // cover after this many seconds.
    private boolean holding;
    private float holdSeconds;
    private static final float HOLD_MAX_SECONDS = 1.5f;

    /** Starts the animation; entering = false ({@code pbCaveEntrance}). */
    public void start(boolean exiting) {
        this.active = true;
        this.exiting = exiting;
        this.elapsed = 0f;
        this.bandFrame = 0;
        this.fadeFrame = 0;
        this.holding = false;
        this.holdSeconds = 0f;
        java.util.Arrays.fill(grays, exiting ? 0 : 255);
    }

    public boolean active() {
        return active;
    }

    /** True once the animation ended and the final cover is still held. */
    public boolean holding() {
        return holding;
    }

    /** Releases the held cover (the map switch took over). */
    public void stop() {
        active = false;
        holding = false;
    }

    public boolean exiting() {
        return exiting;
    }

    /** Seconds the running event waits for the whole animation (1.1 s). */
    public float durationSeconds() {
        return PHASE_FRAMES * FRAME_SECONDS * 2f + (TONE_FRAMES + PAUSE_FRAMES) * FRAME_SECONDS;
    }

    /**
     * 0 = bands, 1 = overlay and tone, 2 = tone reset, 3 = pause. The map
     * screen hands the screen tone over at the 0/1 and 1/2 boundaries.
     */
    public int phase() {
        float bands = PHASE_FRAMES * FRAME_SECONDS;
        float hold = bands * 2f;
        float tone = hold + TONE_FRAMES * FRAME_SECONDS;
        if (elapsed < bands) {
            return 0;
        }
        if (elapsed < hold) {
            return 1;
        }
        if (elapsed < tone) {
            return 2;
        }
        return 3;
    }

    public void update(float delta) {
        if (!active) {
            return;
        }
        if (holding) {
            holdSeconds += Math.max(0f, delta);
            if (holdSeconds >= HOLD_MAX_SECONDS) {
                stop(); // safety: the script did not lead to a transfer
            }
            return;
        }
        elapsed += Math.max(0f, delta);
        // The epsilon keeps float accumulation (delta sums) from losing a frame
        // right on a phase boundary: the Ruby loop is exact frame counting.
        int frame = Math.min(PHASE_FRAMES, (int) ((elapsed + 1e-4f) / FRAME_SECONDS));
        while (bandFrame < frame) {
            stepBandFrame();
        }
        if (elapsed >= PHASE_FRAMES * FRAME_SECONDS) {
            fadeFrame = Math.min(PHASE_FRAMES,
                    (int) ((elapsed - PHASE_FRAMES * FRAME_SECONDS + 1e-4f) / FRAME_SECONDS));
        }
        if (elapsed + 1e-4f >= durationSeconds()) {
            holding = true; // L6d: keep the final cover for the transfer
            holdSeconds = 0f;
        }
    }

    /** One 40 fps frame of the Ruby band loop (prepended to j = 1..15). */
    private void stepBandFrame() {
        // The Ruby loop runs `totalFrames.times do |j|` and updates every band
        // k < totalBands*j/totalFrames before it draws that frame.
        int j = bandFrame;
        for (int k = 0; k < BANDS; k++) {
            if (k >= BANDS * j / PHASE_FRAMES) {
                continue;
            }
            grays[k] += exiting ? INCREMENT : -INCREMENT;
            if (grays[k] < 0) {
                grays[k] = 0;
            }
            if (grays[k] > 255) {
                grays[k] = 255;
            }
        }
        bandFrame++;
    }

    public int gray(int band) {
        return grays[band];
    }

    /**
     * The colour a band is drawn with right now. During the sprite fade the
     * script blends every band towards solid black (entering) or white
     * (exiting) with {@code sprite.color = Color.new(target, target, target,
     * j*increment)}, which is what finishes the bands the loop left unfinished.
     */
    public int color(int band) {
        if (elapsed < PHASE_FRAMES * FRAME_SECONDS) {
            return grays[band];
        }
        float blend = Math.min(1f, fadeFrame * INCREMENT / 255f);
        int target = exiting ? 255 : 0;
        return Math.round(grays[band] * (1f - blend) + target * blend);
    }

    public int bandFrame() {
        return bandFrame;
    }

    /**
     * Draws the nested bands into a world-space rectangle (the visible area),
     * oldest (outermost) first so every inner band covers the previous one.
     */
    public void render(SpriteBatch batch, Texture pixel, float x, float y,
                       float width, float height) {
        if (!active) {
            return;
        }
        float bandWidth = Math.max(1f, (width / 2f - 12f) / BANDS);
        float bandHeight = Math.max(1f, (height / 2f - 10f) / BANDS);
        float left = x;
        float bottom = y;
        float rectWidth = width;
        float rectHeight = height;
        for (int i = 0; i < BANDS; i++) {
            float gray = color(i) / 255f;
            batch.setColor(gray, gray, gray, 1f);
            batch.draw(pixel, left, bottom, Math.max(1f, rectWidth), Math.max(1f, rectHeight));
            left += bandWidth;
            bottom += bandHeight;
            rectWidth -= bandWidth * 2f;
            rectHeight -= bandHeight * 2f;
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }
}
