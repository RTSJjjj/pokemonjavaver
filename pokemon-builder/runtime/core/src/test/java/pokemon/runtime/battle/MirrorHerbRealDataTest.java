package pokemon.runtime.battle;

import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Mirror Herb (104_BattleHandlers_Items:1780-1798) copies a stat raise of a move, on the real data. */
class MirrorHerbRealDataTest {

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

    @Test void mirrorHerbCopiesSwordsDance() {
        PbsData d = data();
        Pokemon user = new Pokemon(d.species("GARCHOMP"), 50, d);
        user.moves.clear();
        user.moves.add(new Pokemon.MoveSlot(d.move("SWORDSDANCE")));
        Battle b = new Battle(d, new Random(3), (u, t, moves) -> 0)
                .addPlayer(user)
                .addFoe(new Pokemon(d.species("GYARADOS"), 50, d));
        b.player().ability = "";
        b.foe().ability = "";
        b.foe().item = "MIRRORHERB";

        b.player().pbUseMoveSimple("SWORDSDANCE", b.player().index);

        assertEquals(2, b.player().stage(PBStats.ATTACK));
        assertEquals(2, b.foe().stage(PBStats.ATTACK), "the herb copies the +2 Attack");
        assertTrue(b.foe().item == null || b.foe().item.isEmpty(), "the herb is used up");
    }
}
