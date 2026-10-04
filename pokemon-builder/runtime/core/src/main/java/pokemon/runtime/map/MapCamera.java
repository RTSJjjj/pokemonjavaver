package pokemon.runtime.map;

/**
 * Pixel camera that follows one map position and clamps to the map bounds
 * (project3 section 10). World coordinates are libGDX standard y-up with the
 * origin at the bottom-left of the map; map rows (0 = top) convert through
 * this class only. The logical resolution and viewport policy arrive with the
 * desktop runtime (R11).
 */
public final class MapCamera {

    private final int mapPixelWidth;
    private final int mapPixelHeight;
    private final int viewPixelWidth;
    private final int viewPixelHeight;
    /**
     * R6.23: the project clamps {@code display_x}/{@code display_y} only for
     * maps whose PBS/metadata.txt says {@code SnapEdges = true}. Every other
     * map follows the player past its own border (that is how Essentials map
     * connections show the neighbouring map before the crossing).
     */
    private final boolean clampToMap;

    private float centerX;
    private float centerY;

    public MapCamera(int mapWidth, int mapHeight, int viewPixelWidth, int viewPixelHeight) {
        this(mapWidth, mapHeight, viewPixelWidth, viewPixelHeight, true);
    }

    public MapCamera(int mapWidth, int mapHeight, int viewPixelWidth, int viewPixelHeight,
                     boolean clampToMap) {
        this.mapPixelWidth = mapWidth * TilesetGeometry.TILE_SIZE;
        this.mapPixelHeight = mapHeight * TilesetGeometry.TILE_SIZE;
        this.viewPixelWidth = viewPixelWidth;
        this.viewPixelHeight = viewPixelHeight;
        this.clampToMap = clampToMap;
    }

    /** Snaps the camera to a tile center given in tile coordinates. */
    public void centerOn(int column, int row) {
        centerX = column * TilesetGeometry.TILE_SIZE + TilesetGeometry.TILE_SIZE / 2f;
        centerY = mapPixelHeight - (row * TilesetGeometry.TILE_SIZE + TilesetGeometry.TILE_SIZE / 2f);
    }

    /** Follows the interpolated world position rather than jumping at tile boundaries. */
    public void centerOnPixels(float x, float y) {
        centerX = x + TilesetGeometry.TILE_SIZE / 2f;
        centerY = y + TilesetGeometry.TILE_SIZE / 2f;
    }

    /** Left edge of the visible area, in world pixels, clamped to the map. */
    public int originX() {
        int origin = Math.round(centerX - viewPixelWidth / 2f);
        if (!clampToMap) {
            return origin;
        }
        if (mapPixelWidth <= viewPixelWidth) {
            return -(viewPixelWidth - mapPixelWidth) / 2;
        }
        return Math.max(0, Math.min(origin, mapPixelWidth - viewPixelWidth));
    }

    /** Bottom edge of the visible area, in world pixels, clamped to the map. */
    public int originY() {
        int origin = Math.round(centerY - viewPixelHeight / 2f);
        if (!clampToMap) {
            return origin;
        }
        if (mapPixelHeight <= viewPixelHeight) {
            return -(viewPixelHeight - mapPixelHeight) / 2;
        }
        return Math.max(0, Math.min(origin, mapPixelHeight - viewPixelHeight));
    }

    public int viewPixelWidth() {
        return viewPixelWidth;
    }

    public int viewPixelHeight() {
        return viewPixelHeight;
    }
}
