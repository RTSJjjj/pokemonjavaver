package pokemon.runtime.pokemon;

import pokemon.runtime.state.Inventory;

import java.util.Random;

/**
 * {@code pbGenerateWildPokemon} (PField_Encounters:417-463) on top of the
 * identity a {@code PokeBattle_Pokemon} rolls in its constructor
 * (PokeBattle_Pokemon:909-964).
 *
 * <p>Until R15 the runtime only rolled IVs and a random nature, so every wild
 * Pokemon had {@code personalID == 0}: one fixed gender, a nature unrelated to
 * the personality value, no ability slot, no held item, no Pokérus and never a
 * shiny.</p>
 *
 * <p>The plugin reads {@code ability}, {@code nature} and {@code gender} lazily
 * from the final personal id plus whatever flag a step set along the way
 * (PokeBattle_Pokemon:166-177/219-245/286-288), so this generator derives them
 * at the end, after the Shiny Charm's re-roll.</p>
 */
public final class WildGenerator {

    /** PokeBattle_Pokemon:58 {@code IV_STAT_LIMIT}. */
    private static final int IV_STAT_LIMIT = 31;
    /** Settings:45 {@code POKERUS_CHANCE} (out of 65536). */
    private static final int POKERUS_CHANCE = 3;
    /** PField_Encounters:419-421: {@code rand(4096) < 256} gives the hidden ability. */
    private static final int HIDDEN_ABILITY_ROLL = 4096;
    private static final int HIDDEN_ABILITY_CHANCE = 256;
    /** PField_Encounters:425-427 {@code chances}. */
    private static final int[] ITEM_CHANCES = {50, 5, 1};
    private static final int[] COMPOUND_EYES_CHANCES = {60, 20, 5};
    private static final int[] SUPER_LUCK_CHANCES = {50, 50, 50};
    /** PokeBattle_Pokemon:287 {@code @personalID % 25}. */
    private static final int NATURE_COUNT = 25;

    private WildGenerator() {
    }

    /**
     * {@code pbGenerateWildPokemon(species, level, isRoamer=false)}
     * (PField_Encounters:417-463). The caller supplies what the plugin reads off
     * {@code $Trainer} / {@code $PokemonBag} / {@code $game_map}.
     *
     * @param trainer  the player, for the lead Pokemon's ability, the OT data
     *                 and the trainer id the shiny formula needs
     * @param bag      the bag (Shiny Charm); null when there is none
     * @param mapId    {@code $game_map.map_id} for {@code @obtainMap}
     * @param random   the generator's randomness
     */
    public static Pokemon generate(PbsData data, PbsData.Species species, int level,
                                   TrainerState trainer, Inventory bag, int mapId,
                                   boolean fatefulEncounter, Random random) {
        if (data == null || species == null) {
            return null;
        }
        Random source = random == null ? new Random() : random;
        Pokemon pokemon = new Pokemon(species, level, data);

        // --- PokeBattle_Pokemon.new (909-964) ---
        pokemon.personalID = Pokemon.newPersonalID(source);              // :919-922
        pokemon.ivs = new int[6];                                        // :925-931
        for (int i = 0; i < pokemon.ivs.length; i++) {
            pokemon.ivs[i] = source.nextInt(IV_STAT_LIMIT + 1);
        }
        pokemon.evs = new int[6];
        pokemon.item = null;                                             // :935 @item = 0
        pokemon.ballused = 0;                                            // :939
        if (trainer != null) {                                           // :941-945
            pokemon.setTrainerID(trainer.id);
            pokemon.originalTrainer = trainer.name;
            pokemon.otGender = trainer.gender;
        } else {
            pokemon.setTrainerID(0);                                     // :947-949
            pokemon.originalTrainer = "";
            pokemon.otGender = 2;
        }
        pokemon.obtainMap = mapId;                                       // :951
        pokemon.obtainLevel = level;                                     // :953
        pokemon.obtainMode = fatefulEncounter ? 4 : 0;                   // :954-955
        pokemon.hp = pokemon.maxHp();                                    // :960

        Pokemon lead = trainer == null ? null : trainer.first();

        // --- pbGenerateWildPokemon (417-463) ---
        boolean hiddenAbility = source.nextInt(HIDDEN_ABILITY_ROLL) < HIDDEN_ABILITY_CHANCE;
        giveHeldItem(pokemon, species, lead, source);                    // :422-435
        if (bag != null && bag.has("SHINYCHARM")) {                      // :436-442
            for (int i = 0; i < 2; i++) {
                if (Pokemon.isShiny(pokemon.personalID, pokemon.trainerID)) {
                    break;
                }
                pokemon.personalID = Pokemon.newPersonalID(source);
            }
        }
        if (source.nextInt(65536) < POKERUS_CHANCE) {                    // :443-446
            givePokerus(pokemon, source);
        }
        if (lead != null) {                                              // :448-459
            if (hasAbility(lead, "CUTECHARM") && !PokemonStats.singleGender(species.genderRate)) {
                if (lead.effectiveGender() == PokemonStats.MALE) {
                    pokemon.gender = source.nextInt(3) < 2
                            ? PokemonStats.FEMALE : PokemonStats.MALE;
                } else if (lead.effectiveGender() == PokemonStats.FEMALE) {
                    pokemon.gender = source.nextInt(3) < 2
                            ? PokemonStats.MALE : PokemonStats.FEMALE;
                }
            } else if (hasAbility(lead, "SYNCHRONIZE")) {
                pokemon.nature = lead.nature;                            // setNature(firstPkmn.nature)
            }
        }

        // --- the lazy derivations, from the final personal id (166-288) ---
        pokemon.ability = hiddenAbility ? hiddenAbility(species) : naturalAbility(species, pokemon);
        if (pokemon.nature == null) {
            pokemon.nature = natureOf(data, pokemon);
        }
        pokemon.shiny = Pokemon.isShiny(pokemon.personalID, pokemon.trainerID);
        return pokemon;
    }

    /**
     * {@code ability} with {@code abilIndex = @personalID&1}
     * (PokeBattle_Pokemon:224-245), including the plugin's fallback to the other
     * natural slot when the species leaves one empty.
     */
    public static String naturalAbility(PbsData.Species species, Pokemon pokemon) {
        if (species == null) {
            return null;
        }
        int index = pokemon == null ? 0 : pokemon.personalID & 1;
        if (species.abilities.size > index) {
            return species.abilities.get(index);
        }
        return species.abilities.size > 0 ? species.abilities.get(0) : null;   // :241
    }

    /** {@code setAbility(2)} -> {@code SpeciesHiddenAbility} (:227-234). */
    public static String hiddenAbility(PbsData.Species species) {
        if (species == null) {
            return null;
        }
        if (species.hiddenAbility != null && !species.hiddenAbility.isEmpty()) {
            return species.hiddenAbility;
        }
        return species.abilities.size > 0 ? species.abilities.get(0) : null;
    }

    /**
     * {@code nature = @natureflag || (@personalID%25)} (PokeBattle_Pokemon:286-288).
     */
    public static PbsData.Nature natureOf(PbsData data, Pokemon pokemon) {
        if (data == null || data.natures.size == 0 || pokemon == null) {
            return null;
        }
        return data.natures.get(Math.floorMod(pokemon.personalID, NATURE_COUNT)
                % data.natures.size);
    }

    /** PField_Encounters:422-435: the species' wild hold items and their odds. */
    private static void giveHeldItem(Pokemon pokemon, PbsData.Species species, Pokemon lead,
                                     Random random) {
        String[] items = {
            species.wildItems.common, species.wildItems.uncommon, species.wildItems.rare,
        };
        int[] chances = ITEM_CHANCES;
        if (hasAbility(lead, "COMPOUNDEYES")) {
            chances = COMPOUND_EYES_CHANCES;                             // :426
        } else if (hasAbility(lead, "SUPERLUCK")) {
            chances = SUPER_LUCK_CHANCES;                                // :427
        }
        int roll = random.nextInt(100);
        boolean allEqual = items[0] != null && items[0].equals(items[1])
                && items[1].equals(items[2]);                            // :429
        if (allEqual || roll < chances[0]) {
            pokemon.item = items[0];
        } else if (roll < chances[0] + chances[1]) {
            pokemon.item = items[1];
        } else if (roll < chances[0] + chances[1] + chances[2]) {
            pokemon.item = items[2];
        }
    }

    /** {@code givePokerus} (PokeBattle_Pokemon:368-374). */
    private static void givePokerus(Pokemon pokemon, Random random) {
        int strain = 1 + random.nextInt(15);
        int time = 1 + (strain % 4);
        pokemon.pokerus = time | (strain << 4);
    }

    /** {@code pkmn.hasAbility?(:X)} (PokeBattle_Pokemon:248-252). */
    private static boolean hasAbility(Pokemon pokemon, String ability) {
        return pokemon != null && ability.equals(pokemon.ability);
    }
}
