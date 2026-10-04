// Audio encoder service (project3 sections 33-36, builder phase A4): turns the
// A1/A2 scan of Audio/ into generated/audio/ plus the audio manifest the
// runtime's AudioManifestData reads (it looks the ids up under "audio").

import fs from "node:fs";
import path from "node:path";
import { loopRegion } from "./loop-tags.js";

/** MIDI cannot be streamed by libGDX; it is reported, never silently dropped. */
export const MIDI_REASON = "MIDI needs an external synthesizer; skipped";

/**
 * Trailing/leading silence removed with ffmpeg: silence below the threshold,
 * from both ends (the second pass works on the reversed stream).
 */
export function silenceFilter(options = {}) {
  const threshold = options.silenceThresholdDb || "-50dB";
  const min = options.silenceMinSeconds || 0.05;
  const pass = `silenceremove=start_periods=1:start_threshold=${threshold}:start_silence=${min}`;
  return `${pass},areverse,${pass},areverse`;
}

function picked(file) {
  return {
    category: file.category,
    // scanAudio returns relativePath/ext; the audit report renames them to
    // path/extension, so both spellings are accepted here.
    path: file.relativePath || file.path,
    name: file.name,
    extension: file.ext || file.extension,
    codec: file.codec,
    size: file.size,
    sha256: file.sha256,
  };
}

/** True when the plan produces a loop segment (and maybe an intro segment). */
export function isSplit(plan) {
  return plan.splitStartSamples !== undefined || plan.splitStartSeconds !== undefined;
}

function vorbisQuality(plan, options) {
  return plan.category === "BGM" ? options.bgmQuality : options.bgsQuality;
}

function segmentArgs(plan, options, filter) {
  const args = [
    "-hide_banner", "-loglevel", "error", "-y",
    "-i", plan.path,
    "-af", filter,
    "-vn", "-c:a", "libvorbis", "-q:a", String(vorbisQuality(plan, options) ?? 4),
  ];
  if (options.sampleRate) {
    args.push("-ar", String(options.sampleRate));
  }
  return args;
}

/** Decision for one scanned file. */
export function planFile(file, options = {}, overrides = {}, projectPath = "") {
  const target = `${file.category}/${file.name}.ogg`;
  if (file.unsupported || (file.ext || file.extension) === ".mid") {
    return { ...picked(file), action: "SKIP", target: null, reason: MIDI_REASON };
  }
  // L8.1: a tagged BGM becomes [loop segment] + [intro segment]. The intro
  // plays once; the loop segment loops as a whole file, which is gapless.
  // (libGDX cannot loop a region, and seeking back into one file would cost a
  // 0.3-0.6 s decoded scan on every loop - measured with DecodeProbe.)
  const region = file.category === "BGM"
    ? loopRegion({
        file: path.join(projectPath, file.relativePath || file.path || ""),
        name: file.name,
        sampleRate: file.sampleRate,
        duration: file.duration,
        overrides,
      })
    : null;
  const loop = {};
  if (region) {
    loop.loopStartSamples = region.startSamples;
    loop.loopEndSamples = region.endSamples;
    loop.loopStartSeconds = region.startSeconds;
    loop.loopEndSeconds = region.endSeconds;
    loop.sampleRate = region.sampleRate;
    if (region.startSamples >= 0) {
      loop.splitStartSamples = region.startSamples;
    } else {
      loop.splitStartSeconds = region.startSeconds;
    }
    if (region.endSamples >= 0) {
      loop.splitEndSamples = region.endSamples;
    }
    if (region.startSeconds > 0) {
      loop.introTarget = `${file.category}/${file.name}.intro.ogg`;
    }
  }
  // Silence removal shifts the timeline, so a tagged region wins over it.
  const silence = options.trimSilence && !region ? { trimSilence: true } : {};
  const regionReason = region
    ? `Ogg Vorbis, loop ${region.startSeconds}s`
      + `${region.endSeconds >= 0 ? " .. " + region.endSeconds + "s" : " .. end"}`
      + `${loop.introTarget ? " + intro" : ""}`
    : null;
  if ((file.ext || file.extension) === ".ogg" && /vorbis/i.test(file.codec || "")) {
    return {
      ...picked(file),
      ...loop,
      ...silence,
      action: region || silence.trimSilence ? "TRANSCODE" : "COPY",
      target,
      reason: regionReason
        || (silence.trimSilence ? "Ogg Vorbis, silence trimmed" : "already Ogg Vorbis"),
    };
  }
  return {
    ...picked(file),
    ...loop,
    ...silence,
    action: "TRANSCODE",
    target,
    reason: regionReason || `${file.codec || file.ext || file.extension} -> Ogg Vorbis`,
  };
}

/** Splits a scan into work and problems (section 24 style reporting). */
export function planEncoding(scan, options = {}, overrides = {}, projectPath = "") {
  const plans = [];
  const problems = [];
  for (const file of scan.files || []) {
    const plan = planFile(file, options, overrides, projectPath);
    if (plan.action === "SKIP") {
      problems.push({ file: plan.path, reason: plan.reason });
    } else {
      plans.push(plan);
    }
  }
  return { plans, problems };
}

/** ffmpeg arguments for the loop segment of a plan (whole target file). */
export function ffmpegArgs(plan, options = {}) {
  let filter = null;
  if (isSplit(plan)) {
    const start = plan.splitStartSamples !== undefined
      ? `start_sample=${plan.splitStartSamples}`
      : `start=${plan.splitStartSeconds}`;
    const end = plan.splitEndSamples !== undefined ? `:end_sample=${plan.splitEndSamples}` : "";
    filter = `atrim=${start}${end},asetpts=PTS-STARTPTS`;
  } else if (options.trimSilence && plan.trimSilence) {
    filter = silenceFilter(options);
  }
  const args = [
    "-hide_banner", "-loglevel", "error", "-y",
    "-i", plan.path,
  ];
  if (filter) {
    args.push("-af", filter);
  }
  args.push("-vn", "-c:a", "libvorbis", "-q:a", String(vorbisQuality(plan, options) ?? 4));
  if (options.sampleRate) {
    args.push("-ar", String(options.sampleRate));
  }
  return args;
}

/** ffmpeg arguments for the intro segment [0, loop start) of a split plan. */
export function introFfmpegArgs(plan, options = {}) {
  const end = plan.splitStartSamples !== undefined
    ? `end_sample=${plan.splitStartSamples}`
    : `end=${plan.splitStartSeconds}`;
  return segmentArgs(plan, options, `atrim=${end},asetpts=PTS-STARTPTS`);
}

/** Manifest entry in the shape pokemon.runtime.data.AudioManifestData reads. */
export function manifestEntry(plan) {
  return {
    type: plan.category,
    file: `audio/${plan.target}`,
    source: plan.path.replace(/\\/g, "/"),
    encoding: "ogg/vorbis",
    sampleRate: plan.sampleRate ?? 0,
    loopStartSamples: plan.loopStartSamples ?? -1,
    loopEndSamples: plan.loopEndSamples ?? -1,
    loopStartSeconds: plan.loopStartSeconds ?? -1,
    loopEndSeconds: plan.loopEndSeconds ?? -1,
    ...(plan.introTarget ? { introFile: `audio/${plan.introTarget}` } : {}),
  };
}

/** The manifest document; ids live under "audio" for the runtime parser. */
export function emptyManifest(preset) {
  return { version: "pokemon-builder/audio-manifest/1", preset: preset || "standard", audio: {} };
}

function putEntry(manifest, plan) {
  if (!manifest.audio[plan.name]) {
    manifest.audio[plan.name] = manifestEntry(plan);
  }
}

/**
 * Runs the plan synchronously (the CLI is synchronous) and can be resumed: a
 * finished target is reused, so an interrupted run continues where it stopped.
 *
 * @param runner (command, args) => void; production passes execFileSync.
 * @param copy   (source, target) => void; defaults to fs.copyFileSync.
 */
export function runAudioBuild({ projectPath, generatedRoot, scan, options = {}, runner, copy }) {
  const { plans, problems } = planEncoding(scan, options, options.loopOverrides || {}, projectPath);
  const audioOut = path.join(generatedRoot, "audio");
  fs.mkdirSync(audioOut, { recursive: true });

  const manifest = emptyManifest(options.preset);
  const failures = [];
  let copied = 0;
  let transcoded = 0;
  let reused = 0;
  let looped = 0;
  let introFiles = 0;

  for (const plan of plans) {
    const source = path.join(projectPath, plan.path);
    const target = path.join(audioOut, plan.target);

    // Split BGMs are always rebuilt: both segments derive from the tags, and
    // the loop segment must replace an older full-file copy. Other files
    // resume by existence.
    const split = isSplit(plan);
    if (!split && !plan.trimSilence
        && fs.existsSync(target) && fs.statSync(target).size > 0) {
      reused++;
      putEntry(manifest, plan);
      continue;
    }

    fs.mkdirSync(path.dirname(target), { recursive: true });
    try {
      if (plan.action === "COPY") {
        (copy || fs.copyFileSync)(source, target);
        copied++;
      } else {
        runner(options.ffmpegPath || "ffmpeg",
          [...ffmpegArgs({ ...plan, path: source }, options), target]);
        transcoded++;
        if (plan.introTarget) {
          runner(options.ffmpegPath || "ffmpeg",
            [...introFfmpegArgs({ ...plan, path: source }, options),
              path.join(audioOut, plan.introTarget)]);
          introFiles++;
        }
      }
      if (split) {
        looped++;
      }
      putEntry(manifest, plan);
    } catch (error) {
      failures.push({ file: plan.path, reason: String((error && error.message) || error) });
    }
  }

  return {
    manifest,
    problems: [...problems, ...failures],
    copied,
    transcoded,
    reused,
    looped,
    introFiles,
    skipped: problems.length,
    failed: failures.length,
  };
}
