package pokemon.runtime.ui.menu;

import com.badlogic.gdx.utils.Array;

/**
 * L1: the splash slide sequence of the project's Modular Title Screen
 * ({@code SPLASH_IMAGES} / {@code SECONDS_PER_SPLASH} from the script data).
 * Headless and deterministic: {@link #update(float)} advances the clock,
 * {@link #skip()} jumps to the end (any key during the slides).
 */
public final class SplashSequence {

    private final Array<String> images = new Array<>();
    private final float secondsPerImage;
    private int index;
    private float elapsed;

    public SplashSequence(Array<String> images, float secondsPerImage) {
        if (images != null) {
            this.images.addAll(images);
        }
        this.secondsPerImage = Math.max(0f, secondsPerImage);
    }

    public boolean active() {
        return secondsPerImage > 0f && index < images.size;
    }

    public boolean finished() {
        return !active();
    }

    /** Advances the slide clock by {@code delta} seconds. */
    public void update(float delta) {
        if (!active()) {
            return;
        }
        elapsed += Math.max(0f, delta);
        while (active() && elapsed >= secondsPerImage) {
            elapsed -= secondsPerImage;
            index++;
        }
    }

    /** Skips every remaining slide (a key press during the sequence). */
    public void skip() {
        index = images.size;
        elapsed = 0f;
    }

    /** The image to draw now, or null when the sequence is over. */
    public String currentImage() {
        return active() ? images.get(index) : null;
    }

    public int index() {
        return index;
    }

    /** Seconds elapsed inside the current slide (0 while finished). */
    public float elapsed() {
        return active() ? elapsed : 0f;
    }
}
