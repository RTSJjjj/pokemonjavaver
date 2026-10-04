package pokemon.runtime.event;

/** Event interpreter states (project3 section 17). */
public enum InterpreterState {
    RUNNING,
    WAIT_TIME,
    WAIT_INPUT,
    WAIT_MESSAGE,
    WAIT_MOVEMENT,
    WAIT_TRANSFER,
    FINISHED
}
