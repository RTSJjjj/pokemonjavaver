package pokemon.runtime.save;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.ObjectIntMap;
import pokemon.runtime.app.StoragePort;
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
 * variables, self switches and item counts. Pokemon data is reserved for
 * stage 3 - the "pokemon" key is written empty so the document can grow.</p>
 */
public final class SaveManager {

    public static final int SAVE_VERSION = 1;
    private static final String SAVE_DIRECTORY = "saves";

    /** Serializes the runtime state; pure, so it is unit testable. */
    public String toJson(GameState state) {
        JsonValue root = object();
        root.addChild("saveVersion", new JsonValue(SAVE_VERSION));
        root.addChild("playerName", new JsonValue(state.playerName() == null ? "" : state.playerName()));

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
        root.addChild("pokemon", array()); // reserved for stage 3
        return root.toJson(JsonWriter.OutputType.json);
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
        if (root == null || !root.isObject()
                || root.getInt("saveVersion", -1) != SAVE_VERSION) {
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
        return true;
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
