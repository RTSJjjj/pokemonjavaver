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
        if ("1B2".equals(move.function()) && !atk.isSpecies("SUJINRAKU")) return 0;                   // original Abyss Shadow: pbMoveFailed (species-locked), the AI must never pick it
        String moveType = AiCalc.fx(move).pbCalcType(move, atk);                                      // :126 GetMoveTypeSpecial
        boolean status = move.statusMove();                                                           // :125 moveSplit == SPLIT_STATUS
        String target = move.target();                                                                // :127 GetBaseMoveTarget
        boolean partnerTarget = def != atk && def.foe == atk.foe && AiDoublesScore.isDouble(battle, atk);   // TARGETING_PARTNER (:36)
        int atkSpeed = AiCalc.speed(atk);
        int defSpeed = AiCalc.speed(def);

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
                        if ("ELECTRIC".equals(moveType) && !partnerTarget) return clamp(dec(viability, 20));   // good idea to attack the partner
                        break;
                    case "WATERABSORB": case "DRYSKIN": case "STORMDRAIN":                          // Water
                        if ("WATER".equals(moveType) && !partnerTarget) return clamp(dec(viability, 20));
                        break;
                    case "FLASHFIRE":                                                                // Fire
                        if ("FIRE".equals(moveType) && !partnerTarget) return clamp(dec(viability, 20));
                        break;
                    case "SAPSIPPER":                                                                // Grass
                        if ("GRASS".equals(moveType) && !partnerTarget) return clamp(dec(viability, 20));
                        break;
                    case "EARTHEATER":                                                               // project/Gen 9 absorbers: same MoveImmunityTarget hook as Volt Absorb etc.
                        if ("GROUND".equals(moveType) && !partnerTarget) return clamp(dec(viability, 20));
                        break;
                    case "ICEBSORB":                                                                 // original ability
                        if ("ICE".equals(moveType) && !partnerTarget) return clamp(dec(viability, 20));
                        break;
                    case "WELLBAKEDBODY":
                        if ("FIRE".equals(moveType) && !partnerTarget) return clamp(dec(viability, 20));
                        break;
                    case "GOODASGOLD":                                                               // immune to all status moves (AROMAVEIL-style -10)
                        if (status && !partnerTarget) return clamp(dec(viability, 10));
                        break;
                    case "JUSTIFIED":                                                                // Dark (:222)
                        if ("DARK".equals(moveType) && !status && !partnerTarget && AiCalc.statCanRise(def, PBStats.ATTACK)
                                && !AiCalc.knocksOutXHits(ctx, move, atk, def, 2) && AiCalc.physicalMoveInMoveset(ctx, def)) {
                            viability = dec(viability, 4);                                           // don't risk raising enemy stats; could get worse
                        }
                        break;
                    case "RATTLED":                                                                  // Multiple move types
                        if (!status && ("DARK".equals(moveType) || "GHOST".equals(moveType) || "BUG".equals(moveType))
                                && !partnerTarget && AiCalc.statCanRise(def, PBStats.SPEED) && !AiCalc.knocksOutXHits(ctx, move, atk, def, 1)
                                && atkSpeed > defSpeed) {
                            viability = dec(viability, AiCalc.knocksOutXHits(ctx, move, atk, def, 2) ? 1 : 9);
                        }
                        break;
                    case "STEAMENGINE":
                        if (!status && ("WATER".equals(moveType) || "FIRE".equals(moveType))
                                && !partnerTarget && AiCalc.statCanRise(def, PBStats.SPEED) && !AiCalc.knocksOutXHits(ctx, move, atk, def, 1)
                                && atkSpeed > defSpeed) {
                            viability = dec(viability, AiCalc.knocksOutXHits(ctx, move, atk, def, 2) ? 5 : 9);
                        }
                        break;
                    case "SOUNDPROOF":                                                               // Move category checks (:325)
                        if (AiCalc.has(move, 'k')) return clamp(dec(viability, 10));
                        break;
                    case "BULLETPROOF":
                        if (AiCalc.has(move, 'n')) return clamp(dec(viability, 10));
                        break;
                    case "DAZZLING": case "QUEENLYMAJESTY": case "ARMORTAIL":
                        if (AiCalc.priorityCalc(battle, atk, move) > 0) return clamp(dec(viability, 10));
                        break;
                    case "AROMAVEIL":                                                                // gAromaVeilProtectedMoves: Taunt, Torment, Encore, Disable, Heal Block, Attract
                        if (status && AiCalc.named(move, "TAUNT", "TORMENT", "ENCORE", "DISABLE", "HEALBLOCK", "ATTRACT")) return clamp(dec(viability, 10));
                        break;
                    case "SWEETVEIL":
                        if (AiCalc.oneOf(move, "003", "004")) return clamp(dec(viability, 10));
                        break;
                    case "FLOWERVEIL":
                        if (def.hasType("GRASS") && status && (AiPartner.statLowering(move) || AiNegatives.setsStatus(move) || AiCalc.named(move, "PARTINGSHOT"))) {
                            return clamp(dec(viability, 10));
                        }
                        break;
                    case "MAGICBOUNCE": case "CLEARHEART":
                        if (AiCalc.has(move, 'c')) return clamp(dec(viability, 20));
                        break;
                    case "CONTRARY":
                        if (status && AiPartner.statLowering(move) && !partnerTarget) return clamp(dec(viability, 20));
                        break;
                    case "MIRRORARMOR":
                        if (status && AiPartner.statLowering(move)) return clamp(dec(viability, 20));   // bad even when attacking a partner
                        break;
                    case "CLEARBODY": case "WHITESMOKE": case "FULLMETALBODY": case "NOBLESTRIKE": case "ETERNALSTAR": case "TRANSLUCENTGHOST":   // project: same StatLossImmunity copy
                        if (status && (AiPartner.statLowering(move) || AiCalc.named(move, "PARTINGSHOT"))) return clamp(dec(viability, 10));
                        break;
                    case "HYPERCUTTER":
                        if (status && AiCalc.oneOf(move, "042", "04B")) return clamp(dec(viability, 10));         // EFFECT_ATTACK_DOWN(_2)
                        break;
                    case "KEENEYE": case "MINDSEYE":
                        if (status && AiCalc.oneOf(move, "047")) return clamp(dec(viability, 10));
                        break;
                    case "BIGPECKS":
                        if (status && AiCalc.oneOf(move, "043", "04C")) return clamp(dec(viability, 10));         // EFFECT_DEFENSE_DOWN(_2)
                        break;
                    case "DEFIANT":
                        if (status && AiPartner.statLowering(move) && !partnerTarget && AiCalc.statCanRise(def, PBStats.ATTACK)
                                && AiCalc.physicalMoveInMoveset(ctx, def)) {
                            return clamp(dec(viability, 8));                                         // not 10 because the move still works
                        }
                        break;
                    case "COMPETITIVE":
                        if (status && AiPartner.statLowering(move) && !partnerTarget && AiCalc.statCanRise(def, PBStats.SPATK)
                                && AiCalc.specialMoveInMoveset(ctx, def)) {
                            return clamp(dec(viability, 8));
                        }
                        break;
                    case "COMATOSE":
                        if (setsStatus(move)) return clamp(dec(viability, 10));                      // gSetStatusMoveEffects
                        break;
                    case "SHIELDSDOWN":
                        if (def.isSpecies("MINIOR") && def.form() == 0 && setsStatus(move)) return clamp(dec(viability, 10));
                        break;
                    case "LEAFGUARD": {
                        int w = battle.pbWeather();
                        if ((w == PBWeather.Sun || w == PBWeather.HarshSun) && !def.hasActiveItem("UTILITYUMBRELLA") && setsStatus(move)) {
                            return clamp(dec(viability, 10));
                        }
                        break;
                    }
                    default:
                        break;
                }
            }

            // Partner Ability Checks (:505-:600)
            if (AiDoublesScore.isDouble(battle, atk) && !partnerTarget) {
                Battler defPartner = AiDoublesScore.partner(battle, def);
                Battler atkPartner = AiDoublesScore.partner(battle, atk);
                boolean spreadMove = AiDoublesScore.spread(battle, atk, move);
                boolean foeField = "FoeSide".equals(target) || "BothSides".equals(target);
                boolean redirectionPrevented = atkAbility.equals("STALWART") || atkAbility.equals("PROPELLERTAIL");
                String dpa = defPartner == null || atk.hasMoldBreaker() ? "" : (defPartner.ability == null ? "" : defPartner.ability);
                switch (dpa) {                                                                       // Target Partner Ability Check
                    case "LIGHTNINGROD":
                        if ("ELECTRIC".equals(moveType) && !redirectionPrevented) return clamp(dec(viability, 20));
                        break;
                    case "STORMDRAIN":
                        if ("WATER".equals(moveType) && !redirectionPrevented) return clamp(dec(viability, 20));
                        break;
                    case "MAGICBOUNCE": case "CLEARHEART":
                        if (AiCalc.has(move, 'c') && (spreadMove || foeField)) return clamp(dec(viability, 20));
                        break;
                    case "SWEETVEIL":
                        if (AiCalc.oneOf(move, "003", "004")) return clamp(dec(viability, 10));
                        break;
                    case "FLOWERVEIL":
                        if (def.hasType("GRASS") && status && (AiPartner.statLowering(move) || setsStatus(move) || AiCalc.named(move, "PARTINGSHOT"))) {
                            return clamp(dec(viability, 10));
                        }
                        break;
                    case "AROMAVEIL":
                        if (status && AiCalc.named(move, "TAUNT", "TORMENT", "ENCORE", "DISABLE", "HEALBLOCK", "ATTRACT")) return clamp(dec(viability, 10));
                        break;
                    case "DAZZLING": case "QUEENLYMAJESTY": case "ARMORTAIL":
                        if (AiCalc.priorityCalc(battle, atk, move) > 0) return clamp(dec(viability, 10));
                        break;
                    default:
                        break;
                }
                if (!spreadMove) {                                                                   // Attacker Partner Ability Check: make sure the partner will not steal the move
                    String apa = atkPartner == null ? "" : (atkPartner.ability == null ? "" : atkPartner.ability);
                    if (!"User".equals(target) && !redirectionPrevented) {
                        if (apa.equals("LIGHTNINGROD") && "ELECTRIC".equals(moveType)) return clamp(dec(viability, 10));   // wouldn't be so bad to hit the partner
                        if (apa.equals("STORMDRAIN") && "WATER".equals(moveType)) return clamp(dec(viability, 10));
                    }
                }
            }

            // Prankster (:487)
            if (atk.hasActiveAbility("PRANKSTER") && status && !"FoeSide".equals(target)
                    && def.hasType("DARK")) {
                return clamp(dec(viability, 10));
            }

            // Terrain Check (:505)
            int terrain = battle.terrain();
            boolean grounded = !def.airborne();
            if (terrain == PBBattleTerrains.Electric) {
                if (AiCalc.oneOf(move, "003", "004") && grounded) return clamp(dec(viability, 10));   // EFFECT_SLEEP / EFFECT_YAWN
            } else if (terrain == PBBattleTerrains.Misty) {
                if (grounded && (setsStatus(move) || confuses(move))) {
                    return clamp(dec(viability, 10));
                }
            } else if (terrain == PBBattleTerrains.Psychic) {
                if (AiCalc.priorityCalc(battle, atk, move) > 0 && grounded) return clamp(dec(viability, 10));
            }

            // Powder & Ion Deluge Check (:540) - GOOD_AI
            if (ctx.goodAi() && !partnerTarget) {
                if ("FIRE".equals(moveType)) {
                    if (usedMove(def, "POWDER") && ctx.simulatedRng[0] < 75) return clamp(dec(viability, 19));
                } else if ("NORMAL".equals(moveType)) {
                    if (usedMove(def, "IONDELUGE") && ctx.simulatedRng[0] < 75
                            && (defAbility.equals("VOLTABSORB") || defAbility.equals("MOTORDRIVE") || defAbility.equals("LIGHTNINGROD"))) {
                        return clamp(dec(viability, 19));                                             // IsElectricAbsorptionAblity
                    }
                }
            }
        }

        // Status Wide Guard Check (:683)
        if (!target.equals("User") && AiDoublesScore.isDouble(battle, atk) && AiDoublesScore.spread(battle, atk, move)) {
            Battler defPartner2 = AiDoublesScore.partner(battle, def);
            if (status && ctx.goodAi() && ctx.simulatedRng[0] < 75
                    && (usedMove(def, "WIDEGUARD") || (defPartner2 != null && usedMove(defPartner2, "WIDEGUARD")))) {
                return clamp(dec(viability, 10));
            } else if (def.pbOwnSide().effects.intVal(PBEffects.Side.WideGuard) > 0) {
                return clamp(dec(viability, 10));
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
        if (AiNegativeDoubles.blocks(ctx, atk, def, move)) return clamp(dec(viability, 10));         // the partner already covers this
        AiNegativeEffects.Result effect = AiNegativeEffects.apply(ctx, atk, def, move, viability);
        viability = effect.viability;
        // AI_STANDARD_DAMAGE (:3246)
        if (effect.standardDamage && !status) {
            if (AiCalc.noEffect(battle, atk, def, move)) viability = dec(viability, 15);              // :3249 MOVE_RESULT_NO_EFFECT | MISSED
        }
        if (viability < 0) return 0;                                                                  // :3263
        return viability;
    }

    /** {@code CheckTableForMovesEffect(move,gSetStatusMoveEffects)} (move_effect_table.s:277: sleep, toxic, poison, paralyze, Will-O-Wisp, Yawn). */
    static boolean setsStatus(BattleMove move) {
        return move.statusMove() && AiCalc.oneOf(move, "003", "004", "005", "006", "007", "00A", "159");
    }

    /** {@code CheckTableForMovesEffect(move,gConfusionMoveEffects)} (confuse, Swagger, Flatter). */
    static boolean confuses(BattleMove move) {
        return move.statusMove() && AiCalc.oneOf(move, "013", "040", "041");
    }

    private static int clamp(int viability) {
        return Math.max(0, viability);                                                                // :3263 `if (viability < 0) return 0`
    }

    /** {@code HasUsedMove(bank,move)}: the battler's used-move list ({@code @movesUsed}, reset on switch-in like BATTLE_HISTORY). */
    private static boolean usedMove(Battler b, String internalName) {
        return AiCalc.hasUsedMove(b, internalName);
    }
}
