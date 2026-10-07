package pokemon.runtime.audio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code getPlayTime2} (Audio_Utilities:1102-1175) and the two helpers it calls,
 * {@code getOggPage} (Audio_Utilities:1013-1027) and {@code oggfiletime}
 * (Audio_Utilities:1030-1086): the real length of an audio file in seconds,
 * read straight out of its container header.
 *
 * <p>{@code getPlayTime} (Audio_Utilities:1088-1100) wraps this result:
 * {@code [getPlayTime2(file),0].max}, i.e. every file this class cannot parse
 * counts as 0 seconds. {@code pbCryFrameLength} (PSystem_FileUtilities:448-469)
 * is the only caller in the ported runtime: {@code playtime = getPlayTime(wav)}
 * feeds {@code (playtime*40).ceil+4}.</p>
 *
 * <p>Transcribed containers:</p>
 * <ul>
 *   <li>RIFF/WAVE, {@code datasize/byteRate} (Audio_Utilities:1114-1140)</li>
 *   <li>OGG Vorbis, the last granule position of each stream divided by the
 *       sample rate {@code oggfiletime} reads at :1077 (:1141-1144,
 *       :1030-1086)</li>
 * </ul>
 *
 * <p><b>Plugin bug, transcribed as written.</b> {@code oggfiletime} reads
 * {@code version=fgetdw} (:1075) and then {@code rates[i]=fgetdw} (:1077) back to
 * back - the four bytes at body+11. A Vorbis identification header is laid out
 * as {@code +0} packet type, {@code +1..6} "vorbis", {@code +7..10}
 * vorbis_version, <b>{@code +11} channels (one byte)</b>, {@code +12..15} sample
 * rate, so :1077 swallows the channels byte and computes
 * {@code channels + ((rate & 0xFFFFFF) << 8)} instead of the sample rate. The
 * port follows the plugin and not the plugin's intent, so the value is kept:
 * for this project's own {@code generated/audio/SE/001Cry.ogg} (channels 1,
 * sample rate 44100, last granule 41397) this class returns
 * {@code 41397/11289601 = 0.003667 s} where the sample rate at +12 would give
 * {@code 41397/44100 = 0.9387 s}. {@code pbCryFrameLength} therefore yields 5
 * frames instead of 42, and {@code BattlerFaintAnimation}'s delay is 2 instead
 * of 21 (PokeBattle_SceneAnimations:674). The builder parses the same header at
 * the correct offsets ({@code tools/audio-compiler/index.js:315-316}), so
 * {@code generated/audio-manifest.json}'s {@code sampleRate}/{@code duration}
 * disagree with this value - that disagreement is the plugin's bug, not a
 * parsing error here. Reported to the user for a ruling; switching to the
 * intended offset would be a one-line change (skip the channels byte before
 * :1077).</p>
 *
 * <p><b>Not transcribed:</b> the MP3 branch (Audio_Utilities:1147-1172). It scans
 * for an {@code FF FB} header and indexes a 14-entry bitrate table (:1161-1162)
 * with {@code rstr[1]>>4} (:1158), which can legally be 14 after the
 * {@code t==15} filter - {@code bitrates[14]} is {@code nil} in Ruby, so that
 * branch would raise {@code TypeError} and cannot be completed without inventing
 * a fifteenth table entry. The runtime only ever hands this class files from
 * {@code generated/audio} (2430 of 2430 files are {@code .ogg}), so an MP3 - or
 * any other container - comes back as -1, exactly like an unparsed file.</p>
 */
public final class SoundLength {

    // getPlayTime2's four magic dwords (little-endian, like Ruby's unpack("V")).
    private static final long RIFF = 0x46464952L;                // :1114 "RIFF"
    private static final long WAVE = 0x45564157L;                // :1117 "WAVE"
    private static final long FMT = 0x20746d66L;                 // :1121 "fmt "
    private static final long DATA = 0x61746164L;                // :1135 "data"
    private static final long OGGS = 0x5367674FL;                // :1141 / :1018 "OggS"

    private SoundLength() {
    }

    /**
     * {@code getPlayTime2} (Audio_Utilities:1102-1175).
     *
     * @return the file's length in seconds, or -1 when the file is missing or its
     *         container is not transcribed (Ruby initialises {@code time=-1} at
     *         :1103 and returns it unchanged when it finds no frame)
     */
    public static float duration(File file) {
        if (file == null || !file.isFile()) {
            return -1f;                                          // :1104 return -1 if !safeExists?
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(0);                                         // :1112 file.pos=0
            long magic = dword(raf);                             // :1113 fdw=fgetdw.call(file)
            if (magic == RIFF) {                                 // :1114
                return waveTime(raf);                            // :1115-1140
            }
            if (magic == OGGS) {                                 // :1141
                raf.seek(0);                                     // :1142 file.pos=0
                return oggTime(raf);                             // :1143-1144
            }
            // :1146-1172 MP3: see the class comment.
            return -1f;                                          // :1103
        } catch (IOException e) {
            return -1f;                                          // :1103
        }
    }

    /** {@code getPlayTime2}'s RIFF branch (Audio_Utilities:1114-1140). */
    private static float waveTime(RandomAccessFile raf) throws IOException {
        long fileSize = dword(raf);                              // :1115 filesize
        long wave = dword(raf);                                  // :1116 wave
        if (wave != WAVE) {                                      // :1117
            return -1f;                                          // :1118
        }
        long fmt = dword(raf);                                   // :1120 fmt
        if (fmt != FMT) {                                        // :1121
            return -1f;                                          // :1122
        }
        long fmtSize = dword(raf);                               // :1124 fmtsize
        int format = word(raf);                                  // :1125 format
        int channels = word(raf);                                // :1126 channels
        long rate = dword(raf);                                  // :1127 rate
        long bytesPerSecond = dword(raf);                        // :1128 bytessec
        if (bytesPerSecond == 0) {                               // :1129
            return -1f;                                          // :1130
        }
        int bytesPerSample = word(raf);                          // :1132 bytessample
        int bitsPerSample = word(raf);                           // :1133 bitssample
        long data = dword(raf);                                  // :1134 data
        if (data != DATA) {                                      // :1135
            return -1f;                                          // :1136
        }
        long dataSize = dword(raf);                              // :1138 datasize
        return (float) ((dataSize * 1.0) / bytesPerSecond);      // :1139 time=(datasize*1.0)/bytessec
    }                                                            // :1140 return time

    /** {@code getOggPage} (Audio_Utilities:1013-1027): one page's header and bounds. */
    private static OggPage oggPage(RandomAccessFile raf) throws IOException {
        long magic = dword(raf);                                 // :1017 dw=fgetdw.call(file)
        if (magic != OGGS) {                                     // :1018
            return null;                                         // :1018 return nil
        }
        byte[] header = new byte[22];                            // :1019 header=file.read(22)
        if (raf.read(header) != header.length) {
            // A truncated page: Ruby's header[10,4] (:1057) would raise NoMethodError
            // on the nil slice, so the page counts as absent.
            return null;
        }
        int bodySize = 0;                                        // :1020 bodysize=0
        int segments = byteOrZero(raf);                          // :1021 hdrbodysize=(... rescue 0)
        for (int i = 0; i < segments; i++) {                     // :1022 hdrbodysize.times do
            bodySize += byteOrZero(raf);                         // :1023 bodysize+=(... rescue 0)
        }
        // :1025 ret=[header,file.pos,bodysize,file.pos+bodysize]
        return new OggPage(header, raf.getFilePointer(), bodySize);
    }

    /** {@code oggfiletime} (Audio_Utilities:1030-1086). */
    private static float oggTime(RandomAccessFile raf) throws IOException {
        List<OggPage> pages = new ArrayList<>();                 // :1037 pages=[]
        while (true) {                                           // :1039 loop do
            OggPage page = oggPage(raf);                         // :1040 page=getOggPage(file)
            if (page == null) {
                break;                                           // :1044-1046 else break
            }
            pages.add(page);                                     // :1042 pages.push(page)
            raf.seek(page.next);                                 // :1043 file.pos=page[3]
        }
        if (pages.isEmpty()) {                                   // :1048
            return -1f;                                          // :1049
        }
        Long currentSerial = null;                               // :1051 curserial=nil
        int i = -1;                                              // :1052 i=-1
        List<Long> pcmLengths = new ArrayList<>();               // :1053 pcmlengths=[]
        List<Long> rates = new ArrayList<>();                    // :1054 rates=[]
        for (OggPage page : pages) {                             // :1055 for page in pages
            byte[] header = page.header;                         // :1056 header=page[0]
            // :1057 serial=header[10,4].unpack("V")
            long serial = littleEndian(header, 10, 4);
            // :1058-1066 frame=header[2,8].unpack("C*") then the 8 shifts build
            // the 64-bit granule position, least significant byte first.
            long frameNo = littleEndian(header, 2, 8);
            if (currentSerial == null || serial != currentSerial) {   // :1067 if serial!=curserial
                currentSerial = serial;                          // :1068
                raf.seek(page.bodyStart);                        // :1069 file.pos=page[1]
                int packetType = byteOrZero(raf);                // :1070 (file.read(1)[0] rescue 0)
                String signature = readString(raf, 6);           // :1071 string=file.read(6)
                if (!"vorbis".equals(signature)) {               // :1072
                    return -1f;                                  // :1072 return -1
                }
                if (packetType != 1) {                           // :1073 return -1 if packtype!=1
                    return -1f;
                }
                i += 1;                                          // :1074 i+=1
                long version = dword(raf);                       // :1075 version=fgetdw.call(file)
                if (version != 0) {                              // :1076 return -1 if version!=0
                    return -1f;
                }
                rates.add(dword(raf));                           // :1077 rates[i]=fgetdw.call(file)
            }
            while (pcmLengths.size() <= i) {                     // Ruby's pcmlengths[i]=... (:1079)
                pcmLengths.add(0L);                              // pads the gap with nil
            }
            pcmLengths.set(i, frameNo);                          // :1079 pcmlengths[i]=frameno
        }
        float ret = 0.0f;                                        // :1081 ret=0.0
        for (int k = 0; k < pcmLengths.size(); k++) {            // :1082 for i in 0...pcmlengths.length
            long rate = k < rates.size() ? rates.get(k) : 0L;
            if (rate == 0) {
                // Ruby divides by zero here (:1083 -> Infinity -> the caller's
                // (playtime*40).ceil raises FloatDomainError); a zero sample
                // rate is a malformed identification header, reported as
                // "could not parse" instead of crashing the render loop.
                return -1f;
            }
            ret += pcmLengths.get(k) * 1.0f / rate;              // :1083 ret+=pcmlengths[i]*1.0/rates[i]
        }
        return ret;                                              // :1085 return ret
    }

    /**
     * {@code fgetdw}/{@code fgetw} (Audio_Utilities:1014-1016, :1031-1036,
     * :1105-1110, all the same): a little-endian dword/word, 0 at end of file.
     */
    private static long dword(RandomAccessFile raf) throws IOException {
        return littleEndian(raf, 4);                             // :1105-1107
    }

    private static int word(RandomAccessFile raf) throws IOException {
        return (int) littleEndian(raf, 2);                       // :1108-1110
    }

    /**
     * {@code (file.eof? ? 0 : (file.read(n).unpack("V")[0] || 0))}: at end of
     * file the value is 0, and a short read unpacks to {@code nil}, also 0.
     */
    private static long littleEndian(RandomAccessFile raf, int count) throws IOException {
        byte[] buffer = new byte[count];
        if (raf.read(buffer) != count) {
            return 0L;
        }
        return littleEndian(buffer, 0, count);
    }

    /** Little-endian assembly of {@code count} bytes ({@code unpack("V")}/{@code "C*"}). */
    private static long littleEndian(byte[] bytes, int offset, int count) {
        long value = 0L;
        for (int i = count - 1; i >= 0; i--) {
            value = (value << 8) | (bytes[offset + i] & 0xFFL);
        }
        return value;
    }

    /** {@code (file.read(1)[0] rescue 0)} (:1021/:1023/:1070): 0 at end of file. */
    private static int byteOrZero(RandomAccessFile raf) throws IOException {
        int value = raf.read();
        return value < 0 ? 0 : value;
    }

    /** {@code file.read(n)} (:1071); a short read never equals {@code "vorbis"}. */
    private static String readString(RandomAccessFile raf, int length) throws IOException {
        byte[] buffer = new byte[length];
        int read = raf.read(buffer);
        if (read <= 0) {
            return "";
        }
        return new String(buffer, 0, read, StandardCharsets.ISO_8859_1);
    }

    /** One {@code [header,file.pos,bodysize,file.pos+bodysize]} entry (Audio_Utilities:1025). */
    private static final class OggPage {
        final byte[] header;
        /** {@code page[1]}: where this page's body starts. */
        final long bodyStart;
        /** {@code page[3]}: where the next page starts. */
        final long next;

        OggPage(byte[] header, long bodyStart, int bodySize) {
            this.header = header;
            this.bodyStart = bodyStart;
            this.next = bodyStart + bodySize;
        }
    }
}
