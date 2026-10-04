package pokemon.runtime.ui.menu;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import pokemon.runtime.app.StoragePort;

/**
 * L1: user options shown by the pause menu Options screen. Persisted as
 * {@code settings.json} through the storage port, so a restart keeps the
 * choices. Volumes are percentages (0..100) applied by the AudioManager on top
 * of the volumes the project data asks for.
 */
public final class GameSettings {

    public enum Row {
        BGM_VOLUME,
        SE_VOLUME,
        BGS_VOLUME,
        FULLSCREEN;

        /** The label the options screen shows (project language: Chinese). */
        public String label() {
            switch (this) {
                case BGM_VOLUME:
                    return "背景音乐";
                case SE_VOLUME:
                    return "音效";
                case BGS_VOLUME:
                    return "环境音";
                case FULLSCREEN:
                    return "全屏";
                default:
                    return name();
            }
        }
    }

    private static final String FILE = "settings.json";

    public int bgmVolume = 100;
    public int seVolume = 100;
    public int bgsVolume = 100;
    public boolean fullscreen = false;

    /** Loads the file, falling back to the defaults when it is missing/broken. */
    public static GameSettings load(StoragePort storage) {
        GameSettings settings = new GameSettings();
        if (storage == null) {
            return settings;
        }
        try {
            String json = new String(storage.readUtf8(FILE), java.nio.charset.StandardCharsets.UTF_8);
            JsonValue root = new JsonReader().parse(json);
            settings.bgmVolume = clamp(root.getInt("bgmVolume", 100));
            settings.seVolume = clamp(root.getInt("seVolume", 100));
            settings.bgsVolume = clamp(root.getInt("bgsVolume", 100));
            settings.fullscreen = root.getBoolean("fullscreen", false);
        } catch (RuntimeException error) {
            // missing or unreadable: the defaults are a valid state
        }
        return settings;
    }

    public void save(StoragePort storage) {
        if (storage == null) {
            return;
        }
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"bgmVolume\": ").append(bgmVolume).append(",\n");
        json.append("  \"seVolume\": ").append(seVolume).append(",\n");
        json.append("  \"bgsVolume\": ").append(bgsVolume).append(",\n");
        json.append("  \"fullscreen\": ").append(fullscreen).append("\n");
        json.append("}\n");
        try {
            storage.writeUtf8(FILE, json.toString());
        } catch (RuntimeException error) {
            // A read-only profile must not crash the game; the runtime keeps the
            // in-memory values until the next start.
        }
    }

    /** LEFT/RIGHT adjustment of one row (returns the new display value). */
    public String adjust(Row row, int delta) {
        switch (row) {
            case BGM_VOLUME:
                bgmVolume = clamp(bgmVolume + delta * 10);
                return bgmVolume + "%";
            case SE_VOLUME:
                seVolume = clamp(seVolume + delta * 10);
                return seVolume + "%";
            case BGS_VOLUME:
                bgsVolume = clamp(bgsVolume + delta * 10);
                return bgsVolume + "%";
            case FULLSCREEN:
                fullscreen = !fullscreen;
                return fullscreen ? "开" : "关";
            default:
                return "";
        }
    }

    /** Current display value of a row. */
    public String value(Row row) {
        switch (row) {
            case BGM_VOLUME:
                return bgmVolume + "%";
            case SE_VOLUME:
                return seVolume + "%";
            case BGS_VOLUME:
                return bgsVolume + "%";
            case FULLSCREEN:
                return fullscreen ? "开" : "关";
            default:
                return "";
        }
    }

    /** BGM volume percentage applied by the audio manager. */
    public float bgmFactor() {
        return bgmVolume / 100f;
    }

    public float seFactor() {
        return seVolume / 100f;
    }

    public float bgsFactor() {
        return bgsVolume / 100f;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
