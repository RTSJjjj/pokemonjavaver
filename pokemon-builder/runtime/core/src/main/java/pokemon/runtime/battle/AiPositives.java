package pokemon.runtime.battle;

/**
 * CFRU {@code AIScript_Positives} ({@code ai_positives.c:42-2736}) and {@code DamageMoveViabilityIncrease}
 * ({@code ai_positives.c:2741-2883}), the single-battle branch.
 *
 * <p>This batch transcribes the damaging-move scoring (what makes the AI pick a knock-out, the strongest move, or
 * the fast kill) - the part that decides most turns. The per-effect {@code switch (moveEffect)} (:55-2722:
 * set-up moves, status moves, hazards, healing) is transcribed in later batches; until then a move with no case
 * there is left at its viability, as an effect with no case is in C.</p>
 *
 * <p>登记: {@code IsPredictedToSwitch}-driven doubles branches ({@code IncreaseDoublesDamageViability}, :2870) belong
 * to phase 4; {@code BetterToKOLastFoeMon} and {@code BadIdeaToMakeContactWith} (contact preferences) are not
 * transcribed (the former is treated as false, i.e. the "not the last foe mon" values).</p>
 */
final class AiPositives {

    private AiPositives() {
    }

    /** {@code AIScript_Positives(bankAtk,bankDef,originalMove,originalViability,data)}. */
    static int score(AiCtx ctx, Battler atk, Battler def, BattleMove move, int originalViability) {
        int viability = originalViability;
        BattleMove predictedMove = ctx.prediction(def);                                              // :44 IsValidMovePrediction(bankDef,bankAtk)
        int cls = AiCalc.fightingStyle(ctx, atk);                                                    // :45 GetBankFightingStyle
        String atkAbility = atk.ability == null ? "" : atk.ability;                                  // :51
        String defAbility = def.ability == null ? "" : def.ability;                                  // :52
        if (atk.hasMoldBreaker()) defAbility = "";                                                   // :54

        if (def != atk && def.foe == atk.foe && AiDoublesScore.isDouble(ctx.battle, atk)) {          // :63 IS_DOUBLE_BATTLE && TARGETING_PARTNER
            return AiPartner.score(ctx, atk, def, move, originalViability);
        }

        viability = AiPositiveEffects.apply(ctx, atk, def, move, viability, cls, atkAbility, defAbility);   // :55-2722 switch (moveEffect), part 1

        if (!move.statusMove()) {                                                                    // :2724 moveSplit != SPLIT_STATUS
            viability = damageMoveViabilityIncrease(ctx, atk, def, move, viability, cls, predictedMove, atkAbility, defAbility);
        }
        // :2728-2736 STATUS1_FREEZE unfreeze: this project has no FROSTBITE, so a frozen attacker prefers a thawing move.
        if (atk.hasStatus("FROZEN") && AiCalc.has(move, 'g')) viability += 10;                       // INCREASE_VIABILITY(10) (single battle)
        return Math.min(viability, 255);                                                             // :2738
    }

    /** {@code DamageMoveViabilityIncrease(...)} (ai_positives.c:2741), single-battle branch (:2743-2877). */
    static int damageMoveViabilityIncrease(AiCtx ctx, Battler atk, Battler def, BattleMove move, int viability, int cls,
                                           BattleMove predictedMove, String atkAbility, String defAbility) {
        Battle battle = ctx.battle;
        if (AiDoublesScore.isDouble(battle, atk)) {                                                  // :2850 Double Battle
            return AiDoublesScore.increaseDamage(ctx, viability, cls, atk, def, move);                // :2872 IncreaseDoublesDamageViability
        }
        boolean predictedSuckerPunch = predictedMove != null && AiCalc.named(predictedMove, "SUCKERPUNCH");   // EFFECT_SUCKER_PUNCH
        int atkSpeed = AiCalc.speed(atk);
        int defSpeed = AiCalc.speed(def);
        boolean priorityButSlower = AiCalc.priorityCalc(battle, atk, move) > 0 && atkSpeed < defSpeed;   // PRIORITY_MOVE_BUT_NORMALLY_SLOWER

        if (!ctx.predictedToSwitch(def)                                                              // :2747
                && AiCalc.knocksOutPossiblyGoesFirstWithBestAccuracy(ctx, move, atk, def, true)
                && (AiCalc.hitChance(battle, atk, def, move) >= 70
                || !AiCalc.moveThatCanHelpAttacksHitInMoveset(ctx, atk)
                || AiCalc.canKnockOut(ctx, def, atk))) {
            if (!predictedSuckerPunch                                                                // :2753
                    || AiCalc.classDamager(cls)
                    || (AiCalc.priorityCalc(battle, atk, move) > 0 && atkSpeed > defSpeed)) {
                if (def.effects.intVal(PBEffects.Battler.DestinyBond) == 0                           // :2757
                        || AiCalc.canKnockOut(ctx, def, atk)) {
                    viability += 9;                                                                  // :2762 INCREASE_VIABILITY(9)
                }
            }
        } else if (!ctx.predictedToSwitch(def)                                                       // :2765
                && !AiCalc.moveFunctionInMoveset(atk, AiCalc.PROTECT)
                && !AiCalc.moveWouldHitFirst(ctx, move, atk, def)
                && AiCalc.knocksOutPossiblyGoesFirstWithBestAccuracy(ctx, move, atk, def, false)
                && ((!AiCalc.willFaintFromSecondaryDamage(battle, def)
                && !AiCalc.willFaintFromContactDamage(atk, def, predictedMove))
                || AiCalc.movePredictionIsHealing(ctx, def)
                || AiCalc.isMoxie(atkAbility))) {
            viability = increaseViabilityForSlowKOMove(viability, cls, atk, def);                    // :2771
        } else if ((!usingDesperateMove(atk) || ctx.simulatedRng[3] < 75)                            // :2773
                && !AiCalc.moveFunctionInMoveset(atk, AiCalc.PROTECT)
                && !((AiCalc.takingSecondaryDamage(battle, def) || AiCalc.highChanceOfBeingImmobilized(def))
                && AiCalc.canHealFirstToPreventKnockOut(ctx, atk, def))
                && predictedMove != null && AiCalc.knocksOutXHits(ctx, predictedMove, def, atk, 1)   // :2777 foe can kill attacker
                && AiCalc.strongestMoveGoesFirst(ctx, move, atk, def)                                // :2778
                && (!(cls == AiCalc.CLASS_ENTRY_HAZARDS) || (ctx.simulatedRng[3] & 1) != 0 /* NoUsableHazardsInMoveset: 登记 */)
                && (cls != AiCalc.CLASS_PHAZING || priorityButSlower)
                && (!AiCalc.classStall(cls) || priorityButSlower)
                && !hasMoveNamedUsable(ctx, atk, "FAKEOUT", def)) {
            if (!predictedSuckerPunch || AiCalc.classDamager(cls)
                    || (AiCalc.priorityCalc(battle, atk, move) > 0 && atkSpeed > defSpeed)) {
                viability += 9;                                                                      // :2789
            } else if (AiCalc.isStrongestMove(ctx, move, atk, def)) {
                viability = strongestMoveCheck(ctx, atk, def, move, viability, cls, atkAbility);     // goto STRONGEST_MOVE_CHECK
            }
        } else if (AiCalc.isStrongestMove(ctx, move, atk, def)) {                                    // :2795
            viability = strongestMoveCheck(ctx, atk, def, move, viability, cls, atkAbility);
        }
        return viability;
    }

    /** {@code STRONGEST_MOVE_CHECK:} (ai_positives.c:2797-2838). */
    private static int strongestMoveCheck(AiCtx ctx, Battler atk, Battler def, BattleMove move, int viability, int cls,
                                          String atkAbility) {
        boolean wouldHitFirst = AiCalc.moveWouldHitFirst(ctx, move, atk, def);                       // :2801
        if (wouldHitFirst
                || !AiCalc.willFaintFromSecondaryDamage(ctx.battle, def)
                || AiCalc.movePredictionIsHealing(ctx, def)
                || AiCalc.isMoxie(atkAbility)) {
            if (def.effects.intVal(PBEffects.Battler.DestinyBond) == 0                               // :2808
                    || AiCalc.canKnockOut(ctx, def, atk)
                    || !wouldHitFirst) {
                if (viability == 100                                                                 // :2813 untouched viability
                        && AiCalc.knocksOutXHits(ctx, move, atk, def, 1)
                        && !movePredictionDrainsHp(ctx, def)
                        && !AiCalc.movePredictionIsHealing(ctx, def)) {
                    if (cls == AiCalc.CLASS_CLERIC) viability += 5;
                    else if (cls == AiCalc.CLASS_SCREENS) viability += 6;                            // IsClassSupportScreener
                    else if (cls == AiCalc.CLASS_BATON_PASS) viability += 6;
                    else if (cls == AiCalc.CLASS_PHAZING) viability += 8;
                    else if (cls == AiCalc.CLASS_STALL) viability += 8;
                    else if (cls == AiCalc.CLASS_ENTRY_HAZARDS) viability += 4;
                    else viability += 2;
                } else {
                    viability += 2;                                                                  // :2833
                }
            }
        }
        return viability;
    }

    /** {@code IncreaseViabilityForSlowKOMove(&viability,class,bankAtk,bankDef)} (ai_advanced.c:2764-2845), BetterToKOLastFoeMon taken as false. */
    static int increaseViabilityForSlowKOMove(int viability, int cls, Battler atk, Battler def) {
        switch (cls) {
            case AiCalc.CLASS_SWEEPER_KILL: viability += 8; break;
            case AiCalc.CLASS_SWEEPER_SETUP_STATS: viability += 6; break;
            case AiCalc.CLASS_SWEEPER_SETUP_STATUS: viability += 8; break;
            case AiCalc.CLASS_STALL: viability += 8; break;
            case AiCalc.CLASS_PHAZING: viability += 8; break;
            default: break;
        }
        return Math.min(viability, 255);
    }

    /** {@code gNewBS->ai.usingDesperateMove[bank]}: 登记 - not tracked, treated as "did not use a desperate move last turn". */
    private static boolean usingDesperateMove(Battler atk) {
        return false;
    }

    /** {@code MoveInMovesetAndUsable(MOVE_FAKEOUT,bank) && ShouldUseFakeOut(...)}. */
    private static boolean hasMoveNamedUsable(AiCtx ctx, Battler atk, String name, Battler def) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = atk.moveSlot(i);
            if (m != null && AiCalc.named(m, name) && AiCalc.usable(ctx, atk, i)) {
                return AiCalc.shouldUseFakeOut(ctx, atk, def);
            }
        }
        return false;
    }

    /** {@code IsMovePredictionHPDrainingMove(bankAtk,bankDef)}: the predicted move drains HP (Essentials 0DD, 0DE). */
    private static boolean movePredictionDrainsHp(AiCtx ctx, Battler atk) {
        BattleMove m = ctx.prediction(atk);
        return m != null && AiCalc.oneOf(m, "0DD", "0DE");
    }
}
