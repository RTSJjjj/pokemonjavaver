#!/usr/bin/env node
// R7.1 CLI: compiles generated/scripts/blocks.json into Debug IR plus a
// coverage report (project3 sections 79 and 24/25).
//
// Usage: node builder/src/script-compile.js [builderRoot] [outputDir]

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { writeScriptIr } from "./script-compiler.js";

// Flags start with "--" and must never be mistaken for the root / output path.
const positional = process.argv.slice(2).filter((argument) => !argument.startsWith("--"));
const builderRoot = positional[0]
  ? path.resolve(positional[0])
  : path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");
const allowUnsupported = process.argv.includes("--allow-unsupported");
// Default layout: the IR the runtime reads lives next to blocks.json, the
// coverage report goes to build/reports (project3 sections 24/25/79).
const scriptsDir = positional[1]
  ? path.resolve(positional[1])
  : path.join(builderRoot, "generated", "scripts");

let result;
try {
  result = writeScriptIr(builderRoot, { allowUnsupported });
} catch (error) {
  console.error(error.message);
  process.exit(1);
}
const { coverage, policy } = result;
const irPath = path.join(scriptsDir, "ir.json");
if (path.resolve(result.irPath) !== path.resolve(irPath)) {
  // An explicit output directory was requested: copy the IR there as well.
  fs.mkdirSync(scriptsDir, { recursive: true });
  fs.copyFileSync(result.irPath, irPath);
}

console.log(`blocks=${coverage.blocks} translated=${coverage.translated} `
  + `javaHandlerRequired=${coverage.javaHandlerRequired} unsupported=${coverage.unsupported} `
  + `effectiveCoverage=${coverage.coveragePercent}%`);
console.log(`wrote ${irPath}`);
console.log(`wrote ${path.join(builderRoot, "build", "reports", "script-coverage.json")}`);
console.log(`wrote ${path.join(builderRoot, "build", "reports", "java-handler-required.json")}`);

// R8: unsupported scripts fail the build with their source location (section
// 24); --allow-unsupported is the explicit development escape hatch.
if (policy.exitCode !== 0) {
  console.error(`[R8] ${policy.blocked} unsupported script block(s): release builds must fail `
    + `(project3 section 24). First ones:`);
  for (const block of policy.first) {
    console.error(`  ${block.id}  map=${block.source.mapId} event=${block.source.eventId} `
      + `page=${block.source.page} cmd=${block.source.commandIndex}  ${block.reason}`);
  }
  console.error("Use --allow-unsupported for a development build (never for release).");
} else if (policy.allowed > 0) {
  console.warn(`[R8] development build: ${policy.allowed} unsupported script block(s) allowed.`);
}
process.exit(policy.exitCode);
