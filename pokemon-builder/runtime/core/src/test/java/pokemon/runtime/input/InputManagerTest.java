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
}
