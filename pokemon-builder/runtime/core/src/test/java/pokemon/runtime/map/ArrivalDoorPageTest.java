package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.data.MapData;
import pokemon.runtime.state.GameState;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * R6.15: telling the real door arrival pages apart from normal autorun
 * cutscenes, using the project's own map data (interior map 354 and the town
 * map 353 share one door pair).
 */
class ArrivalDoorPageTest {

    @Test
    @DisplayName("the real door pages are recognised, cutscenes are not")
    void recognisesRealDoorPages() throws Exception {
        File dataRoot = TestData.runtimeDataRoot();
        assumeTrue(dataRoot != null, "runtime data (generated/) not available");

        GameDatabase database = GameDatabase.load(dataRoot.getAbsolutePath(), dataRoot);
        try {
            GameState state = new GameState();
            state.switchNames(database.system().switches);

            // R6.20: discover the door events by shape (a room that carries an
            // arrival page), so data edits do not invalidate the test.
            MapData inside = database.map(354);
            MapData.EventData insideDoor = TestData.arrivalDoor(inside);
            assertNotNull(insideDoor, "map 354 carries an arrival door");
            assertFalse(EventTriggers.isArrivalDoorPage(insideDoor.pages.get(0)),
                    "the player-touch page is the way in, not an arrival page");
            assertNotNull(TestData.arrivalPage(insideDoor), "page 2 is the arrival animation");

            MapData town = database.map(353);
            MapData.EventData outsideDoor = TestData.arrivalDoor(town);
            assertNotNull(outsideDoor, "map 353 carries an arrival door too");
            assertFalse(EventTriggers.isArrivalDoorPage(outsideDoor.pages.get(0)));
            assertTrue(EventTriggers.isArrivalDoorPage(TestData.arrivalPage(outsideDoor)));

            // A plain autorun page (a cutscene) must not get the door treatment.
            MapData.EventPageData cutscene = null;
            for (MapData.EventData event : town.events) {
                for (MapData.EventPageData page : event.pages) {
                    if (page.trigger == EventTriggers.AUTORUN
                            && !EventTriggers.isArrivalDoorPage(page)) {
                        cutscene = page;
                        break;
                    }
                }
                if (cutscene != null) {
                    break;
                }
            }
            assumeTrue(cutscene != null, "map 353 has a plain autorun page");
            assertFalse(EventTriggers.isArrivalDoorPage(cutscene),
                    "a normal autorun cutscene must not get the door treatment");

            // With the hero standing on the door tile the arrival page is the
            // active autorun page - exactly what MapScreen pre-hides for.
            state.enterMap(353, outsideDoor.x, outsideDoor.y);
            MapData.EventPageData active = EventTriggers.pageAt(state, town,
                    outsideDoor.x, outsideDoor.y, EventTriggers.AUTORUN);
            assertTrue(EventTriggers.isArrivalDoorPage(active));
        } finally {
            database.dispose();
        }
    }

}
