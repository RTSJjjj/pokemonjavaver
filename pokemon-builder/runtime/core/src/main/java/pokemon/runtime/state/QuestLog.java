package pokemon.runtime.state;

import java.util.ArrayList;
import java.util.List;

/**
 * The Quest plugin's {@code Player_Quests} (282_002_Quest_Main:29-155): three ordered lists - active, completed and failed
 * quests - of {@link Entry} ({@code Quest}, :4-27). The quest definitions (names, stage texts, rewards) live in the generated
 * quest table; this class only holds what the player did.
 *
 * <p>Every transition returns the {@code pbMessage} the plugin shows (or null), so the event interpreter can show it.</p>
 */
public final class QuestLog {

    public enum Status { ACTIVE, COMPLETED, FAILED }

    /** 281_001_Quest_Config:11 / :14. */
    public static final String QUEST_JINGLE = "Mining found all.ogg";
    public static final String QUEST_FAIL = "GUI sel buzzer.ogg";
    /** {@code colorQuest(nil)} and {@code colorQuest("red")} (281_001_Quest_Config:24-36). */
    public static final String DEFAULT_COLOR = "2D4A5694";
    public static final String RED = "089D5EBF";

    /** One quest ({@code Quest}, 282_002_Quest_Main:4-27). */
    public static final class Entry {
        public String id;
        public Status status = Status.ACTIVE;
        /** {@code @stage}: starts at 1 and never passes the quest's stage count. */
        public int stage = 1;
        /** {@code @new}: the log shows a "new" icon until the player opened the quest. */
        public boolean updated = true;
        /** {@code @color}: the 15-bit colour pair of the name in the list ({@code <c2=...>}). */
        public String color = DEFAULT_COLOR;
        /** {@code @story}: story quests are listed in bold. */
        public boolean story;
        /** {@code @time}: epoch milliseconds of the start (or of the last completion / failure). */
        public long time;
        /** {@code @location}: the map name where the quest began. */
        public String location = "";
    }

    private final List<Entry> active = new ArrayList<>();
    private final List<Entry> completed = new ArrayList<>();
    private final List<Entry> failed = new ArrayList<>();
    private final StateVersion version;

    QuestLog(StateVersion version) {
        this.version = version;
    }

    // ------------------------------------------------------------------
    // The plugin's transitions (282_002_Quest_Main:42-155)
    // ------------------------------------------------------------------

    /** {@code activateQuest(quest, color, story)} (:42-64): the message to show, or null. */
    public String activateQuest(String id, String color, boolean story, String location, int maxStages, long now) {
        if (find(active, id) != null) return "你已经开始这个任务了。";                  // :44-47
        if (find(completed, id) != null) return "你已经完成了这个任务。";                // :50-53
        if (find(failed, id) != null) return "这个任务你已经失败了。";                   // :56-59
        active.add(newQuest(id, color, story, location, maxStages, now));              // :62
        version.bump();
        return "\\se[" + QUEST_JINGLE + "]<ac><c2=" + RED + ">接受到了新的任务！</c2>\n请点开任务日志查看详情！</ac>";   // :63
    }

    /** {@code failQuest(quest, color, story)} (:66-101). */
    public String failQuest(String id, String color, boolean story, String location, int maxStages, long now) {
        if (find(completed, id) != null) return "你已经完成了这个任务。";                // :69-74
        if (find(failed, id) != null) return "你已经失败了这个任务。";                   // :75-80
        Entry entry = find(active, id);
        version.bump();
        if (entry != null) {                                                           // :81-92
            if (color != null) entry.color = color;
            entry.updated = true;                                                      // :86 the "new" icon appears again
            entry.time = now;
            entry.status = Status.FAILED;
            active.remove(entry);
            failed.add(entry);
            return "\\se[" + QUEST_FAIL + "]<ac><c2=" + RED + ">Quest failed!</c2>\nYour quest log has been updated!</ac>";   // :91
        }
        Entry created = newQuest(id, color == null ? DEFAULT_COLOR : color, story, location, maxStages, now);   // :95-98
        created.status = Status.FAILED;
        failed.add(created);
        return null;
    }

    /** {@code completeQuest(quest, color, story)} (:103-139). */
    public String completeQuest(String id, String color, boolean story, String location, int maxStages, long now) {
        if (find(completed, id) != null) return "你已经完成了这个任务。";                // :107-112
        if (find(failed, id) != null) return "你已经失败了这个任务。";                   // :113-118
        Entry entry = find(active, id);
        version.bump();
        if (entry != null) {                                                           // :119-131
            if (color != null) entry.color = color;
            entry.updated = true;
            entry.time = now;
            entry.status = Status.COMPLETED;
            active.remove(entry);
            completed.add(entry);
            return "\\se[" + QUEST_JINGLE + "]<ac><c2=" + RED + ">任务完成！</c2>\n您的任务日志已更新！</ac>";   // :129
        }
        Entry created = newQuest(id, color == null ? DEFAULT_COLOR : color, story, location, maxStages, now);   // :134-136
        created.status = Status.COMPLETED;
        completed.add(created);
        return null;
    }

    /**
     * {@code advanceQuestToStage(quest, stageNum, color, story)} (:141-166). Only an active quest moves on (and says so); any
     * other quest - even a completed one - starts as a new active quest at that stage, silently (:162-166).
     */
    public String advanceQuestToStage(String id, int stage, String color, boolean story, String location,
                                      int maxStages, long now) {
        Entry entry = find(active, id);
        version.bump();
        if (entry != null) {                                                           // :144-152
            entry.stage = clamp(stage, maxStages);
            if (color != null) entry.color = color;
            entry.updated = true;
            return "\\se[" + QUEST_JINGLE + "]<ac><c2=" + RED + ">任务进度更新了！</c2>\n请查看任务日志！</ac>";   // :150
        }
        Entry created = newQuest(id, color == null ? DEFAULT_COLOR : color, story, location, maxStages, now);   // :162-165
        created.stage = clamp(stage, maxStages);
        active.add(created);
        return null;
    }

    /** {@code Quest#initialize} (:11-18) with the stage clamp of {@code stage=} (:21-26). */
    private static Entry newQuest(String id, String color, boolean story, String location, int maxStages, long now) {
        Entry entry = new Entry();
        entry.id = id;
        entry.stage = 1;
        entry.time = now;
        entry.location = location == null ? "" : location;
        entry.updated = true;
        entry.color = color == null ? DEFAULT_COLOR : color;
        entry.story = story;
        entry.stage = clamp(1, maxStages);
        return entry;
    }

    private static int clamp(int stage, int maxStages) {
        return maxStages > 0 && stage > maxStages ? maxStages : stage;
    }

    private static Entry find(List<Entry> list, String id) {
        for (Entry entry : list) {
            if (entry.id.equals(id)) return entry;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Convenience used by tests and conditions (no messages)
    // ------------------------------------------------------------------

    /** True when the quest became newly active. */
    public boolean activate(String id) {
        if (entry(id) != null) return false;
        activateQuest(id, DEFAULT_COLOR, false, "", 0, System.currentTimeMillis());
        return true;
    }

    /** True when the quest was active and moved to {@code stage}. */
    public boolean advance(String id, int stage) {
        if (find(active, id) == null) return false;
        advanceQuestToStage(id, stage, null, false, "", 0, System.currentTimeMillis());
        return true;
    }

    public boolean complete(String id) {
        if (find(active, id) == null) return false;
        completeQuest(id, null, false, "", 0, System.currentTimeMillis());
        return true;
    }

    public boolean fail(String id) {
        if (find(active, id) == null) return false;
        failQuest(id, null, false, "", 0, System.currentTimeMillis());
        return true;
    }

    /** Marks the quest as read (the log clears its "new" icon, {@code quest.new = false}, 283_003_Quest_UI:179). */
    public void markRead(String id) {
        Entry entry = entry(id);
        if (entry != null && entry.updated) {
            entry.updated = false;
            version.bump();
        }
    }

    public Entry entry(String id) {
        Entry found = find(active, id);
        if (found == null) found = find(completed, id);
        if (found == null) found = find(failed, id);
        return found;
    }

    public Status status(String id) {
        Entry entry = entry(id);
        return entry == null ? null : entry.status;
    }

    public int stage(String id) {
        Entry entry = entry(id);
        return entry == null ? -1 : entry.stage;
    }

    public boolean isActive(String id) {
        return status(id) == Status.ACTIVE;
    }

    public boolean isCompleted(String id) {
        return status(id) == Status.COMPLETED;
    }

    public int count(Status status) {
        return list(status).size();
    }

    public int size() {
        return active.size() + completed.size() + failed.size();
    }

    /** {@code hasAnyQuests?} (282_002_Quest_Main:317-323). */
    public boolean hasAny() {
        return size() > 0;
    }

    /** The plugin's lists in their order ({@code active_quests}, {@code completed_quests}, {@code failed_quests}). */
    public List<Entry> list(Status status) {
        switch (status) {
            case COMPLETED: return completed;
            case FAILED: return failed;
            default: return active;
        }
    }

    /** Every entry: active, completed, then failed, each in its own order (the save writes them like this). */
    public List<Entry> entries() {
        List<Entry> all = new ArrayList<>(active);
        all.addAll(completed);
        all.addAll(failed);
        return all;
    }

    /** Applies one saved entry (used by SaveManager); entries arrive in list order. */
    public void restore(Entry entry) {
        list(entry.status).add(entry);
    }

    public void clear() {
        if (size() == 0) {
            return;
        }
        active.clear();
        completed.clear();
        failed.clear();
        version.bump();
    }
}
