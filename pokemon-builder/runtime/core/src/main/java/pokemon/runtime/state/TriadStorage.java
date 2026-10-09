package pokemon.runtime.state;

import java.util.ArrayList;
import java.util.List;

/**
 * 235_PMinigame_TripleTriad {@code TriadStorage} (:1002-1062) with {@code ItemStorageHelper} (195_PItem_Bag:295-369): the cards
 * the player owns, one slot per species, up to {@link #MAXPERSLOT} of each and as many slots as there are species.
 */
public final class TriadStorage {
    public static final int MAXPERSLOT = 99;                                    // :1011 maxPerSlot

    /** One slot: {@code [species, count]}. */
    public static final class Slot {
        public final String species;
        public int count;

        Slot(String species, int count) {
            this.species = species;
            this.count = count;
        }
    }

    private final int maxSize;
    private final List<Slot> items = new ArrayList<>();

    /** @param maxSize {@code PBSpecies.getCount} (:1007) */
    public TriadStorage(int maxSize) {
        this.maxSize = Math.max(1, maxSize);
    }

    public int length() {
        return items.size();
    }

    public boolean empty() {
        return items.isEmpty();
    }

    public Slot get(int index) {
        return index < 0 || index >= items.size() ? null : items.get(index);
    }

    /** {@code pbQuantity(item)} */
    public int pbQuantity(String species) {
        int ret = 0;
        for (Slot slot : items) if (slot.species.equals(species)) ret += slot.count;
        return ret;
    }

    /** {@code pbCanStore?(item,qty)} (:327-343). */
    public boolean pbCanStore(String species, int qty) {
        if (qty == 0) return true;
        for (int i = 0; i < maxSize; i++) {
            Slot slot = i < items.size() ? items.get(i) : null;
            if (slot == null) {
                qty -= Math.min(qty, MAXPERSLOT);
                if (qty == 0) return true;
            } else if (slot.species.equals(species) && slot.count < MAXPERSLOT) {
                int newamt = Math.min(slot.count + qty, MAXPERSLOT);
                qty -= newamt - slot.count;
                if (qty == 0) return true;
            }
        }
        return false;
    }

    /** {@code pbStoreItem(item,qty)} (:345-369). */
    public boolean pbStoreItem(String species, int qty) {
        if (qty == 0) return true;
        for (int i = 0; i < maxSize; i++) {
            Slot slot = i < items.size() ? items.get(i) : null;
            if (slot == null) {
                Slot added = new Slot(species, Math.min(qty, MAXPERSLOT));
                items.add(added);
                qty -= added.count;
                if (qty == 0) return true;
            } else if (slot.species.equals(species) && slot.count < MAXPERSLOT) {
                int newamt = Math.min(slot.count + qty, MAXPERSLOT);
                qty -= newamt - slot.count;
                slot.count = newamt;
                if (qty == 0) return true;
            }
        }
        return false;
    }

    /** {@code pbDeleteItem(item,qty)} (:308-326). */
    public boolean pbDeleteItem(String species, int qty) {
        if (qty == 0) return true;
        boolean ret = false;
        for (int i = 0; i < items.size(); i++) {
            Slot slot = items.get(i);
            if (!slot.species.equals(species)) continue;
            int amount = Math.min(qty, slot.count);
            slot.count -= amount;
            qty -= amount;
            if (qty > 0) continue;
            ret = true;
            break;
        }
        items.removeIf(slot -> slot.count == 0);
        return ret;
    }

    /** Save support: restores one slot as stored. */
    public void restore(String species, int count) {
        if (species != null && count > 0 && items.size() < maxSize) items.add(new Slot(species, Math.min(count, MAXPERSLOT)));
    }
}
