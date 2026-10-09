package pokemon.runtime.ui.menu;

import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.TrainerState;
import pokemon.runtime.state.Inventory;
import pokemon.runtime.state.MartPrices;

import java.util.ArrayList;
import java.util.List;

/**
 * The shop rules of 230_PScreen_Mart without any drawing: {@code PokemonMartAdapter} (:4-82),
 * {@code pbPokemonMart}'s stock filter (:807-815) and the money / bag changes of
 * {@code pbBuyScreen} (:702-768) and {@code pbSellScreen} (:771-805). {@link MartView} owns the
 * dialogs and calls these at the points the plugin does, which keeps the rules unit-testable.
 */
final class MartRules {
    /** 000_Settings:188 BAG_MAX_PER_SLOT. */
    static final int BAG_MAX_PER_SLOT = 999;
    /** 000_Settings:70 INFINITE_TMS. */
    private static final boolean INFINITE_TMS = true;
    /** 000_Settings:96 MAX_MONEY. */
    private static final int MAX_MONEY = 999_999_999;

    private final PbsData pbs;
    private final TrainerState trainer;
    private final Inventory bag;
    private final MartPrices prices;

    MartRules(PbsData pbs, TrainerState trainer, Inventory bag, MartPrices prices) {
        this.pbs = pbs;
        this.trainer = trainer;
        this.bag = bag;
        this.prices = prices;
    }

    /**
     * :808-815 {@code stock[i] = getID(PBItems,stock[i]); nil if unknown / 0 / (important &amp;&amp; owned)}
     * followed by {@code stock.compact!}.
     */
    List<String> filterStock(List<String> items) {
        List<String> stock = new ArrayList<>();
        for (String name : items) {
            PbsData.Item data = pbs == null || name == null ? null : pbs.item(name);
            if (data == null || data.id == 0 || (isImportant(data) && bag.has(name))) {
                continue;
            }
            stock.add(name);
        }
        return stock;
    }

    /** 188_PItem_Items:140-147 pbIsImportantItem?: key items, HMs and (INFINITE_TMS) TMs. */
    static boolean isImportant(PbsData.Item data) {
        if (data == null) {
            return false;
        }
        return data.type == 6 || data.fieldUse == 4 || (data.fieldUse == 3 && INFINITE_TMS);
    }

    boolean isImportant(String item) {
        return isImportant(pbs.item(item));
    }

    /** 188_PItem_Items:69-72 pbIsMachine?: field use 3 (TM), 4 (HM) or 6 (TR). */
    static boolean isMachine(PbsData.Item data) {
        return data.fieldUse == 3 || data.fieldUse == 4 || data.fieldUse == 6;
    }

    int money() {
        return trainer.money;                                      // :6 getMoney
    }

    void setMoney(int value) {
        trainer.money = Math.max(0, Math.min(MAX_MONEY, value));   // :10 setMoney + PokeBattle_Trainer#money=
    }

    /** :36-46 getPrice(item, selling): the event's setPrice overrides, then the PBS price. */
    int price(String item, boolean selling) {
        int[] override = prices.get(item);
        if (override != null) {
            if (selling) {
                if (override[1] >= 0) {
                    return override[1];                            // :40
                }
            } else if (override[0] > 0) {
                return override[0];                                // :42
            }
        }
        PbsData.Item data = pbs.item(item);
        return data == null ? 0 : data.price;                      // :46 pbGetPrice
    }

    /** :13-20 getDisplayName: machines show their move. */
    String displayName(String item) {
        PbsData.Item data = pbs.item(item);
        if (data == null) {
            return item;
        }
        String itemname = data.name == null ? item : data.name;
        if (isMachine(data) && data.machine != null) {
            PbsData.Move move = pbs.move(data.machine);
            itemname = itemname + " " + (move == null ? data.machine : move.name);   // :18 _INTL("{1} {2}")
        }
        return itemname;
    }

    /** :71-73 canSell?: a positive sell price and not an important item. */
    boolean canSell(String item) {
        return price(item, true) > 0 && !isImportant(item);
    }

    /** :720-721 how many the player can afford, capped at one bag slot. */
    int maxAfford(int price) {
        int maxafford = price <= 0 ? BAG_MAX_PER_SLOT : money() / price;
        return Math.min(maxafford, BAG_MAX_PER_SLOT);
    }

    /**
     * :736-761 the purchase: store {@code quantity} items (one at a time, like {@code addItem}),
     * then take the money. {@code pbStoreItem(item,1)} never fails here because a full slot opens a
     * new one (BAG_MAX_POCKET_SIZE is unlimited), so the "no room" branch (:743-749) cannot occur.
     */
    void buy(String item, int quantity, int totalPrice) {
        for (int i = 0; i < quantity; i++) {
            bag.add(item, 1);                                      // :738
        }
        setMoney(money() - totalPrice);                            // :751
    }

    /**
     * :752-756 after a purchase a key item the player now owns leaves the stock
     * ({@code @stock[i]=nil; @stock.compact!}).
     */
    void dropOwnedKeyItems(List<String> stock) {
        for (int i = stock.size() - 1; i >= 0; i--) {
            PbsData.Item data = pbs.item(stock.get(i));
            if (data != null && isImportant(data) && bag.has(stock.get(i))) {
                stock.remove(i);
            }
        }
    }

    /**
     * :760-768 one Premier Ball for every ten Poke Balls bought, when the project has the item.
     *
     * @return how many were handed over
     */
    int premierBonus(String item, int quantity) {
        int premiers = quantity / 10;                              // :760
        PbsData.Item data = pbs.item(item);
        if (premiers > 0 && data != null && data.isPokeBall() && pbs.item("PREMIERBALL") != null) {   // :761
            for (int i = 0; i < premiers; i++) {
                bag.add("PREMIERBALL", 1);                         // :763
            }
            return premiers;
        }
        return 0;
    }

    /** :792-793 {@code price/=2; price*=qty} from the full {@link #price} of one item. */
    static int sellTotal(int fullPrice, int quantity) {
        return fullPrice / 2 * quantity;
    }

    /** :795-798 */
    void sell(String item, int quantity, int total) {
        setMoney(money() + total);
        for (int i = 0; i < quantity; i++) {
            bag.remove(item, 1);
        }
    }
}
