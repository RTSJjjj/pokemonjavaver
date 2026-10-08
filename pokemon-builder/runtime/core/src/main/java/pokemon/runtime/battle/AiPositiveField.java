package pokemon.runtime.battle;

import static pokemon.runtime.battle.AiPositiveHelpers.inc;
import static pokemon.runtime.battle.AiPositiveHelpers.incStatus;

/**
 * ai_positives.c: the field / team / item / disruption cases that are mostly doubles-aware - Follow Me (:1855), Trick Room and
 * the other room, Gravity, Ion Deluge and Court Change moves (:2423-2475), Powder / Telekinesis / Throat Chop / Heal Block / Embargo
 * (:2540-2575), Soak (:2578), Topsy-Turvy / Electrify (:2594), Fairy Lock (:2615), terrain (:2385), Pledge (:2410), Instruct /
 * After You / Quash (:2634), Tailwind / Lucky Chant / Magnet Rise (:2658), Flame Burst (:2699), Sky Drop (:2710), Bug Bite /
 * Incinerate / Smack Down / Clear Smog (:2323-2345), Feint (:2538).
 *
 * <p>登记: Fling (:2468, its item table {@code gFlingTable} was not exported), Z-Moves, Dynamax, Camouflage (:2688, its
 * condition needs a status move to be damaging, so it never applies), Happy Hour / Hold Hands / Celebrate (Z-move only).</p>
 */
final class AiPositiveField {

    private AiPositiveField() {
    }

    static int apply(AiCtx ctx, Battler atk, Battler def, BattleMove move, int viability, int cls, String atkAbility, String defAbility) {
        Battle battle = ctx.battle;
        String f = move.function();
        BattleMove predicted = ctx.prediction(def);
        boolean isDouble = AiDoublesScore.isDouble(battle, atk);
        Battler partner = AiDoublesScore.partner(battle, atk);
        switch (f) {
            case "117":                                                                            // EFFECT_FOLLOW_ME / Rage Powder (:1855)
                if (isDouble && partner != null && !AiPositiveHelpers.isIncapacitated(def)
                        && (!AiCalc.named(move, "RAGEPOWDER") || def.affectedByPowder(false))) {
                    BattleMove onPartner = AiMaster.predictionOf(ctx, def, partner);
                    if (onPartner != null && !onPartner.statusMove()) {
                        return cls == AiCalc.CLASS_D_UTILITY ? inc(viability, 16) : incStatus(ctx, viability, cls, 3, atk, def);
                    }
                }
                return viability;
            case "11F": {                                                                          // MOVE_TRICKROOM (:2423)
                if (!AiCalc.trickRoom(battle) && sideSpeedAverage(battle, atk) < sideSpeedAverage(battle, def)) {
                    boolean trickRoomer = cls == AiCalc.CLASS_D_TRICK_ROOM_ATTACKER || cls == AiCalc.CLASS_D_TRICK_ROOM_SETUP;
                    return trickRoomer ? inc(viability, 19) : incStatus(ctx, viability, cls, 3, atk, def);
                }
                return viability;
            }
            case "118": {                                                                          // MOVE_GRAVITY (:2440)
                boolean levitates = atkAbility.equals("LEVITATE") || atk.effects.intVal(PBEffects.Battler.MagnetRise) > 0;
                if (battle.field.effects.intVal(PBEffects.Field.Gravity) == 0) {
                    if (sleepMoveWithLowAccuracy(ctx, atk, def)) return AiPositiveHelpers.incSleep(ctx, viability, cls, atk, def, move);
                    if (!levitates) return incStatus(ctx, viability, cls, moveWithAccuracyBelow(ctx, atk, def, 90) ? 2 : 1, atk, def);
                } else if (levitates) {
                    return incStatus(ctx, viability, cls, 2, atk, def);                           // undo the Gravity
                }
                return viability;
            }
            case "146":                                                                            // MOVE_IONDELUGE (:2463)
                if ((atkAbility.equals("VOLTABSORB") || atkAbility.equals("MOTORDRIVE") || atkAbility.equals("LIGHTNINGROD"))
                        && predicted != null && AiCalc.fx(predicted).pbCalcType(predicted, def).equals("NORMAL")) {
                    return incStatus(ctx, viability, cls, 2, atk, def);
                }
                return viability;
            case "17A":                                                                            // MOVE_COURTCHANGE (:2468)
                if (atk.foe != def.foe && shouldCourtChange(atk, def) && !shouldCourtChange(def, atk)) return incStatus(ctx, viability, cls, 2, atk, def);
                return viability;
            case "148":                                                                            // MOVE_POWDER (:2541)
                if (predicted != null && !predicted.statusMove() && AiCalc.fx(predicted).pbCalcType(predicted, def).equals("FIRE")) {
                    return incStatus(ctx, viability, cls, 3, atk, def);
                }
                return viability;
            case "11A":                                                                            // MOVE_TELEKINESIS (:2547)
                return moveWithAccuracyBelow(ctx, atk, def, 90) ? incStatus(ctx, viability, cls, 1, atk, def) : viability;
            case "16C":                                                                            // MOVE_THROATCHOP (:2552)
                if (AiCalc.moveWouldHitFirst(ctx, move, atk, def) && predicted != null && AiCalc.has(predicted, 'k')) {
                    return incStatus(ctx, viability, cls, 3, atk, def);
                } else if (isDouble) {
                    return AiDoublesScore.increaseDamageToScore(ctx, viability, cls, 5, atk, def);
                } else if (AiCalc.classSweeper(cls) && soundMoveInMoveset(def)) {
                    return inc(viability, 3);
                }
                return viability;
            case "0BB":                                                                            // Heal Block (:2565)
                if (AiCalc.moveWouldHitFirst(ctx, move, atk, def) && AiCalc.movePredictionIsHealing(ctx, def)) return incStatus(ctx, viability, cls, 3, atk, def);
                if (healingMoveInMoveset(def) || def.hasActiveItem("LEFTOVERS") || (def.hasActiveItem("BLACKSLUDGE") && def.hasType("POISON"))) {
                    return incStatus(ctx, viability, cls, 2, atk, def);
                }
                return viability;
            case "0F8":                                                                            // MOVE_EMBARGO (:2535)
                return def.item != null && !def.item.isEmpty() ? incStatus(ctx, viability, cls, 1, atk, def) : viability;
            case "061":                                                                            // MOVE_SOAK (:2578)
                if (AiCalc.classSweeper(cls) && (AiCalc.damagingTypeInMoveset(ctx, atk, "ELECTRIC") || AiCalc.damagingTypeInMoveset(ctx, atk, "GRASS"))) {
                    return incStatus(ctx, viability, cls, 2, atk, def);
                }
                return viability;
            case "142": case "143":                                                                // Trick-or-Treat, Forest's Curse
                return defAbility.equals("WONDERGUARD") ? incStatus(ctx, viability, cls, 2, atk, def) : viability;
            case "145":                                                                            // MOVE_ELECTRIFY (:2599)
                if (predicted != null && AiCalc.fx(predicted).pbCalcType(predicted, def).equals("NORMAL")
                        && (atkAbility.equals("VOLTABSORB") || atkAbility.equals("MOTORDRIVE") || atkAbility.equals("LIGHTNINGROD"))) {
                    return incStatus(ctx, viability, cls, 3, atk, def);
                }
                return viability;
            case "141": {                                                                          // Topsy-Turvy
                int pos = 0, neg = 0;
                for (int s = PBStats.ATTACK; s <= PBStats.EVASION; s++) {
                    if (def.stage(s) > 0) pos += def.stage(s);
                    else if (def.stage(s) < 0) neg -= def.stage(s);
                }
                return pos > neg ? incStatus(ctx, viability, cls, 1, atk, def) : viability;
            }
            case "152":                                                                            // MOVE_FAIRYLOCK (:2616)
                if (!AiPositiveEffects.trappedPublic(def) && shouldTrap(ctx, atk, def, move, cls)) return inc(viability, 7);
                return viability;
            case "154": case "155": case "156": case "173": {                                      // EFFECT_SET_TERRAIN (:2381)
                if (atk.effects.intVal(PBEffects.Battler.Yawn) > 0 && !atk.airborne() && (f.equals("154") || f.equals("156"))) {
                    viability = AiPositiveEffects.increaseFakeOut(ctx, viability, cls, atk, def, move);   // stop yourself from falling asleep
                }
                if (isDouble) {
                    if (cls == AiCalc.CLASS_D_SETUP_ATTACKER) return inc(viability, 17);
                    boolean partnerExpanding = partner != null && namedUsable(ctx, partner, "EXPANDINGFORCE");
                    if (AiCalc.classDoublesTeamSupport(cls) && partnerExpanding) return inc(viability, 15);
                    if (cls == AiCalc.CLASS_D_UTILITY && partnerExpanding) return inc(viability, 15);
                }
                return incStatus(ctx, viability, cls, 2, atk, def);
            }
            case "106": case "107": case "108":                                                    // EFFECT_PLEDGE (:2410)
                if (isDouble && partner != null && AiCalc.moveFunctionInMoveset(partner, "106", "107", "108")) return inc(viability, 3);
                return viability;
            case "11E":                                                                            // MOVE_QUASH (:2634)
                if (isDouble && partner != null) {
                    BattleMove pm = battle.chosenMove(partner.index);
                    if (pm == null || !AiCalc.wouldHitBefore(battle, pm, partner, predicted, def)) {
                        return AiCalc.classDoublesTeamSupport(cls) ? inc(viability, 7) : incStatus(ctx, viability, cls, 1, atk, def);
                    }
                }
                return viability;
            case "05B":                                                                            // MOVE_TAILWIND (:2658)
                return AiDoublesScore.increaseTailwind(ctx, viability, cls, atk, def);
            case "0A1":                                                                            // MOVE_LUCKYCHANT (:2662)
                if (!isDouble) return incStatus(ctx, viability, cls, 1, atk, def);
                if (AiCalc.classDoublesTeamSupport(cls) && AiDoublesScore.foes(battle, atk).size() > 1) return inc(viability, 8);
                return viability;
            case "119": {                                                                          // MOVE_MAGNETRISE (:2668)
                if (!atk.airborne() && damagingMoveTypeInMoveset(ctx, def, "GROUND")) {
                    BattleMove eq = AiCalc.moveByName(battle, "EARTHQUAKE");
                    if (eq != null && AiCalc.calcDmg(battle, def, atk, eq).typeMod >= 8) {         // it does not resist the Ground move
                        boolean predictedGround = predicted != null && AiCalc.fx(predicted).pbCalcType(predicted, def).equals("GROUND");
                        if (AiCalc.moveWouldHitFirst(ctx, move, atk, def) && predictedGround) return incStatus(ctx, viability, cls, 3, atk, def);
                        return incStatus(ctx, viability, cls, predictedGround ? 2 : 1, atk, def);
                    }
                }
                return viability;
            }
            case "074": {                                                                          // EFFECT_FLAMEBURST (:2699)
                Battler defPartner = AiDoublesScore.partner(battle, def);
                if (isDouble && defPartner != null && AiCalc.healthPercent(defPartner) < 12 && !defPartner.hasActiveAbility("MAGICGUARD")
                        && !defPartner.hasType("FIRE")) {
                    return AiDoublesScore.increaseDamageToScore(ctx, viability, cls, 10, atk, def);
                }
                return viability;
            }
            case "0CE":                                                                            // EFFECT_SKY_DROP (:2710)
                if (!isDouble) {
                    if (AiCalc.classSweeper(cls) && AiCalc.takingSecondaryDamage(battle, def)) return inc(viability, 3);
                } else if (AiCalc.takingSecondaryDamage(battle, def)) {
                    return AiDoublesScore.increaseDamageToScore(ctx, viability, cls, 5, atk, def);
                }
                return viability;
            case "0F4": case "0F5":                                                                // EFFECT_EAT_BERRY: Bug Bite, Pluck, Incinerate (:2323)
                if (def.effects.intVal(PBEffects.Battler.Substitute) > 0 || defAbility.equals("STICKYHOLD")) return viability;
                if (isBerry(def) || (AiCalc.named(move, "INCINERATE") && def.item != null && def.item.endsWith("GEM"))) return inc(viability, 3);
                return viability;
            case "11C":                                                                            // EFFECT_SMACK_DOWN (:2338)
                return def.airborne() ? inc(viability, 3) : viability;
            case "050":                                                                            // Clear Smog: EFFECT_REMOVE_TARGET_STAT_CHANGES (:2347)
                if (!AiCalc.blockedBySubstitute(move, atk, def) && AiPositiveHelpers.shouldPhaze(ctx, atk, def, move, cls)) {
                    if (cls == AiCalc.CLASS_D_PHAZING) return inc(viability, 16);
                    if (AiCalc.classPhazer(cls)) return inc(viability, 8);
                    if (cls == AiCalc.CLASS_D_SETUP_ATTACKER) return inc(viability, 12);
                    return incStatus(ctx, viability, cls, 2, atk, def);
                }
                return viability;
            case "0BD": case "0BE": case "0BF": case "0C0": case "193": {                          // EFFECT_MULTI_HIT / TRIPLE_KICK / DOUBLE_HIT (:612)
                BattleMove strongest = AiCalc.strongestMove(ctx, atk, def);
                boolean strongestMulti = strongest != null && AiCalc.oneOf(strongest, "0BD", "0BF", "0C0");
                if (!isDouble && AiCalc.classSweeper(cls) && !AiCalc.isStrongestMove(ctx, move, atk, def) && !strongestMulti
                        && (AiCalc.blockedBySubstitute(move, atk, def) || atk.hasActiveItem(new String[] {"KINGSROCK", "RAZORFANG"}))) {
                    return inc(viability, 3);                                                      // move past the strongest move
                } else if (f.equals("193") && !atkAbility.equals("CONTRARY") && AiPositiveHelpers.goodIdeaToRaiseSpeed(ctx, atk, def, 1)) {
                    return AiPositiveHelpers.incStat(ctx, viability, cls, 3, atk, def, move, PBStats.SPEED, 2);   // Scale Shot: AI_SPEED_PLUS_FULL
                }
                return viability;
            }
            case "150": {                                                                          // EFFECT_ATTACK_UP_HIT: Fell Stinger (:1670)
                if (atkAbility.equals("CONTRARY")) return viability;
                if (AiCalc.statCanRise(atk, PBStats.ATTACK) && AiCalc.knocksOutXHits(ctx, move, atk, def, 1)) {
                    if (!isDouble) return inc(viability, AiCalc.moveWouldHitFirst(ctx, move, atk, def) ? 9 : 3);
                    return AiDoublesScore.increaseDamageToScore(ctx, viability, cls, 6, atk, def);
                }
                if (AiCalc.secondaryEffectChance(move, atk) >= 75 && AiPositiveHelpers.goodIdeaToRaiseAttack(ctx, atk, def, 1)) {
                    return AiPositiveHelpers.incStat(ctx, viability, cls, 2, atk, def, move, PBStats.ATTACK, 2);
                }
                return viability;
            }
            case "0AD":                                                                            // EFFECT_FEINT (:2538)
                return predicted != null && AiCalc.oneOf(predicted, AiCalc.PROTECT) ? inc(viability, 3) : viability;
            default:
                return viability;
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** {@code GetPokemonOnSideSpeedAverage(bank)} (ai_util.c:2043). */
    static int sideSpeedAverage(Battle battle, Battler b) {
        int total = 0;
        int n = 0;
        if (!b.fainted()) {
            total += AiCalc.speed(b);
            n++;
        }
        Battler partner = AiDoublesScore.partner(battle, b);
        if (!battle.singleBattle() && partner != null) {
            total += AiCalc.speed(partner);
            n++;
        }
        return n == 0 ? 0 : total / n;
    }

    private static boolean isBerry(Battler b) {
        return b.item != null && b.item.endsWith("BERRY");
    }

    private static boolean namedUsable(AiCtx ctx, Battler b, String name) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = b.moveSlot(i);
            if (m != null && AiCalc.named(m, name) && AiCalc.usable(ctx, b, i)) return true;
        }
        return false;
    }

    private static boolean damagingMoveTypeInMoveset(AiCtx ctx, Battler b, String type) {
        return AiCalc.damagingTypeInMoveset(ctx, b, type);
    }

    private static boolean soundMoveInMoveset(Battler b) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = b.moveSlot(i);
            if (m != null && AiCalc.has(m, 'k')) return true;
        }
        return false;
    }

    private static boolean healingMoveInMoveset(Battler b) {
        return AiCalc.moveFunctionInMoveset(b, "0D5", "0D6", "0D7", "0D8", "0DD", "0DE", "114");
    }

    private static boolean sleepMoveWithLowAccuracy(AiCtx ctx, Battler atk, Battler def) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = atk.moveSlot(i);
            if (m != null && AiCalc.usable(ctx, atk, i) && AiCalc.oneOf(m, "003", "004") && AiCalc.hitChance(ctx.battle, atk, def, m) < 90) return true;
        }
        return false;
    }

    private static boolean moveWithAccuracyBelow(AiCtx ctx, Battler atk, Battler def, int acc) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = atk.moveSlot(i);
            if (m != null && !m.statusMove() && AiCalc.usable(ctx, atk, i) && AiCalc.hitChance(ctx.battle, atk, def, m) < acc) return true;
        }
        return false;
    }

    /** {@code ShouldTrap(bankAtk,bankDef,move,class)} (ai_advanced.c:831). */
    static boolean shouldTrap(AiCtx ctx, Battler atk, Battler def, BattleMove move, int cls) {
        if (AiCalc.willFaintFromSecondaryDamage(ctx.battle, atk)) return false;
        if (!AiCalc.classStall(cls)) return false;
        if (AiUtil.benchAlive(ctx.battle, def) == 0) return false;                                 // !HasMonToSwitchTo(bankDef)
        if (AiCalc.moveWouldHitFirst(ctx, move, atk, def)) return !AiCalc.canKnockOut(ctx, def, atk);
        return !AiCalc.canKnockOut(ctx, def, atk) && !AiCalc.can2HKO(ctx, def, atk);
    }

    /** {@code ShouldCourtChange(bankAtk,bankDef)} (ai_advanced.c:1673): swapping the side conditions would help {@code atk}. */
    static boolean shouldCourtChange(Battler atk, Battler def) {
        BattleSide a = atk.pbOwnSide();
        BattleSide d = def.pbOwnSide();
        return goodIdeaToSwap(a, d, PBEffects.Side.SeaOfFire, false) || goodIdeaToSwap(a, d, PBEffects.Side.Swamp, false)
                || goodIdeaToSwap(a, d, PBEffects.Side.Rainbow, true) || goodIdeaToSwap(a, d, PBEffects.Side.LuckyChant, true)
                || goodIdeaToSwap(a, d, PBEffects.Side.Tailwind, true) || goodIdeaToSwap(a, d, PBEffects.Side.AuroraVeil, true)
                || goodIdeaToSwap(a, d, PBEffects.Side.Reflect, true) || goodIdeaToSwap(a, d, PBEffects.Side.LightScreen, true)
                || goodIdeaToSwap(a, d, PBEffects.Side.Safeguard, true) || goodIdeaToSwap(a, d, PBEffects.Side.Mist, true)
                || goodIdeaToSwap(a, d, PBEffects.Side.Spikes, false) || goodIdeaToSwap(a, d, PBEffects.Side.StealthRock, false)
                || goodIdeaToSwap(a, d, PBEffects.Side.StickyWeb, false);
    }

    private static boolean goodIdeaToSwap(BattleSide atk, BattleSide def, int effect, boolean wantToKeep) {
        int ta = atk.effects.intVal(effect);
        int td = def.effects.intVal(effect);
        return wantToKeep ? ta == 0 && td != 0 : td == 0 && ta != 0;
    }
}
