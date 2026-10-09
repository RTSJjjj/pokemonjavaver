package pokemon.runtime.audio;

/**
 * 058_Audio_Play:235-275 {@code pbPlayCursorSE} / {@code pbPlayDecisionSE} / {@code pbPlayCancelSE} /
 * {@code pbPlayBuzzerSE}: the interface sounds the message and choice windows play. The project's
 * {@code $data_system} sounds are all blank (Data/System.rxdata), so each falls through to the
 * {@code Audio/SE/GUI sel ...} file at volume 80.
 */
public final class UiSounds {
    private UiSounds() { }

    public static void cursor(AudioManager audio) {
        play(audio, "GUI sel cursor");
    }

    public static void decision(AudioManager audio) {
        play(audio, "GUI sel decision");
    }

    public static void cancel(AudioManager audio) {
        play(audio, "GUI sel cancel");
    }

    public static void buzzer(AudioManager audio) {
        play(audio, "GUI sel buzzer");
    }

    /** {@code pbSEPlay(name)} at full volume: the plugin's own named sounds (a missing file plays nothing). */
    public static void named(AudioManager audio, String name) {
        if (audio != null) {
            audio.playSe(name, 100, 100);
        }
    }

    private static void play(AudioManager audio, String name) {
        if (audio != null) {
            audio.playSe(name, 80, 100);
        }
    }
}
