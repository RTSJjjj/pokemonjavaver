// Runtime detection unit tests (Phase 9).
//
// `java -version` is injected, so these tests pass on machines with and
// without a JDK installed and never depend on the environment of the CI box.

import test from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, writeFileSync, rmSync } from "node:fs";
import path from "node:path";
import os from "node:os";

import {
  ANDROID_TARGETS,
  androidTarget,
  detectGradle,
  detectJavaRuntime,
  formatGradleStatus,
  formatRuntimeStatus,
  formatTargetStatus,
  probeJavaExecutable,
  probeOutput,
  runtimeImageNeeded,
} from "../src/runtime.js";

function scratch(test, prefix = "pb-runtime-") {
  const dir = mkdtempSync(path.join(os.tmpdir(), prefix));
  test.after(() => rmSync(dir, { recursive: true, force: true }));
  return dir;
}

// Stand-in for `java -version`: exit code 0 and the usual banner on stderr.
function fakeJava(version) {
  return () => ({ status: 0, stdout: "", stderr: 'openjdk version "' + version + '" 2022-01-18\nOpenJDK Runtime Environment' });
}

function noJava() {
  return () => ({ status: 1, stdout: "", stderr: "" });
}

function notInstalled() {
  return () => ({ error: new Error("spawn ENOENT"), status: null });
}

function writeBundledJdk(root) {
  const java = path.join(root, "runtime", "java", "bin", process.platform === "win32" ? "java.exe" : "java");
  mkdirSync(path.dirname(java), { recursive: true });
  writeFileSync(java, "not executed: the probe is injected", "utf8");
  return java;
}

test("probeJavaExecutable reads the version from the java -version banner", () => {
  const version = probeJavaExecutable("java", fakeJava("17.0.2"));
  assert.equal(version, "17.0.2");
});

test("probeJavaExecutable reads versions printed without quotes", () => {
  const run = () => ({ status: 0, stdout: "java version 1.8.0_312\n", stderr: "" });
  assert.equal(probeJavaExecutable("java", run), "1.8.0_312");
});

test("probeJavaExecutable returns null when the executable is unusable", () => {
  assert.equal(probeJavaExecutable("java", noJava()), null);
  assert.equal(probeJavaExecutable("java", notInstalled()), null);
  assert.equal(probeJavaExecutable("java", () => ({ error: new Error("boom"), status: 0 })), null);
  assert.equal(probeJavaExecutable("java", () => {
    throw new Error("spawn crashed");
  }), null);
  assert.equal(probeJavaExecutable("", noJava()), null);
});

test("detectJavaRuntime prefers a JDK bundled with the builder", (t) => {
  const root = scratch(t);
  writeBundledJdk(root);
  const runtime = detectJavaRuntime(root, {
    run: fakeJava("21"),
    env: { JAVA_HOME: path.join(root, "jdk-from-env") },
  });
  assert.equal(runtime.found, true);
  assert.equal(runtime.source, "bundled");
  assert.equal(runtime.javaHome, path.join(root, "runtime", "java"));
  assert.equal(runtime.version, "21");
  assert.match(runtime.probed.join(" | "), /bundled runtime\/java/);
});

test("detectJavaRuntime uses JAVA_HOME when no bundled JDK exists", (t) => {
  const root = scratch(t);
  const home = path.join(root, "jdk");
  const java = path.join(home, "bin", process.platform === "win32" ? "java.exe" : "java");
  mkdirSync(path.dirname(java), { recursive: true });
  writeFileSync(java, "not executed: the probe is injected", "utf8");
  const runtime = detectJavaRuntime(root, { run: fakeJava("17.0.2"), env: { JAVA_HOME: home } });
  assert.equal(runtime.found, true);
  assert.equal(runtime.source, "JAVA_HOME");
  assert.equal(runtime.javaHome, home);
  assert.equal(runtime.version, "17.0.2");
  // Only the bundled JDK is reported as missing: JAVA_HOME really exists here.
  assert.equal(runtime.skipped.length, 1);
  assert.ok(runtime.skipped[0].includes("bundled runtime/java is not installed"));
});

test("detectJavaRuntime falls back to PATH and reports every probe", (t) => {
  const root = scratch(t);
  const runtime = detectJavaRuntime(root, { run: noJava(), env: {} });
  assert.equal(runtime.found, false);
  assert.equal(runtime.source, null);
  assert.deepEqual(runtime.probed, ["PATH"]);
  assert.ok(runtime.skipped.some((note) => note.includes("JAVA_HOME is not set")));
  assert.ok(runtime.skipped.some((note) => note.includes("bundled runtime/java is not installed")));
});

test("detectJavaRuntime skips JAVA_HOME when it is not installed", (t) => {
  const root = scratch(t);
  const missing = path.join(root, "no-such-jdk");
  const runtime = detectJavaRuntime(root, { run: fakeJava("11"), env: { JAVA_HOME: missing } });
  // The JAVA_HOME candidate is probed and fails, so PATH still wins.
  assert.equal(runtime.found, true);
  assert.equal(runtime.source, "PATH");
  assert.equal(runtime.javaHome, "");
  assert.deepEqual(runtime.probed, ["PATH"]);
  assert.ok(runtime.skipped.some((note) => note.includes("JAVA_HOME has no java binary")));
});

test("detectJavaRuntime never reports a runtime it could not start", (t) => {
  const root = scratch(t);
  const runtime = detectJavaRuntime(root, {
    run: notInstalled(),
    env: { JAVA_HOME: path.join(root, "broken-jdk") },
  });
  assert.equal(runtime.found, false);
  assert.equal(runtime.version, "");
  assert.equal(runtime.executable, "");
  assert.deepEqual(runtime.probed, ["PATH"]);
  assert.ok(runtime.skipped.some((note) => note.includes("JAVA_HOME has no java binary")));
});

test("formatRuntimeStatus describes both outcomes on one line", () => {
  const found = formatRuntimeStatus({ found: true, source: "JAVA_HOME", version: "17.0.2" });
  assert.equal(found, "Java runtime: found (JAVA_HOME, Java 17.0.2)");
  const missing = formatRuntimeStatus({ found: false, probed: ["PATH"] });
  assert.equal(missing, "Java runtime: not found (checked PATH)");
});

test("detectJavaRuntime works against the real environment", (t) => {
  const root = scratch(t);
  const runtime = detectJavaRuntime(root);
  assert.equal(typeof runtime.found, "boolean");
  assert.ok(runtime.probed.length >= 1);
  if (runtime.found) {
    assert.ok(runtime.version.length > 0);
    assert.ok(runtime.source);
  } else {
    assert.equal(runtime.version, "");
    assert.ok(runtime.skipped.length >= 1);
  }
});

test("probeOutput returns the combined output and null on failure", () => {
  const run = () => ({ status: 0, stdout: "hello\n", stderr: "world\n" });
  assert.equal(probeOutput("tool", ["--version"], run), "hello\n\nworld\n");
  assert.equal(probeOutput("tool", ["--version"], noJava()), null);
});

test("androidTarget returns the modern and the legacy descriptor", () => {
  const modern = androidTarget(false);
  assert.equal(modern.id, "android");
  assert.equal(modern.command, "build-android");
  assert.equal(modern.apiLevel, "API 21+");
  assert.equal(modern.minApiLevel, 21);
  assert.equal(modern.gradleTask, "assembleDebug");
  assert.equal(modern.technicalPreview, true);
  assert.deepEqual(modern.abi, ["armeabi-v7a", "arm64-v8a", "x86", "x86_64"]);
  assert.equal(modern.artifact, "PokemonGame-Android.apk");

  const legacy = androidTarget(true);
  assert.equal(legacy.id, "androidLegacy");
  assert.equal(legacy.command, "build-android-legacy");
  assert.equal(legacy.apiLevel, "API 21+ placeholder");
  assert.equal(legacy.minApiLevel, 21);
  assert.equal(legacy.gradleTask, "assembleDebug");
  assert.equal(legacy.technicalPreview, true);
  assert.deepEqual(legacy.abi, ["armeabi-v7a", "arm64-v8a", "x86", "x86_64"]);
  assert.equal(legacy.artifact, "PokemonGame-Android-Legacy.apk");
});

test("androidTarget --release switches to the signed release variant (L3)", () => {
  const release = androidTarget(false, true);
  assert.equal(release.gradleTask, "assembleRelease");
  assert.equal(release.artifact, "PokemonGame-Android-release.apk");
  assert.equal(release.release, true);
  assert.equal(release.gradleModule, "android", "only the task/artifact change");

  // The legacy placeholder ignores the release switch.
  assert.equal(androidTarget(true, true).gradleTask, "assembleDebug");
});

test("the two Android targets never share a Gradle module or artifact", () => {
  const modern = ANDROID_TARGETS.modern;
  const legacy = ANDROID_TARGETS.legacy;
  assert.notEqual(modern.gradleModule, legacy.gradleModule);
  assert.notEqual(modern.artifact, legacy.artifact);
  assert.notEqual(modern.runtime, legacy.runtime);
  assert.notEqual(modern.apiLevel, legacy.apiLevel);
});

test("formatTargetStatus describes one target on a single line", () => {
  const line = formatTargetStatus(androidTarget(true));
  assert.match(line, /Legacy Android \(API 21\+ placeholder/);
  assert.match(line, /armeabi-v7a/);
  assert.match(line, /:android-legacy:assembleDebug/);
  assert.match(line, /dist\\PokemonGame-Android-Legacy\.apk/);
});

test("detectGradle prefers the runtime's gradlew wrapper (L5 path fix)", (t) => {
  const root = scratch(t);
  mkdirSync(path.join(root, "runtime"), { recursive: true });
  writeFileSync(path.join(root, "runtime", "gradlew.bat"), "wrapper", "utf8");
  const gradle = detectGradle(root, {
    run: () => ({ status: 0, stdout: "Welcome to Gradle 8.5!\n", stderr: "" }),
  });
  assert.equal(gradle.found, true);
  assert.equal(gradle.source, "gradlew");
  assert.equal(gradle.version, "8.5");
  assert.equal(gradle.executable, path.join(root, "runtime", "gradlew.bat"));
  assert.match(formatGradleStatus(gradle), /Gradle: found \(gradlew, 8\.5\)/);
});

test("detectGradle falls back to gradle on PATH", (t) => {
  const root = scratch(t);
  const gradle = detectGradle(root, {
    run: () => ({ status: 0, stdout: "Gradle 7.6\n", stderr: "" }),
  });
  assert.equal(gradle.found, true);
  assert.equal(gradle.source, "PATH");
  assert.equal(gradle.version, "7.6");
  assert.deepEqual(gradle.probed, ["PATH (gradle)"]);
  assert.ok(gradle.skipped.some((note) => note.includes("no gradlew in the runtime directory")));
});

test("detectGradle reports what it checked when nothing is installed", (t) => {
  const root = scratch(t);
  const gradle = detectGradle(root, { run: noJava() });
  assert.equal(gradle.found, false);
  assert.equal(gradle.source, null);
  assert.equal(gradle.version, "");
  assert.deepEqual(gradle.probed, ["PATH (gradle)"]);
  assert.match(formatGradleStatus(gradle), /Gradle: not found \(checked PATH \(gradle\)\)/);
});

test("runtimeImageNeeded flags a jlink runtime without jmods as --runtime-image (L5)", () => {
  assert.equal(runtimeImageNeeded(""), false, "no home -> nothing to bundle");
  assert.equal(runtimeImageNeeded("/jdk", { exists: () => true }), false, "jmods/ present -> default jlink");
  assert.equal(
    runtimeImageNeeded("/jre", { exists: (file) => !String(file).endsWith("jmods") }),
    true,
    "java binary but no jmods/ -> jpackage must bundle it",
  );
  assert.equal(
    runtimeImageNeeded("/broken", { exists: (file) => String(file).endsWith("jmods") }),
    false,
    "no java binary -> unusable home",
  );
});