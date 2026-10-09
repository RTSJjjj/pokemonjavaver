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
import pokemon.runtime.state.Inventory;
import pokemon.runtime.ui.WindowSkin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * 段 PScreen_Mart (Scripts.rxdata #230, 935 行；原文 {@code plugin-src/ruby/230_PScreen_Mart.rb})：
 * {@code pbPokemonMart(stock,speech,cantsell)}(:807-846)、{@code PokemonMartScreen#pbBuyScreen}(:702-768)、
 * {@code #pbSellScreen}(:771-805)、{@code PokemonMart_Scene}(:278-690) 和 {@code PokemonMartAdapter}(:4-82)。
 *
 * <p>阻塞的 {@code pbMessage} / {@code pbDisplay*} / {@code pbConfirm} / {@code pbChooseNumber} 拆成状态机，
 * 续体接着往下走（与 {@link PokeCenterPcView} 同一写法）。卖东西的背包是 {@link BagView}（原文
 * {@code PokemonBag_Scene} 作为 {@code @subscene}）。</p>
 *
 * <p>登记: ①{@code pbScrollMap(6,5,5)} / {@code pbScrollMap(4,5,5)}（进出商店时地图滚动）未建模；
 * ②{@code Window_DrawableCommand} 的上下箭头 (uparrow/downarrow) 未画；③{@code pbSellScene2} 的黑幕渐变 (:384-394, :433-444)
 * 未建模；④{@code getMoneyString} 的 {@code to_s_formatted} 用千位逗号；⑤{@code Window_AdvancedTextPokemon}
 * 的 \r\n / {@code <r>} 控制符只实现了本段用到的「左 / 右」两栏写法。</p>
 */
public final class MartView {
    private static final int ROW = 32;
    private static final int BORDER = 32;

    private static final Color WHITE = new Color(248f / 255f, 248f / 255f, 248f / 255f, 1f);
    private static final Color LIST_SHADOW = new Color(80f / 255f, 80f / 255f, 88f / 255f, 1f);   // :235
    private static final Color TEXT_SHADOW = new Color(88f / 255f, 88f / 255f, 80f / 255f, 1f);   // :355 / :369
    private static final Color NUM_BASE = new Color(88f / 255f, 88f / 255f, 80f / 255f, 1f);      // :575
    private static final Color NUM_SHADOW = new Color(168f / 255f, 184f / 255f, 184f / 255f, 1f); // :576
    private static final Color GOLD_BASE = new Color(48f / 255f, 48f / 255f, 48f / 255f, 1f);     // :418
    private static final Color GOLD_SHADOW = new Color(168f / 255f, 184f / 255f, 184f / 255f, 1f);// :419

    private enum Mode { MESSAGE, BUY_LIST, DISPLAY, CONFIRM, NUMBER, SELL_BAG, IDLE }

    private final RuntimeContext context;
    private final TrainerState trainer;
    private final Inventory bag;
    private final PbsData pbs;
    private final PbMessage pbMessage;
    private final MartRules rules;
    private final MenuClock clock = new MenuClock();
    private final List<String> stock;
    private final String speech;
    private final boolean cantSell;

    private Mode mode = Mode.IDLE;
    private boolean finished;

    // Window_PokemonMart (:225-269)
    private int listIndex;
    private int listTop;

    // helpwindow (pbDisplay / pbDisplayPaused / pbConfirm)
    private boolean helpVisible;
    private String helpText = "";
    private int helpShown;
    private int helpCounter;
    private Runnable helpShownCallback;
    private boolean helpShownDone;
    private boolean helpBrief;
    private Runnable helpDone;

    // pbConfirm
    private int confirmIndex;
    private java.util.function.Consumer<Boolean> confirmDone;

    // pbChooseNumber
    private int curNumber;
    private int maxNumber;
    private int itemPrice;
    private int bagQuantity;
    private IntConsumer numberDone;

    // sell scene
    private boolean selling;
    private boolean moneyVisible = true;
    private BagView bagView;
    private java.util.function.Consumer<String> sellPicked;

    public MartView(RuntimeContext context, List<String> items, String speech, boolean cantSell) {
        this.context = context;
        this.trainer = context.gameState().trainer();
        this.bag = context.gameState().inventory();
        this.pbs = context.pbsData();
        this.pbMessage = new PbMessage(context);
        this.speech = speech;
        this.cantSell = cantSell;
        this.rules = new MartRules(pbs, trainer, bag, context.gameState().martPrices());
        this.stock = rules.filterStock(items);                    // :808-815
        pbPokemonMart();
    }

    public boolean finished() {
        return finished;
    }

    // =====================================================================
    // :807-846 pbPokemonMart
    // =====================================================================

    private void pbPokemonMart() {
        List<String> commands = new ArrayList<>();
        commands.add("购买");                                     // :817
        if (!cantSell) {
            commands.add("卖东西");                               // :818
        }
        commands.add("退出");                                     // :819
        int cmdQuit = commands.size() - 1;
        pbMessage(speech != null ? speech : "欢迎！要买什么？", commands, cmdQuit + 1, 0,   // :820-822
                cmd -> martLoop(commands, cmdQuit, cmd));
    }

    /** :823-841 */
    private void martLoop(List<String> commands, int cmdQuit, int cmd) {
        if (cmd == 0) {                                           // :825 cmdBuy
            pbBuyScreen(() -> afterScreen(commands, cmdQuit));
        } else if (!cantSell && cmd == 1) {                       // :829 cmdSell
            pbSellScreen(() -> afterScreen(commands, cmdQuit));
        } else {
            pbMessage("欢迎再次光临！", () -> {                   // :834
                context.gameState().martPrices().clear();        // :841 $game_temp.clear_mart_prices
                finished = true;
            });
        }
    }

    private void afterScreen(List<String> commands, int cmdQuit) {
        pbMessage("还有什么可以帮助您的吗？", commands, cmdQuit + 1, 0,    // :837-838
                next -> martLoop(commands, cmdQuit, next));
    }

    // =====================================================================
    // :702-768 PokemonMartScreen#pbBuyScreen
    // =====================================================================

    private void pbBuyScreen(Runnable done) {
        selling = false;
        moneyVisible = true;
        listIndex = 0;
        listTop = 0;                                              // :331 pbStartBuyScene
        buyLoop(done);
    }

    private void buyLoop(Runnable done) {
        mode = Mode.BUY_LIST;
        helpVisible = false;
        buyListDone = item -> {
            if (item == null) {                                   // :706 break if item==0
                mode = Mode.IDLE;
                done.run();                                       // :762 @scene.pbEndBuyScene
                return;
            }
            String itemname = displayName(item);                  // :707
            int price = price(item, false);                       // :708
            if (money() < price) {                                // :709
                pbDisplayPaused("拥有的钱不够购买。", null, () -> buyLoop(done));   // :710-711 next
                return;
            }
            if (rules.isImportant(item)) {                        // :713
                pbConfirm(format("你想要{1}，\n这需要支付${2}。确定吗？", itemname, formatted(price)),   // :714-715
                        ok -> {
                            if (!ok) {
                                buyLoop(done);                    // :716 next
                            } else {
                                completePurchase(item, itemname, price, 1, done);   // :718 quantity=1
                            }
                        });
                return;
            }
            int maxafford = rules.maxAfford(price);               // :720-721
            pbChooseNumber(format("购买{1}吗？要买多少个呢？", itemname), item, maxafford, quantity -> {   // :722-723
                if (quantity == 0) {
                    buyLoop(done);                                // :724 next if quantity==0
                    return;
                }
                int total = price * quantity;                     // :725
                pbConfirm(format("决定购买{1}？,购买{2}个是吗？\n好的，一共{3}元。",   // :726-727
                        itemname, String.valueOf(quantity), formatted(total)), ok -> {
                    if (!ok) {
                        buyLoop(done);                            // :728 next
                    } else {
                        completePurchase(item, itemname, total, quantity, done);
                    }
                });
            });
        };
    }

    private ItemCallback buyListDone;

    /** A callback taking the chosen item name, or null for "退出" / B. */
    private interface ItemCallback {
        void accept(String item);
    }

    /** :732-761 */
    private void completePurchase(String item, String itemname, int price, int quantity, Runnable done) {
        if (money() < price) {                                    // :732
            pbDisplayPaused("拥有的钱不够购买。", null, () -> buyLoop(done));   // :733 next
            return;
        }
        rules.buy(item, quantity, price);                         // :736-751 addItem x quantity, setMoney(money-price)
        rules.dropOwnedKeyItems(stock);                           // :752-756
        pbDisplayPaused("好的，给您！\n谢谢惠顾！", () -> playSe("Mart buy item"), () -> {   // :758
            int premiers = rules.premierBonus(item, quantity);    // :760-763
            if (premiers > 0) {
                pbDisplayPaused(format("这是赠送的{1}个纪念球，请收好。", String.valueOf(premiers)), null,   // :768
                        () -> buyLoop(done));
            } else {
                buyLoop(done);
            }
        });
    }

    // =====================================================================
    // :771-805 PokemonMartScreen#pbSellScreen
    // =====================================================================

    private void pbSellScreen(Runnable done) {
        selling = true;
        moneyVisible = false;                                      // :414 @sprites["moneywindow"].visible=false
        bagView = new BagView(context);
        bagView.pickFromBag();                                     // @subscene.pbStartScene(bag)
        sellLoop(done);
    }

    private void sellLoop(Runnable done) {
        if (bagView != null) {
            bagView.model.refresh();                               // the quantities changed after a sale
        }
        mode = Mode.SELL_BAG;
        helpVisible = false;
        moneyVisible = false;
        sellPicked = item -> {
            if (item == null) {                                    // :773 break if item==0
                mode = Mode.IDLE;
                selling = false;
                bagView = null;
                done.run();                                        // :800 @scene.pbEndSellScene
                return;
            }
            String itemname = displayName(item);                   // :774
            int price = price(item, true);                         // :775
            if (!rules.canSell(item)) {                            // :776
                pbDisplayPaused(format("{1}? 不好意思，这个我不能收。", itemname), null, () -> sellLoop(done));   // :777 next
                return;
            }
            int qty = bag.count(item);                             // :780
            if (qty == 0) {
                sellLoop(done);                                    // :781 next if qty==0
                return;
            }
            moneyVisible = true;                                   // :782 pbShowMoney
            if (qty > 1) {
                pbChooseNumber(format("{1}？\n你想要卖多少？", itemname), item, qty,           // :784-785
                        chosen -> afterSellQuantity(item, itemname, price, chosen, done));
            } else {
                afterSellQuantity(item, itemname, price, qty, done);
            }
        };
    }

    /** :787-799 */
    private void afterSellQuantity(String item, String itemname, int fullPrice, int qty, Runnable done) {
        if (qty == 0) {
            moneyVisible = false;                                  // :788 pbHideMoney
            sellLoop(done);                                        // :789 next
            return;
        }
        final int total = MartRules.sellTotal(fullPrice, qty);     // :792-793
        pbConfirm(format("我将用${1}买下。\n可以接受吗？", formatted(total)), ok -> {   // :794
            if (ok) {
                rules.sell(item, qty, total);                      // :795-798
                pbDisplayPaused(format("卖出了{1}，\n获得了${2}。", itemname, formatted(total)),   // :799
                        () -> playSe("Mart buy item"), () -> {
                            moneyVisible = false;                  // :802 pbHideMoney
                            sellLoop(done);
                        });
            } else {
                moneyVisible = false;
                sellLoop(done);
            }
        });
    }

    // =====================================================================
    // Adapter (:4-82)
    // =====================================================================

    private int money() {
        return rules.money();                                      // :6 getMoney
    }

    private int price(String item, boolean sellingPrice) {
        return rules.price(item, sellingPrice);
    }

    private String displayName(String item) {
        return rules.displayName(item);
    }

    private static String formatted(int value) {
        return String.format("%,d", value);                        // Integer#to_s_formatted
    }

    private static String format(String template, String... args) {
        String result = template;
        for (int i = 0; i < args.length; i++) {
            result = result.replace("{" + (i + 1) + "}", args[i]);
        }
        return result;
    }

    private void playSe(String name) {
        if (context.audioManager() != null) {
            context.audioManager().playSe(name, 100, 100);
        }
    }

    // =====================================================================
    // Message helpers (pbMessage = the map message window)
    // =====================================================================

    private void pbMessage(String message, Runnable then) {
        mode = Mode.MESSAGE;
        pbMessage.start(message, null, 0, 0, result -> {
            mode = Mode.IDLE;
            then.run();
        });
    }

    private void pbMessage(String message, List<String> commands, int cmdIfCancel, int defaultCmd, IntConsumer done) {
        mode = Mode.MESSAGE;
        pbMessage.start(message, commands, cmdIfCancel, defaultCmd, result -> {
            mode = Mode.IDLE;
            done.accept(result);
        });
    }

    // =====================================================================
    // Scene helpers: pbDisplay / pbDisplayPaused / pbConfirm / pbChooseNumber (:447-583)
    // =====================================================================

    private void startHelp(String text) {
        helpText = text;
        helpShown = 0;
        helpCounter = 0;
        helpShownDone = false;
        helpVisible = true;
        MenuSe.decision(context.audioManager());                   // pbPlayDecisionSE
    }

    /** :495-516 pbDisplayPaused(msg) { block }: waits for C once the text is complete. */
    private void pbDisplayPaused(String message, Runnable onShown, Runnable then) {
        startHelp(message);
        helpShownCallback = onShown;
        helpBrief = false;
        helpDone = then;
        mode = Mode.DISPLAY;
    }

    /** :470-493 pbDisplay(msg, brief=true): returns as soon as the text is complete; the window stays. */
    private void pbDisplayBrief(String message, Runnable then) {
        startHelp(message);
        helpShownCallback = null;
        helpBrief = true;
        helpDone = then;
        mode = Mode.DISPLAY;
    }

    /** :518-541 pbConfirm */
    private void pbConfirm(String message, java.util.function.Consumer<Boolean> done) {
        startHelp(message);
        helpShownCallback = null;
        helpBrief = false;
        confirmIndex = 0;                                          // :531 cw.index=0
        confirmDone = done;
        helpDone = null;
        mode = Mode.CONFIRM;
    }

    /** :543-583 pbChooseNumber */
    private void pbChooseNumber(String helptext, String item, int maximum, IntConsumer done) {
        curNumber = 1;
        maxNumber = maximum;
        itemPrice = price(item, !selling);
        if (selling) {
            itemPrice /= 2;                                        // :546 itemprice/=2 if !@buying
        }
        bagQuantity = bag.count(item);
        numberDone = done;
        pbDisplayBrief(helptext, () -> mode = Mode.NUMBER);       // :548 pbDisplay(helptext,true)
    }

    // =====================================================================
    // Update
    // =====================================================================

    public boolean update(InputManager input) {
        int ticks = clock.advance();
        switch (mode) {
            case MESSAGE:
                pbMessage.update(input, ticks);
                break;
            case BUY_LIST:
                updateBuyList(input);
                break;
            case DISPLAY:
                updateDisplay(input, ticks);
                break;
            case CONFIRM:
                updateConfirm(input, ticks);
                break;
            case NUMBER:
                updateNumber(input);
                break;
            case SELL_BAG:
                updateSellBag(input);
                break;
            default:
                break;
        }
        return finished;
    }

    /** Window_PokemonMart + pbChooseBuyItem (:586-616). */
    private void updateBuyList(InputManager input) {
        int count = stock.size() + 1;                              // :242 itemCount
        if (input.wasRepeated(GameAction.UP)) {                    // SpriteWindow_Selectable#update wraps around
            listIndex = (listIndex - 1 + count) % count;
            MenuSe.cursor(context.audioManager());
        } else if (input.wasRepeated(GameAction.DOWN)) {
            listIndex = (listIndex + 1) % count;
            MenuSe.cursor(context.audioManager());
        }
        // priv_update_cursor_rect: the cursor stays in the middle of the visible rows.
        int pageRows = pageRows();
        int newTop = listIndex - (pageRows - 1) / 2;
        newTop = Math.max(0, Math.min(newTop, Math.max(0, count - pageRows)));
        listTop = newTop;
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {   // :598
            MenuSe.close(context.audioManager());                  // pbPlayCloseMenuSE
            fireBuy(null);
        } else if (input.wasPressed(GameAction.CONFIRM)) {         // :602
            fireBuy(listIndex < stock.size() ? stock.get(listIndex) : null);
        }
    }

    private void fireBuy(String item) {
        ItemCallback callback = buyListDone;
        if (callback != null) {
            callback.accept(item);
        }
    }

    private int pageRows() {
        float height = ScreenMetrics.logicalHeight() - 126f;       // :343 Graphics.height-126
        return Math.max(1, (int) ((height - BORDER) / ROW));
    }

    /** The letter-by-letter help window (Window_AdvancedTextPokemon, letterbyletter = true). */
    private boolean helpBusy() {
        return helpShown < helpText.length();
    }

    private void tickHelp(int ticks) {
        for (int t = 0; t < ticks && helpBusy(); t++) {
            int speed = context.settings().textspeed;
            int step = speed == 0 ? ((helpCounter++ % 3 == 0) ? 1 : 0) : speed == 2 ? 3 : 1;
            helpShown = Math.min(helpText.length(), helpShown + step);
        }
    }

    private void updateDisplay(InputManager input, int ticks) {
        tickHelp(ticks);
        if (helpBusy()) {
            return;
        }
        if (helpShownCallback != null && !helpShownDone) {         // :507 yield when the text is complete
            helpShownDone = true;
            helpShownCallback.run();
        }
        if (helpBrief) {                                           // :481 return if brief
            Runnable next = helpDone;
            helpDone = null;
            if (next != null) {
                next.run();
            }
            return;
        }
        if (input.wasPressed(GameAction.CONFIRM)) {                // :511 Input.trigger?(C) && cw.resume && !cw.busy?
            helpVisible = false;
            Runnable next = helpDone;
            helpDone = null;
            if (next != null) {
                next.run();
            }
        }
    }

    private void updateConfirm(InputManager input, int ticks) {
        tickHelp(ticks);
        if (helpBusy()) {
            return;
        }
        if (input.wasRepeated(GameAction.UP) || input.wasRepeated(GameAction.DOWN)) {
            confirmIndex = 1 - confirmIndex;
            MenuSe.cursor(context.audioManager());
        }
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {      // :533
            helpVisible = false;
            java.util.function.Consumer<Boolean> next = confirmDone;
            confirmDone = null;
            next.accept(false);
        } else if (input.wasPressed(GameAction.CONFIRM)) {                                    // :538
            helpVisible = false;
            java.util.function.Consumer<Boolean> next = confirmDone;
            confirmDone = null;
            next.accept(confirmIndex == 0);
        }
    }

    private void updateNumber(InputManager input) {
        if (input.wasRepeated(GameAction.LEFT)) {                  // :563
            MenuSe.cursor(context.audioManager());
            curNumber -= 10;
            if (curNumber < 1) {
                curNumber = 1;
            }
        } else if (input.wasRepeated(GameAction.RIGHT)) {          // :568
            MenuSe.cursor(context.audioManager());
            curNumber += 10;
            if (curNumber > maxNumber) {
                curNumber = maxNumber;
            }
        } else if (input.wasRepeated(GameAction.UP)) {             // :573
            MenuSe.cursor(context.audioManager());
            curNumber += 1;
            if (curNumber > maxNumber) {
                curNumber = 1;
            }
        } else if (input.wasRepeated(GameAction.DOWN)) {           // :578
            MenuSe.cursor(context.audioManager());
            curNumber -= 1;
            if (curNumber < 1) {
                curNumber = maxNumber;
            }
        } else if (input.wasPressed(GameAction.CONFIRM)) {         // :583
            MenuSe.decision(context.audioManager());
            finishNumber(curNumber);
        } else if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {   // :587
            MenuSe.close(context.audioManager());                  // pbPlayCancelSE
            finishNumber(0);
        }
    }

    private void finishNumber(int result) {
        helpVisible = false;                                       // :626 helpwindow.visible=false
        IntConsumer next = numberDone;
        numberDone = null;
        mode = Mode.IDLE;
        next.accept(result);
    }

    private void updateSellBag(InputManager input) {
        if (bagView == null) {
            return;
        }
        if (bagView.update(input)) {
            String picked = bagView.pickedItem();                  // 0 on cancel / "关闭背包"
            java.util.function.Consumer<String> next = sellPicked;
            sellPicked = null;
            if (next != null) {
                next.accept(picked);
            }
        }
    }

    // =====================================================================
    // Render
    // =====================================================================

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, WindowSkin speechSkin, MenuFont detail) {
        float w = ScreenMetrics.logicalWidth();
        float h = ScreenMetrics.logicalHeight();
        if (mode == Mode.MESSAGE && !selling) {
            pbMessage.render(b, a, f, skin, speechSkin, w, h);
            return;
        }
        if (selling && bagView != null) {
            bagView.render(b, a, f, skin, speechSkin, detail);     // the PokemonBag_Scene subscene
            renderOverlays(b, a, f, skin, h, w);
            return;
        }
        // :336-340 martScreen / martScreenf
        Texture background = a.graphic("Pictures", trainer.gender == pokemon.runtime.pokemon.PokemonStats.FEMALE
                ? "martScreenf" : "martScreen");
        if (background != null) {
            b.draw(background, 0f, 0f, w, h);
        }
        renderItemWindow(b, a, f, w, h);
        renderItemText(b, a, f, w, h);
        renderOverlays(b, a, f, skin, h, w);
    }

    /** Window_PokemonMart#drawItem (:247-269) in a skinless 346x(h-126) window at (w-332, 12). */
    private void renderItemWindow(SpriteBatch b, MenuAssets a, MenuFont f, float w, float h) {
        float x = w - 316f - 16f;
        float y = 12f;
        float height = h - 126f;
        float contentsX = x + 16f;
        float contentsTop = y + 16f;
        int count = stock.size() + 1;
        int rows = pageRows();
        Texture arrow = a.graphic("Pictures", "martSel");
        for (int i = listTop; i < Math.min(count, listTop + rows + 1); i++) {
            float rowTop = contentsTop + (i - listTop) * ROW;
            if (rowTop + ROW > y + height - 16f + 1f) {
                continue;
            }
            float textY = h - (rowTop + 2f + (ROW - f.lineHeight()) / 2f);
            if (i == listIndex && arrow != null) {
                b.draw(arrow, contentsX, h - rowTop - arrow.getHeight());    // drawCursor
            }
            float textX = contentsX + 16f;
            if (i == count - 1) {
                f.draw(b, "退出", textX, textY, WHITE, LIST_SHADOW);        // :251
            } else {
                String item = stock.get(i);
                String qty = " " + formatted(price(item, false));          // getDisplayPrice " {1}"
                f.draw(b, displayName(item), textX, textY, WHITE, LIST_SHADOW);
                float xQty = contentsX + 16f + (314f - 16f) - f.width(qty) - 2f - 16f;
                f.draw(b, qty, xQty, textY, WHITE, LIST_SHADOW);
            }
        }
        // the icon and the description follow the highlighted row (:597-601)
        String current = listIndex < stock.size() ? stock.get(listIndex) : null;
        if (current != null) {
            Texture icon = itemIcon(a, current);
            if (icon != null) {
                // ItemIconSprite.new(48, Graphics.height-50): centred on that point, 48x48
                b.draw(icon, 48f - 24f, h - (h - 50f) - 24f, 0, 0, 48, 48);
            }
        }
    }

    private void renderItemText(SpriteBatch b, MenuAssets a, MenuFont f, float w, float h) {
        String current = listIndex < stock.size() && mode != Mode.SELL_BAG ? stock.get(listIndex) : null;
        String text = current == null ? "退出购物" : description(current);          // :594
        float x = 70f + 16f;
        float top = h - 96f - 16f + 16f;
        String[] lines = wrap(f, text == null ? "" : text, w - 64f - 32f);
        for (int i = 0; i < lines.length && i < 3; i++) {
            f.draw(b, lines[i], x, h - (top + i * ROW + (ROW - f.lineHeight()) / 2f), WHITE, TEXT_SHADOW);
        }
    }

    /** The money window, the help window, the Yes/No window and the number windows. */
    private void renderOverlays(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, float h, float w) {
        if (moneyVisible) {
            renderMoneyWindow(b, a, f, h);
        }
        if (mode == Mode.MESSAGE && selling) {
            WindowSkin speechSkin = skin;
            pbMessage.render(b, a, f, skin, speechSkin, w, h);
            return;
        }
        if (!helpVisible) {
            return;
        }
        float helpHeight = BORDER + 2 * ROW;                       // pbBottomLeftLines(cw,2)
        float helpTop = h - helpHeight;
        Color[] tc = MenuPanel.textColors(skin);
        MenuPanel.window(b, a, skin, 0f, 0f, w, helpHeight);
        String visible = helpText.substring(0, Math.min(helpShown, helpText.length()));
        String[] lines = visible.split("\n", -1);
        for (int i = 0; i < lines.length && i < 2; i++) {
            float top = helpTop + 16f + i * ROW + (ROW - f.lineHeight()) / 2f;
            f.draw(b, lines[i], 16f, h - top, tc[0], tc[1]);
        }
        if (mode == Mode.CONFIRM && !helpBusy()) {                 // :523-526 Window_CommandPokemon "是的 / 不"
            String[] labels = {"是的", "不"};
            float width = Math.max(f.width(labels[0]), f.width(labels[1])) + 16f + 16f + 4f + BORDER;
            float height = BORDER + 2 * ROW;
            float x = w - width;
            float top = helpTop - height;
            MenuPanel.window(b, a, skin, x, h - top - height, width, height);
            for (int i = 0; i < 2; i++) {
                float rowTop = top + 16f + i * ROW;
                f.draw(b, labels[i], x + 32f, h - (rowTop + (ROW - f.lineHeight()) / 2f), tc[0], tc[1]);
            }
            MenuPanel.cursor(b, a, skin, x, h - top - height, height, confirmIndex, confirmIndex, ROW);
        }
        if (mode == Mode.NUMBER) {
            // numwindow 224x64 bottom right, inbagwindow 190x64 bottom left, both above the help window (:557-577)
            float numTop = helpTop - 64f;
            MenuPanel.window(b, a, skin, w - 224f, h - numTop - 64f, 224f, 64f);
            float baseline = numTop + 16f + (ROW - f.lineHeight()) / 2f;
            f.draw(b, "x" + curNumber, w - 224f + 16f, h - baseline, NUM_BASE, NUM_SHADOW);
            String total = " " + formatted(curNumber * itemPrice);
            f.drawRight(b, total, w - 16f, h - baseline, NUM_BASE, NUM_SHADOW);
            if (!selling) {                                        // :571 inbagwindow.visible=@buying
                MenuPanel.window(b, a, skin, 0f, h - numTop - 64f, 190f, 64f);
                f.draw(b, "背包里：", 16f, h - baseline, NUM_BASE, NUM_SHADOW);
                f.drawRight(b, bagQuantity + "  ", 190f - 16f, h - baseline, NUM_BASE, NUM_SHADOW);
            }
        }
    }

    /** :362-376 moneywindow: "零花钱：\r\n<r>{1}" at (0,0), 190x96 (buy) / 186x96 goldskin (sell). */
    private void renderMoneyWindow(SpriteBatch b, MenuAssets a, MenuFont f, float h) {
        WindowSkin moneySkin = a.skin(selling ? "goldskin" : "choice 29");
        float width = selling ? 186f : 190f;
        MenuPanel.window(b, a, moneySkin, 0f, h - 96f, width, 96f);
        Color base = selling ? GOLD_BASE : WHITE;
        Color shadow = selling ? GOLD_SHADOW : TEXT_SHADOW;
        float line = (ROW - f.lineHeight()) / 2f;
        f.draw(b, "零花钱：", 16f, h - (16f + line), base, shadow);
        f.drawRight(b, "$" + formatted(money()), width - 16f, h - (16f + ROW + line), base, shadow);
    }

    private Texture itemIcon(MenuAssets a, String id) {
        return ItemIcons.of(a, pbs, id);
    }

    private String description(String id) {
        PbsData.Item data = pbs == null ? null : pbs.item(id);
        return data == null || data.description == null ? "" : data.description;
    }

    private static String[] wrap(MenuFont f, String text, float width) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < paragraph.length(); i++) {
                current.append(paragraph.charAt(i));
                if (f.width(current.toString()) > width && current.length() > 1) {
                    current.setLength(current.length() - 1);
                    lines.add(current.toString());
                    current.setLength(0);
                    current.append(paragraph.charAt(i));
                }
            }
            lines.add(current.toString());
        }
        return lines.toArray(new String[0]);
    }

    public void dispose() {
        bagView = null;
    }
}
