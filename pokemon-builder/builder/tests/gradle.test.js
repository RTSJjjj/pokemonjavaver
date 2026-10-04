// Unit tests for the R14 Gradle runner (project3 sections 45, 52-54).
// Run with: node --test builder/tests

import test from "node:test";
import assert from "node:assert/strict";
import path from "node:path";

import {
  androidApkPath,
  desktopArtifacts,
  fileSizeLabel,
  findFreeDrive,
  isAscii,
  missingArtifacts,
  mountAsciiDrive,
  needsAsciiDrive,
  runGradleTasks,
} from "../src/gradle.js";

test("isAscii accepts every byte of an ASCII path and rejects CJK", () => {
  assert.equal(isAscii("E:\\repo\\pokemon-builder"), true);
  assert.equal(isAscii("E:\\仓库\\范例"), false);
});

test("needsAsciiDrive is Windows only and only for non-ASCII paths", () => {
  assert.equal(needsAsciiDrive("C:\\ascii", "win32"), false);
  assert.equal(needsAsciiDrive("C:\\仓库", "win32"), true);
  assert.equal(needsAsciiDrive("/home/仓库", "linux"), false);
});

test("findFreeDrive picks the first free letter of the tried set", () => {
  const taken = new Set(["Y:\\"]);
  assert.equal(findFreeDrive({ exists: (p) => taken.has(p) }), "Z");
  assert.equal(findFreeDrive({ exists: () => true }), null);
});

test("mountAsciiDrive substs the root and reports failures", () => {
  const calls = [];
  const spawn = (command, args) => {
    calls.push([command, args]);
    return { status: 0, stdout: "", stderr: "" };
  };
  const mount = mountAsciiDrive("C:\\仓库", { spawn, letter: "Y" });
  assert.equal(mount.root, "Y:");
  assert.deepEqual(calls[0], ["subst", ["Y:", "C:\\仓库"]]);
  mount.unmount();
  assert.deepEqual(calls[1], ["subst", ["Y:", "/d"]]);

  const failing = mountAsciiDrive("C:\\仓库", { spawn: () => ({ status: 1 }), letter: "Y" });
  assert.equal(failing, null);
});

test("runGradleTasks reports the missing wrapper instead of spawning", () => {
  const result = runGradleTasks("C:\\missing", ["agentCheck"], { exists: () => false });
  assert.equal(result.missing, true);
  assert.equal(result.ok, false);
});

test("runGradleTasks runs an ASCII path directly through cmd on Windows", () => {
  const calls = [];
  const spawn = (command, args, options) => {
    calls.push({ command, args, options });
    return { status: 0, stdout: "BUILD SUCCESSFUL", stderr: "" };
  };
  const result = runGradleTasks("C:\\repo\\builder", ["agentCheck"], {
    spawn,
    exists: () => true,
    platform: "win32",
    javaHome: "C:\\jdk",
  });
  assert.equal(result.ok, true);
  assert.equal(result.exitCode, 0);
  assert.match(result.output, /BUILD SUCCESSFUL/);
  assert.equal(calls.length, 1);
  // .bat files can only be spawned through cmd.exe.
  assert.equal(calls[0].command, "cmd.exe");
  assert.deepEqual(calls[0].args.slice(0, 3), ["/d", "/s", "/c"]);
  assert.equal(
    calls[0].args[3],
    path.join("C:\\repo\\builder", "runtime", "gradlew.bat") + " agentCheck",
  );
  assert.equal(calls[0].options.cwd, path.join("C:\\repo\\builder", "runtime"));
  assert.equal(calls[0].options.env.JAVA_HOME, "C:\\jdk");
});

test("runGradleTasks runs a Chinese path through an ASCII subst drive", () => {
  const calls = [];
  const spawn = (command, args, options) => {
    calls.push({ command, args, options });
    return { status: 0, stdout: "", stderr: "" };
  };
  const wrapper = path.join("C:\\仓库\\builder", "runtime", "gradlew.bat");
  const result = runGradleTasks("C:\\仓库\\builder", ["lwjgl3:dist"], {
    spawn,
    exists: (file) => file === wrapper,
    platform: "win32",
  });
  assert.equal(result.ok, true);
  // subst mount + gradle run + subst /d unmount
  assert.equal(calls.length, 3);
  assert.deepEqual(calls[0].args.slice(0, 1), ["Y:"]);
  assert.equal(calls[1].options.cwd, path.join("Y:\\", "runtime"));
  assert.deepEqual(calls[2].args, ["Y:", "/d"]);
});

test("artifact helpers point at the documented locations", () => {
  const root = "C:\\builder";
  const desktop = desktopArtifacts(root);
  assert.equal(desktop.runnableJar, path.join(root, "dist", "PokemonGame.jar"));
  assert.equal(desktop.windowsExe, path.join(root, "dist", "Windows", "PokemonGame.exe"));

  const apk = androidApkPath(root, { gradleModule: "android-legacy" });
  assert.equal(
    apk,
    path.join(root, "runtime", "android-legacy", "build", "outputs", "apk", "debug", "android-legacy-debug.apk"),
  );

  const missing = missingArtifacts(["a.jar", "b.exe"], { exists: (file) => file === "a.jar" });
  assert.deepEqual(missing, ["b.exe"]);
});

test("fileSizeLabel formats bytes, KB and MB", () => {
  assert.equal(fileSizeLabel("f", { stat: () => ({ size: 512 }) }), "512 bytes");
  assert.equal(fileSizeLabel("f", { stat: () => ({ size: 2048 }) }), "2 KB");
  assert.equal(fileSizeLabel("f", { stat: () => ({ size: 2 * 1024 * 1024 }) }), "2.0 MB");
  assert.equal(fileSizeLabel("f", { stat: () => { throw new Error("nope"); } }), "");
});
