package pokemon.runtime.battle;

import static pokemon.runtime.battle.AiPositiveHelpers.inc;
import static pokemon.runtime.battle.AiPositiveHelpers.incStat;
import static pokemon.runtime.battle.AiPositiveHelpers.incStatus;

import pokemon.runtime.pokemon.Pokemon;

/**
 * The per-effect {@code switch (moveEffect)} of {@code AIScript_Positives} (ai_positives.c:55-2722), part 1 (:57-1010): Sleep/Yawn,
 * drain moves, the stat-raising and stat-lowering moves, Haze, Roar, multi-hit, recovery, poison, Rest, Mist, Focus Energy, Confuse,
 * Paralyze, Leech Seed, Pain Split, Snore/Sleep Talk, Laser Focus. CFRU's {@code EFFECT_*} ids map to the movefx function codes.
 *
 * <p>登记 (not in this part): the secondary-effect "...HIT" cases ({@code CalcSecondaryEffectChance}), screens
 * ({@code ShouldSetUpScreens}), Substitute ({@code IncreaseSubstituteViability}), Mimic/Disable/Encore (last-used move),
 * Trap ({@code ShouldTrap}), speed control ({@code IncreaseViabilityForSpeedControl}), Explosion, Mirror Move/Copycat, and everything from
 * ai_positives.c:1010 on (Protect, hazards, weather, Baton Pass, Taunt, Trick, ...).</p>
 */
final class AiPositiveEffects {

    private AiPositiveEffects() {
    }

    private static final int ATK = PBStats.ATTACK, DEF = PBStats.DEFENSE, SPD = PBStats.SPEED, SPA = PBStats.SPATK, SPD_EF = PBStats.SPDEF,
            ACC = PBStats.ACCURACY, EVA = PBStats.EVASION;

    /** @return the viability after this move's effect case. */
    static int apply(AiCtx ctx, Battler atk, Battler def, BattleMove move, int viability, int cls, String atkAbility, String defAbility) {
        Battle battle = ctx.battle;
        String f = move.function();
        boolean contrary = "CONTRARY".equals(atkAbility);
        int atkSpeed = AiCalc.speed(atk);
        int defSpeed = AiCalc.speed(def);
        BattleMove predicted = ctx.prediction(def);
        if (AiCalc.isSideEffectHit(move)) {                                                    // EFFECT_*_HIT (:93-113, :839-885): only worth it for a likely side effect
            int chance = AiCalc.secondaryEffectChance(move, atk);
            boolean blocked = AiCalc.blockedBySubstitute(move, atk, def);
            if (AiCalc.oneOf(move, "0A7", "0EF")) {
                return viability;                                                                  // plain damage
            } else if (AiCalc.oneOf(move, "003", "005", "006", "007", "00A", "00C", "01C", "01D", "01F", "020", "022", "179")) {
                boolean selfRaise = AiCalc.oneOf(move, "01C", "01D", "01F", "020", "022", "179");
                if (chance < 75 || (blocked && !selfRaise)) return viability;
            } else if (f.equals("013")) {
                if (chance < 75 || blocked || !AiCalc.moveWillHit(battle, atk, def, move)) return viability;
            } else if (f.equals("044")) {
                if (chance < 50 || blocked) return viability;
            } else if (AiCalc.classDamager(cls) || chance < 50 || blocked) {                     // STAT_DOWN_HIT_CHECK
                return viability;
            }
        }
        switch (f) {
            case "003": case "004":                                                                // EFFECT_SLEEP / YAWN (:68)
                return AiPositiveHelpers.incSleep(ctx, viability, cls, atk, def, move);

            case "0DD": case "0DE": case "05A": {                                                  // EFFECT_ABSORB / DREAM_EATER (:75), PAIN_SPLIT (:931)
                if (f.equals("05A")) {
                    int newHealth = (atk.hp + def.hp) / 2;
                    if (newHealth <= atk.hp * 12 / 10) return viability;
                }
                if (AiPositiveHelpers.shouldRecover(ctx, atk, def, move)) {                         // AI_DRAIN_HP_CHECK
                    if (AiCalc.classStall(cls)) viability = incStatus(ctx, viability, cls, 2, atk, def);
                    else viability = inc(viability, 3);                                             // IS_SINGLE_BATTLE: past strongest move
                }
                return viability;
            }

            // ---- raised stats (:215-) ----
            case "01C": case "02E":                                                                // EFFECT_ATTACK_UP(_2)
                if (!contrary && AiPositiveHelpers.goodIdeaToRaiseAttack(ctx, atk, def, f.equals("02E") ? 2 : 1)) {
                    viability = incStat(ctx, viability, cls, 2, atk, def, move, ATK, 2);          // INCREASE_STAT_VIABILITY(ATK, 8, 2)
                }
                return viability;
            case "01D": case "02F": case "01E": case "038": {                                      // EFFECT_DEFENSE_UP(_2) / DEFENSE_CURL
                if (f.equals("01E") && AiCalc.moveFunctionInMoveset(atk, "0D3") && atk.effects.intVal(PBEffects.Battler.DefenseCurl) == 0) {
                    return incStat(ctx, viability, cls, 5, atk, def, move, DEF, 6);                // Rollout/Ice Ball in the moveset
                }
                int ret = contrary ? 0 : AiPositiveHelpers.goodIdeaToRaiseDefense(ctx, atk, def, f.equals("01D") || f.equals("01E") ? 1 : 2);
                if (ret == 1) viability = incStat(ctx, viability, cls, 2, atk, def, move, ATK, 4);  // AI_DEFENSE_PLUS: foe likely physical
                else if (ret != 0) viability = incStat(ctx, viability, cls, 1, atk, def, move, DEF, 2);
                return viability;
            }
            case "020": case "032": case "039":                                                    // EFFECT_SPECIAL_ATTACK_UP(_2)
                if (!contrary && AiPositiveHelpers.goodIdeaToRaiseSpAttack(ctx, atk, def, f.equals("020") ? 1 : 2)) {
                    viability = incStat(ctx, viability, cls, 2, atk, def, move, SPA, 2);
                }
                return viability;
            case "033":                                                                            // EFFECT_SPECIAL_DEFENSE_UP_2
                if (!contrary && AiPositiveHelpers.goodIdeaToRaiseSpDefense(ctx, atk, def, 2)) {
                    viability = incStat(ctx, viability, cls, 1, atk, def, move, SPD_EF, 4);       // INCREASE_STAT_VIABILITY(SPDEF, 10, 1)
                }
                return viability;
            case "01F": case "030": case "031":                                                    // EFFECT_SPEED_UP(_2) (:262)
                if (!contrary && AiPositiveHelpers.goodIdeaToRaiseSpeed(ctx, atk, def, f.equals("01F") ? 1 : 2)) {
                    viability = incStat(ctx, viability, cls, 3, atk, def, move, SPD, 2);
                }
                return viability;
            case "022": case "034": case "037": {                                                  // EFFECT_EVASION_UP(_2) / MINIMIZE / ACUPRESSURE (:284)
                if (f.equals("037")) {
                    if (ctx.simulatedRng[1] < 25 && !contrary && !AiPositiveHelpers.badIdeaToRaiseStat(ctx, atk, def, true)) {
                        viability = incStat(ctx, viability, cls, 2, atk, def, move, AiPositiveHelpers.ALL_STATS, 6);
                    }
                    return viability;
                }
                if (!contrary && AiPositiveHelpers.goodIdeaToRaiseEvasion(ctx, atk, def, f.equals("022") ? 1 : 2)) {
                    viability = incStat(ctx, viability, cls, 4, atk, def, move, EVA, 6);          // best to go until maxed
                }
                return viability;
            }
            case "027": case "028": case "15C":                                                    // EFFECT_ATK_SPATK_UP (:305)
                if (!contrary) {
                    if (AiCalc.physicalMoveInMoveset(ctx, atk) && AiPositiveHelpers.goodIdeaToRaiseAttack(ctx, atk, def, 1)) {
                        viability = incStat(ctx, viability, cls, 2, atk, def, move, ATK, 2);
                    } else if (AiCalc.specialMoveInMoveset(ctx, atk) && AiPositiveHelpers.goodIdeaToRaiseSpAttack(ctx, atk, def, 1)) {
                        viability = incStat(ctx, viability, cls, 2, atk, def, move, SPA, 2);
                    }
                }
                return viability;
            case "029":                                                                            // EFFECT_ATK_ACC_UP (:315)
                if (!contrary) {
                    if (AiCalc.physicalMoveInMoveset(ctx, atk) && AiPositiveHelpers.goodIdeaToRaiseAttack(ctx, atk, def, 1)) {
                        viability = incStat(ctx, viability, cls, 2, atk, def, move, ATK, 2);
                    } else if (AiPositiveHelpers.goodIdeaToRaiseAccuracy(ctx, atk, def, 1)) {
                        viability = incStat(ctx, viability, cls, 2, atk, def, move, ACC, 6);
                    }
                }
                return viability;
            case "02A": case "137": case "112":                                                    // EFFECT_COSMIC_POWER (:329); Stockpile (:493) falls to it
                if (f.equals("112") && AiCalc.moveFunctionInMoveset(atk, "113", "114")) {
                    return incStatus(ctx, viability, cls, 2, atk, def);
                }
                if (!contrary) {
                    int ret = AiPositiveHelpers.goodIdeaToRaiseDefense(ctx, atk, def, 1);
                    if (ret == 1) viability = incStat(ctx, viability, cls, 2, atk, def, move, ATK, 4);
                    else if (ret != 0) viability = incStat(ctx, viability, cls, 1, atk, def, move, DEF, 2);
                    else if (AiPositiveHelpers.goodIdeaToRaiseSpDefense(ctx, atk, def, 1)) {
                        viability = incStat(ctx, viability, cls, 1, atk, def, move, SPD_EF, 4);
                    }
                }
                return viability;
            case "024": case "025":                                                                // EFFECT_BULK_UP (:341)
                if (!contrary) {
                    if (AiCalc.physicalMoveInMoveset(ctx, atk) && AiPositiveHelpers.goodIdeaToRaiseAttack(ctx, atk, def, 1)) {
                        viability = incStat(ctx, viability, cls, 2, atk, def, move, ATK, 2);
                    } else if (AiPositiveHelpers.goodIdeaToRaiseDefense(ctx, atk, def, 1) != 0) {
                        viability = incStat(ctx, viability, cls, 1, atk, def, move, DEF, 2);
                    }
                }
                return viability;
            case "02C": case "02B": {                                                              // EFFECT_CALM_MIND / Quiver Dance (:354)
                if (contrary) return viability;
                if (f.equals("02B")) {
                    if (defAbility.equals("DANCER")) return viability;
                    if (AiPositiveHelpers.goodIdeaToRaiseSpeed(ctx, atk, def, 2)) return incStat(ctx, viability, cls, 3, atk, def, move, SPD, 2);
                }
                if (AiPositiveHelpers.goodIdeaToRaiseSpAttack(ctx, atk, def, 1)) {
                    viability = incStat(ctx, viability, cls, 2, atk, def, move, SPA, 2);
                } else if (AiPositiveHelpers.goodIdeaToRaiseSpDefense(ctx, atk, def, 1)) {
                    viability = incStat(ctx, viability, cls, 1, atk, def, move, SPD_EF, 4);
                }
                return viability;
            }
            case "026": case "035": case "036": {                                                  // EFFECT_DRAGON_DANCE; Shell Smash; Shift Gear (:410)
                if (f.equals("035")) {
                    if (contrary) {                                                                 // AI_COSMIC_POWER
                        int ret = AiPositiveHelpers.goodIdeaToRaiseDefense(ctx, atk, def, 1);
                        if (ret == 1) return incStat(ctx, viability, cls, 2, atk, def, move, ATK, 4);
                        if (ret != 0) return incStat(ctx, viability, cls, 1, atk, def, move, DEF, 2);
                        if (AiPositiveHelpers.goodIdeaToRaiseSpDefense(ctx, atk, def, 1)) return incStat(ctx, viability, cls, 1, atk, def, move, SPD_EF, 4);
                        return viability;
                    }
                    if (AiPositiveHelpers.goodIdeaToRaiseSpeed(ctx, atk, def, 2)) return incStat(ctx, viability, cls, 3, atk, def, move, SPD, 2);
                    if (AiPositiveHelpers.goodIdeaToRaiseAttack(ctx, atk, def, 2)) return incStat(ctx, viability, cls, 2, atk, def, move, ATK, 2);
                    if (AiPositiveHelpers.goodIdeaToRaiseSpAttack(ctx, atk, def, 2)) return incStat(ctx, viability, cls, 2, atk, def, move, SPA, 2);
                    return viability;
                }
                if (contrary) return viability;
                if (AiPositiveHelpers.goodIdeaToRaiseSpeed(ctx, atk, def, 1)) return incStat(ctx, viability, cls, 3, atk, def, move, SPD, 2);
                if (AiPositiveHelpers.goodIdeaToRaiseAttack(ctx, atk, def, 1)) return incStat(ctx, viability, cls, 2, atk, def, move, ATK, 2);
                return viability;
            }
            case "021": {                                                                          // EFFECT_CHARGE (:488)
                if (AiCalc.damagingTypeInMoveset(ctx, atk, "ELECTRIC")) return incStatus(ctx, viability, cls, 2, atk, def);
                if (!contrary && AiPositiveHelpers.goodIdeaToRaiseSpDefense(ctx, atk, def, 1)) {   // AI_SP_DEFENSE_PLUS_FULL
                    viability = incStat(ctx, viability, cls, 1, atk, def, move, SPD_EF, 4);
                }
                return viability;
            }

            // ---- lowered target stats (:520-) ----
            case "042": case "04B":
                return AiPositiveHelpers.goodIdeaToLowerAttack(ctx, def, atk, move) ? incStatus(ctx, viability, cls, 1, atk, def) : viability;
            case "043": case "04C":
                return AiPositiveHelpers.goodIdeaToLowerDefense(ctx, def, atk, move) ? incStatus(ctx, viability, cls, 1, atk, def) : viability;
            case "044":
                return AiPositiveHelpers.goodIdeaToLowerSpeed(ctx, def, atk, move, 1) ? incStatus(ctx, viability, cls, 2, atk, def) : viability;
            case "04D":
                return AiPositiveHelpers.goodIdeaToLowerSpeed(ctx, def, atk, move, 2) ? incStatus(ctx, viability, cls, 2, atk, def) : viability;
            case "045": case "04E":
                return AiPositiveHelpers.goodIdeaToLowerSpAtk(ctx, def, atk, move) ? incStatus(ctx, viability, cls, 1, atk, def) : viability;
            case "046": case "04F":
                return AiPositiveHelpers.goodIdeaToLowerSpDef(ctx, def, atk, move) ? incStatus(ctx, viability, cls, 1, atk, def) : viability;
            case "047":
                return AiPositiveHelpers.goodIdeaToLowerAccuracy(ctx, def, atk, move) ? incStatus(ctx, viability, cls, 1, atk, def) : viability;
            case "048":
                return AiPositiveHelpers.goodIdeaToLowerEvasion(ctx, def, atk) ? incStatus(ctx, viability, cls, 2, atk, def) : viability;
            case "139":                                                                            // EFFECT_PLAY_NICE
                return AiPositiveHelpers.goodIdeaToLowerAttack(ctx, def, atk, move) || AiPositiveHelpers.goodIdeaToLowerSpAtk(ctx, def, atk, move)
                        ? incStatus(ctx, viability, cls, 1, atk, def) : viability;
            case "140":                                                                            // EFFECT_VENOM_DRENCH
                return AiPositiveHelpers.goodIdeaToLowerAttack(ctx, def, atk, move) || AiPositiveHelpers.goodIdeaToLowerSpAtk(ctx, def, atk, move)
                        || AiPositiveHelpers.goodIdeaToLowerSpeed(ctx, def, atk, move, 1) ? incStatus(ctx, viability, cls, 1, atk, def) : viability;
            case "04A":                                                                            // EFFECT_TICKLE
                return AiPositiveHelpers.goodIdeaToLowerAttack(ctx, def, atk, move) || AiPositiveHelpers.goodIdeaToLowerDefense(ctx, def, atk, move)
                        ? incStatus(ctx, viability, cls, 1, atk, def) : viability;

            case "051": {                                                                          // EFFECT_HAZE (:640)
                if (AiPositiveHelpers.shouldPhaze(ctx, atk, def, move, cls)) {
                    if (cls == AiCalc.CLASS_PHAZING) viability = inc(viability, 8);
                    else viability = incStatus(ctx, viability, cls, 2, atk, def);
                } else if (AiPositiveHelpers.countUsefulStatChanges(ctx, atk, atk, def, true) > 0) {
                    viability = incStatus(ctx, viability, cls, 1, atk, def);                      // reset lowered stats
                }
                return viability;
            }
            case "0EB": {                                                                          // EFFECT_ROAR (:660)
                if (!AiCalc.blockedBySubstitute(move, atk, def) && AiPositiveHelpers.shouldPhaze(ctx, atk, def, move, cls)) {
                    if (cls == AiCalc.CLASS_PHAZING) {
                        viability = AiCalc.canKnockOut(ctx, def, atk) && move.priority() < 0 ? inc(viability, 1) : inc(viability, 8);
                    } else {
                        viability = incStatus(ctx, viability, cls, 2, atk, def);
                    }
                }
                return viability;
            }

            case "0D5": case "0D6": case "0D8": case "114":                                         // EFFECT_RESTORE_HP / MORNING_SUN / SWALLOW (:698)
                return recover(ctx, atk, def, move, viability, cls);
            case "0D9": {                                                                          // EFFECT_REST (:680)
                if (resistsAllMovesAtFullHealth(ctx, def, atk) && !AiCalc.moveFunctionInMoveset(def, "02C" /* offensive set-up: 登记 */)) {
                    if (atk.hp < atk.maxHp() && AiCalc.takingSecondaryDamage(battle, def)) return recoverBoost(ctx, atk, def, viability, cls);
                    return recover(ctx, atk, def, move, viability, cls);
                }
                if (atk.hasActiveItem(new String[] {"CHESTOBERRY", "LUMBERRY"}) || AiCalc.moveFunctionInMoveset(atk, "0B4", "011")
                        || atkAbility.equals("SHEDSKIN") || atkAbility.equals("EARLYBIRD")) {
                    return recover(ctx, atk, def, move, viability, cls);
                }
                if (AiCalc.takingSecondaryDamage(battle, def) && AiCalc.classStall(cls)) return recoverBoost(ctx, atk, def, viability, cls);
                if (atk.statused() && (ctx.random() & 1) != 0) return recover(ctx, atk, def, move, viability, cls);
                return viability;
            }

            case "005": case "006": {                                                              // EFFECT_POISON / TOXIC (:729)
                if (!badIdeaToPoison(ctx, def, atk)) {
                    if (AiCalc.named(move, "VENOSHOCK") || AiCalc.moveFunctionInMoveset(atk, "07B", "140") || atkAbility.equals("MERCILESS")) {
                        viability = incStatus(ctx, viability, cls, 2, atk, def);
                    } else {
                        viability = incStatus(ctx, viability, cls, 1, atk, def);                  // AI enjoys poisoning
                    }
                }
                return viability;
            }
            case "056":                                                                            // EFFECT_MIST (:756)
                if (cls == AiCalc.CLASS_SCREENS || cls == AiCalc.CLASS_SWEEPER_SETUP_SCREENS) viability = inc(viability, 6);
                return viability;
            case "023":                                                                            // EFFECT_FOCUS_ENERGY (:769)
                return incStatus(ctx, viability, cls, atkAbility.equals("SUPERLUCK") || atkAbility.equals("SNIPER") || atk.hasActiveItem("SCOPELENS") ? 2 : 1, atk, def);
            case "013": {                                                                          // EFFECT_CONFUSE (:778)
                if (AiCalc.canBeConfused(battle, def, atk)) {
                    boolean boost = def.hasStatus("PARALYSIS") || def.effects.intVal(PBEffects.Battler.Attract) >= 0;
                    viability = incStatus(ctx, viability, cls, boost ? 2 : 1, atk, def);
                }
                return viability;
            }
            case "007": {                                                                          // EFFECT_PARALYZE (:821) 登记: IncreaseViabilityForSpeedControl, flinch moves
                if (!badIdeaToParalyze(ctx, def, atk)) {
                    boolean goFirstAfter = defSpeed >= atkSpeed && defSpeed / 2 < atkSpeed;
                    boolean boost = goFirstAfter || def.effects.intVal(PBEffects.Battler.Attract) >= 0 || def.effects.intVal(PBEffects.Battler.Confusion) > 0;
                    viability = incStatus(ctx, viability, cls, boost ? 2 : 1, atk, def);
                }
                return viability;
            }
            case "0DC": {                                                                          // EFFECT_LEECH_SEED (:944)
                if (def.hasType("GRASS") || def.effects.intVal(PBEffects.Battler.LeechSeed) != -1
                        || AiCalc.moveFunctionInMoveset(def, "110") || defAbility.equals("LIQUIDOOZE") || defAbility.equals("MAGICGUARD")) {
                    return viability;
                }
                return incStatus(ctx, viability, cls, 3, atk, def);
            }
            case "011": case "0B4": {                                                              // EFFECT_SNORE / SLEEP_TALK (:1002)
                if (atk.hasStatus("SLEEP") && atk.statusCount > 1) viability = inc(viability, 10);
                return viability;
            }
            case "15E": {                                                                          // MOVE_LASERFOCUS (:1010)
                if (AiCalc.canKnockOut(ctx, atk, def)) return viability;
                if (atkSpeed > defSpeed) { if (AiCalc.canKnockOut(ctx, def, atk)) return viability; }
                else if (AiCalc.can2HKO(ctx, def, atk)) return viability;
                return incStatus(ctx, viability, cls, 1, atk, def);
            }
            default:
                return part2(ctx, atk, def, move, viability, cls, atkAbility, defAbility);
        }
    }

    /**
     * Part 2 (ai_positives.c:1010-1760): Destiny Bond, Nightmare, Curse, Foresight/Miracle Eye, Perish Song, Swagger/Flatter, Attract, Safeguard,
     * Rollout, Fury Cutter, Belly Drum, weather moves, Pursuit, Baton Pass (the class bonus), the entry hazards.
     * 登记: Protect ({@code ShouldProtect}), Spite, Heal Bell/Wish, Thief, Mean Look (ShouldTrap), pivots ({@code ShouldPivot}), Rapid Spin/Defog,
     * Psych Up, the Z-Crystal/doubles branches.
     */
    private static int part2(AiCtx ctx, Battler atk, Battler def, BattleMove move, int viability, int cls, String atkAbility, String defAbility) {
        Battle battle = ctx.battle;
        String f = move.function();
        int atkSpeed = AiCalc.speed(atk);
        int defSpeed = AiCalc.speed(def);
        switch (f) {
            case "0E7": {                                                                          // EFFECT_DESTINY_BOND (:1012)
                if (AiCalc.moveWouldHitFirst(ctx, move, atk, def)) { if (AiCalc.canKnockOut(ctx, def, atk)) viability = incStatus(ctx, viability, cls, 3, atk, def); }
                else if (AiCalc.can2HKO(ctx, def, atk)) viability = incStatus(ctx, viability, cls, 3, atk, def);
                return viability;
            }
            case "10F": {                                                                          // EFFECT_NIGHTMARE (:1105)
                if (defAbility.equals("MAGICGUARD")) return viability;
                if (defAbility.equals("COMATOSE") || (def.hasStatus("SLEEP") && def.statusCount > 1)) viability = incStatus(ctx, viability, cls, 3, atk, def);
                return viability;
            }
            case "10D": {                                                                          // EFFECT_CURSE (:1112)
                if (atk.hasType("GHOST")) return incStatus(ctx, viability, cls, isTrapped(def) ? 3 : 1, atk, def);
                if (atkAbility.equals("CONTRARY") || defAbility.equals("MAGICGUARD")) return viability;
                BattleMove foe = ctx.prediction(def);
                if (foe != null && AiCalc.oneOf(foe, "051", "0EB")) return viability;               // IsMovePredictionPhazingMove
                if (AiCalc.moveFunctionInMoveset(atk, "08D")) return incStat(ctx, viability, cls, 4, atk, def, move, ATK, 6);   // Gyro Ball
                if (atk.stage(SPD) < -3) return viability;
                if (atk.stage(ATK) < 2) return incStat(ctx, viability, cls, 2, atk, def, move, ATK, 2);
                if (atk.stage(DEF) < 2) return incStat(ctx, viability, cls, 1, atk, def, move, DEF, 2);
                return viability;
            }
            case "0A8": {                                                                          // MOVE_MIRACLEEYE (:1370)
                if (def.stage(EVA) > 0 || (def.hasType("DARK") && AiCalc.damagingTypeInMoveset(ctx, atk, "PSYCHIC"))) viability = incStatus(ctx, viability, cls, 2, atk, def);
                return viability;
            }
            case "0A7": {                                                                          // EFFECT_FORESIGHT (:1378)
                if (atkAbility.equals("SCRAPPY")) return viability;
                if (def.stage(EVA) > 0 || (def.hasType("GHOST")
                        && (AiCalc.damagingTypeInMoveset(ctx, atk, "NORMAL") || AiCalc.damagingTypeInMoveset(ctx, atk, "FIGHTING")))) {
                    viability = incStatus(ctx, viability, cls, 2, atk, def);
                }
                return viability;
            }
            case "0E5":                                                                            // EFFECT_PERISH_SONG (:1402)
                return isTrapped(def) ? incStatus(ctx, viability, cls, 3, atk, def) : viability;
            case "041": case "040": {                                                              // EFFECT_SWAGGER / FLATTER (:1440)
                boolean psychUp = AiCalc.moveFunctionInMoveset(atk, "055", "15D") || (f.equals("041") && AiCalc.named(firstNamed(atk, "FOULPLAY"), "FOULPLAY"));
                if (psychUp) return incStatus(ctx, viability, cls, 2, atk, def);
                if (AiCalc.canBeConfused(battle, def, atk)) {
                    boolean boost = def.hasStatus("PARALYSIS") || def.effects.intVal(PBEffects.Battler.Attract) >= 0;
                    viability = incStatus(ctx, viability, cls, boost ? 2 : 1, atk, def);
                }
                return viability;
            }
            case "016": {                                                                          // EFFECT_ATTRACT (:1465)
                if (AiCalc.willFaintFromSecondaryDamage(battle, def) && !AiCalc.moveWouldHitFirst(ctx, move, atk, def)) return viability;
                boolean boost = def.statused() || def.effects.intVal(PBEffects.Battler.Confusion) > 0 || isTrapped(def);
                return incStatus(ctx, viability, cls, boost ? 2 : 1, atk, def);
            }
            case "01A": {                                                                          // EFFECT_SAFEGUARD (:1476)
                boolean teamSupport = cls == AiCalc.CLASS_BATON_PASS || cls == AiCalc.CLASS_CLERIC || cls == AiCalc.CLASS_SCREENS || cls == AiCalc.CLASS_PHAZING;
                if (teamSupport && !(battle.terrain() == PBBattleTerrains.Misty && !atk.airborne())) viability = incStatus(ctx, viability, cls, 1, atk, def);
                return viability;
            }
            case "0D3": {                                                                          // EFFECT_ROLLOUT (:1432)
                if (AiCalc.classSweeper(cls) && atk.effects.intVal(PBEffects.Battler.DefenseCurl) > 0) viability = inc(viability, 8);
                return viability;
            }
            case "091": {                                                                          // EFFECT_FURY_CUTTER (:1457)
                if (AiCalc.classSweeper(cls) && atk.hasActiveItem("METRONOME")) viability = inc(viability, 3);
                return viability;
            }
            case "03A": {                                                                          // EFFECT_BELLY_DRUM (:1593)
                if (!atkAbility.equals("CONTRARY") && AiCalc.physicalMoveInMoveset(ctx, atk)) {
                    if (AiPositiveHelpers.badIdeaToRaiseStat(ctx, atk, def, true)) return viability;
                    viability = incStat(ctx, viability, cls, 2, atk, def, move, ATK, 2);
                }
                return viability;
            }
            case "0FF": case "100": case "101": {                                                  // EFFECT_SUNNY_DAY / RAIN_DANCE / SANDSTORM (:1405,:1536,:1565)
                boolean useful;
                if (f.equals("101")) {
                    useful = atkAbility.equals("SANDVEIL") || atkAbility.equals("SANDRUSH") || atkAbility.equals("SANDFORCE") || atkAbility.equals("OVERCOAT")
                            || atkAbility.equals("MAGICGUARD") || atk.hasActiveItem("SAFETYGOGGLES") || atk.hasType("ROCK") || atk.hasType("STEEL")
                            || atk.hasType("GROUND") || AiCalc.named(firstNamed(atk, "SHOREUP"), "SHOREUP") || AiCalc.named(firstNamed(atk, "WEATHERBALL"), "WEATHERBALL")
                            || atk.hasActiveItem("SMOOTHROCK");
                } else if (f.equals("100")) {
                    useful = !atk.hasActiveItem("UTILITYUMBRELLA") && (atkAbility.equals("SWIFTSWIM") || atkAbility.equals("FORECAST") || atkAbility.equals("HYDRATION")
                            || atkAbility.equals("RAINDISH") || atkAbility.equals("DRYSKIN") || AiCalc.moveFunctionInMoveset(atk, "008", "015")
                            || AiCalc.moveFunctionInMoveset(def, "0D8") || AiCalc.named(firstNamed(atk, "WEATHERBALL"), "WEATHERBALL")
                            || AiCalc.damagingTypeInMoveset(ctx, atk, "WATER") || AiCalc.damagingTypeInMoveset(ctx, def, "FIRE") || atk.hasActiveItem("DAMPROCK"));
                } else {
                    useful = !atk.hasActiveItem("UTILITYUMBRELLA") && (atkAbility.equals("CHLOROPHYLL") || atkAbility.equals("FLOWERGIFT") || atkAbility.equals("FORECAST")
                            || atkAbility.equals("LEAFGUARD") || atkAbility.equals("SOLARPOWER") || atkAbility.equals("HARVEST") || AiCalc.moveFunctionInMoveset(atk, "0C4", "0D8")
                            || AiCalc.moveFunctionInMoveset(def, "008", "015") || AiCalc.named(firstNamed(atk, "WEATHERBALL"), "WEATHERBALL") || AiCalc.named(firstNamed(atk, "GROWTH"), "GROWTH")
                            || AiCalc.damagingTypeInMoveset(ctx, atk, "FIRE") || AiCalc.damagingTypeInMoveset(ctx, def, "WATER") || atk.hasActiveItem("HEATROCK"));
                }
                return useful ? incStatus(ctx, viability, cls, 2, atk, def) : viability;
            }
            case "088": {                                                                          // EFFECT_PURSUIT (:1520)
                if (AiCalc.classSweeper(cls)) {
                    BattleMove foe = ctx.prediction(def);
                    if (ctx.predictedToSwitch(def)) viability = inc(viability, 3);
                    else if (foe != null && AiCalc.oneOf(foe, "0EE") && !AiCalc.moveWouldHitFirst(ctx, move, atk, def)) viability = inc(viability, 3);
                }
                return viability;
            }
            case "0ED": {                                                                          // MOVE_BATONPASS (:1502)
                if (cls == AiCalc.CLASS_BATON_PASS) viability = inc(viability, 3);
                return viability;
            }
            case "103": case "104": case "105": case "153":                                        // EFFECT_SPIKES (:1360)
                return hazards(ctx, atk, def, move, viability, cls);
            default:
                return part3(ctx, atk, def, move, viability, cls, atkAbility, defAbility);
        }
    }

    /**
     * Part 3 (ai_positives.c:1760-2125): Fake Out, Hail, Torment, Will-O-Wisp, Memento, Taunt, Ingrain/Aqua Ring, Magic Coat, Brick Break.
     * 登记: Trick/Bestow and Knock Off (item tables), Skill Swap family (ability ratings), Wish, Follow Me (doubles), Superpower/Overheat (Contrary).
     */
    private static int part3(AiCtx ctx, Battler atk, Battler def, BattleMove move, int viability, int cls, String atkAbility, String defAbility) {
        Battle battle = ctx.battle;
        String f = move.function();
        BattleMove predicted = ctx.prediction(def);
        switch (f) {
            case "012": {                                                                          // EFFECT_FAKE_OUT (:1765): every singles class falls to the default +8
                if (AiCalc.named(move, "FAKEOUT") && AiCalc.shouldUseFakeOut(ctx, atk, def)) viability = inc(viability, 8);
                return viability;
            }
            case "102": {                                                                          // EFFECT_HAIL (:1773)
                if (AiCalc.moveFunctionInMoveset(atk, "167")) {                                   // Aurora Veil usable
                    if (cls == AiCalc.CLASS_SCREENS || cls == AiCalc.CLASS_SWEEPER_SETUP_SCREENS) return inc(viability, 8);
                }
                if (atkAbility.equals("SNOWCLOAK") || atkAbility.equals("ICEBODY") || atkAbility.equals("FORECAST") || atkAbility.equals("SLUSHRUSH")
                        || atkAbility.equals("MAGICGUARD") || atkAbility.equals("OVERCOAT") || AiCalc.named(firstNamed(atk, "BLIZZARD"), "BLIZZARD")
                        || AiCalc.moveFunctionInMoveset(atk, "167") || AiCalc.named(firstNamed(atk, "WEATHERBALL"), "WEATHERBALL")
                        || AiCalc.moveFunctionInMoveset(def, "0D8") || atk.hasActiveItem("ICYROCK")) {
                    viability = incStatus(ctx, viability, cls, 2, atk, def);
                }
                return viability;
            }
            case "0B7":                                                                            // EFFECT_TORMENT (:1810)
                return incStatus(ctx, viability, cls, AiCalc.choiceLocked(def) ? 2 : 0, atk, def);
            case "00A": {                                                                          // EFFECT_WILL_O_WISP (:1818)
                if (!badIdeaToBurn(ctx, def, atk)) {
                    if (predicted != null && predicted.physical() && !predicted.statusMove() && AiCalc.knocksOutXHits(ctx, predicted, def, atk, 1)) {
                        viability = incStatus(ctx, viability, cls, 3, atk, def);
                    } else if (AiCalc.physicalMoveInMoveset(ctx, def) || AiCalc.named(firstNamed(atk, "INFERNALPARADE"), "INFERNALPARADE")) {
                        viability = incStatus(ctx, viability, cls, 2, atk, def);
                    } else {
                        viability = incStatus(ctx, viability, cls, 1, atk, def);
                    }
                }
                return viability;
            }
            case "0E2":                                                                            // EFFECT_MEMENTO: Memento (:1845)
                return Math.max(0, viability - 5);
            case "0BA": {                                                                          // EFFECT_TAUNT (:1873)
                if (def.hasStatus("SLEEP") && !AiCalc.moveFunctionInMoveset(def, "0B4") && !AiCalc.willFaintFromSecondaryDamage(battle, atk)) {
                    if (AiCalc.speed(atk) <= AiCalc.speed(def)) { if (def.statusCount > 2) return viability; }
                    else if (def.statusCount > 1) return viability;                                // wait until the last possible turn
                }
                if (predicted != null && predicted.statusMove() || goodToTaunt(AiCalc.fightingStyle(ctx, def))) {
                    return incStatus(ctx, viability, cls, 3, atk, def);
                }
                for (int i = 0; i < Battler.MOVES_MAX; i++) {                                      // StatusMoveInMoveset(bankDef)
                    BattleMove m = def.moveSlot(i);
                    if (m != null && m.statusMove()) return incStatus(ctx, viability, cls, 2, atk, def);
                }
                return viability;
            }
            case "0DA": case "0DB":                                                                // EFFECT_INGRAIN / Aqua Ring (:2002)
                return incStatus(ctx, viability, cls, atk.hasActiveItem("BIGROOT") ? 2 : 1, atk, def);
            case "0B1": {                                                                          // EFFECT_MAGIC_COAT (:2042)
                if (predicted != null && predicted.statusMove() && AiCalc.has(predicted, 'c')) viability = incStatus(ctx, viability, cls, 3, atk, def);
                return viability;
            }
            case "10A": {                                                                          // EFFECT_BRICK_BREAK (:2055), no raid shields
                BattleSide side = def.pbOwnSide();
                if (side.effects.intVal(PBEffects.Side.Reflect) > 0 || side.effects.intVal(PBEffects.Side.LightScreen) > 0
                        || side.effects.intVal(PBEffects.Side.AuroraVeil) > 0) {
                    viability = AiCalc.classSweeper(cls) ? inc(viability, 3) : incStatus(ctx, viability, cls, 2, atk, def);
                }
                return viability;
            }
            default:
                return AiPositiveMore.apply(ctx, atk, def, move, viability, cls, atkAbility, defAbility);
        }
    }

    /** {@code IsClassGoodToTaunt(class)} (ai_advanced.c:326). */
    private static boolean goodToTaunt(int c) {
        return c == AiCalc.CLASS_STALL || c == AiCalc.CLASS_SWEEPER_SETUP_SCREENS || c == AiCalc.CLASS_BATON_PASS || c == AiCalc.CLASS_CLERIC
                || c == AiCalc.CLASS_SCREENS || c == AiCalc.CLASS_ENTRY_HAZARDS;
    }

    /** {@code BadIdeaToBurn(bankDef,bankAtk)} (ai_util.c:2951), single battle. */
    static boolean badIdeaToBurn(AiCtx ctx, Battler def, Battler atk) {
        String d = def.ability == null ? "" : def.ability;
        return !AiCalc.canBeBurned(ctx.battle, def, atk)
                || d.equals("SHEDSKIN") || d.equals("QUICKFEET") || d.equals("MAGICGUARD")
                || (d.equals("MARVELSCALE") && AiCalc.physicalMoveInMoveset(ctx, atk))
                || (d.equals("FLAREBOOST") && AiCalc.specialMoveInMoveset(ctx, def))
                || (d.equals("GUTS") && AiCalc.physicalMoveInMoveset(ctx, def))
                || hasNamed(def, "FACADE") || AiCalc.moveFunctionInMoveset(def, "01B");
    }

    private static BattleMove firstNamed(Battler b, String name) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = b.moveSlot(i);
            if (m != null && AiCalc.named(m, name)) return m;
        }
        return new BattleMove(null);
    }

    /** {@code IsTrapped(bank,TRUE)} (ai_util.c): held in by a trapping move, Mean Look or a Ghost-type exemption. */
    private static boolean isTrapped(Battler b) {
        if (b.hasType("GHOST")) return false;
        return b.effects.intVal(PBEffects.Battler.MeanLook) >= 0 || b.effects.intVal(PBEffects.Battler.Trapping) > 0
                || b.effects.truthy(PBEffects.Battler.Ingrain);                                    // 登记: Shadow Tag / Arena Trap / Magnet Pull
    }

    /** {@code IsMonAffectedByHazards(mon)} + grounding for the bench Pokemon of {@code def}'s side. */
    private static boolean benchAffectedByHazards(Battler member, boolean needsGrounding) {
        Pokemon p = member.pokemon;
        if ("MAGICGUARD".equals(p.ability) || "HEAVYDUTYBOOTS".equals(p.item)) return false;
        if (needsGrounding && (p.types().contains("FLYING", false) || "LEVITATE".equals(p.ability) || "AIRBALLOON".equals(p.item))) return false;
        return true;
    }

    /** The EFFECT_SPIKES case (:1360-1560): does the foe's bench have a Pokemon the hazard hurts. */
    private static int hazards(AiCtx ctx, Battler atk, Battler def, BattleMove move, int viability, int cls) {
        String f = move.function();
        boolean useful = false;
        for (Battler m : ctx.battle.partyOf(def.index)) {
            if (m == null || m == def || m.removedFromParty || m.fainted() || m.pokemon.egg) continue;     // i != gBattlerPartyIndexes[foe]
            if (f.equals("153")) {                                                                           // Sticky Web: grounded and slower than one of ours
                if (!benchAffectedByHazards(m, true)) continue;
                for (Battler mine : ctx.battle.partyOf(atk.index)) {
                    if (mine == null || mine.removedFromParty || mine.fainted() || mine.pokemon.egg) continue;
                    if (mine.pokemon.speed() < m.pokemon.speed()) { useful = true; break; }
                }
            } else if (f.equals("105")) {                                                                    // Stealth Rock
                useful = benchAffectedByHazards(m, false);
            } else if (f.equals("104")) {                                                                    // Toxic Spikes
                if (!benchAffectedByHazards(m, true)) continue;
                if (m.pokemon.types().contains("POISON", false)) { useful = false; break; }                  // someone can just absorb them
                if (!m.pokemon.types().contains("STEEL", false) && !"IMMUNITY".equals(m.pokemon.ability)) useful = true;
            } else {                                                                                         // Spikes
                useful = benchAffectedByHazards(m, true);
            }
            if (useful && !f.equals("104")) break;
        }
        return useful ? increaseEntryHazards(ctx, viability, cls, atk, def, move) : viability;
    }

    /** {@code IncreaseEntryHazardsViability} (ai_advanced.c:2428-2570), single battle. 登记: HasUsedMoveWithEffect(Magic Coat). */
    private static int increaseEntryHazards(AiCtx ctx, int viability, int cls, Battler atk, Battler def, BattleMove move) {
        int monsLeft = AiUtil.benchAlive(ctx.battle, def) + 1;
        boolean worth = monsLeft != 2;                                                                       // IsWorthSettingHazards
        String f = move.function();
        BattleSide side = def.pbOwnSide();
        switch (cls) {
            case AiCalc.CLASS_SWEEPER_KILL: return viability;
            case AiCalc.CLASS_SWEEPER_SETUP_STATS: case AiCalc.CLASS_SWEEPER_SETUP_STATUS:
                return worth ? incStatus(ctx, viability, cls, 2, atk, def) : viability;
            case AiCalc.CLASS_STALL: case AiCalc.CLASS_CLERIC: case AiCalc.CLASS_SCREENS: case AiCalc.CLASS_SWEEPER_SETUP_SCREENS:
                return incStatus(ctx, viability, cls, worth ? 2 : 1, atk, def);
            case AiCalc.CLASS_BATON_PASS: {
                // effect == EFFECT_SPIKES for every function in this case
                if (AiCalc.moveWouldHitFirst(ctx, move, atk, def)) {
                    if (!AiCalc.canKnockOut(ctx, def, atk) && !AiCalc.willFaintFromSecondaryDamage(ctx.battle, atk)) return inc(viability, 4);
                } else if (!AiCalc.can2HKO(ctx, def, atk)) {
                    return inc(viability, 4);
                }
                return viability;
            }
            case AiCalc.CLASS_PHAZING: {
                boolean moreLayers = (AiCalc.named(move, "SPIKES") && side.effects.intVal(PBEffects.Side.Spikes) > 0)
                        || (AiCalc.named(move, "TOXICSPIKES") && side.effects.intVal(PBEffects.Side.ToxicSpikes) > 0);
                boolean otherHazard = (side.effects.intVal(PBEffects.Side.StealthRock) == 0 && AiCalc.moveFunctionInMoveset(atk, "105"))
                        || (side.effects.intVal(PBEffects.Side.StickyWeb) == 0 && AiCalc.moveFunctionInMoveset(atk, "153"));
                if (moreLayers && otherHazard) return incStatus(ctx, viability, cls, monsLeft == 2 ? 1 : 2, atk, def);
                return incStatus(ctx, viability, cls, monsLeft == 2 ? 2 : 3, atk, def);
            }
            case AiCalc.CLASS_ENTRY_HAZARDS:
                return inc(viability, f.equals("153") ? 7 : f.equals("105") ? 6 : f.equals("104") ? 5 : 4);
            default: return viability;
        }
    }

    /** {@code AI_RECOVER:} (:698-714). */
    static int recover(AiCtx ctx, Battler atk, Battler def, BattleMove move, int viability, int cls) {
        if (AiPositiveHelpers.shouldRecover(ctx, atk, def, move)) return recoverBoost(ctx, atk, def, viability, cls);
        return viability;                                                                           // 登记: Jungle Healing / Lunar Blessing (not in this project)
    }

    /** {@code AI_RECOVER_VIABILITY_INCREASE:} (:701-709). */
    private static int recoverBoost(AiCtx ctx, Battler atk, Battler def, int viability, int cls) {
        if (AiCalc.classStall(cls) || cls == AiCalc.CLASS_PHAZING) return inc(viability, 8);
        return incStatus(ctx, viability, cls, 3, atk, def);
    }

    /** {@code ResistsAllMovesAtFullHealth(bankDef,bankAtk)}: the foe cannot make a dent from full health. 登记: approximated by "no move deals 25%+". */
    private static boolean resistsAllMovesAtFullHealth(AiCtx ctx, Battler foe, Battler self) {
        int full = self.maxHp();
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = foe.moveSlot(i);
            if (m == null || m.statusMove() || !AiCalc.usable(ctx, foe, i)) continue;
            if (AiCalc.calcDmg(ctx.battle, foe, self, m).dmg * 4 >= full) return false;
        }
        return true;
    }

    /** {@code BadIdeaToPoison(bankDef,bankAtk)} (ai_util.c:2878), single battle. */
    static boolean badIdeaToPoison(AiCtx ctx, Battler def, Battler atk) {
        String d = def.ability == null ? "" : def.ability;
        return !AiCalc.canBePoisoned(ctx.battle, def, atk)
                || d.equals("SHEDSKIN") || d.equals("POISONHEAL") || d.equals("QUICKFEET")
                || (d.equals("MAGICGUARD") && !AiCalc.moveFunctionInMoveset(atk, "07B"))
                || (d.equals("MARVELSCALE") && AiCalc.physicalMoveInMoveset(ctx, atk))
                || (d.equals("TOXICBOOST") && AiCalc.physicalMoveInMoveset(ctx, def))
                || (d.equals("GUTS") && AiCalc.physicalMoveInMoveset(ctx, def))
                || hasNamed(def, "FACADE") || AiCalc.moveFunctionInMoveset(def, "01B");
    }

    /** {@code BadIdeaToParalyze(bankDef,bankAtk)} (ai_util.c:2918), single battle. */
    static boolean badIdeaToParalyze(AiCtx ctx, Battler def, Battler atk) {
        String d = def.ability == null ? "" : def.ability;
        return !AiCalc.canBeParalyzed(ctx.battle, def, atk)
                || d.equals("SHEDSKIN") || d.equals("QUICKFEET")
                || (d.equals("MARVELSCALE") && AiCalc.physicalMoveInMoveset(ctx, atk))
                || (d.equals("GUTS") && AiCalc.physicalMoveInMoveset(ctx, def))
                || hasNamed(def, "FACADE") || AiCalc.moveFunctionInMoveset(def, "01B", "0D9");
    }

    private static boolean hasNamed(Battler b, String name) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = b.moveSlot(i);
            if (m != null && AiCalc.named(m, name)) return true;
        }
        return false;
    }
}
