package pokemon.runtime.data;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Stage 3 / P4: the project's in-game pinyin name table, extracted from the
 * Scripts "字库" plugin ({@code PBZ_IM_quanpin::PY}: a pinyin syllable to the
 * characters it can produce). The desktop runtime cannot open an OS text box
 * (libGDX's LWJGL3 {@code getTextInput} is a documented no-op), so the trainer
 * name is entered from this table, exactly like the original game.
 */
public final class PinyinTable {

    private final ObjectMap<String, String> table = new ObjectMap<>();

    public boolean loaded;
    public int syllables;

    private PinyinTable() {
    }

    /** Loads text/pinyin.json; a missing/old build leaves an empty table. */
    public static PinyinTable load(File dataRoot) {
        PinyinTable result = new PinyinTable();
        if (dataRoot == null) {
            return result;
        }
        File file = new File(dataRoot, "text" + File.separator + "pinyin.json");
        if (!file.isFile()) {
            return result;
        }
        try {
            JsonValue root = new JsonReader().parse(
                    new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            JsonValue map = root.get("table");
            if (map != null && map.isObject()) {
                for (JsonValue entry = map.child; entry != null; entry = entry.next) {
                    if (entry.isString()) {
                        result.table.put(entry.name, entry.asString());
                    }
                }
            }
            result.syllables = result.table.size;
            result.loaded = true;
        } catch (Exception error) {
            // a corrupt table must not take the game down; the entry falls back
            result.table.clear();
        }
        return result;
    }

    public boolean isEmpty() {
        return table.size == 0;
    }

    /** Characters for a pinyin syllable (empty when the syllable is unknown). */
    public String candidates(String syllable) {
        return syllable == null || syllable.isEmpty() ? "" : table.get(syllable, "");
    }
}
