package pokemon.runtime.map;

import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.MapData;
import pokemon.runtime.data.MoveRoute;
import pokemon.runtime.event.MoveRoutePlayer;
import pokemon.runtime.state.GameState;

import static org.junit.jupiter.api.Assertions.*;

/** R6.3b-2: runtime event characters back Set Event Location / route / transparency. */
class EventCharactersTest {

    private static MapData mapData() {
        MapData data = new MapData();
        data.mapId = 4;
        data.width = 8;
        data.height = 8;
        MapData.EventData npc = new MapData.EventData();
        npc.id = 2;
        npc.x = 3;
        npc.y = 4;
        MapData.EventPageData page = new MapData.EventPageData();
        page.trigger = 0;
        page.graphic = new MapData.EventGraphic();
        page.graphic.characterName = "NPC 002";
        page.graphic.direction = 2;
        page.graphic.pattern = 3; // doors keep their image in the 4th column
        npc.pages.add(page);
        data.events.add(npc);

        MapData.EventData sign = new MapData.EventData();
        sign.id = 5;
        sign.x = 1;
        sign.y = 1;
        MapData.EventPageData signPage = new MapData.EventPageData();
        signPage.graphic = new MapData.EventGraphic();
        signPage.graphic.tileId = 385;
        sign.pages.add(signPage);
        data.events.add(sign);
        return data;
    }

    @Test
    @DisplayName("Erase Event drops the pages, sheet and blocking for this map visit (116)")
    void eraseEvent() {
        MapData data = mapData();
        GameState state = new GameState();
        state.enterMap(data.mapId, 0, 0);
        EventCharacters events = new EventCharacters(data, new TileMap(data, null), state);
        long version = state.version();

        events.erase(2);

        assertNull(EventPages.resolve(state, data.mapId, data.events.get(0)),
                "no page resolves after erase");
        MapCharacter npc = events.character(2);
        assertNull(npc.characterName, "the sprite is gone");
        assertTrue(npc.through, "erased events no longer block");
        assertNotEquals(version, state.version(), "the page caches are told to re-resolve");
        assertNotNull(EventPages.resolve(state, data.mapId, data.events.get(1)),
                "other events keep their pages");
    }

    @Test
    @DisplayName("every event gets a runtime character; tile pages keep no sheet (R6.26)")
    void createsRuntimeCharacters() {
        MapData data = mapData();
        GameState state = new GameState();
        state.enterMap(data.mapId, 0, 0);
        EventCharacters events = new EventCharacters(data, new TileMap(data, null), state);

        MapCharacter npc = events.character(2);
        assertNotNull(npc);
        assertEquals(3, npc.x());
        assertEquals(4, npc.y());
        assertEquals(2, npc.direction());
        assertEquals(3, npc.pattern(), "the page's pattern column is the standing frame");

        // R6.26 (all events own a character, like RGSS's Game_Event): the
        // tile-graphic sign can be moved / hidden / re-sheeted by a route, it
        // just has no character sheet while its page shows a tile.
        MapCharacter sign = events.character(5);
        assertNotNull(sign);
        assertNull(sign.characterName);
        assertEquals(1, sign.x());
        assertNull(events.character(99));
    }

    @Test
    @DisplayName("a tile event moved by 202 writes its live position back to MapData (R6.26)")
    void tileEventFollowsSetLocation() {
        MapData data = mapData();
        GameState state = new GameState();
        state.enterMap(data.mapId, 0, 0);
        EventCharacters events = new EventCharacters(data, new TileMap(data, null), state);

        events.setLocation(5, 6, 2, 0);
        events.update(0f, new MoveRoutePlayer.Context() {
            @Override
            public boolean step(MapCharacter character, int direction) {
                return false;
            }
        });

        MapCharacter sign = events.character(5);
        assertEquals(6, sign.x());
        assertEquals(2, sign.y());
        // The static renderer and collision read MapData, so the move has to be
        // visible there too.
        assertEquals(6, data.events.get(1).x);
        assertEquals(2, data.events.get(1).y);
    }

    @Test
    @DisplayName("a page without a character sheet drops the stale sprite (R6.21)")
    void pageWithoutGraphicDropsTheSprite() {
        MapData data = mapData();
        MapData.EventData npc = data.events.first();
        // A second page (self switch A on) without any character sheet - the
        // "taken item ball" / "finished NPC" shape this project uses.
        MapData.EventPageData done = new MapData.EventPageData();
        done.trigger = 0;
        done.conditions = new MapData.EventConditions();
        done.conditions.selfSwitchCh = "A";
        done.conditions.selfSwitchValid = true;
        done.graphic = new MapData.EventGraphic();
        npc.pages.add(done);

        GameState state = new GameState();
        state.enterMap(data.mapId, 0, 0);
        EventCharacters events = new EventCharacters(data, new TileMap(data, null), state);
        MapCharacter character = events.character(2);
        assertNotNull(character);
        assertEquals("NPC 002", character.characterName);

        state.selfSwitches().set(data.mapId, 2, "A", true);
        events.refreshGraphics();

        assertSame(done, EventPages.resolve(state, data.mapId, npc),
                "the blank page is active now");
        assertNull(character.characterName,
                "the new page has no sheet, so the stale sprite must go");

        state.selfSwitches().set(data.mapId, 2, "A", false);
        events.refreshGraphics();
        assertEquals("NPC 002", character.characterName,
                "switching back restores the page's sheet");
    }

    @Test
    @DisplayName("set location, transparency and route playback are reflected")
    void locationTransparencyAndRoutes() {
        MapData data = mapData();
        GameState state = new GameState();
        state.enterMap(data.mapId, 0, 0);
        EventCharacters events = new EventCharacters(data, new TileMap(data, null), state);

        events.setLocation(2, 5, 6, 4);
        assertEquals(5, events.character(2).x());
        assertEquals(6, events.character(2).y());
        assertEquals(4, events.character(2).direction());

        assertFalse(events.transparent(2));
        events.setTransparent(2, true);
        assertTrue(events.transparent(2));

        MoveRoute route = MoveRoute.parse(new JsonReader().parse(
                "{\"list\":[{\"code\":17},{\"code\":0}],\"skippable\":true}"));
        events.setMoveRoute(2, route);
        assertTrue(events.isMoving(2));
        events.update(0f, new MoveRoutePlayer.Context() {
            @Override
            public boolean step(MapCharacter character, int direction) {
                return false;
            }
        });
        assertFalse(events.isMoving(2));
        assertEquals(4, events.character(2).direction());
    }

    @Test
    @DisplayName("a page change swaps the character sheet without moving the NPC")
    void refreshGraphicsFollowsPages() {
        MapData data = mapData();
        MapData.EventData npc = data.events.get(0);
        MapData.EventPageData later = new MapData.EventPageData();
        later.conditions = new MapData.EventConditions();
        later.conditions.switch1Valid = true;
        later.conditions.switch1Id = 4;
        later.graphic = new MapData.EventGraphic();
        later.graphic.characterName = "NPC 099";
        later.graphic.direction = 6;
        npc.pages.add(later);

        GameState state = new GameState();
        state.enterMap(data.mapId, 0, 0);
        EventCharacters events = new EventCharacters(data, new TileMap(data, null), state);
        events.setLocation(2, 5, 6, 2);
        assertEquals("NPC 002", events.character(2).characterName);

        state.switches().set(4, true);
        events.refreshGraphics();
        assertEquals("NPC 099", events.character(2).characterName);
        assertEquals(6, events.character(2).direction());
        assertEquals(5, events.character(2).x());
        assertEquals(6, events.character(2).y());
    }

    @Test
    @DisplayName("a running route's through / always-on-top survive a refresh of an unchanged page")
    void routeFlagsSurviveUnchangedPageRefresh() {
        MapData data = mapData();
        GameState state = new GameState();
        state.enterMap(data.mapId, 0, 0);
        EventCharacters events = new EventCharacters(data, new TileMap(data, null), state);
        MapCharacter npc = events.character(2);

        MoveRoute route = MoveRoute.parse(new JsonReader().parse(
                "{\"list\":[{\"code\":37},{\"code\":39},{\"code\":0}],\"skippable\":true}"));
        events.setMoveRoute(2, route);
        events.update(0f, new MoveRoutePlayer.Context() {
            @Override
            public boolean step(MapCharacter character, int direction) {
                return false;
            }
        });
        assertTrue(npc.through, "code 37 turns through on");
        assertTrue(npc.alwaysOnTop, "code 39 turns always-on-top on");

        // A parallel event elsewhere flips a switch: MapScreen refreshes the
        // pages, but this event's page object is unchanged, so RGSS
        // Game_Event#refresh returns early and the route's flags must stay.
        state.switches().set(3, true);
        events.refreshGraphics();
        assertTrue(npc.through, "through must survive the refresh");
        assertTrue(npc.alwaysOnTop, "always on top must survive the refresh");
    }

    @Test
    @DisplayName("an event move route actually walks the NPC")
    void npcRouteMovesTheCharacter() {
        MapData data = mapData();
        GameState state = new GameState();
        state.enterMap(data.mapId, 0, 0);
        EventCharacters events = new EventCharacters(data, new TileMap(data, null), state);
        MoveRoute route = MoveRoute.parse(new JsonReader().parse(
                "{\"list\":[{\"code\":1},{\"code\":0}],\"skippable\":true}"));
        events.setMoveRoute(2, route);

        MoveRoutePlayer.Context context = new MoveRoutePlayer.Context() {
            @Override
            public boolean step(MapCharacter character, int direction) {
                return character.startMove(Collision.targetX(character.x(), direction),
                        Collision.targetY(character.y(), direction), direction);
            }
        };
        for (int frame = 0; frame < 40; frame++) {
            events.update(1f / 60f, context);
        }
        // R6.9: the route used to start the step and never advance it, so NPCs
        // stayed frozen on the first tile of every move route.
        assertEquals(5, events.character(2).y(), "the NPC walked one tile down");
        assertEquals(3, events.character(2).x());
        assertFalse(events.isMoving(2), "and the route finished");
    }
}
