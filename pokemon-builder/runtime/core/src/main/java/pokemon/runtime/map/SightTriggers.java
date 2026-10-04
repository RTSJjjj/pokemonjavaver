package pokemon.runtime.map;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * L6b: Essentials' line-of-sight triggers. Events named {@code Trainer(N)} or
 * {@code Counter(N)} with trigger 2 (event touch) start when the player is on
 * the event's facing line within N tiles (PBFieldManager's
 * {@code pbTriggeredTrainerEvents} / {@code pbTriggeredCounterEvents}):
 *
 * <ul>
 *   <li>{@code Trainer(N)} uses {@code pbEventCanReachPlayer?}: the player must
 *       be on the facing axis within N tiles and every tile strictly between
 *       must be passable (the trainer "sees" you);</li>
 *   <li>{@code Counter(N)} uses {@code pbEventFacesPlayer?}: just the facing
 *       axis within N tiles, no passability check (service counters).</li>
 * </ul>
 *
 * <p>The pure math lives here so it can be unit tested headlessly; the map
 * screen supplies positions, directions and the passability predicate.</p>
 */
public final class SightTriggers {

    public enum Kind {
        TRAINER,
        COUNTER
    }

    /** One parsed sight event (the name never changes for a map event). */
    public static final class Sight {
        public final int eventId;
        public final Kind kind;
        public final int distance;

        Sight(int eventId, Kind kind, int distance) {
            this.eventId = eventId;
            this.kind = kind;
            this.distance = distance;
        }
    }

    private static final Pattern TRAINER = Pattern.compile("trainer\\((\\d+)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COUNTER = Pattern.compile("counter\\((\\d+)\\)", Pattern.CASE_INSENSITIVE);

    private SightTriggers() {
    }

    /** Parses {@code Trainer(N)} / {@code Counter(N)}, or null for other names. */
    public static Sight parse(int eventId, String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        Matcher trainer = TRAINER.matcher(lower);
        if (trainer.find()) {
            return new Sight(eventId, Kind.TRAINER, Integer.parseInt(trainer.group(1)));
        }
        Matcher counter = COUNTER.matcher(lower);
        if (counter.find()) {
            return new Sight(eventId, Kind.COUNTER, Integer.parseInt(counter.group(1)));
        }
        return null;
    }

    /**
     * {@code pbEventCanReachPlayer?} distance half: steps from the event
     * towards its facing direction until the player's tile, or -1 when the
     * player is off the facing axis / farther than {@code distance}. The
     * returned step count excludes the event's own tile (1 = the player stands
     * right in front), exactly like the plugin's {@code realdist + 1}.
     */
    public static int lineSteps(int x, int y, int direction, int playerX, int playerY, int distance) {
        if (distance <= 0) {
            return -1;
        }
        if (x != playerX && y != playerY) {
            return -1;
        }
        int dx = direction == 6 ? 1 : direction == 4 ? -1 : 0;
        int dy = direction == 2 ? 1 : direction == 8 ? -1 : 0;
        if (dx == 0 && dy == 0) {
            return -1;
        }
        int curX = x;
        int curY = y;
        for (int step = 1; step <= distance; step++) {
            curX += dx;
            curY += dy;
            if (curX == playerX && curY == playerY) {
                return step;
            }
        }
        return -1;
    }
}
