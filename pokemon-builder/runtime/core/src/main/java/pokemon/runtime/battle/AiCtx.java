package pokemon.runtime.battle;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;

/**
 * One AI decision's working data: CFRU's {@code AI_THINKING_STRUCT} ({@code ai_master.c:112-163}) plus the
 * per-turn caches of {@code gNewBS->ai} that the singles scorers read ({@code movePredictions},
 * {@code strongestMove}). Singles only: every battler has exactly one opponent.
 */
final class AiCtx {
    /** {@code AI_SCRIPT_CHECK_BAD_MOVE}: the Negatives script. */
    static final int FLAG_CHECK_BAD_MOVE = 1;
    /** {@code AI_SCRIPT_SEMI_SMART}. */
    static final int FLAG_SEMI_SMART = 2;
    /** {@code AI_SCRIPT_CHECK_GOOD_MOVE}: the Positives script. */
    static final int FLAG_CHECK_GOOD_MOVE = 4;

    /** {@code MOVE_PREDICTION_SWITCH}: the foe is predicted to switch rather than attack. */
    static final BattleMove SWITCH = new BattleMove(null);

    final Battle battle;
    final Random rng;
    /** {@code AI_THINKING_STRUCT->aiFlags}. */
    int flags;
    /** {@code AI_THINKING_STRUCT->simulatedRNG[i]} (:152): {@code AIRandom() % 100}. */
    final int[] simulatedRng = new int[Battler.MOVES_MAX];
    /** {@code gNewBS->ai.suckerPunchOkay[bank]} (ai_master.c:1253): is a revealed Sucker Punch okay this turn. */
    boolean suckerPunchOkay;

    /** {@code gNewBS->ai.movePredictions[bankAtk][bankDef]}: absent = MOVE_NONE. */
    private final Map<Battler, BattleMove> predictions = new IdentityHashMap<>();
    /** {@code gNewBS->ai.strongestMove[bankAtk][bankDef]}: filled lazily. */
    private final Map<Battler, BattleMove> strongest = new IdentityHashMap<>();
    private final Map<Battler, Boolean> strongestKnown = new IdentityHashMap<>();

    /** {@code gNewBS->ai.bestDoublesKillingMoves/Scores}: filled lazily per (attacker, defender) pair. */
    final Map<Integer, AiDoublesScore.Killing> killing = new java.util.HashMap<>();

    AiCtx(Battle battle, Random rng, int flags) {
        this.battle = battle;
        this.rng = rng;
        this.flags = flags;
    }

    /** {@code AIRandom()}. */
    int random() {
        return rng.nextInt(Integer.MAX_VALUE);
    }

    /** The Pokemon choosing and the one it is being scored against ({@code gBankAttacker} / {@code gBankTarget}). */
    Battler user;
    Battler target;

    /** The opposing Pokemon of {@code bank} for this evaluation (singles: the other side's battler). */
    Battler foeOf(Battler bank) {
        if (user != null && target != null) {
            if (bank == user) return target;
            if (bank == target) return user;
        }
        return battle.battlerAt(bank.index ^ 1);
    }

    /** {@code GOOD_AI} (ai_negatives.c:35): {@code aiFlags > AI_SCRIPT_CHECK_BAD_MOVE}. */
    boolean goodAi() {
        return flags > FLAG_CHECK_BAD_MOVE;
    }

    /** {@code IsValidMovePrediction(bankAtk,bankDef)} (ai_util.c:3435): the move {@code atk} is predicted to use, or null. */
    BattleMove prediction(Battler atk) {
        BattleMove move = predictions.get(atk);
        return move == SWITCH ? null : move;
    }

    /** {@code IsPredictedToSwitch(bankAtk,bankDef)} (ai_util.c:3443). */
    boolean predictedToSwitch(Battler atk) {
        return predictions.get(atk) == SWITCH;
    }

    /** Whether a prediction (or a predicted switch) was stored for {@code atk}. */
    boolean hasPrediction(Battler atk) {
        return predictions.containsKey(atk);
    }

    void storePrediction(Battler atk, BattleMove move) {
        if (move == null) predictions.remove(atk);
        else predictions.put(atk, move);
    }

    BattleMove cachedStrongest(Battler atk) {
        return strongestKnown.containsKey(atk) ? strongest.get(atk) : null;
    }

    boolean hasCachedStrongest(Battler atk) {
        return strongestKnown.containsKey(atk);
    }

    void cacheStrongest(Battler atk, BattleMove move) {
        strongestKnown.put(atk, Boolean.TRUE);
        if (move == null) strongest.remove(atk);
        else strongest.put(atk, move);
    }
}
