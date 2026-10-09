package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.audio.UiSounds;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.HabitatConfig;
import pokemon.runtime.pokemon.HabitatLog;
import pokemon.runtime.pokemon.PbsData;

import java.util.List;

/**
 * 294_Boonzeet_s_Habitat_List {@code PokemonHabitatList_Scene} and {@code Window_HabitatList} (:667-897): the pause menu's
 * "分布图鉴". A list of the habitats the player has been to - the name of the area, a tile for each kind of encounter it has
 * (a ball marks "everything caught", "seen" or "something new to look at") and a stamp when every kind is complete - with the
 * thumbnail of the region it is in. C opens the habitat ({@link HabitatDetailView}), B closes.
 */
public final class HabitatListView {

    private static final Color BASE = new Color(88f / 255f, 88f / 255f, 80f / 255f, 1f);
    private static final Color SHADOW = new Color(168f / 255f, 184f / 255f, 184f / 255f, 1f);
    private static final Color TITLE = new Color(248f / 255f, 248f / 255f, 248f / 255f, 1f);
    private static final int WIN_X = 100, WIN_Y = 34, WIN_W = 500, WIN_H = 410 + 32;
    private static final int CONTENT_X = WIN_X + 16, CONTENT_Y = WIN_Y + 16;       // the window has no skin
    private static final int ROW_H = 32;
    private static final int VISIBLE = (WIN_H - 32) / ROW_H;                          // page_item_max

    private final RuntimeContext context;
    private final List<HabitatLog.Entry> list;
    private int index;
    private int top;
    private HabitatDetailView detail;
    private String thumb = "mapthumbRegion0";

    public HabitatListView(RuntimeContext context) {
        this.context = context;
        this.list = context.gameState().trainer().habitats.habitatList(context.gameState().fieldGlobals().visitedMaps);
        int remembered = context.gameState().fieldGlobals().habitatIndex;                // :796-800 $PokemonGlobal.habitatIndex
        this.index = remembered < 0 || remembered > list.size() ? 0 : remembered;       // :846 (index > size ? 0 : index)
        if (index >= list.size()) index = 0;
        keepVisible();
        updateThumb();
    }

    /** The region of the selected habitat's first map ({@code Window_HabitatList#refresh}, :754-757), 0 by default. */
    private int region() {
        if (list.isEmpty()) return 0;
        PbsData pbs = context.pbsData();
        PbsData.Metadata meta = pbs == null ? null : pbs.mapMetadata(list.get(index).mapId);
        return meta == null || meta.mapPosition == null || meta.mapPosition.length < 3 ? 0 : meta.mapPosition[0];
    }

    private void updateThumb() {
        thumb = "mapthumbRegion" + region();
    }

    /** @return true once the list is closed */
    public boolean update(InputManager input) {
        if (detail != null) {
            if (detail.update(input)) {
                int ret = detail.index();                                            // :893-897 the list comes back on the last habitat viewed
                detail = null;
                context.gameState().fieldGlobals().habitatIndex = ret;
                index = Math.max(0, Math.min(list.size() - 1, ret));
                keepVisible();
                updateThumb();
            }
            return false;
        }
        int old = index;
        if (!list.isEmpty()) {
            if (input.wasPressed(GameAction.UP) || input.wasRepeated(GameAction.UP)) {
                if (index > 0) index--;
                else if (input.wasPressed(GameAction.UP)) index = list.size() - 1;       // Window_DrawableCommand wraps on a fresh press
            } else if (input.wasPressed(GameAction.DOWN) || input.wasRepeated(GameAction.DOWN)) {
                if (index < list.size() - 1) index++;
                else if (input.wasPressed(GameAction.DOWN)) index = 0;
            } else if (input.wasPressed(GameAction.SHOULDER_LEFT)) {
                index = Math.max(0, index - VISIBLE);
            } else if (input.wasPressed(GameAction.SHOULDER_RIGHT)) {
                index = Math.min(list.size() - 1, index + VISIBLE);
            }
        }
        if (index != old) {
            UiSounds.cursor(context.audioManager());
            keepVisible();
            context.gameState().fieldGlobals().habitatIndex = index;                 // :824-831
            updateThumb();
        }
        if (input.wasPressed(GameAction.CONFIRM)) {
            UiSounds.decision(context.audioManager());                               // :833
            if (!list.isEmpty()) {
                detail = new HabitatDetailView(context, list, index, region());     // :859-869 pbHabitatDetail
            }
        } else if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            UiSounds.cancel(context.audioManager());                                 // :836-838
            return true;
        }
        return false;
    }

    private void keepVisible() {
        if (index < top) top = index;
        else if (index >= top + VISIBLE) top = index - VISIBLE + 1;
        top = Math.max(0, Math.min(top, Math.max(0, list.size() - VISIBLE)));
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f) {
        if (detail != null) {
            detail.render(b, a, f);
            return;
        }
        float w = ScreenMetrics.logicalWidth(), h = ScreenMetrics.logicalHeight();
        b.setColor(Color.WHITE);
        Texture bg = a.graphic("Pictures/Habitats", "bg_list");                      // addBackgroundPlane (:786)
        if (bg != null) b.draw(bg, 0f, h - bg.getHeight());
        Texture thumbnail = a.graphic("Pictures/Habitats", thumb);
        if (thumbnail != null) b.draw(thumbnail, 8f, h - 68f - thumbnail.getHeight());   // :789-790 (8, 68)
        f.drawCentered(b, "分布列表", w / 2f, h - 2f, TITLE, Color.BLACK);                // :852-856 pbRefresh
        drawRows(b, a, f, h);
    }

    private void drawRows(SpriteBatch b, MenuAssets a, MenuFont f, float h) {
        Texture cursor = a.graphic("Pictures/Habitats", "cursor_list");
        Texture alert = a.graphic("Pictures/Habitats", "icon_list_alert");
        Texture owned = a.graphic("Pictures/Habitats", "icon_list_owned");
        Texture seen = a.graphic("Pictures/Habitats", "icon_list_seen");
        Texture stamp = a.graphic("Pictures/Habitats", "stamp_seen");
        Texture pixel = a.pixel();
        for (int i = top; i < list.size() && i < top + VISIBLE; i++) {
            float x = CONTENT_X;
            float y = CONTENT_Y + (i - top) * ROW_H;
            HabitatLog.Habitat habitat = list.get(i).habitat;
            if (habitat.completed && stamp != null) {                                 // :727-730 the stamp (the part 0,14 / 84x38 of stamp_seen)
                b.draw(stamp, x + 244 + 64, h - (y + 2 + 2) - 38, 84, 38, 0, 14, 84, 38, false, false);
            }
            int slot = 1;
            for (String kind : HabitatConfig.TYPE_ORDER) {                            // :732-737
                HabitatLog.Kind encounter = habitat.encounters.get(kind);
                if (encounter == null) continue;
                float baseX = x + 104 - 32 * slot;                                    // drawEncounterIcon :706
                drawTile(b, pixel, kind, baseX, y + 6, h);
                Texture mark = encounter.alert ? alert : encounter.owned ? owned : encounter.seen ? seen : null;
                if (mark != null) b.draw(mark, baseX + 2, h - (y + 8) - mark.getHeight());
                slot++;
            }
            String name = context.database() == null ? "" : context.database().mapName(list.get(i).mapId);
            f.draw(b, name == null ? "" : name, x + 128, h - (y + 6), BASE, SHADOW);   // :739-741
            if (i == index && cursor != null) {                                       // drawCursor (Window_DrawableCommand)
                b.draw(cursor, x, h - y - cursor.getHeight());
            }
        }
    }

    /** {@code @habitatbg}: a 32x32 square in the kind's colour with the plugin's translucent black frame over it (:668-675, :688-707). */
    private void drawTile(SpriteBatch b, Texture pixel, String kind, float x, float y, float h) {
        float[] c = "surf".equals(kind) ? new float[] {24, 206, 239} : "fish".equals(kind) ? new float[] {24, 111, 214}
                : new float[] {86, 168, 0};
        b.setColor(c[0] / 255f, c[1] / 255f, c[2] / 255f, 1f);
        b.draw(pixel, x, h - y - 32f, 32f, 32f);
        b.setColor(0f, 0f, 0f, 100f / 255f);                                           // fill_rect(0,0,32,32, black alpha 100) minus the inner windows
        b.draw(pixel, x, h - y - 2f, 32f, 2f);
        b.draw(pixel, x, h - y - 32f, 32f, 2f);
        b.draw(pixel, x, h - y - 30f, 2f, 28f);
        b.draw(pixel, x + 30f, h - y - 30f, 2f, 28f);
        b.setColor(0f, 0f, 0f, 20f / 255f);                                            // fill_rect(2,16,28,14, black alpha 20)
        b.draw(pixel, x + 2f, h - (y + 16f) - 14f, 28f, 14f);
        b.setColor(Color.WHITE);
    }
}
