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
    /** PokeBattle_Pokemon:335 {{@code superShiny?}}: the rare shiny variant. */
    public boolean superShiny;
    public boolean egg;
    public String item;
    public int happiness;
    public int stepsToHatch;
    /** Ribbons awarded by events (P3). */
    public final Array<String> ribbons = new Array<>();
    /** Cumulative experience (P2); level is kept in sync when it grows. */
    public int exp;
    /** Original trainer name written by {@code p.ot = "..."} (P0c). */
    public String originalTrainer;
    /** Essence-battle rank written by {@code p.battleRank = n} (P0c). */
    public int battleRank;

    // --- Essentials PokeBattle_Pokemon fields the Summary screen shows ---
    /** 32-bit personality value (nature/characteristic/IV tie-break seed). */
    public int personalID;
    /**
     * {@code @trainerID} (PokeBattle_Pokemon:39): the original trainer's full
     * 32-bit id. The visible half is {@link #publicID}, and the whole value is
     * what the shiny formula XORs with the personality value
     * (PokeBattle_Pokemon:314-321).
     */
    public int trainerID;
    /** Trainer ID shown on the summary ({@code trainerID & 0xFFFF}, :67-69). */
    public int publicID;
    /** OT gender: 0 male, 1 female, -1 unknown (base colour). */
    public int otGender = -1;
    /** Ball the Pokemon was caught in (icon_ball_%02d). */
    public int ballused;
    /** Marking bits (6 marks, bit 0..5). */
    public int markings;
    public int obtainMap;
    public int obtainLevel;
    /** 0 met, 1 hatched, 2 traded, 3 fateful, 4 fateful. */
    public int obtainMode;
    public String obtainText;
    /** PokeRus stage: 0 none, 1 infected, 2 cured. */
    public int pokerus;

    /** One known move with its current PP (up to 4 by the time battle lands). */
    public static final class MoveSlot {
        public PbsData.Move move;
        public int pp;
        public int maxPp;
        public int ppUp;

        public MoveSlot(PbsData.Move move) {
            this.move = move;
            this.maxPp = move == null ? 0 : move.pp;
            this.pp = this.maxPp;
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
                        if (moves.size > 4) moves.removeIndex(0);
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

    /**
     * PokeBattle_Pokemon:166-177: the gender the data boxes and menus show.
     * Single-gender species come from their rate; a mixed species uses an
     * explicit male/female (the plugin's {@code @genderflag}, set from
     * trainers.txt in this runtime), and every other Pokemon derives it from
     * its personal id (PBGenderRates:11-23).
     */
    public int displayGender() {
        return effectiveGender();
    }

    /**
     * {@code gender} (PokeBattle_Pokemon:166-177): the sole option for an
     * all-male / all-female / genderless species, otherwise the explicit flag
     * ({@code @genderflag}, this field), otherwise the personality value's low
     * byte against the species' gender threshold (PBGenderRates:11-23).
     */
    public int effectiveGender() {
        String rate = genderRate();
        if (PokemonStats.singleGender(rate)) {
            return PokemonStats.gender(rate, 0f);
        }
        if (gender == PokemonStats.MALE || gender == PokemonStats.FEMALE) {
            return gender;
        }
        return (personalID & 0xFF) < PokemonStats.genderByte(rate)
                ? PokemonStats.FEMALE : PokemonStats.MALE;
    }

    // ------------------------------------------------------------------
    // Mega Evolution / Primal Reversion (Pokemon_MegaEvolution)
    // ------------------------------------------------------------------

    /** True when the current form is a Mega form (it has an unmegaForm). */
    public boolean isMega() {
        return form != null && form.unmegaForm != null;
    }

    /** {@code getMegaForm}: the form index whose MegaStone the Pokemon holds. */
    public int megaFormIndex(PbsData data) {
        if (data == null || species == null) {
            return 0;
        }
        int current = form == null ? 0 : form.form;
        for (int i = 1; i < 40; i++) {
            PbsData.SpeciesForm candidate = data.form(species.internalName, i);
            if (candidate == null || candidate.megaStone == null || candidate.megaStone.isEmpty()) {
                continue;
            }
            if (item == null || !item.equalsIgnoreCase(candidate.megaStone)) {
                continue;
            }
            int unmega = candidate.unmegaForm == null ? 0 : candidate.unmegaForm;
            if (current == unmega) {
                return i;
            }
        }
        return 0;
    }

    public boolean hasMegaForm(PbsData data) {
        int index = megaFormIndex(data);
        return index > 0 && index != (form == null ? 0 : form.form);
    }

    /** {@code makeMega}: switch to the Mega form (keeps the base species). */
    public void makeMega(PbsData data) {
        int index = megaFormIndex(data);
        if (index > 0) {
            setForm(data, index);
        }
    }

    public void makeUnmega(PbsData data) {
        if (isMega()) {
            setForm(data, form.unmegaForm == null ? 0 : form.unmegaForm);
        }
    }

    /**
     * {@code pbRecordFirstMoves} (PokeBattle_Pokemon:524-527): the moves the
     * Pokemon knew when it was obtained, which the move relearner offers back
     * (PScreen_MoveRelearner:21-22). {@code PokeBattle_BattleCommon:156} calls
     * it on a successful capture.
     */
    public final Array<String> firstMoves = new Array<>();

    public void recordFirstMoves() {
        firstMoves.clear();
        for (MoveSlot slot : moves) {
            if (slot != null && slot.move != null && slot.move.internalName != null) {
                firstMoves.add(slot.move.internalName);
            }
        }
    }

    /**
     * {@code @trainerID = value}: the visible half follows the full id, like the
     * plugin's derived {@code publicID} (:67-69).
     */
    public void setTrainerID(int value) {
        this.trainerID = value;
        this.publicID = value & 0xFFFF;
    }

    /** Settings:44 {@code SHINY_POKEMON_CHANCE}. */
    public static final int SHINY_POKEMON_CHANCE = 32;

    /**
     * {@code shiny?} (PokeBattle_Pokemon:314-321):
     * {@code a = personalID ^ trainerID; d = (a & 0xFFFF) ^ ((a >> 16) & 0xFFFF);
     * d < SHINY_POKEMON_CHANCE}.
     */
    public static boolean isShiny(int personalID, int trainerID) {
        int a = personalID ^ trainerID;
        int d = (a & 0xFFFF) ^ ((a >> 16) & 0xFFFF);
        return d < SHINY_POKEMON_CHANCE;
    }

    /**
     * {@code @personalID = rand(256) | rand(256)&lt;&lt;8 | ... }
     * (PokeBattle_Pokemon:919-922): four independent bytes.
     */
    public static int newPersonalID(java.util.Random random) {
        java.util.Random source = random == null ? new java.util.Random() : random;
        return source.nextInt(256)
                | (source.nextInt(256) << 8)
                | (source.nextInt(256) << 16)
                | (source.nextInt(256) << 24);
    }

    /** Mega form name (the form's formName, or "超级{species}"). */
    public String megaName() {
        if (form != null && form.formName != null && !form.formName.isEmpty()) {
            return form.formName;
        }
        return "超级" + (species == null ? "" : species.name);
    }

    /** Groudon/Kyogre revert with the Red/Blue Orb (Primal Reversion). */
    public boolean hasPrimalForm() {
        return isSpecies("GROUDON") || isSpecies("KYOGRE");
    }

    public boolean isPrimal() {
        return hasPrimalForm() && form != null && form.form == 1 && form.unmegaForm != null;
    }

    public void makePrimal(PbsData data) {
        if (isSpecies("GROUDON") && "REDORB".equals(item)) {
            setForm(data, 1);
        } else if (isSpecies("KYOGRE") && "BLUEORB".equals(item)) {
            setForm(data, 1);
        }
    }

    public void makeUnprimal(PbsData data) {
        if (isPrimal()) {
            setForm(data, 0);
        }
    }

    private boolean isSpecies(String name) {
        return species != null && species.internalName != null
                && name.equalsIgnoreCase(species.internalName);
    }

    /** Applies a form index; index 0 is the base species (form = null). */
    public void setForm(PbsData data, int index) {
        form = index <= 0 || data == null || species == null
                ? null : data.form(species.internalName, index);
        if (form != null && form.abilities != null && form.abilities.size > 0) {
            ability = form.abilities.get(0);
        } else if (species != null && species.abilities.size > 0) {
            ability = species.abilities.get(0);
        }
    }
}
