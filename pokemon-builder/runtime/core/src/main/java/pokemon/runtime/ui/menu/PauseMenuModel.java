package pokemon.runtime.ui.menu;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.state.GameState;

/**
 * L1: the pause menu model (Modular Pause Menu layout). Only entries the
 * stage-2 runtime can actually do are listed; the plugin's other entries
 * (Pokedex / Pokemon / Bag / Storage / Habitat / Pokegear / Debug) need the
 * stage-3 domain and stay hidden - exactly like the plugin hides entries whose
 * availability check fails.
 */
public final class PauseMenuModel {

    public enum Action {
        /** Trainer info (name, map, items, quests). */
        TRAINER,
        SAVE,
        LOAD,
        OPTIONS,
        /** Back to the title screen. */
        TITLE,
        /** Quit the application. */
        EXIT
    }

    public static final class Entry {
        public final Action action;
        public final String label;
        public final String icon;

        Entry(Action action, String label, String icon) {
            this.action = action;
            this.label = label;
            this.icon = icon;
        }
    }

    private final Array<Entry> entries = new Array<>();
    private int index;

    public PauseMenuModel(GameState state) {
        String name = state == null || state.playerName() == null || state.playerName().isEmpty()
                ? "训练家"
                : state.playerName();
        entries.add(new Entry(Action.TRAINER, name, "menuTrainer"));
        entries.add(new Entry(Action.SAVE, "保存", "menuSave"));
        entries.add(new Entry(Action.LOAD, "读档", "menuLoad"));
        entries.add(new Entry(Action.OPTIONS, "设置", "menuOptions"));
        entries.add(new Entry(Action.TITLE, "回到标题", "menuQuit"));
        entries.add(new Entry(Action.EXIT, "退出游戏", "menuexit"));
    }

    public int size() {
        return entries.size;
    }

    public int index() {
        return index;
    }

    public void move(int delta) {
        if (entries.size == 0) {
            index = 0;
            return;
        }
        index = Math.floorMod(index + delta, entries.size);
    }

    public void index(int value) {
        index = Math.floorMod(value, Math.max(1, entries.size));
    }

    public Entry selected() {
        return entries.size == 0 ? null : entries.get(index);
    }

    public Action selectedAction() {
        Entry entry = selected();
        return entry == null ? null : entry.action;
    }

    public Entry entryAt(int position) {
        return position >= 0 && position < entries.size ? entries.get(position) : null;
    }
}
