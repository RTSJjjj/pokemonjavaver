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
    /** PBTerrain::StillWater - {@code hasReflections?} (0167.rb:80). */
    public static final int TERRAIN_STILL_WATER = 6;
    public static final int TERRAIN_TALL_GRASS = 10;
    public static final int TERRAIN_NEUTRAL = 13;
    public static final int TERRAIN_SOOT_GRASS = 14;
    public static final int TERRAIN_BRIDGE = 15;
    /** PBTerrain::Puddle - {@code hasReflections?} too. */
    public static final int TERRAIN_PUDDLE = 16;

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
     *
     * <p>This is the "all other events" half of {@code Game_Map#passable?}
     * (0025.rb:163-222); the source has no bridge rule here - a bridge tile is
     * judged by its own passage bits like any other tile. Only the player goes
     * through {@link #playerPassable} (Game_Map:162), which is where
     * {@code $PokemonGlobal.bridge} lives.</p>
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
            if (tileset.terrainTag(tile) == TERRAIN_NEUTRAL) continue;
            if ((tileset.passage(tile) & directionBit) != 0) {
                return false;
            }
            // A passable priority-zero upper tile replaces the ground below it.
            if (tileset.priority(tile) == 0) return true;
        }
        return true;
    }

    /**
     * {@code Game_Map#playerPassable?} (0025.rb:225-252): the player's own tile
     * half of the passage check. It is the only path that looks at
     * {@code $PokemonGlobal.bridge}:
     *
     * <ul>
     *   <li>bridge 0 - bridge tiles are skipped, so the tile below decides
     *       (walking under, or up to, the bridge);</li>
     *   <li>bridge &gt; 0 - the first bridge tile scanned answers on its own
     *       passage bits and stops the search (walking on the bridge);</li>
     *   <li>every other tile behaves like {@link #passable}.</li>
     * </ul>
     *
     * @param bridgeHeight {@code $PokemonGlobal.bridge}
     */
    public boolean playerPassable(int column, int row, int directionBit, int bridgeHeight) {
        if (!valid(column, row)) {
            return false;
        }
        for (int layer = TOP_LAYER; layer >= GROUND_LAYER; layer--) {
            int tile = tileId(layer, column, row);
            if (tile <= 0) {
                continue;
            }
            int terrain = tileset.terrainTag(tile);
            // `next if PBTerrain.isBridge?(terrain) && $PokemonGlobal.bridge==0`
            if (terrain == TERRAIN_BRIDGE && bridgeHeight == 0) {
                continue;
            }
            // `elsif isBridge? && bridge>0 then return (passage & bit == 0 &&
            //  passage & 0x0f != 0x0f)` - the bridge tile decides, immediately.
            if (terrain == TERRAIN_BRIDGE) {
                int passage = tileset.passage(tile);
                return (passage & directionBit) == 0 && (passage & 0x0f) != 0x0f;
            }
            if (terrain == TERRAIN_NEUTRAL) {
                continue;
            }
            int passage = tileset.passage(tile);
            // `if passage & bit != 0 || passage & 0x0f == 0x0f` - the second
            // clause only bites for the direction-less jump check (bit 0).
            if ((passage & directionBit) != 0 || (passage & 0x0f) == 0x0f) {
                return false;
            }
            if (tileset.priority(tile) == 0) {
                return true;
            }
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
        return terrainTag(column, row, countBridge, 0);
    }

    /**
     * {@code Game_Map#terrain_tag(x, y, countBridge)} (0025.rb:304-313) with the
     * bridge state: the source skips a bridge tile only when
     * {@code !countBridge && $PokemonGlobal.bridge == 0}, so on a bridge the
     * bridge tile answers instead of the tile below it.
     *
     * @param bridgeHeight {@code $PokemonGlobal.bridge}
     */
    public int terrainTag(int column, int row, boolean countBridge, int bridgeHeight) {
        if (!valid(column, row) || tileset == null) {
            return 0;
        }
        for (int layer = TOP_LAYER; layer >= GROUND_LAYER; layer--) {
            int tile = tileId(layer, column, row);
            if (tile <= 0) {
                continue;
            }
            int terrain = tileset.terrainTag(tile);
            if (!countBridge && terrain == TERRAIN_BRIDGE && bridgeHeight == 0) {
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
     * Like {@link #passable} this is the non-player half of the source check;
     * the player's jump goes through {@link #playerPassable} (Game_Map:162).
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
            if (tileset.terrainTag(tile) == TERRAIN_NEUTRAL) continue;
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
