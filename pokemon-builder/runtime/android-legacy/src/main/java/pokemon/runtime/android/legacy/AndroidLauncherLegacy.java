package pokemon.runtime.android.legacy;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import pokemon.runtime.app.PokemonGame;

import java.io.File;

/**
 * Legacy Android placeholder (project3 section 50).
 *
 * <p>Stage 2 establishes this module only, so that core, the runtime data and
 * GameAction stay reusable for the real API 14 / ARMv7 backend later.
 * minSdk is 21 because the current libGDX android backend requires it (build
 * evidence); the true API 14 device work needs the older backend and stays a
 * separate milestone. The reuse contract itself is checked by
 * {@link LegacyRuntime} compiling against {@code :core}.</p>
 */
public final class AndroidLauncherLegacy extends AndroidApplication {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        initialize(new PokemonGame(dataDirectory(), -1, new LegacyKeyStateSource()), config);
    }

    /**
     * Same data location as the modern backend: {@code runtime-data/} or
     * {@code generated/} below the app's external files directory.
     */
    private String dataDirectory() {
        File base = getExternalFilesDir(null);
        if (base == null) {
            base = getFilesDir();
        }
        return base.getAbsolutePath();
    }
}
