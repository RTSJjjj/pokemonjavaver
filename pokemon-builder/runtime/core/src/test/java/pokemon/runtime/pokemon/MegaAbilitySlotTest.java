package pokemon.runtime.pokemon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * {@code @abilityflag} (197_PokeBattle_Pokemon:219-245) survives a form change: a Pokemon that Mega Evolves and returns
 * has the ability of the slot it started with, not the first natural ability. Skipped without generated/pbs.
 */
class MegaAbilitySlotTest {

    private static PbsData data() {
        for (File d = new File("").getAbsoluteFile(); d != null; d = d.getParentFile()) {
            for (String candidate : new String[] {"generated", "pokemon-builder/generated"}) {
                File root = new File(d, candidate);
                if (new File(root, "pbs").isDirectory()) return PbsData.parse(root);
            }
        }
        assumeTrue(false, "generated/pbs not found");
        return null;
    }

    @Test
    @DisplayName("the second natural ability comes back after Mega Evolution ends")
    void secondSlotSurvives() {
        PbsData data = data();
        Pokemon gardevoir = new Pokemon(data.species("GARDEVOIR"), 50, data);
        gardevoir.item = "GARDEVOIRITE";
        gardevoir.setAbilitySlot(1);
        assertEquals("TRACE", gardevoir.ability);
        gardevoir.makeMega(data);
        assertEquals("PIXILATE", gardevoir.ability, "the Mega form's own ability");
        gardevoir.makeUnmega(data);
        assertEquals("TRACE", gardevoir.ability, "not SYNCHRONIZE, the first slot");
    }

    @Test
    @DisplayName("the hidden ability comes back after Mega Evolution ends")
    void hiddenSlotSurvives() {
        PbsData data = data();
        Pokemon charizard = new Pokemon(data.species("CHARIZARD"), 50, data);
        charizard.item = "CHARIZARDITEX";
        charizard.setAbilitySlot(2);
        assertEquals("SOLARPOWER", charizard.ability);
        charizard.makeMega(data);
        assertEquals("TOUGHCLAWS", charizard.ability);
        charizard.makeUnmega(data);
        assertEquals("SOLARPOWER", charizard.ability);
    }

    @Test
    @DisplayName("an ability set by hand is not overwritten by a stale slot")
    void manualAbilityWins() {
        PbsData data = data();
        Pokemon gardevoir = new Pokemon(data.species("GARDEVOIR"), 50, data);
        gardevoir.item = "GARDEVOIRITE";
        gardevoir.setAbilitySlot(1);
        gardevoir.ability = "SYNCHRONIZE";
        gardevoir.makeMega(data);
        gardevoir.makeUnmega(data);
        assertEquals("SYNCHRONIZE", gardevoir.ability);
    }
}
