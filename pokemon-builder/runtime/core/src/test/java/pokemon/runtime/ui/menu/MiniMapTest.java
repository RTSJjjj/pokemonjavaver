package pokemon.runtime.ui.menu;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.app.StoragePort;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 335-338 ESMM: the banned maps, the name splitting and the options that survive settings.json. */
class MiniMapTest {

    @Test
    @DisplayName("BAN_MAPS keeps the mini map away from the story maps")
    void bannedMaps() {
        assertTrue(MiniMap.banned(1));
        assertTrue(MiniMap.banned(119));
        assertTrue(MiniMap.banned(462));
        assertFalse(MiniMap.banned(10));
    }

    @Test
    @DisplayName("split_str: lines of ceil((width - 4) / 16) characters")
    void splitsNames() {
        List<String> lines = MiniMap.split("一二三四五六七八九十", 112f);   // map size 7: 7 characters a line
        assertEquals(List.of("一二三四五六七", "八九十"), lines);
        assertEquals(List.of("短名"), MiniMap.split("短名", 112f));
    }

    @Test
    @DisplayName("the mini map options are saved with the other settings")
    void settingsRoundTrip() throws Exception {
        File home = Files.createTempDirectory("pb-minimap-").toFile();
        String previous = System.getProperty("user.home");
        System.setProperty("user.home", home.getAbsolutePath());
        try {
            StoragePort storage = new StoragePort();
            GameSettings settings = new GameSettings();
            assertEquals(1, settings.showMiniMap, "hidden by default");
            settings.showMiniMap = 0;
            settings.miniMapOpacity = 40;
            settings.miniMapPosition = 3;
            settings.miniMapSize = 2;
            settings.miniMapZoom = 0;
            settings.miniMapBorder = 5;
            settings.save(storage);
            GameSettings loaded = GameSettings.load(storage);
            assertEquals(0, loaded.showMiniMap);
            assertEquals(40, loaded.miniMapOpacity);
            assertEquals(3, loaded.miniMapPosition);
            assertEquals(2, loaded.miniMapSize);
            assertEquals(0, loaded.miniMapZoom);
            assertEquals(5, loaded.miniMapBorder);
        } finally {
            if (previous == null) System.clearProperty("user.home"); else System.setProperty("user.home", previous);
        }
    }
}
