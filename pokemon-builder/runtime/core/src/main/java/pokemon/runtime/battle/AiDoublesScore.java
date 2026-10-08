package pokemon.runtime.battle;

import java.util.ArrayList;
import java.util.List;

/**
 * CFRU's doubles scoring helpers: {@code UpdateBestDoubleKillingMoveScore} / {@code GetDoubleKillingScore}
 * ({@code ai_util.c:796-1130}), {@code gDoublesDamageViabilityMapping} and the {@code IncreaseDoubles*Viability}
 * functions ({@code ai_advanced.c:60-180, 3010-3200}).
 *
 * <p>A "doubles" battle here is CFRU's: not a 1v1 ({@code IS_DOUBLE_BATTLE}, {@code ai_advanced.c:186}). Foes are the
 * living opposing battlers and the partner the first other living battler of the same side; CFRU has exactly two of each,
 * so in a 3v1 Boss fight the boss simply sees three foes and no partner (sums run over all foes, which equals CFRU's
 * {@code bankDef + bankDefPartner} when there are two).</p>
 *
 * <p>登记: Z-moves and Dynamax (no such mechanic in this project).</p>
 */
final class AiDoublesScore {
    static final int HIT_FOE = 4;                // DOUBLES_INCREASE_HIT_FOE
    static final int KO_FOE = 2;                 // DOUBLES_INCREASE_KO_FOE
    static final int STRONGEST_MOVE = 1;         // DOUBLES_INCREASE_STRONGEST_MOVE
    static final int DECREASE_HIT_PARTNER = 3;   // DOUBLES_DECREASE_HIT_PARTNER
    static final int DECREASE_DESTINY_BOND = 2;  // DOUBLES_DECREASE_DESTINY_BOND
    static final int BEST_KO_SCORE = HIT_FOE + HIT_FOE + KO_FOE + KO_FOE;   // BEST_DOUBLES_KO_SCORE (12)

    static final int CHECK_REGULAR_PROTECTION = 0x1, CHECK_QUICK_GUARD = 0x2, CHECK_WIDE_GUARD = 0x4,
            CHECK_CRAFTY_SHIELD = 0x8, CHECK_MAT_BLOCK = 0x10;

    /** {@code gDoublesDamageViabilityMapping[class - FIGHT_CLASS_DOUBLES_ALL_OUT_ATTACKER][score]}; index 0 is unused (0). */
    private static final int[][] MAPPING = {
        {0, 9, 9, 9, 9, 10, 11, 11, 13, 14, 16, 17, 19},   // ALL_OUT_ATTACKER
        {0, 4, 4, 4, 4, 9, 11, 11, 12, 13, 15, 16, 19},    // SETUP_ATTACKER
        {0, 3, 3, 3, 3, 9, 10, 10, 11, 14, 15, 16, 18},    // TRICK_ROOM_ATTACKER
        {0, 1, 1, 1, 1, 1, 2, 2, 3, 4, 5, 6, 18},          // TRICK_ROOM_SETUP
        {0, 1, 1, 1, 1, 1, 1, 1, 2, 3, 8, 9, 18},          // UTILITY
        {0, 4, 4, 4, 4, 4, 4, 4, 5, 6, 7, 8, 18},          // PHAZING
        {0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 7, 9, 16},          // TEAM_SUPPORT
        {0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 7, 9, 16},          // TOTAL_TEAM_SUPPORT
    };

    private AiDoublesScore() {
    }

    // ------------------------------------------------------------------
    // Sides
    // ------------------------------------------------------------------

    /** The first other living battler on {@code b}'s side ({@code PARTNER(bank)}), or null. */
    static Battler partner(Battle battle, Battler b) {
        for (Battler o : battle.eachSameSideBattler(b.index)) if (o != b && !o.fainted()) return o;
        return null;
    }

    /** The living opposing battlers (CFRU: {@code FOE(bank)} and its partner). */
    static List<Battler> foes(Battle battle, Battler b) {
        List<Battler> list = new ArrayList<>();
        for (Battler o : battle.eachBattler()) if (o.foe != b.foe && !o.fainted()) list.add(o);
        return list;
    }

    /** {@code IS_DOUBLE_BATTLE} (ai_advanced.c:186): not a 1v1 - two foes alive or a partner alive. */
    static boolean isDouble(Battle battle, Battler atk) {
        if (battle.singleBattle()) return false;
        return foes(battle, atk).size() >= 2 || partner(battle, atk) != null;
    }

    /** {@code MOVE_TARGET_ALL}-style: hits every other battler, including the partner. */
    static boolean hitsAll(Battle battle, Battler atk, BattleMove move) {
        int t = AiCalc.fx(move).pbTarget(move, atk);
        return t == PBTargets.AllNearOthers || t == PBTargets.AllBattlers;
    }

    /** {@code MOVE_TARGET_SPREAD}: hits several foes. */
    static boolean spread(Battle battle, Battler atk, BattleMove move) {
        int t = AiCalc.fx(move).pbTarget(move, atk);
        return t == PBTargets.AllNearFoes || t == PBTargets.AllNearOthers || t == PBTargets.AllFoes;
    }

    /** {@code HasProtectionMoveInMoveset(bank,checkType)} (ai_util.c:3801). */
    static boolean hasProtectionMove(AiCtx ctx, Battler b, int checkType) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = b.moveSlot(i);
            if (m == null || !AiCalc.usable(ctx, b, i)) continue;
            if (AiCalc.oneOf(m, AiCalc.PROTECT) && (checkType & CHECK_REGULAR_PROTECTION) != 0) return true;
            if (AiCalc.oneOf(m, "0AB") && (checkType & CHECK_QUICK_GUARD) != 0) return true;
            if (AiCalc.oneOf(m, "0AC") && (checkType & CHECK_WIDE_GUARD) != 0) return true;
            if (AiCalc.oneOf(m, "14A") && (checkType & CHECK_CRAFTY_SHIELD) != 0) return true;
            if (AiCalc.oneOf(m, "149") && (checkType & CHECK_MAT_BLOCK) != 0 && AiCalc.firstTurn(b)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // UpdateBestDoubleKillingMoveScore / GetDoubleKillingScore
    // ------------------------------------------------------------------

    /** One attacker/defender pair's result: the best move and its score on every battler index. */
    static final class Killing {
        BattleMove best;
        int[] scores;
        boolean recalculated;
    }

    private static Killing killing(AiCtx ctx, Battler atk, Battler def) {
        Killing k = ctx.killing.get(atk.index * 16 + def.index);
        if (k == null) {
            k = new Killing();
            update(ctx, atk, def, k);
            ctx.killing.put(atk.index * 16 + def.index, k);
        }
        return k;
    }

    /** {@code gNewBS->ai.bestDoublesKillingMoves[bankAtk][bankDef]}. */
    static BattleMove bestKillingMove(AiCtx ctx, Battler atk, Battler def) {
        return killing(ctx, atk, def).best;
    }

    /** {@code GetDoubleKillingScore(move,bankAtk,bankDef)} (ai_util.c:1113). */
    static int doubleKillingScore(AiCtx ctx, BattleMove move, Battler atk, Battler def) {
        Battle battle = ctx.battle;
        Battler partner = partner(battle, atk);
        Killing k = killing(ctx, atk, def);
        BattleMove partnerChosen = partner == null ? null : battle.chosenMove(partner.index);
        if (partnerChosen != null && AiCalc.oneOf(partnerChosen, AiCalc.PROTECT) && hitsAll(battle, atk, move) && !k.recalculated) {
            k.recalculated = true;                  // the partner protects from the spread move: recalculate
            update(ctx, atk, def, k);
        }
        if (move != k.best) return 0;
        int sum = 0;
        for (Battler f : foes(battle, atk)) sum += k.scores[f.index];
        if (partner != null) sum += k.scores[partner.index];
        return sum;
    }

    /** {@code UpdateBestDoubleKillingMoveScore(bankAtk,bankDef,bankAtkPartner,bankDefPartner,bestMoveScores,bestMove)} (ai_util.c:796-1111). */
    private static void update(AiCtx ctx, Battler atk, Battler def, Killing out) {
        Battle battle = ctx.battle;
        int n = battle.maxBattlerIndex() + 1;
        int[][] moveScores = new int[Battler.MOVES_MAX][n];
        Battler partner = partner(battle, atk);
        List<Battler> foes = foes(battle, atk);
        // CFRU's foes[] = {bankDef, bankDefPartner}: the examined target first
        List<Battler> order = new ArrayList<>();
        order.add(def);
        for (Battler f : foes) if (f != def) order.add(f);
        boolean[] partnerHandling = new boolean[order.size()];

        BattleMove partnerMove = null;
        int partnerTarget = -1;
        if (partner != null && battle.chosenMove(partner.index) != null) {
            partnerMove = battle.chosenMove(partner.index);
            Object[] pc = battle.choices(partner.index);
            partnerTarget = pc[3] instanceof Integer ? (Integer) pc[3] : -1;
        }
        int foesAlive = foes.size();

        boolean foeHasWideGuard = false;
        if (ctx.random() % 100 < 75) {                                                      // 75 % chance the AI cares about Wide Guard this round
            for (Battler f : order) {
                if (!f.fainted() && hasProtectionMove(ctx, f, CHECK_WIDE_GUARD) && !AiPositiveHelpers.isIncapacitated(f)) {
                    foeHasWideGuard = true;
                    break;
                }
            }
        }

        boolean partnerHitsBothFoes = false;
        boolean partnerWillAttack = partner != null && !AiPositiveHelpers.isIncapacitated(partner) && partnerMove != null;
        boolean partnerWillUseDamagingMove = partnerWillAttack && !partnerMove.statusMove();
        if (partnerWillAttack && foesAlive >= 2) {
            partnerHitsBothFoes = spread(battle, partner, partnerMove);
            if (!partnerHitsBothFoes) {
                Battler pt = partnerTarget >= 0 ? battle.battlerAt(partnerTarget) : null;
                if (pt != null && pt != partner && AiCalc.knocksOutXHits(ctx, partnerMove, partner, pt, 1)) {
                    for (int j = 0; j < order.size(); j++) if (order.get(j) == pt) partnerHandling[j] = true;
                }
            } else {
                for (int j = 0; j < order.size(); j++) {
                    if (AiCalc.knocksOutXHits(ctx, partnerMove, partner, order.get(j), 1)) partnerHandling[j] = true;
                }
                boolean all = true;
                for (boolean h : partnerHandling) all &= h;
                if (all && order.size() >= 2 && AiCalc.priorityCalc(battle, partner, partnerMove) <= 0) {
                    int partnerSpeed = AiCalc.speed(partner);
                    boolean slower = false;
                    for (int j = 0; j < Math.min(2, order.size()); j++) slower |= partnerSpeed < AiCalc.speed(order.get(j));
                    if (slower) partnerHandling[0] = false;                                 // still consider bankDef a viable single target
                }
            }
        }

        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove move = atk.moveSlot(i);
            if (move == null) break;
            if (move.statusMove()) continue;
            if (!AiCalc.usable(ctx, atk, i)) continue;                                      // move limitations: pretend it does not exist
            boolean spreadMove = spread(battle, atk, move);
            if (foeHasWideGuard && spreadMove) continue;                                    // MOVE_LOOP_END: pretend this move sucks
            for (int j = 0; j < order.size(); j++) {
                Battler cur = order.get(j);
                if (cur.fainted() || !(j == 0 || spreadMove) || partnerHandling[j]) continue;
                int dmg = AiCalc.finalDamage(ctx, move, atk, cur, 1);
                if (dmg <= 0) continue;
                moveScores[i][cur.index] += HIT_FOE;
                if (move.function().equals("06C") && partnerWillUseDamagingMove                  // Super Fang after a partner attack on this target
                        && (partnerTarget == cur.index || partnerHitsBothFoes)
                        && AiCalc.wouldHitBefore(battle, partnerMove, partner, move, atk)) {
                    moveScores[i][cur.index] -= 2;
                }
                if (AiCalc.knocksOutXHits(ctx, move, atk, cur, 1)) {
                    moveScores[i][cur.index] += KO_FOE;
                    if (cur.effects.intVal(PBEffects.Battler.DestinyBond) > 0) moveScores[i][cur.index] -= DECREASE_DESTINY_BOND;
                } else if (AiCalc.isStrongestMove(ctx, move, atk, cur)) {
                    moveScores[i][cur.index] += STRONGEST_MOVE;
                } else if (!goodSideEffect(ctx, move, atk, cur) && foesAlive >= 2) {         // DEFAULT_CHECK
                    if (dmg < cur.maxHp() / 4) moveScores[i][cur.index] -= HIT_FOE - 1;
                    else if (dmg < cur.maxHp() / 3) moveScores[i][cur.index] -= HIT_FOE - 2;
                }
            }
            if (hitsAll(battle, atk, move) && partner != null && rangeMoveCanHurtPartner(ctx, move, atk, partner)) {
                boolean awake = !(partner.hasStatus("SLEEP") && partner.statusCount > 1) && !partner.hasStatus("FROZEN")
                        && !(partner.hasActiveAbility("TRUANT") && partner.effects.truthy(PBEffects.Battler.Truant));
                if (awake) {
                    if (partnerMove != null) {
                        boolean protects = AiCalc.oneOf(partnerMove, AiCalc.PROTECT) || AiCalc.oneOf(partnerMove, "0E8", "0AC")
                                || (AiCalc.oneOf(partnerMove, "149") && AiCalc.firstTurn(partner));   // EFFECT_PROTECT except Quick Guard / Crafty Shield
                        if (protects) {
                            // the partner is protecting, so the spread move is fine
                        } else if (isSemiInvulnerableMove(partnerMove) && !AiCalc.moveWouldHitFirst(ctx, move, atk, partner)) {
                            // the partner has left the field by the time the move lands
                        } else {
                            moveScores[i][partner.index] -= DECREASE_HIT_PARTNER;
                        }
                    } else if (!hasProtectionMove(ctx, partner, CHECK_WIDE_GUARD | CHECK_MAT_BLOCK)
                            || (isProtect(AiCalc.lastUsedMove(battle, partner)) && partner.effects.intVal(PBEffects.Battler.ProtectRate) > 1)) {
                        moveScores[i][partner.index] -= DECREASE_HIT_PARTNER;
                    }
                } else {
                    moveScores[i][partner.index] -= DECREASE_HIT_PARTNER;
                }
            }
        }

        int bestIndex = 0;
        for (int i = 1; i < Battler.MOVES_MAX; i++) {
            int cur = total(moveScores[i], foes, partner);
            int best = total(moveScores[bestIndex], foes, partner);
            if (cur > best) {
                bestIndex = i;
            } else if (cur == best && atk.moveSlot(i) != null && atk.moveSlot(bestIndex) != null) {
                BattleMove thisMove = atk.moveSlot(i);
                BattleMove bestMove = atk.moveSlot(bestIndex);
                if (hitsAll(battle, atk, bestMove) && !hitsAll(battle, atk, thisMove) && spread(battle, atk, thisMove)
                        && partner != null && rangeMoveCanHurtPartner(ctx, bestMove, atk, partner)) {
                    bestIndex = i;                    // an all-hitting move and a both-foes move tie: use the one that only hits both foes
                }
            }
        }
        out.scores = moveScores[bestIndex];
        out.best = atk.moveSlot(bestIndex);
    }

    private static int total(int[] scores, List<Battler> foes, Battler partner) {
        int sum = 0;
        for (Battler f : foes) sum += scores[f.index];
        if (partner != null) sum += scores[partner.index];
        return sum;
    }

    private static boolean isProtect(BattleMove m) {
        return m != null && AiCalc.oneOf(m, AiCalc.PROTECT);
    }

    private static boolean isSemiInvulnerableMove(BattleMove m) {
        return AiCalc.oneOf(m, "0C9", "0CA", "0CB", "0CC", "0CD", "0CE", "14D");   // EFFECT_SEMI_INVULNERABLE: the two-turn hiding moves
    }

    /** {@code RangeMoveCanHurtPartner(move,bankAtk,bankAtkPartner)} (ai_util.c). */
    static boolean rangeMoveCanHurtPartner(AiCtx ctx, BattleMove move, Battler atk, Battler partner) {
        return partner != null && !partner.fainted() && !partner.hasActiveAbility("TELEPATHY")
                && !(partner.semiInvulnerable() && AiCalc.moveWouldHitFirst(ctx, move, atk, partner))
                && !AiCalc.noEffect(ctx.battle, atk, partner, move);
    }

    /**
     * The "these move effects are good even if they do minimal damage" switch (ai_util.c:938-1019): true when the
     * move's side effect is worth using it (so no damage penalty applies).
     */
    private static boolean goodSideEffect(AiCtx ctx, BattleMove move, Battler atk, Battler cur) {
        if (!AiCalc.isSideEffectHit(move)) return false;
        boolean calc = AiCalc.secondaryEffectChance(move, atk) >= 50;
        String f = move.function();
        switch (f) {
            case "00F": case "010":                                                          // EFFECT_FLINCH_HIT
                return AiCalc.moveWouldHitFirst(ctx, move, atk, cur);
            case "007": return calc && !AiPositiveEffects.badIdeaToParalyze(ctx, cur, atk);
            case "00A": return calc && !AiPositiveEffects.badIdeaToBurn(ctx, cur, atk);
            case "00C": return calc && !badIdeaToFreeze(ctx, cur, atk);
            case "005": case "006": return calc && !AiPositiveEffects.badIdeaToPoison(ctx, cur, atk);
            case "042": return calc && AiPositiveHelpers.goodIdeaToLowerAttack(ctx, cur, atk, move);
            case "043": return calc && AiPositiveHelpers.goodIdeaToLowerDefense(ctx, cur, atk, move);
            case "045": return calc && AiPositiveHelpers.goodIdeaToLowerSpAtk(ctx, cur, atk, move);
            case "046": case "04F": return calc && AiPositiveHelpers.goodIdeaToLowerSpDef(ctx, cur, atk, move);
            case "044": return calc && AiPositiveHelpers.goodIdeaToLowerSpeed(ctx, cur, atk, move, 1);
            case "047": return calc && AiPositiveHelpers.goodIdeaToLowerAccuracy(ctx, cur, atk, move);
            case "048": return calc && AiPositiveHelpers.goodIdeaToLowerEvasion(ctx, cur, atk);
            case "013": return calc && AiCalc.canBeConfused(ctx.battle, cur, atk);
            case "01C": case "01F": case "020": return calc;                                  // the self stat-raising hits
            default: return false;
        }
    }

    /** {@code CanBeFrozen(bankDef,bankAtk,TRUE)}. */
    static boolean canBeFrozenBy(AiCtx ctx, Battler def, Battler atk) {
        return def.pbCanFreeze(atk, false, null);
    }

    /** {@code BadIdeaToFreeze(bankDef,bankAtk)} (ai_util.c:2998). */
    static boolean badIdeaToFreeze(AiCtx ctx, Battler def, Battler atk) {
        return !def.pbCanFreeze(atk, false, null)
                || def.hasActiveItem("ASPEARBERRY") || def.hasActiveItem("LUMBERRY")
                || (def.hasActiveAbility("SYNCHRONIZE") && atk.pbCanFreeze(def, false, null))
                || (def.hasActiveAbility("NATURALCURE") && AiUtil.benchAlive(ctx.battle, def) > 0)
                || unfreezingMoveInMoveset(def);
    }

    /** {@code UnfreezingMoveInMoveset(bank)}: a move that thaws the user (flag g). */
    private static boolean unfreezingMoveInMoveset(Battler b) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = b.moveSlot(i);
            if (m != null && AiCalc.has(m, 'g')) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Viability increases (ai_advanced.c:3010-3200)
    // ------------------------------------------------------------------

    private static int row(int cls) {
        return cls - AiCalc.CLASS_D_ALL_OUT_ATTACKER;
    }

    /** {@code IncreaseDoublesDamageViability(&viability,class,bankAtk,bankDef,move)} (ai_advanced.c:3180). */
    static int increaseDamage(AiCtx ctx, int viability, int cls, Battler atk, Battler def, BattleMove move) {
        if (!AiCalc.classDoublesSpecific(cls)) cls = AiCalc.CLASS_D_ALL_OUT_ATTACKER;        // 0xFF: dumb AI
        return Math.min(viability + MAPPING[row(cls)][doubleKillingScore(ctx, move, atk, def)], 255);
    }

    /** {@code IncreaseDoublesDamageViabilityToScore(&viability,class,score,bankAtk,bankDef)} (ai_advanced.c:3191). */
    static int increaseDamageToScore(AiCtx ctx, int viability, int cls, int score, Battler atk, Battler def) {
        Battle battle = ctx.battle;
        Battler partner = partner(battle, atk);
        if (!AiCalc.classDoublesSpecific(cls)) cls = AiCalc.CLASS_D_ALL_OUT_ATTACKER;
        if (partner != null && battle.chosenMove(partner.index) != null) {
            BattleMove partnerMove = battle.chosenMove(partner.index);
            Object[] pc = battle.choices(partner.index);
            int partnerTarget = pc[3] instanceof Integer ? (Integer) pc[3] : -1;
            if (def.index == partnerTarget && partnerTarget != partner.index
                    && AiCalc.knocksOutXHits(ctx, partnerMove, partner, def, 1)) {
                return Math.min(viability + MAPPING[row(cls)][1], 255);                      // minimal score: the partner already KOs it
            }
        }
        return Math.min(viability + MAPPING[row(cls)][score], 255);
    }

    /** {@code IncreaseHelpingHandViability(&viability,class)} (ai_advanced.c:3010). */
    static int increaseHelpingHand(int viability, int cls) {
        int add;
        switch (cls) {
            case AiCalc.CLASS_D_ALL_OUT_ATTACKER: add = 6; break;
            case AiCalc.CLASS_D_SETUP_ATTACKER: case AiCalc.CLASS_D_TRICK_ROOM_ATTACKER: case AiCalc.CLASS_D_PHAZING: add = 1; break;
            case AiCalc.CLASS_D_TRICK_ROOM_SETUP: add = 15; break;
            case AiCalc.CLASS_D_UTILITY: add = 13; break;
            case AiCalc.CLASS_D_TEAM_SUPPORT: case AiCalc.CLASS_D_TOTAL_TEAM_SUPPORT: add = 10; break;
            default: add = 0;
        }
        return Math.min(viability + add, 255);
    }

    /** {@code IncreaseHealPartnerViability(&viability,class,partner)} (ai_advanced.c:3048). */
    static int increaseHealPartner(AiCtx ctx, int viability, int cls, Battler partner) {
        if (partner != null && !partner.fainted() && partner.hp > partner.maxHp() * 2 / 3) return viability;   // only heal at 2/3 HP or less
        if (partner != null && ctx.battle.choices(partner.index)[0] != null && ":UseItem".equals(ctx.battle.choices(partner.index)[0])) {
            return viability;                                                                 // an item already heals the partner
        }
        int add;
        switch (cls) {
            case AiCalc.CLASS_D_ALL_OUT_ATTACKER: add = 7; break;
            case AiCalc.CLASS_D_SETUP_ATTACKER: case AiCalc.CLASS_D_TRICK_ROOM_ATTACKER: case AiCalc.CLASS_D_PHAZING: add = 2; break;
            case AiCalc.CLASS_D_TRICK_ROOM_SETUP: add = 12; break;
            case AiCalc.CLASS_D_UTILITY: add = 15; break;
            case AiCalc.CLASS_D_TEAM_SUPPORT: case AiCalc.CLASS_D_TOTAL_TEAM_SUPPORT: add = 13; break;
            default: add = 0;
        }
        return Math.min(viability + add, 255);
    }

    /** {@code IncreaseAllyProtectionViability(&viability,class)} (ai_advanced.c:2893). */
    static int increaseAllyProtection(int viability, int cls) {
        int add;
        switch (cls) {
            case AiCalc.CLASS_D_SETUP_ATTACKER: case AiCalc.CLASS_D_TRICK_ROOM_SETUP: case AiCalc.CLASS_D_UTILITY:
            case AiCalc.CLASS_D_PHAZING: case AiCalc.CLASS_D_TEAM_SUPPORT: case AiCalc.CLASS_D_TOTAL_TEAM_SUPPORT: add = 14; break;
            case AiCalc.CLASS_D_TRICK_ROOM_ATTACKER: add = 12; break;
            default: add = 15;                                                              // ALL_OUT_ATTACKER and the dumb AI
        }
        return Math.min(viability + add, 255);
    }

    /** {@code IncreaseTeamProtectionViability(&viability,class)} (ai_advanced.c:2934). */
    static int increaseTeamProtection(int viability, int cls) {
        int add;
        switch (cls) {
            case AiCalc.CLASS_D_ALL_OUT_ATTACKER: add = 8; break;
            case AiCalc.CLASS_D_SETUP_ATTACKER: case AiCalc.CLASS_D_TRICK_ROOM_ATTACKER: case AiCalc.CLASS_D_PHAZING: add = 3; break;
            case AiCalc.CLASS_D_TRICK_ROOM_SETUP: add = 13; break;
            case AiCalc.CLASS_D_UTILITY: case AiCalc.CLASS_D_TEAM_SUPPORT: case AiCalc.CLASS_D_TOTAL_TEAM_SUPPORT: add = 12; break;
            default: add = 0;
        }
        return Math.min(viability + add, 255);
    }

    /** {@code IncreaseTailwindViability(&viability,class,bankAtk,bankDef)} (ai_advanced.c:2972). */
    static int increaseTailwind(AiCtx ctx, int viability, int cls, Battler atk, Battler def) {
        switch (cls) {
            case AiCalc.CLASS_SWEEPER_SETUP_SCREENS: case AiCalc.CLASS_BATON_PASS: case AiCalc.CLASS_CLERIC:
            case AiCalc.CLASS_SCREENS: case AiCalc.CLASS_PHAZING:
                return AiPositiveHelpers.incStatus(ctx, viability, cls, 2, atk, def);
            case AiCalc.CLASS_D_SETUP_ATTACKER:
                return AiCalc.speed(atk) < AiCalc.speed(def) ? Math.min(viability + 18, 255) : viability;
            case AiCalc.CLASS_D_UTILITY: return Math.min(viability + 11, 255);
            case AiCalc.CLASS_D_TEAM_SUPPORT: case AiCalc.CLASS_D_TOTAL_TEAM_SUPPORT: return Math.min(viability + 18, 255);
            default:
                return AiPositiveHelpers.incStatus(ctx, viability, cls, AiCalc.classDoublesSpecific(cls) ? 3 : 1, atk, def);
        }
    }

    /** {@code IncreaseViabilityForSpeedControl(&viability,class,bankAtk,bankDef)} (ai_advanced.c:3146): the second value is CFRU's return. */
    static int[] increaseSpeedControl(AiCtx ctx, int viability, int cls, Battler atk, Battler def) {
        Battler partner = partner(ctx.battle, atk);
        boolean ret = false;
        if (!AiCalc.trickRoom(ctx.battle)
                && (AiCalc.speed(def) >= AiCalc.speed(atk) || (partner != null && AiCalc.speed(def) >= AiCalc.speed(partner)))) {
            if (cls == AiCalc.CLASS_D_UTILITY || AiCalc.classDoublesTeamSupport(cls)) {
                viability = Math.min(viability + 11, 255);
                ret = true;
            }
        }
        return new int[] {viability, ret ? 1 : 0};
    }
}
