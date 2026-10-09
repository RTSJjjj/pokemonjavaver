package pokemon.runtime.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 195_PItem_Bag:237-369 PCItemStorage / ItemStorageHelper. */
class PcItemStorageTest {
    @Test
    @DisplayName("a new storage holds one Potion")
    void startsWithAPotion() {
        PcItemStorage storage = PcItemStorage.withPotion(true);
        assertEquals(1, storage.length());
        assertEquals(1, storage.pbQuantity("POTION"));
        assertTrue(PcItemStorage.withPotion(false).empty());
    }

    @Test
    @DisplayName("slots hold 999 each and overflow into the next slot")
    void slots() {
        PcItemStorage storage = new PcItemStorage();
        assertTrue(storage.pbStoreItem("ETHER", 1500));
        assertEquals(2, storage.length());
        assertEquals(999, storage.get(0).count);
        assertEquals(501, storage.get(1).count);
        assertEquals(1500, storage.pbQuantity("ETHER"));
        assertTrue(storage.pbStoreItem("ETHER", 498));
        assertEquals(999, storage.get(1).count);
    }

    @Test
    @DisplayName("deleting spans slots and drops empty ones; the 50 slots limit refuses more")
    void deleteAndLimit() {
        PcItemStorage storage = new PcItemStorage();
        storage.pbStoreItem("ETHER", 1500);
        assertTrue(storage.pbDeleteItem("ETHER", 1000));
        assertEquals(1, storage.length());
        assertEquals(500, storage.pbQuantity("ETHER"));
        assertFalse(storage.pbDeleteItem("ETHER", 501));
        PcItemStorage full = new PcItemStorage();
        for (int i = 0; i < PcItemStorage.MAXSIZE; i++) {
            assertTrue(full.pbStoreItem("ITEM" + i, 1));
        }
        assertFalse(full.pbCanStore("OTHER", 1));
        assertFalse(full.pbStoreItem("OTHER", 1));
        assertTrue(full.pbCanStore("ITEM3", 5));
    }
}
