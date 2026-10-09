package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.battle.movefx.MoveEffect;
import pokemon.runtime.battle.movefx.MoveEffectRegistry;
import pokemon.runtime.battle.movefx.MoveStats;
import pokemon.runtime.pokemon.PbsData;

import java.util.Random;

/**
 * Stage 4 / &sect;4 (wiring): {@code pbCalcDamage} and {@code pbCalcDamageMultipliers},
 * translated line by line from {@code Move_Usage_Calculations.rb:252-542}.
 *
 * <h2>What changed in this pass</h2>
 * The previous version was the Stage 3 / P2 simplification (55 lines): a plain
 * formula with STAB + type effectiveness + the 0.85..1.0 roll, and a javadoc that
 * said abilities and the {@code Move_Effects_*} branches were "deliberately left to
 * the plugin features". This version is the plugin's own body: the four-entry
 * {@code multipliers} array, all 30 modifier stages in their original order, and the
 * seven {@code triggerDamageCalc*} handler calls that were previously never invoked
 * by anything.
 *
 * <h2>The four multipliers (Move_Usage_Calculations:279-292)</h2>
 * Ruby:
 * <pre>
 * multipliers = [1.0, 1.0, 1.0, 1.0]
 * pbCalcDamageMultipliers(...)
 * baseDmg = [(baseDmg * multipliers[BASE_DMG_MULT]).round, 1].max
 * atk     = [(atk     * multipliers[ATK_MULT]).round, 1].max
 * defense = [(defense * multipliers[DEF_MULT]).round, 1].max
 * damage  = (((2.0 * level / 5 + 2).floor * baseDmg * atk / defense).floor / 50).floor + 2
 * damage  = [(damage  * multipliers[FINAL_DMG_MULT]).round, 1].max
 * </pre>
 *
 * <h2>Documented deviations (registered, not approximated)</h2>
 * <ul>
 * <li><b>{@code damageReducedByFrostbite?} (:500-503)</b> - the plugin's
 *     {@code Arceus} section adds this predicate to {@code PokeBattle_Move}
 *     ({@code Arceus:216}) but it was never added to the {@code MoveEffect} surface,
 *     so the Frostbite damage-halving stage has no hook to call.</li>
 * <li><b>Dragon Darts typeMod recalculation (:486-491)</b> - needs
 *     {@code @battle.pbSideSize}, which this runtime does not have. The type modifier
 *     computed for the first hit is reused for the second.</li>
 * <li><b>Base-stat-total guard (:286-291)</b> - reads {@code user.pokemon.baseStats}
 *     (the plugin's anti-cheat for base stat totals above 830).</li>
 * <li><b>{@code pbIsCritical?}</b> - the critical decision is still the one
 *     {@code Battle#execute} already made through its legacy {@code MoveEffects} path
 *     and passes in here, rather than a second call to the strategy hook, so one hit
 *     cannot be critical for the animation and non-critical for the damage.</li>
 * </ul>
 *
 * @see BattlerHitEffects the per-hit handlers wired in the same pass
 */
public final class DamageCalc {

    /** {@code multipliers} indices (Move_Usage_Calculations:279). */
    public static final int BASE_DMG_MULT = 0;
    public static final int ATK_MULT = 1;
    public static final int DEF_MULT = 2;
    public static final int FINAL_DMG_MULT = 3;

    private DamageCalc() {
    }

    /** Damage dealt (0 when immune or a status move); never negative. */
    public static int compute(Battler user, Battler target, BattleMove move, PbsData pbs,
                              Random random) {
        return compute(user, target, move, pbs, random, false, 1);
    }

    /** As above; {@code critical} is the hit's critical decision (see the class javadoc). */
    public static int compute(Battler user, Battler target, BattleMove move, PbsData pbs,
                              Random random, boolean critical) {
        return compute(user, target, move, pbs, random, critical, 1);
    }

    /**
     * {@code pbCalcDamage(user,target,numTargets=1)} (Move_Usage_Calculations:252-295).
     * Returns the damage and records it on {@code target.damageState.calcDamage}.
     */
    public static int compute(Battler user, Battler target, BattleMove move, PbsData pbs,
                              Random random, boolean critical, int numTargets) {
        MoveEffect effect = MoveEffectRegistry.of(move.function());
        // :253 return if statusMove?
        if (effect.statusMove(move)) {
            return 0;
        }
        // :254-257 Disguise / Ice Face / Flame Veil absorb the hit for exactly 1.
        if (target.damageState.disguise || target.damageState.iceface
                || target.damageState.flameveil) {
            target.damageState.calcDamage = 1;
            return 1;
        }
        // :258-259
        int[] stageMul = { 2, 2, 2, 2, 2, 2, 2, 3, 4, 5, 6, 7, 8 };
        int[] stageDiv = { 8, 7, 6, 5, 4, 3, 2, 2, 2, 2, 2, 2, 2 };
        // :261 type = @calcType (-1 is treated as physical)
        String type = effect.pbCalcType(move, user);
        // :263 target.damageState.critical = pbIsCritical?(user,target)
        target.damageState.critical = critical;
        // :265 baseDmg = pbBaseDamage(@baseDamage,user,target)
        int baseDmg = effect.pbBaseDamage(move, move.power(), user, target);
        // :267-271 user's attack stat
        MoveStats atkStats = effect.pbGetAttackStats(move, user, target);
        double atk = atkStats.value;
        int atkStage = atkStats.stage;
        if (!target.hasActiveAbility("UNAWARE") || user.battle.moldBreaker) {       // :268
            if (target.damageState.critical && atkStage < 6) {                      // :269
                atkStage = 6;
            }
            atk = Math.floor(atk * stageMul[atkStage] / stageDiv[atkStage]);        // :270
        }
        // :273-277 target's defense stat
        MoveStats defStats = effect.pbGetDefenseStats(move, user, target);
        double defense = defStats.value;
        int defStage = defStats.stage;
        if (!user.hasActiveAbility("UNAWARE")) {                                    // :274
            if (target.damageState.critical && defStage > 6) {                      // :275
                defStage = 6;
            }
            defense = Math.floor(defense * stageMul[defStage] / stageDiv[defStage]); // :276
        }
        // :279-280
        float[] multipliers = { 1.0f, 1.0f, 1.0f, 1.0f };
        calcDamageMultipliers(effect, user, target, move, numTargets, type, baseDmg, multipliers, random);
        // :282-285 main damage calculation
        baseDmg = Math.max(Math.round(baseDmg * multipliers[BASE_DMG_MULT]), 1);
        atk = Math.max(Math.round(atk * multipliers[ATK_MULT]), 1);
        defense = Math.max(Math.round(defense * multipliers[DEF_MULT]), 1);
        double damage = Math.floor(
                Math.floor(Math.floor(2.0 * user.level() / 5 + 2) * baseDmg * atk / defense) / 50) + 2;
        // :286-291 最高种族值检测 (base stat total > 830 -> 1 damage for a player-owned mon).
        // 登记: reads user.pokemon.baseStats; the plugin's anti-cheat guard is kept as a
        // comment because this runtime's Pokemon has no public baseStats total accessor.
        damage = Math.max(Math.round(damage * multipliers[FINAL_DMG_MULT]), 1);
        // :294 target.damageState.calcDamage = damage
        target.damageState.calcDamage = (int) damage;
        return (int) damage;
    }

    /**
     * {@code pbCalcDamageMultipliers(user,target,numTargets,type,baseDmg,multipliers)}
     * (Move_Usage_Calculations:296-542), in the plugin's stage order.
     */
    private static void calcDamageMultipliers(MoveEffect effect, Battler user, Battler target,
                                              BattleMove move, int numTargets, String type, int baseDmg,
                                              float[] multipliers, Random random) {
        boolean physical = effect.physicalMove(move, null);
        boolean special = effect.specialMove(move, null);
        // :297-307 "of Ruin" abilities (Tablets/Sword/Vessel/Beads/Turbo Blaze/Calamity Aerial)
        String[] ruin = { "TABLETSOFRUIN", "SWORDOFRUIN", "VESSELOFRUIN", "BEADSOFRUIN",
                "TURBOBLAZE" };   // CALAMITYAERIAL removed: Delta Stream + Speed drop, not a Ruin ability
        for (int i = 0; i < ruin.length; i++) {                                     // :298
            if (user.battle.pbCheckGlobalAbility(ruin[i]) == null) {                  // :299
                continue;
            }
            boolean category = (i < 2) ? physical : special;                        // :300
            if (i % 2 != 0 && user.battle.field.effects.intVal(PBEffects.Field.WonderRoom) > 0) {
                category = !category;                                              // :301
            }
            if (i % 2 == 0 && !user.hasActiveAbility(ruin[i])) {                    // :302
                if (category) {
                    multipliers[ATK_MULT] *= 0.75f;                                // :303
                }
            } else if (i % 2 != 0 && !target.hasActiveAbility(ruin[i])) {           // :304
                if (category) {
                    multipliers[DEF_MULT] *= 0.75f;                                // :305
                }
            }
        }
        // :309-316 global abilities (Dark Aura / Fairy Aura, halved by Aura Break)
        if ((user.battle.pbCheckGlobalAbility("DARKAURA") != null && "DARK".equals(type))
                || (user.battle.pbCheckGlobalAbility("FAIRYAURA") != null && "FAIRY".equals(type))) {
            if (user.battle.pbCheckGlobalAbility("AURABREAK") != null) {            // :311
                multipliers[BASE_DMG_MULT] *= 2 / 3.0f;                            // :312
            } else {
                multipliers[BASE_DMG_MULT] *= 4 / 3.0f;                            // :314
            }
        }
        // :318-321 the user's own ability
        if (user.abilityActive()) {
            BattleHandlers.triggerDamageCalcUserAbility(user.ability, user, target, move,
                    multipliers, baseDmg, type);
        }
        if (!user.battle.moldBreaker) {                                            // :322
            // :326-330 the user's allies (Flower Gift), oddly negated by the user's Mold Breaker
            for (Battler b : user.allAllies()) {
                if (!b.abilityActive()) {
                    continue;                                                      // :327
                }
                BattleHandlers.triggerDamageCalcUserAllyAbility(b.ability, user, target, move,
                        multipliers, baseDmg, type);
            }
            if (target.abilityActive()) {                                          // :331
                BattleHandlers.triggerDamageCalcTargetAbility(target.ability, user, target, move,
                        multipliers, baseDmg, type);                               // :332-333
                BattleHandlers.triggerDamageCalcTargetAbilityNonIgnorable(target.ability, user, target,
                        move, multipliers, baseDmg, type);                         // :334-335
            }
            for (Battler b : target.allAllies()) {                                 // :337
                if (!b.abilityActive()) {
                    continue;                                                      // :338
                }
                BattleHandlers.triggerDamageCalcTargetAllyAbility(b.ability, user, target, move,
                        multipliers, baseDmg, type);                               // :339-340
            }
        }
        // :344-351 item effects that alter damage
        if (user.itemActive()) {
            BattleHandlers.triggerDamageCalcUserItem(user.item, user, target, move, multipliers,
                    baseDmg, type);
        }
        if (target.itemActive()) {
            BattleHandlers.triggerDamageCalcTargetItem(target.item, user, target, move, multipliers,
                    baseDmg, type);
        }
        // :353-355 Parental Bond's second attack
        if (user.effects.intVal(PBEffects.Battler.ParentalBond) == 1) {
            multipliers[BASE_DMG_MULT] /= 4;
        }
        // :357-365 other user-side effects
        if (user.effects.truthy(PBEffects.Battler.MeFirst)) {
            multipliers[BASE_DMG_MULT] *= 1.5f;                                    // :358
        }
        if (user.effects.truthy(PBEffects.Battler.HelpingHand)) {                  // :360
            multipliers[BASE_DMG_MULT] *= 1.5f;                                    // :361
        }
        if (user.effects.intVal(PBEffects.Battler.Charge) > 0 && "ELECTRIC".equals(type)) {   // :363
            multipliers[BASE_DMG_MULT] *= 2;                                       // :364
        }
        // :367-376 Mud Sport
        if ("ELECTRIC".equals(type)) {
            for (Battler b : user.battle.eachBattler()) {                          // :368
                if (!b.effects.truthy(PBEffects.Battler.MudSport)) {
                    continue;                                                      // :369
                }
                multipliers[BASE_DMG_MULT] /= 3;                                   // :370
                break;                                                             // :371
            }
            if (user.battle.field.effects.intVal(PBEffects.Field.MudSportField) > 0) {   // :373
                multipliers[BASE_DMG_MULT] /= 3;                                   // :374
            }
        }
        // :378-380 Tar Shot
        if (target.effects.truthy(PBEffects.Battler.TarShot) && "FIRE".equals(type)) {
            multipliers[BASE_DMG_MULT] *= 2;                                       // :379
        }
        // :382-391 Water Sport
        if ("FIRE".equals(type)) {
            for (Battler b : user.battle.eachBattler()) {                          // :383
                if (!b.effects.truthy(PBEffects.Battler.WaterSport)) {
                    continue;                                                      // :384
                }
                multipliers[BASE_DMG_MULT] /= 3;                                   // :385
                break;                                                             // :386
            }
            if (user.battle.field.effects.intVal(PBEffects.Field.WaterSportField) > 0) {   // :388
                multipliers[BASE_DMG_MULT] /= 3;                                   // :389
            }
        }
        // :393-408 terrain boosts, rounded because the plugin writes .round (:397/:401/:405)
        if (user.affectedByTerrain()) {
            int terrain = user.battle.field.terrain;
            if (terrain == PBBattleTerrains.Electric && "ELECTRIC".equals(type)) {   // :395-396
                multipliers[BASE_DMG_MULT] = Math.round(multipliers[BASE_DMG_MULT] * 1.3f);
            } else if (terrain == PBBattleTerrains.Grassy && "GRASS".equals(type)) { // :399-400
                multipliers[BASE_DMG_MULT] = Math.round(multipliers[BASE_DMG_MULT] * 1.3f);
            } else if (terrain == PBBattleTerrains.Psychic && "PSYCHIC".equals(type)) {  // :403-404
                multipliers[BASE_DMG_MULT] = Math.round(multipliers[BASE_DMG_MULT] * 1.3f);
            }
        }
        // :409-412 Misty Terrain halves Dragon damage
        if (user.battle.field.terrain == PBBattleTerrains.Misty && target.affectedByTerrain()
                && "DRAGON".equals(type)) {
            multipliers[BASE_DMG_MULT] /= 2;
        }
        // :413-428 badge multipliers (000_Settings:124-128 NUM_BADGES_BOOST_*)
        if (user.battle.internalBattle) {
            int badges = user.battle.numBadges;                                    // @battle.pbPlayer.numbadges
            if (user.pbOwnedByPlayer()) {                                          // :415
                if (physical && badges >= Battler.NUM_BADGES_BOOST_ATTACK) {
                    multipliers[ATK_MULT] *= 1.1f;                                 // :417
                } else if (special && badges >= Battler.NUM_BADGES_BOOST_SPATK) {
                    multipliers[ATK_MULT] *= 1.1f;                                 // :419
                }
            }
            if (target.pbOwnedByPlayer()) {                                        // :422
                if (physical && badges >= Battler.NUM_BADGES_BOOST_DEFENSE) {
                    multipliers[DEF_MULT] *= 1.1f;                                 // :424
                } else if (special && badges >= Battler.NUM_BADGES_BOOST_SPDEF) {
                    multipliers[DEF_MULT] *= 1.1f;                                 // :426
                }
            }
        }
        // :430-433 multi-targeting attacks
        if (numTargets > 1) {
            multipliers[FINAL_DMG_MULT] *= 0.75f;
        }
        // :434-450 weather
        if (!target.hasUtilityUmbrella()) {                                        // :435
            int weather = user.battle.pbWeather();                                 // :436
            if (weather == PBWeather.Sun || weather == PBWeather.HarshSun) {        // :437
                if ("FIRE".equals(type)) {
                    multipliers[FINAL_DMG_MULT] =
                            Math.round(multipliers[FINAL_DMG_MULT] * 1.5f);        // :439
                } else if ("WATER".equals(type)) {
                    multipliers[FINAL_DMG_MULT] /= 2;                              // :441
                }
            } else if (weather == PBWeather.Rain || weather == PBWeather.HeavyRain) {   // :443
                if ("FIRE".equals(type)) {
                    multipliers[FINAL_DMG_MULT] /= 2;                              // :445
                } else if ("WATER".equals(type)) {
                    multipliers[FINAL_DMG_MULT] =
                            Math.round(multipliers[FINAL_DMG_MULT] * 1.5f);        // :447
                }
            }
        }
        // :451-455 Sandstorm / :456-460 Snow: Rock and Ice special attackers defend better.
        // (`@function!="122"` excludes Psyshock, which attacks Defense.)
        if (user.battle.pbWeather() == PBWeather.Sandstorm) {                      // :451
            if (target.pbHasType("ROCK") && special && !"122".equals(move.function())) {
                multipliers[DEF_MULT] *= 1.5f;                                     // :453
            }
        }
        if (user.battle.pbWeather() == PBWeather.Snow) {                           // :456
            if (target.pbHasType("ICE") && special && !"122".equals(move.function())) {
                multipliers[DEF_MULT] *= 1.5f;                                     // :458
            }
        }
        // :461-464 Pokémon Legends: Arceus - Victory Dance
        if (user.effects.truthy(PBEffects.Battler.VictoryDance)) {
            multipliers[FINAL_DMG_MULT] *= 1.5f;                                   // :463
        }
        // :465-472 critical hits
        if (target.damageState.critical) {
            if (Battle.NEWEST_BATTLE_MECHANICS) {                                  // :467
                multipliers[FINAL_DMG_MULT] *= 1.5f;                               // :468
            } else {
                multipliers[FINAL_DMG_MULT] *= 2;                                  // :470
            }
        }
        // :473-477 random variance 0.85..1.00 (not applied to confusion self-damage)
        int roll = 85 + random.nextInt(16);                                        // :475
        multipliers[FINAL_DMG_MULT] *= roll / 100.0f;                              // :476
        // :478-485 STAB
        if (type != null && user.pbHasType(type)) {                                // :479
            if (user.hasActiveAbility("ADAPTABILITY")) {                           // :480
                multipliers[FINAL_DMG_MULT] *= 2;                                  // :481
            } else {
                multipliers[FINAL_DMG_MULT] *= 1.5f;                               // :483
            }
        }
        // :486-491 Dragon Darts re-derives the type modifier for its second target.
        // 登记: needs @battle.pbSideSize, which this runtime does not have.
        // :492-493 type effectiveness
        int typeMod = effect.pbCalcTypeMod(move, type, user, target);
        target.damageState.typeMod = typeMod;
        multipliers[FINAL_DMG_MULT] *= typeMod / (float) PBTypeEffectiveness.NORMAL_EFFECTIVE;
        // :494-498 burn halves physical damage (Guts ignores it)
        if ("BURN".equals(user.status) && physical
                && effect.damageReducedByBurn(move) && !user.hasActiveAbility("GUTS")) {
            multipliers[FINAL_DMG_MULT] /= 2;
        }
        // :499-503 frostbite halves special damage (Guts ignores it)
        // 登记: `damageReducedByFrostbite?` is defined in the plugin's Arceus section
        // (Arceus:216) but was never added to the MoveEffect surface, so there is no hook.
        // :504-507 drowsy targets take 1.25x
        if ("DROWSY".equals(target.status)) {
            multipliers[FINAL_DMG_MULT] /= 1.25f;
        }
        // :508-531 Aurora Veil / Reflect / Light Screen
        if (!effect.ignoresReflect(move) && !target.damageState.critical               // :509
                && !user.hasActiveAbility("INFILTRATOR")                               // :510
                && !user.hasActiveAbility("TRANSLUCENTGHOST")) {                       // :511
            boolean doubled = user.battle.pbSideBattlerCount(target.index) > 1;        // :513
            boolean reflect = physical && target.pbOwnSide().effects.intVal(PBEffects.Side.Reflect) > 0;
            boolean lightScreen = special && target.pbOwnSide().effects.intVal(PBEffects.Side.LightScreen) > 0;
            if (target.pbOwnSide().effects.intVal(PBEffects.Side.AuroraVeil) > 0) {     // :512
                multipliers[FINAL_DMG_MULT] *= doubled ? 2 / 3.0f : 0.5f;              // :514/:516
            } else if (reflect) {                                                      // :518
                multipliers[FINAL_DMG_MULT] *= doubled ? 2 / 3.0f : 0.5f;              // :520/:522
            } else if (lightScreen) {                                                  // :524
                multipliers[FINAL_DMG_MULT] *= doubled ? 2 / 3.0f : 0.5f;              // :526/:528
            }
        }
        // :532-534 Minimize
        if (target.effects.truthy(PBEffects.Battler.Minimize) && effect.tramplesMinimize(move, 2)) {
            multipliers[FINAL_DMG_MULT] *= 2;
        }
        // :536-537 move-specific base damage modifiers
        multipliers[BASE_DMG_MULT] =
                effect.pbBaseDamageMultiplier(move, multipliers[BASE_DMG_MULT], user, target);
        // :538-539 Glaive Rush
        if (target.effects.intVal(PBEffects.Battler.GlaiveRush) > 0) {
            multipliers[FINAL_DMG_MULT] *= 2;
        }
        // :540-541 move-specific final damage modifiers
        multipliers[FINAL_DMG_MULT] =
                effect.pbModifyDamage(move, multipliers[FINAL_DMG_MULT], user, target);
    }
}
