// Debug IR converter (Phase 8).
//
// Turns the scanned / analysed project into the JSON Debug IR that the
// translator and the later build stages consume. The RMXP project stays an
// input only: nothing here writes into it.
//
//   generated/project.json                    manifest + counters
//   generated/maps/index.json                 one entry per MapXXX.rxdata
//   generated/maps/map-NNN.json               full map data (events, commands)
//   generated/events/index.json               which maps carry script blocks
//   generated/events/map-NNN.json             merged 355/655 Ruby blocks
//   generated/common-events/index.json        one entry per common event
//   generated/common-events/common-event-NNN.json
//   generated/scripts/sections.json           Scripts.rxdata sections + origin
//   generated/scripts/blocks.json             every event script block (classified)
//   generated/scripts/apis.json               API usage + unique argument patterns
//   generated/metadata/build.json             build manifest (version, timings)
//   generated/metadata/sources.json           source inventory (sha1 + bytes)
//   generated/metadata/problems.json          everything that could not be read
//
// Phase 13 adds the incremental build cache (see cache.js): after a successful
// conversion build/cache.json records the sha1 + mtime fingerprint of every
// unit input and the IR files each unit produced, so the next build can skip
// the units whose inputs and outputs are unchanged (Map001 untouched is not
// recompiled; only Map002 is rebuilt). IR files this build no longer owns are
// removed, and a cache that cannot be written fails the build.
//
// Phase 1 only requires JSON as a Debug IR; a binary format can replace these
// files later without changing the pipeline.

import { existsSync, mkdirSync, readdirSync, readFileSync, rmSync, statSync, writeFileSync } from "node:fs";
import path from "node:path";

import { marshalLoad, hashEntries } from "../../builder/src/marshal.js";
import { sha1 } from "../../builder/src/util.js";
import { validateProject } from "../../builder/src/project.js";
import { isInside } from "../../builder/src/config.js";
import { scanProject } from "../scanner/index.js";
import { analyzeProjectEvents } from "../event-analyzer/index.js";
import { readScriptSources } from "../scanner/index.js";
import { analyzeScriptUsage } from "../script-analyzer/index.js";
import { rubyText } from "../scanner/index.js";
import {
  CACHE_FORMAT,
  cacheFilePath,
  loadCache,
  saveCache,
  unitIsFresh,
} from "./cache.js";

// /3 (R6.23): every map IR gained the PBS/metadata.txt "SnapEdges" flag and
// the converter writes the PBS/connections.txt map links (Essentials
// MapFactory). The version bump also drops pre-existing build caches, so the
// first build after this change rewrites all maps instead of reusing IR
// without the new fields.
export const IR_FORMAT = "pokemon-builder/debug-ir/4";

const IR_SUBDIRS = ["maps", "events", "common-events", "scripts", "metadata"];

function pad(value) {
  return String(value).padStart(3, "0");
}

function audioIr(node) {
  if (!node || typeof node !== "object") return null;
  return {
    name: rubyText(node["@name"]),
    volume: typeof node["@volume"] === "number" ? node["@volume"] : null,
    pitch: typeof node["@pitch"] === "number" ? node["@pitch"] : null,
  };
}

// Converts a Marshal node into a plain JSON value: strings are decoded as
// UTF-8 (latin1 fallback), Ruby objects become { class, ...ivars } and the
// leading @ is dropped from instance variable names.
/**
 * RPG::Tone and RPG::Color are user defined Marshal objects (_dump payload in
 * __userdef__): four little endian doubles, so without decoding them the IR
 * only carried { class: "RPG::Tone" } and the runtime could not apply the
 * screen tone / flash colour of commands 223, 224 and 234.
 *
 * Returns null when the payload is not a plausible 32 byte RGSS colour, so a
 * wrong guess can never pollute the IR (the caller falls back to { class }).
 */
export function decodeRgssColorLike(node) {
  if (!node || typeof node !== "object") return null;
  const className = node.__class__;
  const bytes = node.__userdef__;
  if (!bytes || bytes.length < 32) return null;
  // The Marshal reader yields the symbol exactly as the file stores it, which
  // is "Tone" / "Color" for these two classes (other objects keep the RPG::
  // prefix), so both spellings are accepted.
  const short = String(className).replace(/^RPG::/, "");
  if (short !== "Tone" && short !== "Color") return null;
  const names = short === "Tone"
    ? ["red", "green", "blue", "gray"]
    : ["red", "green", "blue", "alpha"];
  const out = { class: className };
  for (let i = 0; i < 4; i++) {
    const raw = bytes.readDoubleLE(i * 8);
    if (!Number.isFinite(raw) || Math.abs(raw) > 1000) return null;
    out[names[i]] = Math.round(raw * 1000) / 1000;
  }
  return out;
}

function irValue(value) {
  if (value === null || value === undefined) return null;
  if (typeof value === "string") return rubyText(value);
  if (Array.isArray(value)) return value.map(irValue);
  if (typeof value === "object") {
    const decoded = decodeRgssColorLike(value);
    if (decoded) return decoded;
    const className = value.__class__;
    const out = className ? { class: className } : {};
    for (const [key, item] of Object.entries(value)) {
      if (key === "__class__" || key === "__userdef__") continue;
      const name = key.startsWith("@") ? key.slice(1) : key;
      out[name] = irValue(item);
    }
    return out;
  }
  return value;
}

// The RPG::Map tile table is a user defined Marshal object (raw bytes in this
// reader). It is huge and byte level, so the IR only records that it exists.
// RGSS Table wire format (the Marshal "u" userdef payload of RPG::Table),
// verified against every table of this project (522 maps + 150 tileset
// tables, zero violations):
//   uint32 dim0, uint32 dim1, uint32 dim2, uint32 dim0 (repeated),
//   uint32 totalElementCount  (= dim0 * dim1 * dim2)
//   totalElementCount x uint16 little endian, iterated dim0, then dim1, dim2
// Map tile data uses dim0=3 (ground / middle / top layer), dim1=width,
// dim2=height; tileset tables use dim0=1, dim1=count, dim2=1. Decoding the
// real layout (instead of only recording its presence) is what lets the Java
// runtime render maps and do passability lookups without touching the source
// project (project3 sections 9-10).
export function decodeTable(node) {
  if (!node || typeof node !== "object" || !node.__userdef__) {
    return { present: false };
  }
  const bytes = node.__userdef__;
  if (bytes.length < 20) {
    return {
      present: true,
      error: "table payload too small: " + bytes.length + " bytes",
    };
  }
  const dim0 = bytes.readUInt32LE(0);
  const dim1 = bytes.readUInt32LE(4);
  const dim2 = bytes.readUInt32LE(8);
  const repeated = bytes.readUInt32LE(12);
  const total = bytes.readUInt32LE(16);
  if (dim0 <= 0 || dim1 <= 0 || dim2 <= 0 || dim0 * dim1 * dim2 !== total) {
    return {
      present: true,
      error:
        "table dims " + dim0 + "x" + dim1 + "x" + dim2 + " do not match element count " + total,
      dims: { dim0, dim1, dim2 },
      total,
    };
  }
  if (repeated !== dim0) {
    return {
      present: true,
      error: "table header is inconsistent (dim0 " + dim0 + " repeated as " + repeated + ")",
      dims: { dim0, dim1, dim2 },
      total,
    };
  }
  const expected = 20 + total * 2;
  if (bytes.length !== expected) {
    return {
      present: true,
      error: "table payload length " + bytes.length + " bytes, expected " + expected,
      dims: { dim0, dim1, dim2 },
      total,
    };
  }
  // one array per dim0 layer; within a layer the index is row * dim2 + col
  // with dim2 the inner dimension (this matches the RGSS dump iteration
  // order), so layer0 is the ground layer of a map.
  const perLayer = total / dim0;
  const layers = [];
  for (let layer = 0; layer < dim0; layer++) {
    const values = new Array(perLayer);
    for (let i = 0; i < perLayer; i++) {
      values[i] = bytes.readUInt16LE(20 + (layer * perLayer + i) * 2);
    }
    layers.push(values);
  }
  return { present: true, z: dim0, x: dim1, y: dim2, total, layers };
}

/**
 * R6.27: the generic RGSS Table header. {@code decodeTable} above labels the
 * first field "z" because the map and tileset tables are one-dimensional
 * ({@code dimensions == 1}, so everything lands in layer 0); the animation
 * cell tables are 2D ({@code dimensions == 2, xsize == cellMax, ysize == 8}),
 * which that layout cannot express. Values are stored with x fastest:
 * {@code value(x, y) = values[x + xsize * y]}.
 */
export function decodeTableRaw(node) {
  if (!node || typeof node !== "object" || !node.__userdef__) {
    return null;
  }
  const bytes = node.__userdef__;
  if (bytes.length < 20) {
    return null;
  }
  const dimensions = bytes.readUInt32LE(0);
  const xsize = bytes.readUInt32LE(4);
  const ysize = bytes.readUInt32LE(8);
  const zsize = bytes.readUInt32LE(12);
  const total = bytes.readUInt32LE(16);
  if (xsize <= 0 || ysize <= 0 || zsize <= 0 || total !== xsize * ysize * zsize) {
    return null;
  }
  if (bytes.length < 20 + total * 2) {
    return null;
  }
  const values = new Array(total);
  for (let i = 0; i < total; i++) {
    values[i] = bytes.readInt16LE(20 + i * 2);
  }
  return { dimensions, xsize, ysize, zsize, total, values };
}

/** RGSS Color userdef: four little-endian doubles (r, g, b, a). */
function colorUserdef(node, fallback) {
  const bytes = node && node.__userdef__;
  if (!bytes || bytes.length < 32) {
    return fallback;
  }
  return [Math.round(bytes.readDoubleLE(0)), Math.round(bytes.readDoubleLE(8)),
    Math.round(bytes.readDoubleLE(16)), Math.round(bytes.readDoubleLE(24))];
}

/**
 * R6.27: one RPG::Animation as the runtime plays it (Show Animation, 207).
 * Frames hold their cells in canvas coordinates; a cell's graphic index is the
 * sheet column/row (5 columns in RMXP, some of this project's sheets are
 * packed differently - the runtime derives the column count from the texture).
 */
function animationIr(animation) {
  const cellValue = (values, cellMax, cell, attribute) => values[cell + cellMax * attribute];
  const frames = [];
  for (const frame of animation["@frames"] || []) {
    const cellMax = typeof frame["@cell_max"] === "number" ? frame["@cell_max"] : 0;
    const table = decodeTableRaw(frame["@cell_data"]);
    const cells = [];
    if (table && table.xsize === cellMax && table.ysize === 8) {
      for (let cell = 0; cell < cellMax; cell++) {
        cells.push({
          cell: cellValue(table.values, cellMax, cell, 0),
          x: cellValue(table.values, cellMax, cell, 1),
          y: cellValue(table.values, cellMax, cell, 2),
          zoom: cellValue(table.values, cellMax, cell, 3),
          angle: cellValue(table.values, cellMax, cell, 4),
          flip: cellValue(table.values, cellMax, cell, 5) !== 0,
          opacity: cellValue(table.values, cellMax, cell, 6),
          blend: cellValue(table.values, cellMax, cell, 7),
        });
      }
    }
    frames.push(cells);
  }
  const timings = [];
  for (const timing of animation["@timings"] || []) {
    const se = timing["@se"];
    timings.push({
      frame: numOrNull(timing["@frame"]) || 0,
      condition: numOrNull(timing["@condition"]) || 0,
      scope: numOrNull(timing["@flash_scope"]) || 0,
      flashDuration: numOrNull(timing["@flash_duration"]) || 0,
      color: colorUserdef(timing["@flash_color"], [255, 255, 255, 255]),
      se: se && typeof se["@name"] === "string" && se["@name"] !== ""
        ? { name: se["@name"], volume: numOrNull(se["@volume"]) || 100,
            pitch: numOrNull(se["@pitch"]) || 100 }
        : null,
    });
  }
  return {
    id: numOrNull(animation["@id"]),
    name: typeof animation["@name"] === "string" ? animation["@name"] : "",
    graphic: typeof animation["@animation_name"] === "string" ? animation["@animation_name"] : "",
    hue: numOrNull(animation["@animation_hue"]) || 0,
    position: numOrNull(animation["@position"]) || 0,
    frameMax: numOrNull(animation["@frame_max"]) || 0,
    frames,
    timings,
  };
}

function tileDataIr(node) {
  return decodeTable(node);
}

function numOrNull(value) {
  return typeof value === "number" ? value : null;
}

// Tileset IR (project3 sections 9-12): names plus the decoded passability,
// priority and terrain tables the runtime needs for collision and rendering.
function buildTilesetIr(entry) {
  return {
    id: numOrNull(entry["@id"]),
    name: rubyText(entry["@name"]),
    tilesetName: rubyText(entry["@tileset_name"]),
    autotileNames: Array.isArray(entry["@autotile_names"])
      ? entry["@autotile_names"].map((name) => rubyText(name))
      : [],
    panoramaName: rubyText(entry["@panorama_name"]),
    panoramaHue: numOrNull(entry["@panorama_hue"]),
    fogName: rubyText(entry["@fog_name"]),
    fogHue: numOrNull(entry["@fog_hue"]),
    fogOpacity: numOrNull(entry["@fog_opacity"]),
    fogBlendType: numOrNull(entry["@fog_blend_type"]),
    fogZoom: numOrNull(entry["@fog_zoom"]),
    fogSx: numOrNull(entry["@fog_sx"]),
    fogSy: numOrNull(entry["@fog_sy"]),
    battlebackName: rubyText(entry["@battleback_name"]),
    passages: decodeTable(entry["@passages"]),
    priorities: decodeTable(entry["@priorities"]),
    terrainTags: decodeTable(entry["@terrain_tags"]),
  };
}

// System IR (project3 section 9): the runtime start position and the display
// names the game data refers to (elements, switches, variables, words).
function buildSystemIr(system) {
  // RPG::System::Words is a plain object of ivars; the reader also returns
  // hashes as Maps when the keys are not plain strings. Emit the words as a
  // flat [key, text] list either way.
  const wordPairs = [];
  const words = system["@words"];
  const wordEntries = words instanceof Map ? hashEntries(words) : words && typeof words === "object" ? Object.entries(words) : [];
  for (const [key, value] of wordEntries) {
    if (key === "__class__") continue;
    wordPairs.push([String(key).replace(/^@/, ""), rubyText(value)]);
  }
  return {
    format: IR_FORMAT,
    kind: "system",
    magicNumber: numOrNull(system["@magic_number"]),
    startMapId: numOrNull(system["@start_map_id"]),
    startX: numOrNull(system["@start_x"]),
    startY: numOrNull(system["@start_y"]),
    editMapId: numOrNull(system["@edit_map_id"]),
    windowskinName: rubyText(system["@windowskin_name"]),
    titleName: rubyText(system["@title_name"]),
    gameoverName: rubyText(system["@gameover_name"]),
    battlebackName: rubyText(system["@battleback_name"]),
    elements: Array.isArray(system["@elements"]) ? system["@elements"].map((item) => rubyText(item)) : [],
    switches: Array.isArray(system["@switches"]) ? system["@switches"].map((item) => rubyText(item)) : [],
    variables: Array.isArray(system["@variables"]) ? system["@variables"].map((item) => rubyText(item)) : [],
    words: wordPairs,
  };
}

function conditionIr(node) {
  if (!node || typeof node !== "object") return null;
  const plain = irValue(node);
  return {
    selfSwitchCh: plain.self_switch_ch === undefined ? null : plain.self_switch_ch,
    selfSwitchValid: Boolean(plain.self_switch_valid),
    switch1Valid: Boolean(plain.switch1_valid),
    switch1Id: plain.switch1_id === undefined ? null : plain.switch1_id,
    switch2Valid: Boolean(plain.switch2_valid),
    switch2Id: plain.switch2_id === undefined ? null : plain.switch2_id,
    variableValid: Boolean(plain.variable_valid),
    variableId: plain.variable_id === undefined ? null : plain.variable_id,
    variableValue: plain.variable_value === undefined ? null : plain.variable_value,
  };
}

function graphicIr(page) {
  const node = page["@graphic"];
  if (node && typeof node === "object") {
    const plain = irValue(node);
    return {
      characterName: plain.character_name === undefined ? "" : plain.character_name,
      characterHue: plain.character_hue === undefined ? 0 : plain.character_hue,
      direction: plain.direction === undefined ? 2 : plain.direction,
      pattern: plain.pattern === undefined ? 0 : plain.pattern,
      tileId: plain.tile_id === undefined ? 0 : plain.tile_id,
      opacity: plain.opacity === undefined ? 255 : plain.opacity,
      blendType: plain.blend_type === undefined ? 0 : plain.blend_type,
    };
  }
  return {
    characterName: rubyText(page["@character_name"]),
    characterHue: typeof page["@character_hue"] === "number" ? page["@character_hue"] : 0,
    direction: typeof page["@direction"] === "number" ? page["@direction"] : 2,
    pattern: typeof page["@pattern"] === "number" ? page["@pattern"] : 0,
    tileId: 0,
    opacity: 255,
    blendType: 0,
  };
}

function moveRouteIr(node) {
  if (!node || typeof node !== "object") return null;
  const plain = irValue(node);
  const list = Array.isArray(plain.list) ? plain.list : [];
  return {
    repeat: Boolean(plain.repeat),
    skippable: Boolean(plain.skippable),
    commands: list.map((command) => ({
      code: command.code === undefined ? null : command.code,
      parameters: command.parameters === undefined ? [] : command.parameters,
    })),
  };
}

// Full event command list of one page, in editor order. The 355/655 script
// commands stay in place (the translator must see where they were) and carry
// the id of the merged Ruby block so the split view stays linked.
function commandsIr(list, blockIds) {
  if (!Array.isArray(list)) return [];
  return list.map((command, index) => ({
    index,
    code: typeof command["@code"] === "number" ? command["@code"] : null,
    indent: typeof command["@indent"] === "number" ? command["@indent"] : 0,
    parameters: Array.isArray(command["@parameters"]) ? irValue(command["@parameters"]) : [],
    scriptBlockId: blockIds.has(index) ? blockIds.get(index) : null,
  }));
}

function pageIr(page, pageIndex, blockIds) {
  return {
    page: pageIndex + 1,
    trigger: typeof page["@trigger"] === "number" ? page["@trigger"] : null,
    conditions: conditionIr(page["@condition"]),
    graphic: graphicIr(page),
    movement: {
      moveType: typeof page["@move_type"] === "number" ? page["@move_type"] : null,
      moveSpeed: typeof page["@move_speed"] === "number" ? page["@move_speed"] : null,
      moveFrequency: typeof page["@move_frequency"] === "number" ? page["@move_frequency"] : null,
      moveRoute: moveRouteIr(page["@move_route"]),
      through: Boolean(page["@through"]),
      alwaysOnTop: Boolean(page["@always_on_top"]),
      walkAnime: Boolean(page["@walk_anime"]),
      stepAnime: Boolean(page["@step_anime"]),
      directionFix: Boolean(page["@direction_fix"]),
    },
    commands: commandsIr(page["@list"], blockIds),
  };
}

// A merged Ruby block covers its own command index plus every 655 continuation
// that followed it, so the IR links every covered command to the block.
function blockSpan(block) {
  const start = block.commandIndex;
  if (!Number.isInteger(start)) return 0;
  return 1 + (Number.isInteger(block.continuations) ? block.continuations : 0);
}

// L6d: block ids are scoped to their page. Keyed by command index alone, a
// page-1 script leaked onto the same-index command of every later page (a 509
// move payload inherited pbCaveEntrance and replayed the whole cave animation
// on the walk-out page). `pageNumber` is omitted for common events, which
// have no pages.
export function blockIndex(blocks, pageNumber) {
  const index = new Map();
  for (const block of blocks) {
    if (Number.isInteger(pageNumber) && Number.isInteger(block.page)
        && block.page !== pageNumber) {
      continue;
    }
    const span = blockSpan(block);
    for (let offset = 0; offset < span; offset++) {
      index.set(block.commandIndex + offset, block.id);
    }
  }
  return index;
}

function eventIr(event, mapId, mapName, blocks) {
  const pages = Array.isArray(event["@pages"]) ? event["@pages"] : [];
  const own = blocks.filter((block) => block.eventId === event["@id"]);
  return {
    id: typeof event["@id"] === "number" ? event["@id"] : null,
    name: rubyText(event["@name"]),
    x: typeof event["@x"] === "number" ? event["@x"] : null,
    y: typeof event["@y"] === "number" ? event["@y"] : null,
    pages: pages.map((page, pageIndex) =>
      pageIr(page, pageIndex, blockIndex(own, pageIndex + 1))),
    scriptBlockIds: own.map((block) => block.id),
  };
}

// Maps arrive as Marshal hashes { eventId => RPG::Event }; RMXP pads them with
// nil, so drop the empty slots and sort by numeric id (stable reports).
function eventEntriesOf(data) {
  const entries = hashEntries(data && data["@events"]);
  const usable = entries.filter((entry) => entry[1] && typeof entry[1] === "object");
  usable.sort((a, b) => {
    const left = typeof a[0] === "number" ? a[0] : Number.MAX_SAFE_INTEGER;
    const right = typeof b[0] === "number" ? b[0] : Number.MAX_SAFE_INTEGER;
    return left - right;
  });
  return usable;
}

export function buildMapIr({ file, mapId, mapName, data, blocks, outdoor = false, snapEdges = false }) {
  const plainEncounters = Array.isArray(data["@encounter_list"]) ? data["@encounter_list"] : [];
  return {
    format: IR_FORMAT,
    kind: "map",
    mapId,
    name: mapName,
    file,
    // R6.16: PBDayNight only shades maps whose PBS/metadata.txt says
    // "Outdoor = true" (pbDayNightTint checks MetadataOutdoor), so the runtime
    // needs the same per-map flag to keep indoor maps free of the night tint.
    outdoor: outdoor === true,
    // R6.23: Game_Map#display_x= clamps the camera only for maps whose
    // PBS/metadata.txt says "SnapEdges = true" (the default is false), which is
    // also what makes Essentials map connections scroll seamlessly.
    snapEdges: snapEdges === true,
    width: typeof data["@width"] === "number" ? data["@width"] : null,
    height: typeof data["@height"] === "number" ? data["@height"] : null,
    tilesetId: typeof data["@tileset_id"] === "number" ? data["@tileset_id"] : null,
    encounterStep: typeof data["@encounter_step"] === "number" ? data["@encounter_step"] : null,
    bgm: audioIr(data["@bgm"]),
    bgs: audioIr(data["@bgs"]),
    autoplayBgm: Boolean(data["@autoplay_bgm"]),
    autoplayBgs: Boolean(data["@autoplay_bgs"]),
    encounters: plainEncounters.map((entry) => {
      const plain = irValue(entry);
      return {
        troopId: plain.troop_id === undefined ? null : plain.troop_id,
        weight: plain.weight === undefined ? null : plain.weight,
        regions: Array.isArray(plain.regions) ? plain.regions : [],
      };
    }),
    tileData: tileDataIr(data["@data"]),
    events: eventEntriesOf(data).map(([eventId, event]) => eventIr(event, mapId, mapName, blocks)),
    scriptBlockIds: blocks.map((block) => block.id),
  };
}

// Common event blocks carry the common event id in eventId; the event analyzer
// has no page concept for them.
export function ownBlocksForCommonEvent(blocks, commonEventId) {
  return blocks.filter(
    (block) => block.source === "commonEvent" && block.eventId === commonEventId,
  );
}

export function buildCommonEventIr(event, slot, blocks) {
  const commonEventId = typeof event["@id"] === "number" ? event["@id"] : slot + 1;
  const own = ownBlocksForCommonEvent(blocks, commonEventId);
  const blockIds = blockIndex(own);
  return {
    format: IR_FORMAT,
    kind: "commonEvent",
    id: commonEventId,
    slot,
    name: rubyText(event["@name"]),
    trigger: typeof event["@trigger"] === "number" ? event["@trigger"] : null,
    switchId: typeof event["@switch_id"] === "number" ? event["@switch_id"] : null,
    commands: Array.isArray(event["@list"])
      ? commandsIr(event["@list"], blockIds)
      : [],
    scriptBlockIds: own.map((block) => block.id),
  };
}

// PBS inventory: name, size, sha1 and mtime. A file that cannot be read
// or stat-ed is reported in `problems` (never silently skipped): the
// caller turns those into a hard failure (Phase 13 error handling).
/**
 * Runtime profile (R6.12): the handful of project-specific knobs the Java
 * runtime would otherwise hardcode - the player's walking / running charsets
 * (PBS/metadata.txt "PlayerA"), the message font (Fonts/), and the screen size
 * the project configures (SCREEN_WIDTH / SCREEN_HEIGHT in its Settings script).
 * Exported into project.json so pointing the Builder at another project never
 * needs runtime edits: build-data + build-audio + script IR are enough.
 */
function runtimeProfile(projectPath, scriptSources) {
  const profile = {};
  try {
    const metadata = path.join(projectPath, "PBS", "metadata.txt");
    if (existsSync(metadata)) {
      const line = readFileSync(metadata, "utf8").split(/\r?\n/)
        .find((entry) => /^PlayerA\s*=/.test(entry.trim()));
      if (line) {
        const fields = line.slice(line.indexOf("=") + 1).split(",").map((field) => field.trim());
        if (fields[1]) profile.playerCharset = fields[1];
        if (fields[4]) profile.runningCharset = fields[4];
      }
    }
  } catch (error) {
    // The profile is optional: a project without PBS/metadata.txt keeps the
    // runtime defaults.
  }
  try {
    const fontsDir = path.join(projectPath, "Fonts");
    if (existsSync(fontsDir)) {
      const fonts = readdirSync(fontsDir)
        .filter((name) => name.toLowerCase().endsWith(".ttf")).sort();
      if (fonts.length > 0) profile.messageFont = fonts[0];
    }
  } catch (error) {
    // optional
  }
  try {
    const settings = (scriptSources && scriptSources.sections ? scriptSources.sections : [])
      .map((section) => section.source || "").join("\n");
    const width = settings.match(/SCREEN_WIDTH\s*=\s*(\d+)/);
    const height = settings.match(/SCREEN_HEIGHT\s*=\s*(\d+)/);
    if (width) profile.screenWidth = Number(width[1]);
    if (height) profile.screenHeight = Number(height[1]);
  } catch (error) {
    // optional
  }
  return profile;
}
/**
 * L1: Modular Title Screen configuration (the project ships the plugin section
 * "Modular Title Screen"). The runtime replays the configured splash slides and
 * uses the BGM token from the active MODIFIERS line; commented preset lines are
 * ignored. Returns null when the project has no such section, so projects
 * without the plugin keep their generated tree unchanged.
 */
export function buildTitleIr(scriptSources) {
  const sections = scriptSources && scriptSources.sections ? scriptSources.sections : [];
  const section = sections.find((entry) => /module\s+ModularTitle\b/.test(entry.source || ""));
  if (!section) return null;
  // Drop comment lines first: the MODIFIERS presets above the active line are
  // commented out but still contain bgm: tokens.
  const active = (section.source || "")
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => !line.startsWith("#"))
    .join("\n");
  const ir = {
    format: IR_FORMAT,
    kind: "title",
    source: section.name,
    splashImages: [],
    secondsPerSplash: 0,
    bgm: "",
    bgmVolume: 100,
    // L14: the original visual system read from the same scripts.
    modifiers: [],
    footerLeft: "",
    footerRight: "",
    splashMessages: [],
    fadeTicks: 8,
  };
  const splash = /SPLASH_IMAGES\s*=\s*\[([\s\S]*?)\]/.exec(active);
  if (splash) {
    ir.splashImages = [...splash[1].matchAll(/['"]([^'"]+)['"]/g)].map((match) => match[1]);
  }
  const seconds = /SECONDS_PER_SPLASH\s*=\s*([0-9]+(?:\.[0-9]+)?)/.exec(active);
  if (seconds) ir.secondsPerSplash = Number(seconds[1]);
  const volume = /BGM_VOLUME\s*=\s*([0-9]+)/.exec(active);
  if (volume) ir.bgmVolume = Number(volume[1]);
  const bgm = /['"]bgm:([^'"]+)['"]/.exec(active);
  if (bgm) ir.bgm = bgm[1];
  // Active MODIFIERS entries (the visual recipe: background/overlay/logo/
  // effects/misc + bgm token).
  const modifiers = /MODIFIERS\s*=\s*\[([\s\S]*?)\]/.exec(active);
  if (modifiers) {
    ir.modifiers = [...modifiers[1].matchAll(/['"]([^'"]+)['"]/g)].map((match) => match[1]);
  }
  // Footer bar: version name (left) + copyright (right) from the plugin's own
  // sections; the rotating splash messages and the fade length likewise.
  const versionSection = sections.find((entry) => /CURRENT_NAME\s*=/.test(entry.source || ""));
  if (versionSection) {
    const name = /CURRENT_NAME\s*=\s*["']([^"']*)["']/.exec(versionSection.source || "");
    if (name) ir.footerLeft = name[1];
  }
  const splashSection = sections.find((entry) => /class\s+Splash_Message\b/.test(entry.source || ""));
  if (splashSection) {
    const source = splashSection.source || "";
    const list = /SPLASH_MESSAGE\s*=\s*\[([\s\S]*?)\]/.exec(source);
    if (list) {
      ir.splashMessages = [...list[1].matchAll(/["']([^"']+)["']/g)].map((match) => match[1]);
    }
    const copyright = /\[\s*_INTL\(\s*"([^"]*)"\s*\)\s*,\s*Graphics\.width\s*,\s*0\s*,\s*1/.exec(source);
    if (copyright) ir.footerRight = copyright[1];
  }
  const introSection = sections.find((entry) => /FADE_TICKS\s*=/.test(entry.source || ""));
  if (introSection) {
    const ticks = /FADE_TICKS\s*=\s*(\d+)/.exec(introSection.source || "");
    if (ticks) ir.fadeTicks = Number(ticks[1]);
  }
  return ir;
}
/**
 * PBS/metadata.txt map flags (R6.16). The project's PBDayNight tints a map
 * only when its metadata says "Outdoor = true" (pbDayNightTint checks
 * MetadataOutdoor and zeroes the tone everywhere else), so the runtime needs
 * the same per-map flag to leave indoor maps unshaded. Unknown keys are
 * ignored; a missing file simply means "no map is outdoor".
 */
function parseMapMetadata(projectPath) {
  const outdoor = new Map();
  const snapEdges = new Map();
  const file = path.join(projectPath, "PBS", "metadata.txt");
  if (!existsSync(file)) return { outdoor, snapEdges };
  let text;
  try {
    text = readFileSync(file, "utf8");
  } catch (error) {
    return { outdoor, snapEdges };
  }
  let mapId = null;
  for (const raw of text.split(/\r?\n/)) {
    const line = raw.trim();
    if (line === "" || line.startsWith("#")) continue;
    const section = /^\[(\d+)\]$/.exec(line);
    if (section) {
      mapId = Number(section[1]);
      continue;
    }
    if (mapId === null) continue;
    const keyValue = /^([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$/.exec(line);
    if (!keyValue) continue;
    const key = keyValue[1].toLowerCase();
    if (key !== "outdoor" && key !== "snapedges") continue;
    const value = keyValue[2].trim().toLowerCase();
    const flag = value === "true" || value === "1" || value === "yes";
    if (key === "outdoor") {
      outdoor.set(mapId, flag);
    } else {
      snapEdges.set(mapId, flag);
    }
  }
  return { outdoor, snapEdges };
}

/**
 * R6.23: PBS/connections.txt. One connection per line:
 * {@code mapA, edgeA, offsetA, mapB, edgeB, offsetB} with an edge of
 * N/E/S/W (or a raw coordinate) and an offset in tiles. The runtime needs the
 * same normalized border points as the project's
 * MapFactoryHelper#getMapConnections: N/S keep the offset as x and use
 * y = 0/height, E/W use x = width/0 and keep the offset as y.
 */
export function resolveConnectionEdge(edge, offset, width, height) {
  const raw = String(edge).trim().toUpperCase();
  // The project's compiler accepts both "N" and "North" (csvEnumFieldOrInt).
  const value = { NORTH: "N", SOUTH: "S", EAST: "E", WEST: "W" }[raw] || raw;
  if (value === "N") return [offset, 0];
  if (value === "S") return [offset, height];
  if (value === "W") return [0, offset];
  if (value === "E") return [width, offset];
  // csvEnumFieldOrInt: a raw coordinate stays as given (x = field, y = offset).
  return [Number(value), offset];
}

/**
 * Normalizes one connections.txt record against the map dimensions.
 * Returns {@code {a, ax, ay, b, bx, by}} or throws a message naming the line.
 */
export function normalizeConnection(fields, lineNo, dimsById) {
  if (!Array.isArray(fields) || fields.length < 6) {
    throw new Error("line " + lineNo + ": expected mapA, edgeA, offsetA, mapB, edgeB, offsetB");
  }
  const a = Number(fields[0]);
  const b = Number(fields[3]);
  const offsetA = Number(fields[2]);
  const offsetB = Number(fields[5]);
  if (!Number.isInteger(a) || !Number.isInteger(b)
      || !Number.isInteger(offsetA) || !Number.isInteger(offsetB)) {
    throw new Error("map ids and offsets must be integers");
  }
  const dimsA = dimsById.get(a);
  const dimsB = dimsById.get(b);
  if (!dimsA) throw new Error("map " + a + " was not found");
  if (!dimsB) throw new Error("map " + b + " was not found");
  const [ax, ay] = resolveConnectionEdge(fields[1], offsetA, dimsA[0], dimsA[1]);
  const [bx, by] = resolveConnectionEdge(fields[4], offsetB, dimsB[0], dimsB[1]);
  if (![ax, ay, bx, by].every(Number.isFinite)) {
    throw new Error("edge must be N/E/S/W or a coordinate");
  }
  return { a, ax, ay, b, bx, by };
}

function parseConnections(projectPath, mapsIndex, warnings) {
  const dimsById = new Map(mapsIndex.map((entry) => [entry.mapId, [entry.width, entry.height]]));
  const dims = {};
  const connections = [];
  const file = path.join(projectPath, "PBS", "connections.txt");
  if (!existsSync(file)) {
    return { format: IR_FORMAT, kind: "mapConnections", total: 0, dims, connections };
  }
  let text;
  try {
    text = readFileSync(file, "utf8");
  } catch (error) {
    warnings.push({ file: "PBS/connections.txt", error: String(error.message || error) });
    return { format: IR_FORMAT, kind: "mapConnections", total: 0, dims, connections };
  }
  const lines = text.split(/\r?\n/);
  for (let index = 0; index < lines.length; index++) {
    const line = lines[index].replace(/#.*$/, "").trim();
    if (line === "") continue;
    try {
      const connection = normalizeConnection(line.split(",").map((part) => part.trim()),
          index + 1, dimsById);
      connections.push(connection);
      dims[String(connection.a)] = dimsById.get(connection.a);
      dims[String(connection.b)] = dimsById.get(connection.b);
    } catch (error) {
      warnings.push({ file: "PBS/connections.txt", line: index + 1,
        error: String(error.message || error) });
    }
  }
  return { format: IR_FORMAT, kind: "mapConnections", total: connections.length, dims, connections };
}

function pbsInventory(projectPath) {
  const pbsDir = path.join(projectPath, "PBS");
  if (!existsSync(pbsDir)) return { directory: pbsDir, exists: false, files: [], problems: [] };
  const files = [];
  const problems = [];
  let names;
  try {
    names = readdirSync(pbsDir).sort();
  } catch (error) {
    return {
      directory: pbsDir,
      exists: true,
      files: [],
      problems: [{ file: "PBS", error: "cannot list directory: " + String(error.message || error) }],
    };
  }
  for (const name of names) {
    const filePath = path.join(pbsDir, name);
    let stat = null;
    try {
      stat = statSync(filePath);
    } catch (error) {
      problems.push({ file: name, error: "cannot stat file: " + String(error.message || error) });
      continue;
    }
    if (!stat.isFile()) continue;
    let digest = null;
    try {
      digest = sha1(readFileSync(filePath).toString("latin1"));
    } catch (error) {
      problems.push({ file: name, error: "cannot read file: " + String(error.message || error) });
      continue;
    }
    files.push({
      name,
      bytes: stat.size,
      sha1: digest,
      mtime: stat.mtime.toISOString(),
      mtimeMs: stat.mtimeMs,
    });
  }
  return { directory: pbsDir, exists: true, files, problems };
}

// Creates the subdirectories this tool owns. Files inside them are kept:
// Phase 13 reuses the IR units that are still current instead of wiping
// everything (see removeStaleOutputs for the stale file cleanup). Never
// touches anything outside the generated directory.
function prepareOwnedDirs(generatedDir) {
  if (!isInside(generatedDir, generatedDir)) {
    throw new Error("refusing to write outside the generated directory: " + generatedDir);
  }
  mkdirSync(generatedDir, { recursive: true });
  for (const sub of IR_SUBDIRS) {
    const dir = path.join(generatedDir, sub);
    if (!isInside(generatedDir, dir)) {
      throw new Error("refusing to clean outside the generated directory: " + dir);
    }
    mkdirSync(dir, { recursive: true });
  }
}

// Removes the IR files this tool owns but that the current build no longer
// produces (a deleted Map001.rxdata must not leave map-001.json behind).
// Files the build just reused stay untouched, and unrelated files inside
// generated/ (hand written notes) are never removed.
function removeStaleOutputs(generatedDir, expected) {
  for (const sub of IR_SUBDIRS) {
    const dir = path.join(generatedDir, sub);
    if (!existsSync(dir)) continue;
    if (!isInside(generatedDir, dir)) {
      throw new Error("refusing to clean outside the generated directory: " + dir);
    }
    for (const name of readdirSync(dir)) {
      if (!name.endsWith(".json")) continue;
      const relative = sub + "/" + name;
      if (expected.has(relative)) continue;
      rmSync(path.join(dir, name), { force: true });
    }
  }
}

export function convertProject(projectPath, options = {}) {
  const started = Date.now();
  const onStep = typeof options.onStep === "function" ? options.onStep : () => {};
  const generatedDir = path.resolve(
    options.generatedDir || path.join(options.builderRoot || process.cwd(), "generated"),
  );
  const version = options.version || "0.0.0";
  const projectName = options.projectName || path.basename(projectPath);
  // Phase 13: incremental build cache. It is only used when the caller
  // names a cache directory (the builder passes build/); callers that do
  // not (unit tests, ad-hoc scripts) always get a full rebuild.
  const cacheDir = options.cacheDir ? path.resolve(options.cacheDir) : null;
  const useCache = Boolean(cacheDir) && options.useCache !== false;
  // Callers (build-data, build-pc) use different step numbering, so the
  // internal step labels are configurable and default to the build-data ones.
  const stepLabels = options.stepLabels || {};
  const scanStepLabel = stepLabels.scan || "2/3";
  const convertStepLabel = stepLabels.convert || "3/3";

  const result = {
    project: projectPath,
    generatedDir,
    convertedAt: new Date().toISOString(),
    durationMs: 0,
    ok: false,
    errors: [],
    warnings: [],
    problems: {
      unreadableData: [],
      uninterpretableScripts: [],
      unreadableScriptSections: [],
      unresolvedIdentifiers: [],
      unreadablePbs: [],
    },
    incremental: {
      used: false,
      reason: "",
      cacheFile: cacheDir ? cacheFilePath(cacheDir) : null,
      skipped: [],
      rebuilt: [],
    },
    counts: {},
    files: [],
    bytes: 0,
  };

  const fail = () => {
    result.durationMs = Date.now() - started;
    return result;
  };

  // Hard rule: the RMXP project is an input only, so the intermediate data must
  // never be written into the project itself. The builder root may live inside
  // the project folder (pokemon-builder/ next to Data/, Graphics/...), so its
  // own generated/ directory stays allowed.
  const builderRoot = options.builderRoot ? path.resolve(options.builderRoot) : null;
  const insideSourceProject = isInside(path.resolve(projectPath), generatedDir);
  const insideBuilderRoot = builderRoot ? isInside(builderRoot, generatedDir) : false;
  if (insideSourceProject && !insideBuilderRoot) {
    result.errors.push({
      file: generatedDir,
      error:
        "the generated directory must live outside the source project: " + projectPath,
    });
    return fail();
  }

  const validation = options.validation || validateProject(projectPath);
  for (const warning of validation.warnings) result.warnings.push("validate: " + warning);
  for (const error of validation.errors) result.errors.push({ file: projectPath, error });
  if (!validation.ok) return fail();

  onStep(scanStepLabel, "Scanning RMXP data, events and Ruby scripts...");
  const scan = scanProject(projectPath);
  const events = analyzeProjectEvents(projectPath);
  const scriptSources = readScriptSources(projectPath);
  let analysis = null;
  if (scriptSources.errors.length === 0) {
    analysis = analyzeScriptUsage(events, scriptSources.sections);
  }
  result.problems.unreadableData = scan.errors.slice();
  result.problems.uninterpretableScripts = events.problems.slice();
  result.problems.unreadableScriptSections = scriptSources.problems.slice();
  result.problems.unresolvedIdentifiers = analysis ? analysis.unresolvedIdentifiers.slice() : [];
  result.warnings.push(...scan.warnings.map((warning) => "scan: " + warning));
  result.warnings.push(...events.warnings.map((warning) => "events: " + warning));

  if (!scan.ok) {
    for (const entry of scan.errors) result.errors.push({ file: projectPath, error: entry.file + " -> " + entry.error });
    return fail();
  }
  if (!events.ok) {
    for (const entry of events.errors) result.errors.push({ file: projectPath, error: entry.file + " -> " + entry.error });
    return fail();
  }
  if (scriptSources.errors.length > 0) {
    for (const entry of scriptSources.errors) result.errors.push({ file: "Scripts.rxdata", error: entry.error });
    return fail();
  }
  if (analysis && analysis.errors.length > 0) {
    for (const entry of analysis.errors) result.errors.push({ file: "Scripts.rxdata", error: entry.error });
    return fail();
  }

  onStep(convertStepLabel, "Converting to intermediate data (Debug IR) in " + generatedDir + "...");

  // One entry per parsed map, in file order; used by the maps IR below.
  const mapFiles = events.maps.filter((entry) => entry.ok);
  const commonEventsPath = path.join(projectPath, "Data", "CommonEvents.rxdata");
  let commonEventData = [];
  try {
    commonEventData = marshalLoad(readFileSync(commonEventsPath));
    if (!Array.isArray(commonEventData)) commonEventData = [];
  } catch (error) {
    result.errors.push({ file: "CommonEvents.rxdata", error: String(error.message || error) });
    return fail();
  }
  const commonBlocks = events.scriptBlocks.filter((block) => block.source === "commonEvent");

  // Phase 13: fingerprint every input (sha1 from the scan, mtime from the
  // file system) so the cache can tell which units are still current.
  const fingerprints = {};
  for (const entry of scan.data.files) {
    const relative = "Data/" + entry.name;
    let mtime = null;
    let mtimeMs = null;
    try {
      const stat = statSync(path.join(projectPath, "Data", entry.name));
      mtime = stat.mtime.toISOString();
      mtimeMs = stat.mtimeMs;
    } catch (error) {
      result.errors.push({ file: entry.name, error: "cannot stat file: " + String(error.message || error) });
      return fail();
    }
    fingerprints[relative] = { file: relative, bytes: entry.bytes, sha1: entry.sha1, mtime, mtimeMs };
  }
  // R6.16: the Outdoor flag lives in PBS/metadata.txt, so every map unit
  // depends on that file too (editing it must recompile the map IR).
  try {
    const metadataFile = path.join(projectPath, "PBS", "metadata.txt");
    const stat = statSync(metadataFile);
    fingerprints["PBS/metadata.txt"] = {
      file: "PBS/metadata.txt",
      bytes: stat.size,
      sha1: sha1(readFileSync(metadataFile).toString("latin1")),
      mtime: stat.mtime.toISOString(),
      mtimeMs: stat.mtimeMs,
    };
  } catch (error) {
    // No PBS/metadata.txt: every map keeps outdoor=false, no fingerprint.
  }

  // The cache is an accelerator only: a missing, foreign or outdated cache
  // (and --no-cache) simply means a full rebuild.
  const cache = useCache
    ? loadCache(cacheDir, { irFormat: IR_FORMAT, version, project: projectPath })
    : { used: false, reason: "cache disabled (full rebuild)", cache: null, file: cacheDir ? cacheFilePath(cacheDir) : null };
  result.incremental.used = cache.used;
  result.incremental.reason = cache.reason;
  if (cache.file) result.incremental.cacheFile = cache.file;

  try {
    prepareOwnedDirs(generatedDir);
  } catch (error) {
    result.errors.push({ file: generatedDir, error: String(error.message || error) });
    return fail();
  }

  const write = (relative, payload) => {
    const target = path.join(generatedDir, relative);
    if (!isInside(generatedDir, target)) {
      throw new Error("refusing to write outside the generated directory: " + target);
    }
    mkdirSync(path.dirname(target), { recursive: true });
    const text = JSON.stringify(payload, null, 2);
    writeFileSync(target, text, "utf8");
    result.files.push({ file: relative, bytes: Buffer.byteLength(text, "utf8") });
    result.bytes += Buffer.byteLength(text, "utf8");
  };

  const dataDir = path.join(projectPath, "Data");
  const mapsIndex = [];
  const eventsIndex = [];
  // R6.16: per-map PBS/metadata.txt flags (currently only Outdoor).
  const mapMetadata = parseMapMetadata(projectPath);
  // Cache units of this run: kept for every map, common events and the
  // script aggregates, so the next build knows what is still current.
  const cacheUnits = {};

  // Unit dependencies: a map IR shows the map file, the map name comes from
  // MapInfos and the script block verdicts come from Scripts.rxdata, so all
  // three inputs are tracked per map.
  const mapUnitDeps = (entry) => {
    const deps = [
      "Data/" + entry.file,
      "Data/MapInfos.rxdata",
      "Data/Scripts.rxdata",
    ];
    if (fingerprints["PBS/metadata.txt"]) deps.push("PBS/metadata.txt");
    return deps;
  };
  const fingerprintSubset = (deps) => {
    const subset = {};
    for (const dep of deps) {
      const entry = fingerprints[dep];
      if (entry && !entry.error) subset[dep] = entry.sha1;
    }
    return subset;
  };

  for (const entry of mapFiles) {
    const unitId = "map:" + entry.mapId;
    const blocks = events.scriptBlocks.filter((block) => block.mapId === entry.mapId);
    const relative = "maps/map-" + pad(entry.mapId) + ".json";
    const outputs =
      blocks.length > 0 ? [relative, "events/map-" + pad(entry.mapId) + ".json"] : [relative];
    if (unitIsFresh(cache.cache, unitId, fingerprints, generatedDir)) {
      // Unchanged since the last build: reuse the IR files on disk and the
      // cached index entries instead of recompiling this map.
      const unit = cache.cache.units[unitId];
      mapsIndex.push(unit.mapsIndex);
      if (unit.eventsIndex) eventsIndex.push(unit.eventsIndex);
      result.incremental.skipped.push(unitId);
      cacheUnits[unitId] = unit;
      continue;
    }
    let mapData;
    try {
      mapData = marshalLoad(readFileSync(path.join(dataDir, entry.file)));
    } catch (error) {
      result.errors.push({ file: entry.file, error: String(error.message || error) });
      return fail();
    }
    const ir = buildMapIr({
      file: entry.file,
      mapId: entry.mapId,
      mapName: entry.mapName,
      data: mapData,
      blocks,
      outdoor: mapMetadata.outdoor.get(entry.mapId) === true,
      snapEdges: mapMetadata.snapEdges.get(entry.mapId) === true,
    });
    write(relative, ir);
    const commands = ir.events.reduce(
      (sum, event) => sum + event.pages.reduce((inner, page) => inner + page.commands.length, 0),
      0,
    );
    const indexEntry = {
      mapId: entry.mapId,
      name: entry.mapName,
      file: entry.file,
      width: ir.width,
      height: ir.height,
      tilesetId: ir.tilesetId,
      events: ir.events.length,
      pages: ir.events.reduce((sum, event) => sum + event.pages.length, 0),
      commands,
      scriptBlocks: blocks.length,
      ir: relative,
    };
    mapsIndex.push(indexEntry);
    let eventsIndexEntry = null;
    if (blocks.length > 0) {
      const eventsRelative = "events/map-" + pad(entry.mapId) + ".json";
      write(eventsRelative, {
        format: IR_FORMAT,
        kind: "mapEvents",
        mapId: entry.mapId,
        mapName: entry.mapName,
        file: entry.file,
        scriptBlocks: blocks.map((block) => mergedBlock(block, analysis)),
      });
      eventsIndexEntry = { mapId: entry.mapId, mapName: entry.mapName, ir: eventsRelative, scriptBlocks: blocks.length };
      eventsIndex.push(eventsIndexEntry);
    }
    result.incremental.rebuilt.push(unitId);
    cacheUnits[unitId] = {
      kind: "map",
      source: "Data/" + entry.file,
      dependencies: mapUnitDeps(entry),
      fingerprints: fingerprintSubset(mapUnitDeps(entry)),
      outputs,
      mapsIndex: indexEntry,
      eventsIndex: eventsIndexEntry,
    };
  }

  // R6.23: Essentials MapFactory links (PBS/connections.txt). Global output
  // like tilesets.json: always rewritten, maps carry the SnapEdges flag above.
  const connectionsIr = parseConnections(projectPath, mapsIndex, result.warnings);
  write("connections.json", connectionsIr);

  const commonEventsIndex = [];
  const commonEventsUnitId = "commonEvents";
  const commonEventsDeps = ["Data/CommonEvents.rxdata", "Data/Scripts.rxdata"];
  if (unitIsFresh(cache.cache, commonEventsUnitId, fingerprints, generatedDir)) {
    // CommonEvents.rxdata and the script symbol table are unchanged:
    // reuse the cached common event IR files and index entries.
    const unit = cache.cache.units[commonEventsUnitId];
    if (Array.isArray(unit.index)) commonEventsIndex.push(...unit.index);
    result.incremental.skipped.push(commonEventsUnitId);
    cacheUnits[commonEventsUnitId] = unit;
  } else {
    commonEventData.forEach((event, slot) => {
      if (!event || typeof event !== "object") return;
      const commonEventId = typeof event["@id"] === "number" ? event["@id"] : slot + 1;
      const blocks = ownBlocksForCommonEvent(commonBlocks, commonEventId);
      const ir = buildCommonEventIr(event, slot, blocks);
      const relative = "common-events/common-event-" + pad(commonEventId) + ".json";
      write(relative, ir);
      commonEventsIndex.push({
        id: commonEventId,
        slot,
        name: ir.name,
        trigger: ir.trigger,
        switchId: ir.switchId,
        commands: ir.commands.length,
        scriptBlocks: blocks.length,
        ir: relative,
      });
    });
    result.incremental.rebuilt.push(commonEventsUnitId);
    cacheUnits[commonEventsUnitId] = {
      kind: "commonEvents",
      source: "Data/CommonEvents.rxdata",
      dependencies: commonEventsDeps,
      fingerprints: fingerprintSubset(commonEventsDeps),
      outputs: commonEventsIndex.map((entry) => entry.ir),
      index: commonEventsIndex,
    };
  }

  // Scripts: section inventory (core / plugin), every event script block and
  // the API usage with unique argument patterns (the migration work list).
  // Scripts: the aggregates depend on Scripts.rxdata and on every map
  // (every event script block comes from a map or a common event), so they
  // are cached as one unit instead of file by file.
  const scriptsUnitId = "scripts";
  const scriptsDeps = ["Data/Scripts.rxdata", "Data/MapInfos.rxdata"].concat(
    mapFiles.map((entry) => "Data/" + entry.file),
  );
  const scriptsOutputs = ["scripts/sections.json", "scripts/blocks.json", "scripts/apis.json"];
  let scriptsUnitFresh = false;
  if (unitIsFresh(cache.cache, scriptsUnitId, fingerprints, generatedDir)) {
    scriptsUnitFresh = true;
    result.incremental.skipped.push(scriptsUnitId);
    cacheUnits[scriptsUnitId] = cache.cache.units[scriptsUnitId];
  }

  if (!scriptsUnitFresh) {
  const classificationById = new Map();
  for (const record of analysis.classification) classificationById.set(record.id, record);
  write("scripts/sections.json", {
    format: IR_FORMAT,
    kind: "scriptSections",
    total: analysis.sections.total,
    readable: analysis.sections.readable,
    coreCutoff: analysis.sections.coreCutoff,
    sections: analysis.sectionDetails.map((section) => ({
      index: section.index,
      name: section.name,
      lines: section.lines,
      origin: section.isCore ? "core" : "plugin",
      definedSymbols: section.symbols,
    })),
  });
  write("scripts/blocks.json", {
    format: IR_FORMAT,
    kind: "scriptBlocks",
    total: events.scriptBlocks.length,
    blocks: events.scriptBlocks.map((block) => mergedBlock(block, analysis)),
  });
  write("scripts/apis.json", {
    format: IR_FORMAT,
    kind: "apiUsage",
    summary: analysis.summary,
    apis: analysis.apis,
    eventGlobals: analysis.eventGlobals,
  });
  result.incremental.rebuilt.push(scriptsUnitId);
  cacheUnits[scriptsUnitId] = {
    kind: "scripts",
    source: "Data/Scripts.rxdata",
    dependencies: scriptsDeps,
    fingerprints: fingerprintSubset(scriptsDeps),
    outputs: scriptsOutputs,
  };
  }

  const pbs = pbsInventory(projectPath);
  // An unreadable PBS file is a hard failure, not a silent null digest.
  for (const problem of pbs.problems) {
    result.problems.unreadablePbs.push(problem);
    result.errors.push({ file: "PBS/" + problem.file, error: problem.error });
  }
  if (pbs.problems.length > 0) return fail();
  for (const file of pbs.files) {
    const relative = "PBS/" + file.name;
    fingerprints[relative] = {
      file: relative,
      bytes: file.bytes,
      sha1: file.sha1,
      mtime: file.mtime,
      mtimeMs: file.mtimeMs,
    };
  }
  // Stage 2 runtime data (project3 section 9): the tileset passability /
  // priority / terrain tables and the System start position. Unreadable files
  // are a hard failure, exactly like unreadable map data.
  let tilesetsIr;
  try {
    const raw = marshalLoad(readFileSync(path.join(dataDir, "Tilesets.rxdata")));
    if (!Array.isArray(raw)) throw new Error("Tilesets.rxdata root is not an Array");
    const tilesets = raw
      .filter((entry) => entry && typeof entry === "object")
      .map((entry) => buildTilesetIr(entry));
    tilesets.sort((a, b) => (a.id === null ? 0 : a.id) - (b.id === null ? 0 : b.id));
    tilesetsIr = { format: IR_FORMAT, kind: "tilesets", total: tilesets.length, tilesets };
  } catch (error) {
    result.errors.push({ file: "Tilesets.rxdata", error: String(error.message || error) });
    return fail();
  }
  let systemIr;
  try {
    systemIr = buildSystemIr(marshalLoad(readFileSync(path.join(dataDir, "System.rxdata"))));
  } catch (error) {
    result.errors.push({ file: "System.rxdata", error: String(error.message || error) });
    return fail();
  }
  write("tilesets.json", tilesetsIr);
  write("system.json", systemIr);
  // L1: title screen configuration (only when the project uses the plugin).
  const titleIr = buildTitleIr(scriptSources);
  if (titleIr) write("title.json", titleIr);
  // R6.27: Show Animation (207) plays RPG::Animation data. The cell tables are
  // 2D RGSS tables, so the raw decoder above feeds this export.
  let animationsIr = { format: IR_FORMAT, kind: "animations", total: 0, animations: [] };
  try {
    const animationsFile = path.join(dataDir, "Animations.rxdata");
    const animationsData = existsSync(animationsFile) ? marshalLoad(readFileSync(animationsFile)) : [];
    const animations = [];
    for (let id = 1; id < animationsData.length; id++) {
      const animation = animationsData[id];
      if (!animation || typeof animation !== "object") continue;
      const ir = animationIr(animation);
      if (ir.id === null) ir.id = id;
      animations.push(ir);
    }
    animationsIr = { format: IR_FORMAT, kind: "animations", total: animations.length, animations };
  } catch (error) {
    result.warnings.push({ file: "Animations.rxdata", error: String(error.message || error) });
  }
  write("animations.json", animationsIr);
  write("metadata/sources.json", {
    format: IR_FORMAT,
    kind: "sources",
    dataDir,
    dataFiles: scan.data.files,
    otherDataFiles: scan.data.otherFiles,
    pbs,
  });
  write("metadata/problems.json", {
    format: IR_FORMAT,
    kind: "problems",
    ...result.problems,
  });

  result.counts = {
    mapFiles: mapsIndex.length,
    maps: mapsIndex.length,
    events: mapsIndex.reduce((sum, entry) => sum + entry.events, 0),
    eventPages: mapsIndex.reduce((sum, entry) => sum + entry.pages, 0),
    eventCommands: mapsIndex.reduce((sum, entry) => sum + entry.commands, 0),
    commonEvents: commonEventsIndex.length,
    commonEventScriptBlocks: commonBlocks.length,
    scriptBlocks: events.scriptBlocks.length,
    scriptSections: scriptSources.totalSections,
    scriptSectionsReadable: scriptSources.readableSections,
    scriptLines: scriptSources.totalLines,
    uniqueApis: analysis.summary.uniqueApis,
    uniquePatterns: analysis.summary.uniquePatterns,
    unresolvedIdentifiers: analysis.summary.unresolvedIdentifiers,
    tilesets: tilesetsIr.tilesets.length,
    dataFiles: scan.data.rxdataCount,
    dataFilesParsed: scan.data.parsedCount,
    pbsFiles: pbs.files.length,
    mapsSkipped: result.incremental.skipped.filter((id) => id.startsWith("map:")).length,
    mapsRebuilt: result.incremental.rebuilt.filter((id) => id.startsWith("map:")).length,
  };

  write("project.json", {
    format: IR_FORMAT,
    kind: "project",
    projectName,
    builderVersion: version,
    source: { project: projectPath, dataDir },
    generatedAt: result.convertedAt,
    counts: result.counts,
    runtime: runtimeProfile(projectPath, scriptSources),
    outputs: {
      maps: "maps/index.json",
      events: "events/index.json",
      commonEvents: "common-events/index.json",
      scripts: "scripts/blocks.json",
      apis: "scripts/apis.json",
      connections: "connections.json",
      animations: "animations.json",
      tilesets: "tilesets.json",
      system: "system.json",
      ...(titleIr ? { title: "title.json" } : {}),
      build: "metadata/build.json",
      sources: "metadata/sources.json",
      problems: "metadata/problems.json",
    },
    status: "ok",
  });
  write("maps/index.json", { format: IR_FORMAT, kind: "maps", total: mapsIndex.length, maps: mapsIndex });
  write("events/index.json", { format: IR_FORMAT, kind: "mapEvents", total: eventsIndex.length, maps: eventsIndex });
  write("common-events/index.json", {
    format: IR_FORMAT,
    kind: "commonEvents",
    total: commonEventsIndex.length,
    commonEvents: commonEventsIndex,
  });
  write("metadata/build.json", {
    format: IR_FORMAT,
    kind: "build",
    builderVersion: version,
    projectName,
    generatedAt: result.convertedAt,
    source: { project: projectPath, dataDir },
    counts: result.counts,
    outputs: result.files,
    totalBytes: result.bytes,
    durations: { scanMs: scan.durationMs, eventsMs: events.durationMs, convertMs: Date.now() - started },
    warnings: result.warnings,
    incremental: {
      used: result.incremental.used,
      reason: result.incremental.reason,
      cacheFile: result.incremental.cacheFile,
      skipped: result.incremental.skipped,
      rebuilt: result.incremental.rebuilt,
    },
    ok: true,
  });

  // Phase 13: drop the IR files this build no longer owns (a deleted map
  // must not leave map-NNN.json behind), then persist the cache so the next
  // build can skip what did not change.
  const expectedOutputs = new Set([
    "project.json",
    "maps/index.json",
    "events/index.json",
    "common-events/index.json",
    "connections.json",
    "animations.json",
    "tilesets.json",
    "system.json",
    "metadata/build.json",
    "metadata/sources.json",
    "metadata/problems.json",
  ]);
  if (titleIr) expectedOutputs.add("title.json");
  for (const unit of Object.values(cacheUnits)) {
    for (const output of unit.outputs) expectedOutputs.add(output);
  }
  try {
    removeStaleOutputs(generatedDir, expectedOutputs);
  } catch (error) {
    result.errors.push({ file: generatedDir, error: String(error.message || error) });
    return fail();
  }

  if (cacheDir) {
    try {
      result.incremental.cacheFile = saveCache(cacheDir, {
        format: CACHE_FORMAT,
        irFormat: IR_FORMAT,
        builderVersion: version,
        project: projectPath,
        generatedDir,
        updatedAt: new Date().toISOString(),
        sources: fingerprints,
        units: cacheUnits,
      });
    } catch (error) {
      // A build without a usable cache must not be reported as a success:
      // the next build would silently lose its incremental information.
      result.errors.push({
        file: cacheFilePath(cacheDir),
        error: "cannot write build cache: " + String(error.message || error),
      });
      return fail();
    }
  }

  result.ok = true;
  result.durationMs = Date.now() - started;
  return result;
}

// Joins the Phase 4 block (Raw Ruby) with the Phase 5 verdict (category,
// calls, unresolved names) into a single translator record.
function mergedBlock(block, analysis) {
  const verdict = analysis && analysis.classification
    ? analysis.classification.find((record) => record.id === block.id)
    : null;
  return {
    id: block.id,
    source: block.source,
    file: block.file,
    mapId: block.mapId === undefined ? null : block.mapId,
    mapName: block.mapName === undefined ? null : block.mapName,
    eventId: block.eventId === undefined ? null : block.eventId,
    eventName: block.eventName === undefined ? null : block.eventName,
    page: block.page === undefined ? null : block.page,
    commandIndex: block.commandIndex === undefined ? null : block.commandIndex,
    indent: block.indent === undefined ? 0 : block.indent,
    continuations: block.continuations === undefined ? 0 : block.continuations,
    lines: block.lines === undefined ? null : block.lines,
    characters: block.characters === undefined ? null : block.characters,
    switchId: block.switchId === undefined ? null : block.switchId,
    category: verdict ? verdict.category : null,
    apis: verdict ? verdict.apis : [],
    calls: verdict ? verdict.calls : [],
    unresolved: verdict ? verdict.unresolved : [],
    interpreterState: verdict ? verdict.interpreterState : null,
    rubySource: block.rubySource,
  };
}

export function formatConvertSummary(result) {
  const lines = [];
  if (!result.ok && result.errors.length > 0) {
    lines.push("conversion errors: " + result.errors.length);
    return lines;
  }
  const counts = result.counts;
  lines.push("intermediate data written to " + result.generatedDir);
  lines.push(
    "maps: " + counts.maps + " (events " + counts.events + ", pages " + counts.eventPages +
      ", commands " + counts.eventCommands + ")",
  );
  lines.push(
    "common events: " + counts.commonEvents + " (script blocks " + counts.commonEventScriptBlocks + ")",
  );
  lines.push(
    "script blocks: " + counts.scriptBlocks + " from " + counts.scriptSections + " sections (" +
      counts.scriptLines + " lines)",
  );
  lines.push(
    "APIs: " + counts.uniqueApis + " unique, " + counts.uniquePatterns + " unique argument patterns",
  );
  if (result.incremental && result.incremental.used) {
    lines.push(
      "incremental build: " +
        result.incremental.skipped.length +
        " units reused, " +
        result.incremental.rebuilt.length +
        " rebuilt (cache " + result.incremental.cacheFile + ")",
    );
  }
  lines.push("files written: " + result.files.length + " (" + result.bytes + " bytes)");
  return lines;
}
