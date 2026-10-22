package pokemon.runtime.data;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RubyHashOrderTest {
    /** Data/Map402.rxdata saves its @events keys in this order (read with Marshal); Ruby lists them the same way. */
    private static final int[] MAP402 = {5, 16, 11, 17, 6, 1, 12, 18, 7, 2, 13, 8, 19, 3, 14, 9, 20, 15, 4, 21, 10};

    @Test void map402KeepsTheOrderItWasSavedIn() {
        assertArrayEquals(MAP402, RubyHashOrder.gameMapEvents(MAP402));
    }

    @Test void idOrderStillPutsEvent17BeforeEvent2() {
        int[] ids = new int[21];
        for (int i = 0; i < ids.length; i++) ids[i] = i + 1;
        int[] order = RubyHashOrder.gameMapEvents(ids);
        int at2 = -1, at17 = -1;
        for (int i = 0; i < order.length; i++) {
            if (order[i] == 2) at2 = i;
            if (order[i] == 17) at17 = i;
        }
        assertTrue(at17 >= 0 && at17 < at2, "bin 35%11=2 comes before bin 5%11=5");
    }

    @Test void aGrownTableRehashes() {
        int[] ids = new int[70];
        for (int i = 0; i < ids.length; i++) ids[i] = i + 1;
        int[] order = RubyHashOrder.iterate(ids);
        // the 67th insert sees 66 / 11 > 5: 19 bins; the rehash walks the old bins (66 in 1, 28 in 2, 47 in 7, 9 in 8),
        // pushing each onto the head of new bin 0: 9, 47, 28, 66
        assertArrayEquals(new int[] {9, 47, 28, 66}, java.util.Arrays.copyOf(order, 4));
    }
}
