package pokemon.runtime.ui.menu;

import pokemon.runtime.pokemon.BattlerBitmaps;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.battle.PBNatures;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.field.EvolutionWorld;
import pokemon.runtime.field.ItemHandlers;
import pokemon.runtime.field.ItemScene;
import pokemon.runtime.field.ItemTask;
import pokemon.runtime.field.TaskItemScene;
import pokemon.runtime.pokemon.BallTypes;
import pokemon.runtime.pokemon.ItemUse;
import pokemon.runtime.pokemon.PBEvolution;
import pokemon.runtime.pokemon.PBExperience;
import pokemon.runtime.pokemon.Party;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;
import pokemon.runtime.pokemon.TrainerState;
import pokemon.runtime.ui.WindowSkin;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 段 PScreen_Party (Scripts.rxdata #210, 1697 行；原文抽取见
 * {@code __r20-ref/ruby/PScreen_Party.rb}，对账表见 docs/stage4-pscreen-party-gap.md)。
 *
 * <p>RGSS 的精灵/窗口对象在这里是即时绘制：{@code PokemonPartySelectionPanel} 等的
 * 状态位（selected / preselected / switching / text）保存在本类的数组里，
 * {@code refresh} 的副作用（把选中格的宝可梦交给详情面板）由 {@link #refreshPanel} 保留。
 * 原文里的阻塞 {@code loop do ... end}（pbChoosePokemon / pbShowCommands / pbDisplay /
 * pbDisplayConfirm / pbWait）拆成 {@link Mode} 状态机，每个调用点的后续逻辑是一个续体。
 * 坐标全部用 RGSS 的左上原点写（与原文一一对应），只在 {@link #img}/{@link #txt}/{@link #window}
 * 里换成 libGDX 的下原点。</p>
 *
 * <p>登记（整段共通）：①淡入淡出(pbFadeInAndShow/pbFadeOutAndHide/pbFadeOutIn)未建模；
 * ②{@code Input.repeat?} 用 {@code InputManager#wasRepeated}（40fps 帧计数），等待/图标动画/逐字显示按 {@link MenuClock} 的 40fps tick；③原文的 Windowskins/bw choice
 * 在 Graphics/Windowskins 里不存在，SpriteWindow:925 {@code setSkin} 对不存在的文件直接
 * {@code return}，窗口保持默认系统窗口皮肤，这里同样用传入的 {@code skin}；④
 * {@code Input::F5}(寄存系统) 没有对应的 {@link GameAction}；⑤pbPlayCursorSE/DecisionSE
 * 在原文先取 $data_system 的音效，未建模，用 {@link MenuSe}。</p>
 */
public final class PartyView {
    private static final Color LINE = new Color(1f, 1f, 1f, 1f);                                 // :279 line
    private static final Color OUTLINE = new Color(132f / 255f, 132f / 255f, 132f / 255f, 1f);   // :280 outline
    private static final Color DARK = new Color(90f / 255f, 82f / 255f, 82f / 255f, 1f);         // :283
    private static final Color DARK_SHADOW = new Color(165f / 255f, 165f / 255f, 173f / 255f, 1f);
    private static final Color STORAGE_BASE = new Color(248f / 255f, 248f / 255f, 248f / 255f, 1f); // :669
    private static final Color COLOR_KEY_BASE = new Color(0f, 80f / 255f, 160f / 255f, 1f);        // :133
    private static final Color COLOR_KEY_SHADOW = new Color(128f / 255f, 192f / 255f, 240f / 255f, 1f); // :134

    private static final int SLOT_WIDTH = 72;       // :167/:429
    private static final int CANCEL = 6;             // :901 cancelsprite (multiselect=false)
    private static final int NUMSPRITES = 7;         // :938 numsprites (multiselect=false)
    private static final int ROW = 32;               // SpriteWindow_text:759 rowHeight
    private static final int BORDER = 32;            // SpriteWindow:445/453 borderX/borderY
    private static final char PAUSE = '\u0001';      // _INTL("...\1")

    private enum Mode { CHOOSE, COMMANDS, MESSAGE, CONFIRM, WAIT, SUMMARY, NUMBER, TOPRIGHT }

    /** pbChoosePokemon 的返回续体: result = 选中的格号/-1，switchRequested = {@code return [1,@activecmd]}。 */
    private interface ChooseDone {
        void done(int result, boolean switchRequested);
    }

    private interface IntDone {
        void done(int value);
    }

    private final RuntimeContext context;
    private final MenuClock clock = new MenuClock();
    private final TrainerState trainer;
    private Party party;
    /** Battle only: display slot -> index in the trainer's party (pbPlayerDisplayParty puts the active battlers first). */
    private int[] displayMap;
    private java.util.function.Consumer<Pokemon> bagHost;
    private Runnable storageHost;
    private boolean pendingHardRefresh;
    private SummaryView summary;
    /** 226 PokemonEvolutionScene, running over the party screen (the command 进化, or an evolution stone's handler). */
    private EvolutionView evolutionView;
    private Runnable evolutionDone;
    private RelearnerView relearnView;
    private Runnable relearnDone;
    /** :592-598 pbCheckEvolution(@pokemon)>0 of each panel, recomputed on the next draw after a refresh (null = stale). */
    private final Boolean[] evolvable = new Boolean[6];
    /** The summary screen's forget mode of an item handler's pbForgetMove. */
    private SummaryView forgetSummary;
    private Runnable summaryYield;
    private Runnable summaryThen;

    // ---- PokemonParty_Scene ivars (:645-651, :698)
    private int activecmd;
    private boolean canAccessStorage;
    private String helpText = "";
    private int helpWidth;
    private float resolvedHelpWidth = 378f;
    private List<String> resolvedHelpLines = new ArrayList<>();
    private boolean helpVisible = true;
    private final boolean[] selected = new boolean[NUMSPRITES];
    private final boolean[] preselected = new boolean[6];
    private final boolean[] switching = new boolean[6];
    private final String[] annotations = new String[6];
    private Pokemon detailsPokemon;
    private String detailsText = "";
    private final ExpBar expBar = new ExpBar();
    // PokemonIconSprite (Pokemon_Sprites:86-212) per-slot animation state
    private final int[] iconCounter = new int[6];
    private final int[] iconCurrent = new int[6];
    private final int[] iconNum = new int[6];

    // ---- 阻塞调用点的状态机
    private Mode mode = Mode.CHOOSE;
    private boolean finished;
    private ChooseDone chooseDone;
    private boolean chooseSwitching;
    private int chooseCanswitch;
    private List<String> cmdLabels = new ArrayList<>();
    private List<Integer> cmdColorKey = new ArrayList<>();
    private int cmdIndex;
    private IntDone cmdDone;
    private String messageText = "";
    private int messageShown;
    private boolean messagePausing;
    private Runnable messageDone;
    private java.util.function.Consumer<Boolean> confirmDone;
    private int confirmIndex;
    private int waitFrames;
    private Runnable waitDone;

    // ---- 战斗内换人 (BattleScreen.openPartyScreen 复用；该调用不在 PScreen_Party)
    private boolean battleChoose;
    private boolean battleCanCancel = true;
    private int chosenIndex = -1;

    public PartyView(RuntimeContext context) {
        this(context, true);
    }

    private PartyView(RuntimeContext context, boolean top) {
        this.context = context;
        this.trainer = context.gameState().trainer();
        this.party = trainer.party;
        // :1297-1300 pbPokemonScreen -> @scene.pbStartScene(@party, 请选择宝可梦。, nil, false, $Trainer.pokepc)
        startScene(party.size() > 1 ? "请选择宝可梦。" : "选择宝可梦或取消。", top && trainer.pokepc);
        if (top) {
            startTop();
        }
    }

    /** The summary's "携带道具" hands off to the bag (the host owns both views). */
    public void bagHost(java.util.function.Consumer<Pokemon> host) {
        this.bagHost = host;
    }

    /** PScreen_Party:925-930: [F]:寄存系统 hands off to the storage system (the host owns both views). */
    public void storageHost(Runnable host) {
        this.storageHost = host;
    }

    /**
     * 登记: 战斗内的 pbPartyScreen 不在 PScreen_Party（PokeBattle_Scene 自己的段）。
     * 这里只复用本段的选择格逻辑(pbChoosePokemon/pbChangeSelection)，选中后把格号交还战斗。
     */
    public void battleChoose(boolean value) {
        battleChoose(value, true);
    }

    /**
     * Battle_Phase_Command:109-115 {@code pbPlayerDisplayParty}: the party as the battle shows it. {@code order[i]} is
     * the trainer's party index shown in slot i; results of the chooser are mapped back.
     */
    public void displayOrder(int[] order) {
        Party shown = new Party();
        for (int index : order) {
            shown.add(trainer.party.get(index));
        }
        this.party = shown;
        this.displayMap = order.clone();
        startScene(party.size() > 1 ? "请选择宝可梦。" : "选择宝可梦或取消。", false);
    }

    /** The trainer's party index of a displayed slot. */
    public int realIndex(int shown) {
        return displayMap != null && shown >= 0 && shown < displayMap.length ? displayMap[shown] : shown;
    }

    public void battleChoose(boolean value, boolean canCancel) {
        if (!value) {
            party = trainer.party;                                // the battle's display order ends with the screen
            displayMap = null;
        }
        battleChoose = value;
        battleCanCancel = canCancel;
        chosenIndex = -1;
        summary = null;
        finished = false;
        if (value) {
            startScene("选择宝可梦或取消。", false);
            beginChoose(false, -1, 0, this::battleChosen);
        }
    }

    /** The index chosen in battle mode, or -1 when the player cancelled. */
    public int chosenIndex() {
        return chosenIndex;
    }

    private void battleChosen(int result, boolean switchRequested) {
        if (result < 0 && !battleCanCancel) {
            // Battle_Action_Switching:241-253: the battle has to send one out.
            beginChoose(false, -1, 0, this::battleChosen);
            return;
        }
        chosenIndex = result < 0 ? result : realIndex(result);
        finished = true;
    }

    // =====================================================================
    // PokemonParty_Scene (:644-1031)
    // =====================================================================

    /** :645-701 pbStartScene (multiselect=false，annotations=nil)。 */
    private void startScene(String starthelptext, boolean canAccessStorage) {
        this.canAccessStorage = canAccessStorage;              // :651
        java.util.Arrays.fill(annotations, null);              // :689 (annotations=nil)
        java.util.Arrays.fill(selected, false);
        java.util.Arrays.fill(preselected, false);
        java.util.Arrays.fill(switching, false);
        helpVisible = true;                                    // :675
        prefetchIndex = 0;
        detailsPokemon = party.get(0);                         // :679 PokemonPartyDetailsPanel.new(@viewport, @party[0])
        detailsText = "";                                      // :210
        expBar.pokemon(detailsPokemon);                        // :222
        for (int i = 0; i < 6; i++) {                          // :683-690
            iconCounter[i] = 0;
            iconCurrent[i] = 0;
        }
        pbSetHelpText(starthelptext);                          // :681
        activecmd = 0;                                         // :698
        setSelected(0, true);                                  // :699 (refresh 把 @party[0] 交给详情面板)
        mode = Mode.CHOOSE;
        // :700 pbFadeInAndShow { update } — 登记: 淡入未建模
    }

    /** :790-797 pbSetHelpText */
    private void pbSetHelpText(String text) {
        helpText = text;                                       // :793
        helpWidth = 378;                                       // :795 Changed help window width
        helpVisible = true;                                    // :796
    }

    /** :798-800 */
    private boolean pbHasAnnotations() {
        return annotations[0] != null;
    }

    /** :801-805 */
    private void pbAnnotate(String[] annot) {
        for (int i = 0; i < 6; i++) {
            annotations[i] = annot != null && i < annot.length ? annot[i] : null;
            refreshPanel(i);
        }
    }

    /** :806-812 */
    private void pbSelect(int item) {
        activecmd = item;
        for (int i = 0; i < NUMSPRITES; i++) setSelected(i, i == activecmd);
    }

    /** :813-815 */
    private void pbPreSelect(int item) {
        activecmd = item;
    }

    /** :816-821 pbSwitchBegin */
    private void pbSwitchBegin(int oldid, int newid, Runnable then) {
        playSe("GUI party switch", 100);                       // :817
        pbWait(16, then);                                        // :820
    }

    /** :822-831 pbSwitchEnd */
    private void pbSwitchEnd(int oldid, int newid, Runnable then) {
        playSe("GUI party switch", 100);                       // :823
        refreshPanel(oldid);                                   // :826 oldsprite.pokemon = @party[oldid]
        refreshPanel(newid);                                   // :827
        pbWait(16, () -> {                                       // :828
            pbClearSwitching();                                // :829
            pbRefresh();                                       // :830
            then.run();
        });
    }

    /** :832-837 */
    private void pbClearSwitching() {
        for (int i = 0; i < 6; i++) {
            setPreselected(i, false);
            switching[i] = false;
        }
    }

    /** :838-846 pbSummary */
    private void pbSummary(int pkmnid, Runnable yield, Runnable then) {
        // :839 pbFadeOutAndHide — 登记: 淡出未建模
        Pokemon pokemon = party.get(pkmnid);
        // :840-842 PokemonSummary_Scene / PokemonSummaryScreen#pbStartScreen(@party,pkmnid)
        summary = new SummaryView(context, party.members(), pkmnid, bagHost);
        // BW PScreen_Summary#pbStartScene: pbPlayCry(@pokemon)
        context.audioManager().playCry(pokemon == null || pokemon.species == null ? 0 : pokemon.species.id);
        summaryYield = yield;
        summaryThen = then;
        mode = Mode.SUMMARY;
    }

    private void summaryClosed() {
        summary = null;
        Runnable yield = summaryYield;
        summaryYield = null;
        if (yield != null) yield.run();                        // :843 yield if block_given?
        // :844 pbFadeInAndShow — 登记: 淡入未建模
        pbHardRefresh();                                       // :845
        mode = Mode.CHOOSE;
        Runnable then = summaryThen;
        summaryThen = null;
        if (then != null) then.run();
    }

    /** :847-856 pbChooseItem — 经宿主打开背包选携带品(PauseMenuOverlay.openBagForHold)。 */
    private void pbChooseItem(Pokemon target) {
        // 登记: pbFadeOutIn{ PokemonBag_Scene / pbChooseItemScreen(Proc{pbCanHoldItem?}) } 的返回值
        // 在本工程由宿主的 BagView.chooseHold 直接完成携带，不经 PartyView。
        if (bagHost != null) bagHost.accept(target);
    }

    private java.util.function.BiConsumer<java.util.function.Predicate<String>, java.util.function.Consumer<String>> itemHost;
    private ItemHandlers itemHandlers;

    /** The host opens the bag to pick one item (pbChooseItemScreen) and hands the item back; null when cancelled. */
    public void itemHost(java.util.function.BiConsumer<java.util.function.Predicate<String>, java.util.function.Consumer<String>> host,
                         ItemHandlers handlers) {
        this.itemHost = host;
        this.itemHandlers = handlers;
    }

    /** :857-873 pbUseItem, then :1522-1525 pbUseItemOnPokemon(item, pkmn, self). */
    private void pbUseItem(Pokemon target, int pkmnid, Runnable next) {
        if (itemHost == null || itemHandlers == null) {
            next.run();
            return;
        }
        itemHost.accept(item -> {                              // :861-868 the bag's filter
            if (!itemHandlers.hasUseOnPokemon(item) && !itemHandlers.isMachine(item)) return false;   // pbCanUseOnPokemon?
            if (itemHandlers.isMachine(item)) {
                String machine = itemHandlers.machineMove(item);
                PbsData.Move move = machine == null ? null : context.pbsData().move(machine);
                if (move == null) return false;
                for (Pokemon.MoveSlot known : target.moves) {  // pokemon.hasMove?(move)
                    if (known.move != null && known.move.internalName.equals(move.internalName)) return false;
                }
                return itemHandlers.compatibleWithMove(target, move);
            }
            return true;
        }, picked -> {
            if (picked == null) {                              // :1522 item>0
                pbSetHelpText(party.size() > 1 ? "请选择宝可梦。" : "选择宝可梦或取消。");   // :1520
                next.run();
                return;
            }
            pbSetHelpText(party.size() > 1 ? "请选择宝可梦。" : "选择宝可梦或取消。");
            runItemTask(scene -> itemHandlers.pbUseItemOnPokemon(picked, target, scene), () -> {
                pbRefreshSingle(pkmnid);                       // :1524
                next.run();
            });
        });
    }

    /** Runs a handler on this screen (the screen is already open: no pbStartScene of its own) and then {@code then}. */
    private void runItemTask(java.util.function.Consumer<ItemScene> body, Runnable then) {
        itemTask = ItemTask.start(body);
        Runnable previous = itemDone;
        itemDone = null;
        itemAfter = () -> {
            itemAfter = null;
            itemDone = previous;
            then.run();
        };
        pumpItemTask();
    }

    private Runnable itemAfter;

    /** :874-936 pbChoosePokemon 的开头(:875-880)。 */
    private void beginChoose(boolean sw, int initialsel, int canswitch, ChooseDone done) {
        for (int i = 0; i < 6; i++) {                          // :875-878
            setPreselected(i, sw && i == activecmd);
            switching[i] = sw;
        }
        if (initialsel >= 0) activecmd = initialsel;           // :879
        pbRefresh();                                           // :880
        chooseSwitching = sw;
        chooseCanswitch = canswitch;
        chooseDone = done;
        mode = Mode.CHOOSE;
    }

    /** :881-935 pbChoosePokemon 循环体(每帧一次)。 */
    private void updateChoose(InputManager input) {
        int oldsel = activecmd;                                // :885
        int key = -1;                                          // :886
        GameAction pressed = null;
        if (input.wasRepeated(GameAction.DOWN)) pressed = GameAction.DOWN;     // :887 Input.repeat?
        if (input.wasRepeated(GameAction.RIGHT)) pressed = GameAction.RIGHT;   // :888
        if (input.wasRepeated(GameAction.LEFT)) pressed = GameAction.LEFT;     // :889
        if (input.wasRepeated(GameAction.UP)) pressed = GameAction.UP;         // :890
        if (pressed != null) {                                 // :891
            activecmd = pbChangeSelection(pressed, activecmd); // :892
        }
        if (activecmd != oldsel) {                             // :894
            playCursorSe();                                    // :895
            for (int i = 0; i < NUMSPRITES; i++) setSelected(i, i == activecmd);   // :896-899
        }
        boolean cancelKey = input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU); // Input::B
        boolean specialKey = input.wasPressed(GameAction.SPECIAL);                                     // Input::A
        if (specialKey && chooseCanswitch == 1 && activecmd != CANCEL) {       // :902
            playDecisionSe();                                  // :903
            finishChoose(activecmd, true);                     // :904
        } else if (specialKey && chooseCanswitch == 2) {       // :905
            finishChoose(-1, false);                           // :906
        } else if (cancelKey) {                                // :907
            if (!chooseSwitching) playCloseMenuSe();           // :908
            finishChoose(-1, false);                           // :909
        } else if (input.wasPressed(GameAction.CONFIRM)) {     // :910
            if (activecmd == CANCEL) {                         // :911
                if (chooseSwitching) playDecisionSe(); else playCloseMenuSe();     // :912
                finishChoose(-1, false);                       // :913
            } else {
                playDecisionSe();                              // :915
                finishChoose(activecmd, false);                // :916
            }
        }
        else if (input.wasPressed(GameAction.F5) && trainer.pokepc && canAccessStorage && chooseCanswitch != 2) {   // :918-919
            int[] maps = {95, 96, 97, 98, 117, 118, 119, 153, 299, 357, 416, 465, 467};   // :920
            boolean blocked = false;
            for (int map : maps) {
                if (map == context.gameState().currentMapId()) blocked = true;             // :921
            }
            if (blocked) {
                display("暂时无法使用。", () -> mode = Mode.CHOOSE);                       // :922 pbMessage / :923 next
            } else if (storageHost != null) {
                // :925-930 pbFadeOutIn { PokemonStorageScene / PokemonStorageScreen#pbStartScreen(0); pbHardRefresh }
                pendingHardRefresh = true;                                                 // :929 在回到本界面后的第一帧执行
                storageHost.run();
            }
            // :931-933 $PokemonGlobal.followerToggled -> dependentEvents.come_back — 登记: 跟随者未建模，空实现。
        }
    }

    private void finishChoose(int result, boolean switchRequested) {
        ChooseDone done = chooseDone;
        chooseDone = null;
        if (done != null) done.done(result, switchRequested);
    }

    /** :937-982 pbChangeSelection */
    private int pbChangeSelection(GameAction key, int currentsel) {
        int numsprites = NUMSPRITES;                           // :938
        int length = party.size();
        switch (key) {
            case LEFT:                                         // :940
                do {
                    currentsel -= 1;                           // :942
                } while (currentsel > 0 && currentsel < length && party.get(currentsel) == null);   // :943
                if (currentsel >= length && currentsel < 6) {  // :944
                    currentsel = length - 1;                   // :945
                }
                if (currentsel < 0) currentsel = numsprites - 1;   // :947
                break;
            case RIGHT:                                        // :948
                do {
                    currentsel += 1;                           // :950
                } while (currentsel < length && party.get(currentsel) == null);   // :951
                if (currentsel == length) {                    // :952
                    currentsel = 6;                            // :953
                } else if (currentsel == numsprites) {         // :954
                    currentsel = 0;                            // :955
                }
                break;
            case UP:                                           // :957
                if (currentsel == 0) {                         // :958
                    currentsel = 6;                            // :959
                } else {
                    do {
                        currentsel = 0;                        // :962
                    } while (currentsel > 0 && party.get(currentsel) == null);   // :963
                }
                if (currentsel >= length && currentsel < 6) {  // :965
                    currentsel = length - 1;                   // :966
                }
                if (currentsel < 0) currentsel = numsprites - 1;   // :968
                break;
            case DOWN:                                         // :969
                if (currentsel == 6) {                         // :970
                    currentsel = 0;                            // :971
                } else {
                    currentsel = 6;                            // :973
                }
                if (currentsel >= length && currentsel < 6) {  // :975
                    currentsel = 6;                            // :976
                } else if (currentsel >= numsprites) {         // :977
                    currentsel = 0;                            // :978
                }
                break;
            default:
                break;
        }
        return currentsel;                                     // :981
    }

    /** :983-1005 pbHardRefresh */
    private void pbHardRefresh() {
        int lastselected = -1;                                 // :985
        for (int i = 0; i < 6; i++) {                          // :986-990
            if (selected[i]) lastselected = i;
        }
        if (lastselected >= party.size()) lastselected = party.size() - 1;   // :992
        if (lastselected < 0) lastselected = 0;                // :993
        java.util.Arrays.fill(selected, false);                // :989-995 面板重建，选中态清零
        java.util.Arrays.fill(preselected, false);
        detailsPokemon = party.get(0);                         // :994 PokemonPartyDetailsPanel.new(@viewport, @party[0])
        detailsText = "";                                      // :210
        expBar.pokemon(detailsPokemon);
        pbSelect(lastselected);                                // :1004
    }

    /** :1006-1017 pbRefresh */
    private void pbRefresh() {
        for (int i = 0; i < 6; i++) refreshPanel(i);
    }

    /** :1018-1027 pbRefreshSingle */
    private void pbRefreshSingle(int i) {
        refreshPanel(i);
    }

    private void setSelected(int i, boolean value) {            // :511-516 selected=
        if (selected[i] != value) {
            selected[i] = value;
            refreshPanel(i);
        }
    }

    private void setPreselected(int i, boolean value) {         // :517-522 preselected=
        if (preselected[i] != value) {
            preselected[i] = value;
            refreshPanel(i);
        }
    }

    /**
     * :540-612 SelectionPanel#refresh 里“选中时把宝可梦交给详情面板”的那一段(:599-605)；
     * 其余的绘制在 {@link #render}。
     */
    private void refreshPanel(int i) {
        if (i < 0 || i >= 6) return;
        evolvable[i] = null;
        Pokemon pokemon = party.get(i);
        if (pokemon != null && selected[i]) {                  // :599
            if (annotations[i] != null) detailsText = annotations[i];   // :603 @details.text = @text (text= 只接受 String，nil 被忽略 :227)
            detailsPokemon = pokemon;                          // :604 @details.pokemon = @pokemon
            expBar.pokemon(pokemon);                           // :260
        }
    }

    // =====================================================================
    // PokemonPartyScreen (:1035-1589)
    // =====================================================================

    /** :1301-1304 pbPokemonScreen 循环头。 */
    private void startTop() {
        pbSetHelpText(party.size() > 1 ? "请选择宝可梦。" : "选择宝可梦或取消。");   // :1302
        beginChoose(false, -1, 1, this::topChosen);            // :1303 pbChoosePokemon(false,-1,1)
    }

    private void topChosen(int pkmnid, boolean switchRequested) {
        if (pkmnid < 0) {                                      // :1304 break
            endScene();                                        // :1586 @scene.pbEndScene
            return;
        }
        if (switchRequested) {                                 // :1305 Switch
            pbSetHelpText("移动到哪里？");                     // :1306
            int oldpkmnid = pkmnid;                            // :1307
            beginChoose(true, -1, 2, (target, ignored) -> {    // :1308
                if (target >= 0 && target != oldpkmnid) {      // :1309
                    pbSwitch(oldpkmnid, target, this::startTop);   // :1310
                } else {
                    startTop();                                // :1312 next
                }
            });
            return;
        }
        openCommands(pkmnid);
    }

    private void endScene() {                                   // :702-706 pbEndScene
        // :703 pbFadeOutAndHide — 登记: 淡出未建模
        finished = true;
    }

    /** :1122-1130 pbSwitch */
    private void pbSwitch(int oldid, int newid, Runnable then) {
        if (oldid != newid) {                                  // :1123
            pbSwitchBegin(oldid, newid, () -> {                // :1124
                party.swap(oldid, newid);                      // :1125-1127
                pbSwitchEnd(oldid, newid, then);               // :1128
            });
        } else {
            then.run();
        }
    }

    // ---- :1314-1588 命令菜单

    private boolean hasItem(Pokemon p) {                        // pkmn.hasItem?
        return p.item != null && !p.item.isEmpty();
    }

    /** 登记: 邮件未建模，{@code pkmn.mail} 恒为 nil。 */
    private boolean hasMail(Pokemon p) {
        return false;
    }

    /** 登记: 邮件未建模，{@code pbIsMail?(item)} 恒为 false。 */
    private boolean isMail(String item) {
        return false;
    }

    private String itemName(String id) {
        PbsData.Item item = context.pbsData() == null ? null : context.pbsData().item(id);
        return item == null ? (id == null ? "" : id) : item.name;
    }

    private void openCommands(int pkmnid) {
        final Pokemon pkmn = party.get(pkmnid);                // :1314
        final boolean ret = canEvolveNow(pkmn);                // :1315-1318 ret>0
        final List<String> commands = new ArrayList<>();       // :1319
        final List<Integer> colorKeys = new ArrayList<>();
        int cmdSummary, cmdPokedex = -1, cmdNickname = -1, cmdEvolution = -1, cmdRelearn = -1,
                cmdDebug = -1, cmdSwitch = -1, cmdMail = -1, cmdItem = -1;          // :1320-1329
        final int[] cmdMoves = {-1, -1, -1, -1};               // :1326
        // Build the commands
        cmdSummary = add(commands, colorKeys, "查看能力", 0);                        // :1331
        if (trainer.pokedex && !pkmn.egg) cmdPokedex = add(commands, colorKeys, "查看图鉴", 0);      // :1332
        if (party.size() > 1) cmdSwitch = add(commands, colorKeys, "移动位置", 0);                   // :1333
        if (!pkmn.egg && trainer.id == pkmn.trainerID) cmdNickname = add(commands, colorKeys, "昵称", 0);  // :1334
        if (ret) cmdEvolution = add(commands, colorKeys, "进化", 0);                 // :1335
        if (!pkmn.egg) cmdRelearn = add(commands, colorKeys, "招式", 0);             // :1336
        for (int i = 0; i < pkmn.moves.size; i++) {            // :1337
            Pokemon.MoveSlot move = pkmn.moves.get(i);         // :1338
            // Check for hidden moves and add any that were found
            if (!pkmn.egg && move.move != null && (isMove(move, "MILKDRINK") || isMove(move, "SOFTBOILED")
                    || hasHiddenMoveHandler(move))) {          // :1340-1342
                if (i < 4) cmdMoves[i] = add(commands, colorKeys, move.move.name, 1);   // :1343
            }
        }
        if (!pkmn.egg) {                                       // :1346
            if (hasMail(pkmn)) {
                cmdMail = add(commands, colorKeys, "邮件", 0);                       // :1348
            } else {
                cmdItem = add(commands, colorKeys, "道具", 0);                       // :1350
            }
        }
        // :1353 调试 — 登记: $DEBUG 未建模，恒为 false，不加入命令表
        add(commands, colorKeys, "退出", 0);                                          // :1354
        final int fSummary = cmdSummary, fPokedex = cmdPokedex, fNickname = cmdNickname,
                fEvolution = cmdEvolution, fRelearn = cmdRelearn, fSwitch = cmdSwitch,
                fMail = cmdMail, fItem = cmdItem;
        showCommands(intl("要对{1}做什么？", pkmn.name), commands, colorKeys, 0, command -> {   // :1355
            for (int i = 0; i < 4; i++) {                      // :1357
                if (cmdMoves[i] >= 0 && command == cmdMoves[i]) {   // :1358
                    hiddenMove(pkmn, pkmnid, i);               // :1359-1412
                    return;                                    // :1414 next if havecommand
                }
            }
            Runnable next = this::startTop;
            if (fSummary >= 0 && command == fSummary) {        // :1415
                pbSummary(pkmnid, () -> pbSetHelpText(party.size() > 1 ? "请选择宝可梦。" : "选择宝可梦或取消。"), next);   // :1416-1418
            } else if (fPokedex >= 0 && command == fPokedex) { // :1420 查看图鉴
                pokedexInfo(pkmn, next);                       // :1421-1427
            } else if (fSwitch >= 0 && command == fSwitch) {   // :1430
                pbSetHelpText("移动到哪里？");                 // :1431
                final int oldpkmnid = pkmnid;                  // :1432
                beginChoose(true, -1, 0, (target, ignored) -> {   // :1433
                    if (target >= 0 && target != oldpkmnid) {  // :1434
                        pbSwitch(oldpkmnid, target, next);     // :1435
                    } else {
                        next.run();
                    }
                });
            } else if (fNickname >= 0 && command == fNickname) {   // :1437
                nickname(pkmn, pkmnid, next);                  // :1438-1448
            } else if (fEvolution >= 0 && command == fEvolution) { // :1449
                evolution(pkmn, next);                         // :1450-1460
            } else if (fRelearn >= 0 && command == fRelearn) { // :1461
                relearnMenu(pkmn, pkmnid, 0, next);            // :1462-1490
            } else if (fMail >= 0 && command == fMail) {       // :1491
                mailMenu(pkmn, pkmnid);                        // :1492-1504
                next.run();
            } else if (fItem >= 0 && command == fItem) {       // :1505
                itemMenu(pkmn, pkmnid, next);                  // :1506-1584
            } else {
                next.run();                                    // 退出 / B: 回到循环头
            }
        });
    }

    private static int add(List<String> commands, List<Integer> colorKeys, String label, int colorKey) {
        commands.add(label);                                   // commands[cmdX = commands.length] = ...
        if (colorKeys != null) colorKeys.add(colorKey);
        return commands.size() - 1;
    }

    private boolean isMove(Pokemon.MoveSlot slot, String internalName) {   // isConst?(move.id,PBMoves,:X)
        return slot.move != null && internalName.equals(slot.move.internalName);
    }

    /** {@code HiddenMoveHandlers.hasHandler(move.id)} (179_PField_FieldMoves:21-23). */
    private boolean hasHiddenMoveHandler(Pokemon.MoveSlot slot) {
        return slot.move != null && pokemon.runtime.field.HiddenMoves.hasHandler(slot.move.internalName);
    }

    /** The hidden move the player chose ({@code return [pkmn, move]}, :1401/:1407), for the pause menu to run once it closed. */
    private Pokemon hiddenMovePokemon;
    private String hiddenMoveId;
    /** {@code PokemonRegionMapScreen#pbStartFlyScreen} (:1396-1398): the host picks the map and hands [map, x, y] back, or null. */
    private java.util.function.Consumer<java.util.function.Consumer<int[]>> flyHost;

    public void flyHost(java.util.function.Consumer<java.util.function.Consumer<int[]>> host) {
        this.flyHost = host;
    }

    public Pokemon hiddenMovePokemon() {
        return hiddenMovePokemon;
    }

    public String hiddenMoveId() {
        return hiddenMoveId;
    }

    public void clearHiddenMove() {
        hiddenMovePokemon = null;
        hiddenMoveId = null;
    }

    /** :1357-1413 隐藏招式(乳饮/鸡蛋炸弹式的“治疗”招式、其余走 HiddenMoveHandlers)。 */
    private void hiddenMove(Pokemon pkmn, int pkmnid, int i) {
        Pokemon.MoveSlot slot = pkmn.moves.get(i);
        if (isMove(slot, "SOFTBOILED") || isMove(slot, "MILKDRINK")) {      // :1360-1361
            final int amt = Math.max(pkmn.maxHp() / 5, 1);     // :1362
            if (pkmn.hp <= amt) {                              // :1363
                display("没有足够的HP...", this::startTop);    // :1364-1365 break
                return;
            }
            pbSetHelpText("要对哪只宝可梦使用？");             // :1367
            softboiledLoop(pkmn, pkmnid, pkmnid, amt, slot);   // :1368-1391
        } else {
            // :1392-1411 pbCanUseHiddenMove? / pbConfirmUseHiddenMove / the fly map
            final String move = slot.move.internalName;
            pokemon.runtime.state.GameState gameState = context.gameState();
            pokemon.runtime.field.HiddenMoves.World world = new pokemon.runtime.field.MapPortWorld(
                    context.mapPort(), gameState, context.database());
            pokemon.runtime.field.HiddenMoves.Check check = pokemon.runtime.field.HiddenMoves.canUse(
                    move, pkmn, gameState, context.pbsData(), world);
            if (!check.ok) {
                if (check.message != null) {
                    display(check.message, this::startTop);        // showmsg: the line, then the party list again (:1409-1410 break)
                } else {
                    startTop();
                }
                return;
            }
            String question = pokemon.runtime.field.HiddenMoves.confirmQuestion(move, gameState, context.pbsData(), world);
            Runnable proceed = () -> useHiddenMove(pkmn, move);
            if (question == null) {
                proceed.run();                                     // no ConfirmUseMove handler: true
            } else if (question.isEmpty()) {
                startTop();                                        // the handler answers false without asking
            } else {
                confirm(question, yes -> {
                    if (yes) proceed.run(); else startTop();
                });
            }
        }
    }

    /** :1394-1407 the party screen ends and the move is used from the map. */
    private void useHiddenMove(Pokemon pkmn, String move) {
        if ("FLY".equals(move)) {                                  // :1395-1406 the fly map first
            if (flyHost == null) {
                startTop();                                        // 登记: no host opens the fly map
                return;
            }
            flyHost.accept(destination -> {
                if (destination == null) {
                    startTop();                                    // :1403-1405 back to the party screen
                    return;
                }
                context.gameState().flyData(destination);          // :1400 $PokemonTemp.flydata = ret
                hiddenMovePokemon = pkmn;
                hiddenMoveId = move;
                finished = true;
            });
            return;
        }
        hiddenMovePokemon = pkmn;                                  // :1407
        hiddenMoveId = move;
        finished = true;
    }

    private void softboiledLoop(Pokemon pkmn, int oldpkmnid, int current, int amt, Pokemon.MoveSlot slot) {
        pbPreSelect(oldpkmnid);                                // :1370
        beginChoose(true, current, 0, (picked, ignored) -> {   // :1371
            if (picked < 0) {                                  // :1372
                softboiledEnd(oldpkmnid);
                return;
            }
            final Pokemon newpkmn = party.get(picked);         // :1373
            final String movename = slot.move.name;            // :1374
            Runnable cont = () -> {
                if (pkmn.hp <= amt) {                          // :1387
                    softboiledEnd(oldpkmnid);
                } else {
                    softboiledLoop(pkmn, oldpkmnid, picked, amt, slot);
                }
            };
            if (picked == oldpkmnid) {                         // :1375
                display(intl("{1}不能对自己使用{2}！", pkmn.name, movename), cont);   // :1376
            } else if (newpkmn.egg) {                          // :1377
                display(intl("{1}不能对宝可梦蛋使用！", movename), cont);             // :1378
            } else if (newpkmn.hp == 0 || newpkmn.hp == newpkmn.maxHp()) {            // :1379
                display(intl("{1}不能对这只宝可梦使用！", movename), cont);           // :1380
            } else {
                pkmn.hp -= amt;                                // :1382
                int hpgain = itemRestoreHp(newpkmn, amt);      // :1383
                display(intl("{1}的HP恢复了{2}点。", newpkmn.name, hpgain), () -> {   // :1384
                    pbRefresh();                               // :1385
                    cont.run();
                });
            }
        });
    }

    private void softboiledEnd(int oldpkmnid) {
        pbSelect(oldpkmnid);                                   // :1389
        pbRefresh();                                           // :1390
        startTop();
    }

    /** 他段: PItem_Items:567-573 pbItemRestoreHP */
    private int itemRestoreHp(Pokemon pkmn, int restoreHp) {
        int newHp = pkmn.hp + restoreHp;
        if (newHp > pkmn.maxHp()) newHp = pkmn.maxHp();
        int hpGain = newHp - pkmn.hp;
        pkmn.hp = newHp;
        return hpGain;
    }

    private DexEntryView dexView;
    private Runnable dexDone;

    /**
     * :1420-1427 查看图鉴: {@code PokemonPokedexInfoScreen#pbStartSceneSingle(pkmn.species)} - the species' own entry page
     * (the BW Pokedex entry of 330_PokedexEntry_BW_Style) without the list around it, then {@code dorefresh = true}.
     * 登记: {@code pbUpdateLastSeenForm(pkmn)} and the {@code pbFadeOutIn} fade are not modelled.
     */
    private void pokedexInfo(Pokemon pkmn, Runnable next) {
        if (pkmn.species == null) {
            next.run();
            return;
        }
        dexView = new DexEntryView(context, java.util.Collections.singletonList(pkmn.species), 0);
        context.audioManager().playCry(pkmn.species.id);                     // pbPlayCrySpecies on open
        dexDone = () -> {
            pbHardRefresh();                                                 // :1427 dorefresh = true
            next.run();
        };
    }

    private TextEntryView nameEntry;
    private Consumer<String> nameDone;

    /** :1438-1448 昵称 — 登记: the name screen shows no Pokemon icon (pbEnterPokemonName's pokemon argument). */
    private void nickname(Pokemon pkmn, int pkmnid, Runnable next) {
        String speciesname = pkmn.species == null ? "" : pkmn.species.name;                  // :1439
        String oldname = pkmn.name != null && !pkmn.name.equals(speciesname) ? pkmn.name : "";   // :1440
        nameEntry = new TextEntryView(context, intl("{1}的昵称是？", speciesname), 0, 10, oldname, null);   // :1441-1442
        nameDone = newname -> {
            if (!newname.isEmpty()) {                                                        // :1443
                pkmn.name = newname;                                                         // :1444
            } else {
                pkmn.name = speciesname;                                                     // :1447
            }
            pbRefreshSingle(pkmnid);                                                         // :1445 / :1448
            next.run();
        };
    }

    /** :1450-1460 进化: the evolution scene (cancellable), the saved BGM comes back when it closes. */
    private void evolution(Pokemon pkmn, Runnable next) {
        PbsData.Species target = evolutionTarget(pkmn);                  // :1315 ret
        if (target == null) {
            next.run();
            return;
        }
        startEvolution(pkmn, target, true, next);
    }

    private void startEvolution(Pokemon pkmn, PbsData.Species target, boolean canCancel, Runnable then) {
        evolutionView = new EvolutionView(context, pkmn, target, canCancel);   // pbStartScreen(pkmn,ret)
        evolutionDone = then;
    }

    private void evolutionClosed() {                                   // pbEndScreen, then the scene refreshes
        evolutionView = null;
        Runnable then = evolutionDone;
        evolutionDone = null;
        pbHardRefresh();
        mode = Mode.CHOOSE;
        if (then != null) then.run();
    }

    /** :1462-1490 招式：回忆/忘记 */
    private void relearnMenu(Pokemon pkmn, int pkmnid, int command, Runnable next) {
        List<String> commands = new ArrayList<>();
        commands.add("回忆");
        commands.add("忘记");
        commands.add("取消");
        showCommands(intl("要对{1}的招式做什么？", pkmn.name), commands, null, command, picked -> {   // :1464-1465
            switch (picked) {
                case 0:                                        // :1467
                    if (pokemon.runtime.pokemon.MoveRelearner.hasRelearnableMove(pkmn, context.pbsData())) {   // :1468
                        relearnView = new RelearnerView(context, pkmn);        // :1469 pbRelearnMoveScreen(pkmn)
                        relearnDone = () -> {
                            pbHardRefresh();                                   // :1470
                            pbRefreshSingle(pkmnid);                           // :1471
                            relearnMenu(pkmn, pkmnid, picked, next);
                        };
                    } else {
                        display(intl("{1}没有可以回忆的招式。", pkmn.name),        // :1473
                                () -> relearnMenu(pkmn, pkmnid, picked, next));
                    }
                    break;
                case 1:                                        // :1475
                    if (pkmn.moves.size > 1 && pkmn.moves.get(1).move != null) {   // :1476
                        chooseMove(pkmn, "选择需要忘记的招式。", 0, moveindex -> {    // :1477
                            if (moveindex >= 0) {              // :1478
                                String movename = pkmn.moves.get(moveindex).move.name;   // :1479
                                pkmn.moves.removeIndex(moveindex);                       // :1480 pkmn.pbDeleteMoveAtIndex
                                display(intl("{1}忘记了{2}。", pkmn.name, movename), () -> {   // :1481
                                    pbHardRefresh();           // :1482
                                    relearnMenu(pkmn, pkmnid, picked, next);
                                });
                            } else {
                                relearnMenu(pkmn, pkmnid, picked, next);
                            }
                        });
                    } else {
                        display(intl("{1}不想再忘记招式了！", pkmn.name),      // :1485
                                () -> relearnMenu(pkmn, pkmnid, picked, next));
                    }
                    break;
                default:                                       // :1487
                    next.run();                                // :1488 break
                    break;
            }
        });
    }

    /** :1131-1142 pbChooseMove */
    private void chooseMove(Pokemon pokemon, String helptext, int index, IntDone done) {
        List<String> movenames = new ArrayList<>();            // :1132
        for (int i = 0; i < pokemon.moves.size; i++) {         // :1133
            Pokemon.MoveSlot slot = pokemon.moves.get(i);
            if (slot.move == null) break;                      // :1134
            if (slot.maxPp <= 0) {                             // :1135 i.totalpp<=0
                movenames.add(intl("{1} (PP：---)", slot.move.name));                       // :1136
            } else {
                movenames.add(intl("{1} (PP：{2}/{3})", slot.move.name, slot.pp, slot.maxPp));   // :1138
            }
        }
        showCommands(helptext, movenames, null, index, done);  // :1141
    }

    /** :1492-1504 邮件 — 登记: 邮件未建模(hasMail 恒 false，此分支不可达)，空实现。 */
    private void mailMenu(Pokemon pkmn, int pkmnid) {
        // 阅读: pbFadeOutIn{ pbDisplayMail(pkmn.mail,pkmn); pbSetHelpText(...) }
        // Take: pbTakeItemFromPokemon(pkmn,self) -> pbRefreshSingle(pkmnid)
        // 菜单文案原文就是 [_INTL("阅读"),_INTL("Take"),_INTL("Cancel")]（:1493，插件缺陷，原样登记）
    }

    /** :1506-1584 道具 */
    private void itemMenu(Pokemon pkmn, int pkmnid, Runnable next) {
        List<String> itemcommands = new ArrayList<>();         // :1506
        int cmdUseItem, cmdGiveItem, cmdTakeItem = -1, cmdMoveItem = -1;   // :1507-1510
        // Build the commands
        cmdUseItem = add(itemcommands, null, "使用", 0);       // :1512
        cmdGiveItem = add(itemcommands, null, "携带", 0);      // :1513
        if (hasItem(pkmn)) cmdTakeItem = add(itemcommands, null, "收回", 0);                  // :1514
        if (hasItem(pkmn) && !isMail(pkmn.item)) cmdMoveItem = add(itemcommands, null, "移动", 0);   // :1515
        add(itemcommands, null, "退出", 0);                    // :1516
        final int fUse = cmdUseItem, fGive = cmdGiveItem, fTake = cmdTakeItem, fMove = cmdMoveItem;
        showCommands("用道具做什么？", itemcommands, null, 0, command -> {   // :1517
            if (fUse >= 0 && command == fUse) {                // :1518 Use
                pbUseItem(pkmn, pkmnid, next);                 // :1519-1525
            } else if (fGive >= 0 && command == fGive) {       // :1526 Give
                pbChooseItem(pkmn);                            // :1527-1534
                pbRefreshSingle(pkmnid);
                next.run();
            } else if (fTake >= 0 && command == fTake) {       // :1535 Take
                takeItemFromPokemon(pkmn, () -> {              // :1536
                    pbRefreshSingle(pkmnid);                   // :1537
                    next.run();
                });
            } else if (fMove >= 0 && command == fMove) {       // :1539 Move
                final String item = pkmn.item;                 // :1540
                final String itemname = itemName(item);        // :1541
                pbSetHelpText(intl("要将{1}移动到哪里？", itemname));   // :1542
                itemMoveLoop(pkmn, pkmnid, pkmnid, item, itemname, next);
            } else {
                next.run();
            }
        });
    }

    /** :1544-1582 */
    private void itemMoveLoop(Pokemon pkmn, int oldpkmnid, int current, String item, String itemname, Runnable next) {
        pbPreSelect(oldpkmnid);                                // :1545
        beginChoose(true, current, 0, (picked, ignored) -> {   // :1546
            if (picked < 0) {                                  // :1547
                next.run();
                return;
            }
            final Pokemon newpkmn = party.get(picked);         // :1548
            Runnable again = () -> itemMoveLoop(pkmn, oldpkmnid, picked, item, itemname, next);
            if (picked == oldpkmnid) {                         // :1549
                next.run();                                    // :1550 break
            } else if (newpkmn.egg) {                          // :1551
                display("宝可梦蛋不能携带物品。", again);      // :1552
            } else if (!hasItem(newpkmn)) {                    // :1553
                newpkmn.item = item;                           // :1554
                pkmn.item = null;                              // :1555
                pbClearSwitching();                            // :1556
                pbRefresh();                                   // :1557
                display(intl("{1}已交给{2}携带。", newpkmn.name, itemname), next);   // :1558-1559
            } else if (isMail(newpkmn.item)) {                 // :1560
                display(intl("必须先拿回{1}身上的邮件，\n然后才能给予道具。", newpkmn.name), again);   // :1561
            } else {
                final String newitem = newpkmn.item;           // :1563
                final String newitemname = itemName(newitem);  // :1564
                // :1565-1571 原文这三句是英文(插件缺陷，照抄)；LEFTOVERS/元音判断照抄。
                String holding;
                if ("LEFTOVERS".equals(newitem)) {             // :1565
                    holding = intl("{1} is already holding some {2}.\u0001", newpkmn.name, newitemname);   // :1566
                } else if (startsWithVowel(newitemname)) {     // :1567
                    holding = intl("{1} is already holding an {2}.\u0001", newpkmn.name, newitemname);     // :1568
                } else {
                    holding = intl("{1} is already holding a {2}.\u0001", newpkmn.name, newitemname);      // :1570
                }
                display(holding, () -> confirm("想要交换这两个道具吗？", yes -> {   // :1572
                    if (yes) {
                        newpkmn.item = item;                   // :1573
                        pkmn.item = newitem;                   // :1574
                        pbClearSwitching();                    // :1575
                        pbRefresh();                           // :1576
                        display(intl("{1}已交给{2}携带。", newpkmn.name, itemname),               // :1577
                                () -> display(intl("{1}已交给{2}携带。", pkmn.name, newitemname), next));   // :1578
                    } else {
                        again.run();
                    }
                }));
            }
        });
    }

    /** 他段: String#starts_with_vowel? */
    private static boolean startsWithVowel(String text) {
        return text != null && !text.isEmpty() && "aeiouAEIOU".indexOf(text.charAt(0)) >= 0;
    }

    /** 他段: PItem_Items:1054-1085 pbTakeItemFromPokemon（邮件分支不可达，背包容量未建模）。 */
    private void takeItemFromPokemon(Pokemon pkmn, Runnable then) {
        if (!hasItem(pkmn)) {                                  // :1056
            display(intl("{1}现在什么也没携带。", pkmn.name), then);                 // :1057
        } else {
            // :1058 $PokemonBag.pbCanStore? — 登记: 背包容量未建模，恒可存放；:1060-1076 邮件分支不可达。
            String itemname = itemName(pkmn.item);             // :1079
            context.gameState().inventory().add(pkmn.item, 1); // :1078 pbStoreItem
            pkmn.item = null;                                  // :1081 pkmn.setItem(0)
            display(intl("从{2}那接收了{1}。", itemname, pkmn.name), then);          // :1080
        }
    }

    // ---- :1049-1078 / :1143-1296: 未接到任何调用方的 PokemonPartyScreen 接口。登记: 空实现，保留入口。

    /** :1049-1059 pbPokemonGiveScreen — 登记: 空实现(道具“给哪个宝可梦？”流程由 BagView 自带)。 */
    public boolean pbPokemonGiveScreen(String item) {
        return false;
    }

    /** :1060-1078 pbPokemonGiveMailScreen — 登记: 邮件未建模，空实现。 */
    public void pbPokemonGiveMailScreen(int mailIndex) {
    }

    /** :1143-1151 pbRefreshAnnotations — 登记: 空实现(无调用方)。 */
    public void pbRefreshAnnotations() {
    }

    /** :1152-1154 pbClearAnnotations */
    public void pbClearAnnotations() {
        pbAnnotate(null);                                      // :1153
    }

    /** :1155-1243 pbPokemonMultipleEntryScreenEx — 登记: 对战规则选队(ruleset)未建模，空实现。 */
    public int[] pbPokemonMultipleEntryScreenEx() {
        return null;
    }

    /** :1244-1269 pbChooseAblePokemon — 登记: 空实现(无调用方)。 */
    public int pbChooseAblePokemon() {
        return -1;
    }

    /** :1270-1296 pbChooseTradablePokemon — 登记: 空实现(无调用方)。 */
    public int pbChooseTradablePokemon() {
        return -1;
    }

    // =====================================================================
    // 道具处理器的屏幕: ItemHandlers 的阻塞 Ruby 在 ItemTask 线程里跑，每个 scene 调用在这里用状态机应答
    // =====================================================================

    private ItemTask itemTask;
    private Runnable itemDone;
    private com.badlogic.gdx.graphics.Texture pixel;

    // Window_InputNumberPokemon (065_SpriteWindow_text:612-): pbMessageChooseNumber
    private int numberDigits, numberValue, numberIndex, numberMax, numberMin, numberCancel, numberFrame;
    private IntDone numberDone;
    // pbTopRightWindow (188_PItem_Items:547-562)
    private String topRightText = "";
    private Runnable topRightDone;

    /**
     * The party screen the item handlers run on (pbUseItem / pbUseItemOnPokemon / pbMoveTutorChoose): the screen is
     * started by the handler's own {@code pbStartScene} and closes when the handler returns.
     *
     * @param done runs when the screen has closed (the host returns to the bag)
     */
    public static PartyView forItem(RuntimeContext context, java.util.function.Consumer<ItemScene> body, Runnable done) {
        return forItem(context, body, done, null);
    }

    /** {@link #forItem(RuntimeContext, java.util.function.Consumer, Runnable)} for a battle: the party in its display order. */
    public static PartyView forItem(RuntimeContext context, java.util.function.Consumer<ItemScene> body, Runnable done,
                                    int[] displayOrder) {
        PartyView view = new PartyView(context, false);
        if (displayOrder != null) {
            view.displayOrder(displayOrder);
        }
        view.itemTask = ItemTask.start(body);
        view.itemDone = done;
        view.pumpItemTask();
        return view;
    }

    /** Runs the handler up to its next screen call and starts that call. */
    private void pumpItemTask() {
        while (itemTask != null) {
            if (itemTask.resume()) {                           // the handler returned
                itemTask = null;
                if (itemAfter != null) {                       // started from this screen's own item menu
                    itemAfter.run();
                } else {
                    endScene();                                // pbEndScene
                }
                return;
            }
            TaskItemScene.Request r = itemTask.pending();
            switch (r.kind) {
                case START_SCENE:
                    pbSetHelpText(r.text);                     // :681 / :884
                    pbAnnotate(r.annotations);
                    itemTask.answer(null);
                    break;
                case HELP_TEXT:
                    pbSetHelpText(r.text);
                    itemTask.answer(null);
                    break;
                case ANNOTATIONS: {                            // :1143-1151 pbRefreshAnnotations
                    if (pbHasAnnotations()) {
                        String[] annot = new String[party.size()];
                        for (int i = 0; i < annot.length; i++) {
                            annot[i] = r.able.test(party.get(i)) ? "可以使用" : "无效";
                        }
                        pbAnnotate(annot);
                    }
                    itemTask.answer(null);
                    break;
                }
                case CLEAR_ANNOTATIONS:
                    pbClearAnnotations();
                    itemTask.answer(null);
                    break;
                case REFRESH:
                    pbRefresh();
                    itemTask.answer(null);
                    break;
                case HARD_REFRESH:
                    pbHardRefresh();
                    itemTask.answer(null);
                    break;
                case SE:
                    if (!r.text.isEmpty()) playSe(r.text, 100);
                    itemTask.answer(null);
                    break;
                case EVOLUTION:                                // 189:408-415 PokemonEvolutionScene.pbEvolution(false)
                    startEvolution(r.pokemon, r.species, false, () -> answerItem(null));
                    return;
                case DISPLAY:
                case MESSAGE:
                    display(itemText(r.text), () -> answerItem(null));
                    return;
                case CONFIRM:
                    confirm(itemText(r.text), yes -> answerItem(yes));
                    return;
                case CHOOSE_POKEMON:
                    if (r.text != null && !r.text.isEmpty()) pbSetHelpText(r.text);
                    beginChoose(false, -1, 0, (picked, switchRequested) -> answerItem(picked));
                    return;
                case CHOOSE_MOVE: {                            // :1131-1142 pbChooseMove
                    List<String> names = new ArrayList<>();
                    for (Pokemon.MoveSlot slot : r.pokemon.moves) {
                        String name = slot.move == null ? "" : slot.move.name;
                        if (slot.totalPp() <= 0) names.add(intl("{1} (PP：---)", name));
                        else names.add(intl("{1} (PP：{2}/{3})", name, slot.pp, slot.totalPp()));
                    }
                    showCommands(r.text, names, null, 0, index -> answerItem(index));
                    return;
                }
                case SHOW_COMMANDS:
                    showCommands(r.text, r.commands, null, r.number, index -> answerItem(index));
                    return;
                case MESSAGE_COMMANDS:
                    messageCommands(r);
                    return;
                case CHOOSE_NUMBER:
                    startNumber(itemText(r.text), r.number, r.defaultValue, r.cancelValue);
                    return;
                case TOP_RIGHT:
                    startTopRight(r.text);
                    return;
                case FORGET_MOVE:                              // 303_BW_PScreen_Summary:1566-1580 pbStartForgetScreen
                    forgetSummary = SummaryView.forForget(context, r.pokemon, r.move);
                    return;
                default:
                    itemTask.answer(null);
                    break;
            }
        }
    }

    private void answerItem(Object value) {
        if (itemTask != null) {
            itemTask.answer(value);
            pumpItemTask();
        }
    }

    /** pbMessage(text, commands, cmdIfCancel) (071_Messages): B answers cmdIfCancel-1, -1 stays -1, 0 cannot cancel. */
    private void messageCommands(TaskItemScene.Request r) {
        showCommands(r.text, r.commands, null, 0, index -> {
            if (index < 0) {
                if (r.cancelValue > 0) {
                    answerItem(r.cancelValue - 1);
                } else if (r.cancelValue == 0) {
                    messageCommands(r);                        // the question cannot be cancelled
                } else {
                    answerItem(-1);
                }
            } else {
                answerItem(index);
            }
        });
    }

    /** The control codes of a handler's text: {@code \se[name]} plays the SE, {@code \wt[n]} and other waits are dropped. */
    private String itemText(String text) {
        if (text == null) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\\\(se|wt|wtnp|me|bgm)\\[([^\\]]*)\\]").matcher(text);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            if ("se".equals(m.group(1)) && !m.group(2).isEmpty()) playSe(m.group(2), 100);
            m.appendReplacement(out, "");
        }
        m.appendTail(out);
        return out.toString();
    }

    // ---- pbMessageChooseNumber

    private void startNumber(String text, int max, int defaultValue, int cancelValue) {
        messageText = text;
        messageShown = 0;
        messagePausing = false;
        helpVisible = false;
        numberMax = max;
        numberMin = 1;
        numberDigits = String.valueOf(Math.max(1, max)).length();       // ChooseNumberParams#setRange
        numberValue = Math.max(0, Math.min(defaultValue, (int) Math.pow(10, numberDigits) - 1));
        numberIndex = numberDigits - 1;
        numberCancel = cancelValue;
        numberFrame = 0;
        numberDone = value -> answerItem(value);
        mode = Mode.NUMBER;
    }

    private void updateNumber(InputManager input, int ticks) {
        for (int t = 0; t < ticks; t++) advanceMessage();
        if (messageBusy()) {
            if (input.wasPressed(GameAction.CONFIRM) && messagePausing) {
                playDecisionSe();
                messagePausing = false;
                messageShown++;
            }
            return;
        }
        numberFrame = (numberFrame + ticks) % 30;
        if (input.wasRepeated(GameAction.UP) || input.wasRepeated(GameAction.DOWN)) {   // :679-694
            playCursorSe();
            int place = (int) Math.pow(10, numberDigits - 1 - numberIndex);
            int n = numberValue / place % 10;
            numberValue -= n * place;
            n = input.wasRepeated(GameAction.UP) ? (n + 1) % 10 : (n + 9) % 10;
            numberValue += n * place;
        } else if (input.wasRepeated(GameAction.RIGHT)) {                 // :695-701
            if (numberDigits >= 2) {
                playCursorSe();
                numberIndex = (numberIndex + 1) % numberDigits;
                numberFrame = 0;
            }
        } else if (input.wasRepeated(GameAction.LEFT)) {                  // :702-708
            if (numberDigits >= 2) {
                playCursorSe();
                numberIndex = (numberIndex + numberDigits - 1) % numberDigits;
                numberFrame = 0;
            }
        }
        if (input.wasPressed(GameAction.CONFIRM)) {                       // 071_Messages:781-790
            if (numberValue > numberMax || numberValue < numberMin) {
                MenuSe.buzzer(context.audioManager());
            } else {
                playDecisionSe();
                finishNumber(numberValue);
            }
        } else if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            playSe("GUI sel cancel", 80);                                 // pbPlayCancelSE
            finishNumber(numberCancel);
        }
    }

    private void finishNumber(int value) {
        IntDone done = numberDone;
        numberDone = null;
        helpVisible = true;
        mode = Mode.CHOOSE;
        if (done != null) done.done(value);
    }

    private void drawNumber(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, Color[] tc, float w, float h) {
        drawMessage(b, a, f, skin, tc, w, h);
        if (messageBusy()) return;
        float width = numberDigits * 24 + 8 + BORDER;                    // :622
        float height = 32 + BORDER;                                       // :623
        float x = w - width, top = h - (BORDER + 2 * ROW) - height;       // pbPositionNearMsgWindow(:right)
        window(b, a, skin, x, top, width, height);
        String digits = String.format("%0" + numberDigits + "d", numberValue);
        for (int i = 0; i < numberDigits; i++) {
            float cx = x + 16f + i * 24f + 12f;
            txt(b, f, digits.substring(i, i + 1), cx, top + 16f + (32f - f.lineHeight()) / 2f, 2, tc[0], tc[1]);
            if (i == numberIndex && numberFrame / 15 == 0) {              // :719 underline
                float tw = f.width(digits.substring(i, i + 1));
                fillRect(b, cx - tw / 2f, top + 16f + 30f, tw, 2f, tc[0]);
            }
        }
    }

    private void fillRect(SpriteBatch b, float x, float top, float width, float height, Color color) {
        if (pixel == null) {
            com.badlogic.gdx.graphics.Pixmap pm = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            pm.setColor(1f, 1f, 1f, 1f);
            pm.fill();
            pixel = new Texture(pm);
            pm.dispose();
        }
        Color old = b.getColor().cpy();
        b.setColor(color);
        b.draw(pixel, x, screenH - top - height, width, height);
        b.setColor(old);
    }

    // ---- pbTopRightWindow

    private void startTopRight(String text) {
        topRightText = text;
        topRightDone = () -> answerItem(null);
        playDecisionSe();                                                 // :553 pbPlayDecisionSE
        mode = Mode.TOPRIGHT;
    }

    private void updateTopRight(InputManager input) {
        if (input.wasPressed(GameAction.CONFIRM)) {                       // :559 break if Input.trigger?(Input::C)
            Runnable done = topRightDone;
            topRightDone = null;
            mode = Mode.CHOOSE;
            if (done != null) done.run();
        }
    }

    private void drawTopRight(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, Color[] tc, float w) {
        String[] lines = topRightText.split("\r\n");
        float width = 198f;                                               // :549
        float height = BORDER + lines.length * ROW;
        float x = w - width;
        window(b, a, skin, x, 0f, width, height);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int tag = line.indexOf("<r>");
            float y = 16f + i * ROW + (ROW - f.lineHeight()) / 2f;
            if (tag < 0) {
                txt(b, f, line, x + 16f, y, 0, tc[0], tc[1]);
            } else {
                txt(b, f, line.substring(0, tag), x + 16f, y, 0, tc[0], tc[1]);
                txt(b, f, line.substring(tag + 3), x + width - 16f, y, 1, tc[0], tc[1]);
            }
        }
    }

    // =====================================================================
    // 阻塞调用点: pbShowCommands / pbDisplay / pbDisplayConfirm / pbWait
    // =====================================================================

    /** :761-789 pbShowCommands */
    private void showCommands(String helptext, List<String> commands, List<Integer> colorKeys, int index, IntDone done) {
        cmdLabels = commands;
        cmdColorKey = colorKeys;
        cmdIndex = index;                                      // :767
        cmdDone = done;
        helpVisible = true;                                    // :764
        helpText = helptext;                                   // :770
        // :769 helpwindow.resizeHeightToFit(helptext, Graphics.width-cmdwindow.width) — 在 render/布局里按此宽度折行
        helpWidth = -1;
        mode = Mode.COMMANDS;
    }

    private void updateCommands(InputManager input) {
        int n = cmdLabels.size();
        if (input.wasRepeated(GameAction.DOWN)) {              // SpriteWindow_SelectableEx: 长按到底停住，首次按下才回绕
            if (input.wasPressed(GameAction.DOWN) || cmdIndex < n - 1) {
                cmdIndex = (cmdIndex + 1) % n;
                playCursorSe();
            }
        } else if (input.wasRepeated(GameAction.UP)) {
            if (input.wasPressed(GameAction.UP) || cmdIndex > 0) {
                cmdIndex = Math.floorMod(cmdIndex - 1, n);
                playCursorSe();
            }
        }
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {   // :777
            playSe("GUI sel cancel", 80);                      // :778 pbPlayCancelSE
            finishCommands(-1);                                // :779
        } else if (input.wasPressed(GameAction.CONFIRM)) {     // :781
            playDecisionSe();                                  // :782
            finishCommands(cmdIndex);                          // :783
        }
    }

    private void finishCommands(int ret) {
        IntDone done = cmdDone;
        cmdDone = null;
        mode = Mode.CHOOSE;
        if (done != null) done.done(ret);
    }

    /** :707-729 pbDisplay */
    private void display(String text, Runnable then) {
        messageText = text;                                    // :708
        messageShown = 0;
        messagePausing = false;
        helpVisible = false;                                   // :710
        playDecisionSe();                                      // :711
        messageDone = then;
        mode = Mode.MESSAGE;
    }

    private boolean messageBusy() {                             // Window_AdvancedTextPokemon#busy?
        return messageShown < messageText.length();
    }

    private int revealCounter;

    /** One 40fps tick of the letter-by-letter window (speed table of {@link pokemon.runtime.ui.MessageWindow}). */
    private void advanceMessage() {
        if (messagePausing || !messageBusy()) return;
        int speed = context.settings().textspeed;
        int step = speed == 0 ? ((revealCounter++ % 3 == 0) ? 1 : 0) : speed == 2 ? 3 : 1;
        for (int k = 0; k < step && messageBusy(); k++) {
            if (messageText.charAt(messageShown) == PAUSE) {   // \1 为翻页等待
                messagePausing = true;
                break;
            }
            messageShown++;
        }
    }

    private void updateMessage(InputManager input, int ticks) {
        for (int t = 0; t < ticks; t++) advanceMessage();      // self.update
        boolean confirm = input.wasPressed(GameAction.CONFIRM);
        boolean cancel = input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU);
        if (messageBusy()) {                                   // :716
            if (confirm) {                                     // :717
                if (messagePausing) {
                    playDecisionSe();                          // :718
                    messagePausing = false;                    // :719 resume
                    messageShown++;
                }
            }
        } else if (cancel || confirm) {                        // :721-723
            helpVisible = true;                                // :728
            Runnable then = messageDone;
            messageDone = null;
            mode = Mode.CHOOSE;
            if (then != null) then.run();
        }
    }

    /** :730-760 pbDisplayConfirm */
    private void confirm(String text, java.util.function.Consumer<Boolean> done) {
        messageText = text;                                    // :732
        messageShown = 0;
        messagePausing = false;
        helpVisible = false;                                   // :734
        confirmIndex = 0;
        confirmDone = done;
        mode = Mode.CONFIRM;
    }

    private void updateConfirm(InputManager input, int ticks) {
        for (int t = 0; t < ticks; t++) advanceMessage();
        if (messageBusy()) {                                   // :743 cmdwindow 在消息播完前不可见
            if (input.wasPressed(GameAction.CONFIRM) && messagePausing) {
                playDecisionSe();
                messagePausing = false;
                messageShown++;
            }
            return;
        }
        if (input.wasPressed(GameAction.UP) || input.wasPressed(GameAction.DOWN)) {   // 两项的窗口: 首次按下回绕
            confirmIndex = 1 - confirmIndex;
            playCursorSe();
        } else if (input.wasRepeated(GameAction.DOWN) && confirmIndex < 1) {
            confirmIndex = 1;
            playCursorSe();
        } else if (input.wasRepeated(GameAction.UP) && confirmIndex > 0) {
            confirmIndex = 0;
            playCursorSe();
        }
        boolean decided = false;
        boolean ret = false;
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {   // :747
            ret = false;                                       // :748
            decided = true;
        } else if (input.wasPressed(GameAction.CONFIRM)) {     // :750
            ret = confirmIndex == 0;                           // :751
            decided = true;
        }
        if (decided) {
            helpVisible = true;                                // :758
            java.util.function.Consumer<Boolean> done = confirmDone;
            confirmDone = null;
            mode = Mode.CHOOSE;
            if (done != null) done.accept(ret);
        }
    }

    /** :820/:828 pbWait */
    private void pbWait(int frames, Runnable then) {
        waitFrames = frames;
        waitDone = then;
        mode = Mode.WAIT;
    }

    // =====================================================================
    // update
    // =====================================================================

    /** @return true when the screen should close (the top loop broke, or battle chose). */
    public boolean update(InputManager input) {
        if (pendingHardRefresh) {
            pendingHardRefresh = false;
            pbHardRefresh();                                   // :929
        }
        if (forgetSummary != null) {
            if (forgetSummary.update(input)) {
                int chosen = forgetSummary.forgetResult();
                forgetSummary = null;
                answerItem(chosen);
            }
            return finishedNow();
        }
        if (summary != null) {
            if (summary.update(input)) summaryClosed();
            return finishedNow();
        }
        if (evolutionView != null) {
            if (evolutionView.update(input)) evolutionClosed();
            return finishedNow();
        }
        if (nameEntry != null) {
            if (nameEntry.update(input)) {
                String typed = nameEntry.result();
                nameEntry.dispose();
                nameEntry = null;
                Consumer<String> then = nameDone;
                nameDone = null;
                if (then != null) then.accept(typed == null ? "" : typed);
            }
            return finishedNow();
        }
        if (dexView != null) {
            if (dexView.update(input)) {
                dexView = null;
                Runnable then = dexDone;
                dexDone = null;
                mode = Mode.CHOOSE;
                if (then != null) then.run();
            }
            return finishedNow();
        }
        if (relearnView != null) {
            if (relearnView.update(input)) {
                relearnView = null;
                Runnable then = relearnDone;
                relearnDone = null;
                mode = Mode.CHOOSE;
                if (then != null) then.run();
            }
            return finishedNow();
        }
        int ticks = clock.advance();                           // RGSS 的 40fps
        for (int t = 0; t < ticks; t++) updateIcons();         // :529-538 update
        switch (mode) {
            case CHOOSE:
                if (chooseDone != null) updateChoose(input);
                break;
            case COMMANDS:
                updateCommands(input);
                break;
            case MESSAGE:
                updateMessage(input, ticks);
                break;
            case CONFIRM:
                updateConfirm(input, ticks);
                break;
            case NUMBER:
                updateNumber(input, ticks);
                break;
            case TOPRIGHT:
                updateTopRight(input);
                break;
            case WAIT:
                waitFrames -= ticks;
                if (waitFrames <= 0) {
                    Runnable then = waitDone;
                    waitDone = null;
                    mode = Mode.CHOOSE;
                    if (then != null) then.run();
                }
                break;
            default:
                break;
        }
        return finishedNow();
    }

    private boolean finishedNow() {
        if (finished) {
            finished = false;
            if (itemDone != null) {
                Runnable done = itemDone;
                itemDone = null;
                done.run();
            }
            return true;
        }
        return false;
    }

    /** Pokemon_Sprites:184-211 PokemonIconSprite#update (counterLimit + 跳跃动画的帧计数)。 */
    private void updateIcons() {
        for (int i = 0; i < 6; i++) {
            Pokemon p = party.get(i);
            if (p == null) continue;
            int frames = Math.max(1, iconNum[i]);
            int cl = counterLimit(p, frames);
            if (cl == 0) {
                iconCurrent[i] = 0;
            } else {
                iconCounter[i]++;
                if (iconCounter[i] >= cl) {
                    iconCurrent[i] = (iconCurrent[i] + 1) % frames;
                    iconCounter[i] = 0;
                }
            }
        }
    }

    /** Pokemon_Sprites:171-182 counterLimit */
    private static int counterLimit(Pokemon p, int numFrames) {
        if (p.fainted()) return 0;                             // :172
        int ret = 40 / 4;                                      // :175
        if (p.hp <= p.maxHp() / 4) ret *= 4;                   // :176
        else if (p.hp <= p.maxHp() / 2) ret *= 2;              // :177
        ret /= numFrames;                                      // :179
        if (ret < 1) ret = 1;                                  // :180
        return ret;
    }

    // =====================================================================
    // 绘制
    // =====================================================================

    private float screenH;
    /** Next party slot whose battler sprite is preloaded by {@link #render}. */
    private int prefetchIndex;

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, MenuFont smallFont) {
        float w = ScreenMetrics.logicalWidth(), h = ScreenMetrics.logicalHeight();
        screenH = h;
        if (forgetSummary != null) {
            forgetSummary.render(b, a, f, skin);
            return;
        }
        if (summary != null) {
            summary.render(b, a, f, skin);
            return;
        }
        if (evolutionView != null && !evolutionView.hostVisible()) {
            evolutionView.render(b, a, f, skin);
            return;
        }
        if (nameEntry != null) {
            nameEntry.render(b, a, f, skin);
            return;
        }
        if (dexView != null) {
            dexView.render(b, a, f, smallFont);
            return;
        }
        if (relearnView != null && !relearnView.hostVisible()) {
            relearnView.render(b, a, f, skin);
            return;
        }
        // The details panel's battler sprite is decoded from disk the first time a
        // Pokemon is selected, which shows as a hitch on every cursor move. Load
        // one Pokemon's sprite per frame while the screen is idle instead.
        if (prefetchIndex < 6) {
            Pokemon next = party.get(prefetchIndex++);
            if (next != null) battler(a, next);
        }
        Color[] tc = MenuPanel.textColors(skin);
        // :652 addBackgroundPlane partybg (z=0)
        plane(b, a, "bg", w, h);
        // :678 PokemonPartySelectionBackgroundPanel (z=0): x=(width-430)/2, y=228+64
        Texture selbg = a.graphic("Pictures/Party", "panel_pok_base_bg");
        if (selbg != null) img(b, selbg, (w - 430f) / 2f, 292f, 0, 0, selbg.getWidth(), selbg.getHeight());
        // :197-201 details panel: PokemonSprite centred at (width/2, height/2-128+30), z=0
        drawDetailsSprite(b, a, w, h);
        // slots (z 0..2)
        for (int i = 0; i < 6; i++) drawSlot(b, a, i, party.get(i), w);
        // :653-654 partybg2 (z=viewport.z+1)
        plane(b, a, "bg2", w, h);
        // :222-224 / :245 details item icon + exp bar (z=viewport.z+1, 创建在 bg2 之后)
        drawDetailsItemAndExp(b, a);
        // :203-209 details overlays (z=viewport.z+2)
        drawDetailsOverlays(b, a, f, smallFont, w, h);
        // :662-671 storagetext
        drawStorageText(b, f, w);
        // :673-680 helpwindow
        drawCancel(b, a, f);
        if (helpVisible) drawHelp(b, a, f, skin, tc, w);
        switch (mode) {
            case COMMANDS:
                drawCommandWindow(b, a, f, skin, tc, w, h);
                break;
            case MESSAGE:
                drawMessage(b, a, f, skin, tc, w, h);
                break;
            case CONFIRM:
                drawMessage(b, a, f, skin, tc, w, h);
                if (!messageBusy()) drawConfirmWindow(b, a, f, skin, tc, w, h);
                break;
            case NUMBER:
                drawNumber(b, a, f, skin, tc, w, h);
                break;
            case TOPRIGHT:
                drawTopRight(b, a, f, skin, tc, w);
                break;
            default:
                break;
        }
        if (evolutionView != null) evolutionView.render(b, a, f, skin);   // the black fades over the party screen
        if (relearnView != null) relearnView.render(b, a, f, skin);
    }

    private void plane(SpriteBatch b, MenuAssets a, String name, float w, float h) {
        Texture texture = a.graphic("Pictures/Party", name);
        if (texture != null) b.draw(texture, 0f, 0f, w, h);
    }

    /** 以 RGSS 左上原点画位图的一块。 */
    private void img(SpriteBatch b, Texture t, float x, float y, int sx, int sy, int sw, int sh) {
        b.draw(t, x, screenH - y - sh, sw, sh, sx, sy, sw, sh, false, false);
    }

    /** pbDrawTextPositions: align 1=右, 2=居中, 其余=左 (DrawText:1165-1169)。 */
    private void txt(SpriteBatch b, MenuFont f, String text, float x, float y, int align, Color main, Color shadow) {
        if (text == null) return;
        float ty = screenH - y;
        if (align == 1) f.drawRight(b, text, x, ty, main, shadow);
        else if (align == 2) f.drawCentered(b, text, x, ty, main, shadow);
        else f.draw(b, text, x, ty, main, shadow);
    }

    private void drawDetailsSprite(SpriteBatch b, MenuAssets a, float w, float h) {
        Pokemon p = detailsPokemon;
        if (p == null) return;
        Texture sprite = battler(a, p);
        if (sprite == null) return;
        // PokemonSprite#setOffset(Center): ox=width/2, oy=height/2; x=336, y=96+30
        img(b, sprite, w / 2f - sprite.getWidth() / 2f, h / 2f - 128f + 30f - sprite.getHeight() / 2f,
                0, 0, sprite.getWidth(), sprite.getHeight());
    }

    /** :540-612 SelectionPanel#refresh 的绘制部分 + :425-471 初始坐标。 */
    private boolean evolvableOf(int index, Pokemon p) {
        if (evolvable[index] == null) evolvable[index] = canEvolveNow(p);
        return evolvable[index];
    }

    private void drawSlot(SpriteBatch b, MenuAssets a, int index, Pokemon p, float w) {
        if (p == null) return;                                 // :687 BlankPanel 不画
        float x = (w - 430f) / 2f + SLOT_WIDTH * index;        // :430-431
        float y = 226f + 64f;                                  // :432
        Texture shadow = a.graphic("Pictures/Party", "shadow");
        if (shadow != null) img(b, shadow, x + 24f, y + 64f, 0, 0, shadow.getWidth(), shadow.getHeight());   // :547-548
        Texture icon = pokemonIcon(a, p);
        if (icon != null) {
            int size = icon.getHeight();
            int frames = Math.max(1, icon.getWidth() / Math.max(1, size));
            iconNum[index] = frames;
            int frame = Math.min(iconCurrent[index], frames - 1);
            float jumpX = 0f, jumpY = 0f;
            if (selected[index]) {                             // Pokemon_Sprites:202-204
                jumpX = 4f;
                jumpY = frame >= frames / 2 ? -2f : 6f;
            }
            // changeOrigin(Center): ox = width/2, oy = height*5/8; x=+36, y=+52
            img(b, icon, x + 36f - size / 2f + jumpX, y + 52f - size * 5f / 8f + jumpY, frame * size, 0, size, size);
        }
        // :563-570 held item: x+40, y+56, ES's Battle Info Display:590 zoom 0.5
        if (hasItem(p)) {
            Texture item = heldItemIcon(a, p.item);
            if (item != null) {
                b.draw(item, x + 40f, screenH - (y + 56f) - item.getHeight() * 0.5f,
                        item.getWidth() * 0.5f, item.getHeight() * 0.5f);
            }
        }
        // :581-610 overlay (x+12, y)
        int status = statusIndex(p);
        Texture statuses = a.graphic("Pictures", "statuses");
        if (status >= 0 && statuses != null && status * 16 + 16 <= statuses.getHeight()) {   // :587-590
            img(b, statuses, x + 12f, y + 12f, 0, status * 16, 44, 16);
        }
        if (evolvableOf(index, p)) {                           // :592-598
            Texture evo = a.graphic("Pictures/Party", "icon_evo");
            if (evo != null) img(b, evo, x + 12f, y + 64f, 0, 0, Math.min(34, evo.getWidth()), Math.min(13, evo.getHeight()));
        }
        if (selected[index]) {                                 // :599-601
            Texture arrow = a.graphic("Pictures/Party", "arrow_normal");
            if (arrow != null) img(b, arrow, x + 12f + 12f, y, 0, 0, Math.min(20, arrow.getWidth()), Math.min(12, arrow.getHeight()));
        }
        if (preselected[index]) {                              // :606-608
            Texture arrow = a.graphic("Pictures/Party", "arrow_preselect");
            if (arrow != null) img(b, arrow, x + 12f + 12f, y, 0, 0, Math.min(20, arrow.getWidth()), Math.min(12, arrow.getHeight()));
        }
    }

    /** :245-246/:397-402 ItemIconSprite(194,194+64) 与 :222-224 PokemonPartyExpBar。 */
    private void drawDetailsItemAndExp(SpriteBatch b, MenuAssets a) {
        Pokemon p = detailsPokemon;
        if (p == null) return;
        if (hasItem(p)) {                                      // :398-402 item>0 才可见
            Texture item = itemIcon(a, p.item);
            if (item != null) {
                // ItemIconSprite#item= -> changeOrigin with @offset=nil => Center (PItem_Sprites:49-67, 89):
                // ox = width/2, oy = height/2, so (194,258) is the icon centre, not its top-left.
                int frameWidth = item.getHeight() == 48 ? 48 : item.getWidth();   // :77-83 numframes>1 -> 48 wide frames
                frameWidth = Math.min(frameWidth, item.getWidth());
                img(b, item, 194f - frameWidth / 2f, 258f - item.getHeight() / 2f, 0, 0, frameWidth, item.getHeight());
            }
        }
        expBar.draw(b, a, this);
    }

    /** :263-376 refreshOverlay */
    private void drawDetailsOverlays(SpriteBatch b, MenuAssets a, MenuFont f, MenuFont small, float w, float h) {
        Pokemon p = detailsPokemon;
        if (p == null) return;
        float offset = w / 2f - 96f;                           // :265
        // :269-277 HP bar
        Texture hp = a.graphic("Pictures/Party", "overlay_hp");
        if (hp != null && p.maxHp() > 0) {
            float hpw = p.hp * 96f / p.maxHp();                // :269
            if (hpw < 1f) hpw = 1f;                            // :270
            hpw = Math.round(hpw / 2f) * 2f;                   // :271
            int hpzone = 0;                                    // :272
            if (p.hp <= p.maxHp() / 2) hpzone = 1;             // :273
            if (p.hp <= p.maxHp() / 4) hpzone = 2;             // :274
            img(b, hp, 91f + offset, 194f + 56f, 0, hpzone * 6, (int) hpw, 6);   // :276
        }
        // :281-285 textpos (system font)
        txt(b, f, p.name == null ? "" : p.name, -90f + offset, 15f, 1, LINE, OUTLINE);          // :282
        txt(b, f, String.valueOf(p.level), 262f + offset + 32f, 18f, 3, DARK, DARK_SHADOW);     // :283
        txt(b, f, detailsText, w - 16f, 200f + 32f, 1, LINE, OUTLINE);                          // :284 @text
        if (!p.egg) {                                          // :289-298 gender
            if (p.effectiveGender() == PokemonStats.MALE) {               // :291
                Texture g = a.graphic("Pictures/Party", "icon_male");
                if (g != null) img(b, g, 152f, 18f, 0, 0, 20, 20);                              // :292-293
            } else if (p.effectiveGender() == PokemonStats.FEMALE) {      // :294
                Texture g = a.graphic("Pictures/Party", "icon_famale");
                if (g != null) img(b, g, 152f, 18f, 0, 0, 20, 20);                              // :295-296
            }
        }
        if (p.shiny) {                                         // :301-309
            Texture s = a.graphic("Pictures", p.superShiny ? "superShiny" : "shiny");
            if (s != null) img(b, s, 358f + offset, 18f, 0, 0, 20, 20);
        }
        if (!p.egg) {                                          // :310-315 ball icon
            PbsData.Item ball = BallTypes.item(context.pbsData(), p.ballused);   // :312 pbBallTypeToItem
            Texture ballIcon = ball == null ? null : itemIcon(a, ball.internalName);
            if (ballIcon != null) img(b, ballIcon, w - 52f, 3f, 0, 0, 48, 48);                  // :313-314
        }
        // :317-375 overlaysprite2 (small font)
        if (!p.egg) {                                          // :320
            String formName = formName(p);                     // :322-325
            txt(b, small, String.format("%3d/%3d", p.hp, p.maxHp()), 130f + offset, 200f + 56f, 2, LINE, OUTLINE);   // :287
            txt(b, small, formName, 4f, 108f + 8f, 0, DARK, DARK_SHADOW);                       // :327
            String nature = p.nature == null ? "" : PBNatures.getName(p.nature.id);             // :329
            txt(b, small, "性格:" + nature, 4f, 134f + 8f, 0, DARK, DARK_SHADOW);               // :330
            txt(b, small, "特性:" + abilityName(p), 4f, 160f + 8f, 0, DARK, DARK_SHADOW);       // :333
            txt(b, small, "个体:", 4f, 186f + 8f, 0, DARK, DARK_SHADOW);                        // :335
            txt(b, small, "招式:", 232f + offset + 42f, 54f + 32f, 0, LINE, OUTLINE);           // :337
            int i = 0;                                         // :338
            for (Pokemon.MoveSlot m : p.moves) {               // :339
                if (m.move == null) continue;                  // :340 next if m.id == 0
                txt(b, small, m.move.name, 248f + offset + 24f, 78f + 32f + 24f * i, 0, DARK, DARK_SHADOW);   // :342
                i += 1;                                        // :343
            }
            txt(b, small, "属性:", 4f, 56f, 0, LINE, OUTLINE);                                  // :346
            drawTypes(b, a, p);                                // :348-353
            // :356-373 IV ratings
            float[] xs = {56f, 71f, 86f, 132f, 101f, 116f};
            for (int k = 0; k < 6; k++) {
                Texture rating = a.graphic("Pictures/Summary", ratingFile(p.ivs[k]));
                if (rating != null) img(b, rating, xs[k], 188f + 8f, 0, 0, 16, 20);
            }
        }
    }

    private void drawTypes(SpriteBatch b, MenuAssets a, Pokemon p) {
        Texture types = a.graphic("Pictures", "types");
        if (types == null || context.pbsData() == null) return;
        com.badlogic.gdx.utils.Array<String> list = p.types();
        PbsData.TypeInfo type1 = list.size > 0 ? context.pbsData().type(list.get(0)) : null;
        PbsData.TypeInfo type2 = list.size > 1 ? context.pbsData().type(list.get(1)) : type1;
        if (type1 != null && type1.id >= 0 && type1.id * 28 + 28 <= types.getHeight()) {
            img(b, types, 4f, 80f, 0, type1.id * 28, 64, 28);                                   // :348-349
        }
        if (type1 != type2 && type2 != null && type2.id >= 0 && type2.id * 28 + 28 <= types.getHeight()) {   // :350
            img(b, types, 68f, 80f, 0, type2.id * 28, 64, 28);                                  // :351-352
        }
    }

    private static String ratingFile(int iv) {                 // :358-370
        if (iv > 30) return "RatingS";
        if (iv > 22) return "RatingA";
        if (iv > 15) return "RatingB";
        if (iv > 7) return "RatingC";
        if (iv > 0) return "RatingD";
        return "RatingF";
    }

    /** :322-325 pbGetMessage(FormNames) —— 登记: 用 pbs 的 formName 数据代替消息表。 */
    private String formName(Pokemon p) {
        String name = p.form != null ? p.form.formName : (p.species == null ? null : p.species.formName);
        if (name == null || name.isEmpty()) name = "默认形态";  // :324
        // :325 formName += "※" if @pokemon.forcedForm — 登记: forcedForm 未建模，恒 false
        return name;
    }

    private String abilityName(Pokemon p) {                     // :332 PBAbilities.getName
        PbsData.Ability ability = context.pbsData() == null ? null : context.pbsData().ability(p.ability);
        if (ability != null && ability.name != null) return ability.name;
        return p.ability == null ? "" : p.ability;
    }

    /** :662-671 */
    private void drawStorageText(SpriteBatch b, MenuFont f, float w) {
        if (!canAccessStorage) return;                         // :663 text == ""
        float offsetX = f.width("[F]: 寄存系统");              // :664
        float x = w - offsetX - 16f;                           // :665
        float y = 182f + 32f;                                  // :666
        // windowskin = nil (:671)；文本在窗口内容区 (x+16, y+16)
        txt(b, f, "[F]:寄存系统", x + 16f, y + 16f + (ROW - f.lineHeight()) / 2f, 0, STORAGE_BASE, OUTLINE);
    }

    /** :100-115 PokemonPartyCancelSprite 与 :34-98 PokemonPartyConfirmCancelSprite。 */
    private void drawCancel(SpriteBatch b, MenuAssets a, MenuFont f) {
        Texture button = a.graphic("Pictures/Party", selected[CANCEL] ? "icon_cancel_sel" : "icon_cancel");   // :88
        float x = 378f + 64f + 64f, y = 330f + 64f;            // :103
        if (button != null) img(b, button, x, y, 0, 0, button.getWidth(), button.getHeight());
        txt(b, f, "取消", x + 55f, y + 8f, 2, LINE, OUTLINE);   // :53 [text,55,(narrowbox)?4:8,2,white,132]
    }

    // ---- 窗口 (SpriteWindow)

    /** 窗口顶部 RGSS y -> 画窗口框。 */
    private void window(SpriteBatch b, MenuAssets a, WindowSkin skin, float x, float top, float width, float height) {
        MenuPanel.window(b, a, skin, x, screenH - top - height, width, height);
    }

    private List<String> wrap(MenuFont f, String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : (text == null ? "" : text).split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < paragraph.length(); i++) {
                char c = paragraph.charAt(i);
                if (c == PAUSE) continue;
                if (line.length() > 0 && f.width(line.toString() + c) > maxWidth) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                line.append(c);
            }
            lines.add(line.toString());
        }
        return lines;
    }

    private static float commandWindowWidth(MenuFont f, List<String> labels) {   // Window_DrawableCommand#getAutoDims
        float width = 0f;
        for (String label : labels) width = Math.max(width, f.width(label));
        return width + 16f + 16f + 4f + BORDER;                 // +TEXTPADDING(SpriteWindow:887)
    }

    private void drawHelp(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, Color[] tc, float w) {
        if (mode == Mode.COMMANDS && helpWidth < 0) {          // :769 resizeHeightToFit(helptext, Graphics.width-cmdwindow.width)
            resolvedHelpWidth = w - commandWindowWidth(f, cmdLabels);
            resolvedHelpLines = wrap(f, helpText, resolvedHelpWidth - BORDER - 4f);
        }
        float width;
        List<String> lines;
        if (helpWidth < 0) {                                   // 命令菜单留下的窗口尺寸一直保留到下一次 pbSetHelpText
            width = resolvedHelpWidth;
            lines = resolvedHelpLines;
        } else {
            width = helpWidth;
            lines = new ArrayList<>();
            lines.add(helpText);
        }
        float height = BORDER + lines.size() * ROW;
        float top = screenH - height;                          // :771 pbBottomLeft / :680 pbBottomLeftLines
        window(b, a, skin, 0f, top, width, height);
        for (int i = 0; i < lines.size(); i++) {
            txt(b, f, lines.get(i), 16f, top + 16f + i * ROW + (ROW - f.lineHeight()) / 2f, 0, tc[0], tc[1]);
        }
    }

    private void drawCommandWindow(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, Color[] tc, float w, float h) {
        float width = commandWindowWidth(f, cmdLabels);
        float height = Math.min(BORDER + cmdLabels.size() * ROW, h);
        float x = w - width, top = h - height;                 // :768 pbBottomRight
        window(b, a, skin, x, top, width, height);
        for (int i = 0; i < cmdLabels.size(); i++) {
            Color main = tc[0], shadow = tc[1];
            if (cmdColorKey != null && cmdColorKey.get(i) == 1) {   // :132-135 Window_CommandPokemonColor#drawItem
                main = COLOR_KEY_BASE;
                shadow = COLOR_KEY_SHADOW;
            }
            float rowTop = top + 16f + i * ROW;
            txt(b, f, cmdLabels.get(i), x + 16f + 16f, rowTop + (ROW - f.lineHeight()) / 2f, 0, main, shadow);
        }
        MenuPanel.cursor(b, a, skin, x, screenH - top - height, height, cmdIndex, cmdIndex, ROW);   // :129 drawCursor
    }

    private void drawMessage(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, Color[] tc, float w, float h) {
        float height = BORDER + 2 * ROW;                       // :660 pbBottomLeftLines(messagebox,2)
        float top = h - height;
        window(b, a, skin, 0f, top, w, height);
        String shown = messageText.substring(0, Math.min(messageShown, messageText.length()));
        List<String> lines = wrap(f, shown, w - BORDER - 4f);
        for (int i = 0; i < lines.size() && i < 2; i++) {
            txt(b, f, lines.get(i), 16f, top + 16f + i * ROW + (ROW - f.lineHeight()) / 2f, 0, tc[0], tc[1]);
        }
    }

    private void drawConfirmWindow(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, Color[] tc, float w, float h) {
        List<String> labels = java.util.Arrays.asList("是", "否");   // :735
        float width = commandWindowWidth(f, labels);
        float height = BORDER + 2 * ROW;
        float x = w - width, top = h - height - (BORDER + 2 * ROW);   // :737-738
        window(b, a, skin, x, top, width, height);
        for (int i = 0; i < 2; i++) {
            txt(b, f, labels.get(i), x + 32f, top + 16f + i * ROW + (ROW - f.lineHeight()) / 2f, 0, tc[0], tc[1]);
        }
        MenuPanel.cursor(b, a, skin, x, screenH - top - height, height, confirmIndex, confirmIndex, ROW);
    }

    // =====================================================================
    // PokemonPartyExpBar (:1598-1697)
    // =====================================================================

    private final class ExpBar {
        static final float EXP_BAR_FILL_TIME = 1.75f;           // :1602
        private Pokemon pokemon;
        private boolean animating;                              // :1607
        private float currentExp, endExp, rangeExp, expIncPerFrame;
        private float srcWidth = -1f;                           // 未 refresh 过时是整张位图宽度(src_rect 默认)
        private static final float X = 235f + 48f;              // :223
        private static final float Y = 220f + 56f;              // :224

        void pokemon(Pokemon value) {                           // :1654-1657
            pokemon = value;
            refresh();
        }

        /** :1659-1662 */
        float expFraction() {
            if (pokemon == null || pokemon.egg) return 0f;
            if (animating) return currentExp / rangeExp;
            return pokemonExpFraction(pokemon);
        }

        /** :1664-1670 */
        void animateExp(float oldExp, float newExp, float range) {
            currentExp = oldExp;
            endExp = newExp;
            rangeExp = range;
            expIncPerFrame = range / (EXP_BAR_FILL_TIME * 40f);
            animating = true;
        }

        /** :1672-1680 (宽度依赖位图尺寸，登记: 延迟到 draw 时按当时的经验比例计算) */
        void refresh() {
            pendingRefresh = pokemon != null && !pokemon.egg;   // :1673 return if !@pokemon || @pokemon.egg?
        }

        private boolean pendingRefresh = true;

        /** :1682-1696 */
        void update() {
            if (animating) {
                if (currentExp < endExp) {
                    currentExp += expIncPerFrame;
                    if (currentExp >= endExp) currentExp = endExp;
                } else if (currentExp > endExp) {
                    currentExp -= expIncPerFrame;
                    if (currentExp <= endExp) currentExp = endExp;
                }
                refresh();
                if (currentExp == endExp) animating = false;
            }
        }

        void draw(SpriteBatch b, MenuAssets a, PartyView view) {
            Texture bar = a.graphic("Pictures/Party", "overlay_exp");
            if (bar == null) return;
            if (srcWidth < 0f) srcWidth = bar.getWidth();
            if (pendingRefresh && pokemon != null && !pokemon.egg) {
                float wd = expFraction() * bar.getWidth();      // :1675
                if (!Float.isNaN(wd)) {                         // :1676
                    srcWidth = Math.round(wd / 2f) * 2f;        // :1678-1679
                }
                pendingRefresh = false;
            }
            int sw = Math.max(0, Math.min((int) srcWidth, bar.getWidth()));
            if (sw > 0) view.img(b, bar, X, Y, 0, 0, sw, bar.getHeight());   // :1613-1616
        }
    }

    /** 他段: PokeBattle_Pokemon:153-160 expFraction (PBExperience.maxLevel=210)。 */
    private static float pokemonExpFraction(Pokemon p) {
        int l = p.level;
        if (l >= PBExperience.maxLevel()) return 0f;
        int startexp = PokemonStats.experienceForLevel(p.growthRate(), l);
        int endexp = PokemonStats.experienceForLevel(p.growthRate(), l + 1);
        return 1f * (p.exp - startexp) / (endexp - startexp);
    }

    // =====================================================================
    // 音效 / 文本工具
    // =====================================================================

    private void playSe(String name, int volume) {
        if (context.audioManager() != null) context.audioManager().playSe(name, volume, 100);
    }

    private void playCursorSe() {                               // 登记: $data_system.cursor_se 未建模
        MenuSe.cursor(context.audioManager());
    }

    private void playDecisionSe() {                             // 登记: $data_system.decision_se 未建模
        MenuSe.decision(context.audioManager());
    }

    private void playCloseMenuSe() {                            // Audio_Play:288-292 pbPlayCloseMenuSE
        playSe("GUI menu close", 80);
    }

    /** _INTL 的 {1}{2}{3} 占位符替换。 */
    private static String intl(String template, Object... args) {
        String out = template;
        for (int i = 0; i < args.length; i++) out = out.replace("{" + (i + 1) + "}", String.valueOf(args[i]));
        return out;
    }

    // =====================================================================
    // 他段已有的转译（原样保留）: 状态索引 / 进化判定 / 素材名
    // =====================================================================

    private static int statusIndex(Pokemon p) {
        if (p == null) return -1;
        // :583-586: pokerus 8, then status-1 overrides it, then fainted 7 overrides both.
        int status = -1;
        if (p.pokerusStage() == 1) status = 8;                        // :584
        if (p.status != null && !p.status.isEmpty()) {         // :585
            switch (p.status) {
                case "SLEEP": status = 0; break;
                case "POISON": status = 1; break;
                case "BURN": status = 2; break;
                case "PARALYSIS": status = 3; break;
                case "FROZEN": status = 4; break;
                default: break;
            }
        }
        if (p.hp <= 0) status = 7;                             // :586
        return status;
    }

    /**
     * {@code pbCheckEvolution(pkmn)} (201_Pokemon_Evolution:280-283) > 0 (PScreen_Party:592-598 / :1315-1318): the check
     * of every level-up evolution method of the species, with the world they read ({@link EvolutionWorld}).
     */
    private boolean canEvolveNow(Pokemon p) {
        return evolutionTarget(p) != null;
    }

    private PbsData.Species evolutionTarget(Pokemon p) {
        if (p == null || p.species == null || p.egg || context.pbsData() == null) return null;
        String name = PBEvolution.checkEvolution(p, null, new EvolutionWorld(context));
        return name == null ? null : context.pbsData().species(name);
    }

    private boolean partyHasType(String type) {
        if (context.pbsData() == null) return false;
        for (Pokemon member : party.members()) {
            if (member == null || member.species == null) continue;
            if (member.types().contains(type, false)) return true;
        }
        return false;
    }

    private boolean ownedSpecies(String name) {
        return name != null && !name.isEmpty() && trainer.owned.contains(name);
    }

    private static int intParameter(String parameter) {
        try {
            return parameter == null ? 0 : Integer.parseInt(parameter.trim());
        } catch (NumberFormatException invalid) {
            return 0;
        }
    }

    private Texture pokemonIcon(MenuAssets a, Pokemon p) {
        if (p.species == null) return null;
        Texture icon = PokemonIcons.of(a, p);   // pbPokemonIconFile: shiny / form fall back to the plain icon
        return icon;
    }

    /** PSystem_FileUtilities:273-300 pbItemIconFile: item<NAME> -> item%03d -> item000. */
    private Texture itemIcon(MenuAssets a, String id) {
        return ItemIcons.of(a, context.pbsData(), id);
    }

    /** ES's Battle Info Display:597-617 pbHeldItemIconFile: item%03d, 否则 icon_item。 */
    private Texture heldItemIcon(MenuAssets a, String id) {
        PbsData.Item item = context.pbsData() == null ? null : context.pbsData().item(id);
        Texture icon = item == null ? null : a.icon(String.format("item%03d", item.id));
        if (icon == null) icon = a.graphic("Pictures/Party", "icon_item");   // 登记: 原文路径 Graphics/Party/icon_item 不存在，用 Pictures/Party/icon_item
        return icon;
    }

    /** Pokemon_Sprites PokemonSprite#setPokemonBitmap -> pbLoadPokemonBitmap；登记: 沿用 SummaryView 的取图规则。 */
    private Texture battler(MenuAssets a, Pokemon p) {
        if (p.species == null) return null;
        int form = p.form == null ? 0 : p.form.form;
        return BattlerBitmaps.find(p.species, false,
                p.effectiveGender() == PokemonStats.FEMALE, p.shiny, p.superShiny, form,
                name -> a.graphic("Battlers", name));
    }
}
