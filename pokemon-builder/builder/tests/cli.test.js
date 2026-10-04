// Builder CLI unit tests (Phase 2). Run with: node --test builder/tests
// Phase 12 extends this suite with path/encoding/edge-case coverage.

import test from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, writeFileSync, existsSync, readFileSync, readdirSync, rmSync } from "node:fs";
import path from "node:path";
import os from "node:os";

import {
  runCli,
  EXIT_OK,
  EXIT_ERROR,
} from "../src/commands.js";
import { loadConfig, ConfigError } from "../src/config.js";
import { validateProject } from "../src/project.js";
import { array, dump, hash, int, object, userdef, str, deflateText, nilValue } from "./marshal-writer.js";

const CONFIG_JSON = JSON.stringify(
  {
    projectName: "PokemonGame",
    version: "0.1.0",
    source: { rmxpProject: "" },
    targets: { desktop: true, android: true, androidLegacy: false },
    output: { generated: "generated", dist: "dist", logs: "logs" },
  },
  null,
  2
);

function makeBuilderRoot() {
  const root = mkdtempSync(path.join(os.tmpdir(), "pb-builder-"));
  writeFileSync(path.join(root, "builder-config.json"), CONFIG_JSON, "utf8");
  mkdirSync(path.join(root, "docs"), { recursive: true });
  writeFileSync(path.join(root, "docs", "architecture.md"), "# hand written\n", "utf8");
  return root;
}

// RGSS Table payload: dim0, dim1, dim2, dim0, total + uint16 LE values (same
// layout the converter decodes).
function tableBytes(dim0, dim1, dim2, values) {
  const total = dim0 * dim1 * dim2;
  const bytes = Buffer.alloc(20 + total * 2);
  bytes.writeUInt32LE(dim0, 0);
  bytes.writeUInt32LE(dim1, 4);
  bytes.writeUInt32LE(dim2, 8);
  bytes.writeUInt32LE(dim0, 12);
  bytes.writeUInt32LE(total, 16);
  for (let i = 0; i < total; i++) {
    bytes.writeUInt16LE(values[i % values.length], 20 + i * 2);
  }
  return bytes;
}

function makeProject(name = "FakeProject", withScripts = true) {
  const root = mkdtempSync(path.join(os.tmpdir(), "pb-project-"));
  const project = path.join(root, name);
  mkdirSync(path.join(project, "Data"), { recursive: true });
  writeFileSync(path.join(project, "Game.rxproj"), "", "utf8");
  mkdirSync(path.join(project, "PBS"), { recursive: true });
  if (withScripts) {
    const sections = array(
      array(int(0), str("=================="), str(deflateText(""))),
      array(int(1), str("Main"), str(deflateText("pbMain\n"))),
    );
    writeFileSync(path.join(project, "Data", "Scripts.rxdata"), dump(sections));
  }
  // Event data (Phase 4): map info, one map with one scripted page, common events.
  writeFileSync(
    path.join(project, "Data", "MapInfos.rxdata"),
    dump(hash([int(1), object("RPG::MapInfo", { "@name": str("Starting Map"), "@order": int(1), "@parent_id": int(0) })])),
  );
  writeFileSync(
    path.join(project, "Data", "Map001.rxdata"),
    dump(
      object("RPG::Map", {
        "@tileset_id": int(1),
        "@width": int(17),
        "@height": int(13),
        "@events": hash(
          [
            int(1),
            object("RPG::Event", {
              "@id": int(1),
              "@name": str("Professor"),
              "@pages": array(
                object("RPG::Event::Page", {
                  "@trigger": int(0),
                  "@list": array(
                    object("RPG::EventCommand", { "@code": int(355), "@indent": int(0), "@parameters": array(str("pbMain")) }),
                  ),
                }),
              ),
            }),
          ],
        ),
      }),
    ),
  );
  writeFileSync(path.join(project, "Data", "CommonEvents.rxdata"), dump(array(nilValue())));
  // Stage 2 runtime data (project3 section 9): tileset tables + System.
  writeFileSync(
    path.join(project, "Data", "Tilesets.rxdata"),
    dump(
      array(
        nilValue(),
        object("RPG::Tileset", {
          "@id": int(1),
          "@name": str("Fake Tiles"),
          "@tileset_name": str("Fake Tileset"),
          "@autotile_names": array(str(""), str(""), str(""), str(""), str(""), str(""), str("")),
          "@panorama_name": str(""),
          "@panorama_hue": int(0),
          "@fog_name": str(""),
          "@fog_hue": int(0),
          "@fog_opacity": int(64),
          "@fog_blend_type": int(0),
          "@fog_zoom": int(200),
          "@fog_sx": int(0),
          "@fog_sy": int(0),
          "@battleback_name": str(""),
          "@passages": userdef("Table", tableBytes(1, 6624, 1, [0, 0, 0x0f, 0x0f, 0x0f, 0x0f])),
          "@priorities": userdef("Table", tableBytes(1, 6624, 1, [0, 1, 1, 1, 1, 1])),
          "@terrain_tags": userdef("Table", tableBytes(1, 6624, 1, [0, 1, 1, 1, 1, 1])),
        }),
      ),
    ),
  );
  writeFileSync(
    path.join(project, "Data", "System.rxdata"),
    dump(
      object("RPG::System", {
        "@magic_number": int(16522614),
        "@start_map_id": int(1),
        "@start_x": int(9),
        "@start_y": int(7),
        "@edit_map_id": int(1),
        "@windowskin_name": str(""),
        "@title_name": str(""),
        "@gameover_name": str(""),
        "@battleback_name": str(""),
        "@elements": array(str(""), str("")),
        "@switches": array(str(""), str("SW1"), str("SW2")),
        "@variables": array(str(""), str("VAR1")),
        "@words": object("RPG::System::Words", { "@hp": str("HP"), "@attack": str("ATK") }),
      }),
    ),
  );
  return project;
}

function capture(fn) {
  const original = { log: console.log, error: console.error, warn: console.warn };
  const out = [];
  console.log = (...args) => out.push(args.join(" "));
  console.error = (...args) => out.push(args.join(" "));
  console.warn = (...args) => out.push(args.join(" "));
  try {
    const code = fn();
    return { code, out: out.join("\n") };
  } finally {
    Object.assign(console, original);
  }
}

test("help prints usage and succeeds", () => {
  const root = makeBuilderRoot();
  const { code, out } = capture(() => runCli(root, ["help"]));
  assert.equal(code, EXIT_OK);
  assert.match(out, /Usage:/);
  assert.match(out, /audit <project>/);
});

test("no arguments prints usage", () => {
  const root = makeBuilderRoot();
  const { code } = capture(() => runCli(root, []));
  assert.equal(code, EXIT_OK);
});

test("unknown command fails", () => {
  const root = makeBuilderRoot();
  const { code } = capture(() => runCli(root, ["frobnicate"]));
  assert.equal(code, EXIT_ERROR);
});

test("missing builder-config.json fails", () => {
  const root = mkdtempSync(path.join(os.tmpdir(), "pb-builder-"));
  const { code } = capture(() => runCli(root, ["validate", makeProject()]));
  assert.equal(code, EXIT_ERROR);
});

test("validate succeeds on a well-formed project", () => {
  const root = makeBuilderRoot();
  const project = makeProject();
  const { code, out } = capture(() => runCli(root, ["validate", project]));
  assert.equal(code, EXIT_OK);
  assert.match(out, /VALIDATE OK/);
});

test("validate handles project paths with spaces and CJK characters", () => {
  const root = makeBuilderRoot();
  const project = makeProject("my 项目 プロジェクト");
  const { code, out } = capture(() => runCli(root, ["validate", project]));
  assert.equal(code, EXIT_OK);
  assert.match(out, /VALIDATE OK/);
});

test("validate fails on a nonexistent path", () => {
  const root = makeBuilderRoot();
  const missing = path.join(os.tmpdir(), "pb-does-not-exist-12345");
  const { code } = capture(() => runCli(root, ["validate", missing]));
  assert.equal(code, EXIT_ERROR);
});

test("validate fails when Data directory is missing", () => {
  const root = makeBuilderRoot();
  const project = mkdtempSync(path.join(os.tmpdir(), "pb-empty-"));
  const { code } = capture(() => runCli(root, ["validate", project]));
  assert.equal(code, EXIT_ERROR);
});

test("audit runs the whole pipeline and writes reports", () => {
  const root = makeBuilderRoot();
  const project = makeProject();
  const { code, out } = capture(() => runCli(root, ["audit", project]));
  assert.equal(code, EXIT_OK, out);
  assert.match(out, /\[1\/5\] Validating project/);
  assert.match(out, /\[5\/5\] Writing audit reports/);
  assert.match(out, /SCAN OK/);
  assert.match(out, /EVENT ANALYSIS OK/);
  assert.match(out, /SCRIPT ANALYSIS OK/);
  assert.match(out, /REPORTS OK/);
  assert.match(out, /AUDIT SUCCESS/);
  assert.match(out, /BUILD SUCCESS/);
  assert.match(out, /6\/6 .rxdata files parsed/);
  assert.match(out, /2\/2 sections readable/);
  assert.match(out, /script blocks: 1/);
  assert.ok(existsSync(path.join(root, "build", "reports", "scan.json")));
  assert.ok(existsSync(path.join(root, "build", "reports", "events.json")));
  assert.ok(existsSync(path.join(root, "build", "reports", "script-audit.json")));
  for (const report of [
    "source-audit.md",
    "script-event-audit.md",
    "api-usage.md",
    "plugin-usage.md",
    "unsupported-scripts.md",
  ]) {
    assert.ok(existsSync(path.join(root, "docs", report)), report);
    assert.ok(readFileSync(path.join(root, "docs", report), "utf8").length > 50, report);
  }
  // architecture.md is hand written and must never be regenerated.
  assert.equal(readFileSync(path.join(root, "docs", "architecture.md"), "utf8"), "# hand written\n");
});

test("audit fails when the project data cannot be read", () => {
  const root = makeBuilderRoot();
  const project = makeProject();
  writeFileSync(path.join(project, "Data", "Map001.rxdata"), Buffer.from([4, 8, 0x58]));
  const { code, out } = capture(() => runCli(root, ["audit", project]));
  assert.equal(code, EXIT_ERROR);
  assert.match(out, /SCAN FAILED/);
  assert.match(out, /BUILD FAILED/);
});

test("audit fails when Scripts.rxdata is missing", () => {
  const root = makeBuilderRoot();
  const project = makeProject("NoScripts", false);
  const { code, out } = capture(() => runCli(root, ["audit", project]));
  assert.equal(code, EXIT_ERROR);
  assert.match(out, /Scripts\.rxdata/);
});

test("build-pc fails honestly when the Gradle wrapper is missing", () => {
  const root = makeBuilderRoot();
  const project = makeProject();
  const { code, out } = capture(() => runCli(root, ["build-pc", project]));
  assert.equal(code, EXIT_ERROR, out);
  assert.match(out, /\[7\/8\] Runtime tests/);
  assert.match(out, /Gradle wrapper not found/);
  assert.match(out, /RUNTIME TESTS FAILED/);
  assert.match(out, /BUILD FAILED/);
  assert.ok(!/not implemented/i.test(out), out);
});

test("build-pc runs the full 8 step pipeline and packages with a stubbed Gradle", () => {
  const root = makeBuilderRoot();
  const project = makeProject();
  const gradleRunner = (builderRoot, tasks) => {
    if (tasks.includes("lwjgl3:dist")) {
      mkdirSync(path.join(builderRoot, "dist", "Windows"), { recursive: true });
      writeFileSync(path.join(builderRoot, "dist", "PokemonGame.jar"), "jar", "utf8");
      writeFileSync(path.join(builderRoot, "dist", "Windows", "PokemonGame.exe"), "exe", "utf8");
    }
    return { ok: true, exitCode: 0, output: "stub gradle: " + tasks.join(" ") };
  };
  const { code, out } = capture(() => runCli(root, ["build-pc", project], { gradleRunner }));
  assert.equal(code, EXIT_OK, out);
  for (const label of ["[1/8]", "[2/8]", "[3/8]", "[4/8]", "[5/8]", "[6/8]", "[7/8]", "[8/8]"]) {
    assert.ok(out.includes(label), label + " missing in output:\n" + out);
  }
  assert.match(out, /AUDIT SUCCESS/);
  assert.match(out, /DATA BUILD SUCCESS/);
  assert.match(out, /PC BUILD SUCCESS/);
  assert.match(out, /Build report:/);
  // The data stages really produced their artifacts.
  assert.ok(existsSync(path.join(root, "generated", "project.json")));
  assert.ok(existsSync(path.join(root, "generated", "maps", "map-001.json")));
  assert.ok(existsSync(path.join(root, "docs", "source-audit.md")));
  // The package step wrote the report with the section 55 fields.
  const report = JSON.parse(readFileSync(path.join(root, "generated", "build-report.json"), "utf8"));
  assert.equal(report.status, "success");
  assert.equal(report.buildTarget, "desktop");
  assert.ok(report.mapsCompiled >= 1);
  assert.ok(report.gradleResult.ok);
  assert.equal(report.gradleResult.tasks.includes("lwjgl3:dist"), true);
  assert.equal(report.outputArtifact.length, 2);
});

test("build-pc reports a missing artifact instead of claiming success", () => {
  const root = makeBuilderRoot();
  const project = makeProject();
  // Gradle "succeeds" but produced nothing: the package step must fail.
  const gradleRunner = () => ({ ok: true, exitCode: 0, output: "" });
  const { code, out } = capture(() => runCli(root, ["build-pc", project], { gradleRunner }));
  assert.equal(code, EXIT_ERROR, out);
  assert.match(out, /missing build artifact/);
  assert.match(out, /PACKAGE FAILED/);
  assert.ok(!/PC BUILD SUCCESS/.test(out), out);
});

test("build-pc fails when the project data cannot be read", () => {
  const root = makeBuilderRoot();
  const project = makeProject();
  writeFileSync(path.join(project, "Data", "Map001.rxdata"), Buffer.from([4, 8, 0x58]));
  const { code, out } = capture(() => runCli(root, ["build-pc", project]));
  assert.equal(code, EXIT_ERROR, out);
  assert.match(out, /SCAN FAILED/);
  assert.match(out, /BUILD FAILED/);
  assert.ok(!/DATA BUILD SUCCESS/.test(out), out);
  assert.ok(!/PC Runtime is not implemented yet\./.test(out), out);
  assert.ok(!existsSync(path.join(root, "generated", "project.json")));
});

test("build-pc fails on a nonexistent project path", () => {
  const root = makeBuilderRoot();
  const missing = path.join(os.tmpdir(), "pb-buildpc-missing-98765");
  const { code, out } = capture(() => runCli(root, ["build-pc", missing]));
  assert.equal(code, EXIT_ERROR, out);
  assert.match(out, /VALIDATE FAILED/);
  assert.match(out, /BUILD FAILED/);
  assert.ok(!/PC Runtime is not implemented yet\./.test(out), out);
});

test("build-android runs the full pipeline and packages the debug APK with a stubbed Gradle", () => {
  const root = makeBuilderRoot();
  const project = makeProject();
  let seenTasks = [];
  const gradleRunner = (builderRoot, tasks) => {
    seenTasks = tasks;
    const module = tasks[0].split(":")[0];
    const apkDir = path.join(builderRoot, "runtime", module, "build", "outputs", "apk", "debug");
    mkdirSync(apkDir, { recursive: true });
    writeFileSync(path.join(apkDir, module + "-debug.apk"), "apk", "utf8");
    return { ok: true, exitCode: 0, output: "stub gradle: " + tasks.join(" ") };
  };
  const { code, out } = capture(() => runCli(root, ["build-android", project], { gradleRunner }));
  assert.equal(code, EXIT_OK, out);
  assert.deepEqual(seenTasks, ["android:assembleDebug"]);
  for (const label of ["[1/8]", "[2/8]", "[4/8]", "[5/8]", "[6/8]", "[7/8]", "[8/8]"]) {
    assert.ok(out.includes(label), label + " missing in output:\n" + out);
  }
  assert.match(out, /AUDIT SUCCESS/);
  assert.match(out, /DATA BUILD SUCCESS/);
  assert.match(out, /ANDROID BUILD SUCCESS/);
  assert.match(out, /TECHNICAL PREVIEW/);
  assert.ok(existsSync(path.join(root, "dist", "PokemonGame-Android.apk")));
  const report = JSON.parse(readFileSync(path.join(root, "generated", "build-report.json"), "utf8"));
  assert.equal(report.buildTarget, "android");
  assert.equal(report.technicalPreview, true);
  assert.deepEqual(report.outputArtifact, [
    path.join(root, "dist", "PokemonGame-Android.apk"),
    path.join(root, "dist", "PokemonGame-Android-data.zip"),
  ]);
});

test("build-android --no-data keeps the slim APK (no data pack staged) (L3)", () => {
  const root = makeBuilderRoot();
  const project = makeProject();
  const gradleRunner = (builderRoot, tasks) => {
    const module = tasks[0].split(":")[0];
    const apkDir = path.join(builderRoot, "runtime", module, "build", "outputs", "apk", "debug");
    mkdirSync(apkDir, { recursive: true });
    writeFileSync(path.join(apkDir, module + "-debug.apk"), "apk", "utf8");
    return { ok: true, exitCode: 0, output: "" };
  };
  const { code, out } = capture(() => runCli(root, ["build-android", project, "--no-data"], { gradleRunner }));
  assert.equal(code, EXIT_OK, out);
  assert.ok(!existsSync(path.join(root, "dist", "PokemonGame-Android-data.zip")), out);
  assert.ok(!existsSync(path.join(root, "runtime", "android", "build", "android-assets")), out);
});

test("build-android-legacy uses its own Gradle module and artifact", () => {
  const root = makeBuilderRoot();
  const project = makeProject();
  let seenTasks = [];
  const gradleRunner = (builderRoot, tasks) => {
    seenTasks = tasks;
    const apkDir = path.join(builderRoot, "runtime", "android-legacy", "build", "outputs", "apk", "debug");
    mkdirSync(apkDir, { recursive: true });
    writeFileSync(path.join(apkDir, "android-legacy-debug.apk"), "apk", "utf8");
    return { ok: true, exitCode: 0, output: "" };
  };
  const { code, out } = capture(() => runCli(root, ["build-android-legacy", project], { gradleRunner }));
  assert.equal(code, EXIT_OK, out);
  assert.deepEqual(seenTasks, ["android-legacy:assembleDebug"]);
  assert.match(out, /Legacy Android/);
  assert.match(out, /:android-legacy:assembleDebug/);
  // Modern and legacy never share the packaged artifact name.
  assert.ok(existsSync(path.join(root, "dist", "PokemonGame-Android-Legacy.apk")));
  assert.ok(!existsSync(path.join(root, "dist", "PokemonGame-Android.apk")));
  const report = JSON.parse(readFileSync(path.join(root, "generated", "build-report.json"), "utf8"));
  assert.equal(report.buildTarget, "androidLegacy");
});

test("build-android fails when the project data cannot be read", () => {
  const root = makeBuilderRoot();
  const project = makeProject();
  writeFileSync(path.join(project, "Data", "Map001.rxdata"), Buffer.from([4, 8, 0x58]));
  const { code, out } = capture(() => runCli(root, ["build-android", project]));
  assert.equal(code, EXIT_ERROR, out);
  assert.match(out, /SCAN FAILED/);
  assert.match(out, /BUILD FAILED/);
  assert.ok(!/DATA BUILD SUCCESS/.test(out), out);
  assert.ok(!/Runtime is not implemented yet\./.test(out), out);
  assert.ok(!existsSync(path.join(root, "generated", "project.json")));
});

test("build-android fails on a nonexistent project path", () => {
  const root = makeBuilderRoot();
  const missing = path.join(os.tmpdir(), "pb-buildandroid-missing-98765");
  const { code, out } = capture(() => runCli(root, ["build-android", missing]));
  assert.equal(code, EXIT_ERROR, out);
  assert.match(out, /VALIDATE FAILED/);
  assert.match(out, /BUILD FAILED/);
  assert.ok(!/Runtime is not implemented yet\./.test(out), out);
});

test("clean removes build outputs but keeps docs, dist and the source project", () => {
  const root = makeBuilderRoot();
  const project = makeProject();
  for (const dir of ["dist", "build", "generated", "logs"]) {
    mkdirSync(path.join(root, dir), { recursive: true });
  }
  writeFileSync(path.join(root, "docs-keep.md"), "keep", "utf8");
  writeFileSync(path.join(root, "dist", "old-package.bin"), "x", "utf8");
  writeFileSync(path.join(root, "build", "cache.json"), "x", "utf8");
  writeFileSync(path.join(root, "generated", "project.json"), "x", "utf8");
  mkdirSync(path.join(root, "logs", "build-temp"), { recursive: true });
  writeFileSync(path.join(root, "logs", "build-temp", "tmp.bin"), "x", "utf8");
  writeFileSync(path.join(project, "Data", "Map001.rxdata"), "precious", "utf8");

  const { code } = capture(() => runCli(root, ["clean"]));
  assert.equal(code, EXIT_OK);
  assert.ok(existsSync(path.join(root, "build")));
  assert.ok(!existsSync(path.join(root, "build", "cache.json")));
  assert.ok(!existsSync(path.join(root, "generated", "project.json")));
  assert.ok(!existsSync(path.join(root, "logs", "build-temp")));
  assert.ok(existsSync(path.join(root, "dist", "old-package.bin")));
  assert.ok(existsSync(path.join(root, "docs-keep.md")));
  assert.ok(existsSync(path.join(project, "Data", "Map001.rxdata")));
});

test("clean --all also removes dist and old logs", () => {
  const root = makeBuilderRoot();
  for (const dir of ["dist", "logs"]) {
    mkdirSync(path.join(root, dir), { recursive: true });
  }
  writeFileSync(path.join(root, "dist", "old-package.bin"), "x", "utf8");
  writeFileSync(path.join(root, "logs", "2020-01-01_000000.log"), "old", "utf8");
  const { code } = capture(() => runCli(root, ["clean", "--all"]));
  assert.equal(code, EXIT_OK);
  assert.ok(!existsSync(path.join(root, "dist", "old-package.bin")));
  assert.ok(!existsSync(path.join(root, "logs", "2020-01-01_000000.log")));
  assert.ok(existsSync(path.join(root, "logs", "latest.log")));
});

test("each run writes latest.log and a timestamped log", () => {
  const root = makeBuilderRoot();
  capture(() => runCli(root, ["validate", makeProject()]));
  const logs = readdirSync(path.join(root, "logs"));
  assert.ok(logs.includes("latest.log"));
  assert.ok(logs.some((name) => /^\d{4}-\d{2}-\d{2}_\d{6}\.log$/.test(name)));
});

test("loadConfig merges overrides and rejects invalid JSON", () => {
  const root = makeBuilderRoot();
  const config = loadConfig(root, { source: { rmxpProject: "D:\\Game\\X" } });
  assert.equal(config.source.rmxpProject, "D:\\Game\\X");
  assert.equal(config.projectName, "PokemonGame");
  assert.equal(config.targets.desktop, true);

  const broken = mkdtempSync(path.join(os.tmpdir(), "pb-broken-"));
  writeFileSync(path.join(broken, "builder-config.json"), "{ not json", "utf8");
  assert.throws(() => loadConfig(broken), ConfigError);
});

test("validateProject reports a missing Data directory", () => {
  const project = mkdtempSync(path.join(os.tmpdir(), "pb-nodata-"));
  const result = validateProject(project);
  assert.equal(result.ok, false);
  assert.ok(result.errors.some((error) => error.includes("Data")));
});
test("build-data converts the project and writes the JSON Debug IR", () => {
  const root = makeBuilderRoot();
  const project = makeProject();
  const { code, out } = capture(() => runCli(root, ["build-data", project]));
  assert.equal(code, EXIT_OK, out);
  assert.match(out, /\[1\/4\] Validating project/);
  assert.match(out, /\[2\/4\] Scanning RMXP data, events and Ruby scripts/);
  assert.match(out, /\[3\/4\] Converting to intermediate data/);
  assert.match(out, /\[4\/4\] Compiling Essentials script blocks/);
  assert.match(out, /BUILD DATA SUCCESS/);
  assert.match(out, /BUILD SUCCESS/);
  assert.ok(existsSync(path.join(root, "generated", "project.json")));
  assert.ok(existsSync(path.join(root, "generated", "maps", "map-001.json")));
  assert.ok(existsSync(path.join(root, "generated", "events", "map-001.json")));
  assert.ok(existsSync(path.join(root, "generated", "scripts", "blocks.json")));
  assert.ok(existsSync(path.join(root, "generated", "metadata", "build.json")));
  const latest = readFileSync(path.join(root, "logs", "latest.log"), "utf8");
  assert.match(latest, /Command: build-data/);
  assert.match(latest, /exit=0/);
});

test("build-data fails when the project data cannot be read", () => {
  const root = makeBuilderRoot();
  const project = makeProject();
  writeFileSync(path.join(project, "Data", "Map001.rxdata"), Buffer.from([4, 8, 0x58]));
  const { code, out } = capture(() => runCli(root, ["build-data", project]));
  assert.equal(code, EXIT_ERROR);
  assert.match(out, /CONVERT FAILED/);
  assert.match(out, /BUILD FAILED/);
  assert.ok(!existsSync(path.join(root, "generated", "project.json")));
});
// ---------------------------------------------------------------------------
// Phase 13: incremental cache, log outputs and hard failure paths.
// ---------------------------------------------------------------------------

test("build-data reuses cached units on the second run", () => {
  const root = makeBuilderRoot();
  const project = makeProject();

  const first = capture(() => runCli(root, ["build-data", project]));
  assert.equal(first.code, EXIT_OK, first.out);
  assert.match(first.out, /BUILD DATA SUCCESS/);

  const cache = JSON.parse(readFileSync(path.join(root, "build", "cache.json"), "utf8"));
  assert.equal(cache.format, "pokemon-builder/build-cache/1");
  assert.equal(cache.project, project);
  assert.ok(cache.units["map:1"]);

  // The log records every output file plus the cache itself.
  const latest = readFileSync(path.join(root, "logs", "latest.log"), "utf8");
  assert.match(latest, /\[OUTPUT\] .*maps[\\/]map-001\.json/);
  assert.match(latest, /\[OUTPUT\] .*build[\\/]cache\.json/);
  assert.match(latest, /Command: build-data/);
  assert.match(latest, /exit=0 duration=\d+ms/);

  // Nothing changed: the second run reuses every unit instead of rewriting.
  const second = capture(() => runCli(root, ["build-data", project]));
  assert.equal(second.code, EXIT_OK, second.out);
  assert.match(second.out, /incremental build: \d+ units reused, 0 rebuilt/);
  assert.match(second.out, /BUILD DATA SUCCESS/);
});

test("build-data --no-cache forces a full rebuild", () => {
  const root = makeBuilderRoot();
  const project = makeProject();

  assert.equal(capture(() => runCli(root, ["build-data", project])).code, EXIT_OK);
  const forced = capture(() => runCli(root, ["build-data", project, "--no-cache"]));
  assert.equal(forced.code, EXIT_OK, forced.out);
  assert.ok(!/units reused/.test(forced.out), forced.out);
  assert.match(forced.out, /BUILD DATA SUCCESS/);

  // The cache was refreshed, so the next default run is incremental again.
  const next = capture(() => runCli(root, ["build-data", project]));
  assert.equal(next.code, EXIT_OK, next.out);
  assert.match(next.out, /units reused, 0 rebuilt/);
});

test("build-data fails when the output directory is not writable", () => {
  const root = makeBuilderRoot();
  const project = makeProject();
  // A file where generated/ belongs: the run must fail loudly AND log why.
  rmSync(path.join(root, "generated"), { recursive: true, force: true });
  writeFileSync(path.join(root, "generated"), "not a directory", "utf8");

  const { code, out } = capture(() => runCli(root, ["build-data", project]));
  assert.equal(code, EXIT_ERROR);
  assert.match(out, /BUILD FAILED/);
  assert.ok(!/BUILD DATA SUCCESS/.test(out), out);
  // Hard rule 6: the failure is recorded in the log, not only on screen.
  const latest = readFileSync(path.join(root, "logs", "latest.log"), "utf8");
  assert.match(latest, /\[ERROR\] builder setup failed/);
  assert.match(latest, /generated/);
  assert.match(latest, /exit=1 duration=\d+ms/);
});

test("build-data fails when the build cache cannot be written", () => {
  const root = makeBuilderRoot();
  const project = makeProject();
  // A directory where build/cache.json belongs: no silent cache loss.
  mkdirSync(path.join(root, "build", "cache.json"), { recursive: true });

  const { code, out } = capture(() => runCli(root, ["build-data", project]));
  assert.equal(code, EXIT_ERROR);
  assert.match(out, /CONVERT FAILED/);
  assert.match(out, /BUILD FAILED/);
});

test("audit fails when the docs directory is not writable", () => {
  const root = makeBuilderRoot();
  const project = makeProject();
  rmSync(path.join(root, "docs"), { recursive: true, force: true });
  writeFileSync(path.join(root, "docs"), "not a directory", "utf8");

  const { code, out } = capture(() => runCli(root, ["audit", project]));
  assert.equal(code, EXIT_ERROR);
  assert.match(out, /REPORTS FAILED/);
  assert.match(out, /BUILD FAILED/);
  const latest = readFileSync(path.join(root, "logs", "latest.log"), "utf8");
  assert.match(latest, /cannot write audit reports/);
});

test("build-data fails when the PBS directory cannot be listed", () => {
  const root = makeBuilderRoot();
  const project = makeProject();
  rmSync(path.join(project, "PBS"), { recursive: true, force: true });
  writeFileSync(path.join(project, "PBS"), "not a directory", "utf8");

  const { code, out } = capture(() => runCli(root, ["build-data", project]));
  assert.equal(code, EXIT_ERROR);
  assert.match(out, /CONVERT FAILED/);
  assert.match(out, /PBS/);
  assert.match(out, /BUILD FAILED/);
});
