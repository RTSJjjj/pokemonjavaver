import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";
import { parseQuestData } from "../src/quest-data.js";

const here = path.dirname(fileURLToPath(import.meta.url));
const pluginFile = path.join(here, "..", "..", "plugin-src", "ruby", "284_004_Quest_Data.rb");

test("a quest hash: strings, escapes, arrays, comments and trailing commas", () => {
  const quests = parseQuestData(`
module QuestModule
  # a comment
  Quest0 = {

  }
  Quest7 = {
    :ID => "7",
    :Name => "名字 \\"引号\\"",
    :QuestGiver => '无',
    :Stage1 => "第一步",
    :Stage2 => "第二步", # trailing comment
    :Location1 => "地点一",
    :QuestDescription => [
      "段落一\\n换行",
      "段落二",
    ],
    :RewardString => "奖励"
  }
end
`);
  assert.equal(quests.length, 2);
  assert.deepEqual(quests[0], {
    key: "Quest0", id: undefined, name: undefined, questGiver: undefined, stages: {}, locations: {},
    description: undefined, reward: undefined,
  });
  const q = quests[1];
  assert.equal(q.key, "Quest7");
  assert.equal(q.name, '名字 "引号"');
  assert.equal(q.questGiver, "无");
  assert.deepEqual(q.stages, { 1: "第一步", 2: "第二步" });
  assert.deepEqual(q.locations, { 1: "地点一" });
  assert.deepEqual(q.description, ["段落一\n换行", "段落二"]);
});

test("anything outside the literal subset fails instead of being guessed", () => {
  assert.throws(() => parseQuestData('Quest1 = { :Name => "#{x}" }'), /Quest1: string interpolation/);
  assert.throws(() => parseQuestData("Quest1 = { :Name => foo }"), /Quest1: unsupported value/);
});

test("the project's quest table: 120 quests, every one with the stage the story needs", { skip: !existsSync(pluginFile) }, () => {
  const quests = parseQuestData(readFileSync(pluginFile, "utf8"));
  assert.equal(quests.length, 120);
  const q1 = quests.find((q) => q.key === "Quest1");
  assert.equal(q1.id, "1");
  assert.equal(Object.keys(q1.stages).length, 2);
  assert.equal(Array.isArray(q1.description), true);
  const q3 = quests.find((q) => q.key === "Quest3");
  assert.equal(typeof q3.description, "string");
  const q300 = quests.find((q) => q.key === "Quest300");
  assert.equal(q300.locations[1], "群号：537650384");
});
