package pokemon.runtime.pokemon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 3 / P1: the party model (add/remove/swap/heal/first able) that events,
 * the save system and the PC share.
 */
class PartyTest {

    private static Pokemon pokemon() {
        return new Pokemon(null, 5, null);
    }

    @Test
    @DisplayName("P1: a party holds six Pokemon and refuses a seventh")
    void holdsSix() {
        Party party = new Party();
        for (int i = 0; i < Party.LIMIT; i++) {
            assertTrue(party.add(pokemon()), "slot " + i);
        }
        assertTrue(party.isFull());
        assertFalse(party.add(pokemon()));
        assertEquals(Party.LIMIT, party.size());
    }

    @Test
    @DisplayName("P1: remove and swap reorder the slots")
    void removeAndSwap() {
        Party party = new Party();
        Pokemon first = pokemon();
        Pokemon second = pokemon();
        Pokemon third = pokemon();
        party.add(first);
        party.add(second);
        party.add(third);

        party.swap(0, 2);
        assertSame(third, party.get(0));
        assertSame(first, party.get(2));

        assertSame(second, party.remove(1));
        assertEquals(2, party.size());
        assertSame(third, party.get(0));
        assertSame(first, party.get(1));
        assertTrue(party.remove(first));
        assertFalse(party.remove(first), "removing twice is a no-op");
    }

    @Test
    @DisplayName("P1: heal restores every member and clears status")
    void healRestoresEveryone() {
        Party party = new Party();
        Pokemon hurt = pokemon();
        hurt.hp = 1;
        hurt.status = "POISON";
        Pokemon healthy = pokemon();
        healthy.hp = healthy.maxHp();
        party.add(hurt);
        party.add(healthy);

        assertTrue(party.heal());
        assertEquals(hurt.maxHp(), hurt.hp);
        assertTrue(hurt.status.isEmpty());
        assertFalse(party.heal(), "the second heal changes nothing");
    }

    @Test
    @DisplayName("P1: first able skips fainted members and eggs")
    void firstAbleSkipsFaintedAndEggs() {
        Party party = new Party();
        Pokemon egg = pokemon();
        egg.egg = true;
        Pokemon fainted = pokemon();
        fainted.hp = 0;
        Pokemon able = pokemon();
        party.add(egg);
        party.add(fainted);
        party.add(able);

        assertSame(able, party.firstAble());
        assertEquals(1, party.eggCount());

        party.remove(able);
        assertNull(party.firstAble());
    }

    @Test
    @DisplayName("P3: a party egg hatches after its steps")
    void eggHatches() {
        Party party = new Party();
        Pokemon egg = pokemon();
        egg.egg = true;
        egg.stepsToHatch = 2;
        party.add(egg);

        assertTrue(party.stepEggs().isEmpty(), "not yet");
        assertEquals(1, egg.stepsToHatch);
        assertEquals(1, party.stepEggs().size, "the counter reached zero");
        assertFalse(egg.egg);
        assertEquals(0, party.eggCount());
    }
}
