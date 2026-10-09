package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 311_BW_SignPosts / 312_BW_SignPosts_Config. */
class LocationSignpostTest {
    private static LocationSignpost board(String name) {
        return new LocationSignpost(name, 4, 66f, 50f, 448f);
    }

    @Test
    @DisplayName("the board follows the words of the name; later lists win, routes last")
    void boards() {
        assertEquals("town", board("茶月镇").board());
        assertEquals("city", board("苍穹市").board());
        assertEquals("cave", board("幽暗洞窟").board());
        assertEquals("none", board("研究所").board());
        assertEquals("water", board("镜湖").board());
        assertEquals("route", board("12号道路").board());
        assertEquals("route_extended", board("101号道路").board());
    }

    @Test
    @DisplayName("a route's number is drawn least significant digit first, as the plugin does")
    void routeNumber() {
        assertEquals("12", board("12号道路").routeNumber());
        assertArrayEquals(new int[] {2, 1}, LocationSignpost.routeDigits("12"));
        assertEquals(10f, board("12号道路").routeNumberX());
        assertEquals(20f, board("3号道路").routeNumberX());
        assertEquals(14f, board("101号道路").routeNumberX());
        assertNull(board("茶月镇").routeNumber());
    }

    @Test
    @DisplayName("the board slides down for 10 frames, rests, slides up on frames 90-100 and is gone after 101")
    void slide() {
        LocationSignpost sign = board("茶月镇");
        assertEquals(-70f, sign.windowY(), 1e-3);
        for (int i = 0; i < 10; i++) sign.tick();
        assertEquals(-3.9f, sign.windowY(), 1e-2);
        assertEquals(448f - 50f, sign.seasonY(), 1e-3);
        for (int i = 10; i < 90; i++) sign.tick();
        assertEquals(-3.9f, sign.windowY(), 1e-2);
        for (int i = 90; i < 103; i++) sign.tick();
        assertTrue(sign.finished());
    }

    @Test
    @DisplayName("the season strip follows the month")
    void season() {
        assertEquals("Spring", new LocationSignpost("a", 3, 66f, 50f, 448f).season());
        assertEquals("Summer", new LocationSignpost("a", 7, 66f, 50f, 448f).season());
        assertEquals("Autumn", new LocationSignpost("a", 11, 66f, 50f, 448f).season());
        assertEquals("Winter", new LocationSignpost("a", 12, 66f, 50f, 448f).season());
    }
}
