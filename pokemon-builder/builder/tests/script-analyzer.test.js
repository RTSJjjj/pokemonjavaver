// Script analyzer tests (Phase 5). Covers the symbol table, the core/plugin
// boundary detection, classification, unique pattern statistics, unresolved
// identifier reporting and failure paths.

import test from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, writeFileSync, existsSync } from "node:fs";
import { execFileSync } from "node:child_process";
import path from "node:path";
import os from "node:os";
import { fileURLToPath } from "node:url";

import {
  analyzeScriptUsage,
  classifyScript,
  collectDefinitions,
  collectLocals,
  formatScriptSummary,
  classifySections,
  scanScript,
  stripRubyComments,
  originOf,
  ESSENTIALS_API,
  PLUGIN_API,
  RMXP_GLOBAL,
  SIMPLE_EXPRESSION,
  COMPLEX_RUBY,
  UNKNOWN,
} from "../../tools/script-analyzer/index.js";
import { array, dump, hash, int, nilValue, object, str, deflateText } from "./marshal-writer.js";
const REFERENCE_PROJECT = process.env.RMXP_PROJECT || fileURLToPath(new URL("../../../", import.meta.url));

const utf8 = (text) => Buffer.from(text, "utf8").toString("latin1");
const analyzerDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..", "tools", "script-analyzer");

function makeProject(name = "ScriptProject") {
  const root = mkdtempSync(path.join(os.tmpdir(), "pb-scripts-"));
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

function writeMap(project, id, events) {
  const data = object("RPG::Map", {
    "@tileset_id": int(1),
    "@width": int(17),
    "@height": int(13),
    "@events": hash(...events.map(([eventId, value]) => [int(eventId), value])),
  });
  writeFileSync(path.join(project, "Data", "Map" + String(id).padStart(3, "0") + ".rxdata"), dump(data));
}

function writeScripts(project, sections) {
  const payload = sections.map(([id, name, source]) =>
    array(int(id), str(utf8(name)), str(deflateText(source))),
  );
  writeFileSync(path.join(project, "Data", "Scripts.rxdata"), dump(array(...payload)));
}

function writeMapInfos(project, entries) {
  const pairs = entries.map(([id, name]) => [
    int(id),
    object("RPG::MapInfo", { "@name": str(utf8(name)), "@order": int(id), "@parent_id": int(0) }),
  ]);
  writeFileSync(path.join(project, "Data", "MapInfos.rxdata"), dump(hash(...pairs)));
}

function writeCommonEvents(project, entries) {
  writeFileSync(
    path.join(project, "Data", "CommonEvents.rxdata"),
    dump(array(...entries.map((entry) => (entry === null ? nilValue() : entry)))),
  );
}

const commonEvent = (id, name, commands) =>
  object("RPG::CommonEvent", {
    "@id": int(id),
    "@name": str(utf8(name)),
    "@trigger": int(0),
    "@switch_id": int(1),
    "@list": array(...commands),
  });

const CORE_SCRIPT = [
  "def pbSet(index, value) return value end",
  "module PBItems",
  "  def self.getName(id) return id end",
  "end",
  "",
  "def pbSetSelfSwitch(*args)",
  '  $game_self_switches[[@map_id,@event_id,args[0]]] = args[1] ? true : false',
  "end",
  "",
  "def pbItemBall(item, quantity = 1)",
  "  Kernel.pbMessage(_INTL(\"Obtained {1}!\", item))",
  "end",
  "",
  "def pbTrainerEnd",
  "end",
].join("\n");

const PLUGIN_SCRIPT = [
  "def pbGenPkmn(species, level)",
  "  p = pbNewPkmn(species, level)",
  "  return p",
  "end",
  "",
  "def advanceQuestToStage(stage, amount)",
  "end",
].join("\n");

const pluginSections = (project) => ({
  sections: [
    { index: 0, name: "Settings", source: CORE_SCRIPT },
    { index: 1, name: "==================", source: "" },
    { index: 2, name: "Essentials Plugin v1.2", source: PLUGIN_SCRIPT },
  ],
});

// A ready to analyze project: one map with three script blocks plus a common
// event, and a two section script list (core + plugin).
function makeScriptProject(name) {
  const project = makeProject(name);
  writeScripts(project, [
    [0, "Settings", CORE_SCRIPT],
    [1, "Essentials Plugin v1.2", PLUGIN_SCRIPT],
  ]);
  writeMapInfos(project, [[1, "Start Town"]]);
  writeMap(project, 1, [
    [
      1,
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
      2,
      event(2, "Store", [
        page([command(355, str("pbItemBall(:POTION,2)")), command(655, str("pbItemBall(:REPEL,1)"))]),
      ]),
    ],
  ]);
  writeCommonEvents(project, [null, commonEvent(1, "Weather", [command(355, str("advanceQuestToStage(2,3)"))])]);
  return project;
}

async function analyze(project) {
  const { analyzeProjectEvents } = await import("../../tools/event-analyzer/index.js");
  const events = analyzeProjectEvents(project);
  const { readScriptSources } = await import("../../tools/scanner/index.js");
  const sections = readScriptSources(project);
  return analyzeScriptUsage(
    events,
    sections.sections.map((section) => ({ index: section.index, name: section.name, source: section.source })),
  );
}

test("collectDefinitions finds methods, classes and project globals", () => {
  const symbols = collectDefinitions(pluginSections().sections);
  assert.deepEqual([...symbols.keys()].sort(), [
    "$game_self_switches", "PBItems", "advanceQuestToStage", "getName", "pbGenPkmn",
    "pbItemBall", "pbSet", "pbSetSelfSwitch", "pbTrainerEnd",
  ]);
  assert.ok(symbols.get("pbItemBall").kinds.has("method"));
  assert.ok(symbols.get("PBItems").kinds.has("class/module"));
  assert.ok(symbols.get("$game_self_switches").kinds.has("global"),
    "index assignment of a nested hash counts as a global definition");
  assert.ok(symbols.get("getName"), "def self.getName is registered without the self prefix");
  assert.ok(symbols.get("pbItemBall").sections.has(0));
  assert.ok(!symbols.get("pbItemBall").sections.has(2));
  assert.ok(symbols.get("pbGenPkmn").sections.has(2));
});

test("classifySections detects the plugin area from the version marker", () => {
  const { sections, cutoff } = classifySections(pluginSections().sections);
  assert.equal(cutoff, 2);
  assert.equal(sections[0].isPlugin, false);
  assert.equal(sections[0].isCore, true);
  assert.equal(sections[1].isPlugin, false);
  assert.equal(sections[1].isCore, true, "separator sections stay neutral");
  assert.equal(sections[2].isPlugin, true);
  assert.equal(sections[2].isCore, false);
});

test("classifySections treats CJK named utility sections as project code", () => {
  const { sections } = classifySections([
    { index: 0, name: "Settings", source: "def pbSetSelfSwitch\nend" },
    { index: 1, name: "版本与更新_逻辑", source: "def check_update\nend" },
    { index: 2, name: "Interpreter", source: "def pbExecuteScript\nend" },
  ]);
  assert.equal(sections[0].isCore, true);
  assert.equal(sections[1].isCore, false);
  assert.equal(sections[1].isPlugin, false, "an injected script is not a plugin marker");
  assert.equal(sections[2].isCore, true);
});

test("stripRubyComments removes comments and string literals", () => {
  const code = 'x = 1 # a comment with pbItemBall(\n_y = "pbMessage(inside)"\n=begin\npbHidden\n=end\npbKeep()';
  const stripped = stripRubyComments(code);
  assert.equal(stripped.includes("a comment with"), false);
  assert.equal(stripped.includes("inside"), false);
  assert.equal(stripped.includes("pbHidden"), false);
  assert.equal(stripped.includes("pbKeep()"), true);
});

test("collectLocals finds assignments, for loops and block parameters", () => {
  const code = "count = 0\nfor i in 1..count\n  badges = 3\nend\n[1, 2].each do |item|\n  price = item\nend";
  const locals = collectLocals(code);
  for (const name of ["count", "i", "badges", "item", "price"]) assert.ok(locals.has(name), name);
  assert.equal(locals.has("pbItemBall"), false);
});

test("scanScript separates calls, symbols, constants, globals and ivars", () => {
  const scan = scanScript("pbItemBall(:POTION,1)\n$Trainer.pokemonCount\n@p.iv = 1\nPBItems::POTION\nitem != 0");
  const calls = scan.calls.map((call) => call.name + "(" + call.shape + ")");
  assert.ok(calls.includes("pbItemBall(sym,num)"));
  assert.ok(!calls.includes("iv()"), "attribute writes are not calls");
  const receiverCall = scan.symbols.find((symbol) => symbol.name === "pokemonCount");
  assert.equal(receiverCall.receiver, true);
  assert.equal(receiverCall.argumentFollows, true, "an argument can follow $Trainer.pokemonCount");
  const attribute = scan.symbols.find((symbol) => symbol.name === "iv");
  assert.equal(attribute.receiver, true);
  assert.equal(attribute.argumentFollows, false, "p.iv = ... is an attribute write");
  assert.deepEqual(scan.ivars.map((ivar) => ivar.name), ["@p"]);
  assert.deepEqual(scan.globals.map((global) => global.name), ["$Trainer"]);
  assert.deepEqual(scan.constants.map((constant) => constant.name), ["PBItems"]);
  assert.ok(!scan.symbols.some((symbol) => symbol.name === "item!"), "!= is an operator, not a bang method");
});

test("originOf separates essentials, plugin, rmxp and unknown identifiers", () => {
  const { sections, cutoff } = classifySections(pluginSections().sections);
  const symbols = collectDefinitions(sections);
  const core = new Set(sections.filter((section) => section.isCore).map((section) => section.index));
  assert.equal(originOf("pbItemBall", symbols, core), "essential");
  assert.equal(originOf("pbTrainerEnd", symbols, core), "essential");
  assert.equal(originOf("$game_self_switches", symbols, core), "essential", "defined in the core section");
  assert.equal(originOf("pbGenPkmn", symbols, core), "plugin");
  assert.equal(originOf("advanceQuestToStage", symbols, core), "plugin");
  assert.equal(originOf("$game_variables", symbols, core), "rmxp");
  assert.equal(originOf("Sprite", symbols, core), "rmxp");
  assert.equal(originOf("String", symbols, core), "ruby");
  assert.equal(originOf("someUnknownThing", symbols, core), "unresolved");
  assert.equal(
    originOf("pbSomethingUndefined", symbols, core),
    "essential",
    "an Essentials named identifier without a definition is still engine API",
  );
});

test("classifyScript assigns the documented categories", () => {
  const { sections } = classifySections(pluginSections().sections);
  const symbols = collectDefinitions(sections);
  const core = new Set(sections.filter((section) => section.isCore).map((section) => section.index));
  assert.equal(classifyScript("pbItemBall(:POTION,1)", symbols, core).category, ESSENTIALS_API);
  assert.equal(classifyScript("pbGenPkmn(:PIKACHU,5)", symbols, core).category, PLUGIN_API);
  assert.equal(classifyScript("$game_variables[12] += 1", symbols, core).category, RMXP_GLOBAL);
  assert.equal(classifyScript("count = 3", symbols, core).category, SIMPLE_EXPRESSION);
  assert.equal(classifyScript("if $game_switches[1]\n  pbSet(6,1)\nend", symbols, core).category, ESSENTIALS_API);
  assert.equal(classifyScript("while false\n  count = 1\nend", symbols, core).category, COMPLEX_RUBY);
  assert.equal(classifyScript("mysteryCall(1)", symbols, core).category, UNKNOWN);
  const verdict = classifyScript("mysteryCall(1)", symbols, core);
  assert.deepEqual(verdict.unresolved, ["mysteryCall"]);
});

test("analyzeScriptUsage reports unique patterns, not occurrences", () => {
  const project = makeScriptProject();
  return analyze(project).then((result) => {
    assert.equal(result.ok, true, JSON.stringify(result.errors));
    assert.equal(result.summary.totalScriptBlocks, 4);
    assert.equal(result.summary.byCategory[ESSENTIALS_API], 3);
    assert.equal(result.summary.byCategory[PLUGIN_API], 1);
    const itemBall = result.apis.find((api) => api.name === "pbItemBall");
    assert.equal(itemBall.occurrences, 3);
    assert.equal(itemBall.blocks, 2);
    assert.equal(itemBall.category, ESSENTIALS_API);
    assert.deepEqual(
      itemBall.patterns.map((pattern) => pattern.pattern).sort(),
      ["pbItemBall(sym)", "pbItemBall(sym,num)"],
    );
    const trainerEnd = result.apis.find((api) => api.name === "pbTrainerEnd");
    assert.equal(trainerEnd.occurrences, 1);
    assert.deepEqual(trainerEnd.patterns.map((pattern) => pattern.pattern), ["pbTrainerEnd(<no-parens>)"]);
    const quest = result.apis.find((api) => api.name === "advanceQuestToStage");
    assert.equal(quest.category, PLUGIN_API);
    assert.equal(quest.blocks, 1);
    assert.deepEqual(quest.patterns.map((pattern) => pattern.pattern), ["advanceQuestToStage(num,num)"]);
  });
});

test("analyzeScriptUsage keeps every script block classified with its location", () => {
  const project = makeScriptProject();
  return analyze(project).then((result) => {
    assert.equal(result.classification.length, 4);
    const entry = result.classification.find((record) => record.id === "map1/event2/page1/cmd0");
    assert.equal(entry.source, "map");
    assert.equal(entry.mapId, 1);
    assert.equal(entry.mapName, "Start Town");
    assert.equal(entry.eventName, "Store");
    assert.equal(entry.page, 1);
    assert.equal(entry.commandIndex, 0);
    assert.equal(entry.category, ESSENTIALS_API);
    const common = result.classification.find((record) => record.source === "commonEvent");
    assert.equal(common.eventName, "Weather");
    assert.equal(common.category, PLUGIN_API);
    assert.ok(formatScriptSummary(result).some((line) => line.includes("classified script blocks: 4")));
  });
});

test("analyzeScriptUsage reports unresolved identifiers instead of dropping them", () => {
  const project = makeScriptProject();
  writeMap(project, 9, [[1, event(1, "Broken", [page([command(355, str("completelyUnknownHelper(1)")), command(355, str("pbItemBall(:POTION)"))])])]]);
  return analyze(project).then((result) => {
    assert.equal(result.ok, true);
    assert.equal(result.summary.unresolvedIdentifiers, 1);
    const identifier = result.unresolvedIdentifiers[0];
    assert.equal(identifier.name, "completelyUnknownHelper");
    assert.equal(identifier.occurrences, 1);
    assert.equal(identifier.sample.eventName, "Broken");
    assert.ok(
      formatScriptSummary(result).some((line) => line.includes("unresolved: completelyUnknownHelper")),
    );
  });
});

test("analyzeScriptUsage fails when no script section can be read", () => {
  const project = makeProject();
  writeMapInfos(project, [[1, "No Scripts"]]);
  writeMap(project, 1, [[1, event(1, "NPC", [page([command(355, str("pbOkay"))])])]]);
  writeCommonEvents(project, [null]);
  return analyze(project).then((result) => {
    assert.equal(result.ok, false);
    assert.equal(result.summary.totalScriptBlocks, 0);
    assert.ok(result.errors.some((error) => error.file === "Scripts.rxdata"));
  });
});

test("analyzes the real reference project when it is available", (t) => {
  const candidate = path.join(REFERENCE_PROJECT, "Data", "Map001.rxdata");
  if (!existsSync(candidate)) {
    t.skip("reference project not available in this environment");
    return;
  }
  return analyze(path.dirname(path.dirname(candidate))).then((result) => {
    assert.equal(result.ok, true, JSON.stringify(result.errors));
    // Live reference project: the count only grows with new script lines
    // (2026-10-03 baseline: 5441 blocks).
    assert.ok(result.summary.totalScriptBlocks >= 5441,
      `script blocks ${result.summary.totalScriptBlocks}`);
    assert.ok(result.summary.byCategory[ESSENTIALS_API] > 4000);
    assert.ok(result.apis.some((api) => api.name === "pbItemBall" && api.uniquePatterns >= 2));
    assert.ok(result.sections.coreSections.length > 100);
    assert.ok(result.sections.pluginSections.length > 50);
    assert.equal(result.sections.coreCutoff, 272);
  });
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

test("CLI prints the classification and exits 0", () => {
  const project = makeScriptProject();
  const run = runCli([project, "--json", path.join(project, "..", "scripts.json"), "--top", "3"]);
  assert.equal(run.code, 0, run.stdout);
  assert.match(run.stdout, /classified script blocks: 4/);
  assert.match(run.stdout, /ESSENTIALS_API: 3/);
  assert.match(run.stdout, /PLUGIN_API: 1/);
  assert.match(run.stdout, /unique APIs: \d+/);
  assert.match(run.stdout, /SCRIPT ANALYSIS OK/);

  const detail = runCli([project, "--api", "pbItemBall", "--quiet"]);
  assert.equal(detail.code, 0, detail.stdout);
  assert.match(detail.stdout, /pbItemBall\(sym,num\)/);

  const missing = runCli([]);
  assert.equal(missing.code, 1);
  assert.match(missing.stdout, /Usage:/);
});
