package pokemon.runtime.event;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Runtime side of the R7.1 script compiler (project3 sections 78, 79): maps a
 * script block id to the IR the Builder emitted, so the interpreter executes
 * {@code {"command":"GIVE_ITEM","item":"ORANBERRY","amount":1}} instead of ever
 * looking at Ruby (section 4).
 */
public final class ScriptIr {

    private final ObjectMap<String, JsonValue> commands = new ObjectMap<>();

    public static ScriptIr empty() {
        return new ScriptIr();
    }

    /** Reads the {@code {version, commands:[{id, ir}]}} document R7.1 writes. */
    public static ScriptIr load(File file) throws IOException {
        String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        return of(new JsonReader().parse(text));
    }

    public static ScriptIr of(JsonValue root) {
        ScriptIr ir = new ScriptIr();
        JsonValue list = root == null ? null : root.get("commands");
        if (list != null && list.isArray()) {
            for (JsonValue entry = list.child; entry != null; entry = entry.next) {
                String id = entry.getString("id", null);
                JsonValue command = entry.get("ir");
                if (id != null && command != null) {
                    ir.put(id, command);
                }
            }
        }
        return ir;
    }

    public void put(String id, JsonValue command) {
        commands.put(id, command);
    }

    /** IR of one block, or null when it was not compiled / is unsupported. */
    public JsonValue command(String id) {
        return id == null ? null : commands.get(id);
    }

    public int size() {
        return commands.size;
    }
}
