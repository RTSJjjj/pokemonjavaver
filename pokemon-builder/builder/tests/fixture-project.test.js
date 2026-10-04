// R15: Small Test Project fixture (project3 sections 56-59).
//
// The fixture is a two map RMXP project under tests/fixture-project/. These
// tests convert a fresh copy (so they never depend on the committed JSON
// being current for the behavioural assertions) and then compare the committed
// generated/ against it - the "golden" drift check the Java tests rely on.

import test from "node:test";
import assert from "node:assert/strict";
import { existsSync, mkdtempSync, readFileSync } from "node:fs";
import path from "node:path";
import os from "node:os";
import { fileURLToPath } from "node:url";

import { buildFixtureGenerated } from "../../tests/fixture-project/generate.mjs";
import { compileBlocks, evaluateReleasePolicy } from "../src/script-compiler.js";

const BUILDER_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");
const FIXTURE_DIR = path.join(BUILDER_ROOT, "tests", "fixture-project");

function readJson(file) {
  return JSON.parse(readFileSync(file, "utf8"));
}

// One shared fresh conversion for every test in this file.
let fresh = null;
function freshFixture() {
  if (!fresh) {
    fresh = buildFixtureGenerated(mkdtempSync(path.join(os.tmpdir(), "pb-fixture-")));
  }
  return fresh;
}

test("the Small Test Project converts with the documented shape (R15 fixture)", () => {
  const { generatedDir, coverage } = freshFixture();
  const project = readJson(path.join(generatedDir, "project.json"));
  assert.equal(project.status, "ok");
  assert.equal(project.counts.maps, 2);
  assert.equal(project.counts.events, 7);
  assert.equal(project.counts.commonEvents, 1);
  assert.equal(project.counts.scriptBlocks, 2);
  assert.equal(project.counts.unresolvedIdentifiers, 1);
  assert.equal(coverage.blocks, 2);
  assert.equal(coverage.translated, 1);
  assert.equal(coverage.unsupported, 1);

  const map1 = readJson(path.join(generatedDir, "maps", "map-001.json"));
  const byName = (name) => map1.events.find((event) => event.name === name);
  assert.ok(byName("NPC"), "map 1 carries the NPC");
  assert.ok(byName("Door"), "map 1 carries the door");
  assert.ok(byName("Chooser"), "map 1 carries the choice event");
  assert.ok(byName("CallCommon"), "map 1 carries the common event caller");

  // Show Text + Control Switch + Control Variable on the NPC page.
  const npcCommands = byName("NPC").pages[0].commands;
  assert.equal(npcCommands[0].code, 101);
  assert.equal(npcCommands[0].parameters[0], "Hello from the fixture!");
  assert.deepEqual(npcCommands[1].parameters, [1, 1, 0]);
  assert.deepEqual(npcCommands[2].parameters, [1, 1, 0, 0, 5]);

  // The door is a player touch transfer to map 2 (10, 7).
  const doorPage = byName("Door").pages[0];
  assert.equal(doorPage.trigger, 1);
  assert.equal(doorPage.commands[0].code, 201);
  assert.deepEqual(doorPage.commands[0].parameters, [0, 2, 10, 7, 0, 0]);

  // Show Choices with the two documented options and their branches.
  const choiceCommands = byName("Chooser").pages[0].commands;
  assert.equal(choiceCommands[0].code, 102);
  assert.deepEqual(choiceCommands[0].parameters[0], ["Yes", "No"]);
  assert.equal(choiceCommands[1].code, 402);
  assert.deepEqual(choiceCommands[1].parameters, [1, "No"]);
  assert.equal(choiceCommands[2].parameters[4], 9, "second option sets VAR1 = 9");
  assert.equal(choiceCommands[4].parameters[1], 1, "first option switches SW2 on");

  // Call Common Event (RMXP code 116) with common event 1.
  assert.equal(byName("CallCommon").pages[0].commands[0].code, 116);
  assert.deepEqual(byName("CallCommon").pages[0].commands[0].parameters, [1]);

  const common = readJson(path.join(generatedDir, "common-events", "common-event-001.json"));
  assert.equal(common.commands[0].code, 122);
  assert.deepEqual(common.commands[0].parameters, [1, 1, 0, 0, 7]);
});

test("pbItemBall(:POTION, 3) compiles into GIVE_ITEM (R15 section 58)", () => {
  const { generatedDir } = freshFixture();
  const ir = readJson(path.join(generatedDir, "scripts", "ir.json"));
  const give = ir.commands.find((entry) => entry.ir && entry.ir.command === "GIVE_ITEM");
  assert.ok(give, "the fixture's ItemGiver block must translate");
  assert.equal(give.ir.item, "POTION");
  assert.equal(give.ir.amount, 3);
  assert.equal(give.source.mapId, 1);
  assert.equal(give.source.eventId, 5);
  assert.equal(give.source.commandIndex, 0);
});

test("an unknown Ruby block fails a release build with its source location (R15 section 59)", () => {
  const { generatedDir } = freshFixture();
  const blocks = readJson(path.join(generatedDir, "scripts", "blocks.json"));
  const { coverage } = compileBlocks(blocks);
  assert.equal(coverage.unsupported, 1);
  const problem = coverage.problems.unsupported[0];
  assert.match(problem.reason, /some_unknown_ruby_magic/);
  assert.match(problem.rubySource, /some_unknown_ruby_magic\(42\)/);
  // Source location: map / event / page / command.
  assert.equal(problem.source.mapId, 1);
  assert.equal(problem.source.eventId, 6);
  assert.equal(problem.source.page, 1);
  assert.equal(problem.source.commandIndex, 0);
  assert.equal(problem.source.file, "Map001.rxdata");

  const release = evaluateReleasePolicy(coverage.problems);
  assert.equal(release.exitCode, 1, "a release build must FAIL");
  assert.equal(release.blocked, 1);
});

test("the committed fixture generated/ matches a fresh conversion (golden drift)", () => {
  const freshDir = freshFixture().generatedDir;
  const committedDir = path.join(FIXTURE_DIR, "generated");
  assert.ok(existsSync(path.join(committedDir, "project.json")), "committed fixture generated/ exists");

  const stableFiles = [
    "maps/index.json",
    "maps/map-001.json",
    "maps/map-002.json",
    "events/index.json",
    "common-events/index.json",
    "common-events/common-event-001.json",
    "scripts/blocks.json",
    "scripts/apis.json",
    "scripts/sections.json",
    "scripts/ir.json",
    "system.json",
    "tilesets.json",
    "animations.json",
    "connections.json",
    "metadata/problems.json",
  ];
  for (const file of stableFiles) {
    assert.deepEqual(
      readJson(path.join(freshDir, file)),
      readJson(path.join(committedDir, file)),
      "fixture drift in " + file + " - run `node tests/fixture-project/generate.mjs`",
    );
  }

  const freshProject = readJson(path.join(freshDir, "project.json"));
  const committedProject = readJson(path.join(committedDir, "project.json"));
  assert.deepEqual(committedProject.counts, freshProject.counts);
  assert.equal(committedProject.status, "ok");
});
