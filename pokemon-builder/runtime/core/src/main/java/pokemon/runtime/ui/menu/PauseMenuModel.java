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
        , PARTY, BAG, POKEDEX, STORAGE
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
        // Order, labels and icons follow the project's "Modular Menu" plugin
        // (MenuHandlers.addEntry): Pokedex / PC / Pokemon / Bag / Trainer /
        // Save / Load / Quit / Options / Exit. Entries the runtime has no
        // feature for (Habitat / Pokegear / Tasks / Debug) are omitted, and a
        // few are hidden by their availability check, exactly like the plugin:
        // the Pokedex needs $Trainer.pokedex (Modular Menu:67) and the PC needs
        // $Trainer.pokepc (Modular Menu:82).
        if (state != null && state.trainer().pokedex) {
            entries.add(new Entry(Action.POKEDEX, "图鉴", "menuPokedex"));
        }
        if (state != null && state.trainer().pokepc) {
            entries.add(new Entry(Action.STORAGE, "寄存系统", "menuPC"));
        }
        if (state != null && state.trainer().partyCount() > 0) {
            entries.add(new Entry(Action.PARTY, "宝可梦", "menuPokemon"));
        }
        entries.add(new Entry(Action.BAG, "背包", "menuBag"));
        entries.add(new Entry(Action.TRAINER, name, "menuTrainer"));
        entries.add(new Entry(Action.SAVE, "保存", "menuSave"));
        entries.add(new Entry(Action.LOAD, "读档", "menuLoad"));
        entries.add(new Entry(Action.TITLE, "退出", "menuQuit"));
        entries.add(new Entry(Action.OPTIONS, "设置", "menuOptions"));
        entries.add(new Entry(Action.EXIT, "退出游戏", "menuExit"));
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
