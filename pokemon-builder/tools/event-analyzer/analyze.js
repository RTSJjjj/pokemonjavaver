// Standalone event analyzer entry point (Phase 4).
//
//   node tools/event-analyzer/analyze.js "D:\Game\PokemonProject" [--json events.json] [--quiet]
//                                      [--list [N]] [--map <ids>]
//
// Reads MapXXX.rxdata / CommonEvents.rxdata and rebuilds every Ruby script
// held in event commands 355/655. The project is only ever read.
//
// Exit codes: 0 = analysis succeeded, 1 = analysis failed (unreadable data or
// content that could not be interpreted).

import { readFileSync, writeFileSync, mkdirSync, existsSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { analyzeProjectEvents, formatEventSummary } from "./index.js";

function parseArgs(argv) {
  const args = { project: "", json: "", quiet: false, list: 0, map: "" };
  const rest = argv.slice();
  while (rest.length > 0) {
    const token = rest.shift();
    if (token === "--json") {
      args.json = rest.shift() || "";
      continue;
    }
    if (token.startsWith("--json=")) {
      args.json = token.slice("--json=".length);
      continue;
    }
    if (token === "--map") {
      args.map = rest.shift() || "";
      continue;
    }
    if (token.startsWith("--map=")) {
      args.map = token.slice("--map=".length);
      continue;
    }
    if (token === "--list") {
      const next = rest[0];
      if (next !== undefined && !next.startsWith("--") && Number.parseInt(next, 10) > 0) {
        args.list = Number.parseInt(rest.shift(), 10);
      } else {
        args.list = 25;
      }
      continue;
    }
    if (token.startsWith("--list=")) {
      args.list = Math.max(1, Number.parseInt(token.slice("--list=".length), 10) || 25);
      continue;
    }
    if (token === "--quiet" || token === "-q") {
      args.quiet = true;
      continue;
    }
    if (!args.project) args.project = token;
  }
  return args;
}

const USAGE = [
  "Pokemon Builder - RMXP event analyzer (Phase 4)",
  "",
  "Usage:",
  "  node tools/event-analyzer/analyze.js <RMXP-project-path> [options]",
  "",
  "Reads MapXXX.rxdata / CommonEvents.rxdata and merges event commands 355/655",
  "into complete Ruby script blocks. The project is only ever read.",
  "",
  "Options:",
  "  --json <file>    write the machine readable analysis result",
  "  --list [N]       print the first N script blocks (default 25)",
  "  --map <ids>      analyze only these map ids (comma separated)",
  "  --quiet          only print problems and the final status line",
].join("\n");

const args = parseArgs(process.argv.slice(2));
if (!args.project) {
  console.log(USAGE);
  process.exit(1);
}

const projectPath = path.resolve(args.project);
let result;
try {
  result = analyzeProjectEvents(projectPath, {
    onlyMapIds: args.map ? args.map.split(",").map((item) => Number.parseInt(item.trim(), 10)) : null,
  });
} catch (error) {
  console.error("ERROR: event analysis failed: " + (error && error.stack ? error.stack : String(error)));
  process.exit(1);
}

if (!args.quiet) {
  console.log("[Pokemon Builder] event analyzer");
  console.log("Project: " + projectPath);
  console.log("");
  console.log("[1/2] Reading events and merging 355/655 script commands");
  for (const line of formatEventSummary(result)) console.log("      " + line);
  if (args.list > 0) {
    console.log("");
    console.log("[2/2] First " + Math.min(args.list, result.scriptBlocks.length) + " script blocks");
    for (const block of result.scriptBlocks.slice(0, args.list)) {
      console.log(
        "      " +
          block.id +
          " [" +
          block.source +
          " | trigger: " +
          (block.trigger === null ? "?" : block.trigger) +
          " | merged: " +
          (block.continuations + 1) +
          " parts | " +
          block.characters +
          " chars]",
      );
      const preview = block.rubySource.replace(/\r?\n/g, " ").slice(0, 120);
      console.log("        " + preview + (block.rubySource.length > 120 ? "..." : ""));
    }
  } else {
    console.log("");
    console.log("[2/2] Done in " + result.durationMs + "ms");
  }
}

if (args.json) {
  const jsonPath = path.resolve(args.json);
  try {
    mkdirSync(path.dirname(jsonPath), { recursive: true });
    writeFileSync(jsonPath, JSON.stringify(result, null, 2), "utf8");
    if (!args.quiet) console.log("Event report written: " + jsonPath);
  } catch (error) {
    console.error("ERROR: cannot write " + jsonPath + ": " + error.message);
    process.exit(1);
  }
}

if (result.ok) {
  console.log("EVENT ANALYSIS OK");
  process.exit(0);
}
console.log("EVENT ANALYSIS FAILED");
process.exit(1);