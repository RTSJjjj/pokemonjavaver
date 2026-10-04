// Standalone script usage analyzer entry point (Phase 5).
//
//   node tools/script-analyzer/analyze.js "D:\Game\PokemonProject" [--json api.json]
//                                            [--quiet] [--top N] [--api <name>] [--strict]
//
// Reads the event scripts collected in Phase 4 and classifies every block
// (ESSENTIALS_API / PLUGIN_API / RMXP_GLOBAL / SIMPLE_EXPRESSION / COMPLEX_RUBY /
// UNKNOWN) using Scripts.rxdata as the vocabulary, then reports the unique
// pattern statistics the migration estimate is based on.
//
// Exit codes: 0 = analysis succeeded, 1 = analysis failed. With --strict,
// unresolved identifiers also fail the run.

import { writeFileSync, mkdirSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { readScriptSources } from "../scanner/index.js";
import { analyzeProjectEvents } from "../event-analyzer/index.js";
import { analyzeScriptUsage, formatScriptSummary } from "./index.js";

function parseArgs(argv) {
  const args = { project: "", json: "", quiet: false, top: 15, api: "", strict: false };
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
    if (token === "--api") {
      args.api = rest.shift() || "";
      continue;
    }
    if (token.startsWith("--api=")) {
      args.api = token.slice("--api=".length);
      continue;
    }
    if (token === "--top") {
      args.top = Math.max(1, Number.parseInt(rest.shift(), 10) || 15);
      continue;
    }
    if (token.startsWith("--top=")) {
      args.top = Math.max(1, Number.parseInt(token.slice("--top=".length), 10) || 15);
      continue;
    }
    if (token === "--strict") {
      args.strict = true;
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
  "Pokemon Builder - script usage analyzer (Phase 5)",
  "",
  "Usage:",
  "  node tools/script-analyzer/analyze.js <RMXP-project-path> [options]",
  "",
  "Classifies the event scripts of Phase 4 using Scripts.rxdata as vocabulary and",
  "reports unique pattern statistics. The project is only ever read.",
  "",
  "Options:",
  "  --json <file>   write the machine readable analysis result",
  "  --top <n>       how many APIs to list per category (default 15)",
  "  --api <name>    print every pattern and sample location of one API",
  "  --strict        treat unresolved identifiers as a failure",
  "  --quiet         only print problems and the final status line",
].join("\n");

const args = parseArgs(process.argv.slice(2));
if (!args.project) {
  console.log(USAGE);
  process.exit(1);
}

const projectPath = path.resolve(args.project);
let result;
try {
  const events = analyzeProjectEvents(projectPath);
  if (!events.ok) {
    console.error("ERROR: event analysis failed, cannot classify scripts:");
    for (const error of events.errors) console.error("  " + error.file + " -> " + error.error);
    console.log("SCRIPT ANALYSIS FAILED");
    process.exit(1);
  }
  const sections = readScriptSources(projectPath);
  result = analyzeScriptUsage(
    events,
    sections.sections.map((section) => ({ index: section.index, name: section.name, source: section.source })),
  );
} catch (error) {
  console.error("ERROR: script analysis failed: " + (error && error.stack ? error.stack : String(error)));
  process.exit(1);
}

// An explicitly requested API detail is printed even with --quiet.
if (args.api) {
  const api = result.apis.find((entry) => entry.name === args.api);
  if (api) {
    console.log(
      api.name +
        " (" +
        api.category +
        "): " +
        api.occurrences +
        " occurrences, " +
        api.uniquePatterns +
        " unique patterns, " +
        api.blocks +
        " blocks",
    );
    for (const pattern of api.patterns) console.log("  " + pattern.count + "x  " + pattern.pattern);
    console.log("  sample: " + JSON.stringify(api.sample));
  } else {
    console.log("API not used in event scripts: " + args.api);
  }
}

if (!args.quiet) {
  console.log("[Pokemon Builder] script usage analyzer");
  console.log("Project: " + projectPath);
  console.log("");
  for (const line of formatScriptSummary(result, { top: args.top })) console.log("      " + line);
  console.log("");
  console.log("Done in " + result.durationMs + "ms");
}

if (args.json) {
  const jsonPath = path.resolve(args.json);
  try {
    mkdirSync(path.dirname(jsonPath), { recursive: true });
    writeFileSync(jsonPath, JSON.stringify(result, null, 2), "utf8");
    if (!args.quiet) console.log("Script report written: " + jsonPath);
  } catch (error) {
    console.error("ERROR: cannot write " + jsonPath + ": " + error.message);
    process.exit(1);
  }
}

if (!result.ok) {
  console.log("SCRIPT ANALYSIS FAILED");
  process.exit(1);
}
if (args.strict && result.summary.unresolvedIdentifiers > 0) {
  console.error("ERROR: " + result.summary.unresolvedIdentifiers + " unresolved identifier(s) (strict mode):");
  for (const identifier of result.unresolvedIdentifiers) {
    console.error("  " + identifier.name + " x" + identifier.occurrences);
  }
  console.log("SCRIPT ANALYSIS FAILED (strict)");
  process.exit(1);
}
console.log("SCRIPT ANALYSIS OK");
process.exit(0);