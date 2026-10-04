// scripts/*.bat wrapper tests (Phase 7 audit-project.bat, Phase 8 build-data.bat).
//
// The wrappers are batch files, so these tests really execute them through
// cmd.exe. Every run happens inside a throwaway copy of the builder: the real
// docs/, build/ and logs/ directories are never touched, and nothing here
// writes into the RMXP project either (hard rule 1).

import test from "node:test";
import assert from "node:assert/strict";
import { writeFileSync, existsSync, readFileSync, mkdirSync, rmSync } from "node:fs";
import path from "node:path";
import os from "node:os";

import {
  BUILDER_SOURCE,
  REPORT_FILES,
  makeProject,
  copyBuilder,
  runAuditBat,
  runBuildDataBat,
  runBuildPcBat,
  runBuildAndroidBat,
  runBuildAndroidLegacyBat,
  runCleanBuildBat,
  outputOf,
  snapshotProject,
  scratch,
} from "./fixtures.js";

const WRAPPERS = [
  { file: "scripts/audit-project.bat", command: "audit", run: runAuditBat },
  { file: "scripts/build-data.bat", command: "build-data", run: runBuildDataBat },
  { file: "scripts/build-pc.bat", command: "build-pc", run: runBuildPcBat },
  { file: "scripts/build-android.bat", command: "build-android", run: runBuildAndroidBat },
  { file: "scripts/build-android-legacy.bat", command: "build-android-legacy", run: runBuildAndroidLegacyBat },
  { file: "scripts/clean-build.bat", command: "clean", run: runCleanBuildBat },
];

const IR_FILES = [
  "project.json",
  "maps/index.json",
  "events/index.json",
  "common-events/index.json",
  "scripts/sections.json",
  "scripts/blocks.json",
  "scripts/apis.json",
  "metadata/build.json",
  "metadata/sources.json",
  "metadata/problems.json",
];

function drop(target) {
  rmSync(target, { recursive: true, force: true });
}

test("audit-project.bat runs the whole audit pipeline", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => drop(root));
  const project = makeProject(dir, "FakeProject");

  const result = runAuditBat(root, project);
  const out = outputOf(result);
  assert.equal(result.status, 0, out);
  assert.match(out, /\[1\/5\] Validating project/);
  assert.match(out, /\[2\/5\] Scanning RMXP data/);
  assert.match(out, /\[3\/5\] Scanning RMXP events/);
  assert.match(out, /\[4\/5\] Analyzing event scripts/);
  assert.match(out, /\[5\/5\] Writing audit reports/);
  assert.match(out, /SCAN OK/);
  assert.match(out, /EVENT ANALYSIS OK/);
  assert.match(out, /SCRIPT ANALYSIS OK/);
  assert.match(out, /REPORTS OK/);
  assert.match(out, /AUDIT SUCCESS/);
  assert.match(out, /BUILD SUCCESS/);
  assert.ok(!/FAILED/.test(out), out);

  assert.ok(existsSync(path.join(root, "build", "reports", "scan.json")));
  assert.ok(existsSync(path.join(root, "build", "reports", "events.json")));
  assert.ok(existsSync(path.join(root, "build", "reports", "script-audit.json")));
  for (const report of REPORT_FILES) {
    const file = path.join(root, "docs", report);
    assert.ok(existsSync(file), report);
    assert.ok(readFileSync(file, "utf8").length > 50, report);
  }
  assert.ok(existsSync(path.join(root, "logs", "latest.log")));
  const log = readFileSync(path.join(root, "logs", "latest.log"), "utf8");
  assert.match(log, /Command: audit/);
  assert.match(log, /exit=0/);
});

test("audit-project.bat never writes into the source project", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => drop(root));
  const project = makeProject(dir, "Read Only Project");
  const before = snapshotProject(project);

  const result = runAuditBat(root, project);
  assert.equal(result.status, 0, outputOf(result));
  assert.deepEqual(snapshotProject(project), before);
});

test("audit-project.bat fails on a missing project path", (t) => {
  const root = copyBuilder();
  t.after(() => drop(root));
  const missing = path.join(os.tmpdir(), "pb-audit-missing-98765");

  const result = runAuditBat(root, missing);
  const out = outputOf(result);
  assert.equal(result.status, 1, out);
  assert.match(out, /VALIDATE FAILED/);
  assert.match(out, /BUILD FAILED/);
  assert.ok(!existsSync(path.join(root, "docs", "source-audit.md")));
});

test("audit-project.bat fails when the project data cannot be parsed", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => drop(root));
  const project = makeProject(dir, "BrokenProject");
  writeFileSync(path.join(project, "Data", "Map001.rxdata"), Buffer.from([4, 8, 0x58]));

  const result = runAuditBat(root, project);
  const out = outputOf(result);
  assert.equal(result.status, 1, out);
  assert.match(out, /SCAN FAILED/);
  assert.match(out, /BUILD FAILED/);
});

test("build-data.bat converts the project into the JSON Debug IR", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => drop(root));
  const project = makeProject(dir, "FakeProject");

  const result = runBuildDataBat(root, project);
  const out = outputOf(result);
  assert.equal(result.status, 0, out);
  assert.match(out, /\[1\/4\] Validating project/);
  assert.match(out, /\[2\/4\] Scanning RMXP data, events and Ruby scripts/);
  assert.match(out, /\[3\/4\] Converting to intermediate data/);
  assert.match(out, /\[4\/4\] Compiling Essentials script blocks/);
  assert.match(out, /BUILD DATA SUCCESS/);
  assert.match(out, /BUILD SUCCESS/);
  assert.ok(!/FAILED/.test(out), out);

  for (const file of IR_FILES) {
    assert.ok(existsSync(path.join(root, "generated", file)), file);
  }
  const manifest = JSON.parse(readFileSync(path.join(root, "generated", "project.json"), "utf8"));
  assert.equal(manifest.kind, "project");
  assert.equal(manifest.counts.maps, 1);
  assert.equal(manifest.counts.scriptBlocks, 1);
  assert.equal(manifest.counts.scriptSections, 2);

  const map = JSON.parse(readFileSync(path.join(root, "generated", "maps", "map-001.json"), "utf8"));
  assert.equal(map.kind, "map");
  assert.equal(map.mapId, 1);
  assert.equal(map.name, "Starting Map");
  assert.equal(map.events.length, 1);
  const scriptCommand = map.events[0].pages[0].commands.find((command) => command.code === 355);
  assert.equal(scriptCommand.parameters[0], "pbSet(1,1)");
  assert.equal(scriptCommand.scriptBlockId, "map1/event1/page1/cmd0");

  const events = JSON.parse(readFileSync(path.join(root, "generated", "events", "map-001.json"), "utf8"));
  assert.equal(events.scriptBlocks.length, 1);
  assert.equal(events.scriptBlocks[0].rubySource, "pbSet(1,1)\npbMain\n");

  const problems = JSON.parse(
    readFileSync(path.join(root, "generated", "metadata", "problems.json"), "utf8"),
  );
  assert.equal(problems.unreadableData.length, 0);
  assert.equal(problems.uninterpretableScripts.length, 0);
});

test("build-data.bat never writes into the source project", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => drop(root));
  const project = makeProject(dir, "Read Only Project");
  const before = snapshotProject(project);

  const result = runBuildDataBat(root, project);
  assert.equal(result.status, 0, outputOf(result));
  assert.deepEqual(snapshotProject(project), before);
});

test("build-pc.bat runs the data stages and fails honestly without Gradle", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => drop(root));
  const project = makeProject(dir, "FakeProject");

  const result = runBuildPcBat(root, project);
  const out = outputOf(result);
  // The scratch builder has no runtime/ checkout, so the Gradle step fails
  // loudly instead of pretending a build happened (exit code 1).
  assert.equal(result.status, 1, out);
  for (const label of ["[1/8]", "[2/8]", "[3/8]", "[4/8]", "[5/8]", "[6/8]", "[7/8]"]) {
    assert.ok(out.includes(label), label + " missing in output:\n" + out);
  }
  assert.match(out, /AUDIT SUCCESS/);
  assert.match(out, /DATA BUILD SUCCESS/);
  assert.match(out, /Gradle wrapper not found/);
  assert.match(out, /RUNTIME TESTS FAILED/);
  assert.match(out, /BUILD FAILED/);
  assert.ok(!/not implemented/i.test(out), out);

  for (const file of IR_FILES) {
    assert.ok(existsSync(path.join(root, "generated", file)), file);
  }
  assert.ok(existsSync(path.join(root, "docs", "source-audit.md")));
  const log = readFileSync(path.join(root, "logs", "latest.log"), "utf8");
  assert.match(log, /Command: build-pc/);
  assert.match(log, /exit=1/);
});

test("build-pc.bat never writes into the source project", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => drop(root));
  const project = makeProject(dir, "Read Only Project");
  const before = snapshotProject(project);

  const result = runBuildPcBat(root, project);
  assert.equal(result.status, 1, outputOf(result));
  assert.deepEqual(snapshotProject(project), before);
});

test("build-pc.bat fails on a missing project path", (t) => {
  const root = copyBuilder();
  t.after(() => drop(root));
  const missing = path.join(os.tmpdir(), "pb-buildpc-missing-98765");

  const result = runBuildPcBat(root, missing);
  const out = outputOf(result);
  assert.equal(result.status, 1, out);
  assert.match(out, /VALIDATE FAILED/);
  assert.match(out, /BUILD FAILED/);
  assert.ok(!/DATA BUILD SUCCESS/.test(out), out);
  assert.ok(!/PC Runtime is not implemented yet\./.test(out), out);
  assert.ok(!existsSync(path.join(root, "generated", "project.json")));
});

test("build-pc.bat fails when the project data cannot be parsed", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => drop(root));
  const project = makeProject(dir, "BrokenProject");
  writeFileSync(path.join(project, "Data", "Map001.rxdata"), Buffer.from([4, 8, 0x58]));

  const result = runBuildPcBat(root, project);
  const out = outputOf(result);
  assert.equal(result.status, 1, out);
  assert.match(out, /SCAN FAILED/);
  assert.match(out, /BUILD FAILED/);
  assert.ok(!/DATA BUILD SUCCESS/.test(out), out);
  assert.ok(!/PC Runtime is not implemented yet\./.test(out), out);
  assert.ok(!existsSync(path.join(root, "generated", "project.json")));
});
test("build-android.bat runs the data stages and fails honestly without Gradle", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => drop(root));
  const project = makeProject(dir, "FakeProject");

  const result = runBuildAndroidBat(root, project);
  const out = outputOf(result);
  assert.equal(result.status, 1, out);
  for (const label of ["[1/8]", "[2/8]", "[4/8]", "[5/8]", "[6/8]", "[7/8]"]) {
    assert.ok(out.includes(label), label + " missing in output:\n" + out);
  }
  assert.match(out, /AUDIT SUCCESS/);
  assert.match(out, /DATA BUILD SUCCESS/);
  assert.match(out, /android:assembleDebug/);
  assert.match(out, /GRADLE BUILD FAILED/);
  assert.match(out, /BUILD FAILED/);
  assert.ok(!/not implemented/i.test(out), out);

  for (const file of IR_FILES) {
    assert.ok(existsSync(path.join(root, "generated", file)), file);
  }
  assert.ok(existsSync(path.join(root, "docs", "source-audit.md")));
  // Shared game data, no packaged artifact.
  assert.ok(!existsSync(path.join(root, "dist", "PokemonGame-Android.apk")));
  const log = readFileSync(path.join(root, "logs", "latest.log"), "utf8");
  assert.match(log, /Command: build-android/);
  assert.match(log, /exit=1/);
});

test("build-android-legacy.bat uses its own Gradle module", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => drop(root));
  const project = makeProject(dir, "FakeProject");

  const result = runBuildAndroidLegacyBat(root, project);
  const out = outputOf(result);
  assert.equal(result.status, 1, out);
  assert.match(out, /DATA BUILD SUCCESS/);
  assert.match(out, /Android target: Legacy Android \(API 21\+ placeholder/);
  assert.match(out, /armeabi-v7a/);
  assert.match(out, /:android-legacy:assembleDebug/);
  assert.match(out, /dist\\PokemonGame-Android-Legacy\.apk/);
  assert.match(out, /GRADLE BUILD FAILED/);
  // Legacy never shares the modern artifact name or Gradle module.
  assert.ok(!/PokemonGame-Android\.apk/.test(out), out);
  assert.ok(!/:android:assembleDebug/.test(out), out);
  assert.ok(!existsSync(path.join(root, "dist", "PokemonGame-Android-Legacy.apk")));
  // Both targets share the same generated game data.
  assert.ok(existsSync(path.join(root, "generated", "project.json")));
});

test("build-android wrappers never write into the source project", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => drop(root));
  const project = makeProject(dir, "Read Only Project");
  const before = snapshotProject(project);

  for (const run of [runBuildAndroidBat, runBuildAndroidLegacyBat]) {
    assert.equal(run(root, project).status, 1);
  }
  assert.deepEqual(snapshotProject(project), before);
});

test("build-android.bat fails on a missing project path", (t) => {
  const root = copyBuilder();
  t.after(() => drop(root));
  const missing = path.join(os.tmpdir(), "pb-buildandroid-missing-98765");

  const result = runBuildAndroidBat(root, missing);
  const out = outputOf(result);
  assert.equal(result.status, 1, out);
  assert.match(out, /VALIDATE FAILED/);
  assert.match(out, /BUILD FAILED/);
  assert.ok(!/DATA BUILD SUCCESS/.test(out), out);
  assert.ok(!/Runtime is not implemented yet\./.test(out), out);
  assert.ok(!existsSync(path.join(root, "generated", "project.json")));
});

function seedBuildOutputs(root, project) {
  for (const dir of ["logs/build-temp", "dist", "build", "generated"]) {
    mkdirSync(path.join(root, dir), { recursive: true });
  }
  writeFileSync(path.join(root, "logs", "build-temp", "tmp.bin"), "x", "utf8");
  writeFileSync(path.join(root, "logs", "2020-01-01_000000.log"), "old", "utf8");
  writeFileSync(path.join(root, "dist", "old-package.bin"), "x", "utf8");
  writeFileSync(path.join(root, "build", "cache.json"), "x", "utf8");
  writeFileSync(path.join(root, "generated", "project.json"), "x", "utf8");
  writeFileSync(path.join(root, "docs-keep.md"), "keep", "utf8");
  writeFileSync(path.join(project, "Data", "Map001.rxdata"), "precious", "utf8");
}

test("clean-build.bat removes build outputs only", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => drop(root));
  const project = makeProject(dir, "FakeProject");
  seedBuildOutputs(root, project);

  const result = runCleanBuildBat(root);
  const out = outputOf(result);
  assert.equal(result.status, 0, out);
  assert.match(out, /CLEAN SUCCESS/);
  assert.ok(!/FAILED/.test(out), out);

  assert.ok(!existsSync(path.join(root, "build", "cache.json")));
  assert.ok(!existsSync(path.join(root, "generated", "project.json")));
  assert.ok(!existsSync(path.join(root, "logs", "build-temp")));
  // docs/, dist/ and the source project survive a plain clean.
  assert.ok(existsSync(path.join(root, "docs-keep.md")));
  assert.ok(existsSync(path.join(root, "dist", "old-package.bin")));
  assert.ok(existsSync(path.join(root, "logs", "2020-01-01_000000.log")));
  assert.equal(readFileSync(path.join(project, "Data", "Map001.rxdata"), "utf8"), "precious");
  // Output directories are recreated so the next build works.
  assert.ok(existsSync(path.join(root, "build")));
  assert.ok(existsSync(path.join(root, "generated")));
  assert.ok(existsSync(path.join(root, "logs", "latest.log")));
});

test("clean-build.bat --all also removes dist and archived logs", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => drop(root));
  const project = makeProject(dir, "FakeProject");
  seedBuildOutputs(root, project);

  const result = runCleanBuildBat(root, "--all");
  const out = outputOf(result);
  assert.equal(result.status, 0, out);
  assert.match(out, /CLEAN SUCCESS/);
  assert.ok(!existsSync(path.join(root, "dist", "old-package.bin")));
  assert.ok(!existsSync(path.join(root, "logs", "2020-01-01_000000.log")));
  // The current log stays readable, and docs/ is never a clean target.
  assert.ok(existsSync(path.join(root, "docs-keep.md")));
  assert.ok(existsSync(path.join(root, "logs", "latest.log")));
  assert.equal(readFileSync(path.join(project, "Data", "Map001.rxdata"), "utf8"), "precious");
});

test("clean-build.bat never writes into the source project", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => drop(root));
  const project = makeProject(dir, "Read Only Project");
  const before = snapshotProject(project);

  assert.equal(runCleanBuildBat(root).status, 0);
  assert.deepEqual(snapshotProject(project), before);
});

test("clean-build.bat reports an incomplete builder installation", (t) => {
  const root = copyBuilder();
  t.after(() => drop(root));
  rmSync(path.join(root, "builder-config.json"), { force: true });

  const result = runCleanBuildBat(root);
  const out = outputOf(result);
  assert.equal(result.status, 1, out);
  assert.match(out, /\[ERROR\] builder-config\.json not found/);
  // Nothing was removed, because the wrapper stopped before calling the CLI.
  assert.ok(existsSync(path.join(root, "builder", "src", "cli.js")));
});
test("build-android-legacy.bat fails when the project data cannot be parsed", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => drop(root));
  const project = makeProject(dir, "BrokenProject");
  writeFileSync(path.join(project, "Data", "Map001.rxdata"), Buffer.from([4, 8, 0x58]));

  const result = runBuildAndroidLegacyBat(root, project);
  const out = outputOf(result);
  assert.equal(result.status, 1, out);
  assert.match(out, /SCAN FAILED/);
  assert.match(out, /BUILD FAILED/);
  assert.ok(!/DATA BUILD SUCCESS/.test(out), out);
  assert.ok(!/Runtime is not implemented yet\./.test(out), out);
  assert.ok(!existsSync(path.join(root, "generated", "project.json")));
});
test("build-data.bat removes stale intermediate data before writing", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => drop(root));
  const project = makeProject(dir, "FakeProject");
  const stale = path.join(root, "generated", "maps", "map-999.json");
  mkdirSync(path.dirname(stale), { recursive: true });
  writeFileSync(stale, "{}", "utf8");

  const result = runBuildDataBat(root, project);
  assert.equal(result.status, 0, outputOf(result));
  assert.ok(!existsSync(stale));
  assert.ok(existsSync(path.join(root, "generated", "maps", "map-001.json")));
});

test("build-data.bat fails on a missing project path", (t) => {
  const root = copyBuilder();
  t.after(() => drop(root));
  const missing = path.join(os.tmpdir(), "pb-builddata-missing-98765");

  const result = runBuildDataBat(root, missing);
  const out = outputOf(result);
  assert.equal(result.status, 1, out);
  assert.match(out, /VALIDATE FAILED/);
  assert.match(out, /BUILD FAILED/);
  assert.ok(!existsSync(path.join(root, "generated", "project.json")));
});

test("build-data.bat fails when the project data cannot be parsed", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => drop(root));
  const project = makeProject(dir, "BrokenProject");
  writeFileSync(path.join(project, "Data", "Map001.rxdata"), Buffer.from([4, 8, 0x58]));

  const result = runBuildDataBat(root, project);
  const out = outputOf(result);
  assert.equal(result.status, 1, out);
  assert.match(out, /CONVERT FAILED/);
  assert.match(out, /BUILD FAILED/);
  assert.ok(!existsSync(path.join(root, "generated", "project.json")));
});

test("build-data.bat fails when Scripts.rxdata is missing", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => drop(root));
  const project = makeProject(dir, "NoScripts");
  drop(path.join(project, "Data", "Scripts.rxdata"));

  const result = runBuildDataBat(root, project);
  const out = outputOf(result);
  assert.equal(result.status, 1, out);
  assert.match(out, /Scripts\.rxdata/);
  assert.match(out, /BUILD FAILED/);
});

test("scripts wrappers are safe batch files", () => {
  for (const wrapper of WRAPPERS) {
    const bat = readFileSync(path.join(BUILDER_SOURCE, wrapper.file), "utf8");
    // UTF-8 console so Chinese / Japanese project paths print correctly.
    assert.match(bat, /chcp 65001/, wrapper.file);
    // The pipeline lives in the CLI; the wrapper only forwards to it.
    assert.match(bat, new RegExp('call "%BAT%" ' + wrapper.command + " %\\*"), wrapper.file);
    // Exit code must be propagated, never swallowed.
    assert.match(bat, /exit \/b %errorlevel%/, wrapper.file);
    // A wrapper must never delete or move anything.
    assert.ok(!/\b(rd|rmdir|del|erase|move)\b/i.test(bat), wrapper.file);
    // The project path is always passed through quoted (%1 / %*).
    assert.ok(!/\b%~1\b/.test(bat), wrapper.file);
  }

  // test-builder.bat (Phase 12) runs the suite itself instead of delegating
  // to a CLI command, but follows the same safety rules.
  const file = "scripts/test-builder.bat";
  const bat = readFileSync(path.join(BUILDER_SOURCE, file), "utf8");
  assert.match(bat, /chcp 65001/, file);
  assert.match(bat, /node --test "builder\/tests\/\*\.test\.js"/, file);
  // The node:test exit code is propagated, never swallowed.
  assert.match(bat, /exit \/b %STATUS%/, file);
  assert.match(bat, /echo TEST SUCCESS/, file);
  assert.match(bat, /echo TEST FAILED/, file);
  assert.ok(!/\b(rd|rmdir|del|erase|move)\b/i.test(bat), file);
  assert.ok(!/\b%~1\b/.test(bat), file);
});
test("build-data.bat reuses the cache on a second run", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => drop(root));
  const project = makeProject(dir, "FakeProject");

  const first = runBuildDataBat(root, project);
  assert.equal(first.status, 0, outputOf(first));
  assert.ok(existsSync(path.join(root, "build", "cache.json")));

  // Nothing changed: the second run reuses every unit (Phase 13 incremental).
  const second = runBuildDataBat(root, project);
  const out = outputOf(second);
  assert.equal(second.status, 0, out);
  assert.match(out, /incremental build: \d+ units reused, 0 rebuilt/);
  assert.match(out, /BUILD DATA SUCCESS/);
  // The cache is refreshed, never left stale.
  const cache = JSON.parse(readFileSync(path.join(root, "build", "cache.json"), "utf8"));
  assert.equal(cache.project, project);

  // --no-cache forces a full rebuild through the same wrapper.
  const forced = runBuildDataBat(root, project, "--no-cache");
  const forcedOut = outputOf(forced);
  assert.equal(forced.status, 0, forcedOut);
  assert.ok(!/units reused/.test(forcedOut), forcedOut);
});
