package pokemon.runtime.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Android sets pokemon.runtime.userdir (it has no usable user.home): saves and settings must go there. */
class StoragePortTest {

    @Test
    @DisplayName("the launcher's directory property decides where files are written and read")
    void userDirOverride(@TempDir Path dir) {
        String previous = System.getProperty(StoragePort.USER_DIR_PROPERTY);
        System.setProperty(StoragePort.USER_DIR_PROPERTY, dir.toString());
        try {
            StoragePort storage = new StoragePort();
            assertEquals(dir.toString(), storage.userDirectory());
            storage.writeUtf8("saves/1.json", "{\"a\":1}");
            assertTrue(dir.resolve("saves").resolve("1.json").toFile().isFile());
            assertEquals("{\"a\":1}", new String(storage.readUtf8("saves/1.json"), java.nio.charset.StandardCharsets.UTF_8));
        } finally {
            if (previous == null) System.clearProperty(StoragePort.USER_DIR_PROPERTY);
            else System.setProperty(StoragePort.USER_DIR_PROPERTY, previous);
        }
    }
}
