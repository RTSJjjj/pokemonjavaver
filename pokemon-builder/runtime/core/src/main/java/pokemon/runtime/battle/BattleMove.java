package pokemon.runtime.battle;

import pokemon.runtime.pokemon.PbsData;

/**
 * Stage 3 / P2: one move as used in a battle. Wraps the {@code PbsData.Move}
 * with the category questions the engine asks; effects that depend on the
 * move's function code arrive with the plugin features (P5).
 */
public final class BattleMove {

    public final PbsData.Move move;

    public BattleMove(PbsData.Move move) {
        this.move = move;
    }

    public String internalName() {
        return move == null ? null : move.internalName;
    }

    public String name() {
        return move == null ? "?" : move.name;
    }

    public String type() {
        return move == null ? null : move.type;
    }

    public int power() {
        return move == null ? 0 : Math.max(0, move.power);
    }

    public int accuracy() {
        return move == null ? 0 : move.accuracy;
    }

    /** A move that deals no damage (power 0 / Status); the engine skips it. */
    public boolean statusMove() {
        return move == null || move.power <= 0
                || "Status".equalsIgnoreCase(move.category);
    }

    public boolean physical() {
        return move != null && "Physical".equalsIgnoreCase(move.category);
    }
}
