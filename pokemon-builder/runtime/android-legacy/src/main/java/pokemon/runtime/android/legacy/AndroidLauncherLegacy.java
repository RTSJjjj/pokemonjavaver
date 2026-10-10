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

    private android.app.AlertDialog exitDialog;

    /** The Back key / back gesture asks before quitting, so a stray swipe does not close the game. */
    @Override
    public void onBackPressed() {
        runOnUiThread(() -> {
            if (exitDialog != null && exitDialog.isShowing()) {
                return;
            }
            exitDialog = new android.app.AlertDialog.Builder(this)
                    .setMessage("是否退出游戏？")
                    .setPositiveButton("退出", (dialog, which) -> finish())
                    .setNegativeButton("取消", null)
                    .create();
            exitDialog.show();
        });
    }

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
