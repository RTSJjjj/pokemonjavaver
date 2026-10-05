package pokemon.runtime.pokemon;

import com.badlogic.gdx.utils.Array;

/**
 * Stage 3 / P0: one owned Pokemon (the Essentials {@code PokeBattle_Pokemon}
 * core): species (or alternate form), level, IVs/EVs, nature, ability, moves,
 * current HP, gender, shininess, egg state and held item.
 *
 * <p>Battle/party behaviour arrives in later phases; this class owns the data
 * and the derived stats so those systems do not each re-implement them.</p>
 */
public final class Pokemon {

    public PbsData.Species species;
    /** Alternate form when this Pokemon uses one (else null). */
    public PbsData.SpeciesForm form;
    /** Species internal name, or the form key (SPECIES_N) when a form is used. */
    public String internalName;
    public String name;
    public int level;
    public int[] ivs = new int[] {0, 0, 0, 0, 0, 0};
    public int[] evs = new int[] {0, 0, 0, 0, 0, 0};
    public PbsData.Nature nature;
    public String ability;
    public final Array<MoveSlot> moves = new Array<>();
    public int hp;
    public String status = "";
    public int gender = PokemonStats.GENDERLESS;
    public boolean shiny;
    public boolean egg;
    public String item;
    public int happiness;
    public int stepsToHatch;
    /** Cumulative experience (P2); level is kept in sync when it grows. */
    public int exp;
    /** Original trainer name written by {@code p.ot = "..."} (P0c). */
    public String originalTrainer;
    /** Essence-battle rank written by {@code p.battleRank = n} (P0c). */
    public int battleRank;

    /** One known move with its current PP (up to 4 by the time battle lands). */
    public static final class MoveSlot {
        public PbsData.Move move;
        public int pp;
        public int maxPp;
        public int ppUp;

        public MoveSlot(PbsData.Move move) {
            this.move = move;
            this.maxPp = move == null ? 0 : move.pp;
        }
    }

    public Pokemon(PbsData.Species species, int level, PbsData data) {
        this.species = species;
        this.internalName = species == null ? null : species.internalName;
        this.name = species == null ? null : species.name;
        this.level = Math.max(1, level);
        this.happiness = species == null ? 0 : species.happiness;
        this.ability = species != null && species.abilities.size > 0 ? species.abilities.get(0) : null;
        if (data != null && species != null) {
            for (int i = 0; i < species.moves.size; i++) {
                PbsData.LearnMove learn = species.moves.get(i);
                if (learn.level <= this.level) {
                    PbsData.Move move = data.move(learn.move);
                    if (move != null) {
                        moves.add(new MoveSlot(move));
                    }
                }
            }
        }
        this.hp = maxHp();
        this.exp = PokemonStats.experienceForLevel(growthRate(), this.level);
    }

    public int baseStat(int index) {
        if (form != null && form.baseStats != null && index < form.baseStats.length) {
            return form.baseStats[index];
        }
        return species == null ? 0 : species.baseStat(index);
    }

    public Array<String> types() {
        if (form != null && form.types != null && form.types.size > 0) {
            return form.types;
        }
        return species == null ? new Array<>() : species.types;
    }

    public String growthRate() {
        return species == null ? "Medium" : species.growthRate;
    }

    public String genderRate() {
        return species == null ? "Genderless" : species.genderRate;
    }

    public int maxHp() {
        return PokemonStats.maxHp(baseStat(PokemonStats.HP), ivs[PokemonStats.HP],
                evs[PokemonStats.HP], level);
    }

    /** Non-HP stats (index 1..5) with the nature applied. */
    public int attack() {
        return stat(PokemonStats.ATTACK);
    }

    public int defense() {
        return stat(PokemonStats.DEFENSE);
    }

    public int speed() {
        return stat(PokemonStats.SPEED);
    }

    public int spAtk() {
        return stat(PokemonStats.SPATK);
    }

    public int spDef() {
        return stat(PokemonStats.SPDEF);
    }

    public int stat(int index) {
        if (index == PokemonStats.HP) {
            return maxHp();
        }
        return PokemonStats.stat(baseStat(index), ivs[index], evs[index], level,
                PokemonStats.natureMultiplier(nature, index));
    }

    /** Experience to reach the current level (the level is the source of truth). */
    public int experience() {
        return exp;
    }

    /**
     * Adds battle experience (P2) and levels the Pokemon up while the curve
     * allows. Levelling refills HP to the new maximum, like a fresh level-up.
     *
     * @return true when the level changed
     */
    public boolean gainExperience(int amount) {
        if (amount <= 0 || egg) {
            return false;
        }
        exp += amount;
        String growth = growthRate();
        int previous = level;
        int previousMax = maxHp();
        while (level < 100 && exp >= PokemonStats.experienceForLevel(growth, level + 1)) {
            level++;
        }
        if (level != previous) {
            // A level-up raises the current HP by the maximum-HP increase (the
            // project's behaviour), it does not fully heal the Pokemon.
            hp = Math.min(maxHp(), hp + (maxHp() - previousMax));
            return true;
        }
        return false;
    }

    /** Experience still needed to reach the next level (0 at level 100). */
    public int experienceToNextLevel() {
        return level >= 100 ? 0 : PokemonStats.experienceForLevel(growthRate(), level + 1) - exp;
    }

    public boolean fainted() {
        return hp <= 0;
    }

    public boolean genderless() {
        return gender == PokemonStats.GENDERLESS;
    }
}
