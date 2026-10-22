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
     * Moves the cursor to {@code newIndex} without consuming the command there:
     * {@code @index = temp_index} of {@code command_119} (048_Interpreter:801),
     * after which the interpreter's own {@code @index += 1} continues behind
     * the label.
     */
    public void jumpTo(int newIndex) {
        index = newIndex;
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

    /**
     * Stores a conditional branch result, 048_Interpreter:660-667: a true
     * result is deleted right away (the branch is entered), a false one stays
     * until the matching Else consumes it.
     */
    public void branchResult(int indent, boolean value) {
        if (value) {
            branchResult.remove(indent);
        } else {
            branchResult.put(indent, Boolean.FALSE);
        }
    }

    /**
     * Else (411), 048_Interpreter:675-685: enters only when a false result is
     * pending at this indent and deletes it, so reaching the same Else again
     * (e.g. after a Jump to Label back into the true branch) skips it.
     */
    public boolean consumeElse(int indent) {
        if (branchResult.get(indent) == Boolean.FALSE) {
            branchResult.remove(indent);
            return true;
        }
        return false;
    }
}
