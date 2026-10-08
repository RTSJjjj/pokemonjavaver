package pokemon.runtime.battle.movefx;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import pokemon.runtime.battle.BattleMove;
import pokemon.runtime.battle.Battler;
import pokemon.runtime.pokemon.Pokemon;

/**
 * Stage 4 / L2: the move-effect strategy. One instance per <b>function code</b>
 * stands in for the plugin's one {@code PokeBattle_Move_XXX} subclass per
 * function code, so the engine can ask "what does this move do?" without
 * subclassing {@link BattleMove}.
 *
 * <h2>Ruby &rarr; Java shape</h2>
 * The plugin puts the effect on the move object itself:
 * {@code PokeBattle_Move.pbFromPBMove} (PokeBattle_Move.rb:51-59) picks
 * {@code PokeBattle_Move_<function code>} by name, and 496 such classes override
 * the hooks below. Here the effect is a separate object and every hook takes the
 * move as its first parameter, so <b>the first parameter is always
 * {@code BattleMove move} and corresponds to Ruby's {@code self}</b>.
 *
 * <h2>Why every hook is declared, not just the overridden ones</h2>
 * The plugin's 24 reusable base classes ({@link MoveEffectsGeneric}) are
 * themselves subclasses that override hook <i>predicates</i> as well as the
 * {@code pb*} hooks (e.g. {@code PokeBattle_Confusion#physicalMove?}
 * Move_Effects_Generic.rb:42). A method that is not declared here cannot be
 * overridden by a strategy subclass, so the interface covers the whole
 * overridable surface of {@code PokeBattle_Move}, {@code Move_Usage} and
 * {@code Move_Usage_Calculations}: 105 methods.
 *
 * <h2>Documented deviations (task-11 decisions)</h2>
 * <ul>
 * <li>{@code targets} &rarr; {@code Array<Battler>}; {@code switchedBattlers}
 *     &rarr; {@link IntArray} because the plugin stores <i>battler indices</i>
 *     in it (Battler_UseMove_TriggerEffects:130 {@code switchedBattlers = []},
 *     Move_Effects_080-0FF:3193 {@code switchedBattlers.push(b.index)}).</li>
 * <li>{@code mults}/{@code mods}/{@code multipliers}/{@code modifiers} &rarr;
 *     {@code float[]} (the plugin multiplies them by {@code 1.3}/{@code 1.5}).
 *     A single scalar multiplier is {@code float}.</li>
 * <li>Type identity is the internal-name {@code String} in this runtime, so
 *     {@code thisType}/{@code moveType}/{@code defType} are {@code String}, and
 *     the plugin's "no type" sentinel {@code -1} is {@code null} (decision 2).</li>
 * <li>{@code pbGetAttackStats}/{@code pbGetDefenseStats} return
 *     {@link MoveStats} (decision 1).</li>
 * <li>{@code pbFailsAgainstTarget?} is declared with the base class's two
 *     parameters; the plugin's one three-parameter override
 *     (Move_Effects_180-1FF:1237) is an arity conflict - see
 *     {@link #pbFailsAgainstTarget} (decision 6).</li>
 * <li>{@code pbFixedDamage}/{@code pbHealAmount}/{@code pbRecoilDamage} have no
 *     default in the plugin's base class at all - their {@code return 1} lives
 *     in the abstract bases {@code PokeBattle_FixedDamageMove} (Move_Effects_Generic.rb:474),
 *     {@code PokeBattle_HealingMove} (:570) and {@code PokeBattle_RecoilMove}
 *     (:594). See {@link MoveEffectBase} (decision 8).</li>
 * </ul>
 *
 * @see MoveEffectBase the default bodies
 * @see MoveEffectsGeneric the 24 reusable base classes
 * @see MoveEffectRegistry function code &rarr; effect
 */
public interface MoveEffect {

    // ==================================================================
    // A. About the move (PokeBattle_Move.rb:64-142, Move_Usage.rb:38/40)
    // ==================================================================

    /** {@code pbTarget(_user)} (PokeBattle_Move.rb:64). */
    int pbTarget(BattleMove move, Battler user);

    /** {@code totalpp} (PokeBattle_Move.rb:66-70). */
    int totalpp(BattleMove move);

    /** {@code physicalMove?(thisType=nil)} (PokeBattle_Move.rb:74-79). */
    boolean physicalMove(BattleMove move, String thisType);

    /** {@code specialMove?(thisType=nil)} (PokeBattle_Move.rb:83-88). */
    boolean specialMove(BattleMove move, String thisType);

    /** {@code damagingMove?} (PokeBattle_Move.rb:90). */
    boolean damagingMove(BattleMove move);

    /** {@code statusMove?} (PokeBattle_Move.rb:91). */
    boolean statusMove(BattleMove move);

    /** {@code usableWhenAsleep?} (PokeBattle_Move.rb:93). */
    boolean usableWhenAsleep(BattleMove move);

    /** {@code unusableInGravity?} (PokeBattle_Move.rb:94). */
    boolean unusableInGravity(BattleMove move);

    /** {@code healingMove?} (PokeBattle_Move.rb:95). */
    boolean healingMove(BattleMove move);

    /** {@code recoilMove?} (PokeBattle_Move.rb:96). */
    boolean recoilMove(BattleMove move);

    /** {@code flinchingMove?} (PokeBattle_Move.rb:97). */
    boolean flinchingMove(BattleMove move);

    /** {@code callsAnotherMove?} (PokeBattle_Move.rb:98). */
    boolean callsAnotherMove(BattleMove move);

    /** {@code multiHitMove?} (PokeBattle_Move.rb:101). */
    boolean multiHitMove(BattleMove move);

    /** {@code chargingTurnMove?} (PokeBattle_Move.rb:102). */
    boolean chargingTurnMove(BattleMove move);

    /** {@code successCheckPerHit?} (PokeBattle_Move.rb:103). */
    boolean successCheckPerHit(BattleMove move);

    /** {@code hitsFlyingTargets?} (PokeBattle_Move.rb:104). */
    boolean hitsFlyingTargets(BattleMove move);

    /** {@code hitsDiggingTargets?} (PokeBattle_Move.rb:105). */
    boolean hitsDiggingTargets(BattleMove move);

    /** {@code hitsDivingTargets?} (PokeBattle_Move.rb:106). */
    boolean hitsDivingTargets(BattleMove move);

    /** {@code ignoresReflect?} (PokeBattle_Move.rb:107). */
    boolean ignoresReflect(BattleMove move);

    /** {@code cannotRedirect?} (PokeBattle_Move.rb:108). */
    boolean cannotRedirect(BattleMove move);

    /** {@code worksWithNoTargets?} (PokeBattle_Move.rb:109). */
    boolean worksWithNoTargets(BattleMove move);

    /** {@code damageReducedByBurn?} (PokeBattle_Move.rb:110). */
    boolean damageReducedByBurn(BattleMove move);

    /** {@code triggersHyperMode?} (PokeBattle_Move.rb:111). */
    boolean triggersHyperMode(BattleMove move);

    /** {@code contactMove?} - {@code @flags[/a/]} (PokeBattle_Move.rb:113). */
    boolean contactMove(BattleMove move);

    /** {@code canProtectAgainst?} - {@code @flags[/b/]} (PokeBattle_Move.rb:114). */
    boolean canProtectAgainst(BattleMove move);

    /** {@code canMagicCoat?} - {@code @flags[/c/]} (PokeBattle_Move.rb:115). */
    boolean canMagicCoat(BattleMove move);

    /** {@code canSnatch?} - {@code @flags[/d/]} (PokeBattle_Move.rb:116). */
    boolean canSnatch(BattleMove move);

    /** {@code canMirrorMove?} - {@code @flags[/e/]} (PokeBattle_Move.rb:117). */
    boolean canMirrorMove(BattleMove move);

    /** {@code canKingsRock?} - {@code @flags[/f/]} (PokeBattle_Move.rb:118). */
    boolean canKingsRock(BattleMove move);

    /** {@code thawsUser?} - {@code @flags[/g/]} (PokeBattle_Move.rb:119). */
    boolean thawsUser(BattleMove move);

    /** {@code highCriticalRate?} - {@code @flags[/h/]} (PokeBattle_Move.rb:120). */
    boolean highCriticalRate(BattleMove move);

    /** {@code bitingMove?} - {@code @flags[/i/]} (PokeBattle_Move.rb:121). */
    boolean bitingMove(BattleMove move);

    /** {@code punchingMove?} - {@code @flags[/j/]} (PokeBattle_Move.rb:122). */
    boolean punchingMove(BattleMove move);

    /** {@code soundMove?} - {@code @flags[/k/]} (PokeBattle_Move.rb:123). */
    boolean soundMove(BattleMove move);

    /** {@code powderMove?} - {@code @flags[/l/]} (PokeBattle_Move.rb:124). */
    boolean powderMove(BattleMove move);

    /** {@code pulseMove?} - {@code @flags[/m/]} (PokeBattle_Move.rb:125). */
    boolean pulseMove(BattleMove move);

    /** {@code bombMove?} - {@code @flags[/n/]} (PokeBattle_Move.rb:126). */
    boolean bombMove(BattleMove move);

    /** {@code danceMove?} - {@code @flags[/o/]} (PokeBattle_Move.rb:127). */
    boolean danceMove(BattleMove move);

    /** {@code slicingMove?} - {@code @flags[/p/]} (PokeBattle_Move.rb:128). */
    boolean slicingMove(BattleMove move);

    /** {@code windMove?} - {@code @flags[/r/]} (PokeBattle_Move.rb:129). */
    boolean windMove(BattleMove move);

    /** {@code tramplesMinimize?(_param=1)} (PokeBattle_Move.rb:132). */
    boolean tramplesMinimize(BattleMove move, int param);

    /** {@code nonLethal?(_user,_target)} (PokeBattle_Move.rb:133). */
    boolean nonLethal(BattleMove move, Battler user, Battler target);

    /** {@code ignoresSubstitute?(user)} (PokeBattle_Move.rb:135-142). */
    boolean ignoresSubstitute(BattleMove move, Battler user);

    /** {@code pbDamagingMove?} (Move_Usage.rb:38). */
    boolean pbDamagingMove(BattleMove move);

    /** {@code pbContactMove?(user)} (Move_Usage.rb:40-46). */
    boolean pbContactMove(BattleMove move, Battler user);

    // ==================================================================
    // B. Effect methods per move usage (Move_Usage.rb:5-161)
    // ==================================================================

    /** {@code pbCanChooseMove?(user,commandPhase,showMessages)} (Move_Usage.rb:5). */
    boolean pbCanChooseMove(BattleMove move, Battler user, boolean commandPhase, boolean showMessages);

    /** {@code pbDisplayChargeMessage(user)} (Move_Usage.rb:6). */
    void pbDisplayChargeMessage(BattleMove move, Battler user);

    /** {@code pbOnStartUse(user,targets)} (Move_Usage.rb:7). */
    void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets);

    /** {@code pbAddTarget(targets,user)} (Move_Usage.rb:8) - note the reversed parameter order. */
    void pbAddTarget(BattleMove move, Array<Battler> targets, Battler user);

    /** {@code pbChangeUsageCounters(user,specialUsage)} (Move_Usage.rb:11-20). */
    void pbChangeUsageCounters(BattleMove move, Battler user, boolean specialUsage);

    /** {@code pbDisplayUseMessage(user)} (Move_Usage.rb:23-25). */
    void pbDisplayUseMessage(BattleMove move, Battler user);

    /** {@code pbMissMessage(user,target)} (Move_Usage.rb:27). */
    boolean pbMissMessage(BattleMove move, Battler user, Battler target);

    /** {@code pbIsChargingTurn?(user)} (Move_Usage.rb:37). */
    boolean pbIsChargingTurn(BattleMove move, Battler user);

    /** {@code pbNumHits(user,targets)} (Move_Usage.rb:50-58). */
    int pbNumHits(BattleMove move, Battler user, Array<Battler> targets);

    /** {@code pbOverrideSuccessCheckPerHit(user,target)} (Move_Usage.rb:63). */
    boolean pbOverrideSuccessCheckPerHit(BattleMove move, Battler user, Battler target);

    /** {@code pbCrashDamage(user)} (Move_Usage.rb:64). */
    void pbCrashDamage(BattleMove move, Battler user);

    /** {@code pbInitialEffect(user,targets,hitNum)} (Move_Usage.rb:65). */
    void pbInitialEffect(BattleMove move, Battler user, Array<Battler> targets, int hitNum);

    /** {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} (Move_Usage.rb:67-74). */
    void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets, int hitNum,
                         boolean showAnimation);

    /** {@code pbSelfKO(user)} (Move_Usage.rb:76). */
    void pbSelfKO(BattleMove move, Battler user);

    /** {@code pbEffectWhenDealingDamage(user,target)} (Move_Usage.rb:77). */
    void pbEffectWhenDealingDamage(BattleMove move, Battler user, Battler target);

    /** {@code pbEffectAgainstTarget(user,target)} (Move_Usage.rb:78). */
    void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target);

    /** {@code pbEffectGeneral(user)} (Move_Usage.rb:79). */
    void pbEffectGeneral(BattleMove move, Battler user);

    /** {@code pbAdditionalEffect(user,target)} (Move_Usage.rb:80). */
    void pbAdditionalEffect(BattleMove move, Battler user, Battler target);

    /** {@code pbEffectAfterAllHits(user,target)} (Move_Usage.rb:81). */
    void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target);

    /** {@code pbSwitchOutTargetsEffect(user,targets,numHits,switchedBattlers)} (Move_Usage.rb:82). */
    void pbSwitchOutTargetsEffect(BattleMove move, Battler user, Array<Battler> targets, int numHits,
                                  IntArray switchedBattlers);

    /** {@code pbEndOfMoveUsageEffect(user,targets,numHits,switchedBattlers)} (Move_Usage.rb:83). */
    void pbEndOfMoveUsageEffect(BattleMove move, Battler user, Array<Battler> targets, int numHits,
                                IntArray switchedBattlers);

    /** {@code pbImmunityByAbility(user,target)} (Move_Usage.rb:88-96). */
    boolean pbImmunityByAbility(BattleMove move, Battler user, Battler target);

    /** {@code pbMoveFailed?(user,targets)} (Move_Usage.rb:102). */
    boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets);

    /**
     * {@code pbFailsAgainstTarget?(user,target)} (Move_Usage.rb:104).
     *
     * <p><b>Plugin arity conflict (decision 6).</b> The base class takes two
     * parameters, but {@code PokeBattle_Move_1BE} overrides it with a required
     * third one - {@code Move_Effects_180-1FF:1237 def pbFailsAgainstTarget?(user,target,show_message)}
     * - while the only call site, {@code Battler_UseMove_SuccessChecks:394},
     * passes two. In Ruby, using that move raises {@code ArgumentError}. Java
     * cannot have two arities for one method, so this runtime keeps the base
     * arity and a subclass that needs {@code show_message} holds it as its own
     * field (default {@code true}); see {@link MoveEffectsGeneric}.</p>
     */
    boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target);

    /** {@code pbMoveFailedLastInRound?(user)} (Move_Usage.rb:106-120). */
    boolean pbMoveFailedLastInRound(BattleMove move, Battler user);

    /** {@code pbMoveFailedTargetAlreadyMoved?(target)} (Move_Usage.rb:122-129). */
    boolean pbMoveFailedTargetAlreadyMoved(BattleMove move, Battler target);

    /** {@code pbMoveFailedAromaVeil?(user,target,showMessage=true)} (Move_Usage.rb:131-161). */
    boolean pbMoveFailedAromaVeil(BattleMove move, Battler user, Battler target, boolean showMessage);

    // ==================================================================
    // C. Calculations (Move_Usage_Calculations.rb)
    // ==================================================================

    /** {@code pbBaseType(user)} (Move_Usage_Calculations.rb:5-12). */
    String pbBaseType(BattleMove move, Battler user);

    /** {@code pbCalcType(user)} (Move_Usage_Calculations.rb:14-29). */
    String pbCalcType(BattleMove move, Battler user);

    /** {@code pbCalcTypeModSingle(moveType,defType,user,target)} (Move_Usage_Calculations.rb:34-71). */
    int pbCalcTypeModSingle(BattleMove move, String moveType, String defType, Battler user, Battler target);

    /** {@code pbCalcTypeMod(moveType,user,target)} (Move_Usage_Calculations.rb:73-103). */
    int pbCalcTypeMod(BattleMove move, String moveType, Battler user, Battler target);

    /** {@code pbBaseAccuracy(user,target)} (Move_Usage_Calculations.rb:108). */
    int pbBaseAccuracy(BattleMove move, Battler user, Battler target);

    /** {@code pbAccuracyCheck(user,target)} (Move_Usage_Calculations.rb:112-143). */
    boolean pbAccuracyCheck(BattleMove move, Battler user, Battler target);

    /** {@code pbCalcAccuracyModifiers(user,target,modifiers)} (Move_Usage_Calculations.rb:145-183). */
    void pbCalcAccuracyModifiers(BattleMove move, Battler user, Battler target, float[] modifiers);

    /**
     * {@code pbCalcAccuracyMultipliers(user,target,multipliers)} (Move_Effects_080-0FF:1135).
     *
     * <p>In the plugin this hook has no call site and its four overrides use an undefined
     * {@code modifiers}; the overrides are now transcribed on {@link #pbCalcAccuracyModifiers}, which
     * {@code pbAccuracyCheck} really calls. Kept only as the base-class home of the old name.</p>
     */
    void pbCalcAccuracyMultipliers(BattleMove move, Battler user, Battler target, float[] multipliers);

    /** {@code pbCritialOverride(user,target)} (Move_Usage_Calculations.rb:192) - the misspelling is the plugin's. */
    int pbCritialOverride(BattleMove move, Battler user, Battler target);

    /** {@code pbIsCritical?(user,target)} (Move_Usage_Calculations.rb:196-229). */
    boolean pbIsCritical(BattleMove move, Battler user, Battler target);

    /** {@code pbBaseDamage(baseDmg,user,target)} (Move_Usage_Calculations.rb:234). */
    int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target);

    /** {@code pbBaseDamageMultiplier(damageMult,user,target)} (Move_Usage_Calculations.rb:235). */
    float pbBaseDamageMultiplier(BattleMove move, float damageMult, Battler user, Battler target);

    /** {@code pbModifyDamage(damageMult,user,target)} (Move_Usage_Calculations.rb:236). */
    float pbModifyDamage(BattleMove move, float damageMult, Battler user, Battler target);

    /** {@code pbGetAttackStats(user,target)} (Move_Usage_Calculations.rb:238-243). */
    MoveStats pbGetAttackStats(BattleMove move, Battler user, Battler target);

    /** {@code pbGetDefenseStats(user,target)} (Move_Usage_Calculations.rb:245-250). */
    MoveStats pbGetDefenseStats(BattleMove move, Battler user, Battler target);

    /** {@code pbCalcDamage(user,target,numTargets=1)} (Move_Usage_Calculations.rb:252-295). */
    void pbCalcDamage(BattleMove move, Battler user, Battler target, int numTargets);

    /** {@code pbCalcDamageMultipliers(user,target,numTargets,type,baseDmg,multipliers)} (Move_Usage_Calculations.rb:296-546). */
    void pbCalcDamageMultipliers(BattleMove move, Battler user, Battler target, int numTargets, String type,
                                 int baseDmg, float[] multipliers);

    /** {@code pbAdditionalEffectChance(user,target,effectChance=0)} (Move_Usage_Calculations.rb:547-557). */
    int pbAdditionalEffectChance(BattleMove move, Battler user, Battler target, int effectChance);

    /** {@code pbFlinchChance(user,target)} (Move_Usage_Calculations.rb:561-576). */
    int pbFlinchChance(BattleMove move, Battler user, Battler target);

    // ==================================================================
    // D. Hooks that only the Move_Effects_* classes define
    // ==================================================================

    /** {@code pbFixedDamage(user,target)} (Move_Effects_Generic.rb:474, {@code PokeBattle_FixedDamageMove}). */
    int pbFixedDamage(BattleMove move, Battler user, Battler target);

    /** {@code pbHealAmount(user)} (Move_Effects_Generic.rb:570, {@code PokeBattle_HealingMove}). */
    int pbHealAmount(BattleMove move, Battler user);

    /** {@code pbRecoilDamage(user,target)} (Move_Effects_Generic.rb:594, {@code PokeBattle_RecoilMove}). */
    int pbRecoilDamage(BattleMove move, Battler user, Battler target);

    /** {@code pbChargingTurnMessage(user,targets)} (Move_Effects_Generic.rb:536). */
    void pbChargingTurnMessage(BattleMove move, Battler user, Array<Battler> targets);

    /** {@code pbAttackingTurnMessage(user,targets)} (Move_Effects_Generic.rb:540). */
    void pbAttackingTurnMessage(BattleMove move, Battler user, Array<Battler> targets);

    /** {@code pbChargingTurnEffect(user,target)} (Move_Effects_Generic.rb:543). */
    void pbChargingTurnEffect(BattleMove move, Battler user, Battler target);

    /** {@code pbAttackingTurnEffect(user,target)} (Move_Effects_Generic.rb:548). */
    void pbAttackingTurnEffect(BattleMove move, Battler user, Battler target);

    /** {@code pbProtectMessage(user)} (Move_Effects_Generic.rb:661). */
    void pbProtectMessage(BattleMove move, Battler user);

    /**
     * {@code addlEffect} (Move_Effects_000-07F:308, class
     * {@code PokeBattle_Move_014}) - the one override of an {@code attr_reader}
     * (PokeBattle_Move.rb:13).
     *
     * <p>Three places outside the move classes read {@code move.addlEffect}
     * ({@code BattleHandlers_Abilities:1080},
     * {@code Battler_UseMove_TriggerEffects:133},
     * {@code Move_Usage_Calculations:550}), so the hook is part of the frozen
     * surface. {@link MoveEffectBase} returns {@code move.additionalChance()},
     * i.e. the same value the existing {@link BattleMove} accessor already gives
     * (decision 7).</p>
     */
    int addlEffect(BattleMove move);

    /** {@code pbNaturalGiftBaseDamage(heldItem)} (Move_Effects_080-0FF:529); the item is its internal name. */
    int pbNaturalGiftBaseDamage(BattleMove move, String heldItem);

    /** {@code pbCheckFlingSuccess(user)} (Move_Effects_080-0FF:3715) - writes {@code @willFail}, returns nothing. */
    void pbCheckFlingSuccess(BattleMove move, Battler user);

    /** {@code pbAromatherapyHeal(pkmn,battler=nil)} (Move_Effects_000-07F:449). */
    void pbAromatherapyHeal(BattleMove move, Pokemon pkmn, Battler battler);

    /** The {@code battler=nil} form of {@code pbAromatherapyHeal} (Move_Effects_000-07F:449). */
    void pbAromatherapyHeal(BattleMove move, Pokemon pkmn);

    /**
     * {@code pbHitEffectivenessMessages(user,target,numTargets=1)}
     * (Move_Usage.rb:322-345).
     *
     * <p>Declared because {@code PokeBattle_Move_070} overrides it
     * (Move_Effects_000-07F:2518), but its body belongs to the battle flow
     * (Move_Usage.rb:166-430), which task-11 keeps out of this batch;
     * {@link MoveEffectBase} therefore does not implement the plugin's body.</p>
     */
    void pbHitEffectivenessMessages(BattleMove move, Battler user, Battler target, int numTargets);
}
