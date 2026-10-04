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
