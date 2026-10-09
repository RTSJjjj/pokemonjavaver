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
        assertEquals(1, quests.stage("Q1"), "Quest#initialize: @stage = 1");

        assertTrue(quests.advance("Q1", 3));
        assertEquals(3, quests.stage("Q1"));

        assertTrue(quests.complete("Q1"));
        assertTrue(quests.isCompleted("Q1"));
        assertFalse(quests.advance("Q1", 4), "a completed quest no longer advances");
        assertEquals(1, quests.count(QuestLog.Status.COMPLETED));
        assertEquals(0, quests.count(QuestLog.Status.ACTIVE));
    }

    @Test
    @DisplayName("the plugin's messages and list moves (282_002_Quest_Main:42-166)")
    void messages() {
        QuestLog quests = new GameState().quests();
        String accepted = quests.activateQuest("Quest1", QuestLog.DEFAULT_COLOR, false, "茶月镇", 2, 1000L);
        assertEquals("\\se[Mining found all.ogg]<ac><c2=089D5EBF>接受到了新的任务！</c2>\n请点开任务日志查看详情！</ac>", accepted);
        assertEquals("你已经开始这个任务了。", quests.activateQuest("Quest1", null, false, "", 2, 2000L));
        assertEquals("茶月镇", quests.entry("Quest1").location);
        assertEquals(1000L, quests.entry("Quest1").time);

        String moved = quests.advanceQuestToStage("Quest1", 9, "7DC076EF", false, "", 2, 3000L);
        assertTrue(moved.contains("任务进度更新了！"));
        assertEquals(2, quests.stage("Quest1"), "the stage never passes the quest's stage count");
        assertEquals("7DC076EF", quests.entry("Quest1").color);

        String done = quests.completeQuest("Quest1", null, false, "", 2, 4000L);
        assertTrue(done.contains("任务完成！") && done.contains("您的任务日志已更新！"));
        assertEquals(4000L, quests.entry("Quest1").time, "completion stamps the time");
        assertEquals("你已经完成了这个任务。", quests.completeQuest("Quest1", null, false, "", 2, 5000L));
        assertEquals("你已经完成了这个任务。", quests.activateQuest("Quest1", null, false, "", 2, 5000L));
        assertEquals("你已经完成了这个任务。", quests.failQuest("Quest1", null, false, "", 2, 5000L));
    }

    @Test
    @DisplayName("advancing / completing a quest that was never started adds it silently")
    void silentCreation() {
        QuestLog quests = new GameState().quests();
        assertNull(quests.advanceQuestToStage("Quest4", 3, null, false, "", 5, 1L));
        assertTrue(quests.isActive("Quest4"));
        assertEquals(3, quests.stage("Quest4"));
        assertNull(quests.completeQuest("Quest8", null, false, "", 2, 1L));
        assertTrue(quests.isCompleted("Quest8"));
        assertEquals(QuestLog.DEFAULT_COLOR, quests.entry("Quest8").color);
        String failed = quests.failQuest("Quest4", null, false, "", 5, 2L);
        assertTrue(failed.contains("Quest failed!") && failed.contains("Your quest log has been updated!"));
        assertEquals(QuestLog.Status.FAILED, quests.status("Quest4"));
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
