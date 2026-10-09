package pokemon.runtime.event;

import pokemon.runtime.field.PBDayNight;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.GameState;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * The atoms the project's script conditions read ({@link ScriptCondition.Env}): game switches / variables / self
 * switches, the trainer (badges, party), the bag, {@code $PokemonGlobal}, the day / night, and the helper functions of
 * 170_PField_Field / 252_PSystem_PokemonUtilities that return a truth value. Atoms that need the Day Care, the Safari
 * Zone or a screen throw {@link ScriptCondition.Unsupported}.
 */
final class ConditionEnv implements ScriptCondition.Env {
    /** 000_Settings:97. */
    static final int MAX_COINS = 99_999;
    /** 000_Settings:318. */
    private static final int SEEN_POKERUS_SWITCH = 2;

    private final GameState state;
    private final PbsData pbs;
    private final IntSupplier mapId;
    private final IntSupplier eventId;
    private final Map<String, Object> instanceVariables;
    private final Supplier<LocalTime> clock;
    private final pokemon.runtime.field.FieldScene scene;
    private final pokemon.runtime.field.FieldMoves fieldMoves;
    private MapPort mapPort;

    ConditionEnv withMapPort(MapPort port) {
        this.mapPort = port;
        return this;
    }

    ConditionEnv(GameState state, PbsData pbs, IntSupplier mapId, IntSupplier eventId,
                 Map<String, Object> instanceVariables, Supplier<LocalTime> clock,
                 pokemon.runtime.field.FieldScene scene) {
        this.state = state;
        this.pbs = pbs;
        this.mapId = mapId;
        this.eventId = eventId;
        this.instanceVariables = instanceVariables;
        this.clock = clock;
        this.scene = scene;
        this.fieldMoves = new pokemon.runtime.field.FieldMoves(pbs, state);
    }

    @Override
    public Object variable(String name) {
        switch (name) {
            case "$game_switches": case "$game_variables": case "$game_self_switches": case "$game_player":
            case "$game_map": case "$Trainer": case "$PokemonBag": case "$PokemonGlobal": case "$PokemonMap":
                return name;
            default:
                if (name.startsWith("@")) {
                    return instanceVariables.get(name);                          // @ch_ret / @ch_cmd (Interpreter ivars)
                }
                throw new ScriptCondition.Unsupported("variable " + name);
        }
    }

    @Override
    public Object constant(String path) {
        switch (path) {
            case "MAX_COINS":
                return MAX_COINS;
            default:
                if (path.contains("::")) {
                    return path.substring(path.indexOf("::") + 2);               // PBMoves::CUT -> "CUT"
                }
                return path;                                                    // a module such as PBDayNight
        }
    }

    private static int toInt(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private Pokemon partyMember(Object index) {
        return state.trainer().party.get(toInt(index));
    }

    @Override
    public Object call(Object receiver, String method, List<Object> args) {
        if (receiver == ScriptCondition.NIL) {
            throw new IllegalStateException("undefined method '" + method + "' for nil");
        }
        if (receiver instanceof String) {
            return callOn((String) receiver, method, args);
        }
        if (receiver instanceof Pokemon) {
            Pokemon p = (Pokemon) receiver;
            switch (method) {
                case "isEgg?": case "egg?": return p.egg;
                case "isShadow?": case "shadowPokemon?": return false;        // 登记: shadow Pokemon are not modelled
                case "fainted?": return p.hp <= 0;
                default: break;
            }
        }
        if (receiver != null) {
            throw new ScriptCondition.Unsupported("." + method + " on " + receiver);
        }
        return function(method, args);
    }

    private Object callOn(String receiver, String method, List<Object> args) {
        switch (receiver) {
            case "$game_switches":
                return state.switches().get(Math.max(1, toInt(args.get(0))));
            case "$game_variables":
                return state.variables().get(Math.max(1, toInt(args.get(0))));
            case "$game_self_switches": {
                Object key = args.get(0);
                if (key instanceof List && ((List<?>) key).size() == 3) {
                    List<?> k = (List<?>) key;
                    return state.selfSwitches().get(toInt(k.get(0)), toInt(k.get(1)), String.valueOf(k.get(2)));
                }
                throw new ScriptCondition.Unsupported("$game_self_switches key");
            }
            case "$game_player":
                if (method.equals("x")) return state.playerX();
                if (method.equals("y")) return state.playerY();
                if (method.equals("direction")) return state.playerDirection();
                break;
            case "$game_map":
                if (method.equals("map_id")) return state.currentMapId();
                break;
            case "$Trainer":
                return trainer(method, args);
            case "$party":
                if (method.equals("length") || method.equals("size")) return state.trainer().party.size();
                break;
            case "$badges":
                if (method.equals("[]")) return state.trainer().badges.contains(toInt(args.get(0)));
                break;
            case "$PokemonBag":
                return bag(method, args);
            case "$PokemonGlobal":
                switch (method) {
                    case "followerToggled": return state.followerToggled();
                    case "coins": return state.fieldGlobals().coins;
                    case "bicycle": return state.fieldGlobals().bicycle;
                    case "surfing": return state.fieldGlobals().surfing;
                    case "diving": return state.fieldGlobals().diving;
                    case "repel": return state.fieldGlobals().repel;
                    default: break;
                }
                break;
            case "$PokemonMap":
                if (method.equals("strengthUsed")) return state.pokemonMapStrengthUsed();
                break;
            case "PBDayNight":
                return dayNight(method);
            default:
                break;
        }
        throw new ScriptCondition.Unsupported(receiver + "." + method);
    }

    private Object trainer(String method, List<Object> args) {
        switch (method) {
            case "numbadges": return state.trainer().badges.size();
            case "badges": return "$badges";
            case "party": return "$party";
            case "partyCount": return state.trainer().party.size();
            case "pokemonCount": return state.trainer().pokemonCount();
            case "ablePokemonCount": {
                int count = 0;
                for (Pokemon p : state.trainer().party.members()) {
                    if (!p.egg && p.hp > 0) count++;
                }
                return count;
            }
            case "id": return state.trainer().id;
            case "publicID": return state.trainer().publicID();
            case "money": return state.trainer().money;
            case "name": return state.trainer().name;
            default:
                throw new ScriptCondition.Unsupported("$Trainer." + method);
        }
    }

    private Object bag(String method, List<Object> args) {
        String item = args.isEmpty() ? null : String.valueOf(args.get(0));
        switch (method) {
            case "pbHasItem?": return state.inventory().has(item);
            case "pbQuantity": return state.inventory().count(item);
            case "pbCanStore?":
                // 195_PItem_Bag:90-: the pockets are unbounded (BAG_MAX_POCKET_SIZE is -1), so an item always fits.
                return true;
            default:
                throw new ScriptCondition.Unsupported("$PokemonBag." + method);
        }
    }

    private Object dayNight(String method) {
        LocalTime now = clock.get();
        switch (method) {
            case "isDay?": return PBDayNight.isDay(now);
            case "isNight?": return PBDayNight.isNight(now);
            case "isDawn?": return PBDayNight.isDawn(now);
            case "isMorning?": return PBDayNight.isMorning(now);
            case "isBeforeNoon?": return PBDayNight.isBeforeNoon(now);
            case "isAtNoon?": return PBDayNight.isAtNoon(now);
            case "isAfternoon?": return PBDayNight.isAfternoon(now);
            case "isDusk?": return PBDayNight.isDusk(now);
            case "isEvening?": return PBDayNight.isEvening(now);
            case "isMidnight?": return PBDayNight.isMidnight(now);
            default:
                throw new ScriptCondition.Unsupported("PBDayNight." + method);
        }
    }

    /** The atoms that talk to the player run on a task with a screen. */
    static final java.util.Set<String> SCREEN_CALLS = new java.util.HashSet<>(
            java.util.Arrays.asList("pbCut", "pbRockSmash", "pbStrength", "pbMoveTutorChoose", "pbSurf", "pbWaterfall", "pbDive",
                    "pbSurfacing", "pbRelearnMoveScreen"));

    private boolean needScene(String name) {
        if (scene == null) {
            throw new ScriptCondition.Unsupported(name + " needs a screen");
        }
        return true;
    }

    /** {@code pbDive} / {@code pbSurfacing}: the question, the banner of a Pokemon that knows Dive, then the fade to the other map. */
    private boolean diveOrSurface(boolean dive) {
        int here = mapId.getAsInt();
        int divemap;
        if (dive) {
            pokemon.runtime.pokemon.PbsData.Metadata meta = pbs == null ? null : pbs.mapMetadata(here);
            divemap = meta == null ? -1 : meta.diveMap;                         // :430
        } else {
            if (!state.fieldGlobals().diving) return false;                     // :467
            divemap = pokemon.runtime.field.HiddenMoves.divemapFor(pbs, here);  // :468-474
        }
        if (divemap < 0) return false;
        boolean hm = state.inventory().has("HM06");                             // :433
        if (!fieldMoves.pbCheckHiddenMoveBadge(pokemon.runtime.field.FieldMoves.BADGE_FOR_DIVE, false, scene) || !hm) {
            scene.pbMessage(dive ? "这里的水很深，\n宝可梦也许可以潜入水下。" : "光从上方洒下，\n宝可梦也许可以浮出水面。");   // :435/:479
            return false;
        }
        if (!scene.pbConfirmMessage(dive ? "这里的水很深，要使用潜水吗？" : "光从上方洒下，要使用潜水吗？")) {       // :438/:482
            return false;
        }
        pokemon.runtime.pokemon.PbsData.Move move = pbs == null ? null : pbs.move("DIVE");
        scene.pbMessage(state.trainer().name + "使用" + (move == null ? "DIVE" : move.name) + "！");   // :440/:484
        pokemon.runtime.pokemon.Pokemon finder = fieldMoves.pbCheckMove("DIVE");                    // :442-447
        final pokemon.runtime.pokemon.Pokemon banner = finder;
        if (mapPort != null) {
            scene.runAction(() -> mapPort.hiddenMoveAnimation(banner));
        }
        final int target = divemap;
        scene.runAction(() -> {
            state.fieldGlobals().surfing = !dive;                               // :453/:497
            state.fieldGlobals().diving = dive;                                 // :454/:498
            if (mapPort != null) {
                mapPort.transferThroughFade(target, state.playerX(), state.playerY(), state.playerDirection(), true);
            }
            return 0f;
        });
        return true;
    }

    private Object function(String name, List<Object> args) {
        switch (name) {
            case "pbGet":                                                       // 170_PField_Field pbGet(id)
                return state.variables().get(Math.max(1, toInt(args.get(0))));
            case "pbGetPokemon":                                                // :811-813
                return state.trainer().party.get(state.variables().get(Math.max(1, toInt(args.get(0)))));
            case "pbPokerus?":                                                  // 170_PField_Field:195-201
                if (state.switches().get(SEEN_POKERUS_SWITCH)) return false;
                for (Pokemon p : state.trainer().party.members()) {
                    if (p.pokerus == 1) return true;
                }
                return false;
            case "pbHasSpecies?": {                                             // 252_PSystem_PokemonUtilities:342-348
                String species = String.valueOf(args.get(0));
                int form = args.size() > 1 ? toInt(args.get(1)) : -1;
                for (Pokemon p : state.trainer().party.members()) {
                    if (!p.egg && p.species != null && p.species.internalName.equals(species)
                            && (form < 0 || form == p.formIndex())) {
                        return true;
                    }
                }
                return false;
            }
            case "pbCheckAble": {                                               // :326-333
                int index = toInt(args.get(0));
                for (int i = 0; i < state.trainer().party.size(); i++) {
                    if (i == index) continue;
                    Pokemon p = state.trainer().party.get(i);
                    if (p != null && !p.egg && p.hp > 0) return true;
                }
                return false;
            }
            case "pbBoxesFull?":                                                // :4-6
                return state.trainer().party.size() == 6 && state.trainer().currentStorage().full();
            case "pbGetSelfSwitch": {                                           // 071_Messages:341-344
                int map = args.size() > 2 && toInt(args.get(2)) >= 0 ? toInt(args.get(2)) : mapId.getAsInt();
                return state.selfSwitches().get(map, toInt(args.get(0)), String.valueOf(args.get(1)));
            }
            case "pbMoveTutorChoose": {                                         // 253_PSystem_Utilities:982-1018
                needScene(name);
                @SuppressWarnings("unchecked")
                List<String> movelist = args.size() > 1 && args.get(1) instanceof List ? (List<String>) args.get(1) : null;
                boolean byMachine = args.size() > 2 && ScriptCondition.truthy(args.get(2));
                int ret = scene.pbMoveTutorChoose(String.valueOf(args.get(0)), movelist, byMachine);
                return ret >= 0 ? (Object) ret : Boolean.FALSE;                 // `return ret if ret`: the index (0 is true in Ruby)
            }
            case "pbHasRelearnableMove?":                                       // 228_PScreen_MoveRelearner:8-10
                return pokemon.runtime.pokemon.MoveRelearner.hasRelearnableMove(
                        args.get(0) instanceof Pokemon ? (Pokemon) args.get(0) : null, pbs);
            case "pbRelearnMoveScreen":                                         // 228_PScreen_MoveRelearner:221-228
                needScene(name);
                return args.get(0) instanceof Pokemon && scene.pbRelearnMoveScreen((Pokemon) args.get(0));
            case "pbSurf": {                                                    // 179_PField_FieldMoves:702-720
                needScene(name);
                if (!fieldMoves.pbCheckHiddenMoveBadge(pokemon.runtime.field.FieldMoves.BADGE_FOR_SURF, false, scene)) {
                    return false;                                               // :707-709
                }
                if (scene.pbConfirmMessage("这里的水蔚蓝蔚蓝的，\n你想要在这里冲浪吗？")) {      // :710
                    pokemon.runtime.pokemon.PbsData.Move surf = pbs == null ? null : pbs.move("SURF");
                    scene.pbMessage(state.trainer().name + "使用了" + (surf == null ? "SURF" : surf.name) + "!");   // :711-712
                    new pokemon.runtime.field.Vehicles(pbs, state).cancelVehicles(null);   // :713 pbCancelVehicles
                    if (mapPort != null) {
                        mapPort.startSurfing();                                 // :714-716 (the surf music follows the flag)
                    }
                    return true;
                }
                return false;
            }
            case "pbWaterfall": {                                               // 179_PField_FieldMoves:957-971
                needScene(name);
                pokemon.runtime.pokemon.Pokemon finder = fieldMoves.pbCheckMove("WATERFALL");        // :959
                boolean hm = state.inventory().has("HM05");
                if (!fieldMoves.pbCheckHiddenMoveBadge(pokemon.runtime.field.FieldMoves.BADGE_FOR_WATERFALL, false, scene)
                        || (finder == null && !hm)) {
                    scene.pbMessage("一道瀑布气势非凡地横挂在眼前。");                     // :961
                    return false;
                }
                if (scene.pbConfirmMessage("这是一道巨大的瀑布，要使用攀瀑吗？")) {      // :964
                    pokemon.runtime.pokemon.PbsData.Move move = pbs == null ? null : pbs.move("WATERFALL");
                    scene.pbMessage(state.trainer().name + "使用了" + (move == null ? "WATERFALL" : move.name) + "!");   // :966
                    if (mapPort != null) {
                        scene.runAction(mapPort::ascendWaterfall);                  // :967 pbAscendWaterfall
                    }
                    return true;
                }
                return false;
            }
            case "pbDive":                                                      // 297_Follower_Main:429-463 (the last pbDive)
            case "pbSurfacing":                                                 // 297_Follower_Main:466-508
                return needScene(name) && diveOrSurface("pbDive".equals(name));
            case "pbCut":                                                       // 179_PField_FieldMoves:194-208
                return needScene(name) && fieldMoves.pbCut(scene);
            case "pbRockSmash":                                                 // :610-623
                return needScene(name) && fieldMoves.pbRockSmash(scene);
            case "pbStrength":                                                  // :652-672
                return needScene(name) && fieldMoves.pbStrength(scene);
            case "isTempSwitchOn?":
                return state.tempSwitches().get(mapId.getAsInt(), Math.max(0, eventId.getAsInt()), String.valueOf(args.get(0)));
            case "isTempSwitchOff?":
                return !state.tempSwitches().get(mapId.getAsInt(), Math.max(0, eventId.getAsInt()), String.valueOf(args.get(0)));
            default:
                throw new ScriptCondition.Unsupported(name);
        }
    }
}
