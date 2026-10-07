package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.MapData;
import pokemon.runtime.data.TilesetData;
import pokemon.runtime.state.GameState;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R6.34: {@code pbBridgeOn} / {@code pbBridgeOff} and the bridge half of the
 * passage check.
 *
 * <p>Source: PField_Field:1363-1369 writes {@code $PokemonGlobal.bridge};
 * {@code Game_Map#playerPassable?} (0025.rb:225-252) is its only reader, and
 * Game_Map:162 routes exactly {@code $game_player} into it - every other
 * character uses the branch below it, which has no bridge rule at all.</p>
 *
 * <p>The fixture is the usual bridge shape: a passable bridge tile on the
 * middle layer above an impassable water tile on the ground layer.</p>
 */
class BridgeTest {

    /** Ground tile that is fully blocked in all four directions (water). */
    private static final int WATER = 2;
    /** The bridge tile: terrain tag 15, passage 0. */
    private static final int BRIDGE_TILE = 3;
    /** Terrain tag of the tile underneath; the test reads it back, never assumes it. */
    private static final int UNDER_TAG = TileMap.TERRAIN_TALL_GRASS;

    private static TilesetData tileset() {
        TilesetData data = new TilesetData();
        data.id = 1;
        data.name = "Fake";
        data.tilesetName = "Fake";
        data.autotileNames = new String[] { "a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7" };
        // 0 blank, 1 plain ground, 2 water, 3 bridge.
        data.passages = table(new int[] { 0, 0, 0x0f, 0 });
        data.priorities = table(new int[] { 0, 0, 0, 0 });
        data.terrainTags = table(new int[] { 0, TileMap.TERRAIN_GRASS, UNDER_TAG,
                TileMap.TERRAIN_BRIDGE });
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

    /** 2x1: (0,0) plain ground, (1,0) water with the bridge tile above it. */
    private static MapData mapData() {
        MapData data = new MapData();
        data.mapId = 1;
        data.width = 2;
        data.height = 1;
        data.tilesetId = 1;
        MapData.TileData tileData = new MapData.TileData();
        tileData.present = true;
        tileData.z = 3;
        tileData.x = 2;
        tileData.y = 1;
        tileData.total = 6;
        tileData.layers = new int[3][2];
        tileData.layers[TileMap.GROUND_LAYER][0] = 1;      // plain ground
        tileData.layers[TileMap.GROUND_LAYER][1] = WATER;  // blocked
        tileData.layers[TileMap.MIDDLE_LAYER][1] = BRIDGE_TILE;
        data.tileData = tileData;
        return data;
    }

    private static MapCharacter at(int x, int y, boolean player) {
        MapCharacter character = new MapCharacter(x, y, 2, 1, player ? "hero" : "npc");
        character.isPlayer = player;
        return character;
    }

    @Test
    @DisplayName("the player only walks onto the bridge while $PokemonGlobal.bridge is up")
    void playerCrossesTheBridge() {
        GameState state = new GameState();
        MapData data = mapData();
        TileMap map = new TileMap(data, tileset());

        assertFalse(Collision.canStep(state, map, data, at(0, 0, true), 6),
                "bridge off: the bridge tile is skipped and the water below blocks");

        state.bridge(2); // pbBridgeOn
        assertTrue(Collision.canStep(state, map, data, at(0, 0, true), 6),
                "bridge on: the bridge tile answers with its own passage bits");

        state.bridge(0); // pbBridgeOff
        assertFalse(Collision.canStep(state, map, data, at(0, 0, true), 6),
                "pbBridgeOff closes the bridge again");
    }

    @Test
    @DisplayName("every other character judges a bridge tile like any other tile (Game_Map:163-222)")
    void otherCharactersIgnoreTheBridge() {
        GameState state = new GameState();
        MapData data = mapData();
        TileMap map = new TileMap(data, tileset());

        assertTrue(Collision.canStep(state, map, data, at(0, 0, false), 6),
                "the non-player branch has no bridge rule: the bridge tile is passable");
        state.bridge(2);
        assertTrue(Collision.canStep(state, map, data, at(0, 0, false), 6));
    }

    @Test
    @DisplayName("a jump onto the bridge follows the same rule (Game_Character#jump asks passable? d=0)")
    void jumpOntoTheBridge() {
        GameState state = new GameState();
        MapData data = mapData();
        TileMap map = new TileMap(data, tileset());

        assertFalse(Collision.canLand(state, map, data, at(0, 0, true), 1, 0),
                "bridge off: the water blocks the landing tile");
        state.bridge(2);
        assertTrue(Collision.canLand(state, map, data, at(0, 0, true), 1, 0));
        assertTrue(Collision.canLand(state, map, data, at(0, 0, false), 1, 0),
                "an event never consults the bridge state");
    }

    @Test
    @DisplayName("terrain_tag reports the bridge tile only while the bridge is up")
    void terrainTagFollowsTheBridge() {
        TileMap map = new TileMap(mapData(), tileset());

        assertEquals(UNDER_TAG, map.terrainTag(1, 0, false, 0),
                "bridge off: the tile below answers (the encounter query)");
        assertEquals(UNDER_TAG, map.terrainTag(1, 0, false),
                "the 3-argument overload keeps the bridge-off behaviour");
        assertEquals(TileMap.TERRAIN_BRIDGE, map.terrainTag(1, 0, false, 2),
                "bridge on: the bridge tile itself answers");
        assertEquals(TileMap.TERRAIN_BRIDGE, map.terrainTag(1, 0, true, 0),
                "countBridge (the grass rustle) stops the skip even while the bridge is off");
    }

    @Test
    @DisplayName("real map 21: every bridge tile over blocked ground is a wall while off, open while on")
    void realBridgeMap() {
        java.io.File dataRoot = TestData.runtimeDataRoot();
        org.junit.jupiter.api.Assumptions.assumeTrue(dataRoot != null,
                "runtime data (generated/) not available");
        pokemon.runtime.data.GameDatabase database =
                pokemon.runtime.data.GameDatabase.load(dataRoot.getAbsolutePath(), dataRoot);
        try {
            MapData data = database.map(21); // 沃饶洞, the project's bridge cave
            TileMap map = new TileMap(data, database.tileset(data.tilesetId));

            int bridgeCells = 0;
            int overBlockedBase = 0;
            int openWithBridge = 0;
            int coveringDecks = 0;
            for (int layer = 0; layer < 3; layer++) {
                for (int y = 0; y < map.height(); y++) {
                    for (int x = 0; x < map.width(); x++) {
                        int tile = map.tileId(layer, x, y);
                        if (tile <= 0 || map.tileset().terrainTag(tile) != TileMap.TERRAIN_BRIDGE) {
                            continue;
                        }
                        bridgeCells++;
                        // Tilemap_XP:408-414: bridge off -> the deck keeps its
                        // priority z and covers whoever walks under it; bridge
                        // on -> z=1, the hero walks visibly on the deck.
                        int priority = map.tileset().priority(tile);
                        assertEquals(MapRenderer.BRIDGE_DEPTH,
                                MapRenderer.tileDepth(y, priority, TileMap.TERRAIN_BRIDGE, 2),
                                "bridge on: the deck must drop under the hero at " + x + "," + y);
                        if (priority > 0) {
                            coveringDecks++;
                            assertTrue(MapRenderer.tileDepth(y, priority, TileMap.TERRAIN_BRIDGE, 0)
                                            > MapRenderer.eventDepth(y),
                                    "bridge off: the deck must cover a character at " + x + "," + y);
                        }
                        // The tile that decides while the bridge is skipped.
                        boolean blockedBase = false;
                        for (int below = layer - 1; below >= 0; below--) {
                            int t = map.tileId(below, x, y);
                            if (t <= 0) {
                                continue;
                            }
                            blockedBase = (map.tileset().passage(t) & 0x0f) == 0x0f
                                    && map.tileset().terrainTag(t) != TileMap.TERRAIN_NEUTRAL;
                            break;
                        }
                        if (blockedBase) {
                            overBlockedBase++;
                            for (int direction : new int[] { 2, 4, 6, 8 }) {
                                assertFalse(map.playerPassable(x, y,
                                                Collision.directionBit(direction), 0),
                                        "bridge off at " + x + "," + y
                                                + " must stay a wall (pbBridgeOff)");
                            }
                        }
                        for (int direction : new int[] { 2, 4, 6, 8 }) {
                            if (map.playerPassable(x, y, Collision.directionBit(direction), 2)) {
                                openWithBridge++;
                                break;
                            }
                        }
                    }
                }
            }
            assertTrue(bridgeCells >= 30, "map 21 is a bridge map: " + bridgeCells);
            assertTrue(coveringDecks > 0,
                    "the project's bridges use a covering priority: " + coveringDecks + " tiles");
            assertTrue(overBlockedBase > 0,
                    "the project's bridges sit over water: " + overBlockedBase + " tiles");
            assertEquals(bridgeCells, openWithBridge,
                    "every bridge tile has an open direction while pbBridgeOn is up");
        } finally {
            database.dispose();
        }
    }
}
