package pokemon.runtime.battle.movefx;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.battle.Battle;
import pokemon.runtime.battle.BattleMove;
import pokemon.runtime.battle.Battler;
import pokemon.runtime.battle.DamageState;
import pokemon.runtime.battle.PBEffects;
import pokemon.runtime.battle.PBWeather;
import pokemon.runtime.battle.PendingApi;

/**
 * Stage 4 / L2: the 24 reusable move-effect base classes of the plugin's
 * {@code Move_Effects_Generic.rb} (794 lines), one Java class each, all
 * extending {@link MoveEffectBase}.
 *
 * <p>In the plugin these are ordinary subclasses of {@code PokeBattle_Move} that
 * the function-code classes inherit from - e.g.
 * {@code class PokeBattle_Move_070 < PokeBattle_FixedDamageMove}
 * (Move_Effects_000-07F:2478), {@code PokeBattle_Move_100 < PokeBattle_WeatherMove}
 * (Move_Effects_100-17F:4), {@code PokeBattle_Move_191 < PokeBattle_TwoTurnMove}
 * (Move_Effects_180-1FF:345) - 157 such declarations in this project's scripts.
 * They are nested here (instead of one file each) because task-11 keeps the
 * whole strategy block inside the {@code movefx} package's four files; the class
 * names are kept verbatim so every line can be diffed against the Ruby.</p>
 *
 * <h2>How the constructor arguments become fields</h2>
 * The plugin's {@code initialize(battle,move)} bodies do three different things
 * and they are handled differently here, because the strategy object is shared
 * by every move with the same function code (task-11 decision 4):
 * <ul>
 * <li>fields that describe the function code itself ({@code @toxic},
 *     {@code @sidedEffect}, {@code @weatherType}, {@code @statUp},
 *     {@code @statDown}, {@code @combos}, {@code @effect}) stay on the strategy
 *     and are set by the function-code subclass's constructor;</li>
 * <li>{@code PokeBattle_Confusion} and {@code PokeBattle_Struggle} build the
 *     whole move record instead - those become synthesized
 *     {@code PbsData.Move}s in {@link MoveEffectRegistry} (decision 14);</li>
 * <li>everything else on the move object already lives on {@link BattleMove}.</li>
 * </ul>
 */
public final class MoveEffectsGeneric {

    private MoveEffectsGeneric() {
    }

    // ==================================================================
    // Superclass that handles moves using a non-existent function code
    // (Move_Effects_Generic.rb:1-14)
    // ==================================================================

    /**
     * {@code class PokeBattle_UnimplementedMove} (Move_Effects_Generic.rb:6-14):
     * "Damaging moves just do damage with no additional effect. Status moves
     * always fail."
     */
    public static class PokeBattle_UnimplementedMove extends MoveEffectBase {

        /** The registry's default effect ({@link MoveEffectRegistry#of}). */
        public static final PokeBattle_UnimplementedMove INSTANCE = new PokeBattle_UnimplementedMove();

        /** {@code pbMoveFailed?(user,targets)} (:7-13). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (statusMove(move)) {                                          // :8
                user.battle.display("但是失败了！");   // :9
                return true;                                                 // :10
            }
            return false;                                                    // :12
        }
    }

    // ==================================================================
    // Pseudomove for confusion damage (Move_Effects_Generic.rb:18-45)
    // ==================================================================

    /**
     * {@code class PokeBattle_Confusion} (Move_Effects_Generic.rb:21-45): the
     * pseudomove confusion damage uses. Its {@code initialize} (:22-40) replaces
     * every move field, which is why the registry synthesizes the matching
     * {@link BattleMove} (decision 14).
     */
    public static class PokeBattle_Confusion extends MoveEffectBase {

        /** The singleton returned by {@link MoveEffectRegistry#confusion()}. */
        public static final PokeBattle_Confusion INSTANCE = new PokeBattle_Confusion();

        /** {@code physicalMove?(thisType=nil)} (:42). */
        @Override
        public boolean physicalMove(BattleMove move, String thisType) {
            return true;                                                     // :42
        }

        /** {@code specialMove?(thisType=nil)} (:43). */
        @Override
        public boolean specialMove(BattleMove move, String thisType) {
            return false;                                                    // :43
        }

        /** {@code pbCritialOverride(user,target)} (:44). */
        @Override
        public int pbCritialOverride(BattleMove move, Battler user, Battler target) {
            return -1;                                                       // :44
        }
    }

    // ==================================================================
    // Implements the move Struggle (Move_Effects_Generic.rb:49-83)
    // ==================================================================

    /**
     * {@code class PokeBattle_Struggle} (Move_Effects_Generic.rb:53-83): "For
     * cases where the real move named Struggle is not defined." Its
     * {@code initialize} (:54-72) replaces every move field, so the registry
     * synthesizes the {@link BattleMove} (decision 14).
     */
    public static class PokeBattle_Struggle extends MoveEffectBase {

        /** The singleton returned by {@link MoveEffectRegistry#struggle()}. */
        public static final PokeBattle_Struggle INSTANCE = new PokeBattle_Struggle();

        /** {@code physicalMove?(thisType=nil)} (:74). */
        @Override
        public boolean physicalMove(BattleMove move, String thisType) {
            return true;                                                     // :74
        }

        /** {@code specialMove?(thisType=nil)} (:75). */
        @Override
        public boolean specialMove(BattleMove move, String thisType) {
            return false;                                                    // :75
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:77-82). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (target.damageState.unaffected) {
                return;                                                      // :78
            }
            // :79 user.pbReduceHP((user.totalhp/4.0).round,false);
            //     Battler.maxHp() is this runtime's totalhp.
            user.pbReduceHP(Math.round(user.maxHp() / 4.0f), false, true, true);
            user.battle.display(user.pbThis() + "受到了反作用力的伤害！");        // :80
            user.pbItemHPHealCheck(0, false);                              // :81
        }
    }

    // ==================================================================
    // Generic status problem-inflicting classes (Move_Effects_Generic.rb:87-186)
    // ==================================================================

    /** {@code class PokeBattle_SleepMove} (Move_Effects_Generic.rb:90-105). */
    public static class PokeBattle_SleepMove extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:91-94). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {
                return false;                                                // :92
            }
            return !target.pbCanSleep(user, true, move, false);         // :93
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:96-99). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {
                return;                                                      // :97
            }
            target.pbSleep();                                      // :98
        }

        /** {@code pbAdditionalEffect(user,target)} (:101-104). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {
                return;                                                      // :102
            }
            if (target.pbCanSleep(user, false, move, false)) {          // :103
                target.pbSleep();
            }
        }
    }

    /** {@code class PokeBattle_PoisonMove} (Move_Effects_Generic.rb:109-129). */
    public static class PokeBattle_PoisonMove extends MoveEffectBase {

        /**
         * {@code @toxic = false} (:112 in {@code initialize}).
         *
         * <p>Set to {@code true} by the function-code subclasses that badly
         * poison (e.g. Toxic), which is why it is a field of the strategy: it is
         * a property of the function code, and the strategy is shared by every
         * move with that code.</p>
         */
        protected boolean toxic = false;

        /** {@code pbFailsAgainstTarget?(user,target)} (:115-118). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {
                return false;                                                // :116
            }
            return !target.pbCanPoison(user, true, move);        // :117
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:120-123). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {
                return;                                                      // :121
            }
            target.pbPoison(user, null, toxic);                   // :122
        }

        /** {@code pbAdditionalEffect(user,target)} (:125-128). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {
                return;                                                      // :126
            }
            if (target.pbCanPoison(user, false, move)) {         // :127
                target.pbPoison(user, null, toxic);
            }
        }
    }

    /** {@code class PokeBattle_ParalysisMove} (Move_Effects_Generic.rb:133-148). */
    public static class PokeBattle_ParalysisMove extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:134-137). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {
                return false;                                                // :135
            }
            return !target.pbCanParalyze(user, true, move);      // :136
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:139-142). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {
                return;                                                      // :140
            }
            target.pbParalyze(user, null);                             // :141
        }

        /** {@code pbAdditionalEffect(user,target)} (:144-147). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {
                return;                                                      // :145
            }
            if (target.pbCanParalyze(user, false, move)) {       // :146
                target.pbParalyze(user, null);
            }
        }
    }

    /** {@code class PokeBattle_BurnMove} (Move_Effects_Generic.rb:152-167). */
    public static class PokeBattle_BurnMove extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:153-156). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {
                return false;                                                // :154
            }
            return !target.pbCanBurn(user, true, move);          // :155
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:158-161). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {
                return;                                                      // :159
            }
            target.pbBurn(user, null);                                 // :160
        }

        /** {@code pbAdditionalEffect(user,target)} (:163-166). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {
                return;                                                      // :164
            }
            if (target.pbCanBurn(user, false, move)) {           // :165
                target.pbBurn(user, null);
            }
        }
    }

    /** {@code class PokeBattle_FreezeMove} (Move_Effects_Generic.rb:171-186). */
    public static class PokeBattle_FreezeMove extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:172-175). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {
                return false;                                                // :173
            }
            return !target.pbCanFreeze(user, true, move);        // :174
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:177-180). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {
                return;                                                      // :178
            }
            target.pbFreeze();                                     // :179
        }

        /** {@code pbAdditionalEffect(user,target)} (:182-185). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {
                return;                                                      // :183
            }
            if (target.pbCanFreeze(user, false, move)) {         // :184
                target.pbFreeze();
            }
        }
    }

    // ==================================================================
    // Other problem-causing classes (Move_Effects_Generic.rb:190-225)
    // ==================================================================

    /** {@code class PokeBattle_FlinchMove} (Move_Effects_Generic.rb:193-205). */
    public static class PokeBattle_FlinchMove extends MoveEffectBase {

        /** {@code flinchingMove?} (:194). */
        @Override
        public boolean flinchingMove(BattleMove move) {
            return true;                                                     // :194
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:196-199). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {
                return;                                                      // :197
            }
            target.pbFlinch(user);                               // :198
        }

        /** {@code pbAdditionalEffect(user,target)} (:201-204). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {
                return;                                                      // :202
            }
            target.pbFlinch(user);                               // :203
        }
    }

    /** {@code class PokeBattle_ConfuseMove} (Move_Effects_Generic.rb:209-225). */
    public static class PokeBattle_ConfuseMove extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:210-213). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {
                return false;                                                // :211
            }
            return !target.pbCanConfuse(user, true, move, false);       // :212
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:215-218). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {
                return;                                                      // :216
            }
            target.pbConfuse();                                    // :217
        }

        /** {@code pbAdditionalEffect(user,target)} (:220-224). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {
                return;                                                      // :221
            }
            if (!target.pbCanConfuse(user, false, move, false)) {
                return;                                                      // :222
            }
            target.pbConfuse();                                    // :223
        }
    }

    // ==================================================================
    // Generic user's stat increase/decrease classes (Move_Effects_Generic.rb:229-303)
    // ==================================================================

    /**
     * {@code class PokeBattle_StatUpMove} (Move_Effects_Generic.rb:232-248).
     *
     * <p>{@code @statUp} is a two-element array {@code [stat,increment]} set by
     * the function-code subclass (e.g.
     * {@code Move_Effects_000-07F:1175 @statUp = [PBStats::ATTACK,2]} in
     * {@code initialize}); {@code Move_Effects_000-07F:633-664} show the pattern
     * {@code class PokeBattle_Move_020 < PokeBattle_StatUpMove}.</p>
     */
    public static class PokeBattle_StatUpMove extends MoveEffectBase {

        /** {@code @statUp} - {@code [stat, increment]}, set by the subclass. */
        protected int[] statUp;

        /** {@code pbMoveFailed?(user,targets)} (:233-236). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (damagingMove(move)) {
                return false;                                                // :234
            }
            return !user.pbCanRaiseStatStage(statUp[0], user, move, true);   // :235
        }

        /** {@code pbEffectGeneral(user)} (:238-241). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            if (damagingMove(move)) {
                return;                                                      // :239
            }
            user.pbRaiseStatStage(statUp[0], statUp[1], user, true);          // :240
        }

        /** {@code pbAdditionalEffect(user,target)} (:243-247). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (user.pbCanRaiseStatStage(statUp[0], user, move)) {            // :244
                user.pbRaiseStatStage(statUp[0], statUp[1], user, true);      // :245
            }
        }
    }

    /**
     * {@code class PokeBattle_MultiStatUpMove} (Move_Effects_Generic.rb:252-288).
     *
     * <p>{@code @statUp} is the FLAT pair list {@code [stat1,inc1,stat2,inc2,...]}
     * (the Ruby loops {@code 0...@statUp.length/2} and indexes
     * {@code @statUp[i*2]}/{@code @statUp[i*2+1]}), e.g.
     * {@code Move_Effects_000-07F:1100 @statUp = [PBStats::ATTACK,1,PBStats::SPATK,1]}.</p>
     */
    public static class PokeBattle_MultiStatUpMove extends MoveEffectBase {

        /** {@code @statUp} - the flat {@code [stat, increment, ...]} list, set by the subclass. */
        protected int[] statUp;

        /** {@code pbMoveFailed?(user,targets)} (:253-266). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (damagingMove(move)) {
                return false;                                                // :254
            }
            boolean failed = true;                                           // :255
            for (int i = 0; i < statUp.length / 2; i++) {                    // :256
                if (!user.pbCanRaiseStatStage(statUp[i * 2], user, move)) {
                    continue;                                                // :257
                }
                failed = false;                                              // :258
                break;                                                       // :259
            }
            if (failed) {                                                    // :261
                user.battle.display(user.pbThis() + "的能力不能再提升了！");     // :262
                return true;                                                 // :263
            }
            return false;                                                    // :265
        }

        /** {@code pbEffectGeneral(user)} (:268-277). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            if (damagingMove(move)) {
                return;                                                      // :269
            }
            boolean showAnim = true;                                         // :270
            for (int i = 0; i < statUp.length / 2; i++) {                    // :271
                if (!user.pbCanRaiseStatStage(statUp[i * 2], user, move)) {
                    continue;                                                // :272
                }
                if (user.pbRaiseStatStage(statUp[i * 2], statUp[i * 2 + 1], user, showAnim)) {   // :273
                    showAnim = false;                                        // :274
                }
            }
        }

        /** {@code pbAdditionalEffect(user,target)} (:279-287) - note: no {@code damagingMove?} guard. */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            boolean showAnim = true;                                         // :280
            for (int i = 0; i < statUp.length / 2; i++) {                    // :281
                if (!user.pbCanRaiseStatStage(statUp[i * 2], user, move)) {
                    continue;                                                // :282
                }
                if (user.pbRaiseStatStage(statUp[i * 2], statUp[i * 2 + 1], user, showAnim)) {   // :283
                    showAnim = false;                                        // :284
                }
            }
        }
    }

    /**
     * {@code class PokeBattle_StatDownMove} (Move_Effects_Generic.rb:292-303).
     *
     * <p>{@code @statDown} is the flat {@code [stat, increment, ...]} list set by
     * the subclass, e.g. {@code Arceus:572 class PokeBattle_Move_207 < PokeBattle_StatDownMove}.</p>
     */
    public static class PokeBattle_StatDownMove extends MoveEffectBase {

        /** {@code @statDown} - the flat {@code [stat, increment, ...]} list, set by the subclass. */
        protected int[] statDown;

        /** {@code pbEffectWhenDealingDamage(user,target)} (:293-302). */
        @Override
        public void pbEffectWhenDealingDamage(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            if (battle.pbAllFainted(target.idxOwnSide())) {    // :294
                return;
            }
            boolean showAnim = true;                                         // :295
            for (int i = 0; i < statDown.length / 2; i++) {                  // :296
                if (!user.pbCanLowerStatStage(statDown[i * 2], user, move)) {   // :297
                    continue;
                }
                if (user.pbLowerStatStage(statDown[i * 2], statDown[i * 2 + 1], user, showAnim)) {  // :298
                    showAnim = false;                                        // :299
                }
            }
        }
    }

    // ==================================================================
    // Generic target's stat increase/decrease classes (Move_Effects_Generic.rb:307-466)
    // ==================================================================

    /** {@code class PokeBattle_TargetStatUpMove} (Move_Effects_Generic.rb:310-326). */
    public static class PokeBattle_TargetStatUpMove extends MoveEffectBase {

        /** {@code @statUp} - {@code [stat, increment]}, set by the subclass. */
        protected int[] statUp;

        /** {@code pbFailsAgainstTarget?(user,target)} (:311-314). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {
                return false;                                                // :312
            }
            return !target.pbCanRaiseStatStage(statUp[0], user, move, true);   // :313
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:316-319). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {
                return;                                                      // :317
            }
            target.pbRaiseStatStage(statUp[0], statUp[1], user, true);         // :318
        }

        /** {@code pbAdditionalEffect(user,target)} (:321-325). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {
                return;                                                      // :322
            }
            if (!target.pbCanRaiseStatStage(statUp[0], user, move)) {
                return;                                                      // :323
            }
            target.pbRaiseStatStage(statUp[0], statUp[1], user, true);         // :324
        }
    }

    /** {@code class PokeBattle_TargetMultiStatUpMove} (Move_Effects_Generic.rb:330-387). */
    public static class PokeBattle_TargetMultiStatUpMove extends MoveEffectBase {

        /** {@code @statUp} - the flat {@code [stat, increment, ...]} list, set by the subclass. */
        protected int[] statUp;

        /** {@code pbFailsAgainstTarget?(user,target)} (:331-364). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {
                return false;                                                // :332
            }
            boolean failed = true;                                           // :333
            for (int i = 0; i < statUp.length / 2; i++) {                    // :334
                if (!target.pbCanRaiseStatStage(statUp[i * 2], user, move)) {
                    continue;                                                // :335
                }
                failed = false;                                              // :336
                break;                                                       // :337
            }
            if (failed) {                                                    // :339
                Battle battle = user.battle;
                boolean canRaise = false;                                    // :342
                if (target.hasActiveAbility("CONTRARY") && !battle.moldBreaker) {   // :343
                    for (int i = 0; i < statUp.length / 2; i++) {             // :344
                        if (target.statStageAtMin(statUp[i * 2])) {
                            continue;                                        // :345
                        }
                        canRaise = true;                                     // :346
                        break;                                               // :347
                    }
                    if (!canRaise) {                                         // :349
                        battle.display(target.pbThis() + "的能力不能再降低了！");   // :349
                    }
                } else {                                                     // :350
                    for (int i = 0; i < statUp.length / 2; i++) {             // :351
                        if (target.statStageAtMax(statUp[i * 2])) {
                            continue;                                        // :352
                        }
                        canRaise = true;                                     // :353
                        break;                                               // :354
                    }
                    if (!canRaise) {                                         // :356
                        battle.display(target.pbThis() + "的能力不能再提升了！");   // :356
                    }
                }
                if (canRaise) {                                              // :358
                    // :359 target.pbCanRaiseStatStage?(@statUp[0],user,self,true) - called for its message only
                    target.pbCanRaiseStatStage(statUp[0], user, move, true);
                }
                return true;                                                 // :361
            }
            return false;                                                    // :363
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:366-375). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {
                return;                                                      // :367
            }
            boolean showAnim = true;                                         // :368
            for (int i = 0; i < statUp.length / 2; i++) {                    // :369
                if (!target.pbCanRaiseStatStage(statUp[i * 2], user, move)) {
                    continue;                                                // :370
                }
                if (target.pbRaiseStatStage(statUp[i * 2], statUp[i * 2 + 1], user, showAnim)) {  // :371
                    showAnim = false;                                        // :372
                }
            }
        }

        /**
         * {@code pbAdditionalEffect(user,target)} (:377-386).
         *
         * <p>Plugin quirk reproduced as-is: this is the stat-<b>up</b> class but
         * the guard calls {@code target.pbCanLowerStatStage?} (:381) while the
         * effect calls {@code target.pbRaiseStatStage} (:382).</p>
         */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {
                return;                                                      // :378
            }
            boolean showAnim = true;                                         // :379
            for (int i = 0; i < statUp.length / 2; i++) {                    // :380
                if (!target.pbCanLowerStatStage(statUp[i * 2], user, move)) {   // :381
                    continue;
                }
                if (target.pbRaiseStatStage(statUp[i * 2], statUp[i * 2 + 1], user, showAnim)) {  // :382
                    showAnim = false;                                        // :383
                }
            }
        }
    }

    /** {@code class PokeBattle_TargetStatDownMove} (Move_Effects_Generic.rb:389-405). */
    public static class PokeBattle_TargetStatDownMove extends MoveEffectBase {

        /** {@code @statDown} - {@code [stat, increment]}, set by the subclass. */
        protected int[] statDown;

        /** {@code pbFailsAgainstTarget?(user,target)} (:390-393). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {
                return false;                                                // :391
            }
            return !target.pbCanLowerStatStage(statDown[0], user, move, true);   // :392
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:395-398). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {
                return;                                                      // :396
            }
            target.pbLowerStatStage(statDown[0], statDown[1], user, true);       // :397
        }

        /** {@code pbAdditionalEffect(user,target)} (:400-404). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {
                return;                                                      // :401
            }
            if (!target.pbCanLowerStatStage(statDown[0], user, move)) {
                return;                                                      // :402
            }
            target.pbLowerStatStage(statDown[0], statDown[1], user, true);       // :403
        }
    }

    /** {@code class PokeBattle_TargetMultiStatDownMove} (Move_Effects_Generic.rb:409-466). */
    public static class PokeBattle_TargetMultiStatDownMove extends MoveEffectBase {

        /** {@code @statDown} - the flat {@code [stat, increment, ...]} list, set by the subclass. */
        protected int[] statDown;

        /** {@code pbFailsAgainstTarget?(user,target)} (:410-443). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {
                return false;                                                // :411
            }
            boolean failed = true;                                           // :412
            for (int i = 0; i < statDown.length / 2; i++) {                  // :413
                if (!target.pbCanLowerStatStage(statDown[i * 2], user, move)) {
                    continue;                                                // :414
                }
                failed = false;                                              // :415
                break;                                                       // :416
            }
            if (failed) {                                                    // :418
                Battle battle = user.battle;
                boolean canLower = false;                                    // :421
                if (target.hasActiveAbility("CONTRARY") && !battle.moldBreaker) {   // :422
                    for (int i = 0; i < statDown.length / 2; i++) {           // :423
                        if (target.statStageAtMax(statDown[i * 2])) {
                            continue;                                        // :424
                        }
                        canLower = true;                                     // :425
                        break;                                               // :426
                    }
                    if (!canLower) {                                         // :428
                        battle.display(target.pbThis() + "的能力不能再提升了！");   // :428
                    }
                } else {                                                     // :429
                    for (int i = 0; i < statDown.length / 2; i++) {           // :430
                        if (target.statStageAtMin(statDown[i * 2])) {
                            continue;                                        // :431
                        }
                        canLower = true;                                     // :432
                        break;                                               // :433
                    }
                    if (!canLower) {                                         // :435
                        battle.display(target.pbThis() + "的能力不能再降低了！");   // :435
                    }
                }
                if (canLower) {                                              // :437
                    // :438 target.pbCanLowerStatStage?(@statDown[0],user,self,true) - message only
                    target.pbCanLowerStatStage(statDown[0], user, move, true);
                }
                return true;                                                 // :440
            }
            return false;                                                    // :442
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:445-454). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {
                return;                                                      // :446
            }
            boolean showAnim = true;                                         // :447
            for (int i = 0; i < statDown.length / 2; i++) {                  // :448
                if (!target.pbCanLowerStatStage(statDown[i * 2], user, move)) {
                    continue;                                                // :449
                }
                if (target.pbLowerStatStage(statDown[i * 2], statDown[i * 2 + 1], user, showAnim)) {  // :450
                    showAnim = false;                                        // :451
                }
            }
        }

        /** {@code pbAdditionalEffect(user,target)} (:456-465). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {
                return;                                                      // :457
            }
            boolean showAnim = true;                                         // :458
            for (int i = 0; i < statDown.length / 2; i++) {                  // :459
                if (!target.pbCanLowerStatStage(statDown[i * 2], user, move)) {
                    continue;                                                // :460
                }
                if (target.pbLowerStatStage(statDown[i * 2], statDown[i * 2 + 1], user, showAnim)) {  // :461
                    showAnim = false;                                        // :462
                }
            }
        }
    }

    // ==================================================================
    // Fixed damage-inflicting move (Move_Effects_Generic.rb:470-481)
    // ==================================================================

    /** {@code class PokeBattle_FixedDamageMove} (Move_Effects_Generic.rb:473-481). */
    public static class PokeBattle_FixedDamageMove extends MoveEffectBase {

        /** {@code pbFixedDamage(user,target); return 1; end} (:474). */
        @Override
        public int pbFixedDamage(BattleMove move, Battler user, Battler target) {
            return 1;                                                        // :474
        }

        /** {@code pbCalcDamage(user,target,numTargets=1)} (:476-480). */
        @Override
        public void pbCalcDamage(BattleMove move, Battler user, Battler target, int numTargets) {
            DamageState damageState = target.damageState;
            damageState.critical = false;                                    // :477
            damageState.calcDamage = pbFixedDamage(move, user, target);      // :478
            if (damageState.calcDamage < 1) {                                // :479
                damageState.calcDamage = 1;
            }
        }
    }

    // ==================================================================
    // Two turn move (Move_Effects_Generic.rb:485-561)
    // ==================================================================

    /** {@code class PokeBattle_TwoTurnMove} (Move_Effects_Generic.rb:488-561). */
    public static class PokeBattle_TwoTurnMove extends MoveEffectBase {

        /** {@code @powerHerb} (:495/:500). */
        protected boolean powerHerb;
        /** {@code @chargingTurn} (:496/:501). */
        protected boolean chargingTurn;
        /** {@code @damagingTurn} (:497/:502). */
        protected boolean damagingTurn;

        /** {@code chargingTurnMove?; return true; end} (:489). */
        @Override
        public boolean chargingTurnMove(BattleMove move) {
            return true;                                                     // :489
        }

        /** {@code pbIsChargingTurn?(user)} (:494-505). */
        @Override
        public boolean pbIsChargingTurn(BattleMove move, Battler user) {
            powerHerb = false;                                               // :495
            chargingTurn = false;                                            // :496 Assume damaging turn by default
            damagingTurn = true;                                             // :497
            if (user.effects.intVal(PBEffects.Battler.TwoTurnAttack) == 0) {   // :499
                powerHerb = user.hasActiveItem("POWERHERB");     // :500
                chargingTurn = true;                                         // :501
                damagingTurn = powerHerb;                                    // :502
            }
            return !damagingTurn;                                            // :504 Deliberately not "return @chargingTurn"
        }

        /** {@code pbDamagingMove?} (:507-510) - "Stops damage being dealt in the first (charging) turn". */
        @Override
        public boolean pbDamagingMove(BattleMove move) {
            if (!damagingTurn) {
                return false;                                                // :508
            }
            return super.pbDamagingMove(move);                               // :509 return super
        }

        /** {@code pbAccuracyCheck(user,target)} (:512-515). */
        @Override
        public boolean pbAccuracyCheck(BattleMove move, Battler user, Battler target) {
            if (!damagingTurn) {
                return true;                                                 // :513
            }
            return super.pbAccuracyCheck(move, user, target);                // :514 return super
        }

        /** {@code pbInitialEffect(user,targets,hitNum)} (:517-534). */
        @Override
        public void pbInitialEffect(BattleMove move, Battler user, Array<Battler> targets, int hitNum) {
            if (chargingTurn) {                                              // :518
                pbChargingTurnMessage(move, user, targets);
            }
            if (chargingTurn && damagingTurn) {                              // :519 Move only takes one turn to use
                pbShowAnimation(move, move.id(), user, targets, 1, true);    // :520 Charging anim
                for (Battler b : targets) {                                  // :521
                    pbChargingTurnEffect(move, user, b);
                }
                if (powerHerb) {                                             // :522
                    // :526 if !["0C9","0CA","0CB","0CC","0CD","0CE","14D"].include?(@function)
                    if (!isSemiInvulnerableFunction(move.function())) {
                        // :527 @battle.pbCommonAnimation("UseItem",user)
                        PendingApi.pbCommonAnimation(user.battle, "UseItem", user);
                    }
                    user.battle.display(user.pbThis() + "因强力香草充满了力量！");   // :529
                    user.pbConsumeItem();                          // :530
                }
            }
            if (damagingTurn) {                                              // :533
                pbAttackingTurnMessage(move, user, targets);
            }
        }

        /** The {@code ["0C9",...,"14D"]} literal of :526. */
        private static boolean isSemiInvulnerableFunction(String function) {
            for (String code : SEMI_INVULNERABLE_FUNCTIONS) {
                if (code.equals(function)) {
                    return true;
                }
            }
            return false;
        }

        /** {@code ["0C9","0CA","0CB","0CC","0CD","0CE","14D"]} (:526). */
        private static final String[] SEMI_INVULNERABLE_FUNCTIONS =
                { "0C9", "0CA", "0CB", "0CC", "0CD", "0CE", "14D" };

        /** {@code pbChargingTurnMessage(user,targets)} (:536-538). */
        @Override
        public void pbChargingTurnMessage(BattleMove move, Battler user, Array<Battler> targets) {
            user.battle.display(user.pbThis() + "开始充电!");                       // :537
        }

        /** {@code pbAttackingTurnMessage(user,targets)} (:540-541) - empty. */
        @Override
        public void pbAttackingTurnMessage(BattleMove move, Battler user, Array<Battler> targets) {
            // :540-541 empty
        }

        /** {@code pbChargingTurnEffect(user,target)} (:543-546) - empty. */
        @Override
        public void pbChargingTurnEffect(BattleMove move, Battler user, Battler target) {
            // :543-546 empty (Skull Bash/Sky Drop are the only users)
        }

        /** {@code pbAttackingTurnEffect(user,target)} (:548-549) - empty. */
        @Override
        public void pbAttackingTurnEffect(BattleMove move, Battler user, Battler target) {
            // :548-549 empty
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:551-555). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingTurn) {                                              // :552
                pbAttackingTurnEffect(move, user, target);
            } else if (chargingTurn) {                                       // :553
                pbChargingTurnEffect(move, user, target);
            }
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} (:557-560). */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets, int hitNum,
                                    boolean showAnimation) {
            int animHitNum = hitNum;
            if (chargingTurn && !damagingTurn) {                             // :558
                animHitNum = 1;                                              //     hitNum = 1 if @chargingTurn && !@damagingTurn
            }
            super.pbShowAnimation(move, id, user, targets, animHitNum, showAnimation);   // :559 super
        }
    }

    // ==================================================================
    // Healing move (Move_Effects_Generic.rb:565-585)
    // ==================================================================

    /** {@code class PokeBattle_HealingMove} (Move_Effects_Generic.rb:568-585). */
    public static class PokeBattle_HealingMove extends MoveEffectBase {

        /** {@code healingMove?; return true; end} (:569). */
        @Override
        public boolean healingMove(BattleMove move) {
            return true;                                                     // :569
        }

        /** {@code pbHealAmount(user); return 1; end} (:570). */
        @Override
        public int pbHealAmount(BattleMove move, Battler user) {
            return 1;                                                        // :570
        }

        /** {@code pbMoveFailed?(user,targets)} (:572-578). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.hp == user.maxHp()) {                                   // :573 user.hp==user.totalhp
                user.battle.display(user.pbThis() + "的HP已经满了！");             // :574
                return true;                                                 // :575
            }
            return false;                                                    // :577
        }

        /** {@code pbEffectGeneral(user)} (:580-584). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            int amt = pbHealAmount(move, user);                              // :581
            user.pbRecoverHP(amt);                               // :582
            user.battle.display(user.pbThis() + "的HP回复了。");                 // :583
        }
    }

    // ==================================================================
    // Recoil move (Move_Effects_Generic.rb:589-606)
    // ==================================================================

    /** {@code class PokeBattle_RecoilMove} (Move_Effects_Generic.rb:592-606). */
    public static class PokeBattle_RecoilMove extends MoveEffectBase {

        /** {@code recoilMove?; return true; end} (:593). */
        @Override
        public boolean recoilMove(BattleMove move) {
            return true;                                                     // :593
        }

        /** {@code pbRecoilDamage(user,target); return 1; end} (:594). */
        @Override
        public int pbRecoilDamage(BattleMove move, Battler user, Battler target) {
            return 1;                                                        // :594
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:596-605). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (target.damageState.unaffected) {
                return;                                                      // :597
            }
            if (!user.takesIndirectDamage(false)) {
                return;                                                      // :598
            }
            if (user.hasActiveAbility("ROCKHEAD")
                    || user.hasActiveAbility("RAGEFIREBLAST")) {  // :599
                return;
            }
            int amt = pbRecoilDamage(move, user, target);                    // :600
            if (amt < 1) {                                                   // :601
                amt = 1;
            }
            user.pbReduceHP(amt, false, true, true);                         // :602
            user.battle.display(user.pbThis() + "受到了反作用力的伤害！");        // :603
            user.pbItemHPHealCheck(0, false);                              // :604
        }
    }

    // ==================================================================
    // Protect move (Move_Effects_Generic.rb:610-668)
    // ==================================================================

    /**
     * {@code class PokeBattle_ProtectMove} (Move_Effects_Generic.rb:613-668).
     *
     * <p>Plugin quirk kept as-is: {@code initialize} only sets
     * {@code @sidedEffect = false} (:616) - {@code @effect} is left UNSET and is
     * assigned by each function-code subclass's own {@code initialize}. Task-11
     * decision 16: the field is an {@code Integer} that is {@code null} until
     * then, exactly like Ruby's {@code nil}, so reading it before it is set
     * fails instead of silently using effect index 0.</p>
     */
    public static class PokeBattle_ProtectMove extends MoveEffectBase {

        /** {@code @sidedEffect = false} (:616). */
        protected boolean sidedEffect = false;

        /**
         * {@code @effect} - the {@code PBEffects} index the protection writes;
         * set by the function-code subclass, {@code null} (= Ruby {@code nil})
         * until then (decision 16).
         */
        protected Integer effect;

        /**
         * {@code @effect} read as an effect index. Ruby raises {@code TypeError}
         * for {@code effects[nil]}; unboxing {@code null} reproduces that failure
         * rather than inventing an index.
         */
        private int effectIndex() {
            return effect;
        }

        /** {@code pbChangeUsageCounters(user,specialUsage)} (:619-623). */
        @Override
        public void pbChangeUsageCounters(BattleMove move, Battler user, boolean specialUsage) {
            int oldVal = user.effects.intVal(PBEffects.Battler.ProtectRate);   // :620
            super.pbChangeUsageCounters(move, user, specialUsage);                          // :621
            user.effects.set(PBEffects.Battler.ProtectRate, oldVal);            // :622
        }

        /** {@code pbMoveFailed?(user,targets)} (:625-649). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = user.battle;
            if (sidedEffect) {                                               // :626
                if (user.pbOwnSide().effects.truthy(effectIndex())) {   // :627
                    user.effects.set(PBEffects.Battler.ProtectRate, 1);   // :628
                    battle.display("但是失败了！");                    // :629
                    return true;                                                 // :630
                }
            } else if (user.effects.truthy(effectIndex())) {      // :632
                user.effects.set(PBEffects.Battler.ProtectRate, 1);   // :633
                battle.display("但是失败了！");                        // :634
                return true;                                                     // :635
            }
            // :637-643 "but it failed" chance
            if (!(sidedEffect && Battle.NEWEST_BATTLE_MECHANICS)
                    && user.effects.intVal(PBEffects.Battler.ProtectRate) > 1
                    && battle.pbRandom(user.effects.intVal(PBEffects.Battler.ProtectRate)) != 0) {
                user.effects.set(PBEffects.Battler.ProtectRate, 1);   // :640
                battle.display("但是失败了！");                        // :641
                return true;                                                     // :642
            }
            if (pbMoveFailedLastInRound(move, user)) {                       // :644
                user.effects.set(PBEffects.Battler.ProtectRate, 1);   // :645
                return true;                                                     // :646
            }
            return false;                                                    // :648
        }

        /** {@code pbEffectGeneral(user)} (:651-659). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            if (sidedEffect) {                                               // :652
                user.pbOwnSide().effects.set(effectIndex(), true);      // :653
            } else {
                user.effects.set(effectIndex(), true);                // :655
            }
            // :657 user.effects[ProtectRate] *= (NEWEST_BATTLE_MECHANICS) ? 3 : 2
            user.effects.set(PBEffects.Battler.ProtectRate,
                    user.effects.intVal(PBEffects.Battler.ProtectRate)
                            * (Battle.NEWEST_BATTLE_MECHANICS ? 3 : 2));
            pbProtectMessage(move, user);                                    // :658
        }

        /** {@code pbProtectMessage(user)} (:661-667). */
        @Override
        public void pbProtectMessage(BattleMove move, Battler user) {
            if (sidedEffect) {                                               // :662
                // :663 @battle.pbDisplay(_INTL("{1}保护了{2}！",@name,user.pbTeam(true)))
                user.battle.display(move.name() + "保护了" + user.pbTeam(true) + "！");
            } else {
                // :665 @battle.pbDisplay(_INTL("{1}保护了自己！",user.pbThis))
                user.battle.display(user.pbThis() + "保护了自己！");
            }
        }
    }

    // ==================================================================
    // Weather-inducing move (Move_Effects_Generic.rb:672-701)
    // ==================================================================

    /**
     * {@code class PokeBattle_WeatherMove} (Move_Effects_Generic.rb:675-701).
     *
     * <p>{@code @weatherType = PBWeather::None} (:678) is overridden by each
     * function-code subclass ({@code Move_Effects_100-17F:4-28}: Sun/Rain/Sandstorm).</p>
     */
    public static class PokeBattle_WeatherMove extends MoveEffectBase {

        /** {@code @weatherType} (:678); the function-code subclass overrides it. */
        protected int weatherType = PBWeather.None;

        /** {@code pbMoveFailed?(user,targets)} (:680-696). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = user.battle;
            int weather = battle.field.weather;
            // :681-694 the Ruby is a case/when; the last branch (`when @weatherType`)
            //         is not a constant, so it is the final else-if here.
            if (weather == PBWeather.HarshSun) {                             // :682-684
                battle.display("强烈的阳光没有减弱！");
                return true;
            }
            if (weather == PBWeather.HeavyRain) {                            // :685-687
                battle.display("暴雨的势头依旧不减！");
                return true;
            }
            if (weather == PBWeather.StrongWinds) {                          // :688-690
                battle.display("神秘的乱流仍然持续着！");
                return true;
            }
            if (weather == weatherType) {                                    // :691-693
                battle.display("但是失败了！");
                return true;
            }
            return false;                                                    // :695
        }

        /** {@code pbEffectGeneral(user)} (:698-700). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            // :699 @battle.pbStartWeather(user,@weatherType,true,false)
            // Ruby's 4th parameter is noAnimation=false; Battle#pbStartWeather's 4th is
            // showAnim (Battle.java), so it is !noAnimation = true.
            user.battle.pbStartWeather(user, weatherType, true, true);
        }
    }

    // ==================================================================
    // Pledge move (Move_Effects_Generic.rb:705-793)
    // ==================================================================

    /**
     * {@code class PokeBattle_PledgeMove} (Move_Effects_Generic.rb:708-793).
     *
     * <p>{@code @combos} is set by the three function-code subclasses
     * ({@code Move_Effects_100-17F:106/122/138}: Grass/Fire/Water Pledge); each
     * entry is the Ruby array {@code [function, comboEffect, overrideType,
     * overrideAnim]} (:713/:726 {@code i[0]} ... {@code i[3]}).</p>
     */
    public static class PokeBattle_PledgeMove extends MoveEffectBase {

        /** {@code @combos} - the subclass's combo table ({@code i[0]..i[3]}). */
        protected PledgeCombo[] combos = new PledgeCombo[0];

        /** {@code @pledgeSetup} (:710). */
        protected boolean pledgeSetup;
        /** {@code @pledgeCombo} (:710). */
        protected boolean pledgeCombo;
        /** {@code @pledgeOtherUser} (:710). */
        protected Battler pledgeOtherUser;
        /** {@code @comboEffect} (:711). */
        protected ComboEffect comboEffect;
        /** {@code @overrideType} (:711) - a type internal name, {@code null} = Ruby {@code nil}. */
        protected String overrideType;
        /** {@code @overrideAnim} (:711) - a move's internal name (Ruby: its id), {@code null} = Ruby {@code nil}. */
        protected String overrideAnim;

        /** One entry of {@code @combos} ({@code [function, comboEffect, overrideType, overrideAnim]}). */
        public static final class PledgeCombo {
            /** {@code i[0]} - the other move's function code. */
            public final String function;
            /** {@code i[1]} - one of the {@link ComboEffect} values. */
            public final ComboEffect effect;
            /** {@code i[2]} - the type the combo turns this move into; {@code null} = Ruby {@code nil}. */
            public final String overrideType;
            /**
             * {@code i[3]} - the animation id the combo plays; {@code null} = Ruby
             * {@code nil}. It MUST stay nullable: the three function-code classes
             * pass {@code nil} for their second combo entry
             * (Move_Effects_100-17F.rb:106/122/138, e.g. {@code ["108",:Swamp,nil,nil]}),
             * and {@code pbShowAnimation} decides "do not override the animation" by
             * {@code @overrideAnim != nil} (Move_Effects_Generic.rb:790) - an {@code int}
             * could not tell {@code nil} from a real id {@code 0}.
             */
            public final String overrideAnim;   // the move's internal name (Ruby: its id)

            public PledgeCombo(String function, ComboEffect effect, String overrideType, String overrideAnim) {
                this.function = function;
                this.effect = effect;
                this.overrideType = overrideType;
                this.overrideAnim = overrideAnim;
            }
        }

        /**
         * The three combo symbols of {@code @comboEffect}
         * ({@code :SeaOfFire}/{@code :Rainbow}/{@code :Swamp}, :765/:771/:777).
         */
        public enum ComboEffect {
            /** {@code :SeaOfFire} (:765) - Grass + Fire. */
            SEA_OF_FIRE,
            /** {@code :Rainbow} (:771) - Fire + Water. */
            RAINBOW,
            /** {@code :Swamp} (:777) - Water + Grass. */
            SWAMP
        }

        /** {@code pbOnStartUse(user,targets)} (:709-734). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            pledgeSetup = false;
            pledgeCombo = false;
            pledgeOtherUser = null;                                          // :710
            comboEffect = null;
            overrideType = null;
            overrideAnim = null;                                             // :711
            Battle battle = user.battle;
            for (PledgeCombo combo : combos) {                               // :713
                // :714 next if i[0]!=user.effects[PBEffects::FirstPledge]
                if (!combo.function.equals(user.effects.stringVal(PBEffects.Battler.FirstPledge))) {
                    continue;
                }
                battle.display("两个招式结合成了组合招式！");      // :715
                pledgeCombo = true;                                          // :716
                comboEffect = combo.effect;                                  // :717 i[1]
                overrideType = combo.overrideType;                           //      i[2]
                overrideAnim = combo.overrideAnim;                           //      i[3]
                break;                                                       // :718
            }
            if (pledgeCombo) {                                               // :720
                return;
            }
            for (Battler b : user.allAllies()) {                     // :722
                // :723 next if choices[b.index][0]!=:UseMove || b.movedThisRound?
                if (!choiceIsMoveOrShift(battle, b.index) || b.movedThisRound()) {
                    continue;
                }
                BattleMove other = (BattleMove) battle.choices(b.index)[2];    // :724 choices[b.index][2]
                if (other == null || other.id() <= 0) {                      // :725
                    continue;
                }
                for (PledgeCombo combo : combos) {                            // :726
                    if (!combo.function.equals(other.function())) {           // :727 i[0]!=move.function
                        continue;
                    }
                    pledgeSetup = true;                                       // :728
                    pledgeOtherUser = b;                                      // :729
                    break;                                                    // :730
                }
                if (pledgeSetup) {                                            // :732
                    break;
                }
            }
        }

        /** {@code pbDamagingMove?} (:736-739). */
        @Override
        public boolean pbDamagingMove(BattleMove move) {
            if (pledgeSetup) {
                return false;                                                // :737
            }
            return super.pbDamagingMove(move);                               // :738 return super
        }

        /** {@code pbBaseType(user)} (:741-744). */
        @Override
        public String pbBaseType(BattleMove move, Battler user) {
            if (overrideType != null) {                                      // :742 @overrideType!=nil
                return overrideType;
            }
            return super.pbBaseType(move, user);                             // :743 return super
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:746-749). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            int ret = baseDmg;
            if (pledgeCombo) {                                               // :747
                ret *= 2;
            }
            return ret;                                                      // :748
        }

        /** {@code pbEffectGeneral(user)} (:751-759). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.FirstPledge, 0);   // :752
            if (!pledgeSetup) {                                               // :753
                return;
            }
            // :754-755 @battle.pbDisplay(_INTL("{1}等待着{2}行动……",user.pbThis,@pledgeOtherUser.pbThis(true)))
            user.battle.display(user.pbThis() + "等待着"
                    + pledgeOtherUser.pbThis(true) + "行动……");
            pledgeOtherUser.effects.set(PBEffects.Battler.FirstPledge, move.function());   // :756
            pledgeOtherUser.effects.set(PBEffects.Battler.MoveNext, true);                 // :757
            user.lastMoveFailed = true;                         // :758 Treated as a failure for Stomping Tantrum
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:761-786). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (!pledgeCombo) {                                              // :762
                return;
            }
            Battle battle = user.battle;
            String msg = null;
            String animName = null;                                          // :763
            if (comboEffect == ComboEffect.SEA_OF_FIRE) {                    // :765 Grass + Fire
                if (user.pbOpposingSide().effects.intVal(PBEffects.Side.SeaOfFire) == 0) {   // :766
                    user.pbOpposingSide().effects.set(PBEffects.Side.SeaOfFire, 4);          // :767
                    // :768 _INTL("一片火海包围了{1}！",user.pbOpposingTeam(true))
                    msg = "一片火海包围了" + user.pbOpposingTeam(true) + "！";
                    // :769 animName = (user.opposes?) ? "SeaOfFire" : "SeaOfFireOpp"
                    animName = user.opposes(0) ? "SeaOfFire" : "SeaOfFireOpp";
                }
            } else if (comboEffect == ComboEffect.RAINBOW) {                 // :771 Fire + Water
                if (user.pbOwnSide().effects.intVal(PBEffects.Side.Rainbow) == 0) {          // :772
                    user.pbOwnSide().effects.set(PBEffects.Side.Rainbow, 4);                 // :773
                    msg = "一道彩虹出现在" + user.pbTeam(true) + "身边！";                   // :774
                    animName = user.opposes(0) ? "RainbowOpp" : "Rainbow";                    // :775
                }
            } else if (comboEffect == ComboEffect.SWAMP) {                   // :777 Water + Grass
                if (user.pbOpposingSide().effects.intVal(PBEffects.Side.Swamp) == 0) {       // :778
                    user.pbOpposingSide().effects.set(PBEffects.Side.Swamp, 4);               // :779
                    msg = "一片沼泽包围了" + user.pbOpposingTeam(true) + "！";                // :780
                    animName = user.opposes(0) ? "Swamp" : "SwampOpp";                         // :781
                }
            }
            if (msg != null) {                                               // :784
                battle.display(msg);
            }
            if (animName != null) {                                          // :785
                PendingApi.pbCommonAnimation(battle, animName, null);
            }
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} (:788-792). */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets, int hitNum,
                                    boolean showAnimation) {
            if (pledgeSetup) {                                               // :789
                return;                                                      // No animation for setting up
            }
            int animId = id;
            // :790 id = @overrideAnim if @overrideAnim!=nil
            // The nil test is why the field is an Integer (see PledgeCombo#overrideAnim):
            // Move_Effects_100-17F.rb:106/122/138 pass nil for the second combo entry.
            if (overrideAnim != null) {                                      // :790 @overrideAnim!=nil
                animId = user.battle.pbs().move(overrideAnim).id;           // the move id of that name
            }
            super.pbShowAnimation(move, animId, user, targets, hitNum, showAnimation);   // :791 return super
        }
    }
}
