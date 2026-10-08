package pokemon.runtime.battle;

import java.util.Random;

/**
 * CFRU {@code ai_master.c}: the move-choice harness. Every move starts at 100 (0 when it cannot be used), the
 * enabled scripts ({@link AiNegatives}, then {@link AiPositives}) rewrite each move's score, and the best score
 * wins with ties broken at random ({@code ChooseMoveOrAction_Singles}, :360-411).
 *
 * <p>This project uses one tier for every trainer ("the smartest": {@code AI_SCRIPT_CHECK_BAD_MOVE |
 * AI_SCRIPT_CHECK_GOOD_MOVE}, no difficulty options), so {@code GetAIFlags} (:165-235) reduces to that constant.
 * {@code AIScript_SemiSmart} only runs when {@code CHECK_GOOD_MOVE} is not set (ai_positives.c:2886) and is
 * therefore not part of this tier.</p>
 */
final class AiMaster {

    /** The smartest tier: Negatives + Positives. */
    static final int SMARTEST = AiCtx.FLAG_CHECK_BAD_MOVE | AiCtx.FLAG_CHECK_GOOD_MOVE;
    /** {@code AI_THINKING_STRUCT->aiFlags = 7} while predicting (ai_master.c:1190): every script on. */
    private static final int PREDICTION_FLAGS = 7;

    private AiMaster() {
    }

    /** Bonus for a signature move that was not judged useless (decision with the user: +6). */
    static final int SIGNATURE_BONUS = 6;

    /** What {@link #chooseMove} answers when no move could be scored (the caller falls back). */
    static final int NONE = -1;

    /**
     * {@code BattleAI_ChooseMoveOrAction} (ai_master.c:237) for a single battle: the move slot {@code user} uses.
     * The foe is the battler on the other side (:131 {@code gBankTarget = gBankAttacker ^ BIT_SIDE}).
     */
    static int chooseMove(Battle battle, Battler user, Random rng) {
        Battler foe = battle.battlerAt(user.index ^ 1);
        if (foe == null || foe.pokemon == null) return NONE;
        AiCtx ctx = new AiCtx(battle, rng, SMARTEST);
        // BattleAI_SetupAIData (:112): scores and simulatedRNG
        for (int i = 0; i < Battler.MOVES_MAX; i++) ctx.simulatedRng[i] = ctx.random() % 100;          // :151
        ctx.suckerPunchOkay = (ctx.random() & 1) != 0;                                                 // UpdateStrongestMoves :1253
        // CalculateAIPredictions (:1008): PredictMovesForBanks
        predictMoves(ctx, foe, user);
        predictMoves(ctx, user, foe);

        int[] score = new int[Battler.MOVES_MAX];
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            score[i] = AiCalc.usable(ctx, user, i) ? 100 : 0;                                          // :122-127,:146 limited moves score 0
        }
        // ChooseMoveOrAction_Singles: run each enabled script over every move (BattleAI_DoAIProcessing :875)
        for (int script = 0; script < 3; script++) {
            if ((ctx.flags & (1 << script)) == 0) continue;                                            // :367 while (flags != 0)
            for (int i = 0; i < Battler.MOVES_MAX; i++) {
                BattleMove move = user.moveSlot(i);
                if (move != null && score[i] > 0) {                                                    // :899 moveConsidered != MOVE_NONE && score > 0
                    if (script == 0) score[i] = AiNegatives.score(ctx, user, foe, move, score[i]);
                    else if (script == 2) score[i] = AiPositives.score(ctx, user, foe, move, score[i]);
                    // script 1 = AIScript_SemiSmart: not part of the smartest tier
                } else {
                    score[i] = 0;                                                                      // :913
                }
            }
        }
        // The project's own rule (not CFRU): an original move that only the user's evolution line can learn gets a large bonus,
        // unless the scripts judged it useless (a score below 100 is a penalised move).
        if (battle.pbs() != null && user.pokemon.species != null) {
            for (int i = 0; i < Battler.MOVES_MAX; i++) {
                BattleMove m = user.moveSlot(i);
                if (m != null && score[i] >= 100 && AiSignature.isSignature(battle.pbs(), user.pokemon.species.internalName, m.internalName())) {
                    score[i] = Math.min(score[i] + SIGNATURE_BONUS, 255);
                }
            }
        }
        // :380-411 pick the best, ties at random
        int[] best = new int[Battler.MOVES_MAX];
        int numBest = 1;
        best[0] = 0;
        for (int i = 1; i < Battler.MOVES_MAX; i++) {
            if (user.moveSlot(i) == null) continue;                                                    // :390 moves[i] != MOVE_NONE
            if (score[best[0]] == score[i]) {
                best[numBest++] = i;
            } else if (score[best[0]] < score[i]) {
                numBest = 1;
                best[0] = i;
            }
        }
        return best[ctx.random() % numBest];                                                           // :411
    }

    /**
     * {@code PredictMovesForBanks} (ai_master.c:1146-1221) for one attacker against one defender: run both scripts
     * on every usable move with every script enabled and store the best move, or a switch when even the best
     * scores below 100.
     * 登记: the multi-turn lock-in (:1172) and Truant/recharge (:1163) shortcuts are only applied for recharge and sleep.
     */
    private static void predictMoves(AiCtx ctx, Battler atk, Battler def) {
        if (atk.fainted() || def.fainted()) return;
        if (atk.effects.intVal(PBEffects.Battler.HyperBeam) > 0) {                                      // :1163 STATUS2_RECHARGE
            ctx.storePrediction(atk, null);
            return;
        }
        boolean asleep = atk.hasStatus("SLEEP") && atk.statusCount > 1;                                 // :1168 IsBankAsleep
        if (asleep && !AiCalc.moveFunctionInMoveset(atk, "0B4", "011")) {                                // no Sleep Talk (0B4) / Snore (011)
            ctx.storePrediction(atk, null);
            return;
        }
        int backupFlags = ctx.flags;
        ctx.flags = PREDICTION_FLAGS;                                                                   // :1186
        int[] viability = new int[Battler.MOVES_MAX];
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove move = atk.moveSlot(i);
            if (move == null || !AiCalc.usable(ctx, atk, i)) continue;                                  // :1197
            viability[i] = AiNegatives.score(ctx, atk, def, move, 100);                                 // :1203
            viability[i] = AiPositives.score(ctx, atk, def, move, viability[i]);                        // :1204
        }
        ctx.flags = backupFlags;
        int max = 0;
        for (int i = 1; i < Battler.MOVES_MAX; i++) if (viability[i] > viability[max]) max = i;         // GetMaxByteIndexInList
        if (viability[max] < 100) {                                                                      // :1218
            ctx.storePrediction(atk, AiCtx.SWITCH);                                                      // StoreSwitchPrediction
            return;
        }
        int[] ties = new int[Battler.MOVES_MAX];
        int n = 0;
        for (int i = 0; i < Battler.MOVES_MAX; i++) if (viability[i] == viability[max]) ties[n++] = i;
        ctx.storePrediction(atk, atk.moveSlot(ties[ctx.random() % n]));                                  // :1221
    }
}
