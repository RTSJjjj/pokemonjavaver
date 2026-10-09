package pokemon.runtime.event;

import org.junit.jupiter.api.Test;
import pokemon.runtime.field.FieldScene;
import pokemon.runtime.field.ItemHandlers;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.GameState;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 362_changeShiny teachEggMoves on the project's real species data (skipped when generated/ is missing). */
class EggMoveTutorTest {

    private static final class Script implements FieldScene {
        final List<String> messages = new ArrayList<>();
        boolean confirm = true;
        int chosenPokemon = 0;
        int chosenMove = 0;

        public void pbMessage(String text) { messages.add(text); }
        public boolean pbConfirmMessage(String text) { messages.add(text); return confirm; }
        public int pbMessage(String text, List<String> commands, int cmdIfCancel) {
            messages.add(text + commands);
            int pick = chosenMove;
            chosenMove = -1;                                   // the second round: B
            return pick;
        }
        public void pbWait(int frames) { }
        public int pbMoveTutorChoose(String move, List<String> movelist, boolean byMachine) { return -1; }
        public void pbSEPlay(String name, int volume) { }
        public int pbChooseNonEggPokemon() { return chosenPokemon; }
    }

    @Test
    void teachesAnEggMoveForTheFee() throws Exception {
        File root = pokemon.runtime.map.TestData.runtimeDataRoot();
        if (root == null) return;
        PbsData data = PbsData.parse(root);
        GameState state = new GameState();
        Pokemon torchic = new Pokemon(data.species("TORCHIC"), 5, data);
        state.trainer().party.add(torchic);
        state.trainer().money = 12000;
        Script scene = new Script();
        int moves = torchic.moves.size;
        new EggMoveTutor(state, data, scene, new ItemHandlers(data, state, java.time.LocalTime::now)).teachEggMoves();
        assertEquals(moves + 1, torchic.moves.size, "the chosen egg move was learned");
        assertEquals(7000, state.trainer().money);
        assertTrue(scene.messages.stream().anyMatch(m -> m.contains("你支付了5000金钱")), scene.messages.toString());
    }

    @Test
    void refusesWithoutMoney() throws Exception {
        File root = pokemon.runtime.map.TestData.runtimeDataRoot();
        if (root == null) return;
        PbsData data = PbsData.parse(root);
        GameState state = new GameState();
        state.trainer().party.add(new Pokemon(data.species("TORCHIC"), 5, data));
        state.trainer().money = 4999;
        Script scene = new Script();
        new EggMoveTutor(state, data, scene, new ItemHandlers(data, state, java.time.LocalTime::now)).teachEggMoves();
        assertEquals(List.of("\\G对不起，您的金钱不足。"), scene.messages);
        assertEquals(4999, state.trainer().money);
    }

    @Test
    void changesShinyForTheFee() throws Exception {
        File root = pokemon.runtime.map.TestData.runtimeDataRoot();
        if (root == null) return;
        PbsData data = PbsData.parse(root);
        GameState state = new GameState();
        Pokemon torchic = new Pokemon(data.species("TORCHIC"), 5, data);
        torchic.shiny = false;
        state.trainer().party.add(torchic);
        state.trainer().money = 250000;
        Script scene = new Script();
        new EggMoveTutor(state, data, scene, new ItemHandlers(data, state, java.time.LocalTime::now)).changeShinyByNPC();
        assertTrue(torchic.shiny);
        assertEquals(0, state.trainer().money);
        assertTrue(scene.messages.stream().anyMatch(m -> m.contains("现在变为异色宝可梦了！")), scene.messages.toString());
    }
}
