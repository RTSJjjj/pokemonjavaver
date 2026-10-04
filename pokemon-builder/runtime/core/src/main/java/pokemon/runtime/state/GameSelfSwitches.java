package pokemon.runtime.state;

import com.badlogic.gdx.utils.LongMap;

import java.util.Locale;

/**
 * RMXP self switches (project3 section 28): one boolean per
 * {@code (map id, event id, channel A..D)}. Values belong to the map, so
 * leaving and re-entering a map keeps them, exactly like the source engine.
 */
public final class GameSelfSwitches {

    private static final String CHANNELS = "ABCD";
    private static final Boolean ON = Boolean.TRUE;
    /** Event ids live in the low 30 bits; map ids keep the high 32 bits. */
    private static final int MAX_EVENT_ID = (1 << 30) - 1;

    private final LongMap<Boolean> on = new LongMap<>();
    private final StateVersion version;

    GameSelfSwitches(StateVersion version) {
        this.version = version;
    }

    /** Example: {@code selfSwitches.get(1, 5, "A")} (project3 section 28). */
    public boolean get(int mapId, int eventId, String channel) {
        return on.get(key(mapId, eventId, channel), Boolean.FALSE);
    }

    public void set(int mapId, int eventId, String channel, boolean value) {
        long key = key(mapId, eventId, channel);
        boolean previous = on.get(key, Boolean.FALSE);
        if (previous == value) {
            return;
        }
        if (value) {
            on.put(key, ON);
        } else {
            on.remove(key);
        }
        version.bump();
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

    static long key(int mapId, int eventId, String channel) {
        if (mapId < 0) {
            throw new IllegalArgumentException("map id must be >= 0 but was " + mapId);
        }
        if (eventId < 0 || eventId > MAX_EVENT_ID) {
            throw new IllegalArgumentException("event id out of range: " + eventId);
        }
        if (channel == null || channel.length() != 1) {
            throw new IllegalArgumentException("self switch channel must be A-D but was " + channel);
        }
        int index = CHANNELS.indexOf(channel.toUpperCase(Locale.ROOT).charAt(0));
        if (index < 0) {
            throw new IllegalArgumentException("self switch channel must be A-D but was " + channel);
        }
        return ((long) mapId << 32) | ((long) eventId << 2) | index;
    }

    public static int mapIdOf(long key) {
        return (int) (key >>> 32);
    }

    /** Event id of an encoded key (save files read the keys back). */
    public static int eventIdOf(long key) {
        return (int) ((key >>> 2) & 0x3FFFFFFFL);
    }

    /** Channel letter of an encoded key. */
    public static String channelOf(long key) {
        return String.valueOf(CHANNELS.charAt((int) (key & 0x3)));
    }

    /** Encoded keys of every self switch that is on, for saves (R10). */
    public long[] onKeys() {
        long[] keys = new long[on.size];
        int index = 0;
        for (com.badlogic.gdx.utils.LongMap.Entry<Boolean> entry : on) {
            keys[index++] = entry.key;
        }
        java.util.Arrays.sort(keys);
        return keys;
    }
}
