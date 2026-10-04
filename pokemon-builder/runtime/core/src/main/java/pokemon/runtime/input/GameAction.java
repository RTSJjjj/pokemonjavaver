package pokemon.runtime.input;

/**
 * Logical game actions (project3 section 14). The runtime never looks at raw
 * keys: platform input sources translate keys / touches into these actions.
 */
public enum GameAction {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    CONFIRM,
    CANCEL,
    MENU,
    RUN
}