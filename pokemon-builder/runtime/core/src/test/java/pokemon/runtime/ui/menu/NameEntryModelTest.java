package pokemon.runtime.ui.menu;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.data.PinyinTable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 3 / P4: the headless pinyin name entry (the project's 字库 plugin).
 */
class NameEntryModelTest {

    @Test
    @DisplayName("P4: letters build a pinyin buffer and picking composes the name")
    void typeAndPick(@TempDir Path tempDir) throws Exception {
        PinyinTable table = table(tempDir, "yu", "迂於于鱼渔余", "a", "阿啊锕");
        NameEntryModel model = new NameEntryModel(table, 12, "");

        assertTrue(model.type('y'));
        assertTrue(model.type('u'));
        assertEquals("yu", model.buffer());
        assertEquals("迂於于鱼渔余", model.candidates());

        assertTrue(model.pick(3));
        assertEquals("鱼", model.text());
        assertEquals("", model.buffer(), "picking clears the syllable buffer");
        assertTrue(model.type('a'));
        assertTrue(model.pick(1));
        assertEquals("鱼啊", model.text());
    }

    @Test
    @DisplayName("P4: candidates page when a syllable has more than a page")
    void paging(@TempDir Path tempDir) throws Exception {
        StringBuilder many = new StringBuilder();
        for (int i = 0; i < NameEntryModel.PAGE_SIZE * 2 + 3; i++) {
            many.append((char) ('一' + i));
        }
        NameEntryModel model = new NameEntryModel(table(tempDir, "wu", many.toString()), 12, "");
        model.type('w');
        model.type('u');

        assertEquals(NameEntryModel.PAGE_SIZE * 2 + 3, model.candidateCount());
        assertEquals(3, model.pageCount());
        assertEquals(0, model.page());
        model.nextPage();
        model.nextPage();
        assertEquals(2, model.page());
        model.nextPage();
        assertEquals(2, model.page(), "the last page is the last");
        model.prevPage();
        assertEquals(1, model.page());

        // A page-1 pick maps to the page offset.
        assertEquals((char) ('一' + NameEntryModel.PAGE_SIZE), many.charAt(NameEntryModel.PAGE_SIZE));
        assertTrue(model.pick(0));
        assertEquals(String.valueOf((char) ('一' + NameEntryModel.PAGE_SIZE)), model.text());
    }

    @Test
    @DisplayName("P4: backspace clears the buffer first, then the name; length is capped")
    void backspaceAndCap(@TempDir Path tempDir) throws Exception {
        NameEntryModel model = new NameEntryModel(table(tempDir, "a", "阿啊"), 2, "");
        model.type('a');
        model.backspace();
        assertEquals("", model.buffer());
        model.type('a');
        model.pick(0); // 阿
        model.type('a');
        model.pick(1); // 啊 -> at the 2-char cap
        assertEquals("阿啊", model.text());
        assertTrue(model.full());
        assertFalse(model.type('a'), "a full name takes no more pinyin");
        model.backspace();
        assertEquals("阿", model.text());
    }

    private static PinyinTable table(Path root, String... pairs) throws Exception {
        StringBuilder json = new StringBuilder("{\"total\":2,\"table\":{");
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            if (i > 0) json.append(",");
            json.append("\"").append(pairs[i]).append("\":\"").append(pairs[i + 1]).append("\"");
        }
        json.append("}}");
        Path file = root.resolve("text").resolve("pinyin.json");
        Files.createDirectories(file.getParent());
        Files.write(file, json.toString().getBytes(StandardCharsets.UTF_8));
        return PinyinTable.load(root.toFile());
    }
}
