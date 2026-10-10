package pokemon.runtime.map;

import org.junit.jupiter.api.Test;
import pokemon.runtime.data.MapData;
import pokemon.runtime.state.GameState;

import static org.junit.jupiter.api.Assertions.*;

/** Event positions: a new screen starts from the parsed map, a load puts the events where the save had them. */
class EventPositionsTest {

    private static MapData map() {
        MapData map = new MapData();
        map.mapId = 40;
        map.width = 10;
        map.height = 10;
        for (int i = 0; i < 2; i++) {
            MapData.EventData event = new MapData.EventData();
            event.id = i + 1;
            event.name = "NPC";
            event.x = 2 + i;
            event.y = 3;
            MapData.EventPageData page = new MapData.EventPageData();
            page.graphic = new MapData.EventGraphic();
            page.graphic.characterName = "npc";
            page.graphic.direction = 2;
            page.graphic.opacity = 255;
            event.pages.add(page);
            map.events.add(event);
        }
        return map;
    }

    @Test
    void aNewScreenStartsFromTheParsedEvents() {
        MapData data = map();
        GameState state = new GameState();
        EventCharacters first = new EventCharacters(data, new TileMap(data, null), state);
        MapCharacter npc = first.character(1);
        npc.teleport(7, 8);
        npc.face(6);
        first.positions();                                       // a save in between does not change anything
        data.events.get(0).x = 7;                                // what sync() writes back
        data.events.get(0).y = 8;
        data.events.get(0).pages.get(0).graphic.direction = 6;

        EventCharacters second = new EventCharacters(data, new TileMap(data, null), state);
        assertEquals(2, second.character(1).x());
        assertEquals(3, second.character(1).y());
        assertEquals(2, second.character(1).direction());
    }

    @Test
    void aLoadPutsTheEventsWhereTheSaveHadThem() {
        MapData data = map();
        GameState state = new GameState();
        EventCharacters playing = new EventCharacters(data, new TileMap(data, null), state);
        playing.character(2).teleport(5, 6);
        playing.character(2).face(4);
        int[] saved = playing.positions();
        assertEquals(40, saved[0]);

        EventCharacters loaded = new EventCharacters(data, new TileMap(data, null), state);
        assertEquals(3, loaded.character(2).x(), "a fresh screen is back at the parsed place");
        assertTrue(loaded.restorePositions(saved));
        assertEquals(5, loaded.character(2).x());
        assertEquals(6, loaded.character(2).y());
        assertEquals(4, loaded.character(2).direction());
        assertEquals(2, loaded.character(1).x(), "an event that did not move stays");
    }

    @Test
    void aSaveOfAnotherMapIsNotApplied() {
        MapData data = map();
        EventCharacters characters = new EventCharacters(data, new TileMap(data, null), new GameState());
        assertFalse(characters.restorePositions(new int[] {99, 1, 9, 9, 2}));
        assertEquals(2, characters.character(1).x());
    }
}
