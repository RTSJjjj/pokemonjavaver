package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;

/**
 * Battle_Action_Switching:338-476 ({@code pbOnActiveAll}, {@code pbPriorityNeutralizingGas},
 * {@code pbActivateHealingWish}, {@code pbOnActiveOne}) and
 * {@code Mega evolution:463-502} ({@code pbPrimalReversion}, the version the later
 * section leaves in place), transcribed line by line.
 *
 * <p>登记:
 * <ul>
 * <li>:388-392 Shadow Pokemon introduction: {@link Battler#shadowPokemon()} is false.</li>
 * <li>:399 {@code pbUpdateParticipants}: this runtime has no participants list; experience is
 *     awarded through {@code Battle.pbGainExp}'s bridge.</li>
 * <li>Mega evolution:468-482 {@code za_full_mega_animation?} scenes and {@code battler.pbUpdate(true)}:
 *     scene/graphics only; stats are read from the Pokemon live, so the new form is already in effect.</li>
 * </ul></p>
 */
public final class BattleSwitching {

    private BattleSwitching() {
    }

    /** {@code pbOnActiveAll} (:338-347): abilities upon entering battle, at the start of the battle only. */
    public static void pbOnActiveAll(Battle battle) {
        // Neutralizing Gas activates before anything.
        pbPriorityNeutralizingGas(battle);                                            // :340
        // Weather-inducing abilities, Trace, Imposter, etc.
        battle.pbCalculatePriority(true, null);                                       // :342
        for (Battler b : battle.pbPriority(true)) {                                   // :343
            b.pbEffectsOnSwitchIn(true);
        }
        battle.pbCalculatePriority();                                                 // :344
        // Check forms are correct
        for (Battler b : battle.eachBattler()) {                                      // :346
            b.pbCheckForm(false);
        }
    }

    /** {@code pbPriorityNeutralizingGas} (:350-361). */
    public static void pbPriorityNeutralizingGas(Battle battle) {
        for (Battler b : battle.eachBattler()) {                                      // :351
            if (b == null || b.fainted()) continue;                                   // :352
            if (b.effects.truthy(PBEffects.Battler.GastroAcid)) continue;             // :353
            if ("NEUTRALIZINGGAS".equals(b.ability)) {                                // :355
                BattleHandlers.triggerAbilityOnSwitchIn("NEUTRALIZINGGAS", b, battle);   // :356
            } else if ("STEELDYNASTY".equals(b.ability)) {                            // :357
                BattleHandlers.triggerAbilityOnSwitchIn("STEELDYNASTY", b, battle);   // :358
            }
        }
    }

    /** {@code pbActivateHealingWish(battler)} (:363-382). */
    public static void pbActivateHealingWish(Battle battle, Battler battler) {
        if (!battler.canTakeHealingWish()) return;                                    // :364
        BattlePosition pos = battle.field.positions[battler.index];
        // Healing Wish
        if (pos.effects.truthy(PBEffects.Position.HealingWish)) {                     // :366
            battle.commonAnimation("HealingWish", battler);                           // :367
            battle.display("治愈之愿降临在了" + battler.pbThis(true) + "！");             // :368
            battler.pbRecoverHP(battler.maxHp());                                     // :369
            battler.pbCureStatus(false);                                              // :370
            pos.effects.set(PBEffects.Position.HealingWish, false);                   // :371
        }
        // Lunar Dance
        if (pos.effects.truthy(PBEffects.Position.LunarDance)) {                      // :374
            battle.commonAnimation("LunarDance", battler);                            // :375
            battle.display(battler.pbThis() + "被神秘的月光笼罩！");                       // :376
            battler.pbRecoverHP(battler.maxHp());                                     // :377
            battler.pbCureStatus(false);                                              // :378
            for (int i = 0; i < battler.pokemon.moves.size; i++) {                    // :379 eachMove { |m| m.pp = m.totalpp }
                if (battler.moveSlot(i) == null) continue;
                battler.pokemon.moves.get(i).pp = battler.moveSlotMaxPp(i);
            }
            pos.effects.set(PBEffects.Position.LunarDance, false);                    // :380
        }
    }

    /** {@code pbOnActiveOne(battler)} (:386-476): entry effects and entry hazards. */
    public static boolean pbOnActiveOne(Battle battle, Battler battler) {
        if (battler.fainted()) return false;                                          // :387
        // :388-392 Introduce Shadow Pokemon: see class javadoc
        // Record money-doubling effect of Amulet Coin/Luck Incense
        if ((battler.index & 1) == 0                                                  // :394 !battler.opposes?
                && ("AMULETCOIN".equals(battler.item) || "LUCKINCENSE".equals(battler.item))) {
            battle.field.effects.set(PBEffects.Field.AmuletCoin, true);               // :396
        }
        // :399 eachBattler { |b| b.pbUpdateParticipants }: see class javadoc
        // Healing Wish / Lunar Dance
        pbActivateHealingWish(battle, battler);                                       // :401
        // Entry hazards
        BattleSide side = battler.pbOwnSide();
        // Stealth Rock
        if (side.effects.truthy(PBEffects.Side.StealthRock) && battler.takesIndirectDamage(false)
                && !battler.hasActiveItem("HEAVYDUTYBOOTS")) {                        // :404-405
            String aType = "ROCK";                                                    // :406
            Array<String> bTypes = battler.pbTypes(true);                             // :407
            int eff = PBTypes.getCombinedEffectiveness(battle.pbs(), aType,
                    bTypes.size > 0 ? bTypes.get(0) : null,
                    bTypes.size > 1 ? bTypes.get(1) : null,
                    bTypes.size > 2 ? bTypes.get(2) : null);                           // :408
            if (!PBTypes.ineffective(eff)) {                                          // :409
                float effF = eff / (float) PBTypeEffectiveness.NORMAL_EFFECTIVE;     // :410
                int oldHP = battler.hp;                                               // :411
                battler.pbReduceHP(Math.round(battler.maxHp() * effF / 8), false, true, true);   // :412
                battle.display("尖锐的岩石伤害了" + battler.pbThis() + "！");             // :413
                battler.pbItemHPHealCheck(0, false);                                  // :414
                if (battler.pbAbilitiesOnDamageTaken(oldHP, -1)) {                    // :415 Switched out
                    return pbOnActiveOne(battle, battler);                            // :416 For replacement battler
                }
            }
        }
        // Spikes
        if (side.effects.intVal(PBEffects.Side.Spikes) > 0 && battler.takesIndirectDamage(false)
                && !battler.airborne() && !battler.hasActiveItem("HEAVYDUTYBOOTS")) { // :422-423
            int spikesDiv = new int[]{8, 6, 4}[side.effects.intVal(PBEffects.Side.Spikes) - 1];   // :424
            int oldHP = battler.hp;                                                   // :425
            battler.pbReduceHP(battler.maxHp() / spikesDiv, false, true, true);       // :426
            battle.display(battler.pbThis() + "被地菱伤害了！");                          // :427
            battler.pbItemHPHealCheck(0, false);                                      // :428
            if (battler.pbAbilitiesOnDamageTaken(oldHP, -1)) {                        // :429 Switched out
                return pbOnActiveOne(battle, battler);                                // :430 For replacement battler
            }
        }
        // Toxic Spikes
        if (side.effects.intVal(PBEffects.Side.ToxicSpikes) > 0 && !battler.fainted()
                && !battler.airborne()) {                                             // :434-435
            if (battler.pbHasType("POISON")) {                                        // :436
                side.effects.set(PBEffects.Side.ToxicSpikes, 0);                      // :437
                battle.display(battler.pbThis() + "吸收了毒菱！");                        // :438
            } else if (battler.pbCanPoison(null, false) && !battler.hasActiveItem("HEAVYDUTYBOOTS")) {   // :439
                if (side.effects.intVal(PBEffects.Side.ToxicSpikes) == 2) {           // :440
                    battler.pbPoison(null, battler.pbThis() + "因毒菱中了剧毒！", true);   // :441
                } else {
                    battler.pbPoison(null, battler.pbThis() + "因毒菱中了毒！", false);    // :443
                }
            }
        }
        // Sticky Web
        if (side.effects.truthy(PBEffects.Side.StickyWeb) && !battler.fainted()
                && !battler.airborne() && !battler.hasActiveItem("HEAVYDUTYBOOTS")) { // :448-449
            battle.display(battler.pbThis() + "被粘网困住了！");                          // :450
            if (battler.pbCanLowerStatStage(PBStats.SPEED)) {                         // :451
                int webUser = side.effects.intVal(PBEffects.Side.StickyWebUser);
                Battler stickyuser = webUser > -1 ? battle.battlerAt(webUser) : null; // :452-453
                battler.pbLowerStatStage(PBStats.SPEED, 1, stickyuser);               // :454
                battler.pbItemStatRestoreCheck(0, false);                             // :455
            }
        }
        // ====== 妄之歌：替换上场时检查 ======
        if (side.effects.intVal(PBEffects.Side.DelusionSong) > 0) {                   // :459
            if (battler.effects.intVal(PBEffects.Battler.Substitute) == 0
                    && battler.effects.intVal(PBEffects.Battler.Confusion) == 0
                    && battler.pbCanConfuse(battler, false, null)) {                  // :460-462
                battler.pbConfuse(battler.pbThis() + "被妖精的歌声迷惑了！");              // :463
            }
        }
        // Battler faints if it is knocked out because of an entry hazard above
        if (battler.fainted()) {                                                      // :468
            battler.pbFaint();                                                        // :469
            battle.pbGainExp();                                                       // :470
            battle.pbJudge();                                                         // :471
            return false;                                                             // :472
        }
        battler.pbCheckForm(false);                                                   // :474
        return true;                                                                  // :475
    }

    /** {@code pbPrimalReversion(idxBattler)} (Mega evolution:463-502). */
    public static void pbPrimalReversion(Battle battle, int idxBattler) {
        Battler battler = battle.battlerAt(idxBattler);                               // :464
        if (battler == null || battler.pokemon == null) return;                       // :465
        if (!battler.hasPrimal() || battler.isPrimal()) return;                       // :466
        if (battler.isSpecies("KYOGRE")) {                                            // :470
            battle.commonAnimation("PrimalKyogre", battler);                          // :471
            battler.pokemon.makePrimal(battle.pbs());                                 // :476
            battle.commonAnimation("PrimalKyogre2", battler);                         // :484
        } else if (battler.isSpecies("GROUDON")) {                                    // :485
            battle.commonAnimation("PrimalGroudon", battler);                         // :486
            battler.pokemon.makePrimal(battle.pbs());                                 // :491
            battle.commonAnimation("PrimalGroudon2", battler);                        // :499
        }
        battle.display(battler.pbThis() + "原始回归了！\n" + battler.pbThis() + "回到了原始形态！");   // :501
    }
}
