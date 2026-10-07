package pokemon.runtime.battle.movefx;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;

import pokemon.runtime.battle.Battle;
import pokemon.runtime.battle.BattleMove;
import pokemon.runtime.battle.BattleSide;
import pokemon.runtime.battle.Battler;
import pokemon.runtime.battle.BattlerTargeting;
import pokemon.runtime.battle.PBEffects;
import pokemon.runtime.battle.PBEnvironment;
import pokemon.runtime.battle.PBBattleTerrains;
import pokemon.runtime.battle.PokeBattle_SceneConstants;
import pokemon.runtime.battle.PBStats;
import pokemon.runtime.battle.PBStatuses;
import pokemon.runtime.battle.PBTargets;
import pokemon.runtime.battle.PBWeather;
import pokemon.runtime.battle.PBTypes;
import pokemon.runtime.battle.PendingApi;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

/**
 * Stage 4 / L2: the 128 function-code classes of
 * {@code Move_Effects_000-07F.rb} (2862 lines), one nested static class each,
 * transcribed line by line. Every override carries a
 * {@code // Move_Effects_000-07F.rb:行号} comment.
 *
 * <p>Ruby puts the effect on the move object itself
 * ({@code class PokeBattle_Move_000 < PokeBattle_Move}); here the effect is a
 * strategy object, so each class extends the matching
 * {@link MoveEffectsGeneric} base (or {@link MoveEffectBase} when the Ruby
 * parent is {@code PokeBattle_Move} itself) and every hook takes the move as its
 * first parameter ({@link MoveEffect}'s documented shape).</p>
 *
 * <h2>Documented translations (Lead-approved 2026-10-07)</h2>
 * <ul>
 * <li>{@code isConst?(@id,PBMoves,:X)} / {@code getID(PBMoves,:X)} compare the
 *     move's PBS identity; this runtime stores the internal name, so they become
 *     {@code "X".equals(move.internalName())} - the same comparison
 *     {@code AbilitiesAccuracyCritType} already uses
 *     (e.g. {@code BattleHandlers_Abilities:4291}).</li>
 * <li>{@code @battle.pbDisplay(msg)} &rarr; {@code user.battle.display(msg)}
 *     ({@code Battle.display}, the landed mirror of the plugin's
 *     {@code PokeBattle_Battle#pbDisplay}); {@code @battle.pbRandom(n)} &rarr;
 *     {@code user.battle.pbRandom(n)}; {@code @battle.pbWeather} &rarr;
 *     {@code user.battle.pbWeather()}; {@code @battle.pbShowAbilitySplash} /
 *     {@code pbHideAbilitySplash} / {@code pbReplaceAbilitySplash} &rarr; the
 *     matching {@code Battle} methods; {@code @battle.battlers[i]} &rarr;
 *     {@code battle.battlerAt(i)}; {@code @battle.pbParty(idx)} &rarr;
 *     {@code battle.partyOf(idx)} (this runtime fields every party member as a
 *     {@code Battler}, see {@code AbilitiesDamageTarget:287}).</li>
 * <li>{@code @effects[X] = v} &rarr; {@code effects.set(X, v)} with the
 *     boolean/integer overload that matches the Ruby literal; reads use
 *     {@code truthy} (Ruby truthiness, {@code 0} is truthy) or {@code intVal}
 *     (numeric comparison), per {@code EffectMap}'s javadoc.</li>
 * <li>{@code @status==PBStatuses::X} &rarr; {@code hasStatus("X")};
 *     {@code @status==0} / {@code ==PBStatuses::NONE} &rarr; {@code !statused()};
 *     {@code @status = PBStatuses::NONE} on a party Pokemon &rarr;
 *     {@code pokemon.status = ""} (PBStatuses' documented deviation).</li>
 * <li>{@code stages[PBStats::X]} &rarr; {@code stage(X)} / {@code setStage(X,v)}
 *     ({@code Battler.stage(int)} exists because the runtime array is a 5-element
 *     shifted layout - never index {@code stages} directly);
 *     {@code totalhp} &rarr; {@code maxHp()}.</li>
 * <li>{@code eachMoveWithIndex} &rarr; {@code moveSlots()} with the
 *     {@code id()==0} slots skipped (PokeBattle_Battler:543-549).</li>
 * <li>The methods this runtime has not landed are private helpers at the top of
 *     this file (Lead decision: no new stubs in {@code MoveFxPendingApi}, which
 *     four L2 authors would otherwise contend for). Each helper's javadoc cites
 *     its Ruby {@code 段:行号}; the throwing ones carry the project's
 *     {@code M0 待接线} message. {@code inTwoTurnAttack?} is provably
 *     {@code false} here (nothing in this runtime ever writes
 *     {@code PBEffects::TwoTurnAttack}), so it returns {@code false} with a
 *     comment instead of throwing.</li>
 * <li>{@code pbShowAnimation} overrides are transcribed as-is (they call
 *     {@code super.pbShowAnimation}, which is the animation player's entry
 *     point) and marked {@code // 登记:} where the body needs the animation
 *     player; {@code pbCalcAccuracyMultipliers} overrides keep the plugin's
 *     undefined variable name verbatim ({@code // 登记:} - the plugin's own
 *     defect, not corrected).</li>
 * </ul>
 *
 * <p>Registration into {@link MoveEffectRegistry} is done by the Lead after the
 * L2 retire pass; this file only defines the classes.</p>
 *
 * <p><b>Note on {@code final}:</b> the task brief said {@code public static final
 * class}, but the plugin's function-code classes inherit ACROSS the
 * {@code Move_Effects_*} files - e.g. {@code Move_Effects_180-1FF.rb:144}
 * {@code class PokeBattle_Move_187 < PokeBattle_Move_005} where 005 lives in
 * this file, and {@code Move_Effects_180-1FF.rb:180}
 * {@code PokeBattle_Move_188 < PokeBattle_Move_0A0} - so the classes here are
 * deliberately <b>not</b> final. {@code Move_Effects_180_1FF.java} would not
 * compile otherwise.</p>
 */
public final class MoveEffects_000_07F {

    private MoveEffects_000_07F() {
    }

    // ==================================================================
    // Private helpers for methods this runtime has not landed yet.
    // Lead decision 2026-10-07: no new stubs in MoveFxPendingApi (movebase is
    // editing it for the retire pass and four L2 authors would contend for it).
    // ==================================================================

    /** {@code Battler#canChangeType?} (PokeBattle_Battler:585-589). */
    private static boolean canChangeType(Battler battler) {
        return battler.canChangeType();                   // PokeBattle_Battler:585-589
    }

    /** {@code Battler#pbCheckFormOnMovesetChange} (Battler_ChangeSelf:217-225). */
    private static void pbCheckFormOnMovesetChange(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: Battler_ChangeSelf:217-225 pbCheckFormOnMovesetChange");
    }

    /** {@code Battler#pbEffectsOnSwitchIn(switchIn=false)} (Battler_AbilityAndItem:5-12). */
    private static void pbEffectsOnSwitchIn(Battler battler, boolean switchIn) {
        battler.pbEffectsOnSwitchIn(switchIn);
    }

    /**
     * {@code Battler#pbHasMove?(move_id)} (PokeBattle_Battler:551-553).
     *
     * <p>Takes the move's internal name: this runtime stores
     * {@code lastRegularMoveUsed}/{@code lastMoveUsed} as internal-name Strings
     * with {@code null} for the plugin's {@code -1} (Battler.java:531-534), the
     * documented deviation {@code AbilitiesOnHit:487-489} also relies on.</p>
     */
    private static boolean pbHasMove(Battler battler, String internalName) {
        return battler.pbHasMove(battler.moveIdOf(internalName));   // PokeBattle_Battler:551-553
    }

    /** {@code Battler#attack=} ({@code attr_accessor :attack}, PokeBattle_Battler:15). */
    private static void setAttack(Battler battler, int value) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:15 attr_accessor :attack (=)");
    }

    /** {@code Battler#spatk=} ({@code attr_accessor :spatk}, PokeBattle_Battler:16). */
    private static void setSpAtk(Battler battler, int value) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:16 attr_accessor :spatk (=)");
    }

    /**
     * {@code Battler#defense=} (Guard Split, Move_Effects_000-07F.rb:1694).
     *
     * <p>登记: the plugin declares no {@code defense=} writer at all (only
     * {@code attr_accessor :attack/:spatk/:speed}, PokeBattle_Battler:15-17), so
     * the Ruby would raise {@code NoMethodError}; reproduced, not corrected.</p>
     */
    private static void setDefense(Battler battler, int value) {
        throw new UnsupportedOperationException("M0 待接线: Move_Effects_000-07F.rb:1694 defense= (插件无该访问器)");
    }

    /**
     * {@code Battler#spdef=} (Guard Split, Move_Effects_000-07F.rb:1695).
     *
     * <p>登记: same plugin defect as {@link #setDefense} - no {@code spdef=}
     * writer exists in the plugin; reproduced, not corrected.</p>
     */
    private static void setSpDef(Battler battler, int value) {
        throw new UnsupportedOperationException("M0 待接线: Move_Effects_000-07F.rb:1695 spdef= (插件无该访问器)");
    }

    /** {@code Battler#pbWeight} (PokeBattle_Battler:281-292). */
    private static int pbWeight(Battler battler) {
        return battler.pbWeight();                             // PokeBattle_Battler:281-292
    }

    /** {@code Battler#addSideStatUps(stat,increment)} (PokeBattle_Battler:892-895). */
    private static void addSideStatUps(Battler battler, int stat, int increment) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:892-895 addSideStatUps");
    }

    /**
     * {@code Battle#pbFindBattler(idxParty,idxBattlerOther=0)} (PokeBattle_Battle:631-634).
     *
     * <p>The second parameter is declared as a {@code Battler} because that is what
     * the plugin's own call site passes: {@code @battle.pbFindBattler(i,user)}
     * (Move_Effects_000-07F.rb:491), while the Ruby method feeds it to
     * {@code eachSameSideBattler(idxBattlerOther)} (:632), i.e. treats it as an
     * index. 登记: that is a plugin defect (a Battler where an index is expected);
     * it is reproduced, not corrected.</p>
     */
    private static Battler pbFindBattler(Battle battle, int idxParty, Battler idxBattlerOther) {
        return battle.pbFindBattler(idxParty, idxBattlerOther.index);   // eachSameSideBattler takes a Battler's index (:632)
    }

    /** {@code Battler#moves[i] = move} ({@code attr_accessor :moves}, PokeBattle_Battler:12). */
    private static void setMove(Battler battler, int index, BattleMove move) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:12 attr_accessor :moves (slot write)");
    }

    /** {@code pokemon.moves[i] = move} ({@code attr_accessor :moves}, PokeBattle_Pokemon:22). */
    private static void setPokemonMove(Pokemon pkmn, int index, PbsData.Move move) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Pokemon:22 attr_accessor :moves (slot write)");
    }

    /** {@code Battler#form=} (Move_Effects_000-07F.rb:2664-2665). */
    private static void setForm(Battler battler, int value) {
        battler.setForm(value);
    }

    /** {@code Battler#pbAddTarget(targets,user,target,move,showMessage)} (Move_Effects_000-07F.rb:2536). */
    private static void battlerPbAddTarget(Battler battler, Array<Battler> targets, Battler target, BattleMove move,
                                           boolean showMessage) {
        BattlerTargeting.pbAddTarget(battler, targets, battler, target, move, showMessage, false);   // Battler_UseMove_Targeting:243 (5th arg is nearOnly)
    }

    /**
     * {@code user.pbChangeTypes(target)} (Move_Effects_000-07F.rb:2072).
     *
     * <p>登记: the plugin passes the target {@code Battler} where
     * {@code pbChangeTypes} expects a type, so the Ruby would fail; reproduced,
     * not corrected.</p>
     */
    private static void pbChangeTypesWithBattler(Battler user, Battler target) {
        user.pbChangeTypes(target);
    }

    /** Ruby {@code Array#==} for {@code user.pbTypes==target.pbTypes} (Move_Effects_000-07F.rb:2062). */
    private static boolean sameTypes(Array<String> a, Array<String> b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.size != b.size) {
            return false;
        }
        for (int i = 0; i < a.size; i++) {
            String left = a.get(i);
            String right = b.get(i);
            if (left == null ? right != null : !left.equals(right)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Ruby's {@code Array#include?} for the {@code @moveBlacklist} literals
     * (Move_Effects_000-07F.rb:1795/1846).
     */
    private static boolean contains(String[] array, String value) {
        if (value == null) {
            return false;
        }
        for (String entry : array) {
            if (value.equals(entry)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The id &rarr; internal-name lookup of {@code PBTypes.loadTypeData[2]}'s
     * index space, used by {@code PokeBattle_Move_05F}'s
     * {@code for i in 0..PBTypes.maxValue} loop (:1930).
     *
     * <p>This runtime identifies types by internal name, so the loop walks the
     * ids and maps each to its name. Mirrors {@code PBTypes.regularTypesCount}'s
     * own id walk (PBTypes.java) - the plugin's
     * {@code typeById} is private to {@code PBTypes}.</p>
     */
    private static String typeNameById(PbsData pbs, int id) {
        if (pbs == null) {
            return null;
        }
        for (PbsData.TypeInfo info : pbs.types.values()) {
            if (info != null && info.id == id) {
                return info.internalName;
            }
        }
        return null;
    }

    /** {@code Pokemon#statusCount = value} (PokeBattle_Pokemon attr_accessor, written by PokeBattle_Battler:115). */
    private static void setPokemonStatusCount(Pokemon pkmn, int value) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Pokemon statusCount= (PokeBattle_Battler:115)");
    }

    /**
     * {@code Battler#pbTransform(target)} (Battler_ChangeSelf:418-446) - 登记.
     *
     * <p>登记: the Ruby body needs {@code @battle.scene.pbRefreshOne} and
     * {@code PokeBattle_Move.pbFromPBMove} (the function-code factory), so the
     * m0b notes §384 and {@code AbilitiesSwitchIn:431} both record it as an
     * empty implementation. Nothing is invented here.</p>
     */
    private static void pbTransform(Battler battler, Battler target) {
        // 登记: Battler_ChangeSelf:418-446 pbTransform —— 依赖 @battle.scene.pbRefreshOne
        //       与 PokeBattle_Move.pbFromPBMove（function code → 类名映射），本批未建模。
    }

    /**
     * {@code Battler#inTwoTurnAttack?(*fn)} (PokeBattle_Battler:718-723).
     *
     * <p>Provably {@code false} in this runtime: the Ruby returns
     * {@code false} when {@code @effects[PBEffects::TwoTurnAttack]==0}, and
     * <b>no code in this runtime ever writes {@code PBEffects::TwoTurnAttack}</b>
     * (same reasoning as the approved {@code moldBreaker} and
     * {@code AbilitiesSwitchIn.inTwoTurnAttack} precedents), so the effect reads
     * back as the unset value and the predicate cannot be true. Returns
     * {@code false} rather than throwing, so the damage-calc hooks keep working.</p>
     */
    private static boolean inTwoTurnAttack(Battler battler, String... functions) {
        // PokeBattle_Battler:718-723; @effects[TwoTurnAttack] is never written in this runtime.
        return false;
    }

    /**
     * {@code Pokemon#able?} (PokeBattle_Pokemon:735-737) for the party members of
     * this runtime, which are {@code Battler}s.
     *
     * <p>Lead-approved inline form, mirroring {@code Battle.allFainted}
     * (Battle.java:210 {@code !battler.fainted() && !battler.pokemon.egg}) - the
     * plugin's {@code able?} is "not an egg and HP>0".</p>
     */
    private static boolean able(Battler battler) {
        return battler != null && !battler.fainted() && !battler.pokemon.egg;
    }

    // ==================================================================
    // Classes 000-035 (Move_Effects_000-07F.rb:4-983)
    // ==================================================================

    /** {@code class PokeBattle_Move_000 < PokeBattle_Move} (Move_Effects_000-07F.rb:4-5): no additional effect. */
    public static class PokeBattle_Move_000 extends MoveEffectBase {
    }

    /** {@code class PokeBattle_Move_001 < PokeBattle_Move} (Move_Effects_000-07F.rb:12-18): does absolutely nothing (Splash). */
    public static class PokeBattle_Move_001 extends MoveEffectBase {

        /** {@code unusableInGravity?} (:13). */
        @Override
        public boolean unusableInGravity(BattleMove move) {
            return true;                                                     // :13
        }

        /** {@code pbEffectGeneral(user)} (:15-17). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.battle.display("但什么也没有发生！");                            // :16
        }
    }

    /** {@code class PokeBattle_Move_002 < PokeBattle_Struggle} (Move_Effects_000-07F.rb:25-26). */
    public static class PokeBattle_Move_002 extends MoveEffectsGeneric.PokeBattle_Struggle {
    }

    /** {@code class PokeBattle_Move_003 < PokeBattle_SleepMove} (Move_Effects_000-07F.rb:33-54): puts the target to sleep. */
    public static class PokeBattle_Move_003 extends MoveEffectsGeneric.PokeBattle_SleepMove {

        /** {@code pbMoveFailed?(user,targets)} (:34-43). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (Battle.NEWEST_BATTLE_MECHANICS && "DARKVOID".equals(move.internalName())) {   // :35 isConst?(@id,PBMoves,:DARKVOID)
                if (!user.isSpecies("DARKRAI")                                // :36
                        && !"DARKRAI".equals(user.effects.stringVal(PBEffects.Battler.TransformSpecies))) {   // :37 isConst?(user.effects[TransformSpecies],PBSpecies,:DARKRAI)
                    user.battle.display("但是" + user.pbThis() + "不能使用这个招式！");   // :38
                    return true;                                              // :39
                }
            }
            return false;                                                     // :42
        }

        /** {@code pbEndOfMoveUsageEffect(user,targets,numHits,switchedBattlers)} (:45-53). */
        @Override
        public void pbEndOfMoveUsageEffect(BattleMove move, Battler user, Array<Battler> targets, int numHits,
                                           IntArray switchedBattlers) {
            if (numHits == 0) {                                               // :46
                return;
            }
            if (user.fainted() || user.effects.truthy(PBEffects.Battler.Transform)) {   // :47
                return;
            }
            if (!"RELICSONG".equals(move.internalName())) {                   // :48 isConst?(@id,PBMoves,:RELICSONG)
                return;
            }
            if (!user.isSpecies("MELOETTA")) {                                // :49
                return;
            }
            if (user.hasActiveAbility("SHEERFORCE") && addlEffect(move) > 0) {   // :50
                return;
            }
            int newForm = (user.form() + 1) % 2;                              // :51
            user.pbChangeFormTransform(newForm, user.pbThis() + "变身了！");     // :52
        }
    }

    /** {@code class PokeBattle_Move_004 < PokeBattle_Move} (Move_Effects_000-07F.rb:58-72): makes the target drowsy (Yawn). */
    public static class PokeBattle_Move_004 extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:59-66). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.effects.intVal(PBEffects.Battler.Yawn) > 0) {          // :60
                user.battle.display("但是失败了！");                             // :61
                return true;                                                  // :62
            }
            if (!target.pbCanSleep(user, true, move)) {                       // :64 target.pbCanSleep?(user,true,self)
                return true;
            }
            return false;                                                     // :65
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:68-71). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.Yawn, 2);                    // :69
            user.battle.display(user.pbThis() + "让" + target.pbThis(true) + "昏昏欲睡！");   // :70
        }
    }

    /** {@code class PokeBattle_Move_005 < PokeBattle_PoisonMove} (Move_Effects_000-07F.rb:79-80): poisons the target. */
    public static class PokeBattle_Move_005 extends MoveEffectsGeneric.PokeBattle_PoisonMove {
    }

    /** {@code class PokeBattle_Move_006 < PokeBattle_PoisonMove} (Move_Effects_000-07F.rb:87-96): badly poisons (Poison Fang, Toxic). */
    public static class PokeBattle_Move_006 extends MoveEffectsGeneric.PokeBattle_PoisonMove {

        /** {@code initialize} (:88-91): {@code @toxic = true}. */
        public PokeBattle_Move_006() {
            toxic = true;                                                     // :90
        }

        /** {@code pbOverrideSuccessCheckPerHit(user,target)} (:93-95). */
        @Override
        public boolean pbOverrideSuccessCheckPerHit(BattleMove move, Battler user, Battler target) {
            return Battle.NEWEST_BATTLE_MECHANICS && statusMove(move) && user.pbHasType("POISON");   // :94
        }
    }

    /** {@code class PokeBattle_Move_007 < PokeBattle_ParalysisMove} (Move_Effects_000-07F.rb:105-119): paralyzes (Thunder Wave/Body Slam). */
    public static class PokeBattle_Move_007 extends MoveEffectsGeneric.PokeBattle_ParalysisMove {

        /** {@code tramplesMinimize?(param=1)} (:106-110). */
        @Override
        public boolean tramplesMinimize(BattleMove move, int param) {
            if ("BODYSLAM".equals(move.internalName())) {                     // :108 isConst?(@id,PBMoves,:BODYSLAM)
                return Battle.NEWEST_BATTLE_MECHANICS;
            }
            return super.tramplesMinimize(move, param);                       // :109 return super
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:112-118). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if ("THUNDERWAVE".equals(move.internalName())                     // :113 isConst?(@id,PBMoves,:THUNDERWAVE)
                    && PBTypes.ineffective(target.damageState.typeMod)) {     // :113 PBTypes.ineffective?(target.damageState.typeMod)
                user.battle.display("这不能影响" + target.pbThis(true) + "……");   // :114
                return true;                                                  // :115
            }
            return super.pbFailsAgainstTarget(move, user, target);            // :117 return super
        }
    }

    /** {@code class PokeBattle_Move_008 < PokeBattle_ParalysisMove} (Move_Effects_000-07F.rb:127-142): Thunder. */
    public static class PokeBattle_Move_008 extends MoveEffectsGeneric.PokeBattle_ParalysisMove {

        /** {@code hitsFlyingTargets?} (:128). */
        @Override
        public boolean hitsFlyingTargets(BattleMove move) {
            return true;                                                      // :128
        }

        /** {@code pbBaseAccuracy(user,target)} (:130-141). */
        @Override
        public int pbBaseAccuracy(BattleMove move, Battler user, Battler target) {
            if (target.hasUtilityUmbrella()) {                                // :131
                return super.pbBaseAccuracy(move, user, target);              // :132 return super
            }
            int w = user.battle.pbWeather();                                  // :134 case @battle.pbWeather
            if (w == PBWeather.Sun || w == PBWeather.HarshSun) {              // :135
                return 50;                                                    // :136
            }
            if (w == PBWeather.Rain || w == PBWeather.HeavyRain) {            // :137
                return 0;                                                     // :138
            }
            return super.pbBaseAccuracy(move, user, target);                  // :140 return super
        }
    }

    /** {@code class PokeBattle_Move_009 < PokeBattle_Move} (Move_Effects_000-07F.rb:149-161): Thunder Fang. */
    public static class PokeBattle_Move_009 extends MoveEffectBase {

        /** {@code flinchingMove?} (:150). */
        @Override
        public boolean flinchingMove(BattleMove move) {
            return true;                                                      // :150
        }

        /** {@code pbAdditionalEffect(user,target)} (:152-160). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {                              // :153
                return;
            }
            int chance = pbAdditionalEffectChance(move, user, target, 10);    // :154
            if (chance == 0) {                                                // :155
                return;
            }
            if (user.battle.pbRandom(100) < chance) {                         // :156
                if (target.pbCanParalyze(user, false, move)) {                // :157 target.pbCanParalyze?(user,false,self)
                    target.pbParalyze(user, null);                            // :157
                }
            }
            if (user.battle.pbRandom(100) < chance) {                         // :159
                target.pbFlinch(user);                      // :159 target.pbFlinch(user)
            }
        }
    }

    /** {@code class PokeBattle_Move_00A < PokeBattle_BurnMove} (Move_Effects_000-07F.rb:168-169): burns the target. */
    public static class PokeBattle_Move_00A extends MoveEffectsGeneric.PokeBattle_BurnMove {
    }

    /** {@code class PokeBattle_Move_00B < PokeBattle_Move} (Move_Effects_000-07F.rb:176-188): Fire Fang. */
    public static class PokeBattle_Move_00B extends MoveEffectBase {

        /** {@code flinchingMove?} (:177). */
        @Override
        public boolean flinchingMove(BattleMove move) {
            return true;                                                      // :177
        }

        /** {@code pbAdditionalEffect(user,target)} (:179-187). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {                              // :180
                return;
            }
            int chance = pbAdditionalEffectChance(move, user, target, 10);    // :181
            if (chance == 0) {                                                // :182
                return;
            }
            if (user.battle.pbRandom(100) < chance) {                         // :183
                if (target.pbCanBurn(user, false, move)) {                    // :184 target.pbCanBurn?(user,false,self)
                    target.pbBurn(user, null);                                // :184
                }
            }
            if (user.battle.pbRandom(100) < chance) {                         // :186
                target.pbFlinch(user);                      // :186
            }
        }
    }

    /** {@code class PokeBattle_Move_00C < PokeBattle_FreezeMove} (Move_Effects_000-07F.rb:195-196): freezes the target. */
    public static class PokeBattle_Move_00C extends MoveEffectsGeneric.PokeBattle_FreezeMove {
    }

    /** {@code class PokeBattle_Move_00D < PokeBattle_FreezeMove} (Move_Effects_000-07F.rb:203-208): Blizzard. */
    public static class PokeBattle_Move_00D extends MoveEffectsGeneric.PokeBattle_FreezeMove {

        /** {@code pbBaseAccuracy(user,target)} (:204-207). */
        @Override
        public int pbBaseAccuracy(BattleMove move, Battler user, Battler target) {
            int w = user.battle.pbWeather();                                  // :205
            if (w == PBWeather.Hail || w == PBWeather.Snow) {                 // :205
                return 0;
            }
            return super.pbBaseAccuracy(move, user, target);                  // :206 return super
        }
    }

    /** {@code class PokeBattle_Move_00E < PokeBattle_Move} (Move_Effects_000-07F.rb:215-227): Ice Fang. */
    public static class PokeBattle_Move_00E extends MoveEffectBase {

        /** {@code flinchingMove?} (:216). */
        @Override
        public boolean flinchingMove(BattleMove move) {
            return true;                                                      // :216
        }

        /** {@code pbAdditionalEffect(user,target)} (:218-226). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {                              // :219
                return;
            }
            int chance = pbAdditionalEffectChance(move, user, target, 10);    // :220
            if (chance == 0) {                                                // :221
                return;
            }
            if (user.battle.pbRandom(100) < chance) {                         // :222
                if (target.pbCanFreeze(user, false, move)) {   // :223 target.pbCanFreeze?(user,false,self)
                    target.pbFreeze();                        // :223 target.pbFreeze
                }
            }
            if (user.battle.pbRandom(100) < chance) {                         // :225
                target.pbFlinch(user);                      // :225
            }
        }
    }

    /** {@code class PokeBattle_Move_00F < PokeBattle_FlinchMove} (Move_Effects_000-07F.rb:234-235): causes flinching. */
    public static class PokeBattle_Move_00F extends MoveEffectsGeneric.PokeBattle_FlinchMove {
    }

    /** {@code class PokeBattle_Move_010 < PokeBattle_FlinchMove} (Move_Effects_000-07F.rb:243-250): Dragon Rush/Steamroller/Stomp. */
    public static class PokeBattle_Move_010 extends MoveEffectsGeneric.PokeBattle_FlinchMove {

        /** {@code tramplesMinimize?(param=1)} (:244-249). */
        @Override
        public boolean tramplesMinimize(BattleMove move, int param) {
            if ("DRAGONRUSH".equals(move.internalName()) && !Battle.NEWEST_BATTLE_MECHANICS) {   // :245
                return super.tramplesMinimize(move, param);                   // :245 return super
            }
            if (param == 1 && Battle.NEWEST_BATTLE_MECHANICS) {               // :246
                return true;                                                  // :246
            }
            if (param == 2) {                                                 // :247
                return true;                                                  // :247
            }
            return super.tramplesMinimize(move, param);                       // :248 return super
        }
    }

    /** {@code class PokeBattle_Move_011 < PokeBattle_FlinchMove} (Move_Effects_000-07F.rb:257-267): Snore. */
    public static class PokeBattle_Move_011 extends MoveEffectsGeneric.PokeBattle_FlinchMove {

        /** {@code usableWhenAsleep?} (:258). */
        @Override
        public boolean usableWhenAsleep(BattleMove move) {
            return true;                                                      // :258
        }

        /** {@code pbMoveFailed?(user,targets)} (:260-266). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.asleep()) {                                             // :261
                user.battle.display("但是失败了！");                             // :262
                return true;                                                  // :263
            }
            return false;                                                     // :265
        }
    }

    /** {@code class PokeBattle_Move_012 < PokeBattle_FlinchMove} (Move_Effects_000-07F.rb:275-283): Fake Out. */
    public static class PokeBattle_Move_012 extends MoveEffectsGeneric.PokeBattle_FlinchMove {

        /** {@code pbMoveFailed?(user,targets)} (:276-282). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.turnCount > 1 || user.lastRoundMoved >= 0) {             // :277
                user.battle.display("但是失败了！");                             // :278
                return true;                                                  // :279
            }
            return false;                                                     // :281
        }
    }

    /** {@code class PokeBattle_Move_013 < PokeBattle_ConfuseMove} (Move_Effects_000-07F.rb:290-291): confuses the target. */
    public static class PokeBattle_Move_013 extends MoveEffectsGeneric.PokeBattle_ConfuseMove {
    }

    /** {@code class PokeBattle_Move_014 < PokeBattle_ConfuseMove} (Move_Effects_000-07F.rb:299-312): Chatter. */
    public static class PokeBattle_Move_014 extends MoveEffectsGeneric.PokeBattle_ConfuseMove {

        /** {@code @chatterChance} (:301). */
        private int chatterChance;

        /** {@code pbOnStartUse(user,targets)} (:300-306). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            chatterChance = 0;                                                // :301
            // 登记: Move_Effects_000-07F.rb:302-305 user.pokemon.chatter.intensity ——
            //       本运行时未建模录音/叫声强度（PokeBattle_Pokemon#chatter），
            //       所以 @chatterChance 保持 0，即插件自己写的「没有录音时 0%」分支。
        }

        /** {@code addlEffect} (:308-311). */
        @Override
        public int addlEffect(BattleMove move) {
            if (!Battle.NEWEST_BATTLE_MECHANICS) {                            // :309
                return chatterChance;
            }
            return super.addlEffect(move);                                    // :310 return super
        }
    }

    /** {@code class PokeBattle_Move_015 < PokeBattle_ConfuseMove} (Move_Effects_000-07F.rb:320-335): Hurricane. */
    public static class PokeBattle_Move_015 extends MoveEffectsGeneric.PokeBattle_ConfuseMove {

        /** {@code hitsFlyingTargets?} (:321). */
        @Override
        public boolean hitsFlyingTargets(BattleMove move) {
            return true;                                                      // :321
        }

        /** {@code pbBaseAccuracy(user,target)} (:323-334). */
        @Override
        public int pbBaseAccuracy(BattleMove move, Battler user, Battler target) {
            if (target.hasUtilityUmbrella()) {                                // :324
                return super.pbBaseAccuracy(move, user, target);              // :325 return super
            }
            int w = user.battle.pbWeather();                                  // :327 case @battle.pbWeather
            if (w == PBWeather.Sun || w == PBWeather.HarshSun) {              // :328
                return 50;                                                    // :329
            }
            if (w == PBWeather.Rain || w == PBWeather.HeavyRain) {            // :330
                return 0;                                                     // :331
            }
            return super.pbBaseAccuracy(move, user, target);                  // :333 return super
        }
    }

    /** {@code class PokeBattle_Move_016 < PokeBattle_Move} (Move_Effects_000-07F.rb:342-361): Attract. */
    public static class PokeBattle_Move_016 extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:343). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                      // :343
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:345-350). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {                                         // :346
                return false;
            }
            if (!target.pbCanAttract(user, true)) {                           // :347 target.pbCanAttract?(user)
                return true;
            }
            if (pbMoveFailedAromaVeil(move, user, target, true)) {            // :348
                return true;
            }
            return false;                                                     // :349
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:352-355). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {                                         // :353
                return;
            }
            target.pbAttract(user, null);                                     // :354
        }

        /** {@code pbAdditionalEffect(user,target)} (:357-360). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {                              // :358
                return;
            }
            if (target.pbCanAttract(user, false)) {                           // :359 target.pbCanAttract?(user,false)
                target.pbAttract(user, null);                                 // :359
            }
        }
    }

    /** {@code class PokeBattle_Move_017 < PokeBattle_Move} (Move_Effects_000-07F.rb:368-377): Tri Attack. */
    public static class PokeBattle_Move_017 extends MoveEffectBase {

        /** {@code pbAdditionalEffect(user,target)} (:369-376). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {                              // :370
                return;
            }
            switch (user.battle.pbRandom(3)) {                                // :371 case @battle.pbRandom(3)
                case 0:                                                       // :372
                    if (target.pbCanBurn(user, false, move)) {
                        target.pbBurn(user, null);
                    }
                    break;
                case 1:                                                       // :373
                    if (target.pbCanFreeze(user, false, move)) {
                        target.pbFreeze();
                    }
                    break;
                case 2:                                                       // :374
                    if (target.pbCanParalyze(user, false, move)) {
                        target.pbParalyze(user, null);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /** {@code class PokeBattle_Move_018 < PokeBattle_Move} (Move_Effects_000-07F.rb:384-407): Refresh. */
    public static class PokeBattle_Move_018 extends MoveEffectBase {

        /**
         * {@code pbMoveFailed?(user,targets)} (:385-393).
         *
         * <p>Reopened by the later {@code Arceus} section (Arceus:233-244): its body
         * wins, so the Frostbite/Drowsy checks are the effective version.</p>
         */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.hasStatus("BURN")                                   // :386 user.status!=PBStatuses::BURN
                    && !user.hasStatus("POISON")                          // :387
                    && !user.hasStatus("PARALYSIS")                       // :388
                    && !user.hasStatus("FROSTBITE")                       // Arceus:238
                    && !user.hasStatus("DROWSY")) {                       // Arceus:239
                user.battle.display("但是失败了！");                             // :389
                return true;                                                  // :390
            }
            return false;                                                     // :392
        }

        /** {@code pbEffectGeneral(user)} (:395-406); reopened by {@code Arceus:246-261} (wins). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            String t = user.status;                                           // :396
            user.pbCureStatus(false);                                         // :397
            if ("BURN".equals(t)) {                                           // :399 when PBStatuses::BURN
                user.battle.display(user.pbThis() + "治愈了灼伤！");              // :400
            } else if ("POISON".equals(t)) {                                  // :401
                user.battle.display(user.pbThis() + "治愈了毒！");                // :402
            } else if ("PARALYSIS".equals(t)) {                               // :403
                user.battle.display(user.pbThis() + "治愈了麻痹！");              // :404
            } else if ("FROSTBITE".equals(t)) {                               // Arceus:256
                user.battle.display(user.pbThis() + "治愈了它的冻伤！");           // Arceus:257
            } else if ("DROWSY".equals(t)) {                                  // Arceus:258
                user.battle.display(user.pbThis() + "不再打瞌睡了！");             // Arceus:259
            }
        }
    }

    /** {@code class PokeBattle_Move_019 < PokeBattle_Move} (Move_Effects_000-07F.rb:421-504): Aromatherapy / Heal Bell. */
    public static class PokeBattle_Move_019 extends MoveEffectBase {

        /** {@code worksWithNoTargets?} (:422). */
        @Override
        public boolean worksWithNoTargets(BattleMove move) {
            return true;                                                      // :422
        }

        /** {@code pbMoveFailed?(user,targets)} (:424-443). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            boolean failed = true;                                            // :425
            for (Battler b : user.battle.eachSameSideBattler(user.index)) {   // :426 @battle.eachSameSideBattler(user)
                if (b.status.isEmpty()) {                                     // :427 b.status==PBStatuses::NONE
                    continue;
                }
                failed = false;                                               // :428
                break;                                                        // :429
            }
            if (!failed) {                                                    // :431 if !failed
                for (Battler b : user.battle.partyOf(user.index)) {           // :432 @battle.pbParty(user.index)
                    if (b == null || !able(b) || b.status.isEmpty()) {        // :433 !pkmn || !pkmn.able? || pkmn.status==PBStatuses::NONE
                        continue;
                    }
                    failed = false;                                           // :434
                    break;                                                    // :435
                }
            }
            if (failed) {                                                     // :438
                user.battle.display("但是失败了！");                             // :439
                return true;                                                  // :440
            }
            return false;                                                     // :442
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:445-447). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            return target.status.isEmpty();                                   // :446 target.status==PBStatuses::NONE
        }

        /**
         * {@code pbAromatherapyHeal(pkmn,battler=nil)} (:449-470); reopened by
         * {@code Arceus:269-294} (wins - it adds the Frostbite/Drowsy branches).
         */
        @Override
        public void pbAromatherapyHeal(BattleMove move, Pokemon pkmn, Battler battler) {
            String oldStatus = battler != null ? battler.status : pkmn.status;   // :450
            String curedName = battler != null ? battler.pbThis() : pkmn.name;   // :451
            if (battler != null) {                                            // :452
                battler.pbCureStatus(false);                                  // :453
            } else {
                pkmn.status = "";                                             // :455 pkmn.status = PBStatuses::NONE
                setPokemonStatusCount(pkmn, 0);                               // :456
            }
            if ("SLEEP".equals(oldStatus)) {                                  // :459 when PBStatuses::SLEEP
                battler.battle.display(curedName + "从睡梦中醒来了！");           // :460
            } else if ("POISON".equals(oldStatus)) {                          // :461
                battler.battle.display(curedName + "的毒被消去了！");             // :462
            } else if ("BURN".equals(oldStatus)) {                            // :463
                battler.battle.display(curedName + "的灼伤被治愈了！");           // :464
            } else if ("PARALYSIS".equals(oldStatus)) {                       // :465
                battler.battle.display(curedName + "的麻痹被解除了！");           // :466
            } else if ("FROZEN".equals(oldStatus)) {                          // :467
                battler.battle.display(curedName + "不再被冰冻了");              // :468
            } else if ("FROSTBITE".equals(oldStatus)) {                       // Arceus:289
                battler.battle.display(curedName + "'s frostbite was healed.");   // Arceus:290
            } else if ("DROWSY".equals(oldStatus)) {                          // Arceus:291
                battler.battle.display(curedName + " was pulled out of drowsiness.");   // Arceus:292
            }
        }

        /**
         * The {@code battler=nil} form (:449).
         *
         * <p>登记: this hook carries no battle handle, while the Ruby body
         * displays through {@code @battle}; in this runtime the party members are
         * {@code Battler}s ({@code Battle.partyOf}), so
         * {@link #pbEffectGeneral} passes the battler form and this overload is
         * only reachable for a Pokemon without a battler - there the two state
         * writes are done and the display is registered.</p>
         */
        @Override
        public void pbAromatherapyHeal(BattleMove move, Pokemon pkmn) {
            String oldStatus = pkmn.status;                                   // :450
            pkmn.status = "";                                                 // :455
            setPokemonStatusCount(pkmn, 0);                                   // :456
            // 登记: Move_Effects_000-07F.rb:458-469 的 @battle.pbDisplay —— 本重载没有
            //       battle 句柄（Ruby 用 @battle），状态改动已照做，文案未发。
            if (oldStatus.isEmpty()) {
                return;
            }
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:472-475). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            pbAromatherapyHeal(move, target.pokemon, target);                 // :474
        }

        /** {@code pbEffectGeneral(user)} (:477-494). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            if (pbTarget(move, user) != PBTargets.UserAndAllies) {            // :480 pbTarget(user)!=PBTargets::UserAndAllies
                for (Battler b : user.battle.eachSameSideBattler(user.index)) {   // :481
                    if (b.status.isEmpty()) {                                 // :482
                        continue;
                    }
                    pbAromatherapyHeal(move, b.pokemon, b);                   // :483
                }
            }
            int i = 0;                                                        // :489 each_with_index
            for (Battler b : user.battle.partyOf(user.index)) {               // :489 @battle.pbParty(user.index)
                if (b == null || !able(b) || b.status.isEmpty()) {            // :490
                    i++;
                    continue;
                }
                if (pbFindBattler(user.battle, i, user) != null) {            // :491 next if @battle.pbFindBattler(i,user)
                    i++;
                    continue;
                }
                pbAromatherapyHeal(move, b.pokemon, b);                       // :492
                i++;
            }
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} (:496-503). */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets, int hitNum,
                                    boolean showAnimation) {
            super.pbShowAnimation(move, id, user, targets, hitNum, showAnimation);   // :497 super
            if ("AROMATHERAPY".equals(move.internalName())) {                 // :498 isConst?(@id,PBMoves,:AROMATHERAPY)
                user.battle.display("沁人心脾的香气在场地上扩散开了！");            // :499
            } else if ("HEALBELL".equals(move.internalName())) {              // :500
                user.battle.display("铃声响起！");                              // :501
            }
        }
    }

    /** {@code class PokeBattle_Move_01A < PokeBattle_Move} (Move_Effects_000-07F.rb:512-525): Safeguard. */
    public static class PokeBattle_Move_01A extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:513-519). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.pbOwnSide().effects.intVal(PBEffects.Side.Safeguard) > 0) {   // :514
                user.battle.display("但是失败了！");                             // :515
                return true;                                                  // :516
            }
            return false;                                                     // :518
        }

        /** {@code pbEffectGeneral(user)} (:521-524). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.pbOwnSide().effects.set(PBEffects.Side.Safeguard, 5);        // :522
            user.battle.display(user.pbTeam(false) + "被包围在了白雾之中！");     // :523
        }
    }

    /** {@code class PokeBattle_Move_01B < PokeBattle_Move} (Move_Effects_000-07F.rb:532-573): Psycho Shift. */
    public static class PokeBattle_Move_01B extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:533-539). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.statused()) {                                           // :534 user.status==0
                user.battle.display("但是失败了！");                             // :535
                return true;                                                  // :536
            }
            return false;                                                     // :538
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:541-547). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (!target.pbCanInflictStatus(PBStatuses.idOf(user.status), user, false, move, false)) {   // :542
                user.battle.display("但是失败了！");                             // :543
                return true;                                                  // :544
            }
            return false;                                                     // :546
        }

        /**
         * {@code pbEffectAgainstTarget(user,target)} (:549-572); reopened by
         * {@code Arceus:302-330} (wins - it adds the Frostbite/Drowsy branches).
         */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            String msg = "";                                                  // :550
            if (user.hasStatus("SLEEP")) {                                    // :552 when PBStatuses::SLEEP
                target.pbSleep();                             // :553 target.pbSleep
                msg = user.pbThis() + "醒来了！";                              // :554
            } else if (user.hasStatus("POISON")) {                            // :555
                target.pbPoison(user, null, user.statusCount != 0);           // :556
                msg = user.pbThis() + "的毒被消去了！";                          // :557
            } else if (user.hasStatus("BURN")) {                              // :558
                target.pbBurn(user, null);                                    // :559
                msg = user.pbThis() + "的灼伤被治愈了！";                        // :560
            } else if (user.hasStatus("PARALYSIS")) {                         // :561
                target.pbParalyze(user, null);                                // :562
                msg = user.pbThis() + "的麻痹被解除了！";                        // :563
            } else if (user.hasStatus("FROZEN")) {                            // :564
                target.pbFreeze();                            // :565 target.pbFreeze
                msg = user.pbThis() + "不再被冰冻了";                           // :566
            } else if (user.hasStatus("FROSTBITE")) {                         // Arceus:320
                PendingApi.pbFrostbite(target, null);                         // Arceus:321 target.pbFrostbite(user)
                msg = user.pbThis() + "'s frostbite was healed.";             // Arceus:322
            } else if (user.hasStatus("DROWSY")) {                            // Arceus:323
                PendingApi.pbDrowse(target, null);                            // Arceus:324 target.pbDrowse(user)
                msg = user.pbThis() + " is no longer drowsy.";                // Arceus:325
            }
            if (!msg.isEmpty()) {                                             // :568
                user.pbCureStatus(false);                                     // :569
                user.battle.display(msg);                                     // :570
            }
        }
    }

    /** {@code class PokeBattle_Move_01C < PokeBattle_StatUpMove} (Move_Effects_000-07F.rb:580-585): +1 Attack. */
    public static class PokeBattle_Move_01C extends MoveEffectsGeneric.PokeBattle_StatUpMove {

        /** {@code initialize} (:581-584): {@code @statUp = [PBStats::ATTACK,1]}. */
        public PokeBattle_Move_01C() {
            statUp = new int[] {PBStats.ATTACK, 1};                           // :583
        }
    }

    /** {@code class PokeBattle_Move_01D < PokeBattle_StatUpMove} (Move_Effects_000-07F.rb:592-597): +1 Defense. */
    public static class PokeBattle_Move_01D extends MoveEffectsGeneric.PokeBattle_StatUpMove {

        /** {@code initialize} (:593-596): {@code @statUp = [PBStats::DEFENSE,1]}. */
        public PokeBattle_Move_01D() {
            statUp = new int[] {PBStats.DEFENSE, 1};                          // :595
        }
    }

    /** {@code class PokeBattle_Move_01E < PokeBattle_StatUpMove} (Move_Effects_000-07F.rb:604-614): Defense Curl. */
    public static class PokeBattle_Move_01E extends MoveEffectsGeneric.PokeBattle_StatUpMove {

        /** {@code initialize} (:605-608): {@code @statUp = [PBStats::DEFENSE,1]}. */
        public PokeBattle_Move_01E() {
            statUp = new int[] {PBStats.DEFENSE, 1};                          // :607
        }

        /** {@code pbEffectGeneral(user)} (:610-613). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.DefenseCurl, true);            // :611
            super.pbEffectGeneral(move, user);                                // :612 super
        }
    }

    /** {@code class PokeBattle_Move_01F < PokeBattle_StatUpMove} (Move_Effects_000-07F.rb:621-626): +1 Speed. */
    public static class PokeBattle_Move_01F extends MoveEffectsGeneric.PokeBattle_StatUpMove {

        /** {@code initialize} (:622-625): {@code @statUp = [PBStats::SPEED,1]}. */
        public PokeBattle_Move_01F() {
            statUp = new int[] {PBStats.SPEED, 1};                            // :624
        }
    }

    /** {@code class PokeBattle_Move_020 < PokeBattle_StatUpMove} (Move_Effects_000-07F.rb:633-638): +1 Sp. Atk. */
    public static class PokeBattle_Move_020 extends MoveEffectsGeneric.PokeBattle_StatUpMove {

        /** {@code initialize} (:634-637): {@code @statUp = [PBStats::SPATK,1]}. */
        public PokeBattle_Move_020() {
            statUp = new int[] {PBStats.SPATK, 1};                            // :636
        }
    }

    /** {@code class PokeBattle_Move_021 < PokeBattle_StatUpMove} (Move_Effects_000-07F.rb:646-657): Charge. */
    public static class PokeBattle_Move_021 extends MoveEffectsGeneric.PokeBattle_StatUpMove {

        /** {@code initialize} (:647-650): {@code @statUp = [PBStats::SPDEF,1]}. */
        public PokeBattle_Move_021() {
            statUp = new int[] {PBStats.SPDEF, 1};                            // :649
        }

        /** {@code pbEffectGeneral(user)} (:652-656). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.Charge, 2);                    // :653
            user.battle.display(user.pbThis() + "开始充电！");                  // :654
            super.pbEffectGeneral(move, user);                                // :655 super
        }
    }

    /** {@code class PokeBattle_Move_022 < PokeBattle_StatUpMove} (Move_Effects_000-07F.rb:664-669): +1 evasion. */
    public static class PokeBattle_Move_022 extends MoveEffectsGeneric.PokeBattle_StatUpMove {

        /** {@code initialize} (:665-668): {@code @statUp = [PBStats::EVASION,1]}. */
        public PokeBattle_Move_022() {
            statUp = new int[] {PBStats.EVASION, 1};                          // :667
        }
    }

    /** {@code class PokeBattle_Move_023 < PokeBattle_Move} (Move_Effects_000-07F.rb:676-689): Focus Energy. */
    public static class PokeBattle_Move_023 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:677-683). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.effects.intVal(PBEffects.Battler.FocusEnergy) >= 2) {    // :678
                user.battle.display("但是失败了！");                             // :679
                return true;                                                  // :680
            }
            return false;                                                     // :682
        }

        /** {@code pbEffectGeneral(user)} (:685-688). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.FocusEnergy, 2);               // :686
            user.battle.display(user.pbThis() + "变得兴奋了！");                 // :687
        }
    }

    /** {@code class PokeBattle_Move_024 < PokeBattle_MultiStatUpMove} (Move_Effects_000-07F.rb:696-701): Bulk Up. */
    public static class PokeBattle_Move_024 extends MoveEffectsGeneric.PokeBattle_MultiStatUpMove {

        /** {@code initialize} (:697-700): {@code @statUp = [PBStats::ATTACK,1,PBStats::DEFENSE,1]}. */
        public PokeBattle_Move_024() {
            statUp = new int[] {PBStats.ATTACK, 1, PBStats.DEFENSE, 1};       // :699
        }
    }

    /** {@code class PokeBattle_Move_025 < PokeBattle_MultiStatUpMove} (Move_Effects_000-07F.rb:708-717): Coil. */
    public static class PokeBattle_Move_025 extends MoveEffectsGeneric.PokeBattle_MultiStatUpMove {

        /** {@code initialize} (:709-712): {@code @statUp = [ATTACK,1,DEFENSE,1,ACCURACY,1]}. */
        public PokeBattle_Move_025() {
            statUp = new int[] {PBStats.ATTACK, 1, PBStats.DEFENSE, 1, PBStats.ACCURACY, 1};   // :711
        }

        /** {@code pbEffectGeneral(user)} (:713-716). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            super.pbEffectGeneral(move, user);                                // :714 super
            user.effects.set(PBEffects.Battler.UsedCoil, true);               // :715
        }
    }

    /** {@code class PokeBattle_Move_026 < PokeBattle_MultiStatUpMove} (Move_Effects_000-07F.rb:724-729): Dragon Dance. */
    public static class PokeBattle_Move_026 extends MoveEffectsGeneric.PokeBattle_MultiStatUpMove {

        /** {@code initialize} (:725-728): {@code @statUp = [PBStats::ATTACK,1,PBStats::SPEED,1]}. */
        public PokeBattle_Move_026() {
            statUp = new int[] {PBStats.ATTACK, 1, PBStats.SPEED, 1};         // :727
        }
    }

    /** {@code class PokeBattle_Move_027 < PokeBattle_MultiStatUpMove} (Move_Effects_000-07F.rb:736-741): Work Up. */
    public static class PokeBattle_Move_027 extends MoveEffectsGeneric.PokeBattle_MultiStatUpMove {

        /** {@code initialize} (:737-740): {@code @statUp = [PBStats::ATTACK,1,PBStats::SPATK,1]}. */
        public PokeBattle_Move_027() {
            statUp = new int[] {PBStats.ATTACK, 1, PBStats.SPATK, 1};         // :739
        }
    }

    /** {@code class PokeBattle_Move_028 < PokeBattle_MultiStatUpMove} (Move_Effects_000-07F.rb:749-769): Growth. */
    public static class PokeBattle_Move_028 extends MoveEffectsGeneric.PokeBattle_MultiStatUpMove {

        /** {@code initialize} (:750-754): {@code @statUp = [PBStats::ATTACK,1,PBStats::SPATK,1]}. */
        public PokeBattle_Move_028() {
            statUp = new int[] {PBStats.ATTACK, 1, PBStats.SPATK, 1};         // :753
        }

        /** {@code pbOnStartUse(user,targets)} (:756-768). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            int increment = 1;                                                // :757
            if (user.hasActiveAbility("SUPERSUN")                             // :760 user.hasActiveAbility?(:SUPERSUN)
                    || ((user.battle.pbWeather() == PBWeather.Sun             // :761 @battle.pbWeather == PBWeather::Sun
                    || user.battle.pbWeather() == PBWeather.HarshSun)         // :761
                    && !user.hasUtilityUmbrella())) {                         // :762
                increment = 2;                                                // :763
            }
            statUp[1] = increment;                                            // :767 @statUp[1] = @statUp[3] = increment
            statUp[3] = increment;                                            // :767
        }
    }

    /** {@code class PokeBattle_Move_029 < PokeBattle_MultiStatUpMove} (Move_Effects_000-07F.rb:775-780): Hone Claws. */
    public static class PokeBattle_Move_029 extends MoveEffectsGeneric.PokeBattle_MultiStatUpMove {

        /** {@code initialize} (:776-779): {@code @statUp = [PBStats::ATTACK,1,PBStats::ACCURACY,1]}. */
        public PokeBattle_Move_029() {
            statUp = new int[] {PBStats.ATTACK, 1, PBStats.ACCURACY, 1};      // :778
        }
    }

    /** {@code class PokeBattle_Move_02A < PokeBattle_MultiStatUpMove} (Move_Effects_000-07F.rb:788-793): Cosmic Power/Defend Order. */
    public static class PokeBattle_Move_02A extends MoveEffectsGeneric.PokeBattle_MultiStatUpMove {

        /** {@code initialize} (:789-792): {@code @statUp = [PBStats::DEFENSE,1,PBStats::SPDEF,1]}. */
        public PokeBattle_Move_02A() {
            statUp = new int[] {PBStats.DEFENSE, 1, PBStats.SPDEF, 1};        // :791
        }
    }

    /** {@code class PokeBattle_Move_02B < PokeBattle_MultiStatUpMove} (Move_Effects_000-07F.rb:801-806): Quiver Dance. */
    public static class PokeBattle_Move_02B extends MoveEffectsGeneric.PokeBattle_MultiStatUpMove {

        /** {@code initialize} (:802-805): {@code @statUp = [SPATK,1,SPDEF,1,SPEED,1]}. */
        public PokeBattle_Move_02B() {
            statUp = new int[] {PBStats.SPATK, 1, PBStats.SPDEF, 1, PBStats.SPEED, 1};   // :804
        }
    }

    /** {@code class PokeBattle_Move_02C < PokeBattle_MultiStatUpMove} (Move_Effects_000-07F.rb:813-818): Calm Mind. */
    public static class PokeBattle_Move_02C extends MoveEffectsGeneric.PokeBattle_MultiStatUpMove {

        /** {@code initialize} (:814-817): {@code @statUp = [PBStats::SPATK,1,PBStats::SPDEF,1]}. */
        public PokeBattle_Move_02C() {
            statUp = new int[] {PBStats.SPATK, 1, PBStats.SPDEF, 1};          // :816
        }
    }

    /** {@code class PokeBattle_Move_02D < PokeBattle_MultiStatUpMove} (Move_Effects_000-07F.rb:826-833): Ancient Power etc. */
    public static class PokeBattle_Move_02D extends MoveEffectsGeneric.PokeBattle_MultiStatUpMove {

        /** {@code initialize} (:827-832): {@code @statUp = [ATTACK,1,DEFENSE,1,SPATK,1,SPDEF,1,SPEED,1]}. */
        public PokeBattle_Move_02D() {
            statUp = new int[] {PBStats.ATTACK, 1, PBStats.DEFENSE, 1,           // :829
                    PBStats.SPATK, 1, PBStats.SPDEF, 1,                          // :830
                    PBStats.SPEED, 1};                                           // :831
        }
    }

    /** {@code class PokeBattle_Move_02E < PokeBattle_StatUpMove} (Move_Effects_000-07F.rb:840-845): +2 Attack. */
    public static class PokeBattle_Move_02E extends MoveEffectsGeneric.PokeBattle_StatUpMove {

        /** {@code initialize} (:841-844): {@code @statUp = [PBStats::ATTACK,2]}. */
        public PokeBattle_Move_02E() {
            statUp = new int[] {PBStats.ATTACK, 2};                           // :843
        }
    }

    /** {@code class PokeBattle_Move_02F < PokeBattle_StatUpMove} (Move_Effects_000-07F.rb:852-857): +2 Defense. */
    public static class PokeBattle_Move_02F extends MoveEffectsGeneric.PokeBattle_StatUpMove {

        /** {@code initialize} (:853-856): {@code @statUp = [PBStats::DEFENSE,2]}. */
        public PokeBattle_Move_02F() {
            statUp = new int[] {PBStats.DEFENSE, 2};                          // :855
        }
    }

    /** {@code class PokeBattle_Move_030 < PokeBattle_StatUpMove} (Move_Effects_000-07F.rb:864-869): +2 Speed. */
    public static class PokeBattle_Move_030 extends MoveEffectsGeneric.PokeBattle_StatUpMove {

        /** {@code initialize} (:865-868): {@code @statUp = [PBStats::SPEED,2]}. */
        public PokeBattle_Move_030() {
            statUp = new int[] {PBStats.SPEED, 2};                            // :867
        }
    }

    /** {@code class PokeBattle_Move_031 < PokeBattle_StatUpMove} (Move_Effects_000-07F.rb:877-890): Autotomize. */
    public static class PokeBattle_Move_031 extends MoveEffectsGeneric.PokeBattle_StatUpMove {

        /** {@code initialize} (:878-881): {@code @statUp = [PBStats::SPEED,2]}. */
        public PokeBattle_Move_031() {
            statUp = new int[] {PBStats.SPEED, 2};                            // :880
        }

        /** {@code pbEffectGeneral(user)} (:883-889). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            if (pbWeight(user) + user.effects.intVal(PBEffects.Battler.WeightChange) > 1) {   // :884
                user.effects.add(PBEffects.Battler.WeightChange, -1000);      // :885
                user.battle.display(user.pbThis() + "变得灵活了！");              // :886
            }
            super.pbEffectGeneral(move, user);                                // :888 super
        }
    }

    /** {@code class PokeBattle_Move_032 < PokeBattle_StatUpMove} (Move_Effects_000-07F.rb:897-902): +2 Sp. Atk. */
    public static class PokeBattle_Move_032 extends MoveEffectsGeneric.PokeBattle_StatUpMove {

        /** {@code initialize} (:898-901): {@code @statUp = [PBStats::SPATK,2]}. */
        public PokeBattle_Move_032() {
            statUp = new int[] {PBStats.SPATK, 2};                            // :900
        }
    }

    /** {@code class PokeBattle_Move_033 < PokeBattle_StatUpMove} (Move_Effects_000-07F.rb:909-914): +2 Sp. Def. */
    public static class PokeBattle_Move_033 extends MoveEffectsGeneric.PokeBattle_StatUpMove {

        /** {@code initialize} (:910-913): {@code @statUp = [PBStats::SPDEF,2]}. */
        public PokeBattle_Move_033() {
            statUp = new int[] {PBStats.SPDEF, 2};                            // :912
        }
    }

    /** {@code class PokeBattle_Move_034 < PokeBattle_StatUpMove} (Move_Effects_000-07F.rb:921-931): Minimize. */
    public static class PokeBattle_Move_034 extends MoveEffectsGeneric.PokeBattle_StatUpMove {

        /** {@code initialize} (:922-925): {@code @statUp = [PBStats::EVASION,2]}. */
        public PokeBattle_Move_034() {
            statUp = new int[] {PBStats.EVASION, 2};                          // :924
        }

        /** {@code pbEffectGeneral(user)} (:927-930). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.Minimize, true);               // :928
            super.pbEffectGeneral(move, user);                                // :929 super
        }
    }

    /** {@code class PokeBattle_Move_035 < PokeBattle_Move} (Move_Effects_000-07F.rb:940-982): Shell Smash. */
    public static class PokeBattle_Move_035 extends MoveEffectBase {

        /** {@code @statUp = [PBStats::ATTACK,2,PBStats::SPATK,2,PBStats::SPEED,2]} (:943). */
        private final int[] statUp = {PBStats.ATTACK, 2, PBStats.SPATK, 2, PBStats.SPEED, 2};
        /** {@code @statDown = [PBStats::DEFENSE,1,PBStats::SPDEF,1]} (:944). */
        private final int[] statDown = {PBStats.DEFENSE, 1, PBStats.SPDEF, 1};

        /** {@code pbMoveFailed?(user,targets)} (:947-964). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            boolean failed = true;                                            // :948
            for (int i = 0; i < statUp.length / 2; i++) {                     // :949
                if (user.pbCanRaiseStatStage(statUp[i * 2], user, move, false)) {   // :950
                    failed = false;                                           // :951
                    break;
                }
            }
            for (int i = 0; i < statDown.length / 2; i++) {                   // :954
                if (user.pbCanLowerStatStage(statDown[i * 2], user, move, false)) {   // :955
                    failed = false;                                           // :956
                    break;
                }
            }
            if (failed) {                                                     // :959
                user.battle.display(user.pbThis() + "的能力已经无法再变化了！");     // :960
                return true;                                                  // :961
            }
            return false;                                                     // :963
        }

        /** {@code pbEffectGeneral(user)} (:966-981). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            boolean showAnim = true;                                          // :967
            for (int i = 0; i < statDown.length / 2; i++) {                   // :968
                if (!user.pbCanLowerStatStage(statDown[i * 2], user, move, false)) {   // :969
                    continue;
                }
                if (user.pbLowerStatStage(statDown[i * 2], statDown[i * 2 + 1], user, showAnim)) {   // :970
                    showAnim = false;                                         // :971
                }
            }
            showAnim = true;                                                  // :974
            for (int i = 0; i < statUp.length / 2; i++) {                     // :975
                if (!user.pbCanRaiseStatStage(statUp[i * 2], user, move, false)) {   // :976
                    continue;
                }
                if (user.pbRaiseStatStage(statUp[i * 2], statUp[i * 2 + 1], user, showAnim)) {   // :977
                    showAnim = false;                                         // :978
                }
            }
        }
    }

    /** {@code class PokeBattle_Move_036 < PokeBattle_MultiStatUpMove} (Move_Effects_000-07F.rb:989-994): Shift Gear. */
    public static class PokeBattle_Move_036 extends MoveEffectsGeneric.PokeBattle_MultiStatUpMove {

        /** {@code initialize} (:990-993): {@code @statUp = [PBStats::SPEED,2,PBStats::ATTACK,1]}. */
        public PokeBattle_Move_036() {
            statUp = new int[] {PBStats.SPEED, 2, PBStats.ATTACK, 1};         // :992
        }
    }

    /** {@code class PokeBattle_Move_037 < PokeBattle_Move} (Move_Effects_000-07F.rb:1001-1018): Acupressure. */
    public static class PokeBattle_Move_037 extends MoveEffectBase {

        /** {@code @statArray} (:1003). */
        private final Array<Integer> statArray = new Array<>();

        /** {@code pbFailsAgainstTarget?(user,target)} (:1002-1012). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            statArray.clear();                                                // :1003 @statArray = []
            for (int s : PBStats.EACH_BATTLE_STAT) {                          // :1004 PBStats.eachBattleStat
                if (target.pbCanRaiseStatStage(s, user, move, false)) {       // :1005 target.pbCanRaiseStatStage?(s,user,self)
                    statArray.add(s);                                         // :1005
                }
            }
            if (statArray.size == 0) {                                        // :1007
                user.battle.display(target.pbThis() + "的能力不能再提升了！");     // :1008
                return true;                                                  // :1009
            }
            return false;                                                     // :1011
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1014-1017). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            int stat = statArray.get(user.battle.pbRandom(statArray.size));   // :1015 @statArray[@battle.pbRandom(@statArray.length)]
            target.pbRaiseStatStage(stat, 2, user);                           // :1016
        }
    }

    /** {@code class PokeBattle_Move_038 < PokeBattle_StatUpMove} (Move_Effects_000-07F.rb:1026-1030): +3 Defense. */
    public static class PokeBattle_Move_038 extends MoveEffectsGeneric.PokeBattle_StatUpMove {

        /** {@code initialize} (:1027-1029): {@code @statUp = [PBStats::DEFENSE,3]}. */
        public PokeBattle_Move_038() {
            statUp = new int[] {PBStats.DEFENSE, 3};                          // :1028
        }
    }

    /** {@code class PokeBattle_Move_039 < PokeBattle_StatUpMove} (Move_Effects_000-07F.rb:1038-1042): +3 Sp. Atk. */
    public static class PokeBattle_Move_039 extends MoveEffectsGeneric.PokeBattle_StatUpMove {

        /** {@code initialize} (:1039-1041): {@code @statUp = [PBStats::SPATK,3]}. */
        public PokeBattle_Move_039() {
            statUp = new int[] {PBStats.SPATK, 3};                            // :1040
        }
    }

    /** {@code class PokeBattle_Move_03A < PokeBattle_Move} (Move_Effects_000-07F.rb:1051-1077): Belly Drum. */
    public static class PokeBattle_Move_03A extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:1052-1059). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            int hpLoss = Math.max(user.maxHp() / 2, 1);                       // :1053 [user.totalhp/2,1].max
            if (user.hp <= hpLoss) {                                          // :1054
                user.battle.display("但是失败了！");                             // :1055
                return true;                                                  // :1056
            }
            if (!user.pbCanRaiseStatStage(PBStats.ATTACK, user, move, true)) {   // :1058
                return true;
            }
            return false;                                                     // :1059
        }

        /** {@code pbEffectGeneral(user)} (:1061-1076). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            int hpLoss = Math.max(user.maxHp() / 2, 1);                       // :1062 [user.totalhp/2,1].max
            user.pbReduceHP(hpLoss, false, true, true);                 // :1063 user.pbReduceHP(hpLoss,false)
            if (user.hasActiveAbility("CONTRARY")) {                          // :1064
                user.setStage(PBStats.ATTACK, -6);                            // :1065 user.stages[PBStats::ATTACK] = -6
                user.battle.commonAnimation("StatDown", user);                // :1066
                user.battle.display(user.pbThis() + "消耗HP使得攻击极大幅度降低！");   // :1067
            } else {
                user.setStage(PBStats.ATTACK, 6);                             // :1069
                addSideStatUps(user, PBStats.ATTACK, 6);                      // :1070
                user.statsRaisedThisRound = true;                             // :1071
                user.battle.commonAnimation("StatUp", user);                  // :1072
                user.battle.display(user.pbThis() + "消耗HP使得攻击极大幅度提升！");   // :1073
            }
            user.pbItemHPHealCheck(0, false);                         // :1075 user.pbItemHPHealCheck
        }
    }

    /** {@code class PokeBattle_Move_03B < PokeBattle_StatDownMove} (Move_Effects_000-07F.rb:1085-1089): Superpower. */
    public static class PokeBattle_Move_03B extends MoveEffectsGeneric.PokeBattle_StatDownMove {

        /** {@code initialize} (:1086-1088): {@code @statDown = [PBStats::ATTACK,1,PBStats::DEFENSE,1]}. */
        public PokeBattle_Move_03B() {
            statDown = new int[] {PBStats.ATTACK, 1, PBStats.DEFENSE, 1};     // :1087
        }
    }

    /** {@code class PokeBattle_Move_03C < PokeBattle_StatDownMove} (Move_Effects_000-07F.rb:1098-1102): Close Combat. */
    public static class PokeBattle_Move_03C extends MoveEffectsGeneric.PokeBattle_StatDownMove {

        /** {@code initialize} (:1099-1101): {@code @statDown = [PBStats::DEFENSE,1,PBStats::SPDEF,1]}. */
        public PokeBattle_Move_03C() {
            statDown = new int[] {PBStats.DEFENSE, 1, PBStats.SPDEF, 1};      // :1100
        }
    }

    /** {@code class PokeBattle_Move_03D < PokeBattle_StatDownMove} (Move_Effects_000-07F.rb:1111-1115): V-create. */
    public static class PokeBattle_Move_03D extends MoveEffectsGeneric.PokeBattle_StatDownMove {

        /** {@code initialize} (:1112-1114): {@code @statDown = [SPEED,1,DEFENSE,1,SPDEF,1]}. */
        public PokeBattle_Move_03D() {
            statDown = new int[] {PBStats.SPEED, 1, PBStats.DEFENSE, 1, PBStats.SPDEF, 1};   // :1113
        }
    }

    /** {@code class PokeBattle_Move_03E < PokeBattle_StatDownMove} (Move_Effects_000-07F.rb:1122-1126): Hammer Arm. */
    public static class PokeBattle_Move_03E extends MoveEffectsGeneric.PokeBattle_StatDownMove {

        /** {@code initialize} (:1123-1125): {@code @statDown = [PBStats::SPEED,1]}. */
        public PokeBattle_Move_03E() {
            statDown = new int[] {PBStats.SPEED, 1};                          // :1124
        }
    }

    /** {@code class PokeBattle_Move_03F < PokeBattle_StatDownMove} (Move_Effects_000-07F.rb:1134-1138): -2 Sp. Atk. */
    public static class PokeBattle_Move_03F extends MoveEffectsGeneric.PokeBattle_StatDownMove {

        /** {@code initialize} (:1135-1137): {@code @statDown = [PBStats::SPATK,2]}. */
        public PokeBattle_Move_03F() {
            statDown = new int[] {PBStats.SPATK, 2};                          // :1136
        }
    }

    /** {@code class PokeBattle_Move_040 < PokeBattle_Move} (Move_Effects_000-07F.rb:1145-1167): Flatter. */
    public static class PokeBattle_Move_040 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:1146-1159). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            boolean failed = true;                                            // :1147
            for (Battler b : targets) {                                       // :1148 targets.each do |b|
                if (!b.pbCanRaiseStatStage(PBStats.SPATK, user, move, false)  // :1149
                        && !b.pbCanConfuse(user, false, move)) {              // :1150
                    continue;
                }
                failed = false;                                               // :1151
                break;                                                        // :1152
            }
            if (failed) {                                                     // :1154
                user.battle.display("但是失败了！");                             // :1155
                return true;                                                  // :1156
            }
            return false;                                                     // :1158
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1161-1166). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.pbCanRaiseStatStage(PBStats.SPATK, user, move, false)) {   // :1162
                target.pbRaiseStatStage(PBStats.SPATK, 1, user);              // :1163
            }
            if (target.pbCanConfuse(user, false, move)) {                     // :1165 target.pbCanConfuse?(user,false,self)
                target.pbConfuse();                                 // :1165 target.pbConfuse
            }
        }
    }

    /** {@code class PokeBattle_Move_041 < PokeBattle_Move} (Move_Effects_000-07F.rb:1174-1196): Swagger. */
    public static class PokeBattle_Move_041 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:1175-1188). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            boolean failed = true;                                            // :1176
            for (Battler b : targets) {                                       // :1177 targets.each do |b|
                if (!b.pbCanRaiseStatStage(PBStats.ATTACK, user, move, false)   // :1178
                        && !b.pbCanConfuse(user, false, move)) {               // :1179
                    continue;
                }
                failed = false;                                               // :1180
                break;                                                        // :1181
            }
            if (failed) {                                                     // :1183
                user.battle.display("但是失败了！");                             // :1184
                return true;                                                  // :1185
            }
            return false;                                                     // :1187
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1190-1195). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.pbCanRaiseStatStage(PBStats.ATTACK, user, move, false)) {   // :1191
                target.pbRaiseStatStage(PBStats.ATTACK, 2, user);             // :1192
            }
            if (target.pbCanConfuse(user, false, move)) {                     // :1194
                target.pbConfuse();                                 // :1194
            }
        }
    }

    /** {@code class PokeBattle_Move_042 < PokeBattle_TargetStatDownMove} (Move_Effects_000-07F.rb:1204-1208): -1 target Attack. */
    public static class PokeBattle_Move_042 extends MoveEffectsGeneric.PokeBattle_TargetStatDownMove {

        /** {@code initialize} (:1205-1207): {@code @statDown = [PBStats::ATTACK,1]}. */
        public PokeBattle_Move_042() {
            statDown = new int[] {PBStats.ATTACK, 1};                         // :1206
        }
    }

    /** {@code class PokeBattle_Move_043 < PokeBattle_TargetStatDownMove} (Move_Effects_000-07F.rb:1215-1219): -1 target Defense. */
    public static class PokeBattle_Move_043 extends MoveEffectsGeneric.PokeBattle_TargetStatDownMove {

        /** {@code initialize} (:1216-1218): {@code @statDown = [PBStats::DEFENSE,1]}. */
        public PokeBattle_Move_043() {
            statDown = new int[] {PBStats.DEFENSE, 1};                        // :1217
        }
    }

    /** {@code class PokeBattle_Move_044 < PokeBattle_TargetStatDownMove} (Move_Effects_000-07F.rb:1227-1238): -1 target Speed. */
    public static class PokeBattle_Move_044 extends MoveEffectsGeneric.PokeBattle_TargetStatDownMove {

        /** {@code initialize} (:1228-1230): {@code @statDown = [PBStats::SPEED,1]}. */
        public PokeBattle_Move_044() {
            statDown = new int[] {PBStats.SPEED, 1};                          // :1229
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:1232-1237). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if ("BULLDOZE".equals(move.internalName())                         // :1233 isConst?(@id,PBMoves,:BULLDOZE)
                    && user.battle.field.terrain == PBBattleTerrains.Grassy) {   // :1233
                baseDmg = Math.round(baseDmg / 2.0f);                          // :1234 (baseDmg/2.0).round
            }
            return baseDmg;                                                    // :1236
        }
    }

    /** {@code class PokeBattle_Move_045 < PokeBattle_TargetStatDownMove} (Move_Effects_000-07F.rb:1246-1250): -1 target Sp. Atk. */
    public static class PokeBattle_Move_045 extends MoveEffectsGeneric.PokeBattle_TargetStatDownMove {

        /** {@code initialize} (:1247-1249): {@code @statDown = [PBStats::SPATK,1]}. */
        public PokeBattle_Move_045() {
            statDown = new int[] {PBStats.SPATK, 1};                          // :1248
        }
    }

    /** {@code class PokeBattle_Move_046 < PokeBattle_TargetStatDownMove} (Move_Effects_000-07F.rb:1258-1262): -1 target Sp. Def. */
    public static class PokeBattle_Move_046 extends MoveEffectsGeneric.PokeBattle_TargetStatDownMove {

        /** {@code initialize} (:1259-1261): {@code @statDown = [PBStats::SPDEF,1]}. */
        public PokeBattle_Move_046() {
            statDown = new int[] {PBStats.SPDEF, 1};                          // :1260
        }
    }

    /** {@code class PokeBattle_Move_047 < PokeBattle_TargetStatDownMove} (Move_Effects_000-07F.rb:1270-1274): -1 target accuracy. */
    public static class PokeBattle_Move_047 extends MoveEffectsGeneric.PokeBattle_TargetStatDownMove {

        /** {@code initialize} (:1271-1273): {@code @statDown = [PBStats::ACCURACY,1]}. */
        public PokeBattle_Move_047() {
            statDown = new int[] {PBStats.ACCURACY, 1};                       // :1272
        }
    }

    /** {@code class PokeBattle_Move_048 < PokeBattle_TargetStatDownMove} (Move_Effects_000-07F.rb:1282-1286): Sweet Scent. */
    public static class PokeBattle_Move_048 extends MoveEffectsGeneric.PokeBattle_TargetStatDownMove {

        /** {@code initialize} (:1283-1285): {@code @statDown = [PBStats::EVASION,(NEWEST_BATTLE_MECHANICS) ? 2 : 1]}. */
        public PokeBattle_Move_048() {
            statDown = new int[] {PBStats.EVASION, Battle.NEWEST_BATTLE_MECHANICS ? 2 : 1};   // :1284
        }
    }

    /** {@code class PokeBattle_Move_049 < PokeBattle_TargetStatDownMove} (Move_Effects_000-07F.rb:1295-1393): Defog. */
    public static class PokeBattle_Move_049 extends MoveEffectsGeneric.PokeBattle_TargetStatDownMove {

        /** {@code @weatherType} (:1391) - the Ruby creates the ivar on assignment; the base has no such field. */
        private int weatherType;

        /** {@code ignoresSubstitute?(user)} (:1296). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                      // :1296
        }

        /** {@code initialize} (:1297-1301): {@code @statDown = [PBStats::EVASION,1]}. */
        public PokeBattle_Move_049() {
            statDown = new int[] {PBStats.EVASION, 1};                        // :1299
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:1302-1320). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            BattleSide targetSide = target.pbOwnSide();                       // :1303
            BattleSide targetOpposingSide = target.pbOpposingSide();          // :1304
            if (targetSide.effects.intVal(PBEffects.Side.AuroraVeil) > 0       // :1305
                    || targetSide.effects.intVal(PBEffects.Side.LightScreen) > 0    // :1306
                    || targetSide.effects.intVal(PBEffects.Side.Reflect) > 0        // :1307
                    || targetSide.effects.intVal(PBEffects.Side.Mist) > 0           // :1308
                    || targetSide.effects.intVal(PBEffects.Side.Safeguard) > 0) {   // :1309
                return false;                                                 // :1305
            }
            if (targetSide.effects.truthy(PBEffects.Side.StealthRock)         // :1310
                    || targetSide.effects.intVal(PBEffects.Side.Spikes) > 0         // :1311
                    || targetSide.effects.intVal(PBEffects.Side.ToxicSpikes) > 0    // :1312
                    || targetSide.effects.truthy(PBEffects.Side.StickyWeb)) {       // :1313
                return false;                                                 // :1310
            }
            if (Battle.NEWEST_BATTLE_MECHANICS                               // :1314
                    && (targetOpposingSide.effects.truthy(PBEffects.Side.StealthRock)   // :1315
                    || targetOpposingSide.effects.intVal(PBEffects.Side.Spikes) > 0     // :1316
                    || targetOpposingSide.effects.intVal(PBEffects.Side.ToxicSpikes) > 0   // :1317
                    || targetOpposingSide.effects.truthy(PBEffects.Side.StickyWeb))) {     // :1318
                return false;                                                 // :1314
            }
            return super.pbFailsAgainstTarget(move, user, target);            // :1320 return super
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1322-1392). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.pbCanLowerStatStage(statDown[0], user, move, false)) {   // :1323
                target.pbLowerStatStage(statDown[0], statDown[1], user);      // :1324
            }
            if (target.pbOwnSide().effects.intVal(PBEffects.Side.AuroraVeil) > 0) {   // :1326
                target.pbOwnSide().effects.set(PBEffects.Side.AuroraVeil, 0);   // :1327
                user.battle.display(target.pbTeam(false) + "的极光幕消失了！");    // :1328
            }
            if (target.pbOwnSide().effects.intVal(PBEffects.Side.LightScreen) > 0) {   // :1330
                target.pbOwnSide().effects.set(PBEffects.Side.LightScreen, 0);   // :1331
                user.battle.display(target.pbTeam(false) + "的光墙消失了！");      // :1332
            }
            if (target.pbOwnSide().effects.intVal(PBEffects.Side.Reflect) > 0) {   // :1334
                target.pbOwnSide().effects.set(PBEffects.Side.Reflect, 0);     // :1335
                user.battle.display(target.pbTeam(false) + "的反射盾消失了！");    // :1336
            }
            if (target.pbOwnSide().effects.intVal(PBEffects.Side.Mist) > 0) {   // :1338
                target.pbOwnSide().effects.set(PBEffects.Side.Mist, 0);        // :1339
                user.battle.display(target.pbTeam(false) + "的白雾消失了！");      // :1340
            }
            if (target.pbOwnSide().effects.intVal(PBEffects.Side.Safeguard) > 0) {   // :1342
                target.pbOwnSide().effects.set(PBEffects.Side.Safeguard, 0);   // :1343
                user.battle.display(target.pbTeam(false) + "不再受神秘守护的保护了!");   // :1344
            }
            if (target.pbOwnSide().effects.truthy(PBEffects.Side.StealthRock)      // :1346
                    || (Battle.NEWEST_BATTLE_MECHANICS                             // :1347
                    && target.pbOpposingSide().effects.truthy(PBEffects.Side.StealthRock))) {   // :1348
                target.pbOwnSide().effects.set(PBEffects.Side.StealthRock, false);      // :1349
                if (Battle.NEWEST_BATTLE_MECHANICS) {                          // :1350
                    target.pbOpposingSide().effects.set(PBEffects.Side.StealthRock, false);
                }
                user.battle.display(user.pbThis() + "吹走了隐形岩！");              // :1351
            }
            if (target.pbOwnSide().effects.intVal(PBEffects.Side.Spikes) > 0       // :1353
                    || (Battle.NEWEST_BATTLE_MECHANICS                             // :1354
                    && target.pbOpposingSide().effects.intVal(PBEffects.Side.Spikes) > 0)) {   // :1355
                target.pbOwnSide().effects.set(PBEffects.Side.Spikes, 0);      // :1356
                if (Battle.NEWEST_BATTLE_MECHANICS) {                          // :1357
                    target.pbOpposingSide().effects.set(PBEffects.Side.Spikes, 0);
                }
                user.battle.display(user.pbThis() + "吹走了地菱！");                // :1358
            }
            if (target.pbOwnSide().effects.intVal(PBEffects.Side.ToxicSpikes) > 0   // :1360
                    || (Battle.NEWEST_BATTLE_MECHANICS                             // :1361
                    && target.pbOpposingSide().effects.intVal(PBEffects.Side.ToxicSpikes) > 0)) {   // :1362
                target.pbOwnSide().effects.set(PBEffects.Side.ToxicSpikes, 0);   // :1363
                if (Battle.NEWEST_BATTLE_MECHANICS) {                          // :1364
                    target.pbOpposingSide().effects.set(PBEffects.Side.ToxicSpikes, 0);
                }
                user.battle.display(user.pbThis() + "吹走了毒菱！");                // :1365
            }
            if (target.pbOwnSide().effects.truthy(PBEffects.Side.StickyWeb)       // :1367
                    || (Battle.NEWEST_BATTLE_MECHANICS                            // :1368
                    && target.pbOpposingSide().effects.truthy(PBEffects.Side.StickyWeb))) {   // :1369
                target.pbOwnSide().effects.set(PBEffects.Side.StickyWeb, false);      // :1370
                target.pbOwnSide().effects.set(PBEffects.Side.StickyWebUser, -1);     // :1371
                if (Battle.NEWEST_BATTLE_MECHANICS) {                          // :1372
                    target.pbOpposingSide().effects.set(PBEffects.Side.StickyWeb, false);
                    target.pbOpposingSide().effects.set(PBEffects.Side.StickyWebUser, -1);   // :1373
                }
                user.battle.display(user.pbThis() + "吹走了黏黏网！");              // :1374
            }
            switch (user.battle.field.terrain) {                              // :1376 case @battle.field.terrain
                case PBBattleTerrains.Electric:                               // :1377
                    user.battle.display("场上的电流消失了！");                    // :1378
                    break;
                case PBBattleTerrains.Grassy:                                 // :1379
                    user.battle.display("四周的青草枯萎了！");                    // :1380
                    break;
                case PBBattleTerrains.Misty:                                  // :1381
                    user.battle.display("四周的薄雾消散了！");                    // :1382
                    break;
                case PBBattleTerrains.Psychic:                                // :1383
                    user.battle.display("场地恢复原样了！");                      // :1384
                    break;
                default:
                    break;
            }
            // :1386 @battle.pbStartTerrain(user,PBBattleTerrains::None,true) - the
            //       PendingApi stub carries no showAnim parameter.
            PendingApi.pbStartTerrain(user.battle, user, PBBattleTerrains.None);
            if (user.battle.pbWeather() == PBWeather.Fog) {                   // :1388 case @battle.pbWeather when PBWeather::Fog
                user.battle.display(user.pbThis() + " blew away the deep fog!");   // :1389
                weatherType = PBWeather.None;                                 // :1390 @weatherType = PBWeather::None
            }
        }
    }

    /** {@code class PokeBattle_Move_04A < PokeBattle_TargetMultiStatDownMove} (Move_Effects_000-07F.rb:1401-1405): Tickle. */
    public static class PokeBattle_Move_04A extends MoveEffectsGeneric.PokeBattle_TargetMultiStatDownMove {

        /** {@code initialize} (:1402-1404): {@code @statDown = [PBStats::ATTACK,1,PBStats::DEFENSE,1]}. */
        public PokeBattle_Move_04A() {
            statDown = new int[] {PBStats.ATTACK, 1, PBStats.DEFENSE, 1};     // :1403
        }
    }

    /** {@code class PokeBattle_Move_04B < PokeBattle_TargetStatDownMove} (Move_Effects_000-07F.rb:1413-1417): -2 target Attack. */
    public static class PokeBattle_Move_04B extends MoveEffectsGeneric.PokeBattle_TargetStatDownMove {

        /** {@code initialize} (:1414-1416): {@code @statDown = [PBStats::ATTACK,2]}. */
        public PokeBattle_Move_04B() {
            statDown = new int[] {PBStats.ATTACK, 2};                         // :1415
        }
    }

    /** {@code class PokeBattle_Move_04C < PokeBattle_TargetStatDownMove} (Move_Effects_000-07F.rb:1425-1429): -2 target Defense. */
    public static class PokeBattle_Move_04C extends MoveEffectsGeneric.PokeBattle_TargetStatDownMove {

        /** {@code initialize} (:1426-1428): {@code @statDown = [PBStats::DEFENSE,2]}. */
        public PokeBattle_Move_04C() {
            statDown = new int[] {PBStats.DEFENSE, 2};                        // :1427
        }
    }

    /** {@code class PokeBattle_Move_04D < PokeBattle_TargetStatDownMove} (Move_Effects_000-07F.rb:1437-1443): Cotton Spore etc. */
    public static class PokeBattle_Move_04D extends MoveEffectsGeneric.PokeBattle_TargetStatDownMove {

        /**
         * {@code initialize} (:1438-1442): {@code @statDown = [PBStats::SPEED,inc]}.
         *
         * <p>The Ruby's second line lowers {@code inc} to 1 only for
         * {@code STRINGSHOT} AND {@code !NEWEST_BATTLE_MECHANICS}
         * ({@code Battle.NEWEST_BATTLE_MECHANICS = true} in this project,
         * Battle.java:1209), so that branch is dead here and {@code inc} is 2.</p>
         */
        public PokeBattle_Move_04D() {
            int inc = 2;                                                      // :1439
            // :1440 inc = 1 if isConst?(@id,PBMoves,:STRINGSHOT) && !NEWEST_BATTLE_MECHANICS
            //       - dead: NEWEST_BATTLE_MECHANICS is true in this project.
            statDown = new int[] {PBStats.SPEED, inc};                        // :1441
        }
    }

    /** {@code class PokeBattle_Move_04E < PokeBattle_TargetStatDownMove} (Move_Effects_000-07F.rb:1452-1482): Captivate. */
    public static class PokeBattle_Move_04E extends MoveEffectsGeneric.PokeBattle_TargetStatDownMove {

        /** {@code initialize} (:1453-1455): {@code @statDown = [PBStats::SPATK,2]}. */
        public PokeBattle_Move_04E() {
            statDown = new int[] {PBStats.SPATK, 2};                          // :1454
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:1457-1475). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (super.pbFailsAgainstTarget(move, user, target)) {             // :1458 return true if super
                return true;
            }
            if (damagingMove(move)) {                                         // :1459
                return false;
            }
            if (user.gender() == 2 || target.gender() == 2 || user.gender() == target.gender()) {   // :1460
                user.battle.display(target.pbThis() + "没有受到影响！");           // :1461
                return true;                                                  // :1462
            }
            if (target.hasActiveAbility("OBLIVIOUS") && !user.battle.moldBreaker) {   // :1464
                user.battle.showAbilitySplash(target);                        // :1465
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {           // :1466
                    user.battle.display(target.pbThis() + "没有受到影响！");       // :1467
                } else {
                    user.battle.display(target.abilityName() + "防止了" + target.pbThis() + "着迷！");   // :1469
                }
                user.battle.hideAbilitySplash(target);                        // :1471
                return true;                                                  // :1472
            }
            return false;                                                     // :1474
        }

        /** {@code pbAdditionalEffect(user,target)} (:1477-1481). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (user.gender() == 2 || target.gender() == 2 || user.gender() == target.gender()) {   // :1478
                return;
            }
            if (target.hasActiveAbility("OBLIVIOUS") && !user.battle.moldBreaker) {   // :1479
                return;
            }
            super.pbAdditionalEffect(move, user, target);                     // :1480 super
        }
    }

    /** {@code class PokeBattle_Move_04F < PokeBattle_TargetStatDownMove} (Move_Effects_000-07F.rb:1490-1494): -2 target Sp. Def. */
    public static class PokeBattle_Move_04F extends MoveEffectsGeneric.PokeBattle_TargetStatDownMove {

        /** {@code initialize} (:1491-1493): {@code @statDown = [PBStats::SPDEF,2]}. */
        public PokeBattle_Move_04F() {
            statDown = new int[] {PBStats.SPDEF, 2};                          // :1492
        }
    }

    /** {@code class PokeBattle_Move_050 < PokeBattle_Move} (Move_Effects_000-07F.rb:1501-1509): Clear Smog. */
    public static class PokeBattle_Move_050 extends MoveEffectBase {

        /** {@code pbEffectAgainstTarget(user,target)} (:1502-1508). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.damageState.calcDamage > 0                             // :1503
                    && !target.damageState.substitute                         // :1503
                    && target.hasAlteredStatStages()) {                       // :1504
                target.pbResetStatStages();                                   // :1505
                user.battle.display(target.pbThis() + "的能力变化被重置了！");      // :1506
            }
        }
    }

    /** {@code class PokeBattle_Move_051 < PokeBattle_Move} (Move_Effects_000-07F.rb:1516-1534): Haze. */
    public static class PokeBattle_Move_051 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:1517-1528). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            boolean failed = true;                                            // :1518
            for (Battler b : user.battle.eachBattler()) {                     // :1519 @battle.eachBattler do |b|
                if (b.hasAlteredStatStages()) {                               // :1520
                    failed = false;
                }
                if (!failed) {                                                // :1521 break if !failed
                    break;
                }
            }
            if (failed) {                                                     // :1523
                user.battle.display("但是失败了！");                             // :1524
                return true;                                                  // :1525
            }
            return false;                                                     // :1527
        }

        /** {@code pbEffectGeneral(user)} (:1530-1533). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            for (Battler b : user.battle.eachBattler()) {                     // :1531 @battle.eachBattler { |b| b.pbResetStatStages }
                b.pbResetStatStages();
            }
            user.battle.display("所有能力恢复了原状！");                           // :1532
        }
    }

    /** {@code class PokeBattle_Move_052 < PokeBattle_Move} (Move_Effects_000-07F.rb:1541-1558): Power Swap. */
    public static class PokeBattle_Move_052 extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:1542). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                      // :1542
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1544-1557). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            for (int s : new int[] {PBStats.ATTACK, PBStats.SPATK}) {         // :1545 [PBStats::ATTACK,PBStats::SPATK].each do |s|
                if (user.stage(s) > target.stage(s)) {                        // :1546
                    user.statsLoweredThisRound = true;                        // :1547
                    user.statsDropped = true;                                 // :1548
                } else if (user.stage(s) < target.stage(s)) {                 // :1549
                    user.statsRaisedThisRound = true;                         // :1550
                    target.statsLoweredThisRound = true;                      // :1551
                    target.statsDropped = true;                               // :1552
                }
                int tmp = user.stage(s);                                      // :1554 user.stages[s],target.stages[s] = target.stages[s],user.stages[s]
                user.setStage(s, target.stage(s));
                target.setStage(s, tmp);
            }
            user.battle.display(user.pbThis() + "和对手互换了自己的\n攻击和特攻的能力变化！");   // :1556
        }
    }

    /** {@code class PokeBattle_Move_053 < PokeBattle_Move} (Move_Effects_000-07F.rb:1565-1582): Guard Swap. */
    public static class PokeBattle_Move_053 extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:1566). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                      // :1566
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1568-1581). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            for (int s : new int[] {PBStats.DEFENSE, PBStats.SPDEF}) {        // :1569
                if (user.stage(s) > target.stage(s)) {                        // :1570
                    user.statsLoweredThisRound = true;                        // :1571
                    user.statsDropped = true;                                 // :1572
                } else if (user.stage(s) < target.stage(s)) {                 // :1573
                    user.statsRaisedThisRound = true;                         // :1574
                    target.statsLoweredThisRound = true;                      // :1575
                    target.statsDropped = true;                               // :1576
                }
                int tmp = user.stage(s);                                      // :1578
                user.setStage(s, target.stage(s));
                target.setStage(s, tmp);
            }
            user.battle.display(user.pbThis() + "和对手互换了自己的\n防御和特防的能力变化！");   // :1580
        }
    }

    /** {@code class PokeBattle_Move_054 < PokeBattle_Move} (Move_Effects_000-07F.rb:1590-1607): Heart Swap. */
    public static class PokeBattle_Move_054 extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:1591). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                      // :1591
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1593-1606). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            for (int s : PBStats.EACH_BATTLE_STAT) {                          // :1594 PBStats.eachBattleStat do |s|
                if (user.stage(s) > target.stage(s)) {                        // :1595
                    user.statsLoweredThisRound = true;                        // :1596
                    user.statsDropped = true;                                 // :1597
                } else if (user.stage(s) < target.stage(s)) {                 // :1598
                    user.statsRaisedThisRound = true;                         // :1599
                    target.statsLoweredThisRound = true;                      // :1600
                    target.statsDropped = true;                               // :1601
                }
                int tmp = user.stage(s);                                      // :1603
                user.setStage(s, target.stage(s));
                target.setStage(s, tmp);
            }
            user.battle.display(user.pbThis() + "和对手互换了自己的\n能力变化！");   // :1605
        }
    }

    /** {@code class PokeBattle_Move_055 < PokeBattle_Move} (Move_Effects_000-07F.rb:1614-1633): Psych Up. */
    public static class PokeBattle_Move_055 extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:1615). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                      // :1615
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1617-1632). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            for (int s : PBStats.EACH_BATTLE_STAT) {                          // :1618 PBStats.eachBattleStat do |s|
                if (user.stage(s) > target.stage(s)) {                        // :1619
                    user.statsLoweredThisRound = true;                        // :1620
                    user.statsDropped = true;                                 // :1621
                } else if (user.stage(s) < target.stage(s)) {                 // :1622
                    user.statsRaisedThisRound = true;                         // :1623
                }
                user.setStage(s, target.stage(s));                            // :1625 user.stages[s] = target.stages[s]
            }
            if (Battle.NEWEST_BATTLE_MECHANICS) {                             // :1627
                user.effects.set(PBEffects.Battler.FocusEnergy,              // :1628
                        target.effects.raw(PBEffects.Battler.FocusEnergy));
                user.effects.set(PBEffects.Battler.LaserFocus,               // :1629
                        target.effects.raw(PBEffects.Battler.LaserFocus));
            }
            user.battle.display(user.pbThis() + "复制了\n" + target.pbThis(true) + "的能力变化!");   // :1631
        }
    }

    /** {@code class PokeBattle_Move_056 < PokeBattle_Move} (Move_Effects_000-07F.rb:1640-1653): Mist. */
    public static class PokeBattle_Move_056 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:1641-1647). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.pbOwnSide().effects.intVal(PBEffects.Side.Mist) > 0) {   // :1642
                user.battle.display("但是失败了！");                             // :1643
                return true;                                                  // :1644
            }
            return false;                                                     // :1646
        }

        /** {@code pbEffectGeneral(user)} (:1649-1652). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.pbOwnSide().effects.set(PBEffects.Side.Mist, 5);             // :1650
            user.battle.display(user.pbTeam(false) + "被白雾包围了！");           // :1651
        }
    }

    /** {@code class PokeBattle_Move_057 < PokeBattle_Move} (Move_Effects_000-07F.rb:1660-1666): Power Trick. */
    public static class PokeBattle_Move_057 extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:1661-1665). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            int atk = user.attack();                                          // :1662 user.attack,user.defense = user.defense,user.attack
            setAttack(user, user.defense());
            setDefense(user, atk);
            user.effects.set(PBEffects.Battler.PowerTrick,                    // :1663
                    !user.effects.truthy(PBEffects.Battler.PowerTrick));
            user.battle.display(user.pbThis() + "交换了攻击和防御！");             // :1664
        }
    }

    /** {@code class PokeBattle_Move_058 < PokeBattle_Move} (Move_Effects_000-07F.rb:1674-1682): Power Split. */
    public static class PokeBattle_Move_058 extends MoveEffectBase {

        /** {@code pbEffectAgainstTarget(user,target)} (:1675-1681). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            int newatk = (user.attack() + target.attack()) / 2;               // :1676 ((user.attack+target.attack)/2).floor
            int newspatk = (user.spAtk() + target.spAtk()) / 2;               // :1677
            setAttack(user, newatk);                                          // :1678 user.attack = target.attack = newatk
            setAttack(target, newatk);
            setSpAtk(user, newspatk);                                         // :1679
            setSpAtk(target, newspatk);
            user.battle.display(user.pbThis() + "与目标平分了力量！");             // :1680
        }
    }

    /** {@code class PokeBattle_Move_059 < PokeBattle_Move} (Move_Effects_000-07F.rb:1690-1698): Guard Split. */
    public static class PokeBattle_Move_059 extends MoveEffectBase {

        /** {@code pbEffectAgainstTarget(user,target)} (:1691-1697). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            int newdef = (user.defense() + target.defense()) / 2;             // :1692 ((user.defense+target.defense)/2).floor
            int newspdef = (user.spDef() + target.spDef()) / 2;               // :1693
            // 登记: Move_Effects_000-07F.rb:1694-1695 user.defense=/user.spdef= ——
            //       插件里没有 defense=/spdef= 访问器（只有 attr_accessor
            //       :attack/:spatk/:speed，PokeBattle_Battler:15-17），Ruby 会
            //       NoMethodError；照抄不修正（见私有助手 setDefense/setSpDef）。
            setDefense(user, newdef);                                         // :1694
            setDefense(target, newdef);
            setSpDef(user, newspdef);                                         // :1695
            setSpDef(target, newspdef);
            user.battle.display(user.pbThis() + "与目标平分了防御！");             // :1696
        }
    }

    /** {@code class PokeBattle_Move_05A < PokeBattle_Move} (Move_Effects_000-07F.rb:1705-1726): Pain Split. */
    public static class PokeBattle_Move_05A extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:1706-1712). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.pokemon.battleRank > 1) {                              // :1707
                user.battle.display("但是对" + target.pbThis() + "没有效果！");     // :1708
                return true;                                                  // :1709
            }
            return false;                                                     // :1711
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1714-1725). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            int newHP = (user.hp + target.hp) / 2;                            // :1715
            if (user.hp > newHP) {                                            // :1716
                // :1716 user.pbReduceHP(user.hp-newHP,false,false) - the
                //       MoveFxPendingApi stub carries no registerDamage parameter.
                user.pbReduceHP(user.hp - newHP, false, true, true);
            } else if (user.hp < newHP) {                                     // :1717
                // :1717 user.pbRecoverHP(newHP-user.hp,false) - the stub carries no anim parameter.
                user.pbRecoverHP(newHP - user.hp);
            }
            if (target.hp > newHP) {                                          // :1719
                target.pbReduceHP(target.hp - newHP, false, true, true);
            } else if (target.hp < newHP) {                                   // :1720
                target.pbRecoverHP(newHP - target.hp);
            }
            user.battle.display("双方分担了痛楚！");                              // :1722
            user.pbItemHPHealCheck(0, false);                         // :1723
            target.pbItemHPHealCheck(0, false);                       // :1724
        }
    }

    /** {@code class PokeBattle_Move_05B < PokeBattle_Move} (Move_Effects_000-07F.rb:1733-1757): Tailwind. */
    public static class PokeBattle_Move_05B extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:1734-1740). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.pbOwnSide().effects.intVal(PBEffects.Side.Tailwind) > 0) {   // :1735
                user.battle.display("但是失败了！");                             // :1736
                return true;                                                  // :1737
            }
            return false;                                                     // :1739
        }

        /** {@code pbEffectGeneral(user)} (:1742-1756). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.pbOwnSide().effects.set(PBEffects.Side.Tailwind, 4);         // :1743
            user.battle.display(user.pbTeam(true) + "刮起了顺风！");              // :1744
            for (Battler b : user.battle.allSameSideBattlers(user.index)) {   // :1745 @battle.allSameSideBattlers(user)
                if (b == null || b.fainted()) {                               // :1746
                    continue;
                }
                if (b.hasActiveAbility("WINDRIDER")                           // :1747
                        && b.pbCanRaiseStatStage(PBStats.ATTACK, b, move, false)) {   // :1747
                    b.pbRaiseStatStageByAbility(PBStats.ATTACK, 1, b, true);  // :1748
                } else if (b.hasActiveAbility("WINDPOWER")                    // :1749
                        && b.effects.intVal(PBEffects.Battler.Charge) == 0) {   // :1749
                    user.battle.showAbilitySplash(b);                         // :1750
                    b.effects.set(PBEffects.Battler.Charge, 2);               // :1751
                    user.battle.display("被顺风击中使" + b.pbThis(true) + "充满了力量！");   // :1752
                    user.battle.hideAbilitySplash(b);                         // :1753
                }
            }
        }
    }

    /** {@code class PokeBattle_Move_05C < PokeBattle_Move} (Move_Effects_000-07F.rb:1766-1814): Mimic. */
    public static class PokeBattle_Move_05C extends MoveEffectBase {

        /** {@code @moveBlacklist} (:1771-1781). */
        private final String[] moveBlacklist =
                {"014", "0B6", "002", "05C", "05D", "069"};                   // :1772-1780

        /** {@code ignoresSubstitute?(user)} (:1767). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                      // :1767
        }

        /** {@code pbMoveFailed?(user,targets)} (:1783-1789). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.effects.truthy(PBEffects.Battler.Transform)             // :1784
                    || !pbHasMove(user, move.internalName())) {               // :1784 !user.pbHasMove?(@id)
                user.battle.display("但是失败了！");                             // :1785
                return true;                                                  // :1786
            }
            return false;                                                     // :1788
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:1791-1801). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            // :1792 lastMoveData = pbGetMoveData(target.lastRegularMoveUsed)
            PbsData.Move lastMoveData = user.battle.pbs().move(target.lastRegularMoveUsed);
            if (target.lastRegularMoveUsed == null                          // :1793 target.lastRegularMoveUsed<=0 (runtime -1 = null)
                    || pbHasMove(user, target.lastRegularMoveUsed)           // :1794
                    || (lastMoveData != null                                 // :1795 @moveBlacklist.include?(lastMoveData[MOVE_FUNCTION_CODE])
                    && contains(moveBlacklist, lastMoveData.function))
                    || (lastMoveData != null && "SHADOW".equals(lastMoveData.type))) {   // :1796 isConst?(lastMoveData[MOVE_TYPE],PBTypes,:SHADOW)
                user.battle.display("但是失败了！");                             // :1797
                return true;                                                  // :1798
            }
            return false;                                                     // :1800
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1803-1813). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            Array<BattleMove> slots = user.moveSlots();                       // :1804 user.eachMoveWithIndex
            for (int i = 0; i < slots.size; i++) {
                BattleMove m = slots.get(i);
                if (m == null || m.id() == 0) {                              // :1804 eachMoveWithIndex skips empty slots
                    continue;
                }
                if (m.id() != move.id()) {                                   // :1805 next if m.id!=@id
                    continue;
                }
                PbsData.Move data = user.battle.pbs().move(target.lastRegularMoveUsed);   // :1806-1807 PBMove.new(...) + pbFromPBMove
                if (data != null) {
                    setMove(user, i, new BattleMove(data));                  // :1808 user.moves[i] = PokeBattle_Move.pbFromPBMove(@battle,newMove)
                }
                user.battle.display(user.pbThis() + "学会了" + (data == null ? "?" : data.name) + "！");   // :1809-1810
                pbCheckFormOnMovesetChange(user);                            // :1811
                break;                                                       // :1812
            }
        }
    }

    /** {@code class PokeBattle_Move_05D < PokeBattle_Move} (Move_Effects_000-07F.rb:1821-1866): Sketch. */
    public static class PokeBattle_Move_05D extends MoveEffectBase {

        /** {@code @moveBlacklist} (:1826-1831). */
        private final String[] moveBlacklist = {"014", "05D", "002"};         // :1827-1830

        /** {@code ignoresSubstitute?(user)} (:1822). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                      // :1822
        }

        /** {@code pbMoveFailed?(user,targets)} (:1834-1840). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.effects.truthy(PBEffects.Battler.Transform)             // :1835
                    || !pbHasMove(user, move.internalName())) {               // :1835
                user.battle.display("但是失败了！");                             // :1836
                return true;                                                  // :1837
            }
            return false;                                                     // :1839
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:1842-1852). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            PbsData.Move lastMoveData = user.battle.pbs().move(target.lastRegularMoveUsed);   // :1843
            if (target.lastRegularMoveUsed == null                          // :1844
                    || pbHasMove(user, target.lastRegularMoveUsed)           // :1845
                    || (lastMoveData != null                                 // :1846
                    && contains(moveBlacklist, lastMoveData.function))
                    || (lastMoveData != null && "SHADOW".equals(lastMoveData.type))) {   // :1847
                user.battle.display("但是失败了！");                             // :1848
                return true;                                                  // :1849
            }
            return false;                                                     // :1851
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1854-1865). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            Array<BattleMove> slots = user.moveSlots();                       // :1855 user.eachMoveWithIndex
            for (int i = 0; i < slots.size; i++) {
                BattleMove m = slots.get(i);
                if (m == null || m.id() == 0) {
                    continue;
                }
                if (m.id() != move.id()) {                                   // :1856
                    continue;
                }
                PbsData.Move data = user.battle.pbs().move(target.lastRegularMoveUsed);   // :1857-1858 PBMove.new(...)
                if (data != null) {
                    setPokemonMove(user.pokemon, i, data);                   // :1859 user.pokemon.moves[i] = newMove
                    setMove(user, i, new BattleMove(data));                  // :1860
                }
                user.battle.display(user.pbThis() + "学会了" + (data == null ? "?" : data.name) + "！");   // :1861-1862
                pbCheckFormOnMovesetChange(user);                            // :1863
                break;                                                       // :1864
            }
        }
    }

    /** {@code class PokeBattle_Move_05E < PokeBattle_Move} (Move_Effects_000-07F.rb:1875-1902): Conversion. */
    public static class PokeBattle_Move_05E extends MoveEffectBase {

        /** {@code @newTypes} (:1882). */
        private final Array<String> newTypes = new Array<>();

        /** {@code pbMoveFailed?(user,targets)} (:1876-1893). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!canChangeType(user)) {                                       // :1877
                user.battle.display("但是失败了！");                             // :1878
                return true;                                                  // :1879
            }
            Array<String> userTypes = user.pbTypes(true);                     // :1881 user.pbTypes(true)
            newTypes.clear();                                                 // :1882 @newTypes = []
            Array<BattleMove> slots = user.moveSlots();                       // :1883 user.eachMoveWithIndex
            for (int i = 0; i < slots.size; i++) {
                BattleMove m = slots.get(i);
                if (m == null || m.id() == 0) {
                    continue;
                }
                if (Battle.NEWEST_BATTLE_MECHANICS && i > 0) {                // :1884 break if NEWEST_BATTLE_MECHANICS && i>0
                    break;
                }
                if (PBTypes.isPseudoType(user.battle.pbs(), m.type())) {      // :1885 next if PBTypes.isPseudoType?(m.type)
                    continue;
                }
                if (userTypes.contains(m.type(), false)) {                    // :1886 next if userTypes.include?(m.type)
                    continue;
                }
                if (!newTypes.contains(m.type(), false)) {                    // :1887 @newTypes.push(m.type) if !@newTypes.include?(m.type)
                    newTypes.add(m.type());
                }
            }
            if (newTypes.size == 0) {                                         // :1889
                user.battle.display("但是失败了！");                             // :1890
                return true;                                                  // :1891
            }
            return false;                                                     // :1893
        }

        /** {@code pbEffectGeneral(user)} (:1896-1901). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            String newType = newTypes.get(user.battle.pbRandom(newTypes.size));   // :1897
            PendingApi.pbChangeTypes(user, newType);                          // :1898 user.pbChangeTypes(newType)
            String typeName = PBTypes.getName(user.battle.pbs(), newType);    // :1899
            user.battle.display(user.pbThis() + "变为了" + typeName + "属性！");   // :1900
        }
    }

    /** {@code class PokeBattle_Move_05F < PokeBattle_Move} (Move_Effects_000-07F.rb:1910-1948): Conversion 2. */
    public static class PokeBattle_Move_05F extends MoveEffectBase {

        /** {@code @newTypes} (:1928). */
        private final Array<String> newTypes = new Array<>();

        /** {@code ignoresSubstitute?(user)} (:1911). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                      // :1911
        }

        /** {@code pbMoveFailed?(user,targets)} (:1913-1919). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!canChangeType(user)) {                                       // :1914
                user.battle.display("但是失败了！");                             // :1915
                return true;                                                  // :1916
            }
            return false;                                                     // :1918
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:1921-1940). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            PbsData pbs = user.battle.pbs();
            // :1925 PBTypes.isPseudoType?(pbGetMoveData(target.lastMoveUsed,MOVE_TYPE));
            //       the runtime stores that same type in lastMoveUsedType (Battler.java:531-532).
            if (target.lastMoveUsed == null                                  // :1922 target.lastMoveUsed<=0
                    || target.lastMoveUsedType == null                        // :1923 target.lastMoveUsedType<0
                    || PBTypes.isPseudoType(pbs, target.lastMoveUsedType)) {  // :1924
                user.battle.display("但是失败了！");                             // :1925
                return true;                                                  // :1926
            }
            newTypes.clear();                                                 // :1928 @newTypes = []
            for (int i = 0; i <= PBTypes.maxValue(pbs); i++) {                // :1929 for i in 0..PBTypes.maxValue
                String typeName = typeNameById(pbs, i);                       // :1930-1934 按 id 取内部名
                if (typeName == null) {
                    continue;
                }
                if (PBTypes.isPseudoType(pbs, typeName)) {                    // :1930 next if PBTypes.isPseudoType?(i)
                    continue;
                }
                if (user.pbHasType(typeName)) {                               // :1931 next if user.pbHasType?(i)
                    continue;
                }
                if (!PBTypes.resistant(pbs, target.lastMoveUsedType,         // :1932 next if !PBTypes.resistant?(target.lastMoveUsedType,i)
                        typeName, null, null)) {
                    continue;
                }
                newTypes.add(typeName);                                       // :1933 @newTypes.push(i)
            }
            if (newTypes.size == 0) {                                         // :1935
                user.battle.display("但是失败了！");                             // :1936
                return true;                                                  // :1937
            }
            return false;                                                     // :1939
        }

        /** {@code pbEffectGeneral(user)} (:1942-1947). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            String newType = newTypes.get(user.battle.pbRandom(newTypes.size));   // :1943
            PendingApi.pbChangeTypes(user, newType);                          // :1944
            String typeName = PBTypes.getName(user.battle.pbs(), newType);    // :1945
            user.battle.display(user.pbThis() + "变为了" + typeName + "属性！");   // :1946
        }
    }

    /**
     * {@code class PokeBattle_Move_060 < PokeBattle_Move}
     * (Move_Effects_000-07F.rb:1955-2015): Camouflage.
     *
     * <p>Reopened twice by the later {@code 场地} section with an {@code alias}
     * chain - {@code 场地:190} binds the main body to
     * {@code bug_lure_camouflage_pbMoveFailed} and overrides
     * {@code pbMoveFailed?} with the BugLure branch, then {@code 场地:410} binds
     * that (already overridden) body to
     * {@code cold_terrain_camouflage_pbMoveFailed} and overrides
     * {@code pbMoveFailed?} with the Cold branch. The later section wins, so the
     * effective chain is Cold &rarr; BugLure &rarr; main, reproduced here as the
     * {@code @Override} plus two private methods.</p>
     */
    public static class PokeBattle_Move_060 extends MoveEffectBase {

        /** {@code @newType} (:1961/:2011) - a type internal name in this runtime. */
        private String newType;

        /** {@code pbMoveFailed?(user,targets)} - the outermost {@code 场地:411-427} override (wins). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.battle.field.terrain == PBBattleTerrains.Cold) {         // 场地:412
                if (!canChangeType(user)) {                                   // 场地:413
                    user.battle.display("但是失败了！");                         // 场地:414
                    return true;                                              // 场地:415
                }
                newType = "ICE";                                              // 场地:418 getID(PBTypes,:ICE)
                if (!user.pbHasOtherType(newType)) {                          // 场地:419
                    user.battle.display("但是失败了！");                         // 场地:420
                    return true;                                              // 场地:421
                }
                return false;                                                 // 场地:423
            }
            return coldTerrainCamouflagePbMoveFailed(move, user, targets);    // 场地:426
        }

        /** {@code alias cold_terrain_camouflage_pbMoveFailed} (场地:410) - the BugLure layer. */
        private boolean coldTerrainCamouflagePbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.battle.field.terrain == PBBattleTerrains.BugLure) {      // 场地:192
                if (!canChangeType(user)) {                                   // 场地:193
                    user.battle.display("但是失败了！");                         // 场地:194
                    return true;                                              // 场地:195
                }
                newType = "BUG";                                              // 场地:198 getID(PBTypes,:BUG)
                if (!user.pbHasOtherType(newType)) {                          // 场地:199
                    user.battle.display("但是失败了！");                         // 场地:200
                    return true;                                              // 场地:201
                }
                return false;                                                 // 场地:203
            }
            return bugLureCamouflagePbMoveFailed(move, user, targets);        // 场地:206
        }

        /** {@code alias bug_lure_camouflage_pbMoveFailed} (场地:190) - the main :1956-2008 body. */
        private boolean bugLureCamouflagePbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            PbsData pbs = user.battle.pbs();
            if (!canChangeType(user)) {                                       // :1957
                user.battle.display("但是失败了！");                             // :1958
                return true;                                                  // :1959
            }
            newType = "NORMAL";                                               // :1961 getID(PBTypes,:NORMAL)
            boolean checkedTerrain = false;                                   // :1962
            switch (user.battle.field.terrain) {                              // :1963 case @battle.field.terrain
                case PBBattleTerrains.Electric:                               // :1964
                    if (pbs.types.containsKey("ELECTRIC")) {                  // :1965 hasConst?(PBTypes,:ELECTRIC)
                        newType = "ELECTRIC";                                 // :1966 getID(PBTypes,:ELECTRIC)
                        checkedTerrain = true;
                    }
                    break;
                case PBBattleTerrains.Grassy:                                 // :1968
                    if (pbs.types.containsKey("GRASS")) {                     // :1969
                        newType = "GRASS";                                    // :1970
                        checkedTerrain = true;
                    }
                    break;
                case PBBattleTerrains.Misty:                                  // :1972
                    if (pbs.types.containsKey("FAIRY")) {                     // :1973
                        newType = "FAIRY";                                    // :1974
                        checkedTerrain = true;
                    }
                    break;
                case PBBattleTerrains.Psychic:                                // :1976
                    if (pbs.types.containsKey("PSYCHIC")) {                   // :1977
                        newType = "PSYCHIC";                                  // :1978
                        checkedTerrain = true;
                    }
                    break;
                default:
                    break;
            }
            if (!checkedTerrain) {                                            // :1981
                switch (user.battle.environment) {                            // :1982 case @battle.environment
                    case PBEnvironment.Grass:                                 // :1983
                    case PBEnvironment.TallGrass:                             // :1984
                        newType = "GRASS";                                    // :1983-1984 getID(PBTypes,:GRASS)
                        break;
                    case PBEnvironment.MovingWater:                           // :1985
                    case PBEnvironment.StillWater:                            // :1986
                    case PBEnvironment.Puddle:                                // :1987
                    case PBEnvironment.Underwater:                            // :1988
                        newType = "WATER";                                    // :1985-1988 getID(PBTypes,:WATER)
                        break;
                    case PBEnvironment.Cave:                                  // :1989
                        newType = "ROCK";                                     // :1989 getID(PBTypes,:ROCK)
                        break;
                    case PBEnvironment.Rock:                                  // :1990
                    case PBEnvironment.Sand:                                  // :1991
                        newType = "GROUND";                                   // :1990-1991 getID(PBTypes,:GROUND)
                        break;
                    case PBEnvironment.Forest:                                // :1992
                    case PBEnvironment.ForestGrass:                           // :1993
                        newType = "BUG";                                      // :1992-1993 getID(PBTypes,:BUG)
                        break;
                    case PBEnvironment.Snow:                                  // :1994
                    case PBEnvironment.Ice:                                   // :1995
                        newType = "ICE";                                      // :1994-1995 getID(PBTypes,:ICE)
                        break;
                    case PBEnvironment.Volcano:                               // :1996
                        newType = "FIRE";                                     // :1996 getID(PBTypes,:FIRE)
                        break;
                    case PBEnvironment.Graveyard:                             // :1997
                        newType = "GHOST";                                    // :1997 getID(PBTypes,:GHOST)
                        break;
                    case PBEnvironment.Sky:                                   // :1998
                        newType = "FLYING";                                   // :1998 getID(PBTypes,:FLYING)
                        break;
                    case PBEnvironment.Space:                                 // :1999
                        newType = "DRAGON";                                   // :1999 getID(PBTypes,:DRAGON)
                        break;
                    case PBEnvironment.UltraSpace:                            // :2000
                        newType = "PSYCHIC";                                  // :2000 getID(PBTypes,:PSYCHIC)
                        break;
                    default:
                        break;
                }
            }
            if (!user.pbHasOtherType(newType)) {                              // :2003
                user.battle.display("但是失败了！");                             // :2004
                return true;                                                  // :2005
            }
            return false;                                                     // :2007
        }

        /** {@code pbEffectGeneral(user)} (:2010-2014). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            PendingApi.pbChangeTypes(user, newType);                          // :2011 user.pbChangeTypes(@newType)
            String typeName = PBTypes.getName(user.battle.pbs(), newType);    // :2012
            user.battle.display(user.pbThis() + "变为了" + typeName + "属性！");   // :2013
        }
    }

    /** {@code class PokeBattle_Move_061 < PokeBattle_Move} (Move_Effects_000-07F.rb:2022-2038): Soak. */
    public static class PokeBattle_Move_061 extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:2023-2030). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (!canChangeType(target)                                        // :2024
                    || !target.pbHasOtherType("WATER")) {                     // :2025 getConst(PBTypes,:WATER)
                user.battle.display("但是失败了！");                             // :2026
                return true;                                                  // :2027
            }
            return false;                                                     // :2029
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:2032-2037). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            String newType = "WATER";                                         // :2033 getConst(PBTypes,:WATER)
            PendingApi.pbChangeTypes(target, newType);                        // :2034 target.pbChangeTypes(newType)
            String typeName = PBTypes.getName(user.battle.pbs(), newType);    // :2035
            user.battle.display(target.pbThis() + "变为了" + typeName + "属性！");   // :2036
        }
    }

    /** {@code class PokeBattle_Move_062 < PokeBattle_Move} (Move_Effects_000-07F.rb:2045-2075): Reflect Type. */
    public static class PokeBattle_Move_062 extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:2046). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                      // :2046
        }

        /** {@code pbMoveFailed?(user,targets)} (:2048-2054). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!canChangeType(user)) {                                       // :2049
                user.battle.display("但是失败了！");                             // :2050
                return true;                                                  // :2051
            }
            return false;                                                     // :2053
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:2056-2068). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            Array<String> newTypes = target.pbTypes(true);                    // :2057
            if (newTypes.size == 0) {                                         // :2058 Target has no type to copy
                user.battle.display("但是失败了！");                             // :2059
                return true;                                                  // :2060
            }
            if (sameTypes(user.pbTypes(), target.pbTypes())                   // :2062 user.pbTypes==target.pbTypes
                    && user.effects.intVal(PBEffects.Battler.Type3)           // :2063 user.effects[PBEffects::Type3]==...
                    == target.effects.intVal(PBEffects.Battler.Type3)) {
                user.battle.display("但是失败了！");                             // :2064
                return true;                                                  // :2065
            }
            return false;                                                     // :2067
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:2070-2074). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            pbChangeTypesWithBattler(user, target);                           // :2071 user.pbChangeTypes(target) (插件传的是 Battler)
            user.battle.display(user.pbThis() + "的属性变得跟" + target.pbThis(true) + "相同！");   // :2072-2073
        }
    }

    /** {@code class PokeBattle_Move_063 < PokeBattle_Move} (Move_Effects_000-07F.rb:2082-2114): Simple Beam. */
    public static class PokeBattle_Move_063 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:2083-2089). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.battle.pbs().abilities.containsKey("SIMPLE")) {         // :2084 !hasConst?(PBAbilities,:SIMPLE)
                user.battle.display("但是它失败了！");                           // :2085
                return true;                                                  // :2086
            }
            return false;                                                     // :2088
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:2091-2103). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.unstoppableAbility(null)                               // :2092
                    || "TRUANT".equals(target.ability)                        // :2093 isConst?(target.ability,PBAbilities,:TRUANT)
                    || "SIMPLE".equals(target.ability)) {                     // :2094
                user.battle.display("但是它失败了！");                           // :2095
                return true;                                                  // :2096
            }
            if (target.hasActiveItem("ABILITYSHIELD")) {                      // :2098
                user.battle.display(target.pbThis() + "的特性\n被特性护具的效果保护了！");   // :2099
                return true;                                                  // :2100
            }
            return false;                                                     // :2102
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:2105-2113). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            // :2106 @battle.pbShowAbilitySplash(target,true,false) - the Battle
            //       method carries only the battler parameter.
            user.battle.showAbilitySplash(target);
            String oldAbil = target.ability;                                  // :2107
            target.ability = "SIMPLE";                                        // :2108 getConst(PBAbilities,:SIMPLE)
            user.battle.replaceAbilitySplash(target);                         // :2109
            user.battle.display(target.pbThis() + "获得了" + target.abilityName() + "！");   // :2110
            user.battle.hideAbilitySplash(target);                            // :2111
            target.pbOnAbilityChanged(oldAbil);                               // :2112
        }
    }

    /** {@code class PokeBattle_Move_064 < PokeBattle_Move} (Move_Effects_000-07F.rb:2121-2153): Worry Seed. */
    public static class PokeBattle_Move_064 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:2122-2128). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.battle.pbs().abilities.containsKey("INSOMNIA")) {       // :2123 !hasConst?(PBAbilities,:INSOMNIA)
                user.battle.display("但是它失败了！");                           // :2124
                return true;                                                  // :2125
            }
            return false;                                                     // :2127
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:2130-2142). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.unstoppableAbility(null)                               // :2131
                    || "TRUANT".equals(target.ability)                        // :2132
                    || "INSOMNIA".equals(target.ability)) {                   // :2133
                user.battle.display("但是它失败了！");                           // :2134
                return true;                                                  // :2135
            }
            if (target.hasActiveItem("ABILITYSHIELD")) {                      // :2137
                user.battle.display(target.pbThis() + "的特性\n被特性护具的效果保护了！");   // :2138
                return true;                                                  // :2139
            }
            return false;                                                     // :2141
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:2144-2152). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            user.battle.showAbilitySplash(target);                            // :2145 @battle.pbShowAbilitySplash(target,true,false)
            String oldAbil = target.ability;                                  // :2146
            target.ability = "INSOMNIA";                                      // :2147 getConst(PBAbilities,:INSOMNIA)
            user.battle.replaceAbilitySplash(target);                         // :2148
            user.battle.display(target.pbThis() + "获得了" + target.abilityName() + "！");   // :2149
            user.battle.hideAbilitySplash(target);                            // :2150
            target.pbOnAbilityChanged(oldAbil);                               // :2151
        }
    }

    /** {@code class PokeBattle_Move_065 < PokeBattle_Move} (Move_Effects_000-07F.rb:2160-2202): Role Play. */
    public static class PokeBattle_Move_065 extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:2161). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                      // :2161
        }

        /** {@code pbMoveFailed?(user,targets)} (:2163-2173). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.unstoppableAbility(null)) {                              // :2164
                user.battle.display("但是失败了！");                             // :2165
                return true;                                                  // :2166
            }
            if (user.hasActiveItem("ABILITYSHIELD")) {                        // :2168
                user.battle.display(user.pbThis() + "的特性\n被特性护具的效果保护了！");   // :2169
                return true;                                                  // :2170
            }
            return false;                                                     // :2172
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:2175-2188). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.ability.isEmpty() || user.ability.equals(target.ability)) {   // :2176 target.ability==0 || user.ability==target.ability
                user.battle.display("但是失败了！");                             // :2177
                return true;                                                  // :2178
            }
            if (target.ungainableAbility(null)                                // :2180
                    || "POWEROFALCHEMY".equals(target.ability)                // :2181
                    || "RECEIVER".equals(target.ability)                      // :2182
                    || "TRACE".equals(target.ability)                         // :2183
                    || "WONDERGUARD".equals(target.ability)) {                // :2184
                user.battle.display("但是失败了！");                             // :2185
                return true;                                                  // :2186
            }
            return false;                                                     // :2188
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:2191-2200). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            user.battle.showAbilitySplash(user);                              // :2192 @battle.pbShowAbilitySplash(user,true,false)
            String oldAbil = user.ability;                                    // :2193
            user.ability = target.ability;                                    // :2194
            user.battle.replaceAbilitySplash(user);                           // :2195
            user.battle.display(user.pbThis() + "复制了" + target.pbThis(true) + "的" + target.abilityName() + "！");   // :2196-2197
            user.battle.hideAbilitySplash(user);                              // :2198
            user.pbOnAbilityChanged(oldAbil);                                 // :2199
            pbEffectsOnSwitchIn(user, false);                                 // :2200 user.pbEffectsOnSwitchIn
        }
    }

    /** {@code class PokeBattle_Move_066 < PokeBattle_Move} (Move_Effects_000-07F.rb:2209-2246): Entrainment. */
    public static class PokeBattle_Move_066 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:2210-2222). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.ability.isEmpty()) {                                     // :2211 user.ability==0
                user.battle.display("但是失败了！");                             // :2212
                return true;                                                  // :2213
            }
            if (user.ungainableAbility(null)                                  // :2215
                    || "POWEROFALCHEMY".equals(user.ability)                  // :2216
                    || "RECEIVER".equals(user.ability)                        // :2217
                    || "TRACE".equals(user.ability)) {                        // :2218
                user.battle.display("但是失败了！");                             // :2219
                return true;                                                  // :2220
            }
            return false;                                                     // :2222
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:2225-2234). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.unstoppableAbility(null) || "TRUANT".equals(target.ability)) {   // :2226
                user.battle.display("但是失败了！");                             // :2227
                return true;                                                  // :2228
            }
            if (target.hasActiveItem("ABILITYSHIELD")) {                      // :2230
                user.battle.display(target.pbThis() + "的特性\n被特性护具的效果保护了！");   // :2231
                return true;                                                  // :2232
            }
            return false;                                                     // :2234
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:2237-2244). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            user.battle.showAbilitySplash(target);                            // :2238 @battle.pbShowAbilitySplash(target,true,false)
            String oldAbil = target.ability;                                  // :2239
            target.ability = user.ability;                                    // :2240
            user.battle.display(target.pbThis() + "获得了" + target.abilityName() + "！");   // :2241
            user.battle.hideAbilitySplash(target);                            // :2242
            target.pbOnAbilityChanged(oldAbil);                               // :2243
            pbEffectsOnSwitchIn(target, false);                               // :2244 target.pbEffectsOnSwitchIn
        }
    }

    /** {@code class PokeBattle_Move_067 < PokeBattle_Move} (Move_Effects_000-07F.rb:2253-2329): Skill Swap. */
    public static class PokeBattle_Move_067 extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:2254). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                      // :2254
        }

        /** {@code pbMoveFailed?(user,targets)} (:2256-2273). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.ability.isEmpty()) {                                     // :2257 user.ability==0
                user.battle.display("但是失败了！");                             // :2258
                return true;                                                  // :2259
            }
            if (user.unstoppableAbility(null)) {                              // :2261
                user.battle.display("但是失败了！");                             // :2262
                return true;                                                  // :2263
            }
            if (user.ungainableAbility(null) || "WONDERGUARD".equals(user.ability)) {   // :2265
                user.battle.display("但是失败了！");                             // :2266
                return true;                                                  // :2267
            }
            if (user.hasActiveItem("ABILITYSHIELD")) {                        // :2269
                user.battle.display(user.pbThis() + "的特性\n被特性护具的效果保护了！");   // :2270
                return true;                                                  // :2271
            }
            return false;                                                     // :2273
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:2276-2298). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.ability.isEmpty()                                  // :2277 target.ability==0
                    || (user.ability.equals(target.ability) && !Battle.NEWEST_BATTLE_MECHANICS)) {   // :2278
                user.battle.display("但是失败了！");                             // :2279
                return true;                                                  // :2280
            }
            if (target.unstoppableAbility(null)) {                            // :2282
                user.battle.display("但是失败了！");                             // :2283
                return true;                                                  // :2284
            }
            if (target.ungainableAbility(null) || "WONDERGUARD".equals(target.ability)) {   // :2286
                user.battle.display("但是失败了！");                             // :2287
                return true;                                                  // :2288
            }
            if (target.pokemon.battleRank > 2) {                              // :2290
                user.battle.display("但是它失败了！");                           // :2291
                return true;                                                  // :2292
            }
            if (target.hasActiveItem("ABILITYSHIELD")) {                      // :2294
                user.battle.display(target.pbThis() + "的特性\n被特性护具的效果保护了！");   // :2295
                return true;                                                  // :2296
            }
            return false;                                                     // :2298
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:2301-2328). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (user.opposes(target)) {                                       // :2302
                user.battle.showAbilitySplash(user);                          // :2303 @battle.pbShowAbilitySplash(user,false,false)
                user.battle.showAbilitySplash(target);                        // :2304 @battle.pbShowAbilitySplash(target,true,false)
            }
            String oldUserAbil = user.ability;                                // :2306
            String oldTargetAbil = target.ability;                            // :2307
            user.ability = oldTargetAbil;                                     // :2308
            target.ability = oldUserAbil;                                     // :2309
            if (user.opposes(target)) {                                       // :2310
                user.battle.replaceAbilitySplash(user);                       // :2311
                user.battle.replaceAbilitySplash(target);                     // :2312
            }
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {               // :2314
                user.battle.display(user.pbThis() + "与目标交换了特性！");         // :2315
            } else {
                user.battle.display(user.pbThis() + "将" + target.abilityName() + "与目标的" + user.abilityName() + "交换了！");   // :2317-2318
            }
            if (user.opposes(target)) {                                       // :2320
                user.battle.hideAbilitySplash(user);                          // :2321
                user.battle.hideAbilitySplash(target);                        // :2322
            }
            user.pbOnAbilityChanged(oldUserAbil);                             // :2324
            target.pbOnAbilityChanged(oldTargetAbil);                         // :2325
            pbEffectsOnSwitchIn(user, false);                                 // :2326 user.pbEffectsOnSwitchIn
            pbEffectsOnSwitchIn(target, false);                               // :2327 target.pbEffectsOnSwitchIn
        }
    }

    /** {@code class PokeBattle_Move_068 < PokeBattle_Move} (Move_Effects_000-07F.rb:2336-2355): Gastro Acid. */
    public static class PokeBattle_Move_068 extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:2337-2347). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.unstoppableAbility(null)) {                            // :2338
                user.battle.display("但是失败了！");                             // :2339
                return true;                                                  // :2340
            }
            if (target.hasActiveItem("ABILITYSHIELD")) {                      // :2342
                user.battle.display(target.pbThis() + "的特性\n被特性护具的效果保护了！");   // :2343
                return true;                                                  // :2344
            }
            return false;                                                     // :2346
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:2349-2354). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.GastroAcid, true);           // :2350
            target.effects.set(PBEffects.Battler.Truant, false);              // :2351
            user.battle.display(target.pbThis() + "的特性被消除了！");             // :2352
            target.pbOnAbilityChanged(target.ability);                        // :2353
        }
    }

    /** {@code class PokeBattle_Move_069 < PokeBattle_Move} (Move_Effects_000-07F.rb:2362-2388): Transform. */
    public static class PokeBattle_Move_069 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:2363-2369). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.effects.truthy(PBEffects.Battler.Transform)) {           // :2364
                user.battle.display("但是失败了！");                             // :2365
                return true;                                                  // :2366
            }
            return false;                                                     // :2368
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:2371-2378). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.effects.truthy(PBEffects.Battler.Transform)            // :2372
                    || target.effects.truthy(PBEffects.Battler.Illusion)) {   // :2373
                user.battle.display("但是失败了！");                             // :2374
                return true;                                                  // :2375
            }
            return false;                                                     // :2377
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:2380-2382). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            pbTransform(user, target);                                        // :2381 user.pbTransform(target)
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} (:2384-2387). */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets, int hitNum,
                                    boolean showAnimation) {
            super.pbShowAnimation(move, id, user, targets, hitNum, showAnimation);   // :2385 super
            // 登记: Move_Effects_000-07F.rb:2386 @battle.scene.pbChangePokemon(user,targets[0].pokemon)
            //       —— 依赖 PokeBattle_Scene（本批未建模）。
        }
    }

    /** {@code class PokeBattle_Move_06A < PokeBattle_FixedDamageMove} (Move_Effects_000-07F.rb:2395-2399): Sonic Boom. */
    public static class PokeBattle_Move_06A extends MoveEffectsGeneric.PokeBattle_FixedDamageMove {

        /** {@code pbFixedDamage(user,target)} (:2396-2398). */
        @Override
        public int pbFixedDamage(BattleMove move, Battler user, Battler target) {
            return 20;                                                        // :2397
        }
    }

    /** {@code class PokeBattle_Move_06B < PokeBattle_FixedDamageMove} (Move_Effects_000-07F.rb:2406-2410): Dragon Rage. */
    public static class PokeBattle_Move_06B extends MoveEffectsGeneric.PokeBattle_FixedDamageMove {

        /** {@code pbFixedDamage(user,target)} (:2407-2409). */
        @Override
        public int pbFixedDamage(BattleMove move, Battler user, Battler target) {
            return 40;                                                        // :2408
        }
    }

    /** {@code class PokeBattle_Move_06C < PokeBattle_FixedDamageMove} (Move_Effects_000-07F.rb:2417-2422): Super Fang. */
    public static class PokeBattle_Move_06C extends MoveEffectsGeneric.PokeBattle_FixedDamageMove {

        /** {@code pbFixedDamage(user,target)} (:2418-2421). */
        @Override
        public int pbFixedDamage(BattleMove move, Battler user, Battler target) {
            double ret = target.pokemon.battleRank > 2                       // :2419
                    ? target.hp / 2.0 / 5.0 : target.hp / 2.0;
            return (int) Math.round(ret);                                     // :2420 return ret.round
        }
    }

    /** {@code class PokeBattle_Move_06D < PokeBattle_FixedDamageMove} (Move_Effects_000-07F.rb:2429-2433): Night Shade. */
    public static class PokeBattle_Move_06D extends MoveEffectsGeneric.PokeBattle_FixedDamageMove {

        /** {@code pbFixedDamage(user,target)} (:2430-2432). */
        @Override
        public int pbFixedDamage(BattleMove move, Battler user, Battler target) {
            return user.level();                                              // :2431
        }
    }

    /** {@code class PokeBattle_Move_06E < PokeBattle_FixedDamageMove} (Move_Effects_000-07F.rb:2440-2458): Endeavor. */
    public static class PokeBattle_Move_06E extends MoveEffectsGeneric.PokeBattle_FixedDamageMove {

        /** {@code pbFailsAgainstTarget?(user,target)} (:2441-2451). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.pokemon.battleRank > 1) {                              // :2442
                user.battle.display("但是对" + target.pbThis() + "没有效果！");     // :2443
                return true;                                                  // :2444
            }
            if (user.hp >= target.hp) {                                       // :2446
                user.battle.display("但是失败了！");                             // :2447
                return true;                                                  // :2448
            }
            return false;                                                     // :2450
        }

        /** {@code pbNumHits(user,targets); return 1; end} (:2453). */
        @Override
        public int pbNumHits(BattleMove move, Battler user, Array<Battler> targets) {
            return 1;                                                         // :2453
        }

        /** {@code pbFixedDamage(user,target)} (:2455-2457). */
        @Override
        public int pbFixedDamage(BattleMove move, Battler user, Battler target) {
            return target.hp - user.hp;                                       // :2456
        }
    }

    /** {@code class PokeBattle_Move_06F < PokeBattle_FixedDamageMove} (Move_Effects_000-07F.rb:2465-2471): Psywave. */
    public static class PokeBattle_Move_06F extends MoveEffectsGeneric.PokeBattle_FixedDamageMove {

        /** {@code pbFixedDamage(user,target)} (:2466-2470). */
        @Override
        public int pbFixedDamage(BattleMove move, Battler user, Battler target) {
            int min = user.level() / 2;                                       // :2467 (user.level/2).floor
            int max = user.level() * 3 / 2;                                   // :2468 (user.level*3/2).floor
            return min + user.battle.pbRandom(max - min + 1);                 // :2469
        }
    }

    /** {@code class PokeBattle_Move_070 < PokeBattle_FixedDamageMove} (Move_Effects_000-07F.rb:2478-2524): OHKO. */
    public static class PokeBattle_Move_070 extends MoveEffectsGeneric.PokeBattle_FixedDamageMove {

        /** {@code hitsDiggingTargets?} (:2479). */
        @Override
        public boolean hitsDiggingTargets(BattleMove move) {
            return "FISSURE".equals(move.internalName());                     // :2479 isConst?(@id,PBMoves,:FISSURE)
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:2481-2503). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.level() > user.level()                                // :2482
                    || (target.pokemon.battleRank > 2 && user.pbOwnedByPlayer())) {   // :2482
                user.battle.display(target.pbThis() + "没有受到影响！");           // :2483
                return true;                                                  // :2484
            }
            if (target.hasActiveAbility("STURDY") && !user.battle.moldBreaker) {   // :2486
                user.battle.showAbilitySplash(target);                        // :2487
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {           // :2488
                    user.battle.display("但是不能影响" + target.pbThis(true) + "！");   // :2489
                } else {
                    user.battle.display("但由于" + target.abilityName() + "，\n这不能影响" + target.pbThis(true) + "！");   // :2491-2492
                }
                user.battle.hideAbilitySplash(target);                        // :2494
                return true;                                                  // :2495
            }
            // 登记: Move_Effects_000-07F.rb:2498 isConst?(target.damageState.typeMod,PBTypes,:ICE) ——
            //       插件把「伤害倍率」当作属性 id 比较（typeMod 是 0/1/2/4…，不是类型），
            //       本运行时没有对应表达；照抄不修正，该分支恒为 false。
            boolean typeModIsIce = false;
            if (Battle.NEWEST_BATTLE_MECHANICS && typeModIsIce                // :2497-2498
                    && target.pbHasType("ICE")) {                             // :2499
                user.battle.display("但是失败了！");                             // :2500
                return true;                                                  // :2501
            }
            return false;                                                     // :2503
        }

        /** {@code pbAccuracyCheck(user,target)} (:2505-2512). */
        @Override
        public boolean pbAccuracyCheck(BattleMove move, Battler user, Battler target) {
            if (PendingApi.isCommander(target)) {                             // :2506 target.isCommander?
                return false;
            }
            if (target.effects.intVal(PBEffects.Battler.GlaiveRush) > 0) {    // :2507
                return true;
            }
            int acc = move.accuracy() + user.level() - target.level();        // :2508 @accuracy+user.level-target.level
            if (Battle.NEWEST_BATTLE_MECHANICS                               // :2509
                    && "SHEERCOLD".equals(move.internalName())                // :2510 isConst?(@id,PBMoves,:SHEERCOLD)
                    && !user.pbHasType("ICE")) {                              // :2510
                acc -= 10;                                                    // :2509
            }
            return user.battle.pbRandom(100) < acc;                           // :2511
        }

        /** {@code pbFixedDamage(user,target)} (:2514-2516). */
        @Override
        public int pbFixedDamage(BattleMove move, Battler user, Battler target) {
            return target.maxHp();                                            // :2515 target.totalhp
        }

        /** {@code pbHitEffectivenessMessages(user,target,numTargets=1)} (:2518-2523). */
        @Override
        public void pbHitEffectivenessMessages(BattleMove move, Battler user, Battler target, int numTargets) {
            super.pbHitEffectivenessMessages(move, user, target, numTargets);   // :2519 super
            if (target.fainted()) {                                           // :2520
                user.battle.display("一击必杀！");                              // :2521
            }
        }
    }

    /** {@code class PokeBattle_Move_071 < PokeBattle_FixedDamageMove} (Move_Effects_000-07F.rb:2531-2551): Counter. */
    public static class PokeBattle_Move_071 extends MoveEffectsGeneric.PokeBattle_FixedDamageMove {

        /** {@code pbAddTarget(targets,user)} (:2532-2536). */
        @Override
        public void pbAddTarget(BattleMove move, Array<Battler> targets, Battler user) {
            int t = user.effects.intVal(PBEffects.Battler.CounterTarget);     // :2533
            if (t < 0 || !user.opposes(t)) {                                  // :2534
                return;
            }
            battlerPbAddTarget(user, targets, user.battle.battlerAt(t), move, false);   // :2535 user.pbAddTarget(targets,user,@battle.battlers[t],self,false)
        }

        /** {@code pbMoveFailed?(user,targets)} (:2538-2544). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (targets.size == 0) {                                          // :2539
                user.battle.display("但是没有目标……");                          // :2540
                return true;                                                  // :2541
            }
            return false;                                                     // :2543
        }

        /** {@code pbFixedDamage(user,target)} (:2546-2550). */
        @Override
        public int pbFixedDamage(BattleMove move, Battler user, Battler target) {
            int dmg = user.effects.intVal(PBEffects.Battler.Counter) * 2;     // :2547
            if (dmg == 0) {                                                   // :2548
                dmg = 1;
            }
            return dmg;                                                       // :2549
        }
    }

    /** {@code class PokeBattle_Move_072 < PokeBattle_FixedDamageMove} (Move_Effects_000-07F.rb:2559-2579): Mirror Coat. */
    public static class PokeBattle_Move_072 extends MoveEffectsGeneric.PokeBattle_FixedDamageMove {

        /** {@code pbAddTarget(targets,user)} (:2560-2564). */
        @Override
        public void pbAddTarget(BattleMove move, Array<Battler> targets, Battler user) {
            int t = user.effects.intVal(PBEffects.Battler.MirrorCoatTarget);   // :2561
            if (t < 0 || !user.opposes(t)) {                                  // :2562
                return;
            }
            battlerPbAddTarget(user, targets, user.battle.battlerAt(t), move, false);   // :2563
        }

        /** {@code pbMoveFailed?(user,targets)} (:2566-2572). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (targets.size == 0) {                                          // :2567
                user.battle.display("但是没有目标……");                          // :2568
                return true;                                                  // :2569
            }
            return false;                                                     // :2571
        }

        /** {@code pbFixedDamage(user,target)} (:2574-2578). */
        @Override
        public int pbFixedDamage(BattleMove move, Battler user, Battler target) {
            int dmg = user.effects.intVal(PBEffects.Battler.MirrorCoat) * 2;   // :2575
            if (dmg == 0) {                                                   // :2576
                dmg = 1;
            }
            return dmg;                                                       // :2577
        }
    }

    /** {@code class PokeBattle_Move_073 < PokeBattle_FixedDamageMove} (Move_Effects_000-07F.rb:2587-2608): Metal Burst. */
    public static class PokeBattle_Move_073 extends MoveEffectsGeneric.PokeBattle_FixedDamageMove {

        /** {@code pbAddTarget(targets,user)} (:2588-2593). */
        @Override
        public void pbAddTarget(BattleMove move, Array<Battler> targets, Battler user) {
            if (user.lastFoeAttacker.size == 0) {                             // :2589
                return;
            }
            // :2590 lastAttacker = user.lastFoeAttacker.last - this runtime keeps the
            //       Battlers themselves in that array (Battler.java:520).
            Battler lastAttacker = user.lastFoeAttacker.peek();
            if (lastAttacker == null || !user.opposes(lastAttacker)) {         // :2591 lastAttacker<0 || !user.opposes?(lastAttacker)
                return;
            }
            battlerPbAddTarget(user, targets, lastAttacker, move, false);            // :2592
        }

        /** {@code pbMoveFailed?(user,targets)} (:2595-2601). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (targets.size == 0) {                                          // :2596
                user.battle.display("但是没有目标……");                          // :2597
                return true;                                                  // :2598
            }
            return false;                                                     // :2600
        }

        /** {@code pbFixedDamage(user,target)} (:2603-2607). */
        @Override
        public int pbFixedDamage(BattleMove move, Battler user, Battler target) {
            int dmg = (int) Math.floor(user.lastHPLostFromFoe * 1.5);         // :2604 (user.lastHPLostFromFoe*1.5).floor
            if (dmg == 0) {                                                   // :2605
                dmg = 1;
            }
            return dmg;                                                       // :2606
        }
    }

    /** {@code class PokeBattle_Move_074 < PokeBattle_Move} (Move_Effects_000-07F.rb:2615-2643): Flame Burst. */
    public static class PokeBattle_Move_074 extends MoveEffectBase {

        /** {@code pbEffectWhenDealingDamage(user,target)} (:2616-2642). */
        @Override
        public void pbEffectWhenDealingDamage(BattleMove move, Battler user, Battler target) {
            Array<int[]> hitAlly = new Array<>();                             // :2617
            for (Battler b : target.allAllies()) {                            // :2618 target.eachAlly do |b|
                if (!b.near(target.index)) {                                  // :2619
                    continue;
                }
                hitAlly.add(new int[] {b.index, b.hp});                       // :2620 hitAlly.push([b.index,b.hp])
                b.pbReduceHP(b.maxHp() / 16, false, true, true);        // :2621 b.pbReduceHP(b.totalhp/16,false)
            }
            if (hitAlly.size == 2) {                                          // :2623
                user.battle.display("溅射的火焰击中了"                            // :2624-2625
                        + user.battle.battlerAt(hitAlly.get(0)[0]).pbThis(true) + "和"
                        + user.battle.battlerAt(hitAlly.get(1)[0]).pbThis(true) + "！");
            } else if (hitAlly.size > 0) {                                    // :2627
                for (int[] b : hitAlly) {                                     // :2628 hitAlly.each do |b|
                    user.battle.display("溅射的火焰击中了"                      // :2629-2630
                            + user.battle.battlerAt(b[0]).pbThis(true) + "！");
                }
            }
            Array<Battler> switchedAlly = new Array<>();                      // :2633 switchedAlly = []
            for (int[] b : hitAlly) {                                         // :2634 hitAlly.each do |b|
                Battler ally = user.battle.battlerAt(b[0]);                   // :2635 @battle.battlers[b[0]]
                ally.pbItemHPHealCheck(0, false);                     // :2636 .pbItemHPHealCheck
                if (ally.pbAbilitiesOnDamageTaken(b[1], ally.hp)) {           // :2637 .pbAbilitiesOnDamageTaken(b[1])
                    switchedAlly.add(ally);                                   // :2638
                }
            }
            for (Battler b : switchedAlly) {                                  // :2640 switchedAlly.each { |b| ... }
                pbEffectsOnSwitchIn(b, true);                                 // :2640 b.pbEffectsOnSwitchIn(true)
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_075 < PokeBattle_Move}
     * (Move_Effects_000-07F.rb:2651-2669): Surf.
     */
    public static class PokeBattle_Move_075 extends MoveEffectBase {

        /** {@code hitsDivingTargets?} (:2652). */
        @Override
        public boolean hitsDivingTargets(BattleMove move) {
            return true;                                                      // :2652
        }

        /** {@code pbModifyDamage(damageMult,user,target)} (:2654-2657). */
        @Override
        public float pbModifyDamage(BattleMove move, float damageMult, Battler user, Battler target) {
            if (inTwoTurnAttack(target, "0CB")) {                             // :2655 target.inTwoTurnAttack?("0CB")  # Dive
                damageMult *= 2;
            }
            return damageMult;                                                // :2656
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:2659-2668). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (!target.damageState.unaffected                               // :2660
                    && !target.damageState.protectedFlag                     // :2661 target.damageState.protected
                    && !target.damageState.missed                            // :2661
                    && user.isSpecies("CRAMORANT")                           // :2662 isConst?(user.species,PBSpecies,:CRAMORANT)
                    && user.hasActiveAbility("GULPMISSILE")                   // :2663
                    && user.form() == 0) {                                   // :2663
                setForm(user, 2);                                             // :2664 user.form=2
                if (user.hp > user.maxHp() / 2) {                             // :2665 user.form=1 if user.hp>(user.totalhp/2)
                    setForm(user, 1);
                }
                // 登记: Move_Effects_000-07F.rb:2666 @battle.scene.pbChangePokemon(user,user.pokemon)
                //       —— 依赖 PokeBattle_Scene（本批未建模）。
            }
        }
    }

    /** {@code class PokeBattle_Move_076 < PokeBattle_Move} (Move_Effects_000-07F.rb:2678-2686): Earthquake. */
    public static class PokeBattle_Move_076 extends MoveEffectBase {

        /** {@code hitsDiggingTargets?} (:2678). */
        @Override
        public boolean hitsDiggingTargets(BattleMove move) {
            return true;                                                      // :2678
        }

        /** {@code pbModifyDamage(damageMult,user,target)} (:2680-2684). */
        @Override
        public float pbModifyDamage(BattleMove move, float damageMult, Battler user, Battler target) {
            if (inTwoTurnAttack(target, "0CA")) {                             // :2681 target.inTwoTurnAttack?("0CA")  # Dig
                damageMult *= 2;
            }
            if (user.battle.field.terrain == PBBattleTerrains.Grassy) {       // :2682
                damageMult /= 2;
            }
            return damageMult;                                                // :2683
        }
    }

    /** {@code class PokeBattle_Move_077 < PokeBattle_Move} (Move_Effects_000-07F.rb:2694-2702): Gust. */
    public static class PokeBattle_Move_077 extends MoveEffectBase {

        /** {@code hitsFlyingTargets?} (:2695). */
        @Override
        public boolean hitsFlyingTargets(BattleMove move) {
            return true;                                                      // :2695
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:2697-2700). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (inTwoTurnAttack(target, "0C9", "0CC", "0CE")                 // :2698 Fly/Bounce/Sky Drop
                    || target.effects.intVal(PBEffects.Battler.SkyDrop) >= 0) {   // :2699
                baseDmg *= 2;
            }
            return baseDmg;                                                   // :2700
        }
    }

    /** {@code class PokeBattle_Move_078 < PokeBattle_FlinchMove} (Move_Effects_000-07F.rb:2710-2718): Twister. */
    public static class PokeBattle_Move_078 extends MoveEffectsGeneric.PokeBattle_FlinchMove {

        /** {@code hitsFlyingTargets?} (:2711). */
        @Override
        public boolean hitsFlyingTargets(BattleMove move) {
            return true;                                                      // :2711
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:2713-2716). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (inTwoTurnAttack(target, "0C9", "0CC", "0CE")                 // :2714
                    || target.effects.intVal(PBEffects.Battler.SkyDrop) >= 0) {   // :2715
                baseDmg *= 2;
            }
            return baseDmg;                                                   // :2716
        }
    }

    /** {@code class PokeBattle_Move_079 < PokeBattle_Move} (Move_Effects_000-07F.rb:2725-2744): Fusion Bolt. */
    public static class PokeBattle_Move_079 extends MoveEffectBase {

        /** {@code @doublePower} (:2726). */
        private boolean doublePower;

        /** {@code pbChangeUsageCounters(user,specialUsage)} (:2725-2728). */
        @Override
        public void pbChangeUsageCounters(BattleMove move, Battler user, boolean specialUsage) {
            doublePower = user.battle.field.effects.truthy(PBEffects.Field.FusionFlare);   // :2726 @doublePower = @battle.field.effects[PBEffects::FusionFlare]
            super.pbChangeUsageCounters(move, user, specialUsage);            // :2727 super
        }

        /** {@code pbBaseDamageMultiplier(damageMult,user,target)} (:2730-2733). */
        @Override
        public float pbBaseDamageMultiplier(BattleMove move, float damageMult, Battler user, Battler target) {
            if (doublePower) {                                                // :2731
                damageMult *= 2;
            }
            return damageMult;                                                // :2732
        }

        /** {@code pbEffectGeneral(user)} (:2735-2737). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.battle.field.effects.set(PBEffects.Field.FusionBolt, true);  // :2736
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} (:2739-2743). */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets, int hitNum,
                                    boolean showAnimation) {
            int animHitNum = hitNum;                                          // :2740
            if ((targets.size > 0 && targets.get(0).damageState.critical)     // :2741
                    || doublePower) {                                         // :2741 Charged anim
                animHitNum = 1;
            }
            super.pbShowAnimation(move, id, user, targets, animHitNum, showAnimation);   // :2742 super
        }
    }

    /** {@code class PokeBattle_Move_07A < PokeBattle_Move} (Move_Effects_000-07F.rb:2752-2771): Fusion Flare. */
    public static class PokeBattle_Move_07A extends MoveEffectBase {

        /** {@code @doublePower} (:2753). */
        private boolean doublePower;

        /** {@code pbChangeUsageCounters(user,specialUsage)} (:2752-2755). */
        @Override
        public void pbChangeUsageCounters(BattleMove move, Battler user, boolean specialUsage) {
            doublePower = user.battle.field.effects.truthy(PBEffects.Field.FusionBolt);   // :2753
            super.pbChangeUsageCounters(move, user, specialUsage);            // :2754 super
        }

        /** {@code pbBaseDamageMultiplier(damageMult,user,target)} (:2757-2760). */
        @Override
        public float pbBaseDamageMultiplier(BattleMove move, float damageMult, Battler user, Battler target) {
            if (doublePower) {                                                // :2758
                damageMult *= 2;
            }
            return damageMult;                                                // :2759
        }

        /** {@code pbEffectGeneral(user)} (:2762-2764). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.battle.field.effects.set(PBEffects.Field.FusionFlare, true);   // :2763
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} (:2766-2770). */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets, int hitNum,
                                    boolean showAnimation) {
            int animHitNum = hitNum;                                          // :2767
            if ((targets.size > 0 && targets.get(0).damageState.critical)     // :2768
                    || doublePower) {                                         // :2768
                animHitNum = 1;
            }
            super.pbShowAnimation(move, id, user, targets, animHitNum, showAnimation);   // :2769 super
        }
    }

    /** {@code class PokeBattle_Move_07B < PokeBattle_Move} (Move_Effects_000-07F.rb:2779-2786): Venoshock. */
    public static class PokeBattle_Move_07B extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:2780-2785). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (target.poisoned()                                            // :2781
                    && (target.effects.intVal(PBEffects.Battler.Substitute) == 0   // :2782
                    || ignoresSubstitute(move, user))) {                      // :2782
                baseDmg *= 2;                                                 // :2783
            }
            return baseDmg;                                                   // :2784
        }
    }

    /** {@code class PokeBattle_Move_07C < PokeBattle_Move} (Move_Effects_000-07F.rb:2795-2809): Smelling Salts. */
    public static class PokeBattle_Move_07C extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:2796-2801). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (target.paralyzed()                                           // :2797
                    && (target.effects.intVal(PBEffects.Battler.Substitute) == 0   // :2798
                    || ignoresSubstitute(move, user))) {                      // :2798
                baseDmg *= 2;                                                 // :2799
            }
            return baseDmg;                                                   // :2800
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:2803-2808). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (target.fainted()) {                                           // :2804
                return;
            }
            if (target.damageState.unaffected || target.damageState.substitute) {   // :2805
                return;
            }
            if (!target.hasStatus("PARALYSIS")) {                             // :2806 target.status!=PBStatuses::PARALYSIS
                return;
            }
            target.pbCureStatus();                                            // :2807
        }
    }

    /** {@code class PokeBattle_Move_07D < PokeBattle_Move} (Move_Effects_000-07F.rb:2817-2831): Wake-Up Slap. */
    public static class PokeBattle_Move_07D extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:2818-2823). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (target.asleep()                                              // :2819
                    && (target.effects.intVal(PBEffects.Battler.Substitute) == 0   // :2820
                    || ignoresSubstitute(move, user))) {                      // :2820
                baseDmg *= 2;                                                 // :2821
            }
            return baseDmg;                                                   // :2822
        }

        /**
         * {@code pbEffectAfterAllHits(user,target)} (:2825-2830); reopened by
         * {@code Arceus:339-345} (wins - it also cures Drowsy).
         */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (target.fainted()) {                                           // Arceus:340
                return;
            }
            if (target.damageState.unaffected || target.damageState.substitute) {   // Arceus:341
                return;
            }
            if (!target.hasStatus("SLEEP") && !target.hasStatus("DROWSY")) {  // Arceus:342
                return;
            }
            target.pbCureStatus();                                            // Arceus:343
        }
    }

    /** {@code class PokeBattle_Move_07E < PokeBattle_Move} (Move_Effects_000-07F.rb:2840-2846): Facade. */
    public static class PokeBattle_Move_07E extends MoveEffectBase {

        /** {@code damageReducedByBurn?; return !NEWEST_BATTLE_MECHANICS; end} (:2840). */
        @Override
        public boolean damageReducedByBurn(BattleMove move) {
            return !Battle.NEWEST_BATTLE_MECHANICS;                           // :2840
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:2842-2845). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (user.poisoned() || user.burned() || user.paralyzed()) {        // :2843
                baseDmg *= 2;
            }
            return baseDmg;                                                   // :2844
        }
    }

    /** {@code class PokeBattle_Move_07F < PokeBattle_Move} (Move_Effects_000-07F.rb:2854-2860): Hex. */
    public static class PokeBattle_Move_07F extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:2855-2859). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (target.pbHasAnyStatus()                                      // :2856
                    && (target.effects.intVal(PBEffects.Battler.Substitute) == 0   // :2857
                    || ignoresSubstitute(move, user))) {                      // :2857
                baseDmg *= 2;                                                 // :2858
            }
            return baseDmg;                                                   // :2859
        }
    }

    /**
     * {@code PokeBattle_Move.pbFromPBMove} (PokeBattle_Move.rb:51-59) for the
     * 128 function-code classes this file holds: the Ruby builds
     * {@code "PokeBattle_Move_#{function}"} and instantiates it on demand, so each
     * code is bound to its no-argument constructor here and
     * {@link MoveEffectRegistry#of} calls it lazily (some constructors still touch
     * not-yet-wired stubs, which must not break the whole table).
     *
     * <p>Generated by {@code __l2register.mjs} - re-run it with {@code --write}
     * after the class set changes.</p>
     */
    static void register() {
        MoveEffectRegistry.register("000", PokeBattle_Move_000::new);
        MoveEffectRegistry.register("001", PokeBattle_Move_001::new);
        MoveEffectRegistry.register("002", PokeBattle_Move_002::new);
        MoveEffectRegistry.register("003", PokeBattle_Move_003::new);
        MoveEffectRegistry.register("004", PokeBattle_Move_004::new);
        MoveEffectRegistry.register("005", PokeBattle_Move_005::new);
        MoveEffectRegistry.register("006", PokeBattle_Move_006::new);
        MoveEffectRegistry.register("007", PokeBattle_Move_007::new);
        MoveEffectRegistry.register("008", PokeBattle_Move_008::new);
        MoveEffectRegistry.register("009", PokeBattle_Move_009::new);
        MoveEffectRegistry.register("00A", PokeBattle_Move_00A::new);
        MoveEffectRegistry.register("00B", PokeBattle_Move_00B::new);
        MoveEffectRegistry.register("00C", PokeBattle_Move_00C::new);
        MoveEffectRegistry.register("00D", PokeBattle_Move_00D::new);
        MoveEffectRegistry.register("00E", PokeBattle_Move_00E::new);
        MoveEffectRegistry.register("00F", PokeBattle_Move_00F::new);
        MoveEffectRegistry.register("010", PokeBattle_Move_010::new);
        MoveEffectRegistry.register("011", PokeBattle_Move_011::new);
        MoveEffectRegistry.register("012", PokeBattle_Move_012::new);
        MoveEffectRegistry.register("013", PokeBattle_Move_013::new);
        MoveEffectRegistry.register("014", PokeBattle_Move_014::new);
        MoveEffectRegistry.register("015", PokeBattle_Move_015::new);
        MoveEffectRegistry.register("016", PokeBattle_Move_016::new);
        MoveEffectRegistry.register("017", PokeBattle_Move_017::new);
        MoveEffectRegistry.register("018", PokeBattle_Move_018::new);
        MoveEffectRegistry.register("019", PokeBattle_Move_019::new);
        MoveEffectRegistry.register("01A", PokeBattle_Move_01A::new);
        MoveEffectRegistry.register("01B", PokeBattle_Move_01B::new);
        MoveEffectRegistry.register("01C", PokeBattle_Move_01C::new);
        MoveEffectRegistry.register("01D", PokeBattle_Move_01D::new);
        MoveEffectRegistry.register("01E", PokeBattle_Move_01E::new);
        MoveEffectRegistry.register("01F", PokeBattle_Move_01F::new);
        MoveEffectRegistry.register("020", PokeBattle_Move_020::new);
        MoveEffectRegistry.register("021", PokeBattle_Move_021::new);
        MoveEffectRegistry.register("022", PokeBattle_Move_022::new);
        MoveEffectRegistry.register("023", PokeBattle_Move_023::new);
        MoveEffectRegistry.register("024", PokeBattle_Move_024::new);
        MoveEffectRegistry.register("025", PokeBattle_Move_025::new);
        MoveEffectRegistry.register("026", PokeBattle_Move_026::new);
        MoveEffectRegistry.register("027", PokeBattle_Move_027::new);
        MoveEffectRegistry.register("028", PokeBattle_Move_028::new);
        MoveEffectRegistry.register("029", PokeBattle_Move_029::new);
        MoveEffectRegistry.register("02A", PokeBattle_Move_02A::new);
        MoveEffectRegistry.register("02B", PokeBattle_Move_02B::new);
        MoveEffectRegistry.register("02C", PokeBattle_Move_02C::new);
        MoveEffectRegistry.register("02D", PokeBattle_Move_02D::new);
        MoveEffectRegistry.register("02E", PokeBattle_Move_02E::new);
        MoveEffectRegistry.register("02F", PokeBattle_Move_02F::new);
        MoveEffectRegistry.register("030", PokeBattle_Move_030::new);
        MoveEffectRegistry.register("031", PokeBattle_Move_031::new);
        MoveEffectRegistry.register("032", PokeBattle_Move_032::new);
        MoveEffectRegistry.register("033", PokeBattle_Move_033::new);
        MoveEffectRegistry.register("034", PokeBattle_Move_034::new);
        MoveEffectRegistry.register("035", PokeBattle_Move_035::new);
        MoveEffectRegistry.register("036", PokeBattle_Move_036::new);
        MoveEffectRegistry.register("037", PokeBattle_Move_037::new);
        MoveEffectRegistry.register("038", PokeBattle_Move_038::new);
        MoveEffectRegistry.register("039", PokeBattle_Move_039::new);
        MoveEffectRegistry.register("03A", PokeBattle_Move_03A::new);
        MoveEffectRegistry.register("03B", PokeBattle_Move_03B::new);
        MoveEffectRegistry.register("03C", PokeBattle_Move_03C::new);
        MoveEffectRegistry.register("03D", PokeBattle_Move_03D::new);
        MoveEffectRegistry.register("03E", PokeBattle_Move_03E::new);
        MoveEffectRegistry.register("03F", PokeBattle_Move_03F::new);
        MoveEffectRegistry.register("040", PokeBattle_Move_040::new);
        MoveEffectRegistry.register("041", PokeBattle_Move_041::new);
        MoveEffectRegistry.register("042", PokeBattle_Move_042::new);
        MoveEffectRegistry.register("043", PokeBattle_Move_043::new);
        MoveEffectRegistry.register("044", PokeBattle_Move_044::new);
        MoveEffectRegistry.register("045", PokeBattle_Move_045::new);
        MoveEffectRegistry.register("046", PokeBattle_Move_046::new);
        MoveEffectRegistry.register("047", PokeBattle_Move_047::new);
        MoveEffectRegistry.register("048", PokeBattle_Move_048::new);
        MoveEffectRegistry.register("049", PokeBattle_Move_049::new);
        MoveEffectRegistry.register("04A", PokeBattle_Move_04A::new);
        MoveEffectRegistry.register("04B", PokeBattle_Move_04B::new);
        MoveEffectRegistry.register("04C", PokeBattle_Move_04C::new);
        MoveEffectRegistry.register("04D", PokeBattle_Move_04D::new);
        MoveEffectRegistry.register("04E", PokeBattle_Move_04E::new);
        MoveEffectRegistry.register("04F", PokeBattle_Move_04F::new);
        MoveEffectRegistry.register("050", PokeBattle_Move_050::new);
        MoveEffectRegistry.register("051", PokeBattle_Move_051::new);
        MoveEffectRegistry.register("052", PokeBattle_Move_052::new);
        MoveEffectRegistry.register("053", PokeBattle_Move_053::new);
        MoveEffectRegistry.register("054", PokeBattle_Move_054::new);
        MoveEffectRegistry.register("055", PokeBattle_Move_055::new);
        MoveEffectRegistry.register("056", PokeBattle_Move_056::new);
        MoveEffectRegistry.register("057", PokeBattle_Move_057::new);
        MoveEffectRegistry.register("058", PokeBattle_Move_058::new);
        MoveEffectRegistry.register("059", PokeBattle_Move_059::new);
        MoveEffectRegistry.register("05A", PokeBattle_Move_05A::new);
        MoveEffectRegistry.register("05B", PokeBattle_Move_05B::new);
        MoveEffectRegistry.register("05C", PokeBattle_Move_05C::new);
        MoveEffectRegistry.register("05D", PokeBattle_Move_05D::new);
        MoveEffectRegistry.register("05E", PokeBattle_Move_05E::new);
        MoveEffectRegistry.register("05F", PokeBattle_Move_05F::new);
        MoveEffectRegistry.register("060", PokeBattle_Move_060::new);
        MoveEffectRegistry.register("061", PokeBattle_Move_061::new);
        MoveEffectRegistry.register("062", PokeBattle_Move_062::new);
        MoveEffectRegistry.register("063", PokeBattle_Move_063::new);
        MoveEffectRegistry.register("064", PokeBattle_Move_064::new);
        MoveEffectRegistry.register("065", PokeBattle_Move_065::new);
        MoveEffectRegistry.register("066", PokeBattle_Move_066::new);
        MoveEffectRegistry.register("067", PokeBattle_Move_067::new);
        MoveEffectRegistry.register("068", PokeBattle_Move_068::new);
        MoveEffectRegistry.register("069", PokeBattle_Move_069::new);
        MoveEffectRegistry.register("06A", PokeBattle_Move_06A::new);
        MoveEffectRegistry.register("06B", PokeBattle_Move_06B::new);
        MoveEffectRegistry.register("06C", PokeBattle_Move_06C::new);
        MoveEffectRegistry.register("06D", PokeBattle_Move_06D::new);
        MoveEffectRegistry.register("06E", PokeBattle_Move_06E::new);
        MoveEffectRegistry.register("06F", PokeBattle_Move_06F::new);
        MoveEffectRegistry.register("070", PokeBattle_Move_070::new);
        MoveEffectRegistry.register("071", PokeBattle_Move_071::new);
        MoveEffectRegistry.register("072", PokeBattle_Move_072::new);
        MoveEffectRegistry.register("073", PokeBattle_Move_073::new);
        MoveEffectRegistry.register("074", PokeBattle_Move_074::new);
        MoveEffectRegistry.register("075", PokeBattle_Move_075::new);
        MoveEffectRegistry.register("076", PokeBattle_Move_076::new);
        MoveEffectRegistry.register("077", PokeBattle_Move_077::new);
        MoveEffectRegistry.register("078", PokeBattle_Move_078::new);
        MoveEffectRegistry.register("079", PokeBattle_Move_079::new);
        MoveEffectRegistry.register("07A", PokeBattle_Move_07A::new);
        MoveEffectRegistry.register("07B", PokeBattle_Move_07B::new);
        MoveEffectRegistry.register("07C", PokeBattle_Move_07C::new);
        MoveEffectRegistry.register("07D", PokeBattle_Move_07D::new);
        MoveEffectRegistry.register("07E", PokeBattle_Move_07E::new);
        MoveEffectRegistry.register("07F", PokeBattle_Move_07F::new);
    }

}
