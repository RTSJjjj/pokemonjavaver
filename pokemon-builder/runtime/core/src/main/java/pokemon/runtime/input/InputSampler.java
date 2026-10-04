package pokemon.runtime.input;

/**
 * Samples a KeyStateSource through the key bindings into the InputManager
 * (project3 section 14): one frame snapshot, one press/release edge detection.
 * Pure Java: no libGDX, no backend.
 */
public final class InputSampler {

    private final DefaultKeyBindings bindings;

    public InputSampler(DefaultKeyBindings bindings) {
        this.bindings = bindings;
    }

    public void sample(KeyStateSource source, InputManager input) {
        for (GameAction action : GameAction.values()) {
            boolean down = false;
            for (int key : bindings.keysFor(action)) {
                if (source.isDown(key)) {
                    down = true;
                    break;
                }
            }
            input.set(action, down);
        }
    }
}