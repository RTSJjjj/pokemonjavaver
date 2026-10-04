package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.data.MapData;
import pokemon.runtime.state.GameState;

import static org.junit.jupiter.api.Assertions.*;

/** R6 loose ends: autorun (3) and event touch (2) page selection. */
class EventTriggerKindsTest {

    private static MapData mapData() {
        MapData data = new MapData();
        data.mapId = 2;
        data.width = 6;
        data.height = 6;
        return data;
    }

    private static MapData.EventData event(int id, int x, int y, int trigger) {
        MapData.EventData event = new MapData.EventData();
        event.id = id;
        event.x = x;
        event.y = y;
        MapData.EventPageData page = new MapData.EventPageData();
        page.trigger = trigger;
        page.graphic = new MapData.EventGraphic();
        page.graphic.characterName = "NPC";
        EventCommand command = new EventCommand();
        command.index = 0;
        command.code = 106;
        page.commands.add(command);
        event.pages.add(page);
        return event;
    }

    @Test
    @DisplayName("the lowest event id wins the autorun slot")
    void autorunPicksFirstEvent() {
        MapData data = mapData();
        data.events.add(event(7, 1, 1, EventTriggers.AUTORUN));
        data.events.add(event(3, 2, 2, EventTriggers.AUTORUN));
        GameState state = new GameState();
        state.enterMap(data.mapId, 0, 0);

        assertNotNull(EventTriggers.autorunPage(state, data));
        // events are scanned in list order, like the map file itself
        assertEquals(7, EventTriggers.autorunEventId(state, data));
    }

    @Test
    @DisplayName("event touch answers the event standing on the player tile")
    void eventTouchMatchesTile() {
        MapData data = mapData();
        data.events.add(event(4, 3, 3, EventTriggers.EVENT_TOUCH));
        data.events.add(event(5, 1, 1, EventTriggers.EVENT_TOUCH));
        GameState state = new GameState();
        state.enterMap(data.mapId, 0, 0);

        assertNotNull(EventTriggers.eventTouchPage(state, data, 3, 3));
        assertEquals(4, EventTriggers.eventTouchEventId(state, data, 3, 3));
        assertNull(EventTriggers.eventTouchPage(state, data, 5, 0));
        assertNull(EventTriggers.autorunPage(state, data));
    }

    @Test
    @DisplayName("a parallel page is only reported for its own event")
    void parallelPages() {
        MapData data = mapData();
        MapData.EventData parallelEvent = event(6, 4, 4, EventTriggers.PARALLEL);
        MapData.EventData actionEvent = event(8, 5, 5, EventTriggers.ACTION);
        data.events.add(parallelEvent);
        data.events.add(actionEvent);
        GameState state = new GameState();
        state.enterMap(data.mapId, 0, 0);

        assertNotNull(EventTriggers.parallelPage(state, data, parallelEvent));
        assertNull(EventTriggers.parallelPage(state, data, actionEvent));
    }
}
