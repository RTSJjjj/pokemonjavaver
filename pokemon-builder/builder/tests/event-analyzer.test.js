// Event analyzer tests (Phase 4). Covers 355/655 merging, location fields,
// map name lookup (Hash and Array forms of MapInfos), problem reporting,
// common events, failure paths and path handling (spaces / CJK).

import test from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, writeFileSync, existsSync } from "node:fs";
import { execFileSync } from "node:child_process";
import path from "node:path";
import os from "node:os";
import { fileURLToPath } from "node:url";

import { analyzeProjectEvents, formatEventSummary } from "../../tools/event-analyzer/index.js";
import { array, dump, hash, int, nilValue, object, str, deflateText } from "./marshal-writer.js";
const REFERENCE_PROJECT = process.env.RMXP_PROJECT || fileURLToPath(new URL("../../../", import.meta.url));

const utf8 = (text) => Buffer.from(text, "utf8").toString("latin1");
const analyzerDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..", "tools", "event-analyzer");

function makeProject(name = "EventProject") {
  const root = mkdtempSync(path.join(os.tmpdir(), "pb-events-"));
  const project = path.join(root, name);
  mkdirSync(path.join(project, "Data"), { recursive: true });
  return project;
}

const command = (code, ...parameters) =>
  object("RPG::EventCommand", { "@code": int(code), "@indent": int(0), "@parameters": array(...parameters) });

function event(id, name, pages) {
  return object("RPG::Event", {
    "@id": int(id),
    "@name": str(utf8(name)),
    "@x": int(0),
    "@y": int(0),
    "@pages": array(...pages),
  });
}

const page = (commands, trigger = 0) =>
  object("RPG::Event::Page", { "@trigger": int(trigger), "@list": array(...commands) });

function writeMap(project, id, events) {
  const data = object("RPG::Map", {
    "@tileset_id": int(1),
    "@width": int(17),
    "@height": int(13),
    "@events": hash(...events.map(([eventId, value]) => [int(eventId), value])),
  });
  writeFileSync(path.join(project, "Data", "Map" + String(id).padStart(3, "0") + ".rxdata"), dump(data));
}

// RMXP stores MapInfos as a Hash { mapId => RPG::MapInfo }.
function writeMapInfosHash(project, entries) {
  const pairs = entries.map(([id, name]) => [
    int(id),
    object("RPG::MapInfo", { "@name": str(utf8(name)), "@order": int(id), "@parent_id": int(0) }),
  ]);
  writeFileSync(path.join(project, "Data", "MapInfos.rxdata"), dump(hash(...pairs)));
}

// Some tools rewrite it as an Array of [id, MapInfo] pairs.
function writeMapInfosArray(project, entries) {
  const pairs = entries.map(([id, name]) =>
    array(int(id), object("RPG::MapInfo", { "@name": str(utf8(name)), "@order": int(id), "@parent_id": int(0) })),
  );
  writeFileSync(path.join(project, "Data", "MapInfos.rxdata"), dump(array(...pairs)));
}

function writeCommonEvents(project, entries) {
  writeFileSync(
    path.join(project, "Data", "CommonEvents.rxdata"),
    dump(array(...entries.map((entry) => (entry === null ? nilValue() : entry)))),
  );
}

const commonEvent = (id, name, commands, trigger = 0, switchId = 3) =>
  object("RPG::CommonEvent", {
    "@id": int(id),
    "@name": str(utf8(name)),
    "@trigger": int(trigger),
    "@switch_id": int(switchId),
    "@list": array(...commands),
  });

test("merges 355 and its 655 continuations into the complete Ruby block", () => {
  const project = makeProject();
  writeMap(
    project,
    1,
    [
      [
        7,
        event(7, "Sign", [
          page([
            command(101, str("hello")),
            command(355, str("if $game_variables[1] > 3")),
            command(655, str("  $game_variables[1] = 0")),
            command(655, str("end")),
            command(0),
          ]),
        ]),
      ],
    ],
  );
  writeMapInfosHash(project, [[1, "Town Map"]]);
  writeCommonEvents(project, [null]);

  const result = analyzeProjectEvents(project);
  assert.equal(result.ok, true, JSON.stringify(result.problems) + JSON.stringify(result.errors));
  assert.equal(result.scriptBlocks.length, 1);
  const block = result.scriptBlocks[0];
  assert.equal(block.rubySource, "if $game_variables[1] > 3\n  $game_variables[1] = 0\nend\n");
  assert.equal(block.continuations, 2);
  assert.equal(block.commandIndex, 1);
  assert.equal(block.mapId, 1);
  assert.equal(block.mapName, "Town Map");
  assert.equal(block.eventId, 7);
  assert.equal(block.eventName, "Sign");
  assert.equal(block.page, 1);
  assert.equal(block.pageIndex, 0);
  assert.equal(block.trigger, "action button");
  assert.equal(block.lines, 3);
  assert.equal(result.summary.continuationCommands, 2);
});

test("keeps unicode ruby sources and map names byte exact", () => {
  const project = makeProject();
  const source = "# 中文のコメント\npbItemBall(:POTION)";
  writeMap(project, 3, [[2, event(2, "看板", [page([command(355, str(utf8(source)))])])]]);
  writeMapInfosHash(project, [[3, "茶月镇"]]);
  writeCommonEvents(project, [null]);

  const result = analyzeProjectEvents(project);
  assert.equal(result.ok, true, JSON.stringify(result.errors));
  assert.equal(result.scriptBlocks.length, 1);
  assert.equal(result.scriptBlocks[0].rubySource, source + "\n");
  assert.equal(result.scriptBlocks[0].mapName, "茶月镇");
  assert.equal(result.scriptBlocks[0].eventName, "看板");
});

test("reads map names from the Array form of MapInfos too", () => {
  const project = makeProject();
  writeMap(project, 5, [[1, event(1, "NPC", [page([command(355, str("pbOkay"))])])]]);
  writeMapInfosArray(project, [[5, "Legacy Array Form"]]);
  writeCommonEvents(project, [null]);

  const result = analyzeProjectEvents(project);
  assert.equal(result.ok, true, JSON.stringify(result.errors));
  assert.equal(result.scriptBlocks[0].mapName, "Legacy Array Form");
});

test("orders events numerically and reports every page", () => {
  const project = makeProject();
  writeMap(
    project,
    1,
    [
      [10, event(10, "Tenth", [page([command(355, str("pbTen")), command(355, str("pbTenB"))])])],
      [2, event(2, "Second", [page([command(355, str("pbTwoA"))]), page([command(355, str("pbTwoB"))], 3)])],
    ],
  );
  writeMapInfosHash(project, []);
  writeCommonEvents(project, [null]);

  const result = analyzeProjectEvents(project);
  assert.equal(result.ok, true, JSON.stringify(result.errors));
  assert.deepEqual(
    result.scriptBlocks.map((block) => [block.eventId, block.page, block.rubySource.trim()]),
    [[2, 1, "pbTwoA"], [2, 2, "pbTwoB"], [10, 1, "pbTen\npbTenB"]],
  );
  assert.equal(result.summary.events, 2);
  assert.equal(result.summary.pages, 3);
  assert.equal(result.summary.mapsWithScripts, 1);
});

test("analyzes common events, skipping padded nil slots", () => {
  const project = makeProject();
  writeMap(project, 1, [[1, event(1, "NPC", [page([command(401)])])]]);
  writeMapInfosHash(project, [[1, "Town"]]);
  writeCommonEvents(project, [
    null,
    commonEvent(1, "Heal party", [command(355, str("pbHealParty"))]),
    null,
    commonEvent(3, "Weather", [command(355, str("pbWeather(:RAIN)")), command(0)], 1, 9),
  ]);

  const result = analyzeProjectEvents(project);
  assert.equal(result.ok, true, JSON.stringify(result.errors));
  assert.equal(result.summary.commonEvents, 2);
  assert.equal(result.summary.commonEventScriptBlocks, 2);
  const [first, second] = result.scriptBlocks;
  assert.equal(first.source, "commonEvent");
  assert.equal(first.mapId, null);
  assert.equal(first.commonEventId, undefined); // common event scope is explicit in the record
  assert.equal(first.eventId, 1);
  assert.equal(first.eventName, "Heal party");
  assert.equal(first.trigger, "autorun");
  assert.equal(first.switchId, 3);
  assert.equal(second.trigger, "parallel process");
  assert.equal(second.switchId, 9);
});

test("treats consecutive 355 commands as one script, like the project interpreter", () => {
  const project = makeProject();
  // Game_Interpreter#command_355 keeps consuming while the next command is
  // 655 OR 355, and terminates every chunk with a newline.
  writeMap(
    project,
    7,
    [
      [
        3,
        event(3, "Three chunks", [
          page([
            command(355, str("count=$Trainer.pokemonCount")),
            command(355, str("for i in 1..count")),
            command(655, str("  pbSet(6,i)")),
            command(655, str("end")),
            command(0),
          ]),
        ]),
      ],
    ],
  );
  writeMapInfosHash(project, [[7, "Chunked"]]);
  writeCommonEvents(project, [null]);

  const result = analyzeProjectEvents(project);
  assert.equal(result.ok, true, JSON.stringify(result.problems));
  assert.equal(result.scriptBlocks.length, 1);
  const block = result.scriptBlocks[0];
  assert.equal(block.rubySource, "count=$Trainer.pokemonCount\nfor i in 1..count\n  pbSet(6,i)\nend\n");
  assert.equal(block.continuations, 3);
  assert.equal(block.lines, 4);
  assert.equal(block.commandIndex, 0);
  assert.equal(result.summary.scriptLines, 4);
});

test("reports an orphan 655 with its location and fails the analysis", () => {
  const project = makeProject();
  writeMap(project, 4, [[1, event(1, "Broken", [page([command(655, str("orphan chunk")), command(355, str("pbOkay"))])])]]);
  writeMapInfosHash(project, [[4, "Broken Map"]]);

  const result = analyzeProjectEvents(project);
  assert.equal(result.ok, false);
  assert.equal(result.scriptBlocks.length, 1);
  assert.equal(result.problems.length, 1);
  const problem = result.problems[0];
  assert.equal(problem.code, 655);
  assert.equal(problem.commandIndex, 0);
  assert.match(problem.location, /Map004\.rxdata#event1 page 1 cmd 0/);
  assert.match(problem.reason, /without a preceding script command 355/);
  assert.ok(
    formatEventSummary(result).some((line) => line.includes("Map004.rxdata") && line.includes("uninterpretable")),
  );
});

test("reports a 355 without a string payload instead of guessing", () => {
  const project = makeProject();
  writeMap(project, 9, [[1, event(1, "Odd", [page([command(355, int(42))])])]]);
  writeMapInfosHash(project, []);
  writeCommonEvents(project, [null]);

  const result = analyzeProjectEvents(project);
  assert.equal(result.ok, false);
  assert.equal(result.scriptBlocks.length, 0);
  assert.equal(result.problems.length, 1);
  assert.match(result.problems[0].reason, /355 has no string payload/);
  assert.match(result.problems[0].location, /Map009\.rxdata#event1/);
});

test("stops merging when a 655 continuation is malformed", () => {
  const project = makeProject();
  writeMap(project, 6, [[1, event(1, "Half", [page([command(355, str("pbStart")), command(655, int(1))])])]]);
  writeMapInfosHash(project, [[6, "Half Map"]]);
  writeCommonEvents(project, [null]);

  const result = analyzeProjectEvents(project);
  assert.equal(result.ok, false);
  assert.equal(result.problems.length, 1);
  assert.equal(result.scriptBlocks.length, 1);
  assert.equal(result.scriptBlocks[0].rubySource, "pbStart\n");
  assert.equal(result.scriptBlocks[0].continuations, 0);
});

test("fails with a location when a map file cannot be parsed", () => {
  const project = makeProject();
  writeMap(project, 1, [[1, event(1, "Good", [page([command(355, str("pbOkay"))])])]]);
  writeMapInfosHash(project, [[1, "Good Map"]]);
  writeFileSync(path.join(project, "Data", "Map087.rxdata"), Buffer.from([4, 8, 0x5b, 0x0a, 0x69]));

  const result = analyzeProjectEvents(project);
  assert.equal(result.ok, false);
  assert.equal(result.summary.mapsAnalyzed, 1);
  assert.ok(result.errors.some((error) => error.file === "Map087.rxdata"));
  assert.ok(
    formatEventSummary(result).some((line) => line.includes("Map087.rxdata") && line.includes("unreadable data")),
  );
});

test("fails when CommonEvents.rxdata is missing", () => {
  const project = makeProject();
  writeMap(project, 1, [[1, event(1, "NPC", [page([command(355, str("pbOkay"))])])]]);
  writeMapInfosHash(project, [[1, "Town"]]);

  const result = analyzeProjectEvents(project);
  assert.equal(result.ok, false);
  assert.ok(result.errors.some((error) => error.file === "CommonEvents.rxdata"));
});

test("warns when MapInfos.rxdata is missing but still analyzes", () => {
  const project = makeProject();
  writeMap(project, 1, [[1, event(1, "NPC", [page([command(355, str("pbOkay"))])])]]);
  writeCommonEvents(project, [null, commonEvent(1, "Heal party", [command(355, str("pbHealParty"))])]);

  const result = analyzeProjectEvents(project);
  assert.equal(result.ok, true, JSON.stringify(result.errors));
  assert.equal(result.scriptBlocks[0].mapName, null);
  assert.ok(result.warnings.some((warning) => warning.includes("MapInfos.rxdata not found")));
});

test("fails when the Data directory is missing", () => {
  const project = mkdtempSync(path.join(os.tmpdir(), "pb-nodata-"));
  const result = analyzeProjectEvents(project);
  assert.equal(result.ok, false);
  assert.ok(result.errors.some((error) => error.file.includes("Data")));
});

test("handles project paths with spaces, Chinese and Japanese characters", () => {
  const project = makeProject("我的 项目 プロジェクト");
  writeMap(project, 2, [[1, event(1, "鈴", [page([command(355, str(utf8("pbBell(")))])])]]);
  writeMapInfosHash(project, [[2, "ニビシティ"]]);
  writeCommonEvents(project, [null, commonEvent(1, "日本語", [command(355, str("pbOkay"))])]);

  const result = analyzeProjectEvents(project);
  assert.equal(result.ok, true, JSON.stringify(result.errors));
  assert.equal(result.scriptBlocks.length, 2);
  assert.equal(result.scriptBlocks[0].mapName, "ニビシティ");
  assert.equal(result.scriptBlocks[1].eventName, "日本語");
});

test("restricts the analysis to the requested maps", () => {
  const project = makeProject();
  writeMap(project, 1, [[1, event(1, "One", [page([command(355, str("pbOne"))])])]]);
  writeMap(project, 2, [[1, event(1, "Two", [page([command(355, str("pbTwo"))])])]]);
  writeMapInfosHash(project, [[1, "First"], [2, "Second"]]);
  writeCommonEvents(project, [null, commonEvent(1, "Heal party", [command(355, str("pbHealParty"))])]);

  const only = analyzeProjectEvents(project, { onlyMapIds: [2] });
  assert.equal(only.summary.mapFiles, 1);
  assert.equal(only.summary.commonEvents, 0);
  assert.equal(only.scriptBlocks.length, 1);
  assert.equal(only.scriptBlocks[0].rubySource, "pbTwo\n");

  const both = analyzeProjectEvents(project, { onlyMapIds: [] });
  assert.equal(both.summary.mapFiles, 2);

  const missing = analyzeProjectEvents(project, { onlyMapIds: [42] });
  assert.equal(missing.summary.mapFiles, 0);
  assert.ok(missing.warnings.some((warning) => warning.includes("requested map 42 not found")));
});

test("analyzes the real reference project when it is available", (t) => {
  const candidate = path.join(REFERENCE_PROJECT, "Data", "Map001.rxdata");
  if (!existsSync(candidate)) {
    t.skip("reference project not available in this environment");
    return;
  }
  const result = analyzeProjectEvents(path.dirname(path.dirname(candidate)));
  assert.equal(result.ok, true, JSON.stringify(result.errors) + JSON.stringify(result.problems));
  // Live reference project: adding a map raises this, so assert a floor.
  assert.ok(result.summary.mapFiles >= 522, `map files ${result.summary.mapFiles}`);
  assert.ok(result.summary.scriptBlocks > 5000);
  assert.equal(result.summary.problems, 0);
  assert.ok(result.summary.events > 8000);
  assert.ok(result.scriptBlocks.every((block) => block.rubySource.length > 0));
  assert.ok(result.maps.every((map) => map.mapName !== null));
});

function runCli(args) {
  try {
    const stdout = execFileSync(process.execPath, [path.join(analyzerDir, "analyze.js"), ...args], {
      encoding: "utf8",
      stdio: ["ignore", "pipe", "pipe"],
    });
    return { code: 0, stdout };
  } catch (error) {
    return { code: error.status, stdout: String(error.stdout || "") };
  }
}

test("CLI reports success and failure with exit codes", () => {
  const project = makeProject();
  writeMap(project, 1, [[1, event(1, "Sign", [page([command(355, str("pbOkay"))])])]]);
  writeMapInfosHash(project, [[1, "Town"]]);
  writeCommonEvents(project, [null, commonEvent(1, "Heal party", [command(355, str("pbHealParty"))])]);

  const ok = runCli([project, "--json", path.join(project, "..", "events.json"), "--list", "5"]);
  assert.equal(ok.code, 0, ok.stdout);
  assert.match(ok.stdout, /EVENT ANALYSIS OK/);
  assert.match(ok.stdout, /script blocks: 2/);

  const empty = mkdtempSync(path.join(os.tmpdir(), "pb-no-data-"));
  const failed = runCli([empty]);
  assert.equal(failed.code, 1, failed.stdout);
  assert.match(failed.stdout, /EVENT ANALYSIS FAILED/);

  const usage = runCli([]);
  assert.equal(usage.code, 1);
  assert.match(usage.stdout, /Usage:/);
});

test("analyzer scripts.rxdata fixture stays out of the way", () => {
  // The event analyzer must not depend on Scripts.rxdata at all.
  const project = makeProject();
  writeMap(project, 1, [[1, event(1, "NPC", [page([command(355, str("pbOkay"))])])]]);
  writeMapInfosHash(project, [[1, "Town"]]);
  writeCommonEvents(project, [null, commonEvent(1, "Heal party", [command(355, str("pbHealParty"))])]);
  writeFileSync(
    path.join(project, "Data", "Scripts.rxdata"),
    dump(array(array(int(0), str("Main"), str(deflateText("pbMain\n"))))),
  );

  const result = analyzeProjectEvents(project);
  assert.equal(result.ok, true, JSON.stringify(result.errors));
  assert.equal(result.scriptBlocks.length, 2);
});
