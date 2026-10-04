package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * L6b: the Trainer(N)/Counter(N) line-of-sight math (PBFieldManager's
 * {@code pbEventCanReachPlayer?} / {@code pbEventFacesPlayer?}).
 */
class SightTriggersTest {

    @Test
    @DisplayName("Trainer(N)/Counter(N) names parse, other names do not (L6b)")
    void parsesNames() {
        SightTriggers.Sight trainer = SightTriggers.parse(7, "Trainer(3)");
        assertNotNull(trainer);
        assertEquals(SightTriggers.Kind.TRAINER, trainer.kind);
        assertEquals(3, trainer.distance);
        assertEquals(7, trainer.eventId);

        SightTriggers.Sight counter = SightTriggers.parse(9, "counter(4)");
        assertNotNull(counter);
        assertEquals(SightTriggers.Kind.COUNTER, counter.kind);
        assertEquals(4, counter.distance);

        assertNull(SightTriggers.parse(1, "Event1"));
        assertNull(SightTriggers.parse(1, ""));
        assertNull(SightTriggers.parse(1, null));
        assertNull(SightTriggers.parse(1, "Trainer"), "a name without a distance is not a sight event");
    }

    @Test
    @DisplayName("lineSteps walks the facing axis within the distance (L6b)")
    void lineSteps() {
        // Facing east from (3,5): the player at (6,5) is 3 steps away.
        assertEquals(3, SightTriggers.lineSteps(3, 5, 6, 6, 5, 4));
        assertEquals(3, SightTriggers.lineSteps(3, 5, 6, 6, 5, 3), "exactly at the limit");
        assertEquals(-1, SightTriggers.lineSteps(3, 5, 6, 7, 5, 3), "beyond the distance");
        // Facing north from (5,5): the player two tiles up.
        assertEquals(2, SightTriggers.lineSteps(5, 5, 8, 5, 3, 4));
        // Facing west / south.
        assertEquals(1, SightTriggers.lineSteps(5, 5, 4, 4, 5, 3));
        assertEquals(2, SightTriggers.lineSteps(5, 5, 2, 5, 7, 3));
        // Misses.
        assertEquals(-1, SightTriggers.lineSteps(3, 5, 6, 3, 4, 4), "off the row");
        assertEquals(-1, SightTriggers.lineSteps(3, 5, 4, 6, 5, 4), "player behind the event");
        assertEquals(-1, SightTriggers.lineSteps(3, 5, 6, 3, 5, 4), "same tile is not a sight hit");
        assertEquals(-1, SightTriggers.lineSteps(3, 5, 0, 3, 5, 4), "no direction");
        assertEquals(-1, SightTriggers.lineSteps(3, 5, 6, 4, 5, 0), "distance 0 never triggers");
    }
}
