package pokemon.runtime.pokemon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.state.TriadStorage;

import static org.junit.jupiter.api.Assertions.*;

/** 235_PMinigame_TripleTriad: the card values, the shop prices and the card storage. */
class TriadCardTest {
    private static PbsData.Species species(int[] stats, String... types) {
        PbsData.Species s = new PbsData.Species();
        s.internalName = "TEST";
        s.name = "测试";
        s.baseStats = stats;
        for (String type : types) s.types.add(type);
        return s;
    }

    @Test
    @DisplayName("the four numbers come from the base stats (TriadCard#initialize)")
    void numbers() {
        // HP 45, Attack 49, Defense 49, Speed 45, Sp.Atk 65, Sp.Def 65 (Bulbasaur)
        TriadCard card = new TriadCard(null, species(new int[] {45, 49, 49, 45, 65, 65}, "GRASS", "POISON"));
        assertEquals(3, card.west);     // 49 + 45/3 = 64
        assertEquals(3, card.east);     // 49 + 45/3 = 64
        assertEquals(4, card.north);    // 65 + 15 = 80 -> 4 (>=73)
        assertEquals(4, card.south);    // 65 + 15 = 80 -> 4 (>=73)
    }

    @Test
    @DisplayName("stat thresholds: 45, 60, 73, 86, 100, 115, 134, 160, 189")
    void thresholds() {
        assertEquals(1, TriadCard.baseStatToValue(44));
        assertEquals(2, TriadCard.baseStatToValue(45));
        assertEquals(3, TriadCard.baseStatToValue(60));
        assertEquals(4, TriadCard.baseStatToValue(73));
        assertEquals(5, TriadCard.baseStatToValue(86));
        assertEquals(6, TriadCard.baseStatToValue(100));
        assertEquals(7, TriadCard.baseStatToValue(115));
        assertEquals(8, TriadCard.baseStatToValue(134));
        assertEquals(9, TriadCard.baseStatToValue(160));
        assertEquals(10, TriadCard.baseStatToValue(189));
    }

    @Test
    @DisplayName("the price is quantised to the next unit")
    void price() {
        TriadCard weak = new TriadCard(null, species(new int[] {10, 10, 10, 10, 10, 10}, "NORMAL"));
        assertEquals(10, weak.price(), "all ones: (1+ret/10)*10");
        TriadCard strong = new TriadCard(null, species(new int[] {106, 110, 90, 130, 154, 90}, "PSYCHIC"));
        assertTrue(strong.price() > 10000 && strong.price() % 1000 == 0);
    }

    @Test
    @DisplayName("TriadStorage: 99 per slot, one slot per species, delete and quantity")
    void storage() {
        TriadStorage storage = new TriadStorage(10);
        assertTrue(storage.pbStoreItem("A", 120));
        assertEquals(2, storage.length());
        assertEquals(99, storage.get(0).count);
        assertEquals(21, storage.get(1).count);
        assertEquals(120, storage.pbQuantity("A"));
        assertTrue(storage.pbDeleteItem("A", 30));
        assertEquals(90, storage.pbQuantity("A"));
        assertFalse(new TriadStorage(1).pbCanStore("B", 200), "one slot of 99 cannot hold 200");
        assertTrue(new TriadStorage(2).pbCanStore("B", 198));
    }
}
