package pokemon.runtime.battle;

import pokemon.runtime.pokemon.Pokemon;

/**
 * Stage 3 / P2: how a headless battle ended. The event layer maps it back onto
 * the party (a loss is the white-out / respawn case, a catch adds the Pokemon).
 */
public final class BattleResult {

    public enum Outcome {
        /** Every foe fainted. */
        WIN,
        /** Every player Pokemon fainted (white-out). */
        LOSS,
        /** The player fled (not used by the auto battle yet). */
        ESCAPE,
        /** A foe was caught (not used by the auto battle yet). */
        CAUGHT,
        /**
         * {@code pbDecisionOnDraw} (Battle_StartAndEnd:585): both sides are out
         * of able Pokemon, so {@code pbJudge} returns 5 (:590).
         */
        DRAW,
    }

    public final Outcome outcome;
    public final int turns;
    /** The caught Pokemon for {@link Outcome#CAUGHT}, else null. */
    public final Pokemon caught;
    /**
     * The decision code the Ruby battle returned when it is not the one the outcome implies (the Safari Zone's
     * 2 = out of Safari Balls, 242_PBattle_Safari:121-125); -1 = derive it from {@link #outcome}.
     */
    public int decision = -1;

    public BattleResult(Outcome outcome, int turns, Pokemon caught) {
        this.outcome = outcome;
        this.turns = turns;
        this.caught = caught;
    }

    public boolean won() {
        return outcome == Outcome.WIN || outcome == Outcome.CAUGHT;
    }
}
