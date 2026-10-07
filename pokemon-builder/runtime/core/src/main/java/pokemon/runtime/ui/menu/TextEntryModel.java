package pokemon.runtime.ui.menu;

import java.util.ArrayList;
import java.util.List;

/**
 * Headless port of {@code CharacterEntryHelper} (TextEntry, Scripts #70): the text
 * being typed, the insertion cursor and {@code maxlength}. Lengths count characters
 * (the script scans {@code /./m}), so one Chinese character is one slot like in the
 * original name boxes ({@code MAX_PLAYER_NAME_SIZE} / {@code MAX_POKEMON_NAME_SIZE}
 * = 10). Kept free of graphics so it is testable.
 */
public final class TextEntryModel {

    private final List<Integer> chars = new ArrayList<>();
    private final int maxLength;
    private int cursor;

    public TextEntryModel(String initial, int maxLength) {
        this.maxLength = maxLength;
        if (initial != null) {
            initial.codePoints().forEach(chars::add);
        }
        // ensure: an over-long initial text is cut to maxlength
        while (maxLength >= 0 && chars.size() > maxLength) {
            chars.remove(chars.size() - 1);
        }
        cursor = chars.size();
    }

    public String text() {
        StringBuilder out = new StringBuilder();
        for (int c : chars) {
            out.appendCodePoint(c);
        }
        return out.toString();
    }

    public int length() {
        return chars.size();
    }

    public int cursor() {
        return cursor;
    }

    public int maxLength() {
        return maxLength;
    }

    /** The characters as strings, one per slot (what the blanks show). */
    public List<String> textChars() {
        List<String> out = new ArrayList<>(chars.size());
        for (int c : chars) {
            out.add(new String(Character.toChars(c)));
        }
        return out;
    }

    public boolean canInsert() {
        return maxLength < 0 || chars.size() < maxLength;
    }

    /** {@code insert(ch)}: false when the box is full or the character is not printable. */
    public boolean insert(int codePoint) {
        if (!canInsert() || !printable(codePoint)) {
            return false;
        }
        chars.add(cursor, codePoint);
        cursor++;
        return true;
    }

    /** {@code delete}: removes the character before the cursor (Backspace). */
    public boolean delete() {
        if (chars.isEmpty() || cursor <= 0) {
            return false;
        }
        chars.remove(cursor - 1);
        cursor--;
        return true;
    }

    /** The Delete key: removes the character under the cursor. */
    public boolean deleteForward() {
        if (cursor >= chars.size()) {
            return false;
        }
        chars.remove(cursor);
        return true;
    }

    public boolean cursorLeft() {
        if (cursor > 0) {
            cursor--;
            return true;
        }
        return false;
    }

    public boolean cursorRight() {
        if (cursor < chars.size()) {
            cursor++;
            return true;
        }
        return false;
    }

    public void cursorHome() {
        cursor = 0;
    }

    public void cursorEnd() {
        cursor = chars.size();
    }

    /** Control characters (Enter, Tab, Backspace...) never enter a name. */
    static boolean printable(int codePoint) {
        return codePoint >= 0x20 && codePoint != 0x7F && !(codePoint >= 0x80 && codePoint < 0xA0)
                && !Character.isISOControl(codePoint);
    }
}
