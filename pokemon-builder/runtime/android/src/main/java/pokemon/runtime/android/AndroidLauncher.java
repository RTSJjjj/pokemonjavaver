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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String dataRoot = dataDirectory();
        // Saves and settings live in the app's files directory too: Android has no usable user.home, and the default
        // location (<user.home>/.pokemon-runtime) is not writable there - a save would silently fail.
        System.setProperty(StoragePort.USER_DIR_PROPERTY, new File(dataRoot, "user").getAbsolutePath());

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
