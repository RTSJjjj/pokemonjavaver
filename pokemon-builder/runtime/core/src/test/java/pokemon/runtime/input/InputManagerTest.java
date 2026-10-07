package pokemon.runtime.input;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R13 / project3 section 51: the future touch task builds on the GameAction /
 * InputManager abstraction exactly as it is - the runtime must not grow a
 * second input path. These tests pin the semantics the virtual DPad / A-B
 * buttons will drive: held state, one-press edges and the frame sampler
 * contract. Pure Java on purpose (section 61), so it runs without a graphics
 * context on every backend.
 */
class InputManagerTest {

    @Test
    @DisplayName("press holds an action and release clears it (R13)")
    void holdsAndClears() {
        InputManager input = new InputManager();
        assertFalse(input.isDown(GameAction.UP));
        input.press(GameAction.UP);
        assertTrue(input.isDown(GameAction.UP));
        input.release(GameAction.UP);
        assertFalse(input.isDown(GameAction.UP));
    }

    @Test
    @DisplayName("a held action reports one press edge per frame (R13)")
    void oneEdgePerPress() {
        InputManager input = new InputManager();
        input.press(GameAction.CONFIRM);
        assertTrue(input.wasPressed(GameAction.CONFIRM));

        input.press(GameAction.CONFIRM); // still held: no second edge
        assertTrue(input.wasPressed(GameAction.CONFIRM));
        input.endFrame();
        assertFalse(input.wasPressed(GameAction.CONFIRM));
        assertTrue(input.isDown(GameAction.CONFIRM));

        input.press(GameAction.CONFIRM); // already down, still no new edge
        assertFalse(input.wasPressed(GameAction.CONFIRM));

        input.release(GameAction.CONFIRM);
        input.press(GameAction.CONFIRM); // released and pressed again: new edge
        assertTrue(input.wasPressed(GameAction.CONFIRM));
    }

    @Test
    @DisplayName("consumePressed drops this frame's edges but keeps the held state")
    void consumePressedKeepsHeldState() {
        InputManager input = new InputManager();
        input.press(GameAction.CONFIRM);
        assertTrue(input.wasPressed(GameAction.CONFIRM));

        // A scene consumed the press (the pause menu closed on it): the map
        // must not trigger on the same frame's edge any more.
        input.consumePressed();
        assertFalse(input.wasPressed(GameAction.CONFIRM));
        assertTrue(input.isDown(GameAction.CONFIRM), "the key stays held");

        input.endFrame();
        input.press(GameAction.CONFIRM); // still held: no new edge
        assertFalse(input.wasPressed(GameAction.CONFIRM));
    }

    @Test
    @DisplayName("set() is the frame sampler for a held touch button (R13)")
    void setSyncsHeldState() {
        InputManager input = new InputManager();
        input.set(GameAction.LEFT, true);
        assertTrue(input.isDown(GameAction.LEFT));
        assertTrue(input.wasPressed(GameAction.LEFT));

        input.set(GameAction.LEFT, true); // still held across the frame
        input.beginFrame();
        assertFalse(input.wasPressed(GameAction.LEFT));
        assertTrue(input.isDown(GameAction.LEFT));

        input.set(GameAction.LEFT, false);
        assertFalse(input.isDown(GameAction.LEFT));
    }

    @Test
    @DisplayName("Input.repeat? (PSystem_Controls:223-227): first frame, then every even frame after 20 frames at 40fps")
    void repeatsLikeRgss() {
        InputManager input = new InputManager();
        long[] now = {0L};
        input.clock(() -> now[0]);
        final long frame = 25_000_000L; // 1/40 s
        input.press(GameAction.DOWN);
        assertTrue(input.wasRepeated(GameAction.DOWN), "frame 1");
        int repeats = 0;
        for (int f = 2; f <= 24; f++) {
            input.endFrame();
            input.beginFrame();
            now[0] += frame;
            boolean repeated = input.wasRepeated(GameAction.DOWN);
            if (repeated) repeats++;
            if (f <= 20) assertFalse(repeated, "frame " + f);
            else assertEquals(f % 2 == 0, repeated, "frame " + f);
        }
        assertEquals(2, repeats, "frames 22 and 24");
        input.release(GameAction.DOWN);
        input.beginFrame();
        assertFalse(input.wasRepeated(GameAction.DOWN));
    }
}
