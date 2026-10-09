package pokemon.runtime.field;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.TrainerState;
import pokemon.runtime.state.FieldGlobals;
import pokemon.runtime.state.GameState;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

/**
 * {@code PokemonEncounters} and {@code pbEncounter} (175_PField_Encounters, 482 行), transcribed method by method:
 * the encounter types and their weights, the step probability with its item / ability / cycling modifiers, the
 * lead Pokemon's ability effects on the species and the level, the flutes and the repel rule.
 *
 * <p>Encounter types are the PBS method names ({@code EncounterTypes::Names}, :17-31).</p>
 *
 * <p>登记: ①{@code pbInBugContest?} is false (the Bug Catching Contest is not part of this game);
 * ②{@code EncounterModifier.trigger} procs (Roaming Pokemon / Poke Radar) are not registered;
 * ③{@code $game_system.encounter_disabled} has no switch yet (always false); ④the {@code $DEBUG} CTRL skip.</p>
 */
public final class PokemonEncounters {
    /** 000_Settings:42 MAXIMUM_LEVEL (PBExperience.maxLevel, 087_PBExperience:94-96). */
    private static final int MAXIMUM_LEVEL = 210;
    /** 169_PBTerrain: Ice = 12. */
    private static final int TERRAIN_ICE = 12;

    /** :4-15 EncounterTypes. */
    public static final String LAND = "Land";
    public static final String CAVE = "Cave";
    public static final String WATER = "Water";
    public static final String LAND_MORNING = "LandMorning";
    public static final String LAND_DAY = "LandDay";
    public static final String LAND_NIGHT = "LandNight";
    public static final String BUG_CONTEST = "BugContest";

    /** :34-48 EnctypeChances: the weight of every row of the table, per encounter type. */
    private static final Map<String, int[]> CHANCES = new HashMap<>();

    static {
        int[] land = {20, 20, 10, 10, 10, 10, 5, 5, 4, 4, 1, 1};
        int[] water = {60, 30, 5, 4, 1};
        int[] headbutt = {30, 25, 20, 10, 5, 5, 4, 1};
        CHANCES.put("Land", land);
        CHANCES.put("Cave", land);
        CHANCES.put("Water", water);
        CHANCES.put("RockSmash", water);
        CHANCES.put("OldRod", new int[] {70, 30});
        CHANCES.put("GoodRod", new int[] {60, 20, 20});
        CHANCES.put("SuperRod", new int[] {40, 40, 15, 4, 1});
        CHANCES.put("HeadbuttLow", headbutt);
        CHANCES.put("HeadbuttHigh", headbutt);
        CHANCES.put("LandMorning", land);
        CHANCES.put("LandDay", land);
        CHANCES.put("LandNight", land);
        CHANCES.put("BugContest", land);
    }

    /** {@code [species, level]}: what a roll answers. */
    public static final class Encounter {
        public final String species;
        public final int level;

        public Encounter(String species, int level) {
            this.species = species;
            this.level = level;
        }
    }

    private final PbsData pbs;
    private final GameState state;
    private final Random random;
    private final Supplier<LocalTime> clock;

    private PbsData.EncounterMap map;        // @density / @enctypes of the current map; null = none
    private int stepcount;                   // @stepcount

    public PokemonEncounters(PbsData pbs, GameState state, Random random, Supplier<LocalTime> clock) {
        this.pbs = pbs;
        this.state = state;
        this.random = random;
        this.clock = clock;
    }

    private TrainerState trainer() {
        return state.trainer();
    }

    private FieldGlobals globals() {
        return state.fieldGlobals();
    }

    /** :23-40 setup(mapID): the tables of the map the player just entered. */
    public void setup(int mapId) {
        stepcount = 0;
        map = pbs == null ? null : pbs.encounterMap(mapId);
    }

    /** :42 clearStepCount */
    public void clearStepCount() {
        stepcount = 0;
    }

    public int stepcount() {
        return stepcount;
    }

    private Array<PbsData.EncounterEntry> table(String method) {
        return map == null ? null : map.method(method);
    }

    /** :46-49 hasEncounter?(enc): a table is defined for the type on the current map. */
    public boolean hasEncounter(String enc) {
        return map != null && table(enc) != null;
    }

    /** :73-77 isCave? */
    public boolean isCave() {
        return hasEncounter(CAVE);
    }

    /** :81-90 isGrass?: any land-like table, the Bug Contest one included. */
    public boolean isGrass() {
        return hasEncounter(LAND) || hasEncounter(LAND_MORNING) || hasEncounter(LAND_DAY)
                || hasEncounter(LAND_NIGHT) || hasEncounter(BUG_CONTEST);
    }

    /** :95-103 isRegularGrass? */
    public boolean isRegularGrass() {
        return hasEncounter(LAND) || hasEncounter(LAND_MORNING) || hasEncounter(LAND_DAY)
                || hasEncounter(LAND_NIGHT);
    }

    /** :108-111 isWater? */
    public boolean isWater() {
        return hasEncounter(WATER);
    }

    /**
     * :117-129 isEncounterPossibleHere?: surfing always, never on ice, a cave anywhere, grass only on a grass tile.
     *
     * @param terrainTag {@code $game_map.terrain_tag(player.x, player.y)} (PBTerrain)
     */
    public boolean isEncounterPossibleHere(int terrainTag) {
        if (globals().surfing) {
            return true;
        } else if (terrainTag == TERRAIN_ICE) {
            return false;
        } else if (isCave()) {
            return true;
        } else if (isGrass()) {
            return isGrassTag(terrainTag);
        }
        return false;
    }

    /** 169_PBTerrain:56-61 isGrass?: Grass 2, TallGrass 10, UnderwaterGrass 11, SootGrass 14. */
    public static boolean isGrassTag(int tag) {
        return tag == 2 || tag == 10 || tag == 11 || tag == 14;
    }

    /**
     * :135-155 pbEncounterType: the method the current location rolls on, or {@code null} for none (the plugin's -1).
     */
    public String pbEncounterType() {
        if (globals().surfing) {
            return WATER;
        } else if (isCave()) {
            return CAVE;
        } else if (isGrass()) {
            LocalTime time = clock.get();
            String enctype = LAND;
            if (hasEncounter(LAND_NIGHT) && PBDayNight.isNight(time)) {
                enctype = LAND_NIGHT;
            }
            if (hasEncounter(LAND_DAY) && PBDayNight.isDay(time)) {
                enctype = LAND_DAY;
            }
            if (hasEncounter(LAND_MORNING) && PBDayNight.isMorning(time)) {
                enctype = LAND_MORNING;
            }
            // :151 pbInBugContest? is false here (see the class comment).
            return enctype;
        }
        return null;
    }

    /**
     * :201-296 pbEncounteredPokemon(enctype, tries=1): species and level of the wild Pokemon, or {@code null}.
     */
    public Encounter pbEncounteredPokemon(String enctype, int tries) {
        int[] allChances = CHANCES.get(enctype);
        if (allChances == null) {
            throw new IllegalArgumentException("Encounter type out of range");   // :202-204
        }
        Array<PbsData.EncounterEntry> source = table(enctype);                   // :206
        if (source == null) {
            return null;
        }
        List<PbsData.EncounterEntry> encList = new ArrayList<>();
        for (PbsData.EncounterEntry entry : source) {
            encList.add(entry);
        }
        int[] chances = allChances.clone();                                     // :207
        Pokemon firstPkmn = trainer().party.first();                            // :212
        // Static / Magnet Pull ... prefer wild Pokemon of a type (:210-248).
        if (firstPkmn != null && random.nextInt(100) < 50) {                     // :213
            String favoredType = favoredType(firstPkmn.ability);
            if (favoredType != null) {                                          // :230
                List<PbsData.EncounterEntry> newEncList = new ArrayList<>();
                List<Integer> newChances = new ArrayList<>();
                for (int i = 0; i < encList.size(); i++) {                      // :236
                    PbsData.Species species = pbs.species(encList.get(i).species);
                    String t1 = species == null ? null : species.type(0);
                    String t2 = species == null ? null : species.type(1);
                    if (!favoredType.equals(t1) && (t2 == null || !favoredType.equals(t2))) {   // :239
                        continue;
                    }
                    newEncList.add(encList.get(i));                             // :240
                    newChances.add(i < chances.length ? chances[i] : 0);        // :241 chances[i] (position in the full table)
                }
                if (newEncList.size() > 0) {                                    // :244
                    encList = newEncList;
                    chances = new int[newChances.size()];
                    for (int i = 0; i < chances.length; i++) {
                        chances[i] = newChances.get(i);
                    }
                }
            }
        }
        // 343_ChainCatching:171-190: the chain raises the odds of the chained species (up to 60%).
        int chainRate = Math.min(60, trainer().chainCatching.chainTimes);
        if (random.nextInt(100) < chainRate) {
            List<PbsData.EncounterEntry> newEncList = new ArrayList<>();
            List<Integer> newChances = new ArrayList<>();
            for (int i = 0; i < encList.size(); i++) {
                if (!encList.get(i).species.equals(trainer().chainCatching.species)) {
                    continue;
                }
                newEncList.add(encList.get(i));
                newChances.add(i < chances.length ? chances[i] : 0);
            }
            if (newEncList.size() > 0) {
                encList = newEncList;
                chances = new int[newChances.size()];
                for (int i = 0; i < chances.length; i++) {
                    chances[i] = newChances.get(i);
                }
            }
        }
        int chanceTotal = 0;                                                    // :249
        for (int chance : chances) {
            chanceTotal += chance;
        }
        int rnd = 0;                                                            // :256
        for (int t = 0; t < tries; t++) {                                       // :257-260
            int r = random.nextInt(chanceTotal);
            if (rnd < r) {
                rnd = r;
            }
        }
        int chance = 0;                                                         // :261
        int chosenPkmn = 0;
        for (int i = 0; i < chances.length; i++) {                              // :263
            chance += chances[i];
            if (rnd < chance) {
                chosenPkmn = i;
                break;
            }
        }
        PbsData.EncounterEntry encounter = chosenPkmn < encList.size() ? encList.get(chosenPkmn) : null;   // :269
        if (encounter == null) {
            return null;
        }
        int level = encounter.minLevel + random.nextInt(1 + encounter.maxLevel - encounter.minLevel);   // :271
        // Some abilities raise the level of the wild Pokemon (:273-283).
        if (firstPkmn != null && random.nextInt(100) < 50) {
            String ability = firstPkmn.ability;
            if ("HUSTLE".equals(ability) || "VITALSPIRIT".equals(ability) || "PRESSURE".equals(ability)
                    || "CALAMITYABYSSAL".equals(ability) || "CALAMITYINFERNAL".equals(ability)) {
                int level2 = encounter.minLevel + random.nextInt(1 + encounter.maxLevel - encounter.minLevel);
                if (level2 > level) {
                    level = level2;
                }
            }
        }
        // Black / White Flute (:285-292, NEWEST_BATTLE_MECHANICS).
        if (globals().blackFluteUsed) {
            level = Math.min(level + 1 + random.nextInt(3), MAXIMUM_LEVEL);
        } else if (globals().whiteFluteUsed) {
            level = Math.max(level - 1 - random.nextInt(3), 1);
        }
        return new Encounter(encounter.species, level);
    }

    /** :215-228 the lead's ability, and the type it favours. */
    private static String favoredType(String ability) {
        if ("STATIC".equals(ability)) {
            return "ELECTRIC";
        } else if ("MAGNETPULL".equals(ability)) {
            return "STEEL";
        } else if ("FLASHFIRE".equals(ability)) {
            return "FIRE";
        } else if ("HARVEST".equals(ability)) {
            return "GRASS";
        } else if ("LIGHTNINGROD".equals(ability)) {
            return "ELECTRIC";
        } else if ("STORMDRAIN".equals(ability)) {
            return "WATER";
        }
        return null;
    }

    /**
     * :302-376 pbGenerateEncounter(enctype): the roll made on every step. Wild Pokemon cannot appear for the first
     * three steps after the last one; the chance is the table's density times 16 with the cycling, item and ability
     * modifiers, against {@code rand(180*16)}.
     */
    public Encounter pbGenerateEncounter(String enctype) {
        if (!CHANCES.containsKey(enctype)) {
            throw new IllegalArgumentException("Encounter type out of range");   // :303-305
        }
        if (map == null) {
            return null;                                                         // :308
        }
        if (map.density(enctype) == 0) {
            return null;                                                         // :309
        }
        if (table(enctype) == null) {
            return null;                                                         // :310
        }
        stepcount += 1;                                                          // :313
        if (stepcount <= 3) {
            return null;                                                         // :314
        }
        RubyNumber encount = new RubyNumber(map.density(enctype) * 16, true);    // :321
        if (globals().bicycle) {
            encount = encount.times(0.8);                                        // :322
        }
        // (NEWEST_BATTLE_MECHANICS is true: the :323-329 flute block is skipped.)
        Pokemon firstPkmn = trainer().party.first();
        if (firstPkmn != null) {                                                 // :331
            String ability = firstPkmn.ability;
            if ("CLEANSETAG".equals(firstPkmn.item)) {
                encount = encount.times(2).div(3);                               // :333 encount*2/3
            } else if ("PUREINCENSE".equals(firstPkmn.item)) {
                encount = encount.times(2).div(3);                               // :335
            } else {                                                             // ignore ability effects if an item effect applies
                int weather = state.weather().type();
                if ("STENCH".equals(ability) || "WHITESMOKE".equals(ability) || "QUICKFEET".equals(ability)) {
                    encount = encount.div(2);                                    // :338-343
                } else if ("SNOWCLOAK".equals(ability)) {
                    if (weather == 3 || weather == 4) {                          // PBFieldWeather::Snow / Blizzard
                        encount = encount.div(2);                                // :345-347
                    }
                } else if ("SANDVEIL".equals(ability)) {
                    if (weather == 5) {                                          // PBFieldWeather::Sandstorm
                        encount = encount.div(2);                                // :349-351
                    }
                } else if ("SWARM".equals(ability)) {
                    encount = encount.times(1.5);                                // :353
                } else if ("ILLUMINATE".equals(ability) || "ARENATRAP".equals(ability) || "NOGUARD".equals(ability)) {
                    encount = encount.times(2);                                  // :355-361
                }
            }
        }
        if (random.nextInt(180 * 16) >= encount.value) {                         // :365 return nil if rand(180*16)>=encount
            return null;
        }
        Encounter encPkmn = pbEncounteredPokemon(enctype, 1);                    // :367
        if (encPkmn == null) {
            return null;
        }
        // Some abilities make wild encounters less likely if the wild Pokemon is far weaker (:371-378).
        if (firstPkmn != null && random.nextInt(100) < 50) {
            String ability = firstPkmn.ability;
            if ("INTIMIDATE".equals(ability) || "KEENEYE".equals(ability) || "ROSYAEGIS".equals(ability)) {
                if (encPkmn.level <= firstPkmn.level - 5) {
                    return null;                                                 // :376
                }
            }
        }
        return encPkmn;
    }

    /**
     * :384-397 pbCanEncounter?(encounter, repel): not while encounters are disabled or without a roll, and a repel
     * keeps away wild Pokemon weaker than the lead.
     */
    public boolean pbCanEncounter(Encounter encounter, boolean repel) {
        if (encounter == null) {
            return false;                                                        // :386
        }
        // :385 $game_system.encounter_disabled and :387 $DEBUG CTRL: registered, always off.
        // :388 pbPokeRadarOnShakingGrass is false: the Poke Radar is not part of this game.
        if (globals().repel > 0 || repel) {                                      // :390
            Pokemon firstPkmn = trainer().party.first();                         // NEWEST_BATTLE_MECHANICS: firstPokemon
            if (firstPkmn != null && encounter.level < firstPkmn.level) {
                return false;                                                    // :392
            }
        }
        return true;
    }

    /**
     * :484-499 pbEncounter(enctype): used by fishing rods and Headbutt / Rock Smash / Sweet Scent, skipping the
     * probability checks above. Answers the Pokemon met, or null (the battle is the caller's).
     */
    public Encounter pbEncounter(String enctype) {
        return pbEncounteredPokemon(enctype, 1);
    }

    /** An Integer or a Float the way Ruby carries it through {@code *} and {@code /}. */
    private static final class RubyNumber {
        final double value;
        final boolean integer;

        RubyNumber(double value, boolean integer) {
            this.value = value;
            this.integer = integer;
        }

        RubyNumber times(double factor) {
            boolean stays = integer && factor == Math.rint(factor);
            return new RubyNumber(value * factor, stays);
        }

        RubyNumber div(int divisor) {
            return integer ? new RubyNumber(Math.floor(value / divisor), true) : new RubyNumber(value / divisor, false);
        }
    }
}
