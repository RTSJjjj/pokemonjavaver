package pokemon.runtime.save;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.ObjectIntMap;
import pokemon.runtime.app.StoragePort;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;
import pokemon.runtime.pokemon.Storage;
import pokemon.runtime.pokemon.TrainerState;
import pokemon.runtime.state.GameSelfSwitches;
import pokemon.runtime.state.GameState;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * JSON saves with a {@code saveVersion} (project3 section 31), written through
 * the {@link StoragePort} so the location stays platform specific (section 32).
 *
 * <p>Saved state: current map, player position / facing / name, switches,
 * variables, self switches, item counts and - since P1 - the trainer's party,
 * PC storage and respawn point. A document written before P1 (saveVersion 1) is
 * still accepted: the missing trainer key simply leaves the party empty.</p>
 */
public final class SaveManager {

    /** v2 adds the trainer (party / storage / heal point); v1 stays readable. */
    public static final int SAVE_VERSION = 2;
    private static final int OLDEST_SAVE_VERSION = 1;
    private static final String SAVE_DIRECTORY = "saves";

    /** PBS data used to rebuild Pokemon on load; null keeps the party empty. */
    private PbsData pbs;

    /** Attaches the PBS tables the party / storage reconstruction needs (P1). */
    public void attachPbs(PbsData data) {
        this.pbs = data;
    }

    /** Serializes the runtime state; pure, so it is unit testable. */
    public String toJson(GameState state) {
        JsonValue root = object();
        root.addChild("saveVersion", new JsonValue(SAVE_VERSION));
        root.addChild("playerName", new JsonValue(state.playerName() == null ? "" : state.playerName()));
        root.addChild("playerId", new JsonValue(state.playerId()));

        JsonValue map = object();
        map.addChild("id", new JsonValue(state.currentMapId()));
        map.addChild("x", new JsonValue(state.playerX()));
        map.addChild("y", new JsonValue(state.playerY()));
        map.addChild("direction", new JsonValue(state.playerDirection()));
        root.addChild("map", map);

        JsonValue switches = array();
        for (int id : state.switches().onIds()) {
            switches.addChild(new JsonValue(id));
        }
        root.addChild("switches", switches);

        JsonValue variables = object();
        for (int id : state.variables().ids()) {
            variables.addChild(String.valueOf(id), state.variables().isText(id)
                    ? new JsonValue(state.variables().text(id)) : new JsonValue(state.variables().get(id)));
        }
        root.addChild("variables", variables);

        JsonValue selfSwitches = array();
        for (long key : state.selfSwitches().onKeys()) {
            JsonValue entry = object();
            entry.addChild("map", new JsonValue(GameSelfSwitches.mapIdOf(key)));
            entry.addChild("event", new JsonValue(GameSelfSwitches.eventIdOf(key)));
            entry.addChild("channel", new JsonValue(GameSelfSwitches.channelOf(key)));
            selfSwitches.addChild(entry);
        }
        root.addChild("selfSwitches", selfSwitches);

        // Essentials $PokemonGlobal.eventvars (berry plants): the growth state
        // has to survive the save exactly like the self switches.
        JsonValue eventVarsJson = array();
        for (long key : state.eventVars().keys()) {
            int varMap = pokemon.runtime.state.GameEventVars.mapIdOf(key);
            int varEvent = pokemon.runtime.state.GameEventVars.eventIdOf(key);
            JsonValue entry = object();
            entry.addChild("map", new JsonValue(varMap));
            entry.addChild("event", new JsonValue(varEvent));
            JsonValue values = array();
            int[] stored = state.eventVars().get(varMap, varEvent);
            if (stored != null) {
                for (int value : stored) {
                    values.addChild(new JsonValue(value));
                }
            }
            entry.addChild("values", values);
            eventVarsJson.addChild(entry);
        }
        root.addChild("eventVars", eventVarsJson);

        JsonValue items = object();
        for (ObjectIntMap.Entry<String> entry : state.inventory().counts()) {
            items.addChild(entry.key, new JsonValue(entry.value));
        }
        root.addChild("items", items);
        JsonValue quests = array();
        for (pokemon.runtime.state.QuestLog.Entry entry : state.quests().entries()) {
            JsonValue quest = object();
            quest.addChild("id", new JsonValue(entry.id));
            quest.addChild("status", new JsonValue(entry.status.name()));
            quest.addChild("stage", new JsonValue(entry.stage));
            quest.addChild("updated", new JsonValue(entry.updated));
            quests.addChild(quest);
        }
        root.addChild("quests", quests);
        root.addChild("followerToggled", new JsonValue(state.followerToggled()));
        root.addChild("strengthUsed", new JsonValue(state.pokemonMapStrengthUsed()));
        root.addChild("trainer", trainerJson(state.trainer()));
        return root.toJson(JsonWriter.OutputType.json);
    }

    // ------------------------------------------------------------------
    // P1: trainer / Pokemon serialisation
    // ------------------------------------------------------------------

    private JsonValue trainerJson(TrainerState trainer) {
        JsonValue node = object();
        node.addChild("name", new JsonValue(trainer.name == null ? "" : trainer.name));
        node.addChild("gender", new JsonValue(trainer.gender));
        // R15: PokeBattle_Trainer:259-266 - the 32-bit id behind every wild
        // Pokemon's @trainerID and the shiny formula.
        node.addChild("id", new JsonValue(trainer.id));
        node.addChild("money", new JsonValue(trainer.money));
        node.addChild("region", new JsonValue(trainer.region));
        node.addChild("playSeconds", new JsonValue(trainer.playSeconds));
        node.addChild("pokedex", new JsonValue(trainer.pokedex));
        node.addChild("pokepc", new JsonValue(trainer.pokepc));
        node.addChild("expPot", new JsonValue(trainer.expPot));
        JsonValue seen = array(), owned = array(), badges = array();
        for (String id : trainer.seen) seen.addChild(new JsonValue(id));
        for (String id : trainer.owned) owned.addChild(new JsonValue(id));
        for (int id : trainer.badges) badges.addChild(new JsonValue(id));
        node.addChild("seen", seen); node.addChild("owned", owned); node.addChild("badges", badges);
        JsonValue regions = object();
        for (java.util.Map.Entry<Integer, Storage> region : trainer.regionalStorage.entrySet()) {
            regions.addChild(String.valueOf(region.getKey()), storageJson(region.getValue()));
        }
        node.addChild("regionalStorage", regions);
        if (trainer.hasPokemonCenter()) {
            JsonValue heal = object();
            heal.addChild("map", new JsonValue(trainer.healMapId));
            heal.addChild("x", new JsonValue(trainer.healX));
            heal.addChild("y", new JsonValue(trainer.healY));
            heal.addChild("direction", new JsonValue(trainer.healDirection));
            node.addChild("heal", heal);
        }
        JsonValue party = array();
        for (Pokemon pokemon : trainer.party.members()) {
            party.addChild(pokemonJson(pokemon));
        }
        node.addChild("party", party);
        node.addChild("storage", storageJson(trainer.storage));
        return node;
    }

    /**
     * Pokemon_Storage: boxes keep their 30 fixed slots (each saved Pokemon carries its
     * "slot"), plus the box name / wallpaper, the current box and the unlocked
     * wallpapers. Boxes that were never touched are not written.
     */
    private JsonValue storageJson(Storage storage) {
        JsonValue node = object();
        node.addChild("currentBox", new JsonValue(storage.currentBox));
        JsonValue unlocked = array();
        boolean[] flags = storage.unlockedWallpapers();
        for (int i = 0; i < flags.length; i++) if (flags[i]) unlocked.addChild(new JsonValue(i));
        node.addChild("unlockedWallpapers", unlocked);
        JsonValue boxes = array();
        for (int index = 0; index < Storage.BOXES; index++) {
            Storage.Box box = storage.boxIfCreated(index);
            if (box == null) continue;
            boolean renamed = !box.name.equals("盒子 " + (index + 1))
                    || box.background != index % Storage.BASICWALLPAPERQTY;
            if (box.empty() && !renamed) continue;
            JsonValue entry = object();
            entry.addChild("index", new JsonValue(index));
            entry.addChild("name", new JsonValue(box.name));
            entry.addChild("background", new JsonValue(box.background));
            JsonValue slots = array();
            for (int slot = 0; slot < box.length(); slot++) {
                Pokemon pokemon = box.get(slot);
                if (pokemon == null) continue;
                JsonValue p = pokemonJson(pokemon);
                p.addChild("slot", new JsonValue(slot));
                slots.addChild(p);
            }
            entry.addChild("slots", slots);
            boxes.addChild(entry);
        }
        node.addChild("boxes", boxes);
        return node;
    }

    private static JsonValue pokemonJson(Pokemon pokemon) {
        JsonValue node = object();
        node.addChild("species", new JsonValue(
                pokemon.species == null ? "" : pokemon.species.internalName));
        if (pokemon.form != null) {
            node.addChild("form", new JsonValue(pokemon.form.key));
        }
        node.addChild("name", new JsonValue(pokemon.name == null ? "" : pokemon.name));
        node.addChild("level", new JsonValue(pokemon.level));
        node.addChild("exp", new JsonValue(pokemon.exp));
        node.addChild("ivs", intArray(pokemon.ivs));
        node.addChild("evs", intArray(pokemon.evs));
        if (pokemon.nature != null) {
            node.addChild("nature", new JsonValue(pokemon.nature.internalName));
        }
        if (pokemon.ability != null) {
            node.addChild("ability", new JsonValue(pokemon.ability));
        }
        if (pokemon.moves.size > 0) {
            JsonValue moves = array();
            for (Pokemon.MoveSlot slot : pokemon.moves) {
                JsonValue move = object();
                move.addChild("move", new JsonValue(
                        slot.move == null ? "" : slot.move.internalName));
                move.addChild("pp", new JsonValue(slot.pp));
                move.addChild("ppUp", new JsonValue(slot.ppUp));
                moves.addChild(move);
            }
            node.addChild("moves", moves);
        }
        node.addChild("hp", new JsonValue(pokemon.hp));
        node.addChild("status", new JsonValue(pokemon.status == null ? "" : pokemon.status));
        node.addChild("gender", new JsonValue(pokemon.gender));
        node.addChild("shiny", new JsonValue(pokemon.shiny));
        node.addChild("egg", new JsonValue(pokemon.egg));
        node.addChild("item", new JsonValue(pokemon.item == null ? "" : pokemon.item));
        node.addChild("happiness", new JsonValue(pokemon.happiness));
        node.addChild("stepsToHatch", new JsonValue(pokemon.stepsToHatch));
        if (pokemon.ribbons.size > 0) {
            JsonValue ribbons = array();
            for (String ribbon : pokemon.ribbons) {
                ribbons.addChild(new JsonValue(ribbon));
            }
            node.addChild("ribbons", ribbons);
        }
        node.addChild("ot", new JsonValue(pokemon.originalTrainer == null ? "" : pokemon.originalTrainer));
        node.addChild("battleRank", new JsonValue(pokemon.battleRank));
        node.addChild("personalID", new JsonValue(pokemon.personalID));
        // R15: the full OT id (PokeBattle_Pokemon:39); publicID is its low half.
        node.addChild("trainerID", new JsonValue(pokemon.trainerID));
        node.addChild("publicID", new JsonValue(pokemon.publicID));
        node.addChild("otGender", new JsonValue(pokemon.otGender));
        node.addChild("ballused", new JsonValue(pokemon.ballused));
        node.addChild("markings", new JsonValue(pokemon.markings));
        node.addChild("obtainMap", new JsonValue(pokemon.obtainMap));
        node.addChild("obtainLevel", new JsonValue(pokemon.obtainLevel));
        node.addChild("obtainMode", new JsonValue(pokemon.obtainMode));
        node.addChild("obtainText", new JsonValue(pokemon.obtainText == null ? "" : pokemon.obtainText));
        node.addChild("pokerus", new JsonValue(pokemon.pokerus));
        return node;
    }

    private static JsonValue intArray(int[] values) {
        JsonValue node = array();
        for (int value : values) {
            node.addChild(new JsonValue(value));
        }
        return node;
    }

    private static int[] intArray(JsonValue node, int fallbackLength) {
        int[] values = new int[fallbackLength];
        if (node == null || !node.isArray()) {
            return values;
        }
        for (int i = 0; i < values.length && i < node.size; i++) {
            values[i] = node.get(i).asInt();
        }
        return values;
    }

    /**
     * Applies a save document.
     *
     * @return false when the text is not readable or carries another
     *         {@code saveVersion}; the state is left untouched in that case
     */
    public boolean fromJson(String json, GameState state) {
        JsonValue root;
        try {
            root = new JsonReader().parse(json);
        } catch (RuntimeException error) {
            return false;
        }
        if (root == null || !root.isObject()) {
            return false;
        }
        int version = root.getInt("saveVersion", -1);
        if (version < OLDEST_SAVE_VERSION || version > SAVE_VERSION) {
            return false;
        }
        JsonValue map = root.get("map");
        if (map == null || !map.isObject()) {
            return false;
        }
        int mapId = map.getInt("id", -1);
        if (mapId < 0) {
            return false;
        }

        state.switches().clear();
        JsonValue switches = root.get("switches");
        if (switches != null && switches.isArray()) {
            for (JsonValue id = switches.child; id != null; id = id.next) {
                state.switches().set(Math.max(1, id.asInt()), true);
            }
        }

        state.variables().clear();
        JsonValue variables = root.get("variables");
        if (variables != null && variables.isObject()) {
            for (JsonValue value = variables.child; value != null; value = value.next) {
                int id = Math.max(1, Integer.parseInt(value.name));
                if (value.isString()) state.variables().setText(id, value.asString());
                else state.variables().set(id, value.asInt());
            }
        }

        state.selfSwitches().clear();
        JsonValue selfSwitches = root.get("selfSwitches");
        if (selfSwitches != null && selfSwitches.isArray()) {
            for (JsonValue entry = selfSwitches.child; entry != null; entry = entry.next) {
                state.selfSwitches().set(entry.getInt("map", 0), entry.getInt("event", 0),
                        entry.getString("channel", "A"), true);
            }
        }

        state.eventVars().clear();
        JsonValue eventVars = root.get("eventVars");
        if (eventVars != null && eventVars.isArray()) {
            for (JsonValue entry = eventVars.child; entry != null; entry = entry.next) {
                JsonValue values = entry.get("values");
                if (values == null || !values.isArray()) {
                    continue;
                }
                int[] stored = new int[values.size];
                int i = 0;
                for (JsonValue value = values.child; value != null; value = value.next) {
                    stored[i++] = value.asInt();
                }
                state.eventVars().set(entry.getInt("map", 0), entry.getInt("event", 0), stored);
            }
        }

        state.inventory().clear();
        JsonValue items = root.get("items");
        if (items != null && items.isObject()) {
            for (JsonValue value = items.child; value != null; value = value.next) {
                state.inventory().add(value.name, value.asInt());
            }
        }

        state.playerName(root.getString("playerName", state.playerName()));
        // Older saves predate the selector and used the male graphic (id 0);
        // a new game never loads, so it still starts blank (-1).
        state.playerId(root.getInt("playerId", 0));
        state.enterMap(mapId, map.getInt("x", state.playerX()), map.getInt("y", state.playerY()));
        state.setPlayerPosition(map.getInt("x", state.playerX()), map.getInt("y", state.playerY()),
                map.getInt("direction", state.playerDirection()));
        // Plugin batch 1: quests + the follower flag. Both keys are optional so
        // saves written before the batch still load.
        state.quests().clear();
        JsonValue quests = root.get("quests");
        if (quests != null && quests.isArray()) {
            for (JsonValue quest = quests.child; quest != null; quest = quest.next) {
                String id = quest.getString("id", null);
                if (id == null || id.isEmpty()) {
                    continue;
                }
                pokemon.runtime.state.QuestLog.Status status =
                        pokemon.runtime.state.QuestLog.Status.ACTIVE;
                try {
                    status = pokemon.runtime.state.QuestLog.Status.valueOf(
                            quest.getString("status", "ACTIVE"));
                } catch (IllegalArgumentException ignored) {
                    // unknown status: keep it active
                }
                state.quests().restore(id, status, quest.getInt("stage", 0),
                        quest.getBoolean("updated", false));
            }
        }
        state.followerToggled(root.getBoolean("followerToggled", false));
        state.pokemonMapStrengthUsed(root.getBoolean("strengthUsed", false));
        // P1: the trainer / party / PC storage. A v1 document has no "trainer"
        // key (or a null one) and simply leaves the party empty.
        loadTrainer(root.get("trainer"), state.trainer());
        return true;
    }

    // ------------------------------------------------------------------
    // P1: trainer / Pokemon reconstruction
    // ------------------------------------------------------------------

    private void loadTrainer(JsonValue node, TrainerState trainer) {
        trainer.reset();
        if (node == null || !node.isObject()) {
            return; // a save written before P1
        }
        trainer.name = node.getString("name", trainer.name);
        trainer.gender = node.getInt("gender", trainer.gender);
        trainer.id = node.getInt("id", trainer.id);
        trainer.money = node.getInt("money", 0);
        trainer.region = Math.max(0, node.getInt("region", 0));
        trainer.playSeconds = Math.max(0, node.getDouble("playSeconds", 0));
        trainer.pokedex = node.getBoolean("pokedex", false);
        trainer.pokepc = node.getBoolean("pokepc", false);
        trainer.expPot = Math.max(0, node.getInt("expPot", 0));
        JsonValue seen = node.get("seen"), owned = node.get("owned"), badges = node.get("badges");
        if (seen != null && seen.isArray()) for (JsonValue id : seen) trainer.seen.add(id.asString());
        if (owned != null && owned.isArray()) for (JsonValue id : owned) trainer.owned.add(id.asString());
        trainer.seen.addAll(trainer.owned);
        if (badges != null && badges.isArray()) for (JsonValue id : badges) trainer.badges.add(id.asInt());
        JsonValue heal = node.get("heal");
        if (heal != null && heal.isObject()) {
            trainer.setPokemonCenter(heal.getInt("map", -1), heal.getInt("x", 0),
                    heal.getInt("y", 0), heal.getInt("direction", 0));
        }
        JsonValue party = node.get("party");
        if (party != null && party.isArray()) {
            for (JsonValue entry = party.child; entry != null; entry = entry.next) {
                Pokemon pokemon = readPokemon(entry);
                if (pokemon != null) {
                    trainer.party.add(pokemon);
                    trainer.registerOwned(pokemon);
                }
            }
        }
        readStorage(node.get("storage"), trainer.storage, trainer);
        JsonValue regions = node.get("regionalStorage");
        if (regions != null && regions.isObject()) for (JsonValue region : regions) {
            int id;
            try { id = Integer.parseInt(region.name); } catch (NumberFormatException invalid) { continue; }
            if (id <= 0) continue;
            readStorage(region, trainer.storageForRegion(id), trainer);
        }
    }

    /**
     * Reads {@link #storageJson}. The previous format (an array of boxes, each an
     * array of Pokemon with no slot numbers) is migrated by filling each box from
     * its first slot in the saved order.
     */
    private void readStorage(JsonValue node, Storage storage, TrainerState trainer) {
        if (node == null) return;
        if (node.isArray()) {
            int boxIndex = 0;
            for (JsonValue box = node.child; box != null && boxIndex < Storage.BOXES; box = box.next, boxIndex++) {
                if (!box.isArray()) continue;
                int slot = 0;
                for (JsonValue entry = box.child; entry != null; entry = entry.next) {
                    Pokemon pokemon = readPokemon(entry);
                    if (pokemon != null && slot < Storage.SLOTS) {
                        storage.box(boxIndex).set(slot++, pokemon);
                        trainer.registerOwned(pokemon);
                    }
                }
            }
            return;
        }
        if (!node.isObject()) return;
        storage.currentBox = Math.max(0, Math.min(node.getInt("currentBox", 0), Storage.BOXES - 1));
        JsonValue unlocked = node.get("unlockedWallpapers");
        if (unlocked != null && unlocked.isArray()) {
            for (JsonValue value = unlocked.child; value != null; value = value.next) storage.unlockWallpaper(value.asInt());
        }
        JsonValue boxes = node.get("boxes");
        if (boxes == null || !boxes.isArray()) return;
        for (JsonValue entry = boxes.child; entry != null; entry = entry.next) {
            int index = entry.getInt("index", -1);
            if (index < 0 || index >= Storage.BOXES) continue;
            Storage.Box box = storage.box(index);
            box.name = entry.getString("name", box.name);
            box.background = entry.getInt("background", box.background);
            JsonValue slots = entry.get("slots");
            if (slots == null || !slots.isArray()) continue;
            for (JsonValue slotNode = slots.child; slotNode != null; slotNode = slotNode.next) {
                Pokemon pokemon = readPokemon(slotNode);
                int slot = slotNode.getInt("slot", -1);
                if (pokemon != null && slot >= 0 && slot < Storage.SLOTS) {
                    box.set(slot, pokemon);
                    trainer.registerOwned(pokemon);
                }
            }
        }
    }

    /** Rebuilds one Pokemon from its save node; null without PBS data. */
    private Pokemon readPokemon(JsonValue node) {
        if (pbs == null || node == null || !node.isObject()) {
            return null;
        }
        PbsData.Species species = pbs.species(node.getString("species", ""));
        if (species == null) {
            return null;
        }
        Pokemon pokemon = new Pokemon(species, node.getInt("level", 1), pbs);
        String formKey = node.getString("form", null);
        if (formKey != null) {
            PbsData.SpeciesForm form = pbs.form(formKey);
            if (form != null) {
                pokemon.form = form;
                pokemon.internalName = form.key;
            }
        }
        pokemon.name = node.getString("name", pokemon.name);
        pokemon.ivs = intArray(node.get("ivs"), 6);
        pokemon.evs = intArray(node.get("evs"), 6);
        String nature = node.getString("nature", null);
        if (nature != null) {
            pokemon.nature = pbs.nature(nature);
        }
        String ability = node.getString("ability", null);
        if (ability != null) {
            pokemon.ability = ability;
        }
        pokemon.moves.clear();
        JsonValue moves = node.get("moves");
        if (moves != null && moves.isArray()) {
            for (JsonValue entry = moves.child; entry != null; entry = entry.next) {
                PbsData.Move move = pbs.move(entry.getString("move", ""));
                if (move == null) {
                    continue;
                }
                Pokemon.MoveSlot slot = new Pokemon.MoveSlot(move);
                slot.pp = entry.getInt("pp", slot.maxPp);
                slot.ppUp = entry.getInt("ppUp", 0);
                pokemon.moves.add(slot);
            }
        }
        pokemon.hp = node.getInt("hp", pokemon.maxHp());
        pokemon.exp = node.getInt("exp",
                PokemonStats.experienceForLevel(pokemon.growthRate(), pokemon.level));
        pokemon.status = node.getString("status", "");
        pokemon.gender = node.getInt("gender", pokemon.gender);
        pokemon.shiny = node.getBoolean("shiny", false);
        pokemon.egg = node.getBoolean("egg", false);
        String item = node.getString("item", null);
        pokemon.item = item == null || item.isEmpty() ? null : item;
        pokemon.happiness = node.getInt("happiness", pokemon.happiness);
        pokemon.stepsToHatch = node.getInt("stepsToHatch", pokemon.stepsToHatch);
        JsonValue ribbons = node.get("ribbons");
        if (ribbons != null && ribbons.isArray()) {
            for (JsonValue entry = ribbons.child; entry != null; entry = entry.next) {
                pokemon.ribbons.add(entry.asString());
            }
        }
        String originalTrainer = node.getString("ot", null);
        pokemon.originalTrainer = originalTrainer == null || originalTrainer.isEmpty()
                ? null : originalTrainer;
        pokemon.battleRank = node.getInt("battleRank", 0);
        pokemon.personalID = node.getInt("personalID", 0);
        // A v1..v4 save has no full id: the visible half is what it stored.
        pokemon.trainerID = node.getInt("trainerID", node.getInt("publicID", 0));
        pokemon.publicID = node.getInt("publicID", pokemon.trainerID & 0xFFFF);
        pokemon.otGender = node.getInt("otGender", -1);
        pokemon.ballused = node.getInt("ballused", 0);
        pokemon.markings = node.getInt("markings", 0);
        pokemon.obtainMap = node.getInt("obtainMap", 0);
        pokemon.obtainLevel = node.getInt("obtainLevel", pokemon.level);
        pokemon.obtainMode = node.getInt("obtainMode", 0);
        String obtainText = node.getString("obtainText", null);
        pokemon.obtainText = obtainText == null || obtainText.isEmpty() ? null : obtainText;
        pokemon.pokerus = node.getInt("pokerus", 0);
        return pokemon;
    }

    public void save(StoragePort storage, String slot, GameState state) {
        storage.writeUtf8(relativePath(slot), toJson(state));
    }

    public boolean load(StoragePort storage, String slot, GameState state) {
        try {
            return fromJson(new String(storage.readUtf8(relativePath(slot)), StandardCharsets.UTF_8), state);
        } catch (RuntimeException error) {
            return false;
        }
    }

    /** File based variant; the desktop build and the tests use it directly. */
    public void save(File file, GameState state) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }
        Files.write(file.toPath(), toJson(state).getBytes(StandardCharsets.UTF_8));
    }

    public boolean load(File file, GameState state) throws IOException {
        if (!file.isFile()) {
            return false;
        }
        return fromJson(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8), state);
    }

    public void dispose() {
        // Nothing is held open between saves.
    }

    private static String relativePath(String slot) {
        return SAVE_DIRECTORY + "/" + slot + ".json";
    }

    private static JsonValue object() {
        return new JsonValue(JsonValue.ValueType.object);
    }

    private static JsonValue array() {
        return new JsonValue(JsonValue.ValueType.array);
    }
}
