package pokemon.runtime.ui.menu;

import pokemon.runtime.audio.AudioManager;

/**
 * L1: the sounds the project's menus play (taken from Modular Pause Menu /
 * PScreen_Save): the GUI set exists in {@code generated/audio/}. Missing files
 * degrade to silence through the AudioManager.
 */
public final class MenuSe {

    public static final String OPEN = "GUI menu open";
    public static final String CLOSE = "GUI menu close";
    /** The modular pause menu plays SE_Select1 at volume 75 on every move. */
    public static final String CURSOR = "SE_Select1";
    public static final int CURSOR_VOLUME = 75;
    public static final String DECISION = "GUI sel decision";
    public static final String BUZZER = "GUI sel buzzer";
    public static final String SAVE = "GUI save choice";

    private MenuSe() {
    }

    public static void open(AudioManager audio) {
        play(audio, OPEN, 100);
    }

    public static void close(AudioManager audio) {
        play(audio, CLOSE, 100);
    }

    public static void cursor(AudioManager audio) {
        play(audio, CURSOR, CURSOR_VOLUME);
    }

    public static void decision(AudioManager audio) {
        play(audio, DECISION, 100);
    }

    public static void buzzer(AudioManager audio) {
        play(audio, BUZZER, 100);
    }

    public static void save(AudioManager audio) {
        play(audio, SAVE, 100);
    }

    private static void play(AudioManager audio, String name, int volume) {
        if (audio != null) {
            audio.playSe(name, volume, 100);
        }
    }
}
