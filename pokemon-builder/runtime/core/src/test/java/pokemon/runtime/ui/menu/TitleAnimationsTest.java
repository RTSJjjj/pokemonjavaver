package pokemon.runtime.ui.menu;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L14: the title animation clocks (frame constants from the plugin, advanced
 * by delta seconds; RGSS runs at 40 fps).
 */
class TitleAnimationsTest {

    @Test
    @DisplayName("intro fades in 20 frames; the blink and clocks follow RGSS rates (L14)")
    void animates() {
        TitleAnimations anim = new TitleAnimations();
        assertEquals(255f, anim.introAlpha(), 0.001f);
        assertFalse(anim.introFinished());

        anim.update(0.5f); // 20 frames: intro 255 - 13*20 -> 0
        assertTrue(anim.introFinished());
        // The blink runs from the start like the original: 255 - 8*20.
        assertEquals(95f, anim.startAlpha(), 0.5f);

        anim.update(0.1f); // 4 more frames
        assertEquals(63f, anim.startAlpha(), 0.5f);
        assertEquals(224f, anim.shineX(), 0.5f); // -16 + 10*24
        assertEquals(24f, anim.scrollOffset(), 0.5f); // +1 * 24 frames
    }

    @Test
    @DisplayName("the intro is skippable and the blink ping-pongs (L14)")
    void skipAndBlink() {
        TitleAnimations skipped = new TitleAnimations();
        skipped.skipIntro();
        assertTrue(skipped.introFinished());
        assertEquals(0f, skipped.introAlpha(), 0.001f);

        TitleAnimations anim = new TitleAnimations();
        anim.update(255f / (8f * TitleAnimations.RGSS_FPS)); // exactly one down leg
        assertEquals(0f, anim.startAlpha(), 0.5f);
        anim.update(0.1f); // now it must go back up
        assertEquals(32f, anim.startAlpha(), 0.5f); // 8 * 4 frames
    }

    @Test
    @DisplayName("FX6 pulses: zoom decreases 32 frames, then flips (L14)")
    void fx6Pulse() {
        TitleAnimations anim = new TitleAnimations();
        anim.update(32f / TitleAnimations.RGSS_FPS); // 32 frames at -0.005
        assertEquals(0.84f, anim.fxZoom(), 0.002f);
        anim.update(32f / TitleAnimations.RGSS_FPS); // next leg goes back up
        assertEquals(1.0f, anim.fxZoom(), 0.002f);
    }

    @Test
    @DisplayName("the shine sweep resets past the 12-screen threshold (L14)")
    void shineResets() {
        TitleAnimations anim = new TitleAnimations();
        anim.update((672f * 12f + 100f) / (10f * TitleAnimations.RGSS_FPS));
        assertTrue(anim.shineX() >= -16f && anim.shineX() <= 672f * 12f);
    }
}
