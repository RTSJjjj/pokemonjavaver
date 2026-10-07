// R15 · Small Test Project fixture generator (project3 sections 56-58).
//
// Writes a tiny but complete RMXP project (two maps, NPC, door, transfer,
// text, choice, switches, variables, common event, BGM/SE slots) and converts
// it into generated/ runtime data plus the compiled script IR. The committed
// fixture under tests/fixture-project/ is the input the automated tests use:
// the Node tests converts a fresh copy and compares, the Java tests read the
// converted JSON (no Ruby / marshal reader needed on the runtime side).
//
// Usage:
//   node tests/fixture-project/generate.mjs            # rebuild the fixture in place
//   node tests/fixture-project/generate.mjs --dir <dir> # write into another dir
//
// Everything is deterministic: running it twice produces byte identical IR /
// map JSON (no timestamps, no absolute paths inside the data files except the
// documented project.json source block).

import { cpSync, mkdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { array, dump, hash, int, object, str, userdef, deflateText, nilValue } from "../../builder/tests/marshal-writer.js";
import { convertProject } from "../../tools/data-converter/index.js";
import { validateProject } from "../../builder/src/project.js";
import { compileBlocks, IR_VERSION } from "../../builder/src/script-compiler.js";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const BUILDER_ROOT = path.resolve(HERE, "..", "..");

// ---------------------------------------------------------------------------
// RGSS Table payloads (same layout the converter decodes).
// ---------------------------------------------------------------------------

function tableBytes(dim0, dim1, dim2, values) {
  const total = dim0 * dim1 * dim2;
  const bytes = Buffer.alloc(20 + total * 2);
  bytes.writeUInt32LE(dim0, 0);
  bytes.writeUInt32LE(dim1, 4);
  bytes.writeUInt32LE(dim2, 8);
  bytes.writeUInt32LE(dim0, 12);
  bytes.writeUInt32LE(total, 16);
  for (let i = 0; i < total; i++) {
    bytes.writeUInt16LE(values[i] === undefined ? 0 : values[i], 20 + i * 2);
  }
  return bytes;
}

function cyclingTable(dim0, dim1, dim2, values) {
  return tableBytes(dim0, dim1, dim2, Array.from({ length: dim0 * dim1 * dim2 }, (_, i) => values[i % values.length]));
}

// ---------------------------------------------------------------------------
// Map data
// ---------------------------------------------------------------------------

const MAP_WIDTH = 17;
const MAP_HEIGHT = 13;
const WALL_TILE = 2; // tileset passages: tile 0 walkable, tile 2 blocked

/** Three ground layers; `wallAt` places the blocking tile on layer 0. */
function mapDataTable(wallAt) {
  const total = MAP_WIDTH * MAP_HEIGHT * 3;
  const values = new Array(total).fill(0);
  for (const [x, y] of wallAt) {
    values[x + MAP_WIDTH * (y + MAP_HEIGHT * 0)] = WALL_TILE;
  }
  // RGSS Table header: dimension count, xsize, ysize, zsize, total. A map data
  // table is (3, width, height); the helper takes (dimension, xsize, ysize).
  return userdef("Table", tableBytes(3, MAP_WIDTH, MAP_HEIGHT, values));
}

function eventCommand(code, indent, parameters) {
  return object("RPG::EventCommand", {
    "@code": int(code),
    "@indent": int(indent),
    "@parameters": parameters === null ? array() : array(...parameters),
  });
}

function eventPage(trigger, commands, graphicName = "") {
  return object("RPG::Event::Page", {
    "@trigger": int(trigger),
    "@graphic": object("RPG::Event::Page::Graphic", {
      "@character_name": str(graphicName),
      "@character_hue": int(0),
      "@direction": int(2),
      "@pattern": int(0),
      "@opacity": int(255),
      "@blend_type": int(0),
      "@tile_id": int(0),
    }),
    "@list": array(...commands),
  });
}

function mapEvent(id, name, x, y, pages) {
  return object("RPG::Event", {
    "@id": int(id),
    "@name": str(name),
    "@x": int(x),
    "@y": int(y),
    "@pages": array(...pages),
  });
}

export function fixtureMap001() {
  return object("RPG::Map", {
    "@tileset_id": int(1),
    "@width": int(MAP_WIDTH),
    "@height": int(MAP_HEIGHT),
    "@data": mapDataTable([[8, 6], [8, 7], [8, 8]]),
    "@events": hash(
      [int(1), mapEvent(1, "NPC", 7, 5, [
        eventPage(0, [
          eventCommand(101, 0, [str("Hello from the fixture!")]),
          eventCommand(121, 0, [int(1), int(1), int(0)]), // SW1 = ON
          eventCommand(122, 0, [int(1), int(1), int(0), int(0), int(5)]), // VAR1 = 5
        ], "NPC"),
      ])],
      [int(2), mapEvent(2, "Door", 5, 3, [
        eventPage(1, [
          eventCommand(201, 0, [int(0), int(2), int(10), int(7), int(0), int(0)]),
        ]),
      ])],
      [int(3), mapEvent(3, "Chooser", 11, 5, [
        eventPage(0, [
          eventCommand(102, 0, [array(str("Yes"), str("No")), int(0)]),
          eventCommand(402, 0, [int(1), str("No")]),
          eventCommand(122, 1, [int(1), int(1), int(0), int(0), int(9)]), // VAR1 = 9 (second option)
          eventCommand(402, 0, [int(0), str("Yes")]),
          eventCommand(121, 1, [int(2), int(1), int(0)]), // SW2 = ON (first option)
          eventCommand(404, 0, null),
        ]),
      ])],
      [int(4), mapEvent(4, "CallCommon", 13, 5, [
        eventPage(0, [
          eventCommand(117, 0, [int(1)]), // Call Common Event 1 (RMXP code 117)
        ]),
      ])],
      [int(5), mapEvent(5, "ItemGiver", 3, 5, [
        eventPage(0, [
          eventCommand(355, 0, [str("pbItemBall(:POTION, 3)")]),
          eventCommand(655, 0, [str("")]),
        ]),
      ])],
      [int(6), mapEvent(6, "UnknownMagic", 15, 5, [
        eventPage(0, [
          eventCommand(355, 0, [str("some_unknown_ruby_magic(42)")]),
          eventCommand(655, 0, [str("")]),
        ]),
      ])],
    ),
  });
}

export function fixtureMap002() {
  return object("RPG::Map", {
    "@tileset_id": int(1),
    "@width": int(MAP_WIDTH),
    "@height": int(MAP_HEIGHT),
    "@data": mapDataTable([]),
    "@events": hash(
      [int(1), mapEvent(1, "BackDoor", 10, 9, [
        eventPage(1, [
          eventCommand(201, 0, [int(0), int(1), int(5), int(4), int(0), int(0)]),
        ]),
      ])],
    ),
  });
}

// ---------------------------------------------------------------------------
// Project files
// ---------------------------------------------------------------------------

function silentWav(seconds, sampleRate = 8000) {
  const samples = Math.max(1, Math.floor(seconds * sampleRate));
  const data = Buffer.alloc(44 + samples * 2);
  data.write("RIFF", 0, "ascii");
  data.writeUInt32LE(36 + samples * 2, 4);
  data.write("WAVE", 8, "ascii");
  data.write("fmt ", 12, "ascii");
  data.writeUInt32LE(16, 16);
  data.writeUInt16LE(1, 20); // PCM
  data.writeUInt16LE(1, 22); // mono
  data.writeUInt32LE(sampleRate, 24);
  data.writeUInt32LE(sampleRate * 2, 28);
  data.writeUInt16LE(2, 32);
  data.writeUInt16LE(16, 34);
  data.write("data", 36, "ascii");
  data.writeUInt32LE(samples * 2, 40);
  return data;
}

const ONE_PIXEL_PNG = Buffer.from(
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
  "base64",
);

/** Writes the RMXP project files (Data/, PBS/, Graphics/, Audio/) into targetDir. */
export function writeFixtureProject(targetDir) {
  mkdirSync(path.join(targetDir, "Data"), { recursive: true });
  mkdirSync(path.join(targetDir, "PBS"), { recursive: true });
  mkdirSync(path.join(targetDir, "Graphics", "Tilesets"), { recursive: true });
  mkdirSync(path.join(targetDir, "Audio", "BGM"), { recursive: true });
  mkdirSync(path.join(targetDir, "Audio", "SE"), { recursive: true });

  const write = (relative, bytes) => writeFileSync(path.join(targetDir, relative), bytes);
  write("Game.rxproj", Buffer.from("", "utf8"));
  write("PBS/metadata.txt", Buffer.from("[001]\nName = Fixture Map 1\nOutdoor = true\n", "utf8"));
  write("Graphics/Tilesets/Fixture Tiles.png", ONE_PIXEL_PNG);
  write("Audio/BGM/FixtureTheme.wav", silentWav(0.1));
  write("Audio/SE/FixtureSE.wav", silentWav(0.05));

  write("Data/Scripts.rxdata", dump(
    array(
      array(int(0), str("================"), str(deflateText(""))),
      array(int(1), str("Main"), str(deflateText("def pbMain\n  pbSet(1, 1)\nend\n"))),
    ),
  ));
  write("Data/MapInfos.rxdata", dump(hash(
    [int(1), object("RPG::MapInfo", {
      "@name": str("Fixture Map 1"),
      "@order": int(1),
      "@parent_id": int(0),
    })],
    [int(2), object("RPG::MapInfo", {
      "@name": str("Fixture Map 2"),
      "@order": int(2),
      "@parent_id": int(0),
    })],
  )));
  write("Data/Map001.rxdata", dump(fixtureMap001()));
  write("Data/Map002.rxdata", dump(fixtureMap002()));
  write("Data/Tilesets.rxdata", dump(
    array(
      nilValue(),
      object("RPG::Tileset", {
        "@id": int(1),
        "@name": str("Fixture Tiles"),
        "@tileset_name": str("Fixture Tiles"),
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
        "@passages": userdef("Table", cyclingTable(1, 6624, 1, [0, 0, 0x0f, 0x0f])),
        "@priorities": userdef("Table", cyclingTable(1, 6624, 1, [0, 1, 1])),
        "@terrain_tags": userdef("Table", cyclingTable(1, 6624, 1, [0, 2, 0])),
      }),
    ),
  ));
  write("Data/System.rxdata", dump(
    object("RPG::System", {
      "@magic_number": int(16522614),
      "@start_map_id": int(1),
      "@start_x": int(5),
      "@start_y": int(5),
      "@edit_map_id": int(1),
      "@windowskin_name": str(""),
      "@title_name": str(""),
      "@gameover_name": str(""),
      "@battleback_name": str(""),
      "@elements": array(str(""), str("")),
      "@switches": array(str(""), str("SW1"), str("SW2")),
      "@variables": array(str(""), str("VAR1"), str("VAR2")),
      "@words": object("RPG::System::Words", { "@hp": str("HP"), "@attack": str("ATK") }),
    }),
  ));
  write("Data/CommonEvents.rxdata", dump(array(
    nilValue(),
    object("RPG::CommonEvent", {
      "@id": int(1),
      "@name": str("Fixture Common"),
      "@trigger": int(0),
      "@switch_id": int(0),
      "@list": array(
        eventCommand(122, 0, [int(1), int(1), int(0), int(0), int(7)]), // VAR1 = 7
      ),
    }),
  )));
  return targetDir;
}

/**
 * Converts the fixture project into <targetDir>/generated and compiles the
 * script IR. Returns the converter result.
 */
export function buildFixtureGenerated(targetDir) {
  const projectDir = path.join(targetDir, "project");
  writeFixtureProject(projectDir);
  const generatedDir = path.join(targetDir, "generated");
  const result = convertProject(projectDir, {
    generatedDir,
    builderRoot: BUILDER_ROOT,
    version: "0.1.0",
    projectName: "FixtureProject",
    validation: validateProject(projectDir),
    stepLabels: { scan: "1/2", convert: "2/2" },
    useCache: false,
    cacheDir: path.join(targetDir, "build"),
    onStep: () => {},
  });
  if (!result.ok) {
    throw new Error("fixture conversion failed: " + JSON.stringify(result.errors));
  }
  const blocks = JSON.parse(readFileSync(path.join(generatedDir, "scripts", "blocks.json"), "utf8"));
  const { ir, coverage } = compileBlocks(blocks);
  writeFileSync(
    path.join(generatedDir, "scripts", "ir.json"),
    JSON.stringify({ version: IR_VERSION, commands: ir }, null, 1),
    "utf8",
  );
  return { result, coverage, generatedDir };
}

/**
 * The committed fixture must not carry machine specific paths (and the data
 * files must stay byte stable): rewrite the source blocks of project.json /
 * metadata to fixture relative placeholders.
 */
export function normalizeGenerated(generatedDir) {
  const placeholder = "tests/fixture-project/project";
  const projectFile = path.join(generatedDir, "project.json");
  const project = JSON.parse(readFileSync(projectFile, "utf8"));
  project.source = { project: placeholder, dataDir: placeholder + "/Data" };
  project.generatedAt = "fixture";
  writeFileSync(projectFile, JSON.stringify(project, null, 2), "utf8");

  const buildFile = path.join(generatedDir, "metadata", "build.json");
  const build = JSON.parse(readFileSync(buildFile, "utf8"));
  build.source = { project: placeholder, dataDir: placeholder + "/Data" };
  build.generatedAt = "fixture";
  writeFileSync(buildFile, JSON.stringify(build, null, 2), "utf8");

  const sourcesFile = path.join(generatedDir, "metadata", "sources.json");
  const sources = JSON.parse(readFileSync(sourcesFile, "utf8"));
  sources.dataDir = placeholder + "/Data";
  writeFileSync(sourcesFile, JSON.stringify(sources, null, 2), "utf8");
}

/** Rebuilds the committed fixture: the project files and generated/ next to it. */
export function rebuildCommittedFixture(fixtureDir = HERE) {
  const projectDir = path.join(fixtureDir, "project");
  writeFixtureProject(projectDir);
  const { result, coverage } = buildFixtureGenerated(path.join(fixtureDir, ".tmp-build"));
  // Move the freshly converted generated/ next to the fixture project; the
  // committed generated/ is what the Java tests read.
  const source = path.join(fixtureDir, ".tmp-build", "generated");
  const destination = path.join(fixtureDir, "generated");
  rmSync(destination, { recursive: true, force: true });
  cpSync(source, destination, { recursive: true });
  rmSync(path.join(fixtureDir, ".tmp-build"), { recursive: true, force: true });
  normalizeGenerated(destination);
  return { result, coverage, destination };
}

const invokedDirectly = process.argv[1] && path.resolve(process.argv[1]) === path.resolve(fileURLToPath(import.meta.url));
if (invokedDirectly) {
  const dirIndex = process.argv.indexOf("--dir");
  const target = dirIndex >= 0 ? path.resolve(process.argv[dirIndex + 1]) : HERE;
  if (target === HERE) {
    const { coverage, destination } = rebuildCommittedFixture();
    console.log(`fixture rebuilt: ${destination}`);
    console.log(`script blocks: ${coverage.blocks} translated=${coverage.translated} unsupported=${coverage.unsupported}`);
  } else {
    const { generatedDir, coverage } = buildFixtureGenerated(target);
    console.log(`fixture built: ${generatedDir}`);
    console.log(`script blocks: ${coverage.blocks} translated=${coverage.translated} unsupported=${coverage.unsupported}`);
  }
}
