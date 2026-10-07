package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.MapData;
import pokemon.runtime.data.TilesetData;
import pokemon.runtime.state.GameState;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Game_Player#move_generic:74-75 + PField_Field#pbLedge:1135-1145: a ledge the
 * player can step onto from its passable side is jumped over (two tiles), never
 * walked on; a blocked landing consumes the step without a jump.
 */
class LedgeJumpTest {

    private final GameState state = new GameState();

    private MapData data() {
        MapData d = new MapData();
        d.width = 20;
        d.height = 4;
        d.tileData = new MapData.TileData();
        d.tileData.layers = new int[3][80];
        java.util.Arrays.fill(d.tileData.layers[0], 384);
        return d;
    }

    private TilesetData tiles() {
        TilesetData t = new TilesetData();
        t.passages = table();
        t.priorities = table();
        t.terrainTags = table();
        return t;
    }

    private TilesetData.TableData table() {
        TilesetData.TableData t = new TilesetData.TableData();
        t.layers = new int[][] {new int[400]};
        return t;
    }

    /** A ledge tile at (3,1), the player walking right from (2,1). */
    private TileMap ledgeMap(MapData d, TilesetData t) {
        d.tileData.layers[0][3 + 1 * 20] = 385;
        t.terrainTags.layers[0][385] = 1; // PBTerrain::Ledge
        return new TileMap(d, t);
    }

    @Test
    @DisplayName("facing a passable ledge jumps two tiles over it (PBTerrain::Ledge=1)")
    void ledgeJumpLanding() {
        MapData d = data();
        TilesetData t = tiles();
        TileMap map = ledgeMap(d, t);
        MapCharacter player = new MapCharacter(2, 1, 20, 4, "trchar000");

        assertTrue(Collision.isFacingLedge(state, map, d, player, 6),
                "the ledge is passable from this side (Game_Player:74)");
        assertArrayEquals(new int[] {4, 1},
                Collision.ledgeLanding(state, map, d, player, 6),
                "pbJumpToward(2) lands two tiles over the ledge");
    }

    @Test
    @DisplayName("a blocked landing consumes the step without a jump (pbLedge returns true)")
    void blockedLanding() {
        MapData d = data();
        TilesetData t = tiles();
        TileMap map = ledgeMap(d, t);
        d.tileData.layers[0][4 + 1 * 20] = 386;
        t.passages.layers[0][386] = 15; // tile (4,1) blocks every direction
        MapCharacter player = new MapCharacter(2, 1, 20, 4, "trchar000");

        assertTrue(Collision.isFacingLedge(state, map, d, player, 6));
        assertNull(Collision.ledgeLanding(state, map, d, player, 6),
                "the landing is blocked, so pbJumpToward fails");
    }

    @Test
    @DisplayName("the wrong side of a ledge is a normal blocked step (bump)")
    void wrongSideIsNotALedgeJump() {
        MapData d = data();
        TilesetData t = tiles();
        TileMap map = ledgeMap(d, t);
        t.passages.layers[0][385] = 0x02; // entering from the left is blocked
        MapCharacter player = new MapCharacter(2, 1, 20, 4, "trchar000");

        assertFalse(Collision.canStep(state, map, d, player, 6));
        assertFalse(Collision.isFacingLedge(state, map, d, player, 6),
                "passable? is the plugin's gate for pbLedge (Game_Player:74)");
    }
}
