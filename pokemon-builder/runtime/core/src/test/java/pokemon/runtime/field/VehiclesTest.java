package pokemon.runtime.field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.state.GameState;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 025_Game_Player:448-475 and 188_PItem_Items:724-747. */
class VehiclesTest {
    private final GameState state = new GameState();
    private final Vehicles vehicles = new Vehicles(PbsData.parse(new java.io.File("__no_pbs__")), state);
    private final List<String> said = new ArrayList<>();

    VehiclesTest() {
        state.fieldGlobals().outdoorOf = id -> id == 1;          // map 1 is outdoors, map 2 is not
    }

    @Test
    @DisplayName("the bicycle is allowed where the map is Outdoor; a transfer keeps it only there, and ends surfing")
    void cancel() {
        assertTrue(vehicles.canUseBike(1));
        assertFalse(vehicles.canUseBike(2));
        state.fieldGlobals().bicycle = true;
        state.fieldGlobals().surfing = true;
        vehicles.cancelVehicles(1);
        assertTrue(state.fieldGlobals().bicycle);
        assertFalse(state.fieldGlobals().surfing);
        vehicles.cancelVehicles(2);
        assertFalse(state.fieldGlobals().bicycle);
    }

    @Test
    @DisplayName("entering an indoor map walks the player off the bicycle")
    void mapChange() {
        state.fieldGlobals().bicycle = true;
        vehicles.onMapChange(2);
        assertFalse(state.fieldGlobals().bicycle);
    }

    @Test
    @DisplayName("pbBikeCheck: not while surfing, not indoors; fine outdoors")
    void bikeCheck() {
        assertTrue(vehicles.bikeCheck(1, said::add));
        assertFalse(vehicles.bikeCheck(2, said::add));
        assertEquals("这里不能使用。", said.get(0));
        state.fieldGlobals().surfing = true;
        assertFalse(vehicles.bikeCheck(1, said::add));
        assertEquals(2, said.size());
    }
}
