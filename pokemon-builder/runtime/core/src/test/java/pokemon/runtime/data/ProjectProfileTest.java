package pokemon.runtime.data;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R6.12: project.json carries a "runtime" profile (player charsets, message
 * font, screen size) so the Java runtime no longer hardcodes this project's
 * choices. Old manifests without the block keep working through the defaults.
 */
class ProjectProfileTest {

    private static ProjectInfo parse(String json) {
        Json reader = new Json();
        reader.setIgnoreUnknownFields(true);
        return reader.fromJson(ProjectInfo.class, json);
    }

    @Test
    @DisplayName("the runtime profile is read from project.json")
    void readsProfile() {
        ProjectInfo info = parse("{\"kind\":\"project\",\"runtime\":{"
                + "\"playerCharset\":\"trchar000\",\"runningCharset\":\"boy_run\","
                + "\"messageFont\":\"FusionPixelMonoPatched.ttf\","
                + "\"screenWidth\":672,\"screenHeight\":448}}");
        assertNotNull(info.runtime);
        assertEquals("trchar000", info.runtime.playerCharset);
        assertEquals("boy_run", info.runtime.runningCharset);
        assertEquals("FusionPixelMonoPatched.ttf", info.runtime.messageFont);
        assertEquals(672, info.runtime.screenWidth);
        assertEquals(448, info.runtime.screenHeight);
        assertTrue(info.runtime.hasPlayerCharset());
        assertTrue(info.runtime.hasRunningCharset());
        assertTrue(info.runtime.hasMessageFont());
    }

    @Test
    @DisplayName("a manifest without the profile falls back to the runtime defaults")
    void fallsBackWhenAbsent() {
        ProjectInfo info = parse("{\"kind\":\"project\",\"projectName\":\"x\"}");
        assertNull(info.runtime);
        ProjectInfo.RuntimeProfile profile = new ProjectInfo.RuntimeProfile();
        assertFalse(profile.hasPlayerCharset());
        assertFalse(profile.hasRunningCharset());
        assertFalse(profile.hasMessageFont());
        assertEquals("trchar000", pokemon.runtime.map.MapScreen.PLAYER_CHARACTER);
        assertEquals("boy_run", pokemon.runtime.map.MapScreen.PLAYER_RUNNING_CHARACTER);
        assertEquals("FusionPixelMonoPatched.ttf", pokemon.runtime.map.MapScreen.DEFAULT_MESSAGE_FONT);
    }

    @Test
    @DisplayName("screenSize reads the project's resolution from project.json (R6.21)")
    void screenSizeFromManifest(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir)
            throws Exception {
        java.nio.file.Files.writeString(dir.resolve("project.json"),
                "{\"kind\":\"project\",\"runtime\":{\"screenWidth\":672,\"screenHeight\":448}}");
        assertArrayEquals(new int[] {672, 448}, ProjectInfo.screenSize(dir.toFile()));
        assertNull(ProjectInfo.screenSize(dir.resolve("missing").toFile()),
                "no manifest -> the runtime keeps its fallback");
    }
}
