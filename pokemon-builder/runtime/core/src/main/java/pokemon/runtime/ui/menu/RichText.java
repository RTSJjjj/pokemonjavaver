package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.ui.MessagePalette;

/**
 * The text codes of 070_DrawText that a {@link MenuFont} window draws: {@code \c[n]}, {@code <c3=B,S>} / {@code </c3>},
 * {@code <c2=...>} / {@code </c2>} and {@code <icon=X>} (a picture of Graphics/Icons in the line); {@code <ac>} and
 * other tags are dropped. A colour carries over line breaks, like the source renderer. An unfinished code at the end
 * of a revealed prefix draws nothing, so a letter-by-letter window never shows half a tag.
 */
public final class RichText {

    private RichText() { }

    /**
     * Draws {@code text} line by line, the first line at {@code firstY} (libGDX y of the line top) and each next one
     * {@code lineStep} lower.
     *
     * @param dark whether the window is dark (light text): {@code \c[n]} reverses its palette there
     */
    public static void drawLines(SpriteBatch batch, MenuFont font, MenuAssets assets, String text, float x, float firstY,
                                 float lineStep, int maxLines, Color main, Color shadow, boolean dark) {
        if (text == null || text.isEmpty()) {
            return;
        }
        MessagePalette palette = new MessagePalette(dark);
        Color base = main;
        Color shade = shadow;
        int line = 0;
        float cx = x;
        StringBuilder run = new StringBuilder();
        int i = 0;
        while (i < text.length() && line < maxLines) {
            char ch = text.charAt(i);
            float y = firstY - line * lineStep;
            if (ch == '\n') {
                font.draw(batch, run.toString(), cx, y, base, shade);
                run.setLength(0);
                line++;
                cx = x;
                i++;
                continue;
            }
            int end = codeEnd(text, i);
            if (end != i) {
                cx += flush(batch, font, run, cx, y, base, shade);
                if (end < 0) {
                    break;                                          // an unfinished code: the rest is not revealed yet
                }
                String code = text.substring(i, end);
                String lower = code.toLowerCase();
                if (lower.startsWith("\\c[")) {
                    int[] color = palette.color(parseIndex(code.substring(3, code.length() - 1)));
                    boolean reset = color[0] == palette.base && color[1] == palette.shadow;
                    base = reset ? main : rgb(color[0]);
                    shade = reset ? shadow : rgb(color[1]);
                } else if (lower.startsWith("<c3=")) {
                    String[] parts = code.substring(4, code.length() - 1).split(",", -1);
                    base = rgb(MessagePalette.parseRgb32(parts.length > 0 ? parts[0] : null, toRgb(base)));
                    shade = rgb(MessagePalette.parseRgb32(parts.length > 1 ? parts[1] : null, toRgb(shade)));
                } else if (lower.startsWith("<c2=")) {
                    int[] color = MessagePalette.parseRgb16Pair(code.substring(4, code.length() - 1), toRgb(base), toRgb(shade));
                    base = rgb(color[0]);
                    shade = rgb(color[1]);
                } else if (lower.equals("</c3>") || lower.equals("</c2>")) {
                    base = main;
                    shade = shadow;
                } else if (lower.startsWith("<icon=") && assets != null) {   // 070_DrawText:604-610
                    Texture icon = assets.icon(code.substring(6, code.length() - 1).trim());
                    if (icon != null) {
                        batch.draw(icon, cx, y - icon.getHeight() + 6f);
                        cx += icon.getWidth();
                    }
                }
                i = end;
                continue;
            }
            run.append(ch);
            i++;
        }
        if (line < maxLines) {
            font.draw(batch, run.toString(), cx, firstY - line * lineStep, base, shade);
        }
    }

    /** A letter-by-letter window's next reveal position: {@code step} more characters, the codes on the way skipped. */
    public static int advance(String text, int from, int step) {
        int at = Math.max(0, from);
        for (int k = 0; k < step && at < text.length(); k++) {
            at = skipCodes(text, at);
            if (at < text.length()) at++;
        }
        return skipCodes(text, Math.min(at, text.length()));
    }

    /** The first position at or after {@code at} that does not start a code. */
    public static int skipCodes(String text, int at) {
        while (at < text.length()) {
            int end = codeEnd(text, at);
            if (end <= at) break;
            at = end;
        }
        return at;
    }

    /** The text without its codes (for measuring and tests). */
    public static String plain(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            int end = codeEnd(text, i);
            if (end < 0) {
                break;
            }
            if (end != i) {
                i = end;
                continue;
            }
            out.append(text.charAt(i++));
        }
        return out.toString();
    }

    /**
     * The end of the code that starts at {@code i}: {@code i} when no code starts there, -1 when one starts but is not
     * finished yet.
     */
    static int codeEnd(String text, int i) {
        char ch = text.charAt(i);
        if (ch == '\\' && i + 1 < text.length() && (text.charAt(i + 1) == 'c' || text.charAt(i + 1) == 'C')) {
            if (i + 2 >= text.length()) return -1;
            if (text.charAt(i + 2) != '[') return i;
            int close = text.indexOf(']', i);
            return close < 0 ? -1 : close + 1;
        }
        if (ch == '\\' && i + 1 == text.length()) {
            return -1;
        }
        if (ch == '<') {
            int close = text.indexOf('>', i);
            String head = text.substring(i, close < 0 ? Math.min(text.length(), i + 6) : close + 1).toLowerCase();
            boolean tag = head.startsWith("<c3") || head.startsWith("<c2") || head.startsWith("</c")
                    || head.startsWith("<icon") || head.startsWith("<ac") || head.startsWith("</ac");
            if (close < 0) {
                // A prefix of a tag ("<", "<ic", "<c3=FF") is an unfinished code; anything else is plain text.
                String rest = text.substring(i).toLowerCase();
                for (String start : new String[] {"<c3", "<c2", "</c", "<icon", "<ac", "</ac"}) {
                    if (start.startsWith(rest) || rest.startsWith(start)) return -1;
                }
                return i;
            }
            return tag ? close + 1 : i;
        }
        return i;
    }

    private static float flush(SpriteBatch batch, MenuFont font, StringBuilder run, float x, float y, Color base, Color shade) {
        if (run.length() == 0) {
            return 0f;
        }
        String part = run.toString();
        run.setLength(0);
        font.draw(batch, part, x, y, base, shade);
        return advance(font, part);
    }

    /** The pen advance of {@code text}: the layout width leaves out the last glyph's spacing, so the next run would touch it. */
    private static float advance(MenuFont font, String text) {
        com.badlogic.gdx.graphics.g2d.BitmapFont bitmap = font.font();
        if (bitmap == null) {
            return font.width(text);
        }
        com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData data = bitmap.getData();
        float sum = 0f;
        for (int i = 0; i < text.length(); i++) {
            com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph glyph = data.getGlyph(text.charAt(i));
            if (glyph == null) {
                return font.width(text);
            }
            sum += glyph.xadvance;
        }
        return sum * data.scaleX;
    }

    private static int parseIndex(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    private static Color rgb(int value) {
        return new Color((value >>> 16 & 0xff) / 255f, (value >>> 8 & 0xff) / 255f, (value & 0xff) / 255f, 1f);
    }

    private static int toRgb(Color color) {
        return (Math.round(color.r * 255f) << 16) | (Math.round(color.g * 255f) << 8) | Math.round(color.b * 255f);
    }
}
