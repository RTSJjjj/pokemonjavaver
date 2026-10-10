package pokemon.runtime.ui.menu;

import pokemon.runtime.pokemon.BattlerBitmaps;
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
    /** {@code @gender}: 0 male, 1 female (the form page's picture). */
    private int gender;
    private int showShiny;
    /** {@code pbChooseForm}'s loop is running (UP/DOWN pick the form, B/C end it). */
    private boolean choosingForm;
    private int chooseIndex;
    private int dataShowType;
    private int movePage;
    private float scroll;
    private String notice = "";

    public DexEntryView(RuntimeContext context, List<PbsData.Species> dexlist, int index) {
        this.context = context;
        this.dexlist = dexlist;
        this.index = Math.max(0, Math.min(index, Math.max(0, dexlist.size() - 1)));
        loadLastSeen();
    }

    /** {@code @gender = formlastseen[@species][0] || 0; @form = formlastseen[@species][1] || 0} (:242-243). */
    private void loadLastSeen() {
        PbsData.Species s = species();
        int[] last = s == null ? null : context.gameState().trainer().formLastSeen.get(s.internalName);
        gender = last == null ? 0 : last[0];
        form = last == null ? 0 : last[1];
    }

    /** @return true when the player closed the entry (B). */
    public boolean update(InputManager input) {
        scroll += 1f;
        pokemon.runtime.audio.AudioManager audio = context.audioManager();
        if (choosingForm) {                                                   // :950-988 pbChooseForm
            List<FormEntry> available = availableForms();
            if (input.wasPressed(GameAction.UP)) {
                pokemon.runtime.audio.UiSounds.cursor(audio);
                chooseIndex = (chooseIndex + available.size() - 1) % available.size();
                applyChoice(available.get(chooseIndex));
            } else if (input.wasPressed(GameAction.DOWN)) {
                pokemon.runtime.audio.UiSounds.cursor(audio);
                chooseIndex = (chooseIndex + 1) % available.size();
                applyChoice(available.get(chooseIndex));
            } else if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
                choosingForm = false;                                         // :978-980 the last previewed form stays
            } else if (input.wasPressed(GameAction.CONFIRM)) {
                pokemon.runtime.audio.UiSounds.decision(audio);
                choosingForm = false;
            }
            return false;
        }
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            MenuSe.close(audio);                                              // 330_PokedexEntry_BW_Style:1016 pbPlayCloseMenuSE
            return true;
        }
        if (input.wasPressed(GameAction.SPECIAL)) {                           // :994-1013 Input::A
            audio.stopSe();
            if (page == 1 && seen()) {
                playCry(audio);                                               // pbPlayCrySpecies
            } else if (page == 4 && seen()) {                                 // :1008-1013 the shiny / super shiny picture
                pokemon.runtime.audio.UiSounds.cursor(audio);
                showShiny = 1 - showShiny;
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
            loadLastSeen();
            neighbourSound(audio);                                            // :1026-1034
        }
        if (input.wasPressed(GameAction.DOWN) && index < dexlist.size() - 1) {
            index++;
            loadLastSeen();
            neighbourSound(audio);                                            // :1036-1044
        }
        if (input.wasPressed(GameAction.CONFIRM)) {
            if (page == 4 && availableForms().size() > 1 && seen()) {
                pokemon.runtime.audio.UiSounds.decision(audio);               // :1019-1021
                List<FormEntry> available = availableForms();
                chooseIndex = 0;                                              // :951-957 the current gender / form
                for (int i = 0; i < available.size(); i++) {
                    if (available.get(i).gender == gender && available.get(i).index == form) {
                        chooseIndex = i;
                        break;
                    }
                }
                choosingForm = true;
                applyChoice(available.get(chooseIndex));
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
        if (page == 5) {
            drawAreaMap(b, a, s, w, h);                          // areamap, areahighlight, areaoverlay sit below the page overlay
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
        for (float x = -px; x < w; x += tw) {
            for (float y = h - th; y > -th; y -= th) {                   // ScrollingSprite scrolls along x only (y stays put, top aligned)
                b.draw(t, x, y, tw, th);
            }
        }
    }

    private void drawSprite(SpriteBatch b, MenuAssets a, PbsData.Species s, float x, float y,
                            float h, boolean back, boolean shiny) {
        drawSprite(b, a, s, x, y, h, back, shiny, false);
    }

    private void drawSprite(SpriteBatch b, MenuAssets a, PbsData.Species s, float x, float y,
                            float h, boolean back, boolean shiny, boolean superShiny) {
        Texture sprite = battler(a, s, back, shiny, superShiny);
        if (sprite == null) {
            return;
        }
        float size = Math.min(sprite.getWidth(), sprite.getHeight());
        b.draw(sprite, x - size / 2f, h - y - size / 2f, size, size);
    }

    /** {@code formlastseen[@species] = [gender, form]} for the chosen entry (:961-962). */
    private void applyChoice(FormEntry entry) {
        gender = entry.gender;
        form = entry.index;
        PbsData.Species s = species();
        if (s != null) context.gameState().trainer().formLastSeen.put(s.internalName, new int[] {gender, form});
    }

    /** {@code setSpeciesBitmap(species,female,form,shiny,...)}: pbCheckPokemonBitmapFiles' fallbacks for the page's gender / form. */
    private Texture battler(MenuAssets a, PbsData.Species s, boolean back, boolean shiny, boolean superShiny) {
        return BattlerBitmaps.find(s, back, gender == 1, shiny, superShiny, form,
                name -> a.graphic(back ? "BattlersBack" : "Battlers", name));
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
            f.draw(b, "特性" + (i + 1) + ":" + abilityName, 24f, h - (i == 0 ? y0 : i == 1 ? y1 : y2), WHITE, GRAY_SHADOW);
        }
        if (s.hiddenAbility != null && !s.hiddenAbility.isEmpty()) {
            f.draw(b, "隐藏特性:" + abilityName(s.hiddenAbility), 24f, h - y2, WHITE, GRAY_SHADOW);
        }
        int total = 0;
        for (int i = 0; i < 6; i++) {
            total += s.baseStat(i);
        }
        f.drawCentered(b, "种族值总和:" + total, 266f + 32f + 64f, h - 40f, HIGH, HIGH_SHADOW);
        f.draw(b, "HP:  " + s.baseStat(0), 184f + 32f + 64f, h - y0, WHITE, GRAY_SHADOW);
        f.draw(b, "攻击:" + s.baseStat(1), 184f + 32f + 64f, h - y1, WHITE, GRAY_SHADOW);
        f.draw(b, "防御:" + s.baseStat(2), 184f + 32f + 64f, h - y2, WHITE, GRAY_SHADOW);
        f.draw(b, "速度:" + s.baseStat(3), 266f + 32f + 64f, h - y0, WHITE, GRAY_SHADOW);
        f.draw(b, "特攻:" + s.baseStat(4), 266f + 32f + 64f, h - y1, WHITE, GRAY_SHADOW);
        f.draw(b, "特防:" + s.baseStat(5), 266f + 32f + 64f, h - y2, WHITE, GRAY_SHADOW);
        // Wild items.
        f.drawCentered(b, "野生持有物", 450f + 32f + 64f, h - 40f, HIGH, HIGH_SHADOW);
        String common = itemName(s.wildItems == null ? null : s.wildItems.common);
        String uncommon = itemName(s.wildItems == null ? null : s.wildItems.uncommon);
        String rare = itemName(s.wildItems == null ? null : s.wildItems.rare);
        if (common == null && uncommon == null && rare == null) {
            f.draw(b, "100%:----", 380f + 32f + 64f, h - 60f, WHITE, GRAY_SHADOW);
        } else if (java.util.Objects.equals(common, uncommon) && java.util.Objects.equals(uncommon, rare)) {
            f.draw(b, "100%:" + (common == null ? "----" : common), 380f + 32f + 64f, h - 60f, WHITE, GRAY_SHADOW);
        } else {
            f.draw(b, "50%: " + (common == null ? "----" : common), 380f + 32f + 64f, h - 60f, WHITE, GRAY_SHADOW);
            f.draw(b, "5%:  " + (uncommon == null ? "----" : uncommon), 380f + 32f + 64f, h - 80f, WHITE, GRAY_SHADOW);
            f.draw(b, "1%:  " + (rare == null ? "----" : rare), 380f + 32f + 64f, h - 100f, WHITE, GRAY_SHADOW);
        }
        // Move kind selector.
        int last = dataShowType > 0 ? dataShowType - 1 : DATA_MSG.length - 1;
        int next = dataShowType < DATA_MSG.length - 1 ? dataShowType + 1 : 0;
        f.drawRight(b, "◀[A]:" + DATA_MSG[last], ScreenMetrics.logicalWidth() / 2f - 80f - 32f, h - 122f, WHITE, GRAY_SHADOW);
        f.drawCentered(b, "【" + DATA_MSG[dataShowType] + "】", ScreenMetrics.logicalWidth() / 2f, h - 122f, HIGH, HIGH_SHADOW);
        f.draw(b, "[S]:" + DATA_MSG[next] + "▶", ScreenMetrics.logicalWidth() / 2f + 80f + 32f, h - 122f, WHITE, GRAY_SHADOW);
        drawMoveList(b, f, s, h);
    }

    private void drawMoveList(SpriteBatch b, MenuFont f, PbsData.Species s, float h) {
        if (!seen()) {
            f.drawCentered(b, "尚未记录招式数据，请发现宝可梦后查看",
                    ScreenMetrics.logicalWidth() / 2f, h - ScreenMetrics.logicalHeight() / 2f, WHITE, GRAY_SHADOW);
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
            f.drawCentered(b, lines.get(0), ScreenMetrics.logicalWidth() / 2f, h - ys[0], WHITE, GRAY_SHADOW);
            return;
        }
        for (int i = 0; i < lines.size() && i < 30; i++) {
            f.draw(b, lines.get(i), xs[i % 3], h - ys[i / 3], WHITE, GRAY_SHADOW);
        }
    }

    // ------------------------------------------------------------------
    // Page 3: Evolution.
    // ------------------------------------------------------------------
    private void drawEvolution(SpriteBatch b, MenuAssets a, MenuFont f, PbsData.Species s, float h) {
        String name = seen() ? s.name : "??????";
        f.drawRight(b, "进化", 88f, h - 4f, HEAD, HEAD_SHADOW);
        f.drawRight(b, name + "-" + formName(s), ScreenMetrics.logicalWidth() - 66f, h - 4f, HEAD, HEAD_SHADOW);
        f.drawCentered(b, "进化成谁", ScreenMetrics.logicalWidth() / 2f, h - 54f, WHITE, GRAY_SHADOW);
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
            f.drawCentered(b, "无法继续进化", ScreenMetrics.logicalWidth() / 2f, h - ys[0], WHITE, GRAY_SHADOW);
            return;
        }
        for (int i = 0; i < lines.size() && i < 22; i++) {
            f.draw(b, lines.get(i), xs[i / 11], h - ys[i % 11], WHITE, GRAY_SHADOW);
        }
    }

    // ------------------------------------------------------------------
    // Page 4: Forms.
    // ------------------------------------------------------------------
    private void drawForms(SpriteBatch b, MenuAssets a, MenuFont f, PbsData.Species s, float h) {
        f.drawRight(b, "形象", 88f, h - 4f, HEAD, HEAD_SHADOW);
        f.drawCentered(b, seen() ? s.name : "??????", 384f - 32f, h - 72f, WHITE, GRAY_SHADOW);
        f.drawCentered(b, currentFormName(s), 384f - 32f, h - (72f + 32f), WHITE, GRAY_SHADOW);
        if (seen()) {
            f.drawCentered(b, "[C]:切换形态 [Z]:切换" + SWITCH_MSG[showShiny],
                    ScreenMetrics.logicalWidth() / 1.6f, h - 4f, WHITE, HEAD_SHADOW);
        }
        drawSprite(b, a, s, 158f, 240f + 32f, h, false, false);               // "form": the normal picture
        if (showShiny == 0) {                                                  // "forms": shiny / "formss": super shiny
            drawSprite(b, a, s, 414f + 64f + 32f, 240f + 32f, h, false, true);
        } else {
            drawSprite(b, a, s, 414f + 64f + 32f, 240f + 32f, h, false, true, true);
        }
    }

    private String currentFormName(PbsData.Species s) {
        List<FormEntry> forms = availableForms();
        for (FormEntry e : forms) {
            if (e.index == form && e.gender == gender) return e.name;
        }
        for (FormEntry e : forms) {
            if (e.index == form) return e.name;
        }
        return "默认形态";
    }

    /** pbGetAvailableForms: form 0 plus any named alternate forms. */
    private List<FormEntry> availableForms() {
        // 330_PokedexEntry_BW_Style:278-322 pbGetAvailableForms: form 0 and every named form; a species with both genders
        // lists the unnamed form 0 once per gender.
        List<FormEntry> result = new ArrayList<>();
        PbsData.Species s = species();
        if (s == null) {
            return result;
        }
        String rate = s.genderRate == null ? "" : s.genderRate;
        boolean fixedGender = "AlwaysMale".equals(rate) || "AlwaysFemale".equals(rate) || "Genderless".equals(rate);
        int fixed = "AlwaysFemale".equals(rate) ? 1 : 0;
        List<int[]> possible = new ArrayList<>();      // [form, gender 0/1/2]
        List<String> possibleNames = new ArrayList<>();
        boolean multiforms = false;
        for (int i = 0; i < 30; i++) {
            PbsData.SpeciesForm f = i == 0 || context.pbsData() == null ? null : context.pbsData().form(s.internalName, i);
            if (i > 0 && f == null) continue;
            String formname = i == 0 ? (s.formName == null ? "" : s.formName) : (f.formName == null ? "" : f.formName);
            if (i != 0 && formname.isEmpty()) continue;                       // :288 i==0 || (formname && formname!="")
            if (i > 0) multiforms = true;
            if (fixedGender) {
                possible.add(new int[] {i, "Genderless".equals(rate) ? 2 : fixed});
                possibleNames.add(formname);
            } else {
                for (int g = 0; g < 2; g++) {
                    possible.add(new int[] {i, g});
                    possibleNames.add(formname);
                    if (!formname.isEmpty()) break;
                }
            }
        }
        for (int i = 0; i < possible.size(); i++) {
            int[] entry = possible.get(i);
            String name = possibleNames.get(i);
            if (name.isEmpty()) {
                name = entry[1] == 0 ? "雄性" : entry[1] == 1 ? "雌性" : (multiforms ? "默认形态" : "无性别");
            }
            result.add(new FormEntry(entry[0], name, entry[1] == 2 ? 0 : entry[1]));
        }
        return result;
    }

    private static final class FormEntry {
        final int index;
        final String name;
        final int gender;

        FormEntry(int index, String name, int gender) {
            this.index = index;
            this.name = name;
            this.gender = gender;
        }
    }

    // ------------------------------------------------------------------
    // Page 5: Area (the region map data is not modelled; the plugin's
    // "unknown habitat" fallback is shown).
    // ------------------------------------------------------------------
    private void drawArea(SpriteBatch b, MenuAssets a, MenuFont f, PbsData.Species s, float h) {
        if (areaPoints(s).isEmpty()) {                                          // :571-577 no square: "栖息地不明"
            Texture none = a.graphic("Pictures/Pokedex", "overlay_areanone");
            if (none != null) {
                drawImg(b, none, h, 108f + 32f, 148f);
            }
            f.drawCentered(b, "栖息地不明", ScreenMetrics.logicalWidth() / 2f, h - 152f, WHITE, GRAY_SHADOW);
        }
        f.drawCentered(b, "地区", 88f, h - 4f, HEAD, HEAD_SHADOW);
        String pkmnname = seen() ? s.name : "??????";
        f.drawCentered(b, pkmnname + "的分布", ScreenMetrics.logicalWidth() / 1.4f + 32f, h - 4f, HEAD, HEAD_SHADOW);
    }

    private static final int SQUARE = 16;                                   // PokemonRegionMap_Scene::SQUAREWIDTH / SQUAREHEIGHT
    private static final int MAP_WIDTH = 30;                                 // 1 + RIGHT - LEFT
    private static final Color POINT = new Color(0f, 248f / 255f, 248f / 255f, 1f);
    private static final Color POINT_HIGHLIGHT = new Color(192f / 255f, 248f / 255f, 248f / 255f, 1f);
    private String areaSpecies;
    private java.util.Set<Integer> areaCache = new java.util.HashSet<>();

    /** The region the page shows: the current map's own (@region falls back to it, :78-80); 0 without a position. */
    private int areaRegion() {
        PbsData pbs = context.pbsData();
        PbsData.Metadata meta = pbs == null ? null : pbs.mapMetadata(context.gameState().currentMapId());
        return meta != null && meta.mapPosition != null && meta.mapPosition.length >= 3 ? meta.mapPosition[0] : 0;
    }

    /**
     * 330_PokedexEntry_BW_Style:506-541 drawPageArea: the region-map squares of every map whose encounter tables hold the
     * species (the MetadataMapPosition square; a square whose town-map point has an off switch is hidden).
     * 登记: MetadataMapSize is not exported, so a map is always one square.
     */
    private java.util.Set<Integer> areaPoints(PbsData.Species s) {
        if (areaSpecies != null && areaSpecies.equals(s.internalName)) {
            return areaCache;
        }
        areaSpecies = s.internalName;
        areaCache = new java.util.HashSet<>();
        PbsData pbs = context.pbsData();
        if (pbs == null) {
            return areaCache;
        }
        int region = areaRegion();
        PbsData.TownMapRegion townRegion = pbs.townMap == null ? null : pbs.townMap.region(region);
        for (com.badlogic.gdx.utils.ObjectMap.Entry<String, PbsData.EncounterMap> entry : pbs.encounters) {
            if (!hasSpecies(entry.value, s.internalName)) {
                continue;
            }
            int mapId;
            try {
                mapId = Integer.parseInt(entry.key);
            } catch (NumberFormatException invalid) {
                continue;
            }
            PbsData.Metadata meta = pbs.mapMetadata(mapId);
            if (meta == null || meta.mapPosition == null || meta.mapPosition.length < 3 || meta.mapPosition[0] != region) {
                continue;
            }
            int x = meta.mapPosition[1];
            int y = meta.mapPosition[2];
            boolean show = true;
            if (townRegion != null) {
                for (PbsData.TownMapPoint point : townRegion.points) {                     // :520-524
                    if (point.x == x && point.y == y && point.switchId != null && point.switchId > 0
                            && !context.gameState().switches().get(point.switchId)) {
                        show = false;
                    }
                }
            }
            if (show && x >= 0 && y >= 0 && x < MAP_WIDTH) {
                areaCache.add(x + y * MAP_WIDTH);
            }
        }
        return areaCache;
    }

    /** pbFindEncounter(encounter, species) (:36-45). */
    private static boolean hasSpecies(PbsData.EncounterMap map, String species) {
        for (com.badlogic.gdx.utils.ObjectMap.Entry<String, com.badlogic.gdx.utils.Array<PbsData.EncounterEntry>> method : map.methods) {
            if (method.value == null) {
                continue;
            }
            for (PbsData.EncounterEntry entry : method.value) {
                if (entry != null && species.equals(entry.species)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The region map, its highlighted squares (pulsing, :226-231) and the area overlay (:81-94, :543-566). */
    private void drawAreaMap(SpriteBatch b, MenuAssets a, PbsData.Species s, float w, float h) {
        PbsData pbs = context.pbsData();
        PbsData.TownMapRegion townRegion = pbs == null || pbs.townMap == null ? null : pbs.townMap.region(areaRegion());
        Texture map = null;
        if (townRegion != null && townRegion.filename != null) {
            String name = townRegion.filename.endsWith(".png")
                    ? townRegion.filename.substring(0, townRegion.filename.length() - 4) : townRegion.filename;
            map = a.graphic("Pictures", name);
        }
        float mapWidth = map == null ? 0f : map.getWidth();
        float mapHeight = map == null ? 0f : map.getHeight();
        float originX = (w - mapWidth) / 2f;
        float originY = (h - mapHeight) / 2f;
        if (map != null) {
            b.setColor(Color.WHITE);
            b.draw(map, originX, h - originY - mapHeight);
        }
        java.util.Set<Integer> points = areaPoints(s);
        if (!points.isEmpty()) {
            int tick = (int) ((System.nanoTime() / 25_000_000L) % 40L);                // Graphics.frame_count % 40 at 40 fps
            int intensity = tick * 12;
            if (intensity > 240) {
                intensity = 480 - intensity;
            }
            float alpha = Math.max(0, Math.min(255, intensity)) / 255f;
            for (int j : points) {
                float x = originX + (j % MAP_WIDTH) * SQUARE;
                float y = originY + (j / MAP_WIDTH) * SQUARE;
                float gy = h - y - SQUARE;
                MenuPanel.fill(b, a, x, gy, SQUARE, SQUARE, POINT.r, POINT.g, POINT.b, alpha);
                float hr = POINT_HIGHLIGHT.r, hg = POINT_HIGHLIGHT.g, hb = POINT_HIGHLIGHT.b;
                if (!points.contains(j - MAP_WIDTH)) {                                  // :555-557 top edge
                    MenuPanel.fill(b, a, x, gy + SQUARE, SQUARE, 2f, hr, hg, hb, alpha);
                }
                if (!points.contains(j + MAP_WIDTH)) {                                  // :558-560 bottom edge
                    MenuPanel.fill(b, a, x, gy - 2f, SQUARE, 2f, hr, hg, hb, alpha);
                }
                if (j % MAP_WIDTH == 0 || !points.contains(j - 1)) {                    // :561-563 left edge
                    MenuPanel.fill(b, a, x - 2f, gy, 2f, SQUARE, hr, hg, hb, alpha);
                }
                if ((j + 1) % MAP_WIDTH == 0 || !points.contains(j + 1)) {              // :564-566 right edge
                    MenuPanel.fill(b, a, x + SQUARE, gy, 2f, SQUARE, hr, hg, hb, alpha);
                }
            }
        }
        Texture overlay = a.graphic("Pictures/Pokedex", "overlay_area");
        if (overlay != null) {
            b.setColor(Color.WHITE);
            b.draw(overlay, 0f, 0f, w, h);
        }
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
