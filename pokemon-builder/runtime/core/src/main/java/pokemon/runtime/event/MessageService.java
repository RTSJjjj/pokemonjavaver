package pokemon.runtime.event;

import com.badlogic.gdx.utils.Array;

/**
 * Message and choice state (project3 sections 19, 20). The interpreter writes
 * it, the message window renders it, and neither knows about the other: no UI
 * type appears here, so the interpreter stays headless-testable.
 */
public final class MessageService {

    /**
     * The project's message window shows two 32px lines: the name-box plugin's
     * {@code pbMessageDisplay} uses {@code (Graphics.height>480) ? 3 : 2} and
     * the project's screen is 672x448.
     */
    public static final int LINES_PER_PAGE = 2;

    private final Array<String> lines = new Array<>();
    private final Array<String> choices = new Array<>();
    private String speaker;
    private int cursor;
    private int selected = -1;
    private int cancelType;
    private boolean visible;
    private boolean waiting;
    private boolean choiceMode;
    /** Window_InputNumberPokemon (065_SpriteWindow_text:612-): digit count, value and cursor digit. */
    private boolean numberMode;
    private int numberDigits;
    private int number;
    private int numberIndex;
    /** Window height of the current message (R6.31: {@code \l[n]}). */
    private int linesPerPage = LINES_PER_PAGE;
    /** Windowskin requested by {@code \w[skin]} (R6.31); null = default. */
    private String skin;
    /** {@code \wm} / {@code \wu}: where the window sits (0 bottom, 1 middle, 2 top). */
    private int position;

    /** Shows one page of text; {@code wait} false means the page needs no confirm. */
    public void showLines(Array<String> pageLines, String speakerName, boolean wait) {
        showLines(pageLines, speakerName, wait, LINES_PER_PAGE, null);
    }

    /**
     * Shows one page of text with the window's skin and line count (R6.31).
     * {@code linesPerPage} 0 keeps the default.
     */
    public void showLines(Array<String> pageLines, String speakerName, boolean wait,
                          int linesPerPage, String skin, int position) {
        showLines(pageLines, speakerName, wait, linesPerPage, skin);
        this.position = position;
    }

    public void showLines(Array<String> pageLines, String speakerName, boolean wait,
                          int linesPerPage, String skin) {
        close();
        this.position = 0;
        lines.addAll(pageLines);
        speaker = speakerName;
        this.linesPerPage = linesPerPage <= 0 ? LINES_PER_PAGE : linesPerPage;
        this.skin = skin;
        visible = true;
        waiting = wait;
    }

    /** Window height of the current page (number of 32px lines). */
    public int linesPerPage() {
        return linesPerPage;
    }

    /** 0 bottom, 1 middle of the screen, 2 top. */
    public int position() {
        return position;
    }

    /** Windowskin for the current message, null = the project's speech skin. */
    public String skin() {
        return skin;
    }

    /**
     * Shows the choice window (RMXP command 102). The question text stays
     * visible while the player picks: RMXP shows the message and the choice
     * window together, so the lines and speaker survive the mode switch.
     *
     * @param cancelType 0 = cancel is ignored, 1 = cancel allowed, 2 = cancel branch
     */
    public void showChoices(Array<String> options, int cancelType) {
        choices.clear();
        choices.addAll(options);
        this.cancelType = cancelType;
        selected = -1;
        cursor = 0;
        visible = true;
        waiting = true;
        choiceMode = true;
    }

    /**
     * Opens the number input window (Input Number, command 103): 071_Messages:493-503
     * builds {@code ChooseNumberParams} ({@code setMaxDigits}, the variable's value as
     * default) and {@code pbChooseNumber} (071_Messages:761-) shows a
     * {@code Window_InputNumberPokemon}. The message text on screen stays.
     */
    public void showNumberInput(int digitsMax, int initial) {
        numberDigits = Math.max(1, digitsMax);                          // setMaxDigits :[1,value].max
        long limit = 1;
        for (int i = 0; i < numberDigits; i++) {
            limit *= 10L;
        }
        number = (int) Math.max(0L, Math.min(limit - 1L, initial));     // number= / clamp(initial, min, max)
        numberIndex = numberDigits - 1;                                 // @index=digits_max-1
        visible = true;
        waiting = true;
        numberMode = true;
    }

    public boolean numberMode() {
        return numberMode;
    }

    public int numberDigits() {
        return numberDigits;
    }

    public int number() {
        return number;
    }

    public int numberIndex() {
        return numberIndex;
    }

    /**
     * UP / DOWN on the window (065_SpriteWindow_text:673-686): the digit under
     * the cursor goes up or down by one, wrapping 9 -> 0 and 0 -> 9.
     */
    public void changeDigit(int delta) {
        if (!numberMode) {
            return;
        }
        long place = 1;
        for (int i = 0; i < numberDigits - 1 - numberIndex; i++) {
            place *= 10L;                                               // 10 ** (digits - 1 - @index)
        }
        long n = number / place % 10;                                   // :680
        long value = number - n * place;                                // :681
        n = delta > 0 ? (n + 1) % 10 : (n + 9) % 10;                    // :683/:685
        number = (int) (value + n * place);                             // :687
    }

    /** LEFT / RIGHT (065_SpriteWindow_text:689-702): moves the cursor, wrapping, when there are two or more digits. */
    public void moveDigitCursor(int delta) {
        if (!numberMode || numberDigits < 2) {
            return;
        }
        numberIndex = Math.floorMod(numberIndex + delta, numberDigits);
    }

    public boolean visible() {
        return visible;
    }

    /** True while the player must act: confirm a page or pick a choice. */
    public boolean waiting() {
        return waiting;
    }

    public boolean choiceMode() {
        return choiceMode;
    }

    public Array<String> lines() {
        return lines;
    }

    public String speaker() {
        return speaker;
    }

    public Array<String> choices() {
        return choices;
    }

    public int cursor() {
        return cursor;
    }

    /** Index of the chosen option, or -1 while nothing was chosen. */
    public int selected() {
        return selected;
    }

    public int cancelType() {
        return cancelType;
    }

    public void moveCursor(int delta) {
        if (!choiceMode || choices.size == 0) {
            return;
        }
        cursor = Math.floorMod(cursor + delta, choices.size);
    }

    /** Confirm: closes the page, or records the highlighted choice. */
    public void confirm() {
        if (choiceMode) {
            selected = cursor;
        }
        close();
    }

    /** Cancel: ignored when the choices forbid it, otherwise it selects nothing. */
    public void cancel() {
        if (!choiceMode || cancelType == 0) {
            return;
        }
        selected = -1;
        close();
    }

    /** {@code \g}: the money window is up beside this message. */
    private boolean gold;

    public boolean gold() {
        return gold;
    }

    public void gold(boolean value) {
        gold = value;
    }

    /** Hides the window and clears the per message buffers. */
    public void close() {
        gold = false;
        lines.clear();
        choices.clear();
        speaker = null;
        cursor = 0;
        visible = false;
        waiting = false;
        choiceMode = false;
        numberMode = false;
        linesPerPage = LINES_PER_PAGE;
        skin = null;
    }

    /** Full reset, including the last selection. */
    public void clear() {
        close();
        selected = -1;
        cancelType = 0;
    }
}
