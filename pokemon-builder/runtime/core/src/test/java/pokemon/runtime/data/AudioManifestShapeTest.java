package pokemon.runtime.data;

import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R9: the A4 builder output and the runtime parser must agree on the manifest
 * shape - the ids live under "audio" (a mismatched key made every lookup miss).
 */
class AudioManifestShapeTest {

    private static final String MANIFEST = "{"
            + "\"version\":\"pokemon-builder/audio-manifest/1\","
            + "\"preset\":\"standard\","
            + "\"audio\":{\"Door enter\":{\"type\":\"SE\","
            + "\"file\":\"audio/SE/Door enter.ogg\",\"source\":\"Audio/SE/Door enter.ogg\","
            + "\"encoding\":\"ogg/vorbis\",\"loopStartSamples\":-1,\"loopEndSamples\":-1}}}";

    @Test
    @DisplayName("ids resolve through the parser the game uses")
    void parsesBuilderOutput() {
        AudioManifestData manifest = AudioManifestData.parse(new JsonReader().parse(MANIFEST));
        assertTrue(manifest.loaded);
        assertEquals("standard", manifest.preset);
        AudioManifestData.Entry entry = manifest.find("Door enter");
        assertNotNull(entry, "the builder writes the ids under \"audio\"");
        assertEquals("SE", entry.type);
        assertEquals("audio/SE/Door enter.ogg", entry.file);
        assertNull(manifest.find("no such sound"));
    }
}
