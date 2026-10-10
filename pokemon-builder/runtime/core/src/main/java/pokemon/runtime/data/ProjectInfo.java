package pokemon.runtime.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.File;

/**
 * The generated/ project.json manifest (Debug IR root). Bound by name; unknown
 * fields are ignored so the manifest can grow without breaking the runtime.
 */
public class ProjectInfo {

    public String format;
    public String kind;
    public String projectName;
    public String builderVersion;
    public String generatedAt;
    public String status;
    public SourceInfo source;
    public Counts counts;
    public Outputs outputs;
    /**
     * Project-specific runtime knobs (R6.12): the Java runtime used to hardcode
     * the hero's walk/run charsets, the message font and the screen size. The
     * Builder now exports them from PBS/metadata.txt, Fonts/ and the project's
     * Settings script, so pointing the Builder at another project needs no
     * runtime edits. Absent in manifests from older builds.
     */
    public RuntimeProfile runtime;

    /**
     * R6.21: the source project's own screen size from a generated root's
     * project.json ({@code runtime.screenWidth/Height}), or null when the
     * manifest or its runtime profile is missing. The launcher uses this as the
     * default render resolution, so the runtime draws the original RMXP framing
     * without any hardcoded project value.
     */
    public static int[] screenSize(File dataRoot) {
        if (dataRoot == null) {
            return null;
        }
        File manifest = new File(dataRoot, "project.json");
        if (!manifest.isFile()) {
            return null;
        }
        try {
            JsonValue root = new JsonReader().parse(pokemon.runtime.data.ResourceCrypto.handle(manifest));
            JsonValue runtime = root.get("runtime");
            if (runtime == null) {
                return null;
            }
            int width = runtime.getInt("screenWidth", 0);
            int height = runtime.getInt("screenHeight", 0);
            return width > 0 && height > 0 ? new int[] {width, height} : null;
        } catch (RuntimeException error) {
            return null;
        }
    }

    public static class SourceInfo {
        public String project;
        public String dataDir;
    }

    public static class Counts {
        public int mapFiles;
        public int maps;
        public int events;
        public int commonEvents;
        public int scriptBlocks;
        public int tilesets;
        public int uniqueApis;
    }

    public static class Outputs {
        public String maps;
        public String events;
        public String commonEvents;
        public String scripts;
        public String apis;
        public String build;
        public String tilesets;
        public String system;
    }

    public static class RuntimeProfile {
        public String playerCharset;
        public String runningCharset;
        public String messageFont;
        public int screenWidth;
        public int screenHeight;
        /** PlayerA..PlayerH graphics (id 0..7) for pbChangePlayer. */
        public com.badlogic.gdx.utils.Array<PlayerGraphic> players;

        public boolean hasPlayerCharset() {
            return playerCharset != null && !playerCharset.isEmpty();
        }

        public boolean hasRunningCharset() {
            return runningCharset != null && !runningCharset.isEmpty();
        }

        public boolean hasMessageFont() {
            return messageFont != null && !messageFont.isEmpty();
        }

        /** The graphics of one player id, or null when the manifest lacks it. */
        public PlayerGraphic player(int id) {
            return players == null || id < 0 || id >= players.size ? null : players.get(id);
        }
    }

    /** One PBS/metadata.txt PlayerX entry. */
    public static class PlayerGraphic {
        public String trainerType;
        public String charset;
        public String bikeCharset;
        public String surfCharset;
        public String runningCharset;
        public String fishCharset;
        /** PlayerX field 8: the fishing sheet used while surfing ({@code meta[7]}); null in data built before it was exported. */
        public String surfFishCharset;
    }
}
