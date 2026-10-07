package pokemon.runtime.battle.movefx;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;

import pokemon.runtime.battle.Battle;
import pokemon.runtime.battle.BattleMove;
import pokemon.runtime.battle.Battler;
import pokemon.runtime.battle.PBEffects;
import pokemon.runtime.battle.PBStats;
import pokemon.runtime.battle.PBWeather;
import pokemon.runtime.battle.PokeBattle_SceneConstants;
import pokemon.runtime.pokemon.PbsData;

/**
 * Stage 4 / L2: the function-code classes {@code 0D5}-{@code 0FF} (<b>43</b> of
 * them) of the plugin's {@code Move_Effects_080-0FF.rb} (3921 lines), transcribed
 * one class at a time.
 *
 * <p>The rest of that Ruby file lives in the sibling files of this package:
 * {@code MoveEffects_080_0AF.java} ({@code 080}-{@code 0AF}) and
 * {@code MoveEffects_0B0_0D4.java} ({@code 0B0}-{@code 0D4}). Every class here
 * whose Ruby parent is {@code PokeBattle_Move} extends {@link MoveEffectBase};
 * the ones whose parent is one of the 24 reusable bases extends that base of
 * {@link MoveEffectsGeneric} (all six parents used in this range -
 * {@code PokeBattle_HealingMove}, {@code PokeBattle_FixedDamageMove},
 * {@code PokeBattle_TargetMultiStatDownMove}, {@code PokeBattle_ProtectMove},
 * {@code PokeBattle_RecoilMove}, {@code PokeBattle_WeatherMove} - live there, so
 * no cross-file {@code extends} is needed in this file).</p>
 *
 * <h2>Shape</h2>
 * Each Ruby {@code class PokeBattle_Move_XXX < PokeBattle_Yyy} becomes a nested
 * {@code public static class PokeBattle_Move_XXX} extending the matching
 * base. (The task sheet spells the nested classes {@code public static final};
 * {@code final} is dropped here because the joint gate needs cross-file
 * {@code extends} - {@code MoveEffects_180_1FF.PokeBattle_Move_196} extends this
 * file's {@code PokeBattle_Move_0E0} (:435-442) - and every sibling file of this
 * batch ({@code MoveEffects_080_0AF}, {@code _0B0_0D4}, {@code _100_17F},
 * {@code _180_1FF}) likewise declares its classes non-final. Registered with the
 * Lead.) Only the hooks the class really overrides are overridden, each carrying
 * its {@code // Move_Effects_080-0FF.rb:行号} comment (the numbers are the
 * section's own line numbers, the same ones the sibling files use). Constructors
 * transcribe the Ruby {@code initialize} line by line; the strategy object is
 * shared by every move with the same function code (task-11 decision 4), so
 * function-code-describing values set by {@code initialize} ({@code @healAmount},
 * {@code @flingPowers}, {@code @statDown}, {@code @weatherType}, {@code @effect},
 * {@code @finalGambitDamage}) become fields of the class and are set in its
 * constructor.
 *
 * <h2>Translations used in this file</h2>
 * <ul>
 * <li>{@code @battle.pbDisplay(msg)} is this runtime's landed
 *     {@code Battle.display(msg)} ({@code PokeBattle_Battle:773}); it is called
 *     directly rather than through the {@code PendingApi} stub, exactly like the
 *     already-retired bodies of {@link MoveEffectBase}/{@link MoveEffectsGeneric}
 *     (see {@code __movefx-retire.mjs}), and so is every other method this
 *     runtime has already landed (the register has no other row for it below).</li>
 * <li>{@code user.totalhp} is {@code user.maxHp()}; {@code user.hp} is the
 *     public {@code Battler.hp}.</li>
 * <li>{@code user.item==0} / {@code target.item==0} is an empty
 *     {@code Battler.item} String (item identity is the internal name); the
 *     {@link #noItem} / {@link #hasItem} helpers name the two tests.</li>
 * <li>{@code @battle.positions[i]} is parked on
 *     {@code battle.field.positions[i]} in this runtime (see
 *     {@code BattleField}'s class comment - {@code Battle.java} is owned by
 *     another agent this round).</li>
 * <li>{@code @battle.rules[...]}, {@code @battle.pbPriority(true)},
 *     {@code @battle.pbPursuit}, {@code @battle.pbAbleNonActiveCount},
 *     {@code battler.pbEffectsOnSwitchIn}, {@code battler.pbRecoverHPFromDrain},
 *     {@code battler.pbSleepSelf}, {@code battler.pbFlinch} and
 *     {@code battler.isCommander?} have no runtime counterpart yet: each is a
 *     file-local {@code private static} helper that <b>throws</b> (never a
 *     default value) - see the "File-local M0 helpers" section at the bottom.
 *     (The name-taking {@code pbHeldItemTriggerCheck} does <b>not</b> belong on
 *     this list: {@code Battler.pbHeldItemTriggerCheck(String,boolean)} is landed
 *     at {@code Battler.java:3572} (the internal-name port of
 *     Battler_AbilityAndItem:235-249), so 0F4 :3544 and 0F7 :3784 call it
 *     directly - the same overload {@code MoveEffects_180_1FF.java:168/221}
 *     uses.)</li>
 * <li>{@code _INTL} strings are concatenated at the call site with the plugin's
 *     wording copied verbatim, including the plugin's own typos
 *     (e.g. {@code "但是出错了!"} at :3056).</li>
 * </ul>
 *
 * <h2>登记 (registered, not "fixed")</h2>
 * <ul>
 * <li>{@code PokeBattle_Move_0E7#pbFailsAgainstTarget?} (:3062-3068) has no
 *     explicit {@code return} on its last line; Ruby yields {@code nil}, Java
 *     needs {@code false}. Registered at the line.</li>
 * <li>{@code PokeBattle_Move_0F2}'s last display (:3479) passes two interpolation
 *     arguments to the <b>empty</b> format string {@code _INTL("")}, so the
 *     plugin shows nothing; copied verbatim and registered.</li>
 * <li>{@code PokeBattle_Move_0F7#pbBaseDamage} (:3756-3759) dereferences
 *     {@code pbGetMoveData(pbGetMachine(item))} without a nil check - a plugin
 *     defect. It is copied as-is: a printable TR item with no move makes the
 *     Java line throw {@code NullPointerException}, the analogue of Ruby's
 *     {@code NoMethodError}. Registered at the line.</li>
 * <li>{@code String#starts_with_vowel?} (:3585) is a global monkey-patch whose
 *     definition is not in this batch's reference set; both of its branches
 *     display the identical {@code _INTL} string, so its result cannot change the
 *     behaviour here. See {@link #startsWithVowel}.</li>
 * <li>The scene/party-screen halves of the switching moves ({@code @scene.pbRecall},
 *     {@code pbMessagesOnReplace}) are registered inside the landed
 *     {@code Battle.pbRecallAndReplace} / {@code Battle.showAbilitySplash} bodies;
 *     this file calls those landed methods directly instead of duplicating the 登记.</li>
 * </ul>
 */
public final class MoveEffects_0D5_0FF {

    private MoveEffects_0D5_0FF() {
    }

    // ==================================================================
    // 0D5-0D9: healing moves (Move_Effects_080-0FF.rb:2608-2715)
    // ==================================================================

    /** {@code class PokeBattle_Move_0D5 < PokeBattle_HealingMove} (:2608-2612): heals 1/2 max HP. */
    public static class PokeBattle_Move_0D5 extends MoveEffectsGeneric.PokeBattle_HealingMove {

        /** {@code pbHealAmount(user)} (:2609-2611). */
        @Override
        public int pbHealAmount(BattleMove move, Battler user) {
            return Math.round(user.maxHp() / 2.0f);                          // :2610 (user.totalhp/2.0).round
        }
    }

    /**
     * {@code class PokeBattle_Move_0D6 < PokeBattle_HealingMove} (:2620-2628):
     * heals 1/2 max HP and the user roosts (its Flying type is ignored).
     */
    public static class PokeBattle_Move_0D6 extends MoveEffectsGeneric.PokeBattle_HealingMove {

        /** {@code pbHealAmount(user)} (:2621-2623). */
        @Override
        public int pbHealAmount(BattleMove move, Battler user) {
            return Math.round(user.maxHp() / 2.0f);                          // :2622
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:2625-2627). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            user.effects.set(PBEffects.Battler.Roost, true);                  // :2626
        }
    }

    /**
     * {@code class PokeBattle_Move_0D7 < PokeBattle_Move} (:2636-2652): Wish - the
     * battler in the user's position is healed at the end of the next round.
     */
    public static class PokeBattle_Move_0D7 extends MoveEffectBase {

        /** {@code healingMove?; return true; end} (:2637). */
        @Override
        public boolean healingMove(BattleMove move) {
            return true;                                                     // :2637
        }

        /** {@code pbMoveFailed?(user,targets)} (:2639-2645). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = user.battle;
            // :2640 @battle.positions[user.index].effects[PBEffects::Wish]>0
            //       登记: @battle.positions[i] 在本运行时停在 battle.field.positions[i]（BattleField 类注释）
            if (battle.field.positions[user.index].effects.intVal(PBEffects.Position.Wish) > 0) {
                battle.display("但是失败了！");                                 // :2641
                return true;                                                 // :2642
            }
            return false;                                                    // :2644
        }

        /** {@code pbEffectGeneral(user)} (:2647-2651). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            Battle battle = user.battle;
            battle.field.positions[user.index].effects.set(PBEffects.Position.Wish, 2);   // :2648
            battle.field.positions[user.index].effects
                    .set(PBEffects.Position.WishAmount, Math.round(user.maxHp() / 2.0f));   // :2649 (user.totalhp/2.0).round
            battle.field.positions[user.index].effects
                    .set(PBEffects.Position.WishMaker, user.pokemonIndex);   // :2650
        }
    }

    /**
     * {@code class PokeBattle_Move_0D8 < PokeBattle_HealingMove} (:2660-2689):
     * Moonlight / Morning Sun / Synthesis - the heal amount depends on the
     * weather.
     */
    public static class PokeBattle_Move_0D8 extends MoveEffectsGeneric.PokeBattle_HealingMove {

        /** {@code @healAmount} - set by {@code pbOnStartUse} (:2663-2682), read at :2687. */
        private int healAmount;

        /** {@code pbOnStartUse(user,targets)} (:2661-2684). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.hasActiveAbility("SUPERSUN")) {                         // :2662
                healAmount = Math.round(user.maxHp() * 2 / 3.0f);            // :2663 (user.totalhp*2/3.0).round
                return;                                                      // :2664
            }
            Battle battle = user.battle;
            int weather = battle.pbWeather();                                // :2666 case @battle.pbWeather
            if (weather == PBWeather.Sun || weather == PBWeather.HarshSun) {  // :2667
                if (!user.hasUtilityUmbrella()) {                            // :2668
                    healAmount = Math.round(user.maxHp() * 2 / 3.0f);        // :2669
                } else {
                    healAmount = Math.round(user.maxHp() / 2.0f);            // :2671
                }
            } else if (weather == PBWeather.Rain || weather == PBWeather.HeavyRain) {   // :2673
                if (!user.hasUtilityUmbrella()) {                            // :2674
                    healAmount = Math.round(user.maxHp() / 4.0f);            // :2675
                } else {
                    healAmount = Math.round(user.maxHp() / 2.0f);            // :2677
                }
            } else if (weather == PBWeather.None || weather == PBWeather.StrongWinds) {  // :2679
                healAmount = Math.round(user.maxHp() / 2.0f);                // :2680
            } else {                                                         // :2681 the Ruby else
                healAmount = Math.round(user.maxHp() / 4.0f);                // :2682
            }
        }

        /** {@code pbHealAmount(user)} (:2686-2688). */
        @Override
        public int pbHealAmount(BattleMove move, Battler user) {
            return healAmount;                                               // :2687
        }
    }

    /**
     * {@code class PokeBattle_Move_0D9 < PokeBattle_HealingMove} (:2696-2715):
     * Rest - heals to full HP and falls asleep for 2 more rounds.
     */
    public static class PokeBattle_Move_0D9 extends MoveEffectsGeneric.PokeBattle_HealingMove {

        /** {@code pbMoveFailed?(user,targets)} (:2697-2705). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = user.battle;
            if (user.asleep()) {                                             // :2698
                battle.display("但是失败了！");                                 // :2699
                return true;                                                 // :2700
            }
            if (!user.pbCanSleep(user, true, move, true)) {                   // :2702 !user.pbCanSleep?(user,true,self,true)
                return true;                                                 // :2702
            }
            if (super.pbMoveFailed(move, user, targets)) {                     // :2703 return true if super
                return true;
            }
            return false;                                                    // :2704
        }

        /** {@code pbHealAmount(user)} (:2707-2709). */
        @Override
        public int pbHealAmount(BattleMove move, Battler user) {
            return user.maxHp() - user.hp;                                   // :2708 user.totalhp-user.hp
        }

        /** {@code pbEffectGeneral(user)} (:2711-2714). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            pbSleepSelf(user, user.pbThis() + "睡着了并恢复了健康！", 3);       // :2712 user.pbSleepSelf(_INTL(...),3)
            super.pbEffectGeneral(move, user);                               // :2713 super
        }
    }

    // ==================================================================
    // 0DA-0DC: self-targeted field effects (Move_Effects_080-0FF.rb:2723-2787)
    // ==================================================================

    /** {@code class PokeBattle_Move_0DA < PokeBattle_Move} (:2723-2736): Aqua Ring. */
    public static class PokeBattle_Move_0DA extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:2724-2730). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.effects.truthy(PBEffects.Battler.AquaRing)) {            // :2725
                user.battle.display("但是失败了！");                            // :2726
                return true;                                                 // :2727
            }
            return false;                                                    // :2729
        }

        /** {@code pbEffectGeneral(user)} (:2732-2735). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.AquaRing, true);               // :2733
            user.battle.display(user.pbThis() + "用水幕笼罩着四周！");           // :2734
        }
    }

    /** {@code class PokeBattle_Move_0DB < PokeBattle_Move} (:2744-2757): Ingrain. */
    public static class PokeBattle_Move_0DB extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:2745-2751). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.effects.truthy(PBEffects.Battler.Ingrain)) {             // :2746
                user.battle.display("但是失败了！");                            // :2747
                return true;                                                 // :2748
            }
            return false;                                                    // :2750
        }

        /** {@code pbEffectGeneral(user)} (:2753-2756). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.Ingrain, true);                // :2754
            user.battle.display(user.pbThis() + "扎下了根！");                  // :2755
        }
    }

    /** {@code class PokeBattle_Move_0DC < PokeBattle_Move} (:2765-2787): Leech Seed. */
    public static class PokeBattle_Move_0DC extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:2766-2776). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            if (target.effects.intVal(PBEffects.Battler.LeechSeed) >= 0) {     // :2767
                battle.display(target.pbThis() + "躲避了攻击！");                // :2768
                return true;                                                 // :2769
            }
            if (target.pbHasType("GRASS")) {                                 // :2771
                battle.display("这不能影响" + target.pbThis(true) + "……");      // :2772
                return true;                                                 // :2773
            }
            return false;                                                    // :2775
        }

        /** {@code pbMissMessage(user,target)} (:2778-2781). */
        @Override
        public boolean pbMissMessage(BattleMove move, Battler user, Battler target) {
            user.battle.display(target.pbThis() + "躲避了攻击！");               // :2779
            return true;                                                     // :2780
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:2783-2786). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.LeechSeed, user.index);       // :2784
            user.battle.display(target.pbThis() + "被种下了种子！");             // :2785
        }
    }

    // ==================================================================
    // 0DD-0DF: draining and healing the target (Move_Effects_080-0FF.rb:2794-2855)
    // ==================================================================

    /** {@code class PokeBattle_Move_0DD < PokeBattle_Move} (:2794-2802): gains half the damage dealt. */
    public static class PokeBattle_Move_0DD extends MoveEffectBase {

        /** {@code healingMove?; return NEWEST_BATTLE_MECHANICS; end} (:2795). */
        @Override
        public boolean healingMove(BattleMove move) {
            return Battle.NEWEST_BATTLE_MECHANICS;                           // :2795
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:2797-2801). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.damageState.hpLost <= 0) {                             // :2798
                return;                                                      // :2798
            }
            int hpGain = Math.round(target.damageState.hpLost / 2.0f);        // :2799 (hpLost/2.0).round
            pbRecoverHPFromDrain(user, hpGain, target);                       // :2800 user.pbRecoverHPFromDrain(hpGain,target)
        }
    }

    /** {@code class PokeBattle_Move_0DE < PokeBattle_Move} (:2810-2826): Dream Eater. */
    public static class PokeBattle_Move_0DE extends MoveEffectBase {

        /** {@code healingMove?; return NEWEST_BATTLE_MECHANICS; end} (:2811). */
        @Override
        public boolean healingMove(BattleMove move) {
            return Battle.NEWEST_BATTLE_MECHANICS;                           // :2811
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:2813-2819). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (!target.asleep()) {                                          // :2814
                user.battle.display(target.pbThis() + "没有受到影响！");       // :2815
                return true;                                                 // :2816
            }
            return false;                                                    // :2818
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:2821-2825). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.damageState.hpLost <= 0) {                             // :2822
                return;                                                      // :2822
            }
            int hpGain = Math.round(target.damageState.hpLost / 2.0f);        // :2823
            pbRecoverHPFromDrain(user, hpGain, target);                       // :2824
        }
    }

    /** {@code class PokeBattle_Move_0DF < PokeBattle_Move} (:2833-2855): Heal Pulse. */
    public static class PokeBattle_Move_0DF extends MoveEffectBase {

        /** {@code healingMove?; return true; end} (:2834). */
        @Override
        public boolean healingMove(BattleMove move) {
            return true;                                                     // :2834
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:2836-2844). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            if (target.hp == target.maxHp()) {                               // :2837 target.hp==target.totalhp
                battle.display(target.pbThis() + "的HP已经满了！");             // :2838
                return true;                                                 // :2839
            } else if (!target.canHeal()) {                                  // :2840
                battle.display(target.pbThis() + "没有受到影响！");            // :2841
                return true;                                                 // :2842
            }
            return false;                                                    // :2844
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:2847-2854). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            int hpGain = Math.round(target.maxHp() / 2.0f);                   // :2848 (target.totalhp/2.0).round
            if (pulseMove(move) && user.hasActiveAbility("MEGALAUNCHER")) {    // :2849 pulseMove? && hasActiveAbility?(:MEGALAUNCHER)
                hpGain = Math.round(target.maxHp() * 3 / 4.0f);               // :2850 (target.totalhp*3/4.0).round
            }
            target.pbRecoverHP(hpGain);                                      // :2852
            user.battle.display(target.pbThis() + "的HP回复了。");              // :2853
        }
    }

    // ==================================================================
    // 0E0-0E2: the user faints (Move_Effects_080-0FF.rb:2862-2938)
    // ==================================================================

    /** {@code class PokeBattle_Move_0E0 < PokeBattle_Move} (:2862-2888): Explosion / Self-Destruct. */
    public static class PokeBattle_Move_0E0 extends MoveEffectBase {

        /** {@code worksWithNoTargets?; return true; end} (:2863). */
        @Override
        public boolean worksWithNoTargets(BattleMove move) {
            return true;                                                     // :2863
        }

        /** {@code pbNumHits(user,targets); return 1; end} (:2864). */
        @Override
        public int pbNumHits(BattleMove move, Battler user, Array<Battler> targets) {
            return 1;                                                        // :2864
        }

        /** {@code pbMoveFailed?(user,targets)} (:2866-2882). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = user.battle;
            if (!battle.moldBreaker) {                                       // :2867
                Battler bearer = battle.pbCheckGlobalAbility("DAMP");         // :2868 @battle.pbCheckGlobalAbility(:DAMP)
                if (bearer != null) {                                        // :2869
                    battle.showAbilitySplash(bearer);                        // :2870
                    if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {      // :2871
                        battle.display(user.pbThis() + "不能使用" + move.name() + "了！");   // :2872
                    } else {
                        battle.display(bearer.pbThis(true) + "的" + bearer.abilityName()
                                + "使得" + user.pbThis() + "无法使用" + move.name() + "了！");   // :2874-2875
                    }
                    battle.hideAbilitySplash(bearer);                        // :2877
                    return true;                                             // :2878
                }
            }
            return false;                                                    // :2881
        }

        /** {@code pbSelfKO(user)} (:2884-2888). */
        @Override
        public void pbSelfKO(BattleMove move, Battler user) {
            if (user.fainted()) {                                            // :2885
                return;
            }
            user.pbReduceHP(user.hp, false, true, true);                      // :2886 user.pbReduceHP(user.hp,false)
            user.pbItemHPHealCheck(0, false);                                 // :2887 user.pbItemHPHealCheck
        }
    }

    /** {@code class PokeBattle_Move_0E1 < PokeBattle_FixedDamageMove} (:2897-2913): Final Gambit. */
    public static class PokeBattle_Move_0E1 extends MoveEffectsGeneric.PokeBattle_FixedDamageMove {

        /** {@code @finalGambitDamage} - set at :2901, read at :2905. */
        private int finalGambitDamage;

        /** {@code pbNumHits(user,targets); return 1; end} (:2898). */
        @Override
        public int pbNumHits(BattleMove move, Battler user, Array<Battler> targets) {
            return 1;                                                        // :2898
        }

        /** {@code pbOnStartUse(user,targets)} (:2900-2902). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            finalGambitDamage = user.hp;                                      // :2901
        }

        /** {@code pbFixedDamage(user,target)} (:2904-2906). */
        @Override
        public int pbFixedDamage(BattleMove move, Battler user, Battler target) {
            return finalGambitDamage;                                        // :2905
        }

        /** {@code pbSelfKO(user)} (:2908-2912). */
        @Override
        public void pbSelfKO(BattleMove move, Battler user) {
            if (user.fainted()) {                                            // :2909
                return;
            }
            user.pbReduceHP(user.hp, false, true, true);                      // :2910
            user.pbItemHPHealCheck(0, false);                                 // :2911
        }
    }

    /** {@code class PokeBattle_Move_0E2 < PokeBattle_TargetMultiStatDownMove} (:2921-2938): Memento. */
    public static class PokeBattle_Move_0E2
            extends MoveEffectsGeneric.PokeBattle_TargetMultiStatDownMove {

        /** {@code initialize(battle,move)} (:2922-2925): {@code super} then {@code @statDown}. */
        public PokeBattle_Move_0E2() {
            // :2923 super —— Java 的基类构造器是无参的，隐式调用
            statDown = new int[] {PBStats.ATTACK, 2, PBStats.SPATK, 2};        // :2924
        }

        /**
         * {@code pbFailsAgainstTarget?(user,target)} (:2929-2931) - NOTE (:2927-2928):
         * the user faints even if the target's stats cannot be changed, so this
         * must always return false.
         */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            return false;                                                    // :2930
        }

        /** {@code pbSelfKO(user)} (:2933-2937). */
        @Override
        public void pbSelfKO(BattleMove move, Battler user) {
            if (user.fainted()) {                                            // :2934
                return;
            }
            user.pbReduceHP(user.hp, false, true, true);                      // :2935
            user.pbItemHPHealCheck(0, false);                                 // :2936
        }
    }

    // ==================================================================
    // 0E3-0E7: fainting, perish song, grudge, destiny bond
    // (Move_Effects_080-0FF.rb:2946-3074)
    // ==================================================================

    /** {@code class PokeBattle_Move_0E3 < PokeBattle_Move} (:2946-2963): Healing Wish. */
    public static class PokeBattle_Move_0E3 extends MoveEffectBase {

        /** {@code healingMove?; return true; end} (:2947). */
        @Override
        public boolean healingMove(BattleMove move) {
            return true;                                                     // :2947
        }

        /** {@code pbMoveFailed?(user,targets)} (:2949-2955). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.battle.pbCanChooseNonActive(user.index)) {              // :2950
                user.battle.display("但是失败了！");                            // :2951
                return true;                                                 // :2952
            }
            return false;                                                    // :2954
        }

        /** {@code pbSelfKO(user)} (:2957-2962). */
        @Override
        public void pbSelfKO(BattleMove move, Battler user) {
            if (user.fainted()) {                                            // :2958
                return;
            }
            user.pbReduceHP(user.hp, false, true, true);                      // :2959
            user.pbItemHPHealCheck(0, false);                                 // :2960
            user.battle.field.positions[user.index].effects
                    .set(PBEffects.Position.HealingWish, true);               // :2961
        }
    }

    /** {@code class PokeBattle_Move_0E4 < PokeBattle_Move} (:2971-2988): Lunar Dance. */
    public static class PokeBattle_Move_0E4 extends MoveEffectBase {

        /** {@code healingMove?; return true; end} (:2972). */
        @Override
        public boolean healingMove(BattleMove move) {
            return true;                                                     // :2972
        }

        /** {@code pbMoveFailed?(user,targets)} (:2974-2980). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.battle.pbCanChooseNonActive(user.index)) {              // :2975
                user.battle.display("但是失败了！");                            // :2976
                return true;                                                 // :2977
            }
            return false;                                                    // :2979
        }

        /** {@code pbSelfKO(user)} (:2982-2987). */
        @Override
        public void pbSelfKO(BattleMove move, Battler user) {
            if (user.fainted()) {                                            // :2983
                return;
            }
            user.pbReduceHP(user.hp, false, true, true);                      // :2984
            user.pbItemHPHealCheck(0, false);                                 // :2985
            user.battle.field.positions[user.index].effects
                    .set(PBEffects.Position.LunarDance, true);                // :2986
        }
    }

    /** {@code class PokeBattle_Move_0E5 < PokeBattle_Move} (:2995-3028): Perish Song. */
    public static class PokeBattle_Move_0E5 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:2996-3008). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            boolean failed = true;                                           // :2997
            for (Battler b : targets) {                                      // :2998 targets.each do |b|
                if (b.effects.intVal(PBEffects.Battler.PerishSong) > 0) {     // :2999 next if b.effects[...]>0  # Heard it before
                    continue;
                }
                failed = false;                                              // :3000
                break;                                                       // :3001
            }
            if (failed) {                                                    // :3003
                user.battle.display("但是失败了！");                            // :3004
                return true;                                                 // :3005
            }
            return false;                                                    // :3007
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:3010-3017). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            if (battleRules(battle, "perishsongclause")                       // :3011 @battle.rules["perishsongclause"]
                    && pbAbleNonActiveCount(battle, user.idxOwnSide()) == 0) {  // :3012 @battle.pbAbleNonActiveCount(user.idxOwnSide)==0
                battle.display("但是失败了！");                                // :3013
                return true;                                                 // :3014
            }
            return target.effects.intVal(PBEffects.Battler.PerishSong) > 0;    // :3016
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:3019-3022). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.PerishSong, 4);              // :3020
            target.effects.set(PBEffects.Battler.PerishSongUser, user.index);  // :3021
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} (:3024-3027). */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets, int hitNum,
                                    boolean showAnimation) {
            super.pbShowAnimation(move, id, user, targets, hitNum, showAnimation);   // :3025 super
            user.battle.display("所有听见歌声的宝可梦都将在三回合后倒下！");        // :3026
        }
    }

    /** {@code class PokeBattle_Move_0E6 < PokeBattle_Move} (:3036-3041): Grudge. */
    public static class PokeBattle_Move_0E6 extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:3037-3040). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.Grudge, true);                 // :3038
            user.battle.display(user.pbThis() + "想向对手施加怨念。");           // :3039
        }
    }

    /** {@code class PokeBattle_Move_0E7 < PokeBattle_Move} (:3049-3074): Destiny Bond. */
    public static class PokeBattle_Move_0E7 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:3050-3060). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = user.battle;
            if (Battle.NEWEST_BATTLE_MECHANICS                           // :3051
                    && user.effects.truthy(PBEffects.Battler.DestinyBondPrevious)) {
                battle.display("但是失败了！");                                // :3052
                return true;                                                 // :3053
            }
            if (targets.size > 0 && targets.get(0) != null                  // :3055 targets[0] && targets[0].pokemon.battleRank > 1
                    && targets.get(0).pokemon.battleRank > 1) {
                battle.display("但是出错了!");                                 // :3056（插件原文用的是 ASCII 叹号）
                return true;                                                 // :3057
            }
            return false;                                                    // :3059
        }

        /**
         * {@code pbFailsAgainstTarget?(user,target)} (:3062-3068).
         *
         * <p>登记: the Ruby body has no {@code return} on its last line, so it
         * yields {@code nil} (falsy) when the rule branch is not taken; Java needs
         * an explicit {@code false} at :3068.</p>
         */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            if (battleRules(battle, "perishsongclause")                       // :3063
                    && pbAbleNonActiveCount(battle, user.idxOwnSide()) == 0) {  // :3064
                battle.display("但是失败了！");                                // :3065
                return true;                                                 // :3066
            }
            return false;                                                    // :3068 Ruby 的隐式 nil
        }

        /** {@code pbEffectGeneral(user)} (:3070-3073). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.DestinyBond, true);            // :3071
            user.battle.display(user.pbThis() + "想要与对手同归于尽！");         // :3072
        }
    }

    // ==================================================================
    // 0E8-0E9: Endure and the non-lethal moves (Move_Effects_080-0FF.rb:3081-3100)
    // ==================================================================

    /** {@code class PokeBattle_Move_0E8 < PokeBattle_ProtectMove} (:3081-3090): Endure. */
    public static class PokeBattle_Move_0E8 extends MoveEffectsGeneric.PokeBattle_ProtectMove {

        /** {@code initialize(battle,move)} (:3082-3085): {@code super} then {@code @effect}. */
        public PokeBattle_Move_0E8() {
            // :3083 super —— Java 的基类构造器是无参的，隐式调用
            effect = PBEffects.Battler.Endure;                               // :3084
        }

        /** {@code pbProtectMessage(user)} (:3087-3089). */
        @Override
        public void pbProtectMessage(BattleMove move, Battler user) {
            user.battle.display(user.pbThis() + "挺住了！");                    // :3088
        }
    }

    /** {@code class PokeBattle_Move_0E9 < PokeBattle_Move} (:3098-3100): False Swipe, Hold Back. */
    public static class PokeBattle_Move_0E9 extends MoveEffectBase {

        /** {@code nonLethal?(user,target); return true; end} (:3099). */
        @Override
        public boolean nonLethal(BattleMove move, Battler user, Battler target) {
            return true;                                                     // :3099
        }
    }

    // ==================================================================
    // 0EA-0EE: escaping and switching (Move_Effects_080-0FF.rb:3107-3309)
    // ==================================================================

    /** {@code class PokeBattle_Move_0EA < PokeBattle_Move} (:3107-3120): Teleport. */
    public static class PokeBattle_Move_0EA extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:3108-3114). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.battle.pbCanRun(user.index)) {                         // :3109
                user.battle.display("但是失败了！");                            // :3110
                return true;                                                 // :3111
            }
            return false;                                                    // :3113
        }

        /** {@code pbEffectGeneral(user)} (:3116-3119). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            Battle battle = user.battle;
            battle.display(user.pbThis() + "逃离战斗了！");                     // :3117
            battle.decision = 3;                                             // :3118 # Escaped
        }
    }

    /**
     * {@code class PokeBattle_Move_0EB < PokeBattle_Move} (:3130-3203): Roar /
     * Whirlwind - the target flees (wild) or is switched out (trainer).
     */
    public static class PokeBattle_Move_0EB extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user); return true; end} (:3131). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :3131
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:3133-3174). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            if (isCommander(target)) {                                       // :3134 target.isCommander?
                battle.display("但是它失败了！");                              // :3135
                return true;                                                 // :3136
            }
            if (target.hasActiveAbility("SUCTIONCUPS") && !battle.moldBreaker) {   // :3138
                battle.showAbilitySplash(target);                            // :3139
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {          // :3140
                    battle.display(target.pbThis() + "用吸盘粘在了地面上！");   // :3141
                } else {
                    battle.display(target.abilityName() + "固定住了" + target.pbThis() + "！");   // :3143
                }
                battle.hideAbilitySplash(target);                            // :3145
                return true;                                                 // :3146
            }
            if (target.effects.truthy(PBEffects.Battler.Ingrain)) {           // :3148
                battle.display("扎下的根固定住了" + target.pbThis());          // :3149
                return true;                                                 // :3150
            }
            // :3152 @battle.wildBattle? && target.level>user.level || target.pokemon.battleRank > 1
            if ((battle.wildBattle() && target.level() > user.level())
                    || target.pokemon.battleRank > 1) {
                battle.display("但是失败了！");                                // :3153
                return true;                                                 // :3154
            }
            if (target.effects.intVal(PBEffects.Battler.CurseNail) > 0) {     // :3157 咒钉，吹飞、吼叫无效
                battle.display("但是" + target.pbThis() + "被咒钉钉住了！");    // :3158
                return true;                                                 // :3159
            }
            if (battle.trainerBattle || user.pokemon.battleRank > 1) {        // :3161
                boolean canSwitch = false;                                   // :3162
                // :3163 @battle.eachInTeamFromBattlerIndex(target.index) —— 本运行时对应
                //        Battle.partyOf(idxBattler) 的 0..size-1 槽位（Battle.java:2535
                //        的 pbGetReplacementPokemonIndex 用的是同一映射）
                Array<Battler> team = battle.partyOf(target.index);
                for (int i = 0; i < team.size; i++) {
                    // :3164 next if !@battle.pbCanSwitchLax?(target.index,i)
                    //       本运行时的 Battle.canSwitchLax 返回“失败原因”String，null = 允许
                    if (battle.canSwitchLax(target.index, i) != null) {
                        continue;
                    }
                    canSwitch = true;                                        // :3165
                    break;                                                   // :3166
                }
                if (!canSwitch) {                                            // :3168
                    battle.display("但是失败了！");                            // :3169
                    return true;                                             // :3170
                }
            }
            return false;                                                    // :3173
        }

        /** {@code pbEffectGeneral(user)} (:3176-3178). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            // :3177 @battle.decision = 3 if @battle.wildBattle? && user.pokemon.battleRank <= 1
            if (user.battle.wildBattle() && user.pokemon.battleRank <= 1) {
                user.battle.decision = 3;                                    // :3177
            }
        }

        /** {@code pbSwitchOutTargetsEffect(user,targets,numHits,switchedBattlers)} (:3180-3202). */
        @Override
        public void pbSwitchOutTargetsEffect(BattleMove move, Battler user, Array<Battler> targets, int numHits,
                                             IntArray switchedBattlers) {
            Battle battle = user.battle;
            if (battle.wildBattle()) {                                       // :3181
                return;
            }
            if (user.fainted() || numHits == 0) {                            // :3182
                return;
            }
            Array<Integer> roarSwitched = new Array<>();                      // :3183 roarSwitched = []
            for (Battler b : targets) {                                      // :3184 targets.each do |b|
                if (b.fainted() || b.damageState.unaffected                       // :3185
                        || switchedBattlers.contains(b.index)) {
                    continue;
                }
                // :3186 next if b.hasActiveAbility?([:SUCTIONCUPS,:FIGHTTODIE,:GUARDDOG]) && !@battle.moldBreaker
                if (b.hasActiveAbility(new String[] {"SUCTIONCUPS", "FIGHTTODIE", "GUARDDOG"})
                        && !battle.moldBreaker) {
                    continue;
                }
                if (isCommander(b)) {                                        // :3187
                    continue;
                }
                int newPkmn = battle.pbGetReplacementPokemonIndex(b.index, true);   // :3188 (Random)
                if (newPkmn < 0) {                                           // :3189
                    continue;
                }
                // :3190 登记: Ruby 的第三实参 randomReplacement=true 在 Battle.pbRecallAndReplace
                //        (Battle_Action_Switching:256-262) 的 2 参运行时签名里没有对应档，
                //        按运行时形状调用
                battle.pbRecallAndReplace(b.index, newPkmn);
                battle.display(b.pbThis() + "被拖入了战斗！");                 // :3191
                battle.pbClearChoice(b.index);                               // :3192
                switchedBattlers.add(b.index);                               // :3193
                roarSwitched.add(b.index);                                   // :3194
            }
            if (roarSwitched.size > 0) {                                     // :3196
                if (roarSwitched.contains(user.index, false)) {               // :3197 roarSwitched.include?(user.index)
                    battle.moldBreaker = false;
                }
                for (Battler b : pbPriority(battle, true)) {                  // :3198 @battle.pbPriority(true).each
                    if (roarSwitched.contains(b.index, false)) {              // :3199
                        pbEffectsOnSwitchIn(b, true);                        // :3199 b.pbEffectsOnSwitchIn(true)
                    }
                }
            }
        }
    }

    /**
     * {@code class PokeBattle_Move_0EC < PokeBattle_Move} (:3213-3246): Circle
     * Throw / Dragon Tail - the damaging counterpart of 0EB.
     */
    public static class PokeBattle_Move_0EC extends MoveEffectBase {

        /** {@code pbEffectAgainstTarget(user,target)} (:3214-3220). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            if (battle.wildBattle() && target.level() <= user.level()         // :3215-3216
                    && user.pokemon.battleRank == 0 && target.pokemon.battleRank == 0   // :3216
                    && (target.effects.intVal(PBEffects.Battler.Substitute) == 0
                        || ignoresSubstitute(move, user))) {                  // :3217
                battle.decision = 3;                                         // :3218
            }
        }

        /** {@code pbSwitchOutTargetsEffect(user,targets,numHits,switchedBattlers)} (:3223-3245). */
        @Override
        public void pbSwitchOutTargetsEffect(BattleMove move, Battler user, Array<Battler> targets, int numHits,
                                             IntArray switchedBattlers) {
            Battle battle = user.battle;
            if (battle.wildBattle()) {                                       // :3224
                return;
            }
            if (user.fainted() || numHits == 0) {                            // :3225
                return;
            }
            Array<Integer> roarSwitched = new Array<>();                      // :3226
            for (Battler b : targets) {                                      // :3227
                if (b.fainted() || b.damageState.unaffected                       // :3228
                        || switchedBattlers.contains(b.index)) {
                    continue;
                }
                if (b.hasActiveAbility(new String[] {"SUCTIONCUPS", "FIGHTTODIE", "GUARDDOG"})   // :3229
                        && !battle.moldBreaker) {
                    continue;
                }
                if (isCommander(b)) {                                        // :3230
                    continue;
                }
                int newPkmn = battle.pbGetReplacementPokemonIndex(b.index, true);   // :3231 (Random)
                if (newPkmn < 0) {                                           // :3232
                    continue;
                }
                // :3233 登记: 同 0EB 的 :3190（randomReplacement=true 无对应档）
                battle.pbRecallAndReplace(b.index, newPkmn);
                battle.display(b.pbThis() + "被拖入了战斗！");                 // :3234
                battle.pbClearChoice(b.index);                               // :3235
                switchedBattlers.add(b.index);                               // :3236
                roarSwitched.add(b.index);                                   // :3237
            }
            if (roarSwitched.size > 0) {                                     // :3239
                if (roarSwitched.contains(user.index, false)) {               // :3240
                    battle.moldBreaker = false;
                }
                for (Battler b : pbPriority(battle, true)) {                  // :3241
                    if (roarSwitched.contains(b.index, false)) {              // :3242
                        pbEffectsOnSwitchIn(b, true);                        // :3242
                    }
                }
            }
        }
    }

    /** {@code class PokeBattle_Move_0ED < PokeBattle_Move} (:3254-3276): Baton Pass. */
    public static class PokeBattle_Move_0ED extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:3255-3261). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (!user.battle.pbCanChooseNonActive(user.index)) {              // :3256
                user.battle.display("但是失败了！");                            // :3257
                return true;                                                 // :3258
            }
            return false;                                                    // :3260
        }

        /** {@code pbEndOfMoveUsageEffect(user,targets,numHits,switchedBattlers)} (:3263-3275). */
        @Override
        public void pbEndOfMoveUsageEffect(BattleMove move, Battler user, Array<Battler> targets, int numHits,
                                           IntArray switchedBattlers) {
            Battle battle = user.battle;
            if (user.fainted() || numHits == 0) {                            // :3264
                return;
            }
            if (!battle.pbCanChooseNonActive(user.index)) {                   // :3265
                return;
            }
            pbPursuit(battle, user.index);                                   // :3266 @battle.pbPursuit(user.index)
            if (user.fainted()) {                                            // :3267
                return;
            }
            int newPkmn = battle.pbGetReplacementPokemonIndex(user.index);    // :3268 (Owner chooses)
            if (newPkmn < 0) {                                               // :3269
                return;
            }
            // :3270 登记: Ruby 的后两实参 (randomReplacement=false,batonPass=true) 在
            //        Battle.pbRecallAndReplace 的 2 参运行时签名里没有对应档
            battle.pbRecallAndReplace(user.index, newPkmn);
            battle.pbClearChoice(user.index);                                // :3271
            battle.moldBreaker = false;                                      // :3272
            switchedBattlers.add(user.index);                                // :3273
            pbEffectsOnSwitchIn(user, true);                                 // :3274 user.pbEffectsOnSwitchIn(true)
        }
    }

    /** {@code class PokeBattle_Move_0EE < PokeBattle_Move} (:3284-3309): U-turn, Volt Switch. */
    public static class PokeBattle_Move_0EE extends MoveEffectBase {

        /** {@code pbEndOfMoveUsageEffect(user,targets,numHits,switchedBattlers)} (:3285-3308). */
        @Override
        public void pbEndOfMoveUsageEffect(BattleMove move, Battler user, Array<Battler> targets, int numHits,
                                           IntArray switchedBattlers) {
            Battle battle = user.battle;
            if (user.fainted() || numHits == 0) {                            // :3286
                return;
            }
            if (user.effects.intVal(PBEffects.Battler.CurseNail) > 0) {       // :3288 咒钉，急速折返、伏特替换无效
                battle.display(user.pbThis() + "的咒钉使它无法替换！");          // :3289
                return;                                                      // :3290
            }
            boolean targetSwitched = true;                                   // :3292
            for (Battler b : targets) {                                      // :3293
                if (!switchedBattlers.contains(b.index)) {                    // :3294
                    targetSwitched = false;
                }
            }
            if (targetSwitched) {                                            // :3296
                return;
            }
            if (!battle.pbCanChooseNonActive(user.index)) {                   // :3297
                return;
            }
            battle.display(user.pbThis() + "回到了" + battle.pbGetOwnerName(user.index)
                    + "身边！");                                             // :3298-3299
            pbPursuit(battle, user.index);                                   // :3300
            if (user.fainted()) {                                            // :3301
                return;
            }
            int newPkmn = battle.pbGetReplacementPokemonIndex(user.index);    // :3302 (Owner chooses)
            if (newPkmn < 0) {                                               // :3303
                return;
            }
            battle.pbRecallAndReplace(user.index, newPkmn);                   // :3304
            battle.pbClearChoice(user.index);                                // :3305
            battle.moldBreaker = false;                                      // :3306
            switchedBattlers.add(user.index);                                // :3307
            pbEffectsOnSwitchIn(user, true);                                 // :3308
        }
    }

    // ==================================================================
    // 0EF: trapping (Move_Effects_080-0FF.rb:3318-3344)
    // ==================================================================

    /** {@code class PokeBattle_Move_0EF < PokeBattle_Move} (:3318-3344): Mean Look and friends. */
    public static class PokeBattle_Move_0EF extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:3319-3329). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {                                        // :3320
                return false;
            }
            Battle battle = user.battle;
            if (target.effects.intVal(PBEffects.Battler.MeanLook) >= 0) {      // :3321
                battle.display("但是失败了！");                                // :3322
                return true;                                                 // :3323
            }
            if (Battle.NEWEST_BATTLE_MECHANICS && target.pbHasType("GHOST")) {  // :3325
                battle.display("这不能影响" + target.pbThis(true) + "……");      // :3326
                return true;                                                 // :3327
            }
            return false;                                                    // :3329
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:3332-3335). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {                                        // :3333
                return;
            }
            target.effects.set(PBEffects.Battler.MeanLook, user.index);        // :3334
            user.battle.display(target.pbThis() + "不能逃脱！");                // :3335
        }

        /** {@code pbAdditionalEffect(user,target)} (:3338-3344). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.fainted() || target.damageState.substitute) {          // :3339
                return;
            }
            if (target.effects.intVal(PBEffects.Battler.MeanLook) >= 0) {      // :3340
                return;
            }
            if (Battle.NEWEST_BATTLE_MECHANICS && target.pbHasType("GHOST")) {  // :3341
                return;
            }
            target.effects.set(PBEffects.Battler.MeanLook, user.index);        // :3342
            user.battle.display(target.pbThis() + "不能逃脱！");                // :3343
        }
    }

    // ==================================================================
    // 0F0-0F6: item manipulation (Move_Effects_080-0FF.rb:3353-3592)
    // ==================================================================

    /** {@code class PokeBattle_Move_0F0 < PokeBattle_Move} (:3353-3381): Knock Off. */
    public static class PokeBattle_Move_0F0 extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:3354-3360). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (user.pbOwnedByPlayer() && target.pokemon.battleRank > 2) {     // :3355
                user.battle.display("但是失败了！");                            // :3356
                return true;                                                 // :3357
            }
            return false;                                                    // :3359
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:3362-3370). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (Battle.NEWEST_BATTLE_MECHANICS                              // :3363
                    && hasItem(target.item)                                  // :3364 target.item!=0
                    && !target.unlosableItem(target.item)) {                 // :3364
                // NOTE (:3365-3366): the damage is still boosted even if the
                // target has Sticky Hold or a substitute.
                baseDmg = Math.round(baseDmg * 1.5f);                        // :3367 (baseDmg*1.5).round
            }
            return baseDmg;                                                  // :3369
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:3372-3380). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            if (battle.wildBattle() && user.opposes(0)) {                     // :3373 # Wild Pokémon can't knock off
                return;
            }
            if (user.fainted()) {                                            // :3374
                return;
            }
            if (target.damageState.unaffected || target.damageState.substitute) {   // :3375
                return;
            }
            if (!hasItem(target.item) || target.unlosableItem(target.item)) {   // :3376 target.item==0 || unlosableItem?
                return;
            }
            if (target.hasActiveAbility("STICKYHOLD") && !battle.moldBreaker) {   // :3377
                return;
            }
            String itemName = target.itemName();                             // :3378
            target.pbRemoveItem(false);                                      // :3379 pbRemoveItem(false)
            battle.display(target.pbThis() + "的" + itemName + "被拍落了！");   // :3380
        }
    }

    /** {@code class PokeBattle_Move_0F1 < PokeBattle_Move} (:3390-3411): Covet, Thief. */
    public static class PokeBattle_Move_0F1 extends MoveEffectBase {

        /** {@code pbEffectAfterAllHits(user,target)} (:3391-3410). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            if (battle.wildBattle() && user.opposes(0)) {                     // :3392 # Wild Pokémon can't thieve
                return;
            }
            if (user.fainted()) {                                            // :3393
                return;
            }
            if (target.damageState.unaffected || target.damageState.substitute) {   // :3394
                return;
            }
            if (!hasItem(target.item) || hasItem(user.item)) {                // :3395 target.item==0 || user.item!=0
                return;
            }
            if (target.unlosableItem(target.item)) {                          // :3396
                return;
            }
            if (user.unlosableItem(target.item)) {                            // :3397
                return;
            }
            if (target.hasActiveAbility("STICKYHOLD") && !battle.moldBreaker) {   // :3398
                return;
            }
            String itemName = target.itemName();                             // :3399
            user.item = target.item;                                         // :3400
            // Permanently steal the item from wild Pokémon (:3402-3403)
            if (battle.wildBattle() && target.opposes(0)                      // :3402
                    && target.initialItem().equals(target.item)               // :3403 target.initialItem==target.item
                    && user.initialItem().isEmpty()) {                        // :3403 user.initialItem==0
                user.setInitialItem(target.item);                             // :3404
                target.pbRemoveItem(true);                                    // :3405 pbRemoveItem（无参 = permanent=true）
            } else {
                target.pbRemoveItem(false);                                   // :3407
            }
            battle.display(user.pbThis() + "偷窃了" + target.pbThis(true) + "的" + itemName + "！");   // :3409
            user.pbHeldItemTriggerCheck(0, false);                            // :3410 pbHeldItemTriggerCheck（无参）
        }
    }

    /** {@code class PokeBattle_Move_0F2 < PokeBattle_Move} (:3420-3482): Switcheroo, Trick. */
    public static class PokeBattle_Move_0F2 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:3421-3427). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.battle.wildBattle() && user.opposes(0)) {                 // :3422
                user.battle.display("但是失败了！");                            // :3423
                return true;                                                 // :3424
            }
            return false;                                                    // :3426
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:3429-3457). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            if (noItem(user) && noItem(target)) {                            // :3430 user.item==0 && target.item==0
                battle.display("但是失败了！");                                // :3431
                return true;                                                 // :3432
            }
            if (user.pbOwnedByPlayer() && target.pokemon.battleRank > 2) {     // :3434
                battle.display("但是失败了！");                                // :3435
                return true;                                                 // :3436
            }
            if (target.unlosableItem(target.item)                             // :3438
                    || target.unlosableItem(user.item)                        // :3439
                    || user.unlosableItem(user.item)                          // :3440
                    || user.unlosableItem(target.item)) {                     // :3441
                battle.display("但是失败了！");                                // :3442
                return true;                                                 // :3443
            }
            if (target.hasActiveAbility("STICKYHOLD") && !battle.moldBreaker) {   // :3445
                battle.showAbilitySplash(target);                            // :3446
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {          // :3447
                    battle.display("但是不能影响" + target.pbThis(true) + "！");   // :3448
                } else {
                    battle.display("但由于" + target.abilityName() + "，\n这不能影响"
                            + target.pbThis(true) + "！");                     // :3450-3451
                }
                battle.hideAbilitySplash(target);                            // :3453
                return true;                                                 // :3454
            }
            return false;                                                    // :3456
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:3459-3481). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            String oldUserItem = user.item;     String oldUserItemName = user.itemName();       // :3460
            String oldTargetItem = target.item; String oldTargetItemName = target.itemName();   // :3461
            user.item = oldTargetItem;                                        // :3462
            user.effects.set(PBEffects.Battler.ChoiceBand, -1);               // :3463
            user.effects.set(PBEffects.Battler.Unburden,
                    noItem(user) && hasItem(oldUserItem));                    // :3464 (user.item==0 && oldUserItem>0)
            target.item = oldUserItem;                                        // :3465
            target.effects.set(PBEffects.Battler.ChoiceBand, -1);             // :3466
            target.effects.set(PBEffects.Battler.Unburden,
                    noItem(target) && hasItem(oldTargetItem));                 // :3467
            // Permanently steal the item from wild Pokémon (:3469-3471)
            if (battle.wildBattle() && target.opposes(0)                      // :3469
                    && target.initialItem().equals(oldTargetItem)              // :3470
                    && user.initialItem().isEmpty()) {                         // :3470
                user.setInitialItem(oldTargetItem);                           // :3471
            }
            battle.display(user.pbThis() + "与目标交换了道具！");                // :3473
            if (hasItem(oldUserItem) && hasItem(oldTargetItem)) {              // :3474
                battle.display(user.pbThis() + "得到了" + oldTargetItemName + "。");   // :3475
            } else if (hasItem(oldTargetItem)) {                               // :3476
                battle.display(user.pbThis() + "得到了" + oldTargetItemName + "。");   // :3477
            }
            if (hasItem(oldUserItem)) {                                        // :3479
                // 登记: 插件原文 :3479 是 @battle.pbDisplay(_INTL("",target.pbThis,oldUserItemName)) ——
                //       格式串为空，两个插值参数不会出现在任何文本里（掺入它们就是在“修正”原文）。
                //       照抄其行为：显示空串。
                battle.display("");
            }
            user.pbHeldItemTriggerCheck(0, false);                            // :3480
            target.pbHeldItemTriggerCheck(0, false);                          // :3481
        }
    }

    /** {@code class PokeBattle_Move_0F3 < PokeBattle_Move} (:3491-3526): Bestow. */
    public static class PokeBattle_Move_0F3 extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user)} (:3492-3495). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            if (Battle.NEWEST_BATTLE_MECHANICS) {                            // :3493
                return true;
            }
            return super.ignoresSubstitute(move, user);                      // :3494 return super
        }

        /** {@code pbMoveFailed?(user,targets)} (:3497-3502). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (noItem(user) || user.unlosableItem(user.item)) {              // :3498
                user.battle.display("但是失败了！");                            // :3499
                return true;                                                 // :3500
            }
            return false;                                                    // :3502
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:3505-3510). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (hasItem(target.item) || target.unlosableItem(user.item)) {     // :3506 target.item!=0 || target.unlosableItem?(user.item)
                user.battle.display("但是失败了！");                            // :3507
                return true;                                                 // :3508
            }
            return false;                                                    // :3510
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:3513-3525). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            String itemName = user.itemName();                               // :3514
            target.item = user.item;                                         // :3515
            // Permanently steal the item from wild Pokémon (:3517-3519)
            if (battle.wildBattle() && target.opposes(0)                      // :3517
                    && user.initialItem().equals(user.item)                    // :3518 user.initialItem==user.item
                    && target.initialItem().isEmpty()) {                       // :3518 target.initialItem==0
                target.setInitialItem(user.item);                             // :3519
                user.pbRemoveItem(true);                                      // :3520 pbRemoveItem（无参 = permanent=true）
            } else {
                user.pbRemoveItem(false);                                     // :3522
            }
            battle.display(target.pbThis() + "从" + user.pbThis(true) + "那里获得了"
                    + itemName + "！");                                       // :3524
            target.pbHeldItemTriggerCheck(0, false);                          // :3525
        }
    }

    /** {@code class PokeBattle_Move_0F4 < PokeBattle_Move} (:3534-3545): Bug Bite, Pluck. */
    public static class PokeBattle_Move_0F4 extends MoveEffectBase {

        /** {@code pbEffectAfterAllHits(user,target)} (:3535-3544). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            if (user.fainted() || target.fainted()) {                        // :3536
                return;
            }
            if (target.damageState.unaffected || target.damageState.substitute) {   // :3537
                return;
            }
            if (!hasItem(target.item) || !isBerry(battle, target.item)) {      // :3538 target.item==0 || !pbIsBerry?
                return;
            }
            if (target.hasActiveAbility("STICKYHOLD") && !battle.moldBreaker) {   // :3539
                return;
            }
            String item = target.item;                                       // :3540
            String itemName = target.itemName();                             // :3541
            target.pbRemoveItem(true);                                       // :3542 pbRemoveItem（无参 = permanent=true）
            battle.display(user.pbThis() + "偷走并吃掉了" + itemName + "!");    // :3543
            user.pbHeldItemTriggerCheck(item, false);                        // :3544 user.pbHeldItemTriggerCheck(item,false)
        }
    }

    /** {@code class PokeBattle_Move_0F5 < PokeBattle_Move} (:3553-3560): Incinerate. */
    public static class PokeBattle_Move_0F5 extends MoveEffectBase {

        /** {@code pbEffectWhenDealingDamage(user,target)} (:3554-3559). */
        @Override
        public void pbEffectWhenDealingDamage(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            if (target.damageState.substitute || target.damageState.berryWeakened) {   // :3555
                return;
            }
            if (!isBerry(battle, target.item)                                // :3556 !pbIsBerry?(target.item)
                    && !(Battle.NEWEST_BATTLE_MECHANICS
                        && isGem(battle, target.item))) {                     // :3557 (NEWEST_BATTLE_MECHANICS && pbIsGem?)
                return;
            }
            target.pbRemoveItem(true);                                       // :3558 pbRemoveItem（无参 = permanent=true）
            battle.display(target.pbThis() + "的" + target.itemName() + "被烧成灰烬了！");   // :3559
        }
    }

    /** {@code class PokeBattle_Move_0F6 < PokeBattle_Move} (:3568-3592): Recycle. */
    public static class PokeBattle_Move_0F6 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:3569-3574). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.recycleItem().isEmpty()) {                              // :3570 user.recycleItem==0
                user.battle.display("但是失败了！");                            // :3571
                return true;                                                 // :3572
            }
            return false;                                                    // :3574
        }

        /** {@code pbEffectGeneral(user)} (:3577-3590). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            Battle battle = user.battle;
            String item = user.recycleItem();                                // :3578
            user.item = item;                                                // :3579
            if (battle.wildBattle() && user.initialItem().isEmpty()) {        // :3580 @battle.wildBattle? && user.initialItem==0
                user.setInitialItem(item);                                   // :3580
            }
            user.setRecycleItem("");                                         // :3581 setRecycleItem(0)
            user.effects.set(PBEffects.Battler.PickupItem, 0);               // :3582
            user.effects.set(PBEffects.Battler.PickupUse, 0);                // :3583
            String itemName = itemDisplayName(battle, item);                 // :3584 itemName = PBItems.getName(item)
            if (startsWithVowel(itemName)) {                                 // :3585
                battle.display(user.pbThis() + "回收了" + itemName + "！");     // :3586
            } else {
                battle.display(user.pbThis() + "回收了" + itemName + "！");     // :3588
            }
            user.pbHeldItemTriggerCheck(0, false);                           // :3590
        }
    }

    // ==================================================================
    // 0F7: Fling (Move_Effects_080-0FF.rb:3599-3795)
    // ==================================================================

    /**
     * {@code class PokeBattle_Move_0F7 < PokeBattle_Move} (:3599-3795): Fling.
     *
     * <p>{@code @flingPowers} (:3604-3713) is the hash power &rarr; list of item
     * internal names; it is kept as an insertion-ordered list of
     * {@link FlingPower} because Ruby's {@code Hash#each} iterates in insertion
     * order and the lookup at :3763-3765 returns the first matching power.</p>
     */
    public static class PokeBattle_Move_0F7 extends MoveEffectBase {

        /** One entry of {@code @flingPowers}: {@code power => [items...]}. */
        private static final class FlingPower {
            /** The hash key ({@code 130}, {@code 100}, ...). */
            final int power;
            /** The hash value - the item internal names of that group. */
            final Array<String> items;

            FlingPower(int power, String... items) {
                this.power = power;
                this.items = new Array<>(items);
            }
        }

        /** {@code @flingPowers = {...}} (:3604-3713). */
        private final Array<FlingPower> flingPowers = new Array<>();

        /** {@code @willFail} (:3716-3717): set by {@code pbCheckFlingSuccess}, read at :3737/:3747. */
        private boolean willFail;

        /** {@code initialize(battle,move)} (:3600-3713). */
        public PokeBattle_Move_0F7() {
            // :3601 super —— Java 的基类构造器是无参的，隐式调用
            // 80 => all Mega Stones / 10 => all Berries are handled by the
            // pbIsMegaStone?/pbIsBerry? branches at :3762/:3761, not by the table.
            flingPowers.add(new FlingPower(130, "IRONBALL"));                 // :3605-3606
            flingPowers.add(new FlingPower(100, "HARDSTONE", "RAREBONE",      // :3607-3612 (Fossils)
                    "ARMORFOSSIL", "CLAWFOSSIL", "COVERFOSSIL", "DOMEFOSSIL", "HELIXFOSSIL",
                    "JAWFOSSIL", "OLDAMBER", "PLUMEFOSSIL", "ROOTFOSSIL", "SAILFOSSIL",
                    "SKULLFOSSIL"));
            flingPowers.add(new FlingPower(90, "DEEPSEATOOTH", "GRIPCLAW", "THICKCLUB",   // :3613-3619 (Plates)
                    "DRACOPLATE", "DREADPLATE", "EARTHPLATE", "FISTPLATE", "FLAMEPLATE",
                    "ICICLEPLATE", "INSECTPLATE", "IRONPLATE", "MEADOWPLATE", "MINDPLATE",
                    "PIXIEPLATE", "SKYPLATE", "SPLASHPLATE", "SPOOKYPLATE", "STONEPLATE",
                    "TOXICPLATE", "ZAPPLATE"));
            flingPowers.add(new FlingPower(80, "ASSAULTVEST", "DAWNSTONE", "DUSKSTONE",   // :3620-3623
                    "ELECTIRIZER", "MAGMARIZER", "ODDKEYSTONE", "OVALSTONE", "PROTECTOR",
                    "QUICKCLAW", "RAZORCLAW", "SAFETYGOGGLES", "SHINYSTONE", "STICKYBARB",
                    "WEAKNESSPOLICY"));
            flingPowers.add(new FlingPower(70, "DRAGONFANG", "POISONBARB",    // :3624-3630 (EV items, Drives)
                    "POWERANKLET", "POWERBAND", "POWERBELT", "POWERBRACER", "POWERLENS",
                    "POWERWEIGHT",
                    "BURNDRIVE", "CHILLDRIVE", "DOUSEDRIVE", "SHOCKDRIVE"));
            flingPowers.add(new FlingPower(60, "ADAMANTORB", "DAMPROCK", "GRISEOUSORB",   // :3631-3633
                    "HEATROCK", "LUSTROUSORB", "MACHOBRACE", "ROCKYHELMET", "STICK",
                    "TERRAINEXTENDER"));
            flingPowers.add(new FlingPower(50, "DUBIOUSDISC", "SHARPBEAK",    // :3634-3640 (Memories)
                    "BUGMEMORY", "DARKMEMORY", "DRAGONMEMORY", "ELECTRICMEMORY", "FAIRYMEMORY",
                    "FIGHTINGMEMORY", "FIREMEMORY", "FLYINGMEMORY", "GHOSTMEMORY",
                    "GRASSMEMORY", "GROUNDMEMORY", "ICEMEMORY", "POISONMEMORY",
                    "PSYCHICMEMORY", "ROCKMEMORY", "STEELMEMORY", "WATERMEMORY"));
            flingPowers.add(new FlingPower(40, "EVIOLITE", "ICYROCK", "LUCKYPUNCH"));     // :3641-3642
            flingPowers.add(new FlingPower(30, "ABSORBBULB", "ADRENALINEORB", "AMULETCOIN",   // :3643-3691
                    "BINDINGBAND", "BLACKBELT", "BLACKGLASSES", "BLACKSLUDGE", "BOTTLECAP",
                    "CELLBATTERY", "CHARCOAL", "CLEANSETAG", "DEEPSEASCALE", "DRAGONSCALE",
                    "EJECTBUTTON", "ESCAPEROPE", "EXPSHARE", "FLAMEORB", "FLOATSTONE",
                    "FLUFFYTAIL", "GOLDBOTTLECAP", "HEARTSCALE", "HONEY", "KINGSROCK",
                    "LIFEORB", "LIGHTBALL", "LIGHTCLAY", "LUCKYEGG", "LUMINOUSMOSS",
                    "MAGNET", "METALCOAT", "METRONOME", "MIRACLESEED", "MYSTICWATER",
                    "NEVERMELTICE", "PASSORB", "POKEDOLL", "POKETOY", "PRISMSCALE",
                    "PROTECTIVEPADS", "RAZORFANG", "SACREDASH", "SCOPELENS", "SHELLBELL",
                    "SHOALSALT", "SHOALSHELL", "SMOKEBALL", "SNOWBALL", "SOULDEW",
                    "SPELLTAG", "TOXICORB", "TWISTEDSPOON", "UPGRADE",
                    // Healing items
                    "ANTIDOTE", "AWAKENING", "BERRYJUICE", "BIGMALASADA", "BLUEFLUTE",
                    "BURNHEAL", "CASTELIACONE", "ELIXIR", "ENERGYPOWDER", "ENERGYROOT",
                    "ETHER", "FRESHWATER", "FULLHEAL", "FULLRESTORE", "HEALPOWDER",
                    "HYPERPOTION", "ICEHEAL", "LAVACOOKIE", "LEMONADE", "LUMIOSEGALETTE",
                    "MAXELIXIR", "MAXETHER", "MAXPOTION", "MAXREVIVE", "MOOMOOMILK",
                    "OLDGATEAU", "PARALYZEHEAL", "PARLYZHEAL", "PEWTERCRUNCHIES", "POTION",
                    "RAGECANDYBAR", "REDFLUTE", "REVIVALHERB", "REVIVE", "SHALOURSABLE",
                    "SODAPOP", "SUPERPOTION", "SWEETHEART", "YELLOWFLUTE",
                    // Battle items
                    "XACCURACY", "XACCURACY2", "XACCURACY3", "XACCURACY6",
                    "XATTACK", "XATTACK2", "XATTACK3", "XATTACK6",
                    "XDEFEND", "XDEFEND2", "XDEFEND3", "XDEFEND6",
                    "XDEFENSE", "XDEFENSE2", "XDEFENSE3", "XDEFENSE6",
                    "XSPATK", "XSPATK2", "XSPATK3", "XSPATK6",
                    "XSPECIAL", "XSPECIAL2", "XSPECIAL3", "XSPECIAL6",
                    "XSPDEF", "XSPDEF2", "XSPDEF3", "XSPDEF6",
                    "XSPEED", "XSPEED2", "XSPEED3", "XSPEED6",
                    "DIREHIT", "DIREHIT2", "DIREHIT3",
                    "ABILITYURGE", "GUARDSPEC", "ITEMDROP", "ITEMURGE", "RESETURGE",
                    // Vitamins
                    "CALCIUM", "CARBOS", "HPUP", "IRON", "PPUP", "PPMAX", "PROTEIN", "ZINC",
                    "RARECANDY",
                    // Most evolution stones (see also 80)
                    "EVERSTONE", "FIRESTONE", "ICESTONE", "LEAFSTONE", "MOONSTONE",
                    "SUNSTONE", "THUNDERSTONE", "WATERSTONE",
                    // Repels
                    "MAXREPEL", "REPEL", "SUPERREPEL",
                    // Mulches
                    "AMAZEMULCH", "BOOSTMULCH", "DAMPMULCH", "GOOEYMULCH", "GROWTHMULCH",
                    "RICHMULCH", "STABLEMULCH", "SURPRISEMULCH",
                    // Shards
                    "BLUESHARD", "GREENSHARD", "REDSHARD", "YELLOWSHARD",
                    // Valuables
                    "BALMMUSHROOM", "BIGMUSHROOM", "BIGNUGGET", "BIGPEARL", "COMETSHARD",
                    "NUGGET", "PEARL", "PEARLSTRING", "RELICBAND", "RELICCOPPER",
                    "RELICCROWN", "RELICGOLD", "RELICSILVER", "RELICSTATUE", "RELICVASE",
                    "STARDUST", "STARPIECE", "STRANGESOUVENIR", "TINYMUSHROOM"));
            flingPowers.add(new FlingPower(20, "CLEVERWING", "GENIUSWING", "HEALTHWING",   // :3692-3695 (Wings)
                    "MUSCLEWING", "PRETTYWING", "RESISTWING", "SWIFTWING"));
            flingPowers.add(new FlingPower(10, "AIRBALLOON", "BIGROOT", "BRIGHTPOWDER",   // :3696-3711
                    "CHOICEBAND", "CHOICESCARF", "CHOICESPECS", "DESTINYKNOT",
                    "DISCOUNTCOUPON", "EXPERTBELT", "FOCUSBAND", "FOCUSSASH", "LAGGINGTAIL",
                    "LEFTOVERS", "MENTALHERB", "METALPOWDER", "MUSCLEBAND", "POWERHERB",
                    "QUICKPOWDER", "REAPERCLOTH", "REDCARD", "RINGTARGET", "SHEDSHELL",
                    "SILKSCARF", "SILVERPOWDER", "SMOOTHROCK", "SOFTSAND", "SOOTHEBELL",
                    "WHITEHERB", "WIDELENS", "WISEGLASSES", "ZOOMLENS",
                    // Terrain seeds
                    "ELECTRICSEED", "GRASSYSEED", "MISTYSEED", "PSYCHICSEED",
                    // Nectar
                    "PINKNECTAR", "PURPLENECTAR", "REDNECTAR", "YELLOWNECTAR",
                    // Incenses
                    "FULLINCENSE", "LAXINCENSE", "LUCKINCENSE", "ODDINCENSE", "PUREINCENSE",
                    "ROCKINCENSE", "ROSEINCENSE", "SEAINCENSE", "WAVEINCENSE",
                    // Scarves
                    "BLUESCARF", "GREENSCARF", "PINKSCARF", "REDSCARF", "YELLOWSCARF"));
        }

        /** {@code pbCheckFlingSuccess(user)} (:3715-3733). */
        @Override
        public void pbCheckFlingSuccess(BattleMove move, Battler user) {
            Battle battle = user.battle;
            willFail = false;                                                // :3716
            if (noItem(user) || !user.itemActive() || user.unlosableItem(user.item)) {   // :3717
                willFail = true;
            }
            if (isBerry(battle, user.item)) {                                 // :3718
                if (battle.pbCheckOpposingAbility("UNNERVE", user.index, false) != null) {   // :3719 @battle.pbCheckOpposingAbility(:UNNERVE,user.index)
                    willFail = true;
                }
                return;                                                      // :3720
            }
            if (isMegaStone(battle, user.item)) {                             // :3722
                return;
            }
            if (Battle.NEWEST_BATTLE_MECHANICS && isTechnicalRecord(battle, user.item)) {   // :3723
                return;
            }
            boolean flingableItem = false;                                   // :3724
            for (FlingPower group : flingPowers) {                           // :3725 @flingPowers.each do |_power,items|
                for (String i : group.items) {                               // :3726 items.each do |i|
                    if (!user.item.equals(i)) {                              // :3727 next if !isConst?(user.item,PBItems,i)
                        continue;
                    }
                    flingableItem = true;                                    // :3728
                    break;                                                   // :3729
                }
                if (flingableItem) {                                         // :3731
                    break;
                }
            }
            if (!flingableItem) {                                            // :3733
                willFail = true;
            }
        }

        /** {@code pbMoveFailed?(user,targets)} (:3736-3741). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (willFail) {                                                  // :3737
                user.battle.display("但是失败了！");                            // :3738
                return true;                                                 // :3739
            }
            return false;                                                    // :3741
        }

        /** {@code pbDisplayUseMessage(user)} (:3744-3749). */
        @Override
        public void pbDisplayUseMessage(BattleMove move, Battler user) {
            super.pbDisplayUseMessage(move, user);                            // :3745 super
            pbCheckFlingSuccess(move, user);                                  // :3746
            if (!willFail) {                                                 // :3747
                user.battle.display(user.pbThis() + "投掷了" + user.itemName() + "！");   // :3748
            }
        }

        /** {@code pbNumHits(user,targets); return 1; end} (:3752). */
        @Override
        public int pbNumHits(BattleMove move, Battler user, Array<Battler> targets) {
            return 1;                                                        // :3752
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:3754-3766). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            Battle battle = user.battle;
            if (isTechnicalRecord(battle, user.item)) {                       // :3755
                // :3756 movedata = pbGetMoveData(pbGetMachine(user.item))
                PbsData.Move movedata = moveData(battle, machineMove(battle, user.item));
                // 登记: 插件缺陷 —— ITEM_MACHINE 查不到招式时 movedata 是 nil，:3757 的
                //        movedata[MOVE_CATEGORY] 在 Ruby 会 NoMethodError；照抄不修正
                //        （Java 在 :3757 抛 NPE，同样是“即时报错”）。
                if ("Status".equals(movedata.category)) {                     // :3757 MOVE_CATEGORY == 2 (status move)
                    return 10;                                               // :3757
                }
                if (movedata.power < 10) {                                    // :3758 MOVE_BASE_DAMAGE < 10
                    return 10;                                               // :3758
                }
                return movedata.power;                                        // :3759 MOVE_BASE_DAMAGE
            }
            if (isBerry(battle, user.item)) {                                 // :3761
                return 10;
            }
            if (isMegaStone(battle, user.item)) {                             // :3762
                return 80;
            }
            for (FlingPower group : flingPowers) {                            // :3763 @flingPowers.each do |power,items|
                for (String i : group.items) {                                // :3764 items.each { |i| return power if isConst?(...) }
                    if (user.item.equals(i)) {
                        return group.power;                                   // :3764
                    }
                }
            }
            return 10;                                                        // :3766
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:3769-3787). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = user.battle;
            if (target.damageState.substitute) {                              // :3770
                return;
            }
            if (target.hasActiveItem("COVERTCLOAK")) {                        // :3771
                return;
            }
            if (target.hasActiveAbility("SHIELDDUST") && !battle.moldBreaker) {   // :3772
                return;
            }
            if ("POISONBARB".equals(user.item)) {                             // :3773 isConst?(user.item,PBItems,:POISONBARB)
                if (target.pbCanPoison(user, false, move)) {                  // :3774
                    target.pbPoison(user, null, false);                       // :3774 pbPoison(user)
                }
            } else if ("TOXICORB".equals(user.item)) {                        // :3775
                if (target.pbCanPoison(user, false, move)) {                  // :3776
                    target.pbPoison(user, null, true);                        // :3776 pbPoison(user,nil,true)
                }
            } else if ("FLAMEORB".equals(user.item)) {                        // :3777
                if (target.pbCanBurn(user, false, move)) {                    // :3778
                    target.pbBurn(user, null);                                // :3778 pbBurn(user)
                }
            } else if ("LIGHTBALL".equals(user.item)) {                       // :3779
                if (target.pbCanParalyze(user, false, move)) {                // :3780
                    target.pbParalyze(user, null);                            // :3780 pbParalyze(user)
                }
            } else if ("KINGSROCK".equals(user.item)                          // :3781
                    || "RAZORFANG".equals(user.item)) {                       // :3782
                pbFlinch(target, user);                                       // :3783 target.pbFlinch(user)
            } else {                                                          // :3784
                target.pbHeldItemTriggerCheck(user.item, true);                // :3785 target.pbHeldItemTriggerCheck(user.item,true)
            }
        }

        /** {@code pbEndOfMoveUsageEffect(user,targets,numHits,switchedBattlers)} (:3789-3795). */
        @Override
        public void pbEndOfMoveUsageEffect(BattleMove move, Battler user, Array<Battler> targets, int numHits,
                                           IntArray switchedBattlers) {
            // NOTE (:3790-3793): the item is consumed even if this move was
            // protected against or it missed; it is not consumed if the target was
            // switched out by an effect like a Red Card. There is no item
            // consumption animation.
            if (hasItem(user.item)) {                                        // :3794 user.item>0
                user.pbConsumeItem(true, true, false);                        // :3794
            }
        }
    }

    // ==================================================================
    // 0F8-0F9: item/field lock (Move_Effects_080-0FF.rb:3804-3840)
    // ==================================================================

    /** {@code class PokeBattle_Move_0F8 < PokeBattle_Move} (:3804-3816): Embargo. */
    public static class PokeBattle_Move_0F8 extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:3805-3810). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.effects.intVal(PBEffects.Battler.Embargo) > 0) {        // :3806
                user.battle.display("但是失败了！");                            // :3807
                return true;                                                 // :3808
            }
            return false;                                                    // :3810
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:3813-3815). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.Embargo, 5);                 // :3814
            user.battle.display(target.pbThis() + "无法使用道具了！");           // :3815
        }
    }

    /** {@code class PokeBattle_Move_0F9 < PokeBattle_Move} (:3825-3840): Magic Room. */
    public static class PokeBattle_Move_0F9 extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:3826-3833). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            Battle battle = user.battle;
            if (battle.field.effects.intVal(PBEffects.Field.MagicRoom) > 0) {   // :3827
                battle.field.effects.set(PBEffects.Field.MagicRoom, 0);        // :3828
                battle.display("空间恢复正常了！");                             // :3829
            } else {
                battle.field.effects.set(PBEffects.Field.MagicRoom, 5);        // :3831
                battle.display("制造出了使宝可梦携带道具\n效果消失的魔法空间！");   // :3832
            }
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} (:3836-3839). */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets, int hitNum,
                                    boolean showAnimation) {
            if (user.battle.field.effects.intVal(PBEffects.Field.MagicRoom) > 0) {   // :3837 # No animation
                return;
            }
            super.pbShowAnimation(move, id, user, targets, hitNum, showAnimation);   // :3838 super
        }
    }

    // ==================================================================
    // 0FA-0FE: recoil moves (Move_Effects_080-0FF.rb:3847-3907)
    // ==================================================================

    /** {@code class PokeBattle_Move_0FA < PokeBattle_RecoilMove} (:3847-3850): 1/4 of the damage dealt. */
    public static class PokeBattle_Move_0FA extends MoveEffectsGeneric.PokeBattle_RecoilMove {

        /** {@code pbRecoilDamage(user,target)} (:3848-3850). */
        @Override
        public int pbRecoilDamage(BattleMove move, Battler user, Battler target) {
            return Math.round(target.damageState.totalHPLost / 4.0f);          // :3849 (totalHPLost/4.0).round
        }
    }

    /** {@code class PokeBattle_Move_0FB < PokeBattle_RecoilMove} (:3858-3861): 1/3 of the damage dealt. */
    public static class PokeBattle_Move_0FB extends MoveEffectsGeneric.PokeBattle_RecoilMove {

        /** {@code pbRecoilDamage(user,target)} (:3859-3861). */
        @Override
        public int pbRecoilDamage(BattleMove move, Battler user, Battler target) {
            return Math.round(target.damageState.totalHPLost / 3.0f);          // :3860 (totalHPLost/3.0).round
        }
    }

    /** {@code class PokeBattle_Move_0FC < PokeBattle_RecoilMove} (:3870-3873): 1/2 of the damage dealt. */
    public static class PokeBattle_Move_0FC extends MoveEffectsGeneric.PokeBattle_RecoilMove {

        /** {@code pbRecoilDamage(user,target)} (:3871-3873). */
        @Override
        public int pbRecoilDamage(BattleMove move, Battler user, Battler target) {
            return Math.round(target.damageState.totalHPLost / 2.0f);          // :3872 (totalHPLost/2.0).round
        }
    }

    /** {@code class PokeBattle_Move_0FD < PokeBattle_RecoilMove} (:3882-3891): Volt Tackle. */
    public static class PokeBattle_Move_0FD extends MoveEffectsGeneric.PokeBattle_RecoilMove {

        /** {@code pbRecoilDamage(user,target)} (:3883-3885). */
        @Override
        public int pbRecoilDamage(BattleMove move, Battler user, Battler target) {
            return Math.round(target.damageState.totalHPLost / 3.0f);          // :3884
        }

        /** {@code pbAdditionalEffect(user,target)} (:3887-3890). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {                              // :3888
                return;
            }
            if (target.pbCanParalyze(user, false, move)) {                     // :3889
                target.pbParalyze(user, null);                                // :3889 target.pbParalyze(user)
            }
        }
    }

    /** {@code class PokeBattle_Move_0FE < PokeBattle_RecoilMove} (:3899-3907): Flare Blitz. */
    public static class PokeBattle_Move_0FE extends MoveEffectsGeneric.PokeBattle_RecoilMove {

        /** {@code pbRecoilDamage(user,target)} (:3900-3902). */
        @Override
        public int pbRecoilDamage(BattleMove move, Battler user, Battler target) {
            return Math.round(target.damageState.totalHPLost / 3.0f);          // :3901
        }

        /** {@code pbAdditionalEffect(user,target)} (:3904-3907). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {                              // :3905
                return;
            }
            if (target.pbCanBurn(user, false, move)) {                         // :3906
                target.pbBurn(user, null);                                    // :3906 target.pbBurn(user)
            }
        }
    }

    // ==================================================================
    // 0FF: weather (Move_Effects_080-0FF.rb:3915-3919)
    // ==================================================================

    /** {@code class PokeBattle_Move_0FF < PokeBattle_WeatherMove} (:3915-3919): Sunny Day. */
    public static class PokeBattle_Move_0FF extends MoveEffectsGeneric.PokeBattle_WeatherMove {

        /** {@code initialize(battle,move)} (:3916-3918): {@code super} then {@code @weatherType}. */
        public PokeBattle_Move_0FF() {
            // :3917 super —— Java 的基类构造器是无参的，隐式调用
            weatherType = PBWeather.Sun;                                     // :3918
        }
    }

    // ==================================================================
    // File-local M0 helpers (Lead ruling: no shared-file edits, one file per L2
    // author). Every THROWING helper below stands for a plugin method this
    // runtime has not landed - never a made-up value. They are collected here so
    // the retire pass has one place to look per file.
    // ==================================================================

    /**
     * {@code Battler#pbRecoverHPFromDrain(amt,target,msg=nil)}
     * (Battler_ChangeSelf:33-39, used by 0DD :2800 and 0DE :2824). 本运行时缺此方法。
     */
    private static int pbRecoverHPFromDrain(Battler battler, int amount, Battler target) {
        throw new UnsupportedOperationException("M0 待接线: Battler_ChangeSelf:33-39 pbRecoverHPFromDrain");
    }

    /**
     * {@code Battler#pbSleepSelf(msg,duration)} (used by Rest, 0D9 :2712).
     * 本运行时缺此方法。
     */
    private static void pbSleepSelf(Battler battler, String msg, int duration) {
        throw new UnsupportedOperationException("M0 待接线: Battler pbSleepSelf(msg,duration) (0D9 :2712)");
    }

    /**
     * {@code Battle#pbAbleNonActiveCount(idxBattler)} (used by 0E5 :3012 and
     * 0E7 :3064). 本运行时缺此方法（{@code Battle.allFainted} 是「全部倒下」的零测试，
     * 不是「还有几个能上场的后备」）。
     */
    private static int pbAbleNonActiveCount(Battle battle, int idxBattler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battle pbAbleNonActiveCount(idxOwnSide)");
    }

    /**
     * {@code battle.rules["perishsongclause"]} (PokeBattle_Battle:71/:148, read by
     * 0E5 :3011 and 0E7 :3063). 本运行时缺此方法。
     */
    private static boolean battleRules(Battle battle, String rule) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battle:148 battle.rules[\"" + rule + "\"]");
    }

    /**
     * {@code Battler#pbFlinch(_user=nil)} (Battler_Statuses:619, used by Fling
     * 0F7 :3782). 本运行时缺此方法。
     */
    private static void pbFlinch(Battler battler, Battler user) {
        throw new UnsupportedOperationException("M0 待接线: Battler_Statuses:619 pbFlinch");
    }

    // The name-taking pbHeldItemTriggerCheck used by Bug Bite/Pluck (0F4 :3544)
    // and Fling (0F7 :3784) is NOT a stub: this runtime already landed
    // Battler.pbHeldItemTriggerCheck(String forcedItemName, boolean fling)
    // (Battler.java:3564-3588, the internal-name port of
    // Battler_AbilityAndItem:235-249) - the same overload
    // MoveEffects_180_1FF.java:168/221 calls for the same Ruby idiom - so those
    // two call sites call it directly. No file-local helper is needed, and the
    // int-taking overload is only for the no-argument Ruby calls
    // (user.pbHeldItemTriggerCheck = forcedItem 0, fling false).

    /**
     * {@code Battler#pbEffectsOnSwitchIn(switchIn=false)}
     * (Battler_AbilityAndItem:5-35, used by 0EB :3199, 0EC :3242, 0ED :3274,
     * 0EE :3308). 本运行时缺此方法（它要 {@code Battle.pbOnActiveOne}、
     * {@code pbPrimalReversion}、{@code BattleHandlers.triggerAbilityOnSwitchIn} 等一整套入场钩子）。
     */
    private static void pbEffectsOnSwitchIn(Battler battler, boolean switchIn) {
        throw new UnsupportedOperationException("M0 待接线: Battler_AbilityAndItem:5-35 pbEffectsOnSwitchIn");
    }

    /**
     * {@code Battle#pbPursuit(idxSwitcher)} (Battle_Phase_Attack:24, used by
     * 0ED :3266 and 0EE :3300). 本运行时缺此方法。
     */
    private static void pbPursuit(Battle battle, int idxSwitcher) {
        throw new UnsupportedOperationException("M0 待接线: Battle_Phase_Attack:24 pbPursuit");
    }

    /**
     * {@code Battle#pbPriority(all=true)} (PokeBattle_Battle, used by 0EB :3198
     * and 0EC :3241). 本运行时缺此方法（{@code Battle.fieldedBySpeed()} 是私有的
     * 单打视图，且语义只覆盖场上两人）。
     */
    private static Array<Battler> pbPriority(Battle battle, boolean all) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battle pbPriority(all)");
    }

    /**
     * {@code Battler#isCommander?} (PokeBattle_Battler:866-869, used by 0EB :3134,
     * 0EC :3187). 本运行时缺此方法（没有 commander 状态）。
     */
    private static boolean isCommander(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:866-869 isCommander?");
    }

    // ------------------------------------------------------------------
    // Landed data lookups (no stubs): the PBS item/move tables
    // ------------------------------------------------------------------

    /** The PBS item entry of an item internal name, or {@code null} (Ruby {@code pbLoadItemsData[id]} = nil). */
    private static PbsData.Item itemEntry(Battle battle, String item) {
        PbsData pbs = battle == null ? null : battle.pbs();
        return pbs == null || item == null ? null : pbs.item(item);
    }

    /** {@code pbIsBerry?(item)} (PItem_Items:99-102): {@code ITEM_TYPE == 5}. */
    private static boolean isBerry(Battle battle, String item) {
        PbsData.Item entry = itemEntry(battle, item);
        return entry != null && entry.isBerry();
    }

    /** {@code pbIsGem?(item)} (PItem_Items:124-127): {@code ITEM_TYPE == 10}. */
    private static boolean isGem(Battle battle, String item) {
        PbsData.Item entry = itemEntry(battle, item);
        return entry != null && entry.type == 10;
    }

    /** {@code pbIsMegaStone?(item)} (PItem_Items:134-137): {@code ITEM_TYPE == 12}. */
    private static boolean isMegaStone(Battle battle, String item) {
        PbsData.Item entry = itemEntry(battle, item);
        return entry != null && entry.type == 12;
    }

    /** {@code pbIsTechnicalRecord?(item)} (PItem_Items:74-77): {@code ITEM_FIELD_USE == 6}. */
    private static boolean isTechnicalRecord(Battle battle, String item) {
        PbsData.Item entry = itemEntry(battle, item);
        return entry != null && entry.fieldUse == 6;
    }

    /**
     * {@code pbGetMachine(item)} (PItem_Items:54-57): {@code ITEM_MACHINE}, the
     * move a TM/TR teaches.
     *
     * <p>Ruby's {@code ret || 0} spells "no machine" as move id 0; this runtime's
     * move identity is the internal name, so "no machine" is {@code null}.</p>
     */
    private static String machineMove(Battle battle, String item) {
        PbsData.Item entry = itemEntry(battle, item);
        return entry == null ? null : entry.machine;
    }

    /**
     * {@code pbGetMoveData(move)} (PBMove.rb:32) for a move internal name;
     * {@code null} = Ruby {@code nil}.
     */
    private static PbsData.Move moveData(Battle battle, String internalName) {
        PbsData pbs = battle == null ? null : battle.pbs();
        return pbs == null || internalName == null ? null : pbs.move(internalName);
    }

    /** {@code PBItems.getName(item)} (Compiler_PBS:446) - the item's display name. */
    private static String itemDisplayName(Battle battle, String item) {
        PbsData.Item entry = itemEntry(battle, item);
        return entry == null ? null : entry.name;
    }

    // ------------------------------------------------------------------
    // Tiny landed predicates used by the classes above
    // ------------------------------------------------------------------

    /** Ruby {@code item==0}: this runtime's item identity is the internal name, so "no item" is empty. */
    private static boolean noItem(Battler battler) {
        return battler.item == null || battler.item.isEmpty();
    }

    /** Ruby {@code item!=0} / {@code item>0}. */
    private static boolean hasItem(String item) {
        return item != null && !item.isEmpty();
    }

    /**
     * {@code String#starts_with_vowel?} (:3585, PItem_Items:986, PokeBattle_BattleCommon:85).
     *
     * <p><b>登记</b>: the monkey-patch's own definition is not part of this
     * batch's reference set, so its exact body cannot be quoted. Both branches of
     * {@code PokeBattle_Move_0F6} :3585-3589 display the <b>same</b>
     * {@code _INTL} string, so the predicate cannot change the behaviour of this
     * class; the implementation below is the only sensible reading
     * ("starts with a, e, i, o, u") and keeps the transcription a real branch
     * instead of a hard-coded one. Lead review point.</p>
     */
    private static boolean startsWithVowel(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return "aeiouAEIOU".indexOf(text.charAt(0)) >= 0;
    }

    /**
     * {@code PokeBattle_Move.pbFromPBMove} (PokeBattle_Move.rb:51-59) for the
     * 43 function-code classes this file holds: the Ruby builds
     * {@code "PokeBattle_Move_#{function}"} and instantiates it on demand, so each
     * code is bound to its no-argument constructor here and
     * {@link MoveEffectRegistry#of} calls it lazily (some constructors still touch
     * not-yet-wired stubs, which must not break the whole table).
     *
     * <p>Generated by {@code __l2register.mjs} - re-run it with {@code --write}
     * after the class set changes.</p>
     */
    static void register() {
        MoveEffectRegistry.register("0D5", PokeBattle_Move_0D5::new);
        MoveEffectRegistry.register("0D6", PokeBattle_Move_0D6::new);
        MoveEffectRegistry.register("0D7", PokeBattle_Move_0D7::new);
        MoveEffectRegistry.register("0D8", PokeBattle_Move_0D8::new);
        MoveEffectRegistry.register("0D9", PokeBattle_Move_0D9::new);
        MoveEffectRegistry.register("0DA", PokeBattle_Move_0DA::new);
        MoveEffectRegistry.register("0DB", PokeBattle_Move_0DB::new);
        MoveEffectRegistry.register("0DC", PokeBattle_Move_0DC::new);
        MoveEffectRegistry.register("0DD", PokeBattle_Move_0DD::new);
        MoveEffectRegistry.register("0DE", PokeBattle_Move_0DE::new);
        MoveEffectRegistry.register("0DF", PokeBattle_Move_0DF::new);
        MoveEffectRegistry.register("0E0", PokeBattle_Move_0E0::new);
        MoveEffectRegistry.register("0E1", PokeBattle_Move_0E1::new);
        MoveEffectRegistry.register("0E2", PokeBattle_Move_0E2::new);
        MoveEffectRegistry.register("0E3", PokeBattle_Move_0E3::new);
        MoveEffectRegistry.register("0E4", PokeBattle_Move_0E4::new);
        MoveEffectRegistry.register("0E5", PokeBattle_Move_0E5::new);
        MoveEffectRegistry.register("0E6", PokeBattle_Move_0E6::new);
        MoveEffectRegistry.register("0E7", PokeBattle_Move_0E7::new);
        MoveEffectRegistry.register("0E8", PokeBattle_Move_0E8::new);
        MoveEffectRegistry.register("0E9", PokeBattle_Move_0E9::new);
        MoveEffectRegistry.register("0EA", PokeBattle_Move_0EA::new);
        MoveEffectRegistry.register("0EB", PokeBattle_Move_0EB::new);
        MoveEffectRegistry.register("0EC", PokeBattle_Move_0EC::new);
        MoveEffectRegistry.register("0ED", PokeBattle_Move_0ED::new);
        MoveEffectRegistry.register("0EE", PokeBattle_Move_0EE::new);
        MoveEffectRegistry.register("0EF", PokeBattle_Move_0EF::new);
        MoveEffectRegistry.register("0F0", PokeBattle_Move_0F0::new);
        MoveEffectRegistry.register("0F1", PokeBattle_Move_0F1::new);
        MoveEffectRegistry.register("0F2", PokeBattle_Move_0F2::new);
        MoveEffectRegistry.register("0F3", PokeBattle_Move_0F3::new);
        MoveEffectRegistry.register("0F4", PokeBattle_Move_0F4::new);
        MoveEffectRegistry.register("0F5", PokeBattle_Move_0F5::new);
        MoveEffectRegistry.register("0F6", PokeBattle_Move_0F6::new);
        MoveEffectRegistry.register("0F7", PokeBattle_Move_0F7::new);
        MoveEffectRegistry.register("0F8", PokeBattle_Move_0F8::new);
        MoveEffectRegistry.register("0F9", PokeBattle_Move_0F9::new);
        MoveEffectRegistry.register("0FA", PokeBattle_Move_0FA::new);
        MoveEffectRegistry.register("0FB", PokeBattle_Move_0FB::new);
        MoveEffectRegistry.register("0FC", PokeBattle_Move_0FC::new);
        MoveEffectRegistry.register("0FD", PokeBattle_Move_0FD::new);
        MoveEffectRegistry.register("0FE", PokeBattle_Move_0FE::new);
        MoveEffectRegistry.register("0FF", PokeBattle_Move_0FF::new);
    }

}
