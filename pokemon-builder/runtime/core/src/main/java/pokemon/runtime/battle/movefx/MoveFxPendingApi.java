package pokemon.runtime.battle.movefx;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.battle.Battle;
import pokemon.runtime.battle.BattleMove;
import pokemon.runtime.battle.BattleSide;
import pokemon.runtime.battle.Battler;
import pokemon.runtime.battle.DamageState;
import pokemon.runtime.battle.EffectMap;

/**
 * Stage 4 / L2: the remaining stub layer of the move-effect block - <b>28
 * methods</b> after the two retirement passes. Round 1 (after task-12/13)
 * replaced every stub whose real {@code Battler}/{@code Battle} method had
 * landed and deleted the 44 that became unreferenced; the 21 that the in-flight
 * function-code batch still called were restored in between, and round 2 (after
 * the whole L2 batch landed) deleted the 8 that were then unreferenced. What
 * remains is what has no faithful counterpart yet - see the list below - and
 * this class disappears with the wiring step.
 *
 * <h2>What was retired</h2>
 * <p>The first pass had 59 stubs. Every stub whose real {@code Battler}/{@code
 * Battle} method has since landed was replaced by a direct call at its call
 * sites (120 call sites across {@link MoveEffectBase} and
 * {@link MoveEffectsGeneric}), and the 44 stubs that then had zero references
 * anywhere in the runtime were deleted. Each replacement is documented at its
 * call site, e.g. {@code damageState(x)} &rarr; {@code x.damageState},
 * {@code pbOwnSide(x)} &rarr; {@code x.pbOwnSide()},
 * {@code statStage(x,s)} &rarr; {@code x.stage(s)},
 * {@code pbRandom(b,n)} &rarr; {@code b.pbRandom(n)},
 * {@code PBTargets_fromName(s)} &rarr; {@code PBTargets.fromName(s)},
 * {@code setLastMoveFailed(x,v)} &rarr; {@code x.lastMoveFailed = v},
 * {@code moldBreaker(b)} &rarr; {@code b.moldBreaker},
 * {@code fieldEffects(b)} &rarr; {@code b.field.effects}.</p>
 *
 * <h2>What is left, and why</h2>
 * <ul>
 * <li>{@code attack} / {@code defense} / {@code spatk} / {@code spdef}: the
 *     runtime's {@code Battler.attack()}/{@code defense()}/{@code spAtk()}/
 *     {@code spDef()} are <b>stage-scaled</b> ({@code plainStats()} delegates to
 *     them too), while Ruby's {@code user.attack} is the raw stat that
 *     {@code Move_Usage_Calculations:238-250} scales itself - a direct
 *     replacement would apply the stages twice.</li>
 * <li>{@code movedThisRound}, {@code pbSleep}, {@code pbFreeze},
 *     {@code pbFlinch}, {@code pbCanFreeze}: no such {@code Battler} method has
 *     landed yet.</li>
 * <li>{@code PBTypes_isSpecialType}: the real
 *     {@code PBTypes.isSpecialType(PbsData,String)} needs a {@code PbsData},
 *     which the {@code physicalMove?}/{@code specialMove?} hook has no source
 *     for (and {@code MOVE_CATEGORY_PER_MOVE=true} makes that branch
 *     unreachable anyway).</li>
 * <li>{@code PBMoves_id}: {@code getConst(PBMoves,:X)} needs a
 *     {@code PbsData.move(name)} lookup plus a documented "0 when absent"
 *     fallback, which is a wiring decision rather than a rename.</li>
 * <li>{@code choiceIsMoveOrShift}, {@code choiceMove}, {@code eachAlly},
 *     {@code idxOwnSide}: still referenced by other teammates' files
 *     ({@code MoveEffects_080_0FF.java}, {@code AbilitiesDamageUser.java},
 *     {@code ItemsDamageTarget.java}), so they stay until those are
 *     retired too. The movefx files themselves no longer call them.</li>
 * </ul>
 *
 * <h2>Temporary by design</h2>
 * <p>Every method is a <b>throwing stub</b> - deliberately not a default value,
 * because returning one would silently invent plugin behaviour. When the real
 * methods land, each call site becomes a direct call and this class is
 * deleted.</p>
 *
 * <h2>Naming</h2>
 * <p>A plugin <i>module function</i> cannot keep the dot in Java, so
 * {@code PBTypes.isSpecialType?} is written {@code PBTypes_isSpecialType}. Each
 * javadoc names the Ruby source line.</p>
 */
public final class MoveFxPendingApi {

    private MoveFxPendingApi() {
    }

    // ==================================================================
    // Task-11 decision 2/3: the datatables that are not in this package yet
    // ==================================================================

    /** {@code PBTypes.isSpecialType?(type)} (PBTypes_Extra.rb:38-40). */
    public static boolean PBTypes_isSpecialType(String type) {
        throw new UnsupportedOperationException("M0 待接线: PBTypes_Extra.rb:38 isSpecialType?");
    }

    // ==================================================================
    // Battle methods used by MoveEffectBase's default bodies
    // ==================================================================

    /** {@code getConst(PBMoves,:BAMBOOSWORD)} (Move_Usage.rb:20). */
    public static int PBMoves_id(String internalName) {
        throw new UnsupportedOperationException("M0 待接线: getConst(PBMoves,:BAMBOOSWORD) (Move_Usage.rb:20)");
    }

    /**
     * The {@code @battle.choices[idx][0]} test shared by three call sites: true
     * when the command is {@code :UseMove} or {@code :Shift}
     * (Move_Usage.rb:110, :124 {@code @battle.choices[..][0]!=:UseMove &&
     * ...!=:Shift}; Move_Effects_Generic.rb:723).
     */
    public static boolean choiceIsMoveOrShift(Battle battle, int idxBattler) {
        throw new UnsupportedOperationException("M0 待接线: @battle.choices[i][0] is :UseMove/:Shift (Move_Usage.rb:110)");
    }

    /** {@code @battle.choices[idxBattler][2]} (Move_Effects_Generic.rb:724). */
    public static BattleMove choiceMove(Battle battle, int idxBattler) {
        throw new UnsupportedOperationException("M0 待接线: @battle.choices[i][2] (Move_Effects_Generic.rb:724)");
    }

    /** {@code Battler#movedThisRound?} (PokeBattle_Battler:704). */
    public static boolean movedThisRound(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:704 movedThisRound?");
    }

    /** {@code Battler#eachAlly} (PokeBattle_Battler:823, Move_Usage.rb:146). */
    public static Array<Battler> eachAlly(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:823 eachAlly");
    }

    // ==================================================================
    // Battler methods used by pbGetAttackStats / pbGetDefenseStats
    // ==================================================================

    /** {@code Battler#attack} (Move_Usage_Calculations.rb:242). */
    public static int attack(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: Move_Usage_Calculations.rb:242 user.attack");
    }

    /** {@code Battler#defense} (Move_Usage_Calculations.rb:249). */
    public static int defense(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: Move_Usage_Calculations.rb:249 target.defense");
    }

    /** {@code Battler#spatk} (Move_Usage_Calculations.rb:240). */
    public static int spatk(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: Move_Usage_Calculations.rb:240 user.spatk");
    }

    /** {@code Battler#spdef} (Move_Usage_Calculations.rb:247). */
    public static int spdef(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: Move_Usage_Calculations.rb:247 target.spdef");
    }

    // ==================================================================
    // Battler methods used by MoveEffectsGeneric's bodies
    // ==================================================================

    /** {@code Battler#pbSleep(msg=nil)} (Battler_Statuses:354). */
    public static void pbSleep(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: Battler_Statuses:354 pbSleep");
    }

    /** {@code Battler#pbCanFreeze?(user,showMessages,move)} (Battler_Statuses:432). */
    public static boolean pbCanFreeze(Battler battler, Battler user, boolean showMessages, BattleMove move) {
        throw new UnsupportedOperationException("M0 待接线: Battler_Statuses:432 pbCanFreeze?");
    }

    /** {@code Battler#pbFreeze(msg=nil)} (Battler_Statuses:436). */
    public static void pbFreeze(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: Battler_Statuses:436 pbFreeze");
    }

    /** {@code Battler#pbFlinch(_user=nil)} (Battler_Statuses:619). */
    public static void pbFlinch(Battler battler, Battler user) {
        throw new UnsupportedOperationException("M0 待接线: Battler_Statuses:619 pbFlinch");
    }

    // ==================================================================
    // Restored after the first retirement pass: the in-flight function-code
    // batch (MoveEffects_080_0AF / _100_17F / _180_1FF, which live in this same
    // package) still calls these by the stub's old shape, so they cannot be
    // deleted until those files are retired too. Each one's real counterpart is
    // named in the javadoc for the next pass.
    // ==================================================================

    /** {@code PBTargets} name &rarr; constant; real: {@code PBTargets.fromName(String)}. */
    public static int PBTargets_fromName(String pbsName) {
        throw new UnsupportedOperationException("M0 待接线: PBTargets target name -> constant (PokeBattle_Move.rb:40)");
    }

    /** {@code Battle#field.effects}; real: {@code battle.field.effects}. */
    public static EffectMap fieldEffects(Battle battle) {
        throw new UnsupportedOperationException("M0 待接线: @battle.field.effects (Move_Usage.rb:16-17)");
    }

    /** {@code Battle#moldBreaker}; real: {@code battle.moldBreaker}. */
    public static boolean moldBreaker(Battle battle) {
        throw new UnsupportedOperationException("M0 待接线: @battle.moldBreaker (Move_Usage.rb:89)");
    }

    /** {@code Battle#eachBattler}; real: {@code battle.eachBattler()}. */
    public static Array<Battler> eachBattler(Battle battle) {
        throw new UnsupportedOperationException("M0 待接线: @battle.eachBattler (Move_Usage.rb:108)");
    }

    /** {@code Battler#pbReduceHP(amt,anim)}; real: {@code battler.pbReduceHP(amt,anim,true,true)}. */
    public static void pbReduceHP(Battler battler, int amount, boolean anim) {
        throw new UnsupportedOperationException("M0 待接线: Battler_ChangeSelf:5 pbReduceHP");
    }

    /** {@code Battler#pbItemHPHealCheck}; real: {@code battler.pbItemHPHealCheck(0,false)}. */
    public static void pbItemHPHealCheck(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: Battler_AbilityAndItem:253 pbItemHPHealCheck");
    }

    /** {@code Battler#pbCanSleep?(user,showMessages,move)}; real: {@code battler.pbCanSleep(user,showMessages,move,false)}. */
    public static boolean pbCanSleep(Battler battler, Battler user, boolean showMessages, BattleMove move) {
        throw new UnsupportedOperationException("M0 待接线: Battler_Statuses:318 pbCanSleep?");
    }

    /** {@code Battler#pbCanParalyze?(user,showMessages,move)}; real: {@code battler.pbCanParalyze(user,showMessages,move)}. */
    public static boolean pbCanParalyze(Battler battler, Battler user, boolean showMessages, BattleMove move) {
        throw new UnsupportedOperationException("M0 待接线: Battler_Statuses:413 pbCanParalyze?");
    }

    /** {@code Battler#pbParalyze(user=nil,msg=nil)}; real: {@code battler.pbParalyze(user,null)}. */
    public static void pbParalyze(Battler battler, Battler user) {
        throw new UnsupportedOperationException("M0 待接线: Battler_Statuses:421 pbParalyze");
    }

    /** {@code Battler#pbCanBurn?(user,showMessages,move)}; real: {@code battler.pbCanBurn(user,showMessages,move)}. */
    public static boolean pbCanBurn(Battler battler, Battler user, boolean showMessages, BattleMove move) {
        throw new UnsupportedOperationException("M0 待接线: Battler_Statuses:394 pbCanBurn?");
    }

    /** {@code Battler#pbBurn(user=nil,msg=nil)}; real: {@code battler.pbBurn(user,null)}. */
    public static void pbBurn(Battler battler, Battler user) {
        throw new UnsupportedOperationException("M0 待接线: Battler_Statuses:402 pbBurn");
    }

    /** {@code Battler#pbCanLowerStatStage?(stat,user,move)}; real: the same 3-arg method. */
    public static boolean pbCanLowerStatStage(Battler battler, int stat, Battler user, BattleMove move) {
        throw new UnsupportedOperationException("M0 待接线: Battler_StatStages:130 pbCanLowerStatStage?");
    }

    /** {@code Battler#pbCanLowerStatStage?(stat,user,move,showFailMsg)}; real: the same 4-arg method. */
    public static boolean pbCanLowerStatStage(Battler battler, int stat, Battler user, BattleMove move,
                                              boolean showFailMsg) {
        throw new UnsupportedOperationException("M0 待接线: Battler_StatStages:130 pbCanLowerStatStage?(showFailMsg)");
    }

    /** {@code Battler#pbLowerStatStage(stat,increment,user,showAnim)}; real: the same 4-arg method. */
    public static boolean pbLowerStatStage(Battler battler, int stat, int increment, Battler user, boolean showAnim) {
        throw new UnsupportedOperationException("M0 待接线: Battler_StatStages:191 pbLowerStatStage");
    }
}
