package pokemon.runtime.field;

import java.util.concurrent.Semaphore;

/**
 * One run of plugin code written as ordinary blocking Ruby (an item handler that calls {@code scene.pbDisplay},
 * {@code pbChooseMove} ...) that has to wait for the player. It runs on its own thread and stops inside each screen
 * call; only one of the two threads ever runs, so the game state is never touched by both at once (the same device as
 * the battle engine's {@code EngineCoroutine}).
 *
 * <p>Screen side: {@link #resume()} until it returns true (the body ended); after each stop {@link #pending()} is the
 * request to show, and {@link #answer(Object)} stores its result before the next {@code resume}. Task side:
 * {@link #call(Object)} hands the request over and returns the answer.</p>
 */
public final class BlockingTask {

    /** Unwinds the task thread when the screen is closed while the task waits. */
    private static final class Abort extends Error {
        private static final long serialVersionUID = 1L;

        Abort() {
            super(null, null, false, false);
        }
    }

    private final Semaphore toTask = new Semaphore(0);
    private final Semaphore toScreen = new Semaphore(0);
    private final Thread thread;
    private volatile boolean finished;
    private volatile boolean aborted;
    private volatile Throwable failure;
    private volatile Object pending;
    private volatile Object answer;

    public BlockingTask(Runnable body) {
        thread = new Thread(null, () -> {
            toTask.acquireUninterruptibly();
            try {
                if (!aborted) {
                    body.run();
                }
            } catch (Abort ignored) {
                // the screen is gone
            } catch (Throwable t) {
                failure = t;
            }
            finished = true;
            toScreen.release();
        }, "item-task", 16L * 1024L * 1024L);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Runs the task until it ends or asks the screen for something ({@link #pending()}).
     *
     * @return true when the task has ended
     */
    public boolean resume() {
        pending = null;
        toTask.release();
        toScreen.acquireUninterruptibly();
        Throwable error = failure;
        if (error != null) {
            failure = null;
            if (error instanceof RuntimeException) throw (RuntimeException) error;
            if (error instanceof Error) throw (Error) error;
            throw new IllegalStateException(error);
        }
        return finished;
    }

    /** The request the task is waiting on, or null. */
    public Object pending() {
        return pending;
    }

    /** Screen side: the result {@link #call(Object)} returns after the next {@link #resume()}. */
    public void answer(Object value) {
        this.answer = value;
    }

    /** Task side: hands {@code request} to the screen and waits for the answer. */
    public Object call(Object request) {
        pending = request;
        answer = null;
        toScreen.release();
        toTask.acquireUninterruptibly();
        if (aborted) {
            throw new Abort();
        }
        return answer;
    }

    public boolean finished() {
        return finished;
    }

    /** Lets a task that waits on the screen unwind (the screen closed). */
    public void abort() {
        aborted = true;
        toTask.release();
    }
}
