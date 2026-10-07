package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.event.ScreenEffects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R6.17: the day/night tone table is the project's own
 * {@code PBDayNight::HourlyTones} (PField_Time, script section 181): one tone
 * per hour, interpolated into the next hour by the current minute, with the
 * outdoor gate from R6.16 and a script tone (223) always winning.
 */
class DayNightToneTest {

    @Test
    @DisplayName("the hourly table matches the project's PBDayNight values")
    void hourlyTable() {
        assertArrayEquals(new float[] {-90f, -90f, 30f, 55f},
                DayNightTone.hourlyTone(0), 0.01f, "00:00 night");
        assertArrayEquals(new float[] {-60f, -70f, -5f, 50f},
                DayNightTone.hourlyTone(4), 0.01f, "04:00");
        assertArrayEquals(new float[] {-40f, -50f, -35f, 50f},
                DayNightTone.hourlyTone(7), 0.01f, "07:00 morning");
        assertArrayEquals(new float[] {0f, 0f, 0f, 0f},
                DayNightTone.hourlyTone(12), 0.01f, "noon is neutral");
        assertArrayEquals(new float[] {10f, -25f, -15f, 10f},
                DayNightTone.hourlyTone(16), 0.01f, "16:00");
        assertArrayEquals(new float[] {-5f, -30f, -20f, 0f},
                DayNightTone.hourlyTone(18), 0.01f, "18:00 evening");
        assertArrayEquals(new float[] {-70f, -90f, 15f, 55f},
                DayNightTone.hourlyTone(21), 0.01f, "21:00");
    }

    @Test
    @DisplayName("minutes interpolate into the next hour (getToneInternal)")
    void interpolation() {
        float[] half = DayNightTone.at(16, 30);
        assertEquals(7.5f, half[0], 0.01f);
        assertEquals(-30f, half[1], 0.01f);
        assertEquals(-17.5f, half[2], 0.01f);
        assertEquals(12.5f, half[3], 0.01f);

        // 23:xx wraps into 00:00, which carries the same night tone.
        assertArrayEquals(DayNightTone.hourlyTone(0), DayNightTone.at(23, 30), 0.01f);
    }

    @Test
    @DisplayName("night darkens red/green, adds blue and desaturates")
    void nightTone() {
        float[] night = DayNightTone.at(2, 0);
        assertEquals(-90f, night[0], 0.01f);
        assertEquals(-90f, night[1], 0.01f);
        assertTrue(night[2] > 0f, "the project's night tone adds blue");
        assertTrue(night[3] > 0f, "...and desaturates (gray)");
    }

    @Test
    @DisplayName("a scripted tone (223) wins over the ambient one")
    void scriptedToneWins() {
        ScreenEffects effects = new ScreenEffects();
        effects.ambientTint(-60f, -60f, -20f, 0f, 0);
        assertFalse(effects.toneFromScript());
        assertEquals(-60f, effects.toneRed(), 0.01f);

        effects.tint(-255f, -255f, -255f, 0f, 0); // a door / cave blackout
        assertTrue(effects.toneFromScript());
        assertEquals(-255f, effects.toneRed(), 0.01f);

        effects.clearTone(); // map setup: the ambient effect may take over again
        assertFalse(effects.toneFromScript());
        assertEquals(0f, effects.toneRed(), 0.01f);
    }

    @Test
    @DisplayName("only outdoor maps are shaded (PBS/metadata.txt Outdoor)")
    void outdoorGate() {
        assertTrue(DayNightTone.shades(Boolean.TRUE), "outdoor maps get the tone");
        assertFalse(DayNightTone.shades(Boolean.FALSE), "rooms keep a neutral tone");
        assertTrue(DayNightTone.shades(null), "old data keeps the previous behaviour");
    }

    @Test
    @DisplayName("-Dpokemon.daynight.hour pins the ambient tone too, not just isDay?")
    void pinnedHourDrivesTheAmbientTone() {
        // R12: the capture probe compares screenshots taken at different
        // wall-clock times, so the documented pin has to cover the tone the map
        // layer actually applies - not only the isDay?/isNight? predicates.
        String previous = System.getProperty("pokemon.daynight.hour");
        try {
            System.setProperty("pokemon.daynight.hour", "12");
            java.time.LocalTime now = java.time.LocalTime.now();
            assertArrayEquals(DayNightTone.at(12, now.getMinute()), DayNightTone.now(), 0.01f,
                    "noon is the neutral tone whatever the real clock says");

            System.setProperty("pokemon.daynight.hour", "2");
            float[] night = DayNightTone.now();
            assertEquals(-90f, night[0], 0.01f);
            assertEquals(-90f, night[1], 0.01f);
            assertTrue(night[3] > 0f, "the pinned hour desaturates like the real one");
        } finally {
            if (previous == null) {
                System.clearProperty("pokemon.daynight.hour");
            } else {
                System.setProperty("pokemon.daynight.hour", previous);
            }
        }
    }
}
