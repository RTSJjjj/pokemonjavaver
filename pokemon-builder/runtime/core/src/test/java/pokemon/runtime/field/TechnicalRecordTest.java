package pokemon.runtime.field;

import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.GameState;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 188_PItem_Items:948-953: a Technical Record is used up and its move stays relearnable (trmoves). */
class TechnicalRecordTest {
    private static final class Scene implements ItemScene {
        public void pbStartScene(String helpText, String[] annotations) { }
        public void pbSetHelpText(String text) { }
        public void pbRefreshAnnotations(java.util.function.Predicate<Pokemon> able) { }
        public void pbClearAnnotations() { }
        public void pbDisplay(String text) { }
        public boolean pbConfirm(String text) { return true; }
        public void pbRefresh() { }
        public void pbHardRefresh() { }
        public int pbChooseMove(Pokemon pkmn, String text) { return -1; }
        public int pbChoosePokemon(String text) { return -1; }
        public void pbMessage(String text) { }
        public int pbMessage(String text, List<String> commands, int cmdIfCancel) { return -1; }
        public int pbShowCommands(String text, List<String> commands, int index) { return -1; }
        public int pbMessageChooseNumber(String text, int max, int defaultValue, int cancelValue) { return cancelValue; }
        public void pbTopRightWindow(String text) { }
        public int pbForgetMove(Pokemon pkmn, PbsData.Move moveToLearn) { return -1; }
        public void pbSEPlay(String name) { }
        public void pbEvolution(Pokemon pkmn, PbsData.Species species, String item) { }
    }

    @Test
    void aRecordIsUsedUpAndRemembered() throws Exception {
        File root = new File("E:/仓库/范例/929/pokemon-builder/generated");
        if (!root.isDirectory()) return;
        PbsData data = PbsData.parse(root);
        GameState state = new GameState();
        ItemHandlers handlers = new ItemHandlers(data, state, java.time.LocalTime::now);
        Pokemon torchic = new Pokemon(data.species("TORCHIC"), 5, data);
        String record = null;
        for (PbsData.Item item : data.items.values()) {
            if (item.fieldUse != 6 || item.machine == null) continue;
            PbsData.Move move = data.move(item.machine);
            boolean known = false;
            for (Pokemon.MoveSlot slot : torchic.moves) known |= slot.move != null && slot.move.internalName.equals(item.machine);
            if (move != null && !known && handlers.compatibleWithMove(torchic, move)) {
                record = item.internalName;
                break;
            }
        }
        assertNotNull(record, "Torchic can learn some record");
        String machine = data.item(record).machine;
        if (torchic.moves.size >= 4) torchic.moves.removeIndex(3);
        state.inventory().add(record, 1);
        assertTrue(handlers.pbUseItemOnPokemon(record, torchic, new Scene()));
        assertFalse(state.inventory().has(record), "the record is used up");
        assertTrue(torchic.trMoves.contains(machine, false), "and remembered");
    }
}
