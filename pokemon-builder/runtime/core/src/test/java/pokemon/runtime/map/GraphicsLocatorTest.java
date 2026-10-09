package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * L4: the packaged Graphics/Fonts below the runtime data root win over the
 * source project; without a pack the source tree stays the fallback.
 */
class GraphicsLocatorTest {

    private static File write(File file, String content) throws Exception {
        Files.createDirectories(file.getParentFile().toPath());
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("packaged Graphics below the data root win over the source project (L4)")
    void packedGraphicsWin() throws Exception {
        File source = Files.createTempDirectory("pb-src-").toFile();
        File data = Files.createTempDirectory("pb-data-").toFile();
        write(new File(source, "Graphics/Pictures/pause.png"), "source");
        write(new File(data, "Graphics/Pictures/pause.png"), "packed");

        GraphicsLocator locator = new GraphicsLocator(source, data);
        assertEquals(new File(data, "Graphics"), locator.graphicsRoot());
        File found = locator.find("Pictures", "pause.png");
        assertNotNull(found);
        assertEquals("packed", read(found));

        // Case-insensitive lookup still applies to the packed tree.
        assertEquals(found, locator.find("Pictures", "PAUSE.PNG"));
    }

    @Test
    @DisplayName("a folder is listed once; names are found whatever their case and pngNames lists the pictures")
    void listingIsKeptPerFolder() throws Exception {
        File source = Files.createTempDirectory("pb-src-").toFile();
        write(new File(source, "Graphics/Icons/itemPOTION.png"), "a");
        write(new File(source, "Graphics/Icons/item001.png"), "b");
        write(new File(source, "Graphics/Icons/readme.txt"), "c");
        GraphicsLocator locator = new GraphicsLocator(source, null);
        assertNotNull(locator.find("Icons", "itempotion.PNG"));
        assertNull(locator.find("Icons", "itemSUPERPOTION.png"));
        assertEquals(java.util.Arrays.asList("item001", "itemPOTION"), locator.pngNames("Icons"));
        assertEquals(0, locator.pngNames("Missing").size());
    }

    @Test
    @DisplayName("without a packaged copy the source project is used (L4)")
    void fallsBackToSource() throws Exception {
        File source = Files.createTempDirectory("pb-src2-").toFile();
        File data = Files.createTempDirectory("pb-data2-").toFile();
        write(new File(source, "Graphics/Pictures/pause.png"), "source");

        GraphicsLocator locator = new GraphicsLocator(source, data);
        assertEquals(new File(source, "Graphics"), locator.graphicsRoot());
        assertEquals("source", read(locator.find("Pictures", "pause.png")));
        assertNull(locator.find("Pictures", "missing.png"));
    }

    @Test
    @DisplayName("Fonts prefer the packaged copy, then the source project (L4)")
    void fontPrefersPack() throws Exception {
        File source = Files.createTempDirectory("pb-src3-").toFile();
        File data = Files.createTempDirectory("pb-data3-").toFile();
        write(new File(source, "Fonts/Fusion.ttf"), "source-font");
        write(new File(data, "Fonts/Fusion.ttf"), "packed-font");

        GraphicsLocator locator = new GraphicsLocator(source, data);
        assertEquals("packed-font", read(locator.font("Fusion.ttf")));
        assertEquals("packed-font", read(locator.font("fusion.ttf")));

        GraphicsLocator sourceOnly = new GraphicsLocator(source, null);
        assertEquals("source-font", read(sourceOnly.font("Fusion.ttf")));
        assertNull(sourceOnly.font("missing.ttf"));
    }
}
