package pokemon.runtime.map;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.Random;

/**
 * 179_PField_FieldMoves:78-189 {@code pbHiddenMoveAnimation}: the banner that opens across the screen, the Pokemon sliding
 * in from the right with its cry, then out to the left, and the strobes streaming across it. One {@link #tick()} is one
 * game frame at 40 fps. The banner is a band of {@code hiddenMovebg} the height of the picture, centred on the screen.
 */
public final class HiddenMoveAnimation {
    private static final int EXPAND_FRAMES = 40 / 4;
    private static final int SLIDE_FRAMES = 40 * 4 / 10;
    private static final int WAIT_FRAMES = 40 * 3 / 4;
    private static final int STROBES = 15;
    private static final int STROBE_WIDTH = 26 * 2;
    private static final int STROBE_HEIGHT = 8 * 2;
    /** {@code strobeSpeed = 64*20/40}. */
    private static final int STROBE_SPEED = 64 * 20 / 40;

    private final Texture background;
    private final Texture strobeSheet;
    private final Texture pokemon;
    private final int screenWidth;
    private final int screenHeight;
    private final Random random;
    private final int[] strobeX = new int[STROBES];
    private final int[] strobeY = new int[STROBES];
    private final boolean[] strobeVisible = new boolean[STROBES];

    private int phase = 1;
    private int frames;
    private boolean cryDue;
    private float pokemonX;
    private float pokemonY;
    private boolean pokemonVisible;

    /** The seconds the whole animation takes (phases 1-5). */
    public static float durationSeconds() {
        return (EXPAND_FRAMES + SLIDE_FRAMES + WAIT_FRAMES + 2 + SLIDE_FRAMES + EXPAND_FRAMES) / 40f;
    }

    public HiddenMoveAnimation(Texture background, Texture strobeSheet, Texture pokemon, int screenWidth,
                               int screenHeight, Random random) {
        this.background = background;
        this.strobeSheet = strobeSheet;
        this.pokemon = pokemon;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.random = random;
    }

    private int bandHeight() {
        return background == null ? 0 : background.getHeight();
    }

    private int bandTop() {
        return (screenHeight - bandHeight()) / 2;
    }

    /** The visible band height of this frame (the viewport rectangle of the plugin). */
    private float visibleBand() {
        int height = bandHeight();
        switch (phase) {
            case 1:
                return height * Math.min(1f, frames / (float) EXPAND_FRAMES);
            case 5:
                return height * Math.max(0f, 1f - frames / (float) EXPAND_FRAMES);
            case 6:
                return 0f;
            default:
                return height;
        }
    }

    public boolean finished() {
        return phase == 6;
    }

    /** True once, on the frame the Pokemon reaches the centre ({@code pbPlayCry(pokemon)}). */
    public boolean takeCry() {
        boolean due = cryDue;
        cryDue = false;
        return due;
    }

    /** One frame of the loop (:107-180). */
    public void tick() {
        if (phase == 6) {
            return;
        }
        int pokemonWidth = pokemon == null ? 0 : pokemon.getWidth();
        frames++;
        switch (phase) {
            case 1:                                                                // :112 the band opens
                if (frames >= EXPAND_FRAMES) {
                    phase = 2;
                    frames = 0;
                }
                break;
            case 2: {                                                              // :123 the Pokemon slides in
                float t = Math.min(1f, frames / (float) SLIDE_FRAMES);
                pokemonX = screenWidth + pokemonWidth / 2f + (screenWidth / 2f - (screenWidth + pokemonWidth / 2f)) * t;
                pokemonY = bandHeight() / 2f;
                pokemonVisible = true;
                if (frames >= SLIDE_FRAMES) {
                    phase = 3;
                    cryDue = true;                                                 // :130
                    frames = 0;
                }
                break;
            }
            case 3:                                                                // :133 it waits
                if (frames > WAIT_FRAMES) {
                    phase = 4;
                    frames = 0;
                }
                break;
            case 4: {                                                              // :143 it slides off to the left
                float t = Math.min(1f, frames / (float) SLIDE_FRAMES);
                pokemonX = screenWidth / 2f + (-(pokemonWidth / 2f) - screenWidth / 2f) * t;
                if (frames >= SLIDE_FRAMES) {
                    phase = 5;
                    pokemonVisible = false;
                    frames = 0;
                }
                break;
            }
            case 5:                                                                // :155 the band closes
                if (frames >= EXPAND_FRAMES) {
                    phase = 6;
                }
                break;
            default:
                break;
        }
        for (int i = 0; i < STROBES; i++) {                                        // :162-177
            if (!strobeVisible[i]) {
                strobeY[i] = randomY();
                strobeX[i] = random.nextInt(Math.max(1, screenWidth));
                strobeVisible[i] = true;
            } else if (strobeX[i] < screenWidth) {
                strobeX[i] += STROBE_SPEED;
            } else {
                strobeY[i] = randomY();
                strobeX[i] = -STROBE_WIDTH - random.nextInt(Math.max(1, screenWidth / 4));
            }
        }
    }

    private int randomY() {
        int rows = Math.max(1, bandHeight() / 16 - 2);
        return 16 * (1 + random.nextInt(rows)) + bandTop();
    }

    /**
     * Draws the banner. {@code left}/{@code bottom}/{@code height} are the screen's lower-left corner and height in world
     * pixels (y up); the plugin's y runs down from the top.
     */
    public void render(SpriteBatch batch, float left, float bottom, float height) {
        if (background == null || phase == 6) {
            return;
        }
        float band = visibleBand();
        float bandTopY = (screenHeight - band) / 2f;           // the viewport's top, in screen y (down)
        float bandBottomY = bandTopY + band;
        if (band <= 0f) {
            return;
        }
        // :115 bg.oy = (bg.height - rect.height) / 2: the middle of the picture shows
        int srcY = Math.round((bandHeight() - band) / 2f);
        int shown = Math.round(band);
        batch.draw(background, left, bottom + height - bandTopY - shown, screenWidth, shown, 0, srcY,
                background.getWidth(), shown, false, false);
        if (strobeSheet != null) {
            // The z order of the plugin: the even strobes (z 2) are above the Pokemon, the odd ones (z 0) below.
            for (int i = 1; i < STROBES; i += 2) {
                drawStrobe(batch, i, left, bottom, height, bandTopY, bandBottomY);
            }
        }
        if (pokemonVisible && pokemon != null) {                // :124-127 sprite.visible
            float w = pokemon.getWidth();
            float h = pokemon.getHeight();
            float x = pokemonX - w / 2f;
            float yTop = (screenHeight - bandHeight()) / 2f + pokemonY - h / 2f;
            drawClipped(batch, pokemon, 0, 0, pokemon.getWidth(), pokemon.getHeight(), x, yTop, left, bottom, height,
                    bandTopY, bandBottomY);
        }
        if (strobeSheet != null) {
            for (int i = 0; i < STROBES; i += 2) {
                drawStrobe(batch, i, left, bottom, height, bandTopY, bandBottomY);
            }
        }
    }

    private void drawStrobe(SpriteBatch batch, int i, float left, float bottom, float height, float top, float bottomY) {
        if (!strobeVisible[i]) {
            return;
        }
        drawClipped(batch, strobeSheet, 0, (i % 2) * STROBE_HEIGHT, STROBE_WIDTH, STROBE_HEIGHT,
                strobeX[i], strobeY[i], left, bottom, height, top, bottomY);
    }

    /** Draws a region at (x, yTop) in screen pixels (y down), clipped to the band and to the screen width. */
    private void drawClipped(SpriteBatch batch, Texture texture, int srcX, int srcY, int srcW, int srcH, float x, float yTop,
                             float left, float bottom, float height, float bandTopY, float bandBottomY) {
        float x0 = Math.max(x, 0f);
        float x1 = Math.min(x + srcW, screenWidth);
        float y0 = Math.max(yTop, bandTopY);
        float y1 = Math.min(yTop + srcH, bandBottomY);
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        int cropX = srcX + Math.round(x0 - x);
        int cropY = srcY + Math.round(y0 - yTop);
        int cropW = Math.round(x1 - x0);
        int cropH = Math.round(y1 - y0);
        batch.draw(texture, left + x0, bottom + height - y0 - cropH, cropW, cropH, cropX, cropY, cropW, cropH, false, false);
    }
}
