package pokemon.runtime.pokemon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 087_PBExperience: the tables up to level 100 and the plugin's own formulas up to level 210. */
class PBExperienceTest {
    private static final String[] RATES = {"Medium", "Erratic", "Fluctuating", "Parabolic", "Fast", "Slow"};

    @Test
    @DisplayName("the table entries the plugin prints (:19-24, :49-53, :111-113)")
    void tableSamples() {
        assertEquals(1000000, PBExperience.pbGetExpInternal(100, "Medium"));
        assertEquals(600000, PBExperience.pbGetExpInternal(100, "Erratic"));
        assertEquals(1640000, PBExperience.pbGetExpInternal(100, "Fluctuating"));
        assertEquals(1059860, PBExperience.pbGetExpInternal(100, "Parabolic"));
        assertEquals(800000, PBExperience.pbGetExpInternal(100, "Fast"));
        assertEquals(1250000, PBExperience.pbGetExpInternal(100, "Slow"));
        assertEquals(-1, PBExperience.pbGetExpInternal(0, "Medium"));
        assertEquals(0, PBExperience.pbGetExpInternal(1, "Slow"));
    }

    @Test
    @DisplayName("above level 100 pbGetExpInternal uses its own formulas (:117-147)")
    void formulasAbove100() {
        assertEquals(3375000, PBExperience.pbGetExpInternal(150, "Medium"));
        assertEquals(624362, PBExperience.pbGetExpInternal(101, "Erratic"));          // floor(101**4 * 0.6 / 100)
        assertEquals(2700000, PBExperience.pbGetExpInternal(150, "Fast"));            // 150**3 * 4 / 5
        assertEquals(4218750, PBExperience.pbGetExpInternal(150, "Slow"));            // 150**3 * 5 / 4
        assertEquals(3375000 * 6 / 5 - 15 * 150 * 150 + 15000 - 140, PBExperience.pbGetExpInternal(150, "Parabolic"));
        // Fluctuating: rate 82 - (150-100)/2 = 57; level*rate/100 = 85; 150**3 * 85 / 50.0
        assertEquals((int) Math.floor(3375000.0 * 85 / 50.0), PBExperience.pbGetExpInternal(150, "Fluctuating"));
        // the rate stops at 40: 82 - (210-100)/2 = 27 -> 40
        assertEquals((int) Math.floor(9261000.0 * (210 * 40 / 100) / 50.0), PBExperience.pbGetExpInternal(210, "Fluctuating"));
    }

    @Test
    @DisplayName("pbGetStartExperience limits the level, pbAddExperience limits the total (:161-182)")
    void limits() {
        assertEquals(PBExperience.pbGetExpInternal(210, "Medium"), PBExperience.pbGetStartExperience(999, "Medium"));
        assertEquals(PBExperience.pbGetMaxExperience("Fast"), PBExperience.pbAddExperience(1, Integer.MAX_VALUE / 2, "Fast"));
        assertEquals(150, PBExperience.pbAddExperience(100, 50, "Fast"));
    }

    @Test
    @DisplayName("pbGetLevelFromExperience inverts the curve for every level of every rate (:186-199)")
    void levelFromExperience() {
        for (String rate : RATES) {
            int previous = -2;
            for (int level = 1; level <= PBExperience.MAXIMUM_LEVEL; level++) {
                int exp = PBExperience.pbGetStartExperience(level, rate);
                // the plugin's own Fluctuating formula above level 100 is not monotonic (its rate shrinks with the level)
                boolean grows = exp > previous;
                assertTrue(grows || ("Fluctuating".equals(rate) && level > 100), rate + " " + level + " grows");
                previous = exp;
                if (grows) {
                    assertEquals(level, PBExperience.pbGetLevelFromExperience(exp, rate), rate + " at exactly " + level);
                }
            }
            assertEquals(1, PBExperience.pbGetLevelFromExperience(0, rate));
            assertEquals(PBExperience.MAXIMUM_LEVEL,
                    PBExperience.pbGetLevelFromExperience(Integer.MAX_VALUE, rate), "clamped to the maximum");
        }
    }
}
