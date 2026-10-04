package pokemon.runtime.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless R5 tests for GameState and its containers (project3 sections 28,
 * 30, 61): defaults, change tracking, id scoping and the map / player record.
 */
class GameStateTest {

    @Test
    @DisplayName("switches default to off and only a real change bumps the version")
    void switches() {
        GameState state = new GameState();
        long start = state.version();
        assertFalse(state.switches().get(12));

        state.switches().set(12, true);
        assertTrue(state.switches().get(12));
        long after = state.version();
        assertTrue(after > start);

        state.switches().set(12, true);              // no change, no bump
        assertEquals(after, state.version());

        state.switches().set(12, false);
        assertFalse(state.switches().get(12));
        assertArrayEquals(new int[0], state.switches().onIds());

        state.switches().set(45, true);
        state.switches().set(12, true);
        assertEquals(2, state.switches().size());
        assertArrayEquals(new int[] { 12, 45 }, state.switches().onIds());
        state.switches().clear();
        assertEquals(0, state.switches().size());
        assertThrows(IllegalArgumentException.class, () -> state.switches().set(0, true));
    }

    @Test
    @DisplayName("variables default to 0 and only non-zero values are stored")
    void variables() {
        GameState state = new GameState();
        assertEquals(0, state.variables().get(45));
        state.variables().set(45, 3);
        assertEquals(3, state.variables().get(45));
        assertEquals(1, state.variables().size());

        state.variables().set(7, -2);
        assertArrayEquals(new int[] { 7, 45 }, state.variables().ids());

        state.variables().set(45, 0);                // 0 is the default again
        assertEquals(0, state.variables().get(45));
        assertArrayEquals(new int[] { 7 }, state.variables().ids());
        assertThrows(IllegalArgumentException.class, () -> state.variables().get(0));
    }

    @Test
    @DisplayName("self switches are scoped by map, event and channel")
    void selfSwitches() {
        GameState state = new GameState();
        assertFalse(state.selfSwitches().get(1, 5, "A"));
        state.selfSwitches().set(1, 5, "A", true);
        assertTrue(state.selfSwitches().get(1, 5, "A"));
        assertTrue(state.selfSwitches().get(1, 5, "a"));      // channel letter is case-insensitive
        assertFalse(state.selfSwitches().get(2, 5, "A"));     // other map
        assertFalse(state.selfSwitches().get(1, 6, "A"));     // other event
        assertFalse(state.selfSwitches().get(1, 5, "B"));     // other channel
        assertEquals(1, state.selfSwitches().size());
        assertThrows(IllegalArgumentException.class, () -> state.selfSwitches().get(1, 5, "E"));
    }

    @Test
    @DisplayName("entering a map records the player and drops that map's temporary event state")
    void mapAndPlayer() {
        GameState state = new GameState();
        state.enterMap(3, 7, 9);
        assertEquals(3, state.currentMapId());
        assertEquals(7, state.playerX());
        assertEquals(9, state.playerY());

        state.setPlayerPosition(8, 10, 4);
        assertEquals(4, state.playerDirection());

        state.temporary().of(3, 1);
        state.temporary().of(4, 1);
        assertEquals(2, state.temporary().size());
        state.enterMap(3, 1, 1);                     // re-entering resets map 3 only
        assertNull(state.temporary().find(3, 1));
        assertNotNull(state.temporary().find(4, 1));

        long version = state.version();
        state.setPlayerPosition(2, 2, 6);            // walking never changes page conditions
        assertEquals(version, state.version());
        assertThrows(IllegalArgumentException.class, () -> state.enterMap(-1, 0, 0));
    }
}
