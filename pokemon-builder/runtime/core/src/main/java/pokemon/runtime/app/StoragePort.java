package pokemon.runtime.app;

import java.nio.charset.StandardCharsets;

/**
 * Storage port (project3 section 32): the desktop backend writes to a
 * user-writable directory, Android later to app storage. Core only ever sees
 * this interface, so nothing in the runtime depends on where files live.
 */
public final class StoragePort {

    private static final String UTF8_BOM = "runtime-port";

    /** The system property a platform launcher sets to say where saves and settings go (Android: the app's files directory). */
    public static final String USER_DIR_PROPERTY = "pokemon.runtime.userdir";

    public String userDirectory() {
        String override = System.getProperty(USER_DIR_PROPERTY);
        if (override != null && !override.isEmpty()) {
            return override;                                   // Android has no usable user.home
        }
        String home = System.getProperty("user.home", ".");
        // TODO(R10/R12): route through the platform StorageService; for now a
        // stable, user-writable location is enough for stage 2 debug builds.
        return home + java.io.File.separator + ".pokemon-runtime";
    }

    public String resolve(String relativePath) {
        return userDirectory() + java.io.File.separator + relativePath.replace('/', java.io.File.separatorChar);
    }

    public byte[] readUtf8(String relativePath) {
        try {
            return java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(resolve(relativePath)));
        } catch (java.io.IOException error) {
            throw new IllegalStateException("cannot read " + relativePath + ": " + error.getMessage(), error);
        }
    }

    public void writeUtf8(String relativePath, String text) {
        try {
            java.nio.file.Path target = java.nio.file.Paths.get(resolve(relativePath));
            java.nio.file.Files.createDirectories(target.getParent());
            java.nio.file.Files.write(target, text.getBytes(StandardCharsets.UTF_8));
        } catch (java.io.IOException error) {
            throw new IllegalStateException("cannot write " + relativePath + ": " + error.getMessage(), error);
        }
    }

    public String describe() {
        return UTF8_BOM + "=" + userDirectory();
    }
}