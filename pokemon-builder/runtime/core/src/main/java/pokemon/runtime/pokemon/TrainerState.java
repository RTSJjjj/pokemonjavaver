package pokemon.runtime.pokemon;

import com.badlogic.gdx.utils.Array;

/**
 * Stage 3 / P0c: the player trainer's Pokemon state, the Essentials
 * {@code $Trainer} core: name, gender, money and the party. The Pokemon
 * construction scripts in events fill the party through
 * {@code pbGenPkmn} / {@code pbAddPokemon}; the bag, PC boxes, badges, seen /
 * owned flags and the save format arrive in P1/P4.
 *
 * <p>Pokemon that do not fit in the six party slots go to {@link #storage}, so
 * an event can never silently drop a gift. The save system serialises this
 * object in P1.</p>
 */
public final class TrainerState {

    public static final int PARTY_LIMIT = 6;

    public String name = "训练家";
    public int gender = PokemonStats.MALE;
    public int money;
    public final Array<Pokemon> party = new Array<>();
    /** Overflow box until the PC (P4) exists; nothing is ever discarded. */
    public final Array<Pokemon> storage = new Array<>();

    public int partyCount() {
        return party.size;
    }

    public Pokemon first() {
        return party.size == 0 ? null : party.first();
    }

    /**
     * Adds a Pokemon to the party, or to the overflow box when the party is
     * full.
     *
     * @return true when it joined the party, false when it went to the box
     */
    public boolean addToParty(Pokemon pokemon) {
        if (pokemon == null) {
            return false;
        }
        if (party.size < PARTY_LIMIT) {
            party.add(pokemon);
            return true;
        }
        storage.add(pokemon);
        return false;
    }
}
