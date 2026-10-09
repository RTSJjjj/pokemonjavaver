package pokemon.runtime.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 346_Speed_Up: the stages, the reset and the label. */
class GameSpeedTest {

    @AfterEach
    void back() {
        GameSpeed.reset();
    }

    @Test
    @DisplayName("Alt steps 1x, 2x, 3x, 4x and wraps to 1x (the port stops at 4x)")
    void cycle() {
        assertEquals(1, GameSpeed.multiplier());
        assertNull(GameSpeed.label(), "no text at 1x (346_Speed_Up:30)");
        GameSpeed.cycle();
        assertEquals(2, GameSpeed.multiplier());
        assertEquals("2×", GameSpeed.label());
        GameSpeed.cycle();
        GameSpeed.cycle();
        assertEquals(4, GameSpeed.multiplier());
        GameSpeed.cycle();
        assertEquals(1, GameSpeed.multiplier());
    }

    @Test
    @DisplayName("Ctrl+Alt returns to 1x")
    void reset() {
        GameSpeed.cycle();
        GameSpeed.cycle();
        GameSpeed.reset();
        assertEquals(1, GameSpeed.multiplier());
    }

    @Test
    @DisplayName("every screen's frame time is scaled by the multiplier")
    void scale() {
        assertEquals(0.02f, GameSpeed.scale(0.02f), 1e-6f);
        GameSpeed.cycle();
        GameSpeed.cycle();
        GameSpeed.cycle();
        assertEquals(0.08f, GameSpeed.scale(0.02f), 1e-6f);
    }
}
