package pokemon.runtime.ui.menu;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.state.GameState;

import static org.junit.jupiter.api.Assertions.*;

/**
 * L1: the pause menu only lists entries the stage-2 runtime can do; the
 * stage-3 entries of the project's plugin stay hidden.
 */
class PauseMenuModelTest {

    @Test
    @DisplayName("the stage-2 pause menu lists trainer/save/load/options/title/exit (L1)")
    void stageTwoEntries() {
        GameState state = new GameState();
        state.playerName("测试家");
        PauseMenuModel menu = new PauseMenuModel(state);
        assertEquals(6, menu.size());
        assertEquals(PauseMenuModel.Action.TRAINER, menu.entryAt(0).action);
        assertEquals("测试家", menu.entryAt(0).label, "the trainer entry shows the player name");
        assertEquals("menuTrainer", menu.entryAt(0).icon);
        assertEquals(PauseMenuModel.Action.SAVE, menu.entryAt(1).action);
        assertEquals(PauseMenuModel.Action.LOAD, menu.entryAt(2).action);
        assertEquals(PauseMenuModel.Action.OPTIONS, menu.entryAt(3).action);
        assertEquals(PauseMenuModel.Action.TITLE, menu.entryAt(4).action);
        assertEquals(PauseMenuModel.Action.EXIT, menu.entryAt(5).action);
    }

    @Test
    @DisplayName("the cursor wraps in both directions (L1)")
    void cursorWraps() {
        PauseMenuModel menu = new PauseMenuModel(new GameState());
        assertEquals(0, menu.index());
        menu.move(-1);
        assertEquals(menu.size() - 1, menu.index());
        menu.move(1);
        assertEquals(0, menu.index());
        menu.move(2);
        assertEquals(2, menu.index());
        assertEquals(PauseMenuModel.Action.LOAD, menu.selectedAction());
    }
}
