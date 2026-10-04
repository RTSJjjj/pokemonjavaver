// Unit tests for the section 55 build report.

import test from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, readFileSync } from "node:fs";
import path from "node:path";
import os from "node:os";

import { createBuildReport, formatBuildReport, writeBuildReport } from "../src/build-report.js";

test("createBuildReport maps the section 55 fields and keeps defaults", () => {
  const report = createBuildReport();
  assert.equal(report.status, "unknown");
  assert.equal(report.runtimeVersion, "");
  assert.equal(report.mapsCompiled, 0);
  assert.deepEqual(report.outputArtifact, []);
  assert.equal(report.gradleResult.ok, false);
  assert.equal(report.gradleResult.exitCode, null);

  const filled = createBuildReport({
    status: "success",
    buildTarget: "desktop",
    runtimeVersion: "0.1.0",
    libGdxVersion: "1.13.5",
    javaVersion: "21.0.7",
    mapsCompiled: 213,
    eventsCompiled: 5432,
    scriptsTranslated: 3712,
    scriptsUnsupported: 838,
    audioAssets: 2403,
    gradle: { tasks: "agentCheck, lwjgl3:dist", ok: true, exitCode: 0 },
    outputArtifact: ["dist/PokemonGame.jar"],
  });
  assert.equal(filled.buildTarget, "desktop");
  assert.equal(filled.mapsCompiled, 213);
  assert.equal(filled.gradleResult.tasks, "agentCheck, lwjgl3:dist");
  assert.equal(filled.technicalPreview, false);
});

test("formatBuildReport prints every build report field", () => {
  const lines = formatBuildReport(
    createBuildReport({
      runtimeVersion: "0.1.0",
      libGdxVersion: "1.13.5",
      javaVersion: "21.0.7",
      buildTarget: "android",
      technicalPreview: true,
      mapsCompiled: 213,
      eventsCompiled: 5432,
      scriptsTranslated: 3712,
      scriptsUnsupported: 838,
      audioAssets: 2403,
      gradle: { tasks: "android:assembleDebug", ok: true, exitCode: 0 },
      outputArtifact: ["dist/PokemonGame-Android.apk"],
      notes: ["Technical Preview"],
    }),
  );
  const text = lines.join("\n");
  assert.match(text, /Runtime Version   : 0\.1\.0/);
  assert.match(text, /libGDX Version    : 1\.13\.5/);
  assert.match(text, /Java Version      : 21\.0\.7/);
  assert.match(text, /Build Target      : android \(Technical Preview\)/);
  assert.match(text, /Maps Compiled     : 213/);
  assert.match(text, /Events Compiled   : 5432/);
  assert.match(text, /Scripts Translated: 3712/);
  assert.match(text, /Scripts Unsupported: 838/);
  assert.match(text, /Audio Assets      : 2403/);
  assert.match(text, /Gradle Result     : android:assembleDebug -> success \(exit 0\)/);
  assert.match(text, /Output Artifact   : dist\/PokemonGame-Android\.apk/);
  assert.match(text, /Note              : Technical Preview/);
});

test("writeBuildReport writes generated/build-report.json", () => {
  const dir = mkdtempSync(path.join(os.tmpdir(), "pb-report-"));
  const file = writeBuildReport(dir, createBuildReport({ status: "success" }));
  assert.equal(file, path.join(dir, "build-report.json"));
  const parsed = JSON.parse(readFileSync(file, "utf8"));
  assert.equal(parsed.status, "success");
});
