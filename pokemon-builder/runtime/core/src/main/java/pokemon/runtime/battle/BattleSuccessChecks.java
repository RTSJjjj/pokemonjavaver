package pokemon.runtime.battle;

import pokemon.runtime.battle.movefx.MoveEffect;
import pokemon.runtime.battle.movefx.MoveEffectRegistry;

/**
 * Stage 4 / &sect;4 B1+B2+B3 (wiring): {@code pbSuccessCheckAgainstTarget}
 * (Battler_UseMove_SuccessChecks:389-587), the phase that decides whether a move
 * actually lands on a given target.
 *
 * <h2>Why this class exists</h2>
 * The plugin runs these checks before a move resolves (Battler_UseMove_SuccessChecks
 * {@code :389-587}). Only the type-immunity test had been ported - inline in
 * {@code Battle#execute} - so the rest of the phase was dead: powder moves hit
 * Grass-types, Levitate did not block Ground moves, Protect did nothing, Magic Bounce
 * did not reflect, and no move effect's {@code pbFailsAgainstTarget?} ever ran.
 *
 * <h2>Order (the plugin's)</h2>
 * <ol>
 * <li>:389-390 type modifier recorded on {@code damageState.typeMod}</li>
 * <li>:392 a charging turn of a two-turn move cannot fail here</li>
 * <li>:394 {@code pbFailsAgainstTarget?} - the move's own failure condition</li>
 * <li>:396-401 Psychic Terrain blocks priority moves <b>(登记: needs the priority slot
 *     {@code @battle.choices[i][4]}, which this runtime does not model yet)</b></li>
 * <li>:403-421 Crafty Shield / Wide Guard</li>
 * <li>:422-510 the protect family: Quick Guard, Protect, Obstruct, King's Shield,
 *     Spiky Shield, Burning Bulwark, Baneful Bunker, Mat Block</li>
 * <li>:511-526 Magic Coat / Magic Bounce</li>
 * <li>:527-528 {@code pbImmunityByAbility}</li>
 * <li>:529-533 type immunity <b>(already in {@code Battle#execute}; not repeated)</b></li>
 * <li>:535-540 Dark-type immunity to Prankster-boosted moves</li>
 * <li>:542-566 airborne immunity to Ground moves (Levitate, Air Balloon, Magnet Rise,
 *     Telekinesis)</li>
 * <li>:567-587 immunity to powder moves</li>
 * </ol>
 *
 * <h2>Registered gaps (explicit, not approximated)</h2>
 * <ul>
 * <li><b>{@code @battle.successStates[user.index].protected = true}</b> - this runtime
 *     has no per-battler success-state table (it feeds the AI and scene decisions, not
 *     the damage). Every protect-family branch still sets
 *     {@link DamageState#protectedFlag}, which is what the move flow reads.</li>
 * <li><b>Psychic Terrain priority block (:396-401) and Quick Guard (:424-431)</b> -
 *     both gate on {@code @battle.choices[user.index][4]}, the priority value
 *     {@code pbCalculatePriority} saved. Neither the fifth choice slot nor
 *     {@code pbCalculatePriority} exists in this runtime (docs/stage4-wiring-queue.md
 *     C6), so the two conditions cannot be evaluated. They are written as comments at
 *     their lines.</li>
 * <li><b>{@code :524-526}</b> - the reference dump repeats the Magic Coat/Magic Bounce
 *     block as one run-together line; that is an extraction artefact, not plugin
 *     behaviour, so it is ignored (the block is transcribed once, at :511-526).</li>
 * </ul>
 */
public final class BattleSuccessChecks {

    private BattleSuccessChecks() {
    }

    /** {@code unseenfist} (Battler_UseMove_SuccessChecks:387-388). */
    private static boolean unseenFist(BattleMove move, Battler user) {
        return ("UNSEENFIST".equals(user.ability) || "PIERCINGDRILL".equals(user.ability))
                && MoveEffectRegistry.of(move.function()).contactMove(move);       // :387-388 && move.contactMove?
    }

    /** The plugin's {@code hasMoldBreaker?}: the ability, or the field flag. */
    private static boolean hasMoldBreaker(Battle battle, Battler battler) {
        return battler.hasActiveAbility("MOLDBREAKER") || battle.moldBreaker;
    }

    /**
     * The whole target-side success check (Battler_UseMove_SuccessChecks:389-587).
     * Returns {@code true} when the move may proceed against {@code target};
     * {@code false} when it is blocked (the plugin's message has already been shown).
     */
    public static boolean pbSuccessCheckAgainstTarget(Battle battle, BattleMove move,
                                                      Battler user, Battler target) {
        MoveEffect effect = MoveEffectRegistry.of(move.function());
        // :384 @battle.moldBreaker = user.hasMoldBreaker? || (move.statusMove? && user.hasActiveAbility?(:MYCELIUMMIGHT)) if !@battle.moldBreaker
        if (!battle.moldBreaker) {
            battle.moldBreaker = user.hasMoldBreaker()
                    || (effect.statusMove(move) && user.hasActiveAbility("MYCELIUMMIGHT"));
        }
        if (target.hasActiveItem("ABILITYSHIELD")) battle.moldBreaker = false;             // :385
        // :386-388 Unseen Fist
        boolean unseenfist = unseenFist(move, user);
        // :389-390 typeMod = move.pbCalcTypeMod(move.calcType,user,target)
        int typeMod = effect.pbCalcTypeMod(move, move.calcType(), user, target);
        target.damageState.typeMod = typeMod;
        // :392 two-turn attacks can't fail here in the charging turn
        if (user.effects.intVal(PBEffects.Battler.TwoTurnAttack) > 0) {
            return true;
        }
        // :394 move-specific failures
        if (effect.pbFailsAgainstTarget(move, user, target)) {
            return false;
        }
        // :396-401 Immunity to priority moves because of Psychic Terrain
        // Move priority saved from pbCalculatePriority: @battle.choices[user.index][4]
        final int savedPriority = (Integer) battle.choices(user.index)[4];
        if (battle.field.terrain == PBBattleTerrains.Psychic && target.affectedByTerrain()     // :396
                && target.opposes(user)                                                         // :397
                && savedPriority > 0) {                                                         // :398
            battle.display(target.pbThis() + "周围变得十分奇妙！");                              // :399
            return false;                                                                       // :400
        }
        // :403-410 Crafty Shield
        // 登记 (plugin defect): :404 ends in `!move.function == "18E"`, which parses as
        //   `(!move.function) == "18E"` i.e. `false == "18E"` - always false - so this
        //   Crafty Shield block can never run in the plugin. Transcribed as written.
        final boolean craftyShieldTail = false;                                                // :404 !move.function == "18E"
        if (target.pbOwnSide().effects.truthy(PBEffects.Side.CraftyShield)                    // :403
                && user.index != target.index
                && effect.statusMove(move)
                && effect.pbTarget(move, user) != PBTargets.AllBattlers                        // :404
                && !unseenfist && craftyShieldTail) {
            battle.commonAnimation("CraftyShield", target);                                    // :405
            battle.display("戏法防守保护了" + target.pbThis(true) + "！");                     // :406
            target.damageState.protectedFlag = true;                                           // :407
            // :408 @battle.successStates[user.index].protected = true - 登记 (see class javadoc)
            return false;                                                                      // :409
        }
        // :411-421 Wide Guard (move.function 17C is Dragon Darts and is excluded)
        if (target.pbOwnSide().effects.truthy(PBEffects.Side.WideGuard)                        // :412
                && user.index != target.index
                && PBTargets.multipleTargets(effect.pbTarget(move, user))                      // :413
                && !"17C".equals(move.function())
                && (Battle.NEWEST_BATTLE_MECHANICS || effect.damagingMove(move))               // :414
                && !unseenfist) {
            battle.commonAnimation("WideGuard", target);                                       // :416
            battle.display("广域防守保护了" + target.pbThis(true) + "！");                     // :417
            target.damageState.protectedFlag = true;                                           // :418
            return false;                                                                      // :420
        }
        // :422-510 the protect family
        if (effect.canProtectAgainst(move)) {
            // :424-431 Quick Guard
            if (target.pbOwnSide().effects.truthy(PBEffects.Side.QuickGuard)                    // :424
                    && savedPriority > 0 && !unseenfist) {                                       // :425
                battle.commonAnimation("QuickGuard", target);                                    // :426
                battle.display("快速防守保护了" + target.pbThis(true) + "！");                    // :427
                target.damageState.protectedFlag = true;                                         // :428
                battle.successStates[user.index].protectedFlag = true;                           // :429
                return false;                                                                    // :430
            }
            // :432-439 Protect
            if (target.effects.truthy(PBEffects.Battler.Protect) && !unseenfist) {              // :433
                battle.commonAnimation("Protect", target);                                      // :434
                battle.display(target.pbThis() + "保护了自己！");                              // :435
                target.damageState.protectedFlag = true;                                        // :436
                return false;                                                                    // :438
            }
            // :440-451 Obstruct (lowers the attacker's Defense on contact)
            if (target.effects.truthy(PBEffects.Battler.Obstruct) && !unseenfist) {             // :441
                battle.commonAnimation("Obstruct", target);                                     // :442
                battle.display(target.pbThis() + "保护了自己！");                              // :443
                target.damageState.protectedFlag = true;                                        // :444
                if (effect.pbContactMove(move, user) && user.affectedByContactEffect(false)) {  // :445
                    if (user.pbCanLowerStatStage(PBStats.DEFENSE)) {                            // :446
                        user.pbLowerStatStage(PBStats.DEFENSE, 2, null);                        // :447
                    }
                }
                return false;                                                                    // :450
            }
            // :452-464 King's Shield (lowers the attacker's Attack on contact)
            if (target.effects.truthy(PBEffects.Battler.KingsShield)                            // :453
                    && effect.damagingMove(move) && !unseenfist) {
                battle.commonAnimation("KingsShield", target);                                  // :454
                battle.display(target.pbThis() + "保护了自己！");                              // :455
                target.damageState.protectedFlag = true;                                        // :456
                if (effect.pbContactMove(move, user) && user.affectedByContactEffect(false)) {  // :458
                    if (user.pbCanLowerStatStage(PBStats.ATTACK)) {                             // :459
                        user.pbLowerStatStage(PBStats.ATTACK, 1, null);                         // :460
                    }
                }
                return false;                                                                    // :463
            }
            // :465-478 Spiky Shield (damages the attacker on contact)
            if (target.effects.truthy(PBEffects.Battler.SpikyShield) && !unseenfist) {          // :466
                battle.commonAnimation("SpikyShield", target);                                  // :467
                battle.display(target.pbThis() + "保护了自己！");                              // :468
                target.damageState.protectedFlag = true;                                        // :469
                if (effect.pbContactMove(move, user) && user.affectedByContactEffect(false)) {  // :471
                    // :472 @battle.scene.pbDamageAnimation(user) - 登记: scene not modelled
                    user.pbReduceHP(user.maxHp() / 8, false, true, true);                       // :473
                    battle.display(user.pbThis() + "受到了伤害！");                             // :474
                    user.pbItemHPHealCheck(0, false);                                            // :475
                }
                return false;                                                                    // :477
            }
            // :479-490 Burning Bulwark (burns the attacker on contact)
            if (target.effects.truthy(PBEffects.Battler.BurningBulwark)                         // :480
                    && effect.damagingMove(move) && !unseenfist) {
                battle.commonAnimation("BurningBulwark", target);                               // :481
                battle.display(target.pbThis() + "保护了自己！");                              // :482
                target.damageState.protectedFlag = true;                                        // :483
                if (effect.pbContactMove(move, user) && user.affectedByContactEffect(false)     // :485
                        && user.pbCanBurn(target, false)) {                                     // :486
                    user.pbBurn(target, null);                                                   // :487
                }
                return false;                                                                    // :489
            }
            // :491-501 Baneful Bunker (poisons the attacker on contact)
            if (target.effects.truthy(PBEffects.Battler.BanefulBunker) && !unseenfist) {        // :492
                battle.commonAnimation("BanefulBunker", target);                                // :493
                battle.display(target.pbThis() + "保护了自己！");                              // :494
                target.damageState.protectedFlag = true;                                        // :495
                if (effect.pbContactMove(move, user) && user.affectedByContactEffect(false)     // :497
                        && user.pbCanPoison(target, false)) {                                    // :498
                    user.pbPoison(target, null, false);                                          // :498
                }
                return false;                                                                    // :500
            }
            // :502-509 Mat Block (no common animation for this one - the plugin says so)
            if (target.pbOwnSide().effects.truthy(PBEffects.Side.MatBlock)                      // :503
                    && effect.damagingMove(move) && !unseenfist) {
                battle.display(move.name() + "被掀起的榻榻米挡住了！");                        // :505
                target.damageState.protectedFlag = true;                                        // :506
                return false;                                                                    // :508
            }
        }
        // :511-526 Magic Coat / Magic Bounce
        if (effect.canMagicCoat(move) && !target.semiInvulnerable() && target.opposes(user)) {  // :512
            if (target.effects.truthy(PBEffects.Battler.MagicCoat)) {                           // :513
                target.damageState.magicCoat = true;                                            // :514
                target.effects.set(PBEffects.Battler.MagicCoat, false);                         // :515
                return false;                                                                    // :516
            }
            // :518-522 Magic Bounce (and the project's extra abilities)
            if ((target.hasActiveAbility("MAGICBOUNCE") || target.hasActiveAbility("MOERAE")
                    || target.hasActiveAbility("RADIANTRULE") || target.hasActiveAbility("CLEARHEART"))
                    && !battle.moldBreaker
                    && !target.effects.truthy(PBEffects.Battler.MagicBounce)) {
                target.damageState.magicBounce = true;                                          // :520
                target.effects.set(PBEffects.Battler.MagicBounce, true);                        // :521
                return false;                                                                    // :522
            }
        }
        // :527-528 Immunity because of ability (intentionally before the type immunity check)
        if (effect.pbImmunityByAbility(move, user, target)) {
            return false;
        }
        // :529-533 Type immunity
        if (effect.pbDamagingMove(move) && PBTypes.ineffective(typeMod)) {                      // :529
            battle.display("这不能影响" + target.pbThis(true) + "……");                         // :531
            return false;                                                                        // :532
        }
        // :535-540 Dark-type immunity to moves made faster by Prankster
        if (Battle.NEWEST_BATTLE_MECHANICS && user.effects.truthy(PBEffects.Battler.Prankster)  // :535
                && target.pbHasType("DARK") && target.opposes(user)) {                          // :536
            battle.display("这不能影响" + target.pbThis(true) + "……");                         // :538
            return false;                                                                        // :539
        }
        // :542-566 airborne immunity to Ground moves
        if (effect.damagingMove(move) && "GROUND".equals(move.calcType())                       // :542-543
                && target.airborne() && !effect.hitsFlyingTargets(move)) {                      // :543
            // :544-553 Levitate
            if (target.hasActiveAbility("LEVITATE") && !battle.moldBreaker) {                   // :544
                battle.showAbilitySplash(target);                                               // :545
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                             // :546
                    battle.display(target.pbThis() + "避开了攻击!");                            // :547
                } else {
                    battle.display(target.pbThis() + "用" + target.abilityName()               // :549
                            + "避开了攻击!");
                }
                battle.hideAbilitySplash(target);                                               // :551
                return false;                                                                    // :552
            }
            // :554-557 Air Balloon
            if (target.hasActiveItem("AIRBALLOON")) {
                battle.display(target.pbThis() + "的" + target.itemName()                      // :555
                        + "使地面属性招式无效！");
                return false;                                                                    // :556
            }
            // :558-561 Magnet Rise
            if (target.effects.intVal(PBEffects.Battler.MagnetRise) > 0) {
                battle.display(target.pbThis() + "处于悬浮状态！\n地面属性的招式无效！");      // :559
                return false;                                                                    // :560
            }
            // :562-565 Telekinesis
            if (target.effects.intVal(PBEffects.Battler.Telekinesis) > 0) {
                battle.display(target.pbThis() + "处于飘浮状态！\n地面属性的招式无效！");      // :563
                return false;                                                                    // :564
            }
        }
        // :567-587 Immunity to powder-based moves
        if (Battle.NEWEST_BATTLE_MECHANICS && effect.powderMove(move)) {                         // :568
            if (target.pbHasType("GRASS")) {                                                     // :569
                battle.display("这不能影响" + target.pbThis(true) + "……");                      // :571
                return false;                                                                    // :572
            }
            if (target.hasActiveAbility("OVERCOAT") && !battle.moldBreaker) {                    // :574
                battle.showAbilitySplash(target);                                                // :575
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                              // :576
                    battle.display("这不能影响" + target.pbThis(true) + "……");                  // :577
                } else {
                    battle.display("因为" + target.abilityName()                                 // :579
                            + "\n这并没有影响到" + target.pbThis(true) + "！");
                }
                battle.hideAbilitySplash(target);                                                // :581
                return false;                                                                    // :582
            }
            if (target.hasActiveItem("SAFETYGOGGLES")) {                                         // :584
                battle.display("这不能影响" + target.pbThis(true) + "……");                      // :586
                return false;
            }
        }
        // :590-596 Substitute
        if (target.effects.intVal(PBEffects.Battler.Substitute) > 0 && effect.statusMove(move)
                && !effect.ignoresSubstitute(move, user) && user.index != target.index) {        // :591-592
            battle.display(target.pbThis(true) + "避开了攻击!");                                  // :594
            return false;                                                                        // :595
        }
        return true;                                                                             // :597
    }
}
