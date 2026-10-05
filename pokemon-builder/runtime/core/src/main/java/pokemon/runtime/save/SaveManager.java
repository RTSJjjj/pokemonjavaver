package pokemon.runtime.save;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.ObjectIntMap;
import pokemon.runtime.app.StoragePort;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;
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
            variables.addChild(String.valueOf(id), new JsonValue(state.variables().get(id)));
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
        node.addChild("money", new JsonValue(trainer.money));
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
        JsonValue storage = array();
        for (com.badlogic.gdx.utils.Array<Pokemon> box : trainer.storage.boxes()) {
            JsonValue entries = array();
            for (Pokemon pokemon : box) {
                entries.addChild(pokemonJson(pokemon));
            }
            storage.addChild(entries);
        }
        node.addChild("storage", storage);
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
        node.addChild("ot", new JsonValue(pokemon.originalTrainer == null ? "" : pokemon.originalTrainer));
        node.addChild("battleRank", new JsonValue(pokemon.battleRank));
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
                state.variables().set(Math.max(1, Integer.parseInt(value.name)), value.asInt());
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
        trainer.party.members().clear();
        trainer.storage.clear();
        trainer.healMapId = -1;
        if (node == null || !node.isObject()) {
            return; // a save written before P1
        }
        trainer.name = node.getString("name", trainer.name);
        trainer.gender = node.getInt("gender", trainer.gender);
        trainer.money = node.getInt("money", 0);
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
                }
            }
        }
        JsonValue storage = node.get("storage");
        if (storage != null && storage.isArray()) {
            int boxIndex = 0;
            for (JsonValue box = storage.child; box != null; box = box.next, boxIndex++) {
                if (!box.isArray()) {
                    continue;
                }
                for (JsonValue entry = box.child; entry != null; entry = entry.next) {
                    Pokemon pokemon = readPokemon(entry);
                    if (pokemon != null) {
                        trainer.storage.box(boxIndex).add(pokemon);
                    }
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
        String originalTrainer = node.getString("ot", null);
        pokemon.originalTrainer = originalTrainer == null || originalTrainer.isEmpty()
                ? null : originalTrainer;
        pokemon.battleRank = node.getInt("battleRank", 0);
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
