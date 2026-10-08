package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;

import java.util.Random;

import pokemon.runtime.battle.AiCalc.AiDmg;

/**
 * CFRU {@code ai_switching.c} (singles): the trainer AI's voluntary switching ({@code ShouldSwitch}, :50) and the
 * bench scorer ({@code CalcMostSuitableMonToSwitchInto}, :1980) that also picks the replacement after a faint.
 *
 * <p>Bench Pokemon are {@link Battler}s of the party, so the same damage code the move scorer uses ({@link AiCalc})
 * evaluates them; a bench Pokemon is evaluated with neutral stat stages and no field state of its own.</p>
 *
 * <p>登记 (not transcribed): {@code PassOnWish} (:751), {@code CanStopLockedMove} (:939) and
 * {@code SemiInvulnerableTroll} (:803; it can never return TRUE in the source either), {@code ShouldSwitchIfPerishSong}
 * (its body is not in the cached source), {@code ShouldSwitchIfWonderGuard} (:1323), the pivot hand-off
 * ({@code ConfirmAISwitch(.., willPivot)} :153: a fast pivoting move simply declines the switch here), Disguise on the
 * incoming Pokemon, Dynamax, Imposter/Trace on the incoming Pokemon, Steelsurge, Wish recovery on the incoming Pokemon,
 * and everything doubles-only. {@code switchingCooldown} (set outside the cached files) is read as "this Pokemon has
 * not yet had a turn".</p>
 */
final class AiSwitching {

    static final int INCREASE_KO_FOE = 31;
    static final int INCREASE_RESIST_ALL_MOVES = 17;
    static final int INCREASE_REVENGE_KILL = 8;
    static final int INCREASE_WALLS_FOE = 2;
    static final int INCREASE_CAN_2HKO = 2;
    static final int INCREASE_OUTSPEEDS = 14;
    static final int SCORE_MAX = INCREASE_KO_FOE + INCREASE_RESIST_ALL_MOVES + INCREASE_REVENGE_KILL + INCREASE_OUTSPEEDS;
    static final int INCREASE_CAN_REMOVE_HAZARDS = SCORE_MAX + 1;
    static final int DECREASE_WEAK_TO_MOVE = 1;
    static final int DECREASE_FAINTS_FROM_FOE = 39;
    static final int DECREASE_FAINTS_FROM_FOE_BUT_OUTSPEEDS = 15;

    static final int FLAG_KO_FOE = 1;
    static final int FLAG_RESIST_ALL_MOVES = 1 << 1;
    static final int FLAG_REVENGE_KILL = 1 << 2;
    static final int FLAG_WALLS_FOE = 1 << 3;
    static final int FLAG_CAN_2HKO = 1 << 4;
    static final int FLAG_CAN_REMOVE_HAZARDS = 1 << 5;
    static final int FLAG_OUTSPEEDS = 1 << 6;
    static final int FLAG_FAINTS_FROM_FOE = 1 << 7;

    /** {@code OFFENSIVE_STAT_MIN_NUM}: switch when an offensive stat is -3 or less. */
    static final int OFFENSIVE_STAT_MIN_NUM = 3;

    static final int NONE = -1;

    private AiSwitching() {
    }

    /** {@code gNewBS->ai.bestMonIdToSwitchInto*} and {@code monIdToSwitchInto*} for one battler. */
    static final class Bench {
        final int[] scores;
        final int[] flags;
        int best = NONE;
        int second = NONE;
        int bestScore;
        int secondScore;
        int bestFlags;
        int secondFlags;

        Bench(int size) {
            scores = new int[size];
            flags = new int[size];
        }
    }

    // ------------------------------------------------------------------
    // Entry hazards on a bench Pokemon (ai_util.c)
    // ------------------------------------------------------------------

    /** {@code IsMonAffectedByHazards}-style gate: no Magic Guard, not immune to indirect damage. */
    private static boolean affectedByHazards(Battler mon) {
        return !mon.hasActiveAbility("MAGICGUARD");
    }

    /** {@code GetMonEntryHazardDamage(mon,side)} (ai_util.c): Stealth Rock, Spikes (grounded). 登记: Steelsurge. */
    static int hazardDamage(Battle battle, Battler user, Battler mon) {
        if (!affectedByHazards(mon) || mon.hasActiveItem("HEAVYDUTYBOOTS")) return 0;
        BattleSide side = user.pbOwnSide();
        int dmg = 0;
        if (side.effects.truthy(PBEffects.Side.StealthRock)) {
            Array<String> t = mon.pbTypes(true);
            int eff = PBTypes.getCombinedEffectiveness(battle.pbs(), "ROCK",
                    t.size > 0 ? t.get(0) : null, t.size > 1 ? t.get(1) : null, t.size > 2 ? t.get(2) : null);
            if (!PBTypes.ineffective(eff)) {
                dmg += Math.round(mon.maxHp() * (eff / (float) PBTypeEffectiveness.NORMAL_EFFECTIVE) / 8);
            }
        }
        int spikes = side.effects.intVal(PBEffects.Side.Spikes);
        if (spikes > 0 && !mon.airborne()) {
            dmg += mon.maxHp() / new int[]{8, 6, 4}[Math.min(spikes, 3) - 1];
        }
        return dmg;
    }

    /** {@code WillFaintFromEntryHazards(mon,side)}. */
    static boolean willFaintFromHazards(Battle battle, Battler user, Battler mon) {
        return hazardDamage(battle, user, mon) >= mon.hp;
    }

    /** {@code WillTakeSignificantDamageFromEntryHazards(bank,healthFraction)} (ai_util.c:1958). */
    static boolean significantHazardDamage(Battle battle, Battler user, int healthFraction) {
        BattleSide side = user.pbOwnSide();
        if (!(side.effects.truthy(PBEffects.Side.StealthRock) || side.effects.intVal(PBEffects.Side.Spikes) > 0)) return false;
        int dmg = hazardDamage(battle, user, user);
        if (dmg >= user.hp) return true;
        return dmg >= user.maxHp() / healthFraction;
    }

    /** {@code GetMonPassiveRecovery(mon)} (ai_util.c:2510). */
    static int passiveRecovery(Battle battle, Battler mon) {
        int amount = 0;
        int max = mon.maxHp();
        String item = mon.hasActiveItem("LEFTOVERS") ? "LEFTOVERS" : mon.hasActiveItem("BLACKSLUDGE") ? "BLACKSLUDGE" : "";
        if (item.equals("LEFTOVERS")) amount += Math.max(1, max / 16);
        else if (item.equals("BLACKSLUDGE")) amount += mon.pbHasType("POISON") ? Math.max(1, max / 16) : -Math.max(1, max / 8);
        int w = battle.pbWeather();
        boolean rain = w == PBWeather.Rain || w == PBWeather.HeavyRain;
        boolean sun = w == PBWeather.Sun || w == PBWeather.HarshSun;
        if (rain) {
            if (mon.hasActiveAbility("RAINDISH")) amount += Math.max(1, max / 16);
            else if (mon.hasActiveAbility("DRYSKIN")) amount += Math.max(1, max / 8);
        }
        if (sun && (mon.hasActiveAbility("SOLARPOWER") || mon.hasActiveAbility("DRYSKIN"))) amount -= Math.max(1, max / 8);
        if (w == PBWeather.Hail && mon.hasActiveAbility("ICEBODY")) amount += Math.max(1, max / 16);
        if (mon.hasActiveAbility("POISONHEAL") && mon.hasStatus("POISON")) amount += Math.max(1, max / 8);
        if (battle.terrain() == PBBattleTerrains.Grassy && !mon.airborne()) amount += Math.max(1, max / 16);
        return amount;
    }

    // ------------------------------------------------------------------
    // Damage helpers for bench Pokemon
    // ------------------------------------------------------------------

    /** Slot is usable by a bench Pokemon ({@code CheckMoveLimitationsFromParty}: PP). */
    private static boolean benchUsable(Battler mon, int slot) {
        return mon.moveSlot(slot) != null && (mon.moveSlotPp(slot) > 0 || mon.moveSlotMaxPp(slot) == 0);
    }

    /** {@code MoveKnocksOutFromParty(move,mon,foe)}: the top-roll AI damage reaches the foe's HP. */
    private static boolean moveKnocksOut(AiCtx ctx, BattleMove move, Battler mon, Battler foe) {
        if (move == null || move.statusMove()) return false;
        return AiCalc.knocksOutXHits(ctx, move, mon, foe, 1);
    }

    /** {@code CanKnockOutFromParty(mon,foe)}. */
    private static boolean canKnockOutFromParty(AiCtx ctx, Battler mon, Battler foe) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            if (benchUsable(mon, i) && moveKnocksOut(ctx, mon.moveSlot(i), mon, foe)) return true;
        }
        return false;
    }

    /** {@code monMaxDamage[side][mon][foe]}: the largest AI damage among the mon's usable moves. */
    private static int maxDamage(AiCtx ctx, Battler mon, Battler foe) {
        int max = 0;
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = mon.moveSlot(i);
            if (m == null || m.statusMove() || !benchUsable(mon, i)) continue;
            max = Math.max(max, AiCalc.calcDmg(ctx.battle, mon, foe, m).dmg);
        }
        return max;
    }

    private static boolean trappedByAbility(Battler foe, Battler mon) {
        if (mon.hasActiveAbility("SHADOWTAG")) return !foe.hasActiveAbility("SHADOWTAG");
        if (mon.hasActiveAbility("ARENATRAP")) return !foe.airborne();
        if (mon.hasActiveAbility("MAGNETPULL")) return foe.pbHasType("STEEL");
        return false;
    }

    private static int viableMons(Battle battle, Battler user) {
        int n = 0;
        for (Battler b : battle.partyOf(user.index)) {
            if (b != null && !b.fainted() && !b.pokemon.egg) n++;
        }
        return n;
    }

    /** {@code PredictedMoveWontDoTooMuchToMon(activeBattler,mon,foe,switchFlags)} (:168). */
    static boolean predictedMoveWontDoTooMuch(AiCtx ctx, Battler user, Battler mon, Battler foe, int switchFlags) {
        BattleMove defMove = ctx.prediction(foe);
        if ((switchFlags & FLAG_FAINTS_FROM_FOE) != 0) return false;                            // :173
        if (defMove == null || defMove.statusMove() || defMove.function().equals("088")) return true;   // :177-179 Pursuit
        if ((switchFlags & FLAG_RESIST_ALL_MOVES) != 0) return true;                            // :187
        if (defMove.function().equals("116")) return true;                                      // :190 Sucker Punch
        int predicted = AiCalc.calcDmg(ctx.battle, foe, mon, defMove).dmg + hazardDamage(ctx.battle, user, mon);   // :202
        if (predicted >= mon.hp) return false;                                                  // :204
        if (predicted * 2 < mon.hp) return true;                                                // :207
        return predicted * 2 >= mon.maxHp() && healingMoveInMoveset(mon) && mon.speed() > foe.speed();   // :210-212
    }

    /** {@code PredictedMoveWontKOMon(activeBattler,mon,foe)} (:215). */
    static boolean predictedMoveWontKO(AiCtx ctx, Battler user, Battler mon, Battler foe) {
        BattleMove defMove = ctx.prediction(foe);
        if (defMove == null || defMove.statusMove()) return true;                               // :220
        if (defMove.function().equals("116")) return true;                                      // :223
        int predicted = AiCalc.calcDmg(ctx.battle, foe, mon, defMove).dmg + hazardDamage(ctx.battle, user, mon);   // :235
        return predicted < mon.hp;                                                              // :237
    }

    /** {@code PredictedMoveKOsSelfDueToContact(activeBattler,mon,foe)} (:243). */
    static boolean predictedMoveKOsSelfByContact(AiCtx ctx, Battler mon, Battler foe) {
        BattleMove defMove = ctx.prediction(foe);
        if (defMove == null) return false;
        return AiCalc.willFaintFromContactDamage(foe, mon, defMove);                            // :247-250
    }

    private static boolean healingMoveInMoveset(Battler b) {
        return AiCalc.moveFunctionInMoveset(b, "0D5", "0D8", "114", "0D7");   // HealingMoveInMoveset: restore HP / morning sun / Swallow / Wish
    }

    // ------------------------------------------------------------------
    // CalcMostSuitableMonToSwitchInto (:1980)
    // ------------------------------------------------------------------

    /** Party index of {@code user} in its own party. */
    private static int partyIndexOf(Battle battle, Battler user) {
        return battle.partyOf(user.index).indexOf(user, true);
    }

    /** Whether the party member can be considered at all (not on the field, able to fight). */
    private static boolean candidate(Battle battle, Battler user, int i) {
        return battle.canSwitchLax(user.index, i) == null && i != partyIndexOf(battle, user);
    }

    static Bench calcMostSuitable(AiCtx ctx, Battler user, Battler foe) {
        Battle battle = ctx.battle;
        Array<Battler> party = battle.partyOf(user.index);
        Bench out = new Bench(party.size);
        int count = 0;
        for (int i = 0; i < party.size; i++) if (candidate(battle, user, i)) count++;           // :2005-2018
        if (count == 0) return out;                                                              // :2020 ResetBestMonToSwitchInto
        int best = NONE;
        int second = NONE;
        int lastValid = NONE;
        int secondLastValid = NONE;
        boolean[] canNegateToxicSpikes = new boolean[party.size];
        boolean[] canRemoveHazards = new boolean[party.size];
        BattleSide side = user.pbOwnSide();
        boolean spikesOnSide = side.effects.truthy(PBEffects.Side.StealthRock) || side.effects.intVal(PBEffects.Side.Spikes) > 0
                || side.effects.intVal(PBEffects.Side.ToxicSpikes) > 0 || side.effects.truthy(PBEffects.Side.StickyWeb);
        for (int i = 0; i < party.size; i++) {                                                   // :2034
            Battler mon = party.get(i);
            if (!candidate(battle, user, i)) continue;
            boolean asleepOrFrozen = mon.hasStatus("SLEEP") || mon.hasStatus("FREEZE");
            if (asleepOrFrozen && mon.statusCount != 1) continue;                                 // :2041-2045
            int[] scores = out.scores;
            int[] flags = out.flags;
            String ability = mon.ability == null ? "" : mon.ability;
            int speed = mon.speed();
            secondLastValid = lastValid;                                                          // :2057
            lastValid = i;
            canNegateToxicSpikes[i] = mon.pbHasType("POISON") && !mon.airborne();                 // :2059
            if (willFaintFromHazards(battle, user, mon)) continue;                                // :2061
            int passive = passiveRecovery(battle, mon);                                           // :2068
            int wishRecovery = 0;                                                                 // :2069 登记: Wish
            int hpOnSwitchIn = Math.max(0, mon.hp - hazardDamage(battle, user, mon));             // :2252
            boolean skipMon = false;
            {   // the foe loop (:2071) - one foe in singles
                boolean weak = false;
                boolean faints = false;
                int normalEffectiveness = 0;
                if (speed >= foe.speed()) {                                                       // :2088
                    scores[i] += INCREASE_OUTSPEEDS;
                    flags[i] |= FLAG_OUTSPEEDS;
                }
                if (canKnockOutFromParty(ctx, mon, foe)) {                                        // :2095
                    scores[i] += INCREASE_KO_FOE;
                    flags[i] |= FLAG_KO_FOE;
                    if (AiCalc.isMoxie(ability) || trappedByAbility(foe, mon)) {                   // :2100
                        scores[i] += INCREASE_REVENGE_KILL;
                        flags[i] |= FLAG_REVENGE_KILL;
                    } else {
                        // priority moves first (:2113-2150), then the loop (:2153)
                        java.util.List<BattleMove> moves = new java.util.ArrayList<>();
                        java.util.List<Boolean> isPriority = new java.util.ArrayList<>();
                        for (int k = 0; k < Battler.MOVES_MAX; k++) {
                            if (!benchUsable(mon, k)) continue;
                            BattleMove m = mon.moveSlot(k);
                            if (AiCalc.priorityCalc(battle, mon, m) > 0) {
                                moves.add(0, m);
                                isPriority.add(0, true);
                            } else {
                                moves.add(m);
                                isPriority.add(false);
                            }
                        }
                        for (int k = 0; k < moves.size(); k++) {
                            BattleMove m = moves.get(k);
                            if (m.function().equals("110") || m.function().equals("049")) {        // :2157 Rapid Spin / Defog
                                if (spikesOnSide) canRemoveHazards[i] = viableMons(battle, user) >= 2;
                            }
                            if (!m.statusMove() && isPriority.get(k) && moveKnocksOut(ctx, m, mon, foe)) {   // :2166
                                scores[i] += INCREASE_REVENGE_KILL;
                                flags[i] |= FLAG_REVENGE_KILL;
                                if ((flags[i] & FLAG_OUTSPEEDS) == 0) {                            // :2173
                                    scores[i] += INCREASE_OUTSPEEDS;
                                    flags[i] |= FLAG_OUTSPEEDS;
                                }
                                break;
                            } else if (m.function().equals("088") || AiCalc.named(m, "FELLSTINGER")) {   // :2182 Pursuit / Fell Stinger
                                if (moveKnocksOut(ctx, m, mon, foe)) {
                                    scores[i] += INCREASE_REVENGE_KILL;
                                    flags[i] |= FLAG_REVENGE_KILL;
                                    break;
                                }
                            }
                        }
                    }
                } else {                                                                          // :2195
                    boolean hasUsableMove = false;
                    boolean flagged2hko = false;
                    for (int k = 0; k < Battler.MOVES_MAX; k++) {
                        if (!benchUsable(mon, k)) continue;
                        BattleMove m = mon.moveSlot(k);
                        hasUsableMove = true;
                        if ((m.function().equals("110") || m.function().equals("049")) && spikesOnSide) {   // :2211
                            canRemoveHazards[i] = viableMons(battle, user) >= 2;
                        }
                        if (!flagged2hko && !m.statusMove() && maxDamage(ctx, mon, foe) >= foe.hp / 2) {   // :2220-2223
                            scores[i] += INCREASE_CAN_2HKO;
                            flags[i] |= FLAG_CAN_2HKO;
                            flagged2hko = true;
                            break;
                        }
                    }
                    if (!hasUsableMove) {                                                         // :2231
                        scores[i] = -1;
                        skipMon = true;
                    }
                }
                if (!skipMon) {
                    // Defensive capabilities (:2238)
                    java.util.List<BattleMove> moves = new java.util.ArrayList<>();
                    java.util.List<Boolean> isPriority = new java.util.ArrayList<>();
                    boolean physInMoveset = false;
                    boolean specInMoveset = false;
                    for (int k = 0; k < Battler.MOVES_MAX; k++) {
                        BattleMove m = foe.moveSlot(k);
                        if (m == null) break;
                        if (!(foe.moveSlotPp(k) > 0 || foe.moveSlotMaxPp(k) == 0)) continue;       // :2270
                        if (m.statusMove()) continue;                                              // :2278
                        if (m.physical()) physInMoveset = true;
                        else specInMoveset = true;
                        if (AiCalc.priorityCalc(battle, foe, m) > 0) {
                            moves.add(0, m);
                            isPriority.add(0, true);
                        } else {
                            moves.add(m);
                            isPriority.add(false);
                        }
                    }
                    for (int k = 0; k < moves.size(); k++) {                                       // :2302
                        BattleMove m = moves.get(k);
                        int firstHit = AiCalc.calcDmg(battle, foe, mon, m).dmg;                    // :2309 (goodAi, one foe)
                        int otherHits = firstHit;
                        if (firstHit >= hpOnSwitchIn) {                                            // :2319
                            faints = true;
                            if ((flags[i] & FLAG_OUTSPEEDS) != 0 && isPriority.get(k) && battle.terrain() != PBBattleTerrains.Psychic) {   // :2323
                                flags[i] &= ~FLAG_OUTSPEEDS;
                            }
                            break;
                        }
                        if (mon.hp == mon.maxHp() && mon.hasActiveAbility("MULTISCALE") && !foe.hasMoldBreaker()) otherHits *= 2;   // :2334
                        int adjustedHp = Math.min(hpOnSwitchIn + wishRecovery, mon.maxHp());       // :2337
                        if (adjustedHp + passive <= firstHit + otherHits) {                        // :2338
                            weak = true;
                        } else if (adjustedHp + passive * 2 <= firstHit + otherHits * 2) {          // :2342
                            normalEffectiveness++;
                        }
                    }
                    boolean weakDecrement = false;
                    if (faints) {                                                                  // :2365
                        if ((flags[i] & FLAG_OUTSPEEDS) == 0) {
                            flags[i] |= FLAG_FAINTS_FROM_FOE;
                            scores[i] = scores[i] >= DECREASE_FAINTS_FROM_FOE ? scores[i] - DECREASE_FAINTS_FROM_FOE : 0;
                        } else if ((flags[i] & FLAG_KO_FOE) == 0) {
                            scores[i] = scores[i] >= DECREASE_FAINTS_FROM_FOE_BUT_OUTSPEEDS ? scores[i] - DECREASE_FAINTS_FROM_FOE_BUT_OUTSPEEDS : 0;
                        } else {
                            weakDecrement = true;
                        }
                    } else if (weak) {                                                             // :2389
                        weakDecrement = true;
                    } else if (normalEffectiveness == 0) {                                         // :2397
                        scores[i] += INCREASE_RESIST_ALL_MOVES;
                        flags[i] |= FLAG_RESIST_ALL_MOVES;
                    } else {                                                                       // :2402
                        boolean cantWall = false;
                        int attack = foe.attack();
                        int spAttack = foe.spAtk();
                        if (physInMoveset && mon.defense() <= attack) cantWall = true;
                        else if (specInMoveset && mon.spDef() <= spAttack) cantWall = true;
                        if (!cantWall) {
                            scores[i] += INCREASE_WALLS_FOE;
                            flags[i] |= FLAG_WALLS_FOE;
                        }
                    }
                    if (weakDecrement) {                                                           // :2391
                        scores[i] = scores[i] >= DECREASE_WEAK_TO_MOVE ? scores[i] - DECREASE_WEAK_TO_MOVE : 0;
                    }
                }
            }
            if (skipMon) continue;                                                                // :2234 goto CHECK_NEXT_MON
            if (scores[i] >= SCORE_MAX && canRemoveHazards[i]) {                                  // :2423
                second = best;
                best = i;
            } else if (best == NONE || scores[i] > scores[best]
                    || (scores[i] == scores[best] && (flags[best] & FLAG_FAINTS_FROM_FOE) != 0 && (flags[i] & FLAG_FAINTS_FROM_FOE) == 0)
                    || (scores[i] == scores[best] && (flags[best] & FLAG_FAINTS_FROM_FOE) == 0 && (flags[i] & FLAG_FAINTS_FROM_FOE) == 0
                        && ctx.random() % 100 < 50)) {
                second = best;                                                                    // :2439
                best = i;
            } else if (second == NONE || scores[i] > scores[second]
                    || (scores[i] == scores[second] && (flags[second] & FLAG_FAINTS_FROM_FOE) != 0 && (flags[i] & FLAG_FAINTS_FROM_FOE) == 0)
                    || (scores[i] == scores[second] && (flags[second] & FLAG_FAINTS_FROM_FOE) == 0 && (flags[i] & FLAG_FAINTS_FROM_FOE) == 0
                        && ctx.random() % 100 < 50)) {
                second = i;
            }
        }
        if (best != NONE) {                                                                       // :2460
            int[] flags = out.flags;
            if ((flags[best] & FLAG_RESIST_ALL_MOVES) == 0
                    && (flags[best] & (FLAG_KO_FOE | FLAG_OUTSPEEDS)) != (FLAG_KO_FOE | FLAG_OUTSPEEDS)) {   // :2462
                boolean tSpikes = side.effects.intVal(PBEffects.Side.ToxicSpikes) > 0;
                for (int i = 0; i < party.size; i++) {                                            // :2466
                    if ((canRemoveHazards[i] && (flags[i] & FLAG_FAINTS_FROM_FOE) == 0) || (tSpikes && canNegateToxicSpikes[i])) {
                        out.bestScore = INCREASE_CAN_REMOVE_HAZARDS;                              // :2484
                        out.bestFlags = FLAG_CAN_REMOVE_HAZARDS;
                        second = best;
                        best = i;
                        out.best = best;
                        out.second = second;
                        out.secondScore = out.scores[second];
                        out.secondFlags = flags[second];
                        return out;
                    }
                }
            }
            out.best = best;                                                                      // :2499
            out.second = second;
            out.bestScore = out.scores[best];
            out.bestFlags = flags[best];
            if (second != NONE) {
                out.secondScore = out.scores[second];
                out.secondFlags = flags[second];
            }
            return out;
        }
        out.best = lastValid;                                                                     // :2523 pick the last checked mon
        out.second = secondLastValid;
        return out;
    }

    // ------------------------------------------------------------------
    // ShouldSwitch (:50)
    // ------------------------------------------------------------------

    /**
     * {@code AI_TrySwitchOrUseItem}'s switch half (ai_master.c:948): the party index the trainer switches to this turn,
     * or {@link #NONE}.
     */
    static int decide(Battle battle, Battler user, Random rng) {
        Battler foe = battle.battlerAt(user.index ^ 1);
        if (foe == null || foe.pokemon == null || foe.fainted() || user.fainted()) return NONE;
        if (battle.canSwitch(user.index, -1) != null) return NONE;   // :56 IsTrapped
        int available = 0;
        for (int i = 0; i < battle.partyOf(user.index).size; i++) if (candidate(battle, user, i)) available++;     // :75-88
        if (available == 0) return NONE;                                                           // :90
        AiCtx ctx = AiMaster.prepare(battle, user, foe, rng);
        Bench bench = calcMostSuitable(ctx, user, foe);
        int pick = shouldSwitch(ctx, user, foe, bench);
        if (pick == NONE) return NONE;
        if (pick == -2) {                                                                          // PARTY_SIZE: "best mon" (ai_master.c:956)
            pick = bench.best;
        }
        return pick;
    }

    /** The replacement after a faint: {@code GetMostSuitableMonToSwitchInto}, or {@link #NONE}. */
    static int replacement(Battle battle, Battler fainted, Random rng) {
        Battler foe = battle.battlerAt(fainted.index ^ 1);
        if (foe == null || foe.pokemon == null) return NONE;
        AiCtx ctx = AiMaster.prepare(battle, fainted, foe, rng);
        Bench bench = calcMostSuitable(ctx, fainted, foe);
        return bench.best;
    }

    private static final int BEST = -2;

    /** Runs the {@code ShouldSwitch} checks in source order; returns a party index, {@link #BEST} or {@link #NONE}. */
    private static int shouldSwitch(AiCtx ctx, Battler user, Battler foe, Bench bench) {
        int r;
        if ((r = absorbsOpponentsMove(ctx, user, foe, bench)) != NONE) return r;                  // :92
        if ((r = onlyBadMovesLeft(ctx, user, foe, bench)) != NONE) return r;                      // :102
        if ((r = naturalCureOrRegenerator(ctx, user, foe, bench)) != NONE) return r;              // :104
        if ((r = whenYawned(ctx, user, foe, bench)) != NONE) return r;                            // :108
        if ((r = whileAsleep(ctx, user, foe, bench)) != NONE) return r;                           // :110
        if ((r = annoyingSecondaryDamage(ctx, user, foe, bench)) != NONE) return r;               // :112
        if ((r = toAvoidDeath(ctx, user, foe, bench)) != NONE) return r;                          // :114
        if ((r = whenOffensiveStatsLow(ctx, user, foe, bench)) != NONE) return r;                 // :116
        return saveSweeperForLater(ctx, user, foe, bench);                                        // :118
    }

    private static boolean justSwitchedIn(Battler user) {
        return AiCalc.firstTurn(user);                                                             // switchingCooldown
    }

    private static boolean behindSubstitute(Battler b) {
        return b.effects.intVal(PBEffects.Battler.Substitute) > 0;
    }

    private static boolean anyStatGreaterThan(Battler b, int stage) {
        for (int s = PBStats.ATTACK; s <= PBStats.SPDEF; s++) if (b.stage(s) > stage) return true;
        return false;
    }

    private static boolean hasFastPivot(Battler b) {
        return AiCalc.moveFunctionInMoveset(b, "0EE");                                             // U-Turn / Volt Switch / Flip Turn
    }

    // --- FindMonThatAbsorbsOpponentsMove (:403) ---

    private static int absorbsOpponentsMove(AiCtx ctx, Battler user, Battler foe, Bench bench) {
        Battle battle = ctx.battle;
        BattleMove predicted = ctx.prediction(foe);
        if (typeAbsorbCooldown(battle, user)) return NONE;                                          // :415
        if (user.stage(PBStats.EVASION) >= 3) return NONE;                                         // :418 (6+3)
        if (predicted == null || predicted.statusMove()) return NONE;                               // :421-423
        int foeClass = AiCalc.fightingStyle(ctx, foe);                                              // :427
        if (!AiCalc.classDamager(foeClass)) {
            if (justSwitchedIn(user)) return NONE;                                                  // :431
            if ((ctx.random() & 1) != 0) return NONE;                                               // :434
        }
        if (!AiCalc.moveWouldHitFirst(ctx, predicted, foe, user)) {                                 // :438 AI goes first
            if (AiCalc.canKnockOut(ctx, user, foe)) return NONE;
        } else {
            if (!AiCalc.canKnockOut(ctx, foe, user) && AiCalc.canKnockOut(ctx, user, foe) && anyStatGreaterThan(user, 0)) return NONE;   // :445
        }
        if (!AiCalc.canKnockOut(ctx, foe, user) && anyStatGreaterThan(user, 1)) return NONE;       // :451
        if (behindSubstitute(user) && !damagingMoveBreaksSubstitute(ctx, foe, user)) return NONE;   // :455
        String type = AiCalc.fx(predicted).pbCalcType(predicted, foe);                                       // :502
        if (foe.hasMoldBreaker()) return NONE;                                                      // :503
        String[] abil;
        switch (type) {
            case "FIRE": abil = new String[]{"FLASHFIRE"}; break;
            case "ELECTRIC": abil = new String[]{"VOLTABSORB", "LIGHTNINGROD", "MOTORDRIVE"}; break;
            case "WATER": abil = new String[]{"WATERABSORB", "DRYSKIN", "STORMDRAIN"}; break;
            case "GRASS": abil = new String[]{"SAPSIPPER"}; break;
            default: return NONE;
        }
        for (String a : abil) if (user.hasActiveAbility(a)) return NONE;                            // :531
        Array<Battler> party = battle.partyOf(user.index);
        int bestId = bench.best;
        int secondId = bench.second;
        if (bestId != NONE && absorbSwitchCheck(ctx, user, foe, party.get(bestId), bestId, predicted, abil)) return bestId;       // :541
        if (secondId != NONE && absorbSwitchCheck(ctx, user, foe, party.get(secondId), secondId, predicted, abil)) return secondId;   // :549
        for (int i = 0; i < party.size; i++) {                                                      // :557
            if (!candidate(battle, user, i) || i == bestId || i == secondId) continue;
            if (absorbSwitchCheck(ctx, user, foe, party.get(i), i, predicted, abil)) return i;
        }
        return NONE;
    }

    /** {@code gNewBS->ai.typeAbsorbSwitchingCooldown}: two turns after a type-absorb switch. */
    private static boolean typeAbsorbCooldown(Battle battle, Battler user) {
        Integer turn = battle.aiTypeAbsorbSwitchTurn[user.index & 1];
        return turn != null && battle.turns() - turn < 2;
    }

    private static boolean absorbSwitchCheck(AiCtx ctx, Battler user, Battler foe, Battler mon, int monId,
                                             BattleMove predicted, String[] abilities) {
        Battle battle = ctx.battle;
        int side = user.index & 1;
        if ((battle.aiAbsorbSwitched[side] & (1 << monId)) != 0) {                                  // :354
            int noSwitchChance = justSwitchedIn(user) ? 75 : 25;
            if (ctx.random() % 100 < noSwitchChance) return false;
        }
        boolean matches = false;
        for (String a : abilities) if (mon.hasActiveAbility(a)) matches = true;                       // :365
        if (!matches) return false;
        if (willFaintFromHazards(battle, user, mon)) return false;                                  // :369
        boolean hpAbsorb = mon.hasActiveAbility("VOLTABSORB") || mon.hasActiveAbility("WATERABSORB") || mon.hasActiveAbility("DRYSKIN");
        if (hpAbsorb && mon.hp == mon.maxHp() && hazardDamage(battle, user, mon) == 0) {             // :374-377
            if (justSwitchedIn(user)) return false;                                                 // :379
            int dmg = AiCalc.finalDamage(ctx, predicted, foe, user, 2);                             // :382
            if (dmg < user.hp) return false;                                                        // :384
        }
        if (user.hasActiveAbility("VOLTABSORB") || user.hasActiveAbility("WATERABSORB") || user.hasActiveAbility("DRYSKIN")) {   // :389
            battle.aiAbsorbSwitched[side] |= 1 << partyIndexOf(battle, user);
        }
        battle.aiAbsorbSwitched[side] |= 1 << monId;                                                // :392
        battle.aiTypeAbsorbSwitchTurn[side] = battle.turns();                                       // :393
        return true;
    }

    // --- ShouldSwitchIfOnlyBadMovesLeft (:256) ---

    private static int onlyBadMovesLeft(AiCtx ctx, Battler user, Battler foe, Bench bench) {
        if (justSwitchedIn(user)) return NONE;                                                      // :262
        BattleMove foePredicted = ctx.prediction(foe);
        if (user.effects.truthy(PBEffects.Battler.DestinyBond) && foePredicted != null
                && AiCalc.knocksOutXHits(ctx, foePredicted, foe, user, 1) && AiCalc.moveWouldHitFirst(ctx, foePredicted, foe, user)) {
            return NONE;                                                                            // :282
        }
        if (!onlyBadMovesLeftInMoveset(ctx, user, foe, bench)) return NONE;                      // :287
        int bestMon = bench.best;
        int sw = bench.bestFlags;
        int wanted = FLAG_OUTSPEEDS | FLAG_WALLS_FOE | FLAG_RESIST_ALL_MOVES;
        Array<Battler> party = ctx.battle.partyOf(user.index);
        if (bestMon != NONE && (sw & wanted) != 0 && predictedMoveWontDoTooMuch(ctx, user, party.get(bestMon), foe, sw)) {   // :295
            return bestMon;
        }
        int second = bench.second;
        if (second != NONE && (bench.secondFlags & wanted) != 0
                && predictedMoveWontDoTooMuch(ctx, user, party.get(second), foe, bench.secondFlags)) {   // :307
            return second;
        }
        if (AiCalc.choiceLocked(user)) {                                                            // :317
            if (bestMon != NONE && (sw & FLAG_FAINTS_FROM_FOE) == 0 && predictedMoveWontDoTooMuch(ctx, user, party.get(bestMon), foe, sw)) {
                return bestMon;                                                                     // :320
            }
            if (second != NONE && (bench.secondFlags & FLAG_FAINTS_FROM_FOE) == 0
                    && bestMon != NONE && predictedMoveWontDoTooMuch(ctx, user, party.get(bestMon), foe, bench.secondFlags)) {
                return second;                                                                      // :329 (the source passes party[bestMon] here)
            }
        }
        return NONE;
    }

    // --- ShouldSwitchIfNaturalCureOrRegenerator (:635) ---

    private static int naturalCureOrRegenerator(AiCtx ctx, Battler user, Battler foe, Bench bench) {
        Battle battle = ctx.battle;
        if (user.hasActiveAbility("NATURALCURE")) {
            if (significantHazardDamage(battle, user, 4)) return NONE;                               // :646
            if (battle.partyOf(user.index).size > 0 && user.pbOwnSide().effects.intVal(PBEffects.Side.ToxicSpikes) > 0
                    && user.hasStatus("POISON") && !user.pbHasType("POISON") && !user.airborne()) {   // :650-654
                Battler bestMon = bench.best == NONE ? null : battle.partyOf(user.index).get(bench.best);
                if (bestMon == null || !bestMon.pbHasType("POISON") || bestMon.airborne()) return NONE;   // :660
            }
            if (user.hasStatus("SLEEP") || user.hasStatus("FREEZE")) {
                // fall through to the switch (:664-669)
            } else if (user.statused() && user.hp >= user.maxHp() / 2) {                              // :670
                // fall through
            } else {
                return NONE;                                                                         // :674
            }
        } else if (user.hasActiveAbility("REGENERATOR")) {
            if (user.hp > user.maxHp() / 2) return NONE;                                             // :678
            if (significantHazardDamage(battle, user, 3)) return NONE;                               // :682
            BattleMove foeMove = ctx.prediction(foe);
            if (foeMove != null && AiCalc.knocksOutXHits(ctx, foeMove, foe, user, 1)) {              // :688
                BattleMove aiMove = ctx.prediction(user);
                if (aiMove != null && AiCalc.moveWouldHitFirst(ctx, aiMove, user, foe) && AiCalc.knocksOutXHits(ctx, aiMove, user, foe, 1)) {
                    return NONE;                                                                     // :694
                }
            } else {
                return NONE;                                                                         // :710
            }
        } else {
            return NONE;                                                                             // :713
        }
        int resist = switchToBestResistMon(ctx, user, foe, bench, null);                             // :716
        if (resist != NONE) return resist;
        int most = bench.best;
        if (most == NONE) return BEST;
        Battler mostMon = battle.partyOf(user.index).get(most);
        if (!predictedMoveWontDoTooMuch(ctx, user, mostMon, foe, bench.bestFlags)) {                 // :720
            if (predictedMoveWontKO(ctx, user, mostMon, foe)) {
                if ((ctx.random() & 1) != 0) return NONE;                                            // :724
            } else {
                return NONE;                                                                         // :728
            }
        }
        return BEST;                                                                                 // :731 switchoutIndex = PARTY_SIZE
    }

    // --- SwitchToBestResistMon (:598) ---

    private static int resistHelper(AiCtx ctx, Battler user, Battler foe, int monId, int switchFlags, boolean withFoe) {
        if (monId == NONE) return NONE;
        Battler mon = ctx.battle.partyOf(user.index).get(monId);
        if ((switchFlags & FLAG_RESIST_ALL_MOVES) != 0
                || (withFoe && predictedMoveKOsSelfByContact(ctx, mon, foe) && predictedMoveWontKO(ctx, user, mon, foe))) {   // :584-589
            return monId;
        }
        return NONE;
    }

    /** @param willPivotFoe the foe for the contact clause, or null (the source passes 0xFF) */
    private static int switchToBestResistMon(AiCtx ctx, Battler user, Battler foe, Bench bench, Battler willPivotFoe) {
        boolean withFoe = willPivotFoe != null;
        int r = resistHelper(ctx, user, foe, bench.best, bench.bestFlags, withFoe);                   // :603
        if (r != NONE) return r;
        r = resistHelper(ctx, user, foe, bench.second, bench.secondFlags, withFoe);                   // :609
        if (r != NONE) return r;
        for (int i = 0; i < bench.scores.length; i++) {                                              // :618
            if (i == bench.best || i == bench.second || !candidate(ctx.battle, user, i)) continue;
            r = resistHelper(ctx, user, foe, i, bench.flags[i], withFoe);
            if (r != NONE) return r;
        }
        return NONE;
    }

    // --- ShouldSwitchWhenYawned (:975) ---

    private static int whenYawned(AiCtx ctx, Battler user, Battler foe, Bench bench) {
        Battle battle = ctx.battle;
        if (!user.effects.truthy(PBEffects.Battler.Yawn) || user.hasActiveAbility("NATURALCURE")
                || user.hasActiveItem("CHESTOBERRY") || user.hasActiveItem("LUMBERRY")
                || user.hp <= user.maxHp() / 4 || !AiCalc.canBePutToSleep(battle, user, user)) {   // :979-985
            return NONE;
        }
        if (viableMons(battle, foe) <= 1 && AiCalc.canKnockOut(ctx, user, foe)) return NONE;       // :993-1005
        if (user.stage(PBStats.EVASION) >= 3 && !foe.hasActiveAbility("UNAWARE") && !foe.hasActiveAbility("KEENEYE")) return NONE;   // :1020
        if (user.hasActiveAbility("EARLYBIRD") || user.hasActiveAbility("SHEDSKIN")
                || AiCalc.moveFunctionInMoveset(user, "011", "0B4")) return NONE;                   // :1029-1032 Snore / Sleep Talk
        BattleMove foePred = ctx.prediction(foe);
        if (!ctx.predictedToSwitch(foe) && foePred != null && !AiCalc.oneOf(foePred, AiCalc.PROTECT)
                && !(foePred.function().equals("0CA") || foePred.function().equals("0C9") || foePred.function().equals("0CC")
                     || foePred.function().equals("0CB") || foePred.function().equals("0CD")) ) {   // :1042-1044 not protecting / semi-invulnerable first
            if (!AiCalc.moveWouldHitFirst(ctx, foePred, foe, user)) {                                // :1046
                if (AiCalc.canKnockOut(ctx, user, foe)) return NONE;
            } else if (!AiCalc.canKnockOut(ctx, foe, user) && AiCalc.canKnockOut(ctx, user, foe)) {
                return NONE;                                                                         // :1053
            }
        }
        return BEST;                                                                                 // :1078
    }

    // --- ShouldSwitchWhileAsleep (:1086) ---

    private static int whileAsleep(AiCtx ctx, Battler user, Battler foe, Bench bench) {
        Battle battle = ctx.battle;
        if (justSwitchedIn(user)) return NONE;                                                       // :1088
        if (!(user.hasStatus("SLEEP") && user.statusCount > 1) || (ctx.random() & 1) == 0) return NONE;   // :1091-1092
        if (user.stage(PBStats.ATTACK) >= 2 || user.stage(PBStats.SPATK) >= 2 || user.stage(PBStats.SPEED) >= 2
                || user.stage(PBStats.EVASION) >= 3) {                                               // :1098-1101
            if ((ctx.random() & 1) != 0) return NONE;
        }
        if (user.hasActiveAbility("SHEDSKIN") || user.hasActiveAbility("EARLYBIRD")
                || (user.hasActiveAbility("HYDRATION") && (battle.pbWeather() == PBWeather.Rain || battle.pbWeather() == PBWeather.HeavyRain))
                || AiCalc.moveFunctionInMoveset(user, "0B4", "011", "0D9")                             // Sleep Talk / Snore / Rest
                || (AiCalc.classStall(AiCalc.fightingStyle(ctx, user)) && foe.effects.intVal(PBEffects.Battler.MeanLook) >= 0)) {   // :1107-1114
            return NONE;
        }
        int bestId = bench.best;
        if (bestId == NONE) return BEST;
        Battler best = battle.partyOf(user.index).get(bestId);
        if (best.hasStatus("SLEEP") && best.statusCount > 1) return NONE;                            // :1122
        if (!predictedMoveWontDoTooMuch(ctx, user, best, foe, bench.bestFlags)) return NONE;         // :1125
        return BEST;
    }

    // --- IsTakingAnnoyingSecondaryDamage (:1179) ---

    private static int annoyingSecondaryDamage(AiCtx ctx, Battler user, Battler foe, Bench bench) {
        Battle battle = ctx.battle;
        if (user.hasActiveAbility("MAGICGUARD") || AiCalc.canKnockOut(ctx, user, foe) || !ctx.goodAi()) return NONE;   // :1183-1186
        boolean trySwitch = false;
        boolean urgent = false;
        if (user.effects.intVal(PBEffects.Battler.LeechSeed) >= 0 && (ctx.random() & 3) == 0
                && !anyUsefulOffensiveStatRaised(ctx, user) && !AiCalc.canKnockOut(ctx, user, foe)) {   // :1191-1193
            trySwitch = true;
        } else if ((user.hasStatus("SLEEP") && user.statusCount > 1 && user.effects.truthy(PBEffects.Battler.Nightmare))
                || user.effects.truthy(PBEffects.Battler.Curse)
                || (user.hasStatus("POISON") && user.statusCount > 6 && !user.hasActiveAbility("POISONHEAL"))) {   // :1197-1199
            trySwitch = true;
            urgent = true;
        }
        if (trySwitch && !significantHazardDamage(battle, user, 4)) {                                 // :1207
            int r = annoyingCheck(ctx, user, foe, bench.best, bench.bestFlags, urgent);
            if (r != NONE) return r;
            return annoyingCheck(ctx, user, foe, bench.second, bench.secondFlags, urgent);
        }
        return NONE;
    }

    private static int annoyingCheck(AiCtx ctx, Battler user, Battler foe, int monId, int sw, boolean urgent) {
        if (monId == NONE) return NONE;
        Battler mon = ctx.battle.partyOf(user.index).get(monId);
        boolean good = false;
        if (urgent) {
            good = true;                                                                              // :1146
        } else if ((sw & FLAG_OUTSPEEDS) != 0 && ((sw & FLAG_KO_FOE) != 0 || ((sw & FLAG_CAN_2HKO) != 0 && (sw & FLAG_FAINTS_FROM_FOE) == 0))
                && predictedMoveWontDoTooMuch(ctx, user, mon, foe, sw)) {                              // :1147-1151
            good = true;
        } else if ((sw & (FLAG_RESIST_ALL_MOVES | FLAG_WALLS_FOE)) != 0) {
            good = true;                                                                              // :1153
        } else if (!foe.fainted() && AiCalc.can2HKO(ctx, user, foe)) {
            good = true;                                                                              // :1158
        }
        return good ? monId : NONE;
    }

    // --- ShouldSwitchToAvoidDeath (:1248) ---

    private static int avoidDeathHelper(AiCtx ctx, Battler user, Battler foe, BattleMove defMove, int monId, int sw) {
        if (monId == NONE) return NONE;
        Battler mon = ctx.battle.partyOf(user.index).get(monId);
        boolean a = (sw & (FLAG_OUTSPEEDS | FLAG_WALLS_FOE | FLAG_RESIST_ALL_MOVES)) != 0 && AiCalc.noEffect(ctx.battle, foe, mon, defMove);   // :1227
        boolean b = ((sw & (FLAG_WALLS_FOE | FLAG_RESIST_ALL_MOVES)) != 0 || ((sw & FLAG_KO_FOE) != 0 && (sw & FLAG_OUTSPEEDS) != 0))
                && predictedMoveWontDoTooMuch(ctx, user, mon, foe, sw);                                  // :1231-1234
        boolean c = predictedMoveKOsSelfByContact(ctx, mon, foe) && predictedMoveWontKO(ctx, user, mon, foe);   // :1237
        return (a || b || c) ? monId : NONE;
    }

    private static int toAvoidDeath(AiCtx ctx, Battler user, Battler foe, Bench bench) {
        Battle battle = ctx.battle;
        if (justSwitchedIn(user)) return NONE;                                                        // :1250
        if (user.effects.truthy(PBEffects.Battler.DestinyBond)) return NONE;                          // :1253
        if (!ctx.goodAi()) return NONE;                                                               // :1257
        BattleMove atkMove = ctx.prediction(user);
        BattleMove defMove = ctx.prediction(foe);
        if (user.hasStatus("PARALYSIS") && user.hp < user.maxHp() / 3
                && !user.hasActiveAbility("QUICKFEET") && !user.hasActiveAbility("NATURALCURE")) {     // :1263-1268
            return NONE;
        }
        int score = bench.best == NONE ? 0 : bench.bestScore;
        if (defMove != null
                && (atkMove == null || !AiCalc.moveWouldHitFirst(ctx, atkMove, user, foe))             // :1281
                && (!behindSubstitute(user) || !AiCalc.blockedBySubstitute(defMove, foe, user))         // :1282
                && AiCalc.knocksOutXHits(ctx, defMove, foe, user, 1)                                    // :1283
                && !significantHazardDamage(battle, user, 3)                                            // :1284
                && (AiCalc.healthPercent(user) > 20 || score >= SCORE_MAX)) {                           // :1285
            int r = avoidDeathHelper(ctx, user, foe, defMove, bench.best, bench.bestFlags);
            if (r != NONE) return r;
            r = avoidDeathHelper(ctx, user, foe, defMove, bench.second, bench.secondFlags);
            if (r != NONE) return r;
            for (int i = 0; i < bench.scores.length; i++) {                                              // :1300
                if (i == bench.best || i == bench.second || !candidate(battle, user, i)) continue;
                r = avoidDeathHelper(ctx, user, foe, defMove, i, bench.flags[i]);
                if (r != NONE) return r;
            }
        }
        return NONE;
    }

    // --- ShouldSwitchWhenOffensiveStatsAreLow (:1550) ---

    private static int whenOffensiveStatsLow(AiCtx ctx, Battler user, Battler foe, Bench bench) {
        if (justSwitchedIn(user)) return NONE;                                                        // :1559
        int cls = AiCalc.fightingStyle(ctx, user);
        int min = -OFFENSIVE_STAT_MIN_NUM;
        if (!(AiCalc.classDamager(cls)
                && (user.stage(PBStats.ATTACK) <= min || user.stage(PBStats.SPATK) <= min || user.stage(PBStats.ACCURACY) <= min))) {
            return NONE;                                                                              // :1564-1568
        }
        BattleMove pred = ctx.prediction(user);
        if (pred != null && pred.function().equals("054")) return NONE;                              // :1577 Heart Swap
        boolean hasPhysical = false;
        boolean hasSpecial = false;
        for (int i = 0; i < Battler.MOVES_MAX; i++) {                                                 // :1583
            BattleMove m = user.moveSlot(i);
            if (m == null) break;
            if (!AiCalc.usable(ctx, user, i) || m.power() == 0) continue;
            if (m.physical()) {
                if (!m.function().equals("071") && !AiCalc.named(m, "BODYPRESS", "FOULPLAY")) hasPhysical = true;   // :1596 Counter
            } else if (!m.function().equals("072")) {                                                 // :1601 Mirror Coat
                hasSpecial = true;
            }
        }
        boolean atkLow = user.stage(PBStats.ATTACK) <= min;
        boolean spaLow = user.stage(PBStats.SPATK) <= min;
        boolean accLow = user.stage(PBStats.ACCURACY) <= min;
        if (atkLow && hasPhysical && !hasSpecial) {
            // :1607
        } else if (spaLow && hasSpecial && !hasPhysical) {
            // :1612
        } else if (atkLow && spaLow && hasPhysical && hasSpecial) {
            // :1617
        } else if (accLow && allMovesAccuracyBelow(ctx, user, foe, 70) && !AiCalc.classStall(cls)) {
            // :1623
        } else {
            return NONE;                                                                              // :1630
        }
        if (hasFastPivot(user)) return NONE;                                                          // 登记: willPivot hand-off
        if (!AiCalc.canKnockOut(ctx, foe, user) || accLow) {                                          // :1635
            int bestId = bench.best;
            int sw = bench.bestFlags;
            if (bestId == NONE) return NONE;
            Battler best = ctx.battle.partyOf(user.index).get(bestId);
            if ((sw & (FLAG_WALLS_FOE | FLAG_RESIST_ALL_MOVES)) != 0
                    || ((sw & FLAG_OUTSPEEDS) != 0 && predictedMoveWontDoTooMuch(ctx, user, best, foe, sw))) {   // :1641
                return bestId;
            }
        }
        return NONE;
    }

    /** {@code AllMovesInMovesetWithAccuracyLessThan(bank,foe,70,TRUE)}. */
    private static boolean allMovesAccuracyBelow(AiCtx ctx, Battler user, Battler foe, int accuracy) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = user.moveSlot(i);
            if (m == null || !AiCalc.usable(ctx, user, i)) continue;
            if (AiCalc.hitChance(ctx.battle, user, foe, m) >= accuracy) return false;
        }
        return true;
    }

    // --- ShouldSaveSweeperForLater (:1698) ---

    private static int saveSweeperHelper(AiCtx ctx, Battler user, Battler foe, int monId, int sw) {
        if (monId == NONE) return NONE;
        Battler mon = ctx.battle.partyOf(user.index).get(monId);
        int wanted = FLAG_OUTSPEEDS | FLAG_KO_FOE;
        if (((sw & wanted) == wanted && predictedMoveWontDoTooMuch(ctx, user, mon, foe, sw))
                || (predictedMoveKOsSelfByContact(ctx, mon, foe) && predictedMoveWontKO(ctx, user, mon, foe))) {   // :1661-1664
            return monId;
        }
        return NONE;
    }

    private static int saveChoiceSweeper(AiCtx ctx, Battler user, Battler foe, int monId, int sw) {
        if (monId == NONE) return NONE;
        int wanted = FLAG_OUTSPEEDS | FLAG_KO_FOE;
        if ((sw & FLAG_FAINTS_FROM_FOE) != 0) {                                                        // :1681
            if ((sw & wanted) == 0) return monId;
        } else if (predictedMoveWontDoTooMuch(ctx, user, ctx.battle.partyOf(user.index).get(monId), foe, sw)) {   // :1692
            return monId;
        }
        return NONE;
    }

    private static int saveSweeperForLater(AiCtx ctx, Battler user, Battler foe, Bench bench) {
        Battle battle = ctx.battle;
        if (justSwitchedIn(user)) return NONE;                                                         // :1705
        BattleMove foePred = ctx.prediction(foe);
        if (!ctx.goodAi()) return NONE;                                                                // :1714
        if (user.hasStatus("FREEZE")) return NONE;                                                     // :1720
        if (user.hasStatus("PARALYSIS") && !user.hasActiveAbility("QUICKFEET") && !user.hasActiveAbility("GUTS")) return NONE;   // :1722
        if (!AiCalc.classDamager(AiCalc.fightingStyle(ctx, user))) return NONE;                        // :1724
        boolean canKO = AiCalc.canKnockOut(ctx, user, foe);
        boolean fakeOutFirst = foePred != null && AiCalc.named(foePred, "FAKEOUT") && !user.hasActiveAbility("INNERFOCUS")
                && AiCalc.finalDamage(ctx, foePred, foe, user, 1) >= user.maxHp() / 2;                  // :1731-1735
        if (canKO && !fakeOutFirst) return NONE;                                                       // :1726-1736
        if (behindSubstitute(user) && !damagingMoveBreaksSubstitute(ctx, foe, user)) return NONE;   // :1737
        if (significantHazardDamage(battle, user, 4)) return NONE;                                     // :1738
        if ((isTrapped(foe) && AiCalc.takingSecondaryDamage(ctx.battle, foe))) return NONE;                                                  // :1739
        boolean foeKOs = AiCalc.canKnockOut(ctx, foe, user);
        boolean optionA = foeKOs && !AiCalc.canHealFirstToPreventKnockOut(ctx, user, foe);             // :1743
        boolean optionB = false;
        if (!optionA) {                                                                                // :1747
            optionB = !anyUsefulOffensiveStatRaised(ctx, user) && user.stage(PBStats.EVASION) < 3
                    && !offensiveSetupMoveInMoveset(ctx, user, foe)
                    && ((bench.bestFlags & FLAG_KO_FOE) != 0                                           // :1754
                        || chanceToSwitch(ctx, user, foe));                                            // :1757-1760
        }
        if (!(optionA || optionB)) return NONE;
        BattleMove movePred = ctx.prediction(user);
        if (movePred != null && (AiCalc.named(movePred, "FAKEOUT") || AiCalc.oneOf(movePred, AiCalc.PROTECT))) return NONE;   // :1765
        if (hasFastPivot(user)) return NONE;                                                           // 登记: willPivot hand-off (:1769)
        if (foeKOs) {                                                                                  // :1771
            if (movePred != null && AiCalc.moveWouldHitFirst(ctx, movePred, user, foe)) {
                if (movePred.function().equals("0E7")) return NONE;                                    // :1776 Destiny Bond
            } else if (user.effects.truthy(PBEffects.Battler.DestinyBond)) {
                return NONE;                                                                           // :1781
            }
            int r = saveSweeperHelper(ctx, user, foe, bench.best, bench.bestFlags);                     // :1789
            if (r != NONE) return r;
            r = saveSweeperHelper(ctx, user, foe, bench.second, bench.secondFlags);                     // :1795
            if (r != NONE) return r;
            if (AiCalc.choiceLocked(user)) {                                                            // :1801
                // 登记: strongest-move-as-if-unlocked (:1804-1807); approximated by the unlocked strongest move
                BattleMove strongest = AiCalc.calcStrongestMove(ctx, user, foe);
                if (strongest != null && AiCalc.finalDamage(ctx, strongest, user, foe, 1) >= foe.hp
                        && AiCalc.moveWouldHitFirst(ctx, strongest, user, foe)) {
                    r = saveChoiceSweeper(ctx, user, foe, bench.best, bench.bestFlags);
                    if (r != NONE) return r;
                    r = saveChoiceSweeper(ctx, user, foe, bench.second, bench.secondFlags);
                    if (r != NONE) return r;
                }
            }
        }
        return switchToBestResistMon(ctx, user, foe, bench, foe);                                      // :1823
    }

    /** :1757-1760 option B2 (50% to throw off the player, 75% only when it cannot 2HKO). */
    private static boolean chanceToSwitch(AiCtx ctx, Battler user, Battler foe) {
        int randVal = ctx.random();
        return (randVal & 1) != 0
                && (!AiCalc.can2HKO(ctx, user, foe) || (randVal & 2) != 0)
                && !resistsAllMoves(ctx, foe, user)
                && !strongestMoveIsHpDraining(ctx, user, foe);
    }

    // ------------------------------------------------------------------
    // ai_util.c helpers
    // ------------------------------------------------------------------

    /** {@code IsTrapped(bank,TRUE)}. 登记: Shadow Tag / Arena Trap / Magnet Pull on the foe's side. */
    private static boolean isTrapped(Battler b) {
        if (b.pbHasType("GHOST")) return false;
        return b.effects.intVal(PBEffects.Battler.MeanLook) >= 0 || b.effects.intVal(PBEffects.Battler.Trapping) > 0
                || b.effects.truthy(PBEffects.Battler.Ingrain);
    }

    /** {@code DamagingMoveThaCanBreakThroughSubstituteInMoveset(bankAtk,bankDef)} (ai_util.c:4245). */
    private static boolean damagingMoveBreaksSubstitute(AiCtx ctx, Battler atk, Battler def) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = atk.moveSlot(i);
            if (m != null && AiCalc.usable(ctx, atk, i) && !m.statusMove() && !AiCalc.blockedBySubstitute(m, atk, def)) return true;
        }
        return false;
    }

    /** {@code AnyUsefulOffseniveStatIsRaised(bank)} (ai_util.c:5231). 登记: Flash Fire, Unburden. */
    private static boolean anyUsefulOffensiveStatRaised(AiCtx ctx, Battler b) {
        if (b.stage(PBStats.ATTACK) > 0 && AiCalc.physicalMoveInMoveset(ctx, b)) return true;
        if (b.stage(PBStats.DEFENSE) > 0 && AiCalc.named(firstMove(b, "BODYPRESS"), "BODYPRESS")) return true;
        if (b.stage(PBStats.SPATK) > 0 && AiCalc.specialMoveInMoveset(ctx, b)) return true;
        return b.stage(PBStats.SPEED) > 0;
    }

    private static BattleMove firstMove(Battler b, String name) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = b.moveSlot(i);
            if (m != null && AiCalc.named(m, name)) return m;
        }
        return null;
    }

    /** {@code OffensiveSetupMoveInMoveset(bankAtk,bankDef)} (ai_util.c:4443). 登记: the secondary-effect stat raisers. */
    private static boolean offensiveSetupMoveInMoveset(AiCtx ctx, Battler atk, Battler def) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = atk.moveSlot(i);
            if (m == null || !AiCalc.usable(ctx, atk, i)) continue;
            if (AiCalc.oneOf(m, "01C", "020", "022", "02E", "030", "032", "034", "024", "026", "027", "028", "029", "02C", "03A")) return true;
        }
        return false;
    }

    /** {@code ResistsAllMoves(bankAtk,bankDef)} (ai_util.c:1714): none of {@code atk}'s moves does a third of {@code def}'s HP. */
    private static boolean resistsAllMoves(AiCtx ctx, Battler atk, Battler def) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = atk.moveSlot(i);
            if (m == null) break;
            if (!AiCalc.usable(ctx, atk, i)) continue;
            if (AiCalc.finalDamage(ctx, m, atk, def, 1) >= def.hp / 3) return false;
        }
        return true;
    }

    /** {@code IsStrongestMoveHPDrainingMove(bankAtk,bankDef)} (ai_util.c:3563): Absorb family / Dream Eater. */
    private static boolean strongestMoveIsHpDraining(AiCtx ctx, Battler atk, Battler def) {
        BattleMove m = AiCalc.strongestMove(ctx, atk, def);
        return m != null && AiCalc.oneOf(m, "0DD", "0DE");
    }

    /** {@code CalcOnlyBadMovesLeftInMoveset(bankAtk,bankDef)} (ai_util.c:4737), singles. 登记: Dynamax, Knock Off item check. */
    private static boolean onlyBadMovesLeftInMoveset(AiCtx ctx, Battler atk, Battler def, Bench bench) {
        if (ctx.flags == AiCtx.FLAG_CHECK_BAD_MOVE) return false;                                  // :4745
        int numDamageMoves = 0;
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = atk.moveSlot(i);
            if (m == null) break;
            if (!AiCalc.usable(ctx, atk, i)) continue;
            int viability = AiNegatives.score(ctx, atk, def, m, 100);                              // :4764
            if (viability >= 100) {
                if (m.statusMove()) return false;                                                  // :4768
                if (m.function().equals("0F0")) return false;                                      // :4771 Knock Off: 登记 CanKnockOffItem
                numDamageMoves++;
            }
        }
        if (AiCalc.moveFunctionInMoveset(atk, AiCalc.PROTECT)) return false;                       // :4780 single-battle protectors stall
        if (numDamageMoves == 0) {                                                                 // :4782
            if (behindSubstitute(atk) && !damagingMoveBreaksSubstitute(ctx, def, atk) && AiCalc.takingSecondaryDamage(ctx.battle, def)) return false;
            return true;
        }
        if (behindSubstitute(atk) && !damagingMoveBreaksSubstitute(ctx, def, atk)) return false;   // :4800
        if (AiCalc.moveFunctionInMoveset(atk, "0EE")                                               // :4806 PivotingMoveInMovesetThatAffects (approximated)
                || (!def.hasActiveAbility("MAGICGUARD") && def.effects.intVal(PBEffects.Battler.Trapping) > 0)
                || (isTrapped(def) && def.effects.intVal(PBEffects.Battler.PerishSong) > 0)) {
            return false;
        }
        BattleMove strongest = AiCalc.strongestMove(ctx, atk, def);                                // :4814
        int dmg = AiCalc.finalDamage(ctx, strongest, atk, def, 1);
        if (dmg >= def.hp) return false;                                                           // :4816
        int leftovers = Math.max(0, passiveRecovery(ctx.battle, def));
        if (leftovers != 0 || healingMoveInMoveset(def)) {                                         // :4820
            if (dmg * 3 >= def.hp + leftovers * 2) return false;
        } else if (dmg * 3 >= def.hp) {
            return false;
        }
        int sw = bench.bestFlags;                                                                  // :4846
        if (bench.best == NONE || sw == 0) return false;                                           // :4857
        if ((sw & (FLAG_KO_FOE | FLAG_CAN_2HKO)) != 0) {
            // new mon has the advantage
        } else if ((sw & FLAG_RESIST_ALL_MOVES) != 0) {
            if (resistsAllMoves(ctx, def, atk)) return false;                                      // :4870
        } else if ((sw & FLAG_WALLS_FOE) != 0) {
            if (wallsFoe(def, atk)) return false;                                                  // :4876
        }
        return true;
    }

    /** {@code WallsFoe(bankAtk,bankDef)} (ai_util.c:1748). */
    private static boolean wallsFoe(Battler atk, Battler def) {
        boolean cantWall = false;
        if (hasPhysical(atk) && def.defense() <= atk.attack()) cantWall = true;
        else if (hasSpecial(atk) && def.spDef() <= atk.spAtk()) cantWall = true;
        return !cantWall;
    }

    private static boolean hasPhysical(Battler b) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = b.moveSlot(i);
            if (m != null && m.power() > 0 && !m.statusMove() && m.physical()) return true;
        }
        return false;
    }

    private static boolean hasSpecial(Battler b) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = b.moveSlot(i);
            if (m != null && m.power() > 0 && !m.statusMove() && !m.physical()) return true;
        }
        return false;
    }
}
