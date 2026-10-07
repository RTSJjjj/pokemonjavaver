package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;

import java.util.List;

/**
 * Stage 4 / M0: {@code BattleHandlers} (BattleHandlers.rb:1-675), transcribed
 * line by line.
 *
 * <h2>What the Ruby section contains</h2>
 * <ul>
 * <li>{@code BattleHandlers.rb:3-105}: <b>86</b> {@code HandlerHash} declarations
 *     for <b>85</b> distinct group names. {@code ItemOnOpposingStatGain} is
 *     declared TWICE - {@code :28} and {@code :92} - and the later declaration
 *     wins ({@code Event_Handlers.rb:110-113} is a plain {@code @hash[id] =
 *     handler} assignment, and the two declarations are {@code AbilityHandlerHash}
 *     / {@code ItemHandlerHash} instances with no registrations in between, so no
 *     handler is lost). Java has one {@code static final} field per name, so the
 *     single field below carries the javadoc for both lines. ({@code :93} is
 *     {@code CertainSwitchingUserItem}, NOT a third declaration.)</li>
 * <li>{@code BattleHandlers.rb:109-527}: <b>85</b> {@code def self.trigger*}
 *     wrappers - one per distinct group, in declaration order.</li>
 * <li>{@code BattleHandlers.rb:533-542}: the 9 array-index constants.</li>
 * <li>{@code BattleHandlers.rb:544-675}: the 7 top-level {@code pbBattle*}
 *     helpers, transcribed in {@link BattleHandlerHelpers}.</li>
 * </ul>
 *
 * <h2>Handler signature (why the first parameter is a String)</h2>
 * Ruby's {@code HandlerHash#trigger(sym,*args)} calls
 * {@code handler.call(fromSymbol(sym),*args)} ({@code Event_Handlers.rb:138-141}),
 * so a handler receives the resolved id first, then the wrapper's own arguments.
 * This runtime keys abilities and items by their internal name String
 * ({@link HandlerHash}'s "Documented deviation"), so each interface below takes
 * that String first - exactly the {@code sym} that was looked up.
 *
 * <h2>Documented deviations from the Ruby types</h2>
 * <ul>
 * <li>{@code mults} / {@code mods} are {@code float[]}, not {@code int[]}: the
 *     plugin does floating-point arithmetic on them
 *     ({@code mults[BASE_DMG_MULT] *= 1.3} BattleHandlers.rb:639,
 *     {@code *= 1.5} :641, {@code (mults[FINAL_DMG_MULT]/2).round} :651,
 *     {@code mods[ACC_MULT] *= 1.1} BattleHandlers_Abilities:814/849).</li>
 * <li>{@code status} is the numeric {@code PBStatuses} id, as in Ruby
 *     ({@code isConst?(status,PBStatuses,:POISON)}); the battle state stores the
 *     matching internal-name String, so the one conversion point is
 *     {@code PBStatuses.idOf(battler.status)}.</li>
 * <li>{@code type} / {@code immuneType} / the {@code pbBattleGem} {@code type}
 *     are the internal-name String: this runtime identifies a type by its name
 *     ({@code BattleMove.type()} returns String, {@code Battler.hasType(String)},
 *     {@code PbsData.TypeInfo.internalName}).</li>
 * </ul>
 *
 * <h2>Initialisation order is semantic</h2>
 * {@link #init()} runs {@link BattleHandlerRegistry} once, in Ruby section
 * order, because {@code add} overwrites an existing entry and the sections after
 * {@code BattleHandlers_Abilities}/{@code _Items} ({@code Arceus},
 * {@code 场地}) really do override main-table entries. Java's own static
 * initialisation order is not controllable, so the order is fixed by code, not
 * by field order. Every {@code trigger*} wrapper calls {@link #init()} first.
 */
public final class BattleHandlers {

    private BattleHandlers() {
    }

    // =====================================================================
    // Array-index constants (BattleHandlers.rb:533-542)
    // =====================================================================

    /** {@code BASE_ACC} (BattleHandlers.rb:533). */
    public static final int BASE_ACC = 0;
    /** {@code ACC_STAGE} (BattleHandlers.rb:534). */
    public static final int ACC_STAGE = 1;
    /** {@code EVA_STAGE} (BattleHandlers.rb:535). */
    public static final int EVA_STAGE = 2;
    /** {@code ACC_MULT} (BattleHandlers.rb:536). */
    public static final int ACC_MULT = 3;
    /** {@code EVA_MULT} (BattleHandlers.rb:537). */
    public static final int EVA_MULT = 4;

    /** {@code BASE_DMG_MULT} (BattleHandlers.rb:539). */
    public static final int BASE_DMG_MULT = 0;
    /** {@code ATK_MULT} (BattleHandlers.rb:540). */
    public static final int ATK_MULT = 1;
    /** {@code DEF_MULT} (BattleHandlers.rb:541). */
    public static final int DEF_MULT = 2;
    /** {@code FINAL_DMG_MULT} (BattleHandlers.rb:542). */
    public static final int FINAL_DMG_MULT = 3;

    // =====================================================================
    // The 86 declarations (85 distinct names) - BattleHandlers.rb:3-105
    // =====================================================================

    // {# Battler's speed calculation} (BattleHandlers.rb:2)
    /** {@code SpeedCalcAbility} (BattleHandlers.rb:3). */
    public static final HandlerHash<SpeedCalcAbility> SpeedCalcAbility = new HandlerHash<>();
    /** {@code SpeedCalcItem} (BattleHandlers.rb:4). */
    public static final HandlerHash<SpeedCalcItem> SpeedCalcItem = new HandlerHash<>();

    // {# Battler's weight calculation} (BattleHandlers.rb:5)
    /** {@code WeightCalcAbility} (BattleHandlers.rb:6). */
    public static final HandlerHash<WeightCalcAbility> WeightCalcAbility = new HandlerHash<>();
    /** {@code WeightCalcItem} (BattleHandlers.rb:7, {@code # Float Stone}). */
    public static final HandlerHash<WeightCalcItem> WeightCalcItem = new HandlerHash<>();

    // {# Battler's HP changed} (BattleHandlers.rb:8)
    /** {@code HPHealItem} (BattleHandlers.rb:9). */
    public static final HandlerHash<HPHealItem> HPHealItem = new HandlerHash<>();
    /** {@code AbilityOnHPDroppedBelowHalf} (BattleHandlers.rb:10). */
    public static final HandlerHash<AbilityOnHPDroppedBelowHalf> AbilityOnHPDroppedBelowHalf = new HandlerHash<>();

    // {# Battler's status problem} (BattleHandlers.rb:11)
    /** {@code StatusCheckAbilityNonIgnorable} (BattleHandlers.rb:12, {@code # Comatose}). */
    public static final HandlerHash<StatusCheckAbilityNonIgnorable> StatusCheckAbilityNonIgnorable = new HandlerHash<>();
    /** {@code StatusImmunityAbility} (BattleHandlers.rb:13). */
    public static final HandlerHash<StatusImmunityAbility> StatusImmunityAbility = new HandlerHash<>();
    /** {@code StatusImmunityAbilityNonIgnorable} (BattleHandlers.rb:14). */
    public static final HandlerHash<StatusImmunityAbilityNonIgnorable> StatusImmunityAbilityNonIgnorable = new HandlerHash<>();
    /** {@code StatusImmunityAllyAbility} (BattleHandlers.rb:15). */
    public static final HandlerHash<StatusImmunityAllyAbility> StatusImmunityAllyAbility = new HandlerHash<>();
    /** {@code AbilityOnStatusInflicted} (BattleHandlers.rb:16, {@code # Synchronize}). */
    public static final HandlerHash<AbilityOnStatusInflicted> AbilityOnStatusInflicted = new HandlerHash<>();
    /** {@code StatusCureItem} (BattleHandlers.rb:17). */
    public static final HandlerHash<StatusCureItem> StatusCureItem = new HandlerHash<>();
    /** {@code StatusCureAbility} (BattleHandlers.rb:18). */
    public static final HandlerHash<StatusCureAbility> StatusCureAbility = new HandlerHash<>();
    /** {@code AbilityOnInflictingStatus} (BattleHandlers.rb:19, {@code # Poison Puppeteer}). */
    public static final HandlerHash<AbilityOnInflictingStatus> AbilityOnInflictingStatus = new HandlerHash<>();

    // {# Battler's stat stages} (BattleHandlers.rb:20)
    /** {@code StatGainImmunityAbility} (BattleHandlers.rb:21). */
    public static final HandlerHash<StatGainImmunityAbility> StatGainImmunityAbility = new HandlerHash<>();
    /** {@code StatLossImmunityAbility} (BattleHandlers.rb:22). */
    public static final HandlerHash<StatLossImmunityAbility> StatLossImmunityAbility = new HandlerHash<>();
    /** {@code StatLossImmunityAbilityNonIgnorable} (BattleHandlers.rb:23, {@code # Full Metal Body}). */
    public static final HandlerHash<StatLossImmunityAbilityNonIgnorable> StatLossImmunityAbilityNonIgnorable = new HandlerHash<>();
    /** {@code StatLossImmunityAllyAbility} (BattleHandlers.rb:24, {@code # Flower Veil}). */
    public static final HandlerHash<StatLossImmunityAllyAbility> StatLossImmunityAllyAbility = new HandlerHash<>();
    /** {@code AbilityOnStatGain} (BattleHandlers.rb:25, {@code # None!}). */
    public static final HandlerHash<AbilityOnStatGain> AbilityOnStatGain = new HandlerHash<>();
    /** {@code AbilityOnStatLoss} (BattleHandlers.rb:26). */
    public static final HandlerHash<AbilityOnStatLoss> AbilityOnStatLoss = new HandlerHash<>();
    /** {@code AbilityModifyTypeEffectiveness} (BattleHandlers.rb:27, {@code # Tera Shell (damage)}). */
    public static final HandlerHash<AbilityModifyTypeEffectiveness> AbilityModifyTypeEffectiveness = new HandlerHash<>();
    /**
     * {@code ItemOnOpposingStatGain} - declared TWICE by the plugin:
     * {@code BattleHandlers.rb:28} and {@code :92} ({@code # Opportunist}), both
     * {@code ItemHandlerHash.new}. The later declaration overwrites the earlier
     * ({@code Event_Handlers.rb:110-113} assigns into {@code @hash} directly), so
     * {@code :92} wins; Java keeps a single field. Nothing is registered between
     * the two lines, so the earlier table is never populated.
     */
    public static final HandlerHash<ItemOnOpposingStatGain> ItemOnOpposingStatGain = new HandlerHash<>();
    /** {@code AbilityOnMoveSuccessCheck} (BattleHandlers.rb:29, {@code # Tera Shell (display)}). */
    public static final HandlerHash<AbilityOnMoveSuccessCheck> AbilityOnMoveSuccessCheck = new HandlerHash<>();
    /** {@code StatLossImmunityItem} (BattleHandlers.rb:30, {@code # Clear Amulet}). */
    public static final HandlerHash<StatLossImmunityItem> StatLossImmunityItem = new HandlerHash<>();

    // {# Priority and turn order} (BattleHandlers.rb:31)
    /** {@code PriorityChangeAbility} (BattleHandlers.rb:32). */
    public static final HandlerHash<PriorityChangeAbility> PriorityChangeAbility = new HandlerHash<>();
    /** {@code PriorityBracketChangeAbility} (BattleHandlers.rb:33, {@code # Stall}). */
    public static final HandlerHash<PriorityBracketChangeAbility> PriorityBracketChangeAbility = new HandlerHash<>();
    /** {@code PriorityBracketChangeItem} (BattleHandlers.rb:34). */
    public static final HandlerHash<PriorityBracketChangeItem> PriorityBracketChangeItem = new HandlerHash<>();
    /** {@code PriorityBracketUseAbility} (BattleHandlers.rb:35, {@code # None!}). */
    public static final HandlerHash<PriorityBracketUseAbility> PriorityBracketUseAbility = new HandlerHash<>();
    /** {@code PriorityBracketUseItem} (BattleHandlers.rb:36). */
    public static final HandlerHash<PriorityBracketUseItem> PriorityBracketUseItem = new HandlerHash<>();

    // {# Move usage failures} (BattleHandlers.rb:37)
    /** {@code AbilityOnFlinch} (BattleHandlers.rb:38, {@code # Steadfast}). */
    public static final HandlerHash<AbilityOnFlinch> AbilityOnFlinch = new HandlerHash<>();
    /** {@code MoveBlockingAbility} (BattleHandlers.rb:39). */
    public static final HandlerHash<MoveBlockingAbility> MoveBlockingAbility = new HandlerHash<>();
    /** {@code MoveImmunityTargetAbility} (BattleHandlers.rb:40). */
    public static final HandlerHash<MoveImmunityTargetAbility> MoveImmunityTargetAbility = new HandlerHash<>();

    // {# Move usage} (BattleHandlers.rb:41)
    /** {@code MoveBaseTypeModifierAbility} (BattleHandlers.rb:42). */
    public static final HandlerHash<MoveBaseTypeModifierAbility> MoveBaseTypeModifierAbility = new HandlerHash<>();

    // {# Accuracy calculation} (BattleHandlers.rb:43)
    /** {@code AccuracyCalcUserAbility} (BattleHandlers.rb:44). */
    public static final HandlerHash<AccuracyCalcUserAbility> AccuracyCalcUserAbility = new HandlerHash<>();
    /** {@code AccuracyCalcUserAllyAbility} (BattleHandlers.rb:45, {@code # Victory Star}). */
    public static final HandlerHash<AccuracyCalcUserAllyAbility> AccuracyCalcUserAllyAbility = new HandlerHash<>();
    /** {@code AccuracyCalcTargetAbility} (BattleHandlers.rb:46). */
    public static final HandlerHash<AccuracyCalcTargetAbility> AccuracyCalcTargetAbility = new HandlerHash<>();
    /** {@code AccuracyCalcUserItem} (BattleHandlers.rb:47). */
    public static final HandlerHash<AccuracyCalcUserItem> AccuracyCalcUserItem = new HandlerHash<>();
    /** {@code AccuracyCalcTargetItem} (BattleHandlers.rb:48). */
    public static final HandlerHash<AccuracyCalcTargetItem> AccuracyCalcTargetItem = new HandlerHash<>();

    // {# Damage calculation} (BattleHandlers.rb:49)
    /** {@code DamageCalcUserAbility} (BattleHandlers.rb:50). */
    public static final HandlerHash<DamageCalcUserAbility> DamageCalcUserAbility = new HandlerHash<>();
    /** {@code DamageCalcUserAllyAbility} (BattleHandlers.rb:51). */
    public static final HandlerHash<DamageCalcUserAllyAbility> DamageCalcUserAllyAbility = new HandlerHash<>();
    /** {@code DamageCalcTargetAbility} (BattleHandlers.rb:52). */
    public static final HandlerHash<DamageCalcTargetAbility> DamageCalcTargetAbility = new HandlerHash<>();
    /** {@code DamageCalcTargetAbilityNonIgnorable} (BattleHandlers.rb:53). */
    public static final HandlerHash<DamageCalcTargetAbilityNonIgnorable> DamageCalcTargetAbilityNonIgnorable = new HandlerHash<>();
    /** {@code DamageCalcTargetAllyAbility} (BattleHandlers.rb:54). */
    public static final HandlerHash<DamageCalcTargetAllyAbility> DamageCalcTargetAllyAbility = new HandlerHash<>();
    /** {@code DamageCalcUserItem} (BattleHandlers.rb:55). */
    public static final HandlerHash<DamageCalcUserItem> DamageCalcUserItem = new HandlerHash<>();
    /** {@code DamageCalcTargetItem} (BattleHandlers.rb:56). */
    public static final HandlerHash<DamageCalcTargetItem> DamageCalcTargetItem = new HandlerHash<>();

    // {# Critical hit calculation} (BattleHandlers.rb:57)
    /** {@code CriticalCalcUserAbility} (BattleHandlers.rb:58). */
    public static final HandlerHash<CriticalCalcUserAbility> CriticalCalcUserAbility = new HandlerHash<>();
    /** {@code CriticalCalcTargetAbility} (BattleHandlers.rb:59). */
    public static final HandlerHash<CriticalCalcTargetAbility> CriticalCalcTargetAbility = new HandlerHash<>();
    /** {@code CriticalCalcUserItem} (BattleHandlers.rb:60). */
    public static final HandlerHash<CriticalCalcUserItem> CriticalCalcUserItem = new HandlerHash<>();
    /** {@code CriticalCalcTargetItem} (BattleHandlers.rb:61, {@code # None!}). */
    public static final HandlerHash<CriticalCalcTargetItem> CriticalCalcTargetItem = new HandlerHash<>();

    // {# Upon a move hitting a target} (BattleHandlers.rb:62)
    /** {@code TargetAbilityOnHit} (BattleHandlers.rb:63). */
    public static final HandlerHash<TargetAbilityOnHit> TargetAbilityOnHit = new HandlerHash<>();
    /** {@code UserAbilityOnHit} (BattleHandlers.rb:64, {@code # Poison Touch}). */
    public static final HandlerHash<UserAbilityOnHit> UserAbilityOnHit = new HandlerHash<>();
    /** {@code TargetItemOnHit} (BattleHandlers.rb:65). */
    public static final HandlerHash<TargetItemOnHit> TargetItemOnHit = new HandlerHash<>();
    /** {@code TargetItemOnHitPositiveBerry} (BattleHandlers.rb:66). */
    public static final HandlerHash<TargetItemOnHitPositiveBerry> TargetItemOnHitPositiveBerry = new HandlerHash<>();

    // {# Abilities/items that trigger at the end of using a move} (BattleHandlers.rb:67)
    /** {@code UserAbilityEndOfMove} (BattleHandlers.rb:68). */
    public static final HandlerHash<UserAbilityEndOfMove> UserAbilityEndOfMove = new HandlerHash<>();
    /** {@code TargetItemAfterMoveUse} (BattleHandlers.rb:69). */
    public static final HandlerHash<TargetItemAfterMoveUse> TargetItemAfterMoveUse = new HandlerHash<>();
    /** {@code ItemOnStatLoss} (BattleHandlers.rb:70). */
    public static final HandlerHash<ItemOnStatLoss> ItemOnStatLoss = new HandlerHash<>();
    /** {@code UserItemAfterMoveUse} (BattleHandlers.rb:71). */
    public static final HandlerHash<UserItemAfterMoveUse> UserItemAfterMoveUse = new HandlerHash<>();
    /** {@code TargetAbilityAfterMoveUse} (BattleHandlers.rb:72). */
    public static final HandlerHash<TargetAbilityAfterMoveUse> TargetAbilityAfterMoveUse = new HandlerHash<>();
    /** {@code EndOfMoveItem} (BattleHandlers.rb:73, {@code # Leppa Berry}). */
    public static final HandlerHash<EndOfMoveItem> EndOfMoveItem = new HandlerHash<>();
    /** {@code EndOfMoveStatRestoreItem} (BattleHandlers.rb:74, {@code # White Herb}). */
    public static final HandlerHash<EndOfMoveStatRestoreItem> EndOfMoveStatRestoreItem = new HandlerHash<>();

    // {# Experience and EV gain} (BattleHandlers.rb:75)
    /** {@code ExpGainModifierItem} (BattleHandlers.rb:76, {@code # Lucky Egg}). */
    public static final HandlerHash<ExpGainModifierItem> ExpGainModifierItem = new HandlerHash<>();
    /** {@code EVGainModifierItem} (BattleHandlers.rb:77). */
    public static final HandlerHash<EVGainModifierItem> EVGainModifierItem = new HandlerHash<>();

    // {# Weather and terrin} (BattleHandlers.rb:78 - the plugin's typo)
    /** {@code WeatherExtenderItem} (BattleHandlers.rb:79). */
    public static final HandlerHash<WeatherExtenderItem> WeatherExtenderItem = new HandlerHash<>();
    /** {@code TerrainExtenderItem} (BattleHandlers.rb:80, {@code # Terrain Extender}). */
    public static final HandlerHash<TerrainExtenderItem> TerrainExtenderItem = new HandlerHash<>();
    /** {@code TerrainStatBoostItem} (BattleHandlers.rb:81). */
    public static final HandlerHash<TerrainStatBoostItem> TerrainStatBoostItem = new HandlerHash<>();

    // {# End Of Round} (BattleHandlers.rb:82)
    /** {@code EORWeatherAbility} (BattleHandlers.rb:83). */
    public static final HandlerHash<EORWeatherAbility> EORWeatherAbility = new HandlerHash<>();
    /** {@code EORHealingAbility} (BattleHandlers.rb:84). */
    public static final HandlerHash<EORHealingAbility> EORHealingAbility = new HandlerHash<>();
    /** {@code EORHealingItem} (BattleHandlers.rb:85). */
    public static final HandlerHash<EORHealingItem> EORHealingItem = new HandlerHash<>();
    /** {@code EOREffectAbility} (BattleHandlers.rb:86). */
    public static final HandlerHash<EOREffectAbility> EOREffectAbility = new HandlerHash<>();
    /** {@code EOREffectItem} (BattleHandlers.rb:87). */
    public static final HandlerHash<EOREffectItem> EOREffectItem = new HandlerHash<>();
    /** {@code EORGainItemAbility} (BattleHandlers.rb:88). */
    public static final HandlerHash<EORGainItemAbility> EORGainItemAbility = new HandlerHash<>();

    // {# Switching and fainting} (BattleHandlers.rb:89)
    /** {@code CertainSwitchingUserAbility} (BattleHandlers.rb:90, {@code # None!}). */
    public static final HandlerHash<CertainSwitchingUserAbility> CertainSwitchingUserAbility = new HandlerHash<>();
    /** {@code AbilityOnOpposingStatGain} (BattleHandlers.rb:91, {@code # Opportunist}). */
    public static final HandlerHash<AbilityOnOpposingStatGain> AbilityOnOpposingStatGain = new HandlerHash<>();
    /** {@code CertainSwitchingUserItem} (BattleHandlers.rb:93, {@code # Shed Shell}). */
    public static final HandlerHash<CertainSwitchingUserItem> CertainSwitchingUserItem = new HandlerHash<>();
    /** {@code TrappingTargetAbility} (BattleHandlers.rb:94). */
    public static final HandlerHash<TrappingTargetAbility> TrappingTargetAbility = new HandlerHash<>();
    /** {@code TrappingTargetItem} (BattleHandlers.rb:95, {@code # None!}). */
    public static final HandlerHash<TrappingTargetItem> TrappingTargetItem = new HandlerHash<>();
    /** {@code AbilityOnSwitchIn} (BattleHandlers.rb:96). */
    public static final HandlerHash<AbilityOnSwitchIn> AbilityOnSwitchIn = new HandlerHash<>();
    /** {@code ItemOnSwitchIn} (BattleHandlers.rb:97, {@code # Air Balloon}). */
    public static final HandlerHash<ItemOnSwitchIn> ItemOnSwitchIn = new HandlerHash<>();
    /** {@code ItemOnIntimidated} (BattleHandlers.rb:98, {@code # Adrenaline Orb}). */
    public static final HandlerHash<ItemOnIntimidated> ItemOnIntimidated = new HandlerHash<>();
    /** {@code AbilityOnSwitchOut} (BattleHandlers.rb:99). */
    public static final HandlerHash<AbilityOnSwitchOut> AbilityOnSwitchOut = new HandlerHash<>();
    /** {@code AbilityChangeOnBattlerFainting} (BattleHandlers.rb:100). */
    public static final HandlerHash<AbilityChangeOnBattlerFainting> AbilityChangeOnBattlerFainting = new HandlerHash<>();
    /** {@code AbilityOnBattlerFainting} (BattleHandlers.rb:101, {@code # Soul-Heart}). */
    public static final HandlerHash<AbilityOnBattlerFainting> AbilityOnBattlerFainting = new HandlerHash<>();

    // {# Running from battle} (BattleHandlers.rb:102)
    /** {@code RunFromBattleAbility} (BattleHandlers.rb:103, {@code # Run Away}). */
    public static final HandlerHash<RunFromBattleAbility> RunFromBattleAbility = new HandlerHash<>();
    /** {@code RunFromBattleItem} (BattleHandlers.rb:104, {@code # Smoke Ball}). */
    public static final HandlerHash<RunFromBattleItem> RunFromBattleItem = new HandlerHash<>();
    /** {@code AbilityOnTerrainChange} (BattleHandlers.rb:105, {@code #夸克充能}). */
    public static final HandlerHash<AbilityOnTerrainChange> AbilityOnTerrainChange = new HandlerHash<>();

    // =====================================================================
    // One-shot registration, in Ruby section order
    // =====================================================================

    private static boolean initialized;

    /**
     * Idempotent: runs every {@code register()} in Ruby section order, once.
     *
     * <p>Ruby loads {@code BattleHandlers_Abilities}, then
     * {@code BattleHandlers_Items}, then {@code Arceus}, then {@code 场地}, and
     * {@code HandlerHash#add} overwrites an existing entry
     * ({@code Event_Handlers.rb:110-113}) - so the later sections really do
     * replace main-table handlers. Java's static-initialiser order is not
     * controllable, so the order lives in {@link BattleHandlerRegistry}.</p>
     *
     * <p>Every {@code trigger*} wrapper calls this first (one boolean test), so
     * no caller has to remember to initialise.</p>
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        BattleHandlerRegistry.init();
    }

    // =====================================================================
    // Handler interfaces - one per registry, in the same order
    // =====================================================================

    /**
     * Handler of {@link #SpeedCalcAbility} ({@code BattleHandlers.rb:109-112}).
     * {@code mult} is a {@code float}: the plugin writes {@code mult*1.5}
     * (BattleHandlers_Items:7).
     */
    @FunctionalInterface
    public interface SpeedCalcAbility {
        Float apply(String ability, Battler battler, float mult);
    }

    /** Handler of {@link #SpeedCalcItem} ({@code BattleHandlers.rb:114-117}); {@code mult*1.5} at BattleHandlers_Items:7. */
    @FunctionalInterface
    public interface SpeedCalcItem {
        Float apply(String item, Battler battler, float mult);
    }

    /** Handler of {@link #WeightCalcAbility} ({@code BattleHandlers.rb:121-124}); {@code w} is a float ({@code [w/2,1].max} BattleHandlers_Abilities:69). */
    @FunctionalInterface
    public interface WeightCalcAbility {
        Float apply(String ability, Battler battler, float w);
    }

    /** Handler of {@link #WeightCalcItem} ({@code BattleHandlers.rb:126-129}); {@code [w/2,1].max} BattleHandlers_Items:40. */
    @FunctionalInterface
    public interface WeightCalcItem {
        Float apply(String item, Battler battler, float w);
    }

    /** Handler of {@link #HPHealItem} ({@code BattleHandlers.rb:133-136}). */
    @FunctionalInterface
    public interface HPHealItem {
        Boolean apply(String item, Battler battler, Battle battle, boolean forced);
    }

    /** Handler of {@link #AbilityOnHPDroppedBelowHalf} ({@code BattleHandlers.rb:138-141}). */
    @FunctionalInterface
    public interface AbilityOnHPDroppedBelowHalf {
        Boolean apply(String ability, Battler user, Battle battle);
    }

    /** Handler of {@link #StatusCheckAbilityNonIgnorable} ({@code BattleHandlers.rb:145-148}); {@code status} is a numeric PBStatuses id. */
    @FunctionalInterface
    public interface StatusCheckAbilityNonIgnorable {
        Boolean apply(String ability, Battler battler, int status);
    }

    /** Handler of {@link #StatusImmunityAbility} ({@code BattleHandlers.rb:150-153}); numeric PBStatuses id. */
    @FunctionalInterface
    public interface StatusImmunityAbility {
        Boolean apply(String ability, Battler battler, int status);
    }

    /** Handler of {@link #StatusImmunityAbilityNonIgnorable} ({@code BattleHandlers.rb:155-158}). */
    @FunctionalInterface
    public interface StatusImmunityAbilityNonIgnorable {
        Boolean apply(String ability, Battler battler, int status);
    }

    /** Handler of {@link #StatusImmunityAllyAbility} ({@code BattleHandlers.rb:160-163}). */
    @FunctionalInterface
    public interface StatusImmunityAllyAbility {
        Boolean apply(String ability, Battler battler, int status);
    }

    /** Handler of {@link #AbilityOnStatusInflicted} ({@code BattleHandlers.rb:165-168}). */
    @FunctionalInterface
    public interface AbilityOnStatusInflicted {
        void apply(String ability, Battler battler, Battler user, int status);
    }

    /** Handler of {@link #StatusCureItem} ({@code BattleHandlers.rb:171-174}). */
    @FunctionalInterface
    public interface StatusCureItem {
        Boolean apply(String item, Battler battler, Battle battle, boolean forced);
    }

    /** Handler of {@link #StatusCureAbility} ({@code BattleHandlers.rb:176-179}). */
    @FunctionalInterface
    public interface StatusCureAbility {
        Boolean apply(String ability, Battler battler);
    }

    /**
     * Handler of {@link #AbilityModifyTypeEffectiveness}
     * ({@code BattleHandlers.rb:180-182}). The plugin registers NO handler for
     * this group anywhere and never calls the wrapper, so {@code null} is its
     * only reachable result; the interface still returns {@code Integer} because
     * Ruby's {@code return ...trigger(...)} can yield {@code nil}.
     */
    @FunctionalInterface
    public interface AbilityModifyTypeEffectiveness {
        Integer apply(String ability, Battler user, Battler target, BattleMove move, Battle battle, int effectiveness);
    }

    /** Handler of {@link #AbilityOnMoveSuccessCheck} ({@code BattleHandlers.rb:184-186}). */
    @FunctionalInterface
    public interface AbilityOnMoveSuccessCheck {
        void apply(String ability, Battler user, Battler target, BattleMove move, Battle battle);
    }

    /** Handler of {@link #AbilityOnInflictingStatus} ({@code BattleHandlers.rb:188-190}). */
    @FunctionalInterface
    public interface AbilityOnInflictingStatus {
        void apply(String ability, Battler battler, Battler user, int status);
    }

    /** Handler of {@link #StatGainImmunityAbility} ({@code BattleHandlers.rb:192-195}); {@code stat} is a PBStats index. */
    @FunctionalInterface
    public interface StatGainImmunityAbility {
        Boolean apply(String ability, Battler battler, int stat, Battle battle, boolean showMessages);
    }

    /** Handler of {@link #StatLossImmunityAbility} ({@code BattleHandlers.rb:197-200}). */
    @FunctionalInterface
    public interface StatLossImmunityAbility {
        Boolean apply(String ability, Battler battler, int stat, Battle battle, boolean showMessages);
    }

    /** Handler of {@link #StatLossImmunityAbilityNonIgnorable} ({@code BattleHandlers.rb:202-205}). */
    @FunctionalInterface
    public interface StatLossImmunityAbilityNonIgnorable {
        Boolean apply(String ability, Battler battler, int stat, Battle battle, boolean showMessages);
    }

    /** Handler of {@link #StatLossImmunityAllyAbility} ({@code BattleHandlers.rb:207-210}); the wrapper's first argument is the {@code bearer}. */
    @FunctionalInterface
    public interface StatLossImmunityAllyAbility {
        Boolean apply(String ability, Battler bearer, Battler battler, int stat, Battle battle, boolean showMessages);
    }

    /** Handler of {@link #AbilityOnStatGain} ({@code BattleHandlers.rb:212-214}). */
    @FunctionalInterface
    public interface AbilityOnStatGain {
        void apply(String ability, Battler battler, int stat, Battler user);
    }

    /** Handler of {@link #AbilityOnStatLoss} ({@code BattleHandlers.rb:216-218}). */
    @FunctionalInterface
    public interface AbilityOnStatLoss {
        void apply(String ability, Battler battler, int stat, Battler user);
    }

    /**
     * Handler of {@link #AbilityOnOpposingStatGain} ({@code BattleHandlers.rb:219-221}).
     * {@code statUps} is {@code battle.sideStatUps[side]}: a list of
     * {@code [stat, increment]} pairs.
     */
    @FunctionalInterface
    public interface AbilityOnOpposingStatGain {
        void apply(String ability, Battler battler, Battle battle, List<int[]> statUps);
    }

    /** Handler of {@link #ItemOnOpposingStatGain} ({@code BattleHandlers.rb:223-225}); returns the handler's value (Ruby can yield {@code nil}). */
    @FunctionalInterface
    public interface ItemOnOpposingStatGain {
        Boolean apply(String item, Battler battler, Battle battle, List<int[]> statUps, boolean forced);
    }

    /** Handler of {@link #StatLossImmunityItem} ({@code BattleHandlers.rb:227-229}); {@code show_message} keeps Ruby's spelling. */
    @FunctionalInterface
    public interface StatLossImmunityItem {
        Boolean apply(String item, Battler battler, int stat, Battle battle, boolean show_message);
    }

    /** Handler of {@link #PriorityChangeAbility} ({@code BattleHandlers.rb:233-236}). */
    @FunctionalInterface
    public interface PriorityChangeAbility {
        Integer apply(String ability, Battler battler, BattleMove move, int pri);
    }

    /** Handler of {@link #PriorityBracketChangeAbility} ({@code BattleHandlers.rb:238-241}). */
    @FunctionalInterface
    public interface PriorityBracketChangeAbility {
        Integer apply(String ability, Battler battler, int subPri, Battle battle);
    }

    /** Handler of {@link #PriorityBracketChangeItem} ({@code BattleHandlers.rb:243-246}). */
    @FunctionalInterface
    public interface PriorityBracketChangeItem {
        Integer apply(String item, Battler battler, int subPri, Battle battle);
    }

    /** Handler of {@link #PriorityBracketUseAbility} ({@code BattleHandlers.rb:248-250}). */
    @FunctionalInterface
    public interface PriorityBracketUseAbility {
        void apply(String ability, Battler battler, Battle battle);
    }

    /** Handler of {@link #PriorityBracketUseItem} ({@code BattleHandlers.rb:252-254}). */
    @FunctionalInterface
    public interface PriorityBracketUseItem {
        void apply(String item, Battler battler, Battle battle);
    }

    /** Handler of {@link #AbilityOnFlinch} ({@code BattleHandlers.rb:258-260}). */
    @FunctionalInterface
    public interface AbilityOnFlinch {
        void apply(String ability, Battler battler, Battle battle);
    }

    /** Handler of {@link #MoveBlockingAbility} ({@code BattleHandlers.rb:262-265}). */
    @FunctionalInterface
    public interface MoveBlockingAbility {
        Boolean apply(String ability, Battler bearer, Battler user, Array<Battler> targets, BattleMove move, Battle battle);
    }

    /**
     * Handler of {@link #MoveImmunityTargetAbility} ({@code BattleHandlers.rb:267-270}).
     * {@code type} is the internal-name String (see the class javadoc); here
     * {@code target} IS a Battler (the wrapper's own parameter name).
     */
    @FunctionalInterface
    public interface MoveImmunityTargetAbility {
        Boolean apply(String ability, Battler user, Battler target, BattleMove move, String type, Battle battle);
    }

    /** Handler of {@link #MoveBaseTypeModifierAbility} ({@code BattleHandlers.rb:274-277}); returns a type internal name, defaulting to {@code type}. */
    @FunctionalInterface
    public interface MoveBaseTypeModifierAbility {
        String apply(String ability, Battler user, BattleMove move, String type);
    }

    /** Handler of {@link #AccuracyCalcUserAbility} ({@code BattleHandlers.rb:281-283}); {@code mods} is {@code float[]} (see class javadoc). */
    @FunctionalInterface
    public interface AccuracyCalcUserAbility {
        void apply(String ability, float[] mods, Battler user, Battler target, BattleMove move, String type);
    }

    /** Handler of {@link #AccuracyCalcUserAllyAbility} ({@code BattleHandlers.rb:285-287}). */
    @FunctionalInterface
    public interface AccuracyCalcUserAllyAbility {
        void apply(String ability, float[] mods, Battler user, Battler target, BattleMove move, String type);
    }

    /** Handler of {@link #AccuracyCalcTargetAbility} ({@code BattleHandlers.rb:289-291}). */
    @FunctionalInterface
    public interface AccuracyCalcTargetAbility {
        void apply(String ability, float[] mods, Battler user, Battler target, BattleMove move, String type);
    }

    /** Handler of {@link #AccuracyCalcUserItem} ({@code BattleHandlers.rb:293-295}). */
    @FunctionalInterface
    public interface AccuracyCalcUserItem {
        void apply(String item, float[] mods, Battler user, Battler target, BattleMove move, String type);
    }

    /** Handler of {@link #AccuracyCalcTargetItem} ({@code BattleHandlers.rb:297-299}). */
    @FunctionalInterface
    public interface AccuracyCalcTargetItem {
        void apply(String item, float[] mods, Battler user, Battler target, BattleMove move, String type);
    }

    /** Handler of {@link #DamageCalcUserAbility} ({@code BattleHandlers.rb:303-305}); {@code mults} is {@code float[]}. */
    @FunctionalInterface
    public interface DamageCalcUserAbility {
        void apply(String ability, Battler user, Battler target, BattleMove move, float[] mults, int baseDmg, String type);
    }

    /** Handler of {@link #DamageCalcUserAllyAbility} ({@code BattleHandlers.rb:307-309}). */
    @FunctionalInterface
    public interface DamageCalcUserAllyAbility {
        void apply(String ability, Battler user, Battler target, BattleMove move, float[] mults, int baseDmg, String type);
    }

    /** Handler of {@link #DamageCalcTargetAbility} ({@code BattleHandlers.rb:311-313}). */
    @FunctionalInterface
    public interface DamageCalcTargetAbility {
        void apply(String ability, Battler user, Battler target, BattleMove move, float[] mults, int baseDmg, String type);
    }

    /** Handler of {@link #DamageCalcTargetAbilityNonIgnorable} ({@code BattleHandlers.rb:315-317}). */
    @FunctionalInterface
    public interface DamageCalcTargetAbilityNonIgnorable {
        void apply(String ability, Battler user, Battler target, BattleMove move, float[] mults, int baseDmg, String type);
    }

    /** Handler of {@link #DamageCalcTargetAllyAbility} ({@code BattleHandlers.rb:319-321}). */
    @FunctionalInterface
    public interface DamageCalcTargetAllyAbility {
        void apply(String ability, Battler user, Battler target, BattleMove move, float[] mults, int baseDmg, String type);
    }

    /** Handler of {@link #DamageCalcUserItem} ({@code BattleHandlers.rb:323-325}). */
    @FunctionalInterface
    public interface DamageCalcUserItem {
        void apply(String item, Battler user, Battler target, BattleMove move, float[] mults, int baseDmg, String type);
    }

    /** Handler of {@link #DamageCalcTargetItem} ({@code BattleHandlers.rb:327-329}). */
    @FunctionalInterface
    public interface DamageCalcTargetItem {
        void apply(String item, Battler user, Battler target, BattleMove move, float[] mults, int baseDmg, String type);
    }

    /** Handler of {@link #CriticalCalcUserAbility} ({@code BattleHandlers.rb:333-336}); {@code c} is the integer critical-hit count. */
    @FunctionalInterface
    public interface CriticalCalcUserAbility {
        Integer apply(String ability, Battler user, Battler target, int c);
    }

    /** Handler of {@link #CriticalCalcTargetAbility} ({@code BattleHandlers.rb:338-341}). */
    @FunctionalInterface
    public interface CriticalCalcTargetAbility {
        Integer apply(String ability, Battler user, Battler target, int c);
    }

    /** Handler of {@link #CriticalCalcUserItem} ({@code BattleHandlers.rb:343-346}). */
    @FunctionalInterface
    public interface CriticalCalcUserItem {
        Integer apply(String item, Battler user, Battler target, int c);
    }

    /** Handler of {@link #CriticalCalcTargetItem} ({@code BattleHandlers.rb:348-351}). */
    @FunctionalInterface
    public interface CriticalCalcTargetItem {
        Integer apply(String item, Battler user, Battler target, int c);
    }

    /** Handler of {@link #TargetAbilityOnHit} ({@code BattleHandlers.rb:355-357}). */
    @FunctionalInterface
    public interface TargetAbilityOnHit {
        void apply(String ability, Battler user, Battler target, BattleMove move, Battle battle);
    }

    /** Handler of {@link #UserAbilityOnHit} ({@code BattleHandlers.rb:359-361}). */
    @FunctionalInterface
    public interface UserAbilityOnHit {
        void apply(String ability, Battler user, Battler target, BattleMove move, Battle battle);
    }

    /** Handler of {@link #TargetItemOnHit} ({@code BattleHandlers.rb:363-365}). */
    @FunctionalInterface
    public interface TargetItemOnHit {
        void apply(String item, Battler user, Battler target, BattleMove move, Battle battle);
    }

    /** Handler of {@link #TargetItemOnHitPositiveBerry} ({@code BattleHandlers.rb:367-370}). */
    @FunctionalInterface
    public interface TargetItemOnHitPositiveBerry {
        Boolean apply(String item, Battler battler, Battle battle, boolean forced);
    }

    /** Handler of {@link #UserAbilityEndOfMove} ({@code BattleHandlers.rb:374-376}); {@code targets} is an Array of Battler. */
    @FunctionalInterface
    public interface UserAbilityEndOfMove {
        void apply(String ability, Battler user, Array<Battler> targets, BattleMove move, Battle battle);
    }

    /** Handler of {@link #TargetItemAfterMoveUse} ({@code BattleHandlers.rb:378-380}); {@code switched} is Ruby's array of slot indices. */
    @FunctionalInterface
    public interface TargetItemAfterMoveUse {
        void apply(String item, Battler battler, Battler user, BattleMove move, Array<Integer> switched, Battle battle);
    }

    /** Handler of {@link #UserItemAfterMoveUse} ({@code BattleHandlers.rb:382-384}). */
    @FunctionalInterface
    public interface UserItemAfterMoveUse {
        void apply(String item, Battler user, Array<Battler> targets, BattleMove move, int numHits, Battle battle);
    }

    /** Handler of {@link #TargetAbilityAfterMoveUse} ({@code BattleHandlers.rb:386-388}). */
    @FunctionalInterface
    public interface TargetAbilityAfterMoveUse {
        void apply(String ability, Battler target, Battler user, BattleMove move, Array<Integer> switched, Battle battle);
    }

    /** Handler of {@link #EndOfMoveItem} ({@code BattleHandlers.rb:390-393}). */
    @FunctionalInterface
    public interface EndOfMoveItem {
        Boolean apply(String item, Battler battler, Battle battle, boolean forced);
    }

    /** Handler of {@link #EndOfMoveStatRestoreItem} ({@code BattleHandlers.rb:395-398}). */
    @FunctionalInterface
    public interface EndOfMoveStatRestoreItem {
        Boolean apply(String item, Battler battler, Battle battle, boolean forced);
    }

    /** Handler of {@link #ItemOnStatLoss} ({@code BattleHandlers.rb:402-404}). */
    @FunctionalInterface
    public interface ItemOnStatLoss {
        void apply(String item, Battler battler, Battler user, BattleMove move, Array<Integer> switched, Battle battle);
    }

    /** Handler of {@link #ExpGainModifierItem} ({@code BattleHandlers.rb:408-411}). */
    @FunctionalInterface
    public interface ExpGainModifierItem {
        Integer apply(String item, Battler battler, int exp);
    }

    /**
     * Handler of {@link #EVGainModifierItem} ({@code BattleHandlers.rb:413-417}).
     * The wrapper ignores the handler's return value (it returns true/false
     * itself), so this one is {@code void}.
     */
    @FunctionalInterface
    public interface EVGainModifierItem {
        void apply(String item, Battler battler, int[] evarray);
    }

    /** Handler of {@link #WeatherExtenderItem} ({@code BattleHandlers.rb:421-424}); {@code weather} is a PBWeather id. */
    @FunctionalInterface
    public interface WeatherExtenderItem {
        Integer apply(String item, int weather, int duration, Battler battler, Battle battle);
    }

    /** Handler of {@link #TerrainExtenderItem} ({@code BattleHandlers.rb:426-429}); {@code terrain} is a PBBattleTerrains id. */
    @FunctionalInterface
    public interface TerrainExtenderItem {
        Integer apply(String item, int terrain, int duration, Battler battler, Battle battle);
    }

    /** Handler of {@link #TerrainStatBoostItem} ({@code BattleHandlers.rb:431-434}). */
    @FunctionalInterface
    public interface TerrainStatBoostItem {
        Boolean apply(String item, Battler battler, Battle battle);
    }

    /** Handler of {@link #EORWeatherAbility} ({@code BattleHandlers.rb:438-440}). */
    @FunctionalInterface
    public interface EORWeatherAbility {
        void apply(String ability, int weather, Battler battler, Battle battle);
    }

    /** Handler of {@link #EORHealingAbility} ({@code BattleHandlers.rb:442-444}). */
    @FunctionalInterface
    public interface EORHealingAbility {
        void apply(String ability, Battler battler, Battle battle);
    }

    /** Handler of {@link #EORHealingItem} ({@code BattleHandlers.rb:446-448}). */
    @FunctionalInterface
    public interface EORHealingItem {
        void apply(String item, Battler battler, Battle battle);
    }

    /** Handler of {@link #EOREffectAbility} ({@code BattleHandlers.rb:450-452}). */
    @FunctionalInterface
    public interface EOREffectAbility {
        void apply(String ability, Battler battler, Battle battle);
    }

    /** Handler of {@link #EOREffectItem} ({@code BattleHandlers.rb:454-456}). */
    @FunctionalInterface
    public interface EOREffectItem {
        void apply(String item, Battler battler, Battle battle);
    }

    /** Handler of {@link #EORGainItemAbility} ({@code BattleHandlers.rb:458-460}). */
    @FunctionalInterface
    public interface EORGainItemAbility {
        void apply(String ability, Battler battler, Battle battle);
    }

    /** Handler of {@link #CertainSwitchingUserAbility} ({@code BattleHandlers.rb:464-467}). */
    @FunctionalInterface
    public interface CertainSwitchingUserAbility {
        Boolean apply(String ability, Battler switcher, Battle battle);
    }

    /** Handler of {@link #CertainSwitchingUserItem} ({@code BattleHandlers.rb:469-472}). */
    @FunctionalInterface
    public interface CertainSwitchingUserItem {
        Boolean apply(String item, Battler switcher, Battle battle);
    }

    /** Handler of {@link #TrappingTargetAbility} ({@code BattleHandlers.rb:474-477}). */
    @FunctionalInterface
    public interface TrappingTargetAbility {
        Boolean apply(String ability, Battler switcher, Battler bearer, Battle battle);
    }

    /** Handler of {@link #TrappingTargetItem} ({@code BattleHandlers.rb:479-482}). */
    @FunctionalInterface
    public interface TrappingTargetItem {
        Boolean apply(String item, Battler switcher, Battler bearer, Battle battle);
    }

    /** Handler of {@link #AbilityOnSwitchIn} ({@code BattleHandlers.rb:484-491}). */
    @FunctionalInterface
    public interface AbilityOnSwitchIn {
        void apply(String ability, Battler battler, Battle battle);
    }

    /** Handler of {@link #ItemOnSwitchIn} ({@code BattleHandlers.rb:492-494}). */
    @FunctionalInterface
    public interface ItemOnSwitchIn {
        void apply(String item, Battler battler, Battle battle);
    }

    /** Handler of {@link #ItemOnIntimidated} ({@code BattleHandlers.rb:496-499}). */
    @FunctionalInterface
    public interface ItemOnIntimidated {
        Boolean apply(String item, Battler battler, Battle battle);
    }

    /** Handler of {@link #AbilityOnSwitchOut} ({@code BattleHandlers.rb:501-503}). */
    @FunctionalInterface
    public interface AbilityOnSwitchOut {
        void apply(String ability, Battler battler, boolean endOfBattle);
    }

    /** Handler of {@link #AbilityChangeOnBattlerFainting} ({@code BattleHandlers.rb:505-507}). */
    @FunctionalInterface
    public interface AbilityChangeOnBattlerFainting {
        void apply(String ability, Battler battler, Battler fainted, Battle battle);
    }

    /** Handler of {@link #AbilityOnBattlerFainting} ({@code BattleHandlers.rb:509-511}). */
    @FunctionalInterface
    public interface AbilityOnBattlerFainting {
        void apply(String ability, Battler battler, Battler fainted, Battle battle);
    }

    /** Handler of {@link #RunFromBattleAbility} ({@code BattleHandlers.rb:515-518}). */
    @FunctionalInterface
    public interface RunFromBattleAbility {
        Boolean apply(String ability, Battler battler);
    }

    /** Handler of {@link #RunFromBattleItem} ({@code BattleHandlers.rb:520-523}). */
    @FunctionalInterface
    public interface RunFromBattleItem {
        Boolean apply(String item, Battler battler);
    }

    /** Handler of {@link #AbilityOnTerrainChange} ({@code BattleHandlers.rb:525-527}); {@code ability_changed} keeps Ruby's spelling. */
    @FunctionalInterface
    public interface AbilityOnTerrainChange {
        void apply(String ability, Battler battler, Battle battle, boolean ability_changed);
    }

    // =====================================================================
    // The 85 trigger wrappers - BattleHandlers.rb:109-527
    // Each one mirrors HandlerHash#trigger (Event_Handlers.rb:138-141): the
    // handler is looked up by the sym and receives that sym first.
    // =====================================================================

    /** {@code triggerSpeedCalcAbility} (BattleHandlers.rb:109-112). */
    public static float triggerSpeedCalcAbility(String ability, Battler battler, float mult) {
        init();
        SpeedCalcAbility handler = SpeedCalcAbility.get(ability);
        Float ret = handler != null ? handler.apply(ability, battler, mult) : null;
        return ret != null ? ret : mult;                                     // :111
    }

    /** {@code triggerSpeedCalcItem} (BattleHandlers.rb:114-117). */
    public static float triggerSpeedCalcItem(String item, Battler battler, float mult) {
        init();
        SpeedCalcItem handler = SpeedCalcItem.get(item);
        Float ret = handler != null ? handler.apply(item, battler, mult) : null;
        return ret != null ? ret : mult;                                     // :116
    }

    /** {@code triggerWeightCalcAbility} (BattleHandlers.rb:121-124). */
    public static float triggerWeightCalcAbility(String ability, Battler battler, float w) {
        init();
        WeightCalcAbility handler = WeightCalcAbility.get(ability);
        Float ret = handler != null ? handler.apply(ability, battler, w) : null;
        return ret != null ? ret : w;                                        // :123
    }

    /** {@code triggerWeightCalcItem} (BattleHandlers.rb:126-129). */
    public static float triggerWeightCalcItem(String item, Battler battler, float w) {
        init();
        WeightCalcItem handler = WeightCalcItem.get(item);
        Float ret = handler != null ? handler.apply(item, battler, w) : null;
        return ret != null ? ret : w;                                        // :128
    }

    /** {@code triggerHPHealItem} (BattleHandlers.rb:133-136). */
    public static boolean triggerHPHealItem(String item, Battler battler, Battle battle, boolean forced) {
        init();
        HPHealItem handler = HPHealItem.get(item);
        Boolean ret = handler != null ? handler.apply(item, battler, battle, forced) : null;
        return ret != null ? ret : false;                                    // :135
    }

    /** {@code triggerAbilityOnHPDroppedBelowHalf} (BattleHandlers.rb:138-141). */
    public static boolean triggerAbilityOnHPDroppedBelowHalf(String ability, Battler user, Battle battle) {
        init();
        AbilityOnHPDroppedBelowHalf handler = AbilityOnHPDroppedBelowHalf.get(ability);
        Boolean ret = handler != null ? handler.apply(ability, user, battle) : null;
        return ret != null ? ret : false;                                    // :140
    }

    /** {@code triggerStatusCheckAbilityNonIgnorable} (BattleHandlers.rb:145-148). */
    public static boolean triggerStatusCheckAbilityNonIgnorable(String ability, Battler battler, int status) {
        init();
        StatusCheckAbilityNonIgnorable handler = StatusCheckAbilityNonIgnorable.get(ability);
        Boolean ret = handler != null ? handler.apply(ability, battler, status) : null;
        return ret != null ? ret : false;                                    // :147
    }

    /** {@code triggerStatusImmunityAbility} (BattleHandlers.rb:150-153). */
    public static boolean triggerStatusImmunityAbility(String ability, Battler battler, int status) {
        init();
        StatusImmunityAbility handler = StatusImmunityAbility.get(ability);
        Boolean ret = handler != null ? handler.apply(ability, battler, status) : null;
        return ret != null ? ret : false;                                    // :152
    }

    /** {@code triggerStatusImmunityAbilityNonIgnorable} (BattleHandlers.rb:155-158). */
    public static boolean triggerStatusImmunityAbilityNonIgnorable(String ability, Battler battler, int status) {
        init();
        StatusImmunityAbilityNonIgnorable handler = StatusImmunityAbilityNonIgnorable.get(ability);
        Boolean ret = handler != null ? handler.apply(ability, battler, status) : null;
        return ret != null ? ret : false;                                    // :157
    }

    /** {@code triggerStatusImmunityAllyAbility} (BattleHandlers.rb:160-163). */
    public static boolean triggerStatusImmunityAllyAbility(String ability, Battler battler, int status) {
        init();
        StatusImmunityAllyAbility handler = StatusImmunityAllyAbility.get(ability);
        Boolean ret = handler != null ? handler.apply(ability, battler, status) : null;
        return ret != null ? ret : false;                                    // :162
    }

    /**
     * {@code triggerAbilityOnStatusInflicted} (BattleHandlers.rb:165-168).
     * Two triggers: first Poison Puppeteer's group, guarded by
     * {@code user && user.abilityActive?} ({@code :166}), then the caller's own
     * group ({@code :167}).
     */
    public static void triggerAbilityOnStatusInflicted(String ability, Battler battler, Battler user, int status) {
        init();
        if (user != null && user.abilityActive()) {                          // :166 Battler#abilityActive? (PokeBattle_Battler:379)
            String userAbility = user.ability;                               // :166 user.ability (PokeBattle_Battler:11)
            AbilityOnInflictingStatus inflicting = AbilityOnInflictingStatus.get(userAbility);
            if (inflicting != null) {
                inflicting.apply(userAbility, user, battler, status);
            }
        }
        AbilityOnStatusInflicted inflicted = AbilityOnStatusInflicted.get(ability);   // :167
        if (inflicted != null) {
            inflicted.apply(ability, battler, user, status);
        }
    }

    /** {@code triggerStatusCureItem} (BattleHandlers.rb:171-174). */
    public static boolean triggerStatusCureItem(String item, Battler battler, Battle battle, boolean forced) {
        init();
        StatusCureItem handler = StatusCureItem.get(item);
        Boolean ret = handler != null ? handler.apply(item, battler, battle, forced) : null;
        return ret != null ? ret : false;                                    // :173
    }

    /** {@code triggerStatusCureAbility} (BattleHandlers.rb:176-179). */
    public static boolean triggerStatusCureAbility(String ability, Battler battler) {
        init();
        StatusCureAbility handler = StatusCureAbility.get(ability);
        Boolean ret = handler != null ? handler.apply(ability, battler) : null;
        return ret != null ? ret : false;                                    // :178
    }

    /**
     * {@code triggerAbilityModifyTypeEffectiveness} (BattleHandlers.rb:180-182).
     * No {@code ret!=nil} wrapper: Ruby returns whatever the handler returns
     * ({@code nil} when the group has no handler).
     */
    public static Integer triggerAbilityModifyTypeEffectiveness(String ability, Battler user, Battler target,
                                                               BattleMove move, Battle battle, int effectiveness) {
        init();
        AbilityModifyTypeEffectiveness handler = AbilityModifyTypeEffectiveness.get(ability);
        return handler != null ? handler.apply(ability, user, target, move, battle, effectiveness) : null;   // :181
    }

    /** {@code triggerAbilityOnMoveSuccessCheck} (BattleHandlers.rb:184-186) - the wrapper ignores the handler's return value. */
    public static void triggerAbilityOnMoveSuccessCheck(String ability, Battler user, Battler target,
                                                        BattleMove move, Battle battle) {
        init();
        AbilityOnMoveSuccessCheck handler = AbilityOnMoveSuccessCheck.get(ability);
        if (handler != null) {
            handler.apply(ability, user, target, move, battle);               // :185
        }
    }

    /** {@code triggerAbilityOnInflictingStatus} (BattleHandlers.rb:188-190) - the wrapper ignores the handler's return value. */
    public static void triggerAbilityOnInflictingStatus(String ability, Battler battler, Battler user, int status) {
        init();
        AbilityOnInflictingStatus handler = AbilityOnInflictingStatus.get(ability);
        if (handler != null) {
            handler.apply(ability, battler, user, status);                    // :189
        }
    }

    /** {@code triggerStatGainImmunityAbility} (BattleHandlers.rb:192-195). */
    public static boolean triggerStatGainImmunityAbility(String ability, Battler battler, int stat, Battle battle,
                                                         boolean showMessages) {
        init();
        StatGainImmunityAbility handler = StatGainImmunityAbility.get(ability);
        Boolean ret = handler != null ? handler.apply(ability, battler, stat, battle, showMessages) : null;
        return ret != null ? ret : false;                                    // :194
    }

    /** {@code triggerStatLossImmunityAbility} (BattleHandlers.rb:197-200). */
    public static boolean triggerStatLossImmunityAbility(String ability, Battler battler, int stat, Battle battle,
                                                         boolean showMessages) {
        init();
        StatLossImmunityAbility handler = StatLossImmunityAbility.get(ability);
        Boolean ret = handler != null ? handler.apply(ability, battler, stat, battle, showMessages) : null;
        return ret != null ? ret : false;                                    // :199
    }

    /** {@code triggerStatLossImmunityAbilityNonIgnorable} (BattleHandlers.rb:202-205). */
    public static boolean triggerStatLossImmunityAbilityNonIgnorable(String ability, Battler battler, int stat,
                                                                     Battle battle, boolean showMessages) {
        init();
        StatLossImmunityAbilityNonIgnorable handler = StatLossImmunityAbilityNonIgnorable.get(ability);
        Boolean ret = handler != null ? handler.apply(ability, battler, stat, battle, showMessages) : null;
        return ret != null ? ret : false;                                    // :204
    }

    /** {@code triggerStatLossImmunityAllyAbility} (BattleHandlers.rb:207-210). */
    public static boolean triggerStatLossImmunityAllyAbility(String ability, Battler bearer, Battler battler, int stat,
                                                             Battle battle, boolean showMessages) {
        init();
        StatLossImmunityAllyAbility handler = StatLossImmunityAllyAbility.get(ability);
        Boolean ret = handler != null ? handler.apply(ability, bearer, battler, stat, battle, showMessages) : null;
        return ret != null ? ret : false;                                    // :209
    }

    /** {@code triggerAbilityOnStatGain} (BattleHandlers.rb:212-214) - return value ignored. */
    public static void triggerAbilityOnStatGain(String ability, Battler battler, int stat, Battler user) {
        init();
        AbilityOnStatGain handler = AbilityOnStatGain.get(ability);
        if (handler != null) {
            handler.apply(ability, battler, stat, user);                      // :213
        }
    }

    /** {@code triggerAbilityOnStatLoss} (BattleHandlers.rb:216-218) - return value ignored. */
    public static void triggerAbilityOnStatLoss(String ability, Battler battler, int stat, Battler user) {
        init();
        AbilityOnStatLoss handler = AbilityOnStatLoss.get(ability);
        if (handler != null) {
            handler.apply(ability, battler, stat, user);                      // :217
        }
    }

    /** {@code triggerAbilityOnOpposingStatGain} (BattleHandlers.rb:219-221) - return value ignored. */
    public static void triggerAbilityOnOpposingStatGain(String ability, Battler battler, Battle battle,
                                                        List<int[]> statUps) {
        init();
        AbilityOnOpposingStatGain handler = AbilityOnOpposingStatGain.get(ability);
        if (handler != null) {
            handler.apply(ability, battler, battle, statUps);                 // :220
        }
    }

    /**
     * {@code triggerItemOnOpposingStatGain} (BattleHandlers.rb:223-225). No
     * {@code ret!=nil} wrapper: Ruby returns the handler's value directly, which
     * is {@code nil} when nothing is registered (Battler_AbilityAndItem:337
     * tests it as a condition, where {@code nil} is falsy).
     */
    public static Boolean triggerItemOnOpposingStatGain(String item, Battler battler, Battle battle,
                                                        List<int[]> statUps, boolean forced) {
        init();
        ItemOnOpposingStatGain handler = ItemOnOpposingStatGain.get(item);
        return handler != null ? handler.apply(item, battler, battle, statUps, forced) : null;   // :224
    }

    /**
     * {@code triggerStatLossImmunityItem} (BattleHandlers.rb:227-229). No
     * {@code ret!=nil} wrapper: {@code nil} when the item has no handler
     * (Battler_StatStages:138/312 test it as a condition).
     */
    public static Boolean triggerStatLossImmunityItem(String item, Battler battler, int stat, Battle battle,
                                                      boolean show_message) {
        init();
        StatLossImmunityItem handler = StatLossImmunityItem.get(item);
        return handler != null ? handler.apply(item, battler, stat, battle, show_message) : null;   // :228
    }

    /** {@code triggerPriorityChangeAbility} (BattleHandlers.rb:233-236). */
    public static int triggerPriorityChangeAbility(String ability, Battler battler, BattleMove move, int pri) {
        init();
        PriorityChangeAbility handler = PriorityChangeAbility.get(ability);
        Integer ret = handler != null ? handler.apply(ability, battler, move, pri) : null;
        return ret != null ? ret : pri;                                      // :235
    }

    /** {@code triggerPriorityBracketChangeAbility} (BattleHandlers.rb:238-241). */
    public static int triggerPriorityBracketChangeAbility(String ability, Battler battler, int subPri, Battle battle) {
        init();
        PriorityBracketChangeAbility handler = PriorityBracketChangeAbility.get(ability);
        Integer ret = handler != null ? handler.apply(ability, battler, subPri, battle) : null;
        return ret != null ? ret : subPri;                                   // :240
    }

    /** {@code triggerPriorityBracketChangeItem} (BattleHandlers.rb:243-246). */
    public static int triggerPriorityBracketChangeItem(String item, Battler battler, int subPri, Battle battle) {
        init();
        PriorityBracketChangeItem handler = PriorityBracketChangeItem.get(item);
        Integer ret = handler != null ? handler.apply(item, battler, subPri, battle) : null;
        return ret != null ? ret : subPri;                                   // :245
    }

    /** {@code triggerPriorityBracketUseAbility} (BattleHandlers.rb:248-250) - return value ignored. */
    public static void triggerPriorityBracketUseAbility(String ability, Battler battler, Battle battle) {
        init();
        PriorityBracketUseAbility handler = PriorityBracketUseAbility.get(ability);
        if (handler != null) {
            handler.apply(ability, battler, battle);                          // :249
        }
    }

    /** {@code triggerPriorityBracketUseItem} (BattleHandlers.rb:252-254) - return value ignored. */
    public static void triggerPriorityBracketUseItem(String item, Battler battler, Battle battle) {
        init();
        PriorityBracketUseItem handler = PriorityBracketUseItem.get(item);
        if (handler != null) {
            handler.apply(item, battler, battle);                             // :253
        }
    }

    /** {@code triggerAbilityOnFlinch} (BattleHandlers.rb:258-260) - return value ignored. */
    public static void triggerAbilityOnFlinch(String ability, Battler battler, Battle battle) {
        init();
        AbilityOnFlinch handler = AbilityOnFlinch.get(ability);
        if (handler != null) {
            handler.apply(ability, battler, battle);                          // :259
        }
    }

    /** {@code triggerMoveBlockingAbility} (BattleHandlers.rb:262-265). */
    public static boolean triggerMoveBlockingAbility(String ability, Battler bearer, Battler user,
                                                     Array<Battler> targets, BattleMove move, Battle battle) {
        init();
        MoveBlockingAbility handler = MoveBlockingAbility.get(ability);
        Boolean ret = handler != null ? handler.apply(ability, bearer, user, targets, move, battle) : null;
        return ret != null ? ret : false;                                    // :264
    }

    /** {@code triggerMoveImmunityTargetAbility} (BattleHandlers.rb:267-270). */
    public static boolean triggerMoveImmunityTargetAbility(String ability, Battler user, Battler target, BattleMove move,
                                                           String type, Battle battle) {
        init();
        MoveImmunityTargetAbility handler = MoveImmunityTargetAbility.get(ability);
        Boolean ret = handler != null ? handler.apply(ability, user, target, move, type, battle) : null;
        return ret != null ? ret : false;                                    // :269
    }

    /** {@code triggerMoveBaseTypeModifierAbility} (BattleHandlers.rb:274-277); the default is the incoming type name. */
    public static String triggerMoveBaseTypeModifierAbility(String ability, Battler user, BattleMove move, String type) {
        init();
        MoveBaseTypeModifierAbility handler = MoveBaseTypeModifierAbility.get(ability);
        String ret = handler != null ? handler.apply(ability, user, move, type) : null;
        return ret != null ? ret : type;                                     // :276
    }

    /** {@code triggerAccuracyCalcUserAbility} (BattleHandlers.rb:281-283) - return value ignored. */
    public static void triggerAccuracyCalcUserAbility(String ability, float[] mods, Battler user, Battler target,
                                                      BattleMove move, String type) {
        init();
        AccuracyCalcUserAbility handler = AccuracyCalcUserAbility.get(ability);
        if (handler != null) {
            handler.apply(ability, mods, user, target, move, type);           // :282
        }
    }

    /** {@code triggerAccuracyCalcUserAllyAbility} (BattleHandlers.rb:285-287) - return value ignored. */
    public static void triggerAccuracyCalcUserAllyAbility(String ability, float[] mods, Battler user, Battler target,
                                                          BattleMove move, String type) {
        init();
        AccuracyCalcUserAllyAbility handler = AccuracyCalcUserAllyAbility.get(ability);
        if (handler != null) {
            handler.apply(ability, mods, user, target, move, type);           // :286
        }
    }

    /** {@code triggerAccuracyCalcTargetAbility} (BattleHandlers.rb:289-291) - return value ignored. */
    public static void triggerAccuracyCalcTargetAbility(String ability, float[] mods, Battler user, Battler target,
                                                        BattleMove move, String type) {
        init();
        AccuracyCalcTargetAbility handler = AccuracyCalcTargetAbility.get(ability);
        if (handler != null) {
            handler.apply(ability, mods, user, target, move, type);           // :290
        }
    }

    /** {@code triggerAccuracyCalcUserItem} (BattleHandlers.rb:293-295) - return value ignored. */
    public static void triggerAccuracyCalcUserItem(String item, float[] mods, Battler user, Battler target,
                                                   BattleMove move, String type) {
        init();
        AccuracyCalcUserItem handler = AccuracyCalcUserItem.get(item);
        if (handler != null) {
            handler.apply(item, mods, user, target, move, type);              // :294
        }
    }

    /** {@code triggerAccuracyCalcTargetItem} (BattleHandlers.rb:297-299) - return value ignored. */
    public static void triggerAccuracyCalcTargetItem(String item, float[] mods, Battler user, Battler target,
                                                     BattleMove move, String type) {
        init();
        AccuracyCalcTargetItem handler = AccuracyCalcTargetItem.get(item);
        if (handler != null) {
            handler.apply(item, mods, user, target, move, type);              // :298
        }
    }

    /** {@code triggerDamageCalcUserAbility} (BattleHandlers.rb:303-305) - return value ignored. */
    public static void triggerDamageCalcUserAbility(String ability, Battler user, Battler target, BattleMove move,
                                                    float[] mults, int baseDmg, String type) {
        init();
        DamageCalcUserAbility handler = DamageCalcUserAbility.get(ability);
        if (handler != null) {
            handler.apply(ability, user, target, move, mults, baseDmg, type);   // :304
        }
    }

    /** {@code triggerDamageCalcUserAllyAbility} (BattleHandlers.rb:307-309) - return value ignored. */
    public static void triggerDamageCalcUserAllyAbility(String ability, Battler user, Battler target, BattleMove move,
                                                        float[] mults, int baseDmg, String type) {
        init();
        DamageCalcUserAllyAbility handler = DamageCalcUserAllyAbility.get(ability);
        if (handler != null) {
            handler.apply(ability, user, target, move, mults, baseDmg, type);   // :308
        }
    }

    /** {@code triggerDamageCalcTargetAbility} (BattleHandlers.rb:311-313) - return value ignored. */
    public static void triggerDamageCalcTargetAbility(String ability, Battler user, Battler target, BattleMove move,
                                                      float[] mults, int baseDmg, String type) {
        init();
        DamageCalcTargetAbility handler = DamageCalcTargetAbility.get(ability);
        if (handler != null) {
            handler.apply(ability, user, target, move, mults, baseDmg, type);   // :312
        }
    }

    /** {@code triggerDamageCalcTargetAbilityNonIgnorable} (BattleHandlers.rb:315-317) - return value ignored. */
    public static void triggerDamageCalcTargetAbilityNonIgnorable(String ability, Battler user, Battler target,
                                                                  BattleMove move, float[] mults, int baseDmg, String type) {
        init();
        DamageCalcTargetAbilityNonIgnorable handler = DamageCalcTargetAbilityNonIgnorable.get(ability);
        if (handler != null) {
            handler.apply(ability, user, target, move, mults, baseDmg, type);   // :316
        }
    }

    /** {@code triggerDamageCalcTargetAllyAbility} (BattleHandlers.rb:319-321) - return value ignored. */
    public static void triggerDamageCalcTargetAllyAbility(String ability, Battler user, Battler target, BattleMove move,
                                                          float[] mults, int baseDmg, String type) {
        init();
        DamageCalcTargetAllyAbility handler = DamageCalcTargetAllyAbility.get(ability);
        if (handler != null) {
            handler.apply(ability, user, target, move, mults, baseDmg, type);   // :320
        }
    }

    /** {@code triggerDamageCalcUserItem} (BattleHandlers.rb:323-325) - return value ignored. */
    public static void triggerDamageCalcUserItem(String item, Battler user, Battler target, BattleMove move,
                                                 float[] mults, int baseDmg, String type) {
        init();
        DamageCalcUserItem handler = DamageCalcUserItem.get(item);
        if (handler != null) {
            handler.apply(item, user, target, move, mults, baseDmg, type);    // :324
        }
    }

    /** {@code triggerDamageCalcTargetItem} (BattleHandlers.rb:327-329) - return value ignored. */
    public static void triggerDamageCalcTargetItem(String item, Battler user, Battler target, BattleMove move,
                                                   float[] mults, int baseDmg, String type) {
        init();
        DamageCalcTargetItem handler = DamageCalcTargetItem.get(item);
        if (handler != null) {
            handler.apply(item, user, target, move, mults, baseDmg, type);    // :328
        }
    }

    /** {@code triggerCriticalCalcUserAbility} (BattleHandlers.rb:333-336); {@code c} is an integer count. */
    public static int triggerCriticalCalcUserAbility(String ability, Battler user, Battler target, int c) {
        init();
        CriticalCalcUserAbility handler = CriticalCalcUserAbility.get(ability);
        Integer ret = handler != null ? handler.apply(ability, user, target, c) : null;
        return ret != null ? ret : c;                                        // :335
    }

    /** {@code triggerCriticalCalcTargetAbility} (BattleHandlers.rb:338-341). */
    public static int triggerCriticalCalcTargetAbility(String ability, Battler user, Battler target, int c) {
        init();
        CriticalCalcTargetAbility handler = CriticalCalcTargetAbility.get(ability);
        Integer ret = handler != null ? handler.apply(ability, user, target, c) : null;
        return ret != null ? ret : c;                                        // :340
    }

    /** {@code triggerCriticalCalcUserItem} (BattleHandlers.rb:343-346). */
    public static int triggerCriticalCalcUserItem(String item, Battler user, Battler target, int c) {
        init();
        CriticalCalcUserItem handler = CriticalCalcUserItem.get(item);
        Integer ret = handler != null ? handler.apply(item, user, target, c) : null;
        return ret != null ? ret : c;                                        // :345
    }

    /** {@code triggerCriticalCalcTargetItem} (BattleHandlers.rb:348-351). */
    public static int triggerCriticalCalcTargetItem(String item, Battler user, Battler target, int c) {
        init();
        CriticalCalcTargetItem handler = CriticalCalcTargetItem.get(item);
        Integer ret = handler != null ? handler.apply(item, user, target, c) : null;
        return ret != null ? ret : c;                                        // :350
    }

    /** {@code triggerTargetAbilityOnHit} (BattleHandlers.rb:355-357) - return value ignored. */
    public static void triggerTargetAbilityOnHit(String ability, Battler user, Battler target, BattleMove move,
                                                 Battle battle) {
        init();
        TargetAbilityOnHit handler = TargetAbilityOnHit.get(ability);
        if (handler != null) {
            handler.apply(ability, user, target, move, battle);               // :356
        }
    }

    /** {@code triggerUserAbilityOnHit} (BattleHandlers.rb:359-361) - return value ignored. */
    public static void triggerUserAbilityOnHit(String ability, Battler user, Battler target, BattleMove move,
                                               Battle battle) {
        init();
        UserAbilityOnHit handler = UserAbilityOnHit.get(ability);
        if (handler != null) {
            handler.apply(ability, user, target, move, battle);               // :360
        }
    }

    /** {@code triggerTargetItemOnHit} (BattleHandlers.rb:363-365) - return value ignored. */
    public static void triggerTargetItemOnHit(String item, Battler user, Battler target, BattleMove move,
                                              Battle battle) {
        init();
        TargetItemOnHit handler = TargetItemOnHit.get(item);
        if (handler != null) {
            handler.apply(item, user, target, move, battle);                  // :364
        }
    }

    /** {@code triggerTargetItemOnHitPositiveBerry} (BattleHandlers.rb:367-370). */
    public static boolean triggerTargetItemOnHitPositiveBerry(String item, Battler battler, Battle battle,
                                                              boolean forced) {
        init();
        TargetItemOnHitPositiveBerry handler = TargetItemOnHitPositiveBerry.get(item);
        Boolean ret = handler != null ? handler.apply(item, battler, battle, forced) : null;
        return ret != null ? ret : false;                                    // :369
    }

    /** {@code triggerUserAbilityEndOfMove} (BattleHandlers.rb:374-376) - return value ignored. */
    public static void triggerUserAbilityEndOfMove(String ability, Battler user, Array<Battler> targets,
                                                   BattleMove move, Battle battle) {
        init();
        UserAbilityEndOfMove handler = UserAbilityEndOfMove.get(ability);
        if (handler != null) {
            handler.apply(ability, user, targets, move, battle);              // :375
        }
    }

    /** {@code triggerTargetItemAfterMoveUse} (BattleHandlers.rb:378-380) - return value ignored. */
    public static void triggerTargetItemAfterMoveUse(String item, Battler battler, Battler user, BattleMove move,
                                                     Array<Integer> switched, Battle battle) {
        init();
        TargetItemAfterMoveUse handler = TargetItemAfterMoveUse.get(item);
        if (handler != null) {
            handler.apply(item, battler, user, move, switched, battle);       // :379
        }
    }

    /** {@code triggerUserItemAfterMoveUse} (BattleHandlers.rb:382-384) - return value ignored. */
    public static void triggerUserItemAfterMoveUse(String item, Battler user, Array<Battler> targets, BattleMove move,
                                                   int numHits, Battle battle) {
        init();
        UserItemAfterMoveUse handler = UserItemAfterMoveUse.get(item);
        if (handler != null) {
            handler.apply(item, user, targets, move, numHits, battle);        // :383
        }
    }

    /** {@code triggerTargetAbilityAfterMoveUse} (BattleHandlers.rb:386-388) - return value ignored. */
    public static void triggerTargetAbilityAfterMoveUse(String ability, Battler target, Battler user, BattleMove move,
                                                        Array<Integer> switched, Battle battle) {
        init();
        TargetAbilityAfterMoveUse handler = TargetAbilityAfterMoveUse.get(ability);
        if (handler != null) {
            handler.apply(ability, target, user, move, switched, battle);     // :387
        }
    }

    /** {@code triggerEndOfMoveItem} (BattleHandlers.rb:390-393). */
    public static boolean triggerEndOfMoveItem(String item, Battler battler, Battle battle, boolean forced) {
        init();
        EndOfMoveItem handler = EndOfMoveItem.get(item);
        Boolean ret = handler != null ? handler.apply(item, battler, battle, forced) : null;
        return ret != null ? ret : false;                                    // :392
    }

    /** {@code triggerEndOfMoveStatRestoreItem} (BattleHandlers.rb:395-398). */
    public static boolean triggerEndOfMoveStatRestoreItem(String item, Battler battler, Battle battle, boolean forced) {
        init();
        EndOfMoveStatRestoreItem handler = EndOfMoveStatRestoreItem.get(item);
        Boolean ret = handler != null ? handler.apply(item, battler, battle, forced) : null;
        return ret != null ? ret : false;                                    // :397
    }

    /** {@code triggerItemOnStatLoss} (BattleHandlers.rb:402-404) - return value ignored. */
    public static void triggerItemOnStatLoss(String item, Battler battler, Battler user, BattleMove move,
                                             Array<Integer> switched, Battle battle) {
        init();
        ItemOnStatLoss handler = ItemOnStatLoss.get(item);
        if (handler != null) {
            handler.apply(item, battler, user, move, switched, battle);       // :403
        }
    }

    /** {@code triggerExpGainModifierItem} (BattleHandlers.rb:408-411); the default is {@code -1}. */
    public static int triggerExpGainModifierItem(String item, Battler battler, int exp) {
        init();
        ExpGainModifierItem handler = ExpGainModifierItem.get(item);
        Integer ret = handler != null ? handler.apply(item, battler, exp) : null;
        return ret != null ? ret : -1;                                       // :410
    }

    /**
     * {@code triggerEVGainModifierItem} (BattleHandlers.rb:413-417): return
     * false when the item has no handler, otherwise run it and return true - the
     * handler's own return value is deliberately discarded (Ruby ignores it).
     */
    public static boolean triggerEVGainModifierItem(String item, Battler battler, int[] evarray) {
        init();
        if (!EVGainModifierItem.has(item)) {                                 // :414
            return false;
        }
        EVGainModifierItem handler = EVGainModifierItem.get(item);
        if (handler != null) {
            handler.apply(item, battler, evarray);                            // :415
        }
        return true;                                                         // :416
    }

    /** {@code triggerWeatherExtenderItem} (BattleHandlers.rb:421-424); the default is {@code duration}. */
    public static int triggerWeatherExtenderItem(String item, int weather, int duration, Battler battler,
                                                 Battle battle) {
        init();
        WeatherExtenderItem handler = WeatherExtenderItem.get(item);
        Integer ret = handler != null ? handler.apply(item, weather, duration, battler, battle) : null;
        return ret != null ? ret : duration;                                 // :423
    }

    /** {@code triggerTerrainExtenderItem} (BattleHandlers.rb:426-429); the default is {@code duration}. */
    public static int triggerTerrainExtenderItem(String item, int terrain, int duration, Battler battler,
                                                 Battle battle) {
        init();
        TerrainExtenderItem handler = TerrainExtenderItem.get(item);
        Integer ret = handler != null ? handler.apply(item, terrain, duration, battler, battle) : null;
        return ret != null ? ret : duration;                                 // :428
    }

    /** {@code triggerTerrainStatBoostItem} (BattleHandlers.rb:431-434). */
    public static boolean triggerTerrainStatBoostItem(String item, Battler battler, Battle battle) {
        init();
        TerrainStatBoostItem handler = TerrainStatBoostItem.get(item);
        Boolean ret = handler != null ? handler.apply(item, battler, battle) : null;
        return ret != null ? ret : false;                                    // :433
    }

    /** {@code triggerEORWeatherAbility} (BattleHandlers.rb:438-440) - return value ignored. */
    public static void triggerEORWeatherAbility(String ability, int weather, Battler battler, Battle battle) {
        init();
        EORWeatherAbility handler = EORWeatherAbility.get(ability);
        if (handler != null) {
            handler.apply(ability, weather, battler, battle);                 // :439
        }
    }

    /** {@code triggerEORHealingAbility} (BattleHandlers.rb:442-444) - return value ignored. */
    public static void triggerEORHealingAbility(String ability, Battler battler, Battle battle) {
        init();
        EORHealingAbility handler = EORHealingAbility.get(ability);
        if (handler != null) {
            handler.apply(ability, battler, battle);                          // :443
        }
    }

    /** {@code triggerEORHealingItem} (BattleHandlers.rb:446-448) - return value ignored. */
    public static void triggerEORHealingItem(String item, Battler battler, Battle battle) {
        init();
        EORHealingItem handler = EORHealingItem.get(item);
        if (handler != null) {
            handler.apply(item, battler, battle);                             // :447
        }
    }

    /** {@code triggerEOREffectAbility} (BattleHandlers.rb:450-452) - return value ignored. */
    public static void triggerEOREffectAbility(String ability, Battler battler, Battle battle) {
        init();
        EOREffectAbility handler = EOREffectAbility.get(ability);
        if (handler != null) {
            handler.apply(ability, battler, battle);                          // :451
        }
    }

    /** {@code triggerEOREffectItem} (BattleHandlers.rb:454-456) - return value ignored. */
    public static void triggerEOREffectItem(String item, Battler battler, Battle battle) {
        init();
        EOREffectItem handler = EOREffectItem.get(item);
        if (handler != null) {
            handler.apply(item, battler, battle);                             // :455
        }
    }

    /** {@code triggerEORGainItemAbility} (BattleHandlers.rb:458-460) - return value ignored. */
    public static void triggerEORGainItemAbility(String ability, Battler battler, Battle battle) {
        init();
        EORGainItemAbility handler = EORGainItemAbility.get(ability);
        if (handler != null) {
            handler.apply(ability, battler, battle);                          // :459
        }
    }

    /** {@code triggerCertainSwitchingUserAbility} (BattleHandlers.rb:464-467). */
    public static boolean triggerCertainSwitchingUserAbility(String ability, Battler switcher, Battle battle) {
        init();
        CertainSwitchingUserAbility handler = CertainSwitchingUserAbility.get(ability);
        Boolean ret = handler != null ? handler.apply(ability, switcher, battle) : null;
        return ret != null ? ret : false;                                    // :466
    }

    /** {@code triggerCertainSwitchingUserItem} (BattleHandlers.rb:469-472). */
    public static boolean triggerCertainSwitchingUserItem(String item, Battler switcher, Battle battle) {
        init();
        CertainSwitchingUserItem handler = CertainSwitchingUserItem.get(item);
        Boolean ret = handler != null ? handler.apply(item, switcher, battle) : null;
        return ret != null ? ret : false;                                    // :471
    }

    /** {@code triggerTrappingTargetAbility} (BattleHandlers.rb:474-477). */
    public static boolean triggerTrappingTargetAbility(String ability, Battler switcher, Battler bearer, Battle battle) {
        init();
        TrappingTargetAbility handler = TrappingTargetAbility.get(ability);
        Boolean ret = handler != null ? handler.apply(ability, switcher, bearer, battle) : null;
        return ret != null ? ret : false;                                    // :476
    }

    /** {@code triggerTrappingTargetItem} (BattleHandlers.rb:479-482). */
    public static boolean triggerTrappingTargetItem(String item, Battler switcher, Battler bearer, Battle battle) {
        init();
        TrappingTargetItem handler = TrappingTargetItem.get(item);
        Boolean ret = handler != null ? handler.apply(item, switcher, bearer, battle) : null;
        return ret != null ? ret : false;                                    // :481
    }

    /**
     * {@code triggerAbilityOnSwitchIn} (BattleHandlers.rb:484-491): the caller's
     * own group ({@code :485}), then Commander's extra pass over the same side's
     * battlers ({@code :486-490}).
     */
    public static void triggerAbilityOnSwitchIn(String ability, Battler battler, Battle battle) {
        init();
        AbilityOnSwitchIn handler = AbilityOnSwitchIn.get(ability);
        if (handler != null) {
            handler.apply(ability, battler, battle);                          // :485
        }
        for (Battler b : sameSideBattlers(battle, battler.index)) {           // :486
            if (!b.hasActiveAbility("COMMANDER")) {                           // :487 Battler#hasActiveAbility? (PokeBattle_Battler:387)
                continue;
            }
            if (b.effects.truthy(PBEffects.Battler.Commander)) {              // :488 b.effects (PokeBattle_Battler:23)
                continue;
            }
            String commanderAbility = b.ability;                              // :489 b.ability (PokeBattle_Battler:11)
            AbilityOnSwitchIn commander = AbilityOnSwitchIn.get(commanderAbility);   // :489
            if (commander != null) {
                commander.apply(commanderAbility, b, battle);
            }
        }
    }

    /** {@code triggerItemOnSwitchIn} (BattleHandlers.rb:492-494) - return value ignored. */
    public static void triggerItemOnSwitchIn(String item, Battler battler, Battle battle) {
        init();
        ItemOnSwitchIn handler = ItemOnSwitchIn.get(item);
        if (handler != null) {
            handler.apply(item, battler, battle);                             // :493
        }
    }

    /** {@code triggerItemOnIntimidated} (BattleHandlers.rb:496-499). */
    public static boolean triggerItemOnIntimidated(String item, Battler battler, Battle battle) {
        init();
        ItemOnIntimidated handler = ItemOnIntimidated.get(item);
        Boolean ret = handler != null ? handler.apply(item, battler, battle) : null;
        return ret != null ? ret : false;                                    // :498
    }

    /** {@code triggerAbilityOnSwitchOut} (BattleHandlers.rb:501-503) - return value ignored. */
    public static void triggerAbilityOnSwitchOut(String ability, Battler battler, boolean endOfBattle) {
        init();
        AbilityOnSwitchOut handler = AbilityOnSwitchOut.get(ability);
        if (handler != null) {
            handler.apply(ability, battler, endOfBattle);                     // :502
        }
    }

    /** {@code triggerAbilityChangeOnBattlerFainting} (BattleHandlers.rb:505-507) - return value ignored. */
    public static void triggerAbilityChangeOnBattlerFainting(String ability, Battler battler, Battler fainted,
                                                             Battle battle) {
        init();
        AbilityChangeOnBattlerFainting handler = AbilityChangeOnBattlerFainting.get(ability);
        if (handler != null) {
            handler.apply(ability, battler, fainted, battle);                 // :506
        }
    }

    /** {@code triggerAbilityOnBattlerFainting} (BattleHandlers.rb:509-511) - return value ignored. */
    public static void triggerAbilityOnBattlerFainting(String ability, Battler battler, Battler fainted, Battle battle) {
        init();
        AbilityOnBattlerFainting handler = AbilityOnBattlerFainting.get(ability);
        if (handler != null) {
            handler.apply(ability, battler, fainted, battle);                 // :510
        }
    }

    /** {@code triggerRunFromBattleAbility} (BattleHandlers.rb:515-518). */
    public static boolean triggerRunFromBattleAbility(String ability, Battler battler) {
        init();
        RunFromBattleAbility handler = RunFromBattleAbility.get(ability);
        Boolean ret = handler != null ? handler.apply(ability, battler) : null;
        return ret != null ? ret : false;                                    // :517
    }

    /** {@code triggerRunFromBattleItem} (BattleHandlers.rb:520-523). */
    public static boolean triggerRunFromBattleItem(String item, Battler battler) {
        init();
        RunFromBattleItem handler = RunFromBattleItem.get(item);
        Boolean ret = handler != null ? handler.apply(item, battler) : null;
        return ret != null ? ret : false;                                    // :522
    }

    /** {@code triggerAbilityOnTerrainChange} (BattleHandlers.rb:525-527) - return value ignored. */
    public static void triggerAbilityOnTerrainChange(String ability, Battler battler, Battle battle,
                                                     boolean ability_changed) {
        init();
        AbilityOnTerrainChange handler = AbilityOnTerrainChange.get(ability);
        if (handler != null) {
            handler.apply(ability, battler, battle, ability_changed);         // :526
        }
    }

    // =====================================================================
    // Helper
    // =====================================================================

    /**
     * The plugin's {@code battle.allSameSideBattlers(idxBattler)}
     * (AI_Move_EffectScores:3867-3870) =
     * {@code @battlers.select { |b| b && !b.fainted? && !b.opposes?(idxBattler) }},
     * where {@code opposes?} is {@code (@index&1)!=(i&1)}
     * (PokeBattle_Battler:784-787). {@code Battle} has no such method and is
     * outside this task's write scope, so the fielded battlers are visited via
     * {@link Battle#battlerAt(int)}: this runtime fields one battler per side,
     * in slots 0 and 1 ({@code Battle.refreshFieldIndices}), which is exactly
     * the non-empty subset of Ruby's {@code @battlers} here.
     */
    private static Array<Battler> sameSideBattlers(Battle battle, int idxBattler) {
        Array<Battler> result = new Array<>();
        for (int idx = 0; idx < 2; idx++) {
            Battler b = battle.battlerAt(idx);
            if (b != null && (idx & 1) == (idxBattler & 1) && !b.fainted()) {
                result.add(b);
            }
        }
        return result;
    }
}
