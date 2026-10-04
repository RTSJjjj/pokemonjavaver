package pokemon.runtime.map;
import com.badlogic.gdx.math.Rectangle;

/** RMXP: 0..47 blank, 48..383 seven autotiles, 384+ the eight-column tileset. */
public final class TilesetGeometry {
    public static final int TILE_SIZE = 32, COLUMNS = 8, AUTOTILE_COUNT = 48, NORMAL_START = 384;
    // One-based 16px cells in a 6-column, 8-row frame; TL, TR, BL, BR.
    private static final int[][] QUARTERS = {
        {27,28,33,34},{5,28,33,34},{27,6,33,34},{5,6,33,34},
        {27,28,33,12},{5,28,33,12},{27,6,33,12},{5,6,33,12},
        {27,28,11,34},{5,28,11,34},{27,6,11,34},{5,6,11,34},
        {27,28,11,12},{5,28,11,12},{27,6,11,12},{5,6,11,12},
        {25,26,31,32},{25,6,31,32},{25,26,31,12},{25,6,31,12},
        {15,16,21,22},{15,16,21,12},{15,16,11,22},{15,16,11,12},
        {29,30,35,36},{29,30,11,36},{5,30,35,36},{5,30,11,36},
        {39,40,45,46},{5,40,45,46},{39,6,45,46},{5,6,45,46},
        {25,30,31,36},{15,16,45,46},{13,14,19,20},{13,14,19,12},
        {17,18,23,24},{17,18,11,24},{41,42,47,48},{5,42,47,48},
        {37,38,43,44},{37,6,43,44},{13,18,19,24},{13,14,43,44},
        {37,42,43,48},{17,18,47,48},{13,18,43,48},{1,2,7,8}
    };
    private final int normalTileRows;
    public TilesetGeometry(int normalTileRows) { this.normalTileRows = normalTileRows; }
    public boolean isAutotile(int id) { return id >= 48 && id < NORMAL_START; }
    public int autotileIndex(int id) { return isAutotile(id) ? id / 48 - 1 : -1; }
    public int normalX(int id) { return (id - NORMAL_START) % COLUMNS * TILE_SIZE; }
    public int normalY(int id) { return (id - NORMAL_START) / COLUMNS * TILE_SIZE; }
    public boolean isNormal(int id) {
        return id >= NORMAL_START && (id - NORMAL_START) / COLUMNS < normalTileRows;
    }
    public Rectangle normalTileRect(int id) {
        return isNormal(id) ? new Rectangle(normalX(id), normalY(id), TILE_SIZE, TILE_SIZE) : null;
    }
    public int quarterX(int id, int quarter) { return (QUARTERS[id % 48][quarter] - 1) % 6 * 16; }
    public int quarterY(int id, int quarter) { return (QUARTERS[id % 48][quarter] - 1) / 6 * 16; }
    public int frameWidth(int imageHeight) { return imageHeight == 32 ? 32 : 96; }
    public int animationFrame(int width, int height, float elapsed) {
        return (int) (elapsed / 0.4f) % Math.max(1, width / frameWidth(height));
    }
}