// Read-only RPG Maker XP event analyzer (Phase 4).
//
// Walks every MapXXX.rxdata and CommonEvents.rxdata of a project and rebuilds
// the Ruby scripts that event commands carry:
//
//   code 355 -> a chunk of a "Script" command
//   code 655 -> a continuation chunk of the same script (the RMXP event
//               editor splits a script into one chunk per line: the first
//               chunk is 355, every further chunk is 655... unless the
//               author pasted several 355 commands next to each other)
//
// The reconstruction rule is taken from the project's own interpreter
// (Essentials Game_Interpreter#command_355, Scripts.rxdata section 48):
//
//     script = @list[@index].parameters[0] + "\n"
//     loop do
//       break unless @list[@index+1].code == 655 || @list[@index+1].code == 355
//       script += @list[@index+1].parameters[0] + "\n"
//       @index += 1
//     end
//
// i.e. every chunk is terminated by "\n" and BOTH 655 and a following 355
// continue the same script. Merging anything else would silently rewrite
// the Ruby the game actually executes.
//
// Hard rules honoured here:
//   * the source project is only ever read, never written to;
//   * a script block is never reported without its location;
//   * anything that cannot be interpreted (355 without a string payload, an
//     orphan 655, an unreadable data file) is reported with its location,
//     never silently dropped - such records make the analysis fail.

import { existsSync, readdirSync, readFileSync } from "node:fs";
import path from "node:path";

import { marshalLoad, MarshalError, hashEntries } from "../../builder/src/marshal.js";
import { countLines } from "../../builder/src/util.js";
import { mapNameIndex, rubyText } from "../scanner/index.js";

export const CODE_SCRIPT = 355;
export const CODE_SCRIPT_CONTINUATION = 655;

// RPG::Event::Page#trigger
const PAGE_TRIGGERS = ["action button", "player touch", "event touch", "autorun", "parallel process"];
// RPG::CommonEvent#trigger
const COMMON_EVENT_TRIGGERS = ["autorun", "parallel process", "none"];

function isArray(value) {
  return Array.isArray(value);
}

function describeError(error) {
  return error instanceof MarshalError
    ? error.message
    : String((error && error.message) || error);
}

function triggerLabel(value, table) {
  if (typeof value !== "number") return null;
  return table[value] !== undefined ? table[value] : "unknown(" + value + ")";
}

// 355 and 655 both continue a script block (see the header comment).
function codeOf(list, index) {
  return commandCode(list[index]);
}

function isScriptChunk(code) {
  return code === CODE_SCRIPT || code === CODE_SCRIPT_CONTINUATION;
}

function commandCode(entry) {
  if (entry && typeof entry === "object" && typeof entry["@code"] === "number") return entry["@code"];
  return null;
}

function scriptPayload(entry) {
  // The script text of a 355/655 command always lives in parameters[0] as a
  // plain Ruby String (byte-preserving latin1), so decode it like every
  // other text field of the project. Anything else cannot be interpreted
  // on its own.
  const parameters = entry ? entry["@parameters"] : null;
  if (!isArray(parameters) || parameters.length === 0 || typeof parameters[0] !== "string") return null;
  return rubyText(parameters[0]);
}

function mapFileName(mapId) {
  return "Map" + String(mapId).padStart(3, "0") + ".rxdata";
}

function locationLabel(context) {
  const where =
    context.source === "commonEvent"
      ? "CommonEvents.rxdata#event" + context.commonEventId
      : mapFileName(context.mapId) + "#event" + context.eventId;
  return where + " page " + context.page + " cmd " + context.commandIndex;
}

// Reads one 355 command plus every 655/355 that follows it. Returns the merged
// block plus the index the caller should continue from.
function readScriptBlock(list, context, index, problems) {
  const code = commandCode(list[index]);
  const chunk = scriptPayload(list[index]);
  if (chunk === null) {
    problems.push({
      location: locationLabel({ ...context, commandIndex: index }),
      code,
      commandIndex: index,
      reason: "script command 355 has no string payload in parameters[0]",
    });
    return { next: index + 1, block: null };
  }
  const chunks = [chunk];
  let continuations = 0;
  let cursor = index + 1;
  while (cursor < list.length) {
    if (!isScriptChunk(codeOf(list, cursor))) break;
    const extra = scriptPayload(list[cursor]);
    if (extra === null) {
      problems.push({
        location: locationLabel({ ...context, commandIndex: cursor }),
        code: CODE_SCRIPT_CONTINUATION,
        commandIndex: cursor,
        reason: "script continuation 655 has no string payload in parameters[0]",
      });
      cursor += 1;
      break;
    }
    chunks.push(extra);
    continuations += 1;
    cursor += 1;
  }
  // Every chunk ends with a newline in the project interpreter.
  const rubySource = chunks.map((part) => part + "\n").join("");
  const block = {
    id:
      context.source === "commonEvent"
        ? "commonEvent" + context.commonEventId + "/page" + context.page + "/cmd" + index
        : "map" + context.mapId + "/event" + context.eventId + "/page" + context.page + "/cmd" + index,
    source: context.source,
    mapId: context.mapId,
    mapName: context.mapName,
    eventId: context.eventId,
    eventName: context.eventName,
    page: context.page,
    pageIndex: context.pageIndex,
    trigger: context.trigger,
    commandIndex: index,
    indent: typeof list[index]["@indent"] === "number" ? list[index]["@indent"] : 0,
    continuations,
    lines: countLines(rubySource),
    characters: rubySource.length,
    switchId: context.switchId === undefined ? null : context.switchId,
    rubySource,
  };
  return { next: cursor, block };
}

// Walks one event command list (a map event page or a common event list).
export function extractScriptBlocks(list, context, problems) {
  const blocks = [];
  if (!isArray(list)) return blocks;
  let index = 0;
  while (index < list.length) {
    const code = commandCode(list[index]);
    if (code === CODE_SCRIPT) {
      const result = readScriptBlock(list, context, index, problems);
      if (result.block) blocks.push(result.block);
      index = result.next;
      continue;
    }
    if (code === CODE_SCRIPT_CONTINUATION) {
      problems.push({
        location: locationLabel({ ...context, commandIndex: index }),
        code,
        commandIndex: index,
        reason: "script continuation 655 without a preceding script command 355",
      });
    }
    index += 1;
  }
  return blocks;
}

function pageContext(mapId, mapName, eventId, eventName, page, pageIndex) {
  return {
    source: "map",
    mapId,
    mapName,
    eventId,
    eventName,
    page: pageIndex + 1,
    pageIndex,
    trigger: triggerLabel(page["@trigger"], PAGE_TRIGGERS),
  };
}

// Analyzes one already parsed RPG::Map structure.
export function analyzeMapData(data, options = {}) {
  const problems = [];
  const blocks = [];
  const mapId = options.mapId === undefined ? null : options.mapId;
  const mapName = options.mapName === undefined ? null : options.mapName;
  let eventCount = 0;
  let pageCount = 0;
  let commandCount = 0;

  if (!data || typeof data !== "object") {
    return { eventCount, pageCount, commandCount, blocks, problems };
  }
  // Ruby hashes preserve insertion order, so events arrive in editor
  // creation order; sort by numeric id to keep reports stable.
  const eventEntries = hashEntries(data["@events"]);
  eventEntries.sort((a, b) => {
    const left = typeof a[0] === "number" ? a[0] : Number.MAX_SAFE_INTEGER;
    const right = typeof b[0] === "number" ? b[0] : Number.MAX_SAFE_INTEGER;
    return left - right;
  });
  for (const [eventId, event] of eventEntries) {
    // RMXP never stores nil events in a map hash, but stay defensive so a
    // padded entry cannot crash the analyzer.
    if (!event || typeof event !== "object") continue;
    eventCount += 1;
    const eventName = rubyText(event["@name"]);
    const pages = isArray(event["@pages"]) ? event["@pages"] : [];
    pageCount += pages.length;
    pages.forEach((page, pageIndex) => {
      if (!page || typeof page !== "object") return;
      const list = isArray(page["@list"]) ? page["@list"] : [];
      commandCount += list.length;
      const context = pageContext(mapId, mapName, eventId, eventName, page, pageIndex);
      for (const block of extractScriptBlocks(list, context, problems)) blocks.push(block);
    });
  }
  return { eventCount, pageCount, commandCount, blocks, problems };
}

// Analyzes one already parsed RPG::CommonEvent array.
export function analyzeCommonEventsData(data) {
  const problems = [];
  const blocks = [];
  let commonEventCount = 0;
  let commandCount = 0;
  if (!isArray(data)) {
    problems.push({
      location: "CommonEvents.rxdata",
      code: null,
      commandIndex: null,
      reason: "root node is not an Array of RPG::CommonEvent",
    });
    return { commonEventCount, commandCount, blocks, problems };
  }
  // RMXP pads the array with nil slots (common event id 0 is never used).
  data.forEach((event, slot) => {
    if (!event || typeof event !== "object") return;
    commonEventCount += 1;
    const list = isArray(event["@list"]) ? event["@list"] : [];
    commandCount += list.length;
    const commonEventId = typeof event["@id"] === "number" ? event["@id"] : slot + 1;
    const context = {
      source: "commonEvent",
      mapId: null,
      mapName: null,
      eventId: commonEventId,
      eventName: rubyText(event["@name"]),
      commonEventId,
      // Common events have no pages; page 1 stands for their single list.
      page: 1,
      pageIndex: 0,
      trigger: triggerLabel(event["@trigger"], COMMON_EVENT_TRIGGERS),
      indent: null,
      switchId: typeof event["@switch_id"] === "number" ? event["@switch_id"] : null,
    };
    for (const block of extractScriptBlocks(list, context, problems)) blocks.push(block);
  });
  return { commonEventCount, commandCount, blocks, problems };
}

function countMapNames(data) {
  return mapNameIndex(data);
}

function normalizeMapFilter(value) {
  if (!isArray(value)) return null;
  const ids = value.filter((id) => typeof id === "number" && Number.isInteger(id));
  return ids.length > 0 ? new Set(ids) : null;
}

// Full project analysis: maps + common events. Read-only.
// options.onlyMapIds restricts the analysis to the given map ids (debugging
// aid for reports); null or empty analyzes every map.
export function analyzeProjectEvents(projectPath, options = {}) {
  const started = Date.now();
  const onlyMapIds = normalizeMapFilter(options.onlyMapIds);
  const dataDir = path.join(projectPath, "Data");
  const result = {
    project: projectPath,
    dataDir,
    analyzedAt: new Date().toISOString(),
    durationMs: 0,
    ok: false,
    errors: [],
    warnings: [],
    problems: [],
    maps: [],
    scriptBlocks: [],
    summary: {
      mapFiles: 0,
      mapsAnalyzed: 0,
      mapsWithScripts: 0,
      events: 0,
      pages: 0,
      commands: 0,
      scriptBlocks: 0,
      continuationCommands: 0,
      scriptLines: 0,
      scriptCharacters: 0,
      commonEvents: 0,
      commonEventScriptBlocks: 0,
      problems: 0,
      mapFilesAvailable: 0,
    },
  };

  if (!existsSync(dataDir)) {
    result.errors.push({ file: dataDir, error: "Data directory not found" });
    return result;
  }
  let names;
  try {
    names = readdirSync(dataDir).sort();
  } catch (error) {
    result.errors.push({ file: dataDir, error: "cannot list directory: " + error.message });
    return result;
  }

  // Map names come from MapInfos.rxdata, a Hash { mapId => RPG::MapInfo } in
  // real projects. A missing file degrades to null names (warning), an
  // unreadable one is a hard failure.
  const infosPath = path.join(dataDir, "MapInfos.rxdata");
  let mapNames = new Map();
  if (!existsSync(infosPath)) {
    result.warnings.push("MapInfos.rxdata not found; map names will be null");
  } else {
    try {
      mapNames = countMapNames(marshalLoad(readFileSync(infosPath)));
    } catch (error) {
      result.errors.push({ file: "MapInfos.rxdata", error: describeError(error) });
      result.durationMs = Date.now() - started;
      return result;
    }
  }

  const availableMapFiles = names.filter((name) => /^Map\d+\.rxdata$/i.test(name));
  if (availableMapFiles.length === 0) {
    result.warnings.push("no MapXXX.rxdata files found in " + dataDir);
  }
  let mapFiles = availableMapFiles;
  if (onlyMapIds !== null) {
    mapFiles = availableMapFiles.filter((name) =>
      onlyMapIds.has(Number.parseInt(name.slice(3, name.length - 7), 10)),
    );
    for (const id of onlyMapIds) {
      const present = availableMapFiles.some((name) => Number.parseInt(name.slice(3, name.length - 7), 10) === id);
      if (!present) result.warnings.push("requested map " + id + " not found in " + dataDir);
    }
  }
  result.summary.mapFiles = mapFiles.length;
  result.summary.mapFilesAvailable = availableMapFiles.length;

  for (const name of mapFiles) {
    const mapId = Number.parseInt(name.slice(3, name.length - 7), 10);
    const entry = {
      file: name,
      mapId: Number.isFinite(mapId) ? mapId : null,
      mapName: mapNames.has(mapId) ? mapNames.get(mapId) : null,
      events: 0,
      pages: 0,
      scriptBlocks: 0,
      ok: false,
      error: null,
    };
    let data;
    try {
      data = marshalLoad(readFileSync(path.join(dataDir, name)));
    } catch (error) {
      entry.error = describeError(error);
      result.maps.push(entry);
      result.errors.push({ file: name, error: entry.error });
      continue;
    }
    const analyzed = analyzeMapData(data, { mapId: entry.mapId, mapName: entry.mapName });
    entry.ok = true;
    entry.events = analyzed.eventCount;
    entry.pages = analyzed.pageCount;
    entry.scriptBlocks = analyzed.blocks.length;
    result.maps.push(entry);
    result.summary.mapsAnalyzed += 1;
    result.summary.events += analyzed.eventCount;
    result.summary.pages += analyzed.pageCount;
    result.summary.commands += analyzed.commandCount;
    result.summary.scriptBlocks += analyzed.blocks.length;

    if (analyzed.blocks.length > 0) result.summary.mapsWithScripts += 1;
    for (const block of analyzed.blocks) {
      result.scriptBlocks.push({ ...block, file: name });
    }
    for (const problem of analyzed.problems) {
      result.problems.push({ ...problem, file: name });
    }
  }

  const commonEventsPath = path.join(dataDir, "CommonEvents.rxdata");
  // A map filter is a debugging aid: it scopes to MapXXX.rxdata only, so
  // common events are skipped instead of being reported half way.
  if (onlyMapIds === null) {
    if (!existsSync(commonEventsPath)) {
      result.errors.push({ file: "CommonEvents.rxdata", error: "CommonEvents.rxdata not found" });
      result.durationMs = Date.now() - started;
      return result;
    }
    try {
      const analyzed = analyzeCommonEventsData(marshalLoad(readFileSync(commonEventsPath)));
      result.summary.commonEvents = analyzed.commonEventCount;
      result.summary.commonEventScriptBlocks = analyzed.blocks.length;
      for (const block of analyzed.blocks) {
        result.scriptBlocks.push({ ...block, file: "CommonEvents.rxdata" });
      }
      for (const problem of analyzed.problems) {
        result.problems.push({ ...problem, file: "CommonEvents.rxdata" });
      }
    } catch (error) {
      result.errors.push({ file: "CommonEvents.rxdata", error: describeError(error) });
      result.durationMs = Date.now() - started;
      return result;
    }
  } else {
    result.warnings.push("map filter active (" + Array.from(onlyMapIds).join(", ") + "): CommonEvents.rxdata skipped");
  }

  result.summary.problems = result.problems.length;
  result.summary.scriptBlocks += result.summary.commonEventScriptBlocks;
  result.summary.continuationCommands = result.scriptBlocks.reduce((sum, block) => sum + block.continuations, 0);
  result.summary.scriptLines = result.scriptBlocks.reduce((sum, block) => sum + block.lines, 0);
  result.summary.scriptCharacters = result.scriptBlocks.reduce((sum, block) => sum + block.characters, 0);
  result.ok = result.errors.length === 0 && result.problems.length === 0;
  result.durationMs = Date.now() - started;
  return result;
}

// Console summary lines shared by the CLI and the builder audit step.
export function formatEventSummary(result) {
  const s = result.summary;
  const lines = [
    s.mapFiles + " map files, " + s.events + " events, " + s.pages + " pages, " + s.commands + " commands",
    "script blocks: " + s.scriptBlocks + " (continuations merged: " + s.continuationCommands + ")",
    "common events: " + s.commonEvents + " with " + s.commonEventScriptBlocks + " script blocks",
    "script lines: " + s.scriptLines + ", characters: " + s.scriptCharacters,
  ];
  for (const warning of result.warnings) {
    lines.push("warning: " + warning);
  }
  for (const problem of result.problems) {
    lines.push("uninterpretable: " + problem.file + " -> " + problem.location + " -> " + problem.reason);
  }
  for (const error of result.errors) {
    lines.push("unreadable data: " + error.file + " -> " + error.error);
  }
  return lines;
}