package pokemon.runtime.event;

import org.junit.jupiter.api.Test;
import pokemon.runtime.map.TestData;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.GameState;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The project's own NPC scripts (359_MrHyper, 365_changeBalls, 373_GiveAllMemories, 360_pbCrystalWarp). */
class PluginScriptsTest {
    private static final class Script {
        final List<String> messages = new ArrayList<>();
        int choice;
        boolean confirm = true;

        TaskFieldScene scene() {
            return new TaskFieldScene(request -> {
                switch (request.kind) {
                    case MESSAGE: messages.add(request.text); return null;
                    case CONFIRM: messages.add(request.text); return confirm;
                    case CHOOSE: messages.add(request.text); return choice;
                    case CHOOSE_NON_EGG: return 0;
                    case ACTION: request.action.get(); return null;
                    default: return null;
                }
            });
        }
    }

    private static PbsData data() throws Exception {
        File root = TestData.runtimeDataRoot();
        return root == null ? null : PbsData.parse(root);
    }

    @Test
    void mrHyperTrainsOneIvWithABottleCap() throws Exception {
        PbsData data = data();
        if (data == null) return;
        GameState state = new GameState();
        Pokemon p = new Pokemon(data.species("TORCHIC"), 60, data);
        java.util.Arrays.fill(p.ivs, 10);
        state.trainer().party.add(p);
        state.inventory().add("BOTTLECAP", 2);
        Script script = new Script();
        script.choice = 1;                                           // the second stat in the list: 攻击
        // the loop asks again after a training: the second round is cancelled
        TaskFieldScene scene = new TaskFieldScene(request -> {
            if (request.kind == TaskFieldScene.Kind.CHOOSE) {
                int pick = script.choice;
                script.choice = -1;
                return pick;
            }
            if (request.kind == TaskFieldScene.Kind.CHOOSE_NON_EGG) return 0;
            script.messages.add(request.text);
            return null;
        });
        new PluginScripts(state, data, scene, null).pbMrHyper();
        assertEquals(31, p.ivs[1]);
        assertEquals(10, p.ivs[0]);
        assertEquals(1, state.inventory().count("BOTTLECAP"));
        assertTrue(script.messages.stream().anyMatch(m -> m.contains("能力锻炼了")), script.messages.toString());
    }

    @Test
    void mrHyperRefusesBelowLevel50() throws Exception {
        PbsData data = data();
        if (data == null) return;
        GameState state = new GameState();
        state.trainer().party.add(new Pokemon(data.species("TORCHIC"), 20, data));
        Script script = new Script();
        new PluginScripts(state, data, script.scene(), null).pbMrHyper();
        assertTrue(script.messages.get(script.messages.size() - 1).contains("50级"), script.messages.toString());
    }

    @Test
    void changeBallsSwapsTheBallAndKeepsTheOldOne() throws Exception {
        PbsData data = data();
        if (data == null) return;
        GameState state = new GameState();
        state.trainer().money = 5000;
        Pokemon p = new Pokemon(data.species("TORCHIC"), 10, data);
        p.ballused = 0;                                              // POKEBALL
        state.trainer().party.add(p);
        Script script = new Script();
        script.choice = 1;                                           // the second ball of the list (GREATBALL)
        new PluginScripts(state, data, script.scene(), null).changeBalls();
        assertEquals(1, p.ballused);
        assertEquals(3800, state.trainer().money);
        assertEquals(1, state.inventory().count("POKEBALL"), "the old ball went into the bag");
    }

    @Test
    void memoriesAreAllGiven() throws Exception {
        PbsData data = data();
        GameState state = new GameState();
        Script script = new Script();
        new PluginScripts(state, data, script.scene(), null).pbGiveAllMemories();
        assertEquals(1, state.inventory().count("FIREMEMORY"));
        assertEquals(1, state.inventory().count("FAIRYMEMORY"));
        assertEquals(2, script.messages.size());
    }
}
