package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.ui.WindowSkin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 239_PMinigame_Mining: {@code MiningGameScene} inside {@code pbMiningGame}'s {@code pbFadeOutIn}. A 13x10 wall of tiles hides
 * two to four treasures and four to six iron plates; the pick (1 hit) and the hammer (2 hits, wider) wear the tiles down, an
 * iron plate costs a hit without giving way, and the wall collapses after 49 hits. Everything counts in 40 fps ticks
 * ({@code Graphics.update}); the plugin's blocking loops ({@code pbFlashItems}, the collapse) are phases.
 */
public final class MiningView implements MiniGame {
    private static final int BOARDWIDTH = 13;                                   // :135
    private static final int BOARDHEIGHT = 10;                                  // :136
    private static final String DIR = "Pictures/Mining";
    /** :80 ToolPositions: graphic, position. */
    private static final int[][] TOOL_POSITIONS = {{1, 0}, {1, 1}, {1, 1}, {0, 0}, {0, 0},
        {0, 2}, {0, 2}, {0, 0}, {0, 0}, {0, 2}, {0, 2}};
    private static final Color WHITE = new Color(1f, 1f, 1f, 1f);

    private enum Phase { OUTER_OUT, FADE_IN, INTRO, MAIN, FLASH, COLLAPSE, COLLAPSE_MSG, FOUND_WAIT, FOUND_MSG, QUIT_CONFIRM,
        GIVE, FADE_OUT, OUTER_IN, DONE }

    private final RuntimeContext context;
    private final PbsData pbs;
    private final Random random;
    private final MenuClock clock = new MenuClock();
    private final PbMessage pbMessage;
    private final int[] layers = new int[BOARDWIDTH * BOARDHEIGHT];
    /** {@code @items}: index in ITEMS, x, y, revealed (:~205). */
    private final List<int[]> items = new ArrayList<>();
    private final List<String> itemsWon = new ArrayList<>();
    /** {@code @iron}: index in IRON, x, y. */
    private final List<int[]> iron = new ArrayList<>();
    private Phase phase = Phase.OUTER_OUT;
    private int tick;
    private float blackAlpha;
    private boolean finished;
    private boolean pressUp, pressDown, pressLeft, pressRight, pressA, pressC, pressB;
    private boolean pressRepeatUp, pressRepeatDown, pressRepeatLeft, pressRepeatRight;

    // MiningGameCounter / MiningGameCursor / the tool icon
    private int hits;
    private int position = 58;                                                  // :~265 central position, pick
    private int mode;                                                           // 0 = pick, 1 = hammer
    private int hitKind;                                                        // 0 regular, 1 item, 2 iron
    private int counter;
    private boolean cursorVisible = true;
    /** The last {@code update} drew a tool frame (the counter was above 0 before it ran). */
    private boolean animating;
    private boolean collapsed;
    private int flashTick;
    private int[] flashing = new int[0];
    private int collapseTick;
    private int giveIndex;
    private Texture whiteItems;

    public MiningView(RuntimeContext context) {
        this(context, new Random());
    }

    MiningView(RuntimeContext context, Random random) {
        this.context = context;
        this.pbs = context.pbsData();
        this.random = random;
        this.pbMessage = new PbMessage(context);
        for (int i = 0; i < layers.length; i++) {                               // MiningGameTile#initialize (:~46-53)
            int r = random.nextInt(100);
            if (r < 10) layers[i] = 2;                                          // 10%
            else if (r < 25) layers[i] = 3;                                     // 15%
            else if (r < 60) layers[i] = 4;                                     // 35%
            else if (r < 85) layers[i] = 5;                                     // 25%
            else layers[i] = 6;                                                 // 15%
        }
        distributeItems();
        distributeIron();
    }

    private AudioManager audio() {
        return context.audioManager();
    }

    private void se(String name) {
        if (audio() != null) audio().playSe(name, 100, 100);
    }

    // =====================================================================
    // the board (pbDistributeItems / pbDistributeIron / pbCheckOverlaps)
    // =====================================================================

    private static int itemProbability(int i) {
        return (Integer) MiningData.ITEMS[i][1];
    }

    private static int itemWidth(int i) {
        return (Integer) MiningData.ITEMS[i][4];
    }

    private static int itemHeight(int i) {
        return (Integer) MiningData.ITEMS[i][5];
    }

    private static int[] itemPattern(int i) {
        return (int[]) MiningData.ITEMS[i][6];
    }

    private static int ironWidth(int i) {
        return MiningData.IRON[i][2];
    }

    private static int ironHeight(int i) {
        return MiningData.IRON[i][3];
    }

    private static int ironPattern(int i, int j) {
        return MiningData.IRON[i][4 + j];
    }

    /** :~207-247 */
    private void distributeItems() {
        int ptotal = 0;
        for (int i = 0; i < MiningData.ITEMS.length; i++) ptotal += itemProbability(i);
        int numitems = 2 + random.nextInt(3);
        int tries = 0;
        while (numitems > 0) {
            int rnd = random.nextInt(ptotal);
            boolean added = false;
            for (int i = 0; i < MiningData.ITEMS.length; i++) {
                rnd -= itemProbability(i);
                if (rnd < 0) {
                    if (noDuplicateItems((String) MiningData.ITEMS[i][0])) {
                        while (!added) {
                            int provx = random.nextInt(BOARDWIDTH - itemWidth(i) + 1);
                            int provy = random.nextInt(BOARDHEIGHT - itemHeight(i) + 1);
                            if (checkOverlaps(false, provx, provy, itemWidth(i), itemHeight(i), itemPattern(i))) {
                                items.add(new int[] {i, provx, provy, 0});
                                numitems -= 1;
                                added = true;
                            }
                        }
                    } else {
                        break;
                    }
                }
                if (added) break;
            }
            tries += 1;
            if (tries >= 500) break;
        }
    }

    /** :~249-269 */
    private void distributeIron() {
        int numitems = 4 + random.nextInt(3);
        int tries = 0;
        while (numitems > 0) {
            int rnd = random.nextInt(MiningData.IRON.length);
            int provx = random.nextInt(BOARDWIDTH - ironWidth(rnd) + 1);
            int provy = random.nextInt(BOARDHEIGHT - ironHeight(rnd) + 1);
            int[] pattern = Arrays.copyOfRange(MiningData.IRON[rnd], 4, MiningData.IRON[rnd].length);
            if (checkOverlaps(true, provx, provy, ironWidth(rnd), ironHeight(rnd), pattern)) {
                iron.add(new int[] {rnd, provx, provy});
                numitems -= 1;
            }
            tries += 1;
            if (tries >= 500) break;
        }
    }

    private static final List<String> FOSSILS = Arrays.asList("DOMEFOSSIL", "HELIXFOSSIL", "OLDAMBER", "ROOTFOSSIL",
            "SKULLFOSSIL", "ARMORFOSSIL", "CLAWFOSSIL");
    private static final List<String> PLATES = Arrays.asList("INSECTPLATE", "DREADPLATE", "DRACOPLATE", "ZAPPLATE", "FISTPLATE",
            "FLAMEPLATE", "MEADOWPLATE", "EARTHPLATE", "ICICLEPLATE", "TOXICPLATE", "MINDPLATE", "STONEPLATE", "SKYPLATE",
            "SPOOKYPLATE", "IRONPLATE", "SPLASHPLATE");

    /** :~271-285 */
    private boolean noDuplicateItems(String newitem) {
        if ("HEARTSCALE".equals(newitem)) return true;                          // Allow multiple Heart Scales
        for (int[] i : items) {
            String preitem = (String) MiningData.ITEMS[i[0]][0];
            if (preitem.equals(newitem)) return false;                          // No duplicate items
            if (FOSSILS.contains(preitem) && FOSSILS.contains(newitem)) return false;
            if (PLATES.contains(preitem) && PLATES.contains(newitem)) return false;
        }
        return true;
    }

    /** :~287-325 */
    private boolean checkOverlaps(boolean checkiron, int provx, int provy, int provwidth, int provheight, int[] provpattern) {
        for (int[] i : items) {
            if (overlaps(i[1], i[2], itemWidth(i[0]), itemHeight(i[0]), itemPattern(i[0]),
                    provx, provy, provwidth, provheight, provpattern)) return false;
        }
        if (checkiron) {                                                        // Check other irons as well
            for (int[] i : iron) {
                int[] pattern = Arrays.copyOfRange(MiningData.IRON[i[0]], 4, MiningData.IRON[i[0]].length);
                if (overlaps(i[1], i[2], ironWidth(i[0]), ironHeight(i[0]), pattern,
                        provx, provy, provwidth, provheight, provpattern)) return false;
            }
        }
        return true;
    }

    private static boolean overlaps(int prex, int prey, int prewidth, int preheight, int[] prepattern,
            int provx, int provy, int provwidth, int provheight, int[] provpattern) {
        if (provx + provwidth <= prex || provx >= prex + prewidth
                || provy + provheight <= prey || provy >= prey + preheight) return false;
        for (int j = 0; j < prepattern.length; j++) {
            if (prepattern[j] == 0) continue;
            int xco = prex + (j % prewidth);
            int yco = prey + (j / prewidth);
            if (provx + provwidth <= xco || provx > xco || provy + provheight <= yco || provy > yco) continue;
            if (provpattern[xco - provx + (yco - provy) * provwidth] == 1) return true;
        }
        return false;
    }

    private boolean isItemThere(int pos) {
        int posx = pos % BOARDWIDTH;
        int posy = pos / BOARDWIDTH;
        for (int[] i : items) {
            int width = itemWidth(i[0]);
            int height = itemHeight(i[0]);
            if (posx < i[1] || posx >= i[1] + width) continue;
            if (posy < i[2] || posy >= i[2] + height) continue;
            if (itemPattern(i[0])[(posx - i[1]) + (posy - i[2]) * width] > 0) return true;
        }
        return false;
    }

    private boolean isIronThere(int pos) {
        int posx = pos % BOARDWIDTH;
        int posy = pos / BOARDWIDTH;
        for (int[] i : iron) {
            int width = ironWidth(i[0]);
            int height = ironHeight(i[0]);
            if (posx < i[1] || posx >= i[1] + width) continue;
            if (posy < i[2] || posy >= i[2] + height) continue;
            if (ironPattern(i[0], (posx - i[1]) + (posy - i[2]) * width) > 0) return true;
        }
        return false;
    }

    private int[] checkRevealed() {
        List<Integer> ret = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            int[] item = items.get(i);
            if (item[3] != 0) continue;
            boolean revealed = true;
            int width = itemWidth(item[0]);
            int height = itemHeight(item[0]);
            int[] pattern = itemPattern(item[0]);
            for (int j = 0; j < height && revealed; j++) {
                for (int k = 0; k < width && revealed; k++) {
                    int layer = layers[item[1] + k + (item[2] + j) * BOARDWIDTH];
                    if (layer > 0 && pattern[k + j * width] > 0) revealed = false;
                }
            }
            if (revealed) ret.add(i);
        }
        int[] out = new int[ret.size()];
        for (int i = 0; i < out.length; i++) out[i] = ret.get(i);
        return out;
    }

    /** :~327-373 pbHit */
    private void hit() {
        int hittype = 0;
        int[] pattern;
        if (mode == 1) {                                                        // Hammer
            pattern = new int[] {1, 2, 1, 2, 2, 2, 1, 2, 1};
            hits += 2;
        } else {                                                                // Pick
            pattern = new int[] {0, 1, 0, 1, 2, 1, 0, 1, 0};
            hits += 1;
        }
        if (layers[position] <= pattern[4] && isIronThere(position)) {
            layers[position] = Math.max(0, layers[position] - pattern[4]);
            se("Mining iron");
            hittype = 2;
        } else {
            for (int i = 0; i <= 2; i++) {
                int ytile = i - 1 + position / BOARDWIDTH;
                if (ytile < 0 || ytile >= BOARDHEIGHT) continue;
                for (int j = 0; j <= 2; j++) {
                    int xtile = j - 1 + position % BOARDWIDTH;
                    if (xtile < 0 || xtile >= BOARDWIDTH) continue;
                    int at = xtile + ytile * BOARDWIDTH;
                    layers[at] = Math.max(0, layers[at] - pattern[j + i * 3]);   // MiningGameTile#layer=
                }
            }
            se(mode == 1 ? "Mining hammer" : "Mining pick");
        }
        boolean hititem = layers[position] == 0 && isItemThere(position);
        if (hititem) hittype = 1;
        counter = 22;                                                           // MiningGameCursor#animate
        hitKind = hittype;
        int[] revealed = checkRevealed();
        if (revealed.length > 0) {
            se("Mining reveal full");
            flashing = revealed;
            flashTick = 0;
            phase = Phase.FLASH;
        } else if (hititem) {
            se("Mining reveal");
        }
    }

    // =====================================================================
    // update
    // =====================================================================

    @Override
    public boolean update(InputManager input) {
        int ticks = clock.advance();
        if (pbMessage.active()) {
            pbMessage.update(input, ticks);
            return finished;
        }
        pressUp |= input.wasPressed(GameAction.UP) || input.wasRepeated(GameAction.UP);
        pressDown |= input.wasPressed(GameAction.DOWN) || input.wasRepeated(GameAction.DOWN);
        pressLeft |= input.wasPressed(GameAction.LEFT) || input.wasRepeated(GameAction.LEFT);
        pressRight |= input.wasPressed(GameAction.RIGHT) || input.wasRepeated(GameAction.RIGHT);
        pressA |= input.wasPressed(GameAction.SPECIAL);
        pressC |= input.wasPressed(GameAction.CONFIRM);
        pressB |= input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU);
        for (int i = 0; i < ticks && !finished && !pbMessage.active(); i++) {
            step();
            pressUp = pressDown = pressLeft = pressRight = pressA = pressC = pressB = false;
        }
        return finished;
    }

    /** {@code MiningGameCursor#update}'s counter (:~105-111). */
    private void updateCursor() {
        animating = counter > 0;
        if (counter > 0) counter -= 1;
    }

    private void step() {
        switch (phase) {
            case OUTER_OUT:                                                     // pbFadeOutIn
                blackAlpha = Math.min(255f, tick * 16f);
                if (++tick > 16) {
                    phase = Phase.FADE_IN;                                      // pbFadeInAndShow
                    tick = 0;
                }
                break;
            case FADE_IN:
                blackAlpha = Math.max(0f, Math.min(255f, (16 - tick) * 16f));
                if (++tick > 16) {
                    blackAlpha = 0f;
                    phase = Phase.INTRO;
                    se("Mining ping");                                          // pbMain:~386
                    pbMessage.start("墙壁中发出了叮当声！\n已确认" + items.size() + "件物品！", null, 0, 0,
                            ignored -> phase = Phase.MAIN);
                }
                break;
            case MAIN:
                updateCursor();
                if (counter > 0) break;                                         // next if @sprites["cursor"].isAnimating?
                main();
                break;
            case FLASH:
                updateCursor();
                if (++flashTick >= 40 / 8 * 2) {                                // halfFlashTime*2 frames
                    for (int index : flashing) {                                // :~418-423
                        items.get(index)[3] = 1;
                        itemsWon.add((String) MiningData.ITEMS[items.get(index)[0]][0]);
                    }
                    flashing = new int[0];
                    phase = Phase.MAIN;
                }
                break;
            case COLLAPSE:
                collapseTick++;
                if (collapseTick >= 40 * 8 / 10) {                              // collapseTime
                    collapsed = true;                                           // the black bitmap stays until the scene ends
                    phase = Phase.COLLAPSE_MSG;
                    pbMessage.start("轰隆！岩壁塌方了！", null, 0, 0, ignored -> startGive());
                }
                break;
            case FOUND_WAIT:
                if (++tick >= 40 * 3 / 4) {                                     // pbWait(frame_rate*3/4)
                    se("Mining found all");
                    phase = Phase.FOUND_MSG;
                    pbMessage.start("所有宝物都被挖出来啦！", null, 0, 0, ignored -> startGive());
                }
                break;
            case GIVE:
                giveStep();
                break;
            case FADE_OUT:                                                      // pbEndScene: pbFadeOutAndHide
                blackAlpha = Math.min(255f, tick * 16f);
                if (++tick > 16) {
                    phase = Phase.OUTER_IN;
                    tick = 0;
                }
                break;
            case OUTER_IN:
                blackAlpha = Math.max(0f, Math.min(255f, (16 - tick) * 16f));
                if (++tick > 16) {
                    phase = Phase.DONE;
                    finished = true;
                }
                break;
            default:
                break;
        }
    }

    /** One pass of {@code pbMain}'s loop body after the animation check (:~388-466). */
    private void main() {
        if (hits >= 49) {                                                       // Check end conditions
            cursorVisible = false;
            se("Mining collapse");
            collapseTick = 0;
            phase = Phase.COLLAPSE;
            return;
        }
        boolean foundall = true;
        for (int[] i : items) {
            if (i[3] == 0) {
                foundall = false;
                break;
            }
        }
        if (foundall) {
            cursorVisible = false;
            tick = 0;
            phase = Phase.FOUND_WAIT;
            return;
        }
        if (pressUp) {                                                          // Input
            if (position >= BOARDWIDTH) {
                se("Mining cursor");
                position -= BOARDWIDTH;
            }
        } else if (pressDown) {
            if (position < BOARDWIDTH * (BOARDHEIGHT - 1)) {
                se("Mining cursor");
                position += BOARDWIDTH;
            }
        } else if (pressLeft) {
            if (position % BOARDWIDTH > 0) {
                se("Mining cursor");
                position -= 1;
            }
        } else if (pressRight) {
            if (position % BOARDWIDTH < BOARDWIDTH - 1) {
                se("Mining cursor");
                position += 1;
            }
        } else if (pressA) {                                                    // Change tool mode
            se("Mining tool change");
            mode = (mode + 1) % 2;
        } else if (pressC) {                                                    // Hit
            hit();
        } else if (pressB) {                                                    // Quit
            phase = Phase.QUIT_CONFIRM;
            pbMessage.start("确定要放弃吗？", Arrays.asList("是", "否"), 2, 0, index -> {   // pbConfirmMessage
                if (index == 0) startGive();
                else phase = Phase.MAIN;
            });
        }
    }

    private void startGive() {
        giveIndex = 0;
        phase = Phase.GIVE;
    }

    /** :~468-480 pbGiveItems, then pbEndScene. */
    private void giveStep() {
        if (giveIndex < itemsWon.size()) {
            String id = itemsWon.get(giveIndex++);
            PbsData.Item data = pbs == null ? null : pbs.item(id);
            String name = data == null ? id : data.name;
            if (context.gameState().inventory() != null && data != null) {      // $PokemonBag.pbStoreItem: the pockets are unlimited here
                context.gameState().inventory().add(id, 1);
                pbMessage.start("获得了" + name + "。\\se[Mining item get]\\wtnp[30]", null, 0, 0, ignored -> { });
            } else {
                pbMessage.start("发现了" + name + "，但背包已满。", null, 0, 0, ignored -> { });
            }
            return;
        }
        phase = Phase.FADE_OUT;
        tick = 0;
    }

    // =====================================================================
    // render
    // =====================================================================

    private void draw(SpriteBatch b, Texture t, float h, float x, float y, int sx, int sy, int sw, int sh) {
        b.draw(t, x, h - y - sh, sw, sh, sx, sy, sw, sh, false, false);
    }

    /** Draws a region clipped to a viewport's horizontal extent (a {@code Viewport.new(x,y,w,h)} crops its sprites). */
    private void drawClipped(SpriteBatch b, Texture t, float h, float x, float y, int sx, int sy, int sw, int sh,
            float clipX0, float clipX1) {
        float x0 = Math.max(x, clipX0);
        float x1 = Math.min(x + sw, clipX1);
        if (x1 <= x0) return;
        draw(b, t, h, x0, y, sx + (int) (x0 - x), sy, (int) (x1 - x0), sh);
    }

    @Override
    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin) {
        float w = ScreenMetrics.logicalWidth();
        float h = ScreenMetrics.logicalHeight();
        Texture bg = a.graphic(DIR, "miningbg");                                // addBackgroundPlane "Mining/miningbg"
        if (bg != null) {
            for (float y = 0; y < h; y += bg.getHeight()) {
                for (float x = 0; x < w; x += bg.getWidth()) b.draw(bg, x, h - y - bg.getHeight());
            }
        }
        Texture itemsTex = a.graphic(DIR, "items");
        Texture ironsTex = a.graphic(DIR, "irons");
        for (int[] i : items) {                                                 // the item layer (:~239-244)
            int[] row = {(Integer) MiningData.ITEMS[i[0]][2], (Integer) MiningData.ITEMS[i[0]][3]};
            if (itemsTex != null) draw(b, itemsTex, h, 32 * i[1] + 64, 64 + 32 * i[2], 32 * row[0], 32 * row[1],
                    32 * itemWidth(i[0]), 32 * itemHeight(i[0]));
        }
        for (int[] i : iron) {                                                  // (:~261-267)
            int[] row = MiningData.IRON[i[0]];
            if (ironsTex != null) draw(b, ironsTex, h, 32 * i[1] + 64, 64 + 32 * i[2], 32 * row[0], 32 * row[1],
                    32 * ironWidth(i[0]), 32 * ironHeight(i[0]));
        }
        Texture tiles = a.graphic(DIR, "tiles");
        if (tiles != null) {
            for (int i = 0; i < BOARDHEIGHT; i++) {
                for (int j = 0; j < BOARDWIDTH; j++) {
                    int layer = layers[j + i * BOARDWIDTH];
                    if (layer > 0) draw(b, tiles, h, 32 * j + 64, 64 + 32 * i, 0, 32 * (layer - 1), 32, 32);
                }
            }
        }
        if (phase == Phase.FLASH && itemsTex != null) drawFlash(b, itemsTex, h);
        drawCounter(b, a, h);
        drawCursor(b, a, h);
        Texture tool = a.graphic(DIR, "toolicons");                             // :~270-272
        if (tool != null) draw(b, tool, h, 434 + 64, 254 - 144 * mode, 68 * mode, 0, 68, 100);
        if (phase == Phase.COLLAPSE) {                                          // the collapse viewport
            int fraction = (int) Math.ceil(h / (40 * 8 / 10f));
            float bottom = Math.min(h, (2 * collapseTick - 1) * fraction);
            if (bottom > 0) {
                b.setColor(0f, 0f, 0f, 1f);
                b.draw(a.pixel(), 0f, h - bottom, w, bottom);
                b.setColor(WHITE);
            }
        } else if (collapsed) {
            b.setColor(0f, 0f, 0f, 1f);
            b.draw(a.pixel(), 0f, 0f, w, h);
            b.setColor(WHITE);
        }
        if (pbMessage.active()) pbMessage.render(b, a, f, skin, skin, w, h);
        if (blackAlpha > 0f) {
            b.setColor(0f, 0f, 0f, Math.min(1f, blackAlpha / 255f));
            b.draw(a.pixel(), 0f, 0f, w, h);
            b.setColor(WHITE);
        }
    }

    /** {@code MiningGameCounter#update} (:~11-30): the cracks, in a viewport 416 wide at (64, 4). */
    private void drawCounter(SpriteBatch b, MenuAssets a, float h) {
        Texture cracks = a.graphic(DIR, "cracks");
        if (cracks == null) return;
        int value = hits;
        int startx = 416 + 64 - 48;
        while (value > 6) {
            drawClipped(b, cracks, h, 64 + startx, 4, 0, 0, 48, 52, 64, 64 + 416);
            startx -= 48;
            value -= 6;
        }
        startx -= 48;
        if (value > 0) drawClipped(b, cracks, h, 64 + startx, 4, 0, value * 52, 96, 52, 64, 64 + 416);
    }

    /** {@code MiningGameCursor#update} (:~96-135). */
    private void drawCursor(SpriteBatch b, MenuAssets a, float h) {
        if (!cursorVisible) return;
        int x = 32 * (position % BOARDWIDTH) + 64;
        int y = 32 * (position / BOARDWIDTH);
        if (animating) {
            Texture tools = a.graphic(DIR, "tools");
            Texture hitsTex = a.graphic(DIR, "hits");
            int i = 10 - counter / 2;                                           // the counter was decremented first
            int toolx = x;
            int tooly = y;
            if (TOOL_POSITIONS[i][1] == 1) {
                toolx -= 8;
                tooly += 8;
            } else if (TOOL_POSITIONS[i][1] == 2) {
                toolx += 6;
            }
            if (tools != null) draw(b, tools, h, toolx, tooly, 96 * TOOL_POSITIONS[i][0], 96 * mode, 96, 96);
            if (hitsTex != null) {
                if (i < 5 && i % 2 == 0) {
                    draw(b, hitsTex, h, x - 64, y, 160 * (hitKind == 2 ? 2 : mode), 0, 160, 160);
                }
                if (hitKind == 1 && i < 3) draw(b, hitsTex, h, x - 64, y, 160 * i, 160, 160, 160);
            }
        } else {
            Texture cursor = a.graphic(DIR, "cursor");
            if (cursor != null) draw(b, cursor, h, x, y + 64, 32 * mode, 0, 32, 32);
        }
    }

    /** {@code pbFlashItems} (:~395-420): the revealed items flash white and back. */
    private void drawFlash(SpriteBatch b, Texture itemsTex, float h) {
        int halfFlashTime = 40 / 8;
        int alphaDiff = (int) Math.ceil(255.0 / halfFlashTime);
        int i = flashTick + 1;
        int alpha = i > halfFlashTime ? (halfFlashTime * 2 - i) * alphaDiff : i * alphaDiff;
        if (alpha <= 0) return;
        if (whiteItems == null) whiteItems = silhouette(itemsTex);
        b.setColor(1f, 1f, 1f, Math.min(1f, alpha / 255f));
        for (int index : flashing) {
            int[] item = items.get(index);
            int gx = (Integer) MiningData.ITEMS[item[0]][2];
            int gy = (Integer) MiningData.ITEMS[item[0]][3];
            draw(b, whiteItems, h, 32 * item[1] + 64, 64 + 32 * item[2], 32 * gx, 32 * gy,
                    32 * itemWidth(item[0]), 32 * itemHeight(item[0]));
        }
        b.setColor(WHITE);
    }

    /** A copy of the texture with every visible pixel white: drawn over the original at alpha n it is {@code color=(255,255,255,n)}. */
    private static Texture silhouette(Texture source) {
        if (!source.getTextureData().isPrepared()) source.getTextureData().prepare();
        Pixmap src = source.getTextureData().consumePixmap();
        Pixmap out = new Pixmap(src.getWidth(), src.getHeight(), Pixmap.Format.RGBA8888);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int alpha = src.getPixel(x, y) & 0xFF;
                out.drawPixel(x, y, 0xFFFFFF00 | alpha);
            }
        }
        Texture texture = new Texture(new PixmapTextureData(out, null, false, true));
        if (source.getTextureData().disposePixmap()) src.dispose();
        return texture;
    }

    @Override
    public void dispose() {
        if (whiteItems != null) whiteItems.dispose();
        whiteItems = null;
    }
}
