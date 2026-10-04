package pokemon.runtime.map;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.data.MapData;
import pokemon.runtime.data.TilesetData;

/**
 * Runtime tile map model (project3 section 10): the decoded Builder tile
 * table plus the tileset that makes passability and priority questions
 * answerable. Pure data: rendering lives in MapRenderer.
 */
public final class TileMap {

    public static final int GROUND_LAYER = 0;
    public static final int MIDDLE_LAYER = 1;
    public static final int TOP_LAYER = 2;
    /** PBTerrain terrain tags this project uses (see 0167.rb). */
    public static final int TERRAIN_LEDGE = 1;
    public static final int TERRAIN_GRASS = 2;
    public static final int TERRAIN_TALL_GRASS = 10;
    public static final int TERRAIN_NEUTRAL = 13;
    public static final int TERRAIN_SOOT_GRASS = 14;
    public static final int TERRAIN_BRIDGE = 15;

    private final int width;
    private final int height;
    private final int mapId;
    private final int[][] layers;
    private final TilesetData tileset;
    private final Array<MapData.EventData> events;

    public TileMap(MapData data, TilesetData tileset) {
        this.width = data.width;
        this.height = data.height;
        this.mapId = data.mapId;
        this.tileset = tileset;
        this.events = data.events;
        this.layers = new int[3][];
        for (int layer = 0; layer < 3; layer++) {
            int[] values;
            if (data.tileData != null && data.tileData.layers != null && layer < data.tileData.layers.length) {
                values = data.tileData.layers[layer];
            } else {
                values = new int[0];
            }
            this.layers[layer] = values;
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** Map id of the source document; self switches and transfers key on it. */
    public int mapId() {
        return mapId;
    }

    public TilesetData tileset() {
        return tileset;
    }

    public Array<MapData.EventData> events() {
        return events;
    }

    public boolean valid(int column, int row) {
        return column >= 0 && row >= 0 && column < width && row < height;
    }

    /** The tile id at one position; 0 (blank) when outside or missing. */
    public int tileId(int layer, int column, int row) {
        if (layer < 0 || layer >= layers.length || !valid(column, row)) {
            return 0;
        }
        int index = row * width + column;
        int[] values = layers[layer];
        if (index < 0 || index >= values.length) {
            return 0;
        }
        return values[index];
    }

    /**
     * Top-down passage check for one side of a tile: a set direction bit blocks.
     * A passable priority-zero tile terminates the search; transparent overlays do not.
     * Collision checks this for BOTH the source side and the opposite destination side.
     */
    public boolean passable(int column, int row, int directionBit) {
        if (!valid(column, row)) {
            return false;
        }
        for (int layer = 2; layer >= 0; layer--) {
            int tile = tileId(layer, column, row);
            if (tile <= 0) {
                continue;
            }
            // This project's PBTerrain: Neutral=13, Bridge=15. R4 walks
            // underneath bridges (bridge state 0); surfing/bridge state comes later.
            if (tileset.terrainTag(tile) == 13 || tileset.terrainTag(tile) == 15) continue;
            if ((tileset.passage(tile) & directionBit) != 0) {
                return false;
            }
            // A passable priority-zero upper tile replaces the ground below it.
            if (tileset.priority(tile) == 0) return true;
        }
        return true;
    }

    /**
     * R6.28: this project's {@code Game_Map#terrain_tag}: the topmost tile with
     * a positive, non-neutral terrain tag wins. Bridges are skipped unless
     * {@code countBridge} is set, which is how the grass rustle avoids firing
     * while the player walks over a bridge above grass.
     */
    public int terrainTag(int column, int row, boolean countBridge) {
        if (!valid(column, row) || tileset == null) {
            return 0;
        }
        for (int layer = TOP_LAYER; layer >= GROUND_LAYER; layer--) {
            int tile = tileId(layer, column, row);
            if (tile <= 0) {
                continue;
            }
            int terrain = tileset.terrainTag(tile);
            if (!countBridge && terrain == TERRAIN_BRIDGE) {
                continue;
            }
            if (terrain > 0 && terrain != TERRAIN_NEUTRAL) {
                return terrain;
            }
        }
        return 0;
    }

    /**
     * R6.18: the direction-less passage check RGSS uses for jumps
     * ({@code passable?(x, y, 0)}): only a tile that blocks <em>all four</em>
     * directions stops a jump, so a character can jump over obstacles and down
     * ledges. A passable priority-zero tile terminates the search as passable.
     */
    public boolean passableAnyDirection(int column, int row) {
        if (!valid(column, row)) {
            return false;
        }
        for (int layer = 2; layer >= 0; layer--) {
            int tile = tileId(layer, column, row);
            if (tile <= 0) {
                continue;
            }
            if (tileset.terrainTag(tile) == 13 || tileset.terrainTag(tile) == 15) continue;
            if ((tileset.passage(tile) & 0x0f) == 0x0f) {
                return false;
            }
            if (tileset.priority(tile) == 0) return true;
        }
        return true;
    }

    /**
     * R6.30: this project's {@code Game_Map#passableStrict?} tile half - the
     * tile is strictly passable only when the topmost non-neutral tile blocks
     * no direction and a passable priority-zero tile ends the search. Used by
     * the Strength boulder push.
     */
    public boolean passableStrict(int column, int row) {
        if (!valid(column, row)) {
            return false;
        }
        for (int layer = TOP_LAYER; layer >= GROUND_LAYER; layer--) {
            int tile = tileId(layer, column, row);
            if (tile <= 0) {
                continue;
            }
            if (tileset.terrainTag(tile) == TERRAIN_NEUTRAL) {
                continue;
            }
            if ((tileset.passage(tile) & 0x0f) != 0) {
                return false;
            }
            if (tileset.priority(tile) == 0) {
                return true;
            }
        }
        return true;
    }

    /** 0 = below characters, 1 = same layer as characters, 2+ = above. */
    public int priorityAt(int column, int row) {
        if (!valid(column, row)) {
            return 0;
        }
        int priority = 0;
        for (int layer = 0; layer < 3; layer++) {
            int tile = tileId(layer, column, row);
            if (tile <= 0) {
                continue;
            }
            priority = Math.max(priority, tileset.priority(tile));
        }
        return priority;
    }
}
