package pokemon.runtime.ui.menu;

import pokemon.runtime.pokemon.*;
import pokemon.runtime.state.Inventory;
import java.util.ArrayList;
import java.util.List;

/** Headless party browsing, swapping and held-item transactions. */
public final class PartyModel {
    private final Party party;
    private final PbsData data;
    public final MenuListModel cursor = new MenuListModel(6);
    private int swapFrom = -1;
    private int page;
    public PartyModel(Party party, PbsData data) { this.party = party; this.data = data; refresh(); }
    public void refresh() { cursor.size(party.size()); }
    public Pokemon selected() { return party.get(cursor.index()); }
    public boolean swapping() { return swapFrom >= 0; }
    public void swap() {
        if (selected() == null) return;
        if (swapFrom < 0) swapFrom = cursor.index();
        else { party.swap(swapFrom, cursor.index()); swapFrom = -1; }
    }
    public void cancelSwap() { swapFrom = -1; }
    public void page(int delta) { page = Math.floorMod(page + delta, 4); }
    public int page() { return page; }
    public boolean takeItem(Inventory inventory) {
        Pokemon p = selected();
        if (p == null || p.item == null || p.item.isEmpty()) return false;
        inventory.add(p.item, 1); p.item = null; return true;
    }
    public static boolean giveItem(Pokemon p, String id, Inventory inventory, PbsData data) {
        PbsData.Item item = data == null ? null : data.item(id);
        if (p == null || p.egg || item == null || item.pocket == 8 || item.pocket == 4 || !inventory.has(id)) return false;
        inventory.remove(id, 1);
        if (p.item != null && !p.item.isEmpty()) inventory.add(p.item, 1);
        p.item = id;
        return true;
    }
    public List<String> rows() {
        List<String> rows = new ArrayList<>();
        for (Pokemon p : party.members()) rows.add(p.egg ? "神秘的蛋" : p.name + " Lv." + p.level);
        return rows;
    }
    public List<String> details() {
        List<String> lines = new ArrayList<>();
        Pokemon p = selected();
        if (p == null) { lines.add("队伍中没有宝可梦。"); return lines; }
        if (p.egg) { lines.add("神秘的蛋"); lines.add("距离孵化：" + p.stepsToHatch + " 步"); return lines; }
        lines.add(p.name + "  Lv." + p.level + (p.shiny ? " ★" : ""));
        if (page == 0) {
            lines.add("HP " + p.hp + " / " + p.maxHp() + "  " + (p.fainted() ? "濒死" : blank(p.status, "正常")));
            lines.add("性别：" + (p.effectiveGender() == PokemonStats.MALE ? "雄性" : p.effectiveGender() == PokemonStats.FEMALE ? "雌性" : "无性别"));
            lines.add("性格：" + (p.nature == null ? "—" : p.nature.name));
            PbsData.Ability ability = data == null ? null : data.ability(p.ability);
            lines.add("特性：" + (ability == null ? blank(p.ability, "—") : ability.name));
            lines.add("携带：" + itemName(p.item));
            lines.add("经验 " + p.exp + " / 距升级 " + p.experienceToNextLevel());
            lines.add("亲密度 " + p.happiness);
        } else if (page == 1) {
            lines.add("能力          IV / EV");
            String[] names = {"HP", "攻击", "防御", "速度", "特攻", "特防"};
            for (int i = 0; i < 6; i++) lines.add(names[i] + " " + p.stat(i) + "    " + p.ivs[i] + " / " + p.evs[i]);
        } else if (page == 2) {
            for (Pokemon.MoveSlot slot : p.moves) {
                if (slot.move == null) continue;
                lines.add(slot.move.name + "  PP " + slot.pp + "/" + slot.maxPp);
                lines.add("威力 " + slot.move.power + "  命中 " + slot.move.accuracy);
            }
            if (p.moves.size == 0) lines.add("没有招式");
        } else {
            lines.add("初训家：" + blank(p.originalTrainer, "—"));
            lines.add("缎带：" + p.ribbons.size);
            for (String ribbon : p.ribbons) lines.add(ribbon);
        }
        return lines;
    }
    private String itemName(String id) {
        PbsData.Item item = data == null ? null : data.item(id);
        return item == null ? blank(id, "无") : item.name;
    }
    private static String blank(String s, String fallback) { return s == null || s.isEmpty() ? fallback : s; }
}
