package pokemon.runtime.pokemon;

import com.badlogic.gdx.utils.Array;

/**
 * Stage 3 / P1: the PC Pokemon storage, the Essentials {@code $PokemonStorage}
 * core. {@code NUM_STORAGE_BOXES} boxes of thirty slots (the project's
 * Settings) hold the Pokemon that do not fit in the party; boxes are created
 * lazily so an empty save stays small. The PC UI arrives in P4, but the storage
 * itself is what {@code pbAddPokemon} and the save system already need.
 */
public final class Storage {

    /** Settings::NUM_STORAGE_BOXES (the original project uses 200). */
    public static final int BOXES = 200;
    public static final int SLOTS = 30;

    private final Array<Array<Pokemon>> boxes = new Array<>();

    /** Stores in the first free slot; false when every box is full. */
    public boolean store(Pokemon pokemon) {
        if (pokemon == null) {
            return false;
        }
        for (int index = 0; index < BOXES; index++) {
            Array<Pokemon> box = box(index);
            if (box.size < SLOTS) {
                box.add(pokemon);
                return true;
            }
        }
        return false;
    }

    /** The box at {@code index}, created on demand (never null). */
    public Array<Pokemon> box(int index) {
        while (boxes.size <= index) {
            boxes.add(new Array<>());
        }
        return boxes.get(index);
    }

    public Pokemon get(int box, int slot) {
        if (box < 0 || box >= boxes.size) {
            return null;
        }
        Array<Pokemon> members = boxes.get(box);
        return slot < 0 || slot >= members.size ? null : members.get(slot);
    }

    /** Removes and returns the Pokemon at (box, slot), or null. */
    public Pokemon withdraw(int box, int slot) {
        Pokemon pokemon = get(box, slot);
        if (pokemon != null) {
            boxes.get(box).removeIndex(slot);
        }
        return pokemon;
    }

    /** Live view of the boxes (sparse: only boxes touched so far). */
    public Array<Array<Pokemon>> boxes() {
        return boxes;
    }

    /** How many boxes currently hold at least one Pokemon. */
    public int usedBoxes() {
        int used = 0;
        for (Array<Pokemon> box : boxes) {
            if (box.size > 0) {
                used++;
            }
        }
        return used;
    }

    public int count() {
        int total = 0;
        for (Array<Pokemon> box : boxes) {
            total += box.size;
        }
        return total;
    }

    public void clear() {
        boxes.clear();
    }
}
