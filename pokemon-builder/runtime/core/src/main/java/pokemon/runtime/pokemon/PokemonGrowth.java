package pokemon.runtime.pokemon;

/**
 * Stage 3 / P3: the growth effects around a level-up - learning the moves the
 * species learns at the new level(s) and evolving on a reached level condition.
 * Item / trade / location evolutions need the item-use and trade flows (P4);
 * {@link #evolve} is the shared primitive they will call.
 */
public final class PokemonGrowth {

    private PokemonGrowth() {
    }

    /**
     * Applies the level-up effects for a Pokemon that grew from {@code oldLevel}
     * to its current level: learn every move in (oldLevel, level], then evolve on
     * a reached or newly satisfied level / happiness condition.
     *
     * @return true when a move was learned or the Pokemon evolved
     */
    public static boolean afterLevelUp(Pokemon pokemon, PbsData data, int oldLevel) {
        if (data == null || pokemon == null || pokemon.egg || pokemon.species == null
                || pokemon.level <= oldLevel) {
            return false;
        }
        boolean changed = false;
        for (PbsData.LearnMove learn : pokemon.species.moves) {
            if (learn.level > oldLevel && learn.level <= pokemon.level) {
                changed |= learnMove(pokemon, data.move(learn.move));
            }
        }
        return evolveOnCondition(pokemon, data) || changed;
    }

    /** Adds a move when it is not known; the four-move replace prompt is P4. */
    public static boolean learnMove(Pokemon pokemon, PbsData.Move move) {
        if (move == null) {
            return false;
        }
        for (int i = 0; i < pokemon.moves.size; i++) {
            PbsData.Move known = pokemon.moves.get(i).move;
            if (known != null && known.internalName != null
                    && known.internalName.equals(move.internalName)) {
                return false;
            }
        }
        if (pokemon.moves.size >= 4) {
            return false;
        }
        pokemon.moves.add(new Pokemon.MoveSlot(move));
        return true;
    }

    /**
     * Evolves on the first reached level / happiness evolution. Time of day is
     * not modelled yet, so LevelDay / LevelNight behave like Level; item, trade
     * and the remaining special methods are handled elsewhere.
     */
    public static boolean evolveOnCondition(Pokemon pokemon, PbsData data) {
        if (pokemon == null || pokemon.species == null) {
            return false;
        }
        for (PbsData.Evolution evolution : pokemon.species.evolutions) {
            if (!levelConditionReached(evolution, pokemon)) {
                continue;
            }
            PbsData.Species target = data.species(evolution.species);
            if (target != null) {
                evolve(pokemon, target);
                return true;
            }
        }
        return false;
    }

    private static boolean levelConditionReached(PbsData.Evolution evolution, Pokemon pokemon) {
        int parameter = intParameter(evolution.parameter);
        if (parameter < 0) {
            return false;
        }
        switch (evolution.method) {
            case "Level":
            case "LevelDay":
            case "LevelNight":
            case "LevelDarkInParty":
                return pokemon.level >= parameter;
            case "LevelMale":
                return pokemon.gender == PokemonStats.MALE && pokemon.level >= parameter;
            case "LevelFemale":
                return pokemon.gender == PokemonStats.FEMALE && pokemon.level >= parameter;
            case "Happiness":
            case "HappinessDay":
            case "HappinessNight":
                return pokemon.happiness >= parameter;
            default:
                return false;
        }
    }

    private static int intParameter(String parameter) {
        try {
            return parameter == null ? -1 : Integer.parseInt(parameter.trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    /**
     * Replaces the species, keeping the moves, IVs / EVs, nickname and the
     * current HP (raised by the maximum-HP increase). The ability follows the
     * evolved species unless the Pokemon kept a hidden one.
     */
    public static void evolve(Pokemon pokemon, PbsData.Species target) {
        String oldName = pokemon.species == null ? null : pokemon.species.name;
        int oldMax = pokemon.maxHp();
        pokemon.species = target;
        pokemon.form = null;
        pokemon.internalName = target.internalName;
        if (pokemon.name == null || pokemon.name.isEmpty() || pokemon.name.equals(oldName)) {
            pokemon.name = target.name;
        }
        if (target.abilities.size > 0) {
            pokemon.ability = target.abilities.get(0);
        }
        pokemon.hp = Math.min(pokemon.maxHp(), pokemon.hp + (pokemon.maxHp() - oldMax));
    }
}
