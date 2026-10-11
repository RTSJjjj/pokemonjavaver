package pokemon.runtime.ui;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Keeps the FreeType fonts usable on Android. When the app comes back from the background the OpenGL context may have been
 * lost, and the page textures of an incremental FreeType font are not always restored: every glyph then draws as a solid
 * white box. {@link #invalidate()} (called from {@code PokemonGame.resume()}) makes every font owner regenerate its page
 * textures the next time it draws, and {@link #looksBlank(BitmapFont)} lets a freshly generated font prove it really
 * has ink in its pages before it is used.
 */
public final class FontGuard {
    private static volatile int generation;

    private FontGuard() {
    }

    /** The application came back to the foreground: fonts built before this call must be rebuilt. */
    public static void invalidate() {
        generation++;
    }

    /** Changes every time {@link #invalidate()} is called; a font owner compares it with the one it was built under. */
    public static int generation() {
        return generation;
    }

    /**
     * True when the glyphs of a sample text have no ink in the font's page pixmap (all transparent), which is what a lost
     * texture looks like. Reading goes through the page's {@link TextureData}; a font whose pages cannot be read is
     * assumed to be fine.
     */
    public static boolean looksBlank(BitmapFont font) {
        if (font == null) {
            return false;
        }
        boolean readable = false;
        for (char c : new char[] {'A', '一', '的'}) {
            BitmapFont.Glyph glyph = font.getData().getGlyph(c);          // an incremental font generates it right here
            if (glyph == null || glyph.width <= 0 || glyph.height <= 0) {
                continue;
            }
            try {
                if (glyph.page < 0 || glyph.page >= font.getRegions().size) {
                    continue;
                }
                TextureRegion page = font.getRegions().get(glyph.page);
                TextureData data = page.getTexture().getTextureData();
                if (data == null || data.getType() != TextureData.TextureDataType.Pixmap) {
                    continue;
                }
                Pixmap pixmap = data.consumePixmap();
                if (pixmap == null) {
                    continue;
                }
                readable = true;
                for (int y = glyph.srcY; y < glyph.srcY + glyph.height && y < pixmap.getHeight(); y++) {
                    for (int x = glyph.srcX; x < glyph.srcX + glyph.width && x < pixmap.getWidth(); x++) {
                        if ((pixmap.getPixel(x, y) & 0xff) != 0) {
                            return false;                                  // ink found
                        }
                    }
                }
            } catch (RuntimeException unreadable) {
                // a page that cannot be inspected is not evidence of a blank font
            }
        }
        return readable;                                                    // readable pages and no ink in any sample glyph
    }
}
