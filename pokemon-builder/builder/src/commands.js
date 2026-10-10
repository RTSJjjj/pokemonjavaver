// Command handlers for the Builder CLI.
//
// Exit codes:
//   0  success
//   1  error (bad arguments, validation failure, IO failure, failed build step)
//
// A build step that cannot run MUST fail loudly with a clear message;
// pretending a build succeeded is never acceptable.

import { existsSync, mkdirSync, readdirSync, readFileSync, rmSync, writeFileSync, copyFileSync } from "node:fs";
import { execFileSync } from "node:child_process";
import path from "node:path";
import { loadConfig, resolveOutputPaths, isInside, DEFAULT_CONFIG } from "./config.js";
import { Logger } from "./logger.js";
import { validateProject } from "./project.js";
import { scanProject, formatScanSummary } from "../../tools/scanner/index.js";
import { analyzeProjectEvents, formatEventSummary } from "../../tools/event-analyzer/index.js";
import { readScriptSources } from "../../tools/scanner/index.js";
import { analyzeScriptUsage, formatScriptSummary } from "../../tools/script-analyzer/index.js";
import { writeAuditReports } from "../../tools/report-writer/index.js";
import { convertProject, formatConvertSummary } from "../../tools/data-converter/index.js";
import { writeScriptIr } from "./script-compiler.js";
import { syncBossBattles } from "./boss-battles.js";
import { syncQuestData } from "./quest-data.js";
import {
  DEFAULT_AUDIO_OPTIONS,
  AUDIO_PRESETS,
  formatAudioScanSummary,
  resolveAudioOptions,
  scanAudio,
} from "../../tools/audio-compiler/index.js";
import { writeAudioAuditReports } from "../../tools/audio-compiler/report.js";
import { runAudioBuild } from "../../tools/audio-compiler/encoder.js";
import { androidTarget, detectGradle, detectJavaRuntime, formatGradleStatus, formatRuntimeStatus, formatTargetStatus, runtimeImageNeeded } from "./runtime.js";
import {
  androidApkPath,
  desktopArtifacts,
  fileSizeLabel,
  missingArtifacts,
  mountAsciiDrive,
  runGradleTasks,
} from "./gradle.js";
import { runtimeDataSummary, validateRuntimeData } from "./runtime-data.js";
import { copyRuntimeAssets, createDataPack } from "./data-pack.js";
import { encryptTree } from "./resource-crypto.js";
import { createBuildReport, formatBuildReport, writeBuildReport } from "./build-report.js";

export const EXIT_OK = 0;
export const EXIT_ERROR = 1;
export const EXIT_NOT_IMPLEMENTED = 2;

const COMMANDS = [
  "audit",
  "convert",
  "validate",
  "audio-audit",
  "build-audio",
  "build-data",
  "build-pc",
  "build-android",
  "build-android-legacy",
  "clean",
  "help",
];

const USAGE = [
  "Pokemon Builder - RPG Maker XP / Pokemon Essentials automation tool",
  "",
  "Usage:",
  '  builder <command> [project-path] [options]',
  "",
  "Commands:",
  "  audit <project>                  Audit project (scan data, events, scripts, APIs)",
  "  convert <project>                Convert project to intermediate data (Debug IR)",
  "  validate <project>               Validate that a project can be processed",
  "  audio-audit <project>            Audit Audio/ assets (read only scan + codec report)",
  "  build-audio <project>            Compile publish audio into generated/audio/",
  "  build-data <project>             Scan + validate + convert into generated/",
  "  build-pc <project>               Full PC build (audit -> data -> audio -> runtime tests -> Gradle -> dist)",
  "  build-android <project>          Full modern Android build (API 21+, debug APK preview)",
  "  build-android-legacy <project>   Full legacy Android build (API 21+ placeholder backend)",
  "  clean [--all]                    Remove build outputs (never touches the source project)",
  "  help                             Show this help",
  "",
  "Options:",
  "  --project <path>   Override the project path from builder-config.json",
  "  --all              clean: also remove dist/ and logs/*.log",
  "  --no-cache         build-data/build-pc/build-android*: ignore build/cache.json",
  "                     and rebuild every unit (the cache is still refreshed)",
  "  --runtime-image <path>  build-pc: jpackage bundles this JDK/JRE as the game",
  "                     runtime (for JDKs without jmods/; also POKEMON_RUNTIME_IMAGE)",
  "  --no-assets        build-pc: skip copying Graphics/ + Fonts/ into dist",
  "  --no-data          build-android: skip the L3 data pack (slim APK for adb push)",
  "  --no-encrypt       build-pc / build-android: leave the packaged game files plain (P3 encryption off)",
  "  --release          build-android: signed release APK (preview keystore, L3)",
  "",
  "Exit codes:",
  "  0  success",
  "  1  error (bad arguments, validation failure, IO failure, failed build step)",
].join("\n");

function parseArgs(argv) {
  const parsed = { command: "", positional: [], flags: {} };
  const rest = argv.slice();
  parsed.command = rest.shift() || "";
  for (let i = 0; i < rest.length; i++) {
    const token = rest[i];
    if (token === "--project") {
      parsed.flags.project = rest[i + 1];
      i += 1;
      continue;
    }
    if (token.startsWith("--project=")) {
      parsed.flags.project = token.slice("--project=".length);
      continue;
    }
    if (token === "--runtime-image") {
      parsed.flags.runtimeImage = rest[i + 1];
      i += 1;
      continue;
    }
    if (token.startsWith("--runtime-image=")) {
      parsed.flags.runtimeImage = token.slice("--runtime-image=".length);
      continue;
    }
    if (token.startsWith("--")) {
      parsed.flags[token.slice(2)] = true;
      continue;
    }
    parsed.positional.push(token);
  }
  return parsed;
}

export function createContext(builderRoot, options = {}) {
  const config = loadConfig(builderRoot);
  const paths = resolveOutputPaths(builderRoot, config);
  for (const dir of [paths.logs, paths.build, paths.generated, paths.dist]) {
    mkdirSync(dir, { recursive: true });
  }
  return { builderRoot, config, paths, options };
}

function resolveProject(ctx, parsed) {
  const fromArgs = parsed.positional[0] || parsed.flags.project || "";
  const projectPath = fromArgs || ctx.config.source.rmxpProject || "";
  if (!projectPath) {
    return {
      error:
        "no project path given. Pass it as an argument, e.g. " +
        'builder.bat audit "D:\\Game\\PokemonProject", ' +
        "or set source.rmxpProject in builder-config.json.",
    };
  }
  return { projectPath: path.resolve(projectPath) };
}

// Shared first step of every pipeline command. Returns the validation result
// or null (after printing VALIDATE FAILED) when the project is unusable.
function runValidation(ctx, parsed, stepLabel) {
  const resolved = resolveProject(ctx, parsed);
  if (resolved.error) {
    ctx.logger.error(resolved.error);
    console.log("VALIDATE FAILED");
    return null;
  }
  const result = validateProject(resolved.projectPath);
  ctx.logger.step(stepLabel + " Validating project: " + resolved.projectPath);
  for (const warning of result.warnings) ctx.logger.warn(warning);
  for (const error of result.errors) ctx.logger.error(error);
  if (!result.ok) {
    console.log("VALIDATE FAILED");
    return null;
  }
  console.log("VALIDATE OK");
  ctx.logger.info("Project validated: " + resolved.projectPath);
  return { result, projectPath: resolved.projectPath };
}

// Phase 3: run the project scanner and persist a machine readable report.
// Unreadable RMXP data is a hard failure (see hard rules in README).
function runScan(ctx, projectPath, stepLabel) {
  ctx.logger.step(stepLabel + " Scanning RMXP data (Data/*.rxdata + Scripts.rxdata)...");
  const scan = scanProject(projectPath);
  for (const line of formatScanSummary(scan)) ctx.logger.step("      " + line);
  try {
    const reportPath = path.join(ctx.paths.build, "reports", "scan.json");
    mkdirSync(path.dirname(reportPath), { recursive: true });
    writeFileSync(reportPath, JSON.stringify(scan, null, 2), "utf8");
    ctx.logger.output(reportPath);
  } catch (error) {
    ctx.logger.error("cannot write scan report: " + error.message);
    return null;
  }
  if (!scan.ok) {
    for (const error of scan.errors) ctx.logger.error("unreadable data: " + error.file + " -> " + error.error);
    console.log("SCAN FAILED");
    return null;
  }
  console.log("SCAN OK");
  return scan;
}

// Phase 4: run the RMXP event analyzer and persist a machine readable report.
// Unreadable event data is a hard failure (see hard rules in README).
function runEventAnalysis(ctx, projectPath, stepLabel) {
  ctx.logger.step(
    stepLabel +
      " Scanning RMXP events (MapXXX.rxdata / CommonEvents.rxdata, merging commands 355/655)...",
  );
  const events = analyzeProjectEvents(projectPath);
  for (const line of formatEventSummary(events)) ctx.logger.step("      " + line);
  try {
    const reportPath = path.join(ctx.paths.build, "reports", "events.json");
    mkdirSync(path.dirname(reportPath), { recursive: true });
    writeFileSync(reportPath, JSON.stringify(events, null, 2), "utf8");
    ctx.logger.output(reportPath);
  } catch (error) {
    ctx.logger.error("cannot write event report: " + error.message);
    return null;
  }
  if (!events.ok) {
    for (const error of events.errors) ctx.logger.error("unreadable event data: " + error.file + " -> " + error.error);
    for (const problem of events.problems) ctx.logger.error("uninterpretable: " + problem.file + " -> " + problem.location + " -> " + problem.reason);
    console.log("EVENT ANALYSIS FAILED");
    return null;
  }
  console.log("EVENT ANALYSIS OK");
  return events;
}

// Phase 5: classify every event script and build the unique pattern
// statistics. Read-only.
// Phase 5: classify every event script and build the unique pattern
// statistics. Read-only.
function runScriptAnalysis(ctx, projectPath, events, stepLabel) {
  ctx.logger.step(
    stepLabel +
      " Analyzing event scripts (Essentials API / plugins / unique patterns)...",
  );
  let sections;
  try {
    sections = readScriptSources(projectPath);
  } catch (error) {
    ctx.logger.error("cannot read Scripts.rxdata: " + error.message);
    console.log("SCRIPT ANALYSIS FAILED");
    return null;
  }
  for (const problem of sections.problems) {
    ctx.logger.error("unreadable script section: " + problem.index + " \"" + problem.name + "\" -> " + problem.reason);
  }
  if (sections.errors.length > 0) {
    for (const error of sections.errors) ctx.logger.error(error.file + " -> " + error.error);
    console.log("SCRIPT ANALYSIS FAILED");
    return null;
  }
  const analysis = analyzeScriptUsage(
    events,
    sections.sections.map((section) => ({
      index: section.index,
      name: section.name,
      source: section.source,
      lines: section.lines,
    })),
  );
  for (const line of formatScriptSummary(analysis, { top: 10 })) ctx.logger.step("      " + line);
  console.log("SCRIPT ANALYSIS OK");
  return analysis;
}

// Phase 6: write the Markdown reports and the machine readable audit JSON.
function runReports(ctx, projectPath, scan, events, analysis, stepLabel) {
  ctx.logger.step(stepLabel + " Writing audit reports...");
  let written;
  try {
    written = writeAuditReports({
      project: projectPath,
      version: ctx.config.version,
      scan,
      events,
      scripts: analysis,
      docsDir: path.join(ctx.builderRoot, "docs"),
      reportsDir: path.join(ctx.paths.build, "reports"),
    });
  } catch (error) {
    ctx.logger.error("cannot write audit reports: " + error.message);
    console.log("REPORTS FAILED");
    return null;
  }
  for (const file of written.written) ctx.logger.output(file);
  console.log("REPORTS OK");
  return written;
}

function cmdHelp() {
  console.log(USAGE);
  return EXIT_OK;
}

function cmdValidate(ctx, parsed) {
  const result = runValidation(ctx, parsed, "[1/1]");
  return result ? EXIT_OK : EXIT_ERROR;
}

// Shared audit pipeline (Phases 3-6): scan the data, scan the events, classify
// the Ruby scripts and write the reports. Used by `audit` and by every build
// command, so the audit logic has exactly one implementation. `stepLabels`
// keeps each caller's own step numbering in the console output.
function runAuditPipeline(ctx, projectPath, stepLabels) {
  const scan = runScan(ctx, projectPath, stepLabels.scan);
  if (!scan) return null;
  const events = runEventAnalysis(ctx, projectPath, stepLabels.events);
  if (!events) return null;
  const analysis = runScriptAnalysis(ctx, projectPath, events, stepLabels.scripts);
  if (!analysis) return null;
  const reports = runReports(ctx, projectPath, scan, events, analysis, stepLabels.reports);
  if (!reports) return null;
  return { scan, events, analysis, reports };
}

function cmdAudit(ctx, parsed) {
  const validated = runValidation(ctx, parsed, "[1/5]");
  if (!validated) return EXIT_ERROR;
  const audit = runAuditPipeline(ctx, validated.projectPath, {
    scan: "[2/5]",
    events: "[3/5]",
    scripts: "[4/5]",
    reports: "[5/5]",
  });
  if (!audit) return EXIT_ERROR;
  console.log("AUDIT SUCCESS");
  console.log("BUILD SUCCESS");
  return EXIT_OK;
}

// Phase 8: validate -> scan -> convert into generated/ (JSON Debug IR).
// Shared by build-data and build-pc: one conversion implementation, one set of
// failure messages, and never a half written IR.
function runConvert(ctx, validated, stepLabels, useCache = true) {
  let converted;
  try {
    converted = convertProject(validated.projectPath, {
      generatedDir: ctx.paths.generated,
      builderRoot: ctx.builderRoot,
      version: ctx.config.version,
      projectName: ctx.config.projectName,
      validation: validated.result,
      stepLabels,
      useCache,
      // Phase 13: the incremental cache lives outside generated/ so a clean
      // rebuild of the data cannot be mistaken for a cache hit.
      cacheDir: ctx.paths.build,
      onStep: (label, message) => ctx.logger.step("[" + label + "] " + message),
    });
  } catch (error) {
    ctx.logger.error("converter failed: " + (error && error.stack ? error.stack : String(error)));
    console.log("CONVERT FAILED");
    return null;
  }
  for (const line of formatConvertSummary(converted)) ctx.logger.step("      " + line);
  if (converted.incremental && converted.incremental.used) {
    ctx.logger.step(
      "      incremental: " +
        converted.incremental.skipped.length +
        " units reused, " +
        converted.incremental.rebuilt.length +
        " rebuilt",
    );
  }
  // Every output file goes into the log (latest.log + the timestamped log),
  // even when the unit was reused from the cache.
  for (const file of converted.files) ctx.logger.output(path.join(ctx.paths.generated, file.file));
  if (converted.incremental && converted.incremental.cacheFile) {
    ctx.logger.output(converted.incremental.cacheFile);
  }
  for (const problem of converted.problems.unreadableData) {
    ctx.logger.error("unreadable data: " + problem.file + " -> " + problem.error);
  }
  for (const problem of converted.problems.uninterpretableScripts) {
    ctx.logger.error("uninterpretable script: " + problem.location + " -> " + problem.reason);
  }
  for (const problem of converted.problems.unreadableScriptSections) {
    ctx.logger.error(
      "unreadable script section: " + problem.index + " \"" + problem.name + "\" -> " + problem.reason,
    );
  }
  for (const problem of converted.problems.unresolvedIdentifiers) {
    ctx.logger.error("unresolved identifier: " + problem.name);
  }
  for (const error of converted.errors) {
    ctx.logger.error(error.file + " -> " + error.error);
  }
  if (!converted.ok) {
    console.log("CONVERT FAILED");
    return null;
  }
  return converted;
}

function cmdBuildData(ctx, parsed) {
  const validated = runValidation(ctx, parsed, "[1/4]");
  if (!validated) return EXIT_ERROR;
  const converted = runConvert(ctx, validated, { scan: "2/4", convert: "3/4" }, parsed.flags["no-cache"] !== true);
  if (!converted) return EXIT_ERROR;
  if (!compileScripts(ctx, "4/4")) return EXIT_ERROR;
  if (!bossBattleStep(ctx, validated.projectPath, "4/4")) return EXIT_ERROR;
  if (!questDataStep(ctx, validated.projectPath, "4/4")) return EXIT_ERROR;
  console.log("BUILD DATA SUCCESS");
  console.log("BUILD SUCCESS");
  return EXIT_OK;
}

/**
 * R6.11: the runtime reads generated/scripts/ir.json for every Essentials
 * script block (Set Temp Switch, Give Item, pbSmashThisEvent ...). The data
 * conversion replaces generated/scripts, so build-data / build-pc compile the
 * IR right afterwards - a data rebuild can no longer orphan it.
 */
function compileScripts(ctx, step) {
  ctx.logger.step(`[${step}] Compiling Essentials script blocks into runtime IR...`);
  try {
    const { coverage, irPath } = writeScriptIr(ctx.builderRoot, { allowUnsupported: true });
    ctx.logger.step(`      blocks=${coverage.blocks} translated=${coverage.translated} `
      + `javaHandlerRequired=${coverage.javaHandlerRequired} unsupported=${coverage.unsupported} `
      + `coverage=${coverage.coveragePercent}%`);
    ctx.logger.step(`      wrote ${irPath} (development build: unsupported blocks allowed)`);
    return { coverage, irPath };
  } catch (error) {
    ctx.logger.error(`script compilation failed: ${error.message}`);
    return null;
  }
}

/**
 * Boss_Battles: new `def battleXxx` in the plugin section become BossBattleData.java here (sub-step of the script
 * step). A def the strict generator rejects, or an event calling a boss with no def, fails the build.
 */
function bossBattleStep(ctx, projectPath, step) {
  ctx.logger.step(`[${step}] Boss_Battles: exporting the plugin section and generating BossBattleData.java...`);
  const boss = syncBossBattles(ctx.builderRoot, projectPath);
  for (const warning of boss.warnings) ctx.logger.warn("      " + warning);
  if (!boss.ok) {
    for (const error of boss.errors) ctx.logger.error("      " + error);
    ctx.logger.error("boss battle generation failed");
    return false;
  }
  if (!boss.skipped) {
    ctx.logger.step(`      defs=${boss.defs} entries=${boss.entries} ${boss.changed ? "updated" : "unchanged"}`);
  }
  return true;
}

/** Quest plugin: the 004_Quest_Data table becomes generated/quests.json (the quest log's names, stages and rewards). */
function questDataStep(ctx, projectPath, step) {
  ctx.logger.step(`[${step}] Quest data: exporting the plugin table to generated/quests.json...`);
  const quests = syncQuestData(ctx.builderRoot, projectPath);
  for (const warning of quests.warnings) ctx.logger.warn("      " + warning);
  if (!quests.ok) {
    for (const error of quests.errors) ctx.logger.error("      " + error);
    ctx.logger.error("quest data export failed");
    return false;
  }
  if (!quests.skipped) {
    ctx.logger.step(`      quests=${quests.quests} ${quests.changed ? "updated" : "unchanged"}`);
  }
  return true;
}

// ---------------------------------------------------------------------------
// R14: real build pipelines (project3 sections 45, 52-55, 63)
// ---------------------------------------------------------------------------

/** libGDX version from the runtime's gradle.properties (build report field). */
function readLibGdxVersion(builderRoot) {
  try {
    const text = readFileSync(path.join(builderRoot, "runtime", "gradle.properties"), "utf8");
    const match = /^gdxVersion\s*=\s*(.+)$/m.exec(text);
    return match ? match[1].trim() : "";
  } catch (error) {
    return "";
  }
}

/** Runs the runtime Gradle wrapper and echoes its output into the build log. */
function invokeGradle(ctx, tasks, extras = {}) {
  const runner = (ctx.options && ctx.options.gradleRunner) || runGradleTasks;
  const result = runner(ctx.builderRoot, tasks, { javaHome: extras.javaHome });
  const output = result.output || "";
  if (output) process.stdout.write(output.endsWith("\n") ? output : output + "\n");
  if (result.missing) {
    ctx.logger.error(
      "Gradle wrapper not found: " + result.executable + " (is the runtime/ checkout present?)",
    );
  } else if (result.mountFailed) {
    ctx.logger.error("cannot create an ASCII subst drive for the Gradle build"
      + (mountAsciiDrive.lastError ? " (" + mountAsciiDrive.lastError + ")" : "")
      + "; move the project to a path with only ASCII characters to skip the subst drive");
  }
  return result;
}

/**
 * build-pc: `--runtime-image <path>` (or POKEMON_RUNTIME_IMAGE, or
 * config.runtime.packageRuntimeImage) makes jpackage bundle that JDK/JRE
 * instead of running jlink - the way to package with a JDK that has no jmods/.
 *
 * L5 fallback: when nothing explicit is set and the detected runtime is a
 * jlink image without jmods/ (e.g. the JDK shipped with some launchers), it is
 * bundled automatically; jpackage could never jlink such a home.
 */
function explicitRuntimeImage(ctx, parsed) {
  return (
    (parsed.flags && parsed.flags.runtimeImage) ||
    process.env.POKEMON_RUNTIME_IMAGE ||
    (ctx.config.runtime && ctx.config.runtime.packageRuntimeImage) ||
    ""
  );
}

function runtimeImageArgument(ctx, parsed) {
  const explicit = explicitRuntimeImage(ctx, parsed);
  if (explicit) {
    return ["-PruntimeImage=" + explicit];
  }
  const runtime = detectJavaRuntime(ctx.builderRoot);
  if (runtime.found && runtime.javaHome && runtimeImageNeeded(runtime.javaHome)) {
    return ["-PruntimeImage=" + runtime.javaHome];
  }
  return [];
}

/**
 * Step [6/8]: the runtime must be able to load what the build just wrote.
 * Returns the summary counts, or null after printing the problems.
 */
function validateRuntimeDataStep(ctx, stepLabel) {
  const data = validateRuntimeData(ctx.paths.generated);
  if (!data.ok) {
    for (const problem of data.problems) ctx.logger.error(problem);
    console.log("RUNTIME DATA INVALID");
    return null;
  }
  const summary = runtimeDataSummary(ctx.paths.generated);
  ctx.logger.step(
    stepLabel + " Runtime data ready: " + summary.maps + " maps, " + summary.events +
      " events, " + summary.audioAssets + " audio assets",
  );
  return summary;
}

/** Builds, writes and logs the section 55 build report. */
function writeBuildReportStep(ctx, input) {
  const report = createBuildReport(input);
  try {
    const file = writeBuildReport(ctx.paths.generated, report);
    ctx.logger.output(file);
  } catch (error) {
    ctx.logger.warn("cannot write build report: " + error.message);
  }
  for (const line of formatBuildReport(report)) ctx.logger.step("      " + line);
  return report;
}

/** The counts shared by both pipelines' build reports. */
function reportCounts(converted, data, ir) {
  return {
    mapsCompiled: converted.counts ? converted.counts.maps : data.maps,
    eventsCompiled: converted.counts ? converted.counts.events : data.events,
    scriptsTranslated: ir.coverage ? ir.coverage.translated : 0,
    scriptsUnsupported: ir.coverage ? ir.coverage.unsupported : 0,
    audioAssets: data.audioAssets,
  };
}

// R14: the full PC build (project3 sections 45, 63):
//   [1/8] Audit              validate + scan data/events/scripts + reports
//   [2/8] Convert Maps       scan the RMXP data, write the map IR
//   [3/8] Compile Events     the same converter writes the event IR
//   [4/8] Translate Scripts  write generated/scripts/ir.json
//   [5/8] Optimize Audio     generated/audio + runtime manifest
//   [6/8] Validate Runtime Data
//   [7/8] Gradle Build       runtime tests (agentCheck) + lwjgl3:dist
//   [8/8] Package            verify dist artifacts + write the build report
function cmdBuildPc(ctx, parsed) {
  const startedAt = new Date().toISOString();
  const runtime = detectJavaRuntime(ctx.builderRoot);
  const gradle = detectGradle(ctx.builderRoot);

  const validated = runValidation(ctx, parsed, "[1/8]");
  if (!validated) return EXIT_ERROR;

  ctx.logger.step("[1/8] Audit: scanning data, events and scripts...");
  const audit = runAuditPipeline(ctx, validated.projectPath, {
    scan: "[1/8]",
    events: "[1/8]",
    scripts: "[1/8]",
    reports: "[1/8]",
  });
  if (!audit) return EXIT_ERROR;
  console.log("AUDIT SUCCESS");

  const converted = runConvert(ctx, validated, { scan: "[2/8]", convert: "[3/8]" }, parsed.flags["no-cache"] !== true);
  if (!converted) return EXIT_ERROR;
  console.log("DATA BUILD SUCCESS");

  const ir = compileScripts(ctx, "4/8");
  if (!ir) return EXIT_ERROR;
  if (!bossBattleStep(ctx, validated.projectPath, "4/8")) return EXIT_ERROR;
  if (!questDataStep(ctx, validated.projectPath, "4/8")) return EXIT_ERROR;

  const audio = runAudioStep(ctx, parsed, "[5/8]", validated.projectPath);
  if (!audio.ok) return EXIT_ERROR;

  const data = validateRuntimeDataStep(ctx, "[6/8]");
  if (!data) return EXIT_ERROR;

  ctx.logger.step("[7/8] Runtime tests (gradlew agentCheck)...");
  ctx.logger.step("      " + formatRuntimeStatus(runtime));
  const tests = invokeGradle(ctx, ["agentCheck"], { javaHome: runtime.javaHome });
  if (!tests.ok) {
    ctx.logger.error("runtime tests failed (exit " + tests.exitCode + ")");
    for (const line of String(tests.output || "").trim().split(/\r?\n/).slice(-25)) {
      ctx.logger.error("  " + line);
    }
    console.log("RUNTIME TESTS FAILED");
    return EXIT_ERROR;
  }
  ctx.logger.step("      runtime tests passed");

  const runtimeImageTasks = runtimeImageArgument(ctx, parsed);
  const distTasks = ["lwjgl3:dist", ...runtimeImageTasks];
  ctx.logger.step("[7/8] Gradle desktop build (gradlew " + distTasks.join(" ") + ")...");
  ctx.logger.step("      " + formatGradleStatus(gradle));
  if (runtimeImageTasks.length > 0 && !explicitRuntimeImage(ctx, parsed)) {
    ctx.logger.step("      jpackage: the detected runtime has no jmods/, bundling it as --runtime-image");
  }
  const dist = invokeGradle(ctx, distTasks, { javaHome: runtime.javaHome });
  if (!dist.ok) {
    ctx.logger.error("Gradle desktop build failed (exit " + dist.exitCode + ")");
    for (const line of String(dist.output || "").trim().split(/\r?\n/).slice(-25)) {
      ctx.logger.error("  " + line);
    }
    console.log("GRADLE BUILD FAILED");
    return EXIT_ERROR;
  }

  // [8/8] Package: verify every artifact the task claims to have produced.
  const artifacts = desktopArtifacts(ctx.builderRoot);
  const missing = missingArtifacts([artifacts.runnableJar, artifacts.windowsExe]);
  if (missing.length > 0) {
    for (const file of missing) ctx.logger.error("missing build artifact: " + file);
    console.log("PACKAGE FAILED");
    return EXIT_ERROR;
  }
  // L4: the distribution must run without the source project, so the graphics
  // the runtime reads (Graphics/ + Fonts/) travel inside runtime-data next to
  // the generated data. Up-to-date files are skipped, so rebuilds are fast.
  if (parsed.flags["no-assets"] !== true) {
    const runtimeDataRoot = path.join(artifacts.dist, "Windows", "runtime-data");
    const copied = copyRuntimeAssets(validated.projectPath, runtimeDataRoot, ctx.logger);
    ctx.logger.step(
      "      L4 assets: Graphics/ + Fonts/ -> runtime-data (" + copied.files + " new/changed files)",
    );
    // P3: nothing readable is left in the package (the runtime also plays plain files: --no-encrypt)
    if (parsed.flags["no-encrypt"] !== true) {
      const started = Date.now();
      const encrypted = encryptTree(runtimeDataRoot);
      ctx.logger.step(
        "      P3 encryption: " + encrypted + " files in runtime-data (" + ((Date.now() - started) / 1000).toFixed(1) + " s)",
      );
    }
  }
  const outputs = [
    artifacts.runnableJar + " (" + fileSizeLabel(artifacts.runnableJar) + ")",
    artifacts.windowsExe + " (" + fileSizeLabel(artifacts.windowsExe) + ")",
  ];
  ctx.logger.step("[8/8] Package: " + path.join(artifacts.dist, "Windows") + " ready (exe + app/ + runtime/ + runtime-data/)");

  writeBuildReportStep(ctx, {
    status: "success",
    buildTarget: "desktop",
    runtimeVersion: ctx.config.version,
    libGdxVersion: readLibGdxVersion(ctx.builderRoot),
    javaVersion: runtime.version,
    gradleVersion: gradle.version,
    ...reportCounts(converted, data, ir),
    gradle: { tasks: "agentCheck, " + distTasks.join(" "), ok: true, exitCode: dist.exitCode },
    outputArtifact: outputs,
    startedAt,
    finishedAt: new Date().toISOString(),
    notes: runtimeImageTasks.length > 0
      ? ["jpackage bundled --runtime-image (the selected JDK has no jmods/)"]
      : [],
  });

  console.log("PC BUILD SUCCESS");
  console.log("BUILD SUCCESS");
  return EXIT_OK;
}

// R14: the full Android builds (project3 sections 54, 63). Modern and legacy
// are two separate targets: they share the generated game data but never a
// build configuration (API level, ABIs, Gradle module). Both package a debug
// APK marked Technical Preview - the stage 2 Android runtime is not feature
// complete, and there is no release signing setup yet.
function cmdBuildAndroid(ctx, parsed, legacy) {
  const target = androidTarget(legacy, parsed.flags.release === true);
  const startedAt = new Date().toISOString();
  const runtime = detectJavaRuntime(ctx.builderRoot);
  const gradle = detectGradle(ctx.builderRoot);

  const validated = runValidation(ctx, parsed, "[1/8]");
  if (!validated) return EXIT_ERROR;

  ctx.logger.step("[1/8] Audit: scanning data, events and scripts...");
  const audit = runAuditPipeline(ctx, validated.projectPath, {
    scan: "[1/8]",
    events: "[1/8]",
    scripts: "[1/8]",
    reports: "[1/8]",
  });
  if (!audit) return EXIT_ERROR;
  console.log("AUDIT SUCCESS");

  const converted = runConvert(ctx, validated, { scan: "[2/8]", convert: "[3/8]" }, parsed.flags["no-cache"] !== true);
  if (!converted) return EXIT_ERROR;
  console.log("DATA BUILD SUCCESS");

  const ir = compileScripts(ctx, "4/8");
  if (!ir) return EXIT_ERROR;
  if (!bossBattleStep(ctx, validated.projectPath, "4/8")) return EXIT_ERROR;
  if (!questDataStep(ctx, validated.projectPath, "4/8")) return EXIT_ERROR;

  const audio = runAudioStep(ctx, parsed, "[5/8]", validated.projectPath);
  if (!audio.ok) return EXIT_ERROR;

  const data = validateRuntimeDataStep(ctx, "[6/8]");
  if (!data) return EXIT_ERROR;

  // L3: stage the runtime data pack (generated/ + Graphics/ + Fonts/) as one
  // zip asset; the launcher unpacks it on first launch. --no-data keeps the
  // slim APK for the adb-push workflow, and legacy never stages it.
  const stagedAssets = path.join(ctx.builderRoot, "runtime", "android", "build", "android-assets");
  rmSync(stagedAssets, { recursive: true, force: true });
  let distDataPack = null;
  if (!legacy && parsed.flags["no-data"] !== true) {
    mkdirSync(stagedAssets, { recursive: true });
    ctx.logger.step("[7/8] Data pack: generated/ + Graphics/ + Fonts/ -> runtime-data.zip ...");
    const packStarted = Date.now();
    const packZip = path.join(stagedAssets, "runtime-data.zip");
    const pack = createDataPack(
      [
        { root: path.join(ctx.builderRoot, "generated"), prefix: "generated" },
        { root: path.join(validated.projectPath, "Graphics"), prefix: "Graphics" },
        { root: path.join(validated.projectPath, "Fonts"), prefix: "Fonts" },
      ],
      packZip,
      { encrypt: parsed.flags["no-encrypt"] !== true }, // P3: stored encrypted; the app reads them through ResourceCrypto
    );
    writeFileSync(path.join(stagedAssets, "runtime-data.version"), pack.version + "\n", "utf8");
    mkdirSync(ctx.paths.dist, { recursive: true });
    distDataPack = path.join(ctx.paths.dist, "PokemonGame-Android-data.zip");
    copyFileSync(packZip, distDataPack);
    ctx.logger.step(
      "      data pack: " + pack.files + " files, " + fileSizeLabel(packZip) + ", " +
        ((Date.now() - packStarted) / 1000).toFixed(1) + " s (also " + distDataPack + ")",
    );
  }

  ctx.logger.step("      " + formatTargetStatus(target));
  const gradleTasks = [target.gradleModule + ":" + target.gradleTask];
  ctx.logger.step("[7/8] Gradle Android build (gradlew " + gradleTasks.join(" ") + ")...");
  ctx.logger.step("      " + formatRuntimeStatus(runtime));
  ctx.logger.step("      " + formatGradleStatus(gradle));
  const build = invokeGradle(ctx, gradleTasks, { javaHome: runtime.javaHome });
  if (!build.ok) {
    ctx.logger.error("Gradle Android build failed (exit " + build.exitCode + ")");
    for (const line of String(build.output || "").trim().split(/\r?\n/).slice(-25)) {
      ctx.logger.error("  " + line);
    }
    console.log("GRADLE BUILD FAILED");
    return EXIT_ERROR;
  }

  // [8/8] Package: copy the debug APK next to the desktop artifacts.
  const apk = androidApkPath(ctx.builderRoot, target);
  if (missingArtifacts([apk]).length > 0) {
    ctx.logger.error("missing build artifact: " + apk);
    console.log("PACKAGE FAILED");
    return EXIT_ERROR;
  }
  const artifact = path.join(ctx.paths.dist, target.artifact);
  mkdirSync(ctx.paths.dist, { recursive: true });
  copyFileSync(apk, artifact);
  ctx.logger.step(
    "[8/8] Package: " + artifact + " (" + fileSizeLabel(artifact) + ")" +
      (target.technicalPreview ? ", Technical Preview" : ""),
  );

  writeBuildReportStep(ctx, {
    status: "success",
    buildTarget: target.id,
    technicalPreview: target.technicalPreview,
    runtimeVersion: ctx.config.version,
    libGdxVersion: readLibGdxVersion(ctx.builderRoot),
    javaVersion: runtime.version,
    gradleVersion: gradle.version,
    ...reportCounts(converted, data, ir),
    gradle: { tasks: gradleTasks.join(" "), ok: true, exitCode: build.exitCode },
    outputArtifact: distDataPack ? [artifact, distDataPack] : [artifact],
    startedAt,
    finishedAt: new Date().toISOString(),
    notes: target.technicalPreview
      ? ["Technical Preview: the Android runtime is not feature complete yet"]
      : [],
  });

  console.log(target.label.toUpperCase() + " BUILD SUCCESS");
  if (target.technicalPreview) {
    console.log("TECHNICAL PREVIEW: install the APK on a device to verify (no device in this environment).");
  }
  console.log("BUILD SUCCESS");
  return EXIT_OK;
}

// Phase A1: read only audio scan. Records every asset with its real codec
// (detected from content, never from the extension) and every problem with
// its file and reason. Writes build/reports/audio-audit.json and
// docs/audio-audit.md. Whether problems block a release build is a policy
// decision handled by build-audio / the release pipeline, not by this scan.
function cmdAudioAudit(ctx, parsed) {
  const validated = runValidation(ctx, parsed, "[1/2]");
  if (!validated) return EXIT_ERROR;
  let options;
  try {
    options = resolveAudioOptions(ctx.config.audio, {
      preset: parsed.flags.preset,
      bgmQuality: parsed.flags["bgm-quality"],
      bgsQuality: parsed.flags["bgs-quality"],
      sampleRate: parsed.flags["sample-rate"],
      convertSE: parsed.flags["no-se-convert"] ? false : undefined,
      deduplicate: parsed.flags["no-dedup"] ? false : undefined,
      analyzeSilence: parsed.flags["no-analyze-silence"] ? false : undefined,
    });
  } catch (error) {
    ctx.logger.error(error.message);
    console.log("AUDIO AUDIT FAILED");
    return EXIT_ERROR;
  }
  ctx.logger.step("[2/2] Scanning Audio/ assets (BGM/BGS/ME/SE, content based codec detection)...");
  const scan = scanAudio(validated.projectPath, options);
  for (const warning of scan.warnings) ctx.logger.warn(warning);
  for (const line of formatAudioScanSummary(scan)) ctx.logger.step("      " + line);
  const written = writeAudioAuditReports(ctx, validated.projectPath, scan, options, ctx.config.version);
  if (!written) {
    console.log("AUDIO AUDIT FAILED");
    return EXIT_ERROR;
  }
  console.log("AUDIO AUDIT SUCCESS");
  console.log("BUILD SUCCESS");
  return EXIT_OK;
}

// Phase A4 / R14: `build-audio` = validation + the shared audio step below.
function cmdBuildAudio(ctx, parsed) {
  const validated = runValidation(ctx, parsed, "[1/1]");
  if (!validated) return EXIT_ERROR;
  const audio = runAudioStep(ctx, parsed, "[1/1]", validated.projectPath);
  return audio.ok ? EXIT_OK : EXIT_ERROR;
}

// The audio compile shared by `build-audio` and the build pipelines (step
// [5/8]). Returns { ok, manifestCount } and writes the runtime manifest plus
// the build report; an empty Audio/ directory is a valid, silent game.
function runAudioStep(ctx, parsed, stepLabel, projectPath) {
  ctx.logger.step(stepLabel + " Compiling Audio/ into generated/audio...");
  const options = {
    ...DEFAULT_AUDIO_OPTIONS,
    ...resolveAudioOptions(ctx.config && ctx.config.audio, parsed.flags || {}),
    trimSilence: Boolean(parsed.flags && parsed.flags["trim-silence"]),
    // Manual loop start in seconds for tracks without a LOOPSTART tag:
    // {"Battle wild": 3.5, ...} in build/audio-loop-overrides.json
    loopOverrides: (() => {
      try {
        const file = path.join(ctx.paths.build, "audio-loop-overrides.json");
        return existsSync(file) ? JSON.parse(readFileSync(file, "utf8")) : {};
      } catch (error) {
        console.log("audio-loop-overrides.json ignored: " + error.message);
        return {};
      }
    })(),
    ffmpegPath: path.join(ctx.builderRoot, "tools", "ffmpeg", "bin", "ffmpeg.exe"),
  };
  const scan = scanAudio(projectPath, options);
  const build = runAudioBuild({
    projectPath,
    generatedRoot: ctx.paths.generated,
    scan,
    options,
    runner: (command, args) => execFileSync(command, args, { stdio: "inherit" }),
  });

  // The runtime reads generated/audio/audio-manifest.json (GameDatabase); the
  // copy next to generated/ stays for convenience.
  const manifestPath = path.join(ctx.paths.generated, "audio", "audio-manifest.json");
  mkdirSync(path.dirname(manifestPath), { recursive: true });
  writeFileSync(manifestPath, JSON.stringify(build.manifest, null, 1), "utf8");
  writeFileSync(path.join(ctx.paths.generated, "audio-manifest.json"),
      JSON.stringify(build.manifest, null, 1), "utf8");
  const reportPath = path.join(ctx.paths.build, "reports", "audio-build.json");
  mkdirSync(path.dirname(reportPath), { recursive: true });
  writeFileSync(reportPath, JSON.stringify({
    preset: options.preset,
    copied: build.copied,
    transcoded: build.transcoded,
    reused: build.reused,
    looped: build.looped,
    introFiles: build.introFiles,
    skipped: build.skipped,
    failed: build.failed,
    problems: build.problems,
  }, null, 1), "utf8");

  console.log(`AUDIO BUILD: copied=${build.copied} transcoded=${build.transcoded} `
    + `reused=${build.reused} looped=${build.looped} intros=${build.introFiles} `
    + `skipped=${build.skipped} failed=${build.failed}`);
  console.log(`audio-manifest.json entries=${Object.keys(build.manifest.audio).length}`);
  console.log(`wrote ${manifestPath}`);
  console.log(`wrote ${reportPath}`);
  return { ok: build.failed === 0, manifestCount: Object.keys(build.manifest.audio).length, build };
}

function removeAndReset(ctx, dir) {
  if (!isInside(ctx.builderRoot, dir)) {
    throw new Error("refusing to clean outside the builder root: " + dir);
  }
  if (existsSync(dir)) {
    rmSync(dir, { recursive: true, force: true });
    ctx.logger.info("Removed: " + dir);
  }
  mkdirSync(dir, { recursive: true });
  try {
    writeFileSync(path.join(dir, ".gitkeep"), "", "utf8");
  } catch (error) {
    ctx.logger.warn("cannot restore .gitkeep in " + dir + ": " + error.message);
  }
}

function cmdClean(ctx, parsed) {
  const all = Boolean(parsed.flags.all);
  removeAndReset(ctx, ctx.paths.build);
  removeAndReset(ctx, ctx.paths.generated);
  const buildTemp = path.join(ctx.paths.logs, "build-temp");
  if (existsSync(buildTemp)) {
    rmSync(buildTemp, { recursive: true, force: true });
    ctx.logger.info("Removed: " + buildTemp);
  }
  if (all) {
    removeAndReset(ctx, ctx.paths.dist);
    const keep = new Set([
      path.basename(ctx.logger.logFile),
      path.basename(ctx.logger.latestFile),
    ]);
    for (const name of readdirSync(ctx.paths.logs)) {
      if (!name.endsWith(".log") || keep.has(name)) continue;
      rmSync(path.join(ctx.paths.logs, name), { force: true });
      ctx.logger.info("Removed log: " + name);
    }
  }
  ctx.logger.info("Clean finished" + (all ? " (--all)" : ""));
  console.log("CLEAN SUCCESS");
  return EXIT_OK;
}

function dispatch(ctx, parsed) {
  switch (parsed.command) {
    case "help":
      return cmdHelp();
    case "validate":
      return cmdValidate(ctx, parsed);
    case "audit":
      return cmdAudit(ctx, parsed);
    case "convert":
    case "audio-audit":
      return cmdAudioAudit(ctx, parsed);
    case "build-audio":
      return cmdBuildAudio(ctx, parsed);
    case "build-data":
      return cmdBuildData(ctx, parsed);
    case "build-pc":
      return cmdBuildPc(ctx, parsed);
    case "build-android":
      return cmdBuildAndroid(ctx, parsed, false);
    case "build-android-legacy":
      return cmdBuildAndroid(ctx, parsed, true);
    case "clean":
      return cmdClean(ctx, parsed);
    default:
      console.error('ERROR: unknown command "' + parsed.command + '"');
      console.log(USAGE);
      return EXIT_ERROR;
  }
}

// Phase 13: a builder context that survives even when the configuration or
// an output directory is broken, so the failing setup step can still be
// logged (hard rule 6: every error goes to the log and the exit code).
function fallbackContext(builderRoot) {
  let config;
  try {
    config = loadConfig(builderRoot);
  } catch (error) {
    config = structuredClone(DEFAULT_CONFIG);
  }
  let paths;
  try {
    paths = resolveOutputPaths(builderRoot, config);
  } catch (error) {
    paths = {
      root: builderRoot,
      build: path.join(builderRoot, "build"),
      generated: path.join(builderRoot, "generated"),
      dist: path.join(builderRoot, "dist"),
      logs: path.join(builderRoot, "logs"),
    };
  }
  return { builderRoot, config, paths };
}

export function runCli(builderRoot, argv, options = {}) {
  const parsed = parseArgs(argv);
  const command = parsed.command || "help";
  if (!COMMANDS.includes(command)) {
    console.error('ERROR: unknown command "' + command + '"');
    console.log(USAGE);
    return EXIT_ERROR;
  }
  // The logger comes first: an unwritable output directory or a broken
  // builder-config.json is a failure that must still be recorded.
  let ctx;
  let setupError = null;
  try {
    ctx = createContext(builderRoot, options);
  } catch (error) {
    setupError = error;
    ctx = fallbackContext(builderRoot);
  }
  const projectPath = parsed.positional[0] || parsed.flags.project || ctx.config.source.rmxpProject || "";
  ctx.logger = new Logger(ctx.paths.logs, {
    version: ctx.config.version,
    command,
    argv,
    project: projectPath || "(not set)",
  });
  if (setupError) {
    ctx.logger.error("builder setup failed: " + setupError.message);
    console.log("BUILD FAILED");
    ctx.logger.finish(EXIT_ERROR);
    return EXIT_ERROR;
  }
  let exitCode = EXIT_OK;
  try {
    exitCode = dispatch(ctx, { ...parsed, command });
  } catch (error) {
    exitCode = EXIT_ERROR;
    ctx.logger.error(error && error.stack ? error.stack : String(error));
  }
  if (exitCode === EXIT_ERROR) console.log("BUILD FAILED");
  ctx.logger.finish(exitCode);
  return exitCode;
}
