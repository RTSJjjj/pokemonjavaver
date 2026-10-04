package pokemon.runtime.state;

import com.badlogic.gdx.utils.LongMap;

/**
 * Essentials "temp switches" ({@code Game_Event#tempSwitches} with
 * {@code tsOn?} / {@code tsOff?} / {@code setTempSwitchOn}): one transient
 * boolean per (map id, event id, channel A..D).
 *
 * <p>Unlike self switches they belong to the map <em>visit</em>: RMXP builds
 * fresh event instances whenever a map is set up, so {@link GameState#enterMap}
 * clears them. This project's 605 arrival doors condition their autorun page on
 * the named switch {@code s:tsOff?("A")}, play the "hero walks out of the
 * doorway" animation once and then set A to stop the page from repeating.</p>
 */
public final class GameTempSwitches {

    private final LongMap<Boolean> on = new LongMap<>();
    private final StateVersion version;

    GameTempSwitches(StateVersion version) {
        this.version = version;
    }

    public boolean get(int mapId, int eventId, String channel) {
        return on.get(GameSelfSwitches.key(mapId, eventId, channel), Boolean.FALSE);
    }

    public void set(int mapId, int eventId, String channel, boolean value) {
        long key = GameSelfSwitches.key(mapId, eventId, channel);
        boolean previous = on.get(key, Boolean.FALSE);
        if (previous == value) {
            return;
        }
        if (value) {
            on.put(key, Boolean.TRUE);
        } else {
            on.remove(key);
        }
        version.bump();
    }

    public int size() {
        return on.size;
    }

    /** Map setup: RMXP throws every event instance (and its temp switches) away. */
    public void clear() {
        if (on.size == 0) {
            return;
        }
        on.clear();
        version.bump();
    }
}
