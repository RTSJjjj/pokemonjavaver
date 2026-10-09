package pokemon.runtime.pokemon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** 343_ChainCatching: refreshChain / getChain / pbRandomIV. */
class ChainCatchingTest {
    @Test
    @DisplayName("catching the same species raises the chain; another species restarts it at 1")
    void refresh() {
        ChainCatching chain = new ChainCatching();
        for (int i = 0; i < 10; i++) chain.refresh("PIKACHU");
        assertEquals(10, chain.chainTimes);
        assertEquals(16, chain.shinyRetries);
        assertEquals(1, chain.ivGuaranteed);
        chain.refresh("RATTATA");
        assertEquals(1, chain.chainTimes);
        assertEquals(0, chain.shinyRetries);
    }

    @Test
    @DisplayName("60 and more gives 128 retries and five perfect IVs; a different wild species gets nothing")
    void tiers() {
        ChainCatching chain = new ChainCatching();
        chain.species = "PIKACHU";
        chain.chainTimes = 60;
        chain.get("PIKACHU");
        assertEquals(128, chain.shinyRetries);
        assertEquals(5, chain.ivGuaranteed);
        chain.get("RATTATA");
        assertEquals(0, chain.shinyRetries);
        assertEquals(0, chain.ivGuaranteed);
    }

    @Test
    @DisplayName("pbRandomIV(n) has n perfect IVs; 6 or more is all perfect")
    void randomIvs() {
        int[] ivs = ChainCatching.randomIvs(3, new Random(1));
        int perfect = 0;
        for (int iv : ivs) if (iv == 31) perfect++;
        assertTrue(perfect >= 3);
        for (int iv : ChainCatching.randomIvs(6, new Random(1))) assertEquals(31, iv);
    }
}
