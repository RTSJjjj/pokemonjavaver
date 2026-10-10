package pokemon.runtime.ui.menu;

import org.junit.jupiter.api.Test;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.map.TestData;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FastCatchViewTest {

    private FastCatchView view(List<String> balls, int start) {
        File root = TestData.runtimeDataRoot();
        assumeTrue(root != null, "runtime data not available");
        RuntimeContext context = new RuntimeContext("test", null, root.getAbsolutePath());
        return new FastCatchView(context, balls, start, false);
    }

    @Test
    void rightMovesOneBallPerPressAndStaysThere() {
        List<String> balls = Arrays.asList("POKEBALL", "GREATBALL", "ULTRABALL", "NETBALL");
        FastCatchView view = view(balls, 0);
        InputManager input = new InputManager();
        input.beginFrame();
        input.press(GameAction.RIGHT);
        view.update(input);
        assertEquals(1, view.index(), "one step on the press frame");
        input.endFrame();
        for (int frame = 0; frame < 5; frame++) {          // held: no further steps until the repeat delay
            input.beginFrame();
            input.press(GameAction.RIGHT);
            view.update(input);
            assertEquals(1, view.index(), "held frame " + frame);
            input.endFrame();
        }
        input.beginFrame();
        input.release(GameAction.RIGHT);
        view.update(input);
        input.endFrame();
        assertEquals(1, view.index());
    }

    @Test
    void twoBallsToggleOnceAndLeftGoesBack() {
        FastCatchView view = view(Arrays.asList("POKEBALL", "GREATBALL"), 0);
        InputManager input = new InputManager();
        input.beginFrame();
        input.press(GameAction.RIGHT);
        view.update(input);
        input.endFrame();
        assertEquals(1, view.index());
        input.beginFrame();
        input.release(GameAction.RIGHT);
        view.update(input);
        input.endFrame();
        input.beginFrame();
        input.press(GameAction.LEFT);
        view.update(input);
        input.endFrame();
        assertEquals(0, view.index());
    }
}
