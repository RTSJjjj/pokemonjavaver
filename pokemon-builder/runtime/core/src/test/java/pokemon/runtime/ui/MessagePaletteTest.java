package pokemon.runtime.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R6.31: the fixed Essentials palette and the skin-dependent defaults
 * ({@code getSkinColor} / {@code getDefaultTextColors}).
 */
class MessagePaletteTest {

    @Test
    @DisplayName("default text colour follows the skin darkness")
    void defaults() {
        MessagePalette light = new MessagePalette(false);
        assertEquals(0x505058, light.base);
        assertEquals(0xA0A0A8, light.shadow);
        MessagePalette dark = new MessagePalette(true);
        assertEquals(0xF8F8F8, dark.base);
        assertEquals(0x485058, dark.shadow);
    }

    @Test
    @DisplayName("\\c[n] uses the fixed palette, reversed on dark skins")
    void coloured() {
        MessagePalette light = new MessagePalette(false);
        assertArrayEquals(new int[] {0xE82010, 0xF8A8B8}, light.color(2));
        assertArrayEquals(new int[] {0x60B048, 0xB0D090}, light.color(3));
        assertArrayEquals(new int[] {light.base, light.shadow}, light.color(0));
        assertArrayEquals(new int[] {light.base, light.shadow}, light.color(99));

        MessagePalette dark = new MessagePalette(true);
        assertArrayEquals(new int[] {0xF8A8B8, 0xE82010}, dark.color(2));
    }

    @Test
    @DisplayName("\\b / \\r decode the plugin's 16-bit BGR pairs")
    void pluginColours() {
        assertArrayEquals(new int[] {0x3050C8, 0xD0D0C8}, MessagePalette.blue());
        assertArrayEquals(new int[] {0xE00808, 0xD0D0C8}, MessagePalette.red());
    }

    @Test
    @DisplayName("<c3=RRGGBB,RRGGBB> keeps the current colour when a part is empty")
    void rgb32() {
        assertEquals(0x112233, MessagePalette.parseRgb32("112233", 0));
        assertEquals(0x445566, MessagePalette.parseRgb32("445566", 0));
        assertEquals(7, MessagePalette.parseRgb32("", 7));
        assertEquals(7, MessagePalette.parseRgb32(null, 7));
    }
}
