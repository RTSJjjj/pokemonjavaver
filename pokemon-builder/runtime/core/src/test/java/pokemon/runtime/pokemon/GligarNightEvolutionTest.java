package pokemon.runtime.pokemon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.field.EvolutionWorld;
import pokemon.runtime.state.GameState;

import java.io.File;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Gligar holding a Razor Fang evolves on a level up at night ({@code NightHoldItem}, 201_Pokemon_Evolution:608-619).
 * "Night" is {@code PBDayNight.isNight?}: the real clock's hour, 18 to 23 and 0 to 5 (the same hours the screen tone
 * darkens), whatever the minute is.
 */
class GligarNightEvolutionTest {

    private static PbsData data() {
        for (File d = new File("").getAbsoluteFile(); d != null; d = d.getParentFile()) {
            for (String candidate : new String[] {"generated", "pokemon-builder/generated"}) {
                File root = new File(d, candidate);
                if (new File(root, "pbs").isDirectory()) return PbsData.parse(root);
            }
        }
        assumeTrue(false, "generated/pbs not found");
        return null;
    }

    private static String evolvesAt(PbsData data, LocalTime time, String item) {
        Pokemon gligar = new Pokemon(data.species("GLIGAR"), 30, data);
        gligar.item = item;
        PBEvolution.Env env = new EvolutionWorld(new GameState(), data, null, () -> time);
        return PBEvolution.checkEvolution(gligar, null, env);
    }

    @Test
    @DisplayName("at night (18:00-5:59) a Gligar with a Razor Fang evolves, whatever the minute")
    void evolvesAtNight() {
        PbsData data = data();
        for (int hour : new int[] {18, 20, 23, 0, 3, 5}) {
            for (int minute : new int[] {0, 14, 30, 43, 59}) {
                assertEquals("GLISCOR", evolvesAt(data, LocalTime.of(hour, minute), "RAZORFANG"),
                        hour + ":" + minute + " is night");
            }
        }
    }

    @Test
    @DisplayName("by day (6:00-17:59) it does not, whatever the minute")
    void doesNotEvolveByDay() {
        PbsData data = data();
        for (int hour : new int[] {6, 9, 12, 15, 17}) {
            for (int minute : new int[] {0, 14, 30, 44, 59}) {
                assertNull(evolvesAt(data, LocalTime.of(hour, minute), "RAZORFANG"),
                        hour + ":" + minute + " is day");
            }
        }
    }

    @Test
    @DisplayName("without the Razor Fang it never evolves")
    void needsTheItem() {
        PbsData data = data();
        assertNull(evolvesAt(data, LocalTime.of(12, 50), null));
        assertNull(evolvesAt(data, LocalTime.of(12, 50), "RAZORCLAW"));
    }

    @Test
    @DisplayName("the game's own check (no injected clock) follows the real clock like PBDayNight.isNight?")
    void defaultClockUsesTheGameHour() {
        PbsData data = data();
        PBEvolution.Env env = new EvolutionWorld(new GameState(), data, null, null);
        boolean expected = pokemon.runtime.field.PBDayNight.isNight(LocalTime.now());
        assertEquals(expected, env.isNight(), "EvolutionWorld without a clock reads the real time");
    }
}
