package pokemon.runtime.pokemon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 095_PBRibbons ids and 197_PokeBattle_Pokemon:557-615 ribbon methods. */
class RibbonsTest {
    private static Pokemon pokemon() {
        return new Pokemon(null, 5, null);
    }

    @Test
    void constantsAreIds() {
        assertEquals(1, Ribbons.idOf("HOENNCOOL"));
        assertEquals(49, Ribbons.idOf("CHAMPION"));
        assertEquals(80, Ribbons.idOf("worldchampion"));
        assertEquals(66, Ribbons.idOf("66"));
        assertEquals(0, Ribbons.idOf("NOSUCH"));
        assertEquals("冠军缎带", Ribbons.name(49));
    }

    @Test
    void giveHasTake() {
        Pokemon p = pokemon();
        p.giveRibbon("CHAMPION");
        p.giveRibbon("CHAMPION");
        p.giveRibbon("NOSUCH");
        assertEquals(1, p.ribbonCount(), "no duplicates, unknown ignored");
        assertTrue(p.hasRibbon("CHAMPION"));
        assertTrue(p.hasRibbon("49"));
        assertFalse(p.hasRibbon("EFFORT"));
        p.takeRibbon("CHAMPION");
        assertEquals(0, p.ribbonCount());
    }

    @Test
    void upgradeWalksTheChain() {
        Pokemon p = pokemon();
        assertEquals(Ribbons.idOf("HOENNCOOL"), p.upgradeRibbon("HOENNCOOL", "HOENNCOOLSUPER", "HOENNCOOLHYPER"), "none yet: the first");
        assertEquals(Ribbons.idOf("HOENNCOOLSUPER"), p.upgradeRibbon("HOENNCOOL", "HOENNCOOLSUPER", "HOENNCOOLHYPER"));
        assertEquals(Ribbons.idOf("HOENNCOOLHYPER"), p.upgradeRibbon("HOENNCOOL", "HOENNCOOLSUPER", "HOENNCOOLHYPER"));
        assertEquals(0, p.upgradeRibbon("HOENNCOOL", "HOENNCOOLSUPER", "HOENNCOOLHYPER"), "already at the end");
        assertEquals(1, p.ribbonCount());
        p.clearAllRibbons();
        assertEquals(0, p.ribbonCount());
    }
}
