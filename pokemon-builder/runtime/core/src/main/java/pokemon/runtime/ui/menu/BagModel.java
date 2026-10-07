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

    public BagModel(Inventory inventory, PbsData data) { this.inventory = inventory; this.data = data; refresh(); }

    /** Cycles only the real pockets 1..9 (pbChooseItem's LEFT/RIGHT). */
    public void changePocket(int delta) {
        pocket += delta;
        if (pocket < 1) pocket = POCKETS.length - 1;
        if (pocket >= POCKETS.length) pocket = 1;
        cursor.select(0);
        refresh();
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
        refresh();
    }    public List<String> items() { return items; }
    /** The item under the cursor, or null on the trailing "关闭背包" row. */
    public String selected() { return cursor.index() >= items.size() ? null : items.get(cursor.index()); }
    public boolean onCloseRow() { return cursor.index() >= items.size(); }
    public String name(String id) {
        PbsData.Item item = data == null ? null : data.item(id);
        return item == null || item.name == null ? id : item.name;
    }
    public void refresh() {
        items.clear();
        for (ObjectIntMap.Entry<String> entry : inventory.counts()) {
            PbsData.Item item = data == null ? null : data.item(entry.key);
            int section = item == null || item.pocket < 1 || item.pocket >= POCKETS.length ? 0 : item.pocket;
            if (entry.value > 0 && (!battleOnly || item.battleUse > 0)
                && (chooseFilter != null ? chooseFilter.test(entry.key) : section == pocket)) items.add(entry.key);
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
