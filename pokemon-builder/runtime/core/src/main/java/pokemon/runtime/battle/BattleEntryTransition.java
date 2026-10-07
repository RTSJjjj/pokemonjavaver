package pokemon.runtime.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.ui.menu.MenuAssets;
import pokemon.runtime.ui.menu.MenuPanel;

/**
 * The project's KGC special transitions (the {@code Transitions} section,
 * 1718 lines). It aliases {@code Graphics.transition} and
 * {@code judge_special_transition} ({@code Transitions:48-95}) turns every
 * named battle transition into a class; {@code PField_Visuals:58-98} picks the
 * name from the time of day, the battle type and the location and plays it for
 * {@code 40*1.25 = 50} frames.
 *
 * <p>Only the names the battle entry can reach are transcribed:
 * {@code SnakeSquares}, {@code DiagonalBubble} (four origins),
 * {@code RisingSplash}, {@code TwoBallPass}, {@code SpinBallSplit},
 * {@code BallDown}, {@code WavySpinBall} and {@code FourBallBurst}. Frame
 * counts, formulas and z-orders follow the section line by line; the ones that
 * warp the frozen screen draw the captured {@code Graphics.snap_to_bitmap}
 * handed in by {@link BattleEntryAnimation}.</p>
 */
final class BattleEntryTransition {

    /** A lightweight RMXP-style sprite. */
    private static final class Sp {
        String tex;
        float x;
        float y;
        float ox;
        float oy;
        float zoomX = 1f;
        float zoomY = 1f;
        float angle;
        float opacity = 255f;
        boolean visible = true;
        /** src_rect; {@code srcW < 0} means the whole bitmap. */
        int srcX;
        int srcY;
        int srcW = -1;
        int srcH = -1;
        /** SnakeSquares / DiagonalBubble: the frame this tile appears on. */
        float frame;
        int z;
    }

    private final String name;
    private final int numFrames;
    private final MenuAssets assets;
    private Texture buffer;
    private final float width;
    private final float height;
    private Sp[] sprites = new Sp[0];
    /** Render order (indices into {@link #sprites} sorted by z, stable). */
    private int[] drawOrder = new int[0];
    private int duration;

    // The plugin's per-class step values.
    private float addZoom;
    private float addxMult;
    private float addAngle;
    private float subY;
    private float subY2;
    private float subY3;

    BattleEntryTransition(String name, int numFrames, MenuAssets assets, Texture buffer,
            float width, float height) {
        this.name = name == null ? "" : name.toLowerCase(java.util.Locale.ROOT);
        this.numFrames = Math.max(1, numFrames);
        this.assets = assets;
        this.buffer = buffer;
        this.width = width;
        this.height = height;
        this.duration = this.numFrames;
        java.util.List<Sp> list = new java.util.ArrayList<>();
        create(list);
        // RMXP draws sprites in ascending z; the creation order is kept inside
        // one z. Update indices stay in creation order, render uses this map.
        this.sprites = list.toArray(new Sp[0]);
        Integer[] order = new Integer[sprites.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        java.util.Arrays.sort(order, (a, b) -> Integer.compare(sprites[a].z, sprites[b].z));
        this.drawOrder = new int[sprites.length];
        for (int i = 0; i < order.length; i++) {
            drawOrder[i] = order[i];
        }
    }

    /** The captured {@code Graphics.snap_to_bitmap} for the warping transitions. */
    void setBuffer(Texture texture) {
        this.buffer = texture;
    }

    /** Whether this transition needs {@code Graphics.snap_to_bitmap}. */
    boolean needsBuffer() {
        switch (name) {
            case "risingsplash":
            case "twoballpass":
            case "spinballsplit":
            case "balldown":
            case "wavyspinball":
            case "threeballdown":
            case "wavythreeballup":
                return true;
            default:
                return false;
        }
    }

    boolean done() {
        return duration <= 0;
    }

    private Texture texture(String tex) {
        if ("buffer".equals(tex)) {
            return buffer;
        }
        return tex == null ? null : assets.graphic("Transitions", tex);
    }

    private Sp sprite(String tex, float x, float y) {
        Sp s = new Sp();
        s.tex = tex;
        s.x = x;
        s.y = y;
        return s;
    }

    private float texW(String tex) {
        Texture t = assets.graphic("Transitions", tex);
        return t == null ? 0f : t.getWidth();
    }

    private float texH(String tex) {
        Texture t = assets.graphic("Transitions", tex);
        return t == null ? 0f : t.getHeight();
    }

    /** Each transition class' {@code initialize}. */
    private void create(java.util.List<Sp> list) {
        switch (name) {
            case "snakesquares": {
                // Transitions:640-678.
                Texture square = assets.graphic("Transitions", "black_square");
                if (square == null) {
                    return;
                }
                int tw = square.getWidth();
                int th = square.getHeight();
                int cx = (int) (width / tw);
                int cy = (int) (height / th);
                int tiles = cx * cy;
                addZoom = 0.125f * 50f / numFrames;
                float[] frame = new float[tiles];
                for (int i = 0; i < cy; i++) {
                    for (int j = 0; j < cx; j++) {
                        int k = i * cx + j;
                        float x = tw * j;
                        if ((i < 3 && i % 2 == 1) || (i >= 3 && i % 2 == 0)) {
                            x = (cx - 1) * tw - x;
                        }
                        Sp s = sprite("black_square", x + tw / 2f, th * i);
                        s.ox = tw / 2f;
                        s.visible = false;
                        if (k >= tiles / 2) {
                            s.frame = 2f * (tiles - k - 1) * (numFrames - 1f / addZoom) / 50f;
                        } else {
                            s.frame = 2f * k * (numFrames - 1f / addZoom) / 50f;
                        }
                        list.add(s);
                    }
                }
                break;
            }
            case "diagonalbubbletl":
            case "diagonalbubbletr":
            case "diagonalbubblebl":
            case "diagonalbubblebr": {
                // Transitions:722-762. origin 0=TL, 1=TR, 2=BL, 3=BR.
                int origin = name.endsWith("tr") ? 1 : name.endsWith("bl") ? 2
                        : name.endsWith("br") ? 3 : 0;
                Texture square = assets.graphic("Transitions", "black_square");
                if (square == null) {
                    return;
                }
                int tw = square.getWidth();
                int th = square.getHeight();
                int cx = (int) (width / tw);
                int cy = (int) (height / th);
                int tiles = cx * cy;
                float l = 1.2f * width / (tiles - 8);
                float[] frame = new float[tiles];
                Sp[] ordered = new Sp[tiles];
                for (int i = 0; i < cy; i++) {
                    for (int j = 0; j < cx; j++) {
                        Sp s = sprite("black_square", tw * j + tw / 2f, th * i + th / 2f);
                        s.ox = tw / 2f;
                        s.oy = th / 2f;
                        s.visible = false;
                        int k = i * cx + j;
                        switch (origin) {
                            case 1:
                                k = i * cx + (cx - 1 - j);
                                break;
                            case 2:
                                k = tiles - 1 - (i * cx + (cx - 1 - j));
                                break;
                            case 3:
                                k = tiles - 1 - k;
                                break;
                            default:
                                break;
                        }
                        ordered[k] = s;
                        frame[k] = (float) Math.floor(
                                (0.6f * j * tw + 0.8f * i * th) * (numFrames / 50) / l);
                    }
                }
                for (int k = 0; k < tiles; k++) {
                    if (ordered[k] != null) {
                        ordered[k].frame = frame[k];
                        list.add(ordered[k]);
                    }
                }
                addZoom = 0.125f * 50f / numFrames;
                break;
            }
            case "risingsplash": {
                // Transitions:805-857.
                if (assets.graphic("Transitions", "water_1") == null
                        || assets.graphic("Transitions", "water_2") == null
                        || assets.graphic("Transitions", "black_half") == null) {
                    return;
                }
                Sp rear = sprite("black_half", 0f, 0f);
                rear.zoomY = 2f;
                rear.z = 1;
                list.add(rear);
                for (int i = 0; i < height / 2; i++) {
                    Sp s = sprite("buffer", 0f, i * 2f);
                    s.srcX = 0;
                    s.srcY = i * 2;
                    s.srcW = (int) width;
                    s.srcH = 2;
                    s.z = 2;
                    list.add(s);
                }
                Sp bubble = sprite("water_1", 0f, height);
                bubble.z = 3;
                list.add(bubble);
                Sp splash = sprite("water_2", 0f, height);
                splash.z = 4;
                list.add(splash);
                Sp black = sprite("black_half", 0f, height);
                black.zoomY = 2f;
                black.z = 5;
                list.add(black);
                subY = height * 2f / numFrames;
                subY2 = subY * 2f;
                subY3 = height / Math.max(1f, (float) Math.floor(numFrames * 0.1f));
                addAngle = 2f / (numFrames / 50f);
                break;
            }
            case "twoballpass": {
                // Transitions:911-961.
                float ballW = texW("ball_small");
                float ballH = texH("ball_small");
                if (ballW <= 0 || assets.graphic("Transitions", "black_half") == null) {
                    return;
                }
                Sp bg = sprite("buffer", width / 2f, height / 2f);
                bg.ox = width / 2f;
                bg.oy = height / 2f;
                list.add(bg);
                for (int i = 0; i < 2; i++) {
                    Sp black = sprite("black_half", (1 - i * 2) * width, i * height / 2f);
                    black.z = 1;
                    list.add(black);
                    Sp ball = sprite("ball_small",
                            (1 - i) * width + (1 - i * 2) * ballW / 2f,
                            height / 2f + (i * 2 - 1) * ballH / 2f);
                    ball.z = 2;
                    ball.ox = ballW / 2f;
                    ball.oy = ballH / 2f;
                    list.add(ball);
                }
                Sp mid = sprite("black_half", 0f, height / 2f);
                mid.z = 1;
                mid.oy = height / 4f;
                mid.zoomY = 0f;
                list.add(mid);
                addxMult = 2f * width / (float) Math.pow(numFrames * 0.6f, 2);
                addZoom = 0.02f * 50f / numFrames;
                break;
            }
            case "spinballsplit": {
                // Transitions:1011-1059.
                float ballW = texW("ball_large");
                float ballH = texH("ball_large");
                if (ballW <= 0 || assets.graphic("Transitions", "black_half") == null) {
                    return;
                }
                for (int i = 0; i < 2; i++) {
                    Sp bg = sprite("buffer", width / 2f, height / 2f);
                    bg.ox = width / 2f;
                    bg.oy = (1 - i) * height / 2f;
                    bg.srcX = 0;
                    bg.srcY = i * (int) (height / 2f);
                    bg.srcW = (int) width;
                    bg.srcH = (int) (height / 2f);
                    list.add(bg);
                    Sp black = sprite("black_half", (1 - i * 2) * width, i * height / 2f);
                    black.z = 1;
                    list.add(black);
                    Sp ball = sprite("ball_large", width / 2f, height / 2f);
                    ball.z = 2;
                    ball.ox = ballW / 2f;
                    ball.oy = (1 - i) * ballH / 2f;
                    ball.zoomX = 0f;
                    ball.zoomY = 0f;
                    list.add(ball);
                }
                addxMult = 2f * width / (float) Math.pow(numFrames * 0.5f, 2);
                addZoom = 0.02f * 50f / numFrames;
                break;
            }
            case "balldown": {
                // Transitions:1224-1276.
                float curveH = texH("black_curve");
                float ballW = texW("ball_small");
                float ballH = texH("ball_small");
                float blackH = texH("black_half");
                if (curveH <= 0 || ballW <= 0 || blackH <= 0) {
                    return;
                }
                Sp bg = sprite("buffer", width / 2f, height / 2f);
                bg.ox = width / 2f;
                bg.oy = height / 2f;
                list.add(bg);
                Sp blackRear = sprite("black_half", 0f, -curveH);
                blackRear.z = 1;
                blackRear.oy = blackH;
                blackRear.zoomY = 2f;
                list.add(blackRear);
                Sp curve = sprite("black_curve", 0f, -curveH);
                curve.z = 1;
                list.add(curve);
                Sp ball = sprite("ball_small", width / 2f, -ballH / 2f);
                ball.z = 2;
                ball.ox = ballW / 2f;
                ball.oy = ballH / 2f;
                ball.zoomX = 0.25f;
                ball.zoomY = 0.25f;
                list.add(ball);
                subY = (height + ballH * 2.5f) / (0.5f * numFrames);
                addAngle = 1.5f * 360f / (0.5f * numFrames);
                addZoom = 2.5f / (0.5f * numFrames);       // the ball's own zoom
                subY2 = (height + curveH) / (numFrames * 0.5f);
                subY3 = 0.02f * 50f / numFrames;           // the background zoom
                break;
            }
            case "wavyspinball": {
                // Transitions:1426-1477.
                float ballW = texW("ball_large");
                float ballH = texH("ball_large");
                if (ballW <= 0 || assets.graphic("Transitions", "black_half") == null) {
                    return;
                }
                Sp rear = sprite("black_half", 0f, 0f);
                rear.zoomY = 2f;
                rear.z = 1;
                list.add(rear);
                for (int i = 0; i < height / 2; i++) {
                    Sp s = sprite("buffer", 0f, i * 2f);
                    s.srcX = 0;
                    s.srcY = i * 2;
                    s.srcW = (int) width;
                    s.srcH = 2;
                    s.z = 2;
                    list.add(s);
                }
                Sp ball = sprite("ball_large", width / 2f, height / 2f);
                ball.z = 3;
                ball.ox = ballW / 2f;
                ball.oy = ballH / 2f;
                ball.visible = false;
                list.add(ball);
                Sp black = sprite("black_half", width / 2f, height / 2f);
                black.z = 4;
                black.ox = texW("black_half") / 2f;
                black.oy = texH("black_half") / 2f;
                black.visible = false;
                list.add(black);
                addAngle = 4f / (numFrames / 50f);
                break;
            }
            case "threeballdown": {
                // Transitions:1121-1176.
                float blackW = texW("black_square");
                float blackH = texH("black_square");
                float ballW = texW("ball_small");
                float ballH = texH("ball_small");
                if (blackW <= 0 || ballW <= 0) {
                    return;
                }
                int cx = (int) (width / blackW);
                int cy = (int) (height / blackH);
                int tiles = cx * cy;
                Sp bg = sprite("buffer", width / 2f, height / 2f);
                bg.ox = width / 2f;
                bg.oy = height / 2f;
                list.add(bg);
                int[] order = { 0, 4, 1, 6, 7, 2, 5, 3 };
                Sp[] blacks = new Sp[tiles];
                float[] frame = new float[tiles];
                for (int i = 0; i < cy; i++) {
                    for (int j = 0; j < cx; j++) {
                        int k = i * cx + j;
                        Sp s = sprite("black_square", blackW * j, blackH * i);
                        s.visible = false;
                        blacks[k] = s;
                        frame[k] = (float) Math.floor(((cy - i - 1) * 8 + order[j % 8])
                                * (numFrames * 0.75f) / tiles);
                    }
                }
                for (int k = 0; k < tiles; k++) {
                    if (blacks[k] == null) {
                        continue;
                    }
                    blacks[k].frame = frame[k];
                    list.add(blacks[k]);
                }
                int[] ballY = { 400, 0, 100 };
                for (int i = 0; i < 3; i++) {
                    Sp ball = sprite("ball_small", 96 + i * 160, -ballH - ballY[i]);
                    ball.z = 2;
                    ball.ox = ballW / 2f;
                    ball.oy = ballH / 2f;
                    list.add(ball);
                }
                subY = (height + 400 + ballH * 2) / (0.25f * numFrames);
                addAngle = 1.5f * 360f / (0.25f * numFrames);
                addZoom = 0.02f * 50f / numFrames;
                break;
            }
            case "wavythreeballup": {
                // Transitions:1322-1374.
                float ballW = texW("ball_small");
                float ballH = texH("ball_small");
                if (ballW <= 0 || assets.graphic("Transitions", "black_half") == null) {
                    return;
                }
                Sp rear = sprite("black_half", 0f, 0f);
                rear.zoomY = 2f;
                rear.z = 1;
                list.add(rear);
                for (int i = 0; i < height / 2; i++) {
                    Sp s = sprite("buffer", 0f, i * 2f);
                    s.srcX = 0;
                    s.srcY = i * 2;
                    s.srcW = (int) width;
                    s.srcH = 2;
                    s.z = 2;
                    list.add(s);
                }
                float[] ys = { height * 1.5f, height * 3.25f, height * 2.5f };
                for (int i = 0; i < 3; i++) {
                    Sp black = sprite("black_half", (i - 1) * width * 2f / 3f, ys[i]);
                    black.z = 3;
                    black.zoomY = 2f;
                    list.add(black);
                    Sp ball = sprite("ball_small", (2 * i + 1) * width / 6f, ys[i]);
                    ball.z = 4;
                    ball.ox = ballW / 2f;
                    ball.oy = ballH / 2f;
                    list.add(ball);
                }
                subY = height * 3.5f / (numFrames * 0.6f);
                addAngle = 4f / (numFrames / 50f);
                break;
            }
            case "fourballburst": {
                // Transitions:1527-1575.
                float ballW = texW("ball_small");
                float ballH = texH("ball_small");
                if (ballW <= 0 || texW("black_wedge_1") <= 0 || texW("black_wedge_2") <= 0
                        || texW("black_wedge_3") <= 0 || texW("black_wedge_4") <= 0) {
                    return;
                }
                int[] ballZ = { 2, 1, 3, 0 };
                for (int i = 0; i < 4; i++) {
                    Sp ball = sprite("ball_small", width / 2f, height / 2f);
                    ball.z = ballZ[i];
                    ball.ox = ballW / 2f;
                    ball.oy = ballH / 2f;
                    list.add(ball);
                }
                for (int i = 0; i < 4; i++) {
                    String wedge = "black_wedge_" + (i + 1);
                    Sp black = sprite(wedge, i == 1 ? 0f : width / 2f, i == 2 ? 0f : height / 2f);
                    black.ox = (i % 2 == 0) ? texW(wedge) / 2f : 0f;
                    black.oy = (i % 2 == 0) ? 0f : texH(wedge) / 2f;
                    black.zoomX = (i % 2 == 0) ? 0f : 1f;
                    black.zoomY = (i % 2 == 0) ? 1f : 0f;
                    black.visible = false;
                    list.add(black);
                }
                subY = (width / 2f + ballW / 2f) / (numFrames * 0.4f);   // addxball
                subY2 = (height / 2f + ballH / 2f) / (numFrames * 0.4f); // addyball
                addZoom = 1f / (numFrames * 0.6f);
                break;
            }
            default:
                break;
        }
    }

    void update() {
        if (duration <= 0) {
            return;
        }
        int elapsed = numFrames - duration;
        switch (name) {
            case "snakesquares":
            case "diagonalbubbletl":
            case "diagonalbubbletr":
            case "diagonalbubblebl":
            case "diagonalbubblebr": {
                // Transitions:697-712 / :781-797.
                boolean bubble = !"snakesquares".equals(name);
                for (Sp s : sprites) {
                    if (s.frame < elapsed && s.frame + (1f / addZoom) + 1f >= elapsed) {
                        s.visible = true;
                        s.zoomX = Math.min(1f, addZoom * (elapsed - s.frame));
                        s.zoomY = bubble ? s.zoomX : 1f;
                    }
                }
                break;
            }
            case "risingsplash": {
                // Transitions:881-903.
                float angadd = (numFrames - duration) * addAngle;
                float amp = Math.min(6f, 6f * angadd / 8f);
                int rows = (int) (height / 2);
                for (int i = 0; i < rows; i++) {
                    sprites[1 + i].x = (float) (amp * Math.sin((i + angadd) * Math.PI / 10));
                }
                int bubble = 1 + rows;
                sprites[bubble].x = (width - texW("water_1")) / 2f;
                sprites[bubble].x -= 32f * (float) Math.sin(
                        (numFrames - duration) / (numFrames / 50f) * 3 * Math.PI / 60);
                sprites[bubble].y -= subY;
                if (duration < numFrames * 0.5f) {
                    sprites[bubble + 1].y -= subY2;
                }
                if (duration < numFrames * 0.1f) {
                    sprites[bubble + 2].y -= subY3;
                    if (sprites[bubble + 2].y < 0f) {
                        sprites[bubble + 2].y = 0f;
                    }
                }
                break;
            }
            case "twoballpass": {
                // Transitions:982-1002.
                Sp bg = sprites[0];
                Sp blackA = sprites[1];
                Sp ballA = sprites[2];
                Sp blackB = sprites[3];
                Sp ballB = sprites[4];
                Sp mid = sprites[5];
                if (duration >= numFrames * 0.6f) {
                    float step = (width + texW("ball_small")) / (0.4f * numFrames);
                    ballA.x -= step;
                    ballB.x += step;
                    ballA.angle -= 360f / (0.2f * numFrames);
                    ballB.angle += 360f / (0.2f * numFrames);
                } else {
                    float addx = (numFrames * 0.6f - duration) * addxMult;
                    bg.zoomX += addZoom;
                    bg.zoomY += addZoom;
                    blackA.x -= addx;
                    blackB.x += addx;
                    mid.zoomY += 2f * addx / width;
                    if (blackA.x < 0f) {
                        blackA.x = 0f;
                    }
                    if (blackB.x > 0f) {
                        blackB.x = 0f;
                    }
                }
                break;
            }
            case "spinballsplit": {
                // Transitions:1081-1112.
                Sp bgA = sprites[0];
                Sp blackA = sprites[1];
                Sp ballA = sprites[2];
                Sp bgB = sprites[3];
                Sp blackB = sprites[4];
                Sp ballB = sprites[5];
                if (duration >= numFrames * 0.6f) {
                    if (ballA.zoomX < 1f) {
                        ballA.zoomX += 1f / (0.4f * numFrames);
                        ballA.zoomY += 1f / (0.4f * numFrames);
                        ballA.angle -= 360f / (0.4f * numFrames);
                        if (ballA.zoomX >= 1f) {
                            float ballW = texW("ball_large");
                            float ballH = texH("ball_large");
                            for (int i = 0; i < 2; i++) {
                                Sp s = i == 0 ? ballA : ballB;
                                s.srcX = 0;
                                s.srcY = i * (int) (ballH / 2f);
                                s.srcW = (int) ballW;
                                s.srcH = (int) (ballH / 2f);
                                s.zoomX = 1f;
                                s.zoomY = 1f;
                                s.angle = 0f;
                            }
                        }
                    }
                } else if (duration < numFrames * 0.5f) {
                    float addx = (numFrames * 0.5f - duration) * addxMult;
                    bgA.x -= addx;
                    bgB.x += addx;
                    blackA.x -= addx;
                    blackB.x += addx;
                    ballA.x -= addx;
                    ballB.x += addx;
                    bgA.zoomX += addZoom;
                    bgB.zoomX += addZoom;
                    bgA.zoomY += addZoom;
                    bgB.zoomY += addZoom;
                    if (blackA.x < 0f) {
                        blackA.x = 0f;
                    }
                    if (blackB.x > 0f) {
                        blackB.x = 0f;
                    }
                }
                break;
            }
            case "balldown": {
                // Transitions:1298-1313.
                Sp bg = sprites[0];
                Sp blackRear = sprites[1];
                Sp curve = sprites[2];
                Sp ball = sprites[3];
                if (duration >= numFrames * 0.5f) {
                    ball.y += subY;
                    ball.angle -= addAngle;
                    ball.zoomX += addZoom;
                    ball.zoomY += addZoom;
                } else {
                    curve.y += subY2;
                    blackRear.y = curve.y;
                    bg.zoomX += subY3;
                    bg.zoomY += subY3;
                }
                break;
            }
            case "wavyspinball": {
                // Transitions:1498-1518.
                float angadd = (numFrames - duration) * addAngle;
                float amp = Math.min(24f, 24f * angadd / 16f);
                int rows = (int) (height / 2);
                for (int i = 0; i < rows; i++) {
                    sprites[1 + i].x = (float) (amp * Math.sin((i + angadd) * Math.PI / 48))
                            * ((i % 2) * 2 - 1);
                }
                Sp ball = sprites[1 + rows];
                Sp black = sprites[1 + rows + 1];
                ball.visible = true;
                if (duration >= numFrames * 0.6f) {
                    ball.opacity = 255f * (numFrames - duration) / (numFrames * 0.4f);
                    ball.angle = -360f * (numFrames - duration) / (numFrames * 0.4f);
                } else if (duration < numFrames * 0.5f) {
                    black.visible = true;
                    black.zoomX = (numFrames * 0.5f - duration) / (numFrames * 0.5f);
                    black.zoomY = 2f * (numFrames * 0.5f - duration) / (numFrames * 0.5f);
                }
                break;
            }
            case "threeballdown": {
                // Transitions:1197-1214.
                if (duration >= numFrames * 0.75f) {
                    for (int i = 0; i < 3; i++) {
                        Sp ball = sprites[sprites.length - 3 + i];
                        ball.y += subY;
                        ball.angle -= addAngle * (i == 2 ? -1f : 1f);
                    }
                } else {
                    int count = (int) Math.floor(numFrames * 0.75f) - duration;
                    for (Sp s : sprites) {
                        if ("black_square".equals(s.tex) && s.frame <= count) {
                            s.visible = true;
                        }
                    }
                    Sp bg = sprites[0];
                    bg.zoomX += addZoom;
                    bg.zoomY += addZoom;
                }
                break;
            }
            case "wavythreeballup": {
                // Transitions:1398-1416.
                float angadd = (numFrames - duration) * addAngle;
                float amp = Math.min(24f, 24f * angadd / 16f);
                int rows = (int) (height / 2);
                for (int i = 0; i < rows; i++) {
                    sprites[1 + i].x = (float) (amp * Math.sin((i + angadd) * Math.PI / 48))
                            * ((i % 2) * 2 - 1);
                }
                int base = 1 + rows;   // interleaved black, ball per pillar
                if (duration < numFrames * 0.6f) {
                    for (int i = 0; i < 3; i++) {
                        Sp black = sprites[base + i * 2];
                        Sp ball = sprites[base + i * 2 + 1];
                        black.y -= subY;
                        if (black.y < 0f) {
                            black.y = 0f;
                        }
                        ball.y -= subY;
                        ball.angle += (2 * (i % 2) - 1) * (360f / (0.2f * numFrames));
                    }
                }
                break;
            }
            case "fourballburst": {
                // Transitions:1599-1615.
                if (duration >= numFrames * 0.6f) {
                    for (int i = 0; i < 4; i++) {
                        Sp s = sprites[i];
                        if (i == 1) {
                            s.x += subY;
                        } else if (i == 3) {
                            s.x -= subY;
                        }
                        if (i == 0) {
                            s.y += subY2;
                        } else if (i == 2) {
                            s.y -= subY2;
                        }
                    }
                } else {
                    for (int i = 4; i < 8; i++) {
                        Sp s = sprites[i];
                        s.visible = true;
                        if (i % 2 == 0) {
                            s.zoomX += addZoom;
                        } else {
                            s.zoomY += addZoom;
                        }
                    }
                }
                break;
            }
            default:
                break;
        }
        duration--;
    }

    // ------------------------------------------------------------------
    // Render
    // ------------------------------------------------------------------

    void render(SpriteBatch batch, float w, float h) {
        for (int index : drawOrder) {
            Sp s = sprites[index];
            if (!s.visible || s.opacity <= 0f) {
                continue;
            }
            if ("black".equals(s.tex)) {
                MenuPanel.fill(batch, assets, 0f, 0f, w, h, 0f, 0f, 0f, s.opacity / 255f);
                continue;
            }
            Texture tex = texture(s.tex);
            if (tex == null) {
                continue;
            }
            int sw = s.srcW < 0 ? tex.getWidth() : s.srcW;
            int sh = s.srcH < 0 ? tex.getHeight() : s.srcH;
            float dw = sw * s.zoomX;
            float dh = sh * s.zoomY;
            // RMXP: (x,y) is where the bitmap's origin lands; libgdx draws the
            // quad from its bottom-left and flips the y axis.
            float dx = s.x - s.ox * s.zoomX;
            float dy = h - (s.y - s.oy * s.zoomY) - dh;
            batch.setColor(1f, 1f, 1f, s.opacity / 255f);
            if (s.angle != 0f) {
                float originX = s.ox * s.zoomX;
                float originY = dh - s.oy * s.zoomY;
                // RMXP angles run clockwise; libgdx rotation is CCW.
                batch.draw(tex, dx, dy, originX, originY, dw, dh, 1f, 1f, -s.angle,
                        s.srcX, s.srcY, sw, sh, false, false);
            } else {
                batch.draw(tex, dx, dy, dw, dh, s.srcX, s.srcY, sw, sh, false, false);
            }
        }
        batch.setColor(Color.WHITE);
    }
}
