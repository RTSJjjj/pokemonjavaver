package pokemon.runtime.state;

import com.badlogic.gdx.utils.IntMap;

import java.util.Arrays;

/**
 * RMXP {@code $game_switches} (project3 section 28): switch id to boolean with
 * "off" as the default. Ids are 1-based, matching the source project's Switch
 * table; an id below 1 is a data error and is reported instead of ignored.
 */
public final class GameSwitches {

    private static final Boolean ON = Boolean.TRUE;

    private final IntMap<Boolean> on = new IntMap<>();
    private final StateVersion version;

    GameSwitches(StateVersion version) {
        this.version = version;
    }

    /** True when the switch is on; unknown switches are off. */
    public boolean get(int id) {
        requireId(id);
        return on.get(id, Boolean.FALSE);
    }

    public void set(int id, boolean value) {
        requireId(id);
        boolean previous = on.get(id, Boolean.FALSE);
        if (previous == value) {
            return;
        }
        if (value) {
            on.put(id, ON);
        } else {
            on.remove(id);
        }
        version.bump();
    }

    /** Ids of the switches that are currently on, ascending (save / debug). */
    public int[] onIds() {
        int[] ids = new int[on.size];
        int index = 0;
        for (IntMap.Entry<Boolean> entry : on) {
            ids[index++] = entry.key;
        }
        Arrays.sort(ids);
        return ids;
    }

    public int size() {
        return on.size;
    }

    public void clear() {
        if (on.size == 0) {
            return;
        }
        on.clear();
        version.bump();
    }

    private static void requireId(int id) {
        if (id < 1) {
            throw new IllegalArgumentException("switch id must be >= 1 but was " + id);
        }
    }
}
