package pokemon.runtime.android;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import pokemon.runtime.app.PokemonGame;
import pokemon.runtime.data.DataPackUnpacker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

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

    private static final String TAG = "PokemonLauncher";
    private static final String PACK_ASSET = "runtime-data.zip";
    private static final String PACK_VERSION_ASSET = "runtime-data.version";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String dataRoot = dataDirectory();
        unpackDataPack(dataRoot);

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        // The runtime polls the logical actions itself (section 51); the
        // sensors stay off until a feature actually needs them.
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useGyroscope = false;
        config.useImmersiveMode = true;

        initialize(new PokemonGame(dataRoot, -1, new AndroidKeyStateSource()), config);
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

    /**
     * L3: unpacks the data pack into the data root once per pack version. Two
     * delivery modes: the fat APK carries {@code assets/runtime-data.zip}, or
     * the pack zip (with its optional {@code runtime-data.version} file) is
     * copied next to the game data (adb push / file manager). Without any pack
     * the R13 adb-push workflow stays untouched. Runs before
     * {@code initialize} so the game never boots against a half-unpacked tree.
     */
    private void unpackDataPack(String dataRoot) {
        try {
            AssetManager assets = getAssets();
            boolean hasAssetPack = false;
            boolean hasAssetVersion = false;
            for (String entry : assets.list("")) {
                hasAssetPack |= PACK_ASSET.equals(entry);
                hasAssetVersion |= PACK_VERSION_ASSET.equals(entry);
            }
            File packFile = new File(dataRoot, PACK_ASSET);
            boolean hasFilePack = !hasAssetPack && packFile.isFile();
            if (!hasAssetPack && !hasFilePack) {
                return; // adb-push workflow: the data (or its absence) is final
            }

            String version = null;
            if (hasAssetPack && hasAssetVersion) {
                version = readAssetText(assets, PACK_VERSION_ASSET);
            }
            if (version == null || version.isEmpty()) {
                File versionFile = new File(dataRoot, PACK_VERSION_ASSET);
                if (versionFile.isFile()) {
                    version = new String(readFileBytes(versionFile), "UTF-8").trim();
                }
            }
            if (version == null || version.isEmpty()) {
                // Without a version file the pack identity is its size+mtime,
                // so replacing the zip re-unpacks while a relaunch does not.
                version = packFile.isFile() ? packFile.length() + "-" + packFile.lastModified() : "asset-default";
            }

            File target = new File(dataRoot);
            if (DataPackUnpacker.isCurrent(target, version)) {
                Log.i(TAG, "data pack is current: " + version);
                return;
            }
            Toast.makeText(this, "首次启动：正在解包游戏数据，请稍候…", Toast.LENGTH_LONG).show();
            long started = System.currentTimeMillis();
            try (InputStream stream = hasAssetPack ? assets.open(PACK_ASSET)
                    : new java.io.FileInputStream(packFile)) {
                int files = DataPackUnpacker.unpack(stream, target, version);
                Log.i(TAG, "data pack unpacked: " + files + " files, version " + version + ", "
                        + (System.currentTimeMillis() - started) + " ms");
            }
        } catch (IOException error) {
            Log.e(TAG, "data pack unpack failed; the game will report missing data", error);
        }
    }

    private static String readAssetText(AssetManager assets, String name) throws IOException {
        return new String(readAssetBytes(assets, name), "UTF-8").trim();
    }

    private static byte[] readAssetBytes(AssetManager assets, String name) throws IOException {
        try (InputStream in = assets.open(name)) {
            return readAll(in);
        }
    }

    private static byte[] readFileBytes(File file) throws IOException {
        try (InputStream in = new java.io.FileInputStream(file)) {
            return readAll(in);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = in.read(buffer)) > 0) {
            out.write(buffer, 0, count);
        }
        return out.toByteArray();
    }
}
