package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.pokemon.PbsData;

import java.util.ArrayList;
import java.util.List;

/**
 * 337_003_ESMM_Main {@code ESMiniMap} with the constants of 335_001_ESMM_Config: a crop of the player's region map in a corner of
 * the screen, the map's name, the player's tile and the player's head on the player's square of the region map. It is only shown
 * outside {@code BAN_MAPS}, with the Town Map in the bag and when the option is on.
 *
 * <p>登记: {@code MetadataMapSize} (a map spread over several squares of the region map) is not in this project's PBS, so the
 * player's square is always the map's own.</p>
 */
public final class MiniMap {

    /** ESMM_Config::BAN_MAPS. */
    private static final int[] BAN_MAPS = {
            1, 119, 224, 292, 293, 294, 295, 296, 347, 60, 209, 210, 228, 297, 321, 371, 140, 441, 442, 444,
            417, 440, 443, 445, 446, 447, 448, 450, 451, 478, 485, 388, 462};
    /** ESMM_Config::MAP_SIZE / SQUARE / MAP_ZOOM / BORDER_COLOR. */
    public static final int[] SIZES = {5, 7, 9, 11};
    private static final int SQUARE = 16;
    public static final float[] ZOOMS = {0.55f, 1.0f, 1.5f, 2.0f};
    private static final int[][] BORDER_COLORS = {{0, 0, 0}, {255, 255, 255}, {255, 0, 0}, {0, 255, 0}, {0, 0, 255}, {255, 255, 0}};
    private static final int TINY_FONT = 16;                                            // pbSetTinyFont

    private static final Color FONT_COLOR = new Color(248f / 255f, 248f / 255f, 248f / 255f, 1f);
    private static final Color SHADOW_COLOR = new Color(64f / 255f, 64f / 255f, 64f / 255f, 1f);

    private final RuntimeContext context;

    public MiniMap(RuntimeContext context) {
        this.context = context;
    }

    public static boolean banned(int mapId) {
        for (int ban : BAN_MAPS) {
            if (ban == mapId) return true;
        }
        return false;
    }

    /** {@code visible?} (:111-118). */
    public boolean visible(int mapId) {
        return !banned(mapId)
                && context.gameState().inventory().count("TOWNMAP") > 0
                && context.settings().showMiniMap == 0;
    }

    /** The bottom positions ({@code miniMap_position[1] != 0}) are hidden while a message is shown (338:15-28). */
    public boolean atBottom() {
        return (context.settings().miniMapPosition & 2) != 0;
    }

    /**
     * Draws the mini map in screen coordinates (y up, the batch is between begin and end).
     *
     * @param mapName  the current map's name ({@code $game_map.name})
     * @param outdoor  the map is outdoors: the day / night tone is applied to the picture and the head
     * @param toneOn   switches the batch to the day / night tone (and {@code toneOff} back)
     */
    public void render(SpriteBatch b, MenuAssets assets, MenuFont tiny, int mapId, int playerX, int playerY, String mapName,
                       boolean outdoor, Runnable toneOn, Runnable toneOff) {
        GameSettings settings = context.settings();
        PbsData pbs = context.pbsData();
        PbsData.TownMap data = pbs == null ? null : pbs.townMap;
        int[] position = mapPosition(mapId);
        int currentRegion = context.gameState().trainer().region;
        TownMapModel model = new TownMapModel(data, currentRegion, true, position, id -> false);   // init_map_data (:164-190)
        PbsData.TownMapRegion region = model.currentRegion();
        if (region == null) {
            return;                                                                    // pbMessage("找不到地图数据"): nothing to show
        }
        float screenW = ScreenMetrics.logicalWidth();
        float screenH = ScreenMetrics.logicalHeight();
        int mapSize = SIZES[settings.miniMapSize];
        float zoom = ZOOMS[settings.miniMapZoom];
        float bltSize = mapSize / zoom;                                                // @blt_map_size
        int[] border = BORDER_COLORS[settings.miniMapBorder];
        float opacity = (int) (settings.miniMapOpacity * 2.55f) / 255f;               // update_opacity
        float[][] corners = {{0f, 0f}, {screenW, 0f}, {0f, screenH}, {screenW, screenH}};
        float[] corner = corners[settings.miniMapPosition];
        int borderSize = mapSize * SQUARE + 4;
        float borderX = corner[0] == 0f ? 0f : corner[0] - mapSize * SQUARE - 4;
        float borderY = corner[1] == 0f ? 0f : corner[1] - mapSize * SQUARE - 4;

        b.setColor(border[0] / 255f, border[1] / 255f, border[2] / 255f, opacity / 2f);
        b.draw(assets.pixel(), borderX, screenH - borderY - borderSize, borderSize, borderSize);

        float startX = model.mapX() - (bltSize - 1f) / 2f;                             // :63-70
        float startY = model.mapY() - (bltSize - 1f) / 2f;
        float startTileX = Math.min(Math.max(startX, 0f), Math.max(30f - bltSize, 0f));
        float startTileY = Math.min(Math.max(startY, 0f), Math.max(20f - bltSize, 0f));
        int offsetX = (int) (startTileX * SQUARE);
        int offsetY = (int) (startTileY * SQUARE);
        float mapX = borderX + 2f;
        float mapY = borderY + 2f;
        Texture picture = assets.graphic("Pictures", stripExtension(region.filename));
        if (toneOn != null && outdoor) toneOn.run();
        if (picture != null) {
            int srcSize = (int) (bltSize * SQUARE);
            int srcW = Math.max(0, Math.min(srcSize, picture.getWidth() - offsetX));    // Bitmap#blt clips to the picture
            int srcH = Math.max(0, Math.min(srcSize, picture.getHeight() - offsetY));
            b.setColor(1f, 1f, 1f, opacity);
            b.draw(picture, mapX, screenH - mapY - srcH * zoom, srcW * zoom, srcH * zoom, offsetX, offsetY, srcW, srcH, false, false);
        }
        Texture head = TownMapView.playerHeadOf(context, assets);
        if (head != null) {                                                            // :112-118
            float headX = mapX + bltSize * zoom / 2f * SQUARE + (startX - startTileX) * SQUARE * zoom - head.getWidth() / 2 * zoom;
            float headY = mapY + bltSize * zoom / 2f * SQUARE + (startY - startTileY) * SQUARE * zoom - head.getHeight() / 2 * zoom;
            b.setColor(1f, 1f, 1f, opacity);
            b.draw(head, headX, screenH - headY - head.getHeight() * zoom, head.getWidth() * zoom, head.getHeight() * zoom);
        }
        if (toneOff != null && outdoor) toneOff.run();

        if (mapSize >= 5) {                                                            // update_visible: text when @map_size >= 5
            drawText(b, tiny, mapName, mapSize, corner, borderSize, screenH, playerX, playerY, opacity);
        }
        b.setColor(Color.WHITE);
    }

    private void drawText(SpriteBatch b, MenuFont tiny, String mapName, int mapSize, float[] corner, int borderSize, float screenH,
                          int playerX, int playerY, float opacity) {
        float width = mapSize * SQUARE;
        float textX = corner[0] == 0f ? 2f : corner[0] - borderSize + 2f;
        float textY = corner[1] == 0f ? 2f : corner[1] - borderSize + 2f;
        Color main = new Color(FONT_COLOR.r, FONT_COLOR.g, FONT_COLOR.b, opacity);
        Color shadow = new Color(SHADOW_COLOR.r, SHADOW_COLOR.g, SHADOW_COLOR.b, opacity);
        List<String> lines = split(mapName == null || mapName.isEmpty() ? "???" : mapName, width);
        for (int i = 0; i < lines.size(); i++) {
            draw(b, tiny, lines.get(i), textX + width / 2f, textY + TINY_FONT * i, 2, screenH, main, shadow);
        }
        if (mapSize > SIZES[0]) {
            draw(b, tiny, "[M]键", textX + width, textY + width - TINY_FONT, 1, screenH, main, shadow);
        }
        draw(b, tiny, Math.max(playerX, 0) + "," + Math.max(playerY, 0), textX + 2f, textY + width - TINY_FONT, 0, screenH, main, shadow);
    }

    /** {@code pbDrawTextPositions}: align 1 right, 2 centred; y is the top of the text. */
    private static void draw(SpriteBatch b, MenuFont f, String text, float x, float top, int align, float screenH, Color main, Color shadow) {
        float left = x;
        if (align == 1) left = x - f.width(text);
        else if (align == 2) left = x - f.width(text) / 2f;
        f.draw(b, text, left, screenH - top, main, shadow);
    }

    /** {@code split_str} (:213-226): lines of as many characters as fit the width minus 4 pixels. */
    static List<String> split(String text, float width) {
        int length = text.length();
        for (int i = 0; i < text.length(); i++) {
            if ((float) TINY_FONT * i >= width - 4f) {
                length = i;
                break;
            }
        }
        List<String> lines = new ArrayList<>();
        if (length <= 0) {
            lines.add(text);
            return lines;
        }
        for (int i = 0; i < text.length(); i += length) {
            lines.add(text.substring(i, Math.min(text.length(), i + length)));
        }
        return lines;
    }

    /** {@code pbGetMetadata($game_map.map_id, MetadataMapPosition)}, as the region map reads it. */
    private int[] mapPosition(int mapId) {
        pokemon.runtime.data.MapData map = context.database() == null ? null : context.database().map(mapId);
        return map == null ? null : map.mapPosition;
    }

    private static String stripExtension(String filename) {
        if (filename == null || filename.isEmpty()) return "";
        return filename.endsWith(".png") ? filename.substring(0, filename.length() - 4) : filename;
    }
}
