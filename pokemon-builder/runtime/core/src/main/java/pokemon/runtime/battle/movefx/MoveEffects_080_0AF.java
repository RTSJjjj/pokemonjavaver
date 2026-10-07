package pokemon.runtime.battle.movefx;

import com.badlogic.gdx.utils.Array;

import pokemon.runtime.battle.Battle;
import pokemon.runtime.battle.BattleMove;
import pokemon.runtime.battle.Battler;
import pokemon.runtime.battle.PBBattleTerrains;
import pokemon.runtime.battle.PBEffects;
import pokemon.runtime.battle.PBEnvironment;
import pokemon.runtime.battle.PBStats;
import pokemon.runtime.battle.PBTypes;
import pokemon.runtime.battle.PBWeather;
import pokemon.runtime.battle.PendingApi;
import pokemon.runtime.pokemon.PbsData;

/**
 * Stage 4 / L2: the function-code classes {@code 080}-{@code 0AF} (48 of them) of
 * the plugin's {@code Move_Effects_080-0FF.rb} (3921 lines), transcribed one class
 * at a time.
 *
 * <p>The rest of that Ruby file lives in the sibling files of this package:
 * {@code MoveEffects_0B0_0D4.java} ({@code 0B0}-{@code 0D4}) and
 * {@code MoveEffects_0D5_0FF.java} ({@code 0D5}-{@code 0FF}). The split keeps one
 * writer per file; {@code extends} across the ranges uses the fully qualified
 * nested name ({@code MoveEffects_080_0AF.PokeBattle_Move_0xx}).</p>
 *
 * <h2>Shape</h2>
 * Each Ruby {@code class PokeBattle_Move_XXX < PokeBattle_Yyy} becomes a nested
 * {@code public static class PokeBattle_Move_XXX} extending
 * {@link MoveEffectsGeneric}'s matching base (or {@link MoveEffectBase} when the
 * parent is {@code PokeBattle_Move} itself). Only the hooks the class really
 * overrides are overridden, each with its {@code // Move_Effects_080-0FF.rb:行号}
 * comment; constructors transcribe the Ruby {@code initialize} line by line. The
 * strategy object is shared by every move with the same function code (task-11
 * decision 4), so function-code-describing values set by {@code initialize}
 * ({@code @statUp}, {@code @targets}, {@code @power}, ...) become fields of the
 * class, set in its constructor.
 *
 * <h2>Translations used in this file</h2>
 * <ul>
 * <li>Type identity is the internal-name {@code String}, so
 *     {@code getConst(PBTypes,:X)} is the literal {@code "X"} and
 *     {@code getID(PBTypes,:X)} likewise; the plugin's id lists (Hidden Power's
 *     type table) are rebuilt as name lists in id order.</li>
 * <li>Item identity is the internal-name {@code String} too, so
 *     {@code user.item==0} is {@code user.item.isEmpty()} (see
 *     {@code HandlerHash}'s documented deviation).</li>
 * <li>{@code user.stages[s]} (plugin {@code PBStats} numbering) is
 *     {@code user.stage(s)} (the accessor {@code Battler} landed for exactly this
 *     mismatch).</li>
 * <li>{@code user.happiness}/{@code pkmn.iv} are {@code user.pokemon.happiness}/
 *     {@code user.pokemon.ivs}.</li>
 * <li>{@code @battle.choices[i][0]} is this runtime's {@code Battle.choices(i)}
 *     action string ({@code ":UseMove"}/{@code ":Shift"}/{@code ":None"}); see
 *     {@link #choiceIsUseMove} / {@link #choiceIsNone}.</li>
 * <li>{@code super} calls become {@code super.<hook>(...)} - the bases in
 *     {@link MoveEffectsGeneric} already carry the plugin's bodies.</li>
 * <li>{@code pbHiddenPower} (:280-311) and {@code choose_best_type} (:843) are
 *     TOP-LEVEL functions in the plugin, not class hooks, and are transcribed as
 *     private helpers of this class.</li>
 * </ul>
 */
public final class MoveEffects_080_0AF {

    private MoveEffects_080_0AF() {
    }

    // ==================================================================
    // Move_Effects_080-0FF.rb:3-311 - power/type oddities (080-090)
    // ==================================================================

    /** {@code class PokeBattle_Move_080 < PokeBattle_Move} (:3-8): power doubles below half HP. */
    public static class PokeBattle_Move_080 extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:4-7). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (target.hp <= target.maxHp() / 2) {                           // :5 target.hp<=target.totalhp/2
                baseDmg *= 2;
            }
            return baseDmg;                                                  // :6
        }
    }

    /**
     * {@code class PokeBattle_Move_081 < PokeBattle_Move} (:16-21): power doubles
     * if the user lost HP to the target's move this round (Avalanche, Revenge).
     */
    public static class PokeBattle_Move_081 extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:17-20). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            for (Battler attacker : user.lastAttacker) {                     // :18 lastAttacker.include?(target.index)
                if (attacker != null && attacker.index == target.index) {
                    baseDmg *= 2;
                    break;
                }
            }
            return baseDmg;                                                  // :19
        }
    }

    /** {@code class PokeBattle_Move_082 < PokeBattle_Move} (:28-33): Assurance. */
    public static class PokeBattle_Move_082 extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:29-32). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (target.tookDamage) {                                         // :30
                baseDmg *= 2;
            }
            return baseDmg;                                                  // :31
        }
    }

    /**
     * {@code class PokeBattle_Move_083 < PokeBattle_Move} (:41-57): Round - the
     * ally that already used it doubles the power and goes next.
     */
    public static class PokeBattle_Move_083 extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:42-45). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (user.pbOwnSide().effects.truthy(PBEffects.Side.Round)) {     // :43
                baseDmg *= 2;
            }
            return baseDmg;                                                  // :44
        }

        /** {@code pbEffectGeneral(user)} (:47-56). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.pbOwnSide().effects.set(PBEffects.Side.Round, true);        // :48
            for (Battler b : user.allAllies()) {              // :49 eachAlly
                // :50 next if choices[b.index][0]!=:UseMove || b.movedThisRound?
                if (!choiceIsUseMove(user.battle, b.index)
                        || b.movedThisRound()) {
                    continue;
                }
                BattleMove other = (BattleMove) user.battle.choices(b.index)[2];   // :51
                if (other == null || !other.function().equals(move.function())) {   // :51 .function!=@function
                    continue;
                }
                b.effects.set(PBEffects.Battler.MoveNext, true);             // :52
                b.effects.set(PBEffects.Battler.Quash, 0);                   // :53
                break;                                                       // :54
            }
        }
    }

    /** {@code class PokeBattle_Move_084 < PokeBattle_Move} (:64-73): Payback. */
    public static class PokeBattle_Move_084 extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:65-72). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            Battle battle = user.battle;
            // :66-68 choices[target.index][0]!=:None && ((!=:UseMove && !=:Shift) || movedThisRound?)
            if (!choiceIsNone(battle, target.index)
                    && ((!choiceIsMoveOrShift(battle, target.index))
                        || target.movedThisRound())) {
                baseDmg *= 2;                                                // :69
            }
            return baseDmg;                                                  // :71
        }
    }

    /** {@code class PokeBattle_Move_085 < PokeBattle_Move} (:80-86): Retaliate. */
    public static class PokeBattle_Move_085 extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:81-85). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            int lastRoundFainted =
                    user.pbOwnSide().effects.intVal(PBEffects.Side.LastRoundFainted);   // :82
            if (lastRoundFainted >= 0                                            // :83
                    && lastRoundFainted == battle(user).turnCount() - 1) {
                baseDmg *= 2;
            }
            return baseDmg;                                                  // :84
        }
    }

    /** {@code class PokeBattle_Move_086 < PokeBattle_Move} (:93-98): Acrobatics. */
    public static class PokeBattle_Move_086 extends MoveEffectBase {

        /** {@code pbBaseDamageMultiplier(damageMult,user,target)} (:94-97). */
        @Override
        public float pbBaseDamageMultiplier(BattleMove move, float damageMult, Battler user, Battler target) {
            if (user.item.isEmpty()) {                                       // :95 user.item==0
                damageMult *= 2;
            }
            return damageMult;                                               // :96
        }
    }

    /** {@code class PokeBattle_Move_087 < PokeBattle_Move} (:105-158): Weather Ball. */
    public static class PokeBattle_Move_087 extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:106-122). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            // 优先级 1：超级太阳特性直接翻倍威力（无视天气）(:107-110)
            if (user.hasActiveAbility("SUPERSUN")) {                         // :108
                return baseDmg * 2;                                          // :109
            }
            // 优先级 2：常规天气威力翻倍逻辑 (:112-120)
            int weather = battle(user).pbWeather();                          // :113
            if (weather != PBWeather.None && weather != PBWeather.StrongWinds) {
                if (weather == PBWeather.Sandstorm || weather == PBWeather.Hail     // :114-115
                        || weather == PBWeather.Snow || weather == PBWeather.Fog) {
                    baseDmg *= 2;                                            // :116
                } else if (!user.hasUtilityUmbrella()) {                     // :117
                    baseDmg *= 2;                                            // :118
                }
            }
            return baseDmg;                                                  // :121
        }

        /** {@code pbBaseType(user)} (:124-148). */
        @Override
        public String pbBaseType(BattleMove move, Battler user) {
            // 优先级 1：超级太阳强制变为火属性，不论当前是什么天气 (:125-128)
            if (user.hasActiveAbility("SUPERSUN")) {                         // :126
                return "FIRE";                                               // :127 getConst(PBTypes,:FIRE)
            }
            String ret = "NORMAL";                                           // :131 getID(PBTypes,:NORMAL)
            switch (battle(user).pbWeather()) {                              // :132
                case PBWeather.Sun:                                          // :133
                case PBWeather.HarshSun:
                    ret = "FIRE";                                            // :134
                    break;
                case PBWeather.Rain:                                         // :135
                case PBWeather.HeavyRain:
                    ret = "WATER";                                           // :136
                    break;
                case PBWeather.Sandstorm:                                    // :137
                    ret = "ROCK";                                            // :138
                    break;
                case PBWeather.Hail:                                         // :139
                case PBWeather.Snow:
                    ret = "ICE";                                             // :140
                    break;
                default:
                    break;
            }
            // 万能伞对常规天气的修正 (:143-146)
            if (user.hasUtilityUmbrella() && ("FIRE".equals(ret) || "WATER".equals(ret))) {
                ret = "NORMAL";                                              // :145
            }
            return ret;                                                      // :147
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} (:150-157). */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets,
                                    int hitNum, boolean showAnimation) {
            String t = pbBaseType(move, user);                               // :151
            if ("FIRE".equals(t)) {                                          // :152
                hitNum = 1;
            }
            if ("WATER".equals(t)) {                                         // :153
                hitNum = 2;
            }
            if ("ROCK".equals(t)) {                                          // :154
                hitNum = 3;
            }
            if ("ICE".equals(t)) {                                           // :155
                hitNum = 4;
            }
            // :156 super —— 动画播放器未建模，走基类（MoveEffectBase）已转译的播放分支
            super.pbShowAnimation(move, id, user, targets, hitNum, showAnimation);
        }
    }

    /** {@code class PokeBattle_Move_088 < PokeBattle_Move} (:166-176): Pursuit. */
    public static class PokeBattle_Move_088 extends MoveEffectBase {

        /** {@code pbAccuracyCheck(user,target)} (:167-170). */
        @Override
        public boolean pbAccuracyCheck(BattleMove move, Battler user, Battler target) {
            if (switching(battle(user))) {                                   // :168 @battle.switching
                return true;                                                 // :168
            }
            return super.pbAccuracyCheck(move, user, target);                // :169 super
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:172-175). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (switching(battle(user))) {                                   // :173
                baseDmg *= 2;
            }
            return baseDmg;                                                  // :174
        }
    }

    /** {@code class PokeBattle_Move_089 < PokeBattle_Move} (:183-187): Return. */
    public static class PokeBattle_Move_089 extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:184-186). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            return Math.max(user.pokemon.happiness * 2 / 5, 1);              // :185 [(happiness*2/5).floor,1].max
        }
    }

    /** {@code class PokeBattle_Move_08A < PokeBattle_Move} (:194-198): Frustration. */
    public static class PokeBattle_Move_08A extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:195-197). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            return Math.max((255 - user.pokemon.happiness) * 2 / 5, 1);      // :196
        }
    }

    /** {@code class PokeBattle_Move_08B < PokeBattle_Move} (:205-209): Eruption. */
    public static class PokeBattle_Move_08B extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:206-208). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            return Math.max(150 * user.hp / user.maxHp(), 1);                // :207
        }
    }

    /** {@code class PokeBattle_Move_08C < PokeBattle_Move} (:215-219): Crush Grip. */
    public static class PokeBattle_Move_08C extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:216-218). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            return Math.max(120 * target.hp / target.maxHp(), 1);            // :217
        }
    }

    /** {@code class PokeBattle_Move_08D < PokeBattle_Move} (:226-230): Gyro Ball. */
    public static class PokeBattle_Move_08D extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:227-229). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            return Math.max(Math.min(25 * target.speed() / user.speed(), 150), 1);   // :228
        }
    }

    /** {@code class PokeBattle_Move_08E < PokeBattle_Move} (:238-244): Stored Power. */
    public static class PokeBattle_Move_08E extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:239-243). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            int mult = 1;                                                    // :240
            for (int s : PBStats.EACH_BATTLE_STAT) {                         // :241 eachBattleStat
                int stage = user.stage(s);                                   // :241 user.stages[s]
                if (stage > 0) {
                    mult += stage;
                }
            }
            return 20 * mult;                                                // :242
        }
    }

    /** {@code class PokeBattle_Move_08F < PokeBattle_Move} (:252-258): Punishment. */
    public static class PokeBattle_Move_08F extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:253-257). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            int mult = 3;                                                    // :254
            for (int s : PBStats.EACH_BATTLE_STAT) {                         // :255
                int stage = target.stage(s);
                if (stage > 0) {
                    mult += stage;
                }
            }
            return Math.min(20 * mult, 200);                                 // :256 [20*mult,200].min
        }
    }

    /** {@code class PokeBattle_Move_090 < PokeBattle_Move} (:265-276): Hidden Power. */
    public static class PokeBattle_Move_090 extends MoveEffectBase {

        /** {@code pbBaseType(user)} (:266-269). */
        @Override
        public String pbBaseType(BattleMove move, Battler user) {
            HiddenPower hp = pbHiddenPower(user);                            // :267
            return hp.type;                                                  // :268
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:271-275). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (Battle.NEWEST_BATTLE_MECHANICS) {                            // :272 (Settings:160 = true)
                return super.pbBaseDamage(move, baseDmg, user, target);
            }
            HiddenPower hp = pbHiddenPower(user);                            // :273
            return hp.power;                                                 // :274
        }
    }

    // ==================================================================
    // Move_Effects_080-0FF.rb:318-710 - counters, Present, Magnitude,
    // Natural Gift, power scaling, Helping Hand, Mud Sport
    // ==================================================================

    /** {@code class PokeBattle_Move_091 < PokeBattle_Move} (:318-332): Fury Cutter. */
    public static class PokeBattle_Move_091 extends MoveEffectBase {

        /** {@code pbChangeUsageCounters(user,specialUsage)} (:319-327). */
        @Override
        public void pbChangeUsageCounters(BattleMove move, Battler user, boolean specialUsage) {
            int oldVal = user.effects.intVal(PBEffects.Battler.FuryCutter);   // :320
            super.pbChangeUsageCounters(move, user, specialUsage);            // :321
            int maxMult = 1;                                                 // :322
            while ((move.power() << (maxMult - 1)) < 160) {                  // :323 @baseDamage<<(maxMult-1)
                maxMult += 1;                                                // :324
            }
            user.effects.set(PBEffects.Battler.FuryCutter,                    // :326
                    oldVal >= maxMult ? maxMult : oldVal + 1);
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:329-331). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            return baseDmg << (user.effects.intVal(PBEffects.Battler.FuryCutter) - 1);   // :330
        }
    }

    /** {@code class PokeBattle_Move_092 < PokeBattle_Move} (:340-353): Echoed Voice. */
    public static class PokeBattle_Move_092 extends MoveEffectBase {

        /** {@code pbChangeUsageCounters(user,specialUsage)} (:341-348). */
        @Override
        public void pbChangeUsageCounters(BattleMove move, Battler user, boolean specialUsage) {
            int oldVal =
                    user.pbOwnSide().effects.intVal(PBEffects.Side.EchoedVoiceCounter);   // :342
            super.pbChangeUsageCounters(move, user, specialUsage);            // :343
            if (!user.pbOwnSide().effects.truthy(PBEffects.Side.EchoedVoiceUsed)) {   // :344
                user.pbOwnSide().effects.set(PBEffects.Side.EchoedVoiceCounter,   // :345
                        oldVal >= 5 ? 5 : oldVal + 1);
            }
            user.pbOwnSide().effects.set(PBEffects.Side.EchoedVoiceUsed, true);   // :347
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:350-352). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            return baseDmg * user.pbOwnSide().effects.intVal(PBEffects.Side.EchoedVoiceCounter);   // :351
        }
    }

    /** {@code class PokeBattle_Move_093 < PokeBattle_Move} (:362-366): Rage. */
    public static class PokeBattle_Move_093 extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:363-365). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.Rage, true);                   // :364
        }
    }

    /**
     * {@code class PokeBattle_Move_094 < PokeBattle_Move} (:375-413): Present.
     *
     * <p>{@code @presentDmg} (:377) is per-use state; like the other transient
     * state in this package it is a field of the strategy (task-11 decision 4).</p>
     */
    public static class PokeBattle_Move_094 extends MoveEffectBase {

        /** {@code @presentDmg} - 0 = heal, &gt;0 = damage (:377). */
        private int presentDmg;

        /** {@code pbOnStartUse(user,targets)} (:376-383). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            presentDmg = 0;                                                  // :377
            int r = battle(user).pbRandom(100);                              // :378
            if (r < 40) {                                                    // :379
                presentDmg = 40;
            } else if (r < 70) {                                             // :380
                presentDmg = 80;
            } else if (r < 80) {                                             // :381
                presentDmg = 120;
            }
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:385-392). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (presentDmg > 0) {                                            // :386
                return false;
            }
            if (!target.canHeal()) {                                         // :387
                battle(user).display("但是失败了！");             // :388
                return true;                                                 // :389
            }
            return false;                                                    // :391
        }

        /** {@code pbDamagingMove?} (:394-397). */
        @Override
        public boolean pbDamagingMove(BattleMove move) {
            if (presentDmg == 0) {                                           // :395
                return false;
            }
            return super.pbDamagingMove(move);                               // :396
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:399-401). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            return presentDmg;                                               // :400
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:403-407). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (presentDmg > 0) {                                            // :404
                return;
            }
            target.pbRecoverHP(target.maxHp() / 4);                          // :405
            battle(user).display(target.pbThis() + "的HP回复了。");   // :406
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} (:409-412). */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets,
                                    int hitNum, boolean showAnimation) {
            if (presentDmg == 0) {                                           // :410
                hitNum = 1;                                                  // Healing anim
            }
            super.pbShowAnimation(move, id, user, targets, hitNum, showAnimation);   // :411
        }
    }

    /**
     * {@code class PokeBattle_Move_095 < PokeBattle_Move} (:421-449): Magnitude.
     *
     * <p>{@code @magnitudeDmg} (:436) is per-use state (see
     * {@link PokeBattle_Move_094}).</p>
     */
    public static class PokeBattle_Move_095 extends MoveEffectBase {

        /** {@code @magnitudeDmg} (:436). */
        private int magnitudeDmg;

        /** {@code hitsDiggingTargets?} (:422). */
        @Override
        public boolean hitsDiggingTargets(BattleMove move) {
            return true;                                                     // :422
        }

        /** {@code pbOnStartUse(user,targets)} (:424-438). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            int[] baseDmg = {10, 30, 50, 70, 90, 110, 150};                  // :425
            int[] magnitudes = {                                             // :426-434
                4,
                5, 5,
                6, 6, 6, 6,
                7, 7, 7, 7, 7, 7,
                8, 8, 8, 8,
                9, 9,
                10
            };
            int magni = magnitudes[battle(user).pbRandom(magnitudes.length)];   // :435
            magnitudeDmg = baseDmg[magni - 4];                               // :436
            battle(user).display("震级" + magni + "！");        // :437
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:440-442). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            return magnitudeDmg;                                             // :441
        }

        /** {@code pbModifyDamage(damageMult,user,target)} (:444-448). */
        @Override
        public float pbModifyDamage(BattleMove move, float damageMult, Battler user, Battler target) {
            if (inTwoTurnAttack(target, "0CA")) {                            // :445 (Dig)
                damageMult *= 2;
            }
            if (battle(user).field.terrain == PBBattleTerrains.Grassy) {     // :446
                damageMult /= 2;
            }
            return damageMult;                                               // :447
        }
    }

    /**
     * {@code class PokeBattle_Move_096 < PokeBattle_Move} (:457-556): Natural Gift.
     *
     * <p>The two Ruby hashes (:460-496) keep insertion order, so they are
     * {@code LinkedHashMap}s here; their keys are the plugin's type names and the
     * item arrays are internal names, which is already this runtime's identity.
     * {@code @berry} (:497) is per-use state.</p>
     */
    public static class PokeBattle_Move_096 extends MoveEffectBase {

        /** {@code @typeArray} (:460-479), insertion-ordered like the Ruby hash. */
        private final java.util.LinkedHashMap<String, String[]> typeArray = new java.util.LinkedHashMap<>();

        /** {@code @damageArray} (:480-496), insertion-ordered like the Ruby hash. */
        private final java.util.LinkedHashMap<Integer, String[]> damageArray = new java.util.LinkedHashMap<>();

        /** {@code @berry} - the held item's internal name (Ruby id), {@code ""} for none (:497). */
        private String berry = "";

        public PokeBattle_Move_096() {
            typeArray.put("NORMAL", new String[] {"CHILANBERRY"});           // :461
            typeArray.put("FIRE", new String[] {                             // :462
                "CHERIBERRY", "BLUKBERRY", "WATMELBERRY", "OCCABERRY"});
            typeArray.put("WATER", new String[] {                            // :463
                "CHESTOBERRY", "NANABBERRY", "DURINBERRY", "PASSHOBERRY"});
            typeArray.put("ELECTRIC", new String[] {                         // :464
                "PECHABERRY", "WEPEARBERRY", "BELUEBERRY", "WACANBERRY"});
            typeArray.put("GRASS", new String[] {                            // :465
                "RAWSTBERRY", "PINAPBERRY", "RINDOBERRY", "LIECHIBERRY"});
            typeArray.put("ICE", new String[] {                              // :466
                "ASPEARBERRY", "POMEGBERRY", "YACHEBERRY", "GANLONBERRY"});
            typeArray.put("FIGHTING", new String[] {                         // :467
                "LEPPABERRY", "KELPSYBERRY", "CHOPLEBERRY", "SALACBERRY"});
            typeArray.put("POISON", new String[] {                           // :468
                "ORANBERRY", "QUALOTBERRY", "KEBIABERRY", "PETAYABERRY"});
            typeArray.put("GROUND", new String[] {                           // :469
                "PERSIMBERRY", "HONDEWBERRY", "SHUCABERRY", "APICOTBERRY"});
            typeArray.put("FLYING", new String[] {                           // :470
                "LUMBERRY", "GREPABERRY", "COBABERRY", "LANSATBERRY"});
            typeArray.put("PSYCHIC", new String[] {                          // :471
                "SITRUSBERRY", "TAMATOBERRY", "PAYAPABERRY", "STARFBERRY"});
            typeArray.put("BUG", new String[] {                              // :472
                "FIGYBERRY", "CORNNBERRY", "TANGABERRY", "ENIGMABERRY"});
            typeArray.put("ROCK", new String[] {                             // :473
                "WIKIBERRY", "MAGOSTBERRY", "CHARTIBERRY", "MICLEBERRY"});
            typeArray.put("GHOST", new String[] {                            // :474
                "MAGOBERRY", "RABUTABERRY", "KASIBBERRY", "CUSTAPBERRY"});
            typeArray.put("DRAGON", new String[] {                           // :475
                "AGUAVBERRY", "NOMELBERRY", "HABANBERRY", "JABOCABERRY"});
            typeArray.put("DARK", new String[] {                             // :476
                "IAPAPABERRY", "SPELONBERRY", "COLBURBERRY", "ROWAPBERRY", "MARANGABERRY"});
            typeArray.put("STEEL", new String[] {                            // :477
                "RAZZBERRY", "PAMTREBERRY", "BABIRIBERRY"});
            typeArray.put("FAIRY", new String[] {                            // :478
                "ROSELIBERRY", "KEEBERRY"});

            damageArray.put(60, new String[] {                               // :481-487
                "CHERIBERRY", "CHESTOBERRY", "PECHABERRY", "RAWSTBERRY", "ASPEARBERRY",
                "LEPPABERRY", "ORANBERRY", "PERSIMBERRY", "LUMBERRY", "SITRUSBERRY",
                "FIGYBERRY", "WIKIBERRY", "MAGOBERRY", "AGUAVBERRY", "IAPAPABERRY",
                "RAZZBERRY", "OCCABERRY", "PASSHOBERRY", "WACANBERRY", "RINDOBERRY",
                "YACHEBERRY", "CHOPLEBERRY", "KEBIABERRY", "SHUCABERRY", "COBABERRY",
                "PAYAPABERRY", "TANGABERRY", "CHARTIBERRY", "KASIBBERRY", "HABANBERRY",
                "COLBURBERRY", "BABIRIBERRY", "CHILANBERRY", "ROSELIBERRY"});
            damageArray.put(70, new String[] {                               // :488-491
                "BLUKBERRY", "NANABBERRY", "WEPEARBERRY", "PINAPBERRY", "POMEGBERRY",
                "KELPSYBERRY", "QUALOTBERRY", "HONDEWBERRY", "GREPABERRY", "TAMATOBERRY",
                "CORNNBERRY", "MAGOSTBERRY", "RABUTABERRY", "NOMELBERRY", "SPELONBERRY",
                "PAMTREBERRY"});
            damageArray.put(80, new String[] {                               // :492-495
                "WATMELBERRY", "DURINBERRY", "BELUEBERRY", "LIECHIBERRY", "GANLONBERRY",
                "SALACBERRY", "PETAYABERRY", "APICOTBERRY", "LANSATBERRY", "STARFBERRY",
                "ENIGMABERRY", "MICLEBERRY", "CUSTAPBERRY", "JABOCABERRY", "ROWAPBERRY",
                "KEEBERRY", "MARANGABERRY"});
        }

        /** {@code pbMoveFailed?(user,targets)} (:500-508). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            // NOTE: Unnerve does not stop a Pokémon using this move. (:501)
            berry = user.item;                                               // :502
            if (!isBerry(battle(user), user.item) || !user.itemActive()) {   // :503 pbIsBerry?(@berry) || !itemActive?
                battle(user).display("但是失败了！");             // :504
                return true;                                                 // :505
            }
            return false;                                                    // :507
        }

        /** {@code pbBaseType(user)} (:514-526). */
        @Override
        public String pbBaseType(BattleMove move, Battler user) {
            String ret = "NORMAL";                                           // :515 getID(PBTypes,:NORMAL)
            boolean found = false;                                           // :516
            for (java.util.Map.Entry<String, String[]> e : typeArray.entrySet()) {   // :517
                for (String item : e.getValue()) {                           // :518
                    if (!item.equals(berry)) {                               // :519 isConst?(@berry,PBItems,i)
                        continue;
                    }
                    ret = e.getKey();                                        // :520 getConst(PBTypes,type)
                    found = true;
                    break;                                                   // :521
                }
                if (found) {                                                 // :523
                    break;
                }
            }
            return ret;                                                      // :525
        }

        /** {@code pbNaturalGiftBaseDamage(heldItem)} (:529-542). */
        @Override
        public int pbNaturalGiftBaseDamage(BattleMove move, String heldItem) {
            int ret = 1;                                                     // :530
            boolean found = false;                                           // :531
            for (java.util.Map.Entry<Integer, String[]> e : damageArray.entrySet()) {   // :532
                for (String item : e.getValue()) {                           // :533
                    if (!item.equals(heldItem)) {                            // :534 isConst?(heldItem,PBItems,i)
                        continue;
                    }
                    ret = e.getKey();                                        // :535
                    if (Battle.NEWEST_BATTLE_MECHANICS) {                    // :536
                        ret += 20;
                    }
                    found = true;
                    break;                                                   // :537
                }
                if (found) {                                                 // :539
                    break;
                }
            }
            return ret;                                                      // :541
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:544-546). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            return pbNaturalGiftBaseDamage(move, berry);                     // :545
        }

        /** {@code pbEndOfMoveUsageEffect(user,targets,numHits,switchedBattlers)} (:548-555). */
        @Override
        public void pbEndOfMoveUsageEffect(BattleMove move, Battler user, Array<Battler> targets,
                                           int numHits, com.badlogic.gdx.utils.IntArray switchedBattlers) {
            if (!user.item.isEmpty()) {                                      // :553 user.item>0
                user.pbConsumeItem(true, true, false);                       // :553
            }
            berry = "";                                                      // :554 @berry = 0
        }
    }

    /** {@code class PokeBattle_Move_097 < PokeBattle_Move} (:563-569): Trump Card. */
    public static class PokeBattle_Move_097 extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:564-568). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            int[] dmgs = {200, 80, 60, 50, 40};                              // :565
            int ppLeft = Math.min(movePp(move), dmgs.length - 1);            // :566 [@pp,dmgs.length-1].min
            return dmgs[ppLeft];                                             // :567
        }
    }

    /** {@code class PokeBattle_Move_098 < PokeBattle_Move} (:576-588): Flail, Reversal. */
    public static class PokeBattle_Move_098 extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:577-587). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            int ret = 20;                                                    // :578
            int n = 48 * user.hp / user.maxHp();                             // :579
            if (n < 2) {                                                     // :580
                ret = 200;
            } else if (n < 5) {                                              // :581
                ret = 150;
            } else if (n < 10) {                                             // :582
                ret = 100;
            } else if (n < 17) {                                             // :583
                ret = 80;
            } else if (n < 33) {                                             // :584
                ret = 40;
            }
            return ret;                                                      // :586
        }
    }

    /** {@code class PokeBattle_Move_099 < PokeBattle_Move} (:595-606): Electro Ball. */
    public static class PokeBattle_Move_099 extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:596-605). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            int ret = 40;                                                    // :597
            int n = user.speed() / target.speed();                           // :598
            if (n >= 4) {                                                    // :599
                ret = 150;
            } else if (n >= 3) {                                             // :600
                ret = 120;
            } else if (n >= 2) {                                             // :601
                ret = 80;
            } else if (n >= 1) {                                             // :602
                ret = 60;
            }
            return ret;                                                      // :604
        }
    }

    /** {@code class PokeBattle_Move_09A < PokeBattle_Move} (:613-625): Grass Knot, Low Kick. */
    public static class PokeBattle_Move_09A extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:614-624). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            int ret = 20;                                                    // :615
            int weight = pbWeight(target);                                   // :616 target.pbWeight
            if (weight >= 2000) {                                            // :617
                ret = 120;
            } else if (weight >= 1000) {                                     // :618
                ret = 100;
            } else if (weight >= 500) {                                      // :619
                ret = 80;
            } else if (weight >= 250) {                                      // :620
                ret = 60;
            } else if (weight >= 100) {                                      // :621
                ret = 40;
            }
            return ret;                                                      // :623
        }
    }

    /** {@code class PokeBattle_Move_09B < PokeBattle_Move} (:633-649): Heat Crash, Heavy Slam. */
    public static class PokeBattle_Move_09B extends MoveEffectBase {

        /** {@code tramplesMinimize?(param=1)} (:634-637). */
        @Override
        public boolean tramplesMinimize(BattleMove move, int param) {
            if (Battle.NEWEST_BATTLE_MECHANICS) {                            // :635
                return true;                                                 // Perfect accuracy and double damage
            }
            return super.tramplesMinimize(move, param);                      // :636
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:639-648). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            int ret = 40;                                                    // :640
            int n = pbWeight(user) / pbWeight(target);                       // :641 (user.pbWeight/target.pbWeight).floor
            if (n >= 5) {                                                    // :642
                ret = 120;
            } else if (n >= 4) {                                             // :643
                ret = 100;
            } else if (n >= 3) {                                             // :644
                ret = 80;
            } else if (n >= 2) {                                             // :645
                ret = 60;
            }
            return ret;                                                      // :647
        }
    }

    /** {@code class PokeBattle_Move_09C < PokeBattle_Move} (:656-672): Helping Hand. */
    public static class PokeBattle_Move_09C extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:657). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :657
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:659-666). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.fainted() || target.effects.truthy(PBEffects.Battler.HelpingHand)) {   // :660
                battle(user).display("但是失败了！");             // :661
                return true;                                                 // :662
            }
            if (pbMoveFailedTargetAlreadyMoved(move, target)) {              // :664
                return true;
            }
            return false;                                                    // :665
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:668-671). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.HelpingHand, true);          // :669
            battle(user).display(user.pbThis() + "准备帮助"
                    + target.pbThis(true) + "！");                           // :670
        }
    }

    /** {@code class PokeBattle_Move_09D < PokeBattle_Move} (:679-710): Mud Sport. */
    public static class PokeBattle_Move_09D extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:680-694). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (Battle.NEWEST_BATTLE_MECHANICS) {                            // :681
                if (battle(user).field.effects
                        .intVal(PBEffects.Field.MudSportField) > 0) {         // :682
                    battle(user).display("但是失败了！");         // :683
                    return true;                                             // :684
                }
            } else {
                for (Battler b : battle(user).eachBattler()) {   // :687
                    if (!b.effects.truthy(PBEffects.Battler.MudSport)) {      // :688
                        continue;
                    }
                    battle(user).display("但是失败了！");         // :689
                    return true;                                             // :690
                }
            }
            return false;                                                    // :693
        }

        /** {@code pbEffectGeneral(user)} (:696-...). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            if (Battle.NEWEST_BATTLE_MECHANICS) {                            // :697
                battle(user).field.effects
                        .set(PBEffects.Field.MudSportField, 5);               // :698
            } else {
                user.effects.set(PBEffects.Battler.MudSport, true);           // :700
            }
        }
    }

    // ==================================================================
    // Move_Effects_080-0FF.rb:711-1073 - Water Sport, Judgment/plate types,
    // crit override, screens, Secret Power
    // ==================================================================

    /** {@code class PokeBattle_Move_09E < PokeBattle_Move} (:711-736): Water Sport. */
    public static class PokeBattle_Move_09E extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:712-726). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (Battle.NEWEST_BATTLE_MECHANICS) {                            // :713
                if (battle(user).field.effects
                        .intVal(PBEffects.Field.WaterSportField) > 0) {       // :714
                    battle(user).display("但是失败了！");         // :715
                    return true;                                             // :716
                }
            } else {
                for (Battler b : battle(user).eachBattler()) {   // :719
                    if (!b.effects.truthy(PBEffects.Battler.WaterSport)) {    // :720
                        continue;
                    }
                    battle(user).display("但是失败了！");         // :721
                    return true;                                             // :722
                }
            }
            return false;                                                    // :725
        }

        /** {@code pbEffectGeneral(user)} (:728-735). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            if (Battle.NEWEST_BATTLE_MECHANICS) {                            // :729
                battle(user).field.effects
                        .set(PBEffects.Field.WaterSportField, 5);             // :730
            } else {
                user.effects.set(PBEffects.Battler.WaterSport, true);         // :732
            }
            battle(user).display("火属性招式的威力被减弱了！");   // :734
        }
    }

    /**
     * {@code class PokeBattle_Move_09F < PokeBattle_Move} (:743-841): Judgment,
     * Multi-Attack, Techno Blast.
     *
     * <p>{@code @itemTypes} (:747-796) is a move-id-dependent table, so it is
     * built in the constructor from the move's internal name (this runtime's move
     * identity); the maps keep the Ruby hash's insertion order.</p>
     */
    public static class PokeBattle_Move_09F extends MoveEffectBase {

        /** {@code @itemTypes} - held item internal name -> type internal name (:747-796). */
        private java.util.LinkedHashMap<String, String> itemTypes;

        /** {@code initialize(battle,move)} (:744-797). */
        public PokeBattle_Move_09F() {
            // 注册表按 function code 建单例；Ruby 在这里按 @id（招式身份）选表，
            // 本运行时的招式身份是内部名，故 4 个分支的表都在构造器里建好、
            // 由 pbBaseType/pbShowAnimation 按 move.internalName() 选用。
            itemTypes = new java.util.LinkedHashMap<>();
            itemTypes.put("FISTPLATE", "FIGHTING");                          // :748
            itemTypes.put("SKYPLATE", "FLYING");                             // :749
            itemTypes.put("TOXICPLATE", "POISON");                           // :750
            itemTypes.put("EARTHPLATE", "GROUND");                           // :751
            itemTypes.put("STONEPLATE", "ROCK");                             // :752
            itemTypes.put("INSECTPLATE", "BUG");                             // :753
            itemTypes.put("SPOOKYPLATE", "GHOST");                           // :754
            itemTypes.put("IRONPLATE", "STEEL");                             // :755
            itemTypes.put("FLAMEPLATE", "FIRE");                             // :756
            itemTypes.put("SPLASHPLATE", "WATER");                           // :757
            itemTypes.put("MEADOWPLATE", "GRASS");                           // :758
            itemTypes.put("ZAPPLATE", "ELECTRIC");                           // :759
            itemTypes.put("MINDPLATE", "PSYCHIC");                           // :760
            itemTypes.put("ICICLEPLATE", "ICE");                             // :761
            itemTypes.put("DRACOPLATE", "DRAGON");                           // :762
            itemTypes.put("DREADPLATE", "DARK");                             // :763
            itemTypes.put("PIXIEPLATE", "FAIRY");                            // :764
            itemTypes.put("LIGHTPLATE", "LIGHT");                            // :765
            itemTypes.put("SHADOWPLATE", "DIM");                             // :766
        }

        /** {@code pbOnStartUse(user,targets)} (:799-814). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.isSpecies("ARCEUS")) {                                 // :800
                return;
            }
            if (user.hasActiveItem("LEGENDPLATE")) {                         // :801
                if (targets.size == 0) {                                     // :802
                    return;
                }
                Battler b = targets.get(0);                                  // :803
                if (b == null || b.fainted() || isCommander(b)) {             // :804
                    return;
                }
                String currentType = chooseBestType(b, 0);                   // :805 choose_best_type(b,0)
                move.setCalcType(currentType);                               // :807 @calcType = current_form
                int currentForm = typeIdById(battle(user).pbs(), currentType);
                if (currentForm == 9 || currentForm == 19) {                 // :808
                    currentForm = 0;
                }
                if (currentForm > 19) {                                      // :809
                    currentForm -= 1;
                }
                if (user.form() != currentForm) {                            // :810
                    user.pbChangeFormTransform(currentForm, user.pbThis() + "变成了"
                            + typeDisplayName(battle(user), currentType) + "属性！");   // :811
                }
            }
        }

        /** {@code pbBaseType(user)} (:816-828). */
        @Override
        public String pbBaseType(BattleMove move, Battler user) {
            if ("JUDGMENT".equals(move.internalName())) {                    // :817 isConst?(@id,PBMoves,:JUDGMENT)
                Array<String> types = user.types();
                return types.size == 0 ? null : types.get(0);                // :817 user.type1
            }
            String ret = "NORMAL";                                           // :818 getID(PBTypes,:NORMAL)
            if (user.itemActive()) {                                         // :819
                for (java.util.Map.Entry<String, String> e : itemTypes.entrySet()) {   // :820
                    if (!e.getKey().equals(user.item)) {                     // :821 isConst?(user.item,PBItems,item)
                        continue;
                    }
                    ret = e.getValue();                                      // :822 getConst(PBTypes,itemType)
                    break;                                                   // :824
                }
            }
            return ret;                                                      // :827
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} (:830-840). */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets,
                                    int hitNum, boolean showAnimation) {
            if ("TECHNOBLAST".equals(move.internalName())) {                 // :831 isConst?(@id,PBMoves,:TECHNOBLAST)
                String t = pbBaseType(move, user);                           // :832 Type-specific anim
                hitNum = 0;                                                  // :833
                if ("ELECTRIC".equals(t)) {                                  // :834
                    hitNum = 1;
                }
                if ("FIRE".equals(t)) {                                      // :835
                    hitNum = 2;
                }
                if ("ICE".equals(t)) {                                       // :836
                    hitNum = 3;
                }
                if ("WATER".equals(t)) {                                     // :837
                    hitNum = 4;
                }
            }
            super.pbShowAnimation(move, id, user, targets, hitNum, showAnimation);   // :839
        }
    }

    /** {@code class PokeBattle_Move_0A0 < PokeBattle_Move} (:893-895): Frost Breath. */
    public static class PokeBattle_Move_0A0 extends MoveEffectBase {

        /** {@code pbCritialOverride(user,target)} (:894). */
        @Override
        public int pbCritialOverride(BattleMove move, Battler user, Battler target) {
            return 1;                                                        // :894
        }
    }

    /** {@code class PokeBattle_Move_0A1 < PokeBattle_Move} (:902-915): Lucky Chant. */
    public static class PokeBattle_Move_0A1 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:903-909). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.pbOwnSide().effects.intVal(PBEffects.Side.LuckyChant) > 0) {   // :904
                battle(user).display("但是失败了！");             // :905
                return true;                                                 // :906
            }
            return false;                                                    // :908
        }

        /** {@code pbEffectGeneral(user)} (:911-914). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.pbOwnSide().effects.set(PBEffects.Side.LuckyChant, 5);       // :912
            battle(user).display("幸运咒语使" + user.pbTeam(true)
                    + "不会被击中要害！");                                    // :913
        }
    }

    /** {@code class PokeBattle_Move_0A2 < PokeBattle_Move} (:923-937): Reflect. */
    public static class PokeBattle_Move_0A2 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:924-930). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.pbOwnSide().effects.intVal(PBEffects.Side.Reflect) > 0) {   // :925
                battle(user).display("但是失败了！");             // :926
                return true;                                                 // :927
            }
            return false;                                                    // :929
        }

        /** {@code pbEffectGeneral(user)} (:932-936). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.pbOwnSide().effects.set(PBEffects.Side.Reflect, 5);          // :933
            if (user.hasActiveItem("LIGHTCLAY")) {                           // :934
                user.pbOwnSide().effects.set(PBEffects.Side.Reflect, 8);
            }
            battle(user).display(move.name() + "提升了"
                    + user.pbTeam(true) + "的防御！");                        // :935
        }
    }

    /** {@code class PokeBattle_Move_0A3 < PokeBattle_Move} (:944-958): Light Screen. */
    public static class PokeBattle_Move_0A3 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:945-951). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.pbOwnSide().effects.intVal(PBEffects.Side.LightScreen) > 0) {   // :946
                battle(user).display("但是失败了！");             // :947
                return true;                                                 // :948
            }
            return false;                                                    // :950
        }

        /** {@code pbEffectGeneral(user)} (:953-957). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.pbOwnSide().effects.set(PBEffects.Side.LightScreen, 5);      // :954
            if (user.hasActiveItem("LIGHTCLAY")) {                           // :955
                user.pbOwnSide().effects.set(PBEffects.Side.LightScreen, 8);
            }
            battle(user).display(move.name() + "提升了"
                    + user.pbTeam(true) + "的特防！");                        // :956
        }
    }

    /**
     * {@code class PokeBattle_Move_0A4 < PokeBattle_Move} (:965-1073): Secret Power.
     *
     * <p>{@code @secretPower} (:970) is per-use state (see
     * {@link PokeBattle_Move_094}).</p>
     */
    public static class PokeBattle_Move_0A4 extends MoveEffectBase {

        /** {@code @secretPower} (:970-1007). */
        private int secretPower;

        /** {@code flinchingMove?} (:966). */
        @Override
        public boolean flinchingMove(BattleMove move) {
            return secretPower == 6 || secretPower == 10 || secretPower == 12;   // :966 [6,10,12].include?(@secretPower)
        }

        /** {@code @bugLureSecretPower} (场地:158/:163). */
        private boolean bugLureSecretPower;
        /** {@code @coldTerrainSecretPower} (场地:377/:382). */
        private boolean coldTerrainSecretPower;

        /** The original {@code pbOnStartUse(user,targets)} (:968-1008), aliased at 场地:155. */
        private void secretPowerOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            // NOTE: This is Gen 7's list plus some of Gen 6 plus a bit of my own. (:969)
            secretPower = 0;                                                 // :970 Body Slam, paralysis
            Battle battle = battle(user);
            switch (battle.field.terrain) {                                  // :971
                case PBBattleTerrains.Electric:                              // :972
                    secretPower = 1;                                         // Thunder Shock, paralysis
                    break;
                case PBBattleTerrains.Grassy:                                // :974
                    secretPower = 2;                                         // Vine Whip, sleep
                    break;
                case PBBattleTerrains.Misty:                                 // :976
                    secretPower = 3;                                         // Fairy Wind, lower Sp. Atk by 1
                    break;
                case PBBattleTerrains.Psychic:                               // :978
                    secretPower = 4;                                         // Confusion, lower Speed by 1
                    break;
                default:                                                     // :980
                    switch (battle.environment) {                            // :981
                        case PBEnvironment.Grass:                            // :982
                        case PBEnvironment.TallGrass:
                        case PBEnvironment.Forest:
                        case PBEnvironment.ForestGrass:
                            secretPower = 2;                                 // :984 (Same as Grassy Terrain)
                            break;
                        case PBEnvironment.MovingWater:                      // :985
                        case PBEnvironment.StillWater:
                        case PBEnvironment.Underwater:
                            secretPower = 5;                                 // :987 Water Pulse, lower Attack by 1
                            break;
                        case PBEnvironment.Puddle:                           // :988
                            secretPower = 6;                                 // Mud Shot, lower Speed by 1
                            break;
                        case PBEnvironment.Cave:                             // :990
                            secretPower = 7;                                 // Rock Throw, flinch
                            break;
                        case PBEnvironment.Rock:                             // :992
                        case PBEnvironment.Sand:
                            secretPower = 8;                                 // Mud-Slap, lower Acc by 1
                            break;
                        case PBEnvironment.Snow:                             // :994
                        case PBEnvironment.Ice:
                            secretPower = 9;                                 // Ice Shard, freeze
                            break;
                        case PBEnvironment.Volcano:                          // :996
                            secretPower = 10;                                // Incinerate, burn
                            break;
                        case PBEnvironment.Graveyard:                        // :998
                            secretPower = 11;                                // Shadow Sneak, flinch
                            break;
                        case PBEnvironment.Sky:                              // :1000
                            secretPower = 12;                                // Gust, lower Speed by 1
                            break;
                        case PBEnvironment.Space:                            // :1002
                            secretPower = 13;                                // Swift, flinch
                            break;
                        case PBEnvironment.UltraSpace:                       // :1004
                            secretPower = 14;                                // Psywave, lower Defense by 1
                            break;
                        default:
                            break;
                    }
                    break;
            }
        }

        /**
         * {@code bug_lure_secret_pbOnStartUse} plus the redefinition that captures
         * it (场地:155-165): the BugLure layer of the {@code alias} chain.
         */
        private void bugLureSecretPbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            secretPowerOnStartUse(move, user, targets);                      // :157
            bugLureSecretPower = false;                                      // :158
            if (battle(user).field.terrain == PBBattleTerrains.BugLure) {     // :160
                secretPower = 5;                                             // :162 使用原版“降低攻击”效果编号
                bugLureSecretPower = true;                                   // :163
            }
        }

        /**
         * {@code pbOnStartUse(user,targets)} - the final layer
         * (场地:374-384) on top of :155-165. Ruby aliases the previous definition
         * twice (场地:155, 场地:374), so the chain is 主 → BugLure → Cold.
         */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            bugLureSecretPbOnStartUse(move, user, targets);                  // :376
            coldTerrainSecretPower = false;                                  // :377
            if (battle(user).field.terrain == PBBattleTerrains.Cold) {        // :379
                secretPower = 9;                                             // :381 原版秘密之力中的冰冻效果编号
                coldTerrainSecretPower = true;                               // :382
            }
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:1014-1051). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (target.fainted()) {                                          // :1015
                return;
            }
            if (target.damageState.unaffected || target.damageState.substitute) {   // :1016
                return;
            }
            int chance = pbAdditionalEffectChance(move, user, target, 0);    // :1017 pbAdditionalEffectChance(user,target)
            if (battle(user).pbRandom(100) >= chance) {                      // :1018
                return;
            }
            switch (secretPower) {                                           // :1019
                case 2:                                                      // :1020
                    if (target.pbCanSleep(user, false, move, false)) {   // :1021 pbCanSleep?(user,false,self)
                        target.pbSleep();
                    }
                    break;
                case 10:                                                     // :1022
                    if (target.pbCanBurn(user, false, move)) {    // :1023
                        target.pbBurn(user, null);
                    }
                    break;
                case 0:                                                      // :1024
                case 1:
                    if (target.pbCanParalyze(user, false, move)) {   // :1025
                        target.pbParalyze(user, null);
                    }
                    break;
                case 9:                                                      // :1026
                    if (target.pbCanFreeze(user, false, move)) {   // :1027
                        target.pbFreeze();
                    }
                    break;
                case 5:                                                      // :1028
                    if (target.pbCanLowerStatStage(PBStats.ATTACK, user, move)) {   // :1029
                        target.pbLowerStatStage(PBStats.ATTACK, 1, user, true);     // :1030
                    }
                    break;
                case 14:                                                     // :1032
                    if (target.pbCanLowerStatStage(PBStats.DEFENSE, user, move)) {   // :1033
                        target.pbLowerStatStage(PBStats.DEFENSE, 1, user, true);    // :1034
                    }
                    break;
                case 3:                                                      // :1036
                    if (target.pbCanLowerStatStage(PBStats.SPATK, user, move)) {   // :1037
                        target.pbLowerStatStage(PBStats.SPATK, 1, user, true);     // :1038
                    }
                    break;
                case 4:                                                      // :1040
                case 6:
                case 12:
                    if (target.pbCanLowerStatStage(PBStats.SPEED, user, move)) {   // :1041
                        target.pbLowerStatStage(PBStats.SPEED, 1, user, true);     // :1042
                    }
                    break;
                case 8:                                                      // :1044
                    if (target.pbCanLowerStatStage(PBStats.ACCURACY, user, move)) {   // :1045
                        target.pbLowerStatStage(PBStats.ACCURACY, 1, user, true);    // :1046
                    }
                    break;
                case 7:                                                      // :1048
                case 11:
                case 13:
                    target.pbFlinch(user);                 // :1049
                    break;
                default:
                    break;
            }
        }

        /** The original {@code pbShowAnimation(...)} (:1053-1072), aliased at 场地:173. */
        private void secretPowerShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets,
                                             int hitNum, boolean showAnimation) {
            id = moveId(battle(user), "BODYSLAM", id);                       // :1054 Environment-specific anim
            switch (secretPower) {                                           // :1055
                case 1:                                                      // :1056
                    id = moveId(battle(user), "THUNDERSHOCK", id);
                    break;
                case 2:                                                      // :1057
                    id = moveId(battle(user), "VINEWHIP", id);
                    break;
                case 3:                                                      // :1058
                    id = moveId(battle(user), "FAIRYWIND", id);
                    break;
                case 4:                                                      // :1059
                    id = moveId(battle(user), "CONFUSION", id);
                    break;
                case 5:                                                      // :1060
                    id = moveId(battle(user), "WATERPULSE", id);
                    break;
                case 6:                                                      // :1061
                    id = moveId(battle(user), "MUDSHOT", id);
                    break;
                case 7:                                                      // :1062
                    id = moveId(battle(user), "ROCKTHROW", id);
                    break;
                case 8:                                                      // :1063
                    id = moveId(battle(user), "MUDSLAP", id);
                    break;
                case 9:                                                      // :1064
                    id = moveId(battle(user), "ICESHARD", id);
                    break;
                case 10:                                                     // :1065
                    id = moveId(battle(user), "INCINERATE", id);
                    break;
                case 11:                                                     // :1066
                    id = moveId(battle(user), "SHADOWSNEAK", id);
                    break;
                case 12:                                                     // :1067
                    id = moveId(battle(user), "GUST", id);
                    break;
                case 13:                                                     // :1068
                    id = moveId(battle(user), "SWIFT", id);
                    break;
                case 14:                                                     // :1069
                    id = moveId(battle(user), "PSYWAVE", id);
                    break;
                default:
                    break;
            }
            super.pbShowAnimation(move, id, user, targets, hitNum, showAnimation);   // :1071
        }

        /**
         * {@code bug_lure_secret_pbShowAnimation} plus the redefinition that
         * captures it (场地:173-183): the BugLure layer.
         */
        private void bugLureSecretPbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets,
                                                  int hitNum, boolean showAnimation) {
            if (bugLureSecretPower) {                                        // :175
                int anim = moveId(battle(user), "BUGBUZZ", id);              // :176 anim = getConst(PBMoves,:BUGBUZZ) || id
                if (showAnimation) {                                         // :177
                    battle(user).animation(anim, user, targets, hitNum);
                }
                return;                                                      // :178
            }
            secretPowerShowAnimation(move, id, user, targets, hitNum, showAnimation);   // :180-182
        }

        /**
         * {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} - the
         * final layer (场地:392-403) on top of :173-183.
         */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets,
                                    int hitNum, boolean showAnimation) {
            if (coldTerrainSecretPower) {                                    // :394
                int anim = moveId(battle(user), "ICEBEAM", id);              // :395 anim = getConst(PBMoves,:ICEBEAM) || id
                if (showAnimation) {                                         // :396
                    battle(user).animation(anim, user, targets, hitNum);
                }
                return;                                                      // :397
            }
            bugLureSecretPbShowAnimation(move, id, user, targets, hitNum, showAnimation);   // :400-402
        }

        /**
         * The base {@code pbAdditionalEffectChance} (Move_Usage_Calculations:547),
         * aliased at 场地:167 (0A4 itself does not define it, so the alias captures
         * the inherited body).
         */
        private int inheritedAdditionalEffectChance(BattleMove move, Battler user, Battler target, int effectChance) {
            return super.pbAdditionalEffectChance(move, user, target, effectChance);
        }

        /**
         * {@code bug_lure_pbAdditionalEffectChance} plus the redefinition that
         * captures it (场地:167-171): the BugLure layer.
         *
         * <p>登记: the plugin's redefinition takes {@code (user,target)} while the
         * inherited body is {@code (user,target,effectChance=0)}
         * (Move_Usage_Calculations:547), so Ruby would raise {@code ArgumentError}
         * when {@code pbEffectAfterAllHits} calls it with three arguments
         * (:1017). Java cannot express two arities for one method, so this
         * transcription keeps the base arity and passes {@code effectChance}
         * through - the same treatment as {@code pbFailsAgainstTarget?}
         * (task-11 decision 6).</p>
         */
        private int bugLureAdditionalEffectChance(BattleMove move, Battler user, Battler target, int effectChance) {
            if (bugLureSecretPower) {                                        // :169
                return 50;
            }
            return inheritedAdditionalEffectChance(move, user, target, effectChance);   // :170
        }

        /**
         * {@code pbAdditionalEffectChance(user,target)} - the final layer
         * (场地:386-390) on top of :167-171. Same arity note as above.
         */
        @Override
        public int pbAdditionalEffectChance(BattleMove move, Battler user, Battler target, int effectChance) {
            if (coldTerrainSecretPower) {                                    // :388
                return 15;
            }
            return bugLureAdditionalEffectChance(move, user, target, effectChance);   // :389
        }
    }

    // ==================================================================
    // Move_Effects_080-0FF.rb:1080-1319 - always hit, Lock-On, Foresight,
    // stat-ignoring, Protect family, Feint, Mirror Move, Copycat
    // ==================================================================

    /** {@code class PokeBattle_Move_0A5 < PokeBattle_Move} (:1080-1082): always hits. */
    public static class PokeBattle_Move_0A5 extends MoveEffectBase {

        /** {@code pbAccuracyCheck(user,target)} (:1081). */
        @Override
        public boolean pbAccuracyCheck(BattleMove move, Battler user, Battler target) {
            return true;                                                     // :1081
        }
    }

    /** {@code class PokeBattle_Move_0A6 < PokeBattle_Move} (:1090-1096): Lock-On, Mind Reader. */
    public static class PokeBattle_Move_0A6 extends MoveEffectBase {

        /** {@code pbEffectAgainstTarget(user,target)} (:1091-1095). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            user.effects.set(PBEffects.Battler.LockOn, 2);                    // :1092
            user.effects.set(PBEffects.Battler.LockOnPos, target.index);      // :1093
            battle(user).display(user.pbThis() + "瞄准了"
                    + target.pbThis(true) + "！");                           // :1094
        }
    }

    /** {@code class PokeBattle_Move_0A7 < PokeBattle_Move} (:1104-1111): Foresight. */
    public static class PokeBattle_Move_0A7 extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:1105). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :1105
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1107-1110). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.Foresight, true);            // :1108
            battle(user).display(target.pbThis() + "被识破了！");   // :1109
        }
    }

    /** {@code class PokeBattle_Move_0A8 < PokeBattle_Move} (:1119-1126): Miracle Eye. */
    public static class PokeBattle_Move_0A8 extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:1120). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :1120
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1122-1125). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.MiracleEye, true);           // :1123
            battle(user).display(target.pbThis() + "被识破了！");   // :1124
        }
    }

    /**
     * {@code class PokeBattle_Move_0A9 < PokeBattle_Move} (:1134-1144): Chip Away,
     * Darkest Lariat, Sacred Sword.
     */
    public static class PokeBattle_Move_0A9 extends MoveEffectBase {

        /**
         * {@code pbCalcAccuracyMultipliers(user,target,multipliers)} (:1135-1138).
         *
         * <p><b>登记: plugin defect.</b> The parameter is named {@code multipliers}
         * while the body reads {@code modifiers[EVA_STAGE]}, which is a
         * {@code NameError} in Ruby; and the method has no call site
         * ({@code pbAccuracyCheck} calls {@code pbCalcAccuracyModifiers}). Copied
         * verbatim - the reference is kept as a comment, not "fixed".</p>
         */
        @Override
        public void pbCalcAccuracyMultipliers(BattleMove move, Battler user, Battler target,
                                              float[] multipliers) {
            super.pbCalcAccuracyMultipliers(move, user, target, multipliers);   // :1136 super
            // :1137 照抄原文（未定义局部变量 modifiers；等价于 NameError，不修正）：
            // modifiers[EVA_STAGE] = 0   # Accuracy stat stage
        }

        /** {@code pbGetDefenseStats(user,target)} (:1140-1143). */
        @Override
        public MoveStats pbGetDefenseStats(BattleMove move, Battler user, Battler target) {
            MoveStats ret = super.pbGetDefenseStats(move, user, target);      // :1141 ret1,_ret2 = super
            return new MoveStats(ret.value, 6);                              // :1142 return ret1, 6
        }
    }

    /** {@code class PokeBattle_Move_0AA < PokeBattle_ProtectMove} (:1151-1156): Detect, Protect. */
    public static class PokeBattle_Move_0AA extends MoveEffectsGeneric.PokeBattle_ProtectMove {

        /** {@code initialize(battle,move)} (:1152-1155). */
        public PokeBattle_Move_0AA() {
            effect = PBEffects.Battler.Protect;                              // :1154 @effect = PBEffects::Protect
        }
    }

    /** {@code class PokeBattle_Move_0AB < PokeBattle_ProtectMove} (:1164-1170): Quick Guard. */
    public static class PokeBattle_Move_0AB extends MoveEffectsGeneric.PokeBattle_ProtectMove {

        /** {@code initialize(battle,move)} (:1165-1169). */
        public PokeBattle_Move_0AB() {
            effect = PBEffects.Side.QuickGuard;                              // :1167
            sidedEffect = true;                                              // :1168
        }
    }

    /** {@code class PokeBattle_Move_0AC < PokeBattle_ProtectMove} (:1178-1184): Wide Guard. */
    public static class PokeBattle_Move_0AC extends MoveEffectsGeneric.PokeBattle_ProtectMove {

        /** {@code initialize(battle,move)} (:1179-1183). */
        public PokeBattle_Move_0AC() {
            effect = PBEffects.Side.WideGuard;                               // :1181
            sidedEffect = true;                                              // :1182
        }
    }

    /** {@code class PokeBattle_Move_0AD < PokeBattle_Move} (:1191-1203): Feint. */
    public static class PokeBattle_Move_0AD extends MoveEffectBase {

        /** {@code pbEffectAgainstTarget(user,target)} (:1192-1202). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.BurningBulwark, false);      // :1193
            target.effects.set(PBEffects.Battler.BanefulBunker, false);       // :1194
            target.effects.set(PBEffects.Battler.KingsShield, false);         // :1195
            target.effects.set(PBEffects.Battler.Protect, false);             // :1196
            target.effects.set(PBEffects.Battler.SpikyShield, false);         // :1197
            target.pbOwnSide().effects.set(PBEffects.Side.CraftyShield, false);   // :1198
            target.pbOwnSide().effects.set(PBEffects.Side.MatBlock, false);   // :1199
            target.pbOwnSide().effects.set(PBEffects.Side.QuickGuard, false);   // :1200
            target.pbOwnSide().effects.set(PBEffects.Side.WideGuard, false);  // :1201
        }
    }

    /** {@code class PokeBattle_Move_0AE < PokeBattle_Move} (:1210-1230): Mirror Move. */
    public static class PokeBattle_Move_0AE extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:1211). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :1211
        }

        /** {@code callsAnotherMove?} (:1212). */
        @Override
        public boolean callsAnotherMove(BattleMove move) {
            return true;                                                     // :1212
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:1214-1221). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            String lastMove = target.lastRegularMoveUsed;                    // :1215 target.lastRegularMoveUsed<=0
            // :1216 pbGetMoveData(...,MOVE_FLAGS)[/e/] —— 不可被 Mirror Move 复制
            if (lastMove == null || lastMove.isEmpty()
                    || !moveFlags(battle(user), lastMove).contains("e")) {
                battle(user).display("鹦鹉学舌失败了！");        // :1217
                return true;                                                 // :1218
            }
            return false;                                                    // :1220
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1223-1225). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            pbUseMoveSimple(user, target.lastRegularMoveUsed, target.index);   // :1224
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} (:1227-1229): no animation. */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets,
                                    int hitNum, boolean showAnimation) {
            // No animation (:1228)
        }
    }

    /**
     * {@code class PokeBattle_Move_0AF < PokeBattle_Move} (:1237-1319): Copycat.
     *
     * <p>{@code @moveBlacklist} (:1242-1304) is the list of function codes Copycat
     * must not copy.</p>
     */
    public static class PokeBattle_Move_0AF extends MoveEffectBase {

        /** {@code @moveBlacklist} (:1242-1304). */
        private final Array<String> moveBlacklist = new Array<>();

        /** {@code initialize(battle,move)} (:1240-1305). */
        public PokeBattle_Move_0AF() {
            moveBlacklist.add("002");                                        // :1244 Struggle
            moveBlacklist.add("014");                                        // :1245 Chatter
            moveBlacklist.add("158");                                        // :1246 Belch
            moveBlacklist.add("05C");                                        // :1248 Mimic
            moveBlacklist.add("05D");                                        // :1249 Sketch
            moveBlacklist.add("069");                                        // :1250 Transform
            moveBlacklist.add("071");                                        // :1252 Counter
            moveBlacklist.add("072");                                        // :1253 Mirror Coat
            moveBlacklist.add("073");                                        // :1254 Metal Burst
            moveBlacklist.add("09C");                                        // :1256 Helping Hand
            moveBlacklist.add("0AD");                                        // :1257 Feint
            moveBlacklist.add("0AA");                                        // :1259 Detect, Protect
            moveBlacklist.add("0AB");                                        // :1260 Quick Guard
            moveBlacklist.add("0AC");                                        // :1261 Wide Guard
            moveBlacklist.add("0E8");                                        // :1262 Endure
            moveBlacklist.add("149");                                        // :1263 Mat Block
            moveBlacklist.add("14A");                                        // :1264 Crafty Shield
            moveBlacklist.add("14B");                                        // :1265 King's Shield
            moveBlacklist.add("14C");                                        // :1266 Spiky Shield
            moveBlacklist.add("168");                                        // :1267 Baneful Bunker
            moveBlacklist.add("180");                                        // :1268 Obstruct
            moveBlacklist.add("0AE");                                        // :1270 Mirror Move
            moveBlacklist.add("0AF");                                        // :1271 Copycat (this move)
            moveBlacklist.add("0B0");                                        // :1272 Me First
            moveBlacklist.add("0B3");                                        // :1273 Nature Power
            moveBlacklist.add("0B4");                                        // :1274 Sleep Talk
            moveBlacklist.add("0B5");                                        // :1275 Assist
            moveBlacklist.add("0B6");                                        // :1276 Metronome
            moveBlacklist.add("0B1");                                        // :1278 Magic Coat
            moveBlacklist.add("0B2");                                        // :1279 Snatch
            moveBlacklist.add("117");                                        // :1280 Follow Me, Rage Powder
            moveBlacklist.add("16A");                                        // :1281 Spotlight
            moveBlacklist.add("0E6");                                        // :1283 Grudge
            moveBlacklist.add("0E7");                                        // :1284 Destiny Bond
            moveBlacklist.add("0F1");                                        // :1286 Covet, Thief
            moveBlacklist.add("0F2");                                        // :1287 Switcheroo, Trick
            moveBlacklist.add("0F3");                                        // :1288 Bestow
            moveBlacklist.add("0BQ");                                        // :1290 GRANDEUR（插件原文如此，非 16 进制）
            moveBlacklist.add("115");                                        // :1291 Focus Punch
            moveBlacklist.add("171");                                        // :1292 Shell Trap
            moveBlacklist.add("172");                                        // :1293 Beak Blast
            moveBlacklist.add("133");                                        // :1295 Hold Hands
            moveBlacklist.add("134");                                        // :1296 Celebrate
            if (Battle.NEWEST_BATTLE_MECHANICS) {                            // :1298-1304
                moveBlacklist.add("0EB");                                    // :1301 Roar, Whirlwind
                moveBlacklist.add("0EC");                                    // :1302 Circle Throw, Dragon Tail
            }
        }

        /** {@code callsAnotherMove?} (:1238). */
        @Override
        public boolean callsAnotherMove(BattleMove move) {
            return true;                                                     // :1238
        }

        /** {@code pbMoveFailed?(user,targets)} (:1307-1314). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            String lastMove = lastMoveUsed(battle(user));                    // :1308 @battle.lastMoveUsed<=0
            // :1309 @moveBlacklist.include?(pbGetMoveData(@battle.lastMoveUsed,MOVE_FUNCTION_CODE))
            if (lastMove == null || lastMove.isEmpty()
                    || moveBlacklist.contains(moveFunction(battle(user), lastMove), false)) {
                battle(user).display("但是失败了！");             // :1310
                return true;                                                 // :1311
            }
            return false;                                                    // :1313
        }

        /** {@code pbEffectGeneral(user)} (:1316-1318). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            pbUseMoveSimple(user, lastMoveUsed(battle(user)), null);          // :1317
        }
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    /** The pair {@code [type,power]} of {@code pbHiddenPower} (Move_Effects_080-0FF.rb:310). */
    private static final class HiddenPower {
        final String type;
        final int power;

        HiddenPower(String type, int power) {
            this.type = type;
            this.power = power;
        }
    }

    /**
     * {@code pbHiddenPower(pkmn)} (Move_Effects_080-0FF.rb:280-311) - a TOP-LEVEL
     * function in the plugin, not a class hook; it is used by
     * {@code PokeBattle_Move_090} here (and by the debug screen elsewhere).
     *
     * <p>The plugin builds an <em>id</em> list ({@code 0..PBTypes.maxValue} minus
     * the pseudo types and NORMAL/SHADOW, in id order) and indexes it; this
     * runtime's type identity is the internal name, so the list holds names in the
     * same id order. {@code pkmn.iv} is {@code pkmn.pokemon.ivs} (PBS stat
     * order).</p>
     */
    private static HiddenPower pbHiddenPower(Battler pkmn) {
        int[] iv = pkmn.pokemon.ivs;                                         // :283
        int idxType = 0;                                                     // :284
        int power = 60;
        Array<String> types = new Array<>();                                 // :285
        PbsData pbs = battle(pkmn).pbs();
        int maxValue = PBTypes.maxValue(pbs);                                // :286
        for (int i = 0; i <= maxValue; i++) {
            String name = typeNameById(pbs, i);
            if (name == null || PBTypes.isPseudoType(pbs, name)) {           // :287
                continue;
            }
            if ("NORMAL".equals(name) || "SHADOW".equals(name)) {            // :288 isConst?(i,PBTypes,:NORMAL/:SHADOW)
                continue;
            }
            types.add(name);                                                 // :289
        }
        idxType |= (iv[PBStats.HP] & 1);                                     // :291
        idxType |= (iv[PBStats.ATTACK] & 1) << 1;                            // :292
        idxType |= (iv[PBStats.DEFENSE] & 1) << 2;                           // :293
        idxType |= (iv[PBStats.SPEED] & 1) << 3;                             // :294
        idxType |= (iv[PBStats.SPATK] & 1) << 4;                             // :295
        idxType |= (iv[PBStats.SPDEF] & 1) << 5;                             // :296
        idxType = (types.size - 1) * idxType / 63;                           // :297
        String type = types.get(idxType);                                    // :298
        if (!Battle.NEWEST_BATTLE_MECHANICS) {                               // :299 (本工程为 true，故该分支不生效，仍照抄)
            int powerMin = 30;                                               // :300
            int powerMax = 70;                                               // :301
            power |= (iv[PBStats.HP] & 2) >> 1;                              // :302
            power |= (iv[PBStats.ATTACK] & 2);                               // :303
            power |= (iv[PBStats.DEFENSE] & 2) << 1;                         // :304
            power |= (iv[PBStats.SPEED] & 2) << 2;                           // :305
            power |= (iv[PBStats.SPATK] & 2) << 3;                           // :306
            power |= (iv[PBStats.SPDEF] & 2) << 4;                           // :307
            power = powerMin + (powerMax - powerMin) * power / 63;           // :308
        }
        return new HiddenPower(type, power);                                 // :310
    }

    /** The type whose id is {@code id}, or null for a gap in the numbering. */
    private static String typeNameById(PbsData pbs, int id) {
        if (pbs == null) {
            return null;
        }
        for (PbsData.TypeInfo type : pbs.types.values()) {
            if (type != null && type.id == id) {
                return type.internalName;
            }
        }
        return null;
    }

    /** The battle a Battler is in ({@code Battler#battle}, PokeBattle_Battler:3). */
    private static Battle battle(Battler battler) {
        return battler.battle;
    }

    /**
     * {@code @battle.switching} (set {@code true}/{@code false} around the attack
     * phase's switching pass, Battle_Phase_Attack:25/47; read by Pursuit,
     * Move_Effects_080-0FF:168/173).
     *
     * <p><b>待接线 local stub.</b> This runtime has no {@code Battle.switching}
     * yet. This package keeps its stubs in {@code MoveFxPendingApi}, which is
     * another agent's file during this batch, so the stub is declared here; it
     * should move there (or become a real {@code Battle} field) when the wiring
     * lands. It throws rather than returning a default, like every other stub in
     * this layer.</p>
     */
    private static boolean switching(Battle battle) {
        return battle.switching;                                // Battle_Phase_Attack:25 @switching
    }

    /**
     * {@code Battler#pbWeight} (PokeBattle_Battler:700-706; read by Grass Knot
     * Move_Effects_080-0FF:616 and Heat Crash :641).
     *
     * <p><b>待接线 local stub</b> - no runtime counterpart yet (the weight
     * helpers in {@code Move_Usage_Calculations} are not modelled either). Should
     * move to {@code MoveFxPendingApi} or land on {@code Battler}.</p>
     */
    private static int pbWeight(Battler battler) {
        return battler.pbWeight();                             // PokeBattle_Battler:281-292
    }

    /**
     * {@code @pp} (PokeBattle_Move.rb:12/38, read by Trump Card
     * Move_Effects_080-0FF:566).
     *
     * <p><b>待接线 local stub</b> - {@code BattleMove} exposes {@code totalpp()}
     * but not the current PP of the move being used.</p>
     */
    private static int movePp(BattleMove move) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Move.rb:38 @pp");
    }

    /**
     * {@code Battler#inTwoTurnAttack?(*functionCodes)} (PokeBattle_Battler:718-723,
     * read by Magnitude Move_Effects_080-0FF:445).
     *
     * <p><b>待接线 local stub</b> - the runtime's {@code Battler.semiInvulnerable}
     * carries the same 登记 note (it needs {@code pbGetMoveData} to map
     * {@code @effects[TwoTurnAttack]} to a function code), so the parameterized
     * form cannot be answered either.</p>
     */
    private static boolean inTwoTurnAttack(Battler battler, String... functionCodes) {
        return battler.inTwoTurnAttack(functionCodes);         // PokeBattle_Battler:718-723
    }

    /**
     * {@code pbIsBerry?(item)} (PItem_Items:99-102): {@code ITEM_TYPE==5}, looked
     * up in the item table of the battle the user is in (item identity is the
     * internal name here).
     */
    private static boolean isBerry(Battle battle, String item) {
        PbsData pbs = battle == null ? null : battle.pbs();
        PbsData.Item entry = pbs == null || item == null ? null : pbs.item(item);
        return entry != null && entry.isBerry();
    }

    /**
     * {@code @battle.choices[idxBattler][0] == :UseMove}: this runtime's
     * {@code Battle.choices(idx)} returns the action as the String
     * {@code ":UseMove"}/{@code ":Shift"}/{@code ":None"} (Battle.java's
     * {@code choices}).
     */
    private static boolean choiceIsUseMove(Battle battle, int idxBattler) {
        return ":UseMove".equals(choiceAction(battle, idxBattler));
    }

    /** {@code @battle.choices[idxBattler][0] == :None} (see {@link #choiceIsUseMove}). */
    private static boolean choiceIsNone(Battle battle, int idxBattler) {
        return ":None".equals(choiceAction(battle, idxBattler));
    }

    private static String choiceAction(Battle battle, int idxBattler) {
        Object[] choices = battle.choices(idxBattler);
        return choices == null || choices.length == 0 || choices[0] == null
                ? "" : String.valueOf(choices[0]);
    }

    /**
     * {@code choose_best_type(target,default_type=0)}
     * (Move_Effects_080-0FF.rb:843-888) - a TOP-LEVEL function in the plugin, not a
     * class hook; only {@code PokeBattle_Move_09F} (:805) uses it. Transcribed with
     * the same shape as {@link #pbHiddenPower}: the plugin iterates type <em>ids</em>
     * and returns an id, this runtime iterates the same ids to their internal names
     * and returns the name ({@code default_type} stays the plugin's id, used only
     * for the no-candidate fallback).
     */
    private static String chooseBestType(Battler target, int defaultType) {
        PbsData pbs = pbs(target);
        String type1 = type1(target);                                        // :845
        // :846 type2 = (target.type2 && target.type2 != -1) ? target.type2 : nil
        Array<String> targetTypes = target.types();
        String type2 = targetTypes.size > 1 ? targetTypes.get(1) : null;
        // :847 type3 = (effects[Type3] == -1) ? nil : effects[Type3]
        String type3 = target.effects.stringVal(PBEffects.Battler.Type3);

        int bestAttackEff = 0;                                               // :849
        Array<String> bestCandidates = new Array<>();                        // :850

        // 第一遍：寻找最大克制倍率，并收集所有达到该倍率的属性 (:852-865)
        int maxValue = PBTypes.maxValue(pbs);                                // :853 0..maxValue-1
        for (int i = 0; i < maxValue; i++) {
            String attackType = typeNameById(pbs, i);
            if (attackType == null) {
                continue;
            }
            // :854 水属性在烈日天气下无效
            if ("WATER".equals(attackType)
                    && battle(target).pbWeather() == PBWeather.HarshSun) {
                continue;
            }
            // :855 火属性在暴雨天气下无效
            if ("FIRE".equals(attackType)
                    && battle(target).pbWeather() == PBWeather.HeavyRain) {
                continue;
            }
            int eff = PBTypes.getCombinedEffectiveness(pbs, attackType, type1, type2, type3);   // :856
            if (eff < 16) {                                                  // :857 只考虑至少1倍伤害的属性
                continue;
            }
            if (eff > bestAttackEff) {                                       // :859
                bestAttackEff = eff;
                bestCandidates.clear();
                bestCandidates.add(attackType);                              // :861
            } else if (eff == bestAttackEff) {                               // :862
                bestCandidates.add(attackType);                              // :863
            }
        }

        if (bestCandidates.size == 0) {                                      // :868
            return typeNameById(pbs, defaultType);
        }

        // 第二遍：从候选里选最抗打（目标反击总倍率最小）的 (:870-885)
        String bestType = null;                                              // :871
        Integer minResistanceSum = null;                                     // :872
        for (String candidate : bestCandidates) {                            // :874
            int sum = 0;                                                     // :876
            sum += PBTypes.getCombinedEffectiveness(pbs, type1, candidate, null, null);   // :877
            if (type2 != null) {                                             // :878
                sum += PBTypes.getCombinedEffectiveness(pbs, type2, candidate, null, null);
            }
            if (type3 != null) {                                             // :879
                sum += PBTypes.getCombinedEffectiveness(pbs, type3, candidate, null, null);
            }
            if (minResistanceSum == null || sum < minResistanceSum) {         // :881
                minResistanceSum = sum;
                bestType = candidate;                                        // :883
            }
        }
        return bestType == null ? typeNameById(pbs, defaultType) : bestType;  // :887
    }

    /** {@code target.type1} (the raw first type; null when the battler has none). */
    private static String type1(Battler target) {
        Array<String> types = target == null ? null : target.types();
        return types == null || types.size < 1 ? null : types.get(0);
    }

    /** The PBS table of the battle a Battler is in. */
    private static PbsData pbs(Battler battler) {
        return battle(battler).pbs();
    }

    /** The type <em>id</em> of a type internal name, or -1 when unknown (the reverse of {@link #typeNameById}). */
    private static int typeIdById(PbsData pbs, String type) {
        if (pbs == null || type == null) {
            return -1;
        }
        PbsData.TypeInfo info = pbs.types.get(type);
        return info == null ? -1 : info.id;
    }

    /** {@code PBTypes.getName(id)} (Compiler_PBS:446-448): the type's localised name. */
    private static String typeDisplayName(Battle battle, String type) {
        return PBTypes.getName(battle == null ? null : battle.pbs(), type);
    }

    /**
     * {@code getConst(PBMoves,:X)} used as an <em>animation id</em>
     * (Move_Effects_080-0FF.rb:1054-1069, :831): this runtime's move identity is the
     * internal name, so the PBS move id is looked up and the Ruby {@code || id}
     * fallback is kept for an unknown move.
     */
    private static int moveId(Battle battle, String moveInternalName, int fallback) {
        PbsData.Move entry = battle == null || battle.pbs() == null
                ? null : battle.pbs().move(moveInternalName);
        return entry == null ? fallback : entry.id;
    }

    /** {@code pbGetMoveData(id,MOVE_FUNCTION_CODE)} (PBMove.rb:32), by internal name. */
    private static String moveFunction(Battle battle, String moveInternalName) {
        PbsData.Move entry = battle == null || battle.pbs() == null
                ? null : battle.pbs().move(moveInternalName);
        return entry == null ? null : entry.function;
    }

    /** {@code pbGetMoveData(id,MOVE_FLAGS)} (PBMove.rb:32), by internal name ({@code ""} when unknown). */
    private static String moveFlags(Battle battle, String moveInternalName) {
        PbsData.Move entry = battle == null || battle.pbs() == null
                ? null : battle.pbs().move(moveInternalName);
        return entry == null || entry.flags == null ? "" : entry.flags;
    }

    /**
     * {@code Battler#isCommander?} (PokeBattle_Battler, read by
     * Move_Effects_080-0FF:804 {@code b.isCommander?}).
     *
     * <p><b>待接线 local stub</b> - the runtime has no commander state.</p>
     */
    private static boolean isCommander(Battler battler) {
        return battler.isCommander();
    }

    /**
     * {@code @battle.lastMoveUsed} (PokeBattle_Battle, read by Copycat
     * Move_Effects_080-0FF:1308/1309).
     *
     * <p><b>待接线 local stub</b> - the runtime tracks {@code lastMoveUsed} per
     * Battler ({@code Battler.lastMoveUsed}, PokeBattle_Battler:31), not on the
     * battle.</p>
     */
    private static String lastMoveUsed(Battle battle) {
        PbsData.Move m = battle.lastMoveUsed > 0 ? battle.pbs().moveById(battle.lastMoveUsed) : null;   // PokeBattle_Battle @lastMoveUsed (id; -1 = none)
        return m == null ? null : m.internalName;
    }

    /**
     * {@code Battler#pbUseMoveSimple(id,target=nil)} (read by Mirror Move
     * Move_Effects_080-0FF:1224 and Copycat :1317).
     *
     * <p><b>待接线 local stub</b> - the nested-move entry point of the battle flow
     * is not modelled yet; the move is its internal name here ({@code null} for the
     * Ruby {@code target=nil}).</p>
     */
    private static void pbUseMoveSimple(Battler user, String moveInternalName, Integer targetIndex) {
        user.pbUseMoveSimple(moveInternalName, targetIndex == null ? -1 : targetIndex);   // Battler_UseMove:152 (target=nil -> -1)
    }

    /**
     * {@code PokeBattle_Move.pbFromPBMove} (PokeBattle_Move.rb:51-59) for the
     * 48 function-code classes this file holds: the Ruby builds
     * {@code "PokeBattle_Move_#{function}"} and instantiates it on demand, so each
     * code is bound to its no-argument constructor here and
     * {@link MoveEffectRegistry#of} calls it lazily (some constructors still touch
     * not-yet-wired stubs, which must not break the whole table).
     *
     * <p>Generated by {@code __l2register.mjs} - re-run it with {@code --write}
     * after the class set changes.</p>
     */
    static void register() {
        MoveEffectRegistry.register("080", PokeBattle_Move_080::new);
        MoveEffectRegistry.register("081", PokeBattle_Move_081::new);
        MoveEffectRegistry.register("082", PokeBattle_Move_082::new);
        MoveEffectRegistry.register("083", PokeBattle_Move_083::new);
        MoveEffectRegistry.register("084", PokeBattle_Move_084::new);
        MoveEffectRegistry.register("085", PokeBattle_Move_085::new);
        MoveEffectRegistry.register("086", PokeBattle_Move_086::new);
        MoveEffectRegistry.register("087", PokeBattle_Move_087::new);
        MoveEffectRegistry.register("088", PokeBattle_Move_088::new);
        MoveEffectRegistry.register("089", PokeBattle_Move_089::new);
        MoveEffectRegistry.register("08A", PokeBattle_Move_08A::new);
        MoveEffectRegistry.register("08B", PokeBattle_Move_08B::new);
        MoveEffectRegistry.register("08C", PokeBattle_Move_08C::new);
        MoveEffectRegistry.register("08D", PokeBattle_Move_08D::new);
        MoveEffectRegistry.register("08E", PokeBattle_Move_08E::new);
        MoveEffectRegistry.register("08F", PokeBattle_Move_08F::new);
        MoveEffectRegistry.register("090", PokeBattle_Move_090::new);
        MoveEffectRegistry.register("091", PokeBattle_Move_091::new);
        MoveEffectRegistry.register("092", PokeBattle_Move_092::new);
        MoveEffectRegistry.register("093", PokeBattle_Move_093::new);
        MoveEffectRegistry.register("094", PokeBattle_Move_094::new);
        MoveEffectRegistry.register("095", PokeBattle_Move_095::new);
        MoveEffectRegistry.register("096", PokeBattle_Move_096::new);
        MoveEffectRegistry.register("097", PokeBattle_Move_097::new);
        MoveEffectRegistry.register("098", PokeBattle_Move_098::new);
        MoveEffectRegistry.register("099", PokeBattle_Move_099::new);
        MoveEffectRegistry.register("09A", PokeBattle_Move_09A::new);
        MoveEffectRegistry.register("09B", PokeBattle_Move_09B::new);
        MoveEffectRegistry.register("09C", PokeBattle_Move_09C::new);
        MoveEffectRegistry.register("09D", PokeBattle_Move_09D::new);
        MoveEffectRegistry.register("09E", PokeBattle_Move_09E::new);
        MoveEffectRegistry.register("09F", PokeBattle_Move_09F::new);
        MoveEffectRegistry.register("0A0", PokeBattle_Move_0A0::new);
        MoveEffectRegistry.register("0A1", PokeBattle_Move_0A1::new);
        MoveEffectRegistry.register("0A2", PokeBattle_Move_0A2::new);
        MoveEffectRegistry.register("0A3", PokeBattle_Move_0A3::new);
        MoveEffectRegistry.register("0A4", PokeBattle_Move_0A4::new);
        MoveEffectRegistry.register("0A5", PokeBattle_Move_0A5::new);
        MoveEffectRegistry.register("0A6", PokeBattle_Move_0A6::new);
        MoveEffectRegistry.register("0A7", PokeBattle_Move_0A7::new);
        MoveEffectRegistry.register("0A8", PokeBattle_Move_0A8::new);
        MoveEffectRegistry.register("0A9", PokeBattle_Move_0A9::new);
        MoveEffectRegistry.register("0AA", PokeBattle_Move_0AA::new);
        MoveEffectRegistry.register("0AB", PokeBattle_Move_0AB::new);
        MoveEffectRegistry.register("0AC", PokeBattle_Move_0AC::new);
        MoveEffectRegistry.register("0AD", PokeBattle_Move_0AD::new);
        MoveEffectRegistry.register("0AE", PokeBattle_Move_0AE::new);
        MoveEffectRegistry.register("0AF", PokeBattle_Move_0AF::new);
    }

}
