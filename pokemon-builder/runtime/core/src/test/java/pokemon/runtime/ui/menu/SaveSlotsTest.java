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
            assertEquals(8, slots.size, "MANUAL_SLOTS: 存档1..存档8");
            assertEquals("1", slots.get(0).id);
            assertTrue(slots.get(0).exists);
            assertEquals("小测", slots.get(0).playerName);
            assertEquals(2, slots.get(0).mapId);
            assertTrue(slots.get(0).savedAtMillis > 0);
            assertTrue(slots.get(0).describe().contains("小测"));
            assertFalse(slots.get(1).exists);
            assertEquals("空档", slots.get(1).describe());
            assertEquals("存档4", slots.get(3).label);
        } finally {
            if (previous == null) {
                System.clearProperty("user.home");
            } else {
                System.setProperty("user.home", previous);
            }
        }
    }

    @Test
    @DisplayName("the autosave slot comes first on the load screens, one second older than the manual save (378 / 380)")
    void autosaveSlot() throws Exception {
        File home = Files.createTempDirectory("pb-auto-").toFile();
        String previous = System.getProperty("user.home");
        System.setProperty("user.home", home.getAbsolutePath());
        try {
            GameState state = new GameState();
            state.enterMap(2, 7, 5);
            StoragePort storage = new StoragePort();
            SaveManager saves = new SaveManager();
            assertFalse(saves.saveAuto(storage, state), "no manual save yet: last_saved is nil and the plugin's pbSave rescues");
            saves.saveManual(storage, "3", state);
            long manual = state.trainer().lastSaved;
            assertEquals("3", state.trainer().saveSlot);
            assertTrue(saves.saveAuto(storage, state));
            assertEquals(manual - 1, state.trainer().lastSaved);

            var load = SaveSlots.listForLoad(storage, null);
            assertEquals(9, load.size);
            assertEquals("auto", load.get(0).id);
            assertTrue(load.get(0).exists);
            assertEquals(manual * 1000L - 1000L, load.get(0).savedAtMillis);
            assertEquals(8, SaveSlots.list(storage, null).size, "the save screen only offers the manual slots");

            GameState restored = new GameState();
            assertTrue(saves.load(storage, "auto", restored));
            assertEquals("3", restored.trainer().saveSlot);
        } finally {
            if (previous == null) System.clearProperty("user.home"); else System.setProperty("user.home", previous);
        }
    }
}
