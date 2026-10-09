package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.data.MapData;
import pokemon.runtime.data.ProjectInfo;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.state.GameState;

/**
 * PScreen_RegionMap: the full-screen region map behind {@code pbShowMap}
 * (PScreen_RegionMap:431-437). Graphics/Pictures/mapbg covers the screen, the
 * region image sits centered (480x320 at 16px squares), the cursor and the
 * player head sit on the grid, and the bottom band shows the region name, the
 * location and the description. L/R switch regions, B closes (the active
 * 004_ESMM_Overwrite {@code pbMapScene}:52-205).
 */
public final class TownMapView {

    /**
     * Settings:201-204: {@code REGION_MAP_EXTRAS = [[region, switch, x, y,
     * file, wallmap], ...]}; an entry draws when the region matches and either
     * the wall map mode wants it or the switch is on
     * (PScreen_RegionMap:122-134).
     */
    private static final Object[][] REGION_MAP_EXTRAS = {
            { 0, 51, 16, 15, "mapHiddenBerth", false },
            { 0, 52, 20, 14, "mapHiddenFaraday", false },
    };
    /** PScreen_RegionMap:67-68. */
    private static final float SQUARE = 16f;
    /** PScreen_RegionMap:284: 8*20/40 px per 40 fps frame. */
    private static final float SCROLL_SPEED = 8f * 20f / 40f;
    /** AnimatedSprite.create("Graphics/Pictures/mapCursor",2,5) (166). */
    private static final int CURSOR_FRAMES = 2;
    private static final int CURSOR_FRAME_FRAMES = 5;
    /** MapBottomSprite text rows: 4 and 354+64 (PScreen_RegionMap:47-56). */
    private static final float TOP_TEXT_Y = 4f;
    private static final float BOTTOM_TEXT_Y = 354f + 64f;

    /** PScreen_RegionMap:156 {@code AnimatedSprite.create("Graphics/Pictures/mapFly",2,16)}. */
    private static final int FLY_FRAMES = 2;
    private static final int FLY_FRAME_FRAMES = 16;

    private final RuntimeContext context;
    private final MenuAssets assets;
    /** {@code pbStartScene(false, 1)}: choosing a place to fly to. */
    private boolean flyMode;
    private int[] flyResult;
    private final TownMapModel model;
    private final boolean wallmap;
    private final int[] playerPosition;
    private Texture mapImage;
    private float mapLeft;
    private float mapTop;
    private final Texture playerHead;
    private float elapsed;
    /** Plugin top-origin cursor position and its glide offsets (277-288). */
    private float cursorX;
    private float cursorY;
    private float offsetX;
    private float offsetY;
    private float targetX;
    private float targetY;

    public TownMapView(RuntimeContext context, MenuAssets assets,
                       int requestedRegion, boolean wallmap) {
        this.context = context;
        this.assets = assets;
        this.wallmap = wallmap;
        GameState state = context.gameState();
        PbsData pbs = context.pbsData();
        PbsData.TownMap data = pbs == null ? null : pbs.townMap;
        this.playerPosition = currentMapPosition();
        this.model = new TownMapModel(data, requestedRegion, wallmap, playerPosition,
                id -> state.switches().get(id));
        this.playerHead = loadPlayerHead();
        loadRegionImage();
        resetCursor();
    }

    public boolean hasData() {
        return model.hasData();
    }

    /** {@code PokemonRegionMapScreen#pbStartFlyScreen} (214_PScreen_RegionMap:415-420). */
    public void flyMode(boolean value) {
        this.flyMode = value;
    }

    /** The chosen healing spot [map, x, y], or null when the player backed out. */
    public int[] flyResult() {
        return flyResult;
    }

    /** PScreen_RegionMap#pbMapScene (overwrite:71-202). @return true = close */
    public boolean update(InputManager input, float delta) {
        elapsed += Math.max(0f, delta);
        if (offsetX != 0f || offsetY != 0f) {
            // The cursor glides 4 px per 40 fps frame; input waits (283-290).
            float step = SCROLL_SPEED * delta * 40f;
            offsetX = decay(offsetX, step);
            offsetY = decay(offsetY, step);
            cursorX = targetX - offsetX;
            cursorY = targetY - offsetY;
            return false;
        }
        if (input.wasPressed(GameAction.SHOULDER_LEFT)) {
            if (model.switchRegion(-1)) {
                context.audioManager().playSe("GUI naming tab swap start", 100, 100);
                loadRegionImage();
                resetCursor();
            }
            return false;
        }
        if (input.wasPressed(GameAction.SHOULDER_RIGHT)) {
            if (model.switchRegion(1)) {
                context.audioManager().playSe("GUI naming tab swap start", 100, 100);
                loadRegionImage();
                resetCursor();
            }
            return false;
        }
        int ox = (input.isDown(GameAction.LEFT) ? -1 : 0) + (input.isDown(GameAction.RIGHT) ? 1 : 0);
        int oy = (input.isDown(GameAction.UP) ? -1 : 0) + (input.isDown(GameAction.DOWN) ? 1 : 0);
        if (ox != 0 || oy != 0) {
            model.move(ox, oy);
            offsetX = ox * SQUARE;
            offsetY = oy * SQUARE;
            targetX = cursorX + offsetX;
            targetY = cursorY + offsetY;
        }
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            MenuSe.close(context.audioManager());
            flyResult = null;
            return true;
        }
        if (flyMode && input.wasPressed(GameAction.CONFIRM)) {          // :361-367 choosing an area to fly to
            int[] spot = model.flySpot();
            if (spot != null && context.gameState().fieldGlobals().visitedMaps.contains(spot[0])) {
                flyResult = spot;
                return true;
            }
        }
        return false;
    }

    private static float decay(float offset, float step) {
        if (offset > 0f) {
            return Math.max(0f, offset - step);
        }
        return Math.min(0f, offset + step);
    }

    private void resetCursor() {
        cursorX = squareX(model.mapX());
        cursorY = squareY(model.mapY());
        targetX = cursorX;
        targetY = cursorY;
        offsetX = 0f;
        offsetY = 0f;
    }

    /** PScreen_RegionMap:119-121 centers the current region's own image. */
    private void loadRegionImage() {
        PbsData.TownMapRegion region = model.currentRegion();
        mapImage = region == null ? null : assets.graphic("Pictures", stripExtension(region.filename));
        float width = ScreenMetrics.logicalWidth();
        float height = ScreenMetrics.logicalHeight();
        float imageWidth = mapImage == null ? 480f : mapImage.getWidth();
        float imageHeight = mapImage == null ? 320f : mapImage.getHeight();
        mapLeft = (width - imageWidth) / 2f;
        mapTop = (height - imageHeight) / 2f;
    }

    /** Plugin x of the cursor/marker on a square (147-148, 168-169). */
    private float squareX(int mapX) {
        return -SQUARE / 2f + mapX * SQUARE + mapLeft;
    }

    /** Plugin (top-origin) y of the cursor/marker on a square. */
    private float squareY(int mapY) {
        return -SQUARE / 2f + mapY * SQUARE + mapTop;
    }

    public void render(SpriteBatch batch, MenuFont font) {
        float width = ScreenMetrics.logicalWidth();
        float height = ScreenMetrics.logicalHeight();
        // addBackgroundOrColoredPlane (MessageConfig:715-729): mapbg covers the
        // screen; without the bitmap the plane stays black.
        Texture background = assets.graphic("Pictures", "mapbg");
        if (background != null) {
            batch.draw(background, 0f, 0f, width, height);
        }
        if (mapImage != null) {
            batch.draw(mapImage, mapLeft, height - mapTop - mapImage.getHeight());
        }
        for (Object[] extra : REGION_MAP_EXTRAS) {
            if (((Number) extra[0]).intValue() != model.region()) {
                continue;
            }
            int switchId = ((Number) extra[1]).intValue();
            boolean visible = wallmap && Boolean.TRUE.equals(extra[5])
                    || !wallmap && switchId > 0 && context.gameState().switches().get(switchId);
            if (!visible) {
                continue;
            }
            Texture hidden = assets.graphic("Pictures", (String) extra[4]);
            if (hidden == null) {
                continue;
            }
            int x = ((Number) extra[2]).intValue();
            int y = ((Number) extra[3]).intValue();
            batch.draw(hidden, mapLeft + x * SQUARE,
                    height - mapTop - (y * SQUARE + hidden.getHeight()));
        }
        if (flyMode) {                                                    // :150-165 the places the player has been to
            Texture marker = assets.graphic("Pictures", "mapFly");
            if (marker != null) {
                int frameWidth = marker.getWidth() / FLY_FRAMES;
                int frame = (int) (elapsed * 40f / FLY_FRAME_FRAMES) % FLY_FRAMES;
                for (int[] spot : model.flySpots()) {
                    if (!context.gameState().fieldGlobals().visitedMaps.contains(spot[2])) {
                        continue;
                    }
                    batch.draw(marker, squareX(spot[0]), height - squareY(spot[1]) - marker.getHeight(),
                            frameWidth, marker.getHeight(), frame * frameWidth, 0, frameWidth, marker.getHeight(),
                            false, false);
                }
            }
        }
        // The player head stays where pbStartScene put it (144-149).
        if (playerHead != null && playerPosition != null && playerPosition.length >= 3
                && model.region() == playerPosition[0]) {
            batch.draw(playerHead, squareX(playerPosition[1]),
                    height - squareY(playerPosition[2]) - playerHead.getHeight());
        }
        Texture cursor = assets.graphic("Pictures", "mapCursor");
        if (cursor != null) {
            int frame = (int) (elapsed * 40f / CURSOR_FRAME_FRAMES) % CURSOR_FRAMES;
            int frameWidth = cursor.getWidth() / CURSOR_FRAMES;
            int frameHeight = cursor.getHeight();
            batch.draw(cursor, cursorX, height - cursorY - frameHeight,
                    frameWidth, frameHeight, frame * frameWidth, 0,
                    frameWidth, frameHeight, false, false);
        }
        // MapBottomSprite (44-59): white text with the black outline shadow.
        Color main = new Color(248f / 255f, 248f / 255f, 248f / 255f, 1f);
        Color shadow = new Color(0f, 0f, 0f, 1f);
        PbsData.TownMapRegion regionData = model.currentRegion();
        font.draw(batch, regionData == null ? "" : regionData.name,
                18f, height - TOP_TEXT_Y, main, shadow);
        if (model.playerRegion() != 3) {
            font.drawCentered(batch, "[A]/[S]:切换地区",
                    width / 2f, height - TOP_TEXT_Y, main, shadow);
        }
        font.draw(batch, model.location(), 18f, height - BOTTOM_TEXT_Y, main, shadow);
        font.drawRight(batch, model.details(), width - 16f, height - BOTTOM_TEXT_Y, main, shadow);
    }

    /** MetadataMapPosition of the map the player stands on (85). */
    private int[] currentMapPosition() {
        GameState state = context.gameState();
        if (state == null || context.database() == null) {
            return null;
        }
        MapData map = context.database().map(state.currentMapId());
        return map == null ? null : map.mapPosition;
    }

    /**
     * {@code pbPlayerHeadFile($Trainer.trainertype)} (PSystem_FileUtilities:
     * 421-433): mapPlayer&lt;CONST&gt;_&lt;outfit&gt;, then mapPlayer%03d_0.
     * The runtime has no outfit, so it always uses 0.
     */
    private Texture loadPlayerHead() {
        GameState state = context.gameState();
        ProjectInfo project = context.database() == null ? null : context.database().project();
        ProjectInfo.RuntimeProfile profile = project == null ? null : project.runtime;
        int id = state == null ? -1 : state.playerId();
        String type = null;
        if (profile != null && id >= 0) {
            ProjectInfo.PlayerGraphic graphic = profile.player(id);
            type = graphic == null ? null : graphic.trainerType;
        }
        if (type != null && !type.isEmpty()) {
            Texture named = assets.graphic("Pictures", "mapPlayer" + type + "_0");
            if (named != null) {
                return named;
            }
        }
        PbsData pbs = context.pbsData();
        PbsData.TrainerType trainerType = pbs == null || type == null ? null : pbs.trainerTypes.get(type);
        if (trainerType != null) {
            Texture numeric = assets.graphic("Pictures",
                    String.format(java.util.Locale.ROOT, "mapPlayer%03d_0", trainerType.id));
            if (numeric != null) {
                return numeric;
            }
        }
        return null;
    }

    /** "mapRegion0.png" -> "mapRegion0" (MenuAssets appends the extension). */
    private static String stripExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        return filename.endsWith(".png") ? filename.substring(0, filename.length() - 4) : filename;
    }
}
