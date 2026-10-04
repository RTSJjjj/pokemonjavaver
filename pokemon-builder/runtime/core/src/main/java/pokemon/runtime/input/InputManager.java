package pokemon.runtime.input;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Input manager (project3 section 14): the only runtime component that knows
 * whether an action is down, or was pressed this frame.
 *
 * <p>Pure Java on purpose: it must be testable without a graphics context
 * (project3 section 61). Platform sources call {@link #press} /
 * {@link #release}; the game polls {@link #isDown} / {@link #wasPressed}.</p>
 */
public final class InputManager {

    private final Map<GameAction, Boolean> down = new EnumMap<>(GameAction.class);
    private final Set<GameAction> pressedThisFrame = EnumSet.noneOf(GameAction.class);

    public InputManager() {
        for (GameAction action : GameAction.values()) {
            down.put(action, Boolean.FALSE);
        }
    }

    public void press(GameAction action) {
        if (!down.get(action)) {
            pressedThisFrame.add(action);
        }
        down.put(action, Boolean.TRUE);
    }

    public void release(GameAction action) {
        down.put(action, Boolean.FALSE);
    }

    /** Syncs the held state of one action (the frame sampler calls this). */
    public void set(GameAction action, boolean held) {
        if (held) {
            press(action);
        } else {
            release(action);
        }
    }

    public boolean isDown(GameAction action) {
        return down.get(action);
    }

    /** True once per physical press; the edge is cleared in {@link #endFrame}. */
    public boolean wasPressed(GameAction action) {
        return pressedThisFrame.contains(action);
    }

    public void beginFrame() {
        pressedThisFrame.clear();
    }

    public void endFrame() {
        pressedThisFrame.clear();
    }
}