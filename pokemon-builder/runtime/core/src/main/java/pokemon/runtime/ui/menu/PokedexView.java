package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.TrainerState;
import pokemon.runtime.ui.WindowSkin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * BW PokedexMain: the main Pokedex list (the scrolling {@code Pokedex/bg_list}
 * plane + {@code list_overlay}, the 276x344 list window at (368,94) with the
 * {@code cursor_list} selector, the selected species' sprite at (174,228) and
 * the seen/owned counters).
 *
 * <p>Coordinates are the plugin's top-origin values, converted with
 * {@code screenHeight - y} at draw time. Text alignment follows
 * {@code pbDrawTextPositions}: 1 = right, 2 = centre, otherwise left.</p>
 */
public final class PokedexView {

    private static final int WIN_X = 368;
    private static final int WIN_Y = 94;
    private static final int WIN_W = 276;
    private static final int WIN_H = 344;
    private static final int CONTENT_X = WIN_X + 16;   // startX (windowskin nil)
    private static final int CONTENT_Y = WIN_Y + 16;
    private static final int ROW_H = 32;
    private static final int VISIBLE = 9;              // 312 / 32

    private static final Color OK = new Color(222f / 255f, 222f / 255f, 222f / 255f, 1f);
    private static final Color OK_SHADOW = new Color(132f / 255f, 132f / 255f, 132f / 255f, 1f);
    private static final Color INFO = new Color(49f / 255f, 49f / 255f, 49f / 255f, 1f);
    private static final Color INFO_SHADOW = new Color(140f / 255f, 140f / 255f, 140f / 255f, 1f);
    private static final Color NAME = new Color(82f / 255f, 82f / 255f, 90f / 255f, 1f);
    private static final Color DEX_NAME = new Color(222f / 255f, 222f / 255f, 222f / 255f, 1f);

    /** One dex row: the species and its list state. */
    private static final class Entry {
        final PbsData.Species species;
        final boolean seen;
        final boolean owned;

        Entry(PbsData.Species species, boolean seen, boolean owned) {
            this.species = species;
            this.seen = seen;
            this.owned = owned;
        }
    }

    private final RuntimeContext context;
    private final TrainerState trainer;
    private final List<Entry> list = new ArrayList<>();
    private int index;
    private int top;
    private float scroll;
    private String notice = "";
    private DexEntryView entry;

    public PokedexView(RuntimeContext context) {
        this.context = context;
        this.trainer = context.gameState().trainer();
        PbsData data = context.pbsData();
        if (data != null) {
            for (PbsData.Species s : data.species.values()) {
                if (s == null) continue;
                list.add(new Entry(s, trainer.seen.contains(s.internalName),
                        trainer.owned.contains(s.internalName)));
            }
        }
        list.sort(Comparator.comparingInt(e -> e.species.id));
    }

    public boolean update(InputManager input) {
        if (entry != null) {
            if (entry.update(input)) {
                entry = null;
            }
            return false;
        }
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            MenuSe.close(context.audioManager());                                // 329_PokedexMain_BW_Style:1262 pbPlayCloseMenuSE
            return true;
        }
        int old = index;
        if (input.wasPressed(GameAction.UP)) index--;
        if (input.wasPressed(GameAction.DOWN)) index++;
        index = Math.max(0, Math.min(index, Math.max(0, list.size() - 1)));
        if (index != old) {
            keepVisible();
            pokemon.runtime.audio.UiSounds.cursor(context.audioManager());      // Window_Pokedex (Selectable): pbPlayCursorSE
        }
        if (input.wasPressed(GameAction.CONFIRM)) {
            pokemon.runtime.audio.UiSounds.decision(context.audioManager());    // :1271-1273 pbPlayDecisionSE
            List<PbsData.Species> species = speciesList();
            entry = new DexEntryView(context, species, index);
            // PScreen_PokedexEntry: pbPlayCrySpecies(@species,@form) on open.
            if (index >= 0 && index < species.size() && species.get(index) != null) {
                context.audioManager().playCry(species.get(index).id);
            }
        }
        return false;
    }

    private List<PbsData.Species> speciesList() {
        List<PbsData.Species> species = new ArrayList<>(list.size());
        for (Entry e : list) {
            species.add(e.species);
        }
        return species;
    }

    private void keepVisible() {
        if (index < top) {
            top = index;
        } else if (index >= top + VISIBLE) {
            top = index - VISIBLE + 1;
        }
        top = Math.max(0, Math.min(top, Math.max(0, list.size() - VISIBLE)));
    }

    private Entry selected() {
        return list.isEmpty() ? null : list.get(Math.max(0, Math.min(index, list.size() - 1)));
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin) {
        render(b, a, f, skin, f);
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, MenuFont smallFont) {
        if (entry != null) {
            entry.render(b, a, f, smallFont);
            return;
        }
        float w = ScreenMetrics.logicalWidth();
        float h = ScreenMetrics.logicalHeight();
        drawBackground(b, a, w, h);
        Texture overlay = a.graphic("Pictures/Pokedex", "list_overlay");
        if (overlay != null) {
            b.draw(overlay, 0f, 0f, w, h);
        }
        drawSprite(b, a, h);
        drawInfo(b, f, h);
        drawList(b, a, f, h);
        if (!notice.isEmpty()) {
            f.draw(b, notice, 20f, h - 428f, INFO, INFO_SHADOW);
        }
    }

    /** ScrollingSprite: the bg_list tile, one pixel per frame. */
    private void drawBackground(SpriteBatch b, MenuAssets a, float w, float h) {
        Texture bg = a.graphic("Pictures/Pokedex", "bg_list");
        if (bg == null) {
            return;
        }
        scroll += 1f;
        float tw = bg.getWidth();
        float th = bg.getHeight();
        float px = scroll % tw;
        float py = scroll % th;
        for (float x = -px; x < w; x += tw) {
            for (float y = -py; y < h; y += th) {
                b.draw(bg, x, y, tw, th);
            }
        }
    }

    /** PokemonSprite Center at (110+64, 196+32), toned by seen/owned. */
    private void drawSprite(SpriteBatch b, MenuAssets a, float h) {
        Entry entry = selected();
        if (entry == null || entry.species == null) {
            return;
        }
        Texture sprite = a.graphic("Battlers", String.format("%03d", entry.species.id));
        if (sprite == null) {
            sprite = a.graphic("Battlers", entry.species.internalName);
        }
        if (sprite == null) {
            return;
        }
        // Tone: owned normal, seen (not owned) grayscale, unseen black.
        if (entry.owned) {
            b.setColor(Color.WHITE);
        } else if (entry.seen) {
            b.setColor(0.72f, 0.72f, 0.72f, 1f);
        } else {
            b.setColor(0f, 0f, 0f, 1f);
        }
        float size = Math.min(sprite.getWidth(), sprite.getHeight());
        b.draw(sprite, 174f - size / 2f, h - 228f - size / 2f, size, size);
        b.setColor(Color.WHITE);
    }

    /** pbRefresh text: dex name, species name, seen/owned counters. */
    private void drawInfo(SpriteBatch b, MenuFont f, float h) {
        f.draw(b, "宝可梦图鉴", 18f + 64f, h - 8f, DEX_NAME, OK_SHADOW);
        Entry entry = selected();
        if (entry != null) {
            f.drawCentered(b, entry.species.name, 114f + 64f, h - (332f + 64f), NAME, INFO_SHADOW);
        }
        f.draw(b, "发现的：", 30f + 32f, h - 57f, INFO, INFO_SHADOW);
        f.drawRight(b, String.valueOf(trainer.seen.size()), 220f + 32f, h - 57f, INFO, INFO_SHADOW);
        f.draw(b, "拥有的：", 280f + 32f, h - 57f, INFO, INFO_SHADOW);
        f.drawRight(b, String.valueOf(trainer.owned.size()), 470f + 32f, h - 57f, INFO, INFO_SHADOW);
    }

    /** Window_Pokedex: the scrollable list of 32px rows. */
    private void drawList(SpriteBatch b, MenuAssets a, MenuFont f, float h) {
        Texture cursor = a.graphic("Pictures/Pokedex", "cursor_list");
        Texture seenIcon = a.graphic("Pictures/Pokedex", "icon_seen");
        for (int i = top; i < list.size() && i < top + VISIBLE; i++) {
            int rowY = CONTENT_Y + (i - top) * ROW_H;
            Entry entry = list.get(i);
            if (i == index && cursor != null) {
                // drawCursor copies the whole cursor_list (244x44) at (rect.x, rect.y).
                b.draw(cursor, CONTENT_X, h - rowY - cursor.getHeight(),
                        cursor.getWidth(), cursor.getHeight());
            }
            String text;
            if (entry.seen) {
                text = String.format("%04d %s", entry.species.id, entry.species.name);
                if (entry.owned) {
                    Texture icon = pokemonIcon(a, entry.species);
                    if (icon != null) {
                        b.draw(icon, CONTENT_X + 7f, h - (rowY + 4f) - 32f, 32f, 32f, 0, 0,
                                Math.min(64, icon.getWidth()), Math.min(64, icon.getHeight()), false, false);
                    }
                } else if (seenIcon != null) {
                    b.draw(seenIcon, CONTENT_X + 11f, h - (rowY + 8f) - seenIcon.getHeight());
                }
            } else {
                text = String.format("%04d ----------", entry.species.id);
            }
            f.draw(b, text, CONTENT_X + 52f, h - (rowY + 6f), OK, OK_SHADOW);
        }
        drawSlider(b, a, h);
    }

    /** pbRefresh's slider (icon_slider at x=468+32+64*2). */
    private void drawSlider(SpriteBatch b, MenuAssets a, float h) {
        int total = list.size();
        if (total <= VISIBLE) {
            return;
        }
        Texture s = a.graphic("Pictures/Pokedex", "icon_slider");
        if (s == null) {
            return;
        }
        float x = 468f + 32f + 64f * 2f;
        if (top > 0) {
            b.draw(s, x, h - 118f - 30f, 40f, 30f, 0, 0, 40, 30, false, false);
        }
        if (top + VISIBLE < total) {
            b.draw(s, x, h - 307f - 30f, 40f, 30f, 0, 30, 40, 30, false, false);
        }
        float sliderHeight = 276f;
        float boxHeight = (float) Math.floor(sliderHeight * VISIBLE / total);
        boxHeight += Math.min((sliderHeight - boxHeight) / 2f, sliderHeight / 6f);
        boxHeight = Math.max((float) Math.floor(boxHeight), 40f);
        float y = 118f;
        int maxTop = Math.max(1, total - VISIBLE);
        y += (float) Math.floor((sliderHeight - boxHeight) * top / maxTop);
        b.draw(s, x, h - y - 8f, 40f, 8f, 40, 0, 40, 8, false, false);
        float remaining = boxHeight - 8f - 16f;
        int i = 0;
        while (i * 16 < remaining) {
            float seg = Math.min(remaining - i * 16f, 16f);
            b.draw(s, x, h - (y + 8f + i * 16f) - seg, 40f, seg, 40, 8, 40, (int) seg, false, false);
            i++;
        }
        b.draw(s, x, h - (y + boxHeight - 16f) - 16f, 40f, 16f, 40, 24, 40, 16, false, false);
    }

    private Texture pokemonIcon(MenuAssets a, PbsData.Species species) {
        Texture icon = a.icon(String.format("icon%03d", species.id));
        if (icon == null) {
            icon = a.icon("icon" + species.internalName);
        }
        return icon;
    }
}
