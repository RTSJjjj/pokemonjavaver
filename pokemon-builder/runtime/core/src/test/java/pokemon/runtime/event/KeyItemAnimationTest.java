package pokemon.runtime.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 302_BW_Get_Key_Item pbUpdate, frame by frame. */
class KeyItemAnimationTest {
    @Test
    @DisplayName("99 frames at 40 fps: flash, spin in, shake twice, wait 6, fade out")
    void timeline() {
        KeyItemAnimation animation = new KeyItemAnimation("BICYCLE", null);
        assertEquals(1 + 10 + 10 + 18 + 12 + 24 + 6 + 18, animation.frameCount());
        assertEquals(99 / 40f, animation.duration(), 1e-6);
        animation.update(11 / 40f + 0.001f);                                  // the white flash peaks at 250
        assertEquals(250, animation.current().whiteOpacity);
        assertEquals(0, animation.current().itemOpacity);
        animation.update(100f);
        assertTrue(animation.finished());
        KeyItemAnimation.Frame last = animation.current();
        
        assertTrue(last.itemOpacity < 30 && last.bgOpacity < 30);
    }

    @Test
    @DisplayName("opacity truncates to whole steps of 14 and the jingle comes after the 7th pass of the opening loop")
    void jingleAndOpacity() {
        KeyItemAnimation animation = new KeyItemAnimation("BICYCLE", null);
        assertFalse(animation.takeJingle());
        animation.update((21 + 7) / 40f + 0.001f);                            // the frame after the 7th pass
        assertTrue(animation.takeJingle());
        assertFalse(animation.takeJingle(), "once");
        assertEquals(14 * 7, animation.current().itemOpacity);
        assertEquals(180 - 7 * 7, (int) animation.current().itemAngle, "15/2 is 7 in integer division");
        animation.update(18 / 40f);
        assertEquals(252, animation.current().bgOpacity);
    }
}
