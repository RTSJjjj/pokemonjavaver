package pokemon.runtime.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.data.MapData;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.map.EventTriggers;
import pokemon.runtime.map.TestData;
import pokemon.runtime.state.GameState;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Regression for the R6.3 crash: the real Map002 door must transfer to a map
 * that exists. The first version read the 201 parameter at the wrong index
 * (type instead of map id) and asked the screen to open map 0.
 *
 * <p>Skipped when the Builder output is not next to the checkout.</p>
 */
class RealDoorTransferTest {

    @Test
    @DisplayName("Map002 door (player touch) transfers to a real map inside its bounds")
    void doorTransfersToARealMap() {
        File dataRoot = TestData.runtimeDataRoot();
        assumeTrue(dataRoot != null, "runtime data (generated/) not available");

        GameDatabase database = GameDatabase.load(dataRoot.getAbsolutePath(), dataRoot);
        try {
            MapData data = database.map(2);
            // R6.20: discover the door by shape (a player-touch page that hides
            // the hero and transfers through a fade), not by a fixed event id.
            MapData.EventData door = TestData.eventWithDoorAnimation(data,
                    EventTriggers.PLAYER_TOUCH, true);
            assertNotNull(door, "map 2 carries a fading player-touch door");

            GameState state = new GameState();
            state.enterMap(data.mapId, door.x, door.y);
            MapData.EventPageData page =
                    EventTriggers.pageAt(state, data, door.x, door.y, EventTriggers.PLAYER_TOUCH);
            assertNotNull(page, "the door must answer the player-touch trigger");

            List<String> transfers = new ArrayList<>();
            EventInterpreter interpreter = new EventInterpreter(state, new MessageService(),
                    new InputManager(), null, id -> null,
                    new MapPort() {
                        @Override
                        public void transfer(int mapId, int x, int y, int direction) {
                            transfers.add(mapId + "," + x + "," + y + "," + direction + ",0");
                        }

                        @Override
                        public void transfer(int mapId, int x, int y, int direction, int fade) {
                            transfers.add(mapId + "," + x + "," + y + "," + direction + "," + fade);
                        }
                    },
                    new PictureService(), message -> { });
            interpreter.start(page.commands, data.mapId, door.id);
            interpreter.update(0f);
            // The door page waits 8 frames (0.4 s) for its open animation before
            // the transfer command, so the timer has to be advanced.
            interpreter.update(0.5f);
            interpreter.update(0f);

            assertEquals(1, transfers.size(), () -> "expected one transfer, got " + transfers);
            String[] parts = transfers.get(0).split(",");
            int mapId = Integer.parseInt(parts[0]);
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            assertNotEquals("0", parts[4], "the door fades through black (201 param 5)");
            assertTrue(mapId >= 1 && mapId <= database.mapCount(), "target map " + mapId);
            MapData target = database.map(mapId);
            assertTrue(x >= 0 && x < target.width, "target x " + x);
            assertTrue(y >= 0 && y < target.height, "target y " + y);
        } finally {
            database.dispose();
        }
    }

}
