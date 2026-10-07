package pokemon.runtime.battle;

/**
 * CFRU {@code AIScript_Negatives} ({@code ai_negatives.c:111-3267}): every subtraction from a move's viability.
 *
 * <p>This batch transcribes the checks that decide whether a move can work at all against the target - the
 * Gravity / Powder checks (:174-182), the type-absorbing and move-blocking target Abilities (:194-...), the
 * Terrain checks (:547-576), Throat Chop / Heal Block (:629-635), primal weather (:651-661) - and the default
 * damaging branch {@code AI_STANDARD_DAMAGE} (:3246-3252). The per-effect {@code switch (moveEffect)}
 * (:664-3244) is transcribed case by case in later batches; an effect with no case yet falls into the default
 * branch exactly like an effect without a case does in C, so status moves are not penalised yet.</p>
 *
 * <p>登记 (CFRU-only systems that this runtime does not have): Z-Moves ({@code TryReplaceMoveWithZMove}),
 * Dynamax / Raid shields (:187-192, :638-649), Shiny Wild (:663), {@code AI_TRY_TO_KILL_RATE} (:146-169, only for
 * the basic AI - this project uses the smartest tier), Quick Draw. Doubles-only checks (:482-545 partner abilities,
 * Wide Guard) belong to phase 4.</p>
 */
final class AiNegatives {

    private AiNegatives() {
    }

    /** {@code DECREASE_VIABILITY(x)}: {@code viability = max(0, viability - x)} is applied by the caller's return. */
    private static int dec(int viability, int amount) {
        return viability - amount;
    }

    /** {@code AIScript_Negatives(bankAtk,bankDef,originalMove,originalViability,data)}. */
    static int score(AiCtx ctx, Battler atk, Battler def, BattleMove move, int originalViability) {
        Battle battle = ctx.battle;
        int viability = originalViability;
        BattleMove predictedMove = ctx.prediction(def);                                             // :113 IsValidMovePrediction(bankDef,bankAtk)
        String atkAbility = atk.ability == null ? "" : atk.ability;                                   // :117 GetAIAbility
        String defAbility = def.ability == null ? "" : def.ability;                                   // :118
        if (atk.hasMoldBreaker()) defAbility = "";                                                    // :120 IsTargetAbilityIgnored
        String moveType = AiCalc.fx(move).pbCalcType(move, atk);                                      // :126 GetMoveTypeSpecial
        boolean status = move.statusMove();                                                           // :125 moveSplit == SPLIT_STATUS
        String target = move.target();                                                                // :127 GetBaseMoveTarget

        if (!"User".equals(target)) {                                                                 // :138 MOVE_TARGET_USER skips the target checks
            // Gravity Table Prevention Check (:174)
            if (battle.field.effects.intVal(PBEffects.Field.Gravity) > 0
                    && AiCalc.named(move, "BOUNCE", "FLY", "FLYINGPRESS", "HIGHJUMPKICK", "JUMPKICK", "MAGNETRISE",
                    "SKYDROP", "SPLASH", "TELEKINESIS")) {
                return 0;                                                                             // Can't select this move period
            }
            // Powder Move Checks (:181)
            if (AiCalc.has(move, 'l') && !def.affectedByPowder(false)) viability = dec(viability, 10);

            // Target Ability Checks (:194)
            if (!atk.hasMoldBreaker()) {
                switch (defAbility) {
                    case "VOLTABSORB": case "MOTORDRIVE": case "LIGHTNINGROD":                      // Electric
                        if ("ELECTRIC".equals(moveType)) return clamp(dec(viability, 20));
                        break;
                    case "WATERABSORB": case "DRYSKIN": case "STORMDRAIN":                          // Water
                        if ("WATER".equals(moveType)) return clamp(dec(viability, 20));
                        break;
                    case "FLASHFIRE":                                                                // Fire
                        if ("FIRE".equals(moveType)) return clamp(dec(viability, 20));
                        break;
                    case "SAPSIPPER":                                                                // Grass
                        if ("GRASS".equals(moveType)) return clamp(dec(viability, 20));
                        break;
                    case "SOUNDPROOF":                                                               // Move category checks (:325)
                        if (AiCalc.has(move, 'k')) return clamp(dec(viability, 10));
                        break;
                    case "BULLETPROOF":
                        if (AiCalc.has(move, 'n')) return clamp(dec(viability, 10));
                        break;
                    case "DAZZLING": case "QUEENLYMAJESTY":
                        if (AiCalc.priorityCalc(battle, atk, move) > 0) return clamp(dec(viability, 10));
                        break;
                    case "MAGICBOUNCE":
                        if (AiCalc.has(move, 'c')) return clamp(dec(viability, 20));
                        break;
                    case "COMATOSE":
                        if (AiCalc.oneOf(move, "003", "004", "005", "006", "007", "00A", "00B", "00C", "00D", "00E", "0C5", "0C6", "0C7"))
                            return clamp(dec(viability, 10));                                        // gSetStatusMoveEffects subset: status-inflicting
                        break;
                    default:
                        break;
                }
            }
            // 登记: :222-324 JUSTIFIED/RATTLED/STEAMENGINE (AI_STAT_CAN_RISE + MoveKnocksOutXHits), AROMAVEIL, SWEETVEIL,
            //       FLOWERVEIL, CONTRARY, MIRRORARMOR, CLEARBODY/WHITESMOKE, HYPERCUTTER, KEENEYE, BIGPECKS, DEFIANT,
            //       COMPETITIVE, SHIELDSDOWN, WONDERSKIN, LEAFGUARD need gStatLoweringMoveEffects / gSetStatusMoveEffects.

            // Prankster (:487)
            if (atk.hasActiveAbility("PRANKSTER") && status && !"OpposingSide".equals(target)
                    && def.hasType("DARK")) {
                return clamp(dec(viability, 10));
            }

            // Terrain Check (:505)
            int terrain = battle.terrain();
            boolean grounded = !def.airborne();
            if (terrain == PBBattleTerrains.Electric) {
                if (AiCalc.oneOf(move, "003", "004") && grounded) return clamp(dec(viability, 10));   // EFFECT_SLEEP / EFFECT_YAWN
            } else if (terrain == PBBattleTerrains.Misty) {
                if (status && grounded && AiCalc.oneOf(move, "003", "004", "005", "006", "007", "00A", "00B", "00C", "00D", "00E",
                        "013", "014", "015", "016", "017", "018", "019")) {
                    return clamp(dec(viability, 10));
                }
            } else if (terrain == PBBattleTerrains.Psychic) {
                if (AiCalc.priorityCalc(battle, atk, move) > 0 && grounded) return clamp(dec(viability, 10));
            }

            // Powder & Ion Deluge Check (:540) - GOOD_AI
            if (ctx.goodAi() && "FIRE".equals(moveType)) {
                if (usedMove(def, "POWDER") && ctx.simulatedRng[0] < 75) return clamp(dec(viability, 19));
            }
        }

        // Throat Chop Check (:629)
        if (AiCalc.has(move, 'k') && atk.effects.intVal(PBEffects.Battler.ThroatChop) > 0) return 0;
        // Heal Block Check (:633)
        if (AiCalc.oneOf(move, "0D5", "0D6", "0D7", "0D8", "0DD", "0DE", "0DF") && atk.effects.intVal(PBEffects.Battler.HealBlock) > 0) return 0;

        // Primal Weather Check (:651)
        int weather = battle.pbWeather();
        if (weather == PBWeather.HarshSun && "WATER".equals(moveType) && !status) return clamp(dec(viability, 20));
        if (weather == PBWeather.HeavyRain && "FIRE".equals(moveType) && !status) return clamp(dec(viability, 20));

        // Check Move Effects (:667); the effects without a transcribed case fall into the default branch.
        AiNegativeEffects.Result effect = AiNegativeEffects.apply(ctx, atk, def, move, viability);
        viability = effect.viability;
        // AI_STANDARD_DAMAGE (:3246)
        if (effect.standardDamage && !status) {
            if (AiCalc.noEffect(battle, atk, def, move)) viability = dec(viability, 15);              // :3249 MOVE_RESULT_NO_EFFECT | MISSED
        }
        if (viability < 0) return 0;                                                                  // :3263
        return viability;
    }

    private static int clamp(int viability) {
        return Math.max(0, viability);                                                                // :3263 `if (viability < 0) return 0`
    }

    /** {@code HasUsedMove(bank,move)}: 登记 - this runtime keeps no per-battler used-move history for the AI, so only the last move is known. */
    private static boolean usedMove(Battler b, String internalName) {
        return false;
    }
}
