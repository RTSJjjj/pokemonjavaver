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
        generator = new FreeTypeFontGenerator(Gdx.files.absolute(fontFile.getAbsolutePath()));
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter parameter =
                    new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = size;
            parameter.incremental = true; // CJK glyphs are added on demand
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
        font.setColor(shadow);
        font.draw(batch, text, x + 2f, y - 2f);
        font.setColor(main);
        font.draw(batch, text, x, y);
        font.setColor(Color.WHITE);
    }

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
