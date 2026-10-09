package pokemon.runtime.pokemon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

/**
 * 181_PField_DayCare: the two Pokemon left at the Day Care ({@code $PokemonGlobal.daycare}), the egg they make
 * ({@code daycareEgg}, {@code daycareEggSteps}), and everything that happens every step. The conversations are events; the
 * blocking script parts are the IR commands the interpreter answers with the methods here.
 *
 * <p>登记: {@code language} (the Masuda method's {@code father.language!=mother.language}) is not modelled, so only the Shiny
 * Charm gives extra shiny rolls; the compatibility and egg moves of a form read the species' lists (the PBS forms carry
 * neither); {@code pbGetSpeciesEggMoves(species, form)} is the species' egg moves; shadow Pokemon are not modelled.</p>
 */
public final class DayCare {
    /** 000_Settings:43. */
    public static final int EGG_LEVEL = 1;
    /** 000_Settings:45 {@code POKERUS_CHANCE} (out of 65536). */
    private static final int POKERUS_CHANCE = 3;

    /** {@code $PokemonGlobal.daycare[i][0]}. */
    public final Pokemon[] pokemon = new Pokemon[2];
    /** {@code $PokemonGlobal.daycare[i][1]}: the level when it was left. */
    public final int[] level = new int[2];
    /** {@code $PokemonGlobal.daycareEgg}: 1 when an egg waits. */
    public int egg;
    /** {@code $PokemonGlobal.daycareEggSteps}. */
    public int eggSteps;

    public void reset() {
        Arrays.fill(pokemon, null);
        Arrays.fill(level, 0);
        egg = 0;
        eggSteps = 0;
    }

    // =====================================================================
    // Query information about Pokemon in the Day Care (:1-47)
    // =====================================================================

    /** {@code pbDayCareDeposited} (:5-11). */
    public int deposited() {
        int ret = 0;
        for (int i = 0; i < 2; i++) {
            if (pokemon[i] != null) ret++;
        }
        return ret;
    }

    /** Ruby's array index: {@code -1} is the last entry. */
    private static int slot(int index) {
        return Math.floorMod(index, 2);
    }

    /** {@code pbDayCareGetCost(index)} (:31-38). */
    public int cost(int index) {
        Pokemon pkmn = pokemon[slot(index)];
        if (pkmn == null) return 0;
        int cost = pkmn.level - level[slot(index)] + 1;
        return cost * 100;
    }

    /** The Pokemon at {@code index} ({@code nil} when none). */
    public Pokemon get(int index) {
        return pokemon[slot(index)];
    }

    /** {@code pbEggGenerated?} (:40-43). */
    public boolean eggGenerated() {
        return deposited() == 2 && egg == 1;
    }

    // =====================================================================
    // Manipulate Pokemon in the Day Care (:50-104)
    // =====================================================================

    /** {@code pbDayCareDeposit(index)} (:50-63). */
    public void deposit(TrainerState trainer, int index) {
        for (int i = 0; i < 2; i++) {
            if (pokemon[i] != null) continue;
            Pokemon pkmn = trainer.party.get(index);
            pokemon[i] = pkmn;
            level[i] = pkmn.level;
            heal(pkmn);                                                   // :62 heal
            trainer.party.members().removeIndex(index);                  // :63-64 party[index]=nil; compact!
            egg = 0;
            eggSteps = 0;
            return;
        }
        throw new IllegalStateException("No room to deposit a Pokémon");
    }

    private static void heal(Pokemon pkmn) {
        if (pkmn.egg) return;
        pkmn.hp = pkmn.maxHp();
        pkmn.status = "";
        pkmn.statusCount = 0;
        for (Pokemon.MoveSlot move : pkmn.moves) move.pp = move.maxPp;
    }

    /** {@code pbDayCareWithdraw(index)} (:65-76). */
    public void withdraw(TrainerState trainer, int index) {
        int i = slot(index);
        if (pokemon[i] == null) {
            throw new IllegalStateException("There's no Pokémon here...");
        } else if (trainer.party.size() >= 6) {
            throw new IllegalStateException("Can't store the Pokémon...");
        } else {
            trainer.party.add(pokemon[i]);
            pokemon[i] = null;
            level[i] = 0;
            egg = 0;
        }
    }

    /** The choice list of {@code pbDayCareChoose} (:78-104). */
    public List<String> choices() {
        List<String> choices = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Pokemon p = pokemon[i];
            int gender = p.effectiveGender();
            if (gender == PokemonStats.MALE) {
                choices.add(p.name + " (♂, Lv." + p.level + ")");
            } else if (gender == PokemonStats.FEMALE) {
                choices.add(p.name + " (♀, Lv." + p.level + ")");
            } else {
                choices.add(p.name + " (Lv." + p.level + ")");
            }
        }
        choices.add("取消");
        return choices;
    }

    // =====================================================================
    // Check compatibility of Pokemon in the Day Care (:107-164)
    // =====================================================================

    /** {@code pbIsDitto?(pkmn)} (:107-113). */
    public static boolean isDitto(Pokemon pkmn) {
        return pkmn.species != null && pkmn.species.compatibility.contains("Ditto", false);
    }

    /** {@code pbDayCareCompatibleGender(pkmn1, pkmn2)} (:115-123). */
    public static boolean compatibleGender(Pokemon pkmn1, Pokemon pkmn2) {
        boolean f1 = pkmn1.effectiveGender() == PokemonStats.FEMALE, m1 = pkmn1.effectiveGender() == PokemonStats.MALE;
        boolean f2 = pkmn2.effectiveGender() == PokemonStats.FEMALE, m2 = pkmn2.effectiveGender() == PokemonStats.MALE;
        if (f1 && m2) return true;
        if (m1 && f2) return true;
        boolean ditto1 = isDitto(pkmn1);
        boolean ditto2 = isDitto(pkmn2);
        if (ditto1 && !ditto2) return true;
        return ditto2 && !ditto1;
    }

    /** {@code compat[0] || 0}, {@code compat[1] || compat[0]}: an empty list is the group 0. */
    private static String[] groups(Pokemon pkmn) {
        String first = pkmn.species == null || pkmn.species.compatibility.size < 1 ? "0" : pkmn.species.compatibility.get(0);
        String second = pkmn.species == null || pkmn.species.compatibility.size < 2 ? first : pkmn.species.compatibility.get(1);
        return new String[] {first, second};
    }

    /** {@code pbDayCareGetCompat} (:125-164): 0 incompatible, 1-3 how well they get along. */
    public int compat() {
        if (deposited() != 2) return 0;
        Pokemon pkmn1 = pokemon[0];
        Pokemon pkmn2 = pokemon[1];
        String[] g1 = groups(pkmn1);
        String[] g2 = groups(pkmn2);
        String c10 = g1[0], c11 = g1[1], c20 = g2[0], c21 = g2[1];
        if ("Undiscovered".equals(c10) || "Undiscovered".equals(c11) || "Undiscovered".equals(c20)
                || "Undiscovered".equals(c21)) {
            return 0;
        }
        if (c10.equals(c20) || c11.equals(c20) || c10.equals(c21) || c11.equals(c21)
                || "Ditto".equals(c10) || "Ditto".equals(c11) || "Ditto".equals(c20) || "Ditto".equals(c21)) {
            if (compatibleGender(pkmn1, pkmn2)) {
                int ret = 1;
                if (pkmn1.species == pkmn2.species) ret++;
                if (pkmn1.trainerID != pkmn2.trainerID) ret++;
                return ret;
            }
        }
        return 0;
    }

    // =====================================================================
    // Generate an Egg based on Pokemon in the Day Care (:175-534)
    // =====================================================================

    /** What the generator reads off the world. */
    public interface World {
        /** {@code pbGetCurrentRegion}. */
        int region();

        /** {@code $PokemonBag.pbHasItem?(item)}. */
        boolean hasItem(String item);

        int mapId();
    }

    private static boolean has(Pokemon p, String item) {
        return p.item != null && p.item.equalsIgnoreCase(item);
    }

    private static boolean knows(Pokemon p, String move) {
        for (Pokemon.MoveSlot slot : p.moves) {
            if (slot != null && slot.move != null && move.equals(slot.move.internalName)) return true;
        }
        return false;
    }

    private static final String[] ALOLA_GALAR_HISUI_BASES = {
        "RATTATA", "SANDSHREW", "VULPIX", "DIGLETT", "MEOWTH", "GEODUDE", "GRIMER",
        "PONYTA", "SLOWPOKE", "FARFETCHD", "MRMIME", "CORSOLA", "ZIGZAGOON", "DARUMAKA", "YAMASK", "STUNFISK",
        "GROWLITHE", "VOLTORB", "QWILFISH", "SNEASEL", "ZORUA", "SLIGGOO", "ROWLET", "OSHAWOTT", "CYNDAQUIL", "MUNCHLAX",
        "WOOPER", "SNIVY", "HOOTHOOT", "MURKROW", "CHIKORITA", "TORCHIC", "POPPLIO",
    };

    private static boolean in(String name, String... list) {
        for (String s : list) {
            if (s.equals(name)) return true;
        }
        return false;
    }

    /**
     * {@code pbGetBabySpecies(species, item1, item2)} (201_Pokemon_Evolution:159-175) with the incense check.
     */
    private PbsData.Species baby(PbsData pbs, PbsData.Species species, Pokemon mother, Pokemon father) {
        return PBEvolution.babySpecies(pbs, species, mother.item, father.item);
    }

    /**
     * {@code pbDayCareGenerateEgg} (:175-534): the egg joins the party.
     *
     * @return the egg, or null when nothing was generated
     */
    public Pokemon generateEgg(TrainerState trainer, PbsData pbs, Random rand, World world) {
        if (deposited() != 2) return null;                                              // :188
        if (trainer.party.size() >= 6) throw new IllegalStateException("Can't store the egg");   // :189
        Pokemon pokemon0 = pokemon[0];
        Pokemon pokemon1 = pokemon[1];
        Pokemon mother;
        Pokemon father;
        PbsData.Species babyspecies;
        boolean ditto0 = isDitto(pokemon0);
        boolean ditto1 = isDitto(pokemon1);
        if (pokemon0.effectiveGender() == PokemonStats.FEMALE || ditto0) {              // :198
            babyspecies = ditto0 ? pokemon1.species : pokemon0.species;
            mother = pokemon0;
            father = pokemon1;
        } else {
            babyspecies = ditto1 ? pokemon0.species : pokemon1.species;
            mother = pokemon1;
            father = pokemon0;
        }
        // Determine the egg's species (:194-225)
        babyspecies = baby(pbs, babyspecies, mother, father);
        String name = babyspecies.internalName;
        String replace = null;
        if ("MANAPHY".equals(name) && pbs.species("PHIONE") != null) {
            replace = "PHIONE";
        } else if (("NIDORANfE".equals(name) && pbs.species("NIDORANmA") != null)
                || ("NIDORANmA".equals(name) && pbs.species("NIDORANfE") != null)) {
            replace = new String[] {"NIDORANmA", "NIDORANfE"}[rand.nextInt(2)];
        } else if (("VOLBEAT".equals(name) && pbs.species("ILLUMISE") != null)
                || ("ILLUMISE".equals(name) && pbs.species("VOLBEAT") != null)) {
            replace = new String[] {"VOLBEAT", "ILLUMISE"}[rand.nextInt(2)];
        } else if ("SNORLAX".equals(name)) {
            replace = "MUNCHLAX";
        } else if ("SNEASLER".equals(name)) {
            replace = "SNEASEL";
        } else if ("OVERQWIL".equals(name)) {
            replace = "QWILFISH";
        } else if ("CLODSIRE".equals(name)) {
            replace = "WOOPER";
        } else if ("SIRFETCHD".equals(name)) {
            replace = "FARFETCHD";
        } else if ("OBSTAGOON".equals(name)) {
            replace = "ZIGZAGOON";
        } else if ("PERRSERKER".equals(name)) {
            replace = "MEOWTH";
        } else if ("CURSOLA".equals(name)) {
            replace = "CORSOLA";
        } else if ("MRRIME".equals(name)) {
            replace = "MRMIME";
        } else if ("RUNERIGUS".equals(name)) {
            replace = "YAMASK";
        }
        if (replace != null && pbs.species(replace) != null) {
            babyspecies = pbs.species(replace);
            name = replace;
        }
        // Generate egg (:227-231)
        Pokemon egg = new Pokemon(babyspecies, EGG_LEVEL, pbs);                         // :228 pbNewPkmn
        egg.setTrainerID(trainer.id);
        egg.originalTrainer = trainer.name;
        egg.otGender = trainer.gender;
        egg.obtainMap = world.mapId();
        egg.obtainLevel = EGG_LEVEL;
        egg.personalID = (rand.nextInt(65536)) | (rand.nextInt(65536) << 16);          // :230-232
        egg.ivs = new int[6];
        egg.nature = WildGenerator.natureOf(pbs, egg);
        // Inheriting form (:233-244)
        if (in(name, "BURMY", "SHELLOS", "BASCULIN", "FLABEBE", "PUMPKABOO", "ROCKRUFF", "MINIOR")) {
            int newForm = mother.formIndex();
            if (mother.isSpecies("MOTHIM")) newForm = 0;
            egg.setForm(pbs, newForm);
        }
        // Inheriting Regional Forms (:246-290)
        boolean regional = in(name, ALOLA_GALAR_HISUI_BASES);
        boolean fatherLine = baby(pbs, father.species, mother, father) == babyspecies;
        if (regional) {
            if (mother.formIndex() == 1) {
                if (has(mother, "EVERSTONE")) egg.setForm(pbs, 1);
            } else if (fatherLine) {
                if (father.formIndex() == 1 && has(father, "EVERSTONE")) egg.setForm(pbs, 1);
            }
            if (mother.formIndex() == 2) {
                if (has(mother, "EVERSTONE")) egg.setForm(pbs, 2);
            } else if (fatherLine) {
                if (father.formIndex() == 2 && has(father, "EVERSTONE")) egg.setForm(pbs, 2);
            }
        }
        // 原种御三家 (:319-334)
        if (in(name, "MEGANIUM", "BLAZIKEN", "PRIMARINA")) {
            int region = world.region();
            if (has(mother, "EVERSTONE")) {
                egg.setForm(pbs, mother.formIndex());
            } else if (fatherLine && has(father, "EVERSTONE")) {
                egg.setForm(pbs, father.formIndex());
            } else if (region != 0) {
                egg.setForm(pbs, 2);
            } else {
                egg.setForm(pbs, 0);
            }
        }
        // 雪人卡比 (:336-348)
        if ("SNORLAX".equals(name)) {
            int region = world.region();
            if (has(mother, "EVERSTONE")) {
                egg.setForm(pbs, mother.formIndex());
            } else if (fatherLine && has(father, "EVERSTONE")) {
                egg.setForm(pbs, father.formIndex());
            } else if (region != 0) {
                egg.setForm(pbs, 0);
            } else {
                egg.setForm(pbs, 1);
            }
        }
        // :321-358 `a || b && c`: the evolved-form parents
        if (mother.isSpecies("SNEASLER") || father.isSpecies("SNEASLER") && fatherLine) egg.setForm(pbs, 1);
        if (mother.isSpecies("OBSTAGOON") || father.isSpecies("OBSTAGOON") && fatherLine) egg.setForm(pbs, 1);
        if (mother.isSpecies("OVERQWIL") || father.isSpecies("OVERQWIL") && fatherLine) egg.setForm(pbs, 1);
        if (mother.isSpecies("CLODSIRE") || father.isSpecies("CLODSIRE") && fatherLine) egg.setForm(pbs, 1);
        if (mother.isSpecies("PERRSERKER") || father.isSpecies("PERRSERKER") && fatherLine) egg.setForm(pbs, 2);
        if (mother.isSpecies("CURSOLA") || father.isSpecies("CURSOLA") && fatherLine) egg.setForm(pbs, 1);
        if (mother.isSpecies("MRRIME") || father.isSpecies("MRRIME") && fatherLine) egg.setForm(pbs, 1);
        if (mother.isSpecies("RUNERIGUS") || father.isSpecies("RUNERIGUS") && fatherLine) egg.setForm(pbs, 1);
        // Inheriting Moves (:360-424)
        List<String> moves = new ArrayList<>();
        List<String> othermoves = new ArrayList<>();
        Pokemon movefather = father;
        Pokemon movemother = mother;
        if (isDitto(movefather) && mother.effectiveGender() != PokemonStats.FEMALE) {
            movefather = mother;
            movemother = father;
        }
        for (PbsData.LearnMove k : egg.getMoveList(pbs)) {                              // :367-376
            if (k.level <= EGG_LEVEL) {
                moves.add(k.move);
            } else {
                if (!knows(mother, k.move) || !knows(father, k.move)) continue;
                othermoves.add(k.move);
            }
        }
        moves.addAll(othermoves);                                                       // :377-380
        // :381-392 Inheriting Machine Moves: NEWEST_BATTLE_MECHANICS is true, none.
        List<String> babyEggMoves = new ArrayList<>();                                  // :393
        for (String m : egg.species.eggMoves) babyEggMoves.add(m);
        if (movefather.effectiveGender() == PokemonStats.MALE) {                        // :394-396
            for (String m : babyEggMoves) if (knows(movefather, m)) moves.add(m);
        }
        for (String m : babyEggMoves) if (knows(movemother, m)) moves.add(m);           // :397-399 NEWEST_BATTLE_MECHANICS
        boolean lightball = false;                                                      // :401-408
        if ((father.isSpecies("PIKACHU") || father.isSpecies("RAICHU")) && has(father, "LIGHTBALL")) lightball = true;
        if ((mother.isSpecies("PIKACHU") || mother.isSpecies("RAICHU")) && has(mother, "LIGHTBALL")) lightball = true;
        if (lightball && "PICHU".equals(name) && pbs.move("VOLTTACKLE") != null) {
            moves.add("VOLTTACKLE");
        }
        java.util.Collections.reverse(moves);                                           // :409-414
        moves = new ArrayList<>(new LinkedHashSet<>(moves));
        java.util.Collections.reverse(moves);
        List<PbsData.Move> finalmoves = new ArrayList<>();                              // :418-424
        int listend = Math.max(0, moves.size() - 4);
        for (int i = listend; i < listend + 4; i++) {
            if (i < moves.size() && pbs.move(moves.get(i)) != null) finalmoves.add(pbs.move(moves.get(i)));
        }
        // Inheriting Individual Values (:426-462)
        int[] ivs = new int[6];
        for (int i = 0; i < 6; i++) ivs[i] = rand.nextInt(32);
        Integer[] ivinherit = new Integer[2];
        Pokemon[] parents = {mother, father};
        for (int i = 0; i < 2; i++) {
            Pokemon parent = parents[i];
            if (has(parent, "POWERWEIGHT")) ivinherit[i] = 0;
            if (has(parent, "POWERBRACER")) ivinherit[i] = 1;
            if (has(parent, "POWERBELT")) ivinherit[i] = 2;
            if (has(parent, "POWERLENS")) ivinherit[i] = 4;
            if (has(parent, "POWERBAND")) ivinherit[i] = 5;
            if (has(parent, "POWERANKLET")) ivinherit[i] = 3;
        }
        int num = 0;
        int r = rand.nextInt(2);
        for (int times = 0; times < 2; times++) {                                       // :440-446
            if (ivinherit[r] != null) {
                Pokemon parent = parents[r];
                ivs[ivinherit[r]] = parent.ivs[ivinherit[r]];
                num++;
                break;
            }
            r = (r + 1) % 2;
        }
        int limit = (has(mother, "DESTINYKNOT") || has(father, "DESTINYKNOT")) ? 5 : 3;   // :455-456
        List<Integer> inherited = new ArrayList<>();
        for (Integer i : ivinherit) inherited.add(i);
        while (true) {                                                                  // :457-463
            List<Integer> freestats = new ArrayList<>();
            for (int s = 0; s < 6; s++) {
                if (!inherited.contains(s)) freestats.add(s);
            }
            if (freestats.isEmpty()) break;
            int stat = freestats.get(rand.nextInt(freestats.size()));
            Pokemon parent = parents[rand.nextInt(2)];
            ivs[stat] = parent.ivs[stat];
            inherited.add(stat);
            num++;
            if (num >= limit) break;
        }
        // Inheriting nature (:464-469)
        List<PbsData.Nature> newnatures = new ArrayList<>();
        if (has(mother, "EVERSTONE")) newnatures.add(mother.nature);
        if (has(father, "EVERSTONE")) newnatures.add(father.nature);
        if (newnatures.size() > 0) {
            egg.nature = newnatures.get(rand.nextInt(newnatures.size()));
        }
        // Masuda method and Shiny Charm (:471-484); the languages are not modelled
        int shinyretries = 0;
        if (world.hasItem("SHINYCHARM")) shinyretries += 2;
        egg.shiny = Pokemon.isShiny(egg.personalID, egg.trainerID);
        if (shinyretries > 0) {
            for (int i = 0; i < shinyretries; i++) {
                if (egg.shiny) {
                    if (!egg.superShiny && rand.nextInt(16) == 1) egg.superShiny = true;
                    break;
                }
                egg.personalID = rand.nextInt(65536) | (rand.nextInt(65536) << 16);
                egg.shiny = Pokemon.isShiny(egg.personalID, egg.trainerID);
            }
        }
        if (egg.nature == null || newnatures.isEmpty()) egg.nature = WildGenerator.natureOf(pbs, egg);   // @natureflag || personalID%25
        egg.setAbilitySlot(egg.personalID & 1);                                         // ability derives from the personal id
        // Inheriting ability from the mother (:486-501)
        if (!ditto0 && !ditto1) {
            if (mother.abilityIndex() >= 2) {
                if (rand.nextInt(10) < 6) egg.setAbilitySlot(mother.abilityIndex());
            } else {
                if (rand.nextInt(10) < 8) {
                    egg.setAbilitySlot(mother.abilityIndex());
                } else {
                    egg.setAbilitySlot((mother.abilityIndex() + 1) % 2);
                }
            }
        } else if (!(ditto0 && ditto1)) {                                               // NEWEST_BATTLE_MECHANICS
            Pokemon parent = !ditto0 ? mother : father;
            if (parent.abilityIndex() >= 2) {
                if (rand.nextInt(10) < 6) egg.setAbilitySlot(parent.abilityIndex());
            }
        }
        // Inheriting Poke Ball from the mother (:503-507)
        if (mother.effectiveGender() == PokemonStats.FEMALE && mother.ballused != BallIds.MASTERBALL
                && mother.ballused != BallIds.CHERISHBALL) {
            egg.ballused = mother.ballused;
        }
        // Set all stats (:509-526)
        egg.happiness = 120;
        egg.ivs = ivs;
        egg.moves.clear();
        for (PbsData.Move move : finalmoves) egg.moves.add(new Pokemon.MoveSlot(move));
        egg.hp = egg.maxHp();                                                           // calcStats
        egg.obtainText = "抚养夫妇";
        egg.name = "宝可梦蛋";
        egg.egg = true;
        egg.stepsToHatch = babyspecies.stepsToHatch;                                    // :523-524
        if (rand.nextInt(65536) < POKERUS_CHANCE) {                                     // :526
            int strain = 1 + rand.nextInt(15);
            egg.pokerus = (1 + (strain % 4)) | (strain << 4);
        }
        trainer.party.add(egg);                                                         // :528
        return egg;
    }

    /** {@code pbBallTypeToItem} ids of the two balls the egg never inherits. */
    private static final class BallIds {
        static final int MASTERBALL = 4;
        static final int CHERISHBALL = 15;
    }

    // =====================================================================
    // Code that happens every step the player takes (:536-570)
    // =====================================================================

    /**
     * Makes an egg available, and lets the Pokemon in the Day Care gain experience and moves.
     *
     * @param levelLock {@code $game_switches[199]}
     * @param badge     {@code $Trainer.badges[j]}
     */
    public void onStep(PbsData pbs, Random rand, boolean ovalCharm, boolean levelLock, Predicate<Integer> badge) {
        int deposited = deposited();
        if (deposited == 2 && egg == 0) {                                               // :537-547
            eggSteps++;
            if (eggSteps == 256) {
                eggSteps = 0;
                int[] normal = {0, 20, 50, 70};
                int[] charm = {0, 40, 80, 88};
                int compatval = (ovalCharm ? charm : normal)[compat()];
                if (rand.nextInt(100) < compatval) egg = 1;                             // Egg is generated
            }
        }
        for (int i = 0; i < 2; i++) {                                                   // :552-575
            Pokemon pkmn = pokemon[i];
            if (pkmn == null) continue;
            int maxexp = PBExperience.pbGetMaxExperience(pkmn.growthRate());
            if (levelLock) {
                int index = 8;
                for (int j = 0; j < 8; j++) {
                    if (!badge.test(j)) {
                        index = j;
                        break;
                    }
                }
                maxexp = PBExperience.pbGetExpInternal(PBExperience.maxLevelAt(index), pkmn.growthRate());
            }
            if (pkmn.exp >= maxexp) continue;
            int oldlevel = pkmn.level;
            pkmn.gainExperience(1);                                                     // :566 pkmn.exp += 1
            if (pkmn.level == oldlevel) continue;
            for (PbsData.LearnMove m : pkmn.getMoveList(pbs)) {                         // :570-573
                if (m.level == pkmn.level && pbs.move(m.move) != null) pkmn.learnMoveSilently(pbs.move(m.move));
            }
        }
    }
}
