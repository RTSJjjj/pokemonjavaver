package pokemon.runtime.ui.menu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.TrainerState;
import pokemon.runtime.state.Inventory;
import pokemon.runtime.state.MartPrices;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 230_PScreen_Mart: the shop's price, stock and money rules (no drawing). */
class MartRulesTest {

    @TempDir
    Path tempDir;

    private MartRules rules;
    private TrainerState trainer;
    private Inventory bag;
    private MartPrices prices;

    private static String item(int id, String name, int price, int type, int fieldUse, String machine) {
        return "\"" + name + "\":{\"id\":" + id + ",\"internalName\":\"" + name + "\",\"name\":\"" + name
                + "\",\"pocket\":1,\"price\":" + price + ",\"description\":\"d\",\"fieldUse\":" + fieldUse
                + ",\"battleUse\":0,\"type\":" + type + ",\"machine\":" + (machine == null ? "null" : "\"" + machine + "\"")
                + ",\"extra\":[]}";
    }

    @BeforeEach
    void setUp() throws Exception {
        Path pbsDir = tempDir.resolve("pbs");
        Files.createDirectories(pbsDir);
        Files.write(pbsDir.resolve("items.json"), ("{\"items\":{"
                + item(1, "POTION", 300, 0, 2, null) + ","
                + item(2, "POKEBALL", 200, 3, 0, null) + ","
                + item(3, "PREMIERBALL", 200, 3, 0, null) + ","
                + item(4, "BICYCLE", 0, 6, 2, null) + ","
                + item(5, "TM01", 3000, 0, 3, "TACKLE") + ","
                + item(6, "HM01", 0, 0, 4, "CUT") + ","
                + item(7, "NUGGET", 10000, 0, 0, null)
                + "}}").getBytes(StandardCharsets.UTF_8));
        Files.write(pbsDir.resolve("moves.json"), ("{\"moves\":{\"TACKLE\":{\"id\":1,\"internalName\":\"TACKLE\","
                + "\"name\":\"撞击\",\"function\":\"000\",\"power\":40,\"type\":\"NORMAL\",\"category\":\"Physical\","
                + "\"accuracy\":100,\"pp\":35,\"effectChance\":0,\"target\":\"NearOther\",\"priority\":0,\"flags\":\"\"}}}")
                .getBytes(StandardCharsets.UTF_8));
        PbsData pbs = PbsData.parse(tempDir.toFile());
        trainer = new TrainerState();
        bag = new Inventory();
        prices = new MartPrices();
        rules = new MartRules(pbs, trainer, bag, prices);
    }

    @Test
    @DisplayName("the stock drops unknown items and key items the player already owns (:808-815)")
    void stockFilter() {
        bag.add("BICYCLE", 1);
        List<String> stock = rules.filterStock(Arrays.asList("POTION", "BICYCLE", "NOSUCHITEM", "HM01"));
        assertEquals(Arrays.asList("POTION", "HM01"), stock);
    }

    @Test
    @DisplayName("key items, HMs and TMs are important; ordinary items are not (pbIsImportantItem?)")
    void importantItems() {
        assertTrue(rules.isImportant("BICYCLE"));
        assertTrue(rules.isImportant("HM01"));
        assertTrue(rules.isImportant("TM01"), "INFINITE_TMS is true in this project");
        assertFalse(rules.isImportant("POTION"));
    }

    @Test
    @DisplayName("a setPrice override replaces the buy price; the PBS price is the fallback (:36-46)")
    void priceOverrides() {
        assertEquals(300, rules.price("POTION", false));
        prices.setPrice("POTION", 500, -1);
        assertEquals(500, rules.price("POTION", false));
        assertEquals(500, rules.price("POTION", true), "no sell price given: the buy price is reused");
        prices.setPrice("NUGGET", 80000, 0);
        assertEquals(80000, rules.price("NUGGET", false));
        assertEquals(0, rules.price("NUGGET", true), "sell price 0 = cannot sell");
        assertFalse(rules.canSell("NUGGET"));
        prices.setPrice("POKEBALL", -1, 150);
        assertEquals(200, rules.price("POKEBALL", false));
        assertEquals(300, rules.price("POKEBALL", true), "a sell price is stored doubled, the screen halves it again");
        prices.clear();
        assertEquals(300, rules.price("POTION", false), "clear_mart_prices restores the PBS prices");
    }

    @Test
    @DisplayName("machines show their move in the name (:13-20)")
    void machineNames() {
        assertEquals("TM01 撞击", rules.displayName("TM01"));
        assertEquals("POTION", rules.displayName("POTION"));
    }

    @Test
    @DisplayName("how many the player can afford is capped at one bag slot (:720-721)")
    void maxAfford() {
        trainer.money = 1000;
        assertEquals(3, rules.maxAfford(300));
        trainer.money = 999_999_999;
        assertEquals(999, rules.maxAfford(10));
        assertEquals(999, rules.maxAfford(0), "a free item is capped, not infinite");
    }

    @Test
    @DisplayName("buying stores the items and takes the money; a key item then leaves the stock (:736-756)")
    void buying() {
        trainer.money = 5000;
        rules.buy("POTION", 3, 900);
        assertEquals(3, bag.count("POTION"));
        assertEquals(4100, trainer.money);

        List<String> stock = new ArrayList<>(Arrays.asList("POTION", "HM01"));
        rules.buy("HM01", 1, 0);
        rules.dropOwnedKeyItems(stock);
        assertEquals(Arrays.asList("POTION"), stock);
    }

    @Test
    @DisplayName("ten Poke Balls earn one Premier Ball, twenty earn two, other items none (:760-768)")
    void premierBalls() {
        assertEquals(0, rules.premierBonus("POKEBALL", 9));
        assertEquals(1, rules.premierBonus("POKEBALL", 10));
        assertEquals(2, rules.premierBonus("POKEBALL", 25));
        assertEquals(3, bag.count("PREMIERBALL"));
        assertEquals(0, rules.premierBonus("POTION", 50));
    }

    @Test
    @DisplayName("selling pays half the price per item and removes them (:792-798)")
    void selling() {
        bag.add("POTION", 5);
        trainer.money = 100;
        int total = MartRules.sellTotal(rules.price("POTION", true), 3);
        assertEquals(450, total, "300 / 2 * 3");
        rules.sell("POTION", 3, total);
        assertEquals(2, bag.count("POTION"));
        assertEquals(550, trainer.money);
        assertTrue(rules.canSell("POTION"));
        assertFalse(rules.canSell("BICYCLE"), "key items cannot be sold");
    }

    @Test
    @DisplayName("the money never leaves 0 .. MAX_MONEY")
    void moneyIsClamped() {
        trainer.money = 100;
        rules.setMoney(-5);
        assertEquals(0, trainer.money);
        rules.setMoney(Integer.MAX_VALUE);
        assertEquals(999_999_999, trainer.money);
    }
}
