package pokemon.runtime.data;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

/** One generated/common-events/common-event-NNN.json document. */
public final class CommonEventData {

    public int id;
    public int slot;
    public String name;
    public int trigger;
    public int switchId;
    public Array<EventCommand> commands = new Array<>();
    public Array<String> scriptBlockIds = new Array<>();

    public static CommonEventData parse(JsonValue root) {
        CommonEventData data = new CommonEventData();
        data.id = root.getInt("id", -1);
        data.slot = root.getInt("slot", -1);
        data.name = root.getString("name", "");
        data.trigger = root.getInt("trigger", 0);
        data.switchId = root.getInt("switchId", 0);
        JsonValue commands = root.get("commands");
        if (commands != null && commands.isArray()) {
            for (JsonValue command = commands.child; command != null; command = command.next) {
                data.commands.add(EventCommand.parse(command));
            }
        }
        JsonValue scriptBlockIds = root.get("scriptBlockIds");
        if (scriptBlockIds != null && scriptBlockIds.isArray()) {
            for (JsonValue id = scriptBlockIds.child; id != null; id = id.next) {
                data.scriptBlockIds.add(id.asString());
            }
        }
        return data;
    }
}