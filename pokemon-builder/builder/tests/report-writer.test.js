// Report writer tests (Phase 6). Covers the five Markdown reports, the machine
// readable audit JSON and the guarantee that hand written docs are untouched.

import test from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, existsSync } from "node:fs";
import path from "node:path";
import os from "node:os";

import {
  writeAuditReports,
  buildScriptAuditJson,
  isComplexBlock,
  sourceAuditMarkdown,
  scriptEventAuditMarkdown,
  apiUsageMarkdown,
  pluginUsageMarkdown,
  unsupportedScriptsMarkdown,
} from "../../tools/report-writer/index.js";
import { analyzeProjectEvents } from "../../tools/event-analyzer/index.js";
import { analyzeScriptUsage } from "../../tools/script-analyzer/index.js";
import { array, dump, hash, int, nilValue, object, str, deflateText } from "./marshal-writer.js";

const utf8 = (text) => Buffer.from(text, "utf8").toString("latin1");

function makeProject(name = "ReportProject") {
  const root = mkdtempSync(path.join(os.tmpdir(), "pb-reports-"));
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
    "@pages": array(...pages),
  });
}

const page = (commands, trigger = 0) =>
  object("RPG::Event::Page", { "@trigger": int(trigger), "@list": array(...commands) });

const CORE_SCRIPT = [
  "def pbSet(index, value) return value end",
  "module PBItems",
  "  def self.getName(id) return id end",
  "end",
  "def pbItemBall(item, quantity = 1)",
  "  Kernel.pbMessage(_INTL(\"got it\"))",
  "end",
  "def pbTrainerEnd",
  "end",
].join("\n");

const PLUGIN_SCRIPT = [
  "def pbGenPkmn(species, level)",
  "  return pbNewPkmn(species, level)",
  "end",
  "def advanceQuestToStage(stage, amount)",
  "end",
].join("\n");

function buildFixture(name) {
  const project = makeProject(name);
  writeFileSync(
    path.join(project, "Data", "Scripts.rxdata"),
    dump(
      array(
        array(int(0), str(utf8("Settings")), str(deflateText(CORE_SCRIPT))),
        array(int(1), str(utf8("Quest Plugin v1.0")), str(deflateText(PLUGIN_SCRIPT))),
      ),
    ),
  );
  writeFileSync(
    path.join(project, "Data", "MapInfos.rxdata"),
    dump(hash([int(1), object("RPG::MapInfo", { "@name": str(utf8("Start Town")), "@order": int(1), "@parent_id": int(0) })])),
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
            event(1, "Sign", [
              page([
                command(401),
                command(355, str("pbItemBall(:POTION)")),
                command(402, int(0)),
                command(355, str("pbTrainerEnd")),
              ]),
            ]),
          ],
          [
            int(2),
            event(2, "Shop", [page([command(355, str("if $game_switches[1]")), command(655, str("  pbItemBall(:POTION,2)")), command(655, str("end"))])]),
          ],
        ),
      }),
    ),
  );
  writeFileSync(
    path.join(project, "Data", "CommonEvents.rxdata"),
    dump(array(nilValue(), object("RPG::CommonEvent", {
      "@id": int(1),
      "@name": str(utf8("Quest tick")),
      "@trigger": int(0),
      "@switch_id": int(1),
      "@list": array(command(355, str("advanceQuestToStage(2,3)"))),
    }))),
  );
  return project;
}

function analyze(project) {
  const events = analyzeProjectEvents(project);
  const sections = [
    { index: 0, name: "Settings", source: CORE_SCRIPT, lines: 9 },
    { index: 1, name: "Quest Plugin v1.0", source: PLUGIN_SCRIPT, lines: 8 },
  ];
  const scripts = analyzeScriptUsage(events, sections);
  const scan = {
    ok: true,
    warnings: [],
    errors: [],
    data: {
      files: [
        { name: "Map001.rxdata", kind: "map", ok: true, bytes: 1234, error: null },
        { name: "Scripts.rxdata", kind: "scripts", ok: true, bytes: 4321, error: null },
      ],
      otherFiles: [{ name: "items.dat", bytes: 2048 }],
      parsedCount: 2,
      rxdataCount: 2,
      failedCount: 0,
      kindCounts: { map: 1, scripts: 1 },
    },
    scripts: { totalSections: 2, readableSections: 2, totalLines: 17, inflatedBytes: 900, problems: [] },
  };
  return { project, events, scripts, scan };
}

test("isComplexBlock flags control flow, long and merged scripts", () => {
  assert.equal(isComplexBlock({ rubySource: "pbItemBall(:POTION)", lines: 1, continuations: 0 }), false);
  assert.equal(isComplexBlock({ rubySource: "if true\n  pbSet(1,1)\nend", lines: 3, continuations: 0 }), true);
  assert.equal(isComplexBlock({ rubySource: "a = 1\nb = 2\nc = 3", lines: 3, continuations: 0 }), true);
  assert.equal(isComplexBlock({ rubySource: "x", lines: 1, continuations: 2 }), true);
});

test("buildScriptAuditJson carries map / event / page / type / method / source", () => {
  const { project, events, scripts, scan } = analyze(buildFixture());
  const json = buildScriptAuditJson({ project, version: "0.1.0", scan, events, scripts });
  assert.equal(json.totals.scriptBlocks, 4);
  assert.equal(json.scripts.length, 4);
  const signBlock = json.scripts.find((record) => record.eventName === "Sign" && record.page === 1);
  assert.equal(signBlock.map, 1);
  assert.equal(signBlock.mapName, "Start Town");
  assert.equal(signBlock.event, 1);
  assert.equal(signBlock.page, 1);
  assert.equal(signBlock.commandIndex, 1);
  assert.equal(signBlock.type, "ESSENTIALS_API");
  assert.equal(signBlock.method, "pbItemBall");
  assert.equal(signBlock.rubySource, "pbItemBall(:POTION)\n");
  const shop = json.scripts.find((record) => record.eventName === "Shop");
  assert.equal(shop.complex, true);
  assert.equal(shop.continuations, 2);
  assert.match(shop.rubySource, /if \$game_switches\[1\]/);
  assert.equal(
    json.totals.uniqueApis,
    json.apis.filter((api) => api.kind === "api" || api.kind === "method").length,
  );
  assert.ok(json.apis.some((api) => api.name === "advanceQuestToStage" && api.category === "PLUGIN_API"));
  assert.ok(!json.apis.some((api) => api.name === "pbGenPkmn"), "an unused plugin method is not an API usage");
  assert.equal(json.sections.coreCutoff, 1);
  assert.deepEqual(json.maps[0], {
    mapId: 1,
    mapName: "Start Town",
    file: "Map001.rxdata",
    events: 2,
    pages: 2,
    scriptBlocks: 3,
    ok: true,
    error: null,
  });
});

test("markdown reports contain the required sections and numbers", () => {
  const { project, events, scripts, scan } = analyze(buildFixture());
  const context = { project, events, scripts, scan, version: "0.1.0" };

  const source = sourceAuditMarkdown(context);
  assert.match(source, /# source-audit/);
  assert.match(source, /合计: \*\*2\/2\*\* 个 `\.rxdata` 解析成功/);
  assert.match(source, /items\.dat/);

  const eventAudit = scriptEventAuditMarkdown(context);
  assert.match(eventAudit, /# script-event-audit/);
  assert.match(eventAudit, /Script block\(355\/655 合并后\) \| 4/);
  assert.match(eventAudit, /ESSENTIALS_API \| 3/);
  assert.match(eventAudit, /\| API \| Count \| Blocks \| Maps \| Unique patterns \| 参数形态 \|/);
  assert.match(eventAudit, /pbItemBall/);

  const apiUsage = apiUsageMarkdown(context);
  assert.match(apiUsage, /# api-usage/);
  assert.match(apiUsage, /Essentials API\(被事件使用\)/);
  assert.match(apiUsage, /需重点翻译的 API/);

  const pluginUsage = pluginUsageMarkdown(context);
  assert.match(pluginUsage, /# plugin-usage/);
  assert.match(pluginUsage, /第 1 段起出现插件标记/);
  assert.match(pluginUsage, /Quest Plugin v1\.0/);
  assert.match(pluginUsage, /advanceQuestToStage/);

  const unsupported = unsupportedScriptsMarkdown(context);
  assert.match(unsupported, /# unsupported-scripts/);
  assert.match(unsupported, /无。所有事件脚本标识符/);
  assert.match(unsupported, /无法解压的 Scripts\.rxdata 段/);
});

test("unsupported scripts report lists unresolved identifiers with their location", () => {
  const project = buildFixture("BrokenReports");
  writeFileSync(
    path.join(project, "Data", "Map009.rxdata"),
    dump(
      object("RPG::Map", {
        "@events": hash([
          int(1),
          event(1, "Broken", [page([command(355, str("completelyUnknownHelper(1)"))])]),
        ]),
      }),
    ),
  );
  const { events, scripts, scan } = analyze(project);
  const markdown = unsupportedScriptsMarkdown({ project, events, scripts, scan, version: "0.1.0" });
  assert.match(markdown, /completelyUnknownHelper/);
  assert.match(markdown, /Map009\.rxdata event 1/);
});

test("writeAuditReports writes every deliverable and keeps architecture.md", () => {
  const root = mkdtempSync(path.join(os.tmpdir(), "pb-report-out-"));
  mkdirSync(path.join(root, "docs"), { recursive: true });
  writeFileSync(path.join(root, "docs", "architecture.md"), "# hand written\n", "utf8");
  const { project, events, scripts, scan } = analyze(buildFixture("Written"));

  const result = writeAuditReports({
    project,
    version: "0.1.0",
    scan,
    events,
    scripts,
    docsDir: path.join(root, "docs"),
    reportsDir: path.join(root, "build", "reports"),
  });
  assert.equal(result.written.length, 6);
  for (const file of [
    "source-audit.md",
    "script-event-audit.md",
    "api-usage.md",
    "plugin-usage.md",
    "unsupported-scripts.md",
  ]) {
    assert.ok(existsSync(path.join(root, "docs", file)), file);
    assert.ok(readFileSync(path.join(root, "docs", file), "utf8").length > 100, file);
  }
  assert.ok(existsSync(result.json));
  const json = JSON.parse(readFileSync(result.json, "utf8"));
  assert.equal(json.totals.scriptBlocks, 4);
  assert.equal(json.scripts.length, 4);
  assert.equal(readFileSync(path.join(root, "docs", "architecture.md"), "utf8"), "# hand written\n");
});