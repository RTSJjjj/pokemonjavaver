package pokemon.runtime.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** R6.4b: fade / flash / shake timing without any OpenGL. */
class ScreenEffectsTest {

    @Test
    @DisplayName("fade out reaches black, fade in returns to clear")
    void fades() {
        ScreenEffects effects = new ScreenEffects();
        effects.fade(20, true); // 1 s
        effects.update(0.5f);
        assertEquals(127.5f, effects.fade(), 0.5f);
        effects.update(0.6f);
        assertEquals(255f, effects.fade());

        effects.fade(10, false); // 0.5 s
        effects.update(0.5f);
        assertEquals(0f, effects.fade());
    }

    @Test
    @DisplayName("a zero duration fade applies immediately")
    void instantFade() {
        ScreenEffects effects = new ScreenEffects();
        effects.fade(0, true);
        assertEquals(255f, effects.fade());
    }

    @Test
    @DisplayName("flash decays to nothing and keeps its colour")
    void flashDecays() {
        ScreenEffects effects = new ScreenEffects();
        effects.flash(1f, 0f, 0f, 255f, 8); // 0.4 s
        assertTrue(effects.flashing());
        assertEquals(1f, effects.flashAlpha(), 0.01f);
        effects.update(0.2f);
        assertEquals(0.5f, effects.flashAlpha(), 0.05f);
        effects.update(0.3f);
        assertFalse(effects.flashing());
        assertEquals(0f, effects.flashAlpha());
        assertEquals(1f, effects.flashRed());
    }

    @Test
    @DisplayName("shake offsets decay back to zero and clear() resets everything")
    void shakeAndClear() {
        ScreenEffects effects = new ScreenEffects();
        effects.shake(8f, 5f, 20);
        effects.update(0.1f);
        assertNotEquals(0f, effects.shakeX() + effects.shakeY());
        effects.update(1.5f);
        assertEquals(0f, effects.shakeX());
        assertEquals(0f, effects.shakeY());

        effects.fade(0, true);
        effects.clear();
        assertEquals(0f, effects.fade());
    }

    @Test
    @DisplayName("a tone interpolates and the door blackout reaches -255")
    void tint() {
        ScreenEffects effects = new ScreenEffects();
        effects.tint(-255f, -255f, -255f, 0f, 6); // the door's black screen
        effects.update(0.15f);                    // half of 6 frames
        assertEquals(-127.5f, effects.toneRed(), 0.5f);
        effects.update(0.2f);
        assertEquals(-255f, effects.toneRed());
        assertEquals(-255f, effects.toneGreen());
        assertEquals(-255f, effects.toneBlue());
        assertEquals(0f, effects.toneGray());

        effects.tint(0f, 0f, 0f, 0f, 0);
        assertEquals(0f, effects.toneRed());
    }

    @Test
    @DisplayName("clearTone drops the black screen of a door but keeps the fade")
    void clearToneKeepsFade() {
        ScreenEffects effects = new ScreenEffects();
        effects.tint(-255f, -255f, -255f, 0f, 0);
        effects.fade(0, true);
        effects.clearTone();
        assertEquals(0f, effects.toneRed());
        assertEquals(0f, effects.toneGreen());
        assertEquals(255f, effects.fade(), "the transfer fade must survive");
    }
}
