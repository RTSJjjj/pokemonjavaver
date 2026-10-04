package pokemon.runtime.data;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

/**
 * One RMXP Move Route (command 209): the route list plus its repeat /
 * skippable flags. The interpreter hands it to the map side, which plays it
 * (R6.3b-2).
 *
 * <p>Move command codes follow RPG::MoveCommand: 0 end, 1-4 step down/left/
 * right/up, 12/13 one step forward/backward, 15 wait, 16-19 turn down/left/
 * right/up, 20-22 turn relatives, 27/28 switch on/off, 29/30 speed/frequency,
 * 35-38 direction fix / through, 44 play SE, 45 script.</p>
 */
public final class MoveRoute {

    /** One move command of the route. */
    public static final class Command {
        public int code;
        public JsonValue parameters;

        public int intParam(int index, int fallback) {
            JsonValue value = parameters == null || !parameters.isArray() ? null : parameters.get(index);
            return value == null ? fallback : value.asInt();
        }

        /** R6.18: the sheet name of "Change Graphic" (41). */
        public String stringParam(int index, String fallback) {
            JsonValue value = parameters == null || !parameters.isArray() ? null : parameters.get(index);
            return value == null || !value.isString() ? fallback : value.asString();
        }

        /**
         * Audio name of a "play SE" command. The Builder writes this parameter
         * as a Ruby inspect string
         * ({@code "@{class=RPG::AudioFile; ...; name=Door enter; pitch=100}"}),
         * so the name is extracted from that text.
         */
        public String audioName() {
            JsonValue value = parameters == null || !parameters.isArray() ? null : parameters.get(0);
            if (value == null) {
                return null;
            }
            if (value.isObject()) {
                return value.getString("name", null);
            }
            if (!value.isString()) {
                return null;
            }
            String text = value.asString();
            int start = text.indexOf("name=");
            if (start < 0) {
                return null;
            }
            int end = text.indexOf(';', start);
            return (end < 0 ? text.substring(start + 5) : text.substring(start + 5, end)).trim();
        }
    }

    public final Array<Command> commands = new Array<>();
    public boolean repeat;
    public boolean skippable = true;

    public int size() {
        return commands.size;
    }

    /** Parses the object of a 209 parameter: {@code {list, repeat, skippable}}. */
    public static MoveRoute parse(JsonValue node) {
        MoveRoute route = new MoveRoute();
        if (node == null || !node.isObject()) {
            return route;
        }
        route.repeat = node.getBoolean("repeat", false);
        route.skippable = node.getBoolean("skippable", true);
        // Set Move Route (209) carries "list"; an event page's own move route
        // (RPG::Event::Page#move_route) carries the same data as "commands".
        JsonValue list = node.get("list");
        if (list == null || !list.isArray()) {
            list = node.get("commands");
        }
        if (list != null && list.isArray()) {
            for (JsonValue entry = list.child; entry != null; entry = entry.next) {
                Command command = new Command();
                command.code = entry.getInt("code", 0);
                command.parameters = entry.get("parameters");
                route.commands.add(command);
            }
        }
        return route;
    }
}
