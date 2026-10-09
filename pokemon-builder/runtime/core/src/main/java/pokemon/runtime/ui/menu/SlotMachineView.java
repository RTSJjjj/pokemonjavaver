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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 236_PMinigame_SlotMachine: {@code SlotMachineScene} (the three reels, the credit and payout counters, the coin / spin / stop
 * / win windows) inside the {@code pbFadeOutIn} of {@code pbSlotMachine(difficulty)}. Everything counts in 40 fps ticks
 * ({@code Graphics.update}); the plugin's blocking loops ({@code pbPayout}) are phases.
 */
public final class SlotMachineView {
    private static final int MAX_COINS = 99_999;                                // Settings:97
    private static final int SCROLLSPEED = 16;                                  // :14 must be a divisor of 48
    private static final int[][] ICONSPOOL = {
        {0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 6, 6, 7},            // 0 - Easy
        {0, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 4, 4, 5, 6, 7},                  // 1 - Medium (default)
        {0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 4, 4, 5, 6, 7},                        // 2 - Hard
    };
    private static final int[] SLIPPING = {0, 0, 0, 0, 0, 0, 1, 1, 1, 2, 2, 3}; // :20
    private static final String DIR = "Pictures/Slot Machine";

    private enum Phase { OUTER_OUT, FADE_IN, MAIN, WIN_ANIM, PAYOUT, PAYOUT_WAIT, LOSE_ANIM, MESSAGE, FADE_OUT, OUTER_IN, DONE }

    /** {@code SlotMachineReel} (:8-85). */
    private static final class Reel {
        final int[] reel;
        final int x;
        final int y;
        int toppos;
        boolean spinning;
        boolean stopping;
        int slipping;
        int index;

        Reel(int x, int y, int difficulty, Random random) {
            this.x = x;
            this.y = y;
            List<Integer> pool = new ArrayList<>();
            for (int v : ICONSPOOL[difficulty]) pool.add(v);
            Collections.shuffle(pool, random);                                  // :34 @reel.shuffle!
            reel = new int[pool.size()];
            for (int i = 0; i < reel.length; i++) reel[i] = pool.get(i);
            index = random.nextInt(reel.length);                                // :39
        }

        void stopSpinning(boolean noslipping, Random random) {                  // :48-52
            stopping = true;
            slipping = SLIPPING[random.nextInt(SLIPPING.length)];
            if (noslipping) slipping = 0;
        }

        /** {@code showing}: [0] top, [1] middle, [2] bottom. */
        int[] showing() {
            int[] array = new int[3];
            for (int i = 0; i < 3; i++) {
                int num = index - i;
                if (num < 0) num += reel.length;
                array[i] = reel[num];
            }
            return array;
        }

        void update() {                                                         // :62-82
            if (toppos == 0 && stopping && slipping == 0) {
                spinning = stopping = false;
            }
            if (spinning) {
                toppos += SCROLLSPEED;
                if (toppos > 0) {
                    toppos -= 48;
                    index = (index + 1) % reel.length;
                    if (slipping > 0) slipping -= 1;
                }
            }
        }
    }

    private final RuntimeContext context;
    private final Random random = new Random();
    private final MenuClock clock = new MenuClock();
    private final PbMessage pbMessage;
    private final Reel[] reels = new Reel[3];
    private final boolean[] buttons = new boolean[3];
    private final boolean[] rows = new boolean[5];
    private boolean light1;
    private boolean light2;
    private Phase phase = Phase.OUTER_OUT;
    private int tick;
    private float blackAlpha;
    private boolean finished;

    // scene state (:132-)
    private int credit;
    private int payoutScore;
    private int wager;
    private boolean gameRunning;
    private boolean gameEnd;
    private boolean replay;
    private int frame;
    private int bonus;
    private boolean[] wonRow = new boolean[5];
    private String window1;
    private int window1Frame;
    private String window2;
    private int window2Frame;
    private int animFrame;
    private int paywait;

    // input of the frame (Input.trigger?)
    private boolean pressC;
    private boolean pressDown;
    private boolean pressB;

    public SlotMachineView(RuntimeContext context, int difficulty) {
        this.context = context;
        this.pbMessage = new PbMessage(context);
        int level = Math.max(0, Math.min(2, difficulty));
        reels[0] = new Reel(64, 112, level, random);                            // :155-157
        reels[1] = new Reel(144, 112, level, random);
        reels[2] = new Reel(224, 112, level, random);
        credit = Math.min(MAX_COINS, context.gameState().fieldGlobals().coins);  // :176 SlotMachineScore.new(.., $PokemonGlobal.coins)
        window1 = "insert";                                                     // :170-172
        if (audio() != null) audio().memorizeBgmAndBgs();
    }

    private AudioManager audio() {
        return context.audioManager();
    }

    private void se(String name) {
        if (audio() != null) audio().playSe(name, 100, 100);
    }

    // =====================================================================
    // update
    // =====================================================================

    /** @return true when the whole screen is over */
    public boolean update(InputManager input) {
        int ticks = clock.advance();
        if (pbMessage.active()) {
            pbMessage.update(input, ticks);
            return finished;
        }
        pressC |= input.wasPressed(GameAction.CONFIRM);
        pressDown |= input.wasPressed(GameAction.DOWN);
        pressB |= input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU);
        for (int i = 0; i < ticks && !finished && !pbMessage.active(); i++) {
            step();
            pressC = pressDown = pressB = false;                                // Input.trigger? holds for one Graphics.update
        }
        return finished;
    }

    private void updateReels() {
        for (Reel reel : reels) reel.update();
    }

    private void step() {
        switch (phase) {
            case OUTER_OUT:                                                     // pbFadeOutIn
                blackAlpha = Math.min(255f, tick * 16f);
                if (++tick > 16) {
                    phase = Phase.FADE_IN;                                      // :182 pbFadeInAndShow
                    tick = 0;
                }
                break;
            case FADE_IN:
                blackAlpha = Math.max(0f, Math.min(255f, (16 - tick) * 16f));
                if (++tick > 16) {
                    phase = Phase.MAIN;
                    blackAlpha = 0f;
                }
                break;
            case MAIN:
                updateReels();
                main();
                break;
            case WIN_ANIM:
                updateReels();
                winAnimation();
                break;
            case PAYOUT:
                updateReels();
                payoutStep();
                break;
            case PAYOUT_WAIT:
                updateReels();
                if (++paywait >= 40 / 2) {                                      // :218 (Graphics.frame_rate/2).times
                    afterPayout();
                }
                break;
            case LOSE_ANIM:
                updateReels();
                loseAnimation();
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
                    context.gameState().fieldGlobals().coins = credit;           // :259 $PokemonGlobal.coins
                    if (audio() != null) audio().restoreBgmAndBgs();
                    phase = Phase.DONE;
                    finished = true;
                }
                break;
            default:
                break;
        }
    }

    /** One pass of {@code pbMain}'s loop body (:188-258). */
    private void main() {
        window1 = null;
        window2 = null;
        int spinFrameTime = 40 / 4;                                             // :189
        int insertFrameTime = 40 * 4 / 10;                                      // :190
        if (credit == MAX_COINS) {                                              // :196
            endWithMessage("获得了" + String.format("%,d", MAX_COINS) + "枚代币。");   // :197 to_s_formatted
            return;
        } else if (context.gameState().fieldGlobals().coins == 0) {             // :199
            endWithMessage("代币用完了。\n游戏结束！");                              // :200
            return;
        } else if (gameRunning) {                                               // :202 reels are spinning
            window1 = "stop";
            window1Frame = (frame / spinFrameTime) % 4;
            if (pressC) {                                                       // :205
                se("Slots stop");
                if (reels[0].spinning) {
                    reels[0].stopSpinning(replay, random);
                    buttons[0] = true;
                } else if (reels[1].spinning) {
                    reels[1].stopSpinning(replay, random);
                    buttons[1] = true;
                } else if (reels[2].spinning) {
                    reels[2].stopSpinning(replay, random);
                    buttons[2] = true;
                }
            }
            if (!reels[2].spinning) {                                           // :220
                gameEnd = true;
                gameRunning = false;
            }
        } else if (gameEnd) {                                                   // :225 reels have been stopped
            pbPayout();
            gameEnd = false;                                                    // (the reset of :235-240 happens after the payout)
            return;
        } else {                                                                // :242 awaiting coins for the next spin
            window1 = "insert";
            window1Frame = (frame / insertFrameTime) % 2;
            if (wager > 0) {
                window2 = "press";
                window2Frame = (frame / insertFrameTime) % 2;
            }
            if (pressDown && wager < 3 && credit > 0) {                         // :248
                se("Slots coin");
                wager += 1;
                credit -= 1;
                if (wager >= 3) {
                    rows[4] = true;
                    rows[3] = true;
                } else if (wager >= 2) {
                    rows[2] = true;
                    rows[1] = true;
                } else if (wager >= 1) {
                    rows[0] = true;
                }
            } else if (wager >= 3 || (wager > 0 && credit == 0) || (pressC && wager > 0) || replay) {   // :263
                if (replay) {
                    wager = 3;
                    for (int i = 0; i < 5; i++) rows[i] = true;
                }
                for (Reel reel : reels) reel.spinning = true;                    // :272-274 startSpinning
                frame = 0;
                gameRunning = true;
            } else if (pressB && wager == 0) {                                  // :278
                phase = Phase.FADE_OUT;
                tick = 0;
                return;
            }
        }
        frame = (frame + 1) % (40 * 4);                                         // :282
    }

    private void endWithMessage(String text) {
        phase = Phase.MESSAGE;
        pbMessage.start(text, null, 0, 0, ignored -> {
            phase = Phase.FADE_OUT;
            tick = 0;
        });
    }

    // =====================================================================
    // pbPayout (:92-147)
    // =====================================================================

    private void pbPayout() {
        replay = false;
        int payout = 0;
        bonus = 0;
        wonRow = new boolean[5];
        int[] reel1 = reels[0].showing();
        int[] reel2 = reels[1].showing();
        int[] reel3 = reels[2].showing();
        int[][] combinations = {
            {reel1[1], reel2[1], reel3[1]},                                     // centre row
            {reel1[0], reel2[0], reel3[0]},                                     // top row
            {reel1[2], reel2[2], reel3[2]},                                     // bottom row
            {reel1[0], reel2[1], reel3[2]},                                     // diagonal top left -> bottom right
            {reel1[2], reel2[1], reel3[0]},                                     // diagonal bottom left -> top right
        };
        for (int i = 0; i < combinations.length; i++) {
            if (i >= 1 && wager <= 1) break;                                    // one coin = centre row only
            if (i >= 3 && wager <= 2) break;                                    // two coins = three rows only
            wonRow[i] = true;
            int[] c = combinations[i];
            if (same(c, 1, 1, 1)) {
                payout += 8;                                                    // three Magnemites
            } else if (same(c, 2, 2, 2)) {
                payout += 8;                                                    // three Shellders
            } else if (same(c, 3, 3, 3)) {
                payout += 15;                                                   // three Pikachus
            } else if (same(c, 4, 4, 4)) {
                payout += 15;                                                   // three Psyducks
            } else if (same(c, 5, 5, 6) || same(c, 5, 6, 5) || same(c, 6, 5, 5)
                    || same(c, 6, 6, 5) || same(c, 6, 5, 6) || same(c, 5, 6, 6)) {
                payout += 90;                                                   // 777 multi-colored
                if (bonus < 1) bonus = 1;
            } else if (same(c, 5, 5, 5) || same(c, 6, 6, 6)) {
                payout += 300;                                                  // red 777, blue 777
                if (bonus < 2) bonus = 2;
            } else if (same(c, 7, 7, 7)) {
                replay = true;                                                  // three replays
            } else if (c[0] == 0) {                                             // left cherry
                payout += c[1] == 0 ? 4 : 2;                                    // centre cherry as well
            } else {
                wonRow[i] = false;
            }
        }
        payoutScore = Math.min(MAX_COINS, payout);                              // :134 @sprites["payout"].score=payout
        animFrame = 0;
        if (payout > 0 || replay) {
            if (audio() != null) audio().playMe(bonus > 0 ? "Slots big win" : "Slots win", 100, 100);   // :138-142
            phase = Phase.WIN_ANIM;
        } else {
            phase = Phase.LOSE_ANIM;
        }
    }

    private static boolean same(int[] c, int a, int b, int d) {
        return c[0] == a && c[1] == b && c[2] == d;
    }

    /** :143-171 the winning animation: 3 seconds. */
    private void winAnimation() {
        int timePerFrame = 40 / 8;
        window2 = null;
        window1 = "win";
        window1Frame = (animFrame / timePerFrame) % 4;
        if (bonus > 0) {
            window2 = "bonus";
            window2Frame = bonus - 1;
        }
        light1 = light2 = true;
        lightFrame = (animFrame / timePerFrame) % 4;
        for (int i = 0; i < 5; i++) {
            rows[i] = wonRow[i] && ((animFrame / timePerFrame) % 2) == 0;
        }
        animFrame += 1;
        if (animFrame == 40 * 3) {
            light1 = light2 = false;                                            // :173-175
            window1Frame = 0;
            phase = Phase.PAYOUT;
        }
    }

    private int lightFrame;

    /** :177-189 pay out: one coin per tick, C pays the rest at once. */
    private void payoutStep() {
        if (payoutScore <= 0) {
            paywait = 0;
            phase = Phase.PAYOUT_WAIT;
            return;
        }
        payoutScore -= 1;
        credit = Math.min(MAX_COINS, credit + 1);
        if (pressC || credit == MAX_COINS) {
            credit = Math.min(MAX_COINS, credit + payoutScore);
            payoutScore = 0;
        }
    }

    /** :199-205 the losing animation: 2 seconds. */
    private void loseAnimation() {
        int timePerFrame = 40 / 4;
        window2 = null;
        window1 = "lose";
        window1Frame = (animFrame / timePerFrame) % 2;
        animFrame += 1;
        if (animFrame == 40 * 2) afterPayout();
    }

    /** :207 {@code @wager = 0} and the reset of :232-240. */
    private void afterPayout() {
        wager = 0;
        Arrays_fill(buttons);
        for (int i = 0; i < 5; i++) rows[i] = false;
        gameEnd = false;
        phase = Phase.MAIN;
    }

    private static void Arrays_fill(boolean[] flags) {
        for (int i = 0; i < flags.length; i++) flags[i] = false;
    }

    // =====================================================================
    // render
    // =====================================================================

    private void tile(SpriteBatch b, Texture t, float h, float x, float topY, int sx, int sy, int w, int hh) {
        b.draw(t, x, h - topY - hh, w, hh, sx, sy, w, hh, false, false);
    }

    private void drawReel(SpriteBatch b, MenuAssets a, float h, Reel reel) {
        Texture images = a.graphic(DIR, "images");
        if (images != null) {
            for (int i = 0; i < 4; i++) {                                       // :74-78 blt(0, toppos + i*48, images, Rect(reel[num]*64, 0, 64, 48))
                int num = reel.index - i;
                if (num < 0) num += reel.reel.length;
                int top = reel.toppos + i * 48;
                int visTop = Math.max(0, top);
                int visBottom = Math.min(144, top + 48);                         // clipped to the 64 x 144 viewport
                if (visBottom <= visTop) continue;
                int srcY = visTop - top;
                tile(b, images, h, reel.x, reel.y + visTop, reel.reel[num] * 64, srcY, 64, visBottom - visTop);
            }
        }
        Texture shading = a.graphic(DIR, "ReelOverlay");
        if (shading != null) tile(b, shading, h, reel.x, reel.y, 0, 0, 64, 144);
    }

    private void drawScore(SpriteBatch b, MenuAssets a, float h, float x, float y, int score) {
        Texture numbers = a.graphic(DIR, "numbers");
        if (numbers == null) return;
        for (int i = 0; i < 5; i++) {
            int digit = (score / (int) Math.pow(10, i)) % 10;                    // least significant digit first
            tile(b, numbers, h, x + 14 * (4 - i), y, digit * 14, 0, 14, 22);
        }
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin) {
        float w = ScreenMetrics.logicalWidth();
        float h = ScreenMetrics.logicalHeight();
        Texture bg = a.graphic(DIR, "bg");                                      // :152 addBackgroundPlane "Slot Machine/bg"
        if (bg != null) b.draw(bg, 0f, h - bg.getHeight());
        for (Reel reel : reels) drawReel(b, a, h, reel);
        Texture button = a.graphic(DIR, "button");                              // :158-162
        for (int i = 0; i < 3; i++) {
            if (buttons[i] && button != null) b.draw(button, 68 + 80 * i, h - 260 - button.getHeight());
        }
        for (int i = 0; i < 5; i++) {                                           // :163-169 the lines
            if (!rows[i]) continue;
            int[] ys = {170, 122, 218, 82, 82};
            String name = "line" + (1 + (i + 1) / 2) + ((i + 1) >= 4 ? ((i + 1) == 4 ? "a" : "b") : "");
            Texture line = a.graphic(DIR, name);
            if (line != null) b.draw(line, 2, h - ys[i] - line.getHeight());
        }
        Texture lights = a.graphic(DIR, "lights");
        if (lights != null) {
            if (light1) tile(b, lights, h, 16, 32, 0, 26 * lightFrame, 96, 26);                 // :170-173
            if (light2) {                                                                       // :174-177 mirrored
                b.draw(lights, 240, h - 32 - 26, 96, 26, 0, 26 * lightFrame, 96, 26, true, false);
            }
        }
        if (window1 != null) {
            Texture t = a.graphic(DIR, window1);
            if (t != null) tile(b, t, h, 358, 96, 152 * window1Frame, 0, 152, 208);
        }
        if (window2 != null) {
            Texture t = a.graphic(DIR, window2);
            if (t != null) tile(b, t, h, 358, 96, 152 * window2Frame, 0, 152, "bonus".equals(window2) ? 104 : 208);
        }
        drawScore(b, a, h, 360, 66, credit);                                    // :178 credit
        drawScore(b, a, h, 438, 66, payoutScore);                               // :179 payout
        if (pbMessage.active()) pbMessage.render(b, a, f, skin, skin, w, h);
        if (blackAlpha > 0f) {
            b.setColor(0f, 0f, 0f, Math.min(1f, blackAlpha / 255f));
            b.draw(a.pixel(), 0f, 0f, w, h);
            b.setColor(Color.WHITE);
        }
    }
}
