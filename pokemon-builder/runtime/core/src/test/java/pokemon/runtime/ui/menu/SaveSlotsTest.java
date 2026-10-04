package pokemon.runtime.ui.menu;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.app.StoragePort;
import pokemon.runtime.save.SaveManager;
import pokemon.runtime.state.GameState;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * L1: the save/load slot list reads real save files (SaveManager writes them)
 * and reports empty slots honestly.
 */
class SaveSlotsTest {

    @Test
    @DisplayName("slot list shows the written save and marks the others empty (L1)")
    void listsSaves() throws Exception {
        File home = Files.createTempDirectory("pb-slots-").toFile();
        String previous = System.getProperty("user.home");
        System.setProperty("user.home", home.getAbsolutePath());
        try {
            GameState state = new GameState();
            state.enterMap(2, 7, 5);
            state.playerName("小测");
            new SaveManager().save(new StoragePort(), "1", state);

            var slots = SaveSlots.list(new StoragePort(), null);
            assertEquals(4, slots.size, "slots 1/2/3 + quick");
            assertEquals("1", slots.get(0).id);
            assertTrue(slots.get(0).exists);
            assertEquals("小测", slots.get(0).playerName);
            assertEquals(2, slots.get(0).mapId);
            assertTrue(slots.get(0).savedAtMillis > 0);
            assertTrue(slots.get(0).describe().contains("小测"));
            assertFalse(slots.get(1).exists);
            assertEquals("空档", slots.get(1).describe());
            assertEquals("快速存档", slots.get(3).label);
        } finally {
            if (previous == null) {
                System.clearProperty("user.home");
            } else {
                System.setProperty("user.home", previous);
            }
        }
    }
}
