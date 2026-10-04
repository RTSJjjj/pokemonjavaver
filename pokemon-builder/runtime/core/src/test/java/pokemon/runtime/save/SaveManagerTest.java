package pokemon.runtime.save;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.state.GameState;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R10 tests: the save document round-trips every piece of runtime state, and a
 * document with another saveVersion or broken JSON is rejected.
 */
class SaveManagerTest {

    private static GameState state() {
        GameState state = new GameState();
        state.enterMap(7, 4, 5);
        state.setPlayerPosition(4, 5, 6);
        state.playerName("叶昕苍");
        state.switches().set(3, true);
        state.switches().set(9, true);
        state.variables().set(45, 7);
        state.variables().set(7, -2);
        state.selfSwitches().set(7, 12, "B", true);
        state.inventory().add("ORANBERRY", 3);
        state.pokemonMapStrengthUsed(true);
        return state;
    }

    private static void assertSameState(GameState expected, GameState actual) {
        assertEquals(expected.currentMapId(), actual.currentMapId());
        assertEquals(expected.playerX(), actual.playerX());
        assertEquals(expected.playerY(), actual.playerY());
        assertEquals(expected.playerDirection(), actual.playerDirection());
        assertEquals(expected.playerName(), actual.playerName());
        assertEquals(expected.switches().get(3), actual.switches().get(3));
        assertEquals(expected.switches().get(9), actual.switches().get(9));
        assertEquals(2, actual.switches().size());
        assertEquals(7, actual.variables().get(45));
        assertEquals(-2, actual.variables().get(7));
        assertTrue(actual.selfSwitches().get(7, 12, "B"));
        assertFalse(actual.selfSwitches().get(7, 12, "A"));
        assertEquals(3, actual.inventory().count("ORANBERRY"));
        assertEquals(expected.pokemonMapStrengthUsed(), actual.pokemonMapStrengthUsed());
    }

    @Test
    @DisplayName("every piece of state survives a JSON round trip")
    void roundTrip() {
        SaveManager saves = new SaveManager();
        GameState original = state();
        String json = saves.toJson(original);

        GameState restored = new GameState();
        assertTrue(saves.fromJson(json, restored), () -> "document was rejected: " + json);
        assertSameState(original, restored);
        assertTrue(json.contains("\"saveVersion\""), "the version field must be written");
        assertTrue(json.contains(String.valueOf(SaveManager.SAVE_VERSION)));
    }

    @Test
    @DisplayName("a save file round trips through disk")
    void fileRoundTrip() throws Exception {
        File file = new File(Files.createTempDirectory("save-manager").toFile(), "saves/slot1.json");
        SaveManager saves = new SaveManager();
        saves.save(file, state());
        assertTrue(file.isFile());

        GameState restored = new GameState();
        assertTrue(saves.load(file, restored));
        assertSameState(state(), restored);
        assertFalse(saves.load(new File(file.getParentFile(), "missing.json"), new GameState()));
    }

    @Test
    @DisplayName("another saveVersion and broken JSON are rejected without touching the state")
    void rejectsBadDocuments() throws Exception {
        SaveManager saves = new SaveManager();
        GameState target = new GameState();
        target.enterMap(1, 1, 1);

        assertFalse(saves.fromJson("{\"saveVersion\": 99, \"map\": {\"id\": 2}}", target));
        assertFalse(saves.fromJson("{not json", target));
        assertFalse(saves.fromJson("{\"saveVersion\": 1}", target));
        assertEquals(1, target.currentMapId());

        File file = new File(Files.createTempDirectory("save-bad").toFile(), "bad.json");
        Files.write(file.toPath(), "{\"saveVersion\": 0}".getBytes(StandardCharsets.UTF_8));
        assertFalse(saves.load(file, target));
        assertEquals(1, target.currentMapId());
    }
}
