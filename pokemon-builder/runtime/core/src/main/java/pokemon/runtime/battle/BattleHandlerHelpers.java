package pokemon.runtime.battle;

import pokemon.runtime.pokemon.PbsData;

/**
 * Stage 4 / M0: the 7 top-level {@code pbBattle*} helpers of
 * {@code BattleHandlers.rb:544-675}, transcribed line by line.
 *
 * <p>In Ruby these are global methods; here they are {@code public static} on a
 * final utility class. Their names and parameter order are unchanged, including
 * Ruby's default arguments, which become overloads:</p>
 * <ul>
 * <li>{@code pbBattleStatIncreasingBerry(...,increment=1)} ({@code :570})</li>
 * <li>{@code pbBattleWeatherAbility(...,ignorePrimal=false)} ({@code :657})</li>
 * </ul>
 *
 * <h2>Stub retirement (task: retire ②)</h2>
 * The bodies now call the real {@code Battler}/{@code Battle}/{@code PBTypes}/
 * {@code PBNatures} methods wherever they exist ({@code battler.canHeal()},
 * {@code battle.display(...)}, {@code battler.pbRaiseStatStage(...)}, ...).
 * Only these three calls are still {@link PendingApi} stubs, each because the
 * real method does not exist yet:
 * <ul>
 * <li>{@link PendingApi#pbCommonAnimation(Battle, String, Battler)} - the
 *     {@code @scene.pbCommonAnimation} animation pipeline
 *     ({@code PokeBattle_AnimationPlayer}) is not modelled; {@code :548},
 *     {@code :581}, {@code :654}.</li>
 * <li>{@link PendingApi#pbConfuse(Battler)} - {@code Battler#pbConfuse}
 *     (Battler_Statuses:531-540) needs {@code pbConfusionDuration} and the
 *     item/ability cure checks; {@code :566}.</li>
 * <li>{@link PendingApi#isPledgeMove(BattleMove)} - {@code move.is_a?(PokeBattle_PledgeMove)},
 *     {@code :635}.</li>
 * </ul>
 *
 * <h2>Documented deviations</h2>
 * <ul>
 * <li>{@code battler.totalhp} is this runtime's {@link Battler#maxHp()}
 *     ({@code @totalhp} is assigned from {@code pkmn.totalhp} at
 *     Battler_Initialize; the runtime keeps it on the Pokemon).</li>
 * <li>{@code item} is the internal-name String, matching
 *     {@code HandlerHash}'s documented deviation; {@code user.item} likewise
 *     ({@code Pokemon.item} is a String here).</li>
 * <li>{@code mults} is {@code float[]} - see {@link BattleHandlers}' javadoc.</li>
 * <li>{@code PBDebug.log} (2 calls: {@code :555}, {@code :578}) is a debug-only
 *     log with no gameplay effect and no runtime counterpart, so it is kept as a
 *     comment at its line instead of being pointed at something else.</li>
 * <li>{@code _INTL("{1}...",x)} strings are concatenated at the call site, the
 *     convention the rest of this runtime already uses
 *     (e.g. {@code BattleScreen} "{1}升到了{2}级！"), with the plugin's wording
 *     copied verbatim.</li>
 * <li>{@code isConst?(x,PBTypes,:FOO)} is {@code "FOO".equals(x)}: type identity
 *     is the internal-name String, the convention
 *     {@code AbilitiesDamageUser}/{@code AbilitiesDamageTarget} already use.</li>
 * <li>{@code battler.nature} (PokeBattle_Battler:137) is
 *     {@link #natureId(Battler)} = {@code pokemon.nature.id} (the runtime keeps a
 *     {@code PbsData.Nature} record, Ruby a numeric id).</li>
 * <li>{@code PBItems.getName(item)} is {@link #itemName(Battle, String)} =
 *     {@code pbs.item(item).name}, {@code null} when the table has no such item
 *     (Ruby's generated {@code getName} is then {@code nil}).</li>
 * </ul>
 */
public final class BattleHandlerHelpers {

    private BattleHandlerHelpers() {
    }

    /**
     * {@code PBItems.getName(item)} (Compiler_PBS:446 generates
     * {@code def self.getName(id)}): the item's display name, {@code null} when
     * the PBS table has no such item - Ruby's {@code getName} is then
     * {@code nil}. The argument is the internal name, per
     * {@code HandlerHash}'s documented deviation.
     */
    private static String itemName(Battle battle, String item) {
        PbsData.Item data = battle == null || battle.pbs() == null ? null : battle.pbs().item(item);
        return data == null ? null : data.name;
    }

    /**
     * {@code Battler#nature} (PokeBattle_Battler:137
     * {@code return @pokemon ? @pokemon.nature : 0}): the numeric nature id.
     * The runtime stores a {@code PbsData.Nature} record on the Pokemon.
     */
    private static int natureId(Battler battler) {
        return battler.pokemon == null || battler.pokemon.nature == null ? 0 : battler.pokemon.nature.id;
    }

    /**
     * {@code pbBattleConfusionBerry} (BattleHandlers.rb:544-568): the
     * confusion-curing berries. {@code flavor} is the nature flavor index the
     * caller passes ({@code 0..4}); {@code confuseMsg} is the caller's
     * {@code _INTL} line.
     *
     * @return true once the berry was consumed (the caller's branch consumes it)
     */
    public static boolean pbBattleConfusionBerry(Battler battler, Battle battle, String item, boolean forced,
                                                 int flavor, String confuseMsg) {
        if (!forced && !battler.canHeal()) {                        // :545
            return false;
        }
        if (!forced && !battler.pbCanConsumeBerry(item, false)) { // :546
            return false;
        }
        String itemName = itemName(battle, item);                   // :547
        if (!forced) {
            PendingApi.pbCommonAnimation(battle, "EatBerry", battler);        // :548
        }
        // :549 battler.totalhp - see the class javadoc.
        int amt = Battle.NEWEST_BATTLE_MECHANICS
                ? battler.pbRecoverHP(battler.maxHp() / 3)
                : battler.pbRecoverHP(battler.maxHp() / 8);
        if (battler.hasActiveAbility("RIPEN")) {                  // :550
            amt *= 2;                                                         // :551
        }
        if (amt > 0) {                                                        // :553
            if (forced) {
                // :555 PBDebug.log("[Item triggered] Forced consuming of #{itemName}") - debug log, not modelled
                battle.display(battler.pbThis() + "的HP回复了。");            // :557
            } else {
                battle.display(battler.pbThis() + "使用" + itemName + "回复了HP！");                // :559
            }
        }
        int nUp = PBNatures.getStatRaised(natureId(battler));       // :561
        int nDn = PBNatures.getStatLowered(natureId(battler));      // :562
        if (nUp != nDn && nDn - 1 == flavor) {                                // :563
            battle.display(confuseMsg);                         // :564
            if (battler.pbCanConfuse(null, false, null, true)) {                // :565
                PendingApi.pbConfuse(battler);
            }
        }
        return true;                                                          // :567
    }

    /**
     * {@code pbBattleStatIncreasingBerry(...,increment=1)}
     * (BattleHandlers.rb:570-583) with Ruby's default {@code increment} of 1.
     */
    public static boolean pbBattleStatIncreasingBerry(Battler battler, Battle battle, String item, boolean forced,
                                                      int stat) {
        return pbBattleStatIncreasingBerry(battler, battle, item, forced, stat, 1);   // :570 default increment=1
    }

    /** {@code pbBattleStatIncreasingBerry} (BattleHandlers.rb:570-583). */
    public static boolean pbBattleStatIncreasingBerry(Battler battler, Battle battle, String item, boolean forced,
                                                      int stat, int increment) {
        // :571 pbCanConsumeBerry?(item) uses the Ruby default alwaysCheckGluttony=true
        // (Battler_AbilityAndItem:152).
        if (!forced && !battler.pbCanConsumeBerry(item, true)) {
            return false;
        }
        if (!battler.pbCanRaiseStatStage(stat, battler)) {        // :572
            return false;
        }
        String itemName = itemName(battle, item);                   // :573
        if (battler.hasActiveAbility("RIPEN")) {                  // :574
            increment *= 2;                                                   // :575
        }
        if (forced) {                                                         // :577
            // :578 PBDebug.log("[Item triggered] Forced consuming of #{itemName}") - debug log, not modelled
            return battler.pbRaiseStatStage(stat, increment, battler);          // :579
        }
        PendingApi.pbCommonAnimation(battle, "EatBerry", battler);            // :581
        return battler.pbRaiseStatStageByCause(stat, increment, battler, itemName);   // :582
    }

    /**
     * {@code pbBattleMoveImmunityStatAbility} (BattleHandlers.rb:587-607): an
     * ability that is immune to a type and raises a stat instead.
     * {@code moveType} / {@code immuneType} are type internal names (see
     * {@link BattleHandlers}' javadoc).
     */
    public static boolean pbBattleMoveImmunityStatAbility(Battler user, Battler target, BattleMove move,
                                                          String moveType, String immuneType, int stat, int increment,
                                                          Battle battle) {
        if (user.index == target.index) {                                     // :588
            return false;
        }
        if (!immuneType.equals(moveType)) {                      // :589
            return false;
        }
        battle.showAbilitySplash(target);                       // :590
        if (target.pbCanRaiseStatStage(stat, target)) {           // :591
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                            // :592
                target.pbRaiseStatStage(stat, increment, target); // :593
            } else {
                target.pbRaiseStatStageByCause(stat, increment, target,
                        target.abilityName());                      // :595
            }
        } else {
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                            // :598
                battle.display("这不能影响" + target.pbThis(true) + "……");                 // :599
            } else {
                battle.display(target.pbThis() + "的" + target.abilityName()
                                + "使" + move.name() + "无效了！");                            // :601-602
            }
        }
        battle.hideAbilitySplash(target);                       // :605
        return true;                                                          // :606
    }

    /**
     * {@code pbBattleMoveImmunityHealAbility} (BattleHandlers.rb:611-631): an
     * ability that is immune to a type and heals 1/4 of total HP instead.
     */
    public static boolean pbBattleMoveImmunityHealAbility(Battler user, Battler target, BattleMove move,
                                                          String moveType, String immuneType, Battle battle) {
        if (user.index == target.index) {                                     // :612
            return false;
        }
        if (!immuneType.equals(moveType)) {                      // :613
            return false;
        }
        battle.showAbilitySplash(target);                       // :614
        // :615 target.totalhp/4 - see the class javadoc.
        if (target.canHeal() && target.pbRecoverHP(target.maxHp() / 4) > 0) {
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                            // :616
                battle.display(target.pbThis() + "的HP回复了。");        // :617
            } else {
                battle.display(target.pbThis() + "的" + target.abilityName()
                                + "回复了HP。");                                             // :619
            }
        } else {
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                            // :622
                battle.display("这不能影响" + target.pbThis(true) + "……");                 // :623
            } else {
                battle.display(target.pbThis() + "的" + target.abilityName()
                                + "使" + move.name() + "无效了！");                            // :625-626
            }
        }
        battle.hideAbilitySplash(target);                       // :629
        return true;                                                          // :630
    }

    /**
     * {@code pbBattleGem} (BattleHandlers.rb:633-643): a Gem boosts one move of
     * its type and is consumed.
     *
     * <p>{@code user.effects[PBEffects::GemConsumed] = user.item} stores the
     * held item, which this runtime represents by its internal-name String
     * ({@code Pokemon.item}); {@link EffectMap#set(int, Object)} accepts it.</p>
     */
    public static void pbBattleGem(Battler user, String type, BattleMove move, float[] mults, String moveType) {
        if (PendingApi.isPledgeMove(move)) {                                  // :635
            return;
        }
        if (!type.equals(moveType)) {                            // :636
            return;
        }
        user.effects.set(PBEffects.Battler.GemConsumed, user.item);   // :637
        if (Battle.NEWEST_BATTLE_MECHANICS) {                                 // :638
            mults[BattleHandlers.BASE_DMG_MULT] *= 1.3f;                      // :639
        } else {
            mults[BattleHandlers.BASE_DMG_MULT] *= 1.5f;                      // :641
        }
    }

    /**
     * {@code pbBattleTypeWeakingBerry} (BattleHandlers.rb:645-655): a
     * resist-berry weakens the move's final damage.
     */
    public static void pbBattleTypeWeakingBerry(String type, String moveType, Battler target, float[] mults) {
        if (!type.equals(moveType)) {                            // :646
            return;
        }
        if (PBTypes.resistant(target.damageState.typeMod)
                && !"NORMAL".equals(moveType)) {                 // :647
            return;
        }
        if (target.hasActiveAbility("RIPEN")) {                   // :648
            mults[BattleHandlers.FINAL_DMG_MULT] =
                    Math.round(mults[BattleHandlers.FINAL_DMG_MULT] / 4f);    // :649
        } else {
            mults[BattleHandlers.FINAL_DMG_MULT] =
                    Math.round(mults[BattleHandlers.FINAL_DMG_MULT] / 2f);    // :651
        }
        target.damageState.berryWeakened = true;                 // :653
        PendingApi.pbCommonAnimation(target.battle, "EatBerry", target);   // :654
    }

    /**
     * {@code pbBattleWeatherAbility(...,ignorePrimal=false)}
     * (BattleHandlers.rb:657-675) with Ruby's default {@code ignorePrimal}.
     */
    public static void pbBattleWeatherAbility(int weather, Battler battler, Battle battle) {
        pbBattleWeatherAbility(weather, battler, battle, false);              // :657 default ignorePrimal=false
    }

    /**
     * {@code pbBattleWeatherAbility} (BattleHandlers.rb:657-675).
     *
     * <p>{@code battle.field.weather} is read four times, exactly as Ruby does
     * ({@code BattleField.weather}, the real field). The comment at {@code :673}
     * notes that the ability splash is hidden again inside
     * {@code pbStartWeather} - hence no {@code pbHideAbilitySplash} here.</p>
     */
    public static void pbBattleWeatherAbility(int weather, Battler battler, Battle battle, boolean ignorePrimal) {
        if (!ignorePrimal
                && (battle.field.weather == PBWeather.HarshSun       // :658-659
                || battle.field.weather == PBWeather.HeavyRain       // :660
                || battle.field.weather == PBWeather.StrongWinds)) { // :661
            return;
        }
        if (battle.field.weather == weather) {                      // :662
            return;
        }
        battle.showAbilitySplash(battler);                       // :663
        if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                                // :664
            battle.display(battler.pbThis() + "的" + battler.abilityName() + "被触发了！");   // :665
        }
        boolean fixedDuration = false;                                         // :667
        if (Battle.NEWEST_BATTLE_MECHANICS                                     // :668
                && weather != PBWeather.HarshSun                               // :669
                && weather != PBWeather.HeavyRain                              // :670
                && weather != PBWeather.StrongWinds) {                         // :671
            fixedDuration = true;
        }
        battle.pbStartWeather(battler, weather, fixedDuration);     // :672
        // :673 NOTE: The ability splash is hidden again in def pbStartWeather.
    }
}
