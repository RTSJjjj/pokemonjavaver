package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.MapData;
import pokemon.runtime.data.TilesetData;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless tests for the map model, the tileset geometry and the camera
 * (project3 section 61): rendering needs a graphics context, these do not.
 */
class TileMapTest {

    private static TilesetData tileset(int[] passages, int[] priorities) {
        TilesetData data = new TilesetData();
        data.id = 1;
        data.name = "Fake";
        data.tilesetName = "Fake";
        data.autotileNames = new String[] { "a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7" };
        data.passages = table(passages);
        data.priorities = table(priorities);
        data.terrainTags = table(null);
        return data;
    }


    private static TilesetData.TableData table(int[] values) {
        TilesetData.TableData table = new TilesetData.TableData();
        table.present = values != null;
        table.z = 1;
        table.x = 8;
        table.y = 1;
        table.total = values == null ? 0 : values.length;
        table.layers = values == null ? null : new int[][] { values };
        return table;
    }

    @Test
    @DisplayName("addresses the three tile layers with ground/middle/top semantics")
    void addressesLayers() {
        TilesetData tileset = tileset(new int[] { 15, 15, 15, 15, 15, 15, 15, 15 },
                new int[] { 0, 0, 0, 0, 0, 0, 0, 0 });
        TileMap map = new TileMap(mapData(4, 3, null), tileset);

        // ground layer holds sequential ids 1..12 over 4x3
        assertEquals(1, map.tileId(TileMap.GROUND_LAYER, 0, 0));
        assertEquals(4, map.tileId(TileMap.GROUND_LAYER, 3, 0));
        assertEquals(12, map.tileId(TileMap.GROUND_LAYER, 3, 2));
        // middle and top layers are blank in this fixture
        assertEquals(0, map.tileId(TileMap.MIDDLE_LAYER, 1, 1));
        assertEquals(0, map.tileId(TileMap.TOP_LAYER, 2, 2));
        // outside the map is blank and invalid
        assertEquals(0, map.tileId(TileMap.GROUND_LAYER, 4, 0));
        assertFalse(map.valid(4, 0));
        assertFalse(map.valid(0, 3));
        assertFalse(map.valid(-1, 0));
    }

    @Test
    @DisplayName("passage bits block the matching direction only")
    void passability() {
        // tile 1 blocks left+right (0x06), tile 2 blocks down (0x01)
        TilesetData tileset = tileset(new int[] { 0, 6, 1, 15, 15, 15, 15, 15 },
                new int[] { 0, 0, 0, 0, 0, 0, 0, 0 });
        // ground layer 1..12, middle and top blank
        TileMap map = new TileMap(mapData(4, 3, null), tileset);

        // tile 1: passage bits 6 (left+right) block horizontal steps only
        assertTrue(map.passable(0, 0, Collision.directionBit(8)));   // up bit clear
        assertTrue(map.passable(0, 0, Collision.directionBit(2)));   // down bit clear
        assertFalse(map.passable(0, 0, Collision.directionBit(4)));  // left bit set
        assertFalse(map.passable(0, 0, Collision.directionBit(6)));  // right bit set
        // tile 2: blocks only "down"
        assertFalse(map.passable(1, 0, Collision.directionBit(2)));
        assertTrue(map.passable(1, 0, Collision.directionBit(8)));
        // outside the map is never passable
        assertFalse(map.passable(9, 9, Collision.directionBit(2)));
    }

    @Test
    @DisplayName("priority comes from the tileset table, 0 by default")
    void priority() {
        TilesetData tileset = tileset(new int[] { 15, 15, 15, 15, 15, 15, 15, 15 },
                new int[] { 0, 0, 3, 0, 0, 0, 0, 0 });
        TileMap map = new TileMap(mapData(4, 3, 2), tileset);
        assertEquals(3, map.priorityAt(0, 0));
        assertEquals(3, map.priorityAt(3, 2));

        TileMap blank = new TileMap(mapData(4, 3, 1), tileset);
        assertEquals(0, blank.priorityAt(0, 0));
    }

    @Test
    @DisplayName("RMXP autotile slots and quarter cells use the serialized ID contract")
    void autotileGeometry() {
        TilesetGeometry geometry = new TilesetGeometry(100);
        assertFalse(geometry.isAutotile(47));
        assertTrue(geometry.isAutotile(48));
        assertTrue(geometry.isAutotile(383));
        assertFalse(geometry.isAutotile(384));
        assertFalse(geometry.isAutotile(5476));
        assertEquals(0, geometry.autotileIndex(48));
        assertEquals(6, geometry.autotileIndex(383));
        assertEquals(-1, geometry.autotileIndex(384));
        // Shape 0 is the center; shape 1 substitutes only its upper-left inner corner.
        assertEquals(32, geometry.quarterX(48, 0));
        assertEquals(64, geometry.quarterY(48, 0));
        assertEquals(64, geometry.quarterX(49, 0));
        assertEquals(0, geometry.quarterY(49, 0));
        assertEquals(48, geometry.quarterX(49, 3));
        assertEquals(80, geometry.quarterY(49, 3));
        // Isolated shape 46 uses four outside corners, not one rectangular cell.
        assertEquals(0, geometry.quarterX(94, 0));
        assertEquals(32, geometry.quarterY(94, 0));
        assertEquals(80, geometry.quarterX(94, 3));
        assertEquals(112, geometry.quarterY(94, 3));
        java.util.Set<String> shapes = new java.util.HashSet<>();
        for (int shape = 0; shape < 48; shape++) {
            String signature = "";
            for (int q = 0; q < 4; q++) {
                int x = geometry.quarterX(48 + shape, q), y = geometry.quarterY(48 + shape, q);
                assertTrue(x >= 0 && x <= 80 && y >= 0 && y <= 112);
                signature += x + "," + y + ";";
            }
            assertTrue(shapes.add(signature), "Duplicate shape " + shape);
        }
        // Real project: 19-frame water and five-frame 32px flowers.
        assertEquals(18, geometry.animationFrame(1824, 128, 7.21f));
        assertEquals(0, geometry.animationFrame(1824, 128, 7.61f));
        assertEquals(4, geometry.animationFrame(160, 32, 1.61f));
        assertEquals(0, geometry.animationFrame(160, 32, 2.01f));
    }

    @Test
    @DisplayName("normal tiles are an 8 column grid from the top of the tileset image")
    void normalTileGeometry() {
        TilesetGeometry geometry = new TilesetGeometry(100);
        assertEquals(0f, geometry.normalTileRect(384).x);
        assertEquals(0f, geometry.normalTileRect(384).y);
        assertEquals(224f, geometry.normalTileRect(391).x);
        assertEquals(32f, geometry.normalTileRect(392).y);
        assertEquals(64f, geometry.normalTileRect(658).x); // Map002 top-left tree
        assertEquals(1088f, geometry.normalTileRect(658).y);
        assertNull(geometry.normalTileRect(0));
        assertNull(geometry.normalTileRect(383));
        assertNull(geometry.normalTileRect(384 + 800));
        assertNotNull(new TilesetGeometry(780).normalTileRect(5476));
    }

    @Test
    @DisplayName("the camera clamps to the map and centers on small maps")
    void cameraClamps() {
        MapCamera camera = new MapCamera(40, 30, 10 * 32, 10 * 32);
        camera.centerOn(5, 5);
        assertEquals(16, camera.originX());
        assertEquals(30 * 32 - 176 - 160, camera.originY());

        camera.centerOn(0, 0);
        assertEquals(0, camera.originX());            // clamped left
        assertEquals(30 * 32 - 320, camera.originY()); // clamped bottom

        camera.centerOn(39, 29);
        assertEquals(40 * 32 - 320, camera.originX()); // clamped right
        assertEquals(0, camera.originY());

        // A map smaller than the view centers instead of clamping.
        MapCamera small = new MapCamera(8, 6, 10 * 32, 10 * 32);
        small.centerOn(4, 3);
        assertEquals(-32, small.originX());
        assertEquals(-64, small.originY());
    }

    // ------------------------------------------------------------------

    /** Ground layer with sequential ids 1..w*h, or blank when fill is null. */
    private static MapData mapData(int width, int height, Integer fill) {
        MapData data = new MapData();
        data.mapId = 1;
        data.width = width;
        data.height = height;
        data.tilesetId = 1;
        MapData.TileData tileData = new MapData.TileData();
        tileData.present = true;
        tileData.z = 3;
        tileData.x = width;
        tileData.y = height;
        tileData.total = width * height * 3;
        tileData.layers = new int[3][width * height];
        if (fill == null) {
            for (int cell = 0; cell < tileData.layers[0].length; cell++) {
                tileData.layers[0][cell] = cell + 1;
            }
        } else {
            // Only the ground layer carries tiles; middle/top stay blank.
            java.util.Arrays.fill(tileData.layers[0], fill);
        }
        data.tileData = tileData;
        return data;
    }

    @Test
    @DisplayName("terrain tags pick the topmost tile and skip bridges (R6.28)")
    void terrainTags() {
        TilesetData tileset = tileset(
                new int[] { 15, 15, 15, 15, 15, 15, 15, 15 },
                new int[] { 0, 0, 0, 0, 0, 0, 0, 0 });
        // 1 = grass (PBTerrain::Grass), 2 = bridge, 3 = neutral overlay,
        // 4 = soot grass.
        tileset.terrainTags = table(new int[] { 0, 2, 15, 13, 14, 0, 0, 0 });
        MapData data = mapData(2, 1, 1);
        data.tileData.layers[1][1] = 2; // the bridge sits above the grass
        data.tileData.layers[1][0] = 3; // neutral overlay does not win
        MapData dataSoot = mapData(1, 1, 4);
        TileMap map = new TileMap(data, tileset);
        TileMap soot = new TileMap(dataSoot, tileset);

        assertEquals(TileMap.TERRAIN_GRASS, map.terrainTag(0, 0, true),
                "the neutral middle layer is skipped for the grass below");
        assertEquals(TileMap.TERRAIN_BRIDGE, map.terrainTag(1, 0, true),
                "with countBridge the bridge itself answers (no rustle under it)");
        assertEquals(TileMap.TERRAIN_GRASS, map.terrainTag(1, 0, false),
                "without countBridge the grass below shows through");
        assertEquals(TileMap.TERRAIN_SOOT_GRASS, soot.terrainTag(0, 0, true));
        assertEquals(0, map.terrainTag(-1, 0, true));
    }
}
