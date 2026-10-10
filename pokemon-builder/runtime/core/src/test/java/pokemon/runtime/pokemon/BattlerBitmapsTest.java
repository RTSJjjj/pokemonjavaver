package pokemon.runtime.pokemon;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** pbCheckPokemonBitmapFiles' fallback order (251_PSystem_FileUtilities:72-113). */
class BattlerBitmapsTest {
    private static PbsData.Species species() {
        PbsData.Species s = new PbsData.Species();
        s.internalName = "BUTTERFREE";
        s.id = 12;
        return s;
    }

    @Test void aFemaleShinyFormTriesEveryFactorBeforeTheBareSpecies() {
        List<String> names = BattlerBitmaps.names(species(), false, true, true, false, 2);
        // all asked factors first (gender f, shiny s, form _2), then the lowest dropped first: gender
        assertEquals(Arrays.asList("BUTTERFREEfs_2", "012fs_2", "BUTTERFREEs_2", "012s_2", "BUTTERFREEf_2", "012f_2",
                "BUTTERFREE_2", "012_2", "BUTTERFREEfs", "012fs"), names.subList(0, 10));
        assertEquals("012", names.get(names.size() - 1));
    }

    @Test void aPlainMaleSpeciesIsJustTheName() {
        assertEquals(Arrays.asList("BUTTERFREE", "012"), BattlerBitmaps.names(species(), false, false, false, false, 0));
    }

    @Test void theBackSpriteAddsB() {
        assertEquals("012fb", BattlerBitmaps.names(species(), true, true, false, false, 0).get(1));
    }

    @Test void findReturnsTheFirstExistingFile() {
        String found = BattlerBitmaps.find(species(), false, true, false, false, 0, n -> "012".equals(n) ? n : null);
        assertEquals("012", found);
        assertEquals("012f", BattlerBitmaps.find(species(), false, true, false, false, 0, n -> "012f".equals(n) ? n : null));
    }
}
