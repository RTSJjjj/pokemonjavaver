package pokemon.runtime.pokemon;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectMap;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

/**
 * Stage 3 / P0: the Essentials PBS data of {@code generated/pbs/*.json}
 * (species, forms, moves, items, abilities, the type chart, trainer classes,
 * natures and TM compatibility), produced by the Builder from {@code PBS/*.txt}.
 *
 * <p>Pure data: the battle/party/menu systems build on these tables. Every
 * document is optional, so an old runtime-data folder simply yields empty
 * tables instead of failing the load.</p>
 */
public final class PbsData {

    public boolean loaded;
    public String version;

    public final ObjectMap<String, Species> species = new ObjectMap<>();
    /** Dex id -> internal name (the order of pokemon.txt). */
    public final ObjectMap<String, String> speciesById = new ObjectMap<>();
    public final ObjectMap<String, SpeciesForm> forms = new ObjectMap<>();
    public final ObjectMap<String, Move> moves = new ObjectMap<>();
    public final ObjectMap<String, Item> items = new ObjectMap<>();
    public final ObjectMap<String, Ability> abilities = new ObjectMap<>();
    public final ObjectMap<String, TypeInfo> types = new ObjectMap<>();
    public final ObjectMap<String, TrainerType> trainerTypes = new ObjectMap<>();
    public final Array<Nature> natures = new Array<>();
    public final ObjectMap<String, Nature> naturesByName = new ObjectMap<>();
    /** Move internal name -> species internal names that can learn it (TM/HM). */
    public final ObjectMap<String, Array<String>> tmCompatibility = new ObjectMap<>();
    public final ObjectMap<String, Array<String>> tmBySpecies = new ObjectMap<>();
    /** P2: map id (as text) -> wild encounter table. */
    public final ObjectMap<String, EncounterMap> encounters = new ObjectMap<>();
    /** P2: "TYPE,NAME,version" -> trainer party. */
    public final ObjectMap<String, TrainerData> trainers = new ObjectMap<>();

    private PbsData() {
    }

    // ------------------------------------------------------------------
    // Nested data records (the shape of generated/pbs/*.json)
    // ------------------------------------------------------------------

    public static final class LearnMove {
        public int level;
        public String move;
    }

    public static final class Evolution {
        public String species;
        public String method;
        public String parameter;
    }

    public static final class WildItems {
        public String common;
        public String uncommon;
        public String rare;
    }

    public static final class BattlerOffsets {
        public int playerX;
        public int playerY;
        public int enemyX;
        public int enemyY;
        public int shadowX;
        public int shadowSize;
    }

    public static final class Species {
        public int id;
        public String internalName;
        public String name;
        public Array<String> types = new Array<>();
        /** HP, ATTACK, DEFENSE, SPEED, SPATK, SPDEF. */
        public int[] baseStats = new int[] {0, 0, 0, 0, 0, 0};
        public String genderRate = "Unknown";
        public String growthRate = "Medium";
        public int baseExp;
        public int[] effortPoints = new int[] {0, 0, 0, 0, 0, 0};
        public int rareness;
        public int happiness;
        public Array<String> abilities = new Array<>();
        public String hiddenAbility;
        public Array<LearnMove> moves = new Array<>();
        public Array<String> eggMoves = new Array<>();
        public Array<String> compatibility = new Array<>();
        public int stepsToHatch;
        public float height;
        public float weight;
        public String color;
        public int shape;
        public String habitat;
        public String kind;
        public String pokedex;
        public int[] regionalNumbers = new int[0];
        public Array<Evolution> evolutions = new Array<>();
        public WildItems wildItems = new WildItems();
        public String formName;
        public String incense;
        public BattlerOffsets battler = new BattlerOffsets();

        public String type(int index) {
            return index >= 0 && index < types.size ? types.get(index) : null;
        }

        public int baseStat(int index) {
            return index >= 0 && index < baseStats.length ? baseStats[index] : 0;
        }
    }

    /** An alternate form; absent fields inherit the base species. */
    public static final class SpeciesForm {
        public String species;
        public int form;
        public String key;
        public String formName;
        public int[] baseStats;
        public Array<String> types;
        public Array<String> abilities;
        public String hiddenAbility;
        public Float height;
        public Float weight;
        public String color;
        public String pokedex;
        public String megaStone;
        public Integer unmegaForm;
        public Array<LearnMove> moves;
        public Array<Evolution> evolutions;
        public BattlerOffsets battler = new BattlerOffsets();

        public int baseStat(int index) {
            return baseStats != null && index >= 0 && index < baseStats.length ? baseStats[index] : 0;
        }
    }

    public static final class Move {
        public int id;
        public String internalName;
        public String name;
        public String function;
        public int power;
        public String type;
        public String category;
        public int accuracy;
        public int pp;
        public int effectChance;
        public String target;
        public int priority;
        public String flags = "";
        public String description;
    }

    public static final class Item {
        public int id;
        public String internalName;
        public String name;
        public String namePlural;
        public int pocket;
        public int price;
        public String description;
        public int fieldUse;
        public int battleUse;
        public Array<String> extra = new Array<>();
    }

    public static final class Ability {
        public int id;
        public String internalName;
        public String name;
        public String description;
    }

    public static final class TypeInfo {
        public int id;
        public String internalName;
        public String name;
        public Array<String> weaknesses = new Array<>();
        public Array<String> resistances = new Array<>();
        public Array<String> immunities = new Array<>();
    }

    public static final class TrainerType {
        public int id;
        public String internalName;
        public String name;
        public int baseMoney;
        public String skill;
        public Array<String> fields = new Array<>();
    }

    public static final class Nature {
        public int id;
        public String internalName;
        public String name;
        public String statUp;
        public String statDown;

        public boolean neutral() {
            return statUp == null || statDown == null;
        }
    }

    // ------------------------------------------------------------------
    // P2: wild encounters / trainers
    // ------------------------------------------------------------------

    /** One row of a wild encounter table. */
    public static final class EncounterEntry {
        public String species;
        public int minLevel;
        public int maxLevel;

        public int level(java.util.Random random) {
            return maxLevel <= minLevel ? minLevel : minLevel + random.nextInt(maxLevel - minLevel + 1);
        }

        public boolean contains(String speciesName) {
            return species != null && species.equals(speciesName);
        }
    }

    /** One map's wild encounter table, keyed by method (Land/Cave/Water/...). */
    public static final class EncounterMap {
        public int id;
        public String name = "";
        /** Method name -> density (EnctypeDensities with the map overrides applied). */
        public final ObjectIntMap<String> densities = new ObjectIntMap<>();
        public final ObjectMap<String, Array<EncounterEntry>> methods = new ObjectMap<>();

        public Array<EncounterEntry> method(String name) {
            return methods.get(name, null);
        }

        public int density(String name) {
            return densities.get(name, 0);
        }
    }

    /** One trainer's party member (trainers.txt "Pokemon =" plus its fields). */
    public static final class TrainerPokemon {
        public String species;
        public int level = 1;
        public Array<String> moves = new Array<>();
        public String item;
        public String ability;
        public String nature;
        public String gender;
        public String ball;
        public boolean shiny;
        public boolean superShiny;
        public int form;
        public int happiness = -1;
        public int[] ivs;
        public int[] evs;
    }

    /** One trainers.txt section. */
    public static final class TrainerData {
        public String key;
        public String type;
        public String name;
        public int version;
        public String loseText;
        public final Array<String> items = new Array<>();
        public final Array<TrainerPokemon> party = new Array<>();

        public boolean isVersion(int other) {
            return version == other;
        }
    }

    // ------------------------------------------------------------------
    // Lookups
    // ------------------------------------------------------------------

    public Species species(String internalName) {
        if (internalName == null || internalName.isEmpty()) {
            return null;
        }
        Species direct = species.get(internalName);
        if (direct != null) {
            return direct;
        }
        // Form spelling SPECIES_1 -> its base species (the form itself lives in
        // forms() and overrides what it needs).
        int cut = internalName.lastIndexOf('_');
        if (cut > 0) {
            String base = internalName.substring(0, cut);
            String suffix = internalName.substring(cut + 1);
            if (suffix.chars().allMatch(Character::isDigit)) {
                return species.get(base);
            }
        }
        return null;
    }

    public SpeciesForm form(String key) {
        return key == null ? null : forms.get(key);
    }

    public SpeciesForm form(String speciesName, int form) {
        return forms.get(speciesName + "_" + form);
    }

    public Move move(String internalName) {
        return internalName == null ? null : moves.get(internalName);
    }

    public Item item(String internalName) {
        return internalName == null ? null : items.get(internalName);
    }

    public Ability ability(String internalName) {
        return internalName == null ? null : abilities.get(internalName);
    }

    public TypeInfo type(String internalName) {
        return internalName == null ? null : types.get(internalName);
    }

    public Nature nature(int id) {
        return id >= 0 && id < natures.size ? natures.get(id) : null;
    }

    public Nature nature(String name) {
        if (name == null) {
            return null;
        }
        Nature direct = naturesByName.get(name);
        if (direct != null) {
            return direct;
        }
        return naturesByName.get(name.toUpperCase(Locale.ROOT));
    }

    // ------------------------------------------------------------------
    // P2 lookups: wild encounters / trainers
    // ------------------------------------------------------------------

    /** The wild encounter table of a map, or null. */
    public EncounterMap encounterMap(int mapId) {
        return mapId < 0 ? null : encounters.get(String.valueOf(mapId));
    }

    /** Trainer by type + name at version 0 (the default battle). */
    public TrainerData trainer(String type, String name) {
        return trainer(type, name, 0);
    }

    /**
     * Trainer by type + name + version. A missing exact version falls back to
     * version 0, matching Essentials' {@code pbLoadTrainer}.
     */
    public TrainerData trainer(String type, String name, int version) {
        if (type == null || name == null) {
            return null;
        }
        TrainerData direct = trainers.get(type + "," + name + "," + version);
        if (direct != null) {
            return direct;
        }
        return version == 0 ? null : trainers.get(type + "," + name + ",0");
    }

    /**
     * Type effectiveness of one attacking type against a defender's types, in
     * the Essentials chart: 2x weakness, 0.5x resistance, 0x immunity, times
     * each defender type.
     */
    public float effectiveness(String attackType, Array<String> defenderTypes) {
        if (attackType == null || defenderTypes == null || defenderTypes.size == 0) {
            return 1f;
        }
        float result = 1f;
        for (String defender : defenderTypes) {
            TypeInfo info = types.get(defender);
            if (info == null) {
                continue;
            }
            if (info.immunities.contains(attackType, false)) {
                return 0f;
            }
            if (info.weaknesses.contains(attackType, false)) {
                result *= 2f;
            } else if (info.resistances.contains(attackType, false)) {
                result *= 0.5f;
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------

    /** Loads {@code generated/pbs/*.json}; missing documents stay empty. */
    public static PbsData parse(File dataRoot) {
        PbsData data = new PbsData();
        File pbs = new File(dataRoot, "pbs");
        if (!pbs.isDirectory()) {
            return data; // a project without PBS: every table stays empty
        }
        data.loaded = true;
        JsonValue index = read(pbs, "index.json");
        if (index != null) {
            data.version = index.getString("format", null);
        }
        readSpecies(data, read(pbs, "pokemon.json"));
        readForms(data, read(pbs, "pokemonforms.json"));
        readMoves(data, read(pbs, "moves.json"));
        readItems(data, read(pbs, "items.json"));
        readAbilities(data, read(pbs, "abilities.json"));
        readTypes(data, read(pbs, "types.json"));
        readTrainerTypes(data, read(pbs, "trainertypes.json"));
        readNatures(data, read(pbs, "natures.json"));
        readTm(data, read(pbs, "tm.json"));
        readEncounters(data, read(pbs, "encounters.json"));
        readTrainers(data, read(pbs, "trainers.json"));
        return data;
    }

    private static JsonValue read(File pbsDir, String name) {
        File file = new File(pbsDir, name);
        if (!file.isFile()) {
            return null;
        }
        try {
            return new JsonReader().parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        } catch (Exception error) {
            return null; // a corrupt document must not take the game down
        }
    }

    private static JsonValue child(JsonValue node, String name) {
        return node == null ? null : node.get(name);
    }

    private static Array<String> stringArray(JsonValue node) {
        Array<String> out = new Array<>();
        if (node != null && node.isArray()) {
            for (JsonValue entry = node.child; entry != null; entry = entry.next) {
                out.add(entry.asString());
            }
        }
        return out;
    }

    private static int[] intArray(JsonValue node) {
        if (node == null || !node.isArray()) {
            return new int[0];
        }
        int[] out = new int[node.size];
        int i = 0;
        for (JsonValue entry = node.child; entry != null; entry = entry.next, i++) {
            out[i] = entry.asInt();
        }
        return out;
    }

    private static void readSpecies(PbsData data, JsonValue root) {
        JsonValue byId = child(root, "byId");
        if (byId != null && byId.isObject()) {
            for (JsonValue entry = byId.child; entry != null; entry = entry.next) {
                data.speciesById.put(entry.name, entry.asString());
            }
        }
        JsonValue speciesRoot = child(root, "species");
        if (speciesRoot == null || !speciesRoot.isObject()) {
            return;
        }
        for (JsonValue node = speciesRoot.child; node != null; node = node.next) {
            Species entry = new Species();
            entry.internalName = node.getString("internalName", node.name);
            entry.id = node.getInt("id", -1);
            entry.name = node.getString("name", entry.internalName);
            entry.types = stringArray(child(node, "types"));
            entry.baseStats = intArray(child(node, "baseStats"));
            entry.genderRate = node.getString("genderRate", "Unknown");
            entry.growthRate = node.getString("growthRate", "Medium");
            entry.baseExp = node.getInt("baseExp", 0);
            entry.effortPoints = intArray(child(node, "effortPoints"));
            entry.rareness = node.getInt("rareness", 0);
            entry.happiness = node.getInt("happiness", 0);
            entry.abilities = stringArray(child(node, "abilities"));
            entry.hiddenAbility = node.getString("hiddenAbility", null);
            JsonValue moves = child(node, "moves");
            if (moves != null && moves.isArray()) {
                for (JsonValue move = moves.child; move != null; move = move.next) {
                    LearnMove learn = new LearnMove();
                    learn.level = move.getInt("level", 0);
                    learn.move = move.getString("move", null);
                    entry.moves.add(learn);
                }
            }
            entry.eggMoves = stringArray(child(node, "eggMoves"));
            entry.compatibility = stringArray(child(node, "compatibility"));
            entry.stepsToHatch = node.getInt("stepsToHatch", 0);
            entry.height = node.getFloat("height", 0f);
            entry.weight = node.getFloat("weight", 0f);
            entry.color = node.getString("color", null);
            entry.shape = node.getInt("shape", 0);
            entry.habitat = node.getString("habitat", null);
            entry.kind = node.getString("kind", null);
            entry.pokedex = node.getString("pokedex", null);
            entry.regionalNumbers = intArray(child(node, "regionalNumbers"));
            JsonValue evolutions = child(node, "evolutions");
            if (evolutions != null && evolutions.isArray()) {
                for (JsonValue evolution = evolutions.child; evolution != null; evolution = evolution.next) {
                    Evolution evo = new Evolution();
                    evo.species = evolution.getString("species", null);
                    evo.method = evolution.getString("method", null);
                    evo.parameter = evolution.getString("parameter", null);
                    entry.evolutions.add(evo);
                }
            }
            JsonValue wild = child(node, "wildItems");
            if (wild != null) {
                entry.wildItems.common = wild.getString("common", null);
                entry.wildItems.uncommon = wild.getString("uncommon", null);
                entry.wildItems.rare = wild.getString("rare", null);
            }
            entry.formName = node.getString("formName", null);
            entry.incense = node.getString("incense", null);
            readBattler(entry.battler, child(node, "battler"));
            data.species.put(entry.internalName, entry);
        }
    }

    private static void readBattler(BattlerOffsets battler, JsonValue node) {
        if (node == null) {
            return;
        }
        battler.playerX = node.getInt("playerX", 0);
        battler.playerY = node.getInt("playerY", 0);
        battler.enemyX = node.getInt("enemyX", 0);
        battler.enemyY = node.getInt("enemyY", 0);
        battler.shadowX = node.getInt("shadowX", 0);
        battler.shadowSize = node.getInt("shadowSize", 0);
    }

    private static void readForms(PbsData data, JsonValue root) {
        JsonValue formsRoot = child(root, "forms");
        if (formsRoot == null || !formsRoot.isObject()) {
            return;
        }
        for (JsonValue node = formsRoot.child; node != null; node = node.next) {
            SpeciesForm form = new SpeciesForm();
            form.species = node.getString("species", null);
            form.form = node.getInt("form", 0);
            form.key = node.getString("key", node.name);
            form.formName = node.getString("formName", null);
            if (child(node, "baseStats") != null) {
                form.baseStats = intArray(child(node, "baseStats"));
            }
            if (child(node, "types") != null) {
                form.types = stringArray(child(node, "types"));
            }
            if (child(node, "abilities") != null) {
                form.abilities = stringArray(child(node, "abilities"));
            }
            form.hiddenAbility = node.getString("hiddenAbility", null);
            if (child(node, "height") != null) {
                form.height = node.getFloat("height", 0f);
            }
            if (child(node, "weight") != null) {
                form.weight = node.getFloat("weight", 0f);
            }
            form.color = node.getString("color", null);
            form.pokedex = node.getString("pokedex", null);
            form.megaStone = node.getString("megaStone", null);
            if (child(node, "unmegaForm") != null) {
                form.unmegaForm = node.getInt("unmegaForm", 0);
            }
            JsonValue moves = child(node, "moves");
            if (moves != null && moves.isArray()) {
                form.moves = new Array<>();
                for (JsonValue move = moves.child; move != null; move = move.next) {
                    LearnMove learn = new LearnMove();
                    learn.level = move.getInt("level", 0);
                    learn.move = move.getString("move", null);
                    form.moves.add(learn);
                }
            }
            JsonValue evolutions = child(node, "evolutions");
            if (evolutions != null && evolutions.isArray()) {
                form.evolutions = new Array<>();
                for (JsonValue evolution = evolutions.child; evolution != null; evolution = evolution.next) {
                    Evolution evo = new Evolution();
                    evo.species = evolution.getString("species", null);
                    evo.method = evolution.getString("method", null);
                    evo.parameter = evolution.getString("parameter", null);
                    form.evolutions.add(evo);
                }
            }
            readBattler(form.battler, child(node, "battler"));
            data.forms.put(form.key, form);
        }
    }

    private static void readMoves(PbsData data, JsonValue root) {
        JsonValue movesRoot = child(root, "moves");
        if (movesRoot == null || !movesRoot.isObject()) {
            return;
        }
        for (JsonValue node = movesRoot.child; node != null; node = node.next) {
            Move move = new Move();
            move.internalName = node.getString("internalName", node.name);
            move.id = node.getInt("id", -1);
            move.name = node.getString("name", move.internalName);
            move.function = node.getString("function", null);
            move.power = node.getInt("power", 0);
            move.type = node.getString("type", null);
            move.category = node.getString("category", null);
            move.accuracy = node.getInt("accuracy", 0);
            move.pp = node.getInt("pp", 0);
            move.effectChance = node.getInt("effectChance", 0);
            move.target = node.getString("target", null);
            move.priority = node.getInt("priority", 0);
            move.flags = node.getString("flags", "");
            move.description = node.getString("description", "");
            data.moves.put(move.internalName, move);
        }
    }

    private static void readItems(PbsData data, JsonValue root) {
        JsonValue itemsRoot = child(root, "items");
        if (itemsRoot == null || !itemsRoot.isObject()) {
            return;
        }
        for (JsonValue node = itemsRoot.child; node != null; node = node.next) {
            Item item = new Item();
            item.internalName = node.getString("internalName", node.name);
            item.id = node.getInt("id", -1);
            item.name = node.getString("name", item.internalName);
            item.namePlural = node.getString("namePlural", item.name);
            item.pocket = node.getInt("pocket", 0);
            item.price = node.getInt("price", 0);
            item.description = node.getString("description", "");
            item.fieldUse = node.getInt("fieldUse", 0);
            item.battleUse = node.getInt("battleUse", 0);
            item.extra = stringArray(child(node, "extra"));
            data.items.put(item.internalName, item);
        }
    }

    private static void readAbilities(PbsData data, JsonValue root) {
        JsonValue abilitiesRoot = child(root, "abilities");
        if (abilitiesRoot == null || !abilitiesRoot.isObject()) {
            return;
        }
        for (JsonValue node = abilitiesRoot.child; node != null; node = node.next) {
            Ability ability = new Ability();
            ability.internalName = node.getString("internalName", node.name);
            ability.id = node.getInt("id", -1);
            ability.name = node.getString("name", ability.internalName);
            ability.description = node.getString("description", "");
            data.abilities.put(ability.internalName, ability);
        }
    }

    private static void readTypes(PbsData data, JsonValue root) {
        JsonValue typesRoot = child(root, "types");
        if (typesRoot == null || !typesRoot.isObject()) {
            return;
        }
        for (JsonValue node = typesRoot.child; node != null; node = node.next) {
            TypeInfo type = new TypeInfo();
            type.internalName = node.getString("internalName", node.name);
            type.id = node.getInt("id", -1);
            type.name = node.getString("name", type.internalName);
            type.weaknesses = stringArray(child(node, "weaknesses"));
            type.resistances = stringArray(child(node, "resistances"));
            type.immunities = stringArray(child(node, "immunities"));
            data.types.put(type.internalName, type);
        }
    }

    private static void readTrainerTypes(PbsData data, JsonValue root) {
        JsonValue trainerRoot = child(root, "trainerTypes");
        if (trainerRoot == null || !trainerRoot.isObject()) {
            return;
        }
        for (JsonValue node = trainerRoot.child; node != null; node = node.next) {
            TrainerType trainerType = new TrainerType();
            trainerType.internalName = node.getString("internalName", node.name);
            trainerType.id = node.getInt("id", -1);
            trainerType.name = node.getString("name", trainerType.internalName);
            trainerType.baseMoney = node.getInt("baseMoney", 0);
            trainerType.skill = node.getString("skill", null);
            trainerType.fields = stringArray(child(node, "fields"));
            data.trainerTypes.put(trainerType.internalName, trainerType);
        }
    }

    private static void readNatures(PbsData data, JsonValue root) {
        JsonValue naturesRoot = child(root, "natures");
        if (naturesRoot == null || !naturesRoot.isArray()) {
            return;
        }
        for (JsonValue node = naturesRoot.child; node != null; node = node.next) {
            Nature nature = new Nature();
            nature.id = node.getInt("id", -1);
            nature.internalName = node.getString("internalName", null);
            nature.name = node.getString("name", nature.internalName);
            nature.statUp = node.getString("statUp", null);
            nature.statDown = node.getString("statDown", null);
            data.natures.add(nature);
            if (nature.internalName != null) {
                data.naturesByName.put(nature.internalName, nature);
            }
        }
    }

    private static void readTm(PbsData data, JsonValue root) {
        JsonValue compatibility = child(root, "compatibility");
        if (compatibility == null || !compatibility.isObject()) {
            return;
        }
        for (JsonValue node = compatibility.child; node != null; node = node.next) {
            Array<String> learners = stringArray(node);
            data.tmCompatibility.put(node.name, learners);
            for (String learner : learners) {
                Array<String> moves = data.tmBySpecies.get(learner);
                if (moves == null) {
                    moves = new Array<>();
                    data.tmBySpecies.put(learner, moves);
                }
                moves.add(node.name);
            }
        }
    }

    /** P2: generated/pbs/encounters.json -> the per-map wild tables. */
    private static void readEncounters(PbsData data, JsonValue root) {
        JsonValue byMap = child(root, "byMap");
        if (byMap == null || !byMap.isObject()) {
            return;
        }
        for (JsonValue node = byMap.child; node != null; node = node.next) {
            EncounterMap map = new EncounterMap();
            map.id = node.getInt("id", 0);
            map.name = node.getString("name", "");
            JsonValue densities = child(node, "densities");
            if (densities != null && densities.isObject()) {
                for (JsonValue entry = densities.child; entry != null; entry = entry.next) {
                    map.densities.put(entry.name, entry.asInt());
                }
            }
            JsonValue methods = child(node, "methods");
            if (methods != null && methods.isObject()) {
                for (JsonValue table = methods.child; table != null; table = table.next) {
                    if (!table.isArray()) {
                        continue;
                    }
                    Array<EncounterEntry> entries = new Array<>();
                    for (JsonValue row = table.child; row != null; row = row.next) {
                        EncounterEntry entry = new EncounterEntry();
                        entry.species = row.getString("species", null);
                        entry.minLevel = row.getInt("min", 1);
                        entry.maxLevel = row.getInt("max", entry.minLevel);
                        if (entry.species != null) {
                            entries.add(entry);
                        }
                    }
                    map.methods.put(table.name, entries);
                }
            }
            data.encounters.put(node.name, map);
        }
    }

    /** P2: generated/pbs/trainers.json -> the trainer parties. */
    private static void readTrainers(PbsData data, JsonValue root) {
        JsonValue trainers = child(root, "trainers");
        if (trainers == null || !trainers.isObject()) {
            return;
        }
        for (JsonValue node = trainers.child; node != null; node = node.next) {
            TrainerData trainer = new TrainerData();
            trainer.key = node.getString("key", node.name);
            trainer.type = node.getString("type", "");
            trainer.name = node.getString("name", "");
            trainer.version = node.getInt("version", 0);
            trainer.loseText = node.getString("loseText", null);
            JsonValue items = child(node, "items");
            if (items != null && items.isArray()) {
                for (JsonValue item = items.child; item != null; item = item.next) {
                    trainer.items.add(item.asString());
                }
            }
            JsonValue party = child(node, "party");
            if (party != null && party.isArray()) {
                for (JsonValue node2 = party.child; node2 != null; node2 = node2.next) {
                    TrainerPokemon member = new TrainerPokemon();
                    member.species = node2.getString("species", null);
                    member.level = node2.getInt("level", 1);
                    JsonValue moves = child(node2, "moves");
                    if (moves != null && moves.isArray()) {
                        for (JsonValue move = moves.child; move != null; move = move.next) {
                            member.moves.add(move.asString());
                        }
                    }
                    member.item = node2.getString("item", null);
                    member.ability = node2.getString("ability", null);
                    member.nature = node2.getString("nature", null);
                    member.gender = node2.getString("gender", null);
                    member.ball = node2.getString("ball", null);
                    member.shiny = node2.getBoolean("shiny", false);
                    member.superShiny = node2.getBoolean("superShiny", false);
                    member.form = node2.getInt("form", 0);
                    member.happiness = node2.getInt("happiness", -1);
                    member.ivs = optionalIntArray(child(node2, "ivs"));
                    member.evs = optionalIntArray(child(node2, "evs"));
                    if (member.species != null) {
                        trainer.party.add(member);
                    }
                }
            }
            data.trainers.put(node.name, trainer);
        }
    }

    private static int[] optionalIntArray(JsonValue node) {
        if (node == null || !node.isArray()) {
            return null;
        }
        int[] out = new int[node.size];
        for (int i = 0; i < node.size; i++) {
            out[i] = node.get(i).asInt();
        }
        return out;
    }
}
