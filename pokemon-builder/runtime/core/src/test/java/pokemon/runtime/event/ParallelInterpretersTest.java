package pokemon.runtime.event;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.state.GameState;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R6.13 regression: a parallel event page restarts its own interpreter every
 * few frames. Stopping an interpreter used to close the shared message window,
 * so the message the player was reading vanished and the main event ran through
 * all of its remaining Show Text pages in an instant.
 */
class ParallelInterpretersTest {

    private static EventCommand cmd(int index, int code, JsonValue parameters) {
        EventCommand command = new EventCommand();
        command.index = index;
        command.code = code;
        command.indent = 0;
        command.parameters = parameters;
        return command;
    }

    private static Array<EventCommand> program(EventCommand... commands) {
        Array<EventCommand> program = new Array<>();
        for (EventCommand command : commands) {
            program.add(command);
        }
        return program;
    }

    private static EventInterpreter interpreter(GameState state, MessageService messages,
                                                InputManager input, List<String> warnings) {
        return new EventInterpreter(state, messages, input, null, id -> null, null,
                new PictureService(), warnings::add);
    }

    @Test
    @DisplayName("restarting a parallel interpreter keeps the main message open")
    void parallelRestartKeepsMessages() {
        GameState state = new GameState();
        MessageService messages = new MessageService();
        InputManager input = new InputManager();
        List<String> warnings = new ArrayList<>();

        EventInterpreter main = interpreter(state, messages, input, warnings);
        main.start(program(cmd(0, 101, new JsonReader().parse("[\"第一页\"]")),
                cmd(1, 101, new JsonReader().parse("[\"第二页\"]"))), 6, 1);
        main.update(0f);
        assertTrue(messages.visible(), "the first page is on screen");
        assertTrue(main.running(), "the main event waits for the player");

        // A parallel page (e.g. a fog command that is not rendered yet) runs and
        // finishes while the player is still reading.
        EventInterpreter parallel = interpreter(state, messages, input, warnings);
        parallel.start(program(cmd(0, 204, new JsonReader().parse("[1,1,\"fog\",0,0,0,0,100]"))), 6, 9);
        parallel.update(0f);
        parallel.stop();

        assertTrue(messages.visible(), "the parallel event must not close the window");
        assertFalse(parallel.running());

        // The player's own confirm still advances the dialogue, page by page.
        input.beginFrame();
        input.set(pokemon.runtime.input.GameAction.CONFIRM, true);
        main.update(0f);
        input.endFrame();
        assertTrue(messages.visible(), "the second page is now showing");
        assertEquals("第二页", messages.lines().first());
    }

    @Test
    @DisplayName("stopping the main interpreter is handled by the map screen")
    void stopKeepsTheMessageUntilTheScreenClosesIt() {
        GameState state = new GameState();
        MessageService messages = new MessageService();
        InputManager input = new InputManager();
        EventInterpreter main = interpreter(state, messages, input, new ArrayList<>());
        main.start(program(cmd(0, 101, new JsonReader().parse("[\"文字\"]"))), 6, 1);
        main.update(0f);
        main.stop();
        // The interpreter no longer owns the window, but it stays readable; the
        // map screen closes it (see MapScreen.render).
        assertTrue(messages.visible());
        messages.close();
        assertFalse(messages.visible());
    }
}
