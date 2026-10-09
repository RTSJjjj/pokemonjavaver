package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.map.ToneShader;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.ui.WindowSkin;

import java.util.ArrayList;
import java.util.List;

/**
 * 段 DiegoWT's Starter Selection script (Scripts.rxdata #333, 565 行；原文
 * {@code plugin-src/ruby/333_DiegoWT_s_Starter_Selection_script.rb})：
 * {@code DiegoWTsStarterSelection.new(pkmn1,pkmn2,pkmn3)}，BW 风格 ({@code INSTYLE = 2})。
 *
 * <p>原文是阻塞的 {@code while} / {@code N.times do ... pbWait(1) end}，一个 {@code pbWait(1)} 是一帧（40 帧/秒）。
 * 这里拆成「每个 40fps 刻度 {@link #tick} 一次」的状态机，每个阶段的刻度数、每刻的增减量都照原文
 * （整数除法也照搬：{@code 200/20 = 10}、{@code 255/20 = 12}、{@code 255/18 = 14} ...）。</p>
 *
 * <p>登记: ①{@code starterbg.color = Color.new(255,255,255,105)}（RGSS 颜色混合）用
 * 「先画图再叠一层白色」近似；②{@code typebg.tone} 用 {@link ToneShader}；③文字窗口是无皮肤的
 * 消息窗，只实现 {@code <ac>} 居中和 {@code <br>}；④{@code STARTERCZ >= 1}（大圆环）、HGSS 风格 ({@code INSTYLE == 1})
 * 没有用到（原文默认值 0 / 2），未做；⑤选定后给宝可梦（{@code pbGenPkmn}/{@code pbAddPokemon}）由事件解释器完成。</p>
 */
public final class StarterSelectionView {
    private static final String DIR = "Starter Selection";

    // :6-26 constants
    private static final int STARTER_X = 0;
    private static final int STARTER_Y = 0;

    private enum Phase {
        OPEN, ASK, CHOOSE, BALL_A, BALL_B, BALL_C, PKMN_IN, CONFIRM,
        CANCEL_A, CANCEL_B, CANCEL_C, CANCEL_D, CLOSE, DONE
    }

    /** An {@code IconSprite}: top-left position unless {@code centered}, opacity 0..255, angle in degrees. */
    private static final class Sprite {
        float x;
        float y;
        float opacity = 255f;
        float angle;
        boolean visible = true;
        boolean centered;
        Texture texture;
        int srcX;
        int srcY;
        int srcW;
        int srcH;
        Sprite(float x, float y) {
            this.x = x;
            this.y = y;
        }
        void texture(Texture value) {
            texture = value;
            if (value != null) {
                srcX = 0;
                srcY = 0;
                srcW = value.getWidth();
                srcH = value.getHeight();
            }
        }
        void clamp() {
            opacity = Math.max(0f, Math.min(255f, opacity));
        }
    }

    private final RuntimeContext context;
    private final int[] species;                // dex numbers of the three starters
    private final PbsData.Species[] data = new PbsData.Species[3];
    private final MenuClock clock = new MenuClock();
    private final ToneShader tone = new ToneShader();
    private boolean toneReady;

    private Phase phase = Phase.OPEN;
    private int phaseTick;
    private int select;                         // @select, 1..3
    private int oldX;
    private int frame;                          // @frame
    private int selframe;                       // @selframe
    private int frameCount;                     // Graphics.frame_count
    private int choicesel = 1;
    private boolean confirmed;
    private boolean finished;
    private boolean assetsLoaded;

    // sprites
    private final Sprite starterbg = new Sprite(0, 0);
    private final Sprite typebg = new Sprite(0, 0);
    private final Sprite base = new Sprite(0, 138);
    private final Sprite[] shadow = {new Sprite(88 + 64, 212), new Sprite(242 + 64, 212), new Sprite(396 + 64, 212)};
    private final Sprite[] ball = {new Sprite(134 + 64, 188), new Sprite(288 + 64, 188), new Sprite(442 + 64, 188)};
    private final Sprite selectSprite = new Sprite(134 + 64, 188);
    private final Sprite selection = new Sprite(288 + 64, 32);
    private final Sprite ballbase = new Sprite(0, 0);
    private final Sprite[] pkmn = {new Sprite(352 + STARTER_X, 148 + STARTER_Y),
            new Sprite(352 + STARTER_X, 148 + STARTER_Y), new Sprite(352 + STARTER_X, 148 + STARTER_Y)};
    private final Sprite textwnd = new Sprite(0, 272);
    private final Sprite choice1 = new Sprite(402, 174);
    private final Sprite choice2 = new Sprite(402, 220);
    private final Sprite choicesel_ = new Sprite(402, 174);

    // the message window (letter by letter)
    private List<String> textLines = new ArrayList<>();
    private String textFull = "";
    private int textShown;
    private int textCounter;
    private float textY = 352f;                 // pbBottomLeftLines(msgwindow,2): 448-96
    private boolean textVisible = true;

    private String typeName = "";
    private float[] typeTone = new float[3];
    private boolean typeToneSet;

    public StarterSelectionView(RuntimeContext context, int[] dexNumbers) {
        this.context = context;
        this.species = dexNumbers;
        PbsData pbs = context.pbsData();
        for (int i = 0; i < 3; i++) {
            String name = pbs == null ? null : pbs.speciesById.get(String.valueOf(dexNumbers[i]));
            data[i] = name == null ? null : pbs.species(name);
        }
        starterbg.opacity = 0;
        typebg.opacity = 0;
        base.opacity = 0;
        for (int i = 0; i < 3; i++) {
            shadow[i].opacity = 0;
            ball[i].opacity = 0;
            ball[i].centered = true;
            pkmn[i].opacity = 0;
            pkmn[i].centered = true;
        }
        selectSprite.centered = true;
        selectSprite.visible = false;
        selection.visible = false;
        ballbase.centered = true;
        ballbase.opacity = 0;
        textwnd.opacity = 0;
        choice1.visible = false;
        choice2.visible = false;
        choicesel_.visible = false;
    }

    public boolean finished() {
        return finished;
    }

    /** 1..3: the ball that was chosen (valid once {@link #finished()}), 0 when none. */
    public int chosen() {
        return confirmed ? select : 0;
    }

    private void loadAssets(MenuAssets a) {
        if (assetsLoaded) {
            return;
        }
        assetsLoaded = true;
        starterbg.texture(a.graphic("Pictures/" + DIR, "starterbg_custom"));
        typebg.texture(a.graphic("Pictures/" + DIR, "starterbg_custom"));
        base.texture(a.graphic("Pictures/" + DIR, "base"));
        for (int i = 0; i < 3; i++) {
            shadow[i].texture(a.graphic("Pictures/" + DIR, "shadow"));
            ball[i].texture(a.graphic("Pictures/" + DIR, "ball" + (i + 1)));
            pkmn[i].texture(frontSprite(a, data[i]));
        }
        selectSprite.texture(a.graphic("Pictures/" + DIR, "sel"));
        selection.texture(a.graphic("Pictures/" + DIR, "select"));
        ballbase.texture(a.graphic("Pictures/" + DIR, "ballbase"));
        textwnd.texture(a.graphic("Pictures/" + DIR, "window_bw"));
        Texture choice = a.graphic("Pictures/" + DIR, "choice_bw");
        choice1.texture(choice);
        choice2.texture(choice);
        choicesel_.texture(choice);
        setSrc(choice1, 0, 0);
        setSrc(choice2, 140, 0);
        setSrc(choicesel_, 0, 96);
    }

    private static void setSrc(Sprite sprite, int x, int y) {
        sprite.srcX = x;
        sprite.srcY = y;
        sprite.srcW = 140;
        sprite.srcH = 48;
    }

    private Texture frontSprite(MenuAssets a, PbsData.Species s) {
        if (s == null) {
            return null;
        }
        Texture sprite = a.graphic("Battlers", String.format("%03d", s.id));
        if (sprite == null) {
            sprite = a.graphic("Battlers", s.internalName);
        }
        return sprite;
    }

    // =====================================================================
    // Update
    // =====================================================================

    public boolean update(InputManager input) {
        int ticks = clock.advance();
        // input: once per rendered frame, like Input.trigger?
        handleInput(input);
        for (int i = 0; i < ticks; i++) {
            tick();
        }
        return finished;
    }

    private boolean pressedAny(InputManager input) {
        return input.wasPressed(GameAction.RIGHT) || input.wasPressed(GameAction.LEFT)
                || input.wasPressed(GameAction.CONFIRM) || input.wasPressed(GameAction.CANCEL)
                || input.wasPressed(GameAction.MENU);
    }

    /** :207-312 pbStartChoosing / pbChoosingScene and :515-547 pbConfirm. */
    private void handleInput(InputManager input) {
        switch (phase) {
            case ASK:
                if (pressedAny(input)) {                           // :210-214
                    select = 2;                                    // :216
                    oldX = (int) ball[select - 1].x;               // :217
                    frame = 0;                                     // :218
                    MenuSe.cursor(context.audioManager());         // :219
                    selectSprite.visible = true;                   // :220
                    selection.visible = true;                      // :221
                    selectSprite.x = xOf(select);                  // :222
                    selectSprite.y = 188;                          // :223
                    selection.x = xOf(select) - 60;                // :224
                    selection.y = 188 - 154;                       // :225
                    selectSprite.angle = 0;                        // :226
                    phase = Phase.CHOOSE;                          // :227 pbChoosingScene
                    phaseTick = 0;
                }
                break;
            case CHOOSE:
                if (input.wasPressed(GameAction.RIGHT) && select < 3) {            // :248
                    int oldsel = select;
                    select += 1;
                    afterMove(oldsel);
                    selection.y = 188 - 154;                                       // :262
                    selection.x = xOf(select) - 28 + 64;                           // :261
                }
                if (input.wasPressed(GameAction.LEFT) && select > 1) {             // :270
                    int oldsel = select;
                    select -= 1;
                    afterMove(oldsel);
                    selection.y = 188 - 122;                                       // :288
                    selection.x = xOf(select) - 28;                                // :287
                }
                if (input.wasPressed(GameAction.CONFIRM)) {                        // :294
                    MenuSe.decision(context.audioManager());                       // :298
                    pbChooseBall();                                                // :299
                }
                break;
            case CONFIRM:
                if (input.wasPressed(GameAction.DOWN) && choicesel != 2) {         // :522
                    MenuSe.cursor(context.audioManager());
                    choicesel += 1;
                    setSrc(choice1, 0, 0);
                    setSrc(choice2, 140, 48);
                    choicesel_.y += 46;
                }
                if (input.wasPressed(GameAction.UP) && choicesel != 1) {           // :530
                    MenuSe.cursor(context.audioManager());
                    choicesel -= 1;
                    setSrc(choice1, 0, 48);
                    setSrc(choice2, 140, 0);
                    choicesel_.y -= 46;
                }
                if (input.wasPressed(GameAction.CONFIRM) && choicesel == 1) {      // :538
                    choiceBoxesOff();
                    MenuSe.decision(context.audioManager());
                    confirmed = true;
                    afterConfirm(true);
                } else if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)
                        || (input.wasPressed(GameAction.CONFIRM) && choicesel == 2)) {   // :544
                    choiceBoxesOff();
                    MenuSe.close(context.audioManager());                          // pbPlayCancelSE
                    choicesel_.opacity = 255;
                    afterConfirm(false);
                }
                break;
            default:
                break;
        }
    }

    /** The shared tail of the RIGHT / LEFT moves (:251-260, :273-285). */
    private void afterMove(int oldsel) {
        oldX = (int) ball[select - 1].x;                           // @oldx = @sprites["ball_#{@select}"].x
        frame = 0;
        MenuSe.cursor(context.audioManager());
        ball[0].x = oldsel == 1 ? 134 + 64 : ball[0].x;
        ball[1].x = oldsel == 2 ? 288 + 64 : ball[1].x;
        ball[2].x = oldsel == 3 ? 442 + 64 : ball[2].x;
        ball[oldsel - 1].angle = 0;
        shadow[0].x = oldsel == 1 ? 88 + 64 : shadow[0].x;
        shadow[1].x = oldsel == 2 ? 242 + 64 : shadow[1].x;
        shadow[2].x = oldsel == 3 ? 396 + 64 : shadow[2].x;
        selectSprite.x = xOf(select);
        selectSprite.y = 188;
        selframe = 0;
        selectSprite.angle = 0;
    }

    private static int xOf(int slot) {
        return slot == 1 ? 134 + 64 : slot == 2 ? 288 + 64 : 442 + 64;      // @x = [nil,134+64,288+64,442+64]
    }

    // =====================================================================
    // Ticks (one 40 fps frame each)
    // =====================================================================

    private void tick() {
        frameCount++;
        switch (phase) {
            case OPEN:                                              // :168-183 pbOpenScene, 25.times
                starterbg.opacity += 200 / 20;
                base.opacity += 255 / 20;
                for (int i = 0; i < 3; i++) {
                    shadow[i].opacity += 255 / 20;
                    ball[i].opacity += 255 / 20;
                }
                textwnd.opacity += 255 / 20;
                clampAll();
                if (++phaseTick >= 25) {
                    setText("<ac>要选择哪只宝可梦？</ac>");           // :184
                    phase = Phase.ASK;                              // :185 pbStartChoosing
                    phaseTick = 0;
                }
                break;
            case ASK:
                break;
            case CHOOSE:
                pbAnimation();
                break;
            case BALL_A:                                            // :376-387
                if (frame < 16) {
                    pbAnimation();
                }
                typebg.opacity += (int) starterbg.opacity / 18;
                ballbase.opacity += 255 / 18;
                base.opacity -= 105 / 10;
                for (int i = 0; i < 3; i++) {
                    shadow[i].opacity -= 155 / 10;
                    ball[i].opacity -= 105 / 10;
                }
                clampAll();
                if (++phaseTick >= 20) {
                    phase = Phase.BALL_B;
                    phaseTick = 0;
                }
                break;
            case BALL_B:                                            // :388-402
                if (select == 1) ballbase.x += 154 / 20;
                if (select == 3) ballbase.x -= 154 / 20;
                base.y += 40 / 20;
                for (int i = 0; i < 3; i++) {
                    ball[i].y += 40 / 20;
                }
                ballbase.y -= 40 / 20;
                if (++phaseTick >= 20) {
                    phase = Phase.BALL_C;
                    phaseTick = 0;
                }
                break;
            case BALL_C:                                            // :403-408
                if (select == 1) ballbase.x += 6;
                if (select == 3) ballbase.x -= 6;
                if (++phaseTick >= 2) {
                    if (select != 1) {
                        ballbase.x = ball[1].x;                     // :409
                    }
                    phase = Phase.PKMN_IN;
                    phaseTick = 0;
                }
                break;
            case PKMN_IN:                                           // :410-413
                pkmn[select - 1].opacity += 255 / 10;
                clampAll();
                if (++phaseTick >= 10) {
                    if (context.audioManager() != null && data[select - 1] != null) {
                        context.audioManager().playCry(data[select - 1].id);   // :414 pbSEPlay("%03dCry")
                    }
                    choiceBoxesOn();                                // :415
                    phase = Phase.CONFIRM;
                    phaseTick = 0;
                }
                break;
            case CONFIRM:                                           // :517-519 choicesel pulse
                choicesel_.opacity = pulse();
                break;
            case CANCEL_A:                                          // :430-433 10.times
                pkmn[select - 1].opacity -= 255 / 10;
                clampAll();
                if (++phaseTick >= 10) {
                    phase = Phase.CANCEL_B;
                    phaseTick = 0;
                }
                break;
            case CANCEL_B:                                          // :434-439 2.times
                if (select == 1) ballbase.x -= 6;
                if (select == 3) ballbase.x += 6;
                if (++phaseTick >= 2) {
                    phase = Phase.CANCEL_C;
                    phaseTick = 0;
                }
                break;
            case CANCEL_C:                                          // :440-451 20.times
                if (select == 1) ballbase.x -= 154 / 20;
                if (select == 3) ballbase.x += 154 / 20;
                base.y -= 40 / 20;
                for (int i = 0; i < 3; i++) {
                    ball[i].y -= 40 / 20;
                }
                ballbase.y += 40 / 20;
                if (++phaseTick >= 20) {
                    if (select != 1) {
                        ballbase.x = ball[select - 1].x;            // :453
                    }
                    selectSprite.visible = true;                    // :454
                    selection.visible = true;                       // :455
                    phase = Phase.CANCEL_D;
                    phaseTick = 0;
                }
                break;
            case CANCEL_D:                                          // :456-466 20.times
                typebg.opacity -= (int) starterbg.opacity / 18;
                ballbase.opacity -= 255 / 18;
                base.opacity += 105 / 10;
                for (int i = 0; i < 3; i++) {
                    shadow[i].opacity += 155 / 10;
                    ball[i].opacity += 105 / 10;
                }
                clampAll();
                if (++phaseTick >= 20) {
                    phase = Phase.CHOOSE;                           // back to pbChoosingScene's loop
                    phaseTick = 0;
                }
                break;
            case CLOSE:                                             // :548-563 pbCloseScene, 25.times
                starterbg.opacity -= 255 / 20;
                typebg.opacity -= 255 / 20;
                ballbase.opacity -= 255 / 20;
                pkmn[select - 1].opacity -= 255 / 20;
                textwnd.opacity -= 255 / 20;
                clampAll();
                if (++phaseTick >= 25) {
                    phase = Phase.DONE;
                    finished = true;
                }
                break;
            default:
                break;
        }
        tickText();
    }

    private void clampAll() {
        starterbg.clamp();
        typebg.clamp();
        base.clamp();
        ballbase.clamp();
        textwnd.clamp();
        for (int i = 0; i < 3; i++) {
            shadow[i].clamp();
            ball[i].clamp();
            pkmn[i].clamp();
        }
    }

    /** :189-205 pbAnimation: the ball sways, the hand bobs, the outline pulses. */
    private void pbAnimation() {
        Sprite b = ball[select - 1];
        Sprite s = shadow[select - 1];
        if (frame < 4) {                                            // :190
            b.x -= 2;
            b.angle += 2;
            s.x -= 2;
            selectSprite.x -= 2;
            selectSprite.angle += 2;
        } else if (frame >= 4 && frame < 10) {                      // :197
            b.x += 2;
            b.angle -= 2;
            s.x += 2;
            selectSprite.x += 2;
            selectSprite.angle -= 2;
        } else if (frame >= 10 && frame < 15) {                     // :204
            b.x -= 2;
            b.angle += 2;
            s.x -= 2;
            selectSprite.x -= 2;
            selectSprite.angle += 2;
        } else if (frame >= 15 && frame < 16) {                     // :211
            b.x = oldX;
            b.angle = 0;
            selectSprite.x = oldX;
            selectSprite.angle = 0;
            s.x = oldX - 46;
        } else if (frame == 50) {                                   // :218
            frame = 0;
        }
        frame += 1;                                                 // :221
        if (selframe < 15) {                                        // :222
            selection.y += 1;
        } else if (selframe >= 15 && selframe < 22) {               // :224
            selection.y -= 2;
        } else if (selframe == 22) {                                // :226
            selframe = 0;
        }
        selframe += 1;                                              // :229
        selectSprite.opacity = pulse();                             // :230-232
    }

    /** {@code intensity = (Graphics.frame_count%40)*12; intensity = 480-intensity if intensity>240}. */
    private float pulse() {
        int intensity = (frameCount % 40) * 12;
        if (intensity > 240) {
            intensity = 480 - intensity;
        }
        return intensity;
    }

    /** :315-359 type names and the background tone for the chosen starter's first type. */
    private void pbChooseBall() {
        PbsData.Species s = data[select - 1];
        String type = s == null ? "NORMAL" : s.type(0);
        applyType(type == null ? "NORMAL" : type);
        String pkmnName = s == null ? "" : s.name;
        textY = 352f - 16f;                                         // :360 textbox.y -= 16
        setText("<ac>这里面的是" + pkmnName + ", <br>它是一只" + typeName + "属性的宝可梦。</ac>");   // :361
        ballbase.x = xOf(select);                                   // :363
        ballbase.y = 188;                                           // :364
        selectSprite.visible = false;                               // :366
        selection.visible = false;                                  // :367
        phase = Phase.BALL_A;
        phaseTick = 0;
    }

    private void applyType(String type) {
        switch (type) {
            case "NORMAL": typeName = "一般"; typeTone = new float[] {95.25f, 88.5f, 63.0f}; break;
            case "FIGHTING": typeName = "格斗"; typeTone = new float[] {126.75f, 37.5f, 31.5f}; break;
            case "FLYING": typeName = "飞行"; typeTone = new float[] {109.0f, 90.0f, 121.0f}; break;
            case "POISON": typeName = "毒"; typeTone = new float[] {87.25f, 31.0f, 82.75f}; break;
            case "GROUND": typeName = "地面"; typeTone = new float[] {108.0f, 91.0f, 43.0f}; break;
            case "ROCK": typeName = "岩石"; typeTone = new float[] {83.0f, 74.0f, 28.0f}; break;
            case "BUG": typeName = "虫"; typeTone = new float[] {90.0f, 108.0f, 2.0f}; break;
            case "GHOST": typeName = "幽灵"; typeTone = new float[] {58.55f, 43.0f, 95.25f}; break;
            case "STEEL": typeName = "刚"; typeTone = new float[] {79.5f, 79.5f, 79.5f}; break;
            case "QMARKS": typeName = "虚无"; typeTone = new float[] {63.0f, 95.25f, 79.5f}; break;
            case "FIRE": typeName = "火"; typeTone = new float[] {169.0f, 93.0f, 42.0f}; break;
            case "WATER": typeName = "水"; typeTone = new float[] {42.0f, 96.0f, 169.0f}; break;
            case "GRASS": typeName = "草"; typeTone = new float[] {65.25f, 118.5f, 39.0f}; break;
            case "ELECTRIC": typeName = "电"; typeTone = new float[] {135.0f, 126.0f, 23.25f}; break;
            case "PSYCHIC": typeName = "超能"; typeTone = new float[] {128.25f, 51.75f, 96.0f}; break;
            case "ICE": typeName = "冰"; typeTone = new float[] {55.5f, 102.75f, 102.75f}; break;
            case "DRAGON": typeName = "龙"; typeTone = new float[] {54.75f, 43.5f, 114.75f}; break;
            case "DARK": typeName = "恶"; typeTone = new float[] {-23.0f, -40.0f, -56.0f}; break;
            case "FAIRY": typeName = "妖精"; typeTone = new float[] {136.75f, 41.5f, 73.0f}; break;
            default: typeName = "一般"; typeTone = new float[] {95.25f, 88.5f, 63.0f}; break;   // no match: @type keeps its last value
        }
        typeToneSet = true;
    }

    /** :140-167 pbChoiceBoxes(0) */
    private void choiceBoxesOn() {
        setSrc(choice1, 0, 48);                                     // :147 choice1 highlighted
        setSrc(choice2, 140, 0);
        choice1.visible = true;
        choice2.visible = true;
        choicesel_.visible = true;
        choicesel_.y = 174;
        choicesel_.opacity = 255;
        choicesel = 1;                                              // :164
    }

    /** :166-178 pbChoiceBoxes(1) */
    private void choiceBoxesOff() {
        choice1.visible = false;
        choice2.visible = false;
        choicesel_.y = 174;
        choicesel_.visible = false;
        choicesel = 1;
    }

    private void afterConfirm(boolean yes) {
        if (yes) {
            textVisible = false;                                    // :418 textbox.visible=false
            ball[0].visible = false;                                // :548-557 pbCloseScene
            ball[1].visible = false;
            ball[2].visible = false;
            for (Sprite s : shadow) {
                s.visible = false;
            }
            base.visible = false;
            selectSprite.visible = false;
            selection.visible = false;
            phase = Phase.CLOSE;
            phaseTick = 0;
        } else {
            textY = 352f;                                           // :424 textbox.y = @oldMsgY
            setText("<ac>要选择哪只宝可梦？</ac>");                   // :425
            phase = Phase.CANCEL_A;
            phaseTick = 0;
        }
    }

    // =====================================================================
    // The message window (letter by letter)
    // =====================================================================

    private void setText(String markup) {
        textLines = new ArrayList<>();
        String plain = markup.replace("<ac>", "").replace("</ac>", "");
        for (String line : plain.split("<br>")) {
            textLines.add(line);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < textLines.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(textLines.get(i));
        }
        textFull = sb.toString();
        textShown = 0;
        textCounter = 0;
    }

    private void tickText() {
        if (textShown >= textFull.length()) {
            return;
        }
        int speed = context.settings().textspeed;
        int step = speed == 0 ? ((textCounter++ % 3 == 0) ? 1 : 0) : speed == 2 ? 3 : 1;
        textShown = Math.min(textFull.length(), textShown + step);
    }

    // =====================================================================
    // Render
    // =====================================================================

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, WindowSkin speech, MenuFont detail) {
        loadAssets(a);
        float h = ScreenMetrics.logicalHeight();
        float w = ScreenMetrics.logicalWidth();
        // :40-48 starterbg gets a white colour blend (105/255), typebg a type tone.
        draw(b, starterbg, h);
        if (starterbg.opacity > 0f && starterbg.texture != null) {
            b.setColor(1f, 1f, 1f, starterbg.opacity / 255f * 105f / 255f);
            b.draw(a.pixel(), 0f, 0f, w, h);
            b.setColor(Color.WHITE);
        }
        if (typebg.opacity > 0f && typeToneSet) {
            if (!toneReady && tone.isCompiled()) {
                toneReady = true;
            }
            if (toneReady) {
                b.setShader(tone.program());
                tone.setTone(typeTone[0], typeTone[1], typeTone[2], 0f);
            }
            draw(b, typebg, h);
            if (toneReady) {
                b.setShader(null);
            }
        }
        draw(b, base, h);
        for (Sprite s : shadow) {
            draw(b, s, h);
        }
        for (Sprite s : ball) {
            draw(b, s, h);
        }
        draw(b, selectSprite, h);
        draw(b, selection, h);
        draw(b, ballbase, h);
        for (Sprite s : pkmn) {
            draw(b, s, h);
        }
        draw(b, textwnd, h);
        if (textVisible) {
            drawText(b, f, w, h);
        }
        draw(b, choice1, h);
        draw(b, choice2, h);
        draw(b, choicesel_, h);
        if (choice1.visible) {                                      // :155-162 the two labels
            Color shadowColor = new Color(140f / 255f, 140f / 255f, 140f / 255f, 1f);
            f.draw(b, "确定", 420f, h - (185f + (32f - f.lineHeight()) / 2f), Color.WHITE, shadowColor);
            f.draw(b, "取消", 420f, h - (231f + (32f - f.lineHeight()) / 2f), Color.WHITE, shadowColor);
        }
    }

    private void draw(SpriteBatch b, Sprite s, float h) {
        if (!s.visible || s.texture == null || s.opacity <= 0f) {
            return;
        }
        float width = s.srcW;
        float height = s.srcH;
        float left = s.centered ? s.x - width / 2f : s.x;
        float top = s.centered ? s.y - height / 2f : s.y;
        float gdxY = h - top - height;
        b.setColor(1f, 1f, 1f, s.opacity / 255f);
        TextureRegion region = new TextureRegion(s.texture, s.srcX, s.srcY, s.srcW, s.srcH);
        b.draw(region, left, gdxY, width / 2f, height / 2f, width, height, 1f, 1f, s.angle);
        b.setColor(Color.WHITE);
    }

    /** The "nil skin" message window: white text with the (165,165,173) shadow, {@code <ac>} centred. */
    private void drawText(SpriteBatch b, MenuFont f, float w, float h) {
        Color base = Color.WHITE;
        Color shadowColor = new Color(165f / 255f, 165f / 255f, 173f / 255f, 1f);
        String visible = textFull.substring(0, Math.min(textShown, textFull.length()));
        String[] lines = visible.split("\n", -1);
        for (int i = 0; i < lines.length && i < 2; i++) {
            float lineWidth = f.width(lines[i]);
            float x = 16f + (w - 32f - lineWidth) / 2f;
            float top = textY + 16f + i * 32f + (32f - f.lineHeight()) / 2f;
            f.draw(b, lines[i], x, h - top, base, shadowColor);
        }
    }

    public void dispose() {
        tone.dispose();
    }
}
