package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Pixmap;

/**
 * RGSS {@code Bitmap#hue_change(hue)}: rotates every pixel's hue by {@code hue}
 * degrees, leaving saturation, value and alpha alone. {@code BitmapCache.load_bitmap}
 * (BitmapCache:403-409) calls it on a copy of the cached bitmap, which is how
 * {@code AnimatedBitmap.new("Graphics/Animations/"+graphic, hue)} recolours the
 * animation sheets whose {@code @hue} is not 0 (PokeBattle_AnimationPlayer:812).
 *
 * <p>The RGSS DLL is closed source; this is the usual HSV rotation (the one
 * mkxp's {@code Bitmap::hueChange} performs).</p>
 */
public final class HueShift {

    private HueShift() {
    }

    /** One RGBA8888 pixel (libGDX layout, {@code 0xRRGGBBAA}) shifted by {@code hue} degrees. */
    public static int shift(int rgba, int hue) {
        int alpha = rgba & 0xFF;
        if (alpha == 0) {
            return rgba;
        }
        float r = ((rgba >>> 24) & 0xFF) / 255f;
        float g = ((rgba >>> 16) & 0xFF) / 255f;
        float b = ((rgba >>> 8) & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float h;
        if (delta == 0f) {
            return rgba;                         // grey: no hue to rotate
        } else if (max == r) {
            h = ((g - b) / delta) % 6f;
        } else if (max == g) {
            h = (b - r) / delta + 2f;
        } else {
            h = (r - g) / delta + 4f;
        }
        h = h * 60f + hue;
        h = ((h % 360f) + 360f) % 360f;
        float s = max == 0f ? 0f : delta / max;
        float v = max;
        float c = v * s;
        float x = c * (1f - Math.abs((h / 60f) % 2f - 1f));
        float m = v - c;
        float rr;
        float gg;
        float bb;
        int sector = (int) (h / 60f);
        switch (sector) {
            case 0: rr = c; gg = x; bb = 0; break;
            case 1: rr = x; gg = c; bb = 0; break;
            case 2: rr = 0; gg = c; bb = x; break;
            case 3: rr = 0; gg = x; bb = c; break;
            case 4: rr = x; gg = 0; bb = c; break;
            default: rr = c; gg = 0; bb = x; break;
        }
        int nr = Math.round((rr + m) * 255f);
        int ng = Math.round((gg + m) * 255f);
        int nb = Math.round((bb + m) * 255f);
        return (nr << 24) | (ng << 16) | (nb << 8) | alpha;
    }

    /** Shifts every pixel of {@code pixmap} in place. */
    public static void apply(Pixmap pixmap, int hue) {
        int width = pixmap.getWidth();
        int height = pixmap.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixmap.drawPixel(x, y, shift(pixmap.getPixel(x, y), hue));
            }
        }
    }
}
