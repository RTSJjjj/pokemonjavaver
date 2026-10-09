package pokemon.runtime.input.touch;

/**
 * One on-screen button (release plan P1). Coordinates are screen pixels with
 * the origin at the top left, the same frame libGDX reports touches in.
 *
 * <p>A button never talks to the game directly: it stands in for physical
 * keys ({@link #keys}), so touch input flows through the same
 * {@code KeyStateSource} -> {@code InputSampler} -> {@code InputManager}
 * path as a keyboard.</p>
 */
public final class TouchButton {

    public enum Shape { ROUND, SQUARE, PILL, CROSS }

    /** Arrow glyph drawn on a direction key (0 = none). */
    public static final int ARROW_NONE = 0, ARROW_UP = 1, ARROW_DOWN = 2, ARROW_LEFT = 3, ARROW_RIGHT = 4;

    public final String id;
    public final Shape shape;
    public final int arrow;
    /** Large glyph on the button face ("C"), may be empty. */
    public String label;
    /** Small caption under the glyph ("确认"), may be empty. */
    public String caption;
    public float x, y, w, h;

    public TouchButton(String id, Shape shape, int arrow, String label, String caption) {
        this.id = id;
        this.shape = shape;
        this.arrow = arrow;
        this.label = label;
        this.caption = caption;
    }

    public TouchButton place(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        return this;
    }

    public float centerX() { return x + w / 2f; }
    public float centerY() { return y + h / 2f; }

    /** Hit test with an outward slop, so the gaps between keys are not dead zones. */
    public boolean contains(float px, float py, float slop) {
        if (shape == Shape.CROSS) {                        // a plus: only its two bars are keys
            float bar = w / 3f;
            float dx = Math.abs(px - centerX()), dy = Math.abs(py - centerY());
            return (dx <= bar / 2f + slop && dy <= h / 2f + slop) || (dy <= bar / 2f + slop && dx <= w / 2f + slop);
        }
        return px >= x - slop && px <= x + w + slop && py >= y - slop && py <= y + h + slop;
    }

    /** Squared distance from the point to the button centre (nearest-key tie break). */
    public float distanceSquared(float px, float py) {
        float dx = px - centerX(), dy = py - centerY();
        return dx * dx + dy * dy;
    }
}
