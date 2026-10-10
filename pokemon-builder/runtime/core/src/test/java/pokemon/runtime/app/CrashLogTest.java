package pokemon.runtime.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CrashLogTest {
    @Test
    void notesAreAppendedToCrashLog(@TempDir Path dir) throws Exception {
        System.setProperty(StoragePort.USER_DIR_PROPERTY, dir.toString());
        try {
            StoragePort storage = new StoragePort();
            CrashLog.note(storage, "save failed (1)", new IllegalStateException("disk full"));
            CrashLog.note(storage, "second", null);
            String text = new String(Files.readAllBytes(dir.resolve("crash.log")), "UTF-8");
            assertTrue(text.contains("save failed (1)") && text.contains("disk full") && text.contains("second"), text);
        } finally {
            System.clearProperty(StoragePort.USER_DIR_PROPERTY);
        }
    }
}
