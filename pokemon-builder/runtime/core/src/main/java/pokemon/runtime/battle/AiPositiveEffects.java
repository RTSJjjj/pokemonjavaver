package pokemon.runtime.battle;

import static pokemon.runtime.battle.AiPositiveHelpers.inc;
import static pokemon.runtime.battle.AiPositiveHelpers.incStat;
import static pokemon.runtime.battle.AiPositiveHelpers.incStatus;

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
                return viability;
        }
    }

    /** {@code AI_RECOVER:} (:698-714). */
    private static int recover(AiCtx ctx, Battler atk, Battler def, BattleMove move, int viability, int cls) {
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
