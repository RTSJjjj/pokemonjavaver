package pokemon.runtime.audio;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.data.AudioManifestData;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B3 / T4: {@code AudioManager.fadeBgm(float)} - the runtime end of
 * {@code pbBGMFade(1.0)} ({@code PokeBattle_Scene:300}) -> {@code pbBGMStop(x)}
 * (Audio_Play:71) -> {@code Game_System#bgm_fade(time)} (Game_System:111-115):
 * the running BGM fades over {@code time} seconds and then stops, and a
 * non-positive time takes {@code pbBGMStop}'s {@code bgm_stop} branch
 * (Audio_Play:75/78-80).
 */
class AudioManagerFadeBgmTest {

    @TempDir
    Path tempDir;

    private final List<String> calls = new ArrayList<>();
    private FakeMusic music;
    private AudioManager manager;

    /** Records what the manager asks the audio device to do. */
    private final class FakeMusic implements Music {
        private final String id;
        private boolean playing;
        private float volume = 1f;
        private boolean looping;
        private float position;
        private OnCompletionListener completion;

        FakeMusic(String id) {
            this.id = id;
        }

        @Override public void play() { playing = true; calls.add(id + ":play"); }
        @Override public void pause() { playing = false; calls.add(id + ":pause"); }
        @Override public void stop() { playing = false; calls.add(id + ":stop"); }
        @Override public boolean isPlaying() { return playing; }
        @Override public void setLooping(boolean value) { looping = value; }
        @Override public boolean isLooping() { return looping; }
        @Override public void setVolume(float value) { volume = value; calls.add(id + ":volume=" + value); }
        @Override public float getVolume() { return volume; }
        @Override public void setPan(float pan, float value) { }
        @Override public void setPosition(float value) { position = value; }
        @Override public float getPosition() { return position; }
        @Override public void dispose() { calls.add(id + ":dispose"); }
        @Override public void setOnCompletionListener(OnCompletionListener listener) { completion = listener; }
    }

    private final class FakeSound implements Sound {
        @Override public long play() { return 1L; }
        @Override public long play(float volume) { return 1L; }
        @Override public long play(float volume, float pitch, float pan) { return 1L; }
        @Override public long loop() { return 1L; }
        @Override public long loop(float volume) { return 1L; }
        @Override public long loop(float volume, float pitch, float pan) { return 1L; }
        @Override public void stop() { }
        @Override public void stop(long soundId) { }
        @Override public void pause() { }
        @Override public void pause(long soundId) { }
        @Override public void resume() { }
        @Override public void resume(long soundId) { }
        @Override public void dispose() { }
        @Override public void setPitch(long soundId, float pitch) { }
        @Override public void setVolume(long soundId, float volume) { }
        @Override public void setPan(long soundId, float pan, float volume) { }
        @Override public void setLooping(long soundId, boolean looping) { }
    }

    /** A manifest with one plain (untagged) BGM. */
    private void attachBgm() throws Exception {
        Path file = tempDir.resolve("audio").resolve("BGM").resolve("theme.ogg");
        Files.createDirectories(file.getParent());
        Files.write(file, "fake".getBytes(StandardCharsets.UTF_8));
        AudioManifestData manifest = new AudioManifestData();
        manifest.loaded = true;
        manifest.preset = "standard";
        AudioManifestData.Entry entry = new AudioManifestData.Entry();
        entry.type = "BGM";
        entry.file = "audio/BGM/theme.ogg";
        manifest.entries.put("theme", entry);
        manager = new AudioManager();
        manager.attach(manifest, tempDir.toFile());
        manager.setBackend(new AudioManager.Backend() {
            @Override public Music newMusic(File newFile) {
                music = new FakeMusic(newFile.getName());
                return music;
            }
            @Override public Sound newSound(File newFile) {
                return new FakeSound();
            }
        });
        manager.playBgm("theme", 100, 100);
    }

    @Test
    @DisplayName("fadeBgm fades over the given seconds and then stops the BGM (:111-115)")
    void fadeCountsDownThenStops() throws Exception {
        attachBgm();
        assertNotNull(music);
        assertTrue(music.playing);
        calls.clear();

        manager.fadeBgm(1.0f);
        assertNull(manager.currentBgmId(), "Game_System:113 @playing_bgm = nil");

        manager.update(0.25f);
        assertEquals(0.75f, music.getVolume(), 1e-3f,
                "Audio.bgm_fade: fadingVolume * (fadeRemaining/fadeDuration)");
        assertTrue(music.playing, "the stream keeps playing while it fades");

        manager.update(0.5f);
        assertEquals(0.25f, music.getVolume(), 1e-3f);
        assertTrue(music.playing);

        manager.update(0.25f);                       // the fade runs out -> stop
        assertFalse(music.playing);
        assertEquals("theme.ogg:stop", calls.get(calls.size() - 1));

        calls.clear();
        manager.update(1f);                          // nothing left to fade
        assertTrue(calls.isEmpty(), "a finished fade is not advanced again");
    }

    @Test
    @DisplayName("a non-positive fade is pbBGMStop's own bgm_stop branch (:75/:78-80)")
    void nonPositiveFadeStopsAtOnce() throws Exception {
        attachBgm();
        calls.clear();

        manager.fadeBgm(0f);
        assertFalse(music.playing);
        assertNull(manager.currentBgmId());
        assertTrue(calls.contains("theme.ogg:stop"), "stopBgm (Game_System:105-109)");
        assertEquals(1, calls.stream().filter(call -> call.endsWith(":stop")).count());

        calls.clear();
        manager.playBgm("theme", 100, 100);          // restart, then a negative time
        calls.clear();
        manager.fadeBgm(-2f);
        assertFalse(music.playing);
        assertTrue(calls.contains("theme.ogg:stop"));
    }

    @Test
    @DisplayName("fading with no BGM playing does nothing")
    void fadeWithoutBgmDoesNothing() {
        manager = new AudioManager();                // no manifest, nothing playing
        manager.fadeBgm(1f);
        manager.update(1f);
        assertTrue(calls.isEmpty());
        assertNull(manager.currentBgmId());
    }

    @Test
    @DisplayName("a track started after a fade plays normally")
    void playAfterFade() throws Exception {
        attachBgm();
        manager.fadeBgm(0.5f);
        manager.update(0.5f);                        // the fade ends (stop)
        calls.clear();

        manager.playBgm("theme", 100, 100);
        assertTrue(music.playing, "a new fade state does not block the next play");
        assertEquals("theme", manager.currentBgmId());
        assertEquals(1f, music.getVolume(), 1e-3f);
    }
}
