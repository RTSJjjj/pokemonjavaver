package pokemon.runtime.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.save.SaveManager;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plugin batch 1: the Quest plugin's state machine (activate / advance to a
 * stage / complete) and its persistence. The events call these three APIs 331
 * times, so the logic has to survive saves exactly like Essentials' own
 * {@code Player_Quests}.
 */
class QuestLogTest {

    @Test
    @DisplayName("activate, advance and complete follow the plugin's transitions")
    void transitions() {
        GameState state = new GameState();
        QuestLog quests = state.quests();
        assertNull(quests.status("Q1"));
        assertFalse(quests.advance("Q1", 2), "an inactive quest cannot advance");

        assertTrue(quests.activate("Q1"));
        assertFalse(quests.activate("Q1"), "activating twice is ignored");
        assertTrue(quests.isActive("Q1"));
        assertEquals(0, quests.stage("Q1"));

        assertTrue(quests.advance("Q1", 3));
        assertEquals(3, quests.stage("Q1"));

        assertTrue(quests.complete("Q1"));
        assertTrue(quests.isCompleted("Q1"));
        assertFalse(quests.advance("Q1", 4), "a completed quest no longer advances");
        assertEquals(1, quests.count(QuestLog.Status.COMPLETED));
        assertEquals(0, quests.count(QuestLog.Status.ACTIVE));
    }

    @Test
    @DisplayName("quests and the follower flag survive a save round trip")
    void saves() {
        GameState state = new GameState();
        state.enterMap(6, 10, 10);
        state.quests().activate("Main1");
        state.quests().advance("Main1", 2);
        state.quests().activate("Side1");
        state.quests().complete("Side1");
        state.quests().activate("Side2");
        state.quests().fail("Side2");
        state.followerToggled(true);

        SaveManager saves = new SaveManager();
        String json = saves.toJson(state);

        GameState loaded = new GameState();
        assertTrue(saves.fromJson(json, loaded));
        assertTrue(loaded.quests().isActive("Main1"));
        assertEquals(2, loaded.quests().stage("Main1"));
        assertTrue(loaded.quests().isCompleted("Side1"));
        assertEquals(QuestLog.Status.FAILED, loaded.quests().status("Side2"));
        assertTrue(loaded.followerToggled());
    }

    @Test
    @DisplayName("a save written before the plugin batch still loads")
    void olderSavesLoad() {
        String legacy = "{\"saveVersion\":1,\"map\":{\"id\":2,\"x\":36,\"y\":7,\"direction\":8}}";
        GameState state = new GameState();
        assertTrue(new SaveManager().fromJson(legacy, state));
        assertEquals(0, state.quests().size());
        assertFalse(state.followerToggled());
    }
}
