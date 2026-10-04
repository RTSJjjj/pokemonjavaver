package pokemon.runtime.ui;

/**
 * R6.31: text colours of the Essentials message renderer (0060.rb
 * {@code getSkinColor} / {@code getDefaultTextColors}).
 *
 * <p>The palette is fixed; only the default text colour depends on whether the
 * windowskin is dark. {@code \c[n]} resolves through {@link #color(int)} and
 * the {@code <c2=...>} / {@code <c3=...>} tags through the static parsers.</p>
 */
public final class MessagePalette {

    /** Base + shadow for colour numbers 1..12 (the source list). */
    private static final int[] COLORS = {
            0x0070F8, 0x78B8E8, // 1  Blue
            0xE82010, 0xF8A8B8, // 2  Red
            0x60B048, 0xB0D090, // 3  Green
            0x48D8D8, 0xA8E0E0, // 4  Cyan
            0xD038B8, 0xE8A0E0, // 5  Magenta
            0xE8D020, 0xF8E888, // 6  Yellow
            0xA0A0A8, 0xD0D0D8, // 7  Grey
            0xF0F0F8, 0xC8C8D0, // 8  White
            0x9040E8, 0xB8A8E0, // 9  Purple
            0xF89818, 0xF8C898, // 10 Orange
    };
    private static final int DARK_BASE = 0x505058;
    private static final int DARK_SHADOW = 0xA0A0A8;
    private static final int LIGHT_BASE = 0xF8F8F8;
    private static final int LIGHT_SHADOW = 0x485058;

    private final boolean dark;
    public final int base;
    public final int shadow;

    public MessagePalette(boolean dark) {
        this.dark = dark;
        if (dark) {
            base = LIGHT_BASE;
            shadow = LIGHT_SHADOW;
        } else {
            base = DARK_BASE;
            shadow = DARK_SHADOW;
        }
    }

    public boolean dark() {
        return dark;
    }

    /**
     * {@code getSkinColor}: 0 and out-of-range numbers answer the skin's
     * default colour; 1..10 the fixed palette (reversed on dark skins, so
     * light text keeps a dark shadow); 11/12 the dark/light default pair.
     *
     * @return {base, shadow}
     */
    public int[] color(int index) {
        if (index == 0 || index > 12) {
            return new int[] {base, shadow};
        }
        if (index == 11) {
            return dark
                    ? new int[] {DARK_SHADOW, DARK_BASE}
                    : new int[] {DARK_BASE, DARK_SHADOW};
        }
        if (index == 12) {
            return new int[] {LIGHT_BASE, LIGHT_SHADOW};
        }
        int pair = (index - 1) * 2;
        if (dark) {
            return new int[] {COLORS[pair + 1], COLORS[pair]};
        }
        return new int[] {COLORS[pair], COLORS[pair + 1]};
    }

    /**
     * {@code <c3=B,S>}: both colours are 24-bit {@code RRGGBB}; an empty part
     * keeps the current colour (the caller passes what it has).
     */
    public static int parseRgb32(String hex, int fallback) {
        if (hex == null) {
            return fallback;
        }
        String text = hex.trim();
        if (text.length() != 6 && text.length() != 8) {
            return fallback;
        }
        try {
            int value = (int) Long.parseLong(text, 16);
            return text.length() == 8 ? (value >>> 8) & 0xFFFFFF : value & 0xFFFFFF;
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    /**
     * {@code <c2=XXXXYYYY>}: two 16-bit colour numbers in the source's
     * {@code R | G<<5 | B<<10} order (that is what makes {@code \b} blue).
     *
     * @return {base, shadow}
     */
    public static int[] parseRgb16Pair(String hex, int base, int shadow) {
        if (hex == null || hex.trim().length() != 8) {
            return new int[] {base, shadow};
        }
        try {
            int first = Integer.parseInt(hex.trim().substring(0, 4), 16);
            int second = Integer.parseInt(hex.trim().substring(4, 8), 16);
            return new int[] {rgb16(first), rgb16(second)};
        } catch (NumberFormatException error) {
            return new int[] {base, shadow};
        }
    }

    private static int rgb16(int value) {
        int r = (value & 0x1F) << 3;
        int g = ((value >>> 5) & 0x1F) << 3;
        int b = ((value >>> 10) & 0x1F) << 3;
        return (r << 16) | (g << 8) | b;
    }

    /** {@code \b} from the project's name-box plugin: {@code <c2=6546675A>}. */
    public static int[] blue() {
        return parseRgb16Pair("6546675A", DARK_BASE, DARK_SHADOW);
    }

    /** {@code \r} from the project's name-box plugin: {@code <c2=043C675A>}. */
    public static int[] red() {
        return parseRgb16Pair("043C675A", DARK_BASE, DARK_SHADOW);
    }
}
