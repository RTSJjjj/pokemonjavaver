package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.MapData;
import pokemon.runtime.data.TilesetData;
import pokemon.runtime.field.PBTerrain;
import pokemon.runtime.state.GameState;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 027_Game_Map:225-250 playerPassable?: surfing makes water passable, the bicycle stops at tall grass and ice. */
class VehiclePassabilityTest {
    private static final int GROUND = 1, WATER = 2, TALL = 3, ICE = 4;

    private static TilesetData tileset() {
        TilesetData data = new TilesetData();
        data.id = 1;
        data.name = "Fake";
        data.tilesetName = "Fake";
        data.autotileNames = new String[] {"a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7"};
        data.passages = table(new int[] {0, 0, 0x0f, 0, 0});               // water blocks every direction
        data.priorities = table(new int[] {0, 0, 0, 0, 0});
        data.terrainTags = table(new int[] {0, PBTerrain.GRASS, PBTerrain.WATER, PBTerrain.TALL_GRASS, PBTerrain.ICE});
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

    /** One row: ground, water, tall grass, ice. */
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
        tiles.layers[TileMap.GROUND_LAYER][0] = GROUND;
        tiles.layers[TileMap.GROUND_LAYER][1] = WATER;
        tiles.layers[TileMap.GROUND_LAYER][2] = TALL;
        tiles.layers[TileMap.GROUND_LAYER][3] = ICE;
        data.tileData = tiles;
        return data;
    }

    private static MapCharacter player(int x) {
        MapCharacter character = new MapCharacter(x, 0, 2, 1, "hero");
        character.isPlayer = true;
        return character;
    }

    @Test
    @DisplayName("water is passable only while surfing")
    void surfing() {
        GameState state = new GameState();
        MapData data = mapData();
        TileMap map = new TileMap(data, tileset());
        assertFalse(Collision.canStep(state, map, data, player(0), 6), "walking: the water blocks");
        state.fieldGlobals().surfing = true;
        assertTrue(Collision.canStep(state, map, data, player(0), 6), "surfing: the water is passable");
    }

    @Test
    @DisplayName("the bicycle cannot enter tall grass or ice")
    void bicycle() {
        GameState state = new GameState();
        MapData data = mapData();
        TileMap map = new TileMap(data, tileset());
        assertTrue(Collision.canStep(state, map, data, player(1 + 0), 6) == false, "water is still blocked on foot");
        MapData row = mapData();
        row.tileData.layers[TileMap.GROUND_LAYER][1] = GROUND;     // make the middle tile plain so only the grass matters
        TileMap plain = new TileMap(row, tileset());
        assertTrue(Collision.canStep(state, plain, row, player(1), 6), "on foot the tall grass is fine");
        state.fieldGlobals().bicycle = true;
        assertFalse(Collision.canStep(state, plain, row, player(1), 6), "cycling: tall grass blocks");
        assertFalse(Collision.canStep(state, plain, row, player(2), 6), "cycling: ice blocks");
    }
}
