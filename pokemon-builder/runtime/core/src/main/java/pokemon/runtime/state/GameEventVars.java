package pokemon.runtime.state;

import com.badlogic.gdx.utils.LongMap;

/**
 * Essentials {@code $PokemonGlobal.eventvars}: one value per
 * {@code (map id, event id)}, read/written by the map interpreter through
 * {@code getVariable} / {@code setVariable} (PField_Field:827-844) and by
 * {@code Game_Event#variable} (Game_Event:80-87).
 *
 * <p>The berry plants ({@code PField_BerryPlants}) keep their whole growth
 * state in this table - stage, berry item, seconds alive, last checkup, dampness,
 * replant count, yield penalty and mulch - so it has to survive save/load
 * exactly like the self switches do.</p>
 */
public final class GameEventVars {

    /** Event ids live in the low 32 bits; map ids keep the high 32 bits. */
    private static final int MAX_EVENT_ID = (1 << 30) - 1;

    private final LongMap<int[]> values = new LongMap<>();
    /** {@code setVariable(:SYMBOL)}: the event's variable when the script stored a symbol (the lottery prize). */
    private final LongMap<String> texts = new LongMap<>();
    private final StateVersion version;

    GameEventVars(StateVersion version) {
        this.version = version;
    }

    /** @return the stored value, or null when the event has none yet */
    public int[] get(int mapId, int eventId) {
        return values.get(key(mapId, eventId));
    }

    /** Stores a copy (the interpreter hands out its live array). */
    public void set(int mapId, int eventId, int[] value) {
        long key = key(mapId, eventId);
        texts.remove(key);
        if (value == null) {
            if (values.remove(key) != null) {
                version.bump();
            }
            return;
        }
        values.put(key, value.clone());
        version.bump();
    }

    /** The text stored by {@code setVariable(:SYMBOL)}, or null (Ruby nil). */
    public String getText(int mapId, int eventId) {
        return texts.get(key(mapId, eventId));
    }

    /** {@code setVariable(sym)} / {@code pbSetEventTime} share one slot per event: a text replaces the number and vice versa. */
    public void setText(int mapId, int eventId, String value) {
        long key = key(mapId, eventId);
        values.remove(key);
        if (value == null) texts.remove(key); else texts.put(key, value);
        version.bump();
    }

    /** Encoded keys of the events that hold a text, sorted, for saves. */
    public long[] textKeys() {
        long[] keys = new long[texts.size];
        int index = 0;
        for (LongMap.Entry<String> entry : texts) keys[index++] = entry.key;
        java.util.Arrays.sort(keys);
        return keys;
    }

    public int size() {
        return values.size;
    }

    public void clear() {
        if (values.size == 0) {
            return;
        }
        values.clear();
        texts.clear();
        version.bump();
    }

    /** Encoded keys of every stored event variable, sorted, for saves. */
    public long[] keys() {
        long[] keys = new long[values.size];
        int index = 0;
        for (LongMap.Entry<int[]> entry : values) {
            keys[index++] = entry.key;
        }
        java.util.Arrays.sort(keys);
        return keys;
    }

    static long key(int mapId, int eventId) {
        if (mapId < 0) {
            throw new IllegalArgumentException("map id must be >= 0 but was " + mapId);
        }
        if (eventId < 0 || eventId > MAX_EVENT_ID) {
            throw new IllegalArgumentException("event id out of range: " + eventId);
        }
        return ((long) mapId << 32) | (eventId & 0xFFFFFFFFL);
    }

    public static int mapIdOf(long key) {
        return (int) (key >>> 32);
    }

    /** Event id of an encoded key (save files read the keys back). */
    public static int eventIdOf(long key) {
        return (int) (key & 0xFFFFFFFFL);
    }
}
