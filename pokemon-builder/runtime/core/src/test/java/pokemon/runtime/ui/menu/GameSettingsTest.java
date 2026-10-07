package pokemon.runtime.ui.menu;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.app.StoragePort;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * L1: the options screen model - clamping, persistence through the storage
 * port and the display values.
 */
class GameSettingsTest {

    @Test
    @DisplayName("volume rows adjust in 5% steps and clamp at 0/100 (L1)")
    void adjustClamps() {
        GameSettings settings = new GameSettings();
        settings.setValue(GameSettings.Option.BGM,
                settings.step(GameSettings.Option.BGM, settings.value(GameSettings.Option.BGM), -1));
        assertEquals(95, settings.bgmVolume);
        for (int i = 0; i < 30; i++) {
            settings.setValue(GameSettings.Option.BGM,
                    settings.step(GameSettings.Option.BGM, settings.value(GameSettings.Option.BGM), -1));
        }
        assertEquals(0, settings.bgmVolume, "never below 0");
        for (int i = 0; i < 30; i++) {
            settings.setValue(GameSettings.Option.BGM,
                    settings.step(GameSettings.Option.BGM, settings.value(GameSettings.Option.BGM), 1));
        }
        assertEquals(100, settings.bgmVolume, "never above 100");
        // 超级进化规则: 传统模式 <-> ZA模式.
        settings.setValue(GameSettings.Option.BATTLE_RULE,
                settings.step(GameSettings.Option.BATTLE_RULE, 0, 1));
        assertEquals(1, settings.battleRule);
        assertEquals("ZA模式", settings.display(GameSettings.Option.BATTLE_RULE));
    }

    @Test
    @DisplayName("settings survive a save/load round trip (L1)")
    void roundTrip() throws Exception {
        File home = Files.createTempDirectory("pb-settings-").toFile();
        String previous = System.getProperty("user.home");
        System.setProperty("user.home", home.getAbsolutePath());
        try {
            GameSettings settings = new GameSettings();
            settings.bgmVolume = 40;
            settings.seVolume = 70;
            settings.bgsVolume = 0;
            settings.screensize = 3;
            settings.battleRule = 1;
            settings.save(new StoragePort());

            GameSettings loaded = GameSettings.load(new StoragePort());
            assertEquals(40, loaded.bgmVolume);
            assertEquals(70, loaded.seVolume);
            assertEquals(0, loaded.bgsVolume);
            assertTrue(loaded.fullscreen());
            assertEquals(1, loaded.battleRule, "the ZA rule survives");
            assertEquals(0.4f, loaded.bgmFactor(), 0.001f);
        } finally {
            if (previous == null) {
                System.clearProperty("user.home");
            } else {
                System.setProperty("user.home", previous);
            }
        }
    }

    @Test
    @DisplayName("a missing or broken settings file keeps the defaults (L1)")
    void brokenFileFallsBack() throws Exception {
        File home = Files.createTempDirectory("pb-settings-broken-").toFile();
        File runtime = new File(home, ".pokemon-runtime");
        assertTrue(runtime.mkdirs());
        Files.write(new File(runtime, "settings.json").toPath(), "{not json".getBytes("UTF-8"));
        String previous = System.getProperty("user.home");
        System.setProperty("user.home", home.getAbsolutePath());
        try {
            GameSettings loaded = GameSettings.load(new StoragePort());
            assertEquals(100, loaded.bgmVolume);
            assertFalse(loaded.fullscreen());
        } finally {
            if (previous == null) {
                System.clearProperty("user.home");
            } else {
                System.setProperty("user.home", previous);
            }
        }
    }
}
