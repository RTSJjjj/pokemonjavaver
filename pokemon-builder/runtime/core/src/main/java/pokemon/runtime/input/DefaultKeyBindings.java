package pokemon.runtime.input;

import com.badlogic.gdx.Input;

import java.util.EnumMap;
import java.util.Map;

/**
 * Windows default key bindings (project3 section 15): the single place where
 * physical keys become logical actions.
 *
 * <p>The table below mirrors the project's own override of {@code
 * Input.buttonToKey} (PSystem_Controls), so the desktop build uses exactly the
 * original keys:</p>
 *
 * <pre>
 *   Arrows            -> movement        (the original does not use WASD; W/Y/Z
 *                                         are Input::A and A/S are Input::L/R)
 *   C / Enter / Space -> CONFIRM         (Input::C)
 *   X / Escape        -> CANCEL / MENU   (Input::B: cancels in menus, opens the
 *                                         pause menu on the map - Scene_Map:187)
 *   Z / W / Y / Shift -> RUN / SPECIAL   (Input::A: hold to run /
 *                                         Scene_Map:196 toggles, and the battle
 *                                         fight menu's Mega Evolution toggle)
 * </pre>
 *
 * Key codes are libGDX {@link Input.Keys} constants; the map itself is plain
 * data so it stays testable without a backend (section 61).
 */
public final class DefaultKeyBindings {

    private final Map<GameAction, int[]> keys = new EnumMap<>(GameAction.class);

    public DefaultKeyBindings() {
        // Input::DOWN/LEFT/RIGHT/UP: arrow keys only (0x28/0x25/0x27/0x26).
        bind(GameAction.UP, Input.Keys.UP);
        bind(GameAction.DOWN, Input.Keys.DOWN);
        bind(GameAction.LEFT, Input.Keys.LEFT);
        bind(GameAction.RIGHT, Input.Keys.RIGHT);
        // Input::C = C, Enter, Space (0x43, 0x0D, 0x20).
        bind(GameAction.CONFIRM, Input.Keys.C, Input.Keys.ENTER, Input.Keys.SPACE);
        // Input::B = X, Esc (0x58, 0x1B): cancel, and the pause menu on the map.
        bind(GameAction.CANCEL, Input.Keys.X, Input.Keys.ESCAPE);
        bind(GameAction.MENU, Input.Keys.X, Input.Keys.ESCAPE);
        // Input::A = Z, W, Y, Shift (0x5A, 0x57, 0x59, 0x10).
        int[] inputA = { Input.Keys.Z, Input.Keys.W, Input.Keys.Y,
                Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT };
        bind(GameAction.RUN, inputA);
        bind(GameAction.SPECIAL, inputA);
        // Input::L = A, Q, Page Up (0x41, 0x51, 0x21).
        bind(GameAction.SHOULDER_LEFT, Input.Keys.A, Input.Keys.Q, Input.Keys.PAGE_UP);
        // Input::R = S, Page Down (0x53, 0x22).
        bind(GameAction.SHOULDER_RIGHT, Input.Keys.S, Input.Keys.PAGE_DOWN);
        // Input::F5 = F, F5, Tab (0x46, 0x74, 0x09).
        bind(GameAction.F5, Input.Keys.F, Input.Keys.F5, Input.Keys.TAB);
        // 296_Follower_Config:44 TOGGLEFOLLOWERKEY = :CTRL.
        bind(GameAction.TOGGLE_FOLLOWER, Input.Keys.CONTROL_LEFT, Input.Keys.CONTROL_RIGHT);
        // 338_004_ESMM_Overwrite:219-228: M (0x4D), =+ (0xBB), -_ (0xBD).
        bind(GameAction.MAP_KEY, Input.Keys.M);
        bind(GameAction.ZOOM_IN, Input.Keys.EQUALS, Input.Keys.PLUS);
        bind(GameAction.ZOOM_OUT, Input.Keys.MINUS);
    }

    public void bind(GameAction action, int... keyCodes) {
        keys.put(action, keyCodes.clone());
    }

    public int[] keysFor(GameAction action) {
        int[] codes = keys.get(action);
        return codes == null ? new int[0] : codes;
    }
}
