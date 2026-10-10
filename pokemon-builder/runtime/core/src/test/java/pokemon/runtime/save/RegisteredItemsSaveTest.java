package pokemon.runtime.save;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.field.ItemHandlers;
import pokemon.runtime.state.BagMemory;
import pokemon.runtime.state.GameState;

import static org.junit.jupiter.api.Assertions.*;

/** 195_PItem_Bag registered items (the Ready Menu) and their cursor survive a save. */
class RegisteredItemsSaveTest {

    @Test
    @DisplayName("registered items keep their order and the Ready Menu cursor through a save")
    void roundTrip() {
        SaveManager saves = new SaveManager();
        GameState state = new GameState();
        state.enterMap(3, 2, 2);
        BagMemory memory = state.inventory().bagMemory();
        memory.register("TOWNMAP");
        memory.register("BICYCLE");
        memory.register("TOWNMAP");                       // pbRegisterItem ignores a second registration
        memory.registeredIndex[0] = 1;
        memory.registeredIndex[1] = 1;
        memory.registeredIndex[2] = 0;
        memory.unregister("NOTREGISTERED");

        GameState restored = new GameState();
        assertTrue(saves.fromJson(saves.toJson(state), restored));
        BagMemory again = restored.inventory().bagMemory();
        assertEquals(java.util.List.of("TOWNMAP", "BICYCLE"), again.registered);
        assertArrayEquals(new int[] {1, 1, 0}, again.registeredIndex);
        again.unregister("TOWNMAP");
        assertFalse(again.isRegistered("TOWNMAP"));
        assertTrue(again.isRegistered("BICYCLE"));
    }

    @Test
    @DisplayName("only the items with a UseInField handler can be registered")
    void registrable() {
        assertTrue(ItemHandlers.hasUseInFieldHandler("BICYCLE"));
        assertTrue(ItemHandlers.hasUseInFieldHandler("REPEL"));
        assertFalse(ItemHandlers.hasUseInFieldHandler("MACHBIKE"));
        assertFalse(ItemHandlers.hasUseInFieldHandler("POTION"));
    }

    @Test
    @DisplayName("the field weather is part of the save")
    void weatherRoundTrip() {
        SaveManager saves = new SaveManager();
        GameState state = new GameState();
        state.enterMap(3, 2, 2);
        state.weather().set(3, 4, 0);                     // snow, power 4, at once
        GameState restored = new GameState();
        assertTrue(saves.fromJson(saves.toJson(state), restored));
        assertEquals(3, restored.weather().type());
        assertEquals(20f, restored.weather().max(), 0.001f);
    }
}
