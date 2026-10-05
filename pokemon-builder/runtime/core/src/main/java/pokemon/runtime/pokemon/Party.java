package pokemon.runtime.pokemon;

import com.badlogic.gdx.utils.Array;

/**
 * Stage 3 / P1: the player's party of up to six Pokemon, the Essentials
 * {@code $Trainer.party} core. Slot 0 is the lead (and, later, the follower);
 * adding, removing, swapping, healing and the "first able Pokemon" lookup live
 * here so events, battles (P2) and menus (P4) share one implementation.
 */
public final class Party {

    public static final int LIMIT = 6;

    private final Array<Pokemon> members = new Array<>();

    public int size() {
        return members.size;
    }

    public boolean isFull() {
        return members.size >= LIMIT;
    }

    /** The Pokemon at {@code index}, or null when the slot is empty. */
    public Pokemon get(int index) {
        return index < 0 || index >= members.size ? null : members.get(index);
    }

    public Pokemon first() {
        return members.size == 0 ? null : members.first();
    }

    /** Live view of the members (slot 0 first); the save system enumerates it. */
    public Array<Pokemon> members() {
        return members;
    }

    /** Adds to the first free slot; false when the party already holds six. */
    public boolean add(Pokemon pokemon) {
        if (pokemon == null || isFull()) {
            return false;
        }
        members.add(pokemon);
        return true;
    }

    public Pokemon remove(int index) {
        return get(index) == null ? null : members.removeIndex(index);
    }

    public boolean remove(Pokemon pokemon) {
        return members.removeValue(pokemon, true);
    }

    /** Swaps two slots (Essentials party reordering). */
    public void swap(int first, int second) {
        if (first < 0 || second < 0 || first >= members.size || second >= members.size
                || first == second) {
            return;
        }
        members.swap(first, second);
    }

    public boolean contains(Pokemon pokemon) {
        return members.contains(pokemon, true);
    }

    public int indexOf(Pokemon pokemon) {
        return members.indexOf(pokemon, true);
    }

    public int eggCount() {
        int eggs = 0;
        for (Pokemon pokemon : members) {
            if (pokemon.egg) {
                eggs++;
            }
        }
        return eggs;
    }

    /** Heals every member (PokeCenter); true when at least one changed. */
    public boolean heal() {
        boolean changed = false;
        for (Pokemon pokemon : members) {
            boolean damaged = pokemon.hp < pokemon.maxHp();
            boolean statused = pokemon.status != null && !pokemon.status.isEmpty();
            if (damaged || statused) {
                pokemon.hp = pokemon.maxHp();
                pokemon.status = "";
                changed = true;
            }
        }
        return changed;
    }

    /** Essentials {@code pbFirstAblePokemon}: first non-fainted, non-egg member. */
    public Pokemon firstAble() {
        for (Pokemon pokemon : members) {
            if (!pokemon.fainted() && !pokemon.egg) {
                return pokemon;
            }
        }
        return null;
    }
}
