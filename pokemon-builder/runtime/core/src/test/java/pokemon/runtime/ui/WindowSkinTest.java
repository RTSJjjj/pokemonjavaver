package pokemon.runtime.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R6.31: windowskin geometry follows SpriteWindow's rules for the three skin
 * families the project ships, and the dark/light decision mirrors
 * {@code isDarkWindowskin}.
 */
class WindowSkinTest {

    @Test
    @DisplayName("speech bw 1 (96x48): 32px side caps, 16px top/bottom, 16px body")
    void speechGeometry() {
        WindowSkin.Geometry g = WindowSkin.Geometry.of(96, 48);
        assertFalse(g.classic);
        assertEquals(32, g.startX);
        assertEquals(16, g.startY);
        assertEquals(48, g.endX);
        assertEquals(16, g.endY);
        assertEquals(32, g.backX);
        assertEquals(16, g.backY);
        assertEquals(16, g.backW);
        assertEquals(16, g.backH);
        assertEquals(32, g.trimStartX);
        assertEquals(16, g.trimStartY);
        assertEquals(80, g.borderX);
        assertEquals(32, g.borderY);
    }

    @Test
    @DisplayName("choice 1 (48x48): a plain 16px 9-slice")
    void choiceGeometry() {
        WindowSkin.Geometry g = WindowSkin.Geometry.of(48, 48);
        assertEquals(16, g.startX);
        assertEquals(16, g.startY);
        assertEquals(16, g.endX);
        assertEquals(16, g.endY);
        assertEquals(16, g.backX);
        assertEquals(16, g.backY);
        assertEquals(32, g.borderX);
        assertEquals(32, g.borderY);
    }

    @Test
    @DisplayName("sign bw (80x80): 32px caps and the 32,32 body tile")
    void signGeometry() {
        WindowSkin.Geometry g = WindowSkin.Geometry.of(80, 80);
        assertEquals(32, g.startX);
        assertEquals(32, g.startY);
        assertEquals(32, g.endX);
        assertEquals(32, g.endY);
        assertEquals(32, g.backX);
        assertEquals(32, g.backY);
        assertEquals(64, g.borderX);
        assertEquals(32, g.borderY);
    }

    @Test
    @DisplayName("RPG XP (192x128): left 128x128 back, right column frame")
    void xpGeometry() {
        WindowSkin.Geometry g = WindowSkin.Geometry.of(192, 128);
        assertTrue(g.classic);
        assertFalse(g.vx);
        assertEquals(16, g.startX);
        assertEquals(0, g.backX);
        assertEquals(0, g.backY);
        assertEquals(128, g.backW);
        assertEquals(128, g.backH);
        assertEquals(128, g.cornerX[0]);
        assertEquals(176, g.cornerX[1]);
        assertEquals(144, g.sideX[0]);
        assertEquals(32, g.borderX);
        assertEquals(32, g.borderY);
    }

    @Test
    @DisplayName("dark skins are detected from the body region / centre pixel")
    void darkness() {
        assertTrue(WindowSkin.isDarkBackground(96, 48,
                        index -> 0x000000B3, WindowSkin.Geometry.of(96, 48)),
                "the speech body tile is a dark half-transparent band");
        assertFalse(WindowSkin.isDarkBackground(48, 48,
                        index -> 0xFFFFFFFF, WindowSkin.Geometry.of(48, 48)),
                "the choice skin centre is white");
    }
}
