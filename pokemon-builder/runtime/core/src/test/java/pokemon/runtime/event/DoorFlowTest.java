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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * R6.8 regression for two reported bugs on the real Map002 door:
 *
 * <ol>
 *   <li>the hero never walked into the doorway. The door page forces a route on
 *       the player (through ON, move up, through OFF) and waits with 210; the
 *       screen never advanced the started step, so the step hung forever and
 *       the player stayed frozen afterwards;</li>
 *   <li>210 must wait for the player's route as well (RMXP has no target
 *       parameter), otherwise the door transfers before the hero is inside.</li>
 * </ol>
 *
 * <p>The frame loop mirrors {@code MapScreen}: routes advance, a started step
 * always lands, and a transfer request ends the run. Skipped when the Builder
 * output is missing.</p>
 */
class DoorFlowTest {

    @Test
    @DisplayName("the real door walks the hero into the doorway before transferring")
    void doorWalksHeroIn() {
        File dataRoot = TestData.runtimeDataRoot();
        assumeTrue(dataRoot != null, "runtime data (generated/) not available");

        GameDatabase database = GameDatabase.load(dataRoot.getAbsolutePath(), dataRoot);
        try {
            MapData data = database.map(2);
            TileMap map = new TileMap(data, database.tileset(data.tilesetId));
            // R6.20: discover a scripted door (hides the hero, then transfers
            // through a fade) instead of a fixed event id.
            MapData.EventData door = TestData.eventWithDoorAnimation(data,
                    EventTriggers.PLAYER_TOUCH, true);
            assertNotNull(door, "map 2 carries a scripted player-touch door");

            GameState state = new GameState();
            state.enterMap(2, door.x, door.y + 1);
            MapCharacter player = new MapCharacter(door.x, door.y + 1, map.width(), map.height(),
                    "trchar000");
            player.face(8);
            EventCharacters events = new EventCharacters(data, map, state);

            List<String> sounds = new ArrayList<>();
            List<Boolean> playerHidden = new ArrayList<>();
            int[] transferred = null;

            MoveRoutePlayer.Context context = new MoveRoutePlayer.Context() {
                @Override
                public boolean step(MapCharacter character, int direction) {
                    return Collision.canStep(state, map, data, character, direction)
                            && character.startMove(Collision.targetX(character.x(), direction),
                                    Collision.targetY(character.y(), direction), direction);
                }

                @Override
                public void playSe(String name) {
                    sounds.add(name);
                }
            };

            MapPort port = new MapPort() {
                @Override
                public void transfer(int mapId, int x, int y, int direction) {
                }

                @Override
                public void setMoveRoute(int eventId, MoveRoute route) {
                    if (eventId < 0) {
                        playerRoute = new MoveRoutePlayer(route);
                    } else {
                        events.setMoveRoute(eventId, route);
                    }
                }

                @Override
                public boolean isMoving(int eventId) {
                    return eventId < 0 ? playerRoute != null && !playerRoute.finished()
                            : events.isMoving(eventId);
                }

                @Override
                public boolean anyRouteActive() {
                    return (playerRoute != null && !playerRoute.finished()) || player.isMoving()
                            || events.anyRouteActive();
                }

                @Override
                public void setTransparent(int eventId, boolean transparent) {
                    if (eventId < 0) {
                        playerHidden.add(transparent);
                    } else {
                        events.setTransparent(eventId, transparent);
                    }
                }

                @Override
                public void transfer(int mapId, int x, int y, int direction, int fade) {
                    transferRequest = new int[] {player.x(), player.y(), mapId, x, y, fade};
                }
            };

            EventInterpreter interpreter = new EventInterpreter(state, new MessageService(),
                    new InputManager(), null, id -> null, port, new PictureService(), message -> { });
            MapData.EventPageData page =
                    EventTriggers.pageAt(state, data, door.x, door.y, EventTriggers.PLAYER_TOUCH);
            assertNotNull(page, "the door answers the player-touch trigger");
            interpreter.start(page.commands, 2, door.id);

            float dt = 1f / 60f;
            for (int frame = 0; frame < 600 && transferRequest == null; frame++) {
                if (playerRoute != null) {
                    playerRoute.update(dt, player, context);
                    if (playerRoute.finished()) {
                        playerRoute = null;
                    }
                }
                events.update(dt, context);
                boolean scriptDriven = playerRoute != null || interpreter.running();
                if (interpreter.running()) {
                    interpreter.update(dt);
                }
                if (scriptDriven && player.hasStep()) {
                    player.advance(dt);
                }
            }

            assertNotNull(transferRequest, "the door must ask for a transfer");
            assertEquals(door.x, player.x(), "the hero stands inside the doorway");
            assertEquals(door.y, player.y(), "the hero stands inside the doorway");
            assertEquals(door.x, transferRequest[0], "210 waited for the hero's step");
            assertEquals(door.y, transferRequest[1], "210 waited for the hero's step");
            assertEquals(3, transferRequest[2], "and only then transfers to map 3");
            assertEquals(1, transferRequest[5], "with the fade option");
            assertTrue(sounds.contains("Door enter"), sounds.toString());
            assertEquals(List.of(true, false), playerHidden, "the hero is hidden and shown again");
        } finally {
            database.dispose();
        }
    }

    private MoveRoutePlayer playerRoute;
    private int[] transferRequest;

    @Test
    @DisplayName("arrival doors are autorun pages, so landing on one must not fire touch")
    void arrivalDoorsUseAutorunPages() {
        File dataRoot = TestData.runtimeDataRoot();
        assumeTrue(dataRoot != null, "runtime data (generated/) not available");

        GameDatabase database = GameDatabase.load(dataRoot.getAbsolutePath(), dataRoot);
        try {
            // Map353's stairs door sends the hero to map354 (10,4); that tile is
            // the destination door itself: page 1 is Player Touch (leaving the
            // room) and page 2 is the autorun arrival animation. Firing the touch
            // page on arrival is what bounced the hero straight back (R6.9).
            MapData source = database.map(353);
            // R6.20: instead of the fixed id, discover a door whose landing tile
            // carries an arrival door (that is the shape this regression is
            // about).
            MapData.EventData exit = null;
            MapData.EventData door = null;
            for (MapData.EventData candidate : source.events) {
                int[] target = TestData.transferTarget(candidate);
                if (target == null) {
                    continue;
                }
                MapData targetMap = database.map(target[0]);
                MapData.EventData atTarget = TestData.eventAt(targetMap, target[1], target[2]);
                if (atTarget != null && TestData.arrivalPage(atTarget) != null) {
                    exit = candidate;
                    door = atTarget;
                    break;
                }
            }
            assertNotNull(exit, "map 353 has a door that lands on an arrival door");
            MapData.EventPageData arrival = TestData.arrivalPage(door);
            assertNotNull(arrival);
            assertEquals(1, door.pages.get(0).trigger, "page 1 is Player Touch");
            assertEquals(3, arrival.trigger, "the arrival page is an autorun");
            assertEquals(22, arrival.conditions.switch1Id,
                    "the arrival page waits on the named s:tsOff?(\"A\") switch");
            assertTrue(arrival.conditions.switch1Valid);
        } finally {
            database.dispose();
        }
    }

}
