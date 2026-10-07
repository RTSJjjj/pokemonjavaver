package pokemon.runtime.map;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.data.MapData;
import pokemon.runtime.event.EventInterpreter;
import pokemon.runtime.event.MapPort;
import pokemon.runtime.event.MessageService;
import pokemon.runtime.event.PictureService;
import pokemon.runtime.event.ScriptIr;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.state.GameState;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Regression for the reported map6/event29 loop: an autorun page (trigger 3)
 * moves the hero with Transfer Player (201) to another tile of the SAME map and
 * only later flips self switch B. The screen used to rebuild itself on every
 * transfer, dropping the interpreter and restarting the page from command 0
 * forever.
 *
 * <p>This test drives the real page with the same transfer policy MapScreen
 * uses ({@link MapScreen#transferStaysInMap}: same map =&gt; keep the screen and
 * resume the event), and asserts the cutscene reaches its end. Skipped when the
 * Builder output is not next to the checkout.</p>
 */
class RealSameMapCutsceneTest {

    private static final float FRAME = 1f / 40f;
    private static final int MAX_FRAMES = 4000;

    @Test
    @DisplayName("map6 event29 page 2 transfers inside the map and still reaches its end")
    void sameMapCutsceneFinishes() {
        File dataRoot = TestData.runtimeDataRoot();
        assumeTrue(dataRoot != null, "runtime data (generated/) not available");

        GameDatabase database = GameDatabase.load(dataRoot.getAbsolutePath(), dataRoot);
        try {
            MapData data = database.map(6);
            MapData.EventData event = eventWithInMapAutorunTransfer(data);
            assertNotNull(event, "map 6 carries an autorun page that transfers inside the map");
            MapData.EventPageData page = eventWithInMapAutorunTransferPage(data, event);
            assertEquals(3, page.trigger, "the page is autorun");

            GameState state = new GameState();
            state.enterMap(data.mapId, event.x, event.y);
            MessageService messages = new MessageService();
            InputManager input = new InputManager();
            int[] transfer = new int[1];
            int[] transferCount = new int[1];
            MapPort port = new MapPort() {
                @Override
                public void transfer(int mapId, int x, int y, int direction) {
                    transfer[0] = mapId;
                    transferCount[0]++;
                }

                @Override
                public void transfer(int mapId, int x, int y, int direction, int fade) {
                    transfer[0] = mapId;
                    transferCount[0]++;
                }

                @Override
                public boolean isMoving(int eventId) {
                    return false; // no route player: 210 completes at once
                }
            };
            EventInterpreter interpreter = new EventInterpreter(state, messages, input, null,
                    id -> null, port, new PictureService(), message -> { });
            interpreter.attachScriptIr(ScriptIr.empty());
            interpreter.start(page.commands, data.mapId, event.id);

            int frames = 0;
            int applied = 0;
            while (interpreter.running() && frames < MAX_FRAMES) {
                if (messages.visible()) {
                    input.beginFrame();
                    input.press(GameAction.CONFIRM);
                    interpreter.update(0f);
                    input.endFrame();
                    input.release(GameAction.CONFIRM);
                } else {
                    interpreter.update(FRAME);
                }
                if (transferCount[0] > applied) {
                    applied = transferCount[0];
                    // MapScreen's frame-end policy: a same-map transfer keeps
                    // the screen and resumes the event, a cross-map one ends it.
                    if (MapScreen.transferStaysInMap(data.mapId, transfer[0])) {
                        interpreter.resumeAfterTransfer();
                    } else {
                        interpreter.stop();
                    }
                }
                frames++;
            }

            assertFalse(interpreter.running(),
                    "the cutscene must finish instead of looping (" + frames + " frames)");
            assertEquals(1, transferCount[0], "the page transfers exactly once");
            assertTrue(state.selfSwitches().get(data.mapId, event.id, "B"),
                    "the page ran to its last commands (self switch B), not from command 0 again");
        } finally {
            database.dispose();
        }
    }

    /** First trigger-3 page that issues a 201 back into its own map. */
    private static MapData.EventData eventWithInMapAutorunTransfer(MapData data) {
        for (MapData.EventData event : data.events) {
            if (eventWithInMapAutorunTransferPage(data, event) != null) {
                return event;
            }
        }
        return null;
    }

    private static MapData.EventPageData eventWithInMapAutorunTransferPage(MapData data,
                                                                          MapData.EventData event) {
        for (MapData.EventPageData page : event.pages) {
            if (page.trigger != 3) {
                continue;
            }
            Array<EventCommand> commands = page.commands;
            for (int i = 0; i < commands.size; i++) {
                EventCommand command = commands.get(i);
                if (command == null || command.code != 201 || command.parameters == null) {
                    continue;
                }
                if (command.parameters.get(0).asInt() == 0
                        && command.parameters.get(1).asInt() == data.mapId) {
                    return page;
                }
            }
        }
        return null;
    }
}
