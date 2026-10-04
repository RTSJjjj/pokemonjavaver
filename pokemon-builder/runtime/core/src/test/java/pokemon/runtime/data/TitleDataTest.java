package pokemon.runtime.data;

import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** L1: the title configuration parsed from generated/title.json. */
class TitleDataTest {

    @Test
    @DisplayName("title.json parses the splash list and the BGM (L1)")
    void parses() {
        TitleData data = TitleData.parse(new JsonReader().parse(
                "{\"kind\":\"title\",\"source\":\"Modular Title Screen\","
                        + "\"splashImages\":[\"intro1\",\"origin\"],\"secondsPerSplash\":5,"
                        + "\"bgm\":\"title_hgss_0\",\"bgmVolume\":20}"));
        assertEquals("Modular Title Screen", data.source);
        assertEquals(2, data.splashImages.size);
        assertEquals("intro1", data.splashImages.first());
        assertEquals(5f, data.secondsPerSplash, 0.001f);
        assertEquals("title_hgss_0", data.bgm);
        assertEquals(20, data.bgmVolume);
        assertTrue(data.hasSplash());
    }

    @Test
    @DisplayName("title.json parses the L14 visual recipe (modifiers/footer/messages) (L14)")
    void parsesVisualRecipe() {
        TitleData data = TitleData.parse(new JsonReader().parse(
                "{\"modifiers\":[\"background:bw\",\"overlay2\",\"logoY:172\",\"logo:shine\"],"
                        + "\"footerLeft\":\"Test Game 2026\",\"footerRight\":\"Do not pirate\","
                        + "\"splashMessages\":[\"hello\",\"这次一定。\"],\"fadeTicks\":8}"));
        assertEquals(4, data.modifiers.size);
        assertEquals("background:bw", data.modifiers.first());
        assertEquals("Test Game 2026", data.footerLeft);
        assertEquals("Do not pirate", data.footerRight);
        assertEquals(2, data.splashMessages.size);
        assertEquals("这次一定。", data.splashMessages.get(1));
        assertEquals(8, data.fadeTicks);
        assertEquals(0.4f, data.fadeSeconds(), 0.001f);
    }

    @Test
    @DisplayName("a project without the plugin gets an empty configuration (L1)")
    void emptyDefaults() {
        TitleData data = TitleData.parse(null);
        assertNotNull(data);
        assertFalse(data.hasSplash());
        assertEquals("", data.bgm);
        assertEquals(100, data.bgmVolume);

        TitleData zero = TitleData.parse(new JsonReader().parse(
                "{\"splashImages\":[\"a\"],\"secondsPerSplash\":0}"));
        assertFalse(zero.hasSplash(), "zero seconds disables the sequence");
    }
}
