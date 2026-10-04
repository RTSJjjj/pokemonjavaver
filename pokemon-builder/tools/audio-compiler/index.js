// Audio Asset Compiler - core library (Phase A1: scan + content based codec
// detection).
//
// Design rules enforced here:
//   * The source Audio/ tree is strictly read only; nothing in this module
//     writes to the project.
//   * Formats are detected from file *content* (RIFF / ID3+frame sync / OggS /
//     fLaC magic), never from the file extension (project2 requirement 8).
//   * Anything that cannot be read or recognised is reported with its file and
//     reason; it is never silently dropped (requirement 7).
//   * Encoding lives in AudioEncoderService (see encoder.js); this module is
//     pure analysis so that `audio-audit` works without FFmpeg installed.

import { closeSync, openSync, readdirSync, readSync, statSync } from "node:fs";
import { createHash } from "node:crypto";
import path from "node:path";

// The four RMXP audio categories. Other directories under Audio/ (Cries,
// Anim in real projects) are scanned as extra categories and reported, but
// only the four standard ones get publish presets.
export const AUDIO_CATEGORIES = ["BGM", "BGS", "ME", "SE"];

// Requirement 9: AudioBuildOptions and their first version defaults.
// normalize must default to false: the compiler never changes loudness unless
// the user explicitly asks for it.
export const DEFAULT_AUDIO_OPTIONS = {
  preset: "standard",
  bgmQuality: null,
  bgsQuality: null,
  sampleRate: null,
  convertSE: true,
  seThresholdBytes: 256 * 1024,
  deduplicate: true,
  analyzeSilence: true,
  trimSilence: false,
  normalize: false,
};

// Requirement 8: quality presets. Compact keeps 44100 Hz by default and never
// switches to mono on its own.
export const AUDIO_PRESETS = {
  high: { label: "High Quality", bgmQuality: 5, bgsQuality: 4, sampleRate: 44100, convertSE: true },
  standard: { label: "Standard", bgmQuality: 4, bgsQuality: 3, sampleRate: 44100, convertSE: true },
  compact: { label: "Compact", bgmQuality: 3, bgsQuality: 2, sampleRate: 44100, convertSE: true },
};

export const AUDIO_PROBLEM_KINDS = [
  "unreadable",
  "zero-length",
  "invalid-header",
  "unsupported-codec",
  "extension-mismatch",
  "missing-audio",
];

// ---------------------------------------------------------------------------
// Low level file reading helpers
// ---------------------------------------------------------------------------

const HEAD_BYTES = 1024 * 1024;
const TAIL_BYTES = 64 * 1024;

function readHead(filePath, size) {
  const length = Math.min(HEAD_BYTES, size);
  if (length <= 0) return Buffer.alloc(0);
  const fd = openSync(filePath, "r");
  try {
    const buffer = Buffer.allocUnsafe(length);
    readSync(fd, buffer, 0, length, 0);
    return buffer;
  } finally {
    closeSync(fd);
  }
}

function readTail(filePath, size) {
  const length = Math.min(TAIL_BYTES, size);
  if (length <= 0 || length >= size) return readHead(filePath, size);
  const fd = openSync(filePath, "r");
  try {
    const buffer = Buffer.allocUnsafe(length);
    readSync(fd, buffer, 0, length, size - length);
    return buffer;
  } finally {
    closeSync(fd);
  }
}

function sha256File(filePath) {
  const hash = createHash("sha256");
  const fd = openSync(filePath, "r");
  try {
    const chunk = Buffer.allocUnsafe(1024 * 1024);
    let position = 0;
    let read = readSync(fd, chunk, 0, chunk.length, position);
    while (read > 0) {
      hash.update(read === chunk.length ? chunk : chunk.subarray(0, read));
      position += read;
      read = readSync(fd, chunk, 0, chunk.length, position);
    }
  } finally {
    closeSync(fd);
  }
  return hash.digest("hex");
}

// ---------------------------------------------------------------------------
// Container / codec probes. Every probe returns a plain object; `ok` is false
// when the bytes are not a usable instance of that container.
// ---------------------------------------------------------------------------

const WAV_FORMAT_NAMES = {
  0x0001: "PCM",
  0x0002: "ADPCM",
  0x0003: "IEEE float",
  0x0006: "A-law",
  0x0007: "mu-law",
  0x0011: "IMA ADPCM",
  0x0031: "GSM 6.10",
  0x0055: "MP3",
  0x0069: "IAMB E",
  0xfffe: "extensible",
};

export function probeWav(buffer, _tail, fileSize) {
  if (buffer.length < 12) return { ok: false, reason: "file too small for a RIFF header" };
  if (buffer.toString("ascii", 0, 4) !== "RIFF" || buffer.toString("ascii", 8, 12) !== "WAVE") {
    return { ok: false, reason: "missing RIFF/WAVE magic" };
  }
  let offset = 12;
  let formatTag = null;
  let channels = null;
  let sampleRate = null;
  let bitsPerSample = null;
  let byteRate = null;
  let dataBytes = null;
  let dataOffset = null;
  while (offset + 8 <= buffer.length) {
    const chunkId = buffer.toString("ascii", offset, offset + 4);
    const chunkSize = buffer.readUInt32LE(offset + 4);
    const body = offset + 8;
    if (chunkId === "fmt " && body + 16 <= buffer.length) {
      formatTag = buffer.readUInt16LE(body);
      channels = buffer.readUInt16LE(body + 2);
      sampleRate = buffer.readUInt32LE(body + 4);
      byteRate = buffer.readUInt32LE(body + 8);
      bitsPerSample = buffer.readUInt16LE(body + 14);
      if (formatTag === 0xfffe && body + 26 <= buffer.length) {
        // WAVE_FORMAT_EXTENSIBLE: the real codec sits in the SubFormat GUID.
        formatTag = buffer.readUInt16LE(body + 24);
      }
    } else if (chunkId === "data") {
      dataBytes = chunkSize;
      dataOffset = body;
    }
    if (formatTag !== null && dataBytes !== null) break;
    offset = body + chunkSize + (chunkSize % 2);
  }
  if (formatTag === null) return { ok: false, reason: "no fmt chunk found" };
  if (dataBytes === null) return { ok: false, reason: "no data chunk found" };
  const codec = WAV_FORMAT_NAMES[formatTag];
  if (!codec) {
    return { ok: true, unsupported: true, reason: "unknown WAV format tag 0x" + formatTag.toString(16) };
  }
  // Streamed WAV writers put 0xFFFFFFFF or a bogus size in the data chunk;
  // fall back to the real remaining file size so the duration stays honest.
  if (fileSize && dataOffset !== null && dataBytes > fileSize - dataOffset) {
    dataBytes = Math.max(0, fileSize - dataOffset);
  }
  const bitrate = byteRate && byteRate > 0 ? Math.round((byteRate * 8) / 1000) : null;
  const duration = byteRate > 0 ? dataBytes / byteRate : null;
  return {
    ok: true,
    unsupported: false,
    container: "WAV (RIFF)",
    codec: codec + (formatTag === 0x0001 ? " " + String(bitsPerSample) + "-bit" : ""),
    channels,
    sampleRate,
    bitrate,
    duration,
    durationApproximate: false,
    bitsPerSample,
  };
}

const MPEG_VERSION = ["MPEG 2.5", "reserved", "MPEG 2", "MPEG 1"];
const MPEG_LAYER = ["reserved", "Layer III", "Layer II", "Layer I"];
// kbps, indexed by [version group][layer][bitrate index].
//   group 1 = MPEG 1, group 0 = MPEG 2 and MPEG 2.5
const MPEG_BITRATE = {
  1: [
    null,
    [0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448],
    [0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384],
    [0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448],
  ],
  0: [
    null,
    [0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160],
    [0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160],
    [0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256],
  ],
};
const MPEG_SAMPLE_RATE = {
  1: [44100, 48000, 32000],
  2: [22050, 24000, 16000],
  0: [11025, 12000, 8000],
};

function skipId3v2(buffer) {
  if (buffer.length < 10 || buffer.toString("ascii", 0, 3) !== "ID3") return 0;
  const size =
    ((buffer[6] & 0x7f) << 21) | ((buffer[7] & 0x7f) << 14) | ((buffer[8] & 0x7f) << 7) | (buffer[9] & 0x7f);
  return 10 + size;
}

function findFrameSync(buffer, from) {
  for (let i = from; i < buffer.length - 1; i++) {
    if (buffer[i] === 0xff && (buffer[i + 1] & 0xe0) === 0xe0) return i;
  }
  return -1;
}

export function probeMp3(buffer, _tail, fileSize) {
  const start = skipId3v2(buffer);
  const frame = findFrameSync(buffer, start);
  if (frame < 0 || frame + 4 > buffer.length) {
    return { ok: false, reason: "no MPEG audio frame sync found" };
  }
  const header = buffer.readUInt32BE(frame);
  const versionBits = (header >> 19) & 0x3;
  const layerBits = (header >> 17) & 0x3;
  const bitrateIndex = (header >> 12) & 0xf;
  const sampleRateIndex = (header >> 10) & 0x3;
  const channelMode = (header >> 6) & 0x3;
  if (versionBits === 1 || layerBits === 0 || bitrateIndex === 0 || bitrateIndex === 15 || sampleRateIndex === 3) {
    return { ok: false, reason: "unusable MPEG frame header" };
  }
  const version = versionBits === 3 ? 1 : 0;
  const layer = layerBits === 1 ? 3 : layerBits === 2 ? 2 : 1;
  const versionName = MPEG_VERSION[versionBits];
  const layerName = MPEG_LAYER[layerBits];
  const sampleRate = MPEG_SAMPLE_RATE[version][sampleRateIndex];
  const bitrate = MPEG_BITRATE[version][layer][bitrateIndex];
  const channels = channelMode === 3 ? 1 : 2;
  let duration = null;
  let durationApproximate = true;
  let vbr = false;
  let effectiveBitrate = bitrate;
  if (layer === 3) {
    const side = version === 1 ? (channelMode === 3 ? 17 : 32) : channelMode === 3 ? 9 : 17;
    const tagAt = frame + 4 + side;
    const tag = buffer.toString("ascii", tagAt, tagAt + 4);
    if (tag === "Xing" || tag === "Info") {
      vbr = true;
      const flags = buffer.readUInt32BE(tagAt + 4);
      if ((flags & 0x1) !== 0 && tagAt + 12 <= buffer.length) {
        const frames = buffer.readUInt32BE(tagAt + 8);
        const samplesPerFrame = version === 1 ? 1152 : 576;
        if (frames > 0 && sampleRate > 0) {
          duration = (frames * samplesPerFrame) / sampleRate;
          durationApproximate = false;
        }
      }
    } else if (bitrate > 0 && fileSize) {
      duration = ((fileSize - frame) * 8) / (bitrate * 1000);
    }
  } else if (bitrate > 0 && fileSize) {
    duration = (fileSize * 8) / (bitrate * 1000);
  }
  if (vbr && fileSize && duration && duration > 0) {
    // Average across the whole file; the nominal table value is wrong for VBR.
    effectiveBitrate = Math.round(fileSize * 8 / duration / 1000);
  }
  return {
    ok: true,
    unsupported: false,
    container: "MP3 (" + versionName + " " + layerName + ")",
    codec: "MPEG Audio" + (vbr ? " (VBR)" : ""),
    channels,
    sampleRate,
    bitrate: effectiveBitrate || null,
    duration,
    durationApproximate,
    bitsPerSample: null,
    vbr,
  };
}

export function probeOggVorbis(buffer, tail, fileSize) {
  if (buffer.length < 27 || buffer.toString("ascii", 0, 4) !== "OggS") {
    return { ok: false, reason: "missing OggS page magic" };
  }
  const segmentCount = buffer[26];
  const packetStart = 27 + segmentCount;
  if (buffer.length < packetStart + 28) {
    return { ok: false, reason: "truncated first Ogg page" };
  }
  const idHeader = buffer.toString("latin1", packetStart, packetStart + 7);
  if (idHeader !== "\x01vorbis") {
    const other = buffer.toString("latin1", packetStart + 1, packetStart + 8).replace(/[^\x20-\x7e]/g, "").trim();
    return {
      ok: true,
      unsupported: true,
      container: "Ogg",
      codec: other || "unknown ogg codec",
      channels: null,
      sampleRate: null,
      bitrate: null,
      duration: null,
      durationApproximate: true,
      reason: "Ogg container with a codec other than Vorbis needs the FFmpeg encoder to convert",
    };
  }
  const channels = buffer[packetStart + 11];
  const sampleRate = buffer.readUInt32LE(packetStart + 12);
  const bitrateNominal = buffer.readInt32LE(packetStart + 20);
  let duration = null;
  let durationApproximate = false;
  const scan = tail && tail.length >= 27 ? tail : buffer;
  for (let i = scan.length - 27; i >= 0; i--) {
    if (scan.toString("ascii", i, i + 4) !== "OggS") continue;
    if ((scan[i + 5] & 0x04) === 0) continue; // only the last page carries the end granule
    const granule = scan.readBigUInt64LE(i + 6);
    if (granule > 0n && sampleRate > 0) duration = Number(granule) / sampleRate;
    break;
  }
  if (duration === null) durationApproximate = true;
  return {
    ok: true,
    unsupported: false,
    container: "Ogg",
    codec: "Vorbis",
    channels,
    sampleRate,
    bitrate: bitrateNominal > 0 ? Math.round(bitrateNominal / 1000) : null,
    duration,
    durationApproximate,
    bitsPerSample: null,
    vbr: true,
    fileSize,
  };
}

export function probeFlac(buffer) {
  if (buffer.length < 42 || buffer.toString("ascii", 0, 4) !== "fLaC") {
    return { ok: false, reason: "missing fLaC magic" };
  }
  let offset = 4;
  let header = null;
  for (let guard = 0; guard < 32 && offset + 4 <= buffer.length; guard++) {
    const type = buffer[offset] & 0x7f;
    const last = (buffer[offset] & 0x80) !== 0;
    const length = (buffer[offset + 1] << 16) | (buffer[offset + 2] << 8) | buffer[offset + 3];
    if (type === 0 && offset + 4 + length <= buffer.length) {
      header = buffer.subarray(offset + 4, offset + 4 + length);
      break;
    }
    if (last) break;
    offset += 4 + length;
  }
  if (!header || header.length < 18) return { ok: false, reason: "no STREAMINFO metadata block" };
  const sampleRate = (header[10] << 12) | (header[11] << 4) | (header[12] >> 4);
  const channels = ((header[12] >> 1) & 0x7) + 1;
  const bitsPerSample = (((header[12] & 0x1) << 4) | (header[13] >> 4)) + 1;
  const totalSamples = Number(
    (BigInt(header[13] & 0x0f) << 32n) |
      (BigInt(header[14]) << 24n) |
      (BigInt(header[15]) << 16n) |
      (BigInt(header[16]) << 8n) |
      BigInt(header[17]),
  );
  const duration = sampleRate > 0 && totalSamples > 0 ? totalSamples / sampleRate : null;
  const bitrate = sampleRate > 0 ? Math.round((bitsPerSample * sampleRate * channels) / 1000) : null;
  return {
    ok: true,
    unsupported: false,
    container: "FLAC",
    codec: "FLAC " + bitsPerSample + "-bit",
    channels,
    sampleRate,
    bitrate,
    duration,
    durationApproximate: false,
    bitsPerSample,
  };
}

function probeMidi() {
  return {
    ok: true,
    unsupported: true,
    container: "MIDI",
    codec: "MIDI sequence",
    channels: null,
    sampleRate: null,
    bitrate: null,
    duration: null,
    durationApproximate: true,
    reason: "MIDI needs an external synthesizer to play or convert",
  };
}

const PROBES = [
  { container: "WAV (RIFF)", test: (b) => b.length >= 4 && b.toString("ascii", 0, 4) === "RIFF", probe: probeWav },
  { container: "OGG", test: (b) => b.length >= 4 && b.toString("ascii", 0, 4) === "OggS", probe: probeOggVorbis },
  { container: "FLAC", test: (b) => b.length >= 4 && b.toString("ascii", 0, 4) === "fLaC", probe: probeFlac },
  {
    container: "MP3",
    test: (b) => {
      if (b.length >= 3 && b.toString("ascii", 0, 3) === "ID3") return true;
      return b.length >= 2 && b[0] === 0xff && (b[1] & 0xe0) === 0xe0;
    },
    probe: probeMp3,
  },
  { container: "MIDI", test: (b) => b.length >= 4 && b.toString("ascii", 0, 4) === "MThd", probe: probeMidi },
];

// Detect the container from the file bytes. Never uses the file extension.
export function detectAudioFormat(head, tail, fileSize) {
  for (const candidate of PROBES) {
    if (!candidate.test(head)) continue;
    return candidate.probe(head, tail, fileSize);
  }
  return {
    ok: false,
    unsupported: true,
    reason: "unrecognized audio container (content does not match WAV/MP3/OGG/FLAC/MIDI)",
  };
}

const CONTAINER_EXTENSIONS = {
  "WAV (RIFF)": ".wav",
  OGG: ".ogg",
  FLAC: ".flac",
  MIDI: ".mid",
};

function containerExpectedExtension(container) {
  if (CONTAINER_EXTENSIONS[container]) return CONTAINER_EXTENSIONS[container];
  if (container && container.startsWith("MP3 (")) return ".mp3";
  return null;
}

// ---------------------------------------------------------------------------
// Options
// ---------------------------------------------------------------------------

// Requirement 9: one AudioBuildOptions model shared by CLI, BAT and (later)
// the GUI. Precedence: explicit field > preset > built in default.
export function resolveAudioOptions(audioConfig, overrides) {
  const options = { ...DEFAULT_AUDIO_OPTIONS };
  const presetName =
    (overrides && overrides.preset) || (audioConfig && audioConfig.preset) || DEFAULT_AUDIO_OPTIONS.preset;
  const preset = AUDIO_PRESETS[presetName];
  if (!preset) {
    throw new Error("unknown audio preset: " + presetName);
  }
  options.preset = presetName;
  options.bgmQuality = preset.bgmQuality;
  options.bgsQuality = preset.bgsQuality;
  options.sampleRate = preset.sampleRate;
  options.convertSE = preset.convertSE;
  for (const source of [audioConfig || {}, overrides || {}]) {
    if (source.bgmQuality !== undefined) options.bgmQuality = source.bgmQuality;
    if (source.bgsQuality !== undefined) options.bgsQuality = source.bgsQuality;
    if (source.sampleRate !== undefined) options.sampleRate = Number(source.sampleRate) || null;
    if (source.convertSE !== undefined) options.convertSE = Boolean(source.convertSE);
    if (source.seThreshold !== undefined) options.seThresholdBytes = Number(source.seThreshold) || 0;
    if (source.seThresholdBytes !== undefined) options.seThresholdBytes = Number(source.seThresholdBytes) || 0;
    if (source.deduplicate !== undefined) options.deduplicate = Boolean(source.deduplicate);
    if (source.analyzeSilence !== undefined) options.analyzeSilence = Boolean(source.analyzeSilence);
    if (source.trimSilence !== undefined) options.trimSilence = Boolean(source.trimSilence);
    if (source.normalize !== undefined) options.normalize = Boolean(source.normalize);
  }
  return options;
}

// ---------------------------------------------------------------------------
// Scanning
// ---------------------------------------------------------------------------

function existsDir(dir) {
  try {
    return statSync(dir).isDirectory();
  } catch {
    return false;
  }
}

function walkDir(dir, base, category, out) {
  let names;
  try {
    names = readdirSync(dir);
  } catch (error) {
    out.problems.push({
      kind: "unreadable",
      category,
      file: path.relative(base, dir),
      reason: "cannot list directory: " + error.message,
    });
    return;
  }
  for (const name of names.sort()) {
    const full = path.join(dir, name);
    let stats;
    try {
      stats = statSync(full);
    } catch (error) {
      out.problems.push({
        kind: "unreadable",
        category,
        file: path.relative(base, full).replace(/\\/g, "/"),
        reason: "cannot stat: " + error.message,
      });
      continue;
    }
    if (stats.isDirectory()) {
      walkDir(full, base, category, out);
      continue;
    }
    out.files.push({
      category,
      fullPath: full,
      relativePath: path.relative(base, full).replace(/\\/g, "/"),
      size: stats.size,
    });
  }
}

// Scan the Audio/ tree of an RMXP project. Read only: the project files are
// opened for reading and hashed; nothing is written.
export function scanAudio(projectPath, options = {}) {
  const audioRoot = path.join(projectPath, "Audio");
  const scan = {
    project: projectPath,
    audioRoot,
    ok: true,
    warnings: [],
    errors: [],
    problems: [],
    files: [],
    totals: { files: 0, bytes: 0, byCategory: {}, byExtension: {} },
    options: options || {},
    scannedAt: new Date().toISOString(),
  };
  for (const category of AUDIO_CATEGORIES) {
    scan.totals.byCategory[category] = { files: 0, bytes: 0 };
  }
  if (!existsDir(audioRoot)) {
    scan.warnings.push("Audio directory not found: " + audioRoot + " (no audio assets)");
    return scan;
  }
  const collected = { files: [], problems: [] };
  for (const category of AUDIO_CATEGORIES) {
    const dir = path.join(audioRoot, category);
    if (!existsDir(dir)) {
      scan.warnings.push("audio category directory not found: " + path.relative(projectPath, dir));
      continue;
    }
    walkDir(dir, projectPath, category, collected);
  }
  // Extra categories that exist in real projects (Cries, Anim, ...): scan and
  // report them without publish presets.
  for (const entry of readdirSync(audioRoot).sort()) {
    if (AUDIO_CATEGORIES.includes(entry)) continue;
    const dir = path.join(audioRoot, entry);
    if (!existsDir(dir)) continue;
    walkDir(dir, projectPath, entry, collected);
  }
  collected.files.sort((a, b) => (a.relativePath < b.relativePath ? -1 : a.relativePath > b.relativePath ? 1 : 0));
  for (const file of collected.files) {
    const record = probeFile(file, scan);
    scan.files.push(record);
    const totals = scan.totals.byCategory[file.category] || (scan.totals.byCategory[file.category] = { files: 0, bytes: 0 });
    totals.files += 1;
    totals.bytes += file.size;
    const ext = record.ext || "(none)";
    scan.totals.byExtension[ext] = (scan.totals.byExtension[ext] || 0) + 1;
    scan.totals.files += 1;
    scan.totals.bytes += file.size;
  }
  scan.problems.push(...collected.problems);
  // Exact duplicate groups (requirement 15): same content hash, different names.
  scan.duplicates = findDuplicateGroups(scan.files);
  return scan;
}

function probeFile(file, scan) {
  const record = {
    category: file.category,
    relativePath: file.relativePath,
    name: path.basename(file.relativePath, path.extname(file.relativePath)),
    ext: path.extname(file.relativePath).toLowerCase(),
    size: file.size,
    sha256: null,
    container: null,
    codec: null,
    sampleRate: null,
    channels: null,
    bitrate: null,
    duration: null,
    durationApproximate: false,
    bitsPerSample: null,
    matchedExtension: null,
    unsupported: false,
    problem: null,
  };
  if (file.size === 0) {
    record.problem = { kind: "zero-length", file: record.relativePath, reason: "file is empty (0 bytes)" };
    scan.problems.push(record.problem);
    return record;
  }
  let sha256 = null;
  let head = null;
  let tail = null;
  try {
    sha256 = sha256File(file.fullPath);
    head = readHead(file.fullPath, file.size);
    tail = readTail(file.fullPath, file.size);
  } catch (error) {
    record.problem = { kind: "unreadable", file: record.relativePath, reason: error.message };
    scan.problems.push(record.problem);
    return record;
  }
  record.sha256 = sha256;
  const probed = detectAudioFormat(head, tail, file.size);
  if (!probed.ok) {
    record.problem = { kind: "invalid-header", file: record.relativePath, reason: probed.reason };
    scan.problems.push(record.problem);
    return record;
  }
  Object.assign(record, {
    container: probed.container,
    codec: probed.codec,
    sampleRate: probed.sampleRate,
    channels: probed.channels,
    bitrate: probed.bitrate,
    duration: probed.duration,
    durationApproximate: probed.durationApproximate,
    bitsPerSample: probed.bitsPerSample,
  });
  const expectedExtension = containerExpectedExtension(probed.container);
  record.matchedExtension = !expectedExtension || !record.ext || record.ext === expectedExtension;
  if (!record.matchedExtension) {
    scan.problems.push({
      kind: "extension-mismatch",
      file: record.relativePath,
      reason: "extension " + record.ext + " does not match detected container " + probed.container,
    });
  }
  if (probed.unsupported) {
    record.unsupported = true;
    record.problem = {
      kind: "unsupported-codec",
      file: record.relativePath,
      reason: probed.reason || "unsupported codec: " + probed.codec,
    };
    scan.problems.push(record.problem);
  }
  return record;
}

function findDuplicateGroups(files) {
  const byHash = new Map();
  for (const file of files) {
    if (!file.sha256) continue;
    const list = byHash.get(file.sha256) || [];
    list.push(file);
    byHash.set(file.sha256, list);
  }
  const groups = [];
  for (const [sha256, group] of byHash) {
    if (group.length < 2) continue;
    groups.push({
      sha256,
      count: group.length,
      bytes: group[0].size,
      files: group.map((file) => file.relativePath).sort(),
    });
  }
  groups.sort((a, b) => b.count - a.count || (a.files[0] < b.files[0] ? -1 : 1));
  return groups;
}

// ---------------------------------------------------------------------------
// Formatting helpers (shared by CLI, reports and tests)
// ---------------------------------------------------------------------------

export function formatBytes(bytes) {
  if (bytes === null || bytes === undefined) return "-";
  if (bytes < 1024) return bytes + " B";
  const units = ["KiB", "MiB", "GiB", "TiB"];
  let value = bytes / 1024;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit += 1;
  }
  return (value >= 100 ? value.toFixed(0) : value.toFixed(1)) + " " + units[unit];
}

export function formatDuration(seconds) {
  if (seconds === null || seconds === undefined || !isFinite(seconds)) return "-";
  const total = Math.round(seconds);
  const minutes = Math.floor(total / 60);
  const rest = total % 60;
  if (minutes > 0) return minutes + "m " + String(rest).padStart(2, "0") + "s";
  return rest + "s";
}

export function formatAudioScanSummary(scan) {
  const lines = [];
  lines.push("audio files: " + scan.totals.files + " (" + formatBytes(scan.totals.bytes) + ")");
  for (const [category, totals] of Object.entries(scan.totals.byCategory)) {
    if (totals.files === 0) continue;
    lines.push("  " + category + ": " + totals.files + " files (" + formatBytes(totals.bytes) + ")");
  }
  const byCodec = new Map();
  for (const file of scan.files) {
    const key = file.codec || "unrecognized";
    byCodec.set(key, (byCodec.get(key) || 0) + 1);
  }
  if (byCodec.size > 0) {
    lines.push(
      "codecs: " +
        [...byCodec.entries()]
          .sort((a, b) => b[1] - a[1])
          .map(([codec, count]) => codec + "=" + count)
          .join(", "),
    );
  }
  if (scan.duplicates && scan.duplicates.length > 0) {
    const wasted = scan.duplicates.reduce((sum, group) => sum + group.bytes * (group.count - 1), 0);
    lines.push("exact duplicate groups: " + scan.duplicates.length + " (wasted " + formatBytes(wasted) + ")");
  }
  lines.push("problems: " + scan.problems.length);
  return lines;
}
