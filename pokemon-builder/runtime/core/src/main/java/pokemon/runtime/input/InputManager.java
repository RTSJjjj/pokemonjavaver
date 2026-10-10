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

    /** RGSS runs at 40 frames per second; Input.repeat? counts those frames. */
    private static final double RGSS_FRAME_NANOS = 1_000_000_000d / 40d;
    private final Map<GameAction, Long> downSince = new EnumMap<>(GameAction.class);
    private final Map<GameAction, Integer> countAtLastFrame = new EnumMap<>(GameAction.class);
    private final Set<GameAction> repeatedThisFrame = EnumSet.noneOf(GameAction.class);
    private final Set<GameAction> repeatChecked = EnumSet.noneOf(GameAction.class);
    private java.util.function.LongSupplier clock = System::nanoTime;
    /** Actions pressed since a frame that has not counted its first (repeat) frame yet. */
    private final Set<GameAction> firstFramePending = EnumSet.noneOf(GameAction.class);

    public InputManager() {
        for (GameAction action : GameAction.values()) {
            down.put(action, Boolean.FALSE);
        }
    }

    public void press(GameAction action) {
        if (!down.get(action)) {
            pressedThisFrame.add(action);
            downSince.put(action, clock.getAsLong());
            countAtLastFrame.put(action, 0);
            firstFramePending.add(action);
        }
        down.put(action, Boolean.TRUE);
    }

    public void release(GameAction action) {
        down.put(action, Boolean.FALSE);
        downSince.remove(action);
        countAtLastFrame.remove(action);
        firstFramePending.remove(action);
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

    /**
     * RGSS {@code Input.repeat?} (PSystem_Controls:223-227): true on the first
     * frame a button is down, and from then on on every even frame once it has
     * been held for more than half a second (frame_rate/2 = 20 frames at 40fps).
     * The frame count is derived from the held time, so it keeps the RGSS
     * rhythm whatever the render rate is; if several 40fps frames elapsed
     * since the last query, any of them being a repeat frame counts.
     */
    public boolean wasRepeated(GameAction action) {
        if (!down.get(action)) {
            return false;
        }
        if (repeatChecked.add(action)) {
            Long since = downSince.get(action);
            int previous = countAtLastFrame.getOrDefault(action, 0);
            int count = since == null ? previous + 1
                    : (int) ((clock.getAsLong() - since) / RGSS_FRAME_NANOS) + 1;
            boolean repeated = false;
            for (int frame = previous + 1; frame <= Math.max(count, previous + 1); frame++) {
                if (frame == 1 || (frame > 20 && (frame & 1) == 0)) {
                    repeated = true;
                    break;
                }
            }
            countAtLastFrame.put(action, Math.max(count, previous + 1));
            if (repeated) {
                repeatedThisFrame.add(action);
            }
        }
        return repeatedThisFrame.contains(action);
    }

    /** Test hook: the clock behind {@link #wasRepeated}. */
    void clock(java.util.function.LongSupplier value) {
        this.clock = value;
    }

    /**
     * Drops every edge of this frame without touching the held state. A scene
     * that consumed a press (the pause menu closing on it) calls this so the
     * map does not trigger on the same press later in the same frame - RMXP
     * runs {@code Input.update} per scene, so Scene_Map never sees Scene_Item's
     * confirm.
     */
    public void consumePressed() {
        pressedThisFrame.clear();
    }

    /**
     * The frame a button went down is RGSS repeat frame 1 whether or not anyone asked {@link #wasRepeated} that frame
     * (a screen that tests {@code wasPressed(x) || wasRepeated(x)} short-circuits it). Without this, the next frame
     * counted frame 1 again and a tap moved a cursor twice.
     */
    private void countFirstFrames() {
        for (GameAction action : firstFramePending) {
            if (down.get(action)) {
                countAtLastFrame.merge(action, 1, Math::max);
            }
        }
        firstFramePending.clear();
    }

    public void beginFrame() {
        countFirstFrames();
        pressedThisFrame.clear();
        repeatedThisFrame.clear();
        repeatChecked.clear();
    }

    public void endFrame() {
        countFirstFrames();
        pressedThisFrame.clear();
        repeatedThisFrame.clear();
        repeatChecked.clear();
    }
}