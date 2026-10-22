package pokemon.runtime.data;

import java.util.ArrayList;
import java.util.List;

/**
 * The iteration order of a Ruby 1.8 Hash with Integer keys (RGSS1's st_table): {@code $game_map.events.values}.
 *
 * <p>Interpreter#setup_starting_event (048_Interpreter:70-83) starts the first starting event of that iteration, so
 * when two autorun pages are active at once the one Ruby lists first wins - not the lower event id. Map 402: events 2
 * and 17 are both autorun when the player comes back from map 401, and Ruby lists 17 first.</p>
 *
 * <p>st_table: a Fixnum's hash is its VALUE ({@code 2n+1}), the bin is {@code hash % num_bins}, a new entry goes to the
 * head of its bin's chain, the table starts with 11 bins and grows to the next prime of {@code new_size} once
 * {@code num_entries / num_bins > 5} (checked before the insert); a rehash walks the old bins in order and pushes each
 * entry onto the head of its new bin. Iteration walks the bins in order, each chain from its head.</p>
 */
public final class RubyHashOrder {

    private static final int[] PRIMES = {11, 19, 37, 67, 131, 283, 521, 1033, 2053, 4099, 8219, 16427, 32771, 65581,
            131101, 262147, 524309, 1048583};

    private RubyHashOrder() { }

    /**
     * {@code $game_map.events.keys}: Marshal.load fills {@code @map.events} in the file's key order, then
     * Game_Map#setup (027_Game_Map:61-64) inserts every key of that hash, in its iteration order, into {@code @events}.
     */
    public static int[] gameMapEvents(int[] fileKeys) {
        return iterate(iterate(fileKeys));
    }

    /** The iteration order of a Hash filled with {@code keys} in this order. */
    public static int[] iterate(int[] keys) {
        int bins = newSize(0);
        List<List<Integer>> table = newBins(bins);
        int count = 0;
        for (int key : keys) {
            if (count / bins > 5) {                                     // st.c add_direct: ST_DEFAULT_MAX_DENSITY
                int grown = newSize(bins + 1);
                List<List<Integer>> next = newBins(grown);
                for (List<Integer> chain : table) {
                    for (int old : chain) {
                        next.get(bin(old, grown)).add(0, old);
                    }
                }
                table = next;
                bins = grown;
            }
            table.get(bin(key, bins)).add(0, key);
            count++;
        }
        int[] out = new int[count];
        int i = 0;
        for (List<Integer> chain : table) {
            for (int key : chain) {
                out[i++] = key;
            }
        }
        return out;
    }

    private static int bin(int key, int bins) {
        long value = ((long) key << 1) | 1L;                            // the Fixnum VALUE
        return (int) Long.remainderUnsigned(value & 0xffffffffL, bins);
    }

    private static int newSize(int size) {
        int candidate = 8;
        for (int prime : PRIMES) {
            if (candidate > size) {
                return prime;
            }
            candidate <<= 1;
        }
        return PRIMES[PRIMES.length - 1];
    }

    private static List<List<Integer>> newBins(int count) {
        List<List<Integer>> bins = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            bins.add(new ArrayList<>());
        }
        return bins;
    }
}
