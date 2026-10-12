package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** The battler follows the Pokemon's new form on Mega Evolution (Mega_evolution:152 {@code battler.pbUpdate(true)}). */
class MegaBattlerRealDataTest {

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
    @DisplayName("a Mega Evolved battler has the Mega form's ability and types")
    void megaBattlerTakesTheFormAbility() {
        PbsData data = data();
        Pokemon blaziken = new Pokemon(data.species("BLAZIKEN"), 50, data);
        blaziken.item = "BLAZIKENITER";
        blaziken.setAbilitySlot(0);
        Pokemon foe = new Pokemon(data.species("BIDOOF"), 5, data);
        Battle b = new Battle(data, new Random(1), (user, target, moves) -> 0).addPlayer(blaziken).addFoe(foe);
        b.bagHasItem = id -> "MEGARING".equals(id);
        assertEquals("BLAZE", b.player().ability);
        b.pbMegaEvolve(0);
        assertEquals("RAGEFIREBLAST", b.player().ability, "the Mega form's ability, not the one it came in with");
        assertEquals("RAGEFIREBLAST", blaziken.ability);
    }
}
