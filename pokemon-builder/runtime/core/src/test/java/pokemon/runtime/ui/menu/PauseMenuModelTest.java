package pokemon.runtime.ui.menu;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.GameState;

import static org.junit.jupiter.api.Assertions.*;

/**
 * L1/P4: the pause menu follows the project's "Modular Menu" plugin order
 * (Pokedex / PC / Pokemon / Bag / Trainer / Save / Load / Quit / Options /
 * Exit); entries with no runtime feature (Habitat / Pokegear / Tasks / Debug)
 * are omitted, and Pokemon is hidden while the party is empty.
 */
class PauseMenuModelTest {

    @Test
    @DisplayName("the pause menu follows the plugin order (L1/P4)")
    void pluginEntries() {
        GameState state = new GameState();
        state.playerName("测试家");
        // The Pokedex / PC entries need $Trainer.pokedex / $Trainer.pokepc
        // (Modular Menu:67/82).
        state.trainer().pokedex = true;
        state.trainer().pokepc = true;
        PauseMenuModel menu = new PauseMenuModel(state);
        // No party yet: the Pokemon entry is hidden (the plugin's availability
        // check is $Trainer.party.length > 0).
        assertEquals(10, menu.size());
        assertEquals(PauseMenuModel.Action.POKEDEX, menu.entryAt(0).action);
        assertEquals("图鉴", menu.entryAt(0).label);
        assertEquals(PauseMenuModel.Action.STORAGE, menu.entryAt(1).action);
        assertEquals("寄存系统", menu.entryAt(1).label);
        assertEquals("menuPC", menu.entryAt(1).icon);
        assertEquals(PauseMenuModel.Action.BAG, menu.entryAt(2).action);
        assertEquals(PauseMenuModel.Action.QUESTS, menu.entryAt(3).action, "Modular Menu:141 :MQS follows the bag");
        assertEquals("任务", menu.entryAt(3).label);
        assertEquals("menuQuests", menu.entryAt(3).icon);
        assertEquals(PauseMenuModel.Action.TRAINER, menu.entryAt(4).action);
        assertEquals("测试家", menu.entryAt(4).label, "the trainer entry shows the player name");
        assertEquals("menuTrainer", menu.entryAt(4).icon);
        assertEquals(PauseMenuModel.Action.SAVE, menu.entryAt(5).action);
        assertEquals(PauseMenuModel.Action.LOAD, menu.entryAt(6).action);
        assertEquals(PauseMenuModel.Action.TITLE, menu.entryAt(7).action);
        assertEquals("退出", menu.entryAt(7).label);
        assertEquals(PauseMenuModel.Action.OPTIONS, menu.entryAt(8).action);
        assertEquals(PauseMenuModel.Action.EXIT, menu.entryAt(9).action);
        assertEquals("menuExit", menu.entryAt(9).icon);
    }

    @Test
    @DisplayName("inside the Safari Zone the save entry is hidden (Modular Menu:176)")
    void safariHidesSave() {
        GameState state = new GameState();
        for (PauseMenuModel menu : new PauseMenuModel[] {new PauseMenuModel(state, false), new PauseMenuModel(state, true)}) {
            boolean save = false;
            for (int i = 0; i < menu.size(); i++) save |= menu.entryAt(i).action == PauseMenuModel.Action.SAVE;
            assertEquals(menu.size() == new PauseMenuModel(state, false).size(), save);
        }
    }

    @Test
    @DisplayName("a party adds the Pokemon entry in the plugin position (P4)")
    void partyEntry() {
        GameState state = new GameState();
        state.trainer().pokedex = true;
        state.trainer().pokepc = true;
        state.trainer().party.add(new Pokemon(null, 5, null));
        PauseMenuModel menu = new PauseMenuModel(state);
        assertEquals(11, menu.size());
        assertEquals(PauseMenuModel.Action.POKEDEX, menu.entryAt(0).action);
        assertEquals(PauseMenuModel.Action.STORAGE, menu.entryAt(1).action);
        assertEquals(PauseMenuModel.Action.PARTY, menu.entryAt(2).action);
        assertEquals("宝可梦", menu.entryAt(2).label);
        assertEquals(PauseMenuModel.Action.BAG, menu.entryAt(3).action);
    }

    @Test
    @DisplayName("the Pokedex / PC entries stay hidden until their flags are set (Modular Menu:67/82)")
    void dexAndPcNeedTheirFlags() {
        GameState state = new GameState();
        PauseMenuModel menu = new PauseMenuModel(state);
        for (int i = 0; i < menu.size(); i++) {
            assertNotEquals(PauseMenuModel.Action.POKEDEX, menu.entryAt(i).action);
            assertNotEquals(PauseMenuModel.Action.STORAGE, menu.entryAt(i).action);
        }
        state.trainer().pokedex = true;
        PauseMenuModel dexOnly = new PauseMenuModel(state);
        assertEquals(PauseMenuModel.Action.POKEDEX, dexOnly.entryAt(0).action);
        assertEquals(PauseMenuModel.Action.BAG, dexOnly.entryAt(1).action,
                "the PC entry still needs $Trainer.pokepc");
    }

    @Test
    @DisplayName("the cursor wraps in both directions (L1)")
    void cursorWraps() {
        GameState state = new GameState();
        state.trainer().pokedex = true;
        state.trainer().pokepc = true;
        PauseMenuModel menu = new PauseMenuModel(state);
        assertEquals(0, menu.index());
        menu.move(-1);
        assertEquals(menu.size() - 1, menu.index());
        menu.move(1);
        assertEquals(0, menu.index());
        menu.move(2);
        assertEquals(2, menu.index());
        assertEquals(PauseMenuModel.Action.BAG, menu.selectedAction());
    }
}
