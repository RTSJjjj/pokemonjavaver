package pokemon.runtime.pokemon;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 228_PScreen_MoveRelearner:1-34 on the project's real species data (skipped when generated/ is missing). */
class MoveRelearnerTest {

    private static PbsData data() throws Exception {
        File root = pokemon.runtime.map.TestData.runtimeDataRoot();
        return root != null ? PbsData.parse(root) : null;
    }

    @Test
    void offersFirstMovesThenRecordsThenNaturalMoves() throws Exception {
        PbsData data = data();
        if (data == null) return;
        Pokemon torchic = new Pokemon(data.species("TORCHIC"), 30, data);
        List<String> natural = MoveRelearner.pbGetRelearnableMoves(torchic, data);
        assertFalse(natural.isEmpty(), "a level 30 Torchic has forgotten moves");
        for (Pokemon.MoveSlot slot : torchic.moves) {
            assertFalse(natural.contains(slot.move.internalName), "a known move is not offered");
        }
        torchic.firstMoves.add("SPLASH");
        torchic.trMoves.add("CELEBRATE");
        List<String> all = MoveRelearner.pbGetRelearnableMoves(torchic, data);
        assertEquals("SPLASH", all.get(0), "first moves come first");
        assertEquals("CELEBRATE", all.get(1), "then the technical records");
        assertEquals(natural.size() + 2, all.size());
    }

    @Test
    void eggsHaveNone() throws Exception {
        PbsData data = data();
        if (data == null) return;
        Pokemon egg = new Pokemon(data.species("TORCHIC"), 30, data);
        egg.egg = true;
        assertFalse(MoveRelearner.hasRelearnableMove(egg, data));
        assertFalse(MoveRelearner.hasRelearnableMove(null, data));
    }

    @Test
    void aTeachableMoveIsNotOfferedTwice() throws Exception {
        PbsData data = data();
        if (data == null) return;
        Pokemon torchic = new Pokemon(data.species("TORCHIC"), 30, data);
        String natural = MoveRelearner.pbGetRelearnableMoves(torchic, data).get(0);
        torchic.trMoves.add(natural);
        torchic.firstMoves.add(natural);
        List<String> all = MoveRelearner.pbGetRelearnableMoves(torchic, data);
        assertEquals(1, java.util.Collections.frequency(all, natural), "moves|[] removes the duplicates");
    }
}
