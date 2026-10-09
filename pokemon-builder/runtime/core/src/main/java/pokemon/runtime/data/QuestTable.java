package pokemon.runtime.data;

import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Quest plugin's quest definitions ({@code QuestModule}, 284_004_Quest_Data) from {@code generated/quests.json}, with the
 * lookups of 282_002_Quest_Main's {@code QuestData} (:197-257). An unknown quest answers the empty string, like a quest hash
 * with no field (the plugin's {@code "#{nil}"}).
 */
public final class QuestTable {

    /** One quest hash. Missing fields are null. */
    public static final class Quest {
        public String key;
        public String id;
        public String name;
        public String questGiver;
        public final Map<Integer, String> stages = new HashMap<>();
        public final Map<Integer, String> locations = new HashMap<>();
        /** {@code :QuestDescription}: one string, or one per stage. */
        public String description;
        public final List<String> descriptions = new ArrayList<>();
        public String reward;
    }

    private final Map<String, Quest> quests = new HashMap<>();

    public static QuestTable empty() {
        return new QuestTable();
    }

    public static QuestTable parse(JsonValue root) {
        QuestTable table = new QuestTable();
        JsonValue list = root == null ? null : root.get("quests");
        if (list == null || !list.isArray()) {
            return table;
        }
        for (JsonValue node = list.child; node != null; node = node.next) {
            Quest quest = new Quest();
            quest.key = node.getString("key", "");
            quest.id = node.getString("id", null);
            quest.name = node.getString("name", null);
            quest.questGiver = node.getString("questGiver", null);
            readNumbered(node.get("stages"), quest.stages);
            readNumbered(node.get("locations"), quest.locations);
            JsonValue description = node.get("description");
            if (description != null && description.isArray()) {
                for (JsonValue part = description.child; part != null; part = part.next) {
                    quest.descriptions.add(part.asString());
                }
            } else if (description != null && description.isString()) {
                quest.description = description.asString();
            }
            quest.reward = node.getString("reward", null);
            table.quests.put(quest.key, quest);
        }
        return table;
    }

    private static void readNumbered(JsonValue object, Map<Integer, String> target) {
        if (object == null || !object.isObject()) {
            return;
        }
        for (JsonValue entry = object.child; entry != null; entry = entry.next) {
            try {
                target.put(Integer.parseInt(entry.name), entry.asString());
            } catch (NumberFormatException ignored) {
                // not a stage number
            }
        }
    }

    public boolean has(String quest) {
        return quests.containsKey(quest);
    }

    public int size() {
        return quests.size();
    }

    /** {@code getName} (:200-202). */
    public String name(String quest) {
        Quest q = quests.get(quest);
        return q == null || q.name == null ? "" : q.name;
    }

    /** {@code getQuestGiver} (:205-207). */
    public String questGiver(String quest) {
        Quest q = quests.get(quest);
        return q == null || q.questGiver == null ? "" : q.questGiver;
    }

    /** {@code getQuestReward} (:216-218). */
    public String reward(String quest) {
        Quest q = quests.get(quest);
        return q == null || q.reward == null ? "" : q.reward;
    }

    /** {@code getMaxStagesForQuest} (:253-256): the number of {@code :StageN} keys. */
    public int maxStages(String quest) {
        Quest q = quests.get(quest);
        return q == null ? 0 : q.stages.size();
    }

    /** {@code getStageDescription} (:244-248). */
    public String stageDescription(String quest, int stage) {
        Quest q = quests.get(quest);
        String text = q == null ? null : q.stages.get(stage);
        return text == null ? "" : text;
    }

    /** {@code getStageLocation} (:238-242). */
    public String stageLocation(String quest, int stage) {
        Quest q = quests.get(quest);
        String text = q == null ? null : q.locations.get(stage);
        return text == null ? "" : text;
    }

    /** {@code getQuestDescription(quest, stage)} (:221-233): a plain string, or the list entry of the stage (the last one past the end). */
    public String description(String quest, int stage) {
        Quest q = quests.get(quest);
        if (q == null) {
            return "获取出错，请检查代码";                                           // :231 (the quest has no description at all)
        }
        if (q.description != null) {
            return q.description;                                                  // :224-225
        }
        if (!q.descriptions.isEmpty()) {
            return stage - 1 >= q.descriptions.size() ? q.descriptions.get(q.descriptions.size() - 1)
                    : q.descriptions.get(Math.max(0, stage - 1));                  // :226-230
        }
        return "获取出错，请检查代码";
    }
}
