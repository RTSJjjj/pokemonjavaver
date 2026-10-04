package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R6.26: {@code pbCaveEntrance} / {@code pbCaveExit}, checked against the
 * numbers of the project's own 0169.rb ({@code pbCaveEntranceEx}):
 * 16 frames per phase at 40 fps, increment {@code (255.0/16).ceil = 16} and the
 * band k updates while {@code k < 15*j/16}.
 */
class CaveTransitionTest {

    private static final float FRAME = 1f / 40f;
    private static final float BAND_PHASE = 16 * FRAME;

    @Test
    @DisplayName("entering: white bands darken from the outside in")
    void enteringBands() {
        CaveTransition cave = new CaveTransition();
        cave.start(false);
        assertTrue(cave.active());
        assertFalse(cave.exiting());
        assertEquals(255, cave.gray(0));
        assertEquals(255, cave.gray(14));

        // j = 0 and j = 1 update nothing (15*0/16 and 15*1/16 are 0).
        cave.update(2 * FRAME);
        assertEquals(255, cave.gray(0));
        cave.update(FRAME);
        assertEquals(239, cave.gray(0));
        assertEquals(255, cave.gray(14));

        cave.update(BAND_PHASE);
        assertEquals(31, cave.gray(0));    // 255 - 14 * 16
        // The Ruby condition k < 15*j/16 never reaches the innermost band
        // (k = 14 needs 15*j/16 > 14, which is 15 at most), so it stays white
        // until the sprite fade blends it towards black.
        assertEquals(255, cave.gray(14));
        assertEquals(1, cave.phase(), "the band phase is over after 0.4 s");

        // The sprite fade blends every band towards black.
        cave.update(8 * FRAME);
        assertEquals(1, cave.phase());
        assertTrue(cave.color(14) < 239 && cave.color(14) > 0,
                "the inner band darkens during the fade: " + cave.color(14));
        cave.update(8 * FRAME);
        assertEquals(2, cave.phase(), "the tone reset starts at 0.8 s");
        assertEquals(0, cave.color(14));
        cave.update(8 * FRAME);
        assertEquals(3, cave.phase());
        cave.update(4 * FRAME);
        assertTrue(cave.active(), "L6d: the final cover is held for the transfer");
        assertTrue(cave.holding());
        cave.stop();
        assertFalse(cave.active(), "the map switch releases the cover");
        assertEquals(1.1f, cave.durationSeconds(), 0.001f);
    }

    @Test
    @DisplayName("L6d: a script that never transfers releases the held cover")
    void holdingSafetyCap() {
        CaveTransition cave = new CaveTransition();
        cave.start(false);
        cave.update(cave.durationSeconds());
        assertTrue(cave.holding());
        cave.update(1.6f);
        assertFalse(cave.active(), "the hold is capped so the screen cannot stay covered");
    }

    @Test
    @DisplayName("exiting: black bands brighten and blend towards white")
    void exitingBands() {
        CaveTransition cave = new CaveTransition();
        cave.start(true);
        assertTrue(cave.exiting());
        assertEquals(0, cave.gray(0));
        assertEquals(0, cave.gray(14));

        cave.update(BAND_PHASE);
        assertEquals(224, cave.gray(0));   // 14 * 16
        assertEquals(0, cave.gray(14), "the innermost band waits for the fade");
        cave.update(2 * BAND_PHASE);
        assertEquals(255, cave.color(14), "the fade finishes the inner band");
    }

    @Test
    @DisplayName("the phase boundaries drive the screen tone hand-over")
    void phases() {
        CaveTransition cave = new CaveTransition();
        cave.start(false);
        assertEquals(0, cave.phase());
        cave.update(BAND_PHASE);
        assertEquals(1, cave.phase());
        cave.update(BAND_PHASE);
        assertEquals(2, cave.phase());
        cave.update(8 * FRAME);
        assertEquals(3, cave.phase());
        cave.update(4 * FRAME);
        assertTrue(cave.active(), "L6d: the cover is held until stop()");
        assertTrue(cave.holding());
        cave.stop();
        assertFalse(cave.active());
    }
}
