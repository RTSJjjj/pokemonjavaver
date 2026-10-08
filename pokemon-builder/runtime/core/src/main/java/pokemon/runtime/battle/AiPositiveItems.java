package pokemon.runtime.battle;

import static pokemon.runtime.battle.AiPositiveHelpers.inc;
import static pokemon.runtime.battle.AiPositiveHelpers.incStatus;

/**
 * ai_positives.c (singles) part 5, the cases that read items, abilities or whole-party data: Wish and Heal Bell
 * ({@code ShouldUseWishAromatherapy}, ai_advanced.c:1262), Thief/Covet (:1045), Trick/Bestow (:1912), the Skill Swap family (:2110), the
 * stat swap/split family (:2200), Psych Up / Spectral Thief (:1719), Imprison, Refresh, Mud/Water Sport, Magic/Wonder Room, Trick Room.
 *
 * <p>Item "hold effects" are the item's own name here (CHOICEBAND/SPECS/SCARF, TOXICORB, FLAMEORB, BLACKSLUDGE, IRONBALL, LAGGINGTAIL,
 * STICKYBARB); everything else counts as CFRU's {@code default} branch. 登记: Utility Umbrella / Eject Button / Assault Vest branches of Trick,
 * Role Play ("To do" in the source), Psycho Shift's status hand-over, doubles.</p>
 */
final class AiPositiveItems {

    private AiPositiveItems() {
    }

    private static final int ATK = PBStats.ATTACK, SPA = PBStats.SPATK;

    static int apply(AiCtx ctx, Battler atk, Battler def, BattleMove move, int viability, int cls, String atkAbility, String defAbility) {
        Battle battle = ctx.battle;
        String f = move.function();
        switch (f) {
            case "0D7":                                                                            // EFFECT_WISH (:2021)
                if (shouldUseWishAromatherapy(ctx, atk, def, move, cls)) return inc(viability, 8);
                return AiPositiveEffects.recover(ctx, atk, def, move, viability, cls);
            case "019":                                                                            // EFFECT_HEAL_BELL (:1040)
                return shouldUseWishAromatherapy(ctx, atk, def, move, cls) ? inc(viability, 7) : viability;
            case "0F1": return thief(ctx, atk, def, viability, cls, defAbility);                  // EFFECT_THIEF (:1045)
            case "0F2": case "0F3": return trick(ctx, atk, def, move, viability, cls);            // EFFECT_TRICK + Bestow (:1912)
            case "063": case "064": case "068": {                                                  // Simple Beam, Worry Seed, Gastro Acid (:2110)
                if (AiAbilityRatings.of(defAbility) >= 5) return incStatus(ctx, viability, cls, 2, atk, def);
                return viability;
            }
            case "066":                                                                            // MOVE_ENTRAINMENT
                if ((AiAbilityRatings.of(defAbility) >= 5 || AiAbilityRatings.of(atkAbility) <= 0)
                        && !defAbility.equals(atkAbility) && !def.effects.truthy(PBEffects.Battler.GastroAcid)) {
                    return incStatus(ctx, viability, cls, 2, atk, def);
                }
                return viability;
            case "067":                                                                            // MOVE_SKILLSWAP
                return AiAbilityRatings.of(defAbility) > AiAbilityRatings.of(atkAbility) ? incStatus(ctx, viability, cls, 1, atk, def) : viability;
            case "0B8": {                                                                          // EFFECT_IMPRISON (:2135)
                BattleMove predicted = ctx.prediction(def);
                boolean known = predicted != null && ownMove(atk, predicted);
                return incStatus(ctx, viability, cls, known ? 3 : 1, atk, def);
            }
            case "018": return atk.statused() ? incStatus(ctx, viability, cls, 3, atk, def) : viability;   // EFFECT_REFRESH (:2144): default branch
            case "09D":                                                                            // EFFECT_MUD_SPORT (:2262)
                return AiCalc.damagingTypeInMoveset(ctx, def, "ELECTRIC") && !AiCalc.damagingTypeInMoveset(ctx, atk, "ELECTRIC")
                        ? incStatus(ctx, viability, cls, 1, atk, def) : viability;
            case "09E":                                                                            // EFFECT_WATER_SPORT (:2269)
                return AiCalc.damagingTypeInMoveset(ctx, def, "FIRE") && !AiCalc.damagingTypeInMoveset(ctx, atk, "FIRE")
                        ? incStatus(ctx, viability, cls, 1, atk, def) : viability;
            case "052": case "053": case "054": case "057": case "058": case "059": case "161":     // EFFECT_STAT_SWAP_SPLIT (:2276)
                return statSwapSplit(ctx, atk, def, move, viability, cls);
            case "055": return psychUp(ctx, atk, def, move, viability, cls);                      // EFFECT_PSYCH_UP (:1719)
            case "15D": {                                                                          // Spectral Thief (:1721)
                if (atkAbility.equals("CONTRARY")) return viability;
                boolean high = false;
                for (int s = PBStats.ATTACK; s <= PBStats.EVASION; s++) if (def.stage(s) >= 2) high = true;
                if (!high) return viability;
                if (AiCalc.classDamager(cls)) return AiPositiveHelpers.incStat(ctx, viability, cls, 3, atk, def, move, AiPositiveHelpers.ALL_STATS, 6);
                return inc(viability, 3);
            }
            case "0F9": return incStatus(ctx, viability, cls, 1, atk, def);                       // MOVE_MAGICROOM
            case "124": {                                                                          // MOVE_WONDERROOM
                if ((AiCalc.physicalMoveInMoveset(ctx, def) && atk.defense() < atk.spDef())
                        || (AiCalc.specialMoveInMoveset(ctx, def) && atk.spDef() < atk.defense())) return incStatus(ctx, viability, cls, 2, atk, def);
                return viability;
            }
            default: return AiPositiveField.apply(ctx, atk, def, move, viability, cls, atkAbility, defAbility);
        }
    }

    private static boolean ownMove(Battler atk, BattleMove m) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove own = atk.moveSlot(i);
            if (own != null && own.internalName().equals(m.internalName())) return true;
        }
        return false;
    }

    // ---- ShouldUseWishAromatherapy (ai_advanced.c:1262) ----

    static boolean shouldUseWishAromatherapy(AiCtx ctx, Battler atk, Battler def, BattleMove move, int cls) {
        Battle battle = ctx.battle;
        boolean wish = move.function().equals("0D7");
        if (wish && AiUtil.benchAlive(battle, atk) == 0) {
            if ((AiCalc.can2HKO(ctx, def, atk) && !AiCalc.moveFunctionInMoveset(atk, AiCalc.PROTECT)) || AiCalc.willFaintFromSecondaryDamage(battle, atk)) return false;
        }
        if (AiCalc.canKnockOut(ctx, atk, def) && AiUtil.benchAlive(battle, def) == 0) return false;
        boolean needHealing = false;
        boolean hasStatus = false;
        for (Battler m : battle.partyOf(atk.index)) {
            if (m == null || m.pokemon == null || m.pokemon.egg || m.fainted()) continue;
            if (m.hp * 100 / m.maxHp() < 65) needHealing = true;
            if (m.statused() && !(move.function().equals("019") && "SOUNDPROOF".equals(m.ability))) hasStatus = true;
        }
        if (cls == AiCalc.CLASS_CLERIC) {
            if (wish) return needHealing;
            if (move.function().equals("019")) return hasStatus;
        }
        return false;
    }

    // ---- items ----

    private static String item(Battler b) {
        return b.item == null ? "" : b.item;
    }

    private static boolean choiceItem(String it) {
        return it.equals("CHOICEBAND") || it.equals("CHOICESPECS") || it.equals("CHOICESCARF");
    }

    /** {@code MoveSplitInMoveset(bank,split)} for the split a Choice item boosts: Band physical, Specs special. */
    private static boolean choiceSplitInMoveset(AiCtx ctx, String it, Battler b) {
        return it.equals("CHOICEBAND") ? AiCalc.physicalMoveInMoveset(ctx, b) : AiCalc.specialMoveInMoveset(ctx, b);
    }

    private static boolean has(Battler b, String move) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = b.moveSlot(i);
            if (m != null && m.internalName().equals(move)) return true;
        }
        return false;
    }

    /** {@code GoodIdeaToPoisonSelf(bankAtk)} (ai_util.c:2903). */
    static boolean goodIdeaToPoisonSelf(AiCtx ctx, Battler a) {
        String ab = a.ability == null ? "" : a.ability;
        return AiCalc.canBePoisoned(ctx.battle, a, a)
                && (ab.equals("MARVELSCALE") || ab.equals("POISONHEAL") || ab.equals("QUICKFEET") || ab.equals("MAGICGUARD")
                || (ab.equals("TOXICBOOST") && AiCalc.physicalMoveInMoveset(ctx, a)) || (ab.equals("GUTS") && AiCalc.physicalMoveInMoveset(ctx, a))
                || has(a, "FACADE") || AiCalc.moveFunctionInMoveset(a, "01B"));
    }

    /** {@code GoodIdeaToBurnSelf(bankAtk)} (ai_util.c:2973). */
    static boolean goodIdeaToBurnSelf(AiCtx ctx, Battler a) {
        String ab = a.ability == null ? "" : a.ability;
        return AiCalc.canBeBurned(ctx.battle, a, a)
                && (ab.equals("QUICKFEET") || ab.equals("HEATPROOF") || ab.equals("MAGICGUARD")
                || (ab.equals("FLAREBOOST") && AiCalc.specialMoveInMoveset(ctx, a)) || (ab.equals("GUTS") && AiCalc.physicalMoveInMoveset(ctx, a))
                || has(a, "FACADE") || AiCalc.moveFunctionInMoveset(a, "01B"));
    }

    private static int thief(AiCtx ctx, Battler atk, Battler def, int viability, int cls, String defAbility) {
        String di = item(def);
        if (!item(atk).isEmpty() || di.isEmpty() || def.unlosableItem(di) || atk.unlosableItem(di) || has(atk, "ACROBATICS")
                || defAbility.equals("STICKYHOLD") || !AiCalc.classSweeper(cls)) return viability;
        if (choiceItem(di)) {
            return di.equals("CHOICESCARF") || choiceSplitInMoveset(ctx, di, atk) ? incStatus(ctx, viability, cls, 2, atk, def) : viability;
        }
        switch (di) {
            case "TOXICORB": return goodIdeaToPoisonSelf(ctx, atk) ? incStatus(ctx, viability, cls, 2, atk, def) : viability;
            case "FLAMEORB": return goodIdeaToBurnSelf(ctx, atk) ? incStatus(ctx, viability, cls, 2, atk, def) : viability;
            case "BLACKSLUDGE": return atk.pbHasType("POISON") ? incStatus(ctx, viability, cls, 2, atk, def) : viability;
            case "IRONBALL": return has(atk, "FLING") ? incStatus(ctx, viability, cls, 2, atk, def) : viability;
            case "LAGGINGTAIL": case "STICKYBARB": return viability;
            default: return inc(viability, 1);
        }
    }

    private static int trick(AiCtx ctx, Battler atk, Battler def, BattleMove move, int viability, int cls) {
        String ai = item(atk);
        String di = item(def);
        String atkAbility = atk.ability == null ? "" : atk.ability;
        if (choiceItem(ai)) {
            if (ai.equals("CHOICESCARF") || !choiceSplitInMoveset(ctx, ai, def)) return incStatus(ctx, viability, cls, 2, atk, def);
            return viability;
        }
        switch (ai) {
            case "TOXICORB":
                return !goodIdeaToPoisonSelf(ctx, atk) && !AiPositiveEffects.badIdeaToPoison(ctx, def, atk) ? incStatus(ctx, viability, cls, 2, atk, def) : viability;
            case "FLAMEORB":
                return !goodIdeaToBurnSelf(ctx, atk) && !AiPositiveEffects.badIdeaToBurn(ctx, def, atk) ? incStatus(ctx, viability, cls, 2, atk, def) : viability;
            case "BLACKSLUDGE": return !def.pbHasType("POISON") ? incStatus(ctx, viability, cls, 3, atk, def) : viability;
            case "IRONBALL": return !has(def, "FLING") || !def.airborne() ? incStatus(ctx, viability, cls, 2, atk, def) : viability;
            case "LAGGINGTAIL": case "STICKYBARB": return incStatus(ctx, viability, cls, 3, atk, def);
            default: break;
        }
        if (!move.function().equals("0F3") && ai.isEmpty() && !di.isEmpty()) {                     // not Bestow, attacker holds nothing
            if (choiceItem(di)) return viability;
            switch (di) {
                case "TOXICORB": return goodIdeaToPoisonSelf(ctx, atk) ? incStatus(ctx, viability, cls, 2, atk, def) : viability;
                case "FLAMEORB": return goodIdeaToBurnSelf(ctx, atk) ? incStatus(ctx, viability, cls, 2, atk, def) : viability;
                case "BLACKSLUDGE": return atk.pbHasType("POISON") ? incStatus(ctx, viability, cls, 3, atk, def) : viability;
                case "IRONBALL": return has(atk, "FLING") ? incStatus(ctx, viability, cls, 2, atk, def) : viability;
                case "LAGGINGTAIL": case "STICKYBARB": return viability;
                default: return incStatus(ctx, viability, cls, 1, atk, def);
            }
        }
        return viability;
    }

    // ---- stat swaps ----

    /** {@code GoodIdeaToSwapStatStages(bankAtk,bankDef)} (ai_util.c:3426). */
    static boolean goodIdeaToSwapStatStages(AiCtx ctx, Battler atk, Battler def) {
        int good = AiPositiveHelpers.countUsefulStatChanges(ctx, def, atk, def, false) + AiPositiveHelpers.countUsefulStatChanges(ctx, atk, atk, def, true);
        int bad = AiPositiveHelpers.countUsefulStatChanges(ctx, def, atk, def, true) + AiPositiveHelpers.countUsefulStatChanges(ctx, atk, atk, def, false);
        return good - bad >= 2;
    }

    private static int statSwapSplit(AiCtx ctx, Battler atk, Battler def, BattleMove move, int viability, int cls) {
        String f = move.function();
        switch (f) {
            case "053":                                                                            // Guard Swap
                if ((def.stage(PBStats.DEFENSE) > atk.stage(PBStats.DEFENSE) && def.stage(PBStats.SPDEF) >= atk.stage(PBStats.SPDEF))
                        || (def.stage(PBStats.SPDEF) > atk.stage(PBStats.SPDEF) && def.stage(PBStats.DEFENSE) >= atk.stage(PBStats.DEFENSE))) {
                    return incStatus(ctx, viability, cls, 1, atk, def);
                }
                return viability;
            case "052":                                                                            // Power Swap
                if ((def.stage(ATK) > atk.stage(ATK) && def.stage(SPA) >= atk.stage(SPA)) || (def.stage(SPA) > atk.stage(SPA) && def.stage(ATK) >= atk.stage(ATK))) {
                    return incStatus(ctx, viability, cls, 1, atk, def);
                }
                return viability;
            case "057":                                                                            // Power Trick
                if (!atk.effects.truthy(PBEffects.Battler.PowerTrick) && atk.defense() > atk.attack() && AiCalc.physicalMoveInMoveset(ctx, atk)) {
                    return incStatus(ctx, viability, cls, 2, atk, def);
                }
                return viability;
            case "054": return goodIdeaToSwapStatStages(ctx, atk, def) ? incStatus(ctx, viability, cls, 2, atk, def) : viability;   // Heart Swap
            case "161": return AiCalc.speed(def) > AiCalc.speed(atk) ? incStatus(ctx, viability, cls, 3, atk, def) : viability;       // Speed Swap
            case "059": {                                                                          // Guard Split
                int nd = (atk.defense() + def.defense()) / 2;
                int ns = (atk.spDef() + def.spDef()) / 2;
                return (nd > atk.defense() && ns >= atk.spDef()) || (ns > atk.spDef() && nd >= atk.defense()) ? incStatus(ctx, viability, cls, 1, atk, def) : viability;
            }
            case "058": {                                                                          // Power Split
                int na = (atk.attack() + def.attack()) / 2;
                int ns = (atk.spAtk() + def.spAtk()) / 2;
                return (na > atk.attack() && ns >= atk.spAtk()) || (ns > atk.spAtk() && na >= atk.attack()) ? incStatus(ctx, viability, cls, 1, atk, def) : viability;
            }
            default: return viability;
        }
    }

    /** {@code IncreasePsychUpViability} (ai_advanced.c:3099): copy the foe's boosts when swapping is worth 2+ stages. */
    private static int psychUp(AiCtx ctx, Battler atk, Battler def, BattleMove move, int viability, int cls) {
        if (!goodIdeaToSwapStatStages(ctx, atk, def)) return viability;
        int[] stats = {PBStats.ATTACK, PBStats.SPATK, PBStats.SPEED, PBStats.ACCURACY, PBStats.EVASION, PBStats.DEFENSE, PBStats.SPDEF};
        for (int stat : stats) {
            int diff = def.stage(stat) - atk.stage(stat);
            if (diff <= 0) continue;
            boolean use = (stat == ATK && AiCalc.physicalMoveInMoveset(ctx, atk)) || (stat == SPA && AiCalc.specialMoveInMoveset(ctx, atk))
                    || stat == PBStats.ACCURACY || stat == PBStats.EVASION || stat == PBStats.SPEED
                    || ((stat == PBStats.DEFENSE || stat == PBStats.SPDEF) && AiCalc.classStall(cls));
            if (use) return AiPositiveHelpers.incStat(ctx, viability, cls, diff, atk, def, move, stat, 6);
        }
        return viability;
    }
}
