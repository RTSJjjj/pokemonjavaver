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
            return Gdx.audio.newMusic(playable(file));
        }

        @Override
        public Sound newSound(File file) {
            return Gdx.audio.newSound(playable(file));
        }
    };

    /**
     * P3: the packaged audio is encrypted. The desktop backend decodes through the handle's decrypting stream; the Android
     * players (MediaPlayer / SoundPool) need a real file, so the file is decrypted once into the app's cache directory.
     */
    private static com.badlogic.gdx.files.FileHandle playable(File file) {
        if (!pokemon.runtime.data.ResourceCrypto.isEncrypted(file)) {
            return Gdx.files.absolute(file.getAbsolutePath());
        }
        if (Gdx.app != null && Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop) {
            return pokemon.runtime.data.ResourceCrypto.handle(file);
        }
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot);
        File cache = new File(System.getProperty("java.io.tmpdir", "."),
                "pkre-" + Integer.toHexString((file.getAbsolutePath() + file.length() + file.lastModified()).hashCode()) + extension);
        if (!cache.isFile()) {
            try (java.io.InputStream in = pokemon.runtime.data.ResourceCrypto.open(file);
                 java.io.OutputStream out = new java.io.FileOutputStream(cache)) {
                byte[] buffer = new byte[65536];
                int n;
                while ((n = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, n);
                }
            } catch (java.io.IOException error) {
                throw new com.badlogic.gdx.utils.GdxRuntimeException("cannot decrypt " + file, error);
            }
        }
        return Gdx.files.absolute(cache.getAbsolutePath());
    }

    private final ObjectMap<String, Music> musics = new ObjectMap<>();
    private final ObjectMap<String, Sound> sounds = new ObjectMap<>();
    /** Ids that failed to resolve: not rescanned (and not re-logged) on every play. */
    private final com.badlogic.gdx.utils.ObjectSet<String> unresolvedSounds = new com.badlogic.gdx.utils.ObjectSet<>();
    /** The SE samples currently sounding (pbSEStop stops them all). */
    private final java.util.LinkedHashSet<Sound> activeSe = new java.util.LinkedHashSet<>();
    private AudioManifestData manifest;
    private File dataRoot;
    private Backend backend = GDX_BACKEND;
    private Music currentBgm;
    private Music currentBgs;
    private String currentBgmId;
    private float currentVolume = 1f;
    private float currentBgsVolume = 1f;
    /** The BGS the way {@code Game_System#playing_bgs} keeps it: name, volume, pitch (null = none). */
    private String currentBgsId;
    private int currentBgsVolumePercent = 100;
    private int currentBgsPitch = 100;
    /** 020_Game_System:122-123/220-221 {@code @memorized_bgm / @memorized_bgs} (null = nothing playing). */
    private String memorizedBgmId;
    private int memorizedBgmVolume = 100;
    private int memorizedBgmPitch = 100;
    private String memorizedBgsId;
    private int memorizedBgsVolume = 100;
    private int memorizedBgsPitch = 100;
    /** Raw (unfactored) volume / pitch of the running BGM, for {@link #pauseBgm()}. */
    private int currentBgmVolumePercent = 100;
    private int currentBgmPitch = 100;

    // Game_System:85-103 bgm_pause / bgm_resume: the overworld BGM is
    // memorized (with its play position) before a battle and replayed when the
    // battle is over. pbBattleAnimation:26-30 captures getPlayingBGM, pauses,
    // and :112-114 resumes afterwards.
    private boolean bgmPaused;
    private String pausedBgmId;
    private int pausedBgmVolume = 100;
    private int pausedBgmPitch = 100;
    private float pausedBgmPosition;

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
    private int incomingVolumePercent = 100;
    private int incomingPitch = 100;
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
        unresolvedSounds.clear();
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

    /**
     * {@code FileTest.audio_exist?("Audio/BGM/"+name)} (Game_System:69): whether
     * the manifest has a playable file for this BGM. A miss is logged.
     */
    public boolean bgmExists(String logicalId) {
        return logicalId != null && !logicalId.isEmpty() && fileOf(logicalId, "BGM") != null;
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
        currentBgmVolumePercent = volume;
        currentBgmPitch = pitch;
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
        incomingVolumePercent = volume;
        incomingPitch = pitch;
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
                currentBgmVolumePercent = incomingVolumePercent;
                currentBgmPitch = incomingPitch;
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
        if (entry != null) {
            return entry;
        }
        entry = findIgnoringCase(logicalId);
        if (entry != null) {
            return entry;
        }
        // RGSS resolves an audio name through the file system, so a folder
        // path and an extension are both optional there: the metadata of
        // PBS/metadata.txt names files ("Battle wild.mid"), and
        // pbGetWildVictoryME / pbGetTrainerVictoryME / pbGetWildCaptureME even
        // prefix "../../Audio/ME/" to force the ME folder
        // (PSystem_FileUtilities:581/601/697) - which the manifest key does
        // not carry, because the builder indexes by base name.
        String bare = baseName(logicalId);
        if (bare == null || bare.equals(logicalId)) {
            return null;
        }
        entry = manifest.find(bare);
        return entry == null ? findIgnoringCase(bare) : entry;
    }

    /** "…/Audio/ME/Battle victory wild.ogg" -> "Battle victory wild". */
    private static String baseName(String name) {
        if (name == null) {
            return null;
        }
        String base = name.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        return base;
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
        currentBgsId = logicalId;                               // Game_System#bgs_play: @playing_bgs = bgs.clone
        currentBgsVolumePercent = volume;
        currentBgsPitch = pitch;
    }

    public void stopBgm() {
        cancelTransition();
        if (currentBgm != null) {
            currentBgm.stop();
        }
        currentBgm = null;
        currentBgmId = null;
    }

    /**
     * {@code Game_System#bgm_pause} (Game_System:85-90): memorizes the running
     * track and its position. The stream itself keeps playing, exactly like
     * RMXP - the next BGM replaces it, and {@link #resumeBgm()} brings the
     * memorized one back.
     */
    public void pauseBgm() {
        if (bgmPaused || currentBgm == null || currentBgmId == null) {
            return;
        }
        pausedBgmId = currentBgmId;
        pausedBgmVolume = currentBgmVolumePercent;
        pausedBgmPitch = currentBgmPitch;
        pausedBgmPosition = Math.max(0f, currentBgm.getPosition());
        bgmPaused = true;
    }

    /**
     * {@code Game_System#bgm_resume(bgm)} (Game_System:97-103): replays the
     * memorized track from the position it was paused at. A resume without a
     * pause does nothing, and the pause is cleared either way.
     */
    public void resumeBgm() {
        if (!bgmPaused) {
            return;
        }
        bgmPaused = false;
        String id = pausedBgmId;
        float position = pausedBgmPosition;
        pausedBgmId = null;
        pausedBgmPosition = 0f;
        if (id == null || id.isEmpty()) {
            return;
        }
        if (id.equals(currentBgmId) && currentBgm != null) {
            return; // nothing replaced it: it never stopped playing
        }
        playBgm(id, pausedBgmVolume, pausedBgmPitch);
        if (currentBgm != null && position > 0f) {
            currentBgm.setPosition(position);
        }
    }

    /** Whether {@link #pauseBgm()} is waiting for a {@link #resumeBgm()}. */
    public boolean bgmPaused() {
        return bgmPaused;
    }

    /**
     * {@code pbBGMFade(1.0)} ({@code PokeBattle_Scene:300}) is
     * {@code pbBGMStop(x)} (Audio_Play:71), which for {@code x>0.0} falls
     * through to {@code Game_System#bgm_fade(time)} (Game_System:111-115):
     * {@code @playing_bgm = nil} and {@code Audio.bgm_fade((time*1000).floor)},
     * so the running BGM fades out over {@code seconds} and then stops.
     * {@link #update(float)} already advances the fade (:223-231).
     * {@code timeInSeconds<=0.0} (and no {@code bgm_fade} receiver) takes
     * {@code pbBGMStop}'s {@code bgm_stop} branch (Audio_Play:78-80).
     */
    public void fadeBgm(float seconds) {
        if (seconds <= 0f) {                                    // Audio_Play:75
            stopBgm();                                          // Game_System:105-109
            return;
        }
        if (currentBgm == null) {
            return;                                             // nothing to fade
        }
        // Game_System:114 Audio.bgm_fade: reuse cueBgm's fade state (:196-208).
        cancelTransition();
        fadingBgm = currentBgm;
        fadingVolume = currentVolume;
        fadeDuration = seconds;
        fadeRemaining = seconds;
        currentBgm = null;                                      // Game_System:113
        currentBgmId = null;
    }

    public void stopBgs() {
        if (currentBgs != null) {
            currentBgs.stop();
        }
        currentBgs = null;
        currentBgsId = null;                                    // Game_System:200-203 bgs_stop: @playing_bgs = nil
    }

    /**
     * 020_Game_System:210-214 {@code bgs_fade}: {@code @playing_bgs = nil} and
     * {@code Audio.bgs_fade}. 登记: this runtime has no BGS volume ramp, so the
     * BGS stops at once (event command 246 is used once in the project).
     */
    public void fadeBgs(float seconds) {
        stopBgs();
    }

    /**
     * 048_Interpreter:1361-1367 command_247 {@code bgm_memorize}/{@code bgs_memorize}
     * (020_Game_System:122-124 / 220-222): remembers what is playing now.
     */
    public void memorizeBgmAndBgs() {
        memorizedBgmId = currentBgmId;
        memorizedBgmVolume = currentBgmVolumePercent;
        memorizedBgmPitch = currentBgmPitch;
        memorizedBgsId = currentBgsId;
        memorizedBgsVolume = currentBgsVolumePercent;
        memorizedBgsPitch = currentBgsPitch;
    }

    /**
     * 048_Interpreter:1371-1377 command_248 {@code bgm_restore}/{@code bgs_restore}
     * (020_Game_System:127-129 / 224-226): {@code bgm_play(@memorized_bgm)}. A
     * memorized nil stops the track ({@code bgm_play_internal} :73-76), and playing
     * the same file again restarts it, like {@code Audio.bgm_play}.
     */
    public void restoreBgmAndBgs() {
        if (memorizedBgmId == null) {
            stopBgm();
        } else {
            if (memorizedBgmId.equals(currentBgmId)) {
                stopBgm();
            }
            playBgm(memorizedBgmId, memorizedBgmVolume, memorizedBgmPitch);
        }
        if (memorizedBgsId == null) {
            stopBgs();
        } else {
            playBgs(memorizedBgsId, memorizedBgsVolume, memorizedBgsPitch);
        }
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

    /** P4: the SE ids of the manifest that start with one of the prefixes ("GUI ", "Battle "), for the loading screen. */
    public java.util.List<String> seIdsWithPrefix(String... prefixes) {
        java.util.List<String> ids = new java.util.ArrayList<>();
        if (!ready()) {
            return ids;
        }
        for (ObjectMap.Entry<String, AudioManifestData.Entry> entry : manifest.entries) {
            if (!"SE".equals(entry.value.type)) {
                continue;
            }
            for (String prefix : prefixes) {
                if (entry.key.startsWith(prefix)) {
                    ids.add(entry.key);
                    break;
                }
            }
        }
        java.util.Collections.sort(ids);
        return ids;
    }

    /** P4: loads the sample now (the first play then does not decode it); nothing is played. */
    public void preloadSe(String logicalId) {
        if (ready()) {
            sound(logicalId, "SE");
        }
    }

    public void playSe(String logicalId, int volume, int pitch) {
        play(logicalId, "SE", volume, pitch);
    }

    /**
     * {@code pbSEStop} (Audio_Play:223-231) / {@code Game_System#se_stop}: stops
     * every sound effect that is currently playing.
     */
    public void stopSe() {
        for (Sound sound : activeSe) {
            sound.stop();
        }
        activeSe.clear();
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

    /**
     * {@code pbPlayCry(pokemon)} (PSystem_FileUtilities:476): volume 90, the
     * species' cry. The summary / Pokédex / trainer card play this when they
     * open a Pokemon, mirroring the plugin's own call points.
     */
    public void playCry(int species) {
        playCry(species, 90, 100);
    }

    public void playCry(int species, int volume, int pitch) {
        String cry = species <= 0 ? null : resolveCry(species);
        if (cry != null) {
            playSe(cry, volume, pitch);
        }
    }

    private String resolveCryKey(String base) {
        for (String candidate : new String[] { base + "Cry_0", base + "Cry" }) {
            if (manifest.find(candidate) != null || findIgnoringCase(candidate) != null) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * {@code pbCryFile(pokemon,form=0)} (PSystem_FileUtilities:509-539) for a
     * Pokemon object: the species' constant name first, the
     * {@code %03d} species id second, each with the Pokemon's own form appended
     * and then without it. {@code pbResolveAudioSE} checks the four extensions
     * the manifest already folded into one key.
     */
    public String resolveCryFile(int species, String speciesName, int form) {
        if (!ready() || species <= 0) {
            return null;
        }
        String number = String.format(java.util.Locale.ROOT, "%03d", species);
        for (String candidate : new String[] {
                speciesName == null ? null : speciesName + "Cry_" + form,   // :513
                number + "Cry_" + form,                                     // :515
                speciesName == null ? null : speciesName + "Cry",           // :517
                number + "Cry" }) {                                         // :519
            if (candidate == null) {
                continue;
            }
            if (manifest.find(candidate) != null || findIgnoringCase(candidate) != null) {
                return candidate;
            }
        }
        return null;                                                        // :538
    }

    private void play(String logicalId, String type, int volume, int pitch) {
        Sound sound = sound(logicalId, type);
        if (sound == null) {
            return;
        }
        sound.play(volume(volume) * seFactor, pitch(pitch), 0f);
        if ("SE".equals(type)) {
            activeSe.add(sound);
        }
    }

    /**
     * {@code sePlayTime}'s cache: {@link SoundLength} parses a file once, keyed
     * by its absolute path (two manifest ids can resolve to the same file).
     */
    private final ObjectMap<String, Float> sePlayTimes = new ObjectMap<>();

    /**
     * {@code pbResolveAudioSE} + {@code getPlayTime} (PSystem_FileUtilities:441-469):
     * how long an SE file runs, in seconds.
     *
     * <p>{@code pbCryFrameLength} needs it to know how long a Pokemon's cry lasts
     * ({@code playtime = getPlayTime(pkmnwav)}), which decides how long
     * {@code BattlerFaintAnimation} waits before playing "Pkmn faint"
     * (PokeBattle_SceneAnimations:672-675).</p>
     *
     * @return the duration in seconds, or -1 when the file cannot be resolved
     */
    public float sePlayTime(String logicalId) {
        File file = fileOf(logicalId, "SE");
        if (file == null) {
            return -1f;
        }
        String key = file.getAbsolutePath();
        Float cached = sePlayTimes.get(key);
        if (cached != null) {
            return cached;
        }
        float seconds = SoundLength.duration(file);
        sePlayTimes.put(key, seconds);
        return seconds;
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
        // A loaded sample is reused as is: resolving the id again (a manifest
        // scan with a case-insensitive fallback plus a File#isFile stat) on every
        // play stalls cursor-heavy screens such as the party menu.
        Sound sound = sounds.get(logicalId);
        if (sound != null) {
            return sound;
        }
        if (unresolvedSounds.contains(logicalId)) {
            return null;                       // already reported once
        }
        File file = fileOf(logicalId, type);
        if (file == null) {
            if (ready()) {
                unresolvedSounds.add(logicalId);
            }
            return null;
        }
        sound = backend.newSound(file);
        sounds.put(logicalId, sound);
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
