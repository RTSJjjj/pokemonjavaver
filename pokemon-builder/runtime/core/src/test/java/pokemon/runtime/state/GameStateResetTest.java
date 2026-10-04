package pokemon.runtime.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** L1: "new game" from the title screen clears every progress value. */
class GameStateResetTest {

    @Test
    @DisplayName("reset clears switches, variables, items, quests and the position (L1)")
    void resetClearsProgress() {
        GameState state = new GameState();
        state.enterMap(4, 12, 9);
        state.playerName("旧名字");
        state.switches().set(1, true);
        state.variables().set(1, 42);
        state.inventory().add("POTION", 3);
        state.quests().activate("Quest1");
        state.followerToggled(true);
        state.pokemonMapStrengthUsed(true);

        state.reset();

        assertFalse(state.switches().get(1));
        assertEquals(0, state.variables().get(1));
        assertEquals(0, state.inventory().count("POTION"));
        assertNull(state.quests().entry("Quest1"));
        assertFalse(state.followerToggled());
        assertFalse(state.pokemonMapStrengthUsed());
        assertEquals("训练家", state.playerName());
        assertEquals(-1, state.currentMapId(), "the title screen enters the start map next");
    }
}
