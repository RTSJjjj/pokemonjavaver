// Boss_Battles pipeline step: Scripts.rxdata "Boss_Battles" -> exported .rb + BossBattleData.java.

import test from "node:test";
import assert from "node:assert/strict";
import { mkdirSync, readFileSync, writeFileSync, existsSync } from "node:fs";
import path from "node:path";

import { array, dump, int, str, deflateText } from "./marshal-writer.js";
import { scratch } from "./fixtures.js";
import { syncBossBattles } from "../src/boss-battles.js";
import { generateBossBattleData } from "../../tools/boss-battles/generate.mjs";

const bossDef = (name, species, level, decision = 1) => [
  `def ${name}`,
  "  $game_switches[196] = true",
  "  count = $Trainer.ablePokemonCount",
  "  size = (count > 2) ? 3 : (count > 1) ? 2 : 1",
  '  setBattleRule(sprintf("%dv1",size))',
  '  setBattleRule("canlose")',
  '  setBattleRule("noexp")',
  `  pkmn = pbGenPkmn(:${species}, ${level})`,
  "  pkmn.battleRank = 3",
  "  decision = pbWildBattleCore(pkmn)",
  "  $game_switches[196] = false",
  `  return decision==${decision}`,
  "end",
  "",
].join("\n");

function writeProject(root, sections) {
  const project = path.join(root, "Project");
  mkdirSync(path.join(project, "Data"), { recursive: true });
  const rows = [array(int(0), str("Main"), str(deflateText("def pbMain\nend\n")))];
  sections.forEach(([name, source], i) => rows.push(array(int(i + 1), str(name), str(deflateText(source)))));
  writeFileSync(path.join(project, "Data", "Scripts.rxdata"), dump(array(...rows)));
  return project;
}

function writeBlocks(builder, apis) {
  const dir = path.join(builder, "generated", "scripts");
  mkdirSync(dir, { recursive: true });
  writeFileSync(path.join(dir, "blocks.json"), JSON.stringify({ blocks: apis.map((api) => ({ apis: [api] })) }));
}

test("generator exposes the transcription as a function", () => {
  const { entries, java } = generateBossBattleData(bossDef("battleAlpha", "ARCEUS", 80));
  assert.deepEqual(entries.map((e) => e.name), ["battleAlpha"]);
  assert.match(java, /add\(new Entry\("battleAlpha"/);
});

test("a def added to Boss_Battles reaches BossBattleData.java and the section export", (t) => {
  {
    const root = scratch("boss-sync-", t);
    const builder = path.join(root, "builder");
    const project = writeProject(root, [["Boss_Battles", bossDef("battleAlpha", "ARCEUS", 80) + bossDef("battleBeta", "MEW", 70, 2)]]);
    writeBlocks(builder, ["battleAlpha", "battleBeta"]);
    const result = syncBossBattles(builder, project);
    assert.equal(result.ok, true, result.errors.join("; "));
    assert.equal(result.defs, 2);
    assert.equal(result.entries, 2);
    assert.equal(result.changed, true);
    const java = readFileSync(result.javaFile, "utf8");
    assert.match(java, /"battleAlpha"/);
    assert.match(java, /"battleBeta"/);
    assert.ok(existsSync(path.join(builder, "plugin-src", "ruby", "1_Boss_Battles.rb")));

    // A second run with the same project is a no-op.
    assert.equal(syncBossBattles(builder, project).changed, false);
  }
});

test("a def with an unknown statement fails the step with its line", (t) => {
  {
    const root = scratch("boss-bad-", t);
    const bad = bossDef("battleBad", "MEW", 50).replace("  pkmn.battleRank = 3", "  pkmn.explode");
    const project = writeProject(root, [["Boss_Battles", bad]]);
    const result = syncBossBattles(path.join(root, "builder"), project);
    assert.equal(result.ok, false);
    assert.match(result.errors.join("\n"), /battleBad.*unknown statement pkmn\.explode/);
  }
});

test("an event calling a boss that has no def fails the step", (t) => {
  {
    const root = scratch("boss-missing-", t);
    const builder = path.join(root, "builder");
    const project = writeProject(root, [["Boss_Battles", bossDef("battleAlpha", "ARCEUS", 80)]]);
    writeBlocks(builder, ["battleAlpha", "battleGhost"]);
    const result = syncBossBattles(builder, project);
    assert.equal(result.ok, false);
    assert.match(result.errors.join("\n"), /battleGhost/);
  }
});

test("a project without the Boss_Battles section is skipped, not failed", (t) => {
  {
    const root = scratch("boss-none-", t);
    const builder = path.join(root, "builder");
    const project = writeProject(root, []);
    const result = syncBossBattles(builder, project);
    assert.equal(result.ok, true);
    assert.equal(result.skipped, true);
    assert.equal(existsSync(path.join(builder, "runtime")), false);
  }
});
