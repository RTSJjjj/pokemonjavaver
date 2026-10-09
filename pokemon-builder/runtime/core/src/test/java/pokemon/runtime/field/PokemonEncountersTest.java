package pokemon.runtime.field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.GameState;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** 175_PField_Encounters: encounter types, weights, the step probability and its modifiers. */
class PokemonEncountersTest {

    /** nextInt answers the queued values first (clamped below the bound), then 0. */
    private static final class Script extends Random {
        final ArrayDeque<Integer> values = new ArrayDeque<>();

        Script push(int... next) {
            for (int value : next) {
                values.add(value);
            }
            return this;
        }

        @Override
        public int nextInt(int bound) {
            int value = values.isEmpty() ? 0 : values.poll();
            return Math.min(value, bound - 1);
        }
    }

    @TempDir
    Path tempDir;

    private PbsData pbs;
    private GameState state;
    private Script random;
    private LocalTime now = LocalTime.of(12, 0);          // minute 0: game hour 0 (night)

    private static String species(String name, String type) {
        return "\"" + name + "\":{\"id\":1,\"internalName\":\"" + name + "\",\"name\":\"" + name + "\","
                + "\"types\":[\"" + type + "\"],\"baseStats\":[45,49,49,45,65,65],\"rareness\":45,"
                + "\"weight\":6.9,\"genderRate\":\"Female50Percent\",\"evolutions\":[]}";
    }

    private static String rows(String prefix, int count) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"species\":\"").append(prefix).append(i).append("\",\"min\":5,\"max\":8}");
        }
        return sb.append("]").toString();
    }

    @BeforeEach
    void setUp() throws Exception {
        Path pbsDir = tempDir.resolve("pbs");
        Files.createDirectories(pbsDir);
        Files.write(pbsDir.resolve("pokemon.json"), ("{\"total\":3,\"byId\":{},\"species\":{"
                + species("SPARK", "ELECTRIC") + "," + species("SPLASH", "WATER") + "," + species("PLAIN", "NORMAL")
                + "}}").getBytes(StandardCharsets.UTF_8));
        // map 10: grass; map 11: cave; map 12: every time-of-day table; map 13: three typed species.
        Files.write(pbsDir.resolve("encounters.json"), ("{\"byMap\":{"
                + "\"10\":{\"id\":10,\"name\":\"grass\",\"densities\":{\"Land\":15,\"Water\":10},\"methods\":{"
                + "\"Land\":" + rows("L", 12) + ",\"Water\":" + rows("W", 5) + ",\"RockSmash\":" + rows("R", 5) + "}},"
                + "\"11\":{\"id\":11,\"name\":\"cave\",\"densities\":{\"Cave\":10},\"methods\":{\"Cave\":" + rows("C", 12) + "}},"
                + "\"12\":{\"id\":12,\"name\":\"times\",\"densities\":{\"Land\":25,\"LandNight\":25,\"LandDay\":25,\"LandMorning\":25},\"methods\":{"
                + "\"Land\":" + rows("L", 12) + ",\"LandNight\":" + rows("N", 12)
                + ",\"LandDay\":" + rows("D", 12) + ",\"LandMorning\":" + rows("M", 12) + "}},"
                + "\"13\":{\"id\":13,\"name\":\"type\",\"densities\":{\"Land\":25},\"methods\":{\"Land\":["
                + "{\"species\":\"PLAIN\",\"min\":5,\"max\":5},{\"species\":\"SPARK\",\"min\":5,\"max\":5},"
                + "{\"species\":\"SPLASH\",\"min\":5,\"max\":5}]}}"
                + "}}").getBytes(StandardCharsets.UTF_8));
        pbs = PbsData.parse(tempDir.toFile());
        state = new GameState();
        random = new Script();
    }

    private PokemonEncounters encounters(int mapId) {
        PokemonEncounters encounters = new PokemonEncounters(pbs, state, random, () -> now);
        encounters.setup(mapId);
        return encounters;
    }

    private void lead(String ability, String item, int level) {
        Pokemon pokemon = new Pokemon(pbs.species("PLAIN"), level, pbs);
        pokemon.ability = ability;
        pokemon.item = item;
        state.trainer().party.add(pokemon);
    }

    @Test
    @DisplayName("pbEncounterType: surfing is Water, a cave is Cave, grass is Land (:135-155)")
    void encounterTypeBasics() {
        assertEquals("Land", encounters(10).pbEncounterType());
        assertEquals("Cave", encounters(11).pbEncounterType());
        assertNull(encounters(99).pbEncounterType(), "a map without tables rolls nothing");
        state.fieldGlobals().surfing = true;
        assertEquals("Water", encounters(10).pbEncounterType(), "surfing wins over everything");
    }

    @Test
    @DisplayName("pbEncounterType follows the project day/night: one real hour is one game day")
    void encounterTypeByTime() {
        PokemonEncounters times = encounters(12);
        now = LocalTime.of(3, 50);          // minute 50 -> game hour 20: night
        assertEquals("LandNight", times.pbEncounterType());
        now = LocalTime.of(3, 30);          // minute 30 -> game hour 12: day
        assertEquals("LandDay", times.pbEncounterType());
        now = LocalTime.of(3, 20);          // minute 20 -> game hour 8: morning (and day): morning wins
        assertEquals("LandMorning", times.pbEncounterType());
        assertEquals("Land", encounters(10).pbEncounterType(), "a map with only Land keeps Land at any hour");
    }

    @Test
    @DisplayName("isEncounterPossibleHere?: grass needs a grass tile, ice never rolls, surfing always does (:117-129)")
    void possibleHere() {
        PokemonEncounters grass = encounters(10);
        assertTrue(grass.isEncounterPossibleHere(2), "Grass");
        assertTrue(grass.isEncounterPossibleHere(10), "TallGrass");
        assertFalse(grass.isEncounterPossibleHere(13), "a neutral tile");
        assertFalse(grass.isEncounterPossibleHere(12), "ice");
        assertTrue(encounters(11).isEncounterPossibleHere(13), "a cave rolls on any tile");
        state.fieldGlobals().surfing = true;
        assertTrue(grass.isEncounterPossibleHere(13));
    }

    @Test
    @DisplayName("the first three steps after a battle never roll; the fourth can (:313-315)")
    void safeSteps() {
        PokemonEncounters grass = encounters(10);
        for (int i = 0; i < 3; i++) {
            assertNull(grass.pbGenerateEncounter("Land"), "step " + (i + 1) + " is safe");
        }
        assertNotNull(grass.pbGenerateEncounter("Land"));
        grass.clearStepCount();
        assertNull(grass.pbGenerateEncounter("Land"), "clearStepCount makes the next steps safe again");
    }

    @Test
    @DisplayName("the chance is density*16 against rand(180*16): cycling lowers it by 20% (:321-322, :365)")
    void densityAndBicycle() {
        PokemonEncounters grass = encounters(10);
        for (int i = 0; i < 3; i++) {
            grass.pbGenerateEncounter("Land");
        }
        random.push(239);                                   // 239 < 15*16 = 240: hits
        assertNotNull(grass.pbGenerateEncounter("Land"));
        random.push(240);                                   // 240 >= 240: misses
        assertNull(grass.pbGenerateEncounter("Land"));

        state.fieldGlobals().bicycle = true;                // 240 * 0.8 = 192
        random.push(191);
        assertNotNull(grass.pbGenerateEncounter("Land"));
        random.push(192);
        assertNull(grass.pbGenerateEncounter("Land"));
    }

    @Test
    @DisplayName("lead abilities and items scale the chance with Ruby's integer / float arithmetic (:330-361)")
    void abilityModifiers() {
        lead("STENCH", null, 10);
        PokemonEncounters grass = encounters(10);
        for (int i = 0; i < 3; i++) {
            grass.pbGenerateEncounter("Land");
        }
        random.push(119);                                   // 240 / 2 = 120: 119 hits
        assertNotNull(grass.pbGenerateEncounter("Land"));
        random.push(120);
        assertNull(grass.pbGenerateEncounter("Land"));

        state.trainer().party.members().clear();
        lead("SWARM", null, 10);                            // 240 * 1.5 = 360
        random.push(359);
        assertNotNull(grass.pbGenerateEncounter("Land"));
        random.push(360);
        assertNull(grass.pbGenerateEncounter("Land"));

        state.trainer().party.members().clear();
        lead("PLUS", "CLEANSETAG", 10);                     // an item beats the ability: 240 * 2 / 3 = 160
        random.push(159);
        assertNotNull(grass.pbGenerateEncounter("Land"));
        random.push(160);
        assertNull(grass.pbGenerateEncounter("Land"));
    }

    @Test
    @DisplayName("the weights of a Land table are 20,20,10,10,10,10,5,5,4,4,1,1 (:34-48)")
    void weights() {
        PokemonEncounters grass = encounters(10);
        int[][] cases = {{0, 0}, {19, 0}, {20, 1}, {39, 1}, {40, 2}, {49, 2}, {50, 3}, {60, 4}, {80, 6}, {85, 7},
                {90, 8}, {94, 9}, {98, 10}, {99, 11}};
        for (int[] c : cases) {
            random.push(99, c[0], 0);                            // the chain draw, the weighted draw, then the level draw
            PokemonEncounters.Encounter encounter = grass.pbEncounteredPokemon("Land", 1);
            assertEquals("L" + c[1], encounter.species, "draw " + c[0]);
        }
    }

    @Test
    @DisplayName("levels come from min + rand(1 + max - min)")
    void levels() {
        PokemonEncounters grass = encounters(10);
        random.push(99, 0, 3);
        assertEquals(8, grass.pbEncounteredPokemon("Land", 1).level);
        random.push(99, 0, 0);
        assertEquals(5, grass.pbEncounteredPokemon("Land", 1).level);
    }

    @Test
    @DisplayName("Static keeps only Electric species half of the time (:213-247); a failed roll changes nothing")
    void favouredType() {
        lead("STATIC", null, 10);
        PokemonEncounters typed = encounters(13);
        random.push(0, 0, 0);          // the 50% roll succeeds (0 < 50), the weighted draw, the level draw
        PokemonEncounters.Encounter encounter = typed.pbEncounteredPokemon("Land", 1);
        assertEquals("SPARK", encounter.species, "PLAIN and SPLASH were filtered out");
        random.push(60, 0, 0);         // the 50% roll fails
        assertEquals("PLAIN", typed.pbEncounteredPokemon("Land", 1).species);
    }

    @Test
    @DisplayName("Black / White Flute shift the wild level by 1..3 (:285-292)")
    void flutes() {
        PokemonEncounters grass = encounters(10);
        state.fieldGlobals().blackFluteUsed = true;
        random.push(99, 0, 0, 2);          // species, level 5, flute +1+2
        assertEquals(8, grass.pbEncounteredPokemon("Land", 1).level);
        state.fieldGlobals().blackFluteUsed = false;
        state.fieldGlobals().whiteFluteUsed = true;
        random.push(99, 0, 3, 1);          // level 8, flute -1-1
        assertEquals(6, grass.pbEncounteredPokemon("Land", 1).level);
    }

    @Test
    @DisplayName("a repel keeps away wild Pokemon weaker than the lead (:384-397)")
    void repel() {
        lead("BLAZE", null, 20);
        PokemonEncounters grass = encounters(10);
        PokemonEncounters.Encounter weak = new PokemonEncounters.Encounter("L0", 19);
        PokemonEncounters.Encounter equal = new PokemonEncounters.Encounter("L0", 20);
        assertTrue(grass.pbCanEncounter(weak, false), "no repel: anything can happen");
        assertFalse(grass.pbCanEncounter(weak, true));
        assertTrue(grass.pbCanEncounter(equal, true));
        state.fieldGlobals().repel = 5;
        assertFalse(grass.pbCanEncounter(weak, false), "the repel counter alone is enough");
        assertFalse(grass.pbCanEncounter(null, false), "nothing rolled, nothing to encounter");
    }

    @Test
    @DisplayName("Intimidate: 50% of the time a wild Pokemon 5 or more levels weaker is skipped (:371-378)")
    void intimidate() {
        lead("INTIMIDATE", null, 20);
        PokemonEncounters grass = encounters(10);
        for (int i = 0; i < 3; i++) {
            grass.pbGenerateEncounter("Land");
        }
        random.push(0);   // every other draw is 0 too: chance hits, species 0, level 5, both 50% rolls succeed
        assertNull(grass.pbGenerateEncounter("Land"), "level 5 <= 20 - 5");
    }
}
