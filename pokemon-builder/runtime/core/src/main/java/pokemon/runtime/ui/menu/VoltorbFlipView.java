package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.ui.WindowSkin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * 237_PMinigame_VoltorbFlip: {@code VoltorbFlip} / {@code VoltorbFlipScreen} ({@code pbVoltorbFlip}). A 5x5 board of cards
 * (1, 2, 3 or a Voltorb) with the row / column totals and Voltorb counts on its edges; flipping every 2 and 3 card wins the
 * round's coins, a Voltorb loses them. Everything counts in 40 fps ticks ({@code Graphics.update}). The plugin's blocking calls
 * ({@code pbWait}, {@code pbMessage}, the wait for a key) are steps of a queue that the idle loop ({@code getInput}) waits behind.
 * The plugin's own English lines are kept as they are.
 */
public final class VoltorbFlipView implements MiniGame {
    private static final String DIR = "Pictures/Voltorb Flip";
    private static final int MAX_COINS = 99_999;                                 // Settings:97
    private static final int[][] LEVEL_RANGES = {{20, 50}, {50, 100}, {100, 200}, {200, 350},
        {350, 600}, {600, 1000}, {1000, 2000}, {2000, 3500}};                    // :16-17
    private static final Color TEXT_BASE = new Color(60 / 255f, 60 / 255f, 60 / 255f, 1f);
    private static final Color TEXT_SHADOW = new Color(150 / 255f, 190 / 255f, 170 / 255f, 1f);
    private static final Color WHITE = new Color(1f, 1f, 1f, 1f);

    /** One thing the scene does, in order. */
    private interface Step {
        /** @return true when the step blocks (a wait, a message, a key) */
        boolean run();
    }

    private final RuntimeContext context;
    private final Random random;
    private final MenuClock clock = new MenuClock();
    private final PbMessage pbMessage;
    private final Deque<Step> queue = new ArrayDeque<>();
    private int waitTicks;
    private boolean waitingKey;
    private boolean messageOpen;
    private boolean finished;
    private boolean quit;

    private boolean pressUp, pressDown, pressLeft, pressRight, pressC, pressB, pressMode;

    // game state
    private int level;
    private boolean firstRound;
    private int points;
    private final int[] index = new int[2];
    /** {@code @squares}: x, y, value, flipped (:41-43). */
    private int[][] squares = new int[25][];
    private final List<int[]> marks = new ArrayList<>();                          // x, y
    private int cursorSrcX;                                                      // @cursor[0][3]: 0 normal, 64 mark mode
    private boolean memoVisible;
    private float cursorX;
    private float cursorY;
    private boolean cursorDrawn;

    // sprite hash
    private final List<int[]> iconLayer = new ArrayList<>();                      // x, y, src x
    private final int[][] layers = new int[6][25];                                // src x per square, -1 none
    private final boolean[] layerVisible = new boolean[6];
    private final boolean[] layerAbove = new boolean[6];                          // z 99997 instead of 99996
    private int[] animation;                                                     // image (0 tiles, 1 explosion, 2 flip), x, y, src x, w, h
    private final List<int[]> numbers = new ArrayList<>();                        // x, y, src x
    private int[] totalDigits = new int[5];
    private int[] pointDigits = new int[5];
    private int curtainOpacity;                                                  // @sprites["curtain"].opacity
    private float curtainL = -90f;
    private float curtainR;
    private float curtainLY;
    private boolean curtainsVisible = true;
    private float blackAlpha;
    private String levelText = "";

    private MenuFont font26;
    private MenuFont font28;

    public VoltorbFlipView(RuntimeContext context) {
        this(context, new Random());
    }

    VoltorbFlipView(RuntimeContext context, Random random) {
        this.context = context;
        this.random = random;
        this.pbMessage = new PbMessage(context);
        level = 1;                                                               // :11
        firstRound = true;                                                       // :18
        newGame();                                                               // :19 pbNewGame
    }

    private AudioManager audio() {
        return context.audioManager();
    }

    private void se(String name) {
        if (audio() != null) audio().playSe(name, 100, 100);
    }

    private int coins() {
        return context.gameState().fieldGlobals().coins;
    }

    private void setCoins(int value) {
        context.gameState().fieldGlobals().coins = value;
    }

    // =====================================================================
    // steps
    // =====================================================================

    private void add(Step... steps) {
        queue.addAll(Arrays.asList(steps));
    }

    private Step wait(int ticks) {
        return () -> {
            waitTicks = ticks;
            return true;
        };
    }

    private Step run(Runnable r) {
        return () -> {
            r.run();
            return false;
        };
    }

    private Step message(String text) {
        return () -> {
            messageOpen = true;
            pbMessage.start(text, null, 0, 0, ignored -> messageOpen = false);
            return true;
        };
    }

    private Step confirm(String text, Consumer<Boolean> result) {
        return () -> {
            messageOpen = true;
            pbMessage.start(text, Arrays.asList("是", "否"), 2, 0, index -> {      // pbConfirmMessage
                messageOpen = false;
                result.accept(index == 0);
            });
            return true;
        };
    }

    private Step waitKey() {
        return () -> {
            waitingKey = true;
            return true;
        };
    }

    /** Puts steps in front of what is queued: a flow that was decided while the queue was running. */
    private void front(List<Step> steps) {
        for (int i = steps.size() - 1; i >= 0; i--) queue.addFirst(steps.get(i));
    }

    // =====================================================================
    // pbNewGame (:21-124)
    // =====================================================================

    private void newGame() {
        Arrays.fill(layerVisible, false);
        for (int[] row : layers) Arrays.fill(row, -1);
        iconLayer.clear();
        marks.clear();
        numbers.clear();
        animation = null;
        points = 0;
        index[0] = 0;
        index[1] = 0;
        cursorX = 0;
        cursorY = 0;
        cursorDrawn = false;
        memoVisible = false;
        cursorSrcX = 0;
        int[] squareValues = new int[25];
        int total = 1;
        int voltorbs = 0;
        for (int i = 0; i < 25; i++) {
            squareValues[i] = 1;                                                 // :35
            if (voltorbs < 5 + level) {                                          // :38
                squareValues[i] = 0;
                voltorbs++;
            } else if (total < LEVEL_RANGES[level - 1][1]) {                     // :42
                squareValues[i] = random.nextInt(2) + 2;
                total *= squareValues[i];
            }
            if (total > LEVEL_RANGES[level - 1][1]) {                            // :46
                total /= squareValues[i];
                squareValues[i] = 1;
            }
        }
        for (int i = 0; i < 25; i++) {                                           // :53 randomise the values a little
            int temp = squareValues[i];
            if (squareValues[i] > 1) {
                if (random.nextInt(10) > 8) {
                    total /= squareValues[i];
                    squareValues[i] -= 1;
                    total *= squareValues[i];
                }
            }
            if (total < LEVEL_RANGES[level - 1][0]) {
                if (squareValues[i] > 0) {
                    total /= squareValues[i];
                    squareValues[i] = temp;
                    total *= squareValues[i];
                }
            }
        }
        List<Integer> pool = new ArrayList<>();
        for (int v : squareValues) pool.add(v);
        int x = 0;
        for (int i = 0; i < 25; i++) {                                           // :73 populate @squares
            if (i % 5 == 0) x = i;
            int r = random.nextInt(pool.size());
            squares[i] = new int[] {Math.abs(i - x) * 64 + 128, (i / 5) * 64, pool.remove(r), 0};
        }
        // pbCreateSprites (:126-): the display-all frames
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 25; j++) {
                int p = i == 2 ? squares[j][2] : 0;
                layers[i][j] = 320 + i * 64 + p * 64;
            }
        }
        for (int i = 0; i < 25; i++) layers[5][i] = squares[i][2] * 64;
        Arrays.fill(layerAbove, false);
        for (int i = 0; i < 5; i++) {                                            // :88 numbers are all zeroes for now
            updateRowNumbers(0, 0, i);
            updateColumnNumbers(0, 0, i);
        }
        levelText = "等级" + level;                                               // :96-98
        updateCoins();                                                           // :101
        curtainOpacity = 0;
        curtainsVisible = true;
        curtainL = -90f;
        curtainR = 0f;
        curtainLY = 0f;
        // draw curtain effect
        List<Step> flow = new ArrayList<>();
        if (firstRound) {                                                        // :103
            int angleDiff = 10 * 20 / 40;
            flow.add(() -> {
                curtainL -= angleDiff;
                curtainR += angleDiff;
                return curtainL > -180f ? repeatNextTick() : false;
            });
        }
        flow.add(run(() -> {
            curtainsVisible = false;                                             // :114-116
            curtainOpacity = 100;
        }));
        if (coins() >= MAX_COINS) {                                              // :117
            flow.add(message("已经获得了" + String.format("%,d", MAX_COINS) + "枚代币。\n不能再获得代币了。"));
            flow.add(run(() -> {
                setCoins(MAX_COINS);                                             // as a precaution
                quit = true;
            }));
        } else {
            flow.add(run(() -> {
                curtainOpacity = 0;                                              // :126
                numbers.clear();                                                 // :128
                for (int i = 0; i < 25; i++) {                                   // :132 the numbers of each row
                    if (i % 5 == 0) {
                        int num = 0;
                        int volt = 0;
                        for (int k = i; k < i + 5; k++) {
                            num += squares[k][2];
                            if (squares[k][2] == 0) volt++;
                        }
                        updateRowNumbers(num, volt, i / 5);
                    }
                }
                for (int i = 0; i < 5; i++) {                                    // :152 and of each column
                    int num = 0;
                    int volt = 0;
                    for (int j = 0; j < 5; j++) {
                        num += squares[i + j * 5][2];
                        if (squares[i + j * 5][2] == 0) volt++;
                    }
                    updateColumnNumbers(num, volt, i);
                }
            }));
        }
        front(flow);                                                             // pbNewGame finishes before the caller goes on
    }

    /** A step that runs again next tick (a {@code loop do ... Graphics.update} body). */
    private boolean repeatNextTick() {
        return true;
    }

    private static int[] digits(int value, int count) {
        String text = String.valueOf(value);
        StringBuilder padded = new StringBuilder();
        for (int i = text.length(); i < count; i++) padded.append('0');
        padded.append(text);
        int[] out = new int[count];
        for (int i = 0; i < count; i++) out[i] = padded.charAt(i) - '0';           // numText.split(//)[0...count]
        return out;
    }

    /** pbUpdateRowNumbers (:292-312). */
    private void updateRowNumbers(int num, int voltorbs, int i) {
        int[] d = digits(num, 2);
        for (int j = 0; j < 2; j++) numbers.add(new int[] {472 + j * 16, i * 64 + 8, d[j] * 16, 16});
        numbers.add(new int[] {488, i * 64 + 34, voltorbs * 16, 16});
    }

    /** pbUpdateColumnNumbers (:314-333). */
    private void updateColumnNumbers(int num, int voltorbs, int i) {
        int[] d = digits(num, 2);
        for (int j = 0; j < 2; j++) numbers.add(new int[] {i * 64 + 152 + j * 16, 328, d[j] * 16, 16});
        numbers.add(new int[] {i * 64 + 168, 354, voltorbs * 16, 16});
    }

    /** pbUpdateCoins (:345-353). */
    private void updateCoins() {
        totalDigits = digits(coins(), 5);
        pointDigits = digits(points, 5);
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
        pressUp |= input.wasPressed(GameAction.UP);
        pressDown |= input.wasPressed(GameAction.DOWN);
        pressLeft |= input.wasPressed(GameAction.LEFT);
        pressRight |= input.wasPressed(GameAction.RIGHT);
        pressC |= input.wasPressed(GameAction.CONFIRM);
        pressB |= input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU);
        pressMode |= input.wasPressed(GameAction.TOGGLE_FOLLOWER);               // Input::CTRL
        for (int i = 0; i < ticks && !finished && !pbMessage.active(); i++) {
            step();
            pressUp = pressDown = pressLeft = pressRight = pressC = pressB = pressMode = false;
        }
        return finished;
    }

    private void step() {
        if (messageOpen) return;
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        if (waitingKey) {
            if (pressC || pressB) waitingKey = false;
            return;
        }
        if (!queue.isEmpty()) {
            Step next = queue.pollFirst();
            boolean blocked = next.run();
            // a step that blocks without a wait, a message or a key is a loop body: it runs again next tick
            if (blocked && waitTicks == 0 && !messageOpen && !waitingKey) queue.addFirst(next);
            return;
        }
        if (quit) {
            endScene();
            return;
        }
        getInput();
    }

    // =====================================================================
    // getInput (:176-444)
    // =====================================================================

    private void cursorSe() {
        MenuSe.cursor(audio());
    }

    private void getInput() {
        if (pressUp) {
            cursorSe();
            if (index[1] > 0) { index[1]--; cursorY -= 64; } else { index[1] = 4; cursorY = 256; }
        } else if (pressDown) {
            cursorSe();
            if (index[1] < 4) { index[1]++; cursorY += 64; } else { index[1] = 0; cursorY = 0; }
        } else if (pressLeft) {
            cursorSe();
            if (index[0] > 0) { index[0]--; cursorX -= 64; } else { index[0] = 4; cursorX = 256; }
        } else if (pressRight) {
            cursorSe();
            if (index[0] < 4) { index[0]++; cursorX += 64; } else { index[0] = 0; cursorX = 0; }
        } else if (pressC) {
            if (cursorSrcX == 64) {                                              // in mark mode
                mark();
            } else {
                flip();
            }
        } else if (pressMode) {                                                  // Input::CTRL
            MenuSe.decision(audio());
            cursorDrawn = false;                                                 // @sprites["cursor"].bitmap.clear
            if (cursorSrcX == 0) {
                cursorSrcX = 64;                                                 // mark mode
                memoVisible = true;
            } else {
                cursorSrcX = 0;
                memoVisible = false;
            }
        } else if (pressB) {
            leave();
        }
        if (queue.isEmpty()) cursorDrawn = true;                                 // :443 pbDrawImagePositions(cursor)
    }

    /** :183-203 the mark mode's C. */
    private void mark() {
        int x = index[0] * 64 + 128;
        int y = index[1] * 64;
        for (int[] square : squares) {
            if (x == square[0] && y == square[1] && square[3] == 0) se("Voltorb Flip mark");   // :186-188
        }
        for (int i = 0; i < marks.size() + 1; i++) {                             // :190
            if (i == marks.size()) {
                marks.add(new int[] {x, y});
                break;
            } else if (marks.get(i)[0] == x && marks.get(i)[1] == y) {
                marks.remove(i);
                break;
            }
        }
        add(wait(40 / 20));                                                      // :200 pbWait(Graphics.frame_rate/20)
    }

    /** :204-358 the normal mode's C: flip the card. */
    private void flip() {
        int x = index[0] * 64 + 128;
        int y = index[1] * 64;
        for (int i = 0; i < squares.length; i++) {
            int[] square = squares[i];
            if (x == square[0] && y == square[1] && square[3] == 0) {
                List<Step> flow = new ArrayList<>();
                animateTile(flow, x, y, square[2]);                              // :209
                final int slot = i;
                flow.add(run(() -> squares[slot][3] = 1));                       // :210
                if (square[2] == 0) {                                            // a Voltorb
                    voltorbFlow(flow, x, y);
                } else {
                    for (int j = 0; j < 4; j++) {                                // :334-340 the tile animation
                        final int frame = j;
                        flow.add(run(() -> animation = new int[] {2, x - 14, y - 16, frame * 92, 92, 96}));
                        flow.add(wait(2));
                        flow.add(run(() -> animation = null));
                    }
                    flow.add(run(() -> {
                        int value = squares[slot][2];
                        if (points == 0) {                                       // :342
                            points += value;
                            se("Voltorb Flip point");
                        } else if (value > 1) {
                            points *= value;
                            se("Voltorb Flip point");
                        }
                    }));
                }
                flow.add(run(this::afterFlip));                                  // :360-
                front(flow);
                return;
            }
        }
        afterFlipNow();
    }

    /** pbAnimateTile (:355-367). */
    private void animateTile(List<Step> flow, int x, int y, int tile) {
        for (int i = 0; i < 3; i++) {
            final int frame = i;
            flow.add(run(() -> iconLayer.add(new int[] {x, y, 320 + frame * 64 + (frame == 2 ? tile : 0) * 64})));
            flow.add(wait(2));
        }
        flow.add(run(() -> {
            iconLayer.add(new int[] {x, y, tile * 64});
            se("Voltorb Flip tile");
        }));
    }

    /** :213-333 */
    private void voltorbFlow(List<Step> flow, int x, int y) {
        flow.add(run(() -> se("Voltorb Flip explosion")));
        for (int j = 0; j < 3; j++) {                                            // Part1
            final int frame = j;
            flow.add(run(() -> animation = new int[] {0, x, y, 704 + 64 * frame, 64, 64}));
            flow.add(wait(2));
            flow.add(run(() -> animation = null));
        }
        for (int j = 0; j < 6; j++) {                                            // Part2
            final int frame = j;
            flow.add(run(() -> animation = new int[] {1, x - 32, y - 32, frame * 128, 128, 128}));
            flow.add(wait(40 / 10));
            flow.add(run(() -> animation = null));
        }
        flow.add(message("\\me[Voltorb Flip game over]Oh no! You get 0 Coins!\\wtnp[50]"));   // unskippable text block
        showAndDispose(flow);
        flow.add(run(() -> {
            marks.clear();
        }));
        flow.add(run(() -> {
            if (level > 1) {                                                     // :245
                int newLevel = 0;
                for (int[] square : squares) if (square[3] == 1 && square[2] > 1) newLevel++;
                if (newLevel > level) newLevel = level;
                if (level > newLevel) {
                    level = newLevel;
                    if (level < 1) level = 1;
                    front(Arrays.asList(message("\\se[Voltorb Flip level down]Dropped to Game Lv. " + level + "!")));
                }
            }
        }));
        flow.add(run(() -> {
            levelText = "Level " + level;                                        // :263 the plugin's English text
            points = 0;
            updateCoins();
            numbers.clear();                                                     // revert numbers to 0s
            for (int i = 0; i < 5; i++) {
                updateRowNumbers(0, 0, i);
                updateColumnNumbers(0, 0, i);
            }
            firstRound = false;
            newGame();                                                           // pbDisposeSpriteHash + pbNewGame
        }));
    }

    /** What follows a C in normal mode (:360-): the coins, and the game clear when every 2 and 3 is found. */
    private void afterFlipNow() {
        afterFlip();
    }

    private void afterFlip() {
        int count = 0;
        for (int[] square : squares) if (square[3] == 0 && square[2] > 1) count++;   // :360-364
        updateCoins();                                                           // :365
        if (count != 0) return;                                                  // Game cleared
        List<Step> flow = new ArrayList<>();
        flow.add(run(() -> curtainOpacity = 100));
        flow.add(message("\\me[Voltorb Flip win]Game clear!\\wtnp[40]"));
        flow.add(message("\\se[Voltorb Flip gain coins]" + context.gameState().trainer().name + " received "
                + String.format("%,d", points) + " Coins!"));
        flow.add(run(() -> {
            levelText = "等级" + level;
            setCoins(coins() + points);
            points = 0;
            updateCoins();
            curtainOpacity = 0;
        }));
        showAndDispose(flow);
        flow.add(run(() -> {
            numbers.clear();                                                     // revert numbers to 0s
            for (int i = 0; i < 5; i++) {
                updateRowNumbers(0, 0, i);
                updateColumnNumbers(0, 0, i);
            }
            curtainOpacity = 100;
        }));
        flow.add(run(() -> {
            if (level < 8) {
                level += 1;
                front(Arrays.asList(message("\\se[Voltorb Flip level up]Advanced to Game Lv. " + level + "!")));
                firstRound = false;
            }
        }));
        flow.add(run(this::newGame));
        front(flow);
    }

    /** :408-442 the Back key. */
    private void leave() {
        List<Step> flow = new ArrayList<>();
        flow.add(run(() -> curtainOpacity = 100));
        if (points == 0) {
            flow.add(confirm("You haven't found any Coins! Are you sure you want to quit?", yes -> {
                if (!yes) return;
                List<Step> more = new ArrayList<>();
                more.add(run(() -> curtainOpacity = 0));
                showAndDispose(more);
                more.add(run(() -> quit = true));
                front(more);
            }));
        } else {
            final int earned = points;
            flow.add(confirm("现在退出将获得" + String.format("%,d", earned) + "枚代币。\n确定退出吗？", yes -> {
                if (!yes) return;
                List<Step> more = new ArrayList<>();
                more.add(message(context.gameState().trainer().name + "收到了" + String.format("%,d", earned) + "枚代币！"));
                more.add(run(() -> {
                    setCoins(coins() + points);
                    points = 0;
                    updateCoins();
                    curtainOpacity = 0;
                }));
                showAndDispose(more);
                more.add(run(() -> quit = true));
                front(more);
            }));
        }
        flow.add(run(() -> curtainOpacity = 0));
        front(flow);
    }

    /** pbShowAndDispose (:369-421): every card flips face up, then the board is cleared column by column. */
    private void showAndDispose(List<Step> flow) {
        for (int i = 0; i < 5; i++) {
            final int layer = i;
            flow.add(run(() -> layerVisible[layer] = true));
            if (i < 3) flow.add(wait(40 / 20));
            flow.add(run(() -> {
                Arrays.fill(layers[layer], -1);                                  // @sprites[i].bitmap.clear
                layerAbove[layer] = true;                                        // z = 99997
            }));
        }
        flow.add(run(() -> {
            se("Voltorb Flip tile");
            layerVisible[5] = true;
            marks.clear();
        }));
        flow.add(wait(40 / 10));
        flow.add(waitKey());                                                     // wait for user input to continue
        for (int i = 0; i < 5; i++) {                                            // dispose of tiles by column
            final int column = i;
            int[] srcs = {448, 384, 320, 896};
            flow.add(run(() -> se("Voltorb Flip tile")));
            for (int step = 0; step < 4; step++) {
                final int src = srcs[step];
                flow.add(run(() -> {
                    for (int j = 0; j < 5; j++) {
                        int[] square = squares[column + j * 5];
                        layers[column][column + j * 5] = src + (src == 448 ? square[2] * 64 : 0);
                    }
                }));
                flow.add(wait(40 / 20));
            }
        }
        flow.add(run(() -> {
            iconLayer.clear();
            for (int i = 0; i < 6; i++) Arrays.fill(layers[i], -1);
            cursorDrawn = false;                                                 // @sprites["cursor"].bitmap.clear
        }));
    }

    /** pbEndScene (:446-472): the curtains close, then the sprites fade out. */
    private int endPhase;
    private int endTick;

    private void endScene() {
        if (endPhase == 0) {
            curtainL = -180f;
            curtainR = 90f;
            curtainsVisible = true;
            endPhase = 1;
            return;
        }
        if (endPhase == 1) {
            int angleDiff = 18 * 20 / 40;
            curtainL += angleDiff;
            curtainR -= angleDiff;
            if (curtainL >= -90f) curtainLY -= 2;                                // fixes a minor graphical bug
            if (curtainL >= -90f) {
                endPhase = 2;
                endTick = 0;
            }
            return;
        }
        blackAlpha = Math.min(255f, endTick * 16f);                              // pbFadeOutAndHide
        if (++endTick > 16) finished = true;
    }

    // =====================================================================
    // render
    // =====================================================================

    private void draw(SpriteBatch b, Texture t, float h, float x, float y, int sx, int sy, int sw, int sh) {
        b.draw(t, x, h - y - sh, sw, sh, sx, sy, sw, sh, false, false);
    }

    private void text(SpriteBatch b, MenuFont f, String text, float x, float y, float w, float height) {
        if (f == null) return;
        float top = y + (height - f.lineHeight()) / 2f;
        f.drawCenteredFitted(b, text, x + w / 2f, ScreenMetrics.logicalHeight() - top, w, TEXT_BASE, TEXT_SHADOW);
    }

    @Override
    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin) {
        float w = ScreenMetrics.logicalWidth();
        float h = ScreenMetrics.logicalHeight();
        if (font26 == null) {
            font26 = MiniGames.font(context, 26);
            font28 = MiniGames.font(context, 28);
        }
        Texture bg = a.graphic(DIR, "boardbg");
        if (bg != null) b.draw(bg, 0f, h - bg.getHeight());
        text(b, font26, "你的代币", 8, 16, 118, 26);                                // :89-92
        text(b, font26, "获得的代币", 8, 82, 118, 26);
        text(b, font28, levelText, 8, 150, 118, 28);
        Texture tiles = a.graphic(DIR, "tiles");
        for (int[] mark : marks) if (tiles != null) draw(b, tiles, h, mark[0], mark[1], 256, 0, 64, 64);
        if (memoVisible) {
            Texture memo = a.graphic(DIR, "memo");
            if (memo != null) draw(b, memo, h, 10, 244, 0, 0, memo.getWidth(), memo.getHeight());
        }
        Texture small = a.graphic(DIR, "numbersSmall");
        for (int[] n : numbers) if (small != null) draw(b, small, h, n[0], n[1], n[2], 0, 16, 16);
        Texture score = a.graphic(DIR, "numbersScore");
        if (score != null) {
            for (int i = 0; i < 5; i++) {
                draw(b, score, h, 6 + i * 24, 44, totalDigits[i] * 24, 0, 24, 38);
                draw(b, score, h, 6 + i * 24, 110, pointDigits[i] * 24, 0, 24, 38);
            }
        }
        drawLayers(b, tiles, h, false);                                          // z 99996
        for (int[] icon : iconLayer) if (tiles != null) draw(b, tiles, h, icon[0], icon[1], icon[2], 0, 64, 64);   // z 99997
        drawLayers(b, tiles, h, true);
        Texture cursor = a.graphic(DIR, "cursor");
        if (cursor != null && cursorDrawn) draw(b, cursor, h, 128 + cursorX, cursorY, cursorSrcX, 0, 64, 64);     // z 99998
        if (curtainOpacity > 0) {                                                // z 99999 the dimming layer
            b.setColor(0f, 0f, 0f, curtainOpacity / 255f);
            b.draw(a.pixel(), 0f, 0f, w, h);
            b.setColor(WHITE);
        }
        if (curtainsVisible) drawCurtains(b, a, w, h);
        if (animation != null) {
            Texture image = a.graphic(DIR, animation[0] == 0 ? "tiles" : animation[0] == 1 ? "explosion" : "flipAnimation");
            if (image != null) draw(b, image, h, animation[1], animation[2], animation[3], 0, animation[4], animation[5]);
        }
        if (pbMessage.active()) pbMessage.render(b, a, f, skin, skin, w, h);
        if (blackAlpha > 0f) {
            b.setColor(0f, 0f, 0f, Math.min(1f, blackAlpha / 255f));
            b.draw(a.pixel(), 0f, 0f, w, h);
            b.setColor(WHITE);
        }
    }

    private void drawLayers(SpriteBatch b, Texture tiles, float h, boolean above) {
        if (tiles == null) return;
        for (int i = 0; i < 6; i++) {
            if (!layerVisible[i] || layerAbove[i] != above) continue;
            for (int j = 0; j < 25; j++) {
                if (layers[i][j] < 0) continue;
                draw(b, tiles, h, squares[j][0], squares[j][1], layers[i][j], 0, 64, 64);
            }
        }
    }

    /** The two black curtains: bitmaps rotated about (width/2, 0) by RGSS angles (positive = counter-clockwise). */
    private void drawCurtains(SpriteBatch b, MenuAssets a, float w, float h) {
        b.setColor(0f, 0f, 0f, 1f);
        // curtainL: Graphics.width x Graphics.height at (w/2, y), angle curtainL
        b.draw(a.pixel(), w / 2f, h - curtainLY - h, 0f, h, w, h, 1f, 1f, curtainL, 0, 0, 1, 1, false, false);
        // curtainR: Graphics.width x Graphics.height*2 at (w/2, 0), angle curtainR
        b.draw(a.pixel(), w / 2f, h - 2f * h, 0f, 2f * h, w, 2f * h, 1f, 1f, curtainR, 0, 0, 1, 1, false, false);
        b.setColor(WHITE);
    }

    @Override
    public void dispose() {
        if (font26 != null) font26.dispose();
        if (font28 != null) font28.dispose();
        font26 = font28 = null;
    }
}
