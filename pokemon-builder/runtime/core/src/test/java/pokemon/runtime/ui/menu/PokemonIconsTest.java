package pokemon.runtime.ui.menu;

import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PokemonIconsTest {

    private static PbsData.Species species() {
        PbsData.Species s = new PbsData.Species();
        s.id = 3;
        s.internalName = "VENUSAUR";
        return s;
    }

    @Test
    void aShinyFallsBackToThePlainIconOfTheSameForm() {
        List<String> names = PokemonIcons.candidates(species(), false, true, false, 1, false, false);
        assertEquals("iconVENUSAURs_1", names.get(0), "the exact file first");
        assertEquals("icon003s_1", names.get(1));
        int plainForm = names.indexOf("iconVENUSAUR_1");
        int shinyBase = names.indexOf("iconVENUSAURs");
        assertTrue(plainForm > 1, names.toString());
        assertTrue(plainForm < names.indexOf("iconVENUSAUR"), "the form before the base species");
        assertTrue(names.contains("icon003"), "the base icon is the last resort");
        assertTrue(shinyBase > 0);
    }

    @Test
    void aFormWithoutItsOwnIconUsesTheBaseSpecies() {
        List<String> names = PokemonIcons.candidates(species(), false, false, false, 2, false, false);
        assertEquals("iconVENUSAUR_2", names.get(0));
        assertEquals("iconVENUSAUR", names.get(2));
        assertEquals("icon003", names.get(3));
    }

    @Test
    void anEggFollowsTheEggChain() {
        List<String> names = PokemonIcons.candidates(species(), false, true, false, 0, false, true);
        assertEquals("iconVENUSAURegg_0", names.get(0));
        assertEquals("iconEgg", names.get(names.size() - 1));
    }

    @Test
    void superShinyDoublesTheLetter() {
        List<String> names = PokemonIcons.candidates(species(), false, true, true, 0, false, false);
        assertEquals("iconVENUSAURss", names.get(0));
    }
}
