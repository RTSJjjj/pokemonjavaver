package pokemon.runtime.state;

import com.badlogic.gdx.utils.ObjectIntMap;

/**
 * Minimal item counts (R7.2b) so {@code pbItemBall} / {@code pbReceiveItem} /
 * {@code pbDeleteItem} IR has something to change. The full bag (pockets, UI)
 * belongs to the stage 3 Pokemon domain; the save system reads it in R10.
 */
public final class Inventory {

    private final ObjectIntMap<String> counts = new ObjectIntMap<>();
    private final BagMemory memory = new BagMemory();

    /** The pocket and cursor the field bag was left on (saved with the game). */
    public BagMemory bagMemory() {
        return memory;
    }

    /** Adds items (negative removes); returns the new amount, never below zero. */
    public int add(String item, int amount) {
        if (item == null || item.isEmpty() || amount == 0) {
            return count(item);
        }
        int now = Math.max(0, count(item) + amount);
        if (now == 0) {
            counts.remove(item, 0);
        } else {
            counts.put(item, now);
        }
        return now;
    }

    public int remove(String item, int amount) {
        return add(item, -Math.abs(amount));
    }

    public int count(String item) {
        return item == null ? 0 : counts.get(item, 0);
    }

    public boolean has(String item) {
        return count(item) > 0;
    }

    public int size() {
        return counts.size;
    }

    public void clear() {
        counts.clear();
    }

    /** Live view of the counts; used by the save system (R10) to enumerate. */
    public ObjectIntMap<String> counts() {
        return counts;
    }
}
