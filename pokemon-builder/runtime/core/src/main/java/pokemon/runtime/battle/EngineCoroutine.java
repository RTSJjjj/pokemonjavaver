package pokemon.runtime.battle;

import java.util.concurrent.Semaphore;

/**
 * One run of battle-engine code that can stop at a {@code @scene.*} call which has to wait for the player
 * or an animation (the party screen, a yes/no question, the recall and send-out animations) and carry on
 * from the same place once the battle screen has played it.
 *
 * <p>The plugin's engine is ordinary blocking Ruby: {@code pbSwitchInBetween} simply returns the party index the
 * player picked, in the middle of a move's effect. The engine code here is transcribed the same way, so it
 * runs on its own thread and the thread stops inside the scene call. Only one of the two threads ever runs:
 * {@link #resume()} hands control to the engine until it ends or asks for the screen, and
 * {@link #call(Battle.SceneCall)} hands it back; the battle's state is therefore never touched by two threads
 * at once.</p>
 */
final class EngineCoroutine {

    /** Unwinds the engine thread when the battle screen is closed while the engine waits. */
    private static final class Abort extends Error {
        private static final long serialVersionUID = 1L;

        Abort() {
            super(null, null, false, false);
        }
    }

    private final Semaphore toEngine = new Semaphore(0);
    private final Semaphore toScreen = new Semaphore(0);
    private final Thread thread;
    private volatile boolean finished;
    private volatile boolean aborted;
    private volatile Throwable failure;
    private volatile Battle.SceneCall pending;

    EngineCoroutine(Runnable body) {
        thread = new Thread(null, () -> {
            toEngine.acquireUninterruptibly();
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
        }, "battle-engine", 32L * 1024L * 1024L);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Runs the engine until it ends or asks the screen for something ({@link #pending()}).
     *
     * @return true when the engine code has ended
     */
    boolean resume() {
        pending = null;
        toEngine.release();
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

    /** The scene call the engine is waiting on, or null. */
    Battle.SceneCall pending() {
        return pending;
    }

    /** Whether the calling thread is the engine's. */
    boolean onEngineThread() {
        return Thread.currentThread() == thread;
    }

    /** Engine thread: hands {@code call} to the screen and waits until it has been played. */
    Object call(Battle.SceneCall call) {
        pending = call;
        toScreen.release();
        toEngine.acquireUninterruptibly();
        if (aborted) {
            throw new Abort();
        }
        return call.result;
    }

    /** Lets an engine that waits on the screen unwind (the battle screen closed). */
    void abort() {
        aborted = true;
        toEngine.release();
    }
}
