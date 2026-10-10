package pokemon.runtime.android;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import pokemon.runtime.data.DataPackUnpacker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The launcher activity: makes the game data ready, then starts {@link AndroidLauncher}.
 *
 * <p>The data pack ({@code assets/runtime-data.zip}, several hundred MB) unpacks into the app's files directory once per
 * pack version. That used to run inside the game activity's {@code onCreate}, so the screen stayed black for the whole
 * unpack. Here it runs on a worker thread behind a progress bar. Delivery modes (same as before): the APK asset, or the
 * same zip (+ {@code runtime-data.version}) copied into the files directory; with neither, the adb-push layout is left
 * untouched.</p>
 */
public final class PrepareActivity extends Activity {

    private static final String TAG = "PokemonLauncher";
    private static final String PACK_ASSET = "runtime-data.zip";
    private static final String PACK_VERSION_ASSET = "runtime-data.version";

    private TextView status;
    private ProgressBar bar;
    private volatile boolean leaving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final File target = dataDirectory();
        buildView();
        new Thread(() -> {
            try {
                unpackDataPack(target);
            } catch (IOException | RuntimeException error) {
                Log.e(TAG, "data pack unpack failed; the game will report missing data", error);
            }
            runOnUiThread(this::startGame);
        }, "pokemon-unpack").start();
    }

    private void buildView() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.rgb(12, 14, 24));
        int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());
        layout.setPadding(pad, pad, pad, pad);

        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        status.setGravity(Gravity.CENTER);
        status.setText("正在准备游戏数据…");
        layout.addView(status);

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(1000);
        bar.setIndeterminate(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 360, getResources().getDisplayMetrics()),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = pad / 2;
        layout.addView(bar, params);

        TextView note = new TextView(this);
        note.setTextColor(Color.argb(180, 255, 255, 255));
        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        note.setGravity(Gravity.CENTER);
        note.setText("第一次启动或数据更新后需要几分钟，请不要退出。");
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        noteParams.topMargin = pad / 2;
        layout.addView(note, noteParams);
        setContentView(layout);
    }

    private void startGame() {
        if (leaving) {
            return;
        }
        leaving = true;
        startActivity(new Intent(this, AndroidLauncher.class));
        finish();
    }

    /** The external files directory: visible to adb push without asking for storage permissions. */
    private File dataDirectory() {
        File base = getExternalFilesDir(null);
        return base != null ? base : getFilesDir();
    }

    private void progress(final String text, final int permille) {
        runOnUiThread(() -> {
            status.setText(text);
            bar.setIndeterminate(false);
            bar.setProgress(permille);
        });
    }

    private void unpackDataPack(File target) throws IOException {
        AssetManager assets = getAssets();
        boolean hasAssetPack = false;
        boolean hasAssetVersion = false;
        for (String entry : assets.list("")) {
            hasAssetPack |= PACK_ASSET.equals(entry);
            hasAssetVersion |= PACK_VERSION_ASSET.equals(entry);
        }
        File packFile = new File(target, PACK_ASSET);
        boolean hasFilePack = !hasAssetPack && packFile.isFile();
        if (!hasAssetPack && !hasFilePack) {
            return; // adb-push workflow: the data (or its absence) is final
        }

        String version = null;
        if (hasAssetPack && hasAssetVersion) {
            version = readAssetText(assets, PACK_VERSION_ASSET);
        }
        if (version == null || version.isEmpty()) {
            File versionFile = new File(target, PACK_VERSION_ASSET);
            if (versionFile.isFile()) {
                version = new String(readFileBytes(versionFile), "UTF-8").trim();
            }
        }
        if (version == null || version.isEmpty()) {
            // Without a version file the pack identity is its size+mtime, so replacing the zip re-unpacks while a
            // relaunch does not.
            version = packFile.isFile() ? packFile.length() + "-" + packFile.lastModified() : "asset-default";
        }
        if (DataPackUnpacker.isCurrent(target, version)) {
            Log.i(TAG, "data pack is current: " + version);
            return;
        }

        long total = 0L;
        if (hasAssetPack) {
            try (AssetFileDescriptor fd = assets.openFd(PACK_ASSET)) {
                total = fd.getLength();
            } catch (IOException compressed) {
                total = 0L;                                  // a compressed asset has no length: indeterminate bar
            }
        } else {
            total = packFile.length();
        }
        final long size = total;
        long started = System.currentTimeMillis();
        try (InputStream raw = hasAssetPack ? assets.open(PACK_ASSET) : new java.io.FileInputStream(packFile);
             InputStream stream = new CountingStream(raw, size)) {
            int files = DataPackUnpacker.unpack(stream, target, version);
            Log.i(TAG, "data pack unpacked: " + files + " files, version " + version + ", "
                    + (System.currentTimeMillis() - started) + " ms");
        }
    }

    /** Reports how much of the pack has been read: the unpack progress. */
    private final class CountingStream extends FilterInputStream {
        private final long total;
        private long read;
        private int lastPermille = -1;

        CountingStream(InputStream in, long total) {
            super(in);
            this.total = total;
        }

        private void count(long n) {
            if (n <= 0 || total <= 0) {
                return;
            }
            read += n;
            int permille = (int) Math.min(1000L, read * 1000L / total);
            if (permille != lastPermille) {
                lastPermille = permille;
                progress("正在准备游戏数据… " + (permille / 10) + "%", permille);
            }
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b >= 0) {
                count(1);
            }
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int n = super.read(buffer, offset, length);
            count(n);
            return n;
        }
    }

    private static String readAssetText(AssetManager assets, String name) throws IOException {
        try (InputStream in = assets.open(name)) {
            return new String(readAll(in), "UTF-8").trim();
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
