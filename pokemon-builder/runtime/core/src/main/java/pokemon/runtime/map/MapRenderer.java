package pokemon.runtime.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.data.MapData;
import pokemon.runtime.state.GameState;
import java.io.File;
import com.badlogic.gdx.utils.Array;

import java.util.Arrays;

/** Cached assets and primitive draw order; no file IO or per-tile allocation during rendering. */
public final class MapRenderer {
    private static final int TILE = TilesetGeometry.TILE_SIZE;
    private final TileMap map;
    private final TextureRepository textures;
    private final GraphicsLocator graphics;
    private final GameState state;
    private final TilesetGeometry geometry;
    private final Texture[] tileset;
    private final Texture[] autotiles = new Texture[7];
    private final boolean[] usedAutotiles = new boolean[7];
    /** Slots the tileset leaves empty: RMXP draws these tiles as nothing. */
    private final boolean[] emptyAutotiles = new boolean[7];
    private final Texture[] characters;
    private final MapData.EventGraphic[] eventGraphics;
    private final long[] tilesAndEvents;
    private long[] drawOrder;
    /** Event indices drawn by the entity path (R6: NPCs walk smoothly). */
    private boolean[] runtimeEvents = new boolean[0];
    private final int cells;
    private float elapsed;
    /** State version of the currently loaded event graphics (R5). */
    private long pageVersion;

    private final Array<MapCharacter> entities = new Array<>();
    private Texture[] entityTextures = new Texture[0];
    private Texture[] runningTextures = new Texture[0];
    private int[] entityDepths = new int[0];
    private long[] entityOrder = new long[0];
    /** Character sheets that failed to load; never retried, never fatal (R6.24). */
    private final java.util.Set<String> missingCharacters = new java.util.HashSet<>();
    /** Next event index {@link #warmCharacters} continues from (R6.24). */
    private int warmCursor;

    /**
     * R6.24: loads the next still-missing event sheets until the time budget is
     * used up. A neighbour preload then stays short and the sheets arrive over
     * the frames the player walks towards the border, instead of one long frame
     * at the crossing.
     */
    public void warmCharacters(long budgetNanos) {
        if (warmCursor >= characters.length) {
            return;
        }
        long deadline = System.nanoTime() + Math.max(0L, budgetNanos);
        while (warmCursor < characters.length) {
            int index = warmCursor++;
            MapData.EventGraphic graphic = eventGraphics[index];
            if (graphic == null || graphic.tileId != 0 || graphic.characterName == null
                    || graphic.characterName.isEmpty() || characters[index] != null) {
                continue;
            }
            characters[index] = lazyCharacterTexture(graphic.characterName);
            if (System.nanoTime() >= deadline) {
                return;
            }
        }
    }

    public MapRenderer(TileMap map, TextureRepository textures, GraphicsLocator graphics, GameState state) {
        this.map = map;
        this.textures = textures;
        this.graphics = graphics;
        this.state = state;
        if (map.tileset() == null) throw new IllegalArgumentException("Map has no tileset");
        File file = requireGraphic(graphics, "Tilesets", map.tileset().tilesetName);
        tileset = textures.loadTileset(file);
        int height = (tileset.length - 1) * TextureRepository.PAGE_HEIGHT + tileset[tileset.length - 1].getHeight();
        geometry = new TilesetGeometry(height / TILE);
        for (int layer = 0; layer < 3; layer++)
            for (int row = 0; row < map.height(); row++)
                for (int col = 0; col < map.width(); col++) {
                    int id = map.tileId(layer, col, row);
                    if (geometry.isAutotile(id)) usedAutotiles[geometry.autotileIndex(id)] = true;
                    else if (id >= 384 && !geometry.isNormal(id))
                        throw new IllegalArgumentException("Tile outside tileset: " + id + " at " + col + "," + row);
                }

        characters = new Texture[map.events().size];
        eventGraphics = new MapData.EventGraphic[map.events().size];
        for (int i = 0; i < map.events().size; i++) applyEventGraphic(i, pageGraphic(map.events().get(i)));
        for (int i = 0; i < autotiles.length; i++) {
            if (!usedAutotiles[i]) continue;
            loadAutotile(i);
        }
        cells = map.width() * map.height();
        long[] order = new long[cells * 3 + map.events().size];
        int count = 0;
        for (int layer = 0; layer < 3; layer++)
            for (int row = 0; row < map.height(); row++)
                for (int col = 0; col < map.width(); col++) {
                    int id = map.tileId(layer, col, row);
                    if (id < 48) continue;
                    int depth = tileDepth(row, map.tileset().priority(id));
                    order[count++] = ((long) depth << 32) | (layer * cells + row * map.width() + col);
                }
        // Every event keeps a draw slot: a switch can activate a page with a
        // graphic long after the map was loaded; drawEvent skips empty pages.
        for (int i = 0; i < map.events().size; i++)
            order[count++] = ((long) eventDepth(map.events().get(i).y) << 32) | (3 * cells + i);
        tilesAndEvents = Arrays.copyOf(order, count);
        Arrays.sort(tilesAndEvents);
        drawOrder = tilesAndEvents;
        pageVersion = state.version();
    }

    /**
     * Registers walking characters (R4): they join the same depth order as the
     * events. Reusable arrays merge interpolated character depths with cached tiles.
     */
    public void setEntities(Array<MapCharacter> characters, TextureRepository textures, GraphicsLocator graphics) {
        entities.clear();
        entities.addAll(characters);
        int count = characters.size;
        entityTextures = new Texture[count];
        runningTextures = new Texture[count];
        entityDepths = new int[count];
        entityOrder = new long[count];
        drawOrder = new long[tilesAndEvents.length + count];
        for (int i = 0; i < count; i++) {
            MapCharacter character = characters.get(i);
            // R6.24: sheets load on their first draw (drawEntity); binding the
            // entities of a map with many events used to stall the crossing.
            entityDepths[i] = entityDepth(character);
        }
        rebuildOrder();
    }

    /** Loads one character sheet on demand; a broken sheet is skipped once. */
    private Texture lazyCharacterTexture(String name) {
        if (name == null || name.isEmpty() || missingCharacters.contains(name)) {
            return null;
        }
        try {
            return textures.load("character:" + name,
                    requireGraphic(graphics, "Characters", name));
        } catch (RuntimeException error) {
            missingCharacters.add(name);
            if (Gdx.app != null) {
                Gdx.app.error("MapRenderer", "character sheet missing: " + name
                        + " (" + error.getMessage() + ")");
            }
            return null;
        }
    }

    private void rebuildOrder() {
        for (int i = 0; i < entities.size; i++) {
            entityOrder[i] = ((long) entityDepths[i] << 32) | (3 * cells + map.events().size + i);
        }
        Arrays.sort(entityOrder);
        int tile = 0, entity = 0, out = 0;
        while (tile < tilesAndEvents.length || entity < entityOrder.length) {
            if (entity >= entityOrder.length || (tile < tilesAndEvents.length
                    && tilesAndEvents[tile] <= entityOrder[entity])) drawOrder[out++] = tilesAndEvents[tile++];
            else drawOrder[out++] = entityOrder[entity++];
        }
    }

    private int entityDepth(MapCharacter character) {
        if (character.alwaysOnTop) {
            return Integer.MAX_VALUE / 2; // "total 在最前面" (route code 39)
        }
        return Math.round(map.height() * TILE - character.pixelY()) + 16;
    }

    private static File requireGraphic(GraphicsLocator locator, String directory, String name) {
        File file = locator.find(directory, name + ".png");
        if (file == null) throw new IllegalArgumentException("Missing graphic: " + directory + "/" + name);
        return file;
    }

    /** Live page graphic of one event (R5): switches, variables and self switches decide. */
    private MapData.EventGraphic pageGraphic(MapData.EventData event) {
        MapData.EventPageData page = EventPages.resolve(state, map.mapId(), event);
        return page == null ? null : page.graphic;
    }

    /**
     * Applies an event page; the character sheet itself loads on demand in
     * {@link #drawEvent} (R6.24), so a neighbour map with many events costs
     * nothing until its sprites are actually on screen.
     */
    private void applyEventGraphic(int index, MapData.EventGraphic graphic) {
        eventGraphics[index] = graphic;
        characters[index] = null;
        if (graphic == null || graphic.opacity == 0) return;
        if (geometry.isAutotile(graphic.tileId)) {
            int slot = geometry.autotileIndex(graphic.tileId);
            usedAutotiles[slot] = true;
            loadAutotile(slot);
        } else if (graphic.tileId >= 384 && !geometry.isNormal(graphic.tileId)) {
            throw new IllegalArgumentException("Tile outside tileset: " + graphic.tileId
                    + " on event " + map.events().get(index).id);
        }
    }

    private void loadAutotile(int slot) {
        if (autotiles[slot] != null || emptyAutotiles[slot]) return;
        String[] names = map.tileset().autotileNames;
        if (names == null || slot >= names.length || names[slot] == null || names[slot].isEmpty()) {
            // 22 of this project's 50 tilesets define no autotile graphics at all
            // (for example "g4 Indoors"), yet their maps still store tile ids in
            // the autotile range. RMXP renders those cells as empty, so skipping
            // them keeps the map open instead of failing the whole transfer.
            emptyAutotiles[slot] = true;
            if (Gdx.app != null) {
                Gdx.app.log("MapRenderer", "autotile slot " + slot
                        + " has no graphic in tileset " + map.tileset().name + "; tiles stay empty");
            }
            return;
        }
        Texture image = textures.load("autotile:" + names[slot], requireGraphic(graphics, "Autotiles", names[slot]));
        if ((image.getHeight() != 32 && image.getHeight() != 128)
                || image.getWidth() % geometry.frameWidth(image.getHeight()) != 0)
            throw new IllegalArgumentException("Invalid autotile dimensions: " + names[slot]);
        autotiles[slot] = image;
    }

    /**
     * Re-resolves every event page after a switch / variable / self switch
     * change (R5). A no-op in the common frame: one version comparison.
     */
    public void refreshEventPages() {
        long version = state.version();
        if (version == pageVersion) return;
        pageVersion = version;
        for (int i = 0; i < map.events().size; i++) {
            applyEventGraphic(i, pageGraphic(map.events().get(i)));
        }
    }

    static int tileDepth(int row, int priority) { return priority == 0 ? 0 : (row + priority + 1) * TILE; }
    static int eventDepth(int row) { return (row + 1) * TILE + 16; }
    public void update(float delta) { elapsed += Math.max(0f, delta); }

    public void render(SpriteBatch batch, MapCamera camera) {
        render(batch, camera, 0f, 0f);
    }

    /** Draw pending world effects before the next map depth, with batch begun. */
    public interface DepthLayer {
        void beforeDepth(int depth);
    }

    public void render(SpriteBatch batch, MapCamera camera, DepthLayer effects) {
        render(batch, camera, 0f, 0f, effects);
    }

    /**
     * R6.23: draws this map shifted by a map-connection offset (the tiles and
     * the static events), so a neighbouring map stays visible beyond the
     * border. Positions and the culling rectangle are both in the current
     * map's space: the rect is the visible window, the tiles carry the offset.
     */
    public void render(SpriteBatch batch, MapCamera camera, float offsetX, float offsetY) {
        render(batch, camera, offsetX, offsetY, null);
    }

    private void render(SpriteBatch batch, MapCamera camera, float offsetX, float offsetY,
                        DepthLayer effects) {
        int left = camera.originX();
        int bottom = camera.originY();
        int right = left + camera.viewPixelWidth(), top = bottom + camera.viewPixelHeight();
        refreshEventPages();
        rebuildOrderIfNeeded();
        batch.begin();
        int lastDepth = Integer.MIN_VALUE;
        for (long entry : drawOrder) {
            int depth = (int) (entry >> 32);
            if (effects != null && depth != lastDepth) {
                effects.beforeDepth(depth);
                lastDepth = depth;
            }
            int token = (int) entry;
            if (token >= cells * 3) {
                int index = token - cells * 3;
                if (index < map.events().size) {
                    drawEvent(batch, index, left, bottom, right, top, offsetX, offsetY);
                } else {
                    drawEntity(batch, index - map.events().size, left, bottom, right, top,
                            offsetX, offsetY);
                }
                continue;
            }
            int layer = token / cells, cell = token % cells;
            int col = cell % map.width(), row = cell / map.width();
            int x = col * TILE + Math.round(offsetX), y = worldY(row) + Math.round(offsetY);
            if (x >= right || x + TILE <= left || y >= top || y + TILE <= bottom) continue;
            drawTile(batch, map.tileId(layer, col, row), x, y);
        }
        if (effects != null) effects.beforeDepth(Integer.MAX_VALUE);
        batch.end();
    }
    private int worldY(int row) { return (map.height() - row - 1) * TILE; }

    private void drawTile(SpriteBatch batch, int id, int x, int y) {
        if (geometry.isAutotile(id)) {
            Texture image = autotiles[geometry.autotileIndex(id)];
            if (image == null) return;
            int frameX = geometry.animationFrame(image.getWidth(), image.getHeight(), elapsed)
                    * geometry.frameWidth(image.getHeight());
            if (image.getHeight() == TILE) {
                batch.draw(image, x, y, TILE, TILE, frameX, 0, TILE, TILE, false, false);
            } else {
                for (int q = 0; q < 4; q++)
                    batch.draw(image, x + q % 2 * 16, y + (1 - q / 2) * 16, 16, 16,
                            frameX + geometry.quarterX(id, q), geometry.quarterY(id, q), 16, 16, false, false);
            }
        } else if (geometry.isNormal(id)) {
            int sourceY = geometry.normalY(id);
            Texture page = tileset[sourceY / TextureRepository.PAGE_HEIGHT];
            batch.draw(page, x, y, TILE, TILE, geometry.normalX(id),
                    sourceY % TextureRepository.PAGE_HEIGHT, TILE, TILE, false, false);
        }
    }

    private void drawEvent(SpriteBatch batch, int index, int left, int bottom, int right, int top,
                           float offsetX, float offsetY) {
        if (index < runtimeEvents.length && runtimeEvents[index]) {
            return; // this event is drawn as a walking entity instead
        }
        MapData.EventGraphic graphic = eventGraphics[index];
        if (graphic == null || graphic.opacity == 0) return;
        MapData.EventData event = map.events().get(index);
        Texture image = characters[index];
        int w = image == null ? TILE : image.getWidth() / 4;
        int h = image == null ? TILE : image.getHeight() / 4;
        int x = event.x * TILE + (TILE - w) / 2 + Math.round(offsetX);
        int y = worldY(event.y) + Math.round(offsetY);
        if (x >= right || x + w <= left || y >= top || y + h <= bottom) return;
        if (image == null && graphic.tileId == 0 && graphic.characterName != null
                && !graphic.characterName.isEmpty()) {
            // R6.24: load the sheet the first time the event is really drawn -
            // off-screen events of a neighbour map cost nothing.
            image = lazyCharacterTexture(graphic.characterName);
            characters[index] = image;
            if (image != null) {
                w = image.getWidth() / 4;
                h = image.getHeight() / 4;
                x = event.x * TILE + (TILE - w) / 2 + Math.round(offsetX);
                y = worldY(event.y) + Math.round(offsetY);
            }
        }
        if (image == null && graphic.tileId <= 0) return;
        batch.setColor(1, 1, 1, graphic.opacity / 255f);
        if (graphic.blendType == 1) batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        if (graphic.tileId > 0) drawTile(batch, graphic.tileId, x, y);
        else if (image != null)
            batch.draw(image, x, y, w, h, Math.floorMod(graphic.pattern, 4) * w,
                    directionRow(graphic.direction) * h, w, h, false, false);
        batch.setColor(1, 1, 1, 1);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }
    private int directionRow(int direction) {
        return direction == 4 ? 1 : direction == 6 ? 2 : direction == 8 ? 3 : 0;
    }

    /** Rebuilds when the interpolated screen depth changes, with no per-frame array allocation. */
    private void rebuildOrderIfNeeded() {
        boolean changed = false;
        for (int i = 0; i < entities.size; i++) {
            int depth = entityDepth(entities.get(i));
            if (depth != entityDepths[i]) {
                entityDepths[i] = depth;
                changed = true;
            }
        }
        if (changed) {
            rebuildOrder();
        }
    }

    private void drawEntity(SpriteBatch batch, int index, int left, int bottom, int right, int top,
                            float offsetX, float offsetY) {
        MapCharacter character = entities.get(index);
        boolean running = character.runningCharacterName != null
                && character.runningCharacterName.equals(character.graphicName());
        Texture image;
        if (running) {
            if (runningTextures[index] == null) {
                runningTextures[index] = lazyCharacterTexture(character.runningCharacterName);
            }
            image = runningTextures[index];
            if (image == null) {
                if (entityTextures[index] == null) {
                    entityTextures[index] = lazyCharacterTexture(character.characterName);
                }
                image = entityTextures[index];
            }
        } else {
            if (entityTextures[index] == null) {
                entityTextures[index] = lazyCharacterTexture(character.characterName);
            }
            image = entityTextures[index];
        }
        if (image == null) return;
        int w = image.getWidth() / 4;
        int h = image.getHeight() / 4;
        int x = Math.round(character.pixelX() + (TILE - w) / 2f + offsetX);
        int y = Math.round(character.pixelY() + offsetY);
        if (x >= right || x + w <= left || y >= top || y + h <= bottom) return;
        batch.setColor(1f, 1f, 1f, Math.max(0f, Math.min(1f, character.opacity)));
        batch.draw(image, x, y, w, h, Math.floorMod(character.pattern(), 4) * w,
                directionRow(character.direction()) * h, w, h, false, false);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    /** Marks the events that the entity path draws; the static path skips them. */
    public void setRuntimeEvents(boolean[] flags) {
        runtimeEvents = flags == null ? new boolean[0] : flags;
    }
}
