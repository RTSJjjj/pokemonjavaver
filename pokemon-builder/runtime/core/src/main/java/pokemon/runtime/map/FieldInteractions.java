package pokemon.runtime.map;

import pokemon.runtime.data.MapData;
import pokemon.runtime.state.GameState;

/**
 * R6.30: this project's field interactions that need no Pokemon data, ported
 * from the plugin scripts so the event IR has something real to call.
 *
 * <ul>
 *   <li>{@code toggle_liefeng_switches} (33968587.rb): floating plates react to
 *       the tile the player stands on.</li>
 *   <li>{@code pbPushThisBoulder} (0168.rb): the Strength field-move boulder
 *       push. The field move itself is stage 3 (badges / party); this class is
 *       the movement half and only runs while
 *       {@code $PokemonMap.strengthUsed} is set.</li>
 * </ul>
 *
 * <p>Pure map/state logic, so it stays headless-testable.</p>
 */
public final class FieldInteractions {

    /** Maps whose floating plates react to the player's tile (from the plugin). */
    private static final int[] FLOAT_PLATE_MAPS = { 60, 207, 209, 210, 371 };
    private static final String FLOAT_PLATE_EVENT = "float_plate";

    private FieldInteractions() {
    }

    /**
     * toggle_liefeng_switches: on the plate maps, every event named
     * {@code float_plate} sets self switch A to "the player stands on me" and
     * clears it everywhere else.
     *
     * <p>The source compares against {@code $game_player.x}/{@code .y}, which
     * RMXP moves to the destination tile when a step <em>starts</em>. The map
     * screen therefore passes the {@link MapCharacter#logicalX() logical} tile;
     * this overload keeps the plain {@code GameState} position for callers that
     * have no character (a transfer or a headless test).</p>
     */
    public static void toggleFloatPlates(GameState state, MapData data) {
        if (state == null) {
            return;
        }
        toggleFloatPlates(state, data, state.playerX(), state.playerY());
    }

    /** {@code toggle_liefeng_switches} against an explicit player tile. */
    public static void toggleFloatPlates(GameState state, MapData data, int playerX, int playerY) {
        if (state == null || data == null || !reactsToPlates(data.mapId)) {
            return;
        }
        for (MapData.EventData event : data.events) {
            if (event.name == null || !event.name.contains(FLOAT_PLATE_EVENT)) {
                continue;
            }
            boolean onPlate = event.x == playerX && event.y == playerY;
            state.selfSwitches().set(data.mapId, event.id, "A", onPlate);
        }
    }

    private static boolean reactsToPlates(int mapId) {
        for (int id : FLOAT_PLATE_MAPS) {
            if (id == mapId) {
                return true;
            }
        }
        return false;
    }

    /**
     * pbPushThisEvent: the boulder's own tile has to be strictly passable
     * ({@code Game_Map#passableStrict?}), then it steps one tile in the
     * player's facing direction ({@code move_down/left/right/up}).
     *
     * @return seconds the push step takes, or 0 when the boulder did not move
     */
    public static float pushBoulder(GameState state, TileMap map, MapData data,
                                    EventCharacters characters, int eventId, int direction) {
        if (map == null || data == null || characters == null) {
            return 0f;
        }
        MapCharacter boulder = characters.character(eventId);
        if (boulder == null || boulder.isMoving()) {
            return 0f;
        }
        if (!strictlyPassable(state, map, data, eventId, boulder.x(), boulder.y())) {
            return 0f;
        }
        int x = Collision.targetX(boulder.x(), direction);
        int y = Collision.targetY(boulder.y(), direction);
        if (!Collision.canStep(state, map, data, boulder, direction)) {
            return 0f;
        }
        if (!boulder.startMove(x, y, direction)) {
            return 0f;
        }
        return 1f / boulder.speed();
    }

    /**
     * The map half of RGSS {@code passableStrict?}: no blocking event on the
     * tile and - through the topmost non-neutral tiles - no blocked direction.
     * A passable priority-zero tile ends the search as passable, matching the
     * source engine's early returns.
     */
    public static boolean strictlyPassable(GameState state, TileMap map, MapData data,
                                           int selfEventId, int x, int y) {
        if (map == null || data == null || !map.valid(x, y)) {
            return false;
        }
        for (MapData.EventData event : data.events) {
            if (event.id == selfEventId || event.x != x || event.y != y) {
                continue;
            }
            MapData.EventPageData page = EventPages.resolve(state, data.mapId, event);
            if (page == null || EventPages.through(page) || page.graphic == null) {
                continue;
            }
            int tile = page.graphic.tileId;
            if (tile <= 0 || map.tileset().terrainTag(tile) == TileMap.TERRAIN_NEUTRAL) {
                continue;
            }
            if ((map.tileset().passage(tile) & 0x0f) != 0) {
                return false;
            }
            if (map.tileset().priority(tile) == 0) {
                return true;
            }
        }
        return map.passableStrict(x, y);
    }
}
