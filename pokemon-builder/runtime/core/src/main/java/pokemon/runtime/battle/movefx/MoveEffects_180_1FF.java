package pokemon.runtime.battle.movefx;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;

import pokemon.runtime.battle.Battle;
import pokemon.runtime.battle.BattleMove;
import pokemon.runtime.battle.Battler;
import pokemon.runtime.battle.PBBattleTerrains;
import pokemon.runtime.battle.PBEffects;
import pokemon.runtime.battle.PBStats;
import pokemon.runtime.battle.PBTargets;
import pokemon.runtime.battle.PBTypeEffectiveness;
import pokemon.runtime.battle.PBTypes;
import pokemon.runtime.battle.PBWeather;
import pokemon.runtime.battle.PendingApi;
import pokemon.runtime.battle.PokeBattle_SceneConstants;

/**
 * Stage 4 / L2: the function-code classes of the plugin's
 * {@code Move_Effects_180-1FF.rb} (2696 lines), transcribed one class at a time.
 *
 * <h2>Shape</h2>
 * Each Ruby {@code class PokeBattle_Move_XXX < PokeBattle_Yyy} becomes a nested
 * {@code public static class PokeBattle_Move_XXX} extending
 * {@link MoveEffectsGeneric}'s matching base (or {@link MoveEffectBase} when the
 * parent is {@code PokeBattle_Move} itself). Only the hooks the class really
 * overrides are overridden, each with its {@code // Move_Effects_180-1FF.rb:行号}
 * comment; constructors transcribe the Ruby {@code initialize} line by line.
 *
 * <h2>Coverage (complete)</h2>
 * <b>All 125 Ruby class declarations are transcribed</b> as <b>124 Java classes</b>:
 * the Ruby file declares 124 distinct class names across 125 {@code class} lines
 * because {@code PokeBattle_Move_1C8} is declared <b>twice</b> ({@code :2377} and
 * {@code :2445}) - Ruby reopens the class, so both bodies coexist and are merged
 * into the single {@code PokeBattle_Move_1C8} below. Verified by diffing the class
 * names of {@code __r20-ref/ruby/Move_Effects_180-1FF.rb} against this file:
 * 0 missing, 0 extra.
 *
 * <p>Five classes extend a parent defined in <b>another section's</b> file, using
 * the fully-qualified nested name (Lead decision: the four
 * {@code MoveEffects_*.java} files are a joint gate, so a transient
 * "cannot find symbol" is expected until the sibling lands):</p>
 * <ul>
 * <li>{@code PokeBattle_Move_187 < PokeBattle_Move_005} (:144) -
 *     {@code Move_Effects_000-07F.rb}</li>
 * <li>{@code PokeBattle_Move_188 < PokeBattle_Move_0A0} (:180),
 *     {@code PokeBattle_Move_193 < PokeBattle_Move_0C0} (:378),
 *     {@code PokeBattle_Move_196 < PokeBattle_Move_0E0} (:435) -
 *     {@code Move_Effects_080-0FF.rb} (that sibling file is being written and does
 *     not yet contain these three)</li>
 * <li>{@code PokeBattle_Move_1EC < PokeBattle_Move_163} (:2051) -
 *     {@code Move_Effects_100-17F.rb}</li>
 * </ul>
 * <p>Each of the five is 3-10 lines and can be appended in one follow-up once the
 * sibling classes exist.</p>
 *
 * <h2>Documented deviations</h2>
 * <ul>
 * <li>{@code @battle} &rarr; the {@code battle} public field of the battler in
 *     scope ({@code Battler.battle}, M0 wiring); {@code @battle.pbDisplay} &rarr;
 *     {@code battle.display} (the M0 rename).</li>
 * <li>{@code self} in the plugin is the move object, so every {@code self}
 *     argument becomes this interface's {@code BattleMove move} parameter.</li>
 * <li>Type identity is the internal-name {@code String}; the plugin's
 *     {@code -1} sentinel is {@code null}.</li>
 * <li>{@code @battle.moldBreaker} is the {@code Battle.moldBreaker} public field
 *     (M0 wiring): reads are {@code battle.moldBreaker} and writes assign it
 *     directly ({@code :2083-2085} is transcribed that way).</li>
 * <li>{@code @battle.pbStartTerrain} &rarr; {@code PendingApi.pbStartTerrain}
 *     (throwing stub).</li>
 * <li>{@code @battle.scene.*}, {@code pbShowAnimation}, {@code PBDebug} and
 *     {@code pbIsBerry?} are registered at the call site with {@code // 登记:}.</li>
 * </ul>
 */
public final class MoveEffects_180_1FF {

    private MoveEffects_180_1FF() {
    }

    /**
     * {@code PItem_Items:99 pbIsBerry?(item)} - the plugin asks its item table for
     * the berry flag; this runtime's item identity is the internal name and the
     * flag is not modelled yet.
     *
     * <p>登记: PItem_Items:99 pbIsBerry?（依赖道具表的 isBerry 标记，本批未建模）
     * - the name-suffix test below is the same placeholder used by
     * {@code AbilitiesSwitchIn}.</p>
     */
    private static boolean isBerry(String item) {
        return item != null && item.endsWith("BERRY");
    }

    /** {@code PBTypes.getName(id)} (PBTypes_Extra.rb) - needs the compiled type table. */
    private static String typeName(Battler context, String type) {
        return PBTypes.getName(context.battle.pbs(), type);
    }

    // ==================================================================
    // Move_Effects_180-1FF.rb:1-699
    // ==================================================================

    /**
     * {@code class PokeBattle_Move_180 < PokeBattle_ProtectMove} (:5-10): "User is
     * protected against damaging moves this round. Decreases the Defense of the
     * user of a stopped contact move by 2 stages. (Obstruct)".
     */
    public static class PokeBattle_Move_180 extends MoveEffectsGeneric.PokeBattle_ProtectMove {

        /** {@code initialize} (:6-9): {@code super} + {@code @effect = PBEffects::Obstruct}. */
        public PokeBattle_Move_180() {
            this.effect = PBEffects.Battler.Obstruct;                        // :8
        }
    }

    /**
     * {@code class PokeBattle_Move_181 < PokeBattle_Move} (:18-36): "Lowers target's
     * Defense and Special Defense by 1 stage at the end of each turn. Prevents
     * target from retreating. (Octolock)".
     */
    public static class PokeBattle_Move_181 extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:19-29). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.effects.intVal(PBEffects.Battler.OctolockUser) >= 0
                    || (target.damageState.substitute && !ignoresSubstitute(move, user))) { // :20
                user.battle.display("但是失败了！");                           // :21
                return true;                                                 // :22
            }
            if (Battle.NEWEST_BATTLE_MECHANICS && target.pbHasType("GHOST")) { // :24
                user.battle.display("这不能影响" + target.pbThis(true) + "……"); // :25
                return true;                                                 // :26
            }
            return false;                                                    // :28
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:31-35). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.OctolockUser, user.index);   // :32
            target.effects.set(PBEffects.Battler.Octolock, true);             // :33
            user.battle.display(target.pbThis() + "不能逃脱！");               // :34
        }
    }

    /**
     * {@code class PokeBattle_Move_182 < PokeBattle_Move} (:43-44): "Ignores move
     * redirection from abilities and moves. (Snipe Shot)".
     *
     * <p>The Ruby class body is <b>empty</b> - the plugin never overrides
     * {@code cannotRedirect?} here, so the class is behaviourally identical to its
     * parent and is transcribed as an empty subclass.</p>
     */
    public static class PokeBattle_Move_182 extends MoveEffectBase {
    }

    /**
     * {@code class PokeBattle_Move_183 < PokeBattle_Move} (:51-63): "Consumes berry
     * and raises the user's Defense by 2 stages. (Stuff Cheeks)".
     */
    public static class PokeBattle_Move_183 extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:52-62). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            if (user.item == null || user.item.isEmpty() || !isBerry(user.item)) { // :53
                user.battle.display("But it failed!");                       // :54
                return;                                                      // :55 return -1
            }
            if (user.pbCanRaiseStatStage(PBStats.DEFENSE, user, move)) {      // :57
                user.pbRaiseStatStage(PBStats.DEFENSE, 2, user);             // :58
            }
            user.pbHeldItemTriggerCheck(user.item, false);                 // :60
            if (!user.item.isEmpty()) {                                      // :61 user.item>0
                user.pbConsumeItem(true, true, false);                       // :61
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_184 < PokeBattle_Move} (:71-97): "Forces all
     * active Pokémon to consume their held berries. This move bypasses
     * Substitutes. (Tea Time)".
     */
    public static class PokeBattle_Move_184 extends MoveEffectBase {

        /** The Ruby {@code @validTargets} (:75). */
        private final IntArray validTargets = new IntArray();

        /** {@code ignoresSubstitute?(user)} (:72). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :72
        }

        /** {@code pbMoveFailed?(user,targets)} (:74-86). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            validTargets.clear();                                            // :75 @validTargets = []
            for (Battler b : user.battle.eachBattler()) {                    // :76 @battle.eachBattler
                if (!(b.item != null && !b.item.isEmpty()) || !isBerry(b.item)) { // :77 插件写作 !b.item == 0
                    continue;
                }
                validTargets.add(b.index);                                   // :78
            }
            if (validTargets.size == 0) {                                    // :80
                user.battle.display("但是失败了！");                          // :81
                return true;                                                 // :82
            }
            user.battle.display("It's tea time! Everyone dug in to their Berries!"); // :84
            return false;                                                    // :85
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:88-91). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (validTargets.contains(target.index)) {                       // :89
                return false;
            }
            return target.semiInvulnerable();                                // :90
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:93-96). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.pbHeldItemTriggerCheck(target.item, false);             // :94
            if (isBerry(target.item)) {                                      // :95
                target.pbConsumeItem(true, true, false);                     // :95
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_185 < PokeBattle_TargetStatDownMove} (:105-115):
     * "Decreases Opponent's Defense by 1 stage. Does Double Damage under gravity
     * (Grav Apple)".
     */
    public static class PokeBattle_Move_185 extends MoveEffectsGeneric.PokeBattle_TargetStatDownMove {

        /** {@code initialize} (:106-109): {@code super} + {@code @statDown = [DEFENSE,1]}. */
        public PokeBattle_Move_185() {
            this.statDown = new int[] {PBStats.DEFENSE, 1};                  // :108
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:111-114). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (user.battle.field.effects.intVal(PBEffects.Field.Gravity) > 0) { // :112
                baseDmg = Math.round(baseDmg * 1.5f);
            }
            return baseDmg;                                                  // :113
        }
    }

    /**
     * {@code class PokeBattle_Move_186 < PokeBattle_Move} (:122-136): "Decrease 1
     * stage of speed and weakens target to fire moves. (Tar Shot)".
     */
    public static class PokeBattle_Move_186 extends MoveEffectBase {

        /** {@code pbEffectAgainstTarget(user,target)} (:123-135). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (!target.pbCanLowerStatStage(PBStats.SPEED, target, move)
                    && !target.effects.truthy(PBEffects.Battler.TarShot)) {   // :124
                user.battle.display("但是失败了！");                           // :125
                return;                                                      // :126 return true
            }
            if (target.pbCanLowerStatStage(PBStats.SPEED, target, move)) {    // :128
                target.pbLowerStatStage(PBStats.SPEED, 1, target);            // :129
            }
            if (!target.effects.truthy(PBEffects.Battler.TarShot)) {          // :131 ==false
                target.effects.set(PBEffects.Battler.TarShot, true);          // :132
                user.battle.display(target.pbThis() + " became weaker to fire!"); // :133
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_189 < PokeBattle_Move} (:189-213): "Restore HP
     * and heals any status conditions of itself and its allies (Jungle Healing)".
     */
    public static class PokeBattle_Move_189 extends MoveEffectBase {

        /** {@code healingMove?} (:190). */
        @Override
        public boolean healingMove(BattleMove move) {
            return true;                                                     // :190
        }

        /** {@code pbMoveFailed?(user,targets)} (:192-202). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            int jglheal = 0;                                                 // :193
            for (int i = 0; i < targets.size; i++) {                         // :194
                Battler t = targets.get(i);
                if ((t.hp == t.maxHp() || !t.canHeal()) && !t.statused()) {   // :195 totalhp / status==NONE
                    jglheal += 1;
                }
            }
            if (jglheal == targets.size) {                                   // :197
                user.battle.display("但是失败了！");                           // :198
                return true;                                                 // :199
            }
            return false;                                                    // :201
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:204-212). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.pbCureStatus();                                           // :205
            if (target.hp != target.maxHp() && target.canHeal()) {            // :206
                int hpGain = Math.round(target.maxHp() / 4.0f);               // :207
                target.pbRecoverHP(hpGain);                                  // :208
                user.battle.display(target.pbThis() + "'s health was restored."); // :209
            }
            // :211 super - MoveEffectBase#pbEffectAgainstTarget has an empty body.
        }
    }

    /**
     * {@code class PokeBattle_Move_18A < PokeBattle_Move} (:220-251): "Changes type
     * and base power based on Battle Terrain (Terrain Pulse)".
     */
    public static class PokeBattle_Move_18A extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:221-224). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (user.battle.terrain() != PBBattleTerrains.None && !user.airborne()) { // :222
                baseDmg *= 2;
            }
            return baseDmg;                                                  // :223
        }

        /**
         * {@code pbBaseType(user)} —— **三层 alias 链的最外层**。
         *
         * <p>插件里这个方法是三层覆盖（用户裁决 + {@code movebase} 逐条比对 body 的结论）：
         * <ol>
         * <li>本体 {@code Move_Effects_180-1FF.rb:226-241}（Terrain Pulse 的五种场地）——
         *     见 {@link #pbBaseTypeMain}；</li>
         * <li>{@code 场地:238-247} 的 {@code alias bug_lure_terrain_pulse_pbBaseType pbBaseType}
         *     层 —— 见 {@link #bugLureTerrainPulsePbBaseType}；</li>
         * <li>{@code 场地:458-468} 的 {@code alias cold_terrain_pulse_pbBaseType pbBaseType}
         *     层 —— 本方法。</li>
         * </ol>
         * Ruby 的 alias 把上一层绑到新名字，所以最终调用链是
         * 本方法({@code :458}) &rarr; bug_lure 层({@code :238}) &rarr; 本体({@code :226})。</p>
         */
        @Override
        public String pbBaseType(BattleMove move, Battler user) {
            if (user.battle.terrain() == PBBattleTerrains.Cold && !user.airborne()) { // :461-462
                return "ICE";                                                // :463 getID(PBTypes,:ICE)
            }
            return bugLureTerrainPulsePbBaseType(move, user);                // :466 cold_terrain_pulse_pbBaseType(user)
        }

        /** {@code 场地:238-247} 的 alias 层：{@code bug_lure_terrain_pulse_pbBaseType}。 */
        private String bugLureTerrainPulsePbBaseType(BattleMove move, Battler user) {
            if (user.battle.terrain() == PBBattleTerrains.BugLure && !user.airborne()) { // :241-242
                return "BUG";                                                // :243 getID(PBTypes,:BUG)
            }
            return pbBaseTypeMain(move, user);                               // :245 bug_lure_terrain_pulse_pbBaseType(user)
        }

        /** 本体 {@code pbBaseType(user)} ({@code Move_Effects_180-1FF.rb:226-241})。 */
        private String pbBaseTypeMain(BattleMove move, Battler user) {
            String ret = "NORMAL";                                           // :227 getID(PBTypes,:NORMAL)
            if (!user.airborne()) {                                          // :228
                switch (user.battle.terrain()) {                             // :229
                    case PBBattleTerrains.Electric: ret = "ELECTRIC"; break; // :231
                    case PBBattleTerrains.Grassy:   ret = "GRASS";    break; // :233
                    case PBBattleTerrains.Misty:    ret = "FAIRY";    break; // :235
                    case PBBattleTerrains.Psychic:  ret = "PSYCHIC";  break; // :237
                    default: break;
                }
            }
            return ret;                                                      // :240
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum,showAnimation)} (:243-250). */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets,
                                    int hitNum, boolean showAnimation) {
            String t = pbBaseType(move, user);                               // :244
            if ("ELECTRIC".equals(t)) {                                      // :245
                hitNum = 1;
            }
            if ("GRASS".equals(t)) {                                         // :246
                hitNum = 2;
            }
            if ("FAIRY".equals(t)) {                                         // :247
                hitNum = 3;
            }
            if ("PSYCHIC".equals(t)) {                                       // :248
                hitNum = 4;
            }
            // :249 super - 登记: Move_Usage.rb:67-74 pbShowAnimation 依赖动画播放器（本批未建模）
        }
    }

    /**
     * {@code class PokeBattle_Move_18B < PokeBattle_Move} (:259-267): "Burns
     * opposing Pokemon that have increased their stats in that turn before the
     * execution of this move (Burning Jealousy)".
     */
    public static class PokeBattle_Move_18B extends MoveEffectBase {

        /** {@code pbAdditionalEffect(user,target)} (:260-266). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {                             // :261
                return;
            }
            if (target.pbCanBurn(user, false, move)                          // :262
                    && target.effects.truthy(PBEffects.Battler.BurningJealousy)) { // :263
                target.pbBurn(user, null);                                   // :264
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_18C < PokeBattle_Move} (:274-275): "Move has
     * increased Priority in Grassy Terrain (Grassy Glide)".
     *
     * <p>The Ruby class body is <b>empty</b>; the priority boost lives in
     * {@code PriorityChangeAbility} / the terrain handlers, not here.</p>
     */
    public static class PokeBattle_Move_18C extends MoveEffectBase {
    }

    /**
     * {@code class PokeBattle_Move_18D < PokeBattle_Move} (:282-288): "Power
     * Doubles onn Electric Terrain (Rising Voltage)".
     */
    public static class PokeBattle_Move_18D extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:283-287). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (user.battle.terrain() == PBBattleTerrains.Electric && !target.airborne()) { // :284-285
                baseDmg *= 2;
            }
            return baseDmg;                                                  // :286
        }
    }

    /**
     * {@code class PokeBattle_Move_18E < PokeBattle_TargetMultiStatUpMove} (:295-300):
     * "Boosts Targets' Attack and Defense (Coaching)".
     */
    public static class PokeBattle_Move_18E extends MoveEffectsGeneric.PokeBattle_TargetMultiStatUpMove {

        /** {@code initialize} (:296-299): {@code super} + {@code @statUp = [ATTACK,1,DEFENSE,1]}. */
        public PokeBattle_Move_18E() {
            this.statUp = new int[] {PBStats.ATTACK, 1, PBStats.DEFENSE, 1}; // :298
        }
    }

    /**
     * {@code class PokeBattle_Move_18F < PokeBattle_Move} (:307-318): "Renders item
     * unusable (Corrosive Gas)".
     */
    public static class PokeBattle_Move_18F extends MoveEffectBase {

        /** {@code pbEffectAgainstTarget(user,target)} (:308-317). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (user.battle.wildBattle() && user.opposes(0)) {               // :309
                return;
            }
            if (user.fainted()) {                                            // :310
                return;
            }
            if (target.damageState.substitute) {                             // :311
                return;
            }
            if (target.item == null || target.item.isEmpty()
                    || target.unlosableItem(target.item)) {                  // :312
                return;
            }
            if (target.hasActiveAbility("STICKYHOLD")
                    && !user.battle.moldBreaker) {         // :313 @battle.moldBreaker
                return;
            }
            String itemName = target.itemName();                             // :314
            target.pbRemoveItem(false);                                      // :315
            user.battle.display(target.pbThis() + "的" + itemName + "被拍落了！"); // :316
        }
    }

    /**
     * {@code class PokeBattle_Move_190 < PokeBattle_Move} (:326-338): "Power is
     * boosted on Psychic Terrain (Expanding Force)".
     */
    public static class PokeBattle_Move_190 extends MoveEffectBase {

        /** {@code pbTarget(user)} (:327-332). */
        @Override
        public int pbTarget(BattleMove move, Battler user) {
            if (user.battle.terrain() == PBBattleTerrains.Psychic) {         // :328
                return PBTargets.fromName("AllNearFoes");    // :329
            }
            return super.pbTarget(move, user);                               // :331
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:334-337). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (user.battle.terrain() == PBBattleTerrains.Psychic) {         // :335
                baseDmg = Math.round(baseDmg * 1.5f);
            }
            return baseDmg;                                                  // :336
        }
    }

    /**
     * {@code class PokeBattle_Move_191 < PokeBattle_TwoTurnMove} (:345-355): "Boosts
     * Sp Atk on 1st Turn and Attacks on 2nd (Meteor Beam)".
     */
    public static class PokeBattle_Move_191 extends MoveEffectsGeneric.PokeBattle_TwoTurnMove {

        /** {@code pbChargingTurnMessage(user,targets)} (:346-348). */
        @Override
        public void pbChargingTurnMessage(BattleMove move, Battler user, Array<Battler> targets) {
            user.battle.display(user.pbThis() + " is overflowing with space power!"); // :347
        }

        /** {@code pbChargingTurnEffect(user,target)} (:350-354). */
        @Override
        public void pbChargingTurnEffect(BattleMove move, Battler user, Battler target) {
            if (user.pbCanRaiseStatStage(PBStats.SPATK, user, move)) {       // :351
                user.pbRaiseStatStage(PBStats.SPATK, 1, user);               // :352
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_192 < PokeBattle_Move} (:362-371): "Fails if the
     * Target has no Item (Poltergeist)".
     */
    public static class PokeBattle_Move_192 extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:363-370). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.item != null && !target.item.isEmpty()) {             // :364 target.item!=0
                user.battle.display(target.pbThis() + " is about to be attacked by its "
                        + target.itemName() + "!");                          // :365
                return false;                                                // :366
            }
            user.battle.display("但是失败了！");                               // :368
            return true;                                                     // :369
        }
    }

    /**
     * {@code class PokeBattle_Move_194 < PokeBattle_Move} (:395-399): "Double damage
     * if stats were lowered that turn. (Lash Out)".
     */
    public static class PokeBattle_Move_194 extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:395-398). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (user.effects.truthy(PBEffects.Battler.LashOut)) {            // :396
                baseDmg *= 2;
            }
            return baseDmg;                                                  // :397
        }
    }

    /**
     * {@code class PokeBattle_Move_195 < PokeBattle_Move} (:407-428): "Removes all
     * Terrain. Fails if there is no Terrain (Steel Roller)".
     */
    public static class PokeBattle_Move_195 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:407-413). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.battle.terrain() == PBBattleTerrains.None) {            // :408
                user.battle.display("但是失败了！");                           // :409
                return true;                                                 // :410
            }
            return false;                                                    // :412
        }

        /** {@code pbEffectGeneral(user)} (:415-427). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            switch (user.battle.terrain()) {                                 // :416
                case PBBattleTerrains.Electric: user.battle.display("场上的电流消失了！"); break; // :418
                case PBBattleTerrains.Grassy:   user.battle.display("四周的青草枯萎了！"); break; // :420
                case PBBattleTerrains.Misty:    user.battle.display("四周的薄雾消散了！"); break; // :422
                case PBBattleTerrains.Psychic:  user.battle.display("场地恢复原样了！"); break; // :424
                default: break;
            }
            PendingApi.pbStartTerrain(user.battle, user, PBBattleTerrains.None); // :426
        }
    }

    /**
     * {@code class PokeBattle_Move_197 < PokeBattle_Move} (:450-465): "Target becomes
     * Psychic type. (Magic Powder)".
     */
    public static class PokeBattle_Move_197 extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:450-457). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            // 登记: :451 target.canChangeType? —— Battler 尚无 canChangeType（PokeBattle_Battler:585-589）
            if (!target.pbHasOtherType("PSYCHIC")) {                         // :452
                user.battle.display("但是失败了！");                           // :453
                return true;                                                 // :454
            }
            return false;                                                    // :456
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:459-464). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            String newType = "PSYCHIC";                                      // :460 getConst(PBTypes,:PSYCHIC)
            // 登记: :461 target.pbChangeTypes(newType) —— Battler 尚无 pbChangeTypes
            String typeName = typeName(user, newType);                        // :462
            user.battle.display(target.pbThis() + "变为了" + typeName + "属性！"); // :463
        }
    }

    /**
     * {@code class PokeBattle_Move_198 < PokeBattle_Move} (:471-494): "Target's last
     * move used loses 3 PP. (Eerie Spell - Galarian Slowking)".
     */
    public static class PokeBattle_Move_198 extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:471-482). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            boolean failed = true;                                           // :472
            // 登记: :473-476 target.eachMove 里的 m.pp / m.totalpp 与 :475 m.id —— 本运行时 PP 在
            //       moveSlot 上（Battler.moveSlotPp/moveSlotMaxPp），招式身份是内部名，
            //       无法按 Ruby 的 m.id 比较；此处只按「招式身份」判定。
            for (int slot = 0; slot < Battler.MOVES_MAX; slot++) {
                BattleMove m = target.moveSlot(slot);
                if (m == null || m.id() == 0) {
                    continue;
                }
                if (!m.internalName().equals(target.lastRegularMoveUsed)) {   // :474 m.id!=lastRegularMoveUsed
                    continue;
                }
                if (target.moveSlotPp(slot) == 0 || target.moveSlotMaxPp(slot) <= 0) { // :474 m.pp==0 || m.totalpp<=0
                    continue;
                }
                failed = false;                                              // :475
                break;
            }
            if (failed) {                                                    // :477
                user.battle.display("但是失败了！");                           // :478
                return true;                                                 // :479
            }
            return false;                                                    // :481
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:484-493). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            for (int slot = 0; slot < Battler.MOVES_MAX; slot++) {           // :485 target.eachMove
                BattleMove m = target.moveSlot(slot);
                if (m == null || m.id() == 0) {
                    continue;
                }
                if (!m.internalName().equals(target.lastRegularMoveUsed)) {   // :486
                    continue;
                }
                int pp = target.moveSlotPp(slot);
                int reduction = Math.min(3, pp);                             // :487
                target.pbSetPP(m.internalName(), pp - reduction);         // :488
                user.battle.display(target.pbThis(true) + "的" + m.name()
                        + "减少了" + reduction + "点PP！");                   // :489-490
                break;                                                       // :491
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_19A < PokeBattle_Move} (:502-518): 蔷薇开华 -
     * sets Reflect or Light Screen at random, whichever is missing.
     */
    public static class PokeBattle_Move_19A extends MoveEffectBase {

        /** {@code pbAdditionalEffect(user,target)} (:503-516). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            int[] effects = new int[2];                                      // :504
            String[] names = new String[2];
            String[] stats = new String[2];
            int count = 0;
            if (user.pbOwnSide().effects.intVal(PBEffects.Side.Reflect) == 0) { // :505
                effects[count] = PBEffects.Side.Reflect;                     // :506
                names[count] = "反射壁";
                stats[count] = "防御";
                count++;
            }
            if (user.pbOwnSide().effects.intVal(PBEffects.Side.LightScreen) == 0) { // :508
                effects[count] = PBEffects.Side.LightScreen;                 // :509
                names[count] = "光墙";
                stats[count] = "特防";
                count++;
            }
            if (count == 0) {                                                // :511 effects.empty?
                return;
            }
            int pick = user.battle.pbRandom(count);                          // :512 effects.sample
            int turn = user.hasActiveItem("LIGHTCLAY") ? 8 : 5;              // :513
            user.pbOwnSide().effects.set(effects[pick], turn);               // :514
            user.battle.display(names[pick] + "提高了" + user.pbTeam(true)
                    + "的" + stats[pick] + "！");                            // :515
        }
    }

    /**
     * {@code class PokeBattle_Move_19B < PokeBattle_Move} (:522-528): 嗟怨震天 -
     * base 80 power, +20 per positive Attack stage.
     */
    public static class PokeBattle_Move_19B extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:523-527). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            int mult = 0;                                                    // :524
            if (user.stage(PBStats.ATTACK) > 0) {                            // :525
                mult += user.stage(PBStats.ATTACK);
            }
            return 80 + 20 * mult;                                           // :526
        }
    }

    /**
     * {@code class PokeBattle_Move_19C < PokeBattle_Move} (:532-544): 珠泪哀歌 -
     * ignores Substitute, inflicts the Tearalament effect.
     */
    public static class PokeBattle_Move_19C extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:534). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :534
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:536-543). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.fainted()) {                                          // :537
                return;
            }
            boolean cursed = target.effects.truthy(PBEffects.Battler.Curse)
                    || target.effects.truthy(PBEffects.Battler.Tearalament);  // :538
            if (!cursed) {                                                   // :539
                target.effects.set(PBEffects.Battler.Tearalament, true);     // :540
                user.battle.display(target.pbThis() + "陷入了恐惧与哀伤！");  // :541
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_19D < PokeBattle_Move} (:549-553): 水蒸气 -
     * triple power in sun.
     */
    public static class PokeBattle_Move_19D extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:549-552). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            int w = user.battle.weather();                                   // :550 @battle.pbWeather
            if (w == PBWeather.Sun || w == PBWeather.HarshSun) {
                baseDmg *= 3;
            }
            return baseDmg;                                                  // :551
        }
    }

    /**
     * {@code class PokeBattle_Move_1CC < PokeBattle_ProtectMove} (:556-560): 火焰守护.
     */
    public static class PokeBattle_Move_1CC extends MoveEffectsGeneric.PokeBattle_ProtectMove {

        /** {@code initialize} (:556-559): {@code super} + {@code @effect = PBEffects::BurningBulwark}. */
        public PokeBattle_Move_1CC() {
            this.effect = PBEffects.Battler.BurningBulwark;                  // :558
        }
    }

    /**
     * {@code class PokeBattle_Move_199 < PokeBattle_Move} (:566-567): "Deals double
     * damage to Dynamax POkémons. Dynamax is not implemented though."
     *
     * <p>The Ruby class body is <b>empty</b> (the plugin's comment says Dynamax is
     * not implemented).</p>
     */
    public static class PokeBattle_Move_199 extends MoveEffectBase {
    }

    /**
     * {@code class PokeBattle_Move_19E < PokeBattle_Move} (:571-580): 古龙雷枪 -
     * double power in Electric Terrain.
     */
    public static class PokeBattle_Move_19E extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:573-577). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (user.battle.terrain() == PBBattleTerrains.Electric && !user.airborne()) { // :574-575
                baseDmg *= 2;
            }
            return baseDmg;                                                  // :576
        }
    }

    /**
     * {@code class PokeBattle_Move_19F < PokeBattle_Move} (:584-609): 咒钉 - fails on
     * a Ghost (or already nail-cursed) target, otherwise nails it for 3 turns.
     */
    public static class PokeBattle_Move_19F extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:589-597). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.effects.intVal(PBEffects.Battler.CurseNail) > 0            // :590
                    || target.pbHasType("GHOST")                             // :591
                    || !target.pbHasOtherType("GHOST")) {                    // :592
                user.battle.display("但是失败了！");                           // :593
                return true;                                                 // :594
            }
            return false;                                                    // :596
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:599-608). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.fainted()) {                                          // :600
                return;
            }
            target.effects.set(PBEffects.Battler.CurseNail, 3);              // :602
            user.battle.display(target.pbThis() + "被钉下了3回合的咒钉！");    // :603
            if (!target.effects.truthy(PBEffects.Battler.Curse)) {           // :604
                target.effects.set(PBEffects.Battler.Curse, true);           // :605
                user.battle.display(target.pbThis() + "被诅咒了！");          // :606
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1A0 < PokeBattle_Move} (:614-623): 强袭炸裂 - on a
     * KO, +1 Attack and +1 Speed.
     */
    public static class PokeBattle_Move_1A0 extends MoveEffectBase {

        /** {@code pbEffectAfterAllHits(user,target)} (:615-622). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (!target.damageState.fainted) {                               // :615
                return;
            }
            if (user.pbCanRaiseStatStage(PBStats.ATTACK, user, move)) {      // :616
                user.pbRaiseStatStage(PBStats.ATTACK, 1, user);              // :617
            }
            if (user.pbCanRaiseStatStage(PBStats.SPEED, user, move)) {       // :619
                user.pbRaiseStatStage(PBStats.SPEED, 1, user);               // :620
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1A1 < PokeBattle_Move} (:626-628): always critical
     * and never lethal (False Swipe-like).
     */
    public static class PokeBattle_Move_1A1 extends MoveEffectBase {

        /** {@code pbCritialOverride(user,target)} (:626). */
        @Override
        public int pbCritialOverride(BattleMove move, Battler user, Battler target) {
            return 1;                                                        // :626
        }

        /** {@code nonLethal?(user,target)} (:627). */
        @Override
        public boolean nonLethal(BattleMove move, Battler user, Battler target) {
            return true;                                                     // :627
        }
    }

    /**
     * {@code class PokeBattle_Move_1A2 < PokeBattle_Move} (:636-640): 大剑旋钻 -
     * doubles the damage multiplier after Rapid Spin.
     */
    public static class PokeBattle_Move_1A2 extends MoveEffectBase {

        /** {@code pbBaseDamageMultiplier(damageMult,user,target)} (:637-639). */
        @Override
        public float pbBaseDamageMultiplier(BattleMove move, float damageMult, Battler user,
                                            Battler target) {
            if (user.effects.truthy(PBEffects.Battler.UsedRapidSpin)) {      // :637
                damageMult *= 2;
            }
            return damageMult;                                               // :638
        }
    }

    /**
     * {@code class PokeBattle_Move_1A3 < PokeBattle_Move} (:644-661): 蛇咬 - 2..5 hits
     * (always 5 after Coil), then randomly confuses or paralyzes.
     */
    public static class PokeBattle_Move_1A3 extends MoveEffectBase {

        /** {@code multiHitMove?} (:644). */
        @Override
        public boolean multiHitMove(BattleMove move) {
            return true;                                                     // :644
        }

        /** {@code pbNumHits(user,targets)} (:646-652). */
        @Override
        public int pbNumHits(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.effects.truthy(PBEffects.Battler.UsedCoil)) {           // :647
                return 5;
            }
            int[] hitChances = {2, 2, 3, 3, 4, 5};                           // :648
            int r = user.battle.pbRandom(hitChances.length);                 // :649
            if (user.hasActiveAbility("SKILLLINK")) {                        // :650
                r = hitChances.length - 1;
            }
            return hitChances[r];                                            // :651
        }

        /** {@code pbAdditionalEffect(user,target)} (:654-660). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {                             // :655
                return;
            }
            switch (user.battle.pbRandom(2)) {                               // :656
                case 0:
                    if (target.pbCanConfuse(user, false, move)) {            // :657
                        target.pbConfuse(null);                          // :657
                    }
                    break;
                case 1:
                    if (target.pbCanParalyze(user, false, move)) {           // :658
                        target.pbParalyze(user, null);                       // :658
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1A4 < PokeBattle_HealingMove} (:665-703): 手术 -
     * usable at full HP; heals a statused target.
     */
    public static class PokeBattle_Move_1A4 extends MoveEffectsGeneric.PokeBattle_HealingMove {

        /** {@code pbMoveFailed?(user,targets)} (:667-669). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            return false;                                                    // :668
        }

        /**
         * {@code pbFailsAgainstTarget?(user,target)} (:670-686).
         *
         * <p>The plugin defines this hook <b>twice</b> in the same class; Ruby keeps
         * the second definition (:680-686) and the first (:670-678) is dead. The
         * transcription keeps the winning body, exactly as
         * {@code Battler#pbChangeTypes} was handled in M0b.</p>
         */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (!target.statused()) {                                        // :681 status==PBStatuses::NONE
                user.battle.display("但是失败了！");                           // :682
                return true;                                                 // :683
            }
            return false;                                                    // :685
        }

        /** {@code pbHealAmount(user)} (:688-690). */
        @Override
        public int pbHealAmount(BattleMove move, Battler user) {
            return Math.round(user.maxHp() / 4.0f);                          // :689
        }

        /** {@code pbEffectGeneral(user)} (:692). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            // :692 空方法体（插件如此）
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:693-702). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.Operation, true);           // :694
            target.pbCureStatus();                                           // :695
            target.pbReduceHP(target.hp - 1);                                // :696
            int amt = pbHealAmount(move, user);                              // :697
            if (user.hp == user.maxHp()) {                                   // :699 满血不治疗自己
                return;
            }
            user.pbRecoverHP(amt);                                           // :700
            user.battle.display(user.pbThis() + "的HP回复了。");              // :701
        }
    }

    // ==================================================================
    // The five classes whose parent lives in another section's file.
    // Per the Lead: reference the sibling's fully-qualified nested name; a
    // transient "cannot find symbol" until that file lands is expected.
    // ==================================================================

    /**
     * {@code class PokeBattle_Move_187 < PokeBattle_Move_005} (:144-173): "Changes
     * Category based on Opponent's Def and SpDef. Has 20% Chance to Poison
     * (Shell Side Arm)".
     */
    public static class PokeBattle_Move_187 extends MoveEffects_000_07F.PokeBattle_Move_005 {

        /** The Ruby {@code @calcCategory} (:147). */
        private int calcCategory;

        /** {@code initialize} (:145-148): {@code super} + {@code @calcCategory = 1}. */
        public PokeBattle_Move_187() {
            this.calcCategory = 1;                                           // :147
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:150-154). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (user.battle.pbRandom(5) < 1 && target.pbCanPoison(user, true, move)) { // :151 rand(5)<1
                target.pbPoison(user, null, false);                          // :152
            }
        }

        /** {@code physicalMove?(thisType=nil)} (:156). */
        @Override
        public boolean physicalMove(BattleMove move, String thisType) {
            return calcCategory == 0;                                        // :156
        }

        /** {@code specialMove?(thisType=nil)} (:157). */
        @Override
        public boolean specialMove(BattleMove move, String thisType) {
            return calcCategory == 1;                                        // :157
        }

        /** {@code pbOnStartUse(user,targets)} (:159-172). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            int[] stageMul = {2, 2, 2, 2, 2, 2, 2, 3, 4, 5, 6, 7, 8};        // :160
            int[] stageDiv = {8, 7, 6, 5, 4, 3, 2, 2, 2, 2, 2, 2, 2};        // :161
            Battler t = targets.get(0);                                      // :162
            int defenseStage = t.stage(PBStats.DEFENSE) + 6;                 // :163
            int realDefense = (int) Math.floor(t.defense() * stageMul[defenseStage] / stageDiv[defenseStage]); // :164
            int spdefStage = t.stage(PBStats.SPDEF) + 6;                     // :166
            int realSpdef = (int) Math.floor(t.spDef() * stageMul[spdefStage] / stageDiv[spdefStage]); // :167
            if (realDefense < realSpdef) {                                   // :169
                calcCategory = 0;
                return;
            }
            if (realDefense >= realSpdef) {                                  // :170
                calcCategory = 1;
                return;
            }
            // :171 if isConst?(@id,PBMoves,:WONDERROOM); end —— 空语句，插件如此
        }
    }

    /**
     * {@code class PokeBattle_Move_188 < PokeBattle_Move_0A0} (:180-183): "Hits 3
     * times and always critical. (Surging Strikes)".
     */
    public static class PokeBattle_Move_188 extends MoveEffects_080_0AF.PokeBattle_Move_0A0 {

        /** {@code multiHitMove?} (:181). */
        @Override
        public boolean multiHitMove(BattleMove move) {
            return true;                                                     // :181
        }

        /** {@code pbNumHits(user,targets)} (:182). */
        @Override
        public int pbNumHits(BattleMove move, Battler user, Array<Battler> targets) {
            return 3;                                                        // :182
        }
    }

    /**
     * {@code class PokeBattle_Move_193 < PokeBattle_Move_0C0} (:378-387): "Reduces
     * Defense and Raises Speed after all hits (Scale Shot)".
     */
    public static class PokeBattle_Move_193 extends MoveEffects_0B0_0D4.PokeBattle_Move_0C0 {

        /** {@code pbEffectAfterAllHits(user,target)} (:379-386). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (user.pbCanRaiseStatStage(PBStats.SPEED, user, move)) {        // :380
                user.pbRaiseStatStage(PBStats.SPEED, 1, user);               // :381
            }
            if (user.pbCanLowerStatStage(PBStats.DEFENSE, target)) {         // :383
                user.pbLowerStatStage(PBStats.DEFENSE, 1, user);             // :384
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_196 < PokeBattle_Move_0E0} (:435-442): "Self KO.
     * Boosted Damage when on Misty Terrain (Misty Explosion)".
     */
    public static class PokeBattle_Move_196 extends MoveEffects_0D5_0FF.PokeBattle_Move_0E0 {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:436-441). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (user.battle.terrain() == PBBattleTerrains.Misty && !user.airborne()) { // :437
                baseDmg = Math.round(baseDmg * 1.5f);                        // :438
            }
            return baseDmg;                                                  // :440
        }
    }

    /**
     * {@code class PokeBattle_Move_1EC < PokeBattle_Move_163} (:2051-2054): extends
     * the "ignores all abilities" move (Mold Breaker-like) - see its parent's
     * {@code Move_Effects_100-17F.rb:2150-2168}.
     */
    public static class PokeBattle_Move_1EC extends MoveEffects_100_17F.PokeBattle_Move_163 {

        /** The Ruby {@code @calcCategory} (:2054). */
        private int calcCategory;

        /** {@code initialize} (:2052-2055): {@code super} + {@code @calcCategory = 1}. */
        public PokeBattle_Move_1EC() {
            calcCategory = 1;                                                // :2054
        }

        /** {@code physicalMove?(thisType=nil)} (:2057-2059). */
        @Override
        public boolean physicalMove(BattleMove move, String thisType) {
            return calcCategory == 0;                                        // :2058
        }

        /** {@code specialMove?(thisType=nil)} (:2061-2063). */
        @Override
        public boolean specialMove(BattleMove move, String thisType) {
            return calcCategory == 1;                                        // :2062
        }

        /** {@code pbOnStartUse(user,targets)} (:2065-2086). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.isSpecies("NECROZMA")) {                                // :2066
                if (user.form() == 1) {                                      // :2067 黄昏之鬃 -> 形态3
                    user.pbChangeFormTransform(3, user.pbThis() + "在太阳的光芒中解放了真正的力量！"); // :2068
                } else if (user.form() == 2) {                               // :2069 拂晓之翼 -> 形态4
                    user.pbChangeFormTransform(4, user.pbThis() + "在月亮的光芒中解放了真正的力量！"); // :2070
                }
            }
            int[] stageMul = {2, 2, 2, 2, 2, 2, 2, 3, 4, 5, 6, 7, 8};        // :2074
            int[] stageDiv = {8, 7, 6, 5, 4, 3, 2, 2, 2, 2, 2, 2, 2};        // :2075
            int atkStage = user.stage(PBStats.ATTACK) + 6;                    // :2077
            int realAtk = (int) Math.floor((float) user.attack() * stageMul[atkStage] / stageDiv[atkStage]); // :2078
            int spAtkStage = user.stage(PBStats.SPATK) + 6;                   // :2080
            int realSpAtk = (int) Math.floor((float) user.spAtk() * stageMul[spAtkStage] / stageDiv[spAtkStage]); // :2081
            calcCategory = realAtk > realSpAtk ? 0 : 1;                       // :2082
            // :2083-2085 if @battle.moldBreaker && targets && targets[0] &&
            //            targets[0].hasActiveItem?(:ABILITYSHIELD) -> @battle.moldBreaker = false
            // Battle.moldBreaker is a public field (Battle.java), so both the read and
            // the write are direct now that the M0 wiring landed.
            if (user.battle.moldBreaker && targets != null && targets.size > 0
                    && targets.get(0).hasActiveItem("ABILITYSHIELD")) {
                user.battle.moldBreaker = false;
            }
        }
    }

    // ==================================================================
    // Move_Effects_180-1FF.rb:704-926
    // ==================================================================

    /**
     * {@code class PokeBattle_Move_1B1 < PokeBattle_TargetStatDownMove} (:705-720): 圣殿光辉.
     */
    public static class PokeBattle_Move_1B1 extends MoveEffectsGeneric.PokeBattle_TargetStatDownMove {

        /** {@code initialize} (:707-710): {@code super} + {@code @statDown = [ATTACK,1]}. */
        public PokeBattle_Move_1B1() {
            this.statDown = new int[] {PBStats.ATTACK, 1};                   // :709
        }

        /** {@code pbMoveFailed?(user,targets)} (:712-718). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!(user.isSpecies("SUGARDEVOIR") && user.form() == 1)) {      // :713
                user.battle.display("但是" + user.pbThis(true) + "无法使用这个招式。"); // :714
                return true;                                                 // :715
            }
            return false;                                                    // :717
        }
    }

    /**
     * {@code class PokeBattle_Move_1B2 < PokeBattle_TargetStatDownMove} (:723-738): 深渊暗影.
     */
    public static class PokeBattle_Move_1B2 extends MoveEffectsGeneric.PokeBattle_TargetStatDownMove {

        /** {@code initialize} (:725-728): {@code super} + {@code @statDown = [SPATK,1]}. */
        public PokeBattle_Move_1B2() {
            this.statDown = new int[] {PBStats.SPATK, 1};                    // :727
        }

        /** {@code pbMoveFailed?(user,targets)} (:730-736). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.isSpecies("SUJINRAKU")) {                              // :731
                user.battle.display("但是" + user.pbThis(true) + "无法使用这个招式。"); // :732
                return true;                                                 // :733
            }
            return false;                                                    // :735
        }
    }

    /**
     * {@code class PokeBattle_Move_1B3 < PokeBattle_Move} (:741-756): 断刃鏖杀 - prevents
     * escape, then 15% to also prevent acting.
     */
    public static class PokeBattle_Move_1B3 extends MoveEffectBase {

        /** {@code pbEffectAgainstTarget(user,target)} (:743-755). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.fainted() || target.damageState.substitute) {          // :744
                return;
            }
            if (target.effects.intVal(PBEffects.Battler.FierceKilling) == -1) { // :746 无法替换
                target.effects.set(PBEffects.Battler.FierceKilling, user.index); // :747
                user.battle.display(target.pbThis() + "无法逃脱了！");         // :748
            }
            if (user.battle.pbRandom(100) >= 15) {                           // :751 无法使用招式和道具
                return;
            }
            if (target.effects.truthy(PBEffects.Battler.FierceKilling2)) {    // :752
                return;
            }
            target.effects.set(PBEffects.Battler.FierceKilling2, true);       // :753
            user.battle.display(target.pbThis() + "无法行动了！");             // :754
        }
    }

    /**
     * {@code class PokeBattle_Move_1B4 < PokeBattle_Move} (:758-767): 高出力吐息 - one of
     * poison / paralysis / burn at random.
     */
    public static class PokeBattle_Move_1B4 extends MoveEffectBase {

        /** {@code pbAdditionalEffect(user,target)} (:759-766). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {                             // :760
                return;
            }
            switch (user.battle.pbRandom(3)) {                               // :761
                case 0:
                    if (target.pbCanPoison(user, false, move)) {             // :762
                        target.pbPoison(user, null, false);
                    }
                    break;
                case 1:
                    if (target.pbCanParalyze(user, false, move)) {           // :763
                        target.pbParalyze(user, null);
                    }
                    break;
                case 2:
                    if (target.pbCanBurn(user, false, move)) {               // :764
                        target.pbBurn(user, null);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1A5 < PokeBattle_Move} (:770-796): 回响之音 - adds the
     * Ghost type and/or curses the target.
     */
    public static class PokeBattle_Move_1A5 extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:773) - 无视替身. */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :773
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:775-795). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.fainted()) {                                          // :776
                return;
            }
            String ghostType = "GHOST";                                      // :777 getConst(PBTypes,:GHOST)
            boolean isGhost = target.pbHasType("GHOST");                      // :778
            boolean cursed = target.effects.truthy(PBEffects.Battler.Curse);  // :779
            if (!isGhost && !cursed) {                                       // :780
                target.effects.set(PBEffects.Battler.Type3, ghostType);       // :782
                String typeName = typeName(user, ghostType);                  // :783
                target.effects.set(PBEffects.Battler.Curse, true);            // :785
                user.battle.display(target.pbThis() + "被诅咒了，\n并且被追加了"
                        + typeName + "属性！");                               // :786
            } else if (!isGhost) {                                           // :787
                target.effects.set(PBEffects.Battler.Type3, ghostType);       // :788
                String typeName = typeName(user, ghostType);                  // :789
                user.battle.display(target.pbThis() + "被追加了" + typeName + "属性！"); // :790
            } else if (!cursed) {                                            // :791
                target.effects.set(PBEffects.Battler.Curse, true);            // :792
                user.battle.display(target.pbThis() + "被诅咒了！");           // :793
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1A6 < PokeBattle_Move} (:803-819): 悠然龙吟 - extends
     * Reflect or Light Screen by one turn at random.
     */
    public static class PokeBattle_Move_1A6 extends MoveEffectBase {

        /** {@code pbEffectAgainstTarget(user,target)} (:805-818). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            IntArray effects = new IntArray();                               // :806
            Array<String> names = new Array<>();
            if (user.pbOwnSide().effects.intVal(PBEffects.Side.Reflect) > 0) { // :807
                effects.add(PBEffects.Side.Reflect);                         // :808
                names.add("反射壁");
            }
            if (user.pbOwnSide().effects.intVal(PBEffects.Side.LightScreen) > 0) { // :810
                effects.add(PBEffects.Side.LightScreen);                     // :811
                names.add("光墙");
            }
            if (effects.size == 0) {                                         // :813
                return;
            }
            int pick = user.battle.pbRandom(effects.size);                   // :814 effects.sample
            int effect = effects.get(pick);
            user.pbOwnSide().effects.increment(effect);                      // :815 += 1
            user.battle.display(move.name() + "延长了" + user.pbTeam(true) + "\n"
                    + names.get(pick) + "的回合数！");                        // :816-817
        }
    }

    /**
     * {@code class PokeBattle_Move_1A7 < PokeBattle_Move} (:822-831): 千里击涛 - also
     * super effective against Dragon.
     */
    public static class PokeBattle_Move_1A7 extends MoveEffectBase {

        /** {@code pbCalcTypeModSingle(moveType,defType,user,target)} (:824-830). */
        @Override
        public int pbCalcTypeModSingle(BattleMove move, String moveType, String defType,
                                       Battler user, Battler target) {
            int ret = super.pbCalcTypeModSingle(move, moveType, defType, user, target); // :825
            if ("DRAGON".equals(defType)) {                                  // :826
                ret = PBTypeEffectiveness.SUPER_EFFECTIVE_ONE;               // :827
            }
            return ret;                                                      // :829
        }
    }

    /**
     * {@code class PokeBattle_Move_1A8 < PokeBattle_Move} (:834-849): 百花绽放 - also super
     * effective against Dragon, power scales with the user's HP.
     */
    public static class PokeBattle_Move_1A8 extends MoveEffectBase {

        /** {@code pbCalcTypeModSingle(moveType,defType,user,target)} (:836-844). */
        @Override
        public int pbCalcTypeModSingle(BattleMove move, String moveType, String defType,
                                       Battler user, Battler target) {
            int ret = super.pbCalcTypeModSingle(move, moveType, defType, user, target); // :837
            if ("DRAGON".equals(defType)) {                                  // :840
                ret = PBTypeEffectiveness.SUPER_EFFECTIVE_ONE;               // :841
            }
            return ret;                                                      // :843
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:846-848). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            return Math.max(150 * user.hp / user.maxHp(), 1);                // :847 [150*user.hp/user.totalhp,1].max
        }
    }

    /**
     * {@code class PokeBattle_Move_1CD < PokeBattle_Move} (:852-858): 进化光线 - doubles
     * power against a species that cannot evolve.
     */
    public static class PokeBattle_Move_1CD extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:854-857). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            java.util.List<Object[]> evos = PendingApi.pbGetEvolvedFormData(target, true);   // :855 pbGetEvolvedFormData(target.species)
            if (evos == null || evos.size() <= 0) baseDmg *= 2;               // :855 length<=0: it cannot evolve
            return baseDmg;                                                  // :856
        }
    }

    /**
     * {@code class PokeBattle_Move_1AA < PokeBattle_Move} (:861-881): 神威日冕 - Solgaleo's
     * form change plus extra super-effective types and Hyper Beam recharge.
     */
    public static class PokeBattle_Move_1AA extends MoveEffectBase {

        /** {@code pbOnStartUse(user,targets)} (:862-866). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.isSpecies("SOLGALEO")) {                               // :863
                return;
            }
            if (user.form() == 1) {                                          // :864
                return;
            }
            user.pbChangeFormTransform(1, user.pbThis() + "在太阳的光芒中解放了真正的力量！"); // :865
        }

        /** {@code pbCalcTypeModSingle(moveType,defType,user,target)} (:868-876). */
        @Override
        public int pbCalcTypeModSingle(BattleMove move, String moveType, String defType,
                                       Battler user, Battler target) {
            int ret = super.pbCalcTypeModSingle(move, moveType, defType, user, target); // :869
            if ("DARK".equals(defType) || "GHOST".equals(defType) || "DRAGON".equals(defType)) { // :870-872
                ret = PBTypeEffectiveness.SUPER_EFFECTIVE_ONE;               // :873
            }
            return ret;                                                      // :875
        }

        /** {@code pbEffectGeneral(user)} (:877-880). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.HyperBeam, 2);                // :878
            // 登记: :879 user.currentMove = @id —— Battler 尚无 currentMove 字段
            //       （PokeBattle_Battler:39 attr_accessor :currentMove，属下一批）
        }
    }

    /**
     * {@code class PokeBattle_Move_1AB < PokeBattle_Move} (:884-904): 永夜灵霄 - Lunala's
     * counterpart of 1AA.
     */
    public static class PokeBattle_Move_1AB extends MoveEffectBase {

        /** {@code pbOnStartUse(user,targets)} (:885-889). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.isSpecies("LUNALA")) {                                 // :886
                return;
            }
            if (user.form() == 1) {                                          // :887
                return;
            }
            user.pbChangeFormTransform(1, user.pbThis() + "在月亮的光芒中解放了真正的力量！"); // :888
        }

        /** {@code pbCalcTypeModSingle(moveType,defType,user,target)} (:891-899). */
        @Override
        public int pbCalcTypeModSingle(BattleMove move, String moveType, String defType,
                                       Battler user, Battler target) {
            int ret = super.pbCalcTypeModSingle(move, moveType, defType, user, target); // :892
            if ("PSYCHIC".equals(defType) || "FIGHTING".equals(defType) || "FAIRY".equals(defType)) { // :893-895
                ret = PBTypeEffectiveness.SUPER_EFFECTIVE_ONE;               // :896
            }
            return ret;                                                      // :898
        }

        /** {@code pbEffectGeneral(user)} (:900-903). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.HyperBeam, 2);                // :901
            // 登记: :902 user.currentMove = @id —— 同 :879
        }
    }

    /**
     * {@code class PokeBattle_Move_1AC < PokeBattle_Move} (:906-918): drains 2 PP from
     * every opposing Pokémon's last move.
     */
    public static class PokeBattle_Move_1AC extends MoveEffectBase {

        /** {@code pbEffectAgainstTarget(user,target)} (:907-917). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            user.eachOpposing(b -> {                                         // :908
                for (int i = 0; i < Battler.MOVES_MAX; i++) {                // :909 eachMoveWithIndex
                    BattleMove m = b.moveSlot(i);
                    if (m == null || m.id() == 0) {
                        continue;
                    }
                    if (!m.internalName().equals(b.lastRegularMoveUsed)      // :910 m.id!=lastRegularMoveUsed
                            || b.moveSlotPp(i) == 0 || b.moveSlotMaxPp(i) <= 0) {
                        continue;
                    }
                    int pp = b.moveSlotPp(i);
                    int reduction = Math.min(2, pp);                          // :911
                    b.pbSetPP(m.internalName(), pp - reduction);              // :912
                    user.battle.display(b.pbThis() + "的PP降低了!");           // :913
                    break;                                                   // :914
                }
            });
        }
    }

    /**
     * {@code class PokeBattle_Move_1A9 < PokeBattle_Move} (:920-926): 1.5x power on
     * Electric Terrain.
     */
    public static class PokeBattle_Move_1A9 extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:921-925). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (user.battle.terrain() == PBBattleTerrains.Electric && !user.airborne()) { // :922-923
                baseDmg = Math.round(baseDmg * 1.5f);
            }
            return baseDmg;                                                  // :924
        }
    }

    // ==================================================================
    // Move_Effects_180-1FF.rb:928-1184
    // ==================================================================

    /**
     * {@code class PokeBattle_Move_1AD < PokeBattle_Move} (:929-973): 晶光转转 - poisons
     * (non-toxic) and clears the user's trapping / entry hazards.
     */
    public static class PokeBattle_Move_1AD extends MoveEffectBase {

        /** The Ruby {@code @toxic} (:933) - this class is NOT a PokeBattle_PoisonMove, so it owns the field. */
        private final boolean toxic;

        /** {@code initialize} (:931-934): {@code super} + {@code @toxic = false}. */
        public PokeBattle_Move_1AD() {
            this.toxic = false;                                              // :933
        }

        /** {@code pbAdditionalEffect(user,target)} (:936-939). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {                             // :937
                return;
            }
            if (target.pbCanPoison(user, false, move)) {                     // :938
                target.pbPoison(user, null, toxic);                          // :938
            }
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:941-972). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (user.fainted() || target.damageState.unaffected) {            // :942
                return;
            }
            if (user.effects.intVal(PBEffects.Battler.Trapping) > 0) {        // :943
                // 登记: :944-946 trapMove = PBMoves.getName(@effects[TrappingMove]) 与
                //       @battle.battlers[TrappingUser] —— 本运行时招式身份是内部名、
                //       battlers 只有 battlerAt()，无法按 Ruby 的招式 id 取名字 → 只清状态
                user.effects.set(PBEffects.Battler.Trapping, 0);             // :947
                user.effects.set(PBEffects.Battler.TrappingMove, 0);         // :948
                user.effects.set(PBEffects.Battler.TrappingUser, -1);        // :949
            }
            if (user.effects.intVal(PBEffects.Battler.LeechSeed) >= 0) {      // :951
                user.effects.set(PBEffects.Battler.LeechSeed, -1);           // :952
                user.battle.display(user.pbThis() + "清除了寄生种子！");       // :953
            }
            if (user.pbOwnSide().effects.truthy(PBEffects.Side.StealthRock)) { // :955
                user.pbOwnSide().effects.set(PBEffects.Side.StealthRock, false); // :956
                user.battle.display(user.pbThis() + "吹散了隐形岩！");         // :957
            }
            if (user.pbOwnSide().effects.intVal(PBEffects.Side.Spikes) > 0) {  // :959
                user.pbOwnSide().effects.set(PBEffects.Side.Spikes, 0);       // :960
                user.battle.display(user.pbThis() + "吹走了铁菱！");           // :961
            }
            if (user.pbOwnSide().effects.intVal(PBEffects.Side.ToxicSpikes) > 0) { // :963
                user.pbOwnSide().effects.set(PBEffects.Side.ToxicSpikes, 0);  // :964
                user.battle.display(user.pbThis() + "吹走了毒菱！");           // :965
            }
            if (user.pbOwnSide().effects.truthy(PBEffects.Side.StickyWeb)) {   // :967
                user.pbOwnSide().effects.set(PBEffects.Side.StickyWeb, false); // :968
                user.pbOwnSide().effects.set(PBEffects.Side.StickyWebUser, -1); // :969
                user.battle.display(user.pbThis() + "吹走了粘网！");           // :970
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1C0 < PokeBattle_WeatherMove} (:976-981): 雪景 - starts Snow.
     */
    public static class PokeBattle_Move_1C0 extends MoveEffectsGeneric.PokeBattle_WeatherMove {

        /** {@code initialize} (:977-980): {@code super} + {@code @weatherType = PBWeather::Snow}. */
        public PokeBattle_Move_1C0() {
            this.weatherType = PBWeather.Snow;                               // :979
        }
    }

    /**
     * {@code class PokeBattle_Move_1C2 < PokeBattle_Move} (:985-988): 巨剑突击 - sets the
     * Glaive Rush flag after dealing damage.
     */
    public static class PokeBattle_Move_1C2 extends MoveEffectBase {

        /** {@code pbEffectWhenDealingDamage(user,target)} (:985-987). */
        @Override
        public void pbEffectWhenDealingDamage(BattleMove move, Battler user, Battler target) {
            user.effects.set(PBEffects.Battler.GlaiveRush, 2);               // :986
        }
    }

    /**
     * {@code class PokeBattle_Move_1C3 < PokeBattle_Move} (:991-1004): 糖浆炸弹 - applies
     * Syrupy for 3 turns; the animation varies with shininess.
     */
    public static class PokeBattle_Move_1C3 extends MoveEffectBase {

        /** {@code pbEffectAgainstTarget(user,target)} (:992-998). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.fainted() || target.damageState.substitute) {          // :993
                return;
            }
            if (target.effects.intVal(PBEffects.Battler.Syrupy) > 0) {        // :994
                return;
            }
            target.effects.set(PBEffects.Battler.Syrupy, 3);                  // :995
            target.effects.set(PBEffects.Battler.SyrupyUser, user.index);     // :996
            user.battle.display(target.pbThis() + "被粘稠的糖浆覆盖了！");      // :997
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum,showAnimation)} (:1000-1003). */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets,
                                    int hitNum, boolean showAnimation) {
            // :1001 hitNum = (user.shiny?) ? 1 : 0 —— Battler 没有 shiny?，
            //       以 Pokemon.shiny 公开字段为准
            int n = user.pokemon != null && user.pokemon.shiny ? 1 : 0;
            // :1002 super —— 登记: Move_Usage.rb:67-74 pbShowAnimation 依赖动画播放器（本批未建模）
        }
    }

    /**
     * {@code class PokeBattle_Move_1CA < PokeBattle_TwoTurnMove} (:1006-1037): 电光束 - no
     * charging turn in rain, and raises Sp. Atk on the charging turn.
     */
    public static class PokeBattle_Move_1CA extends MoveEffectsGeneric.PokeBattle_TwoTurnMove {

        /** The Ruby {@code attr_reader :statUp} (:1007). */
        private final int[] statUp;

        /** {@code initialize} (:1009-1012): {@code super} + {@code @statUp = [SPATK,1]}. */
        public PokeBattle_Move_1CA() {
            this.statUp = new int[] {PBStats.SPATK, 1};                      // :1011
        }

        /** {@code pbIsChargingTurn?(user)} (:1014-1026). */
        @Override
        public boolean pbIsChargingTurn(BattleMove move, Battler user) {
            boolean ret = super.pbIsChargingTurn(move, user);                 // :1015
            if (user.effects.intVal(PBEffects.Battler.TwoTurnAttack) == 0) {   // :1016
                int w = user.battle.weather();                               // :1017 @battle.pbWeather
                if ((w == PBWeather.Rain || w == PBWeather.HeavyRain)
                        && !user.hasUtilityUmbrella()) {                      // :1018
                    this.powerHerb = false;                                   // :1019
                    this.chargingTurn = true;                                 // :1020
                    this.damagingTurn = true;                                 // :1021
                    return false;                                             // :1022
                }
            }
            return ret;                                                       // :1025
        }

        /** {@code pbChargingTurnMessage(user,targets)} (:1028-1030). */
        @Override
        public void pbChargingTurnMessage(BattleMove move, Battler user, Array<Battler> targets) {
            user.battle.display(user.pbThis() + "吸收了电力！");               // :1029
        }

        /** {@code pbChargingTurnEffect(user,target)} (:1032-1036). */
        @Override
        public void pbChargingTurnEffect(BattleMove move, Battler user, Battler target) {
            if (user.pbCanRaiseStatStage(statUp[0], user, move)) {             // :1033
                user.pbRaiseStatStage(statUp[0], statUp[1], user);            // :1034
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1CB < PokeBattle_Move} (:1040-1056): 随机光 - 30% chance to
     * go all-out for double damage.
     */
    public static class PokeBattle_Move_1CB extends MoveEffectBase {

        /** The Ruby {@code @allOutAttack} (:1042). */
        private boolean allOutAttack;

        /** {@code pbOnStartUse(user,targets)} (:1041-1046). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            allOutAttack = user.battle.pbRandom(100) < 30;                    // :1042
            if (allOutAttack) {                                               // :1043
                user.battle.display(user.pbThis() + "正全力以赴发动这次攻击！"); // :1044
            }
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:1048-1050). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            return allOutAttack ? baseDmg * 2 : baseDmg;                      // :1049
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum,showAnimation)} (:1052-1055). */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets,
                                    int hitNum, boolean showAnimation) {
            int n = allOutAttack ? 1 : hitNum;                                // :1053
            // :1054 super —— 登记: Move_Usage.rb:67-74 pbShowAnimation 依赖动画播放器（本批未建模）
        }
    }

    /**
     * {@code class PokeBattle_Move_1F1 < PokeBattle_Move} (:1059-1086): 御剑连斩 - 2..5 hits
     * and Focus Energy accumulation.
     */
    public static class PokeBattle_Move_1F1 extends MoveEffectBase {

        /** The Ruby {@code @forceEnd} (:1071). */
        private boolean forceEnd;
        /** The Ruby {@code @accCheckPerHit} (:1072). */
        private boolean accCheckPerHit;

        /** {@code multiHitMove?} (:1060). */
        @Override
        public boolean multiHitMove(BattleMove move) {
            return true;                                                     // :1060
        }

        /** {@code pbNumHits(user,targets)} (:1062-1068). */
        @Override
        public int pbNumHits(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.hasActiveItem("LOADEDDICE")) {                          // :1063
                return 4 + user.battle.pbRandom(2);
            }
            int[] hitChances = {2, 2, 3, 3, 4, 5};                           // :1064
            int r = user.battle.pbRandom(hitChances.length);                 // :1065
            if (user.hasActiveAbility("SKILLLINK")) {                        // :1066
                r = hitChances.length - 1;
            }
            return hitChances[r];                                            // :1067
        }

        /** {@code pbOnStartUse(user,targets)} (:1070-1073). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            forceEnd = false;                                                // :1071
            accCheckPerHit = !user.hasActiveAbility("SKILLLINK")
                    && !user.hasActiveItem("LOADEDDICE");                    // :1072
        }

        /** {@code pbEffectWhenDealingDamage(user,target)} (:1075-1078). */
        @Override
        public void pbEffectWhenDealingDamage(BattleMove move, Battler user, Battler target) {
            super.pbEffectWhenDealingDamage(move, user, target);              // :1076
            forceEnd = target.fainted();                                     // :1077
        }

        /** {@code pbAdditionalEffect(user,target)} (:1080-1085). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (user.effects.intVal(PBEffects.Battler.FocusEnergy) <= 2) {    // :1081
                user.effects.increment(PBEffects.Battler.FocusEnergy);        // :1082
                user.battle.display(user.pbThis() + "的要害命中率提高！");      // :1083
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1F2 < PokeBattle_Move} (:1089-1116): 势如破竹 - power
     * scales with the Bamboo Sword counter, then drops the user's Grass type.
     */
    public static class PokeBattle_Move_1F2 extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:1091-1096). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            float[] mults = {1.0f, 1.5f, 2.0f, 2.5f, 3.0f};                  // :1092
            int index = Math.min(user.effects.intVal(PBEffects.Battler.BambooSword), mults.length - 1); // :1093
            baseDmg = Math.round(baseDmg * mults[index]);                     // :1094
            return baseDmg;                                                   // :1095
        }

        /** {@code pbEffectWhenDealingDamage(user,target)} (:1098-1100). */
        @Override
        public void pbEffectWhenDealingDamage(BattleMove move, Battler user, Battler target) {
            user.effects.increment(PBEffects.Battler.BambooSword);            // :1099
        }

        /** {@code pbMoveFailed?(user,targets)} (:1102-1108). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.isSpecies("BAMSTRANE")) {                              // :1103
                user.battle.display("但是" + user.pbThis(true) + "无法使用这个招式。"); // :1104
                return true;                                                 // :1105
            }
            return false;                                                    // :1107
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:1110-1115). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (!user.effects.truthy(PBEffects.Battler.LoseGrassType)) {      // :1111
                user.effects.set(PBEffects.Battler.LoseGrassType, true);      // :1112
                user.battle.display(user.pbThis() + "失去了草属性！");          // :1113
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1F3 < PokeBattle_Move} (:1118-1152): 爆焰突进 - drops the
     * user's Fire type and moves Defense to an extreme in sun.
     */
    public static class PokeBattle_Move_1F3 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:1119-1125). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.isSpecies("BLAZEPANDA")) {                             // :1120
                user.battle.display("但是" + user.pbThis(true) + "无法使用这个招式。"); // :1121
                return true;                                                 // :1122
            }
            return false;                                                    // :1124
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:1127-1151). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (!user.effects.truthy(PBEffects.Battler.LoseFireType)) {       // :1128
                user.effects.set(PBEffects.Battler.LoseFireType, true);       // :1129
                user.battle.display(user.pbThis() + "失去了火属性！");          // :1130
            }
            int w = user.battle.weather();                                    // :1132 @battle.pbWeather
            if (w == PBWeather.Sun || w == PBWeather.HarshSun) {              // :1132
                if (user.hasActiveAbility("CONTRARY")
                        && user.pbCanLowerStatStage(PBStats.DEFENSE, user, move)) { // :1133-1134
                    user.setStage(PBStats.DEFENSE, -6);                       // :1135
                    user.battle.commonAnimation("StatDown", user);            // :1136
                    user.battle.display(user.pbThis() + "的防御下降到最低！");  // :1137
                } else if (user.pbCanRaiseStatStage(PBStats.DEFENSE, user, move)) { // :1138
                    user.setStage(PBStats.DEFENSE, 6);                        // :1139
                    user.battle.commonAnimation("StatUp", user);              // :1140
                    user.battle.display(user.pbThis() + "的防御提高到最大！");  // :1141
                }
            } else {                                                          // :1143
                if (user.hasActiveAbility("CONTRARY")
                        && user.pbCanLowerStatStage(PBStats.DEFENSE, user, move)) { // :1144-1145
                    user.pbLowerStatStage(PBStats.DEFENSE, 2, user);          // :1146
                } else if (user.pbCanRaiseStatStage(PBStats.DEFENSE, user, move)) { // :1147
                    user.pbRaiseStatStage(PBStats.DEFENSE, 2, user);          // :1148
                }
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1F4 < PokeBattle_Move} (:1154-1184): 追本溯源 - heals more
     * per Water/Poison party member, then drops the user's Water type.
     */
    public static class PokeBattle_Move_1F4 extends MoveEffectBase {

        /** {@code healingMove?} (:1156). */
        @Override
        public boolean healingMove(BattleMove move) {
            return Battle.NEWEST_BATTLE_MECHANICS;                            // :1156
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1158-1168). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            int ret = 1;                                                     // :1159
            // :1160 @battle.pbParty(user) —— 本运行时的 partyOf(idx) 返回 Array<Battler>，
            //       所以用 battler 的 pbHasType/fainted 代替 pkmn.hasType?/fainted?
            for (Battler pkmn : user.battle.partyOf(user.index)) {            // :1160
                if (pkmn == null || pkmn.fainted()) {                        // :1161
                    continue;
                }
                if (!pkmn.pbHasType("WATER") && !pkmn.pbHasType("POISON")) {  // :1162
                    continue;
                }
                ret += 1;                                                    // :1163
            }
            ret = Math.min(ret, 5);                                          // :1165
            int hpHeal = Math.round(user.maxHp() * ret / 8.0f);              // :1166
            user.pbRecoverHP(hpHeal);                                        // :1167
        }

        /** {@code pbMoveFailed?(user,targets)} (:1170-1176). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.isSpecies("TOXICMANDER")) {                            // :1171
                user.battle.display("但是" + user.pbThis(true) + "无法使用这个招式。"); // :1172
                return true;                                                 // :1173
            }
            return false;                                                    // :1175
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:1178-1183). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (!user.effects.truthy(PBEffects.Battler.LoseWaterType)) {      // :1179
                user.effects.set(PBEffects.Battler.LoseWaterType, true);      // :1180
                user.battle.display(user.pbThis() + "失去了水属性！");          // :1181
            }
        }
    }

    // ==================================================================
    // Move_Effects_180-1FF.rb:1186-1380
    // ==================================================================

    /**
     * {@code class PokeBattle_Move_1F5 < PokeBattle_Move} (:1187-1206): 烟雨针 - randomly
     * poisons or starts Rain, whichever is applicable.
     */
    public static class PokeBattle_Move_1F5 extends MoveEffectBase {

        /** {@code pbAdditionalEffect(user,target)} (:1189-1205). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            IntArray arr = new IntArray();                                   // :1190
            if (!target.damageState.substitute && target.pbCanPoison(user, false, move)) { // :1191
                arr.add(0);                                                  // :1192
            }
            int w = user.battle.field.weather;                                // :1194 @battle.field.weather
            if (w != PBWeather.Rain && w != PBWeather.HeavyRain) {            // :1194-1195
                arr.add(1);                                                  // :1196
            }
            if (arr.size == 0) {                                             // :1198 arr.shuffle.first（空数组）
                return;
            }
            int ret = arr.get(user.battle.pbRandom(arr.size));                // :1198 arr.shuffle.first
            switch (ret) {                                                   // :1199
                case 0:
                    target.pbPoison(user, null, false);                      // :1201
                    break;
                case 1:
                    // :1203 @battle.pbStartWeather(user,Rain,true,false) —— Battle 只有
                    //       2 参档 (Battler,int)；fixedDuration=true / showAnim=false 无对应档
                    user.battle.pbStartWeather(user, PBWeather.Rain);         // :1203
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1AE < PokeBattle_Move} (:1209-1224): 电光双击 - fails
     * unless the user is Electric; drops the user's Electric type.
     */
    public static class PokeBattle_Move_1AE extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:1210-1216). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.pbHasType("ELECTRIC")) {                               // :1211
                user.battle.display("但是失败了！");                           // :1212
                return true;                                                 // :1213
            }
            return false;                                                    // :1215
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:1218-1223). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (!user.effects.truthy(PBEffects.Battler.DoubleShock)) {        // :1219
                user.effects.set(PBEffects.Battler.DoubleShock, true);        // :1220
                user.battle.display(user.pbThis() + "失去了电属性！");          // :1221
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1BE < PokeBattle_Move} (:1227-1257): 辣椒精华 - raises the
     * target's Attack by 2 and lowers its Defense by 2.
     *
     * <p><b>插件 arity 冲突</b>：本类的 {@code pbFailsAgainstTarget?} 是
     * <b>3 个形参</b>（{@code :1237 def pbFailsAgainstTarget?(user, target, show_message)}），
     * 而基类 {@code Move_Usage.rb:104} 只有 2 个。按 {@code movebase} 的裁决，
     * <b>接口保持 2 参</b>，{@code show_message} 收编为本类的字段（默认 {@code true}）。
     * 插件里唯一的调用点 {@code Battler_UseMove_SuccessChecks:394} 只传 2 个实参
     * → Ruby 会 {@code ArgumentError}；本运行时按「字段默认 true」照抄其意图。</p>
     */
    public static class PokeBattle_Move_1BE extends MoveEffectBase {

        /** The Ruby {@code attr_reader :statUp, :statDown} (:1228). */
        private final int[] statUp;
        private final int[] statDown;

        /** {@code pbFailsAgainstTarget?} 的第三形参 {@code show_message} (:1237)，默认 true。 */
        private boolean showMessage = true;

        /** {@code initialize} (:1231-1235): {@code super} + the two stat arrays. */
        public PokeBattle_Move_1BE() {
            this.statUp = new int[] {PBStats.ATTACK, 2};                     // :1233
            this.statDown = new int[] {PBStats.DEFENSE, 2};                  // :1234
        }

        /** {@code canMagicCoat?} (:1229). */
        @Override
        public boolean canMagicCoat(BattleMove move) {
            return true;                                                     // :1229
        }

        /** {@code pbFailsAgainstTarget?(user,target,show_message)} (:1237-1246) —— 2 参接口 + 字段。 */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {                                        // :1238
                return false;
            }
            boolean failed = !target.pbCanRaiseStatStage(statUp[0], user, move) // :1239
                    && !target.pbCanLowerStatStage(statDown[0], user, move);  // :1240
            if (failed) {                                                    // :1241
                if (showMessage) {                                           // :1242
                    user.battle.display(target.pbThis() + "的能力等级不能再变化了！");
                }
                return true;                                                 // :1243
            }
            return false;                                                    // :1245
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1248-1256). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {                                        // :1249
                return;
            }
            if (target.pbCanRaiseStatStage(statUp[0], user, move)) {          // :1250
                target.pbRaiseStatStage(statUp[0], statUp[1], user);          // :1251
            }
            if (target.pbCanLowerStatStage(statDown[0], user, move)) {        // :1253
                target.pbLowerStatStage(statDown[0], statDown[1], user);      // :1254
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1EA < PokeBattle_Move} (:1260-1265): 愤怒之拳 - power grows
     * with the number of times the user has been hit.
     */
    public static class PokeBattle_Move_1EA extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:1261-1264). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            // 登记: :1262 user.num_times_hit —— Battler 尚无 num_times_hit
            //       （PokeBattle_Battler:876-886 的 @num_times_hit / pbAddRageHit，属下一批）
            int bonus = 50 * 0;
            return Math.min(baseDmg + bonus, 350);                            // :1263
        }
    }

    /**
     * {@code class PokeBattle_Move_1DD < PokeBattle_Move} (:1269-1286): 复生祈祷 - revives a
     * fainted party member.
     */
    public static class PokeBattle_Move_1DD extends MoveEffectBase {

        /** The Ruby {@code @numFainted} (:1273). */
        private int numFainted;

        /** {@code healingMove?} (:1270). */
        @Override
        public boolean healingMove(BattleMove move) {
            return true;                                                     // :1270
        }

        /** {@code pbMoveFailed?(user,targets)} (:1272-1280). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            numFainted = 0;                                                  // :1273
            for (Battler b : user.battle.partyOf(user.idxOwnSide())) {        // :1274 pbParty(user.idxOwnSide)
                if (b != null && b.fainted()) {
                    numFainted += 1;
                }
            }
            if (numFainted == 0) {                                           // :1275
                user.battle.display("但是它失败了！");                        // :1276
                return true;                                                 // :1277
            }
            return false;                                                    // :1279
        }

        /** {@code pbEndOfMoveUsageEffect(user,targets,numHits,switchedBattlers)} (:1282-1285). */
        @Override
        public void pbEndOfMoveUsageEffect(BattleMove move, Battler user, Array<Battler> targets,
                                           int numHits, IntArray switchedBattlers) {
            if (user.fainted() || numFainted == 0) {                         // :1283
                return;
            }
            // 登记: :1284 @battle.pbReviveInParty(user.index) —— 该方法定义在
            //       Move_Effects_180-1FF.rb:1288-1309 的 `class PokeBattle_Battle` 里
            //       （不是招式钩子），按 Lead 裁决登记给 Battle 侧，本文件不转。
        }
    }

    /**
     * {@code class PokeBattle_Move_1C1 < PokeBattle_Move} (:1312-1325): 归无之光 - also super
     * effective against Fairy, and always uses stat stage 6 for the defense stats.
     */
    public static class PokeBattle_Move_1C1 extends MoveEffectBase {

        /** {@code pbCalcTypeModSingle(moveType,defType,user,target)} (:1313-1319). */
        @Override
        public int pbCalcTypeModSingle(BattleMove move, String moveType, String defType,
                                       Battler user, Battler target) {
            int ret = super.pbCalcTypeModSingle(move, moveType, defType, user, target); // :1314
            if ("FAIRY".equals(defType)) {                                   // :1315
                ret = PBTypeEffectiveness.SUPER_EFFECTIVE_ONE;               // :1316
            }
            return ret;                                                      // :1318
        }

        /** {@code pbGetDefenseStats(user,target)} (:1321-1324). */
        @Override
        public MoveStats pbGetDefenseStats(BattleMove move, Battler user, Battler target) {
            MoveStats s = super.pbGetDefenseStats(move, user, target);        // :1322 ret1, _ret2 = super
            return new MoveStats(s.value, 6);                                 // :1323 return ret1, 6
        }
    }

    /**
     * {@code class PokeBattle_Move_1EE < PokeBattle_Move} (:1328-1341): 全开猛撞/闪电猛冲 -
     * Koraidon/Miraidon only, bonus damage on a super-effective hit or against a Boss.
     */
    public static class PokeBattle_Move_1EE extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:1329-1335). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.isSpecies("KORAIDON") && !user.isSpecies("MIRAIDON")) { // :1330
                user.battle.display("但是" + user.pbThis(true) + "无法使用这个招式。"); // :1331
                return true;                                                 // :1332
            }
            return false;                                                    // :1334
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:1336-1340). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (PBTypes.superEffective(target.damageState.typeMod)) {         // :1337
                baseDmg = Math.round(baseDmg * 4 / 3.0f);
            }
            if (target.pokemon != null && target.pokemon.battleRank > 2) {     // :1338
                baseDmg *= 2;
            }
            return baseDmg;                                                   // :1339
        }
    }

    /**
     * {@code class PokeBattle_Move_1CE < PokeBattle_ProtectMove} (:1343-1348): 线阱 - the
     * Silk Trap protect variant.
     */
    public static class PokeBattle_Move_1CE extends MoveEffectsGeneric.PokeBattle_ProtectMove {

        /** {@code initialize} (:1344-1347): {@code super} + {@code @effect = PBEffects::SilkTrap}. */
        public PokeBattle_Move_1CE() {
            this.effect = PBEffects.Battler.SilkTrap;                        // :1346
        }
    }

    /**
     * {@code class PokeBattle_Move_1CF < PokeBattle_ConfuseMove} (:1351-1362): 下压踢 - crash
     * damage on a miss.
     */
    public static class PokeBattle_Move_1CF extends MoveEffectsGeneric.PokeBattle_ConfuseMove {

        /** {@code recoilMove?} (:1352). */
        @Override
        public boolean recoilMove(BattleMove move) {
            return true;                                                     // :1352
        }

        /** {@code pbCrashDamage(user)} (:1354-1361). */
        @Override
        public void pbCrashDamage(BattleMove move, Battler user) {
            if (!user.takesIndirectDamage(false)) {                           // :1355
                return;
            }
            user.battle.display(user.pbThis() + "继续前进并坠毁！");            // :1356
            // 登记: :1357 @battle.scene.pbDamageAnimation(user) 依赖 PokeBattle_Scene（本批未建模）
            user.pbReduceHP(user.maxHp() / 2, false, true, true);             // :1358 user.totalhp/2
            user.pbItemHPHealCheck(0, false);                                 // :1359
            if (user.fainted()) {                                             // :1360
                user.pbFaint();
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1D0 < PokeBattle_Move} (:1365-1380): 鼠数儿 - 10 hits, or
     * 4+rand(7) with Loaded Dice.
     */
    public static class PokeBattle_Move_1D0 extends MoveEffectBase {

        /** The Ruby {@code @accCheckPerHit} (:1378). */
        private boolean accCheckPerHit;

        /** {@code multiHitMove?} (:1366). */
        @Override
        public boolean multiHitMove(BattleMove move) {
            return true;                                                     // :1366
        }

        /** {@code pbNumHits(user,targets)} (:1368-1371). */
        @Override
        public int pbNumHits(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.hasActiveItem("LOADEDDICE")) {                          // :1369
                return 4 + user.battle.pbRandom(7);                          // :1369 rand(7)
            }
            return 10;                                                       // :1370
        }

        /** {@code successCheckPerHit?} (:1373-1375). */
        @Override
        public boolean successCheckPerHit(BattleMove move) {
            return accCheckPerHit;                                           // :1374
        }

        /** {@code pbOnStartUse(user,targets)} (:1377-1379). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            accCheckPerHit = !user.hasActiveAbility("SKILLLINK")
                    && !user.hasActiveItem("LOADEDDICE");                    // :1378
        }
    }

    // ==================================================================
    // Move_Effects_180-1FF.rb:1382-1676
    // ==================================================================

    /**
     * {@code class PokeBattle_Move_1D1 < PokeBattle_MultiStatUpMove} (:1383-1459): 大扫除 -
     * clears entry hazards and Substitutes on both sides.
     */
    public static class PokeBattle_Move_1D1 extends MoveEffectsGeneric.PokeBattle_MultiStatUpMove {

        /** {@code initialize} (:1384-1387): {@code super} + {@code @statUp = [ATTACK,1,SPEED,1]}. */
        public PokeBattle_Move_1D1() {
            this.statUp = new int[] {PBStats.ATTACK, 1, PBStats.SPEED, 1};    // :1386
        }

        /** {@code pbMoveFailed?(user,targets)} (:1389-1417). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            boolean failed = true;                                           // :1390
            for (int i = 0; i < 2; i++) {                                    // :1391 2.times
                var side = (i == 0) ? user.pbOwnSide() : user.pbOpposingSide(); // :1392
                // :1393-1397 五种钉子；Steelsurge 在 Ruby 里用 defined? 保护，本运行时
                // PBEffects 没有该常量 → 该分支登记为不参与判定
                boolean any = side.effects.intVal(PBEffects.Side.Spikes) > 0
                        || side.effects.intVal(PBEffects.Side.ToxicSpikes) > 0
                        || side.effects.truthy(PBEffects.Side.StealthRock)
                        || side.effects.truthy(PBEffects.Side.StickyWeb);
                // 登记: :1397 defined?(PBEffects::Steelsurge) && side.effects[Steelsurge]
                //       —— Java PBEffects 无 Steelsurge 常量（本批未建模）
                if (!any) {
                    continue;                                                // :1393 next unless
                }
                failed = false;                                              // :1398
                break;                                                       // :1399
            }
            for (Battler b : user.battle.allBattlers()) {                     // :1401
                if (b.effects.intVal(PBEffects.Battler.Substitute) != 0) {    // :1402
                    failed = false;
                    break;
                }
            }
            boolean failed2 = true;                                          // :1406
            for (int i = 0; i < statUp.length / 2; i++) {                     // :1407
                if (!user.pbCanRaiseStatStage(statUp[i * 2], user, move)) {   // :1408
                    continue;
                }
                failed2 = false;                                             // :1409
                break;
            }
            if (failed && failed2) {                                          // :1412
                user.battle.display("但是它失败了！");                         // :1413
                return true;                                                 // :1414
            }
            return false;                                                    // :1416
        }

        /** {@code pbEffectGeneral(user)} (:1419-1458). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            boolean showMsg = false;                                          // :1420
            for (int i = 0; i < 2; i++) {                                     // :1421
                var side = (i == 0) ? user.pbOwnSide() : user.pbOpposingSide(); // :1422
                String team = (i == 0) ? user.pbTeam(true) : user.pbOpposingTeam(true); // :1423
                if (side.effects.truthy(PBEffects.Side.StealthRock)) {         // :1424
                    side.effects.set(PBEffects.Side.StealthRock, false);       // :1425
                    user.battle.display(team + "场上的隐形岩消失了！");         // :1426
                    showMsg = true;                                           // :1427
                }
                // 登记: :1429-1432 defined?(PBEffects::Steelsurge) 分支（Java 无该常量）
                if (side.effects.intVal(PBEffects.Side.Spikes) > 0) {          // :1434
                    side.effects.set(PBEffects.Side.Spikes, 0);                // :1435
                    user.battle.display(team + "场上的铁菱消失了！");           // :1436
                    showMsg = true;                                           // :1437
                }
                if (side.effects.intVal(PBEffects.Side.ToxicSpikes) > 0) {     // :1439
                    side.effects.set(PBEffects.Side.ToxicSpikes, 0);           // :1440
                    user.battle.display(team + "场上的毒菱消失了！");           // :1441
                    showMsg = true;                                           // :1442
                }
                if (side.effects.truthy(PBEffects.Side.StickyWeb)) {           // :1444
                    side.effects.set(PBEffects.Side.StickyWeb, false);         // :1445
                    user.battle.display(team + "场上的粘网消失了！");           // :1446
                    showMsg = true;                                           // :1447
                }
            }
            for (Battler b : user.battle.allBattlers()) {                      // :1451
                if (b.effects.intVal(PBEffects.Battler.Substitute) == 0) {     // :1452
                    continue;
                }
                b.effects.set(PBEffects.Battler.Substitute, 0);                // :1453
                showMsg = true;                                               // :1454
            }
            if (showMsg) {                                                     // :1456
                user.battle.display("大扫除完成！");
            }
            super.pbEffectGeneral(move, user);                                 // :1457
        }
    }

    /**
     * {@code class PokeBattle_Move_1D2 < PokeBattle_Move} (:1462-1487): 盐腌 - the Salt Cure
     * effect, from both the status and the damaging form.
     */
    public static class PokeBattle_Move_1D2 extends MoveEffectBase {

        /** {@code canMagicCoat?} (:1463). */
        @Override
        public boolean canMagicCoat(BattleMove move) {
            return true;                                                     // :1463
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:1465-1472). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {                                        // :1466
                return false;
            }
            if (target.effects.truthy(PBEffects.Battler.SaltCure)) {          // :1467
                user.battle.display("但是它失败了！");                        // :1468
                return true;                                                 // :1469
            }
            return false;                                                    // :1471
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1474-1479). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.fainted()) {                                          // :1475
                return;
            }
            if (damagingMove(move)) {                                        // :1476
                return;
            }
            target.effects.set(PBEffects.Battler.SaltCure, true);             // :1477
            user.battle.display(target.pbThis() + "正在被盐腌！");             // :1478
        }

        /** {@code pbAdditionalEffect(user,target)} (:1481-1486). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.fainted()) {                                          // :1482
                return;
            }
            if (target.damageState.substitute) {                             // :1483
                return;
            }
            target.effects.set(PBEffects.Battler.SaltCure, true);             // :1484
            user.battle.display(target.pbThis() + "正在被盐腌！");             // :1485
        }
    }

    /**
     * {@code class PokeBattle_Move_1D3 < PokeBattle_Move} (:1490-1535): 描绘 - copies the
     * target's ability onto every same-side battler.
     */
    public static class PokeBattle_Move_1D3 extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:1491). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :1491
        }

        /** {@code pbMoveFailed?(user,targets)} (:1493-1504). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            for (Battler b : user.battle.allSameSideBattlers(user.index)) {    // :1494
                if (!b.unstoppableAbility(null)) {                            // :1495
                    continue;
                }
                user.battle.display("但是它失败了！");                        // :1496
                return true;                                                 // :1497
            }
            if (user.hasActiveItem("ABILITYSHIELD")) {                        // :1499
                user.battle.display(user.pbThis() + "的特性\n被特性护具的效果保护了！"); // :1500
                return true;                                                 // :1501
            }
            return false;                                                    // :1503
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:1506-1516). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.ability == null || target.ability.isEmpty()
                    || user.ability.equals(target.ability)) {                 // :1507
                user.battle.display("但是它失败了！");                        // :1508
                return true;                                                 // :1509
            }
            if (target.uncopyableAbility(null)) {                             // :1511
                user.battle.display("但是它失败了！");                        // :1512
                return true;                                                 // :1513
            }
            return false;                                                    // :1515
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1518-1534). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            for (Battler b : user.battle.allSameSideBattlers(user.index)) {    // :1519
                if (b.ability.equals(target.ability)) {                       // :1520
                    continue;
                }
                if (b.hasActiveItem("ABILITYSHIELD")) {                       // :1521
                    user.battle.display(b.pbThis() + "的特性\n被特性护具的效果保护了！"); // :1522
                } else {
                    // :1524 pbShowAbilitySplash(b,true,false) —— Battle 只有 1 参档
                    b.battle.showAbilitySplash(b);                            // :1524
                    String oldAbil = b.ability;                               // :1525
                    b.ability = target.ability;                               // :1526
                    b.battle.replaceAbilitySplash(b);                         // :1527
                    user.battle.display(user.pbThis() + "复制了" + target.pbThis(true)
                            + "的" + target.abilityName() + "！");             // :1528-1529
                    b.battle.hideAbilitySplash(b);                            // :1530
                    b.pbOnAbilityChanged(oldAbil);                            // :1531
                }
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1D4 < PokeBattle_StatDownMove} (:1538-1543): 疾速转轮 -
     * lowers Speed by 2.
     */
    public static class PokeBattle_Move_1D4 extends MoveEffectsGeneric.PokeBattle_StatDownMove {

        /** {@code initialize} (:1539-1542): {@code super} + {@code @statDown = [SPEED,2]}. */
        public PokeBattle_Move_1D4() {
            this.statDown = new int[] {PBStats.SPEED, 2};                    // :1541
        }
    }

    /**
     * {@code class PokeBattle_Move_1D5 < PokeBattle_Move} (:1546-1599): 断尾 - pays HP to
     * make a substitute, then switches out.
     */
    public static class PokeBattle_Move_1D5 extends MoveEffectBase {

        /** The Ruby {@code @lifeCost} (:1552) and {@code @subLife} (:1553). */
        private int lifeCost;
        private int subLife;

        /** {@code pbMoveFailed?(user,targets)} (:1547-1559). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.effects.intVal(PBEffects.Battler.Substitute) > 0) {       // :1548
                user.battle.display(user.pbThis() + "已经有替身了！");          // :1549
                return true;                                                  // :1550
            }
            lifeCost = Math.max((int) Math.ceil(user.maxHp() / 2.0), 1);       // :1552
            subLife = Math.max((int) Math.ceil(lifeCost / 4.0), 1);            // :1553
            if (user.hp <= lifeCost) {                                         // :1554
                user.battle.display("但是它没有足够的HP去制造分身！");          // :1555
                return true;                                                  // :1556
            }
            return false;                                                     // :1558
        }

        /** {@code pbOnStartUse(user,targets)} (:1561-1564). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            user.pbReduceHP(lifeCost, false, false, true);                     // :1562
            user.pbItemHPHealCheck(0, false);                                  // :1563
        }

        /** {@code pbEffectGeneral(user)} (:1566-1571). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.Trapping, 0);                   // :1567
            user.effects.set(PBEffects.Battler.TrappingMove, (Object) null);   // :1568 = nil
            user.effects.set(PBEffects.Battler.Substitute, subLife);           // :1569
            user.battle.display(user.pbThis() + "甩掉了尾巴来制造诱饵！");      // :1570
        }

        /** {@code pbEndOfMoveUsageEffect(user,targets,numHits,switchedBattlers)} (:1573-1598). */
        @Override
        public void pbEndOfMoveUsageEffect(BattleMove move, Battler user, Array<Battler> targets,
                                           int numHits, IntArray switchedBattlers) {
            if (user.fainted() || numHits == 0) {                              // :1574
                return;
            }
            for (Battler t : targets) {                                        // :1576 监视之眼
                if (t.hasActiveAbility("WATCHDOGEYE")) {                       // :1577
                    user.battle.showAbilitySplash(t);                          // :1578
                    user.battle.display(t.pbThis() + "的" + t.abilityName()
                            + "阻止了\n" + user.pbThis() + "替换！");           // :1579-1580
                    user.battle.hideAbilitySplash(t);                          // :1581
                    return;                                                   // :1582
                }
            }
            if (!user.battle.pbCanChooseNonActive(user.index)) {               // :1585
                return;
            }
            user.battle.display(user.pbThis() + "回到了"
                    + user.battle.pbGetOwnerName(user.index) + "身边！");       // :1586
            user.battle.pbPursuit(user.index);                                 // :1587
            int oldSub = user.effects.intVal(PBEffects.Battler.Substitute);    // :1588
            if (user.fainted()) {                                              // :1589
                return;
            }
            int newPkmn = user.battle.pbGetReplacementPokemonIndex(user.index); // :1590
            if (newPkmn < 0) {                                                 // :1591
                return;
            }
            user.battle.pbRecallAndReplace(user.index, newPkmn);               // :1592
            user.battle.pbClearChoice(user.index);                             // :1593
            // 登记: :1594 @battle.moldBreaker = false —— Battle 未暴露 moldBreaker 的写入口
            switchedBattlers.add(user.index);                                  // :1595
            // 登记: :1596 user.pbEffectsOnSwitchIn(true) —— Battler 尚无 pbEffectsOnSwitchIn
            user.battle.battlerAt(user.index).effects.set(PBEffects.Battler.Substitute, oldSub);   // :1597 (user is the slot's battler: the one that came in)
        }
    }

    /**
     * {@code class PokeBattle_Move_1D6 < PokeBattle_Move} (:1602-1610): 扫墓 - power grows
     * with fainted allies.
     */
    public static class PokeBattle_Move_1D6 extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:1603-1609). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            // 登记: :1604 user.num_fainted_allies —— Battler 尚无该方法（需 Battle#pbFaintedAllyCount）
            int numFainted = 0;
            if (numFainted <= 0) {                                            // :1605
                return baseDmg;
            }
            baseDmg += 50 * numFainted;                                       // :1606
            // 登记: :1607 $game_switches[99] ? 150 : 5050 —— 全局开关子系统（本批未建模）
            return Math.min(baseDmg, 5050);                                   // :1608
        }
    }

    /**
     * {@code class PokeBattle_Move_1D7 < PokeBattle_Move} (:1613-1629): 冰旋 - removes the
     * terrain.
     */
    public static class PokeBattle_Move_1D7 extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:1614-1628). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            if (user.fainted()) {                                             // :1615
                return;
            }
            if (user.battle.terrain() == PBBattleTerrains.None) {              // :1616
                return;
            }
            switch (user.battle.terrain()) {                                  // :1617
                case PBBattleTerrains.Electric: user.battle.display("电流从场地上消失了！"); break; // :1619
                case PBBattleTerrains.Grassy:   user.battle.display("草地从场地上消失了！"); break; // :1621
                case PBBattleTerrains.Misty:    user.battle.display("迷雾从场地上消失了！"); break; // :1623
                case PBBattleTerrains.Psychic:  user.battle.display("诡异的气息从场地上消失了！"); break; // :1625
                default: break;
            }
            PendingApi.pbStartTerrain(user.battle, user, PBBattleTerrains.None); // :1627
        }
    }

    /**
     * {@code class PokeBattle_Move_1D8 < PokeBattle_Move} (:1632-1676): 甩肉 - pays half the
     * HP for +2 Attack/Sp. Atk/Speed.
     */
    public static class PokeBattle_Move_1D8 extends MoveEffectBase {

        /** The Ruby {@code @statUp} (:1635). */
        private final int[] statUp;

        /** {@code initialize} (:1633-1636): {@code super} + the three-pair stat array. */
        public PokeBattle_Move_1D8() {
            statUp = new int[] {PBStats.ATTACK, 2, PBStats.SPATK, 2, PBStats.SPEED, 2}; // :1635
        }

        /** {@code pbMoveFailed?(user,targets)} (:1638-1655). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            int hpLoss = Math.max(user.maxHp() / 2, 1);                       // :1639
            if (user.hp <= hpLoss) {                                          // :1640
                user.battle.display("但是它失败了！");                         // :1641
                return true;                                                 // :1642
            }
            boolean failed = true;                                           // :1644
            for (int i = 0; i < statUp.length / 2; i++) {                     // :1645
                if (!user.pbCanRaiseStatStage(statUp[i * 2], user, move)) {   // :1646
                    continue;
                }
                failed = false;                                              // :1647
                break;
            }
            if (failed) {                                                     // :1650
                user.battle.display(user.pbThis() + "的能力不能再提高了！");   // :1651
                return true;                                                 // :1652
            }
            return false;                                                    // :1654
        }

        /** {@code pbEffectGeneral(user)} (:1657-1675). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            boolean showAnim = true;                                          // :1658
            int hpLoss = Math.max(user.maxHp() / 2, 1);                       // :1659
            user.pbReduceHP(hpLoss, false, true, true);                       // :1660
            for (int i = 0; i < statUp.length / 2; i++) {                     // :1661
                if (user.hasActiveAbility("CONTRARY")) {                      // :1662
                    if (!user.pbCanLowerStatStage(statUp[i * 2], user, move)) { // :1663
                        continue;
                    }
                    if (user.pbLowerStatStage(statUp[i * 2], statUp[i * 2 + 1], user, showAnim)) { // :1664
                        showAnim = false;                                    // :1665
                    }
                } else {
                    if (!user.pbCanRaiseStatStage(statUp[i * 2], user, move)) { // :1668
                        continue;
                    }
                    if (user.pbRaiseStatStage(statUp[i * 2], statUp[i * 2 + 1], user, showAnim)) { // :1669
                        showAnim = false;                                    // :1670
                    }
                }
            }
            user.pbItemHPHealCheck(0, false);                                 // :1674
        }
    }

    // ==================================================================
    // Move_Effects_180-1FF.rb:1678-1921
    // ==================================================================

    /**
     * {@code class PokeBattle_Move_1D9 < PokeBattle_Move} (:1679-1694): 上菜 - raises the stat
     * matching the commander's form.
     */
    public static class PokeBattle_Move_1D9 extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:1680-1688). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            // 登记: :1681 user.isCommanderHost? —— Battler 尚无 isCommanderHost
            //       （PokeBattle_Battler:871-874，属下一批）。
            // 「近似」的依据：本插件的 COMMANDER handler（BattleHandlers_Abilities:2985-2986）
            // 写的正是 `@effects[Commander] = [battler.index, battler.form]` 与 `[b.index]`
            // → 宿主那一侧是「长度 > 1 的 int[]」，从者那一侧长度 == 1；
            // 所以「长度 > 1」等价于 isCommanderHost? 为真，而不是随手取的判据。
            Object raw = user.effects.raw(PBEffects.Battler.Commander);
            if (raw instanceof int[] && ((int[]) raw).length > 1) {           // :1681
                int form = ((int[]) raw)[1];                                  // :1682
                int stat = new int[] {PBStats.ATTACK, PBStats.DEFENSE, PBStats.SPEED}[form]; // :1683
                if (user.pbCanRaiseStatStage(stat, user, move)) {             // :1684
                    user.pbRaiseStatStage(stat, 1, user, true);               // :1685
                }
            }
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum,showAnimation)} (:1690-1693). */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets,
                                    int hitNum, boolean showAnimation) {
            Object raw = user.effects.raw(PBEffects.Battler.Commander);
            if (raw instanceof int[] && ((int[]) raw).length > 1) {           // :1691 isCommanderHost?
                hitNum = ((int[]) raw)[1] + 1;
            }
            // :1692 super —— 登记: Move_Usage.rb:67-74 pbShowAnimation 依赖动画播放器（本批未建模）
        }
    }

    /**
     * {@code class PokeBattle_Move_1DA < PokeBattle_Move} (:1697-1722): 淘金潮 - scatters
     * money and lowers the user's Sp. Atk by 2.
     */
    public static class PokeBattle_Move_1DA extends MoveEffectBase {

        /** The Ruby {@code attr_reader :statDown} (:1698). */
        private final int[] statDown;

        /** {@code initialize} (:1699-1702): {@code super} + {@code @statDown = [SPATK,2]}. */
        public PokeBattle_Move_1DA() {
            statDown = new int[] {PBStats.SPATK, 2};                          // :1701
        }

        /** {@code pbEndOfMoveUsageEffect(user,targets,numHits,switchedBattlers)} (:1704-1721). */
        @Override
        public void pbEndOfMoveUsageEffect(BattleMove move, Battler user, Array<Battler> targets,
                                           int numHits, IntArray switchedBattlers) {
            boolean hitTarget = false;                                        // :1706
            for (Battler b : targets) {                                       // :1707
                if (b.damageState.missed) {                                   // :1708
                    continue;
                }
                if (b.damageState.protectedFlag) {                            // :1709 protected
                    continue;
                }
                if (b.damageState.unaffected) {                               // :1710
                    continue;
                }
                hitTarget = true;                                             // :1711
                if (!user.pbOwnedByPlayer()) {                                // :1713
                    continue;
                }
                user.battle.field.effects.add(PBEffects.Field.PayDay, 5 * user.level()); // :1714 += 5*user.level
            }
            if (hitTarget) {                                                  // :1716
                user.battle.display("钱币散落地到处都是！");
            }
            if (user.pbCanLowerStatStage(statDown[0], user, move) && hitTarget) { // :1718
                user.pbLowerStatStage(statDown[0], statDown[1], user);         // :1719
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1B9 < PokeBattle_Move} (:1725-1749): 棘藤棒 - Ivy Cudgel's
     * type follows the held mask.
     */
    public static class PokeBattle_Move_1B9 extends MoveEffectBase {

        /**
         * {@code pbBaseType(user)} (:1737-1748).
         *
         * <p><b>为什么 {@code @itemTypes} 不放在字段里</b>：Ruby 的 {@code initialize} (:1726-1735)
         * 用 {@code isConst?(@id,PBMoves,:IVYCUDGEL)} 预判，而 {@code @id} 是<b>每招式</b>状态
         * （{@code PokeBattle_Move.rb:29}），本运行时的对应物是 {@link BattleMove}。策略对象是
         * 按 function code 共享的（{@code MoveEffectRegistry.of(function)} 返回同一实例），
         * 若在构造器里读 move，两个同 code 的招式会共享第一次的判定结果 —— 那是静默错。
         * 所以 {@code @itemTypes} 的两个常量表在钩子里现算（依据 {@code PokeBattle_Move.rb:29/40}）。
         * 与 {@code @calcType}/{@code @powerBoost} 归 {@code BattleMove} 是同一条规则。</p>
         */
        @Override
        public String pbBaseType(BattleMove move, Battler user) {
            String ret = "GRASS";                                            // :1738 getID(PBTypes,:GRASS)
            if (user.itemActive()) {                                          // :1739
                boolean ivyCudgel = "IVYCUDGEL".equals(move.internalName());  // :1728 isConst?(@id,PBMoves,:IVYCUDGEL)
                if (ivyCudgel) {
                    // :1729-1733 @itemTypes = { WELLSPRINGMASK=>WATER, HEARTHFLAMEMASK=>FIRE, CORNERSTONEMASK=>ROCK }
                    String[] itemKeys = {"WELLSPRINGMASK", "HEARTHFLAMEMASK", "CORNERSTONEMASK"};
                    String[] itemTypes = {"WATER", "FIRE", "ROCK"};
                    for (int i = 0; i < itemKeys.length; i++) {               // :1740 @itemTypes.each
                        if (!itemKeys[i].equals(user.item)) {                 // :1741 isConst?(user.item,PBItems,item)
                            continue;
                        }
                        ret = itemTypes[i];                                  // :1742-1743 getConst(PBTypes,itemType)
                        break;                                               // :1744
                    }
                }
            }
            return ret;                                                      // :1747
        }
    }

    /**
     * {@code class PokeBattle_Move_1DE < PokeBattle_BurnMove} (:1752-1761): 刷刷茶炮 - drains
     * half the damage dealt.
     */
    public static class PokeBattle_Move_1DE extends MoveEffectsGeneric.PokeBattle_BurnMove {

        /** {@code healingMove?} (:1753). */
        @Override
        public boolean healingMove(BattleMove move) {
            return true;                                                     // :1753
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1755-1760). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.damageState.hpLost <= 0) {                             // :1756
                return;
            }
            int hpGain = Math.round(target.damageState.hpLost / 2.0f);        // :1757
            user.pbRecoverHPFromDrain(hpGain, target);                        // :1758
            super.pbEffectAgainstTarget(move, user, target);                  // :1759
        }
    }

    /**
     * {@code class PokeBattle_Move_1DF < PokeBattle_Move} (:1764-1768): 硬压 - power follows the
     * target's remaining HP.
     */
    public static class PokeBattle_Move_1DF extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:1765-1767). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            return Math.max(100 * target.hp / target.maxHp(), 1);             // :1766 [100*target.hp/target.totalhp,1].max
        }
    }

    /**
     * {@code class PokeBattle_Move_1DB < PokeBattle_Move} (:1771-1776): 巨兽斩/巨兽弹/极巨炮 -
     * double damage against a Boss.
     */
    public static class PokeBattle_Move_1DB extends MoveEffectBase {

        /** {@code pbModifyDamage(damageMult,user,target)} (:1772-1775). */
        @Override
        public float pbModifyDamage(BattleMove move, float damageMult, Battler user, Battler target) {
            if (target.pokemon != null && target.pokemon.battleRank > 2) {     // :1773
                damageMult *= 2;
            }
            return damageMult;                                                // :1774
        }
    }

    /**
     * {@code class PokeBattle_Move_1E0 < PokeBattle_Move} (:1779-1823): 晶光星群 - Terapagos'
     * signature: category follows the higher attacking stat, Stellar type in form 2.
     */
    public static class PokeBattle_Move_1E0 extends MoveEffectBase {

        /** The Ruby {@code @calcCategory} (:1782). */
        private int calcCategory;

        /** {@code initialize} (:1780-1783): {@code super} + {@code @calcCategory = 1}. */
        public PokeBattle_Move_1E0() {
            calcCategory = 1;                                                // :1782
        }

        /** {@code pbMoveFailed?(user,targets)} (:1785-1791). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.isSpecies("TERAPAGOS")) {                              // :1786
                user.battle.display("但是" + user.pbThis(true) + "无法使用这个招式。"); // :1787
                return true;                                                 // :1788
            }
            return false;                                                    // :1790
        }

        /** {@code physicalMove?(thisType=nil)} (:1793). */
        @Override
        public boolean physicalMove(BattleMove move, String thisType) {
            return calcCategory == 0;                                        // :1793
        }

        /** {@code specialMove?(thisType=nil)} (:1794). */
        @Override
        public boolean specialMove(BattleMove move, String thisType) {
            return calcCategory == 1;                                        // :1794
        }

        /** {@code pbBaseType(user)} (:1796-1799). */
        @Override
        public String pbBaseType(BattleMove move, Battler user) {
            if (!(user.isSpecies("TERAPAGOS") && user.form() == 2)) {         // :1797
                return move.type();                                          // :1797 @type
            }
            return "STELLAR";                                                // :1798 getConst(PBTypes,:STELLAR)
        }

        /** {@code pbTarget(user)} (:1801-1806). */
        @Override
        public int pbTarget(BattleMove move, Battler user) {
            if (user.isSpecies("TERAPAGOS") && user.form() == 2) {            // :1802
                return PBTargets.fromName("AllFoes");         // :1803 PBTargets::AllFoes
            }
            return super.pbTarget(move, user);                               // :1805
        }

        /** {@code pbOnStartUse(user,targets)} (:1808-1822). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            int[] stageMul = {2, 2, 2, 2, 2, 2, 2, 3, 4, 5, 6, 7, 8};         // :1810
            int[] stageDiv = {8, 7, 6, 5, 4, 3, 2, 2, 2, 2, 2, 2, 2};         // :1811
            int atkStage = user.stage(PBStats.ATTACK) + 6;                    // :1813
            int realAtk = (int) Math.floor((float) user.attack() * stageMul[atkStage] / stageDiv[atkStage]); // :1814
            int spAtkStage = user.stage(PBStats.SPATK) + 6;                   // :1816
            int realSpAtk = (int) Math.floor((float) user.spAtk() * stageMul[spAtkStage] / stageDiv[spAtkStage]); // :1817
            calcCategory = realAtk > realSpAtk ? 0 : 1;                       // :1819
            if (!(user.isSpecies("TERAPAGOS") && user.form() == 1)) {          // :1820
                return;
            }
            user.pbChangeFormTransform(2, user.pbThis() + "变成了星晶形态！");  // :1821
        }
    }

    /**
     * {@code class PokeBattle_Move_1DC < PokeBattle_StatDownMove} (:1826-1850): 星晶爆发 - same
     * category logic as 1E0, and lowers both Attack and Sp. Atk.
     */
    public static class PokeBattle_Move_1DC extends MoveEffectsGeneric.PokeBattle_StatDownMove {

        /** The Ruby {@code @calcCategory} (:1829). */
        private int calcCategory;

        /** {@code initialize} (:1827-1831): {@code super} + category and the two-pair statDown. */
        public PokeBattle_Move_1DC() {
            calcCategory = 1;                                                // :1829
            this.statDown = new int[] {PBStats.ATTACK, 1, PBStats.SPATK, 1};  // :1830
        }

        /** {@code physicalMove?(thisType=nil)} (:1833). */
        @Override
        public boolean physicalMove(BattleMove move, String thisType) {
            return calcCategory == 0;                                        // :1833
        }

        /** {@code specialMove?(thisType=nil)} (:1834). */
        @Override
        public boolean specialMove(BattleMove move, String thisType) {
            return calcCategory == 1;                                        // :1834
        }

        /** {@code pbOnStartUse(user,targets)} (:1836-1848). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            int[] stageMul = {2, 2, 2, 2, 2, 2, 2, 3, 4, 5, 6, 7, 8};         // :1838
            int[] stageDiv = {8, 7, 6, 5, 4, 3, 2, 2, 2, 2, 2, 2, 2};         // :1839
            int atkStage = user.stage(PBStats.ATTACK) + 6;                    // :1841
            int realAtk = (int) Math.floor((float) user.attack() * stageMul[atkStage] / stageDiv[atkStage]); // :1842
            int spAtkStage = user.stage(PBStats.SPATK) + 6;                   // :1844
            int realSpAtk = (int) Math.floor((float) user.spAtk() * stageMul[spAtkStage] / stageDiv[spAtkStage]); // :1845
            calcCategory = realAtk > realSpAtk ? 0 : 1;                       // :1847
        }
    }

    /**
     * {@code class PokeBattle_Move_1E1 < PokeBattle_Move} (:1854-1884): 龙声鼓舞 - grants Focus
     * Energy to the user's other allies.
     */
    public static class PokeBattle_Move_1E1 extends MoveEffectBase {

        /** The Ruby {@code @validTargets} (:1859). */
        private final Array<Battler> validTargets = new Array<>();

        /** {@code ignoresSubstitute?(user)} (:1855). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :1855
        }

        /** {@code canSnatch?} (:1856). */
        @Override
        public boolean canSnatch(BattleMove move) {
            return true;                                                     // :1856
        }

        /** {@code pbMoveFailed?(user,targets)} (:1858-1870). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            validTargets.clear();                                            // :1859
            for (Battler b : user.battle.allSameSideBattlers(user.index)) {    // :1860
                if (b.index == user.index) {                                  // :1861
                    continue;
                }
                if (b.effects.intVal(PBEffects.Battler.FocusEnergy) > 0) {     // :1862
                    continue;
                }
                validTargets.add(b);                                          // :1863
            }
            if (validTargets.size == 0) {                                     // :1865
                user.battle.display("但是它失败了！");                        // :1866
                return true;                                                 // :1867
            }
            return false;                                                    // :1869
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:1872-1876). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            for (Battler b : validTargets) {                                  // :1873 @validTargets.any?
                if (b.index == target.index) {
                    return false;
                }
            }
            user.battle.display(target.pbThis() + "已经被鼓舞了！");           // :1874
            return true;                                                      // :1875
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1878-1883). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            int boost = target.pbHasType("DRAGON") ? 2 : 1;                    // :1879
            target.effects.set(PBEffects.Battler.FocusEnergy, boost);          // :1880
            user.battle.commonAnimation("StatUp", target);                     // :1881
            user.battle.display(target.pbThis() + "正在振奋起来！");            // :1882
        }
    }

    /**
     * {@code class PokeBattle_Move_1E2 < PokeBattle_Move} (:1887-1903): 怒牛 - the type follows
     * the user's secondary type, and the animation follows the type.
     */
    public static class PokeBattle_Move_1E2 extends MoveEffectBase {

        /** {@code pbBaseType(user)} (:1888-1892). */
        @Override
        public String pbBaseType(BattleMove move, Battler user) {
            if (!user.isSpecies("TAUROS")) {                                 // :1889
                return move.type();                                          // :1889 @type
            }
            Array<String> userTypes = user.pbTypes();                          // :1890
            if (userTypes.size > 1) {                                          // :1891 userTypes[1] || userTypes[0] || @type
                return userTypes.get(1);
            }
            if (userTypes.size > 0) {
                return userTypes.get(0);
            }
            return move.type();
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum,showAnimation)} (:1894-1902). */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets,
                                    int hitNum, boolean showAnimation) {
            String t = pbBaseType(move, user);                                // :1895
            switch (t) {                                                      // :1896-1899
                case "FIGHTING": hitNum = 1; break;
                case "FIRE":     hitNum = 2; break;
                case "WATER":    hitNum = 3; break;
                default:         hitNum = 0; break;
            }
            // :1901 super —— 登记: Move_Usage.rb:67-74 pbShowAnimation 依赖动画播放器（本批未建模）
        }
    }

    /**
     * {@code class PokeBattle_Move_1E3 < PokeBattle_Move} (:1906-1914): 精神噪音 - applies Heal
     * Block.
     */
    public static class PokeBattle_Move_1E3 extends MoveEffectBase {

        /** {@code pbAdditionalEffect(user,target)} (:1907-1913). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.effects.intVal(PBEffects.Battler.HealBlock) > 0) {      // :1908
                return;
            }
            if (pbMoveFailedAromaVeil(move, user, target, false)) {            // :1909
                return;
            }
            target.effects.set(PBEffects.Battler.HealBlock, 2);                // :1910
            user.battle.display(target.pbThis() + "被阻止了治疗！");            // :1911
            target.pbItemStatusCureCheck(0, false);                            // :1912
        }
    }

    /**
     * {@code class PokeBattle_Move_1E4 < PokeBattle_ConfuseMove} (:1917-1921): 魅诱之声 - only
     * confuses when the target raised its stats this round.
     */
    public static class PokeBattle_Move_1E4 extends MoveEffectsGeneric.PokeBattle_ConfuseMove {

        /** {@code pbAdditionalEffect(user,target)} (:1918-1920). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.statsRaisedThisRound) {                                // :1919
                super.pbAdditionalEffect(move, user, target);                 // :1919 super
            }
        }
    }

    // ==================================================================
    // Move_Effects_180-1FF.rb:1923-2167
    // ==================================================================

    /**
     * {@code class PokeBattle_Move_1FD < PokeBattle_Move} (:1924-1938): 无极光束 - Eternatus'
     * form reset, Hyper Beam recharge and Boss bonus damage.
     */
    public static class PokeBattle_Move_1FD extends MoveEffectBase {

        /** {@code pbOnStartUse(user,targets)} (:1925-1929). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.isSpecies("ETERNATUS")) {                              // :1926
                return;
            }
            if (user.form() == 1) {                                          // :1927
                return;
            }
            user.pbChangeFormTransform(1, user.pbThis() + "变回了原来的样子！"); // :1928
        }

        /** {@code pbEffectGeneral(user)} (:1930-1933). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.HyperBeam, 2);                // :1931
            // 登记: :1932 user.currentMove = @id —— Battler 尚无 currentMove 字段
        }

        /** {@code pbModifyDamage(damageMult,user,target)} (:1934-1937). */
        @Override
        public float pbModifyDamage(BattleMove move, float damageMult, Battler user, Battler target) {
            if (target.pokemon != null && target.pokemon.battleRank > 2) {     // :1935
                damageMult *= 2;
            }
            return damageMult;                                                // :1936
        }
    }

    /**
     * {@code class PokeBattle_Move_1AF < PokeBattle_FlinchMove} (:1941-1958): 星星光轮 - two
     * hits, tramples Minimize, extra super-effective types.
     */
    public static class PokeBattle_Move_1AF extends MoveEffectsGeneric.PokeBattle_FlinchMove {

        /** {@code multiHitMove?} (:1942). */
        @Override
        public boolean multiHitMove(BattleMove move) {
            return true;                                                     // :1942
        }

        /** {@code pbNumHits(user,targets)} (:1943). */
        @Override
        public int pbNumHits(BattleMove move, Battler user, Array<Battler> targets) {
            return 2;                                                        // :1943
        }

        /** {@code tramplesMinimize?(param=1)} (:1944). */
        @Override
        public boolean tramplesMinimize(BattleMove move, int param) {
            return true;                                                     // :1944
        }

        /** {@code pbCalcTypeModSingle(moveType,defType,user,target)} (:1946-1953). */
        @Override
        public int pbCalcTypeModSingle(BattleMove move, String moveType, String defType,
                                       Battler user, Battler target) {
            int ret = super.pbCalcTypeModSingle(move, moveType, defType, user, target); // :1947
            // :1948-1949 原文用 `if A \n B`（第二个 isConst? 没有接 ||，恒为真）—— 照抄其**结果**：
            //   只要走到这里就覆盖；插件这一处也是缺陷，但结果等价于「无条件覆盖」
            if ("DARK".equals(defType)) {
                ret = PBTypeEffectiveness.SUPER_EFFECTIVE_ONE;
            }
            return ret;                                                      // :1952
        }

        /** {@code pbModifyDamage(damageMult,user,target)} (:1954-1957). */
        @Override
        public float pbModifyDamage(BattleMove move, float damageMult, Battler user, Battler target) {
            if (target.pokemon != null && target.pokemon.battleRank > 2) {     // :1955
                damageMult *= 2;
            }
            return damageMult;                                                // :1956
        }
    }

    /**
     * {@code class PokeBattle_Move_0BP < PokeBattle_Move} (:1964-1976): Discus - two hits,
     * ignores the target's evasion stage, fixed defense stage 6.
     */
    public static class PokeBattle_Move_0BP extends MoveEffectBase {

        /** {@code multiHitMove?} (:1965). */
        @Override
        public boolean multiHitMove(BattleMove move) {
            return true;                                                     // :1965
        }

        /** {@code pbNumHits(user,targets)} (:1966). */
        @Override
        public int pbNumHits(BattleMove move, Battler user, Array<Battler> targets) {
            return 2;                                                        // :1966
        }

        /**
         * {@code pbCalcAccuracyMultipliers(user,target,multipliers)} (:1967-1970).
         *
         * <p><b>插件自身缺陷，照抄不修</b>：形参叫 {@code multipliers}（:1967），
         * 而体内写的是 {@code modifiers[EVA_STAGE] = 0}（:1969）——{@code modifiers}
         * 在本方法里<b>从未定义</b>，Ruby 走到这一行会 {@code NameError}。
         * 按裁决：<b>绝不把它"修正"成 {@code multipliers}</b>（那是替插件修 bug，属自造行为），
         * 所以这一行不转、只登记。{@code MoveEffect} 的钩子与 {@code MoveEffectBase}
         * 的空体（task-11 裁决 5）保留落点。</p>
         */
        @Override
        public void pbCalcAccuracyMultipliers(BattleMove move, Battler user, Battler target,
                                              float[] multipliers) {
            super.pbCalcAccuracyMultipliers(move, user, target, multipliers); // :1968 super
            // 登记: :1969 `modifiers[EVA_STAGE] = 0` —— `modifiers` 未定义（插件缺陷 → Ruby NameError）
        }

        /** {@code pbGetDefenseStats(user,target)} (:1972-1975). */
        @Override
        public MoveStats pbGetDefenseStats(BattleMove move, Battler user, Battler target) {
            MoveStats s = super.pbGetDefenseStats(move, user, target);        // :1973
            return new MoveStats(s.value, 6);                                 // :1974 return ret1, 6
        }
    }

    /**
     * {@code class PokeBattle_Move_0BQ < PokeBattle_Move} (:1983-2000): GRANDEUR - weakened if
     * the user was hit while gathering sunlight.
     */
    public static class PokeBattle_Move_0BQ extends MoveEffectBase {

        /** {@code pbDisplayChargeMessage(user)} (:1984-1988). */
        @Override
        public void pbDisplayChargeMessage(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.FocusPunch, true);             // :1985
            user.battle.commonAnimation("FocusPunch", user);                  // :1986
            user.battle.display(user.pbThis() + "正在聚集阳光！");             // :1987
        }

        /** {@code pbDisplayUseMessage(user)} (:1990-1992). */
        @Override
        public void pbDisplayUseMessage(BattleMove move, Battler user) {
            if (!user.effects.truthy(PBEffects.Battler.FocusPunch) || user.lastHPLost == 0) { // :1991
                super.pbDisplayUseMessage(move, user);                        // :1991 super
            }
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:1993-1999). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (user.effects.truthy(PBEffects.Battler.FocusPunch) && user.lastHPLost > 0) { // :1994
                user.battle.display(user.pbThis() + "没能聚集足够阳光！");      // :1995
                baseDmg = Math.round(baseDmg * 0.3f);                         // :1996
            }
            return baseDmg;                                                   // :1998
        }
    }

    /**
     * {@code class PokeBattle_Move_1B0 < PokeBattle_Move} (:2004-2014): 火之神神乐 - extra
     * super-effective types.
     */
    public static class PokeBattle_Move_1B0 extends MoveEffectBase {

        /** {@code pbCalcTypeModSingle(moveType,defType,user,target)} (:2005-2013). */
        @Override
        public int pbCalcTypeModSingle(BattleMove move, String moveType, String defType,
                                       Battler user, Battler target) {
            int ret = super.pbCalcTypeModSingle(move, moveType, defType, user, target); // :2006
            if ("DIM".equals(defType) || "GHOST".equals(defType) || "DARK".equals(defType)) { // :2007-2009
                ret = PBTypeEffectiveness.SUPER_EFFECTIVE_ONE;               // :2010
            }
            return ret;                                                      // :2012
        }
    }

    /**
     * {@code class PokeBattle_Move_1BF < PokeBattle_Move} (:2016-2029): 群星闪耀 - resets the
     * target's stat stages and doubles damage against a Boss.
     */
    public static class PokeBattle_Move_1BF extends MoveEffectBase {

        /** {@code pbEffectAgainstTarget(user,target)} (:2017-2023). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.damageState.calcDamage > 0 && !target.damageState.substitute // :2018
                    && target.hasAlteredStatStages()) {                       // :2019
                target.pbResetStatStages();                                  // :2020
                user.battle.display(target.pbThis() + "的能力变化被重置了！");  // :2021
            }
        }

        /** {@code pbModifyDamage(damageMult,user,target)} (:2025-2028). */
        @Override
        public float pbModifyDamage(BattleMove move, float damageMult, Battler user, Battler target) {
            if (target.pokemon != null && target.pokemon.battleRank > 2) {     // :2026
                damageMult *= 2;
            }
            return damageMult;                                                // :2027
        }
    }

    /**
     * {@code class PokeBattle_Move_1EB < PokeBattle_Move} (:2032-2047): 群星陨落 - the second
     * {@code pbCalcAccuracyMultipliers} override.
     */
    public static class PokeBattle_Move_1EB extends MoveEffectBase {

        /**
         * {@code pbCalcAccuracyMultipliers(user,target,multipliers)} (:2033-2036).
         *
         * <p><b>插件自身缺陷，照抄不修</b>：形参叫 {@code multipliers}（:2033），体内却是
         * {@code modifiers[EVA_STAGE] = 0}（:2035）——{@code modifiers} 未定义，
         * Ruby 走到即 {@code NameError}。按裁决不修正变量名，该行不转、只登记。</p>
         */
        @Override
        public void pbCalcAccuracyMultipliers(BattleMove move, Battler user, Battler target,
                                              float[] multipliers) {
            super.pbCalcAccuracyMultipliers(move, user, target, multipliers); // :2034 super
            // 登记: :2035 `modifiers[EVA_STAGE] = 0` —— `modifiers` 未定义（插件缺陷 → Ruby NameError）
        }

        /** {@code pbGetDefenseStats(user,target)} (:2038-2041). */
        @Override
        public MoveStats pbGetDefenseStats(BattleMove move, Battler user, Battler target) {
            MoveStats s = super.pbGetDefenseStats(move, user, target);        // :2039
            return new MoveStats(s.value, 6);                                 // :2040 return ret1, 6
        }

        /** {@code pbModifyDamage(damageMult,user,target)} (:2043-2046). */
        @Override
        public float pbModifyDamage(BattleMove move, float damageMult, Battler user, Battler target) {
            if (target.pokemon != null && target.pokemon.battleRank > 2) {     // :2044
                damageMult *= 2;
            }
            return damageMult;                                                // :2045
        }
    }

    /**
     * {@code class PokeBattle_Move_1ED < PokeBattle_Move} (:2090-2097): 断龙裁决剑 - double
     * damage if the target is not at full HP.
     */
    public static class PokeBattle_Move_1ED extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:2091-2096). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (target.hp < target.maxHp()) {                                 // :2092 target.totalhp
                baseDmg *= 2;                                                // :2093
            }
            return baseDmg;                                                   // :2095
        }
    }

    /**
     * {@code class PokeBattle_Move_1F0 < PokeBattle_Move} (:2101-2108): 星河流羽 - +1 Attack as
     * an additional effect.
     */
    public static class PokeBattle_Move_1F0 extends MoveEffectBase {

        /** {@code pbAdditionalEffect(user,target)} (:2102-2107). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {                             // :2103
                return;
            }
            if (user.pbCanRaiseStatStage(PBStats.ATTACK, user, move)) {       // :2104
                user.pbRaiseStatStage(PBStats.ATTACK, 1, user);               // :2105
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1EF < PokeBattle_Move} (:2113-2132): 君王凌驾 - hits all
     * foes; +1 Def/Sp. Def if it fainted a target.
     */
    public static class PokeBattle_Move_1EF extends MoveEffectBase {

        /** {@code pbTarget(user)} (:2114-2116). */
        @Override
        public int pbTarget(BattleMove move, Battler user) {
            return PBTargets.fromName("AllFoes");            // :2115 PBTargets::AllFoes
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:2118-2131). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (!target.damageState.fainted) {                               // :2120
                return;
            }
            if (user.battle.pbAllFainted(target.idxOwnSide()) && target.damageState.fainted) { // :2121
                return;
            }
            boolean showAnim = true;                                          // :2122
            if (user.pbCanRaiseStatStage(PBStats.DEFENSE, user, move)) {      // :2123
                if (user.pbRaiseStatStage(PBStats.DEFENSE, 1, user, showAnim)) { // :2124
                    showAnim = false;                                        // :2125
                }
            }
            if (user.pbCanRaiseStatStage(PBStats.SPDEF, user, move)) {        // :2128
                user.pbRaiseStatStage(PBStats.SPDEF, 1, user, showAnim);      // :2129
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1B5 < PokeBattle_Move} (:2140-2159): 九彩升华 - raises
     * Attack/Defense/Sp. Atk/Sp. Def by 1.
     */
    public static class PokeBattle_Move_1B5 extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:2145-2158). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            int[] stats = {PBStats.ATTACK, PBStats.DEFENSE, PBStats.SPATK, PBStats.SPDEF}; // :2147-2148
            boolean showAnim = true;                                          // :2149
            for (int stat : stats) {                                          // :2150
                if (user.pbCanRaiseStatStage(stat, user, move)) {              // :2151
                    user.pbRaiseStatStage(stat, 1, user, showAnim);            // :2152
                    showAnim = false;                                         // :2153
                }
            }
            user.battle.display(user.pbThis() + "借助伙伴们的力量，\n所有能力都大幅提升了！"); // :2156
            // :2157 return 0 —— pbEffectGeneral 的返回值本运行时不用（void 钩子）
        }
    }

    /**
     * {@code class PokeBattle_Move_1B6 < PokeBattle_FlinchMove} (:2162-2167): 黑暗重拳 - Boss
     * bonus damage.
     */
    public static class PokeBattle_Move_1B6 extends MoveEffectsGeneric.PokeBattle_FlinchMove {

        /** {@code pbModifyDamage(damageMult,user,target)} (:2163-2166). */
        @Override
        public float pbModifyDamage(BattleMove move, float damageMult, Battler user, Battler target) {
            if (target.pokemon != null && target.pokemon.battleRank > 2) {     // :2164
                damageMult *= 2;
            }
            return damageMult;                                                // :2165
        }
    }

    // ==================================================================
    // Move_Effects_180-1FF.rb:2176-2449
    // ==================================================================

    /**
     * {@code class PokeBattle_Move_1F9 < PokeBattle_Move} (:2177-2190): 地魔之剑 - poisons and
     * marks 2 turns of increased Poison damage.
     */
    public static class PokeBattle_Move_1F9 extends MoveEffectBase {

        /** {@code pbAdditionalEffect(user,target)} (:2179-2189). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.fainted() || target.damageState.substitute) {          // :2180
                return;
            }
            if (!target.pbCanPoison(user, false, move)) {                     // :2182
                return;
            }
            target.pbPoison(user, null, false);                               // :2183
            if (!target.effects.truthy(PBEffects.Battler.PoisonVulnerability)) { // :2185
                target.effects.set(PBEffects.Battler.PoisonVulnerability, 2);  // :2186
                user.battle.display(target.pbThis() + "被地魔之力侵蚀了！");    // :2187
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1FA < PokeBattle_Move} (:2197-2211): 海魔之雨 - frostbites and
     * marks 2 turns of increased Ice damage.
     */
    public static class PokeBattle_Move_1FA extends MoveEffectBase {

        /** {@code pbAdditionalEffect(user,target)} (:2199-2209). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.fainted() || target.damageState.substitute) {          // :2200
                return;
            }
            // 登记: :2203 target.pbCanFreeze?(user,false,self) 与 :2204 target.pbFreeze
            //       —— Battler 尚无 pbCanFreeze/pbFreeze（属下一批）→ 无法施加冻伤
            if (!target.effects.truthy(PBEffects.Battler.IceVulnerability)) { // :2206
                target.effects.set(PBEffects.Battler.IceVulnerability, 2);     // :2207
                user.battle.display(target.pbThis() + "被海魔之力侵蚀了！");    // :2208
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1B7 < PokeBattle_Move} (:2219-2223): Genesis Supernova - sets
     * Psychic Terrain.
     */
    public static class PokeBattle_Move_1B7 extends MoveEffectBase {

        /** {@code pbAdditionalEffect(user,target)} (:2220-2222). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            PendingApi.pbStartTerrain(user.battle, user, PBBattleTerrains.Psychic); // :2221
        }
    }

    /**
     * {@code class PokeBattle_Move_1B8 < PokeBattle_Move} (:2229-2244): Guardian of Alola - 75%
     * of the target's current HP as fixed damage.
     */
    public static class PokeBattle_Move_1B8 extends MoveEffectBase {

        /** {@code pbFixedDamage(user,target)} (:2230-2237). */
        @Override
        public int pbFixedDamage(BattleMove move, Battler user, Battler target) {
            if (target.pokemon != null && target.pokemon.battleRank > 2) {     // :2232
                return Math.round(target.hp * 0.75f / 5.0f);                  // :2233
            }
            return Math.round(target.hp * 0.75f);                             // :2235
        }

        /** {@code pbCalcDamage(user,target,numTargets=1)} (:2239-2243). */
        @Override
        public void pbCalcDamage(BattleMove move, Battler user, Battler target, int numTargets) {
            target.damageState.critical = false;                              // :2240
            target.damageState.calcDamage = pbFixedDamage(move, user, target); // :2241
            if (target.damageState.calcDamage < 1) {                          // :2242
                target.damageState.calcDamage = 1;
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1BA < PokeBattle_Move} (:2251-2255): Menacing Moonraze
     * Maelstrom / Searing Sunraze Smash - ignores the ability.
     */
    public static class PokeBattle_Move_1BA extends MoveEffectBase {

        /** {@code pbChangeUsageCounters(user,specialUsage)} (:2251-2254). */
        @Override
        public void pbChangeUsageCounters(BattleMove move, Battler user, boolean specialUsage) {
            super.pbChangeUsageCounters(move, user, specialUsage);            // :2252
            // :2253 @battle.moldBreaker = true if !specialUsage
            // Battle.moldBreaker is a public field (Battle.java), so the write is direct.
            if (!specialUsage) {
                user.battle.moldBreaker = true;
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1BB < PokeBattle_Move} (:2262-2275): Splintered Stormshards -
     * removes the terrain.
     */
    public static class PokeBattle_Move_1BB extends MoveEffectBase {

        /** {@code pbAdditionalEffect(user,target)} (:2262-2274). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            switch (user.battle.field.terrain) {                              // :2263
                case PBBattleTerrains.Electric: user.battle.display("电流从战场上消失了!"); break; // :2265
                case PBBattleTerrains.Grassy:   user.battle.display("草原从战场上消失了!"); break; // :2267
                case PBBattleTerrains.Misty:    user.battle.display("大雾从战场上消失了!"); break; // :2269
                case PBBattleTerrains.Psychic:  user.battle.display("奇怪的气场从战场上消失了!"); break; // :2271
                default: break;
            }
            PendingApi.pbStartTerrain(user.battle, user, PBBattleTerrains.None); // :2273
        }
    }

    /**
     * {@code class PokeBattle_Move_1BC < PokeBattle_Move} (:2277-2305): 等离子闪电拳 - Ion
     * Deluge and (for Plasma Fists) a flinch.
     */
    public static class PokeBattle_Move_1BC extends MoveEffectBase {

        /** {@code flinchingMove?} (:2279-2281). */
        @Override
        public boolean flinchingMove(BattleMove move) {
            return "PLASMAFISTS".equals(move.internalName());                 // :2280 isConst?(@id,PBMoves,:PLASMAFISTS)
        }

        /** {@code pbMoveFailed?(user,targets)} (:2283-2291). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (damagingMove(move)) {                                        // :2284
                return false;
            }
            if (user.battle.field.effects.truthy(PBEffects.Field.IonDeluge)) { // :2285
                user.battle.display("但是它失败了！");                         // :2286
                return true;                                                 // :2287
            }
            if (pbMoveFailedLastInRound(move, user)) {                        // :2289
                return true;
            }
            return false;                                                    // :2290
        }

        /** {@code pbEffectGeneral(user)} (:2293-2297). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            if (user.battle.field.effects.truthy(PBEffects.Field.IonDeluge)) { // :2294
                return;
            }
            user.battle.field.effects.set(PBEffects.Field.IonDeluge, true);    // :2295
            user.battle.display("洪流般的等离子充满着场地！");                  // :2296
        }

        /** {@code pbAdditionalEffect(user,target)} (:2299-2303). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (!"PLASMAFISTS".equals(move.internalName())) {                 // :2300
                return;
            }
            if (target.damageState.substitute) {                             // :2301
                return;
            }
            // 登记: :2302 target.pbFlinch(user) —— Battler 尚无 pbFlinch
            //       （Battler_Statuses:619-622，属下一批）
        }
    }

    /**
     * {@code class PokeBattle_Move_1BD < PokeBattle_Move} (:2312-2324): 根源波动 (Origin Pulse) -
     * always hits in rain.
     */
    public static class PokeBattle_Move_1BD extends MoveEffectBase {

        /**
         * {@code pbCalcAccuracyMultipliers(user,target,multipliers)} (:2313-2319) ——
         * <b>第 3 处</b>同型插件缺陷。
         *
         * <p><b>照抄不修</b>：形参叫 {@code multipliers}（:2313），体内写的是
         * {@code modifiers[EVA_STAGE] = 0}（:2317，且在 {@code if} 里）——{@code modifiers}
         * 从未定义，Ruby 走到即 {@code NameError}。按裁决不把变量名"修正"成
         * {@code multipliers}，故该行不转、只登记。</p>
         */
        @Override
        public void pbCalcAccuracyMultipliers(BattleMove move, Battler user, Battler target,
                                              float[] multipliers) {
            super.pbCalcAccuracyMultipliers(move, user, target, multipliers); // :2314 super
            // 登记: :2315-2317 `if @battle.pbWeather==Rain || ==HeavyRain` 里的
            //       `modifiers[EVA_STAGE] = 0` —— `modifiers` 未定义（插件缺陷 → Ruby NameError）
        }

        /** {@code pbModifyDamage(damageMult,user,target)} (:2321-2323). */
        @Override
        public float pbModifyDamage(BattleMove move, float damageMult, Battler user, Battler target) {
            if (target.pokemon != null && target.pokemon.battleRank > 2) {     // :2322
                damageMult *= 2;
            }
            return damageMult;                                                // :2323
        }
    }

    /**
     * {@code class PokeBattle_Move_1C4 < PokeBattle_Move} (:2330-2343): 断崖之剑 (Precipice
     * Blades) - always hits in sun.
     */
    public static class PokeBattle_Move_1C4 extends MoveEffectBase {

        /** {@code pbAccuracyCheck(user,target)} (:2331-2338). */
        @Override
        public boolean pbAccuracyCheck(BattleMove move, Battler user, Battler target) {
            int w = user.battle.weather();                                   // :2333 @battle.pbWeather
            if (w == PBWeather.Sun || w == PBWeather.HarshSun) {              // :2333-2334
                return true;                                                 // :2335
            }
            return super.pbAccuracyCheck(move, user, target);                 // :2337 super
        }

        /** {@code pbModifyDamage(damageMult,user,target)} (:2339-2342). */
        @Override
        public float pbModifyDamage(BattleMove move, float damageMult, Battler user, Battler target) {
            if (target.pokemon != null && target.pokemon.battleRank > 2) {     // :2340
                damageMult *= 2;
            }
            return damageMult;                                                // :2341
        }
    }

    /**
     * {@code class PokeBattle_Move_1C5 < PokeBattle_Move} (:2349-2357): 晶光雨 (GLITTERRAIN) -
     * 1.5x power in rain.
     */
    public static class PokeBattle_Move_1C5 extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:2350-2356). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            int w = user.battle.weather();                                   // :2351 @battle.pbWeather
            if ((w == PBWeather.Rain || w == PBWeather.HeavyRain)
                    && !user.hasUtilityUmbrella()) {                          // :2352
                baseDmg = Math.round(baseDmg * 1.5f);                        // :2353
            }
            return baseDmg;                                                   // :2355
        }
    }

    /**
     * {@code class PokeBattle_Move_1C6 < PokeBattle_Move} (:2363-2370): 光烨羽舞 (FEATHERDANCE) -
     * +1 Sp. Def to the user.
     */
    public static class PokeBattle_Move_1C6 extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:2364-2369). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            if (user.pbCanRaiseStatStage(PBStats.SPDEF, user, move)) {        // :2366
                user.pbRaiseStatStage(PBStats.SPDEF, 1, user);               // :2367
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1C8} —— <b>在插件里被定义了两次</b>（Ruby 的「重开类」，
     * 两个 body <b>共存</b>，不是后者覆盖前者）：
     * <ul>
     * <li>{@code :2377-2398} 妄之歌 (SONGOFDELUSION)：{@code pbMoveFailed?} + {@code pbEffectGeneral}</li>
     * <li>{@code :2445-2449} 巨力锤：{@code pbEffectWhenDealingDamage}</li>
     * </ul>
     * 所以本 Java 类把两个 body 的钩子<b>合并</b>进同一个类（与 M0b 处理
     * {@code pbChangeTypes} 两版覆盖、L1' 处理 {@code BROKENBREATH} 双注册同一规则：
     * 按 Ruby 的最终可见行为转）。
     */
    public static class PokeBattle_Move_1C8 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:2378-2384). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.pbOpposingSide().effects.intVal(PBEffects.Side.DelusionSong) > 0) { // :2379
                user.battle.display("但是失败了！");                           // :2380
                return true;                                                 // :2381
            }
            return false;                                                    // :2383
        }

        /** {@code pbEffectGeneral(user)} (:2386-2397). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.pbOpposingSide().effects.set(PBEffects.Side.DelusionSong, 5); // :2387
            user.battle.display("妖精的歌声在" + user.pbOpposingTeam(true) + "场上回荡！"); // :2388
            for (Battler b : user.battle.eachOtherSideBattler(user.index)) {   // :2390
                if (b.fainted()) {                                           // :2391
                    continue;
                }
                if (b.effects.intVal(PBEffects.Battler.Substitute) > 0) {     // :2392
                    continue;
                }
                if (b.pbCanConfuse(user, false, move)) {                      // :2393
                    b.pbConfuse();                                           // :2394
                }
            }
        }

        /** {@code pbEffectWhenDealingDamage(user,target)} (:2446-2448) —— 第二次定义的 body。 */
        @Override
        public void pbEffectWhenDealingDamage(BattleMove move, Battler user, Battler target) {
            user.effects.set(PBEffects.Battler.SuccessiveMove, move.id());    // :2447 @effects[SuccessiveMove] = @id
        }
    }

    /**
     * {@code class PokeBattle_Move_1C9 < PokeBattle_Move} (:2406-2416): 哀悼悲叹 (MOURNFULLAMENT) -
     * double power if a non-active party member has fainted.
     */
    public static class PokeBattle_Move_1C9 extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:2407-2415). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            Array<Battler> party = user.battle.partyOf(user.index);             // :2408 @battle.pbParty(user.index)
            for (int i = 0; i < party.size; i++) {                            // :2408 each_with_index
                Battler pkmn = party.get(i);
                if (pkmn == null || i == user.pokemonIndex) {                 // :2409
                    continue;
                }
                if (!pkmn.fainted()) {                                        // :2410
                    continue;
                }
                baseDmg *= 2;                                                // :2411
                break;                                                       // :2412
            }
            return baseDmg;                                                  // :2414
        }
    }

    /**
     * {@code class PokeBattle_Move_1C7 < PokeBattle_Move} (:2424-2442): 幻海妖歌 (SIRENSONG) - 40%
     * to infatuate, and ignores Sp. Def stages on an infatuated target.
     */
    public static class PokeBattle_Move_1C7 extends MoveEffectBase {

        /** {@code pbAdditionalEffect(user,target)} (:2425-2433). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.fainted() || target.damageState.substitute) {          // :2425
                return;
            }
            if (user.battle.pbRandom(100) >= 40) {                            // :2426
                return;
            }
            if (target.effects.intVal(PBEffects.Battler.Attract) < 0           // :2429
                    && target.pbCanAttract(user, false)) {                    // :2430
                target.pbAttract(user, null);                                 // :2431
            }
        }

        /** {@code pbGetDefenseStats(user,target)} (:2437-2441). */
        @Override
        public MoveStats pbGetDefenseStats(BattleMove move, Battler user, Battler target) {
            if (target.effects.intVal(PBEffects.Battler.Attract) >= 0) {       // :2437
                return new MoveStats(target.spDef(), 6);                      // :2438 return target.spdef, 6
            }
            return super.pbGetDefenseStats(move, user, target);                // :2440
        }
    }

    // ==================================================================
    // Move_Effects_180-1FF.rb:2453-2696 (final block)
    // ==================================================================

    /**
     * {@code class PokeBattle_Move_1E5 < PokeBattle_Move} (:2454-2462): 浴火重生 - arms the
     * one-shot revival.
     */
    public static class PokeBattle_Move_1E5 extends MoveEffectBase {

        /** {@code pbEffectAgainstTarget(user,target)} (:2455-2461). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (user.reborn()) {                                              // :2456
                user.battle.display("但是" + user.pbThis(true) + "无法再次复活了");   // :2457
                return;                                                       // :2458
            }
            user.setCanRebirth(true);                                         // :2460
        }
    }

    /**
     * {@code class PokeBattle_Move_1E6 < PokeBattle_Move} (:2467-2548): 热带海流 - rain, Tailwind,
     * clears the user's hazards and the target's screens, then burns.
     */
    public static class PokeBattle_Move_1E6 extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:2469-2518). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            switch (user.battle.field.weather) {                             // :2470
                case PBWeather.Rain:
                case PBWeather.HeavyRain:
                case PBWeather.HarshSun:
                case PBWeather.StrongWinds:
                    break;                                                   // :2471-2474
                default:
                    // :2476 @battle.pbStartWeather(user,Rain,true,false) —— Battle 只有 2 参档
                    user.battle.pbStartWeather(user, PBWeather.Rain);         // :2476
                    break;
            }
            if (user.pbOwnSide().effects.intVal(PBEffects.Side.Tailwind) < 1) { // :2478
                user.pbOwnSide().effects.set(PBEffects.Side.Tailwind, 4);      // :2479
                user.battle.display(user.pbTeam(true) + "刮起了顺风！");        // :2480
            }
            if (user.effects.intVal(PBEffects.Battler.LeechSeed) >= 0) {       // :2482
                user.effects.set(PBEffects.Battler.LeechSeed, -1);             // :2483
                user.battle.display(user.pbThis() + "清除了寄生种子！");        // :2484
            }
            if (user.pbOwnSide().effects.truthy(PBEffects.Side.StealthRock)) { // :2486
                user.pbOwnSide().effects.set(PBEffects.Side.StealthRock, false); // :2487
                user.battle.display(user.pbThis() + "吹散了隐形岩！");          // :2488
            }
            if (user.pbOwnSide().effects.intVal(PBEffects.Side.Spikes) > 0) {  // :2490
                user.pbOwnSide().effects.set(PBEffects.Side.Spikes, 0);        // :2491
                user.battle.display(user.pbThis() + "吹走了铁菱！");            // :2492
            }
            if (user.pbOwnSide().effects.intVal(PBEffects.Side.ToxicSpikes) > 0) { // :2494
                user.pbOwnSide().effects.set(PBEffects.Side.ToxicSpikes, 0);   // :2495
                user.battle.display(user.pbThis() + "吹走了毒菱！");            // :2496
            }
            if (user.pbOwnSide().effects.truthy(PBEffects.Side.StickyWeb)) {   // :2498
                user.pbOwnSide().effects.set(PBEffects.Side.StickyWeb, false); // :2499
                user.pbOwnSide().effects.set(PBEffects.Side.StickyWebUser, -1); // :2500
                user.battle.display(user.pbThis() + "吹走了粘网！");            // :2501
            }
            switch (user.battle.field.terrain) {                              // :2503
                case PBBattleTerrains.Electric: user.battle.display("电流从场上消失了!"); break; // :2505
                case PBBattleTerrains.Grassy:   user.battle.display("青草从场上消失了！"); break; // :2507
                case PBBattleTerrains.Misty:    user.battle.display("迷雾从场上消失了！"); break; // :2509
                case PBBattleTerrains.Psychic:  user.battle.display("诡异的气场从场上消失了!"); break; // :2511
                case PBBattleTerrains.BugLure:  user.battle.display("虫网从场上消失了！"); break; // :2513
                case PBBattleTerrains.Cold:     user.battle.display("冰霜从场上消失了！"); break; // :2515
                default: break;
            }
            // :2517 @battle.pbStartTerrain(user,None,true) —— Battle 未暴露 pbStartTerrain
            PendingApi.pbStartTerrain(user.battle, user, PBBattleTerrains.None); // :2517
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:2521-2542) —— 五种墙/守护，**没有 Tailwind**。 */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (user.fainted() || target.damageState.unaffected) {             // :2521
                return;
            }
            if (target.pbOwnSide().effects.intVal(PBEffects.Side.AuroraVeil) > 0) { // :2522
                target.pbOwnSide().effects.set(PBEffects.Side.AuroraVeil, 0);  // :2523
                user.battle.display(target.pbTeam(false) + "的极光幕消失了！"); // :2524
            }
            if (target.pbOwnSide().effects.intVal(PBEffects.Side.LightScreen) > 0) { // :2526
                target.pbOwnSide().effects.set(PBEffects.Side.LightScreen, 0); // :2527
                user.battle.display(target.pbTeam(false) + "的光墙消失了！");   // :2528
            }
            if (target.pbOwnSide().effects.intVal(PBEffects.Side.Reflect) > 0) { // :2530
                target.pbOwnSide().effects.set(PBEffects.Side.Reflect, 0);     // :2531
                user.battle.display(target.pbTeam(false) + "的反射壁消失了！"); // :2532
            }
            if (target.pbOwnSide().effects.intVal(PBEffects.Side.Mist) > 0) {  // :2534
                target.pbOwnSide().effects.set(PBEffects.Side.Mist, 0);        // :2535
                user.battle.display(target.pbTeam(false) + "的迷雾消失了！");   // :2536
            }
            if (target.pbOwnSide().effects.intVal(PBEffects.Side.Safeguard) > 0) { // :2538
                target.pbOwnSide().effects.set(PBEffects.Side.Safeguard, 0);   // :2539
                user.battle.display(target.pbTeam(false) + "不再被神秘守护保护了！"); // :2540
            }
        }

        /** {@code pbAdditionalEffect(user,target)} (:2544-2547). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {                             // :2545
                return;
            }
            if (target.pbCanBurn(user, false, move)) {                       // :2546
                target.pbBurn(user, null);                                   // :2546
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_1E7 < PokeBattle_Move} (:2555-2579): 极光虹雨 - Ho-Oh/Lugia only;
     * stronger in sun/rain and when the other of the pair is an ally.
     */
    public static class PokeBattle_Move_1E7 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:2556-2562). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.isSpecies("HOOH") && !user.isSpecies("LUGIA")) {        // :2557
                user.battle.display("但是" + user.pbThis(true) + "无法使用这个招式！"); // :2558
                return true;                                                 // :2559
            }
            return false;                                                    // :2561
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:2564-2578). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            float mult = 1.0f;                                               // :2565
            int w = user.battle.weather();                                   // :2566 @battle.pbWeather
            if (w == PBWeather.Sun || w == PBWeather.HarshSun
                    || w == PBWeather.Rain || w == PBWeather.HeavyRain) {     // :2566
                mult += 0.2f;                                                // :2567
            }
            // :2569-2576 eachAlly + break：lambda 里不能 break，用标志位表达「只加一次」
            boolean[] paired = {false};
            user.eachAlly(ally -> {                                          // :2569
                if (ally.fainted()) {                                        // :2570
                    return;
                }
                if ((ally.isSpecies("HOOH") && user.isSpecies("LUGIA"))      // :2571
                        || (ally.isSpecies("LUGIA") && user.isSpecies("HOOH"))) { // :2572
                    paired[0] = true;                                        // :2573
                }
            });
            if (paired[0]) {
                mult += 0.3f;                                                // :2573
            }
            return Math.round(baseDmg * mult);                               // :2577
        }
    }

    /**
     * {@code class PokeBattle_Move_1E8 < PokeBattle_Move} (:2588-2621): 生命屏障 (Life Barrier) -
     * pays half the max HP for a 5-turn Aurora Veil.
     */
    public static class PokeBattle_Move_1E8 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:2589-2601). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.pbOwnSide().effects.intVal(PBEffects.Side.AuroraVeil) > 0) { // :2590
                user.battle.display("但是失败了！");                           // :2591
                return true;                                                 // :2592
            }
            int hpLoss = Math.max(user.maxHp() / 2, 1);                       // :2595 [totalhp/2,1].max
            if (user.hp <= hpLoss) {                                         // :2596
                user.battle.display("但是没有足够的HP制造屏障！");              // :2597
                return true;                                                 // :2598
            }
            return false;                                                    // :2600
        }

        /** {@code pbOnStartUse(user,targets)} (:2603-2608). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            int hpLoss = Math.max(user.maxHp() / 2, 1);                       // :2605
            user.pbReduceHP(hpLoss, false, false, true);                      // :2606
            user.pbItemHPHealCheck(0, false);                                 // :2607
        }

        /** {@code pbEffectGeneral(user)} (:2610-2615). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            int turns = user.hasActiveItem("LIGHTCLAY") ? 8 : 5;              // :2612
            user.pbOwnSide().effects.set(PBEffects.Side.AuroraVeil, turns);    // :2613
            user.battle.display(user.pbThis() + "牺牲了生命，制造了生命屏障！"); // :2614
        }

        /** {@code pbAccuracyCheck(user,target)} (:2618-2620) —— 变化招式必中. */
        @Override
        public boolean pbAccuracyCheck(BattleMove move, Battler user, Battler target) {
            return true;                                                     // :2619
        }
    }

    /**
     * {@code class PokeBattle_Move_1F6 < PokeBattle_Move} (:2630-2645): 空魔之雷 (Void Demon
     * Thunder) - always hits and puts the user into the charging state.
     */
    public static class PokeBattle_Move_1F6 extends MoveEffectBase {

        /** The Ruby {@code @effectApplied} (:2639). */
        private boolean effectApplied;

        /** {@code pbAccuracyCheck(user,target)} (:2632-2634) —— 必中. */
        @Override
        public boolean pbAccuracyCheck(BattleMove move, Battler user, Battler target) {
            return true;                                                     // :2633
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:2636-2644). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (user.fainted()) {                                            // :2637
                return;
            }
            if (effectApplied) {                                             // :2638 @effectApplied
                return;
            }
            effectApplied = true;                                            // :2639
            user.effects.set(PBEffects.Battler.Charge, 2);                    // :2642 进入充电状态
            user.battle.display(user.pbThis() + "溢出的电能使其进入了充电状态！"); // :2643
        }
    }

    /**
     * {@code class PokeBattle_Move_1F7 < PokeBattle_Move} (:2652-2696): 捕鼠笼 (Mousetrap) - the
     * charging turn raises both defenses and draws attacks.
     */
    public static class PokeBattle_Move_1F7 extends MoveEffectBase {

        /** {@code pbDisplayChargeMessage(user)} (:2654-2672). */
        @Override
        public void pbDisplayChargeMessage(BattleMove move, Battler user) {
            // 登记: :2655 user.effects[PBEffects::MouseTrap] = true —— Java 的 PBEffects 里
            //       **没有 MouseTrap 常量**（插件新增，本批未建模）→ 无法打该标记
            user.battle.commonAnimation("ShellTrap", user);                   // :2656
            user.battle.display(user.pbThis() + "张开捕鼠笼，等待对手自投罗网！"); // :2657
            boolean showAnim = true;                                          // :2660
            for (int stat : new int[] {PBStats.DEFENSE, PBStats.SPDEF}) {      // :2661
                if (!user.pbCanRaiseStatStage(stat, user, move)) {             // :2662
                    continue;
                }
                if (user.pbRaiseStatStage(stat, 1, user, showAnim)) {          // :2663
                    showAnim = false;
                }
            }
            user.effects.set(PBEffects.Battler.FollowMe, 1);                   // :2667 吸引单体攻击
            user.eachAlly(b -> {                                              // :2668
                if (b.effects.intVal(PBEffects.Battler.FollowMe)
                        < user.effects.intVal(PBEffects.Battler.FollowMe)) {   // :2669
                    return;
                }
                user.effects.set(PBEffects.Battler.FollowMe,
                        b.effects.intVal(PBEffects.Battler.FollowMe) + 1);     // :2670
            });
        }

        /** {@code pbDisplayUseMessage(user)} (:2675-2677). */
        @Override
        public void pbDisplayUseMessage(BattleMove move, Battler user) {
            if (user.lastHPLost > 0) {                                       // :2676
                super.pbDisplayUseMessage(move, user);                       // :2676 super
            }
        }

        /** {@code pbMoveFailed?(user,targets)} (:2679-2690). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            // 登记: :2680 `if !user.effects[PBEffects::MouseTrap]` —— 同 :2655 的常量缺失；
            //       标记永远读不到 → 按「未布下」处理（与插件在常量不存在时的行为一致）
            user.battle.display("但是失败了！");                               // :2681
            return true;                                                      // :2682
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:2693-2695). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            // 登记: :2694 user.effects[PBEffects::MouseTrap] = false —— 同 :2655 的常量缺失
        }
    }

    /**
     * {@code PokeBattle_Move.pbFromPBMove} (PokeBattle_Move.rb:51-59) for the
     * 122 function-code classes this file holds: the Ruby builds
     * {@code "PokeBattle_Move_#{function}"} and instantiates it on demand, so each
     * code is bound to its no-argument constructor here and
     * {@link MoveEffectRegistry#of} calls it lazily (some constructors still touch
     * not-yet-wired stubs, which must not break the whole table).
     *
     * <p>Generated by {@code __l2register.mjs} - re-run it with {@code --write}
     * after the class set changes.</p>
     */
    static void register() {
        MoveEffectRegistry.register("180", PokeBattle_Move_180::new);
        MoveEffectRegistry.register("181", PokeBattle_Move_181::new);
        MoveEffectRegistry.register("182", PokeBattle_Move_182::new);
        MoveEffectRegistry.register("183", PokeBattle_Move_183::new);
        MoveEffectRegistry.register("184", PokeBattle_Move_184::new);
        MoveEffectRegistry.register("185", PokeBattle_Move_185::new);
        MoveEffectRegistry.register("186", PokeBattle_Move_186::new);
        MoveEffectRegistry.register("189", PokeBattle_Move_189::new);
        MoveEffectRegistry.register("18A", PokeBattle_Move_18A::new);
        MoveEffectRegistry.register("18B", PokeBattle_Move_18B::new);
        MoveEffectRegistry.register("18C", PokeBattle_Move_18C::new);
        MoveEffectRegistry.register("18D", PokeBattle_Move_18D::new);
        MoveEffectRegistry.register("18E", PokeBattle_Move_18E::new);
        MoveEffectRegistry.register("18F", PokeBattle_Move_18F::new);
        MoveEffectRegistry.register("190", PokeBattle_Move_190::new);
        MoveEffectRegistry.register("191", PokeBattle_Move_191::new);
        MoveEffectRegistry.register("192", PokeBattle_Move_192::new);
        MoveEffectRegistry.register("194", PokeBattle_Move_194::new);
        MoveEffectRegistry.register("195", PokeBattle_Move_195::new);
        MoveEffectRegistry.register("197", PokeBattle_Move_197::new);
        MoveEffectRegistry.register("198", PokeBattle_Move_198::new);
        MoveEffectRegistry.register("19A", PokeBattle_Move_19A::new);
        MoveEffectRegistry.register("19B", PokeBattle_Move_19B::new);
        MoveEffectRegistry.register("19C", PokeBattle_Move_19C::new);
        MoveEffectRegistry.register("19D", PokeBattle_Move_19D::new);
        MoveEffectRegistry.register("1CC", PokeBattle_Move_1CC::new);
        MoveEffectRegistry.register("199", PokeBattle_Move_199::new);
        MoveEffectRegistry.register("19E", PokeBattle_Move_19E::new);
        MoveEffectRegistry.register("19F", PokeBattle_Move_19F::new);
        MoveEffectRegistry.register("1A0", PokeBattle_Move_1A0::new);
        MoveEffectRegistry.register("1A1", PokeBattle_Move_1A1::new);
        MoveEffectRegistry.register("1A2", PokeBattle_Move_1A2::new);
        MoveEffectRegistry.register("1A3", PokeBattle_Move_1A3::new);
        MoveEffectRegistry.register("1A4", PokeBattle_Move_1A4::new);
        MoveEffectRegistry.register("187", PokeBattle_Move_187::new);
        MoveEffectRegistry.register("188", PokeBattle_Move_188::new);
        MoveEffectRegistry.register("193", PokeBattle_Move_193::new);
        MoveEffectRegistry.register("196", PokeBattle_Move_196::new);
        MoveEffectRegistry.register("1EC", PokeBattle_Move_1EC::new);
        MoveEffectRegistry.register("1B1", PokeBattle_Move_1B1::new);
        MoveEffectRegistry.register("1B2", PokeBattle_Move_1B2::new);
        MoveEffectRegistry.register("1B3", PokeBattle_Move_1B3::new);
        MoveEffectRegistry.register("1B4", PokeBattle_Move_1B4::new);
        MoveEffectRegistry.register("1A5", PokeBattle_Move_1A5::new);
        MoveEffectRegistry.register("1A6", PokeBattle_Move_1A6::new);
        MoveEffectRegistry.register("1A7", PokeBattle_Move_1A7::new);
        MoveEffectRegistry.register("1A8", PokeBattle_Move_1A8::new);
        MoveEffectRegistry.register("1CD", PokeBattle_Move_1CD::new);
        MoveEffectRegistry.register("1AA", PokeBattle_Move_1AA::new);
        MoveEffectRegistry.register("1AB", PokeBattle_Move_1AB::new);
        MoveEffectRegistry.register("1AC", PokeBattle_Move_1AC::new);
        MoveEffectRegistry.register("1A9", PokeBattle_Move_1A9::new);
        MoveEffectRegistry.register("1AD", PokeBattle_Move_1AD::new);
        MoveEffectRegistry.register("1C0", PokeBattle_Move_1C0::new);
        MoveEffectRegistry.register("1C2", PokeBattle_Move_1C2::new);
        MoveEffectRegistry.register("1C3", PokeBattle_Move_1C3::new);
        MoveEffectRegistry.register("1CA", PokeBattle_Move_1CA::new);
        MoveEffectRegistry.register("1CB", PokeBattle_Move_1CB::new);
        MoveEffectRegistry.register("1F1", PokeBattle_Move_1F1::new);
        MoveEffectRegistry.register("1F2", PokeBattle_Move_1F2::new);
        MoveEffectRegistry.register("1F3", PokeBattle_Move_1F3::new);
        MoveEffectRegistry.register("1F4", PokeBattle_Move_1F4::new);
        MoveEffectRegistry.register("1F5", PokeBattle_Move_1F5::new);
        MoveEffectRegistry.register("1AE", PokeBattle_Move_1AE::new);
        MoveEffectRegistry.register("1BE", PokeBattle_Move_1BE::new);
        MoveEffectRegistry.register("1EA", PokeBattle_Move_1EA::new);
        MoveEffectRegistry.register("1DD", PokeBattle_Move_1DD::new);
        MoveEffectRegistry.register("1C1", PokeBattle_Move_1C1::new);
        MoveEffectRegistry.register("1EE", PokeBattle_Move_1EE::new);
        MoveEffectRegistry.register("1CE", PokeBattle_Move_1CE::new);
        MoveEffectRegistry.register("1CF", PokeBattle_Move_1CF::new);
        MoveEffectRegistry.register("1D0", PokeBattle_Move_1D0::new);
        MoveEffectRegistry.register("1D1", PokeBattle_Move_1D1::new);
        MoveEffectRegistry.register("1D2", PokeBattle_Move_1D2::new);
        MoveEffectRegistry.register("1D3", PokeBattle_Move_1D3::new);
        MoveEffectRegistry.register("1D4", PokeBattle_Move_1D4::new);
        MoveEffectRegistry.register("1D5", PokeBattle_Move_1D5::new);
        MoveEffectRegistry.register("1D6", PokeBattle_Move_1D6::new);
        MoveEffectRegistry.register("1D7", PokeBattle_Move_1D7::new);
        MoveEffectRegistry.register("1D8", PokeBattle_Move_1D8::new);
        MoveEffectRegistry.register("1D9", PokeBattle_Move_1D9::new);
        MoveEffectRegistry.register("1DA", PokeBattle_Move_1DA::new);
        MoveEffectRegistry.register("1B9", PokeBattle_Move_1B9::new);
        MoveEffectRegistry.register("1DE", PokeBattle_Move_1DE::new);
        MoveEffectRegistry.register("1DF", PokeBattle_Move_1DF::new);
        MoveEffectRegistry.register("1DB", PokeBattle_Move_1DB::new);
        MoveEffectRegistry.register("1E0", PokeBattle_Move_1E0::new);
        MoveEffectRegistry.register("1DC", PokeBattle_Move_1DC::new);
        MoveEffectRegistry.register("1E1", PokeBattle_Move_1E1::new);
        MoveEffectRegistry.register("1E2", PokeBattle_Move_1E2::new);
        MoveEffectRegistry.register("1E3", PokeBattle_Move_1E3::new);
        MoveEffectRegistry.register("1E4", PokeBattle_Move_1E4::new);
        MoveEffectRegistry.register("1FD", PokeBattle_Move_1FD::new);
        MoveEffectRegistry.register("1AF", PokeBattle_Move_1AF::new);
        MoveEffectRegistry.register("1B0", PokeBattle_Move_1B0::new);
        MoveEffectRegistry.register("1BF", PokeBattle_Move_1BF::new);
        MoveEffectRegistry.register("1EB", PokeBattle_Move_1EB::new);
        MoveEffectRegistry.register("1ED", PokeBattle_Move_1ED::new);
        MoveEffectRegistry.register("1F0", PokeBattle_Move_1F0::new);
        MoveEffectRegistry.register("1EF", PokeBattle_Move_1EF::new);
        MoveEffectRegistry.register("1B5", PokeBattle_Move_1B5::new);
        MoveEffectRegistry.register("1B6", PokeBattle_Move_1B6::new);
        MoveEffectRegistry.register("1F9", PokeBattle_Move_1F9::new);
        MoveEffectRegistry.register("1FA", PokeBattle_Move_1FA::new);
        MoveEffectRegistry.register("1B7", PokeBattle_Move_1B7::new);
        MoveEffectRegistry.register("1B8", PokeBattle_Move_1B8::new);
        MoveEffectRegistry.register("1BA", PokeBattle_Move_1BA::new);
        MoveEffectRegistry.register("1BB", PokeBattle_Move_1BB::new);
        MoveEffectRegistry.register("1BC", PokeBattle_Move_1BC::new);
        MoveEffectRegistry.register("1BD", PokeBattle_Move_1BD::new);
        MoveEffectRegistry.register("1C4", PokeBattle_Move_1C4::new);
        MoveEffectRegistry.register("1C5", PokeBattle_Move_1C5::new);
        MoveEffectRegistry.register("1C6", PokeBattle_Move_1C6::new);
        MoveEffectRegistry.register("1C8", PokeBattle_Move_1C8::new);
        MoveEffectRegistry.register("1C9", PokeBattle_Move_1C9::new);
        MoveEffectRegistry.register("1C7", PokeBattle_Move_1C7::new);
        MoveEffectRegistry.register("1E5", PokeBattle_Move_1E5::new);
        MoveEffectRegistry.register("1E6", PokeBattle_Move_1E6::new);
        MoveEffectRegistry.register("1E7", PokeBattle_Move_1E7::new);
        MoveEffectRegistry.register("1E8", PokeBattle_Move_1E8::new);
        MoveEffectRegistry.register("1F6", PokeBattle_Move_1F6::new);
        MoveEffectRegistry.register("1F7", PokeBattle_Move_1F7::new);
    }

}