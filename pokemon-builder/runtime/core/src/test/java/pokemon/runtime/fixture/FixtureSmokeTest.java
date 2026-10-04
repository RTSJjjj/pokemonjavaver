package pokemon.runtime.fixture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.CommonEventData;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.data.MapData;
import pokemon.runtime.event.EventInterpreter;
import pokemon.runtime.event.MapPort;
import pokemon.runtime.event.MessageService;
import pokemon.runtime.event.PictureService;
import pokemon.runtime.event.ScriptIr;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.map.TileMap;
import pokemon.runtime.state.GameState;
import pokemon.runtime.state.Inventory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * R15 headless runtime smoke test (project3 sections 60-61): load the fixture,
 * start every event page and run it to completion without a graphics context -
 * "start -> load Map001 -> run frames -> no exception", plus the tile map.
 */
class FixtureSmokeTest {

    private static final float FRAME = 1f / 40f;
    private static final int MAX_FRAMES = 240;

    private final List<String> warnings = new ArrayList<>();
    private final List<String> transfers = new ArrayList<>();

    @Test
    @DisplayName("every fixture event page runs headlessly and finishes (R15 smoke)")
    void everyPageRunsHeadlessly() throws IOException {
        GameDatabase database = FixtureData.load();
        File root = FixtureData.root();
        assumeTrue(database != null && root != null, "fixture data not available");
        try {
            // Loading a real map is part of the smoke ("load Map001").
            MapData map1 = database.map(1);
            new TileMap(map1, database.tileset(map1.tilesetId));

            ScriptIr ir = ScriptIr.load(new File(root, "scripts/ir.json"));
            int pages = 0;
            for (int mapId = 1; mapId <= database.mapCount(); mapId++) {
                MapData map = database.map(mapId);
                for (MapData.EventData event : map.events) {
                    for (MapData.EventPageData page : event.pages) {
                        GameState state = new GameState();
                        state.enterMap(mapId, event.x, event.y);
                        MessageService messages = new MessageService();
                        InputManager input = new InputManager();
                        EventInterpreter interpreter = newInterpreter(database, state, messages, input);
                        interpreter.attachScriptIr(ir);
                        interpreter.attachInventory(new Inventory());
                        interpreter.start(page.commands, mapId, event.id);
                        pump(interpreter, messages, input, "page " + mapId + "/" + event.id,
                                transfers.size());
                        assertFalse(interpreter.running(), "page " + mapId + "/" + event.id + " finishes");
                        pages++;
                    }
                }
            }
            assertTrue(pages >= 7, "the fixture runs every event page, got " + pages);
            assertTrue(transfers.stream().anyMatch(entry -> entry.startsWith("2,10,7,")),
                    "the map 1 door produced its transfer: " + transfers);
            assertTrue(transfers.stream().anyMatch(entry -> entry.startsWith("1,5,4,")),
                    "the map 2 back door produced its transfer: " + transfers);
            assertTrue(warnings.stream().noneMatch(warning -> warning.contains("Exception")),
                    "no exception reached the warning log: " + warnings);
        } finally {
            database.dispose();
        }
    }

    private EventInterpreter newInterpreter(GameDatabase database, GameState state,
                                            MessageService messages, InputManager input) {
        return new EventInterpreter(state, messages, input, null,
                id -> {
                    CommonEventData common = database.commonEvent(id);
                    return common == null ? null : common.commands;
                },
                new MapPort() {
                    @Override
                    public void transfer(int mapId, int x, int y, int direction) {
                        transfers.add(mapId + "," + x + "," + y + "," + direction);
                    }

                    @Override
                    public void transfer(int mapId, int x, int y, int direction, int fade) {
                        transfers.add(mapId + "," + x + "," + y + "," + direction + "," + fade);
                    }
                },
                new PictureService(), warnings::add);
    }

    /**
     * Confirms messages / picks the first choice until the page ends. A
     * Transfer Player ends the page from the screen's point of view: the old
     * map screen (and its interpreter) is disposed on the map change, so the
     * pump stops there instead of waiting for a command that never resumes.
     */
    private void pump(EventInterpreter interpreter, MessageService messages, InputManager input,
                      String label, int transferBaseline) {
        int frames = 0;
        while (interpreter.running() && frames < MAX_FRAMES) {
            if (transfers.size() > transferBaseline) {
                interpreter.stop();
                break;
            }
            if (messages.visible()) {
                input.beginFrame();
                input.press(GameAction.CONFIRM);
                interpreter.update(0f);
                input.endFrame();
                input.release(GameAction.CONFIRM);
            } else {
                interpreter.update(FRAME);
            }
            frames++;
        }
        if (transfers.size() <= transferBaseline) {
            assertTrue(frames < MAX_FRAMES, label + " did not loop forever");
        }
    }
}
