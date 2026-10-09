package pokemon.runtime.ui.menu;

import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.battle.PBNatures;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.Party;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.Storage;
import pokemon.runtime.pokemon.TrainerState;
import pokemon.runtime.ui.WindowSkin;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

/**
 * 段 B2W2 PC (Scripts.rxdata #306, 2347 行；原文 {@code __r20-ref/ruby/B2W2_PC.rb}，对账表
 * docs/stage4-pc-storage-gap.md)：{@code PokemonStorageScene} + {@code PokemonStorageScreen}
 * 以及各精灵类。#306 覆盖了 PScreen_PokemonStorage(#222) 的全部类与方法，#222 不转译。
 *
 * <p>RGSS 里阻塞的 {@code loop do ... end}（pbSelectBoxInternal / pbShowCommands / pbDisplay /
 * pbHold / pbPlace / pbSwitchBoxToRight ...）拆成 {@link Mode} 状态机，调用点之后的逻辑是续体。
 * 精灵对象保留成 {@link Icon} / {@link BoxSprite} / {@link PartySprite}：原文里「精灵」和「数据」会在动画期间
 * 暂时不一致（例如拿起宝可梦时图标先离开槽位，数据后删），这里照样保留。
 * 坐标用 RGSS 左上原点，只在 {@link #img}/{@link #txt}/{@link #window} 里换成 libGDX 下原点。
 * 动画和等待按 40fps 的 tick 计（{@link MenuClock}），输入每个渲染帧处理一次。</p>
 *
 * <p>登记（整段共通）：淡入淡出(pbFadeInAndShow/pbFadeOutAndHide/pbFadeOutIn)未建模；
 * pbEnterPokemonName / pbEnterBoxName / 搜索的文字输入、pbPokemonDebug、邮件(pkmn.mail 恒 nil)未建模，
 * 对应选项留空；{@code pkmn.formTime}、Graphics.frame_reset 未建模；
 * pbPlayCursorSE/DecisionSE 原文先取 $data_system，这里用 {@link MenuSe}。</p>
 */
public final class StorageView {
    private static final int BOX_X = 190;                       // B2W2:358
    private static final int BOX_Y = 18;                        // :359
    private static final int ROW = 32;
    private static final int BORDER = 32;

    private static final Color BTN = new Color(239f / 255f, 239f / 255f, 239f / 255f, 1f);          // :1465
    private static final Color BTN_SHADOW = new Color(132f / 255f, 132f / 255f, 132f / 255f, 1f);   // :1466
    private static final Color BASE = new Color(90f / 255f, 82f / 255f, 82f / 255f, 1f);            // :1482
    private static final Color SHADOW = new Color(165f / 255f, 165f / 255f, 173f / 255f, 1f);       // :1483
    private static final Color BOX_NAME = new Color(41f / 255f, 41f / 255f, 41f / 255f, 1f);        // :463
    private static final Color WHITE = new Color(1f, 1f, 1f, 1f);

    private enum Mode { SELECT_BOX, SELECT_PARTY, COMMANDS, DISPLAY, MARKING, ANIM, MESSAGE, SUMMARY, BAG, TEXT, IDLE }

    // ------------------------------------------------------------------
    // 精灵 (PokemonBoxIcon / PokemonBoxSprite / PokemonBoxPartySprite / PokemonBoxArrow)
    // ------------------------------------------------------------------

    /** :41-86 PokemonBoxIcon (含 Interpolator 补间)。 */
    private static final class Icon {
        Pokemon pokemon;
        float x, y;
        float zoom = 1f, opacity = 1f;
        boolean disposed, centered;
        boolean tweening, startRelease;
        int step, frames;
        float zoom0, zoomTarget, opacity0, opacityTarget;
        boolean tweenZoom;

        Icon(Pokemon pokemon) {                                  // :42-48
            this.pokemon = pokemon;
        }

        boolean releasing() {                                    // :50-52
            return tweening;
        }

        /** :54-65 release: zoom/opacity -> 0 over 100 frames, around the icon centre. */
        void release() {
            centered = true;                                     // ox/oy = 32, x/y += 32
            x += 32f;
            y += 32f;
            tween(true, 0f, 0f, 100);
            startRelease = true;
        }

        /** :67-72 releaseAll: opacity -> 0 over 60 frames. */
        void releaseAll() {
            tween(false, 1f, 0f, 60);
            startRelease = true;
        }

        private void tween(boolean withZoom, float zoomTo, float opacityTo, int frameCount) {   // Interpolators:22-51
            tweenZoom = withZoom;
            zoom0 = zoom;
            zoomTarget = zoomTo;
            opacity0 = opacity;
            opacityTarget = opacityTo;
            frames = frameCount;
            step = 0;
            tweening = true;
        }

        /** :80-85 update + Interpolator#update (:53-86). */
        void update() {
            if (tweening) {
                float t = step * 1f / frames;
                if (tweenZoom) zoom = zoom0 + (zoomTarget - zoom0) * t;
                opacity = opacity0 + (opacityTarget - opacity0) * t;
                step++;
                if (step == frames) {
                    step = 0;
                    frames = 0;
                    tweening = false;
                }
            }
            if (startRelease && !tweening) disposed = true;      // dispose if @startRelease && !releasing?
        }
    }

    /** :340-492 PokemonBoxSprite */
    private final class BoxSprite {
        final int number;
        float x = BOX_X, y = BOX_Y;
        final Icon[] icons = new Icon[Storage.SLOTS];
        boolean refreshSprites = true;
        float tintAlpha;                                         // color=Color(248,248,248,alpha)

        BoxSprite(int number) {                                  // :343-361
            this.number = number;
            for (int i = 0; i < Storage.SLOTS; i++) icons[i] = new Icon(storage.get(number, i));
            refresh();
        }

        /** :453-482 (只做图标定位；盒子图/盒名在绘制时取 box.name/background) */
        void refresh() {
            float yval = y + 36f;                                // :467
            for (int j = 0; j < 5; j++) {
                float xval = x + 10f;                            // :469
                for (int k = 0; k < 6; k++) {
                    Icon sprite = icons[j * 6 + k];
                    if (sprite != null && !sprite.disposed) {
                        sprite.x = xval;
                        sprite.y = yval;
                    }
                    xval += 48f;
                }
                yval += 48f;
            }
        }

        Icon getPokemon(int index) {                             // :428-430
            return icons[index];
        }

        void setPokemon(int index, Icon sprite) {                // :432-436
            icons[index] = sprite;
            if (sprite != null) sprite.update();                 // sprite.refresh (bitmap reload)
            refresh();
        }

        void grabPokemon(int index) {                            // :438-445
            Icon sprite = icons[index];
            if (sprite != null) {
                arrowGrab(sprite);
                icons[index] = null;
                refresh();
            }
        }

        void deletePokemon(int index) {                          // :447-451
            if (icons[index] != null) icons[index].disposed = true;
            icons[index] = null;
            refresh();
        }

        void update() {                                          // :484-491
            for (Icon icon : icons) if (icon != null && !icon.disposed) icon.update();
        }
    }

    /** :499-614 PokemonBoxPartySprite */
    private final class PartySprite {
        float x = 182, y;
        final List<Icon> icons = new ArrayList<>();

        PartySprite(Party party, float y) {                      // :500-518
            this.y = y;
            for (int i = 0; i < 6; i++) {
                Pokemon pokemon = party.get(i);
                if (pokemon != null) icons.add(new Icon(pokemon));
            }
            refresh();
        }

        /** :584-606 refresh: icon positions xvalues/yvalues. */
        void refresh() {
            float[] xvalues = {18, 90, 18, 90, 18, 90};
            float[] yvalues = {2, 18, 66, 82, 130, 146};
            icons.removeIf(icon -> icon != null && icon.disposed);
            for (int j = 0; j < icons.size() && j < 6; j++) {
                Icon sprite = icons.get(j);
                sprite.x = x + xvalues[j];
                sprite.y = y + yvalues[j];
            }
        }

        Icon getPokemon(int index) {                             // :557-559
            return index >= 0 && index < icons.size() ? icons.get(index) : null;
        }

        void setPokemon(int index, Icon sprite) {                // :561-565 (@pokemonsprites.compact!)
            while (icons.size() <= index) icons.add(null);
            icons.set(index, sprite);
            icons.removeIf(icon -> icon == null);
            refresh();
        }

        void grabPokemon(int index) {                            // :567-575
            Icon sprite = getPokemon(index);
            if (sprite != null) {
                arrowGrab(sprite);
                icons.set(index, null);
                icons.removeIf(icon -> icon == null);
                refresh();
            }
        }

        void deletePokemon(int index) {                          // :577-582
            Icon sprite = getPokemon(index);
            if (sprite != null) sprite.disposed = true;
            if (index >= 0 && index < icons.size()) icons.set(index, null);
            icons.removeIf(icon -> icon == null);
            refresh();
        }

        void update() {                                          // :608-613
            for (Icon icon : icons) if (icon != null && !icon.disposed) icon.update();
        }
    }

    // ------------------------------------------------------------------
    // 状态
    // ------------------------------------------------------------------
    private final RuntimeContext context;
    private final TrainerState trainer;
    private final Storage storage;
    private final Party party;
    private final MenuClock clock = new MenuClock();
    private final PbMessage pbMessage;
    private final int command;
    private Mode mode = Mode.IDLE;
    private boolean finished;
    private float screenH, screenW;

    // PokemonStorageScene ivars (:625, :641-645)
    private int selection;
    private boolean quickswap;
    private boolean choseFromParty;
    private int boxForMosaic = Integer.MIN_VALUE, selectionForMosaic = Integer.MIN_VALUE;
    private BoxSprite box;
    private BoxSprite newBox;
    private PartySprite boxparty;
    // arrow (:167-333)
    private float arrowX, arrowY;                                // pbSetArrow 写入的 x/y（@spriteX/@spriteY）
    private float arrowYOffset;
    private int arrowFrame;
    private int grabbingState, placingState;
    private boolean holding, arrowQuick;
    private Icon heldSprite;
    /** The sprite a pbPlace is putting down: the arrow lets go of it (:311 @heldpkmn = nil) but the sprite stays on screen until pbPlace moves it into its slot. */
    private Icon floating;
    private String handImage = "cursor_point_1";
    // overlay (pbUpdateOverlay :1460-1604)
    private Pokemon overlayPokemon;
    private boolean overlayHasPokemon;
    private int overlayPartyCount;
    private Pokemon mosaicPokemon;
    private int mosaic;
    private Texture mosaicTexture;
    private int mosaicTextureValue = -1;
    private Pokemon mosaicTexturePokemon;
    // marking (:1355-1435)
    private boolean markingVisible;
    private int markingIndex, markingValue;
    private Pokemon markingPokemon;
    private Runnable markingDone;

    // PokemonStorageScreen (:1623)
    private Pokemon heldpkmn;

    // 阻塞调用点
    private Consumer<int[]> selectDone;
    private IntConsumer selectPartyDone;
    private boolean selectPartyDepositing;
    private int selectPartyLastsel;
    private String displayText = "";
    private Runnable displayDone;
    private String cmdMessage = "";
    private List<String> cmdLabels = new ArrayList<>();
    private int cmdIndex, cmdTop;
    private IntConsumer cmdDone;
    private BooleanSupplier animStep;
    private Runnable animDone;
    private SummaryView summary;
    private Runnable summaryDone;
    private int[] summarySlots;
    private int summarySelectedBox;
    private boolean summaryParty;
    private java.util.function.BiConsumer<Predicate<String>, Consumer<String>> itemHost;
    private java.util.function.Consumer<Pokemon> bagHost;

    public StorageView(RuntimeContext context) {
        this(context, 0);
    }

    private boolean chooseEgg;
    private int[] eggChoice;

    /** {@code PokemonStorageScreen#pbChooseEggToHatch} (306_B2W2_PC:2273-2316): the box screen of the Egg Hatcher. */
    public static StorageView chooseEggToHatch(RuntimeContext context) {
        StorageView view = new StorageView(context, 1, false);
        view.chooseEgg = true;
        return view;
    }

    /** {@code pbChooseEggToHatch}'s return value: {@code [box, slot]} or null. */
    public int[] eggChoice() {
        return eggChoice;
    }

    /** {@code PokemonStorageScreen#pbStartScreen(command)}: 0 整理 / 1 取出 / 2 存放 / 3 仅打开关闭。 */
    public StorageView(RuntimeContext context, int command) {
        this(context, command, false);
    }

    /**
     * {@code menuEntry}: the pause menu's 寄存系统 entry (Modular Menu:70-82): plays
     * {@code BW2MenuChoose} and refuses on the 13 blocked maps with 「暂时无法使用。」
     * before {@code pbStartScreen(0)}.
     */
    public StorageView(RuntimeContext context, int command, boolean menuEntry) {
        this.context = context;
        this.trainer = context.gameState().trainer();
        this.storage = trainer.currentStorage();
        this.party = storage.party();
        this.pbMessage = new PbMessage(context);
        this.command = command;
        screenH = ScreenMetrics.logicalHeight();
        screenW = ScreenMetrics.logicalWidth();
        if (menuEntry) {
            playSe("BW2MenuChoose", 100);                        // Modular Menu:71
            int[] maps = {95, 96, 97, 98, 117, 118, 119, 153, 299, 357, 416, 465, 467};   // :72
            for (int map : maps) {
                if (map == context.gameState().currentMapId()) {                           // :73
                    pbMessagePlain("暂时无法使用。", () -> finished = true);                // :74
                    return;
                }
            }
        }
        startScreen();
    }

    /** {@code pbChooseItem}(:1286-1294): the host opens the bag, then hands back the chosen item id (or null). */
    public void itemHost(java.util.function.BiConsumer<Predicate<String>, Consumer<String>> host) {
        this.itemHost = host;
    }

    /** Summary page "携带道具" hand-off, like {@code PartyView#bagHost}. */
    public void bagHost(java.util.function.Consumer<Pokemon> host) {
        this.bagHost = host;
    }

    public void dispose() {
        if (mosaicTexture != null) {
            mosaicTexture.dispose();
            mosaicTexture = null;
        }
    }

    // =====================================================================
    // PokemonStorageScene
    // =====================================================================

    /** :628-685 pbStartBox */
    private void sceneStartBox() {
        selection = 0;                                           // :641
        quickswap = false;                                       // :642
        choseFromParty = false;                                  // :644
        box = new BoxSprite(storage.currentBox);                 // :647
        boxparty = new PartySprite(party, screenHeightOrDefault() - 352f);   // :660
        if (command != 2) {                                      // :661 Drop down tab only on Deposit
            boxparty.x = 182;
            boxparty.y = screenHeightOrDefault();
            boxparty.refresh();                                  // :525-540 x= / y= move the icons with the tab
        }
        arrowGrabbed(null);
        grabbingState = 0;
        placingState = 0;
        holding = false;
        heldSprite = null;
        if (command != 2) {                                      // :674
            pbSetArrow(selection);                               // :675
            pbUpdateOverlay(selection, false);                   // :676
            pbSetMosaic(selection);                              // :677
        } else {
            pbPartySetArrow(selection);                          // :679
            pbUpdateOverlay(selection, true);                    // :680
            pbSetMosaic(selection);                              // :681
        }
        playSe("PC access", 100);                                // :683
        // :684 pbFadeInAndShow — 登记: 淡入未建模
    }

    private float screenHeightOrDefault() {
        return ScreenMetrics.logicalHeight();
    }

    /** :687-694 pbCloseBox (pbFadeOutAndHide — 登记: 淡出未建模) */
    private void sceneCloseBox() {
        finished = true;
    }

    /** :754-769 pbSetArrow */
    private void pbSetArrow(int sel) {
        float x, y;
        switch (sel) {
            case -1: case -4: case -5:                           // Box name, move left, move right
                x = 157 * 2 + 32;
                y = -12 * 2;
                break;
            case -2:                                             // Party Pokémon
                x = 119 * 2;
                y = 139 * 2 + 64;
                break;
            case -3:                                             // Close Box
                x = (207 + 64) * 2;
                y = 139 * 2 + 64;
                break;
            default:
                x = (97 + 24 * (sel % 6)) * 2;
                y = (8 + 24 * (sel / 6)) * 2;
                break;
        }
        setArrowXY(x, y);
    }

    /** :231-243 x= / y= : 手持图标跟着手(x, y+16)。 */
    private void setArrowXY(float x, float y) {
        arrowX = x;
        arrowY = y;
        if (holding && heldSprite != null) {
            heldSprite.x = x;
            heldSprite.y = y + 16f;
        }
    }

    /** :822-833 pbPartySetArrow */
    private void pbPartySetArrow(int sel) {
        if (sel >= 0) {
            int[] xvalues = {100, 136, 100, 136, 100, 136, 118};
            int[] yvalues = {1, 9, 33, 41, 65, 73, 110};
            setArrowXY(xvalues[sel] * 2, yvalues[sel] * 2 + 64);
        }
    }

    /** :1314-1325 pbMarkingSetArrow */
    private void pbMarkingSetArrow(int sel) {
        if (sel >= 0) {
            int[] xvalues = {162, 191, 220, 162, 191, 220, 190, 190};
            int[] yvalues = {24, 24, 24, 49, 49, 49, 77, 109};
            setArrowXY(xvalues[sel] * 2 + 32, yvalues[sel] * 2);
        }
    }

    /** :771-820 pbChangeSelection */
    private int pbChangeSelection(GameAction key, int sel) {
        switch (key) {
            case UP:
                if (sel == -1) {                                 // Box name
                    sel = -2;
                } else if (sel == -2) {                          // Party
                    sel = 25;
                } else if (sel == -3) {                          // Close Box
                    sel = 28;
                } else {
                    sel -= 6;
                    if (sel < 0) sel = -1;
                }
                break;
            case DOWN:
                if (sel == -1) {
                    sel = 2;
                } else if (sel == -2) {
                    sel = -1;
                } else if (sel == -3) {
                    sel = -1;
                } else {
                    sel += 6;
                    if (sel == 30 || sel == 31 || sel == 32) sel = -2;
                    if (sel == 33 || sel == 34 || sel == 35) sel = -3;
                }
                break;
            case LEFT:
                if (sel == -1) {
                    sel = -4;                                    // Move to previous box
                } else if (sel == -2) {
                    sel = -3;
                } else if (sel == -3) {
                    sel = -2;
                } else {
                    sel -= 1;
                    if (sel == -1 || Math.floorMod(sel, 6) == 5) sel += 6;
                }
                break;
            case RIGHT:
                if (sel == -1) {
                    sel = -5;                                    // Move to next box
                } else if (sel == -2) {
                    sel = -3;
                } else if (sel == -3) {
                    sel = -2;
                } else {
                    sel += 1;
                    if (sel % 6 == 0) sel -= 6;
                }
                break;
            default:
                break;
        }
        return sel;
    }

    /** :835-859 pbPartyChangeSelection */
    private int pbPartyChangeSelection(GameAction key, int sel) {
        switch (key) {
            case LEFT:
                sel -= 1;
                if (sel < 0) sel = 6;
                break;
            case RIGHT:
                sel += 1;
                if (sel > 6) sel = 0;
                break;
            case UP:
                if (sel == 6) {
                    sel = 5;
                } else {
                    sel -= 2;
                    if (sel < 0) sel = 6;
                }
                break;
            case DOWN:
                if (sel == 6) {
                    sel = 0;
                } else {
                    sel += 2;
                    if (sel > 6) sel = 6;
                }
                break;
            default:
                break;
        }
        return sel;
    }

    /** The key RGSS reads from the four repeat tests (:869-873): the last one set wins. */
    private GameAction repeatKey(InputManager input) {
        GameAction key = null;
        if (input.wasRepeated(GameAction.DOWN)) key = GameAction.DOWN;
        if (input.wasRepeated(GameAction.RIGHT)) key = GameAction.RIGHT;
        if (input.wasRepeated(GameAction.LEFT)) key = GameAction.LEFT;
        if (input.wasRepeated(GameAction.UP)) key = GameAction.UP;
        return key;
    }

    private boolean cancelPressed(InputManager input) {          // Input::B
        return input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU);
    }

    /** :861-933 pbSelectBoxInternal 的开头；循环体在 {@link #updateSelectBox}。 */
    private void pbSelectBoxInternal(Consumer<int[]> done) {
        selectDone = done;
        pbSetArrow(selection);                                   // :863
        pbUpdateOverlay(selection, false);                       // :864
        pbSetMosaic(selection);                                  // :865
        mode = Mode.SELECT_BOX;
    }

    private void updateSelectBox(InputManager input) {
        GameAction key = repeatKey(input);                       // :869-873
        if (key != null) {                                       // :874
            playCursorSe();                                      // :875
            selection = pbChangeSelection(key, selection);       // :876
            pbSetArrow(selection);                               // :877
            if (selection == -4) {                               // :878
                int nextbox = (storage.currentBox + storage.maxBoxes() - 1) % storage.maxBoxes();
                pbSwitchBoxToLeft(nextbox, () -> {
                    storage.currentBox = nextbox;                // :881
                    selection = -1;                              // :887
                    pbUpdateOverlay(selection, false);           // :888
                    pbSetMosaic(selection);                      // :889
                    mode = Mode.SELECT_BOX;
                });
                return;
            } else if (selection == -5) {                        // :882
                int nextbox = (storage.currentBox + 1) % storage.maxBoxes();
                pbSwitchBoxToRight(nextbox, () -> {
                    storage.currentBox = nextbox;                // :885
                    selection = -1;
                    pbUpdateOverlay(selection, false);
                    pbSetMosaic(selection);
                    mode = Mode.SELECT_BOX;
                });
                return;
            }
            pbUpdateOverlay(selection, false);                   // :888
            pbSetMosaic(selection);                              // :889
        }
        if (input.wasPressed(GameAction.SHOULDER_LEFT)) {        // :892 Input::L
            playCursorSe();
            int nextbox = (storage.currentBox + storage.maxBoxes() - 1) % storage.maxBoxes();
            pbSwitchBoxToLeft(nextbox, () -> {
                storage.currentBox = nextbox;
                pbUpdateOverlay(selection, false);
                pbSetMosaic(selection);
                mode = Mode.SELECT_BOX;
            });
        } else if (input.wasPressed(GameAction.SHOULDER_RIGHT)) {   // :899 Input::R
            playCursorSe();
            int nextbox = (storage.currentBox + 1) % storage.maxBoxes();
            pbSwitchBoxToRight(nextbox, () -> {
                storage.currentBox = nextbox;
                pbUpdateOverlay(selection, false);
                pbSetMosaic(selection);
                mode = Mode.SELECT_BOX;
            });
        } else if (input.wasPressed(GameAction.F5)) {            // :906 Jump to box name
            if (selection != -1) {
                playCursorSe();
                selection = -1;
                pbSetArrow(selection);
                pbUpdateOverlay(selection, false);
                pbSetMosaic(selection);
            }
        } else if (input.wasPressed(GameAction.SPECIAL) && command == 0) {   // :914 Organize only
            playDecisionSe();
            pbSetQuickSwap(!quickswap);
        } else if (cancelPressed(input)) {                       // :917
            finishSelectBox(null);
        } else if (input.wasPressed(GameAction.CONFIRM)) {       // :920
            if (selection >= 0) {
                finishSelectBox(new int[] {storage.currentBox, selection});
            } else if (selection == -1) {                        // Box name
                finishSelectBox(new int[] {-4, -1});
            } else if (selection == -2) {                        // Party Pokémon
                finishSelectBox(new int[] {-2, -1});
            } else if (selection == -3) {                        // Close Box
                finishSelectBox(new int[] {-3, -1});
            }
        }
    }

    private void finishSelectBox(int[] result) {
        Consumer<int[]> done = selectDone;
        selectDone = null;
        mode = Mode.IDLE;
        done.accept(result);
    }

    /** :935-961 pbSelectBox */
    private void pbSelectBox(Consumer<int[]> done) {
        if (command == 1) {                                      // Withdraw
            pbSelectBoxInternal(done);
            return;
        }
        selectBoxLoop(done);
    }

    private void selectBoxLoop(Consumer<int[]> done) {
        if (!choseFromParty) {
            pbSelectBoxInternal(ret -> afterSelectBox(ret, done));
        } else {
            afterSelectBox(null, done);
        }
    }

    private void afterSelectBox(int[] ret, Consumer<int[]> done) {
        if (choseFromParty || (ret != null && ret[0] == -2)) {   // :942 Party Pokémon
            Runnable chooseParty = () -> pbSelectPartyInternal(false, partyRet -> {   // :947
                if (partyRet < 0) {                              // :948
                    pbHidePartyTab(() -> {
                        selection = 0;                           // :950
                        choseFromParty = false;                  // :951
                        selectBoxLoop(done);
                    });
                } else {
                    choseFromParty = true;                       // :953
                    done.accept(new int[] {-1, partyRet});       // :954
                }
            });
            if (!choseFromParty) {                               // :943
                pbShowPartyTab(() -> {
                    selection = 0;                               // :945
                    chooseParty.run();
                });
            } else {
                chooseParty.run();
            }
        } else {
            choseFromParty = false;                              // :957
            done.accept(ret);                                    // :958
        }
    }

    /** :963-1009 pbSelectPartyInternal */
    private void pbSelectPartyInternal(boolean depositing, IntConsumer done) {
        selectPartyDone = done;
        selectPartyDepositing = depositing;
        pbPartySetArrow(selection);                              // :965
        pbUpdateOverlay(selection, true);                        // :966
        pbSetMosaic(selection);                                  // :967
        selectPartyLastsel = 1;                                  // :968
        mode = Mode.SELECT_PARTY;
    }

    private void updateSelectParty(InputManager input) {
        GameAction key = repeatKey(input);                       // :972-976
        if (key != null) {                                       // :977
            playCursorSe();
            int newselection = pbPartyChangeSelection(key, selection);   // :979
            if (newselection == -1) {                            // :980
                if (!selectPartyDepositing) {
                    finishSelectParty(-1);                       // :981
                    return;
                }
            } else if (newselection == -2) {                     // :982
                selection = selectPartyLastsel;                  // :983
            } else {
                selection = newselection;                        // :985
            }
            pbPartySetArrow(selection);                          // :987
            if (selection > 0) selectPartyLastsel = selection;   // :988
            pbUpdateOverlay(selection, true);                    // :989
            pbSetMosaic(selection);                              // :990
        }
        if (input.wasPressed(GameAction.SPECIAL) && command == 0) {   // :993 Organize only
            playDecisionSe();
            pbSetQuickSwap(!quickswap);
        } else if (cancelPressed(input)) {                       // :996
            finishSelectParty(-1);
        } else if (input.wasPressed(GameAction.CONFIRM)) {       // :999
            if (selection >= 0 && selection < 6) {               // :1000
                finishSelectParty(selection);
            } else if (selection == 6) {                         // Close Box
                finishSelectParty(selectPartyDepositing ? -3 : -1);   // :1005
            }
        }
    }

    private void finishSelectParty(int result) {
        // :997/:1001/:1004 @selection = selection (B returns the selection it stopped on)
        IntConsumer done = selectPartyDone;
        selectPartyDone = null;
        mode = Mode.IDLE;
        done.accept(result);
    }

    /** :1011-1013 pbSelectParty */
    private void pbSelectParty(IntConsumer done) {
        pbSelectPartyInternal(true, done);
    }

    /** :1015-1044 pbChangeBackground */
    private void pbChangeBackground(int wp, Runnable then) {
        box.refreshSprites = false;                              // :1016
        final int[] alpha = {0};
        final int timeTaken = 40 * 4 / 10;                       // :1020
        final int alphaDiff = (int) Math.ceil(255.0 / timeTaken);   // :1021
        final int[] phase = {0, 0};
        animate(() -> {
            if (phase[0] == 0) {                                 // :1022 timeTaken.times { alpha += alphaDiff }
                alpha[0] += alphaDiff;
                box.tintAlpha = Math.min(255, alpha[0]) / 255f;  // :1026
                if (++phase[1] >= timeTaken) {
                    phase[0] = 1;
                    phase[1] = 0;
                    storage.box(storage.currentBox).background = wp;   // :1029-1030
                }
            } else if (phase[0] == 1) {                          // :1031 (40/10).times { }
                if (++phase[1] >= 40 / 10) {
                    phase[0] = 2;
                    phase[1] = 0;
                }
            } else {                                             // :1036 timeTaken.times { alpha -= alphaDiff }
                alpha[0] -= alphaDiff;
                box.tintAlpha = Math.max(0, alpha[0]) / 255f;    // :1040
                if (++phase[1] >= timeTaken) {
                    box.refreshSprites = true;                   // :1043
                    return true;
                }
            }
            return false;
        }, then);
    }

    /** :1046-1066 pbSwitchBoxToRight */
    private void pbSwitchBoxToRight(int newbox, Runnable then) {
        newBox = new BoxSprite(newbox);                          // :1047
        newBox.x = 520;                                          // :1048
        newBox.refresh();
        final float distancePerFrame = 64 * 20 / 40;             // :1050
        animate(() -> {
            box.x -= distancePerFrame;                           // :1054
            newBox.x -= distancePerFrame;                        // :1055
            box.refresh();
            newBox.refresh();
            if (newBox.x <= BOX_X) {                             // :1058
                newBox.x = BOX_X;                                // :1062
                newBox.refresh();
                box = newBox;                                    // :1064-1065
                newBox = null;
                return true;
            }
            return false;
        }, then);
    }

    /** :1068-1088 pbSwitchBoxToLeft */
    private void pbSwitchBoxToLeft(int newbox, Runnable then) {
        newBox = new BoxSprite(newbox);                          // :1069
        newBox.x = -152;                                         // :1070
        newBox.refresh();
        final float distancePerFrame = 64 * 20 / 40;             // :1072
        animate(() -> {
            box.x += distancePerFrame;                           // :1076
            newBox.x += distancePerFrame;                        // :1077
            box.refresh();
            newBox.refresh();
            if (newBox.x >= BOX_X) {                             // :1080
                newBox.x = BOX_X;                                // :1084
                newBox.refresh();
                box = newBox;                                    // :1086-1087
                newBox = null;
                return true;
            }
            return false;
        }, then);
    }

    /** :1090-1099 pbJumpToBox */
    private void pbJumpToBox(int newbox, Runnable then) {
        if (storage.currentBox != newbox) {                      // :1091
            Runnable after = () -> {
                storage.currentBox = newbox;                     // :1097
                then.run();
            };
            if (newbox > storage.currentBox) {                   // :1092
                pbSwitchBoxToRight(newbox, after);               // :1093
            } else {
                pbSwitchBoxToLeft(newbox, after);                // :1095
            }
        } else {
            then.run();
        }
    }

    /** :1101-1109 pbSetMosaic */
    private void pbSetMosaic(int sel) {
        if (heldpkmn == null) {                                  // !@screen.pbHeldPokemon
            if (boxForMosaic != storage.currentBox || selectionForMosaic != sel) {
                mosaic = 40 / 4;                                 // :1104
                boxForMosaic = storage.currentBox;
                selectionForMosaic = sel;
            }
        }
    }

    /** :1111-1114 pbSetQuickSwap */
    private void pbSetQuickSwap(boolean value) {
        quickswap = value;
        arrowQuick = value;
    }

    /** :1116-1127 pbShowPartyTab */
    private void pbShowPartyTab(Runnable then) {
        playSe("GUI storage show party panel", 100);             // :1117
        final float distancePerFrame = 48 * 20 / 40;             // :1118
        final float target = screenHeightOrDefault() - 352f;
        animate(() -> {
            boxparty.y -= distancePerFrame;                      // :1122
            boxparty.refresh();
            if (boxparty.y <= target) {                          // :1124
                boxparty.y = target;                             // :1126
                boxparty.refresh();
                return true;
            }
            return false;
        }, then);
    }

    /** :1129-1140 pbHidePartyTab */
    private void pbHidePartyTab(Runnable then) {
        playSe("GUI storage hide party panel", 100);             // :1130
        final float distancePerFrame = 48 * 20 / 40;
        final float target = screenHeightOrDefault();
        animate(() -> {
            boxparty.y += distancePerFrame;                      // :1135
            boxparty.refresh();
            if (boxparty.y >= target) {                          // :1137
                boxparty.y = target;                             // :1139
                boxparty.refresh();
                return true;
            }
            return false;
        }, then);
    }

    /** :1142-1154 pbHold */
    private void pbHoldScene(int[] selected, Runnable then) {
        playSe("GUI storage pick up", 100);                      // :1143
        if (selected[0] == -1) {                                 // :1144
            boxparty.grabPokemon(selected[1]);                   // :1145
        } else {
            box.grabPokemon(selected[1]);                        // :1147
        }
        animate(() -> !grabbing(), then);                        // :1149 while @sprites["arrow"].grabbing?
    }

    /** :1156-1174 pbSwap */
    private void pbSwapScene(int[] selected) {
        playSe("GUI storage pick up", 100);                      // :1157
        Icon heldpokesprite = heldSprite;                        // :1158
        Icon boxpokesprite = selected[0] == -1 ? boxparty.getPokemon(selected[1]) : box.getPokemon(selected[1]);   // :1160-1164
        if (selected[0] == -1) {                                 // :1165
            boxparty.setPokemon(selected[1], heldpokesprite);    // :1166
        } else {
            box.setPokemon(selected[1], heldpokesprite);         // :1168
        }
        arrowSetSprite(boxpokesprite);                           // :1170
        mosaic = 10;                                             // :1171
        boxForMosaic = storage.currentBox;                       // :1172
        selectionForMosaic = selected[1];                        // :1173
    }

    /** :1176-1192 pbPlace */
    private void pbPlaceScene(int[] selected, Runnable then) {
        playSe("GUI storage put down", 100);                     // :1177
        final Icon heldpokesprite = heldSprite;                  // :1178
        floating = heldpokesprite;
        placingState = 1;                                        // :1179 arrow.place
        animate(() -> !placing(), () -> {                        // :1180
            floating = null;
            if (selected[0] == -1) {                             // :1185
                boxparty.setPokemon(selected[1], heldpokesprite);   // :1186
            } else {
                box.setPokemon(selected[1], heldpokesprite);     // :1188
            }
            boxForMosaic = storage.currentBox;                   // :1190
            selectionForMosaic = selected[1];                    // :1191
            then.run();
        });
    }

    /** :1194-1200 pbWithdraw */
    private void pbWithdrawScene(int[] selected, boolean heldpoke, int partyindex, Runnable then) {
        Runnable afterHold = () -> pbShowPartyTab(() -> {        // :1196
            pbPartySetArrow(partyindex);                         // :1197
            pbPlaceScene(new int[] {-1, partyindex}, () -> pbHidePartyTab(then));   // :1198-1199
        });
        if (!heldpoke) {
            pbHoldScene(selected, afterHold);                    // :1195
        } else {
            afterHold.run();
        }
    }

    /** :1202-1220 pbStore */
    private void pbStoreScene(int[] selected, boolean heldpoke, int destbox, int firstfree) {
        if (heldpoke) {                                          // :1203
            if (destbox == storage.currentBox) {                 // :1204
                Icon heldpokesprite = heldSprite;                // :1205
                box.setPokemon(firstfree, heldpokesprite);       // :1206
                arrowSetSprite(null);                            // :1207
            } else {
                arrowDeleteSprite();                             // :1209
            }
        } else {
            Icon sprite = boxparty.getPokemon(selected[1]);      // :1212
            if (destbox == storage.currentBox) {                 // :1213
                box.setPokemon(firstfree, sprite);               // :1214
                boxparty.setPokemon(selected[1], null);          // :1215
            } else {
                boxparty.deletePokemon(selected[1]);             // :1217
            }
        }
    }

    /** :1222-1240 pbRelease */
    private void pbReleaseScene(int[] selected, boolean heldpoke, Runnable then) {
        int index = selected[1];
        Icon sprite;
        if (heldpoke) {                                          // :1225
            sprite = heldSprite;
        } else if (selected[0] == -1) {                          // :1227
            sprite = boxparty.getPokemon(index);
        } else {
            sprite = box.getPokemon(index);
        }
        if (sprite != null) {                                    // :1232
            sprite.release();                                    // :1233
            animate(() -> !sprite.releasing(), then);            // :1234
        } else {
            then.run();
        }
    }

    /** :1242-1262 pbReleaseAll: the box loop, one pokemon at a time (a shiny asks first). */
    private void pbReleaseAllScene(int boxNumber, int startIndex, int count, IntConsumer done) {
        int index = startIndex;
        Storage.Box current = storage.box(boxNumber);
        while (index < current.length()) {                       // :1244
            Pokemon pkmn = current.get(index);                   // :1245
            if (pkmn == null) { index++; continue; }             // :1246
            if (isSpecialSpecies(pkmn)) { index++; continue; }   // :1247-1249
            if (pkmn.shiny) {                                    // :1250
                final int at = index;
                final int counted = count;
                pbShowCommands(intl("真的确定要提交{1}吗？", pkmn.name), java.util.Arrays.asList("是", "否"), 0, choice -> {
                    if (choice != 0) {
                        pbReleaseAllScene(boxNumber, at + 1, counted, done);       // :1251 next
                    } else {
                        releaseOne(boxNumber, at);
                        pbReleaseAllScene(boxNumber, at + 1, counted + (box.getPokemon(at) != null ? 1 : 0), done);
                    }
                });
                return;
            }
            Icon sprite = box.getPokemon(index);                 // :1253
            if (sprite != null) {                                // :1254
                sprite.releaseAll();                             // :1255
                storage.pbDelete(boxNumber, index);              // :1256
                count += 1;                                      // :1257
            }
            index++;
        }
        done.accept(count);                                      // :1261
    }

    private void releaseOne(int boxNumber, int index) {          // :1253-1258 for the shiny branch
        Icon sprite = box.getPokemon(index);
        if (sprite != null) {
            sprite.releaseAll();
            storage.pbDelete(boxNumber, index);
        }
    }

    /** :1247-1249 / :2014-2016 */
    private static boolean isSpecialSpecies(Pokemon pkmn) {
        if (pkmn.species == null) return false;
        String name = pkmn.species.internalName;
        return "SUGARDEVOIR".equals(name) || "SUJINRAKU".equals(name) || "VALKYRIE".equals(name)
                || "BLACKKNIGHT".equals(name) || "CHALLEN".equals(name);
    }

    /** :1264-1273 pbChooseBox */
    private void pbChooseBox(String msg, IntConsumer done) {
        List<String> commands = new ArrayList<>();
        for (int i = 0; i < storage.maxBoxes(); i++) {           // :1266
            Storage.Box b = storage.box(i);
            commands.add(intl("{1} ({2}/{3})", b.name, b.nitems(), b.length()));   // :1269
        }
        pbShowCommands(msg, commands, storage.currentBox, done);   // :1272
    }

    /** :1275-1284 pbBoxName — 登记: pbEnterBoxName(文字输入)未建模，空实现。 */
    private void pbBoxName(String helptext, int minchars, int maxchars, Runnable then) {
        enterText(helptext, minchars, maxchars, "", ret -> {     // :1277 pbEnterBoxName (:1276 fades the sprites out)
            if (ret.length() > 0) {                              // :1278
                storage.box(storage.currentBox).name = ret;      // :1279
            }
            box = new BoxSprite(storage.currentBox);             // :1281 @sprites["box"].refreshBox = true
            then.run();                                          // :1282 pbRefresh
        });
    }

    private TextEntryView textEntry;
    private Consumer<String> textDone;

    /** {@code pbEnterBoxName / pbEnterPokemonName}: the text entry scene over the storage screen. 登记: no subject sprite. */
    private void enterText(String helptext, int min, int max, String initial, Consumer<String> done) {
        textEntry = new TextEntryView(context, helptext, min, max, initial, null);
        textDone = done;
        mode = Mode.TEXT;
    }

    /** :1286-1294 pbChooseItem */
    private void pbChooseItemScene(Consumer<String> done) {
        if (itemHost == null) {                                  // 登记: 背包宿主未接
            done.accept(null);
            return;
        }
        mode = Mode.BAG;
        itemHost.accept(this::canHoldItem, picked -> {
            mode = Mode.IDLE;
            done.accept(picked);
        });
    }

    /** 他段: PItem_Items:140-151 pbCanHoldItem? = !pbIsImportantItem? (INFINITE_TMS = true, Settings:70) */
    private boolean canHoldItem(String id) {
        PbsData.Item item = context.pbsData() == null ? null : context.pbsData().item(id);
        if (item == null) return false;
        if (item.type == 6) return false;                        // Key item
        if (item.fieldUse == 4) return false;                    // HM
        return item.fieldUse != 3;                               // TM (INFINITE_TMS)
    }

    /** :1296-1312 pbSummary */
    private void pbSummaryScene(int[] selected, Pokemon heldpoke, Runnable then) {
        // :1297 pbFadeOutAndHide — 登记: 淡出未建模
        com.badlogic.gdx.utils.Array<Pokemon> list = new com.badlogic.gdx.utils.Array<>();
        int index = 0;
        summarySlots = null;
        summaryParty = false;
        if (heldpoke != null) {                                  // :1300
            list.add(heldpoke);                                  // :1301
        } else if (selected[0] == -1) {                          // :1302
            for (Pokemon p : party.members()) list.add(p);
            index = selected[1];
            summaryParty = true;
        } else {                                                 // :1306
            // 登记: PokemonSummary 接收的是带 nil 槽的盒子；这里压成紧凑数组并记下槽位映射。
            Storage.Box b = storage.box(selected[0]);
            summarySlots = new int[b.length()];
            int n = 0;
            for (int i = 0; i < b.length(); i++) {
                if (b.get(i) != null) {
                    if (i == selected[1]) index = n;
                    summarySlots[n++] = i;
                    list.add(b.get(i));
                }
            }
            summarySelectedBox = selected[0];
        }
        summary = new SummaryView(context, list, index, bagHost);
        final boolean fromParty = summaryParty;
        final boolean fromHeld = heldpoke != null;
        summaryDone = () -> {
            if (!fromHeld) {
                int endIndex = summary.index();
                if (fromParty) {                                 // :1303-1305
                    selection = endIndex;
                    pbPartySetArrow(selection);
                    pbUpdateOverlay(selection, true);
                } else {                                         // :1307-1309
                    selection = summarySlots != null && endIndex < summarySlots.length ? summarySlots[endIndex] : selection;
                    pbSetArrow(selection);
                    pbUpdateOverlay(selection, false);
                }
            }
            summary = null;
            then.run();                                          // :1311 pbFadeInAndShow
        };
        mode = Mode.SUMMARY;
    }

    /** :1327-1353 pbMarkingChangeSelection */
    private int pbMarkingChangeSelection(GameAction key, int sel) {
        switch (key) {
            case LEFT:
                if (sel < 6) {
                    sel -= 1;
                    if (Math.floorMod(sel, 3) == 2) sel += 3;
                }
                break;
            case RIGHT:
                if (sel < 6) {
                    sel += 1;
                    if (sel % 3 == 0) sel -= 3;
                }
                break;
            case UP:
                if (sel == 7) sel = 6;
                else if (sel == 6) sel = 4;
                else if (sel < 3) sel = 7;
                else sel -= 3;
                break;
            case DOWN:
                if (sel == 7) sel = 1;
                else if (sel == 6) sel = 7;
                else if (sel >= 3) sel = 6;
                else sel += 3;
                break;
            default:
                break;
        }
        return sel;
    }

    /** :1355-1435 pbMark (开头；循环体 {@link #updateMarking}) */
    private void pbMarkScene(int[] selected, Pokemon heldpoke, Runnable then) {
        markingVisible = true;                                   // :1356-1357
        Pokemon pokemon;
        if (heldpoke != null) {                                  // :1369
            pokemon = heldpoke;
        } else if (selected[0] == -1) {                          // :1371
            pokemon = party.get(selected[1]);
        } else {
            pokemon = storage.get(selected[0], selected[1]);     // :1374
        }
        markingPokemon = pokemon;
        markingValue = pokemon.markings;                         // :1376
        markingIndex = 0;                                        // :1377
        pbMarkingSetArrow(markingIndex);                         // :1394
        markingDone = then;
        mode = Mode.MARKING;
    }

    private void updateMarking(InputManager input) {
        GameAction key = repeatKey(input);                       // :1399-1403
        if (key != null) {                                       // :1404
            int oldindex = markingIndex;
            markingIndex = pbMarkingChangeSelection(key, markingIndex);   // :1406
            if (markingIndex != oldindex) playCursorSe();        // :1407
            pbMarkingSetArrow(markingIndex);                     // :1408
        }
        if (cancelPressed(input)) {                              // :1411
            playSe("GUI sel cancel", 80);                        // :1412 pbPlayCancelSE
            endMarking();
        } else if (input.wasPressed(GameAction.CONFIRM)) {       // :1414
            playDecisionSe();                                    // :1415
            if (markingIndex == 6) {                             // OK
                markingPokemon.markings = markingValue;          // :1417
                endMarking();
            } else if (markingIndex == 7) {                      // Cancel
                endMarking();
            } else {
                int mask = 1 << markingIndex;                    // :1422
                if ((markingValue & mask) == 0) {
                    markingValue |= mask;                        // :1424
                } else {
                    markingValue &= ~mask;                       // :1426
                }
            }
        }
    }

    private void endMarking() {
        markingVisible = false;                                  // :1432-1433
        Runnable then = markingDone;
        markingDone = null;
        mode = Mode.IDLE;
        then.run();
    }

    /** :1437-1440 pbRefresh / :1442-1449 pbHardRefresh */
    private void pbRefresh() {
        box.refresh();
        boxparty.refresh();
    }

    private void pbHardRefresh() {
        float oldPartyY = boxparty.y;                            // :1443
        box = new BoxSprite(storage.currentBox);                 // :1445
        boxparty = new PartySprite(party, oldPartyY);            // :1447-1448
    }

    /** :1460-1604 pbUpdateOverlay：选中项的信息面板内容(绘制在 {@link #renderOverlay})。 */
    private void pbUpdateOverlay(int sel, boolean partyFlag) {
        overlayPartyCount = party.size();                        // :1468 (@storage.party.length)
        Pokemon pokemon = null;
        if (heldpkmn != null) {                                  // :1472
            pokemon = heldpkmn;
        } else if (sel >= 0) {                                   // :1474
            pokemon = partyFlag ? party.get(sel) : storage.get(storage.currentBox, sel);
        }
        overlayPokemon = pokemon;
        overlayHasPokemon = pokemon != null;
        if (pokemon != null) {                                   // :1603 @sprites["pokemon"].setPokemonBitmap(pokemon)
            mosaicPokemon = pokemon;
        }
    }

    // ---- 手形光标 PokemonBoxArrow (:167-333)

    private boolean grabbing() {                                 // :223
        return grabbingState > 0;
    }

    private boolean placing() {                                  // :227
        return placingState > 0;
    }

    private boolean holding() {                                  // :219
        return heldSprite != null && holding;
    }

    private void arrowGrab(Icon sprite) {                        // :268-274
        grabbingState = 1;
        heldSprite = sprite;
    }

    private void arrowGrabbed(Icon sprite) {
        heldSprite = sprite;
    }

    /** :250-258 setSprite */
    private void arrowSetSprite(Icon sprite) {
        if (holding()) {
            heldSprite = sprite;
            if (sprite == null) holding = false;
        }
    }

    /** :260-266 deleteSprite */
    private void arrowDeleteSprite() {
        holding = false;
        if (heldSprite != null) {
            heldSprite.disposed = true;
            heldSprite = null;
        }
    }

    /** :284-332 PokemonBoxArrow#update (once per 40fps tick) */
    private void arrowUpdate() {
        if (heldSprite != null && heldSprite.disposed) heldSprite = null;   // heldPokemon
        if (heldSprite != null) heldSprite.update();             // :288
        if (heldSprite == null) holding = false;                 // :290
        String suffix = arrowQuick ? "q" : "";
        if (grabbingState > 0) {                                 // :291
            if (grabbingState <= 4 * 40 / 20) {                  // :292
                handImage = "cursor_grab" + (arrowQuick ? "_q" : "");
                arrowYOffset = 4.0f * grabbingState * 20 / 40;   // :294
                grabbingState += 1;
            } else if (grabbingState <= 8 * 40 / 20) {           // :296
                holding = true;
                handImage = "cursor_fist" + (arrowQuick ? "_q" : "");
                arrowYOffset = 4 * (8 * 40 / 20 - grabbingState) * 20 / 40;   // :299
                grabbingState += 1;
                followHeld();
            } else {
                grabbingState = 0;                               // :302
            }
        } else if (placingState > 0) {                           // :304
            if (placingState <= 4 * 40 / 20) {                   // :305
                handImage = "cursor_fist" + (arrowQuick ? "_q" : "");
                arrowYOffset = 4.0f * placingState * 20 / 40;    // :307
                placingState += 1;
                followHeld();
            } else if (placingState <= 8 * 40 / 20) {            // :309
                holding = false;                                 // :310
                heldSprite = null;                               // :311
                handImage = "cursor_grab" + (arrowQuick ? "_q" : "");
                arrowYOffset = 4 * (8 * 40 / 20 - placingState) * 20 / 40;   // :313
                placingState += 1;
            } else {
                placingState = 0;                                // :316
            }
        } else if (holding()) {                                  // :318
            handImage = "cursor_fist" + (arrowQuick ? "_q" : "");
        } else {
            arrowYOffset = 0f;                                   // :321-322 x=@spriteX / y=@spriteY
            if (arrowFrame < 40 / 2) {                           // :323
                handImage = "cursor_point_1" + (arrowQuick ? "_q" : "");
            } else {
                handImage = "cursor_point_2" + (arrowQuick ? "_q" : "");
            }
        }
        arrowFrame += 1;                                         // :329
        if (arrowFrame >= 40) arrowFrame = 0;                    // :330
    }

    /** :242 {@code heldPokemon.y = self.y+16 if holding?} after {@code self.y=} in update. */
    private void followHeld() {
        if (holding() && heldSprite != null) {
            heldSprite.y = arrowY + arrowYOffset + 16f;
        }
    }

    /** pbUpdateSpriteHash(@sprites): every sprite's update (:1606-1608). */
    private void sceneTick() {
        if (box != null) box.update();
        if (newBox != null) newBox.update();
        if (boxparty != null) boxparty.update();
        arrowUpdate();
        if (mosaic > 0) mosaic -= 1;                             // AutoMosaicPokemonSprite#update :158 (clamped by mosaic=)
    }

    // =====================================================================
    // 阻塞调用点: pbShowCommands / pbDisplay / 动画
    // =====================================================================

    private void animate(BooleanSupplier step, Runnable then) {
        animStep = step;
        animDone = then;
        mode = Mode.ANIM;
    }

    /** :717-752 pbShowCommands */
    private void pbShowCommands(String message, List<String> commands, int index, IntConsumer done) {
        cmdMessage = message;
        cmdLabels = commands;
        cmdIndex = index;                                        // :733
        cmdTop = 0;
        cmdDone = done;
        mode = Mode.COMMANDS;
    }

    private void updateCommands(InputManager input) {
        int n = cmdLabels.size();
        if (input.wasRepeated(GameAction.DOWN)) {                // SpriteWindow_SelectableEx 移动(列数 1)
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
        if (cancelPressed(input)) {                              // :739
            finishCommands(-1);                                  // :740
        } else if (input.wasPressed(GameAction.CONFIRM)) {       // :742
            finishCommands(cmdIndex);                            // :743
        }
    }

    private void finishCommands(int ret) {
        IntConsumer done = cmdDone;
        cmdDone = null;
        mode = Mode.IDLE;
        done.accept(ret);
    }

    /** :696-715 pbDisplay */
    private void pbDisplay(String message, Runnable then) {
        displayText = message;
        displayDone = then;
        mode = Mode.DISPLAY;
    }

    private void updateDisplay(InputManager input) {
        if (cancelPressed(input) || input.wasPressed(GameAction.CONFIRM)) {   // :707
            Runnable then = displayDone;
            displayDone = null;
            mode = Mode.IDLE;
            then.run();
        }
    }

    // =====================================================================
    // PokemonStorageScreen
    // =====================================================================

    private void startScreen() {
        heldpkmn = null;                                         // :1627
        screenH = ScreenMetrics.logicalHeight();
        screenW = ScreenMetrics.logicalWidth();
        sceneStartBox();                                         // :1630/:1712/:1774/:1827
        if (chooseEgg) {
            eggChooseLoop();                                     // 306:2273 pbChooseEggToHatch
            return;
        }
        switch (command) {
            case 0: organiseLoop(); break;                       // ORGANISE
            case 1: withdrawLoop(); break;                       // WITHDRAW
            case 2: depositLoop(); break;                        // DEPOSIT
            default: sceneCloseBox(); break;                     // :1826-1829
        }
    }

    /** :1631-1708 ORGANISE 循环 */
    private void organiseLoop() {
        pbSelectBox(selected -> {                                // :1632
            if (selected == null) {                              // :1633
                if (heldpkmn != null) {                          // :1634
                    pbDisplay("正拿着宝可梦！", this::organiseLoop);   // :1635-1636
                    return;
                }
                pbConfirm("要继续操作储存系统吗？", yes -> {     // :1638
                    if (yes) organiseLoop(); else sceneCloseBox();
                });
            } else if (selected[0] == -3) {                      // :1640 Close box
                if (heldpkmn != null) {
                    pbDisplay("正拿着宝可梦！", this::organiseLoop);
                    return;
                }
                pbConfirm("退出？", yes -> {                     // :1645
                    if (yes) {
                        playSe("PC close", 100);                 // :1646
                        sceneCloseBox();
                    } else {
                        organiseLoop();
                    }
                });
            } else if (selected[0] == -4) {                      // :1650 Box name
                pbBoxCommands(this::organiseLoop);               // :1651
            } else {
                Pokemon pokemon = storage.get(selected[0], selected[1]);   // :1653
                Pokemon heldpoke = heldpkmn;                     // :1654
                if (pokemon == null && heldpoke == null) {       // :1655
                    organiseLoop();
                    return;
                }
                if (quickswap) {                                 // :1656
                    if (heldpkmn != null) {                      // :1657
                        if (pokemon != null) pbSwap(selected, this::organiseLoop);
                        else pbPlace(selected, this::organiseLoop);
                    } else {
                        pbHold(selected, this::organiseLoop);    // :1660
                    }
                    return;
                }
                List<String> commands = new ArrayList<>();       // :1663
                int cmdMove = -1, cmdSummary, cmdWithdraw, cmdItem, cmdMark, cmdRelease;
                String helptext = "";
                if (heldpoke != null) {                          // :1672
                    helptext = intl("已选择{1}。", heldpoke.name);   // :1673
                    cmdMove = add(commands, pokemon != null ? "交换" : "放置");   // :1674
                } else if (pokemon != null) {                    // :1675
                    helptext = intl("已选择{1}。", pokemon.name);   // :1676
                    cmdMove = add(commands, "移动");              // :1677
                }
                cmdSummary = add(commands, "概况");                                  // :1679
                cmdWithdraw = add(commands, selected[0] == -1 ? "存放" : "取出");     // :1680
                cmdItem = add(commands, "道具");                                     // :1681
                cmdMark = add(commands, "标记");                                     // :1682
                cmdRelease = add(commands, "提交");                                  // :1683
                // :1684 调试 — 登记: $DEBUG 未建模，恒为 false，不加入命令表
                add(commands, "取消");                                               // :1685
                final int fMove = cmdMove, fSummary = cmdSummary, fWithdraw = cmdWithdraw, fItem = cmdItem,
                        fMark = cmdMark, fRelease = cmdRelease;
                pbShowCommands(helptext, commands, 0, cmd -> {   // :1686
                    Runnable next = this::organiseLoop;
                    if (fMove >= 0 && cmd == fMove) {            // :1687 Move/Shift/Place
                        if (heldpkmn != null) {                  // :1688
                            if (pokemon != null) pbSwap(selected, next); else pbPlace(selected, next);
                        } else {
                            pbHold(selected, next);              // :1691
                        }
                    } else if (fSummary >= 0 && cmd == fSummary) {   // :1693
                        pbSummary(selected, heldpkmn, next);
                    } else if (fWithdraw >= 0 && cmd == fWithdraw) {   // :1695
                        if (selected[0] == -1) pbStore(selected, heldpkmn, next);
                        else pbWithdraw(selected, heldpkmn, next);
                    } else if (fItem >= 0 && cmd == fItem) {     // :1697
                        pbItem(selected, heldpkmn, next);
                    } else if (fMark >= 0 && cmd == fMark) {     // :1699
                        pbMark(selected, heldpkmn, next);
                    } else if (fRelease >= 0 && cmd == fRelease) {   // :1701
                        pbRelease(selected, heldpkmn, next);
                    } else {
                        next.run();
                    }
                });
            }
        });
    }

    /** 306_B2W2_PC:2273-2316 pbChooseEggToHatch: choose a Pokemon egg of the boxes. */
    private void eggChooseLoop() {
        pbSelectBox(selected -> {                                // :2278
            if (selected != null && selected[0] == -3) {         // :2279 Close box
                pbConfirm("要退出寄放系统吗？", yes -> {          // :2280
                    if (yes) {
                        playSe("PC close", 100);                 // :2281
                        sceneCloseBox();                         // :2315 pbCloseBox
                    } else {
                        eggChooseLoop();                         // :2284 next
                    }
                });
                return;
            }
            if (selected == null) {                              // :2286
                pbConfirm("要继续操作盒子吗？", yes -> {          // :2287
                    if (yes) eggChooseLoop(); else sceneCloseBox();
                });
                return;
            }
            if (selected[0] == -4) {                             // :2289 Box name
                pbBoxCommands(this::eggChooseLoop);              // :2290
                return;
            }
            Pokemon pokemon = selected[0] < 0 ? null : storage.get(selected[0], selected[1]);   // :2292
            if (pokemon == null) {                               // :2293
                eggChooseLoop();
                return;
            }
            if (!pokemon.egg) {                                  // :2294
                pbDisplay("这不是一颗宝可梦蛋。", this::eggChooseLoop);   // :2295-2296
                return;
            }
            pbShowCommands(intl("已选择{1}。", pokemon.name),     // :2298-2304
                    java.util.Arrays.asList("选择", "概况", "取消"), 0, cmd -> {
                if (cmd == 0) {                                  // :2306 Select
                    eggChoice = selected;                        // :2308 retval = selected
                    sceneCloseBox();
                } else if (cmd == 1) {
                    pbSummary(selected, null, this::eggChooseLoop);   // :2311
                } else {
                    eggChooseLoop();
                }
            });
        });
    }

    /** :1713-1770 WITHDRAW 循环 */
    private void withdrawLoop() {
        pbSelectBox(selected -> {                                // :1714
            if (selected == null) {                              // :1715
                pbConfirm("要继续操作盒子吗？", yes -> {         // :1716
                    if (yes) withdrawLoop(); else sceneCloseBox();
                });
                return;
            }
            switch (selected[0]) {                               // :1719
                case -2:                                         // Party Pokémon
                    pbDisplay("要取出哪只宝可梦？", this::withdrawLoop);   // :1721-1722
                    return;
                case -3:                                         // Close box
                    pbConfirm("要退出宝可梦寄放系统吗？", yes -> {   // :1724
                        if (yes) {
                            playSe("PC close", 100);             // :1725
                            sceneCloseBox();
                        } else {
                            withdrawLoop();
                        }
                    });
                    return;
                case -4:                                         // Box name
                    pbBoxCommands(this::withdrawLoop);           // :1730
                    return;
                default:
                    break;
            }
            Pokemon pokemon = storage.get(selected[0], selected[1]);   // :1733
            if (pokemon == null) {                               // :1734
                withdrawLoop();
                return;
            }
            pbShowCommands(intl("已选择{1}。", pokemon.name),     // :1735
                    java.util.Arrays.asList("取出", "概况", "昵称", "标记", "提交", "取消"), 0, cmd -> {
                Runnable next = this::withdrawLoop;
                switch (cmd) {
                    case 0: pbWithdraw(selected, null, next); break;       // :1744
                    case 1: pbSummary(selected, null, next); break;        // :1745
                    case 2: nicknameCommand(pokemon, selected, next); break;   // :1746-1765
                    case 3: pbMark(selected, null, next); break;           // :1766
                    case 4: pbRelease(selected, null, next); break;        // :1767
                    default: next.run(); break;
                }
            });
        });
    }

    /** :1773-1824 DEPOSIT 循环 */
    private void depositLoop() {
        pbSelectParty(selected -> {                              // :1776
            if (selected == -3) {                                // :1777 Close box
                pbConfirm("要退出宝可梦寄放系统吗？", yes -> {   // :1778
                    if (yes) {
                        playSe("PC close", 100);                 // :1779
                        sceneCloseBox();
                    } else {
                        depositLoop();
                    }
                });
            } else if (selected < 0) {                           // :1783
                pbConfirm("要继续操作盒子吗？", yes -> {         // :1784
                    if (yes) depositLoop(); else sceneCloseBox();
                });
            } else {
                Pokemon pokemon = storage.get(-1, selected);     // :1787
                if (pokemon == null) {                           // :1788
                    depositLoop();
                    return;
                }
                pbShowCommands(intl("已选择{1}。", pokemon.name), // :1789
                        java.util.Arrays.asList("存放", "概况", "昵称", "标记", "提交", "取消"), 0, cmd -> {
                    Runnable next = this::depositLoop;
                    int[] sel = {-1, selected};
                    switch (cmd) {
                        case 0: pbStore(sel, null, next); break;           // :1798
                        case 1: pbSummary(sel, null, next); break;         // :1799
                        case 2: nicknameCommand(pokemon, sel, next); break;   // :1800-1819
                        case 3: pbMark(sel, null, next); break;            // :1820
                        case 4: pbRelease(sel, null, next); break;         // :1821
                        default: next.run(); break;
                    }
                });
            }
        });
    }

    /**
     * :1746-1765 / :1800-1819 昵称。原文写了 {@code return}(:1749/:1753/:1803/:1807)，会直接退出
     * pbStartScreen 而不 pbCloseBox —— 插件缺陷，照抄: 这里直接结束视图。
     * :1761/:1815 {@code pbRefreshSingle(pkmnid)} 引用未定义变量(插件缺陷)，因 pbEnterPokemonName 未建模，
     * newname 恒为 nil，不会走到。
     */
    private void nicknameCommand(Pokemon pokemon, int[] selected, Runnable next) {
        if (trainer.id != pokemon.trainerID) {                   // :1747
            pbDisplay("无法修改原主人的宝可梦的昵称。", () -> finished = true);   // :1748-1749 return
            return;
        }
        if (pokemon.egg) {                                       // :1751
            pbDisplay("无法修改宝可梦蛋的昵称。", () -> finished = true);       // :1752-1753 return
            return;
        }
        String speciesname = pokemon.species == null ? "" : pokemon.species.name;        // :1755
        String oldname = pokemon.name != null && !pokemon.name.equals(speciesname) ? pokemon.name : "";   // :1756
        enterText("请输入昵称", 0, 10, oldname, newname -> {                  // :1757-1758 MAX_POKEMON_NAME_SIZE = 10
            if (!newname.isEmpty()) {                            // :1759
                pokemon.name = newname;                          // :1760
            } else {
                pokemon.name = speciesname;                      // :1763 (the plugin's pbRefreshSingle here is an undefined variable)
            }
            next.run();
        });
    }

    /** :1849-1851 pbConfirm = pbShowCommands(str,[是,否])==0 */
    private void pbConfirm(String str, Consumer<Boolean> done) {
        pbShowCommands(str, java.util.Arrays.asList("是", "否"), 0, c -> done.accept(c == 0));
    }

    /** :1857-1859 */
    private static boolean pbAble(Pokemon pokemon) {
        return pokemon != null && !pokemon.egg && pokemon.hp > 0;
    }

    /** :1861-1867 */
    private int pbAbleCount() {
        int count = 0;
        for (Pokemon p : party.members()) if (pbAble(p)) count += 1;
        return count;
    }

    /** 登记: 邮件未建模，{@code pkmn.mail} 恒为 nil。 */
    private static boolean hasMail(Pokemon p) {
        return false;
    }

    /** :1873-1892 pbWithdraw */
    private void pbWithdraw(int[] selected, Pokemon heldpoke, Runnable then) {
        int box = selected[0], index = selected[1];
        if (box == -1) throw new IllegalStateException("不能取出宝可梦……");   // :1877
        if (party.size() >= 6) {                                 // :1879
            pbDisplay("你的队伍已经满了！", then);               // :1880
            return;
        }
        pbWithdrawScene(selected, heldpoke != null, party.size(), () -> {   // :1883
            if (heldpoke != null) {                              // :1884
                storage.pbMoveCaughtToParty(heldpoke);           // :1885
                heldpkmn = null;                                 // :1886
            } else {
                storage.pbMove(-1, -1, box, index);              // :1888
            }
            pbRefresh();                                         // :1890
            then.run();
        });
    }

    /** :1894-1934 pbStore */
    private void pbStore(int[] selected, Pokemon heldpoke, Runnable then) {
        int box = selected[0], index = selected[1];
        if (box != -1) throw new IllegalStateException("不能储存宝可梦……");     // :1898
        if (pbAbleCount() <= 1 && pbAble(storage.get(box, index)) && heldpoke == null) {   // :1900
            playBuzzerSe();                                      // :1901
            pbDisplay("这是最后的宝可梦了！", then);             // :1902
        } else if (heldpoke != null && hasMail(heldpoke)) {      // :1903
            pbDisplay("请先取出宝可梦的邮件。", then);           // :1904
        } else if (heldpoke == null && hasMail(storage.get(box, index))) {   // :1905
            pbDisplay("请先取出宝可梦的邮件。", then);           // :1906
        } else {
            storeLoop(selected, heldpoke, then);
        }
    }

    private void storeLoop(int[] selected, Pokemon heldpoke, Runnable then) {
        int index = selected[1];
        pbChooseBox("储存到哪个盒子？", destbox -> {              // :1909
            if (destbox >= 0) {                                  // :1910
                int firstfree = storage.pbFirstFreePos(destbox); // :1911
                if (firstfree < 0) {                             // :1912
                    pbDisplay("盒子已经满了……", () -> storeLoop(selected, heldpoke, then));   // :1913-1914 next
                    return;
                }
                if (heldpoke != null || selected[0] == -1) {     // :1916
                    Pokemon p = heldpoke != null ? heldpoke : storage.get(-1, index);   // :1917
                    Storage.resetForm(p);                        // :1918-1919 (formTime 未建模)
                    Storage.heal(p);                             // :1920
                }
                pbStoreScene(selected, heldpoke != null, destbox, firstfree);   // :1922
                if (heldpoke != null) {                          // :1923
                    storage.pbMoveCaughtToBox(heldpoke, destbox);   // :1924
                    heldpkmn = null;                             // :1925
                } else {
                    storage.pbMove(destbox, -1, -1, index);      // :1927
                }
            }
            pbRefresh();                                         // :1932
            then.run();
        });
    }

    /** :1936-1948 pbHold */
    private void pbHold(int[] selected, Runnable then) {
        int box = selected[0], index = selected[1];
        if (box == -1 && pbAble(storage.get(box, index)) && pbAbleCount() <= 1) {   // :1939
            playBuzzerSe();                                      // :1940
            pbDisplay("这是最后的宝可梦了！", then);             // :1941
            return;
        }
        pbHoldScene(selected, () -> {                            // :1944
            heldpkmn = storage.get(box, index);                  // :1945
            storage.pbDelete(box, index);                        // :1946
            pbRefresh();                                         // :1947
            then.run();
        });
    }

    /** :1950-1976 pbPlace */
    private void pbPlace(int[] selected, Runnable then) {
        int box = selected[0], index = selected[1];
        if (storage.get(box, index) != null) {                   // :1953
            throw new IllegalStateException("位置" + box + "，" + index + "不为空……");
        }
        if (box != -1 && index >= storage.maxPokemon(box)) {     // :1956
            pbDisplay("Can't place that there.", then);          // :1957 (原文英文)
            return;
        }
        if (box != -1 && hasMail(heldpkmn)) {                    // :1960
            pbDisplay("Please remove the mail.", then);          // :1961 (原文英文)
            return;
        }
        if (box >= 0) {                                          // :1964
            Storage.resetForm(heldpkmn);                         // :1965-1966 (formTime 未建模)
        }
        pbPlaceScene(selected, () -> {                           // :1969
            storage.set(box, index, heldpkmn);                   // :1970 (party.compact! 由 Storage#set 完成)
            pbRefresh();                                         // :1974
            heldpkmn = null;                                     // :1975
            then.run();
        });
    }

    /** :1978-2004 pbSwap */
    private void pbSwap(int[] selected, Runnable then) {
        int box = selected[0], index = selected[1];
        if (storage.get(box, index) == null) {                   // :1981
            throw new IllegalStateException("位置" + box + "，" + index + "为空……");
        }
        if (box == -1 && pbAble(storage.get(box, index)) && pbAbleCount() <= 1 && !pbAble(heldpkmn)) {   // :1984
            playBuzzerSe();                                      // :1985
            pbDisplay("那是你最后一只宝可梦！", then);           // :1986-1987
            return;
        }
        if (box != -1 && hasMail(heldpkmn)) {                    // :1989
            pbDisplay("请移除邮件。", then);                     // :1990-1991
            return;
        }
        if (box >= 0) {                                          // :1993
            Storage.resetForm(heldpkmn);                         // :1994-1995
        }
        pbSwapScene(selected);                                   // :1998
        Pokemon tmp = storage.get(box, index);                   // :1999
        storage.set(box, index, heldpkmn);                       // :2000
        heldpkmn = tmp;                                          // :2001
        pbRefresh();                                             // :2002
        then.run();
    }

    /** :2006-2046 pbRelease (提交) */
    private void pbRelease(int[] selected, Pokemon heldpoke, Runnable then) {
        int box = selected[0], index = selected[1];
        Pokemon pokemon = heldpoke != null ? heldpoke : storage.get(box, index);   // :2009
        if (pokemon == null) {                                   // :2010
            then.run();
            return;
        }
        if (pokemon.egg) {                                       // :2011
            pbDisplay("不能提交蛋。", then);                     // :2012
            return;
        } else if (isSpecialSpecies(pokemon)) {                  // :2014
            pbDisplay(intl("不能提交{1}。", pokemon.name), then);   // :2017
            return;
        } else if (hasMail(pokemon)) {                           // :2019
            pbDisplay("请移除邮件。", then);                     // :2020
            return;
        }
        if (box == -1 && pbAbleCount() <= 1 && pbAble(pokemon) && heldpoke == null) {   // :2023
            playBuzzerSe();                                      // :2024
            pbDisplay("那是你最后一只宝可梦！", then);           // :2025
            return;
        }
        pbShowCommands(intl("要提交{1}给博士做研究吗？", pokemon.name),   // :2028
                java.util.Arrays.asList("是", "否"), 0, command -> {
            if (command != 0) {                                  // :2029
                then.run();
                return;
            }
            String pkmnname = pokemon.name;                      // :2030
            Runnable go = () -> pbReleaseScene(selected, heldpoke != null, () -> {   // :2034
                if (heldpoke != null) {                          // :2035
                    heldpkmn = null;                             // :2036
                } else {
                    storage.pbDelete(box, index);                // :2038
                }
                pbRefresh();                                     // :2040
                pbDisplay(intl("{1}被提交了。", pkmnname), () -> {   // :2041
                    submitPkmn(1, () -> {                        // :2042
                        pbRefresh();                             // :2043
                        then.run();
                    });
                });
            });
            if (pokemon.shiny) {                                 // :2031
                pbConfirm(intl("真的确定要提交{1}吗？", pkmnname), yes -> {
                    if (yes) go.run(); else then.run();          // :2032 return false
                });
            } else {
                go.run();
            }
        });
    }

    /** :2048-2054 pbReleaseAll */
    private void pbReleaseAll(int boxNumber, Runnable then) {
        pbReleaseAllScene(boxNumber, 0, 0, count -> {            // :2049
            pbRefresh();                                         // :2050
            pbDisplay(intl("提交了盒子{1}内的{2}只宝可梦！", boxNumber + 1, count), () -> {   // :2051
                submitPkmn(count, () -> {                        // :2052
                    pbRefresh();                                 // :2053
                    then.run();
                });
            });
        });
    }

    /** :2056-2067 pbChooseMove —— 本段内无调用方，原样保留。 */
    @SuppressWarnings("unused")
    private void pbChooseMove(Pokemon pkmn, String helptext, int index, IntConsumer done) {
        List<String> movenames = new ArrayList<>();
        for (Pokemon.MoveSlot slot : pkmn.moves) {
            if (slot.move == null) break;
            if (slot.maxPp <= 0) {
                movenames.add(intl("{1} (PP：---)", slot.move.name));
            } else {
                movenames.add(intl("{1} (PP：{2}/{3})", slot.move.name, slot.pp, slot.maxPp));
            }
        }
        pbShowCommands(helptext, movenames, index, done);
    }

    /** :2069-2071 */
    private void pbSummary(int[] selected, Pokemon heldpoke, Runnable then) {
        pbSummaryScene(selected, heldpoke, then);
    }

    /** :2073-2075 */
    private void pbMark(int[] selected, Pokemon heldpoke, Runnable then) {
        pbMarkScene(selected, heldpoke, then);
    }

    /** :2077-2109 pbItem */
    private void pbItem(int[] selected, Pokemon heldpoke, Runnable then) {
        int box = selected[0], index = selected[1];
        Pokemon pokemon = heldpoke != null ? heldpoke : storage.get(box, index);   // :2080
        if (pokemon.egg) {                                       // :2081
            pbDisplay("蛋无法持有道具", then);                   // :2082
            return;
        } else if (hasMail(pokemon)) {                           // :2084
            pbDisplay("请移除邮件。", then);                     // :2085
            return;
        }
        if (pokemon.item != null && !pokemon.item.isEmpty()) {   // :2088
            String itemname = itemName(pokemon.item);            // :2089
            pbConfirm(intl("要取出{1}吗?", itemname), yes -> {   // :2090
                if (!yes) {
                    then.run();
                    return;
                }
                // :2091 $PokemonBag.pbStoreItem(pokemon.item) — 登记: 背包容量未建模，恒可存放
                context.gameState().inventory().add(pokemon.item, 1);
                pbDisplay(intl("取出了{1}.", itemname), () -> {  // :2094
                    pokemon.item = null;                         // :2095
                    pbHardRefresh();                             // :2096
                    then.run();
                });
            });
        } else {
            pbChooseItemScene(item -> {                          // :2100
                if (item != null) {                              // :2101
                    String itemname = itemName(item);            // :2102
                    pokemon.item = item;                         // :2103
                    context.gameState().inventory().remove(item, 1);   // :2104
                    pbDisplay(intl("{1}正在被持有。", itemname), () -> {   // :2105
                        pbHardRefresh();                         // :2106
                        then.run();
                    });
                } else {
                    then.run();
                }
            });
        }
    }

    private String itemName(String id) {
        PbsData.Item item = context.pbsData() == null ? null : context.pbsData().item(id);
        return item == null ? id : item.name;
    }

    /** :2111-2216 pbBoxCommands */
    private void pbBoxCommands(Runnable then) {
        List<String> commands = java.util.Arrays.asList("跳至", "换位", "搜索", "提交", "壁纸", "命名", "取消");   // :2112-2120
        pbShowCommands("你想要做什么？", commands, 0, command -> {   // :2121
            switch (command) {
                case 0:                                          // :2124
                    pbChooseBox("要跳至哪一个盒子？", destbox -> {   // :2125
                        if (destbox >= 0) pbJumpToBox(destbox, () -> afterJump(then));
                        else then.run();
                    });
                    break;
                case 1: {                                        // :2129
                    final int curbox = storage.currentBox;       // :2130
                    pbChooseBox("要和哪一个盒子交换？", destbox -> {   // :2131
                        if (destbox < 0 || destbox > storage.maxBoxes()) {   // :2132
                            then.run();
                            return;
                        }
                        if (curbox == destbox) {                 // :2133
                            pbDisplay("这样不是什么也没换吗！", then);   // :2134-2135
                            return;
                        }
                        Pokemon[] temp = storage.box(curbox).pokemon().clone();   // :2137
                        storage.box(curbox).pokemon(storage.box(destbox).pokemon().clone());   // :2138
                        storage.box(destbox).pokemon(temp);      // :2139
                        pbJumpToBox(destbox, () -> {             // :2140
                            box = new BoxSprite(storage.currentBox);   // 盒内容已换，重建精灵(原文由 PokemonBoxSprite 切盒时重建)
                            pbDisplay(intl("交换了盒子{1}与{2}的位置！", curbox + 1, destbox + 1), then);   // :2141
                        });
                    });
                    break;
                }
                case 2:                                          // :2142 搜索宝可梦
                    searchPokemon(then);
                    break;
                case 3:                                          // :2185
                    pbMessageConfirm("确定提交当前盒子内的所有宝可梦吗？", yes -> {   // :2186 pbConfirmMessage
                        if (!yes) {
                            then.run();
                            return;
                        }
                        int count = 0;                           // :2187
                        Storage.Box current = storage.box(storage.currentBox);
                        for (int index = 0; index < current.length(); index++) {   // :2188
                            if (current.get(index) != null) count += 1;            // :2189-2191
                        }
                        if (count == 0) {                        // :2193
                            playSe("GUI sel buzzer", 100);       // :2194
                            pbDisplay("当前盒子没有可提交的宝可梦。", then);   // :2195-2196
                            return;
                        }
                        playSe("GUI trainer card open", 100);    // :2198
                        pbReleaseAll(storage.currentBox, then);  // :2199
                    });
                    break;
                case 4: {                                        // :2201
                    List<Object[]> papers = storage.availableWallpapers();   // :2202
                    int index = 0;                               // :2203
                    for (int i = 0; i < papers.size(); i++) {    // :2204
                        if ((Integer) papers.get(i)[1] == storage.box(storage.currentBox).background) {
                            index = i;                           // :2206
                            break;
                        }
                    }
                    List<String> names = new ArrayList<>();
                    for (Object[] paper : papers) names.add((String) paper[0]);
                    pbShowCommands("选择一个壁纸。", names, index, wpaper -> {   // :2209
                        if (wpaper >= 0) {                       // :2210
                            pbChangeBackground((Integer) papers.get(wpaper)[1], then);   // :2211
                        } else {
                            then.run();
                        }
                    });
                    break;
                }
                case 5:                                          // :2213
                    pbBoxName("要给盒子命名为什么？", 0, 12, then);   // :2214
                    break;
                default:
                    then.run();
                    break;
            }
        });
    }

    /** 306_B2W2_PC:2142-2184 搜索宝可梦. */
    private void searchPokemon(Runnable then) {
        final int max = storage.maxBoxes();
        Consumer<Integer> afterRange = range -> {
            enterText("要搜索的名称是?(支持模糊搜索)", 0, 10, "", pkmnName -> {     // :2152
                if (pkmnName.isEmpty()) {                                          // :2154
                    then.run();
                    return;
                }
                int start = 0, end = max;                                          // :2157-2168
                if (range == 1) {
                    end = storage.currentBox;
                } else if (range == 2) {
                    start = storage.currentBox;
                }
                int destbox = -1;
                String found = pkmnName;
                for (int j = start; j < end; j++) {                                // :2169
                    for (int i = 0; i < storage.maxPokemon(j); i++) {
                        Pokemon pkmn = storage.get(j, i);
                        if (pkmn != null && pkmn.species != null && pkmn.species.name.contains(pkmnName)) {   // :2173 speciesName.include?
                            found = pkmn.species.name;                              // :2174
                            destbox = j;                                            // :2175
                            break;
                        }
                    }
                }
                if (destbox >= 0) {                                                // :2179
                    final int target = destbox;
                    pbMessagePlain(intl("在盒子{1}内搜索到{2}了!", target + 1, found),
                            () -> pbJumpToBox(target, () -> afterJump(then)));     // :2180-2181
                } else {
                    pbMessagePlain(intl("未在指定范围内搜索到\n名字包含{1}的精灵。", found), then);   // :2183
                }
            });
        };
        if (storage.currentBox == 0 || storage.currentBox == max - 1) {            // :2144
            afterRange.accept(0);
        } else {
            mode = Mode.MESSAGE;                                                   // :2147 pbMessage(范围, [...], -1)
            pbMessage.start("要在什么范围搜索宝可梦？", java.util.Arrays.asList("全部盒子",
                    intl("盒子1~盒子{1}", storage.currentBox), intl("盒子{1}~盒子{2}", storage.currentBox + 1, max)),
                    -1, 0, picked -> {
                        mode = Mode.IDLE;
                        if (picked == -1) {                                        // :2151
                            then.run();
                        } else {
                            afterRange.accept(picked);
                        }
                    });
        }
    }

    private void afterJump(Runnable then) {
        pbUpdateOverlay(selection, false);
        then.run();
    }

    /** 他段: Messages:1320 pbConfirmMessage = pbMessage(message,[是,否],2)==0 */
    private void pbMessageConfirm(String message, Consumer<Boolean> done) {
        mode = Mode.MESSAGE;
        pbMessage.start(message, java.util.Arrays.asList("是", "否"), 2, 0, result -> {
            mode = Mode.IDLE;
            done.accept(result == 0);
        });
    }

    /** 他段: Messages:1304 pbMessage(message) */
    private void pbMessagePlain(String message, Runnable then) {
        mode = Mode.MESSAGE;
        pbMessage.start(message, null, 0, 0, result -> {
            mode = Mode.IDLE;
            then.run();
        });
    }

    /** :2319-2347 submitPkmn */
    private void submitPkmn(int count, Runnable then) {
        java.util.Random random = new java.util.Random();        // 登记: rand 的种子未建模
        int money = count * (random.nextInt(100) + 50);          // :2320
        trainer.money += money;                                  // :2321
        pbMessagePlain(intl("\\G\\se[Mart buy item]{1}获得了{2}。\\wtnp[30]", trainer.name, money), () -> {   // :2322
            List<Object[]> rewards = new ArrayList<>();
            if (count == 30) {                                   // :2323
                rewards.add(new Object[] {"PROFSEEDRED", random.nextInt(2)});
                rewards.add(new Object[] {"PROFSEEDBLUE", random.nextInt(5) + 1});
                rewards.add(new Object[] {"EXPCANDYL", random.nextInt(10) + 1});
                rewards.add(new Object[] {"EXPCANDYM", 10});
            } else if (count >= 20) {                            // :2328
                rewards.add(new Object[] {"PROFSEEDRED", random.nextInt(2)});
                rewards.add(new Object[] {"PROFSEEDBLUE", random.nextInt(6)});
                rewards.add(new Object[] {"EXPCANDYL", random.nextInt(count - 19) + 1});
                rewards.add(new Object[] {"EXPCANDYM", 5});
            } else if (count >= 10) {                            // :2333
                rewards.add(new Object[] {"PROFSEEDBLUE", random.nextInt(4)});
                rewards.add(new Object[] {"EXPCANDYM", random.nextInt(count - 9) + 1});
                rewards.add(new Object[] {"EXPCANDYS", 10});
            } else if (count > 1) {                              // :2337
                if (count >= 5) {                                // :2338
                    rewards.add(new Object[] {"EXPCANDYS", random.nextInt(count - 4) + 1});
                    rewards.add(new Object[] {"EXPCANDYXS", 5});
                } else {
                    rewards.add(new Object[] {"EXPCANDYXS", random.nextInt(count - 1) + 2});
                }
            } else {
                rewards.add(new Object[] {"EXPCANDYXS", 1});     // :2345
            }
            receiveItems(rewards, 0, then);
        });
    }

    private void receiveItems(List<Object[]> rewards, int index, Runnable then) {
        if (index >= rewards.size()) {
            then.run();
            return;
        }
        Object[] reward = rewards.get(index);
        pbReceiveItem((String) reward[0], (Integer) reward[1], () -> receiveItems(rewards, index + 1, then));
    }

    /** 他段: Item Find #309 :185-206 pbReceiveItem（LEFTOVERS/招式机分支本段用不到）。 */
    private void pbReceiveItem(String internalName, int quantity, Runnable then) {
        PbsData.Item item = context.pbsData() == null ? null : context.pbsData().item(internalName);
        if (item == null || quantity < 1) {                      // :187 return false
            then.run();
            return;
        }
        String itemname = quantity > 1 && item.namePlural != null && !item.namePlural.isEmpty() ? item.namePlural : item.name;   // :188
        String meName = item.type == 6 ? "Key item get" : "Item get";   // :190
        String text = quantity > 1
                ? intl("\\me[{1}]获得了{2}个\\c[1]{3}\\c[0]！\\wtnp[30]", meName, quantity, itemname)   // :196
                : intl("\\me[{1}]获得了1个\\c[1]{2}\\c[0]！\\wtnp[30]", meName, itemname);              // :198
        pbMessagePlain(text, () -> {
            context.gameState().inventory().add(internalName, quantity);   // :200 pbStoreItem
            pbMessagePlain(intl("你将{1}放进了<icon=bagPocket{2}>\\c[1]{3}口袋\\c[0]。",             // :201
                    itemname, item.pocket, PbMessage.pocketName(item.pocket)), then);
        });
    }

    // ---- :2218-2317 pbChoosePokemon / pbChooseEggToHatch — 登记: 由事件调用，本工程未接，空实现。

    /** :2218-2270 PokemonStorageScreen#pbChoosePokemon — 登记: 空实现(无调用方)。 */
    @SuppressWarnings("unused")
    public int[] pbChoosePokemon() {
        return null;
    }

    // :2273-2317 pbChooseEggToHatch：原文写成顶层 def 却使用 @scene/@storage，运行即报错(插件缺陷)，
    // 登记: 不转译。

    // =====================================================================
    // update
    // =====================================================================

    private static int add(List<String> commands, String label) {
        commands.add(label);
        return commands.size() - 1;
    }

    /** @return true when the screen should close. */
    public boolean update(InputManager input) {
        int ticks = clock.advance();
        if (finished) return true;
        if (mode == Mode.SUMMARY && summary != null) {
            if (summary.update(input)) {
                Runnable done = summaryDone;
                summaryDone = null;
                mode = Mode.IDLE;
                done.run();
            }
            return finished;
        }
        if (mode == Mode.MESSAGE) {
            pbMessage.update(input, ticks);
            return finished;
        }
        if (mode == Mode.BAG) {
            return finished;
        }
        if (mode == Mode.TEXT && textEntry != null) {
            if (textEntry.update(input)) {
                String typed = textEntry.result();
                textEntry.dispose();
                textEntry = null;
                mode = Mode.IDLE;
                Consumer<String> done = textDone;
                textDone = null;
                if (done != null) done.accept(typed == null ? "" : typed);
            }
            return finished;
        }
        for (int t = 0; t < ticks && !finished; t++) {
            sceneTick();
            if (mode == Mode.ANIM && animStep.getAsBoolean()) {
                Runnable done = animDone;
                animDone = null;
                animStep = null;
                mode = Mode.IDLE;
                done.run();
            }
        }
        switch (mode) {
            case SELECT_BOX: updateSelectBox(input); break;
            case SELECT_PARTY: updateSelectParty(input); break;
            case COMMANDS: updateCommands(input); break;
            case DISPLAY: updateDisplay(input); break;
            case MARKING: updateMarking(input); break;
            default: break;
        }
        return finished;
    }

    // =====================================================================
    // 绘制
    // =====================================================================

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, MenuFont smallFont) {
        render(b, a, f, skin, skin, smallFont);
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, WindowSkin speech, MenuFont smallFont) {
        float w = ScreenMetrics.logicalWidth(), h = ScreenMetrics.logicalHeight();
        screenH = h;
        screenW = w;
        if (mode == Mode.SUMMARY && summary != null) {
            summary.render(b, a, f, skin);
            return;
        }
        if (mode == Mode.TEXT && textEntry != null) {
            textEntry.render(b, a, f, skin);
            return;
        }
        if (box == null) {                                       // 菜单入口被地图拦下: 只有那条提示
            if (mode == Mode.MESSAGE) pbMessage.render(b, a, f, skin, speech, w, h);
            return;
        }
        Texture bg = a.graphic("Pictures/Storage", "bg");
        if (bg != null) b.draw(bg, 0f, 0f, w, h);                // :646 addBackgroundPlane
        drawBox(b, a, f, box);                                   // :647
        if (newBox != null) drawBox(b, a, f, newBox);
        drawBattler(b, a, h);                                    // :654-659 (z-1: below the frame)
        Texture main = a.graphic("Pictures/Storage", "overlay_main");
        if (main != null) b.draw(main, 0f, 0f, w, h);            // :648-649
        renderOverlay(b, a, f, smallFont);                       // :650-653
        drawParty(b, a, f);                                      // :660
        if (markingVisible) drawMarking(b, a, f);                // :666-671
        drawArrow(b, a);                                         // :672
        Color[] sc = MenuPanel.textColors(skin);
        switch (mode) {
            case COMMANDS:
                drawCommands(b, a, f, skin, sc, w, h);
                break;
            case DISPLAY:
                drawMessageWindow(b, a, f, skin, sc, displayText, w, h);
                break;
            case MARKING:
                drawMessageWindow(b, a, f, skin, sc, "标记宝可梦。", w, h);   // :1358-1365
                break;
            case MESSAGE:
                pbMessage.render(b, a, f, skin, speech, w, h);
                break;
            default:
                break;
        }
    }

    private void img(SpriteBatch b, Texture t, float x, float y, int sx, int sy, int sw, int sh) {
        b.draw(t, x, screenH - y - sh, sw, sh, sx, sy, sw, sh, false, false);
    }

    private void txt(SpriteBatch b, MenuFont f, String text, float x, float y, int align, Color main, Color shadow) {
        if (text == null) return;
        float ty = screenH - y;
        if (align == 1) f.drawRight(b, text, x, ty, main, shadow);
        else if (align == 2) f.drawCentered(b, text, x, ty, main, shadow);
        else f.draw(b, text, x, ty, main, shadow);
    }

    private void window(SpriteBatch b, MenuAssets a, WindowSkin skin, float x, float top, float width, float height) {
        MenuPanel.window(b, a, skin, x, screenH - top - height, width, height);
    }

    private void drawIcon(SpriteBatch b, MenuAssets a, Icon icon) {
        if (icon == null || icon.disposed || icon.pokemon == null) return;
        Texture tex = pokemonIcon(a, icon.pokemon);
        if (tex == null) return;
        int size = tex.getHeight();                              // :77 src_rect = (0,0,height,height)
        float alpha = Math.max(0f, Math.min(1f, icon.opacity));
        b.setColor(1f, 1f, 1f, alpha);
        if (icon.centered) {
            float z = Math.max(0f, icon.zoom);
            float cx = icon.x, cy = icon.y;                      // x/y already moved by +32 (centre)
            b.draw(tex, cx - size * z / 2f, screenH - cy - size * z / 2f, size * z, size * z, 0, 0, size, size, false, false);
        } else {
            img(b, tex, icon.x, icon.y, 0, 0, size, size);
        }
        b.setColor(Color.WHITE);
    }

    /** :453-482 PokemonBoxSprite#refresh 的绘制：盒图 + 盒名 + 图标。 */
    private void drawBox(SpriteBatch b, MenuAssets a, MenuFont f, BoxSprite sprite) {
        Storage.Box data = storage.box(sprite.number);
        int bg = data.background;                                // :408-424 getBoxBitmap
        if (bg < 0 || !storage.isAvailableWallpaper(bg)) bg = sprite.number % Storage.BASICWALLPAPERQTY;
        Texture boxTex = a.graphic("Pictures/Storage", "box_" + bg);
        if (boxTex != null) {
            int bw = Math.min(324, boxTex.getWidth()), bh = Math.min(302, boxTex.getHeight());
            img(b, boxTex, sprite.x, sprite.y, 0, 0, bw, bh);                      // :458 blt
            if (sprite.tintAlpha > 0f) {                         // pbChangeBackground :1026 color=(248,248,248,alpha)
                float al = sprite.tintAlpha;
                b.setColor(1f - al, 1f - al, 1f - al, 1f);
                b.draw(boxTex, sprite.x, screenH - sprite.y - bh, bw, bh, 0, 0, bw, bh, false, false);
                b.flush();
                b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                float add = al * 248f / 255f;
                b.setColor(add, add, add, 1f);
                b.draw(boxTex, sprite.x, screenH - sprite.y - bh, bw, bh, 0, 0, bw, bh, false, false);
                b.flush();
                b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                b.setColor(Color.WHITE);
            }
            String name = data.name;                             // :455
            float widthval = f.width(name);                      // :460
            float xval = 162 - widthval / 2f;                    // :461
            // :463 pbDrawShadowText(@contents,xval,8,widthval,40,...): 文字在 40px 高的矩形里垂直居中
            float top = sprite.y + 8f + (40f - f.lineHeight()) / 2f;
            txt(b, f, name, sprite.x + xval, top, 0, BOX_NAME, BTN_SHADOW);
        }
        for (Icon icon : sprite.icons) drawIcon(b, a, icon);
    }

    /** :654-659 战斗图 (MosaicPokemonSprite, Center at (98,180)). */
    private void drawBattler(SpriteBatch b, MenuAssets a, float h) {
        if (!overlayHasPokemon || mosaicPokemon == null) return;     // :1478 visible=false
        Texture sprite = battler(a, mosaicPokemon);
        if (sprite == null) return;
        Texture shown = sprite;
        if (mosaic > 0) shown = mosaicTexture(sprite, mosaicPokemon, mosaic);
        float sx = 98f - shown.getWidth() / 2f, sy = 180f - shown.getHeight() / 2f;
        b.draw(shown, sx, h - sy - shown.getHeight());
    }

    /** :124-150 mosaicRefresh（RGSS stretch_blt 近似为最近邻取样）。 */
    private Texture mosaicTexture(Texture source, Pokemon pokemon, int m) {
        if (mosaicTexture != null && mosaicTextureValue == m && mosaicTexturePokemon == pokemon) return mosaicTexture;
        TextureData data = source.getTextureData();
        boolean prepared = data.isPrepared();
        if (!prepared) data.prepare();
        Pixmap src = data.consumePixmap();
        int w = src.getWidth(), h = src.getHeight();
        int newW = Math.max(w / m, 1), newH = Math.max(h / m, 1);          // :135-136
        int[] small = new int[newW * newH];
        for (int y = 0; y < newH; y++) {                          // :142 @mosaicbitmap.stretch_blt(newW,newH <- old)
            for (int x = 0; x < newW; x++) small[y * newW + x] = src.getPixel(x * w / newW, y * h / newH);
        }
        Pixmap out = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        out.setBlending(Pixmap.Blending.None);
        int ox = -m / 2 + 1, oy = -m / 2 + 1;                      // :144-145 Rect(-@mosaic/2+1, ...)
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int dx = x - ox, dy = y - oy;
                if (dx < 0 || dy < 0 || dx >= w || dy >= h) { out.drawPixel(x, y, 0); continue; }
                out.drawPixel(x, y, small[(dy * newH / h) * newW + (dx * newW / w)]);
            }
        }
        if (data.disposePixmap()) src.dispose();
        if (mosaicTexture != null) mosaicTexture.dispose();
        mosaicTexture = new Texture(out);
        mosaicTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        out.dispose();
        mosaicTextureValue = m;
        mosaicTexturePokemon = pokemon;
        return mosaicTexture;
    }

    /** :1460-1604 pbUpdateOverlay 的绘制(overlay 系统字体 + overlay2 小字体)。 */
    private void renderOverlay(SpriteBatch b, MenuAssets a, MenuFont f, MenuFont small) {
        txt(b, f, intl("查看队伍: {1}", overlayPartyCount), 274f, 347f + 64f, 2, BTN, BTN_SHADOW);   // :1468
        txt(b, f, "退出", 510f + 96f, 347f + 64f, 2, BTN, BTN_SHADOW);                                // :1469
        Pokemon p = overlayPokemon;
        if (p == null) return;                                   // :1477
        txt(b, f, p.name == null ? "" : p.name, 10f, 13f, 0, BASE, SHADOW);                          // :1488
        if (p.egg) return;                                       // :1491
        Texture hpbg = a.graphic("Pictures/Party", p.fainted() ? "overlay_hp_back_faint" : "overlay_hp_back");   // :1494-1498
        if (hpbg != null) img(b, hpbg, 26f, 68f, 0, 0, 138, 14);                                      // :1499-1500
        if (p.hp > 0) {                                          // :1501
            Texture hpbar = a.graphic("Pictures/Storage", "overlay_hp");
            if (hpbar != null && p.maxHp() > 0) {
                float wd = p.hp * 96f / p.maxHp();               // :1503
                if (wd < 1f) wd = 1f;                            // :1504
                wd = Math.round(wd / 2f) * 2f;                   // :1505
                int hpzone = 0;                                  // :1506
                if (p.hp <= p.maxHp() / 2) hpzone = 1;           // :1507
                if (p.hp <= p.maxHp() / 4) hpzone = 2;           // :1508
                img(b, hpbar, 58f, 70f, 0, hpzone * 8, (int) wd, 8);                                   // :1509-1510
            }
        }
        int status = -1;                                         // :1513
        if (p.pokerusStage() == 1) status = 8;                          // :1514
        if (p.status != null && !p.status.isEmpty()) {           // :1515
            switch (p.status) {
                case "SLEEP": status = 0; break;
                case "POISON": status = 1; break;
                case "BURN": status = 2; break;
                case "PARALYSIS": status = 3; break;
                case "FROZEN": status = 4; break;
                default: break;
            }
        }
        if (p.hp <= 0) status = 7;                               // :1516
        Texture statuses = a.graphic("Pictures", "statuses");
        if (status >= 0 && statuses != null && status * 16 + 16 <= statuses.getHeight()) {
            img(b, statuses, 132f, 240f + 64f, 0, 16 * status, 44, 16);                                // :1518
        }
        float[] xs = {576, 591, 606, 651, 621, 637};             // :1521
        for (int i = 0; i < 6; i++) {                            // :1522-1537
            Texture rating = a.graphic("Pictures/Summary", ratingFile(p.ivs[i]));
            if (rating != null) img(b, rating, xs[i], 114f, 0, 0, 14, 20);
        }
        String formName = formName(p);                           // :1538-1539
        txt(b, small, intl("图鉴ID:") + String.format("%04d", p.species == null ? 0 : p.species.id), 532f, 13f, 0, BASE, SHADOW);   // :1541
        txt(b, small, formName, 524f, 80f, 0, BASE, SHADOW);                                           // :1542
        txt(b, small, "个体:", 524f, 112f, 0, BASE, SHADOW);                                           // :1543
        txt(b, small, "性格:" + (p.nature == null ? "" : PBNatures.getName(p.nature.id)), 524f, 142f, 0, BASE, SHADOW);   // :1544
        float moveY = 174f;                                      // :1546
        for (Pokemon.MoveSlot m : p.moves) {                     // :1547
            if (m == null || m.move == null) continue;           // :1548
            txt(b, small, m.move.name, 524f, moveY, 0, BASE, SHADOW);                                  // :1549
            moveY += 30f;                                        // :1550
        }
        if (p.gender == pokemon.runtime.pokemon.PokemonStats.MALE) {          // :1553
            txt(b, f, "♂", 148f, 14f, 0, new Color(0f, 0f, 214f / 255f, 1f), new Color(15f / 255f, 148f / 255f, 1f, 1f));
        } else if (p.gender == pokemon.runtime.pokemon.PokemonStats.FEMALE) {  // :1555
            txt(b, f, "♀", 148f, 14f, 0, new Color(198f / 255f, 0f, 0f, 1f), new Color(1f, 155f / 255f, 155f / 255f, 1f));
        }
        Texture lv = a.graphic("Pictures/Storage", "overlay_lv");                                    // :1559
        if (lv != null) img(b, lv, 6f, 268f + 64f, 0, 0, lv.getWidth(), lv.getHeight());
        txt(b, f, String.valueOf(p.level), 28f, 261f + 64f, 0, WHITE, BASE);                          // :1560
        String ability = abilityName(p);                          // :1563-1567
        txt(b, f, ability == null ? "无特性" : ability, 94f, 327f + 64f, 2, BASE, SHADOW);
        String item = p.item != null && !p.item.isEmpty() ? itemName(p.item) : null;                  // :1570-1574
        txt(b, f, item == null ? "无道具" : item, 94f, 358f + 64f, 2, BASE, SHADOW);
        if (p.shiny) {                                           // :1577-1583
            Texture shiny = a.graphic("Pictures", p.superShiny ? "superShiny" : "shiny");
            if (shiny != null) img(b, shiny, 64f, 262f + 64f, 0, 0, shiny.getWidth(), shiny.getHeight());
        }
        Texture types = a.graphic("Pictures", "types");           // :1586-1594
        com.badlogic.gdx.utils.Array<String> list = p.types();
        if (types != null && context.pbsData() != null && list.size > 0) {
            PbsData.TypeInfo t1 = context.pbsData().type(list.get(0));
            PbsData.TypeInfo t2 = list.size > 1 ? context.pbsData().type(list.get(1)) : t1;
            if (t1 == t2 || t2 == null) {
                if (t1 != null) img(b, types, 62f, 292f + 64f, 0, t1.id * 28, 64, 28);                // :1590
            } else {
                if (t1 != null) img(b, types, 26f, 292f + 64f, 0, t1.id * 28, 64, 28);                // :1592
                img(b, types, 96f, 292f + 64f, 0, t2.id * 28, 64, 28);                                // :1593
            }
        }
        drawMarkings(b, a, 86f, 262f + 64f, p.markings);         // :1597
    }

    /** :1451-1458 drawMarkings: 8 个槽，markings.png 只有 6 个(96 宽)，超出的 blt 无效果。 */
    private void drawMarkings(SpriteBatch b, MenuAssets a, float x, float y, int markings) {
        Texture marks = a.graphic("Pictures/Storage", "markings");
        if (marks == null) return;
        for (int i = 0; i < 8; i++) {
            if (i * 16 + 16 > marks.getWidth()) continue;
            int srcY = (markings & (1 << i)) != 0 ? 16 : 0;
            img(b, marks, x + i * 16f, y, i * 16, srcY, 16, 16);
        }
    }

    /** :499-614 PokemonBoxPartySprite 绘制。 */
    private void drawParty(SpriteBatch b, MenuAssets a, MenuFont f) {
        Texture panel = a.graphic("Pictures/Storage", "overlay_party");
        if (panel != null) img(b, panel, boxparty.x, boxparty.y, 0, 0, Math.min(172, panel.getWidth()), Math.min(352, panel.getHeight()));   // :585
        txt(b, f, "返回", boxparty.x + 86f, boxparty.y + 242f, 2, BTN, BTN_SHADOW);                  // :588
        for (Icon icon : boxparty.icons) drawIcon(b, a, icon);
    }

    /** :1355-1435 markingbg + markingoverlay. */
    private void drawMarking(SpriteBatch b, MenuAssets a, MenuFont f) {
        Texture bg = a.graphic("Pictures/Storage", "overlay_marking");
        if (bg != null) img(b, bg, 292f + 32f, 68f, 0, 0, bg.getWidth(), bg.getHeight());            // :666
        Texture marks = a.graphic("Pictures/Storage", "markings");
        if (marks != null) {
            for (int i = 0; i < 6; i++) {                        // :1384
                int srcY = (markingValue & (1 << i)) != 0 ? 16 : 0;                                   // :1386
                img(b, marks, 336f + 58f * (i % 3) + 32f, 106f + 50f * (i / 3), i * 16, srcY, 16, 16);   // :1387
            }
        }
        txt(b, f, "确认", 402f + 32f, 210f, 2, BTN, BTN_SHADOW);                                     // :1390
        txt(b, f, "取消", 402f + 32f, 274f, 2, BTN, BTN_SHADOW);                                     // :1391
    }

    /** :167-333 PokemonBoxArrow 的手形 + 手持图标。 */
    private void drawArrow(SpriteBatch b, MenuAssets a) {
        if (heldSprite != null && !heldSprite.disposed && (holding || grabbingState > 0 || placingState > 0)) {
            drawIcon(b, a, heldSprite);                          // z=1
        }
        if (floating != null && floating != heldSprite && !floating.disposed) {
            drawIcon(b, a, floating);                            // 放下的后半段: 图标还在原处，直到 setPokemon
        }
        Texture hand = a.graphic("Pictures/Storage", handImage);
        if (hand != null) img(b, hand, arrowX, arrowY + arrowYOffset, 0, 0, hand.getWidth(), hand.getHeight());   // z=2
    }

    /** :696-715 pbDisplay / :1359-1365 的右下角文字窗：Window_UnformattedTextPokemon.newWithSize("",180,0,W-180,32)。 */
    private void drawMessageWindow(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, Color[] tc,
                                   String message, float w, float h) {
        float width = w - 180f;
        List<String> lines = wrap(f, message, width - BORDER - 4f);
        float height = BORDER + lines.size() * ROW;              // resizeHeightToFit
        float top = h - height;                                  // pbBottomRight
        window(b, a, skin, 180f, top, width, height);
        for (int i = 0; i < lines.size(); i++) {
            txt(b, f, lines.get(i), 180f + 16f, top + 16f + i * ROW + (ROW - f.lineHeight()) / 2f, 0, tc[0], tc[1]);
        }
    }

    /** :717-752 pbShowCommands 的两个窗口。 */
    private void drawCommands(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, Color[] tc, float w, float h) {
        float msgWidth = w - 180f;
        List<String> lines = wrap(f, cmdMessage, msgWidth - BORDER - 4f);
        float msgHeight = BORDER + lines.size() * ROW;           // :724 resizeHeightToFit
        float msgTop = h - msgHeight;                            // :725 pbBottomRight
        window(b, a, skin, 180f, msgTop, msgWidth, msgHeight);
        for (int i = 0; i < lines.size(); i++) {
            txt(b, f, lines.get(i), 180f + 16f, msgTop + 16f + i * ROW + (ROW - f.lineHeight()) / 2f, 0, tc[0], tc[1]);
        }
        float width = 0f;                                        // Window_CommandPokemon#resizeToFit :729
        for (String label : cmdLabels) width = Math.max(width, f.width(label));
        width += 16f + 16f + 4f + BORDER;
        float height = Math.min(BORDER + cmdLabels.size() * ROW, h);
        if (height > h - msgHeight) height = h - msgHeight;      // :730
        float x = w - width;                                     // :731 pbBottomRight
        float top = h - height - msgHeight;                      // :732 y -= msgwindow.height
        window(b, a, skin, x, top, width, height);
        int visible = Math.max(1, (int) ((height - BORDER) / ROW));
        if (cmdIndex < cmdTop) cmdTop = cmdIndex;
        if (cmdIndex >= cmdTop + visible) cmdTop = cmdIndex - visible + 1;
        for (int row = 0; row < visible && cmdTop + row < cmdLabels.size(); row++) {
            float rowTop = top + 16f + row * ROW;
            txt(b, f, cmdLabels.get(cmdTop + row), x + 32f, rowTop + (ROW - f.lineHeight()) / 2f, 0, tc[0], tc[1]);
        }
        MenuPanel.cursor(b, a, skin, x, screenH - top - height, height, cmdIndex - cmdTop, cmdIndex - cmdTop, ROW);
    }

    private List<String> wrap(MenuFont f, String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : (text == null ? "" : text).split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < paragraph.length(); i++) {
                char c = paragraph.charAt(i);
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

    // =====================================================================
    // 小工具
    // =====================================================================

    private static String ratingFile(int iv) {                   // :1523-1535
        if (iv > 30) return "RatingS";
        if (iv > 22) return "RatingA";
        if (iv > 15) return "RatingB";
        if (iv > 7) return "RatingC";
        if (iv > 0) return "RatingD";
        return "RatingF";
    }

    /** :1538-1539 pbGetMessage(FormNames) —— 登记: 用 pbs 的 formName 数据代替消息表。 */
    private String formName(Pokemon p) {
        String name = p.form != null ? p.form.formName : (p.species == null ? null : p.species.formName);
        return name == null || name.isEmpty() ? "默认形态" : name;
    }

    private String abilityName(Pokemon p) {                       // :1563-1564 PBAbilities.getName
        if (p.ability == null || p.ability.isEmpty()) return null;
        PbsData.Ability ability = context.pbsData() == null ? null : context.pbsData().ability(p.ability);
        return ability != null && ability.name != null ? ability.name : p.ability;
    }

    private Texture pokemonIcon(MenuAssets a, Pokemon p) {
        if (p == null || p.species == null) return null;
        String suffix = (p.shiny ? "s" : "") + (p.egg ? "egg" : "");
        Texture icon = a.icon(String.format("icon%03d%s", p.species.id, suffix));
        if (icon == null) icon = a.icon("icon" + p.species.internalName + suffix);
        if (icon == null && p.egg) icon = a.icon("iconEgg");
        return icon;
    }

    /** PokemonSprite#setPokemonBitmap -> pbLoadPokemonBitmap；登记: 沿用 SummaryView 的取图规则。 */
    private Texture battler(MenuAssets a, Pokemon p) {
        if (p.species == null) return null;
        String base = String.format("%03d", p.species.id) + (p.shiny ? "s" : "");
        int form = p.form == null ? 0 : p.form.form;
        Texture sprite = a.graphic("Battlers", form > 0 ? base + "_" + form : base);
        if (sprite == null) sprite = a.graphic("Battlers", base);
        if (sprite == null) sprite = a.graphic("Battlers", String.format("%03d", p.species.id));
        if (sprite == null) sprite = a.graphic("Battlers", p.species.internalName);
        return sprite;
    }

    private static String intl(String template, Object... args) {
        String out = template;
        for (int i = 0; i < args.length; i++) out = out.replace("{" + (i + 1) + "}", String.valueOf(args[i]));
        return out;
    }

    private void playSe(String name, int volume) {
        if (context.audioManager() != null) context.audioManager().playSe(name, volume, 100);
    }

    private void playCursorSe() {                                 // 登记: $data_system.cursor_se 未建模
        MenuSe.cursor(context.audioManager());
    }

    private void playDecisionSe() {
        MenuSe.decision(context.audioManager());
    }

    private void playBuzzerSe() {
        MenuSe.buzzer(context.audioManager());
    }
}
