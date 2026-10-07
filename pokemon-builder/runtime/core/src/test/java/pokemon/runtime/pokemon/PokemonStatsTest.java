package pokemon.runtime.pokemon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 3 / P0: the Essentials stat/experience/gender formulas against known
 * values (level-100 experience totals, Gen-3 stat floors, nature multipliers).
 */
class PokemonStatsTest {

    @Test
    @DisplayName("HP and stats follow the project's formula")
    void statFormulas() {
        // (2*45 + 31 + 0) * 50 / 100 + 50 + 10 = 120
        assertEquals(120, PokemonStats.maxHp(45, 31, 0, 50));
        // (2*49 + 31 + 0) * 50 / 100 + 5 = 69
        assertEquals(69, PokemonStats.stat(49, 31, 0, 50, 1f));
        // EVs quarter into the formula: (2*99 + 0 + 63) * 100 / 100 + 5 = 266
        assertEquals(266, PokemonStats.stat(99, 0, 252, 100, 1f));
    }

    @Test
    @DisplayName("natures raise one stat by 10% and lower another, floored")
    void natureMultipliers() {
        PbsData.Nature adamant = nature("Adamant", "ATTACK", "SPATK");
        assertEquals(1.1f, PokemonStats.natureMultiplier(adamant, PokemonStats.ATTACK));
        assertEquals(0.9f, PokemonStats.natureMultiplier(adamant, PokemonStats.SPATK));
        assertEquals(1f, PokemonStats.natureMultiplier(adamant, PokemonStats.SPEED));
        PbsData.Nature hardy = nature("Hardy", null, null);
        assertEquals(1f, PokemonStats.natureMultiplier(hardy, PokemonStats.ATTACK));

        // (2*49 + 31) * 50 / 100 + 5 = 69 -> 69 * 1.1 floored = 75; * 0.9 = 62
        assertEquals(75, PokemonStats.stat(49, 31, 0, 50, 1.1f));
        assertEquals(62, PokemonStats.stat(49, 31, 0, 50, 0.9f));
    }

    @Test
    @DisplayName("level-100 experience totals match the growth-rate curves")
    void experienceCurves() {
        assertEquals(1_000_000, PokemonStats.experienceForLevel("Medium", 100));
        assertEquals(800_000, PokemonStats.experienceForLevel("Fast", 100));
        assertEquals(1_250_000, PokemonStats.experienceForLevel("Slow", 100));
        assertEquals(1_059_860, PokemonStats.experienceForLevel("Parabolic", 100));
        assertEquals(600_000, PokemonStats.experienceForLevel("Erratic", 100));
        assertEquals(1_640_000, PokemonStats.experienceForLevel("Fluctuating", 100));
        assertEquals(0, PokemonStats.experienceForLevel("Medium", 1));
    }

    @Test
    @DisplayName("gender rates map to male/female/genderless")
    void genders() {
        assertEquals(PokemonStats.MALE, PokemonStats.gender("AlwaysMale", 0.9f));
        assertEquals(PokemonStats.FEMALE, PokemonStats.gender("AlwaysFemale", 0.1f));
        assertEquals(PokemonStats.GENDERLESS, PokemonStats.gender("Genderless", 0.5f));
        assertEquals(PokemonStats.MALE, PokemonStats.gender("Female50Percent", 0.1f));
        assertEquals(PokemonStats.FEMALE, PokemonStats.gender("Female50Percent", 0.9f));
        assertEquals(PokemonStats.MALE, PokemonStats.gender("Female25Percent", 0.5f));
        assertEquals(PokemonStats.FEMALE, PokemonStats.gender("Female25Percent", 0.8f));
        assertEquals(PokemonStats.MALE, PokemonStats.gender("Female75Percent", 0.1f));
        assertEquals(PokemonStats.FEMALE, PokemonStats.gender("Female75Percent", 0.5f));
        assertTrue(PokemonStats.singleGender("Genderless"));
        assertFalse(PokemonStats.singleGender("Female50Percent"));
    }

    @Test
    @DisplayName("genderByte follows PBGenderRates:11-23")
    void genderBytes() {
        assertEquals(0, PokemonStats.genderByte("AlwaysMale"));
        assertEquals(32, PokemonStats.genderByte("FemaleOneEighth"));
        assertEquals(64, PokemonStats.genderByte("Female25Percent"));
        assertEquals(128, PokemonStats.genderByte("Female50Percent"));
        assertEquals(192, PokemonStats.genderByte("Female75Percent"));
        assertEquals(224, PokemonStats.genderByte("FemaleSevenEighths"));
        assertEquals(254, PokemonStats.genderByte("AlwaysFemale"));
        assertEquals(255, PokemonStats.genderByte("Genderless"));
    }

    private static PbsData.Nature nature(String name, String up, String down) {
        PbsData.Nature nature = new PbsData.Nature();
        nature.name = name;
        nature.internalName = name.toUpperCase();
        nature.statUp = up;
        nature.statDown = down;
        return nature;
    }
}
