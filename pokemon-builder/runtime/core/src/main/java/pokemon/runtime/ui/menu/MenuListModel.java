package pokemon.runtime.ui.menu;

/** Headless cursor/scroll state shared by the P4 list/detail screens. */
public final class MenuListModel {
    private int size;
    private int index;
    private int first;
    private final int visible;

    public MenuListModel(int visible) { this.visible = Math.max(1, visible); }
    public void size(int value) {
        size = Math.max(0, value);
        index = Math.max(0, Math.min(index, size - 1));
        reveal();
    }
    public void move(int delta) {
        if (size > 0) index = Math.floorMod(index + delta, size);
        reveal();
    }
    public void select(int value) {
        index = Math.max(0, Math.min(value, size - 1));
        reveal();
    }
    private void reveal() {
        first = Math.max(0, Math.min(first, Math.max(0, size - visible)));
        if (index < first) first = index;
        if (index >= first + visible) first = index - visible + 1;
    }
    public int index() { return index; }
    public int first() { return first; }
    public int end() { return Math.min(size, first + visible); }
    public int size() { return size; }
    public int visible() { return visible; }
}
