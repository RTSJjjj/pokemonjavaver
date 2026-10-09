package pokemon.runtime.event;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.state.GameState;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Message markup for the Essentials message window (project3 section 20).
 *
 * <p>RMXP writes control codes such as {@code \n} (line break), {@code \v[n]}
 * (variable) and {@code \^} (do not wait), and this project's data also uses
 * Essentials / plugin tags. R6.31 keeps the styling the window can draw:</p>
 * <ul>
 *   <li>{@code \c[n]} and {@code <c3=B,S>} / {@code </c3>} / {@code <c2=...>}
 *       text colours (the window resolves them against the current skin);</li>
 *   <li>{@code <ac>} central alignment;</li>
 *   <li>{@code \b} / {@code \r} become the two colours the project's name-box
 *       plugin uses;</li>
 * </ul>
 * everything else is stripped as before. {@code \w[skin]} (and the
 * {@code \sign[skin]} spelling the plugin rewrites to it) becomes
 * {@link Parsed#skin}; {@code \l[n]} becomes {@link Parsed#lineCount}.
 */
public final class MessageText {

    /** \v[12], \xn[Name], \c[2], \l[5], \w[sign bw town], \PN, ... */
    private static final Pattern CODE = Pattern.compile("\\\\[A-Za-z]+(\\[[^\\]]*\\])?");
    /** Timing codes without a letter: \!, \., \|, \^. */
    private static final Pattern TIMING = Pattern.compile("\\\\[.!|^]");
    /** Essentials rich text tags such as <ac> or <c3=292929,73946b>. */
    private static final Pattern TAG = Pattern.compile("<[^<>]{1,32}>");
    private static final Pattern VARIABLE = Pattern.compile("\\\\v\\[(\\d+)\\]");
    private static final Pattern SPEAKER = Pattern.compile("\\\\xn\\[([^\\]]*)\\]");
    /** \w[skin] and the plugin's \sign[skin] spelling. */
    private static final Pattern SKIN = Pattern.compile("\\\\(?:w|sign)\\[([^\\]]*)\\]",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SE = Pattern.compile("\\\\se\\[([^\\]]*)\\]", Pattern.CASE_INSENSITIVE);
    /** {@code \g}: the money window (071_Messages:1199, pbDisplayGoldWindow). */
    private static final Pattern GOLD = Pattern.compile("\\\\g(?![A-Za-z])", Pattern.CASE_INSENSITIVE);
    private static final Pattern OPEN = Pattern.compile("\\\\op(?![A-Za-z])", Pattern.CASE_INSENSITIVE);

    /** The characters a text would show: control codes and tags removed. */
    private static int visibleLength(String text) {
        return text.replaceAll("\\\\[A-Za-z]+(\\[[^\\]]*\\])?", "").replaceAll("<[^<>]*>", "").length();
    }

    /** \me[name] and \wtnp[n] (071_Messages:1016-1066). */
    private static final Pattern ME = Pattern.compile("\\\\me\\[([^\\]]*)\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern WTNP = Pattern.compile("\\\\wtnp\\[(\\d+)\\]", Pattern.CASE_INSENSITIVE);
    /** \l[n]: the message asks for at least n lines. */
    private static final Pattern LINE_COUNT = Pattern.compile("\\\\l\\[(\\d+)\\]");
    /** {@code \wm} / {@code \wu} / {@code \wd}: the window in the middle, at the top, at the bottom (071_Messages:1198-1217). */
    private static final Pattern POSITION = Pattern.compile("\\\\w([mud])(?![A-Za-z\\[])", Pattern.CASE_INSENSITIVE);
    /** \b / \r (not followed by another letter) - the plugin's colour codes. */
    private static final Pattern BLUE = Pattern.compile("\\\\b(?![A-Za-z])",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RED = Pattern.compile("\\\\r(?![A-Za-z])",
            Pattern.CASE_INSENSITIVE);
    private static final String BLUE_TAG = "<c2=6546675A>";
    private static final String RED_TAG = "<c2=043C675A>";
    /** \PN / \pn: the player's name. */
    private static final Pattern PLAYER_NAME = Pattern.compile("\\\\pn(?![A-Za-z])",
            Pattern.CASE_INSENSITIVE);

    /** One parsed message: lines for the window, speaker and wait behaviour. */
    public static final class Parsed {
        public final Array<String> lines = new Array<>();
        public String speaker;
        /** False when the message ends with {@code \^}: do not wait for input. */
        public boolean waitForInput = true;
        /** {@code \w[skin]}: the windowskin for this message, null = default. */
        public String skin;
        /** {@code \l[n]}: requested minimum lines, 0 = the window default. */
        public int lineCount;
        /** {@code \wd} (default) 0 = bottom, {@code \wm} 1 = middle of the screen, {@code \wu} 2 = top. */
        public int position;
        /** {@code \me[name]}: a jingle played when the message appears (071_Messages), null = none. */
        public String me;
        /**
         * {@code \se[name]} before the first visible character (071_Messages:1145-1152 {@code startSE}): replaces the decision
         * sound the message opens with; an empty name makes it silent. Null = no such control.
         */
        public String startSe;
        /** {@code \g}: the money window is shown beside the message. */
        public boolean gold;
        /** {@code \op}: the sign window opens (:1122-1123 signWaitCount), which plays no opening sound. */
        public boolean open;
        /** {@code \wtnp[n]}: the message closes by itself after n frames (071_Messages), 0 = waits for input. */
        public int autoFrames;
    }

    private MessageText() { }

    public static Parsed parse(Array<String> rawLines, GameState state) {
        return parse(rawLines, state, 0);
    }

    /**
     * @param units wrap lines at this many half-width units (RMXP wraps at a
     *              fixed pixel width; CJK counts two units); 0 keeps the
     *              source line breaks only
     */
    public static Parsed parse(Array<String> rawLines, GameState state, int units) {
        Parsed parsed = new Parsed();
        String playerName = playerName(state);
        for (int i = 0; i < rawLines.size; i++) {
            String raw = rawLines.get(i);
            if (raw == null) {
                continue;
            }
            if (i == rawLines.size - 1 && raw.contains("\\^")) {
                parsed.waitForInput = false; // "\^": do not wait for input
            }
            Matcher speaker = SPEAKER.matcher(raw);
            if (speaker.find() && parsed.speaker == null) {
                parsed.speaker = replaceName(speaker.group(1), playerName);
            }
            if (parsed.skin == null) {
                Matcher skin = SKIN.matcher(raw);
                if (skin.find()) {
                    parsed.skin = skin.group(1).trim();
                }
            }
            if (i == 0) {
                Matcher se = SE.matcher(raw);
                if (se.find() && visibleLength(raw.substring(0, se.start())) == 0) {
                    parsed.startSe = se.group(1).trim();
                }
            }
            Matcher where = POSITION.matcher(raw);
            while (where.find()) {
                char code = Character.toLowerCase(where.group(1).charAt(0));
                parsed.position = code == 'm' ? 1 : code == 'u' ? 2 : 0;
            }
            if (OPEN.matcher(raw).find()) {
                parsed.open = true;
            }
            if (GOLD.matcher(raw).find()) {
                parsed.gold = true;
            }
            Matcher jingle = ME.matcher(raw);
            if (jingle.find() && parsed.me == null) {
                parsed.me = jingle.group(1).trim();
            }
            Matcher auto = WTNP.matcher(raw);
            if (auto.find()) {
                parsed.autoFrames = Integer.parseInt(auto.group(1));
            }
            Matcher lines = LINE_COUNT.matcher(raw);
            if (lines.find()) {
                parsed.lineCount = Math.max(parsed.lineCount, Integer.parseInt(lines.group(1)));
            }
            String cleaned = clean(raw, state, playerName);
            // RMXP uses a literal \n inside a line as a line break; clean()
            // already turned it into a real newline.
            String[] parts = cleaned.split("\\n", -1);
            for (String part : parts) {
                parsed.lines.add(part.stripTrailing());
            }
        }
        if (parsed.lines.size == 0) {
            parsed.lines.add("");
        }
        if (units > 0) {
            wrap(parsed.lines, units);
        }
        return parsed;
    }

    /**
     * Splits lines that are longer than the message window. Colours and tags
     * are zero width and stay atomic; a colour that is still open when a line
     * breaks is repeated on the continuation line (the source colour stack
     * persists across line breaks).
     */
    public static void wrap(Array<String> lines, int units) {
        if (units <= 0) {
            return;
        }
        Array<String> wrapped = new Array<>();
        String active = null;
        for (String line : lines) {
            StringBuilder current = new StringBuilder();
            int width = 0;
            int i = 0;
            while (i < line.length()) {
                char ch = line.charAt(i);
                if (ch == '\\' && i + 2 < line.length()
                        && (line.charAt(i + 1) == 'c' || line.charAt(i + 1) == 'C')
                        && line.charAt(i + 2) == '[') {
                    int close = line.indexOf(']', i);
                    if (close > 0) {
                        String code = line.substring(i, close + 1);
                        current.append(code);
                        active = code; // \c[n] keeps applying to the rest
                        i = close + 1;
                        continue;
                    }
                }
                if (ch == '<') {
                    int close = line.indexOf('>', i);
                    if (close > 0) {
                        String tag = line.substring(i, close + 1);
                        String lower = tag.toLowerCase();
                        boolean keeps = lower.startsWith("<c3") || lower.startsWith("<c2")
                                || lower.equals("</c3>") || lower.equals("</c2>")
                                || lower.equals("<ac>") || lower.equals("</ac>");
                        if (keeps) {
                            current.append(tag);
                        }
                        if (lower.startsWith("<c3") || lower.startsWith("<c2")) {
                            active = tag;
                        } else if (lower.equals("</c3>") || lower.equals("</c2>")) {
                            active = null;
                        }
                        i = close + 1;
                        continue;
                    }
                }
                int cp = line.codePointAt(i);
                int w = charWidth(cp);
                if (width + w > units && current.length() > 0) {
                    wrapped.add(current.toString());
                    current = new StringBuilder();
                    if (active != null) {
                        current.append(active);
                    }
                    width = 0;
                }
                current.appendCodePoint(cp);
                width += w;
                i += Character.charCount(cp);
            }
            wrapped.add(current.toString());
        }
        lines.clear();
        lines.addAll(wrapped);
    }

    /** Half-width units of one code point for the project's monospace font. */
    public static int charWidth(int codePoint) {
        return isWide(codePoint) ? 2 : 1;
    }

    private static boolean isWide(int cp) {
        return (cp >= 0x1100 && cp <= 0x115F)
                || (cp >= 0x2E80 && cp <= 0x303E)
                || (cp >= 0x3041 && cp <= 0x33FF)
                || (cp >= 0x3400 && cp <= 0x4DBF)
                || (cp >= 0x4E00 && cp <= 0x9FFF)
                || (cp >= 0xA000 && cp <= 0xA4CF)
                || (cp >= 0xAC00 && cp <= 0xD7A3)
                || (cp >= 0xF900 && cp <= 0xFAFF)
                || (cp >= 0xFE30 && cp <= 0xFE4F)
                || (cp >= 0xFF00 && cp <= 0xFF60)
                || (cp >= 0xFFE0 && cp <= 0xFFE6)
                || (cp >= 0x20000 && cp <= 0x3FFFD);
    }

    /** Strips every control code and tag the window cannot draw. */
    public static String clean(String raw, GameState state) {
        return clean(raw, state, playerName(state));
    }

    private static String clean(String raw, GameState state, String playerName) {
        String text = VARIABLE.matcher(raw)
                .replaceAll(match -> Matcher.quoteReplacement(variableValue(state, match.group(1))));
        text = replaceName(text, playerName);
        text = SKIN.matcher(text).replaceAll("");
        text = SPEAKER.matcher(text).replaceAll("");
        text = LINE_COUNT.matcher(text).replaceAll("");
        text = BLUE.matcher(text).replaceAll(Matcher.quoteReplacement(BLUE_TAG));
        text = RED.matcher(text).replaceAll(Matcher.quoteReplacement(RED_TAG));
        text = stripCodesAndTags(text);
        return text;
    }

    /** Removes control codes, timing codes and un-drawable html tags. */
    private static String stripCodesAndTags(String text) {
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char ch = text.charAt(i);
            if (ch == '\u0001') {
                // \1: the quarter-second pause of pbMessageDisplay
                // (Messages:1028); the window draws the whole page at once.
                i++;
                continue;
            }
            if (ch == '\\') {
                int next = i + 1;
                if (next < text.length() && (text.charAt(next) == '!' || text.charAt(next) == '.'
                        || text.charAt(next) == '|' || text.charAt(next) == '^'
                        || text.charAt(next) == '1')) {
                    i += 2;
                    continue;
                }
                int end = next;
                while (end < text.length() && isAsciiLetter(text.charAt(end))) {
                    end++;
                }
                if (end > next) {
                    if (end < text.length() && text.charAt(end) == '[') {
                        int close = text.indexOf(']', end);
                        if (close > 0) {
                            end = close + 1;
                        }
                    }
                    if (text.regionMatches(true, next, "c[", 0, 2)) {
                        // \c[n] is kept as it is.
                        out.append(text, i, end);
                        i = end;
                        continue;
                    }
                    if (end == next + 1 && (text.charAt(next) == 'n' || text.charAt(next) == 'N')) {
                        // Literal \n is the line break the parser splits on.
                        out.append('\n');
                        i = end;
                        continue;
                    }
                    i = end;
                    continue;
                }
                i++;
                continue;
            }
            if (ch == '<') {
                int close = text.indexOf('>', i);
                if (close > 0 && close - i <= 32) {
                    String tag = text.substring(i, close + 1);
                    String lower = tag.toLowerCase();
                    if (lower.startsWith("<c3") || lower.startsWith("<c2")
                            || lower.equals("</c3>") || lower.equals("</c2>")
                            || lower.equals("<ac>") || lower.equals("</ac>")) {
                        out.append(tag);
                    }
                    i = close + 1;
                    continue;
                }
            }
            out.append(ch);
            i++;
        }
        return out.toString();
    }

    /** Control codes are ASCII; CJK text is not part of a code. */
    private static boolean isAsciiLetter(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
    }

    private static String replaceName(String text, String playerName) {
        return PLAYER_NAME.matcher(text).replaceAll(Matcher.quoteReplacement(playerName));
    }

    private static String playerName(GameState state) {
        String name = state == null ? null : state.playerName();
        return name == null || name.isEmpty() ? "训练家" : name;
    }

    /** \v[n] resolves through the state; without a state it reads as 0. */
    private static String variableValue(GameState state, String id) {
        return state == null ? "0" : state.variables().text(Integer.parseInt(id));
    }
}
