package pokemon.runtime.event;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import pokemon.runtime.data.EventCommand;

/**
 * Cursor over one command list: an event page or one common event (project3
 * section 16). Commands with a deeper indent belong to the block opened by the
 * command at the current indent, which is how RMXP skips branches.
 */
public final class EventProgram {

    private final Array<EventCommand> commands;
    private final IntMap<Boolean> branchResult = new IntMap<>();
    private int index;

    public EventProgram(Array<EventCommand> commands) {
        this.commands = commands;
    }

    public Array<EventCommand> commands() {
        return commands;
    }

    public boolean finished() {
        return index >= commands.size;
    }

    public EventCommand current() {
        return finished() ? null : commands.get(index);
    }

    public int index() {
        return index;
    }

    public int indent() {
        EventCommand command = current();
        return command == null ? 0 : command.indent;
    }

    public void advance() {
        index++;
    }

    /**
     * Skips this command and every following command of its block: RMXP's
     * {@code command_skip}, used by conditional branches and choice blocks.
     */
    public void skipBlock() {
        int depth = indent();
        advance();
        while (!finished() && commands.get(index).indent > depth) {
            index++;
        }
    }

    /** Ends the current list (RMXP "Exit Event Processing" / end of a call). */
    public void finish() {
        index = commands.size;
    }

    public void branchResult(int indent, boolean value) {
        branchResult.put(indent, value);
    }

    /** Result of the conditional branch that opened this indent (false if none). */
    public boolean branchResult(int indent) {
        return branchResult.get(indent, Boolean.FALSE);
    }
}
