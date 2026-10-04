// Vorbis loop comments use source-rate sample positions; LOOPEND is exclusive.
// L8.1 (after user feedback): the builder no longer trims the intro away. The
// region is recorded for the runtime, which plays the intro once and then seeks
// back to LOOPSTART when the stream reaches LOOPEND.
import fs from "node:fs";

export function readLoopTags(file) {
  const missing = { start: -1, end: -1, length: -1 };
  try {
    const handle = fs.openSync(file, "r");
    let buffer;
    try {
      buffer = Buffer.alloc(65536);
      buffer = buffer.subarray(0, fs.readSync(handle, buffer, 0, buffer.length, 0));
    } finally { fs.closeSync(handle); }
    const packet = buffer.indexOf(Buffer.from("\x03vorbis", "latin1"));
    if (packet < 0) return missing;
    let offset = packet + 7;
    const uint = () => {
      if (offset + 4 > buffer.length) throw new Error("truncated comment");
      const n = buffer.readUInt32LE(offset); offset += 4; return n;
    };
    const vendorLength = uint(); offset += vendorLength;
    const count = uint();
    const tags = {};
    for (let i = 0; i < count; i++) {
      const length = uint();
      if (offset + length > buffer.length) throw new Error("truncated comment");
      const text = buffer.subarray(offset, offset + length).toString("utf8"); offset += length;
      const match = /^(LOOPSTART|LOOPEND|LOOPLENGTH)=(\d+)$/i.exec(text);
      if (/^(LOOPSTART|LOOPEND|LOOPLENGTH)=/i.test(text) && !match) return missing;
      if (match) {
        const key = match[1].toUpperCase();
        const value = Number(match[2]);
        if (!Number.isSafeInteger(value) || (tags[key] !== undefined && tags[key] !== value)) return missing;
        tags[key] = value;
      }
    }
    return { start: tags.LOOPSTART ?? -1, end: tags.LOOPEND ?? -1, length: tags.LOOPLENGTH ?? -1 };
  } catch { return missing; }
}

export function readLoopStartSamples(file) { return readLoopTags(file).start; }

function toRegion(tags, rate, duration) {
  if (tags.start < 0 || !Number.isFinite(rate) || rate <= 0) return null;
  if (Number.isFinite(duration) && tags.start / rate >= duration) return null;
  let end = tags.end >= 0 ? tags.end : tags.length > 0 ? tags.start + tags.length : -1;
  // A broken end loops to the file end instead of dropping the whole region.
  if (end !== -1 && (!Number.isSafeInteger(end) || end <= tags.start
      || (Number.isFinite(duration) && end / rate > duration + 1 / rate))) {
    end = -1;
  }
  return {
    startSamples: tags.start,
    endSamples: end,
    startSeconds: tags.start / rate,
    endSeconds: end >= 0 ? end / rate : -1,
    sampleRate: rate,
    source: "tags",
  };
}

/**
 * Loop region of one file, or null when it has none.
 *
 * <p>The numeric overrides ({@code audio-loop-overrides.json}) give a loop
 * start in seconds for tracks without tags; the region then runs to the file
 * end. They no longer mean "seconds to cut".</p>
 */
export function loopRegion({ file, name, sampleRate, duration, overrides = {} }) {
  if (Object.hasOwn(overrides, name)) {
    const seconds = Number(overrides[name]);
    if (Number.isFinite(seconds) && seconds >= 0) {
      return {
        startSamples: -1,
        endSamples: -1,
        startSeconds: seconds,
        endSeconds: -1,
        sampleRate: Number(sampleRate) || 0,
        source: "override",
      };
    }
  }
  return toRegion(readLoopTags(file), Number(sampleRate), duration);
}
