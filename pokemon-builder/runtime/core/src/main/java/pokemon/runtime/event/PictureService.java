package pokemon.runtime.event;

import com.badlogic.gdx.utils.IntMap;

/**
 * Show / Move / Erase Picture state (project3 section 18). Headless on
 * purpose: the interpreter writes it, {@code ui.PictureLayer} draws it, and
 * neither knows about the other.
 *
 * <p>Coordinates are RMXP screen pixels with y growing downwards; origin 0 is
 * the top-left corner, origin 1 the centre. Zoom / opacity are the command's
 * percentages and 0..255 values.</p>
 */
public final class PictureService {

    /** One picture on screen. */
    public static final class Picture {
        public final int id;
        public String name;
        public int origin;
        public float x;
        public float y;
        public float zoomX = 100f;
        public float zoomY = 100f;
        public float opacity = 255f;
        public int blendType;

        private float fromX, fromY, fromZoomX, fromZoomY, fromOpacity;
        private float toX, toY, toZoomX, toZoomY, toOpacity;
        private int toOrigin, toBlend;
        private float moveLeft, moveTotal;
        private boolean moving;

        Picture(int id) {
            this.id = id;
        }

        /** True while a Move Picture command is still interpolating. */
        public boolean moving() {
            return moving;
        }

        void startMove(int durationFrames, int origin, float x, float y,
                       float zoomX, float zoomY, float opacity, int blendType) {
            fromX = this.x;
            fromY = this.y;
            fromZoomX = this.zoomX;
            fromZoomY = this.zoomY;
            fromOpacity = this.opacity;
            toX = x;
            toY = y;
            toZoomX = zoomX;
            toZoomY = zoomY;
            toOpacity = opacity;
            toOrigin = origin;
            toBlend = blendType;
            moveTotal = Math.max(0f, durationFrames / 20f);
            moveLeft = moveTotal;
            moving = moveTotal > 0f;
            if (!moving) {
                land();
            }
        }

        void advance(float delta) {
            if (!moving) {
                return;
            }
            moveLeft -= Math.max(0f, delta);
            float t = 1f - Math.max(0f, moveLeft) / moveTotal;
            x = lerp(fromX, toX, t);
            y = lerp(fromY, toY, t);
            zoomX = lerp(fromZoomX, toZoomX, t);
            zoomY = lerp(fromZoomY, toZoomY, t);
            opacity = lerp(fromOpacity, toOpacity, t);
            if (moveLeft <= 0f) {
                moving = false;
                land();
            }
        }

        private void land() {
            x = toX;
            y = toY;
            zoomX = toZoomX;
            zoomY = toZoomY;
            opacity = toOpacity;
            origin = toOrigin;
            blendType = toBlend;
        }

        private static float lerp(float from, float to, float t) {
            return from + (to - from) * Math.max(0f, Math.min(1f, t));
        }
    }

    private final IntMap<Picture> pictures = new IntMap<>();

    /** Show Picture (231). */
    public Picture show(int id, String name, int origin, float x, float y,
                        float zoomX, float zoomY, float opacity, int blendType) {
        Picture picture = new Picture(id);
        picture.name = name;
        picture.origin = origin;
        picture.x = x;
        picture.y = y;
        picture.zoomX = zoomX;
        picture.zoomY = zoomY;
        picture.opacity = opacity;
        picture.blendType = blendType;
        pictures.put(id, picture);
        return picture;
    }

    /** Move Picture (232); a missing picture is ignored, like RMXP. */
    public void move(int id, int durationFrames, int origin, float x, float y,
                     float zoomX, float zoomY, float opacity, int blendType) {
        Picture picture = pictures.get(id);
        if (picture != null) {
            picture.startMove(durationFrames, origin, x, y, zoomX, zoomY, opacity, blendType);
        }
    }

    /** Erase Picture (235). */
    public void erase(int id) {
        pictures.remove(id);
    }

    public Picture get(int id) {
        return pictures.get(id);
    }

    /** Live pictures; iterating this does not allocate per frame. */
    public IntMap.Values<Picture> values() {
        return pictures.values();
    }

    public int size() {
        return pictures.size;
    }

    public boolean isEmpty() {
        return pictures.size == 0;
    }

    /** Called once per frame; advances Move Picture interpolation. */
    public void update(float delta) {
        if (pictures.size == 0) {
            return;
        }
        for (Picture picture : pictures.values()) {
            picture.advance(delta);
        }
    }

    public void clear() {
        pictures.clear();
    }
}
