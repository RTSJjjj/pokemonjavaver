package pokemon.runtime.android;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import pokemon.runtime.app.PokemonGame;
import pokemon.runtime.app.StoragePort;
import pokemon.runtime.input.touch.TouchControls;

import java.io.File;

/**
 * Modern Android entry point (project3 sections 48-49, 54): API 21+, the
 * current libGDX Android backend, the same {@link PokemonGame} and the same
 * {@code generated/} data as the desktop build.
 *
 * <p>Runtime data lives where an app may read plain files: the external files
 * directory of this package. Two delivery modes share that directory:</p>
 * <ul>
 *   <li><b>L3 data pack</b> (build-android): the APK carries
 *       {@code assets/runtime-data.zip} (+ {@code runtime-data.version}); the
 *       launcher unpacks it here on first launch and after every pack change
 *       (marker {@code .data-pack-version}).</li>
 *   <li><b>adb push</b> (R13 workflow): push {@code runtime-data/} or
 *       {@code generated/} below this directory once; no pack asset, no
 *       unpacking.</li>
 * </ul>
 */
public final class AndroidLauncher extends AndroidApplication {

    private File lifecycleLog;

    /** Appends one line to lifecycle.log (user directory): what the system did to the app, for reports of "it closes in the background". */
    private void lifecycle(String line) {
        if (lifecycleLog == null) {
            return;
        }
        try (java.io.FileWriter writer = new java.io.FileWriter(lifecycleLog, true)) {
            writer.write(new java.util.Date() + " " + line + "\n");
        } catch (java.io.IOException | RuntimeException ignored) {
            // diagnostics only
        }
    }

    @Override
    protected void onPause() {
        lifecycle("onPause finishing=" + isFinishing());
        super.onPause();
    }

    @Override
    protected void onStop() {
        lifecycle("onStop finishing=" + isFinishing());
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        lifecycle("onResume");
    }

    @Override
    protected void onDestroy() {
        lifecycle("onDestroy finishing=" + isFinishing() + " changingConfigurations=" + isChangingConfigurations());
        super.onDestroy();
    }

    @Override
    public void onTrimMemory(int level) {
        lifecycle("onTrimMemory level=" + level);
        super.onTrimMemory(level);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String dataRoot = dataDirectory();
        // Saves and settings live in the app's files directory too: Android has no usable user.home, and the default
        // location (<user.home>/.pokemon-runtime) is not writable there - a save would silently fail.
        System.setProperty(StoragePort.USER_DIR_PROPERTY, new File(dataRoot, "user").getAbsolutePath());
        File userDir = new File(dataRoot, "user");
        userDir.mkdirs();
        File previous = new File(userDir, "lifecycle.prev.log");
        lifecycleLog = new File(userDir, "lifecycle.log");
        if (lifecycleLog.isFile()) {
            previous.delete();
            lifecycleLog.renameTo(previous);                 // keep the last run's lines next to this run's
        }
        lifecycle("onCreate");

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        // The runtime polls the logical actions itself (section 51); the
        // sensors stay off until a feature actually needs them.
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useGyroscope = false;
        config.useImmersiveMode = true;

        // Release plan P1: the screen keys stand in for physical keys, so a
        // keyboard still works and the game sees ordinary GameActions.
        TouchControls touch = new TouchControls();
        initialize(new PokemonGame(dataRoot, -1, touch.wrap(new AndroidKeyStateSource()), touch), config);
    }

    /**
     * Root the runtime reads (R2): {@code runtime-data/} or {@code generated/}
     * below this directory. The external files directory keeps the data
     * visible to {@code adb push} without asking for storage permissions.
     */
    private String dataDirectory() {
        File base = getExternalFilesDir(null);
        if (base == null) {
            base = getFilesDir();
        }
        return base.getAbsolutePath();
    }
}
