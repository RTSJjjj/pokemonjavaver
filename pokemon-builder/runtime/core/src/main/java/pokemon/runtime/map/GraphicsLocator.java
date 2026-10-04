package pokemon.runtime.map;

import pokemon.runtime.data.GameDatabase;

import java.io.File;

/**
 * Locates the read-only Graphics/ tree the runtime renders (project3 sections
 * 9, 24).
 *
 * <p>L4: a packaged copy below the runtime data root (the build pipelines copy
 * {@code Graphics/} there for a distribution without the source project) wins
 * over the source RMXP project; during development the source project stays
 * the fallback. Fonts are resolved the same way (the message window needs
 * them too).</p>
 */
public final class GraphicsLocator {

    private final File sourceRoot;
    private final File dataRoot;
    private final File graphicsRoot;

    public GraphicsLocator(GameDatabase database) {
        this(
                database == null || database.project() == null || database.project().source == null
                        ? null
                        : new File(database.project().source.project),
                database == null ? null : database.dataRoot());
    }

    /** Test/embedding constructor: explicit source project + runtime data root. */
    public GraphicsLocator(File sourceRoot, File dataRoot) {
        this.sourceRoot = sourceRoot;
        this.dataRoot = dataRoot;
        File packed = dataRoot == null ? null : new File(dataRoot, "Graphics");
        this.graphicsRoot = packed != null && packed.isDirectory()
                ? packed
                : (sourceRoot == null ? null : new File(sourceRoot, "Graphics"));
    }

    public File graphicsRoot() {
        return graphicsRoot;
    }

    /** Case-insensitive file lookup inside a Graphics subdirectory. */
    public File find(String subDirectory, String name) {
        if (graphicsRoot == null || name == null || name.isEmpty()) {
            return null;
        }
        return lookup(new File(graphicsRoot, subDirectory), name);
    }

    /** Case-insensitive lookup of one of the project's Fonts (R6 message window). */
    public File font(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        if (dataRoot != null) {
            File packed = lookup(new File(dataRoot, "Fonts"), name);
            if (packed != null) {
                return packed;
            }
        }
        return sourceRoot == null ? null : lookup(new File(sourceRoot, "Fonts"), name);
    }

    private static File lookup(File directory, String name) {
        File exact = new File(directory, name);
        if (exact.isFile()) {
            return exact;
        }
        File[] entries = directory.listFiles();
        if (entries == null) {
            return null;
        }
        for (File entry : entries) {
            if (entry.isFile() && entry.getName().equalsIgnoreCase(name)) {
                return entry;
            }
        }
        return null;
    }
}
