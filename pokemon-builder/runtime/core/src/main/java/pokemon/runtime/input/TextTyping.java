package pokemon.runtime.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Keyboard text for the name boxes. The game polls actions through {@link InputManager}
 * (Z/X/C are buttons there), so typed characters need their own path: while a text
 * box is open this adapter is the libGDX input processor and queues what the OS
 * delivers - letters, digits and whatever the player's input method commits (pinyin
 * IME on Windows arrives through GLFW's character callback as finished Chinese text) -
 * plus the editing keys. {@link #end} puts the previous processor back.
 *
 * <p>libGDX's LWJGL3 backend has no working {@code getTextInput}, so the desktop
 * build types in-game; Android has no physical keyboard, so {@link #useDialog}
 * tells the caller to use the system text dialog there instead.</p>
 */
public final class TextTyping extends InputAdapter {

    /** One queued event: a typed character, or an editing key. */
    public static final class Event {
        public enum Kind { CHAR, ENTER, ESCAPE, BACKSPACE, DELETE, LEFT, RIGHT, HOME, END }

        public final Kind kind;
        public final int codePoint;

        Event(Kind kind, int codePoint) {
            this.kind = kind;
            this.codePoint = codePoint;
        }
    }

    private final List<Event> queue = new ArrayList<>();
    private InputProcessor previous;
    private boolean active;
    private char highSurrogate;

    /** True on a platform with no usable physical keyboard path (Android). */
    public static boolean useDialog() {
        return Gdx.app != null && Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android;
    }

    public void begin() {
        if (active || Gdx.input == null) {
            return;
        }
        previous = Gdx.input.getInputProcessor();
        Gdx.input.setInputProcessor(this);
        queue.clear();
        active = true;
    }

    public void end() {
        if (!active) {
            return;
        }
        active = false;
        if (Gdx.input != null) {
            Gdx.input.setInputProcessor(previous);
        }
        previous = null;
        queue.clear();
    }

    public boolean active() {
        return active;
    }

    /** Returns and clears everything typed since the last call. */
    public List<Event> drain() {
        if (queue.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<Event> out = new ArrayList<>(queue);
        queue.clear();
        return out;
    }

    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.ENTER:
            case Input.Keys.NUMPAD_ENTER:
                queue.add(new Event(Event.Kind.ENTER, 0));
                return true;
            case Input.Keys.ESCAPE:
                queue.add(new Event(Event.Kind.ESCAPE, 0));
                return true;
            case Input.Keys.BACKSPACE:
                queue.add(new Event(Event.Kind.BACKSPACE, 0));
                return true;
            case Input.Keys.FORWARD_DEL:
                queue.add(new Event(Event.Kind.DELETE, 0));
                return true;
            case Input.Keys.LEFT:
                queue.add(new Event(Event.Kind.LEFT, 0));
                return true;
            case Input.Keys.RIGHT:
                queue.add(new Event(Event.Kind.RIGHT, 0));
                return true;
            case Input.Keys.HOME:
                queue.add(new Event(Event.Kind.HOME, 0));
                return true;
            case Input.Keys.END:
                queue.add(new Event(Event.Kind.END, 0));
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean keyTyped(char character) {
        // Backspace / Delete / Enter come through keyDown; their control characters are dropped.
        if (Character.isHighSurrogate(character)) {
            highSurrogate = character;
            return true;
        }
        int codePoint = character;
        if (Character.isLowSurrogate(character)) {
            if (highSurrogate == 0) {
                return true;
            }
            codePoint = Character.toCodePoint(highSurrogate, character);
        }
        highSurrogate = 0;
        if (codePoint < 0x20 || codePoint == 0x7F) {
            return true;
        }
        queue.add(new Event(Event.Kind.CHAR, codePoint));
        return true;
    }
}
