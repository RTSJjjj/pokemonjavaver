import { mkdirSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { convertProject, formatConvertSummary } from "./index.js";

// Standalone Debug IR converter entry point (Phase 8).
//
//   node tools/data-converter/convert.js "D:\Game\PokemonProject" [--out generated] [--quiet]
//
// Exit codes: 0 = data written, 1 = validation / scan / conversion failed.
// The project is only ever read; everything goes to --out (default: ./generated
// next to the builder root).

function parseArgs(argv) {
  const args = { project: "", out: "", cache: "", useCache: true, quiet: false, version: "0.0.0" };
  const rest = argv.slice();
  while (rest.length > 0) {
    const token = rest.shift();
    if (token === "--out" || token === "-o") {
      args.out = rest.shift() || "";
      continue;
    }
    if (token.startsWith("--out=")) {
      args.out = token.slice("--out=".length);
      continue;
    }
    if (token === "--cache" || token === "-c") {
      args.cache = rest.shift() || "";
      continue;
    }
    if (token.startsWith("--cache=")) {
      args.cache = token.slice("--cache=".length);
      continue;
    }
    if (token === "--no-cache") {
      args.useCache = false;
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
  "Pokemon Builder - Debug IR converter (Phase 8)",
  "",
  "Usage:",
  '  node tools/data-converter/convert.js <RMXP-project-path> [--out <dir>] [--quiet]',
  "",
  "Writes the JSON Debug IR (project.json, maps/, events/, common-events/,",
  "scripts/, metadata/) into the output directory. The project is only read.",
  "",
  "Options:",
  "  --out <dir>   output directory (default: <builder root>/generated)",
  "  --cache <dir> incremental cache directory (default: build/ next to the",
  "                 output directory; --no-cache disables reuse)",
  "  --no-cache    ignore the cache and rebuild every unit",
  "  --quiet       only print problems and the final status line",
].join("\n");

const args = parseArgs(process.argv.slice(2));
if (!args.project) {
  console.log(USAGE);
  process.exit(1);
}

const projectPath = path.resolve(args.project);
const builderRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");
const outDir = path.resolve(args.out || path.join(builderRoot, "generated"));
// The cache is an accelerator: it only exists when a cache directory is
// known (default: build/ next to the output directory). --no-cache forces
// a full rebuild but still refreshes the cache afterwards.
const cacheDir = args.cache || path.resolve(path.dirname(outDir), "build");

let result;
try {
  result = convertProject(projectPath, {
    generatedDir: outDir,
    cacheDir,
    useCache: args.useCache,
  });
} catch (error) {
  console.error("ERROR: conversion failed: " + (error && error.stack ? error.stack : String(error)));
  process.exit(1);
}

if (!args.quiet) {
  console.log("[Pokemon Builder] Debug IR converter");
  console.log("Project: " + projectPath);
  console.log("Output: " + outDir);
  console.log("");
}

for (const problem of result.problems.unreadableData) {
  console.error("ERROR: unreadable data: " + problem.file + " -> " + problem.error);
}
for (const problem of result.problems.uninterpretableScripts) {
  console.error("ERROR: uninterpretable script: " + problem.location + " -> " + problem.reason);
}
for (const problem of result.problems.unreadableScriptSections) {
  console.error("ERROR: unreadable script section " + problem.index + " \"" + problem.name + "\" -> " + problem.reason);
}
for (const problem of result.problems.unresolvedIdentifiers) {
  console.error("WARNING: unresolved identifier: " + problem.name);
}
for (const problem of result.problems.unreadablePbs) {
  console.error("ERROR: unreadable PBS file: " + problem.file + " -> " + problem.error);
}
for (const warning of result.warnings) console.log("WARNING: " + warning);

if (!args.quiet) {
  for (const line of formatConvertSummary(result)) console.log("      " + line);
}

if (result.ok) {
  console.log("BUILD DATA OK");
  process.exit(0);
}
console.log("BUILD DATA FAILED");
process.exit(1);