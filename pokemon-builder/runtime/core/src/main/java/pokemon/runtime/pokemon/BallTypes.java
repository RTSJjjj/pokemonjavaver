package pokemon.runtime.pokemon;

import com.badlogic.gdx.utils.ObjectIntMap;

/**
 * {@code $BallTypes} (PokeBall_CatchEffects:1-30): the project's ball type
 * index -> item internal name table, plus {@code pbBallTypeToItem} (:32-42) and
 * {@code pbGetBallType} (:44-50).
 *
 * <p>The index is what a Pokemon stores in {@code ballused}
 * (PokeBattle_BattleCommon:152), which is what the summary / PC / battle
 * send-out use to pick the ball graphic.</p>
 */
public final class BallTypes {

    /** Index -> item internal name, exactly the project's table order. */
    private static final String[] BALL_ITEMS = {
        "POKEBALL", "GREATBALL", "SAFARIBALL", "ULTRABALL", "MASTERBALL",
        "NETBALL", "DIVEBALL", "NESTBALL", "REPEATBALL", "TIMERBALL",
        "LUXURYBALL", "PREMIERBALL", "DUSKBALL", "HEALBALL", "QUICKBALL",
        "CHERISHBALL", "FASTBALL", "LEVELBALL", "LUREBALL", "HEAVYBALL",
        "LOVEBALL", "FRIENDBALL", "MOONBALL", "SPORTBALL", "DREAMBALL",
        "BEASTBALL", "PETBALL", "WILDERNESSBALL",
    };

    private static ObjectIntMap<String> indexByItem;

    private BallTypes() {
    }

    public static int count() {
        return BALL_ITEMS.length;
    }

    /** The item internal name of a ball type, or {@code null} past the table. */
    public static String itemName(int ballType) {
        return ballType < 0 || ballType >= BALL_ITEMS.length ? null : BALL_ITEMS[ballType];
    }

    /**
     * {@code pbBallTypeToItem} (:32-42): the ball type's item, falling back to
     * type 0's item and then to POKEBALL when the project has no such item.
     */
    public static PbsData.Item item(PbsData pbs, int ballType) {
        if (pbs == null) {
            return null;
        }
        PbsData.Item item = pbs.item(itemName(ballType));
        if (item != null) {
            return item;
        }
        item = pbs.item(BALL_ITEMS[0]);
        return item != null ? item : pbs.item("POKEBALL");
    }

    /**
     * {@code pbGetBallType} (:44-50): the ball type of an item, or 0 when the
     * item is not one of the table's balls (an unknown ball falls back to the
     * plain Poké Ball type, like the plugin's {@code return 0}).
     */
    public static int ballType(PbsData pbs, String itemId) {
        if (pbs == null || itemId == null) {
            return 0;
        }
        PbsData.Item item = pbs.item(itemId);
        if (item == null) {
            return 0;
        }
        if (indexByItem == null) {
            ObjectIntMap<String> table = new ObjectIntMap<>();
            for (int i = 0; i < BALL_ITEMS.length; i++) {
                table.put(BALL_ITEMS[i], i);
            }
            indexByItem = table;
        }
        // The plugin compares item ids (isConst?(ball,PBItems,$BallTypes[key])),
        // so an item that only shares a name with a ball still matches by id.
        int index = indexByItem.get(item.internalName, -1);
        return index < 0 ? 0 : index;
    }
}
