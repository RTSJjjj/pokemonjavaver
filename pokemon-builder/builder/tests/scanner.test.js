// Scanner tests (Phase 3). Covers .rxdata parsing, Scripts.rxdata
// inflation, failure reporting and path handling (spaces / CJK).

import test from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, writeFileSync, rmSync, existsSync } from "node:fs";
import path from "node:path";
import os from "node:os";

import { scanProject, classifyDataFile, readScriptsSection, formatScanSummary } from "../../tools/scanner/index.js";
import { array, dump, hash, int, object, str, deflateText } from "./marshal-writer.js";
import { fileURLToPath } from "node:url";
const REFERENCE_PROJECT = process.env.RMXP_PROJECT || fileURLToPath(new URL("../../../", import.meta.url));

const utf8 = (text) => Buffer.from(text, "utf8").toString("latin1");

function makeProject(name = "ScanProject") {
  const root = mkdtempSync(path.join(os.tmpdir(), "pb-scan-"));
  const project = path.join(root, name);
  mkdirSync(path.join(project, "Data"), { recursive: true });
  return project;
}

function writeMap(project, id, options = {}) {
  const pairs = [];
  for (let i = 1; i <= (options.events || 1); i++) {
    pairs.push([
      int(i),
      object("RPG::Event", {
        "@id": int(i),
        "@name": str("Event" + i),
        "@x": int(0),
        "@y": int(0),
        "@pages": array(
          object("RPG::Event::Page", {
            "@move_type": int(0),
            "@list": array(
              object("RPG::EventCommand", { "@parameters": array(), "@indent": int(0), "@code": int(401) }),
            ),
          }),
        ),
      }),
    ]);
  }
  const map = object("RPG::Map", {
    "@tileset_id": int(1),
    "@width": int(17),
    "@height": int(13),
    "@events": hash(...pairs),
  });
  writeFileSync(path.join(project, "Data", "Map" + String(id).padStart(3, "0") + ".rxdata"), dump(map));
}

function writeScripts(project, sections) {
  const payload = sections.map(([id, name, source]) => array(int(id), str(name), str(deflateText(source))));
  writeFileSync(path.join(project, "Data", "Scripts.rxdata"), dump(array(...payload)));
}

test("classifies RGSS data files", () => {
  assert.equal(classifyDataFile("Map001.rxdata"), "map");
  assert.equal(classifyDataFile("Map123.rxdata"), "map");
  assert.equal(classifyDataFile("MapInfos.rxdata"), "mapInfos");
  assert.equal(classifyDataFile("CommonEvents.rxdata"), "commonEvents");
  assert.equal(classifyDataFile("System.rxdata"), "system");
  assert.equal(classifyDataFile("Tilesets.rxdata"), "tilesets");
  assert.equal(classifyDataFile("Actors.rxdata"), "database");
});

test("scans maps, common events, scripts and reports counts", () => {
  const project = makeProject();
  writeMap(project, 1, { events: 2 });
  writeMap(project, 2, { events: 1 });
  writeFileSync(
    path.join(project, "Data", "MapInfos.rxdata"),
    dump(array(array(int(1), object("RPG::MapInfo", { "@name": str("Town Map") })))),
  );
  writeFileSync(
    path.join(project, "Data", "CommonEvents.rxdata"),
    dump(array(
      object("RPG::CommonEvent", {
        "@name": str("Heal"),
        "@list": array(object("RPG::EventCommand", { "@parameters": array(str("pbHealParty")), "@indent": int(0), "@code": int(355) })),
      }),
    )),
  );
  writeScripts(project, [
    [0, "==================", ""],
    [1, "Trainer Battle", "def pbTrainerBattle\n  return true\nend\n"],
    [2, utf8("中文脚本"), "# コメント\npbItemBall(:POTION)\n"],
  ]);

  const scan = scanProject(project);
  assert.equal(scan.ok, true, JSON.stringify(scan.errors));
  assert.equal(scan.data.parsedCount, 5);
  assert.equal(scan.data.failedCount, 0);
  assert.equal(scan.data.kindCounts.map, 2);
  assert.equal(scan.data.kindCounts.mapInfos, 1);
  assert.equal(scan.data.kindCounts.commonEvents, 1);
  assert.equal(scan.data.kindCounts.scripts, 1);

  const mapFile = scan.data.files.find((file) => file.name === "Map001.rxdata");
  assert.equal(mapFile.summary.width, 17);
  assert.equal(mapFile.summary.height, 13);
  assert.equal(mapFile.summary.events, 2);
  assert.equal(mapFile.sha1.length > 0, true);

  const infos = scan.data.files.find((file) => file.name === "MapInfos.rxdata");
  assert.equal(infos.summary.maps[0].name, "Town Map");

  assert.equal(scan.scripts.totalSections, 3);
  assert.equal(scan.scripts.readableSections, 3);
  assert.equal(scan.scripts.problems.length, 0);
  assert.deepEqual(scan.scripts.sections.map((section) => section.name), ["==================", "Trainer Battle", "中文脚本"]);
  assert.ok(scan.scripts.totalLines >= 5);
  assert.ok(formatScanSummary(scan).some((line) => line.includes("3/3 sections readable")));
});

test("reports script sections that cannot be inflated instead of dropping them", () => {
  const project = makeProject();
  writeScripts(project, [[1, "Good", "pbOkay\n"]]);
  // Corrupt the payload of a manual section: not a zlib stream and not text.
  // One section cannot be inflated (not zlib, not readable text).
  writeFileSync(
    path.join(project, "Data", "Scripts.rxdata"),
    dump(array(
      array(int(1), str("Good"), str(deflateText("pbOkay\n"))),
      array(int(2), str("Broken"), str("\u0000\u0001\u0002binary\u0000garbage")),
    )),
  );
  const scan = scanProject(project);
  assert.equal(scan.ok, false);
  assert.equal(scan.scripts.problems.length, 1);
  const problem = scan.scripts.problems[0];
  assert.equal(problem.index, 1);
  assert.equal(problem.id, 2);
  assert.equal(problem.name, "Broken");
  assert.match(problem.reason, /neither zlib nor readable text/);
});

test("reports corrupted .rxdata files with their location", () => {
  const project = makeProject();
  writeMap(project, 1);
  writeFileSync(path.join(project, "Data", "Map087.rxdata"), Buffer.from([4, 8, 0x5b, 0x0a, 0x69]));
  const scan = scanProject(project);
  assert.equal(scan.ok, false);
  assert.equal(scan.data.failedCount, 1);
  const failure = scan.data.files.find((file) => file.name === "Map087.rxdata");
  assert.equal(failure.ok, false);
  assert.ok(failure.error.length > 0);
  assert.ok(scan.errors.some((error) => error.file === "Map087.rxdata"));
  assert.ok(formatScanSummary(scan).some((line) => line.includes("Map087.rxdata")));
});

test("fails when the Data directory is missing", () => {
  const project = mkdtempSync(path.join(os.tmpdir(), "pb-nodata-"));
  const scan = scanProject(project);
  assert.equal(scan.ok, false);
  assert.ok(scan.errors.some((error) => error.file.includes("Data")));
});

test("fails when Scripts.rxdata is missing", () => {
  const project = makeProject();
  writeMap(project, 1);
  const scan = scanProject(project);
  assert.equal(scan.ok, false);
  assert.ok(scan.errors.some((error) => error.file === "Scripts.rxdata"));
});

test("handles project paths with spaces, Chinese and Japanese characters", () => {
  const project = makeProject("我的 项目 プロジェクト");
  writeMap(project, 1);
  writeScripts(project, [[1, "Main", "pbMain\n"]]);
  const scan = scanProject(project);
  assert.equal(scan.ok, true, JSON.stringify(scan.errors));
  assert.equal(scan.data.parsedCount, 2);
});

test("readScriptsSection marks uncompressed but readable payloads", () => {
  const project = makeProject();
  writeFileSync(
    path.join(project, "Data", "Scripts.rxdata"),
    dump(array(array(int(1), str("Plain"), str("# plain ruby\npbOkay\n")))),
  );
  const scripts = readScriptsSection(project);
  assert.equal(scripts.errors.length, 0);
  assert.equal(scripts.readableSections, 1);
  assert.equal(scripts.sections[0].mode, "plain");
  assert.equal(scripts.sections[0].lines, 2);
});

test("scans the real reference project when it is available", (t) => {
  const candidate = path.join(REFERENCE_PROJECT, "Data", "Map001.rxdata");
  if (!existsSync(candidate)) {
    t.skip("reference project not available in this environment");
    return;
  }
  const scan = scanProject(path.dirname(path.dirname(candidate)));
  assert.equal(scan.ok, true, JSON.stringify(scan.errors));
  assert.ok(scan.data.parsedCount > 500);
  assert.ok(scan.scripts.totalSections > 300);
  assert.equal(scan.scripts.problems.length, 0);
  assert.ok(scan.data.files.some((file) => file.kind === "map" && file.summary.width > 0));
});
