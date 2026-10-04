package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.data.MapData;
import pokemon.runtime.data.MoveRoute;
import pokemon.runtime.event.MoveRoutePlayer;
import pokemon.runtime.state.GameState;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * R6.32: the real map route context must carry the live player tile - that is
 * what "turn toward/away from the player" (25/26) and "move toward/away"
 * (10/11) read. The old anonymous context kept the interface defaults (-1), so
 * those commands silently did nothing (the same trap as R6.14, one layer
 * down).
 */
class MapRouteContextTest {

    private static MoveRoute route(int... codes) {
        MoveRoute result = new MoveRoute();
        for (int code : codes) {
            MoveRoute.Command command = new MoveRoute.Command();
            command.code = code;
            result.commands.add(command);
        }
        return result;
    }

    @Test
    @DisplayName("the route context exposes the live player tile (R6.32)")
    void carriesThePlayerTile() {
        File dataRoot = TestData.runtimeDataRoot();
        assumeTrue(dataRoot != null, "runtime data (generated/) not available");
        GameDatabase database = GameDatabase.load(dataRoot.getAbsolutePath(), dataRoot);
        try {
            MapData data = database.map(2);
            TileMap map = new TileMap(data, database.tileset(data.tilesetId));
            GameState state = new GameState();
            MapData.EventData event = data.events.get(0);
            MapCharacter player = new MapCharacter(event.x, event.y + 1,
                    map.width(), map.height(), "trchar000");
            state.enterMap(2, player.x(), player.y());
            EventCharacters characters = new EventCharacters(data, map, state);
            MapRouteContext context = new MapRouteContext(state, map, data, player,
                    (character, direction) -> false, null, message -> { });

            assertEquals(player.x(), context.playerX());
            assertEquals(player.y(), context.playerY());

            MapCharacter npc = characters.character(event.id);
            assertNotNull(npc);
            // Player one tile south: 25 faces it (down), 26 faces away (up).
            MoveRoutePlayer towards = new MoveRoutePlayer(route(25));
            towards.update(0f, npc, context);
            assertEquals(2, npc.direction());
            MoveRoutePlayer away = new MoveRoutePlayer(route(26));
            away.update(0f, npc, context);
            assertEquals(8, npc.direction());
        } finally {
            database.dispose();
        }
    }

    @Test
    @DisplayName("L6c: the player blocks a named character's step (RMXP passableEx?)")
    void playerBlocksNamedCharacters() {
        File dataRoot = TestData.runtimeDataRoot();
        assumeTrue(dataRoot != null, "runtime data (generated/) not available");
        GameDatabase database = GameDatabase.load(dataRoot.getAbsolutePath(), dataRoot);
        try {
            MapData data = database.map(2);
            TileMap map = new TileMap(data, database.tileset(data.tilesetId));
            GameState state = new GameState();
            state.enterMap(2, 1, 1);
            MapCharacter player = new MapCharacter(0, 0, map.width(), map.height(), "trchar000");
            MapRouteContext context = new MapRouteContext(state, map, data, player,
                    (character, direction) -> false, null, message -> { });

            boolean checked = false;
            for (int y = 1; y < map.height() - 1 && !checked; y++) {
                for (int x = 1; x < map.width(); x++) {
                    if (!map.passableAnyDirection(x, y) || !map.passableAnyDirection(x, y + 1)) {
                        continue;
                    }
                    if (TestData.eventAt(data, x, y) != null
                            || TestData.eventAt(data, x, y + 1) != null) {
                        continue;
                    }
                    player.teleport(x, y + 1);
                    MapCharacter npc = new MapCharacter(x, y, map.width(), map.height(), "NPC 003");
                    if (!Collision.canStep(state, map, data, npc, 2)) {
                        continue; // the raw collision must allow it to prove the block
                    }
                    assertFalse(context.step(npc, 2), "the player blocks a named character");
                    MapCharacter ghost = new MapCharacter(x, y, map.width(), map.height(), "");
                    assertTrue(context.step(ghost, 2), "a nameless character may step onto the player");
                    checked = true;
                    break;
                }
            }
            assertTrue(checked, "map 2 has a free tile pair for the player-block check");
        } finally {
            database.dispose();
        }
    }

    @Test
    @DisplayName("diagonal passability checks the two L-shaped paths (R6.32)")
    void diagonalPassability() {
        File dataRoot = TestData.runtimeDataRoot();
        assumeTrue(dataRoot != null, "runtime data (generated/) not available");
        GameDatabase database = GameDatabase.load(dataRoot.getAbsolutePath(), dataRoot);
        try {
            MapData data = database.map(2);
            TileMap map = new TileMap(data, database.tileset(data.tilesetId));
            GameState state = new GameState();
            state.enterMap(2, 1, 1);
            MapCharacter probe = new MapCharacter(1, 1, map.width(), map.height(), "");

            boolean found = false;
            for (int y = 1; y < map.height() && !found; y++) {
                for (int x = 1; x < map.width(); x++) {
                    if (!map.passableAnyDirection(x, y)
                            || !map.passableAnyDirection(x - 1, y)
                            || !map.passableAnyDirection(x, y - 1)
                            || !map.passableAnyDirection(x - 1, y - 1)) {
                        continue;
                    }
                    probe.teleport(x, y);
                    if (Collision.canStepDiagonal(state, map, data, probe, 4, 8)) {
                        found = true;
                        break;
                    }
                }
            }
            assertTrue(found, "map 2 has at least one free upper-left diagonal");

            probe.teleport(0, 0);
            assertFalse(Collision.canStepDiagonal(state, map, data, probe, 4, 8),
                    "the map corner has no diagonal");
        } finally {
            database.dispose();
        }
    }
}
