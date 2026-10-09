package pokemon.runtime.ui.menu;

import pokemon.runtime.audio.UiSounds;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.state.PcItemStorage;
import pokemon.runtime.ui.WindowSkin;

import java.util.Arrays;
import java.util.List;

/**
 * 223_PScreen_ItemStorage: {@code WithdrawItemScene} / {@code TossItemScene} (the list of the PC's stored items on
 * {@code pcItembg}) with the loops of {@code pbWithdrawItemScreen} and {@code pbTossItemScreen}
 * (211_PScreen_Bag:591-621, :659-690). Depositing uses the bag itself ({@link BagView#depositMode}).
 *
 * <p>登记: the list window's text uses the system font at the bag's size; the letter-by-letter message and the yes/no
 * window of {@code UIHelper} are the shared {@link PbMessage}.</p>
 */
public final class ItemStorageView {
    /** Which screen: the title of the scene and its loop. */
    public enum Kind { WITHDRAW, TOSS }

    private static final int ITEMS_VISIBLE = 7;                                   // :59
    private static final Color LIST_BASE = new Color(88f / 255f, 88f / 255f, 80f / 255f, 1f);        // :53
    private static final Color LIST_SHADOW = new Color(168f / 255f, 184f / 255f, 184f / 255f, 1f);   // :54
    private static final Color TEXT_BASE = new Color(248f / 255f, 248f / 255f, 248f / 255f, 1f);     // :55
    private static final Color TEXT_SHADOW = new Color(0f, 0f, 0f, 1f);                              // :56

    private final RuntimeContext context;
    private final Kind kind;
    private final PcItemStorage storage;
    private final MenuListModel cursor = new MenuListModel(ITEMS_VISIBLE);
    private final PbMessage pbMessage;
    private final NumberPrompt numberPrompt;
    private final MenuClock clock = new MenuClock();

    public ItemStorageView(RuntimeContext context, Kind kind, PcItemStorage storage) {
        this.context = context;
        this.kind = kind;
        this.storage = storage;
        this.pbMessage = new PbMessage(context);
        this.numberPrompt = new NumberPrompt(context);
        refresh();
    }

    private void refresh() {
        cursor.size(storage.length() + 1);                                        // itemCount = length + 1
        if (cursor.index() >= cursor.size()) cursor.select(cursor.size() - 1);
    }

    private PbsData.Item data(String id) {
        return context.pbsData() == null || id == null ? null : context.pbsData().item(id);
    }

    /** {@code @adapter.getDisplayName(item)} (230_PScreen_Mart:21-28): a machine shows its move after its name. */
    private String name(String id) {
        PbsData.Item item = data(id);
        String name = item == null || item.name == null ? id : item.name;
        if (item != null && (item.fieldUse == 3 || item.fieldUse == 4 || item.fieldUse == 6) && item.machine != null) {
            PbsData.Move move = context.pbsData().move(item.machine);
            name = name + " " + (move == null ? item.machine : move.name);
        }
        return name;
    }

    private String plural(String id) {
        PbsData.Item item = data(id);
        return item == null || item.namePlural == null ? name(id) : item.namePlural;
    }

    /** pbIsImportantItem? (key items and HMs: no quantity). */
    private boolean important(String id) {
        PbsData.Item item = data(id);
        return item != null && (item.pocket == 8 || item.fieldUse == 4);
    }

    private PcItemStorage.Slot selectedSlot() {
        return storage.get(cursor.index());
    }

    private void say(String text, Runnable then) {
        pbMessage.start(text, null, 0, 0, ignored -> {
            if (then != null) then.run();
        });
    }

    /** @return true when the screen should close (pbChooseItem answered 0) */
    public boolean update(InputManager input) {
        int ticks = clock.advance();
        if (pbMessage.active()) {
            pbMessage.update(input, ticks);
            return false;
        }
        if (numberPrompt.active()) {
            numberPrompt.update(input);
            return false;
        }
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {   // :157
            return true;
        }
        int before = cursor.index();
        if (input.wasRepeated(GameAction.UP)) cursor.move(-1);
        if (input.wasRepeated(GameAction.DOWN)) cursor.move(1);
        if (cursor.index() != before) UiSounds.cursor(context.audioManager());      // Window_DrawableCommand: pbPlayCursorSE
        if (!input.wasPressed(GameAction.CONFIRM)) return false;
        PcItemStorage.Slot slot = selectedSlot();
        if (slot == null) return true;                                            // :164 the trailing "取消" row
        String item = slot.item;
        if (kind == Kind.WITHDRAW) {
            withdraw(item);
        } else {
            toss(item);
        }
        return false;
    }

    /** 211_PScreen_Bag:600-618. */
    private void withdraw(String item) {
        int qty = storage.pbQuantity(item);
        if (qty > 1 && !important(item)) {                                        // :601
            numberPrompt.start("想要取回多少？", qty, 1, n -> withdrawAfter(item, n));   // :602
        } else {
            withdrawAfter(item, qty);
        }
    }

    private void withdrawAfter(String item, int qty) {
        if (qty <= 0) return;                                                     // :604
        // :605 @bag.pbCanStore?: the bag has no capacity here (roadmap stage 3.1), so it always fits.
        storage.pbDeleteItem(item, qty);                                          // :606
        context.gameState().inventory().add(item, qty);                           // :609
        refresh();                                                                // :612
        int dispqty = important(item) ? 1 : qty;                                  // :613
        String itemname = dispqty > 1 ? plural(item) : name(item);                // :614
        say("取出了" + dispqty + "个" + itemname + "。", null);                       // :615
    }

    /** 211_PScreen_Bag:668-687. */
    private void toss(String item) {
        if (important(item)) {                                                    // :668
            say("这不能丢弃！", null);                                              // :669
            return;
        }
        int qty = storage.pbQuantity(item);                                       // :672
        if (qty > 1) {                                                            // :675
            numberPrompt.start("要丢弃几个" + plural(item) + "？", qty, 1, n -> tossAfter(item, n));   // :676
        } else {
            tossAfter(item, qty);
        }
    }

    private void tossAfter(String item, int qty) {
        if (qty <= 0) return;                                                     // :678
        String itemname = qty > 1 ? plural(item) : name(item);                    // :679
        pbMessage.start("确定要丢弃" + qty + "个" + itemname + "？", Arrays.asList("是", "否"), 2, 0, index -> {   // :680
            if (index != 0) return;
            storage.pbDeleteItem(item, qty);                                      // :681
            refresh();                                                            // :684
            say("丢弃了" + qty + "个" + itemname + "。", null);                       // :685
        });
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, WindowSkin speech) {
        float w = ScreenMetrics.logicalWidth(), h = ScreenMetrics.logicalHeight();
        Texture bg = a.graphic("Pictures", "pcItembg");                           // :75
        if (bg != null) b.draw(bg, 0f, 0f, w, h);
        PcItemStorage.Slot current = selectedSlot();
        // :76 ItemIconSprite at (83,334), centred
        if (current != null) {
            PbsData.Item item = data(current.item);
            Texture icon = item == null ? null : ItemIcons.of(a, context.pbsData(), current.item);
            if (icon != null) {
                b.draw(icon, 83f - 24f, h - 334f - 24f, 48f, 48f, 0, 0,
                        Math.min(48, icon.getWidth()), Math.min(48, icon.getHeight()), false, false);
            }
        }
        // :85-88, :132 the title at (56,16): two lines of 32
        String title = kind == Kind.WITHDRAW ? "取出\n道具" : "丢弃\n道具";
        String[] lines = title.split("\n");
        for (int i = 0; i < lines.length; i++) {
            f.drawCentered(b, lines[i], 56f + 60f, h - (16f + i * 32f + (32f - f.lineHeight()) / 2f), TEXT_BASE, TEXT_SHADOW);
        }
        // :78 the list window (130,14), contents 16 inside
        float contentX = 130f + 16f, contentY = 14f + 16f, contentW = 334f - 32f;
        int first = cursor.first();
        int end = Math.min(cursor.end(), storage.length() + 1);
        for (int i = first; i < end; i++) {
            int row = i - first;
            float rowTop = contentY + row * 32f;
            float textY = h - (rowTop + (32f - f.lineHeight()) / 2f);
            if (i == cursor.index()) {
                Texture arrow = a.graphic("Pictures", "selarrow");
                if (arrow != null) b.draw(arrow, contentX, h - rowTop - 32f + (32f - arrow.getHeight()) / 2f);
            }
            if (i == storage.length()) {
                f.draw(b, "取消", contentX + 16f, textY, LIST_BASE, LIST_SHADOW);                 // :33
                continue;
            }
            PcItemStorage.Slot slot = storage.get(i);
            f.draw(b, name(slot.item), contentX + 16f, textY, LIST_BASE, LIST_SHADOW);           // :41
            if (!important(slot.item)) {                                                         // :42
                f.drawRight(b, String.format("x%2d", slot.count), contentX + contentW - 2f, textY, LIST_BASE, LIST_SHADOW);   // :37
            }
        }
        // :90 the description window (116,270)
        String text = current == null ? "关闭系统" : data(current.item) == null ? "" : data(current.item).description;   // :138-140
        float dx = 116f + 16f, dy = 270f + 16f;
        List<String> wrapped = wrap(f, text == null ? "" : text, w - 84f - 32f);
        for (int i = 0; i < wrapped.size() && i < 4; i++) {
            f.draw(b, wrapped.get(i), dx, h - (dy + i * 32f + (32f - f.lineHeight()) / 2f), TEXT_BASE, TEXT_SHADOW);
        }
        if (pbMessage.active()) pbMessage.render(b, a, f, skin, speech, w, h);
        numberPrompt.render(b, a, f, skin, speech, w, h);
    }

    private static List<String> wrap(MenuFont f, String text, float width) {
        List<String> lines = new java.util.ArrayList<>();
        for (String paragraph : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < paragraph.length(); i++) {
                line.append(paragraph.charAt(i));
                if (f.width(line.toString()) > width) {
                    line.setLength(line.length() - 1);
                    lines.add(line.toString());
                    line.setLength(0);
                    line.append(paragraph.charAt(i));
                }
            }
            lines.add(line.toString());
        }
        return lines;
    }
}
