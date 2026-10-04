// A4 tests: encoding plan + manifest shape (no ffmpeg needed).

import test from "node:test";
import assert from "node:assert/strict";

import {
  planFile,
  planEncoding,
  ffmpegArgs,
  manifestEntry,
  runAudioBuild,
} from "../../tools/audio-compiler/encoder.js";

const file = (over) => ({
  category: "SE",
  path: "Audio/SE/Door enter.wav",
  name: "Door enter",
  extension: ".wav",
  codec: "WAV (RIFF) / PCM 16-bit",
  size: 100,
  sha256: "abc",
  ...over,
});

test("Ogg Vorbis is copied, PCM is transcoded, MIDI is reported", () => {
  assert.equal(planFile(file({ extension: ".ogg", codec: "Ogg / Vorbis" })).action, "COPY");
  assert.equal(planFile(file()).action, "TRANSCODE");
  const midi = planFile(file({ extension: ".mid", codec: "MIDI sequence", unsupported: true }));
  assert.equal(midi.action, "SKIP");
  assert.match(midi.reason, /MIDI/);
  assert.equal(planEncoding({ files: [file(), file({ extension: ".mid", unsupported: true })] }).problems.length, 1);
});

test("ffmpeg arguments select libvorbis with the preset quality", () => {
  const args = ffmpegArgs({ category: "BGM", path: "/in.wav" }, { bgmQuality: 5, sampleRate: 44100 });
  assert.ok(args.includes("libvorbis"));
  assert.equal(args[args.indexOf("-q:a") + 1], "5");
  assert.equal(args[args.indexOf("-ar") + 1], "44100");
});

test("manifest entries match the runtime shape", () => {
  assert.deepEqual(
    manifestEntry({ category: "SE", path: "Audio/SE/Door enter.wav", target: "SE/Door enter.ogg" }),
    {
      type: "SE",
      file: "audio/SE/Door enter.ogg",
      source: "Audio/SE/Door enter.wav",
      encoding: "ogg/vorbis",
      sampleRate: 0,
      loopStartSamples: -1,
      loopEndSamples: -1,
      loopStartSeconds: -1,
      loopEndSeconds: -1,
    },
  );
});

test("a tagged BGM lands in the manifest with its loop region and intro file", () => {
  const entry = manifestEntry({
    category: "BGM",
    path: "Audio/BGM/song.ogg",
    target: "BGM/song.ogg",
    sampleRate: 48000,
    loopStartSamples: 48000,
    loopEndSamples: 144000,
    loopStartSeconds: 1,
    loopEndSeconds: 3,
    introTarget: "BGM/song.intro.ogg",
  });
  assert.equal(entry.loopStartSeconds, 1);
  assert.equal(entry.loopEndSeconds, 3);
  assert.equal(entry.sampleRate, 48000);
  assert.equal(entry.introFile, "audio/BGM/song.intro.ogg");
});

test("untagged entries carry no introFile", () => {
  const entry = manifestEntry({ category: "BGM", path: "Audio/BGM/plain.ogg", target: "BGM/plain.ogg" });
  assert.equal("introFile" in entry, false);
});

test("the build copies, transcodes and records failures", () => {
  const scan = {
    files: [
      file({ extension: ".ogg", codec: "Ogg / Vorbis", name: "theme" }),
      file({ name: "Door enter" }),
      file({ name: "broken", path: "Audio/SE/broken.wav" }),
      file({ extension: ".mid", codec: "MIDI sequence", unsupported: true, name: "tune" }),
    ],
  };
  const result = runAudioBuild({
    projectPath: "P",
    generatedRoot: process.env.TEMP || "/tmp",
    scan,
    options: { preset: "standard", bgmQuality: 4, bgsQuality: 3 },
    runner: (command, args) => {
      if (String(args[args.indexOf("-i") + 1]).includes("broken")) throw new Error("ffmpeg failed");
    },
    copy: () => {},
  });

  assert.equal(result.copied, 1);
  assert.equal(result.transcoded, 1);
  assert.equal(result.failed, 1);
  assert.equal(result.skipped, 1);
  assert.ok(result.manifest.audio.theme, "copied audio lands in the manifest");
  assert.ok(result.manifest.audio["Door enter"]);
  assert.equal(result.manifest.preset, "standard");
  assert.ok(result.problems.some((p) => /ffmpeg failed/.test(p.reason)));
});

test("an interrupted run can be resumed: finished targets are reused", async () => {
  const fs = await import("node:fs");
  const path = await import("node:path");
  const os = await import("node:os");
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "audio-resume-"));
  const target = path.join(root, "audio", "SE", "Door enter.ogg");
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.writeFileSync(target, "already converted");

  const result = runAudioBuild({
    projectPath: "P",
    generatedRoot: root,
    scan: { files: [file({ name: "Door enter" })] },
    options: { preset: "standard" },
    runner: () => {
      throw new Error("ffmpeg must not run for a finished target");
    },
    copy: () => {},
  });

  assert.equal(result.reused, 1);
  assert.equal(result.transcoded, 0);
  assert.ok(result.manifest.audio["Door enter"], "reused audio still reaches the manifest");
  fs.rmSync(root, { recursive: true, force: true });
});
