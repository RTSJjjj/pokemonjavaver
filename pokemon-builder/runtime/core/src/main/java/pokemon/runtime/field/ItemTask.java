package pokemon.runtime.field;

import java.util.function.Consumer;

/**
 * An item handler running on a {@link BlockingTask}: the screen calls {@link #resume()}, reads {@link #pending()} and
 * answers it with {@link #answer(Object)}, until {@link #resume()} returns true.
 */
public final class ItemTask {
    private final BlockingTask task;

    private ItemTask(BlockingTask task) {
        this.task = task;
    }

    /** Starts {@code body} (not yet running: the first {@link #resume()} runs it up to its first screen call). */
    public static ItemTask start(Consumer<ItemScene> body) {
        BlockingTask[] holder = new BlockingTask[1];
        TaskItemScene scene = new TaskItemScene(request -> holder[0].call(request));
        holder[0] = new BlockingTask(() -> body.accept(scene));
        return new ItemTask(holder[0]);
    }

    /** @return true when the handler has ended */
    public boolean resume() {
        return task.resume();
    }

    public TaskItemScene.Request pending() {
        return (TaskItemScene.Request) task.pending();
    }

    public void answer(Object value) {
        task.answer(value);
    }

    public boolean finished() {
        return task.finished();
    }

    public void abort() {
        task.abort();
    }
}
