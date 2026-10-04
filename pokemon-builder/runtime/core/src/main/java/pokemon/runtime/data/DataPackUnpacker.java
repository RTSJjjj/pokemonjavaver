package pokemon.runtime.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * L3: the first-run unpack of the runtime data pack. The Android APK carries
 * one zip holding {@code generated/} + {@code Graphics/} + {@code Fonts/} as an
 * asset; the launcher streams it in here on first launch (and after the pack
 * changes). Pure Java (no libGDX / Android APIs) so the desktop tests cover
 * the unpack rules headlessly.
 */
public final class DataPackUnpacker {

    /** Marker file written into the target root after a successful unpack. */
    public static final String MARKER_FILE = ".data-pack-version";

    private DataPackUnpacker() {
    }

    /** True when {@code targetDir} already holds this pack version. */
    public static boolean isCurrent(File targetDir, String version) {
        if (targetDir == null || version == null || version.isEmpty()) {
            return false;
        }
        File marker = new File(targetDir, MARKER_FILE);
        if (!marker.isFile()) {
            return false;
        }
        try (InputStream in = new FileInputStream(marker)) {
            byte[] bytes = new byte[(int) Math.min(marker.length(), 4096L)];
            int read = in.read(bytes);
            return read > 0 && new String(bytes, 0, read, StandardCharsets.UTF_8).trim().equals(version);
        } catch (IOException error) {
            return false;
        }
    }

    /**
     * Unpacks every entry of {@code zipStream} below {@code targetDir}, then
     * writes the version marker. Entry names that escape the target directory
     * (zip-slip) abort with an {@link IOException}; the already written files
     * stay, and a later run overwrites them.
     *
     * @return number of files written
     */
    public static int unpack(InputStream zipStream, File targetDir, String version) throws IOException {
        if (zipStream == null) {
            throw new IOException("no data pack stream");
        }
        if (targetDir == null) {
            throw new IOException("no target directory");
        }
        if (!targetDir.isDirectory() && !targetDir.mkdirs() && !targetDir.isDirectory()) {
            throw new IOException("cannot create " + targetDir);
        }
        String root = targetDir.getCanonicalPath() + File.separator;
        int files = 0;
        byte[] buffer = new byte[64 * 1024];
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(zipStream))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                File out = new File(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    out.mkdirs();
                } else {
                    if (!out.getCanonicalPath().startsWith(root)) {
                        throw new IOException("zip entry escapes the target: " + entry.getName());
                    }
                    File parent = out.getParentFile();
                    if (parent != null) {
                        parent.mkdirs();
                    }
                    try (OutputStream os = new BufferedOutputStream(new FileOutputStream(out))) {
                        int count;
                        while ((count = zip.read(buffer)) > 0) {
                            os.write(buffer, 0, count);
                        }
                    }
                    files++;
                }
                zip.closeEntry();
            }
        }
        if (version != null && !version.isEmpty()) {
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(new File(targetDir, MARKER_FILE)),
                    StandardCharsets.UTF_8)) {
                writer.write(version);
            }
        }
        return files;
    }
}
