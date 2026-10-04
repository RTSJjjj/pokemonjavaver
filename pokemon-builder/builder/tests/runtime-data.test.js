// Unit tests for step [6/8]: runtime data validation (project3 section 63).

import test from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, writeFileSync } from "node:fs";
import path from "node:path";
import os from "node:os";

import { runtimeDataSummary, validateRuntimeData } from "../src/runtime-data.js";

function makeGenerated() {
  const dir = mkdtempSync(path.join(os.tmpdir(), "pb-generated-"));
  mkdirSync(path.join(dir, "scripts"), { recursive: true });
  mkdirSync(path.join(dir, "audio"), { recursive: true });
  mkdirSync(path.join(dir, "maps"), { recursive: true });
  writeFileSync(path.join(dir, "project.json"), JSON.stringify({ counts: { maps: 3, events: 4 } }), "utf8");
  writeFileSync(path.join(dir, "scripts", "ir.json"), JSON.stringify({ commands: [] }), "utf8");
  writeFileSync(path.join(dir, "audio", "audio-manifest.json"), JSON.stringify({ audio: { a: {}, b: {} } }), "utf8");
  return dir;
}

test("validateRuntimeData accepts a complete generated tree", () => {
  const dir = makeGenerated();
  const result = validateRuntimeData(dir);
  assert.equal(result.ok, true);
  assert.deepEqual(result.problems, []);
  assert.equal(result.project.counts.maps, 3);
  assert.deepEqual(result.ir.commands, []);
});

test("validateRuntimeData reports every missing piece", () => {
  const dir = mkdtempSync(path.join(os.tmpdir(), "pb-generated-empty-"));
  const result = validateRuntimeData(dir);
  assert.equal(result.ok, false);
  assert.equal(result.problems.length, 3);
  assert.match(result.problems.join("\n"), /project\.json/);
  assert.match(result.problems.join("\n"), /scripts\/ir\.json/);
  assert.match(result.problems.join("\n"), /audio-manifest\.json/);
});

test("validateRuntimeData rejects unreadable JSON and a broken IR shape", () => {
  const dir = makeGenerated();
  writeFileSync(path.join(dir, "project.json"), "{not json", "utf8");
  writeFileSync(path.join(dir, "scripts", "ir.json"), JSON.stringify({ noCommands: true }), "utf8");
  const result = validateRuntimeData(dir);
  assert.equal(result.ok, false);
  assert.match(result.problems.join("\n"), /project\.json is not readable JSON/);
  assert.match(result.problems.join("\n"), /has no commands array/);
});

test("runtimeDataSummary reads the report counts", () => {
  const dir = makeGenerated();
  const summary = runtimeDataSummary(dir);
  assert.equal(summary.maps, 3);
  assert.equal(summary.events, 4);
  assert.equal(summary.audioAssets, 2);
});

test("runtimeDataSummary falls back to the maps directory when counts are absent", () => {
  const dir = makeGenerated();
  writeFileSync(path.join(dir, "project.json"), JSON.stringify({}), "utf8");
  writeFileSync(path.join(dir, "maps", "map-001.json"), "{}", "utf8");
  writeFileSync(path.join(dir, "maps", "map-002.json"), "{}", "utf8");
  const summary = runtimeDataSummary(dir);
  assert.equal(summary.maps, 2);
  assert.equal(summary.events, 0);
});
