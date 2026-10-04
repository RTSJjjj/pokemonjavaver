package pokemon.runtime.android.legacy;

import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.input.InputManager;

import java.io.File;

/**
 * Reuse contract of the legacy module (project3 section 50).
 *
 * <p>Stage 2 does not implement the API 14 / ARMv7 backend. What this class
 * fixes is what the future backend can rely on: the Builder runtime data and
 * the logical input layer are platform free, so the legacy backend will load
 * the same {@code generated/} output and drive the same {@link InputManager}
 * the modern backends use. The methods below are the compile-checked proof -
 * if core ever grows a desktop or modern-Android dependency, this class stops
 * compiling and the contract breaks loudly.</p>
 */
public final class LegacyRuntime {

    private LegacyRuntime() {
    }

    /**
     * Loads the same Builder output every backend consumes (Runtime Data
     * reuse), resolved by the same {@code RuntimeDataLocator} rules.
     */
    public static GameDatabase loadData(String runtimeDataRoot, File baseDir) {
        return GameDatabase.load(runtimeDataRoot, baseDir);
    }

    /**
     * Creates the pure-Java action state (GameAction reuse) that the future
     * legacy touch/key layer will feed.
     */
    public static InputManager newInput() {
        return new InputManager();
    }
}
