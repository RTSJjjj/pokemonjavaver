package pokemon.runtime.event;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.data.MapData;
import pokemon.runtime.data.MoveRoute;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.map.Collision;
import pokemon.runtime.map.EventCharacters;
import pokemon.runtime.map.EventTriggers;
import pokemon.runtime.map.MapCharacter;
import pokemon.runtime.map.TestData;
import pokemon.runtime.map.TileMap;
import pokemon.runtime.state.GameState;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * R6.11 end to end: the real arrival door of map 354. The hero lands on the
 * door tile, the door's autorun page (active because the named switch
 * {@code s:tsOff?("A")} is true) notices the player with
 * {@code get_character(0).onEvent?} and walks them one tile down out of the
 * doorway; the page then sets temp switch A so it never repeats that visit.
 */
class DoorArrivalTest {

    @Test
    @DisplayName("the real arrival door walks the hero out of the doorway once")
    void arrivalDoorWalksTheHeroOut() throws Exception {
        File dataRoot = TestData.runtimeDataRoot();
        assumeTrue(dataRoot != null, "runtime data (generated/) not available");

        GameDatabase database = GameDatabase.load(dataRoot.getAbsolutePath(), dataRoot);
        try {
            MapData data = database.map(354);
            TileMap map = new TileMap(data, database.tileset(data.tilesetId));
            // R6.20: anchors are discovered by shape - the author edits event
            // ids and command indices while the port is running.
            MapData.EventData door = TestData.arrivalDoor(data);
            assertNotNull(door, "map 354 carries an arrival door page");
            MapData.EventPageData arrival = TestData.arrivalPage(door);
            assertNotNull(arrival);
            assertEquals(1, door.pages.get(0).trigger, "page 1 is Player Touch");
            assertEquals(3, arrival.trigger, "the arrival page is an autorun");

            GameState state = new GameState();
            state.switchNames(database.system().switches);
            state.enterMap(354, door.x, door.y + 1);
            MapCharacter player = new MapCharacter(door.x, door.y + 1, map.width(), map.height(),
                    "trchar000");
            player.face(8);
            EventCharacters events = new EventCharacters(data, map, state);

            // The page is active before the hero arrives: tsOff?("A") is true.
            MapData.EventPageData page = EventTriggers.autorunPage(state, data);
            assertNotNull(page, "the arrival page is active on a fresh visit");
            assertSame(arrival, page, "the active autorun page is the arrival animation");
            assertTrue(page.commands.size > 5);
            assertEquals(111, page.commands.first().code, "it asks 'is the player on me?'");

            MoveRoutePlayer[] playerRoute = new MoveRoutePlayer[1];
            MoveRoutePlayer.Context context = new MoveRoutePlayer.Context() {
                @Override
                public boolean step(MapCharacter character, int direction) {
                    return Collision.canStep(state, map, data, character, direction)
                            && character.startMove(Collision.targetX(character.x(), direction),
                                    Collision.targetY(character.y(), direction), direction);
                }
            };
            MapPort port = new MapPort() {
                @Override
                public void transfer(int mapId, int x, int y, int direction) {
                }

                @Override
                public void setMoveRoute(int eventId, MoveRoute route) {
                    if (eventId < 0) {
                        playerRoute[0] = new MoveRoutePlayer(route);
                    } else {
                        events.setMoveRoute(eventId, route);
                    }
                }

                @Override
                public boolean anyRouteActive() {
                    return (playerRoute[0] != null && !playerRoute[0].finished())
                            || player.isMoving() || events.anyRouteActive();
                }

                @Override
                public boolean playerOnCharacter(int characterId) {
                    MapCharacter other = characterId < 0 ? player : events.character(characterId);
                    return other != null && player.x() == other.x() && player.y() == other.y();
                }

                @Override
                public void setTransparent(int eventId, boolean transparent) {
                    if (eventId < 0) {
                        player.opacity = transparent ? 0f : 1f;
                    }
                }
            };
            EventInterpreter interpreter = new EventInterpreter(state, new MessageService(),
                    new InputManager(), null, id -> null, port, new PictureService(), message -> { });
            // The one-shot guard is the translated setTempSwitchOn("A") script.
            File irFile = new File(dataRoot, "scripts/ir.json");
            assertTrue(irFile.isFile(), "run builder/src/script-compile.js first");
            interpreter.attachScriptIr(ScriptIr.load(irFile));

            // The hero arrives on the door tile: the map screen would start the
            // autorun page for us.
            state.setPlayerPosition(door.x, door.y);
            player.teleport(door.x, door.y);
            interpreter.start(page.commands, 354, door.id);

            float dt = 1f / 60f;
            for (int frame = 0; frame < 400; frame++) {
                if (playerRoute[0] != null) {
                    playerRoute[0].update(dt, player, context);
                    if (playerRoute[0].finished()) {
                        playerRoute[0] = null;
                    }
                }
                events.update(dt, context);
                boolean scriptDriven = playerRoute[0] != null || interpreter.running();
                if (interpreter.running()) {
                    interpreter.update(dt);
                } else if (frame > 0) {
                    // The map screen's autorun driver: run the page while it holds.
                    MapData.EventPageData again = EventTriggers.autorunPage(state, data);
                    if (again == null) {
                        break;
                    }
                    interpreter.start(again.commands, 354, door.id);
                }
                if (scriptDriven && player.hasStep()) {
                    player.advance(dt);
                }
            }

            assertEquals(door.y + 1, player.y(), "the hero walked one tile out of the doorway");
            assertEquals(door.x, player.x());
            assertTrue(state.tempSwitches().get(354, door.id, "A"),
                    "and the page switched itself off for this visit");
            assertNull(EventTriggers.autorunPage(state, data), "the door no longer autoruns");
            assertEquals(1f, player.opacity, "the hero is visible again");
        } finally {
            database.dispose();
        }
    }

}
