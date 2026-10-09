package pokemon.runtime.ui.menu;

import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.Party;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.TrainerState;
import pokemon.runtime.ui.WindowSkin;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

/**
 * 段 PScreen_PC (Scripts.rxdata #224, 259 行；原文 {@code __r20-ref/ruby/PScreen_PC.rb})：
 * {@code pbPokeCenterPC}(:211-221) 和它调用的 PC 列表 {@code PokemonPCList}(:225-254)、
 * {@code StorageSystemPC}(:148-201)、{@code TrainerPC}(:123-136)。
 *
 * <p>阻塞的 {@code pbMessage} / {@code pbShowCommandsWithHelp} 拆成状态机，续体接着往下走；
 * 寄存系统本体是 {@link StorageView}，作为子视图打开。</p>
 *
 * <p>登记: ①{@code PokemonPCList.registerPC} 在原文还登记了 PurifyChamberPC
 * (PScreen_PurifyChamber:267，shouldShow? = $PokemonGlobal.seenPurifyChamber) 和 HallOfFamePC
 * (PScreen_HallOfFame:517，hallOfFameLastNumber>0)，净化室没有建模，不显示；冠军殿堂已建模（`hallOfFameLastNumber>0` 时显示）；
 * 但 {@code callCommand} 的 {@code cmd>=@@pclist.length} 仍按 4 台电脑计(:242)；
 * ②{@code $PokemonGlobal.seenStorageCreator}/{@code pbGetStorageCreator} 未建模，
 * StorageSystemPC 的名字走 {@code 精灵寄存系统} 分支；③「整理道具」(PCItemStorage 及 Withdraw/Toss/
 * DepositItemScene)、邮件、Follower_Main:571 的 dependentEvents.come_back 未建模，对应选项留空；
 * ④淡入淡出未建模。</p>
 */
public final class PokeCenterPcView {
    private static final int ROW = 32;
    private static final int BORDER = 32;

    private enum Mode { MESSAGE, HELPMENU, STORAGE, ITEMSTORAGE, DEPOSIT, HALL, IDLE }

    private final RuntimeContext context;
    private final TrainerState trainer;
    private final PbMessage pbMessage;
    private final MenuClock clock = new MenuClock();
    private Mode mode = Mode.IDLE;
    private boolean finished;
    private StorageView storageView;
    private HallOfFameView hallView;
    private Runnable hallDone;
    private Runnable storageDone;
    private ItemStorageView itemStorageView;
    private BagView depositBag;
    private Runnable itemDone;
    private java.util.function.BiConsumer<Predicate<String>, Consumer<String>> itemHost;
    private Consumer<Pokemon> bagHost;
    private float screenH;

    // pbShowCommandsWithHelp (Messages:1391-1438)
    private List<String> helpLabels = new ArrayList<>();
    private List<String> helpTexts = new ArrayList<>();
    private int helpCmdIfCancel;
    private int helpIndex;
    private IntConsumer helpDone;

    public PokeCenterPcView(RuntimeContext context) {
        this(context, false);
    }

    /** @param trainerOnly {@code pbTrainerPC} (PScreen_PC:205-209): straight to the player's own PC. */
    public PokeCenterPcView(RuntimeContext context, boolean trainerOnly) {
        this.context = context;
        this.trainer = context.gameState().trainer();
        this.pbMessage = new PbMessage(context);
        if (trainerOnly) {
            pbMessage(intl("\\se[PC open]{1}打开了电脑", trainer.name), () ->           // :206
                    trainerPcMenu(0, () -> {                                           // :207 pbTrainerPCMenu
                        playSe("PC close");                                            // :208
                        finished = true;
                    }));
        } else {
            pbPokeCenterPC();
        }
    }

    public void itemHost(java.util.function.BiConsumer<Predicate<String>, Consumer<String>> host) {
        this.itemHost = host;
    }

    public void bagHost(Consumer<Pokemon> host) {
        this.bagHost = host;
    }

    // =====================================================================
    // :211-221 pbPokeCenterPC
    // =====================================================================

    private void pbPokeCenterPC() {
        pbMessage(intl("\\se[PC open]{1}打开了电脑", trainer.name), () -> pcLoop(0));   // :212
    }

    /** :214-219 */
    private void pcLoop(int command) {
        List<String> commands = getCommandList();                // :215
        pbMessage("要登录哪一个服务？", commands, commands.size(), command, picked -> {   // :216
            callCommand(picked, ok -> {                          // :218
                if (ok) {
                    pcLoop(picked);
                } else {
                    playSe("PC close");                          // :220
                    finished = true;
                }
            });
        });
    }

    /** :232-239 PokemonPCList.getCommandList */
    private List<String> getCommandList() {
        List<String> commands = new ArrayList<>();
        commands.add(storageSystemName());                       // StorageSystemPC#name (shouldShow? = true)
        commands.add(intl("{1}的电脑", trainer.name));           // TrainerPC#name (shouldShow? = true)
        if (trainer.hallOfFameLastNumber > 0) {                  // PScreen_HallOfFame:517 HallOfFamePC#shouldShow?
            commands.add("冠军殿堂");                               // :521 name
        }
        commands.add("关闭电脑");                                // :237
        return commands;
    }

    /** :153-159 StorageSystemPC#name — 登记: seenStorageCreator 未建模，恒走「精灵寄存系统」。 */
    private String storageSystemName() {
        return "精灵寄存系统";
    }

    /** :241-253 PokemonPCList.callCommand (@@pclist 共 4 台，只有前 2 台显示，见类注释) */
    private void callCommand(int cmd, Consumer<Boolean> done) {
        if (cmd < 0 || cmd >= 4) {                               // :242
            done.accept(false);
            return;
        }
        int i = 0;                                               // :243
        // 注册顺序: StorageSystemPC, TrainerPC, PurifyChamberPC(不显示), HallOfFamePC(不显示)
        if (i == cmd) {                                          // :246
            storageSystemAccess(() -> done.accept(true));        // :247 pc.access
            return;
        }
        i += 1;                                                  // :250
        if (i == cmd) {
            trainerPcAccess(() -> done.accept(true));
            return;
        }
        if (trainer.hallOfFameLastNumber > 0) {                  // PurifyChamberPC does not show; HallOfFamePC (shouldShow?)
            i += 1;
            if (i == cmd) {
                pbMessage("\\se[PC access]进入了冠军殿堂。", () -> {          // PScreen_HallOfFame:525 access
                    hallView = HallOfFameView.pc(context);       // :526 pbHallOfFamePC
                    hallDone = () -> done.accept(true);
                    mode = Mode.HALL;
                });
                return;
            }
        }
        done.accept(false);                                      // :252
    }

    // =====================================================================
    // :148-201 StorageSystemPC#access
    // =====================================================================

    private void storageSystemAccess(Runnable then) {
        pbMessage("\\se[PC access]登录了精灵寄存服务！", () -> storageMenu(0, then));   // :162
    }

    private void storageMenu(int command, Runnable then) {
        List<String> commands = Arrays.asList("整理盒子", "取出宝可梦", "存放宝可梦", "登出");   // :166-169
        List<String> help = Arrays.asList("整理您存在盒子和队伍里的宝可梦。", "取出您存在盒子里的宝可梦。",
                "把您队伍里的宝可梦存放在盒子里。", "关闭服务");                                 // :170-173
        pbShowCommandsWithHelp(commands, help, -1, command, picked -> {   // :165
            if (picked >= 0 && picked < 3) {                     // :175
                Party party = trainer.party;
                if (picked == 1) {                               // :176 Withdraw
                    if (party.size() >= 6) {                     // :177
                        pbMessage("您的队伍满了！", () -> storageMenu(picked, then));   // :178-179 next
                        return;
                    }
                } else if (picked == 2) {                        // :181 Deposit
                    int count = 0;                               // :182
                    for (Pokemon p : party.members()) {          // :183
                        if (p != null && !p.egg && p.hp > 0) count += 1;   // :184
                    }
                    if (count <= 1) {                            // :186
                        pbMessage("不能存放最后的宝可梦！", () -> storageMenu(picked, then));   // :187-188 next
                        return;
                    }
                }
                // :191-195 pbFadeOutIn { PokemonStorageScene / PokemonStorageScreen#pbStartScreen(command) }
                openStorage(picked, () -> storageMenu(picked, then));
            } else {
                then.run();                                      // :197 break
            }
        });
    }

    private void openStorage(int command, Runnable then) {
        storageView = new StorageView(context, command);
        storageView.itemHost(itemHost);
        storageView.bagHost(bagHost);
        storageDone = then;
        mode = Mode.STORAGE;
    }

    // =====================================================================
    // :123-136 TrainerPC#access / :105-119 pbTrainerPCMenu
    // =====================================================================

    private void trainerPcAccess(Runnable then) {
        pbMessage(intl("\\se[PC access]访问了{1}的电脑。", trainer.name), () -> trainerPcMenu(0, then));   // :133-134
    }

    private void trainerPcMenu(int command, Runnable then) {
        pbMessage("要做什么？", Arrays.asList("整理道具", "邮件", "关闭电脑"), -1, command, picked -> {   // :108-112
            switch (picked) {
                case 0:                                          // :114
                    pbPCItemStorage(0, () -> trainerPcMenu(picked, then));
                    break;
                case 1:                                          // :115
                    pbPCMailbox(() -> trainerPcMenu(picked, then));
                    break;
                default:
                    then.run();                                  // :116 break
                    break;
            }
        });
    }

    /** :4-54 pbPCItemStorage */
    private void pbPCItemStorage(int command, Runnable then) {
        List<String> commands = Arrays.asList("取出道具", "储存道具", "扔掉道具", "退出");        // :8-11
        List<String> help = Arrays.asList("从电脑中取出物品。", "将道具储存在电脑中。", "扔掉电脑里储存的道具。", "返回上一级。");   // :12-14
        pbShowCommandsWithHelp(commands, help, -1, command, picked -> {   // :7
            switch (picked) {
                case 0:                                          // :18 Withdraw Item
                case 2: {                                        // :37 Toss Item
                    pokemon.runtime.state.PcItemStorage storage = pcItemStorage();   // :19-21
                    Runnable again = () -> pbPCItemStorage(picked, then);
                    if (storage.empty()) {                       // :22
                        pbMessage("没有道具。", again);          // :23
                    } else {                                     // :25 pbFadeOutIn { WithdrawItemScene / TossItemScene }
                        itemStorageView = new ItemStorageView(context,
                                picked == 0 ? ItemStorageView.Kind.WITHDRAW : ItemStorageView.Kind.TOSS, storage);
                        itemDone = again;
                        mode = Mode.ITEMSTORAGE;
                    }
                    break;
                }
                case 1: {                                        // :31 Deposit Item: the bag in deposit mode
                    depositBag = new BagView(context);
                    depositBag.depositMode(pcItemStorage());
                    itemDone = () -> pbPCItemStorage(picked, then);
                    mode = Mode.DEPOSIT;
                    break;
                }
                default:
                    then.run();                                  // :51 break
                    break;
            }
        });
    }

    /** {@code $PokemonGlobal.pcItemStorage} created on first use (:19-21, PCItemStorage#initialize: a Potion). */
    private pokemon.runtime.state.PcItemStorage pcItemStorage() {
        pokemon.runtime.state.FieldGlobals globals = context.gameState().fieldGlobals();
        if (globals.pcItemStorage == null) {
            globals.pcItemStorage = pokemon.runtime.state.PcItemStorage.withPotion(
                    context.pbsData() != null && context.pbsData().item("POTION") != null);
        }
        return globals.pcItemStorage;
    }

    /** :56-103 pbPCMailbox — 登记: 邮件未建模，邮箱恒空 ⇒ 走 :57-58 的「电脑里没有邮件。」。 */
    private void pbPCMailbox(Runnable then) {
        pbMessage("电脑里没有邮件。", then);                     // :58
    }

    // =====================================================================
    // 他段: Messages #71 pbMessage / pbShowCommandsWithHelp
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

    /** Messages:1391-1438 pbShowCommandsWithHelp(nil,commands,help,cmdIfCancel,defaultCmd) */
    private void pbShowCommandsWithHelp(List<String> commands, List<String> help, int cmdIfCancel, int defaultCmd, IntConsumer done) {
        helpLabels = commands;
        helpTexts = help;
        helpCmdIfCancel = cmdIfCancel;
        helpIndex = defaultCmd;                                  // :1402
        helpDone = done;
        mode = Mode.HELPMENU;
    }

    private void updateHelpMenu(InputManager input) {
        int n = helpLabels.size();
        if (input.wasRepeated(GameAction.DOWN)) {                // SpriteWindow_SelectableEx
            if (input.wasPressed(GameAction.DOWN) || helpIndex < n - 1) {
                helpIndex = (helpIndex + 1) % n;
                MenuSe.cursor(context.audioManager());
            }
        } else if (input.wasRepeated(GameAction.UP)) {
            if (input.wasPressed(GameAction.UP) || helpIndex > 0) {
                helpIndex = Math.floorMod(helpIndex - 1, n);
                MenuSe.cursor(context.audioManager());
            }
        }
        boolean cancel = input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU);
        if (cancel) {                                            // :1416
            if (helpCmdIfCancel > 0) {                           // :1417
                finishHelp(helpCmdIfCancel - 1);
            } else if (helpCmdIfCancel < 0) {                    // :1420
                finishHelp(helpCmdIfCancel);
            }
        } else if (input.wasPressed(GameAction.CONFIRM)) {       // :1425
            finishHelp(helpIndex);
        }
    }

    private void finishHelp(int command) {
        IntConsumer done = helpDone;
        helpDone = null;
        mode = Mode.IDLE;
        done.accept(command);
    }

    private void playSe(String name) {
        if (context.audioManager() != null) context.audioManager().playSe(name, 100, 100);
    }

    private static String intl(String template, Object... args) {
        String out = template;
        for (int i = 0; i < args.length; i++) out = out.replace("{" + (i + 1) + "}", String.valueOf(args[i]));
        return out;
    }

    // =====================================================================
    // update / render
    // =====================================================================

    /** @return true when the PC screen closed (pbSEPlay("PC close") already played). */
    public boolean update(InputManager input) {
        int ticks = clock.advance();
        if (mode == Mode.STORAGE) {
            if (storageView != null && storageView.update(input)) {
                storageView.dispose();
                storageView = null;
                Runnable then = storageDone;
                storageDone = null;
                mode = Mode.IDLE;
                if (then != null) then.run();
            }
            return finished;
        }
        if (mode == Mode.HALL) {
            if (hallView != null && hallView.update(input)) {
                hallView = null;
                Runnable then = hallDone;
                hallDone = null;
                mode = Mode.IDLE;
                if (then != null) then.run();
            }
            return finished;
        }
        if (mode == Mode.ITEMSTORAGE || mode == Mode.DEPOSIT) {
            boolean closed = mode == Mode.ITEMSTORAGE ? itemStorageView.update(input) : depositBag.update(input);
            if (closed) {
                itemStorageView = null;
                depositBag = null;
                Runnable then = itemDone;
                itemDone = null;
                mode = Mode.IDLE;
                if (then != null) then.run();
            }
            return finished;
        }
        if (mode == Mode.MESSAGE) {
            pbMessage.update(input, ticks);
        } else if (mode == Mode.HELPMENU) {
            updateHelpMenu(input);
        }
        return finished;
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, WindowSkin speech, MenuFont smallFont) {
        float w = ScreenMetrics.logicalWidth(), h = ScreenMetrics.logicalHeight();
        screenH = h;
        if (mode == Mode.STORAGE && storageView != null) {
            storageView.render(b, a, f, skin, speech, smallFont);
            return;
        }
        if (mode == Mode.HALL && hallView != null) {
            hallView.render(b, a, f, skin);
            return;
        }
        if (mode == Mode.ITEMSTORAGE && itemStorageView != null) {
            itemStorageView.render(b, a, f, skin, speech);
            return;
        }
        if (mode == Mode.DEPOSIT && depositBag != null) {
            depositBag.render(b, a, f, skin, speech, smallFont);
            return;
        }
        if (mode == Mode.MESSAGE) {
            pbMessage.render(b, a, f, skin, speech, w, h);
        } else if (mode == Mode.HELPMENU) {
            // Messages:1393 pbCreateMessageWindow: bottom-left 2 lines, speech frame; text = help[index], no letter-by-letter
            float msgHeight = BORDER + 2 * ROW;
            float msgTop = h - msgHeight;
            Color[] mc = MenuPanel.textColors(speech);
            MenuPanel.window(b, a, speech, 0f, 0f, w, msgHeight);
            String[] lines = helpTexts.get(Math.min(helpIndex, helpTexts.size() - 1)).split("\n", -1);
            for (int i = 0; i < lines.length && i < 2; i++) {
                float top = msgTop + 16f + i * ROW + (ROW - f.lineHeight()) / 2f;
                f.draw(b, lines[i], 16f, h - top, mc[0], mc[1]);
            }
            // :1397-1401 Window_CommandPokemonEx at (0,0), height capped to msgwin.y
            Color[] cc = MenuPanel.textColors(skin);
            float width = 0f;
            for (String label : helpLabels) width = Math.max(width, f.width(label));
            width += 16f + 16f + 4f + BORDER;
            float height = Math.min(BORDER + helpLabels.size() * ROW, msgTop);
            MenuPanel.window(b, a, skin, 0f, h - height, width, height);
            for (int i = 0; i < helpLabels.size(); i++) {
                float rowTop = 16f + i * ROW;
                f.draw(b, helpLabels.get(i), 32f, h - (rowTop + (ROW - f.lineHeight()) / 2f), cc[0], cc[1]);
            }
            MenuPanel.cursor(b, a, skin, 0f, h - height, height, helpIndex, helpIndex, ROW);
        }
    }
}
