package pokemon.runtime.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.ObjectMap;
import pokemon.runtime.data.AudioManifestData;

import java.io.File;

/**
 * Audio runtime (project3 sections 33-36, R9): every logical id is looked up in
 * {@code generated/audio-manifest.json} and played from {@code generated/audio/}.
 * BGM / BGS stream through libGDX {@link Music} (never decoded into RAM, section
 * 35), ME / SE are short preloaded {@link Sound} samples.
 *
 * <p>The backend is a thin seam over {@code Gdx.audio} so the lookup, caching,
 * volume and dispose rules can be unit tested without an audio device.</p>
 */
public final class AudioManager implements com.badlogic.gdx.utils.Disposable {

    /** Seam over Gdx.audio: production uses libGDX, tests record calls. */
    public interface Backend {
        Music newMusic(File file);

        Sound newSound(File file);
    }

    private static final Backend GDX_BACKEND = new Backend() {
        @Override
        public Music newMusic(File file) {
            return Gdx.audio.newMusic(Gdx.files.absolute(file.getAbsolutePath()));
        }

        @Override
        public Sound newSound(File file) {
            return Gdx.audio.newSound(Gdx.files.absolute(file.getAbsolutePath()));
        }
    };

    private final ObjectMap<String, Music> musics = new ObjectMap<>();
    private final ObjectMap<String, Sound> sounds = new ObjectMap<>();
    private AudioManifestData manifest;
    private File dataRoot;
    private Backend backend = GDX_BACKEND;
    private Music currentBgm;
    private Music currentBgs;
    private String currentBgmId;
    private float currentVolume = 1f;
    private float currentBgsVolume = 1f;

    // L8.1: a tagged BGM ships as [loop segment] + [intro segment]. The intro
    // plays once, then the loop segment starts and loops as a whole file.
    // (libGDX Music can only loop whole files, and seeking a region costs a
    // 0.3-0.6 s decoded scan, so the builder splits the track instead.)
    private Music pendingLoop;
    private String pendingLoopId;
    private float pendingLoopVolume;
    private final Music.OnCompletionListener introFinished = music -> startPendingLoop();

    /**
     * R6.24: {@code pbCueBGM} starts the incoming track once 60 % of the fade
     * has passed ({@code cueFrames = seconds*40*3/5} in the project scripts).
     */
    private static final float CUE_START_RATIO = 0.6f;

    // R6.24: map BGM transition (project: autofade / pbCueBGM). The outgoing
    // stream keeps playing while it fades; the incoming one overlaps it.
    private Music fadingBgm;
    private float fadingVolume;
    private float fadeDuration;
    private float fadeRemaining;
    private Music incomingBgm;
    private float incomingVolume;
    private float cueRemaining;

    public void setBackend(Backend backend) {
        this.backend = backend == null ? GDX_BACKEND : backend;
    }

    // ------------------------------------------------------------------
    // L1: user volume factors (options screen), 1.0 = the project volume
    // ------------------------------------------------------------------

    private float bgmFactor = 1f;
    private float seFactor = 1f;
    private float bgsFactor = 1f;

    /** Applies the options-screen percentages; a running BGM updates live. */
    public void setVolumeFactors(float bgm, float se, float bgs) {
        this.bgmFactor = clampFactor(bgm);
        this.seFactor = clampFactor(se);
        this.bgsFactor = clampFactor(bgs);
        if (currentBgm != null) {
            currentBgm.setVolume(currentVolume * bgmFactor);
        }
        if (currentBgs != null) {
            // BGS volume is not tracked per track; the next play uses the factor
            currentBgs.setVolume(currentBgsVolume * bgsFactor);
        }
    }

    private static float clampFactor(float factor) {
        return Math.max(0f, Math.min(1f, factor));
    }

    /** Called once the database (and with it the manifest) is loaded. */
    public void attach(AudioManifestData manifest, File dataRoot) {
        this.manifest = manifest;
        this.dataRoot = dataRoot;
    }

    public boolean ready() {
        return manifest != null && manifest.loaded && dataRoot != null;
    }

    // ------------------------------------------------------------------
    // BGM / BGS: streamed, looping
    // ------------------------------------------------------------------

    public void playBgm(String logicalId) {
        playBgm(logicalId, 100, 100);
    }

    /**
     * R6.24: opens the stream ahead of time. Preloading a neighbouring map
     * warms the BGM it will play, so the crossing itself does not pay for
     * opening the file.
     */
    public void warmBgm(String logicalId) {
        if (logicalId == null || logicalId.isEmpty()) {
            return;
        }
        music(logicalId, "BGM");
        intro(logicalId);
    }

    public void playBgm(String logicalId, int volume, int pitch) {
        if (logicalId == null || logicalId.isEmpty() || logicalId.equals(currentBgmId)) {
            return;
        }
        stopBgm();
        Music loop = music(logicalId, "BGM");
        if (loop == null) {
            return;
        }
        currentVolume = volume(volume) * bgmFactor;
        currentBgmId = logicalId;
        startLoopOrIntro(logicalId, loop, currentVolume);
    }

    /**
     * R6.24: {@code pbCueBGM(bgm, seconds)} from the project's Essentials
     * scripts - {@code autoplayAsCue} calls it with 1.0 s whenever the map
     * changes. The BGM that is playing fades out over {@code fadeSeconds}
     * while the new one starts at 60 % of that time, so the two overlap
     * instead of cutting. The same track keeps playing untouched; a cue from
     * silence and {@code fadeSeconds <= 0} behave like {@link #playBgm}.
     */
    public void cueBgm(String logicalId, int volume, int pitch, float fadeSeconds) {
        if (logicalId == null || logicalId.isEmpty() || logicalId.equals(currentBgmId)) {
            return;
        }
        if (fadeSeconds <= 0f || currentBgm == null) {
            playBgm(logicalId, volume, pitch);
            return;
        }
        Music music = music(logicalId, "BGM");
        if (music == null) {
            return;
        }
        if (music == currentBgm) {
            // Same file through a different spelling: keep it playing.
            currentBgmId = logicalId;
            return;
        }
        cancelTransition();
        fadingBgm = currentBgm;
        fadingVolume = currentVolume;
        fadeDuration = fadeSeconds;
        fadeRemaining = fadeSeconds;
        incomingBgm = music;
        incomingVolume = volume(volume) * bgmFactor;
        cueRemaining = fadeSeconds * CUE_START_RATIO;
        currentBgm = null;
        currentBgmId = logicalId;
    }

    /** Advances a cued BGM transition; the game loop calls this once per frame. */
    public void update(float delta) {
        float step = Math.max(0f, delta);
        if (incomingBgm != null) {
            cueRemaining -= step;
            if (cueRemaining <= 0f) {
                Music music = incomingBgm;
                incomingBgm = null;
                startLoopOrIntro(currentBgmId, music, incomingVolume);
            }
        }
        if (fadingBgm != null) {
            fadeRemaining -= step;
            if (fadeRemaining <= 0f) {
                fadingBgm.stop();
                fadingBgm = null;
            } else {
                fadingBgm.setVolume(fadingVolume * (fadeRemaining / fadeDuration));
            }
        }
        // Safety net for a completion callback that never fired (long frame,
        // backend quirk): the intro stopped, so start the queued loop.
        if (pendingLoop != null && currentBgm != null && currentBgm != pendingLoop
                && pendingLoopId != null && pendingLoopId.equals(currentBgmId)
                && !currentBgm.isPlaying()) {
            startPendingLoop();
        }
    }

    /**
     * L8.1: starts the intro segment (once) with the loop segment queued after
     * it, or just the loop segment when the track is not split.
     */
    private void startLoopOrIntro(String logicalId, Music loop, float volume) {
        Music intro = intro(logicalId);
        if (intro == null || intro == loop) {
            loop.setLooping(true);
            loop.setVolume(volume);
            loop.play();
            currentBgm = loop;
            return;
        }
        pendingLoop = loop;
        pendingLoopId = logicalId;
        pendingLoopVolume = volume;
        intro.setLooping(false);
        intro.setVolume(volume);
        intro.setOnCompletionListener(introFinished);
        intro.play();
        currentBgm = intro;
    }

    /** Starts the queued loop segment once the intro segment has ended. */
    private void startPendingLoop() {
        Music loop = pendingLoop;
        String id = pendingLoopId;
        if (loop == null || id == null) {
            return;
        }
        pendingLoop = null;
        pendingLoopId = null;
        if (!id.equals(currentBgmId) || currentBgm == loop) {
            return; // stopped or switched to another track while the intro played
        }
        loop.setLooping(true);
        loop.setVolume(pendingLoopVolume);
        loop.play();
        currentBgm = loop;
    }

    /** The intro segment Music of a tagged BGM, or null when there is none. */
    private Music intro(String logicalId) {
        if (!ready()) {
            return null;
        }
        AudioManifestData.Entry entry = entryOf(logicalId);
        if (entry == null || !entry.hasIntro()) {
            return null;
        }
        File file = new File(dataRoot, entry.introFile);
        if (!file.isFile()) {
            log("BGM intro file missing: " + file.getAbsolutePath());
            return null;
        }
        return cachedMusic(logicalId + "#intro", file);
    }

    private AudioManifestData.Entry entryOf(String logicalId) {
        AudioManifestData.Entry entry = manifest.find(logicalId);
        return entry == null ? findIgnoringCase(logicalId) : entry;
    }

    /** Drops a running fade (a script command or a new cue wins). */
    private void cancelTransition() {
        if (fadingBgm != null) {
            fadingBgm.stop();
            fadingBgm = null;
        }
        incomingBgm = null;
        pendingLoop = null;
        pendingLoopId = null;
        fadeRemaining = 0f;
        cueRemaining = 0f;
    }

    public void playBgs(String logicalId) {
        playBgs(logicalId, 100, 100);
    }

    public void playBgs(String logicalId, int volume, int pitch) {
        Music music = music(logicalId, "BGS");
        if (music == null) {
            return;
        }
        if (currentBgs != null && currentBgs != music) {
            currentBgs.stop();
        }
        music.setLooping(true);
        currentBgsVolume = volume(volume) * bgsFactor;
        music.setVolume(currentBgsVolume);
        music.play();
        currentBgs = music;
    }

    public void stopBgm() {
        cancelTransition();
        if (currentBgm != null) {
            currentBgm.stop();
        }
        currentBgm = null;
        currentBgmId = null;
    }

    public void stopBgs() {
        if (currentBgs != null) {
            currentBgs.stop();
        }
        currentBgs = null;
    }

    public String currentBgmId() {
        return currentBgmId;
    }

    // ------------------------------------------------------------------
    // ME / SE: preloaded samples
    // ------------------------------------------------------------------

    public void playMe(String logicalId) {
        playMe(logicalId, 100, 100);
    }

    public void playMe(String logicalId, int volume, int pitch) {
        play(logicalId, "ME", volume, pitch);
    }

    public void playSe(String logicalId) {
        playSe(logicalId, 100, 100);
    }

    public void playSe(String logicalId, int volume, int pitch) {
        play(logicalId, "SE", volume, pitch);
    }

    // ------------------------------------------------------------------
    // R6.30: cries (pbCryFile)
    // ------------------------------------------------------------------

    /**
     * Resolves the cry file of a numeric species id the way the project names
     * them ({@code %03dCry_0} then {@code %03dCry}; the files under
     * {@code Audio/SE/Cries} keep their numeric names). Returns the manifest
     * key or null - the Ruby {@code pbCryFile} also returns nil for a species
     * without a file, and the event then plays nothing.
     */
    public String resolveCry(int species) {
        if (!ready() || species <= 0) {
            return null;
        }
        return resolveCryKey(String.format("%03d", species));
    }

    /** Cry addressed by a name / symbol; only name-based files can match. */
    public String resolveCry(String name) {
        if (!ready() || name == null || name.isEmpty()) {
            return null;
        }
        String base = name.startsWith(":") ? name.substring(1) : name;
        return resolveCryKey(base);
    }

    private String resolveCryKey(String base) {
        for (String candidate : new String[] { base + "Cry_0", base + "Cry" }) {
            if (manifest.find(candidate) != null || findIgnoringCase(candidate) != null) {
                return candidate;
            }
        }
        return null;
    }

    private void play(String logicalId, String type, int volume, int pitch) {
        Sound sound = sound(logicalId, type);
        if (sound == null) {
            return;
        }
        sound.play(volume(volume) * seFactor, pitch(pitch), 0f);
    }

    // ------------------------------------------------------------------

    /** Stops every stream and releases every sample. */
    @Override
    public void dispose() {
        stopBgm();
        for (Music music : musics.values()) {
            music.dispose();
        }
        for (Sound sound : sounds.values()) {
            sound.dispose();
        }
        musics.clear();
        sounds.clear();
    }

    private Music music(String logicalId, String type) {
        File file = fileOf(logicalId, type);
        if (file == null) {
            return null;
        }
        return cachedMusic(logicalId, file);
    }

    private Music cachedMusic(String key, File file) {
        Music music = musics.get(key);
        if (music == null) {
            music = backend.newMusic(file);
            musics.put(key, music);
        }
        return music;
    }

    private Sound sound(String logicalId, String type) {
        File file = fileOf(logicalId, type);
        if (file == null) {
            return null;
        }
        Sound sound = sounds.get(logicalId);
        if (sound == null) {
            sound = backend.newSound(file);
            sounds.put(logicalId, sound);
        }
        return sound;
    }

    /** Logical id -> generated/audio file, with a clear message when missing. */
    private File fileOf(String logicalId, String type) {
        if (!ready()) {
            log("audio manifest not loaded; cannot play " + type + " " + logicalId);
            return null;
        }
        AudioManifestData.Entry entry = entryOf(logicalId);
        if (entry == null) {
            log(type + " not in the audio manifest: " + logicalId
                    + " (run builder build-audio and rebuild the data)");
            return null;
        }
        File file = new File(dataRoot, entry.file);
        if (!file.isFile()) {
            log(type + " file missing: " + file.getAbsolutePath());
            return null;
        }
        return file;
    }

    /**
     * RMXP resolves audio names through the Windows file system, so it does not
     * care about case ("Voltorb Flip Explosion" vs "...explosion"). The
     * manifest keeps the authored spelling, so fall back to a case-insensitive
     * match before reporting a miss.
     */
    private AudioManifestData.Entry findIgnoringCase(String logicalId) {
        for (ObjectMap.Entry<String, AudioManifestData.Entry> entry : manifest.entries) {
            if (entry.key.equalsIgnoreCase(logicalId)) {
                return entry.value;
            }
        }
        return null;
    }

    private static float volume(int percent) {
        return Math.max(0f, Math.min(1f, percent / 100f));
    }

    private static float pitch(int percent) {
        return Math.max(0.1f, percent / 100f);
    }

    private static void log(String message) {
        if (Gdx.app != null) {
            Gdx.app.log("AudioManager", message);
        }
    }
}
