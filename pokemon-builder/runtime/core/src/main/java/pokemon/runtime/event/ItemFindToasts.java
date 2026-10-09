package pokemon.runtime.event;

import java.util.ArrayList;
import java.util.List;

/**
 * 308_ItemFindSimple_Scene:1-87: the small box at the right edge that shows a picked-up item (icon, name, "x qty")
 * for about three seconds. Each new box sits 28px above the previous ones; creating one first removes the boxes
 * older than a second ({@code disposeTempItem(1)}).
 */
public final class ItemFindToasts {
    /** :6-7 width 184, height 28. */
    public static final int WIDTH = 184, HEIGHT = 28;
    /** :78-88 disposeTempItem(3) from Sprite_Character#update. */
    private static final float LIFETIME = 3f;

    public static final class Toast {
        public final String item;
        public final String name;
        public final int qty;
        /** :11 {@code @y = 192 - count * @height}, in RGSS screen coordinates (y down). */
        public final int y;
        /** "×qty", built once (drawn every frame). */
        public final String qtyText;
        float age;

        Toast(String item, String name, int qty, int y) {
            this.item = item;
            this.name = name;
            this.qty = qty;
            this.y = y;
            this.qtyText = "×" + qty;
        }
    }

    private final List<Toast> toasts = new ArrayList<>();

    public List<Toast> list() {
        return toasts;
    }

    /** {@code ItemFindSimple_Scene.new(item, qty)}. */
    public void add(String item, String name, int qty) {
        dispose(1f);
        toasts.add(new Toast(item, name, qty, 192 - toasts.size() * HEIGHT));
    }

    public void update(float delta) {
        if (toasts.isEmpty()) {
            return;
        }
        for (Toast toast : toasts) {
            toast.age += Math.max(0f, delta);
        }
        dispose(LIFETIME);
    }

    /** :78-88: the oldest boxes go first; the scan stops at the first one that is still young. */
    private void dispose(float seconds) {
        while (!toasts.isEmpty() && toasts.get(0).age >= seconds) {
            toasts.remove(0);
        }
    }

    public void clear() {
        toasts.clear();
    }
}
