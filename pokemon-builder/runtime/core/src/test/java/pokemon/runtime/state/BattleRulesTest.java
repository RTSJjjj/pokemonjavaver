package pokemon.runtime.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PField_Battles:27-55: {@code recordBattleRule} - the rule strings the
 * interpreter writes into {@code $PokemonTemp.battleRules}.
 */
class BattleRulesTest {

    @Test
    @DisplayName("records the plugin's rule strings and their values")
    void recordsPluginRules() {
        BattleRules rules = new BattleRules();
        assertTrue(rules.record("double", null));
        assertEquals("double", rules.size);
        assertTrue(rules.record("canLose", null));
        assertEquals(Boolean.TRUE, rules.canLose);
        assertTrue(rules.record("cannotrun", null));
        assertEquals(Boolean.FALSE, rules.canRun);
        assertTrue(rules.record("noexp", null));
        assertEquals(Boolean.FALSE, rules.expGain);
        assertTrue(rules.record("outcomevar", 7));
        assertEquals(Integer.valueOf(7), rules.outcomeVar);
        assertTrue(rules.record("backdrop", "field"));
        assertEquals("field", rules.backdrop);
        assertFalse(rules.record("bogus", null), "unknown rules are reported by the caller");
    }

    @Test
    @DisplayName("clear resets every recorded rule (PField_Battles:23-25)")
    void clearResets() {
        BattleRules rules = new BattleRules();
        rules.record("double", null);
        rules.record("canLose", null);
        rules.record("outcomevar", 3);
        rules.clear();
        assertNull(rules.size);
        assertNull(rules.canLose);
        assertNull(rules.outcomeVar);
    }
}
