package pokemon.runtime.ui.menu;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** L1: the splash sequence timing (headless, driven by update(delta)). */
class SplashSequenceTest {

    @Test
    @DisplayName("slides advance after their duration and finish (L1)")
    void advances() {
        SplashSequence sequence = new SplashSequence(Array.with("a", "b"), 1f);
        assertEquals("a", sequence.currentImage());
        sequence.update(0.5f);
        assertEquals("a", sequence.currentImage());
        sequence.update(0.6f);
        assertEquals("b", sequence.currentImage());
        sequence.update(1.0f);
        assertTrue(sequence.finished());
        assertNull(sequence.currentImage());
    }

    @Test
    @DisplayName("skip ends the sequence and zero duration disables it (L1)")
    void skipAndDisabled() {
        SplashSequence sequence = new SplashSequence(Array.with("a", "b"), 5f);
        sequence.skip();
        assertTrue(sequence.finished());

        SplashSequence disabled = new SplashSequence(Array.with("a"), 0f);
        assertTrue(disabled.finished(), "no seconds per splash means no sequence");
        SplashSequence empty = new SplashSequence(null, 5f);
        assertTrue(empty.finished(), "no images means no sequence");
    }
}
