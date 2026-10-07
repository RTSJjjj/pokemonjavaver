package pokemon.runtime.battle.movefx;

import com.badlogic.gdx.utils.ObjectMap;
import pokemon.runtime.battle.BattleMove;
import pokemon.runtime.pokemon.PbsData;

/**
 * Stage 4 / L2: function code &rarr; {@link MoveEffect}.
 *
 * <p>In the plugin the same lookup is done by name at run time:
 * {@code PokeBattle_Move.pbFromPBMove} (PokeBattle_Move.rb:51-59) builds the
 * class name {@code "PokeBattle_Move_<function code>"} and, when the constant
 * exists, instantiates it; otherwise it returns
 * {@code PokeBattle_UnimplementedMove} (:58).</p>
 *
 * <p>The table is filled by the seven per-file {@code register()} methods
 * ({@link #init}), one per {@code MoveEffects_*} file, in the same order as the
 * plugin's sections. The pseudomoves stay out of it: {@code PokeBattle_Confusion}
 * carries {@code @function = "000"} and {@code PokeBattle_Struggle} {@code "002"}
 * (Move_Effects_Generic.rb:27/59) but both are constructed directly, never
 * through {@code pbFromPBMove} (task-11 decision 13), so {@link #confusion()} and
 * {@link #struggle()} are the only way to reach them - {@code of("000")} is the
 * real move class {@code PokeBattle_Move_000}.</p>
 */
public final class MoveEffectRegistry {

    /** function code &rarr; its class's no-argument constructor; filled by {@link #init()}. */
    private static final ObjectMap<String, java.util.function.Supplier<MoveEffect>> BY_FUNCTION = new ObjectMap<>();

    /** Instantiated strategies, one per code, created on first use (PokeBattle_Move.rb:56). */
    private static final ObjectMap<String, MoveEffect> INSTANCES = new ObjectMap<>();

    /** Guards {@link #init()} - the seven tables are built on first use. */
    private static boolean initialised;

    private MoveEffectRegistry() {
    }

    /**
     * One entry of {@code pbFromPBMove}'s table (PokeBattle_Move.rb:55-57): the
     * function code and the constructor of its class. Called by the seven per-file
     * {@code register()} methods.
     *
     * <p>The constructor is invoked lazily by {@link #of}: Ruby instantiates only
     * the class it is asked for ({@code Object.const_get(className).new(battle,move)}),
     * and several of the 526 constructors still call not-yet-wired stubs, so
     * building them all up front would break the whole table.</p>
     */
    static void register(String function, java.util.function.Supplier<MoveEffect> constructor) {
        BY_FUNCTION.put(function, constructor);
    }

    /**
     * Runs the seven per-file tables in the plugin's section order -
     * {@code Move_Effects_000-07F}, {@code 080-0FF}, {@code 100-17F},
     * {@code 180-1FF} and then the out-of-bounds {@code Arceus} / {@code 场地} /
     * {@code Pokemon_ShadowPokemon} classes - which is also the order in which
     * Ruby's later definitions win (PokeBattle_Move.rb:51-59).
     */
    private static void init() {
        if (initialised) {
            return;
        }
        initialised = true;
        MoveEffects_000_07F.register();
        MoveEffects_080_0AF.register();
        MoveEffects_0B0_0D4.register();
        MoveEffects_0D5_0FF.register();
        MoveEffects_100_17F.register();
        MoveEffects_180_1FF.register();
        MoveEffects_Extra.register();
    }

    /**
     * The effect for a function code; unknown codes get the plugin's
     * {@code PokeBattle_UnimplementedMove}
     * ({@code PokeBattle_Move.rb:58}, {@code Move_Effects_Generic.rb:6-14}).
     */
    public static MoveEffect of(String function) {
        init();
        if (function != null) {
            MoveEffect cached = INSTANCES.get(function);
            if (cached != null) {
                return cached;
            }
            java.util.function.Supplier<MoveEffect> constructor = BY_FUNCTION.get(function);
            if (constructor != null) {
                MoveEffect created = constructor.get();       // :56 Object.const_get(className).new(battle,move)
                INSTANCES.put(function, created);
                return created;
            }
        }
        return MoveEffectsGeneric.PokeBattle_UnimplementedMove.INSTANCE;
    }

    /** The number of registered function codes (for the self-check/report). */
    public static int size() {
        init();
        return BY_FUNCTION.size;
    }

    /** The pseudomove for confusion damage ({@code Move_Effects_Generic.rb:21-45}). */
    public static MoveEffect confusion() {
        return MoveEffectsGeneric.PokeBattle_Confusion.INSTANCE;
    }

    /** The move Struggle ({@code Move_Effects_Generic.rb:53-83}). */
    public static MoveEffect struggle() {
        return MoveEffectsGeneric.PokeBattle_Struggle.INSTANCE;
    }

    /**
     * The synthesized move record of {@code PokeBattle_Confusion#initialize}
     * (Move_Effects_Generic.rb:22-40), one field per Ruby line:
     * {@code @id=0} (:25), {@code @name=""} (:26), {@code @function="000"} (:27),
     * {@code @baseDamage=40} (:28), {@code @type=-1} (:29 - {@code null} here),
     * {@code @category=0} (:30 - {@code "Physical"}), {@code @accuracy=100} (:31),
     * {@code @pp=-1} (:32), {@code @target=0} (:33 - {@code PBTargets::NearOther}
     * is 0), {@code @priority=0} (:34), {@code @flags=""} (:35),
     * {@code @addlEffect=0} (:36).
     */
    public static BattleMove confusionMove() {
        PbsData.Move data = new PbsData.Move();
        data.id = 0;                       // :25 @id = 0
        data.name = "";                    // :26 @name = ""
        data.function = "000";             // :27 @function = "000"
        data.power = 40;                   // :28 @baseDamage = 40
        data.type = null;                  // :29 @type = -1   (null is the plugin's -1)
        data.category = "Physical";        // :30 @category = 0
        data.accuracy = 100;               // :31 @accuracy = 100
        data.pp = -1;                      // :32 @pp = -1
        data.target = "NearOther";         // :33 @target = 0  (PBTargets::NearOther == 0)
        data.priority = 0;                 // :34 @priority = 0
        data.flags = "";                   // :35 @flags = ""
        data.effectChance = 0;             // :36 @addlEffect = 0
        return new BattleMove(data);
    }

    /**
     * The synthesized move record of {@code PokeBattle_Struggle#initialize}
     * (Move_Effects_Generic.rb:54-72), one field per Ruby line:
     * {@code @realMove=nil} (:56), {@code @id=(move) ? move.id : -1} (:57),
     * {@code @name=(move) ? PBMoves.getName(@id) : _INTL("挣扎")} (:58),
     * {@code @function="002"} (:59), {@code @baseDamage=50} (:60),
     * {@code @type=-1} (:61 - {@code null}), {@code @category=0} (:62 -
     * {@code "Physical"}), {@code @accuracy=0} (:63), {@code @pp=-1} (:64),
     * {@code @target=0} (:65), {@code @priority=0} (:66), {@code @flags=""} (:67),
     * {@code @addlEffect=0} (:68).
     *
     * @param move the real Struggle move when the project defines one
     *             ({@code PBMoves.getName(@id)} is that move data's name, i.e.
     *             {@link BattleMove#name()}), or {@code null} to use
     *             {@code "挣扎"}
     */
    public static BattleMove struggleMove(BattleMove move) {
        PbsData.Move data = new PbsData.Move();
        data.id = move != null ? move.id() : -1;                           // :57 @id = (move) ? move.id : -1
        data.name = move != null ? move.name() : "挣扎";                     // :58 @name = (move) ? PBMoves.getName(@id) : _INTL("挣扎")
        data.function = "002";             // :59 @function = "002"
        data.power = 50;                   // :60 @baseDamage = 50
        data.type = null;                  // :61 @type = -1
        data.category = "Physical";        // :62 @category = 0
        data.accuracy = 0;                 // :63 @accuracy = 0
        data.pp = -1;                      // :64 @pp = -1
        data.target = "NearOther";         // :65 @target = 0
        data.priority = 0;                 // :66 @priority = 0
        data.flags = "";                   // :67 @flags = ""
        data.effectChance = 0;             // :68 @addlEffect = 0
        return new BattleMove(data);
    }
}
