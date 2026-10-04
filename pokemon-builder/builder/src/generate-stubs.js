#!/usr/bin/env node
// R8.2 CLI: turns build/reports/java-handler-required.json into Java stubs.
//
// Usage: node builder/src/generate-stubs.js [--limit N] [--runtime]
//   --limit N   write at most N stubs (default 20, 0 = all)
//   --runtime   write into runtime/core/src/main/java/pokemon/runtime/script/generated
//               (default: build/stubs)

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { generateStubs } from "./handler-stubs.js";

const builderRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");
const limitArg = process.argv.indexOf("--limit");
const limit = limitArg >= 0 ? Number(process.argv[limitArg + 1]) : 20;
const intoRuntime = process.argv.includes("--runtime");

const reportPath = path.join(builderRoot, "build", "reports", "java-handler-required.json");
if (!fs.existsSync(reportPath)) {
  console.error(`report not found: ${reportPath} (run script-compile.js first)`);
  process.exit(1);
}

const report = JSON.parse(fs.readFileSync(reportPath, "utf8"));
const problems = { javaHandlerRequired: report.blocks || [] };
const stubs = generateStubs(problems, { limit: Number.isFinite(limit) ? limit : 20 });

const outputDir = intoRuntime
  ? path.join(builderRoot, "runtime", "core", "src", "main", "java", "pokemon", "runtime", "script", "generated")
  : path.join(builderRoot, "build", "stubs");
fs.mkdirSync(outputDir, { recursive: true });

for (const stub of stubs) {
  fs.writeFileSync(path.join(outputDir, stub.fileName), stub.source);
}

console.log(`java-handler-required blocks=${(report.blocks || []).length} `
  + `stubs written=${stubs.length}${limit > 0 && report.blocks.length > limit ? " (use --limit 0 for all)" : ""}`);
console.log(`wrote ${outputDir}`);
