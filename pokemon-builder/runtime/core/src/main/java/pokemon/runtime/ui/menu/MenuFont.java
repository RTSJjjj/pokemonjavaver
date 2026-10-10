package pokemon.runtime.ui.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Disposable;

import java.io.File;

/**
 * L1/L14: the menu font - the project's pixel font through FreeType, same size
 * as the message window (R6.31) by default, with a two pixel shadow so labels
 * stay readable on the menu backgrounds. The title screen also uses a smaller
 * instance for the footer bar and the rotating message.
 */
public final class MenuFont implements Disposable {

    private static final int DEFAULT_SIZE = 22;
    private static final Color DEFAULT_SHADOW = new Color(0.34f, 0.34f, 0.34f, 1f);
    private static final Color DEFAULT_MAIN = Color.WHITE;

    private final GlyphLayout layout = new GlyphLayout();
    private FreeTypeFontGenerator generator;
    private BitmapFont font;

    public MenuFont(File fontFile) {
        this(fontFile, DEFAULT_SIZE);
    }

    public MenuFont(File fontFile, int size) {
        if (fontFile == null || !fontFile.isFile()) {
            Gdx.app.error("MenuFont", "menu font not found; menu text stays hidden");
            return;
        }
        generator = new FreeTypeFontGenerator(pokemon.runtime.data.ResourceCrypto.handle(fontFile));
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter parameter =
                    new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = size;
            parameter.incremental = true; // CJK glyphs are added on demand
            // RGSS draws text anti-aliased and unhinted: a stroke of the 12px-grid
            // FusionPixelMono at 20-22px is ~1.7px, so it shows as one solid pixel plus
            // one partial pixel and the 2px pbDrawShadowText copies sit right against it.
            // 1-bit (mono) rendering turned every stroke into a solid 2px bar and left
            // the shadow detached ("ghosting"), so the glyphs are anti-aliased like the
            // original (measured against the plugin screenshots).
            parameter.hinting = FreeTypeFontGenerator.Hinting.None;
            parameter.mono = false;
            parameter.minFilter = Texture.TextureFilter.Nearest;
            parameter.magFilter = Texture.TextureFilter.Nearest;
            font = generator.generateFont(parameter);
        } catch (RuntimeException error) {
            generator.dispose();
            generator = null;
            throw error;
        }
    }

    public boolean ready() {
        return font != null;
    }

    public BitmapFont font() {
        return font;
    }

    /**
     * The font's line height in pixels (0 when missing). Text drawn with
     * {@code y == lineHeight()} sits exactly on the bottom edge, because the
     * menu font anchors at the top of the text and draws downwards.
     */
    /**
     * True when the font really has a glyph for this character. The incremental
     * FreeType font generates glyphs on demand and answers null for characters the
     * font file lacks, so callers draw a box instead of risking the missing glyph.
     */
    public boolean hasGlyph(int codePoint) {
        if (font == null || codePoint > 0xFFFF) {
            return false;
        }
        try {
            return font.getData().getGlyph((char) codePoint) != null;
        } catch (RuntimeException error) {
            return false;
        }
    }

    public float lineHeight() {
        return font == null ? 0f : font.getData().lineHeight;
    }

    /** Width of {@code text} in pixels (0 when the font is missing). */
    public float width(String text) {
        if (font == null || text == null || text.isEmpty()) {
            return 0f;
        }
        layout.setText(font, text);
        return layout.width;
    }

    /** Draws text with the project's 2 px shadow (the default menu colors). */
    public void draw(SpriteBatch batch, String text, float x, float y) {
        draw(batch, text, x, y, DEFAULT_MAIN, DEFAULT_SHADOW);
    }

    /** Draws text with explicit main / shadow colors (L14 footer & message). */
    public void draw(SpriteBatch batch, String text, float x, float y, Color main, Color shadow) {
        if (font == null || text == null || text.isEmpty()) {
            return;
        }
        // pbDrawShadowText draws the shadow THREE times - at (+2,0), (0,+2) and
        // (+2,+2) - and the base last. A single diagonal copy (what this used to
        // do) leaves the strokes looking thin and lets the background show
        // through, so all three are drawn here too.
        if (shadowEnabled) {
            font.setColor(shadow);
            font.draw(batch, text, x + 2f, y);
            font.draw(batch, text, x, y - 2f);
            font.draw(batch, text, x + 2f, y - 2f);
        }
        font.setColor(main);
        font.draw(batch, text, x, y);
        font.setColor(Color.WHITE);
    }

    /**
     * Menu text shadow: ON, like {@code pbDrawShadowText}. The ghosting
     * experiment is over - run with {@code -Dpokemon.menu.shadow=false} to
     * render the base text only.
     */
    public static boolean shadowEnabled = !"false".equalsIgnoreCase(System.getProperty("pokemon.menu.shadow"));

    /** Draws text centred on {@code centerX}. */
    public void drawCentered(SpriteBatch batch, String text, float centerX, float y) {
        drawCentered(batch, text, centerX, y, DEFAULT_MAIN, DEFAULT_SHADOW);
    }

    public void drawCentered(SpriteBatch batch, String text, float centerX, float y, Color main, Color shadow) {
        if (font == null || text == null || text.isEmpty()) {
            return;
        }
        draw(batch, text, centerX - width(text) / 2f, y, main, shadow);
    }

    /**
     * {@code Bitmap#draw_text(x,y,width,height,text,align)} with a rectangle narrower than the text: RGSS squeezes the
     * text horizontally to fit. Draws centred on {@code centerX}, at most {@code maxWidth} wide.
     */
    public void drawCenteredFitted(SpriteBatch batch, String text, float centerX, float y, float maxWidth, Color main, Color shadow) {
        if (font == null || text == null || text.isEmpty()) {
            return;
        }
        float natural = width(text);
        if (natural <= maxWidth) {
            drawCentered(batch, text, centerX, y, main, shadow);
            return;
        }
        float squeeze = maxWidth / natural;
        font.getData().setScale(squeeze, 1f);
        float x = centerX - maxWidth / 2f;
        draw(batch, text, x, y, main, shadow);
        font.getData().setScale(1f, 1f);
    }

    /** Draws text right-aligned to {@code rightX}. */
    public void drawRight(SpriteBatch batch, String text, float rightX, float y, Color main, Color shadow) {
        if (font == null || text == null || text.isEmpty()) {
            return;
        }
        draw(batch, text, rightX - width(text), y, main, shadow);
    }

    @Override
    public void dispose() {
        if (font != null) {
            font.dispose();
            font = null;
        }
        if (generator != null) {
            generator.dispose();
            generator = null;
        }
    }
}
