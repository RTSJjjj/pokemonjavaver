package pokemon.runtime.state;

/**
 * {@code SafariState} (242_PBattle_Safari:1-52): the Safari Zone visit in progress. {@code start} is the reception
 * map, tile and facing the player came from ({@code pbStart}); {@code pbGoToStart} brings the player back there.
 */
public final class SafariState {
    /** {@code SAFARI_STEPS} (000_Settings:76). */
    public static final int SAFARI_STEPS = 1800;

    /** {@code @start}: map id, x, y, direction; null when no visit is running. */
    public int[] start;
    public int ballcount;
    public boolean inProgress;
    public int steps;
    public int decision;

    /** {@code pbReceptionMap} (:17-19). */
    public int receptionMap() {
        return inProgress ? start[0] : 0;
    }

    /** {@code pbStart(ballcount)} (:35-40). */
    public void begin(int mapId, int x, int y, int direction, int balls) {
        start = new int[] {mapId, x, y, direction};
        ballcount = balls;
        inProgress = true;
        steps = SAFARI_STEPS;
    }

    /** {@code pbEnd} (:42-49). */
    public void end() {
        start = null;
        ballcount = 0;
        inProgress = false;
        steps = 0;
        decision = 0;
    }
}
