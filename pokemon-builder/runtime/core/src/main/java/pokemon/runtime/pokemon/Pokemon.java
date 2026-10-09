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
    public int statusCount;                      // @statusCount (PokeBattle_Pokemon): sleep turns or the toxic counter
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
    /** {@code hatchedMap} / {@code timeEggHatched} (197:47, :98-109; epoch seconds, 0 = unset): where and when an egg hatched. */
    public int hatchedMap;
    public long timeEggHatched;
    public String obtainText;
    /** PokeRus stage: 0 none, 1 infected, 2 cured. */
    public int pokerus;
    /** {@code @beauty} (the contest stat Feebas' Beauty evolution reads). 登记: nothing raises it yet. */
    public int beauty;
    /** {@code @criticalHits} (the Pokemon-side mirror of the battler's, 201_Pokemon_Evolution CriticalHits). */
    public int criticalHits;
    /** {@code @yamaskhp} (the damage Yamask took, 201_Pokemon_Evolution DamageDone). */
    public int yamaskhp;

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

        /** {@code PBMove#totalpp} (085_PBMove:70-73): the move's PP plus 1/5 of it for each PP Up. */
        public int totalPp() {
            int base = move == null ? 0 : move.pp;
            return base + base * ppUp / 5;
        }

        /** {@code ppup=}: keeps {@link #maxPp} equal to {@link #totalPp()}. */
        public void setPpUp(int value) {
            ppUp = value;
            maxPp = totalPp();
        }
    }

    /** The form number ({@code form}, 0 for the base form). */
    public int formIndex() {
        return form == null ? looseForm : form.form;
    }

    /**
     * {@code @form} when the data has no entry for it on the current species (a Magikarp whose {@code form = 2} was set by
     * the Thunder Stone, a Goomy's {@code form = 1}): Ruby keeps the number on the Pokemon, and the species it evolves into
     * has the entry. 0 when {@link #form} is set or the base form is meant.
     */
    public int looseForm;

    /** {@code @fused} (PokeBattle_Pokemon:27): the Pokemon fused into this one (DNA Splicers, N-Solarizer ...). */
    public Pokemon fused;

    /**
     * {@code pbLearnMove(move)} (PokeBattle_Pokemon:467-492): learns the move silently. A known move moves to the end of
     * the list; a full set forgets the first move.
     */
    public void learnMoveSilently(PbsData.Move move) {
        if (move == null || move.internalName == null) {
            return;
        }
        for (int i = 0; i < moves.size; i++) {
            if (moves.get(i).move != null && move.internalName.equals(moves.get(i).move.internalName)) {
                moves.add(moves.removeIndex(i));                               // :470-481 relocate to the end
                return;
            }
        }
        if (moves.size >= 4) {
            moves.removeIndex(0);                                              // :488-491 forget the first move
        }
        moves.add(new MoveSlot(move));                                         // :482-486
    }

    /**
     * {@code calcStats} (PokeBattle_Pokemon:868-891): the stats are derived on the fly here, but the current HP keeps its
     * distance to the maximum: {@code hpDiff = totalhp - hp} before, {@code hp = totalhp - hpDiff} after (:884-888).
     */
    /** {@code level=} (197_PokeBattle_Pokemon:121-127): the level and the start experience of that level. */
    public void setLevelAndExp(int value) {
        level = Math.max(1, value);
        exp = PokemonStats.experienceForLevel(growthRate(), level);
    }

    public void recalculatingStats(Runnable change) {
        int hpDiff = maxHp() - hp;
        change.run();
        int total = maxHp();
        hp = Math.max(0, total - hpDiff);
        if (hp > total) {
            hp = total;
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

    /**
     * {@code resetMoves} (PokeBattle_Pokemon:448-464): the last four distinct moves the species learns up to the
     * current level. 登记: {@code getMoveList} is {@code species.moves}; a form's own list is not modelled here.
     */
    public void resetMoves(PbsData data) {
        java.util.List<String> moveList = new java.util.ArrayList<>();
        if (species != null) {
            for (int i = 0; i < species.moves.size; i++) {                    // :451-452 m[0]<=lvl
                PbsData.LearnMove learn = species.moves.get(i);
                if (learn.level <= level) moveList.add(learn.move);
            }
        }
        java.util.Collections.reverse(moveList);                              // :453
        moveList = new java.util.ArrayList<>(new java.util.LinkedHashSet<>(moveList));   // :454 moveList |= []
        java.util.Collections.reverse(moveList);                              // :455
        int listend = Math.max(0, moveList.size() - 4);                       // :456-457
        moves.clear();
        for (int i = listend; i < listend + 4; i++) {                          // :459-463
            if (i >= moveList.size()) continue;                               // :460 moveid 0 = an empty slot
            PbsData.Move move = data == null ? null : data.move(moveList.get(i));
            if (move != null) moves.add(new MoveSlot(move));
        }
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

    /**
     * How many times {@code @totalhp} was multiplied after the last {@code calcStats}
     * ({@code pkmn.totalhp = pkmn.totalhp * 7}, Boss_Battles:2725); {@code calcStats} (PokeBattle_Pokemon:868-891)
     * writes {@code @totalhp} again and resets it to 1.
     */
    public int totalHpFactor = 1;

    /**
     * {@code changeHappiness(method)} (197_PokeBattle_Pokemon:777-833): the walking / level-up / vitamin / faint ...
     * changes, boosted when positive by the map the Pokemon was met on, a Luxury Ball, the Soothe Bell and super
     * shininess; clamped to 0..255.
     *
     * @param currentMapId {@code $game_map.map_id} (:826)
     * @param luxuryBallType {@code pbGetBallType(:LUXURYBALL)} (:827), -1 when the project has no such ball
     */
    public void changeHappiness(String method, int currentMapId, int luxuryBallType) {
        int gain;
        switch (method) {
            case "walking":
                gain = happiness < 200 ? 2 : 1;                                       // :781-782
                break;
            case "levelup":
                gain = happiness < 100 ? 5 : happiness < 200 ? 4 : 3;                 // :784-786
                break;
            case "groom":
                gain = happiness < 200 ? 10 : 4;                                      // :788-789
                break;
            case "evberry":
                gain = happiness < 100 ? 10 : happiness < 200 ? 5 : 2;                // :791-793
                break;
            case "vitamin":
                gain = happiness < 100 ? 5 : happiness < 200 ? 3 : 2;                 // :795-797
                break;
            case "wing":
                gain = happiness < 100 ? 3 : happiness < 200 ? 2 : 1;                 // :799-801
                break;
            case "machine":
            case "battleitem":
                gain = happiness < 200 ? 1 : 0;                                       // :803-808
                break;
            case "faint":
                gain = -1;                                                            // :809
                break;
            case "faintbad":
                gain = happiness < 200 ? -5 : -10;                                    // :811-813
                break;
            case "powder":
                gain = happiness < 200 ? -5 : -10;                                    // :814-816
                break;
            case "energyroot":
                gain = happiness < 200 ? -10 : -15;                                   // :817-819
                break;
            case "revivalherb":
                gain = happiness < 200 ? -15 : -20;                                   // :820-822
                break;
            default:
                throw new IllegalArgumentException("Unknown happiness-changing method: " + method);   // :824
        }
        if (gain > 0) {                                                               // :826
            if (obtainMap == currentMapId) {
                gain += 1;                                                            // :827
            }
            if (luxuryBallType >= 0 && ballused == luxuryBallType) {
                gain += 1;                                                            // :828
            }
            if ("SOOTHEBELL".equals(item)) {
                gain = (int) Math.floor(gain * 1.5);                                  // :829
            }
            if (superShiny) {
                gain *= 2;                                                            // :830
            }
        }
        happiness += gain;                                                            // :832
        happiness = Math.max(0, Math.min(255, happiness));                            // :833
    }

    public int maxHp() {
        return totalHpFactor * PokemonStats.maxHp(baseStat(PokemonStats.HP), ivs[PokemonStats.HP],
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
        exp = Math.min(exp, PBExperience.pbGetMaxExperience(growth));                 // pbAddExperience (:175-182)
        while (level < PBExperience.MAXIMUM_LEVEL && exp >= PokemonStats.experienceForLevel(growth, level + 1)) {
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
        return level >= PBExperience.MAXIMUM_LEVEL ? 0 : PokemonStats.experienceForLevel(growthRate(), level + 1) - exp;
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
    // Mega Evolution (Pokemon_MegaEvolution:1-76, :125-163)
    // ------------------------------------------------------------------

    /** {@code hasItem?(item)}. */
    private boolean holds(String name) {
        return item != null && item.equalsIgnoreCase(name);
    }

    /** {@code hasMove?(move)}. */
    private boolean knowsMoveNamed(String name) {
        for (MoveSlot slot : moves) {
            if (slot != null && slot.move != null && name.equalsIgnoreCase(slot.move.internalName)) return true;
        }
        return false;
    }

    private int formNumber() {
        return form == null ? 0 : form.form;
    }

    /**
     * {@code MultipleForms.call("getSpecificMegaForm",self)} (:154-158): only Slowbro registers it
     * ({@code next 2 if form==0 && hasItem?(:SLOWBRONITE)}); null is Ruby's nil.
     */
    private Integer specificMegaForm() {
        if (isSpecies("SLOWBRO") && formNumber() == 0 && holds("SLOWBRONITE")) return 2;
        return null;
    }

    /** {@code MultipleForms.call("getSpecificUnmegaForm",self)} (:159-162). */
    private Integer specificUnmegaForm() {
        if (isSpecies("SLOWBRO") && formNumber() == 2) return 0;
        return null;
    }

    /** {@code getMegaForm(checkItemOnly=false)} (:6-33): the form number, or 0 if no accessible Mega form. */
    public int getMegaForm(PbsData data, boolean checkItemOnly) {
        if (data == null || species == null) return 0;
        int ret = 0;                                                                // :9
        Integer specific = specificMegaForm();                                      // :10 hasSpecificMegaForm?
        if (specific != null) {
            ret = specific;                                                         // :11
        } else {
            for (int i = 1; i < 40; i++) {                                          // :14 formData[@species] (0 is the base form)
                PbsData.SpeciesForm fSpec = data.form(species.internalName, i);
                if (fSpec == null) continue;                                        // :16
                if (fSpec.megaStone != null && !fSpec.megaStone.isEmpty() && holds(fSpec.megaStone)) {   // :18
                    int unmegaForm = fSpec.unmegaForm == null ? 0 : fSpec.unmegaForm;   // :19
                    if (formNumber() == unmegaForm) {                               // :20
                        ret = i;                                                    // :21
                        break;
                    }
                }
                if (!checkItemOnly) {                                               // :24
                    if (fSpec.megaMove != null && !fSpec.megaMove.isEmpty() && knowsMoveNamed(fSpec.megaMove)) {   // :26
                        ret = i;                                                    // :27
                        break;
                    }
                }
            }
        }
        return ret;                                                                 // :32
    }

    /** {@code getMegaForm} with the default argument. */
    public int megaFormIndex(PbsData data) {
        return getMegaForm(data, false);
    }

    /** {@code getUnmegaForm} (:35-40): -1 when it is not a Mega form. */
    public int getUnmegaForm(PbsData data) {
        Integer specific = specificUnmegaForm();
        if (!isMega() && specific == null) return -1;                               // :36
        int unmegaForm = form != null && form.unmegaForm != null ? form.unmegaForm : 0;   // :37
        if (specific != null) unmegaForm = specific;                                // :38
        return unmegaForm;                                                          // :39
    }

    /** {@code hasMegaForm?} (:42-45). */
    public boolean hasMegaForm(PbsData data) {
        int megaForm = getMegaForm(data, false);                                    // :43
        return megaForm > 0 && megaForm != formNumber();                            // :44
    }

    /** {@code mega?} (:47-54): the current form has an UnmegaForm. */
    public boolean isMega() {
        return form != null && form.unmegaForm != null;
    }

    /** {@code makeMega} (:58-61). */
    public void makeMega(PbsData data) {
        int megaForm = getMegaForm(data, false);                                    // :59
        if (megaForm > 0) setForm(data, megaForm);                                  // :60
    }

    /** {@code makeUnmega} (:63-66). */
    public void makeUnmega(PbsData data) {
        int unmegaForm = getUnmegaForm(data);                                       // :64
        if (unmegaForm >= 0) setForm(data, unmegaForm);                             // :65
    }

    /**
     * {@code megaMessage} (:73-75): 0 = default message, 1 = Rayquaza message; the data of the form
     * {@code getMegaForm} points at.
     */
    public int megaMessage(PbsData data) {
        int megaForm = getMegaForm(data, false);
        if (data == null || species == null || megaForm <= 0) return 0;
        PbsData.SpeciesForm f = data.form(species.internalName, megaForm);
        return f == null || f.megaMessage == null ? 0 : f.megaMessage;
    }

    /**
     * {@code pbRecordFirstMoves} (PokeBattle_Pokemon:524-527): the moves the
     * Pokemon knew when it was obtained, which the move relearner offers back
     * (PScreen_MoveRelearner:21-22). {@code PokeBattle_BattleCommon:156} calls
     * it on a successful capture.
     */
    public final Array<String> firstMoves = new Array<>();

    /** {@code trmoves} (197_PokeBattle_Pokemon:25, :643-646): the moves taught by Technical Records, which stay relearnable. */
    public final Array<String> trMoves = new Array<>();

    /**
     * {@code getMoveList} (197_PokeBattle_Pokemon:443-445): {@code pbGetSpeciesMoveset(@species, formSimple)}, the level-up
     * list of the form's own entry. 登记: a form without moves of its own reads the species' list.
     */
    public Array<PbsData.LearnMove> getMoveList(PbsData data) {
        if (species != null && data != null && formIndex() > 0) {
            PbsData.SpeciesForm f = data.form(species.internalName, formIndex());
            if (f != null && f.moves != null) return f.moves;
        }
        return species == null ? new Array<>() : species.moves;
    }

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

    /** {@code megaName} (:68-71): the form's name, else "Mega {species}". */
    public String megaName() {
        if (form != null && form.formName != null && !form.formName.isEmpty()) {
            return form.formName;                                                   // :70
        }
        return "Mega " + (species == null ? "" : species.name);                     // :70 _INTL("Mega {1}",...)
    }

    // ------------------------------------------------------------------
    // Primal Reversion (Pokemon_MegaEvolution:84-123)
    // ------------------------------------------------------------------

    /** {@code getPrimalForm} registrations (:111-123): 1 for Groudon with the Red Orb / Kyogre with the Blue Orb, else null. */
    private Integer primalForm() {
        if (isSpecies("GROUDON") && holds("REDORB")) return 1;                      // :113
        if (isSpecies("KYOGRE") && holds("BLUEORB")) return 1;                      // :120
        return null;
    }

    /** {@code hasPrimalForm?} (:85-88). */
    public boolean hasPrimalForm() {
        return primalForm() != null;
    }

    /** {@code primal?} (:90-93). */
    public boolean isPrimal() {
        Integer v = primalForm();
        return v != null && v == formNumber();
    }

    /** {@code makePrimal} (:96-99). */
    public void makePrimal(PbsData data) {
        Integer v = primalForm();
        if (v != null) setForm(data, v);
    }

    /** {@code makeUnprimal} (:101-106): no getUnprimalForm is registered, so {@code primal?} decides. */
    public void makeUnprimal(PbsData data) {
        if (isPrimal()) setForm(data, 0);                                           // :104
    }

    /** {@code isSpecies?(s)} (PokeBattle_Pokemon:668-671). */
    public boolean isSpecies(String name) {
        return species != null && species.internalName != null
                && name.equalsIgnoreCase(species.internalName);
    }

    /** {@code pkmn.clone} (197_PokeBattle_Pokemon:893+): an independent copy (the moves, stats and lists are copied). */
    public Pokemon copy() {
        Pokemon c = new Pokemon(species, level, null);
        c.form = form;
        c.internalName = internalName;
        c.name = name;
        c.ivs = ivs.clone();
        c.evs = evs.clone();
        c.nature = nature;
        c.ability = ability;
        c.moves.clear();
        for (MoveSlot slot : moves) {
            MoveSlot m = new MoveSlot(slot.move);
            m.pp = slot.pp;
            m.maxPp = slot.maxPp;
            m.ppUp = slot.ppUp;
            c.moves.add(m);
        }
        c.hp = hp;
        c.statusCount = statusCount;
        c.status = status;
        c.gender = gender;
        c.shiny = shiny;
        c.superShiny = superShiny;
        c.egg = egg;
        c.item = item;
        c.happiness = happiness;
        c.stepsToHatch = stepsToHatch;
        c.ribbons.addAll(ribbons);
        c.firstMoves.addAll(firstMoves);
        c.trMoves.addAll(trMoves);
        c.exp = exp;
        c.originalTrainer = originalTrainer;
        c.battleRank = battleRank;
        c.personalID = personalID;
        c.trainerID = trainerID;
        c.publicID = publicID;
        c.otGender = otGender;
        c.ballused = ballused;
        c.markings = markings;
        c.obtainMap = obtainMap;
        c.obtainLevel = obtainLevel;
        c.obtainMode = obtainMode;
        c.hatchedMap = hatchedMap;
        c.timeEggHatched = timeEggHatched;
        c.obtainText = obtainText;
        c.pokerus = pokerus;
        c.beauty = beauty;
        c.criticalHits = criticalHits;
        c.yamaskhp = yamaskhp;
        c.fused = fused;
        c.totalHpFactor = totalHpFactor;
        return c;
    }

    /**
     * {@code species=} (197_PokeBattle_Pokemon:658-666) as {@code PokemonEvolutionScene} uses it: the new species keeps the
     * form number, the nickname (a name that is not the old species name), the ability slot (natural 0/1 or the hidden
     * one) and the missing HP; the level follows the experience under the new species' growth rate.
     */
    public void changeSpecies(PbsData data, PbsData.Species target) {
        PbsData.Species old = species;
        boolean nicknamed = old != null && name != null && !name.equals(old.name);
        int formNumber = formIndex();
        int slot = abilitySlot();
        int oldMax = maxHp();
        species = target;
        form = formNumber > 0 && data != null ? data.form(target.internalName, formNumber) : null;
        looseForm = form == null && formNumber > 0 ? formNumber : 0;
        internalName = form != null ? form.key : target.internalName;
        if (!nicknamed) {
            name = target.name;
        }
        if (slot >= 0) {
            ability = abilityForSlot(slot);
        }
        level = Math.max(1, PokemonStats.levelForExperience(growthRate(), exp));
        hp = Math.max(0, Math.min(maxHp(), maxHp() - (oldMax - hp)));
    }

    /** {@code abilityIndex} (197:219-221): the slot set by {@code setAbility}, else the personal id's low bit. */
    public int abilityIndex() {
        int slot = abilitySlot();
        return slot >= 0 ? slot : (personalID & 1);
    }

    /** {@code setAbility(value)} (197:255-257): 0/1 natural, 2 hidden. */
    public void setAbilitySlot(int slot) {
        ability = abilityForSlot(slot);
    }

    /** The ability slot of the current ability: 0/1 natural, 2 hidden, -1 when it is none of the species' own. */
    private int abilitySlot() {
        if (ability == null) {
            return -1;
        }
        Array<String> natural = form != null && form.abilities != null ? form.abilities : species.abilities;
        String hidden = form != null && form.hiddenAbility != null ? form.hiddenAbility : species.hiddenAbility;
        if (natural.size > 0 && ability.equals(natural.get(0))) {
            return 0;
        }
        if (natural.size > 1 && ability.equals(natural.get(1))) {
            return 1;
        }
        return hidden != null && ability.equals(hidden) ? 2 : -1;
    }

    /** {@code ability} (197:224-245) for a slot: hidden when the species has one, else the personality's natural slot. */
    private String abilityForSlot(int slot) {
        Array<String> natural = form != null && form.abilities != null ? form.abilities : species.abilities;
        String hidden = form != null && form.hiddenAbility != null ? form.hiddenAbility : species.hiddenAbility;
        int index = slot;
        if (index >= 2) {
            if (hidden != null && !hidden.isEmpty()) {
                return hidden;                                              // :228-234
            }
            index = personalID & 1;                                         // :235
        }
        String result = index < natural.size ? natural.get(index) : null;   // :238-243
        if (result == null || result.isEmpty()) {
            int other = (index + 1) % 2;
            result = other < natural.size ? natural.get(other) : null;
        }
        return result == null ? ability : result;
    }

    /** Applies a form index; index 0 is the base species (form = null). */
    public void setForm(PbsData data, int index) {
        int slot = species == null ? -1 : abilitySlot();
        form = index <= 0 || data == null || species == null
                ? null : data.form(species.internalName, index);
        looseForm = form == null && index > 0 ? index : 0;                      // the number survives without a data entry
        if (species != null) {
            internalName = form != null ? form.key : species.internalName;
        }
        // 198_Pokemon_Forms:17-24 setForm does not touch the ability; it follows the new form's table by slot.
        if (slot >= 0) {
            ability = abilityForSlot(slot);
        } else if (ability == null && species != null && species.abilities.size > 0) {
            ability = species.abilities.get(0);
        }
    }
}
