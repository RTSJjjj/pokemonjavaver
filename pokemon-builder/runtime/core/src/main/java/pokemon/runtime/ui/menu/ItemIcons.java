package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Texture;
import pokemon.runtime.pokemon.ItemUse;
import pokemon.runtime.pokemon.PbsData;

import java.util.function.Predicate;

/** 258_PSystem_FileUtilities:273-302 {@code pbItemIconFile}: the icon file of an item, TM/HM/TR by the move's type. */
public final class ItemIcons {
    private ItemIcons() { }

    static Texture of(MenuAssets a, PbsData pbs, String id) {
        if (id == null || id.isEmpty()) return null;
        return a.itemIcon(id, () -> {
            String name = name(pbs, id, candidate -> a.icon(candidate) != null);
            return name == null ? null : a.icon(name);
        });
    }

    /** The icon file (Graphics/Icons, without extension) of the item; {@code exists} is {@code pbResolveBitmap}. */
    public static String name(PbsData pbs, String id, Predicate<String> exists) {
        if (id == null || id.isEmpty()) return null;
        PbsData.Item item = pbs == null ? null : pbs.item(id);
        if (exists.test("item" + id)) return "item" + id;                          // :279 item<CONSTANT>
        if (item != null && exists.test(String.format("item%03d", item.id))) {      // :281
            return String.format("item%03d", item.id);
        }
        if (item != null) {
            String type = machineType(pbs, item);
            if (type != null && item.fieldUse == 6 && exists.test("itemRecord" + type)) {   // :282-289 pbIsTechnicalRecord?
                return "itemRecord" + type;
            }
            if (type != null && isMachine(item) && exists.test("itemMachine" + type)) {    // :290-297
                return "itemMachine" + type;
            }
        }
        return exists.test("item000") ? "item000" : null;                          // :298
    }

    /** {@code pbIsMachine?} (188_PItem_Items:69-72): TM, HM or TR. */
    static boolean isMachine(PbsData.Item item) {
        return item.fieldUse == 3 || item.fieldUse == 4 || item.fieldUse == 6;
    }

    /** {@code pbGetMoveData(pbGetMachine(item), MOVE_TYPE)}: the internal name of the machine move's type. */
    private static String machineType(PbsData pbs, PbsData.Item item) {
        if (!isMachine(item)) return null;
        PbsData.Move move = item.machine != null && !item.machine.isEmpty() ? pbs.move(item.machine) : ItemUse.machineMove(item, pbs);
        return move == null ? null : move.type;
    }
}
