package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.MapData;
import pokemon.runtime.data.TilesetData;
import pokemon.runtime.state.Dependent;
import pokemon.runtime.state.GameState;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 182_PField_DependentEvents:194-313 {@code pbFollowEventAcrossMaps} with the 297_Follower_Main overrides: the dependent
 * event walks onto the tile the player just left, and faces the player once the step is over.
 */
class FollowerControllerTest {

    private static TilesetData tileset() {
        TilesetData data = new TilesetData();
        data.id = 1;
        data.name = "Fake";
        data.tilesetName = "Fake";
        data.autotileNames = new String[] {"a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7"};
        data.passages = table(new int[] {0, 0});
        data.priorities = table(new int[] {0, 0});
        data.terrainTags = table(new int[] {0, 0});
        return data;
    }

    private static TilesetData.TableData table(int[] values) {
        TilesetData.TableData table = new TilesetData.TableData();
        table.present = true;
        table.z = 1;
        table.x = 2;
        table.y = 1;
        table.total = values.length;
        table.layers = new int[][] {values};
        return table;
    }

    private static MapData mapData() {
        MapData data = new MapData();
        data.mapId = 1;
        data.width = 8;
        data.height = 8;
        data.tilesetId = 1;
        MapData.TileData tileData = new MapData.TileData();
        tileData.present = true;
        tileData.z = 3;
        tileData.x = 8;
        tileData.y = 8;
        tileData.total = 8 * 8 * 3;
        tileData.layers = new int[3][64];
        for (int i = 0; i < 64; i++) {
            tileData.layers[TileMap.GROUND_LAYER][i] = 1;       // open ground
        }
        data.tileData = tileData;
        return data;
    }

    private static final FollowerController.Host HOST = new FollowerController.Host() {
        @Override
        public void playAnimation(int animationId, int x, int y) {
        }

        @Override
        public String mapName() {
            return "test";
        }

        @Override
        public boolean outdoor() {
            return true;
        }

        @Override
        public boolean encounterPossible() {
            return false;
        }

        @Override
        public int playerTerrainTag() {
            return 0;
        }

        @Override
        public boolean characterExists(String path) {
            return true;
        }

        @Override
        public void eraseEvent(int eventId) {
        }

        @Override
        public String eventGraphic(int eventId) {
            return "npc";
        }

        @Override
        public int[] eventPosition(int eventId) {
            return new int[] {3, 2, 2};
        }
    };

    @Test
    @DisplayName("the follower steps onto the tile the player left and turns toward the player")
    void followsTheLeader() {
        MapData data = mapData();
        TileMap map = new TileMap(data, tileset());
        GameState state = new GameState();
        MapCharacter player = new MapCharacter(3, 3, 8, 8, "hero");
        player.isPlayer = true;
        player.face(2);
        Dependent entry = new Dependent();
        entry.originalMap = 1;
        entry.eventId = 5;
        entry.currentMap = 1;
        entry.x = 3;
        entry.y = 2;
        entry.direction = 2;
        entry.characterName = "npc";
        entry.name = "Partner";
        state.fieldGlobals().dependents.add(entry);
        FollowerController controller = new FollowerController(state, map, data, player, HOST);
        controller.bind(map, data, true);
        MapCharacter follower = controller.characterOf(entry);
        assertNotNull(follower);
        assertEquals(3, follower.x(), "bind puts the follower behind the player");
        assertEquals(2, follower.y());

        // One step down: the follower walks to (3,3) (the player's old tile).
        assertTrue(player.startMove(3, 4, 2));
        controller.afterPlayer(0f);
        assertTrue(follower.hasStep());
        assertEquals(3, follower.logicalX());
        assertEquals(3, follower.logicalY());
        for (int i = 0; i < 60; i++) {
            controller.advance(0.05f);
            player.advance(0.05f);
            controller.afterPlayer(0.05f);
        }
        assertEquals(3, follower.x());
        assertEquals(3, follower.y());
        assertEquals(2, follower.direction(), "it faces the player (below it)");

        // The player turns right and walks: the follower takes the tile behind, to the left.
        player.face(6);
        assertTrue(player.startMove(4, 4, 6));
        controller.afterPlayer(0f);
        for (int i = 0; i < 80; i++) {
            controller.advance(0.05f);
            player.advance(0.05f);
            controller.afterPlayer(0.05f);
        }
        assertEquals(3, follower.x());
        assertEquals(4, follower.y());
    }

    @Test
    @DisplayName("pbRemoveDependenciesExceptFollower keeps only the following Pokemon")
    void removesTheOthers() {
        MapData data = mapData();
        TileMap map = new TileMap(data, tileset());
        GameState state = new GameState();
        MapCharacter player = new MapCharacter(3, 3, 8, 8, "hero");
        player.isPlayer = true;
        FollowerController controller = new FollowerController(state, map, data, player, HOST);
        Dependent partner = controller.addEvent(5, "Partner", 25);
        Dependent follower = controller.addEvent(6, Dependent.FOLLOWER_NAME, 5);
        assertNotNull(partner);
        assertNotNull(follower);
        assertEquals(2, state.fieldGlobals().dependents.size());
        assertTrue(controller.hasDependents());
        controller.removeAllButFollower();
        assertEquals(1, state.fieldGlobals().dependents.size());
        assertSame(follower, controller.follower());
        controller.removeAll();
        assertFalse(controller.hasDependents());
    }
}
