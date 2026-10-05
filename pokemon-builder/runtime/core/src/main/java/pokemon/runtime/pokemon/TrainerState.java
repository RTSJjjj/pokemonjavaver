package pokemon.runtime.pokemon;

/**
 * Stage 3 / P1: the player trainer's Pokemon state, the Essentials
 * {@code $Trainer} core: name, gender, money, the party, the PC storage and the
 * respawn point set by {@code pbSetPokemonCenter}. The Pokemon construction
 * scripts in events fill the party through {@code pbGenPkmn} /
 * {@code pbAddPokemon}; the bag, badges, seen / owned flags, the PC UI and the
 * save format arrive with P1/P4.
 *
 * <p>Pokemon that do not fit in the six party slots go to {@link Storage}, so
 * an event can never silently drop a gift. The save system serialises this
 * object in P1.</p>
 */
public final class TrainerState {

    /** Backwards-compatible alias of {@link Party#LIMIT}. */
    public static final int PARTY_LIMIT = Party.LIMIT;

    public String name = "训练家";
    public int gender = PokemonStats.MALE;
    public int money;
    public final Party party = new Party();
    public final Storage storage = new Storage();

    /** Respawn point written by {@code pbSetPokemonCenter} (-1 = unset). */
    public int healMapId = -1;
    public int healX;
    public int healY;
    public int healDirection;

    public int partyCount() {
        return party.size();
    }

    public Pokemon first() {
        return party.first();
    }

    /**
     * Adds a Pokemon to the party, or to the PC storage when the party is full.
     *
     * @return true when it joined the party, false when it went to storage
     */
    public boolean addToParty(Pokemon pokemon) {
        if (pokemon == null) {
            return false;
        }
        if (party.add(pokemon)) {
            return true;
        }
        storage.store(pokemon);
        return false;
    }

    /** Essentials {@code pbSetPokemonCenter}: remembers the PokeCenter respawn. */
    public void setPokemonCenter(int mapId, int x, int y, int direction) {
        this.healMapId = mapId;
        this.healX = x;
        this.healY = y;
        this.healDirection = direction;
    }

    public boolean hasPokemonCenter() {
        return healMapId >= 0;
    }

    /** Heals the whole party (PokeCenter), returning true when it changed. */
    public boolean healParty() {
        return party.heal();
    }
}
