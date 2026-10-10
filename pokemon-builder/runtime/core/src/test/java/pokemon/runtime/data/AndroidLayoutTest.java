package pokemon.runtime.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.map.GraphicsLocator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Android data pack unpacks below the app's files directory as {@code generated/} with {@code Graphics/} and
 * {@code Fonts/} beside it; the launcher hands over the files directory itself.
 */
class AndroidLayoutTest {

    @Test
    @DisplayName("a root without project.json that has generated/ below it is that folder")
    void rootBelow(@TempDir Path dir) throws Exception {
        Path generated = Files.createDirectories(dir.resolve("generated"));
        Files.write(generated.resolve("project.json"), "{}".getBytes());
        assertEquals(generated.toFile(), RuntimeDataLocator.resolve(dir.toString(), new File(".")));
        assertEquals(generated.toFile(), RuntimeDataLocator.resolve(generated.toString(), new File(".")));
    }

    @Test
    @DisplayName("runtime-data/ below the root works the same (the desktop package layout)")
    void runtimeDataBelow(@TempDir Path dir) throws Exception {
        Path data = Files.createDirectories(dir.resolve("runtime-data"));
        Files.write(data.resolve("project.json"), "{}".getBytes());
        assertEquals(data.toFile(), RuntimeDataLocator.resolve(dir.toString(), new File(".")));
    }

    @Test
    @DisplayName("Graphics/ and Fonts/ beside generated/ are found")
    void picturesBeside(@TempDir Path dir) throws Exception {
        Path generated = Files.createDirectories(dir.resolve("generated"));
        Path pictures = Files.createDirectories(dir.resolve("Graphics").resolve("Pictures"));
        Files.write(pictures.resolve("a.png"), new byte[] {1});
        Path fonts = Files.createDirectories(dir.resolve("Fonts"));
        Files.write(fonts.resolve("f.ttf"), new byte[] {1});
        GraphicsLocator locator = new GraphicsLocator(null, generated.toFile());
        assertNotNull(locator.find("Pictures", "a.png"));
        assertNotNull(locator.font("f.ttf"));
    }
}
