package pokemon.runtime.ui.menu;

import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.input.*;
import pokemon.runtime.pokemon.*;
import pokemon.runtime.ui.WindowSkin;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.*;

/**
 * BW Bag (PScreen Bag Graphical Overhaul): the animated grid, the pocket bag
 * sprite, the 9 row item window (cursor + 24px icon + name + quantity / the
 * trailing "关闭背包"), the selected item's 48px icon and its description.
 *
 * <p>{@code Window_PokemonBag} sets {@code windowskin = nil}, so the
 * SpriteWindow frame is 32 and {@code startX/startY} are 16: the item window at
 * (250,-8) 358x368 has its contents at (266,8) and {@code drawItem} adds
 * (+16,+16); every position below follows that. The item -> command window flow
 * uses the project's windowskin (the plugin's {@code pbShowCommands}).</p>
 */
public final class BagView {
    private enum Step { ITEMS, ACTION, TARGET, REPLACE }

    private static final int ITEM_VISIBLE = 9;
    private static final int ROW_H = 32;
    // Window_PokemonBag.new(bag, filter, pocket, 218-32+64, -8, 326+32, 80+9*32).
    private static final float LIST_X = 250f;
    private static final float LIST_Y = -8f;
    private static final float WINDOW_W = 358f;
    private static final float CONTENT_X = LIST_X + 16f;      // startX
    private static final float CONTENT_Y = LIST_Y + 16f;      // startY
    private static final float CONTENT_W = WINDOW_W - 32f;    // border 32

    private static final Color NAME_BASE = new Color(255f / 255f, 255f / 255f, 255f / 255f, 1f);
    private static final Color NAME_SHADOW = new Color(156f / 255f, 156f / 255f, 156f / 255f, 1f);
    private static final Color TEXT_BASE = new Color(248f / 255f, 248f / 255f, 248f / 255f, 1f);
    private static final Color TEXT_SHADOW = new Color(90f / 255f, 90f / 255f, 90f / 255f, 1f);
    private static final Color CURSOR_MAIN = new Color(1f, 216f / 255f, 0f, 1f);

    private final RuntimeContext context;
    public final BagModel model;
    private final PartyModel party;
    private final MenuListModel action = new MenuListModel(6), moves = new MenuListModel(4);
    private Step step = Step.ITEMS;
    private String item;
    private String notice = "";
    private float gridOffset;
    private Pokemon holdTarget;
    private java.util.function.BiConsumer<String, Integer> battleUse;
    private boolean itemPick;
    private String pickedItem;

    /**
     * {@code pbChooseItemScreen(filter)}: the bag is opened only to pick one item
     * that matches the filter (PField_BerryPlants:350-354). The caller reads
     * {@link #pickedItem()} after {@link #update} returns true.
     */
    public void chooseItem(java.util.function.Predicate<String> filter) {
        battleUse = null;
        holdTarget = null;
        model.chooseFilter(filter);
        model.cursor.select(0);
        itemPick = true;
        pickedItem = null;
        step = Step.ITEMS;
        notice = "";
    }

    /** The item chosen by {@link #chooseItem}, or null when the player cancelled. */
    public String pickedItem() {
        return pickedItem;
    }

    public BagView(RuntimeContext context) {
        this.context = context;
        model = new BagModel(context.gameState().inventory(), context.pbsData());
        party = new PartyModel(context.gameState().trainer().party, context.pbsData());
        action.size(4);
    }

    /** The summary's "携带道具" (pbChooseItemScreen): pick an item for one Pokemon. */
    public void chooseHold(Pokemon target) {
        holdTarget = target;
        step = Step.ITEMS;
        notice = "";
        model.refresh();
    }

    /**
     * pbItemMenu: while a battle is running the bag only lists battle-usable
     * items and uses them immediately - PokemonBag_Scene started with
     * {@code battle=true} (Scene_Commands:235). The consumer receives the item
     * and the party index (-1 for items without a target, e.g. Poké Balls).
     */
    public void battleMode(java.util.function.BiConsumer<String, Integer> use) {
        battleUse = use;
        holdTarget = null;
        step = Step.ITEMS;
        notice = "";
        model.battleOnly(true);
    }

    public boolean update(InputManager input) {
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            if (step == Step.ITEMS) return true;
            step = step == Step.REPLACE ? Step.TARGET : Step.ITEMS;
            notice = ""; return false;
        }
        MenuListModel cursor = step == Step.ITEMS ? model.cursor : step == Step.ACTION ? action : step == Step.TARGET ? party.cursor : moves;
        if (input.wasPressed(GameAction.UP)) cursor.move(-1);
        if (input.wasPressed(GameAction.DOWN)) cursor.move(1);
        if (step == Step.ITEMS) {
            if (input.wasPressed(GameAction.LEFT)) { model.changePocket(-1); notice = ""; }
            if (input.wasPressed(GameAction.RIGHT)) { model.changePocket(1); notice = ""; }
        }
        if (!input.wasPressed(GameAction.CONFIRM)) return false;
        switch (step) {
            case ITEMS:
                if (model.onCloseRow()) return true; // 关闭背包
                item = model.selected();
                if (item == null) break;
                if (itemPick) { pickedItem = item; return true; }
                if (battleUse != null) {
                    // In battle a Poké Ball is used straight away; everything else
                    // asks for the party member it is used on (pbItemMenu).
                    if (context.pbsData().item(item).pocket == 3) {
                        battleUse.accept(item, -1);
                        return true;
                    }
                    party.refresh(); step = Step.TARGET;
                    break;
                }
                if (holdTarget != null) {
                    // pbCanHoldItem? / pbGiveItemToPokemon
                    if (PartyModel.giveItem(holdTarget, item, context.gameState().inventory(), context.pbsData())) {
                        return true;
                    }
                    notice = "不能携带这个道具。";
                    break;
                }
                { step = Step.ACTION; action.select(0); notice = ""; }
                break;
            case ACTION:
                if (action.index() == 3) step = Step.ITEMS;       // 取消
                else if (action.index() == 2) toss();             // 丢弃
                else { party.refresh(); step = Step.TARGET; }     // 使用 / 给予
                break;
            case TARGET:
                if (party.selected() == null) break;
                if (battleUse != null) {
                    battleUse.accept(item, party.cursor.index());
                    return true;
                }
                if (action.index() == 1) {
                    finish(PartyModel.giveItem(party.selected(), item, context.gameState().inventory(), context.pbsData())
                            ? "已交给宝可梦。" : "不能携带这个道具。");
                } else use(-1);
                break;
            case REPLACE: use(moves.index()); break;
        }
        return false;
    }

    private void toss() {
        context.gameState().inventory().remove(item, 1);
        finish("丢弃了1个" + model.name(item) + "。");
    }

    private void use(int replace) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        ItemUse.Result result = ItemUse.use(item, party.selected(), context.gameState().trainer(),
                context.gameState().inventory(), context.pbsData(), replace, hour >= 6 && hour < 20);
        if (result == ItemUse.Result.REPLACE_MOVE) {
            moves.size(party.selected().moves.size); moves.select(0); step = Step.REPLACE;
        } else if (!ItemUse.lastMessage.isEmpty()) {
            // PItem_Items: the handler's own scene.pbDisplay text (success or
            // "这没有任何效果。"); an ineffective item is never consumed.
            finish(ItemUse.lastMessage);
        } else finish("");
    }

    private void finish(String text) { notice = text; step = Step.ITEMS; model.refresh(); }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, MenuFont smallFont) {
        render(b, a, f, skin, skin, smallFont);
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, WindowSkin speech, MenuFont smallFont) {
        gridOffset += Gdx.graphics.getDeltaTime() * 40f;
        float w = ScreenMetrics.logicalWidth(), h = ScreenMetrics.logicalHeight();
        drawBackground(b, a, w, h);
        drawPocket(b, a, f, h);
        drawList(b, a, f, h);
        drawSlider(b, a, h);
        drawDescription(b, f, h);
        drawSelectedIcon(b, a, h);
        // BW Bag's overlay2 (pbSetSmallFont = size 20) draws the hint centred at
        // (92+16, 8) (pbDrawTextPositions align 2).
        smallFont.drawCentered(b, "[Z]:手动 [Shift]:自动", 108f, h - 8f, TEXT_BASE, TEXT_SHADOW);
        if (step != Step.ITEMS) drawAction(b, a, f, skin, speech, w, h);
        if (!notice.isEmpty()) {
            f.draw(b, notice, 20f, h - 396f, TEXT_BASE, TEXT_SHADOW);
        }
    }

    private boolean female() {
        return context.gameState().trainer().gender == 1;
    }

    private void drawBackground(SpriteBatch b, MenuAssets a, float w, float h) {
        // AnimatedPlane grid (ANIMEBG scrolls it 1px/frame; 576x336 tile).
        Texture grid = a.graphic("Pictures/Bag", female() ? "bg_gridf" : "bg_grid");
        if (grid != null) {
            int tw = grid.getWidth(), th = grid.getHeight();
            float ox = -Math.floorMod((int) gridOffset, tw);
            for (float x = ox; x < w; x += tw) {
                for (float y = 0f; y < h; y += th) {
                    b.draw(grid, x, y);
                }
            }
        }
        // bagsprite at (-30, 10); the female art exists only for some pockets.
        int pocket = model.pocket();
        Texture bag = null;
        if (female()) {
            bag = a.graphic("Pictures/Bag", "bag_" + pocket + "_f");
        }
        if (bag == null) {
            bag = a.graphic("Pictures/Bag", "bag_" + pocket);
        }
        if (bag != null) {
            b.draw(bag, -30f, h - 10f - bag.getHeight());
        }
        Texture bg = a.graphic("Pictures/Bag", female() ? "bg_f" : "bg");
        if (bg != null) {
            b.draw(bg, 0f, 0f, w, h);
        }
    }

    private void drawPocket(SpriteBatch b, MenuAssets a, MenuFont f, float h) {
        // pocketicon: a 186x32 sprite at (0,-3) blts the pocket's 28x28 icon at (2,2).
        Texture sheet = a.graphic("Pictures/Bag", "icon_pocket");
        int pocket = model.pocket();
        if (sheet != null && pocket - 1 >= 0 && (pocket - 1) * 28 + 28 <= sheet.getWidth()) {
            int col = pocket - 1;
            b.draw(sheet, 2f, h - (-1f) - 28f, 28f, 28f, col * 28, 0, 28, 28, false, false);
        }
        if (pocket >= 0 && pocket < BagModel.POCKETS.length) {
            f.drawRight(b, BagModel.POCKETS[pocket], 110f, h - 186f, TEXT_BASE, TEXT_SHADOW);
        }
    }

    private void drawList(SpriteBatch b, MenuAssets a, MenuFont f, float h) {
        Texture cursor = a.graphic("Pictures/Bag", "cursor");
        Texture register = a.graphic("Pictures/Bag", "icon_register");
        List<String> ids = model.items();
        int first = model.cursor.first();
        int end = model.cursor.end();
        for (int i = first; i < end; i++) {
            float rowTop = CONTENT_Y + (i - first) * ROW_H; // absolute top-origin
            boolean current = i == model.cursor.index();
            if (current && cursor != null) {
                // drawCursor copies the WHOLE 314x68 image; its rounded box is at
                // y=16..53, so drawing at (rowTop-1) centres the box on the row and
                // keeps it clear of the next row ("关闭背包").
                b.draw(cursor, CONTENT_X + 12f, h - (rowTop - 1f) - cursor.getHeight(),
                        cursor.getWidth(), cursor.getHeight(), 0, 0,
                        cursor.getWidth(), cursor.getHeight(), false, false);
            }
            if (i >= ids.size()) {
                f.draw(b, "关闭背包", CONTENT_X + 44f, h - (rowTop + 21f), NAME_BASE, NAME_SHADOW);
                continue;
            }
            String id = ids.get(i);
            PbsData.Item data = context.pbsData() == null ? null : context.pbsData().item(id);
            PbsData.Item importantData = data;
            Texture icon = itemIcon(a, id);
            if (icon != null) {
                b.draw(icon, CONTENT_X + 16f, h - (rowTop + 23f) - 24f, 24f, 24f, 0, 0,
                        Math.min(48, icon.getWidth()), Math.min(48, icon.getHeight()), false, false);
            }
            f.draw(b, model.name(id), CONTENT_X + 44f, h - (rowTop + 21f), NAME_BASE, NAME_SHADOW);
            if (!important(importantData)) {
                long qty = context.gameState().inventory().count(id);
                f.drawRight(b, String.format("x%3d", qty), CONTENT_X + CONTENT_W - 16f, h - (rowTop + 21f),
                        NAME_BASE, NAME_SHADOW);
            } else if (register != null) {
                // pbDrawImagePositions: (rect.x+rect.width-64, ypos+4), src row 24.
                b.draw(register, CONTENT_X + CONTENT_W - 64f, h - (rowTop + 25f) - 24f, 56f, 24f,
                        0, 24, 56, 24, false, false);
            }
        }
    }

    /** pbRefreshIndexChanged's slider (534+64, top 54, height 174). */
    private void drawSlider(SpriteBatch b, MenuAssets a, float h) {
        int size = model.cursor.size();
        if (size <= ITEM_VISIBLE) {
            return;
        }
        Texture s = a.graphic("Pictures/Bag", "icon_slider");
        if (s == null) {
            return;
        }
        float x = 598f;
        int first = model.cursor.first();
        if (first > 0) {
            b.draw(s, x, h - 16f - 38f, 36f, 38f, 0, 0, 36, 38, false, false);
        }
        if (model.cursor.end() < size) {
            b.draw(s, x, h - 228f - 38f, 36f, 38f, 0, 38, 36, 38, false, false);
        }
        float sliderHeight = 174f;
        float boxHeight = (float) Math.floor(sliderHeight * ITEM_VISIBLE / size);
        boxHeight += Math.min((sliderHeight - boxHeight) / 2f, sliderHeight / 6f);
        boxHeight = Math.max(boxHeight, 38f);
        float y = 54f;
        int scrollRows = size - ITEM_VISIBLE;
        if (scrollRows > 0) {
            y += (float) Math.floor((sliderHeight - boxHeight) * first / scrollRows);
        }
        b.draw(s, x, h - y - 4f, 36f, 4f, 36, 0, 36, 4, false, false);
        float remaining = boxHeight - 4f - 18f;
        int i = 0;
        while (i * 16 < remaining) {
            float seg = Math.min(remaining - i * 16f, 16f);
            b.draw(s, x, h - (y + 4f + i * 16f) - seg, 36f, seg, 36, 4, 36, (int) seg, false, false);
            i++;
        }
        b.draw(s, x, h - (y + boxHeight - 18f) - 18f, 36f, 18f, 36, 20, 36, 18, false, false);
    }

    private void drawDescription(SpriteBatch b, MenuFont f, float h) {
        // itemtext window at (84,334): contents at (100,350).
        String id = model.selected();
        String text = id == null ? "关闭背包" : description(id);
        float x = 100f;
        float top = 350f;
        float maxW = (ScreenMetrics.logicalWidth() - 84f - 24f) - 16f;
        String remaining = text == null ? "" : text.replace("\n", " ");
        int lines = 0;
        while (!remaining.isEmpty() && lines < 4) {
            int end = remaining.length();
            while (end > 1 && f.width(remaining.substring(0, end)) > maxW) end--;
            f.draw(b, remaining.substring(0, end), x, h - (top + lines * 32f), TEXT_BASE, TEXT_SHADOW);
            remaining = remaining.substring(end);
            lines++;
        }
    }

    private void drawSelectedIcon(SpriteBatch b, MenuAssets a, float h) {
        String id = model.selected();
        if (id == null) return;
        Texture icon = itemIcon(a, id);
        if (icon == null) return;
        // ItemIconSprite at (48, height-48), Center origin: top-left (24, 376) at 48px.
        b.draw(icon, 24f, h - 376f - 48f, 48f, 48f, 0, 0,
                Math.min(48, icon.getWidth()), Math.min(48, icon.getHeight()), false, false);
    }

    /**
     * The plugin's {@code pbShowCommands} window (commands + "已选择<item>"
     * message), drawn with the project's windowskin.
     */
    private void drawAction(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, WindowSkin speech, float w, float h) {
        List<String> rows;
        MenuListModel cursor;
        List<String> detail = new ArrayList<>();
        if (step == Step.ACTION) {
            rows = Arrays.asList("使用", "给予", "丢弃", "取消");
            cursor = action;
        } else if (step == Step.TARGET) {
            party.refresh();
            rows = party.rows();
            cursor = party.cursor;
            detail = party.details();
        } else {
            rows = new ArrayList<>();
            cursor = moves;
            for (Pokemon.MoveSlot move : party.selected().moves) rows.add(move.move == null ? "—" : move.move.name);
            detail.add("选择要遗忘的招式。");
        }
        int visible = Math.min(rows.size(), 5);
        float rowH = 30f;
        float winH = visible * rowH + 20f;
        float winW = 180f;
        float wx = w - winW - 24f;
        // The bottom message window is at (16,16,w-32,64); pbShowCommands keeps
        // the command window flush on top of it.
        float mx = 16f;
        float mh = 64f;
        float my = 16f;
        float wy = my + mh;
        MenuPanel.window(b, a, skin, wx, wy, winW, winH);
        Color[] text = MenuPanel.textColors(skin);
        for (int i = 0; i < rows.size(); i++) {
            MenuPanel.cursor(b, a, skin, wx, wy, winH, i, cursor.index(), rowH);
            float ty = MenuPanel.rowY(wy, winH, i, rowH, f.lineHeight());
            f.draw(b, rows.get(i), MenuPanel.rowX(wx), ty, text[0], text[1]);
        }
        // Bottom message window: "已选择<item>" (or the option detail).
        MenuPanel.window(b, a, speech, mx, my, w - 32f, mh);
        String message = step == Step.ACTION && item != null ? "已选择" + model.name(item) : detail.isEmpty() ? "" : detail.get(0);
        Color[] msgText = MenuPanel.textColors(speech);
        f.draw(b, message, mx + 16f, my + mh - 28f, msgText[0], msgText[1]);
    }

    private Texture itemIcon(MenuAssets a, String id) {
        PbsData.Item data = context.pbsData() == null ? null : context.pbsData().item(id);
        Texture icon = null;
        if (data != null) {
            icon = a.icon(String.format("item%03d", data.id));
        }
        if (icon == null) {
            icon = a.icon("item" + id);
        }
        return icon;
    }

    private String description(String id) {
        PbsData.Item data = context.pbsData() == null ? null : context.pbsData().item(id);
        return data == null || data.description == null ? "" : data.description;
    }

    /** Key items and HMs never show a quantity (pbIsImportantItem?). */
    private static boolean important(PbsData.Item data) {
        return data != null && (data.pocket == 8 || data.fieldUse == 4);
    }
}
