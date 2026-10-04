// Logger unit tests (Phase 13).
//
// Every run must record the command, the project path, the builder version,
// the steps, the warnings, the errors, the duration and every output file in
// logs/latest.log plus a timestamped log - so a failed or partial build can
// always be reconstructed after the fact.

import test from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, readFileSync, readdirSync, rmSync } from "node:fs";
import path from "node:path";
import os from "node:os";

import { Logger } from "../src/logger.js";

function makeLogsDir(t) {
  const dir = mkdtempSync(path.join(os.tmpdir(), "pb-logs-"));
  t.after(() => rmSync(dir, { recursive: true, force: true }));
  return dir;
}

test("latest.log and the timestamped log record the whole run", (t) => {
  const dir = makeLogsDir(t);
  const logger = new Logger(dir, {
    version: "9.9.9",
    command: "build-data",
    argv: ["build-data", "D:\\Game\\Pokemon Project"],
    project: "D:\\Game\\Pokemon Project",
  });
  logger.step("[1/3] Validating project...");
  logger.warn("PBS directory not found: D:\\Game\\Pokemon Project\\PBS");
  logger.error("unreadable data: Map001.rxdata -> bad marshal version");
  logger.output("D:\\builder\\generated\\maps\\map-001.json");
  logger.info("conversion finished");
  logger.finish(1);

  const latest = readFileSync(path.join(dir, "latest.log"), "utf8");
  assert.match(latest, /=== Pokemon Builder 9\.9\.9 ===/);
  assert.match(latest, /Command: build-data/);
  assert.match(latest, /Arguments: build-data D:\\Game\\Pokemon Project/);
  assert.match(latest, /Project: D:\\Game\\Pokemon Project/);
  assert.match(latest, /Started: \d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?Z/);
  assert.match(latest, /\[STEP\] \[1\/3\] Validating project/);
  assert.match(latest, /\[WARN\] PBS directory not found/);
  assert.match(latest, /\[ERROR\] unreadable data: Map001\.rxdata/);
  assert.match(latest, /\[INFO\] conversion finished/);
  // Output files are recorded in the log but never printed to the console.
  assert.match(latest, /\[OUTPUT\] D:\\builder\\generated\\maps\\map-001\.json/);
  assert.match(latest, /Finished: \d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?Z exit=1 duration=\d+ms/);

  const stamped = readdirSync(dir).filter((name) => /^\d{4}-\d{2}-\d{2}_\d{6}\.log$/.test(name));
  assert.equal(stamped.length, 1, "exactly one timestamped log expected");
  assert.equal(readFileSync(path.join(dir, stamped[0]), "utf8"), latest);
});

test("finish records the exit code and the duration", (t) => {
  const dir = makeLogsDir(t);
  const logger = new Logger(dir, { version: "0.1.0", command: "validate", argv: ["validate"], project: "(not set)" });
  logger.finish(0);
  const latest = readFileSync(path.join(dir, "latest.log"), "utf8");
  assert.match(latest, /exit=0 duration=\d+ms/);
  assert.match(latest, /Project: \(not set\)/);
});

test("a second run keeps its own log files and overwrites latest.log", (t) => {
  const dir = makeLogsDir(t);
  const first = new Logger(dir, { version: "1", command: "audit", argv: ["audit"], project: "A" });
  first.step("first run step");
  first.finish(0);
  const firstStamp = path.basename(first.logFile);

  const second = new Logger(dir, { version: "1", command: "clean", argv: ["clean"], project: "A" });
  second.step("second run step");
  second.finish(0);

  // latest.log is the current run...
  const latest = readFileSync(path.join(dir, "latest.log"), "utf8");
  assert.match(latest, /second run step/);
  assert.ok(!/first run step/.test(latest));
  // ...and the archived timestamped logs of earlier runs stay readable.
  const firstLog = readFileSync(path.join(dir, firstStamp), "utf8");
  assert.match(firstLog, /first run step/);
  const stamps = readdirSync(dir).filter((name) => /^\d{4}-\d{2}-\d{2}_\d{6}\.log$/.test(name));
  assert.equal(stamps.length, 1, "runs within the same second share the timestamp file");
});