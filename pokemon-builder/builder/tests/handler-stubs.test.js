// R8.2 tests: handler stub generation (project3 section 26).

import test from "node:test";
import assert from "node:assert/strict";

import { stubClassName, commentSafe, stubSource, generateStubs } from "../src/handler-stubs.js";

const block = {
  id: "map70/event4/page1/cmd2",
  category: "PLUGIN_API",
  apis: ["advanceQuestToStage"],
  reason: "project plugin API",
  source: { mapId: 70, eventId: 4, page: 1, commandIndex: 2 },
  rubySource: "advanceQuestToStage(3)",
};

test("class names are valid Java identifiers", () => {
  assert.equal(stubClassName("map70/event4/page1/cmd2"), "Script_map70_event4_page1_cmd2_Handler");
  assert.equal(stubClassName("9 weird-id"), "Script_9_weird_id_Handler");
  assert.match(stubClassName(""), /^Script_/);
});

test("a stub embeds the source location and the original Ruby", () => {
  const source = stubSource(block);
  assert.match(source, /package pokemon\.runtime\.script\.generated;/);
  assert.match(source, /class Script_map70_event4_page1_cmd2_Handler/);
  assert.match(source, /map=70 event=4 page=1/);
  assert.match(source, /advanceQuestToStage\(3\)/);
  assert.ok(source.includes("public final class"), "stubs must be compileable Java");
});

test("Ruby that would close the javadoc cannot break the class", () => {
  const source = stubSource({ ...block, rubySource: "a */ b" });
  assert.ok(source.includes("*\\/"), "*/ must be escaped");
  assert.ok(!source.includes("a */ b"), "raw */ must not survive");
});

test("generation honours the limit", () => {
  const problems = { javaHandlerRequired: [block, { ...block, id: "map70/event4/page1/cmd3" }] };
  assert.equal(generateStubs(problems, { limit: 1 }).length, 1);
  assert.equal(generateStubs(problems, { limit: 0 }).length, 2);
  assert.equal(generateStubs({}, {}).length, 0);
});
