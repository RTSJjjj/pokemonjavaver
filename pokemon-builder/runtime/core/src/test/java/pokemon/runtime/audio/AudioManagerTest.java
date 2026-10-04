package pokemon.runtime.audio;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.AudioManifestData;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R9 tests: manifest lookup, streamed BGM/BGS, preloaded ME/SE, caching and
 * dispose - all with a fake backend, so no audio device is needed.
 */
class AudioManagerTest {

    private final List<String> calls = new ArrayList<>();
    private final Map<String, FakeMusic> fakeMusics = new HashMap<>();
    private File dataRoot;
    private AudioManager manager;

    /** Records what the manager asks the audio device to do. */
    private final class FakeMusic implements Music {
        private final String id;
        private final List<String> log;
        private boolean playing;
        private boolean looping;
        private float position;
        private OnCompletionListener completion;

        FakeMusic(File file) {
            this.id = file.getName();
            this.log = AudioManagerTest.this.calls;
        }

        private void record(String what) {
            log.add("music:" + id + ":" + what);
        }

        /** Simulates the stream reaching its end (fires the completion listener). */
        void finish() {
            playing = false;
            if (completion != null) {
                completion.onCompletion(this);
            }
        }

        @Override public void play() { playing = true; record("play"); }
        @Override public void pause() { playing = false; record("pause"); }
        @Override public void stop() { playing = false; record("stop"); }
        @Override public boolean isPlaying() { return playing; }
        @Override public void setLooping(boolean looping) { this.looping = looping; record("loop=" + looping); }
        @Override public boolean isLooping() { return looping; }
        @Override public void setVolume(float volume) { record("volume=" + volume); }
        @Override public float getVolume() { return 1f; }
        @Override public void setPan(float pan, float volume) { }
        @Override public void setPosition(float position) { this.position = position; record("seek=" + position); }
        @Override public float getPosition() { return position; }
        @Override public void dispose() { record("dispose"); }
        @Override public void setOnCompletionListener(OnCompletionListener listener) { this.completion = listener; }
    }

    private final class FakeSound implements Sound {
        private final String id;

        FakeSound(File file) {
            this.id = file.getName();
        }

        @Override public long play() { calls.add("sound:" + id + ":play"); return 1L; }
        @Override public long play(float volume) { calls.add("sound:" + id + ":play=" + volume); return 1L; }
        @Override public long play(float volume, float pitch, float pan) {
            calls.add("sound:" + id + ":play=" + volume + "@" + pitch);
            return 1L;
        }
        @Override public long loop(float volume) { return play(volume); }
        @Override public long loop() { return play(); }
        @Override public long loop(float volume, float pitch, float pan) { return play(volume, pitch, pan); }
        @Override public void stop() { calls.add("sound:" + id + ":stop"); }
        @Override public void stop(long soundId) { }
        @Override public void pause() { }
        @Override public void pause(long soundId) { }
        @Override public void resume() { }
        @Override public void resume(long soundId) { }
        @Override public void dispose() { calls.add("sound:" + id + ":dispose"); }
        @Override public void setPitch(long soundId, float pitch) { }
        @Override public void setVolume(long soundId, float volume) { }
        @Override public void setPan(long soundId, float pan, float volume) { }
        @Override public void setLooping(long soundId, boolean looping) { }
    }

    @BeforeEach
    void setUp() throws Exception {
        calls.clear();
        fakeMusics.clear();
        dataRoot = Files.createTempDirectory("audio-manager").toFile();
        File se = new File(dataRoot, "audio/SE/Door enter.ogg");
        se.getParentFile().mkdirs();
        Files.write(se.toPath(), "fake".getBytes(StandardCharsets.UTF_8));

        AudioManifestData manifest = new AudioManifestData();
        manifest.loaded = true;
        manifest.preset = "standard";
        AudioManifestData.Entry entry = new AudioManifestData.Entry();
        entry.type = "SE";
        entry.file = "audio/SE/Door enter.ogg";
        manifest.entries.put("Door enter", entry);

        manager = new AudioManager();
        manager.attach(manifest, dataRoot);
        manager.setBackend(new AudioManager.Backend() {
            @Override public Music newMusic(File file) {
                FakeMusic music = new FakeMusic(file);
                fakeMusics.put(file.getName(), music);
                return music;
            }
            @Override public Sound newSound(File file) { return new FakeSound(file); }
        });
    }

    @Test
    @DisplayName("SE plays the manifest file with volume and pitch")
    void playsSe() {
        manager.playSe("Door enter", 80, 100);
        assertEquals(List.of("sound:Door enter.ogg:play=0.8@1.0"), calls);
    }

    @Test
    @DisplayName("BGM streams, loops, and is reused while already playing")
    void playsBgmOnce() {
        File bgm = new File(dataRoot, "audio/BGM/theme.ogg");
        bgm.getParentFile().mkdirs();
        try {
            Files.write(bgm.toPath(), "fake".getBytes(StandardCharsets.UTF_8));
        } catch (Exception error) {
            fail(error);
        }
        // The manifest above only has the SE; add the BGM through a fresh attach.
        AudioManifestData.Entry entry = new AudioManifestData.Entry();
        entry.type = "BGM";
        entry.file = "audio/BGM/theme.ogg";
        manager.attach(withEntry("theme", entry), dataRoot);

        manager.playBgm("theme", 50, 100);
        manager.playBgm("theme", 50, 100);
        assertEquals(List.of(
                "music:theme.ogg:loop=true",
                "music:theme.ogg:volume=0.5",
                "music:theme.ogg:play"), calls);
        assertEquals("theme", manager.currentBgmId());

        manager.stopBgm();
        assertTrue(calls.contains("music:theme.ogg:stop"));
        assertNull(manager.currentBgmId());
    }

    @Test
    @DisplayName("a missing logical id is reported and plays nothing")
    void missingId() {
        manager.playSe("nope");
        assertTrue(calls.isEmpty());
    }

    @Test
    @DisplayName("audio names resolve ignoring case, like the RMXP file system")
    void caseInsensitiveLookup() {
        manager.playSe("door ENTER", 100, 100);
        assertEquals(List.of("sound:Door enter.ogg:play=1.0@1.0"), calls);
    }

    @Test
    @DisplayName("dispose releases music and samples")
    void disposes() {
        manager.playSe("Door enter");
        manager.dispose();
        assertTrue(calls.contains("sound:Door enter.ogg:dispose"));
    }

    @Test
    @DisplayName("a cued BGM change fades the old out and starts the new at 60 % (R6.24)")
    void cuesBgmChange() {
        attachBgms("theme", "theme.ogg", "route", "route.ogg");
        manager.playBgm("theme", 100, 100);
        calls.clear();

        manager.cueBgm("route", 80, 100, 1f);
        assertTrue(calls.isEmpty(), "nothing may start before 60 % of the fade");

        manager.update(0.6f);
        // float math: 1.0 * (0.4 / 1.0) prints as 0.39999998
        assertTrue(calls.stream().anyMatch(call -> call.startsWith("music:theme.ogg:volume=0.39")),
                calls.toString());
        assertTrue(calls.contains("music:route.ogg:loop=true"), calls.toString());
        assertTrue(calls.contains("music:route.ogg:volume=0.8"), calls.toString());
        assertTrue(calls.contains("music:route.ogg:play"), calls.toString());
        assertEquals("route", manager.currentBgmId());

        calls.clear();
        manager.update(0.4f);
        assertTrue(calls.contains("music:theme.ogg:stop"), calls.toString());
        assertFalse(calls.contains("music:route.ogg:play"), "the new track keeps playing");

        calls.clear();
        manager.update(1f);
        assertTrue(calls.isEmpty(), calls.toString());
    }

    @Test
    @DisplayName("cuing the BGM that is already playing does not restart it (R6.24)")
    void cuesSameBgm() {
        attachBgms("theme", "theme.ogg");
        manager.playBgm("theme", 100, 100);
        calls.clear();

        manager.cueBgm("theme", 100, 100, 1f);
        manager.update(1f);

        assertEquals(List.of(), calls);
        assertEquals("theme", manager.currentBgmId());
    }

    @Test
    @DisplayName("a cue from silence plays immediately, like pbBGMPlay (R6.24)")
    void cuesFromSilence() {
        attachBgms("theme", "theme.ogg");
        manager.cueBgm("theme", 60, 100, 1f);
        assertEquals(List.of(
                "music:theme.ogg:loop=true",
                "music:theme.ogg:volume=0.6",
                "music:theme.ogg:play"), calls);
    }

    @Test
    @DisplayName("L8.1: a tagged BGM plays the intro once, then loops the segment")
    void playsIntroThenLoop() {
        attachRegionBgm("route", "route.ogg", "route.intro.ogg", 2f, 5f);
        manager.playBgm("route", 100, 100);
        assertEquals(List.of(
                "music:route.intro.ogg:loop=false",
                "music:route.intro.ogg:volume=1.0",
                "music:route.intro.ogg:play"), calls);
        assertEquals("route", manager.currentBgmId());

        FakeMusic intro = fakeMusics.get("route.intro.ogg");
        manager.update(0.016f);
        assertFalse(calls.stream().anyMatch(call -> call.contains("route.ogg")),
                "the loop must not start while the intro plays");

        intro.playing = false; // the intro reached its end
        manager.update(0.016f);
        assertTrue(calls.contains("music:route.ogg:loop=true"), calls.toString());
        assertTrue(calls.contains("music:route.ogg:volume=1.0"), calls.toString());
        assertTrue(calls.contains("music:route.ogg:play"), calls.toString());

        calls.clear();
        manager.update(0.5f);
        assertEquals(List.of(), calls, "a looping segment is not restarted");
    }

    @Test
    @DisplayName("L8.1: the intro completion listener starts the loop right away")
    void completionListenerStartsLoop() {
        attachRegionBgm("route", "route.ogg", "route.intro.ogg", 2f, 5f);
        manager.playBgm("route", 100, 100);
        FakeMusic intro = fakeMusics.get("route.intro.ogg");
        calls.clear();

        intro.finish();
        assertEquals(List.of(
                "music:route.ogg:loop=true",
                "music:route.ogg:volume=1.0",
                "music:route.ogg:play"), calls);

        calls.clear();
        manager.update(0.016f);
        assertEquals(List.of(), calls, "the safety net must not restart the loop");
    }

    @Test
    @DisplayName("L8.1: stopping during the intro must not start the loop later")
    void stopDuringIntroCancelsLoop() {
        attachRegionBgm("route", "route.ogg", "route.intro.ogg", 2f, 5f);
        manager.playBgm("route", 100, 100);
        FakeMusic intro = fakeMusics.get("route.intro.ogg");
        calls.clear();

        manager.stopBgm();
        assertTrue(calls.contains("music:route.intro.ogg:stop"), calls.toString());

        calls.clear();
        manager.update(1f);
        assertEquals(List.of(), calls);
        assertNull(manager.currentBgmId());
    }

    @Test
    @DisplayName("L8.1: a cued tagged BGM chains intro -> loop too")
    void cuesTaggedRegion() throws Exception {
        writeBgm("theme.ogg");
        writeBgm("route.ogg");
        writeBgm("route.intro.ogg");
        AudioManifestData manifest = new AudioManifestData();
        manifest.loaded = true;
        manifest.preset = "standard";
        manifest.entries.put("theme", bgmEntry("theme.ogg"));
        AudioManifestData.Entry route = bgmEntry("route.ogg");
        route.introFile = "audio/BGM/route.intro.ogg";
        route.loopStartSeconds = 2f;
        route.loopEndSeconds = 5f;
        manifest.entries.put("route", route);
        manager.attach(manifest, dataRoot);

        manager.playBgm("theme", 100, 100);
        calls.clear();
        manager.cueBgm("route", 100, 100, 1f);
        manager.update(0.6f); // the cue fires
        assertTrue(calls.contains("music:route.intro.ogg:play"), calls.toString());

        FakeMusic intro = fakeMusics.get("route.intro.ogg");
        calls.clear();
        intro.finish();
        assertTrue(calls.contains("music:route.ogg:play"), calls.toString());
        assertEquals("route", manager.currentBgmId());
    }

    @Test
    @DisplayName("cry files resolve from the numeric species id, symbols by name (R6.30)")
    void resolvesCries() throws Exception {
        File cry = new File(dataRoot, "audio/SE/Cries/251Cry.ogg");
        cry.getParentFile().mkdirs();
        Files.write(cry.toPath(), "fake".getBytes(StandardCharsets.UTF_8));

        AudioManifestData manifest = new AudioManifestData();
        manifest.loaded = true;
        manifest.preset = "standard";
        manifest.entries.put("251Cry", cryEntry("251Cry"));
        manifest.entries.put("SNORLAXCry", cryEntry("SNORLAXCry"));
        manager.attach(manifest, dataRoot);

        assertEquals("251Cry", manager.resolveCry(251));
        assertNull(manager.resolveCry(1014), "a species without a file stays silent");
        assertEquals("SNORLAXCry", manager.resolveCry(":SNORLAX"),
                "a symbol cry resolves through its name");

        manager.playSe(manager.resolveCry(251), 100, 100);
        assertEquals(List.of("sound:251Cry.ogg:play=1.0@1.0"), calls);
    }

    /** L8.1: a manifest with one split BGM (loop segment + intro segment). */
    private void attachRegionBgm(String id, String loopName, String introName, float loopStart, float loopEnd) {
        try {
            writeBgm(loopName);
            if (introName != null) {
                writeBgm(introName);
            }
        } catch (Exception error) {
            fail(error);
        }
        AudioManifestData manifest = new AudioManifestData();
        manifest.loaded = true;
        manifest.preset = "standard";
        manifest.entries.put("Door enter", managerEntry());
        AudioManifestData.Entry entry = bgmEntry(loopName);
        if (introName != null) {
            entry.introFile = "audio/BGM/" + introName;
        }
        entry.loopStartSeconds = loopStart;
        entry.loopEndSeconds = loopEnd;
        manifest.entries.put(id, entry);
        manager.attach(manifest, dataRoot);
    }

    private void writeBgm(String name) throws Exception {
        File file = new File(dataRoot, "audio/BGM/" + name);
        file.getParentFile().mkdirs();
        Files.write(file.toPath(), "fake".getBytes(StandardCharsets.UTF_8));
    }

    private AudioManifestData.Entry bgmEntry(String name) {
        AudioManifestData.Entry entry = new AudioManifestData.Entry();
        entry.type = "BGM";
        entry.file = "audio/BGM/" + name;
        return entry;
    }

    private AudioManifestData.Entry cryEntry(String name) {
        AudioManifestData.Entry entry = new AudioManifestData.Entry();
        entry.type = "SE";
        entry.file = "audio/SE/Cries/" + name + ".ogg";
        return entry;
    }

    /** Re-attaches a manifest with the SE plus the given BGM id/file pairs. */
    private void attachBgms(String... idFilePairs) {
        AudioManifestData manifest = new AudioManifestData();
        manifest.loaded = true;
        manifest.preset = "standard";
        manifest.entries.put("Door enter", managerEntry());
        for (int i = 0; i + 1 < idFilePairs.length; i += 2) {
            String id = idFilePairs[i];
            String name = idFilePairs[i + 1];
            File file = new File(dataRoot, "audio/BGM/" + name);
            file.getParentFile().mkdirs();
            try {
                Files.write(file.toPath(), "fake".getBytes(StandardCharsets.UTF_8));
            } catch (Exception error) {
                fail(error);
            }
            AudioManifestData.Entry entry = new AudioManifestData.Entry();
            entry.type = "BGM";
            entry.file = "audio/BGM/" + name;
            manifest.entries.put(id, entry);
        }
        manager.attach(manifest, dataRoot);
    }

    private AudioManifestData withEntry(String id, AudioManifestData.Entry entry) {
        AudioManifestData manifest = new AudioManifestData();
        manifest.loaded = true;
        manifest.preset = "standard";
        manifest.entries.put("Door enter", managerEntry());
        manifest.entries.put(id, entry);
        return manifest;
    }

    private AudioManifestData.Entry managerEntry() {
        AudioManifestData.Entry entry = new AudioManifestData.Entry();
        entry.type = "SE";
        entry.file = "audio/SE/Door enter.ogg";
        return entry;
    }
}
