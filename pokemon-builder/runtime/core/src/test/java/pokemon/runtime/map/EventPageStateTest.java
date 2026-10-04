package pokemon.runtime.map;

import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.MapData;
import pokemon.runtime.data.TilesetData;
import pokemon.runtime.state.GameState;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R5 integration tests: live switches, variables and self switches decide both
 * the page the renderer shows and the collision the player feels (project3
 * sections 28-30, 61). No GL context required.
 */
class EventPageStateTest {

    private static final int RIGHT = 6;

    /** 3x3 map whose ground layer is passable tile 1; no events yet. */
    private static MapData mapData() {
        MapData data = new MapData();
        data.mapId = 1;
        data.width = 3;
        data.height = 3;
        data.tilesetId = 1;
        MapData.TileData table = new MapData.TileData();
        table.present = true;
        table.z = 3;
        table.x = 3;
        table.y = 3;
        table.total = 9 * 3;
        table.layers = new int[3][9];
        java.util.Arrays.fill(table.layers[0], 1);
        data.tileData = table;
        return data;
    }

    private static TilesetData tileset() {
        TilesetData tileset = new TilesetData();
        TilesetData.TableData passages = new TilesetData.TableData();
        passages.present = true;
        passages.z = 1;
        passages.x = 8;
        passages.y = 1;
        passages.total = 8;
        passages.layers = new int[][] { new int[8] };   // every tile passes all directions
        tileset.passages = passages;
        return tileset;                                  // priority / terrain tag default to 0
    }

    private static MapData.EventPageData page(MapData.EventConditions conditions) {
        MapData.EventPageData page = new MapData.EventPageData();
        page.conditions = conditions;
        page.graphic = new MapData.EventGraphic();
        page.graphic.characterName = "NPC";
        return page;
    }

    private static MapData.EventData blockingEvent(int id, int x, int y) {
        MapData.EventData event = new MapData.EventData();
        event.id = id;
        event.x = x;
        event.y = y;
        event.pages.add(page(null));
        return event;
    }

    private static MapData.EventConditions conditions() {
        return new MapData.EventConditions();
    }

    /** Stands left of tile (1, 1) and walks right into the event. */
    private static MapCharacter actor() {
        return new MapCharacter(0, 1, 3, 3, "trchar000");
    }

    @Test
    @DisplayName("a switch page replaces the first page only while its switch is on")
    void switchPages() {
        MapData data = mapData();
        TileMap map = new TileMap(data, tileset());
        MapData.EventData event = blockingEvent(1, 1, 1);
        MapData.EventPageData later = page(conditions());
        later.conditions.switch1Valid = true;
        later.conditions.switch1Id = 12;
        later.graphic = null;                        // blank page hides the NPC
        event.pages.add(later);
        data.events.add(event);

        GameState state = new GameState();
        state.enterMap(data.mapId, 0, 1);
        assertSame(event.pages.first(), EventPages.resolve(state, data.mapId, event));
        assertFalse(Collision.canStep(state, map, data, actor(), RIGHT));

        state.switches().set(12, true);
        assertSame(later, EventPages.resolve(state, data.mapId, event));
        assertTrue(Collision.canStep(state, map, data, actor(), RIGHT));

        state.switches().set(12, false);
        assertSame(event.pages.first(), EventPages.resolve(state, data.mapId, event));
        assertFalse(Collision.canStep(state, map, data, actor(), RIGHT));
    }

    @Test
    @DisplayName("variable pages activate on >=, so a zero threshold is met immediately")
    void variablePages() {
        MapData data = mapData();
        MapData.EventData event = blockingEvent(1, 1, 1);
        MapData.EventPageData later = page(conditions());
        later.conditions.variableValid = true;
        later.conditions.variableId = 45;
        later.conditions.variableValue = 3;
        event.pages.add(later);
        data.events.add(event);

        GameState state = new GameState();
        state.variables().set(45, 2);
        assertSame(event.pages.first(), EventPages.resolve(state, data.mapId, event));
        state.variables().set(45, 3);
        assertSame(later, EventPages.resolve(state, data.mapId, event));

        later.conditions.variableValue = 0;          // "variable 45 >= 0" holds even at zero
        state.variables().set(45, 0);
        assertSame(later, EventPages.resolve(state, data.mapId, event));
    }

    @Test
    @DisplayName("self switch pages are per map, event and channel")
    void selfSwitchPages() {
        MapData data = mapData();
        MapData.EventData event = blockingEvent(1, 1, 1);
        MapData.EventPageData later = page(conditions());
        later.conditions.selfSwitchValid = true;
        later.conditions.selfSwitchCh = "A";
        event.pages.add(later);
        data.events.add(event);
        MapData.EventData other = blockingEvent(2, 2, 1);
        data.events.add(other);

        GameState state = new GameState();
        assertSame(event.pages.first(), EventPages.resolve(state, data.mapId, event));
        state.selfSwitches().set(data.mapId, 1, "A", true);
        assertSame(later, EventPages.resolve(state, data.mapId, event));
        assertSame(other.pages.first(), EventPages.resolve(state, data.mapId, other));
        state.selfSwitches().set(data.mapId, 1, "B", true);   // other channel
        assertSame(later, EventPages.resolve(state, data.mapId, event));

        GameState otherMap = new GameState();
        otherMap.selfSwitches().set(9, 1, "A", true);         // same event id, other map
        assertSame(event.pages.first(), EventPages.resolve(otherMap, data.mapId, event));
    }

    @Test
    @DisplayName("a through page from live state opens the tile for the player")
    void throughPageFromState() {
        MapData data = mapData();
        TileMap map = new TileMap(data, tileset());
        MapData.EventData event = blockingEvent(1, 1, 1);
        MapData.EventPageData later = page(conditions());
        later.conditions.switch1Valid = true;
        later.conditions.switch1Id = 3;
        later.movement.through = true;
        event.pages.add(later);
        data.events.add(event);

        GameState state = new GameState();
        assertFalse(Collision.canStep(state, map, data, actor(), RIGHT));
        state.switches().set(3, true);
        assertTrue(Collision.canStep(state, map, data, actor(), RIGHT));
    }

    @Test
    @DisplayName("the resolved page is recorded in the temporary event state")
    void resolvedPageIsRecorded() {
        MapData data = mapData();
        MapData.EventData event = blockingEvent(7, 1, 1);
        data.events.add(event);
        GameState state = new GameState();
        assertEquals(0, state.temporary().size());

        EventPages.resolve(state, data.mapId, event);
        assertEquals(1, state.temporary().size());
        assertEquals(0, state.temporary().find(data.mapId, 7).pageIndex());
        assertEquals(0, state.temporary().of(data.mapId, 7).pageIndex());
    }
}
