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
import pokemon.runtime.data.ProjectInfo;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.ui.WindowSkin;

/**
 * 004_MSF_UI_Load: the load screen the pause menu (and the title) show. The
 * {@code loadbg} background, the small-font slot tip, the {@code loadPanels}
 * rows at (144, y) (a 408x222 continue panel plus 408x46 command rows), and
 * {@code pbSetParty}'s walking charset + party icons.
 *
 * <p>Coordinates are the plugin's top-origin values, converted with
 * {@code screenHeight - y}. Text alignment follows {@code pbDrawTextPositions}:
 * 1 = right, 2 = centre, otherwise left.</p>
 */
public final class LoadView {

    public enum Result {
        NONE,
        LOADED,
        CANCELLED
    }

    private static final Color TEXT = new Color(232f / 255f, 232f / 255f, 232f / 255f, 1f);
    private static final Color TEXT_SHADOW = new Color(136f / 255f, 136f / 255f, 136f / 255f, 1f);
    private static final Color MALE = new Color(56f / 255f, 160f / 255f, 248f / 255f, 1f);
    private static final Color MALE_SHADOW = new Color(56f / 255f, 104f / 255f, 168f / 255f, 1f);
    private static final Color FEMALE = new Color(240f / 255f, 72f / 255f, 88f / 255f, 1f);
    private static final Color FEMALE_SHADOW = new Color(160f / 255f, 64f / 255f, 64f / 255f, 1f);
    private static final Color TIP = new Color(248f / 255f, 248f / 255f, 248f / 255f, 128f / 255f);
    private static final Color TIP_SHADOW = new Color(88f / 255f, 88f / 255f, 88f / 255f, 128f / 255f);

    private static final int[] LINE_Y = {10, 64, 96, 128};

    private final RuntimeContext context;
    private final Array<SaveSlots.Slot> slots = new Array<>();
    private boolean showContinue;
    private int slotIndex;
    private int index;

    public LoadView(RuntimeContext context) {
        this.context = context;
    }

    public void refresh(StoragePort storage, GameDatabase database) {
        slots.clear();
        slots.addAll(SaveSlots.list(storage, database));
        showContinue = false;
        for (SaveSlots.Slot slot : slots) {
            if (slot.exists) {
                showContinue = true;
                break;
            }
        }
        slotIndex = Math.max(0, Math.min(slotIndex, slots.size - 1));
        index = showContinue ? 0 : 0;
    }

    public SaveSlots.Slot selectedSlot() {
        return slots.size == 0 ? null : slots.get(slotIndex);
    }

    public Result update(InputManager input, AudioManager audio) {
        int rows = commandCount();
        if (input.wasPressed(GameAction.UP)) {
            index = Math.floorMod(index - 1, rows);
            MenuSe.cursor(audio);
        }
        if (input.wasPressed(GameAction.DOWN)) {
            index = Math.floorMod(index + 1, rows);
            MenuSe.cursor(audio);
        }
        if (showContinue && index == 0) {
            // 004: the continue row cycles the save slots with LEFT/RIGHT.
            if (input.wasPressed(GameAction.LEFT)) {
                cycleSlot(-1);
                MenuSe.cursor(audio);
            }
            if (input.wasPressed(GameAction.RIGHT)) {
                cycleSlot(1);
                MenuSe.cursor(audio);
            }
        }
        if (input.wasPressed(GameAction.CONFIRM)) {
            MenuSe.decision(audio);
            if (showContinue && index == 0) {
                return selectedSlot() != null && selectedSlot().exists ? Result.LOADED : Result.NONE;
            }
            return Result.NONE;
        }
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            MenuSe.buzzer(audio);
            return Result.CANCELLED;
        }
        return Result.NONE;
    }

    private int commandCount() {
        // continue? + 新的冒险 + 打开存档文件夹 + 选项 + 退出游戏
        return (showContinue ? 1 : 0) + 4;
    }

    private void cycleSlot(int direction) {
        if (slots.size == 0) {
            return;
        }
        slotIndex = Math.floorMod(slotIndex + direction, slots.size);
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, MenuFont smallFont) {
        float w = ScreenMetrics.logicalWidth();
        float h = ScreenMetrics.logicalHeight();
        // addBackgroundOrColoredPlane(..., "loadbg", Color.new(248,248,248)).
        Texture bg = a.graphic("Pictures", "loadbg");
        if (bg != null) {
            b.draw(bg, 0f, 0f, w, h);
        } else {
            b.setColor(248f / 255f, 248f / 255f, 248f / 255f, 1f);
            b.draw(a.pixel(), 0f, 0f, w, h);
            b.setColor(Color.WHITE);
        }
        drawPartySprites(b, a, selectedSlot(), h);
        Texture panels = a.graphic("Pictures", "loadPanels");
        float y = 32f;
        int row = 0;
        if (showContinue) {
            drawContinuePanel(b, panels, f, y, row == index);
            y += 224f;
            row++;
        }
        String[] others = {"新的冒险", "打开存档文件夹", "选项", "退出游戏"};
        for (String title : others) {
            if (row >= commandCount()) {
                break;
            }
            drawCommandPanel(b, panels, f, title, y, row == index);
            y += 48f;
            row++;
        }
        // pbStartScene's tips (small font), shown only on the continue row.
        if (showContinue && index == 0) {
            smallFont.draw(b, "[←]/[→]:切换存档插槽", 0f, h - 0f, TIP, TIP_SHADOW);
        }
    }

    /** PokemonLoadPanel#refresh: the 408x222 continue panel at (144, y). */
    private void drawContinuePanel(SpriteBatch b, Texture panels, MenuFont f, float top, boolean selected) {
        if (panels == null) {
            return;
        }
        float x = 24f * 2f + 32f + 64f; // 144
        float h = ScreenMetrics.logicalHeight();
        b.draw(panels, x, h - top - 222f, 408f, 222f, 0, selected ? 222 : 0, 408, 222, false, false);
        SaveSlots.Slot slot = selectedSlot();
        if (slot == null) {
            return;
        }
        f.draw(b, continueTitle(slot), x + 28f, h - (top + LINE_Y[0]), TEXT, TEXT_SHADOW);
        f.draw(b, "徽章：", x + 268f, h - (top + LINE_Y[1]), TEXT, TEXT_SHADOW);
        f.drawRight(b, String.valueOf(slot.badges), x + 388f, h - (top + LINE_Y[1]), TEXT, TEXT_SHADOW);
        f.draw(b, "图鉴：", x + 268f, h - (top + LINE_Y[2]), TEXT, TEXT_SHADOW);
        f.drawRight(b, String.valueOf(slot.seen), x + 388f, h - (top + LINE_Y[2]), TEXT, TEXT_SHADOW);
        f.draw(b, "时长：", x + 28f, h - (top + LINE_Y[2]), TEXT, TEXT_SHADOW);
        f.drawRight(b, duration(slot.playSeconds), x + 248f, h - (top + LINE_Y[2]), TEXT, TEXT_SHADOW);
        f.draw(b, "保存时间：", x + 28f, h - (top + LINE_Y[3]), TEXT, TEXT_SHADOW);
        f.drawRight(b, lastSaved(slot.savedAtMillis), x + 248f, h - (top + LINE_Y[3]), TEXT, TEXT_SHADOW);
        Color name = slot.gender == 1 ? FEMALE : MALE;
        Color nameShadow = slot.gender == 1 ? FEMALE_SHADOW : MALE_SHADOW;
        f.draw(b, slot.playerName == null || slot.playerName.isEmpty() ? "训练家" : slot.playerName,
                x + 112f, h - (top + 56f), name, nameShadow);
        f.drawRight(b, slot.mapName == null || slot.mapName.isEmpty() ? "" : slot.mapName,
                x + 388f, h - (top + LINE_Y[0]), TEXT, TEXT_SHADOW);
    }

    /** PokemonLoadPanel#refresh: the 408x46 command rows (src y 444/490). */
    private void drawCommandPanel(SpriteBatch b, Texture panels, MenuFont f, String title,
                                  float top, boolean selected) {
        if (panels == null) {
            return;
        }
        float x = 24f * 2f + 32f + 64f; // 144
        float h = ScreenMetrics.logicalHeight();
        b.draw(panels, x, h - top - 46f, 408f, 46f, 0, 444 + (selected ? 46 : 0), 408, 46, false, false);
        f.draw(b, title, x + 32f, h - (top + LINE_Y[0]), TEXT, TEXT_SHADOW);
    }

    /** 004's selected_title: ←自动保存→ / ←存档N→. */
    private static String continueTitle(SaveSlots.Slot slot) {
        if ("quick".equals(slot.id)) {
            return "←自动保存→";
        }
        return "←存档" + slot.id + "→";
    }

    private static String duration(int playSeconds) {
        int hour = playSeconds / 3600;
        int min = playSeconds / 60 % 60;
        return hour > 0 ? hour + "小时" + min + "分钟" : min + "分钟";
    }

    /** 004's SecondsToTime: 刚刚 / N分钟前 / N小时前 / N天前 / ... */
    private static String lastSaved(long savedAtMillis) {
        long seconds = Math.max(0, (System.currentTimeMillis() - savedAtMillis) / 1000);
        if (seconds < 60) return "刚刚";
        if (seconds < 3600) return (seconds / 60) + "分钟前";
        if (seconds < 86400) return (seconds / 3600) + "小时前";
        if (seconds < 86400L * 30) return (seconds / 86400) + "天前";
        if (seconds < 86400L * 365) return (seconds / (86400L * 30)) + "个月前";
        if (seconds < 86400L * 365 * 2) return (seconds / (86400L * 365)) + "年前";
        return "很久之前";
    }

    /** {@code pbSetParty}: the walking charset (first frame) and party icons. */
    private void drawPartySprites(SpriteBatch b, MenuAssets a, SaveSlots.Slot slot, float height) {
        if (slot == null || !slot.exists) {
            return;
        }
        String charset = playerCharset(slot.playerId);
        Texture sheet = charset == null ? null : a.character(charset);
        if (sheet != null && sheet.getWidth() >= 4 && sheet.getHeight() >= 4) {
            int frameW = sheet.getWidth() / 4;
            int frameH = sheet.getHeight() / 4;
            b.draw(sheet, 208f - frameW / 2f, height - 64f - frameH / 2f, frameW, frameH,
                    0, 0, frameW, frameH, false, false);
        }
        for (int i = 0; i < slot.party.size; i++) {
            Texture icon = partyIcon(a, slot.party.get(i));
            if (icon == null || icon.getHeight() <= 0) {
                continue;
            }
            int size = icon.getHeight();
            float originX = 188f + 64f * i;
            b.draw(icon, originX - size / 2f, height - 220f + size * 5f / 8f - size, size, size,
                    0, 0, size, size, false, false);
        }
    }

    private String playerCharset(int playerId) {
        ProjectInfo.RuntimeProfile profile =
                context.database() == null || context.database().project() == null
                        ? null : context.database().project().runtime;
        if (profile == null) {
            return null;
        }
        if (playerId >= 0) {
            ProjectInfo.PlayerGraphic graphic = profile.player(playerId);
            if (graphic != null && graphic.charset != null && !graphic.charset.isEmpty()) {
                return graphic.charset;
            }
        }
        return profile.playerCharset == null || profile.playerCharset.isEmpty() ? null : profile.playerCharset;
    }

    private Texture partyIcon(MenuAssets a, SaveSlots.Slot.PartyEntry entry) {
        if (entry == null || entry.species == null || entry.species.isEmpty()) {
            return null;
        }
        String suffix = (entry.gender == 1 ? "f" : "") + (entry.shiny ? "s" : "");
        String extra = entry.egg ? "egg" : "";
        Texture icon = null;
        PbsData data = context.pbsData();
        PbsData.Species species = data == null ? null : data.species(entry.species);
        if (species != null) {
            icon = a.icon(String.format("icon%03d%s%s", species.id, suffix, extra));
        }
        if (icon == null) {
            icon = a.icon("icon" + entry.species + suffix + extra);
        }
        if (icon == null && entry.egg) {
            icon = a.icon("iconEgg");
        }
        return icon;
    }
}
