package pokemon.runtime.input;

/**
 * Raw key state provider (project3 section 14): the lwjgl3 backend answers
 * from libGDX, headless tests answer from a stub. The mapping lives in
 * DefaultKeyBindings, so nothing else knows about physical keys.
 */
public interface KeyStateSource {

    /** True while the physical key is held down. */
    boolean isDown(int keyCode);
}