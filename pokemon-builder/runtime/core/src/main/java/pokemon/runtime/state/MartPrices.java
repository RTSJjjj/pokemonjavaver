package pokemon.runtime.state;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code $game_temp.mart_prices} (230_PScreen_Mart:850-860): the per-item price overrides an
 * event sets with {@code setPrice} / {@code setSellPrice} before opening a shop. Each entry is
 * {@code [buyPrice, sellPrice]}, -1 meaning "use the PBS price". It lives only until the shop
 * closes ({@code clear_mart_prices}, :841), so it is not saved.
 */
public final class MartPrices {
    private final Map<String, int[]> prices = new HashMap<>();

    /** {@code mart_prices[item]}, or null when the event set nothing for this item. */
    public int[] get(String item) {
        return prices.get(item);
    }

    /**
     * {@code setPrice(item,buyprice=-1,sellprice=-1)} (230_PScreen_Mart:891-900):
     * a positive buy price replaces the PBS price; a sell price of 0 means "cannot sell",
     * a positive one is stored doubled (the screen halves it when selling), and without one
     * the buy price is reused.
     */
    public void setPrice(String item, int buyPrice, int sellPrice) {
        int[] entry = prices.computeIfAbsent(item, key -> new int[] {-1, -1});   // :893
        if (buyPrice > 0) {                                                       // :894
            entry[0] = buyPrice;
        }
        if (sellPrice >= 0) {                                                     // :895
            entry[1] = sellPrice * 2;                                             // :896
        } else if (buyPrice > 0) {                                                // :897-898
            entry[1] = buyPrice;
        }
    }

    /** {@code setSellPrice(item,sellprice)} (:902-904). */
    public void setSellPrice(String item, int sellPrice) {
        setPrice(item, -1, sellPrice);
    }

    /** {@code clear_mart_prices} (:841). */
    public void clear() {
        prices.clear();
    }
}
