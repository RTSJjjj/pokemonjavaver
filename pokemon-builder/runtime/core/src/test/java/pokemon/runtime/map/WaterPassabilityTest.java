package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.MapData;
import pokemon.runtime.data.TilesetData;
import pokemon.runtime.field.PBTerrain;
import pokemon.runtime.state.GameState;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 027_Game_Map:163-221 passable? for an event on water (a swimmer's sight line crosses the sea), and MapFactory:172-174
 * for a surfing player stepping onto a connected map's water.
 */
class WaterPassabilityTest {
    private static final int GROUND = 1, WATER = 2;

    private static TilesetData tileset() {
        TilesetData data = new TilesetData();
        data.id = 1;
        data.name = "Fake";
        data.tilesetName = "Fake";
        data.autotileNames = new String[] {"a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7"};
        data.passages = table(new int[] {0, 0, 0x0f});                      // water blocks every direction
        data.priorities = table(new int[] {0, 0, 0});
        data.terrainTags = table(new int[] {0, PBTerrain.GRASS, PBTerrain.WATER});
        return data;
    }

    private static TilesetData.TableData table(int[] values) {
        TilesetData.TableData table = new TilesetData.TableData();
        table.present = true;
        table.z = 1;
        table.x = 8;
        table.y = 1;
        table.total = values.length;
        table.layers = new int[][] {values};
        return table;
    }

    /** One row: water, water, water, ground. */
    private static MapData mapData() {
        MapData data = new MapData();
        data.mapId = 1;
        data.width = 4;
        data.height = 1;
        data.tilesetId = 1;
        MapData.TileData tiles = new MapData.TileData();
        tiles.present = true;
        tiles.z = 3;
        tiles.x = 4;
        tiles.y = 1;
        tiles.total = 12;
        tiles.layers = new int[3][4];
        int[] row = {WATER, WATER, WATER, GROUND};
        System.arraycopy(row, 0, tiles.layers[TileMap.GROUND_LAYER], 0, 4);
        data.tileData = tiles;
        return data;
    }

    @Test
    @DisplayName("an event on water goes on to water, never onto land (027_Game_Map:179-189)")
    void swimmer() {
        GameState state = new GameState();
        MapData data = mapData();
        TileMap map = new TileMap(data, tileset());
        MapCharacter swimmer = new MapCharacter(0, 0, 6, 1, "swimmer");
        assertTrue(Collision.canStep(state, map, data, swimmer, 6), "water to water");
        assertTrue(Collision.canStepFrom(state, map, data, swimmer, 1, 0, 6), "the sight line goes on over the water");
        assertFalse(Collision.canStepFrom(state, map, data, swimmer, 2, 0, 6), "water to land");
    }

    @Test
    @DisplayName("a surfing player may cross onto the connected map's water")
    void surfIntoNeighbour() {
        GameState state = new GameState();
        MapData data = mapData();
        TileMap map = new TileMap(data, tileset());
        MapCharacter player = new MapCharacter(0, 0, 4, 1, "hero");
        player.isPlayer = true;
        assertFalse(Collision.canStepIntoNeighbour(state, map, data, player, 1, 0), "walking: the water blocks");
        state.fieldGlobals().surfing = true;
        assertTrue(Collision.canStepIntoNeighbour(state, map, data, player, 1, 0), "surfing: the water is open");
    }
}
