// Build report (project3 section 55): one machine readable JSON next to the
// generated runtime data, plus the console summary. The fields are exactly the
// ones the task book asks for; unknown values stay empty instead of inventing
// numbers.

import { writeFileSync } from "node:fs";
import path from "node:path";

export function createBuildReport(input = {}) {
  const gradle = input.gradle || {};
  return {
    status: input.status || "unknown",
    buildTarget: input.buildTarget || "",
    technicalPreview: Boolean(input.technicalPreview),
    runtimeVersion: input.runtimeVersion || "",
    libGdxVersion: input.libGdxVersion || "",
    javaVersion: input.javaVersion || "",
    gradleVersion: input.gradleVersion || "",
    mapsCompiled: Number(input.mapsCompiled) || 0,
    eventsCompiled: Number(input.eventsCompiled) || 0,
    scriptsTranslated: Number(input.scriptsTranslated) || 0,
    scriptsUnsupported: Number(input.scriptsUnsupported) || 0,
    audioAssets: Number(input.audioAssets) || 0,
    gradleResult: {
      tasks: gradle.tasks || "",
      ok: Boolean(gradle.ok),
      exitCode: typeof gradle.exitCode === "number" ? gradle.exitCode : null,
    },
    outputArtifact: Array.isArray(input.outputArtifact) ? input.outputArtifact.slice() : [],
    startedAt: input.startedAt || "",
    finishedAt: input.finishedAt || "",
    notes: Array.isArray(input.notes) ? input.notes.slice() : [],
  };
}

export function formatBuildReport(report) {
  const lines = [];
  lines.push("Build report:");
  lines.push("  Runtime Version   : " + report.runtimeVersion);
  lines.push("  libGDX Version    : " + report.libGdxVersion);
  lines.push("  Java Version      : " + report.javaVersion);
  lines.push("  Build Target      : " + report.buildTarget + (report.technicalPreview ? " (Technical Preview)" : ""));
  lines.push("  Maps Compiled     : " + report.mapsCompiled);
  lines.push("  Events Compiled   : " + report.eventsCompiled);
  lines.push("  Scripts Translated: " + report.scriptsTranslated);
  lines.push("  Scripts Unsupported: " + report.scriptsUnsupported);
  lines.push("  Audio Assets      : " + report.audioAssets);
  lines.push(
    "  Gradle Result     : " +
      (report.gradleResult.tasks || "n/a") +
      " -> " +
      (report.gradleResult.ok ? "success" : "failed") +
      (report.gradleResult.exitCode === null ? "" : " (exit " + report.gradleResult.exitCode + ")"),
  );
  for (const artifact of report.outputArtifact) {
    lines.push("  Output Artifact   : " + artifact);
  }
  for (const note of report.notes) {
    lines.push("  Note              : " + note);
  }
  return lines;
}

/** Writes `generated/build-report.json` and returns its path. */
export function writeBuildReport(generatedDir, report, options = {}) {
  const writeFile = options.writeFile || writeFileSync;
  const file = path.join(generatedDir, "build-report.json");
  writeFile(file, JSON.stringify(report, null, 2), "utf8");
  return file;
}
