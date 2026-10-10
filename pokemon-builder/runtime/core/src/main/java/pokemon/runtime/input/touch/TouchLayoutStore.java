package pokemon.runtime.input.touch;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import pokemon.runtime.app.StoragePort;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The player's own arrangement of the on-screen keys (release plan P1, the layout editor): where each key's centre sits
 * (fractions of the screen width / height, so it survives a different screen), how much it is scaled, and the opacity of
 * all keys. Persisted as {@code touch-layout.json} next to {@code settings.json}; a missing or broken file is the default
 * layout.
 */
public final class TouchLayoutStore {

    private static final String FILE = "touch-layout.json";

    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 2f;
    public static final float MIN_OPACITY = 0.2f;

    /** Key id -> {centre x fraction, centre y fraction, scale}. */
    public final Map<String, float[]> keys = new LinkedHashMap<>();
    /** 0.2..1: how solid the keys are drawn. */
    public float opacity = 1f;

    public static TouchLayoutStore load(StoragePort storage) {
        TouchLayoutStore store = new TouchLayoutStore();
        if (storage == null) {
            return store;
        }
        try {
            String json = new String(storage.readUtf8(FILE), java.nio.charset.StandardCharsets.UTF_8);
            JsonValue root = new JsonReader().parse(json);
            store.opacity = clamp(root.getFloat("opacity", 1f), MIN_OPACITY, 1f);
            JsonValue list = root.get("keys");
            if (list != null) {
                for (JsonValue key = list.child; key != null; key = key.next) {
                    float x = key.getFloat("x", -1f);
                    float y = key.getFloat("y", -1f);
                    if (x < 0f || x > 1f || y < 0f || y > 1f) {
                        continue;
                    }
                    store.keys.put(key.name, new float[] {x, y, clamp(key.getFloat("s", 1f), MIN_SCALE, MAX_SCALE)});
                }
            }
        } catch (RuntimeException error) {
            // missing or unreadable: the default layout is a valid state
        }
        return store;
    }

    public void save(StoragePort storage) {
        if (storage == null) {
            return;
        }
        StringBuilder json = new StringBuilder("{\n");
        json.append("  \"opacity\": ").append(number(opacity)).append(",\n");
        json.append("  \"keys\": {");
        boolean first = true;
        for (Map.Entry<String, float[]> entry : keys.entrySet()) {
            json.append(first ? "\n" : ",\n");
            first = false;
            float[] v = entry.getValue();
            json.append("    \"").append(entry.getKey()).append("\": {\"x\": ").append(number(v[0]))
                    .append(", \"y\": ").append(number(v[1])).append(", \"s\": ").append(number(v[2])).append("}");
        }
        json.append(first ? "}\n" : "\n  }\n").append("}\n");
        try {
            storage.writeUtf8(FILE, json.toString());
        } catch (RuntimeException error) {
            // the arrangement just does not persist
        }
    }

    /** The saved entry of a key, created at the key's current centre when there is none yet. */
    public float[] entry(TouchButton key, int width, int height) {
        float[] entry = keys.get(key.id);
        if (entry == null) {
            entry = new float[] {key.centerX() / width, key.centerY() / height, 1f};
            keys.put(key.id, entry);
        }
        return entry;
    }

    public void reset() {
        keys.clear();
        opacity = 1f;
    }

    private static String number(float value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    static float clamp(float value, float min, float max) {
        return value < min ? min : value > max ? max : value;
    }
}
