package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.MapData;
import pokemon.runtime.state.GameState;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R6.11: Essentials "named switches". A switch whose System.rxdata name starts
 * with {@code s:} is a script expression evaluated for the asking event
 * ({@code Game_Event#switchIsOn?}) instead of a stored boolean. 671 event pages
 * in this project condition on {@code s:tsOff?("A")} / {@code s:tsOn?("A")} -
 * the arrival doors - and expect them to be true until the door ran once.
 */
class NamedSwitchTest {

    /** Switches 21 / 22 carry this project's own names. */
    private static GameState state() {
        GameState state = new GameState();
        String[] names = new String[32];
        names[21] = "s:tsOn?(\"A\")";
        names[22] = "s:tsOff?(\"A\")";
        names[23] = "s:isOff?(\"A\")";
        names[24] = "s:pbInSafari?";
        names[25] = "s:这件事根本不存在";
        state.switchNames(names);
        return state;
    }

    private static MapData.EventData event() {
        MapData.EventData event = new MapData.EventData();
        event.id = 4;
        return event;
    }

    @Test
    @DisplayName("tsOff?(\"A\") is true until the event sets its temp switch A")
    void tempSwitchOff() {
        GameState state = state();
        MapData.EventData event = event();
        state.enterMap(9, 0, 0);

        assertTrue(EventPages.switchOn(state, 9, event, 22), "fresh visit: A is off");
        assertFalse(EventPages.switchOn(state, 9, event, 21), "and tsOn? is false");

        state.tempSwitches().set(9, event.id, "A", true);
        assertFalse(EventPages.switchOn(state, 9, event, 22), "the door page stops asking");
        assertTrue(EventPages.switchOn(state, 9, event, 21));
    }

    @Test
    @DisplayName("temp switches are per event and disappear when the map is entered again")
    void tempSwitchesArePerVisit() {
        GameState state = state();
        MapData.EventData event = event();
        state.enterMap(9, 0, 0);
        state.tempSwitches().set(9, 5, "A", true);
        assertTrue(EventPages.switchOn(state, 9, event, 22), "another event keeps its own flag");
        assertFalse(EventPages.switchOn(state, 9, newEvent(5), 22), "event 5 already ran its page");

        state.tempSwitches().set(9, event.id, "A", true);
        assertFalse(EventPages.switchOn(state, 9, event, 22), "event 4 too");

        state.enterMap(9, 0, 0); // RMXP: fresh event instances
        assertTrue(EventPages.switchOn(state, 9, event, 22), "the arrival door can run again");
        assertEquals(0, state.tempSwitches().size());
    }

    @Test
    @DisplayName("isOff?() reads the self switch and unknown names report once as false")
    void selfSwitchAndUnsupported() {
        GameState state = state();
        List<String> warnings = new ArrayList<>();
        state.switchWarnings(warnings::add);
        MapData.EventData event = event();
        state.enterMap(9, 0, 0);

        assertTrue(EventPages.switchOn(state, 9, event, 23), "self switch A starts off");
        state.selfSwitches().set(9, event.id, "A", true);
        assertFalse(EventPages.switchOn(state, 9, event, 23));

        assertFalse(EventPages.switchOn(state, 9, event, 24), "safari is not implemented yet");
        assertFalse(EventPages.switchOn(state, 9, event, 25), "unknown expression counts as false");
        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("not supported yet"), warnings.toString());
    }

    @Test
    @DisplayName("plain switches still read the switch table")
    void plainSwitches() {
        GameState state = state();
        MapData.EventData event = event();
        state.enterMap(9, 0, 0);
        assertFalse(EventPages.switchOn(state, 9, event, 8));
        state.switches().set(8, true);
        assertTrue(EventPages.switchOn(state, 9, event, 8));
    }

    private static MapData.EventData newEvent(int id) {
        MapData.EventData event = new MapData.EventData();
        event.id = id;
        return event;
    }
}
