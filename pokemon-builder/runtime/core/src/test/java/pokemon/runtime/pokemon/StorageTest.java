package pokemon.runtime.pokemon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pokemon_Storage (#204): fixed 30-slot boxes with names and wallpapers, box -1 is the
 * party, and the move/copy/store helpers that the B2W2 PC screen builds on.
 */
class StorageTest {

    private static Pokemon pokemon() {
        return new Pokemon(null, 5, null);
    }

    @Test
    @DisplayName("Pokemon_Storage:240 pbDelete leaves a hole, the other slots do not move")
    void slotsAreStable() {
        Storage storage = new Storage();
        Pokemon a = pokemon(), b = pokemon();
        storage.set(0, 0, a);
        storage.set(0, 1, b);
        storage.pbDelete(0, 0);
        assertNull(storage.get(0, 0));
        assertSame(b, storage.get(0, 1));
        assertEquals(0, storage.pbFirstFreePos(0));
        assertEquals(1, storage.box(0).nitems());
    }

    @Test
    @DisplayName("Pokemon_Storage:216 pbStoreCaught fills the current box first, then the first free slot")
    void storeCaughtStartsAtCurrentBox() {
        Storage storage = new Storage();
        storage.currentBox = 3;
        assertEquals(3, storage.pbStoreCaught(pokemon()));
        for (int i = 1; i < Storage.SLOTS; i++) storage.pbStoreCaught(pokemon());
        assertTrue(storage.box(3).full());
        storage.set(0, 7, pokemon());
        assertEquals(0, storage.pbStoreCaught(pokemon()), "the first box with a hole");
        assertEquals(0, storage.currentBox);
    }

    @Test
    @DisplayName("Pokemon_Storage:190 pbMove between party (-1) and a box")
    void movesBetweenPartyAndBox() {
        Party party = new Party();
        Storage storage = new Storage(party);
        Pokemon p = pokemon();
        party.add(p);
        party.add(pokemon());
        assertTrue(storage.pbMove(2, -1, -1, 0));
        assertSame(p, storage.get(2, 0));
        assertEquals(1, party.size());
        assertTrue(storage.pbMove(-1, -1, 2, 0));
        assertSame(p, party.get(1));
        assertNull(storage.get(2, 0));
        for (int i = party.size(); i < 6; i++) party.add(pokemon());
        storage.set(2, 0, pokemon());
        assertFalse(storage.pbMove(-1, -1, 2, 0), "a full party refuses");
        assertNotNull(storage.get(2, 0));
    }

    @Test
    @DisplayName("Pokemon_Storage:164 pbCopy into a box heals the Pokemon")
    void copyingIntoABoxHeals() {
        Party party = new Party();
        Storage storage = new Storage(party);
        Pokemon p = pokemon();
        p.hp = 1;
        p.status = "BURN";
        party.add(p);
        assertTrue(storage.pbMove(0, -1, -1, 0));
        assertEquals(p.maxHp(), p.hp);
        assertEquals("", p.status);
    }

    @Test
    @DisplayName("Pokemon_Storage:50-98 boxes are named and get wallpaper i%42; wallpapers beyond 42 need unlocking")
    void boxesAndWallpapers() {
        Storage storage = new Storage();
        assertEquals("盒子 1", storage.box(0).name);
        assertEquals("盒子 45", storage.box(44).name);
        assertEquals(44 % 42, storage.box(44).background);
        assertEquals(42, storage.allWallpapers().length);
        assertEquals(42, storage.availableWallpapers().size());
        assertEquals("森林", storage.availableWallpapers().get(0)[0]);
        assertFalse(storage.isAvailableWallpaper(42));
    }

    @Test
    @DisplayName("B2W2 PC:2137 box swap exchanges the slot arrays, not names or wallpapers")
    void boxSwapKeepsNames() {
        Storage storage = new Storage();
        Pokemon p = pokemon();
        storage.set(0, 4, p);
        Pokemon[] first = storage.box(0).pokemon().clone();
        storage.box(0).pokemon(storage.box(1).pokemon().clone());
        storage.box(1).pokemon(first);
        assertNull(storage.get(0, 4));
        assertSame(p, storage.get(1, 4));
        assertEquals("盒子 1", storage.box(0).name);
    }

    @Test
    @DisplayName("project capacity: 200 boxes of 30, and store() refuses when everything is full")
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
        assertTrue(storage.full());
    }
}
