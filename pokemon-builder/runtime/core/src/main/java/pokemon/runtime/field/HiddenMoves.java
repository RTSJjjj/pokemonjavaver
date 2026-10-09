package pokemon.runtime.field;

import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.GameState;

import java.util.Arrays;

/**
 * 179_PField_FieldMoves:12-56 {@code HiddenMoveHandlers}: which moves have a handler, and each handler's
 * {@code CanUseMove} / {@code ConfirmUseMove} (the {@code UseMove} bodies run on a task, see
 * {@code event.HiddenMoveTask}). The handlers answer with the line the plugin would show when {@code showmsg} is on;
 * the caller shows it.
 */
public final class HiddenMoves {
    /** The ids with a handler: 179_PField_FieldMoves plus 202_Pokemon_Chatter. */
    private static final java.util.Set<String> MOVES = new java.util.HashSet<>(Arrays.asList(
            "CUT", "DIG", "DIVE", "FLASH", "FLY", "HEADBUTT", "ROCKSMASH", "STRENGTH", "SURF", "SWEETSCENT",
            "TELEPORT", "WATERFALL", "DEFOG", "CHATTER"));
    /** 335_001_ESMM_Config:10-17 {@code BAN_MAPS}: Fly (and the other map-jumping items) are refused here. */
    public static final int[] BAN_MAPS = {
        1, 119, 224, 292, 293, 294, 295, 296, 347, 60, 209, 210, 228, 297, 321, 371, 140, 441, 442, 444,
        417, 440, 443, 445, 446, 447, 448, 450, 451, 478, 485, 388, 462};
    /** 179_PField_FieldMoves:875 the maps Teleport refuses. */
    private static final int[] NO_TELEPORT_MAPS = {60, 226, 207, 321, 322, 209, 210};
    /** 000_Settings:68. */
    public static final boolean DIVING_SURFACE_ANYWHERE = false;

    private HiddenMoves() {
    }

    /** What the handlers ask the field about. */
    public interface World {
        /** {@code $game_player.pbFacingEvent.name.downcase}, or null when no event is in front. */
        String facingEventName();

        /** {@code pbFacingTerrainTag}. */
        int facingTerrainTag();

        /** {@code $game_player.terrain_tag}. */
        int playerTerrainTag();

        /** {@code $game_map.passable?($game_player.x, $game_player.y, $game_player.direction, $game_player)}. */
        boolean facingPassable();

        /** {@code $game_player.pbHasDependentEvents?} (297_Follower_Main:964: the follower alone does not count). */
        boolean hasDependentEvents();

        /** {@code $game_map.map_id}. */
        int mapId();

        /** {@code pbGetMetadata($game_map.map_id, MetadataOutdoor)}. */
        boolean outdoor();

        /** {@code $MapFactory.getTerrainTag(map, $game_player.x, $game_player.y)}. */
        int terrainTagOn(int map);

        /** {@code pbGetMapNameFromId(id)}. */
        String mapName(int id);
    }

    /** A handler's answer: whether the move can be used, and the line to show when it cannot. */
    public static final class Check {
        public final boolean ok;
        /** The line ({@code pbMessage}) or null. */
        public final String message;
        /** True when the plugin shows the line whatever {@code showmsg} says. */
        public final boolean always;

        Check(boolean ok, String message, boolean always) {
            this.ok = ok;
            this.message = message;
            this.always = always;
        }
    }

    private static final Check YES = new Check(true, null, false);
    private static final Check NO = new Check(false, null, false);

    private static Check no(String message) {
        return new Check(false, message, false);
    }

    /** {@code HiddenMoveHandlers.hasHandler(move)}. */
    public static boolean hasHandler(String move) {
        return move != null && MOVES.contains(move);
    }

    /** {@code pbCheckHiddenMoveBadge(badge, showmsg)} (:63-72) as a check ($DEBUG is false). */
    private static Check badge(GameState state, int badge) {
        if (badge < 0) {
            return YES;
        }
        boolean ok = FieldMoves.FIELD_MOVES_COUNT_BADGES ? state.trainer().badges.size() >= badge
                : state.trainer().badges.contains(badge);
        return ok ? YES : no("对不起，\n这需要拥有对应的徽章。");
    }

    private static boolean contains(int[] list, int value) {
        for (int entry : list) {
            if (entry == value) {
                return true;
            }
        }
        return false;
    }

    private static PbsData.Metadata metadata(PbsData pbs, int mapId) {
        return pbs == null ? null : pbs.mapMetadata(mapId);
    }

    /** The map whose {@code DiveMap} is {@code mapId} ({@code pbSurfacing}'s search through {@code pbLoadMetadata}), or -1. */
    public static int divemapFor(PbsData pbs, int mapId) {
        if (pbs == null) {
            return -1;
        }
        for (int id : pbs.metadataMapIds()) {
            PbsData.Metadata meta = pbs.mapMetadata(id);
            if (meta != null && meta.diveMap >= 0 && meta.diveMap == mapId) {
                return id;
            }
        }
        return -1;
    }

    /** {@code healing = $PokemonGlobal.healingSpot; healing = pbGetMetadata(0, MetadataHome) if !healing}. */
    public static int[] healingSpot(PbsData pbs, GameState state) {
        int[] healing = state.fieldGlobals().healingSpot;
        if (healing == null) {
            PbsData.Metadata global = pbs == null ? null : pbs.globalMetadata();
            healing = global == null ? null : global.home;
        }
        return healing;
    }

    /** {@code HiddenMoveHandlers::CanUseMove[move].call(move, pkmn, showmsg)}. */
    public static Check canUse(String move, Pokemon pkmn, GameState state, PbsData pbs, World world) {
        if (!hasHandler(move)) {
            return NO;                                                              // :27 return false if !CanUseMove[item]
        }
        switch (move) {
            case "CUT": {                                                           // :210-218
                Check badge = badge(state, FieldMoves.BADGE_FOR_CUT);
                if (!badge.ok) return badge;
                String facing = world.facingEventName();
                return facing != null && facing.equals("tree") ? YES : no("不能在这里使用。");
            }
            case "DIG": {                                                           // :255-266
                if (state.fieldGlobals().escapePoint.length == 0) return no("不能在这里使用。");
                if (world.hasDependentEvents()) return no("与他人同行时不能使用。");
                return YES;
            }
            case "DIVE": {                                                          // :401-429
                Check badge = badge(state, FieldMoves.BADGE_FOR_DIVE);
                if (!badge.ok) return badge;
                if (!state.inventory().has("HM06")) return NO;                      // :404 (no line)
                if (state.fieldGlobals().diving) {
                    if (DIVING_SURFACE_ANYWHERE) return YES;                        // :406
                    int divemap = divemapFor(pbs, world.mapId());
                    if (!PBTerrain.isDeepWater(world.terrainTagOn(divemap))) {
                        return no("不能在这里使用。");                                // :414-417
                    }
                } else {
                    PbsData.Metadata meta = metadata(pbs, world.mapId());
                    if (meta == null || meta.diveMap < 0) return no("不能在这里使用。");     // :419-422
                    if (!PBTerrain.isDeepWater(world.playerTerrainTag())) return no("不能在这里使用。");   // :423-426
                }
                return YES;
            }
            case "FLASH": {                                                         // :466-477
                Check badge = badge(state, FieldMoves.BADGE_FOR_FLASH);
                if (!badge.ok) return badge;
                PbsData.Metadata meta = metadata(pbs, world.mapId());
                if (meta == null || !meta.darkMap) return no("不能在这里使用。");
                if (state.fieldGlobals().flashUsed) return no("已经使用闪光了。");
                return YES;
            }
            case "FLY": {                                                           // :502-513
                if (world.hasDependentEvents()) return no("与他人同行时不能使用。");
                if (!world.outdoor() || contains(BAN_MAPS, world.mapId())) return no("不能在这里使用。");
                return YES;
            }
            case "HEADBUTT": {                                                      // :582-589
                String facing = world.facingEventName();
                return facing != null && facing.equals("headbutttree") ? YES : no("不能在这里使用。");
            }
            case "ROCKSMASH": {                                                     // :625-633
                Check badge = badge(state, FieldMoves.BADGE_FOR_ROCKSMASH);
                if (!badge.ok) return badge;
                String facing = world.facingEventName();
                return facing != null && facing.equals("rock") ? YES : no("不能在这里使用。");
            }
            case "STRENGTH": {                                                      // :679-686
                Check badge = badge(state, FieldMoves.BADGE_FOR_STRENGTH);
                if (!badge.ok) return badge;
                return state.pokemonMapStrengthUsed() ? no("已经使用怪力了。") : YES;
            }
            case "SURF": {                                                          // :774-794
                Check badge = badge(state, FieldMoves.BADGE_FOR_SURF);
                if (!badge.ok) return badge;
                if (state.fieldGlobals().surfing) return no("已经使用冲浪了。");
                if (world.hasDependentEvents()) return no("与他人同行时不能使用。");
                if (new Vehicles(pbs, state).bicycleAlways(world.mapId())) return no("享受骑车的乐趣吧！");
                if (!PBTerrain.isSurfable(world.facingTerrainTag()) || !world.facingPassable()) {
                    return no("这里不能冲浪！");
                }
                return YES;
            }
            case "SWEETSCENT":                                                      // :847-849
            case "CHATTER":                                                         // 202_Pokemon_Chatter:31-33
                return YES;
            case "TELEPORT": {                                                      // :864-884
                if (!world.outdoor()) return no("不能在这里使用。");
                if (healingSpot(pbs, state) == null) return no("不能在这里使用。");
                if (contains(NO_TELEPORT_MAPS, world.mapId())) return new Check(false, "这里不能使用。", true);   // :876 (no showmsg guard)
                if (world.hasDependentEvents()) return no("与他人同行时不能使用。");
                return YES;
            }
            case "WATERFALL": {                                                     // :983-990
                Check badge = badge(state, FieldMoves.BADGE_FOR_WATERFALL);
                if (!badge.ok) return badge;
                return world.facingTerrainTag() == PBTerrain.WATERFALL ? YES : no("不能在这里使用。");
            }
            case "DEFOG":                                                           // :1007-1010 (no line)
                return state.weather().type() == pokemon.runtime.state.ScreenWeather.FOG ? YES : NO;
            default:
                return NO;
        }
    }

    /**
     * {@code HiddenMoveHandlers.triggerConfirmUseMove}: the question some moves ask before they are used (Dig, Teleport);
     * null when the handler has none ({@code return true if !ConfirmUseMove[item]}), and an empty string when the handler
     * answers false without asking.
     */
    public static String confirmQuestion(String move, GameState state, PbsData pbs, World world) {
        if ("DIG".equals(move)) {                                                   // :268-273
            int[] escape = state.fieldGlobals().escapePoint;
            return escape.length == 0 ? "" : "想从这里出去回到" + world.mapName(escape[0]) + "吗？";
        }
        if ("TELEPORT".equals(move)) {                                              // :886-892
            int[] healing = healingSpot(pbs, state);
            return healing == null ? "" : "想要回到" + world.mapName(healing[0]) + "吗？";
        }
        return null;
    }
}
