// Phase 8: Debug IR converter tests.
//
// convertProject() never writes into the RMXP project, so the tests can run
// it against the fixture project and against the real reference project with
// a scratch output directory.

import test from "node:test";
import assert from "node:assert/strict";
import { existsSync, mkdirSync, readdirSync, writeFileSync, rmSync, readFileSync, statSync } from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";

import {
  convertProject,
  buildTitleIr,
  formatConvertSummary,
  normalizeConnection,
  resolveConnectionEdge,
  blockIndex,
  IR_FORMAT,
} from "../../tools/data-converter/index.js";
import { array, bool, dump, hash, int, nilValue, object, str } from "./marshal-writer.js";
import { userdef } from "./marshal-writer.js";
import { makeProject, snapshotProject, scratch } from "./fixtures.js";
import { fileURLToPath } from "node:url";
const REFERENCE_PROJECT = process.env.RMXP_PROJECT || fileURLToPath(new URL("../../../", import.meta.url));

const converterDir = path.resolve(import.meta.dirname, "..", "..", "tools", "data-converter");

function readJson(file) {
  return JSON.parse(readFileSync(file, "utf8"));
}

test("script block ids stay on their own page (L6d)", () => {
  const blocks = [
    { id: "p1cmd3", page: 1, commandIndex: 3, continuations: 0 },
    { id: "p2cmd7", page: 2, commandIndex: 7, continuations: 1 },
  ];
  const page1 = blockIndex(blocks, 1);
  assert.equal(page1.get(3), "p1cmd3");
  assert.equal(page1.has(7), false, "page 2's block must not leak onto page 1");
  const page2 = blockIndex(blocks, 2);
  assert.equal(page2.get(7), "p2cmd7");
  assert.equal(page2.get(8), "p2cmd7", "the 655 continuations follow their block");
  assert.equal(page2.has(3), false,
    "page 1's block must not leak onto the same-index command of page 2");
  const common = blockIndex(blocks);
  assert.equal(common.get(3), "p1cmd3", "page-less callers (common events) keep every block");
  assert.equal(common.get(7), "p2cmd7");
});

test("converts a project into the JSON Debug IR", (t) => {
  const dir = scratch("pb-scratch-", t);
  const generatedDir = path.join(dir, "generated");
  const project = makeProject(dir, "FakeProject");
  writeFileSync(path.join(project, "PBS", "pokemon.txt"), "[1]\nName = Bulbasaur\n", "utf8");
  // A page with conditions, a graphic and a move route so the IR covers them.
  // One real common event so the common-events IR is covered too.
  writeFileSync(
    path.join(project, "Data", "CommonEvents.rxdata"),
    dump(
      array(
        nilValue(),
        object("RPG::CommonEvent", {
          "@id": int(1),
          "@name": str("Intro Setup"),
          "@trigger": int(0),
          "@switch_id": int(1),
          "@list": array(
            object("RPG::EventCommand", {
              "@code": int(355),
              "@indent": int(0),
              "@parameters": array(str("pbSet(1,1)")),
            }),
            object("RPG::EventCommand", {
              "@code": int(101),
              "@indent": int(0),
              "@parameters": array(str("Setup done")),
            }),
          ),
        }),
      ),
    ),
  );
  writeFileSync(
    path.join(project, "Data", "Map001.rxdata"),
    dump(
      object("RPG::Map", {
        "@tileset_id": int(1),
        "@width": int(17),
        "@height": int(13),
        "@encounter_step": int(30),
        "@bgm": object("RPG::AudioFile", { "@name": str("Field"), "@volume": int(80), "@pitch": int(100) }),
        "@encounter_list": array(
          object("RPG::Encounter", { "@troop_id": int(3), "@weight": int(20), "@regions": array(int(1), int(2)) }),
        ),
        "@events": hash([
          int(1),
          object("RPG::Event", {
            "@id": int(1),
            "@name": str("Professor"),
            "@x": int(5),
            "@y": int(6),
            "@pages": array(
              object("RPG::Event::Page", {
                "@trigger": int(1),
                "@condition": object("RPG::Event::Page::Condition", {
                  "@switch1_valid": bool(true),
                  "@switch1_id": int(3),
                  "@variable_valid": bool(true),
                  "@variable_id": int(12),
                  "@variable_value": int(4),
                  "@self_switch_valid": bool(true),
                  "@self_switch_ch": str("A"),
                }),
                "@graphic": object("RPG::Event::Page::Graphic", {
                  "@character_name": str("boy"),
                  "@character_hue": int(0),
                  "@direction": int(4),
                  "@pattern": int(1),
                  "@tile_id": int(0),
                  "@opacity": int(255),
                  "@blend_type": int(0),
                }),
                "@move_route": object("RPG::MoveRoute", {
                  "@repeat": bool(true),
                  "@skippable": bool(false),
                  "@list": array(object("RPG::MoveCommand", { "@code": int(0), "@parameters": array() })),
                }),
                "@list": array(
                  object("RPG::EventCommand", {
                    "@code": int(355),
                    "@indent": int(0),
                    "@parameters": array(str("pbSet(1,1)")),
                  }),
                  object("RPG::EventCommand", {
                    "@code": int(655),
                    "@indent": int(0),
                    "@parameters": array(str("pbMain")),
                  }),
                  object("RPG::EventCommand", {
                    "@code": int(101),
                    "@indent": int(0),
                    "@parameters": array(str("Hello"), ),
                  }),
                ),
              }),
            ),
          }),
        ]),
      }),
    ),
  );
  const before = snapshotProject(project);

  const result = convertProject(project, { generatedDir, version: "0.1.0" });
  assert.equal(result.ok, true, JSON.stringify(result.errors) + JSON.stringify(result.problems));
  assert.equal(result.generatedDir, generatedDir);
  assert.ok(result.files.length > 10);
  assert.ok(result.bytes > 0);

  const manifest = readJson(path.join(generatedDir, "project.json"));
  assert.equal(manifest.format, IR_FORMAT);
  assert.equal(manifest.kind, "project");
  assert.equal(manifest.status, "ok");
  assert.equal(manifest.counts.maps, 1);
  assert.equal(manifest.counts.events, 1);
  assert.equal(manifest.counts.scriptBlocks, 2);
  assert.equal(manifest.counts.scriptSections, 2);
  assert.equal(manifest.counts.scriptSectionsReadable, 2);
  assert.equal(manifest.counts.commonEvents, 1);
  assert.equal(manifest.counts.commonEventScriptBlocks, 1);
  assert.equal(manifest.counts.pbsFiles, 1);
  assert.equal(manifest.counts.unresolvedIdentifiers, 0);

  const mapsIndex = readJson(path.join(generatedDir, "maps", "index.json"));
  assert.equal(mapsIndex.total, 1);
  assert.equal(mapsIndex.maps[0].mapId, 1);
  assert.equal(mapsIndex.maps[0].name, "Starting Map");
  assert.equal(mapsIndex.maps[0].ir, "maps/map-001.json");

  const map = readJson(path.join(generatedDir, "maps", "map-001.json"));
  assert.equal(map.format, IR_FORMAT);
  assert.equal(map.kind, "map");
  assert.equal(map.width, 17);
  assert.equal(map.height, 13);
  assert.equal(map.tilesetId, 1);
  assert.equal(map.bgm.name, "Field");
  assert.equal(map.bgm.volume, 80);
  assert.equal(map.encounters.length, 1);
  assert.equal(map.encounters[0].troopId, 3);
  assert.deepEqual(map.encounters[0].regions, [1, 2]);
  assert.equal(map.encounterStep, 30);
  assert.equal(map.tileData.present, false);
  assert.equal(map.events.length, 1);
  const event = map.events[0];
  assert.equal(event.id, 1);
  assert.equal(event.name, "Professor");
  assert.equal(event.pages.length, 1);
  assert.equal(event.pages[0].trigger, 1);
  assert.equal(event.pages[0].conditions.switch1Valid, true);
  assert.equal(event.pages[0].conditions.switch1Id, 3);
  assert.equal(event.pages[0].conditions.selfSwitchCh, "A");
  assert.equal(event.pages[0].graphic.characterName, "boy");
  assert.equal(event.pages[0].movement.moveRoute.repeat, true);
  assert.equal(event.pages[0].movement.moveRoute.commands.length, 1);
  assert.equal(event.pages[0].commands.length, 3);
  assert.equal(event.pages[0].commands[0].code, 355);
  assert.equal(event.pages[0].commands[0].parameters[0], "pbSet(1,1)");
  assert.equal(event.pages[0].commands[0].scriptBlockId, "map1/event1/page1/cmd0");
  assert.equal(event.pages[0].commands[1].code, 655);
  assert.equal(event.pages[0].commands[1].scriptBlockId, "map1/event1/page1/cmd0");
  assert.equal(event.pages[0].commands[2].code, 101);
  assert.equal(event.pages[0].commands[2].parameters[0], "Hello");
  assert.deepEqual(event.scriptBlockIds, ["map1/event1/page1/cmd0"]);

  const events = readJson(path.join(generatedDir, "events", "map-001.json"));
  assert.equal(events.kind, "mapEvents");
  assert.equal(events.mapId, 1);
  assert.equal(events.scriptBlocks.length, 1);
  assert.equal(events.scriptBlocks[0].rubySource, "pbSet(1,1)\npbMain\n");
  assert.equal(events.scriptBlocks[0].continuations, 1);
  assert.equal(events.scriptBlocks[0].category, "ESSENTIALS_API");

  const commonIndex = readJson(path.join(generatedDir, "common-events", "index.json"));
  assert.equal(commonIndex.total, 1);
  assert.equal(commonIndex.commonEvents[0].commands, 2);
  assert.equal(commonIndex.commonEvents[0].scriptBlocks, 1);

  const sections = readJson(path.join(generatedDir, "scripts", "sections.json"));
  assert.equal(sections.total, 2);
  assert.equal(sections.readable, 2);
  assert.equal(sections.sections.length, 2);
  assert.equal(sections.sections[0].origin, "core");
  assert.equal(sections.sections[1].origin, "core");
  assert.equal(sections.sections[1].name, "Main");

  const blocks = readJson(path.join(generatedDir, "scripts", "blocks.json"));
  assert.equal(blocks.total, 2);
  assert.equal(blocks.blocks[0].id, "map1/event1/page1/cmd0");
  assert.equal(blocks.blocks[0].source, "map");
  assert.equal(blocks.blocks[0].category, "ESSENTIALS_API");
  assert.ok(blocks.blocks[0].calls.length > 0);

  const apis = readJson(path.join(generatedDir, "scripts", "apis.json"));
  assert.equal(apis.summary.totalScriptBlocks, 2);
  assert.ok(apis.apis.some((api) => api.name === "pbSet"));

  const sources = readJson(path.join(generatedDir, "metadata", "sources.json"));
  // Scripts, MapInfos, Map001, CommonEvents, Tilesets, System.\n  assert.equal(sources.dataFiles.length, 6);
  assert.equal(sources.pbs.exists, true);
  assert.equal(sources.pbs.files.length, 1);

  const build = readJson(path.join(generatedDir, "metadata", "build.json"));
  assert.equal(build.kind, "build");
  assert.equal(build.builderVersion, "0.1.0");
  assert.equal(build.ok, true);
  // The manifest cannot list itself, so it covers every other written file.
  const ownEntry = result.files.find((entry) => entry.file === "metadata/build.json");
  assert.ok(ownEntry);
  assert.equal(build.outputs.length, result.files.length - 1);
  assert.equal(build.totalBytes, result.bytes - ownEntry.bytes);
  assert.ok(build.outputs.some((entry) => entry.file === "maps/map-001.json"));

  const problems = readJson(path.join(generatedDir, "metadata", "problems.json"));
  assert.equal(problems.unreadableData.length, 0);
  assert.equal(problems.uninterpretableScripts.length, 0);
  assert.equal(problems.unreadableScriptSections.length, 0);
  assert.equal(problems.unresolvedIdentifiers.length, 0);

  // The source project is an input only.
  assert.deepEqual(snapshotProject(project), before);
});

test("replaces stale intermediate data", (t) => {
  const dir = scratch("pb-scratch-", t);
  const generatedDir = path.join(dir, "generated");
  const project = makeProject(dir, "FakeProject");
  const stale = path.join(generatedDir, "maps", "map-999.json");
  mkdirSync(path.dirname(stale), { recursive: true });
  writeFileSync(stale, "{}", "utf8");

  const result = convertProject(project, { generatedDir });
  assert.equal(result.ok, true, JSON.stringify(result.errors));
  assert.ok(!existsSync(stale));
  assert.ok(existsSync(path.join(generatedDir, "maps", "map-001.json")));
  for (const name of readdirSync(generatedDir)) {
    assert.ok(["project.json", "maps", "events", "common-events", "scripts", "metadata", "battle-animations",
      "connections.json", "animations.json", "tilesets.json", "system.json", "pbs", "text"].includes(name), name);
  }
});

test("keeps unrelated files inside generated/", (t) => {
  const dir = scratch("pb-scratch-", t);
  const generatedDir = path.join(dir, "generated");
  const project = makeProject(dir, "FakeProject");
  const keep = path.join(generatedDir, "notes.txt");
  mkdirSync(generatedDir, { recursive: true });
  writeFileSync(keep, "hand written note", "utf8");

  const result = convertProject(project, { generatedDir });
  assert.equal(result.ok, true, JSON.stringify(result.errors));
  assert.ok(existsSync(keep));
});

test("refuses to write inside the source project", (t) => {
  const dir = scratch("pb-scratch-", t);
  const project = makeProject(dir, "FakeProject");

  const result = convertProject(project, { generatedDir: path.join(project, "generated") });
  assert.equal(result.ok, false);
  assert.equal(result.errors.length, 1);
  assert.match(result.errors[0].error, /outside the source project/);
});

test("allows generated/ inside the builder root even when the builder sits in the project", (t) => {
  const dir = scratch("pb-scratch-", t);
  const project = makeProject(dir, "FakeProject");
  // The documented layout keeps the builder (and therefore its generated/
  // directory) inside the project folder.
  const builderRoot = path.join(project, "pokemon-builder");
  mkdirSync(builderRoot, { recursive: true });

  const result = convertProject(project, {
    generatedDir: path.join(builderRoot, "generated"),
    builderRoot,
  });
  assert.equal(result.ok, true, JSON.stringify(result.errors));
  assert.ok(existsSync(path.join(builderRoot, "generated", "project.json")));
});

test("fails when the Data directory is missing", (t) => {
  const dir = scratch("pb-scratch-", t);
  const project = path.join(dir, "Empty");
  mkdirSync(project, { recursive: true });

  const result = convertProject(project, { generatedDir: path.join(dir, "generated") });
  assert.equal(result.ok, false);
  assert.match(JSON.stringify(result.errors), /Data/);
});

test("fails when a map file cannot be parsed", (t) => {
  const dir = scratch("pb-scratch-", t);
  const project = makeProject(dir, "BrokenProject");
  writeFileSync(path.join(project, "Data", "Map001.rxdata"), Buffer.from([4, 8, 0x58]));

  const result = convertProject(project, { generatedDir: path.join(dir, "generated") });
  assert.equal(result.ok, false);
  assert.match(JSON.stringify(result.errors), /Map001\.rxdata/);
});

test("fails when Scripts.rxdata is missing", (t) => {
  const dir = scratch("pb-scratch-", t);
  const project = makeProject(dir, "NoScripts");
  rmSync(path.join(project, "Data", "Scripts.rxdata"), { force: true });

  const result = convertProject(project, { generatedDir: path.join(dir, "generated") });
  assert.equal(result.ok, false);
  assert.match(JSON.stringify(result.errors), /Scripts\.rxdata/);
});

test("reports malformed event scripts instead of dropping them", (t) => {
  const dir = scratch("pb-scratch-", t);
  const project = makeProject(dir, "BadScript");
  // Replace the map with a page whose 355 carries no string payload.
  // One real common event so the common-events IR is covered too.
  writeFileSync(
    path.join(project, "Data", "CommonEvents.rxdata"),
    dump(
      array(
        nilValue(),
        object("RPG::CommonEvent", {
          "@id": int(1),
          "@name": str("Intro Setup"),
          "@trigger": int(0),
          "@switch_id": int(1),
          "@list": array(
            object("RPG::EventCommand", {
              "@code": int(355),
              "@indent": int(0),
              "@parameters": array(str("pbSet(1,1)")),
            }),
            object("RPG::EventCommand", {
              "@code": int(101),
              "@indent": int(0),
              "@parameters": array(str("Setup done")),
            }),
          ),
        }),
      ),
    ),
  );
  writeFileSync(
    path.join(project, "Data", "Map001.rxdata"),
    dump(
      object("RPG::Map", {
        "@tileset_id": int(1),
        "@width": int(17),
        "@height": int(13),
        "@events": hash([
          int(1),
          object("RPG::Event", {
            "@id": int(1),
            "@name": str("Professor"),
            "@pages": array(
              object("RPG::Event::Page", {
                "@trigger": int(0),
                "@list": array(
                  object("RPG::EventCommand", {
                    "@code": int(355),
                    "@indent": int(0),
                    "@parameters": array(int(7)),
                  }),
                ),
              }),
            ),
          }),
        ]),
      }),
    ),
  );

  const result = convertProject(project, { generatedDir: path.join(dir, "generated") });
  assert.equal(result.ok, false);
  assert.ok(result.problems.uninterpretableScripts.length > 0);
  assert.match(JSON.stringify(result.problems.uninterpretableScripts), /355/);
  assert.match(JSON.stringify(result.problems.uninterpretableScripts), /Map001\.rxdata/);
});

test("converts the real reference project when it is available", (t) => {
  const candidate = path.join(REFERENCE_PROJECT, "Data", "Map001.rxdata");
  if (!existsSync(candidate)) {
    t.skip("reference project not available in this environment");
    return;
  }
  const dir = scratch("pb-scratch-", t);
  const result = convertProject(path.dirname(path.dirname(candidate)), { generatedDir: dir });
  assert.equal(result.ok, true, JSON.stringify(result.errors) + JSON.stringify(result.problems));
  // The reference project is a live checkout: every map, event or script the
  // author adds raises these numbers, so they are floors and not exact values
  // (2026-10-03 baseline: 522 maps, 8835 events, 5441 script blocks).
  assert.ok(result.counts.maps >= 522, `maps ${result.counts.maps}`);
  assert.ok(result.counts.events >= 8835, `events ${result.counts.events}`);
  assert.ok(result.counts.scriptBlocks >= 5441, `script blocks ${result.counts.scriptBlocks}`);
  assert.ok(result.counts.scriptSections >= 384, `script sections ${result.counts.scriptSections}`);
  assert.ok(result.counts.commonEvents >= 200, `common events ${result.counts.commonEvents}`);
  assert.ok(result.counts.commonEventScriptBlocks >= 11,
    `common event script blocks ${result.counts.commonEventScriptBlocks}`);
  assert.ok(result.counts.uniqueApis >= 151, `unique APIs ${result.counts.uniqueApis}`);
  assert.ok(result.counts.pbsFiles >= 19);
  assert.ok(existsSync(path.join(dir, "maps", "map-522.json")));
  assert.ok(existsSync(path.join(dir, "common-events", "common-event-001.json")));

  // Stage 2 (R2): the real map tile tables are decoded, not just recorded.
  const map = readJson(path.join(dir, "maps", "map-001.json"));
  assert.equal(map.tileData.z, 3);
  assert.equal(map.tileData.x, 20);
  assert.equal(map.tileData.y, 15);
  assert.equal(map.tileData.total, 900);
  assert.equal(map.tileData.layers.length, 3);
  assert.equal(map.tileData.layers[0].length, 300);

  const tilesets = readJson(path.join(dir, "tilesets.json"));
  assert.equal(tilesets.total, 50);
  const real = tilesets.tilesets.find((entry) => entry.id === 1);
  assert.equal(real.passages.total, 6624);
  assert.equal(real.passages.layers.length, 1);
  assert.equal(real.priorities.total, 6624);
  assert.equal(real.terrainTags.total, 6624);

  const system = readJson(path.join(dir, "system.json"));
  assert.equal(system.startMapId, 1);
  assert.equal(system.startX, 9);
  assert.equal(system.startY, 7);
  assert.ok(system.words.some(([key]) => key === "hp"));
  assert.ok(system.switches.length > 1);

  // R6.12: the runtime profile carries this project's own charsets / font /
  // screen size, so the Java runtime needs no per-project edits.
  const profile = readJson(path.join(dir, "project.json")).runtime;
  assert.equal(profile.playerCharset, "trchar000");
  assert.equal(profile.runningCharset, "boy_run");
  assert.ok(profile.messageFont.endsWith(".ttf"), profile.messageFont);
  assert.equal(profile.screenWidth, 672);
  assert.ok(profile.screenHeight > 0);

  // R6.16: PBS/metadata.txt "Outdoor" decides which maps get the day/night
  // shading (the project's pbDayNightTint only shades MetadataOutdoor maps).
  assert.equal(readJson(path.join(dir, "maps", "map-002.json")).outdoor, true,
    "茶月镇 is an outdoor town");
  assert.equal(readJson(path.join(dir, "maps", "map-353.json")).outdoor, false,
    "map 353 is an indoor room");
  const outdoorMaps = readdirSync(path.join(dir, "maps"))
    .filter((name) => /^map-\d+\.json$/.test(name))
    .map((name) => readJson(path.join(dir, "maps", name)))
    .filter((map) => map.outdoor === true).length;
  assert.ok(outdoorMaps > 100, "the project has many outdoor maps, got " + outdoorMaps);
});

test("exports the PBS metadata Outdoor flag per map (R6.16)", (t) => {
  const dir = scratch("pb-scratch-", t);
  const project = makeProject(dir, "FakeProject");
  writeFileSync(path.join(project, "PBS", "metadata.txt"), "[001]\nOutdoor = true\n", "utf8");
  const generatedDir = path.join(dir, "generated");
  const result = convertProject(project, { generatedDir });
  assert.equal(result.ok, true, JSON.stringify(result.errors));
  assert.equal(readJson(path.join(generatedDir, "maps", "map-001.json")).outdoor, true);

  // Without PBS/metadata.txt a map stays indoor (the project's own default).
  const other = scratch("pb-scratch-", t);
  const otherProject = makeProject(other, "FakeProject");
  const otherDir = path.join(other, "generated");
  assert.equal(convertProject(otherProject, { generatedDir: otherDir }).ok, true);
  assert.equal(readJson(path.join(otherDir, "maps", "map-001.json")).outdoor, false);
});

test("editing PBS/metadata.txt invalidates the cached map IR (R6.16)", (t) => {
  const dir = scratch("pb-scratch-", t);
  const project = makeProject(dir, "FakeProject");
  const generatedDir = path.join(dir, "generated");
  const cacheDir = path.join(dir, "build");
  const metadata = path.join(project, "PBS", "metadata.txt");
  writeFileSync(metadata, "[001]\nOutdoor = true\n", "utf8");

  const options = { generatedDir, cacheDir };
  const first = convertProject(project, options);
  assert.equal(first.ok, true, JSON.stringify(first.errors));
  assert.equal(readJson(path.join(generatedDir, "maps", "map-001.json")).outdoor, true);
  const cache = readJson(path.join(cacheDir, "cache.json"));
  assert.ok(cache.units["map:1"].dependencies.includes("PBS/metadata.txt"),
    "the map unit tracks the metadata file");

  writeFileSync(metadata, "[001]\nOutdoor = false\n", "utf8");
  const second = convertProject(project, options);
  assert.equal(second.ok, true, JSON.stringify(second.errors));
  assert.equal(second.incremental.used, true, "the cache is reused");
  assert.ok(!second.incremental.skipped.includes("map:1"),
    "changing Outdoor must recompile the map");
  assert.equal(readJson(path.join(generatedDir, "maps", "map-001.json")).outdoor, false);
});

test("resolves connection edges like MapFactoryHelper#getMapEdge (R6.23)", () => {
  // N/S keep the offset as x and use y = 0/height; E/W use x = width/0 and
  // keep the offset as y (the runtime derives the neighbour offsets from this).
  assert.deepEqual(resolveConnectionEdge("N", 3, 20, 15), [3, 0]);
  assert.deepEqual(resolveConnectionEdge("S", 3, 20, 15), [3, 15]);
  assert.deepEqual(resolveConnectionEdge("W", 4, 20, 15), [0, 4]);
  assert.deepEqual(resolveConnectionEdge("E", 4, 20, 15), [20, 4]);
  // csvEnumFieldOrInt allows a raw coordinate: it stays as x, offset is y.
  assert.deepEqual(resolveConnectionEdge("7", 2, 20, 15), [7, 2]);
  assert.deepEqual(resolveConnectionEdge("north", 1, 20, 15), [1, 0]);
});

test("normalizes connections.txt records against the map dimensions (R6.23)", () => {
  const dims = new Map([[195, [20, 30]], [189, [25, 30]]]);
  assert.deepEqual(
    normalizeConnection(["195", "West", "0", "189", "East", "0"], 1, dims),
    { a: 195, ax: 0, ay: 0, b: 189, bx: 25, by: 0 },
  );
  assert.deepEqual(
    normalizeConnection(["195", "North", "2", "189", "South", "4"], 2, dims),
    { a: 195, ax: 2, ay: 0, b: 189, bx: 4, by: 30 },
  );
  assert.throws(() => normalizeConnection(["195", "West", "0", "999", "East", "0"], 3, dims),
    /map 999 was not found/);
  assert.throws(() => normalizeConnection(["195", "West", "0"], 4, dims),
    /expected mapA/);
});

test("exports Show Animation data (R6.27)", (t) => {
  const dir = scratch("pb-scratch-", t);
  const project = makeProject(dir, "FakeProject");
  const generatedDir = path.join(dir, "generated");

  // One animation: a single frame with two cells plus a screen-flash timing.
  const cellBytes = Buffer.alloc(20 + 16 * 2);
  cellBytes.writeUInt32LE(2, 0);    // dimensions
  cellBytes.writeUInt32LE(2, 4);    // xsize = cell_max
  cellBytes.writeUInt32LE(8, 8);    // ysize = attributes per cell
  cellBytes.writeUInt32LE(1, 12);   // zsize
  cellBytes.writeUInt32LE(16, 16);  // total
  const rows = [[3, 100, 120, 100, 0, 0, 255, 0], [7, 200, 220, 100, 0, 1, 200, 1]];
  for (let cell = 0; cell < 2; cell++) {
    for (let attribute = 0; attribute < 8; attribute++) {
      cellBytes.writeInt16LE(rows[cell][attribute], 20 + (cell + 2 * attribute) * 2);
    }
  }
  const color = Buffer.alloc(32);
  [255, 128, 0, 255].forEach((value, i) => color.writeDoubleLE(value, i * 8));
  writeFileSync(path.join(project, "Data", "Animations.rxdata"), dump(array(
    nilValue(),
    object("RPG::Animation", {
      "@id": int(1),
      "@name": str("Grass rustle"),
      "@animation_name": str("DustandGrass"),
      "@animation_hue": int(0),
      "@position": int(1),
      "@frame_max": int(1),
      "@frames": array(object("RPG::Animation::Frame", {
        "@cell_max": int(2),
        "@cell_data": userdef("Table", cellBytes),
      })),
      "@timings": array(object("RPG::Animation::Timing", {
        "@frame": int(0),
        "@condition": int(0),
        "@flash_scope": int(2),
        "@flash_duration": int(5),
        "@flash_color": userdef("Color", color),
        "@se": object("RPG::AudioFile", {
          "@name": str("Exclaim"), "@volume": int(80), "@pitch": int(100),
        }),
      })),
    }),
  )));

  const result = convertProject(project, { generatedDir });
  assert.equal(result.ok, true, JSON.stringify(result.errors));
  const animations = readJson(path.join(generatedDir, "animations.json"));
  assert.equal(animations.total, 1);
  const animation = animations.animations[0];
  assert.equal(animation.graphic, "DustandGrass");
  assert.equal(animation.position, 1);
  assert.equal(animation.frames[0].length, 2);
  assert.deepEqual(animation.frames[0][1], {
    cell: 7, x: 200, y: 220, zoom: 100, angle: 0, flip: true, opacity: 200, blend: 1,
  });
  assert.equal(animation.timings[0].scope, 2);
  assert.deepEqual(animation.timings[0].color, [255, 128, 0, 255]);
  assert.deepEqual(animation.timings[0].se, { name: "Exclaim", volume: 80, pitch: 100 });
});

test("emits decoded tileset tables and the System start position (R2)", (t) => {
  const dir = scratch("pb-scratch-", t);
  const project = makeProject(dir, "FakeProject");
  const generatedDir = path.join(dir, "generated");
  const result = convertProject(project, { generatedDir });
  assert.equal(result.ok, true, JSON.stringify(result.errors));

  const tilesets = readJson(path.join(generatedDir, "tilesets.json"));
  assert.equal(tilesets.total, 1);
  const first = tilesets.tilesets[0];
  assert.equal(first.passages.total, 6624);
  assert.deepEqual(first.passages.layers[0].slice(0, 6), [0, 0, 0x0f, 0x0f, 0x0f, 0x0f]);
  assert.equal(first.priorities.layers[0][1], 1);
  assert.equal(first.terrainTags.layers[0][1], 1);

  const system = readJson(path.join(generatedDir, "system.json"));
  assert.equal(system.startMapId, 1);
  assert.equal(system.startX, 9);
  assert.equal(system.startY, 7);
  assert.ok(system.words.some(([key, text]) => key === "hp" && text === "HP"));

  const manifest = readJson(path.join(generatedDir, "project.json"));
  assert.equal(manifest.outputs.tilesets, "tilesets.json");
  assert.equal(manifest.outputs.system, "system.json");
});

test("standalone CLI writes the Debug IR and reports failures", (t) => {
  const dir = scratch("pb-scratch-", t);
  const project = makeProject(dir, "FakeProject");
  const out = path.join(dir, "out");

  let stdout = "";
  try {
    stdout = execFileSync(
      process.execPath,
      [path.join(converterDir, "convert.js"), project, "--out", out],
      { encoding: "utf8", stdio: ["ignore", "pipe", "pipe"] },
    );
  } catch (error) {
    assert.fail("converter CLI failed: " + String(error.stderr || error.message));
  }
  assert.match(stdout, /BUILD DATA OK/);
  assert.ok(existsSync(path.join(out, "project.json")));

  const missing = path.join(dir, "missing-project");
  let status = 0;
  try {
    execFileSync(process.execPath, [path.join(converterDir, "convert.js"), missing], {
      encoding: "utf8",
      stdio: ["ignore", "pipe", "pipe"],
    });
  } catch (error) {
    status = error.status;
  }
  assert.equal(status, 1);
});

test("formatConvertSummary stays short and factual", (t) => {
  const dir = scratch("pb-scratch-", t);
  const project = makeProject(dir, "FakeProject");
  const result = convertProject(project, { generatedDir: path.join(dir, "generated") });
  const lines = formatConvertSummary(result);
  assert.ok(lines.length > 0);
  assert.ok(lines.every((line) => typeof line === "string" && line.length > 0));
  assert.match(lines.join("\n"), /maps: 1/);
});
// ---------------------------------------------------------------------------
// Phase 13: incremental build cache (build/cache.json).
//
// The cache records the sha1 + mtime fingerprint of every unit input and the
// IR files each unit produced, so unchanged units are reused instead of being
// recompiled: "Map001 untouched is not recompiled, only Map002 is rebuilt".
// The cache is an accelerator only - a missing, foreign, corrupt or disabled
// cache must always produce a correct, full rebuild instead.
// ---------------------------------------------------------------------------

function mapFileNode(tilesetId) {
  return dump(
    object("RPG::Map", {
      "@tileset_id": int(tilesetId),
      "@width": int(20),
      "@height": int(15),
      "@events": hash(),
    }),
  );
}

// Millisecond mtime, used to prove that a reused IR file is not rewritten.
function mtimeMsOf(file) {
  return statSync(file).mtimeMs;
}

test("writes build/cache.json with fingerprints and dependencies", (t) => {
  const dir = scratch("pb-scratch-", t);
  const generatedDir = path.join(dir, "generated");
  const cacheDir = path.join(dir, "build");
  const project = makeProject(dir, "FakeProject");

  const result = convertProject(project, { generatedDir, cacheDir, version: "1.2.3" });
  assert.equal(result.ok, true, JSON.stringify(result.errors));
  assert.equal(result.incremental.used, false, "the first build cannot reuse anything");

  const cache = readJson(path.join(cacheDir, "cache.json"));
  assert.equal(cache.format, "pokemon-builder/build-cache/2");
  assert.equal(cache.irFormat, IR_FORMAT);
  assert.equal(cache.builderVersion, "1.2.3");
  assert.equal(cache.project, project);

  const mapUnit = cache.units["map:1"];
  assert.ok(mapUnit, "map unit missing from the cache");
  assert.equal(mapUnit.kind, "map");
  assert.equal(mapUnit.source, "Data/Map001.rxdata");
  assert.deepEqual(mapUnit.dependencies, [
    "Data/Map001.rxdata",
    "Data/MapInfos.rxdata",
    "Data/Scripts.rxdata",
  ]);
  assert.deepEqual(Object.keys(mapUnit.fingerprints).sort(), [...mapUnit.dependencies].sort());
  for (const dependency of mapUnit.dependencies) {
    assert.match(mapUnit.fingerprints[dependency], /^[0-9A-F]{16}$/, dependency);
  }
  assert.deepEqual(mapUnit.outputs, ["maps/map-001.json", "events/map-001.json"]);
  assert.equal(mapUnit.mapsIndex.mapId, 1);
  assert.equal(mapUnit.mapsIndex.ir, "maps/map-001.json");
  assert.ok(cache.units.commonEvents && cache.units.scripts, "aggregate units missing");

  // Every input keeps its sha1, byte size and mtime for change detection.
  const source = cache.sources["Data/Map001.rxdata"];
  assert.match(source.sha1, /^[0-9A-F]{16}$/);
  assert.ok(source.bytes > 0);
  assert.equal(typeof source.mtime, "string");
  assert.ok(cache.sources["PBS/abilities.txt"] === undefined, "no PBS in the fixture");
});

test("reuses unchanged units and rebuilds only the changed map", (t) => {
  const dir = scratch("pb-scratch-", t);
  const generatedDir = path.join(dir, "generated");
  const cacheDir = path.join(dir, "build");
  const project = makeProject(dir, "FakeProject");
  writeFileSync(path.join(project, "Data", "Map002.rxdata"), mapFileNode(2));

  const options = { generatedDir, cacheDir, version: "1.2.3" };
  const first = convertProject(project, options);
  assert.equal(first.ok, true, JSON.stringify(first.errors));
  const firstMapOne = mtimeMsOf(path.join(generatedDir, "maps", "map-001.json"));

  // Nothing changed: every unit is reused and map-001.json is not rewritten.
  const second = convertProject(project, options);
  assert.equal(second.ok, true, JSON.stringify(second.errors));
  assert.equal(second.incremental.used, true);
  assert.deepEqual(second.incremental.rebuilt, []);
  assert.deepEqual(
    second.incremental.skipped.slice().sort(),
    ["commonEvents", "map:1", "map:2", "scripts"],
  );
  assert.equal(second.counts.mapsSkipped, 2);
  assert.equal(second.counts.mapsRebuilt, 0);
  assert.equal(mtimeMsOf(path.join(generatedDir, "maps", "map-001.json")), firstMapOne);

  // Only Map002.rxdata changes: map:1 stays untouched, map:2 is rebuilt and
  // the script aggregates (which embed every event block) are rewritten.
  writeFileSync(path.join(project, "Data", "Map002.rxdata"), mapFileNode(3));
  const third = convertProject(project, options);
  assert.equal(third.ok, true, JSON.stringify(third.errors));
  assert.deepEqual(third.incremental.rebuilt.slice().sort(), ["map:2", "scripts"]);
  assert.deepEqual(third.incremental.skipped.slice().sort(), ["commonEvents", "map:1"]);
  assert.equal(mtimeMsOf(path.join(generatedDir, "maps", "map-001.json")), firstMapOne);
  assert.equal(readJson(path.join(generatedDir, "maps", "map-002.json")).tilesetId, 3);

  // The aggregate index still lists both maps with their current data.
  const index = readJson(path.join(generatedDir, "maps", "index.json"));
  assert.deepEqual(index.maps.map((entry) => [entry.mapId, entry.tilesetId]), [
    [1, 1],
    [2, 3],
  ]);
  assert.equal(readJson(path.join(generatedDir, "project.json")).counts.maps, 2);
});

test("useCache:false rebuilds every unit and refreshes the cache", (t) => {
  const dir = scratch("pb-scratch-", t);
  const generatedDir = path.join(dir, "generated");
  const cacheDir = path.join(dir, "build");
  const project = makeProject(dir, "FakeProject");
  const options = { generatedDir, cacheDir, version: "1.2.3" };

  assert.equal(convertProject(project, options).ok, true);
  const forced = convertProject(project, { ...options, useCache: false });
  assert.equal(forced.ok, true);
  assert.equal(forced.incremental.used, false);
  assert.equal(forced.incremental.skipped.length, 0);
  assert.ok(forced.incremental.rebuilt.length >= 3, "full rebuild expected");

  // The refreshed cache is used again on the next default run.
  const next = convertProject(project, options);
  assert.equal(next.incremental.used, true);
  assert.deepEqual(next.incremental.rebuilt, []);
});

test("a deleted IR output invalidates only its own cache unit", (t) => {
  const dir = scratch("pb-scratch-", t);
  const generatedDir = path.join(dir, "generated");
  const cacheDir = path.join(dir, "build");
  const project = makeProject(dir, "FakeProject");
  const options = { generatedDir, cacheDir, version: "1.2.3" };

  assert.equal(convertProject(project, options).ok, true);
  rmSync(path.join(generatedDir, "maps", "map-001.json"), { force: true });

  const second = convertProject(project, options);
  assert.equal(second.ok, true, JSON.stringify(second.errors));
  assert.ok(second.incremental.rebuilt.includes("map:1"), "map:1 must be rebuilt");
  assert.ok(second.incremental.skipped.includes("commonEvents"), "other units stay reused");
  assert.ok(existsSync(path.join(generatedDir, "maps", "map-001.json")));
});

test("a corrupt or foreign cache triggers a full rebuild", (t) => {
  const dir = scratch("pb-scratch-", t);
  const generatedDir = path.join(dir, "generated");
  const cacheDir = path.join(dir, "build");
  const project = makeProject(dir, "FakeProject");
  const options = { generatedDir, cacheDir, version: "1.2.3" };

  assert.equal(convertProject(project, options).ok, true);

  const cacheFile = path.join(cacheDir, "cache.json");
  writeFileSync(cacheFile, "{ this is not json", "utf8");
  const corrupt = convertProject(project, options);
  assert.equal(corrupt.ok, true, JSON.stringify(corrupt.errors));
  assert.equal(corrupt.incremental.used, false);
  assert.ok(corrupt.incremental.rebuilt.length >= 3);

  // A cache from another project (or another builder version) is ignored too.
  const foreign = readJson(cacheFile);
  foreign.project = path.join(dir, "SomeOtherProject");
  writeFileSync(cacheFile, JSON.stringify(foreign), "utf8");
  const mismatched = convertProject(project, options);
  assert.equal(mismatched.ok, true);
  assert.equal(mismatched.incremental.used, false);

  const wrongVersion = readJson(cacheFile);
  wrongVersion.builderVersion = "0.0.1";
  writeFileSync(cacheFile, JSON.stringify(wrongVersion), "utf8");
  const stale = convertProject(project, options);
  assert.equal(stale.ok, true);
  assert.equal(stale.incremental.used, false);
});

test("a cache that cannot be written fails the build", (t) => {
  const dir = scratch("pb-scratch-", t);
  const generatedDir = path.join(dir, "generated");
  const cacheDir = path.join(dir, "build");
  const project = makeProject(dir, "FakeProject");
  // A directory where cache.json belongs: the write must fail loudly.
  mkdirSync(path.join(cacheDir, "cache.json"), { recursive: true });

  const result = convertProject(project, { generatedDir, cacheDir, version: "1.2.3" });
  assert.equal(result.ok, false);
  assert.equal(result.errors.length, 1);
  assert.match(result.errors[0].error, /cannot write build cache/);
});

test("removes IR files that no longer exist in the project", (t) => {
  const dir = scratch("pb-scratch-", t);
  const generatedDir = path.join(dir, "generated");
  const cacheDir = path.join(dir, "build");
  const project = makeProject(dir, "FakeProject");
  writeFileSync(path.join(project, "Data", "Map002.rxdata"), mapFileNode(2));
  const options = { generatedDir, cacheDir, version: "1.2.3" };

  assert.equal(convertProject(project, options).ok, true);
  assert.ok(existsSync(path.join(generatedDir, "maps", "map-002.json")));

  // Map002.rxdata disappears: its IR files must not survive the rebuild.
  rmSync(path.join(project, "Data", "Map002.rxdata"), { force: true });
  const second = convertProject(project, options);
  assert.equal(second.ok, true, JSON.stringify(second.errors));
  assert.ok(!existsSync(path.join(generatedDir, "maps", "map-002.json")));
  assert.ok(existsSync(path.join(generatedDir, "maps", "map-001.json")));
  const index = readJson(path.join(generatedDir, "maps", "index.json"));
  assert.equal(index.total, 1);
});

test("an unreadable PBS directory fails the conversion", (t) => {
  const dir = scratch("pb-scratch-", t);
  const project = makeProject(dir, "FakeProject");
  rmSync(path.join(project, "PBS"), { recursive: true, force: true });
  writeFileSync(path.join(project, "PBS"), "not a directory", "utf8");

  const result = convertProject(project, { generatedDir: path.join(dir, "generated") });
  assert.equal(result.ok, false);
  assert.ok(result.problems.unreadablePbs.length > 0, "PBS problem must be reported");
  assert.match(result.errors[0].error, /PBS/);
});

test("buildTitleIr reads the active Modular Title Screen configuration (L1)", () => {
  const ir = buildTitleIr({
    sections: [
      {
        name: "Modular Title Screen",
        source: [
          "module ModularTitle",
          "  SPLASH_IMAGES      = ['intro1','pokefans games','origin']",
          "  SECONDS_PER_SPLASH = 5",
          "  BGM_VOLUME         = 20",
          "  MODIFIERS = [",
          "    # \"background:frlg\", \"bgm:title_frlg\"",
          "    \"background:bw\", \"overlay2\", \"logoY:172\", \"logo:shine\", \"bgm:title_hgss_0\"",
          "  ]",
          "end",
        ].join("\n"),
      },
      {
        name: "Version config",
        source: ["CURRENT_NAME = \"Test Game 2026\""].join("\n"),
      },
      {
        name: "Splash_Message",
        source: [
          "SPLASH_MESSAGE = [",
          "  \"hello\",",
          "  \"这次一定。\",",
          "]",
          "class Splash_Message",
          "  cprtpos = [",
          "    [_INTL(\"{1}\", CURRENT_NAME), 0, 0, 0, @font_color, @shadow_color],",
          "    [_INTL(\"Rx Team. Do not pirate\"), Graphics.width, 0, 1, @font_color, @shadow_color]",
          "  ]",
          "end",
        ].join("\n"),
      },
      {
        name: "Scene_Intro",
        source: ["module IntroEventScene", "  FADE_TICKS = 8", "end"].join("\n"),
      },
    ],
  });
  assert.equal(ir.kind, "title");
  assert.equal(ir.source, "Modular Title Screen");
  assert.deepEqual(ir.splashImages, ["intro1", "pokefans games", "origin"]);
  assert.equal(ir.secondsPerSplash, 5);
  assert.equal(ir.bgmVolume, 20);
  assert.equal(ir.bgm, "title_hgss_0", "the commented preset must not win");
  // L14 fields
  assert.deepEqual(ir.modifiers, ["background:bw", "overlay2", "logoY:172", "logo:shine", "bgm:title_hgss_0"]);
  assert.equal(ir.footerLeft, "Test Game 2026");
  assert.equal(ir.footerRight, "Rx Team. Do not pirate");
  assert.deepEqual(ir.splashMessages, ["hello", "这次一定。"]);
  assert.equal(ir.fadeTicks, 8);
});

test("buildTitleIr returns null for a project without the plugin (L1)", () => {
  const ir = buildTitleIr({ sections: [{ name: "Main", source: "def pbMain\nend" }] });
  assert.equal(ir, null);
});
