package pokemon.runtime.battle;

import static pokemon.runtime.battle.AiPositiveHelpers.inc;
import static pokemon.runtime.battle.AiPositiveHelpers.incStatus;

/**
 * ai_positives.c (singles) part 4: Explosion (:117), screens (:770), Substitute (:886), Protect (:1132), pivoting moves
 * (:1496) and Knock Off (:2086), with their helpers from ai_advanced.c ({@code ShouldSetUpScreens} :1367,
 * {@code ShouldPivot} :1468, {@code IncreasePivotViability} :2658, {@code IncreaseSubstituteViability} :2323,
 * {@code ShouldUseSubstitute} :2071, {@code ShouldProtect} :1108, {@code IncreaseFoeProtectionViability} :2842).
 *
 * <p>登记: Quick Guard / Wide Guard / Crafty Shield / Mat Block (team protections), Aegislash King's Shield, Z-moves and Dynamax
 * clauses, Wish after a pivot (:1520), Corrosive Gas / Poltergeist, {@code RecalcStrongestMoveIgnoringMove}, Mimic / Disable / Encore /
 * Spite / Thief / Trick / Psych Up / Wish / Heal Bell / Skill Swap family (these need item or ability tables or move history).</p>
 */
final class AiPositiveMore {

    private AiPositiveMore() {
    }

    private static final int DONT_PROTECT = 0, USE_PROTECT = 1, USE_STATUS_THEN_PROTECT = 2;
    private static final int PIVOT = 1, CAN_TRY_PIVOT = 2, DONT_PIVOT = 3;

    static int apply(AiCtx ctx, Battler atk, Battler def, BattleMove move, int viability, int cls, String atkAbility, String defAbility) {
        Battle battle = ctx.battle;
        String f = move.function();
        BattleMove predicted = ctx.prediction(def);
        switch (f) {
            case "0E0": {                                                                          // EFFECT_EXPLOSION (:117)
                if (predicted == null || AiCalc.oneOf(predicted, AiCalc.PROTECT)) return viability;
                if (atk.item != null && atk.item.equals(AiCalc.fx(move).pbCalcType(move, atk) + "GEM")) {
                    return viability + 4;                                                          // gem: the AI was meant to explode
                }
                if (atk.hasActiveItem("CUSTAPBERRY") && AiCalc.healthPercent(atk) <= 25) return viability + 4;
                if (!AiNegativeEffects.canKnockOutWithoutMove(ctx, move, atk, def)) {
                    if (AiCalc.speed(atk) >= AiCalc.speed(def)) {
                        if (AiCalc.can2HKO(ctx, atk, def) && !AiCalc.canKnockOut(ctx, def, atk)) return viability;   // sponge a hit first
                    } else if (AiCalc.can2HKO(ctx, atk, def) && !AiCalc.can2HKO(ctx, def, atk)) {
                        return viability;
                    }
                    return viability + 4;                                                          // counteract the negative check
                }
                return viability;
            }
            case "0A2": case "0A3": case "167": {                                                  // EFFECT_REFLECT / LIGHT_SCREEN / Aurora Veil (:770)
                if (shouldSetUpScreens(ctx, atk, def, move)) {
                    boolean veil = f.equals("167");
                    if (cls == AiCalc.CLASS_SCREENS || cls == AiCalc.CLASS_SWEEPER_SETUP_SCREENS) return viability + (veil ? 8 : 7);
                    return incStatus(ctx, viability, cls, 2, atk, def);
                }
                return viability;
            }
            case "10C": return substitute(ctx, atk, def, viability, cls);                          // EFFECT_SUBSTITUTE (:886)
            case "0AA": case "14B": case "14C": case "168": case "0E8": {                          // EFFECT_PROTECT (:1132)
                if (f.equals("0E8")) return endure(ctx, atk, def, viability, cls);
                if (f.equals("168") && predicted != null && AiCalc.has(predicted, 'a') && AiCalc.canBePoisoned(battle, def, atk)
                        && cls == AiCalc.CLASS_STALL) {
                    return viability + 8;                                                          // Baneful Bunker as a staller
                }
                if (shouldProtect(ctx, atk, def, move) == USE_PROTECT) return foeProtection(ctx, viability, cls, atk, def);
                return viability;
            }
            case "0EE": case "151":                                                                // EFFECT_BATON_PASS: U-Turn, Volt Switch, Parting Shot (:1496)
                return pivot(ctx, atk, def, move, viability, cls);
            case "0F0": {                                                                          // EFFECT_KNOCK_OFF (:2086)
                String item = def.item == null ? "" : def.item;
                if (item.equals("IRONBALL") || item.equals("LAGGINGTAIL") || item.equals("STICKYBARB")) return viability;
                if (!item.isEmpty() && !def.hasActiveAbility("STICKYHOLD") && !AiCalc.blockedBySubstitute(move, atk, def)
                        && !AiNegativeEffects.canKnockOutWithoutMove(ctx, move, atk, def)) {
                    return viability + 3;                                                          // increase past the strongest move
                }
                return viability;
            }
            default:
                return viability;
        }
    }

    // ---- screens ----

    private static boolean moveSplitOnTeam(Battle battle, Battler bank, boolean physical) {
        for (Battler b : battle.partyOf(bank.index)) {
            if (b == null || b.fainted() || b.pokemon == null || b.pokemon.egg) continue;
            for (int i = 0; i < Battler.MOVES_MAX; i++) {
                BattleMove m = b.moveSlot(i);
                if (m != null && !m.statusMove() && (b.moveSlotPp(i) > 0 || b.moveSlotMaxPp(i) == 0) && m.physical() == physical) return true;
            }
        }
        return false;
    }

    /** {@code ShouldSetUpScreens(bankAtk,bankDef,move)} (ai_advanced.c:1367). */
    static boolean shouldSetUpScreens(AiCtx ctx, Battler atk, Battler def, BattleMove move) {
        Battle battle = ctx.battle;
        BattleSide side = atk.pbOwnSide();
        if (side.effects.intVal(PBEffects.Side.AuroraVeil) > 0) return false;
        boolean reflectUp = side.effects.intVal(PBEffects.Side.Reflect) > 0;
        boolean screenUp = side.effects.intVal(PBEffects.Side.LightScreen) > 0;
        String f = move.function();
        if (battle.pbWeather() == PBWeather.Hail && f.equals("167") && !(reflectUp && screenUp)) return true;
        BattleMove defPrediction = ctx.prediction(def);
        boolean defPhysical = AiCalc.physicalMoveInMoveset(ctx, def);
        boolean defSpecial = AiCalc.specialMoveInMoveset(ctx, def);
        if (f.equals("0A2")) {                                                                     // Reflect
            if (defPhysical || moveSplitOnTeam(battle, def, true)) {
                boolean hasLightScreen = !screenUp && AiCalc.moveFunctionInMoveset(atk, "0A3");
                if (!defPhysical && !defSpecial) return true;
                if (defPhysical && defSpecial && hasLightScreen && defPrediction != null && !defPrediction.statusMove() && !defPrediction.physical()) return false;
                if (defPhysical || !hasLightScreen || !defSpecial) return true;
            }
        } else if (f.equals("0A3")) {                                                              // Light Screen
            if (defSpecial || moveSplitOnTeam(battle, def, false)) {
                boolean hasReflect = !reflectUp && AiCalc.moveFunctionInMoveset(atk, "0A2");
                if (!defPhysical && !defSpecial) return true;
                if (defPhysical && defSpecial && hasReflect && defPrediction != null && !defPrediction.statusMove() && defPrediction.physical()) return false;
                if (defSpecial || !hasReflect || !defPhysical) return true;
            }
        }
        return false;
    }

    // ---- Substitute ----

    private static boolean behindSubstitute(Battler b) {
        return b.effects.intVal(PBEffects.Battler.Substitute) > 0;
    }

    /** {@code ShouldUseSubstitute(bankAtk,bankDef)} (ai_advanced.c:2071). */
    private static boolean shouldUseSubstitute(AiCtx ctx, Battler atk, Battler def, BattleMove substitute) {
        BattleMove defPrediction = ctx.prediction(def);
        if (def.hasStatus("SLEEP") || def.hasStatus("FREEZE")) return true;                       // IsBankIncapacitated
        if (defPrediction != null) {
            if (AiCalc.moveWouldHitFirst(ctx, substitute, atk, def)) {
                return AiCalc.finalDamage(ctx, defPrediction, def, atk, 1) < Math.max(1, atk.maxHp() / 4);
            }
            if (ctx.predictedToSwitch(def)) return true;
            return atk.hp - AiCalc.finalDamage(ctx, defPrediction, def, atk, 1) > atk.maxHp() / 4;
        }
        return false;
    }

    private static int substitute(AiCtx ctx, Battler atk, Battler def, int viability, int cls) {
        if (behindSubstitute(atk)) return viability;
        BattleMove sub = null;
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = atk.moveSlot(i);
            if (m != null && m.function().equals("10C")) sub = m;
        }
        if (sub == null || !shouldUseSubstitute(ctx, atk, def, sub)) return viability;
        switch (cls) {
            case AiCalc.CLASS_SWEEPER_SETUP_STATS: return inc(viability, 8);
            case AiCalc.CLASS_SWEEPER_SETUP_STATUS: case AiCalc.CLASS_STALL:
                return incStatus(ctx, viability, cls, 2, atk, def);
            case AiCalc.CLASS_BATON_PASS: return inc(viability, 4);
            case AiCalc.CLASS_CLERIC: case AiCalc.CLASS_SCREENS: case AiCalc.CLASS_SWEEPER_SETUP_SCREENS:
            case AiCalc.CLASS_PHAZING: case AiCalc.CLASS_ENTRY_HAZARDS:
                return incStatus(ctx, viability, cls, 1, atk, def);
            default: return viability;
        }
    }

    // ---- Protect ----

    private static boolean trapped(Battler b) {
        if (b.pbHasType("GHOST")) return false;
        return b.effects.intVal(PBEffects.Battler.MeanLook) >= 0 || b.effects.intVal(PBEffects.Battler.Trapping) > 0
                || b.effects.truthy(PBEffects.Battler.Ingrain);
    }

    /** {@code ShouldProtect(bankAtk,bankDef,move)} (ai_advanced.c:1108), singles: USE_PROTECT / USE_STATUS_THEN_PROTECT count as "protect". */
    private static int shouldProtect(AiCtx ctx, Battler atk, Battler def, BattleMove move) {
        Battle battle = ctx.battle;
        BattleMove predicted = ctx.prediction(def);
        if (AiCalc.willFaintFromSecondaryDamage(battle, atk) && !AiCalc.isMoxie(def.ability == null ? "" : def.ability)) return DONT_PROTECT;
        if (def.hasStatus("SLEEP") || def.hasStatus("FREEZE") || def.effects.intVal(PBEffects.Battler.HyperBeam) > 0) return DONT_PROTECT;   // IsBankIncapacitated
        if (usefulItemToProtectFor(ctx, atk) || usefulAbilityToProtectFor(atk, def)
                || AiCalc.willFaintFromSecondaryDamage(battle, def)
                || (trapped(def) && AiCalc.takingSecondaryDamage(battle, def))
                || (predicted != null && predicted.function().equals("0E0"))
                || (predicted != null && AiCalc.oneOf(predicted, "0C9", "0CA", "0CB", "0CC", "0CD") && def.semiInvulnerable() && AiCalc.has(predicted, 'b'))) {
            return USE_PROTECT;
        }
        if (def.effects.intVal(PBEffects.Battler.Yawn) > 0 && !AiCalc.canKnockOut(ctx, atk, def)
                && AiCalc.canKnockOut(ctx, def, atk) && AiUtil.benchAlive(battle, atk) == 0) {
            return USE_PROTECT;
        }
        int heal = AiPositiveHelpers.amountToRecover(ctx, atk, def, move);
        if (AiCalc.canKnockOut(ctx, def, atk)) {
            if (!AiCalc.canKnockOutAfterHealing(ctx, def, atk, heal) || AiCalc.takingSecondaryDamage(battle, def)) return USE_PROTECT;
            return USE_STATUS_THEN_PROTECT;
        }
        if (AiCalc.can2HKO(ctx, def, atk)) {
            if (!AiCalc.canKnockOutAfterHealing(ctx, def, atk, heal) || AiCalc.takingSecondaryDamage(battle, def)) return USE_PROTECT;   // 登记: the 2-hit variant of CanKnockOutAfterHealing
            return USE_STATUS_THEN_PROTECT;
        }
        return DONT_PROTECT;
    }

    private static boolean usefulItemToProtectFor(AiCtx ctx, Battler b) {
        String a = b.ability == null ? "" : b.ability;
        if (b.hasActiveItem("TOXICORB") && AiCalc.canBePoisoned(ctx.battle, b, b)
                && (a.equals("POISONHEAL") || a.equals("TOXICBOOST") || a.equals("QUICKFEET") || a.equals("MAGICGUARD")
                    || AiCalc.named(firstNamed(b, "FACADE"), "FACADE"))) return true;
        return b.hasActiveItem("FLAMEORB") && AiCalc.canBeBurned(ctx.battle, b, b)
                && (a.equals("GUTS") || a.equals("FLAREBOOST") || a.equals("MAGICGUARD") || AiCalc.named(firstNamed(b, "FACADE"), "FACADE"));
    }

    private static boolean usefulAbilityToProtectFor(Battler atk, Battler def) {
        String a = atk.ability == null ? "" : atk.ability;
        if (a.equals("SPEEDBOOST")) return AiCalc.speed(atk) <= AiCalc.speed(def) && atk.stage(PBStats.SPEED) < 6;
        if (a.equals("MOODY")) {
            for (int s = PBStats.ATTACK; s <= PBStats.EVASION; s++) if (s != PBStats.HP && atk.stage(s) < 6) return true;
        }
        return false;
    }

    private static BattleMove firstNamed(Battler b, String name) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = b.moveSlot(i);
            if (m != null && AiCalc.named(m, name)) return m;
        }
        return null;
    }

    /** {@code IncreaseFoeProtectionViability} (ai_advanced.c:2842), singles. */
    private static int foeProtection(AiCtx ctx, int viability, int cls, Battler atk, Battler def) {
        if (cls == AiCalc.CLASS_STALL) return inc(viability, trapped(def) ? 7 : 3);
        return incStatus(ctx, viability, cls, 3, atk, def);
    }

    private static int endure(AiCtx ctx, Battler atk, Battler def, int viability, int cls) {
        if (!AiCalc.canKnockOut(ctx, def, atk)) return viability;
        if (atk.hp > atk.maxHp() / 4 && atk.item != null && atk.item.endsWith("BERRY")
                && !atk.item.equals("LEPPABERRY")) {                                               // IsPinchBerryItemEffect (approximated)
            return incStatus(ctx, viability, cls, 3, atk, def);
        }
        if (atk.hp > 1 && AiCalc.moveFunctionInMoveset(atk, "098", "06E")) return incStatus(ctx, viability, cls, 3, atk, def);   // Flail/Reversal, Endeavor
        return viability;
    }

    // ---- pivoting ----

    private static boolean sashOrSturdyOrMultiscale(Battler def, Battler atk) {
        boolean full = def.hp == def.maxHp();
        if (!full) return false;
        if (def.hasActiveItem("FOCUSSASH")) return true;
        if (atk.hasMoldBreaker()) return false;
        return def.hasActiveAbility("STURDY") || def.hasActiveAbility("MULTISCALE") || def.hasActiveAbility("SHADOWSHIELD");
    }

    /** {@code ShouldPivot(bankAtk,bankDef,move,class)} (ai_advanced.c:1468), singles. */
    private static int shouldPivot(AiCtx ctx, Battler atk, Battler def, BattleMove move, int cls) {
        Battle battle = ctx.battle;
        boolean damager = AiCalc.classDamager(cls);
        boolean boost = (anyUsefulOffensiveStatRaised(ctx, atk) && damager) || def.stage(PBStats.EVASION) >= 4;
        if (AiSwitching.significantHazardDamage(battle, atk, 4)) return DONT_PIVOT;
        if (AiUtil.benchAlive(battle, atk) == 0) return CAN_TRY_PIVOT;                             // !HasMonToSwitchTo
        if (ctx.predictedToSwitch(def) && !boost) return PIVOT;
        int sw = AiSwitching.calcMostSuitable(ctx, atk, def).bestFlags;
        boolean damaging = !move.statusMove();
        boolean onlyKo = !AiNegativeEffects.canKnockOutWithoutMove(ctx, move, atk, def);
        boolean defKo = AiCalc.canKnockOut(ctx, def, atk);
        boolean atkKo = AiCalc.canKnockOut(ctx, atk, def);
        if (AiCalc.moveWouldHitFirst(ctx, move, atk, def)) {
            if (onlyKo) {
                if (atkKo) return CAN_TRY_PIVOT;
                if (damager && AiCalc.can2HKO(ctx, atk, def)) {
                    if (defKo) return PIVOT;
                    if (damaging && (((sw & AiSwitching.FLAG_RESIST_ALL_MOVES) != 0 && (sw & (AiSwitching.FLAG_KO_FOE | AiSwitching.FLAG_CAN_REMOVE_HAZARDS)) != 0)
                            || sashOrSturdyOrMultiscale(def, atk))) return PIVOT;
                } else if (!boost) {
                    if (damager && damaging && sashOrSturdyOrMultiscale(def, atk)) return PIVOT;
                    if ((sw & (AiSwitching.FLAG_WALLS_FOE | AiSwitching.FLAG_RESIST_ALL_MOVES)) != 0) {
                        if (hazardsOnOwnSide(atk) && (sw & AiSwitching.FLAG_CAN_REMOVE_HAZARDS) != 0) return PIVOT;
                        if (AiCalc.willFaintFromSecondaryDamage(battle, atk)) return PIVOT;
                        if (damager) return offenceBad(ctx, atk) ? PIVOT : CAN_TRY_PIVOT;
                    }
                }
            }
        } else {                                                                                   // the foe goes first
            if (defKo) return CAN_TRY_PIVOT;                                                       // (Teleport has no singles code here)
            if (AiCalc.can2HKO(ctx, def, atk)) {
                if (atkKo) {
                    if (onlyKo) return CAN_TRY_PIVOT;
                } else if (damager && (sw & AiSwitching.FLAG_KO_FOE) != 0
                        && (sw & (AiSwitching.FLAG_OUTSPEEDS | AiSwitching.FLAG_WALLS_FOE | AiSwitching.FLAG_RESIST_ALL_MOVES)) != 0) {
                    return PIVOT;
                }
            } else if (atkKo) {
                if (onlyKo && !boost) return CAN_TRY_PIVOT;
            } else if (AiCalc.can2HKO(ctx, atk, def)) {
                if (damager && damaging) {
                    if (((sw & AiSwitching.FLAG_KO_FOE) != 0 && (sw & (AiSwitching.FLAG_OUTSPEEDS | AiSwitching.FLAG_WALLS_FOE | AiSwitching.FLAG_RESIST_ALL_MOVES)) != 0)
                            || ((sw & AiSwitching.FLAG_RESIST_ALL_MOVES) != 0 && (sw & AiSwitching.FLAG_CAN_REMOVE_HAZARDS) != 0)
                            || sashOrSturdyOrMultiscale(def, atk)) return PIVOT;
                }
            } else {
                if (damager && (sw & AiSwitching.FLAG_KO_FOE) != 0 && (sw & AiSwitching.FLAG_OUTSPEEDS) != 0) return PIVOT;
                if (!boost && (sw & (AiSwitching.FLAG_OUTSPEEDS | AiSwitching.FLAG_WALLS_FOE | AiSwitching.FLAG_RESIST_ALL_MOVES)) != 0) {
                    if (hazardsOnOwnSide(atk) && (sw & AiSwitching.FLAG_CAN_REMOVE_HAZARDS) != 0) return PIVOT;
                    if (AiCalc.willFaintFromSecondaryDamage(battle, atk)) return PIVOT;
                    if (damager && offenceBad(ctx, atk)) return PIVOT;
                    return CAN_TRY_PIVOT;
                }
            }
        }
        return DONT_PIVOT;
    }

    private static boolean hazardsOnOwnSide(Battler atk) {
        BattleSide s = atk.pbOwnSide();
        return s.effects.truthy(PBEffects.Side.StealthRock) || s.effects.intVal(PBEffects.Side.Spikes) > 0
                || s.effects.intVal(PBEffects.Side.ToxicSpikes) > 0 || s.effects.truthy(PBEffects.Side.StickyWeb);
    }

    /** "Pivot if attacking stats are bad" (:1545 ff). */
    private static boolean offenceBad(AiCtx ctx, Battler atk) {
        boolean phys = AiCalc.physicalMoveInMoveset(ctx, atk);
        boolean spec = AiCalc.specialMoveInMoveset(ctx, atk);
        int min = -AiSwitching.OFFENSIVE_STAT_MIN_NUM;
        if (phys && !spec) return atk.stage(PBStats.ATTACK) <= min;
        if (!phys && spec) return atk.stage(PBStats.SPATK) <= min;
        return phys && atk.stage(PBStats.ATTACK) <= min && atk.stage(PBStats.SPATK) <= min;
    }

    private static boolean anyUsefulOffensiveStatRaised(AiCtx ctx, Battler b) {
        return (b.stage(PBStats.ATTACK) > 0 && AiCalc.physicalMoveInMoveset(ctx, b))
                || (b.stage(PBStats.SPATK) > 0 && AiCalc.specialMoveInMoveset(ctx, b)) || b.stage(PBStats.SPEED) > 0;
    }

    private static int pivot(AiCtx ctx, Battler atk, Battler def, BattleMove move, int viability, int cls) {
        int should = shouldPivot(ctx, atk, def, move, cls);
        if (should == PIVOT) {                                                                     // IncreasePivotViability (ai_advanced.c:2658)
            switch (cls) {
                case AiCalc.CLASS_SWEEPER_KILL: return inc(viability, 3);
                case AiCalc.CLASS_SWEEPER_SETUP_STATS: case AiCalc.CLASS_SWEEPER_SETUP_STATUS: case AiCalc.CLASS_STALL:
                case AiCalc.CLASS_BATON_PASS: return inc(viability, 9);
                case AiCalc.CLASS_CLERIC: case AiCalc.CLASS_SCREENS: case AiCalc.CLASS_SWEEPER_SETUP_SCREENS: return inc(viability, 7);
                case AiCalc.CLASS_PHAZING: return inc(viability, 4);
                case AiCalc.CLASS_ENTRY_HAZARDS: return inc(viability, 3);
                default: return viability;
            }
        }
        if (should == DONT_PIVOT) return Math.max(0, viability - 9);                               // bad idea to use this move
        return viability;
    }
}
