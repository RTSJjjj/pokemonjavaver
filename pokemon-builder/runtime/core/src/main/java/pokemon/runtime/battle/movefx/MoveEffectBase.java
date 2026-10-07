package pokemon.runtime.battle.movefx;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import pokemon.runtime.battle.Battle;
import pokemon.runtime.battle.BattleHandlers;
import pokemon.runtime.battle.BattleMove;
import pokemon.runtime.battle.Battler;
import pokemon.runtime.battle.MoveUsage;
import pokemon.runtime.battle.EffectMap;
import pokemon.runtime.battle.PBEffects;
import pokemon.runtime.battle.PBStats;
import pokemon.runtime.battle.PBTargets;
import pokemon.runtime.battle.PendingApi;
import pokemon.runtime.battle.PokeBattle_SceneConstants;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.battle.PBTypeEffectiveness;
import pokemon.runtime.battle.PBTypes;
import pokemon.runtime.battle.PBWeather;
import pokemon.runtime.pokemon.PbsData;

/**
 * Stage 4 / L2: the default bodies of every {@link MoveEffect} hook, transcribed
 * from the plugin's base class - {@code PokeBattle_Move}
 * (PokeBattle_Move.rb:64-142), {@code Move_Usage} (Move_Usage.rb:5-161) and
 * {@code Move_Usage_Calculations} (Move_Usage_Calculations.rb).
 *
 * <p>Every subclass of this class (the 24 reusable bases in
 * {@link MoveEffectsGeneric} and, in the next batch, the 496 function-code
 * classes) overrides only the hooks the plugin's class overrides.</p>
 *
 * <h2>Methods this batch does NOT transcribe</h2>
 * <p>Task-11 decision 10: the default bodies whose dependency chain needs
 * {@code Battler}/{@code Battle}/{@code PBTypes} methods that do not exist yet
 * throw {@code UnsupportedOperationException("M0 待接线: <Ruby line range>")} -
 * an honest stub, never a made-up value. They are:
 * {@code pbCalcType} (:14-29), {@code pbCalcTypeModSingle} (:34-71),
 * {@code pbCalcTypeMod} (:73-103), {@code pbAccuracyCheck} (:112-143),
 * {@code pbCalcAccuracyModifiers} (:145-183), {@code pbIsCritical?} (:196-229),
 * {@code pbCalcDamage} (:252-295) and {@code pbCalcDamageMultipliers}
 * (:296-546). The required {@code PendingApi} additions are listed in the
 * task-11 report.</p>
 *
 * <p>Task-11 decision 8: {@code pbFixedDamage}, {@code pbHealAmount} and
 * {@code pbRecoilDamage} have <b>no</b> default in the plugin's base class - the
 * only {@code return 1} bodies live in the abstract bases
 * {@code PokeBattle_FixedDamageMove} (Move_Effects_Generic.rb:474),
 * {@code PokeBattle_HealingMove} (:570) and {@code PokeBattle_RecoilMove}
 * (:594) - so they throw here and are implemented by those subclasses.</p>
 */
public class MoveEffectBase implements MoveEffect {

    /** {@code MOVE_CATEGORY_PER_MOVE = true} (Settings:159). */
    protected static final boolean MOVE_CATEGORY_PER_MOVE = true;

    // ==================================================================
    // A. About the move (PokeBattle_Move.rb:64-142)
    // ==================================================================

    /** {@code def pbTarget(_user); return @target; end} (PokeBattle_Move.rb:64). */
    @Override
    public int pbTarget(BattleMove move, Battler user) {
        // :64 @target is the MOVE_TARGET integer; this runtime stores the PBS
        //     name in PbsData.Move.target, so it is mapped through PBTargets
        //     (task-11 decision 3).
        return PBTargets.fromName(move.target());
    }

    /** {@code totalpp} (PokeBattle_Move.rb:66-70). */
    @Override
    public int totalpp(BattleMove move) {
        int override = move.totalpp();
        if (override > 0) {
            return override;                     // :67 return @totalpp if @totalpp && @totalpp>0
        }
        // :68-69 @realMove.totalpp / 0 - the move slot's max PP; the caller
        //        resolves that branch (BattleMove.totalpp()'s javadoc).
        return 0;
    }

    /** {@code physicalMove?(thisType=nil)} (PokeBattle_Move.rb:74-79). */
    @Override
    public boolean physicalMove(BattleMove move, String thisType) {
        if (MOVE_CATEGORY_PER_MOVE) {
            return "Physical".equals(move.category());   // :75 return (@category==0)
        }
        String type = thisType;
        if (type == null && move.calcType() != null) {
            type = move.calcType();                      // :76 thisType ||= @calcType if @calcType>=0
        }
        if (type == null) {
            type = move.type();                          // :77 thisType = @type if !thisType
        }
        return !MoveFxPendingApi.PBTypes_isSpecialType(type);  // :78
    }

    /** {@code specialMove?(thisType=nil)} (PokeBattle_Move.rb:83-88). */
    @Override
    public boolean specialMove(BattleMove move, String thisType) {
        if (MOVE_CATEGORY_PER_MOVE) {
            return "Special".equals(move.category());    // :85 return (@category==1)
        }
        String type = thisType;
        if (type == null && move.calcType() != null) {
            type = move.calcType();                      // :86
        }
        if (type == null) {
            type = move.type();                          // :87
        }
        return MoveFxPendingApi.PBTypes_isSpecialType(type);   // :88
    }

    /** {@code damagingMove?} (PokeBattle_Move.rb:90). */
    @Override
    public boolean damagingMove(BattleMove move) {
        return !"Status".equals(move.category());        // :90 @category!=2
    }

    /** {@code statusMove?} (PokeBattle_Move.rb:91). */
    @Override
    public boolean statusMove(BattleMove move) {
        return "Status".equals(move.category());         // :91 @category==2
    }

    /** {@code usableWhenAsleep?} (PokeBattle_Move.rb:93). */
    @Override
    public boolean usableWhenAsleep(BattleMove move) {
        return false;                                    // :93
    }

    /** {@code unusableInGravity?} (PokeBattle_Move.rb:94). */
    @Override
    public boolean unusableInGravity(BattleMove move) {
        return false;                                    // :94
    }

    /** {@code healingMove?} (PokeBattle_Move.rb:95). */
    @Override
    public boolean healingMove(BattleMove move) {
        return false;                                    // :95
    }

    /** {@code recoilMove?} (PokeBattle_Move.rb:96). */
    @Override
    public boolean recoilMove(BattleMove move) {
        return false;                                    // :96
    }

    /** {@code flinchingMove?} (PokeBattle_Move.rb:97). */
    @Override
    public boolean flinchingMove(BattleMove move) {
        return false;                                    // :97
    }

    /** {@code callsAnotherMove?} (PokeBattle_Move.rb:98). */
    @Override
    public boolean callsAnotherMove(BattleMove move) {
        return false;                                    // :98
    }

    /** {@code multiHitMove?} (PokeBattle_Move.rb:101). */
    @Override
    public boolean multiHitMove(BattleMove move) {
        return false;                                    // :101
    }

    /** {@code chargingTurnMove?} (PokeBattle_Move.rb:102). */
    @Override
    public boolean chargingTurnMove(BattleMove move) {
        return false;                                    // :102
    }

    /** {@code successCheckPerHit?} (PokeBattle_Move.rb:103). */
    @Override
    public boolean successCheckPerHit(BattleMove move) {
        return false;                                    // :103
    }

    /** {@code hitsFlyingTargets?} (PokeBattle_Move.rb:104). */
    @Override
    public boolean hitsFlyingTargets(BattleMove move) {
        return false;                                    // :104
    }

    /** {@code hitsDiggingTargets?} (PokeBattle_Move.rb:105). */
    @Override
    public boolean hitsDiggingTargets(BattleMove move) {
        return false;                                    // :105
    }

    /** {@code hitsDivingTargets?} (PokeBattle_Move.rb:106). */
    @Override
    public boolean hitsDivingTargets(BattleMove move) {
        return false;                                    // :106
    }

    /** {@code ignoresReflect?} (PokeBattle_Move.rb:107) - for Brick Break. */
    @Override
    public boolean ignoresReflect(BattleMove move) {
        return false;                                    // :107
    }

    /** {@code cannotRedirect?} (PokeBattle_Move.rb:108) - for Future Sight/Doom Desire. */
    @Override
    public boolean cannotRedirect(BattleMove move) {
        return false;                                    // :108
    }

    /** {@code worksWithNoTargets?} (PokeBattle_Move.rb:109) - for Explosion. */
    @Override
    public boolean worksWithNoTargets(BattleMove move) {
        return false;                                    // :109
    }

    /** {@code damageReducedByBurn?} (PokeBattle_Move.rb:110) - for Facade. */
    @Override
    public boolean damageReducedByBurn(BattleMove move) {
        return true;                                     // :110
    }

    /** {@code triggersHyperMode?} (PokeBattle_Move.rb:111). */
    @Override
    public boolean triggersHyperMode(BattleMove move) {
        return false;                                    // :111
    }

    /** {@code contactMove?} (PokeBattle_Move.rb:113) - {@code @flags[/a/]}. */
    @Override
    public boolean contactMove(BattleMove move) {
        return hasFlag(move, 'a');                       // :113
    }

    /** {@code canProtectAgainst?} (PokeBattle_Move.rb:114) - {@code @flags[/b/]}. */
    @Override
    public boolean canProtectAgainst(BattleMove move) {
        return hasFlag(move, 'b');                       // :114
    }

    /** {@code canMagicCoat?} (PokeBattle_Move.rb:115) - {@code @flags[/c/]}. */
    @Override
    public boolean canMagicCoat(BattleMove move) {
        return hasFlag(move, 'c');                       // :115
    }

    /** {@code canSnatch?} (PokeBattle_Move.rb:116) - {@code @flags[/d/]}. */
    @Override
    public boolean canSnatch(BattleMove move) {
        return hasFlag(move, 'd');                       // :116
    }

    /** {@code canMirrorMove?} (PokeBattle_Move.rb:117) - {@code @flags[/e/]}. */
    @Override
    public boolean canMirrorMove(BattleMove move) {
        return hasFlag(move, 'e');                       // :117
    }

    /** {@code canKingsRock?} (PokeBattle_Move.rb:118) - {@code @flags[/f/]}. */
    @Override
    public boolean canKingsRock(BattleMove move) {
        return hasFlag(move, 'f');                       // :118
    }

    /** {@code thawsUser?} (PokeBattle_Move.rb:119) - {@code @flags[/g/]}. */
    @Override
    public boolean thawsUser(BattleMove move) {
        return hasFlag(move, 'g');                       // :119
    }

    /** {@code highCriticalRate?} (PokeBattle_Move.rb:120) - {@code @flags[/h/]}. */
    @Override
    public boolean highCriticalRate(BattleMove move) {
        return hasFlag(move, 'h');                       // :120
    }

    /** {@code bitingMove?} (PokeBattle_Move.rb:121) - {@code @flags[/i/]}. */
    @Override
    public boolean bitingMove(BattleMove move) {
        return hasFlag(move, 'i');                       // :121
    }

    /** {@code punchingMove?} (PokeBattle_Move.rb:122) - {@code @flags[/j/]}. */
    @Override
    public boolean punchingMove(BattleMove move) {
        return hasFlag(move, 'j');                       // :122
    }

    /** {@code soundMove?} (PokeBattle_Move.rb:123) - {@code @flags[/k/]}. */
    @Override
    public boolean soundMove(BattleMove move) {
        return hasFlag(move, 'k');                       // :123
    }

    /** {@code powderMove?} (PokeBattle_Move.rb:124) - {@code @flags[/l/]}. */
    @Override
    public boolean powderMove(BattleMove move) {
        return hasFlag(move, 'l');                       // :124
    }

    /** {@code pulseMove?} (PokeBattle_Move.rb:125) - {@code @flags[/m/]}. */
    @Override
    public boolean pulseMove(BattleMove move) {
        return hasFlag(move, 'm');                       // :125
    }

    /** {@code bombMove?} (PokeBattle_Move.rb:126) - {@code @flags[/n/]}. */
    @Override
    public boolean bombMove(BattleMove move) {
        return hasFlag(move, 'n');                       // :126
    }

    /** {@code danceMove?} (PokeBattle_Move.rb:127) - {@code @flags[/o/]}. */
    @Override
    public boolean danceMove(BattleMove move) {
        return hasFlag(move, 'o');                       // :127
    }

    /** {@code slicingMove?} (PokeBattle_Move.rb:128) - {@code @flags[/p/]}. */
    @Override
    public boolean slicingMove(BattleMove move) {
        return hasFlag(move, 'p');                       // :128
    }

    /** {@code windMove?} (PokeBattle_Move.rb:129) - {@code @flags[/r/]}. */
    @Override
    public boolean windMove(BattleMove move) {
        return hasFlag(move, 'r');                       // :129
    }

    /**
     * Ruby {@code @flags[/x/]}: a truthy match. {@code PbsData.Move.flags} is the
     * verbatim PBS string (PokeBattle_Move.rb:114-129 read it by letter).
     */
    private static boolean hasFlag(BattleMove move, char letter) {
        String flags = move.flags();
        return flags != null && flags.indexOf(letter) >= 0;
    }

    /**
     * The {@code @battle.choices[idx][0]} test shared by three call sites: true
     * when the command is {@code :UseMove} or {@code :Shift}
     * (Move_Usage.rb:110, :124 {@code @battle.choices[..][0]!=:UseMove &&
     * ...!=:Shift}; Move_Effects_Generic.rb:723).
     *
     * <p>{@code Battle#choices(idxBattler)} (Battle.java) assembles the plugin's
     * {@code [action,arg1,arg2,arg3]} view on demand and documents that slot 0
     * "reads exactly like the plugin's {@code @choices[idxBattler][0]}"; the
     * runtime's derived view emits {@code ":UseMove"}/{@code ":SwitchOut"}/
     * {@code ":None"} and never {@code ":Shift"} (the runtime has no Shift
     * command), so the second half of the Ruby test is simply never true here -
     * it is kept because the plugin's test has it.</p>
     */
    protected static boolean choiceIsMoveOrShift(Battle battle, int idxBattler) {
        String action = (String) battle.choices(idxBattler)[0];
        return ":UseMove".equals(action) || ":Shift".equals(action);
    }

    /** {@code tramplesMinimize?(_param=1)} (PokeBattle_Move.rb:132). */
    @Override
    public boolean tramplesMinimize(BattleMove move, int param) {
        return false;                                    // :132
    }

    /** {@code nonLethal?(_user,_target)} (PokeBattle_Move.rb:133) - for False Swipe. */
    @Override
    public boolean nonLethal(BattleMove move, Battler user, Battler target) {
        return false;                                    // :133
    }

    /**
     * {@code ignoresSubstitute?(user)} (PokeBattle_Move.rb:135-142).
     *
     * <p>{@code NEWEST_BATTLE_MECHANICS} is {@code true} (Settings:160), read
     * from {@link Battle#NEWEST_BATTLE_MECHANICS}.</p>
     */
    @Override
    public boolean ignoresSubstitute(BattleMove move, Battler user) {
        if (Battle.NEWEST_BATTLE_MECHANICS) {
            if (soundMove(move)) {
                return true;                             // :137
            }
            if (user != null && user.hasActiveAbility("INFILTRATOR")) {
                return true;                             // :138
            }
            if (user != null && user.hasActiveAbility("TRANSLUCENTGHOST")) {
                return true;                             // :139
            }
        }
        return false;                                    // :141
    }

    /** {@code pbDamagingMove?} (Move_Usage.rb:38). */
    @Override
    public boolean pbDamagingMove(BattleMove move) {
        return damagingMove(move);                       // :38
    }

    /** {@code pbContactMove?(user)} (Move_Usage.rb:40-46). */
    @Override
    public boolean pbContactMove(BattleMove move, Battler user) {
        if (user.hasActiveAbility("LONGREACH")) {
            return false;                                // :41
        }
        if (user.hasActiveAbility("NOBLESTRIKE")) {
            return false;                                // :42
        }
        if (user.hasActiveAbility("RUYIBLADE")) {
            return false;                                // :43
        }
        if (physicalMove(move, null) && "196".equals(move.function())) {
            return true;                                 // :44 Shell Side Arm
        }
        return contactMove(move);                        // :45
    }

    // ==================================================================
    // B. Effect methods per move usage (Move_Usage.rb:5-161)
    // ==================================================================

    /** {@code pbCanChooseMove?(user,commandPhase,showMessages)} (Move_Usage.rb:5) - for Belch. */
    @Override
    public boolean pbCanChooseMove(BattleMove move, Battler user, boolean commandPhase, boolean showMessages) {
        return true;                                     // :5
    }

    /** {@code pbDisplayChargeMessage(user)} (Move_Usage.rb:6). */
    @Override
    public void pbDisplayChargeMessage(BattleMove move, Battler user) {
        // :6 empty
    }

    /** {@code pbOnStartUse(user,targets)} (Move_Usage.rb:7). */
    @Override
    public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
        // :7 empty
    }

    /** {@code pbAddTarget(targets,user)} (Move_Usage.rb:8) - for Counter, Bide. */
    @Override
    public void pbAddTarget(BattleMove move, Array<Battler> targets, Battler user) {
        // :8 empty
    }

    /** {@code pbChangeUsageCounters(user,specialUsage)} (Move_Usage.rb:11-20). */
    @Override
    public void pbChangeUsageCounters(BattleMove move, Battler user, boolean specialUsage) {
        EffectMap effects = user.effects;
        effects.set(PBEffects.Battler.GlaiveRush, 0);                    // :12
        effects.set(PBEffects.Battler.FuryCutter, 0);                    // :13
        effects.set(PBEffects.Battler.ParentalBond, 0);                  // :14
        effects.set(PBEffects.Battler.ProtectRate, 1);                   // :15
        Battle battle = user.battle;
        EffectMap fieldEffects = battle.field.effects;
        fieldEffects.set(PBEffects.Field.FusionBolt, false);             // :16
        fieldEffects.set(PBEffects.Field.FusionFlare, false);            // :17
        // :18 user.effects[SuccessiveMove] = -1 if it != @id; an unset key is
        //     Ruby's nil, which is != any id, so the fallback must be a value no
        //     move id can take.
        if (effects.intVal(PBEffects.Battler.SuccessiveMove, Integer.MIN_VALUE) != move.id()) {
            effects.set(PBEffects.Battler.SuccessiveMove, -1);
        }
        if (MoveFxPendingApi.PBMoves_id("BAMBOOSWORD") != move.id()) {      // :19
            effects.set(PBEffects.Battler.BambooSword, 0);
        }
    }

    /** {@code pbDisplayUseMessage(user)} (Move_Usage.rb:23-25). */
    @Override
    public void pbDisplayUseMessage(BattleMove move, Battler user) {
        // :24 @battle.pbDisplayBrief(_INTL("{1}使用{2}！",user.pbThis,@name))
        user.battle.displayBrief(user.pbThis() + "使用" + move.name() + "！");
    }

    /** {@code pbMissMessage(user,target)} (Move_Usage.rb:27). */
    @Override
    public boolean pbMissMessage(BattleMove move, Battler user, Battler target) {
        return false;                                    // :27
    }

    /** {@code pbIsChargingTurn?(user)} (Move_Usage.rb:37). */
    @Override
    public boolean pbIsChargingTurn(BattleMove move, Battler user) {
        return false;                                    // :37
    }

    /** {@code pbNumHits(user,targets)} (Move_Usage.rb:50-58). */
    @Override
    public int pbNumHits(BattleMove move, Battler user, Array<Battler> targets) {
        if (user.hasActiveAbility("PARENTALBOND") && pbDamagingMove(move)
                && !chargingTurnMove(move) && targets.size == 1) {       // :51-52
            user.effects.set(PBEffects.Battler.ParentalBond, 3);  // :54
            return 2;                                                     // :55
        }
        return 1;                                                        // :57
    }

    /** {@code pbOverrideSuccessCheckPerHit(user,target)} (Move_Usage.rb:63). */
    @Override
    public boolean pbOverrideSuccessCheckPerHit(BattleMove move, Battler user, Battler target) {
        return false;                                    // :63
    }

    /** {@code pbCrashDamage(user)} (Move_Usage.rb:64). */
    @Override
    public void pbCrashDamage(BattleMove move, Battler user) {
        // :64 empty
    }

    /** {@code pbInitialEffect(user,targets,hitNum)} (Move_Usage.rb:65). */
    @Override
    public void pbInitialEffect(BattleMove move, Battler user, Array<Battler> targets, int hitNum) {
        // :65 empty
    }

    /** {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} (Move_Usage.rb:67-74). */
    @Override
    public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets, int hitNum,
                                boolean showAnimation) {
        if (!showAnimation) {
            return;                                      // :68 return if !showAnimation
        }
        if (user.effects.intVal(PBEffects.Battler.ParentalBond) == 1) {   // :69
            user.battle.commonAnimation("ParentalBond", user, targets);  // :70
        } else {
            user.battle.animation(id, user, targets, hitNum);            // :72
        }
    }

    /** {@code pbSelfKO(user)} (Move_Usage.rb:76). */
    @Override
    public void pbSelfKO(BattleMove move, Battler user) {
        // :76 empty
    }

    /** {@code pbEffectWhenDealingDamage(user,target)} (Move_Usage.rb:77). */
    @Override
    public void pbEffectWhenDealingDamage(BattleMove move, Battler user, Battler target) {
        // :77 empty
    }

    /** {@code pbEffectAgainstTarget(user,target)} (Move_Usage.rb:78). */
    @Override
    public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
        // :78 empty
    }

    /** {@code pbEffectGeneral(user)} (Move_Usage.rb:79). */
    @Override
    public void pbEffectGeneral(BattleMove move, Battler user) {
        // :79 empty
    }

    /** {@code pbAdditionalEffect(user,target)} (Move_Usage.rb:80). */
    @Override
    public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
        // :80 empty
    }

    /** {@code pbEffectAfterAllHits(user,target)} (Move_Usage.rb:81). */
    @Override
    public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
        // :81 empty
    }

    /** {@code pbSwitchOutTargetsEffect(user,targets,numHits,switchedBattlers)} (Move_Usage.rb:82). */
    @Override
    public void pbSwitchOutTargetsEffect(BattleMove move, Battler user, Array<Battler> targets, int numHits,
                                        IntArray switchedBattlers) {
        // :82 empty
    }

    /** {@code pbEndOfMoveUsageEffect(user,targets,numHits,switchedBattlers)} (Move_Usage.rb:83). */
    @Override
    public void pbEndOfMoveUsageEffect(BattleMove move, Battler user, Array<Battler> targets, int numHits,
                                       IntArray switchedBattlers) {
        // :83 empty
    }

    /** {@code pbImmunityByAbility(user,target)} (Move_Usage.rb:88-96). */
    @Override
    public boolean pbImmunityByAbility(BattleMove move, Battler user, Battler target) {
        Battle battle = user.battle;
        if (battle.moldBreaker) {
            return false;                                // :89 return false if @battle.moldBreaker
        }
        boolean ret = false;                             // :90
        if (target.abilityActive()) {          // :91
            // :92-93 BattleHandlers.triggerMoveImmunityTargetAbility(ability,
            //       user,target,self,@calcType,@battle); @calcType is null when -1.
            ret = BattleHandlers.triggerMoveImmunityTargetAbility(target.ability,
                    user, target, move, move.calcType(), battle);
        }
        return ret;                                      // :95
    }

    /** {@code pbMoveFailed?(user,targets)} (Move_Usage.rb:102). */
    @Override
    public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
        return false;                                    // :102
    }

    /**
     * {@code pbFailsAgainstTarget?(user,target)} (Move_Usage.rb:104).
     *
     * <p>Two parameters, not the plugin's one three-parameter override - see
     * {@link MoveEffect#pbFailsAgainstTarget} (task-11 decision 6).</p>
     */
    @Override
    public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
        return false;                                    // :104
    }

    /** {@code pbMoveFailedLastInRound?(user)} (Move_Usage.rb:106-120). */
    @Override
    public boolean pbMoveFailedLastInRound(BattleMove move, Battler user) {
        Battle battle = user.battle;
        boolean unmoved = false;                         // :107
        for (Battler b : battle.eachBattler()) {                    // :108
            if (b.index == user.index) {
                continue;                                // :109 next if b.index==user.index
            }
            if (!choiceIsMoveOrShift(battle, b.index)) {
                continue;                                // :110 next if choices[b.index][0] is neither :UseMove nor :Shift
            }
            if (b.movedThisRound()) {
                continue;                                // :111 next if b.movedThisRound?
            }
            unmoved = true;                              // :112
            break;                                       // :113
        }
        if (!unmoved) {                                  // :115
            battle.display("但是失败了！");   // :116
            return true;                                 // :117
        }
        return false;                                    // :119
    }

    /** {@code pbMoveFailedTargetAlreadyMoved?(target)} (Move_Usage.rb:122-129). */
    @Override
    public boolean pbMoveFailedTargetAlreadyMoved(BattleMove move, Battler target) {
        Battle battle = target.battle;
        // :123-124 (choices[target.index][0] is neither :UseMove nor :Shift) || target.movedThisRound?
        if (!choiceIsMoveOrShift(battle, target.index) || target.movedThisRound()) {
            battle.display("但是失败了！");   // :125
            return true;                                 // :126
        }
        return false;                                    // :128
    }

    /** {@code pbMoveFailedAromaVeil?(user,target,showMessage=true)} (Move_Usage.rb:131-161). */
    @Override
    public boolean pbMoveFailedAromaVeil(BattleMove move, Battler user, Battler target, boolean showMessage) {
        Battle battle = user.battle;
        if (battle.moldBreaker) {
            return false;                                // :132
        }
        if (target.hasActiveAbility("AROMAVEIL")) {               // :133
            if (showMessage) {                                               // :134
                battle.showAbilitySplash(target);              // :135
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                                    // :136
                    battle.display(target.pbThis() + "没有受到影响！");  // :137
                } else {
                    battle.display(target.pbThis() + "因"
                            + target.abilityName() + "而不受影响！");               // :139-140
                }
                battle.hideAbilitySplash(target);              // :142
            }
            return true;                                                     // :144
        }
        for (Battler b : target.allAllies()) {                      // :146
            if (!b.hasActiveAbility("AROMAVEIL")) {
                continue;                                                    // :147
            }
            if (showMessage) {                                               // :148
                battle.showAbilitySplash(target);              // :149
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                                    // :150
                    battle.display(target.pbThis() + "没有受到影响！");  // :151
                } else {
                    battle.display("由于" + b.pbThis(true) + "的"
                            + b.abilityName() + "\n" + target.pbThis() + "不受影响！");  // :153-154
                }
                battle.hideAbilitySplash(target);              // :156
            }
            return true;                                                     // :158
        }
        return false;                                                        // :160
    }

    // ==================================================================
    // C. Calculations (Move_Usage_Calculations.rb)
    // ==================================================================

    /** {@code pbBaseType(user)} (Move_Usage_Calculations.rb:5-12). */
    @Override
    public String pbBaseType(BattleMove move, Battler user) {
        String ret = move.type();                        // :6 ret = @type
        if (ret == null) {                               // :7 return ret if !ret || ret<0
            return null;                                 //    (null is the plugin's -1)
        }
        if (user.abilityActive()) {            // :8
            // :9 BattleHandlers.triggerMoveBaseTypeModifierAbility(user.ability,user,self,ret)
            ret = BattleHandlers.triggerMoveBaseTypeModifierAbility(user.ability, user, move, ret);
        }
        return ret;                                      // :11
    }

    /**
     * {@code pbCalcType(user)} (Move_Usage_Calculations.rb:14-29) - not
     * transcribed in this batch (task-11 decision 10).
     */
    @Override
    public String pbCalcType(BattleMove move, Battler user) {
        move.setPowerBoost(false);                                    // :15 @powerBoost = false
        String ret = pbBaseType(move, user);                          // :16 ret = pbBaseType(user)
        if (ret == null) {                                           // :17 return ret if !ret || ret<0
            return null;                                             //    (null is the plugin's -1)
        }
        // :18 hasConst?(PBTypes,:ELECTRIC) - the runtime's type table always has ELECTRIC
        // :19-22 Ion Deluge turns a Normal-type move Electric
        // 登记: the plugin's @battle is always set, so :19 reads it unguarded
        // (`@battle.field.effects[...]`). This runtime hangs the battle on the battler
        // rather than on the move, and unit tests build battlers with no battle at all,
        // so the read is guarded instead of allowed to NPE. In a real battle the guard
        // is always true and the branch runs exactly as written.
        if (user.battle != null
                && user.battle.field.effects.truthy(PBEffects.Field.IonDeluge) && "NORMAL".equals(ret)) {
            ret = "ELECTRIC";                                        // :20 getConst(PBTypes,:ELECTRIC)
            move.setPowerBoost(false);                               // :21
        }
        // :23-26 Electrify
        if (user.effects.truthy(PBEffects.Battler.Electrify)) {
            ret = "ELECTRIC";                                        // :24
            move.setPowerBoost(false);                               // :25
        }
        return ret;                                                  // :28
    }

    /** {@code pbCalcTypeModSingle(moveType,defType,user,target)} (Move_Usage_Calculations.rb:34-71) - not transcribed (decision 10). */
    @Override
    public int pbCalcTypeModSingle(BattleMove move, String moveType, String defType, Battler user, Battler target) {
        PbsData pbs = user.battle.pbs();
        int ret = PBTypes.getEffectiveness(pbs, moveType, defType);  // :35 PBTypes.getEffectiveness
        // :36-39 Ring Target
        if (target.hasActiveItem("RINGTARGET") && PBTypes.ineffective(ret)) {
            ret = PBTypeEffectiveness.NORMAL_EFFECTIVE_ONE;          // :38
        }
        // :40-45 Foresight / Scrappy / Mind's Eye
        if ("GHOST".equals(defType) && PBTypes.ineffective(ret)) {   // :41
            if (user.hasActiveAbility("SCRAPPY") || user.hasActiveAbility("MINDSEYE")
                    || target.effects.truthy(PBEffects.Battler.Foresight)) {   // :42
                ret = PBTypeEffectiveness.NORMAL_EFFECTIVE_ONE;      // :43
            }
        }
        // :46-50 FEARLESS
        if (user.hasActiveAbility("FEARLESS")
                || target.effects.truthy(PBEffects.Battler.Foresight)) {       // :47
            if ("GHOST".equals(defType) && PBTypes.ineffective(ret)) {         // :48
                ret = PBTypeEffectiveness.NORMAL_EFFECTIVE_ONE;      // :49
            }
        }
        // :51-55 Miracle Eye
        if (target.effects.truthy(PBEffects.Battler.MiracleEye)) {   // :52
            if ("DARK".equals(defType) && PBTypes.ineffective(ret)) {          // :53
                ret = PBTypeEffectiveness.NORMAL_EFFECTIVE_ONE;      // :54
            }
        }
        // :56-60 Delta Stream's weather
        if (user.battle.pbWeather() == PBWeather.StrongWinds) {      // :57
            if ("FLYING".equals(defType) && PBTypes.superEffective(ret)) {     // :58
                ret = PBTypeEffectiveness.NORMAL_EFFECTIVE_ONE;      // :59
            }
        }
        // :61-65 grounded Flying-type Pokemon become susceptible to Ground moves
        if (!target.airborne()) {                                    // :62
            if ("FLYING".equals(defType) && "GROUND".equals(moveType)) {       // :63-64
                ret = PBTypeEffectiveness.NORMAL_EFFECTIVE_ONE;
            }
        }
        // :66-69 Tar Shot
        if (target.effects.truthy(PBEffects.Battler.TarShot) && "FIRE".equals(moveType)) {
            // PBTypes.normalEffective?/notVeryEffective? take the COMBINED value; the
            // plugin's getCombinedEffectiveness multiplies up to three types and pads
            // with neutral, so two types give e1*e2*2 (Move_Usage_Calculations:81).
            int combined = 1;
            Array<String> tarTypes = target.pbTypes();
            for (String t : tarTypes) {
                combined *= PBTypes.getEffectiveness(pbs, moveType, t);
            }
            for (int i = tarTypes.size; i < 3; i++) {
                combined *= PBTypeEffectiveness.NORMAL_EFFECTIVE_ONE;
            }
            if (PBTypes.normalEffective(combined)) {                 // :67
                ret = PBTypeEffectiveness.SUPER_EFFECTIVE_ONE;
            }
            if (PBTypes.notVeryEffective(combined)) {                // :68
                ret = PBTypeEffectiveness.NORMAL_EFFECTIVE_ONE;
            }
        }
        return ret;                                                  // :70
    }

    /** {@code pbCalcTypeMod(moveType,user,target)} (Move_Usage_Calculations.rb:73-103) - not transcribed (decision 10). */
    @Override
    public int pbCalcTypeMod(BattleMove move, String moveType, Battler user, Battler target) {
        if (moveType == null) {                                      // :74 if moveType<0
            return PBTypeEffectiveness.NORMAL_EFFECTIVE;
        }
        // :75-76 Ground vs a Flying-type holding an Iron Ball
        if ("GROUND".equals(moveType) && target.pbHasType("FLYING")
                && target.hasActiveItem("IRONBALL")) {
            return PBTypeEffectiveness.NORMAL_EFFECTIVE;
        }
        if ("STELLAR".equals(moveType)) {                            // :77
            return PBTypeEffectiveness.NORMAL_EFFECTIVE * 2;
        }
        // :78-84 get the effectivenesses of each of the target's types
        Array<String> tTypes = target.pbTypes(true);                 // :79
        int[] typeMods = new int[3];                                 // :81 [NORMAL_EFFECTIVE_ONE] * 3
        for (int i = 0; i < typeMods.length; i++) {
            typeMods[i] = PBTypeEffectiveness.NORMAL_EFFECTIVE_ONE;
        }
        for (int i = 0; i < tTypes.size; i++) {                      // :82-83
            typeMods[i] = pbCalcTypeModSingle(move, moveType, tTypes.get(i), user, target);
        }
        // :85-87 multiply all effectivenesses together
        int ret = 1;
        for (int m : typeMods) {
            ret *= m;
        }
        // :88-92 Tera Shell (the plugin's hasMoldBreaker? is the ability plus the field flag)
        if (target.hasActiveAbility("TERASHELL") && damagingMove(move)          // :88
                && !(user.hasActiveAbility("MOLDBREAKER") || user.battle.moldBreaker)
                && target.hp == target.maxHp() && ret >= PBTypeEffectiveness.NORMAL_EFFECTIVE) {
            target.damageState.terashell = true;                     // :90
            ret = PBTypeEffectiveness.NORMAL_EFFECTIVE_ONE;          // :91
        }
        // :93-98 地魔之剑 / 海魔之雨的易伤效果
        if ("POISON".equals(moveType)
                && target.effects.intVal(PBEffects.Battler.PoisonVulnerability) > 0) {   // :96
            ret = Math.round(ret * 1.5f);                            // :97
        }
        if ("ICE".equals(moveType)
                && target.effects.intVal(PBEffects.Battler.IceVulnerability) > 0) {      // :99
            ret = Math.round(ret * 1.5f);                            // :100
        }
        return ret;                                                  // :102
    }

    /** {@code pbBaseAccuracy(user,target)} (Move_Usage_Calculations.rb:108). */
    @Override
    public int pbBaseAccuracy(BattleMove move, Battler user, Battler target) {
        return move.accuracy();                          // :108 return @accuracy
    }

    /** {@code pbAccuracyCheck(user,target)} (Move_Usage_Calculations.rb:112-143) - not transcribed (decision 10). */
    @Override
    public boolean pbAccuracyCheck(BattleMove move, Battler user, Battler target) {
        throw new UnsupportedOperationException("M0 待接线: Move_Usage_Calculations.rb:112-143 pbAccuracyCheck");
    }

    /** {@code pbCalcAccuracyModifiers(user,target,modifiers)} (Move_Usage_Calculations.rb:145-183) - not transcribed (decision 10). */
    @Override
    public void pbCalcAccuracyModifiers(BattleMove move, Battler user, Battler target, float[] modifiers) {
        throw new UnsupportedOperationException(
                "M0 待接线: Move_Usage_Calculations.rb:145-183 pbCalcAccuracyModifiers");
    }

    /**
     * {@code pbCalcAccuracyMultipliers(user,target,multipliers)} - dead and
     * broken in the plugin (task-11 decision 5); empty body so the four
     * overrides have a {@code super} to call.
     */
    @Override
    public void pbCalcAccuracyMultipliers(BattleMove move, Battler user, Battler target, float[] multipliers) {
        // Move_Effects_080-0FF:1135-1138: the plugin never calls this method
        // (pbAccuracyCheck:126 calls pbCalcAccuracyModifiers:145) and every
        // override reads an undefined local `modifiers`, so there is no
        // behaviour to reproduce. Nothing is done here on purpose.
    }

    /** {@code pbCritialOverride(user,target)} (Move_Usage_Calculations.rb:192). */
    @Override
    public int pbCritialOverride(BattleMove move, Battler user, Battler target) {
        return 0;                                        // :192 -1 never, 0 normal, 1 always
    }

    /** {@code pbIsCritical?(user,target)} (Move_Usage_Calculations.rb:196-229) - not transcribed (decision 10). */
    @Override
    public boolean pbIsCritical(BattleMove move, Battler user, Battler target) {
        throw new UnsupportedOperationException("M0 待接线: Move_Usage_Calculations.rb:196-229 pbIsCritical?");
    }

    /** {@code pbBaseDamage(baseDmg,user,target)} (Move_Usage_Calculations.rb:234). */
    @Override
    public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
        return baseDmg;                                  // :234
    }

    /** {@code pbBaseDamageMultiplier(damageMult,user,target)} (Move_Usage_Calculations.rb:235). */
    @Override
    public float pbBaseDamageMultiplier(BattleMove move, float damageMult, Battler user, Battler target) {
        return damageMult;                               // :235
    }

    /** {@code pbModifyDamage(damageMult,user,target)} (Move_Usage_Calculations.rb:236). */
    @Override
    public float pbModifyDamage(BattleMove move, float damageMult, Battler user, Battler target) {
        return damageMult;                               // :236
    }

    /** {@code pbGetAttackStats(user,target)} (Move_Usage_Calculations.rb:238-243). */
    @Override
    public MoveStats pbGetAttackStats(BattleMove move, Battler user, Battler target) {
        if (specialMove(move, null)) {                                           // :239
            // :240 return user.spatk, user.stages[PBStats::SPATK]+6
            return new MoveStats(user.pokemon.spAtk(),
                    user.stage(PBStats.SPATK) + 6);
        }
        // :242 return user.attack, user.stages[PBStats::ATTACK]+6
        return new MoveStats(user.pokemon.attack(),
                user.stage(PBStats.ATTACK) + 6);
    }

    /** {@code pbGetDefenseStats(user,target)} (Move_Usage_Calculations.rb:245-250). */
    @Override
    public MoveStats pbGetDefenseStats(BattleMove move, Battler user, Battler target) {
        if (specialMove(move, null)) {                                           // :246
            // :247 return target.spdef, target.stages[PBStats::SPDEF]+6
            return new MoveStats(target.pokemon.spDef(),
                    target.stage(PBStats.SPDEF) + 6);
        }
        // :249 return target.defense, target.stages[PBStats::DEFENSE]+6
        return new MoveStats(target.pokemon.defense(),
                target.stage(PBStats.DEFENSE) + 6);
    }

    /** {@code pbCalcDamage(user,target,numTargets=1)} (Move_Usage_Calculations.rb:252-295) - not transcribed (decision 10). */
    @Override
    public void pbCalcDamage(BattleMove move, Battler user, Battler target, int numTargets) {
        throw new UnsupportedOperationException("M0 待接线: Move_Usage_Calculations.rb:252-295 pbCalcDamage");
    }

    /** {@code pbCalcDamageMultipliers(...)} (Move_Usage_Calculations.rb:296-546) - not transcribed (decision 10). */
    @Override
    public void pbCalcDamageMultipliers(BattleMove move, Battler user, Battler target, int numTargets, String type,
                                        int baseDmg, float[] multipliers) {
        throw new UnsupportedOperationException(
                "M0 待接线: Move_Usage_Calculations.rb:296-546 pbCalcDamageMultipliers");
    }

    /** {@code pbAdditionalEffectChance(user,target,effectChance=0)} (Move_Usage_Calculations.rb:547-557). */
    @Override
    public int pbAdditionalEffectChance(BattleMove move, Battler user, Battler target, int effectChance) {
        if (target.hasActiveItem("COVERTCLOAK")) {
            return 0;                                    // :548
        }
        if (target.hasActiveAbility("SHIELDDUST") && !user.battle.moldBreaker) {
            return 0;                                    // :549
        }
        int ret = effectChance > 0 ? effectChance : addlEffect(move);         // :550
        if (Battle.NEWEST_BATTLE_MECHANICS || !"0A4".equals(move.function())) {  // :551
            // :552-553 ret *= 2 if Serene Grace or the side's Rainbow is up
            if (user.hasActiveAbility("SERENEGRACE")
                    || user.pbOwnSide().effects.intVal(PBEffects.Side.Rainbow) > 0) {
                ret *= 2;
            }
        }
        // :555 ret = 100 if $DEBUG && Input.press?(Input::CTRL) - NOT
        //      transcribed: this runtime has no debug key input (registered).
        return ret;                                      // :556
    }

    /** {@code pbFlinchChance(user,target)} (Move_Usage_Calculations.rb:561-576). */
    @Override
    public int pbFlinchChance(BattleMove move, Battler user, Battler target) {
        if (flinchingMove(move)) {
            return 0;                                    // :562
        }
        if (target.hasActiveItem("COVERTCLOAK")) {
            return 0;                                    // :563
        }
        if (target.hasActiveAbility("SHIELDDUST") && !user.battle.moldBreaker) {
            return 0;                                    // :564
        }
        int ret = 0;                                     // :565
        if (user.hasActiveAbility("STENCH", true)) {              // :566
            ret = 10;                                                         // :567
        } else if (user.hasActiveAbility("SEAMLESS", true)) {     // :568
            ret = 30;                                                         // :569
        } else if (user.hasActiveItem(new String[] { "KINGSROCK", "RAZORFANG" }, true)) {  // :570
            ret = 10;                                                         // :571
        }
        // :573-574 ret *= 2 if Serene Grace or the side's Rainbow is up
        if (user.hasActiveAbility("SERENEGRACE")
                || user.pbOwnSide().effects.intVal(PBEffects.Side.Rainbow) > 0) {
            ret *= 2;
        }
        return ret;                                      // :575
    }

    // ==================================================================
    // D. Hooks the plugin's base class does not define
    // ==================================================================

    /** {@code PokeBattle_FixedDamageMove#pbFixedDamage} (Move_Effects_Generic.rb:474) - no base default (decision 8). */
    @Override
    public int pbFixedDamage(BattleMove move, Battler user, Battler target) {
        throw new UnsupportedOperationException(
                "插件无默认实现（只有 PokeBattle_FixedDamageMove 定义它）: Move_Effects_Generic.rb:474");
    }

    /** {@code PokeBattle_HealingMove#pbHealAmount} (Move_Effects_Generic.rb:570) - no base default (decision 8). */
    @Override
    public int pbHealAmount(BattleMove move, Battler user) {
        throw new UnsupportedOperationException(
                "插件无默认实现（只有 PokeBattle_HealingMove 定义它）: Move_Effects_Generic.rb:570");
    }

    /** {@code PokeBattle_RecoilMove#pbRecoilDamage} (Move_Effects_Generic.rb:594) - no base default (decision 8). */
    @Override
    public int pbRecoilDamage(BattleMove move, Battler user, Battler target) {
        throw new UnsupportedOperationException(
                "插件无默认实现（只有 PokeBattle_RecoilMove 定义它）: Move_Effects_Generic.rb:594");
    }

    /** {@code PokeBattle_TwoTurnMove#pbChargingTurnMessage} (Move_Effects_Generic.rb:536) - no base default. */
    @Override
    public void pbChargingTurnMessage(BattleMove move, Battler user, Array<Battler> targets) {
        throw new UnsupportedOperationException(
                "插件无默认实现（只有 PokeBattle_TwoTurnMove 定义它）: Move_Effects_Generic.rb:536");
    }

    /** {@code PokeBattle_TwoTurnMove#pbAttackingTurnMessage} (Move_Effects_Generic.rb:540) - no base default. */
    @Override
    public void pbAttackingTurnMessage(BattleMove move, Battler user, Array<Battler> targets) {
        throw new UnsupportedOperationException(
                "插件无默认实现（只有 PokeBattle_TwoTurnMove 定义它）: Move_Effects_Generic.rb:540");
    }

    /** {@code PokeBattle_TwoTurnMove#pbChargingTurnEffect} (Move_Effects_Generic.rb:543) - no base default. */
    @Override
    public void pbChargingTurnEffect(BattleMove move, Battler user, Battler target) {
        throw new UnsupportedOperationException(
                "插件无默认实现（只有 PokeBattle_TwoTurnMove 定义它）: Move_Effects_Generic.rb:543");
    }

    /** {@code PokeBattle_TwoTurnMove#pbAttackingTurnEffect} (Move_Effects_Generic.rb:548) - no base default. */
    @Override
    public void pbAttackingTurnEffect(BattleMove move, Battler user, Battler target) {
        throw new UnsupportedOperationException(
                "插件无默认实现（只有 PokeBattle_TwoTurnMove 定义它）: Move_Effects_Generic.rb:548");
    }

    /** {@code PokeBattle_ProtectMove#pbProtectMessage} (Move_Effects_Generic.rb:661) - no base default. */
    @Override
    public void pbProtectMessage(BattleMove move, Battler user) {
        throw new UnsupportedOperationException(
                "插件无默认实现（只有 PokeBattle_ProtectMove 定义它）: Move_Effects_Generic.rb:661");
    }

    /**
     * {@code addlEffect} (PokeBattle_Move.rb:13 {@code attr_reader :addlEffect})
     * - the value is the move's {@code MOVE_EFFECT_CHANCE}
     * (PokeBattle_Move.rb:39), i.e. {@link BattleMove#additionalChance()}
     * (task-11 decision 7).
     */
    @Override
    public int addlEffect(BattleMove move) {
        return move.additionalChance();                  // :39 @addlEffect = moveData[MOVE_EFFECT_CHANCE]
    }

    /** {@code pbNaturalGiftBaseDamage(heldItem)} (Move_Effects_080-0FF:529) - body not transcribed (class 096). */
    @Override
    public int pbNaturalGiftBaseDamage(BattleMove move, String heldItem) {
        throw new UnsupportedOperationException(
                "M0 待接线: Move_Effects_080-0FF:529 pbNaturalGiftBaseDamage");
    }

    /** {@code pbCheckFlingSuccess(user)} (Move_Effects_080-0FF:3715) - body not transcribed (class 0F7). */
    @Override
    public void pbCheckFlingSuccess(BattleMove move, Battler user) {
        throw new UnsupportedOperationException("M0 待接线: Move_Effects_080-0FF:3715 pbCheckFlingSuccess");
    }

    /** {@code pbAromatherapyHeal(pkmn,battler=nil)} (Move_Effects_000-07F:449) - body not transcribed (class 019). */
    @Override
    public void pbAromatherapyHeal(BattleMove move, Pokemon pkmn, Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: Move_Effects_000-07F:449 pbAromatherapyHeal");
    }

    /** The {@code battler=nil} form of {@code pbAromatherapyHeal} (Move_Effects_000-07F:449). */
    @Override
    public void pbAromatherapyHeal(BattleMove move, Pokemon pkmn) {
        pbAromatherapyHeal(move, pkmn, null);
    }

    /**
     * {@code pbHitEffectivenessMessages(user,target,numTargets=1)}
     * (Move_Usage.rb:322-345) - transcribed in {@link MoveUsage} (stage 5 / 2b).
     */
    @Override
    public void pbHitEffectivenessMessages(BattleMove move, Battler user, Battler target, int numTargets) {
        MoveUsage.pbHitEffectivenessMessages(this, move, user, target, numTargets);   // Move_Usage:322-345 (stage 5 / 2b)
    }
}
