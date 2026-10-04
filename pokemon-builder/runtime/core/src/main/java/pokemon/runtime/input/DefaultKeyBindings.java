package pokemon.runtime.input;

import com.badlogic.gdx.Input;

import java.util.EnumMap;
import java.util.Map;

/**
 * Windows default key bindings (project3 section 15): the single place where
 * physical keys become logical actions.
 *
 * <pre>
 *   Arrows / WASD -> movement
 *   Z / Enter     -> CONFIRM
 *   X / Escape    -> CANCEL
 *   Shift         -> RUN
 *   X / Menu      -> MENU
 * </pre>
 *
 * Key codes are libGDX {@link Input.Keys} constants; the map itself is plain
 * data so it stays testable without a backend (section 61).
 */
public final class DefaultKeyBindings {

    private final Map<GameAction, int[]> keys = new EnumMap<>(GameAction.class);

    public DefaultKeyBindings() {
        bind(GameAction.UP, Input.Keys.UP, Input.Keys.W);
        bind(GameAction.DOWN, Input.Keys.DOWN, Input.Keys.S);
        bind(GameAction.LEFT, Input.Keys.LEFT, Input.Keys.A);
        bind(GameAction.RIGHT, Input.Keys.RIGHT, Input.Keys.D);
        bind(GameAction.CONFIRM, Input.Keys.Z, Input.Keys.ENTER);
        bind(GameAction.CANCEL, Input.Keys.X, Input.Keys.ESCAPE);
        bind(GameAction.RUN, Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT);
        bind(GameAction.MENU, Input.Keys.X, Input.Keys.MENU);
    }

    public void bind(GameAction action, int... keyCodes) {
        keys.put(action, keyCodes.clone());
    }

    public int[] keysFor(GameAction action) {
        int[] codes = keys.get(action);
        return codes == null ? new int[0] : codes;
    }
}
