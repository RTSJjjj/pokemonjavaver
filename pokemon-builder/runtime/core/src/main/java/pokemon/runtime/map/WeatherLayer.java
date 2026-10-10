package pokemon.runtime.map;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.state.ScreenWeather;

import java.util.Random;

/**
 * 172_PField_Weather {@code RPG::Weather}: the rain / storm / snow / blizzard / sandstorm particles of the field. Up to 40 sprites
 * drift across the map view; the plugin's own bitmaps are drawn here with {@link Pixmap}s (the same {@code fill_rect}s).
 *
 * <p>登记: the plugin also sets a tone and a storm flash on {@code @viewport}, a viewport that holds no sprite (the particles live
 * in the map's viewport), so neither is ever visible there and neither is drawn here; Sun and Fog have no bitmaps either.
 * {@code $GameSpeed} is not modelled (the weather runs every frame).</p>
 */
public final class WeatherLayer {

    /** {@code @weatherTypes[type]}: [+x per frame, +y per frame, +opacity per frame]; null entries draw nothing. */
    private static final int[][] STEP = new int[9][];

    static {
        STEP[ScreenWeather.RAIN] = new int[] {-6, 24, -8};
        STEP[ScreenWeather.HEAVY_RAIN] = new int[] {-24, 24, -4};
        STEP[ScreenWeather.STORM] = new int[] {-24, 24, -4};
        STEP[ScreenWeather.SNOW] = new int[] {-4, 8, 0};
        STEP[ScreenWeather.BLIZZARD] = new int[] {-16, 16, -4};
        STEP[ScreenWeather.SANDSTORM] = new int[] {-12, 4, -2};
    }

    private static final float FRAMES_PER_SECOND = 40f;
    private static final int SPRITES = 40;

    private final Random random;
    private final int viewWidth;
    private final int viewHeight;

    private int type;
    private int max;
    private float ox;
    private float oy;
    private float clock;

    private final float[] x = new float[SPRITES];
    private final float[] y = new float[SPRITES];
    private final float[] opacity = new float[SPRITES];
    private final boolean[] mirror = new boolean[SPRITES];
    private final int[] bitmap = new int[SPRITES];            // index into the type's bitmaps, -1 = none
    private boolean spritesExist;
    private Texture[] bitmaps = new Texture[0];

    public WeatherLayer(Random random, int viewWidth, int viewHeight) {
        this.random = random;
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
    }

    // ---------------------------------------------------------------- state (Spriteset_Map:156-158)

    /** {@code @weather.type = $game_screen.weather_type}, {@code .max = weather_max}, {@code .ox/.oy}, then {@code update} per frame. */
    public void update(float delta, int newType, float newMax, float mapOx, float mapOy) {
        setType(newType);
        setMax(newMax);
        ox = mapOx;
        oy = mapOy;
        clock += Math.max(0f, delta) * FRAMES_PER_SECOND;
        int frames = 0;
        while (clock >= 1f && frames < 8) {                    // a long pause must not replay minutes of rain
            clock -= 1f;
            frames++;
            frame();
        }
        if (frames == 8) {
            clock = 0f;
        }
    }

    /** {@code max=} (:172-183): 0 removes the sprites. */
    private void setMax(float value) {
        int clamped = (int) Math.min(Math.max(value, 0f), 40f);
        if (max == clamped) {
            return;
        }
        max = clamped;
        if (max == 0) {
            spritesExist = false;
        }
    }

    /** {@code type=} (:185-208). */
    private void setType(int value) {
        if (type == value) {
            return;
        }
        type = value;
        if (type == ScreenWeather.NONE) {
            spritesExist = false;
            return;
        }
        disposeBitmaps();
        switch (type) {
            case ScreenWeather.RAIN: bitmaps = new Texture[] {rainBitmap()}; break;
            case ScreenWeather.HEAVY_RAIN:
            case ScreenWeather.STORM: bitmaps = new Texture[] {stormBitmap()}; break;
            case ScreenWeather.SNOW: bitmaps = snowBitmaps(); break;
            case ScreenWeather.BLIZZARD: bitmaps = blizzardBitmaps(); break;
            case ScreenWeather.SANDSTORM: bitmaps = sandstormBitmaps(); break;
            default: bitmaps = new Texture[0]; break;          // Sun / Fog
        }
        ensureSprites();
        boolean flips = type == ScreenWeather.BLIZZARD || type == ScreenWeather.SANDSTORM;
        for (int i = 0; i < SPRITES; i++) {
            mirror[i] = flips && random.nextInt(2) == 0;
            bitmap[i] = bitmaps.length == 0 ? -1 : spriteBitmap(i);
        }
    }

    /** {@code weatherBitmaps[i % weatherBitmaps.length]}; the blizzard lists its bitmaps 3x for the big ones (:154-161). */
    private int spriteBitmap(int i) {
        return i % bitmapSlots();
    }

    private int bitmapSlots() {
        return type == ScreenWeather.BLIZZARD ? 8 : bitmaps.length;
    }

    /** {@code ensureSprites} (:162-175): 40 invisible sprites at the origin; the first frame places them. */
    private void ensureSprites() {
        if (spritesExist) {
            return;
        }
        spritesExist = true;
        for (int i = 0; i < SPRITES; i++) {
            x[i] = 0f;
            y[i] = 0f;
            opacity[i] = 0f;
            mirror[i] = false;
            bitmap[i] = -1;
        }
    }

    // ---------------------------------------------------------------- one frame (:231-279)

    private void frame() {
        if (type == ScreenWeather.NONE || type == ScreenWeather.SUN || type == ScreenWeather.FOG) {
            return;
        }
        int[] step = STEP[type];
        if (step == null) {
            return;
        }
        ensureSprites();
        boolean flips = type == ScreenWeather.BLIZZARD || type == ScreenWeather.SANDSTORM;
        boolean drifts = type == ScreenWeather.SNOW || type == ScreenWeather.BLIZZARD;
        for (int i = 1; i <= max && i < SPRITES; i++) {
            x[i] += step[0];
            if (drifts) {
                x[i] += new int[] {2, 0, 0, -2}[random.nextInt(4)];
            }
            y[i] += step[1];
            opacity[i] = Math.max(0f, Math.min(255f, opacity[i] + step[2]));
            float sx = x[i] - ox;
            float sy = y[i] - oy;
            if (opacity[i] < 64 || sx < -50 || sx > viewWidth + 128 || sy < -300 || sy > viewHeight + 20) {
                x[i] = random.nextInt(viewWidth + 150) - 50 + ox;
                y[i] = random.nextInt(viewHeight + 150) - 200 + oy;
                opacity[i] = 255;
                mirror[i] = flips && random.nextInt(2) == 0;
            }
        }
    }

    // ---------------------------------------------------------------- drawing

    /**
     * Draws the particles in world coordinates (y up): {@code worldOriginX/Y} is the lower left corner of the view. The batch
     * must be between begin and end.
     */
    public void render(SpriteBatch batch, float worldOriginX, float worldOriginY) {
        if (!spritesExist || type == ScreenWeather.NONE || bitmaps.length == 0) {
            return;
        }
        for (int i = 0; i < SPRITES && i <= max; i++) {
            int slot = bitmap[i];
            if (slot < 0 || opacity[i] <= 0f) {
                continue;
            }
            Texture texture = bitmaps[type == ScreenWeather.BLIZZARD ? blizzardIndex(slot) : slot];
            float screenX = x[i] - ox;
            float screenY = y[i] - oy;
            if (screenX > viewWidth || screenY > viewHeight || screenX + texture.getWidth() < 0 || screenY + texture.getHeight() < 0) {
                continue;
            }
            batch.setColor(1f, 1f, 1f, opacity[i] / 255f);
            float worldY = worldOriginY + viewHeight - screenY - texture.getHeight();
            batch.draw(texture, worldOriginX + screenX, worldY, texture.getWidth(), texture.getHeight(),
                    0, 0, texture.getWidth(), texture.getHeight(), mirror[i], false);
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    /** The blizzard's list of 8: 1, 2, 3, 3, 3, 4, 4, 4 over four bitmaps (:154-161). */
    private static int blizzardIndex(int slot) {
        return slot < 2 ? slot : slot < 5 ? 2 : 3;
    }

    // ---------------------------------------------------------------- bitmaps (:70-152)

    private static Pixmap pixmap(int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        return pixmap;
    }

    private static Texture texture(Pixmap pixmap) {
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }

    private static void rect(Pixmap pixmap, int x, int y, int w, int h, int rgb) {
        pixmap.setColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, 1f);
        pixmap.fillRectangle(x, y, w, h);
    }

    /** {@code prepareRainBitmap} (:70-77). */
    private static Texture rainBitmap() {
        Pixmap p = pixmap(32, 128);
        for (int i = 0; i < 16; i++) {
            rect(p, 30 - i * 2, i * 8, 2, 8, 0xFFFFFF);
        }
        return texture(p);
    }

    /** {@code prepareStormBitmap} (:79-86). */
    private static Texture stormBitmap() {
        Pixmap p = pixmap(192, 192);
        for (int i = 0; i < 96; i++) {
            rect(p, 190 - i * 2, i * 2, 2, 2, 0xFFFFFF);
        }
        return texture(p);
    }

    private static final int SNOW = 0xE0E8F0;

    /** The fatter + shape / the diamond of {@code prepareSnowBitmaps} (:88-110). */
    private static Pixmap fatterPlus() {
        Pixmap p = pixmap(10, 10);
        rect(p, 2, 0, 4, 2, SNOW);
        rect(p, 0, 2, 8, 4, SNOW);
        rect(p, 2, 6, 4, 2, SNOW);
        return p;
    }

    private static Pixmap diamond() {
        Pixmap p = pixmap(10, 10);
        rect(p, 4, 0, 2, 2, SNOW);
        rect(p, 2, 2, 6, 2, SNOW);
        rect(p, 0, 4, 10, 2, SNOW);
        rect(p, 2, 6, 6, 2, SNOW);
        rect(p, 4, 8, 2, 2, SNOW);
        return p;
    }

    private static Texture[] snowBitmaps() {
        Pixmap small = pixmap(10, 10);
        rect(small, 4, 2, 2, 2, SNOW);                         // small + shape
        rect(small, 2, 4, 6, 2, SNOW);
        rect(small, 4, 6, 2, 2, SNOW);
        return new Texture[] {texture(small), texture(fatterPlus()), texture(diamond())};
    }

    private Texture noise(int[] colors) {
        Pixmap p = pixmap(200, 200);
        for (int i = 0; i < 540; i++) {
            int color = colors[random.nextInt(colors.length)];
            rect(p, random.nextInt(100) * 2, random.nextInt(100) * 2, 2, 2, color);
        }
        return texture(p);
    }

    /** {@code prepareBlizzardBitmaps} (:112-142): two shapes and two 200x200 fields of 540 flecks. */
    private Texture[] blizzardBitmaps() {
        int[] white = {SNOW};
        return new Texture[] {texture(fatterPlus()), texture(diamond()), noise(white), noise(white)};
    }

    /** {@code prepareSandstormBitmaps} (:144-165). */
    private Texture[] sandstormBitmaps() {
        int[] colors = {rgb8(31, 28, 17), rgb8(23, 16, 9), rgb8(29, 24, 15), rgb8(26, 20, 12), rgb8(20, 13, 6),
                rgb8(31, 30, 20), rgb8(27, 25, 20)};
        return new Texture[] {noise(colors), noise(colors)};
    }

    private static int rgb8(int r, int g, int b) {
        return ((r * 8) << 16) | ((g * 8) << 8) | (b * 8);
    }

    private void disposeBitmaps() {
        for (Texture texture : bitmaps) {
            if (texture != null) texture.dispose();
        }
        bitmaps = new Texture[0];
    }

    public void dispose() {
        disposeBitmaps();
    }
}
