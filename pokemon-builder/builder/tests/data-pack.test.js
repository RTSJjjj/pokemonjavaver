// L3/L4: the data pack writer and the desktop runtime-asset copier.
//
// The zip createDataPack writes must be a real, readable ZIP, so the test
// parses the central directory and extracts entries with its own tiny reader
// (plus zlib inflate) instead of trusting the writer's internal state.

import test from "node:test";
import assert from "node:assert/strict";
import { inflateRawSync } from "node:zlib";
import { existsSync, mkdirSync, mkdtempSync, readFileSync, utimesSync, writeFileSync } from "node:fs";
import os from "node:os";
import path from "node:path";

import { copyRuntimeAssets, createDataPack } from "../src/data-pack.js";

function scratch() {
  return mkdtempSync(path.join(os.tmpdir(), "pb-pack-"));
}

function writeTree(root, files) {
  for (const [relative, content] of Object.entries(files)) {
    const file = path.join(root, relative);
    mkdirSync(path.dirname(file), { recursive: true });
    writeFileSync(file, content);
  }
}

function readEntries(file) {
  const buffer = readFileSync(file);
  const eocd = buffer.lastIndexOf(Buffer.from([0x50, 0x4b, 0x05, 0x06]));
  assert.ok(eocd >= 0, "end-of-central-directory record present");
  const count = buffer.readUInt16LE(eocd + 10);
  let offset = buffer.readUInt32LE(eocd + 16);
  const entries = [];
  for (let i = 0; i < count; i++) {
    assert.equal(buffer.readUInt32LE(offset), 0x02014b50, "central directory header signature");
    const nameLength = buffer.readUInt16LE(offset + 28);
    const extraLength = buffer.readUInt16LE(offset + 30);
    const commentLength = buffer.readUInt16LE(offset + 32);
    entries.push({
      name: buffer.toString("utf8", offset + 46, offset + 46 + nameLength),
      method: buffer.readUInt16LE(offset + 10),
      compressed: buffer.readUInt32LE(offset + 20),
      uncompressed: buffer.readUInt32LE(offset + 24),
      localOffset: buffer.readUInt32LE(offset + 42),
    });
    offset += 46 + nameLength + extraLength + commentLength;
  }
  return { buffer, entries };
}

function readEntryData(buffer, entry) {
  assert.equal(buffer.readUInt32LE(entry.localOffset), 0x04034b50, "local header signature");
  const nameLength = buffer.readUInt16LE(entry.localOffset + 26);
  const extraLength = buffer.readUInt16LE(entry.localOffset + 28);
  const start = entry.localOffset + 30 + nameLength + extraLength;
  const payload = buffer.subarray(start, start + entry.compressed);
  assert.equal(payload.length, entry.compressed);
  return entry.method === 0 ? Buffer.from(payload) : inflateRawSync(payload);
}

test("createDataPack writes a readable zip with prefixed, sorted entries (L3)", () => {
  const root = scratch();
  const generated = path.join(root, "generated");
  const graphics = path.join(root, "Graphics");
  const fonts = path.join(root, "Fonts");
  writeTree(generated, {
    "b.txt": "second",
    "a/x.json": "{\"hello\":\"world\"}",
  });
  writeTree(graphics, { "img.png": Buffer.from([1, 2, 3, 4, 5]) });
  writeTree(fonts, { "font.ttf": "font-bytes" });

  const zipPath = path.join(root, "out", "runtime-data.zip");
  const pack = createDataPack(
    [
      { root: generated, prefix: "generated" },
      { root: graphics, prefix: "Graphics" },
      { root: fonts, prefix: "Fonts" },
    ],
    zipPath,
  );

  assert.equal(pack.files, 4);
  assert.match(pack.version, /^[0-9a-f]{16}-\d+$/);

  const { buffer, entries } = readEntries(zipPath);
  assert.deepEqual(
    entries.map((entry) => entry.name),
    ["Fonts/font.ttf", "Graphics/img.png", "generated/a/x.json", "generated/b.txt"],
  );
  const byName = new Map(entries.map((entry) => [entry.name, entry]));
  assert.equal(readEntryData(buffer, byName.get("generated/a/x.json")).toString("utf8"), "{\"hello\":\"world\"}");
  assert.equal(readEntryData(buffer, byName.get("generated/b.txt")).toString("utf8"), "second");
  assert.equal(readEntryData(buffer, byName.get("Fonts/font.ttf")).toString("utf8"), "font-bytes");
  assert.deepEqual([...readEntryData(buffer, byName.get("Graphics/img.png"))], [1, 2, 3, 4, 5]);
  for (const entry of entries) {
    assert.equal(entry.uncompressed, readEntryData(buffer, entry).length);
  }
});

test("the pack version is content-addressed (L3)", () => {
  const root = scratch();
  writeTree(path.join(root, "generated"), { "a.json": "one" });
  const first = createDataPack([{ root: path.join(root, "generated"), prefix: "generated" }],
    path.join(root, "one.zip"));
  const second = createDataPack([{ root: path.join(root, "generated"), prefix: "generated" }],
    path.join(root, "two.zip"));
  assert.equal(first.version, second.version, "identical content keeps the unpack marker stable");

  writeFileSync(path.join(root, "generated", "a.json"), "two");
  const third = createDataPack([{ root: path.join(root, "generated"), prefix: "generated" }],
    path.join(root, "three.zip"));
  assert.notEqual(third.version, first.version, "changed content forces a re-unpack");
});

test("missing source roots contribute nothing instead of failing (L3)", () => {
  const root = scratch();
  writeTree(path.join(root, "generated"), { "a.json": "x" });
  const pack = createDataPack(
    [
      { root: path.join(root, "generated"), prefix: "generated" },
      { root: path.join(root, "Graphics"), prefix: "Graphics" }, // does not exist
    ],
    path.join(root, "pack.zip"),
  );
  assert.equal(pack.files, 1);
});

test("copyRuntimeAssets copies Graphics and Fonts, then skips up-to-date files (L4)", () => {
  const root = scratch();
  const project = path.join(root, "project");
  const target = path.join(root, "runtime-data");
  writeTree(project, {
    "Graphics/Pictures/pause.png": "png",
    "Graphics/Tilesets/tiles.png": "tiles",
    "Fonts/font.ttf": "font",
    "Audio/bgm.ogg": "not copied by L4",
  });

  const first = copyRuntimeAssets(project, target, null);
  assert.equal(first.files, 3);
  assert.equal(readFileSync(path.join(target, "Graphics/Pictures/pause.png"), "utf8"), "png");
  assert.equal(readFileSync(path.join(target, "Fonts/font.ttf"), "utf8"), "font");
  assert.ok(!existsSync(path.join(target, "Audio", "bgm.ogg")), "Audio is not part of the L4 asset copy");

  const second = copyRuntimeAssets(project, target, null);
  assert.equal(second.files, 0, "unchanged files are not copied again");

  const source = path.join(project, "Graphics/Pictures/pause.png");
  const future = new Date(Date.now() + 5000);
  utimesSync(source, future, future);
  const third = copyRuntimeAssets(project, target, null);
  assert.equal(third.files, 1, "a newer source file is copied again");
});
