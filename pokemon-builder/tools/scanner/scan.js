import { readFileSync, writeFileSync, existsSync, mkdirSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { scanProject, formatScanSummary } from "./index.js";

// Standalone scanner entry point.
//
//   node tools/scanner/scan.js "D:\Game\PokemonProject" [--json scan.json] [--quiet]
//
// Exit codes: 0 = scan succeeded, 1 = scan failed (unreadable data).
// Never writes to the scanned project.

function parseArgs(argv) {
  const args = { project: "", json: "", quiet: false };
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
    if (token === "--quiet" || token === "-q") {
      args.quiet = true;
      continue;
    }
    if (!args.project) args.project = token;
  }
  return args;
}

const USAGE = [
  "Pokemon Builder - project scanner (Phase 3)",
  "",
  "Usage:",
  '  node tools/scanner/scan.js <RMXP-project-path> [--json <file>] [--quiet]',
  "",
  "Reads Data/*.rxdata (Ruby Marshal 4.8) and Scripts.rxdata (zlib deflated).",
  "The project is only ever read, never modified.",
  "",
  "Options:",
  "  --json <file>   write the machine readable scan result",
  "  --quiet         only print problems and the final status line",
].join("\n");

const args = parseArgs(process.argv.slice(2));
if (!args.project) {
  console.log(USAGE);
  process.exit(1);
}

const projectPath = path.resolve(args.project);
let scan;
try {
  scan = scanProject(projectPath);
} catch (error) {
  console.error("ERROR: scan failed: " + (error && error.stack ? error.stack : String(error)));
  process.exit(1);
}

if (!args.quiet) {
  console.log("[Pokemon Builder] project scanner");
  console.log("Project: " + projectPath);
  console.log("");
  if (!scan.data.exists) {
    console.log("[1/2] Data directory not found: " + scan.data.directory);
  } else {
    console.log("[1/2] Scanned " + scan.data.rxdataCount + " .rxdata files in Data/");
  }
  for (const line of formatScanSummary(scan)) console.log("      " + line);
  console.log("");
  console.log("[2/2] Done in " + scan.durationMs + "ms");
  console.log("");
}

if (args.json) {
  const jsonPath = path.resolve(args.json);
  try {
    mkdirSync(path.dirname(jsonPath), { recursive: true });
    writeFileSync(jsonPath, JSON.stringify(scan, null, 2), "utf8");
    if (!args.quiet) console.log("Scan report written: " + jsonPath);
  } catch (error) {
    console.error("ERROR: cannot write " + jsonPath + ": " + error.message);
    process.exit(1);
  }
}

if (scan.ok) {
  console.log("SCAN OK");
  process.exit(0);
}
console.log("SCAN FAILED");
process.exit(1);
