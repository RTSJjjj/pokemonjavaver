package pokemon.runtime.event;

/** Event interpreter states (project3 section 17). */
public enum InterpreterState {
    RUNNING,
    WAIT_TIME,
    WAIT_INPUT,
    WAIT_MESSAGE,
    WAIT_MOVEMENT,
    WAIT_TRANSFER,
    /** P3: a berry plant interaction is playing (pbBerryPlant / pbPickBerry). */
    WAIT_BERRY,
    FINISHED
}
