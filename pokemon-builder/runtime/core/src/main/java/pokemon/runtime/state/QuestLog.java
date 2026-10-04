package pokemon.runtime.state;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * The Quest plugin's runtime state (plugin batch 1): Essentials' own
 * {@code Player_Quests} keeps one entry per quest with a status
 * (active / completed / failed) and a stage number; the quest *definitions*
 * (names, stage texts) only matter for the quest log UI, which arrives with the
 * menu batch - the event scripts only pass ids, so tracking the state here is
 * enough to make 331 script calls work.
 */
public final class QuestLog {

    public enum Status { ACTIVE, COMPLETED, FAILED }

    /** One quest's state; {@code stage} mirrors {@code Quest#stage}. */
    public static final class Entry {
        public String id;
        public Status status = Status.ACTIVE;
        public int stage;
        /** "new": the log shows a "!" until the player looked at it. */
        public boolean updated;
    }

    private final ObjectMap<String, Entry> quests = new ObjectMap<>();
    private final StateVersion version;

    QuestLog(StateVersion version) {
        this.version = version;
    }

    /** {@code activateQuest(id)}: true when the quest became newly active. */
    public boolean activate(String id) {
        Entry entry = quests.get(id);
        if (entry != null) {
            return false; // already active / completed / failed
        }
        Entry created = new Entry();
        created.id = id;
        created.updated = true;
        quests.put(id, created);
        version.bump();
        return true;
    }

    /** {@code advanceQuestToStage(id, stage)}: only active quests move on. */
    public boolean advance(String id, int stage) {
        Entry entry = quests.get(id);
        if (entry == null || entry.status != Status.ACTIVE) {
            return false;
        }
        entry.stage = stage;
        entry.updated = true;
        version.bump();
        return true;
    }

    /** {@code completeQuest(id)}. */
    public boolean complete(String id) {
        return finish(id, Status.COMPLETED);
    }

    /** The plugin's "fail quest" helper; kept for completeness. */
    public boolean fail(String id) {
        return finish(id, Status.FAILED);
    }

    private boolean finish(String id, Status status) {
        Entry entry = quests.get(id);
        if (entry == null || entry.status == status) {
            return false;
        }
        entry.status = status;
        entry.updated = true;
        version.bump();
        return true;
    }

    /** Marks the quest as read (the log UI clears the "!" icon). */
    public void markRead(String id) {
        Entry entry = quests.get(id);
        if (entry != null && entry.updated) {
            entry.updated = false;
            version.bump();
        }
    }

    public Entry entry(String id) {
        return quests.get(id);
    }

    public Status status(String id) {
        Entry entry = quests.get(id);
        return entry == null ? null : entry.status;
    }

    public int stage(String id) {
        Entry entry = quests.get(id);
        return entry == null ? -1 : entry.stage;
    }

    public boolean isActive(String id) {
        return status(id) == Status.ACTIVE;
    }

    public boolean isCompleted(String id) {
        return status(id) == Status.COMPLETED;
    }

    public int count(Status status) {
        int total = 0;
        for (Entry entry : quests.values()) {
            if (entry.status == status) {
                total++;
            }
        }
        return total;
    }

    public int size() {
        return quests.size;
    }

    /** Every entry, ordered by id, for saves and the future quest log. */
    public Array<Entry> entries() {
        Array<String> ids = new Array<>();
        for (String id : quests.keys()) {
            ids.add(id);
        }
        ids.sort();
        Array<Entry> ordered = new Array<>();
        for (String id : ids) {
            ordered.add(quests.get(id));
        }
        return ordered;
    }

    /** Applies one saved entry (used by SaveManager). */
    public void restore(String id, Status status, int stage, boolean updated) {
        Entry entry = new Entry();
        entry.id = id;
        entry.status = status;
        entry.stage = stage;
        entry.updated = updated;
        quests.put(id, entry);
    }

    public void clear() {
        if (quests.size == 0) {
            return;
        }
        quests.clear();
        version.bump();
    }
}
