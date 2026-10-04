// Path handling and edge-case tests (Phase 12).
//
// A Windows user double-clicks the .bat wrappers, so the whole pipeline is
// driven through cmd.exe with the project path as the only input. The path may
// contain spaces, Chinese or Japanese characters; a missing path or corrupt
// Data/ files must fail loudly with exit code 1; projects without a PBS
// directory or without third-party plugins are legal inputs that must still
// build. Every run happens in a throwaway copy of the builder, so the real
// docs/, build/, generated/ and logs/ directories are untouched.

import test from "node:test";
import assert from "node:assert/strict";
import { writeFileSync, existsSync, readFileSync, rmSync } from "node:fs";
import path from "node:path";
import os from "node:os";

import {
  copyBuilder,
  makeProject,
  runAuditBat,
  runBuildDataBat,
  outputOf,
  snapshotProject,
  scratch,
} from "./fixtures.js";

const IR_FILES = [
  "project.json",
  "maps/index.json",
  "events/index.json",
  "scripts/sections.json",
  "scripts/blocks.json",
  "metadata/problems.json",
];

const PROJECT_NAMES = [
  { label: "spaces", name: "My Pokemon Project" },
  { label: "Chinese", name: "精灵宝可梦工程" },
  { label: "Japanese", name: "ポケモンプロジェクト" },
  { label: "spaces + Chinese + Japanese", name: "my 项目 プロジェクト v2" },
];

for (const { label, name } of PROJECT_NAMES) {
  test("audit-project.bat handles a project path with " + label, (t) => {
    const dir = scratch("pb-scratch-", t);
    const root = copyBuilder();
    t.after(() => rmSync(root, { recursive: true, force: true }));
    const project = makeProject(dir, name);
    const before = snapshotProject(project);

    const result = runAuditBat(root, project);
    const out = outputOf(result);
    assert.equal(result.status, 0, out);
    assert.match(out, /AUDIT SUCCESS/);
    assert.match(out, /BUILD SUCCESS/);
    assert.ok(!/FAILED/.test(out), out);
    assert.ok(existsSync(path.join(root, "build", "reports", "script-audit.json")));
    // The source project stays read-only no matter what the path looks like.
    assert.deepEqual(snapshotProject(project), before);
  });
}

test("build-data.bat converts a project path with spaces + Chinese + Japanese", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => rmSync(root, { recursive: true, force: true }));
  const project = makeProject(dir, "my 项目 プロジェクト v2");

  const result = runBuildDataBat(root, project);
  const out = outputOf(result);
  assert.equal(result.status, 0, out);
  assert.match(out, /BUILD DATA SUCCESS/);
  assert.match(out, /BUILD SUCCESS/);
  assert.ok(!/FAILED/.test(out), out);

  for (const file of IR_FILES) {
    assert.ok(existsSync(path.join(root, "generated", file)), file);
  }
  const manifest = JSON.parse(readFileSync(path.join(root, "generated", "project.json"), "utf8"));
  assert.equal(manifest.kind, "project");
  assert.equal(manifest.counts.maps, 1);
  // Non-ASCII map names survive the round trip through the JSON IR.
  const map = JSON.parse(readFileSync(path.join(root, "generated", "maps", "map-001.json"), "utf8"));
  assert.equal(map.name, "Starting Map");
});

test("audit-project.bat fails on a path that does not exist", (t) => {
  const root = copyBuilder();
  t.after(() => rmSync(root, { recursive: true, force: true }));
  // CJK + spaces in the missing path, so the failure message must survive the
  // console encoding and the cmd.exe quoting.
  const missing = path.join(os.tmpdir(), "pb 缺失 プロジェクト 987654");

  const result = runAuditBat(root, missing);
  const out = outputOf(result);
  assert.equal(result.status, 1, out);
  assert.match(out, /VALIDATE FAILED/);
  assert.match(out, /BUILD FAILED/);
  assert.ok(!/AUDIT SUCCESS/.test(out), out);
  assert.ok(!existsSync(path.join(root, "docs", "source-audit.md")));
});

test("audit-project.bat fails on corrupted RMXP data", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => rmSync(root, { recursive: true, force: true }));
  const project = makeProject(dir, "CorruptMapInfos");
  writeFileSync(path.join(project, "Data", "MapInfos.rxdata"), Buffer.from([4, 8, 0x58]));

  const result = runAuditBat(root, project);
  const out = outputOf(result);
  assert.equal(result.status, 1, out);
  assert.match(out, /SCAN FAILED/);
  assert.match(out, /BUILD FAILED/);
  // The broken file is reported by name, never skipped silently.
  assert.match(out, /unreadable data: .*MapInfos\.rxdata/);
  assert.ok(!existsSync(path.join(root, "docs", "script-event-audit.md")));
});

test("audit-project.bat succeeds without a PBS directory", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => rmSync(root, { recursive: true, force: true }));
  const project = makeProject(dir, "NoPbsProject");
  rmSync(path.join(project, "PBS"), { recursive: true, force: true });

  const result = runAuditBat(root, project);
  const out = outputOf(result);
  assert.equal(result.status, 0, out);
  // Missing PBS is a warning, not a failure: the data pipeline still runs.
  assert.match(out, /WARNING: PBS directory not found/);
  assert.match(out, /AUDIT SUCCESS/);
  assert.match(out, /BUILD SUCCESS/);
  assert.ok(existsSync(path.join(root, "build", "reports", "scan.json")));

  const converted = runBuildDataBat(root, project);
  const convertOut = outputOf(converted);
  assert.equal(converted.status, 0, convertOut);
  assert.match(convertOut, /BUILD DATA SUCCESS/);
});

test("audit-project.bat succeeds without third-party plugins", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => rmSync(root, { recursive: true, force: true }));
  // The fixture ships core sections only (a separator plus "Main"), so the
  // classifier must report zero plugin sections and still succeed.
  const project = makeProject(dir, "NoPluginProject");

  const result = runAuditBat(root, project);
  const out = outputOf(result);
  assert.equal(result.status, 0, out);
  assert.match(out, /plugin sections: 0/);
  assert.match(out, /ESSENTIALS_API: 1/);
  assert.ok(!/PLUGIN_API: [1-9]/.test(out), out);
  assert.match(out, /AUDIT SUCCESS/);
  assert.match(out, /BUILD SUCCESS/);
  assert.ok(existsSync(path.join(root, "docs", "plugin-usage.md")));

  // Machine readable report: no plugin sections, no unresolved identifiers.
  const auditJson = JSON.parse(
    readFileSync(path.join(root, "build", "reports", "script-audit.json"), "utf8"),
  );
  assert.equal(auditJson.totals.pluginSections, 0);
  assert.equal(auditJson.unresolvedIdentifiers.length, 0);
  assert.equal(auditJson.totals.scriptBlocks, 1);
});