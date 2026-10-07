package pokemon.runtime.ui.menu;

import pokemon.runtime.data.PinyinTable;

/**
 * Stage 3 / P4: headless pinyin name entry, the project's "字库"
 * ({@code PBZ_IM_quanpin}) behavior. The player types Latin letters, the model
 * lists the syllable's characters (paged, {@code MaxCharsPerLine = 11}), and
 * picking one appends it to the name. Kept free of graphics so it is testable.
 */
public final class NameEntryModel {

    /** PokemonEntryScene2::MaxCharsPerLine. */
    public static final int COLUMNS = 11;
    public static final int ROWS = 2;
    public static final int PAGE_SIZE = COLUMNS * ROWS;

    private final PinyinTable table;
    private final int maxLength;
    private String text;
    private String buffer = "";
    private int page;

    public NameEntryModel(PinyinTable table, int maxLength, String initial) {
        this.table = table;
        this.maxLength = Math.max(1, maxLength);
        this.text = initial == null ? "" : initial;
    }

    public String text() {
        return text;
    }

    public String buffer() {
        return buffer;
    }

    public int page() {
        return page;
    }

    public boolean full() {
        return text.length() >= maxLength;
    }

    /** A typed Latin letter extends the pinyin buffer; other chars are ignored. */
    public boolean type(char c) {
        char lower = Character.toLowerCase(c);
        if (lower < 'a' || lower > 'z' || full()) {
            return false;
        }
        buffer += lower;
        page = 0;
        return true;
    }

    public String candidates() {
        return table == null ? "" : table.candidates(buffer);
    }

    public int candidateCount() {
        return candidates().length();
    }

    public int pageCount() {
        int count = candidateCount();
        return count == 0 ? 0 : (count + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    public void nextPage() {
        if (page + 1 < pageCount()) {
            page++;
        }
    }

    public void prevPage() {
        if (page > 0) {
            page--;
        }
    }

    /** Appends candidate {@code indexInPage} of the current page. */
    public boolean pick(int indexInPage) {
        if (indexInPage < 0 || full()) {
            return false;
        }
        int index = page * PAGE_SIZE + indexInPage;
        String chars = candidates();
        if (index < 0 || index >= chars.length()) {
            return false;
        }
        text += chars.charAt(index);
        buffer = "";
        page = 0;
        return true;
    }

    /** Backspace removes from the pinyin buffer first, then the name. */
    public void backspace() {
        if (!buffer.isEmpty()) {
            buffer = buffer.substring(0, buffer.length() - 1);
            page = 0;
        } else if (!text.isEmpty()) {
            text = text.substring(0, text.length() - 1);
        }
    }

    /** Clears the pinyin buffer; true when there was something to clear. */
    public boolean clearBuffer() {
        if (buffer.isEmpty()) {
            return false;
        }
        buffer = "";
        page = 0;
        return true;
    }
}
