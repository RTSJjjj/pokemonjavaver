package pokemon.runtime.data;

import com.badlogic.gdx.files.FileHandle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * P3: the packaged game's files (data JSON, pictures, fonts, audio) are stored encrypted. Every game file is read through
 * here: a file that starts with the 16-byte header {@code "PKRE" 01 000000 nonce(8)} is AES-128-CTR ciphertext (the counter
 * block is the nonce followed by a zero 64-bit counter), anything else is read as it is - so the development tree and the
 * tests keep working with plain files.
 *
 * <p>The key is built from three fragments of the code, not stored as one constant (the same function is in
 * builder/src/resource-crypto.js). This keeps the files from being opened by ordinary unpacking tools; it does not stop a
 * determined person with the program in hand, because the key has to be in the program to play the game.</p>
 */
public final class ResourceCrypto {

    public static final int HEADER = 16;
    private static final byte[] MAGIC = {'P', 'K', 'R', 'E', 1, 0, 0, 0};

    private static final int[] A = {
        0xD3, 0xC3, 0xF6, 0xFC, 0x22, 0x4C, 0x47, 0x96, 0x9E, 0x2A, 0x12, 0x50, 0x15, 0xA2, 0x80, 0xDB,
        0x7D, 0x11, 0x24, 0x10, 0x96, 0xD9, 0xB6, 0x42, 0xEF, 0xD9, 0x67, 0xDE, 0x3C, 0xDE, 0x5A, 0x48};
    private static final int[] B = {
        0x39, 0xFB, 0xE5, 0xA6, 0x48, 0x23, 0x66, 0x07, 0x22, 0xC4, 0x67, 0x5B, 0xE9, 0x27, 0xE5, 0x14,
        0xBA, 0x1C, 0xF0, 0xAF, 0xA8, 0x52, 0x9E, 0xAE, 0x5C, 0x21, 0x65, 0x3E, 0x5F, 0x35, 0xF0, 0xA9};
    private static final int[] C = {
        0x85, 0xF3, 0xF5, 0x98, 0xF1, 0x2E, 0xD0, 0xCE, 0x31, 0x3B, 0x4D, 0x79, 0x64, 0xB9, 0x05, 0xB6,
        0x47, 0x81, 0x68, 0xE4, 0xE8, 0xA0, 0x3B, 0x62, 0x42, 0xA6, 0x08, 0x4E, 0x2C, 0xF2, 0x20, 0x37};

    private static byte[] key;

    private ResourceCrypto() {
    }

    /** {@code SHA-256(A[i] ^ B[(i+3)%32] ^ C[31-i])[0..16]}. */
    static synchronized byte[] key() {
        if (key == null) {
            byte[] mix = new byte[32];
            for (int i = 0; i < 32; i++) {
                mix[i] = (byte) (A[i] ^ B[(i + 3) % 32] ^ C[31 - i]);
            }
            try {
                key = Arrays.copyOf(MessageDigest.getInstance("SHA-256").digest(mix), 16);
            } catch (java.security.NoSuchAlgorithmException error) {
                throw new IllegalStateException(error);
            }
        }
        return key;
    }

    /** True when the file starts with the encrypted-file header. */
    public static boolean isEncrypted(File file) {
        if (file == null || !file.isFile() || file.length() < HEADER) {
            return false;
        }
        try (InputStream in = new FileInputStream(file)) {
            byte[] head = new byte[HEADER];
            return readFully(in, head) && hasMagic(head);
        } catch (IOException error) {
            return false;
        }
    }

    private static boolean hasMagic(byte[] head) {
        for (int i = 0; i < MAGIC.length; i++) {
            if (head[i] != MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean readFully(InputStream in, byte[] buffer) throws IOException {
        int off = 0;
        while (off < buffer.length) {
            int n = in.read(buffer, off, buffer.length - off);
            if (n < 0) {
                return false;
            }
            off += n;
        }
        return true;
    }

    /** A stream of the file's plain content (decrypted when the file is encrypted). The caller closes it. */
    public static InputStream open(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            byte[] head = new byte[HEADER];
            if (readFully(in, head) && hasMagic(head)) {
                byte[] iv = new byte[16];
                System.arraycopy(head, 8, iv, 0, 8);
                Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key(), "AES"), new IvParameterSpec(iv));
                return new CipherInputStream(in, cipher);
            }
            in.close();                                                       // plain: start again from the beginning
            return new FileInputStream(file);
        } catch (java.security.GeneralSecurityException error) {
            in.close();
            throw new IOException(error);
        } catch (IOException error) {
            in.close();
            throw error;
        }
    }

    public static byte[] readBytes(File file) throws IOException {
        try (InputStream in = open(file)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(
                    (int) Math.max(32, Math.min(Integer.MAX_VALUE - 8, file.length())));
            byte[] buffer = new byte[16384];
            int n;
            while ((n = in.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

    public static String readString(File file) throws IOException {
        return new String(readBytes(file), java.nio.charset.StandardCharsets.UTF_8);
    }

    /** A libGDX handle for the file: plain files get an ordinary handle, encrypted ones a decrypting handle. */
    public static FileHandle handle(File file) {
        return isEncrypted(file) ? new EncryptedHandle(file) : new FileHandle(file);
    }

    private static final class EncryptedHandle extends FileHandle {
        private final File source;

        EncryptedHandle(File file) {
            super(file);
            this.source = file;
        }

        @Override
        public InputStream read() {
            try {
                return open(source);
            } catch (IOException error) {
                throw new com.badlogic.gdx.utils.GdxRuntimeException("cannot read " + source, error);
            }
        }

        @Override
        public byte[] readBytes() {
            try {
                return ResourceCrypto.readBytes(source);
            } catch (IOException error) {
                throw new com.badlogic.gdx.utils.GdxRuntimeException("cannot read " + source, error);
            }
        }

        /** FreeType maps the file when it can: a mapped encrypted file would be handed over as ciphertext. */
        @Override
        public java.nio.ByteBuffer map(java.nio.channels.FileChannel.MapMode mode) {
            throw new com.badlogic.gdx.utils.GdxRuntimeException("encrypted file cannot be mapped: " + source);
        }

        @Override
        public long length() {
            return Math.max(0L, source.length() - HEADER);
        }
    }
}
