package pokemon.runtime.state;

import com.badlogic.gdx.utils.IntMap;

import java.util.Arrays;

/**
 * RMXP {@code $game_variables} (project3 section 28): variable id to integer
 * with 0 as the default. Only non-zero values are stored; ids are 1-based.
 */
public final class GameVariables {

    private static final Integer ZERO = 0;

    private final IntMap<Integer> values = new IntMap<>();
    private final IntMap<String> texts = new IntMap<>();
    public String text(int id) { requireId(id); return texts.get(id, String.valueOf(get(id))); }
    public boolean isText(int id) { return texts.containsKey(id); }
    public Object value(int id) { return isText(id) ? texts.get(id) : get(id); }
    public void setText(int id, String value) {
        requireId(id); values.remove(id); texts.put(id, value == null ? "" : value); version.bump();
    }
    private final StateVersion version;

    GameVariables(StateVersion version) {
        this.version = version;
    }

    /** Current value; unknown variables read as 0, like the source engine. */
    public int get(int id) {
        requireId(id);
        return values.get(id, ZERO);
    }

    public void set(int id, int value) {
        requireId(id);
        if (texts.remove(id) != null) version.bump();
        int previous = values.get(id, ZERO);
        if (previous == value) {
            return;
        }
        if (value == 0) {
            values.remove(id);
        } else {
            values.put(id, value);
        }
        version.bump();
    }

    /** Ids of the variables that hold a non-zero value, ascending. */
    public int[] ids() {
        int[] ids = new int[values.size + texts.size];
        int index = 0;
        for (IntMap.Entry<Integer> entry : values) {
            ids[index++] = entry.key;
        }
        for (IntMap.Entry<String> entry : texts) ids[index++] = entry.key;
        Arrays.sort(ids);
        return ids;
    }

    public int size() {
        return values.size + texts.size;
    }

    public void clear() {
        if (values.size == 0 && texts.size == 0) {
            return;
        }
        values.clear();
        texts.clear();
        version.bump();
    }

    private static void requireId(int id) {
        if (id < 1) {
            throw new IllegalArgumentException("variable id must be >= 1 but was " + id);
        }
    }
}
