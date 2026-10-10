package pokemon.runtime.ui.menu;

import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.field.ItemHandlers;
import pokemon.runtime.field.ItemScene;
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
    private final PbMessage pbMessage;
    private final NumberPrompt numberPrompt;
    private pokemon.runtime.state.PcItemStorage depositTo;
    private final MenuClock messageClock = new MenuClock();
    private final ItemHandlers handlers;
    private java.util.function.Consumer<java.util.function.Consumer<ItemScene>> useHost;
    private static final int CMD_USE = 0, CMD_GIVE = 1, CMD_TOSS = 2, CMD_CANCEL = 3, CMD_REGISTER = 4;
    private final List<Integer> actionKinds = new ArrayList<>();
    private List<String> actionRows = new ArrayList<>();

    private Runnable endScreen;

    /** UseFromBag's return 2 (188_PItem_Items:922 "Item used, end screen"): the host closes the bag and the pause menu. */
    public void endScreen(Runnable host) {
        this.endScreen = host;
    }

    /** The map side of the items that act on the map (fishing rod, ropes, Lantern, flutes): the check of {@code UseFromBag} and the hand-over. */
    public interface MapItems {
        /**
         * {@code ItemHandlers::UseFromBag} (189_PItem_ItemEffects:25-63, :1523-1570, :1649-1660): the lines the handler shows when
         * the item cannot be used here, or null when it can.
         */
        String[] unusable(String item);

        /** {@code UseFromBag} answered 2 / 4: the screens close and {@code UseInField} runs on the map. */
        void use(String item);
    }

    private MapItems mapItems;
    private Runnable townMapHost;

    /** {@code UseInField :TOWNMAP} (189:372-375): the host shows the region map over the bag and comes back to it. */
    public void townMapHost(Runnable host) {
        this.townMapHost = host;
    }

    public void mapItems(MapItems host) {
        this.mapItems = host;
    }

    private static boolean isBicycle(String item) {
        return "BICYCLE".equals(item) || "MACHBIKE".equals(item) || "ACROBIKE".equals(item);
    }

    /** The host opens the party screen and runs the handler on it ({@code pbFadeOutIn { PokemonParty_Scene ... }}). */
    private Runnable hatcherHost;

    /** 323_Egg_Hatcher {@code ItemHandlers::UseFromBag.add(:EGGHATCHER)}: opens the hatcher screen. */
    public void hatcherHost(Runnable host) {
        this.hatcherHost = host;
    }

    public void useHost(java.util.function.Consumer<java.util.function.Consumer<ItemScene>> host) {
        this.useHost = host;
    }

    /** Back from the party screen of an item use: the bag shows the new quantities (:914 bagscene.pbRefresh). */
    public void afterUse() {
        step = Step.ITEMS;
        notice = "";
        model.refresh();
    }

    /**
     * {@code pbChooseItemScreen(filter)}: the bag is opened only to pick one item
     * that matches the filter (PField_BerryPlants:350-354). The caller reads
     * {@link #pickedItem()} after {@link #update} returns true.
     */
    public void chooseItem(java.util.function.Predicate<String> filter) {
        battleUse = null;
        holdTarget = null;
        model.useMemory(context.gameState().inventory().bagMemory().copy());   // pbChooseItemScreen restores the bag's memory afterwards
        model.chooseFilter(filter);
        model.cursor.select(0);
        itemPick = true;
        pickedItem = null;
        step = Step.ITEMS;
        notice = "";
    }

    /**
     * The pocketed bag opened only to pick one item, without a filter
     * ({@code PokemonMart_Scene#pbStartSellScene2}: {@code @subscene.pbStartScene(bag)} then
     * {@code @subscene.pbChooseItem}, 230_PScreen_Mart:384-413 / :685-690).
     */
    public void pickFromBag() {
        battleUse = null;
        holdTarget = null;
        model.useMemory(context.gameState().inventory().bagMemory().copy());
        model.chooseFilter(null);
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
        pbMessage = new PbMessage(context);
        numberPrompt = new NumberPrompt(context);
        handlers = new ItemHandlers(context.pbsData(), context.gameState(), java.time.LocalTime::now);
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
    /** Scene_Commands:235-348: in battle a chosen item asks "使用/取消", then the real party screen picks the Pokemon. */
    private java.util.function.Consumer<String> battleItemHost;

    public void battleItemHost(java.util.function.Consumer<String> host) {
        this.battleItemHost = host;
    }

    public void battleMode(java.util.function.BiConsumer<String, Integer> use) {
        battleUse = use;
        holdTarget = null;
        step = Step.ITEMS;
        notice = "";
        model.battleOnly(true);
        model.reload();                                       // Scene_Commands:239-243 + 305_BW_Bag:197: the battle bag's own pocket and cursor
    }

    /** The memory this bag opens on (the battle bag keeps its own for the whole battle). */
    public void useMemory(pokemon.runtime.state.BagMemory memory) {
        model.useMemory(memory);
    }

    public boolean update(InputManager input) {
        int messageTicks = messageClock.advance();
        if (pbMessage.active()) {                             // a pbMessage / pbConfirmMessage of the Use flow
            pbMessage.update(input, messageTicks);
            return false;
        }
        if (numberPrompt.active()) {                          // UIHelper.pbChooseNumber
            numberPrompt.update(input);
            return false;
        }
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            if (step == Step.ITEMS) {
                pokemon.runtime.audio.UiSounds.named(context.audioManager(), "BW2CloseMenu");   // 305_BW_Bag:633-634
                pickedItem = null;                            // a cancel picks nothing (the last sold item must not be picked again)
                return true;
            }
            step = step == Step.REPLACE ? Step.TARGET : Step.ITEMS;
            notice = ""; return false;
        }
        MenuListModel cursor = step == Step.ITEMS ? model.cursor : step == Step.ACTION ? action : step == Step.TARGET ? party.cursor : moves;
        int cursorBefore = cursor.index();
        if (input.wasRepeated(GameAction.UP)) cursor.move(-1);        // Window_DrawableCommand: Input.repeat? - holding scrolls
        if (input.wasRepeated(GameAction.DOWN)) cursor.move(1);
        if (cursor.index() != cursorBefore) {
            pokemon.runtime.audio.UiSounds.cursor(context.audioManager());       // 065_SpriteWindow_text:851-866 pbPlayCursorSE
        }
        if (step == Step.ITEMS) {
            int pocketBefore = model.pocket();
            if (input.wasPressed(GameAction.LEFT)) { model.changePocket(-1); notice = ""; }
            if (input.wasPressed(GameAction.RIGHT)) { model.changePocket(1); notice = ""; }
            if (model.pocket() != pocketBefore) {
                pokemon.runtime.audio.UiSounds.named(context.audioManager(), "BW2BagSound");   // 305_BW_Bag:463/487
            }
            model.remember();                                                                 // :429 @bag.setChoice(pocket, index)
        }
        if (!input.wasPressed(GameAction.CONFIRM)) return false;
        switch (step) {
            case ITEMS:
                pokemon.runtime.audio.UiSounds.decision(context.audioManager());  // 305_BW_Bag:636-637 pbPlayDecisionSE
                if (model.onCloseRow()) { pickedItem = null; return true; } // 关闭背包: picks nothing
                item = model.selected();
                if (item == null) break;
                if (depositTo != null) { deposit(item); break; }
                if (itemPick) { pickedItem = item; return true; }
                if (battleUse != null) {
                    // In battle a Poké Ball is used straight away; everything else
                    // asks for the party member it is used on (pbItemMenu).
                    if (context.pbsData().item(item).pocket == 3) {
                        battleUse.accept(item, -1);
                        return true;
                    }
                    if (battleItemHost != null) {
                        actionKinds.clear();                                       // :258-262 [使用, 取消]
                        actionRows = new ArrayList<>();
                        actionKinds.add(CMD_USE);
                        actionRows.add("使用");
                        actionKinds.add(CMD_CANCEL);
                        actionRows.add("取消");
                        action.size(actionRows.size());
                        action.select(0);
                        step = Step.ACTION;
                        break;
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
                openActionMenu();
                break;
            case ACTION: {
                int kind = action.index() < actionKinds.size() ? actionKinds.get(action.index()) : CMD_CANCEL;
                if (kind == CMD_CANCEL) step = Step.ITEMS;        // 取消
                else if (kind == CMD_TOSS) toss();                // 丢弃
                else if (kind == CMD_REGISTER) {                  // :524-531
                    pokemon.runtime.state.BagMemory memory = state().inventory().bagMemory();
                    if (memory.isRegistered(item)) memory.unregister(item); else memory.register(item);
                    step = Step.ITEMS;                            // @scene.pbRefresh
                    model.refresh();
                }
                else if (kind == CMD_USE && battleUse != null && battleItemHost != null) {
                    step = Step.ITEMS;                            // Scene_Commands:263-346: the battle's party screen takes over
                    battleItemHost.accept(item);
                }
                else if (kind == CMD_USE) useItem();              // 使用 (211_PScreen_Bag:491-496 pbUseItem)
                else giveItem();                                  // 给予
                break;
            }
            case TARGET:
                if (party.selected() == null) break;
                if (battleUse != null) {
                    battleUse.accept(item, party.cursor.index());
                    return true;
                }
                {
                    finish(PartyModel.giveItem(party.selected(), item, context.gameState().inventory(), context.pbsData())
                            ? "已交给宝可梦。" : "不能携带这个道具。");
                }
                break;
            case REPLACE: break;
        }
        return false;
    }

    private String plural(String id) {
        PbsData.Item data = context.pbsData().item(id);
        return data == null || data.namePlural == null ? model.name(id) : data.namePlural;
    }

    /** 211_PScreen_Bag:510-523 the Toss command. */
    private void toss() {
        final String tossed = item;
        step = Step.ITEMS;
        int qty = context.gameState().inventory().count(tossed);                     // :511
        if (qty > 1) {                                                                // :512
            numberPrompt.start("要丢弃几个" + plural(tossed) + "？", qty, 1, n -> tossAfter(tossed, n));   // :513-514
        } else {
            tossAfter(tossed, qty);
        }
    }

    private void tossAfter(String tossed, int qty) {
        if (qty <= 0) return;                                                         // :516
        String itemname = qty > 1 ? plural(tossed) : model.name(tossed);              // :517
        confirm("确定要丢弃" + qty + "个" + itemname + "？", yes -> {                   // :518
            if (!yes) return;
            say("丢弃了" + qty + "个" + itemname + "。", () -> {                          // :519
                context.gameState().inventory().remove(tossed, qty);                 // :520
                model.refresh();                                                      // :521
            });
        });
    }

    /** 211_PScreen_Bag:624-656 pbDepositItemScreen: the bag picks the items to store in the PC. */
    public void depositMode(pokemon.runtime.state.PcItemStorage storage) {
        depositTo = storage;
        battleUse = null;
        holdTarget = null;
        model.chooseFilter(null);
        model.cursor.select(0);
        step = Step.ITEMS;
        notice = "";
    }

    private void deposit(String stored) {
        int qty = context.gameState().inventory().count(stored);                     // :634
        PbsData.Item data = context.pbsData().item(stored);
        if (qty > 1 && !important(data)) {                                            // :635
            numberPrompt.start("想要储存几个？", qty, 1, n -> depositAfter(stored, n));   // :636
        } else {
            depositAfter(stored, qty);
        }
    }

    private void depositAfter(String stored, int qty) {
        if (qty <= 0) return;                                                         // :638
        if (!depositTo.pbCanStore(stored, qty)) {                                     // :639
            say("电脑里的储存盒已经满了……", null);                                       // :640
            return;
        }
        context.gameState().inventory().remove(stored, qty);                         // :642
        depositTo.pbStoreItem(stored, qty);                                           // :645
        model.refresh();                                                              // :648
        int dispqty = important(context.pbsData().item(stored)) ? 1 : qty;            // :649
        String itemname = dispqty > 1 ? plural(stored) : model.name(stored);          // :650
        say("储存了" + dispqty + "个" + itemname + "。", null);                          // :651
    }

    /** 211_PScreen_Bag:467-483: the commands of the chosen item. */
    private void openActionMenu() {
        PbsData.Item data = context.pbsData().item(item);
        actionKinds.clear();
        actionRows = new ArrayList<>();
        boolean hasParty = context.gameState().trainer().pokemonCount() > 0;
        if (handlers.hasUseOnPokemon(item) || handlers.hasBagFieldHandler(item) || (handlers.isMachine(item) && context.gameState().trainer().party.size() > 0)) {
            actionKinds.add(CMD_USE);                          // :468-474 (UseText is only registered for the bicycles)
            // UseText (189_PItem_ItemEffects:4-8): the bicycles say "步行" while riding and the plugin's own "Use" otherwise.
            actionRows.add(isBicycle(item) ? (context.gameState().fieldGlobals().bicycle ? "步行" : "Use") : "使用");
        }
        if (hasParty && data != null && !important(data)) {    // :475 pbCanHoldItem?
            actionKinds.add(CMD_GIVE);
            actionRows.add("给予");
        }
        if (!important(data)) {                                // :476
            actionKinds.add(CMD_TOSS);
            actionRows.add("丢弃");
        }
        if (state().inventory().bagMemory().isRegistered(item)) {          // :477-481
            actionKinds.add(CMD_REGISTER);
            actionRows.add("取消登录");
        } else if (ItemHandlers.hasUseInFieldHandler(item)) {              // pbCanRegisterItem?
            actionKinds.add(CMD_REGISTER);
            actionRows.add("登录");
        }
        actionKinds.add(CMD_CANCEL);                           // :483
        actionRows.add("取消");
        action.size(actionRows.size());
        step = Step.ACTION;
        action.select(0);
        notice = "";
    }

    /** 211_PScreen_Bag:497-509: the party screen asks who holds the item (pbPokemonGiveScreen). */
    private void giveItem() {
        step = Step.ITEMS;
        if (handlers.noPokemonMessage() != null) {                          // :498
            say("没有宝可梦。", null);
        } else if (important(context.pbsData().item(item))) {               // :500
            say("宝可梦不能携带" + model.name(item) + "。", null);
        } else if (useHost != null) {
            final String given = item;
            useHost.accept(scene -> handlers.pbPokemonGiveScreen(given, scene));
        }
    }

    private pokemon.runtime.state.GameState state() {
        return context.gameState();
    }

    private pokemon.runtime.field.ItemTask fieldTask;

    /** Runs a state-only item handler on the bag: its messages are the bag's pbMessage windows. */
    private void runFieldTask(java.util.function.Consumer<ItemScene> body) {
        fieldTask = pokemon.runtime.field.ItemTask.start(body);
        pumpFieldTask();
    }

    private void pumpFieldTask() {
        while (fieldTask != null) {
            if (fieldTask.resume()) {
                fieldTask = null;
                afterUse();
                return;
            }
            pokemon.runtime.field.TaskItemScene.Request r = fieldTask.pending();
            switch (r.kind) {
                case MESSAGE:
                case DISPLAY:
                    say(r.text, () -> answerField(null));
                    return;
                case CONFIRM:
                    confirm(r.text, yes -> answerField(yes));
                    return;
                default:
                    fieldTask.answer(null);
                    break;
            }
        }
    }

    private void answerField(Object value) {
        if (fieldTask != null) {
            fieldTask.answer(value);
            pumpFieldTask();
        }
    }

    private void say(String text, Runnable then) {
        pbMessage.start(text, null, 0, 0, ignored -> {
            if (then != null) then.run();
        });
    }

    private void confirm(String text, java.util.function.Consumer<Boolean> then) {
        pbMessage.start(text, Arrays.asList("是", "否"), 2, 0, index -> then.accept(index == 0));   // pbConfirmMessage
    }

    /** 188_PItem_Items:844-931 pbUseItem for the items used on a Pokemon, TMs / HMs included. */
    private void useItem() {
        PbsData.Item data = context.pbsData().item(item);
        int useType = data == null ? 0 : data.fieldUse;
        String none = handlers.noPokemonMessage();
        if (handlers.isMachine(item)) {                        // :847
            if (none != null) {                                // :848-851
                say(none, null);
                return;
            }
            String machine = handlers.machineMove(item);       // :852
            PbsData.Move move = machine == null ? null : context.pbsData().move(machine);
            if (move == null) {
                return;                                        // :853
            }
            String moveName = move.name == null ? move.internalName : move.name;
            say("\\se[PC access]启动了" + model.name(item) + "。\u0001", () ->   // :855
                    confirm("想教" + moveName + "给宝可梦吗？", yes -> {      // :856
                        if (yes && useHost != null) {
                            useHost.accept(scene -> handlers.pbUseMachine(item, move, scene));   // :858
                        }
                    }));
            return;
        }
        if (useType == 1 || useType == 5) {                    // :867
            if (none != null) {                                // :868-870
                say(none, null);
                return;
            }
            if (useHost != null) {
                final String used = item;
                useHost.accept(scene -> handlers.pbUseItemOnParty(used, useType, scene));   // :872-916
            }
            return;
        }
        if (item.equals("EGGHATCHER") && hatcherHost != null) {     // 323:245-249 pbFadeOutIn { openHatcher }; next 1
            hatcherHost.run();
            return;
        }
        if (item.equals("SACREDASH") && useHost != null) {     // UseInField :SACREDASH opens the party screen from the bag
            final String ash = item;
            useHost.accept(scene -> {
                if (handlers.sacredAsh(scene) == 3) {
                    state().inventory().remove(ash, 1);                // :923 3 = used, consume
                }
            });
            return;
        }
        if ("TOWNMAP".equals(item) && townMapHost != null) {      // 189:372-375 pbShowMap(-1, false)
            townMapHost.run();
            return;
        }
        if (ItemHandlers.isMapItem(item) && mapItems != null) {   // 189 UseFromBag: SUPERROD / ropes / LANTERN / EONFLUTE / ETHEREALNEXUS
            String[] refusal = mapItems.unusable(item);
            if (refusal != null) {
                sayAll(refusal, 0);
                return;
            }
            if (ItemHandlers.consumedInBag(item)) {
                state().inventory().remove(item, 1);                // :917 case 4: bag.pbDeleteItem(item), end screen
            }
            mapItems.use(item);                                    // next 2 / 4: end screen, then pbUseKeyItemInField
            return;
        }
        if (handlers.hasBagFieldHandler(item)) {               // :917-927 triggerUseFromBag falls back to UseInField
            final String used = item;
            runFieldTask(scene -> {
                int ret = handlers.useInField(used, scene);
                if (ret == 3) {
                    state().inventory().remove(used, 1);               // :923 3 = used, consume
                }
                if (ret == 1 && isBicycle(used) && endScreen != null) {
                    endScreen.run();                                   // UseFromBag :49-51 returns 2: the screens end, the bike is used
                }
            });
            return;
        }
        say("这里不能使用。", null);                            // :929
    }

    /** Shows the lines one after the other (a handler that refuses can show more than one pbMessage). */
    private void sayAll(String[] lines, int index) {
        if (index >= lines.length) {
            return;
        }
        say(lines[index], index + 1 < lines.length ? () -> sayAll(lines, index + 1) : null);
    }

    private void finish(String text) { notice = text; step = Step.ITEMS; model.refresh(); }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, MenuFont smallFont) {
        render(b, a, f, skin, skin, smallFont);
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, WindowSkin speech, MenuFont smallFont) {
        gridOffset += pokemon.runtime.app.GameSpeed.scale(Gdx.graphics.getDeltaTime()) * 40f;
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
        if (pbMessage.active()) {
            pbMessage.render(b, a, f, skin, speech, w, h);
        }
        numberPrompt.render(b, a, f, skin, speech, w, h);
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
                // :85-95: the registered icon (row 0), or the "can be registered" one (row 24); (rect.x+rect.width-72, ypos+4)
                boolean registered = state().inventory().bagMemory().isRegistered(id);
                if (registered || ItemHandlers.hasUseInFieldHandler(id)) {
                    b.draw(register, CONTENT_X + CONTENT_W - 72f, h - (rowTop + 25f) - 24f, 56f, 24f,
                            0, registered ? 0 : 24, 56, 24, false, false);
                }
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
            rows = actionRows;
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
        return ItemIcons.of(a, context.pbsData(), id);
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
