package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.audio.UiSounds;
import pokemon.runtime.battle.BattleSpriteShader;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.HabitatConfig;
import pokemon.runtime.pokemon.HabitatLog;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.TrainerState;

import java.util.ArrayList;
import java.util.List;

/**
 * 294_Boonzeet_s_Habitat_List {@code HabitatDetailScene} (:899-1378): one habitat. Its kinds (grass / cave, surf, fish) are pages
 * that show the Pokemon of that kind in a 6-wide grid with their seen / caught marks and, when everything of a kind was seen or
 * caught, the stamp that drops onto the page (the first time it is looked at after the status changed). In the list's version the
 * page before the first kind is the region map with the habitat's tiles lit; UP / DOWN go to the previous / next habitat of the
 * list. From the region map (F5 on a tile) only the kinds are shown.
 *
 * <p>Coordinates are the plugin's top-origin values (the screen is 672x448).</p>
 */
public final class HabitatDetailView {

    private static final Color BASE = new Color(88f / 255f, 88f / 255f, 80f / 255f, 1f);
    private static final Color SHADOW = new Color(168f / 255f, 184f / 255f, 184f / 255f, 1f);
    /** 294:1146-1149. */
    private static final String[] LABEL_KEYS = {"grass", "place", "surf", "fish"};
    private static final String[] LABELS = {"在地面上遭遇的宝可梦", "在洞穴中遭遇的宝可梦", "在冲浪时遭遇的宝可梦", "在垂钓时遭遇的宝可梦"};
    private static final int MAP_WIDTH = 30;                                        // 1 + RIGHT - LEFT (214:63-66)
    private static final float SQUARE = 16f;

    private final RuntimeContext context;
    private final TrainerState trainer;
    private final PbsData pbs;
    private final List<HabitatLog.Entry> list;     // null in the region map's version
    private final boolean single;
    private final int region;
    private final MenuClock clock = new MenuClock();

    private int index;                             // the position in the list, or the habitat's own index when single
    private HabitatLog.Habitat habitat;
    private final List<String> order = new ArrayList<>();
    private int encpages;
    private int page;
    private HabitatLog.Kind encounter;
    private boolean closed;
    private boolean loaded;
    private int frameCount;

    // the stamp sprite: it keeps its position and zoom between animations, like the plugin's sprite
    private double stampX = 400 + 64, stampY = 296 + 64, stampZoom = 1.0;
    private int stampOpacity = 255;
    private boolean stampVisible;
    private boolean stampOwned;
    private int stampFrame = -1;                   // 0..17 while the animation runs, -1 otherwise

    /** From the habitat list ({@code pbStartScene(index, habitatlist, region)}, :961-997). */
    public HabitatDetailView(RuntimeContext context, List<HabitatLog.Entry> list, int index, int region) {
        this.context = context;
        this.trainer = context.gameState().trainer();
        this.pbs = context.pbsData();
        this.list = list;
        this.single = false;
        this.region = region;
        this.index = index;
        this.habitat = trainer.habitats.data.get(list.get(index).index);
        start();
    }

    /** From the region map ({@code pbStartSceneSingle(index)}, :999-1014). */
    public HabitatDetailView(RuntimeContext context, int habitatIndex) {
        this.context = context;
        this.trainer = context.gameState().trainer();
        this.pbs = context.pbsData();
        this.list = null;
        this.single = true;
        this.region = 0;
        this.index = habitatIndex;
        this.habitat = trainer.habitats.data.get(habitatIndex);
        start();
    }

    private void start() {
        rebuildOrder();
        encpages = habitat.encounters.size() - 1;                                 // :969 / :1005
        page = 0;
        setEncounter();
        drawPage();
        loaded = true;                                                            // :990-991 after the fade-in
        updateStamp();
    }

    /** The position in the list the screen ended on ({@code pbScene} returns {@code @index}). */
    public int index() {
        return index;
    }

    private void rebuildOrder() {
        order.clear();
        for (String key : HabitatConfig.TYPE_ORDER) {                             // :970-974
            if (habitat.encounters.containsKey(key)) order.add(key);
        }
    }

    private void setEncounter() {
        if (order.isEmpty()) {
            encounter = null;
            return;
        }
        int at = page < 0 ? order.size() + page : page;                           // Ruby's @order[-1] is the last
        encounter = habitat.encounters.get(order.get(Math.max(0, Math.min(order.size() - 1, at))));
    }

    // ---------------------------------------------------------------- input

    /** @return true once the screen is closed */
    public boolean update(InputManager input) {
        int ticks = clock.advance();
        for (int t = 0; t < ticks; t++) {
            frameCount++;
            if (stampFrame >= 0) stepStamp();
        }
        if (closed) return true;
        if (stampFrame >= 0) return false;                                        // :1170-1190 animateStamp blocks the scene
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            UiSounds.cancel(context.audioManager());                              // :1297 pbPlayCancelSE
            closed = true;
            return true;
        }
        boolean refresh = false;
        if (!single && input.wasPressed(GameAction.UP)) {                         // :1303-1312
            int old = index;
            if (index > 0) index -= 1;                                            // pbGoToPrevious
            if (index != old) {
                UiSounds.decision(context.audioManager());
                updateHabitat();
                refresh = true;
            }
        } else if (!single && input.wasPressed(GameAction.DOWN)) {                // :1313-1322
            int old = index;
            if (index < list.size() - 1) index += 1;                              // pbGoToNext
            if (index != old) {
                UiSounds.decision(context.audioManager());
                updateHabitat();
                refresh = true;
            }
        } else if (input.wasPressed(GameAction.LEFT) || input.wasPressed(GameAction.RIGHT)) {   // :1323-1360
            int old = page;
            page += input.wasPressed(GameAction.LEFT) ? -1 : 1;
            page = Math.max(single ? 0 : -1, page);
            page = Math.min(encpages, page);
            if (page != old) {
                UiSounds.cursor(context.audioManager());
                refresh = true;
            }
        }
        if (refresh) {
            drawPage();
        }
        return false;
    }

    /** {@code pbUpdateHabitat} (:1016-1040): the habitat of the new list position, its kinds, and the page kept inside them. */
    private void updateHabitat() {
        habitat = trainer.habitats.data.get(list.get(index).index);
        rebuildOrder();
        int oldsize = encpages;
        encpages = order.size() - 1;
        if (page == oldsize) {
            page = encpages;
        } else if (page >= encpages) {
            page = encpages;
        }
    }

    // ---------------------------------------------------------------- page logic

    /** {@code drawPage(page)} (:1063-1132) for the state: the encounter of the page and the stamp. */
    private void drawPage() {
        setEncounter();
        stampVisible = false;                                                     // :1090
        if (single || page > -1) {
            updateStamp();                                                        // drawPageInfo -> updateStamp (:1244-1247)
        }
    }

    /** {@code updateStamp} (:1134-1160). */
    private void updateStamp() {
        if (page > -1 && encounter != null) {
            if (encounter.alert) {                                                // the status changed since it was last shown
                if (!loaded) return;                                              // wait until load to play the animation
                animateStamp(encounter.owned);
                encounter.alert = false;
                boolean completed = true;
                for (HabitatLog.Kind kind : habitat.encounters.values()) {
                    if (kind.alert || !kind.owned) completed = false;
                }
                habitat.completed = completed;
            } else if (encounter.owned || encounter.seen) {
                stampOwned = encounter.owned;                                     // drawStamp (:1192-1196)
                stampVisible = true;
            }
        }
    }

    /** {@code animateStamp(owned)} (:1162-1190): 18 frames; the plugin starts it 34 px up-left and zoomed to 1.51. */
    private void animateStamp(boolean owned) {
        stampOwned = owned;
        stampOpacity = 0;
        stampX -= 34;
        stampY -= 34;
        stampZoom = 1.51;
        stampVisible = true;
        stampFrame = 0;
    }

    private void stepStamp() {
        stampOpacity += 15;
        if (stampZoom > 1.0) {
            stampZoom -= 0.03;
        }
        if (stampX < 400) {                                                       // (the plugin's start x is 430, so it never moves)
            stampX += 2;
            stampY += 2;
        }
        if (stampOpacity > 255) stampOpacity = 255;
        stampFrame++;
        if (stampFrame > 17) {
            stampFrame = -1;
        }
    }

    // ---------------------------------------------------------------- drawing

    private float w, h;

    private void image(SpriteBatch b, Texture t, double x, double y) {
        if (t != null) b.draw(t, (float) x, (float) (h - y - t.getHeight()));
    }

    private void imagePart(SpriteBatch b, Texture t, float x, float y, int sx, int sy, int sw, int sh) {
        if (t != null) b.draw(t, x, h - y - sh, sw, sh, sx, sy, sw, sh, false, false);
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f) {
        w = ScreenMetrics.logicalWidth();
        h = ScreenMetrics.logicalHeight();
        b.setColor(Color.WHITE);
        image(b, a.graphic("Pictures/Habitats", "bg_info"), 0, 0);
        // the arrows (:937-943, :1100-1118)
        Texture arrows = a.graphic("Pictures/Habitats", "selarrows");
        boolean leftDim = single ? page == 0 : page == -1;
        b.setColor(1f, 1f, 1f, leftDim ? 125f / 255f : 1f);
        imagePart(b, arrows, 20f, 10f, 0, 0, 18, 26);
        b.setColor(1f, 1f, 1f, page == encpages ? 125f / 255f : 1f);
        imagePart(b, arrows, w - 38f, 10f, 18, 0, 18, 26);
        b.setColor(Color.WHITE);
        // the kinds' icons and the cursor (:1075-1099)
        float cursorX = 48f;
        for (int i = 0; i < order.size(); i++) {
            float x = 52f + (i + (single ? 0 : 1)) * 70f;
            String key = order.get(i);
            String file = "surf".equals(key) ? "habitat_surf" : "fish".equals(key) ? "habitat_fish" : "habitat_grass";
            image(b, a.graphic("Pictures/Habitats", file), x, 6);
            if (i == page) cursorX = x - 4f;
        }
        if (!single) {
            b.setColor(1f, 1f, 1f, page == -1 ? 1f : 125f / 255f);
            image(b, a.graphic("Pictures/Habitats", "habitat_map"), 60, 14);
            b.setColor(Color.WHITE);
        }
        image(b, a.graphic("Pictures/Habitats", "habitat_cursor"), cursorX, 2);
        if (single || page > -1) {
            drawPageInfo(b, a, f);
        } else {
            drawPageArea(b, a, f);
        }
    }

    private void text(SpriteBatch b, MenuFont f, String text, float x, float y, int align) {
        if (align == 2) f.drawCentered(b, text, x, h - y, BASE, SHADOW);
        else f.draw(b, text, x, h - y, BASE, SHADOW);
    }

    /** {@code drawPageInfo} (:1198-1232). */
    private void drawPageInfo(SpriteBatch b, MenuAssets a, MenuFont f) {
        if (encounter == null) return;
        String key = order.get(Math.max(0, Math.min(order.size() - 1, page)));
        for (int i = 0; i < encounter.list.size(); i++) {
            float x = (i % 6) * 72 + 44 + 80;
            float y = (i / 6) * 64 + 80 + 32;
            drawPanel(b, a, encounter.list.get(i), x, y);
        }
        String label = LABELS[1];
        for (int i = 0; i < LABEL_KEYS.length; i++) {
            if (LABEL_KEYS[i].equals(key)) label = LABELS[i];
        }
        String mapName = context.database() == null ? "" : context.database().mapName(habitat.mapIds[0]);
        text(b, f, mapName == null ? "" : mapName, 24 + 64, 298 + 64, 0);
        text(b, f, label, 24 + 64, 338 + 64, 0);
        if (stampVisible) {
            Texture stamp = a.graphic("Pictures/Habitats", stampOwned ? "stamp_owned" : "stamp_seen");
            if (stamp != null) {
                b.setColor(1f, 1f, 1f, stampOpacity / 255f);
                float zoom = (float) stampZoom;
                b.draw(stamp, (float) stampX, (float) (h - stampY - stamp.getHeight() * zoom),
                        stamp.getWidth() * zoom, stamp.getHeight() * zoom);
                b.setColor(Color.WHITE);
            }
        }
    }

    /** {@code HabitatPokemonPanel} (:1398-1494): the panel's background, the icon (a dark silhouette until seen) and the mark. */
    private void drawPanel(SpriteBatch b, MenuAssets a, String entry, float x, float y) {
        String base = HabitatLog.baseSpecies(pbs, entry);
        int form = HabitatLog.formOf(entry);
        boolean owned = trainer.owned.contains(base);
        boolean seen = trainer.seen.contains(base);
        image(b, a.graphic("Pictures/Habitats", "ListPokemonBg"), x, y);
        PbsData.Species species = pbs == null ? null : pbs.species(base);
        Texture icon = species == null ? null : speciesIcon(a, species, form);
        if (icon != null) {
            int size = Math.min(64, icon.getHeight());
            if (!seen) {
                toneDark(b, icon, x, y - 16f, size);                                // @pkmnsprite.tone = Tone.new(-64,-64,-64,255)
            } else {
                imagePart(b, icon, x, y - 16f, 0, 0, size, size);
            }
        }
        if (owned) {
            image(b, a.graphic("Pictures/Habitats", "iconowned"), x + 46, y + 2);
        } else if (seen) {
            image(b, a.graphic("Pictures/Habitats", "iconseen"), x + 46, y + 2);
        }
    }

    /** {@code pbCheckPokemonIconFiles([id, false, false, form, false])}: the species' icon of that form, else its own. */
    private Texture speciesIcon(MenuAssets a, PbsData.Species species, int form) {
        Texture icon = null;
        if (form > 0) {
            icon = a.icon(String.format("icon%03d_%d", species.id, form));
            if (icon == null) icon = a.icon("icon" + species.internalName + "_" + form);
        }
        if (icon == null) icon = a.icon(String.format("icon%03d", species.id));
        if (icon == null) icon = a.icon("icon" + species.internalName);
        return icon;
    }

    private static BattleSpriteShader toneShader;

    /** A sprite with {@code Tone.new(-64,-64,-64,255)}: fully gray and darkened. */
    private void toneDark(SpriteBatch b, Texture icon, float x, float y, int size) {
        if (toneShader == null) toneShader = new BattleSpriteShader();
        if (!toneShader.isCompiled()) {
            b.setColor(0.3f, 0.3f, 0.3f, 1f);
            imagePart(b, icon, x, y, 0, 0, size, size);
            b.setColor(Color.WHITE);
            return;
        }
        b.flush();
        b.setShader(toneShader.program());
        toneShader.setTone(new float[] {-64f, -64f, -64f, 255f});
        toneShader.setColor(new float[] {0f, 0f, 0f, 0f});
        toneShader.setMosaic(0f, icon.getWidth(), icon.getHeight());
        imagePart(b, icon, x, y, 0, 0, size, size);
        b.flush();
        b.setShader(null);
    }

    /** {@code drawPageArea} (:1234-1295): the region map with the habitat's tiles lit. */
    private void drawPageArea(SpriteBatch b, MenuAssets a, MenuFont f) {
        PbsData.TownMapRegion map = pbs == null || pbs.townMap == null ? null : pbs.townMap.region(region);
        Texture mapImage = map == null ? null : a.graphic("Pictures", stripExtension(map.filename));
        float mapX = mapImage == null ? 0f : (w - mapImage.getWidth()) / 2f;
        float mapY = mapImage == null ? 0f : (h + 46f - mapImage.getHeight()) / 2f;
        if (mapImage != null) {
            image(b, mapImage, mapX, mapY);
            for (Object[] extra : TownMapView.REGION_MAP_EXTRAS) {                // :983-990 REGION_MAP_EXTRAS
                int extraRegion = (Integer) extra[0], extraSwitch = (Integer) extra[1];
                if (extraRegion == region && extraSwitch > 0 && context.gameState().switches().get(extraSwitch)) {
                    image(b, a.graphic("Pictures", (String) extra[4]), mapX + (Integer) extra[2] * SQUARE, mapY + (Integer) extra[3] * SQUARE);
                }
            }
        }
        // the tiles (:1241-1264)
        java.util.Set<Integer> tiles = new java.util.TreeSet<>();
        for (int mapId : habitat.mapIds) {
            PbsData.Metadata meta = pbs == null ? null : pbs.mapMetadata(mapId);
            if (meta == null || meta.mapPosition == null || meta.mapPosition.length < 3 || meta.mapPosition[0] != region) continue;
            boolean showPoint = true;
            if (map != null) {
                for (PbsData.TownMapPoint loc : map.points) {
                    if (loc.x == meta.mapPosition[1] && loc.y == meta.mapPosition[2] && loc.switchId != null
                            && !context.gameState().switches().get(loc.switchId)) {
                        showPoint = false;
                    }
                }
            }
            if (showPoint) {
                tiles.add(meta.mapPosition[1] + meta.mapPosition[2] * MAP_WIDTH);          // (MetadataMapSize is not used by this project)
            }
        }
        int intensity = (frameCount % 60) * 12;                                    // :1038-1040 the pulse
        if (intensity > 240) intensity = 480 - intensity;
        Texture pixel = a.pixel();
        for (int j : tiles) {
            float x = (j % MAP_WIDTH) * SQUARE + mapX;
            float y = (j / MAP_WIDTH) * SQUARE + mapY;
            b.setColor(0f, 248f / 255f, 248f / 255f, intensity / 255f);
            b.draw(pixel, x, h - y - SQUARE, SQUARE, SQUARE);
            b.setColor(192f / 255f, 248f / 255f, 248f / 255f, intensity / 255f);
            if (j - MAP_WIDTH < 0 || !tiles.contains(j - MAP_WIDTH)) b.draw(pixel, x, h - (y - 2f) - 2f, SQUARE, 2f);
            if (!tiles.contains(j + MAP_WIDTH)) b.draw(pixel, x, h - (y + SQUARE) - 2f, SQUARE, 2f);
            if (j % MAP_WIDTH == 0 || !tiles.contains(j - 1)) b.draw(pixel, x - 2f, h - y - SQUARE, 2f, SQUARE);
            if ((j + 1) % MAP_WIDTH == 0 || !tiles.contains(j + 1)) b.draw(pixel, x + SQUARE, h - y - SQUARE, 2f, SQUARE);
        }
        b.setColor(Color.WHITE);
        if (tiles.isEmpty()) {                                                     // :1279-1285
            image(b, a.graphic("Pictures/Pokedex", "overlay_areanone"), 108, 188);
            text(b, f, "Area unknown", w / 2f, h / 2f, 2);
        }
    }

    private static String stripExtension(String file) {
        return file == null ? "" : file.replaceAll("(?i)\\.(png|bmp|jpg|jpeg|gif)$", "");
    }
}
