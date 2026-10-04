package pokemon.runtime.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R11: the launcher's display options (window / fullscreen / resolution /
 * VSync) are parsed headlessly; the lwjgl3 launcher then applies them to its
 * window configuration and the map screen fits the logical resolution into
 * whatever window size results.
 */
class DisplaySettingsTest {

    @Test
    @DisplayName("the default window is the largest integer scale that fits the monitor")
    void defaults() {
        DisplaySettings settings = DisplaySettings.defaults();
        assertEquals(0, settings.windowWidth, "no explicit size: the scale is resolved at launch");
        assertEquals(ScreenMetrics.LOGICAL_WIDTH, settings.logicalWidth);
        assertFalse(settings.fullscreen);
        assertTrue(settings.vsync, "desktop builds sync to the monitor by default");
        assertFalse(settings.resizable);

        int[] onFullHd = settings.resolveWindow(1920, 1080);
        assertEquals(2 * ScreenMetrics.LOGICAL_WIDTH, onFullHd[0], "1920x1080 fits 2x");
        assertEquals(2 * ScreenMetrics.LOGICAL_HEIGHT, onFullHd[1]);
        int[] onSmall = settings.resolveWindow(1366, 768);
        assertEquals(ScreenMetrics.LOGICAL_WIDTH, onSmall[0], "a small screen keeps 1x");
        int[] unknown = settings.resolveWindow(0, 0);
        assertEquals(ScreenMetrics.LOGICAL_WIDTH, unknown[0]);
    }

    @Test
    @DisplayName("the project's screen size is the default and the window keeps its size (R6.21)")
    void projectLogicalKeepsTheWindow() {
        DisplaySettings settings = DisplaySettings.defaults().withProjectLogical(672, 448);
        assertEquals(672, settings.logicalWidth);
        assertEquals(448, settings.logicalHeight, "the render follows the project's own size");
        int[] onFullHd = settings.resolveWindow(1920, 1080);
        assertEquals(2 * ScreenMetrics.WINDOW_WIDTH, onFullHd[0], "the window keeps 672x488 x2");
        assertEquals(2 * ScreenMetrics.WINDOW_HEIGHT, onFullHd[1]);
    }

    @Test
    @DisplayName("--logical still beats the project's screen size")
    void explicitLogicalWinsOverTheProject() {
        DisplaySettings settings = DisplaySettings.parse(new String[] {"--logical", "800x600"})
                .withProjectLogical(672, 448);
        assertEquals(800, settings.logicalWidth);
        assertEquals(600, settings.logicalHeight);
        assertEquals(800, settings.resolveWindow(1920, 1080)[0],
                "an explicit logical size becomes the window unit");
    }

    @Test
    @DisplayName("--scale and --size override the automatic window scale")
    void explicitWindow() {
        DisplaySettings scaled = DisplaySettings.parse(new String[] {"--scale", "3"});
        int[] window = scaled.resolveWindow(1920, 1080);
        assertEquals(3 * ScreenMetrics.LOGICAL_WIDTH, window[0]);
        assertEquals(3 * ScreenMetrics.LOGICAL_HEIGHT, window[1]);

        DisplaySettings fixed = DisplaySettings.parse(new String[] {"--size", "800x600"});
        int[] explicit = fixed.resolveWindow(1920, 1080);
        assertEquals(800, explicit[0], "an explicit size always wins");
        assertEquals(600, explicit[1]);
    }

    @Test
    @DisplayName("fullscreen, window size, logical size and vsync flags are read")
    void flags() {
        DisplaySettings settings = DisplaySettings.parse(new String[] {
                "--fullscreen", "--size", "1280x720", "--logical", "672x448",
                "--no-vsync", "--resizable"});
        assertTrue(settings.fullscreen);
        assertEquals(1280, settings.windowWidth);
        assertEquals(720, settings.windowHeight);
        assertEquals(672, settings.logicalWidth, "the project's own resolution wins");
        assertEquals(448, settings.logicalHeight);
        assertFalse(settings.vsync);
        assertTrue(settings.resizable);
        assertTrue(settings.describe().contains("fullscreen"));
    }

    @Test
    @DisplayName("positional arguments survive the flags (data root and map id)")
    void positionals() {
        List<String> positional = DisplaySettings.positionals(new String[] {
                "E:/data/generated", "12", "--size", "960x540", "--fullscreen"});
        assertEquals(List.of("E:/data/generated", "12"), positional);
        assertTrue(DisplaySettings.positionals(new String[] {"--size", "640x480"}).isEmpty());
    }

    @Test
    @DisplayName("a malformed size is ignored instead of breaking the launch")
    void malformedSize() {
        DisplaySettings settings = DisplaySettings.parse(new String[] {"--size", "wide"});
        int[] window = settings.resolveWindow(1920, 1080);
        assertEquals(0, window[0] % ScreenMetrics.LOGICAL_WIDTH,
                "falls back to an integer scale of the logical size");
        assertEquals(0, window[1] % ScreenMetrics.LOGICAL_HEIGHT);

        DisplaySettings missing = DisplaySettings.parse(new String[] {"--size"});
        assertEquals(ScreenMetrics.LOGICAL_HEIGHT, missing.resolveWindow(0, 0)[1],
                "an unknown display keeps 1x");
        assertEquals(ScreenMetrics.LOGICAL_WIDTH, missing.resolveWindow(0, 0)[0]);
    }
}
