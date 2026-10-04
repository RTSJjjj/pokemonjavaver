package pokemon.runtime.map;

/**
 * R6.15: keeps the hero invisible during a door <em>arrival</em> page.
 *
 * <p>Pokémon Essentials arrival pages are autorun pages that start with "is
 * the player on me?" and then hide the hero (208[0]), play the door, show the
 * hero (208[1]) and only then walk them out of the doorway. The user asked
 * that the hero must not become visible before that walk-out step actually
 * starts, so the early 208[1] is remembered here and applied when the forced
 * route takes its first step. Safety rails release the hold when the page
 * ends, when the hero moves on its own, or after {@link #MAX_HOLD_SECONDS}, so
 * the hero can never be stranded invisible.</p>
 */
public final class DoorShowHold {

    /** Never keep the hero hidden longer than this without a walk-out step. */
    public static final float MAX_HOLD_SECONDS = 3f;

    private boolean active;
    private boolean showRequested;
    private float heldSeconds;

    /** The door page hid the hero (208[0]), or the map screen pre-hid them. */
    public void hide() {
        active = true;
        showRequested = false;
        heldSeconds = 0f;
    }

    /** The door page asked for the hero back (208[1]); may be deferred. */
    public void requestShow() {
        if (active) {
            showRequested = true;
        }
    }

    public boolean isHolding() {
        return active;
    }

    /**
     * Advances the hold.
     *
     * @param pageRunning the door page is still executing
     * @param heroMoving  the hero is currently playing a forced walk step
     * @return true on the frame the hold ends and the hero must be shown again
     */
    public boolean update(float delta, boolean pageRunning, boolean heroMoving) {
        if (!active) {
            return false;
        }
        heldSeconds += Math.max(0f, delta);
        if ((showRequested && heroMoving) || !pageRunning || heldSeconds >= MAX_HOLD_SECONDS) {
            clear();
            return true;
        }
        return false;
    }

    /** Drops the hold without reporting a release (map change / new page). */
    public void clear() {
        active = false;
        showRequested = false;
        heldSeconds = 0f;
    }
}
