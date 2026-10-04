package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.data.MapData;
import pokemon.runtime.state.GameState;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * R6.33: on maps with several doors, every arrival page is active at once
 * (each door evaluates its own {@code s:tsOff?("A")}), so the map-wide autorun
 * queue runs the other doors' pages before the one the player stands on. The
 * map screen burns through them one game frame at a time (RMXP parity) and the
 * door hold keeps the hero hidden while any arrival page is still queued -
 * without that, the gap between two pages released the hold and the hero stood
 * visible on the doorstep for ~0.5 s (reported on the 茶月镇 houses).
 */
class ArrivalDoorQueueTest {

    @Test
    @DisplayName("map 2 queues six arrival pages; the player's own door runs last (R6.33)")
    void arrivalPagesQueueOnMultiDoorMaps() {
        File dataRoot = TestData.runtimeDataRoot();
        assumeTrue(dataRoot != null, "runtime data (generated/) not available");
        GameDatabase database = GameDatabase.load(dataRoot.getAbsolutePath(), dataRoot);
        try {
            MapData data = database.map(2);
            GameState state = new GameState();
            state.switchNames(database.system().switches);
            state.enterMap(2, 11, 28);

            MapData.EventPageData first = EventTriggers.autorunPage(state, data);
            MapData.EventPageData onTile = EventTriggers.pageAt(state, data, 11, 28,
                    EventTriggers.AUTORUN);
            assertNotNull(first, "the map has queued arrival pages");
            assertNotNull(onTile, "the door under the player has an arrival page");
            assertNotSame(first, onTile,
                    "another door's arrival page is queued before the player's own");

            int activeArrivalPages = 0;
            for (MapData.EventData event : data.events) {
                MapData.EventPageData page = EventPages.resolve(state, data.mapId, event);
                if (page != null && EventTriggers.isArrivalDoorPage(page)) {
                    activeArrivalPages++;
                }
            }
            assertTrue(activeArrivalPages >= 6,
                    "all door arrival pages are active right after arrival: " + activeArrivalPages);
        } finally {
            database.dispose();
        }
    }
}
