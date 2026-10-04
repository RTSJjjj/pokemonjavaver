package pokemon.runtime.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless R6.4 tests for Show / Move / Erase Picture (project3 section 18):
 * interpolation and the "wait for completion" flag the interpreter uses.
 */
class PictureServiceTest {

    @Test
    @DisplayName("show stores the command values and a later show replaces the picture")
    void show() {
        PictureService pictures = new PictureService();
        pictures.show(1, "【立绘】南晓", 0, 0, 0, 100, 100, 255, 0);
        assertEquals(1, pictures.size());
        PictureService.Picture picture = pictures.get(1);
        assertEquals("【立绘】南晓", picture.name);
        assertEquals(0, picture.origin);
        assertEquals(100f, picture.zoomX);
        assertEquals(255f, picture.opacity);

        pictures.show(1, "【立绘】索伽", 1, 300, 200, 50, 50, 128, 1);
        assertEquals(1, pictures.size());
        assertEquals("【立绘】索伽", pictures.get(1).name);
        assertEquals(1, pictures.get(1).origin);
        assertEquals(128f, pictures.get(1).opacity);
    }

    @Test
    @DisplayName("move interpolates over its frame duration and lands exactly")
    void move() {
        PictureService pictures = new PictureService();
        pictures.show(2, "pic", 0, 0, 0, 100, 100, 255, 0);
        pictures.move(2, 10, 0, 100, 50, 200, 200, 128, 0); // 10 frames = 0.5 s
        PictureService.Picture picture = pictures.get(2);
        assertTrue(picture.moving());

        pictures.update(0.25f);
        assertEquals(50f, picture.x, 0.01f);
        assertEquals(150f, picture.zoomX, 0.01f);
        assertEquals(191.5f, picture.opacity, 0.01f);

        pictures.update(0.3f);
        assertFalse(picture.moving());
        assertEquals(100f, picture.x);
        assertEquals(200f, picture.zoomX);
        assertEquals(128f, picture.opacity);
    }

    @Test
    @DisplayName("a zero duration move lands immediately and erase removes the picture")
    void instantMoveAndErase() {
        PictureService pictures = new PictureService();
        pictures.show(3, "pic", 0, 10, 10, 100, 100, 255, 0);
        pictures.move(3, 0, 1, 20, 30, 100, 100, 0, 0);
        assertFalse(pictures.get(3).moving());
        assertEquals(20f, pictures.get(3).x);
        assertEquals(0f, pictures.get(3).opacity);

        pictures.erase(3);
        assertTrue(pictures.isEmpty());
    }
}
