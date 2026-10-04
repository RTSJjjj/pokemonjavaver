package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R6.22: Scroll Map (203) - this project's Game_Map moves the display by
 * 2^speed quarter-pixels per 40 fps frame (speed 4 = 5 tiles/s) and leaves the
 * view where it stopped; the camera re-centres once the player moves.
 */
class CameraScrollTest {

    @Test
    @DisplayName("speed 4 scrolls four tiles in 0.8 s and then stops")
    void scrollsAtRgssSpeed() {
        CameraScroll scroll = new CameraScroll();
        scroll.start(8, 4, 4); // up, 4 tiles
        assertEquals(0f, scroll.offsetY(), 0.001f);

        // 16 quarter-pixels per frame = 4 px per frame = 160 px/s at 40 fps.
        for (int frame = 0; frame < 32; frame++) {
            scroll.update(1f / 40f);
        }
        assertEquals(4 * TilesetGeometry.TILE_SIZE, scroll.offsetY(), 0.1f);
        assertFalse(scroll.active(), "4 tiles at 5 tiles/s = 0.8 s");
    }

    @Test
    @DisplayName("directions map onto the y-up world")
    void directions() {
        CameraScroll up = new CameraScroll();
        up.start(8, 1, 6);
        up.update(1f);
        assertTrue(up.offsetY() > 0f, "up moves the camera towards larger world y");

        CameraScroll down = new CameraScroll();
        down.start(2, 1, 6);
        down.update(1f);
        assertTrue(down.offsetY() < 0f);

        CameraScroll left = new CameraScroll();
        left.start(4, 1, 6);
        left.update(1f);
        assertTrue(left.offsetX() < 0f);

        CameraScroll right = new CameraScroll();
        right.start(6, 1, 6);
        right.update(1f);
        assertTrue(right.offsetX() > 0f);
    }

    @Test
    @DisplayName("a new scroll resets the offset, a bad command is ignored")
    void resetAndValidation() {
        CameraScroll scroll = new CameraScroll();
        scroll.start(6, 2, 4);
        for (int frame = 0; frame < 16; frame++) {
            scroll.update(1f / 40f);
        }
        assertTrue(scroll.offsetX() > 0f);
        scroll.reset();
        assertEquals(0f, scroll.offsetX(), 0.001f);
        assertFalse(scroll.active());

        scroll.start(0, 0, 4);
        assertFalse(scroll.active(), "invalid direction/distance is ignored");
    }
}
