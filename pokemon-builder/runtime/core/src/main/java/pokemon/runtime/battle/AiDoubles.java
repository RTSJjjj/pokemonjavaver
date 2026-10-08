package pokemon.runtime.battle;

import java.util.Random;

/**
 * CFRU {@code ChooseMoveOrAction_Doubles} and {@code ChooseTarget_Doubles} (ai_master.c:525-846, the static helpers
 * :412-523): every move is scored against every living target with the same scripts the singles AI uses, then the
 * target is picked among those whose best move scores highest.
 *
 * <p>登记: {@code ai_partner.c} (an ally as a target is always -1, i.e. never chosen), Z-moves
 * ({@code TryReplaceMoveWithZMove}), the doubles fighting classes ({@code IsClassDoublesAttacker} is read from the
 * singles class: Sweeper/Damager), {@code CanKnockOutWithFasterMove} (approximated by "can knock out and its strongest
 * move goes first"), and the doubles branches of the scripts and of switching/item use.</p>
 */
final class AiDoubles {

    /** The chosen move slot and the battler index to aim at. */
    static final class Choice {
        final int slot;
        final int target;

        Choice(int slot, int target) {
            this.slot = slot;
            this.target = target;
        }
    }

    private AiDoubles() {
    }

    private static boolean spread(Battle battle, Battler user, BattleMove move) {
        int t = AiCalc.fx(move).pbTarget(move, user);
        return t == PBTargets.AllNearFoes || t == PBTargets.AllNearOthers || t == PBTargets.AllFoes;
    }

    private static boolean selected(Battle battle, Battler user, BattleMove move) {
        return PBTargets.oneTarget(AiCalc.fx(move).pbTarget(move, user));
    }

    private static Battler partnerOf(Battle battle, Battler b) {
        for (Battler o : battle.eachSameSideBattler(b.index)) if (o != b && !o.fainted()) return o;
        return null;
    }

    private static Battler otherFoe(Battle battle, Battler foe) {
        return partnerOf(battle, foe);
    }

    /** {@code HasChosenToDamageTarget(bankAtk,bankDef)} (:426). */
    private static boolean hasChosenToDamageTarget(Battle battle, Battler atk, Battler def) {
        if (atk == null || atk.fainted() || AiCalc.highChanceOfBeingImmobilized(atk)) return false;
        BattleMove chosen = battle.chosenMove(atk.index);
        if (chosen == null) return false;
        Object[] c = battle.choices(atk.index);
        return (c[3] instanceof Integer && (Integer) c[3] == def.index) || spread(battle, atk, chosen);
    }

    /** {@code CanKnockOutWithFasterMove(bankAtk,bankDef,move)}. */
    private static boolean canKnockOutWithFasterMove(AiCtx ctx, Battler atk, Battler def, BattleMove defMove) {
        if (atk == null || def == null || !AiCalc.canKnockOut(ctx, atk, def)) return false;
        return AiCalc.wouldHitBefore(ctx.battle, AiCalc.strongestMove(ctx, atk, def), atk, defMove, def);
    }

    /** {@code PartnerWillKOTargetBeforeItCanAttack(bankAtk,bankDef)} (:433). */
    private static boolean partnerWillKOTarget(AiCtx ctx, Battler atk, Battler def) {
        Battle battle = ctx.battle;
        Battler partner = partnerOf(battle, atk);
        if (!hasChosenToDamageTarget(battle, partner, def)) return false;
        BattleMove chosen = battle.chosenMove(partner.index);
        return !canKnockOutWithFasterMove(ctx, def, partner, chosen)
                && !canKnockOutWithFasterMove(ctx, otherFoe(battle, def), partner, chosen)
                && AiCalc.wouldHitBefore(battle, chosen, partner, ctx.prediction(def), def);
    }

    /** {@code WontHitTargetWithMove(bankAtk,bankDef,move)} (:412). */
    private static boolean wontHit(AiCtx ctx, Battler atk, Battler def, BattleMove move) {
        if (move == null || !def.semiInvulnerable()) return false;
        return (selected(ctx.battle, atk, move) || spread(ctx.battle, atk, move))
                && !AiCalc.moveWillHit(ctx.battle, atk, def, move)
                && AiCalc.moveWouldHitFirst(ctx, move, atk, def);
    }

    /** {@code GetTargetsKnockedOut(move,bankAtk,baseBankDef)} (:453). */
    private static int targetsKnockedOut(AiCtx ctx, BattleMove move, Battler atk, Battler def) {
        Battle battle = ctx.battle;
        int n = 0;
        Battler atkPartner = partnerOf(battle, atk);
        Battler[] victims = {def, spread(battle, atk, move) ? otherFoe(battle, def) : null};
        for (Battler v : victims) {
            if (v == null || v.fainted()) continue;
            if (AiCalc.knocksOutXHits(ctx, move, atk, v, 1)) {
                n++;
            } else if (!move.function().equals("06C") && hasChosenToDamageTarget(battle, atkPartner, v)
                    && AiCalc.wouldHitBefore(battle, battle.chosenMove(atkPartner.index), atkPartner, null, v)) {
                BattleMove pm = battle.chosenMove(atkPartner.index);
                int partnerDmg = AiCalc.finalDamage(ctx, pm, atkPartner, v, 1);
                if (partnerDmg < v.hp && AiCalc.finalDamage(ctx, move, atk, v, 1) + partnerDmg >= v.hp) n++;
            }
        }
        return n;
    }

    /** {@code GetTotalDamageForTargets(move,bankAtk,baseBankDef)} (:497): percentage of max HP. */
    private static int totalDamage(AiCtx ctx, BattleMove move, Battler atk, Battler def) {
        int pct = 0;
        if (!def.fainted()) pct += AiCalc.finalDamage(ctx, move, atk, def, 1) * 100 / def.maxHp();
        Battler other = otherFoe(ctx.battle, def);
        if (other != null && spread(ctx.battle, atk, move)) {
            pct += AiCalc.finalDamage(ctx, move, atk, other, 1) * 100 / def.maxHp();   // sic: the source divides by the base target's max HP
        }
        return pct;
    }

    private static boolean classDoublesAttacker(AiCtx ctx, Battler b) {
        int cls = AiCalc.fightingStyle(ctx, b);
        return AiCalc.classSweeper(cls) || AiCalc.classDamager(cls);
    }

    /** {@code ChooseMoveOrAction_Doubles}. Null when nothing could be scored. */
    static Choice choose(Battle battle, Battler user, Random rng) {
        int n = battle.maxBattlerIndex() + 1;
        int[] points = new int[n];
        int[] slotFor = new int[n];
        AiCtx[] ctxs = new AiCtx[n];
        for (int i = 0; i < n; i++) {
            Battler t = battle.battlerAt(i);
            if (t == null || t == user || t.fainted()) {                                       // :538
                slotFor[i] = 0xFF;
                points[i] = -1;
                continue;
            }
            AiCtx ctx = AiMaster.prepare(battle, user, t, rng);                                 // :546 BattleAI_SetupAIData + the defender
            ctxs[i] = ctx;
            int[] score = AiMaster.scoreMoves(ctx, battle, user, t);
            int[] best = new int[Battler.MOVES_MAX];
            int num = 1;
            best[0] = 0;
            for (int j = 1; j < Battler.MOVES_MAX; j++) {                                       // :578
                if (user.moveSlot(j) == null) continue;
                if (score[best[0]] == score[j]) best[num++] = j;
                else if (score[best[0]] < score[j]) {
                    num = 1;
                    best[0] = j;
                }
            }
            slotFor[i] = best[ctx.random() % num];                                              // :599
            points[i] = score[best[0]];
            if (t.foe == user.foe && points[i] < 101) points[i] = -1;                           // :603 never an ally unless clearly good
            if (t.foe == user.foe) points[i] = -1;                                              // 登记: ai_partner.c
        }
        AiCtx ctx = null;
        for (AiCtx c : ctxs) if (c != null) { ctx = c; break; }
        if (ctx == null) return null;
        return chooseTarget(battle, user, ctx, points, slotFor, rng);
    }

    private static BattleMove moveAt(Battler user, int slot) {
        return slot >= 0 && slot < Battler.MOVES_MAX ? user.moveSlot(slot) : null;
    }

    private static Choice chooseTarget(Battle battle, Battler user, AiCtx ctx, int[] points, int[] slotFor, Random rng) {
        int n = points.length;
        boolean tryKnockOut = classDoublesAttacker(ctx, user);                                  // ShouldPrioritizeKOingFoesDoubles (smart AI)
        boolean tryMostDamage = classDoublesAttacker(ctx, user) && ctx.random() % 4 == 0;       // ShouldPrioritizeMostDamageDoubles
        boolean tryDangerous = classDoublesAttacker(ctx, user);                                 // ShouldPrioritizeDangerousTarget
        int[] viable = new int[n];
        int numViable = 1;
        viable[0] = 0;
        int mostPoints = points[0];
        BattleMove firstMove = moveAt(user, slotFor[0]);
        boolean usingDefault = true;
        if (firstMove != null && battle.battlerAt(0) != null && wontHit(ctx, user, battle.battlerAt(0), firstMove)) {
            // :639 change the default target to the next foe (CFRU: bank 1; a 3v1 boss fight keeps the boss at index 1,
            // so the next living opponent of the user is taken instead)
            int second = 1;
            for (int i = 1; i < n; i++) {
                Battler o = battle.battlerAt(i);
                if (o != null && o != user && o.foe != user.foe && !o.fainted()) {
                    second = i;
                    break;
                }
            }
            viable[0] = second;
            mostPoints = points[second];
            firstMove = moveAt(user, slotFor[second]);
        }
        int mostDmgTarget = viable[0];
        int mostDamage = 0;
        int mostKnockouts = 0;
        boolean statusOption = false;
        if (firstMove != null) {
            Battler t = battle.battlerAt(mostDmgTarget);
            if (!firstMove.statusMove()) {
                if (t != null && t.foe != user.foe) {
                    if (tryKnockOut) mostKnockouts = targetsKnockedOut(ctx, firstMove, user, t);
                    if (tryMostDamage || tryDangerous) mostDamage = totalDamage(ctx, firstMove, user, t);
                }
            } else {
                statusOption = true;
            }
        }
        for (int def = 1; def < n; def++) {                                                     // :676
            BattleMove move = moveAt(user, slotFor[def]);
            Battler d = battle.battlerAt(def);
            if (d != null && move != null && wontHit(ctx, user, d, move)) {
                if (!usingDefault) continue;
                if (!wontHit(ctx, user, battle.battlerAt(viable[0]), firstMove)) continue;
            }
            boolean add = false;
            if (points[def] == mostPoints) {
                statusOption = statusOption || (move != null && move.statusMove());
                boolean damaging = d != null && d.foe != user.foe && move != null && !move.statusMove();
                if (damaging) {
                    int thisKnockouts = tryKnockOut ? targetsKnockedOut(ctx, move, user, d) : 0;
                    int thisDamage = (tryMostDamage || tryDangerous) ? totalDamage(ctx, move, user, d) : 0;
                    int verdict;                                                                // 0 skip, 1 add, 2 replace
                    if (!tryKnockOut) {
                        verdict = cantKnockOut(ctx, user, battle, d, battle.battlerAt(mostDmgTarget), thisDamage, mostDamage, tryDangerous, tryMostDamage);
                    } else if (thisKnockouts < mostKnockouts) {
                        verdict = 0;
                    } else if (thisKnockouts > mostKnockouts) {
                        verdict = 2;
                    } else if (thisKnockouts > 0) {
                        verdict = tryDangerous ? dangerous(ctx, user, battle, d, battle.battlerAt(mostDmgTarget)) : 1;
                        if (verdict == 3) verdict = damageVerdict(thisDamage, mostDamage, tryMostDamage);
                    } else {
                        verdict = cantKnockOut(ctx, user, battle, d, battle.battlerAt(mostDmgTarget), thisDamage, mostDamage, tryDangerous, tryMostDamage);
                    }
                    if (verdict == 0) continue;
                    if (verdict == 2) {
                        if (!statusOption) {
                            numViable = 1;
                            viable[0] = mostDmgTarget = def;
                        } else {
                            mostDmgTarget = def;
                            boolean[] keep = new boolean[n];
                            keep[def] = true;
                            for (int k = 0; k < numViable; k++) {
                                BattleMove m = moveAt(user, slotFor[viable[k]]);
                                if (m == null || m.statusMove()) keep[viable[k]] = true;
                            }
                            numViable = 0;
                            for (int k = 0; k < n; k++) if (keep[k]) viable[numViable++] = k;
                        }
                        mostKnockouts = thisKnockouts;
                        mostDamage = thisDamage;
                        usingDefault = false;
                        continue;
                    }
                    add = true;
                } else if (move != null && move.statusMove() && !move.internalName().equals("QUASH")
                        && d != null && partnerWillKOTarget(ctx, user, d)) {
                    if (!usingDefault || !uselessStatusOnDefault(ctx, user, battle.battlerAt(viable[0]), slotFor)) continue;
                    add = true;
                } else {
                    add = true;
                }
                if (add) {
                    viable[numViable++] = def;
                    usingDefault = false;
                }
            } else if (points[def] > mostPoints) {                                              // :791
                if (mostPoints >= 0 && move != null && move.statusMove() && !move.internalName().equals("QUASH")
                        && d != null && partnerWillKOTarget(ctx, user, d)) {
                    if (!usingDefault || !uselessStatusOnDefault(ctx, user, battle.battlerAt(viable[0]), slotFor)) continue;
                }
                numViable = 1;
                viable[0] = def;
                mostPoints = points[def];
                usingDefault = false;
                if (move != null && !move.statusMove()) {
                    statusOption = false;
                    mostDmgTarget = def;
                    if (d != null && d.foe != user.foe) {
                        if (tryKnockOut) mostKnockouts = targetsKnockedOut(ctx, move, user, d);
                        if (tryMostDamage || tryDangerous) mostDamage = totalDamage(ctx, move, user, d);
                    }
                } else {
                    statusOption = true;
                }
            }
        }
        int target = viable[ctx.random() % numViable];                                          // :826
        Battler t = battle.battlerAt(target);
        if (t != null && t.foe == user.foe && mostPoints < 0) {                                 // never target your partner if it is a bad idea
            for (int i = 0; i < n; i++) {
                Battler f = battle.battlerAt(i);
                if (f != null && f.foe != user.foe && !f.fainted()) {
                    target = i;
                    break;
                }
            }
        }
        if (slotFor[target] >= Battler.MOVES_MAX) return null;
        return new Choice(slotFor[target], target);
    }

    private static boolean uselessStatusOnDefault(AiCtx ctx, Battler user, Battler def, int[] slotFor) {
        if (def == null) return false;
        BattleMove m = moveAt(user, slotFor[def.index]);
        return def.foe != user.foe && m != null && m.statusMove() && selected(ctx.battle, user, m) && partnerWillKOTarget(ctx, user, def);
    }

    /** :669-700 TRY_HIT_DANGEROUS_TARGET. 0 skip, 1 add, 2 replace, 3 compare the damage. */
    private static int dangerous(AiCtx ctx, Battler user, Battle battle, Battler thisFoe, Battler bestFoe) {
        boolean thisKO = AiCalc.canKnockOut(ctx, thisFoe, user);
        boolean bestKO = bestFoe != null && AiCalc.canKnockOut(ctx, bestFoe, user);
        if (!thisKO && bestKO) return 0;
        if (thisKO && bestKO) {
            boolean thisMega = thisFoe.pokemon != null && thisFoe.isMega();
            boolean bestMega = bestFoe.pokemon != null && bestFoe.isMega();
            if (!thisMega && bestMega) return 0;
            if (thisMega && !bestMega) return 2;
            return 1;
        }
        if (!thisKO && !bestKO) return 3;                                                       // neither foe can KO the AI: TRY_HIT_MOST_DAMAGE
        return 2;
    }

    /** CANT_KNOCK_OUT_EITHER_TARGET (:719-745). */
    private static int cantKnockOut(AiCtx ctx, Battler user, Battle battle, Battler d, Battler best,
                                    int thisDamage, int mostDamage, boolean tryDangerous, boolean tryMostDamage) {
        if (tryDangerous && thisDamage >= 50 && mostDamage >= 50) {
            int v = dangerous(ctx, user, battle, d, best);
            if (v != 3) return v;
        }
        return damageVerdict(thisDamage, mostDamage, tryMostDamage);
    }

    /** TRY_HIT_MOST_DAMAGE (:733). */
    private static int damageVerdict(int thisDamage, int mostDamage, boolean tryMostDamage) {
        if (!tryMostDamage) return 1;
        if (thisDamage < mostDamage) return 0;
        return thisDamage == mostDamage ? 1 : 2;
    }
}
