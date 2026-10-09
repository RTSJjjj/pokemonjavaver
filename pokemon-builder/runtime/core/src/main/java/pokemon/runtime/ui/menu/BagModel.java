package pokemon.runtime.ui.menu;

import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.state.Inventory;
import com.badlogic.gdx.utils.ObjectIntMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pockets from the source project's {@code Settings#pbPocketNames}: index 0 is
 * blank and the real pockets are 1..9, exactly like {@code PokemonBag.numPockets}
 * (the runtime used to invent a 0/"其它" pocket - the original has none).
 */
public final class BagModel {
    public static final String[] POCKETS = {"", "道具", "回复道具", "精灵球", "招式学习机",
            "树果", "超级石", "对战道具", "重要道具", "特殊道具"};
    private final Inventory inventory;
    private final PbsData data;
    private int pocket = 1;
    /** pbItemMenu: while a battle is running only battle-usable items are listed. */
    private boolean battleOnly;
    /** pbChooseItemScreen: when set, only matching items are listed (any pocket). */
    private java.util.function.Predicate<String> chooseFilter;
    public final MenuListModel cursor = new MenuListModel(9);
    private final List<String> items = new ArrayList<>();

    /** {@code PokemonBag#lastpocket / @choices}: the field bag's own, or the battle's (Scene_Commands:239-243). */
    private pokemon.runtime.state.BagMemory memory;

    public BagModel(Inventory inventory, PbsData data) {
        this.inventory = inventory;
        this.data = data;
        this.memory = inventory.bagMemory();
        reload();
    }

    /** Switches to another memory (the battle bag's) and opens on it. */
    public void useMemory(pokemon.runtime.state.BagMemory value) {
        memory = value;
        reload();
    }

    /**
     * Pbstartscene's start position (305_BW_Bag:160-197): the last pocket and
     * {@code getChoice(pocket)}; a battle bag whose last pocket lists no usable
     * item moves to the first pocket that does (:180-190).
     */
    public void reload() {
        pocket = Math.max(1, Math.min(POCKETS.length - 1, memory.lastPocket));
        refresh();
        if (battleOnly && items.isEmpty()) {
            for (int i = 1; i < POCKETS.length; i++) {
                pocket = i;
                refresh();
                if (!items.isEmpty()) break;
            }
        }
        memory.lastPocket = pocket;                            // :192 @bag.lastpocket = lastpocket
        cursor.select(memory.choice(pocket));                  // :197 @bag.getChoice(lastpocket)
    }

    /** {@code @bag.setChoice(pocket, index)} (:429): called whenever the cursor may have moved. */
    public void remember() {
        memory.choice(pocket, cursor.index());
    }

    /** Cycles only the real pockets 1..9 (pbChooseItem's LEFT/RIGHT). */
    public void changePocket(int delta) {
        int start = pocket;
        do {
            pocket += delta;
            if (pocket < 1) pocket = POCKETS.length - 1;
            if (pocket >= POCKETS.length) pocket = 1;
            // 305_BW_Bag:446-455 while choosing with a filter, pockets without a matching item are skipped
        } while (chooseFilter != null && pocket != start && filteredCount(pocket) == 0);
        memory.lastPocket = pocket;                            // :459 @bag.lastpocket = itemwindow.pocket
        refresh();
        cursor.select(memory.choice(pocket));                  // :33 self.index = @bag.getChoice(@pocket)
    }

    /** The items of {@code pocketNumber} the choose filter accepts (305_BW_Bag:393-401 {@code @filterlist[i].length}). */
    private int filteredCount(int pocketNumber) {
        int count = 0;
        for (ObjectIntMap.Entry<String> entry : inventory.counts()) {
            PbsData.Item item = data == null ? null : data.item(entry.key);
            int section = item == null || item.pocket < 1 || item.pocket >= POCKETS.length ? 0 : item.pocket;
            if (entry.value > 0 && section == pocketNumber && chooseFilter.test(entry.key)) count++;
        }
        return count;
    }
    public int pocket() { return pocket; }

    /** PBattle: PokemonBag_Scene's battle mode filter (ITEM_BATTLE_USE > 0). */
    public void battleOnly(boolean value) {
        battleOnly = value;
        refresh();
    }

    /**
     * {@code pbChooseItemScreen}: only items the caller accepts are listed, and
     * the pocket grouping is bypassed - the whole bag is searched
     * (PField_BerryPlants:350-354).
     */
    public void chooseFilter(java.util.function.Predicate<String> value) {
        chooseFilter = value;
        if (value != null && filteredCount(pocket) == 0) {
            for (int i = 1; i < POCKETS.length; i++) {          // 305_BW_Bag:181-191 the first pocket that has a matching item
                if (filteredCount(i) > 0) {
                    pocket = i;
                    break;
                }
            }
        }
        cursor.select(0);
        refresh();
    }    public List<String> items() { return items; }
    /** The item under the cursor, or null on the trailing "关闭背包" row. */
    public String selected() { return cursor.index() >= items.size() ? null : items.get(cursor.index()); }
    public boolean onCloseRow() { return cursor.index() >= items.size(); }
    /** {@code @adapter.getDisplayName(item)} (230_PScreen_Mart:21-28): a TM / HM / TR shows its move after its name. */
    public String name(String id) {
        PbsData.Item item = data == null ? null : data.item(id);
        String name = item == null || item.name == null ? id : item.name;
        if (item != null && (item.fieldUse == 3 || item.fieldUse == 4 || item.fieldUse == 6) && item.machine != null) {
            PbsData.Move move = data.move(item.machine);
            name = name + " " + (move == null ? item.machine : move.name);
        }
        return name;
    }

    /** The item's own name, without a machine's move. */
    public String plainName(String id) {
        PbsData.Item item = data == null ? null : data.item(id);
        return item == null || item.name == null ? id : item.name;
    }
    public void refresh() {
        items.clear();
        for (ObjectIntMap.Entry<String> entry : inventory.counts()) {
            PbsData.Item item = data == null ? null : data.item(entry.key);
            int section = item == null || item.pocket < 1 || item.pocket >= POCKETS.length ? 0 : item.pocket;
            if (entry.value > 0 && (!battleOnly || item.battleUse > 0)
                && section == pocket && (chooseFilter == null || chooseFilter.test(entry.key))) items.add(entry.key);
        }
        items.sort(Comparator.comparingInt((String id) -> data == null || data.item(id) == null ? Integer.MAX_VALUE : data.item(id).id)
                .thenComparing(id -> id));
        // One extra row for "关闭背包" (004/BW Bag's drawItem adds it).
        cursor.size(items.size() + 1);
    }
    public List<String> rows() {
        List<String> result = new ArrayList<>();
        for (String id : items) result.add(name(id) + " ×" + inventory.count(id));
        result.add("关闭背包");
        return result;
    }
}
