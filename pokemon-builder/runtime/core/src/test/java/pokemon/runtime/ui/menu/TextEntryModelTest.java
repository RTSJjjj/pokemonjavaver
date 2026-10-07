package pokemon.runtime.ui.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** CharacterEntryHelper (TextEntry #70): maxlength counts characters, the cursor edits in place. */
class TextEntryModelTest {

    @Test
    void chineseCharactersCountOneEachAndStopAtMaxLength() {
        TextEntryModel model = new TextEntryModel("", 3);
        assertTrue(model.insert('沐'));
        assertTrue(model.insert('桐'));
        assertTrue(model.insert('岚'));
        assertFalse(model.canInsert());
        assertFalse(model.insert('佑'));
        assertEquals("沐桐岚", model.text());
        assertEquals(3, model.length());
    }

    @Test
    void backspaceDeletesBeforeTheCursorAndInsertGoesAtTheCursor() {
        TextEntryModel model = new TextEntryModel("abcd", 10);
        model.cursorLeft();
        model.cursorLeft();                 // a b | c d
        assertTrue(model.delete());
        assertEquals("acd", model.text());
        assertTrue(model.insert('X'));
        assertEquals("aXcd", model.text());
        assertEquals(2, model.cursor());
        model.cursorHome();
        assertFalse(model.delete());        // nothing before the cursor
        assertTrue(model.deleteForward());
        assertEquals("Xcd", model.text());
    }

    @Test
    void controlCharactersAndOverlongInitialTextAreHandled() {
        TextEntryModel model = new TextEntryModel("0123456789ABC", 10);
        assertEquals("0123456789", model.text());
        TextEntryModel empty = new TextEntryModel(null, 10);
        assertFalse(empty.insert('\n'));
        assertFalse(empty.insert(0x7F));
        assertTrue(empty.insert(0x20B9F));  // a supplementary-plane character is one slot
        assertEquals(1, empty.length());
        assertEquals(1, empty.textChars().size());
    }
}
