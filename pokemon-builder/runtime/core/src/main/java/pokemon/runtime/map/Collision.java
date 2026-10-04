package pokemon.runtime.map;

import pokemon.runtime.data.MapData;
import pokemon.runtime.state.GameState;

/**
 * RMXP movement rules (13): map boundary, the 4-direction passage bits of all
 * three tile layers, and the other characters standing on the target tile.
 *
 * <p>Passage semantics (fixed in R4): a bit SET on the tile blocks movement in
 * that direction; blank (0) is passable. Bits are down=1, left=2, right=4,
 * up=8, matching {@code [1,2,4,8][d/2-1]} of the RMXP interpreter.</p>
 */
public final class Collision {

    /** Direction -> passage bit (RMXP directions are 2 down, 4 left, 6 right, 8 up). */
    public static int directionBit(int direction) {
        switch (direction) {
            case 2:
                return 0x01;
            case 4:
                return 0x02;
            case 6:
                return 0x04;
            case 8:
                return 0x08;
            default:
                return 0;
        }
    }

    /** Tile one step in a direction from (x, y). */
    public static int targetX(int x, int direction) {
        return x + (direction == 4 ? -1 : direction == 6 ? 1 : 0);
    }

    public static int targetY(int y, int direction) {
        return y + (direction == 8 ? -1 : direction == 2 ? 1 : 0);
    }

    /** Tile events are checked before the map layers, as in the source Game_Map. */
    private static boolean passage(GameState state, TileMap map, MapData data, int x, int y, int bit) {
        for (MapData.EventData event : data.events) {
            if (event.x != x || event.y != y) continue;
            MapData.EventPageData page = EventPages.resolve(state, data.mapId, event);
            if (page == null || EventPages.through(page) || page.graphic == null) continue;
            int tile = page.graphic.tileId;
            if (tile <= 0 || map.tileset().terrainTag(tile) == 13) continue;
            if ((map.tileset().passage(tile) & bit) != 0) return false;
            if (map.tileset().priority(tile) == 0) return true;
        }
        return map.passable(x, y, bit);
    }

    private static boolean characterAt(GameState state, MapData data, int x, int y) {
        for (MapData.EventData event : data.events) {
            if (event.x != x || event.y != y) continue;
            MapData.EventPageData page = EventPages.resolve(state, data.mapId, event);
            if (page == null || EventPages.through(page) || page.graphic == null) continue;
            String name = page.graphic.characterName;
            // Opacity does not disable collision. Blank/tile events use passage checks.
            if (name != null && !name.isEmpty()) return true;
        }
        return false;
    }

    /** The character may leave its tile in a direction only if the way is open. */
    public static boolean canStep(GameState state, TileMap map, MapData data,
                                  MapCharacter character, int direction) {
        return canStepFrom(state, map, data, character, character.x(), character.y(), direction);
    }

    /**
     * L6b: the same check from an explicit tile. The line-of-sight sweep uses
     * it to walk a trainer's passability along its facing axis without moving
     * the character.
     */
    public static boolean canStepFrom(GameState state, TileMap map, MapData data,
                                      MapCharacter character, int x, int y, int direction) {
        int tx = Collision.targetX(x, direction);
        int ty = Collision.targetY(y, direction);
        int bit = directionBit(direction);
        if (bit == 0 || !map.valid(x, y) || !map.valid(tx, ty)) return false;
        if (character.through) return true;
        return passage(state, map, data, x, y, bit)
                && passage(state, map, data, tx, ty, directionBit(10 - direction))
                && !characterAt(state, data, tx, ty);
    }

    /**
     * R6.32: move route codes 5-8 (diagonal steps). RGSS
     * {@code move_upper_left} & co. accept the step when one of the two
     * L-shaped paths is open: leave the current tile vertically and then the
     * neighbour horizontally, or the other way round. The landing tile must
     * not be taken by another character.
     */
    public static boolean canStepDiagonal(GameState state, TileMap map, MapData data,
                                          MapCharacter character, int horz, int vert) {
        int x = character.x();
        int y = character.y();
        int nx = x + (horz == 4 ? -1 : 1);
        int ny = y + (vert == 8 ? -1 : 1);
        if (directionBit(horz) == 0 || directionBit(vert) == 0
                || !map.valid(x, y) || !map.valid(nx, ny)) {
            return false;
        }
        if (character.through) {
            return true;
        }
        boolean verticalThenHorizontal =
                passage(state, map, data, x, y, directionBit(vert))
                && passage(state, map, data, x, ny, directionBit(horz));
        boolean horizontalThenVertical =
                passage(state, map, data, x, y, directionBit(horz))
                && passage(state, map, data, nx, y, directionBit(vert));
        return (verticalThenHorizontal || horizontalThenVertical)
                && !characterAt(state, data, nx, ny);
    }

    /**
     * R6.18: move route code 14 (jump). RGSS checks
     * {@code passable?(new_x, new_y, 0)}: only the landing tile matters (the
     * tile in between is ignored, which is what makes ledge jumps work) and a
     * tile that blocks all four directions - or another character - rejects
     * the jump.
     */
    public static boolean canLand(GameState state, TileMap map, MapData data,
                                  MapCharacter character, int x, int y) {
        if (!map.valid(x, y)) {
            return false;
        }
        if (character.through) {
            return true;
        }
        return map.passableAnyDirection(x, y) && !characterAt(state, data, x, y);
    }

    /**
     * R6.23: MapFactory#isPassable? for a step off the current map's edge.
     * RMXP checks the neighbour cell with {@code d = 0} (passable in at least
     * one direction), ignores the current map's own side and then rejects a
     * character standing on the landing tile.
     */
    public static boolean canStepIntoNeighbour(GameState state, TileMap map, MapData data,
                                               MapCharacter character, int x, int y) {
        if (!map.valid(x, y)) {
            return false;
        }
        if (character.through) {
            return true;
        }
        return map.passableAnyDirection(x, y) && !characterAt(state, data, x, y);
    }

    /** Nearest usable spawn for a debug map override; never changes the source map. */
    public static int[] debugSpawn(GameState state, TileMap map, MapData data,
                                   int requestedX, int requestedY) {
        int cx = Math.max(0, Math.min(requestedX, map.width() - 1));
        int cy = Math.max(0, Math.min(requestedY, map.height() - 1));
        int bestX = cx, bestY = cy, bestDistance = Integer.MAX_VALUE;
        for (int y = 0; y < map.height(); y++) for (int x = 0; x < map.width(); x++) {
            int distance = Math.abs(x - cx) + Math.abs(y - cy);
            if (distance >= bestDistance || characterAt(state, data, x, y)) continue;
            boolean hasTile = false;
            for (int layer = 0; layer < 3; layer++) hasTile |= map.tileId(layer, x, y) >= 48;
            if (!hasTile) continue;
            MapCharacter probe = new MapCharacter(x, y, map.width(), map.height(), "");
            if (canStep(state, map, data, probe, 2) || canStep(state, map, data, probe, 4)
                    || canStep(state, map, data, probe, 6) || canStep(state, map, data, probe, 8)) {
                bestX = x; bestY = y; bestDistance = distance;
            }
        }
        // Empty/cutscene maps remain viewable even without a walkable tile.
        return new int[] {bestX, bestY};
    }
}
