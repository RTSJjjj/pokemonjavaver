package pokemon.runtime.state;

/**
 * Monotonically increasing change counter shared by the R5 containers of
 * {@link GameState} (project3 section 30). Renderers compare this number
 * instead of diffing whole switch / variable tables every frame.
 */
public final class StateVersion {

    private long value;

    public long value() {
        return value;
    }

    void bump() {
        value++;
    }
}
