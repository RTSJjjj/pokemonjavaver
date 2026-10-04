package pokemon.runtime.app;

/**
 * Desktop display options of the launcher (project3 section 38): window mode,
 * fullscreen, resolution and VSync. Pure Java so the parsing is testable
 * without a window; the lwjgl3 launcher applies the result to its
 * {@code Lwjgl3ApplicationConfiguration}.
 *
 * <p>Flags: {@code --fullscreen} / {@code --windowed}, {@code --size 1280x720},
 * {@code --logical 672x448}, {@code --vsync} / {@code --no-vsync},
 * {@code --resizable} / {@code --fixed}.</p>
 */
public final class DisplaySettings {

    public final int windowWidth;
    public final int windowHeight;
    /** 0 = pick the largest integer scale that fits the monitor. */
    public final int scale;
    public final boolean fullscreen;
    public final boolean vsync;
    public final boolean resizable;
    public final int logicalWidth;
    public final int logicalHeight;
    /** True when {@code --logical} was given; the project size then yields. */
    public final boolean logicalExplicit;

    private DisplaySettings(int windowWidth, int windowHeight, boolean fullscreen, boolean vsync,
                            boolean resizable, int scale, int logicalWidth, int logicalHeight,
                            boolean logicalExplicit) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.fullscreen = fullscreen;
        this.vsync = vsync;
        this.resizable = resizable;
        this.scale = scale;
        this.logicalWidth = logicalWidth;
        this.logicalHeight = logicalHeight;
        this.logicalExplicit = logicalExplicit;
    }

    public static DisplaySettings defaults() {
        return parse(new String[0]);
    }

    /** Parses the launcher flags; unknown arguments are ignored on purpose. */
    public static DisplaySettings parse(String[] args) {
        int width = ScreenMetrics.LOGICAL_WIDTH;
        int height = ScreenMetrics.LOGICAL_HEIGHT;
        boolean explicitSize = false;
        int scale = 0;
        int logicalWidth = ScreenMetrics.LOGICAL_WIDTH;
        int logicalHeight = ScreenMetrics.LOGICAL_HEIGHT;
        boolean logicalExplicit = false;
        boolean fullscreen = false;
        boolean vsync = true;
        boolean resizable = false;
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                String argument = args[i];
                switch (argument) {
                    case "--fullscreen":
                        fullscreen = true;
                        break;
                    case "--windowed":
                        fullscreen = false;
                        break;
                    case "--vsync":
                        vsync = true;
                        break;
                    case "--no-vsync":
                        vsync = false;
                        break;
                    case "--resizable":
                        resizable = true;
                        break;
                    case "--fixed":
                        resizable = false;
                        break;
                    case "--size":
                        int[] window = size(args, i);
                        if (window != null) {
                            width = window[0];
                            height = window[1];
                            explicitSize = true;
                            i++;
                        }
                        break;
                    case "--scale":
                        int requested = value(args, i);
                        if (requested > 0) {
                            scale = Math.min(6, requested);
                            i++;
                        }
                        break;
                    case "--logical":
                        int[] logical = size(args, i);
                        if (logical != null) {
                            logicalWidth = logical[0];
                            logicalHeight = logical[1];
                            logicalExplicit = true;
                            i++;
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return new DisplaySettings(explicitSize ? width : 0, explicitSize ? height : 0,
                fullscreen, vsync, resizable, scale, logicalWidth, logicalHeight, logicalExplicit);
    }

    /**
     * R6.21: applies the source project's own screen size (project.json runtime
     * profile) as the render resolution, unless the user passed
     * {@code --logical}. The window keeps its own 672x488 scale unit, so it
     * opens at exactly the size it did before and the project's framing is
     * letterboxed inside.
     */
    public DisplaySettings withProjectLogical(int width, int height) {
        if (logicalExplicit || width <= 0 || height <= 0) {
            return this;
        }
        return new DisplaySettings(windowWidth, windowHeight, fullscreen, vsync, resizable,
                scale, width, height, true);
    }

    /**
     * The window size to open: an explicit {@code --size}, {@code --scale N}, or
     * else the largest integer scale of the logical resolution that fits the
     * monitor (pixel art stays crisp, and a 672x488 game no longer opens in a
     * tiny 672x488 window on a big screen).
     */
    public int[] resolveWindow(int displayWidth, int displayHeight) {
        if (windowWidth > 0 && windowHeight > 0) {
            return new int[] {windowWidth, windowHeight};
        }
        int wanted = scale > 0 ? scale : autoScale(displayWidth, displayHeight);
        return new int[] {windowUnitWidth() * wanted, windowUnitHeight() * wanted};
    }

    /** Largest integer scale that fits, leaving room for window borders/taskbar. */
    int autoScale(int displayWidth, int displayHeight) {
        if (displayWidth <= 0 || displayHeight <= 0) {
            return 1;
        }
        int byWidth = Math.max(1, (displayWidth - 40) / windowUnitWidth());
        int byHeight = Math.max(1, (displayHeight - 80) / windowUnitHeight());
        return Math.max(1, Math.min(4, Math.min(byWidth, byHeight)));
    }

    /** R6.21: the window never opens smaller than its historical 672x488 unit. */
    private int windowUnitWidth() {
        return Math.max(logicalWidth, ScreenMetrics.WINDOW_WIDTH);
    }

    private int windowUnitHeight() {
        return Math.max(logicalHeight, ScreenMetrics.WINDOW_HEIGHT);
    }

    /** Reads "1280x720" (also accepts 1280X720 / 1280*720). */
    private static int[] size(String[] args, int index) {
        if (index + 1 >= args.length) {
            return null;
        }
        String[] parts = args[index + 1].split("[xX*]");
        if (parts.length != 2) {
            return null;
        }
        try {
            int width = Integer.parseInt(parts[0].trim());
            int height = Integer.parseInt(parts[1].trim());
            return width > 0 && height > 0 ? new int[] {width, height} : null;
        } catch (NumberFormatException error) {
            return null;
        }
    }

    /** Reads a single integer flag value ("--scale 3"). */
    private static int value(String[] args, int index) {
        if (index + 1 >= args.length) {
            return -1;
        }
        try {
            return Integer.parseInt(args[index + 1].trim());
        } catch (NumberFormatException error) {
            return -1;
        }
    }

    public String describe() {
        return (fullscreen ? "fullscreen"
                : (windowWidth > 0 ? windowWidth + "x" + windowHeight
                : "scale " + (scale > 0 ? scale : "auto")))
                + " logical " + logicalWidth + "x" + logicalHeight
                + " vsync=" + vsync + (resizable ? " resizable" : "");
    }

    /**
     * The launcher's positional arguments ({@code [dataRoot] [mapId]}), with the
     * flags and their values removed.
     */
    public static java.util.List<String> positionals(String[] args) {
        java.util.List<String> positional = new java.util.ArrayList<>();
        if (args == null) {
            return positional;
        }
        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            if (argument == null || argument.startsWith("--")) {
                if ("--size".equals(argument) || "--logical".equals(argument)) {
                    i++; // skip the flag's value
                }
                continue;
            }
            positional.add(argument);
        }
        return positional;
    }
}
