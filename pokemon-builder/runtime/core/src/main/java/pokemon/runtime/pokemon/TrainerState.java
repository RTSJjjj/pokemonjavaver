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
    /**
     * PokeBattle_Trainer:259-266: the trainer's 32-bit id ({@code @id}), whose
     * low half is the public ID shown on the trainer card. A Pokemon's
     * {@code trainerID} copies it, and it is the other half of the shiny
     * formula (PokeBattle_Pokemon:314-321).
     */
    public int id;
    public final Party party = new Party();
    public final Storage storage = new Storage(party);
    /** Region zero is the legacy storage; optional fields preserve v1/v2 saves. */
    public final java.util.Map<Integer, Storage> regionalStorage = new java.util.TreeMap<>();
    public final java.util.Set<String> seen = new java.util.TreeSet<>();
    public final java.util.Set<String> owned = new java.util.TreeSet<>();
    public final java.util.Set<Integer> badges = new java.util.TreeSet<>();
    /**
     * PokeBattle_Trainer:15 / :23: {@code $Trainer.pokedex} (the Pokédex was
     * obtained) and {@code $Trainer.pokepc} (the PC was obtained). Both gate
     * their pause menu entries (Modular Menu:67/82).
     */
    public boolean pokedex;
    public boolean pokepc;
    /** PokeBattle_Trainer:19 / Settings:28: the exp pot ("经验储罐"). */
    public int expPot;
    public double playSeconds;
    public int region;

    public Storage storageForRegion(int value) {
        if (value <= 0) return storage;
        return regionalStorage.computeIfAbsent(value, key -> new Storage(party));
    }

    /**
     * {@code PokeBattle_Trainer#publicID} (:35-37): the portion of the id shown
     * on the trainer card.
     */
    public int publicID() {
        return id & 0xFFFF;
    }

    /**
     * A fresh trainer id, exactly like {@code PokeBattle_Trainer#initialize}
     * (:263-266): four independent bytes.
     */
    public void newId(java.util.Random random) {
        java.util.Random source = random == null ? new java.util.Random() : random;
        id = source.nextInt(256)
                | (source.nextInt(256) << 8)
                | (source.nextInt(256) << 16)
                | (source.nextInt(256) << 24);
    }
    public Storage currentStorage() { return storageForRegion(region); }
    public void registerSeen(Pokemon p) {
        if (p != null && !p.egg && p.species != null) seen.add(p.species.internalName);
    }
    public void registerOwned(Pokemon p) {
        registerSeen(p);
        if (p != null && !p.egg && p.species != null) owned.add(p.species.internalName);
    }
    public void reset() {
        name = "训练家"; gender = PokemonStats.MALE; money = 0;
        newId(null);                       // a new game rolls a new trainer id
        party.members().clear(); storage.clear(); regionalStorage.clear();
        seen.clear(); owned.clear(); badges.clear(); playSeconds = 0; region = 0;
        pokedex = false; pokepc = false; expPot = 0;
        healMapId = -1; healX = 0; healY = 0; healDirection = 0;
    }

    /** Respawn point written by {@code pbSetPokemonCenter} (-1 = unset). */
    public int healMapId = -1;
    public int healX;
    public int healY;
    public int healDirection;

    public int partyCount() {
        return party.size();
    }

    /**
     * Essentials {@code $Trainer.pokemonCount}: party members that are not eggs
     * (PokeBattle_Trainer:131-135). The event-glue ball-shake loop counts these.
     */
    public int pokemonCount() {
        int count = 0;
        for (Pokemon pokemon : party.members()) {
            if (pokemon != null && !pokemon.egg) {
                count++;
            }
        }
        return count;
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
            registerOwned(pokemon);
            return true;
        }
        if (!currentStorage().store(pokemon)) throw new IllegalStateException("PC storage is full");
        registerOwned(pokemon);
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
