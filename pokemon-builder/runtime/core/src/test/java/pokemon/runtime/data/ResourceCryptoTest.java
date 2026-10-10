package pokemon.runtime.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** P3: the cipher matches the builder's (builder/tests/resource-crypto.test.js has the same vector). */
class ResourceCryptoTest {

    private static final String VECTOR = "504b52450100000001020304050607089995c27102bea08ba863223940d427b2a1";

    private static byte[] hex(String text) {
        byte[] out = new byte[text.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(text.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    @Test
    @DisplayName("a file the builder encrypted reads back as its plain text, through the stream, bytes and handle")
    void decryptsBuilderVector(@TempDir Path dir) throws Exception {
        File file = dir.resolve("data.json").toFile();
        Files.write(file.toPath(), hex(VECTOR));
        assertTrue(ResourceCrypto.isEncrypted(file));
        assertEquals("hello 世界 PKRE", ResourceCrypto.readString(file));
        assertEquals("hello 世界 PKRE", new String(ResourceCrypto.handle(file).readBytes(), StandardCharsets.UTF_8));
        assertEquals(VECTOR.length() / 2 - ResourceCrypto.HEADER, ResourceCrypto.handle(file).length());
    }

    @Test
    @DisplayName("a plain file is read as it is (development tree and tests)")
    void plainPassesThrough(@TempDir Path dir) throws Exception {
        File file = dir.resolve("plain.json").toFile();
        Files.write(file.toPath(), "{\"a\":1}".getBytes(StandardCharsets.UTF_8));
        assertFalse(ResourceCrypto.isEncrypted(file));
        assertEquals("{\"a\":1}", ResourceCrypto.readString(file));
        assertEquals(7L, ResourceCrypto.handle(file).length());
    }
}
