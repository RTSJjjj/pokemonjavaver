package pokemon.runtime.state;

import com.badlogic.gdx.utils.LongArray;
import com.badlogic.gdx.utils.LongMap;

/**
 * Per-event runtime state (project3 section 30 "Temporary Event State").
 *
 * <p>R5 records the page that the last page query resolved for every event, so
 * the renderer and (R6) the interpreter read one shared answer. R6 adds the
 * interpreter fields (running state, wait counters, move route) here.</p>
 */
public final class TemporaryEventState {

    /** Runtime record of one event of one map. */
    public static final class EventState {
        private int pageIndex = -1;

        /** Index in {@code MapData.EventData.pages}, or -1 when no page is active. */
        public int pageIndex() {
            return pageIndex;
        }

        /** Records the page a query resolved; called by the map package. */
        public void pageIndex(int index) {
            this.pageIndex = index;
        }
    }

    private final LongMap<EventState> events = new LongMap<>();

    /** The record of one event, or null when the event was never queried. */
    public EventState find(int mapId, int eventId) {
        return events.get(key(mapId, eventId));
    }

    /** The record of one event, created on first use. */
    public EventState of(int mapId, int eventId) {
        long key = key(mapId, eventId);
        EventState state = events.get(key);
        if (state == null) {
            state = new EventState();
            events.put(key, state);
        }
        return state;
    }

    public int size() {
        return events.size;
    }

    public void clear() {
        events.clear();
    }

    /** Drops the state of one map, as a fresh Game_Map would on map setup. */
    public void clearMap(int mapId) {
        LongArray doomed = new LongArray();
        for (LongMap.Entry<EventState> entry : events) {
            if (GameSelfSwitches.mapIdOf(entry.key) == mapId) {
                doomed.add(entry.key);
            }
        }
        for (int i = 0; i < doomed.size; i++) {
            events.remove(doomed.get(i));
        }
    }

    static long key(int mapId, int eventId) {
        if (mapId < 0) {
            throw new IllegalArgumentException("map id must be >= 0 but was " + mapId);
        }
        if (eventId < 0) {
            throw new IllegalArgumentException("event id must be >= 0 but was " + eventId);
        }
        return ((long) mapId << 32) | (eventId & 0xFFFFFFFFL);
    }
}
