package pokemon.runtime.data;

import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * generated/audio/audio-manifest.json, produced by the audio phase (A3).
 *
 * <p>Until the audio compiler runs the manifest simply does not exist and
 * {@link #loaded} stays false: the runtime says so instead of pretending
 * audio is available.</p>
 */
public final class AudioManifestData {

    public boolean loaded;
    public String preset;
    public ObjectMap<String, Entry> entries = new ObjectMap<>();

    /** One logical audio resource: logical id -> physical file under generated/audio/. */
    public static final class Entry {
        public String type;
        public String file;
        public String source;
        public String encoding;
        public int loopStartSamples = -1;
        public int loopEndSamples = -1;
        /** Source sample rate of the loop sample positions (informational). */
        public float sampleRate;
        /** L8.1: loop region in seconds (informational; the builder splits the file). */
        public float loopStartSeconds = -1f;
        public float loopEndSeconds = -1f;
        /** L8.1: intro segment played once before {@link #file} loops. */
        public String introFile;

        public boolean hasLoopPoints() {
            return loopStartSamples >= 0 && loopEndSamples > loopStartSamples;
        }

        /** A tagged BGM with an intro segment that plays once, then loops. */
        public boolean hasIntro() {
            return introFile != null && !introFile.isEmpty();
        }

        public static Entry parse(JsonValue node) {
            Entry entry = new Entry();
            entry.type = node.getString("type", null);
            entry.file = node.getString("file", null);
            entry.source = node.getString("source", null);
            entry.encoding = node.getString("encoding", null);
            entry.loopStartSamples = node.getInt("loopStartSamples", -1);
            entry.loopEndSamples = node.getInt("loopEndSamples", -1);
            entry.sampleRate = node.getFloat("sampleRate", 0f);
            entry.loopStartSeconds = node.getFloat("loopStartSeconds", -1f);
            entry.loopEndSeconds = node.getFloat("loopEndSeconds", -1f);
            entry.introFile = node.getString("introFile", null);
            return entry;
        }
    }

    public static AudioManifestData parse(JsonValue root) {
        AudioManifestData manifest = new AudioManifestData();
        manifest.loaded = true;
        manifest.preset = root.getString("preset", null);
        JsonValue audio = root.get("audio");
        if (audio != null && audio.isObject()) {
            for (JsonValue entry = audio.child; entry != null; entry = entry.next) {
                manifest.entries.put(entry.name, Entry.parse(entry));
            }
        }
        return manifest;
    }

    /** Missing manifest: never a hard error, audio is simply unavailable. */
    public static AudioManifestData notLoaded() {
        AudioManifestData manifest = new AudioManifestData();
        manifest.loaded = false;
        return manifest;
    }

    public Entry find(String logicalId) {
        if (logicalId == null || logicalId.isEmpty()) {
            return null;
        }
        return entries.get(logicalId);
    }
}