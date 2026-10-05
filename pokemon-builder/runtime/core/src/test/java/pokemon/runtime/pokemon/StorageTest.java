package pokemon.runtime.pokemon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 3 / P1: the PC storage boxes (thirty boxes of thirty), used by the
 * overflow of pbAddPokemon and, later, the PC UI.
 */
class StorageTest {

    private static Pokemon pokemon() {
        return new Pokemon(null, 5, null);
    }

    @Test
    @DisplayName("P1: storage fills box by box and withdraws by slot")
    void fillsAndWithdraws() {
        Storage storage = new Storage();
        for (int i = 0; i < Storage.SLOTS; i++) {
            assertTrue(storage.store(pokemon()));
        }
        // The next one opens box 1.
        Pokemon overflow = pokemon();
        assertTrue(storage.store(overflow));
        assertEquals(Storage.SLOTS + 1, storage.count());
        assertEquals(2, storage.usedBoxes());
        assertSame(overflow, storage.get(1, 0));

        assertSame(overflow, storage.withdraw(1, 0));
        assertNull(storage.get(1, 0));
        assertEquals(Storage.SLOTS, storage.count());
        assertNull(storage.withdraw(1, 0), "withdrawing an empty slot returns null");
    }

    @Test
    @DisplayName("P1: an empty storage reports zero and clears back to empty")
    void emptyAndClear() {
        Storage storage = new Storage();
        assertTrue(storage.box(0).isEmpty());
        assertEquals(0, storage.count());
        assertEquals(0, storage.usedBoxes());
        storage.store(pokemon());
        storage.clear();
        assertEquals(0, storage.count());
    }

    @Test
    @DisplayName("P1: storage matches the project's 200 boxes and refuses when every slot is full")
    void matchesProjectCapacity() {
        assertEquals(200, Storage.BOXES, "Settings::NUM_STORAGE_BOXES");
        assertEquals(30, Storage.SLOTS);
        Storage storage = new Storage();
        int capacity = Storage.BOXES * Storage.SLOTS;
        for (int i = 0; i < capacity; i++) {
            assertTrue(storage.store(pokemon()), "slot " + i);
        }
        assertFalse(storage.store(pokemon()), "no room left");
        assertEquals(capacity, storage.count());
        assertEquals(Storage.BOXES, storage.usedBoxes());
    }
}
