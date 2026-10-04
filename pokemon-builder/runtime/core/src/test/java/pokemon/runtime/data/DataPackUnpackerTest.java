package pokemon.runtime.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L3: the Android data pack unpacker - entries land below the target root, the
 * version marker gates re-unpacking and zip-slip entries are rejected.
 */
class DataPackUnpackerTest {

    private static byte[] zipOf(String name, String content) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    @Test
    @DisplayName("unpack writes the entries and the version marker (L3)")
    void unpacksAndMarks() throws Exception {
        File dir = Files.createTempDirectory("pb-unpack-").toFile();
        byte[] pack = zipOf("generated/system.json", "{\"a\":1}");

        assertEquals(1, DataPackUnpacker.unpack(new ByteArrayInputStream(pack), dir, "pack-v1"));
        File written = new File(dir, "generated/system.json");
        assertTrue(written.isFile());
        assertEquals("{\"a\":1}", new String(Files.readAllBytes(written.toPath()), StandardCharsets.UTF_8));

        assertTrue(DataPackUnpacker.isCurrent(dir, "pack-v1"));
        assertFalse(DataPackUnpacker.isCurrent(dir, "pack-v2"), "a new pack version must retrigger the unpack");
        assertFalse(DataPackUnpacker.isCurrent(Files.createTempDirectory("pb-unpack-empty-").toFile(), "pack-v1"));
        assertFalse(DataPackUnpacker.isCurrent(null, "pack-v1"));
    }

    @Test
    @DisplayName("unpack rejects entries that escape the target (zip-slip) (L3)")
    void rejectsZipSlip() throws Exception {
        File dir = Files.createTempDirectory("pb-unpack-slip-").toFile();
        byte[] pack = zipOf("../evil.txt", "boom");
        assertThrows(IOException.class, () -> DataPackUnpacker.unpack(new ByteArrayInputStream(pack), dir, "v1"));
        assertFalse(new File(dir.getParentFile(), "evil.txt").exists());
    }

    @Test
    @DisplayName("a re-unpack overwrites files from the previous pack (L3)")
    void reUnpackOverwrites() throws Exception {
        File dir = Files.createTempDirectory("pb-unpack-re-").toFile();
        DataPackUnpacker.unpack(new ByteArrayInputStream(zipOf("generated/system.json", "old")), dir, "v1");
        DataPackUnpacker.unpack(new ByteArrayInputStream(zipOf("generated/system.json", "new")), dir, "v2");
        assertEquals("new", new String(Files.readAllBytes(new File(dir, "generated/system.json").toPath()),
                StandardCharsets.UTF_8));
        assertTrue(DataPackUnpacker.isCurrent(dir, "v2"));
        assertFalse(DataPackUnpacker.isCurrent(dir, "v1"));
    }
}
