package pokemon.runtime.battle.movefx;

import com.badlogic.gdx.utils.Array;

import pokemon.runtime.battle.Battle;
import pokemon.runtime.battle.BattleMove;
import pokemon.runtime.battle.Battler;
import pokemon.runtime.battle.BattleSide;
import pokemon.runtime.battle.EffectMap;
import pokemon.runtime.battle.PBBattleTerrains;
import pokemon.runtime.battle.PBEffects;
import pokemon.runtime.battle.PBStats;
import pokemon.runtime.battle.PBStatuses;
import pokemon.runtime.battle.PBTargets;
import pokemon.runtime.battle.PBTypeEffectiveness;
import pokemon.runtime.battle.PBTypes;
import pokemon.runtime.battle.PBWeather;
import pokemon.runtime.battle.PendingApi;
import pokemon.runtime.battle.PokeBattle_SceneConstants;
import pokemon.runtime.pokemon.Pokemon;

/**
 * Stage 4 / L2: the <b>115</b> {@code PokeBattle_Move_XXX} function-code classes
 * of {@code Move_Effects_100-17F.rb} (2968 lines), transcribed line by line.
 *
 * <p>Each Ruby class becomes a nested {@code static} class with the same name.
 * The parent is one of {@link MoveEffectsGeneric}'s 24 reusable bases, or
 * {@link MoveEffectBase} when the Ruby parent is {@code PokeBattle_Move} itself.
 * Only the hooks the Ruby class actually overrides are overridden here.</p>
 *
 * <h2>Constructors</h2>
 * <p>Ruby's {@code initialize(battle,move)} only ever sets the base class's
 * configuration fields ({@code @weatherType}, {@code @statUp}, {@code @combos},
 * {@code @statDown}, {@code @powerHerb}, ...) - the ported bases read everything
 * else from the {@code move}/{@code user} arguments of each hook - so each
 * subclass here has a no-arg constructor that sets those fields, one statement
 * per Ruby line.</p>
 *
 * <h2>Conventions (same as {@link MoveEffectsGeneric})</h2>
 * <ul>
 * <li>{@code @battle.pbDisplay(msg)} = {@code user.battle.display(msg)};
 *     {@code user.effects[...]} = {@code user.effects}.</li>
 * <li>Everything that needs a {@code Battler}/{@code Battle} method this runtime
 *     has not landed goes through {@link MoveFxPendingApi} (throwing M0 stubs),
 *     never a made-up value.</li>
 * <li>{@code _INTL} strings are concatenated at the call site with the plugin's
 *     wording copied verbatim.</li>
 * <li>{@code pbShowAnimation} overrides only adjust {@code hitNum} and then call
 *     {@code super}: the animation player is not modelled, so the override is
 *     copied in shape and registered at its line.</li>
 * </ul>
 *
 * <h2>登记 (registered, not translated)</h2>
 * <ul>
 * <li>{@code battle.scene.pbDamageAnimation(user)} (10B:212, 14A:1600 area, ...):
 *     no engine-side counterpart, registered as a comment.</li>
 * <li>{@code @battle.pbStartTerrain} (PokeBattle_Battle:741-769, roster §B
 *     "登记"): the animation and the terrain subsystem are not modelled; the
 *     call is kept at its line.</li>
 * <li>Cross-file parents: {@code PokeBattle_Move_136 < PokeBattle_Move_02F},
 *     {@code 14D < PokeBattle_Move_0CD} and {@code 17C < PokeBattle_Move_0BD}
 *     extend classes that live in {@code MoveEffects_000_07F.java} /
 *     {@code MoveEffects_080_0FF.java} (other files of this batch); they are
 *     referenced by name here and must exist for this file to compile.</li>
 * <li>{@code PokeBattle_Move_106/107/108}: Ruby's second combo entry has
 *     {@code nil} for the override animation, which
 *     {@code MoveEffectsGeneric.PokeBattle_PledgeMove.PledgeCombo}'s
 *     {@code int overrideAnim} field cannot express - registered at the line.</li>
 * </ul>
 */
public final class MoveEffects_100_17F {

    private MoveEffects_100_17F() {
    }

    // ==================================================================
    // 100-102: weather-setting moves (PokeBattle_WeatherMove)
    // ==================================================================

    /** {@code class PokeBattle_Move_100 < PokeBattle_WeatherMove} (Move_Effects_100-17F.rb:4-9); Rain Dance. */
    public static class PokeBattle_Move_100 extends MoveEffectsGeneric.PokeBattle_WeatherMove {
        /** {@code initialize} (:5-8). */
        public PokeBattle_Move_100() {
            weatherType = PBWeather.Rain;                                    // :7
        }
    }

    /** {@code class PokeBattle_Move_101 < PokeBattle_WeatherMove} (:16-21); Sandstorm. */
    public static class PokeBattle_Move_101 extends MoveEffectsGeneric.PokeBattle_WeatherMove {
        /** {@code initialize} (:17-20). */
        public PokeBattle_Move_101() {
            weatherType = PBWeather.Sandstorm;                               // :19
        }
    }

    /** {@code class PokeBattle_Move_102 < PokeBattle_WeatherMove} (:28-33); Hail. */
    public static class PokeBattle_Move_102 extends MoveEffectsGeneric.PokeBattle_WeatherMove {
        /** {@code initialize} (:29-32). */
        public PokeBattle_Move_102() {
            weatherType = PBWeather.Hail;                                    // :31
        }
    }

    // ==================================================================
    // 103-105: entry hazards
    // ==================================================================

    /** {@code class PokeBattle_Move_103 < PokeBattle_Move} (:40-54); Spikes (max. 3 layers). */
    public static class PokeBattle_Move_103 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:41-47). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.pbOpposingSide().effects.intVal(PBEffects.Side.Spikes) >= 3) { // :42
                user.battle.display("但是失败了！");   // :43
                return true;                                                     // :44
            }
            return false;                                                        // :46
        }

        /** {@code pbEffectGeneral(user)} (:49-53). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.pbOpposingSide().effects.increment(PBEffects.Side.Spikes); // :50
            user.battle.display("地菱撒在了"
                    + user.pbOpposingTeam(true) + "周围！");   // :51-52
        }
    }

    /** {@code class PokeBattle_Move_104 < PokeBattle_Move} (:62-76); Toxic Spikes (max. 2 layers). */
    public static class PokeBattle_Move_104 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:63-69). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.pbOpposingSide().effects.intVal(PBEffects.Side.ToxicSpikes) >= 2) { // :64
                user.battle.display("但是失败了！");   // :65
                return true;                                                     // :66
            }
            return false;                                                        // :68
        }

        /** {@code pbEffectGeneral(user)} (:71-75). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.pbOpposingSide().effects.increment(PBEffects.Side.ToxicSpikes); // :72
            user.battle.display("毒菱撒在了"
                    + user.pbOpposingTeam(true) + "周围！");   // :73-74
        }
    }

    /** {@code class PokeBattle_Move_105 < PokeBattle_Move} (:83-97); Stealth Rock. */
    public static class PokeBattle_Move_105 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:84-90). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.pbOpposingSide().effects.truthy(PBEffects.Side.StealthRock)) { // :85
                user.battle.display("但是失败了！");   // :86
                return true;                                                     // :87
            }
            return false;                                                        // :89
        }

        /** {@code pbEffectGeneral(user)} (:92-96). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.pbOpposingSide().effects.set(PBEffects.Side.StealthRock, true); // :93
            user.battle.display("尖锐的岩石漂浮在了"
                    + user.pbOpposingTeam(true) + "周围！");   // :94-95
        }
    }

    // ==================================================================
    // 106-108: the Pledge moves (PokeBattle_PledgeMove)
    // ==================================================================

    /** {@code class PokeBattle_Move_106 < PokeBattle_PledgeMove} (:106-113); Grass Pledge. */
    public static class PokeBattle_Move_106 extends MoveEffectsGeneric.PokeBattle_PledgeMove {
        /** {@code initialize} (:107-112). */
        public PokeBattle_Move_106() {
            // :110-111 @combos = [["107",:SeaOfFire,FIRE,FIREPLEDGE], ["108",:Swamp,nil,nil]]
            // 登记: 第 2 项 Ruby 的 overrideType/overrideAnim 都是 nil；
            //   PledgeCombo.overrideType 是 String（可 null）而 overrideAnim 是 int
            //   （MoveEffectsGeneric.java:1244），无法表达 nil —— 这里传 0 并登记，
            //   需要该字段改成 Integer 才能逐行等价。
            combos = new PledgeCombo[] {
                new PledgeCombo("107", ComboEffect.SEA_OF_FIRE, "FIRE",
                        MoveFxPendingApi.PBMoves_id("FIREPLEDGE")),
                new PledgeCombo("108", ComboEffect.SWAMP, null, 0),
            };
        }
    }

    /** {@code class PokeBattle_Move_107 < PokeBattle_PledgeMove} (:122-129); Fire Pledge. */
    public static class PokeBattle_Move_107 extends MoveEffectsGeneric.PokeBattle_PledgeMove {
        /** {@code initialize} (:123-128). */
        public PokeBattle_Move_107() {
            // :126-127 @combos = [["108",:Rainbow,WATER,WATERPLEDGE], ["106",:SeaOfFire,nil,nil]]
            // 登记: 同上，第 2 项 Ruby 的 overrideType/overrideAnim 是 nil。
            combos = new PledgeCombo[] {
                new PledgeCombo("108", ComboEffect.RAINBOW, "WATER",
                        MoveFxPendingApi.PBMoves_id("WATERPLEDGE")),
                new PledgeCombo("106", ComboEffect.SEA_OF_FIRE, null, 0),
            };
        }
    }

    /** {@code class PokeBattle_Move_108 < PokeBattle_PledgeMove} (:138-145); Water Pledge. */
    public static class PokeBattle_Move_108 extends MoveEffectsGeneric.PokeBattle_PledgeMove {
        /** {@code initialize} (:139-144). */
        public PokeBattle_Move_108() {
            // :142-143 @combos = [["106",:Swamp,GRASS,GRASSPLEDGE], ["107",:Rainbow,nil,nil]]
            // 登记: 同上，第 2 项 Ruby 的 overrideType/overrideAnim 是 nil。
            combos = new PledgeCombo[] {
                new PledgeCombo("106", ComboEffect.SWAMP, "GRASS",
                        MoveFxPendingApi.PBMoves_id("GRASSPLEDGE")),
                new PledgeCombo("107", ComboEffect.RAINBOW, null, 0),
            };
        }
    }

    // ==================================================================
    // 109-10F
    // ==================================================================

    /** {@code class PokeBattle_Move_109 < PokeBattle_Move} (:156-163); Pay Day. */
    public static class PokeBattle_Move_109 extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:157-162). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            Battle battle = user.battle;
            if (user.pbOwnedByPlayer()) {                                    // :158
                battle.field.effects
                        .add(PBEffects.Field.PayDay, 5 * user.level());      // :159
            }
            battle.display("金币撒的到处都是！");                  // :161
        }
    }

    /** {@code class PokeBattle_Move_10A < PokeBattle_Move} (:171-197); Brick Break / Psychic Fangs. */
    public static class PokeBattle_Move_10A extends MoveEffectBase {

        /** {@code ignoresReflect?} (:172). */
        @Override
        public boolean ignoresReflect(BattleMove move) {
            return true;                                                     // :172
        }

        /** {@code pbEffectGeneral(user)} (:174-187). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            Battle battle = user.battle;
            if (user.pbOpposingSide().effects.intVal(PBEffects.Side.LightScreen) > 0) { // :175
                user.pbOpposingSide().effects.set(PBEffects.Side.LightScreen, 0);       // :176
                battle.display(user.pbOpposingTeam(false)
                        + "的光墙消失了！");                                      // :177
            }
            if (user.pbOpposingSide().effects.intVal(PBEffects.Side.Reflect) > 0) {     // :179
                user.pbOpposingSide().effects.set(PBEffects.Side.Reflect, 0);           // :180
                battle.display(user.pbOpposingTeam(false)
                        + "的反射盾消失了！");                                    // :181
            }
            if (user.pbOpposingSide().effects.intVal(PBEffects.Side.AuroraVeil) > 0) {  // :183
                user.pbOpposingSide().effects.set(PBEffects.Side.AuroraVeil, 0);        // :184
                battle.display(user.pbOpposingTeam(false)
                        + "的极光幕消失了！");                                    // :185
            }
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} (:189-196) - 登记: 动画播放器未建模. */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets, int hitNum,
                                    boolean showAnimation) {
            if (user.pbOpposingSide().effects.intVal(PBEffects.Side.LightScreen) > 0
                    || user.pbOpposingSide().effects.intVal(PBEffects.Side.Reflect) > 0
                    || user.pbOpposingSide().effects.intVal(PBEffects.Side.AuroraVeil) > 0) { // :190-192
                hitNum = 1;                                                  // :193 Wall-breaking anim
            }
            super.pbShowAnimation(move, id, user, targets, hitNum, showAnimation);   // :195
        }
    }

    /** {@code class PokeBattle_Move_10B < PokeBattle_Move} (:206-217); High Jump Kick / Jump Kick. */
    public static class PokeBattle_Move_10B extends MoveEffectBase {

        /** {@code recoilMove?} (:206). */
        @Override
        public boolean recoilMove(BattleMove move) {
            return true;                                                     // :206
        }

        /** {@code unusableInGravity?} (:207). */
        @Override
        public boolean unusableInGravity(BattleMove move) {
            return true;                                                     // :207
        }

        /** {@code pbCrashDamage(user)} (:209-216). */
        @Override
        public void pbCrashDamage(BattleMove move, Battler user) {
            if (!user.takesIndirectDamage(false)) {               // :210
                return;
            }
            user.battle.display(user.pbThis() + "因无法停下而受到伤害！");          // :211
            // :212 @battle.scene.pbDamageAnimation(user) —— 登记: Scene_Animations:224
            //      依赖 PokeBattle_Scene（未建模），无引擎侧入口，不造替代实现
            user.pbReduceHP(user.maxHp() / 2, false, true, true);      // :213 (anim=false)
            user.pbItemHPHealCheck(0, false);                        // :214
            if (user.fainted()) {                                            // :215
                user.pbFaint();
            }
        }
    }

    /** {@code class PokeBattle_Move_10C < PokeBattle_Move} (:224-250); Substitute. */
    public static class PokeBattle_Move_10C extends MoveEffectBase {

        /** {@code @subLife} - set by {@code pbMoveFailed?} and read by the two hooks below (:230-231). */
        private int subLife;

        /** {@code pbMoveFailed?(user,targets)} (:225-237). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.effects.intVal(PBEffects.Battler.Substitute) > 0) {   // :226
                user.battle.display(user.pbThis() + "已经有一个替身了！");           // :227
                return true;                                                     // :228
            }
            subLife = user.maxHp() / 4;                                      // :230
            if (subLife < 1) {
                subLife = 1;                                                 // :231
            }
            if (user.hp <= subLife) {                                        // :232
                user.battle.display("但是没有足够的HP制造替身！"); // :233
                return true;                                                     // :234
            }
            return false;                                                        // :236
        }

        /** {@code pbOnStartUse(user,targets)} (:239-242). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            pbReduceHP(user, subLife, false, false);                         // :240 (anim=false,registerDamage=false)
            user.pbItemHPHealCheck(0, false);                        // :241
        }

        /** {@code pbEffectGeneral(user)} (:244-249). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.Trapping, 0);           // :245
            user.effects.set(PBEffects.Battler.TrappingMove, 0);       // :246
            user.effects.set(PBEffects.Battler.Substitute, subLife);   // :247
            user.battle.display(user.pbThis() + "制造了替身！");                     // :248
        }
    }

    /** {@code class PokeBattle_Move_10D < PokeBattle_Move} (:260-317); Curse. */
    public static class PokeBattle_Move_10D extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:261). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :261
        }

        /** {@code pbTarget(user)} (:263-266). */
        @Override
        public int pbTarget(BattleMove move, Battler user) {
            if (user.pbHasType("GHOST")) {                                   // :264
                return PBTargets.NearFoe;                                    // :264 PBTargets::NearFoe
            }
            return super.pbTarget(move, user);                               // :265
        }

        /** {@code pbMoveFailed?(user,targets)} (:268-277). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.pbHasType("GHOST")) {                                   // :269
                return false;
            }
            if (!user.pbCanLowerStatStage(PBStats.SPEED, user, move, false)   // :270
                    && !user.pbCanRaiseStatStage(PBStats.ATTACK, user, move, false)   // :271
                    && !user.pbCanRaiseStatStage(PBStats.DEFENSE, user, move, false)) { // :272
                user.battle.display("但是失败了！");     // :273
                return true;                                                     // :274
            }
            return false;                                                        // :276
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:279-285). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (user.pbHasType("GHOST")                                      // :280
                    && target.effects.truthy(PBEffects.Battler.Curse)) { // :280 target.effects[Curse]
                user.battle.display("但是失败了！");     // :281
                return true;                                                     // :282
            }
            return false;                                                        // :284
        }

        /** {@code pbEffectGeneral(user)} (:287-302). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            if (user.pbHasType("GHOST")) {                                   // :288
                return;
            }
            // Non-Ghost effect (:289)
            if (user.pbCanLowerStatStage(PBStats.SPEED, user, move, false)) {   // :290
                user.pbLowerStatStage(PBStats.SPEED, 1, user, true, false, false);     // :291
            }
            boolean showAnim = true;                                         // :293
            if (user.pbCanRaiseStatStage(PBStats.ATTACK, user, move, false)) {  // :294
                if (user.pbRaiseStatStage(PBStats.ATTACK, 1, user, showAnim, false)) { // :295
                    showAnim = false;                                        // :296
                }
            }
            if (user.pbCanRaiseStatStage(PBStats.DEFENSE, user, move, false)) { // :299
                user.pbRaiseStatStage(PBStats.DEFENSE, 1, user, showAnim, false); // :300
            }
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:304-311). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (!user.pbHasType("GHOST")) {                                  // :305
                return;
            }
            // Ghost effect (:306)
            user.battle.display(user.pbThis()
                    + "消耗了HP来附加诅咒给" + target.pbThis(true) + "！");  // :307
            target.effects.set(PBEffects.Battler.Curse, true);   // :308
            user.pbReduceHP(user.maxHp() / 2, false, true, true);      // :309
            user.pbItemHPHealCheck(0, false);                        // :310
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} (:313-316) - 登记: 动画播放器未建模. */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets, int hitNum,
                                    boolean showAnimation) {
            if (!user.pbHasType("GHOST")) {                                  // :314
                hitNum = 1;                                                  // :314 Non-Ghost anim
            }
            super.pbShowAnimation(move, id, user, targets, hitNum, showAnimation);   // :315
        }
    }

    /** {@code class PokeBattle_Move_10E < PokeBattle_Move} (:324-350); Spite. */
    public static class PokeBattle_Move_10E extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:325). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :325
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:327-338). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            boolean failed = true;                                           // :328
            for (BattleMove m : eachMove(target)) {                          // :329 target.eachMove
                // :330 next if m.id!=target.lastRegularMoveUsed || m.pp==0 || m.totalpp<=0
                //      lastRegularMoveUsed 在本运行时存内部名（null = Ruby 的 -1），
                //      故按 AbilitiesOnHit.java:487 的既有写法比较内部名。
                if (target.lastRegularMoveUsed == null
                        || !target.lastRegularMoveUsed.equals(m.internalName())
                        || pp(m) == 0 || m.totalpp() <= 0) {
                    continue;
                }
                failed = false;                                              // :331
                break;                                                       // :331
            }
            if (failed) {                                                    // :333
                user.battle.display("但是失败了！");  // :334
                return true;                                                 // :335
            }
            return false;                                                    // :337
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:340-349). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            for (BattleMove m : eachMove(target)) {                          // :341
                // :342 next if m.id!=target.lastRegularMoveUsed（内部名比较，见 :330 的说明）
                if (target.lastRegularMoveUsed == null
                        || !target.lastRegularMoveUsed.equals(m.internalName())) {
                    continue;
                }
                int reduction = Math.min(4, pp(m));                          // :343 [4,m.pp].min
                pbSetPP(target, m, pp(m) - reduction);                       // :344
                user.battle.display(target.pbThis(true) + "的" + m.name()
                                + "减少了" + reduction + "点PP！");            // :345-346
                break;                                                       // :347
            }
        }
    }

    /** {@code class PokeBattle_Move_10F < PokeBattle_Move} (:357-370); Nightmare. */
    public static class PokeBattle_Move_10F extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:358-364). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (!target.asleep()                                              // :359
                    || target.effects.truthy(PBEffects.Battler.Nightmare)) {
                user.battle.display("但是失败了！");  // :360
                return true;                                                 // :361
            }
            return false;                                                    // :363
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:366-369). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.Nightmare, true);   // :367
            user.battle.display(target.pbThis() + "开始做噩梦了！");               // :368
        }
    }

    // ==================================================================
    // 110-11C
    // ==================================================================

    /** {@code class PokeBattle_Move_110 < PokeBattle_Move} (:378-412); Rapid Spin. */
    public static class PokeBattle_Move_110 extends MoveEffectBase {

        /** {@code pbEffectAfterAllHits(user,target)} (:379-411). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (user.fainted() || target.damageState.unaffected) {   // :380
                return;
            }
            Battle battle = user.battle;
            if (user.effects.intVal(PBEffects.Battler.Trapping) > 0) {     // :381
                String trapMove = PBMoves_getName(user.effects
                        .intVal(PBEffects.Battler.TrappingMove));                 // :382
                Battler trapUser = battle.battlerAt(user.effects
                        .intVal(PBEffects.Battler.TrappingUser));                 // :383 @battle.battlers[...]
                battle.display(user.pbThis() + "摆脱了"
                        + trapUser.pbThis(true) + "的" + trapMove + "！");   // :384
                user.effects.set(PBEffects.Battler.Trapping, 0);       // :385
                user.effects.set(PBEffects.Battler.TrappingMove, 0);   // :386
                user.effects.set(PBEffects.Battler.TrappingUser, -1);  // :387
            }
            if (user.effects.intVal(PBEffects.Battler.LeechSeed) >= 0) {   // :389
                user.effects.set(PBEffects.Battler.LeechSeed, -1);     // :390
                battle.display(user.pbThis() + "撒下了种子！"); // :391
            }
            if (user.pbOwnSide().effects.truthy(PBEffects.Side.StealthRock)) { // :393
                user.pbOwnSide().effects.set(PBEffects.Side.StealthRock, false); // :394
                battle.display(user.pbThis() + "吹走了隐形岩！"); // :395
            }
            if (user.pbOwnSide().effects.intVal(PBEffects.Side.Spikes) > 0) { // :397
                user.pbOwnSide().effects.set(PBEffects.Side.Spikes, 0);  // :398
                battle.display(user.pbThis() + "吹走了地菱！");   // :399
            }
            if (user.pbOwnSide().effects.intVal(PBEffects.Side.ToxicSpikes) > 0) { // :401
                user.pbOwnSide().effects.set(PBEffects.Side.ToxicSpikes, 0); // :402
                battle.display(user.pbThis() + "吹走了毒菱！");   // :403
            }
            if (user.pbOwnSide().effects.truthy(PBEffects.Side.StickyWeb)) { // :405
                user.pbOwnSide().effects.set(PBEffects.Side.StickyWeb, false); // :406
                user.pbOwnSide().effects.set(PBEffects.Side.StickyWebUser, -1); // :407
                battle.display(user.pbThis() + "吹走了黏黏网！"); // :408
            }
            // --- Arceus 段重开类覆盖版（Arceus:370-412）：同名 pbEffectAfterAllHits 覆盖主段，
            //     比主段多出下面 4 行（:402-409）。按 Lead 的「重开类 = 后者胜出」口径，用覆盖版替换。---
            // Pokémon Legends: Arceus (:402)
            if (user.effects.intVal(PBEffects.Battler.CeaselessEdge) > -1
                    || user.effects.intVal(PBEffects.Battler.StoneAxe) > -1) { // :403-404
                user.effects.set(PBEffects.Battler.StoneAxe, -1);        // :405
                user.effects.set(PBEffects.Battler.CeaselessEdge, -1);   // :406
                battle.display(user.pbThis() + "吹散了碎片！"); // :407
            }
            user.effects.set(PBEffects.Battler.UsedRapidSpin, true);      // :409
            user.pbRaiseStatStage(PBStats.SPEED, 1, user, true, false);     // :410
        }
    }

    /** {@code class PokeBattle_Move_111 < PokeBattle_Move} (:419-463); Doom Desire / Future Sight. */
    public static class PokeBattle_Move_111 extends MoveEffectBase {

        /** {@code cannotRedirect?} (:420). */
        @Override
        public boolean cannotRedirect(BattleMove move) {
            return true;                                                     // :420
        }

        /** {@code pbDamagingMove?} (:422-425) - "Stops damage being dealt in the setting-up turn". */
        @Override
        public boolean pbDamagingMove(BattleMove move) {
            // :423 return false if !@battle.futureSight
            //      本 hook 无 user 参数，而本运行时 BattleMove 没有 battle 反向引用；
            //      `Battle.futureSight` 全工程只有读取处（AbilitiesOnHit:1247）、**无任何写入处**，
            //      故 `!@battle.futureSight` 可证为 true —— 与 Lead 对 inTwoTurnAttack? 的裁决同源，
            //      此处内联（不抛异常、不造替代）。
            return super.pbDamagingMove(move);                               // :424 return super
        }

        /** {@code pbAccuracyCheck(user,target)} (:427-430). */
        @Override
        public boolean pbAccuracyCheck(BattleMove move, Battler user, Battler target) {
            if (!user.battle.futureSight) {                      // :428
                return true;
            }
            return super.pbAccuracyCheck(move, user, target);                // :429
        }

        /** {@code pbDisplayUseMessage(user)} (:432-434). */
        @Override
        public void pbDisplayUseMessage(BattleMove move, Battler user) {
            if (!user.battle.futureSight) {                      // :433
                super.pbDisplayUseMessage(move, user);
            }
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:436-443). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            if (!battle.futureSight                                            // :437
                    && battle.field.positions[target.index].effects
                            .intVal(PBEffects.Position.FutureSightCounter) > 0) { // :438
                battle.display("但是失败了！");                    // :439
                return true;                                                  // :440
            }
            return false;                                                     // :442
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:445-457). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            if (battle.futureSight) {                                        // :446
                return;
            }
            EffectMap effects = battle.field.positions[target.index].effects; // :447
            effects.set(PBEffects.Position.FutureSightCounter, 3);           // :448
            effects.set(PBEffects.Position.FutureSightMove, move.id());      // :449
            effects.set(PBEffects.Position.FutureSightUserIndex, user.index); // :450
            effects.set(PBEffects.Position.FutureSightUserPartyIndex, user.pokemonIndex); // :451
            if (move.id() == MoveFxPendingApi.PBMoves_id("DOOMDESIRE")) {    // :452 isConst?(@id,PBMoves,:DOOMDESIRE)
                battle.display(user.pbThis() + "选择了破灭的未来！");         // :453
            } else {
                battle.display(user.pbThis() + "预知到了攻击！");             // :455
            }
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} (:459-462) - 登记: 动画播放器未建模. */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets, int hitNum,
                                    boolean showAnimation) {
            if (!user.battle.futureSight) {                      // :460
                hitNum = 1;                                                  // :460 Charging anim
            }
            super.pbShowAnimation(move, id, user, targets, hitNum, showAnimation);   // :461
        }
    }

    /** {@code class PokeBattle_Move_112 < PokeBattle_Move} (:471-497); Stockpile. */
    public static class PokeBattle_Move_112 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:472-478). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.effects.intVal(PBEffects.Battler.Stockpile) >= 3) {   // :473
                user.battle.display(user.pbThis() + "不能再积蓄更多了！");            // :474
                return true;                                                     // :475
            }
            return false;                                                        // :477
        }

        /** {@code pbEffectGeneral(user)} (:480-496). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.increment(PBEffects.Battler.Stockpile);         // :481
            user.battle.display(user.pbThis()
                    + "蓄力了" + user.effects.intVal(PBEffects.Battler.Stockpile)
                    + "次！");                                                   // :482-483
            boolean showAnim = true;                                             // :484
            if (user.pbCanRaiseStatStage(PBStats.DEFENSE, user, move, false)) { // :485
                if (user.pbRaiseStatStage(PBStats.DEFENSE, 1, user, showAnim, false)) { // :486
                    user.effects.increment(PBEffects.Battler.StockpileDef); // :487
                    showAnim = false;                                        // :488
                }
            }
            if (user.pbCanRaiseStatStage(PBStats.SPDEF, user, move, false)) { // :491
                if (user.pbRaiseStatStage(PBStats.SPDEF, 1, user, showAnim, false)) { // :492
                    user.effects.increment(PBEffects.Battler.StockpileSpDef); // :493
                }
            }
        }
    }

    /** {@code class PokeBattle_Move_113 < PokeBattle_Move} (:505-538); Spit Up. */
    public static class PokeBattle_Move_113 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:506-512). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.effects.intVal(PBEffects.Battler.Stockpile) == 0) {   // :507
                user.battle.display("但并不能吐出东西！"); // :508
                return true;                                                     // :509
            }
            return false;                                                        // :511
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:514-516). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            return 100 * user.effects.intVal(PBEffects.Battler.Stockpile); // :515
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:518-537). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (user.fainted()
                    || user.effects.intVal(PBEffects.Battler.Stockpile) == 0) { // :519
                return;
            }
            if (target.damageState.unaffected) {           // :520
                return;
            }
            Battle battle = user.battle;
            battle.display(user.pbThis() + "积蓄的力量消失了！"); // :521
            if (battle.pbAllFainted(target.idxOwnSide())) {                  // :522
                return;
            }
            boolean showAnim = true;                                         // :523
            if (user.effects.intVal(PBEffects.Battler.StockpileDef) > 0
                    && user.pbCanLowerStatStage(PBStats.DEFENSE, user, move, false)) { // :524-525
                if (user.pbLowerStatStage(PBStats.DEFENSE,
                        user.effects.intVal(PBEffects.Battler.StockpileDef), user, showAnim, false, false)) { // :526
                    showAnim = false;                                        // :527
                }
            }
            if (user.effects.intVal(PBEffects.Battler.StockpileSpDef) > 0
                    && user.pbCanLowerStatStage(PBStats.SPDEF, user, move, false)) { // :530-531
                user.pbLowerStatStage(PBStats.SPDEF,
                        user.effects.intVal(PBEffects.Battler.StockpileSpDef), user, showAnim, false, false); // :532
            }
            user.effects.set(PBEffects.Battler.Stockpile, 0);         // :534
            user.effects.set(PBEffects.Battler.StockpileDef, 0);      // :535
            user.effects.set(PBEffects.Battler.StockpileSpDef, 0);    // :536
        }
    }

    /** {@code class PokeBattle_Move_114 < PokeBattle_Move} (:546-589); Swallow. */
    public static class PokeBattle_Move_114 extends MoveEffectBase {

        /** {@code healingMove?} (:547). */
        @Override
        public boolean healingMove(BattleMove move) {
            return true;                                                     // :547
        }

        /** {@code pbMoveFailed?(user,targets)} (:549-561). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.effects.intVal(PBEffects.Battler.Stockpile) == 0) {   // :550
                user.battle.display("但无法吞下！");      // :551
                return true;                                                     // :552
            }
            if (!user.canHeal()                                              // :554
                    && user.effects.intVal(PBEffects.Battler.StockpileDef) == 0   // :555
                    && user.effects.intVal(PBEffects.Battler.StockpileSpDef) == 0) { // :556
                user.battle.display("但是失败了！");      // :557
                return true;                                                     // :558
            }
            return false;                                                        // :560
        }

        /** {@code pbEffectGeneral(user)} (:563-588). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            Battle battle = user.battle;
            int hpGain = 0;                                                  // :564
            switch (Math.max(user.effects.intVal(PBEffects.Battler.Stockpile), 1)) { // :565
                case 1: hpGain = user.maxHp() / 4; break;                    // :566
                case 2: hpGain = user.maxHp() / 2; break;                    // :567
                case 3: hpGain = user.maxHp(); break;                        // :568
                default: break;
            }
            if (user.pbRecoverHP(hpGain) > 0) {                              // :570
                battle.display(user.pbThis() + "的HP回复了。"); // :571
            }
            battle.display(user.pbThis() + "积蓄的力量消失了！"); // :573
            boolean showAnim = true;                                         // :574
            if (user.effects.intVal(PBEffects.Battler.StockpileDef) > 0
                    && user.pbCanLowerStatStage(PBStats.DEFENSE, user, move, false)) { // :575-576
                if (user.pbLowerStatStage(PBStats.DEFENSE,
                        user.effects.intVal(PBEffects.Battler.StockpileDef), user, showAnim, false, false)) { // :577
                    showAnim = false;                                        // :578
                }
            }
            if (user.effects.intVal(PBEffects.Battler.StockpileSpDef) > 0
                    && user.pbCanLowerStatStage(PBStats.SPDEF, user, move, false)) { // :581-582
                user.pbLowerStatStage(PBStats.SPDEF,
                        user.effects.intVal(PBEffects.Battler.StockpileSpDef), user, showAnim, false, false); // :583
            }
            user.effects.set(PBEffects.Battler.Stockpile, 0);         // :585
            user.effects.set(PBEffects.Battler.StockpileDef, 0);      // :586
            user.effects.set(PBEffects.Battler.StockpileSpDef, 0);    // :587
        }
    }

    /** {@code class PokeBattle_Move_115 < PokeBattle_Move} (:596-614); Focus Punch. */
    public static class PokeBattle_Move_115 extends MoveEffectBase {

        /** {@code pbDisplayChargeMessage(user)} (:597-601). */
        @Override
        public void pbDisplayChargeMessage(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.FocusPunch, true);     // :598
            PendingApi.pbCommonAnimation(user.battle, "FocusPunch", user); // :599
            user.battle.display(user.pbThis() + "正在集中注意力！");                // :600
        }

        /** {@code pbDisplayUseMessage(user)} (:603-605). */
        @Override
        public void pbDisplayUseMessage(BattleMove move, Battler user) {
            if (!user.effects.truthy(PBEffects.Battler.FocusPunch)
                    || user.lastHPLost == 0) {                               // :604
                super.pbDisplayUseMessage(move, user);
            }
        }

        /** {@code pbMoveFailed?(user,targets)} (:607-613). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.effects.truthy(PBEffects.Battler.FocusPunch)
                    && user.lastHPLost > 0) {                                // :608
                user.battle.display(user.pbThis() + "无法集中注意力和行动！");       // :609
                return true;                                                 // :610
            }
            return false;                                                    // :612
        }
    }

    /** {@code class PokeBattle_Move_116 < PokeBattle_Move} (:622-637); Sucker Punch. */
    public static class PokeBattle_Move_116 extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:623-636). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            if (!":UseMove".equals(battle.choices(target.index)[0])) {       // :624 @battle.choices[i][0]!=:UseMove
                battle.display("但是失败了！");                  // :625
                return true;                                                 // :626
            }
            BattleMove oppMove = (BattleMove) battle.choices(target.index)[2]; // :628
            if (oppMove == null || oppMove.id() <= 0                        // :629
                    || (!"0B0".equals(oppMove.function())                   // :630 Me First
                    && (target.movedThisRound()             // :631
                    || MoveEffectRegistry.of(oppMove.function()).statusMove(oppMove)))) { // :631 oppMove.statusMove?
                battle.display("但是失败了！");                  // :632
                return true;                                                 // :633
            }
            return false;                                                    // :635
        }
    }

    /** {@code class PokeBattle_Move_117 < PokeBattle_Move} (:645-655); Follow Me / Rage Powder. */
    public static class PokeBattle_Move_117 extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:646-654). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.FollowMe, 1);          // :647
            for (Battler b : user.allAllies()) {                   // :648 user.eachAlly
                if (b.effects.intVal(PBEffects.Battler.FollowMe)
                        < user.effects.intVal(PBEffects.Battler.FollowMe)) { // :649
                    continue;
                }
                user.effects.set(PBEffects.Battler.FollowMe,
                        b.effects.intVal(PBEffects.Battler.FollowMe) + 1);   // :650
            }
            if (move.id() == MoveFxPendingApi.PBMoves_id("RAGEPOWDER")) {    // :652 isConst?(@id,PBMoves,:RAGEPOWDER)
                user.effects.set(PBEffects.Battler.RagePowder, true);    // :652
            }
            user.battle.display(user.pbThis() + "成为了视线的焦点！");               // :653
        }
    }

    /** {@code class PokeBattle_Move_118 < PokeBattle_Move} (:663-694); Gravity. */
    public static class PokeBattle_Move_118 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:664-670). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = user.battle;
            if (battle.field.effects.intVal(PBEffects.Field.Gravity) > 0) { // :665
                battle.display("但是失败了！");                  // :666
                return true;                                                 // :667
            }
            return false;                                                    // :669
        }

        /** {@code pbEffectGeneral(user)} (:672-693). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            Battle battle = user.battle;
            battle.field.effects.set(PBEffects.Field.Gravity, 5);   // :673
            battle.display("重力变强了！");                         // :674
            for (Battler b : battle.eachBattler()) {              // :675 @battle.eachBattler
                boolean showMessage = false;                                 // :676
                // :677 b.inTwoTurnAttack?("0C9","0CC","0CE") —— 本运行时 @effects[TwoTurnAttack]
                //      无人写入（battlerapi 中间档未落该效果），可证为 false；按 Lead 裁决内联 false。
                if (false) {
                    b.effects.set(PBEffects.Battler.TwoTurnAttack, 0);    // :678
                    if (!b.movedThisRound()) {               // :679
                        battle.pbClearChoice(b.index);
                    }
                    showMessage = true;                                      // :680
                }
                if (b.effects.intVal(PBEffects.Battler.MagnetRise) > 0   // :682
                        || b.effects.intVal(PBEffects.Battler.Telekinesis) > 0   // :683
                        || b.effects.intVal(PBEffects.Battler.SkyDrop) >= 0) {   // :684
                    b.effects.set(PBEffects.Battler.MagnetRise, 0);      // :685
                    b.effects.set(PBEffects.Battler.Telekinesis, 0);     // :686
                    b.effects.set(PBEffects.Battler.SkyDrop, -1);        // :687
                    showMessage = true;                                      // :688
                }
                if (showMessage) {                                           // :690-691
                    battle.display(b.pbThis() + "因为重力无法停留在空中");       // :690-691
                }
            }
        }
    }

    /** {@code class PokeBattle_Move_119 < PokeBattle_Move} (:701-718); Magnet Rise. */
    public static class PokeBattle_Move_119 extends MoveEffectBase {

        /** {@code unusableInGravity?} (:702). */
        @Override
        public boolean unusableInGravity(BattleMove move) {
            return true;                                                     // :702
        }

        /** {@code pbMoveFailed?(user,targets)} (:704-712). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.effects.truthy(PBEffects.Battler.Ingrain)     // :705
                    || user.effects.truthy(PBEffects.Battler.SmackDown)   // :706
                    || user.effects.intVal(PBEffects.Battler.MagnetRise) > 0) { // :707
                user.battle.display("但是失败了！");   // :708
                return true;                                                     // :709
            }
            return false;                                                        // :711
        }

        /** {@code pbEffectGeneral(user)} (:714-717). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.MagnetRise, 5);        // :715
            user.battle.display(user.pbThis() + "通过电磁力浮了起来！");             // :716
        }
    }

    /** {@code class PokeBattle_Move_11A < PokeBattle_Move} (:725-750); Telekinesis. */
    public static class PokeBattle_Move_11A extends MoveEffectBase {

        /** {@code unusableInGravity?} (:726). */
        @Override
        public boolean unusableInGravity(BattleMove move) {
            return true;                                                     // :726
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:728-744). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            if (target.effects.truthy(PBEffects.Battler.Ingrain)     // :729
                    || target.effects.truthy(PBEffects.Battler.SmackDown)   // :730
                    || target.effects.intVal(PBEffects.Battler.Telekinesis) > 0) { // :731
                battle.display("但是失败了！");                    // :732
                return true;                                                   // :733
            }
            if (target.isSpecies("DIGLETT")                                   // :735
                    || target.isSpecies("DUGTRIO")                            // :736
                    || target.isSpecies("SANDYGAST")                          // :737
                    || target.isSpecies("PALOSSAND")                          // :738
                    || (target.isSpecies("GENGAR") && mega(target))) {        // :739
                battle.display("但是失败了！");                    // :740
                return true;                                                   // :741
            }
            return false;                                                      // :743
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:746-749). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.Telekinesis, 3);      // :747
            user.battle.display(target.pbThis() + "被投向了空中！");                 // :748
        }
    }

    /** {@code class PokeBattle_Move_11B < PokeBattle_Move} (:757-759); Sky Uppercut. */
    public static class PokeBattle_Move_11B extends MoveEffectBase {

        /** {@code hitsFlyingTargets?} (:758). */
        @Override
        public boolean hitsFlyingTargets(BattleMove move) {
            return true;                                                     // :758
        }
    }

    /** {@code class PokeBattle_Move_11C < PokeBattle_Move} (:767-790); Smack Down / Thousand Arrows. */
    public static class PokeBattle_Move_11C extends MoveEffectBase {

        /** {@code hitsFlyingTargets?} (:768). */
        @Override
        public boolean hitsFlyingTargets(BattleMove move) {
            return true;                                                     // :768
        }

        /** {@code pbCalcTypeModSingle(moveType,defType,user,target)} (:770-774). */
        @Override
        public int pbCalcTypeModSingle(BattleMove move, String moveType, String defType, Battler user,
                                       Battler target) {
            if ("GROUND".equals(moveType) && "FLYING".equals(defType)) {      // :771-772
                return PBTypeEffectiveness.NORMAL_EFFECTIVE_ONE;             // :772
            }
            return super.pbCalcTypeModSingle(move, moveType, defType, user, target); // :773
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:776-789). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (target.fainted()) {                                          // :777
                return;
            }
            if (target.damageState.unaffected
                    || target.damageState.substitute) {    // :778
                return;
            }
            // :779 target.inTwoTurnAttack?("0CE") || target.effects[PBEffects::SkyDrop]>=0
            //      —— inTwoTurnAttack? 按 Lead 裁决内联 false（本运行时无人写 TwoTurnAttack）；
            //      保留 SkyDrop 那一支。
            if (target.effects.intVal(PBEffects.Battler.SkyDrop) >= 0) {   // :779 Sky Drop
                return;
            }
            // :780 return if !target.airborne? && !target.inTwoTurnAttack?("0C9","0CC")
            //      —— 第二项内联 false，故等价于「不浮空即返回」。
            if (!target.airborne()) {                                        // :780
                return;
            }
            Battle battle = user.battle;
            target.effects.set(PBEffects.Battler.SmackDown, true);     // :781
            // :782-785 target.inTwoTurnAttack?("0C9","0CC")（Fly/Bounce，注意不含 Sky Drop）
            //      —— 按 Lead 裁决内联 false，该块整体不执行。
            target.effects.set(PBEffects.Battler.MagnetRise, 0);       // :786
            target.effects.set(PBEffects.Battler.Telekinesis, 0);      // :787
            battle.display(target.pbThis() + "被击落了！"); // :788
        }
    }

    /** {@code class PokeBattle_Move_11D < PokeBattle_Move} (:797-822); After You. */
    public static class PokeBattle_Move_11D extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:798). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :798
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:800-815). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            // :802 Target has already moved this round
            if (pbMoveFailedTargetAlreadyMoved(move, target)) {
                return true;
            }
            // :804 Target was going to move next anyway (somehow)
            if (target.effects.truthy(PBEffects.Battler.MoveNext)) {   // :804
                battle.display("但是失败了！");                    // :805
                return true;                                                   // :806
            }
            // :808-809 Target didn't choose to use a move this round
            BattleMove oppMove = (BattleMove) battle.choices(target.index)[2]; // :809
            if (oppMove == null || oppMove.id() <= 0) {                      // :810
                battle.display("但是失败了！");                    // :811
                return true;                                                   // :812
            }
            return false;                                                      // :814
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:817-821). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.MoveNext, true);      // :818
            target.effects.set(PBEffects.Battler.Quash, 0);            // :819
            user.battle.display(target.pbThis() + "接受了好意！");                   // :820
        }
    }

    /** {@code class PokeBattle_Move_11E < PokeBattle_Move} (:829-868); Quash. */
    public static class PokeBattle_Move_11E extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:830-855). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            if (pbMoveFailedTargetAlreadyMoved(move, target)) {              // :831
                return true;
            }
            // :833 Target isn't going to use a move
            BattleMove oppMove = (BattleMove) battle.choices(target.index)[2]; // :833
            if (oppMove == null || oppMove.id() <= 0) {                      // :834
                battle.display("但是它失败了！");                  // :835
                return true;                                                   // :836
            }
            // :839 Target is already maximally Quashed and will move last anyway
            int highestQuash = 0;                                            // :839
            for (Battler b : battlers(battle)) {                             // :840 @battle.battlers.each
                if (b == null) {                                             // :841
                    continue;
                }
                if (b.effects.intVal(PBEffects.Battler.Quash) <= highestQuash) { // :842
                    continue;
                }
                highestQuash = b.effects.intVal(PBEffects.Battler.Quash);   // :843
            }
            if (highestQuash > 0
                    && target.effects.intVal(PBEffects.Battler.Quash) == highestQuash) { // :845
                battle.display("但是它失败了！");                  // :846
                return true;                                                   // :847
            }
            // :850 Target was already going to move last
            if (highestQuash == 0 && pbPriority(battle).peek().index == target.index) {
                battle.display("但是它失败了！");                  // :851
                return true;                                                   // :852
            }
            return false;                                                      // :854
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:857-867). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            int highestQuash = 0;                                            // :858
            for (Battler b : battlers(battle)) {                             // :859
                if (b == null) {                                             // :860
                    continue;
                }
                if (b.effects.intVal(PBEffects.Battler.Quash) <= highestQuash) { // :861
                    continue;
                }
                highestQuash = b.effects.intVal(PBEffects.Battler.Quash);   // :862
            }
            target.effects.set(PBEffects.Battler.Quash, highestQuash + 1); // :864
            target.effects.set(PBEffects.Battler.MoveNext, false);         // :865
            battle.display(target.pbThis() + "的行动被推迟了！"); // :866
        }
    }

    /** {@code class PokeBattle_Move_11F < PokeBattle_Move} (:878-893); Trick Room. */
    public static class PokeBattle_Move_11F extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:879-887). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            Battle battle = user.battle;
            if (battle.field.effects.intVal(PBEffects.Field.TrickRoom) > 0) { // :880
                battle.field.effects.set(PBEffects.Field.TrickRoom, 0);   // :881
                battle.display(user.pbThis() + "使空间恢复正常了！"); // :882
            } else {
                battle.field.effects.set(PBEffects.Field.TrickRoom, 5);   // :884
                battle.display(user.pbThis() + "扭曲了时空！");    // :885
            }
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} (:889-892) - 登记: 动画播放器未建模. */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets, int hitNum,
                                    boolean showAnimation) {
            if (user.battle.field.effects
                    .intVal(PBEffects.Field.TrickRoom) > 0) {                // :890 No animation
                return;
            }
            super.pbShowAnimation(move, id, user, targets, hitNum, showAnimation);   // :891
        }
    }

    /** {@code class PokeBattle_Move_120 < PokeBattle_Move} (:900-932); Ally Switch. */
    public static class PokeBattle_Move_120 extends MoveEffectBase {

        /** {@code @idxAlly} - set by {@code pbMoveFailed?} and read by {@code pbEffectGeneral} (:904/:924). */
        private int idxAlly;

        /** {@code pbMoveFailed?(user,targets)} (:901-919). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = user.battle;
            int numTargets = 0;                                              // :902
            if (!user.effects.truthy(PBEffects.Battler.Commander)) {   // :903
                idxAlly = -1;                                                // :904
                int idxUserOwner = pbGetOwnerIndexFromBattlerIndex(battle, user.index); // :905
                for (Battler b : user.allAllies()) {           // :906 user.eachAlly
                    if (pbGetOwnerIndexFromBattlerIndex(battle, b.index) != idxUserOwner) { // :907
                        continue;
                    }
                    if (!b.near(user)) {                                     // :908
                        continue;
                    }
                    if (b.effects.truthy(PBEffects.Battler.Commander)) { // :909
                        continue;
                    }
                    numTargets += 1;                                         // :910
                    idxAlly = b.index;                                       // :911
                }
            }
            if (numTargets != 1) {                                           // :914
                battle.display("但是它失败了！");                // :915
                return true;                                                 // :916
            }
            return false;                                                    // :918
        }

        /** {@code pbEffectGeneral(user)} (:921-931). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            Battle battle = user.battle;
            int idxA = user.index;                                           // :922
            int idxB = idxAlly;                                              // :923
            user.effects.set(PBEffects.Battler.SwitchedAlly, idxAlly);   // :924
            if (pbSwapBattlers(battle, idxA, idxB)) {                        // :925
                battle.display(battle.battlerAt(idxB).pbThis()
                        + "和" + battle.battlerAt(idxA).pbThis(true) + "交换了位置！"); // :926-927
                if (Battle.NEWEST_BATTLE_MECHANICS) {                        // :928
                    pbActivateHealingWish(battle, battle.battlerAt(idxA));
                }
                if (Battle.NEWEST_BATTLE_MECHANICS) {                        // :929
                    pbActivateHealingWish(battle, battle.battlerAt(idxB));
                }
            }
        }
    }

    /** {@code class PokeBattle_Move_121 < PokeBattle_Move} (:939-946); Foul Play. */
    public static class PokeBattle_Move_121 extends MoveEffectBase {

        /** {@code pbGetAttackStats(user,target)} (:940-945). */
        @Override
        public MoveStats pbGetAttackStats(BattleMove move, Battler user, Battler target) {
            if (specialMove(move, null)) {                                   // :941
                return new MoveStats(target.spAtk(),
                        target.stage(PBStats.SPATK) + 6);   // :942
            }
            return new MoveStats(target.attack(),
                    target.stage(PBStats.ATTACK) + 6);      // :944
        }
    }

    /** {@code class PokeBattle_Move_122 < PokeBattle_Move} (:954-958); Psyshock / Psystrike / Secret Sword. */
    public static class PokeBattle_Move_122 extends MoveEffectBase {

        /** {@code pbGetDefenseStats(user,target)} (:955-957). */
        @Override
        public MoveStats pbGetDefenseStats(BattleMove move, Battler user, Battler target) {
            return new MoveStats(target.defense(),
                    target.stage(PBStats.DEFENSE) + 6);     // :956
        }
    }

    /** {@code class PokeBattle_Move_123 < PokeBattle_Move} (:965-981); Synchronoise. */
    public static class PokeBattle_Move_123 extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:966-980). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            Array<String> userTypes = user.pbTypes(true);                     // :967
            Array<String> targetTypes = target.pbTypes(true);                 // :968
            boolean sharesType = false;                                       // :969
            for (String t : userTypes) {                                      // :970
                if (!targetTypes.contains(t, false)) {                        // :971
                    continue;
                }
                sharesType = true;                                            // :972
                break;                                                        // :973
            }
            if (!sharesType) {                                                // :975
                user.battle.display(target.pbThis() + "没有受到影响！");           // :976
                return true;                                                  // :977
            }
            return false;                                                     // :979
        }
    }

    /** {@code class PokeBattle_Move_124 < PokeBattle_Move} (:989-1004); Wonder Room. */
    public static class PokeBattle_Move_124 extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:990-998). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            Battle battle = user.battle;
            if (battle.field.effects.intVal(PBEffects.Field.WonderRoom) > 0) { // :991
                battle.field.effects.set(PBEffects.Field.WonderRoom, 0);   // :992
                battle.display("奇妙空间消失了！\n防御与特防恢复了正常！");       // :993
            } else {
                battle.field.effects.set(PBEffects.Field.WonderRoom, 5);   // :995
                battle.display("制造出了防御与特防互换的奇妙空间！");           // :996
            }
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} (:1000-1003) - 登记: 动画播放器未建模. */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets, int hitNum,
                                    boolean showAnimation) {
            if (user.battle.field.effects
                    .intVal(PBEffects.Field.WonderRoom) > 0) {               // :1001 No animation
                return;
            }
            super.pbShowAnimation(move, id, user, targets, hitNum, showAnimation);   // :1002
        }
    }

    /** {@code class PokeBattle_Move_125 < PokeBattle_Move} (:1011-1025); Last Resort. */
    public static class PokeBattle_Move_125 extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:1012-1024). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            boolean hasThisMove = false;                                     // :1013
            boolean hasOtherMoves = false;
            boolean hasUnusedMoves = false;
            for (BattleMove m : eachMove(user)) {                            // :1014 user.eachMove
                if (m.id() == move.id()) {                                   // :1015 m.id==@id
                    hasThisMove = true;
                }
                if (m.id() != move.id()) {                                   // :1016
                    hasOtherMoves = true;
                }
                // :1017 m.id!=@id && !user.movesUsed.include?(m.id) —— movesUsed 存内部名
                if (m.id() != move.id() && !user.movesUsed.contains(m.internalName(), false)) {
                    hasUnusedMoves = true;
                }
            }
            if (!hasThisMove || !hasOtherMoves || hasUnusedMoves) {          // :1019
                user.battle.display("但是失败了！");   // :1020
                return true;                                                 // :1021
            }
            return false;                                                    // :1023
        }
    }

    /** {@code class PokeBattle_Move_133 < PokeBattle_Move} (:1038-1053); Hold Hands. */
    public static class PokeBattle_Move_133 extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:1039). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :1039
        }

        /** {@code pbMoveFailed?(user,targets)} (:1041-1052). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            boolean hasAlly = false;                                         // :1042
            for (Battler ignored : user.allAllies()) {         // :1043 user.eachAlly
                hasAlly = true;                                              // :1044
                break;                                                       // :1045
            }
            if (!hasAlly) {                                                  // :1047
                user.battle.display("但是失败了！");   // :1048
                return true;                                                 // :1049
            }
            return false;                                                    // :1051
        }
    }

    /** {@code class PokeBattle_Move_134 < PokeBattle_Move} (:1060-1068); Celebrate. */
    public static class PokeBattle_Move_134 extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:1061-1067). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            Battle battle = user.battle;
            if (battle.wildBattle() && user.opposes(0)) {                    // :1062 @battle.wildBattle? && user.opposes?
                battle.display("来自" + user.pbThis(true) + "的祝贺！");    // :1063
            } else {
                battle.display("祝贺" + battle.pbGetOwnerName(user.index) + "！");       // :1065
            }
        }
    }

    /** {@code class PokeBattle_Move_135 < PokeBattle_FreezeMove} (:1075-1080); Freeze-Dry. */
    public static class PokeBattle_Move_135 extends MoveEffectsGeneric.PokeBattle_FreezeMove {

        /** {@code pbCalcTypeModSingle(moveType,defType,user,target)} (:1076-1079). */
        @Override
        public int pbCalcTypeModSingle(BattleMove move, String moveType, String defType, Battler user,
                                       Battler target) {
            if ("WATER".equals(defType)) {                                   // :1077
                return PBTypeEffectiveness.SUPER_EFFECTIVE_ONE;              // :1077
            }
            return super.pbCalcTypeModSingle(move, moveType, defType, user, target);   // :1078
        }
    }

    /**
     * {@code class PokeBattle_Move_136 < PokeBattle_Move_02F} (:1087-1091); Diamond Storm.
     *
     * <p>The Ruby body is only a comment: since Gen 7 the move is identical to
     * function code 02F, so the class is an empty subclass. Its parent lives in
     * {@code MoveEffects_000_07F.java} (another file of this batch).</p>
     */
    public static class PokeBattle_Move_136 extends MoveEffects_000_07F.PokeBattle_Move_02F {
    }

    /** {@code class PokeBattle_Move_137 < PokeBattle_Move} (:1107-1148); Magnetic Flux. */
    public static class PokeBattle_Move_137 extends MoveEffectBase {

        /** {@code @validTargets} - filled by {@code pbMoveFailed?}, read by the other two hooks (:1110/:1126/:1146). */
        private final Array<Battler> validTargets = new Array<>();

        /** {@code ignoresSubstitute?(user)} (:1107). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :1107
        }

        /** {@code pbMoveFailed?(user,targets)} (:1109-1122). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = user.battle;
            validTargets.clear();                                            // :1110 @validTargets = []
            for (Battler b : battle.eachSameSideBattler(user.index)) {        // :1111 @battle.eachSameSideBattler(user)
                if (!b.hasActiveAbility(new String[] {"MINUS", "PLUS"})) {    // :1112
                    continue;
                }
                if (!b.pbCanRaiseStatStage(PBStats.DEFENSE, user, move, false)   // :1113
                        && !b.pbCanRaiseStatStage(PBStats.SPDEF, user, move, false)) { // :1114
                    continue;
                }
                validTargets.add(b);                                         // :1115
            }
            if (validTargets.size == 0) {                                    // :1117
                battle.display("但是失败了！");                  // :1118
                return true;                                                 // :1119
            }
            return false;                                                    // :1121
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:1124-1129). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            for (Battler b : validTargets) {                                 // :1125 @validTargets.any? { |b| b.index==target.index }
                if (b.index == target.index) {
                    return false;
                }
            }
            if (!target.hasActiveAbility(new String[] {"MINUS", "PLUS"})) {   // :1126
                return true;
            }
            user.battle.display(target.pbThis() + "的能力已经无法再提升了！");       // :1127
            return true;                                                     // :1128
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1132-1142). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            boolean showAnim = true;                                         // :1133
            if (target.pbCanRaiseStatStage(PBStats.DEFENSE, user, move, false)) { // :1134
                if (target.pbRaiseStatStage(PBStats.DEFENSE, 1, user, showAnim, false)) { // :1135
                    showAnim = false;                                        // :1136
                }
            }
            if (target.pbCanRaiseStatStage(PBStats.SPDEF, user, move, false)) { // :1139
                target.pbRaiseStatStage(PBStats.SPDEF, 1, user, showAnim, false); // :1140
            }
        }

        /** {@code pbEffectGeneral(user)} (:1144-1147). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            if (pbTarget(move, user) == PBTargets.UserAndAllies) {           // :1145
                return;
            }
            for (Battler b : validTargets) {                                 // :1146 @validTargets.each { |b| pbEffectAgainstTarget(user,b) }
                pbEffectAgainstTarget(move, user, b);
            }
        }
    }

    /** {@code class PokeBattle_Move_138 < PokeBattle_Move} (:1155-1166); Aromatic Mist. */
    public static class PokeBattle_Move_138 extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:1156). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :1156
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:1158-1161). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (!target.pbCanRaiseStatStage(PBStats.SPDEF, user, move, true)) { // :1159
                return true;
            }
            return false;                                                    // :1160
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1163-1165). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.pbRaiseStatStage(PBStats.SPDEF, 1, user, true, false);   // :1164
        }
    }

    /** {@code class PokeBattle_Move_139 < PokeBattle_TargetStatDownMove} (:1173-1180); Play Nice. */
    public static class PokeBattle_Move_139 extends MoveEffectsGeneric.PokeBattle_TargetStatDownMove {

        /** {@code ignoresSubstitute?(user)} (:1174). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :1174
        }

        /** {@code initialize} (:1176-1179). */
        public PokeBattle_Move_139() {
            statDown = new int[] {PBStats.ATTACK, 1};                        // :1178
        }
    }

    // ==================================================================
    // 13A-149
    // ==================================================================

    /** {@code class PokeBattle_Move_13A < PokeBattle_TargetMultiStatDownMove} (:1190-1199); Noble Roar. */
    public static class PokeBattle_Move_13A extends MoveEffectsGeneric.PokeBattle_TargetMultiStatDownMove {

        /** {@code ignoresSubstitute?(user)} (:1191). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :1191
        }

        /** {@code initialize} (:1193-1196). */
        public PokeBattle_Move_13A() {
            statDown = new int[] {PBStats.ATTACK, 1, PBStats.SPATK, 1};      // :1195
        }

        /** {@code pbAccuracyCheck(user,target); return true; end} (:1198). */
        @Override
        public boolean pbAccuracyCheck(BattleMove move, Battler user, Battler target) {
            return true;                                                     // :1198
        }
    }

    /** {@code class PokeBattle_Move_13B < PokeBattle_StatDownMove} (:1207-1239); Hyperspace Fury. */
    public static class PokeBattle_Move_13B extends MoveEffectsGeneric.PokeBattle_StatDownMove {

        /** {@code ignoresSubstitute?(user)} (:1208). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :1208
        }

        /** {@code initialize} (:1210-1213). */
        public PokeBattle_Move_13B() {
            statDown = new int[] {PBStats.DEFENSE, 1};                       // :1212
        }

        /** {@code pbMoveFailed?(user,targets)} (:1215-1224). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.isSpecies("HOOPA")) {                                  // :1216
                user.battle.display("但是"
                        + user.pbThis(true) + "不能使用这个招式！"); // :1217
                return true;                                                 // :1218
            } else if (user.form() != 1) {                                   // :1219
                user.battle.display("但是"
                        + user.pbThis(true) + "使用失败了！");       // :1220
                return true;                                                 // :1221
            }
            return false;                                                    // :1223
        }

        /** {@code pbAccuracyCheck(user,target); return true; end} (:1226). */
        @Override
        public boolean pbAccuracyCheck(BattleMove move, Battler user, Battler target) {
            return true;                                                     // :1226
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1228-1238). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.BurningBulwark, false);   // :1229
            target.effects.set(PBEffects.Battler.BanefulBunker, false);    // :1230
            target.effects.set(PBEffects.Battler.KingsShield, false);      // :1231
            target.effects.set(PBEffects.Battler.Protect, false);          // :1232
            target.effects.set(PBEffects.Battler.SpikyShield, false);      // :1233
            target.pbOwnSide().effects.set(PBEffects.Side.CraftyShield, false);        // :1234
            target.pbOwnSide().effects.set(PBEffects.Side.MatBlock, false);            // :1235
            target.pbOwnSide().effects.set(PBEffects.Side.QuickGuard, false);          // :1236
            target.pbOwnSide().effects.set(PBEffects.Side.WideGuard, false);           // :1237
        }
    }

    /** {@code class PokeBattle_Move_13C < PokeBattle_TargetStatDownMove} (:1246-1255); Confide. */
    public static class PokeBattle_Move_13C extends MoveEffectsGeneric.PokeBattle_TargetStatDownMove {

        /** {@code ignoresSubstitute?(user)} (:1247). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :1247
        }

        /** {@code initialize} (:1249-1252). */
        public PokeBattle_Move_13C() {
            statDown = new int[] {PBStats.SPATK, 1};                         // :1251
        }

        /** {@code pbAccuracyCheck(user,target); return true; end} (:1254). */
        @Override
        public boolean pbAccuracyCheck(BattleMove move, Battler user, Battler target) {
            return true;                                                     // :1254
        }
    }

    /** {@code class PokeBattle_Move_13D < PokeBattle_TargetStatDownMove} (:1262-1267); Eerie Impulse. */
    public static class PokeBattle_Move_13D extends MoveEffectsGeneric.PokeBattle_TargetStatDownMove {

        /** {@code initialize} (:1263-1266). */
        public PokeBattle_Move_13D() {
            statDown = new int[] {PBStats.SPATK, 2};                         // :1265
        }
    }

    /** {@code class PokeBattle_Move_13E < PokeBattle_Move} (:1275-1311); Rototiller. */
    public static class PokeBattle_Move_13E extends MoveEffectBase {

        /** {@code @validTargets} - indices filled by {@code pbMoveFailed?}, read by the other two hooks (:1277/:1293). */
        private final com.badlogic.gdx.utils.IntArray validTargets = new com.badlogic.gdx.utils.IntArray();

        /** {@code pbMoveFailed?(user,targets)} (:1276-1290). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = user.battle;
            validTargets.clear();                                            // :1277 @validTargets = []
            for (Battler b : battle.eachBattler()) {                          // :1278 @battle.eachBattler
                if (!b.pbHasType("GRASS")) {                                 // :1279
                    continue;
                }
                if (b.airborne() || b.semiInvulnerable()) {                   // :1280
                    continue;
                }
                if (!b.pbCanRaiseStatStage(PBStats.ATTACK, user, move, false)   // :1281
                        && !b.pbCanRaiseStatStage(PBStats.SPATK, user, move, false)) { // :1282
                    continue;
                }
                validTargets.add(b.index);                                   // :1283
            }
            if (validTargets.size == 0) {                                    // :1285
                battle.display("但是失败了！");                  // :1286
                return true;                                                 // :1287
            }
            return false;                                                    // :1289
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:1292-1298). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (validTargets.contains(target.index)) {                       // :1293 @validTargets.include?(target.index)
                return false;
            }
            if (!target.pbHasType("GRASS")) {                                // :1294
                return true;
            }
            if (target.airborne() || target.semiInvulnerable()) {            // :1295
                return true;
            }
            user.battle.display(target.pbThis() + "的能力已经无法再提升了！");       // :1296
            return true;                                                     // :1297
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1300-1310). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            boolean showAnim = true;                                         // :1301
            if (target.pbCanRaiseStatStage(PBStats.ATTACK, user, move, false)) { // :1302
                if (target.pbRaiseStatStage(PBStats.ATTACK, 1, user, showAnim, false)) { // :1303
                    showAnim = false;                                        // :1304
                }
            }
            if (target.pbCanRaiseStatStage(PBStats.SPATK, user, move, false)) { // :1307
                target.pbRaiseStatStage(PBStats.SPATK, 1, user, showAnim, false); // :1308
            }
        }
    }

    /** {@code class PokeBattle_Move_13F < PokeBattle_Move} (:1319-1344); Flower Shield. */
    public static class PokeBattle_Move_13F extends MoveEffectBase {

        /** {@code @validTargets} (:1321/:1336). */
        private final com.badlogic.gdx.utils.IntArray validTargets = new com.badlogic.gdx.utils.IntArray();

        /** {@code pbMoveFailed?(user,targets)} (:1320-1333). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = user.battle;
            validTargets.clear();                                            // :1321
            for (Battler b : battle.eachBattler()) {                          // :1322
                if (!b.pbHasType("GRASS")) {                                 // :1323
                    continue;
                }
                if (b.semiInvulnerable()) {                                  // :1324
                    continue;
                }
                if (!b.pbCanRaiseStatStage(PBStats.DEFENSE, user, move, false)) { // :1325
                    continue;
                }
                validTargets.add(b.index);                                   // :1326
            }
            if (validTargets.size == 0) {                                    // :1328
                battle.display("但是失败了！");                  // :1329
                return true;                                                 // :1330
            }
            return false;                                                    // :1332
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:1335-1339). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (validTargets.contains(target.index)) {                       // :1336
                return false;
            }
            if (!target.pbHasType("GRASS") || target.semiInvulnerable()) {   // :1337
                return true;
            }
            return !target.pbCanRaiseStatStage(PBStats.DEFENSE, user, move, true); // :1338
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1341-1343). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.pbRaiseStatStage(PBStats.DEFENSE, 1, user, true, false);   // :1342
        }
    }

    /** {@code class PokeBattle_Move_140 < PokeBattle_Move} (:1352-1380); Venom Drench. */
    public static class PokeBattle_Move_140 extends MoveEffectBase {

        /** {@code @validTargets} (:1354/:1371). */
        private final com.badlogic.gdx.utils.IntArray validTargets = new com.badlogic.gdx.utils.IntArray();

        /** {@code pbMoveFailed?(user,targets)} (:1353-1368). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = user.battle;
            validTargets.clear();                                            // :1354
            for (Battler b : targets) {                                      // :1355 targets.each
                if (b == null || b.fainted()) {                              // :1356
                    continue;
                }
                if (!b.poisoned()) {                                         // :1357
                    continue;
                }
                if (!b.pbCanLowerStatStage(PBStats.ATTACK, user, move, false)   // :1358
                        && !b.pbCanLowerStatStage(PBStats.SPATK, user, move, false)  // :1359
                        && !b.pbCanLowerStatStage(PBStats.SPEED, user, move, false)) { // :1360
                    continue;
                }
                validTargets.add(b.index);                                   // :1361
            }
            if (validTargets.size == 0) {                                    // :1363
                battle.display("但是失败了！");                  // :1364
                return true;                                                 // :1365
            }
            return false;                                                    // :1367
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1370-1379). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (!validTargets.contains(target.index)) {                      // :1371
                return;
            }
            boolean showAnim = true;                                         // :1372
            for (int s : new int[] {PBStats.ATTACK, PBStats.SPATK, PBStats.SPEED}) { // :1373
                if (!target.pbCanLowerStatStage(s, user, move, false)) {  // :1374
                    continue;
                }
                if (target.pbLowerStatStage(s, 1, user, showAnim, false, false)) { // :1375
                    showAnim = false;                                        // :1376
                }
            }
        }
    }

    /** {@code class PokeBattle_Move_141 < PokeBattle_Move} (:1387-1414); Topsy-Turvy. */
    public static class PokeBattle_Move_141 extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:1388-1400). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            boolean failed = true;                                           // :1389
            for (int s : PBStats.EACH_BATTLE_STAT) {                         // :1390 PBStats.eachBattleStat
                if (target.stage(s) == 0) {                                  // :1391
                    continue;
                }
                failed = false;                                              // :1392
                break;                                                       // :1393
            }
            if (failed) {                                                    // :1395
                user.battle.display("但是失败了！");   // :1396
                return true;                                                 // :1397
            }
            return false;                                                    // :1399
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1402-1413). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            for (int s : PBStats.EACH_BATTLE_STAT) {                         // :1403
                if (target.stage(s) > 0) {                                   // :1404
                    target.statsLoweredThisRound = true;                     // :1405
                    target.statsDropped = true;                              // :1406
                } else if (target.stage(s) < 0) {                            // :1407
                    target.statsRaisedThisRound = true;                      // :1408
                }
                target.setStage(s, target.stage(s) * -1);                    // :1410 target.stages[s] *= -1
            }
            user.battle.display(target.pbThis() + "的能力等级反转了！");              // :1412
        }
    }

    /** {@code class PokeBattle_Move_142 < PokeBattle_Move} (:1422-1437); Trick-or-Treat. */
    public static class PokeBattle_Move_142 extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:1423-1429). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            // :1424 !hasConst?(PBTypes,:GHOST) || target.pbHasType?(:GHOST) || !target.canChangeType?
            //      hasConst?(PBTypes,:GHOST) 在本工程恒真（PBS/types.txt [7] GHOST，task-7 已核），
            //      按 Lead 对「可证值」的口径内联。
            if (target.pbHasType("GHOST") || !canChangeType(target)) {
                user.battle.display("但是失败了！");   // :1425
                return true;                                                 // :1426
            }
            return false;                                                    // :1428
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1431-1436). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            String ghostType = "GHOST";                                      // :1432 getConst(PBTypes,:GHOST)
            target.effects.set(PBEffects.Battler.Type3, ghostType);   // :1433
            String typeName = PBTypes.getName(user.battle.pbs(), ghostType);   // :1434
            user.battle.display(target.pbThis() + "变为了" + typeName + "属性！");   // :1435
        }
    }

    /** {@code class PokeBattle_Move_143 < PokeBattle_Move} (:1444-1459); Forest's Curse. */
    public static class PokeBattle_Move_143 extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:1445-1451). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            // :1446 hasConst?(PBTypes,:GRASS) 恒真（types.txt [12] GRASS），同上内联。
            if (target.pbHasType("GRASS") || !canChangeType(target)) {
                user.battle.display("但是失败了！");   // :1447
                return true;                                                 // :1448
            }
            return false;                                                    // :1450
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1453-1458). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            String grassType = "GRASS";                                      // :1454 getConst(PBTypes,:GRASS)
            target.effects.set(PBEffects.Battler.Type3, grassType);   // :1455
            String typeName = PBTypes.getName(user.battle.pbs(), grassType);   // :1456
            user.battle.display(target.pbThis() + "变为了" + typeName + "属性！");   // :1457
        }
    }

    /** {@code class PokeBattle_Move_144 < PokeBattle_Move} (:1468-1483); Flying Press. */
    public static class PokeBattle_Move_144 extends MoveEffectBase {

        /** {@code tramplesMinimize?(param=1)} (:1469-1473). */
        @Override
        public boolean tramplesMinimize(BattleMove move, int param) {
            if (param == 1 && Battle.NEWEST_BATTLE_MECHANICS) {               // :1470 Perfect accuracy
                return true;
            }
            if (param == 2) {                                                // :1471 Double damage
                return true;
            }
            return super.tramplesMinimize(move, param);                      // :1472
        }

        /** {@code pbCalcTypeModSingle(moveType,defType,user,target)} (:1475-1482). */
        @Override
        public int pbCalcTypeModSingle(BattleMove move, String moveType, String defType, Battler user,
                                       Battler target) {
            int ret = super.pbCalcTypeModSingle(move, moveType, defType, user, target);   // :1476
            // :1477 hasConst?(PBTypes,:FLYING) 恒真（types.txt [2] FLYING），同上内联。
            int flyingEff = PBTypes.getEffectiveness(user.battle.pbs(), "FLYING", defType); // :1478
            // :1479 ret *= flyingEff.to_f/PBTypeEffectiveness::NORMAL_EFFECTIVE_ONE
            //      登记: Ruby 这一行把 ret 变成 Float（flyingEff/2 可以是 0.5），而本运行时
            //      MoveEffect#pbCalcTypeModSingle 的返回类型是 int（task-11 冻结），故此处截断。
            ret = (int) (ret * (flyingEff / (float) PBTypeEffectiveness.NORMAL_EFFECTIVE_ONE));
            return ret;                                                      // :1481
        }
    }

    /** {@code class PokeBattle_Move_145 < PokeBattle_Move} (:1490-1504); Electrify. */
    public static class PokeBattle_Move_145 extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:1491-1498). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.effects.truthy(PBEffects.Battler.Electrify)) {   // :1492
                user.battle.display("但是失败了！");      // :1493
                return true;                                                     // :1494
            }
            if (pbMoveFailedTargetAlreadyMoved(move, target)) {              // :1496
                return true;
            }
            return false;                                                    // :1497
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1500-1503). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.Electrify, true);     // :1501
            user.battle.display(target.pbThis() + "的招式带电了！");                 // :1502
        }
    }

    /** {@code class PokeBattle_Move_146 < PokeBattle_Move} (:1512-1528); Ion Deluge / Plasma Fists. */
    public static class PokeBattle_Move_146 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:1513-1521). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = user.battle;
            if (damagingMove(move)) {                                        // :1514
                return false;
            }
            if (battle.field.effects.truthy(PBEffects.Field.IonDeluge)) { // :1515
                battle.display("但是失败了！");                  // :1516
                return true;                                                 // :1517
            }
            if (pbMoveFailedLastInRound(move, user)) {                       // :1519
                return true;
            }
            return false;                                                    // :1520
        }

        /** {@code pbEffectGeneral(user)} (:1523-1527). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            Battle battle = user.battle;
            if (battle.field.effects.truthy(PBEffects.Field.IonDeluge)) { // :1524
                return;
            }
            battle.field.effects.set(PBEffects.Field.IonDeluge, true);   // :1525
            battle.display("离子风暴正在肆虐！");                     // :1526
        }
    }

    /** {@code class PokeBattle_Move_147 < PokeBattle_Move} (:1535-1549); Hyperspace Hole. */
    public static class PokeBattle_Move_147 extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:1536). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :1536
        }

        /** {@code pbAccuracyCheck(user,target); return true; end} (:1537). */
        @Override
        public boolean pbAccuracyCheck(BattleMove move, Battler user, Battler target) {
            return true;                                                     // :1537
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1539-1548). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.BanefulBunker, false);   // :1540
            target.effects.set(PBEffects.Battler.KingsShield, false);     // :1541
            target.effects.set(PBEffects.Battler.Protect, false);         // :1542
            target.effects.set(PBEffects.Battler.SpikyShield, false);     // :1543
            target.pbOwnSide().effects.set(PBEffects.Side.CraftyShield, false);       // :1544
            target.pbOwnSide().effects.set(PBEffects.Side.MatBlock, false);           // :1545
            target.pbOwnSide().effects.set(PBEffects.Side.QuickGuard, false);         // :1546
            target.pbOwnSide().effects.set(PBEffects.Side.WideGuard, false);          // :1547
        }
    }

    /** {@code class PokeBattle_Move_148 < PokeBattle_Move} (:1557-1572); Powder. */
    public static class PokeBattle_Move_148 extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:1558). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :1558
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:1560-1566). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.effects.truthy(PBEffects.Battler.Powder)) {   // :1561
                user.battle.display("但是失败了！");    // :1562
                return true;                                                   // :1563
            }
            return false;                                                      // :1565
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1568-1571). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.Powder, true);      // :1569
            // :1570 @battle.pbDisplay(_INTL("{1}被粉尘包裹着！",user.pbThis)) —— 原文用 user.pbThis，照抄
            user.battle.display(user.pbThis() + "被粉尘包裹着！");
        }
    }

    // ==================================================================
    // 15A + 163（163 优先落盘：MoveEffects_180_1FF 的
    // `1EC extends MoveEffects_100_17F.PokeBattle_Move_163` 依赖它）
    // ==================================================================

    /**
     * {@code class PokeBattle_Move_15A < PokeBattle_Move} (:1921-1927); Sparkling Aria.
     *
     * <p>The body below is the {@code Arceus} override
     * ({@code Arceus:418-424}), which reopens the class and replaces the
     * same-named {@code pbAdditionalEffect}: the main file only checks
     * {@code BURN}, the {@code Arceus} version checks {@code BURN} <b>or</b>
     * {@code FROSTBITE}. Per the Lead's "reopened class = later section wins"
     * ruling the override body is used.</p>
     */
    public static class PokeBattle_Move_15A extends MoveEffectBase {

        /** {@code pbAdditionalEffect(user,target)} (main :1922-1926; {@code Arceus:419-423} override). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.fainted() || target.damageState.substitute) {   // :420
                return;
            }
            int status = PBStatuses.idOf(target.status);                     // :421 target.status
            if (status != PBStatuses.BURN && status != PBStatuses.FROSTBITE) {   // :421
                return;
            }
            target.pbCureStatus(true);                                       // :422 (showMessages=true)
        }
    }

    /** {@code class PokeBattle_Move_163 < PokeBattle_Move} (:2159-2169); Photon Geyser / Moongeist Beam family. */
    public static class PokeBattle_Move_163 extends MoveEffectBase {

        /** {@code pbOnStartUse(user,targets)} (:2160-2164). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = user.battle;
            if (battle.moldBreaker                                          // :2161
                    && targets.size > 0 && targets.get(0).hasActiveItem("ABILITYSHIELD", false)) {
                battle.moldBreaker = false;                                  // :2162
            }
        }

        /** {@code pbChangeUsageCounters(user,specialUsage)} (:2165-2168). */
        @Override
        public void pbChangeUsageCounters(BattleMove move, Battler user, boolean specialUsage) {
            super.pbChangeUsageCounters(move, user, specialUsage);            // :2166
            if (!specialUsage) {                                             // :2167
                user.battle.moldBreaker = true;                  // :2167
            }
        }
    }

    // ==================================================================
    // 149-152
    // ==================================================================

    /** {@code class PokeBattle_Move_149 < PokeBattle_Move} (:1579-1593); Mat Block. */
    public static class PokeBattle_Move_149 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:1580-1587). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.turnCount > 1 || user.lastRoundMoved >= 0) {             // :1581
                user.battle.display("但是失败了！");    // :1582
                return true;                                                   // :1583
            }
            if (pbMoveFailedLastInRound(move, user)) {                       // :1585
                return true;
            }
            return false;                                                    // :1586
        }

        /** {@code pbEffectGeneral(user)} (:1589-1592). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.pbOwnSide().effects.set(PBEffects.Side.MatBlock, true);          // :1590
            user.battle.display(user.pbThis() + "举起榻榻米挡下了攻击！");            // :1591
        }
    }

    /** {@code class PokeBattle_Move_14A < PokeBattle_Move} (:1600-1614); Crafty Shield. */
    public static class PokeBattle_Move_14A extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:1601-1608). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.pbOwnSide().effects.truthy(PBEffects.Side.CraftyShield)) {   // :1602
                user.battle.display("但是失败了！");      // :1603
                return true;                                                     // :1604
            }
            if (pbMoveFailedLastInRound(move, user)) {                       // :1606
                return true;
            }
            return false;                                                    // :1607
        }

        /** {@code pbEffectGeneral(user)} (:1610-1613). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.pbOwnSide().effects.set(PBEffects.Side.CraftyShield, true);       // :1611
            user.battle.display("戏法防守保护了" + user.pbTeam(true) + "！");                   // :1612
        }
    }

    /** {@code class PokeBattle_Move_14B < PokeBattle_ProtectMove} (:1622-1627); King's Shield. */
    public static class PokeBattle_Move_14B extends MoveEffectsGeneric.PokeBattle_ProtectMove {
        /** {@code initialize} (:1623-1626). */
        public PokeBattle_Move_14B() {
            effect = PBEffects.Battler.KingsShield;                          // :1625
        }
    }

    /** {@code class PokeBattle_Move_14C < PokeBattle_ProtectMove} (:1635-1640); Spiky Shield. */
    public static class PokeBattle_Move_14C extends MoveEffectsGeneric.PokeBattle_ProtectMove {
        /** {@code initialize} (:1636-1639). */
        public PokeBattle_Move_14C() {
            effect = PBEffects.Battler.SpikyShield;                          // :1638
        }
    }

    /**
     * {@code class PokeBattle_Move_14D < PokeBattle_Move_0CD} (:1648-1650); Phantom Force.
     *
     * <p>The Ruby body is only a NOTE: the move is identical to function code 0CD
     * (Shadow Force), so the class is an empty subclass. Its parent lives in
     * {@code MoveEffects_0B0_0D4.java} (another file of this batch).</p>
     */
    public static class PokeBattle_Move_14D extends MoveEffects_0B0_0D4.PokeBattle_Move_0CD {
    }

    /** {@code class PokeBattle_Move_14E < PokeBattle_TwoTurnMove} (:1658-1684); Geomancy. */
    public static class PokeBattle_Move_14E extends MoveEffectsGeneric.PokeBattle_TwoTurnMove {

        /** {@code pbMoveFailed?(user,targets)} (:1659-1668). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.effects.intVal(PBEffects.Battler.TwoTurnAttack) > 0) {   // :1660 Charging turn
                return false;
            }
            if (!user.pbCanRaiseStatStage(PBStats.SPATK, user, move, false)   // :1661
                    && !user.pbCanRaiseStatStage(PBStats.SPDEF, user, move, false)  // :1662
                    && !user.pbCanRaiseStatStage(PBStats.SPEED, user, move, false)) { // :1663
                user.battle.display(user.pbThis() + "的能力不能再提高了！");          // :1664
                return true;                                                     // :1665
            }
            return false;                                                        // :1667
        }

        /** {@code pbChargingTurnMessage(user,targets)} (:1670-1672). */
        @Override
        public void pbChargingTurnMessage(BattleMove move, Battler user, Array<Battler> targets) {
            user.battle.display(user.pbThis() + "正在吸收能量！");                   // :1671
        }

        /** {@code pbEffectGeneral(user)} (:1674-1683). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            if (!damagingTurn) {                                             // :1675 @damagingTurn
                return;
            }
            boolean showAnim = true;                                         // :1676
            for (int s : new int[] {PBStats.SPATK, PBStats.SPDEF, PBStats.SPEED}) { // :1677
                if (!user.pbCanRaiseStatStage(s, user, move, false)) {   // :1678
                    continue;
                }
                if (user.pbRaiseStatStage(s, 2, user, showAnim, false)) { // :1679
                    showAnim = false;                                        // :1680
                }
            }
        }
    }

    /** {@code class PokeBattle_Move_14F < PokeBattle_Move} (:1691-1699); Draining Kiss / Oblivion Wing. */
    public static class PokeBattle_Move_14F extends MoveEffectBase {

        /** {@code healingMove?} (:1692). */
        @Override
        public boolean healingMove(BattleMove move) {
            return Battle.NEWEST_BATTLE_MECHANICS;                           // :1692
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1694-1698). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.damageState.hpLost <= 0) {          // :1695
                return;
            }
            int hpGain = Math.round(target.damageState.hpLost * 0.75f); // :1696
            pbRecoverHPFromDrain(user, hpGain, target);                      // :1697
        }
    }

    /** {@code class PokeBattle_Move_150 < PokeBattle_Move} (:1707-1713); Fell Stinger. */
    public static class PokeBattle_Move_150 extends MoveEffectBase {

        /** {@code pbEffectAfterAllHits(user,target)} (:1708-1712). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (!target.damageState.fainted) {             // :1709
                return;
            }
            if (!user.pbCanRaiseStatStage(PBStats.ATTACK, user, move, false)) { // :1710
                return;
            }
            user.pbRaiseStatStage(PBStats.ATTACK, 3, user, true, false);   // :1711
        }
    }

    /** {@code class PokeBattle_Move_151 < PokeBattle_TargetMultiStatDownMove} (:1721-1747); Parting Shot. */
    public static class PokeBattle_Move_151 extends MoveEffectsGeneric.PokeBattle_TargetMultiStatDownMove {

        /** {@code initialize} (:1722-1725). */
        public PokeBattle_Move_151() {
            statDown = new int[] {PBStats.ATTACK, 1, PBStats.SPATK, 1};      // :1724
        }

        /** {@code pbEndOfMoveUsageEffect(user,targets,numHits,switchedBattlers)} (:1727-1746). */
        @Override
        public void pbEndOfMoveUsageEffect(BattleMove move, Battler user, Array<Battler> targets, int numHits,
                                           com.badlogic.gdx.utils.IntArray switchedBattlers) {
            Battle battle = user.battle;
            Battler switcher = user;                                         // :1728
            for (Battler b : targets) {                                      // :1729
                if (switchedBattlers.contains(b.index)) {                    // :1730
                    continue;
                }
                if (b.effects.truthy(PBEffects.Battler.MagicCoat)
                        || b.effects.truthy(PBEffects.Battler.MagicBounce)) { // :1731
                    switcher = b;
                }
            }
            if (switcher.fainted() || numHits == 0) {                        // :1733
                return;
            }
            if (!battle.pbCanChooseNonActive(switcher.index)) {               // :1734
                return;
            }
            battle.display(switcher.pbThis() + "回到了"
                    + battle.pbGetOwnerName(switcher.index) + "身边！");        // :1735-1736
            pbPursuit(battle, switcher.index);                               // :1737
            if (switcher.fainted()) {                                        // :1738
                return;
            }
            int newPkmn = battle.pbGetReplacementPokemonIndex(switcher.index); // :1739 # Owner chooses
            if (newPkmn < 0) {                                               // :1740
                return;
            }
            battle.pbRecallAndReplace(switcher.index, newPkmn);               // :1741
            battle.pbClearChoice(switcher.index);                             // :1742 # Replacement does nothing this round
            if (switcher.index == user.index) {                              // :1743
                battle.moldBreaker = false;
            }
            switchedBattlers.add(switcher.index);                            // :1744
            pbEffectsOnSwitchIn(switcher, true);                             // :1745
        }
    }

    /** {@code class PokeBattle_Move_152 < PokeBattle_Move} (:1755-1768); Fairy Lock. */
    public static class PokeBattle_Move_152 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:1756-1762). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.battle.field.effects
                    .intVal(PBEffects.Field.FairyLock) > 0) {                // :1757
                user.battle.display("但是失败了！");    // :1758
                return true;                                                   // :1759
            }
            return false;                                                      // :1761
        }

        /** {@code pbEffectGeneral(user)} (:1764-1767). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            Battle battle = user.battle;
            battle.field.effects.set(PBEffects.Field.FairyLock, 2);   // :1765
            battle.display("下回合结束前双方均不可交换！");             // :1766
        }
    }

    // ==================================================================
    // 153-159
    // ==================================================================

    /** {@code class PokeBattle_Move_153 < PokeBattle_Move} (:1775-1790); Sticky Web. */
    public static class PokeBattle_Move_153 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:1776-1782). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.pbOpposingSide().effects.truthy(PBEffects.Side.StickyWeb)) {   // :1777
                user.battle.display("但是失败了！");       // :1778
                return true;                                                      // :1779
            }
            return false;                                                         // :1781
        }

        /** {@code pbEffectGeneral(user)} (:1784-1789). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.pbOpposingSide().effects.set(PBEffects.Side.StickyWeb, true);      // :1785
            user.pbOpposingSide().effects.set(PBEffects.Side.StickyWebUser, user.index);   // :1786
            user.battle.display("一张黏网出现在"
                    + user.pbOpposingTeam(true) + "身边！");                       // :1787-1788
        }
    }

    /** {@code class PokeBattle_Move_154 < PokeBattle_Move} (:1799-1811); Electric Terrain. */
    public static class PokeBattle_Move_154 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:1800-1806). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.battle.field.terrain == PBBattleTerrains.Electric) {   // :1801
                user.battle.display("但是失败了！");       // :1802
                return true;                                                      // :1803
            }
            return false;                                                         // :1805
        }

        /** {@code pbEffectGeneral(user)} (:1808-1810) - 登记: pbStartTerrain（演出+场地子系统未建模）. */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            pbStartTerrain(user.battle, user, PBBattleTerrains.Electric, true);   // :1809
        }
    }

    /** {@code class PokeBattle_Move_155 < PokeBattle_Move} (:1820-1832); Grassy Terrain. */
    public static class PokeBattle_Move_155 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:1821-1827). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.battle.field.terrain == PBBattleTerrains.Grassy) {   // :1822
                user.battle.display("但是失败了！");       // :1823
                return true;                                                      // :1824
            }
            return false;                                                         // :1826
        }

        /** {@code pbEffectGeneral(user)} (:1829-1831) - 登记: pbStartTerrain. */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            pbStartTerrain(user.battle, user, PBBattleTerrains.Grassy, true);   // :1830
        }
    }

    /** {@code class PokeBattle_Move_156 < PokeBattle_Move} (:1841-1853); Misty Terrain. */
    public static class PokeBattle_Move_156 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:1842-1848). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.battle.field.terrain == PBBattleTerrains.Misty) {   // :1843
                user.battle.display("但是失败了！");       // :1844
                return true;                                                      // :1845
            }
            return false;                                                         // :1847
        }

        /** {@code pbEffectGeneral(user)} (:1850-1852) - 登记: pbStartTerrain. */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            pbStartTerrain(user.battle, user, PBBattleTerrains.Misty, true);   // :1851
        }
    }

    /** {@code class PokeBattle_Move_157 < PokeBattle_Move} (:1860-1865); Happy Hour. */
    public static class PokeBattle_Move_157 extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:1861-1864). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            Battle battle = user.battle;
            if (!user.opposes(0)) {                                          // :1862 !user.opposes?
                battle.field.effects.set(PBEffects.Field.HappyHour, true);   // :1862
            }
            battle.display("大家都沉浸在欢乐的气氛中！");               // :1863
        }
    }

    /** {@code class PokeBattle_Move_158 < PokeBattle_Move} (:1872-1891); Belch. */
    public static class PokeBattle_Move_158 extends MoveEffectBase {

        /** {@code pbCanChooseMove?(user,commandPhase,showMessages)} (:1873-1882). */
        @Override
        public boolean pbCanChooseMove(BattleMove move, Battler user, boolean commandPhase, boolean showMessages) {
            if (!belched(user)) {                                            // :1874
                if (showMessages) {                                          // :1875
                    String msg = user.pbThis() + "没有吃任何树果\n无法打嗝！";   // :1876
                    if (commandPhase) {                                      // :1877
                        user.battle.displayPaused(msg);
                    } else {
                        user.battle.display(msg);
                    }
                }
                return false;                                                // :1879
            }
            return true;                                                     // :1881
        }

        /** {@code pbMoveFailed?(user,targets)} (:1884-1890). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!belched(user)) {                                            // :1885
                user.battle.display("但是失败了！");    // :1886
                return true;                                                   // :1887
            }
            return false;                                                      // :1889
        }
    }

    /** {@code class PokeBattle_Move_159 < PokeBattle_Move} (:1898-1914); Toxic Thread. */
    public static class PokeBattle_Move_159 extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:1899-1906). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (!target.pbCanPoison(user, false, move)                       // :1900
                    && !target.pbCanLowerStatStage(PBStats.SPEED, user, move, false)) { // :1901
                user.battle.display("但是失败了！");     // :1902
                return true;                                                     // :1903
            }
            return false;                                                        // :1905
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1908-1913). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.pbCanPoison(user, false, move)) {                     // :1909
                target.pbPoison(user, null, false);                          // :1909 target.pbPoison(user)
            }
            if (target.pbCanLowerStatStage(PBStats.SPEED, user, move, false)) {   // :1911
                target.pbLowerStatStage(PBStats.SPEED, 2, user, true, false, false);     // :1912
            }
        }
    }

    // ==================================================================
    // 15B-160
    // ==================================================================

    /** {@code class PokeBattle_Move_15B < PokeBattle_HealingMove} (:1935-1952); Purify. */
    public static class PokeBattle_Move_15B extends MoveEffectsGeneric.PokeBattle_HealingMove {

        /** {@code pbFailsAgainstTarget?(user,target)} (:1936-1942). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (PBStatuses.idOf(target.status) == PBStatuses.NONE) {         // :1937 target.status==PBStatuses::NONE
                user.battle.display("但是失败了！");    // :1938
                return true;                                                   // :1939
            }
            return false;                                                      // :1941
        }

        /** {@code pbHealAmount(user)} (:1944-1946). */
        @Override
        public int pbHealAmount(BattleMove move, Battler user) {
            return Math.round(user.maxHp() / 2.0f);                          // :1945 (user.totalhp/2.0).round
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1948-1951). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.pbCureStatus(true);                                       // :1949 (showMessages=true)
            super.pbEffectAgainstTarget(move, user, target);                 // :1950
        }
    }

    /** {@code class PokeBattle_Move_15C < PokeBattle_Move} (:1967-2008); Gear Up. */
    public static class PokeBattle_Move_15C extends MoveEffectBase {

        /** {@code @validTargets} (:1971/:1986/:2006). */
        private final Array<Battler> validTargets = new Array<>();

        /** {@code ignoresSubstitute?(user)} (:1968). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :1968
        }

        /** {@code pbMoveFailed?(user,targets)} (:1970-1983). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = user.battle;
            validTargets.clear();                                            // :1971
            for (Battler b : battle.eachSameSideBattler(user.index)) {        // :1972 @battle.eachSameSideBattler(user)
                if (!b.hasActiveAbility(new String[] {"MINUS", "PLUS"})) {    // :1973
                    continue;
                }
                if (!b.pbCanRaiseStatStage(PBStats.ATTACK, user, move, false)   // :1974
                        && !b.pbCanRaiseStatStage(PBStats.SPATK, user, move, false)) { // :1975
                    continue;
                }
                validTargets.add(b);                                         // :1976
            }
            if (validTargets.size == 0) {                                    // :1978
                battle.display("但是失败了！");                  // :1979
                return true;                                                 // :1980
            }
            return false;                                                    // :1982
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:1985-1990). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            for (Battler b : validTargets) {                                 // :1986
                if (b.index == target.index) {
                    return false;
                }
            }
            if (!target.hasActiveAbility(new String[] {"MINUS", "PLUS"})) {   // :1987
                return true;
            }
            user.battle.display(target.pbThis() + "的能力已经无法再提升了！");       // :1988
            return true;                                                     // :1989
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1992-2002). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            boolean showAnim = true;                                         // :1993
            if (target.pbCanRaiseStatStage(PBStats.ATTACK, user, move, false)) {   // :1994
                if (target.pbRaiseStatStage(PBStats.ATTACK, 1, user, showAnim, false)) { // :1995
                    showAnim = false;                                        // :1996
                }
            }
            if (target.pbCanRaiseStatStage(PBStats.SPATK, user, move, false)) {   // :1999
                target.pbRaiseStatStage(PBStats.SPATK, 1, user, showAnim, false);  // :2000
            }
        }

        /** {@code pbEffectGeneral(user)} (:2004-2007). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            if (pbTarget(move, user) == PBTargets.UserAndAllies) {           // :2005
                return;
            }
            for (Battler b : validTargets) {                                 // :2006
                pbEffectAgainstTarget(move, user, b);
            }
        }
    }

    /** {@code class PokeBattle_Move_15D < PokeBattle_Move} (:2017-2037); Spectral Thief. */
    public static class PokeBattle_Move_15D extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:2018). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :2018
        }

        /** {@code pbCalcDamage(user,target,numTargets=1)} (:2020-2036). */
        @Override
        public void pbCalcDamage(BattleMove move, Battler user, Battler target, int numTargets) {
            Battle battle = user.battle;
            if (hasRaisedStatStages(target)) {                               // :2021
                // :2022 pbShowAnimation(@id,user,target,1) —— 原文把单个 Battler 当作 targets 传入
                //      （Move_Usage.rb:67-74 的 targets 允许非数组）；本运行时该形参是 Array<Battler>，
                //      故包一层。登记: 动画播放器未建模。
                Array<Battler> animTargets = new Array<>();
                animTargets.add(target);
                pbShowAnimation(move, move.id(), user, animTargets, 1, true);
                battle.display(user.pbThis() + "偷取了目标提升了的能力！");       // :2023
                boolean showAnim = true;                                     // :2024
                for (int s : PBStats.EACH_BATTLE_STAT) {                     // :2025 PBStats.eachBattleStat
                    if (target.stage(s) <= 0) {                              // :2026
                        continue;
                    }
                    if (user.pbCanRaiseStatStage(s, user, move, false)) {     // :2027
                        if (user.pbRaiseStatStage(s, target.stage(s), user, showAnim, false)) { // :2028
                            showAnim = false;                                // :2029
                        }
                    }
                    target.setStage(s, 0);                                   // :2032 target.stages[s] = 0
                }
            }
            super.pbCalcDamage(move, user, target, numTargets);              // :2035
        }
    }

    /** {@code class PokeBattle_Move_15E < PokeBattle_Move} (:2045-2050); Laser Focus. */
    public static class PokeBattle_Move_15E extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:2046-2049). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.LaserFocus, 2);        // :2047
            user.battle.display(user.pbThis() + "集中了精神！");                    // :2048
        }
    }

    /** {@code class PokeBattle_Move_15F < PokeBattle_StatDownMove} (:2057-2062); Clanging Scales. */
    public static class PokeBattle_Move_15F extends MoveEffectsGeneric.PokeBattle_StatDownMove {

        /** {@code initialize} (:2058-2061). */
        public PokeBattle_Move_15F() {
            statDown = new int[] {PBStats.DEFENSE, 1};                       // :2060
        }
    }

    /** {@code class PokeBattle_Move_160 < PokeBattle_Move} (:2071-2115); Strength Sap. */
    public static class PokeBattle_Move_160 extends MoveEffectBase {

        /** {@code healingMove?} (:2072). */
        @Override
        public boolean healingMove(BattleMove move) {
            return true;                                                     // :2072
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:2074-2089). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            // :2075-2079 NOTE (copied): 官方游戏似乎只检查目标攻击等级是否 -6，
            //        我另外加了「目标有 Contrary 且 +6 也失败」以对称。即使因特性/其它
            //        效果无法改变等级，本招式仍然有效。
            if (!battle.moldBreaker && target.hasActiveAbility("CONTRARY")    // :2081
                    && target.statStageAtMax(PBStats.ATTACK)) {               // :2081
                battle.display("但是失败了！");                  // :2082
                return true;                                                 // :2083
            } else if (target.statStageAtMin(PBStats.ATTACK)) {               // :2084
                battle.display("但是失败了！");                  // :2085
                return true;                                                 // :2086
            }
            return false;                                                    // :2088
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:2091-2114). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            // :2093-2094 Calculate target's effective attack value
            int[] stageMul = {2, 2, 2, 2, 2, 2, 2, 3, 4, 5, 6, 7, 8};
            int[] stageDiv = {8, 7, 6, 5, 4, 3, 2, 2, 2, 2, 2, 2, 2};
            int atk = target.attack();                                       // :2095
            int atkStage = target.stage(PBStats.ATTACK) + 6;                 // :2096
            int healAmt = (int) Math.floor(atk * (double) stageMul[atkStage] / stageDiv[atkStage]); // :2097
            // :2099 Reduce target's Attack stat
            if (target.pbCanLowerStatStage(PBStats.ATTACK, user, move, false)) {   // :2100
                target.pbLowerStatStage(PBStats.ATTACK, 1, user, true, false, false); // :2101
            }
            // :2103 Heal user
            if (target.hasActiveAbility("LIQUIDOOZE")) {                     // :2104
                battle.showAbilitySplash(target);                            // :2105
                user.pbReduceHP(healAmt);                                    // :2106
                battle.display(user.pbThis() + "吸到了污泥浆！");            // :2107
                battle.hideAbilitySplash(target);                            // :2108
                user.pbItemHPHealCheck(0, false);                            // :2109
            } else if (user.canHeal()) {                                     // :2110
                if (user.hasActiveItem("BIGROOT", false)) {                   // :2111
                    healAmt = (int) Math.floor(healAmt * 1.3);                // :2111
                }
                user.pbRecoverHP(healAmt);                                   // :2112
                battle.display(user.pbThis() + "的HP回复了。");              // :2113
            }
        }
    }

    // ==================================================================
    // 161-16A
    // ==================================================================

    /** {@code class PokeBattle_Move_161 < PokeBattle_Move} (:2122-2129); Speed Swap. */
    public static class PokeBattle_Move_161 extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:2123). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :2123
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:2125-2128). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            swapSpeed(user, target);                                         // :2126 user.speed,target.speed = target.speed,user.speed
            user.battle.display(user.pbThis() + "与目标交换了速度！");              // :2127
        }
    }

    /** {@code class PokeBattle_Move_162 < PokeBattle_Move} (:2136-2151); Burn Up. */
    public static class PokeBattle_Move_162 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:2137-2143). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.pbHasType("FIRE")) {                                   // :2138
                user.battle.display("但是失败了！");    // :2139
                return true;                                                   // :2140
            }
            return false;                                                      // :2142
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:2145-2150). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (!user.effects.truthy(PBEffects.Battler.BurnUp)) {   // :2146
                user.effects.set(PBEffects.Battler.BurnUp, true);   // :2147
                user.battle.display(user.pbThis() + "燃尽了自身！");               // :2148
            }
        }
    }

    /** {@code class PokeBattle_Move_164 < PokeBattle_Move_163} (:2178-2203); Photon Geyser. */
    public static class PokeBattle_Move_164 extends PokeBattle_Move_163 {

        /** {@code @calcCategory} (:2181/:2198; 1 by default, like the Ruby {@code initialize}). */
        private int calcCategory = 1;

        /** {@code physicalMove?(thisType=nil)} (:2184). */
        @Override
        public boolean physicalMove(BattleMove move, String thisType) {
            return calcCategory == 0;                                        // :2184
        }

        /** {@code specialMove?(thisType=nil)} (:2185). */
        @Override
        public boolean specialMove(BattleMove move, String thisType) {
            return calcCategory == 1;                                        // :2185
        }

        /** {@code pbOnStartUse(user,targets)} (:2187-2202). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = user.battle;
            // :2189-2190 Calculate user's effective attacking value
            int[] stageMul = {2, 2, 2, 2, 2, 2, 2, 3, 4, 5, 6, 7, 8};
            int[] stageDiv = {8, 7, 6, 5, 4, 3, 2, 2, 2, 2, 2, 2, 2};
            int atk = user.attack();                                         // :2191
            int atkStage = user.stage(PBStats.ATTACK) + 6;                   // :2192
            int realAtk = (int) Math.floor(atk * (double) stageMul[atkStage] / stageDiv[atkStage]); // :2193
            int spAtk = user.spAtk();                                        // :2194
            int spAtkStage = user.stage(PBStats.SPATK) + 6;                  // :2195
            int realSpAtk = (int) Math.floor(spAtk * (double) stageMul[spAtkStage] / stageDiv[spAtkStage]); // :2196
            // :2198 Determine move's category
            calcCategory = realAtk > realSpAtk ? 0 : 1;
            if (battle.moldBreaker                                          // :2199
                    && targets.size > 0 && targets.get(0).hasActiveItem("ABILITYSHIELD", false)) {
                battle.moldBreaker = false;                                  // :2200
            }
        }
    }

    /** {@code class PokeBattle_Move_165 < PokeBattle_Move} (:2209-2222); Core Enforcer. */
    public static class PokeBattle_Move_165 extends MoveEffectBase {

        /** {@code pbEffectAgainstTarget(user,target)} (:2210-2221). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            if (target.damageState.substitute
                    || target.effects.truthy(PBEffects.Battler.GastroAcid)) {   // :2211
                return;
            }
            if (target.unstoppableAbility(target.ability)) {                 // :2212（Ruby 无参 = 默认 @ability）
                return;
            }
            if (target.hasActiveItem("ABILITYSHIELD", false)) {               // :2213
                return;
            }
            // :2214-2216 @battle.choices[i][0]!=:UseItem && !((==:UseMove || ==:Shift) && target.movedThisRound?)
            String action = (String) battle.choices(target.index)[0];
            boolean useItem = ":UseItem".equals(action);
            boolean moveOrShift = ":UseMove".equals(action) || ":Shift".equals(action);
            if (!useItem && !(moveOrShift && target.movedThisRound())) {
                return;
            }
            target.effects.set(PBEffects.Battler.GastroAcid, true);    // :2217
            target.effects.set(PBEffects.Battler.Truant, false);       // :2218
            battle.display(target.pbThis() + "的特性被抑制了！");               // :2219
            target.pbOnAbilityChanged(target.ability);                       // :2220
        }
    }

    /** {@code class PokeBattle_Move_166 < PokeBattle_Move} (:2229-2233); Stomping Tantrum. */
    public static class PokeBattle_Move_166 extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:2230-2232). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (user.lastRoundMoveFailed) {                                  // :2230
                baseDmg *= 2;
            }
            return baseDmg;                                                  // :2231
        }
    }

    /** {@code class PokeBattle_Move_167 < PokeBattle_Move} (:2242-2260); Aurora Veil. */
    public static class PokeBattle_Move_167 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:2243-2252). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = user.battle;
            if (battle.pbWeather() != PBWeather.Hail && battle.pbWeather() != PBWeather.Snow) { // :2244
                battle.display("但是它失败了！");                  // :2245
                return true;                                                   // :2246
            }
            if (user.pbOwnSide().effects.intVal(PBEffects.Side.AuroraVeil) > 0) {   // :2248
                battle.display("但是它失败了！");                  // :2249
                return true;                                                   // :2250
            }
            return false;                                                      // :2252
        }

        /** {@code pbEffectGeneral(user)} (:2254-2259). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.pbOwnSide().effects.set(PBEffects.Side.AuroraVeil, 5);           // :2255
            if (user.hasActiveItem("LIGHTCLAY", false)) {                         // :2256
                user.pbOwnSide().effects.set(PBEffects.Side.AuroraVeil, 8);
            }
            user.battle.display(move.name() + "使"
                    + user.pbTeam(true) + "受到的物理和特殊伤害减弱了！");          // :2257-2258
        }
    }

    /** {@code class PokeBattle_Move_168 < PokeBattle_ProtectMove} (:2269-2274); Baneful Bunker. */
    public static class PokeBattle_Move_168 extends MoveEffectsGeneric.PokeBattle_ProtectMove {
        /** {@code initialize} (:2270-2273). */
        public PokeBattle_Move_168() {
            effect = PBEffects.Battler.BanefulBunker;                        // :2272
        }
    }

    /** {@code class PokeBattle_Move_169 < PokeBattle_Move} (:2282-2286); Revelation Dance. */
    public static class PokeBattle_Move_169 extends MoveEffectBase {

        /** {@code pbBaseType(user)} (:2283-2285). */
        @Override
        public String pbBaseType(BattleMove move, Battler user) {
            Array<String> userTypes = user.pbTypes(true);                     // :2283
            return userTypes.size == 0 ? null : userTypes.get(0);            // :2284 (null = Ruby 的 -1)
        }
    }

    /** {@code class PokeBattle_Move_16A < PokeBattle_Move} (:2295-2303); Spotlight. */
    public static class PokeBattle_Move_16A extends MoveEffectBase {

        /** {@code pbEffectAgainstTarget(user,target)} (:2296-2302). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.Spotlight, 1);        // :2297
            for (Battler b : target.allAllies()) {                                 // :2298 target.eachAlly
                if (b.effects.intVal(PBEffects.Battler.Spotlight)
                        < target.effects.intVal(PBEffects.Battler.Spotlight)) {   // :2299
                    continue;
                }
                target.effects.set(PBEffects.Battler.Spotlight,
                        b.effects.intVal(PBEffects.Battler.Spotlight) + 1);        // :2300
            }
            user.battle.display(target.pbThis() + "成为了视线的焦点！");               // :2301
        }
    }

    // ==================================================================
    // 16B-16F
    // ==================================================================

    /** {@code class PokeBattle_Move_16B < PokeBattle_Move} (:2310-2393); Instruct. */
    public static class PokeBattle_Move_16B extends MoveEffectBase {

        /** {@code @moveBlacklist} (:2315-2356), verbatim from the Ruby array. */
        private static final String[] MOVE_BLACKLIST = {
            "0D4",  // Bide
            "14B",  // King's Shield
            "16B",  // Instruct (this move)
            // Struggle
            "002",  // Struggle
            // Moves that affect the moveset
            "05C",  // Mimic
            "05D",  // Sketch
            "069",  // Transform
            // Moves that call other moves
            "0AE",  // Mirror Move
            "0AF",  // Copycat
            "0B0",  // Me First
            "0B3",  // Nature Power
            "0B4",  // Sleep Talk
            "0B5",  // Assist
            "0B6",  // Metronome
            // Moves that require a recharge turn
            "0C2",  // Hyper Beam
            // Two-turn attacks
            "0C3",  // Razor Wind
            "0C4",  // Solar Beam, Solar Blade
            "0C5",  // Freeze Shock
            "0C6",  // Ice Burn
            "0C7",  // Sky Attack
            "0C8",  // Skull Bash
            "0C9",  // Fly
            "0CA",  // Dig
            "0CB",  // Dive
            "0CC",  // Bounce
            "0CD",  // Shadow Force
            "0CE",  // Sky Drop
            "12E",  // Shadow Half
            "14D",  // Phantom Force
            "14E",  // Geomancy
            // Moves that start focussing at the start of the round
            "0BQ",  // GRANDEUR
            "115",  // Focus Punch
            "171",  // Shell Trap
            "172"   // Beak Blast
        };

        /** {@code ignoresSubstitute?(user)} (:2311). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :2311
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:2359-2388). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            // :2360 target.lastRegularMoveUsed<0 —— 本运行时存内部名，null 即 Ruby 的 -1
            if (target.lastRegularMoveUsed == null
                    || !pbHasMove(target, target.lastRegularMoveUsed)) {
                battle.display("但是失败了！");                  // :2361
                return true;                                                 // :2362
            }
            if (usingMultiTurnAttack(target)) {                              // :2364
                battle.display("但是失败了！");                  // :2365
                return true;                                                 // :2366
            }
            BattleMove targetMove = (BattleMove) battle.choices(target.index)[2];   // :2368
            if (targetMove != null                                            // :2369-2371
                    && ("115".equals(targetMove.function())                   // Focus Punch
                    || "171".equals(targetMove.function())                    // Shell Trap
                    || "172".equals(targetMove.function()))) {                // Beak Blast
                battle.display("但是失败了！");                  // :2372
                return true;                                                 // :2373
            }
            // :2375 @moveBlacklist.include?(pbGetMoveData(lastRegularMoveUsed,MOVE_FUNCTION_CODE))
            String lastFunction = moveFunctionCode(target.lastRegularMoveUsed);
            for (String black : MOVE_BLACKLIST) {
                if (black.equals(lastFunction)) {
                    battle.display("但是失败了！");              // :2376
                    return true;                                             // :2377
                }
            }
            // :2379-2382 idxMove = -1; target.eachMoveWithIndex { |m,i| idxMove = i if m.id==target.lastRegularMoveUsed }
            Array<BattleMove> withIndex = eachMoveWithIndex(target);
            int idxMove = -1;
            for (int i = 0; i < withIndex.size; i++) {
                BattleMove m = withIndex.get(i);
                if (m != null && m.internalName() != null
                        && m.internalName().equals(target.lastRegularMoveUsed)) {
                    idxMove = i;
                }
            }
            // :2383-2386 target.moves[idxMove].pp==0 && target.moves[idxMove].totalpp>0
            if (idxMove >= 0) {
                BattleMove slotMove = withIndex.get(idxMove);
                if (pp(slotMove) == 0 && slotMove.totalpp() > 0) {
                    battle.display("但是失败了！");              // :2384
                    return true;                                             // :2385
                }
            }
            return false;                                                    // :2387
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:2390-2392). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.Instruct, true);     // :2391
        }
    }

    /** {@code class PokeBattle_Move_16C < PokeBattle_Move} (:2400-2407); Throat Chop. */
    public static class PokeBattle_Move_16C extends MoveEffectBase {

        /** {@code pbAdditionalEffect(user,target)} (:2401-2406). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.fainted() || target.damageState.substitute) {         // :2402
                return;
            }
            if (target.effects.intVal(PBEffects.Battler.ThroatChop) == 0) {   // :2404
                user.battle.display(move.name() + "使"
                        + target.pbThis(true) + "无法使用某些招式！");   // :2403-2404
            }
            target.effects.set(PBEffects.Battler.ThroatChop, 3);      // :2405
        }
    }

    /** {@code class PokeBattle_Move_16D < PokeBattle_HealingMove} (:2414-2419); Shore Up. */
    public static class PokeBattle_Move_16D extends MoveEffectsGeneric.PokeBattle_HealingMove {

        /** {@code pbHealAmount(user)} (:2415-2418). */
        @Override
        public int pbHealAmount(BattleMove move, Battler user) {
            if (user.battle.pbWeather() == PBWeather.Sandstorm) {   // :2416
                return Math.round(user.maxHp() * 2 / 3.0f);                     // :2416 (totalhp*2/3.0).round
            }
            return Math.round(user.maxHp() / 2.0f);                             // :2417 (totalhp/2.0).round
        }
    }

    /** {@code class PokeBattle_Move_16E < PokeBattle_Move} (:2427-2447); Floral Healing. */
    public static class PokeBattle_Move_16E extends MoveEffectBase {

        /** {@code healingMove?} (:2428). */
        @Override
        public boolean healingMove(BattleMove move) {
            return true;                                                     // :2428
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:2430-2439). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.hp == target.maxHp()) {                               // :2431
                user.battle.display(target.pbThis() + "的HP已经满了！");           // :2432
                return true;                                                 // :2433
            } else if (!target.canHeal()) {                                  // :2434
                user.battle.display(target.pbThis() + "没有受到影响！");          // :2435
                return true;                                                 // :2436
            }
            return false;                                                    // :2438
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:2441-2446). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            int hpGain = Math.round(target.maxHp() / 2.0f);                  // :2442
            if (user.battle.field.terrain == PBBattleTerrains.Grassy) {   // :2443
                hpGain = Math.round(target.maxHp() * 2 / 3.0f);
            }
            target.pbRecoverHP(hpGain);                                      // :2444
            user.battle.display(target.pbThis() + "的HP回复了。");                // :2445
        }
    }

    /** {@code class PokeBattle_Move_16F < PokeBattle_Move} (:2455-2494); Pollen Puff. */
    public static class PokeBattle_Move_16F extends MoveEffectBase {

        /** {@code @healing} (:2462-2463/:2479/:2486/:2491). */
        private boolean healing;

        /** {@code pbTarget(user)} (:2456-2459). */
        @Override
        public int pbTarget(BattleMove move, Battler user) {
            if (user.effects.intVal(PBEffects.Battler.HealBlock) > 0) {   // :2457
                return PBTargets.NearFoe;
            }
            return super.pbTarget(move, user);                               // :2458
        }

        /** {@code pbOnStartUse(user,targets)} (:2461-2464). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            healing = false;                                                 // :2462
            if (targets.size > 0) {                                          // :2463
                healing = !user.opposes(targets.get(0));
            }
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:2466-2477). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (!healing) {                                                  // :2467
                return false;
            }
            if (target.effects.intVal(PBEffects.Battler.Substitute) > 0
                    && !ignoresSubstitute(move, user)) {                     // :2468
                user.battle.display("但是失败了！");    // :2469
                return true;                                                   // :2470
            }
            if (!target.canHeal()) {                                         // :2472
                user.battle.display("但是失败了！");    // :2473
                return true;                                                   // :2474
            }
            return false;                                                      // :2476
        }

        /** {@code pbDamagingMove?} (:2479-2482). */
        @Override
        public boolean pbDamagingMove(BattleMove move) {
            if (healing) {                                                   // :2480
                return false;
            }
            return super.pbDamagingMove(move);                               // :2481
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:2484-2488). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (!healing) {                                                  // :2485
                return;
            }
            target.pbRecoverHP(target.maxHp() / 2);                          // :2486
            user.battle.display(target.pbThis() + "的HP回复了。");                // :2487
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} (:2490-2493) - 登记: 动画播放器未建模. */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets, int hitNum,
                                    boolean showAnimation) {
            if (healing) {                                                   // :2491 Healing anim
                hitNum = 1;
            }
            super.pbShowAnimation(move, id, user, targets, hitNum, showAnimation);   // :2492
        }
    }

    // ==================================================================
    // 170-179
    // ==================================================================

    /** {@code class PokeBattle_Move_170 < PokeBattle_Move} (:2501-2527); Mind Blown. */
    public static class PokeBattle_Move_170 extends MoveEffectBase {

        /** {@code worksWithNoTargets?} (:2502). */
        @Override
        public boolean worksWithNoTargets(BattleMove move) {
            return true;                                                     // :2502
        }

        /** {@code pbMoveFailed?(user,targets)} (:2504-2520). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = user.battle;
            if (!battle.moldBreaker) {                                       // :2505
                Battler bearer = battle.pbCheckGlobalAbility("DAMP");         // :2506
                if (bearer != null) {                                        // :2507
                    battle.showAbilitySplash(bearer);                        // :2508
                    if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {       // :2509
                        battle.display(user.pbThis()
                                + "不能使用" + move.name() + "了！");            // :2510
                    } else {
                        battle.display(bearer.pbThis(true) + "的"
                                + bearer.abilityName() + "使得" + user.pbThis()
                                + "无法使用" + move.name() + "了！");             // :2512-2513
                    }
                    battle.hideAbilitySplash(bearer);                        // :2515
                    return true;                                             // :2516
                }
            }
            return false;                                                    // :2519
        }

        /** {@code pbSelfKO(user)} (:2522-2526). */
        @Override
        public void pbSelfKO(BattleMove move, Battler user) {
            if (!user.takesIndirectDamage(false)) {                          // :2523 (showMsg=false)
                return;
            }
            user.pbReduceHP(Math.round(user.maxHp() / 2.0f), false, true, true);   // :2524 (anim=false)
            user.pbItemHPHealCheck(0, false);                                // :2525
        }
    }

    /** {@code class PokeBattle_Move_171 < PokeBattle_Move} (:2535-2557); Shell Trap. */
    public static class PokeBattle_Move_171 extends MoveEffectBase {

        /** {@code pbDisplayChargeMessage(user)} (:2536-2540). */
        @Override
        public void pbDisplayChargeMessage(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.ShellTrap, true);       // :2537
            user.battle.commonAnimation("ShellTrap", user);            // :2538
            user.battle.display(user.pbThis() + "设置了一个甲壳陷阱！");             // :2539
        }

        /** {@code pbDisplayUseMessage(user)} (:2542-2544). */
        @Override
        public void pbDisplayUseMessage(BattleMove move, Battler user) {
            if (user.tookPhysicalHit) {                                      // :2543
                super.pbDisplayUseMessage(move, user);
            }
        }

        /** {@code pbMoveFailed?(user,targets)} (:2546-2556). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.effects.truthy(PBEffects.Battler.ShellTrap)) {   // :2547
                user.battle.display("但是失败了！");     // :2548
                return true;                                                     // :2549
            }
            if (!user.tookPhysicalHit) {                                     // :2551
                user.battle.display(user.pbThis() + "的陷阱甲壳没有生效！");          // :2552
                return true;                                                     // :2553
            }
            return false;                                                        // :2555
        }
    }

    /** {@code class PokeBattle_Move_172 < PokeBattle_Move} (:2565-2579); Beak Blast. */
    public static class PokeBattle_Move_172 extends MoveEffectBase {

        /** {@code pbDisplayChargeMessage(user)} (:2566-2570). */
        @Override
        public void pbDisplayChargeMessage(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.BeakBlast, true);       // :2567
            user.battle.commonAnimation("BeakBlast", user);            // :2568
            user.battle.display(user.pbThis() + "开始加热鸟喙了！");                 // :2569
        }

        /** {@code pbMoveFailed?(user,targets)} (:2572-2578). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.effects.truthy(PBEffects.Battler.BeakBlast)) {   // :2573
                user.battle.display("但是失败了！");     // :2574
                return true;                                                     // :2575
            }
            return false;                                                        // :2577
        }
    }

    /** {@code class PokeBattle_Move_173 < PokeBattle_Move} (:2588-2600); Psychic Terrain. */
    public static class PokeBattle_Move_173 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:2589-2595). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.battle.field.terrain == PBBattleTerrains.Psychic) {   // :2590
                user.battle.display("但是失败了！");     // :2591
                return true;                                                     // :2592
            }
            return false;                                                        // :2594
        }

        /** {@code pbEffectGeneral(user)} (:2597-2599) - 登记: pbStartTerrain（演出+场地子系统未建模）. */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            pbStartTerrain(user.battle, user, PBBattleTerrains.Psychic, true);   // :2598
        }
    }

    /** {@code class PokeBattle_Move_174 < PokeBattle_Move} (:2607-2615); First Impression. */
    public static class PokeBattle_Move_174 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:2608-2614). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.turnCount > 1 || user.lastRoundMoved >= 0) {             // :2609
                user.battle.display("但是失败了！");     // :2610
                return true;                                                     // :2611
            }
            return false;                                                        // :2613
        }
    }

    /** {@code class PokeBattle_Move_175 < PokeBattle_FlinchMove} (:2623-2627); Double Iron Bash. */
    public static class PokeBattle_Move_175 extends MoveEffectsGeneric.PokeBattle_FlinchMove {

        /** {@code multiHitMove?} (:2624). */
        @Override
        public boolean multiHitMove(BattleMove move) {
            return true;                                                     // :2624
        }

        /** {@code pbNumHits(user,targets)} (:2625). */
        @Override
        public int pbNumHits(BattleMove move, Battler user, Array<Battler> targets) {
            return 2;                                                        // :2625
        }

        /** {@code tramplesMinimize?(param=1)} (:2626). */
        @Override
        public boolean tramplesMinimize(BattleMove move, int param) {
            return true;                                                     // :2626
        }
    }

    /** {@code class PokeBattle_Move_176 < PokeBattle_StatUpMove} (:2635-2662); Aura Wheel. */
    public static class PokeBattle_Move_176 extends MoveEffectsGeneric.PokeBattle_StatUpMove {

        /** {@code initialize} (:2636-2639). */
        public PokeBattle_Move_176() {
            statUp = new int[] {PBStats.SPEED, 1};                           // :2638
        }

        /** {@code pbMoveFailed?(user,targets)} (:2641-2650). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            // :2642 NEWEST_BATTLE_MECHANICS && isConst?(@id,PBMoves,:AURAWHEEL)
            if (Battle.NEWEST_BATTLE_MECHANICS
                    && move.id() == MoveFxPendingApi.PBMoves_id("AURAWHEEL")) {
                // :2643-2644 !isConst?(user.species,PBSpecies,:MORPEKO) && !isConst?(user.effects[TransformSpecies],…)
                if (!"MORPEKO".equals(species(user))
                        && !"MORPEKO".equals(user.effects
                                .stringVal(PBEffects.Battler.TransformSpecies))) {
                    user.battle.display("但是"
                            + user.pbThis() + "不能使用这个招式！");      // :2645
                    return true;                                                 // :2646
                }
            }
            return false;                                                        // :2649
        }

        /** {@code pbBaseType(user)} (:2652-2661). */
        @Override
        public String pbBaseType(BattleMove move, Battler user) {
            String ret = "NORMAL";                                           // :2653 getID(PBTypes,:NORMAL)
            switch (user.form()) {                                           // :2654 case user.form
                case 0: ret = "ELECTRIC"; break;                             // :2656 getConst(PBTypes,:ELECTRIC) || ret
                case 1: ret = "DARK"; break;                                 // :2658 getConst(PBTypes,:DARK) || ret
                default: break;
            }
            return ret;                                                      // :2660
        }
    }

    /** {@code class PokeBattle_Move_177 < PokeBattle_Move} (:2671-2674); Body Press. */
    public static class PokeBattle_Move_177 extends MoveEffectBase {

        /** {@code pbGetAttackStats(user,target)} (:2672-2673). */
        @Override
        public MoveStats pbGetAttackStats(BattleMove move, Battler user, Battler target) {
            return new MoveStats(user.defense(), user.stage(PBStats.DEFENSE) + 6);   // :2672
        }
    }

    /** {@code class PokeBattle_Move_178 < PokeBattle_Move} (:2683-2692); Fishious Rend / Bolt Beak. */
    public static class PokeBattle_Move_178 extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:2684-2691). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            Battle battle = user.battle;
            String action = (String) battle.choices(target.index)[0];        // :2685-2687 @battle.choices[i][0]
            if (!":None".equals(action)
                    && ((":UseMove".equals(action) ? false : ":Shift".equals(action))
                    || target.movedThisRound())) {
                // :2688-2689 (the Ruby `if` body is empty: the doubling is the `else`)
            } else {
                baseDmg *= 2;                                                // :2689
            }
            return baseDmg;                                                  // :2691
        }
    }

    /** {@code class PokeBattle_Move_179 < PokeBattle_Move} (:2700-2732); Clangorous Soul. */
    public static class PokeBattle_Move_179 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:2701-2712). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.hp <= user.maxHp() / 3                                   // :2702 user.hp<=(user.totalhp/3)
                    || (!user.pbCanRaiseStatStage(PBStats.ATTACK, user, move, false)    // :2703
                    && !user.pbCanRaiseStatStage(PBStats.DEFENSE, user, move, false)    // :2704
                    && !user.pbCanRaiseStatStage(PBStats.SPEED, user, move, false)      // :2705
                    && !user.pbCanRaiseStatStage(PBStats.SPATK, user, move, false)      // :2706
                    && !user.pbCanRaiseStatStage(PBStats.SPDEF, user, move, false))) {  // :2707
                user.battle.display("但是失败了！");     // :2708
                return true;                                                     // :2709
            }
            return false;                                                        // :2711
        }

        /** {@code pbEffectGeneral(user)} (:2714-2731). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            if (user.pbCanRaiseStatStage(PBStats.ATTACK, user, move, false)) {    // :2715
                user.pbRaiseStatStage(PBStats.ATTACK, 1, user, true, false);      // :2716
            }
            if (user.pbCanRaiseStatStage(PBStats.DEFENSE, user, move, false)) {   // :2718
                user.pbRaiseStatStage(PBStats.DEFENSE, 1, user, true, false);     // :2719
            }
            if (user.pbCanRaiseStatStage(PBStats.SPEED, user, move, false)) {     // :2721
                user.pbRaiseStatStage(PBStats.SPEED, 1, user, true, false);       // :2722
            }
            if (user.pbCanRaiseStatStage(PBStats.SPATK, user, move, false)) {     // :2724
                user.pbRaiseStatStage(PBStats.SPATK, 1, user, true, false);       // :2725
            }
            if (user.pbCanRaiseStatStage(PBStats.SPDEF, user, move, false)) {     // :2727
                user.pbRaiseStatStage(PBStats.SPDEF, 1, user, true, false);       // :2728
            }
            user.pbReduceHP(user.maxHp() / 3, false, true, true);            // :2730 (anim=false)
        }
    }

    // ==================================================================
    // 17A-17F（本文件最后 6 个类）
    // ==================================================================

    /** {@code class PokeBattle_Move_17A < PokeBattle_Move} (:2740-2821); Court Change. */
    public static class PokeBattle_Move_17A extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:2741-2820). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            Battle battle = user.battle;
            boolean changeside = false;                                  // :2742
            BattleSide[] sides = {user.pbOwnSide(), user.pbOpposingSide()};   // :2743
            for (int i = 0; i < 2; i++) {                                // :2744 for i in 0...2
                if (sideEffectsEmpty(sides[i])) {                        // :2745-2757
                    continue;
                }
                changeside = true;                                       // :2758
            }
            if (!changeside) {                                           // :2760
                battle.display("但是失败了！");              // :2761
                // :2762 return -1 —— pbEffectGeneral 在本运行时是 void，Ruby 的返回值被丢弃
                return;
            }
            BattleSide ownside = sides[0];                               // :2764
            BattleSide oppside = sides[1];
            // :2766-2816 逐个交换两边的 12 个效果，Ruby 每段都是「取临时值 → 双向赋值」
            swapEffect(ownside.effects, oppside.effects, PBEffects.Side.Reflect);      // :2766-2768
            swapEffect(ownside.effects, oppside.effects, PBEffects.Side.LightScreen);  // :2770-2772
            swapEffect(ownside.effects, oppside.effects, PBEffects.Side.AuroraVeil);   // :2774-2776
            swapEffect(ownside.effects, oppside.effects, PBEffects.Side.SeaOfFire);    // :2778-2780
            swapEffect(ownside.effects, oppside.effects, PBEffects.Side.Swamp);        // :2782-2784
            swapEffect(ownside.effects, oppside.effects, PBEffects.Side.Rainbow);      // :2786-2788
            swapEffect(ownside.effects, oppside.effects, PBEffects.Side.Mist);         // :2790-2792
            swapEffect(ownside.effects, oppside.effects, PBEffects.Side.Spikes);       // :2794-2796
            swapEffect(ownside.effects, oppside.effects, PBEffects.Side.ToxicSpikes);  // :2798-2800
            swapEffect(ownside.effects, oppside.effects, PBEffects.Side.StealthRock);  // :2802-2804
            swapEffect(ownside.effects, oppside.effects, PBEffects.Side.StickyWeb);    // :2806-2808
            // :2810 Sticky Web user is preserved, for Defiant/Competitive.
            swapEffect(ownside.effects, oppside.effects, PBEffects.Side.StickyWebUser); // :2810-2812
            swapEffect(ownside.effects, oppside.effects, PBEffects.Side.Tailwind);     // :2814-2816
            battle.display(user.pbThis()
                    + " swapped the battle effects affecting each side of the field!"); // :2817
            // :2818 return 0 —— 同 :2762，返回值被丢弃
        }
    }

    /** {@code class PokeBattle_Move_17B < PokeBattle_TargetMultiStatUpMove} (:2829-2834); Decorate. */
    public static class PokeBattle_Move_17B extends MoveEffectsGeneric.PokeBattle_TargetMultiStatUpMove {

        /** {@code initialize} (:2830-2833). */
        public PokeBattle_Move_17B() {
            statUp = new int[] {PBStats.ATTACK, 2, PBStats.SPATK, 2};        // :2832
        }
    }

    /**
     * {@code class PokeBattle_Move_17C < PokeBattle_Move_0BD} (:2845-2850); Dragon Darts.
     *
     * <p>Its parent lives in {@code MoveEffects_0B0_0D4.java} (another file of
     * this batch).</p>
     */
    public static class PokeBattle_Move_17C extends MoveEffects_0B0_0D4.PokeBattle_Move_0BD {

        /** {@code pbNumHits(user,targets)} (:2846-2849). */
        @Override
        public int pbNumHits(BattleMove move, Battler user, Array<Battler> targets) {
            if (targets.size > 1) {                                      // :2847
                return 1;
            }
            return 2;                                                    // :2848
        }
    }

    /** {@code class PokeBattle_Move_17D < PokeBattle_Move} (:2857-2868); Jaw Lock. */
    public static class PokeBattle_Move_17D extends MoveEffectBase {

        /** {@code pbEffectAgainstTarget(user,target)} (:2858-2867). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.effects.intVal(PBEffects.Battler.JawLockUser) < 0
                    && !target.effects.truthy(PBEffects.Battler.JawLock)      // :2860
                    && user.effects.intVal(PBEffects.Battler.JawLockUser) < 0 // :2861
                    && !user.effects.truthy(PBEffects.Battler.JawLock)) {
                user.effects.set(PBEffects.Battler.JawLock, true);        // :2862
                target.effects.set(PBEffects.Battler.JawLock, true);      // :2863
                user.effects.set(PBEffects.Battler.JawLockUser, user.index);   // :2864
                target.effects.set(PBEffects.Battler.JawLockUser, user.index); // :2865
                user.battle.display("双方都无法逃跑了！");    // :2866
            }
        }
    }

    /** {@code class PokeBattle_Move_17E < PokeBattle_Move} (:2878-2915); Life Dew. */
    public static class PokeBattle_Move_17E extends MoveEffectBase {

        /** {@code healingMove?} (:2879). */
        @Override
        public boolean healingMove(BattleMove move) {
            return true;                                                     // :2879
        }

        /** {@code worksWithNoTargets?} (:2880). */
        @Override
        public boolean worksWithNoTargets(BattleMove move) {
            return true;                                                     // :2880
        }

        /** {@code pbMoveFailed?(user,targets)} (:2882-2893). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = user.battle;
            boolean failed = true;                                           // :2883
            for (Battler b : battle.eachSameSideBattler(user.index)) {        // :2884
                if (b.hp == b.maxHp()) {                                     // :2885
                    continue;
                }
                failed = false;                                              // :2886
                break;                                                       // :2887
            }
            if (failed) {                                                    // :2889
                battle.display("但是失败了！");                  // :2890
                return true;                                                 // :2891
            }
            return false;                                                    // :2893
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:2895-2904). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.hp == target.maxHp()) {                               // :2896
                user.battle.display(target.pbThis() + "的HP已经满了！");           // :2897
                return true;                                                 // :2898
            } else if (!target.canHeal()) {                                  // :2899
                user.battle.display(target.pbThis() + "没有受到影响！");          // :2900
                return true;                                                 // :2901
            }
            return false;                                                    // :2903
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:2906-2910). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            int hpGain = Math.round(target.maxHp() / 4.0f);                  // :2907 (target.totalhp/4.0).round
            target.pbRecoverHP(hpGain);                                      // :2908
            user.battle.display(target.pbThis() + "的HP回复了。");                // :2909
        }

        /** {@code pbHealAmount(user)} (:2912-2914). */
        @Override
        public int pbHealAmount(BattleMove move, Battler user) {
            return Math.round(user.maxHp() / 4.0f);                          // :2913 (user.totalhp/4.0).round
        }
    }

    /** {@code class PokeBattle_Move_17F < PokeBattle_MultiStatUpMove} (:2922-2962); No Retreat. */
    public static class PokeBattle_Move_17F extends MoveEffectsGeneric.PokeBattle_MultiStatUpMove {

        /** {@code pbMoveFailed?(user,targets)} (:2923-2937). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.effects.truthy(PBEffects.Battler.NoRetreat)) {   // :2924
                user.battle.display("但是失败了！");     // :2925
                return true;                                                     // :2926
            }
            if (!user.pbCanRaiseStatStage(PBStats.ATTACK, user, move, true)      // :2928 (showFailMsg=true)
                    && !user.pbCanRaiseStatStage(PBStats.DEFENSE, user, move, true)   // :2929
                    && !user.pbCanRaiseStatStage(PBStats.SPATK, user, move, true)     // :2930
                    && !user.pbCanRaiseStatStage(PBStats.SPDEF, user, move, true)     // :2931
                    && !user.pbCanRaiseStatStage(PBStats.SPEED, user, move, true)) {  // :2932
                user.battle.display("但是失败了！");     // :2933
                return true;                                                     // :2934
            }
            return false;                                                        // :2936
        }

        /** {@code pbEffectGeneral(user)} (:2939-2961). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            Battle battle = user.battle;
            if (user.pbCanRaiseStatStage(PBStats.ATTACK, user, move, false)) {    // :2940
                user.pbRaiseStatStage(PBStats.ATTACK, 1, user, true, false);      // :2941
            }
            if (user.pbCanRaiseStatStage(PBStats.DEFENSE, user, move, false)) {   // :2943
                user.pbRaiseStatStage(PBStats.DEFENSE, 1, user, true, false);     // :2944
            }
            if (user.pbCanRaiseStatStage(PBStats.SPEED, user, move, false)) {     // :2946
                user.pbRaiseStatStage(PBStats.SPEED, 1, user, true, false);       // :2947
            }
            if (user.pbCanRaiseStatStage(PBStats.SPATK, user, move, false)) {     // :2949
                user.pbRaiseStatStage(PBStats.SPATK, 1, user, true, false);       // :2950
            }
            if (user.pbCanRaiseStatStage(PBStats.SPDEF, user, move, false)) {     // :2952
                user.pbRaiseStatStage(PBStats.SPDEF, 1, user, true, false);       // :2953
            }
            // :2956-2957 !(MeanLook>=0 || Trapping>0 || JawLock || OctolockUser>=0)
            if (!(user.effects.intVal(PBEffects.Battler.MeanLook) >= 0
                    || user.effects.intVal(PBEffects.Battler.Trapping) > 0
                    || user.effects.truthy(PBEffects.Battler.JawLock)
                    || user.effects.intVal(PBEffects.Battler.OctolockUser) >= 0)) {
                user.effects.set(PBEffects.Battler.NoRetreat, true);   // :2958
                battle.display(user.pbThis() + "无法再退出战斗！");              // :2959
            }
        }
    }

    // ==================================================================
    // File-local M0 helpers (Lead ruling: no shared-file edits, one file per
    // L2 author). Every helper below is a THROWING stub for a plugin method
    // this runtime has not landed - never a made-up value. They are collected
    // here so the retire pass has one place to look per file.
    // ==================================================================

    /**
     * {@code Battler#eachMove} (PokeBattle_Battler:543-545) - walks the
     * non-empty move slots in slot order. 本运行时缺此方法。
     */
    private static Array<BattleMove> eachMove(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:543-545 eachMove");
    }

    /**
     * {@code Battler#eachMoveWithIndex} (PokeBattle_Battler:547-549).
     * 本运行时缺此方法。
     */
    private static Array<BattleMove> eachMoveWithIndex(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:547-549 eachMoveWithIndex");
    }

    /** {@code PokeBattle_Move#pp} (PokeBattle_Move.rb:12/66-70 的 {@code @pp}). 本运行时缺此方法。 */
    private static int pp(BattleMove move) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Move pp");
    }

    /** {@code Battler#pbSetPP(move,pp)} (Battler_ChangeSelf:132-135). 本运行时缺此方法。 */
    private static void pbSetPP(Battler battler, BattleMove move, int newPp) {
        throw new UnsupportedOperationException("M0 待接线: Battler_ChangeSelf:132-135 pbSetPP");
    }

    /** {@code Battler#mega?} (PokeBattle_Battler:148). 本运行时缺此方法。 */
    private static boolean mega(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:148 mega?");
    }

    /** {@code Battler#canChangeType?} (PokeBattle_Battler:585-589). 本运行时缺此方法。 */
    private static boolean canChangeType(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:585-589 canChangeType?");
    }

    /** {@code Battler#hasRaisedStatStages?} (Battler_StatStages:383-386). 本运行时缺此方法。 */
    private static boolean hasRaisedStatStages(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: Battler_StatStages:383-386 hasRaisedStatStages?");
    }

    /**
     * {@code Battler#pbHasMove?(move_id)} (PokeBattle_Battler:551-553)：是否知道某招式。
     * 参数按本运行时的招式身份（内部名）取，{@code null} 即 Ruby 的 -1。本运行时缺此方法。
     */
    private static boolean pbHasMove(Battler battler, String moveInternalName) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:551-553 pbHasMove?");
    }

    /**
     * {@code pbGetMoveData(move, MOVE_FUNCTION_CODE)} (PBMove:32 的顶层函数)：
     * 按招式身份取 function code。本运行时缺此方法。
     */
    private static String moveFunctionCode(String moveInternalName) {
        throw new UnsupportedOperationException("M0 待接线: PBMove:32 pbGetMoveData(MOVE_FUNCTION_CODE)");
    }

    /** {@code Battler#usingMultiTurnAttack?} (PokeBattle_Battler:708-711). 本运行时缺此方法。 */
    private static boolean usingMultiTurnAttack(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:708-711 usingMultiTurnAttack?");
    }

    /** {@code Battler#belched?} (PokeBattle_Battler:756-758). 本运行时缺此方法。 */
    private static boolean belched(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:756-758 belched?");
    }

    /** {@code Battler#pbRecoverHPFromDrain(amt,target,msg=nil)} (Battler_ChangeSelf:33-39). 本运行时缺此方法。 */
    private static int pbRecoverHPFromDrain(Battler battler, int amount, Battler target) {
        throw new UnsupportedOperationException("M0 待接线: Battler_ChangeSelf:33-39 pbRecoverHPFromDrain");
    }

    /** {@code Battler#species} (PokeBattle_Battler，返回种族内部名). 本运行时缺此方法。 */
    private static String species(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler species");
    }

    /**
     * {@code Battler#pbReduceHP(amt,anim,registerDamage,anyAnim)} 的 4 实参形态
     * (Battler_ChangeSelf:5-17)。{@code battler.pbReduceHP(amount, anim, true, true)}
     * 只有 3 参，表达不了 {@code registerDamage=false}，故在本文件内另立。
     * 本运行时缺此方法。
     */
    private static int pbReduceHP(Battler battler, int amount, boolean anim, boolean registerDamage) {
        throw new UnsupportedOperationException("M0 待接线: Battler_ChangeSelf:5-17 pbReduceHP(4 参)");
    }

    /** {@code PBMoves.getName(id)} (Compiler_PBS:446 生成的名称表). 本运行时缺此方法。 */
    private static String PBMoves_getName(int id) {
        throw new UnsupportedOperationException("M0 待接线: PBMoves.getName (Compiler_PBS:446)");
    }

    /**
     * {@code user.speed, target.speed = target.speed, user.speed}
     * (Move_Effects_100-17F.rb:2126)：交换双方「当前速度值」（不是等级）。
     * 本运行时 {@code Battler.speed()} 是计算属性、无 setter，故在本文件内另立。
     * 本运行时缺此方法。
     */
    private static void swapSpeed(Battler user, Battler target) {
        throw new UnsupportedOperationException("M0 待接线: Move_Effects_100-17F.rb:2126 speed 交换（Battler.speed 无 setter）");
    }

    /** {@code Battler#pbEffectsOnSwitchIn(switchIn=false)} (Battler_AbilityAndItem:5-39). 本运行时缺此方法。 */
    private static void pbEffectsOnSwitchIn(Battler battler, boolean switchIn) {
        throw new UnsupportedOperationException("M0 待接线: Battler_AbilityAndItem:5-39 pbEffectsOnSwitchIn");
    }

    /**
     * {@code Court Change} 的「这一侧没有任何可交换效果」判定
     * (Move_Effects_100-17F.rb:2745-2757)，逐项照抄。
     */
    private static boolean sideEffectsEmpty(BattleSide side) {
        EffectMap e = side.effects;
        return e.intVal(PBEffects.Side.Reflect) == 0
                && e.intVal(PBEffects.Side.LightScreen) == 0
                && e.intVal(PBEffects.Side.AuroraVeil) == 0
                && e.intVal(PBEffects.Side.SeaOfFire) == 0
                && e.intVal(PBEffects.Side.Swamp) == 0
                && e.intVal(PBEffects.Side.Rainbow) == 0
                && e.intVal(PBEffects.Side.Mist) == 0
                && e.intVal(PBEffects.Side.Safeguard) == 0
                && !e.truthy(PBEffects.Side.StealthRock)
                && e.intVal(PBEffects.Side.Spikes) == 0
                && !e.truthy(PBEffects.Side.StickyWeb)
                && e.intVal(PBEffects.Side.ToxicSpikes) == 0
                && e.intVal(PBEffects.Side.Tailwind) == 0;
    }

    /**
     * {@code x=a.effects[k]; a.effects[k]=b.effects[k]; b.effects[k]=x}
     * (Move_Effects_100-17F.rb:2766-2816 的每一段都是这三行)。
     */
    private static void swapEffect(EffectMap a, EffectMap b, int idx) {
        Object tmp = a.raw(idx);
        a.set(idx, b.raw(idx));
        b.set(idx, tmp);
    }

    /** {@code Battle#pbPriority} (PokeBattle_Battle，按速度排序的场上 battler 数组). 本运行时缺此方法。 */
    private static Array<Battler> pbPriority(Battle battle) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battle pbPriority");
    }

    /**
     * {@code @battle.battlers} (PokeBattle_Battle:46) 的原始 6 槽（含空槽与已倒下者）。
     * {@code Battle.allBattlers()} 是「未倒下」语义，替不了 {@code battlers.each} 的扫描。
     * 本运行时缺此方法。
     */
    private static Array<Battler> battlers(Battle battle) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battle:46 @battlers");
    }

    /** {@code Battle#pbActivateHealingWish(battler)} (Battle_Action_Switching:363). 本运行时缺此方法。 */
    private static void pbActivateHealingWish(Battle battle, Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: Battle_Action_Switching:363 pbActivateHealingWish");
    }

    /** {@code Battle#pbGetOwnerIndexFromBattlerIndex(idxBattler)} (PokeBattle_Battle:232). 本运行时缺此方法。 */
    private static int pbGetOwnerIndexFromBattlerIndex(Battle battle, int idxBattler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battle:232 pbGetOwnerIndexFromBattlerIndex");
    }

    /** {@code Battle#pbSwapBattlers(idxA,idxB)} (PokeBattle_Battle:593). 本运行时缺此方法。 */
    private static boolean pbSwapBattlers(Battle battle, int idxA, int idxB) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battle:593 pbSwapBattlers");
    }

    /** {@code Battle#pbPursuit(idxSwitcher)} (Battle_Phase_Attack:24). 本运行时缺此方法。 */
    private static void pbPursuit(Battle battle, int idxSwitcher) {
        throw new UnsupportedOperationException("M0 待接线: Battle_Phase_Attack:24 pbPursuit");
    }

    /**
     * {@code Battle#pbStartTerrain(user,newTerrain,fixedDuration=true)}
     * (PokeBattle_Battle:741-769).
     *
     * <p>登记: roster §B 已把 {@code pbStartTerrain} 列为「登记」——它要
     * {@code PBBattleTerrains.animationName} 的演出与 {@code pbCalculatePriority}
     * 的 AI 子系统，本批都未建模。照抄调用形状，不造替代实现。</p>
     */
    private static void pbStartTerrain(Battle battle, Battler user, int newTerrain, boolean fixedDuration) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battle:741-769 pbStartTerrain（登记：演出+场地子系统未建模）");
    }

    // __APPEND_MARKER__

    /**
     * {@code PokeBattle_Move.pbFromPBMove} (PokeBattle_Move.rb:51-59) for the
     * 115 function-code classes this file holds: the Ruby builds
     * {@code "PokeBattle_Move_#{function}"} and instantiates it on demand, so each
     * code is bound to its no-argument constructor here and
     * {@link MoveEffectRegistry#of} calls it lazily (some constructors still touch
     * not-yet-wired stubs, which must not break the whole table).
     *
     * <p>Generated by {@code __l2register.mjs} - re-run it with {@code --write}
     * after the class set changes.</p>
     */
    static void register() {
        MoveEffectRegistry.register("100", PokeBattle_Move_100::new);
        MoveEffectRegistry.register("101", PokeBattle_Move_101::new);
        MoveEffectRegistry.register("102", PokeBattle_Move_102::new);
        MoveEffectRegistry.register("103", PokeBattle_Move_103::new);
        MoveEffectRegistry.register("104", PokeBattle_Move_104::new);
        MoveEffectRegistry.register("105", PokeBattle_Move_105::new);
        MoveEffectRegistry.register("106", PokeBattle_Move_106::new);
        MoveEffectRegistry.register("107", PokeBattle_Move_107::new);
        MoveEffectRegistry.register("108", PokeBattle_Move_108::new);
        MoveEffectRegistry.register("109", PokeBattle_Move_109::new);
        MoveEffectRegistry.register("10A", PokeBattle_Move_10A::new);
        MoveEffectRegistry.register("10B", PokeBattle_Move_10B::new);
        MoveEffectRegistry.register("10C", PokeBattle_Move_10C::new);
        MoveEffectRegistry.register("10D", PokeBattle_Move_10D::new);
        MoveEffectRegistry.register("10E", PokeBattle_Move_10E::new);
        MoveEffectRegistry.register("10F", PokeBattle_Move_10F::new);
        MoveEffectRegistry.register("110", PokeBattle_Move_110::new);
        MoveEffectRegistry.register("111", PokeBattle_Move_111::new);
        MoveEffectRegistry.register("112", PokeBattle_Move_112::new);
        MoveEffectRegistry.register("113", PokeBattle_Move_113::new);
        MoveEffectRegistry.register("114", PokeBattle_Move_114::new);
        MoveEffectRegistry.register("115", PokeBattle_Move_115::new);
        MoveEffectRegistry.register("116", PokeBattle_Move_116::new);
        MoveEffectRegistry.register("117", PokeBattle_Move_117::new);
        MoveEffectRegistry.register("118", PokeBattle_Move_118::new);
        MoveEffectRegistry.register("119", PokeBattle_Move_119::new);
        MoveEffectRegistry.register("11A", PokeBattle_Move_11A::new);
        MoveEffectRegistry.register("11B", PokeBattle_Move_11B::new);
        MoveEffectRegistry.register("11C", PokeBattle_Move_11C::new);
        MoveEffectRegistry.register("11D", PokeBattle_Move_11D::new);
        MoveEffectRegistry.register("11E", PokeBattle_Move_11E::new);
        MoveEffectRegistry.register("11F", PokeBattle_Move_11F::new);
        MoveEffectRegistry.register("120", PokeBattle_Move_120::new);
        MoveEffectRegistry.register("121", PokeBattle_Move_121::new);
        MoveEffectRegistry.register("122", PokeBattle_Move_122::new);
        MoveEffectRegistry.register("123", PokeBattle_Move_123::new);
        MoveEffectRegistry.register("124", PokeBattle_Move_124::new);
        MoveEffectRegistry.register("125", PokeBattle_Move_125::new);
        MoveEffectRegistry.register("133", PokeBattle_Move_133::new);
        MoveEffectRegistry.register("134", PokeBattle_Move_134::new);
        MoveEffectRegistry.register("135", PokeBattle_Move_135::new);
        MoveEffectRegistry.register("136", PokeBattle_Move_136::new);
        MoveEffectRegistry.register("137", PokeBattle_Move_137::new);
        MoveEffectRegistry.register("138", PokeBattle_Move_138::new);
        MoveEffectRegistry.register("139", PokeBattle_Move_139::new);
        MoveEffectRegistry.register("13A", PokeBattle_Move_13A::new);
        MoveEffectRegistry.register("13B", PokeBattle_Move_13B::new);
        MoveEffectRegistry.register("13C", PokeBattle_Move_13C::new);
        MoveEffectRegistry.register("13D", PokeBattle_Move_13D::new);
        MoveEffectRegistry.register("13E", PokeBattle_Move_13E::new);
        MoveEffectRegistry.register("13F", PokeBattle_Move_13F::new);
        MoveEffectRegistry.register("140", PokeBattle_Move_140::new);
        MoveEffectRegistry.register("141", PokeBattle_Move_141::new);
        MoveEffectRegistry.register("142", PokeBattle_Move_142::new);
        MoveEffectRegistry.register("143", PokeBattle_Move_143::new);
        MoveEffectRegistry.register("144", PokeBattle_Move_144::new);
        MoveEffectRegistry.register("145", PokeBattle_Move_145::new);
        MoveEffectRegistry.register("146", PokeBattle_Move_146::new);
        MoveEffectRegistry.register("147", PokeBattle_Move_147::new);
        MoveEffectRegistry.register("148", PokeBattle_Move_148::new);
        MoveEffectRegistry.register("15A", PokeBattle_Move_15A::new);
        MoveEffectRegistry.register("163", PokeBattle_Move_163::new);
        MoveEffectRegistry.register("149", PokeBattle_Move_149::new);
        MoveEffectRegistry.register("14A", PokeBattle_Move_14A::new);
        MoveEffectRegistry.register("14B", PokeBattle_Move_14B::new);
        MoveEffectRegistry.register("14C", PokeBattle_Move_14C::new);
        MoveEffectRegistry.register("14D", PokeBattle_Move_14D::new);
        MoveEffectRegistry.register("14E", PokeBattle_Move_14E::new);
        MoveEffectRegistry.register("14F", PokeBattle_Move_14F::new);
        MoveEffectRegistry.register("150", PokeBattle_Move_150::new);
        MoveEffectRegistry.register("151", PokeBattle_Move_151::new);
        MoveEffectRegistry.register("152", PokeBattle_Move_152::new);
        MoveEffectRegistry.register("153", PokeBattle_Move_153::new);
        MoveEffectRegistry.register("154", PokeBattle_Move_154::new);
        MoveEffectRegistry.register("155", PokeBattle_Move_155::new);
        MoveEffectRegistry.register("156", PokeBattle_Move_156::new);
        MoveEffectRegistry.register("157", PokeBattle_Move_157::new);
        MoveEffectRegistry.register("158", PokeBattle_Move_158::new);
        MoveEffectRegistry.register("159", PokeBattle_Move_159::new);
        MoveEffectRegistry.register("15B", PokeBattle_Move_15B::new);
        MoveEffectRegistry.register("15C", PokeBattle_Move_15C::new);
        MoveEffectRegistry.register("15D", PokeBattle_Move_15D::new);
        MoveEffectRegistry.register("15E", PokeBattle_Move_15E::new);
        MoveEffectRegistry.register("15F", PokeBattle_Move_15F::new);
        MoveEffectRegistry.register("160", PokeBattle_Move_160::new);
        MoveEffectRegistry.register("161", PokeBattle_Move_161::new);
        MoveEffectRegistry.register("162", PokeBattle_Move_162::new);
        MoveEffectRegistry.register("164", PokeBattle_Move_164::new);
        MoveEffectRegistry.register("165", PokeBattle_Move_165::new);
        MoveEffectRegistry.register("166", PokeBattle_Move_166::new);
        MoveEffectRegistry.register("167", PokeBattle_Move_167::new);
        MoveEffectRegistry.register("168", PokeBattle_Move_168::new);
        MoveEffectRegistry.register("169", PokeBattle_Move_169::new);
        MoveEffectRegistry.register("16A", PokeBattle_Move_16A::new);
        MoveEffectRegistry.register("16B", PokeBattle_Move_16B::new);
        MoveEffectRegistry.register("16C", PokeBattle_Move_16C::new);
        MoveEffectRegistry.register("16D", PokeBattle_Move_16D::new);
        MoveEffectRegistry.register("16E", PokeBattle_Move_16E::new);
        MoveEffectRegistry.register("16F", PokeBattle_Move_16F::new);
        MoveEffectRegistry.register("170", PokeBattle_Move_170::new);
        MoveEffectRegistry.register("171", PokeBattle_Move_171::new);
        MoveEffectRegistry.register("172", PokeBattle_Move_172::new);
        MoveEffectRegistry.register("173", PokeBattle_Move_173::new);
        MoveEffectRegistry.register("174", PokeBattle_Move_174::new);
        MoveEffectRegistry.register("175", PokeBattle_Move_175::new);
        MoveEffectRegistry.register("176", PokeBattle_Move_176::new);
        MoveEffectRegistry.register("177", PokeBattle_Move_177::new);
        MoveEffectRegistry.register("178", PokeBattle_Move_178::new);
        MoveEffectRegistry.register("179", PokeBattle_Move_179::new);
        MoveEffectRegistry.register("17A", PokeBattle_Move_17A::new);
        MoveEffectRegistry.register("17B", PokeBattle_Move_17B::new);
        MoveEffectRegistry.register("17C", PokeBattle_Move_17C::new);
        MoveEffectRegistry.register("17D", PokeBattle_Move_17D::new);
        MoveEffectRegistry.register("17E", PokeBattle_Move_17E::new);
        MoveEffectRegistry.register("17F", PokeBattle_Move_17F::new);
    }

}
