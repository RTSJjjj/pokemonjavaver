// Step [6/8] of the build pipelines (project3 section 63): before Gradle is
// asked to package anything, the runtime data the build just wrote must be
// loadable. This catches a broken converter run (or a script compile that
// never happened) before a long Gradle build hides the real error.
//
// Read-only: it only reads generated/ and never modifies the source project.

import { existsSync, readFileSync, readdirSync } from "node:fs";
import path from "node:path";

/**
 * Validates the generated runtime data root.
 *
 * @returns {{ ok: boolean, problems: string[], project: object|null, ir: object|null }}
 */
export function validateRuntimeData(generatedDir, options = {}) {
  const exists = options.exists || existsSync;
  const readFile = options.readFile || readFileSync;
  const problems = [];
  let project = null;
  let ir = null;

  const projectFile = path.join(generatedDir, "project.json");
  if (!exists(projectFile)) {
    problems.push("missing project.json - run the data compile first");
  } else {
    try {
      project = JSON.parse(readFile(projectFile, "utf8"));
    } catch (error) {
      problems.push("project.json is not readable JSON: " + error.message);
    }
  }

  const irFile = path.join(generatedDir, "scripts", "ir.json");
  if (!exists(irFile)) {
    problems.push("missing scripts/ir.json - the Essentials script blocks were not translated");
  } else {
    try {
      ir = JSON.parse(readFile(irFile, "utf8"));
      if (!Array.isArray(ir.commands)) {
        problems.push("scripts/ir.json has no commands array");
      }
    } catch (error) {
      problems.push("scripts/ir.json is not readable JSON: " + error.message);
    }
  }

  const audioFile = path.join(generatedDir, "audio", "audio-manifest.json");
  if (!exists(audioFile)) {
    problems.push("missing audio/audio-manifest.json - the audio step did not run");
  }

  return { ok: problems.length === 0, problems, project, ir };
}

/**
 * The counts the build report (project3 section 55) needs from the runtime
 * data. Everything is optional: missing fields come back as 0/empty.
 */
export function runtimeDataSummary(generatedDir, options = {}) {
  const readFile = options.readFile || readFileSync;
  const readdir = options.readdir || readdirSync;
  const summary = { maps: 0, events: 0, audioAssets: 0 };

  try {
    const project = JSON.parse(readFile(path.join(generatedDir, "project.json"), "utf8"));
    if (project && project.counts && typeof project.counts === "object") {
      summary.maps = Number(project.counts.maps) || 0;
      summary.events = Number(project.counts.events) || 0;
    }
  } catch (error) {
    // the validator reports the real problem first
  }

  if (!summary.maps) {
    try {
      summary.maps = readdir(path.join(generatedDir, "maps"))
        .filter((name) => name.startsWith("map-") && name.endsWith(".json")).length;
    } catch (error) {
      // keep 0
    }
  }

  try {
    const manifest = JSON.parse(readFile(path.join(generatedDir, "audio", "audio-manifest.json"), "utf8"));
    if (manifest && manifest.audio && typeof manifest.audio === "object") {
      summary.audioAssets = Object.keys(manifest.audio).length;
    }
  } catch (error) {
    // optional for the report
  }

  return summary;
}
