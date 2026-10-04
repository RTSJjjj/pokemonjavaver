// Read-only project scanner for RPG Maker XP / Pokemon Essentials projects.
//
// Phase 3 deliverable: reads Data/*.rxdata with the Ruby Marshal reader and
// recovers the Ruby sources stored inside Scripts.rxdata (zlib deflated).
// The project is an input only - this module never writes to it.
//
// Hard rules honoured here:
//   * Unreadable RMXP data is a hard failure (never skipped silently).
//   * Script sections that cannot be inflated / decoded are reported with
//     their location (section index + name), never dropped quietly.

import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import path from "node:path";

import { marshalLoad, MarshalError } from "../../builder/src/marshal.js";
import { decodeRubyString, sha1, countLines, inflateRubySource } from "../../builder/src/util.js";

// Data file kinds, ordered roughly by how much of the pipeline needs them.
const KIND_PATTERNS = [
  [/^Scripts\.rxdata$/i, "scripts"],
  [/^MapInfos\.rxdata$/i, "mapInfos"],
  [/^Map\d+\.rxdata$/i, "map"],
  [/^CommonEvents\.rxdata$/i, "commonEvents"],
  [/^System\.rxdata$/i, "system"],
  [/^TilesetsTemp\.rxdata$/i, "tilesets"],
  [/^Tilesets\.rxdata$/i, "tilesets"],
];

export function classifyDataFile(name) {
  for (const [pattern, kind] of KIND_PATTERNS) {
    if (pattern.test(name)) return kind;
  }
  return "database";
}

// Marshal keeps strings byte-preserving (latin1). RMXP projects store text
// as UTF-8, so translate here; invalid UTF-8 falls back to latin1.
export function rubyText(value) {
  if (value === null || value === undefined) return "";
  if (typeof value !== "string") return String(value);
  return decodeRubyString(Buffer.from(value, "latin1"));
}

function isArray(value) {
  return Array.isArray(value);
}

// MapInfos.rxdata is a Ruby Hash { mapId => RPG::MapInfo } in real RMXP
// projects, but some tools rewrite it as an Array of [id, MapInfo] pairs.
// Accept both shapes so map names are never dropped silently.
export function readMapNames(data) {
  const names = [];
  const push = (id, info) => {
    if (!info || info["@name"] === undefined) return;
    names.push({
      id: typeof id === "number" ? id : null,
      name: rubyText(info["@name"]),
      order: typeof info["@order"] === "number" ? info["@order"] : null,
      parentId: typeof info["@parent_id"] === "number" ? info["@parent_id"] : null,
    });
  };
  if (data instanceof Map) {
    for (const [, entry] of data) push(entry.key, entry.value);
  } else if (isArray(data)) {
    for (const entry of data) if (isArray(entry) && entry.length >= 2) push(entry[0], entry[1]);
  } else {
    throw new Error("MapInfos.rxdata has an unexpected root node (neither Hash nor Array)");
  }
  names.sort((a, b) => (a.id === null ? 1 : b.id === null ? -1 : a.id - b.id));
  return names;
}

// id -> name lookup consumed by the event analyzer (Phase 4).
export function mapNameIndex(data) {
  const index = new Map();
  for (const entry of readMapNames(data)) if (entry.id !== null) index.set(entry.id, entry.name);
  return index;
}

// Best-effort structural summary used for the scan report. Only touches
// fields that are known to exist in RGSS data; anything unexpected simply
// yields an empty summary instead of an error.
function summarizeFile(kind, data) {
  try {
    if (kind === "map") {
      const events = data["@events"];
      let eventCount = 0;
      let pageCount = 0;
      if (events instanceof Map) {
        for (const [, entry] of events) {
          eventCount += 1;
          if (isArray(entry.value && entry.value["@pages"])) {
            pageCount += entry.value["@pages"].length;
          }
        }
      }
      return {
        width: typeof data["@width"] === "number" ? data["@width"] : null,
        height: typeof data["@height"] === "number" ? data["@height"] : null,
        events: eventCount,
        eventPages: pageCount,
      };
    }
    if (kind === "mapInfos") {
      const maps = readMapNames(data);
      return {
        maps,
        mapNames: maps.length,
        source: data instanceof Map ? "hash" : "array",
      };
    }
    if (kind === "commonEvents" && isArray(data)) {
      let scriptCommands = 0;
      for (const event of data) {
        if (!event || !isArray(event["@list"])) continue;
        for (const command of event["@list"]) {
          if (command && command["@code"] === 355) scriptCommands += 1;
        }
      }
      return { commonEvents: data.length, scriptCommands };
    }
    if (kind === "system") {
      return {
        startMapId: typeof data["@start_map_id"] === "number" ? data["@start_map_id"] : null,
        elements: isArray(data["@elements"]) ? data["@elements"].length : null,
      };
    }
    if (kind === "tilesets" && isArray(data)) {
      return { tilesets: data.length };
    }
    if (kind === "database" && isArray(data)) {
      return { entries: data.length };
    }
  } catch (error) {
    return { summaryError: error.message };
  }
  return {};
}

// Parses every Data/*.rxdata file. Individual parse failures are collected
// (with file name and marshal error) - the caller decides to abort.
export function scanDataFiles(projectPath) {
  const dataDir = path.join(projectPath, "Data");
  const result = {
    dataDir,
    exists: existsSync(dataDir),
    rxdataCount: 0,
    parsedCount: 0,
    failedCount: 0,
    kindCounts: {},
    files: [],
    errors: [],
    warnings: [],
  };
  if (!result.exists) {
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

  const rxdataNames = names.filter((name) => /\.rxdata$/i.test(name));
  const otherNames = names.filter((name) => !/\.rxdata$/i.test(name));
  result.rxdataCount = rxdataNames.length;
  result.otherFiles = otherNames.map((name) => {
    let bytes = null;
    try {
      bytes = statSync(path.join(dataDir, name)).size;
    } catch (error) {
      bytes = null;
    }
    return { name, kind: "pbs-data", bytes };
  });
  if (rxdataNames.length === 0) {
    result.warnings.push("no .rxdata files found in " + dataDir);
  }

  for (const name of rxdataNames) {
    const filePath = path.join(dataDir, name);
    let bytes = null;
    try {
      bytes = readFileSync(filePath);
    } catch (error) {
      result.failedCount += 1;
      result.errors.push({ file: name, error: "cannot read file: " + error.message });
      continue;
    }
    const kind = classifyDataFile(name);
    const entry = {
      name,
      kind,
      bytes: bytes.length,
      sha1: sha1(bytes.toString("latin1")),
      ok: false,
    };
    try {
      const data = marshalLoad(bytes);
      entry.ok = true;
      entry.summary = kind === "scripts" ? { sections: isArray(data) ? data.length : null } : summarizeFile(kind, data);
      result.parsedCount += 1;
      result.kindCounts[kind] = (result.kindCounts[kind] || 0) + 1;
    } catch (error) {
      const detail =
        error instanceof MarshalError
          ? error.message + " (offset in file: see marshal reader)"
          : String(error.message || error);
      entry.error = detail;
      result.failedCount += 1;
      result.errors.push({ file: name, error: detail });
    }
    result.files.push(entry);
  }
  return result;
}

// Reads Scripts.rxdata and inflates every script section. Sections whose
// payload cannot be recovered are reported in `problems` with their index,
// id and name; they are never silently dropped.
// Same as readScriptsSection(projectPath) but keeps every section source.
// Needed by the script analyzer (Phase 5); the plain scan report stays small.
export function readScriptSources(projectPath) {
  return readScriptsSection(projectPath, { withSources: true });
}

export function readScriptsSection(projectPath, options = {}) {
  const filePath = path.join(projectPath, "Data", "Scripts.rxdata");
  const result = {
    file: filePath,
    exists: existsSync(filePath),
    sections: [],
    problems: [],
    totalSections: 0,
    readableSections: 0,
    totalLines: 0,
    compressedBytes: 0,
    inflatedBytes: 0,
    errors: [],
  };
  if (!result.exists) {
    result.errors.push({ file: "Scripts.rxdata", error: "Scripts.rxdata not found" });
    return result;
  }

  let bytes;
  try {
    bytes = readFileSync(filePath);
  } catch (error) {
    result.errors.push({ file: "Scripts.rxdata", error: "cannot read file: " + error.message });
    return result;
  }
  let data;
  try {
    data = marshalLoad(bytes);
  } catch (error) {
    result.errors.push({
      file: "Scripts.rxdata",
      error: error instanceof MarshalError ? error.message : String(error.message || error),
    });
    return result;
  }
  if (!isArray(data)) {
    result.errors.push({
      file: "Scripts.rxdata",
      error: "unexpected root node (expected an Array of script sections)",
    });
    return result;
  }

  result.totalSections = data.length;
  data.forEach((section, index) => {
    if (!isArray(section) || section.length < 3) {
      result.problems.push({
        index,
        id: isArray(section) && section.length ? section[0] : null,
        name: isArray(section) && section.length > 1 ? rubyText(section[1]) : null,
        reason: "section is not a [id, name, payload] triple",
      });
      return;
    }
    const [id, rawName, payload] = section;
    const name = rubyText(rawName);
    const record = {
      index,
      id: typeof id === "number" ? id : null,
      name,
      lines: 0,
      compressedBytes: typeof payload === "string" ? payload.length : 0,
      inflatedBytes: 0,
      mode: null,
    };
    result.compressedBytes += record.compressedBytes;

    const inflated = inflateRubySource(payload);
    if (!inflated.ok) {
      result.problems.push({
        index,
        id: record.id,
        name,
        reason: inflated.error,
      });
      return;
    }
    record.mode = inflated.mode;
    record.lines = countLines(inflated.source);
    record.inflatedBytes = inflated.source.length;
    if (options.withSources) record.source = inflated.source;
    result.totalLines += record.lines;
    result.inflatedBytes += record.inflatedBytes;
    result.readableSections += 1;
    result.sections.push(record);
  });

  if (result.problems.length > 0) {
    result.errors.push({
      file: "Scripts.rxdata",
      error:
        result.problems.length +
        " script section(s) could not be recovered; see problems[] for locations",
    });
  }
  return result;
}

export function scanProject(projectPath) {
  const started = Date.now();
  const rxdata = scanDataFiles(projectPath);
  const scripts = readScriptsSection(projectPath);
  const errors = [...rxdata.errors, ...scripts.errors];
  const warnings = [...rxdata.warnings];
  if (!scripts.exists) {
    warnings.push("Scripts.rxdata not found; event scripts cannot be analysed");
  }
  return {
    project: projectPath,
    scannedAt: new Date().toISOString(),
    durationMs: Date.now() - started,
    ok: errors.length === 0,
    errors,
    warnings,
    data: {
      directory: rxdata.dataDir,
      exists: rxdata.exists,
      rxdataCount: rxdata.rxdataCount,
      parsedCount: rxdata.parsedCount,
      failedCount: rxdata.failedCount,
      kindCounts: rxdata.kindCounts,
      otherFiles: rxdata.otherFiles,
      files: rxdata.files,
    },
    scripts: {
      sections: scripts.sections,
      problems: scripts.problems,
      totalSections: scripts.totalSections,
      readableSections: scripts.readableSections,
      totalLines: scripts.totalLines,
      compressedBytes: scripts.compressedBytes,
      inflatedBytes: scripts.inflatedBytes,
    },
  };
}

// Human readable summary lines (used by the CLI and by the builder audit).
export function formatScanSummary(scan) {
  const lines = [];
  if (!scan.data.exists) {
    lines.push("Data directory not found: " + scan.data.directory);
    return lines;
  }
  lines.push(
    scan.data.parsedCount + "/" + scan.data.rxdataCount + " .rxdata files parsed" +
    (scan.data.failedCount > 0 ? " (" + scan.data.failedCount + " FAILED)" : ""),
  );
  const kinds = Object.entries(scan.data.kindCounts)
    .map(([kind, count]) => kind + "=" + count)
    .join(" ");
  if (kinds) lines.push("kinds: " + kinds);
  lines.push(
    "Scripts.rxdata: " +
      scan.scripts.readableSections +
      "/" +
      scan.scripts.totalSections +
      " sections readable, " +
      scan.scripts.totalLines +
      " script lines",
  );
  for (const problem of scan.scripts.problems) {
    lines.push(
      "unrecognized script: section " +
        problem.index +
        (problem.name ? " \"" + problem.name + "\"" : "") +
        " -> " +
        problem.reason,
    );
  }
  for (const error of scan.errors) {
    lines.push("unreadable data: " + error.file + " -> " + error.error);
  }
  return lines;
}
