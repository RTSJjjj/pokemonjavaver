package pokemon.runtime.map;

import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.MapData;
import pokemon.runtime.event.MoveRoutePlayer;
import pokemon.runtime.state.GameState;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R6.10: the event page's movement half (RGSS {@code RPG::Event::Page}) is now
 * applied - the five option checkboxes, the autonomous move type with its speed
 * / frequency and the page's own move route. Before this every event stood
 * still facing one direction and Pokemon never bobbed in place.
 */
class EventMovementTest {

    /** One event with a character graphic and the given page movement JSON. */
    private static MapData map(String movementJson) {
        MapData data = new MapData();
        data.mapId = 7;
        data.width = 12;
        data.height = 12;
        MapData.EventData npc = new MapData.EventData();
        npc.id = 3;
        npc.x = 4;
        npc.y = 4;
        MapData.EventPageData page = new MapData.EventPageData();
        page.trigger = 0;
        page.graphic = new MapData.EventGraphic();
        page.graphic.characterName = "BW 014";
        page.graphic.direction = 2;
        page.movement = MapData.EventMovement.parse(new JsonReader().parse(movementJson));
        npc.pages.add(page);
        data.events.add(npc);
        return data;
    }

    private static EventCharacters events(MapData data) {
        GameState state = new GameState();
        state.enterMap(data.mapId, 0, 0);
        return new EventCharacters(data, new TileMap(data, null), state);
    }

    /** Steps everything the way the map screen does (collision is not the point here). */
    private static MoveRoutePlayer.Context allowAll() {
        return new MoveRoutePlayer.Context() {
            @Override
            public boolean step(MapCharacter character, int direction) {
                return character.startMove(Collision.targetX(character.x(), direction),
                        Collision.targetY(character.y(), direction), direction);
            }
        };
    }

    @Test
    @DisplayName("page checkboxes reach the runtime character")
    void pageOptionsApply() {
        MapData data = map("{\"moveType\":0,\"moveSpeed\":5,\"moveFrequency\":6,"
                + "\"through\":true,\"alwaysOnTop\":true,\"walkAnime\":false,"
                + "\"stepAnime\":true,\"directionFix\":true}");
        EventCharacters characters = events(data);
        MapCharacter npc = characters.character(3);
        assertNotNull(npc);
        assertTrue(npc.through);
        assertTrue(npc.alwaysOnTop);
        assertFalse(npc.walkAnime);
        assertTrue(npc.stepAnime);
        assertTrue(npc.directionFix);
        assertEquals(10f, npc.speed(), 0.001f, "move speed 5 = 10 tiles/s");
        assertEquals(6, npc.moveFrequency);
    }

    @Test
    @DisplayName("step anime keeps the Pokemon marching in place")
    void stepAnimeMovesThePattern() {
        MapData data = map("{\"moveType\":0,\"moveSpeed\":3,\"moveFrequency\":3,"
                + "\"stepAnime\":true}");
        EventCharacters characters = events(data);
        MapCharacter npc = characters.character(3);
        int first = npc.pattern();
        // Walking speed 4 advances the pattern every 5 frames, so check one step
        // of the cycle (40 frames would be exactly eight cycles back to 0).
        for (int frame = 0; frame < 6; frame++) {
            characters.update(1f / 40f, allowAll());
        }
        assertNotEquals(first, npc.pattern(), "the pattern keeps cycling while standing");
        assertEquals(4, npc.x(), "but it does not move: move type is fixed");
        assertEquals(4, npc.y());
    }

    @Test
    @DisplayName("a wandering event (move type 1) leaves its tile")
    void randomMovementMoves() {
        MapData data = map("{\"moveType\":1,\"moveSpeed\":4,\"moveFrequency\":6}");
        EventCharacters characters = events(data);
        MapCharacter npc = characters.character(3);
        for (int frame = 0; frame < 600 && npc.x() == 4 && npc.y() == 4; frame++) {
            characters.update(1f / 40f, allowAll());
        }
        assertTrue(npc.x() != 4 || npc.y() != 4, "a random-moving NPC wanders off its tile");
    }

    @Test
    @DisplayName("move type 2 walks toward the player")
    void towardPlayerMovement() {
        MapData data = map("{\"moveType\":2,\"moveSpeed\":4,\"moveFrequency\":6}");
        EventCharacters characters = events(data);
        MapCharacter npc = characters.character(3);
        MoveRoutePlayer.Context context = new MoveRoutePlayer.Context() {
            @Override
            public boolean step(MapCharacter character, int direction) {
                return character.startMove(Collision.targetX(character.x(), direction),
                        Collision.targetY(character.y(), direction), direction);
            }

            @Override
            public int playerX() {
                return 4;
            }

            @Override
            public int playerY() {
                return 8;
            }
        };
        int startDistance = Math.abs(npc.y() - 8);
        for (int frame = 0; frame < 400; frame++) {
            characters.update(1f / 40f, context);
        }
        assertTrue(Math.abs(npc.y() - 8) < startDistance, "the NPC closed the distance");
    }

    @Test
    @DisplayName("move type 3 repeats the page's own move route")
    void customRouteMovement() {
        MapData data = map("{\"moveType\":3,\"moveSpeed\":4,\"moveFrequency\":6,"
                + "\"moveRoute\":{\"repeat\":true,\"skippable\":true,"
                + "\"commands\":[{\"code\":1,\"parameters\":[]},{\"code\":4,\"parameters\":[]},"
                + "{\"code\":0,\"parameters\":[]}]}}");
        EventCharacters characters = events(data);
        MapCharacter npc = characters.character(3);
        boolean wentDown = false;
        boolean cameBack = false;
        for (int frame = 0; frame < 120; frame++) {
            characters.update(1f / 40f, allowAll());
            wentDown |= npc.y() == 5;
            cameBack |= wentDown && npc.y() == 4;
        }
        assertTrue(wentDown, "the first command walks one tile down");
        assertTrue(cameBack, "the route repeats and walks back up");
    }
}
