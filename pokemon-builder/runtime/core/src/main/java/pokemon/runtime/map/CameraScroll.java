package pokemon.runtime.map;

/**
 * R6.22: Scroll Map (203). This project's {@code Game_Map#start_scroll} moves
 * the display by {@code 2^speed} quarter-pixels per 40 fps frame (speed 4 =
 * 5 tiles per second) and leaves the view where it stopped; the camera follows
 * the player again once the player moves.
 */
public final class CameraScroll {

    private int direction;
    private float remaining;
    private float speedPixels;
    private float offsetX;
    private float offsetY;

    /** Starts a scroll; a direction other than 2/4/6/8 or distance 0 is ignored. */
    public void start(int direction, int distance, int speed) {
        if (direction != 2 && direction != 4 && direction != 6 && direction != 8) {
            return;
        }
        if (distance <= 0) {
            return;
        }
        this.direction = direction;
        this.remaining = distance * TilesetGeometry.TILE_SIZE;
        int level = Math.max(1, Math.min(6, speed));
        this.speedPixels = (1 << level) / 4f * 40f; // quarter-px/frame -> px/s
    }

    public boolean active() {
        return remaining > 0.01f;
    }

    public float offsetX() {
        return offsetX;
    }

    public float offsetY() {
        return offsetY;
    }

    /** Advances the scroll; returns true while it is still running. */
    public boolean update(float delta) {
        if (!active()) {
            return false;
        }
        float step = Math.min(remaining, speedPixels * Math.max(0f, delta));
        switch (direction) {
            case 8: // up: the view follows the player from above -> world +y
                offsetY += step;
                break;
            case 2: // down
                offsetY -= step;
                break;
            case 4: // left
                offsetX -= step;
                break;
            case 6: // right
                offsetX += step;
                break;
            default:
                break;
        }
        remaining -= step;
        return active();
    }

    /** RMXP re-centres on the player as soon as the player moves. */
    public boolean reset() {
        boolean wasActive = active();
        remaining = 0f;
        offsetX = 0f;
        offsetY = 0f;
        return wasActive;
    }
}
