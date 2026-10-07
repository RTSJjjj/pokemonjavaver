package pokemon.runtime.map;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.data.MapData;
import pokemon.runtime.data.MoveRoute;
import pokemon.runtime.event.EventInterpreter;
import pokemon.runtime.event.MapPort;
import pokemon.runtime.event.MessageService;
import pokemon.runtime.event.MoveRoutePlayer;
import pokemon.runtime.event.PictureService;
import pokemon.runtime.event.ScriptIr;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.state.GameState;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Regression for the reported map36/event6 stall: page 2 transfers the hero to
 * (64,17) and then branches on {@code $game_player.y} to pick one of three move
 * routes for the NPC (event 3). The conditional branch type 12 used to fall
 * through to {@code unsupported()} - every {@code $game_player.y==N} was false,
 * so the page always took the else route, which walks the NPC across the tile
 * the hero occupies.
 *
 * <p>The test drives the real page with a MapPort that records the route side
 * (movement itself is covered by {@link MapRouteContextTest}) and keeps the
 * hero at the given row. Skipped when the Builder output is missing.</p>
 */
class RealCutsceneBranchTest {

    private static final float FRAME = 1f / 40f;
    private static final int MAX_FRAMES = 4000;
    /** The NPC the page gives its conditional routes to (209 target = event 3). */
    private static final int NPC_EVENT_ID = 3;

    @Test
    @DisplayName("hero at y=16 takes the '向下' route, hero at y=17 takes the '横穿' route")
    void conditionalBranchPicksTheRouteForTheHeroRow() {
        File dataRoot = TestData.runtimeDataRoot();
        assumeTrue(dataRoot != null, "runtime data (generated/) not available");

        GameDatabase database = GameDatabase.load(dataRoot.getAbsolutePath(), dataRoot);
        try {
            MapData data = database.map(36);
            MapData.EventData event = pageWithPlayerRowBranch(data);
            assertNotNull(event, "map 36 carries the $game_player.y branch page");
            MapData.EventPageData page = pageWithPlayerRowBranchPage(data, event);

            // map36 event 6 stands at (64,15) and its page 2 moves the NPC to
            // (66,17). With the hero one row above, the first branch matches and
            // the route starts by stepping down (37 through, 39 on top, 1 down).
            List<Integer> above = lastNpcRoute(data, event.id, page, 16);
            assertEquals(37, above.get(0), "the route turns through on first");
            assertEquals(39, above.get(1), "then always-on-top");
            assertEquals(1, above.get(2), "hero at y=16 -> the route steps down first");

            // Hero level with the NPC: the else route walks left across the
            // hero's tile (through on, no down step) - the shape that bumped.
            List<Integer> level = lastNpcRoute(data, event.id, page, 17);
            assertEquals(37, level.get(0), "the route turns through on first");
            assertEquals(2, level.get(1), "hero at y=17 -> the route steps left");
            assertTrue(level.contains(38) && level.contains(40),
                    "and it turns through / always-on-top back off at the end: " + level);
        } finally {
            database.dispose();
        }
    }

    @Test
    @DisplayName("the level route walks the NPC across the hero's tile instead of stalling")
    void npcRouteWalksOverTheHero() {
        File dataRoot = TestData.runtimeDataRoot();
        assumeTrue(dataRoot != null, "runtime data (generated/) not available");

        GameDatabase database = GameDatabase.load(dataRoot.getAbsolutePath(), dataRoot);
        try {
            MapData data = database.map(36);
            MapData.EventData event = pageWithPlayerRowBranch(data);
            assertNotNull(event, "map 36 carries the $game_player.y branch page");
            MapData.EventPageData page = pageWithPlayerRowBranchPage(data, event);
            MoveRoute route = routeStartingWith(page, 37, 2);
            assertNotNull(route, "the else branch gives the NPC the 'walk left' route");

            TileMap map = new TileMap(data, database.tileset(data.tilesetId));
            GameState state = new GameState();
            state.enterMap(data.mapId, 64, 17);
            MapCharacter player = new MapCharacter(64, 17, map.width(), map.height(), "trchar000");
            player.isPlayer = true;
            // The page's 202 put the NPC at (66,17); the route walks it left.
            MapCharacter npc = new MapCharacter(66, 17, map.width(), map.height(), "NPC 003");
            MapRouteContext context = new MapRouteContext(state, map, data, player,
                    (character, direction) -> false, null, message -> { });

            MoveRoutePlayer runner = new MoveRoutePlayer(route);
            int frames = 0;
            while (!runner.finished() && frames < 3000) {
                runner.update(FRAME, npc, context);
                npc.advance(FRAME);
                frames++;
            }
            assertTrue(runner.finished(),
                    "the route must finish even though it crosses the hero (" + frames + " frames)");
            assertEquals(64, player.x());
            assertEquals(17, player.y());
            assertEquals(51, npc.x(), "66 minus the route's 15 left steps");
            assertEquals(17, npc.y());
            assertFalse(npc.through, "the route turns through back off (code 38)");
            assertFalse(npc.alwaysOnTop, "and always-on-top off (code 40)");
        } finally {
            database.dispose();
        }
    }

    /** First move route in the page that starts with the given codes. */
    private static MoveRoute routeStartingWith(MapData.EventPageData page, int... codes) {
        for (int i = 0; i < page.commands.size; i++) {
            EventCommand command = page.commands.get(i);
            if (command == null || command.code != 209 || command.parameters == null) {
                continue;
            }
            com.badlogic.gdx.utils.JsonValue list = command.parameters.get(1);
            if (list == null || list.get("list") == null) {
                continue;
            }
            com.badlogic.gdx.utils.JsonValue entries = list.get("list");
            if (entries.size < codes.length) {
                continue;
            }
            boolean matches = true;
            for (int c = 0; c < codes.length; c++) {
                matches &= entries.get(c).getInt("code", -1) == codes[c];
            }
            if (matches) {
                return MoveRoute.parse(list);
            }
        }
        return null;
    }

    /**
     * Runs the page with the hero pinned to {@code heroY} and returns the last
     * move route (code list) the page gave the NPC.
     */
    private List<Integer> lastNpcRoute(MapData data, int eventId,
                                       MapData.EventPageData page, int heroY) {
        GameState state = new GameState();
        state.enterMap(data.mapId, 64, heroY);
        MessageService messages = new MessageService();
        InputManager input = new InputManager();
        List<List<Integer>> npcRoutes = new ArrayList<>();
        boolean[] transferring = new boolean[1];
        MapPort port = new MapPort() {
            @Override
            public void transfer(int mapId, int x, int y, int direction) {
                transferring[0] = true;
            }

            @Override
            public void transfer(int mapId, int x, int y, int direction, int fade) {
                transferring[0] = true;
            }

            @Override
            public void setMoveRoute(int eventId, MoveRoute route) {
                if (eventId != NPC_EVENT_ID) {
                    return;
                }
                List<Integer> codes = new ArrayList<>();
                for (int i = 0; i < route.size(); i++) {
                    codes.add(route.commands.get(i).code);
                }
                npcRoutes.add(codes);
            }

            @Override
            public boolean isMoving(int eventId) {
                return false; // the route playback is not part of this test
            }
        };
        EventInterpreter interpreter = new EventInterpreter(state, messages, input, null,
                id -> null, port, new PictureService(), message -> { });
        interpreter.attachScriptIr(ScriptIr.empty());
        interpreter.start(page.commands, data.mapId, eventId);

        int frames = 0;
        while (interpreter.running() && frames < MAX_FRAMES) {
            if (messages.visible()) {
                input.beginFrame();
                input.press(GameAction.CONFIRM);
                interpreter.update(0f);
                input.endFrame();
                input.release(GameAction.CONFIRM);
            } else {
                interpreter.update(FRAME);
            }
            if (transferring[0]) {
                transferring[0] = false;
                interpreter.resumeAfterTransfer(); // the hero stays where he is
            }
            frames++;
        }
        assertFalse(interpreter.running(),
                "the page must finish for hero y=" + heroY + " (" + frames + " frames)");
        assertFalse(npcRoutes.isEmpty(), "the page gave the NPC a route");
        return npcRoutes.get(npcRoutes.size() - 1);
    }

    /** First trigger-2 page whose conditional branch asks for {@code $game_player.y}. */
    private static MapData.EventData pageWithPlayerRowBranch(MapData data) {
        for (MapData.EventData event : data.events) {
            if (pageWithPlayerRowBranchPage(data, event) != null) {
                return event;
            }
        }
        return null;
    }

    private static MapData.EventPageData pageWithPlayerRowBranchPage(MapData data,
                                                                    MapData.EventData event) {
        for (MapData.EventPageData page : event.pages) {
            Array<EventCommand> commands = page.commands;
            for (int i = 0; i < commands.size; i++) {
                EventCommand command = commands.get(i);
                if (command != null && command.code == 111 && command.parameters != null
                        && command.parameters.get(0).asInt() == 12
                        && command.parameters.get(1).asString().contains("$game_player.y")) {
                    return page;
                }
            }
        }
        return null;
    }
}
