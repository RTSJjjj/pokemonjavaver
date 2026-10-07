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

    /**
     * {@code @id} (PokeBattle_Move.rb:29 {@code @id = move.id}): the PBS move
     * id, 0 for a blank slot ({@code PBMove.new(0)}) and -1 for the synthetic
     * Struggle ({@code Move_Effects_Generic.rb:57}).
     */
    public int id() {
        return move == null ? 0 : move.id;
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

    /** Move priority (PBS Priority column); ties break on Speed. */
    public int priority() {
        return move == null ? 0 : move.priority;
    }

    /** Additional-effect chance (PBS EffectChance). */
    public int additionalChance() {
        return move == null ? 0 : Math.max(0, move.effectChance);
    }

    /** The move's function code (the {@code PokeBattle_Move_XXX} table). */
    public String function() {
        return move == null ? "000" : move.function;
    }

    /**
     * The move's PBS flags (PokeBattle_Move:114-129 reads them by letter, e.g.
     * {@code h} = {@code highCriticalRate?}).
     */
    public String flags() {
        return move == null ? "" : move.flags;
    }

    /** A move that deals no damage (power 0 / Status); the engine skips it. */
    public boolean statusMove() {
        return move == null || move.power <= 0
                || "Status".equalsIgnoreCase(move.category);
    }

    public boolean physical() {
        return move != null && "Physical".equalsIgnoreCase(move.category);
    }

    /** The PBS Category column, verbatim ({@code Physical}/{@code Special}/{@code Status}). */
    public String category() {
        return move == null ? null : move.category;
    }

    /**
     * The PBS Target column, verbatim (e.g. {@code "NearOther"}). The plugin's
     * {@code @target} is the matching {@code PBTargets} integer
     * ({@code PokeBattle_Move:40 @target = moveData[MOVE_TARGET]}), so the
     * strategy layer maps this name through {@code PBTargets.fromName(String)}.
     */
    public String target() {
        return move == null ? null : move.target;
    }

    // ------------------------------------------------------------------
    // Per-move mutable battle state (PokeBattle_Move's attr_accessor,
    // PokeBattle_Move.rb:11-19). These are per MOVE, not per function code, so
    // they live here rather than on the MoveEffect strategy (which is shared by
    // every move with the same function code).
    // ------------------------------------------------------------------

    /**
     * {@code @calcType} (PokeBattle_Move.rb:17/43): the type the move is
     * actually treated as for this use. {@code null} is the plugin's
     * {@code -1} ("not overridden"), which every {@code ret<0} test in
     * Move_Usage_Calculations reads.
     */
    private String calcType;

    /** {@code @powerBoost} (PokeBattle_Move.rb:18/44; set by Aerilate etc.). */
    private boolean powerBoost;

    /** {@code @snatched} (PokeBattle_Move.rb:19/45). */
    private boolean snatched;

    /**
     * {@code @totalpp} (PokeBattle_Move.rb:12/66-70): normally undefined, and
     * {@code totalpp} then falls back to {@code @realMove.totalpp}. 0 means
     * "undefined" here; the caller falls back to the move slot's max PP.
     */
    private int totalppOverride;

    /** {@code @calcType}; {@code null} = the plugin's {@code -1}. */
    public String calcType() {
        return calcType;
    }

    public void setCalcType(String type) {
        this.calcType = type;
    }

    /** {@code @powerBoost}. */
    public boolean powerBoost() {
        return powerBoost;
    }

    public void setPowerBoost(boolean value) {
        this.powerBoost = value;
    }

    /** {@code @snatched}. */
    public boolean snatched() {
        return snatched;
    }

    public void setSnatched(boolean value) {
        this.snatched = value;
    }

    /**
     * {@code totalpp} (PokeBattle_Move.rb:66-70): the override when one was set,
     * otherwise 0 - the caller then uses the move slot's max PP, which is what
     * {@code @realMove.totalpp} resolves to.
     */
    public int totalpp() {
        return Math.max(0, totalppOverride);
    }

    public void setTotalpp(int value) {
        this.totalppOverride = value;
    }
}
