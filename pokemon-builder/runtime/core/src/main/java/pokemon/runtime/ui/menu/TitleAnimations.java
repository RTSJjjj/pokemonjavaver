package pokemon.runtime.ui.menu;

/**
 * L14: the original Modular Title Screen animation clocks (RGSS runs at 40
 * frames/s; the model advances by delta seconds here so the visuals behave the
 * same at any frame rate):
 *
 * <ul>
 *   <li>white intro fade: viewport alpha -13/frame over ~20 frames, skippable;</li>
 *   <li>start-prompt blink: opacity -8/frame, ping-pong 255..0 (runs from the
 *       start like the original, visibility is gated by {@link #introFinished()});</li>
 *   <li>logo shine sweep: src x +10/frame across the mask, reset past width*12;</li>
 *   <li>scrolling overlay: src x +1/frame (the view wraps at the image width);</li>
 *   <li>FX6 shine pulse: zoom +0.005/frame with the direction toggling every 32
 *       frames (starts decreasing, like the plugin's first update).</li>
 * </ul>
 */
public final class TitleAnimations {

    public static final float RGSS_FPS = 40f;
    private static final float INTRO_ALPHA_PER_FRAME = 13f;
    private static final float BLINK_ALPHA_PER_FRAME = 8f;
    private static final float SHINE_PIXELS_PER_FRAME = 10f;
    private static final float SCROLL_PIXELS_PER_FRAME = 1f;
    private static final float FX6_ZOOM_PER_FRAME = 0.005f;
    private static final int FX6_TOGGLE_FRAMES = 32;
    /** RGSS `Sprite#src_rect.x > Graphics.width*12` reset for the shine. */
    private static final float SHINE_RESET = 672f * 12f;

    private float introAlpha = 255f;
    private boolean introSkipped;
    private float startAlpha = 255f;
    private int blinkDirection = -1;
    private float shineX = -16f;
    private float scrollOffset;
    private float fxZoom = 1f;
    private int fxDirection = -1;
    private float fxTimer;
    private float messageTimer;
    private int messageStep;

    /** The plugin's message zoom table (3 frames per step, 20 steps). */
    private static final float[] MESSAGE_ZOOMS = {
            1.00f, 1.01f, 1.02f, 1.03f, 1.04f, 1.05f, 1.06f, 1.07f, 1.08f, 1.09f,
            1.10f, 1.09f, 1.08f, 1.07f, 1.06f, 1.05f, 1.04f, 1.03f, 1.02f, 1.01f,
    };

    /** Advances every clock by {@code delta} seconds. */
    public void update(float delta) {
        float frames = Math.max(0f, delta) * RGSS_FPS;

        if (!introFinished()) {
            introAlpha = introSkipped ? 0f : Math.max(0f, introAlpha - INTRO_ALPHA_PER_FRAME * frames);
        }

        float blink = BLINK_ALPHA_PER_FRAME * frames;
        while (blink > 0f) {
            if (blinkDirection < 0) {
                float step = Math.min(blink, startAlpha);
                startAlpha -= step;
                blink -= step;
                if (startAlpha <= 0f) {
                    blinkDirection = 1;
                }
                if (step <= 0f) {
                    break;
                }
            } else {
                float step = Math.min(blink, 255f - startAlpha);
                startAlpha += step;
                blink -= step;
                if (startAlpha >= 255f) {
                    blinkDirection = -1;
                }
                if (step <= 0f) {
                    break;
                }
            }
        }

        shineX += SHINE_PIXELS_PER_FRAME * frames;
        while (shineX > SHINE_RESET) {
            shineX -= SHINE_RESET + 16f; // reset to -16
        }

        scrollOffset += SCROLL_PIXELS_PER_FRAME * frames;

        fxZoom = Math.max(0.1f, fxZoom + FX6_ZOOM_PER_FRAME * frames * fxDirection);
        fxTimer += frames;
        while (fxTimer >= FX6_TOGGLE_FRAMES) {
            fxTimer -= FX6_TOGGLE_FRAMES;
            fxDirection = -fxDirection;
        }

        messageTimer += frames;
        while (messageTimer >= 3f) {
            messageTimer -= 3f;
            messageStep = (messageStep + 1) % MESSAGE_ZOOMS.length;
        }
    }

    /** `Input::C` during the intro: finish it immediately. */
    public void skipIntro() {
        introSkipped = true;
        introAlpha = 0f;
    }

    public boolean introFinished() {
        return introAlpha <= 0f;
    }

    public float introAlpha() {
        return introAlpha;
    }

    public float startAlpha() {
        return startAlpha;
    }

    /** The shine slice's source x (drawn at logoX + shineX like the plugin). */
    public float shineX() {
        return shineX;
    }

    public float scrollOffset() {
        return scrollOffset;
    }

    public float fxZoom() {
        return fxZoom;
    }

    /** The rotating message's pulse zoom (1.00 .. 1.10). */
    public float messageZoom() {
        return MESSAGE_ZOOMS[messageStep];
    }
}
