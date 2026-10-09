package pokemon.runtime.pokemon;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** 197_PokeBattle_Pokemon:355-390. */
class PokerusTest {
    @Test
    void infectionCountsDownToCured() {
        Pokemon p = new Pokemon(null, 5, null);
        assertEquals(0, p.pokerusStage());
        p.givePokerus(5, new Random(1));                       // strain 5: 1 + 5 % 4 = 2 days
        assertEquals(1, p.pokerusStage());
        assertEquals(5, p.pokerusStrain());
        p.lowerPokerusCount();
        assertEquals(1, p.pokerusStage());
        p.lowerPokerusCount();
        assertEquals(2, p.pokerusStage(), "no days left: cured");
        p.lowerPokerusCount();
        assertEquals(2, p.pokerusStage(), "a cured Pokemon stays cured");
        p.givePokerus(3, new Random(1));
        assertEquals(2, p.pokerusStage(), "cannot be re-infected");
        assertEquals(5, p.pokerusStrain());
    }

    @Test
    void randomStrainIsInRange() {
        Pokemon p = new Pokemon(null, 5, null);
        p.givePokerus(0, new Random(3));
        assertTrue(p.pokerusStrain() >= 1 && p.pokerusStrain() <= 15);
        assertEquals(1, p.pokerusStage());
    }
}
