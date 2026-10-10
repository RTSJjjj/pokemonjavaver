package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PokemonStats;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.TrainerState;
import pokemon.runtime.ui.WindowSkin;

import java.util.ArrayList;
import java.util.List;

/**
 * 232_PScreen_HallOfFame: {@code HallOfFame_Scene} + {@code HallOfFameScreen}. {@link #entry} is {@code pbHallOfFameEntry}
 * (the team is recorded, walks in one by one with its data, then the trainer and the title; the screen fades out) and
 * {@link #pc} is {@code pbHallOfFamePC} (browse the recorded teams). Everything counts in 40 fps ticks: the plugin's
 * {@code Graphics.update} loops are tick counters here.
 *
 * <p>登记: the PokemonSprite is the still front sprite (no frame animation, no battler metrics); the left data box
 * ({@code Window_AdvancedTextPokemon}) is drawn as a system-frame window with the {@code <r>} columns; the play time is the
 * trainer's play time (the plugin reads {@code Graphics.frame_count / 40}).</p>
 */
public final class HallOfFameView {
    // ---- the constants of the scene (:21-48)
    private static final boolean SINGLEROW = false;
    private static final boolean ANIMATION = true;
    private static final int ANIMATIONSPEED = 32;
    private static final int ENTRYWAITTIME = 64;                                // 1/20 s
    private static final int HALLLIMIT = 50;
    private static final String ENTRYMUSIC = "Hall of Fame";
    private static final boolean ALLOWEGGS = true;
    private static final boolean REMOVEBARS = true;
    private static final int OPACITY = 64;
    private static final Color BASECOLOR = new Color(248f / 255f, 248f / 255f, 248f / 255f, 1f);
    private static final Color SHADOWCOLOR = new Color(0f, 0f, 0f, 1f);

    private enum Phase { FADE_IN, ANIM, TITLE, TITLE_WAIT, FADE_OUT, PC, PC_FADE_OUT, DONE }

    private final RuntimeContext context;
    private final TrainerState trainer;
    private final boolean pcMode;
    private final MenuClock clock = new MenuClock();
    private final SceneMessage titleMessage;

    private List<Pokemon> hallEntry = new ArrayList<>();
    private int hallIndex;
    private int battlerIndex;
    private int[] xmovement = new int[8];
    private int[] ymovement = new int[8];
    private float[] px = new float[6];
    private float[] py = new float[6];
    private int[] opacity = new int[6];
    private Texture[] textures = new Texture[6];
    private Texture[] loadedFor = new Texture[6];
    private boolean texturesLoaded;
    private boolean useMusic;
    private boolean alreadyFadedInEnd;
    private boolean finished;
    private Phase phase = Phase.FADE_IN;
    private int tick;
    private int wait;
    private Runnable afterWait;
    private float blackAlpha = 255f;

    // sprites of the scene
    private boolean barsVisible = true;
    private boolean trainerVisible;
    private float trainerX;
    private float trainerY;
    private Texture trainerTexture;
    private boolean messageBoxVisible;
    private List<String[]> messageBoxRows = new ArrayList<>();
    private List<String[]> overlayLines = new ArrayList<>();                    // {text, x, y, align}

    private HallOfFameView(RuntimeContext context, boolean pcMode) {
        this.context = context;
        this.trainer = context.gameState().trainer();
        this.pcMode = pcMode;
        this.titleMessage = new SceneMessage(context);
        if (audio() != null) audio().memorizeBgmAndBgs();
    }

    /** {@code pbHallOfFameEntry} (:524-528). */
    public static HallOfFameView entry(RuntimeContext context) {
        HallOfFameView view = new HallOfFameView(context, false);
        view.startEntry();
        return view;
    }

    /** {@code pbHallOfFamePC} (:530-534). */
    public static HallOfFameView pc(RuntimeContext context) {
        HallOfFameView view = new HallOfFameView(context, true);
        view.startPc();
        return view;
    }

    private AudioManager audio() {
        return context.audioManager();
    }

    public boolean finished() {
        return finished;
    }

    private float width() {
        return ScreenMetrics.logicalWidth();
    }

    private float height() {
        return ScreenMetrics.logicalHeight();
    }

    // =====================================================================
    // pbStartSceneEntry / pbStartScenePC (:52-85)
    // =====================================================================

    private void startEntry() {
        battlerIndex = 0;
        useMusic = ENTRYMUSIC != null && !ENTRYMUSIC.isEmpty();                    // :64
        if (useMusic && audio() != null) audio().playBgm(ENTRYMUSIC);              // :65
        saveHallEntry();                                                           // :66
        createBattlers(true);                                                      // :69
    }

    private void startPc() {
        battlerIndex = 0;
        hallIndex = trainer.hallOfFame.size() - 1;                                 // :74
        hallEntry = trainer.hallOfFame.get(trainer.hallOfFame.size() - 1);         // :75
        createBattlers(false);                                                     // :76
    }

    /** {@code saveHallEntry} (:143-154). */
    private void saveHallEntry() {
        hallEntry = new ArrayList<>();
        for (Pokemon member : trainer.party.members()) {
            if (!member.egg || ALLOWEGGS) hallEntry.add(member.copy());            // :146-147 clone
        }
        trainer.hallOfFame.add(hallEntry);                                         // :149
        trainer.hallOfFameLastNumber += 1;                                         // :150
        if (HALLLIMIT > -1 && trainer.hallOfFame.size() > HALLLIMIT) {             // :151-152
            trainer.hallOfFame.remove(0);
        }
    }

    // =====================================================================
    // positions (:156-205)
    // =====================================================================

    private static int xpositionformula(int n) {
        if (!SINGLEROW) {
            return (n / 3 % 2 == 0) ? Math.floorMod(19 - n, 3) : Math.floorMod(19 + n, 3);
        }
        return n % 2 * 2;
    }

    private static int ypositionformula(int n) {
        if (!SINGLEROW) {
            return (n / 3) % 2 * 2;
        }
        return 1;
    }

    private int xpointformula(int n) {
        int ret;
        if (!SINGLEROW) {
            ret = 32 + 200 * xpositionformula(n);
        } else {
            ret = (60 * (n / 2) + 48) * (xpositionformula(n) - 1);
            ret += (int) width() / 2 - 56;
        }
        return ret + 32;
    }

    private int ypointformula(int n) {
        if (!SINGLEROW) {
            return 32 + 128 * ypositionformula(n) / 2;
        }
        return 96 - 8 * (n / 2);
    }

    // =====================================================================
    // createBattlers (:228-263)
    // =====================================================================

    private Texture battlerTexture(MenuAssets a, Pokemon pok) {
        if (pok.egg) {
            String name = pok.species == null ? "" : pok.species.internalName;
            int id = pok.species == null ? 0 : pok.species.id;
            int form = pok.formIndex();
            for (String candidate : new String[] {name + "egg_" + form, String.format("%03degg_%d", id, form), name + "egg",
                    String.format("%03degg", id), "egg"}) {
                Texture t = a.graphic("Battlers", candidate);
                if (t != null) return t;
            }
            return null;
        }
        return EvolutionView.battler(a, pok.species, pok.effectiveGender() == PokemonStats.FEMALE, pok.shiny,
                pok.superShiny, pok.formIndex());
    }

    private void createBattlers(boolean hide) {
        xmovement = new int[8];
        ymovement = new int[8];
        texturesLoaded = false;                                                    // textures are (re)resolved on the next draw
        for (int i = 0; i < 6; i++) {
            opacity[i] = 255;
            if (i >= hallEntry.size()) continue;                                   // :236
            px[i] = xpointformula(i);
            py[i] = ypointformula(i);
        }
        pendingHide = hide;
    }

    private boolean pendingHide;

    /** The second half of createBattlers, which needs the bitmap sizes (:240-262). */
    private void placeBattlers(MenuAssets a) {
        for (int i = 0; i < 6; i++) {
            if (i >= hallEntry.size()) {
                textures[i] = null;
                continue;
            }
            Texture t = battlerTexture(a, hallEntry.get(i));
            textures[i] = t;
            px[i] = xpointformula(i);
            py[i] = ypointformula(i);
            if (t != null) {
                px[i] += (128 - t.getWidth()) / 2;                                 // :246-247
                py[i] += (128 - t.getHeight()) / 2;
            }
            if (!pendingHide) continue;                                            // :249 next if !hide
            int horizontal = 1 - xpositionformula(i);                              // :251
            int vertical = 1 - ypositionformula(i);
            int bw = t == null ? 0 : t.getWidth();
            int bh = t == null ? 0 : t.getHeight();
            int xdistance = (horizontal == -1) ? -bw : (int) width();
            int ydistance = (vertical == -1) ? -bh : (int) height();
            xdistance = Math.abs(Math.floorDiv(xdistance - (int) px[i], ANIMATIONSPEED)) + 1;   // :254
            ydistance = Math.abs(Math.floorDiv(ydistance - (int) py[i], ANIMATIONSPEED)) + 1;
            int bigger = Math.max(xdistance, ydistance);                           // :256
            xmovement[i] = bigger;
            if (horizontal == -1) xmovement[i] *= -1;
            if (horizontal == 0) xmovement[i] = 0;
            ymovement[i] = bigger;
            if (vertical == -1) ymovement[i] *= -1;
            if (vertical == 0) ymovement[i] = 0;
            px[i] += xmovement[i] * ANIMATIONSPEED;                                // :260-261 hide the battlers
            py[i] += ymovement[i] * ANIMATIONSPEED;
        }
        pendingHide = false;
        texturesLoaded = true;
    }

    private void setPokemonSpritesOpacity(int index, int value) {
        for (int n = 0; n < hallEntry.size() && n < 6; n++) {
            opacity[n] = (n == index) ? 255 : value;                               // :134-136
        }
    }

    // =====================================================================
    // text (:312-420)
    // =====================================================================

    private void line(String text, float x, float y, int align) {
        overlayLines.add(new String[] {text, String.valueOf(x), String.valueOf(y), String.valueOf(align)});
    }

    private void writePokemonData(Pokemon pokemon, int hallNumber) {
        overlayLines.clear();
        String pokename = pokemon.name;
        String speciesname = pokemon.species == null ? "" : pokemon.species.name;
        int gender = pokemon.effectiveGender();
        if (gender == PokemonStats.MALE) {
            speciesname += "♂";
        } else if (gender == PokemonStats.FEMALE) {
            speciesname += "♀";
        }
        pokename += "/" + speciesname;
        if (pokemon.egg) pokename = "宝可梦蛋/宝可梦蛋";
        String idno = (pokemon.originalTrainer == null || pokemon.originalTrainer.isEmpty() || pokemon.egg)
                ? "?????" : String.format("%05d", pokemon.publicID & 0xFFFF);
        String dexnumber = pokemon.egg ? "No.???" : String.format("No. %03d", pokemon.species == null ? 0 : pokemon.species.id);
        float w = width();
        float h = height();
        line(dexnumber, 32, h - 80, 0);                                            // :330
        line(pokename, w - 192, h - 80, 2);
        line("Lv." + (pokemon.egg ? "?" : String.valueOf(pokemon.level)), 64, h - 48, 0);
        line("IDNo." + (pokemon.egg ? "?????" : idno), w - 192, h - 48, 2);
        if (hallNumber > -1) {                                                     // :336
            line("冠军殿堂No.", w / 2 - 104, 0, 0);
            line(String.valueOf(hallNumber), w / 2 + 104, 0, 1);
        }
    }

    private void writeWelcome() {
        overlayLines.clear();
        String welcome = "";
        switch (context.gameState().variables().get(100)) {                        // :344 $game_variables[100]
            case 2: welcome = "【昕纪元影辞平均等级模式】已达成！"; break;
            case 1: welcome = "【昕纪元影辞最高等级模式】已达成！"; break;
            case 0: welcome = "【昕纪元影辞简单难度】已达成！"; break;
            default: break;
        }
        if (context.gameState().switches().get(197) || context.gameState().switches().get(42)) {
            welcome = "【昕纪元影辞懒狗难度】已达成！";
        }
        line(welcome, width() / 2, height() - 80, 2);
        line("欢迎进入荣耀殿堂！", width() / 2, height() - 50, 2);
    }

    /** {@code createTrainerBattler} (:265-302). */
    private void createTrainerBattler() {
        trainerVisible = true;
        trainerX = width() - 160;
        trainerY = 160 + 64;
        trainerLoadPending = true;
        if (REMOVEBARS) {
            overlayLines.clear();
            barsVisible = false;
        }
        int slot = hallEntry.size();                                               // @battlerIndex
        xmovement[slot] = 0;
        ymovement[slot] = 0;
        if (ANIMATION && !SINGLEROW) {
            int startpoint = (int) width() / 2;
            xmovement[slot] = Math.floorDiv(startpoint - (int) trainerX, 2);
            trainerX = startpoint;
        }
    }

    private boolean trainerLoadPending;

    /** {@code writeTrainerData} (:304-342). */
    private void writeTrainerData() {
        long totalsec = (long) trainer.playSeconds;
        long hour = totalsec / 60 / 60;
        long min = totalsec / 60 % 60;
        String pubid = String.format("%05d", trainer.publicID() & 0xFFFF);
        messageBoxRows.clear();
        messageBoxRows.add(new String[] {"姓名", trainer.name});
        messageBoxRows.add(new String[] {"IDNo.", pubid});
        messageBoxRows.add(new String[] {"时间", String.format("%02d:%02d", hour, min)});
        messageBoxRows.add(new String[] {"图鉴", trainer.owned.size() + "/" + trainer.seen.size()});
        messageBoxVisible = true;
        boolean postGame = context.gameState().switches().get(44);                 // :318 二周目结束开关
        boolean conte = context.gameState().switches().get(45);                    // :319 康特联盟通关开关
        String title = "柊埃联盟冠军级训练家！\n恭喜！";                              // :313 default
        if (postGame && conte) {
            title = "柊埃联盟冠军级训练家！\n恭喜！";
        } else if (conte && !postGame) {
            title = "通关康特联盟！\n恭喜！";
        } else if (postGame && !conte) {
            title = "【回响之章】完结！\n请继续冒险吧！";
        }
        titleMessage.show(title + "\\wtnp[0]", () -> {                             // \^: no wait for input
            phase = Phase.TITLE_WAIT;
            wait = ENTRYWAITTIME * 40 / 20;
        });
        phase = Phase.TITLE;
    }

    // =====================================================================
    // update
    // =====================================================================

    /** @return true when the whole screen is over */
    public boolean update(InputManager input) {
        int ticks = clock.advance();
        if (phase == Phase.PC) {
            updatePcInput(input);
        }
        if (phase == Phase.TITLE) {
            titleMessage.input(input.wasPressed(GameAction.CONFIRM), input.wasPressed(GameAction.CANCEL));
        }
        for (int i = 0; i < ticks && !finished; i++) {
            step();
        }
        return finished;
    }

    private void step() {
        switch (phase) {
            case FADE_IN:                                                          // pbFadeInAndShow
                blackAlpha = Math.max(0f, Math.min(255f, (16 - tick) * 16f));
                if (++tick > 16) {
                    tick = 0;
                    blackAlpha = 0f;
                    if (pcMode) {
                        phase = Phase.PC;
                        pbUpdatePC();                                              // :79 after the fade
                    } else {
                        phase = Phase.ANIM;
                    }
                }
                break;
            case ANIM:
                animTick();
                break;
            case TITLE:
                titleMessage.tick();
                break;
            case TITLE_WAIT:
                if (wait > 0) {
                    wait--;
                    break;
                }
                if (useMusic && audio() != null) audio().fadeBgm(256f / 20f);      // :432 pbBGMFade((2**fadeSpeed).to_f/20)
                phase = Phase.FADE_OUT;
                tick = 0;
                break;
            case FADE_OUT:                                                         // slowFadeOut(sprites, 8): j in 0..256
                blackAlpha = Math.min(255f, tick);
                if (++tick > 256) {
                    alreadyFadedInEnd = true;
                    battlerIndex += 1;
                    endScene();
                }
                break;
            case PC_FADE_OUT:                                                      // pbEndScene -> pbFadeOutAndHide
                blackAlpha = Math.min(255f, tick * 16f);
                if (++tick > 16) {
                    endScene();
                }
                break;
            default:
                break;
        }
    }

    private void endScene() {
        if (useMusic && audio() != null) audio().restoreBgmAndBgs();               // :93 $game_map.autoplay
        phase = Phase.DONE;
        finished = true;
    }

    /** {@code moveSprite(i)} (:207-226) for one tick; {@code i == -1} is the trainer. */
    private void moveSprite(int i) {
        boolean isTrainer = i < 0;
        int slot = isTrainer ? hallEntry.size() : i;
        int speed = isTrainer ? 2 : ANIMATIONSPEED;
        if (xmovement[slot] != 0) {
            int direction = xmovement[slot] > 0 ? -1 : 1;
            if (isTrainer) trainerX += speed * direction; else px[i] += speed * direction;
            xmovement[slot] += direction;
        }
        if (ymovement[slot] != 0) {
            int direction = ymovement[slot] > 0 ? -1 : 1;
            if (isTrainer) trainerY += speed * direction; else py[i] += speed * direction;
            ymovement[slot] += direction;
        }
    }

    /** {@code pbUpdateAnimation} (:386-436), one call per tick; its inner wait loops are {@code wait}. */
    private void animTick() {
        int size = hallEntry.size();
        if (wait > 0) {
            wait--;
            if (wait == 0 && afterWait != null) {
                Runnable then = afterWait;
                afterWait = null;
                then.run();
            }
            return;
        }
        if (battlerIndex <= size) {                                                // :387
            int slot = battlerIndex;
            if (xmovement[slot] != 0 || ymovement[slot] != 0) {                    // :388
                moveSprite(battlerIndex < size ? battlerIndex : -1);               // :389-390
            } else {
                battlerIndex += 1;                                                 // :392
                if (battlerIndex <= size) {                                        // :393
                    if (audio() != null && hallEntry.get(battlerIndex - 1).species != null) {
                        audio().playCry(hallEntry.get(battlerIndex - 1).species.id);   // :396 pbPlayCry
                    }
                    writePokemonData(hallEntry.get(battlerIndex - 1), -1);         // :397
                    wait = ENTRYWAITTIME * 40 / 20;                                // :398
                    afterWait = () -> {
                        if (battlerIndex < size) {                                 // :402
                            setPokemonSpritesOpacity(battlerIndex, OPACITY);       // :403
                            overlayLines.clear();                                  // :404
                        } else {                                                   // :405
                            setPokemonSpritesOpacity(-1, 255);                     // :406
                            writeWelcome();                                        // :407
                            wait = ENTRYWAITTIME * 2 * 40 / 20;                    // :408
                            afterWait = () -> {
                                if (!SINGLEROW) setPokemonSpritesOpacity(-1, OPACITY);   // :413
                                createTrainerBattler();                            // :414
                            };
                        }
                    };
                }
            }
        } else if (battlerIndex > size) {                                          // :418
            writeTrainerData();                                                    // :420
        }
    }

    // ---- the PC browser (:438-476)

    private void updatePcInput(InputManager input) {
        boolean continueScene = true;
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {   // :362 Exits
            phase = Phase.PC_FADE_OUT;
            tick = 0;
            return;
        }
        if (input.wasPressed(GameAction.CONFIRM)) {                                // :363 one entry backward
            battlerIndex += 10;
            continueScene = pbUpdatePC();
        }
        if (input.wasPressed(GameAction.LEFT)) {
            battlerIndex -= 1;
            continueScene = pbUpdatePC();
        }
        if (input.wasPressed(GameAction.RIGHT)) {
            battlerIndex += 1;
            continueScene = pbUpdatePC();
        }
        if (!continueScene) {                                                      // :376
            phase = Phase.PC_FADE_OUT;
            tick = 0;
        }
    }

    /** {@code pbUpdatePC} (:438-466). */
    private boolean pbUpdatePC() {
        if (battlerIndex >= hallEntry.size()) {                                    // change the team
            hallIndex -= 1;
            if (hallIndex == -1) return false;
            hallEntry = trainer.hallOfFame.get(hallIndex);
            battlerIndex = 0;
            createBattlers(false);
        } else if (battlerIndex < 0) {
            hallIndex += 1;
            if (hallIndex >= trainer.hallOfFame.size()) return false;
            hallEntry = trainer.hallOfFame.get(hallIndex);
            battlerIndex = hallEntry.size() - 1;
            createBattlers(false);
        }
        Pokemon current = hallEntry.get(battlerIndex);                             // change the pokemon
        if (audio() != null && current.species != null) audio().playCry(current.species.id);   // pbPlayCry
        setPokemonSpritesOpacity(battlerIndex, OPACITY);
        int hallNumber = trainer.hallOfFameLastNumber + hallIndex - trainer.hallOfFame.size() + 1;
        writePokemonData(current, hallNumber);
        return true;
    }

    // =====================================================================
    // render
    // =====================================================================

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin) {
        float w = width();
        float h = height();
        if (!texturesLoaded) placeBattlers(a);
        Texture bg = a.graphic("Pictures", "hallfamebg");                          // :56 addBackgroundPlane
        if (bg != null) {
            for (float y = 0; y < h; y += bg.getHeight()) {
                for (float x = 0; x < w; x += bg.getWidth()) {
                    b.draw(bg, x, h - y - bg.getHeight());
                }
            }
        }
        if (barsVisible) {
            Texture bars = a.graphic("Pictures", "hallfamebars");                  // :57-58
            if (bars != null) b.draw(bars, 0f, h - bars.getHeight());
        }
        for (int i = 0; i < hallEntry.size() && i < 6; i++) {
            Texture t = textures[i];
            if (t == null) continue;
            b.setColor(1f, 1f, 1f, opacity[i] / 255f);
            b.draw(t, px[i], h - py[i] - t.getHeight());
            b.setColor(Color.WHITE);
        }
        if (trainerVisible) {
            if (trainerLoadPending || trainerTexture == null) {
                trainerLoadPending = false;
                trainerTexture = a.graphic("Pictures/MPM", trainer.gender == PokemonStats.FEMALE ? "intro_Girl1" : "intro_Boy1");
            }
            if (trainerTexture != null) {                                          // centred on (x, y)
                b.draw(trainerTexture, trainerX - trainerTexture.getWidth() / 2f, h - trainerY - trainerTexture.getHeight() / 2f);
            }
        }
        for (String[] l : overlayLines) {                                          // the overlay bitmap's texts
            float x = Float.parseFloat(l[1]);
            float y = h - Float.parseFloat(l[2]) - (32f - f.lineHeight()) / 2f;
            switch (Integer.parseInt(l[3])) {
                case 1: f.drawRight(b, l[0], x, y, BASECOLOR, SHADOWCOLOR); break;
                case 2: f.drawCentered(b, l[0], x, y, BASECOLOR, SHADOWCOLOR); break;
                default: f.draw(b, l[0], x, y, BASECOLOR, SHADOWCOLOR); break;
            }
        }
        if (messageBoxVisible) drawMessageBox(b, a, f, skin, h);
        if (titleMessage.visible()) titleMessage.draw(b, a, f, skin, w, h);
        if (blackAlpha > 0f) {
            b.setColor(0f, 0f, 0f, Math.min(1f, blackAlpha / 255f));
            b.draw(a.pixel(), 0f, 0f, w, h);
            b.setColor(Color.WHITE);
        }
    }

    /** {@code @sprites["messagebox"]}: the "姓名<r>…<br>" window at the top left, at least 192 wide. */
    private void drawMessageBox(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, float h) {
        float width = 192f;
        for (String[] row : messageBoxRows) {
            width = Math.max(width, f.width(row[0]) + f.width(row[1]) + 32f + 16f);
        }
        float height = 32f + messageBoxRows.size() * 32f;
        MenuPanel.window(b, a, skin, 0f, h - height, width, height);
        Color[] tc = MenuPanel.textColors(skin);
        for (int i = 0; i < messageBoxRows.size(); i++) {
            String[] row = messageBoxRows.get(i);
            float y = h - (16f + i * 32f + (32f - f.lineHeight()) / 2f);
            f.draw(b, row[0], 16f, y, tc[0], tc[1]);
            f.drawRight(b, row[1], width - 16f, y, tc[0], tc[1]);
        }
    }
}
