package pokemon.runtime.data;

import com.badlogic.gdx.utils.JsonValue;

/**
 * One RMXP event command. The parameters stay as raw JSON values: the
 * interpreter (R6) walks them per command code, so no model tries to guess
 * their heterogeneous shape here.
 */
public final class EventCommand {

    public int index;
    public int code;
    public int indent;
    public String scriptBlockId;
    public JsonValue parameters;

    public static EventCommand parse(JsonValue node) {
        EventCommand command = new EventCommand();
        command.index = node.getInt("index", 0);
        command.code = node.getInt("code", -1);
        command.indent = node.getInt("indent", 0);
        command.scriptBlockId = node.getString("scriptBlockId", null);
        command.parameters = node.get("parameters");
        return command;
    }

    /** Parameters carry 241/245/249/250 audio names, switches, text, ... */
    public JsonValue parameter(int position) {
        if (parameters == null || !parameters.isArray() || position >= parameters.size) {
            return null;
        }
        return parameters.get(position);
    }
}