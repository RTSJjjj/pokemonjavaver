package pokemon.runtime.state;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code PCItemStorage} (195_PItem_Bag:237-286) with {@code ItemStorageHelper} (:295-369): up to 50 slots of up to
 * 999 of one item, filled in order.
 */
public final class PcItemStorage {
    public static final int MAXSIZE = 50;
    public static final int MAXPERSLOT = 999;

    /** One slot: {@code [itemID, itemCount]}. */
    public static final class Slot {
        public final String item;
        public int count;

        Slot(String item, int count) {
            this.item = item;
            this.count = count;
        }
    }

    private final List<Slot> items = new ArrayList<>();

    /** A new storage starts with a Potion (:244). */
    public static PcItemStorage withPotion(boolean potionExists) {
        PcItemStorage storage = new PcItemStorage();
        if (potionExists) {
            storage.pbStoreItem("POTION", 1);
        }
        return storage;
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

    public void clear() {
        items.clear();
    }

    /** {@code pbQuantity(item)} (:298-304). */
    public int pbQuantity(String item) {
        int ret = 0;
        for (Slot slot : items) {
            if (slot.item.equals(item)) {
                ret += slot.count;
            }
        }
        return ret;
    }

    /** {@code pbCanStore?(item, qty)} (:327-343). */
    public boolean pbCanStore(String item, int qty) {
        if (qty == 0) {
            return true;
        }
        for (int i = 0; i < MAXSIZE; i++) {
            Slot slot = i < items.size() ? items.get(i) : null;
            if (slot == null) {
                qty -= Math.min(qty, MAXPERSLOT);
                if (qty == 0) {
                    return true;
                }
            } else if (slot.item.equals(item) && slot.count < MAXPERSLOT) {
                int newamt = Math.min(slot.count + qty, MAXPERSLOT);
                qty -= newamt - slot.count;
                if (qty == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** {@code pbStoreItem(item, qty)} (:345-369). */
    public boolean pbStoreItem(String item, int qty) {
        if (qty == 0) {
            return true;
        }
        for (int i = 0; i < MAXSIZE; i++) {
            Slot slot = i < items.size() ? items.get(i) : null;
            if (slot == null) {
                Slot added = new Slot(item, Math.min(qty, MAXPERSLOT));
                items.add(added);
                qty -= added.count;
                if (qty == 0) {
                    return true;
                }
            } else if (slot.item.equals(item) && slot.count < MAXPERSLOT) {
                int newamt = Math.min(slot.count + qty, MAXPERSLOT);
                qty -= newamt - slot.count;
                slot.count = newamt;
                if (qty == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** {@code pbDeleteItem(item, qty)} (:308-326). */
    public boolean pbDeleteItem(String item, int qty) {
        if (qty == 0) {
            return true;
        }
        boolean ret = false;
        for (int i = 0; i < items.size(); i++) {
            Slot slot = items.get(i);
            if (!slot.item.equals(item)) {
                continue;
            }
            int amount = Math.min(qty, slot.count);
            slot.count -= amount;
            qty -= amount;
            if (qty > 0) {
                continue;
            }
            ret = true;
            break;
        }
        items.removeIf(slot -> slot.count == 0);
        return ret;
    }

    /** Save support: restores one slot as stored. */
    public void restore(String item, int count) {
        if (item != null && count > 0 && items.size() < MAXSIZE) {
            items.add(new Slot(item, Math.min(count, MAXPERSLOT)));
        }
    }
}
