package pokemon.runtime.audio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.data.AudioManifestData;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B3: {@link SoundLength} - the transcription of {@code getPlayTime2}
 * (Audio_Utilities:1102-1175), {@code getOggPage} (:1013-1027) and
 * {@code oggfiletime} (:1030-1086) - plus the {@code AudioManager.sePlayTime}
 * wiring {@code pbCryFrameLength} depends on (PSystem_FileUtilities:462-463).
 *
 * <p><b>The OGG test pins a plugin bug on purpose.</b> {@code oggfiletime} reads
 * {@code version=fgetdw} (:1075) and then {@code rates[i]=fgetdw} (:1077)
 * back to back, i.e. the four bytes at body+11. A Vorbis identification header
 * is {@code +0} packet type, {@code +1..6} "vorbis", {@code +7..10}
 * vorbis_version, <b>{@code +11} channels (one byte)</b>, {@code +12..15} sample
 * rate - so :1077 swallows the channels byte. The port follows the plugin line
 * for line (the user's rule: the plugin is the specification, not its intent),
 * so these assertions expect the misaligned rate. The builder parses the same
 * header at the correct offsets ({@code tools/audio-compiler/index.js:315-316}),
 * which is why {@code generated/audio-manifest.json} disagrees with the value
 * measured here. Reported to the user for a ruling.</p>
 */
class SoundLengthTest {

    /**
     * The project's own generated cry. {@code 001Cry.ogg}: mono (channels 1),
     * identification-header sample rate 44100, last page's granule 41397.
     */
    @Test
    @DisplayName("001Cry.ogg measures granule / the dword at body+11, exactly like oggfiletime (:1075-1077)")
    void oggCryDuration() throws Exception {
        Path cry = generatedCry();
        assertNotNull(cry, "generated/audio/SE/001Cry.ogg is missing; run the builder's build-audio");

        byte[] bytes = Files.readAllBytes(cry);
        int packetStart = 27 + (bytes[26] & 0xFF);                  // getOggPage (:1019-1025)
        assertEquals(1, bytes[packetStart + 11] & 0xFF, "channels byte of the identification header");
        assertEquals(44100L, readLe(bytes, packetStart + 12, 4),
                "the real sample rate, the byte Audio_Utilities:1077 never reaches");
        assertEquals(11289601L, readLe(bytes, packetStart + 11, 4),
                "what :1077 reads instead: channels(1) + ((44100 & 0xFFFFFF) << 8)");
        assertEquals(41397L, lastGranule(bytes), "the last Ogg page's granule position (:1058-1066)");

        float duration = SoundLength.duration(cry.toFile());
        // 41397/11289601 = 0.003667 s. The correct offset would give
        // 41397/44100 = 0.9387 s, and pbCryFrameLength 42 instead of 5
        // (BattlerFaintAnimation's delay would be 21 instead of 2).
        assertEquals(41397f / 11289601f, duration, 1e-9f);
        assertTrue(duration > 0f);
    }

    /** {@code getPlayTime2}'s RIFF branch: {@code time=(datasize*1.0)/bytessec} (:1139). */
    @Test
    @DisplayName("a RIFF/WAVE file measures dataSize/byteRate (:1135-1139)")
    void waveDuration(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("cry.wav");
        Files.write(file, wave(16000, 8000));
        assertEquals(2f, SoundLength.duration(file.toFile()), 1e-4f);
    }

    /** :1129-1131 {@code return -1 if bytessec==0}. */
    @Test
    @DisplayName("a WAV without a byte rate is unparsed (:1129-1131)")
    void waveWithoutByteRate(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("bad.wav");
        Files.write(file, wave(16000, 0));
        assertEquals(-1f, SoundLength.duration(file.toFile()), 0f);
    }

    /** :1071-1073: a first page that is not a Vorbis identification header. */
    @Test
    @DisplayName("an Ogg page that is not a Vorbis identification header is unparsed (:1071-1073)")
    void oggWithoutVorbisIdentification(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("fake.ogg");
        Files.write(file, fakeOggPage());
        assertEquals(-1f, SoundLength.duration(file.toFile()), 0f);
    }

    /** The containers the port does not transcribe come back as -1 (:1103). */
    @Test
    @DisplayName("an MP3, a missing file and a too-short file are unparsed (:1103-1104, :1147-1172)")
    void unparsedContainers(@TempDir Path dir) throws Exception {
        Path mp3 = dir.resolve("song.mp3");
        Files.write(mp3, "ID3\u0003\u0000\u0000\u0000\u0000\u0000\u0000".getBytes(
                StandardCharsets.ISO_8859_1));
        assertEquals(-1f, SoundLength.duration(mp3.toFile()), 0f,
                "the MP3 branch (Audio_Utilities:1147-1172) is not transcribed");

        assertEquals(-1f, SoundLength.duration(dir.resolve("missing.ogg").toFile()), 0f);

        Path tiny = dir.resolve("tiny.ogg");
        Files.write(tiny, new byte[] { 1, 2, 3 });
        assertEquals(-1f, SoundLength.duration(tiny.toFile()), 0f);

        assertEquals(-1f, SoundLength.duration(null), 0f);
    }

    /**
     * {@code AudioManager.sePlayTime} resolves the manifest entry, measures the
     * file once and caches it; the second call must not touch the file again.
     */
    @Test
    @DisplayName("AudioManager.sePlayTime measures the manifest's SE file once and caches it")
    void sePlayTimeCaches(@TempDir Path root) throws Exception {
        Path source = generatedCry();
        assertNotNull(source, "generated/audio/SE/001Cry.ogg is missing; run the builder's build-audio");
        Path cry = root.resolve("SE").resolve("001Cry.ogg");
        Files.createDirectories(cry.getParent());
        Files.copy(source, cry);

        AudioManager manager = new AudioManager();
        manager.attach(manifest("001Cry", "SE/001Cry.ogg"), root.toFile());

        float first = manager.sePlayTime("001Cry");
        assertEquals(41397f / 11289601f, first, 1e-9f);

        // A 2 s WAV in the same place: a re-read would return 2, the cache keeps
        // the value measured from the OGG.
        Files.write(cry, wave(16000, 8000));
        assertEquals(first, manager.sePlayTime("001Cry"), 1e-9f);
        assertEquals(-1f, manager.sePlayTime("No such SE"), 0f, "not in the manifest (:629-634)");
    }

    // ------------------------------------------------------------------

    private static AudioManifestData manifest(String id, String file) {
        AudioManifestData manifest = new AudioManifestData();
        manifest.loaded = true;
        manifest.preset = "standard";
        AudioManifestData.Entry entry = new AudioManifestData.Entry();
        entry.type = "SE";
        entry.file = file;
        manifest.entries.put(id, entry);
        return manifest;
    }

    /** {@code generated/audio/SE/001Cry.ogg} of this checkout, or null when absent. */
    private static Path generatedCry() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve("generated").resolve("audio").resolve("SE")
                    .resolve("001Cry.ogg");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        return null;
    }

    /** The last page's granule position, walking pages the way getOggPage does (:1013-1027). */
    private static long lastGranule(byte[] bytes) {
        int pos = 0;
        long granule = 0;
        while (pos + 27 <= bytes.length && "OggS".equals(text(bytes, pos, 4))) {
            int segments = bytes[pos + 26] & 0xFF;
            int bodySize = 0;
            for (int i = 0; i < segments; i++) {
                bodySize += bytes[pos + 27 + i] & 0xFF;
            }
            granule = readLe(bytes, pos + 6, 8);                    // header[2,8] (:1058)
            pos += 27 + segments + bodySize;
        }
        return granule;
    }

    /** One page whose body starts like a non-Vorbis packet (:1070-1073). */
    private static byte[] fakeOggPage() {
        byte[] page = new byte[27 + 1 + 30];
        System.arraycopy("OggS".getBytes(StandardCharsets.ISO_8859_1), 0, page, 0, 4);
        page[4] = 0;                                                // version
        page[5] = 0x02;                                             // BOS flag
        page[10] = 1;                                               // serial
        page[26] = 1;                                               // one segment
        page[27] = 30;                                              // lacing = body size
        page[28] = 0x05;                                            // not a 0x01 identification packet
        System.arraycopy("notvor".getBytes(StandardCharsets.ISO_8859_1), 0, page, 29, 6);
        return page;
    }

    /** A minimal 44-byte-header RIFF/WAVE file with {@code dataSize} bytes of silence. */
    private static byte[] wave(int dataSize, int byteRate) {
        ByteBuffer buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(ascii("RIFF"));
        buffer.putInt(36 + dataSize);                               // :1115 filesize
        buffer.put(ascii("WAVE"));                                  // :1116-1119
        buffer.put(ascii("fmt "));                                  // :1120-1123
        buffer.putInt(16);                                          // :1124 fmtsize
        buffer.putShort((short) 1);                                 // :1125 format = PCM
        buffer.putShort((short) 1);                                 // :1126 channels
        buffer.putInt(byteRate);                                    // :1127 rate
        buffer.putInt(byteRate);                                    // :1128 bytessec
        buffer.putShort((short) 1);                                 // :1132 bytessample
        buffer.putShort((short) 8);                                 // :1133 bitssample
        buffer.put(ascii("data"));                                  // :1134-1137
        buffer.putInt(dataSize);                                    // :1138 datasize
        buffer.position(44 + dataSize);
        return buffer.array();
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String text(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, StandardCharsets.ISO_8859_1);
    }

    private static long readLe(byte[] bytes, int offset, int count) {
        long value = 0L;
        for (int i = count - 1; i >= 0; i--) {
            value = (value << 8) | (bytes[offset + i] & 0xFFL);
        }
        return value;
    }
}
