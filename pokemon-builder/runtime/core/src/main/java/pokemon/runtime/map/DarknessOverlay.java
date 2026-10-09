package pokemon.runtime.map;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * 171_PField_Visuals:364-404 {@code DarknessSprite}: the black layer of a dark map with a soft circle of light around
 * the middle of the screen. Before Flash the circle has radius 64, after it 176.
 */
public final class DarknessOverlay {
    public static final int RADIUS_MIN = 64;                         // :381
    public static final int RADIUS_MAX = 176;                        // :382

    private final int width;
    private final int height;
    private int radius = RADIUS_MIN;
    private Texture texture;

    public DarknessOverlay(int width, int height) {
        this.width = width;
        this.height = height;
        refresh();
    }

    public int radius() {
        return radius;
    }

    /** {@code radius=} (:384-387). */
    public void radius(int value) {
        this.radius = value;
        refresh();
    }

    /** :389-403: black everywhere, then five rings that get lighter towards the middle. */
    private void refresh() {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);                    // fill_rect overwrites the pixels
        pixmap.setColor(0f, 0f, 0f, 1f);
        pixmap.fill();                                               // :390
        int cx = width / 2;
        int cy = height / 2;
        int cradius = radius;
        int numfades = 5;                                            // :394
        for (int i = 1; i <= numfades; i++) {                        // :395
            float alpha = (255f * (numfades - i) / numfades) / 255f;
            pixmap.setColor(0f, 0f, 0f, alpha);
            for (int j = cx - cradius; j <= cx + cradius; j++) {     // :396
                double diff2 = (double) cradius * cradius - (double) (j - cx) * (j - cx);   // :397
                int diff = (int) Math.sqrt(Math.max(0.0, diff2));    // :398
                pixmap.fillRectangle(j, cy - diff, 1, diff * 2);     // :399
            }
            cradius = (int) Math.floor(cradius * 0.9);               // :401
        }
        if (texture != null) {
            texture.dispose();
        }
        texture = new Texture(pixmap);
        pixmap.dispose();
    }

    /** Draws the layer over the screen whose lower-left corner is ({@code left}, {@code bottom}). */
    public void render(SpriteBatch batch, float left, float bottom) {
        batch.draw(texture, left, bottom, width, height);
    }

    public void dispose() {
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
    }
}
