package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.app.StoragePort;
import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.ui.WindowSkin;

import java.time.LocalDateTime;

/**
 * BW Save Screen (BGSTYLE 1 = BW2): the {@code Save/bw2_background}, the party
 * icon row, the clock / badges / dex / map / play time / date text, and the
 * {@code 003_MSF_UI_Save} slot choice (a message window plus a slot list).
 *
 * <p>Coordinates are the plugin's top-origin values, converted with
 * {@code screenHeight - y}. Text alignment follows {@code pbDrawTextPositions}:
 * 1 = right, 2 = centre, otherwise left.</p>
 */
public final class SaveView {

    public enum Result {
        NONE,
        PICKED,
        CANCELLED
    }

    private static final Color BASE = new Color(231f / 255f, 231f / 255f, 231f / 255f, 1f);
    private static final Color SHADOW = new Color(140f / 255f, 140f / 255f, 140f / 255f, 1f);
    private static final Color CURSOR_MAIN = new Color(1f, 216f / 255f, 0f, 1f);

    private final RuntimeContext context;
    private final Array<SaveSlots.Slot> slots = new Array<>();
    private int index;
    private String notice = "";

    public SaveView(RuntimeContext context) {
        this.context = context;
    }

    public void refresh(StoragePort storage, GameDatabase database) {
        slots.clear();
        slots.addAll(SaveSlots.list(storage, database));
        index = Math.max(0, Math.min(index, slots.size - 1));
    }

    public SaveSlots.Slot selected() {
        return slots.size == 0 ? null : slots.get(index);
    }

    public Result update(InputManager input, AudioManager audio) {
        if (slots.size == 0) {
            return Result.NONE;
        }
        if (input.wasPressed(GameAction.UP)) {
            index = Math.floorMod(index - 1, slots.size);
            MenuSe.cursor(audio);
        }
        if (input.wasPressed(GameAction.DOWN)) {
            index = Math.floorMod(index + 1, slots.size);
            MenuSe.cursor(audio);
        }
        if (input.wasPressed(GameAction.CONFIRM)) {
            MenuSe.decision(audio);
            return Result.PICKED;
        }
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            MenuSe.buzzer(audio);
            return Result.CANCELLED;
        }
        return Result.NONE;
    }

    public void notice(String text) {
        notice = text;
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, WindowSkin speech, MenuFont smallFont) {
        float w = ScreenMetrics.logicalWidth();
        float h = ScreenMetrics.logicalHeight();
        // BW2 background (BGSTYLE 1).
        Texture bg = a.graphic("Pictures/Save", "bw2_background");
        if (bg != null) {
            b.draw(bg, 0f, 0f, w, h);
        }
        // Party icon row: PokemonIconSprite at (96+64+64*i, 122+32).
        Array<Pokemon> party = context.gameState().trainer().party.members();
        for (int i = 0; i < party.size && i < 6; i++) {
            Texture icon = pokemonIcon(a, party.get(i));
            if (icon == null) {
                continue;
            }
            int size = icon.getHeight();
            b.draw(icon, 96f + 64f + 64f * i, h - (122f + 32f) - size, size, size,
                    0, 0, size, size, false, false);
        }
        // Clock, badges, dex, map, play time, date (BW2 colours).
        LocalDateTime now = LocalDateTime.now();
        f.drawCentered(b, String.format("%02d : %02d", now.getHour(), now.getMinute()), 288f + 64f, h - 4f, BASE, SHADOW);
        f.draw(b, "徽章：" + context.gameState().trainer().badges.size(), 80f + 64f, h - (203f + 32f), BASE, SHADOW);
        f.draw(b, "图鉴：" + context.gameState().trainer().seen.size(), 288f + 64f, h - (203f + 32f), BASE, SHADOW);
        f.draw(b, mapName(), 80f + 64f, h - (88f + 32f), BASE, SHADOW);
        f.draw(b, "游戏时间：" + playTime(), 80f + 64f, h - (233f + 32f), BASE, SHADOW);
        f.draw(b, String.format("%d年%d月%d日", now.getYear(), now.getMonthValue(), now.getDayOfMonth()),
                78f + 64f, h - (56f + 32f), BASE, SHADOW);
        // pbMessage: pbCreateMessageWindow -> pbBottomLeftLines(msgwindow,2)
        // = (0, height-(borderY+2*32), width, borderY+2*32); speech bw 1 borderY=32
        // so the window is (0, 352, 672, 96).
        float msgH = 32f + 2f * 32f;
        MenuPanel.window(b, a, speech, 0f, 0f, w, msgH);
        Color[] msgText = MenuPanel.textColors(speech);
        // Contents origin startX/startY (speech bw 1: 32/16) -> text at (32, 348+16).
        f.draw(b, "要将目前为止的游戏进度保存到哪里？", 32f, h - (h - msgH + 16f), msgText[0], msgText[1]);
        // pbShowCommands -> pbPositionNearMsgWindow(:right): the command window sits
        // flush on the message window's top edge, right aligned to it.
        float rowH = 32f;
        float winW = 0f;
        for (int i = 0; i < slots.size; i++) {
            winW = Math.max(winW, f.width(slotLabel(slots.get(i))));
        }
        winW += 64f; // resizeToFit's borderX + TEXTPADDING + the font's padding
        float winH = slots.size * rowH + 32f; // borderY
        float wx = w - winW;
        float wy = msgH;
        MenuPanel.window(b, a, skin, wx, wy, winW, winH);
        Color[] text = MenuPanel.textColors(skin);
        for (int i = 0; i < slots.size; i++) {
            MenuPanel.cursor(b, a, skin, wx, wy, winH, i, index, rowH);
            float ty = MenuPanel.rowY(wy, winH, i, rowH, f.lineHeight());
            f.draw(b, slotLabel(slots.get(i)), MenuPanel.rowX(wx), ty, text[0], text[1]);
        }
        if (!notice.isEmpty()) {
            f.drawCentered(b, notice, w / 2f, h - 300f, BASE, SHADOW);
        }
    }

    /** {@code s.gsub("Game","存档")} + the (■)/(□) presence marker. */
    private static String slotLabel(SaveSlots.Slot slot) {
        String name = "quick".equals(slot.id) ? "快速存档" : "存档" + slot.id;
        return name + (slot.exists ? "(■)" : "(□)");
    }

    private String playTime() {
        long seconds = (long) context.gameState().trainer().playSeconds;
        return String.format("%02d:%02d", seconds / 3600, seconds / 60 % 60);
    }

    /** The current map name (GameDatabase#map). */
    private String mapName() {
        GameDatabase database = context.database();
        int mapId = context.gameState().currentMapId();
        if (database != null && mapId >= 0) {
            try {
                pokemon.runtime.data.MapData data = database.map(mapId);
                if (data != null && data.name != null) {
                    return data.name;
                }
            } catch (RuntimeException error) {
                // fall through
            }
        }
        return "未知地点";
    }

    private Texture pokemonIcon(MenuAssets a, Pokemon p) {
        if (p == null || p.species == null) {
            return null;
        }
        String suffix = (p.shiny ? "s" : "") + (p.egg ? "egg" : "");
        Texture icon = a.icon(String.format("icon%03d%s", p.species.id, suffix));
        if (icon == null) {
            icon = a.icon("icon" + p.species.internalName + suffix);
        }
        return icon;
    }
}
