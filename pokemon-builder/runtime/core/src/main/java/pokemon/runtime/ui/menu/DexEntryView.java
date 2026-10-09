package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PbsData;

import java.util.ArrayList;
import java.util.List;

/**
 * BW PokedexEntry: one species' entry screen, transcribed from
 * {@code PokemonPokedexInfo_Scene}: page 1 Info, 2 Data, 3 Evolution, 4 Forms,
 * 5 Area; LEFT/RIGHT change page, UP/DOWN change species, C picks a form.
 *
 * <p>Coordinates are the plugin's top-origin values, converted with
 * {@code screenHeight - y}. Text alignment follows {@code pbDrawTextPositions}:
 * 1 = right, 2 = centre, otherwise left.</p>
 */
public final class DexEntryView {

    private static final Color WHITE = new Color(1f, 1f, 1f, 1f);
    private static final Color GRAY_SHADOW = new Color(165f / 255f, 165f / 255f, 173f / 255f, 1f);
    private static final Color NAME = new Color(82f / 255f, 82f / 255f, 90f / 255f, 1f);
    private static final Color HEAD = new Color(255f / 255f, 255f / 255f, 255f / 255f, 1f);
    private static final Color HEAD_SHADOW = new Color(115f / 255f, 115f / 255f, 115f / 255f, 1f);
    private static final Color HIGH = new Color(255f / 255f, 255f / 255f, 192f / 255f, 1f);
    private static final Color HIGH_SHADOW = new Color(160f / 255f, 160f / 255f, 115f / 255f, 1f);
    private static final String[] DATA_MSG = {"先天招式", "升级招式", "机器招式", "蛋招式"};
    private static final String[] SWITCH_MSG = {"超闪", "异色"};

    private final RuntimeContext context;
    private final List<PbsData.Species> dexlist;
    private int index;
    private int page = 1;
    private int form;
    private int showShiny;
    private int dataShowType;
    private int movePage;
    private float scroll;
    private String notice = "";

    public DexEntryView(RuntimeContext context, List<PbsData.Species> dexlist, int index) {
        this.context = context;
        this.dexlist = dexlist;
        this.index = Math.max(0, Math.min(index, Math.max(0, dexlist.size() - 1)));
    }

    /** @return true when the player closed the entry (B). */
    public boolean update(InputManager input) {
        scroll += 1f;
        pokemon.runtime.audio.AudioManager audio = context.audioManager();
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            MenuSe.close(audio);                                              // 330_PokedexEntry_BW_Style:1016 pbPlayCloseMenuSE
            return true;
        }
        if (input.wasPressed(GameAction.SPECIAL)) {                           // :994-1013 Input::A
            audio.stopSe();
            if (page == 1 && seen()) {
                playCry(audio);                                               // pbPlayCrySpecies
            } else if (page == 2 && dataShowType == 2 && seen()) {
                movePage++;
                pokemon.runtime.audio.UiSounds.cursor(audio);
            }
        }
        if (input.wasPressed(GameAction.LEFT)) {
            page--;
            if (page < 1) page = 5;
            dataShowType = 0;
            pokemon.runtime.audio.UiSounds.named(audio, "GUI naming tab swap start");   // :1049-1052
        }
        if (input.wasPressed(GameAction.RIGHT)) {
            page++;
            if (page > 5) page = 1;
            dataShowType = 0;
            pokemon.runtime.audio.UiSounds.named(audio, "GUI naming tab swap start");   // :1057-1061
        }
        if (input.wasPressed(GameAction.UP) && index > 0) {
            index--;
            form = 0;
            neighbourSound(audio);                                            // :1026-1034
        }
        if (input.wasPressed(GameAction.DOWN) && index < dexlist.size() - 1) {
            index++;
            form = 0;
            neighbourSound(audio);                                            // :1036-1044
        }
        if (input.wasPressed(GameAction.CONFIRM)) {
            if (page == 4 && availableForms().size() > 1) {
                pokemon.runtime.audio.UiSounds.decision(audio);               // :1019-1021
                form = (form + 1) % availableForms().size();
            } else if (page == 2 && dataShowType == 2) {
                movePage++;
            }
        }
        if (input.wasPressed(GameAction.RUN)) {
            // The plugin's L/R cycle the move list kind on the data page.
            if (page == 2 && seen()) {
                dataShowType = (dataShowType + 1) % DATA_MSG.length;
                movePage = 0;
                pokemon.runtime.audio.UiSounds.named(audio, "GUI naming tab swap start");   // :1064-1075
            }
        }
        return false;
    }

    /** pbSEStop, then the cry on the first page of a seen species, else the cursor sound. */
    private void neighbourSound(pokemon.runtime.audio.AudioManager audio) {
        audio.stopSe();
        if (page == 1 && seen()) {
            playCry(audio);
        } else {
            pokemon.runtime.audio.UiSounds.cursor(audio);
        }
    }

    private void playCry(pokemon.runtime.audio.AudioManager audio) {
        PbsData.Species s = species();
        if (s != null) {
            audio.playCry(s.id);
        }
    }

    private PbsData.Species species() {
        return dexlist.isEmpty() ? null : dexlist.get(Math.max(0, Math.min(index, dexlist.size() - 1)));
    }

    private boolean seen() {
        PbsData.Species s = species();
        return s != null && context.gameState().trainer().seen.contains(s.internalName);
    }

    private boolean owned() {
        PbsData.Species s = species();
        return s != null && context.gameState().trainer().owned.contains(s.internalName);
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, MenuFont smallFont) {
        PbsData.Species s = species();
        if (s == null) {
            return;
        }
        float w = ScreenMetrics.logicalWidth();
        float h = ScreenMetrics.logicalHeight();
        String bg = page == 1 ? "bg_info" : page == 3 ? "bg_data" : page == 4 ? "bg_forms" : page == 5 ? "bg_area" : "bg_data";
        String overlay = page == 1 ? "info_overlay" : page == 3 ? "evo_overlay" : page == 4 ? "forms_overlay" : page == 5 ? "map_overlay" : "data_overlay";
        Texture background = a.graphic("Pictures/Pokedex", bg);
        if (background != null) {
            drawScrolling(b, background, w, h);
        }
        Texture frame = a.graphic("Pictures/Pokedex", overlay);
        if (frame != null) {
            b.draw(frame, 0f, 0f, w, h);
        }
        switch (page) {
            case 1: drawInfo(b, a, f, s, h); break;
            case 2: drawData(b, a, smallFont, s, h); break;
            case 3: drawEvolution(b, a, smallFont, s, h); break;
            case 4: drawForms(b, a, f, s, h); break;
            default: drawArea(b, a, f, s, h); break;
        }
        if (!notice.isEmpty()) {
            f.draw(b, notice, 20f, h - 432f, NAME, GRAY_SHADOW);
        }
    }

    private void drawScrolling(SpriteBatch b, Texture t, float w, float h) {
        float tw = t.getWidth();
        float th = t.getHeight();
        float px = scroll % tw;
        float py = scroll % th;
        for (float x = -px; x < w; x += tw) {
            for (float y = -py; y < h; y += th) {
                b.draw(t, x, y, tw, th);
            }
        }
    }

    private void drawSprite(SpriteBatch b, MenuAssets a, PbsData.Species s, float x, float y,
                            float h, boolean back, boolean shiny) {
        Texture sprite = battler(a, s, back, shiny);
        if (sprite == null) {
            return;
        }
        float size = Math.min(sprite.getWidth(), sprite.getHeight());
        b.draw(sprite, x - size / 2f, h - y - size / 2f, size, size);
    }

    private Texture battler(MenuAssets a, PbsData.Species s, boolean back, boolean shiny) {
        String id = String.format("%03d", s.id) + (shiny ? "s" : "");
        Texture t = a.graphic(back ? "BattlersBack" : "Battlers", id);
        if (t == null) {
            t = a.graphic(back ? "BattlersBack" : "Battlers", String.format("%03d", s.id));
        }
        return t;
    }

    // ------------------------------------------------------------------
    // Page 1: Info.
    // ------------------------------------------------------------------
    private void drawInfo(SpriteBatch b, MenuAssets a, MenuFont f, PbsData.Species s, float h) {
        drawSprite(b, a, s, 98f + 64f, 112f, h, false, false);
        String dexText = String.format("%04d", s.id);
        if (!seen()) {
            f.draw(b, dexText + " ??????", 328f + 64f, h - 23f, NAME, GRAY_SHADOW);
            f.draw(b, "？？？宝可梦", 348f + 64f, h - 58f, NAME, GRAY_SHADOW);
            f.drawRight(b, "???.? m", 489f + 32f + 64f, h - 138f, NAME, GRAY_SHADOW);
            f.drawRight(b, "???.? kg", 489f + 32f + 64f, h - 168f, NAME, GRAY_SHADOW);
            return;
        }
        f.draw(b, dexText + " " + s.name, 328f + 64f, h - 23f, NAME, GRAY_SHADOW);
        f.draw(b, "身高", 334f + 64f, h - 138f, NAME, GRAY_SHADOW);
        f.draw(b, "体重", 334f + 64f, h - 168f, NAME, GRAY_SHADOW);
        if (!owned()) {
            f.draw(b, "？？？宝可梦", 348f + 64f, h - 58f, NAME, GRAY_SHADOW);
            f.drawRight(b, "???.? m", 489f + 32f + 64f, h - 138f, NAME, GRAY_SHADOW);
            f.drawRight(b, "???.? kg", 489f + 32f + 64f, h - 168f, NAME, GRAY_SHADOW);
            return;
        }
        String kind = s.kind == null || s.kind.isEmpty() ? "" : s.kind;
        f.drawCentered(b, kind + "宝可梦", 420f + 64f, h - 58f, NAME, GRAY_SHADOW);
        f.drawRight(b, String.format("%.1f m", s.height), 510f + 64f, h - 138f, NAME, GRAY_SHADOW);
        f.drawRight(b, String.format("%.1f kg", s.weight), 510f + 64f, h - 168f, NAME, GRAY_SHADOW);
        // Entry text at (39, 216+64) width 672-80, 4 lines.
        drawWrapped(b, f, s.pokedex, 39f, 216f + 64f, 672f - 80f, 4, h, WHITE, GRAY_SHADOW);
        // Shape icon (60x60) at (260+64, 100).
        Texture shapes = a.graphic("Pictures/Pokedex", "icon_shapes");
        if (shapes != null && s.shape > 0) {
            drawImg(b, shapes, h, 260f + 64f, 100f, 60, 60, 0, (s.shape - 1) * 60);
        }
        // Owned icon (28x28) at (261+64, 19).
        Texture own = a.graphic("Pictures/Pokedex", "icon_own");
        if (own != null) {
            drawImg(b, own, h, 261f + 64f, 19f);
        }
        // Type icons (72x32) at (330+64,97) and (397+64,97).
        Texture types = a.graphic("Pictures/Pokedex", "icon_types");
        if (types != null && s.types.size > 0) {
            PbsData.TypeInfo t1 = typeInfo(s.types.get(0));
            if (t1 != null) {
                drawImg(b, types, h, 330f + 64f, 97f, 72, 32, 0, t1.id * 32);
            }
            if (s.types.size > 1 && !s.types.get(0).equals(s.types.get(1))) {
                PbsData.TypeInfo t2 = typeInfo(s.types.get(1));
                if (t2 != null) {
                    drawImg(b, types, h, 397f + 64f, 97f, 72, 32, 0, t2.id * 32);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Page 2: Data.
    // ------------------------------------------------------------------
    private void drawData(SpriteBatch b, MenuAssets a, MenuFont f, PbsData.Species s, float h) {
        String name = seen() ? s.name : "??????";
        String formName = formName(s);
        f.drawRight(b, "数据", 88f, h - 4f, HEAD, HEAD_SHADOW);
        f.drawRight(b, name + "-" + formName, ScreenMetrics.logicalWidth() - 66f, h - 4f, HEAD, HEAD_SHADOW);
        // 特性 heading (100,40) centre.
        f.drawCentered(b, "特性", 100f, h - 40f, HIGH, HIGH_SHADOW);
        int y0 = 60;
        int y1 = 80;
        int y2 = 100;
        List<String> abilities = new ArrayList<>();
        if (s.abilities != null) {
            for (String ability : s.abilities) {
                if (ability != null && !ability.isEmpty()) abilities.add(ability);
            }
        }
        for (int i = 0; i < abilities.size() && i < 3; i++) {
            String abilityName = abilityName(abilities.get(i));
            f.draw(b, "特性" + (i + 1) + ":" + abilityName, 24f, h - (i == 0 ? y0 : i == 1 ? y1 : y2), NAME, GRAY_SHADOW);
        }
        if (s.hiddenAbility != null && !s.hiddenAbility.isEmpty()) {
            f.draw(b, "隐藏特性:" + abilityName(s.hiddenAbility), 24f, h - y2, NAME, GRAY_SHADOW);
        }
        int total = 0;
        for (int i = 0; i < 6; i++) {
            total += s.baseStat(i);
        }
        f.drawCentered(b, "种族值总和:" + total, 266f + 32f + 64f, h - 40f, HIGH, HIGH_SHADOW);
        f.draw(b, "HP:  " + s.baseStat(0), 184f + 32f + 64f, h - y0, NAME, GRAY_SHADOW);
        f.draw(b, "攻击:" + s.baseStat(1), 184f + 32f + 64f, h - y1, NAME, GRAY_SHADOW);
        f.draw(b, "防御:" + s.baseStat(2), 184f + 32f + 64f, h - y2, NAME, GRAY_SHADOW);
        f.draw(b, "速度:" + s.baseStat(3), 266f + 32f + 64f, h - y0, NAME, GRAY_SHADOW);
        f.draw(b, "特攻:" + s.baseStat(4), 266f + 32f + 64f, h - y1, NAME, GRAY_SHADOW);
        f.draw(b, "特防:" + s.baseStat(5), 266f + 32f + 64f, h - y2, NAME, GRAY_SHADOW);
        // Wild items.
        f.drawCentered(b, "野生持有物", 450f + 32f + 64f, h - 40f, HIGH, HIGH_SHADOW);
        String common = itemName(s.wildItems == null ? null : s.wildItems.common);
        String uncommon = itemName(s.wildItems == null ? null : s.wildItems.uncommon);
        String rare = itemName(s.wildItems == null ? null : s.wildItems.rare);
        if (common == null && uncommon == null && rare == null) {
            f.draw(b, "100%:----", 380f + 32f + 64f, h - 60f, NAME, GRAY_SHADOW);
        } else if (java.util.Objects.equals(common, uncommon) && java.util.Objects.equals(uncommon, rare)) {
            f.draw(b, "100%:" + (common == null ? "----" : common), 380f + 32f + 64f, h - 60f, NAME, GRAY_SHADOW);
        } else {
            f.draw(b, "50%: " + (common == null ? "----" : common), 380f + 32f + 64f, h - 60f, NAME, GRAY_SHADOW);
            f.draw(b, "5%:  " + (uncommon == null ? "----" : uncommon), 380f + 32f + 64f, h - 80f, NAME, GRAY_SHADOW);
            f.draw(b, "1%:  " + (rare == null ? "----" : rare), 380f + 32f + 64f, h - 100f, NAME, GRAY_SHADOW);
        }
        // Move kind selector.
        int last = dataShowType > 0 ? dataShowType - 1 : DATA_MSG.length - 1;
        int next = dataShowType < DATA_MSG.length - 1 ? dataShowType + 1 : 0;
        f.drawRight(b, "◀[A]:" + DATA_MSG[last], ScreenMetrics.logicalWidth() / 2f - 80f - 32f, h - 122f, NAME, GRAY_SHADOW);
        f.drawCentered(b, "【" + DATA_MSG[dataShowType] + "】", ScreenMetrics.logicalWidth() / 2f, h - 122f, HIGH, HIGH_SHADOW);
        f.draw(b, "[S]:" + DATA_MSG[next] + "▶", ScreenMetrics.logicalWidth() / 2f + 80f + 32f, h - 122f, NAME, GRAY_SHADOW);
        drawMoveList(b, f, s, h);
    }

    private void drawMoveList(SpriteBatch b, MenuFont f, PbsData.Species s, float h) {
        if (!seen()) {
            f.drawCentered(b, "尚未记录招式数据，请发现宝可梦后查看",
                    ScreenMetrics.logicalWidth() / 2f, h - ScreenMetrics.logicalHeight() / 2f, NAME, GRAY_SHADOW);
            return;
        }
        float[] xs = {24f, 24f + ScreenMetrics.logicalWidth() / 3f, 24f + ScreenMetrics.logicalWidth() / 3f * 2f};
        int[] ys = {144, 166, 188, 210, 232, 254, 276, 298, 320, 340};
        List<String> lines = new ArrayList<>();
        switch (dataShowType) {
            case 0:
                for (PbsData.LearnMove m : s.moves) {
                    if (m.level > 1) continue;
                    lines.add(m.level == 0 ? "进化:" + moveName(m.move) : m.level + "级:" + moveName(m.move));
                }
                if (lines.isEmpty()) lines.add("没有先天招式");
                break;
            case 1:
                for (PbsData.LearnMove m : s.moves) {
                    if (m.level <= 1) continue;
                    lines.add(m.level + "级:" + moveName(m.move));
                }
                if (lines.isEmpty()) lines.add("没有升级招式");
                break;
            case 2:
                lines.add("没有可学习的学习器招式");
                break;
            default:
                for (String m : s.eggMoves) {
                    lines.add(moveName(m));
                }
                if (lines.isEmpty()) lines.add("没有蛋招式");
                break;
        }
        if (lines.size() == 1 && lines.get(0).startsWith("没有")) {
            f.drawCentered(b, lines.get(0), ScreenMetrics.logicalWidth() / 2f, h - ys[0], NAME, GRAY_SHADOW);
            return;
        }
        for (int i = 0; i < lines.size() && i < 30; i++) {
            f.draw(b, lines.get(i), xs[i % 3], h - ys[i / 3], NAME, GRAY_SHADOW);
        }
    }

    // ------------------------------------------------------------------
    // Page 3: Evolution.
    // ------------------------------------------------------------------
    private void drawEvolution(SpriteBatch b, MenuAssets a, MenuFont f, PbsData.Species s, float h) {
        String name = seen() ? s.name : "??????";
        f.drawRight(b, "进化", 88f, h - 4f, HEAD, HEAD_SHADOW);
        f.drawRight(b, name + "-" + formName(s), ScreenMetrics.logicalWidth() - 66f, h - 4f, HEAD, HEAD_SHADOW);
        f.drawCentered(b, "进化成谁", ScreenMetrics.logicalWidth() / 2f, h - 54f, NAME, GRAY_SHADOW);
        float[] xs = {16f, ScreenMetrics.logicalWidth() / 2f + 16f};
        int[] ys = {96, 118, 140, 162, 184, 206, 228, 250, 272, 294, 316};
        List<String> lines = new ArrayList<>();
        if (s.evolutions != null) {
            for (PbsData.Evolution evo : s.evolutions) {
                if (evo.species == null) continue;
                PbsData.Species target = context.pbsData() == null ? null : context.pbsData().species(evo.species);
                String targetName = target != null ? target.name : evo.species;
                String method = evoMethodName(evo.method);
                boolean hasParam = evo.parameter != null && !evo.parameter.isEmpty();
                lines.add(hasParam ? method + evoParameter(evo) + "→" + targetName : method + "→" + targetName);
            }
        }
        if (lines.isEmpty()) {
            f.drawCentered(b, "无法继续进化", ScreenMetrics.logicalWidth() / 2f, h - ys[0], NAME, GRAY_SHADOW);
            return;
        }
        for (int i = 0; i < lines.size() && i < 22; i++) {
            f.draw(b, lines.get(i), xs[i / 11], h - ys[i % 11], NAME, GRAY_SHADOW);
        }
    }

    // ------------------------------------------------------------------
    // Page 4: Forms.
    // ------------------------------------------------------------------
    private void drawForms(SpriteBatch b, MenuAssets a, MenuFont f, PbsData.Species s, float h) {
        f.drawRight(b, "形象", 88f, h - 4f, HEAD, HEAD_SHADOW);
        f.drawCentered(b, seen() ? s.name : "??????", 384f - 32f, h - 72f, NAME, GRAY_SHADOW);
        f.drawCentered(b, currentFormName(s), 384f - 32f, h - (72f + 32f), NAME, GRAY_SHADOW);
        if (seen()) {
            f.drawCentered(b, "[C]:切换形态 [Z]:切换" + SWITCH_MSG[showShiny],
                    ScreenMetrics.logicalWidth() / 1.6f, h - 4f, WHITE, HEAD_SHADOW);
        }
        if (showShiny == 0) {
            drawSprite(b, a, s, 158f, 240f + 32f, h, false, false);
            drawSprite(b, a, s, 414f + 64f + 32f, 240f + 32f, h, false, true);
        } else {
            drawSprite(b, a, s, 158f, 240f + 32f, h, false, true);
            drawSprite(b, a, s, 414f + 64f + 32f, 240f + 32f, h, false, true);
        }
    }

    private String currentFormName(PbsData.Species s) {
        List<FormEntry> forms = availableForms();
        for (FormEntry e : forms) {
            if (e.index == form) return e.name;
        }
        return "默认形态";
    }

    /** pbGetAvailableForms: form 0 plus any named alternate forms. */
    private List<FormEntry> availableForms() {
        List<FormEntry> result = new ArrayList<>();
        PbsData.Species s = species();
        if (s == null) {
            return result;
        }
        result.add(new FormEntry(0, formName(s)));
        if (context.pbsData() != null) {
            for (int i = 1; i < 30; i++) {
                PbsData.SpeciesForm f = context.pbsData().form(s.internalName, i);
                if (f == null) continue;
                String name = f.formName != null && !f.formName.isEmpty() ? f.formName : "形态" + i;
                result.add(new FormEntry(i, name));
            }
        }
        return result;
    }

    private static final class FormEntry {
        final int index;
        final String name;

        FormEntry(int index, String name) {
            this.index = index;
            this.name = name;
        }
    }

    // ------------------------------------------------------------------
    // Page 5: Area (the region map data is not modelled; the plugin's
    // "unknown habitat" fallback is shown).
    // ------------------------------------------------------------------
    private void drawArea(SpriteBatch b, MenuAssets a, MenuFont f, PbsData.Species s, float h) {
        Texture none = a.graphic("Pictures/Pokedex", "overlay_areanone");
        if (none != null) {
            drawImg(b, none, h, 108f + 32f, 148f);
        }
        f.drawCentered(b, "栖息地不明", ScreenMetrics.logicalWidth() / 2f, h - 152f, WHITE, GRAY_SHADOW);
        f.drawCentered(b, "地区", 88f, h - 4f, HEAD, HEAD_SHADOW);
        String pkmnname = seen() ? s.name : "??????";
        f.drawCentered(b, pkmnname + "的分布", ScreenMetrics.logicalWidth() / 1.4f + 32f, h - 4f, HEAD, HEAD_SHADOW);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private String formName(PbsData.Species s) {
        PbsData.SpeciesForm f = context.pbsData() == null ? null : context.pbsData().form(s.internalName, form);
        if (f != null && f.formName != null && !f.formName.isEmpty()) {
            return f.formName;
        }
        if (s.formName != null && !s.formName.isEmpty()) {
            return s.formName;
        }
        return "默认形态";
    }

    private PbsData.TypeInfo typeInfo(String type) {
        return context.pbsData() == null ? null : context.pbsData().type(type);
    }

    private String abilityName(String ability) {
        PbsData.Ability a = context.pbsData() == null ? null : context.pbsData().ability(ability);
        return a != null && a.name != null ? a.name : (ability == null ? "—" : ability);
    }

    private String itemName(String item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        PbsData.Item i = context.pbsData() == null ? null : context.pbsData().item(item);
        return i != null && i.name != null ? i.name : item;
    }

    private String moveName(String move) {
        PbsData.Move m = context.pbsData() == null ? null : context.pbsData().move(move);
        return m != null && m.name != null ? m.name : (move == null ? "—" : move);
    }

    /** PBEvolution.getName over the handful of methods this project's PBS uses. */
    private static String evoMethodName(String method) {
        if (method == null) return "";
        switch (method) {
            case "Level": return "等级";
            case "Item": case "ItemMale": case "ItemFemale": return "使用";
            case "Trade": case "TradeItem": case "TradeSpecies": return "通讯交换";
            case "Happiness": return "亲密度";
            case "Location": return "在";
            case "Region": return "在";
            case "HasInParty": return "队伍中有";
            case "MoveType": return "";
            case "LevelMale": case "LevelFemale": return "等级";
            case "Beauty": case "Cool": case "Cute": case "Smart": case "Tough": return "华丽度";
            default: return method;
        }
    }

    private String evoParameter(PbsData.Evolution evo) {
        String method = evo.method == null ? "" : evo.method;
        String param = evo.parameter;
        if (param == null || param.isEmpty()) {
            return "";
        }
        if (method.contains("Item") || method.equals("LastEvolution")) {
            String item = itemName(upcase(param));
            return item == null ? param : item;
        }
        if (method.contains("Move")) {
            return moveName(upcase(param));
        }
        if (method.equals("HasInParty") || method.equals("TradeSpecies")) {
            PbsData.Species s = context.pbsData() == null ? null : context.pbsData().species(param);
            return s != null ? s.name : param;
        }
        return param;
    }

    private static String upcase(String value) {
        return value == null ? null : value.toUpperCase(java.util.Locale.ROOT);
    }

    private static void drawImg(SpriteBatch b, Texture t, float h, float x, float topY) {
        b.draw(t, x, h - topY - t.getHeight(), t.getWidth(), t.getHeight());
    }

    private static void drawImg(SpriteBatch b, Texture t, float h, float x, float topY,
                                float w, float hh, int sx, int sy) {
        b.draw(t, x, h - topY - hh, w, hh, sx, sy, (int) w, (int) hh, false, false);
    }

    private void drawWrapped(SpriteBatch b, MenuFont f, String text, float x, float topY,
                             float width, int maxLines, float h, Color main, Color shadow) {
        if (text == null) {
            return;
        }
        String cleaned = text.replace("\r", "").replace("\n", " ");
        StringBuilder line = new StringBuilder();
        float lineHeight = f.lineHeight();
        int drawn = 0;
        for (int i = 0; i < cleaned.length() && drawn < maxLines; i++) {
            char c = cleaned.charAt(i);
            if (f.width(line.toString() + c) > width && line.length() > 0) {
                f.draw(b, line.toString(), x, h - (topY + drawn * lineHeight), main, shadow);
                drawn++;
                line.setLength(0);
            }
            line.append(c);
        }
        if (drawn < maxLines && line.length() > 0) {
            f.draw(b, line.toString(), x, h - (topY + drawn * lineHeight), main, shadow);
        }
    }
}
