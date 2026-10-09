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

    /** The names (without ".png") of the PNG files of a Graphics subdirectory, in file name order. */
    public java.util.List<String> pngNames(String subDirectory) {
        java.util.List<String> names = new java.util.ArrayList<>();
        if (graphicsRoot == null) {
            return names;
        }
        File directory = new File(graphicsRoot, subDirectory);
        lookup(directory, ".");                                                       // makes sure the listing exists
        for (File file : LISTINGS.getOrDefault(directory.getPath(), java.util.Collections.emptyMap()).values()) {
            String name = file.getName();
            if (name.toLowerCase(java.util.Locale.ROOT).endsWith(".png")) {
                names.add(name.substring(0, name.length() - 4));
            }
        }
        java.util.Collections.sort(names);
        return names;
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

    /**
     * One listing per directory, kept for the life of the process: {@code lookup} used to ask the disk
     * ({@code isFile}, and a {@code listFiles()} scan of the whole directory - 4700 files in Icons - when the case did not
     * match) for every candidate name of every icon, so a bag page of items cost thousands of file system calls.
     * Keys are lower-case names; files added to the folder while the game runs are not seen until it restarts.
     */
    private static final java.util.Map<String, java.util.Map<String, File>> LISTINGS =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static File lookup(File directory, String name) {
        java.util.Map<String, File> listing = LISTINGS.computeIfAbsent(directory.getPath(), path -> {
            java.util.Map<String, File> map = new java.util.HashMap<>();
            File[] entries = directory.listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    if (entry.isFile()) {
                        map.putIfAbsent(entry.getName().toLowerCase(java.util.Locale.ROOT), entry);
                    }
                }
            }
            return map;
        });
        File exact = listing.get(name.toLowerCase(java.util.Locale.ROOT));
        if (exact != null) {
            return exact;
        }
        // The name may carry a sub folder ("Pictures/Foo.png" under a directory): resolve it on the disk like before.
        if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0) {
            File nested = new File(directory, name);
            return nested.isFile() ? nested : null;
        }
        return null;
    }
}
