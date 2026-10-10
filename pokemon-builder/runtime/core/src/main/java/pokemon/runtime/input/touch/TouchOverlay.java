package pokemon.runtime.input.touch;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.ui.menu.MenuFont;

import java.util.List;

/**
 * Draws the on-screen keys (release plan P1). No image files: the key faces
 * are drawn into small {@link Pixmap}s once (signed-distance shapes, so the
 * edges are anti-aliased at any size) and scaled by the batch.
 *
 * <p>The overlay is translucent: a dark fill with a light ring reads on both
 * bright and dark maps, and a pressed key lights up.</p>
 */
public final class TouchOverlay {

    private static final int SIZE = 256;
    private static final Color RING = new Color(1f, 1f, 1f, 0.78f);
    private static final Color FILL = new Color(0f, 0f, 0f, 0.34f);
    private static final Color LIT = new Color(1f, 1f, 1f, 0.42f);
    private static final Color TEXT = new Color(1f, 1f, 1f, 0.95f);
    private static final Color TEXT_SHADOW = new Color(0f, 0f, 0f, 0.6f);

    private final Texture round = shape(TouchButton.Shape.ROUND, true);
    private final Texture square = shape(TouchButton.Shape.SQUARE, true);
    private final Texture pill = shape(TouchButton.Shape.PILL, true);
    private final Texture roundLit = shape(TouchButton.Shape.ROUND, false);
    private final Texture squareLit = shape(TouchButton.Shape.SQUARE, false);
    private final Texture cross = shape(TouchButton.Shape.CROSS, true);
    private final Texture crossLit = shape(TouchButton.Shape.CROSS, false);
    private final Texture pillLit = shape(TouchButton.Shape.PILL, false);
    private final Texture arrow = arrowTexture();
    private final Texture gear = gearTexture();
    private final MenuFont bigFont;
    private final MenuFont smallFont;
    private final OrthographicCamera camera = new OrthographicCamera();
    /** 0..1 multiplier from the settings page. */
    public float opacity = 1f;

    public TouchOverlay(MenuFont bigFont, MenuFont smallFont) {
        this.bigFont = bigFont;
        this.smallFont = smallFont;
    }

    public void render(SpriteBatch batch, int width, int height, List<TouchButton> buttons, TouchPad pad) {
        camera.setToOrtho(false, width, height);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (TouchButton button : buttons) {
            boolean lit = pad != null && pad.isDown(button.id);
            float gy = height - button.y - button.h;       // touch frame (y down) -> GL (y up)
            batch.setColor(1f, 1f, 1f, opacity);
            batch.draw(base(button.shape), button.x, gy, button.w, button.h);
            if (button.shape == TouchButton.Shape.CROSS) {
                crossDetails(batch, button, gy, pad);
                continue;
            }
            if (lit) {
                batch.draw(litShape(button.shape), button.x, gy, button.w, button.h);
            }
            if (button.arrow != TouchButton.ARROW_NONE) {
                float a = Math.min(button.w, button.h) * 0.46f;
                float rotation = button.arrow == TouchButton.ARROW_UP ? 0f
                        : button.arrow == TouchButton.ARROW_LEFT ? 90f
                        : button.arrow == TouchButton.ARROW_DOWN ? 180f : 270f;
                batch.draw(arrow, button.centerX() - a / 2, gy + button.h / 2 - a / 2, a / 2, a / 2, a, a,
                        1f, 1f, rotation, 0, 0, SIZE, SIZE, false, false);
            }
            if (TouchLayout.GEAR.equals(button.id)) {                       // the layout editor's key carries a drawn gear
                float a = Math.min(button.w, button.h) * 0.7f;
                batch.setColor(1f, 1f, 1f, opacity);
                batch.draw(gear, button.centerX() - a / 2, gy + button.h / 2 - a / 2, a, a);
            }
            batch.setColor(Color.WHITE);
            text(batch, button, gy);
        }
        batch.end();
    }

    /**
     * The layout editor: every key as usual, the key being moved lit, then the editor's bar and a one-line hint above it.
     *
     * @param selectedId the key picked for moving / resizing, or null
     */
    public void renderEditor(SpriteBatch batch, int width, int height, List<TouchButton> buttons, String selectedId,
                             List<TouchButton> bar, TouchPad barPad, String hint) {
        render(batch, width, height, buttons, null);
        camera.setToOrtho(false, width, height);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (TouchButton button : buttons) {
            if (button.id.equals(selectedId)) {
                float gy = height - button.y - button.h;
                batch.setColor(1f, 1f, 1f, 1f);
                batch.draw(litShape(button.shape), button.x, gy, button.w, button.h);
                batch.draw(litShape(button.shape), button.x, gy, button.w, button.h);   // twice: clearly brighter than a press
            }
        }
        batch.setColor(Color.WHITE);
        if (hint != null && !bar.isEmpty()) {
            TouchButton first = bar.get(0);
            float gy = height - first.y - first.h;
            Color main = new Color(TEXT.r, TEXT.g, TEXT.b, 1f);
            smallFont.drawCentered(batch, hint, width / 2f, gy + first.h + smallFont.lineHeight() * 1.6f, main, TEXT_SHADOW);
        }
        batch.end();
        float saved = opacity;
        opacity = 1f;                                                       // the bar is always readable
        render(batch, width, height, bar, barPad);
        opacity = saved;
    }

    /** Arm highlights (a lit arm is its own key) and the four arrow glyphs. */
    private void crossDetails(SpriteBatch batch, TouchButton pad, float gy, TouchPad state) {
        float bar = pad.w / 3f;
        String[] ids = {TouchLayout.UP, TouchLayout.DOWN, TouchLayout.LEFT, TouchLayout.RIGHT};
        int[] arrows = {TouchButton.ARROW_UP, TouchButton.ARROW_DOWN, TouchButton.ARROW_LEFT, TouchButton.ARROW_RIGHT};
        float[] rotation = {0f, 180f, 90f, 270f};
        float[][] arm = {{bar, 2 * bar}, {bar, 0}, {0, bar}, {2 * bar, bar}};   // lower-left corner of each arm, GL frame
        float a = bar * 0.5f;
        for (int i = 0; i < 4; i++) {
            float ax = pad.x + arm[i][0], ay = gy + arm[i][1];
            if (state != null && state.isDown(ids[i])) {
                batch.setColor(1f, 1f, 1f, opacity);
                batch.draw(squareLit, ax, ay, bar, bar);
            }
            batch.setColor(1f, 1f, 1f, opacity);
            batch.draw(arrow, ax + bar / 2 - a / 2, ay + bar / 2 - a / 2, a / 2, a / 2, a, a,
                    1f, 1f, rotation[i], 0, 0, SIZE, SIZE, false, false);
        }
        batch.setColor(Color.WHITE);
    }

    private void text(SpriteBatch batch, TouchButton button, float gy) {
        Color main = new Color(TEXT.r, TEXT.g, TEXT.b, TEXT.a * opacity);
        Color shadow = new Color(TEXT_SHADOW.r, TEXT_SHADOW.g, TEXT_SHADOW.b, TEXT_SHADOW.a * opacity);
        float centerY = gy + button.h / 2f;
        boolean hasCaption = button.caption != null && !button.caption.isEmpty();
        if (button.label != null && !button.label.isEmpty()) {
            float top = centerY + bigFont.lineHeight() / 2f + (hasCaption ? smallFont.lineHeight() * 0.35f : 0f);
            bigFont.drawCentered(batch, button.label, button.centerX(), top, main, shadow);
        }
        if (hasCaption) {
            float top = centerY - bigFont.lineHeight() * 0.28f;
            smallFont.drawCentered(batch, button.caption, button.centerX(), top, main, shadow);
        }
    }

    private Texture base(TouchButton.Shape shape) {
        return shape == TouchButton.Shape.ROUND ? round : shape == TouchButton.Shape.SQUARE ? square
                : shape == TouchButton.Shape.CROSS ? cross : pill;
    }

    private Texture litShape(TouchButton.Shape shape) {
        return shape == TouchButton.Shape.ROUND ? roundLit : shape == TouchButton.Shape.SQUARE ? squareLit
                : shape == TouchButton.Shape.CROSS ? crossLit : pillLit;
    }

    /** Signed distance of (px, py) from a rounded box centred in the texture. */
    private static float distance(TouchButton.Shape shape, float px, float py) {
        float half = SIZE / 2f;
        float x = px - half, y = py - half;
        if (shape == TouchButton.Shape.ROUND) {
            return (float) Math.sqrt(x * x + y * y) - (half - 2f);
        }
        if (shape == TouchButton.Shape.CROSS) {                       // union of a horizontal and a vertical bar
            float bar = SIZE / 6f, r = SIZE * 0.1f, reach = half - 2f;
            return Math.min(box(x, y, reach - r, bar - r, r), box(x, y, bar - r, reach - r, r));
        }
        float radius = shape == TouchButton.Shape.SQUARE ? SIZE * 0.22f : half - 2f;
        float bx = half - 2f - radius, by = half - 2f - radius;
        float qx = Math.max(Math.abs(x) - bx, 0f), qy = Math.max(Math.abs(y) - by, 0f);
        return (float) Math.sqrt(qx * qx + qy * qy) - radius;
    }

    private static float box(float x, float y, float bx, float by, float radius) {
        float qx = Math.max(Math.abs(x) - bx, 0f), qy = Math.max(Math.abs(y) - by, 0f);
        return (float) Math.sqrt(qx * qx + qy * qy) - radius;
    }

    /** {@code ring} = fill + outline of the idle key, otherwise the lit highlight. */
    private static Texture shape(TouchButton.Shape shape, boolean ring) {
        Pixmap pixmap = new Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        float ringWidth = SIZE * 0.028f;
        for (int py = 0; py < SIZE; py++) {
            for (int px = 0; px < SIZE; px++) {
                float d = distance(shape, px + 0.5f, py + 0.5f);
                float inside = clamp(0.5f - d);                        // 1 inside, anti-aliased edge
                Color color;
                float alpha;
                if (ring) {
                    float ringMask = clamp(0.5f - Math.abs(d + ringWidth / 2f) + ringWidth / 2f);
                    alpha = Math.max(inside * FILL.a, ringMask * RING.a);
                    color = ringMask > 0.5f ? RING : FILL;
                } else {
                    alpha = inside * LIT.a;
                    color = LIT;
                }
                pixmap.drawPixel(px, py, Color.rgba8888(color.r, color.g, color.b, alpha));
            }
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    private static Texture arrowTexture() {
        Pixmap pixmap = new Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        // Upward triangle, anti-aliased by supersampling.
        for (int py = 0; py < SIZE; py++) {
            for (int px = 0; px < SIZE; px++) {
                int hits = 0;
                for (int s = 0; s < 4; s++) {
                    float sx = px + (s % 2) * 0.5f + 0.25f, sy = py + (s / 2) * 0.5f + 0.25f;
                    float t = (sy - SIZE * 0.2f) / (SIZE * 0.6f);              // 0 at the tip, 1 at the base
                    if (t >= 0f && t <= 1f && Math.abs(sx - SIZE / 2f) <= t * SIZE * 0.4f) {
                        hits++;
                    }
                }
                pixmap.drawPixel(px, py, Color.rgba8888(1f, 1f, 1f, 0.9f * hits / 4f));
            }
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    /** A cogwheel: eight rounded teeth around a ring with a hole, anti-aliased by the distance to its edge. */
    private static Texture gearTexture() {
        Pixmap pixmap = new Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        float half = SIZE / 2f;
        float body = SIZE * 0.30f, tooth = SIZE * 0.12f, hole = SIZE * 0.13f;
        for (int py = 0; py < SIZE; py++) {
            for (int px = 0; px < SIZE; px++) {
                float x = px + 0.5f - half, y = py + 0.5f - half;
                float r = (float) Math.sqrt(x * x + y * y);
                float angle = (float) Math.atan2(y, x);
                float wave = clamp(((float) Math.sin(angle * 8f) * 2.2f + 1f) / 2f);   // 0..1, flat tops and gaps
                float edge = r - (body + tooth * wave);                  // < 0 inside the outline
                float alpha = clamp(0.5f - edge) * clamp(0.5f + (r - hole));
                pixmap.drawPixel(px, py, Color.rgba8888(1f, 1f, 1f, 0.92f * alpha));
            }
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    private static float clamp(float value) {
        return value < 0f ? 0f : value > 1f ? 1f : value;
    }

    public void dispose() {
        round.dispose();
        square.dispose();
        pill.dispose();
        roundLit.dispose();
        squareLit.dispose();
        pillLit.dispose();
        cross.dispose();
        crossLit.dispose();
        arrow.dispose();
        gear.dispose();
    }
}
