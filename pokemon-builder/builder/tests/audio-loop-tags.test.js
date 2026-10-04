// L8.1 loop region tests: a tagged BGM is split into [loop segment] + [intro
// segment]; the intro plays once, then the loop segment loops as a whole file.

import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

import { loopRegion, readLoopStartSamples } from "../../tools/audio-compiler/loop-tags.js";
import { ffmpegArgs, introFfmpegArgs, planFile, runAudioBuild } from "../../tools/audio-compiler/encoder.js";

function oggWith(comment) {
  const file = path.join(fs.mkdtempSync(path.join(os.tmpdir(), "loop-tag-")), "song.ogg");
  const uint = n => { const b = Buffer.alloc(4); b.writeUInt32LE(n); return b; };
  const comments = Array.isArray(comment) ? comment : [comment];
  fs.writeFileSync(file, Buffer.concat([Buffer.from("OggS....\x03vorbis", "latin1"), uint(0), uint(comments.length),
    ...comments.flatMap(c => [uint(Buffer.byteLength(c)), Buffer.from(c)]), Buffer.from([1])]));
  return file;
}

test("LOOPSTART is read from the Vorbis comment header", () => {
  const file = oggWith("LOOPSTART=1928730");
  assert.equal(readLoopStartSamples(file), 1928730);
  assert.equal(readLoopStartSamples(oggWith("just a comment")), -1);
});

test("a loop tag splits the BGM into a loop segment and an intro segment", () => {
  const file = oggWith("LOOPSTART=44100");
  const args = { name: "song", sampleRate: 44100 };
  assert.deepEqual(loopRegion({ ...args, file }), {
    startSamples: 44100, endSamples: -1, startSeconds: 1, endSeconds: -1, sampleRate: 44100, source: "tags",
  });
  assert.equal(loopRegion({ ...args, file: oggWith("no tags here") }), null);

  const plan = planFile({
    category: "BGM", relativePath: file, ext: ".ogg", codec: "Ogg / Vorbis",
    name: "song", sampleRate: 44100, duration: 10,
  }, {}, {}, "");
  assert.equal(plan.action, "TRANSCODE");
  assert.equal(plan.target, "BGM/song.ogg");
  assert.equal(plan.introTarget, "BGM/song.intro.ogg");
  assert.equal(plan.splitStartSamples, 44100);
  assert.equal(plan.splitEndSamples, undefined);
  assert.equal(plan.loopStartSeconds, 1);
  assert.equal(plan.loopEndSeconds, -1);
});

test("LOOPSTART=0 has no intro to split off", () => {
  const plan = planFile({
    category: "BGM", relativePath: oggWith(["LOOPSTART=0", "LOOPEND=96000"]), ext: ".ogg",
    codec: "Ogg / Vorbis", name: "song", sampleRate: 48000, duration: 10,
  }, {}, {}, "");
  assert.equal(plan.action, "TRANSCODE");
  assert.equal(plan.introTarget, undefined);
  assert.equal(plan.splitStartSamples, 0);
  assert.equal(plan.splitEndSamples, 96000);
});

test("a manual override gives a loop start in seconds (whole track split)", () => {
  const file = oggWith("no tags here");
  assert.deepEqual(loopRegion({ file, name: "Battle wild", sampleRate: 44100, overrides: { "Battle wild": 3.5 } }), {
    startSamples: -1, endSamples: -1, startSeconds: 3.5, endSeconds: -1, sampleRate: 44100, source: "override",
  });
  assert.equal(loopRegion({ file, name: "other", sampleRate: 44100, overrides: { "Battle wild": 3.5 } }), null);

  const plan = planFile({
    category: "BGM", relativePath: file, ext: ".ogg", codec: "Ogg / Vorbis",
    name: "Battle wild", sampleRate: 44100, duration: 10,
  }, {}, { "Battle wild": 3.5 }, "");
  assert.equal(plan.splitStartSeconds, 3.5);
  assert.equal(plan.splitStartSamples, undefined);
  assert.equal(plan.introTarget, "BGM/Battle wild.intro.ogg");
});

test("ffmpeg cuts the loop segment and the intro segment at the exact points", () => {
  const loopArgs = ffmpegArgs(
    { category: "BGM", path: "/in.ogg", splitStartSamples: 44100, splitEndSamples: 132300 }, { bgmQuality: 4 });
  assert.equal(loopArgs[loopArgs.indexOf("-af") + 1],
    "atrim=start_sample=44100:end_sample=132300,asetpts=PTS-STARTPTS");
  const introArgs = introFfmpegArgs({ category: "BGM", path: "/in.ogg", splitStartSamples: 44100 }, { bgmQuality: 4 });
  assert.equal(introArgs[introArgs.indexOf("-af") + 1], "atrim=end_sample=44100,asetpts=PTS-STARTPTS");
  assert.ok(!loopArgs.join(" ").includes("-ss"));

  // A time-based override uses atrim seconds for tracks without sample tags.
  const bySeconds = ffmpegArgs({ category: "BGM", path: "/in.ogg", splitStartSeconds: 3.5 }, {});
  assert.equal(bySeconds[bySeconds.indexOf("-af") + 1], "atrim=start=3.5,asetpts=PTS-STARTPTS");
});

test("--trim-silence keeps the two-pass filter for files without a region", () => {
  const args = ffmpegArgs({ category: "SE", path: "/in.ogg", trimSilence: true }, { trimSilence: true });
  const filter = args[args.indexOf("-af") + 1];
  assert.match(filter, /silenceremove=start_periods=1/);
  assert.match(filter, /areverse.*areverse/);

  const copy = planFile({ category: "SE", relativePath: "Audio/SE/a.ogg", ext: ".ogg", codec: "Ogg / Vorbis", name: "a" },
      { trimSilence: true }, {}, "P");
  assert.equal(copy.action, "TRANSCODE", "an Ogg with silence trimming must be re-encoded");
  const untouched = planFile({ category: "SE", relativePath: "Audio/SE/a.ogg", ext: ".ogg", codec: "Ogg / Vorbis", name: "a" },
      {}, {}, "P");
  assert.equal(untouched.action, "COPY");

  // A tagged region wins over silence trimming (it would move the split points).
  const tagged = planFile({ category: "BGM", relativePath: oggWith("LOOPSTART=48000"), ext: ".ogg",
      codec: "Ogg / Vorbis", name: "song", sampleRate: 48000, duration: 10 },
      { trimSilence: true }, {}, "");
  assert.equal(tagged.trimSilence, undefined);
  assert.equal(tagged.action, "TRANSCODE");
  assert.equal(tagged.introTarget, "BGM/song.intro.ogg");
});

test("LOOPEND wins over LOOPLENGTH, both at the source sample rate", () => {
  const args = { name: "song", sampleRate: 48000, duration: 10 };
  const region = loopRegion({ ...args, file: oggWith(["loopstart=48000", "LoopEnd=144000"]) });
  assert.deepEqual(region, {
    startSamples: 48000, endSamples: 144000, startSeconds: 1, endSeconds: 3, sampleRate: 48000, source: "tags",
  });
  assert.deepEqual(loopRegion({ ...args, file: oggWith(["LOOPSTART=48000", "LOOPLENGTH=96000"]) }), region);
  assert.equal(loopRegion({ ...args, file: oggWith(["LOOPSTART=48000", "LOOPEND=144000", "LOOPLENGTH=48000"]) }).endSamples, 144000);
});

test("invalid regions and malformed values create no region (or fall back to the file end)", () => {
  const args = { name: "song", sampleRate: 48000, duration: 10 };
  for (const comments of [
    ["LOOPSTART=1.5"], ["LOOPSTART=9007199254740993"],
    ["LOOPSTART=1", "LOOPSTART=2"], ["LOOPEND=144000"], ["LOOPSTART=480000"],
  ]) assert.equal(loopRegion({ ...args, file: oggWith(comments) }), null, JSON.stringify(comments));
  assert.equal(loopRegion({ ...args, sampleRate: null, file: oggWith(["LOOPSTART=48000", "LOOPEND=144000"]) }), null);
  assert.equal(readLoopStartSamples(oggWith("DESCRIPTION=LOOPSTART=48000")), -1);
  // A broken end loops to the file end instead of dropping the region.
  assert.deepEqual(loopRegion({ ...args, file: oggWith(["LOOPSTART=48000", "LOOPEND=48000"]) }), {
    startSamples: 48000, endSamples: -1, startSeconds: 1, endSeconds: -1, sampleRate: 48000, source: "tags",
  });
});

test("split BGMs always rebuild both segments and land in the manifest", () => {
  const file = oggWith(["LOOPSTART=48000", "LOOPEND=240000"]);
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "region-build-"));
  const target = path.join(root, "audio", "BGM", "song.ogg");
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.writeFileSync(target, "old loop segment");
  fs.writeFileSync(path.join(root, "audio", "BGM", "song.intro.ogg"), "old intro");
  const input = { category: "BGM", relativePath: file, ext: ".ogg", codec: "Vorbis", name: "song", sampleRate: 48000, duration: 10 };
  const commands = [];
  const result = runAudioBuild({
    projectPath: "", generatedRoot: root, scan: { files: [input] }, options: {},
    runner: (command, args) => commands.push(args),
  });
  assert.equal(result.reused, 0, "the old segments must be rebuilt");
  assert.equal(result.transcoded, 1);
  assert.equal(result.looped, 1);
  assert.equal(result.introFiles, 1);
  assert.equal(commands.length, 2, "loop segment + intro segment");
  assert.ok(commands[0].at(-1).endsWith("song.ogg"));
  assert.ok(commands[1].at(-1).endsWith("song.intro.ogg"));
  const entry = result.manifest.audio.song;
  assert.equal(entry.loopStartSeconds, 1);
  assert.equal(entry.loopEndSeconds, 5);
  assert.equal(entry.introFile, "audio/BGM/song.intro.ogg");

  // From LOOPSTART=0 there is only the loop segment (no intro output).
  const fromZero = { category: "BGM", relativePath: oggWith(["LOOPSTART=0", "LOOPEND=96000"]), ext: ".ogg",
    codec: "Vorbis", name: "zero", sampleRate: 48000, duration: 10 };
  const second = runAudioBuild({
    projectPath: "", generatedRoot: root, scan: { files: [fromZero] }, options: {},
    runner: (command, args) => commands.push(args),
  });
  assert.equal(second.introFiles, 0);
  assert.equal(second.looped, 1);
  assert.equal("introFile" in second.manifest.audio.zero, false);
  fs.rmSync(root, { recursive: true, force: true });
});

test("an untagged BGM still resumes by existence", () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "region-reuse-"));
  const plain = { category: "BGM", relativePath: oggWith("no tags"), ext: ".ogg", codec: "Vorbis", name: "plain", sampleRate: 48000, duration: 10 };
  fs.mkdirSync(path.join(root, "audio", "BGM"), { recursive: true });
  fs.writeFileSync(path.join(root, "audio", "BGM", "plain.ogg"), "kept");
  const result = runAudioBuild({ projectPath: "", generatedRoot: root, scan: { files: [plain] }, options: {} });
  assert.equal(result.reused, 1);
  assert.equal(result.transcoded, 0);
  fs.rmSync(root, { recursive: true, force: true });
});

test("BGS, ME and SE never get regions", () => {
  const file = oggWith(["LOOPSTART=0", "LOOPEND=96000"]);
  for (const category of ["SE", "BGS", "ME"]) {
    const plan = planFile({ category, relativePath: file, ext: ".ogg", codec: "Vorbis", name: "song", sampleRate: 48000, duration: 10 }, {});
    assert.equal(plan.action, "COPY");
    assert.equal(plan.splitStartSamples, undefined);
    assert.equal(plan.introTarget, undefined);
  }
});
