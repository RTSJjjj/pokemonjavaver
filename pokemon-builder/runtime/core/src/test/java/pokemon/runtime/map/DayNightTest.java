package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plugin batch 2: the PBDayNight named switches ({@code s:PBDayNight.isDay?}
 * and friends gate 1 event page plus any script conditions) must use the
 * plugin's own hour ranges, not an approximation.
 */
class DayNightTest {

    @Test
    @DisplayName("the time-of-day tests follow the plugin's hour ranges")
    void pluginHours() {
        assertTrue(EventPages.dayNight("isDay?", 12));
        assertFalse(EventPages.dayNight("isDay?", 5), "night ends at 6");
        assertTrue(EventPages.dayNight("isDay?", 17));
        assertFalse(EventPages.dayNight("isDay?", 18), "dusk is not 'day'");

        assertTrue(EventPages.dayNight("isNight?", 22));
        assertTrue(EventPages.dayNight("isNight?", 3));
        assertFalse(EventPages.dayNight("isNight?", 12));

        assertTrue(EventPages.dayNight("isMorning?", 6));
        assertTrue(EventPages.dayNight("isMorning?", 8));
        assertFalse(EventPages.dayNight("isMorning?", 9));
        assertTrue(EventPages.dayNight("isBeforeNoon?", 10));
        assertTrue(EventPages.dayNight("isAtNoon?", 11));
        assertTrue(EventPages.dayNight("isAfternoon?", 13));
        assertTrue(EventPages.dayNight("isAfternoon?", 17));
        assertFalse(EventPages.dayNight("isAfternoon?", 18));
        assertTrue(EventPages.dayNight("isDusk?", 18));
        assertTrue(EventPages.dayNight("isEvening?", 19));
        assertFalse(EventPages.dayNight("isEvening?", 22));
        assertTrue(EventPages.dayNight("isMidnight?", 23));
        assertTrue(EventPages.dayNight("isDawn?", 4));
        assertFalse(EventPages.dayNight("isDawn?", 6));
        assertFalse(EventPages.dayNight("isSomethingElse?", 12));
    }
}
