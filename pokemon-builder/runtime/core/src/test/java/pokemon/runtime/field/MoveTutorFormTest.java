package pokemon.runtime.field;

import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.GameState;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/** pbSpeciesCompatible?(fSpecies, move): the tutor's compatibility reads the form's own tm.txt entry. */
class MoveTutorFormTest {
    @Test
    void formsUseTheirOwnCompatibilityEntry() throws Exception {
        File root = new File("E:/仓库/范例/929/pokemon-builder/generated");
        if (!root.isDirectory()) return;
        PbsData data = PbsData.parse(root);
        ItemHandlers handlers = new ItemHandlers(data, new GameState(), java.time.LocalTime::now);
        PbsData.Move move = data.move("HARUKIKAGE");
        Pokemon base = new Pokemon(data.species("JIGGLYPUFF"), 20, data);
        assertTrue(handlers.compatibleWithMove(base, move));
        Pokemon form = new Pokemon(data.species("JIGGLYPUFF"), 20, data);
        form.setForm(data, 1);
        assertEquals("JIGGLYPUFF_1", form.internalName);
        assertTrue(handlers.compatibleWithMove(form, move), "JIGGLYPUFF_1 is listed for HARUKIKAGE");
        Pokemon charmander = new Pokemon(data.species("CHARMANDER"), 20, data);
        assertFalse(handlers.compatibleWithMove(charmander, move));
    }
}
