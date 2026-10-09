package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.MapData;
import pokemon.runtime.data.TilesetData;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code Game_Map#counter?} (027_Game_Map:294-301): bit 0x80 of a tile's passage on
 * any of the three layers makes the tile a counter.
 */
class CounterTest {

    /** 0 blank, 1 plain floor, 2 the counter tile (passage 0x80 + blocked), 3 a wall (blocked, not a counter). */
    private static TilesetData tileset() {
        TilesetData data = new TilesetData();
        data.id = 1;
        data.name = "Fake";
        data.tilesetName = "Fake";
        data.autotileNames = new String[] { "a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7" };
        data.passages = table(new int[] { 0, 0, 0x8f, 0x0f });
        data.priorities = table(new int[] { 0, 0, 0, 0 });
        data.terrainTags = table(new int[] { 0, 0, 0, 0 });
        return data;
    }

    private static TilesetData.TableData table(int[] values) {
        TilesetData.TableData table = new TilesetData.TableData();
        table.present = true;
        table.z = 1;
        table.x = 8;
        table.y = 1;
        table.total = values.length;
        table.layers = new int[][] { values };
        return table;
    }

    /** 4x1: floor, counter on the ground layer, a counter on the TOP layer over floor, a wall. */
    private static MapData mapData() {
        MapData data = new MapData();
        data.mapId = 1;
        data.width = 4;
        data.height = 1;
        data.tilesetId = 1;
        MapData.TileData tileData = new MapData.TileData();
        tileData.present = true;
        tileData.z = 3;
        tileData.x = 4;
        tileData.y = 1;
        tileData.total = 12;
        tileData.layers = new int[3][4];
        tileData.layers[TileMap.GROUND_LAYER][0] = 1;
        tileData.layers[TileMap.GROUND_LAYER][1] = 2;     // counter on the ground layer
        tileData.layers[TileMap.GROUND_LAYER][2] = 1;
        tileData.layers[TileMap.TOP_LAYER][2] = 2;        // counter on the top layer only
        tileData.layers[TileMap.GROUND_LAYER][3] = 3;     // wall: blocked but no 0x80
        data.tileData = tileData;
        return data;
    }

    @Test
    @DisplayName("a tile whose passage has 0x80 on any layer is a counter; a plain wall is not")
    void counterFlag() {
        TileMap map = new TileMap(mapData(), tileset());
        assertFalse(map.counter(0, 0), "plain floor");
        assertTrue(map.counter(1, 0), "ground layer counter");
        assertTrue(map.counter(2, 0), "the top layer decides too");
        assertFalse(map.counter(3, 0), "0x0f alone is a wall, not a counter");
    }

    @Test
    @DisplayName("outside the map is never a counter")
    void outsideTheMap() {
        TileMap map = new TileMap(mapData(), tileset());
        assertFalse(map.counter(-1, 0));
        assertFalse(map.counter(4, 0));
        assertFalse(map.counter(0, 1));
    }
}
