package pokemon.runtime.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.IntBuffer;
import java.util.Date;

/**
 * Field diagnostics: a device that crashes or shows a white screen leaves {@code startup.log} (device, graphics
 * driver, texture limit, memory) and {@code crash.log} (every uncaught exception with its stack) in the user directory,
 * so a failure on a model nobody owns can be diagnosed from two small files instead of being guessed at.
 */
public final class CrashLog {

    private static boolean installed;

    private CrashLog() { }

    /** Records the environment and routes uncaught exceptions of any thread (the GL thread included) to crash.log. */
    public static synchronized void install(StoragePort storage) {
        if (installed) {
            return;
        }
        installed = true;
        final File dir = new File(storage.userDirectory());
        write(new File(dir, "startup.log"), false, describe());
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            StringWriter text = new StringWriter();
            error.printStackTrace(new PrintWriter(text));
            write(new File(dir, "crash.log"), true, new Date() + " thread " + thread.getName() + "\n" + text + "\n");
            if (previous != null) {
                previous.uncaughtException(thread, error);
            }
        });
    }

    /** Writes a caught failure the game survives (a save that could not be written, a screen that fell back). */
    public static void note(StoragePort storage, String what, Throwable error) {
        StringWriter text = new StringWriter();
        if (error != null) {
            error.printStackTrace(new PrintWriter(text));
        }
        write(new File(storage.userDirectory(), "crash.log"), true, new Date() + " " + what + "\n" + text + "\n");
    }

    private static String describe() {
        StringBuilder out = new StringBuilder();
        out.append("time ").append(new Date()).append('\n');
        out.append("runtime ").append(PokemonGame.RUNTIME_VERSION).append('\n');
        out.append("os ").append(System.getProperty("os.name")).append(' ').append(System.getProperty("os.version")).append('\n');
        out.append("java ").append(System.getProperty("java.vm.name")).append(' ').append(System.getProperty("java.version")).append('\n');
        out.append("heap max MB ").append(Runtime.getRuntime().maxMemory() / (1024 * 1024)).append('\n');
        try {
            Class<?> build = Class.forName("android.os.Build");
            out.append("device ").append(build.getField("MANUFACTURER").get(null)).append(' ')
                    .append(build.getField("MODEL").get(null)).append('\n');
            Class<?> version = Class.forName("android.os.Build$VERSION");
            out.append("android ").append(version.getField("RELEASE").get(null)).append(" api ")
                    .append(version.getField("SDK_INT").get(null)).append('\n');
        } catch (ReflectiveOperationException notAndroid) {
            // desktop
        }
        try {
            if (Gdx.graphics != null) {
                out.append("screen ").append(Gdx.graphics.getWidth()).append('x').append(Gdx.graphics.getHeight()).append('\n');
                out.append("gl ").append(Gdx.graphics.getGLVersion().getVendorString()).append(" | ")
                        .append(Gdx.graphics.getGLVersion().getRendererString()).append(" | ")
                        .append(Gdx.graphics.getGLVersion().getDebugVersionString()).append('\n');
                IntBuffer size = com.badlogic.gdx.utils.BufferUtils.newIntBuffer(16);
                Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, size);
                out.append("max texture size ").append(size.get(0)).append('\n');
            }
        } catch (RuntimeException | LinkageError error) {
            out.append("gl info unavailable: ").append(error).append('\n');
        }
        return out.toString();
    }

    private static void write(File file, boolean append, String text) {
        try {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(file, append)) {
                writer.write(text);
            }
        } catch (java.io.IOException | RuntimeException ignored) {
            // diagnostics must never be the thing that fails
        }
    }
}
