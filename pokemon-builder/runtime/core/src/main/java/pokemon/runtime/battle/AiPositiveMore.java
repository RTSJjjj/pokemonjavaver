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

    private static final int DONT_PROTECT = 0, USE_PROTECT = 1, USE_STATUS_THEN_PROTECT = 2, PROTECT_FROM_FOES = 3, PROTECT_FROM_ALLIES = 4;
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
                    if (AiCalc.classDoublesTeamSupport(cls)) return inc(viability, 15);
                    return incStatus(ctx, viability, cls, 2, atk, def);
                }
                return viability;
            }
            case "10C": return substitute(ctx, atk, def, viability, cls);                          // EFFECT_SUBSTITUTE (:886)
            case "0AA": case "14B": case "14C": case "168": case "0E8": case "0AB": case "0AC": case "14A": case "149":
                return protect(ctx, atk, def, move, viability, cls, atkAbility);                       // EFFECT_PROTECT (:1132)
            case "0EE": case "151":                                                                // EFFECT_BATON_PASS: U-Turn, Volt Switch, Parting Shot (:1496)
                return pivot(ctx, atk, def, move, viability, cls);
            case "0AF": {                                                                          // MOVE_COPYCAT (:150)
                BattleMove copy;
                if (AiCalc.moveWouldHitFirst(ctx, move, atk, def) || predicted == null) copy = AiCalc.globalLastUsedMove(battle);
                else copy = predicted;
                if (copy != null && !copy.function().equals("0AF") && !copy.function().equals("0B0") && !ownUsable(ctx, atk, copy)) {
                    return AiPositives.score(ctx, atk, def, copy, viability);                       // run the logic on the copied move instead
                }
                return viability;
            }
            case "05C": {                                                                          // EFFECT_MIMIC (:890)
                BattleMove lastDef = AiCalc.lastUsedMove(battle, def);
                if (AiCalc.moveWouldHitFirst(ctx, move, atk, def)) {
                    if (lastDef != null && !AiCalc.canKnockOut(ctx, def, atk) && AiCalc.knocksOutXHits(ctx, lastDef, atk, def, 1)) {
                        return incStatus(ctx, viability, cls, 2, atk, def);
                    }
                } else if (predicted != null && !AiCalc.can2HKO(ctx, def, atk) && AiCalc.knocksOutXHits(ctx, predicted, atk, def, 1)) {
                    return incStatus(ctx, viability, cls, 1, atk, def);
                }
                return viability;                                                                  // 登记: the Imprison clauses
            }
            case "0B9": {                                                                          // EFFECT_DISABLE (:935)
                if (def.effects.intVal(PBEffects.Battler.Disable) != 0 || def.hasActiveItem("MENTALHERB")) return viability;
                BattleMove lastDef = AiCalc.lastUsedMove(battle, def);
                if (AiCalc.moveWouldHitFirst(ctx, move, atk, def)) {
                    if (lastDef != null) {
                        if (predicted != null && lastDef.internalName().equals(predicted.internalName())) return incStatus(ctx, viability, cls, 3, atk, def);
                        if (AiCalc.knocksOutXHits(ctx, lastDef, def, atk, 1)) return incStatus(ctx, viability, cls, 2, atk, def);
                    }
                } else if (predicted != null && predicted.statusMove()) {
                    return incStatus(ctx, viability, cls, 1, atk, def);
                }
                return viability;
            }
            case "0BC": {                                                                          // EFFECT_ENCORE (:955)
                if (def.effects.intVal(PBEffects.Battler.Encore) != 0 || def.hasActiveItem("MENTALHERB")) return viability;
                BattleMove lastDef = AiCalc.lastUsedMove(battle, def);
                if (AiCalc.moveWouldHitFirst(ctx, move, atk, def)) {
                    if (lastDef != null && (lastDef.statusMove() || AiCalc.noEffect(battle, def, atk, lastDef))) {
                        return incStatus(ctx, viability, cls, 3, atk, def);                          // lock into status moves
                    }
                } else if (predicted != null && predicted.statusMove()) {
                    return incStatus(ctx, viability, cls, 3, atk, def);
                }
                return viability;
            }
            case "10E": {                                                                          // EFFECT_SPITE (:1030)
                if (predicted != null && AiCalc.moveWouldHitFirst(ctx, move, atk, def)) {
                    for (int i = 0; i < Battler.MOVES_MAX; i++) {
                        BattleMove m = def.moveSlot(i);
                        if (m != null && m.internalName().equals(predicted.internalName())) {
                            if (def.moveSlotPp(i) <= 4) return incStatus(ctx, viability, cls, 3, atk, def);
                            break;
                        }
                    }
                }
                return viability;
            }
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
                return AiPositiveItems.apply(ctx, atk, def, move, viability, cls, atkAbility, defAbility);
        }
    }

    /** {@code MoveInMovesetAndUsable(move,bank)}. */
    private static boolean ownUsable(AiCtx ctx, Battler atk, BattleMove m) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove own = atk.moveSlot(i);
            if (own != null && own.internalName().equals(m.internalName()) && AiCalc.usable(ctx, atk, i)) return true;
        }
        return false;
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
        boolean isDouble = AiDoublesScore.isDouble(battle, atk);
        Battler defPartner = isDouble ? AiDoublesScore.partner(battle, def) : null;
        boolean defPhysical = AiCalc.physicalMoveInMoveset(ctx, def) || (defPartner != null && AiCalc.physicalMoveInMoveset(ctx, defPartner));
        boolean defSpecial = AiCalc.specialMoveInMoveset(ctx, def) || (defPartner != null && AiCalc.specialMoveInMoveset(ctx, defPartner));
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
        if (def.hasStatus("SLEEP") || def.hasStatus("FROZEN")) return true;                       // IsBankIncapacitated
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
            case AiCalc.CLASS_D_SETUP_ATTACKER: case AiCalc.CLASS_D_TRICK_ROOM_ATTACKER: case AiCalc.CLASS_D_TRICK_ROOM_SETUP:
            case AiCalc.CLASS_D_UTILITY: case AiCalc.CLASS_D_PHAZING: case AiCalc.CLASS_D_TEAM_SUPPORT: case AiCalc.CLASS_D_TOTAL_TEAM_SUPPORT:
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

    private static boolean singleBattle(AiCtx ctx, Battler atk) {
        return !AiDoublesScore.isDouble(ctx.battle, atk);                                          // IS_SINGLE_BATTLE
    }

    /** The {@code EFFECT_PROTECT} case of {@code AIScript_Positives} (ai_positives.c:1132-1230). */
    private static int protect(AiCtx ctx, Battler atk, Battler def, BattleMove move, int viability, int cls, String atkAbility) {
        Battle battle = ctx.battle;
        String f = move.function();
        BattleMove predicted = ctx.prediction(def);
        boolean single = singleBattle(ctx, atk);
        int predictedTarget = predicted == null ? -1 : AiCalc.fx(predicted).pbTarget(predicted, def);
        switch (f) {
            case "0AB":                                                                            // Quick Guard
                if (predicted != null && AiCalc.priorityCalc(battle, def, predicted) > 0) {
                    return single ? protectChecks(ctx, atk, def, move, viability, cls) : AiDoublesScore.increaseTeamProtection(viability, cls);
                }
                return viability;
            case "0AC":                                                                            // Wide Guard
                if (predicted != null && (predictedTarget == PBTargets.AllNearOthers || predictedTarget == PBTargets.AllBattlers
                        || predictedTarget == PBTargets.AllNearFoes || predictedTarget == PBTargets.AllFoes)) {
                    return single ? protectChecks(ctx, atk, def, move, viability, cls) : AiDoublesScore.increaseTeamProtection(viability, cls);
                }
                if (!single) {
                    Battler partner = AiDoublesScore.partner(battle, atk);
                    BattleMove partnerMove = partner == null ? null : battle.chosenMove(partner.index);
                    if (partnerMove != null && AiDoublesScore.hitsAll(battle, partner, partnerMove) && !atkAbility.equals("TELEPATHY")
                            && !AiCalc.noEffect(battle, partner, atk, partnerMove)) {
                        return AiDoublesScore.increaseAllyProtection(viability, cls);              // protect against the partner's move
                    }
                }
                return viability;
            case "14A":                                                                            // Crafty Shield
                if (predicted != null && predicted.statusMove() && predictedTarget != PBTargets.User) {
                    return single ? protectChecks(ctx, atk, def, move, viability, cls) : AiDoublesScore.increaseTeamProtection(viability, cls);
                }
                return viability;
            case "149":                                                                            // Mat Block
                if (AiCalc.firstTurn(atk) && predicted != null && !predicted.statusMove() && predictedTarget != PBTargets.User) {
                    return single ? protectChecks(ctx, atk, def, move, viability, cls) : AiDoublesScore.increaseTeamProtection(viability, cls);
                }
                return viability;
            case "0E8":
                return endure(ctx, atk, def, viability, cls);
            case "14B":                                                                            // King's Shield: special logic for Aegislash
                if (atkAbility.equals("STANCECHANGE") && !AiPositiveHelpers.isIncapacitated(def)) {
                    boolean contactKo = predicted != null && AiCalc.has(predicted, 'a') && AiCalc.physicalMoveInMoveset(ctx, def);
                    if ((atk.isSpecies("AEGISLASH") && atk.form() == 1)                              // in blade form
                            || (!single && ctx.simulatedRng[1] < 80)                                // 80% chance of spamming in doubles
                            || AiCalc.classStall(cls) || contactKo) {
                        if (AiCalc.classStall(cls)) {
                            if (atk.hp != atk.maxHp() && atk.hasActiveItem("LEFTOVERS")) return inc(viability, 8);
                            return inc(viability, contactKo ? 8 : 3);
                        } else if (!single) {
                            return inc(viability, 19);
                        }
                        viability = incStatus(ctx, viability, cls, 3, atk, def);
                        if (setupSweeper(cls) && AiCalc.can2HKO(ctx, def, atk)) viability = inc(viability, 3);
                    }
                    return viability;
                }
                return protectChecks(ctx, atk, def, move, viability, cls);
            case "168":                                                                            // Baneful Bunker
                if (predicted != null && AiCalc.has(predicted, 'a') && AiCalc.canBePoisoned(battle, def, atk)) {
                    if (AiCalc.classStall(cls)) return inc(viability, 8);
                    if (!single) return inc(viability, 19);
                }
                return protectChecks(ctx, atk, def, move, viability, cls);
            default:
                return protectChecks(ctx, atk, def, move, viability, cls);
        }
    }

    /** {@code IsClassSetupSweeper}. */
    private static boolean setupSweeper(int cls) {
        return cls == AiCalc.CLASS_SWEEPER_SETUP_STATS || cls == AiCalc.CLASS_SWEEPER_SETUP_STATUS || cls == AiCalc.CLASS_SWEEPER_SETUP_SCREENS;
    }

    /** {@code PROTECT_CHECKS:} (ai_positives.c:1217). */
    private static int protectChecks(AiCtx ctx, Battler atk, Battler def, BattleMove move, int viability, int cls) {
        int should = shouldProtect(ctx, atk, def, move);
        if (should == USE_PROTECT || should == PROTECT_FROM_FOES) return foeProtection(ctx, viability, cls, atk, def);
        if (should == PROTECT_FROM_ALLIES) return AiDoublesScore.increaseAllyProtection(viability, cls);
        return viability;
    }

    /** {@code ShouldProtect(bankAtk,bankDef,move)} (ai_advanced.c:1108), singles: USE_PROTECT / USE_STATUS_THEN_PROTECT count as "protect". */
    private static int shouldProtect(AiCtx ctx, Battler atk, Battler def, BattleMove move) {
        Battle battle = ctx.battle;
        BattleMove predicted = ctx.prediction(def);
        if (AiCalc.willFaintFromSecondaryDamage(battle, atk) && !AiCalc.isMoxie(def.ability == null ? "" : def.ability)) return DONT_PROTECT;
        if (def.hasStatus("SLEEP") || def.hasStatus("FROZEN") || def.effects.intVal(PBEffects.Battler.HyperBeam) > 0) return DONT_PROTECT;   // IsBankIncapacitated
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
        if (!singleBattle(ctx, atk)) {                                                             // Double Battle (:1178)
            Battler partner = AiDoublesScore.partner(battle, atk);
            if (partner != null) {
                BattleMove partnerMove = battle.chosenMove(partner.index);
                if (partnerMove != null && !(atk.ability != null && atk.ability.equals("TELEPATHY"))
                        && AiDoublesScore.hitsAll(battle, partner, partnerMove) && !AiCalc.noEffect(battle, partner, atk, partnerMove)
                        && !absorbs(atk, partnerMove, partner)) {
                    return PROTECT_FROM_ALLIES;                                                    // the partner damages the whole field
                }
                if (partnerMove != null && AiDoublesScore.doubleKillingScore(ctx, partnerMove, partner, def) >= AiDoublesScore.BEST_KO_SCORE - 1) {
                    return PROTECT_FROM_FOES;                                                      // the partner has this covered
                }
            }
            return DONT_PROTECT;
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

    /** {@code IsDamagingMoveUnusable(move,bankAtk,bankDef)} for the absorbing Abilities (Volt Absorb, Storm Drain, ...). */
    private static boolean absorbs(Battler target, BattleMove move, Battler user) {
        String a = target.ability == null ? "" : target.ability;
        String type = AiCalc.fx(move).pbCalcType(move, user);
        if (user.hasMoldBreaker()) return false;
        return (type.equals("ELECTRIC") && (a.equals("VOLTABSORB") || a.equals("MOTORDRIVE") || a.equals("LIGHTNINGROD")))
                || (type.equals("WATER") && (a.equals("WATERABSORB") || a.equals("DRYSKIN") || a.equals("STORMDRAIN")))
                || (type.equals("FIRE") && a.equals("FLASHFIRE")) || (type.equals("GRASS") && a.equals("SAPSIPPER"))
                || (type.equals("GROUND") && a.equals("EARTHEATER"));
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
        switch (cls) {
            case AiCalc.CLASS_STALL: return inc(viability, trapped(def) ? 7 : 3);
            case AiCalc.CLASS_D_ALL_OUT_ATTACKER: return inc(viability, 12);
            case AiCalc.CLASS_D_SETUP_ATTACKER: case AiCalc.CLASS_D_UTILITY: return inc(viability, 10);
            case AiCalc.CLASS_D_TRICK_ROOM_ATTACKER: return inc(viability, 4);
            case AiCalc.CLASS_D_TRICK_ROOM_SETUP: return inc(viability, 7);
            case AiCalc.CLASS_D_PHAZING: return inc(viability, 13);
            case AiCalc.CLASS_D_TEAM_SUPPORT: case AiCalc.CLASS_D_TOTAL_TEAM_SUPPORT: return inc(viability, 1);
            default: return incStatus(ctx, viability, cls, 3, atk, def);
        }
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

    /** {@code MoveSplitOnTeam(bank,SPLIT_PHYSICAL)}: some Pokemon of the side has a physical attack. */
    private static boolean physicalOnTeam(Battle battle, Battler b) {
        for (Battler m : battle.partyOf(b.index)) {
            if (m == null || m.removedFromParty || m.fainted()) continue;
            for (int i = 0; i < Battler.MOVES_MAX; i++) {
                BattleMove mv = m.moveSlot(i);
                if (mv != null && !mv.statusMove() && mv.physical()) return true;
            }
        }
        return false;
    }

    private static int pivot(AiCtx ctx, Battler atk, Battler def, BattleMove move, int viability, int cls) {
        if (AiDoublesScore.isDouble(ctx.battle, atk)) {                                           // Double Battle (:1525)
            if (AiUtil.benchAlive(ctx.battle, atk) == 0) return viability;                         // can't switch
            if (atk.pokemon != null && "INTIMIDATE".equals(atk.pokemon.ability) && physicalOnTeam(ctx.battle, def)) {
                if (cls == AiCalc.CLASS_D_UTILITY) return inc(viability, 16);
                if (cls == AiCalc.CLASS_D_ALL_OUT_ATTACKER) return inc(viability, 18);
            }
            return viability;
        }
        int should = shouldPivot(ctx, atk, def, move, cls);
        if (should == PIVOT) {                                                                     // IncreasePivotViability (ai_advanced.c:2658)
            switch (cls) {
                case AiCalc.CLASS_SWEEPER_KILL: return inc(viability, 3);
                case AiCalc.CLASS_SWEEPER_SETUP_STATS: case AiCalc.CLASS_SWEEPER_SETUP_STATUS: case AiCalc.CLASS_STALL:
                case AiCalc.CLASS_BATON_PASS: return inc(viability, 9);
                case AiCalc.CLASS_CLERIC: case AiCalc.CLASS_SCREENS: case AiCalc.CLASS_SWEEPER_SETUP_SCREENS: return inc(viability, 7);
                case AiCalc.CLASS_PHAZING: return inc(viability, 4);
                case AiCalc.CLASS_ENTRY_HAZARDS: return inc(viability, 3);
                case AiCalc.CLASS_D_ALL_OUT_ATTACKER: case AiCalc.CLASS_D_SETUP_ATTACKER: return inc(viability, 18);
                case AiCalc.CLASS_D_TRICK_ROOM_ATTACKER: case AiCalc.CLASS_D_TRICK_ROOM_SETUP: case AiCalc.CLASS_D_UTILITY: return inc(viability, 16);
                case AiCalc.CLASS_D_PHAZING: return inc(viability, 14);
                case AiCalc.CLASS_D_TEAM_SUPPORT: case AiCalc.CLASS_D_TOTAL_TEAM_SUPPORT: return inc(viability, 15);
                default: return viability;
            }
        }
        if (should == DONT_PIVOT) return Math.max(0, viability - 9);                               // bad idea to use this move
        return viability;
    }
}
