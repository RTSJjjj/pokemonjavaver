package pokemon.runtime.map;

import com.badlogic.gdx.utils.JsonValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.data.MapData;
import pokemon.runtime.state.GameState;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R6.3 door tests: RMXP trigger 1 (player touch) events are the doors this
 * project uses, trigger 0 (action button) are signs and NPCs.
 */
class EventTriggersTest {

    private static MapData mapData() {
        MapData data = new MapData();
        data.mapId = 7;
        data.width = 4;
        data.height = 4;
        return data;
    }

    private static MapData.EventData event(int id, int x, int y, int trigger) {
        MapData.EventData event = new MapData.EventData();
        event.id = id;
        event.x = x;
        event.y = y;
        MapData.EventPageData page = new MapData.EventPageData();
        page.page = 1;
        page.trigger = trigger;
        page.graphic = new MapData.EventGraphic();
        page.graphic.characterName = trigger == EventTriggers.PLAYER_TOUCH ? "doors12" : "NPC";
        EventCommand command = new EventCommand();
        command.index = 0;
        command.code = trigger == EventTriggers.PLAYER_TOUCH ? 201 : 101;
        command.parameters = new JsonValue(JsonValue.ValueType.array);
        page.commands.add(command);
        event.pages.add(page);
        return event;
    }

    @Test
    @DisplayName("doors answer player touch, signs and NPCs answer the action button")
    void triggersAreSeparated() {
        MapData data = mapData();
        data.events.add(event(1, 2, 2, EventTriggers.PLAYER_TOUCH)); // door
        data.events.add(event(2, 1, 2, EventTriggers.ACTION));       // sign / NPC
        GameState state = new GameState();
        state.enterMap(data.mapId, 0, 0);

        assertNotNull(EventTriggers.pageAt(state, data, 2, 2, EventTriggers.PLAYER_TOUCH));
        assertNull(EventTriggers.pageAt(state, data, 2, 2, EventTriggers.ACTION));
        assertNotNull(EventTriggers.pageAt(state, data, 1, 2, EventTriggers.ACTION));
        assertNull(EventTriggers.pageAt(state, data, 1, 2, EventTriggers.PLAYER_TOUCH));

        assertEquals(1, EventTriggers.eventIdAt(state, data, 2, 2, EventTriggers.PLAYER_TOUCH));
        assertEquals(-1, EventTriggers.eventIdAt(state, data, 3, 3, EventTriggers.PLAYER_TOUCH));
    }

    @Test
    @DisplayName("an autorun page holding only the closing empty command never starts (Game_Event#start @list.size > 1)")
    void emptyAutorunPageDoesNotStart() {
        MapData data = mapData();
        MapData.EventData empty = event(1, 0, 0, EventTriggers.AUTORUN);
        empty.pages.first().commands.first().code = 0;
        data.events.add(empty);
        data.events.add(event(2, 1, 1, EventTriggers.AUTORUN));
        GameState state = new GameState();
        state.enterMap(data.mapId, 0, 0);

        assertFalse(EventTriggers.startable(empty.pages.first()));
        assertSame(data.events.get(1).pages.first(), EventTriggers.autorunPage(state, data));
        assertEquals(2, EventTriggers.autorunEventId(state, data));
    }

    @Test
    @DisplayName("two autorun pages: the first of Ruby's Hash order starts (map 402 event 17 before event 2)")
    void autorunFollowsRubyHashOrder() {
        MapData data = mapData();
        for (int id = 1; id <= 21; id++) {
            data.events.add(event(id, 0, 0, id == 2 || id == 17 ? EventTriggers.AUTORUN : EventTriggers.ACTION));
        }
        GameState state = new GameState();
        state.enterMap(data.mapId, 0, 0);

        assertEquals(17, EventTriggers.autorunEventId(state, data));
        assertSame(data.events.get(16).pages.first(), EventTriggers.autorunPage(state, data));
    }

    @Test
    @DisplayName("a door whose page is hidden by a switch no longer triggers")
    void hiddenDoorDoesNotTrigger() {
        MapData data = mapData();
        MapData.EventData door = event(1, 2, 2, EventTriggers.PLAYER_TOUCH);
        MapData.EventPageData hidden = new MapData.EventPageData();
        hidden.conditions = new MapData.EventConditions();
        hidden.conditions.switch1Valid = true;
        hidden.conditions.switch1Id = 9;
        hidden.graphic = null;
        door.pages.add(hidden);
        data.events.add(door);
        GameState state = new GameState();

        assertNotNull(EventTriggers.pageAt(state, data, 2, 2, EventTriggers.PLAYER_TOUCH));
        state.switches().set(9, true);
        assertNull(EventTriggers.pageAt(state, data, 2, 2, EventTriggers.PLAYER_TOUCH));
        assertEquals(-1, EventTriggers.eventIdAt(state, data, 2, 2, EventTriggers.PLAYER_TOUCH));
    }
}
