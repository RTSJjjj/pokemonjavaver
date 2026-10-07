package pokemon.runtime.battle.movefx;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.battle.Battle;
import pokemon.runtime.battle.BattleMove;
import pokemon.runtime.battle.BattleSide;
import pokemon.runtime.battle.Battler;
import pokemon.runtime.battle.DamageState;
import pokemon.runtime.battle.EffectMap;

/**
 * Stage 4 / L2: the temporary stub layer for every {@code Battler} / {@code
 * Battle} / {@code PBTypes} / {@code PBTargets} method that {@link
 * MoveEffectBase} and {@link MoveEffectsGeneric} call but that this runtime has
 * not landed yet.
 *
 * <h2>Why a second stub class next to {@code PendingApi}</h2>
 * Task-11 (Lead decision): the move-effect block keeps its own choke point so
 * that the {@code movefx} package owns its write scope instead of editing
 * {@code PendingApi.java} (infra2's file). Methods that {@code PendingApi}
 * <b>already</b> declares ({@code pbDisplay}, {@code effects}, {@code battle},
 * {@code pbThis}, {@code ability}, {@code abilityActive}, {@code abilityName},
 * {@code hasActiveAbility(Battler,String)}, {@code pbShowAbilitySplash},
 * {@code pbHideAbilitySplash}, {@code pbCommonAnimation(Battle,String,Battler)},
 * {@code pbConfuse}, {@code pbRecoverHP}, {@code pbRaiseStatStage(Battler,int,int,Battler)},
 * {@code pbCanRaiseStatStage(Battler,int,Battler)}, {@code fieldWeather}) are
 * called directly on {@code PendingApi}; everything else is here.
 *
 * <h2>Temporary by design</h2>
 * Every method is a <b>throwing stub</b> - deliberately not a default value,
 * because returning one would silently invent plugin behaviour. When the real
 * methods land, each call site becomes a direct call and both stub classes are
 * deleted.
 *
 * <h2>Naming</h2>
 * A plugin <i>method</i> keeps its name ({@code pbPopup}, {@code pbStartWeather},
 * ...); a plugin <i>module function</i> cannot keep the dot in Java, so
 * {@code PBTypes.isSpecialType?} is written {@code PBTypes_isSpecialType}. Each
 * javadoc names the Ruby source line.
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

    /** {@code PBTypes.isPseudoType?(type)} (PBTypes_Extra.rb:34-36). */
    public static boolean PBTypes_isPseudoType(String type) {
        throw new UnsupportedOperationException("M0 待接线: PBTypes_Extra.rb:34 isPseudoType?");
    }

    /** {@code PBTypes.getEffectiveness(attackType,targetType)} (PBTypes_Extra.rb:42-45). */
    public static int PBTypes_getEffectiveness(String attackType, String targetType) {
        throw new UnsupportedOperationException("M0 待接线: PBTypes_Extra.rb:42 getEffectiveness");
    }

    /** {@code PBTypes.getCombinedEffectiveness(attackType,t1,t2=nil,t3=nil)} (PBTypes_Extra.rb:47-59). */
    public static int PBTypes_getCombinedEffectiveness(String attackType, String type1, String type2, String type3) {
        throw new UnsupportedOperationException("M0 待接线: PBTypes_Extra.rb:47 getCombinedEffectiveness");
    }

    /**
     * {@code PBTargets} name &rarr; constant. Ruby stores the target as the
     * integer {@code moveData[MOVE_TARGET]} (PokeBattle_Move.rb:40); this runtime
     * keeps the PBS name in {@code PbsData.Move.target}, so it must be mapped
     * (task-11 decision 3).
     */
    public static int PBTargets_fromName(String pbsName) {
        throw new UnsupportedOperationException("M0 待接线: PBTargets target name -> constant (PokeBattle_Move.rb:40)");
    }

    // ==================================================================
    // Battle methods used by MoveEffectBase's default bodies
    // ==================================================================

    /** {@code Battle#pbDisplayBrief(msg)} (Move_Usage.rb:24). */
    public static void pbDisplayBrief(Battle battle, String message) {
        throw new UnsupportedOperationException("M0 待接线: Move_Usage.rb:24 @battle.pbDisplayBrief");
    }

    /** {@code Battle#pbAnimation(id,user,targets,hitNum)} (Move_Usage.rb:72). */
    public static void pbAnimation(Battle battle, int id, Battler user, Array<Battler> targets, int hitNum) {
        battle.animation(id, user, targets, hitNum);            // PokeBattle_Battle:793-795
    }

    /** {@code Battle#pbCommonAnimation(name,user,targets)} (Move_Usage.rb:70). */
    public static void pbCommonAnimation(Battle battle, String name, Battler user, Array<Battler> targets) {
        battle.commonAnimation(name, user, targets);            // PokeBattle_Battle:797-799
    }

    /** {@code Battle#field.effects} (Move_Usage.rb:16-17). */
    public static EffectMap fieldEffects(Battle battle) {
        throw new UnsupportedOperationException("M0 待接线: @battle.field.effects (Move_Usage.rb:16-17)");
    }

    /** {@code Battle#moldBreaker} (Move_Usage.rb:89, :132). */
    public static boolean moldBreaker(Battle battle) {
        throw new UnsupportedOperationException("M0 待接线: @battle.moldBreaker (Move_Usage.rb:89)");
    }

    /** {@code getConst(PBMoves,:BAMBOOSWORD)} (Move_Usage.rb:20). */
    public static int PBMoves_id(String internalName) {
        throw new UnsupportedOperationException("M0 待接线: getConst(PBMoves,:BAMBOOSWORD) (Move_Usage.rb:20)");
    }

    /** {@code Battle#eachBattler} (Move_Usage.rb:108). */
    public static Array<Battler> eachBattler(Battle battle) {
        throw new UnsupportedOperationException("M0 待接线: @battle.eachBattler (Move_Usage.rb:108)");
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

    /** {@code Battle#pbStartWeather(user,newWeather,fixedDuration,noAnimation)} (Move_Effects_Generic.rb:699). */
    public static void pbStartWeather(Battle battle, Battler user, int newWeather, boolean fixedDuration,
                                      boolean noAnimation) {
        throw new UnsupportedOperationException("M0 待接线: Move_Effects_Generic.rb:699 pbStartWeather");
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

    /**
     * {@code Battler#stages[stat]} indexed by the plugin's {@code PBStats}
     * numbering (Move_Usage_Calculations.rb:240/242/247/249). Task-11 decision 3:
     * a stub, because {@code Battler.stages} is currently a 5-element array whose
     * offsets differ from {@code PBStats} by one and reading it directly would be
     * silently wrong; the wiring step adds {@code Battler.stage(int pbStat)}.
     */
    public static int statStage(Battler battler, int stat) {
        throw new UnsupportedOperationException("M0 待接线: Move_Usage_Calculations.rb:240 user.stages[PBStats::...]");
    }

    // ==================================================================
    // Battler methods used by MoveEffectsGeneric's bodies
    // ==================================================================

    /** {@code Battler#damageState} (PokeBattle_Battler:42 {@code attr_accessor :damageState}). */
    public static DamageState damageState(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:42 damageState");
    }

    /** {@code Battler#pbReduceHP(amt,anim)} (Battler_ChangeSelf:5). */
    public static void pbReduceHP(Battler battler, int amount, boolean anim) {
        throw new UnsupportedOperationException("M0 待接线: Battler_ChangeSelf:5 pbReduceHP");
    }

    /** {@code Battler#pbItemHPHealCheck} (Battler_AbilityAndItem:253). */
    public static void pbItemHPHealCheck(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: Battler_AbilityAndItem:253 pbItemHPHealCheck");
    }

    /** {@code Battler#takesIndirectDamage?} (PokeBattle_Battler:615). */
    public static boolean takesIndirectDamage(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:615 takesIndirectDamage?");
    }

    /** {@code Battler#pbOwnSide} (PokeBattle_Battler:813). */
    public static BattleSide pbOwnSide(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:813 pbOwnSide");
    }

    /** {@code Battler#pbOpposingSide} (PokeBattle_Battler:818). */
    public static BattleSide pbOpposingSide(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:818 pbOpposingSide");
    }

    /** {@code Battler#pbTeam(lowerCase)} (PokeBattle_Battler:233). */
    public static String pbTeam(Battler battler, boolean lowerCase) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:233 pbTeam");
    }

    /** {@code Battler#pbOpposingTeam(lowerCase)} (Move_Effects_Generic.rb:768). */
    public static String pbOpposingTeam(Battler battler, boolean lowerCase) {
        throw new UnsupportedOperationException("M0 待接线: Move_Effects_Generic.rb:768 pbOpposingTeam");
    }

    /** {@code Battler#opposes?(i=0)} (PokeBattle_Battler:784). */
    public static boolean opposes(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:784 opposes?");
    }

    /** {@code Battler#pbCanSleep?(user,showMessages,move)} (Battler_Statuses:318). */
    public static boolean pbCanSleep(Battler battler, Battler user, boolean showMessages, BattleMove move) {
        throw new UnsupportedOperationException("M0 待接线: Battler_Statuses:318 pbCanSleep?");
    }

    /** {@code Battler#pbSleep(msg=nil)} (Battler_Statuses:354). */
    public static void pbSleep(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: Battler_Statuses:354 pbSleep");
    }

    /** {@code Battler#pbCanPoison?(user,showMessages,move)} (Battler_Statuses:375). */
    public static boolean pbCanPoison(Battler battler, Battler user, boolean showMessages, BattleMove move) {
        throw new UnsupportedOperationException("M0 待接线: Battler_Statuses:375 pbCanPoison?");
    }

    /** {@code Battler#pbPoison(user=nil,msg=nil,toxic=false)} (Battler_Statuses:383). */
    public static void pbPoison(Battler battler, Battler user, String msg, boolean toxic) {
        throw new UnsupportedOperationException("M0 待接线: Battler_Statuses:383 pbPoison");
    }

    /** {@code Battler#pbCanParalyze?(user,showMessages,move)} (Battler_Statuses:413). */
    public static boolean pbCanParalyze(Battler battler, Battler user, boolean showMessages, BattleMove move) {
        throw new UnsupportedOperationException("M0 待接线: Battler_Statuses:413 pbCanParalyze?");
    }

    /** {@code Battler#pbParalyze(user=nil,msg=nil)} (Battler_Statuses:421). */
    public static void pbParalyze(Battler battler, Battler user) {
        throw new UnsupportedOperationException("M0 待接线: Battler_Statuses:421 pbParalyze");
    }

    /** {@code Battler#pbCanBurn?(user,showMessages,move)} (Battler_Statuses:394). */
    public static boolean pbCanBurn(Battler battler, Battler user, boolean showMessages, BattleMove move) {
        throw new UnsupportedOperationException("M0 待接线: Battler_Statuses:394 pbCanBurn?");
    }

    /** {@code Battler#pbBurn(user=nil,msg=nil)} (Battler_Statuses:402). */
    public static void pbBurn(Battler battler, Battler user) {
        throw new UnsupportedOperationException("M0 待接线: Battler_Statuses:402 pbBurn");
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

    /** {@code Battler#pbCanConfuse?(user,showMessages,move)} (Battler_Statuses:488). */
    public static boolean pbCanConfuse(Battler battler, Battler user, boolean showMessages, BattleMove move) {
        throw new UnsupportedOperationException("M0 待接线: Battler_Statuses:488 pbCanConfuse?");
    }

    /** {@code Battler#pbCanRaiseStatStage?(stat,user,move)} (Battler_StatStages:9). */
    public static boolean pbCanRaiseStatStage(Battler battler, int stat, Battler user, BattleMove move) {
        throw new UnsupportedOperationException("M0 待接线: Battler_StatStages:9 pbCanRaiseStatStage?");
    }

    /** {@code Battler#pbCanRaiseStatStage?(stat,user,move,showFailMsg)} (Battler_StatStages:9). */
    public static boolean pbCanRaiseStatStage(Battler battler, int stat, Battler user, BattleMove move,
                                              boolean showFailMsg) {
        throw new UnsupportedOperationException("M0 待接线: Battler_StatStages:9 pbCanRaiseStatStage?(showFailMsg)");
    }

    /** {@code Battler#pbRaiseStatStage(stat,increment,user,showAnim)} (Battler_StatStages:47). */
    public static boolean pbRaiseStatStage(Battler battler, int stat, int increment, Battler user, boolean showAnim) {
        throw new UnsupportedOperationException("M0 待接线: Battler_StatStages:47 pbRaiseStatStage");
    }

    /** {@code Battler#pbCanLowerStatStage?(stat,user,move)} (Battler_StatStages:130). */
    public static boolean pbCanLowerStatStage(Battler battler, int stat, Battler user, BattleMove move) {
        throw new UnsupportedOperationException("M0 待接线: Battler_StatStages:130 pbCanLowerStatStage?");
    }

    /** {@code Battler#pbCanLowerStatStage?(stat,user,move,showFailMsg)} (Battler_StatStages:130). */
    public static boolean pbCanLowerStatStage(Battler battler, int stat, Battler user, BattleMove move,
                                              boolean showFailMsg) {
        throw new UnsupportedOperationException("M0 待接线: Battler_StatStages:130 pbCanLowerStatStage?(showFailMsg)");
    }

    /** {@code Battler#pbLowerStatStage(stat,increment,user,showAnim)} (Battler_StatStages:191). */
    public static boolean pbLowerStatStage(Battler battler, int stat, int increment, Battler user, boolean showAnim) {
        throw new UnsupportedOperationException("M0 待接线: Battler_StatStages:191 pbLowerStatStage");
    }

    /** {@code Battler#statStageAtMin?(stat)} (Battler_StatStages:126). */
    public static boolean statStageAtMin(Battler battler, int stat) {
        throw new UnsupportedOperationException("M0 待接线: Battler_StatStages:126 statStageAtMin?");
    }

    /** {@code Battler#statStageAtMax?(stat)} (Battler_StatStages:5). */
    public static boolean statStageAtMax(Battler battler, int stat) {
        throw new UnsupportedOperationException("M0 待接线: Battler_StatStages:5 statStageAtMax?");
    }

    /** {@code Battle#pbAllFainted?(idxBattler)} (PokeBattle_Battle:353). */
    public static boolean pbAllFainted(Battle battle, int idxBattler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battle:353 pbAllFainted?");
    }

    /** {@code Battler#idxOwnSide} (PokeBattle_Battler:802). */
    public static int idxOwnSide(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:802 idxOwnSide");
    }

    /** {@code Battler#pbConsumeItem} (Move_Effects_Generic.rb:530). */
    public static void pbConsumeItem(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: Move_Effects_Generic.rb:530 pbConsumeItem");
    }

    /** {@code Battler#hasActiveItem?(item,ignoreFainted=false)} (Move_Effects_Generic.rb:501). */
    public static boolean hasActiveItem(Battler battler, String item) {
        throw new UnsupportedOperationException("M0 待接线: Move_Effects_Generic.rb:501 hasActiveItem?");
    }

    /** {@code Battler#hasActiveItem?([items],ignoreFainted)} (Move_Usage_Calculations.rb:570). */
    public static boolean hasActiveItemAny(Battler battler, String[] items, boolean ignoreFainted) {
        throw new UnsupportedOperationException("M0 待接线: Move_Usage_Calculations.rb:570 hasActiveItem?([items])");
    }

    /** {@code Battler#hasActiveAbility?(ability,ignoreFainted)} (PokeBattle_Battler:387). */
    public static boolean hasActiveAbility(Battler battler, String ability, boolean ignoreFainted) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:387 hasActiveAbility?(ignoreFainted)");
    }

    /** {@code Battle#pbRandom(max)} (Move_Effects_Generic.rb:639). */
    public static int pbRandom(Battle battle, int max) {
        throw new UnsupportedOperationException("M0 待接线: Move_Effects_Generic.rb:639 @battle.pbRandom");
    }

    /** {@code Battler#lastMoveFailed = value} (PokeBattle_Battler:36) (Move_Effects_Generic.rb:758). */
    public static void setLastMoveFailed(Battler battler, boolean value) {
        throw new UnsupportedOperationException("M0 待接线: Move_Effects_Generic.rb:758 lastMoveFailed=");
    }
}
