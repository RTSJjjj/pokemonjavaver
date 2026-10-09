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

/** 170_PField_Field pbOnStepTaken / pbBattleOnStepTaken and the repel countdown (189_PItem_ItemEffects:162-188). */
class FieldStepsTest {

    /** nextInt answers the queued values first (clamped below the bound), then {@code fallback}. */
    private static final class Script extends Random {
        final ArrayDeque<Integer> values = new ArrayDeque<>();
        int fallback;

        @Override
        public int nextInt(int bound) {
            int value = values.isEmpty() ? fallback : values.poll();
            return Math.min(value, bound - 1);
        }
    }

    @TempDir
    Path tempDir;

    private GameState state;
    private PbsData pbs;
    private Script random;
    private PokemonEncounters encounters;
    private FieldSteps steps;

    @BeforeEach
    void setUp() throws Exception {
        Path pbsDir = tempDir.resolve("pbs");
        Files.createDirectories(pbsDir);
        Files.write(pbsDir.resolve("pokemon.json"), ("{\"total\":1,\"byId\":{},\"species\":{"
                + "\"PLAIN\":{\"id\":1,\"internalName\":\"PLAIN\",\"name\":\"PLAIN\",\"types\":[\"NORMAL\"],"
                + "\"baseStats\":[45,49,49,45,65,65],\"rareness\":45,\"weight\":6.9,\"genderRate\":\"Female50Percent\","
                + "\"evolutions\":[]}}}").getBytes(StandardCharsets.UTF_8));
        Files.write(pbsDir.resolve("encounters.json"), ("{\"byMap\":{\"10\":{\"id\":10,\"name\":\"grass\","
                + "\"densities\":{\"Land\":25},\"methods\":{\"Land\":[{\"species\":\"PLAIN\",\"min\":5,\"max\":5}]}}}}")
                .getBytes(StandardCharsets.UTF_8));
        pbs = PbsData.parse(tempDir.toFile());
        state = new GameState();
        random = new Script();
        encounters = new PokemonEncounters(pbs, state, random, () -> LocalTime.of(12, 0));
        encounters.setup(10);
        steps = new FieldSteps(pbs, state, encounters, random);
        addPokemon(10, 50);
    }

    private Pokemon addPokemon(int level, int happiness) {
        Pokemon pokemon = new Pokemon(pbs.species("PLAIN"), level, pbs);
        pokemon.happiness = happiness;
        pokemon.ballused = 99;                 // not the Luxury Ball (this fixture has no ball table: its type reads 0)
        pokemon.obtainMap = 77;                // not met on the test map
        pokemon.hp = pokemon.maxHp();
        state.trainer().party.add(pokemon);
        return pokemon;
    }

    @Test
    @DisplayName("every step raises the step counter; a forced step (move route / interpreter) changes nothing (:357-366)")
    void stepCounter() {
        steps.onStepTaken(false, false, 13, false);
        steps.onStepTaken(false, false, 13, false);
        assertEquals(2, state.fieldGlobals().stepcount);
        steps.onStepTaken(false, true, 13, false);
        assertEquals(2, state.fieldGlobals().stepcount, "forced steps are not counted");
        state.fieldGlobals().stepcount = Integer.MAX_VALUE;
        steps.onStepTaken(false, false, 13, false);
        assertEquals(0, state.fieldGlobals().stepcount, "&= 0x7FFFFFFF wraps to 0");
    }

    @Test
    @DisplayName("after 128 steps every able Pokemon has a 1 in 2 chance of walking happiness (:262-269)")
    void walkingHappiness() {
        Pokemon second = addPokemon(10, 50);
        second.hp = 0;                                       // fainted: no happiness
        Pokemon lucky = state.trainer().party.get(0);
        for (int i = 0; i < 127; i++) {
            steps.onStepTaken(false, false, 13, false);
        }
        assertEquals(50, lucky.happiness);
        assertEquals(127, state.fieldGlobals().happinessSteps);
        random.fallback = 0;                                 // rand(2) == 0: the roll succeeds
        steps.onStepTaken(false, false, 13, false);
        assertEquals(52, lucky.happiness, "walking gives 2 below 200 happiness");
        assertEquals(50, second.happiness);
        assertEquals(0, state.fieldGlobals().happinessSteps);
    }

    @Test
    @DisplayName("the repel counts down each step, not on ice, and says so when it wears off (:162-188)")
    void repelCountdown() {
        state.fieldGlobals().repel = 2;
        steps.onStepTaken(false, false, 12, false);          // ice
        assertEquals(2, state.fieldGlobals().repel);
        FieldSteps.Result first = steps.onStepTaken(false, false, 13, false);
        assertEquals(1, state.fieldGlobals().repel);
        assertTrue(first.messages.isEmpty());
        FieldSteps.Result last = steps.onStepTaken(false, false, 13, false);
        assertEquals(0, state.fieldGlobals().repel);
        assertEquals(1, last.messages.size());
        assertTrue(last.messages.get(0).contains("喷雾剂"));
    }

    @Test
    @DisplayName("a step on grass can start a wild battle; an event on the step or a menu stops it (:376, :488-516)")
    void wildBattleOnStep() {
        random.fallback = 0;
        for (int i = 0; i < 3; i++) {
            assertTrue(steps.onStepTaken(false, false, 2, false).wild.isEmpty(), "the first steps are safe");
        }
        FieldSteps.Result hit = steps.onStepTaken(false, false, 2, false);
        assertEquals(1, hit.wild.size());
        assertEquals("PLAIN", hit.wild.get(0).species);
        encounters.clearStepCount();
        for (int i = 0; i < 3; i++) {
            steps.onStepTaken(false, false, 2, false);
        }
        assertTrue(steps.onStepTaken(true, false, 2, false).wild.isEmpty(), "an event started on this step");
        assertTrue(steps.onStepTaken(false, false, 2, true).wild.isEmpty(), "in a menu");
        assertTrue(steps.onStepTaken(false, false, 13, false).wild.isEmpty(), "not on a grass tile");
    }

    @Test
    @DisplayName("tall grass with two able Pokemon sometimes starts a double battle (30%); a partner always does")
    void doubleBattle() {
        addPokemon(10, 50);
        random.fallback = 0;
        for (int i = 0; i < 3; i++) {
            steps.onStepTaken(false, false, 10, false);
        }
        FieldSteps.Result tall = steps.onStepTaken(false, false, 10, false);
        assertEquals(2, tall.wild.size(), "TallGrass, two able Pokemon, rand(100) = 0 < 30");

        encounters.clearStepCount();
        for (int i = 0; i < 3; i++) {
            steps.onStepTaken(false, false, 2, false);
        }
        assertEquals(1, steps.onStepTaken(false, false, 2, false).wild.size(), "plain Grass is never a double");
    }

    @Test
    @DisplayName("turning on the spot rolls the encounter too (:381-384)")
    void turning() {
        random.fallback = 0;
        for (int i = 0; i < 3; i++) {
            steps.onChangeDirection(2, false);
        }
        assertEquals(1, steps.onChangeDirection(2, false).wild.size());
        assertTrue(steps.onChangeDirection(2, true).wild.isEmpty(), "not while a menu is open");
    }

    @Test
    @DisplayName("without an able Pokemon nothing is rolled (:489)")
    void noAblePokemon() {
        state.trainer().party.get(0).hp = 0;
        random.fallback = 0;
        for (int i = 0; i < 6; i++) {
            assertTrue(steps.onStepTaken(false, false, 2, false).wild.isEmpty());
        }
    }
}
